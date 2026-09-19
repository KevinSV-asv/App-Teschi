package com.example.appteschi.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appteschi.Routes
import com.example.appteschi.ui.theme.GreenPrimary
import com.example.appteschi.ui.theme.TextMuted

private data class ItemNavegacion(
    val etiqueta: String,
    val icono: ImageVector,
    val ruta: String
)

/** Orden real de los tabs — se usa también para decidir la dirección de la
 *  animación al cambiar entre ellos (ver [direccionTransicion] en MainActivity). */
val TABS_DASHBOARD = listOf(
    Routes.DASHBOARD, Routes.REINSCRIPCION, Routes.TIRA_MATERIAS,
    Routes.CALIFICACIONES, Routes.INTERSEMESTRAL, Routes.KARDEX
)

private val itemsNavegacion = listOf(
    ItemNavegacion("Inicio", Icons.Default.Home, Routes.DASHBOARD),
    ItemNavegacion("Reinscrip.", Icons.Default.Edit, Routes.REINSCRIPCION),
    ItemNavegacion("Materias", Icons.AutoMirrored.Filled.List, Routes.TIRA_MATERIAS),
    ItemNavegacion("Calif.", Icons.Default.Star, Routes.CALIFICACIONES),
    ItemNavegacion("Intersem.", Icons.Default.DateRange, Routes.INTERSEMESTRAL),
    ItemNavegacion("Kardex", Icons.Default.Assessment, Routes.KARDEX)
)

/**
 * Barra de navegación inferior flotante (estilo "pill") con los 6 módulos
 * principales del alumno. Reemplaza la rejilla de tarjetas que antes vivía
 * en el Dashboard — ahora esos módulos son destinos de esta barra, siempre
 * visible mientras el alumno navega entre ellos.
 */
@Composable
fun BottomNavBar(rutaActual: String?, modifier: Modifier = Modifier, onSeleccionar: (String) -> Unit) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp)
            .height(64.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsNavegacion.forEach { item ->
                ItemBarra(
                    item = item,
                    seleccionado = rutaActual == item.ruta,
                    onClick = { onSeleccionar(item.ruta) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun ItemBarra(
    item: ItemNavegacion,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = if (seleccionado) GreenPrimary else TextMuted,
        animationSpec = tween(220),
        label = "colorItemNav"
    )
    val escalaIcono by animateFloatAsState(
        targetValue = if (seleccionado) 1.15f else 1f,
        animationSpec = tween(220),
        label = "escalaItemNav"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            item.icono,
            contentDescription = item.etiqueta,
            tint = color,
            modifier = Modifier
                .size(21.dp)
                .graphicsLayer(scaleX = escalaIcono, scaleY = escalaIcono)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.etiqueta,
            color = color,
            fontSize = 9.5.sp,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center
        )
    }
}
