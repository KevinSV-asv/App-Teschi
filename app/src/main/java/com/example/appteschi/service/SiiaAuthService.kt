package com.example.appteschi.service

import android.util.Log
import com.example.appteschi.network.InMemoryCookieJar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

/**
 * Valida credenciales contra el portal SIIA (ASP.NET Web Forms).
 *
 * Campos confirmados en Login.aspx:
 *   txtUsuario, txtPass, btnAceptar = "Iniciar Sesión"
 * Error de credenciales: script alert('los datos son errores')
 */
/** Permite sustituir SiiaAuthService por un fake en tests. */
interface SiiaAuth {
    suspend fun validarCredenciales(usuario: String, password: String): Result<Unit>
}

object SiiaAuthService : SiiaAuth {

    private const val TAG = "SiiaAuthService"
    private const val BASE_URL = "http://148.230.236.166/Teschi/"
    private const val LOGIN_PAGE = "${BASE_URL}Login.aspx"
    private const val LOGIN_POST =
        "${BASE_URL}default.aspx?ReturnUrl=%2fTeschi%2fLogin.aspx"
    private const val ERROR_MARKER = "los datos son errores"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(InMemoryCookieJar())
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * @param usuario  Matrícula o usuario del SIIA (campo txtUsuario)
     * @param password Contraseña del SIIA (campo txtPass)
     */
    override suspend fun validarCredenciales(
        usuario: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val usuarioLimpio = usuario.trim()
        if (usuarioLimpio.isBlank() || password.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Usuario y contraseña son obligatorios")
            )
        }

        try {
            Log.d(TAG, "GET $LOGIN_PAGE")
            val getRequest = Request.Builder().url(LOGIN_PAGE).get().build()
            client.newCall(getRequest).execute().use { getResponse ->
                if (!getResponse.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("SIIA no disponible (HTTP ${getResponse.code})")
                    )
                }
                val loginHtml = getResponse.body?.string().orEmpty()
                val doc = Jsoup.parse(loginHtml, LOGIN_PAGE)

                val viewState = doc.select("input[name=__VIEWSTATE]").attr("value")
                val eventValidation = doc.select("input[name=__EVENTVALIDATION]").attr("value")
                val viewStateGenerator = doc.select("input[name=__VIEWSTATEGENERATOR]").attr("value")

                if (viewState.isBlank() || eventValidation.isBlank()) {
                    return@withContext Result.failure(
                        Exception("No se pudieron leer los tokens del formulario SIIA")
                    )
                }

                val formBody = FormBody.Builder()
                    .add("__VIEWSTATE", viewState)
                    .add("__EVENTVALIDATION", eventValidation)
                    .add("__VIEWSTATEGENERATOR", viewStateGenerator)
                    .add("__EVENTTARGET", "")
                    .add("__EVENTARGUMENT", "")
                    .add("txtUsuario", usuarioLimpio)
                    .add("txtPass", password)
                    .add("btnAceptar", "Iniciar Sesión")
                    .build()

                Log.d(TAG, "POST login para usuario: $usuarioLimpio")
                val postRequest = Request.Builder()
                    .url(LOGIN_POST)
                    .post(formBody)
                    .build()

                client.newCall(postRequest).execute().use { postResponse ->
                    val body = postResponse.body?.string().orEmpty()

                    if (body.contains(ERROR_MARKER, ignoreCase = true)) {
                        Log.w(TAG, "Credenciales rechazadas por el SIIA")
                        return@withContext Result.failure(
                            Exception("Usuario o contraseña incorrectos")
                        )
                    }

                    val sigueEnLogin = body.contains("id=\"txtUsuario\"") &&
                        body.contains("id=\"btnAceptar\"")

                    if (sigueEnLogin) {
                        Log.w(TAG, "Respuesta sigue mostrando formulario de login")
                        return@withContext Result.failure(
                            Exception("Usuario o contraseña incorrectos")
                        )
                    }

                    Log.d(TAG, "Credenciales validadas correctamente")
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al conectar con el SIIA", e)
            Result.failure(
                Exception("No se pudo conectar al SIIA. Verifica tu conexión a internet.")
            )
        }
    }
}
