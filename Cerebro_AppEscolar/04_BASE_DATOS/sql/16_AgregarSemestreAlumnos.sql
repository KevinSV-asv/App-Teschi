-- =============================================================================
-- AppTESCHI — Agrega el semestre actual del alumno
-- Archivo: 16_AgregarSemestreAlumnos.sql
-- Relacionado con: [[Base_Datos_Usuarios]], DEC-016
--
-- `Alumnos` no tenía forma de saber en qué semestre va cada quien. Se agrega
-- como columna nullable (no todo alumno la tiene capturada todavía) — el
-- administrador la asigna al registrar/revisar un alumno. Junto con
-- `IdCarrera` (ya existente) y `PlanEstudioMaterias`, permite calcular qué
-- materias debería tener inscritas un alumno en su ciclo actual.
-- =============================================================================

USE AppTeschiDB;
GO

IF COL_LENGTH('dbo.Alumnos', 'Semestre') IS NULL
BEGIN
    ALTER TABLE dbo.Alumnos ADD Semestre TINYINT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Alumnos_Semestre')
BEGIN
    ALTER TABLE dbo.Alumnos ADD CONSTRAINT CK_Alumnos_Semestre CHECK (Semestre IS NULL OR Semestre BETWEEN 1 AND 12);
END
GO

PRINT 'AppTeschiDB — columna Semestre agregada a Alumnos.';
GO
