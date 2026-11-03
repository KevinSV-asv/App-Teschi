package com.example.appteschi.data.repository

import com.example.appteschi.service.SiiaLoginResultado
import com.example.appteschi.service.SiiaNipResultado
import com.example.appteschi.testutil.FakeSiiaAuthService
import com.example.appteschi.testutil.cuentaDePrueba
import com.example.appteschi.testutil.sesionDePrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    @Test
    fun `contrasena correcta de alumno pasa al NIP`() = runTest {
        val siia = FakeSiiaAuthService(SiiaLoginResultado.RequiereNip(true, "temp-1", cuentaDePrueba()))
        val r = AuthRepository(siia).iniciarSesion(" 2099000001 ", "secreta") as LoginResultado.RequiereNip
        assertEquals("temp-1", r.tempToken)
        assertTrue(r.tieneNip)
        assertEquals(1, siia.llamadas)
    }

    @Test
    fun `sin datos de la cuenta en el paso 1 tambien pasa al NIP`() = runTest {
        val siia = FakeSiiaAuthService(SiiaLoginResultado.RequiereNip(false, "temp-2", null))
        val r = AuthRepository(siia).iniciarSesion("2099000001", "secreta")
        assertTrue(r is LoginResultado.RequiereNip)
    }

    @Test
    fun `un docente se rechaza antes del NIP`() = runTest {
        val siia = FakeSiiaAuthService(SiiaLoginResultado.RequiereNip(true, "temp-1", cuentaDePrueba("P")))
        val r = AuthRepository(siia).iniciarSesion("docente01", "secreta")
        assertEquals(LoginResultado.Rechazado(MENSAJE_SOLO_ALUMNOS), r)
    }

    @Test
    fun `contrasena incorrecta se rechaza con el mensaje del SIIA`() = runTest {
        val siia = FakeSiiaAuthService(SiiaLoginResultado.Error("Usuario o password incorrectos."))
        assertEquals(
            LoginResultado.Rechazado("Usuario o password incorrectos."),
            AuthRepository(siia).iniciarSesion("2099000001", "mala")
        )
    }

    @Test
    fun `si el SIIA da el token sin pedir NIP se entra directo`() = runTest {
        val siia = FakeSiiaAuthService(SiiaLoginResultado.Sesion(sesionDePrueba()))
        val r = AuthRepository(siia).iniciarSesion("2099000001", "secreta")
        assertEquals(LoginResultado.Sesion(sesionDePrueba()), r)
    }

    @Test
    fun `NIP correcto de alumno entrega la sesion`() = runTest {
        val siia = FakeSiiaAuthService(resultadoNip = SiiaNipResultado.Sesion(sesionDePrueba()))
        val r = AuthRepository(siia).verificarNip("temp-1", "1234", cuentaDePrueba())
        assertEquals(LoginResultado.Sesion(sesionDePrueba()), r)
        assertEquals("temp-1", siia.ultimoTempToken)
        assertEquals(cuentaDePrueba(), siia.ultimaCuentaPaso1)
    }

    @Test
    fun `NIP correcto de un administrativo se rechaza`() = runTest {
        val siia = FakeSiiaAuthService(resultadoNip = SiiaNipResultado.Sesion(sesionDePrueba("A")))
        val r = AuthRepository(siia).verificarNip("temp-1", "1234", null)
        assertEquals(LoginResultado.Rechazado(MENSAJE_SOLO_ALUMNOS), r)
    }

    @Test
    fun `NIP incorrecto se rechaza con el mensaje del SIIA`() = runTest {
        val siia = FakeSiiaAuthService(resultadoNip = SiiaNipResultado.Error("NIP incorrecto."))
        assertEquals(LoginResultado.Rechazado("NIP incorrecto."), AuthRepository(siia).verificarNip("temp-1", "0000", null))
    }

    @Test
    fun `crear NIP usa configurar y entra`() = runTest {
        val siia = FakeSiiaAuthService(resultadoNip = SiiaNipResultado.Sesion(sesionDePrueba()))
        val r = AuthRepository(siia).crearNip("temp-2", "4321", "4321", null)
        assertTrue(r is LoginResultado.Sesion)
        assertEquals("4321", siia.ultimoNuevoNip)
    }
}
