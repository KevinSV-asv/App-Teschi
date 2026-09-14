package com.example.appteschi.core.config

import com.example.appteschi.BuildConfig

/**
 * Fuente única de la URL y la API key del backend AppTeschi.Api.
 *
 * Los valores reales viven en local.properties (API_BASE_URL, API_KEY) →
 * BuildConfig. Ningún servicio debe declarar su propia URL/key.
 *
 * Ya no hay credenciales SMTP aquí (ver DEC-019) — el correo del OTP lo
 * envía el backend, la app solo llama a los endpoints otpEnviar/otpVerificar.
 */
object ApiConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL.trimEnd('/')
    val API_KEY: String = BuildConfig.API_KEY

    val registro = "$BASE_URL/api/registro"
    val authCuenta = "$BASE_URL/api/auth/cuenta"
    val authAdministrador = "$BASE_URL/api/auth/administrador"
    val otpEnviar = "$BASE_URL/api/otp/enviar"
    val otpVerificar = "$BASE_URL/api/otp/verificar"
    val usuarios = "$BASE_URL/api/usuarios"
    val usuariosSync = "$BASE_URL/api/usuarios/sync"
    val auditoria = "$BASE_URL/api/auditoria"
    val administradores = "$BASE_URL/api/administradores"
    val catalogosRegistro = "$BASE_URL/api/catalogos/registro"
    val estadisticas = "$BASE_URL/api/estadisticas"

    fun planEstudios(claveCarrera: String) = "$BASE_URL/api/plan-estudios/$claveCarrera"
    fun grupos(claveCarrera: String) = "$BASE_URL/api/grupos/$claveCarrera"
    fun calificaciones(matricula: String) = "$BASE_URL/api/calificaciones/$matricula"
    fun calificacion(matricula: String, idMateria: Int) = "$BASE_URL/api/calificaciones/$matricula/$idMateria"
}
