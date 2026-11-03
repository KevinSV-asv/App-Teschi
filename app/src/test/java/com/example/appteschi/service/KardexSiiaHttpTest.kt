package com.example.appteschi.service

import com.example.appteschi.core.config.SiiaEndpoints
import com.example.appteschi.data.UserSession
import com.example.appteschi.testutil.cuentaDePrueba
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.ByteString.Companion.encodeUtf8
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KardexSiiaHttpTest {

    private lateinit var siia: MockWebServer

    private val pdf = "%PDF-1.4 kardex de prueba"

    @Before
    fun setUp() {
        siia = MockWebServer().apply { start() }
        UserSession.limpiar()
    }

    @After
    fun tearDown() {
        siia.shutdown()
        UserSession.limpiar()
    }

    private fun servicio(token: String? = "token-final") = KardexSiiaHttp(
        endpoints = SiiaEndpoints(siia.url("/Teschi/api").toString()),
        client = SesionAlumno.client,
        token = { token },
        hoy = { java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.NOVEMBER, 2) } }
    )

    private fun responde(codigo: Int, cuerpo: String) = siia.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))

    @Test
    fun `el kardex se pide con el token como Bearer y sin matricula`() = runTest {
        responde(200, """{ "ok": true, "bloqueado": false, "kardex": [] }""")
        val r = servicio().kardex()
        val peticion = siia.takeRequest()
        assertEquals("GET", peticion.method)
        assertEquals("/Teschi/api/calificaciones/kardex.ashx", peticion.path)
        assertEquals("Bearer token-final", peticion.getHeader("Authorization"))
        assertEquals("02-11-2026", (r as ResultadoKardex.Datos).kardex.fechaElaboracion)
    }

    @Test
    fun `sin token no llama al SIIA`() = runTest {
        assertEquals(MENSAJE_SESION_EXPIRADA, (servicio(token = null).kardex() as ResultadoKardex.Error).mensaje)
        assertEquals(MENSAJE_SESION_EXPIRADA, (servicio(token = null).kardexPdf() as ResultadoKardexPdf.Error).mensaje)
        assertEquals(0, siia.requestCount)
    }

    @Test
    fun `un 401 borra la sesion y avisa que expiro`() = runTest {
        UserSession.establecer(cuentaDePrueba(), "token-final", 480)
        responde(401, """{ "ok": false, "mensaje": "Token requerido o inválido." }""")
        val r = servicio().kardex()
        assertEquals(MENSAJE_SESION_EXPIRADA, (r as ResultadoKardex.Error).mensaje)
        assertNull(UserSession.token)
    }

    @Test
    fun `un 403 con bloqueado se muestra como aviso con sus tramites`() = runTest {
        responde(403, """{ "ok": false, "bloqueado": true, "mensaje": "Tienes trámites pendientes.",
                           "verificaciones": { "pendientes": ["Encuesta de servicio"] } }""")
        val k = (servicio().kardex() as ResultadoKardex.Datos).kardex
        assertTrue(k.bloqueado)
        assertEquals("Tienes trámites pendientes.", k.mensajeBloqueo)
        assertEquals(listOf("Encuesta de servicio"), k.pendientes)
        assertTrue(k.semestres.isEmpty())
    }

    @Test
    fun `un 500 con HTML da un error para el alumno`() = runTest {
        responde(500, "<html>Runtime Error</html>")
        val r = servicio().kardex() as ResultadoKardex.Error
        assertEquals("El SIIA no pudo entregar tu kardex. Intenta más tarde.", r.mensaje)
    }

    @Test
    fun `el PDF lleva el token en el encabezado, nunca en la URL`() = runTest {
        responde(200, """{ "ok": true, "fileName": "Kardex_2099000001.pdf", "mimeType": "application/pdf",
                           "base64": "${pdf.encodeUtf8().base64()}" }""")
        val r = servicio().kardexPdf() as ResultadoKardexPdf.Archivo
        val peticion = siia.takeRequest()
        assertEquals("/Teschi/api/calificaciones/kardex-pdf.ashx?base64=1", peticion.path)
        assertEquals("Bearer token-final", peticion.getHeader("Authorization"))
        assertFalse(peticion.path!!.contains("token"))
        assertEquals("Kardex_2099000001.pdf", r.pdf.nombreArchivo)
        assertArrayEquals(pdf.toByteArray(), r.pdf.contenido)
    }

    @Test
    fun `algo que no es PDF no se guarda`() = runTest {
        responde(200, """{ "ok": true, "fileName": "Kardex.pdf", "base64": "${"<html>error</html>".encodeUtf8().base64()}" }""")
        assertTrue(servicio().kardexPdf() is ResultadoKardexPdf.Error)

        responde(200, """{ "ok": true, "fileName": "Kardex.pdf", "base64": "" }""")
        assertTrue(servicio().kardexPdf() is ResultadoKardexPdf.Error)

        responde(200, """{ "ok": true, "fileName": "Kardex.pdf", "base64": "esto no es base64 !!" }""")
        assertTrue(servicio().kardexPdf() is ResultadoKardexPdf.Error)
    }

    @Test
    fun `con bloqueo no hay PDF y se explica por que`() = runTest {
        responde(403, """{ "ok": false, "bloqueado": true, "mensaje": "Tienes trámites pendientes." }""")
        assertEquals("Tienes trámites pendientes.", (servicio().kardexPdf() as ResultadoKardexPdf.Error).mensaje)
    }

    @Test
    fun `el nombre del archivo no puede salirse de lo seguro`() {
        assertEquals("Kardex_2099000001.pdf", nombreArchivoSeguro("Kardex_2099000001.pdf"))
        assertEquals("evil.pdf", nombreArchivoSeguro("../../evil.pdf"))
        assertEquals("evil.pdf", nombreArchivoSeguro("C:\\temp\\evil.pdf"))
        assertEquals("Kardex.pdf", nombreArchivoSeguro("reporte.exe"))
        assertEquals("Kardex.pdf", nombreArchivoSeguro(""))
    }
}
