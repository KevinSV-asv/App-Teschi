package com.example.appteschi.data.repository

import com.example.appteschi.service.AdminAccountAuthService
import com.example.appteschi.service.LocalAccountAuthService
import com.example.appteschi.service.TipoCuenta
import com.example.appteschi.testutil.FakeAdminAuthService
import com.example.appteschi.testutil.FakeCuentaAuthService
import com.example.appteschi.testutil.FakeRemoteOtpService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    // ── Bypass de desarrollo (solo corre si BuildConfig.DEBUG — el build
    //    "debug" que usa testDebugUnitTest siempre lo es) ─────────────────────

    @Test
    fun `admin-admin ya no es un bypass — pasa por la cascada real como cualquier otro intento`() = runTest {
        // Ver DEC-021: el bypass de administrador se retiró porque dejaba al
        // usuario "adentro" sin un token de sesión válido para los endpoints
        // que ahora lo exigen (DEC-020). "admin"/"admin" debe intentarse
        // contra el servicio real de administrador como cualquier otro usuario.
        val admin = FakeAdminAuthService()
        val cuenta = FakeCuentaAuthService()
        val repository = AuthRepository(adminAuthService = admin, cuentaAuthService = cuenta)

        val resultado = repository.iniciarSesion("admin", "admin")

        assertTrue(resultado is LoginResultado.Rechazado)
        assertEquals(1, admin.llamadas)
        assertEquals(1, cuenta.llamadas)
    }

    @Test
    fun `usuario de prueba local no llama a ningun servicio`() = runTest {
        val cuenta = FakeCuentaAuthService()
        val repository = AuthRepository(cuentaAuthService = cuenta)

        val resultado = repository.iniciarSesion("alumno", "alumno")

        assertTrue(resultado is LoginResultado.AlumnoPruebaSinOtp)
        assertEquals(0, cuenta.llamadas)
    }

    @Test
    fun `usuarios de prueba sem1 a sem9 entran sin OTP con su semestre simulado correcto`() = runTest {
        val cuenta = FakeCuentaAuthService()
        val repository = AuthRepository(cuentaAuthService = cuenta)

        for (semestre in 1..9) {
            val resultado = repository.iniciarSesion("sem$semestre", "sem$semestre")
            val esperado = resultado as? LoginResultado.AlumnoPruebaSinOtp
            assertTrue("sem$semestre debería entrar como alumno de prueba", esperado != null)
            assertEquals(semestre, esperado?.semestreSimulado)
        }
        assertEquals(0, cuenta.llamadas)
    }

    @Test
    fun `sem10 y sem0 no son validos, y la contrasena debe coincidir con la matricula`() = runTest {
        val repository = AuthRepository(cuentaAuthService = FakeCuentaAuthService())

        assertTrue(repository.iniciarSesion("sem10", "sem10") !is LoginResultado.AlumnoPruebaSinOtp)
        assertTrue(repository.iniciarSesion("sem0", "sem0") !is LoginResultado.AlumnoPruebaSinOtp)
        assertTrue(repository.iniciarSesion("sem3", "sem4") !is LoginResultado.AlumnoPruebaSinOtp)
    }

    // ── Cuenta real de administrador (ver DEC-019) ───────────────────────────

    @Test
    fun `cuenta real de administrador siempre requiere OTP, nunca entra directo`() = runTest {
        val admin = FakeAdminAuthService(
            Result.success(AdminAccountAuthService.Account("control_escolar", "Control Escolar", "OPERADOR", "ticket-admin"))
        )
        val repository = AuthRepository(adminAuthService = admin, cuentaAuthService = FakeCuentaAuthService())

        val resultado = repository.iniciarSesion("control_escolar", "unaClaveReal1!")

        val esperado = resultado as? LoginResultado.AdministradorRequiereOtp
        assertTrue(esperado != null)
        assertEquals("control_escolar", esperado?.usuario)
        assertEquals("OPERADOR", esperado?.rol)
        assertEquals("ticket-admin", esperado?.ticket)
    }

    @Test
    fun `admin real se intenta antes que cuenta de alumno`() = runTest {
        val admin = FakeAdminAuthService(
            Result.success(AdminAccountAuthService.Account("admin", "Admin Real", "SUPERADMIN", "ticket-admin"))
        )
        val cuenta = FakeCuentaAuthService()
        val repository = AuthRepository(adminAuthService = admin, cuentaAuthService = cuenta)

        repository.iniciarSesion("otro_usuario", "clave")

        assertEquals(1, admin.llamadas)
        assertEquals(0, cuenta.llamadas)
    }

    // ── Cuenta propia de alumno (única fuente de login — ver DEC-026:
    //    AppTESCHI ya no depende del SIIA real para nada) ─────────────────────

    @Test
    fun `cuenta propia de alumno exitosa requiere otp`() = runTest {
        val cuenta = FakeCuentaAuthService(
            Result.success(LocalAccountAuthService.Account("20240001", "Ana", "ticket-alumno"))
        )
        val repository = AuthRepository(cuentaAuthService = cuenta)

        val resultado = repository.iniciarSesion("20240001", "Password1")

        val esperado = resultado as? LoginResultado.CuentaPropiaRequiereOtp
        assertTrue(esperado != null)
        assertEquals("20240001", esperado?.matricula)
        assertEquals("Ana", esperado?.nombre)
        assertEquals("ticket-alumno", esperado?.ticket)
    }

    @Test
    fun `cuenta propia falla se rechaza directo con el mensaje del servicio, sin fuentes externas`() = runTest {
        val cuenta = FakeCuentaAuthService(Result.failure(IllegalStateException("Cuenta no encontrada")))
        val repository = AuthRepository(cuentaAuthService = cuenta)

        val resultado = repository.iniciarSesion("20240099", "malapass") as LoginResultado.Rechazado

        assertEquals("Cuenta no encontrada", resultado.mensaje)
        assertEquals(1, cuenta.llamadas)
    }

    // ── OTP remoto (envío y verificación) ─────────────────────────────────────

    @Test
    fun `enviarOtp delega en el servicio remoto con el tipo correcto`() = runTest {
        val otp = FakeRemoteOtpService()
        val repository = AuthRepository(otpService = otp)

        val resultado = repository.enviarOtp(TipoCuenta.ALUMNO, "20240001", "alumno@correo.com", "ticket-alumno")

        assertTrue(resultado.isSuccess)
        assertEquals(1, otp.llamadasEnviar)
        assertEquals("ticket-alumno", otp.ultimoTicketEnviar)
    }

    @Test
    fun `verificarOtpRemoto delega en el servicio remoto`() = runTest {
        val otp = FakeRemoteOtpService(resultadoVerificar = Result.failure(IllegalStateException("Código incorrecto")))
        val repository = AuthRepository(otpService = otp)

        val resultado = repository.verificarOtpRemoto(TipoCuenta.ADMINISTRADOR, "admin", "000000", "ticket-admin")

        assertTrue(resultado.isFailure)
        assertEquals(1, otp.llamadasVerificar)
        assertEquals("ticket-admin", otp.ultimoTicketVerificar)
    }
}
