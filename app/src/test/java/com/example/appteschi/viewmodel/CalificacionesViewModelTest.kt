package com.example.appteschi.viewmodel

import com.example.appteschi.service.ResultadoFinales
import com.example.appteschi.service.ResultadoParciales
import com.example.appteschi.testutil.FakeCalificacionesSiiaApi
import com.example.appteschi.testutil.finalesDePrueba
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
class CalificacionesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `al abrir pide el periodo actual y muestra las parciales`() = runTest {
        val api = FakeCalificacionesSiiaApi()
        val vm = CalificacionesViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf<String?>(null), api.periodosPedidos)
        val e = vm.uiState.value
        assertFalse(e.cargando)
        assertNull(e.error)
        assertEquals(parcialesDePrueba(), e.parciales)
    }

    @Test
    fun `cambiar de periodo consulta ese periodo`() = runTest {
        val api = FakeCalificacionesSiiaApi(porPeriodo = mapOf("2025-2" to ResultadoParciales.Datos(parcialesDePrueba("2025-2"))))
        val vm = CalificacionesViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()
        vm.seleccionarPeriodo("2025-2")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("2025-2", vm.uiState.value.parciales!!.periodo)
    }

    @Test
    fun `elegir el mismo periodo no vuelve a consultar`() = runTest {
        val api = FakeCalificacionesSiiaApi()
        val vm = CalificacionesViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()
        vm.seleccionarPeriodo("2026-2")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, api.periodosPedidos.size)
    }

    @Test
    fun `si falla otro periodo se conservan las parciales y se avisa`() = runTest {
        val api = FakeCalificacionesSiiaApi(porPeriodo = mapOf("2025-2" to ResultadoParciales.Error("El SIIA no respondió.")))
        val vm = CalificacionesViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()
        vm.seleccionarPeriodo("2025-2")
        dispatcher.scheduler.advanceUntilIdle()
        val e = vm.uiState.value
        assertEquals("2026-2", e.parciales!!.periodo)
        assertEquals("No se pudo cargar el periodo 2025-2. El SIIA no respondió.", e.errorPeriodo)
        assertNull(e.error)
    }

    @Test
    fun `si falla al abrir muestra el error con reintentar`() = runTest {
        val vm = CalificacionesViewModel(FakeCalificacionesSiiaApi(ResultadoParciales.Error("No se pudo conectar con el SIIA.")))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("No se pudo conectar con el SIIA.", vm.uiState.value.error)
        assertNull(vm.uiState.value.parciales)
    }

    @Test
    fun `bloqueado por tramites no es un error`() = runTest {
        val vm = CalificacionesViewModel(FakeCalificacionesSiiaApi(ResultadoParciales.Datos(parcialesBloqueadas())))
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.parciales!!.bloqueado)
    }

    private fun abierto(api: FakeCalificacionesSiiaApi) =
        CalificacionesViewModel(api).also { dispatcher.scheduler.advanceUntilIdle() }

    @Test
    fun `las finales se piden hasta abrir esa vista y con el periodo que se veia`() = runTest {
        val api = FakeCalificacionesSiiaApi()
        val vm = abierto(api)
        assertTrue(api.periodosFinalesPedidos.isEmpty())

        vm.seleccionarVista(VistaCalificaciones.FINALES)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf<String?>("2026-2"), api.periodosFinalesPedidos)
        assertEquals(VistaCalificaciones.FINALES, vm.uiState.value.vista)
        assertFalse(vm.uiState.value.cargandoFinales)
        assertEquals(finalesDePrueba(), vm.uiState.value.finales)
    }

    @Test
    fun `volver a finales con el mismo periodo no consulta otra vez`() = runTest {
        val api = FakeCalificacionesSiiaApi()
        val vm = abierto(api)
        vm.seleccionarVista(VistaCalificaciones.FINALES)
        dispatcher.scheduler.advanceUntilIdle()
        vm.seleccionarVista(VistaCalificaciones.PARCIALES)
        dispatcher.scheduler.advanceUntilIdle()
        vm.seleccionarVista(VistaCalificaciones.FINALES)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, api.periodosFinalesPedidos.size)
        assertEquals(1, api.periodosPedidos.size)
    }

    @Test
    fun `el periodo elegido en finales se conserva al volver a parciales`() = runTest {
        val api = FakeCalificacionesSiiaApi(
            porPeriodo = mapOf("2025-2" to ResultadoParciales.Datos(parcialesDePrueba("2025-2"))),
            finalesPorPeriodo = mapOf("2025-2" to ResultadoFinales.Datos(finalesDePrueba("2025-2")))
        )
        val vm = abierto(api)
        vm.seleccionarVista(VistaCalificaciones.FINALES)
        dispatcher.scheduler.advanceUntilIdle()
        vm.seleccionarPeriodo("2025-2")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("2025-2", vm.uiState.value.finales!!.periodo)
        assertEquals("2026-2", vm.uiState.value.parciales!!.periodo)

        vm.seleccionarVista(VistaCalificaciones.PARCIALES)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("2025-2", vm.uiState.value.parciales!!.periodo)
    }

    @Test
    fun `el periodo elegido en parciales se usa al abrir finales`() = runTest {
        val api = FakeCalificacionesSiiaApi(
            porPeriodo = mapOf("2025-2" to ResultadoParciales.Datos(parcialesDePrueba("2025-2"))),
            finalesPorPeriodo = mapOf("2025-2" to ResultadoFinales.Datos(finalesDePrueba("2025-2")))
        )
        val vm = abierto(api)
        vm.seleccionarPeriodo("2025-2")
        dispatcher.scheduler.advanceUntilIdle()
        vm.seleccionarVista(VistaCalificaciones.FINALES)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("2025-2", vm.uiState.value.finales!!.periodo)
    }

    @Test
    fun `si las parciales fallaron las finales se piden sin periodo`() = runTest {
        val api = FakeCalificacionesSiiaApi(resultado = ResultadoParciales.Error("Sin parciales."))
        val vm = abierto(api)
        vm.seleccionarVista(VistaCalificaciones.FINALES)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf<String?>(null), api.periodosFinalesPedidos)
        assertEquals(finalesDePrueba(), vm.uiState.value.finales)
        assertEquals("Sin parciales.", vm.uiState.value.error)
    }

    @Test
    fun `si las finales fallan se ve el error y reintentar vuelve a pedirlas`() = runTest {
        val api = FakeCalificacionesSiiaApi(resultadoFinales = ResultadoFinales.Error("Sin finales."))
        val vm = abierto(api)
        vm.seleccionarVista(VistaCalificaciones.FINALES)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Sin finales.", vm.uiState.value.errorFinales)

        vm.reintentar()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, api.periodosFinalesPedidos.size)
        assertEquals(1, api.periodosPedidos.size)
    }

    @Test
    fun `un periodo de finales que falla conserva lo que ya se veia`() = runTest {
        val api = FakeCalificacionesSiiaApi(
            finalesPorPeriodo = mapOf("2025-2" to ResultadoFinales.Error("Sin ese periodo."))
        )
        val vm = abierto(api)
        vm.seleccionarVista(VistaCalificaciones.FINALES)
        dispatcher.scheduler.advanceUntilIdle()
        vm.seleccionarPeriodo("2025-2")
        dispatcher.scheduler.advanceUntilIdle()
        val e = vm.uiState.value
        assertEquals("2026-2", e.finales!!.periodo)
        assertNull(e.errorFinales)
        assertEquals("No se pudo cargar el periodo 2025-2. Sin ese periodo.", e.errorPeriodoFinales)
    }
}
