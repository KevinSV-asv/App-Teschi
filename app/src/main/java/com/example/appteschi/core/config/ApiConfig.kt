package com.example.appteschi.core.config

import android.content.Context
import com.example.appteschi.BuildConfig
import com.example.appteschi.data.AppContextHolder

private const val PREFS_NOMBRE = "appteschi_config"
private const val CLAVE_URL_PERSONALIZADA = "url_personalizada"

/**
 * Decide qué URL base usar: la que el propio alumno/desarrollador haya
 * guardado a mano (solo builds de depuración — ver [ApiConfig.establecerUrlPersonalizada])
 * si existe, o si no la que trae compilada BuildConfig. Función pura, sin
 * Context ni SharedPreferences de por medio, para poder probarla con JUnit.
 */
fun urlBaseEfectiva(urlPersonalizada: String?, urlPorDefecto: String): String =
    (urlPersonalizada?.trim()?.takeIf { it.isNotBlank() } ?: urlPorDefecto).trimEnd('/')

/**
 * Fuente única de la URL del backend AppTeschi.Api.
 *
 * El valor por defecto vive en local.properties (API_BASE_URL) → BuildConfig.
 * Ningún servicio debe declarar su propia URL. Ya no hay API key compilada en
 * el APK (DEC-030): cada alumno se identifica con su token de sesión.
 *
 * Además, solo en builds de depuración, se puede sobreescribir la URL en
 * caliente desde la app (ver [establecerUrlPersonalizada]) — pensado para no
 * tener que recompilar cada vez que cambia la URL del túnel de Cloudflare de
 * desarrollo. Se guarda en SharedPreferences, sobrevive a reinicios de la app,
 * y nunca se usa en builds de release: ahí solo cuenta BuildConfig.API_BASE_URL.
 *
 * Ya no hay credenciales SMTP aquí (ver DEC-019) — el correo del OTP lo
 * envía el backend, la app solo llama a los endpoints otpEnviar/otpVerificar.
 */
object ApiConfig {

    private val prefs by lazy {
        AppContextHolder.requireContext().getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
    }

    /** URL guardada a mano por el alumno/desarrollador, o null si usa la de BuildConfig. */
    fun urlPersonalizada(): String? =
        if (BuildConfig.DEBUG) prefs.getString(CLAVE_URL_PERSONALIZADA, null) else null

    /**
     * Guarda una URL personalizada y la deja activa de inmediato (sin reiniciar
     * la app). Pasa `null` o una cadena vacía para volver a la de BuildConfig.
     * No hace nada en builds de release.
     */
    fun establecerUrlPersonalizada(url: String?) {
        if (!BuildConfig.DEBUG) return
        prefs.edit().apply {
            if (url.isNullOrBlank()) remove(CLAVE_URL_PERSONALIZADA) else putString(CLAVE_URL_PERSONALIZADA, url.trim())
        }.apply()
    }

    val BASE_URL: String get() = urlBaseEfectiva(urlPersonalizada(), BuildConfig.API_BASE_URL)

    val registro get() = "$BASE_URL/api/registro"
    val authCuenta get() = "$BASE_URL/api/auth/cuenta"
    val authAdministrador get() = "$BASE_URL/api/auth/administrador"
    val otpEnviar get() = "$BASE_URL/api/otp/enviar"
    val otpVerificar get() = "$BASE_URL/api/otp/verificar"
    val usuarios get() = "$BASE_URL/api/usuarios"
    val auditoria get() = "$BASE_URL/api/auditoria"
    val administradores get() = "$BASE_URL/api/administradores"
    val catalogosRegistro get() = "$BASE_URL/api/catalogos/registro"
    val estadisticas get() = "$BASE_URL/api/estadisticas"

    val recuperarPasswordSolicitar get() = "$BASE_URL/api/recuperar-password/solicitar"
    val recuperarPasswordConfirmar get() = "$BASE_URL/api/recuperar-password/confirmar"
    val reinscripcionSolicitud get() = "$BASE_URL/api/reinscripcion/solicitud"
    val reinscripcionSolicitudesPendientes get() = "$BASE_URL/api/reinscripcion/solicitudes?estado=PENDIENTE"
    val observacionCrear get() = "$BASE_URL/api/observaciones"

    fun planEstudios(claveCarrera: String) = "$BASE_URL/api/plan-estudios/$claveCarrera"
    fun grupos(claveCarrera: String) = "$BASE_URL/api/grupos/$claveCarrera"
    fun calificaciones(matricula: String) = "$BASE_URL/api/calificaciones/$matricula"
    fun observaciones(matricula: String) = "$BASE_URL/api/observaciones/$matricula"
    fun observacionResolver(id: Int) = "$BASE_URL/api/observaciones/$id/resolver"
    fun reinscripcionSolicitudConfirmar(id: Int) = "$BASE_URL/api/reinscripcion/solicitudes/$id/confirmar"
    fun calificacion(matricula: String, idMateria: Int) = "$BASE_URL/api/calificaciones/$matricula/$idMateria"
    fun reinscripcionEstatus(matricula: String) = "$BASE_URL/api/reinscripcion/estatus/$matricula"
    fun miHistorial(matricula: String) = "$BASE_URL/api/mi-historial/$matricula"
    fun miPerfil(matricula: String) = "$BASE_URL/api/mi-perfil/$matricula"
    val cambiarPasswordPropia get() = "$BASE_URL/api/mi-perfil/cambiar-password"

    val horarioImportar get() = "$BASE_URL/api/horarios/importar"
    val horarios get() = "$BASE_URL/api/horarios"
    fun horarioEliminar(id: Int) = "$BASE_URL/api/horarios/$id"
    fun miHorario(matricula: String) = "$BASE_URL/api/mi-horario/$matricula"
}
