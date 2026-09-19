package com.example.appteschi.ui.modules

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.data.EstatusMateria
import com.example.appteschi.data.MateriaHistorial
import com.example.appteschi.data.UserSession
import com.example.appteschi.pdf.PdfKardexGenerator
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.GreenSecondary
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.viewmodel.KardexUiState
import com.example.appteschi.viewmodel.KardexViewModel
import com.example.appteschi.ui.theme.ErrorRed
import com.example.appteschi.ui.theme.SuccessContainer
import com.example.appteschi.ui.theme.OnSuccessContainer
import com.example.appteschi.ui.theme.ErrorContainer
import com.example.appteschi.ui.theme.OnErrorContainer
import com.example.appteschi.ui.theme.NeutralContainer
import com.example.appteschi.ui.theme.OnNeutralContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KardexScreen(
    navController: NavController,
    viewModel: KardexViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kardex", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        enabled = !uiState.cargando,
                        onClick = {
                            try {
                                val uri = PdfKardexGenerator.generar(
                                    context = contexto,
                                    uiState = uiState,
                                    matricula = UserSession.matricula,
                                    nombre = UserSession.nombreMostrar
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    contexto.startActivity(intent)
                                } catch (sinVisor: ActivityNotFoundException) {
                                    Toast.makeText(
                                        contexto,
                                        "El PDF se guardó en Descargas, pero no hay una app para abrirlo",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (error: Exception) {
                                Toast.makeText(
                                    contexto,
                                    "No se pudo generar el PDF: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Download, "Descargar Kardex en PDF", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        if (uiState.cargando) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Consultando historial académico...", color = LoginInk)
            }
            return@Scaffold
        }

        if (uiState.error != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(uiState.error!!, color = ErrorRed, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = viewModel::reintentar,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) { Text("Reintentar") }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(uiState.carrera, color = LoginInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Alumno: ${UserSession.nombreMostrar}", color = Color.Gray, fontSize = 13.sp)
                Text(
                    "Ingreso: ${uiState.periodoIngreso}  ·  Último período: ${uiState.ultimoPeriodo}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { ResumenCard(uiState) }
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Materias por semestre", color = LoginInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            val porSemestre = uiState.materias.groupBy { it.semestre }.toSortedMap()
            porSemestre.forEach { (semestre, materiasDelSemestre) ->
                item {
                    Text(
                        "Semestre $semestre",
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(materiasDelSemestre) { MateriaRow(it) }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) } // deja el último elemento por encima de la barra flotante
        }
    }
}

@Composable
private fun ResumenCard(uiState: KardexUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Promedio global", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        "%.2f".format(uiState.promedioGlobal),
                        color = GreenPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Créditos cubiertos", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        "${uiState.totalCreditosCursados} / ${uiState.totalCreditosCarrera}",
                        color = LoginInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text("%.1f%%".format(uiState.porcentajeCubierto), color = GreenSecondary, fontSize = 12.sp)
                }
            }
            LinearProgressIndicator(
                progress = { (uiState.porcentajeCubierto / 100).toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = GreenPrimary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContadorChip("Aprobadas", uiState.materiasAprobadas, SuccessContainer, OnSuccessContainer, Modifier.weight(1f))
                ContadorChip("Por cursar", uiState.materiasPorAprobar, NeutralContainer, OnNeutralContainer, Modifier.weight(1f))
                ContadorChip("No aprobadas", uiState.materiasReprobadas, ErrorContainer, OnErrorContainer, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ContadorChip(etiqueta: String, valor: Int, fondo: Color, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = fondo)) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valor.toString(), color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(etiqueta, color = color, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MateriaRow(materia: MateriaHistorial) {
    val (colorFondo, colorTexto) = when (materia.estatus) {
        EstatusMateria.APROBADA -> SuccessContainer to OnSuccessContainer
        EstatusMateria.POR_CURSAR -> NeutralContainer to OnNeutralContainer
        EstatusMateria.NO_APROBADA -> ErrorContainer to OnErrorContainer
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(materia.nombre, color = LoginInk, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("${materia.creditos} créditos", color = Color.Gray, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (materia.calificacion != null) {
                    Text("%.1f".format(materia.calificacion), color = colorTexto, fontWeight = FontWeight.Bold)
                }
                Card(colors = CardDefaults.cardColors(containerColor = colorFondo)) {
                    Text(
                        materia.estatus.etiqueta,
                        color = colorTexto,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
