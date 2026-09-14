package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

private val AUDIT_API_URL = ApiConfig.auditoria

data class RemoteAuditEvent(
    val actorMatricula: String,
    val actorNombre: String,
    val perfilMatricula: String,
    val accion: String,
    val detalle: String,
    val fecha: String
)

object AdminAuditService {
    private val client = OkHttpClient()

    suspend fun fetch(perfilMatricula: String): Result<List<RemoteAuditEvent>> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(perfilMatricula, "UTF-8")
            val url = "$AUDIT_API_URL?matricula=$encoded"
            val request = Request.Builder().url(url).conSesionAdmin().build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(IllegalStateException("No se pudo consultar auditoría"))
                }
                val data = json.getJSONArray("data")
                val events = List(data.length()) { index ->
                    val item = data.getJSONObject(index)
                    RemoteAuditEvent(
                        actorMatricula = item.optString("ActorMatricula"),
                        actorNombre = item.optString("ActorNombre"),
                        perfilMatricula = item.optString("PerfilMatricula"),
                        accion = item.optString("Accion"),
                        detalle = item.optString("Detalle"),
                        fecha = item.optString("FechaMovimiento")
                    )
                }
                Result.success(events)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
