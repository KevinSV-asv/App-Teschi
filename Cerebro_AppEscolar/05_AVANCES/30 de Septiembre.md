# Bitácora de Avances — 30 de Septiembre de 2026

- **Relacionado con:** [[MAPA_PROYECTO]], [[03_Calificaciones]], [[API_SIIA_Oficial]], [[CAMBIOS_DE_CODIGO]] (v0.32), [[DECISIONES_TECNICAS]] (DEC-042)

---

## Completado

- [x] **Calificaciones parciales del SIIA** (DEC-042): quien entra con usuario + NIP del SIIA ve en Calificaciones sus parciales reales (Parcial 1/2/3, promedio, profesor, grupo, estatus) con selector de periodo; si el SIIA lo bloquea por trámites, se muestra la lista de pendientes. Cuentas propias siguen viendo las de AppTeschiDB.
- [x] El token del SIIA se guarda solo en memoria del backend (8 h); al teléfono solo le llega un `sid` dentro de su sesión.
- [x] Pruebas: backend 15/15 con SIIA simulado + NIP 27/27; app 133/133.

---

## Pendiente

- [ ] **Desplegar `server.js`** e instalar el APK compilado el 30/09/2026 09:19 (incluye también el login con NIP de ayer).
- [ ] Entrar con la cuenta real del alumno, crear el NIP y revisar Calificaciones en el teléfono (no se pudo ver la pantalla: el teléfono no estaba conectado).
- [ ] Si la respuesta real del SIIA trae campos distintos a los del Swagger, ajustar `materiaParcial()` en `server.js`.
- [x] Login con NIP probado por el alumno en su teléfono: funciona.
- [ ] **BUG-024:** Calificaciones muestra "No fue posible procesar la solicitud." (mensaje del SIIA). El `User-Agent` no lo resolvió (desplegado y probado). Segundo intento: reintentar con los periodos recientes cuando el SIIA falla con "el periodo actual" (v0.33). APK ya instalado en el teléfono; falta desplegar `server.js`.
