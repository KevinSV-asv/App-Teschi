package com.example.appteschi.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appteschi.service.EstadisticasResumen
import com.example.appteschi.service.EstadisticasService
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.GreenSecondary
import com.example.appteschi.ui.theme.LoginCanvas
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.TextMuted
import com.example.appteschi.ui.theme.ErrorRed
import com.example.appteschi.ui.theme.OnWarningContainer

/** Gráficas del panel de administrador — agregados de GET /api/estadisticas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEstadisticasScreen(navController: NavController) {
    var datos by remember { mutableStateOf<EstadisticasResumen?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        EstadisticasService.obtener().fold(
            onSuccess = { datos = it; error = null },
            onFailure = { error = "No se pudieron cargar las estadísticas." }
        )
        cargando = false
    }

    Scaffold(
        containerColor = LoginCanvas,
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        if (cargando) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Calculando estadísticas…", color = TextMuted)
            }
            return@Scaffold
        }
        if (error != null || datos == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text(error ?: "Sin datos.", color = ErrorRed)
            }
            return@Scaffold
        }

        val d = datos!!
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(LoginCanvas).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ResumenActivos(d) }
            item { GraficaCard("Alumnos por carrera") {
                val maximo = d.porCarrera.maxOfOrNull { it.total } ?: 0
                d.porCarrera.forEach { BarraHorizontal(it.carrera, it.total, maximo, GreenPrimary) }
            } }
            item { GraficaCard("Alumnos por semestre") {
                if (d.porSemestre.isEmpty()) {
                    Text("Ningún alumno tiene semestre asignado todavía.", color = TextMuted, fontSize = 12.sp)
                } else {
                    val maximo = d.porSemestre.maxOfOrNull { it.total } ?: 0
                    d.porSemestre.forEach { BarraHorizontal("Semestre ${it.semestre}", it.total, maximo, GreenSecondary) }
                }
            } }
            item { GraficaCard("Altas de los últimos meses") {
                if (d.altasPorMes.isEmpty()) {
                    Text("Sin registros recientes.", color = TextMuted, fontSize = 12.sp)
                } else {
                    val maximo = d.altasPorMes.maxOfOrNull { it.total } ?: 0
                    d.altasPorMes.forEach { BarraHorizontal(it.mes, it.total, maximo, GreenPrimary) }
                }
            } }
            item { GraficaCard("Calificaciones registradas") {
                if (d.calificaciones.isEmpty()) {
                    Text("Todavía no hay calificaciones capturadas.", color = TextMuted, fontSize = 12.sp)
                } else {
                    val maximo = d.calificaciones.maxOfOrNull { it.total } ?: 0
                    d.calificaciones.forEach {
                        val color = when (it.codigo) {
                            "AP" -> GreenPrimary
                            "NA" -> ErrorRed
                            else -> OnWarningContainer
                        }
                        BarraHorizontal(it.nombre, it.total, maximo, color)
                    }
                }
            } }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ResumenActivos(d: EstadisticasResumen) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Kpi("Activos", d.activos.toString(), GreenPrimary, Modifier.weight(1f))
        Kpi("Inactivos", d.inactivos.toString(), ErrorRed, Modifier.weight(1f))
        Kpi("Sin semestre", d.sinSemestre.toString(), OnWarningContainer, Modifier.weight(1f))
    }
}

@Composable
private fun Kpi(titulo: String, valor: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(valor, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(titulo, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun GraficaCard(titulo: String, contenido: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, color = LoginInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))
            contenido()
        }
    }
}

@Composable
private fun BarraHorizontal(etiqueta: String, valor: Int, maximo: Int, color: Color) {
    val fraccion = if (maximo <= 0) 0f else (valor.toFloat() / maximo).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(etiqueta, fontSize = 12.sp, color = LoginInk, modifier = Modifier.weight(1f), maxLines = 1)
            Text(valor.toString(), fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(fraccion).height(10.dp).clip(RoundedCornerShape(5.dp)).background(color)
            )
        }
    }
}
