package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Permite sustituir RecuperarPasswordService por un fake en tests. */
interface RecuperarPasswordApi {
    /** Éxito trae el correo real enmascarado (ej. "k***n@gmail.com"), nunca
     *  el correo completo — la app no debe mostrar la dirección real. */
    suspend fun solicitar(matricula: String): Result<String>
    suspend fun confirmar(matricula: String, codigo: String, nuevaPassword: String): Result<Unit>
}

/**
 * Recuperación de contraseña — solo para cuentas propias de AppTESCHI
 * (alumnos que se registraron con matrícula/contraseña dentro de la app).
 * El correo de destino lo decide siempre el backend (CorreoOtp ya guardado
 * para esa matrícula) — la app nunca puede elegir a dónde se manda el
 * código, ver DEC-025.
 */
object RecuperarPasswordService : RecuperarPasswordApi {
    private val client = ClienteHttp.nuevo()

    override suspend fun solicitar(matricula: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("matricula", matricula)
                .toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(ApiConfig.recuperarPasswordSolicitar).post(body).build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("error", "No se pudo enviar el código de recuperación"))
                    )
                }
                Result.success(json.optString("correo"))
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun confirmar(matricula: String, codigo: String, nuevaPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("matricula", matricula)
                    .put("codigo", codigo)
                    .put("nuevaPassword", nuevaPassword)
                    .toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(ApiConfig.recuperarPasswordConfirmar).post(body).build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("error", "No se pudo restablecer tu contraseña"))
                        )
                    }
                    Result.success(Unit)
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
}
