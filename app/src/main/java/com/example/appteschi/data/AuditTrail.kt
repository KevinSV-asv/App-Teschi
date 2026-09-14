package com.example.appteschi.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Registro local temporal hasta conectar el endpoint institucional de auditoria. */
data class AuditEvent(
    val actor: String,
    val action: String,
    val detail: String,
    val timestamp: String,
    val location: String,
    val risk: RiskLevel
)

enum class RiskLevel(val label: String) {
    NORMAL("Normal"),
    ATENCION("Atencion"),
    CRITICO("Critico")
}

object AuditTrail {
    private val events = mutableListOf(
        AuditEvent("sistema", "Servicio iniciado", "Auditoria local preparada", now(), "Sin ubicacion", RiskLevel.NORMAL)
    )

    fun record(actor: String, action: String, detail: String, context: Context? = null) {
        val risk = when {
            action.contains("rechaz", ignoreCase = true) || action.contains("bloque", ignoreCase = true) -> RiskLevel.CRITICO
            action.contains("permiso", ignoreCase = true) || action.contains("ubicacion", ignoreCase = true) -> RiskLevel.ATENCION
            else -> RiskLevel.NORMAL
        }
        events.add(0, AuditEvent(actor, action, detail, now(), context?.lastLocationLabel() ?: "Sin ubicacion", risk))
        if (events.size > 100) events.removeLast()
    }

    fun all(): List<AuditEvent> = events.toList()

    private fun now(): String = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())

    @SuppressLint("MissingPermission")
    private fun Context.lastLocationLabel(): String {
        val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return "Sin ubicacion"
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return "GPS desactivado"
        val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: return "Sin ubicacion reciente"
        return "%.4f, %.4f".format(Locale.US, location.latitude, location.longitude)
    }
}
