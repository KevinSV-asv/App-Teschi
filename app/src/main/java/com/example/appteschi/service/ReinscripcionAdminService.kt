package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import com.example.appteschi.data.ObservacionReglamento
import com.example.appteschi.data.SolicitudReinscripcionAdmin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Permite sustituir ReinscripcionAdminService por un fake en tests. */
interface ReinscripcionAdminApi {
    suspend fun observaciones(matricula: String): Result<List<ObservacionReglamento>>
    suspend fun crearObservacion(matricula: String, motivo: String): Result<Unit>
    suspend fun resolverObservacion(id: Int, autorizar: Boolean, resolucion: String?): Result<Unit>
    suspend fun solicitudesPendientes(): Result<List<SolicitudReinscripcionAdmin>>
    suspend fun confirmarSolicitud(id: Int): Result<Unit>
}

/**
 * Lado administrador de Reinscripción Fase 2: observaciones de reglamento
 * (bloquean/desbloquean la reinscripción de un alumno, ver
 * ReinscripcionService/02_MODULOS/01_Reinscripcion.md) y las solicitudes que
 * control escolar debe confirmar de manera presencial. Todos los endpoints
 * exigen sesión de administrador (ver DEC-020).
 */
object ReinscripcionAdminService : ReinscripcionAdminApi {
    private val client = OkHttpClient()

    override suspend fun observaciones(matricula: String): Result<List<ObservacionReglamento>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(ApiConfig.observaciones(matricula))
                    .conSesionAdmin()
                    .build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("error", "No se pudieron consultar las observaciones"))
                        )
                    }
                    val arr = json.getJSONArray("data")
                    val lista = List(arr.length()) { i ->
                        val o = arr.getJSONObject(i)
                        ObservacionReglamento(
                            id = o.optInt("IdObservacion"),
                            motivo = o.optString("Motivo"),
                            estado = o.optString("Estado"),
                            registradaPor = o.optString("RegistradaPor"),
                            fechaRegistro = o.optString("FechaRegistro"),
                            resueltaPor = if (o.isNull("ResueltaPor")) null else o.optString("ResueltaPor"),
                            fechaResolucion = if (o.isNull("FechaResolucion")) null else o.optString("FechaResolucion"),
                            resolucion = if (o.isNull("Resolucion")) null else o.optString("Resolucion")
                        )
                    }
                    Result.success(lista)
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    override suspend fun crearObservacion(matricula: String, motivo: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().put("matricula", matricula).put("motivo", motivo)
                    .toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(ApiConfig.observacionCrear)
                    .conSesionAdmin()
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("error", "No se pudo registrar la observación"))
                        )
                    }
                    Result.success(Unit)
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    override suspend fun resolverObservacion(id: Int, autorizar: Boolean, resolucion: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("decision", if (autorizar) "AUTORIZADA" else "RECHAZADA")
                    .apply { if (!resolucion.isNullOrBlank()) put("resolucion", resolucion) }
                    .toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(ApiConfig.observacionResolver(id))
                    .conSesionAdmin()
                    .put(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("error", "No se pudo resolver la observación"))
                        )
                    }
                    Result.success(Unit)
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    override suspend fun solicitudesPendientes(): Result<List<SolicitudReinscripcionAdmin>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(ApiConfig.reinscripcionSolicitudesPendientes)
                    .conSesionAdmin()
                    .build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("error", "No se pudieron consultar las solicitudes"))
                        )
                    }
                    val arr = json.getJSONArray("data")
                    val lista = List(arr.length()) { i ->
                        val s = arr.getJSONObject(i)
                        SolicitudReinscripcionAdmin(
                            id = s.optInt("IdSolicitud"),
                            folio = s.optString("Folio"),
                            estatus = s.optString("Estatus"),
                            fechaSolicitud = s.optString("FechaSolicitud"),
                            matricula = s.optString("Matricula"),
                            nombreCompleto = s.optString("NombreCompleto"),
                            claveGrupo = s.optString("ClaveGrupo"),
                            periodo = s.optString("Periodo")
                        )
                    }
                    Result.success(lista)
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    override suspend fun confirmarSolicitud(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ApiConfig.reinscripcionSolicitudConfirmar(id))
                .conSesionAdmin()
                .put("".toRequestBody())
                .build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("error", "No se pudo confirmar la solicitud"))
                    )
                }
                Result.success(Unit)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
