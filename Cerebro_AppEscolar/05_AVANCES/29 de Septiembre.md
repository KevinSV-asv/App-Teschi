# Bitácora de Avances — 29 de Septiembre de 2026

- **Relacionado con:** [[MAPA_PROYECTO]], [[API_SIIA_Oficial]], [[CAMBIOS_DE_CODIGO]] (v0.31), [[DECISIONES_TECNICAS]] (DEC-041)

---

## Completado

- [x] **API del SIIA 1.1 analizada** y documentada en [[API_SIIA_Oficial]]: login en dos pasos con NIP de 4 dígitos, nuevo `/nip.ashx` (verificar, configurar, cambiar, recuperar por CURP), nuevo `/calificaciones/parciales.ashx` (parciales 1–3 por periodo, requiere el token final) y `tipo P` = docente.
- [x] **Login del SIIA con NIP** (DEC-041): después de la contraseña la app pide el NIP del SIIA o ayuda a crearlo; con el NIP correcto entra sin código por correo. "¿Olvidaste tu NIP?" por CURP. El `tempToken` del SIIA se queda en el servidor y su token final se descarta.
- [x] `tipo P` reconocido como docente (antes el código suponía `D`).
- [x] Pruebas: backend 27/27 con un SIIA simulado de la API 1.1; app 125/125 (7 nuevas).

---

## Pendiente

- [ ] **Desplegar `server.js`** (`deploy-desde-windows.ps1`, [[03_Manual_Tecnico_Instalacion]] §8) e instalar el APK compilado el 29/09/2026 14:15.
- [ ] Probar con la cuenta real del alumno: crear su NIP desde la app y entrar. Revisar en el log `[AUTH-SIIA][REQUIERE-NIP] … usuarioEnPaso1=` para saber si el SIIA manda los datos en el paso 1.
- [ ] Decidir las otras dos recomendaciones: calificaciones parciales del SIIA en la app y guardar el token del SIIA en memoria durante la sesión.
- [ ] El alumno compartió en el chat la contraseña de su cuenta; se le recomendó cambiarla. No se usó ni se guardó.
