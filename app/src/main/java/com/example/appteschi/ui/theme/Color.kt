package com.example.appteschi.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Paleta Institucional TESCHI ─────────────────────────────────────────────
val GreenPrimary     = Color(0xFF1E5631) // Botones principales, TopAppBar
val GreenSecondary   = Color(0xFF4C9A2A) // Pestañas activas, acentos
val GreenTertiary    = Color(0xFF2E7D32) // Variante para estados hover/focus
val LoginGreenBright = Color(0xFF4CAF50) // Sección inferior del login (referencia web TESCHI)
val LoginGreenDark   = Color(0xFF388E3C) // Caja logo TESChi
val LoginFieldBg     = Color(0xFFF0F2F0) // Fondo de campos de texto en login
val TextMuted        = Color(0xFF757575) // Subtítulos y placeholders
val BackgroundLight  = Color(0xFFF4F7F4) // Fondo general — nunca negro. Única superficie de fondo de la app: ningún Scaffold debe declarar su propio gris/blanco ad-hoc.
val SurfaceWhite     = Color(0xFFFFFFFF) // Superficie de Cards
val DarkSurface      = Color(0xFF0C2B14) // Texto sobre fondo claro (no es color de fondo)
val OnPrimaryWhite   = Color(0xFFFFFFFF) // Texto/iconos sobre GreenPrimary
val ErrorRed         = Color(0xFFB3261E) // Estados de error — mismo valor que ya se usaba repetido (~40 veces) en cada pantalla
val LoginInk         = Color(0xFF1A2E22) // Texto principal oscuro
val LoginAccent      = Color(0xFFC6F27A) // Acento lima
val LoginCanvas      = Color(0xFFF4F7F4) // Lienzo de pantallas
val LoginSurface     = Color(0xFFFFFFFF) // Superficie de formularios

// ─── Colores de estado (contenedor + texto/ícono) ────────────────────────────
// Mismo par de tonos que ya se repetía copiado en Kardex/Calificaciones/
// Reinscripción/AdminProfile/AdminCalificaciones — ahora es un solo lugar.
val SuccessContainer   = Color(0xFFE8F5E9) // Aprobada / autorizada / confirmada
val OnSuccessContainer = GreenPrimary
val WarningContainer   = Color(0xFFFFF3E0) // Pendiente / por cursar / en revisión
val OnWarningContainer = Color(0xFFB37B00)
val ErrorContainer     = Color(0xFFFFEBEE) // No aprobada / rechazada / bloqueada
val OnErrorContainer   = ErrorRed
val NeutralContainer   = Color(0xFFF0F0F0) // Sin estatus / informativo neutro
val OnNeutralContainer = Color(0xFF616161)

// ─── Esquema oscuro ───────────────────────────────────────────────────────────
// La app fuerza siempre el tema claro; estos valores son solo por completitud de API.
val GreenPrimaryDark   = Color(0xFF4CAF50)
val GreenSecondaryDark = Color(0xFF81C784)
val BackgroundDark     = Color(0xFFF4F7F4)  // Mismo fondo claro — no hay modo oscuro
