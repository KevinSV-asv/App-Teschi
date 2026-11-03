package com.example.appteschi.service

import com.example.appteschi.core.config.SiiaEndpoints
import com.example.appteschi.testutil.cuentaDePrueba
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Llama de verdad por HTTP, pero a un SIIA falso (MockWebServer): comprueba lo
 * que la app manda y cómo entiende lo que el SIIA responde. Datos ficticios.
 */
class SiiaHttpAuthServiceTest {

    private lateinit var siia: MockWebServer
    private lateinit var servicio: SiiaHttpAuthService

    private val usuarioJson = """
        { "usuario": "2099000001", "numUsuario": "2099000001", "nombre": "ALUMNO", "paterno": "DE PRUEBA", "materno": "UNO",
          "nombreCompleto": "ALUMNO DE PRUEBA UNO", "correo": "a***o@teschi.edu.mx", "tipo": "E", "idArea": 4,
          "area": "INGENIERÍA EN SISTEMAS COMPUTACIONALES", "idCarrera": 4, "tipoCarrera": "L", "permisos": "",
          "cambioPw": 0, "puedeAsistencia": false }
    """.trimIndent()

    @Before
    fun setUp() {
        siia = MockWebServer().apply { start() }
        servicio = SiiaHttpAuthService(SiiaEndpoints(siia.url("/Teschi/api").toString()), ClienteHttp.nuevo())
    }

    @After
    fun tearDown() = siia.shutdown()

    private fun responde(codigo: Int, cuerpo: String) =
        siia.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo).setHeader("Content-Type", "application/json"))

    @Test
    fun `login manda usuario y contrasena a login_ashx y pasa al NIP con el tempToken`() = runTest {
        responde(200, """{ "ok": true, "mensaje": "", "requiereNip": true, "tieneNip": true, "tempToken": "temp-1",
                          "token": null, "expiraEnMinutos": 0, "usuario": $usuarioJson }""")

        val r = servicio.login(" 2099000001 ", "secreta")

        val peticion = siia.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/Teschi/api/login.ashx", peticion.path)
        val cuerpo = JSONObject(peticion.body.readUtf8())
        assertEquals("2099000001", cuerpo.getString("usuario"))
        assertEquals("secreta", cuerpo.getString("password"))
        assertTrue(peticion.getHeader("User-Agent")!!.startsWith("AppTESCHI-Android/"))

        r as SiiaLoginResultado.RequiereNip
        assertEquals("temp-1", r.tempToken)
        assertTrue(r.tieneNip)
        assertEquals("ALUMNO DE PRUEBA UNO", r.cuenta!!.nombreCompleto)
        assertEquals("INGENIERÍA EN SISTEMAS COMPUTACIONALES", r.cuenta!!.carrera)
        assertEquals(4, r.cuenta!!.idCarrera)
        assertTrue(r.cuenta!!.esAlumno)
    }

    @Test
    fun `login sin bloque usuario sigue al NIP sin cuenta`() = runTest {
        responde(200, """{ "ok": true, "requiereNip": true, "tieneNip": false, "tempToken": "temp-2", "usuario": null }""")
        val r = servicio.login("2099000001", "secreta") as SiiaLoginResultado.RequiereNip
        assertEquals(false, r.tieneNip)
        assertNull(r.cuenta)
    }

    @Test
    fun `login con contrasena incorrecta muestra el mensaje del SIIA`() = runTest {
        responde(401, """{ "ok": false, "mensaje": "Usuario o password incorrectos.", "usuario": null }""")
        val r = servicio.login("2099000001", "mala") as SiiaLoginResultado.Error
        assertEquals("Usuario o password incorrectos.", r.mensaje)
    }

    @Test
    fun `login con respuesta HTML del servidor no truena`() = runTest {
        siia.enqueue(MockResponse().setResponseCode(500).setBody("<html>Runtime Error</html>"))
        val r = servicio.login("2099000001", "secreta") as SiiaLoginResultado.Error
        assertEquals("El SIIA no respondió correctamente. Intenta más tarde.", r.mensaje)
    }

    @Test
    fun `verificar NIP manda accion verificar y entrega el token final con usuarioInfo`() = runTest {
        responde(200, """{ "ok": true, "token": "token-final", "expiraEnMinutos": 480, "usuarioInfo": $usuarioJson }""")

        val r = servicio.verificarNip("temp-1", "1234", cuentaPaso1 = null)

        val peticion = siia.takeRequest()
        assertEquals("/Teschi/api/nip.ashx", peticion.path)
        val cuerpo = JSONObject(peticion.body.readUtf8())
        assertEquals("verificar", cuerpo.getString("accion"))
        assertEquals("temp-1", cuerpo.getString("tempToken"))
        assertEquals("1234", cuerpo.getString("nip"))

        val sesion = (r as SiiaNipResultado.Sesion).sesion
        assertEquals("token-final", sesion.token)
        assertEquals(480, sesion.expiraEnMinutos)
        assertEquals("2099000001", sesion.cuenta.matricula)
    }

    @Test
    fun `si el NIP no repite los datos se usan los del paso 1`() = runTest {
        responde(200, """{ "ok": true, "token": "token-final", "expiraEnMinutos": 480 }""")
        val r = servicio.verificarNip("temp-1", "1234", cuentaPaso1 = cuentaDePrueba()) as SiiaNipResultado.Sesion
        assertEquals(cuentaDePrueba(), r.sesion.cuenta)
    }

    @Test
    fun `sin datos de la cuenta en ningun paso no entra`() = runTest {
        responde(200, """{ "ok": true, "token": "token-final", "expiraEnMinutos": 480 }""")
        val r = servicio.verificarNip("temp-1", "1234", cuentaPaso1 = null) as SiiaNipResultado.Error
        assertEquals("El SIIA no devolvió los datos de tu cuenta. Intenta más tarde.", r.mensaje)
    }

    @Test
    fun `NIP incorrecto muestra el mensaje del SIIA o uno por defecto`() = runTest {
        responde(401, """{ "ok": false, "mensaje": "NIP incorrecto. Te quedan 2 intentos." }""")
        assertEquals("NIP incorrecto. Te quedan 2 intentos.", (servicio.verificarNip("t", "0000", null) as SiiaNipResultado.Error).mensaje)

        responde(401, """{ "ok": false }""")
        assertEquals("NIP incorrecto.", (servicio.verificarNip("t", "0000", null) as SiiaNipResultado.Error).mensaje)
    }

    @Test
    fun `crear NIP manda accion configurar con la confirmacion`() = runTest {
        responde(200, """{ "ok": true, "token": "token-final", "expiraEnMinutos": 480, "usuarioInfo": $usuarioJson }""")
        servicio.crearNip("temp-2", "4321", "4321", cuentaPaso1 = null)
        val cuerpo = JSONObject(siia.takeRequest().body.readUtf8())
        assertEquals("configurar", cuerpo.getString("accion"))
        assertEquals("4321", cuerpo.getString("nuevoNip"))
        assertEquals("4321", cuerpo.getString("confirmarNip"))
    }

    @Test
    fun `recuperar NIP muestra el correo solo si la CURP es de quien inicia sesion`() = runTest {
        responde(200, """{ "ok": true, "usuario": "2099000001", "correo": "a***o@teschi.edu.mx" }""")
        assertEquals("Te enviamos tu NIP a a***o@teschi.edu.mx", servicio.recuperarNip("xxxx010101hdfxxx09", "2099000001").getOrThrow())
        val cuerpo = JSONObject(siia.takeRequest().body.readUtf8())
        assertEquals("recuperar", cuerpo.getString("accion"))
        assertEquals("XXXX010101HDFXXX09", cuerpo.getString("curp"))

        responde(200, """{ "ok": true, "usuario": "2099000002", "correo": "o***o@teschi.edu.mx" }""")
        assertEquals(
            "Si la CURP es correcta, el SIIA envió tu NIP a tu correo institucional.",
            servicio.recuperarNip("XXXX010101HDFXXX09", "2099000001").getOrThrow()
        )
    }

    @Test
    fun `cambiar NIP manda el token como Bearer`() = runTest {
        responde(200, """{ "ok": true, "mensaje": "NIP actualizado correctamente." }""")
        val r = servicio.cambiarNip("token-final", "1234", "5678", "5678")
        assertEquals("NIP actualizado correctamente.", r.getOrThrow())
        val peticion = siia.takeRequest()
        assertEquals("Bearer token-final", peticion.getHeader("Authorization"))
        val cuerpo = JSONObject(peticion.body.readUtf8())
        assertEquals("cambiar", cuerpo.getString("accion"))
        assertEquals("1234", cuerpo.getString("nipActual"))
    }

    @Test
    fun `cambiar NIP con el actual incorrecto falla con el mensaje del SIIA`() = runTest {
        responde(401, """{ "ok": false, "mensaje": "El NIP actual no es correcto." }""")
        val r = servicio.cambiarNip("token-final", "0000", "5678", "5678")
        assertEquals("El NIP actual no es correcto.", r.exceptionOrNull()!!.message)
    }
}
