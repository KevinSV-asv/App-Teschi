# Bitácora de Avances — 28 de Septiembre de 2026

- **Relacionado con:** [[MAPA_PROYECTO]], [[CAMBIOS_DE_CODIGO]] (v0.28 → v0.30), [[DECISIONES_TECNICAS]] (DEC-036, DEC-039, DEC-040), [[BUGS_Y_ERRORES]] (BUG-022, BUG-023)

---

## Completado

- [x] **Logo oficial de la app:** ícono del lanzador (adaptativo, con variante monocromática), pantalla de arranque con `core-splashscreen`, pie del login y cabecera de Inicio. Vistos en el teléfono: arranque, ícono y login.
- [x] **Kardex:** la descarga pasa de un ícono suelto a un botón blanco "Descargar PDF"; se deshabilita si el Kardex muestra un error.
- [x] **Login con cualquier alumno del SIIA** (DEC-040): si el SIIA lo valida y no está en AppTESCHI, entra con sus datos del SIIA sin que se cree nada en la base. Antes se probó el alta automática (DEC-039) y se revirtió porque el alumno no quiere registros automáticos; se le preguntaron las opciones y eligió "entra sin crear registro".
- [x] **BUG-022:** alumno registrado sin correo no podía pedir el código por el SIIA. Corregido.
- [x] **BUG-023:** la fecha de compilación del login quedaba congelada por la caché de configuración de Gradle. Corregido y comprobado en el teléfono.
- [x] Pruebas: backend local con SIIA simulado y correo interceptado 26/26; app 118/118.

---

## Pendiente

- [ ] **Desplegar `server.js` en el servidor** (DEC-040, BUG-022, y también `acd8ffa` y el `server.js` de v0.27). Requiere la contraseña de `sudo` del dueño: `deploy-desde-windows.ps1` ([[03_Manual_Tecnico_Instalacion]] §8). Después, probar con el alumno real no registrado.
- [ ] Ver en el teléfono Inicio y el botón del Kardex con una sesión iniciada.
- [ ] Conexión sin Tailscale: se recomendó Cloudflare Tunnel con dominio propio y retirar el registro público antes de abrir la API a internet; falta la decisión del alumno.
