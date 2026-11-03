-- ============================================================
-- 24_VinculoSiia.sql
-- Fase 2 del módulo 09 — Login unificado con el SIIA
-- Vincula alumnos con sus credenciales del SIIA y agrega la
-- equivalencia de carreras entre SIIA y AppTeschiDB.
--
-- ✅ Aplicada en el servidor el 23/09/2026 (con SET QUOTED_IDENTIFIER ON).
-- Nota: OrigenCuenta = 'SIIA' quedó sin uso — el alta automática se
-- retiró el 25/09/2026 (DEC-036); solo se vinculan alumnos ya existentes.
--
-- Idempotente: se puede correr varias veces sin error.
-- Compatible con SQL Server 2016+.
-- ============================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- ── 1. Alumnos.OrigenCuenta ──────────────────────────────────
-- 'PROPIA'  → registrado con la pantalla de registro de AppTESCHI
-- 'SIIA'    → alta automática al primer login por el SIIA
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Alumnos') AND name = N'OrigenCuenta'
)
BEGIN
    ALTER TABLE dbo.Alumnos
        ADD OrigenCuenta NVARCHAR(10) NOT NULL
            CONSTRAINT DF_Alumnos_OrigenCuenta DEFAULT N'PROPIA'
            CONSTRAINT CK_Alumnos_OrigenCuenta CHECK (OrigenCuenta IN (N'PROPIA', N'SIIA'));
    PRINT 'Columna Alumnos.OrigenCuenta agregada.';
END
ELSE
    PRINT 'Alumnos.OrigenCuenta ya existe — omitida.';
GO

-- ── 2. Alumnos.UsuarioSiia ──────────────────────────────────
-- Número de control / usuario SIIA del alumno. Para la mayoría
-- coincide con la matrícula, pero puede diferir.
-- Nulo mientras no se haya hecho el primer login por el SIIA.
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Alumnos') AND name = N'UsuarioSiia'
)
BEGIN
    ALTER TABLE dbo.Alumnos
        ADD UsuarioSiia NVARCHAR(50) NULL;
    PRINT 'Columna Alumnos.UsuarioSiia agregada.';
END
ELSE
    PRINT 'Alumnos.UsuarioSiia ya existe — omitida.';
GO

-- Índice único (parcial: solo filas con valor)
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.Alumnos') AND name = N'UX_Alumnos_UsuarioSiia'
)
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UX_Alumnos_UsuarioSiia
        ON dbo.Alumnos (UsuarioSiia)
        WHERE UsuarioSiia IS NOT NULL;
    PRINT 'Índice UX_Alumnos_UsuarioSiia creado.';
END
ELSE
    PRINT 'Índice UX_Alumnos_UsuarioSiia ya existe — omitido.';
GO

-- ── 3. CatalogoCarreras.IdCarreraSiia ───────────────────────
-- ID de carrera tal como viene en el campo `idCarrera` del
-- SIIA. Permite mapear "la carrera 5 del SIIA" → "ISC en
-- AppTeschiDB" sin depender del nombre, que puede variar.
-- Nulo si la carrera no tiene equivalencia mapeada todavía.
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.CatalogoCarreras') AND name = N'IdCarreraSiia'
)
BEGIN
    ALTER TABLE dbo.CatalogoCarreras
        ADD IdCarreraSiia INT NULL;
    PRINT 'Columna CatalogoCarreras.IdCarreraSiia agregada.';
END
ELSE
    PRINT 'CatalogoCarreras.IdCarreraSiia ya existe — omitida.';
GO

-- Índice único parcial (una carrera SIIA = una carrera AppTESCHI)
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.CatalogoCarreras') AND name = N'UX_CatalogoCarreras_IdCarreraSiia'
)
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UX_CatalogoCarreras_IdCarreraSiia
        ON dbo.CatalogoCarreras (IdCarreraSiia)
        WHERE IdCarreraSiia IS NOT NULL;
    PRINT 'Índice UX_CatalogoCarreras_IdCarreraSiia creado.';
END
ELSE
    PRINT 'Índice UX_CatalogoCarreras_IdCarreraSiia ya existe — omitido.';
GO

PRINT '24_VinculoSiia.sql completado.';
