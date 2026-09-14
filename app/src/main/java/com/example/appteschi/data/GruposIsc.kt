package com.example.appteschi.data

/**
 * Códigos de grupo reales de ISC — tomados de HORARIOS Sistemas 2026-2 (semestres
 * 1-8) y del comprobante de carga académica del propio alumno (9ISC23).
 *
 * Formato confirmado: {semestre}{carrera}{turno}{numero}
 *   turno: 1 = matutino, 2 = vespertino
 * Ej. "9ISC23" = semestre 9, ISC, vespertino, grupo 3.
 *
 * El portal real solo pide el grupo — no carrera ni semestre por separado,
 * porque van codificados aquí. No se inventan grupos fuera de esta lista.
 */
object GruposIsc {
    val codigos: List<String> = listOf(
        "1ISC11", "1ISC12", "1ISC21",
        "2ISC11", "2ISC21",
        "3ISC11", "3ISC12", "3ISC21",
        "4ISC11", "4ISC21",
        "5ISC11", "5ISC12", "5ISC21",
        "6ISC11", "6ISC21",
        "7ISC21", "7ISC22",
        "8ISC21", "8ISC22",
        "9ISC23"
    )

    private val patron = Regex("""^(\d+)[A-Z]+(\d)\d$""")

    /** Deriva el turno directamente del código de grupo — nunca se pide por separado. */
    fun turno(grupo: String): String? =
        when (patron.find(grupo)?.groupValues?.get(2)) {
            "1" -> "Matutino"
            "2" -> "Vespertino"
            else -> null
        }

    /** Deriva el semestre directamente del código de grupo. */
    fun semestre(grupo: String): Int? =
        patron.find(grupo)?.groupValues?.get(1)?.toIntOrNull()
}
