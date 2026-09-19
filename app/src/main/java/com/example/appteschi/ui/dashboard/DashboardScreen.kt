package com.example.appteschi.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.Routes
import com.example.appteschi.data.EstatusReinscripcion
import com.example.appteschi.service.ClaseHoy
import com.example.appteschi.service.EstadoClase
import com.example.appteschi.ui.theme.BackgroundLight
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.GreenSecondary
import com.example.appteschi.ui.theme.LoginGreenBright
import com.example.appteschi.ui.theme.OnPrimaryWhite
import com.example.appteschi.ui.theme.TextMuted
import com.example.appteschi.viewmodel.InicioUiState
import com.example.appteschi.viewmodel.InicioViewModel
import com.example.appteschi.ui.theme.ErrorRed
import com.example.appteschi.ui.theme.SuccessContainer

/**
 * Tab "Inicio" — pantalla general de bienvenida: quién eres, tu estatus real
 * y tu horario de hoy. Los módulos (Reinscripción, Tira de Materias,
 * Calificaciones, Intersemestral, Kardex) ya no viven aquí como tarjetas: son
 * destinos de [BottomNavBar], siempre visible.
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

            MateriasYHorarioHoy(uiState, onReintentar = viewModel::cargar)

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
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
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
                if (uiState.estatus != null) EtiquetaEstatus(uiState.estatus)
                if (uiState.semestre != null) Etiqueta("${uiState.semestre}° Semestre")
                if (uiState.claveCarrera != null) Etiqueta(uiState.claveCarrera)
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
                text = if (uiState.semestre != null && uiState.carrera != null) {
                    "Estás en tu ${uiState.semestre}° semestre. ¡Sigue así, la meta en ${uiState.carrera} está cada vez más cerca!"
                } else {
                    "Aquí podrás gestionar y dar seguimiento a tus trámites escolares: reinscripción, calificaciones, tira de materias e intersemestral."
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

@Composable
private fun EtiquetaEstatus(estatus: EstatusReinscripcion) {
    val (texto, color) = when (estatus) {
        EstatusReinscripcion.REGULAR -> "ESTATUS: ALUMNO REGULAR" to Color(0xFF81C784)
        EstatusReinscripcion.IRREGULAR -> "ESTATUS: ALUMNO IRREGULAR" to Color(0xFFFFB74D)
        EstatusReinscripcion.BLOQUEADO -> "ESTATUS: BLOQUEADO" to Color(0xFFE57373)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = OnPrimaryWhite.copy(alpha = 0.16f)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(5.dp))
            Text(texto, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnPrimaryWhite)
        }
    }
}

@Composable
private fun MateriasYHorarioHoy(uiState: InicioUiState, onReintentar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Materias y Horario de Hoy", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                }
                if (uiState.diaHorario.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = SuccessContainer) {
                        Text(
                            "${uiState.diaHorario} lectivo", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = GreenPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.cargandoHorario -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Cargando tu horario...", color = TextMuted, fontSize = 13.sp)
                }
                uiState.errorHorario != null -> Column {
                    Text(uiState.errorHorario, color = ErrorRed, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onReintentar, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                        Text("Reintentar")
                    }
                }
                uiState.clases.isEmpty() -> Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, tint = TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No tienes clases programadas para hoy.", color = TextMuted, fontSize = 13.sp)
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.clases.forEach { clase -> ClaseHoyCard(clase) }
                }
            }
        }
    }
}

@Composable
private fun ClaseHoyCard(clase: ClaseHoy) {
    val (colorEstado, textoEstado) = when (clase.estado) {
        EstadoClase.EN_CURSO -> GreenSecondary to "EN CURSO"
        EstadoClase.PROXIMA -> Color(0xFF1976D2) to "PRÓXIMA"
        EstadoClase.TERMINADA -> TextMuted to "TERMINADA"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundLight)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(GreenPrimary)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HORA", fontSize = 8.sp, color = OnPrimaryWhite.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                Text(clase.horaInicio, fontSize = 13.sp, color = OnPrimaryWhite, fontWeight = FontWeight.Bold)
                Text(clase.horaFin, fontSize = 11.sp, color = OnPrimaryWhite.copy(alpha = 0.8f))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(clase.materia, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A2E22))
            if (!clase.profesor.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(clase.profesor, fontSize = 11.sp, color = TextMuted)
                }
            }
            if (!clase.aula.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Default.Room, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(clase.aula, fontSize = 11.sp, color = TextMuted)
                }
            }
        }
        Surface(shape = RoundedCornerShape(8.dp), color = colorEstado.copy(alpha = 0.15f)) {
            Text(
                textoEstado, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colorEstado,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
