package com.example.appteschi.viewmodel

import com.example.appteschi.service.KardexPdf
import com.example.appteschi.service.ResultadoKardex
import com.example.appteschi.service.ResultadoKardexPdf
import com.example.appteschi.testutil.FakeKardexSiiaApi
import com.example.appteschi.testutil.kardexDePrueba
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class KardexViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val pdf = KardexPdf("Kardex_2099000001.pdf", "%PDF-prueba".toByteArray())

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(api: FakeKardexSiiaApi = FakeKardexSiiaApi(resultadoPdf = ResultadoKardexPdf.Archivo(pdf))) =
        KardexViewModel(api, dispatcher).also { dispatcher.scheduler.advanceUntilIdle() }

    @Test
    fun `al abrir consulta el kardex`() = runTest {
        val api = FakeKardexSiiaApi()
        val e = vm(api).uiState.value
        assertEquals(1, api.consultas)
        assertFalse(e.cargando)
        assertNull(e.error)
        assertEquals(kardexDePrueba(), e.kardex)
    }

    @Test
    fun `si falla muestra el error y reintentar vuelve a consultar`() = runTest {
        val api = FakeKardexSiiaApi(resultado = ResultadoKardex.Error("El SIIA no pudo."))
        val vm = vm(api)
        assertEquals("El SIIA no pudo.", vm.uiState.value.error)

        api.resultado = ResultadoKardex.Datos(kardexDePrueba())
        vm.reintentar()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, api.consultas)
        assertNull(vm.uiState.value.error)
        assertEquals(kardexDePrueba(), vm.uiState.value.kardex)
    }

    @Test
    fun `el filtro guarda el semestre y null vuelve a todos`() = runTest {
        val vm = vm()
        vm.filtrarSemestre(2)
        assertEquals(2, vm.uiState.value.semestreFiltro)
        vm.filtrarSemestre(null)
        assertNull(vm.uiState.value.semestreFiltro)
    }

    @Test
    fun `descargar pide destino y guardar escribe el PDF`() = runTest {
        val vm = vm()
        vm.descargarPdf()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Kardex_2099000001.pdf", vm.uiState.value.pedirDestinoPdf)
        assertTrue(vm.uiState.value.descargandoPdf)

        vm.destinoPedido()
        assertNull(vm.uiState.value.pedirDestinoPdf)

        val destino = ByteArrayOutputStream()
        vm.guardarPdf(destino)
        dispatcher.scheduler.advanceUntilIdle()

        assertArrayEquals(pdf.contenido, destino.toByteArray())
        assertTrue(vm.uiState.value.pdfGuardado)
        assertFalse(vm.uiState.value.descargandoPdf)
        assertNull(vm.uiState.value.errorPdf)

        vm.avisoPdfMostrado()
        assertFalse(vm.uiState.value.pdfGuardado)
    }

    @Test
    fun `un error del SIIA al descargar no pide destino`() = runTest {
        val vm = vm(FakeKardexSiiaApi(resultadoPdf = ResultadoKardexPdf.Error("Sin PDF.")))
        vm.descargarPdf()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.uiState.value.pedirDestinoPdf)
        assertFalse(vm.uiState.value.descargandoPdf)
        assertEquals("Sin PDF.", vm.uiState.value.errorPdf)
    }

    @Test
    fun `cancelar el selector no marca error ni guardado`() = runTest {
        val vm = vm()
        vm.descargarPdf()
        dispatcher.scheduler.advanceUntilIdle()
        vm.destinoPedido()
        vm.cancelarGuardado()
        val e = vm.uiState.value
        assertFalse(e.descargandoPdf)
        assertFalse(e.pdfGuardado)
        assertNull(e.errorPdf)
    }

    @Test
    fun `si no se puede abrir el destino o falla la escritura avisa`() = runTest {
        val vm = vm()
        vm.descargarPdf()
        dispatcher.scheduler.advanceUntilIdle()
        vm.guardarPdf(null)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(MENSAJE_PDF_NO_GUARDADO, vm.uiState.value.errorPdf)
        assertFalse(vm.uiState.value.descargandoPdf)

        vm.descargarPdf()
        dispatcher.scheduler.advanceUntilIdle()
        vm.guardarPdf(object : OutputStream() {
            override fun write(b: Int) = throw IOException("disco lleno")
        })
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(MENSAJE_PDF_NO_GUARDADO, vm.uiState.value.errorPdf)
        assertFalse(vm.uiState.value.pdfGuardado)
    }

    @Test
    fun `no se piden dos descargas a la vez`() = runTest {
        val api = FakeKardexSiiaApi(resultadoPdf = ResultadoKardexPdf.Archivo(pdf))
        val vm = vm(api)
        vm.descargarPdf()
        vm.descargarPdf()
        dispatcher.scheduler.advanceUntilIdle()
        vm.descargarPdf()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, api.descargas)
    }
}
