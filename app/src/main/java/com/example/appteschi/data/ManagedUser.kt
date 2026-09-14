package com.example.appteschi.data

data class ManagedUser(
    val id: Int,
    val username: String,
    val name: String,
    val role: UserRole,
    val correo: String = "",
    val correoInstitucional: String = "",
    val carrera: String = "",
    /** Clave de CatalogoCarreras (ej. "ISC") — solo la trae el directorio remoto. */
    val claveCarrera: String = "",
    /** Semestre actual del alumno (1-12), null si el administrador no lo ha asignado. */
    val semestre: Int? = null,
    val active: Boolean = true
)

enum class UserRole(val label: String) {
    ALUMNO("Alumno"),
    ADMINISTRADOR("Administrador")
}
