package com.example.appteschi.ui.login

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.Routes
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.LoginSurface
import com.example.appteschi.viewmodel.RegistroViewModel
import java.util.Calendar
import com.example.appteschi.ui.theme.ErrorRed

private val sistemas = listOf(
    "ESCOLARIZADO" to "Sistema Escolarizado",
    "ABIERTO"      to "Sistema Abierto",
    "DUAL"         to "Modelo Dual"
)

private val carreras = listOf(
    "ANIMACION"               to "Ing. en Animación Digital y Efectos Visuales",
    "ISC"                     to "Ingeniería en Sistemas Computacionales",
    "INDUSTRIAL"              to "Ingeniería Industrial",
    "MECATRONICA"             to "Ingeniería Mecatrónica",
    "QUIMICA"                 to "Ingeniería Química",
    "ADMINISTRACION"          to "Licenciatura en Administración",
    "GASTRONOMIA"             to "Licenciatura en Gastronomía",
    "INDUSTRIAL_DISTANCIA"    to "Ingeniería Industrial (distancia)",
    "ADMINISTRACION_DISTANCIA" to "Licenciatura en Administración (distancia)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    navController: NavController,
    registroViewModel: RegistroViewModel = viewModel()
) {
    val uiState by registroViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var matricula by remember { mutableStateOf("") }
    var nombres   by remember { mutableStateOf("") }
    var paterno   by remember { mutableStateOf("") }
    var materno   by remember { mutableStateOf("") }
    var fecha     by remember { mutableStateOf("") }   // formato AAAA-MM-DD
    var sistema   by remember { mutableStateOf("") }
    var carrera   by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }

    // ── DatePickerDialog nativo de Android ───────────────────────────────────
    val hoy = Calendar.getInstance()
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            // Formato AAAA-MM-DD con ceros a la izquierda
            fecha = "%04d-%02d-%02d".format(year, month + 1, day)
        },
        hoy.get(Calendar.YEAR) - 18,  // año por defecto: 18 años atrás
        hoy.get(Calendar.MONTH),
        hoy.get(Calendar.DAY_OF_MONTH)
    ).apply {
        // No permitir fechas futuras para fecha de nacimiento
        datePicker.maxDate = hoy.timeInMillis
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear una cuenta", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Registro de cuenta AppTESCHI", color = LoginInk, fontSize = 24.sp)
            Text("Completa tus datos para crear tu acceso.", color = Color.Gray)

            Card(colors = CardDefaults.cardColors(containerColor = LoginSurface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ── Datos personales ──────────────────────────────────
                    RegisterField("Matrícula / ID", matricula)   { matricula = it }
                    RegisterField("Nombre(s)",       nombres)    { nombres   = it }
                    RegisterField("Apellido paterno", paterno)   { paterno   = it }
                    RegisterField("Apellido materno", materno)   { materno   = it }

                    // ── Fecha de nacimiento — selector visual ─────────────
                    OutlinedTextField(
                        value         = if (fecha.isBlank()) "" else fecha,
                        onValueChange = { /* solo lectura — se establece por el picker */ },
                        label         = { Text("Fecha de nacimiento") },
                        placeholder   = { Text("Toca el calendario para seleccionar") },
                        readOnly      = true,
                        modifier      = Modifier.fillMaxWidth(),
                        trailingIcon  = {
                            IconButton(onClick = { datePicker.show() }) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Abrir calendario",
                                    tint = GreenPrimary
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor  = Color.White,
                            focusedTextColor        = Color.Black,
                            unfocusedTextColor      = Color.Black,
                            disabledTextColor       = Color.Black,
                            focusedLabelColor       = GreenPrimary,
                            unfocusedLabelColor     = Color.Gray
                        )
                    )
                    // Tap en el campo también abre el picker (además del ícono)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.dp)  // invisible — el outline cubre el área
                            .clickable { datePicker.show() }
                    )

                    // ── Correo institucional (vista previa) ───────────────
                    Text(
                        "Correo institucional: ${
                            matricula.trim().lowercase().ifBlank { "matricula" }
                        }@teschi.edu.mx",
                        color   = GreenPrimary,
                        fontSize = 13.sp
                    )

                    // ── Sistema y carrera ─────────────────────────────────
                    RegisterDropdown(
                        label    = "Sistema",
                        value    = sistemas.firstOrNull { it.first == sistema }?.second.orEmpty(),
                        options  = sistemas.map { it.second },
                        onSelected = { selected ->
                            sistema = sistemas.first { it.second == selected }.first
                        }
                    )
                    RegisterDropdown(
                        label    = "Carrera",
                        value    = carreras.firstOrNull { it.first == carrera }?.second.orEmpty(),
                        options  = carreras.map { it.second },
                        onSelected = { selected ->
                            carrera = carreras.first { it.second == selected }.first
                        }
                    )
                    Text(
                        "Seleccionada: ${carreras.firstOrNull { it.first == carrera }?.second ?: "Pendiente"}",
                        color    = Color.Gray,
                        fontSize = 12.sp
                    )

                    // ── Contraseña ────────────────────────────────────────
                    PasswordField("Contraseña",          password)  { password  = it }
                    PasswordField("Confirmar contraseña", confirmar) { confirmar = it }
                    Text(
                        "Mínimo 8 caracteres, una mayúscula, una minúscula y un número.",
                        color    = Color.Gray,
                        fontSize = 12.sp
                    )

                    // ── Mensaje de estado ─────────────────────────────────
                    if (uiState.mensaje != null) {
                        Text(
                            uiState.mensaje!!,
                            color = if (uiState.exito) GreenPrimary else ErrorRed
                        )
                    }

                    // ── Botón principal ───────────────────────────────────
                    Button(
                        onClick = {
                            registroViewModel.registrar(
                                matricula       = matricula,
                                nombres         = nombres,
                                apellidoPaterno = paterno,
                                apellidoMaterno = materno,
                                fechaNacimiento = fecha,
                                sistema         = sistema,
                                carrera         = carrera,
                                password        = password,
                                confirmarPassword = confirmar
                            )
                        },
                        enabled  = !uiState.enviando && !uiState.exito,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        if (uiState.enviando) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                color    = Color.White
                            )
                        } else {
                            Text(if (uiState.exito) "Cuenta creada ✓" else "Finalizar registro")
                        }
                    }

                    // ── Botón ir al login (solo tras éxito) ───────────────
                    if (uiState.exito) {
                        Button(
                            onClick  = {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.REGISTRO) { inclusive = true }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ir al login")
                        }
                    }
                }
            }
        }
    }
}

// ─── Componentes privados ─────────────────────────────────────────────────────

@Composable
private fun RegisterField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text(label) },
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        colors        = TextFieldDefaults.colors(
            focusedContainerColor   = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor        = Color.Black,
            unfocusedTextColor      = Color.Black,
            focusedLabelColor       = GreenPrimary,
            unfocusedLabelColor     = Color.Gray
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value         = value,
            onValueChange = {},
            readOnly      = true,
            label         = { Text(label) },
            modifier      = Modifier.fillMaxWidth(),
            trailingIcon  = { Text(if (expanded) "▲" else "▼", color = GreenPrimary) },
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor        = Color.Black,
                unfocusedTextColor      = Color.Black,
                focusedLabelColor       = GreenPrimary,
                unfocusedLabelColor     = Color.Gray
            )
        )
        // Capa invisible encima del TextField para capturar el tap
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded        = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text    = { Text(option) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value                = value,
        onValueChange        = onChange,
        label                = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon         = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar $label" else "Mostrar $label"
                )
            }
        },
        modifier             = Modifier.fillMaxWidth(),
        singleLine           = true,
        colors               = TextFieldDefaults.colors(
            focusedContainerColor   = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor        = Color.Black,
            unfocusedTextColor      = Color.Black,
            focusedLabelColor       = GreenPrimary,
            unfocusedLabelColor     = Color.Gray
        )
    )
}
