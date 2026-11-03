package com.example.appteschi.core.config

import java.net.URLEncoder

/**
 * La app habla directo con la API oficial del SIIA (v1.2) desde el 12/10/2026
 * (DEC-043): ya no hay backend propio (AppTeschi.Api), ni URL configurable, ni
 * Tailscale. Es una URL pública por HTTPS.
 */
object ApiConfig {
    const val SIIA_BASE_URL = "https://siia.teschi.edu.mx/Teschi/api"

    /** Portal web del SIIA: lo que la API no permite, como cambiar la contraseña. */
    const val SIIA_PORTAL_URL = "https://siia.teschi.edu.mx/Teschi/"
}

/**
 * Endpoints del SIIA que usa la app. Recibe la URL base para que las pruebas
 * puedan apuntarlos a un servidor falso (MockWebServer).
 */
class SiiaEndpoints(base: String = ApiConfig.SIIA_BASE_URL) {
    private val raiz = base.trim().trimEnd('/')

    /** POST { usuario, password } → requiereNip + tempToken (10 min). */
    val login = "$raiz/login.ashx"

    /** POST { accion: verificar | configurar | recuperar | cambiar, … }. */
    val nip = "$raiz/nip.ashx"

    /** GET con Bearer; sin periodo, el SIIA usa el actual. */
    fun calificacionesParciales(periodo: String?): String = conPeriodo("$raiz/calificaciones/parciales.ashx", periodo)

    /** GET con Bearer: 1ra y 2da vuelta y calificación final; sin periodo, el SIIA usa el actual. */
    fun calificacionesFinales(periodo: String?): String = conPeriodo("$raiz/calificaciones/finales.ashx", periodo)

    /** GET con Bearer: historial académico completo del alumno del token. */
    val kardex = "$raiz/calificaciones/kardex.ashx"

    /**
     * GET con Bearer: PDF oficial del kardex dentro de un JSON. El SIIA también
     * acepta `?token=` en la URL, pero aquí el token siempre va en el encabezado.
     */
    val kardexPdf = "$raiz/calificaciones/kardex-pdf.ashx?base64=1"

    private fun conPeriodo(url: String, periodo: String?): String {
        val p = periodo?.trim().orEmpty()
        return url + if (p.isEmpty()) "" else "?periodo=" + URLEncoder.encode(p, "UTF-8")
    }
}
