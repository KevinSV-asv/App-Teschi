package com.example.appteschi.service

import com.example.appteschi.data.UserSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/** Mensaje cuando el token del SIIA ya no sirve (venció a las 8 h o se cerró la sesión en el SIIA). */
internal const val MENSAJE_SESION_EXPIRADA = "Tu sesión del SIIA expiró. Inicia sesión de nuevo."

/**
 * Cliente HTTP para las consultas con el token del SIIA. Si el SIIA responde
 * 401 a una petición que llevaba el token, la sesión ya no vale: se borra y se
 * avisa por [expirada] para que la app mande al alumno a iniciar sesión, en vez
 * de dejar cada pantalla en "error + Reintentar" sin salida.
 *
 * El cambio de NIP no usa este cliente: ahí un 401 puede significar
 * "NIP actual incorrecto", no sesión vencida.
 */
object SesionAlumno {
    private val _expirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expirada: SharedFlow<Unit> = _expirada.asSharedFlow()

    /** Borra la sesión local y avisa a la navegación. */
    fun terminar() {
        UserSession.limpiar()
        _expirada.tryEmit(Unit)
    }

    private val vigilante = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401 && request.header("Authorization") != null && UserSession.token != null) {
            terminar()
        }
        response
    }

    val client: OkHttpClient by lazy { ClienteHttp.nuevo().newBuilder().addInterceptor(vigilante).build() }
}
