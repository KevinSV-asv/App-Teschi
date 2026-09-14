package com.example.appteschi.data

/** Códigos tal cual aparecen en el formato oficial de Control Escolar. */
enum class EstatusMateria(val codigo: String, val etiqueta: String) {
    APROBADA("AP", "Aprobada"),
    POR_CURSAR("PC", "Por cursar"),
    NO_APROBADA("NA", "No aprobó")
}

/** Una materia ya cruzada con el resultado real del alumno — la usan Kardex y Tira de Materias. */
data class MateriaHistorial(
    val numero: Int,
    val nombre: String,
    val creditos: Int,
    val semestre: Int,
    val calificacion: Double? = null,
    val periodo: String? = null,
    val estatus: EstatusMateria
)

/** Resultado de una materia para un alumno: calificación (si aplica), periodo y estatus. */
private data class RegistroTranscript(
    val calificacion: Double? = null,
    val periodo: String? = null,
    val estatus: EstatusMateria
)

/**
 * Historial académico real de Kevin (matrícula 2022452166), tal como aparece en el
 * documento de Control Escolar con fecha de elaboración 07/09/2026 — actualizado a
 * petición explícita del propio alumno con sus datos reales, ya no es un mock
 * genérico. Se une contra PlanDeEstudiosIsc por nombre de materia: el catálogo
 * dice a qué semestre pertenece cada una, este mapa dice cómo le fue al alumno.
 *
 * Placeholder hasta que exista scraping real de "Reportes > Historial Académico"
 * en el SIIA — ver 02_MODULOS en la bóveda.
 */
private val transcriptReal: Map<String, RegistroTranscript> = mapOf(
    "Cálculo Diferencial" to RegistroTranscript(85.0, "2022-2", EstatusMateria.APROBADA),
    "Fundamentos de Investigación" to RegistroTranscript(95.0, "2022-2", EstatusMateria.APROBADA),
    "Fundamentos de Programación" to RegistroTranscript(93.0, "2022-2", EstatusMateria.APROBADA),
    "Matemáticas Discretas" to RegistroTranscript(75.0, "2022-2", EstatusMateria.APROBADA),
    "Taller de Administración" to RegistroTranscript(70.0, "2022-2", EstatusMateria.APROBADA),
    "Taller de Ética" to RegistroTranscript(70.0, "2022-2", EstatusMateria.APROBADA),
    "Álgebra Lineal" to RegistroTranscript(80.0, "2023-1", EstatusMateria.APROBADA),
    "Cálculo Integral" to RegistroTranscript(70.0, "2023-1", EstatusMateria.APROBADA),
    "Contabilidad Financiera" to RegistroTranscript(91.33, "2023-1", EstatusMateria.APROBADA),
    "Probabilidad y Estadística" to RegistroTranscript(72.0, "2023-1", EstatusMateria.APROBADA),
    "Programación Orientada a Objetos" to RegistroTranscript(99.2, "2023-1", EstatusMateria.APROBADA),
    "Química" to RegistroTranscript(90.0, "2023-1", EstatusMateria.APROBADA),
    "Cálculo Vectorial" to RegistroTranscript(71.0, "2023-2", EstatusMateria.APROBADA),
    "Cultura Empresarial" to RegistroTranscript(99.0, "2023-2", EstatusMateria.APROBADA),
    "Desarrollo Sustentable" to RegistroTranscript(96.0, "2023-2", EstatusMateria.APROBADA),
    "Estructura de Datos" to RegistroTranscript(92.0, "2023-2", EstatusMateria.APROBADA),
    "Física General" to RegistroTranscript(72.0, "2023-2", EstatusMateria.APROBADA),
    "Investigación de Operaciones" to RegistroTranscript(100.0, "2023-2", EstatusMateria.APROBADA),
    "Ecuaciones Diferenciales" to RegistroTranscript(80.0, "2024-1", EstatusMateria.APROBADA),
    "Fundamento de Base de Datos" to RegistroTranscript(80.0, "2024-1", EstatusMateria.APROBADA),
    "Métodos Numéricos" to RegistroTranscript(70.0, "2024-1", EstatusMateria.APROBADA),
    "Principios Eléctricos y Aplicaciones Digitales" to RegistroTranscript(85.0, "2024-1", EstatusMateria.APROBADA),
    "Simulación" to RegistroTranscript(70.0, "2024-1", EstatusMateria.APROBADA),
    "Tópicos Avanzados de Programación" to RegistroTranscript(100.0, "2024-1", EstatusMateria.APROBADA),
    "Arquitectura de Computadoras" to RegistroTranscript(86.92, "2024-2", EstatusMateria.APROBADA),
    "Fundamentos de Ingeniería de Software" to RegistroTranscript(88.0, "2024-2", EstatusMateria.APROBADA),
    "Fundamentos de Telecomunicaciones" to RegistroTranscript(97.33, "2024-2", EstatusMateria.APROBADA),
    "Graficación" to RegistroTranscript(83.33, "2024-2", EstatusMateria.APROBADA),
    "Sistemas Operativos" to RegistroTranscript(92.83, "2024-2", EstatusMateria.APROBADA),
    "Taller de Base de Datos" to RegistroTranscript(100.0, "2024-2", EstatusMateria.APROBADA),
    "Administración de Base de Datos" to RegistroTranscript(100.0, "2025-1", EstatusMateria.APROBADA),
    "Ingeniería de Software" to RegistroTranscript(95.0, "2025-1", EstatusMateria.APROBADA),
    "Lenguajes de Interfaz" to RegistroTranscript(80.0, "2025-1", EstatusMateria.APROBADA),
    "Lenguajes y Autómatas I" to RegistroTranscript(88.67, "2025-1", EstatusMateria.APROBADA),
    "Redes de Computadoras" to RegistroTranscript(96.83, "2025-1", EstatusMateria.APROBADA),
    "Taller de Sistemas Operativos" to RegistroTranscript(100.0, "2025-1", EstatusMateria.APROBADA),
    "Tecnologías Emergentes de Base de Datos" to RegistroTranscript(95.33, "2025-1", EstatusMateria.APROBADA),
    "Conmutación y Enrutamiento en Redes de Datos" to RegistroTranscript(72.18, "2025-2", EstatusMateria.APROBADA),
    "Gestión de Proyectos de Software" to RegistroTranscript(87.33, "2025-2", EstatusMateria.APROBADA),
    "Lenguajes y Autómatas II" to RegistroTranscript(96.33, "2025-2", EstatusMateria.APROBADA),
    "Minería de Datos" to RegistroTranscript(83.67, "2025-2", EstatusMateria.APROBADA),
    "Programación Web" to RegistroTranscript(94.94, "2025-2", EstatusMateria.APROBADA),
    "Sistemas Programables" to RegistroTranscript(87.33, "2025-2", EstatusMateria.APROBADA),
    "Taller de Investigación I" to RegistroTranscript(93.67, "2025-2", EstatusMateria.APROBADA),
    "Administración de Redes" to RegistroTranscript(100.0, "2026-1", EstatusMateria.APROBADA),
    "Ingeniería del Conocimiento" to RegistroTranscript(100.0, "2026-1", EstatusMateria.APROBADA),
    "Inteligencia Artificial" to RegistroTranscript(100.0, "2026-1", EstatusMateria.APROBADA),
    "Inteligencia de Negocios y Analítica de Negocios" to RegistroTranscript(100.0, "2026-1", EstatusMateria.APROBADA),
    "Programación Lógica y Funcional" to RegistroTranscript(100.0, "2026-1", EstatusMateria.APROBADA),
    "Servicio Social" to RegistroTranscript(95.0, "2026-1", EstatusMateria.APROBADA),
    "Taller de Investigación II" to RegistroTranscript(100.0, "2026-1", EstatusMateria.APROBADA),
    "Actividades Complementarias" to RegistroTranscript(calificacion = null, periodo = "2025-1", estatus = EstatusMateria.APROBADA),
    "Residencias Profesionales" to RegistroTranscript(estatus = EstatusMateria.POR_CURSAR)
)

/**
 * Une PlanDeEstudiosIsc con el historial real o simulado por nombre de
 * materia — fuente única para Kardex, Tira de Materias y Calificaciones.
 * Ninguna pantalla debe mantener su propia lista de materias por separado.
 */
object HistorialAcademico {

    /** Historial académico real de Kevin (matrícula 2022452166). */
    val materiasReales: List<MateriaHistorial> by lazy {
        PlanDeEstudiosIsc.materias.mapIndexed { index, plan ->
            val registro = transcriptReal[plan.nombre] ?: RegistroTranscript(estatus = EstatusMateria.POR_CURSAR)
            MateriaHistorial(
                numero = index + 1,
                nombre = plan.nombre,
                creditos = plan.creditos,
                semestre = plan.semestre,
                calificacion = registro.calificacion,
                periodo = registro.periodo,
                estatus = registro.estatus
            )
        }
    }

    /**
     * Historial SIMULADO para los usuarios de prueba sem1..sem9 — sirve para
     * comprobar que Tira de Materias/Calificaciones/Kardex se adaptan bien a
     * cualquier semestre, sin depender solo del caso real de Kevin (que
     * siempre está en semestre 9). Ningún dato aquí es real:
     * - Semestres anteriores al indicado: Aprobada, con calificación sintética.
     * - Semestre indicado: TODAS sus materias Por cursar (el alumno está
     *   inscrito en la carga completa de su semestre y carrera; el docente
     *   todavía no sube calificaciones), para que Tira de Materias muestre el
     *   semestre completo en vez de solo una parte.
     * - Semestres posteriores: Por cursar (todavía no inscrito).
     */
    fun materiasSimuladas(semestreActual: Int): List<MateriaHistorial> {
        val cicloCalificaciones = listOf(85.0, 90.0, 78.0, 92.0, 88.0, 95.0, 80.0)
        var indiceCalificacion = 0

        fun siguienteCalificacion(): Double {
            val calificacion = cicloCalificaciones[indiceCalificacion % cicloCalificaciones.size]
            indiceCalificacion++
            return calificacion
        }

        return PlanDeEstudiosIsc.materias.mapIndexed { index, plan ->
            val (estatus, calificacion, periodo) = if (plan.semestre < semestreActual) {
                Triple(EstatusMateria.APROBADA, siguienteCalificacion(), "SIM-${plan.semestre}")
            } else {
                Triple(EstatusMateria.POR_CURSAR, null, null)
            }
            MateriaHistorial(
                numero = index + 1,
                nombre = plan.nombre,
                creditos = plan.creditos,
                semestre = plan.semestre,
                calificacion = calificacion,
                periodo = periodo,
                estatus = estatus
            )
        }
    }

    /** Punto de acceso único: historial real si semestreSimulado es null, simulado si no. */
    fun materiasPara(semestreSimulado: Int?): List<MateriaHistorial> =
        if (semestreSimulado == null) materiasReales else materiasSimuladas(semestreSimulado)
}
