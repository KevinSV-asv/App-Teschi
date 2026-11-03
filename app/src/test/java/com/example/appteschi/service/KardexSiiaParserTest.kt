package com.example.appteschi.service

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Respuesta de `calificaciones/kardex.ashx` según el esquema de la API v1.2, con datos ficticios. */
class KardexSiiaParserTest {

    private val materiaAprobada = """
        { "num": 1, "clave": "MAT-0001", "nombre": "Materia A", "materia": "Materia A", "creditos": 5, "semestre": 1,
          "calOrd": "90", "calOrdNum": 90, "calOrdStr": "90", "periodoOrd": "2024-1",
          "calRec": null, "calRecStr": "", "periodoRec": null,
          "calEspecial": null, "calEspecialStr": "", "periodoEsp": null,
          "calGlobal": null, "calGlobalStr": "", "periodoGlo": null,
          "calificacion": 90, "calificacionStr": "90", "cursada": "AP", "estatus": "Aprobada", "aprobada": true }
    """

    private val materiaEnRecurse = """
        { "clave": "MAT-0002", "nombre": "Materia B", "creditos": 4.5, "semestre": 1,
          "calOrd": "60", "calOrdStr": "60", "periodoOrd": "2024-1",
          "calRec": 80, "calRecStr": "", "periodoRec": "2024-2",
          "calificacion": 80, "calificacionStr": "80", "cursada": "AP", "estatus": "Aprobada", "aprobada": true }
    """

    private val materiaAcreditada = """
        { "clave": "ACA-0001", "nombre": "Actividad complementaria", "creditos": 2, "semestre": 2,
          "calOrd": "ACA", "calOrdStr": "", "periodoOrd": "2024-2 EQ",
          "calificacion": null, "calificacionStr": "", "cursada": "AP", "estatus": "Aprobada", "aprobada": true }
    """

    private val materiaPorCursar = """
        { "clave": "MAT-0003", "nombre": "Materia C", "creditos": 5, "semestre": 2,
          "calOrd": null, "calOrdStr": "", "periodoOrd": null,
          "calificacion": null, "calificacionStr": "", "cursada": "PC", "estatus": "Por cursar", "aprobada": false }
    """

    private val respuesta = """
        {
          "ok": true, "bloqueado": false, "periodoActivo": "2026-2",
          "reportePdfUrl": "/api/calificaciones/kardex-pdf.ashx?matricula=2099000001",
          "datosPersonales": {
            "matricula": "2099000001", "alumno": "ALUMNO DE PRUEBA UNO", "curp": "XAXX000000HDFXXX00",
            "carrera": "INGENIERÍA EN SISTEMAS COMPUTACIONALES", "especialidad": "", "planEstudios": 2010,
            "situacion": "Regular", "grupo": "4901", "periodoIngreso": "2024-1", "ultimoPeriodo": "2026-1",
            "creditosCarrera": 260, "creditosCursados": 130, "creditosPorCursar": 130, "porcentajeStr": "50.00 %",
            "totalMateriasCarrera": 53, "materiasAprobadas": 3, "materiasReprobadas": 1, "materiasPorAprobar": 50, "materiasPorCursar": 40,
            "promedioGlobal": 85.5, "promedioGlobalStr": "85.50", "promedioLetra": "OCHENTA Y CINCO",
            "promedioSemestreAnterior": 88.4, "promedioSemestreAnteriorStr": "88.4", "parcial": null, "descuento": null
          },
          "semestres": [
            { "semestre": 1, "nombreSemestre": "Semestre 1", "promedioSemestre": 85, "promedioSemestreStr": "85.0",
              "materias": [ $materiaAprobada, $materiaEnRecurse ] },
            { "semestre": 2, "nombreSemestre": "", "promedioSemestre": null, "promedioSemestreStr": "",
              "materias": [ $materiaAcreditada, $materiaPorCursar ] }
          ],
          "simbologia": { "PC": "Por cursar", "AP": "Aprobada", "NA": "No Aprobó", "ZZ": "Otra" }
        }
    """

    @Test
    fun `lee el encabezado del alumno`() {
        val d = parsearKardex(JSONObject(respuesta)).datos!!
        assertEquals("2099000001", d.matricula)
        assertEquals("ALUMNO DE PRUEBA UNO", d.nombre)
        assertEquals("2010", d.planEstudios)
        assertEquals("", d.especialidad)
        assertEquals("Regular", d.situacion)
        assertEquals("2024-1", d.periodoIngreso)
        assertEquals("2026-1", d.ultimoPeriodo)
        assertEquals("85.50", d.promedioGlobal)
        assertEquals("OCHENTA Y CINCO", d.promedioLetra)
        assertEquals("130", d.creditosCursados)
        assertEquals("260", d.creditosCarrera)
        assertEquals("50.00 %", d.porcentaje)
        assertEquals(0.5f, d.avance!!, 0.001f)
        assertEquals(3, d.aprobadas)
        assertEquals(1, d.reprobadas)
        assertEquals(40, d.porCursar)
    }

    @Test
    fun `trae todos los datos del historial oficial`() {
        val d = parsearKardex(JSONObject(respuesta)).datos!!
        assertEquals("XAXX000000HDFXXX00", d.curp)
        assertEquals("4901", d.grupo)
        assertEquals("130", d.creditosPorCursar)
        assertEquals(53, d.totalMaterias)
        assertEquals(50, d.porAprobar)
        assertEquals("88.4", d.promedioSemestreAnterior)
        assertEquals("", d.parcial)
        assertEquals("", d.descuento)
    }

    @Test
    fun `parcial y descuento se leen si el SIIA los da`() {
        val d = parsearKardex(JSONObject("""{ "datosPersonales": { "parcial": 87.5, "descuento": "10 %" } }""")).datos!!
        assertEquals("87.5", d.parcial)
        assertEquals("10 %", d.descuento)
    }

    @Test
    fun `sin totales los calcula con las materias`() {
        val d = parsearKardex(
            JSONObject(
                """{ "datosPersonales": { "alumno": "X" }, "creditosFaltantes": 1,
                     "kardex": [ { "nombre": "M", "semestre": 1, "cursada": "AP" }, { "nombre": "N", "semestre": 1, "cursada": "PC" } ] }"""
            )
        ).datos!!
        assertEquals(2, d.totalMaterias)
        assertEquals(1, d.porAprobar)
        assertEquals("—", d.creditosPorCursar)
        assertEquals("—", d.promedioSemestreAnterior)
    }

    @Test
    fun `la fecha de elaboracion usa el formato del reporte oficial`() {
        val fecha = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.NOVEMBER, 2) }
        assertEquals("02-11-2026", fechaElaboracion(fecha))
        assertEquals("02-11-2026", parsearKardex(JSONObject(respuesta), "02-11-2026").fechaElaboracion)
    }

    @Test
    fun `cada materia trae su numero del reporte y su clave oficial`() {
        val m = parsearKardex(JSONObject(respuesta)).semestres[0].materias[0]
        assertEquals(1, m.numero)
        assertEquals("AP", m.estatus.clave)
        assertEquals(null, parsearKardex(JSONObject(respuesta)).semestres[0].materias[1].numero)
    }

    @Test
    fun `agrupa por semestre con su promedio`() {
        val s = parsearKardex(JSONObject(respuesta)).semestres
        assertEquals(listOf(1, 2), s.map { it.numero })
        assertEquals("Semestre 1", s[0].nombre)
        assertEquals("85.0", s[0].promedio)
        assertEquals(85.0, s[0].promedioNumero!!, 0.001)
        assertEquals("Semestre 2", s[1].nombre)
        assertEquals("—", s[1].promedio)
        assertNull(s[1].promedioNumero)
    }

    @Test
    fun `una materia en ordinario solo trae esa oportunidad`() {
        val m = parsearKardex(JSONObject(respuesta)).semestres[0].materias[0]
        assertEquals("MAT-0001", m.clave)
        assertEquals("Materia A", m.nombre)
        assertEquals("5", m.creditos)
        assertEquals("90", m.calificacion)
        assertEquals(EstatusKardex.APROBADA, m.estatus)
        assertEquals(listOf(OportunidadKardex("Ordinario", "90", "2024-1")), m.oportunidades)
    }

    @Test
    fun `una materia recursada muestra cada intento y la calificacion definitiva`() {
        val m = parsearKardex(JSONObject(respuesta)).semestres[0].materias[1]
        assertEquals("4.5", m.creditos)
        assertEquals("80", m.calificacion)
        assertEquals(
            listOf(OportunidadKardex("Ordinario", "60", "2024-1"), OportunidadKardex("Recurse", "80", "2024-2")),
            m.oportunidades
        )
    }

    @Test
    fun `una actividad acreditada muestra ACA y conserva el sufijo del periodo`() {
        val m = parsearKardex(JSONObject(respuesta)).semestres[1].materias[0]
        assertEquals("ACA", m.calificacion)
        assertEquals(listOf(OportunidadKardex("Ordinario", "ACA", "2024-2 EQ")), m.oportunidades)
    }

    @Test
    fun `una materia por cursar no tiene calificacion ni oportunidades`() {
        val m = parsearKardex(JSONObject(respuesta)).semestres[1].materias[1]
        assertEquals("—", m.calificacion)
        assertEquals(EstatusKardex.POR_CURSAR, m.estatus)
        assertTrue(m.oportunidades.isEmpty())
    }

    @Test
    fun `sin la clave cursada usa el estatus aunque venga con mala codificacion`() {
        fun estatus(campos: String) = parsearKardex(
            JSONObject("""{ "kardex": [ { "nombre": "M", "semestre": 1, $campos } ] }""")
        ).semestres[0].materias[0].estatus

        assertEquals(EstatusKardex.NO_APROBADA, estatus(""" "cursada": "NA" """))
        assertEquals(EstatusKardex.NO_APROBADA, estatus(""" "estatus": "No Aprob�" """))
        assertEquals(EstatusKardex.APROBADA, estatus(""" "estatus": "Aprobada" """))
        assertEquals(EstatusKardex.APROBADA, estatus(""" "aprobada": true """))
        assertEquals(EstatusKardex.POR_CURSAR, estatus(""" "estatus": "Por cursar" """))
    }

    @Test
    fun `sin semestres agrupa la lista plana en orden`() {
        val s = parsearKardex(
            JSONObject("""{ "materias": [ { "nombre": "M2", "semestre": 2 }, { "nombre": "M1", "semestre": 1 } ] }""")
        ).semestres
        assertEquals(listOf(1, 2), s.map { it.numero })
        assertEquals("M1", s[0].materias[0].nombre)
        assertEquals("Semestre 2", s[1].nombre)
    }

    @Test
    fun `acepta los otros nombres del encabezado y cuenta lo que no venga`() {
        val d = parsearKardex(
            JSONObject(
                """{ "resumen": { "nombre": "ALUMNO DE PRUEBA UNO", "ingreso": "2024-1", "perUltimo": "2026-1",
                     "totalCreditosCarrera": 200, "porcentaje": 25.5, "totalMateriasApro": 7, "promedioFinal": 81 },
                     "kardex": [ { "nombre": "M", "semestre": 1, "cursada": "NA" }, { "nombre": "N", "semestre": 1, "cursada": "PC" } ] }"""
            )
        ).datos!!
        assertEquals("ALUMNO DE PRUEBA UNO", d.nombre)
        assertEquals("2024-1", d.periodoIngreso)
        assertEquals("2026-1", d.ultimoPeriodo)
        assertEquals("200", d.creditosCarrera)
        assertEquals("—", d.creditosCursados)
        assertEquals(0.255f, d.avance!!, 0.001f)
        assertEquals("25.5 %", d.porcentaje)
        assertEquals(7, d.aprobadas)
        assertEquals(1, d.reprobadas)
        assertEquals(1, d.porCursar)
        assertEquals("81", d.promedioGlobal)
    }

    @Test
    fun `sin datos de avance no inventa uno`() {
        val d = parsearKardex(JSONObject("""{ "datosPersonales": { "alumno": "X" } }""")).datos!!
        assertNull(d.avance)
        assertEquals("", d.porcentaje)
        assertEquals("—", d.promedioGlobal)
    }

    @Test
    fun `la simbologia sale en orden fijo y conserva claves nuevas`() {
        assertEquals(
            listOf("AP" to "Aprobada", "NA" to "No Aprobó", "PC" to "Por cursar", "ZZ" to "Otra"),
            parsearKardex(JSONObject(respuesta)).simbologia
        )
    }

    @Test
    fun `una respuesta sin materias no truena`() {
        val k = parsearKardex(JSONObject("""{ "ok": true }"""))
        assertFalse(k.bloqueado)
        assertNull(k.datos)
        assertTrue(k.semestres.isEmpty())
        assertTrue(k.simbologia.isEmpty())
    }
}
