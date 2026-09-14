package com.example.appteschi.pdf

import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.viewmodel.KardexUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class KardexPdfFormatoTest {

    @Test
    fun `formatearNumero quita decimales cuando el valor es entero`() {
        assertEquals("85", KardexPdfFormato.formatearNumero(85.0))
        assertEquals("100", KardexPdfFormato.formatearNumero(100.0))
        assertEquals("0", KardexPdfFormato.formatearNumero(0.0))
    }

    @Test
    fun `formatearNumero conserva decimales cuando el valor no es entero, igual que el Kardex oficial`() {
        assertEquals("91.33", KardexPdfFormato.formatearNumero(91.33))
        assertEquals("99.2", KardexPdfFormato.formatearNumero(99.2))
        assertEquals("86.92", KardexPdfFormato.formatearNumero(86.92))
    }

    @Test
    fun `formatearCalificacion devuelve vacio cuando la materia no tiene calificacion`() {
        assertEquals("", KardexPdfFormato.formatearCalificacion(null))
        assertEquals("95", KardexPdfFormato.formatearCalificacion(95.0))
    }

    @Test
    fun `encabezado calcula creditos por cursar y formatea el porcentaje como el documento oficial`() {
        val estado = KardexUiState(
            cargando = false,
            carrera = "Ingeniería en Sistemas Computacionales",
            periodoIngreso = "2022-2",
            ultimoPeriodo = "2026-1",
            promedioGlobal = 88.63,
            promedioUltimoSemestre = 99.29,
            totalCreditosCarrera = 260,
            totalCreditosCursados = 250,
            porcentajeCubierto = 96.15,
            totalMaterias = 53,
            materiasAprobadas = 52,
            materiasReprobadas = 0,
            materiasPorAprobar = 1
        )

        val encabezado = KardexPdfFormato.encabezado(
            uiState = estado,
            matricula = "2022452166",
            nombre = "sanchez vargas kevin antonio",
            fechaElaboracion = "07-09-2026"
        )

        assertEquals("2022452166", encabezado.matricula)
        assertEquals("SANCHEZ VARGAS KEVIN ANTONIO", encabezado.nombre)
        assertEquals("INGENIERÍA EN SISTEMAS COMPUTACIONALES", encabezado.carrera)
        assertEquals("10", encabezado.creditosPorCursar)
        assertEquals("96.15 %", encabezado.porcentajeCubierto)
        assertEquals("99.29", encabezado.promedioUltimoSemestre)
        assertEquals("88.63", encabezado.promedioGlobal)
        assertEquals("07-09-2026", encabezado.fechaElaboracion)
    }

    @Test
    fun `filas preserva el numero real y usa el codigo de estatus en Cursada, igual que AP o PC en el documento`() {
        val materias = listOf(
            MateriaHistorial(
                numero = 1,
                nombre = "cálculo diferencial",
                creditos = 5,
                semestre = 1,
                calificacion = 85.0,
                periodo = "2022-2",
                estatus = EstatusMateria.APROBADA
            ),
            MateriaHistorial(
                numero = 53,
                nombre = "residencias profesionales",
                creditos = 10,
                semestre = 9,
                calificacion = null,
                periodo = null,
                estatus = EstatusMateria.POR_CURSAR
            )
        )

        val filas = KardexPdfFormato.filas(materias)

        assertEquals(1, filas[0].numero)
        assertEquals("CÁLCULO DIFERENCIAL", filas[0].materia)
        assertEquals("85", filas[0].calificacionEvaluacion)
        assertEquals("2022-2", filas[0].periodoEvaluacion)
        assertEquals("AP", filas[0].cursada)

        assertEquals(53, filas[1].numero)
        assertEquals("", filas[1].calificacionEvaluacion)
        assertEquals("", filas[1].periodoEvaluacion)
        assertEquals("PC", filas[1].cursada)
    }
}
