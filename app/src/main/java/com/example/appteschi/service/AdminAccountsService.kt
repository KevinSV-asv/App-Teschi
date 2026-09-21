package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class AdministradorCuenta(
    val usuario: String,
    val nombreCompleto: String,
    val correoInstitucional: String,
    val rol: String,
    val activo: Boolean,
    val ultimoAcceso: String?
)

/**
 * Gestión de cuentas de administrador — exclusiva de SUPERADMIN (ver
 * DEC-020). Sin esto no había forma de crear un OPERADOR desde la app, solo
 * con el script de línea de comandos.
 */
object AdminAccountsService {
    private val client = ClienteHttp.nuevo()

    suspend fun listar(): Result<List<AdministradorCuenta>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(ApiConfig.administradores).conSesionAdmin().build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(IllegalStateException(json.optString("error", "No se pudieron consultar los administradores")))
                }
                val arr = json.getJSONArray("data")
                val cuentas = List(arr.length()) { i ->
                    val item = arr.getJSONObject(i)
                    AdministradorCuenta(
                        usuario = item.optString("Usuario"),
                        nombreCompleto = item.optString("NombreCompleto"),
                        correoInstitucional = item.optString("CorreoInstitucional"),
                        rol = item.optString("Rol"),
                        activo = item.optBoolean("Activo", true),
                        ultimoAcceso = if (item.isNull("UltimoAcceso")) null else item.optString("UltimoAcceso")
                    )
                }
                Result.success(cuentas)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Devuelve la contraseña generada — solo se entrega en esta respuesta, nunca más. */
    suspend fun crear(usuario: String, nombreCompleto: String, rol: String, correoInstitucional: String = ""): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("usuario", usuario)
                    .put("nombreCompleto", nombreCompleto)
                    .put("rol", rol)
                    .put("correoInstitucional", correoInstitucional)
                    .toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(ApiConfig.administradores).conSesionAdmin().post(body).build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        Result.failure(IllegalStateException(json.optString("error", "No se pudo crear el administrador")))
                    } else {
                        Result.success(json.optString("password"))
                    }
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun actualizar(usuario: String, cambios: Map<String, Any?>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            cambios.forEach { (key, value) -> json.put(key, value ?: JSONObject.NULL) }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("${ApiConfig.administradores}/$usuario").conSesionAdmin().put(body).build()
            client.newCall(request).execute().use { response ->
                val respJson = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !respJson.optBoolean("ok")) {
                    Result.failure(IllegalStateException(respJson.optString("error", "No se pudo actualizar el administrador")))
                } else Result.success(Unit)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
