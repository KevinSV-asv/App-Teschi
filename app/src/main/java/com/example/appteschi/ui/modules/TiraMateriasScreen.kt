package com.example.appteschi.ui.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.appteschi.ui.theme.GreenSecondary
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.viewmodel.MateriaTira
import com.example.appteschi.viewmodel.TiraMateriasViewModel
import com.example.appteschi.ui.theme.WarningContainer
import com.example.appteschi.ui.theme.OnWarningContainer
import com.example.appteschi.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiraMateriasScreen(
    navController: NavController,
    viewModel: TiraMateriasViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tira de Materias", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        if (uiState.cargando) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Consultando tus materias en el SIIA...", color = LoginInk)
            }
            return@Scaffold
        }

        if (uiState.error != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(uiState.error!!, color = ErrorRed, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = viewModel::reintentar,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) { Text("Reintentar") }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Tira de materias", color = LoginInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Alumno: ${UserSession.nombreMostrar}", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { InfoCard(uiState.periodo, uiState.grupos, uiState.materias.size) }
            if (uiState.aviso.isNotBlank()) {
                item { AvisoTira(uiState.aviso, WarningContainer, OnWarningContainer) }
            }

            if (uiState.bloqueado) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = WarningContainer)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Consulta bloqueada en el SIIA", color = OnWarningContainer, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(uiState.mensajeBloqueo, color = LoginInk, fontSize = 13.sp)
                            uiState.pendientes.forEach { pendiente ->
                                Text("• $pendiente", color = LoginInk, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Complétalos en el portal del SIIA para ver tus materias.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Materias del periodo", color = LoginInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                if (uiState.materias.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Text(
                                "El SIIA no tiene materias registradas para ti en este periodo.",
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(uiState.materias) { MateriaTiraRow(it) }
                }
                item {
                    Text(
                        "Fuente: SIIA (materias de tus calificaciones parciales del periodo).",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) } // deja el último elemento por encima de la barra flotante
        }
    }
}

@Composable
private fun InfoCard(periodo: String, grupos: List<String>, totalMaterias: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoRow("Periodo", periodo.ifBlank { "Actual" })
            if (grupos.isNotEmpty()) InfoRow(if (grupos.size == 1) "Grupo" else "Grupos", grupos.joinToString(", "))
            InfoRow("Materias", totalMaterias.toString())
        }
    }
}

@Composable
private fun InfoRow(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, color = Color.Gray, fontSize = 13.sp)
        Text(valor, color = LoginInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun AvisoTira(texto: String, fondo: Color, colorTexto: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = fondo)) {
        Text(texto, color = colorTexto, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(12.dp))
    }
}

@Composable
private fun MateriaTiraRow(materia: MateriaTira) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(materia.materia, color = LoginInk, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                if (materia.profesor.isNotBlank()) Text(materia.profesor, color = Color.Gray, fontSize = 11.sp)
            }
            if (materia.grupo.isNotBlank()) {
                Text("Grupo ${materia.grupo}", color = GreenSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
