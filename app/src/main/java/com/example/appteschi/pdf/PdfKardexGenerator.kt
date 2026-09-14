package com.example.appteschi.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.appteschi.viewmodel.KardexUiState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Genera el PDF del Historial Académico (Kardex) replicando la estructura,
 * colores y tablas del formato oficial de Control Escolar del TESCHI
 * (encabezado con logos, panel "Datos de la carrera" + Promedio Global,
 * tabla de materias paginada), con los datos del alumno en sesión.
 *
 * El formateo de texto/números vive en KardexPdfFormato (unidad probable con
 * pruebas JVM normales); este objeto solo dibuja con Canvas/PdfDocument, que
 * requiere el runtime de Android y por eso no se prueba con JUnit puro.
 */
object PdfKardexGenerator {

    private const val ANCHO_PAGINA = 612f // Carta a 72pt/in
    private const val ALTO_PAGINA = 792f
    private const val MARGEN = 28f

    private val VERDE_OSCURO = Color.parseColor("#1E5631")
    private val VERDE_CLARO = Color.parseColor("#4C9A2A")
    private val VERDE_FILA = Color.parseColor("#E8F5E9")
    private val GRIS_BORDE = Color.parseColor("#9E9E9E")
    private val NEGRO = Color.parseColor("#1A2E22")
    private val GRIS_TEXTO = Color.parseColor("#5A5A5A")

    // N°, Materia, Créditos, Semestre, Calif.Eval, Periodo.Eval, Calif.Rep, Periodo.Rep, Calif.Esp, Periodo.Esp, Cursada
    private val anchosColumnas = floatArrayOf(20f, 190f, 34f, 34f, 36f, 40f, 36f, 40f, 36f, 40f, 42f)
    private const val ALTO_FILA = 12.2f
    private const val ALTO_ENCABEZADO_TABLA = 22f

    fun generar(context: Context, uiState: KardexUiState, matricula: String, nombre: String): Uri {
        val fecha = SimpleDateFormat("dd-MM-yyyy", Locale("es", "MX")).format(java.util.Date())
        val encabezado = KardexPdfFormato.encabezado(uiState, matricula, nombre, fecha)
        val filas = KardexPdfFormato.filas(uiState.materias)

        val logoGobierno = bitmapDeRecurso(context, "logo_gobierno_edomex")
        val logoTeschi = bitmapDeRecurso(context, "logo_teschi")

        val documento = PdfDocument()
        val anchoTabla = anchosColumnas.sum()
        val xTabla = (ANCHO_PAGINA - anchoTabla) / 2f

        var indiceFila = 0
        var numeroPagina = 1
        val totalPaginas = calcularTotalPaginas(filas.size)

        do {
            val pageInfo = PdfDocument.PageInfo.Builder(ANCHO_PAGINA.toInt(), ALTO_PAGINA.toInt(), numeroPagina).create()
            val page = documento.startPage(pageInfo)
            val canvas = page.canvas

            var y = dibujarEncabezado(canvas, encabezado, logoGobierno, logoTeschi)
            y = dibujarInfoAlumno(canvas, encabezado, y)
            y = dibujarPanelResumen(canvas, encabezado, y)
            y = dibujarEncabezadoTabla(canvas, xTabla, y)

            val esUltimaPagina = numeroPagina == totalPaginas
            val yMaximaFilas = ALTO_PAGINA - MARGEN - if (esUltimaPagina) 46f else 16f

            while (indiceFila < filas.size && y + ALTO_FILA <= yMaximaFilas) {
                dibujarFila(canvas, filas[indiceFila], xTabla, y)
                y += ALTO_FILA
                indiceFila++
            }

            dibujarPie(canvas, numeroPagina, totalPaginas, esUltimaPagina)

            documento.finishPage(page)
            numeroPagina++
        } while (indiceFila < filas.size)

        val uri = guardarYObtenerUri(context, documento, "Kardex_$matricula.pdf")
        documento.close()
        return uri
    }

    private fun calcularTotalPaginas(totalFilas: Int): Int {
        // Aproximación: misma capacidad de filas por página que la primera
        // (la altura del encabezado/resumen es igual en todas las páginas).
        val capacidadPrimeraPagina = ((ALTO_PAGINA - MARGEN - 16f - alturaHastaTabla()) / ALTO_FILA).toInt()
        if (capacidadPrimeraPagina <= 0) return 1
        return maxOf(1, Math.ceil(totalFilas.toDouble() / capacidadPrimeraPagina).toInt())
    }

    private fun alturaHastaTabla(): Float = 190f // encabezado + info + resumen + encabezado de tabla (estimado, ver dibujarEncabezado/dibujarInfoAlumno/dibujarPanelResumen)

    private fun bitmapDeRecurso(context: Context, nombre: String): Bitmap? {
        val id = context.resources.getIdentifier(nombre, "drawable", context.packageName)
        if (id == 0) return null
        return BitmapFactory.decodeResource(context.resources, id)
    }

    private fun dibujarEncabezado(
        canvas: Canvas,
        encabezado: EncabezadoKardexPdf,
        logoGobierno: Bitmap?,
        logoTeschi: Bitmap?
    ): Float {
        var y = MARGEN

        logoGobierno?.let {
            val alto = 46f
            val ancho = alto * it.width / it.height
            canvas.drawBitmap(it, null, RectF(MARGEN, y, MARGEN + ancho, y + alto), null)
        }
        logoTeschi?.let {
            val alto = 34f
            val ancho = alto * it.width / it.height
            canvas.drawBitmap(
                it, null,
                RectF(ANCHO_PAGINA - MARGEN - ancho, y, ANCHO_PAGINA - MARGEN, y + alto),
                null
            )
        }

        val centroX = ANCHO_PAGINA / 2f
        val tituloPaint = paintTexto(9.5f, NEGRO, negrita = true, alineacion = Paint.Align.CENTER)
        val subPaint = paintTexto(6.3f, NEGRO, alineacion = Paint.Align.CENTER)
        val seccionPaint = paintTexto(7.3f, NEGRO, negrita = true, alineacion = Paint.Align.CENTER)

        canvas.drawText("TECNOLÓGICO DE ESTUDIOS SUPERIORES DE CHIMALHUACÁN", centroX, y + 9f, tituloPaint)
        canvas.drawText("Organismo Público Descentralizado del Gobierno del Estado de México", centroX, y + 18f, subPaint)
        canvas.drawText("DIRECCIÓN ACADÉMICA", centroX, y + 28f, seccionPaint)
        canvas.drawText("DEPARTAMENTO DE CONTROL ESCOLAR", centroX, y + 37f, seccionPaint)
        canvas.drawText("HISTORIAL ACADÉMICO", centroX, y + 46f, seccionPaint)

        val fechaLabelPaint = paintTexto(6.3f, GRIS_TEXTO, alineacion = Paint.Align.RIGHT)
        val fechaValorPaint = paintTexto(7f, NEGRO, negrita = true, alineacion = Paint.Align.RIGHT)
        canvas.drawText("Fecha de elaboración:", ANCHO_PAGINA - MARGEN, y + 44f, fechaLabelPaint)
        canvas.drawText(encabezado.fechaElaboracion, ANCHO_PAGINA - MARGEN, y + 53f, fechaValorPaint)

        y += 58f
        val lineaPaint = Paint().apply { color = GRIS_BORDE; strokeWidth = 0.75f }
        canvas.drawLine(MARGEN, y, ANCHO_PAGINA - MARGEN, y, lineaPaint)
        return y + 10f
    }

    private fun dibujarInfoAlumno(canvas: Canvas, encabezado: EncabezadoKardexPdf, yInicial: Float): Float {
        var y = yInicial
        val labelPaint = paintTexto(7.2f, GRIS_TEXTO)
        val valorPaint = paintTexto(7.6f, NEGRO, negrita = true)

        canvas.drawText("Carrera:", MARGEN, y, labelPaint)
        canvas.drawText(encabezado.carrera, MARGEN + 42f, y, valorPaint)
        y += 12f

        canvas.drawText("Matrícula:", MARGEN, y, labelPaint)
        canvas.drawText(encabezado.matricula, MARGEN + 42f, y, valorPaint)
        canvas.drawText("Nombre:", MARGEN + 160f, y, labelPaint)
        canvas.drawText(encabezado.nombre, MARGEN + 200f, y, valorPaint)
        y += 12f

        canvas.drawText("Período de ingreso:", MARGEN, y, labelPaint)
        canvas.drawText(encabezado.periodoIngreso, MARGEN + 72f, y, valorPaint)
        canvas.drawText("Último período:", MARGEN + 160f, y, labelPaint)
        canvas.drawText(encabezado.ultimoPeriodo, MARGEN + 228f, y, valorPaint)
        y += 14f

        return y
    }

    private fun dibujarPanelResumen(canvas: Canvas, encabezado: EncabezadoKardexPdf, yInicial: Float): Float {
        val x = MARGEN
        val ancho = ANCHO_PAGINA - 2 * MARGEN
        val anchoPromedio = 90f
        val anchoDatos = ancho - anchoPromedio
        val anchoCol = anchoDatos / 3f

        var y = yInicial
        val altoTitulo = 12f
        val altoSubtitulo = 11f
        val altoCuerpo = 44f
        val altoTotal = altoTitulo + altoSubtitulo + altoCuerpo

        val fondoVerde = Paint().apply { color = VERDE_OSCURO; style = Paint.Style.FILL }
        val bordePaint = Paint().apply { color = GRIS_BORDE; style = Paint.Style.STROKE; strokeWidth = 0.75f }
        val tituloPaint = paintTexto(7f, Color.WHITE, negrita = true, alineacion = Paint.Align.CENTER)

        // Fila de título: "DATOS DE LA CARRERA" (col 1-3) + "Promedio Global" (col 4)
        canvas.drawRect(x, y, x + anchoDatos, y + altoTitulo, fondoVerde)
        canvas.drawText("DATOS DE LA CARRERA", x + anchoDatos / 2f, y + altoTitulo - 3.5f, tituloPaint)
        canvas.drawRect(x + anchoDatos, y, x + ancho, y + altoTitulo, fondoVerde)
        canvas.drawText("Promedio Global", x + anchoDatos + anchoPromedio / 2f, y + altoTitulo - 3.5f, tituloPaint)
        y += altoTitulo

        // Subtítulos de columna
        canvas.drawRect(x, y, x + anchoCol, y + altoSubtitulo, fondoVerde)
        canvas.drawText("Total de créditos", x + anchoCol / 2f, y + altoSubtitulo - 3f, tituloPaint)
        canvas.drawRect(x + anchoCol, y, x + anchoCol * 2, y + altoSubtitulo, fondoVerde)
        canvas.drawText("Porcentaje de créditos", x + anchoCol * 1.5f, y + altoSubtitulo - 3f, tituloPaint)
        canvas.drawRect(x + anchoCol * 2, y, x + anchoDatos, y + altoSubtitulo, fondoVerde)
        canvas.drawText("Materias", x + anchoCol * 2.5f, y + altoSubtitulo - 3f, tituloPaint)
        y += altoSubtitulo

        val yCuerpoInicio = y
        val labelPaint = paintTexto(6.3f, NEGRO)
        val valorPaint = paintTexto(6.3f, NEGRO, negrita = true)
        val lineas = 4
        val altoLinea = altoCuerpo / lineas

        fun dibujarDato(colX: Float, indiceLinea: Int, etiqueta: String, valor: String) {
            val yLinea = yCuerpoInicio + altoLinea * indiceLinea + altoLinea - 3f
            canvas.drawText(etiqueta, colX + 4f, yLinea, labelPaint)
            canvas.drawText(valor, colX + anchoCol - 4f, yLinea, valorPaint.apply { textAlign = Paint.Align.RIGHT })
        }

        dibujarDato(x, 0, "Total de créditos de la carrera:", encabezado.totalCreditosCarrera)
        dibujarDato(x, 1, "Total de créditos cursados:", encabezado.totalCreditosCursados)
        dibujarDato(x, 2, "Promedio del último semestre:", encabezado.promedioUltimoSemestre)

        dibujarDato(x + anchoCol, 0, "Créditos por cursar:", encabezado.creditosPorCursar)
        val fondoClaro = Paint().apply { color = VERDE_CLARO; style = Paint.Style.FILL }
        canvas.drawRect(
            x + anchoCol + 4f, yCuerpoInicio + altoLinea + 2f,
            x + anchoCol * 2 - 4f, yCuerpoInicio + altoLinea * 3 - 2f,
            fondoClaro
        )
        val porcentajePaint = paintTexto(7f, Color.WHITE, negrita = true, alineacion = Paint.Align.CENTER)
        canvas.drawText(
            "Porcentaje cubierto: ${encabezado.porcentajeCubierto}",
            x + anchoCol * 1.5f,
            yCuerpoInicio + altoLinea * 2 + 2f,
            porcentajePaint
        )

        dibujarDato(x + anchoCol * 2, 0, "Total de Materias:", encabezado.totalMaterias)
        dibujarDato(x + anchoCol * 2, 1, "Materias Aprobadas:", encabezado.materiasAprobadas)
        dibujarDato(x + anchoCol * 2, 2, "Materias Reprobadas:", encabezado.materiasReprobadas)
        dibujarDato(x + anchoCol * 2, 3, "Materias por Aprobar:", encabezado.materiasPorAprobar)

        // Promedio global — número grande centrado en su propia columna
        val promedioPaint = paintTexto(22f, VERDE_OSCURO, negrita = true, alineacion = Paint.Align.CENTER)
        canvas.drawText(
            encabezado.promedioGlobal,
            x + anchoDatos + anchoPromedio / 2f,
            yCuerpoInicio + altoCuerpo / 2f + 8f,
            promedioPaint
        )

        // Bordes de la tabla completa
        canvas.drawRect(x, yInicial, x + ancho, yInicial + altoTotal, bordePaint)
        canvas.drawLine(x + anchoDatos, yInicial, x + anchoDatos, yInicial + altoTotal, bordePaint)
        canvas.drawLine(x + anchoCol, yCuerpoInicio, x + anchoCol, yCuerpoInicio + altoCuerpo, bordePaint)
        canvas.drawLine(x + anchoCol * 2, yInicial + altoTitulo, x + anchoCol * 2, yInicial + altoTotal, bordePaint)

        return yInicial + altoTotal + 8f
    }

    private val encabezadosTabla = listOf(
        "N°" to "", "Materia" to "", "Créditos" to "", "Semestre" to "",
        "Calificación" to "Evaluación", "Periodo" to "Curso",
        "Calificación" to "Curso Repetición", "Periodo" to "Curso",
        "Calificación" to "Curso Especial", "Periodo" to "Curso",
        "Cursada" to ""
    )

    private fun dibujarEncabezadoTabla(canvas: Canvas, xTabla: Float, yInicial: Float): Float {
        val fondoVerde = Paint().apply { color = VERDE_OSCURO; style = Paint.Style.FILL }
        val bordePaint = Paint().apply { color = GRIS_BORDE; style = Paint.Style.STROKE; strokeWidth = 0.5f }
        val textoPaint = paintTexto(5.6f, Color.WHITE, negrita = true, alineacion = Paint.Align.CENTER)

        var x = xTabla
        val altoSuperior = ALTO_ENCABEZADO_TABLA * 0.4f
        val altoInferior = ALTO_ENCABEZADO_TABLA - altoSuperior

        canvas.drawRect(xTabla, yInicial, xTabla + anchosColumnas.sum(), yInicial + ALTO_ENCABEZADO_TABLA, fondoVerde)

        // Grupos superiores: Evaluación / Curso Repetición / Curso Especial
        val grupoEvalX = xTabla + anchosColumnas.take(4).sum()
        canvas.drawText("Evaluación", grupoEvalX + (anchosColumnas[4] + anchosColumnas[5]) / 2f, yInicial + altoSuperior - 2.5f, textoPaint)
        val grupoRepX = grupoEvalX + anchosColumnas[4] + anchosColumnas[5]
        canvas.drawText("Curso Repetición", grupoRepX + (anchosColumnas[6] + anchosColumnas[7]) / 2f, yInicial + altoSuperior - 2.5f, textoPaint)
        val grupoEspX = grupoRepX + anchosColumnas[6] + anchosColumnas[7]
        canvas.drawText("Curso Especial", grupoEspX + (anchosColumnas[8] + anchosColumnas[9]) / 2f, yInicial + altoSuperior - 2.5f, textoPaint)

        val nombresColumna = listOf("N°", "Materia", "Créditos", "Semestre", "Calificación", "Periodo", "Calificación", "Periodo", "Calificación", "Periodo", "Cursada")
        x = xTabla
        for (i in anchosColumnas.indices) {
            val yTexto = if (i < 4 || i == 10) yInicial + ALTO_ENCABEZADO_TABLA / 2f + 2f else yInicial + altoSuperior + altoInferior - 2.5f
            canvas.drawText(nombresColumna[i], x + anchosColumnas[i] / 2f, yTexto, textoPaint)
            x += anchosColumnas[i]
        }

        canvas.drawRect(xTabla, yInicial, xTabla + anchosColumnas.sum(), yInicial + ALTO_ENCABEZADO_TABLA, bordePaint)
        x = xTabla
        for (ancho in anchosColumnas) {
            canvas.drawLine(x, yInicial, x, yInicial + ALTO_ENCABEZADO_TABLA, bordePaint)
            x += ancho
        }
        canvas.drawLine(xTabla, yInicial + altoSuperior, xTabla + anchosColumnas.take(4).sum(), yInicial + altoSuperior, Paint().apply { color = Color.TRANSPARENT })

        return yInicial + ALTO_ENCABEZADO_TABLA
    }

    private fun dibujarFila(canvas: Canvas, fila: FilaKardexPdf, xTabla: Float, y: Float) {
        val bordePaint = Paint().apply { color = GRIS_BORDE; style = Paint.Style.STROKE; strokeWidth = 0.4f }
        val textoPaint = paintTexto(5.8f, NEGRO, alineacion = Paint.Align.CENTER)
        val textoMateriaPaint = paintTexto(5.8f, NEGRO, alineacion = Paint.Align.LEFT)

        canvas.drawRect(xTabla, y, xTabla + anchosColumnas.sum(), y + ALTO_FILA, bordePaint)

        var x = xTabla
        val valores = listOf(
            fila.numero.toString(), fila.materia, fila.creditos, fila.semestre,
            fila.calificacionEvaluacion, fila.periodoEvaluacion, "", "", "", "", fila.cursada
        )
        for (i in anchosColumnas.indices) {
            canvas.drawLine(x, y, x, y + ALTO_FILA, bordePaint)
            val yTexto = y + ALTO_FILA - 3f
            if (i == 1) {
                dibujarTextoAjustado(canvas, valores[i], x + 3f, yTexto, anchosColumnas[i] - 5f, textoMateriaPaint)
            } else {
                canvas.drawText(valores[i], x + anchosColumnas[i] / 2f, yTexto, textoPaint)
            }
            x += anchosColumnas[i]
        }
    }

    /** Reduce el tamaño de letra hasta que el texto quepa en el ancho disponible, sin cortar información real. */
    private fun dibujarTextoAjustado(canvas: Canvas, texto: String, x: Float, y: Float, anchoDisponible: Float, paintBase: Paint) {
        val paint = Paint(paintBase)
        while (paint.measureText(texto) > anchoDisponible && paint.textSize > 3.5f) {
            paint.textSize -= 0.3f
        }
        canvas.drawText(texto, x, y, paint)
    }

    private fun dibujarPie(canvas: Canvas, pagina: Int, totalPaginas: Int, esUltimaPagina: Boolean) {
        val paginaPaint = paintTexto(6.5f, GRIS_TEXTO, alineacion = Paint.Align.RIGHT)
        canvas.drawText("Página $pagina de $totalPaginas", ANCHO_PAGINA - MARGEN, ALTO_PAGINA - MARGEN + 8f, paginaPaint)

        if (esUltimaPagina) {
            val leyendaPaint = paintTexto(6.3f, NEGRO, alineacion = Paint.Align.CENTER)
            val centroX = ANCHO_PAGINA / 2f
            var y = ALTO_PAGINA - MARGEN - 26f
            canvas.drawText("Cursada  AP.- Aprobada", centroX, y, leyendaPaint)
            y += 8f
            canvas.drawText("PC.- Por cursar", centroX, y, leyendaPaint)
            y += 8f
            canvas.drawText("NA.- No Aprobó", centroX, y, leyendaPaint)
        }
    }

    private fun paintTexto(
        tamano: Float,
        color: Int,
        negrita: Boolean = false,
        alineacion: Paint.Align = Paint.Align.LEFT
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tamano
        this.color = color
        typeface = if (negrita) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textAlign = alineacion
    }

    private fun guardarYObtenerUri(context: Context, documento: PdfDocument, nombreArchivo: String): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val valores = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
                ?: throw IllegalStateException("No se pudo crear el archivo en Descargas")
            resolver.openOutputStream(uri)?.use { salida -> documento.writeTo(salida) }
            return uri
        }

        val carpeta = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        if (!carpeta.exists()) carpeta.mkdirs()
        val archivo = File(carpeta, nombreArchivo)
        FileOutputStream(archivo).use { salida -> documento.writeTo(salida) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    }
}
