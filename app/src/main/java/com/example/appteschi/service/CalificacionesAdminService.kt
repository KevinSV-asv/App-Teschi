package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class AlumnoCalificaciones(
    val matricula: String,
    val nombreCompleto: String,
    val semestre: Int?,
    val carrera: String,
    val claveCarrera: String
)

data class MateriaCalificacion(
    val idMateria: Int,
    val nombre: String,
    val creditos: Int,
    val semestre: Int,
    val calificacion: Double?,
    val estatusCodigo: String?,
    val estatusNombre: String?
)

/**
 * Calificaciones de administrador: trae/edita HistorialAcademico de
 * CUALQUIER alumno (el módulo del alumno en CalificacionesScreen.kt es un
 * mock aparte, sin backend todavía — ver DEC-017). Requiere sesión de
 * administrador (ver DEC-020).
 */
object CalificacionesAdminService {
    private val client = OkHttpClient()

    suspend fun obtener(matricula: String): Result<Pair<AlumnoCalificaciones, List<MateriaCalificacion>>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(ApiConfig.calificaciones(matricula))
                    .conSesionAdmin()
                    .build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        return@withContext Result.failure(IllegalStateException(json.optString("error", "No se pudieron consultar las calificaciones")))
                    }
                    val alumnoJson = json.getJSONObject("alumno")
                    val alumno = AlumnoCalificaciones(
                        matricula = alumnoJson.optString("matricula"),
                        nombreCompleto = alumnoJson.optString("nombreCompleto"),
                        semestre = if (alumnoJson.isNull("semestre")) null else alumnoJson.optInt("semestre"),
                        carrera = alumnoJson.optString("carrera"),
                        claveCarrera = alumnoJson.optString("claveCarrera")
                    )
                    val arr = json.getJSONArray("materias")
                    val materias = List(arr.length()) { i ->
                        val item = arr.getJSONObject(i)
                        MateriaCalificacion(
                            idMateria = item.optInt("IdMateria"),
                            nombre = item.optString("Nombre"),
                            creditos = item.optInt("Creditos"),
                            semestre = item.optInt("Semestre"),
                            calificacion = if (item.isNull("Calificacion")) null else item.optDouble("Calificacion"),
                            estatusCodigo = if (item.isNull("EstatusCodigo")) null else item.optString("EstatusCodigo"),
                            estatusNombre = if (item.isNull("EstatusNombre")) null else item.optString("EstatusNombre")
                        )
                    }
                    Result.success(alumno to materias)
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun actualizar(matricula: String, idMateria: Int, calificacion: Double?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject()
                if (calificacion != null) json.put("calificacion", calificacion) else json.put("calificacion", JSONObject.NULL)
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(ApiConfig.calificacion(matricula, idMateria))
                    .conSesionAdmin()
                    .put(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    val respJson = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !respJson.optBoolean("ok")) {
                        Result.failure(IllegalStateException(respJson.optString("error", "No se pudo guardar la calificación")))
                    } else Result.success(Unit)
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
}
