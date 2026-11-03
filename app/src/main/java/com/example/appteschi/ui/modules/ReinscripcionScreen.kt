package com.example.appteschi.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appteschi.ui.theme.BackgroundLight
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.LoginInk
import com.example.appteschi.ui.theme.SuccessContainer
import com.example.appteschi.ui.theme.TextMuted

private enum class SeccionReinscripcion(val etiqueta: String, val descripcion: String) {
    REINSCRIPCION(
        "Reinscripción",
        "La reinscripción todavía no está disponible en el SIIA para la app. Estamos preparando este apartado."
    ),
    INTERSEMESTRAL(
        "Intersemestral",
        "Los cursos intersemestrales todavía no están disponibles en el SIIA para la app. Estamos preparando este apartado."
    )
}

/**
 * Pestaña Reinscripción, con Intersemestral dentro (DEC-046). La API del SIIA
 * todavía no tiene datos de ninguno de los dos: se muestra que están en construcción.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReinscripcionScreen(navController: NavController) {
    var seccion by rememberSaveable { mutableStateOf(SeccionReinscripcion.REINSCRIPCION) }
    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Reinscripción", color = Color.White) },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val secciones = SeccionReinscripcion.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                secciones.forEachIndexed { i, opcion ->
                    SegmentedButton(
                        selected = opcion == seccion,
                        onClick = { seccion = opcion },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = secciones.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = GreenPrimary,
                            activeContentColor = Color.White,
                            inactiveContainerColor = Color.White,
                            inactiveContentColor = LoginInk
                        )
                    ) { Text(opcion.etiqueta) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TarjetaEnConstruccion(seccion.descripcion)
            Spacer(modifier = Modifier.height(90.dp)) // deja la tarjeta por encima de la barra flotante
        }
    }
}

@Composable
private fun TarjetaEnConstruccion(descripcion: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(SuccessContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Construction, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Estamos trabajando en este apartado",
                color = LoginInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(descripcion, color = TextMuted, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        }
    }
}
