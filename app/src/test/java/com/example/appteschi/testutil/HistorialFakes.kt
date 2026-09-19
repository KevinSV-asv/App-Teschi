package com.example.appteschi.testutil

import com.example.appteschi.data.CarreraCatalogo
import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.GrupoInfo
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.data.MateriaPlan
import com.example.appteschi.service.HistorialAcademicoApi
import com.example.appteschi.service.HistorialAlumnoInfo
import com.example.appteschi.service.PlanEstudiosApi

/** Fake sin red real — el mismo patrón que AuthFakes.kt. */
class FakePlanEstudiosApi(
    private val resultadoCarreras: Result<List<CarreraCatalogo>> = Result.success(emptyList()),
    private val resultadoMaterias: Result<List<MateriaPlan>> = Result.success(emptyList()),
    private val resultadoGrupos: Result<List<GrupoInfo>> = Result.success(emptyList())
) : PlanEstudiosApi {
    override suspend fun carreras(): Result<List<CarreraCatalogo>> = resultadoCarreras
    override suspend fun materias(claveCarrera: String): Result<List<MateriaPlan>> = resultadoMaterias
    override suspend fun grupos(claveCarrera: String): Result<List<GrupoInfo>> = resultadoGrupos
}

/** Fake sin red real — el mismo patrón que AuthFakes.kt/ReinscripcionViewModelTest. */
class FakeHistorialAcademicoApi(
    private val resultado: Result<HistorialAlumnoInfo> = Result.failure(IllegalStateException("no configurado"))
) : HistorialAcademicoApi {
    var llamadas = 0
        private set
    var ultimaMatricula: String? = null
        private set

    override suspend fun historial(matricula: String): Result<HistorialAlumnoInfo> {
        llamadas++
        ultimaMatricula = matricula
        return resultado
    }
}

/**
 * Historial ficticio de un alumno de Ingeniería en Sistemas Computacionales, con las
 * 53 materias del plan de estudios (información pública) y calificaciones sintéticas.
 * Las pruebas fijan aquí el fixture para verificar la lógica del ViewModel sin
 * depender de una base de datos real ni de datos de ninguna persona.
 */
private val gradosIsc: List<Triple<String, Int, Int>> = listOf(
    Triple("Cálculo Diferencial", 5, 1), Triple("Fundamentos de Investigación", 4, 1),
    Triple("Fundamentos de Programación", 5, 1), Triple("Matemáticas Discretas", 5, 1),
    Triple("Taller de Administración", 4, 1), Triple("Taller de Ética", 4, 1),
    Triple("Álgebra Lineal", 5, 2), Triple("Cálculo Integral", 5, 2),
    Triple("Contabilidad Financiera", 4, 2), Triple("Probabilidad y Estadística", 5, 2),
    Triple("Programación Orientada a Objetos", 5, 2), Triple("Química", 4, 2),
    Triple("Cálculo Vectorial", 5, 3), Triple("Cultura Empresarial", 4, 3),
    Triple("Desarrollo Sustentable", 5, 3), Triple("Estructura de Datos", 5, 3),
    Triple("Física General", 5, 3), Triple("Investigación de Operaciones", 4, 3),
    Triple("Ecuaciones Diferenciales", 5, 4), Triple("Fundamento de Base de Datos", 5, 4),
    Triple("Métodos Numéricos", 4, 4), Triple("Principios Eléctricos y Aplicaciones Digitales", 5, 4),
    Triple("Simulación", 5, 4), Triple("Tópicos Avanzados de Programación", 5, 4),
    Triple("Arquitectura de Computadoras", 5, 5), Triple("Fundamentos de Ingeniería de Software", 4, 5),
    Triple("Fundamentos de Telecomunicaciones", 4, 5), Triple("Graficación", 4, 5),
    Triple("Sistemas Operativos", 4, 5), Triple("Taller de Base de Datos", 4, 5),
    Triple("Administración de Base de Datos", 5, 6), Triple("Ingeniería de Software", 5, 6),
    Triple("Lenguajes de Interfaz", 4, 6), Triple("Lenguajes y Autómatas I", 5, 6),
    Triple("Redes de Computadoras", 5, 6), Triple("Taller de Sistemas Operativos", 4, 6),
    Triple("Tecnologías Emergentes de Base de Datos", 6, 6),
    Triple("Conmutación y Enrutamiento en Redes de Datos", 5, 7), Triple("Gestión de Proyectos de Software", 6, 7),
    Triple("Lenguajes y Autómatas II", 5, 7), Triple("Minería de Datos", 6, 7),
    Triple("Programación Web", 5, 7), Triple("Sistemas Programables", 4, 7),
    Triple("Taller de Investigación I", 4, 7),
    Triple("Administración de Redes", 4, 8), Triple("Ingeniería del Conocimiento", 6, 8),
    Triple("Inteligencia Artificial", 4, 8), Triple("Inteligencia de Negocios y Analítica de Negocios", 7, 8),
    Triple("Programación Lógica y Funcional", 4, 8), Triple("Servicio Social", 10, 8),
    Triple("Taller de Investigación II", 4, 8),
    Triple("Actividades Complementarias", 5, 9), Triple("Residencias Profesionales", 10, 9)
)

private val calificacionesIsc: Map<String, Double> = mapOf(
    "Cálculo Diferencial" to 77.0, "Fundamentos de Investigación" to 90.0, "Fundamentos de Programación" to 72.0,
    "Matemáticas Discretas" to 85.0, "Taller de Administración" to 98.0, "Taller de Ética" to 80.0,
    "Álgebra Lineal" to 93.0, "Cálculo Integral" to 75.0, "Contabilidad Financiera" to 88.0,
    "Probabilidad y Estadística" to 70.0, "Programación Orientada a Objetos" to 83.0, "Química" to 96.0,
    "Cálculo Vectorial" to 78.0, "Cultura Empresarial" to 91.0, "Desarrollo Sustentable" to 73.0,
    "Estructura de Datos" to 86.0, "Física General" to 99.0, "Investigación de Operaciones" to 81.0,
    "Ecuaciones Diferenciales" to 94.0, "Fundamento de Base de Datos" to 76.0, "Métodos Numéricos" to 89.0,
    "Principios Eléctricos y Aplicaciones Digitales" to 71.0, "Simulación" to 84.0,
    "Tópicos Avanzados de Programación" to 97.0, "Arquitectura de Computadoras" to 79.0,
    "Fundamentos de Ingeniería de Software" to 92.0, "Fundamentos de Telecomunicaciones" to 74.0,
    "Graficación" to 87.0, "Sistemas Operativos" to 100.0, "Taller de Base de Datos" to 82.0,
    "Administración de Base de Datos" to 95.0, "Ingeniería de Software" to 77.0, "Lenguajes de Interfaz" to 90.0,
    "Lenguajes y Autómatas I" to 72.0, "Redes de Computadoras" to 85.0, "Taller de Sistemas Operativos" to 98.0,
    "Tecnologías Emergentes de Base de Datos" to 80.0, "Conmutación y Enrutamiento en Redes de Datos" to 93.0,
    "Gestión de Proyectos de Software" to 75.0, "Lenguajes y Autómatas II" to 88.0, "Minería de Datos" to 70.0,
    "Programación Web" to 83.0, "Sistemas Programables" to 96.0, "Taller de Investigación I" to 78.0,
    "Administración de Redes" to 91.0, "Ingeniería del Conocimiento" to 73.0, "Inteligencia Artificial" to 86.0,
    "Inteligencia de Negocios y Analítica de Negocios" to 99.0, "Programación Lógica y Funcional" to 81.0,
    "Servicio Social" to 94.0, "Taller de Investigación II" to 76.0
    // Actividades Complementarias y Residencias Profesionales no tienen calificación numérica.
)

fun historialDeAlumnoDePrueba(): HistorialAlumnoInfo = HistorialAlumnoInfo(
    carrera = "Ingeniería en Sistemas Computacionales",
    claveCarrera = "ISC",
    semestre = 9,
    materias = gradosIsc.mapIndexed { index, (nombre, creditos, semestre) ->
        val calificacion = calificacionesIsc[nombre]
        val estatus = when {
            nombre == "Residencias Profesionales" -> EstatusMateria.POR_CURSAR
            else -> EstatusMateria.APROBADA
        }
        MateriaHistorial(
            numero = index + 1,
            nombre = nombre,
            creditos = creditos,
            semestre = semestre,
            calificacion = calificacion,
            periodo = null,
            estatus = estatus
        )
    }
)
