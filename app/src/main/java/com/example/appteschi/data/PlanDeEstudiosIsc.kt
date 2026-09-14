package com.example.appteschi.data

/** Una materia del plan de estudios institucional: nombre, créditos y a qué semestre pertenece. */
data class MateriaPlan(val nombre: String, val creditos: Int, val semestre: Int)

/**
 * Plan de estudios de Ingeniería en Sistemas Computacionales — catálogo curricular
 * institucional (nombres y créditos tomados literalmente del Historial Académico
 * oficial; la relación materia→semestre viene tanto de ahí como de los horarios
 * 2026-2). Es la fuente única de qué materias existen y a qué semestre pertenecen.
 *
 * Kardex, Tira de Materias y Calificaciones deben leer de aquí — ninguno debe
 * mantener su propia lista de materias por separado, para que nunca queden
 * desincronizadas entre sí.
 */
object PlanDeEstudiosIsc {
    val materias: List<MateriaPlan> = listOf(
        // Semestre 1
        MateriaPlan("Cálculo Diferencial", 5, 1),
        MateriaPlan("Fundamentos de Investigación", 4, 1),
        MateriaPlan("Fundamentos de Programación", 5, 1),
        MateriaPlan("Matemáticas Discretas", 5, 1),
        MateriaPlan("Taller de Administración", 4, 1),
        MateriaPlan("Taller de Ética", 4, 1),
        // Semestre 2
        MateriaPlan("Álgebra Lineal", 5, 2),
        MateriaPlan("Cálculo Integral", 5, 2),
        MateriaPlan("Contabilidad Financiera", 4, 2),
        MateriaPlan("Probabilidad y Estadística", 5, 2),
        MateriaPlan("Programación Orientada a Objetos", 5, 2),
        MateriaPlan("Química", 4, 2),
        // Semestre 3
        MateriaPlan("Cálculo Vectorial", 5, 3),
        MateriaPlan("Cultura Empresarial", 4, 3),
        MateriaPlan("Desarrollo Sustentable", 5, 3),
        MateriaPlan("Estructura de Datos", 5, 3),
        MateriaPlan("Física General", 5, 3),
        MateriaPlan("Investigación de Operaciones", 4, 3),
        // Semestre 4
        MateriaPlan("Ecuaciones Diferenciales", 5, 4),
        MateriaPlan("Fundamento de Base de Datos", 5, 4),
        MateriaPlan("Métodos Numéricos", 4, 4),
        MateriaPlan("Principios Eléctricos y Aplicaciones Digitales", 5, 4),
        MateriaPlan("Simulación", 5, 4),
        MateriaPlan("Tópicos Avanzados de Programación", 5, 4),
        // Semestre 5
        MateriaPlan("Arquitectura de Computadoras", 5, 5),
        MateriaPlan("Fundamentos de Ingeniería de Software", 4, 5),
        MateriaPlan("Fundamentos de Telecomunicaciones", 4, 5),
        MateriaPlan("Graficación", 4, 5),
        MateriaPlan("Sistemas Operativos", 4, 5),
        MateriaPlan("Taller de Base de Datos", 4, 5),
        // Semestre 6
        MateriaPlan("Administración de Base de Datos", 5, 6),
        MateriaPlan("Ingeniería de Software", 5, 6),
        MateriaPlan("Lenguajes de Interfaz", 4, 6),
        MateriaPlan("Lenguajes y Autómatas I", 5, 6),
        MateriaPlan("Redes de Computadoras", 5, 6),
        MateriaPlan("Taller de Sistemas Operativos", 4, 6),
        MateriaPlan("Tecnologías Emergentes de Base de Datos", 6, 6),
        // Semestre 7
        MateriaPlan("Conmutación y Enrutamiento en Redes de Datos", 5, 7),
        MateriaPlan("Gestión de Proyectos de Software", 6, 7),
        MateriaPlan("Lenguajes y Autómatas II", 5, 7),
        MateriaPlan("Minería de Datos", 6, 7),
        MateriaPlan("Programación Web", 5, 7),
        MateriaPlan("Sistemas Programables", 4, 7),
        MateriaPlan("Taller de Investigación I", 4, 7),
        // Semestre 8
        MateriaPlan("Administración de Redes", 4, 8),
        MateriaPlan("Ingeniería del Conocimiento", 6, 8),
        MateriaPlan("Inteligencia Artificial", 4, 8),
        MateriaPlan("Inteligencia de Negocios y Analítica de Negocios", 7, 8),
        MateriaPlan("Programación Lógica y Funcional", 4, 8),
        MateriaPlan("Servicio Social", 10, 8),
        MateriaPlan("Taller de Investigación II", 4, 8),
        // Semestre 9
        MateriaPlan("Actividades Complementarias", 5, 9),
        MateriaPlan("Residencias Profesionales", 10, 9)
    )
}
