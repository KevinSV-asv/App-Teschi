package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Datos propios del alumno logueado — lo que ve/edita en "Mi perfil". */
data class PerfilAlumno(
    val matricula: String,
    val nombreCompleto: String,
    val correoOtp: String?,
    val correoInstitucional: String?,
    val carrera: String?,
    val claveCarrera: String?,
    val semestre: Int?
)

/** Permite sustituir PerfilAlumnoService por un fake en tests. */
interface PerfilAlumnoApi {
    suspend fun obtener(matricula: String): Result<PerfilAlumno>
    suspend fun actualizarCorreo(matricula: String, correoOtp: String): Result<Unit>
    suspend fun cambiarPassword(matricula: String, passwordActual: String, passwordNueva: String): Result<Unit>
}

/**
 * Perfil propio del alumno ya logueado: ver sus datos y cambiar su contraseña
 * sin pasar por el flujo de "olvidé mi contraseña". Solo el correo de
 * recuperación (`correoOtp`) es editable aquí — nombre, carrera y semestre son
 * datos oficiales que controla el director desde el panel de administración.
 */
object PerfilAlumnoService : PerfilAlumnoApi {
    private val client = SesionAlumno.client

    override suspend fun obtener(matricula: String): Result<PerfilAlumno> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ApiConfig.miPerfil(matricula))
                .conSesionAlumno()
                .build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("error", "No se pudo consultar tu perfil"))
                    )
                }
                val perfil = json.getJSONObject("perfil")
                Result.success(
                    PerfilAlumno(
                        matricula = perfil.optString("matricula"),
                        nombreCompleto = perfil.optString("nombreCompleto"),
                        correoOtp = if (perfil.isNull("correoOtp")) null else perfil.optString("correoOtp"),
                        correoInstitucional = if (perfil.isNull("correoInstitucional")) null else perfil.optString("correoInstitucional"),
                        carrera = if (perfil.isNull("carrera")) null else perfil.optString("carrera"),
                        claveCarrera = if (perfil.isNull("claveCarrera")) null else perfil.optString("claveCarrera"),
                        semestre = if (perfil.isNull("semestre")) null else perfil.optInt("semestre")
                    )
                )
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun actualizarCorreo(matricula: String, correoOtp: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().put("correoOtp", correoOtp)
                    .toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(ApiConfig.miPerfil(matricula))
                    .conSesionAlumno()
                    .put(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (!response.isSuccessful || !json.optBoolean("ok")) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("error", "No se pudo actualizar tu correo"))
                        )
                    }
                    Result.success(Unit)
                }
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    override suspend fun cambiarPassword(
        matricula: String,
        passwordActual: String,
        passwordNueva: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("matricula", matricula)
                .put("passwordActual", passwordActual)
                .put("passwordNueva", passwordNueva)
                .toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(ApiConfig.cambiarPasswordPropia)
                .conSesionAlumno()
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("error", "No se pudo cambiar tu contraseña"))
                    )
                }
                Result.success(Unit)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
