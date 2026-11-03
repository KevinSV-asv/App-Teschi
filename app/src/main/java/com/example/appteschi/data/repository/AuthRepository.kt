package com.example.appteschi.data.repository

import com.example.appteschi.data.CuentaSiia
import com.example.appteschi.service.SesionSiia
import com.example.appteschi.service.SiiaAccountAuthService
import com.example.appteschi.service.SiiaAuthService
import com.example.appteschi.service.SiiaLoginResultado
import com.example.appteschi.service.SiiaNipResultado

/** Mensaje para docentes y personal administrativo, que todavía no tienen pantallas en la app. */
const val MENSAJE_SOLO_ALUMNOS =
    "Por ahora AppTESCHI es solo para alumnos. El acceso para docentes y personal administrativo llegará más adelante."

/** Resultado de cada paso del login — decide a qué paso pasa la UI. */
sealed class LoginResultado {
    /** Contraseña correcta: falta el NIP (o crearlo si [tieneNip] es false). */
    data class RequiereNip(val tieneNip: Boolean, val tempToken: String, val cuenta: CuentaSiia?) : LoginResultado()

    /** Sesión lista: guardar en UserSession y entrar. */
    data class Sesion(val sesion: SesionSiia) : LoginResultado()

    data class Rechazado(val mensaje: String) : LoginResultado()
}

/**
 * Login de AppTESCHI: contraseña + NIP directo contra la API oficial del SIIA
 * (DEC-041, DEC-043). Ya no hay cuentas propias, administradores, OTP por
 * correo ni usuarios de prueba: el SIIA es la única fuente.
 *
 * No guarda estado: UserSession y el estado de la pantalla son del ViewModel.
 */
class AuthRepository(
    private val siia: SiiaAuthService = SiiaAccountAuthService
) {
    suspend fun iniciarSesion(usuario: String, password: String): LoginResultado =
        when (val r = siia.login(usuario.trim(), password)) {
            is SiiaLoginResultado.RequiereNip ->
                // Si el paso 1 ya dice que no es alumno, no tiene caso pedirle el NIP.
                if (r.cuenta != null && !r.cuenta.esAlumno) LoginResultado.Rechazado(MENSAJE_SOLO_ALUMNOS)
                else LoginResultado.RequiereNip(r.tieneNip, r.tempToken, r.cuenta)
            is SiiaLoginResultado.Sesion -> soloAlumnos(r.sesion)
            is SiiaLoginResultado.Error -> LoginResultado.Rechazado(r.mensaje)
        }

    suspend fun verificarNip(tempToken: String, nip: String, cuentaPaso1: CuentaSiia?): LoginResultado =
        deNip(siia.verificarNip(tempToken, nip, cuentaPaso1))

    suspend fun crearNip(tempToken: String, nuevoNip: String, confirmarNip: String, cuentaPaso1: CuentaSiia?): LoginResultado =
        deNip(siia.crearNip(tempToken, nuevoNip, confirmarNip, cuentaPaso1))

    suspend fun recuperarNip(curp: String, usuario: String): Result<String> = siia.recuperarNip(curp, usuario)

    private fun deNip(r: SiiaNipResultado): LoginResultado = when (r) {
        is SiiaNipResultado.Sesion -> soloAlumnos(r.sesion)
        is SiiaNipResultado.Error -> LoginResultado.Rechazado(r.mensaje)
    }

    private fun soloAlumnos(sesion: SesionSiia): LoginResultado =
        if (sesion.cuenta.esAlumno) LoginResultado.Sesion(sesion) else LoginResultado.Rechazado(MENSAJE_SOLO_ALUMNOS)
}
