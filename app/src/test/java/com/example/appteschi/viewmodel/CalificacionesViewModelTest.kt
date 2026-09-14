package com.example.appteschi.viewmodel

import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.HistorialAcademico
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
class CalificacionesViewModelTest {

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
    fun `muestra exactamente las materias del semestre actual, ni una mas ni una menos`() = runTest {
        val viewModel = CalificacionesViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val esperado = HistorialAcademico.materiasReales.filter { it.semestre == UserSession.semestreActual }
        assertEquals(esperado, viewModel.uiState.value.materias)
    }

    @Test
    fun `cada calificacion mostrada es identica a la que tiene esa misma materia en el Kardex`() = runTest {
        val viewModel = CalificacionesViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.value.materias.forEach { materiaEnCalificaciones ->
            val materiaEnKardex = HistorialAcademico.materiasReales.first { it.nombre == materiaEnCalificaciones.nombre }
            assertEquals(
                "La calificación de '${materiaEnCalificaciones.nombre}' no coincide con el Kardex",
                materiaEnKardex.calificacion,
                materiaEnCalificaciones.calificacion
            )
            assertEquals(materiaEnKardex.estatus, materiaEnCalificaciones.estatus)
        }
    }

    @Test
    fun `hoy Kevin esta en noveno y Residencias Profesionales aparece como pendiente, sin calificacion`() = runTest {
        val viewModel = CalificacionesViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(9, estado.semestreActual)

        val residencias = estado.materias.first { it.nombre == "Residencias Profesionales" }
        assertEquals(EstatusMateria.POR_CURSAR, residencias.estatus)
        assertEquals(null, residencias.calificacion)
    }

    @Test
    fun `hoy el noveno semestre tiene Actividades Complementarias y Residencias Profesionales, nada mas`() = runTest {
        val viewModel = CalificacionesViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val nombres = viewModel.uiState.value.materias.map { it.nombre }.toSet()
        assertEquals(setOf("Actividades Complementarias", "Residencias Profesionales"), nombres)
    }

    @Test
    fun `usuario de prueba sem3 ve todas las materias de tercer semestre, todas pendientes`() = runTest {
        UserSession.establecer(matricula = "sem3", esAdministrador = false, semestreSimulado = 3)
        val viewModel = CalificacionesViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(3, estado.semestreActual)
        val esperadas = com.example.appteschi.data.PlanDeEstudiosIsc.materias.count { it.semestre == 3 }
        assertEquals(esperadas, estado.materias.size)
        assertTrue(estado.materias.all { it.semestre == 3 })
        assertTrue(estado.materias.all { it.calificacion == null && it.estatus == EstatusMateria.POR_CURSAR })
    }
}
