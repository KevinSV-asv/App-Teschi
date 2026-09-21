package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Permite sustituir AdminAccountAuthService por un fake en tests. */
interface AdminAuthService {
    suspend fun autenticar(usuario: String, password: String): Result<AdminAccountAuthService.Account>
}

/**
 * Autenticación de administradores reales contra `Administradores` /
 * `AdministradorCredenciales` (ver DEC-019) — antes el único acceso de
 * administrador era un bypass local sin contraseña real ni OTP.
 */
object AdminAccountAuthService : AdminAuthService {
    data class Account(val usuario: String, val nombre: String, val rol: String, val ticket: String)

    private val client = ClienteHttp.nuevo()

    override suspend fun autenticar(usuario: String, password: String): Result<Account> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("usuario", usuario.trim())
                put("password", password)
            }
            val request = Request.Builder()
                .url(ApiConfig.authAdministrador)
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
                        Result.success(
                            Account(
                                usuario = json.getString("usuario"),
                                nombre = json.optString("nombre"),
                                rol = json.optString("rol", "OPERADOR"),
                                ticket = ticket
                            )
                        )
                    }
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
