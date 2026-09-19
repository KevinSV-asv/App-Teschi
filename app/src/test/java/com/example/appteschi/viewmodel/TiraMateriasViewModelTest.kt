package com.example.appteschi.viewmodel

import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.GrupoInfo
import com.example.appteschi.data.PlanDeEstudiosIsc
import com.example.appteschi.data.UserSession
import com.example.appteschi.testutil.FakeHistorialAcademicoApi
import com.example.appteschi.testutil.FakePlanEstudiosApi
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

private val gruposIscFake = listOf(
    GrupoInfo("3ISC11", 3, "Matutino", 1, "2026-2"),
    GrupoInfo("9ISC23", 9, "Vespertino", 3, "2026-2")
)

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

    private fun planEstudios() = FakePlanEstudiosApi(resultadoGrupos = Result.success(gruposIscFake))

    @Test
    fun `muestra exactamente las materias por cursar del semestre actual`() = runTest {
        UserSession.establecer(matricula = "2099000001")
        val viewModel = TiraMateriasViewModel(FakeHistorialAcademicoApi(Result.success(historialDeAlumnoDePrueba())), planEstudios())
        dispatcher.scheduler.advanceUntilIdle()

        val esperado = historialDeAlumnoDePrueba().materias
            .filter { it.estatus == EstatusMateria.POR_CURSAR && it.semestre == 9 }
        assertEquals(esperado, viewModel.uiState.value.materias)
    }

    @Test
    fun `hoy coincide con el comprobante real - solo Residencias Profesionales`() = runTest {
        UserSession.establecer(matricula = "2099000001")
        val viewModel = TiraMateriasViewModel(FakeHistorialAcademicoApi(Result.success(historialDeAlumnoDePrueba())), planEstudios())
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(1, estado.materias.size)
        assertEquals("Residencias Profesionales", estado.materias.first().nombre)
        assertEquals("9ISC23", estado.grupo)
    }

    @Test
    fun `ninguna materia de la tira esta ya aprobada`() = runTest {
        UserSession.establecer(matricula = "2099000001")
        val viewModel = TiraMateriasViewModel(FakeHistorialAcademicoApi(Result.success(historialDeAlumnoDePrueba())), planEstudios())
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.materias.none { it.estatus == EstatusMateria.APROBADA })
    }

    @Test
    fun `usuario de prueba sem3 ve la carga completa de tercer semestre, no solo una parte`() = runTest {
        UserSession.establecer(matricula = "sem3", esAdministrador = false, semestreSimulado = 3)
        val historial = FakeHistorialAcademicoApi()
        val viewModel = TiraMateriasViewModel(historial, planEstudios())
        dispatcher.scheduler.advanceUntilIdle()

        val esperadas = PlanDeEstudiosIsc.materias.count { it.semestre == 3 }
        val estado = viewModel.uiState.value
        assertEquals(esperadas, estado.materias.size)
        assertTrue(estado.materias.all { it.semestre == 3 && it.estatus == EstatusMateria.POR_CURSAR })
        assertEquals(0, historial.llamadas)
    }

    @Test
    fun `si la consulta de historial falla, el estado pasa a tener error`() = runTest {
        UserSession.establecer(matricula = "2099000001")
        val historial = FakeHistorialAcademicoApi(Result.failure(IllegalStateException("No existe un alumno con esa matrícula")))
        val viewModel = TiraMateriasViewModel(historial, planEstudios())
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertTrue(!estado.cargando)
        assertEquals("No existe un alumno con esa matrícula", estado.error)
    }
}
