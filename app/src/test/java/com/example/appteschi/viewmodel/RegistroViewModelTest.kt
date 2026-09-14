package com.example.appteschi.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * RegistroViewModel hace su llamada de red real dentro de withContext(Dispatchers.IO)
 * (un hilo real, no el tiempo virtual del test). Por eso, en vez de
 * dispatcher.scheduler.advanceUntilIdle(), se usa una espera acotada en tiempo
 * real hasta que el estado deja de estar "enviando" — evita pruebas inestables.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistroViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var server: MockWebServer

    private val datosValidos = mapOf(
        "matricula" to "20240001",
        "nombres" to "Ana",
        "apellidoPaterno" to "Lopez",
        "apellidoMaterno" to "Garcia",
        "fechaNacimiento" to "2000-01-01",
        "sistema" to "ESCOLARIZADO",
        "carrera" to "ISC",
        "password" to "Password1",
        "confirmarPassword" to "Password1"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        server.shutdown()
    }

    private fun viewModelApuntandoAlServidor(readTimeoutMs: Long = 15_000): RegistroViewModel {
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()
        return RegistroViewModel(
            registroApiUrl = server.url("/api/registro").toString(),
            client = client
        )
    }

    private fun registrar(viewModel: RegistroViewModel, overrides: Map<String, String> = emptyMap()) {
        val d = datosValidos + overrides
        viewModel.registrar(
            matricula = d.getValue("matricula"),
            nombres = d.getValue("nombres"),
            apellidoPaterno = d.getValue("apellidoPaterno"),
            apellidoMaterno = d.getValue("apellidoMaterno"),
            fechaNacimiento = d.getValue("fechaNacimiento"),
            sistema = d.getValue("sistema"),
            carrera = d.getValue("carrera"),
            password = d.getValue("password"),
            confirmarPassword = d.getValue("confirmarPassword")
        )
    }

    /**
     * Espera al estado TERMINAL del ViewModel combinando dos cosas:
     * - avanzar el StandardTestDispatcher (Main) en cada vuelta, porque no
     *   ejecuta nada por sí solo — si no se avanza, la corrutina de
     *   viewModelScope.launch nunca llega ni a su primera línea;
     * - una espera acotada en tiempo REAL, porque la llamada de red ocurre en
     *   Dispatchers.IO (un hilo real) fuera del reloj virtual del test.
     * "!enviando" solo no basta como condición: también es el valor inicial
     * antes de procesar, y el estado "enviando" intermedio también trae
     * mensaje ("Registrando cuenta…"). El estado final siempre tiene
     * enviando=false Y mensaje!=null (éxito o error).
     */
    private fun esperarResultado(viewModel: RegistroViewModel, timeoutMs: Long = 5_000): RegistroUiState {
        val limite = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < limite) {
            dispatcher.scheduler.advanceUntilIdle()
            val estado = viewModel.uiState.value
            if (!estado.enviando && estado.mensaje != null) return estado
            Thread.sleep(10)
        }
        throw AssertionError("Tiempo de espera agotado esperando resultado del ViewModel")
    }

    // ── Validaciones locales — no deben tocar la red ─────────────────────────

    @Test
    fun `campos vacios rechaza sin llamar al servidor`() = runTest {
        val viewModel = viewModelApuntandoAlServidor()
        registrar(viewModel, mapOf("nombres" to ""))

        assertEquals(0, server.requestCount)
        assertEquals("Completa todos los campos obligatorios.", viewModel.uiState.value.mensaje)
    }

    @Test
    fun `contrasenas distintas se rechazan sin llamar al servidor`() = runTest {
        val viewModel = viewModelApuntandoAlServidor()
        registrar(viewModel, mapOf("confirmarPassword" to "OtraPassword1"))

        assertEquals(0, server.requestCount)
        assertEquals("Las contraseñas no coinciden.", viewModel.uiState.value.mensaje)
    }

    @Test
    fun `contrasena debil se rechaza sin llamar al servidor`() = runTest {
        val viewModel = viewModelApuntandoAlServidor()
        registrar(viewModel, mapOf("password" to "1234", "confirmarPassword" to "1234"))

        assertEquals(0, server.requestCount)
        assertTrue(viewModel.uiState.value.mensaje?.contains("mínimo 8 caracteres") == true)
    }

    // ── Respuestas del servidor (contra un servidor HTTP real de prueba) ────

    @Test
    fun `respuesta exitosa marca exito y muestra el correo institucional`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """{"ok":true,"mensaje":"Cuenta creada correctamente","matricula":"20240001","correoInstitucional":"20240001@teschi.edu.mx"}"""
                )
        )
        val viewModel = viewModelApuntandoAlServidor()
        registrar(viewModel)
        val estado = esperarResultado(viewModel)

        assertTrue("mensaje real: ${estado.mensaje}", estado.exito)
        assertTrue("mensaje real: ${estado.mensaje}", estado.mensaje?.contains("20240001@teschi.edu.mx") == true)
    }

    @Test
    fun `matricula duplicada muestra el error exacto del servidor`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody("""{"ok":false,"error":"La matrícula ya tiene una cuenta registrada"}""")
        )
        val viewModel = viewModelApuntandoAlServidor()
        registrar(viewModel)
        val estado = esperarResultado(viewModel)

        assertFalse("mensaje real: ${estado.mensaje}", estado.exito)
        assertTrue("mensaje real: ${estado.mensaje}", estado.mensaje?.contains("La matrícula ya tiene una cuenta registrada") == true)
    }

    @Test
    fun `respuesta no JSON muestra mensaje de diagnostico con el codigo HTTP`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("<html><body>Not Found</body></html>")
        )
        val viewModel = viewModelApuntandoAlServidor()
        registrar(viewModel)
        val estado = esperarResultado(viewModel)

        assertFalse("mensaje real: ${estado.mensaje}", estado.exito)
        assertTrue("mensaje real: ${estado.mensaje}", estado.mensaje?.contains("404") == true)
        assertTrue("mensaje real: ${estado.mensaje}", estado.mensaje?.contains("no devolvió JSON") == true)
    }

    @Test
    fun `timeout de red muestra mensaje de tiempo agotado`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val viewModel = viewModelApuntandoAlServidor(readTimeoutMs = 300)
        registrar(viewModel)
        val estado = esperarResultado(viewModel)

        assertFalse("mensaje real: ${estado.mensaje}", estado.exito)
        assertTrue("mensaje real: ${estado.mensaje}", estado.mensaje?.contains("Tiempo de espera agotado") == true)
    }
}
