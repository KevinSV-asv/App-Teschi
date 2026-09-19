package com.example.appteschi.viewmodel

import com.example.appteschi.data.UserSession
import com.example.appteschi.service.PerfilAlumno
import com.example.appteschi.service.PerfilAlumnoApi
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
class FakePerfilAlumnoApi(
    private val resultadoObtener: Result<PerfilAlumno> = Result.failure(IllegalStateException("no configurado")),
    private val resultadoActualizarCorreo: Result<Unit> = Result.success(Unit),
    private val resultadoCambiarPassword: Result<Unit> = Result.success(Unit)
) : PerfilAlumnoApi {
    var llamadasObtener = 0
        private set
    var llamadasActualizarCorreo = 0
        private set
    var ultimoCorreoEnviado: String? = null
        private set
    var llamadasCambiarPassword = 0
        private set
    var ultimaPasswordActualEnviada: String? = null
        private set
    var ultimaPasswordNuevaEnviada: String? = null
        private set

    override suspend fun obtener(matricula: String): Result<PerfilAlumno> {
        llamadasObtener++
        return resultadoObtener
    }

    override suspend fun actualizarCorreo(matricula: String, correoOtp: String): Result<Unit> {
        llamadasActualizarCorreo++
        ultimoCorreoEnviado = correoOtp
        return resultadoActualizarCorreo
    }

    override suspend fun cambiarPassword(matricula: String, passwordActual: String, passwordNueva: String): Result<Unit> {
        llamadasCambiarPassword++
        ultimaPasswordActualEnviada = passwordActual
        ultimaPasswordNuevaEnviada = passwordNueva
        return resultadoCambiarPassword
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

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilAlumnoViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        UserSession.establecer(matricula = "2099000001")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        UserSession.limpiar()
    }

    @Test
    fun `arranca cargando y termina con el perfil real del backend`() = runTest {
        val api = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val vm = PerfilAlumnoViewModel(api)

        assertTrue(vm.uiState.value.cargando)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(false, estado.cargando)
        assertEquals(perfilDeAlumnoDePrueba(), estado.perfil)
        assertEquals(1, api.llamadasObtener)
    }

    @Test
    fun `si el backend falla, se muestra el error real`() = runTest {
        val api = FakePerfilAlumnoApi(
            resultadoObtener = Result.failure(IllegalStateException("No existe un alumno con esa matrícula"))
        )
        val vm = PerfilAlumnoViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertNull(estado.perfil)
        assertEquals("No existe un alumno con esa matrícula", estado.error)
    }

    @Test
    fun `correo con formato invalido rechaza sin llamar al backend`() = runTest {
        val api = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val vm = PerfilAlumnoViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        vm.actualizarCorreo("no-es-un-correo")

        assertEquals("Ingresa un correo válido.", vm.uiState.value.errorCorreo)
        assertEquals(0, api.llamadasActualizarCorreo)
    }

    @Test
    fun `actualizar correo exitoso refleja el nuevo correo en el perfil`() = runTest {
        val api = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val vm = PerfilAlumnoViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        vm.actualizarCorreo("alumno@example.com")
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertTrue(estado.correoActualizado)
        assertEquals("alumno@example.com", estado.perfil?.correoOtp)
        assertEquals("alumno@example.com", api.ultimoCorreoEnviado)
    }

    @Test
    fun `actualizar correo fallido muestra el error real y no toca el perfil`() = runTest {
        val api = FakePerfilAlumnoApi(
            resultadoObtener = Result.success(perfilDeAlumnoDePrueba()),
            resultadoActualizarCorreo = Result.failure(IllegalStateException("No se pudo actualizar tu correo"))
        )
        val vm = PerfilAlumnoViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        vm.actualizarCorreo("alumno@example.com")
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals("No se pudo actualizar tu correo", estado.errorCorreo)
        assertNull(estado.perfil?.correoOtp)
    }

    @Test
    fun `password actual en blanco rechaza sin llamar al backend`() = runTest {
        val api = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val vm = PerfilAlumnoViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        vm.cambiarPassword("", "ClaveNueva1", "ClaveNueva1")

        assertEquals("Ingresa tu contraseña actual.", vm.uiState.value.errorPassword)
        assertEquals(0, api.llamadasCambiarPassword)
    }

    @Test
    fun `passwords nuevas que no coinciden rechazan sin llamar al backend`() = runTest {
        val api = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val vm = PerfilAlumnoViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        vm.cambiarPassword("ClaveVieja1", "ClaveNueva1", "OtraClave2")

        assertEquals("Las contraseñas nuevas no coinciden.", vm.uiState.value.errorPassword)
        assertEquals(0, api.llamadasCambiarPassword)
    }

    @Test
    fun `password nueva debil rechaza sin llamar al backend`() = runTest {
        val api = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val vm = PerfilAlumnoViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        vm.cambiarPassword("ClaveVieja1", "debil", "debil")

        assertEquals(
            "La contraseña requiere mínimo 8 caracteres, una mayúscula, una minúscula y un número.",
            vm.uiState.value.errorPassword
        )
        assertEquals(0, api.llamadasCambiarPassword)
    }

    @Test
    fun `cambiar password exitoso marca passwordCambiada`() = runTest {
        val api = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val vm = PerfilAlumnoViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        vm.cambiarPassword("ClaveVieja1", "ClaveNueva1", "ClaveNueva1")
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertTrue(estado.passwordCambiada)
        assertEquals("ClaveVieja1", api.ultimaPasswordActualEnviada)
        assertEquals("ClaveNueva1", api.ultimaPasswordNuevaEnviada)
    }

    @Test
    fun `cambiar password con actual incorrecta muestra el error real del backend`() = runTest {
        val api = FakePerfilAlumnoApi(
            resultadoObtener = Result.success(perfilDeAlumnoDePrueba()),
            resultadoCambiarPassword = Result.failure(IllegalStateException("La contraseña actual es incorrecta"))
        )
        val vm = PerfilAlumnoViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        vm.cambiarPassword("ClaveIncorrecta1", "ClaveNueva1", "ClaveNueva1")
        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(false, estado.passwordCambiada)
        assertEquals("La contraseña actual es incorrecta", estado.errorPassword)
    }

    @Test
    fun `cargar vuelve a consultar el backend`() = runTest {
        val api = FakePerfilAlumnoApi(resultadoObtener = Result.success(perfilDeAlumnoDePrueba()))
        val vm = PerfilAlumnoViewModel(api)
        dispatcher.scheduler.advanceUntilIdle()

        vm.cargar()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, api.llamadasObtener)
    }
}
