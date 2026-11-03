-- ============================================================
-- AdministradoresSiia.sql
-- ⚠️ PENDIENTE — NO APLICADA en el servidor (Fase 5 del módulo 09).
--    Al aplicarla, renómbrala con el siguiente número libre y muévela a sql/.
-- Fase 5 del módulo 09 — Login unificado con el SIIA
-- Vincula cuentas de administrativo del SIIA con registros
-- existentes en la tabla Administradores de AppTeschiDB.
--
-- El vínculo lo hace EXCLUSIVAMENTE un SUPERADMIN desde la app.
-- Sin el vínculo, una cuenta de "administrativo" del SIIA
-- NO puede entrar al panel (ver /api/auth/siia).
--
-- Idempotente: se puede correr varias veces sin error.
-- Compatible con SQL Server 2016+.
-- ============================================================

-- ── 1. Administradores.UsuarioSiia ───────────────────────────
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Administradores') AND name = N'UsuarioSiia'
)
BEGIN
    ALTER TABLE dbo.Administradores
        ADD UsuarioSiia NVARCHAR(50) NULL;
    PRINT 'Columna Administradores.UsuarioSiia agregada.';
END
ELSE
    PRINT 'Administradores.UsuarioSiia ya existe — omitida.';
GO

-- Un usuario SIIA solo puede estar vinculado a un administrador
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.Administradores') AND name = N'UX_Administradores_UsuarioSiia'
)
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UX_Administradores_UsuarioSiia
        ON dbo.Administradores (UsuarioSiia)
        WHERE UsuarioSiia IS NOT NULL;
    PRINT 'Índice UX_Administradores_UsuarioSiia creado.';
END
ELSE
    PRINT 'Índice UX_Administradores_UsuarioSiia ya existe — omitido.';
GO

PRINT '26_AdministradoresSiia.sql completado.';
