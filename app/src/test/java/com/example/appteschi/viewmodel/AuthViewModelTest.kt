package com.example.appteschi.viewmodel

import com.example.appteschi.data.UserSession
import com.example.appteschi.data.repository.AuthRepository
import com.example.appteschi.service.AdminAccountAuthService
import com.example.appteschi.service.LocalAccountAuthService
import com.example.appteschi.service.TipoCuenta
import com.example.appteschi.testutil.FakeAdminAuthService
import com.example.appteschi.testutil.FakeCuentaAuthService
import com.example.appteschi.testutil.FakeRemoteOtpService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Prueba AuthViewModel a través de un AuthRepository real conectado a fakes
 * de servicio (no un fake de Repository) — así esta suite también cubre que
 * AuthRepository y AuthViewModel se integran correctamente entre sí.
 * La cascada en sí (qué devuelve el Repository para cada combinación) ya se
 * prueba de forma aislada en AuthRepositoryTest. El código OTP en sí ya no se
 * prueba aquí — vive y se verifica en el backend (ver DEC-019); estos tests
 * solo comprueban que el ViewModel reacciona correctamente a lo que el
 * servicio remoto (fake) responde.
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

    private fun viewModel(
        admin: FakeAdminAuthService = FakeAdminAuthService(),
        cuenta: FakeCuentaAuthService = FakeCuentaAuthService(),
        otp: FakeRemoteOtpService = FakeRemoteOtpService()
    ) = AuthViewModel(
        authRepository = AuthRepository(adminAuthService = admin, cuentaAuthService = cuenta, otpService = otp)
    )

    // ── Cascada de login: admin (debug) → admin real → cuenta propia — ver
    //    DEC-026: AppTESCHI ya no depende del SIIA real para nada ───────────

    @Test
    fun `admin-admin ya no es bypass — sin cuenta real detras, se rechaza`() = runTest {
        // Ver DEC-021: retirado porque dejaba esAdministrador=true sin token
        // de sesion valido, rompiendo cada pantalla del panel.
        val admin = FakeAdminAuthService()
        val cuenta = FakeCuentaAuthService()
        val vm = viewModel(admin, cuenta)

        vm.validarCredenciales("admin", "admin")
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertFalse(estado.irAlDashboard)
        assertFalse(UserSession.esAdministrador)
        assertEquals(1, admin.llamadas)
        assertEquals(1, cuenta.llamadas)
    }

    @Test
    fun `cuenta real de administrador avanza a verificacion como ADMINISTRADOR`() = runTest {
        val admin = FakeAdminAuthService(
            Result.success(AdminAccountAuthService.Account("control_escolar", "Control Escolar", "OPERADOR", "ticket-admin"))
        )
        val vm = viewModel(admin = admin)

        vm.validarCredenciales("control_escolar", "unaClaveReal1!")
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(LoginPaso.VERIFICACION, estado.paso)
        assertEquals(TipoCuenta.ADMINISTRADOR, estado.tipoPendiente)
        assertEquals("control_escolar", estado.matriculaValidada)
        assertEquals("OPERADOR", estado.rolValidado)
        // Todavía no debe haber sesión activa: falta el OTP.
        assertFalse(UserSession.esAdministrador)
    }

    @Test
    fun `cuenta propia de alumno exitosa avanza a verificacion como ALUMNO`() = runTest {
        val cuenta = FakeCuentaAuthService(
            Result.success(LocalAccountAuthService.Account("20240001", "Ana", "ticket-alumno"))
        )
        val vm = viewModel(cuenta = cuenta)

        vm.validarCredenciales("20240001", "Password1")
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(LoginPaso.VERIFICACION, estado.paso)
        assertEquals(TipoCuenta.ALUMNO, estado.tipoPendiente)
        assertEquals("20240001", estado.matriculaValidada)
    }

    @Test
    fun `usuario de prueba alumno entra sin OTP y sin volverse administrador`() = runTest {
        val vm = viewModel()

        vm.validarCredenciales("alumno", "alumno")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.irAlDashboard)
        assertFalse(UserSession.esAdministrador)
        assertEquals(null, UserSession.semestreSimulado)
    }

    @Test
    fun `usuario de prueba sem3 entra sin OTP y deja el semestre simulado en la sesion`() = runTest {
        val vm = viewModel()

        vm.validarCredenciales("sem3", "sem3")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.irAlDashboard)
        assertFalse(UserSession.esAdministrador)
        assertEquals(3, UserSession.semestreSimulado)
        assertEquals(3, UserSession.semestreActual)
    }

    @Test
    fun `cuenta propia falla se rechaza directo, sin intentar ninguna fuente externa`() = runTest {
        val cuenta = FakeCuentaAuthService(Result.failure(IllegalStateException("Cuenta no encontrada")))
        val vm = viewModel(cuenta = cuenta)

        vm.validarCredenciales("20240099", "malapass")
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(LoginPaso.CREDENCIALES, estado.paso)
        assertEquals("Cuenta no encontrada", estado.errorLogin)
        assertEquals(1, cuenta.llamadas)
    }

    @Test
    fun `campos vacios no llaman a ningun servicio`() = runTest {
        val cuenta = FakeCuentaAuthService()
        val vm = viewModel(cuenta = cuenta)

        vm.validarCredenciales("", "")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Ingresa tu matrícula y contraseña", vm.uiState.value.errorLogin)
        assertEquals(0, cuenta.llamadas)
    }

    // ── OTP: el ViewModel solo refleja lo que el backend (fake) responde ─────

    @Test
    fun `envio de otp exitoso marca otpEnviado`() = runTest {
        val vm = viewModel()

        vm.enviarCodigoVerificacion("alumno@correo.com") {}
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.otpEnviado)
    }

    @Test
    fun `envio de otp fallido no marca otpEnviado y muestra el error`() = runTest {
        val otp = FakeRemoteOtpService(resultadoEnviar = Result.failure(IllegalStateException("Alumno no encontrado")))
        val vm = viewModel(otp = otp)

        vm.enviarCodigoVerificacion("alumno@correo.com") {}
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertFalse(estado.otpEnviado)
        assertEquals("Alumno no encontrado", estado.errorEnvio)
    }

    @Test
    fun `otp correcto segun el backend verifica con exito`() = runTest {
        val otp = FakeRemoteOtpService(resultadoVerificar = Result.success(null))
        val vm = viewModel(otp = otp)

        vm.enviarCodigoVerificacion("alumno@correo.com") {}
        dispatcher.scheduler.advanceUntilIdle()

        var exito = false
        vm.verificarOtp(codigoIngresado = "123456", onExito = { exito = true }, onFallo = {})
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(exito)
        assertEquals(1, otp.llamadasVerificar)
    }

    @Test
    fun `otp de administrador exitoso guarda el token de sesion en el estado`() = runTest {
        val admin = FakeAdminAuthService(
            Result.success(AdminAccountAuthService.Account("admin", "Admin", "SUPERADMIN", "ticket-admin"))
        )
        val otp = FakeRemoteOtpService(resultadoVerificar = Result.success("token-de-prueba"))
        val vm = viewModel(admin = admin, otp = otp)

        vm.validarCredenciales("admin", "unaClaveReal1!")
        dispatcher.scheduler.advanceUntilIdle()
        vm.enviarCodigoVerificacion("admin@correo.com") {}
        dispatcher.scheduler.advanceUntilIdle()
        vm.verificarOtp(codigoIngresado = "123456", onExito = {}, onFallo = {})
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("token-de-prueba", vm.uiState.value.tokenSesion)
    }

    @Test
    fun `otp de administrador exitoso entrega el token directo a onExito, no solo al estado`() = runTest {
        // Regresión: LoginScreen guardaba UserSession.adminToken leyendo
        // uiState.tokenSesion dentro de onExito — ese State (via collectAsState)
        // podía no haber propagado todavía el update recién hecho, así que
        // UserSession.adminToken se quedaba en null pese a que el login sí
        // funcionaba (ver 04/BASE_DATOS o CAMBIOS_DE_CODIGO). Por eso onExito
        // ahora recibe el token como parámetro — este test cubre ese contrato.
        val admin = FakeAdminAuthService(
            Result.success(AdminAccountAuthService.Account("admin", "Admin", "SUPERADMIN", "ticket-admin"))
        )
        val otp = FakeRemoteOtpService(resultadoVerificar = Result.success("token-de-prueba"))
        val vm = viewModel(admin = admin, otp = otp)

        vm.validarCredenciales("admin", "unaClaveReal1!")
        dispatcher.scheduler.advanceUntilIdle()
        vm.enviarCodigoVerificacion("admin@correo.com") {}
        dispatcher.scheduler.advanceUntilIdle()

        var tokenRecibido: String? = "sin-recibir"
        vm.verificarOtp(codigoIngresado = "123456", onExito = { tokenRecibido = it }, onFallo = {})
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("token-de-prueba", tokenRecibido)
    }

    @Test
    fun `otp incorrecto segun el backend incrementa intentos fallidos y no bloquea`() = runTest {
        val otp = FakeRemoteOtpService(resultadoVerificar = Result.failure(IllegalStateException("Código incorrecto")))
        val vm = viewModel(otp = otp)

        vm.enviarCodigoVerificacion("alumno@correo.com") {}
        dispatcher.scheduler.advanceUntilIdle()

        var mensajeFallo: String? = null
        vm.verificarOtp(codigoIngresado = "000000", onExito = {}, onFallo = { mensajeFallo = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.intentosFallidos)
        assertFalse(vm.uiState.value.bloqueado)
        assertEquals("Código incorrecto", mensajeFallo)
    }

    @Test
    fun `backend reporta demasiados intentos y el ViewModel marca bloqueado`() = runTest {
        val otp = FakeRemoteOtpService(
            resultadoVerificar = Result.failure(IllegalStateException("Demasiados intentos fallidos. Solicita un nuevo código."))
        )
        val vm = viewModel(otp = otp)

        vm.enviarCodigoVerificacion("alumno@correo.com") {}
        dispatcher.scheduler.advanceUntilIdle()

        vm.verificarOtp(codigoIngresado = "000000", onExito = {}, onFallo = {})
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.bloqueado)
    }

    @Test
    fun `se puede verificar con un codigo de un envio anterior, sin llamar a enviarCodigoVerificacion en esta sesion`() = runTest {
        // "Ya tengo un código" en LoginScreen solo revela el campo local —
        // nunca llama a enviarCodigoVerificacion(). El ViewModel no debe
        // bloquear la verificación por eso; el backend (fake, aquí exitoso)
        // es quien de verdad decide si el código sigue vigente.
        val otp = FakeRemoteOtpService(resultadoVerificar = Result.success(null))
        val vm = viewModel(otp = otp)

        var exito = false
        var mensajeFallo: String? = null
        vm.verificarOtp(codigoIngresado = "123456", onExito = { exito = true }, onFallo = { mensajeFallo = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(exito)
        assertEquals(null, mensajeFallo)
        assertEquals(1, otp.llamadasVerificar)
        assertEquals(0, otp.llamadasEnviar)
    }

    @Test
    fun `codigo con longitud invalida se rechaza localmente sin llamar al backend`() = runTest {
        val otp = FakeRemoteOtpService()
        val vm = viewModel(otp = otp)

        var mensajeFallo: String? = null
        vm.verificarOtp(codigoIngresado = "123", onExito = {}, onFallo = { mensajeFallo = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Ingresa los 6 dígitos", mensajeFallo)
        assertEquals(0, otp.llamadasVerificar)
    }

    // ── Ticket de login (DEC-030): sin él el backend no deja pedir ni verificar
    //    el OTP, así que el ViewModel debe llevar el de la cuenta validada ─────

    @Test
    fun `el ticket de la cuenta de alumno viaja tanto al enviar como al verificar el OTP`() = runTest {
        val cuenta = FakeCuentaAuthService(Result.success(LocalAccountAuthService.Account("20240001", "Ana", "ticket-alumno")))
        val otp = FakeRemoteOtpService(resultadoVerificar = Result.success("token-alumno"))
        val vm = viewModel(cuenta = cuenta, otp = otp)

        vm.validarCredenciales("20240001", "Password1")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("ticket-alumno", vm.uiState.value.ticket)

        vm.enviarCodigoVerificacion("ana@correo.com") {}
        dispatcher.scheduler.advanceUntilIdle()
        vm.verificarOtp(codigoIngresado = "123456", onExito = {}, onFallo = {})
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(TipoCuenta.ALUMNO, otp.ultimoTipoEnviar)
        assertEquals("ticket-alumno", otp.ultimoTicketEnviar)
        assertEquals("ticket-alumno", otp.ultimoTicketVerificar)
    }

    @Test
    fun `el ticket de la cuenta de administrador viaja al OTP de administrador`() = runTest {
        val admin = FakeAdminAuthService(Result.success(AdminAccountAuthService.Account("admin", "Admin", "SUPERADMIN", "ticket-admin")))
        val otp = FakeRemoteOtpService(resultadoVerificar = Result.success("token-admin"))
        val vm = viewModel(admin = admin, otp = otp)

        vm.validarCredenciales("admin", "unaClaveReal1!")
        dispatcher.scheduler.advanceUntilIdle()
        vm.enviarCodigoVerificacion("admin@correo.com") {}
        dispatcher.scheduler.advanceUntilIdle()
        vm.verificarOtp(codigoIngresado = "123456", onExito = {}, onFallo = {})
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(TipoCuenta.ADMINISTRADOR, otp.ultimoTipoEnviar)
        assertEquals("ticket-admin", otp.ultimoTicketEnviar)
        assertEquals("ticket-admin", otp.ultimoTicketVerificar)
    }

    @Test
    fun `otp de alumno exitoso entrega el token de sesion del alumno a onExito`() = runTest {
        val cuenta = FakeCuentaAuthService(Result.success(LocalAccountAuthService.Account("20240001", "Ana", "ticket-alumno")))
        val otp = FakeRemoteOtpService(resultadoVerificar = Result.success("token-alumno"))
        val vm = viewModel(cuenta = cuenta, otp = otp)

        vm.validarCredenciales("20240001", "Password1")
        dispatcher.scheduler.advanceUntilIdle()

        var tokenRecibido: String? = "sin-recibir"
        vm.verificarOtp(codigoIngresado = "123456", onExito = { tokenRecibido = it }, onFallo = {})
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("token-alumno", tokenRecibido)
    }

    @Test
    fun `volver al login descarta el ticket para que no se reutilice con otra cuenta`() = runTest {
        val cuenta = FakeCuentaAuthService(Result.success(LocalAccountAuthService.Account("20240001", "Ana", "ticket-alumno")))
        val vm = viewModel(cuenta = cuenta)

        vm.validarCredenciales("20240001", "Password1")
        dispatcher.scheduler.advanceUntilIdle()
        vm.volverAlLogin()

        assertEquals("", vm.uiState.value.ticket)
    }
}
