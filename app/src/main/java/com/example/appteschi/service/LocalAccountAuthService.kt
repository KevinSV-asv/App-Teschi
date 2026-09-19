package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private val API_URL get() = ApiConfig.authCuenta

/** Permite sustituir LocalAccountAuthService por un fake en tests. */
interface CuentaAuthService {
    suspend fun autenticar(matricula: String, password: String): Result<LocalAccountAuthService.Account>
}

/** Cuentas de alumno (`/api/auth/cuenta`) — las de administrador viven en
 *  AdminAccountAuthService, contra una tabla separada (ver DEC-019).
 *  Al validar la contraseña el backend entrega un ticket de 10 minutos
 *  ([Account.ticket]) sin el cual no deja pedir ni verificar el OTP (DEC-030). */
object LocalAccountAuthService : CuentaAuthService {
    data class Account(val matricula: String, val nombre: String, val ticket: String)

    private val client = OkHttpClient()

    override suspend fun autenticar(matricula: String, password: String): Result<Account> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("matricula", matricula.trim())
                put("password", password)
            }
            val request = Request.Builder()
                .url(API_URL)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    Result.failure(IllegalStateException(json.optString("error", "Cuenta no encontrada")))
                } else {
                    val ticket = json.optString("ticket")
                    if (ticket.isBlank()) {
                        Result.failure(IllegalStateException("El servidor no devolvió el ticket de inicio de sesión. ¿Está actualizado?"))
                    } else {
                        Result.success(Account(json.getString("matricula"), json.optString("nombre"), ticket))
                    }
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
