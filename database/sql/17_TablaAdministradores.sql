-- =============================================================================
-- 17_TablaAdministradores.sql — Cuentas reales de administrador
-- =============================================================================
-- Hasta ahora NO existía ninguna cuenta de administrador real: el único
-- acceso al panel era un bypass local (matrícula "admin" / contraseña
-- "admin") en la propia app, sin pasar por la base de datos ni por OTP. Este
-- script crea el par de tablas equivalente a Alumnos/AlumnoCredenciales pero
-- para el personal administrativo — un administrador NO es un alumno (no
-- tiene carrera, semestre, ni historial académico), así que se modela como
-- una entidad separada en vez de forzarlo dentro de Alumnos (ver DEC-014
-- sobre por qué este proyecto evita ese tipo de mezcla).
--
-- Ver DEC-019 en DECISIONES_TECNICAS.md.

USE AppTeschiDB;
GO

IF OBJECT_ID(N'dbo.Administradores', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Administradores (
        IdAdministrador     INT             IDENTITY(1,1) NOT NULL,
        Usuario              NVARCHAR(50)    NOT NULL,
        NombreCompleto       NVARCHAR(200)   NOT NULL,
        CorreoInstitucional  NVARCHAR(255)   NULL,
        Rol                  NVARCHAR(20)    NOT NULL CONSTRAINT DF_Administradores_Rol DEFAULT (N'OPERADOR'),
        Activo               BIT             NOT NULL CONSTRAINT DF_Administradores_Activo DEFAULT (1),
        FechaRegistro        DATETIME2(0)    NOT NULL CONSTRAINT DF_Administradores_FechaRegistro DEFAULT (SYSUTCDATETIME()),
        UltimoAcceso         DATETIME2(0)    NULL,
        CONSTRAINT PK_Administradores PRIMARY KEY CLUSTERED (IdAdministrador),
        CONSTRAINT UQ_Administradores_Usuario UNIQUE (Usuario),
        CONSTRAINT CK_Administradores_Rol CHECK (Rol IN (N'SUPERADMIN', N'OPERADOR'))
    );
END
GO

-- Cardinalidad 1:1 obligatoria (a diferencia de AlumnoCredenciales, que es
-- 1:0..1 porque no todo alumno tiene cuenta local) — un administrador SIEMPRE
-- tiene contraseña, por eso aquí sí podría vivir en la misma tabla; se separa
-- igual para mantener el mismo patrón de aislar el dato más sensible.
IF OBJECT_ID(N'dbo.AdministradorCredenciales', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AdministradorCredenciales (
        IdAdministrador     INT             NOT NULL,
        ContrasenaHash       VARBINARY(256)  NOT NULL,
        ContrasenaSalt       VARBINARY(128)  NOT NULL,
        Estado               NVARCHAR(20)    NOT NULL CONSTRAINT DF_AdminCredenciales_Estado DEFAULT (N'REGISTRADO'),
        FechaCreacion        DATETIME2(0)    NOT NULL CONSTRAINT DF_AdminCredenciales_FechaCreacion DEFAULT (SYSUTCDATETIME()),
        FechaActualizacion   DATETIME2(0)    NULL,
        CONSTRAINT PK_AdministradorCredenciales PRIMARY KEY CLUSTERED (IdAdministrador),
        CONSTRAINT FK_AdminCredenciales_Administradores FOREIGN KEY (IdAdministrador)
            REFERENCES dbo.Administradores (IdAdministrador) ON DELETE CASCADE,
        CONSTRAINT CK_AdminCredenciales_Estado CHECK (Estado IN (N'REGISTRADO', N'BLOQUEADO'))
    );
END
GO

PRINT 'AppTeschiDB — tablas Administradores / AdministradorCredenciales listas.';
GO
