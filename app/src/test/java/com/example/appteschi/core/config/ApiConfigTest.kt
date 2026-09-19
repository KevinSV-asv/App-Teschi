package com.example.appteschi.core.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiConfigTest {

    @Test
    fun `sin url personalizada usa la url por defecto`() {
        assertEquals(
            "https://backend.trycloudflare.com",
            urlBaseEfectiva(null, "https://backend.trycloudflare.com")
        )
    }

    @Test
    fun `url personalizada en blanco tambien usa la de por defecto`() {
        assertEquals(
            "https://backend.trycloudflare.com",
            urlBaseEfectiva("   ", "https://backend.trycloudflare.com")
        )
    }

    @Test
    fun `url personalizada valida tiene prioridad sobre la de por defecto`() {
        assertEquals(
            "https://nueva-url.trycloudflare.com",
            urlBaseEfectiva("https://nueva-url.trycloudflare.com", "https://backend.trycloudflare.com")
        )
    }

    @Test
    fun `se recorta el espacio en blanco y la diagonal final`() {
        assertEquals(
            "https://nueva-url.trycloudflare.com",
            urlBaseEfectiva("  https://nueva-url.trycloudflare.com/  ", "https://backend.trycloudflare.com")
        )
    }
}
