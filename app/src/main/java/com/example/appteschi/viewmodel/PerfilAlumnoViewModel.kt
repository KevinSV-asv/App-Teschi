package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.FotoPerfilArchivos
import com.example.appteschi.data.FotoPerfilStore
import com.example.appteschi.data.UserSession
import com.example.appteschi.service.SiiaAccountAuthService
import com.example.appteschi.service.SiiaAuthService
import com.example.appteschi.service.SiiaLoginResultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream

internal const val MENSAJE_FOTO_NO_VALIDA = "No se pudo usar esa imagen. Prueba con otra."

private val NIP_VALIDO = Regex("^\\d{4}$")

/** Qué se ve en "Seguridad de la Cuenta". */
enum class SeccionSeguridad { TODO, CONTRASENA, NIP }

data class PerfilAlumnoUiState(
    val nombreCompleto: String = "",
    val matricula: String = "",
    val carrera: String = "",
    val correo: String = "",
    /** Ruta de la foto de perfil; null si no tiene. */
    val fotoRuta: String? = null,
    /** Cambia con cada foto nueva, para volver a leerla aunque la ruta sea la misma. */
    val fotoVersion: Int = 0,
    val guardandoFoto: Boolean = false,
    val errorFoto: String? = null,
    val seccion: SeccionSeguridad = SeccionSeguridad.TODO,
    val cambiandoNip: Boolean = false,
    val errorNip: String? = null,
    val mensajeNip: String? = null
)

/**
 * Perfil del alumno: sus datos tal como los da el SIIA (de solo lectura), su foto,
 * guardada solo en el teléfono (DEC-048), y la seguridad de la cuenta (DEC-047).
 * El NIP se cambia con `nip.ashx` (`cambiar`). La API no tiene cómo cambiar la
 * contraseña: eso sigue en el portal del SIIA.
 */
class PerfilAlumnoViewModel(
    private val siia: SiiaAuthService = SiiaAccountAuthService,
    private val fotos: FotoPerfilStore = FotoPerfilArchivos
) : ViewModel() {

    private val cuentaFoto = UserSession.matricula.ifBlank { UserSession.usuario }

    private val _uiState = MutableStateFlow(
        PerfilAlumnoUiState(
            nombreCompleto = UserSession.nombreCompleto,
            matricula = UserSession.matricula,
            carrera = UserSession.carrera,
            correo = UserSession.correo,
            fotoRuta = fotos.ruta(cuentaFoto)
        )
    )
    val uiState: StateFlow<PerfilAlumnoUiState> = _uiState.asStateFlow()

    fun seleccionarSeccion(seccion: SeccionSeguridad) = _uiState.update { it.copy(seccion = seccion) }

    /** [abrirOrigen] abre la imagen que eligió el alumno; se llama varias veces porque se lee en varias pasadas. */
    fun cambiarFoto(abrirOrigen: () -> InputStream?) {
        if (_uiState.value.guardandoFoto) return
        _uiState.update { it.copy(guardandoFoto = true, errorFoto = null) }
        viewModelScope.launch {
            val ruta = fotos.guardar(cuentaFoto, abrirOrigen)
            _uiState.update {
                if (ruta == null) {
                    it.copy(guardandoFoto = false, errorFoto = MENSAJE_FOTO_NO_VALIDA)
                } else {
                    it.copy(guardandoFoto = false, fotoRuta = ruta, fotoVersion = it.fotoVersion + 1)
                }
            }
        }
    }

    fun quitarFoto() {
        viewModelScope.launch {
            fotos.quitar(cuentaFoto)
            _uiState.update { it.copy(fotoRuta = null, fotoVersion = it.fotoVersion + 1) }
        }
    }

    fun errorFotoMostrado() = _uiState.update { it.copy(errorFoto = null) }

    /**
     * `nip.ashx` no pide la contraseña: para que "autorizar" sea real se comprueba
     * antes con `login.ashx`, y solo si es correcta se cambia el NIP.
     */
    fun cambiarNip(password: String, nipActual: String, nuevoNip: String, confirmarNip: String) {
        val error = when {
            password.isBlank() -> "Escribe tu contraseña actual para autorizar el cambio."
            !NIP_VALIDO.matches(nipActual) -> "Escribe tu NIP actual de 4 dígitos."
            !NIP_VALIDO.matches(nuevoNip) -> "El nuevo NIP debe tener 4 dígitos."
            nuevoNip != confirmarNip -> "Los NIP nuevos no coinciden."
            nuevoNip == nipActual -> "El nuevo NIP debe ser distinto del actual."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorNip = error, mensajeNip = null) }
            return
        }
        val token = UserSession.token
        if (token.isNullOrBlank()) {
            _uiState.update { it.copy(errorNip = "Tu sesión del SIIA terminó. Inicia sesión de nuevo.") }
            return
        }
        _uiState.update { it.copy(cambiandoNip = true, errorNip = null, mensajeNip = null) }
        viewModelScope.launch {
            val usuario = UserSession.usuario.ifBlank { UserSession.matricula }
            val autorizacion = siia.login(usuario, password)
            if (autorizacion is SiiaLoginResultado.Error) {
                _uiState.update { it.copy(cambiandoNip = false, errorNip = autorizacion.mensaje) }
                return@launch
            }
            siia.cambiarNip(token, nipActual, nuevoNip, confirmarNip).fold(
                onSuccess = { m -> _uiState.update { it.copy(cambiandoNip = false, mensajeNip = m) } },
                onFailure = { e ->
                    _uiState.update { it.copy(cambiandoNip = false, errorNip = e.message ?: "No se pudo cambiar tu NIP.") }
                }
            )
        }
    }

    fun limpiarMensajeNip() = _uiState.update { it.copy(errorNip = null, mensajeNip = null) }

    fun cerrarSesion() = UserSession.limpiar()
}
