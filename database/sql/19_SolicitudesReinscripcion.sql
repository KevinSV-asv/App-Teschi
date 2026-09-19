-- Solicitudes de reinscripción reales enviadas por el alumno (Fase 1 del
-- módulo de Reinscripción — ver 02_MODULOS/01_Reinscripcion.md). Antes el
-- botón "Reinscribir" de la app solo generaba un folio local y no guardaba
-- nada; ahora cada solicitud queda registrada de verdad, ligada al alumno
-- y al grupo real que eligió (o que el sistema le asignó, si es regular).
-- Idempotente: se puede volver a correr sin duplicar la tabla.

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'SolicitudesReinscripcion')
BEGIN
    CREATE TABLE dbo.SolicitudesReinscripcion (
        IdSolicitud    INT IDENTITY(1,1) PRIMARY KEY,
        IdAlumno       INT NOT NULL REFERENCES dbo.Alumnos(IdAlumno),
        IdGrupo        INT NOT NULL REFERENCES dbo.Grupos(IdGrupo),
        Folio          NVARCHAR(20) NOT NULL UNIQUE,
        -- PENDIENTE: recién enviada, falta confirmarse de manera presencial
        -- (ver instrucciones reales del SIIA: "LA INSCRIPCION SERA DE MANERA
        -- PRESENCIAL"). CONFIRMADA / CANCELADA las gestiona control escolar.
        Estatus        NVARCHAR(20) NOT NULL DEFAULT N'PENDIENTE',
        FechaSolicitud DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );

    CREATE INDEX IX_SolicitudesReinscripcion_IdAlumno ON dbo.SolicitudesReinscripcion(IdAlumno);
END
GO
