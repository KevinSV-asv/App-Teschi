package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.service.CalificacionesFinales
import com.example.appteschi.service.CalificacionesParciales
import com.example.appteschi.service.CalificacionesSiiaApi
import com.example.appteschi.service.CalificacionesSiiaService
import com.example.appteschi.service.ResultadoFinales
import com.example.appteschi.service.ResultadoParciales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VistaCalificaciones { PARCIALES, FINALES }

data class CalificacionesUiState(
    val vista: VistaCalificaciones = VistaCalificaciones.PARCIALES,
    val cargando: Boolean = true,
    val error: String? = null,
    val parciales: CalificacionesParciales? = null,
    /** Error al cambiar de periodo: se muestra sobre las parciales que ya se veían, en vez de ocultarlas. */
    val errorPeriodo: String? = null,
    val cargandoFinales: Boolean = false,
    val errorFinales: String? = null,
    val finales: CalificacionesFinales? = null,
    /** Igual que [errorPeriodo], para las finales. */
    val errorPeriodoFinales: String? = null
)

/**
 * Calificaciones del SIIA por periodo: parciales 1–3 (DEC-042) y finales con
 * 1ra y 2da vuelta (DEC-044). Las finales se piden hasta que el alumno abre esa
 * vista, y el periodo elegido se conserva al cambiar de una a otra.
 */
class CalificacionesViewModel(
    private val siiaService: CalificacionesSiiaApi = CalificacionesSiiaService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalificacionesUiState())
    val uiState: StateFlow<CalificacionesUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun reintentar() = when (_uiState.value.vista) {
        VistaCalificaciones.PARCIALES -> cargar(_uiState.value.parciales?.periodo)
        VistaCalificaciones.FINALES -> cargarFinales(_uiState.value.finales?.periodo ?: periodoParciales())
    }

    fun seleccionarVista(vista: VistaCalificaciones) {
        val e = _uiState.value
        if (vista == e.vista) return
        _uiState.update { it.copy(vista = vista) }
        when (vista) {
            VistaCalificaciones.FINALES -> {
                val periodo = periodoParciales()
                if (e.finales == null || (periodo != null && periodo != e.finales.periodo)) cargarFinales(periodo)
            }
            VistaCalificaciones.PARCIALES -> {
                val periodo = e.finales?.periodo?.takeIf { it.isNotBlank() }
                if (e.parciales == null && !e.cargando) cargar(periodo)
                else if (e.parciales != null && periodo != null && periodo != e.parciales.periodo) cargar(periodo)
            }
        }
    }

    fun seleccionarPeriodo(periodo: String) {
        val e = _uiState.value
        when (e.vista) {
            VistaCalificaciones.PARCIALES ->
                if (periodo != e.parciales?.periodo) cargar(periodo)
                else _uiState.update { it.copy(errorPeriodo = null) }
            VistaCalificaciones.FINALES ->
                if (periodo != e.finales?.periodo) cargarFinales(periodo)
                else _uiState.update { it.copy(errorPeriodoFinales = null) }
        }
    }

    private fun periodoParciales(): String? = _uiState.value.parciales?.periodo?.takeIf { it.isNotBlank() }

    private fun cargar(periodo: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null, errorPeriodo = null) }
            when (val r = siiaService.parciales(periodo)) {
                is ResultadoParciales.Datos -> _uiState.update { it.copy(cargando = false, parciales = r.calificaciones) }
                is ResultadoParciales.Error -> {
                    val anteriores = _uiState.value.parciales
                    _uiState.update {
                        if (periodo != null && anteriores != null) {
                            it.copy(cargando = false, errorPeriodo = "No se pudo cargar el periodo $periodo. ${r.mensaje}")
                        } else {
                            it.copy(cargando = false, error = r.mensaje)
                        }
                    }
                }
            }
        }
    }

    private fun cargarFinales(periodo: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoFinales = true, errorFinales = null, errorPeriodoFinales = null) }
            when (val r = siiaService.finales(periodo)) {
                is ResultadoFinales.Datos -> _uiState.update { it.copy(cargandoFinales = false, finales = r.calificaciones) }
                is ResultadoFinales.Error -> {
                    val anteriores = _uiState.value.finales
                    _uiState.update {
                        if (periodo != null && anteriores != null) {
                            it.copy(cargandoFinales = false, errorPeriodoFinales = "No se pudo cargar el periodo $periodo. ${r.mensaje}")
                        } else {
                            it.copy(cargandoFinales = false, errorFinales = r.mensaje)
                        }
                    }
                }
            }
        }
    }
}
