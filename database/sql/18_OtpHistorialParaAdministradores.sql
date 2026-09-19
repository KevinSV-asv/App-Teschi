-- =============================================================================
-- 18_OtpHistorialParaAdministradores.sql — OTP también para administradores
-- =============================================================================
-- OtpHistorial (creada en la migración V2) solo podía referenciar Alumnos.
-- Ahora que existen cuentas reales de Administrador (17_TablaAdministradores.sql)
-- que también deben pasar por verificación OTP, se generaliza: IdAlumno pasa a
-- ser NULLABLE, se agrega IdAdministrador NULLABLE, y un CHECK garantiza que
-- cada fila pertenece a exactamente uno de los dos (nunca ambos, nunca ninguno).
-- También se agrega Intentos para bloquear fuerza bruta sobre el código
-- directamente en el servidor (antes el conteo de intentos vivía solo en la
-- app, lo cual no protegía nada si alguien llamaba a la API directamente).
--
-- Ver DEC-019 en DECISIONES_TECNICAS.md.

USE AppTeschiDB;
GO

IF EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'OtpHistorial' AND COLUMN_NAME = 'IdAlumno' AND IS_NULLABLE = 'NO'
)
BEGIN
    ALTER TABLE dbo.OtpHistorial ALTER COLUMN IdAlumno INT NULL;
END
GO

IF COL_LENGTH('dbo.OtpHistorial', 'IdAdministrador') IS NULL
BEGIN
    ALTER TABLE dbo.OtpHistorial ADD IdAdministrador INT NULL;
END
GO

IF COL_LENGTH('dbo.OtpHistorial', 'Intentos') IS NULL
BEGIN
    ALTER TABLE dbo.OtpHistorial ADD Intentos TINYINT NOT NULL CONSTRAINT DF_OtpHistorial_Intentos DEFAULT (0);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_OtpHistorial_Administradores')
BEGIN
    ALTER TABLE dbo.OtpHistorial
        ADD CONSTRAINT FK_OtpHistorial_Administradores FOREIGN KEY (IdAdministrador) REFERENCES dbo.Administradores (IdAdministrador);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_OtpHistorial_UnSoloTitular')
BEGIN
    ALTER TABLE dbo.OtpHistorial
        ADD CONSTRAINT CK_OtpHistorial_UnSoloTitular CHECK (
            (IdAlumno IS NOT NULL AND IdAdministrador IS NULL) OR
            (IdAlumno IS NULL AND IdAdministrador IS NOT NULL)
        );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_OtpHistorial_Administrador')
BEGIN
    CREATE NONCLUSTERED INDEX IX_OtpHistorial_Administrador ON dbo.OtpHistorial (IdAdministrador, EnviadoEn DESC);
END
GO

PRINT 'AppTeschiDB — OtpHistorial ahora soporta administradores y cuenta intentos.';
GO
