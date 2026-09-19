package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import com.example.appteschi.data.EstatusReinscripcion
import com.example.appteschi.data.EstatusReinscripcionInfo
import com.example.appteschi.data.GrupoReinscripcion
import com.example.appteschi.data.MateriaPendiente
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Permite sustituir ReinscripcionService por un fake en tests. */
interface ReinscripcionApi {
    suspend fun estatus(matricula: String): Result<EstatusReinscripcionInfo>
    suspend fun enviarSolicitud(matricula: String, claveGrupo: String): Result<String>
}

/**
 * Reinscripción real (Fase 1 — ver 02_MODULOS/01_Reinscripcion.md): consulta
 * si el alumno es regular/irregular contra su historial académico real y
 * envía su solicitud de grupo, que el backend guarda de verdad en
 * dbo.SolicitudesReinscripcion. Ya no hay datos simulados de por medio.
 */
object ReinscripcionService : ReinscripcionApi {
    private val client = SesionAlumno.client

    override suspend fun estatus(matricula: String): Result<EstatusReinscripcionInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ApiConfig.reinscripcionEstatus(matricula))
                .conSesionAlumno()
                .build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("error", "No se pudo consultar tu estatus de reinscripción"))
                    )
                }

                fun grupoDe(o: JSONObject) = GrupoReinscripcion(
                    idGrupo = o.optInt("IdGrupo"),
                    clave = o.optString("Clave"),
                    semestre = o.optInt("Semestre"),
                    turno = o.optString("Turno"),
                    numero = o.optInt("Numero"),
                    periodo = o.optString("Periodo")
                )

                val alumno = json.getJSONObject("alumno")
                val materiasArr = json.getJSONArray("materiasPendientes")
                val materias = List(materiasArr.length()) { i ->
                    val m = materiasArr.getJSONObject(i)
                    MateriaPendiente(
                        idMateria = m.optInt("IdMateria"),
                        nombre = m.optString("Nombre"),
                        creditos = m.optInt("Creditos"),
                        semestre = m.optInt("Semestre")
                    )
                }
                val gruposArr = json.getJSONArray("grupos")
                val grupos = List(gruposArr.length()) { i -> grupoDe(gruposArr.getJSONObject(i)) }
                val grupoAsignado = json.optJSONObject("grupoAsignado")?.let { grupoDe(it) }

                Result.success(
                    EstatusReinscripcionInfo(
                        matricula = alumno.optString("matricula"),
                        nombreCompleto = alumno.optString("nombreCompleto"),
                        semestre = alumno.optInt("semestre"),
                        carrera = alumno.optString("carrera"),
                        estatus = when (json.optString("estatus")) {
                            "REGULAR" -> EstatusReinscripcion.REGULAR
                            "BLOQUEADO" -> EstatusReinscripcion.BLOQUEADO
                            else -> EstatusReinscripcion.IRREGULAR
                        },
                        motivo = if (json.isNull("motivo")) null else json.optString("motivo").takeIf { it.isNotBlank() },
                        materiasPendientes = materias,
                        grupoAsignado = grupoAsignado,
                        grupos = grupos
                    )
                )
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun enviarSolicitud(matricula: String, claveGrupo: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("matricula", matricula)
                    .put("claveGrupo", claveGrupo)
                    .toString()
                    .toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(ApiConfig.reinscripcionSolicitud)
                    .conSesionAlumno()
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("error", "No se pudo enviar tu solicitud de reinscripción"))
                        )
                    }
                    Result.success(json.optString("folio"))
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
}
