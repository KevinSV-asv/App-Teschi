-- =============================================================================
-- AppTESCHI — Plan de estudios: Licenciatura en Administración
-- Archivo: 12_SeedMateriasAdministracion.sql
-- Fuente: lista oficial de materias por semestre (con créditos) proporcionada
-- por el alumno. Suma total verificada: 260 créditos, y la suma de CADA
-- semestre coincide exactamente con el total de créditos por columna que
-- aparece al pie de la retícula oficial LADM-2010-234 que también compartió
-- (ej. semestre 1 = 27, semestre 9 = 33 — ambos coinciden).
-- Relacionado con: [[Base_Datos_Usuarios]]
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.PlanEstudioMaterias
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'ADMINISTRACION')
)
BEGIN
    DECLARE @IdAdministracion INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'ADMINISTRACION');

    INSERT INTO dbo.PlanEstudioMaterias (IdCarrera, Nombre, Creditos, Semestre) VALUES
    -- Semestre 1
    (@IdAdministracion, N'Teoría General de la Administración', 4, 1),
    (@IdAdministracion, N'Informática para la Administración', 5, 1),
    (@IdAdministracion, N'Taller de Ética', 4, 1),
    (@IdAdministracion, N'Fundamentos de Investigación', 4, 1),
    (@IdAdministracion, N'Matemáticas Aplicadas a la Administración', 5, 1),
    (@IdAdministracion, N'Contabilidad General', 5, 1),
    -- Semestre 2
    (@IdAdministracion, N'Función Administrativa I', 5, 2),
    (@IdAdministracion, N'Estadística para la Administración I', 5, 2),
    (@IdAdministracion, N'Derecho Laboral y Seguridad Social', 5, 2),
    (@IdAdministracion, N'Comunicación Corporativa', 4, 2),
    (@IdAdministracion, N'Taller de Desarrollo Humano', 4, 2),
    (@IdAdministracion, N'Costos de Manufactura', 5, 2),
    -- Semestre 3
    (@IdAdministracion, N'Función Administrativa II', 5, 3),
    (@IdAdministracion, N'Estadística para la Administración II', 5, 3),
    (@IdAdministracion, N'Derecho Empresarial', 5, 3),
    (@IdAdministracion, N'Comportamiento Organizacional', 5, 3),
    (@IdAdministracion, N'Dinámica Social', 4, 3),
    (@IdAdministracion, N'Contabilidad Gerencial', 5, 3),
    -- Semestre 4
    (@IdAdministracion, N'Gestión Estratégica del Capital Humano I', 5, 4),
    (@IdAdministracion, N'Procesos Estructurales', 5, 4),
    (@IdAdministracion, N'Métodos Cuantitativos para Administración', 5, 4),
    (@IdAdministracion, N'Fundamentos de Mercadotecnia', 5, 4),
    (@IdAdministracion, N'Economía Empresarial', 5, 4),
    (@IdAdministracion, N'Matemáticas Financieras', 4, 4),
    -- Semestre 5
    (@IdAdministracion, N'Gestión Estratégica del Capital Humano II', 5, 5),
    (@IdAdministracion, N'Derecho Fiscal', 4, 5),
    (@IdAdministracion, N'Mezcla de Mercadotecnia', 4, 5),
    (@IdAdministracion, N'Macroeconomía', 4, 5),
    (@IdAdministracion, N'Administración Financiera I', 5, 5),
    (@IdAdministracion, N'Desarrollo Sustentable', 5, 5),
    -- Semestre 6
    (@IdAdministracion, N'Gestión de la Retribución', 6, 6),
    (@IdAdministracion, N'Producción', 5, 6),
    (@IdAdministracion, N'Taller de Investigación I', 4, 6),
    (@IdAdministracion, N'Sistemas de Información de Mercadotecnia', 5, 6),
    (@IdAdministracion, N'Innovación y Emprendedurismo', 4, 6),
    (@IdAdministracion, N'Administración Financiera II', 5, 6),
    -- Semestre 7
    (@IdAdministracion, N'Plan de Negocios', 5, 7),
    (@IdAdministracion, N'Procesos de Dirección', 4, 7),
    (@IdAdministracion, N'Taller de Investigación II', 4, 7),
    (@IdAdministracion, N'Administración de la Calidad', 5, 7),
    (@IdAdministracion, N'Economía Internacional', 4, 7),
    (@IdAdministracion, N'Diagnóstico y Evaluación Empresarial', 5, 7),
    -- Semestre 8
    (@IdAdministracion, N'Consultoría Empresarial', 4, 8),
    (@IdAdministracion, N'Formulación y Evaluación de Proyectos', 5, 8),
    (@IdAdministracion, N'Desarrollo Organizacional', 5, 8),
    (@IdAdministracion, N'Seminario de Mercadotecnia', 5, 8),
    (@IdAdministracion, N'Seminario de Publicidad', 4, 8),
    (@IdAdministracion, N'Seminario de Promoción de Ventas', 4, 8),
    (@IdAdministracion, N'Seminario de Administración de Ventas y Clientes', 4, 8),
    -- Semestre 9
    (@IdAdministracion, N'Planeación de Mercadotecnia', 4, 9),
    (@IdAdministracion, N'Mercadotecnia Especializada', 4, 9),
    (@IdAdministracion, N'Residencias Profesionales', 10, 9),
    (@IdAdministracion, N'Servicio Social', 10, 9),
    (@IdAdministracion, N'Otros Créditos', 5, 9);

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdAdministracion);
    DECLARE @Creditos INT = (SELECT SUM(CAST(Creditos AS INT)) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdAdministracion);
    PRINT CONCAT('--- Plan de Administración sembrado: ', @Total, ' materias, ', @Creditos, ' créditos totales ---');
END
ELSE
BEGIN
    PRINT '--- El plan de Administración ya estaba sembrado, no se volvió a insertar ---';
END
GO
