package com.example.appteschi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.core.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "RegistroViewModel"

data class RegistroUiState(
    val enviando: Boolean = false,
    val exito: Boolean = false,
    val mensaje: String? = null
)

class RegistroViewModel(
    private val registroApiUrl: String = ApiConfig.registro,
    // Timeouts ampliados + logging para diagnosticar problemas de conexión con la API
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun registrar(
        matricula: String,
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        fechaNacimiento: String,
        sistema: String,
        carrera: String,
        password: String,
        confirmarPassword: String
    ) {
        // ── Validaciones locales ──────────────────────────────────────────
        val values = listOf(
            matricula, nombres, apellidoPaterno, apellidoMaterno,
            fechaNacimiento, sistema, carrera, password, confirmarPassword
        )
        if (values.any { it.trim().isBlank() }) {
            _uiState.value = RegistroUiState(mensaje = "Completa todos los campos obligatorios.")
            return
        }
        if (password != confirmarPassword) {
            _uiState.value = RegistroUiState(mensaje = "Las contraseñas no coinciden.")
            return
        }
        if (!Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)[A-Za-z\\d]{8,}$").matches(password)) {
            _uiState.value = RegistroUiState(
                mensaje = "La contraseña requiere mínimo 8 caracteres, una mayúscula, una minúscula y un número."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = RegistroUiState(enviando = true, mensaje = "Registrando cuenta…")
            val result = withContext(Dispatchers.IO) {
                sendRegistration(
                    matricula, nombres, apellidoPaterno, apellidoMaterno,
                    fechaNacimiento, sistema, carrera, password
                )
            }
            _uiState.value = result
        }
    }

    private fun sendRegistration(
        matricula: String,
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        fechaNacimiento: String,
        sistema: String,
        carrera: String,
        password: String
    ): RegistroUiState {
        return try {
            val payload = JSONObject().apply {
                put("matricula",       matricula.trim())
                put("nombres",         nombres.trim())
                put("apellidoPaterno", apellidoPaterno.trim())
                put("apellidoMaterno", apellidoMaterno.trim())
                put("fechaNacimiento", fechaNacimiento.trim())
                put("sistema",         sistema.trim())
                put("carrera",         carrera.trim())
                put("password",        password)
            }

            Log.d(TAG, "POST $registroApiUrl")
            Log.d(TAG, "Payload: $payload")

            val request = Request.Builder()
                .url(registroApiUrl)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                Log.d(TAG, "HTTP ${response.code} — body: $body")

                // Si la API devuelve HTML en lugar de JSON (ej. 404 de Express o Apache)
                val json = try {
                    JSONObject(body)
                } catch (_: Exception) {
                    return RegistroUiState(
                        mensaje = "Error ${response.code}: La API no devolvió JSON.\n${body.take(150)}"
                    )
                }

                when {
                    response.isSuccessful && json.optBoolean("ok") -> {
                        val correo = json.optString("correoInstitucional")
                        Log.d(TAG, "Registro exitoso — correo: $correo")
                        RegistroUiState(
                            exito = true,
                            mensaje = "¡Cuenta creada! Correo institucional asignado: $correo"
                        )
                    }
                    else -> {
                        // Mostrar el error exacto que devuelve el servidor para facilitar diagnóstico
                        val errorMsg = json.optString("error", "Error ${response.code} sin detalle.")
                        Log.e(TAG, "API rechazó el registro: $errorMsg")
                        RegistroUiState(mensaje = "El servidor respondió: $errorMsg")
                    }
                }
            }
        } catch (e: java.net.ConnectException) {
            // La API no está corriendo o la IP/puerto es incorrecto
            Log.e(TAG, "ConnectException: ${e.message}", e)
            RegistroUiState(
                mensaje = "No se pudo conectar con la API en $registroApiUrl\n" +
                        "Verifica que el servidor esté activo y accesible.\n" +
                        "Detalle: ${e.message}"
            )
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Timeout: ${e.message}", e)
            RegistroUiState(
                mensaje = "Tiempo de espera agotado. El servidor no respondió en 15 segundos."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado: ${e.javaClass.simpleName} — ${e.message}", e)
            RegistroUiState(
                mensaje = "Error: ${e.javaClass.simpleName} — ${e.message}"
            )
        }
    }
}
