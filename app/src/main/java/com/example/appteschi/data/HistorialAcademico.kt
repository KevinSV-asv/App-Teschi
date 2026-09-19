package com.example.appteschi.data

/** Códigos tal cual aparecen en el formato oficial de Control Escolar. */
enum class EstatusMateria(val codigo: String, val etiqueta: String) {
    APROBADA("AP", "Aprobada"),
    POR_CURSAR("PC", "Por cursar"),
    NO_APROBADA("NA", "No aprobó")
}

/** Una materia ya cruzada con el resultado real del alumno — la usan Kardex,
 *  Tira de Materias y Calificaciones. */
data class MateriaHistorial(
    val numero: Int,
    val nombre: String,
    val creditos: Int,
    val semestre: Int,
    val calificacion: Double? = null,
    val periodo: String? = null,
    val estatus: EstatusMateria
)

/**
 * Historial SIMULADO para los usuarios de prueba sem1..sem9 (ver
 * AuthRepository.semestrePruebaLocal) — sirve para comprobar que Tira de
 * Materias/Calificaciones/Kardex se adaptan bien a cualquier semestre sin
 * tocar datos reales. El historial de un alumno real ya no vive aquí: se
 * consulta contra la base de datos vía HistorialAcademicoService
 * (`GET /api/mi-historial/{matricula}`) — ver DECISIONES_TECNICAS.
 */
object HistorialAcademico {

    /**
     * - Semestres anteriores al indicado: Aprobada, con calificación sintética.
     * - Semestre indicado: TODAS sus materias Por cursar (el alumno está
     *   inscrito en la carga completa de su semestre y carrera; el docente
     *   todavía no sube calificaciones).
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
}
