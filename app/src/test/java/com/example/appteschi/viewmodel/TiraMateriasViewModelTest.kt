package com.example.appteschi.viewmodel

import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.HistorialAcademico
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
class TiraMateriasViewModelTest {

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
    fun `muestra exactamente las materias por cursar del semestre actual`() = runTest {
        val viewModel = TiraMateriasViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val esperado = HistorialAcademico.materiasReales
            .filter { it.estatus == EstatusMateria.POR_CURSAR && it.semestre == UserSession.semestreActual }
        assertEquals(esperado, viewModel.uiState.value.materias)
    }

    @Test
    fun `hoy coincide con el comprobante real - solo Residencias Profesionales`() = runTest {
        val viewModel = TiraMateriasViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(1, estado.materias.size)
        assertEquals("Residencias Profesionales", estado.materias.first().nombre)
        assertEquals("9ISC23", estado.grupo)
    }

    @Test
    fun `ninguna materia de la tira esta ya aprobada`() = runTest {
        val viewModel = TiraMateriasViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.materias.none { it.estatus == EstatusMateria.APROBADA })
    }

    @Test
    fun `usuario de prueba sem3 ve la carga completa de tercer semestre, no solo una parte`() = runTest {
        UserSession.establecer(matricula = "sem3", esAdministrador = false, semestreSimulado = 3)
        val viewModel = TiraMateriasViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val esperadas = PlanDeEstudiosIsc.materias.count { it.semestre == 3 }
        val estado = viewModel.uiState.value
        assertEquals(esperadas, estado.materias.size)
        assertTrue(estado.materias.all { it.semestre == 3 && it.estatus == EstatusMateria.POR_CURSAR })
    }
}
