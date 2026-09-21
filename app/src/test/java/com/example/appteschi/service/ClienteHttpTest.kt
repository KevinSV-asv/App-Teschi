package com.example.appteschi.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ClienteHttpTest {

    @Test
    fun `reconoce las direcciones de Tailscale (100_64 a 100_127) y ninguna otra`() {
        assertTrue(esDireccionTailscale("100.64.0.10"))
        assertTrue(esDireccionTailscale("100.64.0.1"))
        assertTrue(esDireccionTailscale("100.127.255.254"))
        assertFalse(esDireccionTailscale("100.63.0.1"))
        assertFalse(esDireccionTailscale("100.128.0.1"))
        assertFalse(esDireccionTailscale("192.168.1.10"))
        assertFalse(esDireccionTailscale("api.ejemplo.mx"))
    }

    @Test
    fun `un timeout contra una direccion de Tailscale sugiere revisar la VPN`() {
        val mensaje = mensajeDeErrorDeRed(SocketTimeoutException("failed to connect after 10000ms"), "100.64.0.10")
        assertEquals(
            "No se pudo conectar con el servidor. Revisa tu conexión a internet y que Tailscale esté activo e inténtalo de nuevo.",
            mensaje
        )
    }

    @Test
    fun `contra un servidor normal no menciona Tailscale`() {
        val mensaje = mensajeDeErrorDeRed(ConnectException("Connection refused"), "api.ejemplo.mx")
        assertEquals("No se pudo conectar con el servidor. Revisa tu conexión a internet e inténtalo de nuevo.", mensaje)
        assertFalse(mensaje.contains("Tailscale"))
    }

    @Test
    fun `un host que no resuelve dice que no se encontro el servidor`() {
        val mensaje = mensajeDeErrorDeRed(UnknownHostException("api.ejemplo.mx"), "api.ejemplo.mx")
        assertEquals("No se encontró el servidor. Revisa tu conexión a internet e inténtalo de nuevo.", mensaje)
    }

    @Test
    fun `cualquier otro fallo de red usa un mensaje generico sin texto tecnico`() {
        val mensaje = mensajeDeErrorDeRed(IOException("unexpected end of stream on http://100.64.0.10:4000"), "100.64.0.10")
        assertEquals("Se perdió la conexión con el servidor. Inténtalo de nuevo.", mensaje)
        assertFalse(mensaje.contains("100.64"))
    }
}
