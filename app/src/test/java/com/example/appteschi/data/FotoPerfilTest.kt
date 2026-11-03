package com.example.appteschi.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FotoPerfilTest {

    @Test
    fun `una imagen chica no se reduce`() {
        assertEquals(1, muestreoFoto(300, 200))
        assertEquals(1, muestreoFoto(1023, 800))
    }

    @Test
    fun `una imagen grande se reduce sin bajar del tamano final`() {
        assertEquals(2, muestreoFoto(1024, 1024))
        assertEquals(4, muestreoFoto(3000, 2100))
        assertEquals(8, muestreoFoto(8000, 6000))
    }

    @Test
    fun `una imagen muy alargada no se lee completa`() {
        assertEquals(8, muestreoFoto(30000, 600))
        assertEquals(8, muestreoFoto(600, 30000))
    }

    @Test
    fun `el nombre del archivo es estable y distinto por cuenta`() {
        assertEquals(nombreArchivoFoto("2099000001"), nombreArchivoFoto("2099000001"))
        assertNotEquals(nombreArchivoFoto("2099000001"), nombreArchivoFoto("2099000002"))
    }

    @Test
    fun `el nombre no trae la matricula ni puede salirse de la carpeta`() {
        val nombre = nombreArchivoFoto("../../2099000001")
        assertFalse(nombre.contains("2099000001"))
        assertFalse(nombre.contains("/"))
        assertFalse(nombre.contains(".."))
        assertTrue(nombre.endsWith(".jpg"))
    }

    @Test
    fun `endereza cada orientacion de la camara`() {
        assertEquals(GiroExif(0, false), giroExif(1))
        assertEquals(GiroExif(0, true), giroExif(2))
        assertEquals(GiroExif(180, false), giroExif(3))
        assertEquals(GiroExif(180, true), giroExif(4))
        assertEquals(GiroExif(90, true), giroExif(5))
        assertEquals(GiroExif(90, false), giroExif(6))
        assertEquals(GiroExif(270, true), giroExif(7))
        assertEquals(GiroExif(270, false), giroExif(8))
        assertEquals(GiroExif(0, false), giroExif(0))
    }

    @Test
    fun `sin iniciar no hay foto ni se puede guardar`() {
        assertNull(FotoPerfilArchivos.ruta("2099000001"))
    }
}
