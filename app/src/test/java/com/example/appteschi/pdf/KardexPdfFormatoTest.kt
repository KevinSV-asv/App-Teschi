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
            promedioGlobal = 86.42,
            promedioUltimoSemestre = 91.75,
            totalCreditosCarrera = 260,
            totalCreditosCursados = 240,
            porcentajeCubierto = 92.31,
            totalMaterias = 53,
            materiasAprobadas = 51,
            materiasReprobadas = 0,
            materiasPorAprobar = 2
        )

        val encabezado = KardexPdfFormato.encabezado(
            uiState = estado,
            matricula = "2099000001",
            nombre = "prueba uno alumno",
            fechaElaboracion = "07-09-2026"
        )

        assertEquals("2099000001", encabezado.matricula)
        assertEquals("PRUEBA UNO ALUMNO", encabezado.nombre)
        assertEquals("INGENIERÍA EN SISTEMAS COMPUTACIONALES", encabezado.carrera)
        assertEquals("20", encabezado.creditosPorCursar)
        assertEquals("92.31 %", encabezado.porcentajeCubierto)
        assertEquals("91.75", encabezado.promedioUltimoSemestre)
        assertEquals("86.42", encabezado.promedioGlobal)
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
