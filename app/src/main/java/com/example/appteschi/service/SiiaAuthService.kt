package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Resultado de POST /api/auth/siia */
sealed class SiiaLoginResultado {
    /** Alumno o docente validado — siguiente paso: enviar OTP */
    data class RequiereOtp(
        val tipo: TipoCuenta,
        val identificador: String,   // matrícula (alumno) o usuario (docente)
        val nombre: String,
        val ticket: String,
        val correoEnmascarado: String,
        val cambioPw: Boolean
    ) : SiiaLoginResultado()

    /** Alumno cuya carrera no tiene equivalencia mapeada aún */
    data class CuentaPendiente(val nombre: String) : SiiaLoginResultado()

    /** Alumno o docente sin correo registrado — deben registrar uno antes */
    data class RequiereCorreo(val tipo: TipoCuenta, val identificador: String) : SiiaLoginResultado()

    /** Credenciales incorrectas u otro error */
    data class Error(val mensaje: String) : SiiaLoginResultado()
}

interface SiiaAuthService {
    suspend fun login(usuario: String, password: String): SiiaLoginResultado
}

object SiiaAccountAuthService : SiiaAuthService {
    private val client = ClienteHttp.nuevo()

    override suspend fun login(usuario: String, password: String): SiiaLoginResultado =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("usuario", usuario.trim())
                    put("password", password)
                    put("tipo", "alumno")   // el backend lo re-clasifica según el SIIA
                }
                val request = Request.Builder()
                    .url(ApiConfig.authSiia)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())

                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        return@withContext SiiaLoginResultado.Error(
                            json.optString("error", "Credenciales incorrectas")
                        )
                    }

                    // ¿cuenta sin carrera mapeada?
                    if (json.optBoolean("cuentaPendiente")) {
                        return@withContext SiiaLoginResultado.CuentaPendiente(
                            nombre = json.optString("nombre", "")
                        )
                    }

                    // ¿sin correo registrado?
                    if (json.optBoolean("requiereCorreo")) {
                        val tipoStr = json.optString("tipo", "Alumno")
                        val tipo = if (tipoStr.equals("Docente", ignoreCase = true))
                            TipoCuenta.DOCENTE else TipoCuenta.ALUMNO
                        return@withContext SiiaLoginResultado.RequiereCorreo(
                            tipo = tipo,
                            identificador = json.optString("identificador")
                                .ifBlank { json.optString("matricula") }
                                .ifBlank { json.optString("usuario") }
                        )
                    }

                    val ticket = json.optString("ticket")
                    if (ticket.isBlank()) {
                        return@withContext SiiaLoginResultado.Error("El servidor no devolvió el ticket.")
                    }

                    val tipoStr = json.optString("tipo", "Alumno")
                    val tipo = if (tipoStr.equals("Docente", ignoreCase = true))
                        TipoCuenta.DOCENTE else TipoCuenta.ALUMNO
                    val identificador = json.optString("identificador")
                        .ifBlank { json.optString("matricula") }
                        .ifBlank { json.optString("usuario") }

                    SiiaLoginResultado.RequiereOtp(
                        tipo = tipo,
                        identificador = identificador,
                        nombre = json.optString("nombre"),
                        ticket = ticket,
                        correoEnmascarado = json.optString("correoEnmascarado"),
                        cambioPw = json.optBoolean("cambioPw")
                    )
                }
            } catch (e: Exception) {
                SiiaLoginResultado.Error(e.message ?: "Error de red")
            }
        }
}
