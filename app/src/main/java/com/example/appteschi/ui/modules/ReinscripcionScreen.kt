package com.example.appteschi.ui.modules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.Routes
import com.example.appteschi.data.GruposIsc
import com.example.appteschi.data.UserSession
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.viewmodel.ReinscripcionEstado
import com.example.appteschi.viewmodel.ReinscripcionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReinscripcionScreen(
    navController: NavController,
    viewModel: ReinscripcionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reinscripción", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Reinscripción escolar", color = LoginInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Alumno: ${UserSession.nombreMostrar}", color = Color.Gray)
            Text("Periodo: ${uiState.periodo}", color = Color.Gray, fontSize = 13.sp)

            InstruccionesCard()

            when (uiState.estado) {
                ReinscripcionEstado.LOADING -> LoadingState()
                ReinscripcionEstado.REGULAR -> RegularCard(onVerTira = { navController.navigate(Routes.TIRA_MATERIAS) })
                ReinscripcionEstado.IRREGULAR_ELEGIBLE -> ReinscripcionForm(
                    grupo = uiState.grupo,
                    error = uiState.error,
                    onGrupoChange = viewModel::actualizarGrupo,
                    onSubmit = viewModel::enviar
                )
                ReinscripcionEstado.BLOQUEADO -> StatusCard(
                    success = false,
                    title = "Requiere autorización",
                    detail = "Tienes una observación de reglamento pendiente. Preséntate con tu director de carrera antes de continuar."
                )
                ReinscripcionEstado.SENDING -> LoadingState()
                ReinscripcionEstado.SUCCESS -> StatusCard(
                    success = true,
                    title = "Selección registrada",
                    detail = "Folio: ${uiState.folio}. La inscripción final se confirma de manera presencial."
                )
                ReinscripcionEstado.ERROR -> StatusCard(false, "No se pudo completar", uiState.error ?: "Intenta nuevamente.")
            }
        }
    }
}

@Composable
private fun InstruccionesCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F0))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Instrucciones", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(
                "Si eres alumno regular, tu tira se asigna automáticamente. Si eres irregular y no tienes " +
                    "observaciones de reglamento, eliges el grupo al que quieres ir. Si tienes una infracción, debes " +
                    "presentarte con tu director de carrera para que autorice tu reinscripción. " +
                    "La inscripción final es de manera presencial.",
                fontSize = 12.sp,
                color = LoginInk
            )
        }
    }
}

@Composable
private fun RegularCard(onVerTira: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Eres alumno regular", color = LoginInk, fontWeight = FontWeight.Bold)
            }
            Text(
                "Tu tira de materias se asigna automáticamente, no necesitas elegir grupo.",
                color = LoginInk,
                fontSize = 13.sp
            )
            Button(onClick = onVerTira, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                Text("Ver tira de materias")
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
        CircularProgressIndicator(color = GreenPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Consultando estatus...", color = LoginInk)
    }
}

@Composable
private fun ReinscripcionForm(
    grupo: String,
    error: String?,
    onGrupoChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val turno = GruposIsc.turno(grupo)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusCard(true, "Estatus: irregular, elegible", "Selecciona el grupo al que quieres ir el próximo periodo.")
            GrupoDropdown(grupo = grupo, onGrupoChange = onGrupoChange)
            if (turno != null) {
                Text("Turno: $turno", color = LoginInk, fontSize = 12.sp)
            }
            if (error != null) Text(error, color = Color(0xFFB3261E))
            Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                Text("Reinscribir")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrupoDropdown(grupo: String, onGrupoChange: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = grupo,
            onValueChange = {},
            readOnly = true,
            label = { Text("Grupo") },
            placeholder = { Text("Selecciona tu grupo") },
            trailingIcon = { Text(if (expandido) "▲" else "▼", color = GreenPrimary) },
            modifier = Modifier.fillMaxWidth()
        )
        // Capa invisible encima del campo para capturar el tap y abrir el menú
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expandido = true }
        )
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            GruposIsc.codigos.forEach { codigo ->
                DropdownMenuItem(
                    text = { Text(codigo) },
                    onClick = {
                        onGrupoChange(codigo)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusCard(success: Boolean, title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = if (success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (success) Icons.Default.CheckCircle else Icons.Default.Warning, title, tint = if (success) GreenPrimary else Color(0xFFB3261E))
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(title, color = LoginInk, fontWeight = FontWeight.Bold)
                Text(detail, color = LoginInk, fontSize = 13.sp)
            }
        }
    }
}
