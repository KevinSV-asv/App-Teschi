package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.AuditTrail
import com.example.appteschi.data.UserSession
import com.example.appteschi.data.repository.AuthRepository
import com.example.appteschi.data.repository.LoginResultado
import com.example.appteschi.service.TipoCuenta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginPaso {
    CREDENCIALES,
    VERIFICACION
}

data class AuthUiState(
    val paso: LoginPaso = LoginPaso.CREDENCIALES,
    /** ALUMNO o ADMINISTRADOR — a quién pertenece el paso de OTP en curso. */
    val tipoPendiente: TipoCuenta = TipoCuenta.ALUMNO,
    val matriculaValidada: String = "",
    val nombreValidado: String = "",
    /** Solo se llena cuando tipoPendiente == ADMINISTRADOR. */
    val rolValidado: String? = null,
    val cargando: Boolean = false,
    val mensajeCarga: String? = null,
    val otpEnviado: Boolean = false,
    val errorLogin: String? = null,
    val errorEnvio: String? = null,
    val intentosFallidos: Int = 0,
    val bloqueado: Boolean = false,
    val irAlDashboard: Boolean = false,
    /** Token de sesión de administrador — solo se llena tras un verificarOtp
     *  exitoso con tipoPendiente == ADMINISTRADOR (ver DEC-020). */
    val tokenAdmin: String? = null
)

/**
 * El código OTP ya no vive aquí (ni en ningún otro lado del cliente): se
 * genera, guarda y verifica enteramente en el backend (ver DEC-019,
 * AuthRepository.enviarOtp/verificarOtpRemoto). Este ViewModel solo orquesta
 * las llamadas y refleja lo que el servidor responde.
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun validarCredencialesSiia(matricula: String, password: String) {
        val matriculaLimpia = matricula.trim()

        when {
            matriculaLimpia.isBlank() || password.isBlank() ->
                _uiState.update { it.copy(errorLogin = "Ingresa tu matrícula y contraseña") }
            else -> viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        cargando = true,
                        mensajeCarga = "Validando credenciales…",
                        errorLogin = null,
                        errorEnvio = null
                    )
                }

                when (val resultado = authRepository.iniciarSesion(matriculaLimpia, password)) {
                    is LoginResultado.AlumnoPruebaSinOtp ->
                        entrarComoAlumnoDePrueba(resultado.matricula, resultado.nombre, resultado.semestreSimulado)

                    is LoginResultado.AdministradorRequiereOtp -> avanzarAVerificacion(
                        tipo = TipoCuenta.ADMINISTRADOR,
                        identificador = resultado.usuario,
                        nombre = resultado.nombre,
                        rol = resultado.rol
                    )

                    is LoginResultado.CuentaPropiaRequiereOtp -> avanzarAVerificacion(
                        tipo = TipoCuenta.ALUMNO,
                        identificador = resultado.matricula,
                        nombre = resultado.nombre
                    )

                    is LoginResultado.SiiaRequiereOtp -> avanzarAVerificacion(
                        tipo = TipoCuenta.ALUMNO,
                        identificador = resultado.matricula,
                        nombre = ""
                    )

                    is LoginResultado.Rechazado -> {
                        AuditTrail.record(matriculaLimpia, "Acceso rechazado", "Credenciales inválidas")
                        _uiState.update {
                            it.copy(
                                cargando = false,
                                mensajeCarga = null,
                                errorLogin = resultado.mensaje
                            )
                        }
                    }
                }
            }
        }
    }

    private fun avanzarAVerificacion(tipo: TipoCuenta, identificador: String, nombre: String, rol: String? = null) {
        _uiState.update {
            it.copy(
                cargando = false,
                mensajeCarga = null,
                paso = LoginPaso.VERIFICACION,
                tipoPendiente = tipo,
                matriculaValidada = identificador,
                nombreValidado = nombre,
                rolValidado = rol,
                otpEnviado = false,
                intentosFallidos = 0,
                bloqueado = false,
                errorLogin = null
            )
        }
    }

    fun enviarCodigoVerificacion(
        correoOtp: String,
        onEnviado: (correo: String) -> Unit
    ) {
        val correoLimpio = correoOtp.trim().lowercase()
        if (correoLimpio.isBlank() || !correoLimpio.contains("@")) {
            _uiState.update { it.copy(errorEnvio = "Ingresa un correo válido") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    cargando = true,
                    mensajeCarga = "Enviando código…",
                    errorEnvio = null
                )
            }

            val estado = _uiState.value
            val resultado = authRepository.enviarOtp(estado.tipoPendiente, estado.matriculaValidada, correoLimpio)
            _uiState.update {
                it.copy(
                    cargando = false,
                    mensajeCarga = null,
                    otpEnviado = resultado.isSuccess,
                    intentosFallidos = 0,
                    bloqueado = false,
                    errorEnvio = resultado.exceptionOrNull()?.message
                )
            }

            if (resultado.isSuccess) {
                onEnviado(correoLimpio)
            }
        }
    }

    fun reenviarOtp(correoOtp: String, onEnviado: (correo: String) -> Unit) {
        enviarCodigoVerificacion(correoOtp, onEnviado)
    }

    fun verificarOtp(
        codigoIngresado: String,
        onExito: () -> Unit,
        onFallo: (mensaje: String) -> Unit
    ) {
        // No se exige haber llamado a enviarCodigoVerificacion() en ESTA
        // sesión de pantalla — el usuario puede traer un código de un envío
        // anterior, todavía dentro de sus 10 minutos de vigencia ("Ya tengo
        // un código" en LoginScreen). El backend es quien de verdad sabe si
        // hay un código pendiente válido; si no lo hay, responde con un
        // mensaje claro ("No hay un código pendiente...") en vez de que la
        // app lo bloquee de antemano.
        if (codigoIngresado.length != 6) {
            onFallo("Ingresa los 6 dígitos")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeCarga = "Verificando…") }
            val estado = _uiState.value
            val resultado = authRepository.verificarOtpRemoto(estado.tipoPendiente, estado.matriculaValidada, codigoIngresado)
            _uiState.update { it.copy(cargando = false, mensajeCarga = null) }

            resultado.fold(
                onSuccess = { token ->
                    _uiState.update { it.copy(tokenAdmin = token) }
                    onExito()
                },
                onFailure = { error ->
                    val mensaje = error.message ?: "Código incorrecto"
                    val bloqueadoAhora = mensaje.contains("Demasiados", ignoreCase = true)
                    _uiState.update {
                        it.copy(intentosFallidos = it.intentosFallidos + 1, bloqueado = bloqueadoAhora)
                    }
                    onFallo(mensaje)
                }
            )
        }
    }

    fun volverAlLogin() {
        _uiState.value = AuthUiState()
    }

    fun limpiarErrores() {
        _uiState.update { it.copy(errorLogin = null, errorEnvio = null) }
    }

    private fun entrarComoAlumnoDePrueba(matricula: String, nombre: String, semestreSimulado: Int?) {
        UserSession.establecer(
            matricula = matricula,
            nombreCompleto = nombre,
            esAdministrador = false,
            semestreSimulado = semestreSimulado
        )
        AuditTrail.record(nombre, "Acceso de prueba", "Ingreso sin OTP — solo builds de depuración")
        _uiState.update {
            it.copy(
                cargando = false,
                mensajeCarga = null,
                matriculaValidada = matricula,
                irAlDashboard = true,
                errorLogin = null
            )
        }
    }
}
