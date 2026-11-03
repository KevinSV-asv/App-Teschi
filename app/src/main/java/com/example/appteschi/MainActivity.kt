package com.example.appteschi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.appteschi.data.FotoPerfilArchivos
import com.example.appteschi.data.UserSession
import com.example.appteschi.service.SesionAlumno
import com.example.appteschi.ui.dashboard.BottomNavBar
import com.example.appteschi.ui.dashboard.DashboardScreen
import com.example.appteschi.ui.dashboard.PerfilAlumnoScreen
import com.example.appteschi.ui.dashboard.TABS_DASHBOARD
import com.example.appteschi.ui.login.LoginScreen
import com.example.appteschi.ui.modules.CalificacionesScreen
import com.example.appteschi.ui.modules.KardexScreen
import com.example.appteschi.ui.modules.ReinscripcionScreen
import com.example.appteschi.ui.modules.TiraMateriasScreen
import com.example.appteschi.ui.theme.AppTeschiTheme
import com.example.appteschi.ui.theme.BackgroundLight

// ─── Actividad Principal ──────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        UserSession.init(this)
        FotoPerfilArchivos.init(this)
        setContent {
            AppTeschiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

// ─── Rutas de Navegación ──────────────────────────────────────────────────────
// Desde DEC-043 la app solo tiene las pantallas del alumno: se quitaron el panel
// de administrador, Registro, recuperar contraseña y el panel de docente.
// Reinscripción volvió como pestaña, con Intersemestral dentro (DEC-046).
object Routes {
    const val LOGIN          = "login"
    const val DASHBOARD      = "dashboard"
    const val TIRA_MATERIAS  = "tira_materias"
    const val CALIFICACIONES = "calificaciones"
    const val REINSCRIPCION  = "reinscripcion"
    const val KARDEX         = "kardex"
    const val PERFIL_ALUMNO  = "perfil_alumno"
}

// ─── Transición entre tabs — desliza en la dirección real del cambio (si el
// destino está a la derecha en TABS_DASHBOARD, entra desde la derecha; si
// está a la izquierda, desde la izquierda) y usa un fundido con escala para
// cualquier otra navegación (login, perfil). ───────────────────────────────────
private fun direccionTransicionTabs(rutaOrigen: String?, rutaDestino: String?): Int {
    val i = TABS_DASHBOARD.indexOf(rutaOrigen)
    val j = TABS_DASHBOARD.indexOf(rutaDestino)
    if (i == -1 || j == -1 || i == j) return 0
    return if (j > i) 1 else -1
}

private const val DURACION_TRANSICION_MS = 340

// ─── NavHost ──────────────────────────────────────────────────────────────────
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route
    val mostrarBarraInferior = rutaActual in TABS_DASHBOARD

    // Con el token del SIIA vigente (8 h) se entra directo a Inicio.
    val inicio = remember { if (UserSession.sesionVigente()) Routes.DASHBOARD else Routes.LOGIN }

    // Si el SIIA rechaza el token (venció o no es válido), SesionAlumno ya borró
    // la sesión local: aquí solo se le avisa y se le manda a iniciar sesión, en
    // vez de dejarlo en "error + Reintentar" sin salida.
    val contexto = LocalContext.current
    LaunchedEffect(Unit) {
        SesionAlumno.expirada.collect {
            Toast.makeText(contexto, "Tu sesión del SIIA expiró. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    // La barra flota SOBRE el contenido (no le reserva espacio fijo) — así cada
    // pantalla puede desplazarse hasta el borde real sin dejar un hueco gris
    // encima de la barra cuando la lista es más corta que el espacio reservado.
    // Cada pantalla-tab agrega su propio espaciador final para que su último
    // elemento se pueda desplazar por completo por encima de la barra.
    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        NavHost(
            navController = navController,
            startDestination = inicio,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                when (direccionTransicionTabs(initialState.destination.route, targetState.destination.route)) {
                    1 -> slideInHorizontally(tween(DURACION_TRANSICION_MS)) { it / 3 } + fadeIn(tween(DURACION_TRANSICION_MS))
                    -1 -> slideInHorizontally(tween(DURACION_TRANSICION_MS)) { -it / 3 } + fadeIn(tween(DURACION_TRANSICION_MS))
                    else -> fadeIn(tween(DURACION_TRANSICION_MS)) + scaleIn(initialScale = 0.96f, animationSpec = tween(DURACION_TRANSICION_MS))
                }
            },
            exitTransition = {
                when (direccionTransicionTabs(initialState.destination.route, targetState.destination.route)) {
                    1 -> slideOutHorizontally(tween(DURACION_TRANSICION_MS)) { -it / 3 } + fadeOut(tween(200))
                    -1 -> slideOutHorizontally(tween(DURACION_TRANSICION_MS)) { it / 3 } + fadeOut(tween(200))
                    else -> fadeOut(tween(200)) + scaleOut(targetScale = 1.03f, animationSpec = tween(200))
                }
            },
            popEnterTransition = { fadeIn(tween(DURACION_TRANSICION_MS)) + scaleIn(initialScale = 0.96f, animationSpec = tween(DURACION_TRANSICION_MS)) },
            popExitTransition = { fadeOut(tween(200)) + scaleOut(targetScale = 1.03f, animationSpec = tween(200)) }
        ) {
            // ── Autenticación (usuario + contraseña + NIP del SIIA) ──────────
            composable(Routes.LOGIN) { LoginScreen(navController) }

            // ── Tabs del alumno (barra inferior) ──────────────────────────────
            composable(Routes.DASHBOARD)      { DashboardScreen(navController) }
            composable(Routes.TIRA_MATERIAS)  { TiraMateriasScreen(navController) }
            composable(Routes.CALIFICACIONES) { CalificacionesScreen(navController) }
            composable(Routes.REINSCRIPCION)  { ReinscripcionScreen(navController) }
            composable(Routes.KARDEX)         { KardexScreen(navController) }

            composable(Routes.PERFIL_ALUMNO)  { PerfilAlumnoScreen(navController) }
        }

        if (mostrarBarraInferior) {
            BottomNavBar(
                rutaActual = rutaActual,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) { destino ->
                if (destino != rutaActual) {
                    navController.navigate(destino) {
                        popUpTo(Routes.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }
}
