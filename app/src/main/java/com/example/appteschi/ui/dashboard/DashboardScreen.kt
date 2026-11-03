package com.example.appteschi.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.R
import com.example.appteschi.Routes
import com.example.appteschi.ui.theme.BackgroundLight
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginGreenBright
import com.example.appteschi.ui.theme.OnPrimaryWhite
import com.example.appteschi.ui.theme.TextMuted
import com.example.appteschi.viewmodel.InicioUiState
import com.example.appteschi.viewmodel.InicioViewModel
import com.example.appteschi.ui.theme.ErrorRed
import com.example.appteschi.ui.theme.LoginInk

/**
 * Tab "Inicio" — bienvenida con los datos del SIIA y el resumen del periodo
 * actual. Los módulos (Tira de Materias, Calificaciones, Reinscripción,
 * Kardex) son destinos de [BottomNavBar], siempre visible.
 */
@Composable
fun DashboardScreen(navController: NavController, viewModel: InicioViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            DashboardHeader(onIrAPerfil = { navController.navigate(Routes.PERFIL_ALUMNO) })

            HeroInicio(uiState)

            ResumenPeriodo(
                uiState,
                onReintentar = viewModel::cargar,
                onVerCalificaciones = {
                    navController.navigate(Routes.CALIFICACIONES) {
                        popUpTo(Routes.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun DashboardHeader(onIrAPerfil: () -> Unit) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo_appteschi),
                    contentDescription = "Logo TESCHI",
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "TESChi", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            }

            Surface(
                onClick = onIrAPerfil,
                shape = RoundedCornerShape(24.dp),
                color = LoginGreenBright
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Mi perfil",
                        tint = OnPrimaryWhite,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mi perfil", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnPrimaryWhite)
                }
            }
        }
    }
}

@Composable
private fun HeroInicio(uiState: InicioUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E6B3E), GreenPrimary, Color(0xFF1A4D2E))
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Etiqueta("Alumno")
                if (uiState.periodo.isNotBlank()) Etiqueta("Periodo ${uiState.periodo}")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "¡Hola, ${uiState.primerNombre}!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnPrimaryWhite
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (uiState.carrera.isNotBlank()) {
                    "${uiState.carrera.lowercase().replaceFirstChar { it.uppercase() }}. Aquí ves tus calificaciones, tus materias y tu kardex del SIIA."
                } else {
                    "Aquí ves tus calificaciones, tus materias y tu kardex del SIIA."
                },
                fontSize = 13.sp,
                color = OnPrimaryWhite.copy(alpha = 0.92f),
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = OnPrimaryWhite.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OnPrimaryWhite.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "Matrícula: ${uiState.matricula}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnPrimaryWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = OnPrimaryWhite.copy(alpha = 0.16f)) {
        Text(
            texto, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnPrimaryWhite,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** Resumen del periodo actual con las parciales del SIIA. */
@Composable
private fun ResumenPeriodo(uiState: InicioUiState, onReintentar: () -> Unit, onVerCalificaciones: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Insights, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tu periodo actual", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))

            val resumen = uiState.resumen
            when {
                uiState.cargandoResumen -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Consultando el SIIA...", color = TextMuted, fontSize = 13.sp)
                }
                uiState.errorResumen != null -> Column {
                    Text(uiState.errorResumen, color = ErrorRed, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onReintentar, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                        Text("Reintentar")
                    }
                }
                uiState.bloqueado -> Text(
                    "El SIIA bloqueó la consulta de tus calificaciones por trámites pendientes. Revisa el detalle en Calif.",
                    color = LoginInk, fontSize = 13.sp, lineHeight = 18.sp
                )
                resumen == null -> Text("El SIIA todavía no tiene materias registradas en este periodo.", color = TextMuted, fontSize = 13.sp)
                else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Indicador("Promedio parcial", resumen.promedio, Modifier.weight(1f))
                    Indicador("Materias", resumen.totalMaterias.toString(), Modifier.weight(1f))
                    Indicador("Evaluadas", resumen.evaluadas.toString(), Modifier.weight(1f))
                }
            }

            if (!uiState.cargandoResumen && uiState.errorResumen == null) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onVerCalificaciones,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) { Text("Ver mis calificaciones") }
            }
        }
    }
}

@Composable
private fun Indicador(etiqueta: String, valor: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundLight)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
        Text(etiqueta, fontSize = 11.sp, color = TextMuted)
    }
}
