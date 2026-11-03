package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.UserSession
import com.example.appteschi.service.CalificacionesSiiaApi
import com.example.appteschi.service.CalificacionesSiiaService
import com.example.appteschi.service.ResultadoParciales
import com.example.appteschi.service.ResumenParcial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InicioUiState(
    val nombreCompleto: String = "",
    val matricula: String = "",
    val carrera: String = "",
    val cargandoResumen: Boolean = true,
    val periodo: String = "",
    val resumen: ResumenParcial? = null,
    val bloqueado: Boolean = false,
    val errorResumen: String? = null
) {
    /** Solo el primer nombre, para el saludo ("¡Hola, Ana!"). */
    val primerNombre: String
        get() = nombreCompleto.trim().split(" ").firstOrNull { it.isNotBlank() }
            ?.lowercase()?.replaceFirstChar { it.uppercase() }
            ?: matricula
}

/**
 * Tab "Inicio": quién es el alumno (datos del SIIA guardados al iniciar
 * sesión) y el resumen de su periodo actual (calificaciones parciales del
 * SIIA). Si el resumen falla, el saludo se sigue viendo: Inicio nunca queda "roto".
 */
class InicioViewModel(
    private val siiaService: CalificacionesSiiaApi = CalificacionesSiiaService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InicioUiState(
            nombreCompleto = UserSession.nombreCompleto,
            matricula = UserSession.matricula,
            carrera = UserSession.carrera
        )
    )
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _uiState.update { it.copy(cargandoResumen = true, errorResumen = null) }
        viewModelScope.launch {
            when (val r = siiaService.parciales(null)) {
                is ResultadoParciales.Datos -> _uiState.update {
                    it.copy(
                        cargandoResumen = false,
                        periodo = r.calificaciones.periodo,
                        resumen = r.calificaciones.resumen,
                        bloqueado = r.calificaciones.bloqueado
                    )
                }
                is ResultadoParciales.Error -> _uiState.update {
                    it.copy(cargandoResumen = false, errorResumen = r.mensaje)
                }
            }
        }
    }
}
