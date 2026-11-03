package com.example.appteschi.service

import com.example.appteschi.core.config.SiiaEndpoints
import com.example.appteschi.data.CuentaSiia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/** Token final del SIIA (después del NIP) con los datos de la cuenta. */
data class SesionSiia(val token: String, val expiraEnMinutos: Int, val cuenta: CuentaSiia)

/** Resultado de `POST login.ashx` sin NIP. */
sealed class SiiaLoginResultado {
    /** Contraseña correcta: falta el NIP (o crearlo si [tieneNip] es false). [tempToken] dura 10 min. */
    data class RequiereNip(val tieneNip: Boolean, val tempToken: String, val cuenta: CuentaSiia?) : SiiaLoginResultado()

    /** El SIIA entregó el token final sin pedir NIP. */
    data class Sesion(val sesion: SesionSiia) : SiiaLoginResultado()

    data class Error(val mensaje: String) : SiiaLoginResultado()
}

/** Resultado de `POST nip.ashx` con `verificar` o `configurar`. */
sealed class SiiaNipResultado {
    data class Sesion(val sesion: SesionSiia) : SiiaNipResultado()
    data class Error(val mensaje: String) : SiiaNipResultado()
}

interface SiiaAuthService {
    suspend fun login(usuario: String, password: String): SiiaLoginResultado

    /** [cuentaPaso1]: datos del login, por si el SIIA no los repite en `usuarioInfo`. */
    suspend fun verificarNip(tempToken: String, nip: String, cuentaPaso1: CuentaSiia?): SiiaNipResultado

    /** Primera vez: crea el NIP y entra. */
    suspend fun crearNip(tempToken: String, nuevoNip: String, confirmarNip: String, cuentaPaso1: CuentaSiia?): SiiaNipResultado

    /** El SIIA manda el NIP al correo institucional de la CURP; devuelve el mensaje para mostrar. */
    suspend fun recuperarNip(curp: String, usuario: String): Result<String>

    /** Cambia el NIP con la sesión ya iniciada (Perfil). */
    suspend fun cambiarNip(token: String, nipActual: String, nuevoNip: String, confirmarNip: String): Result<String>
}

/** Implementación real: llama directo a la API oficial del SIIA (DEC-043). */
class SiiaHttpAuthService(
    private val endpoints: SiiaEndpoints = SiiaEndpoints(),
    private val client: OkHttpClient = ClienteHttp.nuevo()
) : SiiaAuthService {

    private val tipoJson = "application/json".toMediaType()

    private class Respuesta(val codigo: Int, val json: JSONObject?)

    private fun post(url: String, cuerpo: JSONObject, token: String? = null): Respuesta {
        val request = Request.Builder()
            .url(url)
            .post(cuerpo.toString().toRequestBody(tipoJson))
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        return client.newCall(request).execute().use { r -> Respuesta(r.code, jsonONulo(r.body?.string())) }
    }

    override suspend fun login(usuario: String, password: String): SiiaLoginResultado = withContext(Dispatchers.IO) {
        try {
            val r = post(endpoints.login, JSONObject().put("usuario", usuario.trim()).put("password", password))
            val json = r.json
            when {
                r.codigo == 401 || r.codigo == 403 ->
                    SiiaLoginResultado.Error(json.mensajeSiia() ?: "Usuario o contraseña incorrectos.")
                r.codigo == 429 ->
                    SiiaLoginResultado.Error(json.mensajeSiia() ?: "Demasiados intentos en el SIIA. Espera un momento.")
                r.codigo != 200 || json == null || !json.optBoolean("ok") ->
                    SiiaLoginResultado.Error(json.mensajeSiia() ?: "El SIIA no respondió correctamente. Intenta más tarde.")
                json.textoONulo("token") != null -> sesionDe(json, json.optJSONObject("usuario").aCuenta(usuario))
                    ?.let { SiiaLoginResultado.Sesion(it) }
                    ?: SiiaLoginResultado.Error("El SIIA no devolvió los datos de tu cuenta. Intenta más tarde.")
                json.textoONulo("tempToken") != null -> SiiaLoginResultado.RequiereNip(
                    tieneNip = json.optBoolean("tieneNip", true),
                    tempToken = json.optString("tempToken"),
                    cuenta = json.optJSONObject("usuario").aCuenta(usuario)
                )
                else -> SiiaLoginResultado.Error("El SIIA no respondió correctamente. Intenta más tarde.")
            }
        } catch (e: IOException) {
            SiiaLoginResultado.Error(e.message ?: "No se pudo conectar con el SIIA.")
        }
    }

    override suspend fun verificarNip(tempToken: String, nip: String, cuentaPaso1: CuentaSiia?): SiiaNipResultado =
        pasoNip(
            JSONObject().put("accion", "verificar").put("tempToken", tempToken).put("nip", nip),
            cuentaPaso1, porDefecto = "NIP incorrecto."
        )

    override suspend fun crearNip(tempToken: String, nuevoNip: String, confirmarNip: String, cuentaPaso1: CuentaSiia?): SiiaNipResultado =
        pasoNip(
            JSONObject().put("accion", "configurar").put("tempToken", tempToken)
                .put("nuevoNip", nuevoNip).put("confirmarNip", confirmarNip),
            cuentaPaso1, porDefecto = "No se pudo crear tu NIP. Intenta de nuevo."
        )

    private suspend fun pasoNip(cuerpo: JSONObject, cuentaPaso1: CuentaSiia?, porDefecto: String): SiiaNipResultado =
        withContext(Dispatchers.IO) {
            try {
                val r = post(endpoints.nip, cuerpo)
                val json = r.json
                when {
                    r.codigo == 429 -> SiiaNipResultado.Error(json.mensajeSiia() ?: "Demasiados intentos con el NIP. Espera unos minutos.")
                    r.codigo != 200 || json == null || !json.optBoolean("ok") -> SiiaNipResultado.Error(
                        json.mensajeSiia() ?: if (r.codigo in 400..499) porDefecto else "El SIIA no respondió correctamente. Intenta más tarde."
                    )
                    else -> {
                        val usuario = cuentaPaso1?.usuario.orEmpty()
                        val cuenta = json.optJSONObject("usuarioInfo").aCuenta(usuario)
                            ?: json.optJSONObject("usuario").aCuenta(usuario)
                            ?: cuentaPaso1
                        sesionDe(json, cuenta)?.let { SiiaNipResultado.Sesion(it) }
                            ?: SiiaNipResultado.Error("El SIIA no devolvió los datos de tu cuenta. Intenta más tarde.")
                    }
                }
            } catch (e: IOException) {
                SiiaNipResultado.Error(e.message ?: "No se pudo conectar con el SIIA.")
            }
        }

    override suspend fun recuperarNip(curp: String, usuario: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val r = post(endpoints.nip, JSONObject().put("accion", "recuperar").put("curp", curp.trim().uppercase()))
            val json = r.json
            if (r.codigo != 200 || json == null || !json.optBoolean("ok")) {
                return@withContext Result.failure(IllegalStateException(json.mensajeSiia() ?: "No se pudo recuperar tu NIP."))
            }
            // El correo enmascarado solo se muestra si la CURP es de quien está iniciando sesión.
            val dueno = json.textoONulo("usuario")
            val esSuya = dueno == null || dueno.equals(usuario.trim(), ignoreCase = true)
            val correo = json.textoONulo("correo")
            Result.success(
                if (esSuya && correo != null) "Te enviamos tu NIP a $correo"
                else "Si la CURP es correcta, el SIIA envió tu NIP a tu correo institucional."
            )
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    override suspend fun cambiarNip(token: String, nipActual: String, nuevoNip: String, confirmarNip: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val r = post(
                    endpoints.nip,
                    JSONObject().put("accion", "cambiar").put("nipActual", nipActual)
                        .put("nuevoNip", nuevoNip).put("confirmarNip", confirmarNip),
                    token = token
                )
                val json = r.json
                if (r.codigo != 200 || json == null || !json.optBoolean("ok")) {
                    Result.failure(IllegalStateException(json.mensajeSiia() ?: "No se pudo cambiar tu NIP."))
                } else {
                    Result.success(json.mensajeSiia() ?: "Tu NIP se actualizó.")
                }
            } catch (e: IOException) {
                Result.failure(e)
            }
        }

    private fun sesionDe(json: JSONObject, cuenta: CuentaSiia?): SesionSiia? {
        val token = json.textoONulo("token") ?: return null
        return cuenta?.let { SesionSiia(token, json.optInt("expiraEnMinutos", 480), it) }
    }
}

/** La que usa la app. */
object SiiaAccountAuthService : SiiaAuthService by SiiaHttpAuthService()

private fun JSONObject.textoONulo(campo: String): String? =
    if (isNull(campo)) null else optString(campo).trim().takeIf { it.isNotEmpty() }

/** Bloque `usuario` / `usuarioInfo` del SIIA → [CuentaSiia]; null si no viene. */
internal fun JSONObject?.aCuenta(usuarioEscrito: String): CuentaSiia? {
    val o = this ?: return null
    val nombre = o.textoONulo("nombreCompleto")
        ?: listOfNotNull(o.textoONulo("nombre"), o.textoONulo("paterno"), o.textoONulo("materno")).joinToString(" ")
    val usuario = o.textoONulo("usuario") ?: usuarioEscrito.trim()
    return CuentaSiia(
        usuario = usuario,
        matricula = o.textoONulo("numUsuario") ?: usuario,
        nombreCompleto = nombre,
        correo = o.textoONulo("correo").orEmpty(),
        tipo = o.textoONulo("tipo").orEmpty(),
        carrera = o.textoONulo("area").orEmpty(),
        idCarrera = if (o.isNull("idCarrera")) null else o.optInt("idCarrera", -1).takeIf { it > 0 },
        cambioPw = o.optInt("cambioPw", 0) != 0 || o.optBoolean("cambioPw", false)
    )
}
