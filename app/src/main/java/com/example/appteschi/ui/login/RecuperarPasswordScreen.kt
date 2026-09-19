package com.example.appteschi.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.viewmodel.RecuperarPasswordPaso
import com.example.appteschi.viewmodel.RecuperarPasswordViewModel
import com.example.appteschi.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecuperarPasswordScreen(
    navController: NavController,
    viewModel: RecuperarPasswordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var matricula by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var nuevaPassword by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar contraseña", color = Color.White) },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState.paso) {
                RecuperarPasswordPaso.MATRICULA -> PasoMatricula(
                    matricula = matricula,
                    cargando = uiState.cargando,
                    error = uiState.error,
                    onMatriculaChange = { matricula = it; viewModel.limpiarError() },
                    onEnviar = { viewModel.solicitarCodigo(matricula) }
                )
                RecuperarPasswordPaso.CODIGO_Y_NUEVA -> PasoCodigoYNueva(
                    correoEnmascarado = uiState.correoEnmascarado,
                    codigo = codigo,
                    nuevaPassword = nuevaPassword,
                    confirmarPassword = confirmarPassword,
                    cargando = uiState.cargando,
                    error = uiState.error,
                    onCodigoChange = { if (it.length <= 6) { codigo = it; viewModel.limpiarError() } },
                    onNuevaPasswordChange = { nuevaPassword = it; viewModel.limpiarError() },
                    onConfirmarPasswordChange = { confirmarPassword = it; viewModel.limpiarError() },
                    onReenviar = viewModel::reenviarCodigo,
                    onConfirmar = { viewModel.confirmar(codigo, nuevaPassword, confirmarPassword) },
                    onVolver = { viewModel.volverAMatricula(); codigo = ""; nuevaPassword = ""; confirmarPassword = "" }
                )
                RecuperarPasswordPaso.EXITO -> PasoExito(onIrALogin = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun PasoMatricula(
    matricula: String,
    cargando: Boolean,
    error: String?,
    onMatriculaChange: (String) -> Unit,
    onEnviar: () -> Unit
) {
    Text("¿Olvidaste tu contraseña?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
    Text(
        "Ingresa tu matrícula. Solo aplica si tienes una cuenta propia de AppTESCHI (registrada con matrícula y contraseña) — enviaremos un código al correo que ya tienes registrado.",
        fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
    )
    OutlinedTextField(
        value = matricula,
        onValueChange = onMatriculaChange,
        label = { Text("Matrícula") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = Modifier.fillMaxWidth()
    )
    if (error != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(error, color = ErrorRed, fontSize = 12.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onEnviar,
        enabled = !cargando,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
    ) {
        if (cargando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text("Enviar código")
    }
}

@Composable
private fun PasoCodigoYNueva(
    correoEnmascarado: String,
    codigo: String,
    nuevaPassword: String,
    confirmarPassword: String,
    cargando: Boolean,
    error: String?,
    onCodigoChange: (String) -> Unit,
    onNuevaPasswordChange: (String) -> Unit,
    onConfirmarPasswordChange: (String) -> Unit,
    onReenviar: () -> Unit,
    onConfirmar: () -> Unit,
    onVolver: () -> Unit
) {
    var verNueva by remember { mutableStateOf(false) }
    var verConfirmar by remember { mutableStateOf(false) }

    TextButton(onClick = onVolver, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Text("Cambiar matrícula", color = Color.Gray, fontSize = 13.sp)
    }
    Text("Verificación", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
    Text(
        "Enviamos un código a $correoEnmascarado. Escríbelo junto con tu nueva contraseña.",
        fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
    )
    OutlinedTextField(
        value = codigo,
        onValueChange = onCodigoChange,
        label = { Text("Código de 6 dígitos") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = nuevaPassword,
        onValueChange = onNuevaPasswordChange,
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
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = confirmarPassword,
        onValueChange = onConfirmarPasswordChange,
        label = { Text("Confirmar contraseña") },
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(error, color = ErrorRed, fontSize = 12.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onConfirmar,
        enabled = !cargando,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
    ) {
        if (cargando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text("Restablecer contraseña")
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onReenviar, enabled = !cargando) {
        Text("Reenviar código", color = GreenPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun PasoExito(onIrALogin: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Contraseña actualizada", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LoginInk)
        Text(
            "Ya puedes iniciar sesión con tu nueva contraseña.",
            fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )
        Button(
            onClick = onIrALogin,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) { Text("Volver a iniciar sesión") }
    }
}
