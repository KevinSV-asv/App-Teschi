package com.example.appteschi.ui.dashboard

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.Routes
import com.example.appteschi.core.config.ApiConfig
import com.example.appteschi.ui.theme.BackgroundLight
import com.example.appteschi.ui.theme.DarkSurface
import com.example.appteschi.ui.theme.ErrorContainer
import com.example.appteschi.ui.theme.ErrorRed
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginAccent
import com.example.appteschi.ui.theme.LoginGreenBright
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.OnSecurityBlue
import com.example.appteschi.ui.theme.OnSuccessContainer
import com.example.appteschi.ui.theme.OnWarningContainer
import com.example.appteschi.ui.theme.SecurityBlueContainer
import com.example.appteschi.ui.theme.SecurityTeal
import com.example.appteschi.ui.theme.SlateBorder
import com.example.appteschi.ui.theme.SlateSurface
import com.example.appteschi.ui.theme.SuccessContainer
import com.example.appteschi.ui.theme.TextMuted
import com.example.appteschi.ui.theme.WarningContainer
import com.example.appteschi.viewmodel.PerfilAlumnoUiState
import com.example.appteschi.viewmodel.PerfilAlumnoViewModel
import com.example.appteschi.viewmodel.SeccionSeguridad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Mi perfil: foto (DEC-048), datos del SIIA y "Seguridad de la Cuenta", con el diseño del portal del SIIA (DEC-047). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilAlumnoScreen(
    navController: NavController,
    viewModel: PerfilAlumnoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    val avisos = remember { SnackbarHostState() }

    // El selector de fotos de Android no necesita permisos: solo da acceso a la imagen elegida.
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val resolver = contexto.applicationContext.contentResolver
            viewModel.cambiarFoto { resolver.openInputStream(uri) }
        }
    }

    LaunchedEffect(uiState.errorFoto) {
        val error = uiState.errorFoto ?: return@LaunchedEffect
        avisos.showSnackbar(error)
        viewModel.errorFotoMostrado()
    }

    Scaffold(
        containerColor = BackgroundLight,
        snackbarHost = { SnackbarHost(avisos) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            EncabezadoAlumno(
                uiState = uiState,
                onElegirFoto = {
                    try {
                        selectorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(contexto, "Tu teléfono no tiene una app para elegir imágenes.", Toast.LENGTH_LONG).show()
                    }
                },
                onQuitarFoto = viewModel::quitarFoto
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TarjetaDatos(uiState.correo)
                EncabezadoSeguridad()
                SelectorSeccion(uiState.seccion, viewModel::seleccionarSeccion)
                if (uiState.seccion != SeccionSeguridad.NIP) {
                    TarjetaContrasena(uiState.matricula, onAbrirPortal = { abrirPortal(contexto) })
                }
                if (uiState.seccion != SeccionSeguridad.CONTRASENA) {
                    TarjetaNip(
                        cambiando = uiState.cambiandoNip,
                        error = uiState.errorNip,
                        exito = uiState.mensajeNip,
                        onCambiar = viewModel::cambiarNip,
                        onLimpiarMensaje = viewModel::limpiarMensajeNip
                    )
                }
                Surface(
                    onClick = {
                        viewModel.cerrarSesion()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = ErrorContainer,
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar sesión", color = ErrorRed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun abrirPortal(contexto: Context) {
    try {
        contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ApiConfig.SIIA_PORTAL_URL)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(contexto, "No tienes un navegador para abrir el portal del SIIA.", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun EncabezadoAlumno(uiState: PerfilAlumnoUiState, onElegirFoto: () -> Unit, onQuitarFoto: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GreenPrimary, DarkSurface)))
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FotoAlumno(uiState, onElegirFoto, onQuitarFoto)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            uiState.nombreCompleto.ifBlank { "Sin dato" },
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(uiState.matricula, color = LoginAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
        if (uiState.carrera.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                uiState.carrera,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }
    }
}

/** Foto con el botón de la cámara. Sin foto, abre directo el selector; con foto, deja cambiarla o quitarla. */
@Composable
private fun FotoAlumno(uiState: PerfilAlumnoUiState, onElegirFoto: () -> Unit, onQuitarFoto: () -> Unit) {
    var menuAbierto by remember { mutableStateOf(false) }
    val foto by produceState<ImageBitmap?>(null, uiState.fotoRuta, uiState.fotoVersion) {
        value = uiState.fotoRuta?.let { ruta ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(ruta)?.asImageBitmap() }
        }
    }
    val alTocar = {
        if (uiState.fotoRuta == null) onElegirFoto() else menuAbierto = true
    }
    Box(modifier = Modifier.size(94.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(84.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(enabled = !uiState.guardandoFoto, onClickLabel = "Cambiar foto de perfil", onClick = alTocar),
            contentAlignment = Alignment.Center
        ) {
            val imagen = foto
            if (imagen != null) {
                Image(
                    bitmap = imagen,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(56.dp))
            }
            if (uiState.guardandoFoto) {
                CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, GreenPrimary.copy(alpha = 0.3f), CircleShape)
                .clickable(enabled = !uiState.guardandoFoto, onClickLabel = "Cambiar foto de perfil", onClick = alTocar),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = "Cambiar foto de perfil", tint = GreenPrimary, modifier = Modifier.size(17.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 5.dp, bottom = 5.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(3.dp)
                .clip(CircleShape)
                .background(LoginGreenBright)
        )
        DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
            DropdownMenuItem(
                text = { Text("Cambiar foto") },
                leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                onClick = {
                    menuAbierto = false
                    onElegirFoto()
                }
            )
            DropdownMenuItem(
                text = { Text("Quitar foto") },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    menuAbierto = false
                    onQuitarFoto()
                }
            )
        }
    }
}

@Composable
private fun TarjetaDatos(correo: String) {
    Tarjeta {
        Column {
            Text("Correo institucional", fontSize = 11.sp, color = TextMuted)
            Text(correo.ifBlank { "Sin dato" }, fontSize = 14.sp, color = LoginInk, fontWeight = FontWeight.Medium)
        }
        Text(
            "Son los datos del SIIA. Si algo está mal, acude a Servicios Escolares.",
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun EncabezadoSeguridad() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Mosaico(Icons.Outlined.Lock, SuccessContainer, GreenPrimary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Seguridad de la Cuenta", color = GreenPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Contraseña institucional y NIP de seguridad de 4 dígitos", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SelectorSeccion(actual: SeccionSeguridad, onSeccion: (SeccionSeguridad) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OpcionSeccion("Ver todo", Icons.Outlined.Shield, actual == SeccionSeguridad.TODO, Modifier.weight(0.9f)) { onSeccion(SeccionSeguridad.TODO) }
        OpcionSeccion("Solo contraseña", Icons.Outlined.Lock, actual == SeccionSeguridad.CONTRASENA, Modifier.weight(1.3f)) { onSeccion(SeccionSeguridad.CONTRASENA) }
        OpcionSeccion("Solo NIP", Icons.Outlined.Pin, actual == SeccionSeguridad.NIP, Modifier.weight(0.9f)) { onSeccion(SeccionSeguridad.NIP) }
    }
}

@Composable
private fun OpcionSeccion(etiqueta: String, icono: ImageVector, elegida: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (elegida) GreenPrimary else LoginInk
    Surface(
        selected = elegida,
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (elegida) SuccessContainer else Color.White,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                etiqueta,
                color = color,
                fontSize = 11.5.sp,
                fontWeight = if (elegida) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * La API del SIIA no tiene cómo cambiar la contraseña, así que el formulario va
 * apagado para que nadie la escriba en un campo que no hace nada. Cuando exista
 * el endpoint solo hay que encender los campos y conectar el botón.
 */
@Composable
private fun TarjetaContrasena(matricula: String, onAbrirPortal: () -> Unit) {
    Tarjeta {
        EncabezadoTarjeta(
            icono = Icons.Outlined.Key,
            fondoIcono = SecurityBlueContainer,
            colorIcono = OnSecurityBlue,
            titulo = "Contraseña de Acceso",
            subtitulo = "Actualiza tu clave de acceso al portal institucional"
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(WarningContainer)
                .padding(12.dp)
        ) {
            Text(
                "El SIIA todavía no permite cambiar la contraseña desde la app. Por ahora hazlo en su portal.",
                color = OnWarningContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onAbrirPortal,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, OnWarningContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = OnWarningContainer, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir portal del SIIA", color = OnWarningContainer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        CampoSeguridad("Usuario actual", matricula, "", Icons.Outlined.Person, habilitado = false, secreto = false)
        CampoSeguridad("Contraseña actual", "", "Ingresa tu contraseña actual", Icons.Outlined.Lock, habilitado = false)
        HorizontalDivider(color = SlateBorder)
        CampoSeguridad("Nueva contraseña", "", "Mínimo 9 caracteres", Icons.Outlined.Shield, habilitado = false)
        CampoSeguridad("Confirmar nueva contraseña", "", "Confirma la nueva contraseña", Icons.Outlined.TaskAlt, habilitado = false)
        Requisitos(
            "Requisitos de la contraseña:",
            listOf("Al menos 9 caracteres de longitud", "Debe coincidir exactamente con la confirmación")
        )
        BotonActualizar("Actualizar Contraseña", GreenPrimary, habilitado = false, cargando = false) {}
    }
}

@Composable
private fun TarjetaNip(
    cambiando: Boolean,
    error: String?,
    exito: String?,
    onCambiar: (String, String, String, String) -> Unit,
    onLimpiarMensaje: () -> Unit
) {
    // Los secretos solo viven aquí mientras se escriben: nunca en estado guardado.
    var password by remember { mutableStateOf("") }
    var nipActual by remember { mutableStateOf("") }
    var nuevoNip by remember { mutableStateOf("") }
    var confirmarNip by remember { mutableStateOf("") }

    LaunchedEffect(exito) {
        if (exito != null) {
            password = ""; nipActual = ""; nuevoNip = ""; confirmarNip = ""
        }
    }

    Tarjeta {
        EncabezadoTarjeta(
            icono = Icons.Outlined.Shield,
            fondoIcono = SuccessContainer,
            colorIcono = GreenPrimary,
            titulo = "NIP de Seguridad (4 Dígitos)",
            subtitulo = "Es el mismo NIP del SIIA: el cambio también aplica allá"
        )
        Insignia("NIP activo (4 dígitos)")
        CampoSeguridad("Contraseña actual para autorizar", password, "Ingresa tu contraseña actual", Icons.Outlined.Lock) {
            password = it; onLimpiarMensaje()
        }
        CampoSeguridad("NIP actual de 4 dígitos", nipActual, "••••", Icons.Outlined.ErrorOutline, esNip = true) {
            nipActual = it; onLimpiarMensaje()
        }
        HorizontalDivider(color = SlateBorder)
        CampoSeguridad("Nuevo NIP de seguridad", nuevoNip, "4 dígitos numéricos", Icons.Outlined.Pin, esNip = true) {
            nuevoNip = it; onLimpiarMensaje()
        }
        CampoSeguridad("Confirmar nuevo NIP", confirmarNip, "Repite los 4 dígitos", Icons.Outlined.TaskAlt, esNip = true) {
            confirmarNip = it; onLimpiarMensaje()
        }
        Requisitos(
            "Requisitos del NIP de seguridad:",
            listOf("Exactamente 4 dígitos numéricos (0 al 9)", "Clave personal e intransferible")
        )
        if (error != null) {
            Text(error, color = ErrorRed, fontSize = 12.sp)
        }
        if (exito != null) {
            Text(
                exito,
                color = OnSuccessContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SuccessContainer)
                    .padding(10.dp)
            )
        }
        BotonActualizar("Actualizar NIP de Seguridad", SecurityTeal, habilitado = !cambiando, cargando = cambiando) {
            onCambiar(password, nipActual, nuevoNip, confirmarNip)
        }
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SlateBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            contenido()
        }
    }
}

@Composable
private fun EncabezadoTarjeta(icono: ImageVector, fondoIcono: Color, colorIcono: Color, titulo: String, subtitulo: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Mosaico(icono, fondoIcono, colorIcono)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = LoginInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitulo, color = TextMuted, fontSize = 12.sp)
        }
    }
    HorizontalDivider(color = SlateBorder)
}

@Composable
private fun Mosaico(icono: ImageVector, fondo: Color, color: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Insignia(texto: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SuccessContainer)
            .border(1.dp, GreenPrimary.copy(alpha = 0.2f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(GreenPrimary))
        Spacer(modifier = Modifier.width(6.dp))
        Text(texto, color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CampoSeguridad(
    etiqueta: String,
    valor: String,
    placeholder: String,
    icono: ImageVector,
    habilitado: Boolean = true,
    secreto: Boolean = true,
    esNip: Boolean = false,
    onCambio: (String) -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    Column {
        Text(
            etiqueta.uppercase(),
            color = LoginInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = valor,
            onValueChange = { nuevo -> onCambio(if (esNip) nuevo.filter(Char::isDigit).take(4) else nuevo) },
            enabled = habilitado,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(fontSize = 15.sp),
            placeholder = {
                Text(
                    placeholder,
                    fontSize = 14.sp,
                    letterSpacing = if (esNip) 2.sp else 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = { Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = if (secreto) {
                {
                    IconButton(onClick = { visible = !visible }, enabled = habilitado) {
                        Icon(
                            if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (visible) "Ocultar $etiqueta" else "Mostrar $etiqueta",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = if (esNip) KeyboardType.NumberPassword else KeyboardType.Password),
            visualTransformation = if (secreto && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = SlateBorder,
                disabledBorderColor = SlateBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = SlateSurface,
                focusedTextColor = LoginInk,
                unfocusedTextColor = LoginInk,
                disabledTextColor = LoginInk,
                cursorColor = GreenPrimary,
                focusedLeadingIconColor = GreenPrimary,
                unfocusedLeadingIconColor = TextMuted,
                disabledLeadingIconColor = TextMuted,
                focusedTrailingIconColor = TextMuted,
                unfocusedTrailingIconColor = TextMuted,
                disabledTrailingIconColor = TextMuted.copy(alpha = 0.4f),
                focusedPlaceholderColor = TextMuted,
                unfocusedPlaceholderColor = TextMuted,
                disabledPlaceholderColor = TextMuted.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Requisitos(titulo: String, requisitos: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateSurface)
            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(titulo.uppercase(), color = LoginInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        requisitos.forEach { requisito ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(requisito, color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BotonActualizar(texto: String, color: Color, habilitado: Boolean, cargando: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = if (cargando) color else color.copy(alpha = 0.35f),
            disabledContentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
