package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.service.Kardex
import com.example.appteschi.service.KardexPdf
import com.example.appteschi.service.KardexSiiaApi
import com.example.appteschi.service.KardexSiiaService
import com.example.appteschi.service.ResultadoKardex
import com.example.appteschi.service.ResultadoKardexPdf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream

data class KardexUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val kardex: Kardex? = null,
    /** Semestre elegido en el filtro; null = todos. */
    val semestreFiltro: Int? = null,
    val descargandoPdf: Boolean = false,
    /** Nombre propuesto cuando el PDF ya llegó y falta que el alumno elija dónde guardarlo. */
    val pedirDestinoPdf: String? = null,
    val pdfGuardado: Boolean = false,
    val errorPdf: String? = null
)

/** Kardex del SIIA (DEC-044): historial completo y descarga del PDF oficial. */
class KardexViewModel(
    private val siia: KardexSiiaApi = KardexSiiaService,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(KardexUiState())
    val uiState: StateFlow<KardexUiState> = _uiState.asStateFlow()

    // Fuera del estado para no compararlo ni conservarlo más de lo necesario.
    private var pdfPendiente: KardexPdf? = null

    init {
        cargar()
    }

    fun reintentar() = cargar()

    fun filtrarSemestre(numero: Int?) = _uiState.update { it.copy(semestreFiltro = numero) }

    private fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            when (val r = siia.kardex()) {
                is ResultadoKardex.Datos -> _uiState.update { it.copy(cargando = false, kardex = r.kardex) }
                is ResultadoKardex.Error -> _uiState.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    fun descargarPdf() {
        if (_uiState.value.descargandoPdf) return
        _uiState.update { it.copy(descargandoPdf = true, errorPdf = null, pdfGuardado = false) }
        viewModelScope.launch {
            when (val r = siia.kardexPdf()) {
                is ResultadoKardexPdf.Archivo -> {
                    pdfPendiente = r.pdf
                    _uiState.update { it.copy(pedirDestinoPdf = r.pdf.nombreArchivo) }
                }
                is ResultadoKardexPdf.Error -> _uiState.update { it.copy(descargandoPdf = false, errorPdf = r.mensaje) }
            }
        }
    }

    /** La pantalla ya abrió el selector de archivo: evita abrirlo otra vez si se recrea (p. ej. al girar el teléfono). */
    fun destinoPedido() = _uiState.update { it.copy(pedirDestinoPdf = null) }

    /** El alumno canceló el selector, o no se pudo abrir ([error] dice por qué). */
    fun cancelarGuardado(error: String? = null) {
        pdfPendiente = null
        _uiState.update { it.copy(descargandoPdf = false, pedirDestinoPdf = null, errorPdf = error) }
    }

    /** Escribe el PDF donde eligió el alumno; [destino] null = no se pudo abrir ese lugar. */
    fun guardarPdf(destino: OutputStream?) {
        val pdf = pdfPendiente
        pdfPendiente = null
        if (pdf == null || destino == null) {
            _uiState.update { it.copy(descargandoPdf = false, errorPdf = MENSAJE_PDF_NO_GUARDADO) }
            return
        }
        viewModelScope.launch {
            val guardado = withContext(io) {
                try {
                    destino.use { it.write(pdf.contenido) }
                    true
                } catch (_: IOException) {
                    false
                }
            }
            _uiState.update {
                it.copy(descargandoPdf = false, pdfGuardado = guardado, errorPdf = if (guardado) null else MENSAJE_PDF_NO_GUARDADO)
            }
        }
    }

    /** La pantalla ya mostró el resultado de la descarga. */
    fun avisoPdfMostrado() = _uiState.update { it.copy(pdfGuardado = false, errorPdf = null) }
}

internal const val MENSAJE_PDF_NO_GUARDADO = "No se pudo guardar el PDF en ese lugar. Intenta de nuevo."
