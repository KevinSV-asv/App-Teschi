const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const sql = require('mssql');
const crypto = require('crypto');
const nodemailer = require('nodemailer');
const { crearCorreoGraph, variablesGraphFaltantes } = require('./correo-graph');
const jwt = require('jsonwebtoken');
const multer = require('multer');
const ExcelJS = require('exceljs');

dotenv.config({ override: true });

const app = express();
const port = process.env.PORT || 4000;

app.use(cors());
app.use(express.json());

// Solo en memoria (nunca se escribe a disco) — el archivo se procesa y se
// descarta de inmediato. 5 MB es de sobra para un horario semanal en Excel.
const uploadHorario = multer({ storage: multer.memoryStorage(), limits: { fileSize: 5 * 1024 * 1024 } });

const PASSWORD_KEY_LENGTH = 64;
const PASSWORD_SALT_LENGTH = 16;
const PASSWORD_COST = 16384;
const PASSWORD_BLOCK_SIZE = 8;
const PASSWORD_PARALLELIZATION = 1;

const OTP_VALIDEZ_MS = 10 * 60 * 1000;
const OTP_MAX_INTENTOS = 5;

// ─── Límite de intentos (en memoria) ────────────────────────────────────────
// Suficiente para una sola instancia del backend, que es el despliegue actual
// (ver DEC-019). Si algún día se corre en más de una instancia, esto necesita
// moverse a un almacén compartido (Redis) — documentado como límite conocido.
const intentosPorClave = new Map();
function excedeLimite(clave, maxIntentos, ventanaMs) {
  const ahora = Date.now();
  const historial = (intentosPorClave.get(clave) || []).filter((t) => ahora - t < ventanaMs);
  historial.push(ahora);
  intentosPorClave.set(clave, historial);
  return historial.length > maxIntentos;
}

// ─── Envío de correo (OTP) ───────────────────────────────────────────────────
// Antes esto vivía en la app Android con las credenciales SMTP compiladas
// dentro del APK (extraíbles por cualquiera que lo descompilara). Ahora el
// correo se envía únicamente desde el backend — la app solo pide un código
// y lo verifica, nunca ve la contraseña de la cuenta de correo.
let mailTransporter = null;
function getMailTransporter() {
  if (!mailTransporter) {
    mailTransporter = nodemailer.createTransport({
      host: process.env.EMAIL_SMTP_HOST || 'smtp.gmail.com',
      port: Number(process.env.EMAIL_SMTP_PORT) || 587,
      secure: false,
      auth: {
        user: process.env.EMAIL_SENDER,
        pass: String(process.env.EMAIL_APP_PASSWORD || '').replace(/\s+/g, '')
      }
    });
  }
  return mailTransporter;
}

const ASUNTO_OTP = 'Tu código de verificación AppTESCHI';

function htmlCorreoOtp(codigo) {
  return `
      <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"></head>
      <body style="font-family:Arial,sans-serif;background:#F4F7F4;margin:0;padding:20px;">
        <div style="max-width:480px;margin:auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
          <div style="background:#1E5631;padding:24px;text-align:center;">
            <h1 style="color:#fff;margin:0;font-size:24px;">AppTESCHI</h1>
            <p style="color:#a8d5a2;margin:4px 0 0;font-size:14px;">Sistema Escolar TESCHI</p>
          </div>
          <div style="padding:32px 24px;text-align:center;">
            <p style="color:#0C2B14;font-size:16px;margin:0 0 8px;">Tu código de verificación es:</p>
            <div style="background:#F4F7F4;border:2px solid #4C9A2A;border-radius:8px;padding:16px;margin:16px 0;letter-spacing:8px;font-size:36px;font-weight:bold;color:#1E5631;">${codigo}</div>
            <p style="color:#4a6b52;font-size:13px;margin:0;">Ingresa este código en la app para completar tu acceso. Vence en 10 minutos.<br>Si no solicitaste este código, ignora este mensaje.</p>
          </div>
          <div style="background:#e8f0e9;padding:16px 24px;text-align:center;">
            <p style="color:#4a6b52;font-size:11px;margin:0;">Mensaje automático · AppTESCHI<br>Tecnológico de Estudios Superiores de Chimalhuacán</p>
          </div>
        </div>
      </body></html>
    `;
}

// Si están las cuatro variables MS_* el correo sale por Microsoft Graph (cuenta institucional);
// si no, por SMTP (EMAIL_*), como hasta ahora. Ver correo-graph.js.
const faltantesGraph = variablesGraphFaltantes();
let clienteGraph = null;
if (faltantesGraph.length === 0) {
  clienteGraph = crearCorreoGraph({
    tenantId: process.env.MS_TENANT_ID.trim(),
    clientId: process.env.MS_CLIENT_ID.trim(),
    clientSecret: process.env.MS_CLIENT_SECRET.trim(),
    remitente: process.env.MS_SENDER.trim()
  });
} else if (faltantesGraph.length < 4) {
  console.warn(`[API] Correo por Microsoft Graph NO activado: faltan ${faltantesGraph.join(', ')}. Se usa SMTP.`);
}
console.log(`[API] Correo OTP por: ${clienteGraph ? 'Microsoft Graph' : 'SMTP'}`);

async function enviarCorreoOtp(destino, codigo) {
  const html = htmlCorreoOtp(codigo);
  if (clienteGraph) {
    await clienteGraph.enviar({ to: destino, subject: ASUNTO_OTP, html });
    return;
  }
  const remitente = process.env.EMAIL_SENDER;
  if (!remitente) throw new Error('EMAIL_SENDER no configurado en el backend');
  await getMailTransporter().sendMail({
    from: `"AppTESCHI" <${remitente}>`,
    to: destino,
    subject: ASUNTO_OTP,
    html
  });
}

const config = {
  user: process.env.DB_USERNAME || 'sa',
  password: process.env.DB_PASSWORD || '',
  server: process.env.DB_SERVER || 'localhost\\SQLEXPRESS',
  database: process.env.DB_DATABASE || 'AppTeschiDB',
  options: {
    encrypt: process.env.DB_ENCRYPT === 'true',
    trustServerCertificate: true,
    enableArithAbort: true
  },
  pool: {
    max: 10,
    min: 0,
    idleTimeoutMillis: 30000
  }
};

let pool;

// ─── Sesiones (JWT, ver DEC-020 y DEC-030) ──────────────────────────────────
// Hay tres tipos de token firmados con el mismo JWT_SECRET, separados por su
// audiencia (`aud`) para que uno nunca sirva donde se espera otro — antes
// `requireAdmin()` aceptaba CUALQUIER token firmado con el secreto:
//   · ticket de login  → se emite al validar usuario+contraseña y es lo único
//                         que permite pedir/verificar un OTP (10 min).
//   · sesión de admin  → se emite al verificar el OTP de un administrador.
//   · sesión de alumno → se emite al verificar el OTP de un alumno; es lo que
//                         protege /api/mi-* y /api/reinscripcion/*.
// Sin el ticket, el OTP se podía pedir a un correo ajeno para CUALQUIER
// usuario (incluido "admin") sin conocer su contraseña.
if (!process.env.JWT_SECRET || process.env.JWT_SECRET.length < 32) {
  console.error('[API] JWT_SECRET falta o tiene menos de 32 caracteres — no se inicia. Genera uno: openssl rand -base64 48');
  process.exit(1);
}

const AUD_TICKET = 'appteschi:login-ticket';
const AUD_ADMIN = 'appteschi:admin';
const AUD_ALUMNO = 'appteschi:alumno';
const JWT_EXPIRA_EN = '12h';
const JWT_ALUMNO_EXPIRA_EN = process.env.JWT_ALUMNO_EXPIRA_EN || '12h';
const TICKET_EXPIRA_EN = '10m';

function firmarTokenAdministrador(admin) {
  return jwt.sign(
    { idAdministrador: admin.IdAdministrador, usuario: admin.Usuario, nombre: admin.NombreCompleto, rol: admin.Rol },
    process.env.JWT_SECRET,
    { expiresIn: JWT_EXPIRA_EN, audience: AUD_ADMIN }
  );
}

function firmarTokenAlumno(alumno) {
  return jwt.sign(
    { idAlumno: alumno.IdAlumno, matricula: alumno.Matricula },
    process.env.JWT_SECRET,
    { expiresIn: JWT_ALUMNO_EXPIRA_EN, audience: AUD_ALUMNO }
  );
}

/** tipo: 'ALUMNO' | 'ADMINISTRADOR'; identificador: matrícula o usuario ya validados. */
function firmarTicketLogin(tipo, identificador) {
  return jwt.sign({ tipo, id: String(identificador) }, process.env.JWT_SECRET,
    { expiresIn: TICKET_EXPIRA_EN, audience: AUD_TICKET });
}

function ticketLoginValido(ticket, tipo, identificador) {
  try {
    const p = jwt.verify(String(ticket || ''), process.env.JWT_SECRET, { audience: AUD_TICKET });
    return p.tipo === tipo && String(p.id).toLowerCase() === String(identificador).trim().toLowerCase();
  } catch {
    return false;
  }
}

function tokenDeAutorizacion(req) {
  const header = String(req.headers['authorization'] || '');
  return header.startsWith('Bearer ') ? header.slice(7).trim() : null;
}

/** Middleware: exige un token de administrador válido; opcionalmente restringe por rol. */
function requireAdmin(rolesPermitidos = null) {
  return (req, res, next) => {
    const token = tokenDeAutorizacion(req);
    if (!token) {
      return res.status(401).json({ ok: false, error: 'Falta la sesión de administrador' });
    }
    try {
      const payload = jwt.verify(token, process.env.JWT_SECRET, { audience: AUD_ADMIN });
      if (rolesPermitidos && !rolesPermitidos.includes(payload.rol)) {
        console.warn(`[ADMIN-AUTH][SIN-PERMISO] usuario=${payload.usuario} rol=${payload.rol} requiere=${rolesPermitidos.join('|')}`);
        return res.status(403).json({ ok: false, error: 'Tu rol no tiene permiso para esta acción' });
      }
      req.admin = payload;
      next();
    } catch (err) {
      return res.status(401).json({ ok: false, error: 'Sesión de administrador inválida o expirada. Inicia sesión de nuevo.' });
    }
  };
}

/**
 * Middleware: exige la sesión del propio alumno. Si la ruta o el cuerpo traen
 * una matrícula, debe ser la misma de la sesión — un alumno nunca puede leer
 * ni modificar datos de otro. El header X-Sesion-Invalida permite a la app
 * distinguir "sesión vencida" de cualquier otro 401 (p. ej. contraseña actual
 * incorrecta) y mandar al alumno a iniciar sesión de nuevo.
 */
function requireAlumno() {
  return (req, res, next) => {
    const token = tokenDeAutorizacion(req);
    if (!token) {
      res.set('X-Sesion-Invalida', '1');
      return res.status(401).json({ ok: false, error: 'Falta la sesión de alumno. Inicia sesión de nuevo.' });
    }
    let payload;
    try {
      payload = jwt.verify(token, process.env.JWT_SECRET, { audience: AUD_ALUMNO });
    } catch {
      res.set('X-Sesion-Invalida', '1');
      return res.status(401).json({ ok: false, error: 'Tu sesión expiró o no es válida. Inicia sesión de nuevo.' });
    }
    const pedida = req.params?.matricula ?? req.body?.matricula;
    if (pedida !== undefined && String(pedida).trim().toLowerCase() !== String(payload.matricula).toLowerCase()) {
      console.warn(`[ALUMNO-AUTH][OTRA-MATRICULA] sesion=${payload.matricula} pidio=${String(pedida).slice(0, 20)}`);
      return res.status(403).json({ ok: false, error: 'No puedes consultar ni modificar datos de otro alumno' });
    }
    req.alumno = payload;
    next();
  };
}

// IP real del cliente para los límites de intentos. Detrás de un túnel de
// Cloudflare, req.ip es siempre la del túnel (127.0.0.1) y todos los usuarios
// compartirían el mismo cupo. TRUST_CLOUDFLARE=true SOLO debe activarse cuando
// la API escucha únicamente en 127.0.0.1 (HOST=127.0.0.1): si además fuera
// alcanzable directo, cualquiera podría falsificar el header.
function clientIp(req) {
  if (process.env.TRUST_CLOUDFLARE === 'true') {
    const cf = String(req.headers['cf-connecting-ip'] || '').trim();
    if (cf) return cf;
  }
  return req.ip;
}

function hashPassword(password, salt = crypto.randomBytes(PASSWORD_SALT_LENGTH)) {
  return {
    salt,
    hash: crypto.scryptSync(password, salt, PASSWORD_KEY_LENGTH, {
      N: PASSWORD_COST,
      r: PASSWORD_BLOCK_SIZE,
      p: PASSWORD_PARALLELIZATION
    })
  };
}

function verifyPassword(password, storedHash, storedSalt) {
  const candidate = hashPassword(password, storedSalt).hash;
  return crypto.timingSafeEqual(candidate, storedHash);
}

async function getPool() {
  if (!pool) {
    pool = await sql.connect(config);
  }
  return pool;
}

async function ensureAuditTable() {
  const db = await getPool();
  await db.request().query(`
    IF OBJECT_ID(N'dbo.AuditoriaMovimientos', N'U') IS NULL
    BEGIN
      CREATE TABLE dbo.AuditoriaMovimientos (
        IdAuditoria BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_AuditoriaMovimientos PRIMARY KEY,
        ActorMatricula NVARCHAR(50) NOT NULL,
        ActorNombre NVARCHAR(200) NOT NULL,
        Accion NVARCHAR(80) NOT NULL,
        Entidad NVARCHAR(80) NOT NULL,
        Detalle NVARCHAR(1000) NULL,
        FechaMovimiento DATETIME2(0) NOT NULL CONSTRAINT DF_Auditoria_Fecha DEFAULT (SYSUTCDATETIME())
      );
      CREATE INDEX IX_AuditoriaMovimientos_Fecha ON dbo.AuditoriaMovimientos (FechaMovimiento DESC);
    END
  `);
}

app.get('/health', async (req, res) => {
  try {
    const db = await getPool();
    await db.request().query('SELECT 1 AS ok');
    res.json({ ok: true, database: 'AppTeschiDB' });
  } catch (err) {
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.get('/api/catalogos/registro', async (req, res) => {
  try {
    const db = await getPool();
    const sistemas = await db.request().query('SELECT IdSistema AS id, Clave AS clave, Nombre AS nombre FROM dbo.CatalogoSistemas WHERE Activo = 1 ORDER BY Nombre');
    const carreras = await db.request().query('SELECT IdCarrera AS id, Clave AS clave, Nombre AS nombre, IdSistema AS idSistema FROM dbo.CatalogoCarreras WHERE Activo = 1 ORDER BY Nombre');
    res.json({ ok: true, sistemas: sistemas.recordset, carreras: carreras.recordset });
  } catch (err) {
    console.error('[CATALOGOS][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Plan de estudios (materias por semestre) de una carrera — pública, sin
// sesión, igual que /api/catalogos/registro: es catálogo curricular, no
// dato de un alumno. `clave` es la de CatalogoCarreras (ej. "ISC").
app.get('/api/plan-estudios/:clave', async (req, res) => {
  try {
    const clave = String(req.params.clave || '').trim();
    const db = await getPool();
    const result = await db.request()
      .input('Clave', sql.NVarChar(30), clave)
      .query(`
        SELECT pm.IdMateria AS id, pm.Nombre AS nombre, pm.Creditos AS creditos, pm.Semestre AS semestre
        FROM dbo.PlanEstudioMaterias pm
        JOIN dbo.CatalogoCarreras c ON c.IdCarrera = pm.IdCarrera
        WHERE c.Clave = @Clave AND pm.Activo = 1
        ORDER BY pm.Semestre, pm.IdMateria
      `);
    res.json({ ok: true, carrera: clave, materias: result.recordset });
  } catch (err) {
    console.error('[PLAN_ESTUDIOS][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Grupos reales (ciclo actual) de una carrera — misma visibilidad pública
// que el plan de estudios.
app.get('/api/grupos/:clave', async (req, res) => {
  try {
    const clave = String(req.params.clave || '').trim();
    const db = await getPool();
    const result = await db.request()
      .input('Clave', sql.NVarChar(30), clave)
      .query(`
        SELECT g.Clave AS clave, g.Semestre AS semestre, t.Nombre AS turno, g.Numero AS numero, p.Etiqueta AS periodo
        FROM dbo.Grupos g
        JOIN dbo.CatalogoCarreras c ON c.IdCarrera = g.IdCarrera
        JOIN dbo.CatalogoTurnos t ON t.IdTurno = g.IdTurno
        JOIN dbo.CatalogoPeriodos p ON p.IdPeriodo = g.IdPeriodo
        WHERE c.Clave = @Clave AND g.Activo = 1
        ORDER BY g.Semestre, g.Numero
      `);
    res.json({ ok: true, carrera: clave, grupos: result.recordset });
  } catch (err) {
    console.error('[GRUPOS][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// ─── Reinscripción (Fase 1 — ver 02_MODULOS/01_Reinscripcion.md) ────────────
// Regular = ninguna materia de un semestre anterior al suyo sigue sin
// aprobar; ahí el sistema le asigna directo el primer grupo de su semestre
// (no rastreamos a qué turno pertenece cada alumno, así que se toma el de
// menor IdGrupo como valor determinista). Irregular = elige el grupo él
// mismo entre todos los de su carrera/semestre. El caso "bloqueado por
// observación de reglamento" queda para la Fase 2: todavía no existe esa
// tabla, así que este endpoint nunca lo regresa por ahora.
app.get('/api/reinscripcion/estatus/:matricula', requireAlumno(), async (req, res) => {
  try {
    const matricula = String(req.params.matricula || '').trim();
    if (!matricula) return res.status(400).json({ ok: false, error: 'Matrícula requerida' });

    const db = await getPool();
    const alumnoLookup = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .query(`
        SELECT a.IdAlumno, a.NombreCompleto, a.Semestre, c.Nombre AS Carrera, c.Clave AS ClaveCarrera
        FROM dbo.Alumnos a
        LEFT JOIN dbo.CatalogoCarreras c ON c.IdCarrera = a.IdCarrera
        WHERE a.Matricula = @Matricula
      `);
    const alumno = alumnoLookup.recordset[0];
    if (!alumno) return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });
    if (!alumno.ClaveCarrera || !alumno.Semestre) {
      return res.status(409).json({ ok: false, error: 'El alumno no tiene carrera o semestre asignado todavía' });
    }

    // Fase 2: una observación de reglamento sin resolver bloquea la
    // reinscripción hasta que el director la autorice o la rechace — ver
    // 02_MODULOS/01_Reinscripcion.md.
    const bloqueo = await db.request()
      .input('IdAlumno', sql.Int, alumno.IdAlumno)
      .query(`
        SELECT TOP 1 IdObservacion, Motivo, FechaRegistro
        FROM dbo.ObservacionesReglamento
        WHERE IdAlumno = @IdAlumno AND Estado = N'PENDIENTE'
        ORDER BY FechaRegistro DESC
      `);
    const observacionPendiente = bloqueo.recordset[0];
    if (observacionPendiente) {
      return res.json({
        ok: true,
        alumno: {
          matricula, nombreCompleto: alumno.NombreCompleto, semestre: alumno.Semestre,
          carrera: alumno.Carrera, claveCarrera: alumno.ClaveCarrera
        },
        estatus: 'BLOQUEADO',
        motivo: observacionPendiente.Motivo,
        materiasPendientes: [],
        grupoAsignado: null,
        grupos: []
      });
    }

    const pendientes = await db.request()
      .input('IdAlumno', sql.Int, alumno.IdAlumno)
      .input('Clave', sql.NVarChar(30), alumno.ClaveCarrera)
      .input('Semestre', sql.TinyInt, alumno.Semestre)
      .query(`
        SELECT pm.IdMateria, pm.Nombre, pm.Creditos, pm.Semestre,
               es.Codigo AS EstatusCodigo
        FROM dbo.PlanEstudioMaterias pm
        JOIN dbo.CatalogoCarreras c ON c.IdCarrera = pm.IdCarrera AND c.Clave = @Clave
        LEFT JOIN dbo.HistorialAcademico h ON h.IdMateria = pm.IdMateria AND h.IdAlumno = @IdAlumno AND h.TipoRegistro = N'EVALUACION'
        LEFT JOIN dbo.CatalogoEstatusMateria es ON es.IdEstatus = h.IdEstatus
        WHERE pm.Activo = 1 AND pm.Semestre < @Semestre AND ISNULL(es.Codigo, N'PC') <> N'AP'
        ORDER BY pm.Semestre, pm.Nombre
      `);

    const esRegular = pendientes.recordset.length === 0;

    const gruposDelSemestre = await db.request()
      .input('Clave', sql.NVarChar(30), alumno.ClaveCarrera)
      .input('Semestre', sql.TinyInt, alumno.Semestre)
      .query(`
        SELECT g.IdGrupo, g.Clave, g.Semestre, t.Nombre AS Turno, g.Numero, p.Etiqueta AS Periodo
        FROM dbo.Grupos g
        JOIN dbo.CatalogoCarreras c ON c.IdCarrera = g.IdCarrera AND c.Clave = @Clave
        JOIN dbo.CatalogoTurnos t ON t.IdTurno = g.IdTurno
        JOIN dbo.CatalogoPeriodos p ON p.IdPeriodo = g.IdPeriodo
        WHERE g.Activo = 1 AND g.Semestre = @Semestre
        ORDER BY g.IdGrupo
      `);

    res.json({
      ok: true,
      alumno: {
        matricula, nombreCompleto: alumno.NombreCompleto, semestre: alumno.Semestre,
        carrera: alumno.Carrera, claveCarrera: alumno.ClaveCarrera
      },
      estatus: esRegular ? 'REGULAR' : 'IRREGULAR',
      materiasPendientes: pendientes.recordset,
      grupoAsignado: esRegular ? (gruposDelSemestre.recordset[0] || null) : null,
      grupos: esRegular ? [] : gruposDelSemestre.recordset
    });
  } catch (err) {
    console.error('[REINSCRIPCION][ESTATUS][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.post('/api/reinscripcion/solicitud', requireAlumno(), async (req, res) => {
  try {
    const matricula = String(req.body?.matricula || '').trim();
    const claveGrupo = String(req.body?.claveGrupo || '').trim();
    if (!matricula || !claveGrupo) {
      return res.status(400).json({ ok: false, error: 'Matrícula y grupo son obligatorios' });
    }

    const db = await getPool();
    const alumnoLookup = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .query(`SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = @Matricula`);
    const alumno = alumnoLookup.recordset[0];
    if (!alumno) return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });

    const bloqueo = await db.request()
      .input('IdAlumno', sql.Int, alumno.IdAlumno)
      .query(`SELECT TOP 1 1 FROM dbo.ObservacionesReglamento WHERE IdAlumno = @IdAlumno AND Estado = N'PENDIENTE'`);
    if (bloqueo.recordset[0]) {
      return res.status(409).json({ ok: false, error: 'Tienes una observación de reglamento pendiente. Preséntate con tu director de carrera.' });
    }

    const grupoLookup = await db.request()
      .input('Clave', sql.NVarChar(20), claveGrupo)
      .query(`SELECT IdGrupo FROM dbo.Grupos WHERE Clave = @Clave AND Activo = 1`);
    const grupo = grupoLookup.recordset[0];
    if (!grupo) return res.status(404).json({ ok: false, error: 'El grupo no existe o no está activo' });

    const folio = `RE-${crypto.randomBytes(4).toString('hex').toUpperCase()}`;

    await db.request()
      .input('IdAlumno', sql.Int, alumno.IdAlumno)
      .input('IdGrupo', sql.Int, grupo.IdGrupo)
      .input('Folio', sql.NVarChar(20), folio)
      .query(`
        INSERT INTO dbo.SolicitudesReinscripcion (IdAlumno, IdGrupo, Folio)
        VALUES (@IdAlumno, @IdGrupo, @Folio)
      `);

    console.log(`[REINSCRIPCION][SOLICITUD] matricula=${matricula} grupo=${claveGrupo} folio=${folio}`);
    res.json({ ok: true, folio });
  } catch (err) {
    console.error('[REINSCRIPCION][SOLICITUD][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// ─── Reinscripción — lado administrador (Fase 2) ────────────────────────────
// Lista de solicitudes que control escolar debe confirmar de manera
// presencial (ver instrucciones reales del SIIA: "LA INSCRIPCION SERA DE
// MANERA PRESENCIAL"). Sin filtro de estatus regresa todas; con
// ?estado=PENDIENTE solo las que faltan por procesar.
app.get('/api/reinscripcion/solicitudes', requireAdmin(), async (req, res) => {
  try {
    const estado = String(req.query.estado || '').trim().toUpperCase();
    const db = await getPool();
    const request = db.request();
    let filtroEstado = '';
    if (estado) {
      request.input('Estado', sql.NVarChar(20), estado);
      filtroEstado = 'WHERE s.Estatus = @Estado';
    }
    const result = await request.query(`
      SELECT s.IdSolicitud, s.Folio, s.Estatus, s.FechaSolicitud,
             a.Matricula, a.NombreCompleto, g.Clave AS ClaveGrupo, p.Etiqueta AS Periodo
      FROM dbo.SolicitudesReinscripcion s
      JOIN dbo.Alumnos a ON a.IdAlumno = s.IdAlumno
      JOIN dbo.Grupos g ON g.IdGrupo = s.IdGrupo
      JOIN dbo.CatalogoPeriodos p ON p.IdPeriodo = g.IdPeriodo
      ${filtroEstado}
      ORDER BY s.FechaSolicitud DESC
    `);
    res.json({ ok: true, data: result.recordset });
  } catch (err) {
    console.error('[REINSCRIPCION][SOLICITUDES][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.put('/api/reinscripcion/solicitudes/:id/confirmar', requireAdmin(), async (req, res) => {
  try {
    const id = Number(req.params.id);
    if (!Number.isInteger(id)) return res.status(400).json({ ok: false, error: 'Id inválido' });
    const db = await getPool();
    const result = await db.request().input('Id', sql.Int, id)
      .query(`UPDATE dbo.SolicitudesReinscripcion SET Estatus = N'CONFIRMADA' WHERE IdSolicitud = @Id`);
    if (result.rowsAffected[0] === 0) return res.status(404).json({ ok: false, error: 'Solicitud no encontrada' });
    console.log(`[REINSCRIPCION][CONFIRMAR] id=${id} actor=${req.admin.usuario}`);
    res.json({ ok: true });
  } catch (err) {
    console.error('[REINSCRIPCION][CONFIRMAR][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// ─── Observaciones de reglamento (Fase 2 de Reinscripción) ──────────────────
app.get('/api/observaciones/:matricula', requireAdmin(), async (req, res) => {
  try {
    const matricula = String(req.params.matricula || '').trim();
    const db = await getPool();
    const alumnoLookup = await db.request().input('Matricula', sql.NVarChar(20), matricula)
      .query(`SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = @Matricula`);
    const alumno = alumnoLookup.recordset[0];
    if (!alumno) return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });

    const result = await db.request().input('IdAlumno', sql.Int, alumno.IdAlumno).query(`
      SELECT IdObservacion, Motivo, Estado, RegistradaPor, FechaRegistro, ResueltaPor, FechaResolucion, Resolucion
      FROM dbo.ObservacionesReglamento
      WHERE IdAlumno = @IdAlumno
      ORDER BY FechaRegistro DESC
    `);
    res.json({ ok: true, data: result.recordset });
  } catch (err) {
    console.error('[OBSERVACIONES][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.post('/api/observaciones', requireAdmin(), async (req, res) => {
  try {
    const matricula = String(req.body?.matricula || '').trim();
    const motivo = String(req.body?.motivo || '').trim();
    if (!matricula || !motivo) {
      return res.status(400).json({ ok: false, error: 'Matrícula y motivo son obligatorios' });
    }
    const db = await getPool();
    const alumnoLookup = await db.request().input('Matricula', sql.NVarChar(20), matricula)
      .query(`SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = @Matricula`);
    const alumno = alumnoLookup.recordset[0];
    if (!alumno) return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });

    await db.request()
      .input('IdAlumno', sql.Int, alumno.IdAlumno)
      .input('Motivo', sql.NVarChar(500), motivo)
      .input('RegistradaPor', sql.NVarChar(50), req.admin.usuario)
      .query(`
        INSERT INTO dbo.ObservacionesReglamento (IdAlumno, Motivo, RegistradaPor)
        VALUES (@IdAlumno, @Motivo, @RegistradaPor)
      `);

    try {
      await ensureAuditTable();
      await writeAudit(
        { matricula: req.admin.usuario, nombre: req.admin.nombre },
        'Observación de reglamento registrada',
        `Matricula: ${matricula} · Motivo: ${motivo}`,
        matricula
      );
    } catch (auditError) {
      console.error('[AUDITORIA][ERROR]', auditError.message);
    }

    console.log(`[OBSERVACIONES][CREAR] matricula=${matricula} actor=${req.admin.usuario}`);
    res.status(201).json({ ok: true });
  } catch (err) {
    console.error('[OBSERVACIONES][CREAR][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.put('/api/observaciones/:id/resolver', requireAdmin(), async (req, res) => {
  try {
    const id = Number(req.params.id);
    const decision = String(req.body?.decision || '').trim().toUpperCase();
    const resolucion = req.body?.resolucion ? String(req.body.resolucion).trim() : null;
    if (!Number.isInteger(id)) return res.status(400).json({ ok: false, error: 'Id inválido' });
    if (!['AUTORIZADA', 'RECHAZADA'].includes(decision)) {
      return res.status(400).json({ ok: false, error: 'La decisión debe ser AUTORIZADA o RECHAZADA' });
    }

    const db = await getPool();
    const result = await db.request()
      .input('Id', sql.Int, id)
      .input('Estado', sql.NVarChar(20), decision)
      .input('ResueltaPor', sql.NVarChar(50), req.admin.usuario)
      .input('Resolucion', sql.NVarChar(500), resolucion)
      .query(`
        UPDATE dbo.ObservacionesReglamento
        SET Estado = @Estado, ResueltaPor = @ResueltaPor, FechaResolucion = SYSUTCDATETIME(), Resolucion = @Resolucion
        WHERE IdObservacion = @Id AND Estado = N'PENDIENTE'
      `);
    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({ ok: false, error: 'La observación no existe o ya fue resuelta' });
    }

    console.log(`[OBSERVACIONES][RESOLVER] id=${id} decision=${decision} actor=${req.admin.usuario}`);
    res.json({ ok: true });
  } catch (err) {
    console.error('[OBSERVACIONES][RESOLVER][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.post('/api/registro', async (req, res) => {
  try {
    const {
      matricula, nombres, apellidoPaterno, apellidoMaterno, fechaNacimiento,
      sistema, carrera, equivalencias, password
    } = req.body || {};
    const values = [matricula, nombres, apellidoPaterno, apellidoMaterno, fechaNacimiento, sistema, carrera, password];
    if (values.some((value) => typeof value !== 'string' || !value.trim())) {
      return res.status(400).json({ ok: false, error: 'Todos los campos obligatorios deben estar completos' });
    }
    if (!/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)[A-Za-z\d]{8,}$/.test(password)) {
      return res.status(400).json({ ok: false, error: 'La contraseña requiere 8 caracteres, mayúscula, minúscula y número, sin espacios ni símbolos' });
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(fechaNacimiento)) {
      return res.status(400).json({ ok: false, error: 'La fecha debe tener formato AAAA-MM-DD' });
    }
    const db = await getPool();
    const lookup = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula.trim())
      .input('Sistema', sql.NVarChar(30), sistema.trim())
      .input('Carrera', sql.NVarChar(30), carrera.trim())
      .query(`
        SELECT
          (SELECT IdSistema FROM dbo.CatalogoSistemas WHERE Clave = @Sistema AND Activo = 1) AS IdSistema,
          (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = @Carrera AND Activo = 1) AS IdCarrera,
          (SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = @Matricula) AS IdAlumnoExistente,
          (SELECT COUNT(*) FROM dbo.AlumnoCredenciales ac JOIN dbo.Alumnos a ON a.IdAlumno = ac.IdAlumno WHERE a.Matricula = @Matricula) AS YaTieneCredenciales
      `);
    const selected = lookup.recordset[0];
    if (selected.YaTieneCredenciales > 0) return res.status(409).json({ ok: false, error: 'La matrícula ya tiene una cuenta registrada' });
    if (!selected.IdSistema || !selected.IdCarrera) return res.status(400).json({ ok: false, error: 'El sistema o la carrera no existen en el catálogo' });

    const credentials = hashPassword(password);
    const correoInstitucional = `${matricula.trim().toLowerCase()}@teschi.edu.mx`;
    const nombreCompleto = `${nombres.trim()} ${apellidoPaterno.trim()} ${apellidoMaterno.trim()}`.trim();

    // Un alumno puede ya existir en Alumnos (perfil sincronizado desde el SIIA
    // sin contraseña todavía) — en ese caso solo se completan sus datos y se
    // agrega la credencial; si no existe, se crea desde cero. Así nunca hay
    // dos registros distintos para la misma matrícula (ver DEC-014).
    const request = db.request()
      .input('Matricula', sql.NVarChar(20), matricula.trim())
      .input('NombreCompleto', sql.NVarChar(200), nombreCompleto)
      .input('FechaNacimiento', sql.Date, new Date(`${fechaNacimiento}T00:00:00Z`))
      .input('CorreoInstitucional', sql.NVarChar(255), correoInstitucional)
      .input('IdSistema', sql.Int, selected.IdSistema)
      .input('IdCarrera', sql.Int, selected.IdCarrera)
      .input('Equivalencias', sql.Bit, Boolean(equivalencias))
      .input('ContrasenaHash', sql.VarBinary(256), credentials.hash)
      .input('ContrasenaSalt', sql.VarBinary(128), credentials.salt);

    if (selected.IdAlumnoExistente) {
      await request
        .input('IdAlumno', sql.Int, selected.IdAlumnoExistente)
        .query(`
          UPDATE dbo.Alumnos
          SET NombreCompleto = @NombreCompleto, FechaNacimiento = @FechaNacimiento,
              CorreoInstitucional = @CorreoInstitucional, IdSistema = @IdSistema,
              IdCarrera = @IdCarrera, Equivalencias = @Equivalencias
          WHERE IdAlumno = @IdAlumno;
          INSERT INTO dbo.AlumnoCredenciales (IdAlumno, ContrasenaHash, ContrasenaSalt)
          VALUES (@IdAlumno, @ContrasenaHash, @ContrasenaSalt);
        `);
    } else {
      await request.query(`
        DECLARE @NuevoId TABLE (IdAlumno INT);
        INSERT INTO dbo.Alumnos (Matricula, NombreCompleto, FechaNacimiento, CorreoInstitucional, IdSistema, IdCarrera, Equivalencias)
        OUTPUT INSERTED.IdAlumno INTO @NuevoId
        VALUES (@Matricula, @NombreCompleto, @FechaNacimiento, @CorreoInstitucional, @IdSistema, @IdCarrera, @Equivalencias);
        INSERT INTO dbo.AlumnoCredenciales (IdAlumno, ContrasenaHash, ContrasenaSalt)
        SELECT IdAlumno, @ContrasenaHash, @ContrasenaSalt FROM @NuevoId;
      `);
    }
    console.log(`[REGISTRO][OK] matricula=${matricula.trim()} correo=${correoInstitucional}`);
    res.status(201).json({ ok: true, mensaje: 'Cuenta creada correctamente', matricula: matricula.trim(), correoInstitucional });
  } catch (err) {
    console.error('[REGISTRO][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.post('/api/auth/cuenta', async (req, res) => {
  try {
    const matricula = String(req.body?.matricula || '').trim();
    const password = String(req.body?.password || '');
    if (!matricula || !password) return res.status(400).json({ ok: false, error: 'Matrícula y contraseña son obligatorias' });
    if (excedeLimite(`${clientIp(req)}:auth-cuenta:${matricula}`, 8, 5 * 60 * 1000)) {
      return res.status(429).json({ ok: false, error: 'Demasiados intentos. Espera unos minutos e inténtalo de nuevo.' });
    }
    const result = await (await getPool()).request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .query(`SELECT TOP 1 a.Matricula, a.NombreCompleto, ac.ContrasenaHash, ac.ContrasenaSalt
              FROM dbo.Alumnos a
              JOIN dbo.AlumnoCredenciales ac ON ac.IdAlumno = a.IdAlumno
              WHERE a.Matricula = @Matricula AND ac.Estado = N'REGISTRADO'`);
    const account = result.recordset[0];
    if (!account || !verifyPassword(password, account.ContrasenaHash, account.ContrasenaSalt)) {
      console.error(`[AUTH][RECHAZO] matricula=${matricula}`);
      return res.status(401).json({ ok: false, error: 'Matrícula o contraseña incorrectas' });
    }
    console.log(`[AUTH][OK] matricula=${matricula}`);
    res.json({
      ok: true, matricula: account.Matricula, nombre: account.NombreCompleto, esAdministrador: false,
      ticket: firmarTicketLogin('ALUMNO', account.Matricula)
    });
  } catch (err) {
    console.error('[AUTH][ERROR]', err.message);
    res.status(500).json({ ok: false, error: 'No se pudo validar la cuenta' });
  }
});

// ─── Autenticación de administrador (cuenta real, ver DEC-019) ──────────────
app.post('/api/auth/administrador', async (req, res) => {
  try {
    const usuario = String(req.body?.usuario || '').trim();
    const password = String(req.body?.password || '');
    if (!usuario || !password) return res.status(400).json({ ok: false, error: 'Usuario y contraseña son obligatorios' });
    if (excedeLimite(`${clientIp(req)}:auth-admin:${usuario}`, 8, 5 * 60 * 1000)) {
      return res.status(429).json({ ok: false, error: 'Demasiados intentos. Espera unos minutos e inténtalo de nuevo.' });
    }
    const result = await (await getPool()).request()
      .input('Usuario', sql.NVarChar(50), usuario)
      .query(`SELECT TOP 1 a.IdAdministrador, a.Usuario, a.NombreCompleto, a.Rol, ac.ContrasenaHash, ac.ContrasenaSalt
              FROM dbo.Administradores a
              JOIN dbo.AdministradorCredenciales ac ON ac.IdAdministrador = a.IdAdministrador
              WHERE a.Usuario = @Usuario AND a.Activo = 1 AND ac.Estado = N'REGISTRADO'`);
    const cuenta = result.recordset[0];
    if (!cuenta || !verifyPassword(password, cuenta.ContrasenaHash, cuenta.ContrasenaSalt)) {
      console.error(`[AUTH-ADMIN][RECHAZO] usuario=${usuario}`);
      return res.status(401).json({ ok: false, error: 'Usuario o contraseña incorrectos' });
    }
    const db = await getPool();
    await db.request().input('Id', sql.Int, cuenta.IdAdministrador)
      .query(`UPDATE dbo.Administradores SET UltimoAcceso = SYSUTCDATETIME() WHERE IdAdministrador = @Id`);
    console.log(`[AUTH-ADMIN][OK] usuario=${usuario} rol=${cuenta.Rol}`);
    res.json({
      ok: true, usuario: cuenta.Usuario, nombre: cuenta.NombreCompleto, rol: cuenta.Rol, esAdministrador: true,
      ticket: firmarTicketLogin('ADMINISTRADOR', cuenta.Usuario)
    });
  } catch (err) {
    console.error('[AUTH-ADMIN][ERROR]', err.message);
    res.status(500).json({ ok: false, error: 'No se pudo validar la cuenta' });
  }
});

// ─── OTP — generación y verificación 100% en el servidor (ver DEC-019) ──────
// tipo: "ALUMNO" (default) o "ADMINISTRADOR". El código nunca vive en el
// cliente — la app solo sabe si el backend dice que es correcto o no.
app.post('/api/otp/enviar', async (req, res) => {
  try {
    const tipo = String(req.body?.tipo || 'ALUMNO').toUpperCase();
    const identificador = String(req.body?.matricula || req.body?.usuario || '').trim();
    const correo = String(req.body?.correo || '').trim().toLowerCase();
    if (!identificador || !correo || !correo.includes('@')) {
      return res.status(400).json({ ok: false, error: 'Matrícula/usuario y correo son obligatorios' });
    }
    // Sin el ticket que emite /api/auth/* al validar la contraseña, cualquiera
    // podría pedir el código a un correo propio para la cuenta de otro.
    if (!ticketLoginValido(req.body?.ticket, tipo === 'ADMINISTRADOR' ? 'ADMINISTRADOR' : 'ALUMNO', identificador)) {
      console.warn(`[OTP][SIN-TICKET] envio tipo=${tipo} id=${identificador.slice(0, 20)}`);
      return res.status(401).json({ ok: false, error: 'Tu inicio de sesión expiró. Vuelve a ingresar tu usuario y contraseña.' });
    }
    if (excedeLimite(`${clientIp(req)}:otp-enviar:${identificador}`, 5, 10 * 60 * 1000)) {
      return res.status(429).json({ ok: false, error: 'Demasiadas solicitudes de código. Espera unos minutos.' });
    }

    const db = await getPool();
    let idAlumno = null;
    let idAdministrador = null;
    if (tipo === 'ADMINISTRADOR') {
      const r = await db.request().input('Usuario', sql.NVarChar(50), identificador)
        .query(`SELECT IdAdministrador FROM dbo.Administradores WHERE Usuario = @Usuario AND Activo = 1`);
      if (!r.recordset[0]) return res.status(404).json({ ok: false, error: 'Administrador no encontrado' });
      idAdministrador = r.recordset[0].IdAdministrador;
    } else {
      const r = await db.request().input('Matricula', sql.NVarChar(20), identificador)
        .query(`SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = @Matricula`);
      if (!r.recordset[0]) return res.status(404).json({ ok: false, error: 'Alumno no encontrado' });
      idAlumno = r.recordset[0].IdAlumno;
    }

    const codigo = String(crypto.randomInt(100000, 1000000));
    const codigoHash = crypto.createHash('sha256').update(codigo).digest('hex');
    const expiraEn = new Date(Date.now() + OTP_VALIDEZ_MS);

    await db.request()
      .input('IdAlumno', sql.Int, idAlumno)
      .input('IdAdministrador', sql.Int, idAdministrador)
      .input('CorreoDestino', sql.NVarChar(255), correo)
      .input('CodigoHash', sql.Char(64), codigoHash)
      .input('ExpiraEn', sql.DateTime2, expiraEn)
      .query(`INSERT INTO dbo.OtpHistorial (IdAlumno, IdAdministrador, CorreoDestino, CodigoHash, ExpiraEn)
              VALUES (@IdAlumno, @IdAdministrador, @CorreoDestino, @CodigoHash, @ExpiraEn)`);

    await enviarCorreoOtp(correo, codigo);
    console.log(`[OTP][ENVIADO] tipo=${tipo} id=${identificador}`);
    res.json({ ok: true });
  } catch (err) {
    console.error('[OTP][ENVIAR][ERROR]', err.message);
    res.status(500).json({ ok: false, error: 'No se pudo enviar el código. Verifica el correo e inténtalo de nuevo.' });
  }
});

app.post('/api/otp/verificar', async (req, res) => {
  try {
    const tipo = String(req.body?.tipo || 'ALUMNO').toUpperCase();
    const identificador = String(req.body?.matricula || req.body?.usuario || '').trim();
    const codigo = String(req.body?.codigo || '').trim();
    if (!identificador || !/^\d{6}$/.test(codigo)) {
      return res.status(400).json({ ok: false, error: 'Código inválido' });
    }
    if (!ticketLoginValido(req.body?.ticket, tipo === 'ADMINISTRADOR' ? 'ADMINISTRADOR' : 'ALUMNO', identificador)) {
      console.warn(`[OTP][SIN-TICKET] verificacion tipo=${tipo} id=${identificador.slice(0, 20)}`);
      return res.status(401).json({ ok: false, error: 'Tu inicio de sesión expiró. Vuelve a ingresar tu usuario y contraseña.' });
    }
    if (excedeLimite(`${clientIp(req)}:otp-verificar:${identificador}`, 10, 10 * 60 * 1000)) {
      return res.status(429).json({ ok: false, error: 'Demasiados intentos. Solicita un nuevo código.' });
    }

    const db = await getPool();
    const columna = tipo === 'ADMINISTRADOR' ? 'IdAdministrador' : 'IdAlumno';
    const idLookup = tipo === 'ADMINISTRADOR'
      ? await db.request().input('U', sql.NVarChar(50), identificador)
          .query(`SELECT IdAdministrador AS Id FROM dbo.Administradores WHERE Usuario = @U`)
      : await db.request().input('M', sql.NVarChar(20), identificador)
          .query(`SELECT IdAlumno AS Id FROM dbo.Alumnos WHERE Matricula = @M`);
    const id = idLookup.recordset[0]?.Id;
    if (!id) return res.status(404).json({ ok: false, error: 'Cuenta no encontrada' });

    const otpResult = await db.request().input('Id', sql.Int, id).query(`
      SELECT TOP 1 IdOtp, CodigoHash, ExpiraEn, Estado, Intentos
      FROM dbo.OtpHistorial WHERE ${columna} = @Id ORDER BY EnviadoEn DESC
    `);
    const otp = otpResult.recordset[0];
    // PENDIENTE (primer uso) o VERIFICADO (ya se usó con éxito antes) son
    // ambos re-verificables mientras no haya expirado: si la app se cierra
    // por algo externo justo después de un login exitoso, el alumno puede
    // volver a escribir el mismo código sin pedir un correo nuevo, en vez de
    // que quede inservible tras el primer uso (decisión explícita del
    // alumno — antes era estrictamente de un solo uso).
    if (!otp || (otp.Estado !== 'PENDIENTE' && otp.Estado !== 'VERIFICADO')) {
      return res.status(400).json({ ok: false, error: 'No hay un código pendiente. Solicita uno nuevo.' });
    }
    if (new Date(otp.ExpiraEn) < new Date()) {
      await db.request().input('Id', sql.BigInt, otp.IdOtp).query(`UPDATE dbo.OtpHistorial SET Estado = N'EXPIRADO' WHERE IdOtp = @Id`);
      return res.status(400).json({ ok: false, error: 'El código expiró. Solicita uno nuevo.' });
    }
    if (otp.Intentos >= OTP_MAX_INTENTOS) {
      await db.request().input('Id', sql.BigInt, otp.IdOtp).query(`UPDATE dbo.OtpHistorial SET Estado = N'FALLIDO' WHERE IdOtp = @Id`);
      return res.status(429).json({ ok: false, error: 'Demasiados intentos fallidos. Solicita un nuevo código.' });
    }

    const coincide = crypto.createHash('sha256').update(codigo).digest('hex') === otp.CodigoHash;
    if (coincide) {
      await db.request().input('Id', sql.BigInt, otp.IdOtp)
        .query(`UPDATE dbo.OtpHistorial SET Estado = N'VERIFICADO', VerificadoEn = SYSUTCDATETIME() WHERE IdOtp = @Id`);
      console.log(`[OTP][OK] tipo=${tipo} id=${identificador}`);

      // Administrador y alumno reciben su token de sesión (DEC-020 / DEC-030):
      // es lo que identifica quién es en cada endpoint protegido.
      let token = null;
      if (tipo === 'ADMINISTRADOR') {
        const adminRow = await db.request().input('Id', sql.Int, id)
          .query(`SELECT IdAdministrador, Usuario, NombreCompleto, Rol FROM dbo.Administradores WHERE IdAdministrador = @Id`);
        const admin = adminRow.recordset[0];
        if (admin) {
          token = firmarTokenAdministrador(admin);
          await db.request().input('Id', sql.Int, admin.IdAdministrador)
            .query(`UPDATE dbo.Administradores SET UltimoAcceso = SYSUTCDATETIME() WHERE IdAdministrador = @Id`);
        }
      } else {
        const alumnoRow = await db.request().input('Id', sql.Int, id)
          .query(`SELECT IdAlumno, Matricula FROM dbo.Alumnos WHERE IdAlumno = @Id`);
        const alumno = alumnoRow.recordset[0];
        if (alumno) {
          token = firmarTokenAlumno(alumno);
          await db.request().input('Id', sql.Int, alumno.IdAlumno)
            .query(`UPDATE dbo.Alumnos SET UltimoAcceso = SYSUTCDATETIME() WHERE IdAlumno = @Id`);
        }
      }
      res.json({ ok: true, token });
    } else {
      await db.request().input('Id', sql.BigInt, otp.IdOtp).query(`UPDATE dbo.OtpHistorial SET Intentos = Intentos + 1 WHERE IdOtp = @Id`);
      console.warn(`[OTP][RECHAZO] tipo=${tipo} id=${identificador}`);
      res.status(401).json({ ok: false, error: 'Código incorrecto', intentosRestantes: Math.max(0, OTP_MAX_INTENTOS - otp.Intentos - 1) });
    }
  } catch (err) {
    console.error('[OTP][VERIFICAR][ERROR]', err.message);
    res.status(500).json({ ok: false, error: 'No se pudo verificar el código' });
  }
});

// ─── Recuperar contraseña (solo cuenta propia, ver DEC-025) ─────────────────
// A diferencia del OTP de login, aquí el correo NUNCA lo escribe quien hace
// la solicitud — se manda siempre al CorreoOtp que ya está guardado para esa
// matrícula. Si se dejara escribir el correo libremente, cualquiera que
// supiera una matrícula ajena podría robar esa cuenta con su propio correo.
function enmascararCorreo(correo) {
  const [usuario, dominio] = String(correo).split('@');
  if (!dominio || usuario.length <= 2) return correo;
  const visible = usuario[0] + '*'.repeat(Math.max(1, usuario.length - 2)) + usuario[usuario.length - 1];
  return `${visible}@${dominio}`;
}

app.post('/api/recuperar-password/solicitar', async (req, res) => {
  try {
    const matricula = String(req.body?.matricula || '').trim();
    if (!matricula) return res.status(400).json({ ok: false, error: 'Matrícula requerida' });
    if (excedeLimite(`${clientIp(req)}:recuperar-solicitar:${matricula}`, 5, 10 * 60 * 1000)) {
      return res.status(429).json({ ok: false, error: 'Demasiadas solicitudes. Espera unos minutos.' });
    }

    const db = await getPool();
    const lookup = await db.request().input('Matricula', sql.NVarChar(20), matricula).query(`
      SELECT a.IdAlumno, a.CorreoOtp
      FROM dbo.Alumnos a
      JOIN dbo.AlumnoCredenciales ac ON ac.IdAlumno = a.IdAlumno AND ac.Estado = N'REGISTRADO'
      WHERE a.Matricula = @Matricula
    `);
    const alumno = lookup.recordset[0];
    if (!alumno) {
      return res.status(404).json({ ok: false, error: 'Esa matrícula no tiene una cuenta propia registrada.' });
    }
    const correo = String(alumno.CorreoOtp || '').trim();
    if (!correo) {
      return res.status(409).json({ ok: false, error: 'No hay un correo registrado para esta cuenta. Contacta a control escolar.' });
    }

    const codigo = String(crypto.randomInt(100000, 1000000));
    const codigoHash = crypto.createHash('sha256').update(codigo).digest('hex');
    const expiraEn = new Date(Date.now() + OTP_VALIDEZ_MS);
    await db.request()
      .input('IdAlumno', sql.Int, alumno.IdAlumno)
      .input('CorreoDestino', sql.NVarChar(255), correo)
      .input('CodigoHash', sql.Char(64), codigoHash)
      .input('ExpiraEn', sql.DateTime2, expiraEn)
      .query(`
        INSERT INTO dbo.OtpHistorial (IdAlumno, CorreoDestino, CodigoHash, ExpiraEn)
        VALUES (@IdAlumno, @CorreoDestino, @CodigoHash, @ExpiraEn)
      `);
    await enviarCorreoOtp(correo, codigo);

    console.log(`[RECUPERAR][SOLICITAR] matricula=${matricula}`);
    res.json({ ok: true, correo: enmascararCorreo(correo) });
  } catch (err) {
    console.error('[RECUPERAR][SOLICITAR][ERROR]', err.message);
    res.status(500).json({ ok: false, error: 'No se pudo enviar el código de recuperación' });
  }
});

app.post('/api/recuperar-password/confirmar', async (req, res) => {
  try {
    const matricula = String(req.body?.matricula || '').trim();
    const codigo = String(req.body?.codigo || '').trim();
    const nuevaPassword = String(req.body?.nuevaPassword || '');
    if (!matricula || !/^\d{6}$/.test(codigo)) {
      return res.status(400).json({ ok: false, error: 'Código inválido' });
    }
    if (!/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)[A-Za-z\d]{8,}$/.test(nuevaPassword)) {
      return res.status(400).json({ ok: false, error: 'La contraseña requiere 8 caracteres, mayúscula, minúscula y número, sin espacios ni símbolos' });
    }
    if (excedeLimite(`${clientIp(req)}:recuperar-confirmar:${matricula}`, 10, 10 * 60 * 1000)) {
      return res.status(429).json({ ok: false, error: 'Demasiados intentos. Solicita un nuevo código.' });
    }

    const db = await getPool();
    const lookup = await db.request().input('Matricula', sql.NVarChar(20), matricula)
      .query(`SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = @Matricula`);
    const alumno = lookup.recordset[0];
    if (!alumno) return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });

    const otpResult = await db.request().input('IdAlumno', sql.Int, alumno.IdAlumno).query(`
      SELECT TOP 1 IdOtp, CodigoHash, ExpiraEn, Estado, Intentos
      FROM dbo.OtpHistorial WHERE IdAlumno = @IdAlumno ORDER BY EnviadoEn DESC
    `);
    const otp = otpResult.recordset[0];
    if (!otp || (otp.Estado !== 'PENDIENTE' && otp.Estado !== 'VERIFICADO')) {
      return res.status(400).json({ ok: false, error: 'No hay un código pendiente. Solicita uno nuevo.' });
    }
    if (new Date(otp.ExpiraEn) < new Date()) {
      await db.request().input('Id', sql.BigInt, otp.IdOtp).query(`UPDATE dbo.OtpHistorial SET Estado = N'EXPIRADO' WHERE IdOtp = @Id`);
      return res.status(400).json({ ok: false, error: 'El código expiró. Solicita uno nuevo.' });
    }
    if (otp.Intentos >= OTP_MAX_INTENTOS) {
      await db.request().input('Id', sql.BigInt, otp.IdOtp).query(`UPDATE dbo.OtpHistorial SET Estado = N'FALLIDO' WHERE IdOtp = @Id`);
      return res.status(429).json({ ok: false, error: 'Demasiados intentos fallidos. Solicita un nuevo código.' });
    }

    const coincide = crypto.createHash('sha256').update(codigo).digest('hex') === otp.CodigoHash;
    if (!coincide) {
      await db.request().input('Id', sql.BigInt, otp.IdOtp).query(`UPDATE dbo.OtpHistorial SET Intentos = Intentos + 1 WHERE IdOtp = @Id`);
      console.warn(`[RECUPERAR][RECHAZO] matricula=${matricula}`);
      return res.status(401).json({ ok: false, error: 'Código incorrecto', intentosRestantes: Math.max(0, OTP_MAX_INTENTOS - otp.Intentos - 1) });
    }

    const credenciales = hashPassword(nuevaPassword);
    await db.request()
      .input('IdAlumno', sql.Int, alumno.IdAlumno)
      .input('Hash', sql.VarBinary(256), credenciales.hash)
      .input('Salt', sql.VarBinary(128), credenciales.salt)
      .query(`
        UPDATE dbo.AlumnoCredenciales
        SET ContrasenaHash = @Hash, ContrasenaSalt = @Salt, Estado = N'REGISTRADO', FechaActualizacion = SYSUTCDATETIME()
        WHERE IdAlumno = @IdAlumno
      `);
    await db.request().input('Id', sql.BigInt, otp.IdOtp)
      .query(`UPDATE dbo.OtpHistorial SET Estado = N'VERIFICADO', VerificadoEn = SYSUTCDATETIME() WHERE IdOtp = @Id`);

    try {
      await ensureAuditTable();
      await writeAudit({ matricula, nombre: matricula }, 'Contraseña recuperada', 'El alumno restableció su contraseña vía recuperación de cuenta', matricula);
    } catch (auditError) {
      console.error('[AUDITORIA][ERROR]', auditError.message);
    }

    console.log(`[RECUPERAR][OK] matricula=${matricula}`);
    res.json({ ok: true });
  } catch (err) {
    console.error('[RECUPERAR][CONFIRMAR][ERROR]', err.message);
    res.status(500).json({ ok: false, error: 'No se pudo restablecer la contraseña' });
  }
});

app.get('/api/usuarios', requireAdmin(), async (req, res) => {
  try {
    const db = await getPool();
    // IdUsuario/Carrera se mantienen como alias en el JSON de salida: la app
    // Android (AdminUsersService.kt) ya espera esos nombres de campo — el
    // cambio de esquema queda contenido en el backend, sin tocar el contrato
    // con el cliente. ClaveCarrera y Semestre son campos nuevos (aditivos).
    const result = await db.request().query(`
      SELECT a.IdAlumno AS IdUsuario, a.Matricula, a.NombreCompleto, a.CorreoOtp, a.CorreoInstitucional,
             c.Nombre AS Carrera, c.Clave AS ClaveCarrera, a.Semestre, a.Activo, a.FechaRegistro, a.UltimoAcceso,
             CASE WHEN ac.IdAlumno IS NULL THEN 0 ELSE 1 END AS TieneCuentaLocal
      FROM dbo.Alumnos a
      LEFT JOIN dbo.CatalogoCarreras c ON c.IdCarrera = a.IdCarrera
      LEFT JOIN dbo.AlumnoCredenciales ac ON ac.IdAlumno = a.IdAlumno
      ORDER BY a.FechaRegistro DESC
    `);

    res.json({ ok: true, data: result.recordset });
  } catch (err) {
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Asignar/actualizar el semestre de un alumno. Se usa desde AdminProfileScreen
// para poder calcular qué materias le corresponden según PlanEstudioMaterias.
app.put('/api/usuarios/:matricula/semestre', requireAdmin(), async (req, res) => {
  try {
    const matricula = String(req.params.matricula || '').trim();
    const semestre = Number(req.body?.semestre);
    if (!matricula) return res.status(400).json({ ok: false, error: 'Matrícula requerida' });
    if (!Number.isInteger(semestre) || semestre < 1 || semestre > 12) {
      return res.status(400).json({ ok: false, error: 'El semestre debe ser un número entre 1 y 12' });
    }

    const db = await getPool();
    const result = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .input('Semestre', sql.TinyInt, semestre)
      .query(`
        UPDATE dbo.Alumnos SET Semestre = @Semestre WHERE Matricula = @Matricula;
        SELECT @@ROWCOUNT AS Afectados;
      `);
    if (result.recordset[0].Afectados === 0) {
      return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });
    }

    const actor = { matricula: req.admin.usuario, nombre: req.admin.nombre };
    try {
      await ensureAuditTable();
      await writeAudit(actor, 'Semestre actualizado', `Matricula: ${matricula} -> Semestre ${semestre}`, matricula);
    } catch (auditError) {
      console.error('[AUDITORIA][ERROR]', auditError.message);
    }

    console.log(`[SEMESTRE][OK] matricula=${matricula} semestre=${semestre} actor=${actor.matricula}`);
    res.json({ ok: true, matricula, semestre });
  } catch (err) {
    console.error('[SEMESTRE][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// matriculaAfectada (opcional) resuelve IdAlumno por subquery — así cada
// movimiento queda enlazado al perfil del alumno sobre el que se actuó,
// distinto del ActorMatricula (quién hizo el movimiento). Ver DEC-017.
async function writeAudit(actor, action, detail, matriculaAfectada = null) {
  const db = await getPool();
  await db.request()
    .input('ActorMatricula', sql.NVarChar(50), actor.matricula)
    .input('ActorNombre', sql.NVarChar(200), actor.nombre)
    .input('Accion', sql.NVarChar(80), action)
    .input('Entidad', sql.NVarChar(80), 'Usuarios')
    .input('Detalle', sql.NVarChar(1000), detail)
    .input('MatriculaAfectada', sql.NVarChar(20), matriculaAfectada)
    .query(`
      INSERT INTO dbo.AuditoriaMovimientos (ActorMatricula, ActorNombre, Accion, Entidad, Detalle, IdAlumno)
      VALUES (@ActorMatricula, @ActorNombre, @Accion, @Entidad, @Detalle,
        (SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = @MatriculaAfectada))
    `);
}

app.get('/api/auditoria', requireAdmin(), async (req, res) => {
  try {
    await ensureAuditTable();
    const matricula = String(req.query.matricula || '').trim();
    const limit = Math.min(Math.max(Number(req.query.limit) || 100, 1), 500);
    const db = await getPool();
    const request = db.request();
    let where = '';
    if (matricula) {
      request.input('Matricula', sql.NVarChar(20), matricula);
      where = 'WHERE au.ActorMatricula = @Matricula OR a.Matricula = @Matricula';
    }
    const result = await request.query(`
      SELECT TOP ${limit} au.IdAuditoria, au.ActorMatricula, au.ActorNombre, au.Accion, au.Entidad, au.Detalle,
             au.FechaMovimiento, a.Matricula AS PerfilMatricula
      FROM dbo.AuditoriaMovimientos au
      LEFT JOIN dbo.Alumnos a ON a.IdAlumno = au.IdAlumno
      ${where}
      ORDER BY au.FechaMovimiento DESC
    `);
    res.json({ ok: true, data: result.recordset });
  } catch (err) {
    console.error('[AUDITORIA][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// ─── Alta de alumno por administrador (sin contraseña — el alumno la crea al
// registrarse con /api/registro, que ya sabe completar un perfil existente) ──
app.post('/api/usuarios', requireAdmin(), async (req, res) => {
  try {
    const matricula = String(req.body?.matricula || '').trim();
    const nombreCompleto = String(req.body?.nombreCompleto || '').trim();
    const claveCarrera = String(req.body?.claveCarrera || '').trim();
    const correoOtp = String(req.body?.correoOtp || '').trim();
    const correoInstitucionalRaw = String(req.body?.correoInstitucional || '').trim();
    const semestreRaw = req.body?.semestre;
    const semestre = (semestreRaw === undefined || semestreRaw === null || semestreRaw === '') ? null : Number(semestreRaw);

    if (!matricula || !nombreCompleto || !claveCarrera) {
      return res.status(400).json({ ok: false, error: 'Matrícula, nombre completo y carrera son obligatorios' });
    }
    if (semestre !== null && (!Number.isInteger(semestre) || semestre < 1 || semestre > 12)) {
      return res.status(400).json({ ok: false, error: 'El semestre debe ser un número entre 1 y 12' });
    }

    const db = await getPool();
    const lookup = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .input('Clave', sql.NVarChar(30), claveCarrera)
      .query(`
        SELECT
          (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = @Clave AND Activo = 1) AS IdCarrera,
          (SELECT COUNT(*) FROM dbo.Alumnos WHERE Matricula = @Matricula) AS Existe
      `);
    const row = lookup.recordset[0];
    if (!row.IdCarrera) return res.status(400).json({ ok: false, error: 'La carrera no existe en el catálogo' });
    if (row.Existe > 0) return res.status(409).json({ ok: false, error: 'Ya existe un alumno con esa matrícula' });

    const correoInstitucional = correoInstitucionalRaw || `${matricula.toLowerCase()}@teschi.edu.mx`;

    await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .input('NombreCompleto', sql.NVarChar(200), nombreCompleto)
      .input('CorreoOtp', sql.NVarChar(255), correoOtp || null)
      .input('CorreoInstitucional', sql.NVarChar(255), correoInstitucional)
      .input('IdCarrera', sql.Int, row.IdCarrera)
      .input('Semestre', sql.TinyInt, semestre)
      .query(`
        INSERT INTO dbo.Alumnos (Matricula, NombreCompleto, CorreoOtp, CorreoInstitucional, IdCarrera, Semestre)
        VALUES (@Matricula, @NombreCompleto, @CorreoOtp, @CorreoInstitucional, @IdCarrera, @Semestre);
      `);

    const actor = { matricula: req.admin.usuario, nombre: req.admin.nombre };
    try {
      await ensureAuditTable();
      await writeAudit(actor, 'Alumno creado', `Matricula: ${matricula} · ${nombreCompleto}`, matricula);
    } catch (auditError) {
      console.error('[AUDITORIA][ERROR]', auditError.message);
    }

    console.log(`[ALTA][OK] matricula=${matricula} actor=${actor.matricula}`);
    res.status(201).json({ ok: true, matricula });
  } catch (err) {
    console.error('[ALTA][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// ─── Edición de perfil por administrador — actualiza solo los campos que
// llegan en el body (permite ediciones parciales desde la app) ──────────────
app.put('/api/usuarios/:matricula', requireAdmin(), async (req, res) => {
  try {
    const matricula = String(req.params.matricula || '').trim();
    if (!matricula) return res.status(400).json({ ok: false, error: 'Matrícula requerida' });

    const body = req.body || {};
    const db = await getPool();
    const request = db.request().input('Matricula', sql.NVarChar(20), matricula);
    const sets = [];

    if (body.nombreCompleto !== undefined) {
      const v = String(body.nombreCompleto).trim();
      if (!v) return res.status(400).json({ ok: false, error: 'El nombre completo no puede quedar vacío' });
      request.input('NombreCompleto', sql.NVarChar(200), v);
      sets.push('NombreCompleto = @NombreCompleto');
    }
    if (body.correoOtp !== undefined) {
      request.input('CorreoOtp', sql.NVarChar(255), String(body.correoOtp).trim() || null);
      sets.push('CorreoOtp = @CorreoOtp');
    }
    if (body.correoInstitucional !== undefined) {
      request.input('CorreoInstitucional', sql.NVarChar(255), String(body.correoInstitucional).trim() || null);
      sets.push('CorreoInstitucional = @CorreoInstitucional');
    }
    if (body.activo !== undefined) {
      request.input('Activo', sql.Bit, Boolean(body.activo));
      sets.push('Activo = @Activo');
    }
    if (body.semestre !== undefined) {
      const semestre = body.semestre === null ? null : Number(body.semestre);
      if (semestre !== null && (!Number.isInteger(semestre) || semestre < 1 || semestre > 12)) {
        return res.status(400).json({ ok: false, error: 'El semestre debe ser un número entre 1 y 12' });
      }
      // Un alumno nunca retrocede de semestre — solo puede avanzar (ver DEC-018).
      if (semestre !== null) {
        const actual = await db.request()
          .input('MatriculaActual', sql.NVarChar(20), matricula)
          .query(`SELECT Semestre FROM dbo.Alumnos WHERE Matricula = @MatriculaActual`);
        const semestreActual = actual.recordset[0]?.Semestre;
        if (semestreActual != null && semestre <= semestreActual) {
          return res.status(400).json({ ok: false, error: `El semestre no puede retroceder (actual: ${semestreActual})` });
        }
      }
      request.input('Semestre', sql.TinyInt, semestre);
      sets.push('Semestre = @Semestre');
    }
    if (body.claveCarrera !== undefined) {
      const clave = String(body.claveCarrera).trim();
      const carreraLookup = await db.request()
        .input('Clave', sql.NVarChar(30), clave)
        .query(`SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = @Clave AND Activo = 1`);
      if (!carreraLookup.recordset[0]) {
        return res.status(400).json({ ok: false, error: 'La carrera no existe en el catálogo' });
      }
      request.input('IdCarrera', sql.Int, carreraLookup.recordset[0].IdCarrera);
      sets.push('IdCarrera = @IdCarrera');
    }

    if (sets.length === 0) {
      return res.status(400).json({ ok: false, error: 'No se envió ningún campo para actualizar' });
    }

    const result = await request.query(`
      UPDATE dbo.Alumnos SET ${sets.join(', ')} WHERE Matricula = @Matricula;
      SELECT @@ROWCOUNT AS Afectados;
    `);
    if (result.recordset[0].Afectados === 0) {
      return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });
    }

    const actor = { matricula: req.admin.usuario, nombre: req.admin.nombre };
    try {
      await ensureAuditTable();
      await writeAudit(actor, 'Perfil actualizado', `Matricula: ${matricula} -> campos: ${Object.keys(body).join(', ')}`, matricula);
    } catch (auditError) {
      console.error('[AUDITORIA][ERROR]', auditError.message);
    }

    console.log(`[PERFIL][OK] matricula=${matricula} actor=${actor.matricula}`);
    res.json({ ok: true, matricula });
  } catch (err) {
    console.error('[PERFIL][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// ─── Baja de alumno por administrador — elimina en cascada lo que no tiene
// ON DELETE CASCADE propio (AlumnoCredenciales sí lo tiene). La auditoría
// del alumno eliminado se conserva (queda con IdAlumno = NULL) para no
// perder el historial de movimientos aunque el perfil ya no exista. ─────────
// Solo SUPERADMIN — es irreversible y borra historial académico completo.
app.delete('/api/usuarios/:matricula', requireAdmin(['SUPERADMIN']), async (req, res) => {
  try {
    const matricula = String(req.params.matricula || '').trim();
    if (!matricula) return res.status(400).json({ ok: false, error: 'Matrícula requerida' });

    const db = await getPool();
    const lookup = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .query(`SELECT IdAlumno, NombreCompleto FROM dbo.Alumnos WHERE Matricula = @Matricula`);
    const alumno = lookup.recordset[0];
    if (!alumno) return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });

    const transaction = db.transaction();
    await transaction.begin();
    try {
      const scoped = () => transaction.request().input('IdAlumno', sql.Int, alumno.IdAlumno);
      await scoped().query('UPDATE dbo.AuditoriaMovimientos SET IdAlumno = NULL WHERE IdAlumno = @IdAlumno');
      await scoped().query('DELETE FROM dbo.HistorialAcademico WHERE IdAlumno = @IdAlumno');
      await scoped().query('DELETE FROM dbo.OtpHistorial WHERE IdAlumno = @IdAlumno');
      await scoped().query('DELETE FROM dbo.SesionesLogin WHERE IdAlumno = @IdAlumno');
      await scoped().query('DELETE FROM dbo.Alumnos WHERE IdAlumno = @IdAlumno');
      await transaction.commit();
    } catch (txError) {
      await transaction.rollback();
      throw txError;
    }

    const actor = { matricula: req.admin.usuario, nombre: req.admin.nombre };
    try {
      await ensureAuditTable();
      await writeAudit(actor, 'Alumno eliminado', `Matricula: ${matricula} · ${alumno.NombreCompleto}`);
    } catch (auditError) {
      console.error('[AUDITORIA][ERROR]', auditError.message);
    }

    console.log(`[BAJA][OK] matricula=${matricula} actor=${actor.matricula}`);
    res.json({ ok: true, matricula });
  } catch (err) {
    console.error('[BAJA][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// ─── Calificaciones — GET trae todas las materias del plan de estudios de la
// carrera del alumno con su calificación (si existe en HistorialAcademico);
// PUT registra/edita la calificación de una materia concreta. ───────────────
// Historial completo del propio alumno (Kardex/Tira de Materias/Calificaciones
// del lado alumno) — misma consulta que la versión de administrador de abajo,
// pero protegida con requireAlumno() en vez de requireAdmin: solo el propio
// alumno (token de sesión emitido al verificar su OTP, ver DEC-030) puede
// consultar SU historial; pedir otra matrícula responde 403.
app.get('/api/mi-historial/:matricula', requireAlumno(), async (req, res) => {
  try {
    const matricula = String(req.params.matricula || '').trim();
    if (!matricula) return res.status(400).json({ ok: false, error: 'Matrícula requerida' });

    const db = await getPool();
    const alumnoLookup = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .query(`
        SELECT a.IdAlumno, a.NombreCompleto, a.Semestre, c.Nombre AS Carrera, c.Clave AS ClaveCarrera
        FROM dbo.Alumnos a
        LEFT JOIN dbo.CatalogoCarreras c ON c.IdCarrera = a.IdCarrera
        WHERE a.Matricula = @Matricula
      `);
    const alumno = alumnoLookup.recordset[0];
    if (!alumno) return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });

    const alumnoInfo = {
      matricula, nombreCompleto: alumno.NombreCompleto, semestre: alumno.Semestre,
      carrera: alumno.Carrera, claveCarrera: alumno.ClaveCarrera
    };
    if (!alumno.ClaveCarrera || !alumno.Semestre) {
      return res.json({ ok: true, alumno: alumnoInfo, materias: [] });
    }

    const materias = await db.request()
      .input('IdAlumno', sql.Int, alumno.IdAlumno)
      .input('Clave', sql.NVarChar(30), alumno.ClaveCarrera)
      .query(`
        SELECT pm.IdMateria, pm.Nombre, pm.Creditos, pm.Semestre,
               h.Calificacion, es.Codigo AS EstatusCodigo
        FROM dbo.PlanEstudioMaterias pm
        JOIN dbo.CatalogoCarreras c ON c.IdCarrera = pm.IdCarrera AND c.Clave = @Clave
        LEFT JOIN dbo.HistorialAcademico h ON h.IdMateria = pm.IdMateria AND h.IdAlumno = @IdAlumno AND h.TipoRegistro = N'EVALUACION'
        LEFT JOIN dbo.CatalogoEstatusMateria es ON es.IdEstatus = h.IdEstatus
        WHERE pm.Activo = 1
        ORDER BY pm.Semestre, pm.Nombre
      `);

    res.json({ ok: true, alumno: alumnoInfo, materias: materias.recordset });
  } catch (err) {
    console.error('[MI_HISTORIAL][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// ─── Perfil propio del alumno — ver/editar datos personales y cambiar la
// contraseña sin pasar por el flujo de recuperación. Igual que /api/mi-historial,
// protegido con requireAlumno() (sesión propia, ver DEC-030).
app.get('/api/mi-perfil/:matricula', requireAlumno(), async (req, res) => {
  try {
    const matricula = String(req.params.matricula || '').trim();
    if (!matricula) return res.status(400).json({ ok: false, error: 'Matrícula requerida' });

    const db = await getPool();
    const lookup = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .query(`
        SELECT a.NombreCompleto, a.CorreoOtp, a.CorreoInstitucional, a.FechaNacimiento,
               c.Nombre AS Carrera, c.Clave AS ClaveCarrera, a.Semestre
        FROM dbo.Alumnos a
        LEFT JOIN dbo.CatalogoCarreras c ON c.IdCarrera = a.IdCarrera
        WHERE a.Matricula = @Matricula
      `);
    const alumno = lookup.recordset[0];
    if (!alumno) return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });

    res.json({
      ok: true,
      perfil: {
        matricula,
        nombreCompleto: alumno.NombreCompleto,
        correoOtp: alumno.CorreoOtp,
        correoInstitucional: alumno.CorreoInstitucional,
        fechaNacimiento: alumno.FechaNacimiento,
        carrera: alumno.Carrera,
        claveCarrera: alumno.ClaveCarrera,
        semestre: alumno.Semestre
      }
    });
  } catch (err) {
    console.error('[MI_PERFIL][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Solo el correo de recuperación es editable por el propio alumno: nombre,
// carrera y semestre son datos oficiales que controla el director (ver
// PUT /api/usuarios/:matricula) — evita que un alumno se "renombre" o cambie
// su carrera desde la app.
app.put('/api/mi-perfil/:matricula', requireAlumno(), async (req, res) => {
  try {
    const matricula = String(req.params.matricula || '').trim();
    const correoOtp = String(req.body?.correoOtp || '').trim();
    if (!matricula) return res.status(400).json({ ok: false, error: 'Matrícula requerida' });
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correoOtp)) {
      return res.status(400).json({ ok: false, error: 'Correo inválido' });
    }

    const db = await getPool();
    const result = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .input('CorreoOtp', sql.NVarChar(255), correoOtp)
      .query(`
        UPDATE dbo.Alumnos SET CorreoOtp = @CorreoOtp WHERE Matricula = @Matricula;
        SELECT @@ROWCOUNT AS Afectados;
      `);
    if (result.recordset[0].Afectados === 0) {
      return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });
    }

    try {
      await ensureAuditTable();
      await writeAudit({ matricula, nombre: matricula }, 'Perfil actualizado', 'El alumno actualizó su correo de recuperación', matricula);
    } catch (auditError) {
      console.error('[AUDITORIA][ERROR]', auditError.message);
    }

    console.log(`[MI_PERFIL][OK] matricula=${matricula}`);
    res.json({ ok: true });
  } catch (err) {
    console.error('[MI_PERFIL][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Cambio de contraseña estando ya logueado — a diferencia de /api/recuperar-password,
// aquí se exige la contraseña ACTUAL en vez de un código al correo (el alumno ya
// demostró su identidad al iniciar esta sesión).
app.post('/api/mi-perfil/cambiar-password', requireAlumno(), async (req, res) => {
  try {
    const matricula = String(req.body?.matricula || '').trim();
    const passwordActual = String(req.body?.passwordActual || '');
    const passwordNueva = String(req.body?.passwordNueva || '');
    if (!matricula || !passwordActual) {
      return res.status(400).json({ ok: false, error: 'Matrícula y contraseña actual son obligatorias' });
    }
    if (!/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)[A-Za-z\d]{8,}$/.test(passwordNueva)) {
      return res.status(400).json({ ok: false, error: 'La contraseña requiere 8 caracteres, mayúscula, minúscula y número, sin espacios ni símbolos' });
    }
    if (excedeLimite(`${clientIp(req)}:cambiar-password:${matricula}`, 8, 10 * 60 * 1000)) {
      return res.status(429).json({ ok: false, error: 'Demasiados intentos. Espera unos minutos.' });
    }

    const db = await getPool();
    const lookup = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .query(`
        SELECT a.IdAlumno, ac.ContrasenaHash, ac.ContrasenaSalt
        FROM dbo.Alumnos a
        JOIN dbo.AlumnoCredenciales ac ON ac.IdAlumno = a.IdAlumno AND ac.Estado = N'REGISTRADO'
        WHERE a.Matricula = @Matricula
      `);
    const cuenta = lookup.recordset[0];
    if (!cuenta || !verifyPassword(passwordActual, cuenta.ContrasenaHash, cuenta.ContrasenaSalt)) {
      console.warn(`[CAMBIAR_PASSWORD][RECHAZO] matricula=${matricula}`);
      return res.status(401).json({ ok: false, error: 'La contraseña actual es incorrecta' });
    }

    const credenciales = hashPassword(passwordNueva);
    await db.request()
      .input('IdAlumno', sql.Int, cuenta.IdAlumno)
      .input('Hash', sql.VarBinary(256), credenciales.hash)
      .input('Salt', sql.VarBinary(128), credenciales.salt)
      .query(`
        UPDATE dbo.AlumnoCredenciales
        SET ContrasenaHash = @Hash, ContrasenaSalt = @Salt, FechaActualizacion = SYSUTCDATETIME()
        WHERE IdAlumno = @IdAlumno
      `);

    try {
      await ensureAuditTable();
      await writeAudit({ matricula, nombre: matricula }, 'Contraseña cambiada', 'El alumno cambió su contraseña desde su perfil', matricula);
    } catch (auditError) {
      console.error('[AUDITORIA][ERROR]', auditError.message);
    }

    console.log(`[CAMBIAR_PASSWORD][OK] matricula=${matricula}`);
    res.json({ ok: true });
  } catch (err) {
    console.error('[CAMBIAR_PASSWORD][ERROR]', err.message);
    res.status(500).json({ ok: false, error: 'No se pudo cambiar la contraseña' });
  }
});

// ─── Horario de clases — importación desde Excel (admin) + consulta del
// alumno ("Materias y Horario de Hoy" en Inicio). No existía ninguna fuente
// real de día/hora/profesor/aula antes de esto (ver DEC-029) — se llena
// siempre por archivo, nunca capturando materias a mano desde la app. ──────

const DIAS_SEMANA = { lunes: 1, martes: 2, miercoles: 3, jueves: 4, viernes: 5, sabado: 6, domingo: 7 };
const NOMBRE_DIA = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

function normalizarTexto(valor) {
  return String(valor ?? '')
    .toLowerCase()
    .normalize('NFD').replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]/g, '');
}

/** Acepta celda de Excel como Date (formato hora), número (fracción de día,
 *  0.5 = 12:00) o texto "HH:MM" — regresa "HH:MM:00" o null si no es válida. */
function parseHoraCelda(valor) {
  if (valor instanceof Date) {
    const hh = String(valor.getUTCHours()).padStart(2, '0');
    const mm = String(valor.getUTCMinutes()).padStart(2, '0');
    return `${hh}:${mm}:00`;
  }
  if (typeof valor === 'number') {
    const totalMin = Math.round(valor * 24 * 60);
    const hh = String(Math.floor(totalMin / 60) % 24).padStart(2, '0');
    const mm = String(totalMin % 60).padStart(2, '0');
    return `${hh}:${mm}:00`;
  }
  const match = String(valor ?? '').trim().match(/^(\d{1,2}):(\d{2})/);
  if (!match) return null;
  const hh = Number(match[1]);
  const mm = Number(match[2]);
  if (hh > 23 || mm > 59) return null;
  return `${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}:00`;
}

/** "16:05" o "16:05:00" → minutos desde medianoche. */
function horaAMinutos(hhmm) {
  const [h, m] = String(hhmm).split(':').map(Number);
  return h * 60 + m;
}

function estadoClase(horaInicio, horaFin, ahoraMin) {
  const inicio = horaAMinutos(horaInicio);
  const fin = horaAMinutos(horaFin);
  if (ahoraMin < inicio) return 'PROXIMA';
  if (ahoraMin >= inicio && ahoraMin < fin) return 'EN_CURSO';
  return 'TERMINADA';
}

// Importar un Excel de horario para una carrera+semestre(+grupo). El archivo
// va en el campo "archivo" (multipart); claveCarrera/semestre/grupo/periodo
// van como campos de texto del mismo form. Columnas esperadas en la hoja 1
// (encabezados en la fila 1, sin importar mayúsculas/acentos): Materia, Dia,
// HoraInicio, HoraFin, Profesor, Aula, Modalidad (esta última opcional).
// Reemplaza por completo el horario existente de esa carrera+semestre+grupo
// — así, volver a subir un Excel corregido no deja filas viejas mezcladas.
app.post('/api/horarios/importar', requireAdmin(), uploadHorario.single('archivo'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ ok: false, error: 'Falta el archivo de horario (.xlsx)' });
    const claveCarrera = String(req.body?.claveCarrera || '').trim();
    const semestre = Number(req.body?.semestre);
    const grupo = String(req.body?.grupo || '').trim() || null;
    const periodoEtiqueta = String(req.body?.periodoEtiqueta || '').trim() || null;
    if (!claveCarrera || !Number.isInteger(semestre) || semestre < 1 || semestre > 12) {
      return res.status(400).json({ ok: false, error: 'Carrera y semestre (1-12) son obligatorios' });
    }

    const db = await getPool();
    const carreraLookup = await db.request()
      .input('Clave', sql.NVarChar(30), claveCarrera)
      .query(`SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = @Clave AND Activo = 1`);
    const idCarrera = carreraLookup.recordset[0]?.IdCarrera;
    if (!idCarrera) return res.status(400).json({ ok: false, error: 'La carrera no existe en el catálogo' });

    const workbook = new ExcelJS.Workbook();
    try {
      await workbook.xlsx.load(req.file.buffer);
    } catch {
      return res.status(400).json({ ok: false, error: 'El archivo no es un Excel (.xlsx) válido' });
    }
    const hoja = workbook.worksheets[0];
    if (!hoja) return res.status(400).json({ ok: false, error: 'El Excel no tiene ninguna hoja' });

    const encabezados = {};
    hoja.getRow(1).eachCell((celda, col) => { encabezados[normalizarTexto(celda.value)] = col; });
    const col = (...alias) => alias.map(normalizarTexto).map((a) => encabezados[a]).find((c) => c) || null;
    const colMateria = col('materia');
    const colDia = col('dia');
    const colHoraInicio = col('horainicio', 'horade inicio', 'inicio');
    const colHoraFin = col('horafin', 'horadefin', 'fin');
    const colProfesor = col('profesor', 'docente', 'maestro');
    const colAula = col('aula', 'salon', 'lugar');
    const colModalidad = col('modalidad');
    if (!colMateria || !colDia || !colHoraInicio || !colHoraFin) {
      return res.status(400).json({
        ok: false,
        error: 'Al Excel le faltan columnas obligatorias — se esperan al menos: Materia, Dia, HoraInicio, HoraFin'
      });
    }

    const filas = [];
    const errores = [];
    hoja.eachRow((fila, numero) => {
      if (numero === 1) return; // encabezado
      const materia = String(fila.getCell(colMateria).value ?? '').trim();
      if (!materia) return; // fila vacía — se ignora en silencio
      const diaTexto = normalizarTexto(fila.getCell(colDia).value);
      const diaSemana = DIAS_SEMANA[diaTexto];
      const horaInicio = parseHoraCelda(fila.getCell(colHoraInicio).value);
      const horaFin = parseHoraCelda(fila.getCell(colHoraFin).value);
      if (!diaSemana) { errores.push(`Fila ${numero}: día "${fila.getCell(colDia).value}" no reconocido`); return; }
      if (!horaInicio || !horaFin) { errores.push(`Fila ${numero}: hora inválida`); return; }
      filas.push({
        materia,
        diaSemana,
        horaInicio,
        horaFin,
        profesor: colProfesor ? String(fila.getCell(colProfesor).value ?? '').trim() || null : null,
        aula: colAula ? String(fila.getCell(colAula).value ?? '').trim() || null : null,
        modalidad: (colModalidad ? String(fila.getCell(colModalidad).value ?? '').trim() : '') || 'PRESENCIAL'
      });
    });
    if (filas.length === 0) {
      return res.status(400).json({ ok: false, error: 'El Excel no tiene filas válidas para importar', erroresFilas: errores });
    }

    // Reemplaza el horario existente de esa carrera+semestre+grupo antes de
    // insertar el nuevo — una re-subida siempre dexa el horario limpio.
    const tx = db.transaction();
    await tx.begin();
    try {
      const reqDelete = tx.request().input('IdCarrera', sql.Int, idCarrera).input('Semestre', sql.TinyInt, semestre);
      if (grupo) reqDelete.input('Grupo', sql.NVarChar(10), grupo);
      await reqDelete.query(`
        DELETE FROM dbo.Horarios WHERE IdCarrera = @IdCarrera AND Semestre = @Semestre
        AND ${grupo ? 'Grupo = @Grupo' : 'Grupo IS NULL'}
      `);
      for (const f of filas) {
        await tx.request()
          .input('IdCarrera', sql.Int, idCarrera)
          .input('Semestre', sql.TinyInt, semestre)
          .input('Grupo', sql.NVarChar(10), grupo)
          .input('NombreMateria', sql.NVarChar(200), f.materia)
          .input('DiaSemana', sql.TinyInt, f.diaSemana)
          .input('HoraInicio', sql.NVarChar(8), f.horaInicio)
          .input('HoraFin', sql.NVarChar(8), f.horaFin)
          .input('Profesor', sql.NVarChar(200), f.profesor)
          .input('Aula', sql.NVarChar(100), f.aula)
          .input('Modalidad', sql.NVarChar(20), f.modalidad)
          .input('PeriodoEtiqueta', sql.NVarChar(20), periodoEtiqueta)
          .input('CargadoPor', sql.NVarChar(50), req.admin.usuario)
          .query(`
            INSERT INTO dbo.Horarios
              (IdCarrera, Semestre, Grupo, NombreMateria, DiaSemana, HoraInicio, HoraFin, Profesor, Aula, Modalidad, PeriodoEtiqueta, CargadoPor)
            VALUES
              (@IdCarrera, @Semestre, @Grupo, @NombreMateria, @DiaSemana, CAST(@HoraInicio AS TIME), CAST(@HoraFin AS TIME), @Profesor, @Aula, @Modalidad, @PeriodoEtiqueta, @CargadoPor)
          `);
      }
      await tx.commit();
    } catch (txError) {
      await tx.rollback();
      throw txError;
    }

    console.log(`[HORARIOS][IMPORTAR][OK] carrera=${claveCarrera} semestre=${semestre} grupo=${grupo || '-'} filas=${filas.length} actor=${req.admin.usuario}`);
    res.status(201).json({ ok: true, filasImportadas: filas.length, erroresFilas: errores });
  } catch (err) {
    console.error('[HORARIOS][IMPORTAR][ERROR]', err.message);
    res.status(500).json({ ok: false, error: 'No se pudo importar el horario' });
  }
});

// Listado para revisión/edición desde el panel de administrador.
app.get('/api/horarios', requireAdmin(), async (req, res) => {
  try {
    const claveCarrera = String(req.query.claveCarrera || '').trim();
    const semestre = Number(req.query.semestre);
    if (!claveCarrera || !Number.isInteger(semestre)) {
      return res.status(400).json({ ok: false, error: 'claveCarrera y semestre son obligatorios' });
    }
    const db = await getPool();
    const result = await db.request()
      .input('Clave', sql.NVarChar(30), claveCarrera)
      .input('Semestre', sql.TinyInt, semestre)
      .query(`
        SELECT h.IdHorario, h.Grupo, h.NombreMateria, h.DiaSemana,
               CONVERT(VARCHAR(5), h.HoraInicio, 108) AS HoraInicio,
               CONVERT(VARCHAR(5), h.HoraFin, 108) AS HoraFin,
               h.Profesor, h.Aula, h.Modalidad, h.PeriodoEtiqueta, h.CargadoPor, h.FechaCarga
        FROM dbo.Horarios h
        JOIN dbo.CatalogoCarreras c ON c.IdCarrera = h.IdCarrera
        WHERE c.Clave = @Clave AND h.Semestre = @Semestre
        ORDER BY h.DiaSemana, h.HoraInicio
      `);
    res.json({ ok: true, data: result.recordset });
  } catch (err) {
    console.error('[HORARIOS][LISTAR][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.delete('/api/horarios/:id', requireAdmin(), async (req, res) => {
  try {
    const id = Number(req.params.id);
    if (!Number.isInteger(id)) return res.status(400).json({ ok: false, error: 'Id inválido' });
    const db = await getPool();
    const result = await db.request().input('Id', sql.Int, id)
      .query(`DELETE FROM dbo.Horarios WHERE IdHorario = @Id; SELECT @@ROWCOUNT AS Afectados;`);
    if (result.recordset[0].Afectados === 0) return res.status(404).json({ ok: false, error: 'No existe ese horario' });
    res.json({ ok: true });
  } catch (err) {
    console.error('[HORARIOS][ELIMINAR][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Horario de hoy del alumno en sesión — protegido con requireAlumno() igual que
// /api/mi-historial (ver DEC-030). Filtra solo
// por carrera+semestre: la app todavía no guarda a qué grupo específico
// pertenece cada alumno (mismo límite ya aceptado en Reinscripción/Tira de
// Materias), así que si algún día hay más de un grupo por semestre con
// horarios distintos, este endpoint los mezclaría — limitación conocida.
app.get('/api/mi-horario/:matricula', requireAlumno(), async (req, res) => {
  try {
    const matricula = String(req.params.matricula || '').trim();
    if (!matricula) return res.status(400).json({ ok: false, error: 'Matrícula requerida' });

    const db = await getPool();
    const alumnoLookup = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .query(`
        SELECT a.Semestre, c.Nombre AS Carrera, c.Clave AS ClaveCarrera
        FROM dbo.Alumnos a LEFT JOIN dbo.CatalogoCarreras c ON c.IdCarrera = a.IdCarrera
        WHERE a.Matricula = @Matricula
      `);
    const alumno = alumnoLookup.recordset[0];
    if (!alumno) return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });
    if (!alumno.ClaveCarrera || !alumno.Semestre) {
      return res.json({ ok: true, dia: NOMBRE_DIA[new Date().getDay() || 7], clases: [] });
    }

    const ahora = new Date();
    const diaSemana = ahora.getDay() === 0 ? 7 : ahora.getDay();
    const ahoraMin = ahora.getHours() * 60 + ahora.getMinutes();

    const result = await db.request()
      .input('Clave', sql.NVarChar(30), alumno.ClaveCarrera)
      .input('Semestre', sql.TinyInt, alumno.Semestre)
      .input('Dia', sql.TinyInt, diaSemana)
      .query(`
        SELECT h.NombreMateria, CONVERT(VARCHAR(5), h.HoraInicio, 108) AS HoraInicio,
               CONVERT(VARCHAR(5), h.HoraFin, 108) AS HoraFin, h.Profesor, h.Aula, h.Modalidad
        FROM dbo.Horarios h
        JOIN dbo.CatalogoCarreras c ON c.IdCarrera = h.IdCarrera AND c.Clave = @Clave
        WHERE h.Semestre = @Semestre AND h.DiaSemana = @Dia
        ORDER BY h.HoraInicio
      `);

    const clases = result.recordset.map((c) => ({
      materia: c.NombreMateria,
      horaInicio: c.HoraInicio,
      horaFin: c.HoraFin,
      profesor: c.Profesor,
      aula: c.Aula,
      modalidad: c.Modalidad,
      estado: estadoClase(c.HoraInicio, c.HoraFin, ahoraMin)
    }));

    res.json({ ok: true, dia: NOMBRE_DIA[diaSemana], clases });
  } catch (err) {
    console.error('[MI_HORARIO][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.get('/api/calificaciones/:matricula', requireAdmin(), async (req, res) => {
  try {
    const matricula = String(req.params.matricula || '').trim();
    if (!matricula) return res.status(400).json({ ok: false, error: 'Matrícula requerida' });

    const db = await getPool();
    const alumnoLookup = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .query(`
        SELECT a.IdAlumno, a.NombreCompleto, a.Semestre, c.Nombre AS Carrera, c.Clave AS ClaveCarrera
        FROM dbo.Alumnos a
        LEFT JOIN dbo.CatalogoCarreras c ON c.IdCarrera = a.IdCarrera
        WHERE a.Matricula = @Matricula
      `);
    const alumno = alumnoLookup.recordset[0];
    if (!alumno) return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });

    const alumnoInfo = {
      matricula, nombreCompleto: alumno.NombreCompleto, semestre: alumno.Semestre,
      carrera: alumno.Carrera, claveCarrera: alumno.ClaveCarrera
    };
    if (!alumno.ClaveCarrera) {
      return res.json({ ok: true, alumno: alumnoInfo, materias: [] });
    }

    const materias = await db.request()
      .input('IdAlumno', sql.Int, alumno.IdAlumno)
      .input('Clave', sql.NVarChar(30), alumno.ClaveCarrera)
      .query(`
        SELECT pm.IdMateria, pm.Nombre, pm.Creditos, pm.Semestre,
               h.Calificacion, es.Codigo AS EstatusCodigo, es.Nombre AS EstatusNombre, h.FechaActualizacion
        FROM dbo.PlanEstudioMaterias pm
        JOIN dbo.CatalogoCarreras c ON c.IdCarrera = pm.IdCarrera AND c.Clave = @Clave
        LEFT JOIN dbo.HistorialAcademico h ON h.IdMateria = pm.IdMateria AND h.IdAlumno = @IdAlumno AND h.TipoRegistro = N'EVALUACION'
        LEFT JOIN dbo.CatalogoEstatusMateria es ON es.IdEstatus = h.IdEstatus
        WHERE pm.Activo = 1
        ORDER BY pm.Semestre, pm.Nombre
      `);

    res.json({ ok: true, alumno: alumnoInfo, materias: materias.recordset });
  } catch (err) {
    console.error('[CALIFICACIONES][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.put('/api/calificaciones/:matricula/:idMateria', requireAdmin(), async (req, res) => {
  try {
    const matricula = String(req.params.matricula || '').trim();
    const idMateria = Number(req.params.idMateria);
    if (!matricula || !Number.isInteger(idMateria)) {
      return res.status(400).json({ ok: false, error: 'Matrícula e IdMateria son obligatorios' });
    }
    const calificacionRaw = req.body?.calificacion;
    const calificacion = (calificacionRaw === undefined || calificacionRaw === null || calificacionRaw === '')
      ? null : Number(calificacionRaw);
    if (calificacion !== null && (Number.isNaN(calificacion) || calificacion < 0 || calificacion > 10)) {
      return res.status(400).json({ ok: false, error: 'La calificación debe ser un número entre 0 y 10' });
    }
    const estatusCodigo = String(
      req.body?.estatusCodigo || (calificacion === null ? 'PC' : (calificacion >= 6 ? 'AP' : 'NA'))
    ).trim().toUpperCase();

    const db = await getPool();
    const alumnoLookup = await db.request()
      .input('Matricula', sql.NVarChar(20), matricula)
      .query(`SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = @Matricula`);
    const alumno = alumnoLookup.recordset[0];
    if (!alumno) return res.status(404).json({ ok: false, error: 'No existe un alumno con esa matrícula' });

    const estatusLookup = await db.request()
      .input('Codigo', sql.Char(2), estatusCodigo)
      .query(`SELECT IdEstatus FROM dbo.CatalogoEstatusMateria WHERE Codigo = @Codigo`);
    const estatus = estatusLookup.recordset[0];
    if (!estatus) return res.status(400).json({ ok: false, error: 'Estatus inválido' });

    const existing = await db.request()
      .input('IdAlumno', sql.Int, alumno.IdAlumno)
      .input('IdMateria', sql.Int, idMateria)
      .query(`
        SELECT 1 FROM dbo.HistorialAcademico
        WHERE IdAlumno = @IdAlumno AND IdMateria = @IdMateria AND TipoRegistro = N'EVALUACION'
      `);

    const writeRequest = db.request()
      .input('IdAlumno', sql.Int, alumno.IdAlumno)
      .input('IdMateria', sql.Int, idMateria)
      .input('Calificacion', sql.Decimal(5, 2), calificacion)
      .input('IdEstatus', sql.Int, estatus.IdEstatus);

    if (existing.recordset.length > 0) {
      await writeRequest.query(`
        UPDATE dbo.HistorialAcademico
        SET Calificacion = @Calificacion, IdEstatus = @IdEstatus, FechaActualizacion = SYSUTCDATETIME()
        WHERE IdAlumno = @IdAlumno AND IdMateria = @IdMateria AND TipoRegistro = N'EVALUACION'
      `);
    } else {
      await writeRequest.query(`
        INSERT INTO dbo.HistorialAcademico (IdAlumno, IdMateria, TipoRegistro, Calificacion, IdEstatus)
        VALUES (@IdAlumno, @IdMateria, N'EVALUACION', @Calificacion, @IdEstatus)
      `);
    }

    const actor = { matricula: req.admin.usuario, nombre: req.admin.nombre };
    try {
      await ensureAuditTable();
      await writeAudit(
        actor, 'Calificación actualizada',
        `Matricula: ${matricula} · Materia ${idMateria} -> ${calificacion ?? 'sin calificación'} (${estatusCodigo})`,
        matricula
      );
    } catch (auditError) {
      console.error('[AUDITORIA][ERROR]', auditError.message);
    }

    console.log(`[CALIFICACION][OK] matricula=${matricula} idMateria=${idMateria} actor=${actor.matricula}`);
    res.json({ ok: true });
  } catch (err) {
    console.error('[CALIFICACION][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// ─── Estadísticas — agregados para las gráficas del panel de administrador ──
app.get('/api/estadisticas', requireAdmin(), async (req, res) => {
  try {
    const db = await getPool();

    const porCarrera = await db.request().query(`
      SELECT c.Nombre AS Carrera, c.Clave AS ClaveCarrera, COUNT(a.IdAlumno) AS Total
      FROM dbo.CatalogoCarreras c
      LEFT JOIN dbo.Alumnos a ON a.IdCarrera = c.IdCarrera AND a.Activo = 1
      WHERE c.Activo = 1
      GROUP BY c.Nombre, c.Clave
      ORDER BY Total DESC
    `);

    const porSemestre = await db.request().query(`
      SELECT Semestre, COUNT(*) AS Total
      FROM dbo.Alumnos
      WHERE Activo = 1 AND Semestre IS NOT NULL
      GROUP BY Semestre
      ORDER BY Semestre
    `);

    const estado = await db.request().query(`
      SELECT
        SUM(CASE WHEN Activo = 1 THEN 1 ELSE 0 END) AS Activos,
        SUM(CASE WHEN Activo = 0 THEN 1 ELSE 0 END) AS Inactivos,
        SUM(CASE WHEN Semestre IS NULL THEN 1 ELSE 0 END) AS SinSemestre
      FROM dbo.Alumnos
    `);

    const altasPorMes = await db.request().query(`
      SELECT FORMAT(FechaRegistro, 'yyyy-MM') AS Mes, COUNT(*) AS Total
      FROM dbo.Alumnos
      WHERE FechaRegistro >= DATEADD(MONTH, -5, SYSUTCDATETIME())
      GROUP BY FORMAT(FechaRegistro, 'yyyy-MM')
      ORDER BY Mes
    `);

    const calificaciones = await db.request().query(`
      SELECT es.Codigo, es.Nombre, COUNT(*) AS Total
      FROM dbo.HistorialAcademico h
      JOIN dbo.CatalogoEstatusMateria es ON es.IdEstatus = h.IdEstatus
      WHERE h.TipoRegistro = N'EVALUACION'
      GROUP BY es.Codigo, es.Nombre
    `);

    res.json({
      ok: true,
      porCarrera: porCarrera.recordset,
      porSemestre: porSemestre.recordset,
      estado: estado.recordset[0] || { Activos: 0, Inactivos: 0, SinSemestre: 0 },
      altasPorMes: altasPorMes.recordset,
      calificaciones: calificaciones.recordset
    });
  } catch (err) {
    console.error('[ESTADISTICAS][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// ─── Gestión de administradores — exclusivo de SUPERADMIN (ver DEC-020) ────
// Sin esto no había ninguna forma de crear un OPERADOR desde la app: la
// única vía era el script de línea de comandos crear_administrador.js.
function generarPasswordSegura() {
  // Mismo generador que scripts/crear_administrador.js — nunca se inventa
  // una contraseña "memorable" a mano.
  const alfabeto = 'ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%^&*';
  const bytes = crypto.randomBytes(18);
  let out = Array.from(bytes, (b) => alfabeto[b % alfabeto.length]).join('');
  if (!/[A-Z]/.test(out)) out = 'A' + out.slice(1);
  if (!/[a-z]/.test(out)) out = out.slice(0, 1) + 'a' + out.slice(2);
  if (!/[0-9]/.test(out)) out = out.slice(0, 2) + '5' + out.slice(3);
  if (!/[!@#$%^&*]/.test(out)) out = out.slice(0, 3) + '!' + out.slice(4);
  return out;
}

app.get('/api/administradores', requireAdmin(['SUPERADMIN']), async (req, res) => {
  try {
    const db = await getPool();
    const result = await db.request().query(`
      SELECT IdAdministrador, Usuario, NombreCompleto, CorreoInstitucional, Rol, Activo, FechaRegistro, UltimoAcceso
      FROM dbo.Administradores ORDER BY FechaRegistro DESC
    `);
    res.json({ ok: true, data: result.recordset });
  } catch (err) {
    console.error('[ADMINISTRADORES][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.post('/api/administradores', requireAdmin(['SUPERADMIN']), async (req, res) => {
  try {
    const usuario = String(req.body?.usuario || '').trim();
    const nombreCompleto = String(req.body?.nombreCompleto || '').trim();
    const correoInstitucional = String(req.body?.correoInstitucional || '').trim() || null;
    const rol = String(req.body?.rol || 'OPERADOR').toUpperCase();
    if (!usuario || !nombreCompleto) {
      return res.status(400).json({ ok: false, error: 'Usuario y nombre completo son obligatorios' });
    }
    if (!['SUPERADMIN', 'OPERADOR'].includes(rol)) {
      return res.status(400).json({ ok: false, error: 'Rol inválido' });
    }

    const db = await getPool();
    const existe = await db.request().input('Usuario', sql.NVarChar(50), usuario)
      .query(`SELECT 1 FROM dbo.Administradores WHERE Usuario = @Usuario`);
    if (existe.recordset.length > 0) {
      return res.status(409).json({ ok: false, error: 'Ya existe un administrador con ese usuario' });
    }

    const password = generarPasswordSegura();
    const { hash, salt } = hashPassword(password);

    const insertado = await db.request()
      .input('Usuario', sql.NVarChar(50), usuario)
      .input('NombreCompleto', sql.NVarChar(200), nombreCompleto)
      .input('CorreoInstitucional', sql.NVarChar(255), correoInstitucional)
      .input('Rol', sql.NVarChar(20), rol)
      .query(`
        DECLARE @Nuevo TABLE (IdAdministrador INT);
        INSERT INTO dbo.Administradores (Usuario, NombreCompleto, CorreoInstitucional, Rol)
        OUTPUT INSERTED.IdAdministrador INTO @Nuevo
        VALUES (@Usuario, @NombreCompleto, @CorreoInstitucional, @Rol);
        SELECT IdAdministrador FROM @Nuevo;
      `);
    const idAdministrador = insertado.recordset[0].IdAdministrador;
    await db.request()
      .input('Id', sql.Int, idAdministrador)
      .input('Hash', sql.VarBinary(256), hash)
      .input('Salt', sql.VarBinary(128), salt)
      .query(`INSERT INTO dbo.AdministradorCredenciales (IdAdministrador, ContrasenaHash, ContrasenaSalt) VALUES (@Id, @Hash, @Salt)`);

    try {
      await ensureAuditTable();
      await writeAudit({ matricula: req.admin.usuario, nombre: req.admin.nombre }, 'Administrador creado', `Usuario: ${usuario} · Rol: ${rol}`);
    } catch (auditError) {
      console.error('[AUDITORIA][ERROR]', auditError.message);
    }

    console.log(`[ADMIN-CREAR][OK] usuario=${usuario} rol=${rol} por=${req.admin.usuario}`);
    // La contraseña solo se devuelve UNA vez, en esta respuesta — después
    // solo queda su hash en la base de datos, irrecuperable.
    res.status(201).json({ ok: true, usuario, password });
  } catch (err) {
    console.error('[ADMIN-CREAR][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.put('/api/administradores/:usuario', requireAdmin(['SUPERADMIN']), async (req, res) => {
  try {
    const usuario = String(req.params.usuario || '').trim();
    if (!usuario) return res.status(400).json({ ok: false, error: 'Usuario requerido' });

    const rolNuevo = req.body?.rol !== undefined ? String(req.body.rol).toUpperCase() : undefined;
    if (usuario === req.admin.usuario && (req.body?.activo === false || (rolNuevo && rolNuevo !== 'SUPERADMIN'))) {
      return res.status(400).json({ ok: false, error: 'No puedes desactivarte ni quitarte el rol de superadmin a ti mismo' });
    }

    const db = await getPool();
    const request = db.request().input('Usuario', sql.NVarChar(50), usuario);
    const sets = [];

    if (req.body?.nombreCompleto !== undefined) {
      const v = String(req.body.nombreCompleto).trim();
      if (!v) return res.status(400).json({ ok: false, error: 'El nombre no puede quedar vacío' });
      request.input('NombreCompleto', sql.NVarChar(200), v);
      sets.push('NombreCompleto = @NombreCompleto');
    }
    if (rolNuevo !== undefined) {
      if (!['SUPERADMIN', 'OPERADOR'].includes(rolNuevo)) return res.status(400).json({ ok: false, error: 'Rol inválido' });
      request.input('Rol', sql.NVarChar(20), rolNuevo);
      sets.push('Rol = @Rol');
    }
    if (req.body?.activo !== undefined) {
      request.input('Activo', sql.Bit, Boolean(req.body.activo));
      sets.push('Activo = @Activo');
    }
    if (sets.length === 0) return res.status(400).json({ ok: false, error: 'No se envió ningún campo para actualizar' });

    const result = await request.query(`
      UPDATE dbo.Administradores SET ${sets.join(', ')} WHERE Usuario = @Usuario;
      SELECT @@ROWCOUNT AS Afectados;
    `);
    if (result.recordset[0].Afectados === 0) return res.status(404).json({ ok: false, error: 'No existe ese administrador' });

    try {
      await ensureAuditTable();
      await writeAudit(
        { matricula: req.admin.usuario, nombre: req.admin.nombre },
        'Administrador actualizado',
        `Usuario: ${usuario} -> campos: ${Object.keys(req.body || {}).join(', ')}`
      );
    } catch (auditError) {
      console.error('[AUDITORIA][ERROR]', auditError.message);
    }

    console.log(`[ADMIN-EDITAR][OK] usuario=${usuario} por=${req.admin.usuario}`);
    res.json({ ok: true, usuario });
  } catch (err) {
    console.error('[ADMIN-EDITAR][ERROR]', err.message);
    res.status(500).json({ ok: false, error: err.message });
  }
});

const host = process.env.HOST || '0.0.0.0';
app.listen(port, host, () => {
  console.log(`[API] AppTeschi escuchando en http://${host}:${port}`);
  console.log(`[API] Base de datos: ${config.database} | Servidor SQL: ${config.server}`);
});
