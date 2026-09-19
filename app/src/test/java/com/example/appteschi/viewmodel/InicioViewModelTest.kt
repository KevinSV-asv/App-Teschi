package com.example.appteschi.viewmodel

import com.example.appteschi.data.EstatusReinscripcion
import com.example.appteschi.data.EstatusReinscripcionInfo
import com.example.appteschi.data.UserSession
import com.example.appteschi.service.ClaseHoy
import com.example.appteschi.service.EstadoClase
import com.example.appteschi.service.HorarioApi
import com.example.appteschi.service.HorarioHoy
import com.example.appteschi.service.PerfilAlumno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Fake sin red real — el mismo patrón que testutil/AuthFakes.kt. */
class FakeHorarioApi(
    private val resultado: Result<HorarioHoy> = Result.failure(IllegalStateException("no configurado"))
) : HorarioApi {
    var llamadas = 0
        private set
    var ultimaMatriculaConsultada: String? = null
        private set

    override suspend fun horarioDeHoy(matricula: String): Result<HorarioHoy> {
        llamadas++
        ultimaMatriculaConsultada = matricula
        return resultado
    }
}

private fun perfilDeAlumnoDePrueba() = PerfilAlumno(
    matricula = "2099000001",
    nombreCompleto = "Alumno de Prueba Uno",
    correoOtp = null,
    correoInstitucional = "2099000001@teschi.edu.mx",
    carrera = "Ingeniería en Sistemas Computacionales",
    claveCarrera = "ISC",
    semestre = 9
)

private fun estatusRegular() = EstatusReinscripcionInfo(
    matricula = "2099000001",
    nombreCompleto = "Alumno de Prueba Uno",
    semestre = 9,
    carrera = "Ingeniería en Sistemas Computacionales",
    estatus = EstatusReinscripcion.REGULAR,
    motivo = null,
    materiasPendientes = emptyList(),
    grupoAsignado = null,
    grupos = emptyList()
)

private fun claseDeHoy() = ClaseHoy(
    materia = "Residencias Profesionales",
    horaInicio = "16:00",
    horaFin = "18:00",
    profesor = "Mtro. E. Vázquez",
    aula = "Cubículo D-04",
    modalidad = "PRESENCIAL",
    estado = EstadoClase.EN_CURSO
)

@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        UserSession.limpiar()
    }

    @Test
    fun `alumno real carga perfil y estatus reales del backend`() = runTest {
        UserSession.establecer(matricula = "2099000001")
        val perfilApi = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val reinscripcionApi = FakeReinscripcionApi(resultadoEstatus = Result.success(estatusRegular()))
        val horarioApi = FakeHorarioApi(resultado = Result.success(HorarioHoy(dia = "Martes", clases = listOf(claseDeHoy()))))
        val vm = InicioViewModel(perfilApi, reinscripcionApi, horarioApi)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(false, estado.cargandoPerfil)
        assertEquals("Alumno de Prueba Uno", estado.nombreCompleto)
        assertEquals("Alumno", estado.primerNombre)
        assertEquals("ISC", estado.claveCarrera)
        assertEquals(9, estado.semestre)
        assertEquals(EstatusReinscripcion.REGULAR, estado.estatus)
        assertEquals(1, perfilApi.llamadasObtener)
        assertEquals(1, reinscripcionApi.llamadasEstatus)
    }

    @Test
    fun `usuario de prueba simulado no llama al backend real y usa datos de UserSession`() = runTest {
        UserSession.establecer(matricula = "sem9", nombreCompleto = "", semestreSimulado = 9)
        val perfilApi = FakePerfilAlumnoApi(resultadoObtener = Result.failure(IllegalStateException("no existe")))
        val reinscripcionApi = FakeReinscripcionApi()
        val horarioApi = FakeHorarioApi(resultado = Result.success(HorarioHoy(dia = "Martes", clases = listOf(claseDeHoy()))))
        val vm = InicioViewModel(perfilApi, reinscripcionApi, horarioApi)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals("ISC", estado.claveCarrera)
        assertEquals(9, estado.semestre)
        assertNull(estado.estatus)
        assertEquals(0, reinscripcionApi.llamadasEstatus)
        assertEquals(0, horarioApi.llamadas)
        assertTrue(estado.clases.isEmpty())
    }

    @Test
    fun `si el perfil real falla, se usan los datos ya guardados en UserSession sin romper Inicio`() = runTest {
        UserSession.establecer(matricula = "2099000001", nombreCompleto = "Alumno de Prueba Uno")
        val perfilApi = FakePerfilAlumnoApi(resultadoObtener = Result.failure(IllegalStateException("sin conexión")))
        val reinscripcionApi = FakeReinscripcionApi()
        val horarioApi = FakeHorarioApi(resultado = Result.success(HorarioHoy(dia = "Martes", clases = emptyList())))
        val vm = InicioViewModel(perfilApi, reinscripcionApi, horarioApi)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(false, estado.cargandoPerfil)
        assertEquals("Alumno de Prueba Uno", estado.nombreCompleto)
        assertNull(estado.carrera)
        assertNull(estado.semestre)
    }

    @Test
    fun `horario de hoy real se refleja con dia y clases`() = runTest {
        UserSession.establecer(matricula = "2099000001")
        val perfilApi = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val reinscripcionApi = FakeReinscripcionApi(resultadoEstatus = Result.success(estatusRegular()))
        val horarioApi = FakeHorarioApi(resultado = Result.success(HorarioHoy(dia = "Martes", clases = listOf(claseDeHoy()))))
        val vm = InicioViewModel(perfilApi, reinscripcionApi, horarioApi)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(false, estado.cargandoHorario)
        assertEquals("Martes", estado.diaHorario)
        assertEquals(listOf(claseDeHoy()), estado.clases)
        assertEquals("2099000001", horarioApi.ultimaMatriculaConsultada)
    }

    @Test
    fun `si el horario falla, se muestra el error real`() = runTest {
        UserSession.establecer(matricula = "2099000001")
        val perfilApi = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val reinscripcionApi = FakeReinscripcionApi(resultadoEstatus = Result.success(estatusRegular()))
        val horarioApi = FakeHorarioApi(resultado = Result.failure(IllegalStateException("No se pudo consultar tu horario")))
        val vm = InicioViewModel(perfilApi, reinscripcionApi, horarioApi)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(false, estado.cargandoHorario)
        assertEquals("No se pudo consultar tu horario", estado.errorHorario)
        assertTrue(estado.clases.isEmpty())
    }

    @Test
    fun `cargar vuelve a consultar perfil, estatus y horario`() = runTest {
        UserSession.establecer(matricula = "2099000001")
        val perfilApi = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val reinscripcionApi = FakeReinscripcionApi(resultadoEstatus = Result.success(estatusRegular()))
        val horarioApi = FakeHorarioApi(resultado = Result.success(HorarioHoy(dia = "Martes", clases = emptyList())))
        val vm = InicioViewModel(perfilApi, reinscripcionApi, horarioApi)
        dispatcher.scheduler.advanceUntilIdle()

        vm.cargar()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, perfilApi.llamadasObtener)
        assertEquals(2, reinscripcionApi.llamadasEstatus)
        assertEquals(2, horarioApi.llamadas)
    }
}
