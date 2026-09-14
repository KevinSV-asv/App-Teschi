package com.example.appteschi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.HistorialAcademico
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.data.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KardexUiState(
    val cargando: Boolean = true,
    val carrera: String = "",
    val periodoIngreso: String = "",
    val ultimoPeriodo: String = "",
    val promedioGlobal: Double = 0.0,
    val promedioUltimoSemestre: Double = 0.0,
    val totalCreditosCarrera: Int = 0,
    val totalCreditosCursados: Int = 0,
    val porcentajeCubierto: Double = 0.0,
    val totalMaterias: Int = 0,
    val materiasAprobadas: Int = 0,
    val materiasReprobadas: Int = 0,
    val materiasPorAprobar: Int = 0,
    val materias: List<MateriaHistorial> = emptyList()
)

/**
 * Muestra HistorialAcademico.materiasPara(...) (fuente única, compartida con
 * Tira de Materias y Calificaciones) — este ViewModel no guarda su propia
 * copia del historial, solo calcula los totales de resumen a partir de esa
 * lista.
 */
class KardexViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(KardexUiState())
    val uiState: StateFlow<KardexUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(400)
            val materias = HistorialAcademico.materiasPara(UserSession.semestreSimulado)
            _uiState.update {
                it.copy(
                    cargando = false,
                    carrera = "Ingeniería en Sistemas Computacionales",
                    periodoIngreso = "2022-2",
                    ultimoPeriodo = "2026-1",
                    promedioGlobal = promedioAprobadas(materias),
                    promedioUltimoSemestre = promedioAprobadas(materias.filter { m -> m.periodo == "2026-1" }),
                    totalCreditosCarrera = materias.sumOf { m -> m.creditos },
                    totalCreditosCursados = materias
                        .filter { m -> m.estatus == EstatusMateria.APROBADA }
                        .sumOf { m -> m.creditos },
                    porcentajeCubierto = porcentajeCubierto(materias),
                    totalMaterias = materias.size,
                    materiasAprobadas = materias.count { m -> m.estatus == EstatusMateria.APROBADA },
                    materiasReprobadas = materias.count { m -> m.estatus == EstatusMateria.NO_APROBADA },
                    materiasPorAprobar = materias.count { m -> m.estatus == EstatusMateria.POR_CURSAR },
                    materias = materias
                )
            }
        }
    }

    private fun promedioAprobadas(materias: List<MateriaHistorial>): Double {
        val calificaciones = materias.mapNotNull { it.calificacion }
        return if (calificaciones.isEmpty()) 0.0 else calificaciones.average()
    }

    private fun porcentajeCubierto(materias: List<MateriaHistorial>): Double {
        val total = materias.sumOf { it.creditos }
        val cursados = materias.filter { it.estatus == EstatusMateria.APROBADA }.sumOf { it.creditos }
        return if (total == 0) 0.0 else (cursados.toDouble() / total) * 100
    }
}
