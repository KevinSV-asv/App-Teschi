package com.example.appteschi.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

private const val CARPETA_FOTOS = "fotos_perfil"
private const val LADO_FOTO = 512
private const val LADO_MAXIMO_AL_LEER = 4096

/**
 * Foto de perfil del alumno (DEC-048). La API del SIIA no permite subir fotos:
 * solo existe en este teléfono, separada por cuenta y fuera de los respaldos
 * (res/xml/backup_rules.xml, data_extraction_rules.xml).
 */
interface FotoPerfilStore {
    /** Ruta de la foto de [cuenta], o null si no tiene. */
    fun ruta(cuenta: String): String?

    /** Guarda la imagen como foto de [cuenta]. Devuelve su ruta, o null si no se pudo usar esa imagen. */
    suspend fun guardar(cuenta: String, abrirOrigen: () -> InputStream?): String?

    suspend fun quitar(cuenta: String)
}

/**
 * La que usa la app: un JPEG cuadrado en el almacenamiento privado. Como se vuelve
 * a dibujar, no conserva los metadatos del original (como la ubicación GPS).
 */
object FotoPerfilArchivos : FotoPerfilStore {
    private var carpeta: File? = null

    /** Debe llamarse una vez al arrancar la app (ver MainActivity.onCreate). */
    fun init(context: Context) {
        carpeta = File(context.applicationContext.filesDir, CARPETA_FOTOS)
    }

    private fun archivo(cuenta: String): File? =
        if (cuenta.isBlank()) null else carpeta?.let { File(it, nombreArchivoFoto(cuenta)) }

    override fun ruta(cuenta: String): String? = archivo(cuenta)?.takeIf(File::exists)?.path

    override suspend fun guardar(cuenta: String, abrirOrigen: () -> InputStream?): String? = withContext(Dispatchers.IO) {
        val destino = archivo(cuenta) ?: return@withContext null
        val temporal = File(destino.path + ".tmp")
        try {
            val foto = fotoCuadrada(abrirOrigen) ?: return@withContext null
            destino.parentFile?.mkdirs()
            val escrita = temporal.outputStream().use { foto.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            foto.recycle()
            // Se reemplaza hasta tener la nueva completa: si algo falla, se queda la anterior.
            if (escrita && temporal.renameTo(destino)) destino.path else null
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            temporal.delete()
        }
    }

    override suspend fun quitar(cuenta: String) {
        withContext(Dispatchers.IO) { archivo(cuenta)?.delete() }
    }

    private fun fotoCuadrada(abrirOrigen: () -> InputStream?): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (abrirOrigen() ?: return null).use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = muestreoFoto(limites.outWidth, limites.outHeight)
        }
        val original = (abrirOrigen() ?: return null).use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null
        val orientacion = abrirOrigen()?.use { origen ->
            runCatching {
                ExifInterface(origen).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }.getOrNull()
        } ?: ExifInterface.ORIENTATION_NORMAL
        val giro = giroExif(orientacion)

        // Fondo blanco para las imágenes transparentes. Lo que no cabe en el cuadro se recorta, centrado.
        val salida = Bitmap.createBitmap(LADO_FOTO, LADO_FOTO, Bitmap.Config.ARGB_8888)
        val escala = LADO_FOTO.toFloat() / min(original.width, original.height)
        val matriz = Matrix().apply {
            postTranslate(-original.width / 2f, -original.height / 2f)
            if (giro.espejo) postScale(-1f, 1f)
            postRotate(giro.grados.toFloat())
            postScale(escala, escala)
            postTranslate(LADO_FOTO / 2f, LADO_FOTO / 2f)
        }
        Canvas(salida).apply {
            drawColor(Color.WHITE)
            drawBitmap(original, matriz, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        original.recycle()
        return salida
    }
}

/** Mayor reducción (potencia de 2) que deja el lado corto en [LADO_FOTO] o más y limita el largo a [LADO_MAXIMO_AL_LEER]. */
internal fun muestreoFoto(ancho: Int, alto: Int): Int {
    var muestreo = 1
    while (min(ancho, alto) / (muestreo * 2) >= LADO_FOTO) muestreo *= 2
    while (max(ancho, alto) / muestreo > LADO_MAXIMO_AL_LEER) muestreo *= 2
    return muestreo
}

/** La matrícula no queda en el nombre del archivo y no hay forma de que se salga de la carpeta. */
internal fun nombreArchivoFoto(cuenta: String): String =
    MessageDigest.getInstance("SHA-256").digest(cuenta.toByteArray())
        .joinToString("") { "%02x".format(it) } + ".jpg"

internal data class GiroExif(val grados: Int, val espejo: Boolean)

/** Cómo enderezar la foto según la orientación que guardó la cámara. */
internal fun giroExif(orientacion: Int): GiroExif = when (orientacion) {
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> GiroExif(0, true)
    ExifInterface.ORIENTATION_ROTATE_180 -> GiroExif(180, false)
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> GiroExif(180, true)
    ExifInterface.ORIENTATION_TRANSPOSE -> GiroExif(90, true)
    ExifInterface.ORIENTATION_ROTATE_90 -> GiroExif(90, false)
    ExifInterface.ORIENTATION_TRANSVERSE -> GiroExif(270, true)
    ExifInterface.ORIENTATION_ROTATE_270 -> GiroExif(270, false)
    else -> GiroExif(0, false)
}
