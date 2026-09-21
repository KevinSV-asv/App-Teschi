package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import com.example.appteschi.data.CarreraCatalogo
import com.example.appteschi.data.GrupoInfo
import com.example.appteschi.data.MateriaPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/** Permite sustituir PlanEstudiosService por un fake en tests. */
interface PlanEstudiosApi {
    suspend fun carreras(): Result<List<CarreraCatalogo>>
    suspend fun materias(claveCarrera: String): Result<List<MateriaPlan>>
    suspend fun grupos(claveCarrera: String): Result<List<GrupoInfo>>
}

/**
 * Consume el catálogo académico real desde AppTeschi.Api — mismas 7 carreras
 * y planes de estudio sembrados en `PlanEstudioMaterias`/`Grupos` (ver
 * database/sql/04..15). Endpoints públicos, sin x-api-key: es catálogo
 * curricular, no dato personal de un alumno.
 */
object PlanEstudiosService : PlanEstudiosApi {
    private val client = ClienteHttp.nuevo()

    override suspend fun carreras(): Result<List<CarreraCatalogo>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(ApiConfig.catalogosRegistro).build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(IllegalStateException(json.optString("error", "No se pudieron consultar las carreras")))
                }
                val arr = json.getJSONArray("carreras")
                val carreras = List(arr.length()) { i ->
                    val item = arr.getJSONObject(i)
                    CarreraCatalogo(clave = item.optString("clave"), nombre = item.optString("nombre"))
                }
                Result.success(carreras)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun materias(claveCarrera: String): Result<List<MateriaPlan>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(ApiConfig.planEstudios(claveCarrera)).build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(IllegalStateException(json.optString("error", "No se pudo consultar el plan de estudios")))
                }
                val arr = json.getJSONArray("materias")
                val materias = List(arr.length()) { i ->
                    val item = arr.getJSONObject(i)
                    MateriaPlan(
                        nombre = item.optString("nombre"),
                        creditos = item.optInt("creditos"),
                        semestre = item.optInt("semestre")
                    )
                }
                Result.success(materias)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun grupos(claveCarrera: String): Result<List<GrupoInfo>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(ApiConfig.grupos(claveCarrera)).build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(IllegalStateException(json.optString("error", "No se pudieron consultar los grupos")))
                }
                val arr = json.getJSONArray("grupos")
                val grupos = List(arr.length()) { i ->
                    val item = arr.getJSONObject(i)
                    GrupoInfo(
                        clave = item.optString("clave"),
                        semestre = item.optInt("semestre"),
                        turno = item.optString("turno"),
                        numero = item.optInt("numero"),
                        periodo = item.optString("periodo")
                    )
                }
                Result.success(grupos)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
