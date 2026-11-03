package com.example.appteschi.service

import com.example.appteschi.core.config.SiiaEndpoints
import com.example.appteschi.data.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar

/** Una materia con sus parciales del SIIA. Las calificaciones ya vienen como texto para mostrar ("—" si no hay). */
data class MateriaParcial(
    val materia: String,
    val profesor: String,
    val grupo: String,
    val parcial1: String,
    val parcial2: String,
    val parcial3: String,
    val promedio: String,
    val estatus: EstatusParcial
)

enum class EstatusParcial(val etiqueta: String) {
    APROBATORIA("Aprobatoria"),
    REPROBATORIA("Reprobatoria"),
    SIN_CALIFICAR("Sin calificar")
}

data class ResumenParcial(
    val totalMaterias: Int,
    val evaluadas: Int,
    val aprobadas: Int,
    val reprobadas: Int,
    val promedio: String
)

data class CalificacionesParciales(
    val periodo: String,
    val periodosDisponibles: List<String>,
    /** true si el SIIA bloquea la consulta por trámites de inicio de ciclo pendientes. */
    val bloqueado: Boolean,
    val mensajeBloqueo: String,
    val pendientes: List<String>,
    /** Aviso para el alumno, p. ej. cuando el SIIA no pudo dar el periodo actual y se muestra otro (BUG-024). */
    val aviso: String = "",
    val resumen: ResumenParcial?,
    val materias: List<MateriaParcial>
)

sealed class ResultadoParciales {
    data class Datos(val calificaciones: CalificacionesParciales) : ResultadoParciales()
    data class Error(val mensaje: String) : ResultadoParciales()
}

/** Una materia con sus calificaciones finales. Ya vienen como texto para mostrar ("—" si no hay). */
data class MateriaFinal(
    val materia: String,
    val profesor: String,
    val grupo: String,
    val primeraVuelta: String,
    val segundaVuelta: String,
    val calificacionFinal: String,
    val estatus: EstatusParcial
)

data class ResumenFinal(
    val totalMaterias: Int,
    val evaluadas: Int,
    val aprobadas: Int,
    val reprobadas: Int,
    val promedio: String,
    /** Dictamen del semestre según el SIIA: "Aprobatorio", "Regularizar" o "En Curso" (vacío si no viene). */
    val dictamen: String
)

data class CalificacionesFinales(
    val periodo: String,
    val periodosDisponibles: List<String>,
    val bloqueado: Boolean,
    val mensajeBloqueo: String,
    val pendientes: List<String>,
    val aviso: String = "",
    val resumen: ResumenFinal?,
    val materias: List<MateriaFinal>
)

sealed class ResultadoFinales {
    data class Datos(val calificaciones: CalificacionesFinales) : ResultadoFinales()
    data class Error(val mensaje: String) : ResultadoFinales()
}

interface CalificacionesSiiaApi {
    /** periodo = null → el periodo actual del SIIA. */
    suspend fun parciales(periodo: String?): ResultadoParciales

    /** periodo = null → el periodo actual del SIIA. */
    suspend fun finales(periodo: String?): ResultadoFinales
}

/** Respuesta cruda del SIIA: código HTTP y cuerpo (null si no era JSON). */
internal data class RespuestaSiia(val codigo: Int, val json: JSONObject?)

/**
 * `GET calificaciones/parciales.ashx` y `finales.ashx` directo al SIIA con el
 * token del alumno (DEC-042, DEC-043, DEC-044). Mismas reglas que tenía el
 * backend: si el SIIA falla al calcular el periodo actual se prueban los
 * recientes, y el bloqueo por trámites se muestra como aviso, no como error.
 */
class CalificacionesSiiaHttp(
    private val endpoints: SiiaEndpoints = SiiaEndpoints(),
    private val client: OkHttpClient = SesionAlumno.client,
    private val token: () -> String? = { UserSession.token },
    private val hoy: () -> Calendar = { Calendar.getInstance() }
) : CalificacionesSiiaApi {

    override suspend fun parciales(periodo: String?): ResultadoParciales =
        consultarPeriodo(periodo, endpoints::calificacionesParciales, ::interpretarParciales) { ResultadoParciales.Error(it) }

    override suspend fun finales(periodo: String?): ResultadoFinales =
        consultarPeriodo(periodo, endpoints::calificacionesFinales, ::interpretarFinales) { ResultadoFinales.Error(it) }

    private suspend fun <T> consultarPeriodo(
        periodo: String?,
        url: (String?) -> String,
        interpretar: (RespuestaSiia, String) -> T,
        error: (String) -> T
    ): T = withContext(Dispatchers.IO) {
        val tokenActual = token()
        if (tokenActual.isNullOrBlank()) return@withContext error(MENSAJE_SESION_EXPIRADA)
        try {
            var respuesta = consultar(url(periodo), tokenActual)
            var aviso = ""
            if (periodo.isNullOrBlank() && esErrorGenericoSiia(respuesta)) {
                val fecha = hoy()
                for (p in periodosRecientes(fecha.get(Calendar.YEAR), fecha.get(Calendar.MONTH) + 1)) {
                    val intento = consultar(url(p), tokenActual)
                    if (!esErrorGenericoSiia(intento)) {
                        respuesta = intento
                        aviso = "El SIIA no pudo mostrar el periodo actual; se muestra el periodo $p."
                        break
                    }
                }
            }
            interpretar(respuesta, aviso)
        } catch (e: IOException) {
            error(e.message ?: "No se pudo conectar con el SIIA.")
        }
    }

    private fun consultar(url: String, token: String): RespuestaSiia {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
        return client.newCall(request).execute().use { RespuestaSiia(it.code, jsonONulo(it.body?.string())) }
    }
}

/** La que usa la app. */
object CalificacionesSiiaService : CalificacionesSiiaApi by CalificacionesSiiaHttp()

/**
 * Periodo actual y los dos anteriores con el formato del SIIA ("2026-1").
 * Supuesto (BUG-024): "-1" = febrero–julio y "-2" = agosto–enero.
 */
internal fun periodosRecientes(anio: Int, mes: Int): List<String> {
    var (a, n) = when {
        mes >= 8 -> anio to 2
        mes == 1 -> (anio - 1) to 2
        else -> anio to 1
    }
    return List(3) {
        val actual = "$a-$n"
        if (n == 2) n = 1 else { a -= 1; n = 2 }
        actual
    }
}

/** Error genérico del SIIA ("No fue posible procesar la solicitud."): ni sesión vencida (401), ni bloqueo (403), ni alumno inexistente (404). */
internal fun esErrorGenericoSiia(r: RespuestaSiia): Boolean =
    r.codigo >= 500 || r.codigo == 400 || (r.codigo == 200 && (r.json == null || okEsFalso(r.json)))

/** La respuesta real de calificaciones no siempre trae `ok`: solo cuenta como fallo si dice ok:false. */
internal fun okEsFalso(json: JSONObject): Boolean = json.has("ok") && !json.optBoolean("ok", true)

/**
 * Mensaje para el alumno si la consulta falló, o null si [r] trae datos (el
 * bloqueo por trámites cuenta como datos: se muestra como aviso). [que] es lo
 * que se pidió, para el mensaje: "tus calificaciones", "tu kardex"…
 */
internal fun errorDeConsulta(r: RespuestaSiia, que: String): String? {
    val json = r.json
    return when {
        r.codigo == 401 -> MENSAJE_SESION_EXPIRADA
        (r.codigo == 403 || r.codigo == 200) && json?.optBoolean("bloqueado") == true -> null
        r.codigo == 404 -> json.mensajeSiia() ?: "El SIIA no encontró tu registro de alumno."
        r.codigo == 429 -> json.mensajeSiia() ?: "Demasiadas consultas al SIIA. Espera un momento."
        r.codigo != 200 || json == null || okEsFalso(json) -> {
            val detalle = json.mensajeSiia()?.let { " (el SIIA respondió: \"$it\")" }.orEmpty()
            "El SIIA no pudo entregar $que$detalle. Intenta más tarde."
        }
        else -> null
    }
}

internal fun interpretarParciales(r: RespuestaSiia, aviso: String = ""): ResultadoParciales =
    errorDeConsulta(r, "tus calificaciones")?.let { ResultadoParciales.Error(it) }
        ?: ResultadoParciales.Datos(parsearCalificacionesParciales(r.json!!, aviso))

internal fun interpretarFinales(r: RespuestaSiia, aviso: String = ""): ResultadoFinales =
    errorDeConsulta(r, "tus calificaciones finales")?.let { ResultadoFinales.Error(it) }
        ?: ResultadoFinales.Datos(parsearCalificacionesFinales(r.json!!, aviso))

internal fun parsearCalificacionesParciales(json: JSONObject, aviso: String = ""): CalificacionesParciales {
    val arr = json.optJSONArray("calificaciones") ?: JSONArray()
    val materias = List(arr.length()) { i ->
        val m = arr.getJSONObject(i)
        MateriaParcial(
            materia = m.optString("materia"),
            profesor = m.optString("profesor"),
            grupo = m.optString("grupo"),
            parcial1 = textoCalificacion(m, "cal1"),
            parcial2 = textoCalificacion(m, "cal2"),
            parcial3 = textoCalificacion(m, "cal3"),
            promedio = textoCalificacion(m, "promedioParcial"),
            estatus = estatusDe(m.optString("estatus"))
        )
    }
    val resumen = json.optJSONObject("resumen")?.let { r ->
        ResumenParcial(
            totalMaterias = r.optInt("totalMaterias"),
            evaluadas = r.optInt("materiasEvaluadas"),
            aprobadas = r.optInt("materiasAprobadas"),
            reprobadas = r.optInt("materiasReprobadas"),
            promedio = textoCalificacion(r, "promedioParcial")
        )
    }
    val bloqueado = json.optBoolean("bloqueado")
    return CalificacionesParciales(
        periodo = json.optString("periodo"),
        periodosDisponibles = cadenas(json.optJSONArray("periodosDisponibles")),
        bloqueado = bloqueado,
        mensajeBloqueo = json.mensajeSiia()
            ?: if (bloqueado) "Tus calificaciones están bloqueadas en el SIIA por trámites pendientes." else "",
        pendientes = pendientesDe(json),
        aviso = aviso.trim(),
        resumen = if (bloqueado) null else resumen,
        materias = if (bloqueado) emptyList() else materias
    )
}

/**
 * Finales del periodo. La v1.2 del SIIA repite cada dato con varios nombres
 * (`primeraVuelta` = `ordinario` = `calPromedio`) y parciales.ashx trae lo mismo
 * dentro de `calificaciones`/`resumen`: se acepta cualquiera de ellos.
 */
internal fun parsearCalificacionesFinales(json: JSONObject, aviso: String = ""): CalificacionesFinales {
    val arr = json.optJSONArray("finales")?.takeIf { it.length() > 0 } ?: json.optJSONArray("calificaciones") ?: JSONArray()
    val materias = List(arr.length()) { i ->
        val m = arr.getJSONObject(i)
        MateriaFinal(
            materia = m.optString("materia"),
            profesor = m.optString("profesor"),
            grupo = m.optString("grupo"),
            primeraVuelta = primeraCalificacion(m, "primeraVuelta", "ordinario", "calPromedio"),
            segundaVuelta = primeraCalificacion(m, "segundaVuelta", "extraordinario", "segVuelta"),
            calificacionFinal = primeraCalificacion(m, "calFinal", "final"),
            estatus = estatusDe(m.optString("estatusFinal").ifBlank { m.optString("estatus") })
        )
    }
    val resumenFinal = json.optJSONObject("resumenFinal")
    val resumen = if (resumenFinal != null) {
        ResumenFinal(
            totalMaterias = resumenFinal.optInt("totalMaterias"),
            evaluadas = resumenFinal.optInt("materiasEvaluadas"),
            aprobadas = resumenFinal.optInt("materiasAprobadas"),
            reprobadas = resumenFinal.optInt("materiasReprobadas"),
            promedio = primeraCalificacion(resumenFinal, "promedioFinal", "promedio"),
            dictamen = resumenFinal.optString("estatus").trim()
        )
    } else json.optJSONObject("resumen")?.let { r ->
        ResumenFinal(
            totalMaterias = r.optInt("totalMaterias"),
            evaluadas = r.optInt("materiasEvaluadasFinal"),
            aprobadas = r.optInt("materiasAprobadasFinal"),
            reprobadas = r.optInt("materiasReprobadasFinal"),
            promedio = textoCalificacion(r, "promedioFinal"),
            dictamen = r.optString("estatusFinal").trim()
        )
    }
    val bloqueado = json.optBoolean("bloqueado")
    return CalificacionesFinales(
        periodo = json.optString("periodo"),
        periodosDisponibles = cadenas(json.optJSONArray("periodosDisponibles")),
        bloqueado = bloqueado,
        mensajeBloqueo = json.mensajeSiia()
            ?: if (bloqueado) "Tus calificaciones están bloqueadas en el SIIA por trámites pendientes." else "",
        pendientes = pendientesDe(json),
        aviso = aviso.trim(),
        resumen = if (bloqueado) null else resumen,
        materias = if (bloqueado) emptyList() else materias
    )
}

private fun estatusDe(texto: String): EstatusParcial = when (texto.trim().lowercase()) {
    "aprobatoria" -> EstatusParcial.APROBATORIA
    "reprobatoria" -> EstatusParcial.REPROBATORIA
    else -> EstatusParcial.SIN_CALIFICAR
}

/** Trámites que bloquean la consulta. El SIIA los manda en verificaciones.pendientes; el formato del backend los traía arriba. */
internal fun pendientesDe(json: JSONObject): List<String> =
    cadenas(json.optJSONObject("verificaciones")?.optJSONArray("pendientes") ?: json.optJSONArray("pendientes"))

/** Usa el texto que ya formatea el SIIA ("90", "NP"…); si no viene, el valor del campo; si tampoco, "—". */
internal fun textoCalificacion(o: JSONObject, campo: String): String {
    val texto = o.optString("${campo}Str").trim()
    if (texto.isNotEmpty() && texto != "null" && texto != "-") return texto
    if (o.isNull(campo) || !o.has(campo)) return "—"
    val valor = o.optDouble(campo)
    if (valor.isNaN()) return o.optString(campo).trim().takeIf { it.isNotEmpty() && it != "-" } ?: "—"
    return if (valor % 1.0 == 0.0) valor.toInt().toString() else "%.1f".format(java.util.Locale.US, valor)
}

/** La primera calificación con dato entre varios nombres del mismo campo. */
private fun primeraCalificacion(o: JSONObject, vararg campos: String): String =
    campos.map { textoCalificacion(o, it) }.firstOrNull { it != "—" } ?: "—"

private fun cadenas(arr: JSONArray?): List<String> =
    if (arr == null) emptyList() else List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
