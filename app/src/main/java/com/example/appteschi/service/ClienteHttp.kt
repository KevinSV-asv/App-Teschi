package com.example.appteschi.service

import com.example.appteschi.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP común de la app (todas las llamadas van al SIIA). Cuando no hay
 * red o el SIIA no responde, en vez del texto crudo de Java ("failed to connect
 * to siia.teschi.edu.mx/…") el alumno ve un mensaje que le dice qué revisar.
 */
object ClienteHttp {
    fun nuevo(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(EncabezadosSiia)
        .addInterceptor(TraduceErroresDeRed)
        .build()
}

/** Se identifica ante el SIIA y pide JSON (BUG-024: sin User-Agent el SIIA respondía distinto). */
object EncabezadosSiia : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", "AppTESCHI-Android/${BuildConfig.VERSION_NAME}")
                .build()
        )
}

/** Convierte cualquier [IOException] de red en otra con [mensajeDeErrorDeRed]; conserva la causa original. */
object TraduceErroresDeRed : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        try {
            chain.proceed(chain.request())
        } catch (error: IOException) {
            throw IOException(mensajeDeErrorDeRed(error), error)
        }
}

internal fun mensajeDeErrorDeRed(error: IOException): String = when (error) {
    is SocketTimeoutException, is ConnectException, is NoRouteToHostException ->
        "No se pudo conectar con el SIIA. Revisa tu conexión a internet e inténtalo de nuevo."
    is UnknownHostException ->
        "No se encontró el SIIA. Revisa tu conexión a internet e inténtalo de nuevo."
    else ->
        "Se perdió la conexión con el SIIA. Inténtalo de nuevo."
}

/** El SIIA a veces responde HTML (página de error de IIS) en vez de JSON: eso queda como null. */
internal fun jsonONulo(texto: String?): JSONObject? =
    texto?.trim()?.takeIf { it.startsWith("{") }?.let { runCatching { JSONObject(it) }.getOrNull() }

/** `mensaje` del SIIA si viene y no está vacío. */
internal fun JSONObject?.mensajeSiia(): String? =
    this?.optString("mensaje")?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

internal const val MENSAJE_RESPUESTA_INESPERADA = "El SIIA respondió de forma inesperada. Intenta más tarde."
