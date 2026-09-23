# Módulo 1: Reinscripción

- **Estado:** Fases 1 y 2 completas — flujo del alumno y panel del director/control escolar 100% reales (ver secciones al final).
- **Ruta en App:** `Routes.REINSCRIPCION`
- **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]]

---

## Descripción General

Permite al alumno realizar o consultar su reinscripción al periodo siguiente. El flujo incluye verificación de estatus académico, selección de grupo/turno y, si aplica, carga del comprobante de pago.

---

## Campos Requeridos

| Campo                     | Tipo          | Descripción                                       |
|---------------------------|---------------|---------------------------------------------------|
| Estatus del alumno        | Enum          | Regular / Irregular                               |
| Periodo a reinscribir     | String        | Clave del próximo periodo (ej. `2024-B`)          |
| Grupo / Turno             | String/Enum   | Grupo asignado o disponible para selección        |
| Comprobante de pago       | File (PDF/IMG)| Imagen o PDF del recibo de pago                   |
| Matrícula                 | String        | Obtenida del perfil de sesión activa (JWT)        |
| Semestre actual           | Int           | Para validar elegibilidad de reinscripción        |

---

## Flujo de Pantalla

1. Usuario accede desde el Dashboard.
2. App consulta el estatus del alumno vía API.
3. Si tiene adeudos o está en situación irregular, mostrar bloqueo con mensaje explicativo.
4. Si es elegible, mostrar formulario de selección de grupo/turno.
5. Usuario adjunta comprobante de pago.
6. App envía solicitud de reinscripción.
7. Se muestra folio de confirmación o mensaje de error.

---

## Endpoint API

> ⚠️ **PENDIENTE** — Ver [[01_APIS_POSTMAN]] cuando se documente.

```
GET  /api/alumnos/{matricula}/estatus-reinscripcion
POST /api/reinscripcion
Authorization: Bearer {jwt_token}
```

**Payload POST (tentativo):**
```json
{
  "matricula": "12345678",
  "periodo": "2024-B",
  "grupo": "3A",
  "turno": "MATUTINO",
  "comprobante_pago": "<base64 o multipart>"
}
```

---

## Estados de UI

- `Loading`   — CircularProgressIndicator durante la consulta de estatus
- `Bloqueado` — Card de advertencia con motivo del bloqueo (adeudo, documentos, etc.)
- `Formulario`— Pantalla de selección de grupo y carga de comprobante
- `Enviando`  — CircularProgressIndicator durante el envío
- `Éxito`     — Pantalla de confirmación con folio
- `Error`     — Snackbar con descripción del error y opción de reintento

---

## Reglas de Negocio Pendientes de Confirmar

- ¿Existe un periodo de reinscripción activo que la API retorne?
- ¿Qué condiciones bloquean la reinscripción (adeudos, materias reprobadas, documentos)?
- ¿El comprobante se envía como `multipart/form-data` o Base64?
- ¿El sistema asigna grupo automáticamente o el alumno lo elige?

---

## Notas de Implementación

- Lógica en `ReinscripcionViewModel` (MVVM).
- El interceptor de Auditoría debe registrar el envío de la solicitud.
- Para la carga del archivo: usar `ActivityResultContracts.OpenDocument()` con MIME `image/*` y `application/pdf`.

## Implementación inicial 03/09/2026

- La ruta `Routes.REINSCRIPCION` ya muestra una pantalla funcional en Android.
- Se implementaron los estados de carga, elegible, formulario, envío, éxito y error.
- El formulario solicita periodo, grupo, turno y comprobante en PDF o imagen.
- El envío actual genera un folio local y registra el movimiento en la auditoría local.
- La consulta real de estatus y el envío al servidor todavía deben conectarse a los endpoints institucionales definidos arriba; no se simula una confirmación de SQL Server.
- El siguiente paso de backend es definir el contrato final de `GET /api/alumnos/{matricula}/estatus-reinscripcion` y `POST /api/reinscripcion`, incluyendo autenticación y almacenamiento del comprobante.

---

## Corrección 06/09/2026

- **BUG-012 resuelto:** La pantalla existía desde el 03/09 pero `MainActivity.kt` apuntaba la ruta al placeholder genérico. Corregido en v0.6.3.
- La pantalla ahora es accesible desde el Dashboard del alumno con tap en "Reinscripción".

### Estado actual del módulo

| Elemento | Estado |
|---|---|
| `ReinscripcionScreen.kt` | ✅ Implementado — 6 estados, file picker, folio UUID local |
| Consulta estatus vs SIIA | ⚠️ Mock — simula elegibilidad con delay de 350ms |
| Envío real a API | ⚠️ Mock — folio local UUID, sin POST real |
| Registro en AuditTrail local | ✅ Registra el intento de reinscripción |
| Registro en SQL Server | 🔴 Pendiente — requiere endpoint `/api/reinscripcion` |
| `ReinscripcionViewModel` (MVVM) | 🔴 Pendiente — lógica actualmente en el Composable |

---

## Fase 1 — datos reales (15/09/2026)

Se reemplazó por completo la maqueta anterior. Ya no hay comprobante de pago
en el flujo (no aparece en el portal real del SIIA — se corrigió tras
comparar contra una captura real de "Ins-Reinscripción en línea").

**Endpoints reales** (`server.js`, sin JWT de admin — el alumno no tiene
sesión propia, se protegen con `x-api-key` como `/api/otp/enviar`):

```
GET  /api/reinscripcion/estatus/:matricula
POST /api/reinscripcion/solicitud   { matricula, claveGrupo }
```

- **Regular/irregular ya no se simula**: se calcula comparando
  `dbo.HistorialAcademico` contra `dbo.PlanEstudioMaterias` — regular =
  ninguna materia de un semestre anterior al suyo sigue sin `AP`.
- Si es regular, el backend asigna el primer grupo (`MIN(IdGrupo)`) de su
  semestre/carrera — **limitación conocida**: no existe ningún campo que
  registre a qué turno pertenece cada alumno, así que no se puede saber
  cuál de los grupos disponibles es "el suyo" de verdad. Aceptable para
  Fase 1 porque casi todos los semestres solo tienen un grupo por turno.
- Si es irregular, el alumno elige entre todos los grupos de su
  carrera/semestre (`GET .../grupos`, misma tabla que usa PlanEstudiosService).
- El envío ya persiste de verdad en `dbo.SolicitudesReinscripcion`
  (`database/sql/19_SolicitudesReinscripcion.sql`) con folio real y
  `Estatus = PENDIENTE` — la app ya no inventa un folio local.
- **Se detectó y corrigió un hueco de datos**: `dbo.HistorialAcademico` no
  tenía ninguna fila para el alumno real que se usó para probarlo (matrícula
  ficticia `2099000001` en esta documentación) — su historial solo vivía
  hardcodeado en el mock de Kotlin. Se migró a la base de datos real
  (script fuera de este repositorio; ver DEC-032) (transcrito literal
  del Kardex oficial, MERGE idempotente). Ese mismo endpoint real
  (`/api/mi-historial/{matricula}`) también conecta a Kardex/Tira/Calificaciones
  del lado del alumno desde DEC-023 — ya no queda ningún dato simulado ahí.

---

## Fase 2 — bloqueo por reglamento y panel del director (15/09/2026)

**Tabla nueva:** `dbo.ObservacionesReglamento`
(`database/sql/22_ObservacionesReglamento.sql`) — `IdAlumno`, `Motivo`,
`Estado` (PENDIENTE/AUTORIZADA/RECHAZADA), `RegistradaPor`, `FechaRegistro`,
`ResueltaPor`, `FechaResolucion`, `Resolucion`.

**Endpoints reales** (todos `requireAdmin()` salvo que se indique):

```
GET  /api/observaciones/:matricula
POST /api/observaciones                    { matricula, motivo }
PUT  /api/observaciones/:id/resolver        { decision: AUTORIZADA|RECHAZADA, resolucion? }
GET  /api/reinscripcion/solicitudes         ?estado=PENDIENTE (opcional)
PUT  /api/reinscripcion/solicitudes/:id/confirmar
```

- `GET /api/reinscripcion/estatus/:matricula` ahora revisa **primero** si hay
  una observación `PENDIENTE` — si la hay, regresa `estatus: "BLOQUEADO"` con
  el `motivo` real y nunca llega a calcular regular/irregular.
  `POST /api/reinscripcion/solicitud` también rechaza (409) si el alumno
  sigue bloqueado, como defensa adicional del lado del servidor (no solo
  la UI).
- **App — lado alumno:** `ReinscripcionViewModel` ya maneja
  `ReinscripcionEstado.BLOQUEADO` mostrando el motivo real que registró el
  director, en vez del texto genérico que traía desde la Fase 1.
- **App — lado administrador:** en `AdminProfileScreen` (perfil de cada
  alumno) hay una tarjeta "Observaciones de reglamento" — lista el
  historial completo, permite registrar una nueva (bloquea de inmediato) y
  Autorizar/Rechazar las pendientes. Nueva pantalla
  `AdminReinscripcionesScreen` (`Routes.ADMIN_REINSCRIPCIONES`, sección
  "Reinscripciones" del Centro de Control) lista las solicitudes pendientes
  de confirmar presencialmente y las marca `CONFIRMADA`.
- Verificado de punta a punta con curl antes de tocar la app: crear
  observación → estatus pasa a BLOQUEADO con el motivo real → resolver
  (AUTORIZADA) → estatus vuelve a REGULAR/IRREGULAR normalmente.
