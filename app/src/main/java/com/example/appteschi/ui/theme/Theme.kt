package com.example.appteschi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ─── Esquema Claro Institucional (único esquema — no hay modo oscuro) ─────────
private val InstitutionalColorScheme = lightColorScheme(
    primary            = GreenPrimary,
    onPrimary          = OnPrimaryWhite,
    primaryContainer   = GreenTertiary,
    onPrimaryContainer = OnPrimaryWhite,
    secondary          = GreenSecondary,
    onSecondary        = OnPrimaryWhite,
    tertiary           = GreenTertiary,
    onTertiary         = OnPrimaryWhite,
    // ── Fondos ── siempre verde claro institucional, nunca negro ───────────
    background         = BackgroundLight,   // #F4F7F4
    onBackground       = LoginInk,          // #1A2E22
    surface            = SurfaceWhite,      // #FFFFFF
    onSurface          = LoginInk,
    surfaceVariant     = BackgroundLight,
    onSurfaceVariant   = LoginInk,
    // ── Error ──────────────────────────────────────────────────────────────
    error              = ErrorRed,
    onError            = OnPrimaryWhite,
    // ── Contenedores ───────────────────────────────────────────────────────
    errorContainer     = ErrorRed,
    onErrorContainer   = OnPrimaryWhite,
)

/**
 * Tema institucional AppTESCHI.
 *
 * La identidad visual es fija: siempre fondo #F4F7F4, superficies blancas
 * y acentos verdes TESCHI — independientemente de si el sistema está en
 * modo oscuro. Esto elimina el fondo negro que aparecía en algunos
 * Composables cuando el celular tenía activado el tema oscuro del sistema.
 */
@Composable
fun AppTeschiTheme(
    @Suppress("UNUSED_PARAMETER")
    darkTheme: Boolean = isSystemInDarkTheme(), // firma conservada por compatibilidad
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = InstitutionalColorScheme, // siempre claro, ignoramos darkTheme
        typography  = Typography,
        content     = content
    )
}
