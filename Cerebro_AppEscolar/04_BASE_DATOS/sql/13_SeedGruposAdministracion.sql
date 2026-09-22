-- =============================================================================
-- AppTESCHI — Grupos reales: Licenciatura en Administración
-- Archivo: 13_SeedGruposAdministracion.sql
-- Fuente: "HORARIOS Administracion 2026-2.pdf", TESCHI
-- Relacionado con: [[Base_Datos_Usuarios]]
--
-- Clave real de grupo confirmada en el documento: {semestre}LA{turno}{numero},
-- ej. "1LA11" = semestre 1, carrera LA (Licenciatura en Administración),
-- turno 1 (matutino), grupo 1. Turno confirmado cruzando cada grupo contra
-- su franja de horario real (ej. 1LA21 va de 13:00 a 18:00+).
--
-- Es la carrera con más grupos de 9° semestre encontrada hasta ahora:
-- 9LA21, 9LA22, 9LA23 (tres, todos vespertinos).
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.Grupos
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'ADMINISTRACION')
)
BEGIN
    DECLARE @IdAdministracion INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'ADMINISTRACION');
    DECLARE @IdPeriodo2026_2 INT = (SELECT IdPeriodo FROM dbo.CatalogoPeriodos WHERE Anio = 2026 AND Numero = 2);
    DECLARE @IdMatutino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 1);
    DECLARE @IdVespertino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 2);

    ;WITH ClavesGrupo AS (
        SELECT * FROM (VALUES
            (N'1LA11', 1, 1, 1), (N'1LA12', 1, 1, 2), (N'1LA21', 1, 2, 1), (N'1LA22', 1, 2, 2),
            (N'2LA11', 2, 1, 1), (N'2LA21', 2, 2, 1),
            (N'3LA11', 3, 1, 1), (N'3LA12', 3, 1, 2), (N'3LA21', 3, 2, 1), (N'3LA22', 3, 2, 2),
            (N'4LA11', 4, 1, 1), (N'4LA21', 4, 2, 1),
            (N'5LA11', 5, 1, 1), (N'5LA12', 5, 1, 2), (N'5LA21', 5, 2, 1), (N'5LA22', 5, 2, 2),
            (N'6LA11', 6, 1, 1), (N'6LA21', 6, 2, 1),
            (N'7LA11', 7, 1, 1), (N'7LA12', 7, 1, 2), (N'7LA21', 7, 2, 1), (N'7LA22', 7, 2, 2),
            (N'8LA21', 8, 2, 1),
            (N'9LA21', 9, 2, 1), (N'9LA22', 9, 2, 2), (N'9LA23', 9, 2, 3)
        ) AS t(Clave, Semestre, CodigoTurno, Numero)
    )
    INSERT INTO dbo.Grupos (Clave, IdCarrera, Semestre, IdTurno, Numero, IdPeriodo)
    SELECT cg.Clave, @IdAdministracion, cg.Semestre,
           CASE cg.CodigoTurno WHEN 1 THEN @IdMatutino ELSE @IdVespertino END,
           cg.Numero, @IdPeriodo2026_2
    FROM ClavesGrupo cg;

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.Grupos WHERE IdCarrera = @IdAdministracion);
    PRINT CONCAT('--- Grupos de Administración sembrados: ', @Total, ' ---');
END
ELSE
BEGIN
    PRINT '--- Los grupos de Administración ya estaban sembrados, no se volvieron a insertar ---';
END
GO
