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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appteschi.data.SolicitudReinscripcionAdmin
import com.example.appteschi.service.ReinscripcionAdminApi
import com.example.appteschi.service.ReinscripcionAdminService
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginCanvas
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.TextMuted
import kotlinx.coroutines.launch
import com.example.appteschi.ui.theme.ErrorRed

/**
 * Solicitudes de reinscripción pendientes de confirmar de manera presencial
 * (ver instrucciones reales del SIIA) — GET /api/reinscripcion/solicitudes,
 * confirmar con PUT .../confirmar. Fase 2 de Reinscripción.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReinscripcionesScreen(
    navController: NavController,
    servicio: ReinscripcionAdminApi = ReinscripcionAdminService
) {
    var solicitudes by remember { mutableStateOf<List<SolicitudReinscripcionAdmin>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var confirmandoId by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(recarga) {
        cargando = true
        servicio.solicitudesPendientes().fold(
            onSuccess = { solicitudes = it; error = null },
            onFailure = { error = it.message ?: "No se pudieron cargar las solicitudes." }
        )
        cargando = false
    }

    Scaffold(
        containerColor = LoginCanvas,
        topBar = {
            TopAppBar(
                title = { Text("Reinscripciones pendientes", color = Color.White) },
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
            Text(
                "Solicitudes que control escolar debe confirmar de manera presencial.",
                color = TextMuted, fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (error != null) Text(error!!, color = ErrorRed, fontSize = 12.sp)
            if (cargando) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(18.dp))
                    Text("Consultando solicitudes…", color = TextMuted, fontSize = 13.sp)
                }
            } else if (solicitudes.isEmpty()) {
                Text("No hay solicitudes pendientes.", color = TextMuted, fontSize = 13.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(solicitudes, key = { it.id }) { solicitud ->
                        SolicitudRow(
                            solicitud = solicitud,
                            confirmando = confirmandoId == solicitud.id,
                            onConfirmar = {
                                confirmandoId = solicitud.id
                                scope.launch {
                                    servicio.confirmarSolicitud(solicitud.id).fold(
                                        onSuccess = { recarga++ },
                                        onFailure = { error = it.message ?: "No se pudo confirmar la solicitud." }
                                    )
                                    confirmandoId = null
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SolicitudRow(solicitud: SolicitudReinscripcionAdmin, confirmando: Boolean, onConfirmar: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(solicitud.nombreCompleto, color = LoginInk, fontWeight = FontWeight.Bold)
                Text(solicitud.folio, color = TextMuted, fontSize = 11.sp)
            }
            Text(
                "${solicitud.matricula} · Grupo ${solicitud.claveGrupo} · ${solicitud.periodo}",
                color = TextMuted, fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onConfirmar,
                enabled = !confirmando,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text(if (confirmando) "Confirmando…" else "Confirmar inscripción")
            }
        }
    }
}
