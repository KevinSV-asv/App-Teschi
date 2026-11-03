package com.example.appteschi.service

import com.example.appteschi.core.config.SiiaEndpoints
import com.example.appteschi.data.UserSession
import com.example.appteschi.testutil.cuentaDePrueba
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class CalificacionesSiiaHttpTest {

    private lateinit var siia: MockWebServer

    private val ok = """{ "bloqueado": false, "periodo": "2026-2", "periodosDisponibles": ["2026-2"],
                          "resumen": null, "calificaciones": [] }"""
    private val errorGenerico = """{ "ok": false, "mensaje": "No fue posible procesar la solicitud." }"""

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

    /** "Hoy" es octubre de 2026: los periodos recientes son 2026-2, 2026-1 y 2025-2. */
    private fun servicio(token: String? = "token-final") = CalificacionesSiiaHttp(
        endpoints = SiiaEndpoints(siia.url("/Teschi/api").toString()),
        client = SesionAlumno.client,
        token = { token },
        hoy = { Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 12) } }
    )

    private fun responde(codigo: Int, cuerpo: String) = siia.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))

    @Test
    fun `consulta con el token del alumno como Bearer`() = runTest {
        responde(200, ok)
        val r = servicio().parciales("2026-1")
        val peticion = siia.takeRequest()
        assertEquals("GET", peticion.method)
        assertEquals("/Teschi/api/calificaciones/parciales.ashx?periodo=2026-1", peticion.path)
        assertEquals("Bearer token-final", peticion.getHeader("Authorization"))
        assertTrue(r is ResultadoParciales.Datos)
    }

    @Test
    fun `sin token no llama al SIIA`() = runTest {
        val r = servicio(token = null).parciales(null)
        assertEquals(MENSAJE_SESION_EXPIRADA, (r as ResultadoParciales.Error).mensaje)
        assertEquals(0, siia.requestCount)
    }

    @Test
    fun `si el SIIA falla con el periodo actual prueba los recientes y avisa`() = runTest {
        responde(500, errorGenerico)  // sin periodo
        responde(500, errorGenerico)  // 2026-2
        responde(200, ok.replace("2026-2", "2026-1"))  // 2026-1

        val r = servicio().parciales(null) as ResultadoParciales.Datos

        assertEquals("/Teschi/api/calificaciones/parciales.ashx", siia.takeRequest().path)
        assertEquals("/Teschi/api/calificaciones/parciales.ashx?periodo=2026-2", siia.takeRequest().path)
        assertEquals("/Teschi/api/calificaciones/parciales.ashx?periodo=2026-1", siia.takeRequest().path)
        assertEquals("El SIIA no pudo mostrar el periodo actual; se muestra el periodo 2026-1.", r.calificaciones.aviso)
    }

    @Test
    fun `con un periodo elegido no reintenta otros`() = runTest {
        responde(500, errorGenerico)
        val r = servicio().parciales("2025-2")
        assertTrue(r is ResultadoParciales.Error)
        assertEquals(1, siia.requestCount)
    }

    @Test
    fun `un 401 borra la sesion y avisa que expiro`() = runTest {
        UserSession.establecer(cuentaDePrueba(), "token-final", 480)
        responde(401, """{ "ok": false, "mensaje": "Token requerido o inválido. Inicia sesión y valida tu NIP para acceder." }""")

        val r = servicio().parciales(null)

        assertEquals(MENSAJE_SESION_EXPIRADA, (r as ResultadoParciales.Error).mensaje)
        assertNull(UserSession.token)
    }

    @Test
    fun `las finales se consultan en su endpoint con el token del alumno`() = runTest {
        responde(200, ok)
        val r = servicio().finales("2026-1")
        val peticion = siia.takeRequest()
        assertEquals("/Teschi/api/calificaciones/finales.ashx?periodo=2026-1", peticion.path)
        assertEquals("Bearer token-final", peticion.getHeader("Authorization"))
        assertTrue(r is ResultadoFinales.Datos)
    }

    @Test
    fun `si fallan las finales del periodo actual prueba los recientes y avisa`() = runTest {
        responde(500, errorGenerico)  // sin periodo
        responde(200, ok)             // 2026-2

        val r = servicio().finales(null) as ResultadoFinales.Datos

        assertEquals("/Teschi/api/calificaciones/finales.ashx", siia.takeRequest().path)
        assertEquals("/Teschi/api/calificaciones/finales.ashx?periodo=2026-2", siia.takeRequest().path)
        assertEquals("El SIIA no pudo mostrar el periodo actual; se muestra el periodo 2026-2.", r.calificaciones.aviso)
    }

    @Test
    fun `sin token las finales no llaman al SIIA`() = runTest {
        val r = servicio(token = null).finales(null)
        assertEquals(MENSAJE_SESION_EXPIRADA, (r as ResultadoFinales.Error).mensaje)
        assertEquals(0, siia.requestCount)
    }

    @Test
    fun `un 401 en finales borra la sesion`() = runTest {
        UserSession.establecer(cuentaDePrueba(), "token-final", 480)
        responde(401, """{ "ok": false, "mensaje": "Token requerido o inválido." }""")
        assertEquals(MENSAJE_SESION_EXPIRADA, (servicio().finales(null) as ResultadoFinales.Error).mensaje)
        assertNull(UserSession.token)
    }
}
