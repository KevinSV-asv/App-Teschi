-- =============================================================================
-- AppTESCHI — Plan de estudios: Ingeniería Química
-- Archivo: 10_SeedMateriasQuimica.sql
-- Fuente: tabla oficial "CLAVE / ASIGNATURA / HORAS" impresa en cada página
-- de "Horarios Quimica.pdf" (una por grupo, ciclo 2026-2) — más confiable
-- que leer la retícula porque cada valor de créditos viene impreso
-- explícitamente, no inferido de una cuadrícula densa.
-- Suma de créditos verificada: 260 — coincide exactamente con el total
-- institucional oficial de la retícula IQUI-2010-232 que también compartió
-- el alumno (Genérica 210 + Especialidad 25 + Residencia 10 + Servicio
-- Social 10 + Actividades Complementarias 5 = 260).
-- Relacionado con: [[Base_Datos_Usuarios]]
--
-- Las materias "Especialidad I".."Especialidad VI" del listado del alumno
-- corresponden a nombres reales distintos en los horarios (ej. "Especialidad
-- I" = Microbiología Ambiental, "Especialidad II" = Conformación del Aire),
-- pero se sembraron con el nombre genérico que dio el alumno para mantener
-- consistencia con el resto del plan tal como él lo documentó; el crédito
-- de cada una sí es el real confirmado en el horario.
-- =============================================================================

USE AppTeschiDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM dbo.PlanEstudioMaterias
    WHERE IdCarrera = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'QUIMICA')
)
BEGIN
    DECLARE @IdQuimica INT = (SELECT IdCarrera FROM dbo.CatalogoCarreras WHERE Clave = N'QUIMICA');

    INSERT INTO dbo.PlanEstudioMaterias (IdCarrera, Nombre, Creditos, Semestre) VALUES
    -- Semestre 1
    (@IdQuimica, N'Taller de Ética', 4, 1),
    (@IdQuimica, N'Fundamentos de Investigación', 4, 1),
    (@IdQuimica, N'Cálculo Diferencial', 5, 1),
    (@IdQuimica, N'Química Inorgánica', 5, 1),
    (@IdQuimica, N'Programación', 4, 1),
    (@IdQuimica, N'Dibujo Asistido por Computadora', 3, 1),
    -- Semestre 2
    (@IdQuimica, N'Álgebra Lineal', 5, 2),
    (@IdQuimica, N'Mecánica Clásica', 5, 2),
    (@IdQuimica, N'Cálculo Integral', 5, 2),
    (@IdQuimica, N'Química Orgánica I', 5, 2),
    (@IdQuimica, N'Termodinámica', 5, 2),
    (@IdQuimica, N'Química Analítica', 6, 2),
    -- Semestre 3
    (@IdQuimica, N'Análisis de Datos Experimentales', 5, 3),
    (@IdQuimica, N'Electricidad, Magnetismo y Óptica', 5, 3),
    (@IdQuimica, N'Cálculo Vectorial', 5, 3),
    (@IdQuimica, N'Química Orgánica II', 5, 3),
    (@IdQuimica, N'Balance de Materia y Energía', 5, 3),
    (@IdQuimica, N'Gestión de la Calidad', 5, 3),
    -- Semestre 4
    (@IdQuimica, N'Métodos Numéricos', 4, 4),
    (@IdQuimica, N'Ecuaciones Diferenciales', 5, 4),
    (@IdQuimica, N'Mecanismos de Transferencia', 5, 4),
    (@IdQuimica, N'Ingeniería Ambiental', 5, 4),
    (@IdQuimica, N'Fisicoquímica I', 5, 4),
    (@IdQuimica, N'Análisis Instrumental', 5, 4),
    -- Semestre 5
    (@IdQuimica, N'Desarrollo Sustentable', 5, 5),
    (@IdQuimica, N'Ingeniería de Costos', 4, 5),
    (@IdQuimica, N'Balances de Momentum, Calor y Masa', 6, 5),
    (@IdQuimica, N'Procesos de Separación I', 5, 5),
    (@IdQuimica, N'Fisicoquímica II', 5, 5),
    (@IdQuimica, N'Especialidad I', 5, 5),
    (@IdQuimica, N'Especialidad II', 4, 5),
    -- Semestre 6
    (@IdQuimica, N'Taller de Investigación I', 4, 6),
    (@IdQuimica, N'Procesos de Separación II', 5, 6),
    (@IdQuimica, N'Laboratorio Integral I', 6, 6),
    (@IdQuimica, N'Fundamentos de Aguas Residuales', 5, 6),
    (@IdQuimica, N'Especialidad III', 5, 6),
    (@IdQuimica, N'Especialidad IV', 4, 6),
    (@IdQuimica, N'Especialidad V', 3, 6),
    -- Semestre 7
    (@IdQuimica, N'Taller de Investigación II', 4, 7),
    (@IdQuimica, N'Procesos de Separación III', 5, 7),
    (@IdQuimica, N'Síntesis y Optimización de Procesos', 5, 7),
    (@IdQuimica, N'Salud y Seguridad en el Trabajo', 5, 7),
    (@IdQuimica, N'Laboratorio Integral II', 6, 7),
    (@IdQuimica, N'Taller de Administración Gerencial', 3, 7),
    (@IdQuimica, N'Especialidad VI', 4, 7),
    (@IdQuimica, N'Actividades Complementarias', 5, 7),
    -- Semestre 8
    (@IdQuimica, N'Laboratorio Integral III', 6, 8),
    (@IdQuimica, N'Instrumentación y Control', 5, 8),
    (@IdQuimica, N'Ingeniería de Proyectos', 6, 8),
    (@IdQuimica, N'Simulación de Procesos', 5, 8),
    (@IdQuimica, N'Servicio Social', 10, 8),
    -- Semestre 9
    (@IdQuimica, N'Residencias Profesionales', 10, 9);

    DECLARE @Total INT = (SELECT COUNT(*) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdQuimica);
    DECLARE @Creditos INT = (SELECT SUM(CAST(Creditos AS INT)) FROM dbo.PlanEstudioMaterias WHERE IdCarrera = @IdQuimica);
    PRINT CONCAT('--- Plan de Química sembrado: ', @Total, ' materias, ', @Creditos, ' créditos totales ---');
END
ELSE
BEGIN
    PRINT '--- El plan de Química ya estaba sembrado, no se volvió a insertar ---';
END
GO
