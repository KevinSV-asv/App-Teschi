package com.example.appteschi.service

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Cliente HTTP común de la app: cuando no hay red o el servidor no responde, en vez del
 * texto crudo de Java ("failed to connect to /100.x.x.x (port 4000) from /10.x.x.x …")
 * el alumno ve un mensaje que le dice qué revisar.
 */
object ClienteHttp {
    fun nuevo(): OkHttpClient = OkHttpClient.Builder().addInterceptor(TraduceErroresDeRed).build()
}

/** Convierte cualquier [IOException] de red en otra con [mensajeDeErrorDeRed]; conserva la causa original. */
object TraduceErroresDeRed : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        try {
            chain.proceed(chain.request())
        } catch (error: IOException) {
            throw IOException(mensajeDeErrorDeRed(error, chain.request().url.host), error)
        }
}

/**
 * Los servidores en la red privada de Tailscale usan direcciones 100.64.0.0/10 (100.64–100.127):
 * si el fallo es contra una de ellas, la causa más probable es que la VPN del teléfono esté apagada.
 */
internal fun esDireccionTailscale(host: String): Boolean {
    val partes = host.split('.')
    if (partes.size != 4 || partes[0] != "100") return false
    val segundo = partes[1].toIntOrNull() ?: return false
    return segundo in 64..127
}

internal fun mensajeDeErrorDeRed(error: IOException, host: String): String {
    val pistaVpn = if (esDireccionTailscale(host)) " y que Tailscale esté activo" else ""
    return when (error) {
        is SocketTimeoutException, is ConnectException, is NoRouteToHostException ->
            "No se pudo conectar con el servidor. Revisa tu conexión a internet$pistaVpn e inténtalo de nuevo."
        is UnknownHostException ->
            "No se encontró el servidor. Revisa tu conexión a internet e inténtalo de nuevo."
        else ->
            "Se perdió la conexión con el servidor. Inténtalo de nuevo."
    }
}
