package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.service.HistorialAcademicoApi
import com.example.appteschi.service.HistorialAcademicoService
import com.example.appteschi.service.historialDelAlumnoEnSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KardexUiState(
    val cargando: Boolean = true,
    val carrera: String = "",
    val periodoIngreso: String = "",
    val ultimoPeriodo: String = "",
    val promedioGlobal: Double = 0.0,
    val promedioUltimoSemestre: Double = 0.0,
    val totalCreditosCarrera: Int = 0,
    val totalCreditosCursados: Int = 0,
    val porcentajeCubierto: Double = 0.0,
    val totalMaterias: Int = 0,
    val materiasAprobadas: Int = 0,
    val materiasReprobadas: Int = 0,
    val materiasPorAprobar: Int = 0,
    val materias: List<MateriaHistorial> = emptyList(),
    val error: String? = null
)

/**
 * Muestra el historial académico real del alumno (Kardex/Tira de
 * Materias/Calificaciones comparten la misma fuente — ver
 * HistorialAcademicoService.historialDelAlumnoEnSesion) — este ViewModel no
 * guarda su propia copia, solo calcula los totales de resumen a partir de
 * esa lista.
 */
class KardexViewModel(
    private val historialService: HistorialAcademicoApi = HistorialAcademicoService
) : ViewModel() {

    private val _uiState = MutableStateFlow(KardexUiState())
    val uiState: StateFlow<KardexUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun reintentar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            historialDelAlumnoEnSesion(historialService).fold(
                onSuccess = { info ->
                    val materias = info.materias
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            carrera = info.carrera,
                            periodoIngreso = "2022-2",
                            ultimoPeriodo = "2026-1",
                            promedioGlobal = promedioAprobadas(materias),
                            promedioUltimoSemestre = promedioAprobadas(materias.filter { m -> m.semestre == info.semestre - 1 }),
                            totalCreditosCarrera = materias.sumOf { m -> m.creditos },
                            totalCreditosCursados = materias
                                .filter { m -> m.estatus == EstatusMateria.APROBADA }
                                .sumOf { m -> m.creditos },
                            porcentajeCubierto = porcentajeCubierto(materias),
                            totalMaterias = materias.size,
                            materiasAprobadas = materias.count { m -> m.estatus == EstatusMateria.APROBADA },
                            materiasReprobadas = materias.count { m -> m.estatus == EstatusMateria.NO_APROBADA },
                            materiasPorAprobar = materias.count { m -> m.estatus == EstatusMateria.POR_CURSAR },
                            materias = materias
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(cargando = false, error = error.message ?: "No se pudo consultar tu historial académico.")
                    }
                }
            )
        }
    }

    private fun promedioAprobadas(materias: List<MateriaHistorial>): Double {
        val calificaciones = materias.mapNotNull { it.calificacion }
        return if (calificaciones.isEmpty()) 0.0 else calificaciones.average()
    }

    private fun porcentajeCubierto(materias: List<MateriaHistorial>): Double {
        val total = materias.sumOf { it.creditos }
        val cursados = materias.filter { it.estatus == EstatusMateria.APROBADA }.sumOf { it.creditos }
        return if (total == 0) 0.0 else (cursados.toDouble() / total) * 100
    }
}
