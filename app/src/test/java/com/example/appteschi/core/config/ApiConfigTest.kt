package com.example.appteschi.core.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiConfigTest {

    @Test
    fun `por defecto apunta a la API oficial del SIIA por HTTPS`() {
        val e = SiiaEndpoints()
        assertEquals("https://siia.teschi.edu.mx/Teschi/api/login.ashx", e.login)
        assertEquals("https://siia.teschi.edu.mx/Teschi/api/nip.ashx", e.nip)
    }

    @Test
    fun `sin periodo no manda el parametro`() {
        assertEquals(
            "https://siia.teschi.edu.mx/Teschi/api/calificaciones/parciales.ashx",
            SiiaEndpoints().calificacionesParciales(null)
        )
        assertEquals(
            "https://siia.teschi.edu.mx/Teschi/api/calificaciones/parciales.ashx",
            SiiaEndpoints().calificacionesParciales("  ")
        )
    }

    @Test
    fun `con periodo lo agrega codificado`() {
        assertEquals(
            "https://siia.teschi.edu.mx/Teschi/api/calificaciones/parciales.ashx?periodo=2026-1",
            SiiaEndpoints().calificacionesParciales("2026-1")
        )
        assertEquals(
            "https://siia.teschi.edu.mx/Teschi/api/calificaciones/parciales.ashx?periodo=2026+1%26x",
            SiiaEndpoints().calificacionesParciales("2026 1&x")
        )
    }

    @Test
    fun `una base con diagonal final no duplica la diagonal`() {
        assertEquals("http://localhost:8080/api/login.ashx", SiiaEndpoints("http://localhost:8080/api/").login)
    }

    @Test
    fun `finales usa su propio endpoint con el periodo opcional`() {
        assertEquals(
            "https://siia.teschi.edu.mx/Teschi/api/calificaciones/finales.ashx",
            SiiaEndpoints().calificacionesFinales(null)
        )
        assertEquals(
            "https://siia.teschi.edu.mx/Teschi/api/calificaciones/finales.ashx?periodo=2026-1",
            SiiaEndpoints().calificacionesFinales("2026-1")
        )
    }

    @Test
    fun `el kardex y su PDF no llevan datos del alumno en la URL`() {
        assertEquals("https://siia.teschi.edu.mx/Teschi/api/calificaciones/kardex.ashx", SiiaEndpoints().kardex)
        assertEquals("https://siia.teschi.edu.mx/Teschi/api/calificaciones/kardex-pdf.ashx?base64=1", SiiaEndpoints().kardexPdf)
    }

    @Test
    fun `el portal del SIIA va por HTTPS`() {
        assertEquals("https://siia.teschi.edu.mx/Teschi/", ApiConfig.SIIA_PORTAL_URL)
    }
}
