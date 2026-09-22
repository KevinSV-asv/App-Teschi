-- =============================================================================
-- AppTESCHI — Plan de estudios: Ingeniería en Animación Digital y Efectos Visuales
-- Archivo: 04_SeedMateriasAnimacion.sql
-- Ejecutar después de 03_MigracionV2_Normalizacion.sql
-- Relacionado con: [[Base_Datos_Usuarios]]
--
-- Siembra las 48 materias reales del plan de estudios de Ingeniería en
-- Animación Digital y Efectos Visuales en dbo.PlanEstudioMaterias — misma
-- estructura que ya usa ISC (IdCarrera + Nombre + Creditos + Semestre), sin
-- ningún cambio de esquema. Demuestra que el diseño soporta agregar el plan
-- completo de otra carrera sin tocar tablas ni relaciones.
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.PlanEstudioMaterias
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'ANIMACION')
)
BEGIN
    DECLARE @IdAnimacion INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'ANIMACION');

    INSERT INTO dbo.PlanEstudioMaterias (IdCarrera, Nombre, Creditos, Semestre) VALUES
    -- Semestre 1
    (@IdAnimacion, N'Proyecto de Vida y Carrera', 4, 1),
    (@IdAnimacion, N'Diseño Industrial', 5, 1),
    (@IdAnimacion, N'Introducción a las Producciones de Animación', 5, 1),
    (@IdAnimacion, N'Taller de Ética', 4, 1),
    (@IdAnimacion, N'Fundamentos de Programación', 5, 1),
    (@IdAnimacion, N'Álgebra Lineal', 5, 1),
    -- Semestre 2
    (@IdAnimacion, N'Psicología del Comportamiento Humano', 4, 2),
    (@IdAnimacion, N'Introducción al Modelo 3D', 4, 2),
    (@IdAnimacion, N'Arte Digital', 5, 2),
    (@IdAnimacion, N'Infraestructura Computacional', 5, 2),
    (@IdAnimacion, N'Programación Orientada a Objetos', 5, 2),
    (@IdAnimacion, N'Cálculo Diferencial', 5, 2),
    -- Semestre 3
    (@IdAnimacion, N'El Emprendedor y Innovación', 5, 3),
    (@IdAnimacion, N'Modelo 3D', 5, 3),
    (@IdAnimacion, N'Diseño Artístico', 5, 3),
    (@IdAnimacion, N'Física General', 5, 3),
    (@IdAnimacion, N'Estructura de Datos', 5, 3),
    (@IdAnimacion, N'Cálculo Integral', 5, 3),
    -- Semestre 4
    (@IdAnimacion, N'Planeación de Negocios', 4, 4),
    (@IdAnimacion, N'Texturas y Materiales', 5, 4),
    (@IdAnimacion, N'Principios de la Fotografía y Video', 5, 4),
    (@IdAnimacion, N'Física Aplicada', 5, 4),
    (@IdAnimacion, N'Desarrollo Sustentable', 5, 4),
    (@IdAnimacion, N'Matemáticas Discretas', 5, 4),
    -- Semestre 5
    (@IdAnimacion, N'Fundamentos de Audio Digital', 5, 5),
    (@IdAnimacion, N'Rigging', 5, 5),
    (@IdAnimacion, N'Fundamento de Animación', 4, 5),
    (@IdAnimacion, N'Fundamentos de Investigación', 4, 5),
    (@IdAnimacion, N'Programación de Gráficas por Computadora', 5, 5),
    (@IdAnimacion, N'Cálculo Vectorial', 5, 5),
    -- Semestre 6
    (@IdAnimacion, N'Dirección y Negociación', 4, 6),
    (@IdAnimacion, N'Iluminación y Render', 5, 6),
    (@IdAnimacion, N'Animación Avanzada', 5, 6),
    (@IdAnimacion, N'Taller de Investigación I', 4, 6),
    (@IdAnimacion, N'Simulación', 5, 6),
    (@IdAnimacion, N'Ecuaciones Diferenciales', 5, 6),
    -- Semestre 7
    (@IdAnimacion, N'Formulación y Evaluación de Proyectos', 5, 7),
    (@IdAnimacion, N'Composición Digital', 6, 7),
    (@IdAnimacion, N'Desarrollo de Proyectos de la Industria de la Animación', 5, 7),
    (@IdAnimacion, N'Taller de Investigación II', 4, 7),
    (@IdAnimacion, N'Tecnologías Aplicadas para Efectos Especiales I', 6, 7),
    (@IdAnimacion, N'Realidad Aumentada', 5, 7),
    -- Semestre 8
    (@IdAnimacion, N'Integración Empresarial', 4, 8),
    (@IdAnimacion, N'Efectos Visuales', 6, 8),
    (@IdAnimacion, N'Tecnologías Aplicadas para Efectos Especiales II', 5, 8),
    (@IdAnimacion, N'Realidad Virtual', 6, 8),
    (@IdAnimacion, N'Arquitectura de Escenarios', 6, 8),
    -- Semestre 9
    (@IdAnimacion, N'Motion Graphics Avanzado', 6, 9);

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdAnimacion);
    DECLARE @Creditos INT = (SELECT SUM(CAST(Creditos AS INT)) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdAnimacion);
    PRINT CONCAT('--- Plan de Animación sembrado: ', @Total, ' materias, ', @Creditos, ' créditos totales ---');
END
ELSE
BEGIN
    PRINT '--- El plan de Animación ya estaba sembrado, no se volvió a insertar ---';
END
GO
