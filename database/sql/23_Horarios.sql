-- Horario de clases por carrera/semestre/grupo (ver 02_MODULOS/00_Dashboard.md,
-- sección "Materias y Horario de Hoy"). No existía ninguna fuente real de esto
-- (día, hora, profesor, aula) — esta tabla la crea. Se llena por importación
-- de un Excel desde el panel de administrador (POST /api/horarios/importar),
-- nunca a mano por el alumno. Idempotente: se puede volver a correr sin
-- duplicar la tabla.

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Horarios')
BEGIN
    CREATE TABLE dbo.Horarios (
        IdHorario     INT IDENTITY(1,1) PRIMARY KEY,
        IdCarrera     INT NOT NULL REFERENCES dbo.CatalogoCarreras(IdCarrera),
        Semestre      TINYINT NOT NULL,
        Grupo         NVARCHAR(10) NULL,
        NombreMateria NVARCHAR(200) NOT NULL,
        -- 1 = Lunes ... 7 = Domingo (ISO 8601), para poder ordenar/filtrar sin
        -- depender del idioma del texto.
        DiaSemana     TINYINT NOT NULL CHECK (DiaSemana BETWEEN 1 AND 7),
        HoraInicio    TIME NOT NULL,
        HoraFin       TIME NOT NULL,
        Profesor      NVARCHAR(200) NULL,
        Aula          NVARCHAR(100) NULL,
        Modalidad     NVARCHAR(20) NOT NULL DEFAULT N'PRESENCIAL',
        PeriodoEtiqueta NVARCHAR(20) NULL,
        CargadoPor    NVARCHAR(50) NOT NULL,
        FechaCarga    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );

    CREATE INDEX IX_Horarios_CarreraSemestreGrupo ON dbo.Horarios(IdCarrera, Semestre, Grupo, DiaSemana);
END
GO
