package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.UserSession
import com.example.appteschi.service.PerfilAlumno
import com.example.appteschi.service.PerfilAlumnoApi
import com.example.appteschi.service.PerfilAlumnoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Mismo patrón que RegistroViewModel/RecuperarPasswordViewModel. */
private val PASSWORD_VALIDA = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)[A-Za-z\\d]{8,}$")
private val CORREO_VALIDO = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

data class PerfilAlumnoUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val perfil: PerfilAlumno? = null,
    val guardandoCorreo: Boolean = false,
    val errorCorreo: String? = null,
    val correoActualizado: Boolean = false,
    val cambiandoPassword: Boolean = false,
    val errorPassword: String? = null,
    val passwordCambiada: Boolean = false
)

/**
 * Perfil del alumno ya logueado: ver sus datos (nombre, carrera y semestre
 * son de solo lectura, los controla el director) y editar su correo de
 * recuperación + cambiar su contraseña dando la actual — sin pasar por el
 * flujo de "olvidé mi contraseña" (ver RecuperarPasswordViewModel).
 */
class PerfilAlumnoViewModel(
    private val servicio: PerfilAlumnoApi = PerfilAlumnoService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilAlumnoUiState())
    val uiState: StateFlow<PerfilAlumnoUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            servicio.obtener(UserSession.matricula).fold(
                onSuccess = { perfil ->
                    _uiState.update { it.copy(cargando = false, perfil = perfil) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(cargando = false, error = error.message ?: "No se pudo cargar tu perfil.") }
                }
            )
        }
    }

    fun actualizarCorreo(nuevoCorreo: String) {
        val limpio = nuevoCorreo.trim()
        if (!CORREO_VALIDO.matches(limpio)) {
            _uiState.update { it.copy(errorCorreo = "Ingresa un correo válido.") }
            return
        }
        _uiState.update { it.copy(guardandoCorreo = true, errorCorreo = null, correoActualizado = false) }
        viewModelScope.launch {
            servicio.actualizarCorreo(UserSession.matricula, limpio).fold(
                onSuccess = {
                    _uiState.update { estado ->
                        estado.copy(
                            guardandoCorreo = false,
                            correoActualizado = true,
                            perfil = estado.perfil?.copy(correoOtp = limpio)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(guardandoCorreo = false, errorCorreo = error.message ?: "No se pudo actualizar tu correo.") }
                }
            )
        }
    }

    fun cambiarPassword(passwordActual: String, passwordNueva: String, confirmarPassword: String) {
        if (passwordActual.isBlank()) {
            _uiState.update { it.copy(errorPassword = "Ingresa tu contraseña actual.") }
            return
        }
        if (passwordNueva != confirmarPassword) {
            _uiState.update { it.copy(errorPassword = "Las contraseñas nuevas no coinciden.") }
            return
        }
        if (!PASSWORD_VALIDA.matches(passwordNueva)) {
            _uiState.update {
                it.copy(errorPassword = "La contraseña requiere mínimo 8 caracteres, una mayúscula, una minúscula y un número.")
            }
            return
        }
        _uiState.update { it.copy(cambiandoPassword = true, errorPassword = null, passwordCambiada = false) }
        viewModelScope.launch {
            servicio.cambiarPassword(UserSession.matricula, passwordActual, passwordNueva).fold(
                onSuccess = {
                    _uiState.update { it.copy(cambiandoPassword = false, passwordCambiada = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(cambiandoPassword = false, errorPassword = error.message ?: "No se pudo cambiar tu contraseña.") }
                }
            )
        }
    }

    fun limpiarMensajeCorreo() = _uiState.update { it.copy(correoActualizado = false, errorCorreo = null) }
    fun limpiarMensajePassword() = _uiState.update { it.copy(passwordCambiada = false, errorPassword = null) }
}
