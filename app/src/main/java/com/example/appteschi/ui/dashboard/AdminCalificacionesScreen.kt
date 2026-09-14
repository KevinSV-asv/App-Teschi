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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appteschi.data.ManagedUser
import com.example.appteschi.service.AdminUsersService
import com.example.appteschi.service.AlumnoCalificaciones
import com.example.appteschi.service.CalificacionesAdminService
import com.example.appteschi.service.MateriaCalificacion
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.GreenSecondary
import com.example.appteschi.ui.theme.LoginCanvas
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.TextMuted

/**
 * Calificaciones de cualquier alumno — lee/edita HistorialAcademico vía
 * CalificacionesAdminService. Primero se elige el alumno, luego se editan
 * sus materias por semestre. Ver DEC-017.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCalificacionesScreen(navController: NavController) {
    var alumnos by remember { mutableStateOf<List<ManagedUser>>(emptyList()) }
    var cargandoDirectorio by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }
    var seleccionado by remember { mutableStateOf<String?>(null) }

    var alumno by remember { mutableStateOf<AlumnoCalificaciones?>(null) }
    var materias by remember { mutableStateOf<List<MateriaCalificacion>>(emptyList()) }
    var cargandoMaterias by remember { mutableStateOf(false) }
    var errorMaterias by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        AdminUsersService.fetch().onSuccess { alumnos = it }
        cargandoDirectorio = false
    }

    LaunchedEffect(seleccionado, recarga) {
        val matricula = seleccionado ?: return@LaunchedEffect
        cargandoMaterias = true
        errorMaterias = null
        CalificacionesAdminService.obtener(matricula).fold(
            onSuccess = { (a, m) -> alumno = a; materias = m },
            onFailure = { e -> errorMaterias = e.message ?: "No se pudo cargar las calificaciones." }
        )
        cargandoMaterias = false
    }

    Scaffold(
        containerColor = LoginCanvas,
        topBar = {
            TopAppBar(
                title = { Text("Calificaciones", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (seleccionado != null) { seleccionado = null; alumno = null; materias = emptyList() }
                        else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        if (seleccionado == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Selecciona un alumno", color = LoginInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Buscar por matrícula, nombre o carrera") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedTextColor = LoginInk, unfocusedTextColor = LoginInk
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (cargandoDirectorio) {
                    CircularProgressIndicator(color = GreenPrimary)
                } else {
                    val filtrados = alumnos.filter { user ->
                        val q = search.trim()
                        q.isBlank() || listOf(user.username, user.name, user.carrera).any { it.contains(q, ignoreCase = true) }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filtrados) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { seleccionado = user.username },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(user.name.ifBlank { user.username }, color = LoginInk, fontWeight = FontWeight.Bold)
                                    Text("${user.username} · ${user.carrera.ifBlank { "Sin carrera" }}", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(LoginCanvas).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val a = alumno
                if (a != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(a.nombreCompleto, color = LoginInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${a.matricula} · ${a.carrera.ifBlank { "Sin carrera" }} · Semestre: ${a.semestre ?: "sin asignar"}", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (errorMaterias != null) {
                item { Text(errorMaterias!!, color = Color(0xFFB3261E), fontSize = 12.sp) }
            }
            if (cargandoMaterias) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(18.dp))
                        Text("Cargando materias…", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else if (materias.isEmpty()) {
                item { Text("Este alumno no tiene carrera asignada o el plan de estudios está vacío.", color = TextMuted, fontSize = 13.sp) }
            } else {
                materias.groupBy { it.semestre }.toSortedMap().forEach { (semestre, delSemestre) ->
                    item {
                        Text("Semestre $semestre", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    items(delSemestre, key = { it.idMateria }) { materia ->
                        MateriaCalificacionRowEditable(
                            materia = materia,
                            onGuardar = { nuevaCalificacion ->
                                CalificacionesAdminService.actualizar(seleccionado!!, materia.idMateria, nuevaCalificacion)
                                    .onSuccess { recarga++ }
                            }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MateriaCalificacionRowEditable(materia: MateriaCalificacion, onGuardar: suspend (Double?) -> Unit) {
    var texto by remember(materia.idMateria, materia.calificacion) { mutableStateOf(materia.calificacion?.toString().orEmpty()) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(guardando) {
        if (!guardando) return@LaunchedEffect
        val valor = texto.trim()
        val calificacion = if (valor.isBlank()) null else valor.replace(',', '.').toDoubleOrNull()
        if (valor.isNotBlank() && (calificacion == null || calificacion < 0 || calificacion > 10)) {
            error = "0 a 10"
            guardando = false
            return@LaunchedEffect
        }
        error = null
        onGuardar(calificacion)
        guardando = false
    }

    val (colorFondo, colorTexto) = when (materia.estatusCodigo) {
        "AP" -> Color(0xFFE8F5E9) to GreenPrimary
        "NA" -> Color(0xFFFFEBEE) to Color(0xFFB3261E)
        else -> Color(0xFFFFF3E0) to Color(0xFFB37B00)
    }

    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(materia.nombre, color = LoginInk, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("${materia.creditos} créditos", color = TextMuted, fontSize = 11.sp)
                Card(colors = CardDefaults.cardColors(containerColor = colorFondo), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        materia.estatusNombre ?: "Sin calificación",
                        color = colorTexto, fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = texto, onValueChange = { texto = it },
                singleLine = true,
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(90.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                    focusedTextColor = LoginInk, unfocusedTextColor = LoginInk
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { guardando = true },
                enabled = !guardando,
                colors = ButtonDefaults.buttonColors(containerColor = GreenSecondary)
            ) { Text(if (guardando) "…" else "Guardar", fontSize = 12.sp) }
        }
    }
    if (error != null) {
        Text(error!!, color = Color(0xFFB3261E), fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp))
    }
}
