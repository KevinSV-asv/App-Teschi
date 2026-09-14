package com.example.appteschi.data

/** Una carrera del catálogo institucional (viene de /api/catalogos/registro). */
data class CarreraCatalogo(val clave: String, val nombre: String)

/** Un grupo real (viene de /api/grupos/{clave}) — mismo formato que usa el SIIA. */
data class GrupoInfo(
    val clave: String,
    val semestre: Int,
    val turno: String,
    val numero: Int,
    val periodo: String
)
