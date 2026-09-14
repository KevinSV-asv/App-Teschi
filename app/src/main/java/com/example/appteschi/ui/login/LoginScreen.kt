package com.example.appteschi.ui.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.data.UserSession
import com.example.appteschi.Routes
import com.example.appteschi.service.TipoCuenta
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.GreenSecondary
import com.example.appteschi.ui.theme.LoginFieldBg
import com.example.appteschi.ui.theme.LoginGreenBright
import com.example.appteschi.ui.theme.LoginGreenDark
import com.example.appteschi.ui.theme.OnPrimaryWhite
import com.example.appteschi.ui.theme.TextMuted
import com.example.appteschi.viewmodel.AuthViewModel
import com.example.appteschi.viewmodel.LoginPaso

@Composable
fun LoginScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val uiState by authViewModel.uiState.collectAsState()

    var matricula by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var correoOtp by remember { mutableStateOf("") }
    var codigo2FA by remember { mutableStateOf("") }
    // Si el usuario ya tiene un código de un envío anterior (p. ej. la app se
    // fue a segundo plano y regresó a este paso desde cero), no debería tener
    // que pedir uno nuevo solo para que aparezca el campo — "Ya tengo un
    // código" lo revela directo, sin volver a llamar a /api/otp/enviar.
    var tengoCodigo by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val onCodigoEnviado: (String) -> Unit = { correo ->
        codigo2FA = ""
        Toast.makeText(context, "Código enviado a $correo", Toast.LENGTH_LONG).show()
    }

    // ── Navegación post-login para administrador ──────────────────────────────
    // irAlDashboard se activa desde AuthViewModel cuando el login es exitoso sin OTP
    LaunchedEffect(uiState.irAlDashboard) {
        if (uiState.irAlDashboard) {
            val destino = if (UserSession.esAdministrador) {
                Routes.ADMIN_DASHBOARD
            } else {
                Routes.DASHBOARD
            }
            navController.navigate(destino) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8EDE8)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Sección blanca superior ───────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
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
                            onOlvidoClick = { navController.navigate(Routes.RECUPERAR_PASS) },
                            onIniciarSesion = {
                                authViewModel.validarCredencialesSiia(matricula, password)
                            }
                        )

                        LoginPaso.VERIFICACION -> VerificacionSection(
                            correoOtp = correoOtp,
                            codigo2FA = codigo2FA,
                            correoEnviado = uiState.otpEnviado,
                            mostrarCampoCodigo = uiState.otpEnviado || tengoCodigo,
                            errorEnvio = uiState.errorEnvio,
                            bloqueado = uiState.bloqueado,
                            cargando = uiState.cargando,
                            mensajeCarga = uiState.mensajeCarga,
                            intentosFallidos = uiState.intentosFallidos,
                            onCorreoChange = {
                                correoOtp = it
                                authViewModel.limpiarErrores()
                            },
                            onCodigoChange = { if (it.length <= 6) codigo2FA = it },
                            onEnviarCodigo = {
                                authViewModel.enviarCodigoVerificacion(correoOtp, onCodigoEnviado)
                            },
                            onReenviar = {
                                authViewModel.reenviarOtp(correoOtp, onCodigoEnviado)
                            },
                            onTengoCodigo = { tengoCodigo = true },
                            onVolver = {
                                authViewModel.volverAlLogin()
                                codigo2FA = ""
                                tengoCodigo = false
                            },
                            onVerificar = {
                                authViewModel.verificarOtp(
                                    codigoIngresado = codigo2FA,
                                    onExito = {
                                        val esAdmin = uiState.tipoPendiente == TipoCuenta.ADMINISTRADOR
                                        UserSession.establecer(
                                            matricula = uiState.matriculaValidada,
                                            correoOtp = correoOtp.trim().lowercase(),
                                            nombreCompleto = uiState.nombreValidado,
                                            esAdministrador = esAdmin,
                                            adminToken = uiState.tokenAdmin,
                                            rol = uiState.rolValidado
                                        )
                                        val destino = if (esAdmin) Routes.ADMIN_DASHBOARD else Routes.DASHBOARD
                                        navController.navigate(destino) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                        }
                                    },
                                    onFallo = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onVolverInicio = {
                                authViewModel.volverAlLogin()
                                codigo2FA = ""
                                tengoCodigo = false
                            }
                        )
                    }
                }

                // ── Sección verde inferior (branding) ─────────────────────────
                LoginBrandingFooter(
                    onRegistrarse = { navController.navigate(Routes.REGISTRO) }
                )
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
    onOlvidoClick: () -> Unit,
    onIniciarSesion: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Iniciar Sesión",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = GreenPrimary
    )
    Text(
        text = "Ingresa tus credenciales académicas",
        fontSize = 14.sp,
        color = TextMuted,
        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
    )

    LoginField(
        label = "Matrícula",
        value = matricula,
        onValueChange = onMatriculaChange,
        placeholder = "Ej. 202400123 o admin",
        icon = Icons.Default.Badge,
        keyboardType = KeyboardType.Text   // Text permite letras — necesario para "admin"
    )
    Spacer(modifier = Modifier.height(16.dp))
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

    Spacer(modifier = Modifier.height(12.dp))
    TextButton(
        onClick = onOlvidoClick,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.align(Alignment.Start)
    ) {
        Text(
            text = "¿Olvidaste tu contraseña?",
            color = GreenSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onIniciarSesion,
        enabled = !cargando,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
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
    }
}

@Composable
private fun VerificacionSection(
    correoOtp: String,
    codigo2FA: String,
    correoEnviado: Boolean,
    /** true si ya debe verse el campo de código — por haberlo enviado en esta
     *  sesión de pantalla, o porque el usuario dijo "ya tengo un código". */
    mostrarCampoCodigo: Boolean,
    errorEnvio: String?,
    bloqueado: Boolean,
    cargando: Boolean,
    mensajeCarga: String?,
    intentosFallidos: Int,
    onCorreoChange: (String) -> Unit,
    onCodigoChange: (String) -> Unit,
    onEnviarCodigo: () -> Unit,
    onReenviar: () -> Unit,
    onTengoCodigo: () -> Unit,
    onVolver: () -> Unit,
    onVerificar: () -> Unit,
    onVolverInicio: () -> Unit
) {
    if (bloqueado) {
        Text(
            text = "Demasiados intentos fallidos.\nVuelve a iniciar sesión.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = onVolverInicio, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al inicio")
        }
        return
    }

    TextButton(onClick = onVolver, contentPadding = PaddingValues(0.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
            modifier = Modifier.size(16.dp), tint = TextMuted)
        Spacer(modifier = Modifier.width(4.dp))
        Text("Regresar", color = TextMuted, fontSize = 13.sp)
    }

    Text(
        text = "Verificación 2FA",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = GreenPrimary,
        modifier = Modifier.padding(top = 4.dp)
    )
    Text(
        text = "Te enviaremos un código a tu correo",
        fontSize = 14.sp,
        color = TextMuted,
        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
    )

    LoginField(
        label = "Correo electrónico",
        value = correoOtp,
        onValueChange = onCorreoChange,
        placeholder = "tu@correo.com",
        icon = Icons.Default.Email,
        keyboardType = KeyboardType.Email
    )

    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = onEnviarCodigo,
        enabled = !cargando,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenSecondary),
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
    ) {
        if (cargando && !correoEnviado) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(mensajeCarga ?: "Enviando…")
        } else {
            Text(if (correoEnviado) "Reenviar código" else "Enviar código", fontWeight = FontWeight.SemiBold)
        }
    }

    if (!mostrarCampoCodigo) {
        TextButton(
            onClick = onTengoCodigo,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            Text("Ya tengo un código", color = GreenSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }

    if (mostrarCampoCodigo) {
        Spacer(modifier = Modifier.height(16.dp))
        LoginField(
            label = "Código de 6 dígitos",
            value = codigo2FA,
            onValueChange = onCodigoChange,
            placeholder = "000000",
            icon = Icons.Default.Pin,
            keyboardType = KeyboardType.NumberPassword
        )

        if (intentosFallidos > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Intentos fallidos: $intentosFallidos",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onVerificar,
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LoginGreenBright)
        ) {
            Text("Verificar e Ingresar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (errorEnvio != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorEnvio,
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
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = GreenPrimary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LoginFieldBg)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = GreenSecondary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextMuted.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (visible) "Ocultar $label" else "Mostrar $label",
                            tint = GreenSecondary
                        )
                    }
                }
            } else null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = GreenPrimary
            )
        )
    }
}

@Composable
private fun LoginBrandingFooter(onRegistrarse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoginGreenBright)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .background(LoginGreenDark, RoundedCornerShape(8.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "TESChi",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OnPrimaryWhite
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¡Bienvenido!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = OnPrimaryWhite
        )
        Text(
            text = "Sistema Escolar AppTESCHI",
            fontSize = 14.sp,
            color = OnPrimaryWhite.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = OnPrimaryWhite.copy(alpha = 0.35f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "¿NO TIENES UNA CUENTA?",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = OnPrimaryWhite.copy(alpha = 0.85f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onRegistrarse,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnPrimaryWhite),
            border = BorderStroke(1.5.dp, OnPrimaryWhite)
        ) {
            Text("Registrarse", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Tecnológico de Estudios Superiores de Chimalhuacán\n2026",
            fontSize = 10.sp,
            color = OnPrimaryWhite.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
        Text(
            text = "serviciosocial@teschi.edu.mx",
            fontSize = 10.sp,
            color = OnPrimaryWhite.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
