-- =============================================================================
-- AppTESCHI — Grupos reales: Ingeniería Mecatrónica
-- Archivo: 09_SeedGruposMecatronica.sql
-- Fuente: "Horario Mecatronica grupos 26-2.pdf" — Horario general 2026-2, TESCHI
-- Relacionado con: [[Base_Datos_Usuarios]]
--
-- Clave real de grupo confirmada en el documento: {semestre}IM{turno}{numero},
-- ej. "1IM11" = semestre 1, carrera IM (Ingeniería Mecatrónica), turno 1
-- (matutino), grupo 1. Turno confirmado cruzando cada grupo contra su franja
-- de horario real en el PDF (ej. 1IM21 tiene clases de 14:00 a 21:00).
--
-- Igual que Industrial, Mecatrónica sí tiene grupo de 9° semestre (9IM21)
-- con horario de aula regular, a diferencia de ISC/Animación.
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.Grupos
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'MECATRONICA')
)
BEGIN
    DECLARE @IdMecatronica INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'MECATRONICA');
    DECLARE @IdPeriodo2026_2 INT = (SELECT IdPeriodo FROM dbo.CatalogoPeriodos WHERE Anio = 2026 AND Numero = 2);
    DECLARE @IdMatutino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 1);
    DECLARE @IdVespertino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 2);

    ;WITH ClavesGrupo AS (
        SELECT * FROM (VALUES
            (N'1IM11', 1, 1, 1), (N'1IM12', 1, 1, 2), (N'1IM21', 1, 2, 1),
            (N'2IM11', 2, 1, 1),
            (N'3IM11', 3, 1, 1), (N'3IM12', 3, 1, 2), (N'3IM21', 3, 2, 1),
            (N'4IM11', 4, 1, 1),
            (N'5IM11', 5, 1, 1), (N'5IM21', 5, 2, 1),
            (N'6IM11', 6, 1, 1),
            (N'7IM21', 7, 2, 1),
            (N'8IM21', 8, 2, 1),
            (N'9IM21', 9, 2, 1)
        ) AS t(Clave, Semestre, CodigoTurno, Numero)
    )
    INSERT INTO dbo.Grupos (Clave, IdCarrera, Semestre, IdTurno, Numero, IdPeriodo)
    SELECT cg.Clave, @IdMecatronica, cg.Semestre,
           CASE cg.CodigoTurno WHEN 1 THEN @IdMatutino ELSE @IdVespertino END,
           cg.Numero, @IdPeriodo2026_2
    FROM ClavesGrupo cg;

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.Grupos WHERE IdCarrera = @IdMecatronica);
    PRINT CONCAT('--- Grupos de Mecatrónica sembrados: ', @Total, ' ---');
END
ELSE
BEGIN
    PRINT '--- Los grupos de Mecatrónica ya estaban sembrados, no se volvieron a insertar ---';
END
GO
