package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import com.example.appteschi.data.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/** Estado de una clase respecto a la hora actual — lo calcula el backend. */
enum class EstadoClase { EN_CURSO, PROXIMA, TERMINADA }

data class ClaseHoy(
    val materia: String,
    val horaInicio: String,
    val horaFin: String,
    val profesor: String?,
    val aula: String?,
    val modalidad: String,
    val estado: EstadoClase
)

data class HorarioHoy(val dia: String, val clases: List<ClaseHoy>)

/** Permite sustituir HorarioService por un fake en tests. */
interface HorarioApi {
    suspend fun horarioDeHoy(matricula: String): Result<HorarioHoy>
}

/**
 * Horario real del alumno para "Materias y Horario de Hoy" en Inicio —
 * consulta `GET /api/mi-horario/{matricula}`. Ese horario solo existe si un
 * administrador ya subió un Excel para la carrera+semestre del alumno (ver
 * DEC-029 y AdminHorariosScreen) — si no, el backend regresa una lista vacía,
 * no un error.
 */
object HorarioService : HorarioApi {
    private val client = SesionAlumno.client

    override suspend fun horarioDeHoy(matricula: String): Result<HorarioHoy> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ApiConfig.miHorario(matricula))
                .conSesionAlumno()
                .build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("error", "No se pudo consultar tu horario"))
                    )
                }
                val arr = json.getJSONArray("clases")
                val clases = List(arr.length()) { i ->
                    val c = arr.getJSONObject(i)
                    ClaseHoy(
                        materia = c.optString("materia"),
                        horaInicio = c.optString("horaInicio"),
                        horaFin = c.optString("horaFin"),
                        profesor = if (c.isNull("profesor")) null else c.optString("profesor"),
                        aula = if (c.isNull("aula")) null else c.optString("aula"),
                        modalidad = c.optString("modalidad"),
                        estado = when (c.optString("estado")) {
                            "EN_CURSO" -> EstadoClase.EN_CURSO
                            "TERMINADA" -> EstadoClase.TERMINADA
                            else -> EstadoClase.PROXIMA
                        }
                    )
                }
                Result.success(HorarioHoy(dia = json.optString("dia"), clases = clases))
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

/**
 * Punto de acceso único para "Materias y Horario de Hoy": los usuarios de
 * prueba sem1..sem9 no existen en la base de datos, así que nunca llaman al
 * backend — ven el horario simplemente vacío (mismo criterio que
 * [historialDelAlumnoEnSesion] para Kardex/Tira/Calificaciones).
 */
suspend fun horarioDeHoyDelAlumnoEnSesion(horarioService: HorarioApi): Result<HorarioHoy> {
    if (UserSession.semestreSimulado != null) {
        return Result.success(HorarioHoy(dia = "", clases = emptyList()))
    }
    return horarioService.horarioDeHoy(UserSession.matricula)
}
