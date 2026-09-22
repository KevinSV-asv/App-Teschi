-- =============================================================================
-- AppTESCHI — Plan de estudios: Ingeniería Mecatrónica
-- Archivo: 08_SeedMateriasMecatronica.sql
-- Fuente: lista oficial de materias por semestre (con créditos) proporcionada
-- por el alumno. Suma total verificada: 260 créditos — coincide exactamente
-- con el total institucional de la retícula oficial IMCT-2010-229 que
-- también compartió (Genérica 210 + Especialidad 25 + Residencias 10 +
-- Servicio Social 10 + Complementarias 5 = 260).
-- Relacionado con: [[Base_Datos_Usuarios]]
--
-- NOTA: la retícula oficial adjunta es la "Especialidad: Robótica"
-- (variante con materias optativas propias de esa especialidad en 7°-9°
-- semestre) — no se usó para transcribir materias/créditos porque la lista
-- con créditos que dio el alumno ya es autoconsistente (suma exacta a 260)
-- y es el plan genérico de 9 semestres, no la variante de especialidad.
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.PlanEstudioMaterias
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'MECATRONICA')
)
BEGIN
    DECLARE @IdMecatronica INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'MECATRONICA');

    INSERT INTO dbo.PlanEstudioMaterias (IdCarrera, Nombre, Creditos, Semestre) VALUES
    -- Semestre 1
    (@IdMecatronica, N'Química', 4, 1),
    (@IdMecatronica, N'Cálculo Diferencial', 5, 1),
    (@IdMecatronica, N'Taller de Ética', 4, 1),
    (@IdMecatronica, N'Dibujo Asistido por Computadora', 4, 1),
    (@IdMecatronica, N'Metrología y Normalización', 4, 1),
    (@IdMecatronica, N'Fundamentos de Investigación', 4, 1),
    -- Semestre 2
    (@IdMecatronica, N'Cálculo Integral', 5, 2),
    (@IdMecatronica, N'Álgebra Lineal', 5, 2),
    (@IdMecatronica, N'Ciencias e Ingenierías de los Materiales', 5, 2),
    (@IdMecatronica, N'Programación Básica', 5, 2),
    (@IdMecatronica, N'Estadísticas y Control de Calidad', 4, 2),
    (@IdMecatronica, N'Administración y Contabilidad', 4, 2),
    -- Semestre 3
    (@IdMecatronica, N'Cálculo Vectorial', 5, 3),
    (@IdMecatronica, N'Proceso de Fabricación', 4, 3),
    (@IdMecatronica, N'Electromagnetismo', 5, 3),
    (@IdMecatronica, N'Estática', 4, 3),
    (@IdMecatronica, N'Métodos Numéricos', 4, 3),
    (@IdMecatronica, N'Desarrollo Sustentable', 5, 3),
    -- Semestre 4
    (@IdMecatronica, N'Ecuaciones Diferenciales', 5, 4),
    (@IdMecatronica, N'Fundamentos de Termodinámica', 4, 4),
    (@IdMecatronica, N'Mecánica de Materiales', 6, 4),
    (@IdMecatronica, N'Dinámica', 4, 4),
    (@IdMecatronica, N'Análisis de Circuitos Eléctricos', 6, 4),
    -- Semestre 5
    (@IdMecatronica, N'Máquinas Eléctricas', 5, 5),
    (@IdMecatronica, N'Electrónica Analógica', 6, 5),
    (@IdMecatronica, N'Mecanismos', 5, 5),
    (@IdMecatronica, N'Análisis de Fluidos', 4, 5),
    (@IdMecatronica, N'Taller de Investigación I', 4, 5),
    -- Semestre 6
    (@IdMecatronica, N'Electrónica de Potencia Aplicada', 6, 6),
    (@IdMecatronica, N'Instrumentación', 5, 6),
    (@IdMecatronica, N'Diseño de Elementos Mecánicos', 5, 6),
    (@IdMecatronica, N'Electrónica Digital', 5, 6),
    (@IdMecatronica, N'Vibraciones Mecánicas', 5, 6),
    (@IdMecatronica, N'Taller de Investigación II', 4, 6),
    (@IdMecatronica, N'Factores del Trabajo', 5, 6),
    -- Semestre 7
    (@IdMecatronica, N'Dinámica de Sistemas', 5, 7),
    (@IdMecatronica, N'Manufactura Avanzada', 5, 7),
    (@IdMecatronica, N'Circuitos Hidráulicos y Neumáticos', 6, 7),
    (@IdMecatronica, N'Mantenimiento', 5, 7),
    (@IdMecatronica, N'Microcontroladores', 5, 7),
    (@IdMecatronica, N'Programación Avanzada', 6, 7),
    -- Semestre 8
    (@IdMecatronica, N'Control', 6, 8),
    (@IdMecatronica, N'Formulación y Evaluación de Proyectos', 3, 8),
    (@IdMecatronica, N'Controladores Lógicos Programables', 5, 8),
    (@IdMecatronica, N'Control Digital', 5, 8),
    (@IdMecatronica, N'Interfaces y Redes', 5, 8),
    (@IdMecatronica, N'Servicio Social', 10, 8),
    -- Semestre 9
    (@IdMecatronica, N'Robótica', 5, 9),
    (@IdMecatronica, N'Control de Procesos', 5, 9),
    (@IdMecatronica, N'Automatización', 5, 9),
    (@IdMecatronica, N'Residencias Profesionales', 10, 9),
    (@IdMecatronica, N'Actividades Complementarias', 5, 9);

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdMecatronica);
    DECLARE @Creditos INT = (SELECT SUM(CAST(Creditos AS INT)) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdMecatronica);
    PRINT CONCAT('--- Plan de Mecatrónica sembrado: ', @Total, ' materias, ', @Creditos, ' créditos totales ---');
END
ELSE
BEGIN
    PRINT '--- El plan de Mecatrónica ya estaba sembrado, no se volvió a insertar ---';
END
GO
