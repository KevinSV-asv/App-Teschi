-- =============================================================================
-- AppTESCHI — Plan de estudios: Licenciatura en Gastronomía
-- Archivo: 14_SeedMateriasGastronomia.sql
-- Fuente: lista oficial de materias por semestre (con créditos) proporcionada
-- por el alumno + retícula oficial GAST-2010-215.
-- Relacionado con: [[Base_Datos_Usuarios]]
--
-- NOTA: la lista del alumno no incluía "Servicio Social" en ningún
-- semestre, pero la retícula oficial sí lo marca como categoría aparte (10
-- créditos, igual que en las demás carreras). Sumando los 52 valores que sí
-- dio el alumno se llega a 250 créditos; +10 de Servicio Social = 260,
-- coincidiendo exactamente con el total institucional oficial. Se agregó
-- en 8° semestre por ser el mismo lugar donde aparece en ISC/Química/
-- Mecatrónica.
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.PlanEstudioMaterias
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'GASTRONOMIA')
)
BEGIN
    DECLARE @IdGastronomia INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'GASTRONOMIA');

    INSERT INTO dbo.PlanEstudioMaterias (IdCarrera, Nombre, Creditos, Semestre) VALUES
    -- Semestre 1
    (@IdGastronomia, N'Microbiología de los Alimentos', 5, 1),
    (@IdGastronomia, N'Física en Gastronomía', 4, 1),
    (@IdGastronomia, N'Introducción a la Gastronomía', 6, 1),
    (@IdGastronomia, N'Fundamentos de Investigación', 4, 1),
    (@IdGastronomia, N'Matemáticas para Gastronomía', 4, 1),
    (@IdGastronomia, N'Software para Aplicación Ejecutivo', 5, 1),
    -- Semestre 2
    (@IdGastronomia, N'Higiene en el Manejo de Alimentos y Bebidas', 4, 2),
    (@IdGastronomia, N'Cultura y Patrimonio Gastronómico Nacional e Internacional', 5, 2),
    (@IdGastronomia, N'Bases Culinarias', 6, 2),
    (@IdGastronomia, N'Mercadotecnia', 5, 2),
    (@IdGastronomia, N'Fundamentos en Gestión Empresarial', 5, 2),
    (@IdGastronomia, N'Taller de Ética', 4, 2),
    -- Semestre 3
    (@IdGastronomia, N'Química y Conservación de los Alimentos', 5, 3),
    (@IdGastronomia, N'Costos y Manejo de los Almacenes', 5, 3),
    (@IdGastronomia, N'Cocina Mexicana', 6, 3),
    (@IdGastronomia, N'Probabilidad y Estadística', 4, 3),
    (@IdGastronomia, N'Gestión del Capital Humano', 6, 3),
    (@IdGastronomia, N'Fundamentos de Turismo', 4, 3),
    -- Semestre 4
    (@IdGastronomia, N'Tecnología de Frutas, Hortalizas y Confitería', 6, 4),
    (@IdGastronomia, N'Enología y Vitivinicultura', 4, 4),
    (@IdGastronomia, N'Panadería', 6, 4),
    (@IdGastronomia, N'Economía Empresarial', 5, 4),
    (@IdGastronomia, N'Finanzas de las Organizaciones', 5, 4),
    (@IdGastronomia, N'El Emprendedor y la Innovación', 5, 4),
    -- Semestre 5
    (@IdGastronomia, N'Marco Legal de las Organizaciones', 4, 5),
    (@IdGastronomia, N'Coctelería', 4, 5),
    (@IdGastronomia, N'Introducción a la Repostería', 6, 5),
    (@IdGastronomia, N'Investigación de Operaciones', 5, 5),
    (@IdGastronomia, N'Calidad Aplicada a la Gestión Empresarial', 5, 5),
    (@IdGastronomia, N'Protocolo de Seguridad', 3, 5),
    (@IdGastronomia, N'Estructura en Hielo y Mukimono', 5, 5),
    -- Semestre 6
    (@IdGastronomia, N'Taller de Investigación I', 4, 6),
    (@IdGastronomia, N'Estancia Técnica (Nacional)', 4, 6),
    (@IdGastronomia, N'Desarrollo Sustentable', 5, 6),
    (@IdGastronomia, N'Cocina Internacional I', 6, 6),
    (@IdGastronomia, N'Gestión Estratégica', 5, 6),
    (@IdGastronomia, N'Banquetes', 4, 6),
    (@IdGastronomia, N'Dirección de Establecimientos de Alimentos y Bebidas', 4, 6),
    -- Semestre 7
    (@IdGastronomia, N'Taller de Investigación II', 4, 7),
    (@IdGastronomia, N'Nutrición y Dietética', 4, 7),
    (@IdGastronomia, N'Cocina Internacional II', 6, 7),
    (@IdGastronomia, N'Estancia Técnica (Internacional)', 4, 7),
    (@IdGastronomia, N'Arte Culinario Precolombino', 4, 7),
    (@IdGastronomia, N'Arte Cultural Culinario Noroeste', 4, 7),
    (@IdGastronomia, N'Arte Cultural Culinario Noreste', 4, 7),
    -- Semestre 8
    (@IdGastronomia, N'Formulación y Evaluación de Proyectos', 5, 8),
    (@IdGastronomia, N'Cocina Experimental', 5, 8),
    (@IdGastronomia, N'Arte Cultural Culinario del Centro', 4, 8),
    (@IdGastronomia, N'Arte Cultural Culinario del Occidente', 5, 8),
    (@IdGastronomia, N'Arte Cultural Culinario del Sureste', 4, 8),
    (@IdGastronomia, N'Servicio Social', 10, 8),
    -- Semestre 9
    (@IdGastronomia, N'Residencias Profesionales', 10, 9),
    (@IdGastronomia, N'Actividades Complementarias', 5, 9);

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdGastronomia);
    DECLARE @Creditos INT = (SELECT SUM(CAST(Creditos AS INT)) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdGastronomia);
    PRINT CONCAT('--- Plan de Gastronomía sembrado: ', @Total, ' materias, ', @Creditos, ' créditos totales ---');
END
ELSE
BEGIN
    PRINT '--- El plan de Gastronomía ya estaba sembrado, no se volvió a insertar ---';
END
GO
