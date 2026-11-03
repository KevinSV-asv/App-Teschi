package com.example.appteschi.data

import android.content.Context

private const val PREFS_NOMBRE = "appteschi_sesion"
private const val CLAVE_USUARIO = "usuario"
private const val CLAVE_MATRICULA = "matricula"
private const val CLAVE_NOMBRE = "nombreCompleto"
private const val CLAVE_CORREO = "correo"
private const val CLAVE_CARRERA = "carrera"
private const val CLAVE_ID_CARRERA = "idCarrera"
private const val CLAVE_TOKEN = "tokenSiia"
private const val CLAVE_EXPIRA = "tokenExpiraEn"

/** Claves de la sesión del backend propio (antes de DEC-043): si aparecen, se borran. */
private val CLAVES_ANTERIORES = listOf("alumnoToken", "adminToken", "docenteToken", "correoOtp", "sesionSiia", "rol")

/** Margen para dar el token por vencido un poco antes que el SIIA. */
private const val MARGEN_EXPIRACION_MS = 60_000L

/**
 * Sesión del alumno: el token del SIIA (480 min) y los datos de su cuenta.
 * Se guarda en SharedPreferences (excluidas del respaldo en la nube, ver
 * res/xml/backup_rules.xml) para que, mientras el token siga vigente, la app
 * abra directo en Inicio en lugar de pedir otra vez contraseña y NIP.
 */
object UserSession {
    private var prefs: android.content.SharedPreferences? = null

    /** Debe llamarse una vez al arrancar la app (ver MainActivity.onCreate). */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
        val p = prefs ?: return
        if (CLAVES_ANTERIORES.any(p::contains)) {
            p.edit().clear().apply()
            return
        }
        usuario = p.getString(CLAVE_USUARIO, "") ?: ""
        matricula = p.getString(CLAVE_MATRICULA, "") ?: ""
        nombreCompleto = p.getString(CLAVE_NOMBRE, "") ?: ""
        correo = p.getString(CLAVE_CORREO, "") ?: ""
        carrera = p.getString(CLAVE_CARRERA, "") ?: ""
        idCarrera = p.getInt(CLAVE_ID_CARRERA, -1).takeIf { it >= 0 }
        token = p.getString(CLAVE_TOKEN, null)
        tokenExpiraEn = p.getLong(CLAVE_EXPIRA, 0L)
    }

    var usuario: String = ""
        private set
    var matricula: String = ""
        private set
    var nombreCompleto: String = ""
        private set
    var correo: String = ""
        private set
    var carrera: String = ""
        private set
    var idCarrera: Int? = null
        private set

    /** Token final del SIIA (después del NIP); va como `Authorization: Bearer`. */
    var token: String? = null
        private set

    /** Momento (epoch ms) en que vence [token]; 0 si el SIIA no dijo cuándo. */
    var tokenExpiraEn: Long = 0L
        private set

    fun establecer(cuenta: CuentaSiia, token: String, expiraEnMinutos: Int, ahora: Long = System.currentTimeMillis()) {
        usuario = cuenta.usuario
        matricula = cuenta.matricula.ifBlank { cuenta.usuario }
        nombreCompleto = cuenta.nombreCompleto
        correo = cuenta.correo
        carrera = cuenta.carrera
        idCarrera = cuenta.idCarrera
        this.token = token
        tokenExpiraEn = if (expiraEnMinutos > 0) ahora + expiraEnMinutos * 60_000L else 0L
        guardar()
    }

    /** true si hay token y no ha vencido. */
    fun sesionVigente(ahora: Long = System.currentTimeMillis()): Boolean =
        !token.isNullOrBlank() && (tokenExpiraEn == 0L || ahora < tokenExpiraEn - MARGEN_EXPIRACION_MS)

    fun limpiar() {
        usuario = ""
        matricula = ""
        nombreCompleto = ""
        correo = ""
        carrera = ""
        idCarrera = null
        token = null
        tokenExpiraEn = 0L
        prefs?.edit()?.clear()?.apply()
    }

    private fun guardar() {
        prefs?.edit()?.apply {
            clear()
            putString(CLAVE_USUARIO, usuario)
            putString(CLAVE_MATRICULA, matricula)
            putString(CLAVE_NOMBRE, nombreCompleto)
            putString(CLAVE_CORREO, correo)
            putString(CLAVE_CARRERA, carrera)
            idCarrera?.let { putInt(CLAVE_ID_CARRERA, it) }
            putString(CLAVE_TOKEN, token)
            putLong(CLAVE_EXPIRA, tokenExpiraEn)
        }?.apply()
    }

    val nombreMostrar: String
        get() = when {
            nombreCompleto.isNotBlank() -> nombreCompleto.uppercase()
            matricula.isNotBlank() -> "ALUMNO $matricula"
            else -> "ALUMNO TESCHI"
        }
}
