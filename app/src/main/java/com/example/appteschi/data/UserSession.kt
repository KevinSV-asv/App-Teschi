package com.example.appteschi.data

/**
 * Sesión en memoria del usuario autenticado.
 * Se llena tras login (admin directo, cuenta AppTESCHI u OTP); se limpia al cerrar sesión.
 */
object UserSession {
    var matricula: String = ""
    var correoOtp: String = ""
    var nombreCompleto: String = ""
    var esAdministrador: Boolean = false

    /** Token de sesión firmado por el backend — solo lo tiene un administrador
     *  autenticado (ver DEC-020). Cada llamada a un endpoint administrativo
     *  lo manda como `Authorization: Bearer <token>`. */
    var adminToken: String? = null

    /** SUPERADMIN u OPERADOR — solo tiene sentido cuando esAdministrador es true. */
    var rol: String? = null

    /**
     * Semestre a simular (solo lo llenan los usuarios de prueba sem1..sem9,
     * ver AuthRepository). null = usar el historial académico real.
     */
    var semestreSimulado: Int? = null

    /** Semestre "actual" para Tira de Materias/Calificaciones: el simulado si
     *  hay uno activo, o si no, el semestre real (9, ver HistorialAcademico). */
    val semestreActual: Int get() = semestreSimulado ?: 9

    fun establecer(
        matricula: String,
        correoOtp: String = "",
        nombreCompleto: String = "",
        esAdministrador: Boolean = false,
        semestreSimulado: Int? = null,
        adminToken: String? = null,
        rol: String? = null
    ) {
        this.matricula = matricula
        this.correoOtp = correoOtp
        this.nombreCompleto = nombreCompleto
        this.esAdministrador = esAdministrador
        this.semestreSimulado = semestreSimulado
        this.adminToken = adminToken
        this.rol = rol
    }

    fun limpiar() {
        matricula = ""
        correoOtp = ""
        nombreCompleto = ""
        esAdministrador = false
        semestreSimulado = null
        adminToken = null
        rol = null
    }

    val nombreMostrar: String
        get() = when {
            nombreCompleto.isNotBlank() -> nombreCompleto.uppercase()
            esAdministrador -> "ADMINISTRADOR"
            matricula.isNotBlank() -> "ALUMNO $matricula"
            else -> "ALUMNO TESCHI"
        }
}
