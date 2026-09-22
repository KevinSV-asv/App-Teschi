-- =============================================================================
-- AppTESCHI — Migración a esquema normalizado (V2)
-- Archivo: 03_MigracionV2_Normalizacion.sql
-- Ejecutar después de 01_CrearBaseDatos.sql (y de 02_ProcedimientosUsuarios.sql
-- si ya se ejecutó antes; este script no depende de esos SPs)
-- Relacionado con: [[Base_Datos_Usuarios]], [[DECISIONES_TECNICAS]] (DEC-014)
--
-- OBJETIVO — eliminar la redundancia detectada en el esquema V1:
--   1) `Usuarios` y `CuentasRegistro` representaban al MISMO alumno en dos
--      tablas sin relación entre sí (mismo alumno podía tener 2 registros
--      independientes e inconsistentes — se confirmó en datos reales: la
--      matrícula 20240001 tenía nombres distintos en cada tabla).
--   2) `OtpHistorial` y `SesionesLogin` repetían la columna `Matricula` a
--      pesar de ya tener `IdUsuario` — dependencia transitiva (viola 3FN).
--   3) `Usuarios.Carrera` era texto libre mientras `CuentasRegistro.IdCarrera`
--      ya usaba el catálogo — dos formas distintas de representar el mismo dato.
--   4) El formato oficial del Kardex repite 3 veces el grupo de columnas
--      Calificación/Periodo (Evaluación / Curso Repetición / Curso Especial)
--      — un "grupo repetido" clásico que viola 1FN si se copia tal cual en
--      columnas; aquí se modela como FILAS (tabla `HistorialAcademico`).
--
-- Es seguro volver a ejecutar este script completo (todas las secciones usan
-- IF NOT EXISTS / IF EXISTS). Las tablas V1 NO se borran, se renombran con
-- prefijo `_ObsoletoV1_` como respaldo — se pueden eliminar manualmente más
-- adelante una vez confirmado que todo funciona.
-- =============================================================================

USE AppTeschiDB;
GO

-- Requerido por SQL Server para columnas calculadas persistidas e índices
-- filtrados (los usa CatalogoPeriodos.Etiqueta y UQ_Alumnos_CorreoInstitucional).
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- =============================================================================
-- SECCIÓN 1 — Catálogos nuevos
-- =============================================================================

-- ─── Periodos escolares — fuente única para cualquier "2022-2", "2026-1", etc. ─
-- Antes cada tabla guardaba el periodo como NVARCHAR libre (o ni siquiera lo
-- guardaba). Con una tabla catálogo se evita duplicar el mismo texto y se
-- puede ordenar/filtrar cronológicamente sin parsear strings.
IF OBJECT_ID(N'dbo.CatalogoPeriodos', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CatalogoPeriodos (
        IdPeriodo INT IDENTITY(1,1) NOT NULL,
        Anio      SMALLINT NOT NULL,
        Numero    TINYINT  NOT NULL, -- 1 = Ene-Jun, 2 = Ago-Dic
        Etiqueta  AS (CAST(Anio AS NVARCHAR(4)) + N'-' + CAST(Numero AS NVARCHAR(1))) PERSISTED,
        CONSTRAINT PK_CatalogoPeriodos PRIMARY KEY CLUSTERED (IdPeriodo),
        CONSTRAINT UQ_CatalogoPeriodos_AnioNumero UNIQUE (Anio, Numero),
        CONSTRAINT CK_CatalogoPeriodos_Numero CHECK (Numero IN (1,2))
    );
END
GO

-- ─── Estatus de materia — mismos códigos oficiales del Kardex (AP/PC/NA) ──────
IF OBJECT_ID(N'dbo.CatalogoEstatusMateria', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CatalogoEstatusMateria (
        IdEstatus INT IDENTITY(1,1) NOT NULL,
        Codigo    CHAR(2)      NOT NULL,
        Nombre    NVARCHAR(40) NOT NULL,
        CONSTRAINT PK_CatalogoEstatusMateria PRIMARY KEY CLUSTERED (IdEstatus),
        CONSTRAINT UQ_CatalogoEstatusMateria_Codigo UNIQUE (Codigo)
    );
    INSERT INTO dbo.CatalogoEstatusMateria (Codigo, Nombre) VALUES
        (N'AP', N'Aprobada'),
        (N'PC', N'Por cursar'),
        (N'NA', N'No aprobó');
END
GO

-- ─── Turno — evita derivarlo con regex desde la clave del grupo cada vez ──────
IF OBJECT_ID(N'dbo.CatalogoTurnos', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CatalogoTurnos (
        IdTurno INT IDENTITY(1,1) NOT NULL,
        Codigo  TINYINT      NOT NULL,
        Nombre  NVARCHAR(20) NOT NULL,
        CONSTRAINT PK_CatalogoTurnos PRIMARY KEY CLUSTERED (IdTurno),
        CONSTRAINT UQ_CatalogoTurnos_Codigo UNIQUE (Codigo)
    );
    INSERT INTO dbo.CatalogoTurnos (Codigo, Nombre) VALUES
        (1, N'Matutino'),
        (2, N'Vespertino');
END
GO

-- =============================================================================
-- SECCIÓN 2 — Identidad unificada: Alumnos (+ AlumnoCredenciales opcional 1:1)
-- =============================================================================

-- ─── Alumnos — UN solo registro por persona (antes: Usuarios + CuentasRegistro)
-- NombreCompleto se guarda como un solo campo (no Nombres/ApellidoPaterno/
-- ApellidoMaterno por separado): el SIIA y el resto de la app (UserSession.
-- nombreCompleto) siempre manejan el nombre como una sola cadena; separar los
-- apellidos solo existía en el formulario de registro y obligaba a
-- reconcatenar en cada consulta. Menos redundancia, un solo formato real.
IF OBJECT_ID(N'dbo.Alumnos', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Alumnos (
        IdAlumno            INT             IDENTITY(1,1) NOT NULL,
        Matricula           NVARCHAR(20)    NOT NULL,
        NombreCompleto      NVARCHAR(200)   NOT NULL,
        FechaNacimiento     DATE            NULL,
        CorreoInstitucional NVARCHAR(255)   NULL,
        CorreoOtp           NVARCHAR(255)   NULL,
        IdSistema           INT             NULL,
        IdCarrera           INT             NULL,
        Equivalencias       BIT             NOT NULL CONSTRAINT DF_Alumnos_Equivalencias DEFAULT (0),
        Activo              BIT             NOT NULL CONSTRAINT DF_Alumnos_Activo DEFAULT (1),
        FechaRegistro       DATETIME2(0)    NOT NULL CONSTRAINT DF_Alumnos_FechaRegistro DEFAULT (SYSUTCDATETIME()),
        UltimoAcceso        DATETIME2(0)    NULL,
        IntentosFallidosOtp TINYINT         NOT NULL CONSTRAINT DF_Alumnos_Intentos DEFAULT (0),
        BloqueadoHasta      DATETIME2(0)    NULL,
        CONSTRAINT PK_Alumnos PRIMARY KEY CLUSTERED (IdAlumno),
        CONSTRAINT UQ_Alumnos_Matricula UNIQUE (Matricula),
        CONSTRAINT FK_Alumnos_Sistemas FOREIGN KEY (IdSistema) REFERENCES dbo.CatalogoSistemas (IdSistema),
        CONSTRAINT FK_Alumnos_Carreras FOREIGN KEY (IdCarrera) REFERENCES dbo.CatalogoCarreras (IdCarrera)
    );
    -- Índice único parcial: el correo debe ser único SOLO cuando existe.
    -- (SQL Server no permite UNIQUE normal en una columna NULL con más de
    -- una fila NULL, y aquí sí puede haber varios alumnos sin correo aún).
    CREATE UNIQUE NONCLUSTERED INDEX UQ_Alumnos_CorreoInstitucional
        ON dbo.Alumnos (CorreoInstitucional)
        WHERE CorreoInstitucional IS NOT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Alumnos_CorreoOtp')
    CREATE NONCLUSTERED INDEX IX_Alumnos_CorreoOtp ON dbo.Alumnos (CorreoOtp);
GO

-- ─── AlumnoCredenciales — solo para alumnos con cuenta local con contraseña ───
-- Cardinalidad 1 a 0..1: no todo alumno tiene contraseña local (la mayoría
-- valida contra el SIIA y nunca registra una). Separarla de Alumnos evita
-- una columna de contraseña casi siempre en NULL en la tabla principal y
-- aísla el dato más sensible en su propia tabla. IdAlumno es a la vez PK y FK
-- (clave primaria compartida): así se garantiza la cardinalidad 1:0..1 sin
-- necesitar una columna IdCredencial aparte.
IF OBJECT_ID(N'dbo.AlumnoCredenciales', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AlumnoCredenciales (
        IdAlumno           INT             NOT NULL,
        ContrasenaHash     VARBINARY(256)  NOT NULL,
        ContrasenaSalt     VARBINARY(128)  NOT NULL,
        Estado             NVARCHAR(20)    NOT NULL CONSTRAINT DF_AlumnoCredenciales_Estado DEFAULT (N'REGISTRADO'),
        FechaCreacion      DATETIME2(0)    NOT NULL CONSTRAINT DF_AlumnoCredenciales_FechaCreacion DEFAULT (SYSUTCDATETIME()),
        FechaActualizacion DATETIME2(0)    NULL,
        CONSTRAINT PK_AlumnoCredenciales PRIMARY KEY CLUSTERED (IdAlumno),
        CONSTRAINT FK_AlumnoCredenciales_Alumnos FOREIGN KEY (IdAlumno)
            REFERENCES dbo.Alumnos (IdAlumno) ON DELETE CASCADE,
        CONSTRAINT CK_AlumnoCredenciales_Estado CHECK (Estado IN (N'REGISTRADO', N'BLOQUEADO', N'BAJA'))
    );
END
GO

-- =============================================================================
-- SECCIÓN 2.5 — Corrección defensiva de codificación en CatalogoCarreras
-- =============================================================================
-- Se detectó que en esta base de datos los nombres de CatalogoCarreras se
-- guardaron con acentos corruptos (p. ej. "Ingeniería" quedó como
-- "IngenierÃ­a" — típico de ejecutar el script UTF-8 con una herramienta que
-- leyó el archivo con otra códificación). Esto rompía el mapeo de carreras de
-- la migración de abajo. Se corrige aquí de forma idempotente (solo si
-- detecta el patrón corrupto) para que el resto del script sea confiable sin
-- importar cómo se haya ejecutado 01_CrearBaseDatos.sql originalmente.
IF EXISTS (SELECT 1 FROM dbo.CatalogoCarreras WHERE Nombre LIKE '%' + NCHAR(195) + '%')
BEGIN
    PRINT '--- Corrigiendo codificación de acentos en CatalogoCarreras ---';
    UPDATE dbo.CatalogoCarreras SET Nombre = N'Ingeniería en Animación Digital y Efectos Visuales' WHERE Clave = N'ANIMACION';
    UPDATE dbo.CatalogoCarreras SET Nombre = N'Ingeniería en Sistemas Computacionales' WHERE Clave = N'ISC';
    UPDATE dbo.CatalogoCarreras SET Nombre = N'Ingeniería Industrial' WHERE Clave = N'INDUSTRIAL';
    UPDATE dbo.CatalogoCarreras SET Nombre = N'Ingeniería Mecatrónica' WHERE Clave = N'MECATRONICA';
    UPDATE dbo.CatalogoCarreras SET Nombre = N'Ingeniería Química' WHERE Clave = N'QUIMICA';
    UPDATE dbo.CatalogoCarreras SET Nombre = N'Licenciatura en Administración' WHERE Clave = N'ADMINISTRACION';
    UPDATE dbo.CatalogoCarreras SET Nombre = N'Licenciatura en Gastronomía' WHERE Clave = N'GASTRONOMIA';
    UPDATE dbo.CatalogoCarreras SET Nombre = N'Ingeniería Industrial modalidad a distancia' WHERE Clave = N'INDUSTRIAL_DISTANCIA';
    UPDATE dbo.CatalogoCarreras SET Nombre = N'Licenciatura en Administración modalidad a distancia' WHERE Clave = N'ADMINISTRACION_DISTANCIA';
END
GO

-- =============================================================================
-- SECCIÓN 3 — Migración de datos V1 → V2 (se salta si ya se migró antes)
-- =============================================================================

IF NOT EXISTS (SELECT 1 FROM dbo.Alumnos)
   AND OBJECT_ID(N'dbo.Usuarios', N'U') IS NOT NULL
BEGIN
    PRINT '--- Migrando Usuarios + CuentasRegistro -> Alumnos ---';

    -- Mapeo best-effort de Usuarios.Carrera (texto libre) al catálogo, sin
    -- inventar relaciones ambiguas: solo mapea coincidencias razonables
    -- ignorando acentos/mayúsculas; lo que no calza queda IdCarrera = NULL
    -- para revisión manual (ver el PRINT de advertencia al final).
    IF OBJECT_ID('tempdb..#MapaCarreras') IS NOT NULL DROP TABLE #MapaCarreras;
    SELECT u.IdUsuario, u.Carrera AS CarreraTexto, c.IdCarrera
    INTO #MapaCarreras
    FROM dbo.Usuarios u
    LEFT JOIN dbo.CatalogoCarreras c
        ON LOWER(REPLACE(REPLACE(c.Nombre, N'í', N'i'), N'ó', N'o'))
           LIKE '%' + LOWER(REPLACE(REPLACE(u.Carrera, N'í', N'i'), N'ó', N'o')) + '%';

    -- Un alumno = una matrícula, exista en Usuarios, en CuentasRegistro o en
    -- ambas (UNION ya elimina el duplicado de matrícula entre las dos tablas).
    ;WITH Matriculas AS (
        SELECT Matricula FROM dbo.Usuarios
        UNION
        SELECT Matricula FROM dbo.CuentasRegistro
    )
    INSERT INTO dbo.Alumnos (
        Matricula, NombreCompleto, FechaNacimiento, CorreoInstitucional, CorreoOtp,
        IdSistema, IdCarrera, Equivalencias, Activo, FechaRegistro, UltimoAcceso,
        IntentosFallidosOtp, BloqueadoHasta
    )
    SELECT
        m.Matricula,
        -- CuentasRegistro tiene datos capturados por el propio alumno al
        -- registrarse (más confiables) -> gana si existe; si no, se usa el
        -- perfil sincronizado desde el SIIA en Usuarios.
        COALESCE(
            NULLIF(LTRIM(RTRIM(CONCAT(cr.Nombres, N' ', cr.ApellidoPaterno, N' ', cr.ApellidoMaterno))), N''),
            u.NombreCompleto,
            m.Matricula
        ),
        cr.FechaNacimiento,
        COALESCE(cr.CorreoInstitucional, u.CorreoInstitucional),
        u.CorreoOtp,
        cr.IdSistema,
        COALESCE(cr.IdCarrera, mc.IdCarrera),
        COALESCE(cr.Equivalencias, 0),
        COALESCE(u.Activo, 1),
        COALESCE(u.FechaRegistro, cr.FechaRegistro, SYSUTCDATETIME()),
        u.UltimoAcceso,
        COALESCE(u.IntentosFallidosOtp, 0),
        u.BloqueadoHasta
    FROM Matriculas m
    LEFT JOIN dbo.Usuarios u ON u.Matricula = m.Matricula
    LEFT JOIN dbo.CuentasRegistro cr ON cr.Matricula = m.Matricula
    LEFT JOIN #MapaCarreras mc ON mc.IdUsuario = u.IdUsuario;

    -- Credenciales: solo para quienes vinieron de CuentasRegistro (única
    -- fuente que alguna vez tuvo contraseña local).
    INSERT INTO dbo.AlumnoCredenciales (IdAlumno, ContrasenaHash, ContrasenaSalt, Estado, FechaCreacion, FechaActualizacion)
    SELECT a.IdAlumno, cr.ContrasenaHash, cr.ContrasenaSalt, cr.Estado, cr.FechaRegistro, cr.FechaActualizacion
    FROM dbo.CuentasRegistro cr
    JOIN dbo.Alumnos a ON a.Matricula = cr.Matricula;

    DECLARE @SinCarrera NVARCHAR(MAX) = (
        SELECT STRING_AGG(CONCAT(a.Matricula, ' (', ISNULL(u.Carrera, '?'), ')'), ', ')
        FROM dbo.Alumnos a
        JOIN dbo.Usuarios u ON u.Matricula = a.Matricula
        WHERE a.IdCarrera IS NULL
    );
    IF @SinCarrera IS NOT NULL
        PRINT '⚠ No se pudo mapear la carrera de (texto libre no reconocido, revisar manualmente): ' + @SinCarrera;

    DECLARE @TotalAlumnos INT = (SELECT COUNT(*) FROM dbo.Alumnos);
    DECLARE @TotalCredenciales INT = (SELECT COUNT(*) FROM dbo.AlumnoCredenciales);
    PRINT CONCAT('--- Migración completa: ', @TotalAlumnos, ' alumnos, ', @TotalCredenciales, ' con credenciales locales ---');

    DROP TABLE #MapaCarreras;
END
GO

-- =============================================================================
-- SECCIÓN 4 — OtpHistorial y SesionesLogin: quitar Matricula redundante
-- =============================================================================
-- Ambas tablas estaban vacías en producción/desarrollo al momento de migrar
-- (ningún flujo del backend las usa todavía — el OTP se genera y valida hoy
-- desde la app, ver AuthRepository.kt), así que se recrean directamente
-- apuntando a Alumnos en vez de reconciliar filas.

IF OBJECT_ID(N'dbo.OtpHistorial', N'U') IS NOT NULL AND OBJECT_ID(N'dbo.Alumnos', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.OtpHistorial', 'Matricula') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.OtpHistorial)
        THROW 51000, 'OtpHistorial tiene datos: revisar migración manual antes de continuar.', 1;

    DROP TABLE dbo.OtpHistorial;
END
GO

IF OBJECT_ID(N'dbo.OtpHistorial', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OtpHistorial (
        IdOtp         BIGINT       IDENTITY(1,1) NOT NULL,
        IdAlumno      INT          NOT NULL,
        CorreoDestino NVARCHAR(255) NOT NULL,
        CodigoHash    CHAR(64)     NOT NULL, -- SHA-256 en hexadecimal = 64 caracteres exactos (antes NVARCHAR(128), sobredimensionado)
        EnviadoEn     DATETIME2(0) NOT NULL CONSTRAINT DF_Otp_Enviado DEFAULT (SYSUTCDATETIME()),
        ExpiraEn      DATETIME2(0) NOT NULL,
        VerificadoEn  DATETIME2(0) NULL,
        Estado        NVARCHAR(20) NOT NULL CONSTRAINT DF_Otp_Estado DEFAULT (N'PENDIENTE'),
        CONSTRAINT PK_OtpHistorial PRIMARY KEY CLUSTERED (IdOtp),
        CONSTRAINT FK_OtpHistorial_Alumnos FOREIGN KEY (IdAlumno) REFERENCES dbo.Alumnos (IdAlumno),
        CONSTRAINT CK_OtpHistorial_Estado CHECK (Estado IN (N'PENDIENTE', N'VERIFICADO', N'EXPIRADO', N'FALLIDO'))
    );
    CREATE NONCLUSTERED INDEX IX_OtpHistorial_Alumno ON dbo.OtpHistorial (IdAlumno, EnviadoEn DESC);
END
GO

IF OBJECT_ID(N'dbo.SesionesLogin', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.SesionesLogin', 'Matricula') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.SesionesLogin)
        THROW 51001, 'SesionesLogin tiene datos: revisar migración manual antes de continuar.', 1;

    DROP TABLE dbo.SesionesLogin;
END
GO

IF OBJECT_ID(N'dbo.SesionesLogin', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SesionesLogin (
        IdSesion      BIGINT       IDENTITY(1,1) NOT NULL,
        IdAlumno      INT          NOT NULL,
        InicioSesion  DATETIME2(0) NOT NULL CONSTRAINT DF_Sesion_Inicio DEFAULT (SYSUTCDATETIME()),
        FinSesion     DATETIME2(0) NULL,
        IpDispositivo NVARCHAR(45) NULL,
        Latitud       DECIMAL(9,6) NULL,
        Longitud      DECIMAL(9,6) NULL,
        CONSTRAINT PK_SesionesLogin PRIMARY KEY CLUSTERED (IdSesion),
        CONSTRAINT FK_SesionesLogin_Alumnos FOREIGN KEY (IdAlumno) REFERENCES dbo.Alumnos (IdAlumno)
    );
    CREATE NONCLUSTERED INDEX IX_SesionesLogin_Alumno ON dbo.SesionesLogin (IdAlumno, InicioSesion DESC);
END
GO

-- =============================================================================
-- SECCIÓN 5 — AuditoriaMovimientos: agregar referencia opcional a Alumnos
-- =============================================================================
-- Se deja ActorMatricula/ActorNombre como snapshot de texto A PROPÓSITO — una
-- bitácora de auditoría debe conservar el nombre tal como era en el momento
-- del movimiento, aunque el alumno cambie de nombre después. Se añade
-- IdAlumno NULLABLE solo para poder hacer JOIN cuando se conozca el alumno,
-- sin sacrificar el registro histórico inmutable.

IF COL_LENGTH('dbo.AuditoriaMovimientos', 'IdAlumno') IS NULL
BEGIN
    ALTER TABLE dbo.AuditoriaMovimientos ADD IdAlumno INT NULL;
    ALTER TABLE dbo.AuditoriaMovimientos
        ADD CONSTRAINT FK_Auditoria_Alumnos FOREIGN KEY (IdAlumno) REFERENCES dbo.Alumnos (IdAlumno);
END
GO

-- Backfill en su propio batch: una columna agregada arriba no es visible
-- para el resto de sentencias hasta el siguiente GO.
UPDATE au
SET au.IdAlumno = a.IdAlumno
FROM dbo.AuditoriaMovimientos au
JOIN dbo.Alumnos a ON a.Matricula = au.ActorMatricula
WHERE au.IdAlumno IS NULL;
GO

-- =============================================================================
-- SECCIÓN 6 — Retirar tablas V1 (se renombran, no se eliminan — respaldo)
-- =============================================================================

IF OBJECT_ID(N'dbo.Usuarios', N'U') IS NOT NULL AND OBJECT_ID(N'dbo._ObsoletoV1_Usuarios', N'U') IS NULL
    EXEC sp_rename N'dbo.Usuarios', N'_ObsoletoV1_Usuarios';
GO
IF OBJECT_ID(N'dbo.CuentasRegistro', N'U') IS NOT NULL AND OBJECT_ID(N'dbo._ObsoletoV1_CuentasRegistro', N'U') IS NULL
    EXEC sp_rename N'dbo.CuentasRegistro', N'_ObsoletoV1_CuentasRegistro';
GO

-- =============================================================================
-- SECCIÓN 7 — Dominio académico (nuevo, listo para cuando se integre el SIIA)
-- =============================================================================
-- Reemplaza en el backend lo que hoy vive hardcodeado en Kotlin
-- (PlanDeEstudiosIsc.kt, GruposIsc.kt, HistorialAcademico.kt) — mismos datos
-- ya validados contra el Kardex y la Tira de Materias reales del alumno, para
-- que cuando exista scraping real del SIIA solo haya que llenar estas tablas,
-- sin rediseñar nada.

IF OBJECT_ID(N'dbo.PlanEstudioMaterias', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PlanEstudioMaterias (
        IdMateria INT IDENTITY(1,1) NOT NULL,
        IdCarrera INT          NOT NULL,
        Nombre    NVARCHAR(150) NOT NULL,
        Creditos  TINYINT      NOT NULL,
        Semestre  TINYINT      NOT NULL,
        Activo    BIT          NOT NULL CONSTRAINT DF_PlanEstudioMaterias_Activo DEFAULT (1),
        CONSTRAINT PK_PlanEstudioMaterias PRIMARY KEY CLUSTERED (IdMateria),
        CONSTRAINT UQ_PlanEstudioMaterias_CarreraNombre UNIQUE (IdCarrera, Nombre),
        CONSTRAINT FK_PlanEstudioMaterias_Carreras FOREIGN KEY (IdCarrera) REFERENCES dbo.CatalogoCarreras (IdCarrera),
        CONSTRAINT CK_PlanEstudioMaterias_Semestre CHECK (Semestre BETWEEN 1 AND 12)
    );
END
GO

IF OBJECT_ID(N'dbo.Grupos', N'U') IS NULL
BEGIN
    -- Clave tal cual la usa el SIIA: {semestre}{carrera}{turno}{numero}, ej. "9ISC23".
    -- Semestre/Turno/Numero se guardan como columnas propias (no se vuelven a
    -- derivar con una expresión regular cada vez que se necesitan).
    CREATE TABLE dbo.Grupos (
        IdGrupo   INT IDENTITY(1,1) NOT NULL,
        Clave     NVARCHAR(15) NOT NULL,
        IdCarrera INT          NOT NULL,
        Semestre  TINYINT      NOT NULL,
        IdTurno   INT          NOT NULL,
        Numero    TINYINT      NOT NULL,
        IdPeriodo INT          NOT NULL,
        Activo    BIT          NOT NULL CONSTRAINT DF_Grupos_Activo DEFAULT (1),
        CONSTRAINT PK_Grupos PRIMARY KEY CLUSTERED (IdGrupo),
        CONSTRAINT UQ_Grupos_Clave UNIQUE (Clave),
        CONSTRAINT FK_Grupos_Carreras FOREIGN KEY (IdCarrera) REFERENCES dbo.CatalogoCarreras (IdCarrera),
        CONSTRAINT FK_Grupos_Turnos FOREIGN KEY (IdTurno) REFERENCES dbo.CatalogoTurnos (IdTurno),
        CONSTRAINT FK_Grupos_Periodos FOREIGN KEY (IdPeriodo) REFERENCES dbo.CatalogoPeriodos (IdPeriodo)
    );
END
GO

-- ─── HistorialAcademico — Kardex, Tira de Materias y Calificaciones leen de AQUÍ
-- Cada fila = una materia que un alumno cursó/está cursando/debe cursar, en un
-- tipo de evaluación concreto. El formato oficial del Kardex repite 3 veces
-- "Calificación + Periodo" (Evaluación / Curso Repetición / Curso Especial)
-- como columnas — aquí cada una de esas 3 es una FILA distinta (TipoRegistro),
-- así una materia puede tener su intento normal y, si aplica, una fila extra
-- de repetición, sin columnas que casi siempre quedan vacías.
IF OBJECT_ID(N'dbo.HistorialAcademico', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.HistorialAcademico (
        IdHistorial        BIGINT IDENTITY(1,1) NOT NULL,
        IdAlumno           INT            NOT NULL,
        IdMateria          INT            NOT NULL,
        IdPeriodo          INT            NULL, -- NULL = todavía sin periodo asignado (por cursar a futuro)
        IdGrupo            INT            NULL,
        TipoRegistro       NVARCHAR(20)   NOT NULL CONSTRAINT DF_Historial_Tipo DEFAULT (N'EVALUACION'),
        Calificacion       DECIMAL(5,2)   NULL,
        IdEstatus          INT            NOT NULL,
        FechaRegistro      DATETIME2(0)   NOT NULL CONSTRAINT DF_Historial_FechaRegistro DEFAULT (SYSUTCDATETIME()),
        FechaActualizacion DATETIME2(0)   NULL,
        CONSTRAINT PK_HistorialAcademico PRIMARY KEY CLUSTERED (IdHistorial),
        CONSTRAINT FK_Historial_Alumnos FOREIGN KEY (IdAlumno) REFERENCES dbo.Alumnos (IdAlumno),
        CONSTRAINT FK_Historial_Materias FOREIGN KEY (IdMateria) REFERENCES dbo.PlanEstudioMaterias (IdMateria),
        CONSTRAINT FK_Historial_Periodos FOREIGN KEY (IdPeriodo) REFERENCES dbo.CatalogoPeriodos (IdPeriodo),
        CONSTRAINT FK_Historial_Grupos FOREIGN KEY (IdGrupo) REFERENCES dbo.Grupos (IdGrupo),
        CONSTRAINT FK_Historial_Estatus FOREIGN KEY (IdEstatus) REFERENCES dbo.CatalogoEstatusMateria (IdEstatus),
        CONSTRAINT UQ_Historial_AlumnoMateriaTipo UNIQUE (IdAlumno, IdMateria, TipoRegistro),
        CONSTRAINT CK_Historial_TipoRegistro CHECK (TipoRegistro IN (N'EVALUACION', N'REPETICION', N'ESPECIAL'))
    );
    CREATE NONCLUSTERED INDEX IX_Historial_AlumnoPeriodo ON dbo.HistorialAcademico (IdAlumno, IdPeriodo);
END
GO

-- ─── Semillas: mismos datos ya validados contra el Kardex/Tira reales ─────────

IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoPeriodos WHERE Anio = 2026 AND Numero = 2)
    INSERT INTO dbo.CatalogoPeriodos (Anio, Numero) VALUES (2026, 2);
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoPeriodos WHERE Anio = 2026 AND Numero = 1)
    INSERT INTO dbo.CatalogoPeriodos (Anio, Numero) VALUES (2026, 1);
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoPeriodos WHERE Anio = 2022 AND Numero = 2)
    INSERT INTO dbo.CatalogoPeriodos (Anio, Numero) VALUES (2022, 2);
GO

IF NOT EXISTS (SELECT 1 FROM dbo.PlanEstudioMaterias WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'ISC'))
BEGIN
    DECLARE @IdISC INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'ISC');

    INSERT INTO dbo.PlanEstudioMaterias (IdCarrera, Nombre, Creditos, Semestre) VALUES
    (@IdISC, N'Cálculo Diferencial', 5, 1),
    (@IdISC, N'Fundamentos de Investigación', 4, 1),
    (@IdISC, N'Fundamentos de Programación', 5, 1),
    (@IdISC, N'Matemáticas Discretas', 5, 1),
    (@IdISC, N'Taller de Administración', 4, 1),
    (@IdISC, N'Taller de Ética', 4, 1),
    (@IdISC, N'Álgebra Lineal', 5, 2),
    (@IdISC, N'Cálculo Integral', 5, 2),
    (@IdISC, N'Contabilidad Financiera', 4, 2),
    (@IdISC, N'Probabilidad y Estadística', 5, 2),
    (@IdISC, N'Programación Orientada a Objetos', 5, 2),
    (@IdISC, N'Química', 4, 2),
    (@IdISC, N'Cálculo Vectorial', 5, 3),
    (@IdISC, N'Cultura Empresarial', 4, 3),
    (@IdISC, N'Desarrollo Sustentable', 5, 3),
    (@IdISC, N'Estructura de Datos', 5, 3),
    (@IdISC, N'Física General', 5, 3),
    (@IdISC, N'Investigación de Operaciones', 4, 3),
    (@IdISC, N'Ecuaciones Diferenciales', 5, 4),
    (@IdISC, N'Fundamento de Base de Datos', 5, 4),
    (@IdISC, N'Métodos Numéricos', 4, 4),
    (@IdISC, N'Principios Eléctricos y Aplicaciones Digitales', 5, 4),
    (@IdISC, N'Simulación', 5, 4),
    (@IdISC, N'Tópicos Avanzados de Programación', 5, 4),
    (@IdISC, N'Arquitectura de Computadoras', 5, 5),
    (@IdISC, N'Fundamentos de Ingeniería de Software', 4, 5),
    (@IdISC, N'Fundamentos de Telecomunicaciones', 4, 5),
    (@IdISC, N'Graficación', 4, 5),
    (@IdISC, N'Sistemas Operativos', 4, 5),
    (@IdISC, N'Taller de Base de Datos', 4, 5),
    (@IdISC, N'Administración de Base de Datos', 5, 6),
    (@IdISC, N'Ingeniería de Software', 5, 6),
    (@IdISC, N'Lenguajes de Interfaz', 4, 6),
    (@IdISC, N'Lenguajes y Autómatas I', 5, 6),
    (@IdISC, N'Redes de Computadoras', 5, 6),
    (@IdISC, N'Taller de Sistemas Operativos', 4, 6),
    (@IdISC, N'Tecnologías Emergentes de Base de Datos', 6, 6),
    (@IdISC, N'Conmutación y Enrutamiento en Redes de Datos', 5, 7),
    (@IdISC, N'Gestión de Proyectos de Software', 6, 7),
    (@IdISC, N'Lenguajes y Autómatas II', 5, 7),
    (@IdISC, N'Minería de Datos', 6, 7),
    (@IdISC, N'Programación Web', 5, 7),
    (@IdISC, N'Sistemas Programables', 4, 7),
    (@IdISC, N'Taller de Investigación I', 4, 7),
    (@IdISC, N'Administración de Redes', 4, 8),
    (@IdISC, N'Ingeniería del Conocimiento', 6, 8),
    (@IdISC, N'Inteligencia Artificial', 4, 8),
    (@IdISC, N'Inteligencia de Negocios y Analítica de Negocios', 7, 8),
    (@IdISC, N'Programación Lógica y Funcional', 4, 8),
    (@IdISC, N'Servicio Social', 10, 8),
    (@IdISC, N'Taller de Investigación II', 4, 8),
    (@IdISC, N'Actividades Complementarias', 5, 9),
    (@IdISC, N'Residencias Profesionales', 10, 9);
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Grupos)
BEGIN
    DECLARE @IdISC2 INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'ISC');
    DECLARE @IdPeriodo2026_2 INT = (SELECT IdPeriodo FROM dbo.CatalogoPeriodos WHERE Anio = 2026 AND Numero = 2);
    DECLARE @IdMatutino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 1);
    DECLARE @IdVespertino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 2);

    ;WITH ClavesGrupo AS (
        SELECT * FROM (VALUES
            (N'1ISC11', 1, 1, 1), (N'1ISC12', 1, 1, 2), (N'1ISC21', 1, 2, 1),
            (N'2ISC11', 2, 1, 1), (N'2ISC21', 2, 2, 1),
            (N'3ISC11', 3, 1, 1), (N'3ISC12', 3, 1, 2), (N'3ISC21', 3, 2, 1),
            (N'4ISC11', 4, 1, 1), (N'4ISC21', 4, 2, 1),
            (N'5ISC11', 5, 1, 1), (N'5ISC12', 5, 1, 2), (N'5ISC21', 5, 2, 1),
            (N'6ISC11', 6, 1, 1), (N'6ISC21', 6, 2, 1),
            (N'7ISC21', 7, 2, 1), (N'7ISC22', 7, 2, 2),
            (N'8ISC21', 8, 2, 1), (N'8ISC22', 8, 2, 2),
            (N'9ISC23', 9, 2, 3)
        ) AS t(Clave, Semestre, CodigoTurno, Numero)
    )
    INSERT INTO dbo.Grupos (Clave, IdCarrera, Semestre, IdTurno, Numero, IdPeriodo)
    SELECT cg.Clave, @IdISC2, cg.Semestre,
           CASE cg.CodigoTurno WHEN 1 THEN @IdMatutino ELSE @IdVespertino END,
           cg.Numero, @IdPeriodo2026_2
    FROM ClavesGrupo cg;
END
GO

-- =============================================================================
-- SECCIÓN 8 — Vista de conveniencia
-- =============================================================================
-- Une Alumno + Materia + Periodo + Grupo + Estatus en una sola consulta —
-- Kardex, Tira de Materias y Calificaciones son, cada una, un filtro distinto
-- sobre esta misma vista (igual que HistorialAcademico.materiasPara(...) del
-- lado de la app: una sola fuente, cada pantalla filtra distinto).
CREATE OR ALTER VIEW dbo.vw_HistorialAcademico AS
SELECT
    al.IdAlumno,
    al.Matricula,
    al.NombreCompleto,
    ca.Nombre       AS Carrera,
    pm.IdMateria,
    pm.Nombre       AS Materia,
    pm.Creditos,
    pm.Semestre,
    h.TipoRegistro,
    h.Calificacion,
    per.Etiqueta    AS Periodo,
    g.Clave         AS Grupo,
    es.Codigo       AS EstatusCodigo,
    es.Nombre       AS EstatusNombre
FROM dbo.HistorialAcademico h
JOIN dbo.Alumnos al                ON al.IdAlumno = h.IdAlumno
JOIN dbo.PlanEstudioMaterias pm     ON pm.IdMateria = h.IdMateria
JOIN dbo.CatalogoEstatusMateria es  ON es.IdEstatus = h.IdEstatus
LEFT JOIN dbo.CatalogoCarreras ca   ON ca.IdCarrera = al.IdCarrera
LEFT JOIN dbo.CatalogoPeriodos per  ON per.IdPeriodo = h.IdPeriodo
LEFT JOIN dbo.Grupos g              ON g.IdGrupo = h.IdGrupo;
GO

PRINT 'AppTeschiDB — migración V2 (normalización) aplicada correctamente.';
GO
