package com.example.appteschi.data.repository

import com.example.appteschi.service.AdminAccountAuthService
import com.example.appteschi.service.LocalAccountAuthService
import com.example.appteschi.service.TipoCuenta
import com.example.appteschi.testutil.FakeAdminAuthService
import com.example.appteschi.testutil.FakeCuentaAuthService
import com.example.appteschi.testutil.FakeRemoteOtpService
import com.example.appteschi.testutil.FakeSiiaAuth
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
        val siia = FakeSiiaAuth()
        val repository = AuthRepository(adminAuthService = admin, cuentaAuthService = cuenta, siiaAuthService = siia)

        val resultado = repository.iniciarSesion("admin", "admin")

        assertTrue(resultado is LoginResultado.Rechazado)
        assertEquals(1, admin.llamadas)
        assertEquals(1, cuenta.llamadas)
        assertEquals(1, siia.llamadas)
    }

    @Test
    fun `usuario de prueba local no llama a ningun servicio`() = runTest {
        val cuenta = FakeCuentaAuthService()
        val siia = FakeSiiaAuth()
        val repository = AuthRepository(cuentaAuthService = cuenta, siiaAuthService = siia)

        val resultado = repository.iniciarSesion("alumno", "alumno")

        assertTrue(resultado is LoginResultado.AlumnoPruebaSinOtp)
        assertEquals(0, cuenta.llamadas)
        assertEquals(0, siia.llamadas)
    }

    @Test
    fun `usuarios de prueba sem1 a sem9 entran sin OTP con su semestre simulado correcto`() = runTest {
        val cuenta = FakeCuentaAuthService()
        val siia = FakeSiiaAuth()
        val repository = AuthRepository(cuentaAuthService = cuenta, siiaAuthService = siia)

        for (semestre in 1..9) {
            val resultado = repository.iniciarSesion("sem$semestre", "sem$semestre")
            val esperado = resultado as? LoginResultado.AlumnoPruebaSinOtp
            assertTrue("sem$semestre debería entrar como alumno de prueba", esperado != null)
            assertEquals(semestre, esperado?.semestreSimulado)
        }
        assertEquals(0, cuenta.llamadas)
        assertEquals(0, siia.llamadas)
    }

    @Test
    fun `sem10 y sem0 no son validos, y la contrasena debe coincidir con la matricula`() = runTest {
        val repository = AuthRepository(cuentaAuthService = FakeCuentaAuthService(), siiaAuthService = FakeSiiaAuth())

        assertTrue(repository.iniciarSesion("sem10", "sem10") !is LoginResultado.AlumnoPruebaSinOtp)
        assertTrue(repository.iniciarSesion("sem0", "sem0") !is LoginResultado.AlumnoPruebaSinOtp)
        assertTrue(repository.iniciarSesion("sem3", "sem4") !is LoginResultado.AlumnoPruebaSinOtp)
    }

    // ── Cuenta real de administrador (ver DEC-019) ───────────────────────────

    @Test
    fun `cuenta real de administrador siempre requiere OTP, nunca entra directo`() = runTest {
        val admin = FakeAdminAuthService(
            Result.success(AdminAccountAuthService.Account("control_escolar", "Control Escolar", "OPERADOR"))
        )
        val repository = AuthRepository(adminAuthService = admin, cuentaAuthService = FakeCuentaAuthService(), siiaAuthService = FakeSiiaAuth())

        val resultado = repository.iniciarSesion("control_escolar", "unaClaveReal1!")

        val esperado = resultado as? LoginResultado.AdministradorRequiereOtp
        assertTrue(esperado != null)
        assertEquals("control_escolar", esperado?.usuario)
        assertEquals("OPERADOR", esperado?.rol)
    }

    @Test
    fun `admin real se intenta antes que cuenta de alumno y antes que SIIA`() = runTest {
        val admin = FakeAdminAuthService(
            Result.success(AdminAccountAuthService.Account("admin", "Admin Real", "SUPERADMIN"))
        )
        val cuenta = FakeCuentaAuthService()
        val siia = FakeSiiaAuth()
        val repository = AuthRepository(adminAuthService = admin, cuentaAuthService = cuenta, siiaAuthService = siia)

        repository.iniciarSesion("otro_usuario", "clave")

        assertEquals(1, admin.llamadas)
        assertEquals(0, cuenta.llamadas)
        assertEquals(0, siia.llamadas)
    }

    // ── Cuenta propia de alumno ───────────────────────────────────────────────

    @Test
    fun `cuenta propia de alumno exitosa requiere otp sin tocar SIIA`() = runTest {
        val cuenta = FakeCuentaAuthService(
            Result.success(LocalAccountAuthService.Account("20240001", "Ana"))
        )
        val siia = FakeSiiaAuth()
        val repository = AuthRepository(cuentaAuthService = cuenta, siiaAuthService = siia)

        val resultado = repository.iniciarSesion("20240001", "Password1")

        val esperado = resultado as? LoginResultado.CuentaPropiaRequiereOtp
        assertTrue(esperado != null)
        assertEquals("20240001", esperado?.matricula)
        assertEquals("Ana", esperado?.nombre)
        assertEquals(0, siia.llamadas)
    }

    @Test
    fun `cuenta propia falla y SIIA exitoso requiere otp`() = runTest {
        val cuenta = FakeCuentaAuthService(Result.failure(IllegalStateException("no encontrada")))
        val siia = FakeSiiaAuth(Result.success(Unit))
        val repository = AuthRepository(cuentaAuthService = cuenta, siiaAuthService = siia)

        val resultado = repository.iniciarSesion("20240099", "ClaveReal1")

        val esperado = resultado as? LoginResultado.SiiaRequiereOtp
        assertTrue(esperado != null)
        assertEquals("20240099", esperado?.matricula)
        assertEquals(1, siia.llamadas)
    }

    @Test
    fun `cuenta propia falla y SIIA falla se rechaza con el mensaje del SIIA`() = runTest {
        val cuenta = FakeCuentaAuthService(Result.failure(IllegalStateException("no encontrada")))
        val siia = FakeSiiaAuth(Result.failure(Exception("Usuario o contraseña incorrectos")))
        val repository = AuthRepository(cuentaAuthService = cuenta, siiaAuthService = siia)

        val resultado = repository.iniciarSesion("20240099", "malapass") as LoginResultado.Rechazado

        assertEquals("Usuario o contraseña incorrectos", resultado.mensaje)
    }

    // ── OTP remoto (envío y verificación) ─────────────────────────────────────

    @Test
    fun `enviarOtp delega en el servicio remoto con el tipo correcto`() = runTest {
        val otp = FakeRemoteOtpService()
        val repository = AuthRepository(otpService = otp)

        val resultado = repository.enviarOtp(TipoCuenta.ALUMNO, "20240001", "alumno@correo.com")

        assertTrue(resultado.isSuccess)
        assertEquals(1, otp.llamadasEnviar)
    }

    @Test
    fun `verificarOtpRemoto delega en el servicio remoto`() = runTest {
        val otp = FakeRemoteOtpService(resultadoVerificar = Result.failure(IllegalStateException("Código incorrecto")))
        val repository = AuthRepository(otpService = otp)

        val resultado = repository.verificarOtpRemoto(TipoCuenta.ADMINISTRADOR, "admin", "000000")

        assertTrue(resultado.isFailure)
        assertEquals(1, otp.llamadasVerificar)
    }
}
