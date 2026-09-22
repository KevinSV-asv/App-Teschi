-- =============================================================================
-- AppTESCHI — Grupos reales: Ingeniería en Animación Digital y Efectos Visuales
-- Archivo: 05_SeedGruposAnimacion.sql
-- Ejecutar después de 04_SeedMateriasAnimacion.sql
-- Fuente: "Horarios Animacion.pdf" — Asignación de Horario 2026-2, TESCHI
-- Relacionado con: [[Base_Datos_Usuarios]]
--
-- Clave real de grupo confirmada en el documento de horarios:
-- {semestre}{carrera}{turno}{numero}, ej. "1ADYEV11" = semestre 1, carrera
-- ADYEV (Animación Digital y Efectos Visuales — así abrevia el SIIA esta
-- carrera, NO "ANIMACION"), turno 1 (matutino), grupo 1. Mismo formato y
-- misma lógica de derivación que los grupos de ISC (turno 1=matutino,
-- 2=vespertino, confirmado cruzando el horario de cada grupo contra su
-- franja de horas en el PDF).
--
-- El documento no incluye un grupo para 9° semestre (igual que ISC: el
-- semestre 9 no tiene horario de aula regular, ahí solo hay una materia,
-- "Motion Graphics Avanzado", que no aparece en este documento).
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.Grupos
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'ANIMACION')
)
BEGIN
    DECLARE @IdAnimacion INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'ANIMACION');
    DECLARE @IdPeriodo2026_2 INT = (SELECT IdPeriodo FROM dbo.CatalogoPeriodos WHERE Anio = 2026 AND Numero = 2);
    DECLARE @IdMatutino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 1);
    DECLARE @IdVespertino INT = (SELECT IdTurno FROM dbo.CatalogoTurnos WHERE Codigo = 2);

    ;WITH ClavesGrupo AS (
        SELECT * FROM (VALUES
            (N'1ADYEV11', 1, 1, 1), (N'1ADYEV12', 1, 1, 2), (N'1ADYEV21', 1, 2, 1), (N'1ADYEV22', 1, 2, 2),
            (N'2ADYEV11', 2, 1, 1), (N'2ADYEV21', 2, 2, 1), (N'2ADYEV22', 2, 2, 2),
            (N'3ADYEV11', 3, 1, 1), (N'3ADYEV12', 3, 1, 2), (N'3ADYEV21', 3, 2, 1),
            (N'4ADYEV11', 4, 1, 1), (N'4ADYEV21', 4, 2, 1),
            (N'5ADYEV11', 5, 1, 1), (N'5ADYEV21', 5, 2, 1),
            (N'6ADYEV11', 6, 1, 1), (N'6ADYEV12', 6, 1, 2), (N'6ADYEV21', 6, 2, 1),
            (N'7ADYEV11', 7, 1, 1), (N'7ADYEV21', 7, 2, 1),
            (N'8ADYEV11', 8, 1, 1), (N'8ADYEV21', 8, 2, 1)
        ) AS t(Clave, Semestre, CodigoTurno, Numero)
    )
    INSERT INTO dbo.Grupos (Clave, IdCarrera, Semestre, IdTurno, Numero, IdPeriodo)
    SELECT cg.Clave, @IdAnimacion, cg.Semestre,
           CASE cg.CodigoTurno WHEN 1 THEN @IdMatutino ELSE @IdVespertino END,
           cg.Numero, @IdPeriodo2026_2
    FROM ClavesGrupo cg;

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.Grupos WHERE IdCarrera = @IdAnimacion);
    PRINT CONCAT('--- Grupos de Animación sembrados: ', @Total, ' ---');
END
ELSE
BEGIN
    PRINT '--- Los grupos de Animación ya estaban sembrados, no se volvieron a insertar ---';
END
GO
