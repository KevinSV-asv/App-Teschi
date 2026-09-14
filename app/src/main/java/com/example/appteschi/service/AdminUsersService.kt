package com.example.appteschi.service

import com.example.appteschi.core.config.ApiConfig
import com.example.appteschi.data.ManagedUser
import com.example.appteschi.data.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private val USERS_API_URL = ApiConfig.usuarios

object AdminUsersService {
    private val client = OkHttpClient()

    suspend fun fetch(): Result<List<ManagedUser>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(USERS_API_URL).conSesionAdmin().build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(IllegalStateException(json.optString("error", "No se pudieron consultar los usuarios")))
                }
                val data = json.getJSONArray("data")
                val users = List(data.length()) { index ->
                    val item = data.getJSONObject(index)
                    ManagedUser(
                        id = item.getInt("IdUsuario"),
                        username = item.optString("Matricula"),
                        name = item.optString("NombreCompleto"),
                        role = if (item.optString("Carrera").equals("ADMINISTRADOR", true)) UserRole.ADMINISTRADOR else UserRole.ALUMNO,
                        correo = item.optString("CorreoOtp"),
                        correoInstitucional = item.optString("CorreoInstitucional"),
                        carrera = item.optString("Carrera"),
                        claveCarrera = item.optString("ClaveCarrera"),
                        semestre = if (item.isNull("Semestre")) null else item.optInt("Semestre"),
                        active = item.optBoolean("Activo", true)
                    )
                }
                Result.success(users)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Alta de un alumno nuevo por el administrador — sin contraseña (el alumno la crea al registrarse). */
    suspend fun crear(
        matricula: String,
        nombreCompleto: String,
        claveCarrera: String,
        correoOtp: String = "",
        correoInstitucional: String = "",
        semestre: Int? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("matricula", matricula)
                .put("nombreCompleto", nombreCompleto)
                .put("claveCarrera", claveCarrera)
                .put("correoOtp", correoOtp)
                .put("correoInstitucional", correoInstitucional)
            if (semestre != null) json.put("semestre", semestre)
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(USERS_API_URL)
                .conSesionAdmin()
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val respJson = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !respJson.optBoolean("ok")) {
                    Result.failure(IllegalStateException(respJson.optString("error", "No se pudo crear el alumno")))
                } else Result.success(Unit)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Edición parcial de perfil: solo se envían las llaves presentes en [cambios]. */
    suspend fun actualizarPerfil(matricula: String, cambios: Map<String, Any?>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            cambios.forEach { (key, value) -> json.put(key, value ?: JSONObject.NULL) }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$USERS_API_URL/$matricula")
                .conSesionAdmin()
                .put(body)
                .build()
            client.newCall(request).execute().use { response ->
                val respJson = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !respJson.optBoolean("ok")) {
                    Result.failure(IllegalStateException(respJson.optString("error", "No se pudo actualizar el perfil")))
                } else Result.success(Unit)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Baja definitiva de un alumno — elimina también su historial académico y credenciales. Solo SUPERADMIN. */
    suspend fun eliminar(matricula: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$USERS_API_URL/$matricula")
                .conSesionAdmin()
                .delete()
                .build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    Result.failure(IllegalStateException(json.optString("error", "No se pudo eliminar el alumno")))
                } else Result.success(Unit)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Asigna/actualiza el semestre (1-12) de un alumno — usado desde AdminProfileScreen. */
    suspend fun actualizarSemestre(matricula: String, semestre: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("semestre", semestre).toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$USERS_API_URL/$matricula/semestre")
                .conSesionAdmin()
                .put(body)
                .build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    Result.failure(IllegalStateException(json.optString("error", "No se pudo actualizar el semestre")))
                } else {
                    Result.success(Unit)
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
