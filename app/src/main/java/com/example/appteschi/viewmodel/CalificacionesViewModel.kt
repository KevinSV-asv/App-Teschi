package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.service.HistorialAcademicoApi
import com.example.appteschi.service.HistorialAcademicoService
import com.example.appteschi.service.historialDelAlumnoEnSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalificacionesUiState(
    val cargando: Boolean = true,
    val semestreActual: Int = 0,
    val promedioGeneral: Double = 0.0,
    val materias: List<MateriaHistorial> = emptyList(),
    val error: String? = null
)

/**
 * Muestra únicamente las materias del semestre en el que el alumno está
 * inscrito actualmente, tomadas del historial académico real (mismo dato
 * que usa Kardex — ver HistorialAcademicoService.historialDelAlumnoEnSesion,
 * nunca una copia aparte). Las materias sin calificación todavía (el
 * docente no la ha subido) se incluyen igual, con calificacion = null, para
 * que la pantalla las muestre como "Pendiente".
 *
 * El portal real del SIIA separa Calificaciones Parciales/Finales — no
 * tenemos datos de parciales todavía, solo la calificación final que ya
 * trae el Kardex (pendiente documentado, no alcance de este cambio).
 */
class CalificacionesViewModel(
    private val historialService: HistorialAcademicoApi = HistorialAcademicoService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalificacionesUiState())
    val uiState: StateFlow<CalificacionesUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun reintentar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            historialDelAlumnoEnSesion(historialService).fold(
                onSuccess = { info ->
                    val delSemestre = info.materias.filter { it.semestre == info.semestre }
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            semestreActual = info.semestre,
                            promedioGeneral = promedio(delSemestre),
                            materias = delSemestre
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(cargando = false, error = error.message ?: "No se pudieron consultar tus calificaciones.")
                    }
                }
            )
        }
    }

    private fun promedio(materias: List<MateriaHistorial>): Double {
        val calificaciones = materias.mapNotNull { it.calificacion }
        return if (calificaciones.isEmpty()) 0.0 else calificaciones.average()
    }
}
