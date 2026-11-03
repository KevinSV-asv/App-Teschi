package com.example.appteschi.viewmodel

import com.example.appteschi.data.UserSession
import com.example.appteschi.data.repository.AuthRepository
import com.example.appteschi.data.repository.MENSAJE_SOLO_ALUMNOS
import com.example.appteschi.service.SiiaLoginResultado
import com.example.appteschi.service.SiiaNipResultado
import com.example.appteschi.testutil.FakeSiiaAuthService
import com.example.appteschi.testutil.cuentaDePrueba
import com.example.appteschi.testutil.sesionDePrueba
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

/**
 * Prueba AuthViewModel con un AuthRepository real conectado a un SIIA falso:
 * así también cubre que Repository y ViewModel se integran bien.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

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

    private fun viewModel(siia: FakeSiiaAuthService) = AuthViewModel(AuthRepository(siia))

    private val pideNip = SiiaLoginResultado.RequiereNip(tieneNip = true, tempToken = "temp-1", cuenta = cuentaDePrueba())

    @Test
    fun `campos vacios no llaman al SIIA`() = runTest {
        val siia = FakeSiiaAuthService()
        val vm = viewModel(siia)
        vm.validarCredenciales("  ", "")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Ingresa tu usuario y contraseña del SIIA", vm.uiState.value.errorLogin)
        assertEquals(0, siia.llamadas)
    }

    @Test
    fun `contrasena incorrecta se queda en credenciales con el mensaje`() = runTest {
        val vm = viewModel(FakeSiiaAuthService(SiiaLoginResultado.Error("Usuario o password incorrectos.")))
        vm.validarCredenciales("2099000001", "mala")
        dispatcher.scheduler.advanceUntilIdle()
        val e = vm.uiState.value
        assertEquals(LoginPaso.CREDENCIALES, e.paso)
        assertEquals("Usuario o password incorrectos.", e.errorLogin)
        assertFalse(e.cargando)
    }

    @Test
    fun `contrasena correcta pasa al paso NIP con el nombre del alumno`() = runTest {
        val vm = viewModel(FakeSiiaAuthService(pideNip))
        vm.validarCredenciales(" 2099000001 ", "secreta")
        dispatcher.scheduler.advanceUntilIdle()
        val e = vm.uiState.value
        assertEquals(LoginPaso.NIP, e.paso)
        assertEquals("temp-1", e.tempToken)
        assertEquals("2099000001", e.usuario)
        assertEquals("ALUMNO DE PRUEBA UNO", e.nombreValidado)
        assertTrue(e.tieneNip)
    }

    @Test
    fun `un docente no llega al NIP`() = runTest {
        val vm = viewModel(FakeSiiaAuthService(SiiaLoginResultado.RequiereNip(true, "temp-1", cuentaDePrueba("P"))))
        vm.validarCredenciales("docente01", "secreta")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(LoginPaso.CREDENCIALES, vm.uiState.value.paso)
        assertEquals(MENSAJE_SOLO_ALUMNOS, vm.uiState.value.errorLogin)
    }

    @Test
    fun `NIP correcto guarda la sesion del SIIA y entra`() = runTest {
        val siia = FakeSiiaAuthService(pideNip, resultadoNip = SiiaNipResultado.Sesion(sesionDePrueba()))
        val vm = viewModel(siia)
        vm.validarCredenciales("2099000001", "secreta")
        dispatcher.scheduler.advanceUntilIdle()
        vm.verificarNip("1234")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.irAlDashboard)
        assertEquals("temp-1", siia.ultimoTempToken)
        assertEquals("1234", siia.ultimoNip)
        assertEquals(cuentaDePrueba(), siia.ultimaCuentaPaso1)
        assertEquals("token-de-prueba", UserSession.token)
        assertEquals("2099000001", UserSession.matricula)
        assertTrue(UserSession.sesionVigente())
    }

    @Test
    fun `NIP incorrecto muestra el error y no guarda sesion`() = runTest {
        val vm = viewModel(FakeSiiaAuthService(pideNip, resultadoNip = SiiaNipResultado.Error("NIP incorrecto.")))
        vm.validarCredenciales("2099000001", "secreta")
        dispatcher.scheduler.advanceUntilIdle()
        vm.verificarNip("0000")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("NIP incorrecto.", vm.uiState.value.errorNip)
        assertFalse(vm.uiState.value.irAlDashboard)
        assertNull(UserSession.token)
    }

    @Test
    fun `un NIP que no es de 4 digitos no llama al SIIA`() = runTest {
        val siia = FakeSiiaAuthService(pideNip)
        val vm = viewModel(siia)
        vm.validarCredenciales("2099000001", "secreta")
        dispatcher.scheduler.advanceUntilIdle()
        vm.verificarNip("12a")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("El NIP debe tener 4 dígitos", vm.uiState.value.errorNip)
        assertEquals(0, siia.llamadasNip)
    }

    @Test
    fun `crear NIP exige que coincidan`() = runTest {
        val siia = FakeSiiaAuthService(SiiaLoginResultado.RequiereNip(false, "temp-2", null))
        val vm = viewModel(siia)
        vm.validarCredenciales("2099000001", "secreta")
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.tieneNip)
        vm.crearNip("1234", "4321")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Los NIP no coinciden", vm.uiState.value.errorNip)
        assertEquals(0, siia.llamadasNip)
    }

    @Test
    fun `crear NIP correcto entra`() = runTest {
        val siia = FakeSiiaAuthService(
            SiiaLoginResultado.RequiereNip(false, "temp-2", null),
            resultadoNip = SiiaNipResultado.Sesion(sesionDePrueba())
        )
        val vm = viewModel(siia)
        vm.validarCredenciales("2099000001", "secreta")
        dispatcher.scheduler.advanceUntilIdle()
        vm.crearNip("4321", "4321")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.irAlDashboard)
        assertEquals("4321", siia.ultimoNuevoNip)
    }

    @Test
    fun `sin tempToken el paso NIP pide volver a empezar`() = runTest {
        val vm = viewModel(FakeSiiaAuthService())
        vm.verificarNip("1234")
        assertEquals(SESION_VERIFICACION_PERDIDA, vm.uiState.value.errorNip)
    }

    @Test
    fun `recuperar NIP valida la CURP y manda el usuario que inicio sesion`() = runTest {
        val siia = FakeSiiaAuthService(pideNip)
        val vm = viewModel(siia)
        vm.validarCredenciales("2099000001", "secreta")
        dispatcher.scheduler.advanceUntilIdle()

        vm.recuperarNip("CORTA")
        assertEquals("La CURP debe tener 18 letras y números", vm.uiState.value.errorNip)

        vm.recuperarNip("xxxx010101hdfxxx09")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("XXXX010101HDFXXX09", siia.ultimaCurp)
        assertEquals("2099000001", siia.ultimoUsuarioRecuperar)
        assertEquals("Te enviamos tu NIP a a***o@teschi.edu.mx", vm.uiState.value.mensajeNip)
    }

    @Test
    fun `regresar limpia el paso NIP`() = runTest {
        val vm = viewModel(FakeSiiaAuthService(pideNip))
        vm.validarCredenciales("2099000001", "secreta")
        dispatcher.scheduler.advanceUntilIdle()
        vm.volverAlLogin()
        assertEquals(AuthUiState(), vm.uiState.value)
    }
}
