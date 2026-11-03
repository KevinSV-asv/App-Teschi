package com.example.appteschi.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ClienteHttpTest {

    @Test
    fun `un timeout o conexion rechazada pide revisar el internet sin mencionar Tailscale`() {
        val esperado = "No se pudo conectar con el SIIA. Revisa tu conexión a internet e inténtalo de nuevo."
        assertEquals(esperado, mensajeDeErrorDeRed(SocketTimeoutException("failed to connect after 10000ms")))
        assertEquals(esperado, mensajeDeErrorDeRed(ConnectException("Connection refused")))
        assertFalse(esperado.contains("Tailscale"))
    }

    @Test
    fun `un host que no resuelve dice que no se encontro el SIIA`() {
        assertEquals(
            "No se encontró el SIIA. Revisa tu conexión a internet e inténtalo de nuevo.",
            mensajeDeErrorDeRed(UnknownHostException("siia.teschi.edu.mx"))
        )
    }

    @Test
    fun `cualquier otro fallo de red usa un mensaje generico sin texto tecnico`() {
        val mensaje = mensajeDeErrorDeRed(IOException("unexpected end of stream on https://siia.teschi.edu.mx/..."))
        assertEquals("Se perdió la conexión con el SIIA. Inténtalo de nuevo.", mensaje)
        assertFalse(mensaje.contains("https"))
    }

    @Test
    fun `una pagina HTML de error del servidor no se toma como JSON`() {
        assertNull(jsonONulo("<html><body>Runtime Error</body></html>"))
        assertNull(jsonONulo(""))
        assertNull(jsonONulo(null))
        assertEquals("hola", jsonONulo("""{"mensaje":"hola"}""").mensajeSiia())
    }

    @Test
    fun `un mensaje vacio o null del SIIA cuenta como ausente`() {
        assertNull(jsonONulo("""{"mensaje":""}""").mensajeSiia())
        assertNull(jsonONulo("""{"mensaje":null}""").mensajeSiia())
        assertNull(jsonONulo("""{}""").mensajeSiia())
    }
}
