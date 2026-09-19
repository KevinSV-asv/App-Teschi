package com.example.appteschi.viewmodel

import com.example.appteschi.data.EstatusReinscripcion
import com.example.appteschi.data.EstatusReinscripcionInfo
import com.example.appteschi.data.GrupoReinscripcion
import com.example.appteschi.data.UserSession
import com.example.appteschi.service.ReinscripcionApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Fake sin red real — el mismo patrón que testutil/AuthFakes.kt. */
class FakeReinscripcionApi(
    private val resultadoEstatus: Result<EstatusReinscripcionInfo> =
        Result.failure(IllegalStateException("no configurado")),
    private val resultadoSolicitud: Result<String> = Result.success("RE-00000000")
) : ReinscripcionApi {
    var llamadasEstatus = 0
        private set
    var llamadasSolicitud = 0
        private set
    var ultimoGrupoEnviado: String? = null
        private set

    override suspend fun estatus(matricula: String): Result<EstatusReinscripcionInfo> {
        llamadasEstatus++
        return resultadoEstatus
    }

    override suspend fun enviarSolicitud(matricula: String, claveGrupo: String): Result<String> {
        llamadasSolicitud++
        ultimoGrupoEnviado = claveGrupo
        return resultadoSolicitud
    }
}

private val grupoIrregular = GrupoReinscripcion(20, "9ISC23", 9, "Vespertino", 3, "2026-2")

private fun infoIrregular() = EstatusReinscripcionInfo(
    matricula = "2099000001",
    nombreCompleto = "Alumno de Prueba Uno",
    semestre = 9,
    carrera = "Ingeniería en Sistemas Computacionales",
    estatus = EstatusReinscripcion.IRREGULAR,
    motivo = null,
    materiasPendientes = listOf(
        com.example.appteschi.data.MateriaPendiente(1, "Cálculo Diferencial", 5, 1)
    ),
    grupoAsignado = null,
    grupos = listOf(grupoIrregular)
)

private fun infoRegular() = EstatusReinscripcionInfo(
    matricula = "2099000001",
    nombreCompleto = "Alumno de Prueba Uno",
    semestre = 9,
    carrera = "Ingeniería en Sistemas Computacionales",
    estatus = EstatusReinscripcion.REGULAR,
    motivo = null,
    materiasPendientes = emptyList(),
    grupoAsignado = grupoIrregular,
    grupos = emptyList()
)

private fun infoBloqueado(motivo: String) = EstatusReinscripcionInfo(
    matricula = "2099000001",
    nombreCompleto = "Alumno de Prueba Uno",
    semestre = 9,
    carrera = "Ingeniería en Sistemas Computacionales",
    estatus = EstatusReinscripcion.BLOQUEADO,
    motivo = motivo,
    materiasPendientes = emptyList(),
    grupoAsignado = null,
    grupos = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReinscripcionViewModelTest {

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

    @Test
    fun `arranca en LOADING y pasa a IRREGULAR_ELEGIBLE con los grupos reales del backend`() = runTest {
        val api = FakeReinscripcionApi(resultadoEstatus = Result.success(infoIrregular()))
        val viewModel = ReinscripcionViewModel(api)

        assertEquals(ReinscripcionEstado.LOADING, viewModel.uiState.value.estado)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(ReinscripcionEstado.IRREGULAR_ELEGIBLE, estado.estado)
        assertEquals(listOf(grupoIrregular), estado.gruposDisponibles)
        assertEquals(1, api.llamadasEstatus)
    }

    @Test
    fun `alumno regular recibe el grupo asignado sin tener que elegir`() = runTest {
        val api = FakeReinscripcionApi(resultadoEstatus = Result.success(infoRegular()))
        val viewModel = ReinscripcionViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(ReinscripcionEstado.REGULAR, estado.estado)
        assertEquals("9ISC23", estado.grupoAsignado)
    }

    @Test
    fun `alumno con observacion de reglamento pendiente queda bloqueado con el motivo real`() = runTest {
        val api = FakeReinscripcionApi(resultadoEstatus = Result.success(infoBloqueado("Falta administrativa pendiente de resolver")))
        val viewModel = ReinscripcionViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(ReinscripcionEstado.BLOQUEADO, estado.estado)
        assertEquals("Falta administrativa pendiente de resolver", estado.motivoBloqueo)
    }

    @Test
    fun `si la consulta de estatus falla, el estado pasa a ERROR con el mensaje real`() = runTest {
        val api = FakeReinscripcionApi(resultadoEstatus = Result.failure(IllegalStateException("No existe un alumno con esa matrícula")))
        val viewModel = ReinscripcionViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(ReinscripcionEstado.ERROR, estado.estado)
        assertEquals("No existe un alumno con esa matrícula", estado.error)
    }

    @Test
    fun `enviar sin grupo seleccionado rechaza sin llamar al backend`() = runTest {
        val api = FakeReinscripcionApi(resultadoEstatus = Result.success(infoIrregular()))
        val viewModel = ReinscripcionViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.enviar()

        val estado = viewModel.uiState.value
        assertEquals(ReinscripcionEstado.IRREGULAR_ELEGIBLE, estado.estado)
        assertEquals("Selecciona un grupo.", estado.error)
        assertEquals(0, api.llamadasSolicitud)
    }

    @Test
    fun `envio completo guarda la solicitud real y devuelve el folio del backend`() = runTest {
        val api = FakeReinscripcionApi(
            resultadoEstatus = Result.success(infoIrregular()),
            resultadoSolicitud = Result.success("RE-ABC12345")
        )
        val viewModel = ReinscripcionViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.actualizarGrupo("9ISC23")
        viewModel.enviar()

        assertEquals(ReinscripcionEstado.SENDING, viewModel.uiState.value.estado)
        assertNull(viewModel.uiState.value.error)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(ReinscripcionEstado.SUCCESS, estado.estado)
        assertEquals("RE-ABC12345", estado.folio)
        assertEquals("9ISC23", api.ultimoGrupoEnviado)
    }
}
