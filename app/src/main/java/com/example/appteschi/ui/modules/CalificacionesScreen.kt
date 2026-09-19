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
import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.data.UserSession
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.viewmodel.CalificacionesViewModel
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
        if (uiState.cargando) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Consultando calificaciones...", color = LoginInk)
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
                Text("Calificaciones", color = LoginInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Alumno: ${UserSession.nombreMostrar}", color = Color.Gray, fontSize = 13.sp)
                Text("Semestre actual: ${uiState.semestreActual}", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Promedio del semestre", color = Color.Gray, fontSize = 13.sp)
                        Text(
                            "%.2f".format(uiState.promedioGeneral),
                            color = GreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            if (uiState.materias.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Text(
                            "Todavía no hay materias registradas para este semestre.",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(uiState.materias) { MateriaCalificacionRow(it) }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) } // deja el último elemento por encima de la barra flotante
        }
    }
}

@Composable
private fun MateriaCalificacionRow(materia: MateriaHistorial) {
    val (colorFondo, colorTexto) = when (materia.estatus) {
        EstatusMateria.APROBADA -> SuccessContainer to OnSuccessContainer
        EstatusMateria.POR_CURSAR -> WarningContainer to OnWarningContainer
        EstatusMateria.NO_APROBADA -> ErrorContainer to OnErrorContainer
    }
    val etiqueta = if (materia.estatus == EstatusMateria.POR_CURSAR) "Pendiente" else materia.estatus.etiqueta
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
            Column(horizontalAlignment = Alignment.End) {
                if (materia.calificacion != null) {
                    Text("%.1f".format(materia.calificacion), color = colorTexto, fontWeight = FontWeight.Bold)
                }
                Card(colors = CardDefaults.cardColors(containerColor = colorFondo)) {
                    Text(
                        etiqueta,
                        color = colorTexto,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
