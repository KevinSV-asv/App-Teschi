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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appteschi.data.UserSession
import com.example.appteschi.service.AdminAccountsService
import com.example.appteschi.service.AdministradorCuenta
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginCanvas
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.TextMuted

/**
 * Alta y gestión de cuentas de administrador — exclusivo de SUPERADMIN (ver
 * DEC-020). Un OPERADOR nunca debería llegar aquí; la tarjeta que navega a
 * esta pantalla solo aparece en el hub cuando UserSession.rol == SUPERADMIN.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAdministradoresScreen(navController: NavController) {
    var cuentas by remember { mutableStateOf<List<AdministradorCuenta>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableStateOf(0) }

    var mostrarFormulario by remember { mutableStateOf(false) }
    var usuario by remember { mutableStateOf("") }
    var nombreCompleto by remember { mutableStateOf("") }
    var correoInstitucional by remember { mutableStateOf("") }
    var rolSeleccionado by remember { mutableStateOf("OPERADOR") }
    var formError by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var passwordGenerada by remember { mutableStateOf<Pair<String, String>?>(null) } // usuario a contraseña

    var confirmarCambio by remember { mutableStateOf<Pair<AdministradorCuenta, Map<String, Any?>>?>(null) }
    var aplicandoCambio by remember { mutableStateOf(false) }

    LaunchedEffect(recarga) {
        cargando = true
        AdminAccountsService.listar().fold(
            onSuccess = { cuentas = it; error = null },
            onFailure = { error = it.message ?: "No se pudieron cargar los administradores." }
        )
        cargando = false
    }

    Scaffold(
        containerColor = LoginCanvas,
        topBar = {
            TopAppBar(
                title = { Text("Administradores", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        usuario = ""; nombreCompleto = ""; correoInstitucional = ""; rolSeleccionado = "OPERADOR"
                        formError = null; mostrarFormulario = !mostrarFormulario
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Registrar administrador", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(LoginCanvas).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Solo un SUPERADMIN puede crear cuentas de administrador o cambiar su rol. Un OPERADOR tiene control total sobre alumnos, calificaciones y auditoría, pero no puede eliminar alumnos ni gestionar otras cuentas de administrador.",
                    color = TextMuted, fontSize = 12.sp
                )
            }
            if (mostrarFormulario) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Registrar administrador", color = LoginInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            CampoTexto(usuario, { usuario = it }, "Usuario", "Ej. control_escolar")
                            CampoTexto(nombreCompleto, { nombreCompleto = it }, "Nombre completo", "Ej. Juana Pérez")
                            CampoTexto(correoInstitucional, { correoInstitucional = it }, "Correo institucional (opcional)", "juana@teschi.edu.mx")
                            SelectorRol(rolSeleccionado) { rolSeleccionado = it }
                            if (formError != null) Text(formError!!, color = Color(0xFFB3261E), fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        formError = when {
                                            usuario.isBlank() -> "El usuario es obligatorio."
                                            nombreCompleto.isBlank() -> "El nombre completo es obligatorio."
                                            else -> null
                                        }
                                        if (formError == null) guardando = true
                                    },
                                    enabled = !guardando,
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                                ) { Text(if (guardando) "Guardando…" else "Registrar") }
                                OutlinedButton(onClick = { mostrarFormulario = false }, enabled = !guardando) { Text("Cancelar") }
                            }
                        }
                    }
                }
            }
            if (error != null) item { Text(error!!, color = Color(0xFFB3261E), fontSize = 12.sp) }
            if (cargando) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(18.dp))
                        Text("Cargando administradores…", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                items(cuentas) { cuenta ->
                    CuentaAdministradorRow(
                        cuenta = cuenta,
                        esUnoMismo = cuenta.usuario == UserSession.matricula,
                        onCambiarRol = { nuevoRol -> confirmarCambio = cuenta to mapOf("rol" to nuevoRol) },
                        onCambiarActivo = { nuevoActivo -> confirmarCambio = cuenta to mapOf("activo" to nuevoActivo) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    LaunchedEffect(guardando) {
        if (!guardando) return@LaunchedEffect
        AdminAccountsService.crear(usuario.trim(), nombreCompleto.trim(), rolSeleccionado, correoInstitucional.trim()).fold(
            onSuccess = { password -> passwordGenerada = usuario.trim() to password; mostrarFormulario = false; recarga++ },
            onFailure = { e -> formError = e.message ?: "No se pudo crear el administrador." }
        )
        guardando = false
    }

    LaunchedEffect(aplicandoCambio) {
        if (!aplicandoCambio) return@LaunchedEffect
        val (cuenta, cambios) = confirmarCambio ?: run { aplicandoCambio = false; return@LaunchedEffect }
        AdminAccountsService.actualizar(cuenta.usuario, cambios).fold(
            onSuccess = { confirmarCambio = null; recarga++ },
            onFailure = { e -> error = e.message ?: "No se pudo actualizar el administrador."; confirmarCambio = null }
        )
        aplicandoCambio = false
    }

    // ── Mostrar la contraseña generada UNA sola vez ───────────────────────
    passwordGenerada?.let { (usuarioCreado, password) ->
        AlertDialog(
            onDismissRequest = { passwordGenerada = null },
            title = { Text("Cuenta creada: $usuarioCreado") },
            text = {
                Column {
                    Text("Contraseña generada (guárdala ahora, no se vuelve a mostrar):")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(password, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GreenPrimary)
                }
            },
            confirmButton = {
                Button(onClick = { passwordGenerada = null }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                    Text("Ya la guardé")
                }
            }
        )
    }

    // ── Confirmación antes de cambiar rol/estado ──────────────────────────
    confirmarCambio?.let { (cuenta, cambios) ->
        val descripcion = when {
            cambios.containsKey("activo") && cambios["activo"] == false -> "¿Desactivar a ${cuenta.nombreCompleto}?"
            cambios.containsKey("activo") -> "¿Reactivar a ${cuenta.nombreCompleto}?"
            else -> "¿Cambiar el rol de ${cuenta.nombreCompleto} a ${cambios["rol"]}?"
        }
        AlertDialog(
            onDismissRequest = { if (!aplicandoCambio) confirmarCambio = null },
            title = { Text(descripcion) },
            confirmButton = {
                Button(onClick = { aplicandoCambio = true }, enabled = !aplicandoCambio, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                    Text(if (aplicandoCambio) "Aplicando…" else "Confirmar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarCambio = null }, enabled = !aplicandoCambio) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CampoTexto(value: String, onChange: (String) -> Unit, label: String, placeholder: String) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) }, placeholder = { Text(placeholder) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
            focusedTextColor = LoginInk, unfocusedTextColor = LoginInk,
            focusedLabelColor = GreenPrimary, unfocusedLabelColor = TextMuted
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorRol(rol: String, onChange: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = rol, onValueChange = {}, readOnly = true, label = { Text("Rol") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                focusedTextColor = LoginInk, unfocusedTextColor = LoginInk
            )
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            listOf("OPERADOR", "SUPERADMIN").forEach { opcion ->
                DropdownMenuItem(text = { Text(opcion) }, onClick = { onChange(opcion); abierto = false })
            }
        }
    }
}

@Composable
private fun CuentaAdministradorRow(
    cuenta: AdministradorCuenta,
    esUnoMismo: Boolean,
    onCambiarRol: (String) -> Unit,
    onCambiarActivo: (Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(32.dp))
                    Column {
                        Text(cuenta.nombreCompleto.ifBlank { cuenta.usuario }, color = LoginInk, fontWeight = FontWeight.Bold)
                        Text("${cuenta.usuario} · ${cuenta.rol}${if (esUnoMismo) " (tú)" else ""}", color = TextMuted, fontSize = 12.sp)
                    }
                }
                Text(
                    if (cuenta.activo) "Activo" else "Inactivo",
                    color = if (cuenta.activo) GreenPrimary else Color(0xFFB3261E),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
            }
            if (!esUnoMismo) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onCambiarRol(if (cuenta.rol == "SUPERADMIN") "OPERADOR" else "SUPERADMIN") }) {
                        Text(if (cuenta.rol == "SUPERADMIN") "Bajar a OPERADOR" else "Subir a SUPERADMIN")
                    }
                    Button(
                        onClick = { onCambiarActivo(!cuenta.activo) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (cuenta.activo) Color(0xFFB3261E) else GreenPrimary)
                    ) { Text(if (cuenta.activo) "Desactivar" else "Reactivar") }
                }
            }
        }
    }
}
