package com.example.appteschi.ui.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appteschi.data.CarreraCatalogo
import com.example.appteschi.service.HorarioAdminApi
import com.example.appteschi.service.HorarioAdminService
import com.example.appteschi.service.HorarioClase
import com.example.appteschi.service.PlanEstudiosApi
import com.example.appteschi.service.PlanEstudiosService
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginCanvas
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.TextMuted
import kotlinx.coroutines.launch
import com.example.appteschi.ui.theme.ErrorRed
import com.example.appteschi.ui.theme.GreenTertiary

private val NOMBRE_DIA = listOf("", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

/**
 * Panel del administrador para subir el horario real de clases (Excel) de
 * una carrera+semestre — ver DEC-029. La app del alumno lee lo que aquí se
 * suba desde "Materias y Horario de Hoy" en Inicio. Formato esperado del
 * Excel: primera fila con encabezados Materia, Dia, HoraInicio, HoraFin,
 * Profesor, Aula, Modalidad (esta última opcional).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHorariosScreen(
    navController: NavController,
    servicio: HorarioAdminApi = HorarioAdminService,
    catalogoServicio: PlanEstudiosApi = PlanEstudiosService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var carreras by remember { mutableStateOf<List<CarreraCatalogo>>(emptyList()) }
    var claveCarrera by remember { mutableStateOf("") }
    var semestreTexto by remember { mutableStateOf("") }
    var grupoTexto by remember { mutableStateOf("") }
    var periodoTexto by remember { mutableStateOf("") }
    var archivoUri by remember { mutableStateOf<Uri?>(null) }
    var nombreArchivo by remember { mutableStateOf("") }

    var subiendo by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var horarioActual by remember { mutableStateOf<List<HorarioClase>>(emptyList()) }
    var cargandoLista by remember { mutableStateOf(false) }
    var recarga by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        catalogoServicio.carreras().onSuccess { carreras = it }
    }

    val semestre = semestreTexto.toIntOrNull()
    LaunchedEffect(claveCarrera, semestre, recarga) {
        if (claveCarrera.isBlank() || semestre == null) { horarioActual = emptyList(); return@LaunchedEffect }
        cargandoLista = true
        servicio.listar(claveCarrera, semestre).fold(
            onSuccess = { horarioActual = it },
            onFailure = { horarioActual = emptyList() }
        )
        cargandoLista = false
    }

    val seleccionarArchivo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            archivoUri = uri
            nombreArchivo = uri.lastPathSegment?.substringAfterLast('/') ?: "horario.xlsx"
        }
    }

    Scaffold(
        containerColor = LoginCanvas,
        topBar = {
            TopAppBar(
                title = { Text("Horarios", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LoginCanvas)
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Sube el Excel con el horario real (Materia, Dia, HoraInicio, HoraFin, Profesor, Aula, Modalidad) de una carrera y semestre. Al volver a subir, reemplaza por completo el horario anterior de esa carrera+semestre.",
                color = TextMuted, fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CarreraDropdown(carreras, claveCarrera) { claveCarrera = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = semestreTexto, onValueChange = { semestreTexto = it },
                            label = { Text("Semestre") }, placeholder = { Text("1-12") },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(120.dp)
                        )
                        OutlinedTextField(
                            value = grupoTexto, onValueChange = { grupoTexto = it },
                            label = { Text("Grupo (opcional)") }, singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = periodoTexto, onValueChange = { periodoTexto = it },
                        label = { Text("Periodo (opcional)") }, placeholder = { Text("Ej. 2026-1") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { seleccionarArchivo.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (nombreArchivo.isBlank()) "Seleccionar archivo Excel (.xlsx)" else nombreArchivo)
                    }
                    if (error != null) Text(error!!, color = ErrorRed, fontSize = 12.sp)
                    if (mensaje != null) Text(mensaje!!, color = GreenTertiary, fontSize = 12.sp)
                    Button(
                        onClick = {
                            val uri = archivoUri
                            if (claveCarrera.isBlank() || semestre == null || uri == null) {
                                error = "Selecciona carrera, semestre y un archivo."
                                return@Button
                            }
                            subiendo = true; error = null; mensaje = null
                            scope.launch {
                                val bytes = try {
                                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                } catch (e: Exception) { null }
                                if (bytes == null) {
                                    error = "No se pudo leer el archivo seleccionado."
                                    subiendo = false
                                    return@launch
                                }
                                servicio.importar(claveCarrera, semestre, grupoTexto.trim().ifBlank { null }, periodoTexto.trim().ifBlank { null }, nombreArchivo, bytes).fold(
                                    onSuccess = { resultado ->
                                        mensaje = "Se importaron ${resultado.filasImportadas} clases." +
                                            if (resultado.erroresFilas.isNotEmpty()) " Filas ignoradas: ${resultado.erroresFilas.joinToString("; ")}" else ""
                                        archivoUri = null; nombreArchivo = ""
                                        recarga++
                                    },
                                    onFailure = { error = it.message ?: "No se pudo importar el horario." }
                                )
                                subiendo = false
                            }
                        },
                        enabled = !subiendo,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Text(if (subiendo) "Subiendo…" else "Subir horario")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Horario actual", color = LoginInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            when {
                claveCarrera.isBlank() || semestre == null ->
                    Text("Elige carrera y semestre para ver su horario cargado.", color = TextMuted, fontSize = 12.sp)
                cargandoLista -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consultando…", color = TextMuted, fontSize = 12.sp)
                }
                horarioActual.isEmpty() -> Text("Todavía no hay horario cargado para esa carrera y semestre.", color = TextMuted, fontSize = 12.sp)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(horarioActual, key = { it.id }) { clase ->
                        HorarioRow(clase) {
                            scope.launch {
                                servicio.eliminar(clase.id).onSuccess { recarga++ }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarreraDropdown(carreras: List<CarreraCatalogo>, claveCarrera: String, onChange: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    val nombreSeleccionado = carreras.firstOrNull { it.clave == claveCarrera }?.nombre ?: claveCarrera
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = nombreSeleccionado, onValueChange = {},
            readOnly = true, label = { Text("Carrera") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            carreras.forEach { carrera ->
                DropdownMenuItem(
                    text = { Text("${carrera.nombre} (${carrera.clave})") },
                    onClick = { onChange(carrera.clave); expandido = false }
                )
            }
        }
    }
}

@Composable
private fun HorarioRow(clase: HorarioClase, onEliminar: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(clase.materia, color = LoginInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "${NOMBRE_DIA.getOrElse(clase.diaSemana) { "?" }} · ${clase.horaInicio}-${clase.horaFin}" +
                        (clase.profesor?.let { " · $it" } ?: "") + (clase.aula?.let { " · $it" } ?: ""),
                    color = TextMuted, fontSize = 11.sp
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = ErrorRed)
            }
        }
    }
}
