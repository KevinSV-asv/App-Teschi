package com.example.appteschi.service

import com.example.appteschi.data.UserSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Agrega el token de sesión del alumno (DEC-030) como
 * `Authorization: Bearer <token>`. Reemplaza a la x-api-key estática, que iba
 * compilada dentro del APK y no identificaba a nadie: con ella cualquiera que
 * conociera una matrícula podía leer o modificar los datos de ese alumno.
 * Si no hay token (usuarios de prueba sem1..sem9) no se manda el header.
 */
fun Request.Builder.conSesionAlumno(): Request.Builder {
    val token = UserSession.alumnoToken
    return if (token.isNullOrBlank()) this else addHeader("Authorization", "Bearer $token")
}

/**
 * Cliente HTTP compartido por los servicios del alumno. Detecta cuando el
 * backend dice que la sesión venció o no es válida (respuesta 401 con el
 * header `X-Sesion-Invalida`, que solo manda `requireAlumno()` — otros 401,
 * como "contraseña actual incorrecta", no lo llevan), borra la sesión local y
 * avisa por [expirada] para que la app mande al alumno a iniciar sesión.
 * Sin esto, con el token vencido cada pantalla quedaba en "error + Reintentar"
 * sin forma de salir.
 */
object SesionAlumno {
    private val _expirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expirada: SharedFlow<Unit> = _expirada.asSharedFlow()

    private val vigilante = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401 &&
            response.header("X-Sesion-Invalida") == "1" &&
            request.header("Authorization") != null &&
            UserSession.alumnoToken != null
        ) {
            UserSession.limpiar()
            _expirada.tryEmit(Unit)
        }
        response
    }

    val client: OkHttpClient by lazy { OkHttpClient.Builder().addInterceptor(vigilante).build() }
}
