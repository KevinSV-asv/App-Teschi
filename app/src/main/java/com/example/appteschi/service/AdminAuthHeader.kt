package com.example.appteschi.service

import com.example.appteschi.data.UserSession
import okhttp3.Request

/**
 * Agrega el token de sesión del administrador (ver DEC-020) como
 * `Authorization: Bearer <token>`. Reemplaza la x-api-key estática y los
 * headers x-actor-matricula/x-actor-nombre que antes declaraba el propio
 * cliente sin que nada los verificara — ahora el backend saca la identidad
 * real (quién es, qué rol tiene) del token firmado, no de lo que la app diga.
 */
fun Request.Builder.conSesionAdmin(): Request.Builder =
    addHeader("Authorization", "Bearer ${UserSession.adminToken.orEmpty()}")
