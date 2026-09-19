package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class HorarioClase(
    val id: Int,
    val grupo: String?,
    val materia: String,
    val diaSemana: Int,
    val horaInicio: String,
    val horaFin: String,
    val profesor: String?,
    val aula: String?,
    val modalidad: String,
    val periodoEtiqueta: String?
)

data class ResultadoImportacionHorario(val filasImportadas: Int, val erroresFilas: List<String>)

/** Permite sustituir HorarioAdminService por un fake en tests. */
interface HorarioAdminApi {
    suspend fun importar(
        claveCarrera: String,
        semestre: Int,
        grupo: String?,
        periodoEtiqueta: String?,
        nombreArchivo: String,
        contenido: ByteArray
    ): Result<ResultadoImportacionHorario>

    suspend fun listar(claveCarrera: String, semestre: Int): Result<List<HorarioClase>>
    suspend fun eliminar(id: Int): Result<Unit>
}

/**
 * Administración del horario de clases — sube el Excel con el horario real
 * de una carrera+semestre (ver DEC-029) para que la app del alumno pueda
 * mostrar "Materias y Horario de Hoy" en Inicio. Único lugar de la app que
 * sube un archivo (multipart) en vez de JSON.
 */
object HorarioAdminService : HorarioAdminApi {
    private val client = OkHttpClient()
    private val MEDIA_XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaType()

    override suspend fun importar(
        claveCarrera: String,
        semestre: Int,
        grupo: String?,
        periodoEtiqueta: String?,
        nombreArchivo: String,
        contenido: ByteArray
    ): Result<ResultadoImportacionHorario> = withContext(Dispatchers.IO) {
        try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("archivo", nombreArchivo, contenido.toRequestBody(MEDIA_XLSX))
                .addFormDataPart("claveCarrera", claveCarrera)
                .addFormDataPart("semestre", semestre.toString())
                .apply {
                    if (!grupo.isNullOrBlank()) addFormDataPart("grupo", grupo)
                    if (!periodoEtiqueta.isNullOrBlank()) addFormDataPart("periodoEtiqueta", periodoEtiqueta)
                }
                .build()
            val request = Request.Builder().url(ApiConfig.horarioImportar).conSesionAdmin().post(body).build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("error", "No se pudo importar el horario"))
                    )
                }
                val erroresArr = json.optJSONArray("erroresFilas")
                val errores = erroresArr?.let { arr -> List(arr.length()) { arr.getString(it) } } ?: emptyList()
                Result.success(ResultadoImportacionHorario(json.optInt("filasImportadas"), errores))
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun listar(claveCarrera: String, semestre: Int): Result<List<HorarioClase>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${ApiConfig.horarios}?claveCarrera=$claveCarrera&semestre=$semestre"
                val request = Request.Builder().url(url).conSesionAdmin().build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("error", "No se pudo consultar el horario"))
                        )
                    }
                    val arr = json.getJSONArray("data")
                    val lista = List(arr.length()) { i ->
                        val o = arr.getJSONObject(i)
                        HorarioClase(
                            id = o.optInt("IdHorario"),
                            grupo = if (o.isNull("Grupo")) null else o.optString("Grupo"),
                            materia = o.optString("NombreMateria"),
                            diaSemana = o.optInt("DiaSemana"),
                            horaInicio = o.optString("HoraInicio"),
                            horaFin = o.optString("HoraFin"),
                            profesor = if (o.isNull("Profesor")) null else o.optString("Profesor"),
                            aula = if (o.isNull("Aula")) null else o.optString("Aula"),
                            modalidad = o.optString("Modalidad"),
                            periodoEtiqueta = if (o.isNull("PeriodoEtiqueta")) null else o.optString("PeriodoEtiqueta")
                        )
                    }
                    Result.success(lista)
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    override suspend fun eliminar(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(ApiConfig.horarioEliminar(id)).conSesionAdmin().delete().build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("error", "No se pudo eliminar ese horario"))
                    )
                }
                Result.success(Unit)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
