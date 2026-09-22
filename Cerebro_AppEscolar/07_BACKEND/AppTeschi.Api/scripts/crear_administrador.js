// Crea (o resetea la contraseña de) una cuenta de administrador real.
// Genera una contraseña aleatoria segura — nunca se inventa una "memorable"
// a mano — y la imprime UNA sola vez en consola; no queda guardada en
// ningún lado más que el hash en la base de datos.
//
// Uso:
//   node scripts/crear_administrador.js <usuario> "<Nombre Completo>" [rol] [correo]
//   rol: SUPERADMIN | OPERADOR (default: OPERADOR)
//
// Ejemplo (primer superadmin real del sistema):
//   node scripts/crear_administrador.js admin "Administrador TESCHI" SUPERADMIN
require('dotenv').config();
const sql = require('mssql');
const crypto = require('crypto');

const PASSWORD_KEY_LENGTH = 64;
const PASSWORD_SALT_LENGTH = 16;
const PASSWORD_COST = 16384;
const PASSWORD_BLOCK_SIZE = 8;
const PASSWORD_PARALLELIZATION = 1;

function hashPassword(password, salt = crypto.randomBytes(PASSWORD_SALT_LENGTH)) {
  return {
    salt,
    hash: crypto.scryptSync(password, salt, PASSWORD_KEY_LENGTH, {
      N: PASSWORD_COST, r: PASSWORD_BLOCK_SIZE, p: PASSWORD_PARALLELIZATION
    })
  };
}

function generarPasswordSegura() {
  // 18 caracteres, alfabeto sin ambiguos (sin 0/O/1/l/I), siempre incluye
  // mayúscula+minúscula+dígito+símbolo para pasar cualquier política razonable.
  const alfabeto = 'ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%^&*';
  const bytes = crypto.randomBytes(18);
  let out = Array.from(bytes, b => alfabeto[b % alfabeto.length]).join('');
  if (!/[A-Z]/.test(out)) out = 'A' + out.slice(1);
  if (!/[a-z]/.test(out)) out = out.slice(0, 1) + 'a' + out.slice(2);
  if (!/[0-9]/.test(out)) out = out.slice(0, 2) + '5' + out.slice(3);
  if (!/[!@#$%^&*]/.test(out)) out = out.slice(0, 3) + '!' + out.slice(4);
  return out;
}

async function main() {
  const [usuario, nombreCompleto, rolArg, correoArg] = process.argv.slice(2);
  if (!usuario || !nombreCompleto) {
    console.error('Uso: node scripts/crear_administrador.js <usuario> "<Nombre Completo>" [SUPERADMIN|OPERADOR] [correo]');
    process.exit(1);
  }
  const rol = (rolArg || 'OPERADOR').toUpperCase();
  if (!['SUPERADMIN', 'OPERADOR'].includes(rol)) {
    console.error('Rol inválido. Usa SUPERADMIN u OPERADOR.');
    process.exit(1);
  }
  const correo = correoArg || null;

  const config = {
    user: process.env.DB_USERNAME || 'sa',
    password: process.env.DB_PASSWORD || '',
    server: process.env.DB_SERVER || 'localhost',
    database: process.env.DB_DATABASE || 'AppTeschiDB',
    options: { encrypt: process.env.DB_ENCRYPT === 'true', trustServerCertificate: true, enableArithAbort: true }
  };

  const pool = await sql.connect(config);
  const password = generarPasswordSegura();
  const { hash, salt } = hashPassword(password);

  const existente = await pool.request()
    .input('Usuario', sql.NVarChar(50), usuario)
    .query('SELECT IdAdministrador FROM dbo.Administradores WHERE Usuario = @Usuario');

  let idAdministrador;
  if (existente.recordset.length > 0) {
    idAdministrador = existente.recordset[0].IdAdministrador;
    await pool.request()
      .input('Id', sql.Int, idAdministrador)
      .input('NombreCompleto', sql.NVarChar(200), nombreCompleto)
      .input('CorreoInstitucional', sql.NVarChar(255), correo)
      .input('Rol', sql.NVarChar(20), rol)
      .query(`UPDATE dbo.Administradores SET NombreCompleto=@NombreCompleto, CorreoInstitucional=@CorreoInstitucional, Rol=@Rol, Activo=1 WHERE IdAdministrador=@Id`);
    await pool.request()
      .input('Id', sql.Int, idAdministrador)
      .input('Hash', sql.VarBinary(256), hash)
      .input('Salt', sql.VarBinary(128), salt)
      .query(`UPDATE dbo.AdministradorCredenciales SET ContrasenaHash=@Hash, ContrasenaSalt=@Salt, Estado=N'REGISTRADO', FechaActualizacion=SYSUTCDATETIME() WHERE IdAdministrador=@Id`);
    console.log(`Contraseña reseteada para el administrador existente "${usuario}".`);
  } else {
    const insertado = await pool.request()
      .input('Usuario', sql.NVarChar(50), usuario)
      .input('NombreCompleto', sql.NVarChar(200), nombreCompleto)
      .input('CorreoInstitucional', sql.NVarChar(255), correo)
      .input('Rol', sql.NVarChar(20), rol)
      .query(`
        DECLARE @Nuevo TABLE (IdAdministrador INT);
        INSERT INTO dbo.Administradores (Usuario, NombreCompleto, CorreoInstitucional, Rol)
        OUTPUT INSERTED.IdAdministrador INTO @Nuevo
        VALUES (@Usuario, @NombreCompleto, @CorreoInstitucional, @Rol);
        SELECT IdAdministrador FROM @Nuevo;
      `);
    idAdministrador = insertado.recordset[0].IdAdministrador;
    await pool.request()
      .input('Id', sql.Int, idAdministrador)
      .input('Hash', sql.VarBinary(256), hash)
      .input('Salt', sql.VarBinary(128), salt)
      .query(`INSERT INTO dbo.AdministradorCredenciales (IdAdministrador, ContrasenaHash, ContrasenaSalt) VALUES (@Id, @Hash, @Salt)`);
    console.log(`Administrador "${usuario}" creado (rol: ${rol}).`);
  }

  console.log('');
  console.log('======================================================');
  console.log(`  Usuario:    ${usuario}`);
  console.log(`  Contraseña: ${password}`);
  console.log('  Guárdala ahora — no se vuelve a mostrar. Cámbiala tras el primer inicio de sesión.');
  console.log('======================================================');

  await pool.close();
}

main().catch(err => { console.error('Error:', err.message); process.exit(1); });
