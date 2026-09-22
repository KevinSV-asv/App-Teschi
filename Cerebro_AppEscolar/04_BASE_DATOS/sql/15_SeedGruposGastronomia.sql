-- =============================================================================
-- AppTESCHI — Grupos reales: Licenciatura en Gastronomía
-- Archivo: 15_SeedGruposGastronomia.sql
-- Fuente: "HORARIOS DE GRUPOS GASTRONOMIA 2026-2.pdf", TESCHI
-- Relacionado con: [[Base_Datos_Usuarios]]
--
-- Clave real de grupo confirmada en el documento: {semestre}LG{turno}{numero},
-- ej. "1LG11" = semestre 1, carrera LG (Licenciatura en Gastronomía), turno
-- 1 (matutino), grupo 1. Turno confirmado cruzando cada grupo contra su
-- franja de horario real (ej. 1LG21 empieza a las 13:00).
--
-- Igual que ISC/Animación/Química, no tiene grupo de 9° semestre con
-- horario de aula regular (solo Residencias/Actividades Complementarias).
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.Grupos
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'GASTRONOMIA')
)
BEGIN
    DECLARE @IdGastronomia INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'GASTRONOMIA');
    DECLARE @IdPeriodo2026_2 INT = (SELECT IdPeriodo FROM dbo.CatalogoPeriodos WHERE Anio = 2026 AND Numero = 2);
    DECLARE @IdMatutino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 1);
    DECLARE @IdVespertino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 2);

    ;WITH ClavesGrupo AS (
        SELECT * FROM (VALUES
            (N'1LG11', 1, 1, 1), (N'1LG12', 1, 1, 2), (N'1LG13', 1, 1, 3),
            (N'1LG21', 1, 2, 1), (N'1LG22', 1, 2, 2), (N'1LG23', 1, 2, 3),
            (N'2LG11', 2, 1, 1), (N'2LG12', 2, 1, 2), (N'2LG21', 2, 2, 1),
            (N'3LG11', 3, 1, 1), (N'3LG12', 3, 1, 2), (N'3LG13', 3, 1, 3),
            (N'3LG21', 3, 2, 1), (N'3LG22', 3, 2, 2),
            (N'4LG11', 4, 1, 1), (N'4LG12', 4, 1, 2), (N'4LG21', 4, 2, 1),
            (N'5LG11', 5, 1, 1), (N'5LG12', 5, 1, 2), (N'5LG13', 5, 1, 3),
            (N'5LG21', 5, 2, 1), (N'5LG22', 5, 2, 2),
            (N'6LG11', 6, 1, 1), (N'6LG21', 6, 2, 1),
            (N'7LG11', 7, 1, 1), (N'7LG12', 7, 1, 2), (N'7LG21', 7, 2, 1),
            (N'8LG21', 8, 2, 1)
        ) AS t(Clave, Semestre, CodigoTurno, Numero)
    )
    INSERT INTO dbo.Grupos (Clave, IdCarrera, Semestre, IdTurno, Numero, IdPeriodo)
    SELECT cg.Clave, @IdGastronomia, cg.Semestre,
           CASE cg.CodigoTurno WHEN 1 THEN @IdMatutino ELSE @IdVespertino END,
           cg.Numero, @IdPeriodo2026_2
    FROM ClavesGrupo cg;

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.Grupos WHERE IdCarrera = @IdGastronomia);
    PRINT CONCAT('--- Grupos de Gastronomía sembrados: ', @Total, ' ---');
END
ELSE
BEGIN
    PRINT '--- Los grupos de Gastronomía ya estaban sembrados, no se volvieron a insertar ---';
END
GO
