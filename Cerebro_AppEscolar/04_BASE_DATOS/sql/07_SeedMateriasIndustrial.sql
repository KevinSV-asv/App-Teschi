-- =============================================================================
-- AppTESCHI — Plan de estudios: Ingeniería Industrial (parcial)
-- Archivo: 07_SeedMateriasIndustrial.sql
-- Fuente: mapa curricular oficial TecNM "IIND-2010-227" (créditos por
-- materia en formato Horas Teoría-Horas Práctica-Créditos) + lista de
-- nombres por semestre proporcionada por el alumno + "Horarios_Industrial.pdf"
-- (usado para confirmar algunos códigos de materia, ej. CPF-1201)
-- Relacionado con: [[Base_Datos_Usuarios]]
--
-- PARCIAL A PROPÓSITO: semestres 1-7 completos (44 materias) + 2 de las 6
-- materias de Octavo Semestre (las que se pudieron leer con confianza en el
-- mapa curricular) + Residencia Profesional de Noveno (10 créditos, viene en
-- un recuadro explícito del mapa). Quedan PENDIENTES, sin inventar su valor
-- de créditos:
--   - Octavo: Productividad Humana, Temas Selectos de Ingeniería Industrial,
--     Medición y mejoramiento de la Productividad, Gestión de los Sistemas
--     de Calidad Aplicados
--   - Noveno: Productividad Aplicada, Ingeniería de Calidad
-- Agregarlas en un script aparte cuando se confirmen sus créditos.
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.PlanEstudioMaterias
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'INDUSTRIAL')
)
BEGIN
    DECLARE @IdIndustrial INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'INDUSTRIAL');

    INSERT INTO dbo.PlanEstudioMaterias (IdCarrera, Nombre, Creditos, Semestre) VALUES
    -- Semestre 1
    (@IdIndustrial, N'Fundamentos de Investigación', 4, 1),
    (@IdIndustrial, N'Taller de Ética', 4, 1),
    (@IdIndustrial, N'Cálculo Diferencial', 5, 1),
    (@IdIndustrial, N'Taller de Herramientas Intelectuales', 4, 1),
    (@IdIndustrial, N'Química', 4, 1),
    (@IdIndustrial, N'Dibujo Industrial', 6, 1),
    -- Semestre 2
    (@IdIndustrial, N'Electricidad y Electrónica Industrial', 4, 2),
    (@IdIndustrial, N'Propiedades de los Materiales', 4, 2),
    (@IdIndustrial, N'Cálculo Integral', 5, 2),
    (@IdIndustrial, N'Probabilidad y Estadística', 4, 2),
    (@IdIndustrial, N'Análisis de la Realidad Nacional', 3, 2),
    (@IdIndustrial, N'Taller de Liderazgo', 6, 2),
    -- Semestre 3
    (@IdIndustrial, N'Metrología y Normalización', 4, 3),
    (@IdIndustrial, N'Álgebra Lineal', 5, 3),
    (@IdIndustrial, N'Cálculo Vectorial', 5, 3),
    (@IdIndustrial, N'Economía', 4, 3),
    (@IdIndustrial, N'Estadística Inferencial I', 5, 3),
    (@IdIndustrial, N'Estudio del Trabajo I', 6, 3),
    -- Semestre 4
    (@IdIndustrial, N'Procesos de Fabricación', 4, 4),
    (@IdIndustrial, N'Física', 4, 4),
    (@IdIndustrial, N'Algoritmos y Lenguajes de Programación', 4, 4),
    (@IdIndustrial, N'Investigación de Operaciones I', 4, 4),
    (@IdIndustrial, N'Estadística Inferencial II', 5, 4),
    (@IdIndustrial, N'Estudio del Trabajo II', 6, 4),
    -- Semestre 5
    (@IdIndustrial, N'Administración de Proyectos', 3, 5),
    (@IdIndustrial, N'Gestión de Costos', 4, 5),
    (@IdIndustrial, N'Administración de las Operaciones I', 4, 5),
    (@IdIndustrial, N'Investigación de Operaciones II', 4, 5),
    (@IdIndustrial, N'Control Estadístico de la Calidad', 5, 5),
    (@IdIndustrial, N'Ergonomía', 5, 5),
    (@IdIndustrial, N'Desarrollo Sustentable', 5, 5),
    -- Semestre 6
    (@IdIndustrial, N'Taller de Investigación I', 4, 6),
    (@IdIndustrial, N'Ingeniería Económica', 4, 6),
    (@IdIndustrial, N'Administración de las Operaciones II', 4, 6),
    (@IdIndustrial, N'Simulación', 5, 6),
    (@IdIndustrial, N'Administración del Mantenimiento', 4, 6),
    (@IdIndustrial, N'Mercadotecnia', 5, 6),
    -- Semestre 7
    (@IdIndustrial, N'Taller de Investigación II', 4, 7),
    (@IdIndustrial, N'Planeación Financiera', 4, 7),
    (@IdIndustrial, N'Planeación y Diseño de Instalaciones', 4, 7),
    (@IdIndustrial, N'Sistemas de Manufactura', 5, 7),
    (@IdIndustrial, N'Logística y Cadenas de Suministro', 4, 7),
    (@IdIndustrial, N'Gestión de los Sistemas de Calidad', 4, 7),
    (@IdIndustrial, N'Ingeniería de Sistemas', 3, 7),
    -- Semestre 8 (parcial — 2 de 6, ver nota arriba)
    (@IdIndustrial, N'Formulación y Evaluación de Proyectos', 5, 8),
    (@IdIndustrial, N'Relaciones Industriales', 4, 8),
    -- Semestre 9 (parcial — 1 de 3, ver nota arriba)
    (@IdIndustrial, N'Residencia Profesional', 10, 9);

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdIndustrial);
    DECLARE @Creditos INT = (SELECT SUM(CAST(Creditos AS INT)) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdIndustrial);
    PRINT CONCAT('--- Plan de Industrial sembrado (parcial): ', @Total, ' materias, ', @Creditos, ' créditos ---');
END
ELSE
BEGIN
    PRINT '--- El plan de Industrial ya estaba sembrado, no se volvió a insertar ---';
END
GO
