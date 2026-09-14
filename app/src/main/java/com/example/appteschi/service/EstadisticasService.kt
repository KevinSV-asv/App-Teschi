package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class ConteoCarrera(val carrera: String, val claveCarrera: String, val total: Int)
data class ConteoSemestre(val semestre: Int, val total: Int)
data class ConteoMes(val mes: String, val total: Int)
data class ConteoEstatus(val codigo: String, val nombre: String, val total: Int)

data class EstadisticasResumen(
    val porCarrera: List<ConteoCarrera>,
    val porSemestre: List<ConteoSemestre>,
    val activos: Int,
    val inactivos: Int,
    val sinSemestre: Int,
    val altasPorMes: List<ConteoMes>,
    val calificaciones: List<ConteoEstatus>
)

/** Agregados para las gráficas del panel de administrador — ver GET /api/estadisticas. */
object EstadisticasService {
    private val client = OkHttpClient()

    suspend fun obtener(): Result<EstadisticasResumen> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ApiConfig.estadisticas)
                .conSesionAdmin()
                .build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(IllegalStateException(json.optString("error", "No se pudieron consultar las estadísticas")))
                }
                val porCarreraArr = json.getJSONArray("porCarrera")
                val porCarrera = List(porCarreraArr.length()) { i ->
                    val item = porCarreraArr.getJSONObject(i)
                    ConteoCarrera(item.optString("Carrera"), item.optString("ClaveCarrera"), item.optInt("Total"))
                }
                val porSemestreArr = json.getJSONArray("porSemestre")
                val porSemestre = List(porSemestreArr.length()) { i ->
                    val item = porSemestreArr.getJSONObject(i)
                    ConteoSemestre(item.optInt("Semestre"), item.optInt("Total"))
                }
                val estado = json.getJSONObject("estado")
                val altasArr = json.getJSONArray("altasPorMes")
                val altasPorMes = List(altasArr.length()) { i ->
                    val item = altasArr.getJSONObject(i)
                    ConteoMes(item.optString("Mes"), item.optInt("Total"))
                }
                val calificacionesArr = json.getJSONArray("calificaciones")
                val calificaciones = List(calificacionesArr.length()) { i ->
                    val item = calificacionesArr.getJSONObject(i)
                    ConteoEstatus(item.optString("Codigo"), item.optString("Nombre"), item.optInt("Total"))
                }
                Result.success(
                    EstadisticasResumen(
                        porCarrera = porCarrera,
                        porSemestre = porSemestre,
                        activos = estado.optInt("Activos"),
                        inactivos = estado.optInt("Inactivos"),
                        sinSemestre = estado.optInt("SinSemestre"),
                        altasPorMes = altasPorMes,
                        calificaciones = calificaciones
                    )
                )
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
