package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.AuditTrail
import com.example.appteschi.data.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Estados según el flujo real del portal SIIA (Registro > Reinscripción):
 * - REGULAR: el sistema asigna la tira automáticamente, sin selección.
 * - IRREGULAR_ELEGIBLE: el alumno elige el grupo del próximo periodo (el turno
 *   no se pide por separado — va codificado en el propio grupo, ver GruposIsc).
 * - BLOQUEADO: el alumno tiene una observación de reglamento pendiente y
 *   debe presentarse con su director de carrera antes de continuar.
 */
enum class ReinscripcionEstado { LOADING, REGULAR, IRREGULAR_ELEGIBLE, BLOQUEADO, SENDING, SUCCESS, ERROR }

data class ReinscripcionUiState(
    val estado: ReinscripcionEstado = ReinscripcionEstado.LOADING,
    val periodo: String = "2026-2",
    val grupo: String = "",
    val error: String? = null,
    val folio: String = ""
)

/**
 * MOCK — todavía no existe scraping real de reinscripción contra el SIIA.
 * Qué caso (REGULAR / IRREGULAR_ELEGIBLE / BLOQUEADO) le corresponde a cada
 * alumno vendría de la pestaña "Carga Académica" del portal real, que aún
 * no está integrada — por ahora se resuelve siempre a IRREGULAR_ELEGIBLE,
 * que es el único caso con flujo interactivo que sí podemos simular sin
 * inventar datos académicos. Ver 02_MODULOS/01_Reinscripcion.md en la bóveda.
 */
class ReinscripcionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReinscripcionUiState())
    val uiState: StateFlow<ReinscripcionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(350)
            _uiState.update { it.copy(estado = ReinscripcionEstado.IRREGULAR_ELEGIBLE) }
        }
    }

    fun actualizarGrupo(valor: String) = _uiState.update { it.copy(grupo = valor) }

    fun enviar() {
        val actual = _uiState.value
        val error = when {
            actual.periodo.isBlank() -> "El periodo es obligatorio."
            actual.grupo.isBlank() -> "El grupo es obligatorio."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }

        _uiState.update { it.copy(error = null, estado = ReinscripcionEstado.SENDING) }
        viewModelScope.launch {
            delay(700)
            val folio = "RE-${UUID.randomUUID().toString().take(8).uppercase()}"
            val estadoActual = _uiState.value
            AuditTrail.record(
                UserSession.nombreMostrar,
                "Selección de reinscripción registrada",
                "Folio: $folio, periodo: ${estadoActual.periodo}, grupo: ${estadoActual.grupo}"
            )
            _uiState.update { it.copy(estado = ReinscripcionEstado.SUCCESS, folio = folio) }
        }
    }
}
