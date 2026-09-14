package com.example.appteschi.data.repository

import com.example.appteschi.BuildConfig
import com.example.appteschi.service.AdminAccountAuthService
import com.example.appteschi.service.AdminAuthService
import com.example.appteschi.service.CuentaAuthService
import com.example.appteschi.service.LocalAccountAuthService
import com.example.appteschi.service.OtpService
import com.example.appteschi.service.RemoteOtpService
import com.example.appteschi.service.SiiaAuth
import com.example.appteschi.service.SiiaAuthService
import com.example.appteschi.service.TipoCuenta

/** Resultado de intentar iniciar sesión — decide a qué paso pasa la UI. */
sealed class LoginResultado {
    /** Alumno de prueba (solo builds de depuración): entra directo, sin OTP.
     *  semestreSimulado != null cuando es uno de los usuarios sem1..sem9. */
    data class AlumnoPruebaSinOtp(
        val matricula: String,
        val nombre: String,
        val semestreSimulado: Int? = null
    ) : LoginResultado()

    /** Cuenta real de administrador validada — falta el paso de verificación por OTP. */
    data class AdministradorRequiereOtp(val usuario: String, val nombre: String, val rol: String) : LoginResultado()

    /** Cuenta propia de AppTESCHI (alumno) validada — falta el paso de verificación por OTP. */
    data class CuentaPropiaRequiereOtp(val matricula: String, val nombre: String) : LoginResultado()

    /** Credenciales validadas contra el SIIA — falta el paso de verificación por OTP. */
    data class SiiaRequiereOtp(val matricula: String) : LoginResultado()

    /** Ninguna fuente aceptó las credenciales. */
    data class Rechazado(val mensaje: String) : LoginResultado()
}

/**
 * Encapsula la cascada de autenticación de AppTESCHI:
 * bypass de alumno de prueba (solo BuildConfig.DEBUG) → cuenta de
 * administrador real → cuenta propia de alumno → SIIA (scraping).
 *
 * El bypass de administrador ("admin"/"admin" sin OTP) se retiró en DEC-021:
 * desde que los endpoints administrativos exigen un token de sesión (JWT,
 * DEC-020) que solo se emite al verificar el OTP de una cuenta real, ese
 * atajo dejaba al usuario "adentro" pero sin token — cada pantalla fallaba
 * con "Falta la sesión de administrador". Ya no tiene sentido como atajo de
 * desarrollo: usa la cuenta real de administrador (con OTP) para probar.
 *
 * El OTP (envío y verificación) también pasa por aquí, pero como una
 * operación aparte — ver [enviarOtp]/[verificarOtpRemoto] — porque ahora
 * vive enteramente en el backend (DEC-019): la app nunca conoce el código
 * correcto, solo pregunta.
 *
 * No mantiene estado propio a propósito: UserSession, AuditTrail y el estado
 * de la pantalla siguen siendo responsabilidad del ViewModel que lo use.
 */
class AuthRepository(
    private val adminAuthService: AdminAuthService = AdminAccountAuthService,
    private val cuentaAuthService: CuentaAuthService = LocalAccountAuthService,
    private val siiaAuthService: SiiaAuth = SiiaAuthService,
    private val otpService: RemoteOtpService = OtpService
) {
    suspend fun iniciarSesion(matricula: String, password: String): LoginResultado {
        val matriculaLimpia = matricula.trim()

        // Bypass de desarrollo — NUNCA disponible en un build de release.
        if (BuildConfig.DEBUG) {
            if (esAlumnoPruebaLocal(matriculaLimpia, password)) {
                return LoginResultado.AlumnoPruebaSinOtp(matriculaLimpia, "Alumno de Prueba")
            }
            semestrePruebaLocal(matriculaLimpia, password)?.let { semestre ->
                return LoginResultado.AlumnoPruebaSinOtp(
                    matricula = matriculaLimpia,
                    nombre = "Alumno de Prueba — Semestre $semestre",
                    semestreSimulado = semestre
                )
            }
        }

        val cuentaAdmin = adminAuthService.autenticar(matriculaLimpia, password)
        if (cuentaAdmin.isSuccess) {
            val cuenta = cuentaAdmin.getOrThrow()
            return LoginResultado.AdministradorRequiereOtp(cuenta.usuario, cuenta.nombre, cuenta.rol)
        }

        val cuentaLocal = cuentaAuthService.autenticar(matriculaLimpia, password)
        if (cuentaLocal.isSuccess) {
            val cuenta = cuentaLocal.getOrThrow()
            return LoginResultado.CuentaPropiaRequiereOtp(cuenta.matricula, cuenta.nombre)
        }

        val authResult = siiaAuthService.validarCredenciales(matriculaLimpia, password)
        return if (authResult.isSuccess) {
            LoginResultado.SiiaRequiereOtp(matriculaLimpia)
        } else {
            LoginResultado.Rechazado(
                authResult.exceptionOrNull()?.message ?: "Usuario o contraseña incorrectos"
            )
        }
    }

    suspend fun enviarOtp(tipo: TipoCuenta, identificador: String, correo: String): Result<Unit> =
        otpService.enviar(tipo, identificador, correo)

    suspend fun verificarOtpRemoto(tipo: TipoCuenta, identificador: String, codigo: String): Result<String?> =
        otpService.verificar(tipo, identificador, codigo)

    private fun esAlumnoPruebaLocal(matricula: String, password: String): Boolean =
        matricula.equals("alumno", ignoreCase = true) && password == "alumno"

    /**
     * Usuarios de prueba sem1..sem9 (matrícula y contraseña iguales, ej.
     * "sem3"/"sem3") — simulan estar inscrito en ese semestre, para verificar
     * que Tira de Materias/Calificaciones muestren las materias correctas.
     * Solo builds de depuración, igual que esAlumnoPruebaLocal.
     */
    private fun semestrePruebaLocal(matricula: String, password: String): Int? {
        val limpia = matricula.trim().lowercase()
        if (password.trim().lowercase() != limpia) return null
        if (!limpia.startsWith("sem")) return null
        val semestre = limpia.removePrefix("sem").toIntOrNull() ?: return null
        return semestre.takeIf { it in 1..9 }
    }
}
