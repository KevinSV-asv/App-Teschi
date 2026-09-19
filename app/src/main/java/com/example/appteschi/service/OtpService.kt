package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

enum class TipoCuenta { ALUMNO, ADMINISTRADOR }

/** Permite sustituir OtpService por un fake en tests. */
interface RemoteOtpService {
    /** [ticket] es el que devolvió el login (auth/cuenta o auth/administrador) al validar la contraseña (DEC-030). */
    suspend fun enviar(tipo: TipoCuenta, identificador: String, correo: String, ticket: String): Result<Unit>

    /**
     * Éxito trae el token de sesión (JWT) de esa cuenta — de administrador
     * (DEC-020) o de alumno (DEC-030). Solo es null si el backend no encontró
     * la cuenta al emitirlo.
     */
    suspend fun verificar(tipo: TipoCuenta, identificador: String, codigo: String, ticket: String): Result<String?>
}

/**
 * OTP real de dos pasos, generado, guardado (hasheado) y verificado
 * enteramente en el backend (ver DEC-019 y `POST /api/otp/enviar` /
 * `POST /api/otp/verificar`). Antes el código se generaba y comparaba
 * dentro de la propia app — cualquiera que controlara el cliente (un APK
 * modificado, por ejemplo) podía saltarse la verificación por completo.
 * Ahora la app solo pregunta "¿es válido este código?" y el servidor
 * responde; nunca conoce el código correcto.
 */
object OtpService : RemoteOtpService {
    private val client = OkHttpClient()

    override suspend fun enviar(tipo: TipoCuenta, identificador: String, correo: String, ticket: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("tipo", tipo.name)
                    put(if (tipo == TipoCuenta.ADMINISTRADOR) "usuario" else "matricula", identificador)
                    put("correo", correo)
                    put("ticket", ticket)
                }
                enviarPost(ApiConfig.otpEnviar, body)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    override suspend fun verificar(tipo: TipoCuenta, identificador: String, codigo: String, ticket: String): Result<String?> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("tipo", tipo.name)
                    put(if (tipo == TipoCuenta.ADMINISTRADOR) "usuario" else "matricula", identificador)
                    put("codigo", codigo)
                    put("ticket", ticket)
                }
                val request = Request.Builder()
                    .url(ApiConfig.otpVerificar)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        Result.failure(IllegalStateException(json.optString("error", "No se pudo completar la operación")))
                    } else {
                        Result.success(if (json.isNull("token")) null else json.optString("token"))
                    }
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    private fun enviarPost(url: String, body: JSONObject): Result<Unit> {
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val json = JSONObject(response.body?.string().orEmpty())
            return if (!response.isSuccessful || !json.optBoolean("ok")) {
                Result.failure(IllegalStateException(json.optString("error", "No se pudo completar la operación")))
            } else {
                Result.success(Unit)
            }
        }
    }
}
