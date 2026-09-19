package com.example.appteschi.ui.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appteschi.Routes
import com.example.appteschi.data.CarreraCatalogo
import com.example.appteschi.data.ManagedUser
import com.example.appteschi.data.UserSession
import com.example.appteschi.service.AdminUsersService
import com.example.appteschi.service.PlanEstudiosService
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginCanvas
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.TextMuted
import com.example.appteschi.ui.theme.ErrorRed

/**
 * CRUD real de alumnos contra AppTeschiDB — reemplaza el directorio local
 * (AdminDirectory) que solo vivía en SharedPreferences. Ver DEC-017.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAlumnosScreen(navController: NavController) {
    var alumnos by remember { mutableStateOf<List<ManagedUser>>(emptyList()) }
    var carreras by remember { mutableStateOf<List<CarreraCatalogo>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var listError by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var recarga by remember { mutableStateOf(0) }

    var mostrarFormulario by remember { mutableStateOf(false) }
    var editingMatricula by remember { mutableStateOf<String?>(null) }
    var matricula by remember { mutableStateOf("") }
    var nombreCompleto by remember { mutableStateOf("") }
    var correoOtp by remember { mutableStateOf("") }
    var correoInstitucional by remember { mutableStateOf("") }
    var claveCarrera by remember { mutableStateOf("") }
    var semestreTexto by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    var confirmarEliminar by remember { mutableStateOf<ManagedUser?>(null) }
    var eliminando by remember { mutableStateOf(false) }

    LaunchedEffect(recarga) {
        cargando = true
        val resultado = AdminUsersService.fetch()
        resultado.onSuccess { alumnos = it; listError = null }
            .onFailure { listError = it.message ?: "No se pudo cargar el directorio desde la base de datos." }
        if (carreras.isEmpty()) {
            PlanEstudiosService.carreras().onSuccess { carreras = it }
        }
        cargando = false
    }

    fun limpiarFormulario() {
        editingMatricula = null; matricula = ""; nombreCompleto = ""
        correoOtp = ""; correoInstitucional = ""; claveCarrera = ""; semestreTexto = ""
        formError = null
    }

    Scaffold(
        containerColor = LoginCanvas,
        topBar = {
            TopAppBar(
                title = { Text("Alumnos", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        limpiarFormulario()
                        mostrarFormulario = !mostrarFormulario
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Registrar alumno", tint = Color.White)
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
            if (mostrarFormulario) {
                item {
                    AlumnoFormCard(
                        esNuevo = editingMatricula == null,
                        matricula = matricula, onMatriculaChange = { matricula = it },
                        nombreCompleto = nombreCompleto, onNombreChange = { nombreCompleto = it },
                        correoOtp = correoOtp, onCorreoOtpChange = { correoOtp = it },
                        correoInstitucional = correoInstitucional, onCorreoInstitucionalChange = { correoInstitucional = it },
                        carreras = carreras, claveCarrera = claveCarrera, onClaveCarreraChange = { claveCarrera = it },
                        semestreTexto = semestreTexto, onSemestreTextoChange = { semestreTexto = it },
                        error = formError,
                        guardando = guardando,
                        matriculaEditable = editingMatricula == null,
                        onCancelar = { mostrarFormulario = false; limpiarFormulario() },
                        onGuardar = {
                            val mat = matricula.trim()
                            val nombre = nombreCompleto.trim()
                            val clave = claveCarrera.trim()
                            val semestre = semestreTexto.trim().toIntOrNull()
                            formError = when {
                                mat.isBlank() -> "La matrícula es obligatoria."
                                nombre.isBlank() -> "El nombre completo es obligatorio."
                                clave.isBlank() -> "Selecciona una carrera."
                                semestreTexto.isNotBlank() && (semestre == null || semestre < 1 || semestre > 12) ->
                                    "El semestre debe ser un número entre 1 y 12."
                                else -> null
                            }
                            if (formError == null) guardando = true
                        }
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Buscar por matrícula, nombre, correo o carrera") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedTextColor = LoginInk, unfocusedTextColor = LoginInk
                    )
                )
            }
            if (listError != null) {
                item { Text(listError!!, color = ErrorRed, fontSize = 12.sp) }
            }
            if (cargando) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(18.dp))
                        Text("Cargando alumnos…", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                val filtrados = alumnos.filter { user ->
                    val q = search.trim()
                    q.isBlank() || listOf(user.username, user.name, user.correo, user.correoInstitucional, user.carrera)
                        .any { it.contains(q, ignoreCase = true) }
                }
                item { Text("${filtrados.size} de ${alumnos.size} registros", color = TextMuted, fontSize = 12.sp) }
                items(filtrados) { user ->
                    AlumnoRow(
                        user = user,
                        onVerPerfil = { navController.navigate(Routes.adminProfile(user.username)) },
                        onEditar = {
                            editingMatricula = user.username
                            matricula = user.username
                            nombreCompleto = user.name
                            correoOtp = user.correo
                            correoInstitucional = user.correoInstitucional
                            claveCarrera = user.claveCarrera
                            semestreTexto = user.semestre?.toString().orEmpty()
                            formError = null
                            mostrarFormulario = true
                        },
                        onEliminar = { confirmarEliminar = user }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // ── Efecto que ejecuta el guardado (crear o editar) ──────────────────
    LaunchedEffect(guardando) {
        if (!guardando) return@LaunchedEffect
        val mat = matricula.trim()
        val semestre = semestreTexto.trim().toIntOrNull()
        val resultado = if (editingMatricula == null) {
            AdminUsersService.crear(
                matricula = mat, nombreCompleto = nombreCompleto.trim(), claveCarrera = claveCarrera.trim(),
                correoOtp = correoOtp.trim(), correoInstitucional = correoInstitucional.trim(), semestre = semestre
            )
        } else {
            AdminUsersService.actualizarPerfil(
                editingMatricula!!,
                mapOf(
                    "nombreCompleto" to nombreCompleto.trim(),
                    "correoOtp" to correoOtp.trim(),
                    "correoInstitucional" to correoInstitucional.trim(),
                    "claveCarrera" to claveCarrera.trim(),
                    "semestre" to semestre
                )
            )
        }
        resultado.fold(
            onSuccess = {
                mostrarFormulario = false
                limpiarFormulario()
                recarga++
            },
            onFailure = { e -> formError = e.message ?: "No se pudo guardar el registro." }
        )
        guardando = false
    }

    // ── Confirmación de eliminación ───────────────────────────────────────
    val objetivo = confirmarEliminar
    if (objetivo != null) {
        AlertDialog(
            onDismissRequest = { if (!eliminando) confirmarEliminar = null },
            title = { Text("¿Eliminar a ${objetivo.name.ifBlank { objetivo.username }}?") },
            text = {
                Text(
                    "Se eliminará el alumno, su cuenta local (si tiene) y todas sus calificaciones. " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        eliminando = true
                        // Se dispara desde el propio botón: no hay corrutina en scope de AlertDialog,
                        // así que se maneja con un LaunchedEffect ligado a `eliminando`.
                    },
                    enabled = !eliminando,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text(if (eliminando) "Eliminando…" else "Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarEliminar = null }, enabled = !eliminando) { Text("Cancelar") }
            }
        )
    }

    LaunchedEffect(eliminando) {
        if (!eliminando) return@LaunchedEffect
        val user = objetivo ?: run { eliminando = false; return@LaunchedEffect }
        AdminUsersService.eliminar(user.username).fold(
            onSuccess = { confirmarEliminar = null; recarga++ },
            onFailure = { e -> listError = e.message ?: "No se pudo eliminar el alumno."; confirmarEliminar = null }
        )
        eliminando = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlumnoFormCard(
    esNuevo: Boolean,
    matricula: String, onMatriculaChange: (String) -> Unit,
    nombreCompleto: String, onNombreChange: (String) -> Unit,
    correoOtp: String, onCorreoOtpChange: (String) -> Unit,
    correoInstitucional: String, onCorreoInstitucionalChange: (String) -> Unit,
    carreras: List<CarreraCatalogo>, claveCarrera: String, onClaveCarreraChange: (String) -> Unit,
    semestreTexto: String, onSemestreTextoChange: (String) -> Unit,
    error: String?,
    guardando: Boolean,
    matriculaEditable: Boolean,
    onCancelar: () -> Unit,
    onGuardar: () -> Unit
) {
    var carreraMenuAbierto by remember { mutableStateOf(false) }
    val nombreCarreraSeleccionada = carreras.firstOrNull { it.clave == claveCarrera }?.nombre ?: claveCarrera

    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (esNuevo) "Registrar alumno" else "Editar alumno",
                color = LoginInk, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = matricula, onValueChange = onMatriculaChange,
                label = { Text("Matrícula") }, placeholder = { Text("Ej. 202400123") },
                enabled = matriculaEditable, singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = camposColors()
            )
            OutlinedTextField(
                value = nombreCompleto, onValueChange = onNombreChange,
                label = { Text("Nombre completo") }, placeholder = { Text("Ej. Juan Pérez") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = camposColors()
            )
            OutlinedTextField(
                value = correoOtp, onValueChange = onCorreoOtpChange,
                label = { Text("Correo") }, placeholder = { Text("Ej. alumno@gmail.com") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = camposColors()
            )
            OutlinedTextField(
                value = correoInstitucional, onValueChange = onCorreoInstitucionalChange,
                label = { Text("Correo institucional") }, placeholder = { Text("Se genera solo si se deja vacío") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = camposColors()
            )
            ExposedDropdownMenuBox(expanded = carreraMenuAbierto, onExpandedChange = { carreraMenuAbierto = it }) {
                OutlinedTextField(
                    value = nombreCarreraSeleccionada, onValueChange = {},
                    readOnly = true, label = { Text("Carrera") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = carreraMenuAbierto) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    colors = camposColors()
                )
                ExposedDropdownMenu(expanded = carreraMenuAbierto, onDismissRequest = { carreraMenuAbierto = false }) {
                    carreras.forEach { carrera ->
                        DropdownMenuItem(
                            text = { Text("${carrera.nombre} (${carrera.clave})") },
                            onClick = { onClaveCarreraChange(carrera.clave); carreraMenuAbierto = false }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = semestreTexto, onValueChange = onSemestreTextoChange,
                label = { Text("Semestre (opcional)") }, placeholder = { Text("1 a 12") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(180.dp), colors = camposColors()
            )
            if (error != null) Text(error, color = ErrorRed, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onGuardar, enabled = !guardando, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                    Text(if (guardando) "Guardando…" else if (esNuevo) "Registrar" else "Guardar cambios")
                }
                OutlinedButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") }
            }
        }
    }
}

@Composable
private fun camposColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
    focusedTextColor = LoginInk, unfocusedTextColor = LoginInk,
    focusedLabelColor = GreenPrimary, unfocusedLabelColor = TextMuted
)

@Composable
private fun AlumnoRow(user: ManagedUser, onVerPerfil: () -> Unit, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(36.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            user.name.ifBlank { user.username },
                            color = LoginInk,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${user.username} · ${user.carrera.ifBlank { "Sin carrera" }}",
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Semestre: ${user.semestre?.toString() ?: "sin asignar"}", color = TextMuted, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (user.active) "Activo" else "Inactivo",
                    color = if (user.active) GreenPrimary else ErrorRed,
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onVerPerfil, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                    Text("Ver perfil")
                }
                OutlinedButton(onClick = onEditar) { Text("Editar") }
                // Eliminar es SUPERADMIN-only en el backend (ver DEC-020) — un
                // OPERADOR ni ve el botón, para no ofrecer algo que el
                // servidor rechazaría con 403.
                if (UserSession.rol == "SUPERADMIN") {
                    Button(onClick = onEliminar, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) {
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}
