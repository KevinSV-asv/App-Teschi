package com.example.appteschi.viewmodel

import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.PlanDeEstudiosIsc
import com.example.appteschi.data.UserSession
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
        UserSession.limpiar()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        UserSession.limpiar()
    }

    @Test
    fun `arranca cargando y resuelve con una materia por cada una del plan de estudios`() = runTest {
        val viewModel = KardexViewModel()
        assertTrue(viewModel.uiState.value.cargando)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertTrue(!estado.cargando)
        assertEquals(PlanDeEstudiosIsc.materias.size, estado.materias.size)
    }

    @Test
    fun `los totales se derivan de la lista de materias, no estan hardcodeados aparte`() = runTest {
        val viewModel = KardexViewModel()
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
        val viewModel = KardexViewModel()
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
    fun `el promedio del ultimo semestre - 2026-1 - coincide con el Kardex oficial - 99,29`() = runTest {
        val viewModel = KardexViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        val estado = viewModel.uiState.value

        assertEquals(99.29, estado.promedioUltimoSemestre, 0.01)
    }

    @Test
    fun `el porcentaje cubierto coincide con creditos cursados sobre el total`() = runTest {
        val viewModel = KardexViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        val estado = viewModel.uiState.value

        val esperado = (estado.totalCreditosCursados.toDouble() / estado.totalCreditosCarrera) * 100
        assertEquals(esperado, estado.porcentajeCubierto, 0.01)
    }
}
