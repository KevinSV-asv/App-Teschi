package com.example.appteschi.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Valida PlanDeEstudiosIsc contra el Mapa Curricular oficial de ISC (fuente
 * independiente del Kardex — si algún día se editara el catálogo por error,
 * esta prueba lo detecta antes de que Kardex/Tira de Materias muestren datos
 * incorrectos para algún semestre).
 */
class PlanDeEstudiosIscTest {

    @Test
    fun `coincide exactamente con el Mapa Curricular oficial de ISC, semestre por semestre`() {
        val oficial: Map<Int, Set<Pair<String, Int>>> = mapOf(
            1 to setOf(
                "Cálculo Diferencial" to 5,
                "Fundamentos de Programación" to 5,
                "Taller de Ética" to 4,
                "Matemáticas Discretas" to 5,
                "Taller de Administración" to 4,
                "Fundamentos de Investigación" to 4
            ),
            2 to setOf(
                "Cálculo Integral" to 5,
                "Programación Orientada a Objetos" to 5,
                "Contabilidad Financiera" to 4,
                "Química" to 4,
                "Álgebra Lineal" to 5,
                "Probabilidad y Estadística" to 5
            ),
            3 to setOf(
                "Cálculo Vectorial" to 5,
                "Estructura de Datos" to 5,
                "Cultura Empresarial" to 4,
                "Investigación de Operaciones" to 4,
                "Desarrollo Sustentable" to 5,
                "Física General" to 5
            ),
            4 to setOf(
                "Ecuaciones Diferenciales" to 5,
                "Métodos Numéricos" to 4,
                "Tópicos Avanzados de Programación" to 5,
                "Fundamento de Base de Datos" to 5,
                "Simulación" to 5,
                "Principios Eléctricos y Aplicaciones Digitales" to 5
            ),
            5 to setOf(
                "Graficación" to 4,
                "Fundamentos de Telecomunicaciones" to 4,
                "Sistemas Operativos" to 4,
                "Taller de Base de Datos" to 4,
                "Fundamentos de Ingeniería de Software" to 4,
                "Arquitectura de Computadoras" to 5
            ),
            6 to setOf(
                "Lenguajes y Autómatas I" to 5,
                "Redes de Computadoras" to 5,
                "Taller de Sistemas Operativos" to 4,
                "Administración de Base de Datos" to 5,
                "Ingeniería de Software" to 5,
                "Lenguajes de Interfaz" to 4,
                "Tecnologías Emergentes de Base de Datos" to 6
            ),
            7 to setOf(
                "Lenguajes y Autómatas II" to 5,
                "Conmutación y Enrutamiento en Redes de Datos" to 5,
                "Taller de Investigación I" to 4,
                "Programación Web" to 5,
                "Gestión de Proyectos de Software" to 6,
                "Sistemas Programables" to 4,
                "Minería de Datos" to 6
            ),
            8 to setOf(
                "Programación Lógica y Funcional" to 4,
                "Administración de Redes" to 4,
                "Taller de Investigación II" to 4,
                "Ingeniería del Conocimiento" to 6,
                "Inteligencia de Negocios y Analítica de Negocios" to 7,
                "Inteligencia Artificial" to 4,
                "Servicio Social" to 10
            ),
            9 to setOf(
                "Residencias Profesionales" to 10,
                "Actividades Complementarias" to 5
            )
        )

        oficial.forEach { (semestre, materiasEsperadas) ->
            val materiasCatalogo = PlanDeEstudiosIsc.materias
                .filter { it.semestre == semestre }
                .map { it.nombre to it.creditos }
                .toSet()
            assertEquals("Semestre $semestre no coincide con el Mapa Curricular oficial", materiasEsperadas, materiasCatalogo)
        }
    }

    @Test
    fun `total de creditos de la carrera es 260, igual que en el Kardex oficial`() {
        assertEquals(260, PlanDeEstudiosIsc.materias.sumOf { it.creditos })
    }

    @Test
    fun `53 materias en total, igual que en el Kardex oficial`() {
        assertEquals(53, PlanDeEstudiosIsc.materias.size)
    }
}
