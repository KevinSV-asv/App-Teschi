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
import com.example.appteschi.data.GruposIsc
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.data.UserSession
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.GreenSecondary
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.viewmodel.TiraMateriasViewModel
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
                Text("Consultando carga académica...", color = LoginInk)
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
            item { InfoCard(uiState.periodo, uiState.grupo) }
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Materias asignadas", color = LoginInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            if (uiState.materias.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Text(
                            "No tienes materias asignadas para este periodo.",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(uiState.materias) { MateriaTiraRow(it) }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) } // deja el último elemento por encima de la barra flotante
        }
    }
}

@Composable
private fun InfoCard(periodo: String, grupo: String) {
    val turno = GruposIsc.turno(grupo)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoRow("Periodo", periodo)
            InfoRow("Grupo", grupo)
            if (turno != null) InfoRow("Turno", turno)
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
private fun MateriaTiraRow(materia: MateriaHistorial) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(materia.nombre, color = LoginInk, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("Semestre ${materia.semestre}", color = Color.Gray, fontSize = 11.sp)
            }
            Text("${materia.creditos} créditos", color = GreenSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
