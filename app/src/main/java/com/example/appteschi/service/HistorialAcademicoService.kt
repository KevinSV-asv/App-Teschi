package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.HistorialAcademico
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.data.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/** Historial académico de un alumno — carrera/semestre son del propio
 *  alumno en sesión (necesarios para mostrarlos en Kardex). */
data class HistorialAlumnoInfo(
    val carrera: String,
    val claveCarrera: String,
    val semestre: Int,
    val materias: List<MateriaHistorial>
)

/** Permite sustituir HistorialAcademicoService por un fake en tests. */
interface HistorialAcademicoApi {
    suspend fun historial(matricula: String): Result<HistorialAlumnoInfo>
}

/**
 * Historial académico real del alumno (Kardex/Tira de Materias/Calificaciones
 * del lado alumno) — consulta `GET /api/mi-historial/{matricula}`, la misma
 * consulta que ya usa el panel de administrador para editar calificaciones
 * (`/api/calificaciones/{matricula}`), así que ambos lados siempre muestran
 * exactamente lo mismo. `periodo` no se llena — la tabla real
 * dbo.HistorialAcademico no guarda el período por materia todavía.
 */
object HistorialAcademicoService : HistorialAcademicoApi {
    private val client = SesionAlumno.client

    override suspend fun historial(matricula: String): Result<HistorialAlumnoInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ApiConfig.miHistorial(matricula))
                .conSesionAlumno()
                .build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("error", "No se pudo consultar tu historial académico"))
                    )
                }
                val alumno = json.getJSONObject("alumno")
                val arr = json.getJSONArray("materias")
                val materias = List(arr.length()) { i ->
                    val m = arr.getJSONObject(i)
                    val estatusCodigo = if (m.isNull("EstatusCodigo")) null else m.optString("EstatusCodigo")
                    MateriaHistorial(
                        numero = m.optInt("IdMateria"),
                        nombre = m.optString("Nombre"),
                        creditos = m.optInt("Creditos"),
                        semestre = m.optInt("Semestre"),
                        calificacion = if (m.isNull("Calificacion")) null else m.optDouble("Calificacion"),
                        periodo = null,
                        estatus = when (estatusCodigo) {
                            "AP" -> EstatusMateria.APROBADA
                            "NA" -> EstatusMateria.NO_APROBADA
                            else -> EstatusMateria.POR_CURSAR
                        }
                    )
                }
                Result.success(
                    HistorialAlumnoInfo(
                        carrera = alumno.optString("carrera"),
                        claveCarrera = alumno.optString("claveCarrera"),
                        semestre = alumno.optInt("semestre"),
                        materias = materias
                    )
                )
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

/**
 * Punto de acceso único para Kardex/Tira de Materias/Calificaciones: los
 * usuarios de prueba sem1..sem9 ven datos simulados (nunca llaman al
 * backend), cualquier otro alumno ve su historial real. Así ningún
 * ViewModel repite esta decisión por su cuenta.
 */
suspend fun historialDelAlumnoEnSesion(historialService: HistorialAcademicoApi): Result<HistorialAlumnoInfo> {
    val semestreSimulado = UserSession.semestreSimulado
    return if (semestreSimulado != null) {
        Result.success(
            HistorialAlumnoInfo(
                carrera = "Ingeniería en Sistemas Computacionales",
                claveCarrera = "ISC",
                semestre = semestreSimulado,
                materias = HistorialAcademico.materiasSimuladas(semestreSimulado)
            )
        )
    } else {
        historialService.historial(UserSession.matricula)
    }
}
