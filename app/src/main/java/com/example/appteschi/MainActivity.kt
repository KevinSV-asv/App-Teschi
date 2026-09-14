package com.example.appteschi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.appteschi.data.AppContextHolder
import com.example.appteschi.ui.dashboard.AdminAdministradoresScreen
import com.example.appteschi.ui.dashboard.AdminAlumnosScreen
import com.example.appteschi.ui.dashboard.AdminAuditoriaScreen
import com.example.appteschi.ui.dashboard.AdminCalificacionesScreen
import com.example.appteschi.ui.dashboard.AdminDashboardScreen
import com.example.appteschi.ui.dashboard.AdminEstadisticasScreen
import com.example.appteschi.ui.dashboard.AdminProfileScreen
import com.example.appteschi.ui.dashboard.DashboardScreen
import com.example.appteschi.ui.login.LoginScreen
import com.example.appteschi.ui.login.RegistroScreen
import com.example.appteschi.ui.modules.CalificacionesScreen
import com.example.appteschi.ui.modules.KardexScreen
import com.example.appteschi.ui.modules.ReinscripcionScreen
import com.example.appteschi.ui.modules.TiraMateriasScreen
import com.example.appteschi.ui.theme.AppTeschiTheme

// ─── Actividad Principal ──────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContextHolder.init(this)
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
    const val REINSCRIPCION    = "reinscripcion"
    const val TIRA_MATERIAS    = "tira_materias"
    const val CALIFICACIONES   = "calificaciones"
    const val INTERSEMESTRAL   = "intersemestral"
    const val KARDEX           = "kardex"
    const val RECUPERAR_PASS   = "recuperar_pass"
    const val REGISTRO         = "registro"

    /** Helper para navegar al perfil de un usuario específico */
    fun adminProfile(matricula: String) = "admin_profile/$matricula"
}

// ─── NavHost ──────────────────────────────────────────────────────────────────
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        // ── Autenticación ────────────────────────────────────────────────
        composable(Routes.LOGIN) { LoginScreen(navController) }

        // ── Dashboard alumno ─────────────────────────────────────────────
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

        // ── Módulos del alumno ───────────────────────────────────────────
        composable(Routes.REINSCRIPCION)  { ReinscripcionScreen(navController) }
        composable(Routes.TIRA_MATERIAS)  { TiraMateriasScreen(navController) }
        composable(Routes.CALIFICACIONES) { CalificacionesScreen(navController) }
        composable(Routes.INTERSEMESTRAL) { ModuleDetailScreen("Intersemestral", navController) }
        composable(Routes.KARDEX)         { KardexScreen(navController) }
        composable(Routes.RECUPERAR_PASS) { ModuleDetailScreen("Recuperar Contraseña", navController) }

        // ── Registro de nuevo usuario (formulario completo) ───────────────
        composable(Routes.REGISTRO) { RegistroScreen(navController) }
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
