package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.GruposIsc
import com.example.appteschi.data.HistorialAcademico
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.data.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TiraMateriasUiState(
    val cargando: Boolean = true,
    val periodo: String = "",
    val grupo: String = "",
    val materias: List<MateriaHistorial> = emptyList()
)

/**
 * MOCK — todavía no existe scraping real de "Reportes > Tira de materia" del SIIA.
 * Muestra las materias POR_CURSAR del semestre en el que el alumno está
 * inscrito actualmente — el mismo dato que consume Kardex, no una lista
 * aparte. Grupo y periodo salen del comprobante real de carga académica del
 * alumno (07/09/2026) para el caso real; para los usuarios de prueba
 * sem1..sem9 se deriva el grupo real que corresponda a ese semestre.
 */
class TiraMateriasViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TiraMateriasUiState())
    val uiState: StateFlow<TiraMateriasUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(350)
            val semestreActual = UserSession.semestreActual
            val porCursar = HistorialAcademico.materiasPara(UserSession.semestreSimulado)
                .filter { it.estatus == EstatusMateria.POR_CURSAR && it.semestre == semestreActual }
            val grupo = GruposIsc.codigos.firstOrNull { GruposIsc.semestre(it) == semestreActual } ?: "9ISC23"
            _uiState.update {
                it.copy(
                    cargando = false,
                    periodo = "2026-2",
                    grupo = grupo,
                    materias = porCursar
                )
            }
        }
    }
}
