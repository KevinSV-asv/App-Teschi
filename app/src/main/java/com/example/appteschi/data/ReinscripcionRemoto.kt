package com.example.appteschi.data

/** REGULAR = tira asignada automáticamente. IRREGULAR = elige su grupo.
 *  BLOQUEADO = tiene una observación de reglamento pendiente y debe
 *  presentarse con su director de carrera (Fase 2). */
enum class EstatusReinscripcion { REGULAR, IRREGULAR, BLOQUEADO }

/** Una materia de un semestre anterior que el alumno todavía no aprueba —
 *  es lo que hace que su reinscripción se considere irregular. */
data class MateriaPendiente(val idMateria: Int, val nombre: String, val creditos: Int, val semestre: Int)

/** Un grupo real (viene de /api/reinscripcion/estatus, misma tabla dbo.Grupos
 *  que usa PlanEstudiosService). */
data class GrupoReinscripcion(
    val idGrupo: Int,
    val clave: String,
    val semestre: Int,
    val turno: String,
    val numero: Int,
    val periodo: String
)

/** Respuesta completa de GET /api/reinscripcion/estatus/{matricula}. */
data class EstatusReinscripcionInfo(
    val matricula: String,
    val nombreCompleto: String,
    val semestre: Int,
    val carrera: String,
    val estatus: EstatusReinscripcion,
    /** Solo viene lleno cuando estatus == BLOQUEADO — el motivo real que
     *  registró el director/admin. */
    val motivo: String?,
    val materiasPendientes: List<MateriaPendiente>,
    /** Solo viene lleno cuando estatus == REGULAR. */
    val grupoAsignado: GrupoReinscripcion?,
    /** Solo viene lleno cuando estatus == IRREGULAR. */
    val grupos: List<GrupoReinscripcion>
)

/** Una observación de reglamento (Fase 2) — el director la registra y luego
 *  la resuelve (autoriza o rechaza) la reinscripción del alumno. */
data class ObservacionReglamento(
    val id: Int,
    val motivo: String,
    val estado: String,
    val registradaPor: String,
    val fechaRegistro: String,
    val resueltaPor: String?,
    val fechaResolucion: String?,
    val resolucion: String?
)

/** Una solicitud de reinscripción que control escolar debe confirmar de
 *  manera presencial. */
data class SolicitudReinscripcionAdmin(
    val id: Int,
    val folio: String,
    val estatus: String,
    val fechaSolicitud: String,
    val matricula: String,
    val nombreCompleto: String,
    val claveGrupo: String,
    val periodo: String
)
