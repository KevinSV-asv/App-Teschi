package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.EstatusReinscripcion
import com.example.appteschi.data.UserSession
import com.example.appteschi.service.ClaseHoy
import com.example.appteschi.service.HorarioApi
import com.example.appteschi.service.HorarioService
import com.example.appteschi.service.PerfilAlumnoApi
import com.example.appteschi.service.PerfilAlumnoService
import com.example.appteschi.service.ReinscripcionApi
import com.example.appteschi.service.ReinscripcionService
import com.example.appteschi.service.horarioDeHoyDelAlumnoEnSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InicioUiState(
    val cargandoPerfil: Boolean = true,
    val nombreCompleto: String = "",
    val matricula: String = "",
    val carrera: String? = null,
    val claveCarrera: String? = null,
    val semestre: Int? = null,
    val estatus: EstatusReinscripcion? = null,
    val cargandoHorario: Boolean = true,
    val diaHorario: String = "",
    val clases: List<ClaseHoy> = emptyList(),
    val errorHorario: String? = null
) {
    /** Solo el primer nombre, para el saludo ("¡Hola, Ana!"). */
    val primerNombre: String
        get() = nombreCompleto.trim().split(" ").firstOrNull { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
            ?: matricula
}

/**
 * Tab "Inicio" — resumen general del alumno: quién es, su estatus/semestre/
 * carrera reales (mismas fuentes que Perfil y Reinscripción) y su horario de
 * hoy (ver DEC-029). Se degrada con gracia: si el perfil real no se puede
 * consultar (p. ej. los usuarios de prueba sem1..sem9, que no existen en la
 * base de datos), usa lo que ya haya en UserSession en vez de mostrar error
 * — Inicio nunca debe quedar "roto".
 */
class InicioViewModel(
    private val perfilServicio: PerfilAlumnoApi = PerfilAlumnoService,
    private val reinscripcionServicio: ReinscripcionApi = ReinscripcionService,
    private val horarioServicio: HorarioApi = HorarioService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InicioUiState(nombreCompleto = UserSession.nombreCompleto, matricula = UserSession.matricula)
    )
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        cargarPerfil()
        cargarHorario()
    }

    private fun cargarPerfil() {
        _uiState.update { it.copy(cargandoPerfil = true) }
        viewModelScope.launch {
            val semestreSimulado = UserSession.semestreSimulado
            val perfil = perfilServicio.obtener(UserSession.matricula).getOrNull()
            _uiState.update {
                it.copy(
                    cargandoPerfil = false,
                    nombreCompleto = perfil?.nombreCompleto?.takeIf { n -> n.isNotBlank() } ?: UserSession.nombreCompleto,
                    matricula = UserSession.matricula,
                    carrera = perfil?.carrera ?: semestreSimulado?.let { "Ingeniería en Sistemas Computacionales" },
                    claveCarrera = perfil?.claveCarrera ?: semestreSimulado?.let { "ISC" },
                    semestre = perfil?.semestre ?: semestreSimulado
                )
            }
            if (semestreSimulado == null) {
                reinscripcionServicio.estatus(UserSession.matricula).onSuccess { info ->
                    _uiState.update { it.copy(estatus = info.estatus) }
                }
            }
        }
    }

    private fun cargarHorario() {
        _uiState.update { it.copy(cargandoHorario = true, errorHorario = null) }
        viewModelScope.launch {
            horarioDeHoyDelAlumnoEnSesion(horarioServicio).fold(
                onSuccess = { horario ->
                    _uiState.update { it.copy(cargandoHorario = false, diaHorario = horario.dia, clases = horario.clases) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(cargandoHorario = false, errorHorario = error.message ?: "No se pudo consultar tu horario.") }
                }
            )
        }
    }
}
