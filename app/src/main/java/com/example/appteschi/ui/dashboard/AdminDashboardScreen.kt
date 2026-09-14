package com.example.appteschi.ui.dashboard

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appteschi.Routes
import com.example.appteschi.data.AuditEvent
import com.example.appteschi.data.AuditTrail
import com.example.appteschi.data.RiskLevel
import com.example.appteschi.data.UserSession
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.GreenSecondary
import com.example.appteschi.ui.theme.LoginAccent
import com.example.appteschi.ui.theme.LoginCanvas
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.OnPrimaryWhite
import com.example.appteschi.ui.theme.TextMuted

private data class SeccionAdmin(val titulo: String, val icono: ImageVector, val ruta: String)

/**
 * Centro de control del administrador — cada operación sobre la base de
 * datos vive en su propia pantalla (Alumnos, Calificaciones, Auditoría,
 * Estadísticas) en vez de amontonarse aquí. Ver DEC-017.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    val context = LocalContext.current
    var locationGranted by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<RiskLevel?>(null) }

    val secciones = buildList {
        add(SeccionAdmin("Alumnos", Icons.Default.Groups, Routes.ADMIN_ALUMNOS))
        add(SeccionAdmin("Calificaciones", Icons.Default.School, Routes.ADMIN_CALIFICACIONES))
        add(SeccionAdmin("Auditoría", Icons.Default.History, Routes.ADMIN_AUDITORIA))
        add(SeccionAdmin("Estadísticas", Icons.Default.Assessment, Routes.ADMIN_ESTADISTICAS))
        // Solo SUPERADMIN puede gestionar otras cuentas de administrador (DEC-020).
        if (UserSession.rol == "SUPERADMIN") {
            add(SeccionAdmin("Administradores", Icons.Default.AdminPanelSettings, Routes.ADMIN_ADMINISTRADORES))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        AuditTrail.record(
            actor   = UserSession.nombreMostrar,
            action  = if (locationGranted) "Permiso de ubicacion concedido" else "Permiso de ubicacion rechazado",
            detail  = "La app solicito ubicacion para enriquecer la auditoria",
            context = context
        )
    }

    val events = AuditTrail.all().filter { filter == null || it.risk == filter }

    Scaffold(
        containerColor = LoginCanvas,
        topBar = {
            TopAppBar(
                title = { Text("Centro de control", color = OnPrimaryWhite) },
                navigationIcon = {
                    IconButton(onClick = {
                        // Limpia el token de sesión junto con todo lo demás — sin
                        // esto quedaba en memoria hasta que otro login lo pisara.
                        UserSession.limpiar()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.ADMIN_DASHBOARD) { inclusive = true }
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cerrar sesión",
                            tint = OnPrimaryWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(LoginCanvas)
                .padding(scaffoldPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Encabezado ────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Panel de administración", color = TextMuted, fontSize = 14.sp)
                ProfileSummary()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Secciones ─────────────────────────────────────────────────
            item {
                Text("Gestión de la base de datos", color = LoginInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            item {
                val filas = (secciones.size + 1) / 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height(120.dp * filas),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    items(secciones) { seccion ->
                        SeccionCard(seccion) { navController.navigate(seccion.ruta) }
                    }
                }
            }

            // ── Permisos de ubicación ─────────────────────────────────────
            item {
                LocationCard(
                    granted  = locationGranted,
                    onRequest = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                )
            }

            // ── Log de auditoría local (eventos del dispositivo) ──────────
            item {
                Text("Actividad de este dispositivo", color = LoginInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Eventos de seguridad locales (permisos, sesión). Para el historial de movimientos sobre la base de datos, ve a Auditoría.",
                    color = TextMuted, fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    FilterChip(selected = filter == null,               onClick = { filter = null },                label = { Text("Todos") })
                    FilterChip(selected = filter == RiskLevel.ATENCION, onClick = { filter = RiskLevel.ATENCION }, label = { Text("Atención") })
                    FilterChip(selected = filter == RiskLevel.CRITICO,  onClick = { filter = RiskLevel.CRITICO },  label = { Text("Críticos") })
                }
            }
            items(events) { event -> AuditEventRow(event) }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ─── Componentes privados ─────────────────────────────────────────────────────

@Composable
private fun ProfileSummary() {
    Card(
        modifier = Modifier.padding(top = 8.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Sesión activa", color = GreenPrimary, fontWeight = FontWeight.Bold)
            Text("Usuario: ${UserSession.nombreMostrar}",  color = LoginInk, fontSize = 13.sp)
            Text("Matrícula: ${UserSession.matricula.ifBlank { "admin" }}", color = LoginInk, fontSize = 13.sp)
            Text("Rol: ${if (UserSession.esAdministrador) "Administrador" else "Alumno"}", color = LoginInk, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SeccionCard(seccion: SeccionAdmin, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        shape    = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(GreenSecondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(seccion.icono, contentDescription = seccion.titulo, tint = GreenSecondary, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                seccion.titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = GreenPrimary, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LocationCard(granted: Boolean, onRequest: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = GreenPrimary), shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Ubicacion",
                tint = LoginAccent, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Ubicación para auditoría", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    if (granted) "Activa: los eventos incluyen coordenadas"
                    else "Desactivada: activa para registrar contexto",
                    color = Color.White.copy(alpha = .78f), fontSize = 12.sp
                )
            }
            Button(
                onClick = onRequest,
                colors  = ButtonDefaults.buttonColors(containerColor = LoginAccent, contentColor = LoginInk)
            ) { Text(if (granted) "Actualizar" else "Activar") }
        }
    }
}

@Composable
private fun AuditEventRow(event: AuditEvent) {
    val riskColor = when (event.risk) {
        RiskLevel.NORMAL  -> GreenPrimary
        RiskLevel.ATENCION -> Color(0xFF9A6800)
        RiskLevel.CRITICO  -> Color(0xFFB3261E)
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                if (event.risk == RiskLevel.NORMAL) Icons.Default.Security else Icons.Default.Warning,
                contentDescription = event.risk.label,
                tint = riskColor
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(event.action,    color = LoginInk,  fontWeight = FontWeight.Bold)
                Text("${event.actor} · ${event.timestamp}", color = TextMuted, fontSize = 11.sp)
                Text(event.detail,   color = TextMuted,  fontSize = 12.sp)
                Text("Ubicación: ${event.location}", color = riskColor, fontSize = 11.sp)
            }
            Text(event.risk.label, color = riskColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
