package com.example.appteschi.service

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Respuesta de `calificaciones/parciales.ashx` con la forma real del SIIA (05/10/2026), datos ficticios. */
class CalificacionesSiiaParserTest {

    // La respuesta real empieza con "bloqueado" y no trae "ok".
    private val respuesta = """
        {
          "bloqueado": false, "periodo": "2026-1", "periodosDisponibles": ["2026-1", "2025-2", ""],
          "alumno": { "matricula": "2099000001", "nombre": "ALUMNO DE PRUEBA UNO", "carrera": "ISC", "idCarrera": 4 },
          "verificaciones": { "alumnoExiste": true, "cumpleRequisitos": true, "bloqueado": false, "pendientes": [] },
          "resumen": { "totalMaterias": 3, "materiasEvaluadas": 2, "materiasAprobadas": 1, "materiasReprobadas": 1,
                       "promedioParcial": 72.25, "promedioParcialStr": "" },
          "calificaciones": [
            { "num": 1, "idMateria": 10, "materia": "Materia A", "profesor": "PROFESOR UNO", "grupo": "4901",
              "cal1": 90, "cal1Str": "90", "cal2": 85.5, "cal2Str": "", "cal3": null, "cal3Str": "",
              "promedioParcial": 87.75, "promedioParcialStr": "87.8", "estatus": "Aprobatoria" },
            { "num": 2, "idMateria": 11, "materia": "Materia B", "profesor": "PROFESORA DOS", "grupo": "4901",
              "cal1": 60, "cal1Str": "60", "cal2": null, "cal2Str": "NP", "cal3": null, "cal3Str": "",
              "promedioParcial": null, "promedioParcialStr": "", "estatus": "Reprobatoria" },
            { "num": 3, "idMateria": 12, "materia": "Materia C", "profesor": "", "grupo": "",
              "cal1": null, "cal1Str": "", "cal2": null, "cal2Str": "", "cal3": null, "cal3Str": "",
              "promedioParcial": null, "promedioParcialStr": "", "estatus": "Sin calificar" }
          ]
        }
    """.trimIndent()

    private val bloqueada = """
        { "bloqueado": true, "mensaje": "Tienes trámites pendientes.", "periodo": "2026-1",
          "verificaciones": { "bloqueado": true, "datosPersonales": false, "pendientes": ["Actualizar datos personales"] },
          "periodosDisponibles": [], "resumen": null, "calificaciones": [] }
    """.trimIndent()

    @Test
    fun `convierte las materias con sus parciales al texto que se muestra`() {
        val c = parsearCalificacionesParciales(JSONObject(respuesta))

        assertEquals("2026-1", c.periodo)
        assertEquals(listOf("2026-1", "2025-2"), c.periodosDisponibles)
        val a = c.materias[0]
        assertEquals("90", a.parcial1)
        assertEquals("85.5", a.parcial2)
        assertEquals("—", a.parcial3)
        assertEquals("87.8", a.promedio)
        assertEquals(EstatusParcial.APROBATORIA, a.estatus)
        assertEquals("NP", c.materias[1].parcial2)
        assertEquals("—", c.materias[1].promedio)
        assertEquals(EstatusParcial.REPROBATORIA, c.materias[1].estatus)
        assertEquals(EstatusParcial.SIN_CALIFICAR, c.materias[2].estatus)
    }

    @Test
    fun `sin aviso queda vacio y con aviso se conserva`() {
        assertEquals("", parsearCalificacionesParciales(JSONObject(respuesta)).aviso)
        val aviso = "El SIIA no pudo mostrar el periodo actual; se muestra el periodo 2026-1."
        assertEquals(aviso, parsearCalificacionesParciales(JSONObject(respuesta), aviso).aviso)
    }

    @Test
    fun `resumen con promedio numerico sin texto del SIIA`() {
        val r = parsearCalificacionesParciales(JSONObject(respuesta)).resumen!!
        assertEquals(3, r.totalMaterias)
        assertEquals(2, r.evaluadas)
        assertEquals("72.3", r.promedio)
    }

    @Test
    fun `bloqueada por tramites toma los pendientes de verificaciones`() {
        val c = parsearCalificacionesParciales(JSONObject(bloqueada))
        assertTrue(c.bloqueado)
        assertEquals("Tienes trámites pendientes.", c.mensajeBloqueo)
        assertEquals(listOf("Actualizar datos personales"), c.pendientes)
        assertEquals(null, c.resumen)
        assertTrue(c.materias.isEmpty())
    }

    @Test
    fun `bloqueada sin mensaje usa uno por defecto`() {
        val c = parsearCalificacionesParciales(JSONObject("""{ "bloqueado": true }"""))
        assertEquals("Tus calificaciones están bloqueadas en el SIIA por trámites pendientes.", c.mensajeBloqueo)
    }

    // ── Interpretación de la respuesta HTTP (lo que antes hacía el backend) ──

    @Test
    fun `un 200 sin campo ok es exito`() {
        val r = interpretarParciales(RespuestaSiia(200, JSONObject(respuesta)))
        assertTrue(r is ResultadoParciales.Datos)
    }

    @Test
    fun `un 403 bloqueado se muestra como aviso, no como error`() {
        val r = interpretarParciales(RespuestaSiia(403, JSONObject(bloqueada)))
        assertTrue((r as ResultadoParciales.Datos).calificaciones.bloqueado)
    }

    @Test
    fun `un 401 dice que la sesion del SIIA expiro`() {
        val r = interpretarParciales(RespuestaSiia(401, JSONObject("""{"mensaje":"Token requerido o inválido."}""")))
        assertEquals(MENSAJE_SESION_EXPIRADA, (r as ResultadoParciales.Error).mensaje)
    }

    @Test
    fun `un 500 explica que el SIIA fallo e incluye su mensaje`() {
        val r = interpretarParciales(RespuestaSiia(500, JSONObject("""{"ok":false,"mensaje":"No fue posible procesar la solicitud."}""")))
        assertEquals(
            "El SIIA no pudo entregar tus calificaciones (el SIIA respondió: \"No fue posible procesar la solicitud.\"). Intenta más tarde.",
            (r as ResultadoParciales.Error).mensaje
        )
    }

    @Test
    fun `un 200 con ok false cuenta como error generico`() {
        assertTrue(esErrorGenericoSiia(RespuestaSiia(200, JSONObject("""{"ok":false}"""))))
        assertTrue(esErrorGenericoSiia(RespuestaSiia(200, null)))
        assertTrue(esErrorGenericoSiia(RespuestaSiia(500, null)))
        assertFalse(esErrorGenericoSiia(RespuestaSiia(200, JSONObject(respuesta))))
        assertFalse(esErrorGenericoSiia(RespuestaSiia(401, null)))
        assertFalse(esErrorGenericoSiia(RespuestaSiia(403, JSONObject(bloqueada))))
    }

    @Test
    fun `periodos recientes segun el mes`() {
        assertEquals(listOf("2026-2", "2026-1", "2025-2"), periodosRecientes(2026, 10))
        assertEquals(listOf("2025-2", "2025-1", "2024-2"), periodosRecientes(2026, 1))
        assertEquals(listOf("2026-1", "2025-2", "2025-1"), periodosRecientes(2026, 3))
        assertEquals(listOf("2026-2", "2026-1", "2025-2"), periodosRecientes(2026, 8))
    }

    private val respuestaFinales = """
        {
          "ok": true, "bloqueado": false, "periodo": "2026-1", "periodosDisponibles": ["2026-1", "2025-2"],
          "resumenFinal": { "totalMaterias": 3, "materiasEvaluadas": 2, "materiasAprobadas": 1, "materiasReprobadas": 1,
                            "promedio": 75, "promedioStr": "75.0", "promedioFinal": 75, "promedioFinalStr": "75.0", "estatus": "Regularizar" },
          "finales": [
            { "materia": "Materia A", "profesor": "PROFESOR UNO", "grupo": "4901",
              "primeraVuelta": 90, "primeraVueltaStr": "90", "segundaVuelta": null, "segundaVueltaStr": "",
              "calFinal": 90, "calFinalStr": "90", "estatus": "Aprobatoria", "estatusFinal": "Aprobatoria" },
            { "materia": "Materia B", "profesor": "PROFESORA DOS", "grupo": "4901",
              "primeraVuelta": null, "primeraVueltaStr": "NP", "segundaVuelta": 60, "segundaVueltaStr": "60",
              "calFinal": 60, "calFinalStr": "60", "estatus": "Reprobatoria", "estatusFinal": "Reprobatoria" },
            { "materia": "Materia C", "profesor": "", "grupo": "",
              "primeraVuelta": null, "segundaVuelta": null, "calFinal": null, "calFinalStr": "", "estatusFinal": "Sin calificar" }
          ]
        }
    """

    @Test
    fun `lee las finales con 1ra y 2da vuelta`() {
        val f = parsearCalificacionesFinales(JSONObject(respuestaFinales))
        assertEquals("2026-1", f.periodo)
        assertEquals(listOf("2026-1", "2025-2"), f.periodosDisponibles)
        assertEquals(MateriaFinal("Materia A", "PROFESOR UNO", "4901", "90", "—", "90", EstatusParcial.APROBATORIA), f.materias[0])
        assertEquals(MateriaFinal("Materia B", "PROFESORA DOS", "4901", "NP", "60", "60", EstatusParcial.REPROBATORIA), f.materias[1])
        assertEquals(MateriaFinal("Materia C", "", "", "—", "—", "—", EstatusParcial.SIN_CALIFICAR), f.materias[2])
    }

    @Test
    fun `lee el resumen de finales con su dictamen`() {
        val r = parsearCalificacionesFinales(JSONObject(respuestaFinales)).resumen!!
        assertEquals(3, r.totalMaterias)
        assertEquals(2, r.evaluadas)
        assertEquals(1, r.aprobadas)
        assertEquals(1, r.reprobadas)
        assertEquals("75.0", r.promedio)
        assertEquals("Regularizar", r.dictamen)
    }

    @Test
    fun `acepta los otros nombres de cada vuelta y las finales dentro de calificaciones`() {
        val f = parsearCalificacionesFinales(
            JSONObject(
                """{ "periodo": "2026-1",
                     "resumen": { "totalMaterias": 1, "materiasEvaluadasFinal": 1, "materiasAprobadasFinal": 1,
                                  "materiasReprobadasFinal": 0, "promedioFinal": 88.5, "estatusFinal": "Aprobatorio" },
                     "calificaciones": [ { "materia": "Materia A", "calPromedio": 70, "segVuelta": 88.5, "final": 88.5, "estatusFinal": "Aprobatoria" } ] }"""
            )
        )
        val m = f.materias.single()
        assertEquals("70", m.primeraVuelta)
        assertEquals("88.5", m.segundaVuelta)
        assertEquals("88.5", m.calificacionFinal)
        assertEquals(EstatusParcial.APROBATORIA, m.estatus)
        assertEquals("88.5", f.resumen!!.promedio)
        assertEquals("Aprobatorio", f.resumen!!.dictamen)
    }

    @Test
    fun `un 403 en finales es un aviso con los tramites pendientes`() {
        val r = interpretarFinales(
            RespuestaSiia(403, JSONObject("""{ "ok": false, "bloqueado": true, "mensaje": "Trámites pendientes.",
                "verificaciones": { "pendientes": ["Encuesta"] }, "finales": [ { "materia": "No debe verse" } ] }"""))
        ) as ResultadoFinales.Datos
        assertTrue(r.calificaciones.bloqueado)
        assertEquals("Trámites pendientes.", r.calificaciones.mensajeBloqueo)
        assertEquals(listOf("Encuesta"), r.calificaciones.pendientes)
        assertTrue(r.calificaciones.materias.isEmpty())
    }

    @Test
    fun `un 500 en finales lo dice con esas palabras`() {
        val r = interpretarFinales(RespuestaSiia(500, null)) as ResultadoFinales.Error
        assertEquals("El SIIA no pudo entregar tus calificaciones finales. Intenta más tarde.", r.mensaje)
    }
}
