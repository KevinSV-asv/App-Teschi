package com.example.appteschi.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import com.example.appteschi.service.AdminAuditService
import com.example.appteschi.service.RemoteAuditEvent
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginCanvas
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.TextMuted

/**
 * Historial completo de movimientos sobre la base de datos (altas, ediciones,
 * bajas, calificaciones) — GET /api/auditoria, filtrable por matrícula.
 * Distinto del log local de AdminDashboardScreen (eventos del dispositivo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditoriaScreen(navController: NavController) {
    var eventos by remember { mutableStateOf<List<RemoteAuditEvent>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filtro by remember { mutableStateOf("") }

    LaunchedEffect(filtro) {
        cargando = true
        AdminAuditService.fetch(filtro.trim()).fold(
            onSuccess = { eventos = it; error = null },
            onFailure = { error = "No se pudo cargar la auditoría remota." }
        )
        cargando = false
    }

    Scaffold(
        containerColor = LoginCanvas,
        topBar = {
            TopAppBar(
                title = { Text("Auditoría", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(LoginCanvas).padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = filtro, onValueChange = { filtro = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Filtrar por matrícula (vacío = todo)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                    focusedTextColor = LoginInk, unfocusedTextColor = LoginInk
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (error != null) Text(error!!, color = Color(0xFFB3261E), fontSize = 12.sp)
            if (cargando) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(18.dp))
                    Text("Consultando movimientos…", color = TextMuted, fontSize = 13.sp)
                }
            } else if (eventos.isEmpty()) {
                Text("No hay movimientos registrados todavía.", color = TextMuted, fontSize = 13.sp)
            } else {
                Text("${eventos.size} movimientos", color = TextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(eventos) { evento -> AuditoriaRow(evento) }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AuditoriaRow(evento: RemoteAuditEvent) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(evento.accion, color = LoginInk, fontWeight = FontWeight.Bold)
                Text(evento.fecha, color = TextMuted, fontSize = 11.sp)
            }
            Text(evento.detalle.ifBlank { "Sin detalle" }, color = LoginInk, fontSize = 13.sp)
            Text(
                if (evento.perfilMatricula.isNotBlank()) "Perfil: ${evento.perfilMatricula} · Actor: ${evento.actorNombre} (${evento.actorMatricula})"
                else "Actor: ${evento.actorNombre} (${evento.actorMatricula})",
                color = TextMuted, fontSize = 11.sp
            )
        }
    }
}
