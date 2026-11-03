# Bitácora de Avances — 24 al 27 de Septiembre de 2026

- **Relacionado con:** [[MAPA_PROYECTO]], [[09_Login_SIIA_por_Tipo]], [[API_SIIA_Oficial]]
- **Decisiones:** DEC-035, DEC-036, DEC-037 · **Bugs:** BUG-015 → BUG-020

---

## Completado

- [x] **Login real con la API oficial del SIIA** (DEC-035): `/api/auth/siia` llama a `login.ashx` desde el servidor. Antes comparaba contra una columna local y rechazaba a todos (BUG-015). `tipo = "E"` confirmado como alumno (BUG-016).
- [x] **Sin alta automática** (DEC-036): si la matrícula no existe en AppTESCHI → 404 y se pide contactar al administrador. Pedido explícito del alumno.
- [x] **Envío del código arreglado** (BUG-017): la app leía mal el `identificador` y el backend exigía un correo escrito por el cliente. Ahora el correo sale del servidor y la pantalla lo muestra enmascarado.
- [x] **Correo institucional por Microsoft Graph** (DEC-034) funcionando en el servidor: los códigos llegan desde la cuenta de `@teschi.edu.mx`.
- [x] **Probado de punta a punta** en el teléfono con un alumno real de ISC.
- [x] **Despliegue saneado:** scripts `.sh` con LF (BUG-018) y fuera el proceso `nohup` que peleaba el puerto 4000 con systemd (BUG-019).
- [x] **Postman** con dos carpetas nuevas: el flujo completo de AppTESCHI y la API del SIIA directa. Login directo al SIIA probado (200). Ver [[01_POSTMAN_API]].
- [x] **Mapeo de carreras** (DEC-037): el backend enlaza la carrera del alumno con el `idCarrera` del SIIA. En la base del servidor ya está ISC = 4 (se corrió con `sqlcmd`; ver BUG-020 por los detalles de `-C` y `QUOTED_IDENTIFIER`).
- [x] **Commits:** `3bbb1a9` (25/09) y `acd8ffa` (27/09), subidos a GitHub.
- [x] **Bóveda ordenada:** una sola bóveda dentro del repo `App-Teschi` (la de Descargas quedó como respaldo), notas obsoletas borradas, notas de login/SIIA/Postman reescritas, SQL renumerado (`24_VinculoSiia`, `25_MapeoCarrerasSiia`, `pendientes/`).

---

## Pendiente

- [ ] **Desplegar `acd8ffa`** (enlace automático de carrera). Se empezó a preparar el servidor para actualizar con `git pull` (clonar el repo en `/opt/appteschi/repo` y apuntar el servicio ahí); quedó a medias.
- [ ] Correr en el servidor la versión nueva de `25_MapeoCarrerasSiia.sql` para quitar el índice duplicado `UQ_CatalogoCarreras_IdCarreraSiia` (inofensivo, pero sobra).
- [ ] Mapear las otras 8 carreras conforme entren alumnos (log `[AUTH-SIIA][PENDIENTE]`).
- [ ] Probar en Postman "Listar grupos" y "Listar alumnos" de la API del SIIA (sin "Guardar asistencia").
- [ ] Arreglar la sustitución de `{{siia_usuario}}` / `{{siia_password}}` en Postman y quitar la contraseña escrita a mano en el Body.
- [ ] Fase 3: docentes ([[09_Login_SIIA_por_Tipo]]).
