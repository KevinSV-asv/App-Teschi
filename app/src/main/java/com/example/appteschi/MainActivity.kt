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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.appteschi.data.AppContextHolder
import com.example.appteschi.data.UserSession
import com.example.appteschi.service.SesionAlumno
import com.example.appteschi.ui.dashboard.AdminAdministradoresScreen
import com.example.appteschi.ui.dashboard.AdminAlumnosScreen
import com.example.appteschi.ui.dashboard.AdminAuditoriaScreen
import com.example.appteschi.ui.dashboard.AdminCalificacionesScreen
import com.example.appteschi.ui.dashboard.AdminDashboardScreen
import com.example.appteschi.ui.dashboard.AdminEstadisticasScreen
import com.example.appteschi.ui.dashboard.AdminHorariosScreen
import com.example.appteschi.ui.dashboard.AdminProfileScreen
import com.example.appteschi.ui.dashboard.AdminReinscripcionesScreen
import com.example.appteschi.ui.dashboard.BottomNavBar
import com.example.appteschi.ui.dashboard.DashboardScreen
import com.example.appteschi.ui.dashboard.PerfilAlumnoScreen
import com.example.appteschi.ui.dashboard.TABS_DASHBOARD
import com.example.appteschi.ui.login.LoginScreen
import com.example.appteschi.ui.login.RecuperarPasswordScreen
import com.example.appteschi.ui.login.RegistroScreen
import com.example.appteschi.ui.modules.CalificacionesScreen
import com.example.appteschi.ui.modules.KardexScreen
import com.example.appteschi.ui.modules.ReinscripcionScreen
import com.example.appteschi.ui.modules.TiraMateriasScreen
import com.example.appteschi.ui.theme.AppTeschiTheme
import com.example.appteschi.ui.theme.BackgroundLight

// ─── Actividad Principal ──────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContextHolder.init(this)
        UserSession.init(this)
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
object Routes {
    const val LOGIN            = "login"
    const val DASHBOARD        = "dashboard"
    const val ADMIN_DASHBOARD      = "admin_dashboard"
    const val ADMIN_PROFILE        = "admin_profile/{matricula}"
    const val ADMIN_ALUMNOS        = "admin_alumnos"
    const val ADMIN_CALIFICACIONES = "admin_calificaciones"
    const val ADMIN_AUDITORIA      = "admin_auditoria"
    const val ADMIN_ESTADISTICAS   = "admin_estadisticas"
    const val ADMIN_ADMINISTRADORES = "admin_administradores"
    const val ADMIN_REINSCRIPCIONES = "admin_reinscripciones"
    const val ADMIN_HORARIOS       = "admin_horarios"
    const val REINSCRIPCION    = "reinscripcion"
    const val TIRA_MATERIAS    = "tira_materias"
    const val CALIFICACIONES   = "calificaciones"
    const val INTERSEMESTRAL   = "intersemestral"
    const val KARDEX           = "kardex"
    const val RECUPERAR_PASS   = "recuperar_pass"
    const val REGISTRO         = "registro"
    const val PERFIL_ALUMNO    = "perfil_alumno"

    /** Helper para navegar al perfil de un usuario específico */
    fun adminProfile(matricula: String) = "admin_profile/$matricula"
}

// ─── Transición entre tabs — desliza en la dirección real del cambio (si el
// destino está a la derecha en TABS_DASHBOARD, entra desde la derecha; si
// está a la izquierda, desde la izquierda) y usa un fundido con escala para
// cualquier otra navegación (login, admin, perfil, etc.). ─────────────────────
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

    // Si el backend rechaza el token del alumno (venció o no es válido),
    // SesionAlumno ya borró la sesión local: aquí solo se le avisa y se le
    // manda a iniciar sesión, en vez de dejarlo en "error + Reintentar" sin salida.
    val contexto = LocalContext.current
    LaunchedEffect(Unit) {
        SesionAlumno.expirada.collect {
            Toast.makeText(contexto, "Tu sesión expiró. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
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
            startDestination = Routes.LOGIN,
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

            // ── Autenticación ────────────────────────────────────────────────
            composable(Routes.LOGIN) { LoginScreen(navController) }

            // ── Dashboard alumno (tab "Inicio") ────────────────────────────
            composable(Routes.DASHBOARD) { DashboardScreen(navController) }

            // ── Panel de administrador ───────────────────────────────────────
            composable(Routes.ADMIN_DASHBOARD) {
                AdminDashboardScreen(navController)
            }
            composable(
                route = Routes.ADMIN_PROFILE,
                arguments = listOf(
                    navArgument("matricula") { type = NavType.StringType }
                )
            ) { backStack ->
                val matricula = backStack.arguments?.getString("matricula") ?: ""
                AdminProfileScreen(navController = navController, matricula = matricula)
            }
            composable(Routes.ADMIN_ALUMNOS)        { AdminAlumnosScreen(navController) }
            composable(Routes.ADMIN_CALIFICACIONES) { AdminCalificacionesScreen(navController) }
            composable(Routes.ADMIN_AUDITORIA)      { AdminAuditoriaScreen(navController) }
            composable(Routes.ADMIN_ESTADISTICAS)   { AdminEstadisticasScreen(navController) }
            composable(Routes.ADMIN_ADMINISTRADORES) { AdminAdministradoresScreen(navController) }
            composable(Routes.ADMIN_REINSCRIPCIONES) { AdminReinscripcionesScreen(navController) }
            composable(Routes.ADMIN_HORARIOS) { AdminHorariosScreen(navController) }

            // ── Módulos del alumno (tabs de la barra inferior) ───────────────
            composable(Routes.REINSCRIPCION)  { ReinscripcionScreen(navController) }
            composable(Routes.TIRA_MATERIAS)  { TiraMateriasScreen(navController) }
            composable(Routes.CALIFICACIONES) { CalificacionesScreen(navController) }
            composable(Routes.INTERSEMESTRAL) { ModuleDetailScreen("Intersemestral", navController) }
            composable(Routes.KARDEX)         { KardexScreen(navController) }
            composable(Routes.RECUPERAR_PASS) { RecuperarPasswordScreen(navController) }
            composable(Routes.PERFIL_ALUMNO)  { PerfilAlumnoScreen(navController) }

            // ── Registro de nuevo usuario (formulario completo) ───────────────
            composable(Routes.REGISTRO) { RegistroScreen(navController) }
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

// ─── Pantalla genérica de módulo (placeholder hasta implementar cada uno) ──────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(title: String, navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Módulo de $title en desarrollo",
                fontSize = 18.sp,
                color    = Color.Gray
            )
        }
    }
}
