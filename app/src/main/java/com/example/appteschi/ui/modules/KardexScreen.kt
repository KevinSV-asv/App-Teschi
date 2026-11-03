package com.example.appteschi.ui.modules

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appteschi.R
import com.example.appteschi.service.DatosKardex
import com.example.appteschi.service.EstatusKardex
import com.example.appteschi.service.Kardex
import com.example.appteschi.service.MateriaKardex
import com.example.appteschi.service.SemestreKardex
import com.example.appteschi.ui.theme.BackgroundLight
import com.example.appteschi.ui.theme.ErrorContainer
import com.example.appteschi.ui.theme.ErrorRed
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.GreenSecondary
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.NeutralContainer
import com.example.appteschi.ui.theme.OnErrorContainer
import com.example.appteschi.ui.theme.OnNeutralContainer
import com.example.appteschi.ui.theme.OnSuccessContainer
import com.example.appteschi.ui.theme.OnWarningContainer
import com.example.appteschi.ui.theme.SuccessContainer
import com.example.appteschi.ui.theme.TextMuted
import com.example.appteschi.ui.theme.WarningContainer
import com.example.appteschi.viewmodel.KardexViewModel

/**
 * Kardex del SIIA (DEC-044), con el diseño móvil que eligió el alumno
 * (09_ANALISIS_Y_DISENO/08_Kardex_Propuesta_Stitch.html) y los mismos datos del
 * historial académico oficial (DEC-045). Sin el sello de "documento oficial" del
 * diseño: la app no certifica nada; lo oficial es el PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KardexScreen(navController: NavController, viewModel: KardexViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    val avisos = remember { SnackbarHostState() }
    var pdfGuardado by rememberSaveable { mutableStateOf<Uri?>(null) }

    // El alumno elige dónde guardar (no hace falta pedir permiso de almacenamiento).
    val selectorDestino = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri == null) {
            viewModel.cancelarGuardado()
        } else {
            pdfGuardado = uri
            viewModel.guardarPdf(runCatching { contexto.contentResolver.openOutputStream(uri) }.getOrNull())
        }
    }

    LaunchedEffect(uiState.pedirDestinoPdf) {
        val nombre = uiState.pedirDestinoPdf ?: return@LaunchedEffect
        viewModel.destinoPedido()
        try {
            selectorDestino.launch(nombre)
        } catch (_: ActivityNotFoundException) {
            viewModel.cancelarGuardado("Tu teléfono no tiene una app para guardar archivos.")
        }
    }

    // El estado se limpia hasta que se cierra el aviso: limpiarlo antes cancelaría este efecto y el aviso con él.
    LaunchedEffect(uiState.pdfGuardado, uiState.errorPdf) {
        val error = uiState.errorPdf
        if (uiState.pdfGuardado) {
            val r = avisos.showSnackbar("Kardex guardado", actionLabel = "Abrir", duration = SnackbarDuration.Long)
            if (r == SnackbarResult.ActionPerformed) pdfGuardado?.let { abrirPdf(contexto, it) }
            viewModel.avisoPdfMostrado()
        } else if (error != null) {
            avisos.showSnackbar(error, duration = SnackbarDuration.Long)
            viewModel.avisoPdfMostrado()
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        // Por encima de la barra flotante.
        snackbarHost = { SnackbarHost(avisos, modifier = Modifier.padding(bottom = 80.dp)) },
        topBar = {
            TopAppBar(
                title = { Text("Kardex", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    if (uiState.kardex?.bloqueado == false) {
                        BotonDescargarPdf(uiState.descargandoPdf, viewModel::descargarPdf)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        val kardex = uiState.kardex
        when {
            uiState.cargando -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Consultando tu kardex en el SIIA...", color = LoginInk)
            }
            uiState.error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(uiState.error!!, color = ErrorRed, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = viewModel::reintentar,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) { Text("Reintentar") }
            }
            kardex != null -> ContenidoKardex(
                kardex = kardex,
                semestreFiltro = uiState.semestreFiltro,
                modifier = Modifier.fillMaxSize().padding(padding),
                onFiltrar = viewModel::filtrarSemestre
            )
        }
    }
}

private fun abrirPdf(contexto: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        contexto.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(contexto, "No tienes una app para abrir archivos PDF.", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun BotonDescargarPdf(descargando: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !descargando,
        shape = RoundedCornerShape(50),
        color = Color.White,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (descargando) {
                CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Download, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (descargando) "Preparando…" else "Descargar PDF",
                color = GreenPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ContenidoKardex(
    kardex: Kardex,
    semestreFiltro: Int?,
    modifier: Modifier,
    onFiltrar: (Int?) -> Unit
) {
    val visibles = kardex.semestres.filter { semestreFiltro == null || it.numero == semestreFiltro }
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        if (kardex.bloqueado) {
            item { TarjetaBloqueo(kardex.mensajeBloqueo, kardex.pendientes) }
        } else {
            item { EncabezadoInstitucional(kardex.fechaElaboracion) }
            kardex.datos?.let { datos ->
                item { TarjetaAlumno(datos) }
                item { FilaIndicadores(datos) }
                item { FilaContadores(datos) }
                item { TarjetaResumen(datos) }
            }
            if (kardex.semestres.any { it.promedioNumero != null }) {
                item { GraficaSemestres(kardex.semestres, semestreFiltro, onFiltrar) }
            }
            if (kardex.semestres.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Text(
                            "El SIIA no tiene materias registradas en tu kardex.",
                            color = TextMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                item { FiltroSemestres(kardex.semestres, semestreFiltro, onFiltrar) }
            }
            visibles.forEach { semestre ->
                item(key = "semestre-${semestre.numero}") { EncabezadoSemestre(semestre) }
                items(semestre.materias) { TarjetaMateria(it) }
            }
            if (kardex.simbologia.isNotEmpty()) {
                item { TarjetaSimbologia(kardex.simbologia) }
            }
            item {
                Text(
                    "Esta vista es informativa. El documento oficial es el PDF.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item { Spacer(modifier = Modifier.height(90.dp)) } // deja el último elemento por encima de la barra flotante
    }
}

@Composable
private fun TarjetaBloqueo(mensaje: String, pendientes: List<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = WarningContainer)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Kardex bloqueado en el SIIA", color = OnWarningContainer, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(mensaje, color = LoginInk, fontSize = 13.sp)
            pendientes.forEach { pendiente ->
                Text("• $pendiente", color = LoginInk, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Complétalos en el portal del SIIA para ver tu kardex.", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

/** El mismo encabezado del historial académico oficial. */
@Composable
private fun EncabezadoInstitucional(fecha: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // El PNG es cuadrado con márgenes transparentes: se recorta a la franja del logo.
                Image(
                    painter = painterResource(R.drawable.logo_gobierno_edomex),
                    contentDescription = "Gobierno del Estado de México",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.height(30.dp).aspectRatio(3.35f)
                )
                Image(
                    painter = painterResource(R.drawable.logo_teschi_encabezado),
                    contentDescription = "TESCHI",
                    modifier = Modifier.height(42.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "TECNOLÓGICO DE ESTUDIOS SUPERIORES DE CHIMALHUACÁN",
                color = LoginInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Organismo Público Descentralizado del Gobierno del Estado de México",
                color = TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("DIRECCIÓN ACADÉMICA", color = LoginInk, fontSize = 11.sp, textAlign = TextAlign.Center)
            Text("DEPARTAMENTO DE CONTROL ESCOLAR", color = LoginInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("HISTORIAL ACADÉMICO", color = GreenPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (fecha.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Fecha de elaboración: $fecha", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun TarjetaAlumno(datos: DatosKardex) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(datos.nombre, color = LoginInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (datos.matricula.isNotEmpty()) {
                        Text("Matrícula: ${datos.matricula}", color = TextMuted, fontSize = 12.sp)
                    }
                    if (datos.curp.isNotEmpty()) {
                        Text("CURP: ${datos.curp}", color = TextMuted, fontSize = 12.sp)
                    }
                }
                if (datos.situacion.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Etiqueta(datos.situacion, SuccessContainer, OnSuccessContainer)
                }
            }
            if (datos.carrera.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundLight)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(datos.carrera, color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            listOf(
                "Especialidad" to datos.especialidad,
                "Plan de estudios" to datos.planEstudios,
                "Período de ingreso" to datos.periodoIngreso,
                "Último período" to datos.ultimoPeriodo,
                "Grupo" to datos.grupo
            ).filter { it.second.isNotEmpty() }.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    fila.forEach { (etiqueta, valor) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(etiqueta, color = TextMuted, fontSize = 11.sp)
                            Text(valor, color = LoginInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FilaIndicadores(datos: DatosKardex) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Promedio global", color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = GreenSecondary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(datos.promedioGlobal, color = GreenPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                if (datos.promedioLetra.isNotEmpty()) {
                    Text(datos.promedioLetra, color = TextMuted, fontSize = 11.sp)
                }
            }
        }
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Créditos", color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(datos.porcentaje, color = GreenSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                        CircularProgressIndicator(
                            progress = { datos.avance ?: 0f },
                            modifier = Modifier.fillMaxSize(),
                            color = GreenSecondary,
                            trackColor = NeutralContainer,
                            strokeWidth = 5.dp
                        )
                        Icon(Icons.Default.School, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(datos.creditosCursados, color = LoginInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("de ${datos.creditosCarrera}", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaContadores(datos: DatosKardex) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Contador(Icons.Default.CheckCircle, datos.aprobadas, "Aprobadas", SuccessContainer, OnSuccessContainer, Modifier.weight(1f))
        Contador(Icons.Default.Cancel, datos.reprobadas, "Reprobadas", ErrorContainer, OnErrorContainer, Modifier.weight(1f))
        Contador(Icons.Default.Pending, datos.porAprobar, "Por aprobar", WarningContainer, OnWarningContainer, Modifier.weight(1f))
        Contador(Icons.Default.HourglassEmpty, datos.porCursar, "Por cursar", NeutralContainer, OnNeutralContainer, Modifier.weight(1f))
    }
}

/** Lo que el reporte oficial da y no está ya en las tarjetas de arriba. */
@Composable
private fun TarjetaResumen(datos: DatosKardex) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FilaResumen("Total de materias", datos.totalMaterias.toString())
            FilaResumen("Créditos por cursar", datos.creditosPorCursar)
            FilaResumen("Promedio del semestre anterior", datos.promedioSemestreAnterior)
            if (datos.parcial.isNotEmpty()) FilaResumen("Parcial", datos.parcial)
            if (datos.descuento.isNotEmpty()) FilaResumen("Descuento", datos.descuento)
        }
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, color = TextMuted, fontSize = 13.sp)
        Text(valor, color = LoginInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Contador(icono: ImageVector, valor: Int, etiqueta: String, fondo: Color, texto: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(fondo)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = texto, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(valor.toString(), color = texto, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(etiqueta, color = texto, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Barras de 0 a 100 (la misma escala de las calificaciones). Tocar una barra filtra la lista por ese semestre. */
@Composable
private fun GraficaSemestres(semestres: List<SemestreKardex>, filtro: Int?, onFiltrar: (Int?) -> Unit) {
    val alturaMaxima = 84.dp
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Promedio por semestre", color = LoginInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                semestres.filter { it.promedioNumero != null }.forEach { semestre ->
                    val elegido = filtro == null || filtro == semestre.numero
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClickLabel = "Ver ${semestre.nombre}") {
                                onFiltrar(if (filtro == semestre.numero) null else semestre.numero)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(semestre.promedio, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(alturaMaxima * (semestre.promedioNumero!!.toFloat() / 100f).coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (elegido) GreenPrimary else GreenPrimary.copy(alpha = 0.3f))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (semestre.numero > 0) "S${semestre.numero}" else "—",
                            color = LoginInk,
                            fontSize = 10.sp,
                            fontWeight = if (filtro == semestre.numero) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltroSemestres(semestres: List<SemestreKardex>, filtro: Int?, onFiltrar: (Int?) -> Unit) {
    val colores = FilterChipDefaults.filterChipColors(
        selectedContainerColor = GreenPrimary,
        selectedLabelColor = Color.White
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(selected = filtro == null, onClick = { onFiltrar(null) }, label = { Text("Todos") }, colors = colores)
        }
        items(semestres) { semestre ->
            FilterChip(
                selected = filtro == semestre.numero,
                onClick = { onFiltrar(semestre.numero) },
                label = { Text(semestre.nombre) },
                colors = colores
            )
        }
    }
}

@Composable
private fun EncabezadoSemestre(semestre: SemestreKardex) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(GreenSecondary))
        Spacer(modifier = Modifier.width(8.dp))
        Text(semestre.nombre, color = LoginInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        val total = semestre.materias.size
        Text(
            " · $total ${if (total == 1) "materia" else "materias"}",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        if (semestre.promedio != "—") {
            Etiqueta("Promedio ${semestre.promedio}", NeutralContainer, GreenPrimary)
        }
    }
}

@Composable
private fun TarjetaMateria(materia: MateriaKardex) {
    val (fondo, texto) = when (materia.estatus) {
        EstatusKardex.APROBADA -> SuccessContainer to OnSuccessContainer
        EstatusKardex.NO_APROBADA -> ErrorContainer to OnErrorContainer
        EstatusKardex.POR_CURSAR -> NeutralContainer to OnNeutralContainer
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(materia.nombre, color = LoginInk, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    val detalle = listOfNotNull(
                        materia.numero?.let { "No. $it" },
                        materia.clave.takeIf { it.isNotEmpty() },
                        materia.creditos.takeIf { it != "—" }?.let { "$it créditos" }
                    ).joinToString(" · ")
                    if (detalle.isNotEmpty()) Text(detalle, color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(materia.calificacion, color = texto, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Etiqueta("${materia.estatus.clave} · ${materia.estatus.etiqueta}", fondo, texto)
                }
            }
            materia.oportunidades.forEach { oportunidad ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${oportunidad.tipo}: ${oportunidad.calificacion}", color = OnNeutralContainer, fontSize = 11.sp)
                    if (oportunidad.periodo.isNotEmpty()) {
                        Text("Período ${oportunidad.periodo}", color = OnNeutralContainer, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSimbologia(simbologia: List<Pair<String, String>>) {
    Card(colors = CardDefaults.cardColors(containerColor = NeutralContainer)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Simbología", color = LoginInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            simbologia.forEach { (clave, significado) ->
                Text("$clave: $significado", color = OnNeutralContainer, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, fondo: Color, colorTexto: Color) {
    Text(
        texto,
        color = colorTexto,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
