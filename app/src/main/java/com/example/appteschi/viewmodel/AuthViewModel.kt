package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.CuentaSiia
import com.example.appteschi.data.UserSession
import com.example.appteschi.data.repository.AuthRepository
import com.example.appteschi.data.repository.LoginResultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginPaso {
    CREDENCIALES,
    /** NIP de 4 dígitos del SIIA (DEC-041). */
    NIP
}

data class AuthUiState(
    val paso: LoginPaso = LoginPaso.CREDENCIALES,
    /** Usuario que escribió el alumno; sirve para saber si la CURP de "¿Olvidaste tu NIP?" es suya. */
    val usuario: String = "",
    /** Token temporal del SIIA (10 min) entre la contraseña y el NIP. */
    val tempToken: String = "",
    /** Datos de la cuenta que devolvió el paso 1, si los trajo. */
    val cuentaPaso1: CuentaSiia? = null,
    /** false = el alumno aún no tiene NIP y debe crearlo. */
    val tieneNip: Boolean = true,
    val nombreValidado: String = "",
    val cargando: Boolean = false,
    val mensajeCarga: String? = null,
    val errorLogin: String? = null,
    val errorNip: String? = null,
    /** Aviso informativo del paso NIP (p. ej. "Te enviamos tu NIP a …"). */
    val mensajeNip: String? = null,
    val irAlDashboard: Boolean = false
)

/** Mensaje cuando se intenta el paso NIP sin el token temporal del paso 1. */
internal const val SESION_VERIFICACION_PERDIDA =
    "Se perdió el inicio de sesión. Toca \"Regresar\" y vuelve a escribir tu usuario y contraseña."

private val NIP_VALIDO = Regex("^\\d{4}$")
private val CURP_VALIDA = Regex("^[A-Z0-9]{18}$")

/** Login en dos pasos contra el SIIA: usuario + contraseña, luego NIP (o crearlo). */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun validarCredenciales(usuario: String, password: String) {
        val usuarioLimpio = usuario.trim()
        if (usuarioLimpio.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorLogin = "Ingresa tu usuario y contraseña del SIIA") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeCarga = "Verificando en el SIIA…", errorLogin = null) }
            when (val r = authRepository.iniciarSesion(usuarioLimpio, password)) {
                is LoginResultado.RequiereNip -> _uiState.update {
                    it.copy(
                        cargando = false,
                        mensajeCarga = null,
                        paso = LoginPaso.NIP,
                        usuario = usuarioLimpio,
                        tempToken = r.tempToken,
                        cuentaPaso1 = r.cuenta,
                        tieneNip = r.tieneNip,
                        nombreValidado = r.cuenta?.nombreCompleto.orEmpty(),
                        errorNip = null,
                        mensajeNip = null
                    )
                }
                is LoginResultado.Sesion -> entrar(r)
                is LoginResultado.Rechazado -> _uiState.update {
                    it.copy(cargando = false, mensajeCarga = null, errorLogin = r.mensaje)
                }
            }
        }
    }

    fun verificarNip(nip: String) {
        if (!NIP_VALIDO.matches(nip)) {
            _uiState.update { it.copy(errorNip = "El NIP debe tener 4 dígitos") }
            return
        }
        pasoNip("Verificando tu NIP…") { e -> authRepository.verificarNip(e.tempToken, nip, e.cuentaPaso1) }
    }

    fun crearNip(nuevoNip: String, confirmarNip: String) {
        val error = when {
            !NIP_VALIDO.matches(nuevoNip) -> "El NIP debe tener 4 dígitos"
            nuevoNip != confirmarNip -> "Los NIP no coinciden"
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorNip = error) }
            return
        }
        pasoNip("Creando tu NIP…") { e -> authRepository.crearNip(e.tempToken, nuevoNip, confirmarNip, e.cuentaPaso1) }
    }

    private fun pasoNip(mensaje: String, llamada: suspend (AuthUiState) -> LoginResultado) {
        val estado = _uiState.value
        if (estado.tempToken.isBlank()) {
            _uiState.update { it.copy(errorNip = SESION_VERIFICACION_PERDIDA) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeCarga = mensaje, errorNip = null, mensajeNip = null) }
            when (val r = llamada(estado)) {
                is LoginResultado.Sesion -> entrar(r)
                is LoginResultado.Rechazado -> _uiState.update {
                    it.copy(cargando = false, mensajeCarga = null, errorNip = r.mensaje)
                }
                is LoginResultado.RequiereNip -> _uiState.update { it.copy(cargando = false, mensajeCarga = null) }
            }
        }
    }

    fun recuperarNip(curp: String) {
        val limpia = curp.trim().uppercase()
        if (!CURP_VALIDA.matches(limpia)) {
            _uiState.update { it.copy(errorNip = "La CURP debe tener 18 letras y números") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeCarga = "Enviando tu NIP…", errorNip = null, mensajeNip = null) }
            authRepository.recuperarNip(limpia, _uiState.value.usuario).fold(
                onSuccess = { m -> _uiState.update { it.copy(cargando = false, mensajeCarga = null, mensajeNip = m) } },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(cargando = false, mensajeCarga = null, errorNip = e.message ?: "No se pudo recuperar tu NIP")
                    }
                }
            )
        }
    }

    private fun entrar(r: LoginResultado.Sesion) {
        UserSession.establecer(r.sesion.cuenta, r.sesion.token, r.sesion.expiraEnMinutos)
        _uiState.update {
            it.copy(cargando = false, mensajeCarga = null, tempToken = "", irAlDashboard = true)
        }
    }

    fun volverAlLogin() {
        _uiState.value = AuthUiState()
    }

    fun limpiarErrores() {
        _uiState.update { it.copy(errorLogin = null, errorNip = null) }
    }
}
