# Módulo 9: Login unificado con el SIIA (por tipo de usuario)

- **Estado:** 📝 Plan por fases (22/09/2026, reestructurado). Nada implementado todavía.
- **Alcance:** de la API oficial del SIIA **solo se usa `POST /login.ashx`**. Las APIs de asistencia (`/asistencia/*`) quedan **fuera del proyecto** por decisión del alumno (22/09/2026).
- **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]], [[00_ARQUITECTURA_API]], [[06_PanelAdministrador]], [[08_Horarios]], [[DECISIONES_TECNICAS]], [[SIIA_Scraping]] (histórico)

---

## Qué da el SIIA

`POST https://siia.teschi.edu.mx/Teschi/api/login.ashx` con `{ usuario, password }` (documentación: `…/api/swagger/index.html`).

- **200:** `ok`, `mensaje`, `token` (480 min) y `usuario` con `usuario`, `numUsuario`, `nombre`, `paterno`, `materno`, `nombreCompleto`, `correo` (puede venir vacío), `tipo`, `idArea`, `area`, `idCarrera`, `tipoCarrera`, `permisos`, `cambioPw`, `puedeAsistencia`.
- **Errores:** 400, 401 (credenciales), 403 (sin acceso), 405, 422, **429** (5 intentos por usuario+IP cada 60 s, con `Retry-After`), 500.

Como no se usa ningún otro endpoint del SIIA, **el token del SIIA no se guarda**: solo prueba que la contraseña es correcta. Después de eso, la sesión es 100% de AppTESCHI (ticket → OTP → JWT propio), igual que hoy.

## Idea general

```
LoginScreen (un solo formulario)
   │ usuario SIIA + contraseña
   ▼
AppTeschi.Api  POST /api/auth/siia ──► SIIA /login.ashx
   │ 1. Valida la contraseña con el SIIA
   │ 2. Clasifica por `tipo`
   │ 3. Vincula o crea el registro local (Alumnos / Docentes / Administradores)
   │ 4. Devuelve ticket de login (DEC-030)
   ▼
OTP al correo (flujo actual, DEC-019)  ──►  JWT propio por rol
   ▼
Dashboard según el rol:  Alumno  |  Docente  |  Administrador
```

**Regla de reparto de datos:**
- El **SIIA** es la fuente de la identidad: quién es, nombre, correo, tipo y contraseña.
- **AppTeschiDB** es la fuente de lo académico y de los permisos dentro de la app: semestre, historial, reinscripción, horarios y rol de administrador.

### Reparto por tipo

Los valores exactos de `tipo` se confirman en la fase 0.

| `tipo` del SIIA | Registro local | Token | A dónde entra |
|---|---|---|---|
| Alumno | `Alumnos`: se vincula por matrícula; si no existe, se da de alta automáticamente | `AUD_ALUMNO` (el actual) | Dashboard del alumno **sin cambios** |
| Docente | `Docentes` (tabla nueva) | `AUD_DOCENTE` (nuevo) | Dashboard del docente (nuevo) |
| Administrativo / otro | Solo si está vinculado en `Administradores.UsuarioSiia` | `AUD_ADMIN` (el actual) | Panel del administrador |
| Cualquier otro caso | — | — | Pantalla "Tu tipo de cuenta aún no tiene acceso a AppTESCHI" |

> **El SIIA nunca da permisos de administrador por sí solo.** Una cuenta de "administrativo" del SIIA solo entra al panel si un SUPERADMIN la vinculó antes en AppTESCHI.

---

## Vistas (app Android)

### Nuevas

| Vista | Rol | Contenido | Fase |
|---|---|---|---|
| `CambioPasswordRequeridoScreen` | Todos | Aparece cuando el SIIA manda `cambioPw = 1`: explica que debe cambiarla en el SIIA web y trae un botón para abrirlo | 2 |
| `CuentaPendienteScreen` | Alumno / otros | Casos "Tu cuenta está pendiente de que control escolar asigne carrera/semestre" y "Tu tipo de cuenta aún no tiene acceso" | 2 |
| `DocenteDashboardScreen` | Docente | Inicio: hero con nombre, área y fecha, **clases de hoy** (desde `Horarios`) y accesos a Horario y Perfil. Barra inferior propia: Inicio · Horario · Perfil | 3–4 |
| `DocentePerfilScreen` | Docente | Datos del SIIA en solo lectura (nombre, número de usuario, correo, área), botón "Cambiar contraseña en el SIIA" y cerrar sesión | 3 |
| `DocenteHorarioScreen` | Docente | Horario semanal por día (materia, grupo, carrera, aula, hora) | 4 |
| `AdminDocentesScreen` | Administrador | Lista de docentes que ya entraron; activar/desactivar; asignar el "nombre como aparece en el horario" | 4 |
| Vinculación SIIA en admins | SUPERADMIN | Campo "Usuario SIIA" en el formulario de administradores que ya existe | 5 |

### Modificadas

| Vista | Cambio | Fase |
|---|---|---|
| `LoginScreen` | Un solo formulario, "Usuario SIIA" + contraseña. Mensajes claros para 401, 403 y 429 (con cuenta regresiva de `Retry-After`). "¿Olvidaste tu contraseña?" abre el SIIA web | 2 |
| Pantalla de OTP | Sirve para los 3 roles y muestra el correo enmascarado al que se envió | 2 |
| Navegación post-login (`MainActivity` / `AuthRepository`) | Enruta según el rol devuelto; nuevo `LoginResultado` por rol | 2 |
| `PerfilAlumnoScreen` | Etiqueta "Cuenta vinculada al SIIA". Nombre y correo en solo lectura desde el SIIA. "Cambiar contraseña" manda al SIIA web cuando la cuenta es del SIIA | 2 |
| `RegistroScreen` / `RecuperarPasswordScreen` | Durante la transición se quedan solo para cuentas propias, como opción secundaria. Se decide si se retiran en la fase 6 | 2 / 6 |
| Dashboard del alumno y sus módulos | **Sin cambios**: siguen usando el mismo JWT de alumno | — |

---

## Conexiones (backend `AppTeschi.Api`)

### Nuevas

| Endpoint | Protección | Qué hace | Fase |
|---|---|---|---|
| `POST /api/auth/siia` | Límite de intentos | Llama a `/login.ashx`, clasifica por `tipo`, vincula o crea el registro local, actualiza nombre y correo, audita `LOGIN_SIIA`. Responde `{ ok, rol, identificador, nombre, correoEnmascarado, requiereCambioPw, ticket }`. Reenvía el 429 con `Retry-After` | 1 |
| `GET /api/docente/mi-perfil` | `requireDocente()` | Datos del docente desde `Docentes` | 3 |
| `GET /api/docente/mi-horario?dia=` | `requireDocente()` | Clases de `Horarios` donde `Profesor` = `Docentes.NombreEnHorario`; sin `dia` devuelve toda la semana | 4 |
| `GET /api/docentes` | `requireAdmin()` | Lista de docentes con su último acceso | 4 |
| `PUT /api/docentes/:usuario` | `requireAdmin()` | Activar o desactivar al docente y asignar `NombreEnHorario` (auditado) | 4 |

### Modificadas

| Endpoint | Cambio | Fase |
|---|---|---|
| `POST /api/otp/enviar` y `/verificar` | Aceptan `tipo = DOCENTE`. El correo destino sale del servidor (el del SIIA o el `CorreoOtp` guardado), nunca del cliente | 2–3 |
| `firmarTicketLogin` / `ticketLoginValido` | Aceptan `DOCENTE` | 3 |
| `PUT /api/administradores/:usuario` | Acepta `usuarioSiia` para vincular | 5 |
| `POST /api/auth/cuenta` | Se queda como respaldo durante la transición; se decide su retiro en la fase 6 | 6 |

### Archivos y configuración
- `siia-api.js` (nuevo, mismo patrón que `correo-graph.js`): `loginSiia(usuario, password)` con `fetch`, timeout de 10 s y errores sin datos sensibles.
- `.env` / `.env.example`: `SIIA_API_URL=https://siia.teschi.edu.mx/Teschi/api`.
- Nuevos `AUD_DOCENTE`, `firmarTokenDocente()` y `requireDocente()`, calcados de los del alumno.

### Base de datos (`04_BASE_DATOS/sql/`)

| Script | Contenido | Fase |
|---|---|---|
| `24_VinculoSiia.sql` | `Alumnos.UsuarioSiia`, `Alumnos.OrigenCuenta` (`PROPIA`/`SIIA`), `CatalogoCarreras.IdCarreraSiia` (equivalencia de carreras) | 2 |
| `25_Docentes.sql` | Tabla `Docentes` (`IdDocente`, `UsuarioSiia`, `NumUsuario`, `NombreCompleto`, `Correo`, `IdArea`, `Area`, `NombreEnHorario`, `Activo`, `UltimoAcceso`, `CreadoEn`) + `OtpHistorial.IdDocente` | 3 |
| `26_AdministradoresSiia.sql` | `Administradores.UsuarioSiia` (único, opcional) | 5 |

---

## Plan por fases

Cada fase termina con algo **usable y probado**. No se empieza la siguiente sin cerrar la anterior.

### Fase 0 — Preparación
- [ ] Conseguir con TI **cuentas de prueba de cada tipo**: alumno, docente y administrativo.
- [ ] Averiguar en Postman: **valores reales de `tipo`**; si el `usuario` del alumno es su matrícula; si `correo` viene lleno; qué `idCarrera` corresponde a cada carrera; y qué pasa con `cambioPw`.
- [ ] Confirmar que el SIIA responde **desde el servidor Ubuntu** (por SSH: `curl` a `/api/swagger/openapi.json`).
- [ ] Registrar **DEC-035**: "Login unificado con la API oficial del SIIA, por tipo de usuario; asistencia fuera de alcance". Modifica DEC-026 **solo en el login**: los datos académicos siguen siendo propios.
- **Terminado cuando:** está la tabla de tipos → rol y la de equivalencias de carrera.

### Fase 1 — Conexión base (solo backend)
- [ ] `siia-api.js`, `SIIA_API_URL` y `POST /api/auth/siia` que **solo valida y clasifica**, todavía sin vincular.
- [ ] Límite local de intentos y reenvío del 429; auditoría `LOGIN_SIIA`.
- [ ] Carpeta **SIIA** en la colección de Postman.
- **Terminado cuando:** Postman recibe el rol correcto con las 3 cuentas de prueba.

### Fase 2 — Alumnos entran con el SIIA
- **BD:** `24_VinculoSiia.sql` + equivalencias de carrera.
- **Backend:** vinculación por matrícula. Si el alumno no existe, se crea con los datos del SIIA; si falta semestre o carrera, queda como "pendiente". Ticket → OTP → JWT de alumno.
- **App:** `LoginScreen` unificado, OTP con correo enmascarado, enrutador por rol, `CambioPasswordRequeridoScreen`, `CuentaPendienteScreen`, ajustes a `PerfilAlumnoScreen`.
- **Transición:** si el SIIA rechaza la contraseña o no responde, se intenta con la cuenta propia (`/api/auth/cuenta`), para no dejar fuera a nadie mientras se migra.
- **Terminado cuando:** un alumno real entra con su contraseña del SIIA y ve **su** dashboard de siempre.

### Fase 3 — Docentes: acceso y perfil
- **BD:** `25_Docentes.sql`.
- **Backend:** alta automática del docente en `Docentes`, OTP y token de docente, `requireDocente()`, `GET /api/docente/mi-perfil`.
- **App:** `SesionDocente`, `DocenteService` (sobre `ClienteHttp`, DEC-033), `DocenteDashboardScreen` (versión inicial: hero + perfil) y `DocentePerfilScreen`.
- **Terminado cuando:** un docente entra y ve su inicio y su perfil.

### Fase 4 — Horario del docente + gestión de docentes (admin)
- **Backend:** `GET /api/docente/mi-horario`, `GET /api/docentes`, `PUT /api/docentes/:usuario`.
- **App (docente):** "Clases de hoy" en el Inicio y `DocenteHorarioScreen`.
- **App (admin):** `AdminDocentesScreen`, más una tarjeta "Docentes" en el panel.
- **Por qué hace falta `NombreEnHorario`:** en `Horarios`, `Profesor` es texto libre que viene del Excel (DEC-029). El administrador empata una sola vez ese nombre con el docente real; no se adivina por parecido.
- **Terminado cuando:** un docente ve exactamente las clases que el Excel le asigna.

### Fase 5 — Administradores vinculados al SIIA
- **BD:** `26_AdministradoresSiia.sql`.
- **Backend:** si el `usuario` del SIIA está en `Administradores.UsuarioSiia` y el admin está activo → JWT de admin con su rol de siempre.
- **App:** campo "Usuario SIIA" al crear o editar administradores (solo SUPERADMIN).
- **Se conserva** `/api/auth/administrador` con cuenta local como **acceso de emergencia** si el SIIA se cae.
- **Terminado cuando:** un administrativo vinculado entra al panel con su contraseña del SIIA y uno no vinculado es rechazado.

### Fase 6 — Cierre de la transición
- [ ] Decidir (DEC-036) si se retiran el registro y la cuenta propia de alumno, o si se quedan como respaldo.
- [ ] Ajustar `RegistroScreen` y `RecuperarPasswordScreen` según esa decisión.
- [ ] Actualizar [[2FA_Login]], [[REGLAS_PROYECTO]] (aún describe el scraping como vigente), [[00_ARQUITECTURA_API]], [[04_Diagrama_Componentes]], [[03_Diagramas_Secuencia]] y [[01_Casos_de_Uso]] (nuevo actor Docente).

### En cada fase
- [ ] Pruebas unitarias de los ViewModels y servicios nuevos.
- [ ] Casos en [[02_Catalogo_Casos_de_Prueba]]: login por tipo, 401, 403, 429, `cambioPw`, cuenta pendiente, SIIA caído → respaldo.
- [ ] Despliegue con `deploy-desde-windows.ps1` (y `SIIA_API_URL` en el `.env` del servidor).
- [ ] Entradas en [[CAMBIOS_DE_CODIGO]] y en `05_AVANCES`.

---

## Decisiones abiertas
1. **Alumno sin carrera o semestre en AppTeschiDB:** ¿alta automática como "pendiente" (propuesta) o bloquear hasta que control escolar lo dé de alta?
2. **Correo para el OTP cuando el SIIA lo manda vacío:** ¿usar el `CorreoOtp` guardado y, si tampoco existe, pedir que lo registre una sola vez?
3. **Cuentas propias de alumno después de la migración:** ¿se retiran o se quedan como respaldo? (fase 6)
