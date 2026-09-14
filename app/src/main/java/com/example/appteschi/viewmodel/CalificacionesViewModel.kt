package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.HistorialAcademico
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.data.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalificacionesUiState(
    val cargando: Boolean = true,
    val semestreActual: Int = 0,
    val promedioGeneral: Double = 0.0,
    val materias: List<MateriaHistorial> = emptyList()
)

/**
 * MOCK — todavía no existe scraping real de "Reportes > Calificaciones" del
 * SIIA (el portal real separa Parciales/Finales; no tenemos datos de
 * parciales todavía, solo la calificación final que ya trae el Kardex).
 *
 * Muestra únicamente las materias del semestre en el que el alumno está
 * inscrito actualmente (HistorialAcademico.materiasPara(...) filtrado por
 * semestre == UserSession.semestreActual) — el mismo dato que usa Kardex,
 * nunca una copia aparte. Las materias sin calificación todavía (el docente
 * no la ha subido) se incluyen igual, con calificacion = null, para que la
 * pantalla las muestre como "Pendiente".
 */
class CalificacionesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalificacionesUiState())
    val uiState: StateFlow<CalificacionesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(350)
            val semestreActual = UserSession.semestreActual
            val delSemestre = HistorialAcademico.materiasPara(UserSession.semestreSimulado)
                .filter { it.semestre == semestreActual }
            _uiState.update {
                it.copy(
                    cargando = false,
                    semestreActual = semestreActual,
                    promedioGeneral = promedio(delSemestre),
                    materias = delSemestre
                )
            }
        }
    }

    private fun promedio(materias: List<MateriaHistorial>): Double {
        val calificaciones = materias.mapNotNull { it.calificacion }
        return if (calificaciones.isEmpty()) 0.0 else calificaciones.average()
    }
}
