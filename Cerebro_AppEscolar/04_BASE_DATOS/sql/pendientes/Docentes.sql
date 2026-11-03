-- ============================================================
-- Docentes.sql
-- ⚠️ PENDIENTE — NO APLICADA en el servidor (Fase 3 del módulo 09).
--    Al aplicarla, renómbrala con el siguiente número libre y muévela a sql/.
-- Fase 3 del módulo 09 — Login unificado con el SIIA
-- Crea la tabla Docentes y agrega la FK en OtpHistorial.
--
-- Idempotente: se puede correr varias veces sin error.
-- Compatible con SQL Server 2016+.
-- ============================================================

-- ── 1. Tabla Docentes ────────────────────────────────────────
IF OBJECT_ID(N'dbo.Docentes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Docentes (
        IdDocente       INT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_Docentes PRIMARY KEY,

        -- Identidad del SIIA
        UsuarioSiia     NVARCHAR(50)  NOT NULL     -- usuario / número de control del SIIA
            CONSTRAINT UX_Docentes_UsuarioSiia UNIQUE,
        NumUsuario      NVARCHAR(30)  NULL,         -- número de empleado del SIIA
        NombreCompleto  NVARCHAR(200) NOT NULL,
        Correo          NVARCHAR(255) NULL,          -- correo del SIIA (puede estar vacío)

        -- Área académica (del SIIA)
        IdArea          INT           NULL,
        Area            NVARCHAR(150) NULL,

        -- Nombre libre tal como aparece en la columna Profesor del Excel de horarios.
        -- El administrador lo asigna una vez en AdminDocentesScreen (Fase 4).
        -- NULL = aún no mapeado; el horario del docente aparecerá vacío.
        NombreEnHorario NVARCHAR(200) NULL,

        -- Control
        Activo          BIT           NOT NULL CONSTRAINT DF_Docentes_Activo DEFAULT 1,
        UltimoAcceso    DATETIME2(0)  NULL,
        CreadoEn        DATETIME2(0)  NOT NULL CONSTRAINT DF_Docentes_CreadoEn DEFAULT (SYSUTCDATETIME())
    );
    PRINT 'Tabla Docentes creada.';
END
ELSE
    PRINT 'Tabla Docentes ya existe — omitida.';
GO

-- ── 2. OtpHistorial.IdDocente ────────────────────────────────
-- Permite enviar y verificar el OTP de un docente.
-- La lógica de /api/otp/enviar ya soporta IdAlumno e IdAdministrador como NULL;
-- el docente se suma de la misma forma.
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.OtpHistorial') AND name = N'IdDocente'
)
BEGIN
    ALTER TABLE dbo.OtpHistorial
        ADD IdDocente INT NULL
            CONSTRAINT FK_OtpHistorial_Docente
            REFERENCES dbo.Docentes (IdDocente);
    PRINT 'Columna OtpHistorial.IdDocente agregada.';
END
ELSE
    PRINT 'OtpHistorial.IdDocente ya existe — omitida.';
GO

PRINT '25_Docentes.sql completado.';
