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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
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
import com.example.appteschi.data.ManagedUser
import com.example.appteschi.service.AdminAuditService
import com.example.appteschi.service.AdminUsersService
import com.example.appteschi.service.CalificacionesAdminService
import com.example.appteschi.service.MateriaCalificacion
import com.example.appteschi.service.RemoteAuditEvent
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginCanvas
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.OnPrimaryWhite
import com.example.appteschi.ui.theme.TextMuted

private const val SEMESTRE_MAXIMO = 12

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreen(navController: NavController, matricula: String) {
    var perfil by remember { mutableStateOf<ManagedUser?>(null) }
    var movimientos by remember { mutableStateOf<List<RemoteAuditEvent>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var historial by remember { mutableStateOf<List<MateriaCalificacion>>(emptyList()) }
    var cargandoHistorial by remember { mutableStateOf(false) }
    var errorHistorial by remember { mutableStateOf<String?>(null) }

    var guardandoSemestre by remember { mutableStateOf(false) }
    var errorSemestre by remember { mutableStateOf<String?>(null) }
    var semestreObjetivo by remember { mutableStateOf<Int?>(null) }

    var confirmarBaja by remember { mutableStateOf(false) }
    var procesandoBaja by remember { mutableStateOf(false) }
    var errorBaja by remember { mutableStateOf<String?>(null) }

    var recarga by remember { mutableStateOf(0) }

    LaunchedEffect(matricula, recarga) {
        cargando = true
        error = null
        val usuarios = AdminUsersService.fetch()
        val auditoria = AdminAuditService.fetch(matricula)
        perfil = usuarios.getOrNull()?.firstOrNull { it.username.equals(matricula, true) }
        movimientos = auditoria.getOrNull().orEmpty()
        error = when {
            usuarios.isFailure && auditoria.isFailure ->
                "No se pudo cargar el perfil ni la auditoría. Verifica que la API esté activa."
            perfil == null ->
                "El perfil $matricula no está en la base de datos."
            auditoria.isFailure ->
                "Perfil cargado, pero no se pudo leer la auditoría remota."
            else -> null
        }
        cargando = false
    }

    // Historial de materias con sus calificaciones — se recarga junto con el perfil.
    LaunchedEffect(perfil?.claveCarrera, recarga) {
        val clave = perfil?.claveCarrera
        if (clave.isNullOrBlank()) {
            historial = emptyList()
            return@LaunchedEffect
        }
        cargandoHistorial = true
        errorHistorial = null
        CalificacionesAdminService.obtener(matricula).fold(
            onSuccess = { (_, materias) -> historial = materias },
            onFailure = { errorHistorial = "No se pudo cargar el historial de materias." }
        )
        cargandoHistorial = false
    }

    LaunchedEffect(semestreObjetivo) {
        val nuevo = semestreObjetivo ?: return@LaunchedEffect
        guardandoSemestre = true
        errorSemestre = null
        AdminUsersService.actualizarPerfil(matricula, mapOf("semestre" to nuevo)).fold(
            onSuccess = { recarga++ },
            onFailure = { e -> errorSemestre = e.message ?: "No se pudo guardar el semestre." }
        )
        guardandoSemestre = false
        semestreObjetivo = null
    }

    LaunchedEffect(procesandoBaja) {
        if (!procesandoBaja) return@LaunchedEffect
        val nuevoEstado = !(perfil?.active ?: true)
        AdminUsersService.actualizarPerfil(matricula, mapOf("activo" to nuevoEstado)).fold(
            onSuccess = { confirmarBaja = false; recarga++ },
            onFailure = { e -> errorBaja = e.message ?: "No se pudo actualizar el estado del alumno."; confirmarBaja = false }
        )
        procesandoBaja = false
    }

    Scaffold(
        containerColor = LoginCanvas,
        topBar = {
            TopAppBar(
                title = { Text("Perfil y auditoría", color = OnPrimaryWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = OnPrimaryWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        if (cargando) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = GreenPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Cargando perfil desde la base de datos…", color = TextMuted)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(LoginCanvas)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error != null) {
                item { Text(error!!, color = Color(0xFFB3261E), fontSize = 13.sp) }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary)
                            Text("Información del perfil", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        val user = perfil
                        if (user == null) {
                            Text("Sin datos de perfil.", color = TextMuted)
                        } else {
                            Dato("ID", user.id.toString())
                            Dato("Matrícula", user.username)
                            Dato("Nombre", user.name.ifBlank { "Sin nombre" })
                            Dato("Correo", user.correo.ifBlank { "Sin correo" })
                            Dato("Correo institucional", user.correoInstitucional.ifBlank { "Sin correo institucional" })
                            Dato("Carrera / tipo", user.carrera.ifBlank { user.role.label })
                            Dato("Rol en app", user.role.label)
                            Dato("Estado", if (user.active) "Activo" else "Dado de baja")
                        }
                    }
                }
            }
            if (perfil != null) {
                item {
                    SemestreCard(
                        perfil = perfil!!,
                        guardando = guardandoSemestre,
                        error = errorSemestre,
                        onSeleccionarSemestre = { semestreObjetivo = it }
                    )
                }
                item {
                    HistorialMateriasCard(
                        cargando = cargandoHistorial,
                        error = errorHistorial,
                        materias = historial
                    )
                }
                item {
                    BajaCard(
                        activo = perfil!!.active,
                        error = errorBaja,
                        onSolicitarCambio = { confirmarBaja = true }
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = GreenPrimary)
                    Text("Movimientos de este perfil", color = LoginInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text(
                    "Incluye acciones hechas por el usuario y acciones administrativas sobre su cuenta.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
            if (movimientos.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Text(
                            "Este perfil no tiene movimientos registrados todavía.",
                            color = TextMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(movimientos) { evento ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(evento.accion, color = LoginInk, fontWeight = FontWeight.Bold)
                            Text(evento.fecha, color = TextMuted, fontSize = 11.sp)
                            Text(evento.detalle.ifBlank { "Sin detalle" }, color = LoginInk, fontSize = 13.sp)
                            Text(
                                "Actor: ${evento.actorNombre} (${evento.actorMatricula})",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmarBaja) {
        val activo = perfil?.active ?: true
        AlertDialog(
            onDismissRequest = { if (!procesandoBaja) confirmarBaja = false },
            title = { Text(if (activo) "¿Dar de baja a este alumno?" else "¿Reactivar a este alumno?") },
            text = {
                Text(
                    if (activo) "El alumno quedará marcado como inactivo. Su historial y calificaciones se conservan; puedes reactivarlo después."
                    else "El alumno volverá a aparecer como activo en el directorio."
                )
            },
            confirmButton = {
                Button(
                    onClick = { procesandoBaja = true },
                    enabled = !procesandoBaja,
                    colors = ButtonDefaults.buttonColors(containerColor = if (activo) Color(0xFFB3261E) else GreenPrimary)
                ) { Text(if (procesandoBaja) "Procesando…" else if (activo) "Dar de baja" else "Reactivar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarBaja = false }, enabled = !procesandoBaja) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("$etiqueta: ", color = TextMuted, fontSize = 13.sp)
        Text(valor, color = LoginInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Asignación de semestre: una vez que el alumno tiene un semestre asignado,
 * solo se permite avanzar (nunca retroceder) — el desplegable solo ofrece
 * semestres superiores al actual. Ver DEC-018.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SemestreCard(
    perfil: ManagedUser,
    guardando: Boolean,
    error: String?,
    onSeleccionarSemestre: (Int) -> Unit
) {
    var menuAbierto by remember { mutableStateOf(false) }
    val actual = perfil.semestre
    val opciones = ((actual?.plus(1) ?: 1)..SEMESTRE_MAXIMO).toList()

    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = GreenPrimary)
                Text("Semestre", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text(
                if (actual == null) "Este alumno todavía no tiene semestre asignado."
                else "Semestre actual: $actual. Solo puede avanzar — no se permite regresar a un semestre anterior.",
                color = TextMuted, fontSize = 12.sp
            )
            if (error != null) Text(error, color = Color(0xFFB3261E), fontSize = 12.sp)

            if (opciones.isEmpty()) {
                Text("Este alumno ya está en el semestre máximo ($SEMESTRE_MAXIMO).", color = TextMuted, fontSize = 12.sp)
            } else {
                ExposedDropdownMenuBox(expanded = menuAbierto, onExpandedChange = { menuAbierto = it }) {
                    OutlinedTextField(
                        value = if (guardando) "Guardando…" else "Avanzar a…",
                        onValueChange = {}, readOnly = true, enabled = !guardando,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAbierto) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                            focusedTextColor = LoginInk, unfocusedTextColor = LoginInk
                        )
                    )
                    ExposedDropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        opciones.forEach { semestre ->
                            DropdownMenuItem(
                                text = { Text("Semestre $semestre") },
                                onClick = { menuAbierto = false; onSeleccionarSemestre(semestre) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorialMateriasCard(cargando: Boolean, error: String?, materias: List<MateriaCalificacion>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.History, contentDescription = null, tint = GreenPrimary)
                Text("Historial de materias", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            when {
                cargando -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(16.dp))
                    Text("Consultando calificaciones…", color = TextMuted, fontSize = 12.sp)
                }
                error != null -> Text(error, color = Color(0xFFB3261E), fontSize = 12.sp)
                materias.isEmpty() -> Text(
                    "Este alumno no tiene carrera asignada o el plan de estudios está vacío.",
                    color = TextMuted, fontSize = 12.sp
                )
                else -> materias.groupBy { it.semestre }.toSortedMap().forEach { (semestre, delSemestre) ->
                    Text("Semestre $semestre", color = LoginInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    delSemestre.forEach { materia -> HistorialMateriaRow(materia) }
                }
            }
        }
    }
}

@Composable
private fun HistorialMateriaRow(materia: MateriaCalificacion) {
    val (colorFondo, colorTexto) = when (materia.estatusCodigo) {
        "AP" -> Color(0xFFE8F5E9) to GreenPrimary
        "NA" -> Color(0xFFFFEBEE) to Color(0xFFB3261E)
        else -> Color(0xFFFFF3E0) to Color(0xFFB37B00)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(materia.nombre, color = LoginInk, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (materia.calificacion != null) {
                Text("%.1f".format(materia.calificacion), color = colorTexto, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Card(colors = CardDefaults.cardColors(containerColor = colorFondo), shape = RoundedCornerShape(6.dp)) {
                Text(
                    materia.estatusNombre ?: "Por cursar",
                    color = colorTexto, fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BajaCard(activo: Boolean, error: String?, onSolicitarCambio: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PersonOff, contentDescription = null, tint = GreenPrimary)
                Text("Baja de alumno", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text(
                if (activo) "Este alumno está activo. Puedes darlo de baja si ya no cursa en la institución."
                else "Este alumno está dado de baja. Puedes reactivarlo si vuelve a inscribirse.",
                color = TextMuted, fontSize = 12.sp
            )
            if (error != null) Text(error, color = Color(0xFFB3261E), fontSize = 12.sp)
            Button(
                onClick = onSolicitarCambio,
                colors = ButtonDefaults.buttonColors(containerColor = if (activo) Color(0xFFB3261E) else GreenPrimary)
            ) { Text(if (activo) "Dar de baja" else "Reactivar") }
        }
    }
}
