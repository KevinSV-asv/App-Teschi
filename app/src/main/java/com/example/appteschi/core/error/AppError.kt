package com.example.appteschi.core.error

import org.json.JSONException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Modelo de error común para toda llamada al backend AppTeschi.Api.
 *
 * Ninguna pantalla debe mostrar el mensaje de una excepción técnica
 * (SocketTimeoutException, JSONException, etc.) directamente al usuario.
 * Las capas de datos deben traducir sus fallos a uno de estos casos antes
 * de que lleguen al ViewModel/UI.
 *
 * Se introduce en esta fase como infraestructura; su uso en los servicios
 * y ViewModels existentes se conecta en la Fase B (Repository), para no
 * mezclar la centralización de configuración con el refactor de lógica.
 */
sealed class AppError(val mensajeUsuario: String, val detalleTecnico: String? = null) {
    class Red(detalle: String? = null) :
        AppError("No se pudo conectar con el servidor. Verifica tu conexión e inténtalo nuevamente.", detalle)

    class Timeout(detalle: String? = null) :
        AppError("El servidor tardó demasiado en responder. Inténtalo nuevamente.", detalle)

    class NoAutorizado(detalle: String? = null) :
        AppError("No tienes autorización para realizar esta acción.", detalle)

    class Prohibido(detalle: String? = null) :
        AppError("No tienes permiso para realizar esta acción.", detalle)

    class Servidor(detalle: String? = null) :
        AppError("Ocurrió un error en el servidor. Inténtalo más tarde.", detalle)

    class Validacion(mensaje: String) : AppError(mensaje)

    class RespuestaInvalida(detalle: String? = null) :
        AppError("El servidor respondió de forma inesperada.", detalle)

    class Desconocido(detalle: String? = null) :
        AppError("Ocurrió un error inesperado. Inténtalo nuevamente.", detalle)
}

/** Traduce una excepción técnica de red/parsing al modelo de error común. */
fun Throwable.toAppError(): AppError = when (this) {
    is SocketTimeoutException -> AppError.Timeout(message)
    is ConnectException, is UnknownHostException -> AppError.Red(message)
    is JSONException -> AppError.RespuestaInvalida(message)
    else -> AppError.Desconocido(message)
}

/** Traduce un código de estado HTTP al modelo de error común. */
fun httpCodeToAppError(code: Int, detalle: String? = null): AppError = when (code) {
    401 -> AppError.NoAutorizado(detalle)
    403 -> AppError.Prohibido(detalle)
    in 500..599 -> AppError.Servidor(detalle)
    else -> AppError.Desconocido(detalle)
}
