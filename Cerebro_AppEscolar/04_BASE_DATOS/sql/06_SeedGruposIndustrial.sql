-- =============================================================================
-- AppTESCHI — Grupos reales: Ingeniería Industrial
-- Archivo: 06_SeedGruposIndustrial.sql
-- Fuente: "Horarios_Industrial.pdf" — HORARIOS IIND 2026-2, TESCHI
-- Relacionado con: [[Base_Datos_Usuarios]]
--
-- Clave real de grupo confirmada en el documento: {semestre}II{turno}{numero},
-- ej. "1II11" = semestre 1, carrera II (Ingeniería Industrial — así abrevia
-- el SIIA esta carrera), turno 1 (matutino), grupo 1. Turno confirmado
-- cruzando cada grupo contra su franja de horario real en el PDF.
--
-- A diferencia de ISC y Animación, aquí SÍ hay grupos de 9° semestre
-- (9II21, 9II22) — el documento muestra clases de aula regular en ese
-- semestre (Ingeniería de Calidad, etc.), no solo residencia profesional.
--
-- NOTA: esta carrera todavía NO tiene su plan de estudios sembrado en
-- PlanEstudioMaterias — el alumno aún no confirmó los créditos por materia.
-- Grupos no depende de PlanEstudioMaterias (solo de CatalogoCarreras), así
-- que se puede sembrar por separado sin problema.
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.Grupos
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'INDUSTRIAL')
)
BEGIN
    DECLARE @IdIndustrial INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'INDUSTRIAL');
    DECLARE @IdPeriodo2026_2 INT = (SELECT IdPeriodo FROM dbo.CatalogoPeriodos WHERE Anio = 2026 AND Numero = 2);
    DECLARE @IdMatutino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 1);
    DECLARE @IdVespertino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 2);

    ;WITH ClavesGrupo AS (
        SELECT * FROM (VALUES
            (N'1II11', 1, 1, 1), (N'1II12', 1, 1, 2), (N'1II21', 1, 2, 1),
            (N'2II11', 2, 1, 1),
            (N'3II11', 3, 1, 1), (N'3II12', 3, 1, 2), (N'3II21', 3, 2, 1),
            (N'4II11', 4, 1, 1),
            (N'5II11', 5, 1, 1), (N'5II21', 5, 2, 1),
            (N'6II11', 6, 1, 1),
            (N'7II11', 7, 1, 1), (N'7II12', 7, 1, 2),
            (N'8II21', 8, 2, 1),
            (N'9II21', 9, 2, 1), (N'9II22', 9, 2, 2)
        ) AS t(Clave, Semestre, CodigoTurno, Numero)
    )
    INSERT INTO dbo.Grupos (Clave, IdCarrera, Semestre, IdTurno, Numero, IdPeriodo)
    SELECT cg.Clave, @IdIndustrial, cg.Semestre,
           CASE cg.CodigoTurno WHEN 1 THEN @IdMatutino ELSE @IdVespertino END,
           cg.Numero, @IdPeriodo2026_2
    FROM ClavesGrupo cg;

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.Grupos WHERE IdCarrera = @IdIndustrial);
    PRINT CONCAT('--- Grupos de Ingeniería Industrial sembrados: ', @Total, ' ---');
END
ELSE
BEGIN
    PRINT '--- Los grupos de Ingeniería Industrial ya estaban sembrados, no se volvieron a insertar ---';
END
GO
