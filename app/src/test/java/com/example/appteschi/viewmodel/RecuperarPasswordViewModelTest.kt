package com.example.appteschi.viewmodel

import com.example.appteschi.service.RecuperarPasswordApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Fake sin red real — el mismo patrón que testutil/AuthFakes.kt. */
class FakeRecuperarPasswordApi(
    private val resultadoSolicitar: Result<String> = Result.success("k***n@gmail.com"),
    private val resultadoConfirmar: Result<Unit> = Result.success(Unit)
) : RecuperarPasswordApi {
    var llamadasSolicitar = 0
        private set
    var llamadasConfirmar = 0
        private set
    var ultimaMatriculaSolicitada: String? = null
        private set

    override suspend fun solicitar(matricula: String): Result<String> {
        llamadasSolicitar++
        ultimaMatriculaSolicitada = matricula
        return resultadoSolicitar
    }

    override suspend fun confirmar(matricula: String, codigo: String, nuevaPassword: String): Result<Unit> {
        llamadasConfirmar++
        return resultadoConfirmar
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecuperarPasswordViewModelTest {

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
    fun `matricula en blanco rechaza sin llamar al backend`() = runTest {
        val api = FakeRecuperarPasswordApi()
        val vm = RecuperarPasswordViewModel(api)

        vm.solicitarCodigo("   ")

        assertEquals("Ingresa tu matrícula", vm.uiState.value.error)
        assertEquals(0, api.llamadasSolicitar)
    }

    @Test
    fun `solicitar exitoso pasa al paso de codigo con el correo enmascarado`() = runTest {
        val api = FakeRecuperarPasswordApi(resultadoSolicitar = Result.success("k***n@gmail.com"))
        val vm = RecuperarPasswordViewModel(api)

        vm.solicitarCodigo("2099000001")
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(RecuperarPasswordPaso.CODIGO_Y_NUEVA, estado.paso)
        assertEquals("k***n@gmail.com", estado.correoEnmascarado)
        assertEquals("2099000001", api.ultimaMatriculaSolicitada)
    }

    @Test
    fun `solicitar fallido muestra el error real del backend`() = runTest {
        val api = FakeRecuperarPasswordApi(
            resultadoSolicitar = Result.failure(IllegalStateException("Esa matrícula no tiene una cuenta propia registrada."))
        )
        val vm = RecuperarPasswordViewModel(api)

        vm.solicitarCodigo("2099000001")
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(RecuperarPasswordPaso.MATRICULA, estado.paso)
        assertEquals("Esa matrícula no tiene una cuenta propia registrada.", estado.error)
    }

    @Test
    fun `codigo incompleto rechaza sin llamar a confirmar`() = runTest {
        val api = FakeRecuperarPasswordApi()
        val vm = RecuperarPasswordViewModel(api)
        vm.solicitarCodigo("2099000001")
        dispatcher.scheduler.advanceUntilIdle()

        vm.confirmar("123", "ClaveNueva1", "ClaveNueva1")

        assertEquals("Ingresa los 6 dígitos del código.", vm.uiState.value.error)
        assertEquals(0, api.llamadasConfirmar)
    }

    @Test
    fun `contrasenas que no coinciden rechazan sin llamar a confirmar`() = runTest {
        val api = FakeRecuperarPasswordApi()
        val vm = RecuperarPasswordViewModel(api)
        vm.solicitarCodigo("2099000001")
        dispatcher.scheduler.advanceUntilIdle()

        vm.confirmar("123456", "ClaveNueva1", "OtraClave2")

        assertEquals("Las contraseñas no coinciden.", vm.uiState.value.error)
        assertEquals(0, api.llamadasConfirmar)
    }

    @Test
    fun `contrasena debil rechaza sin llamar a confirmar`() = runTest {
        val api = FakeRecuperarPasswordApi()
        val vm = RecuperarPasswordViewModel(api)
        vm.solicitarCodigo("2099000001")
        dispatcher.scheduler.advanceUntilIdle()

        vm.confirmar("123456", "debil", "debil")

        assertEquals(
            "La contraseña requiere mínimo 8 caracteres, una mayúscula, una minúscula y un número.",
            vm.uiState.value.error
        )
        assertEquals(0, api.llamadasConfirmar)
    }

    @Test
    fun `confirmar exitoso pasa al paso de exito`() = runTest {
        val api = FakeRecuperarPasswordApi(resultadoConfirmar = Result.success(Unit))
        val vm = RecuperarPasswordViewModel(api)
        vm.solicitarCodigo("2099000001")
        dispatcher.scheduler.advanceUntilIdle()

        vm.confirmar("123456", "ClaveNueva1", "ClaveNueva1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RecuperarPasswordPaso.EXITO, vm.uiState.value.paso)
        assertEquals(1, api.llamadasConfirmar)
    }

    @Test
    fun `confirmar fallido muestra el error real y no avanza`() = runTest {
        val api = FakeRecuperarPasswordApi(
            resultadoConfirmar = Result.failure(IllegalStateException("Código incorrecto"))
        )
        val vm = RecuperarPasswordViewModel(api)
        vm.solicitarCodigo("2099000001")
        dispatcher.scheduler.advanceUntilIdle()

        vm.confirmar("123456", "ClaveNueva1", "ClaveNueva1")
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(RecuperarPasswordPaso.CODIGO_Y_NUEVA, estado.paso)
        assertEquals("Código incorrecto", estado.error)
    }

    @Test
    fun `reenviar codigo usa la misma matricula ya validada`() = runTest {
        val api = FakeRecuperarPasswordApi()
        val vm = RecuperarPasswordViewModel(api)
        vm.solicitarCodigo("2099000001")
        dispatcher.scheduler.advanceUntilIdle()

        vm.reenviarCodigo()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, api.llamadasSolicitar)
        assertEquals("2099000001", api.ultimaMatriculaSolicitada)
    }
}
