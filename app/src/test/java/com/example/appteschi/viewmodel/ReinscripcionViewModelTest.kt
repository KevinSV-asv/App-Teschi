package com.example.appteschi.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * ReinscripcionViewModel es 100% mock (sin red real, sin Dispatchers.IO) —
 * a diferencia de RegistroViewModel, aquí sí basta con StandardTestDispatcher
 * + advanceUntilIdle(), todo corre en el mismo reloj virtual.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReinscripcionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `arranca en LOADING y pasa a IRREGULAR_ELEGIBLE tras la consulta simulada`() = runTest {
        val viewModel = ReinscripcionViewModel()

        assertEquals(ReinscripcionEstado.LOADING, viewModel.uiState.value.estado)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ReinscripcionEstado.IRREGULAR_ELEGIBLE, viewModel.uiState.value.estado)
    }

    @Test
    fun `enviar sin grupo rechaza sin pasar a SENDING`() = runTest {
        val viewModel = ReinscripcionViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.enviar()

        val estado = viewModel.uiState.value
        assertEquals(ReinscripcionEstado.IRREGULAR_ELEGIBLE, estado.estado)
        assertEquals("El grupo es obligatorio.", estado.error)
    }

    @Test
    fun `envio completo genera folio y limpia el error`() = runTest {
        val viewModel = ReinscripcionViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.actualizarGrupo("9ISC23")
        viewModel.enviar()

        // Justo al enviar, antes de que termine el retraso simulado.
        assertEquals(ReinscripcionEstado.SENDING, viewModel.uiState.value.estado)
        assertNull(viewModel.uiState.value.error)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(ReinscripcionEstado.SUCCESS, estado.estado)
        assertNotNull(estado.folio)
        assert(estado.folio.startsWith("RE-"))
    }
}
