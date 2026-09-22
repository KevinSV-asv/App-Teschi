-- =============================================================================
-- AppTESCHI — Grupos reales: Ingeniería Química
-- Archivo: 11_SeedGruposQuimica.sql
-- Fuente: "Horarios Quimica.pdf" — 2026-2, TESCHI (PDF escaneado, leído
-- página por página con OCR visual — pymupdf para renderizar, sin capa de
-- texto extraíble)
-- Relacionado con: [[Base_Datos_Usuarios]]
--
-- Clave real de grupo confirmada en el documento: {semestre}IQ{turno}{numero},
-- ej. "1IQ11" = semestre 1, carrera IQ (Ingeniería Química), turno 1
-- (matutino), grupo 1. Turno confirmado cruzando cada grupo contra su franja
-- de horario real (ej. 1IQ21 va de 13:00 a 21:00).
--
-- Igual que ISC y Animación, Química NO tiene grupo de 9° semestre con
-- horario de aula regular (solo Residencias Profesionales, sin salón fijo).
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.Grupos
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'QUIMICA')
)
BEGIN
    DECLARE @IdQuimica INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'QUIMICA');
    DECLARE @IdPeriodo2026_2 INT = (SELECT IdPeriodo FROM dbo.CatalogoPeriodos WHERE Anio = 2026 AND Numero = 2);
    DECLARE @IdMatutino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 1);
    DECLARE @IdVespertino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 2);

    ;WITH ClavesGrupo AS (
        SELECT * FROM (VALUES
            (N'1IQ11', 1, 1, 1), (N'1IQ12', 1, 1, 2), (N'1IQ21', 1, 2, 1),
            (N'2IQ11', 2, 1, 1),
            (N'3IQ11', 3, 1, 1), (N'3IQ12', 3, 1, 2), (N'3IQ21', 3, 2, 1),
            (N'4IQ11', 4, 1, 1),
            (N'5IQ11', 5, 1, 1), (N'5IQ21', 5, 2, 1),
            (N'6IQ11', 6, 1, 1),
            (N'7IQ11', 7, 1, 1),
            (N'8IQ21', 8, 2, 1)
        ) AS t(Clave, Semestre, CodigoTurno, Numero)
    )
    INSERT INTO dbo.Grupos (Clave, IdCarrera, Semestre, IdTurno, Numero, IdPeriodo)
    SELECT cg.Clave, @IdQuimica, cg.Semestre,
           CASE cg.CodigoTurno WHEN 1 THEN @IdMatutino ELSE @IdVespertino END,
           cg.Numero, @IdPeriodo2026_2
    FROM ClavesGrupo cg;

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.Grupos WHERE IdCarrera = @IdQuimica);
    PRINT CONCAT('--- Grupos de Química sembrados: ', @Total, ' ---');
END
ELSE
BEGIN
    PRINT '--- Los grupos de Química ya estaban sembrados, no se volvieron a insertar ---';
END
GO
