package com.example.appteschi.service

import com.example.appteschi.core.config.SiiaEndpoints
import com.example.appteschi.data.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.ByteString.Companion.decodeBase64
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/** [clave] es la clave oficial de cursada del kardex. */
enum class EstatusKardex(val clave: String, val etiqueta: String) {
    APROBADA("AP", "Aprobada"),
    NO_APROBADA("NA", "No aprobó"),
    POR_CURSAR("PC", "Por cursar")
}

/** Un intento en la materia: Ordinario, Recurse, Especial o Global. */
data class OportunidadKardex(val tipo: String, val calificacion: String, val periodo: String)

data class MateriaKardex(
    val clave: String,
    val nombre: String,
    val creditos: String,
    val calificacion: String,
    val estatus: EstatusKardex,
    val oportunidades: List<OportunidadKardex>,
    /** Número de la materia en el reporte; null si no viene. */
    val numero: Int? = null
)

data class SemestreKardex(
    val numero: Int,
    val nombre: String,
    val promedio: String,
    /** Para la gráfica; null si el semestre aún no tiene promedio numérico. */
    val promedioNumero: Double?,
    val materias: List<MateriaKardex>
)

/** Los mismos datos del historial académico oficial (DEC-045). Solo viven en memoria: no se guardan en el teléfono. */
data class DatosKardex(
    val matricula: String,
    val nombre: String,
    val curp: String,
    val carrera: String,
    val especialidad: String,
    val planEstudios: String,
    val situacion: String,
    val grupo: String,
    val periodoIngreso: String,
    val ultimoPeriodo: String,
    val promedioGlobal: String,
    val promedioLetra: String,
    val promedioSemestreAnterior: String,
    /** Parcial y descuento del reporte: vacíos si el SIIA no los da. */
    val parcial: String,
    val descuento: String,
    val creditosCursados: String,
    val creditosCarrera: String,
    val creditosPorCursar: String,
    val porcentaje: String,
    /** Avance de 0 a 1 para el anillo; null si el SIIA no da con qué calcularlo. */
    val avance: Float?,
    val totalMaterias: Int,
    val aprobadas: Int,
    val reprobadas: Int,
    val porAprobar: Int,
    val porCursar: Int
)

data class Kardex(
    val bloqueado: Boolean,
    val mensajeBloqueo: String,
    val pendientes: List<String>,
    val datos: DatosKardex?,
    val semestres: List<SemestreKardex>,
    /** Clave y significado ("AP" → "Aprobada"), en el orden en que se muestran. */
    val simbologia: List<Pair<String, String>>,
    /** Día de la consulta ("02-11-2026"), como la fecha del reporte oficial. */
    val fechaElaboracion: String = ""
)

sealed class ResultadoKardex {
    data class Datos(val kardex: Kardex) : ResultadoKardex()
    data class Error(val mensaje: String) : ResultadoKardex()
}

/** PDF oficial ya validado. No es data class: su contenido es un arreglo de bytes. */
class KardexPdf(val nombreArchivo: String, val contenido: ByteArray)

sealed class ResultadoKardexPdf {
    data class Archivo(val pdf: KardexPdf) : ResultadoKardexPdf()
    data class Error(val mensaje: String) : ResultadoKardexPdf()
}

interface KardexSiiaApi {
    suspend fun kardex(): ResultadoKardex
    suspend fun kardexPdf(): ResultadoKardexPdf
}

/**
 * `GET calificaciones/kardex.ashx` y `kardex-pdf.ashx` directo al SIIA con el
 * token del alumno (DEC-044). Un alumno solo puede ver su propio kardex: el SIIA
 * toma la matrícula del token, así que aquí nunca se manda `matricula`.
 */
class KardexSiiaHttp(
    private val endpoints: SiiaEndpoints = SiiaEndpoints(),
    client: OkHttpClient = SesionAlumno.client,
    private val token: () -> String? = { UserSession.token },
    private val hoy: () -> Calendar = { Calendar.getInstance() }
) : KardexSiiaApi {

    // El kardex completo y el reporte de SSRS tardan más que una consulta normal.
    private val cliente = client.newBuilder().readTimeout(60, TimeUnit.SECONDS).build()

    override suspend fun kardex(): ResultadoKardex = withContext(Dispatchers.IO) {
        val tokenActual = token()
        if (tokenActual.isNullOrBlank()) return@withContext ResultadoKardex.Error(MENSAJE_SESION_EXPIRADA)
        try {
            interpretarKardex(consultar(endpoints.kardex, tokenActual), fechaElaboracion(hoy()))
        } catch (e: IOException) {
            ResultadoKardex.Error(e.message ?: "No se pudo conectar con el SIIA.")
        }
    }

    override suspend fun kardexPdf(): ResultadoKardexPdf = withContext(Dispatchers.IO) {
        val tokenActual = token()
        if (tokenActual.isNullOrBlank()) return@withContext ResultadoKardexPdf.Error(MENSAJE_SESION_EXPIRADA)
        try {
            interpretarKardexPdf(consultar(endpoints.kardexPdf, tokenActual))
        } catch (e: IOException) {
            ResultadoKardexPdf.Error(e.message ?: "No se pudo conectar con el SIIA.")
        }
    }

    private fun consultar(url: String, token: String): RespuestaSiia {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
        return cliente.newCall(request).execute().use { RespuestaSiia(it.code, jsonONulo(it.body?.string())) }
    }
}

/** La que usa la app. */
object KardexSiiaService : KardexSiiaApi by KardexSiiaHttp()

internal fun interpretarKardex(r: RespuestaSiia, fecha: String = ""): ResultadoKardex =
    errorDeConsulta(r, "tu kardex")?.let { ResultadoKardex.Error(it) }
        ?: ResultadoKardex.Datos(parsearKardex(r.json!!, fecha))

/** Mismo formato que el reporte oficial: día-mes-año. */
internal fun fechaElaboracion(fecha: Calendar): String = "%02d-%02d-%04d".format(
    Locale.US, fecha.get(Calendar.DAY_OF_MONTH), fecha.get(Calendar.MONTH) + 1, fecha.get(Calendar.YEAR)
)

internal fun interpretarKardexPdf(r: RespuestaSiia): ResultadoKardexPdf {
    errorDeConsulta(r, "tu kardex en PDF")?.let { return ResultadoKardexPdf.Error(it) }
    val json = r.json!!
    if (json.optBoolean("bloqueado")) {
        return ResultadoKardexPdf.Error(json.mensajeSiia() ?: "Tu kardex está bloqueado en el SIIA por trámites pendientes.")
    }
    val bytes = json.optString("base64").substringAfter("base64,").decodeBase64()?.toByteArray()
    if (bytes == null || !esPdf(bytes)) {
        return ResultadoKardexPdf.Error("El SIIA no entregó un PDF válido. Intenta más tarde.")
    }
    return ResultadoKardexPdf.Archivo(KardexPdf(nombreArchivoSeguro(json.optString("fileName")), bytes))
}

private fun esPdf(bytes: ByteArray): Boolean =
    bytes.size >= 4 && String(bytes, 0, 4, Charsets.US_ASCII) == "%PDF"

/** El nombre sale del SIIA y se le propone al alumno al guardar: solo letras, números, punto, guion y guion bajo. */
internal fun nombreArchivoSeguro(nombre: String): String {
    val limpio = nombre.substringAfterLast('/').substringAfterLast('\\').filter { it.isLetterOrDigit() || it in "._-" }
    return if (limpio.length > 4 && limpio.lowercase(Locale.ROOT).endsWith(".pdf")) limpio else "Kardex.pdf"
}

/**
 * El SIIA repite cada dato con varios nombres (`creditosCarrera` =
 * `totalCreditosCarrera`, `kardex` = `materias`, `datosPersonales` = `resumen`):
 * se acepta cualquiera de ellos.
 */
internal fun parsearKardex(json: JSONObject, fecha: String = ""): Kardex {
    val bloqueado = json.optBoolean("bloqueado")
    val mensajeBloqueo = json.mensajeSiia()
        ?: if (bloqueado) "Tu kardex está bloqueado en el SIIA por trámites pendientes." else ""
    if (bloqueado) {
        return Kardex(true, mensajeBloqueo, pendientesDe(json), datos = null, semestres = emptyList(), simbologia = emptyList(), fechaElaboracion = fecha)
    }
    val semestres = semestresDe(json)
    val materias = semestres.flatMap { it.materias }
    val datos = (json.optJSONObject("datosPersonales") ?: json.optJSONObject("resumen"))?.let { datosDe(it, materias) }
    return Kardex(false, mensajeBloqueo, pendientesDe(json), datos, semestres, simbologiaDe(json.optJSONObject("simbologia")), fecha)
}

private fun datosDe(d: JSONObject, materias: List<MateriaKardex>): DatosKardex {
    val cursados = numero(d, "creditosCursados")
    val carrera = numero(d, "creditosCarrera", "totalCreditosCarrera", "creditosCarerra")
    val porcentaje = numero(d, "porcentajeCubierto", "porcentaje")
    val avance = when {
        cursados != null && carrera != null && carrera > 0 -> (cursados / carrera).toFloat()
        porcentaje != null -> (porcentaje / 100).toFloat()
        else -> null
    }?.coerceIn(0f, 1f)
    return DatosKardex(
        matricula = texto(d, "matricula"),
        nombre = texto(d, "alumno", "nombre"),
        curp = texto(d, "curp"),
        carrera = texto(d, "carrera"),
        especialidad = texto(d, "especialidad"),
        planEstudios = texto(d, "planEstudios"),
        situacion = texto(d, "situacion"),
        grupo = texto(d, "grupo"),
        periodoIngreso = texto(d, "periodoIngreso", "ingreso"),
        ultimoPeriodo = texto(d, "ultimoPeriodo", "perUltimo"),
        promedioGlobal = textoCalificacion(d, "promedioGlobal").takeIf { it != "—" } ?: textoCalificacion(d, "promedioFinal"),
        promedioLetra = texto(d, "promedioLetra"),
        promedioSemestreAnterior = textoCalificacion(d, "promedioSemestreAnterior"),
        parcial = textoCalificacion(d, "parcial").takeIf { it != "—" }.orEmpty(),
        descuento = texto(d, "descuento"),
        creditosCursados = cursados?.let(::numeroCorto) ?: "—",
        creditosCarrera = carrera?.let(::numeroCorto) ?: "—",
        creditosPorCursar = numero(d, "creditosPorCursar", "creditosFaltantes")?.let(::numeroCorto) ?: "—",
        porcentaje = texto(d, "porcentajeStr").ifEmpty {
            avance?.let { "%.1f %%".format(Locale.US, it * 100) }.orEmpty()
        },
        avance = avance,
        totalMaterias = entero(d, "totalMateriasCarrera", "totalMaterias") ?: materias.size,
        aprobadas = entero(d, "materiasAprobadas", "totalMateriasApro")
            ?: materias.count { it.estatus == EstatusKardex.APROBADA },
        reprobadas = entero(d, "materiasReprobadas", "totalMateriasRepro")
            ?: materias.count { it.estatus == EstatusKardex.NO_APROBADA },
        porAprobar = entero(d, "materiasPorAprobar")
            ?: materias.count { it.estatus != EstatusKardex.APROBADA },
        porCursar = entero(d, "materiasPorCursar")
            ?: materias.count { it.estatus == EstatusKardex.POR_CURSAR }
    )
}

private fun semestresDe(json: JSONObject): List<SemestreKardex> {
    val arr = json.optJSONArray("semestres")
    val conMaterias = if (arr == null) emptyList() else List(arr.length()) { i ->
        val s = arr.getJSONObject(i)
        semestre(
            numero = s.optInt("semestre"),
            nombre = texto(s, "nombreSemestre"),
            promedio = textoCalificacion(s, "promedioSemestre"),
            materias = materiasDe(s.optJSONArray("materias"))
        )
    }.filter { it.materias.isNotEmpty() }
    if (conMaterias.isNotEmpty()) return conMaterias

    // Sin `semestres`: se agrupa la lista plana por el semestre de cada materia.
    val plana = json.optJSONArray("kardex")?.takeIf { it.length() > 0 } ?: json.optJSONArray("materias") ?: JSONArray()
    return List(plana.length()) { i -> plana.getJSONObject(i) }
        .groupBy { it.optInt("semestre") }
        .toSortedMap()
        .map { (numero, grupo) ->
            semestre(numero, "", textoCalificacion(grupo.first(), "promedioSemestre"), grupo.map(::materiaDe))
        }
}

private fun semestre(numero: Int, nombre: String, promedio: String, materias: List<MateriaKardex>) = SemestreKardex(
    numero = numero,
    nombre = nombre.ifEmpty { if (numero > 0) "Semestre $numero" else "Sin semestre" },
    promedio = promedio,
    promedioNumero = promedio.toDoubleOrNull(),
    materias = materias
)

private fun materiasDe(arr: JSONArray?): List<MateriaKardex> =
    if (arr == null) emptyList() else List(arr.length()) { materiaDe(arr.getJSONObject(it)) }

private fun materiaDe(m: JSONObject): MateriaKardex {
    val oportunidades = listOf(
        Triple("Ordinario", "calOrd", "periodoOrd"),
        Triple("Recurse", "calRec", "periodoRec"),
        Triple("Especial", "calEspecial", "periodoEsp"),
        Triple("Global", "calGlobal", "periodoGlo")
    ).mapNotNull { (tipo, campoCal, campoPeriodo) ->
        val calificacion = textoCalificacion(m, campoCal)
        val periodo = texto(m, campoPeriodo)
        if (calificacion == "—" && periodo.isEmpty()) null else OportunidadKardex(tipo, calificacion, periodo)
    }
    return MateriaKardex(
        clave = texto(m, "clave"),
        nombre = texto(m, "nombre", "materia"),
        creditos = textoCalificacion(m, "creditos"),
        calificacion = textoCalificacion(m, "calificacion").takeIf { it != "—" }
            ?: oportunidades.lastOrNull()?.calificacion ?: "—",
        estatus = estatusKardex(m),
        oportunidades = oportunidades,
        numero = entero(m, "num", "numero")
    )
}

/** `cursada` es la clave oficial (AP/NA/PC); `estatus` trae acento ("No Aprobó"), por eso solo se revisa cómo empieza. */
private fun estatusKardex(m: JSONObject): EstatusKardex = when (m.optString("cursada").trim().uppercase(Locale.ROOT)) {
    "AP" -> EstatusKardex.APROBADA
    "NA" -> EstatusKardex.NO_APROBADA
    "PC" -> EstatusKardex.POR_CURSAR
    else -> {
        val estatus = m.optString("estatus").trim().lowercase(Locale.ROOT)
        when {
            estatus.startsWith("aprob") || m.optBoolean("aprobada") -> EstatusKardex.APROBADA
            estatus.startsWith("no") -> EstatusKardex.NO_APROBADA
            else -> EstatusKardex.POR_CURSAR
        }
    }
}

private fun simbologiaDe(s: JSONObject?): List<Pair<String, String>> {
    if (s == null) return emptyList()
    val conocidas = listOf("AP", "NA", "PC", "AAA", "ACA")
    val otras = s.keys().asSequence().filter { it !in conocidas }.toList().sorted()
    return (conocidas + otras).mapNotNull { clave ->
        texto(s, clave).takeIf { it.isNotEmpty() }?.let { clave to it }
    }
}

/** El primer texto con dato entre varios nombres del mismo campo; vacío si ninguno trae. */
private fun texto(o: JSONObject, vararg campos: String): String =
    campos.firstNotNullOfOrNull { campo ->
        if (o.isNull(campo)) null else o.optString(campo).trim().takeIf { it.isNotEmpty() && it != "null" }
    }.orEmpty()

private fun numero(o: JSONObject, vararg campos: String): Double? =
    campos.firstNotNullOfOrNull { campo -> if (o.isNull(campo)) null else o.optDouble(campo).takeIf { !it.isNaN() } }

private fun entero(o: JSONObject, vararg campos: String): Int? = numero(o, *campos)?.toInt()

private fun numeroCorto(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toInt().toString() else "%.1f".format(Locale.US, valor)
