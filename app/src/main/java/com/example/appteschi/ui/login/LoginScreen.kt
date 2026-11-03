package com.example.appteschi.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.BuildConfig
import com.example.appteschi.R
import com.example.appteschi.Routes
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.GreenSecondary
import com.example.appteschi.ui.theme.LoginFieldBg
import com.example.appteschi.ui.theme.LoginGreenBright
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.OnPrimaryWhite
import com.example.appteschi.ui.theme.TextMuted
import com.example.appteschi.viewmodel.AuthViewModel
import com.example.appteschi.viewmodel.LoginPaso

/** Login con la cuenta del SIIA: usuario + contraseña y después el NIP de 4 dígitos (DEC-041, DEC-043). */
@Composable
fun LoginScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val uiState by authViewModel.uiState.collectAsState()

    var matricula by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var nip by remember { mutableStateOf("") }
    var nuevoNip by remember { mutableStateOf("") }
    var confirmarNip by remember { mutableStateOf("") }
    var curp by remember { mutableStateOf("") }
    var recuperandoNip by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.irAlDashboard) {
        if (uiState.irAlDashboard) {
            navController.navigate(Routes.DASHBOARD) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    // En Android 15+ la app se dibuja debajo de la barra de estado y de los botones
    // de navegación: sin systemBarsPadding() el pie de la tarjeta quedaba tapado.
    // Si aun así no cabe (pantalla chica o letra grande) se desplaza; si sobra
    // espacio, la tarjeta se centra.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8EDE8))
            .systemBarsPadding()
            .imePadding()
    ) {
        val alturaDisponible = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = alturaDisponible)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Sección blanca superior ───────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    when (uiState.paso) {
                        LoginPaso.CREDENCIALES -> CredencialesSection(
                            matricula = matricula,
                            password = password,
                            errorLogin = uiState.errorLogin,
                            cargando = uiState.cargando,
                            mensajeCarga = uiState.mensajeCarga,
                            onMatriculaChange = {
                                matricula = it
                                authViewModel.limpiarErrores()
                            },
                            onPasswordChange = {
                                password = it
                                authViewModel.limpiarErrores()
                            },
                            onIniciarSesion = { authViewModel.validarCredenciales(matricula, password) }
                        )

                        LoginPaso.NIP -> NipSection(
                            tieneNip = uiState.tieneNip,
                            nombre = uiState.nombreValidado,
                            nip = nip,
                            nuevoNip = nuevoNip,
                            confirmarNip = confirmarNip,
                            curp = curp,
                            recuperando = recuperandoNip,
                            cargando = uiState.cargando,
                            mensajeCarga = uiState.mensajeCarga,
                            error = uiState.errorNip,
                            mensaje = uiState.mensajeNip,
                            onNipChange = { nip = soloDigitos(it); authViewModel.limpiarErrores() },
                            onNuevoNipChange = { nuevoNip = soloDigitos(it); authViewModel.limpiarErrores() },
                            onConfirmarNipChange = { confirmarNip = soloDigitos(it); authViewModel.limpiarErrores() },
                            onCurpChange = { curp = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(18) },
                            onEntrar = {
                                if (uiState.tieneNip) authViewModel.verificarNip(nip)
                                else authViewModel.crearNip(nuevoNip, confirmarNip)
                            },
                            onRecuperar = { recuperandoNip = !recuperandoNip },
                            onEnviarCurp = { authViewModel.recuperarNip(curp) },
                            onVolver = {
                                authViewModel.volverAlLogin()
                                nip = ""; nuevoNip = ""; confirmarNip = ""; curp = ""
                                recuperandoNip = false
                            }
                        )
                    }
                }

                // ── Sección verde inferior (branding) ─────────────────────────
                LoginBrandingFooter()
            }
        }
        }
    }
}

@Composable
private fun CredencialesSection(
    matricula: String,
    password: String,
    errorLogin: String?,
    cargando: Boolean,
    mensajeCarga: String?,
    onMatriculaChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onIniciarSesion: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Iniciar Sesión",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = GreenPrimary
    )

    Text(
        text = "Entra con tu usuario y contraseña del SIIA",
        fontSize = 13.sp,
        color = TextMuted,
        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
    )

    LoginField(
        label = "Matrícula o usuario",
        value = matricula,
        onValueChange = onMatriculaChange,
        placeholder = "Ej. 202400123",
        icon = Icons.Default.Badge,
        keyboardType = KeyboardType.Text
    )
    Spacer(modifier = Modifier.height(10.dp))
    LoginField(
        label = "Contraseña",
        value = password,
        onValueChange = onPasswordChange,
        placeholder = "••••••••",
        icon = Icons.Default.Lock,
        isPassword = true
    )

    if (errorLogin != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorLogin,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(14.dp))
    Button(
        onClick = onIniciarSesion,
        enabled = !cargando,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LoginGreenBright)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                color = OnPrimaryWhite,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(mensajeCarga ?: "Validando…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Text("Iniciar Sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    // La contraseña es la del SIIA: se recupera en su portal, no en la app.
    Text(
        text = "¿Olvidaste tu contraseña? Recupérala en el portal del SIIA.",
        fontSize = 12.sp,
        color = TextMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    )
    }
}

private fun soloDigitos(valor: String): String = valor.filter(Char::isDigit).take(4)

/** Paso 2 del login SIIA (DEC-041): NIP de 4 dígitos, o crearlo si aún no tiene. */
@Composable
private fun NipSection(
    tieneNip: Boolean,
    nombre: String,
    nip: String,
    nuevoNip: String,
    confirmarNip: String,
    curp: String,
    recuperando: Boolean,
    cargando: Boolean,
    mensajeCarga: String?,
    error: String?,
    mensaje: String?,
    onNipChange: (String) -> Unit,
    onNuevoNipChange: (String) -> Unit,
    onConfirmarNipChange: (String) -> Unit,
    onCurpChange: (String) -> Unit,
    onEntrar: () -> Unit,
    onRecuperar: () -> Unit,
    onEnviarCurp: () -> Unit,
    onVolver: () -> Unit
) {
    TextButton(onClick = onVolver, contentPadding = PaddingValues(0.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
            modifier = Modifier.size(16.dp), tint = TextMuted)
        Spacer(modifier = Modifier.width(4.dp))
        Text("Regresar", color = TextMuted, fontSize = 13.sp)
    }

    Text(
        text = if (tieneNip) "NIP del SIIA" else "Crea tu NIP del SIIA",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = GreenPrimary,
        modifier = Modifier.padding(top = 4.dp)
    )
    Text(
        text = buildString {
            if (nombre.isNotBlank()) append("Hola, ${nombre.trim().split(" ").first()}. ")
            append(
                if (tieneNip) "Escribe el NIP de 4 dígitos que usas en el SIIA."
                else "El SIIA ahora pide un NIP de 4 dígitos. Créalo una sola vez: te servirá aquí y en el SIIA."
            )
        },
        fontSize = 13.sp,
        color = TextMuted,
        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
    )

    if (tieneNip) {
        LoginField(
            label = "NIP",
            value = nip,
            onValueChange = onNipChange,
            placeholder = "••••",
            icon = Icons.Default.Pin,
            keyboardType = KeyboardType.NumberPassword,
            isPassword = true
        )
    } else {
        LoginField(
            label = "Nuevo NIP",
            value = nuevoNip,
            onValueChange = onNuevoNipChange,
            placeholder = "4 dígitos",
            icon = Icons.Default.Pin,
            keyboardType = KeyboardType.NumberPassword,
            isPassword = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        LoginField(
            label = "Confirma tu NIP",
            value = confirmarNip,
            onValueChange = onConfirmarNipChange,
            placeholder = "4 dígitos",
            icon = Icons.Default.Pin,
            keyboardType = KeyboardType.NumberPassword,
            isPassword = true
        )
    }

    Spacer(modifier = Modifier.height(14.dp))
    Button(
        onClick = onEntrar,
        enabled = !cargando,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LoginGreenBright)
    ) {
        if (cargando && !recuperando) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = OnPrimaryWhite)
            Spacer(modifier = Modifier.width(8.dp))
            Text(mensajeCarga ?: "Verificando…")
        } else {
            Text(if (tieneNip) "Entrar" else "Crear NIP y entrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (tieneNip) {
        TextButton(onClick = onRecuperar, contentPadding = PaddingValues(0.dp), modifier = Modifier.padding(top = 6.dp)) {
            Text(
                if (recuperando) "Ya recordé mi NIP" else "¿Olvidaste tu NIP?",
                color = GreenSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
        }
        if (recuperando) {
            Text(
                text = "Escribe tu CURP y el SIIA te enviará tu NIP a tu correo institucional.",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LoginField(
                label = "CURP",
                value = curp,
                onValueChange = onCurpChange,
                placeholder = "18 caracteres",
                icon = Icons.Default.Badge
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onEnviarCurp,
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenSecondary),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
            ) {
                Text(if (cargando) (mensajeCarga ?: "Enviando…") else "Enviar NIP a mi correo", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (mensaje != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = mensaje, fontSize = 12.sp, color = GreenPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    if (error != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LoginField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = GreenPrimary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LoginFieldBg)
            .padding(start = 14.dp, end = if (isPassword) 4.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = GreenSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = LoginInk),
            cursorBrush = SolidColor(GreenPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.weight(1f),
            decorationBox = { campo ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 15.sp, color = TextMuted.copy(alpha = 0.7f), maxLines = 1)
                    }
                    campo()
                }
            }
        )
        if (isPassword) {
            Icon(
                imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (visible) "Ocultar $label" else "Mostrar $label",
                tint = GreenSecondary,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { visible = !visible }
                    .padding(9.dp)
            )
        }
    }
}

@Composable
private fun LoginBrandingFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoginGreenBright)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.logo_appteschi),
            contentDescription = "Logo TESCHI",
            modifier = Modifier
                .size(72.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "¡Bienvenido!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = OnPrimaryWhite
        )
        Text(
            text = "Sistema Escolar AppTESCHI",
            fontSize = 13.sp,
            color = OnPrimaryWhite.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = OnPrimaryWhite.copy(alpha = 0.35f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Usa la misma cuenta y el mismo NIP que en el SIIA.",
            fontSize = 12.sp,
            color = OnPrimaryWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Tecnológico de Estudios Superiores de Chimalhuacán · 2026",
            fontSize = 10.sp,
            color = OnPrimaryWhite.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )
        Text(
            text = "serviciosocial@teschi.edu.mx",
            fontSize = 10.sp,
            color = OnPrimaryWhite.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME} · compilada ${BuildConfig.FECHA_COMPILACION}",
            fontSize = 10.sp,
            color = OnPrimaryWhite.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
