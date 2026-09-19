package com.example.appteschi.viewmodel

import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.UserSession
import com.example.appteschi.testutil.FakeHistorialAcademicoApi
import com.example.appteschi.testutil.historialDeAlumnoDePrueba
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KardexViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        UserSession.establecer(matricula = "2099000001")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        UserSession.limpiar()
    }

    private fun api() = FakeHistorialAcademicoApi(Result.success(historialDeAlumnoDePrueba()))

    @Test
    fun `arranca cargando y resuelve con las 53 materias del historial real`() = runTest {
        val viewModel = KardexViewModel(api())
        assertTrue(viewModel.uiState.value.cargando)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertTrue(!estado.cargando)
        assertEquals(53, estado.materias.size)
    }

    @Test
    fun `los totales se derivan de la lista de materias, no estan hardcodeados aparte`() = runTest {
        val viewModel = KardexViewModel(api())
        dispatcher.scheduler.advanceUntilIdle()
        val estado = viewModel.uiState.value

        assertEquals(estado.materias.size, estado.totalMaterias)
        assertEquals(estado.materias.count { it.estatus == EstatusMateria.APROBADA }, estado.materiasAprobadas)
        assertEquals(estado.materias.count { it.estatus == EstatusMateria.POR_CURSAR }, estado.materiasPorAprobar)
        assertEquals(estado.materias.count { it.estatus == EstatusMateria.NO_APROBADA }, estado.materiasReprobadas)
        assertEquals(estado.materias.sumOf { it.creditos }, estado.totalCreditosCarrera)
    }

    @Test
    fun `coincide con el historial academico real al 07-09-2026 - 260 creditos, 250 cursados, Residencias por cursar`() = runTest {
        val viewModel = KardexViewModel(api())
        dispatcher.scheduler.advanceUntilIdle()
        val estado = viewModel.uiState.value

        assertEquals(53, estado.totalMaterias)
        assertEquals(52, estado.materiasAprobadas)
        assertEquals(0, estado.materiasReprobadas)
        assertEquals(1, estado.materiasPorAprobar)
        assertEquals(260, estado.totalCreditosCarrera)
        assertEquals(250, estado.totalCreditosCursados)

        val residencias = estado.materias.first { it.nombre == "Residencias Profesionales" }
        assertEquals(EstatusMateria.POR_CURSAR, residencias.estatus)
        assertEquals(9, residencias.semestre)
    }

    @Test
    fun `el promedio del semestre anterior al actual es el promedio de sus materias aprobadas`() = runTest {
        val viewModel = KardexViewModel(api())
        dispatcher.scheduler.advanceUntilIdle()
        val estado = viewModel.uiState.value

        val calificaciones = historialDeAlumnoDePrueba().materias
            .filter { it.semestre == 8 && it.estatus == EstatusMateria.APROBADA }
            .mapNotNull { it.calificacion }
        assertEquals(calificaciones.average(), estado.promedioUltimoSemestre, 0.01)
    }

    @Test
    fun `el porcentaje cubierto coincide con creditos cursados sobre el total`() = runTest {
        val viewModel = KardexViewModel(api())
        dispatcher.scheduler.advanceUntilIdle()
        val estado = viewModel.uiState.value

        val esperado = (estado.totalCreditosCursados.toDouble() / estado.totalCreditosCarrera) * 100
        assertEquals(esperado, estado.porcentajeCubierto, 0.01)
    }

    @Test
    fun `si la consulta falla, el estado pasa a tener error y no se queda cargando`() = runTest {
        val api = FakeHistorialAcademicoApi(Result.failure(IllegalStateException("No existe un alumno con esa matrícula")))
        val viewModel = KardexViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertTrue(!estado.cargando)
        assertEquals("No existe un alumno con esa matrícula", estado.error)
    }

    @Test
    fun `usuario de prueba sem3 no llama al backend - usa el historial simulado`() = runTest {
        UserSession.establecer(matricula = "sem3", semestreSimulado = 3)
        val api = FakeHistorialAcademicoApi()
        val viewModel = KardexViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, api.llamadas)
        assertTrue(!viewModel.uiState.value.cargando)
        assertEquals(null, viewModel.uiState.value.error)
    }
}
