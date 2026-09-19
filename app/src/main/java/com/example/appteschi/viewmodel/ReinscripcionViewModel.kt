package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.AuditTrail
import com.example.appteschi.data.EstatusReinscripcion
import com.example.appteschi.data.GrupoReinscripcion
import com.example.appteschi.data.UserSession
import com.example.appteschi.service.ReinscripcionApi
import com.example.appteschi.service.ReinscripcionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estados según el flujo real del portal SIIA (Registro > Reinscripción):
 * - REGULAR: el sistema asigna la tira automáticamente, sin selección.
 * - IRREGULAR_ELEGIBLE: el alumno elige el grupo del próximo periodo (el turno
 *   no se pide por separado — va codificado en el propio grupo).
 * - BLOQUEADO: el alumno tiene una observación de reglamento pendiente y
 *   debe presentarse con su director de carrera antes de continuar (Fase 2).
 */
enum class ReinscripcionEstado { LOADING, REGULAR, IRREGULAR_ELEGIBLE, BLOQUEADO, SENDING, SUCCESS, ERROR }

data class ReinscripcionUiState(
    val estado: ReinscripcionEstado = ReinscripcionEstado.LOADING,
    val periodo: String = "",
    /** Clave del grupo que el sistema asigna solo (estatus REGULAR). */
    val grupoAsignado: String = "",
    /** Grupos entre los que puede elegir un alumno irregular. */
    val gruposDisponibles: List<GrupoReinscripcion> = emptyList(),
    /** Grupo elegido por el alumno (estatus IRREGULAR_ELEGIBLE). */
    val grupoSeleccionado: String = "",
    /** Motivo real de la observación de reglamento (estatus BLOQUEADO). */
    val motivoBloqueo: String = "",
    val error: String? = null,
    val folio: String = ""
)

/**
 * Reinscripción real (Fase 1): consulta el estatus (regular/irregular) del
 * alumno en sesión contra su historial académico real, y guarda de verdad
 * la solicitud que envía — ver ReinscripcionService/02_MODULOS/01_Reinscripcion.md.
 */
class ReinscripcionViewModel(
    private val reinscripcionService: ReinscripcionApi = ReinscripcionService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReinscripcionUiState())
    val uiState: StateFlow<ReinscripcionUiState> = _uiState.asStateFlow()

    init {
        cargarEstatus()
    }

    private fun cargarEstatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(estado = ReinscripcionEstado.LOADING, error = null) }
            reinscripcionService.estatus(UserSession.matricula).fold(
                onSuccess = { info ->
                    when (info.estatus) {
                        EstatusReinscripcion.REGULAR -> _uiState.update {
                            it.copy(
                                estado = ReinscripcionEstado.REGULAR,
                                periodo = info.grupoAsignado?.periodo.orEmpty(),
                                grupoAsignado = info.grupoAsignado?.clave.orEmpty()
                            )
                        }
                        EstatusReinscripcion.IRREGULAR -> _uiState.update {
                            it.copy(
                                estado = ReinscripcionEstado.IRREGULAR_ELEGIBLE,
                                periodo = info.grupos.firstOrNull()?.periodo.orEmpty(),
                                gruposDisponibles = info.grupos
                            )
                        }
                        EstatusReinscripcion.BLOQUEADO -> _uiState.update {
                            it.copy(
                                estado = ReinscripcionEstado.BLOQUEADO,
                                motivoBloqueo = info.motivo ?: "Tienes una observación de reglamento pendiente."
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(estado = ReinscripcionEstado.ERROR, error = error.message ?: "No se pudo consultar tu estatus de reinscripción.")
                    }
                }
            )
        }
    }

    fun actualizarGrupo(valor: String) = _uiState.update { it.copy(grupoSeleccionado = valor, error = null) }

    fun reintentar() = cargarEstatus()

    fun enviar() {
        val actual = _uiState.value
        val claveGrupo = if (actual.estado == ReinscripcionEstado.REGULAR) actual.grupoAsignado else actual.grupoSeleccionado
        if (claveGrupo.isBlank()) {
            _uiState.update { it.copy(error = "Selecciona un grupo.") }
            return
        }

        _uiState.update { it.copy(error = null, estado = ReinscripcionEstado.SENDING) }
        viewModelScope.launch {
            reinscripcionService.enviarSolicitud(UserSession.matricula, claveGrupo).fold(
                onSuccess = { folio ->
                    AuditTrail.record(
                        UserSession.nombreMostrar,
                        "Solicitud de reinscripción enviada",
                        "Folio: $folio, grupo: $claveGrupo"
                    )
                    _uiState.update { it.copy(estado = ReinscripcionEstado.SUCCESS, folio = folio) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(estado = ReinscripcionEstado.ERROR, error = error.message ?: "No se pudo enviar tu solicitud.")
                    }
                }
            )
        }
    }
}
