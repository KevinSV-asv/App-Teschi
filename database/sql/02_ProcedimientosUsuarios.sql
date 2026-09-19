-- =============================================================================
-- AppTESCHI — Procedimientos almacenados para alumnos
-- Archivo: 02_ProcedimientosUsuarios.sql
-- Ejecutar después de 01_CrearBaseDatos.sql y 03_MigracionV2_Normalizacion.sql
--
-- NOTA: estos procedimientos operan sobre el esquema V2 (tabla `Alumnos`,
-- ver 03_MigracionV2_Normalizacion.sql). Ninguno de ellos lo usa hoy el
-- backend Express (server.js hace sus propias consultas parametrizadas
-- directamente) — se mantienen y actualizan para quien prefiera consumir la
-- base de datos vía procedimientos almacenados en vez de SQL embebido.
-- =============================================================================

USE AppTeschiDB;
GO

-- ─── Registrar o actualizar el perfil de un alumno (p. ej. tras sincronizar
-- desde el SIIA) — NO crea contraseña local, eso es AlumnoCredenciales. ───────
CREATE OR ALTER PROCEDURE dbo.sp_UpsertAlumno
    @Matricula           NVARCHAR(20),
    @CorreoOtp           NVARCHAR(255) = NULL,
    @CorreoInstitucional NVARCHAR(255) = NULL,
    @NombreCompleto      NVARCHAR(200) = NULL,
    @IdCarrera           INT = NULL
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM dbo.Alumnos WHERE Matricula = @Matricula)
    BEGIN
        UPDATE dbo.Alumnos
        SET CorreoOtp           = COALESCE(@CorreoOtp, CorreoOtp),
            CorreoInstitucional = COALESCE(@CorreoInstitucional, CorreoInstitucional),
            NombreCompleto      = COALESCE(@NombreCompleto, NombreCompleto),
            IdCarrera           = COALESCE(@IdCarrera, IdCarrera),
            UltimoAcceso        = SYSUTCDATETIME(),
            Activo              = 1
        WHERE Matricula = @Matricula;
    END
    ELSE
    BEGIN
        INSERT INTO dbo.Alumnos (Matricula, CorreoOtp, CorreoInstitucional, NombreCompleto, IdCarrera, UltimoAcceso)
        VALUES (@Matricula, @CorreoOtp, @CorreoInstitucional, @NombreCompleto, @IdCarrera, SYSUTCDATETIME());
    END

    SELECT IdAlumno, Matricula, CorreoOtp, UltimoAcceso
    FROM dbo.Alumnos
    WHERE Matricula = @Matricula;
END
GO

-- ─── Registrar envío de OTP (guardar solo el hash, nunca el código en claro) ───
CREATE OR ALTER PROCEDURE dbo.sp_RegistrarOtp
    @Matricula      NVARCHAR(20),
    @CorreoDestino  NVARCHAR(255),
    @CodigoHash     CHAR(64),
    @MinutosValidez INT = 10
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @IdAlumno INT = (SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = @Matricula);
    IF @IdAlumno IS NULL
    BEGIN
        RAISERROR(N'Alumno no registrado: %s', 16, 1, @Matricula);
        RETURN;
    END

    INSERT INTO dbo.OtpHistorial (IdAlumno, CorreoDestino, CodigoHash, ExpiraEn)
    VALUES (@IdAlumno, @CorreoDestino, @CodigoHash, DATEADD(MINUTE, @MinutosValidez, SYSUTCDATETIME()));

    SELECT SCOPE_IDENTITY() AS IdOtp;
END
GO

-- ─── Verificar OTP por hash ────────────────────────────────────────────────────
CREATE OR ALTER PROCEDURE dbo.sp_VerificarOtp
    @Matricula  NVARCHAR(20),
    @CodigoHash CHAR(64)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @IdAlumno INT = (SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = @Matricula);
    DECLARE @IdOtp BIGINT;

    SELECT TOP 1 @IdOtp = IdOtp
    FROM dbo.OtpHistorial
    WHERE IdAlumno = @IdAlumno
      AND CodigoHash = @CodigoHash
      AND Estado = N'PENDIENTE'
      AND ExpiraEn > SYSUTCDATETIME()
    ORDER BY EnviadoEn DESC;

    IF @IdOtp IS NULL
    BEGIN
        SELECT 0 AS Verificado;
        RETURN;
    END

    UPDATE dbo.OtpHistorial
    SET Estado = N'VERIFICADO', VerificadoEn = SYSUTCDATETIME()
    WHERE IdOtp = @IdOtp;

    UPDATE dbo.Alumnos
    SET UltimoAcceso = SYSUTCDATETIME(), IntentosFallidosOtp = 0
    WHERE IdAlumno = @IdAlumno;

    SELECT 1 AS Verificado, @IdOtp AS IdOtp;
END
GO

-- ─── Registrar sesión de login ─────────────────────────────────────────────────
CREATE OR ALTER PROCEDURE dbo.sp_IniciarSesion
    @Matricula     NVARCHAR(20),
    @IpDispositivo NVARCHAR(45) = NULL,
    @Latitud       DECIMAL(9,6) = NULL,
    @Longitud      DECIMAL(9,6) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @IdAlumno INT = (SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = @Matricula);
    IF @IdAlumno IS NULL
    BEGIN
        RAISERROR(N'Alumno no registrado: %s', 16, 1, @Matricula);
        RETURN;
    END

    INSERT INTO dbo.SesionesLogin (IdAlumno, IpDispositivo, Latitud, Longitud)
    VALUES (@IdAlumno, @IpDispositivo, @Latitud, @Longitud);

    UPDATE dbo.Alumnos SET UltimoAcceso = SYSUTCDATETIME() WHERE IdAlumno = @IdAlumno;

    SELECT SCOPE_IDENTITY() AS IdSesion;
END
GO

PRINT 'Procedimientos almacenados (V2) creados correctamente.';
GO
select * from dbo.PlanEstudioMaterias;