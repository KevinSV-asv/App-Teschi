package com.example.appteschi.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.draw.clip
import com.example.appteschi.service.CalificacionesFinales
import com.example.appteschi.service.CalificacionesParciales
import com.example.appteschi.service.EstatusParcial
import com.example.appteschi.service.MateriaFinal
import com.example.appteschi.service.MateriaParcial
import com.example.appteschi.service.ResumenFinal
import com.example.appteschi.ui.theme.NeutralContainer
import com.example.appteschi.ui.theme.OnNeutralContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.data.UserSession
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.viewmodel.CalificacionesUiState
import com.example.appteschi.viewmodel.CalificacionesViewModel
import com.example.appteschi.viewmodel.VistaCalificaciones
import com.example.appteschi.ui.theme.ErrorRed
import com.example.appteschi.ui.theme.SuccessContainer
import com.example.appteschi.ui.theme.OnSuccessContainer
import com.example.appteschi.ui.theme.WarningContainer
import com.example.appteschi.ui.theme.OnWarningContainer
import com.example.appteschi.ui.theme.ErrorContainer
import com.example.appteschi.ui.theme.OnErrorContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalificacionesScreen(
    navController: NavController,
    viewModel: CalificacionesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calificaciones", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SelectorVista(uiState.vista, viewModel::seleccionarVista)
            when (uiState.vista) {
                VistaCalificaciones.PARCIALES -> VistaParciales(uiState, viewModel)
                VistaCalificaciones.FINALES -> VistaFinales(uiState, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorVista(actual: VistaCalificaciones, onVista: (VistaCalificaciones) -> Unit) {
    val vistas = listOf(VistaCalificaciones.PARCIALES to "Parciales", VistaCalificaciones.FINALES to "Finales")
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)
    ) {
        vistas.forEachIndexed { i, (vista, etiqueta) ->
            SegmentedButton(
                selected = vista == actual,
                onClick = { onVista(vista) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = vistas.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = GreenPrimary,
                    activeContentColor = Color.White,
                    inactiveContainerColor = Color.White,
                    inactiveContentColor = LoginInk
                )
            ) { Text(etiqueta) }
        }
    }
}

@Composable
private fun VistaParciales(uiState: CalificacionesUiState, viewModel: CalificacionesViewModel) {
    when {
        uiState.cargando -> CargandoCalificaciones("Consultando calificaciones...")
        uiState.error != null -> ErrorCalificaciones(uiState.error, viewModel::reintentar)
        uiState.parciales != null -> ParcialesSiia(
            parciales = uiState.parciales,
            errorPeriodo = uiState.errorPeriodo,
            modifier = Modifier.fillMaxSize(),
            onPeriodo = viewModel::seleccionarPeriodo
        )
    }
}

@Composable
private fun VistaFinales(uiState: CalificacionesUiState, viewModel: CalificacionesViewModel) {
    when {
        uiState.cargandoFinales -> CargandoCalificaciones("Consultando calificaciones finales...")
        uiState.errorFinales != null -> ErrorCalificaciones(uiState.errorFinales, viewModel::reintentar)
        uiState.finales != null -> FinalesSiia(
            finales = uiState.finales,
            errorPeriodo = uiState.errorPeriodoFinales,
            modifier = Modifier.fillMaxSize(),
            onPeriodo = viewModel::seleccionarPeriodo
        )
    }
}

@Composable
private fun CargandoCalificaciones(texto: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = GreenPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(texto, color = LoginInk)
    }
}

@Composable
private fun ErrorCalificaciones(mensaje: String, onReintentar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(mensaje, color = ErrorRed, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onReintentar,
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) { Text("Reintentar") }
    }
}

/** Calificaciones parciales del SIIA (DEC-042), por periodo. */
@Composable
private fun ParcialesSiia(
    parciales: CalificacionesParciales,
    errorPeriodo: String?,
    modifier: Modifier,
    onPeriodo: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            EncabezadoCalificaciones("Calificaciones parciales", parciales.periodo)
        }
        if (parciales.periodosDisponibles.size > 1) {
            item { SelectorPeriodo(parciales.periodosDisponibles, parciales.periodo, onPeriodo) }
        }
        if (parciales.aviso.isNotBlank()) {
            item { AvisoCalificaciones(parciales.aviso, WarningContainer, OnWarningContainer) }
        }
        if (errorPeriodo != null) {
            item { AvisoCalificaciones(errorPeriodo, ErrorContainer, OnErrorContainer) }
        }
        if (parciales.bloqueado) {
            item { TarjetaBloqueo(parciales.mensajeBloqueo, parciales.pendientes) }
        } else {
            parciales.resumen?.let { resumen ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Promedio parcial", color = Color.Gray, fontSize = 13.sp)
                                Text(resumen.promedio, color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${resumen.evaluadas} de ${resumen.totalMaterias} evaluadas", color = LoginInk, fontSize = 13.sp)
                                Text(
                                    "${resumen.aprobadas} aprobatorias · ${resumen.reprobadas} reprobatorias",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            if (parciales.materias.isEmpty()) {
                item { SinMaterias("El SIIA no tiene materias registradas en este periodo.") }
            } else {
                items(parciales.materias) { MateriaParcialCard(it) }
            }
        }
        item { Spacer(modifier = Modifier.height(90.dp)) } // deja el último elemento por encima de la barra flotante
    }
}

/** Calificaciones finales del SIIA (DEC-044), por periodo: 1ra vuelta, 2da vuelta y la final oficial. */
@Composable
private fun FinalesSiia(
    finales: CalificacionesFinales,
    errorPeriodo: String?,
    modifier: Modifier,
    onPeriodo: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            EncabezadoCalificaciones("Calificaciones finales", finales.periodo)
        }
        if (finales.periodosDisponibles.size > 1) {
            item { SelectorPeriodo(finales.periodosDisponibles, finales.periodo, onPeriodo) }
        }
        if (finales.aviso.isNotBlank()) {
            item { AvisoCalificaciones(finales.aviso, WarningContainer, OnWarningContainer) }
        }
        if (errorPeriodo != null) {
            item { AvisoCalificaciones(errorPeriodo, ErrorContainer, OnErrorContainer) }
        }
        if (finales.bloqueado) {
            item { TarjetaBloqueo(finales.mensajeBloqueo, finales.pendientes) }
        } else {
            finales.resumen?.let { resumen -> item { ResumenFinalCard(resumen) } }
            if (finales.materias.isEmpty()) {
                item { SinMaterias("El SIIA no tiene calificaciones finales en este periodo.") }
            } else {
                items(finales.materias) { MateriaFinalCard(it) }
            }
        }
        item { Spacer(modifier = Modifier.height(90.dp)) } // deja el último elemento por encima de la barra flotante
    }
}

@Composable
private fun EncabezadoCalificaciones(titulo: String, periodo: String) {
    Text(titulo, color = LoginInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Alumno: ${UserSession.nombreMostrar}", color = Color.Gray, fontSize = 13.sp)
    Text("Periodo ${periodo.ifBlank { "actual" }} · Fuente: SIIA", color = Color.Gray, fontSize = 13.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorPeriodo(periodos: List<String>, actual: String, onPeriodo: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(periodos) { periodo ->
            FilterChip(
                selected = periodo == actual,
                onClick = { onPeriodo(periodo) },
                label = { Text(periodo) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GreenPrimary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun TarjetaBloqueo(mensaje: String, pendientes: List<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = WarningContainer)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Calificaciones bloqueadas en el SIIA",
                color = OnWarningContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(mensaje, color = LoginInk, fontSize = 13.sp)
            pendientes.forEach { pendiente ->
                Text("• $pendiente", color = LoginInk, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Complétalos en el portal del SIIA para ver tus calificaciones.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SinMaterias(texto: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Text(texto, color = Color.Gray, modifier = Modifier.padding(16.dp))
    }
}

private fun coloresEstatus(estatus: EstatusParcial): Pair<Color, Color> = when (estatus) {
    EstatusParcial.APROBATORIA -> SuccessContainer to OnSuccessContainer
    EstatusParcial.REPROBATORIA -> ErrorContainer to OnErrorContainer
    EstatusParcial.SIN_CALIFICAR -> NeutralContainer to OnNeutralContainer
}

@Composable
private fun MateriaParcialCard(materia: MateriaParcial) {
    val (colorFondo, colorTexto) = coloresEstatus(materia.estatus)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                DatosMateria(materia.materia, materia.profesor, materia.grupo, Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(materia.promedio, color = colorTexto, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    EtiquetaEstatus(materia.estatus.etiqueta, colorFondo, colorTexto)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CeldaParcial("Parcial 1", materia.parcial1, Modifier.weight(1f))
                CeldaParcial("Parcial 2", materia.parcial2, Modifier.weight(1f))
                CeldaParcial("Parcial 3", materia.parcial3, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResumenFinalCard(resumen: ResumenFinal) {
    val (fondoDictamen, textoDictamen) = when (resumen.dictamen.lowercase()) {
        "aprobatorio" -> SuccessContainer to OnSuccessContainer
        "regularizar" -> WarningContainer to OnWarningContainer
        else -> NeutralContainer to OnNeutralContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Promedio final", color = Color.Gray, fontSize = 13.sp)
                Text(resumen.promedio, color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${resumen.evaluadas} de ${resumen.totalMaterias} evaluadas", color = LoginInk, fontSize = 13.sp)
                Text(
                    "${resumen.aprobadas} aprobatorias · ${resumen.reprobadas} reprobatorias",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                if (resumen.dictamen.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    EtiquetaEstatus(resumen.dictamen, fondoDictamen, textoDictamen)
                }
            }
        }
    }
}

@Composable
private fun MateriaFinalCard(materia: MateriaFinal) {
    val (colorFondo, colorTexto) = coloresEstatus(materia.estatus)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                DatosMateria(materia.materia, materia.profesor, materia.grupo, Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Final", color = Color.Gray, fontSize = 10.sp)
                    Text(materia.calificacionFinal, color = colorTexto, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    EtiquetaEstatus(materia.estatus.etiqueta, colorFondo, colorTexto)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CeldaParcial("1ra vuelta", materia.primeraVuelta, Modifier.weight(1f))
                CeldaParcial("2da vuelta", materia.segundaVuelta, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DatosMateria(materia: String, profesor: String, grupo: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(materia, color = LoginInk, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        val detalle = listOf(profesor, grupo.takeIf { it.isNotBlank() }?.let { "Grupo $it" })
            .filter { !it.isNullOrBlank() }
            .joinToString(" · ")
        if (detalle.isNotBlank()) Text(detalle, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun EtiquetaEstatus(texto: String, fondo: Color, colorTexto: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = fondo)) {
        Text(
            texto,
            color = colorTexto,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AvisoCalificaciones(texto: String, fondo: Color, colorTexto: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = fondo)) {
        Text(texto, color = colorTexto, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(12.dp))
    }
}

@Composable
private fun CeldaParcial(etiqueta: String, valor: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NeutralContainer)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(etiqueta, color = Color.Gray, fontSize = 10.sp)
        Text(valor, color = LoginInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
