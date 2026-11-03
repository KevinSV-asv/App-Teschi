package com.example.appteschi.testutil

import com.example.appteschi.data.CuentaSiia
import com.example.appteschi.data.FotoPerfilStore
import com.example.appteschi.service.CalificacionesFinales
import com.example.appteschi.service.CalificacionesParciales
import com.example.appteschi.service.CalificacionesSiiaApi
import com.example.appteschi.service.DatosKardex
import com.example.appteschi.service.EstatusKardex
import com.example.appteschi.service.EstatusParcial
import com.example.appteschi.service.Kardex
import com.example.appteschi.service.KardexSiiaApi
import com.example.appteschi.service.MateriaFinal
import com.example.appteschi.service.MateriaKardex
import com.example.appteschi.service.MateriaParcial
import com.example.appteschi.service.OportunidadKardex
import com.example.appteschi.service.ResultadoFinales
import com.example.appteschi.service.ResultadoKardex
import com.example.appteschi.service.ResultadoKardexPdf
import com.example.appteschi.service.ResultadoParciales
import com.example.appteschi.service.ResumenFinal
import com.example.appteschi.service.ResumenParcial
import com.example.appteschi.service.SemestreKardex
import com.example.appteschi.service.SesionSiia
import com.example.appteschi.service.SiiaAuthService
import com.example.appteschi.service.SiiaLoginResultado
import com.example.appteschi.service.SiiaNipResultado

/** Fakes sin red real y datos ficticios (nunca datos de una persona real, DEC-032). */

fun cuentaDePrueba(tipo: String = "E") = CuentaSiia(
    usuario = "2099000001",
    matricula = "2099000001",
    nombreCompleto = "ALUMNO DE PRUEBA UNO",
    correo = "a***o@teschi.edu.mx",
    tipo = tipo,
    carrera = "INGENIERÍA EN SISTEMAS COMPUTACIONALES",
    idCarrera = 4,
    cambioPw = false
)

fun sesionDePrueba(tipo: String = "E") = SesionSiia(token = "token-de-prueba", expiraEnMinutos = 480, cuenta = cuentaDePrueba(tipo))

/** Fake del login y NIP del SIIA: devuelve siempre los mismos resultados y recuerda lo que recibió. */
class FakeSiiaAuthService(
    private val resultado: SiiaLoginResultado = SiiaLoginResultado.Error("Usuario o password incorrectos."),
    private val resultadoNip: SiiaNipResultado = SiiaNipResultado.Error("no configurado"),
    private val resultadoRecuperar: Result<String> = Result.success("Te enviamos tu NIP a a***o@teschi.edu.mx"),
    private val resultadoCambiar: Result<String> = Result.success("Tu NIP se actualizó.")
) : SiiaAuthService {
    var llamadas = 0
        private set
    var llamadasNip = 0
        private set
    var ultimoTempToken: String? = null
        private set
    var ultimoNip: String? = null
        private set
    var ultimoNuevoNip: String? = null
        private set
    var ultimaCuentaPaso1: CuentaSiia? = null
        private set
    var ultimaCurp: String? = null
        private set
    var ultimoUsuarioRecuperar: String? = null
        private set
    var ultimoTokenCambiar: String? = null
        private set

    override suspend fun login(usuario: String, password: String): SiiaLoginResultado {
        llamadas++
        return resultado
    }

    override suspend fun verificarNip(tempToken: String, nip: String, cuentaPaso1: CuentaSiia?): SiiaNipResultado {
        llamadasNip++
        ultimoTempToken = tempToken
        ultimoNip = nip
        ultimaCuentaPaso1 = cuentaPaso1
        return resultadoNip
    }

    override suspend fun crearNip(tempToken: String, nuevoNip: String, confirmarNip: String, cuentaPaso1: CuentaSiia?): SiiaNipResultado {
        llamadasNip++
        ultimoTempToken = tempToken
        ultimoNuevoNip = nuevoNip
        ultimaCuentaPaso1 = cuentaPaso1
        return resultadoNip
    }

    override suspend fun recuperarNip(curp: String, usuario: String): Result<String> {
        ultimaCurp = curp
        ultimoUsuarioRecuperar = usuario
        return resultadoRecuperar
    }

    override suspend fun cambiarNip(token: String, nipActual: String, nuevoNip: String, confirmarNip: String): Result<String> {
        ultimoTokenCambiar = token
        ultimoNuevoNip = nuevoNip
        return resultadoCambiar
    }
}

/** Fake de las calificaciones del SIIA: responde con [porPeriodo] o con [resultado] (igual para las finales). */
class FakeCalificacionesSiiaApi(
    private val resultado: ResultadoParciales = ResultadoParciales.Datos(parcialesDePrueba()),
    private val porPeriodo: Map<String, ResultadoParciales> = emptyMap(),
    private val resultadoFinales: ResultadoFinales = ResultadoFinales.Datos(finalesDePrueba()),
    private val finalesPorPeriodo: Map<String, ResultadoFinales> = emptyMap()
) : CalificacionesSiiaApi {
    val periodosPedidos = mutableListOf<String?>()
    val periodosFinalesPedidos = mutableListOf<String?>()

    override suspend fun parciales(periodo: String?): ResultadoParciales {
        periodosPedidos += periodo
        return periodo?.let { porPeriodo[it] } ?: resultado
    }

    override suspend fun finales(periodo: String?): ResultadoFinales {
        periodosFinalesPedidos += periodo
        return periodo?.let { finalesPorPeriodo[it] } ?: resultadoFinales
    }
}

/** Fake de la foto de perfil: recuerda por cuenta lo que se guarda; [resultadoGuardar] null simula una imagen que no sirve. */
class FakeFotoPerfilStore(
    private val fotos: MutableMap<String, String> = mutableMapOf(),
    private val resultadoGuardar: String? = "/fotos/nueva.jpg"
) : FotoPerfilStore {
    var guardadas = 0
        private set

    override fun ruta(cuenta: String): String? = fotos[cuenta]

    override suspend fun guardar(cuenta: String, abrirOrigen: () -> java.io.InputStream?): String? {
        guardadas++
        resultadoGuardar?.let { fotos[cuenta] = it }
        return resultadoGuardar
    }

    override suspend fun quitar(cuenta: String) {
        fotos.remove(cuenta)
    }
}

/** Fake del kardex del SIIA; [resultado] se puede cambiar para probar un reintento. */
class FakeKardexSiiaApi(
    var resultado: ResultadoKardex = ResultadoKardex.Datos(kardexDePrueba()),
    private val resultadoPdf: ResultadoKardexPdf = ResultadoKardexPdf.Error("no configurado")
) : KardexSiiaApi {
    var consultas = 0
        private set
    var descargas = 0
        private set

    override suspend fun kardex(): ResultadoKardex {
        consultas++
        return resultado
    }

    override suspend fun kardexPdf(): ResultadoKardexPdf {
        descargas++
        return resultadoPdf
    }
}

fun parcialesDePrueba(periodo: String = "2026-2") = CalificacionesParciales(
    periodo = periodo,
    periodosDisponibles = listOf("2026-2", "2026-1", "2025-2"),
    bloqueado = false,
    mensajeBloqueo = "",
    pendientes = emptyList(),
    resumen = ResumenParcial(totalMaterias = 3, evaluadas = 2, aprobadas = 1, reprobadas = 1, promedio = "77.5"),
    materias = listOf(
        MateriaParcial("Materia A", "PROFESOR UNO", "4901", "90", "85", "—", "87.5", EstatusParcial.APROBATORIA),
        MateriaParcial("Materia B", "PROFESORA DOS", "4901", "60", "NP", "—", "60", EstatusParcial.REPROBATORIA),
        MateriaParcial("Materia C", "", "4901", "—", "—", "—", "—", EstatusParcial.SIN_CALIFICAR)
    )
)

fun parcialesBloqueadas() = CalificacionesParciales(
    periodo = "2026-2",
    periodosDisponibles = emptyList(),
    bloqueado = true,
    mensajeBloqueo = "Tienes trámites pendientes.",
    pendientes = listOf("Actualizar datos personales"),
    resumen = null,
    materias = emptyList()
)

fun finalesDePrueba(periodo: String = "2026-2") = CalificacionesFinales(
    periodo = periodo,
    periodosDisponibles = listOf("2026-2", "2026-1", "2025-2"),
    bloqueado = false,
    mensajeBloqueo = "",
    pendientes = emptyList(),
    resumen = ResumenFinal(totalMaterias = 2, evaluadas = 2, aprobadas = 1, reprobadas = 1, promedio = "75", dictamen = "Regularizar"),
    materias = listOf(
        MateriaFinal("Materia A", "PROFESOR UNO", "4901", "90", "—", "90", EstatusParcial.APROBATORIA),
        MateriaFinal("Materia B", "PROFESORA DOS", "4901", "50", "60", "60", EstatusParcial.REPROBATORIA)
    )
)

fun kardexDePrueba() = Kardex(
    bloqueado = false,
    mensajeBloqueo = "",
    pendientes = emptyList(),
    datos = DatosKardex(
        matricula = "2099000001",
        nombre = "ALUMNO DE PRUEBA UNO",
        curp = "XAXX000000HDFXXX00",
        carrera = "INGENIERÍA EN SISTEMAS COMPUTACIONALES",
        especialidad = "",
        planEstudios = "2010",
        situacion = "Regular",
        grupo = "4901",
        periodoIngreso = "2024-1",
        ultimoPeriodo = "2026-1",
        promedioGlobal = "85.50",
        promedioLetra = "",
        promedioSemestreAnterior = "90",
        parcial = "",
        descuento = "",
        creditosCursados = "130",
        creditosCarrera = "260",
        creditosPorCursar = "130",
        porcentaje = "50.00 %",
        avance = 0.5f,
        totalMaterias = 2,
        aprobadas = 1,
        reprobadas = 0,
        porAprobar = 1,
        porCursar = 1
    ),
    semestres = listOf(
        SemestreKardex(
            numero = 1,
            nombre = "Semestre 1",
            promedio = "90",
            promedioNumero = 90.0,
            materias = listOf(
                MateriaKardex("MAT-0001", "Materia A", "5", "90", EstatusKardex.APROBADA, listOf(OportunidadKardex("Ordinario", "90", "2024-1")))
            )
        ),
        SemestreKardex(
            numero = 2,
            nombre = "Semestre 2",
            promedio = "—",
            promedioNumero = null,
            materias = listOf(MateriaKardex("MAT-0002", "Materia B", "5", "—", EstatusKardex.POR_CURSAR, emptyList()))
        )
    ),
    simbologia = listOf("AP" to "Aprobada")
)
