package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.service.RecuperarPasswordApi
import com.example.appteschi.service.RecuperarPasswordService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Mismo patrón que RegistroViewModel — 8 caracteres, mayúscula, minúscula y número. */
private val PASSWORD_VALIDA = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)[A-Za-z\\d]{8,}$")

enum class RecuperarPasswordPaso { MATRICULA, CODIGO_Y_NUEVA, EXITO }

data class RecuperarPasswordUiState(
    val paso: RecuperarPasswordPaso = RecuperarPasswordPaso.MATRICULA,
    val cargando: Boolean = false,
    val correoEnmascarado: String = "",
    val error: String? = null
)

/**
 * Recuperar contraseña — solo cuentas propias de AppTESCHI (ver
 * RecuperarPasswordService). El código se manda siempre al correo que ya
 * está registrado; la app solo pide la matrícula, nunca un correo.
 */
class RecuperarPasswordViewModel(
    private val servicio: RecuperarPasswordApi = RecuperarPasswordService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecuperarPasswordUiState())
    val uiState: StateFlow<RecuperarPasswordUiState> = _uiState.asStateFlow()

    private var matriculaValidada: String = ""

    fun solicitarCodigo(matricula: String) {
        val limpia = matricula.trim()
        if (limpia.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa tu matrícula") }
            return
        }
        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            servicio.solicitar(limpia).fold(
                onSuccess = { correo ->
                    matriculaValidada = limpia
                    _uiState.update {
                        it.copy(cargando = false, paso = RecuperarPasswordPaso.CODIGO_Y_NUEVA, correoEnmascarado = correo)
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(cargando = false, error = error.message ?: "No se pudo enviar el código.") }
                }
            )
        }
    }

    fun reenviarCodigo() = solicitarCodigo(matriculaValidada)

    fun confirmar(codigo: String, nuevaPassword: String, confirmarPassword: String) {
        if (codigo.trim().length != 6) {
            _uiState.update { it.copy(error = "Ingresa los 6 dígitos del código.") }
            return
        }
        if (nuevaPassword != confirmarPassword) {
            _uiState.update { it.copy(error = "Las contraseñas no coinciden.") }
            return
        }
        if (!PASSWORD_VALIDA.matches(nuevaPassword)) {
            _uiState.update {
                it.copy(error = "La contraseña requiere mínimo 8 caracteres, una mayúscula, una minúscula y un número.")
            }
            return
        }

        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            servicio.confirmar(matriculaValidada, codigo.trim(), nuevaPassword).fold(
                onSuccess = {
                    _uiState.update { it.copy(cargando = false, paso = RecuperarPasswordPaso.EXITO) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(cargando = false, error = error.message ?: "No se pudo restablecer tu contraseña.") }
                }
            )
        }
    }

    fun limpiarError() = _uiState.update { it.copy(error = null) }

    fun volverAMatricula() = _uiState.update {
        RecuperarPasswordUiState(paso = RecuperarPasswordPaso.MATRICULA)
    }
}
