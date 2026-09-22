-- Fase 2 de Reinscripción (ver 02_MODULOS/01_Reinscripcion.md): observaciones
-- de reglamento que un admin/director registra sobre un alumno. Mientras una
-- observación siga PENDIENTE, el alumno queda bloqueado para reinscribirse
-- hasta que el director revise su caso y la autorice o la rechace — mismo
-- flujo descrito en las instrucciones reales del SIIA ("Si corrompiste algún
-- punto de nuestro reglamento, tendrás que presentar con tu director de
-- carrera, el revisara tu caso y el te autoriza la reinscripción si aun
-- puedes"). Idempotente: se puede volver a correr sin duplicar la tabla.

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'ObservacionesReglamento')
BEGIN
    CREATE TABLE dbo.ObservacionesReglamento (
        IdObservacion   INT IDENTITY(1,1) PRIMARY KEY,
        IdAlumno        INT NOT NULL REFERENCES dbo.Alumnos(IdAlumno),
        Motivo          NVARCHAR(500) NOT NULL,
        -- PENDIENTE: bloquea la reinscripción hasta que el director decida.
        -- AUTORIZADA / RECHAZADA: ya se resolvió, deja de bloquear.
        Estado          NVARCHAR(20) NOT NULL DEFAULT N'PENDIENTE',
        RegistradaPor   NVARCHAR(50) NOT NULL,
        FechaRegistro   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        ResueltaPor     NVARCHAR(50) NULL,
        FechaResolucion DATETIME2 NULL,
        Resolucion      NVARCHAR(500) NULL
    );

    CREATE INDEX IX_ObservacionesReglamento_IdAlumno ON dbo.ObservacionesReglamento(IdAlumno);
END
GO
