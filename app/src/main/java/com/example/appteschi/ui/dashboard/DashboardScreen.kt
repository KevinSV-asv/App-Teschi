package com.example.appteschi.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appteschi.Routes
import com.example.appteschi.data.UserSession
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.GreenSecondary
import com.example.appteschi.ui.theme.LoginGreenBright
import com.example.appteschi.ui.theme.OnPrimaryWhite
import com.example.appteschi.ui.theme.TextMuted

private data class ModuloCard(
    val titulo: String,
    val icono: ImageVector,
    val ruta: String
)

@Composable
fun DashboardScreen(navController: NavController) {
    val modulos = listOf(
        ModuloCard("Reinscripción", Icons.Default.Edit, Routes.REINSCRIPCION),
        ModuloCard("Tira de Materias", Icons.AutoMirrored.Filled.List, Routes.TIRA_MATERIAS),
        ModuloCard("Calificaciones", Icons.Default.Star, Routes.CALIFICACIONES),
        ModuloCard("Intersemestral", Icons.Default.DateRange, Routes.INTERSEMESTRAL),
        ModuloCard("Kardex", Icons.Default.Assessment, Routes.KARDEX)
    )

    Scaffold(
        containerColor = Color(0xFFF5F7F5),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Asistente — pendiente */ },
                containerColor = LoginGreenBright,
                contentColor = OnPrimaryWhite,
                icon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
                text = { Text("Asistente", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            DashboardHeader(
                nombreUsuario = UserSession.nombreMostrar,
                onCerrarSesion = {
                    UserSession.limpiar()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )

            HeroBienvenida()

            Text(
                text = "Módulos escolares",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp) // 3 filas (5 tarjetas en grid de 2 columnas)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(modulos) { modulo ->
                    ModuloCardItem(modulo) {
                        navController.navigate(modulo.ruta)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DashboardHeader(
    nombreUsuario: String,
    onCerrarSesion: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TESChi",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = Color(0xFFE53935)) {
                            Text("0", color = Color.White, fontSize = 9.sp)
                        }
                    }
                ) {
                    IconButton(onClick = { /* notificaciones pendientes */ }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
                            tint = GreenPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    onClick = onCerrarSesion,
                    shape = RoundedCornerShape(24.dp),
                    color = LoginGreenBright
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = nombreUsuario.take(18),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnPrimaryWhite,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Perfil / Cerrar sesión",
                            tint = OnPrimaryWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBienvenida() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2E6B3E),
                        GreenPrimary,
                        Color(0xFF1A4D2E)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text = "Bienvenido a\nAppTESCHI",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnPrimaryWhite,
                lineHeight = 30.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Estimado(a) alumno(a), aquí podrás gestionar y dar seguimiento " +
                        "a tus trámites escolares: reinscripción, calificaciones, tira de materias " +
                        "e intersemestral. Mantén tu información actualizada.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ModuloCardItem(modulo: ModuloCard, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GreenSecondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modulo.icono,
                    contentDescription = modulo.titulo,
                    tint = GreenSecondary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = modulo.titulo,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GreenPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
