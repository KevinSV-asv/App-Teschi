package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.service.HistorialAcademicoApi
import com.example.appteschi.service.HistorialAcademicoService
import com.example.appteschi.service.PlanEstudiosApi
import com.example.appteschi.service.PlanEstudiosService
import com.example.appteschi.service.historialDelAlumnoEnSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TiraMateriasUiState(
    val cargando: Boolean = true,
    val periodo: String = "",
    val grupo: String = "",
    val materias: List<MateriaHistorial> = emptyList(),
    val error: String? = null
)

/**
 * Muestra las materias POR_CURSAR del semestre en el que el alumno está
 * inscrito actualmente — mismo historial real que consume Kardex (ver
 * HistorialAcademicoService.historialDelAlumnoEnSesion), no una lista
 * aparte. El grupo se deriva de dbo.Grupos (vía PlanEstudiosService),
 * filtrado por la carrera real del alumno — ya no depende de GruposIsc, que
 * solo cubría ISC.
 */
class TiraMateriasViewModel(
    private val historialService: HistorialAcademicoApi = HistorialAcademicoService,
    private val planEstudiosService: PlanEstudiosApi = PlanEstudiosService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TiraMateriasUiState())
    val uiState: StateFlow<TiraMateriasUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun reintentar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            historialDelAlumnoEnSesion(historialService).fold(
                onSuccess = { info ->
                    val porCursar = info.materias.filter {
                        it.estatus == EstatusMateria.POR_CURSAR && it.semestre == info.semestre
                    }
                    val grupos = planEstudiosService.grupos(info.claveCarrera).getOrDefault(emptyList())
                    val grupo = grupos.firstOrNull { it.semestre == info.semestre }?.clave.orEmpty()
                    val periodo = grupos.firstOrNull { it.semestre == info.semestre }?.periodo.orEmpty()
                    _uiState.update {
                        it.copy(cargando = false, periodo = periodo, grupo = grupo, materias = porCursar)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(cargando = false, error = error.message ?: "No se pudo consultar tu carga académica.")
                    }
                }
            )
        }
    }
}
