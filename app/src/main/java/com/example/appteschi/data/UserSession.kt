package com.example.appteschi.data

import android.content.Context

private const val PREFS_NOMBRE = "appteschi_sesion"
private const val CLAVE_MATRICULA = "matricula"
private const val CLAVE_CORREO_OTP = "correoOtp"
private const val CLAVE_NOMBRE = "nombreCompleto"
private const val CLAVE_ES_ADMIN = "esAdministrador"
private const val CLAVE_ADMIN_TOKEN = "adminToken"
private const val CLAVE_ALUMNO_TOKEN = "alumnoToken"
private const val CLAVE_ROL = "rol"

/**
 * Sesión del usuario autenticado. Los campos de administrador (adminToken,
 * rol) se persisten en SharedPreferences para sobrevivir a que Android mate
 * el proceso o a que se reinstale el APK — sin esto, cada reinicio de la app
 * obligaba a repetir el login con OTP aunque el token del servidor (vigente
 * 12h, ver DEC-020) siguiera siendo válido. matricula/correoOtp/nombreCompleto
 * también se guardan para no perder la sesión de alumno en el mismo caso.
 */
object UserSession {
    private var prefs: android.content.SharedPreferences? = null

    /** Debe llamarse una vez al arrancar la app (ver MainActivity.onCreate). */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
        val p = prefs ?: return
        matricula = p.getString(CLAVE_MATRICULA, "") ?: ""
        correoOtp = p.getString(CLAVE_CORREO_OTP, "") ?: ""
        nombreCompleto = p.getString(CLAVE_NOMBRE, "") ?: ""
        esAdministrador = p.getBoolean(CLAVE_ES_ADMIN, false)
        adminToken = p.getString(CLAVE_ADMIN_TOKEN, null)
        alumnoToken = p.getString(CLAVE_ALUMNO_TOKEN, null)
        rol = p.getString(CLAVE_ROL, null)
    }

    var matricula: String = ""
    var correoOtp: String = ""
    var nombreCompleto: String = ""
    var esAdministrador: Boolean = false

    /** Token de sesión firmado por el backend — solo lo tiene un administrador
     *  autenticado (ver DEC-020). Cada llamada a un endpoint administrativo
     *  lo manda como `Authorization: Bearer <token>`. Vigente 12h en el
     *  servidor; si expiró, el backend responde 401 y hay que loguearse de
     *  nuevo — la app no valida la expiración por su cuenta. */
    var adminToken: String? = null

    /** Token de sesión del alumno (DEC-030): lo emite el backend al verificar su
     *  OTP y es lo que identifica al alumno en los endpoints mi-* y de reinscripción del alumno.
     *  null en los usuarios de prueba (sem1..sem9, alumno), que no llaman al
     *  backend con datos reales. */
    var alumnoToken: String? = null

    /** SUPERADMIN u OPERADOR — solo tiene sentido cuando esAdministrador es true. */
    var rol: String? = null

    /**
     * Semestre a simular (solo lo llenan los usuarios de prueba sem1..sem9,
     * ver AuthRepository). null = usar el historial académico real. No se
     * persiste a propósito: es un atajo de prueba, no una sesión real.
     */
    var semestreSimulado: Int? = null

    /** Semestre "actual" para Tira de Materias/Calificaciones: el simulado si
     *  hay uno activo, o si no, el semestre real (9, ver HistorialAcademico). */
    val semestreActual: Int get() = semestreSimulado ?: 9

    fun establecer(
        matricula: String,
        correoOtp: String = "",
        nombreCompleto: String = "",
        esAdministrador: Boolean = false,
        semestreSimulado: Int? = null,
        adminToken: String? = null,
        rol: String? = null,
        alumnoToken: String? = null
    ) {
        this.matricula = matricula
        this.correoOtp = correoOtp
        this.nombreCompleto = nombreCompleto
        this.esAdministrador = esAdministrador
        this.semestreSimulado = semestreSimulado
        this.adminToken = adminToken
        this.alumnoToken = alumnoToken
        this.rol = rol
        guardar()
    }

    fun limpiar() {
        matricula = ""
        correoOtp = ""
        nombreCompleto = ""
        esAdministrador = false
        semestreSimulado = null
        adminToken = null
        alumnoToken = null
        rol = null
        prefs?.edit()?.clear()?.apply()
    }

    private fun guardar() {
        prefs?.edit()?.apply {
            putString(CLAVE_MATRICULA, matricula)
            putString(CLAVE_CORREO_OTP, correoOtp)
            putString(CLAVE_NOMBRE, nombreCompleto)
            putBoolean(CLAVE_ES_ADMIN, esAdministrador)
            if (adminToken == null) remove(CLAVE_ADMIN_TOKEN) else putString(CLAVE_ADMIN_TOKEN, adminToken)
            if (alumnoToken == null) remove(CLAVE_ALUMNO_TOKEN) else putString(CLAVE_ALUMNO_TOKEN, alumnoToken)
            if (rol == null) remove(CLAVE_ROL) else putString(CLAVE_ROL, rol)
        }?.apply()
    }

    val nombreMostrar: String
        get() = when {
            nombreCompleto.isNotBlank() -> nombreCompleto.uppercase()
            esAdministrador -> "ADMINISTRADOR"
            matricula.isNotBlank() -> "ALUMNO $matricula"
            else -> "ALUMNO TESCHI"
        }
}
