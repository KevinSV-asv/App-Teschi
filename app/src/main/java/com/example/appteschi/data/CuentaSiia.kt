package com.example.appteschi.data

/**
 * Datos de la cuenta tal como los entrega el SIIA (bloque `usuario` del login o
 * `usuarioInfo` del paso NIP). Es todo lo que la app sabe del alumno: ya no
 * hay base de datos propia (DEC-043).
 */
data class CuentaSiia(
    val usuario: String,
    val matricula: String,
    val nombreCompleto: String,
    val correo: String,
    /** E = alumno, P = docente, A = administrativo. */
    val tipo: String,
    /** Nombre de la carrera (campo `area` del SIIA), p. ej. "INGENIERÍA EN SISTEMAS COMPUTACIONALES". */
    val carrera: String,
    val idCarrera: Int?,
    /** El SIIA pide cambiar la contraseña en su portal. */
    val cambioPw: Boolean
) {
    /** Solo los alumnos pueden entrar por ahora; docentes y administrativos quedan para después. */
    val esAlumno: Boolean
        get() {
            val t = tipo.trim().uppercase()
            return t == "E" || t.contains("ALUM") || t.contains("ESTUD")
        }
}
