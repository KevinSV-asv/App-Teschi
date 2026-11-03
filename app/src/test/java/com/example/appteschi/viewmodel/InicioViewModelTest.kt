package com.example.appteschi.viewmodel

import com.example.appteschi.data.UserSession
import com.example.appteschi.service.ResultadoParciales
import com.example.appteschi.testutil.FakeCalificacionesSiiaApi
import com.example.appteschi.testutil.cuentaDePrueba
import com.example.appteschi.testutil.parcialesBloqueadas
import com.example.appteschi.testutil.parcialesDePrueba
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        UserSession.establecer(cuentaDePrueba(), "token-de-prueba", 480)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        UserSession.limpiar()
    }

    @Test
    fun `saluda con el primer nombre y muestra los datos del SIIA`() = runTest {
        val vm = InicioViewModel(FakeCalificacionesSiiaApi())
        val e = vm.uiState.value
        assertEquals("Alumno", e.primerNombre)
        assertEquals("2099000001", e.matricula)
        assertEquals("INGENIERÍA EN SISTEMAS COMPUTACIONALES", e.carrera)
    }

    @Test
    fun `muestra el resumen del periodo actual`() = runTest {
        val vm = InicioViewModel(FakeCalificacionesSiiaApi())
        dispatcher.scheduler.advanceUntilIdle()
        val e = vm.uiState.value
        assertFalse(e.cargandoResumen)
        assertEquals("2026-2", e.periodo)
        assertEquals(parcialesDePrueba().resumen, e.resumen)
        assertNull(e.errorResumen)
    }

    @Test
    fun `si el SIIA bloquea la consulta lo indica sin error`() = runTest {
        val vm = InicioViewModel(FakeCalificacionesSiiaApi(ResultadoParciales.Datos(parcialesBloqueadas())))
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.bloqueado)
        assertNull(vm.uiState.value.errorResumen)
    }

    @Test
    fun `si el resumen falla el saludo se sigue viendo`() = runTest {
        val vm = InicioViewModel(FakeCalificacionesSiiaApi(ResultadoParciales.Error("No se pudo conectar con el SIIA.")))
        dispatcher.scheduler.advanceUntilIdle()
        val e = vm.uiState.value
        assertEquals("No se pudo conectar con el SIIA.", e.errorResumen)
        assertEquals("Alumno", e.primerNombre)
    }

    @Test
    fun `sin nombre saluda con la matricula`() {
        assertEquals("2099000001", InicioUiState(nombreCompleto = "", matricula = "2099000001").primerNombre)
    }
}
