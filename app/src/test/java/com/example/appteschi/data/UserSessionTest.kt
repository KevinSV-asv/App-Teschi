package com.example.appteschi.data

import com.example.appteschi.testutil.cuentaDePrueba
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserSessionTest {

    private val ahora = 1_800_000_000_000L
    private val minuto = 60_000L

    @Before
    fun setUp() = UserSession.limpiar()

    @After
    fun tearDown() = UserSession.limpiar()

    @Test
    fun `guarda los datos del SIIA y el token`() {
        UserSession.establecer(cuentaDePrueba(), "token-de-prueba", 480, ahora)
        assertEquals("2099000001", UserSession.matricula)
        assertEquals("ALUMNO DE PRUEBA UNO", UserSession.nombreMostrar)
        assertEquals("INGENIERÍA EN SISTEMAS COMPUTACIONALES", UserSession.carrera)
        assertEquals("token-de-prueba", UserSession.token)
        assertEquals(ahora + 480 * minuto, UserSession.tokenExpiraEn)
    }

    @Test
    fun `la sesion vale hasta un minuto antes de que venza el token`() {
        UserSession.establecer(cuentaDePrueba(), "token-de-prueba", 480, ahora)
        assertTrue(UserSession.sesionVigente(ahora))
        assertTrue(UserSession.sesionVigente(ahora + 478 * minuto))
        assertFalse(UserSession.sesionVigente(ahora + 479 * minuto))
        assertFalse(UserSession.sesionVigente(ahora + 500 * minuto))
    }

    @Test
    fun `sin token no hay sesion`() {
        assertFalse(UserSession.sesionVigente(ahora))
    }

    @Test
    fun `si el SIIA no dice cuando vence se da por vigente`() {
        UserSession.establecer(cuentaDePrueba(), "token-de-prueba", 0, ahora)
        assertTrue(UserSession.sesionVigente(ahora + 10_000 * minuto))
    }

    @Test
    fun `sin numUsuario la matricula es el usuario`() {
        UserSession.establecer(cuentaDePrueba().copy(matricula = ""), "t", 480, ahora)
        assertEquals("2099000001", UserSession.matricula)
    }

    @Test
    fun `limpiar borra todo`() {
        UserSession.establecer(cuentaDePrueba(), "token-de-prueba", 480, ahora)
        UserSession.limpiar()
        assertNull(UserSession.token)
        assertEquals("", UserSession.matricula)
        assertEquals("ALUMNO TESCHI", UserSession.nombreMostrar)
    }

    @Test
    fun `solo el tipo E (alumno) cuenta como alumno`() {
        assertTrue(cuentaDePrueba("E").esAlumno)
        assertTrue(cuentaDePrueba("e ").esAlumno)
        assertFalse(cuentaDePrueba("P").esAlumno)
        assertFalse(cuentaDePrueba("A").esAlumno)
        assertFalse(cuentaDePrueba("").esAlumno)
    }
}
