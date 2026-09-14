package com.example.appteschi.pdf

import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.viewmodel.KardexUiState

/** Una fila de la tabla de materias, ya formateada tal como se imprime en el PDF oficial. */
data class FilaKardexPdf(
    val numero: Int,
    val materia: String,
    val creditos: String,
    val semestre: String,
    val calificacionEvaluacion: String,
    val periodoEvaluacion: String,
    val cursada: String
)

/** Encabezado y panel de resumen del PDF, ya formateados como texto. */
data class EncabezadoKardexPdf(
    val matricula: String,
    val nombre: String,
    val carrera: String,
    val periodoIngreso: String,
    val ultimoPeriodo: String,
    val fechaElaboracion: String,
    val totalCreditosCarrera: String,
    val totalCreditosCursados: String,
    val creditosPorCursar: String,
    val porcentajeCubierto: String,
    val promedioUltimoSemestre: String,
    val totalMaterias: String,
    val materiasAprobadas: String,
    val materiasReprobadas: String,
    val materiasPorAprobar: String,
    val promedioGlobal: String
)

/**
 * Convierte el estado ya cargado de KardexViewModel al mismo formato de texto
 * que usa el PDF oficial del Historial Académico del TESCHI (mismo redondeo,
 * sin ceros decimales de más). Separado del renderizado con Canvas/PdfDocument
 * para poder probarlo con pruebas unitarias normales.
 */
object KardexPdfFormato {

    fun encabezado(
        uiState: KardexUiState,
        matricula: String,
        nombre: String,
        fechaElaboracion: String
    ): EncabezadoKardexPdf {
        val creditosPorCursar = uiState.totalCreditosCarrera - uiState.totalCreditosCursados
        return EncabezadoKardexPdf(
            matricula = matricula,
            nombre = nombre.uppercase(),
            carrera = uiState.carrera.uppercase(),
            periodoIngreso = uiState.periodoIngreso,
            ultimoPeriodo = uiState.ultimoPeriodo,
            fechaElaboracion = fechaElaboracion,
            totalCreditosCarrera = uiState.totalCreditosCarrera.toString(),
            totalCreditosCursados = uiState.totalCreditosCursados.toString(),
            creditosPorCursar = creditosPorCursar.toString(),
            porcentajeCubierto = "${formatearNumero(uiState.porcentajeCubierto)} %",
            promedioUltimoSemestre = formatearNumero(uiState.promedioUltimoSemestre),
            totalMaterias = uiState.totalMaterias.toString(),
            materiasAprobadas = uiState.materiasAprobadas.toString(),
            materiasReprobadas = uiState.materiasReprobadas.toString(),
            materiasPorAprobar = uiState.materiasPorAprobar.toString(),
            promedioGlobal = formatearNumero(uiState.promedioGlobal)
        )
    }

    fun filas(materias: List<MateriaHistorial>): List<FilaKardexPdf> =
        materias.map { materia ->
            FilaKardexPdf(
                numero = materia.numero,
                materia = materia.nombre.uppercase(),
                creditos = materia.creditos.toString(),
                semestre = materia.semestre.toString(),
                calificacionEvaluacion = formatearCalificacion(materia.calificacion),
                periodoEvaluacion = materia.periodo ?: "",
                cursada = materia.estatus.codigo
            )
        }

    fun formatearCalificacion(valor: Double?): String =
        if (valor == null) "" else formatearNumero(valor)

    /** "85.0" -> "85", "91.33" -> "91.33" — igual que el documento oficial. */
    fun formatearNumero(valor: Double): String {
        val redondeado = Math.round(valor * 100) / 100.0
        return if (redondeado == redondeado.toLong().toDouble()) {
            redondeado.toLong().toString()
        } else {
            redondeado.toString().trimEnd('0').trimEnd('.')
        }
    }
}
