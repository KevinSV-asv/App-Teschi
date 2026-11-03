package com.example.appteschi.viewmodel

import com.example.appteschi.data.UserSession
import com.example.appteschi.service.SiiaLoginResultado
import com.example.appteschi.testutil.FakeFotoPerfilStore
import com.example.appteschi.testutil.FakeSiiaAuthService
import com.example.appteschi.testutil.cuentaDePrueba
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilAlumnoViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        UserSession.establecer(cuentaDePrueba(), "token-de-prueba", 480)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        UserSession.limpiar()
    }

    @Test
    fun `muestra los datos del SIIA de la sesion`() {
        val e = PerfilAlumnoViewModel(FakeSiiaAuthService()).uiState.value
        assertEquals("ALUMNO DE PRUEBA UNO", e.nombreCompleto)
        assertEquals("2099000001", e.matricula)
        assertEquals("INGENIERÍA EN SISTEMAS COMPUTACIONALES", e.carrera)
        assertEquals("a***o@teschi.edu.mx", e.correo)
    }

    private val contrasenaCorrecta = SiiaLoginResultado.RequiereNip(tieneNip = true, tempToken = "temporal", cuenta = null)

    @Test
    fun `cambiar NIP autoriza con la contrasena y manda el token de la sesion`() = runTest {
        val siia = FakeSiiaAuthService(resultado = contrasenaCorrecta)
        val vm = PerfilAlumnoViewModel(siia)
        vm.cambiarNip("secreto", "1234", "5678", "5678")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, siia.llamadas)
        assertEquals("token-de-prueba", siia.ultimoTokenCambiar)
        assertEquals("5678", siia.ultimoNuevoNip)
        assertEquals("Tu NIP se actualizó.", vm.uiState.value.mensajeNip)
        assertFalse(vm.uiState.value.cambiandoNip)
    }

    @Test
    fun `con la contrasena incorrecta no cambia el NIP`() = runTest {
        val siia = FakeSiiaAuthService(resultado = SiiaLoginResultado.Error("Usuario o password incorrectos."))
        val vm = PerfilAlumnoViewModel(siia)
        vm.cambiarNip("mala", "1234", "5678", "5678")
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(siia.ultimoTokenCambiar)
        assertEquals("Usuario o password incorrectos.", vm.uiState.value.errorNip)
        assertNull(vm.uiState.value.mensajeNip)
        assertFalse(vm.uiState.value.cambiandoNip)
    }

    @Test
    fun `valida los datos antes de llamar al SIIA`() = runTest {
        val siia = FakeSiiaAuthService(resultado = contrasenaCorrecta)
        val vm = PerfilAlumnoViewModel(siia)

        vm.cambiarNip(" ", "1234", "5678", "5678")
        assertEquals("Escribe tu contraseña actual para autorizar el cambio.", vm.uiState.value.errorNip)
        vm.cambiarNip("secreto", "12", "5678", "5678")
        assertEquals("Escribe tu NIP actual de 4 dígitos.", vm.uiState.value.errorNip)
        vm.cambiarNip("secreto", "1234", "56", "56")
        assertEquals("El nuevo NIP debe tener 4 dígitos.", vm.uiState.value.errorNip)
        vm.cambiarNip("secreto", "1234", "5678", "8765")
        assertEquals("Los NIP nuevos no coinciden.", vm.uiState.value.errorNip)
        vm.cambiarNip("secreto", "1234", "1234", "1234")
        assertEquals("El nuevo NIP debe ser distinto del actual.", vm.uiState.value.errorNip)

        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, siia.llamadas)
        assertNull(siia.ultimoTokenCambiar)
    }

    @Test
    fun `si el SIIA rechaza el NIP actual se muestra su mensaje`() = runTest {
        val siia = FakeSiiaAuthService(
            resultado = contrasenaCorrecta,
            resultadoCambiar = Result.failure(IllegalStateException("El NIP actual no es correcto."))
        )
        val vm = PerfilAlumnoViewModel(siia)
        vm.cambiarNip("secreto", "0000", "5678", "5678")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("El NIP actual no es correcto.", vm.uiState.value.errorNip)
    }

    @Test
    fun `empieza mostrando todo y cambia de seccion`() {
        val vm = PerfilAlumnoViewModel(FakeSiiaAuthService())
        assertEquals(SeccionSeguridad.TODO, vm.uiState.value.seccion)
        vm.seleccionarSeccion(SeccionSeguridad.NIP)
        assertEquals(SeccionSeguridad.NIP, vm.uiState.value.seccion)
    }

    @Test
    fun `sin foto guardada muestra el icono`() {
        assertNull(PerfilAlumnoViewModel(FakeSiiaAuthService(), FakeFotoPerfilStore()).uiState.value.fotoRuta)
    }

    @Test
    fun `muestra la foto guardada de esta cuenta y no la de otra`() {
        val propia = FakeFotoPerfilStore(mutableMapOf("2099000001" to "/fotos/mia.jpg"))
        assertEquals("/fotos/mia.jpg", PerfilAlumnoViewModel(FakeSiiaAuthService(), propia).uiState.value.fotoRuta)

        val ajena = FakeFotoPerfilStore(mutableMapOf("2099000002" to "/fotos/otra.jpg"))
        assertNull(PerfilAlumnoViewModel(FakeSiiaAuthService(), ajena).uiState.value.fotoRuta)
    }

    @Test
    fun `cambiar la foto la guarda en la cuenta y avisa que hay una nueva`() = runTest {
        val fotos = FakeFotoPerfilStore()
        val vm = PerfilAlumnoViewModel(FakeSiiaAuthService(), fotos)
        vm.cambiarFoto { null }
        assertTrue(vm.uiState.value.guardandoFoto)
        dispatcher.scheduler.advanceUntilIdle()

        val e = vm.uiState.value
        assertEquals("/fotos/nueva.jpg", e.fotoRuta)
        assertEquals(1, e.fotoVersion)
        assertFalse(e.guardandoFoto)
        assertNull(e.errorFoto)
        assertEquals("/fotos/nueva.jpg", fotos.ruta("2099000001"))
    }

    @Test
    fun `no guarda dos fotos a la vez`() = runTest {
        val fotos = FakeFotoPerfilStore()
        val vm = PerfilAlumnoViewModel(FakeSiiaAuthService(), fotos)
        vm.cambiarFoto { null }
        vm.cambiarFoto { null }
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fotos.guardadas)
    }

    @Test
    fun `una imagen que no sirve avisa y conserva la foto anterior`() = runTest {
        val fotos = FakeFotoPerfilStore(mutableMapOf("2099000001" to "/fotos/mia.jpg"), resultadoGuardar = null)
        val vm = PerfilAlumnoViewModel(FakeSiiaAuthService(), fotos)
        vm.cambiarFoto { null }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(MENSAJE_FOTO_NO_VALIDA, vm.uiState.value.errorFoto)
        assertEquals("/fotos/mia.jpg", vm.uiState.value.fotoRuta)
        assertFalse(vm.uiState.value.guardandoFoto)

        vm.errorFotoMostrado()
        assertNull(vm.uiState.value.errorFoto)
    }

    @Test
    fun `quitar la foto la borra`() = runTest {
        val fotos = FakeFotoPerfilStore(mutableMapOf("2099000001" to "/fotos/mia.jpg"))
        val vm = PerfilAlumnoViewModel(FakeSiiaAuthService(), fotos)
        vm.quitarFoto()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.uiState.value.fotoRuta)
        assertNull(fotos.ruta("2099000001"))
    }

    @Test
    fun `cerrar sesion no borra la foto de la cuenta`() {
        val fotos = FakeFotoPerfilStore(mutableMapOf("2099000001" to "/fotos/mia.jpg"))
        PerfilAlumnoViewModel(FakeSiiaAuthService(), fotos).cerrarSesion()
        assertEquals("/fotos/mia.jpg", fotos.ruta("2099000001"))
    }

    @Test
    fun `cerrar sesion borra el token`() {
        PerfilAlumnoViewModel(FakeSiiaAuthService()).cerrarSesion()
        assertNull(UserSession.token)
    }
}
