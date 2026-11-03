package com.example.appteschi.viewmodel

import com.example.appteschi.service.MateriaParcial
import com.example.appteschi.service.EstatusParcial
import com.example.appteschi.service.ResultadoParciales
import com.example.appteschi.testutil.FakeCalificacionesSiiaApi
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TiraMateriasViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `arma la tira con las materias del periodo actual del SIIA`() = runTest {
        val api = FakeCalificacionesSiiaApi()
        val vm = TiraMateriasViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf<String?>(null), api.periodosPedidos)
        val e = vm.uiState.value
        assertEquals("2026-2", e.periodo)
        assertEquals(listOf("4901"), e.grupos)
        assertEquals(
            listOf(
                MateriaTira("Materia A", "PROFESOR UNO", "4901"),
                MateriaTira("Materia B", "PROFESORA DOS", "4901"),
                MateriaTira("Materia C", "", "4901")
            ),
            e.materias
        )
        assertNull(e.error)
    }

    @Test
    fun `con materias en grupos distintos muestra todos los grupos una vez`() = runTest {
        val c = parcialesDePrueba().copy(
            materias = parcialesDePrueba().materias + MateriaParcial("Materia D", "PROFESOR TRES", "4902", "—", "—", "—", "—", EstatusParcial.SIN_CALIFICAR)
        )
        val vm = TiraMateriasViewModel(FakeCalificacionesSiiaApi(ResultadoParciales.Datos(c)))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("4901", "4902"), vm.uiState.value.grupos)
    }

    @Test
    fun `bloqueado por tramites muestra los pendientes`() = runTest {
        val vm = TiraMateriasViewModel(FakeCalificacionesSiiaApi(ResultadoParciales.Datos(parcialesBloqueadas())))
        dispatcher.scheduler.advanceUntilIdle()
        val e = vm.uiState.value
        assertTrue(e.bloqueado)
        assertEquals(listOf("Actualizar datos personales"), e.pendientes)
        assertTrue(e.materias.isEmpty())
    }

    @Test
    fun `error del SIIA con reintentar`() = runTest {
        val api = FakeCalificacionesSiiaApi(ResultadoParciales.Error("No se pudo conectar con el SIIA."))
        val vm = TiraMateriasViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("No se pudo conectar con el SIIA.", vm.uiState.value.error)
        vm.reintentar()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, api.periodosPedidos.size)
    }
}
