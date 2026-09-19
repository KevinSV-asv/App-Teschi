package com.example.appteschi.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GruposIscTest {

    @Test
    fun `deriva turno vespertino del grupo real del comprobante`() {
        assertEquals("Vespertino", GruposIsc.turno("9ISC23"))
        assertEquals(9, GruposIsc.semestre("9ISC23"))
    }

    @Test
    fun `deriva turno matutino correctamente`() {
        assertEquals("Matutino", GruposIsc.turno("1ISC11"))
    }

    @Test
    fun `codigo invalido no revienta, devuelve null`() {
        assertNull(GruposIsc.turno("invalido"))
        assertNull(GruposIsc.semestre("invalido"))
    }

    @Test
    fun `todos los codigos del catalogo son parseables`() {
        GruposIsc.codigos.forEach { codigo ->
            assertEquals("turno nulo para $codigo", false, GruposIsc.turno(codigo) == null)
            assertEquals("semestre nulo para $codigo", false, GruposIsc.semestre(codigo) == null)
        }
    }
}
