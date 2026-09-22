-- =============================================================================
-- AppTESCHI — Base de datos SQL Server Express
-- Archivo: 01_CrearBaseDatos.sql
-- Descripción: Crea la base de datos y el esquema ORIGINAL V1 (Usuarios,
-- CuentasRegistro, catálogos). Se conserva tal cual por historial — el
-- esquema vigente es V2, ver 03_MigracionV2_Normalizacion.sql, que renombra
-- `Usuarios`/`CuentasRegistro` a `_ObsoletoV1_*` y las reemplaza por una
-- única tabla `Alumnos`. Orden de ejecución en una base nueva: 01 → 03 → 02.
-- Relacionado con: [[Base_Datos_Usuarios]] en bóveda Cerebro_AppEscolar
-- =============================================================================

-- Ejecutar en SQL Server Management Studio conectado a tu instancia Express
-- Ejemplo: localhost\SQLEXPRESS

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'AppTeschiDB')
BEGIN
    CREATE DATABASE AppTeschiDB;
END
GO

USE AppTeschiDB;
GO

-- ─── Tabla principal de usuarios ─────────────────────────────────────────────
-- NOTA: La contraseña del SIIA NO se almacena aquí.
--       La autenticación real sigue siendo contra el portal ASP.NET del SIIA.
--       Esta tabla guarda el perfil del alumno en AppTESCHI.

IF OBJECT_ID(N'dbo.Usuarios', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Usuarios (
        IdUsuario           INT             IDENTITY(1,1)   NOT NULL,
        Matricula           NVARCHAR(20)    NOT NULL,
        CorreoOtp           NVARCHAR(255)   NOT NULL,
        CorreoInstitucional NVARCHAR(255)   NULL,
        NombreCompleto      NVARCHAR(200)   NULL,
        Carrera             NVARCHAR(150)   NULL,
        Activo              BIT             NOT NULL        CONSTRAINT DF_Usuarios_Activo DEFAULT (1),
        FechaRegistro       DATETIME2(0)    NOT NULL        CONSTRAINT DF_Usuarios_FechaRegistro DEFAULT (SYSUTCDATETIME()),
        UltimoAcceso        DATETIME2(0)    NULL,
        IntentosFallidosOtp TINYINT         NOT NULL        CONSTRAINT DF_Usuarios_Intentos DEFAULT (0),
        BloqueadoHasta      DATETIME2(0)    NULL,
        CONSTRAINT PK_Usuarios PRIMARY KEY CLUSTERED (IdUsuario),
        CONSTRAINT UQ_Usuarios_Matricula UNIQUE (Matricula)
    );
END
GO

-- ─── Historial de códigos OTP enviados ───────────────────────────────────────
IF OBJECT_ID(N'dbo.OtpHistorial', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OtpHistorial (
        IdOtp               BIGINT          IDENTITY(1,1)   NOT NULL,
        IdUsuario           INT             NULL,
        Matricula           NVARCHAR(20)    NOT NULL,
        CorreoDestino       NVARCHAR(255)   NOT NULL,
        CodigoHash          NVARCHAR(128)   NOT NULL,   -- hash SHA-256 del OTP (nunca texto plano)
        EnviadoEn           DATETIME2(0)    NOT NULL    CONSTRAINT DF_Otp_Enviado DEFAULT (SYSUTCDATETIME()),
        ExpiraEn            DATETIME2(0)    NOT NULL,
        VerificadoEn        DATETIME2(0)    NULL,
        Estado              NVARCHAR(20)    NOT NULL    CONSTRAINT DF_Otp_Estado DEFAULT (N'PENDIENTE'),
        -- Estados válidos: PENDIENTE | VERIFICADO | EXPIRADO | FALLIDO
        CONSTRAINT PK_OtpHistorial PRIMARY KEY CLUSTERED (IdOtp),
        CONSTRAINT FK_OtpHistorial_Usuarios FOREIGN KEY (IdUsuario)
            REFERENCES dbo.Usuarios (IdUsuario)
    );
END
GO

-- ─── Sesiones de login exitosas ──────────────────────────────────────────────
IF OBJECT_ID(N'dbo.SesionesLogin', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SesionesLogin (
        IdSesion            BIGINT          IDENTITY(1,1)   NOT NULL,
        IdUsuario           INT             NOT NULL,
        Matricula           NVARCHAR(20)    NOT NULL,
        InicioSesion        DATETIME2(0)    NOT NULL    CONSTRAINT DF_Sesion_Inicio DEFAULT (SYSUTCDATETIME()),
        FinSesion           DATETIME2(0)    NULL,
        IpDispositivo       NVARCHAR(45)    NULL,
        Latitud             DECIMAL(9,6)    NULL,
        Longitud            DECIMAL(9,6)    NULL,
        CONSTRAINT PK_SesionesLogin PRIMARY KEY CLUSTERED (IdSesion),
        CONSTRAINT FK_SesionesLogin_Usuarios FOREIGN KEY (IdUsuario)
            REFERENCES dbo.Usuarios (IdUsuario)
    );
END
GO

-- ─── Índices de rendimiento ──────────────────────────────────────────────────
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Usuarios_CorreoOtp')
    CREATE NONCLUSTERED INDEX IX_Usuarios_CorreoOtp ON dbo.Usuarios (CorreoOtp);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_OtpHistorial_Matricula')
    CREATE NONCLUSTERED INDEX IX_OtpHistorial_Matricula ON dbo.OtpHistorial (Matricula, EnviadoEn DESC);
GO

IF OBJECT_ID(N'dbo.AuditoriaMovimientos', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AuditoriaMovimientos (
        IdAuditoria     BIGINT IDENTITY(1,1) NOT NULL,
        ActorMatricula  NVARCHAR(50)  NOT NULL,
        ActorNombre     NVARCHAR(200) NOT NULL,
        Accion          NVARCHAR(80)  NOT NULL,
        Entidad         NVARCHAR(80)  NOT NULL,
        Detalle         NVARCHAR(1000) NULL,
        FechaMovimiento DATETIME2(0) NOT NULL CONSTRAINT DF_Auditoria_Fecha DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT PK_AuditoriaMovimientos PRIMARY KEY CLUSTERED (IdAuditoria)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_AuditoriaMovimientos_Fecha')
    CREATE NONCLUSTERED INDEX IX_AuditoriaMovimientos_Fecha ON dbo.AuditoriaMovimientos (FechaMovimiento DESC);
GO

IF OBJECT_ID(N'dbo.CatalogoSistemas', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CatalogoSistemas (
        IdSistema INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CatalogoSistemas PRIMARY KEY,
        Clave NVARCHAR(30) NOT NULL CONSTRAINT UQ_CatalogoSistemas_Clave UNIQUE,
        Nombre NVARCHAR(100) NOT NULL,
        Activo BIT NOT NULL CONSTRAINT DF_CatalogoSistemas_Activo DEFAULT (1)
    );
END
GO

IF OBJECT_ID(N'dbo.CatalogoCarreras', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CatalogoCarreras (
        IdCarrera INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CatalogoCarreras PRIMARY KEY,
        Clave NVARCHAR(30) NOT NULL CONSTRAINT UQ_CatalogoCarreras_Clave UNIQUE,
        Nombre NVARCHAR(180) NOT NULL,
        IdSistema INT NULL,
        Activo BIT NOT NULL CONSTRAINT DF_CatalogoCarreras_Activo DEFAULT (1),
        CONSTRAINT FK_CatalogoCarreras_Sistemas FOREIGN KEY (IdSistema) REFERENCES dbo.CatalogoSistemas(IdSistema)
    );
END
GO

IF OBJECT_ID(N'dbo.CuentasRegistro', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CuentasRegistro (
        IdCuenta INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CuentasRegistro PRIMARY KEY,
        Matricula NVARCHAR(20) NOT NULL CONSTRAINT UQ_CuentasRegistro_Matricula UNIQUE,
        Nombres NVARCHAR(120) NOT NULL,
        ApellidoPaterno NVARCHAR(80) NOT NULL,
        ApellidoMaterno NVARCHAR(80) NOT NULL,
        FechaNacimiento DATE NOT NULL,
        CorreoInstitucional NVARCHAR(255) NOT NULL CONSTRAINT UQ_CuentasRegistro_Correo UNIQUE,
        IdSistema INT NOT NULL,
        IdCarrera INT NOT NULL,
        Equivalencias BIT NOT NULL CONSTRAINT DF_CuentasRegistro_Equivalencias DEFAULT (0),
        ContrasenaHash VARBINARY(256) NOT NULL,
        ContrasenaSalt VARBINARY(128) NOT NULL,
        Estado NVARCHAR(20) NOT NULL CONSTRAINT DF_CuentasRegistro_Estado DEFAULT (N'REGISTRADO'),
        FechaRegistro DATETIME2(0) NOT NULL CONSTRAINT DF_CuentasRegistro_Fecha DEFAULT (SYSUTCDATETIME()),
        FechaActualizacion DATETIME2(0) NULL,
        CONSTRAINT FK_CuentasRegistro_Sistemas FOREIGN KEY (IdSistema) REFERENCES dbo.CatalogoSistemas(IdSistema),
        CONSTRAINT FK_CuentasRegistro_Carreras FOREIGN KEY (IdCarrera) REFERENCES dbo.CatalogoCarreras(IdCarrera)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoSistemas WHERE Clave = N'ESCOLARIZADO')
    INSERT INTO dbo.CatalogoSistemas (Clave, Nombre) VALUES (N'ESCOLARIZADO', N'Sistema Escolarizado');
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoSistemas WHERE Clave = N'ABIERTO')
    INSERT INTO dbo.CatalogoSistemas (Clave, Nombre) VALUES (N'ABIERTO', N'Sistema Abierto');
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoSistemas WHERE Clave = N'DUAL')
    INSERT INTO dbo.CatalogoSistemas (Clave, Nombre) VALUES (N'DUAL', N'Modelo Dual');
GO

IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoCarreras WHERE Clave = N'ANIMACION')
    INSERT INTO dbo.CatalogoCarreras (Clave, Nombre) VALUES (N'ANIMACION', N'Ingeniería en Animación Digital y Efectos Visuales');
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoCarreras WHERE Clave = N'ISC')
    INSERT INTO dbo.CatalogoCarreras (Clave, Nombre) VALUES (N'ISC', N'Ingeniería en Sistemas Computacionales');
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoCarreras WHERE Clave = N'INDUSTRIAL')
    INSERT INTO dbo.CatalogoCarreras (Clave, Nombre) VALUES (N'INDUSTRIAL', N'Ingeniería Industrial');
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoCarreras WHERE Clave = N'MECATRONICA')
    INSERT INTO dbo.CatalogoCarreras (Clave, Nombre) VALUES (N'MECATRONICA', N'Ingeniería Mecatrónica');
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoCarreras WHERE Clave = N'QUIMICA')
    INSERT INTO dbo.CatalogoCarreras (Clave, Nombre) VALUES (N'QUIMICA', N'Ingeniería Química');
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoCarreras WHERE Clave = N'ADMINISTRACION')
    INSERT INTO dbo.CatalogoCarreras (Clave, Nombre) VALUES (N'ADMINISTRACION', N'Licenciatura en Administración');
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoCarreras WHERE Clave = N'GASTRONOMIA')
    INSERT INTO dbo.CatalogoCarreras (Clave, Nombre) VALUES (N'GASTRONOMIA', N'Licenciatura en Gastronomía');
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoCarreras WHERE Clave = N'INDUSTRIAL_DISTANCIA')
    INSERT INTO dbo.CatalogoCarreras (Clave, Nombre) VALUES (N'INDUSTRIAL_DISTANCIA', N'Ingeniería Industrial modalidad a distancia');
IF NOT EXISTS (SELECT 1 FROM dbo.CatalogoCarreras WHERE Clave = N'ADMINISTRACION_DISTANCIA')
    INSERT INTO dbo.CatalogoCarreras (Clave, Nombre) VALUES (N'ADMINISTRACION_DISTANCIA', N'Licenciatura en Administración modalidad a distancia');
GO

PRINT 'AppTeschiDB — esquema creado correctamente.';
GO
