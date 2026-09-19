package com.example.appteschi.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.Routes
import com.example.appteschi.data.UserSession
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.viewmodel.PerfilAlumnoViewModel
import com.example.appteschi.ui.theme.ErrorRed
import com.example.appteschi.ui.theme.GreenTertiary

private val VERDE_EXITO = GreenTertiary
private val ROJO_ERROR = ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilAlumnoScreen(
    navController: NavController,
    viewModel: PerfilAlumnoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        if (uiState.cargando && uiState.perfil == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Cargando tu perfil...", color = LoginInk)
            }
            return@Scaffold
        }

        if (uiState.error != null && uiState.perfil == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(uiState.error!!, color = ROJO_ERROR, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = viewModel::cargar,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) { Text("Reintentar") }
            }
            return@Scaffold
        }

        val perfil = uiState.perfil ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            DatosGeneralesCard(
                nombreCompleto = perfil.nombreCompleto,
                matricula = perfil.matricula,
                carrera = perfil.carrera,
                semestre = perfil.semestre,
                correoInstitucional = perfil.correoInstitucional
            )
            Spacer(modifier = Modifier.height(20.dp))
            CorreoRecuperacionCard(
                correoActual = perfil.correoOtp,
                guardando = uiState.guardandoCorreo,
                error = uiState.errorCorreo,
                exito = uiState.correoActualizado,
                onGuardar = viewModel::actualizarCorreo,
                onLimpiarMensaje = viewModel::limpiarMensajeCorreo
            )
            Spacer(modifier = Modifier.height(20.dp))
            CambiarPasswordCard(
                cambiando = uiState.cambiandoPassword,
                error = uiState.errorPassword,
                exito = uiState.passwordCambiada,
                onCambiar = viewModel::cambiarPassword,
                onLimpiarMensaje = viewModel::limpiarMensajePassword
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    UserSession.limpiar()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ROJO_ERROR)
            ) { Text("Cerrar sesión") }
        }
    }
}

@Composable
private fun DatosGeneralesCard(
    nombreCompleto: String,
    matricula: String,
    carrera: String?,
    semestre: Int?,
    correoInstitucional: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Mis datos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Text(
                "El nombre, la carrera y el semestre son datos oficiales — solo el director puede corregirlos.",
                fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )
            FilaDato("Nombre completo", nombreCompleto)
            FilaDato("Matrícula", matricula)
            FilaDato("Carrera", carrera ?: "Sin asignar")
            FilaDato("Semestre", semestre?.toString() ?: "Sin asignar")
            FilaDato("Correo institucional", correoInstitucional ?: "Sin asignar")
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(etiqueta, fontSize = 11.sp, color = Color.Gray)
        Text(valor, fontSize = 14.sp, color = LoginInk, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CorreoRecuperacionCard(
    correoActual: String?,
    guardando: Boolean,
    error: String?,
    exito: Boolean,
    onGuardar: (String) -> Unit,
    onLimpiarMensaje: () -> Unit
) {
    var correo by remember(correoActual) { mutableStateOf(correoActual.orEmpty()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Correo de recuperación", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Text(
                "Aquí llega el código si alguna vez olvidas tu contraseña. Mantenlo actualizado.",
                fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it; onLimpiarMensaje() },
                label = { Text("Correo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(error, color = ROJO_ERROR, fontSize = 12.sp)
            }
            if (exito) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Correo actualizado.", color = VERDE_EXITO, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onGuardar(correo) },
                enabled = !guardando,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                if (guardando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Guardar correo")
            }
        }
    }
}

@Composable
private fun CambiarPasswordCard(
    cambiando: Boolean,
    error: String?,
    exito: Boolean,
    onCambiar: (String, String, String) -> Unit,
    onLimpiarMensaje: () -> Unit
) {
    var passwordActual by remember { mutableStateOf("") }
    var passwordNueva by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var verActual by remember { mutableStateOf(false) }
    var verNueva by remember { mutableStateOf(false) }
    var verConfirmar by remember { mutableStateOf(false) }

    LaunchedEffect(exito) {
        if (exito) {
            passwordActual = ""; passwordNueva = ""; confirmarPassword = ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cambiar contraseña", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = passwordActual,
                onValueChange = { passwordActual = it; onLimpiarMensaje() },
                label = { Text("Contraseña actual") },
                singleLine = true,
                visualTransformation = if (verActual) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { verActual = !verActual }) {
                        Icon(if (verActual) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = passwordNueva,
                onValueChange = { passwordNueva = it; onLimpiarMensaje() },
                label = { Text("Nueva contraseña") },
                singleLine = true,
                visualTransformation = if (verNueva) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { verNueva = !verNueva }) {
                        Icon(if (verNueva) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = confirmarPassword,
                onValueChange = { confirmarPassword = it; onLimpiarMensaje() },
                label = { Text("Confirmar nueva contraseña") },
                singleLine = true,
                visualTransformation = if (verConfirmar) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { verConfirmar = !verConfirmar }) {
                        Icon(if (verConfirmar) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(error, color = ROJO_ERROR, fontSize = 12.sp)
            }
            if (exito) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Contraseña actualizada.", color = VERDE_EXITO, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onCambiar(passwordActual, passwordNueva, confirmarPassword) },
                enabled = !cambiando,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                if (cambiando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Cambiar contraseña")
            }
        }
    }
}
