package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.service.CalificacionesSiiaApi
import com.example.appteschi.service.CalificacionesSiiaService
import com.example.appteschi.service.ResultadoParciales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Una materia de la carga del periodo, con su profesor y grupo. */
data class MateriaTira(val materia: String, val profesor: String, val grupo: String)

data class TiraMateriasUiState(
    val cargando: Boolean = true,
    val periodo: String = "",
    /** Grupos distintos del periodo (normalmente uno). */
    val grupos: List<String> = emptyList(),
    val materias: List<MateriaTira> = emptyList(),
    val bloqueado: Boolean = false,
    val mensajeBloqueo: String = "",
    val pendientes: List<String> = emptyList(),
    val aviso: String = "",
    val error: String? = null
)

/**
 * La API del SIIA no tiene un endpoint de tira de materias: la carga del
 * periodo actual se arma con las materias de las calificaciones parciales
 * (materia, profesor y grupo), que son las que el alumno está cursando.
 */
class TiraMateriasViewModel(
    private val siiaService: CalificacionesSiiaApi = CalificacionesSiiaService
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
            when (val r = siiaService.parciales(null)) {
                is ResultadoParciales.Datos -> {
                    val c = r.calificaciones
                    val materias = c.materias.map { MateriaTira(it.materia, it.profesor, it.grupo) }
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            periodo = c.periodo,
                            grupos = materias.map { m -> m.grupo.trim() }.filter { g -> g.isNotEmpty() }.distinct(),
                            materias = materias,
                            bloqueado = c.bloqueado,
                            mensajeBloqueo = c.mensajeBloqueo,
                            pendientes = c.pendientes,
                            aviso = c.aviso
                        )
                    }
                }
                is ResultadoParciales.Error -> _uiState.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }
}
