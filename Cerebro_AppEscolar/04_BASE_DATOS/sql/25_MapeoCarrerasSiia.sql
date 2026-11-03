-- ============================================================
-- 25_MapeoCarrerasSiia.sql
-- Equivalencia de carreras SIIA -> AppTeschiDB (DEC-037).
--
-- La columna CatalogoCarreras.IdCarreraSiia y su índice único
-- UX_CatalogoCarreras_IdCarreraSiia ya los crea 24_VinculoSiia.sql.
-- Este script solo:
--   1. Quita el índice duplicado UQ_CatalogoCarreras_IdCarreraSiia,
--      que se creó por error el 27/09/2026 al correr una primera
--      versión de este archivo (entonces llamada 24_MapeoCarrerasSiia.sql)
--      sin saber que 24_VinculoSiia.sql ya estaba aplicado.
--   2. Registra las equivalencias confirmadas con logins reales.
--
-- El SIIA no tiene catálogo de carreras: el idCarrera de cada carrera
-- solo se conoce cuando alguien de esa carrera entra. El backend lo deja
-- en el log como [AUTH-SIIA][PENDIENTE] ... idCarreraSiia=N. Para mapear
-- una carrera nueva basta agregar un UPDATE abajo y volver a correr este
-- archivo (es idempotente).
--
-- Ejecutar en el servidor (ver 04_Manual_Mantenimiento §7):
--   sqlcmd -S 127.0.0.1 -U appteschi_api -P '<DB_PASSWORD>' -d AppTeschiDB -C -f 65001 -i 25_MapeoCarrerasSiia.sql
-- ============================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;   -- obligatorio: la tabla tiene índices filtrados
GO

IF EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.CatalogoCarreras') AND name = N'UQ_CatalogoCarreras_IdCarreraSiia'
)
BEGIN
    DROP INDEX UQ_CatalogoCarreras_IdCarreraSiia ON dbo.CatalogoCarreras;
    PRINT 'Índice duplicado UQ_CatalogoCarreras_IdCarreraSiia eliminado.';
END
ELSE
    PRINT 'No hay índice duplicado — omitido.';
GO

-- Equivalencias confirmadas (idCarrera que devuelve POST /login.ashx)
-- | Clave local | idCarrera SIIA | Confirmado con                       |
-- |-------------|----------------|--------------------------------------|
-- | ISC         | 4              | login real de alumno ISC, 27/09/2026 |
UPDATE dbo.CatalogoCarreras SET IdCarreraSiia = 4 WHERE Clave = N'ISC' AND (IdCarreraSiia IS NULL OR IdCarreraSiia <> 4);
GO

SELECT Clave, IdCarreraSiia FROM dbo.CatalogoCarreras ORDER BY Clave;
GO
