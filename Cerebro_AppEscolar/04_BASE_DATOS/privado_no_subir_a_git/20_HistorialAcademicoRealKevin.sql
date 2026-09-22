-- Carga el historial académico REAL del alumno 2022452166 (Kevin Antonio
-- Sanchez Vargas, ISC) — transcrito literalmente de su Historial Académico
-- oficial de Control Escolar (elaborado 07-09-2026). Hasta ahora esos datos
-- solo vivían hardcodeados en el mock de Kotlin (HistorialAcademico.kt);
-- nunca se habían insertado en la base de datos real, así que cualquier
-- endpoint que consultara dbo.HistorialAcademico para esta matrícula no
-- encontraba nada. Idempotente: usa MERGE, se puede volver a correr sin
-- duplicar filas.

DECLARE @IdAlumno INT = (SELECT IdAlumno FROM dbo.Alumnos WHERE Matricula = N'2022452166');
DECLARE @AP INT = (SELECT IdEstatus FROM dbo.CatalogoEstatusMateria WHERE Codigo = N'AP');
DECLARE @PC INT = (SELECT IdEstatus FROM dbo.CatalogoEstatusMateria WHERE Codigo = N'PC');

IF @IdAlumno IS NULL
BEGIN
    RAISERROR('No existe el alumno 2022452166 — corre primero el registro del alumno.', 16, 1);
    RETURN;
END

DECLARE @Datos TABLE (Nombre NVARCHAR(200), Calificacion DECIMAL(5,2) NULL, IdEstatus INT);
INSERT INTO @Datos (Nombre, Calificacion, IdEstatus) VALUES
(N'Cálculo Diferencial', 85.00, @AP),
(N'Fundamentos de Investigación', 95.00, @AP),
(N'Fundamentos de Programación', 93.00, @AP),
(N'Matemáticas Discretas', 75.00, @AP),
(N'Taller de Administración', 70.00, @AP),
(N'Taller de Ética', 70.00, @AP),
(N'Álgebra Lineal', 80.00, @AP),
(N'Cálculo Integral', 70.00, @AP),
(N'Contabilidad Financiera', 91.33, @AP),
(N'Probabilidad y Estadística', 72.00, @AP),
(N'Programación Orientada a Objetos', 99.20, @AP),
(N'Química', 90.00, @AP),
(N'Cálculo Vectorial', 71.00, @AP),
(N'Cultura Empresarial', 99.00, @AP),
(N'Desarrollo Sustentable', 96.00, @AP),
(N'Estructura de Datos', 92.00, @AP),
(N'Física General', 72.00, @AP),
(N'Investigación de Operaciones', 100.00, @AP),
(N'Ecuaciones Diferenciales', 80.00, @AP),
(N'Fundamento de Base de Datos', 80.00, @AP),
(N'Métodos Numéricos', 70.00, @AP),
(N'Principios Eléctricos y Aplicaciones Digitales', 85.00, @AP),
(N'Simulación', 70.00, @AP),
(N'Tópicos Avanzados de Programación', 100.00, @AP),
(N'Arquitectura de Computadoras', 86.92, @AP),
(N'Fundamentos de Ingeniería de Software', 88.00, @AP),
(N'Fundamentos de Telecomunicaciones', 97.33, @AP),
(N'Graficación', 83.33, @AP),
(N'Sistemas Operativos', 92.83, @AP),
(N'Taller de Base de Datos', 100.00, @AP),
(N'Administración de Base de Datos', 100.00, @AP),
(N'Ingeniería de Software', 95.00, @AP),
(N'Lenguajes de Interfaz', 80.00, @AP),
(N'Lenguajes y Autómatas I', 88.67, @AP),
(N'Redes de Computadoras', 96.83, @AP),
(N'Taller de Sistemas Operativos', 100.00, @AP),
(N'Tecnologías Emergentes de Base de Datos', 95.33, @AP),
(N'Conmutación y Enrutamiento en Redes de Datos', 72.18, @AP),
(N'Gestión de Proyectos de Software', 87.33, @AP),
(N'Lenguajes y Autómatas II', 96.33, @AP),
(N'Minería de Datos', 83.67, @AP),
(N'Programación Web', 94.94, @AP),
(N'Sistemas Programables', 87.33, @AP),
(N'Taller de Investigación I', 93.67, @AP),
(N'Administración de Redes', 100.00, @AP),
(N'Ingeniería del Conocimiento', 100.00, @AP),
(N'Inteligencia Artificial', 100.00, @AP),
(N'Inteligencia de Negocios y Analítica de Negocios', 100.00, @AP),
(N'Programación Lógica y Funcional', 100.00, @AP),
(N'Servicio Social', 95.00, @AP),
(N'Taller de Investigación II', 100.00, @AP),
(N'Actividades Complementarias', NULL, @AP),
(N'Residencias Profesionales', NULL, @PC);

MERGE dbo.HistorialAcademico AS destino
USING (
    SELECT pm.IdMateria, d.Calificacion, d.IdEstatus
    FROM @Datos d
    JOIN dbo.PlanEstudioMaterias pm ON pm.Nombre = d.Nombre
    JOIN dbo.CatalogoCarreras c ON c.IdCarrera = pm.IdCarrera AND c.Clave = N'ISC'
) AS origen
ON destino.IdAlumno = @IdAlumno AND destino.IdMateria = origen.IdMateria AND destino.TipoRegistro = N'EVALUACION'
WHEN MATCHED THEN
    UPDATE SET Calificacion = origen.Calificacion, IdEstatus = origen.IdEstatus, FechaActualizacion = SYSUTCDATETIME()
WHEN NOT MATCHED THEN
    INSERT (IdAlumno, IdMateria, TipoRegistro, Calificacion, IdEstatus)
    VALUES (@IdAlumno, origen.IdMateria, N'EVALUACION', origen.Calificacion, origen.IdEstatus);

PRINT 'Historial académico de 2022452166 sincronizado: ' + CAST(@@ROWCOUNT AS NVARCHAR(10)) + ' filas afectadas en el último MERGE.';
GO
