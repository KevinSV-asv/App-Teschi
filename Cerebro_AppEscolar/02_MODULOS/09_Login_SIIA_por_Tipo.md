# Módulo 9: Login unificado con el SIIA (por tipo de usuario)

- **Estado:** ✅ Alumnos, directo desde la app al SIIA (DEC-043, 12/10/2026). Docentes y administrativos: la app responde "Por ahora AppTESCHI es solo para alumnos…". Las fases de abajo describen el plan con el backend, que ya no se usa; el flujo vigente está en [[2FA_Login]].
- **Alcance:** de la API oficial del SIIA **solo se usa `POST /login.ashx`**. La asistencia (`/asistencia/*`) queda **fuera del proyecto** (decisión del alumno, 22/09/2026 y 25/09/2026).
- **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]], [[API_SIIA_Oficial]], [[00_ARQUITECTURA_API]], [[06_PanelAdministrador]], [[08_Horarios]], [[DECISIONES_TECNICAS]] (DEC-035, DEC-036, DEC-037, DEC-040)

---

## Idea general

```
LoginScreen — formulario único (DEC-038)
   │ usuario SIIA + contraseña
   ▼
AppTeschi.Api  POST /api/auth/siia ──► SIIA /login.ashx
   │ 1. Valida la contraseña con el SIIA
   │ 2. Clasifica por `tipo`
   │ 3. Vincula con su registro local si existe; si no, entra sin registro (nunca crea uno, DEC-040)
   │ 4. Devuelve ticket de login (DEC-030)
   ▼
OTP al correo guardado (DEC-019, DEC-034)  ──►  JWT propio por rol
   ▼
Dashboard según el rol:  Alumno  |  Docente (Fase 3)  |  Administrador (Fase 5)
```

**Reparto de datos:**
- El **SIIA** es la fuente de la identidad: quién es, tipo y contraseña.
- **AppTeschiDB** es la fuente de todo lo demás: quién tiene acceso a AppTESCHI, carrera, semestre, historial, reinscripción, horarios y rol de administrador.

### Reparto por tipo

| `tipo` del SIIA | Registro local | Token | Estado |
|---|---|---|---|
| `E` (alumno) | `Alumnos`, por matrícula. **Si no existe entra sin registro** (solo datos del SIIA, nada se guarda, DEC-040) | `AUD_ALUMNO` (`sinRegistro` si no está dado de alta) | ✅ En producción · sin registro ⏳ sin desplegar |
| Docente | `Docentes` (tabla por crear) | `AUD_DOCENTE` | ⏳ Fase 3 — hoy responde 501 |
| Administrativo | Solo si un SUPERADMIN lo vinculó en `Administradores.UsuarioSiia` | `AUD_ADMIN` | ⏳ Fase 5 — hoy responde 403 |
| Cualquier otro | — | — | 403 "tipo de cuenta no soportado" |

> **Al alumno el SIIA le basta para entrar, pero no para tener datos** (DEC-040): sin alta en AppTESCHI solo ve lo que da el SIIA. Los permisos de administrador y los datos académicos nunca salen del SIIA.

---

## Qué quedó hecho (Fases 1 y 2, alumnos)

### Backend (`server.js`)
- `POST /api/auth/siia` llama al SIIA con `https` nativo (`loginSiiaReal()`, timeout 10 s). **No** usa el `siia-api.js` que se escribió el 23/09 (quedó en la bóveda vieja y se descartó).
- `mapTipoSiia()`: `E` → ALUMNO, `P` → DOCENTE, `A` → ADMINISTRADOR (Swagger 1.1; `E` también confirmado con logins reales).
- NIP del SIIA (API 1.1, DEC-041): `/api/auth/siia` → `requiereNip` + `ticketNip`; `/api/auth/siia/nip` verifica o crea el NIP y entrega la sesión directo (sin código por correo); `/api/auth/siia/nip/recuperar` por CURP.
- Alumno inexistente → entra sin registro: ticket con `sinRegistro`, datos del SIIA y OTP solo en memoria, log `[AUTH-SIIA][SIN-REGISTRO]` (DEC-040; antes 404).
- Primer login → guarda `Alumnos.UsuarioSiia`.
- Sin carrera → intenta enlazar por `CatalogoCarreras.IdCarreraSiia` (`[AUTH-SIIA][CARRERA-ENLAZADA]`); si no está mapeada → `cuentaPendiente` + log `[AUTH-SIIA][PENDIENTE] … idCarreraSiia=N area="…"` (DEC-037).
- Sin ningún correo → ticket con `correoEnmascarado` vacío; la app pide el correo (BUG-022).
- `POST /api/otp/enviar`: el correo sale del servidor (`CorreoOtp` / `CorreoInstitucional`); el del cliente solo si no hay ninguno, y `/api/otp/verificar` lo guarda al verificar (BUG-022).

### App Android
- `LoginScreen`: formulario único desde el 28/09/2026 (antes pestañas "Cuenta AppTESCHI" / "SIIA", DEC-038); el SIIA es el último paso de la cascada; `CuentaPendienteSection`; `NipSection` (NIP del SIIA, crearlo o recuperarlo por CURP, DEC-041). Si el SIIA no pide NIP, en la verificación se muestra el correo enmascarado sin campo editable.
- `SiiaAuthService.kt` lee `identificador` de la respuesta (antes buscaba `matricula`/`usuario` y fallaba, BUG-017).
- `AuthViewModel.enviarCodigoVerificacion()` no exige escribir un correo en el flujo SIIA.

### Base de datos
| Script | Estado |
|---|---|
| `24_VinculoSiia.sql` — `Alumnos.UsuarioSiia`, `Alumnos.OrigenCuenta`, `CatalogoCarreras.IdCarreraSiia` | ✅ Aplicado 23/09/2026 (`OrigenCuenta = 'SIIA'` quedó sin uso) |
| `25_MapeoCarrerasSiia.sql` — equivalencias de carrera (ISC = 4) | ✅ ISC mapeada el 27/09/2026; falta correr la versión nueva para quitar un índice duplicado |
| `pendientes/Docentes.sql` | ⏳ Fase 3 |
| `pendientes/AdministradoresSiia.sql` | ⏳ Fase 5 |

---

## Plan por fases

### Fase 0 — Preparación
- [x] Valor real de `tipo` para alumnos: `E` (25/09/2026).
- [x] El `numUsuario` del alumno es su matrícula.
- [x] El SIIA responde desde el servidor Ubuntu (lo usa en producción).
- [x] `idCarrera` de ISC = 4.
- [x] Valores de `tipo` de docente (`P`) y administrativo (`A`): publicados en el Swagger 1.1 (29/09/2026).
- [ ] Cuentas de prueba de **docente** y **administrativo** (para probar sus flujos).
- [ ] `idCarrera` de las otras 8 carreras (se van llenando con los logs `[PENDIENTE]`).

### Fase 1 — Conexión base ✅ (23/09/2026)

### Fase 2 — Alumnos entran con el SIIA ✅ (25/09/2026)
- [x] Backend, BD y app (ver arriba).
- [x] Probado con un alumno real en el teléfono: SIIA → OTP por correo institucional → dashboard.
- [ ] Desplegar en el servidor el enlace automático de carrera (commit `acd8ffa`, 27/09/2026).
- [ ] `CambioPasswordRequeridoScreen` cuando el SIIA manda `cambioPw` (hoy solo viaja el dato).

### Fase 3 — Docentes: acceso y perfil ⏳
- **BD:** `pendientes/Docentes.sql`.
- **Backend:** vincular docentes (¿alta por el administrador, igual que alumnos?), OTP y token de docente, `requireDocente()`, `GET /api/docente/mi-perfil`.
- **App:** `DocenteDashboardScreen` real (hoy `Routes.DOCENTE_DASHBOARD` existe pero sin contenido), `DocentePerfilScreen`.

### Fase 4 — Horario del docente + gestión de docentes ⏳
- `GET /api/docente/mi-horario`, `GET /api/docentes`, `PUT /api/docentes/:usuario`, `AdminDocentesScreen`.
- `Docentes.NombreEnHorario` empata al docente con el texto libre de `Horarios.Profesor` (DEC-029).

### Fase 5 — Administradores vinculados al SIIA ⏳
- `pendientes/AdministradoresSiia.sql`; campo "Usuario SIIA" en el formulario de administradores; `/api/auth/administrador` se queda como acceso de emergencia.

### Fase 6 — Cierre de la transición ⏳
- [ ] Decidir si se retiran el registro y la cuenta propia de alumno o se quedan como respaldo.

---

## Decisiones tomadas (antes "abiertas")
1. **Alumno que no existe en AppTeschiDB:** entra con los datos del SIIA sin que se cree ningún registro; sus datos académicos aparecen cuando el administrador lo da de alta (DEC-040, 28/09/2026; el alta automática de DEC-039 se descartó).
2. **Correo del OTP:** el guardado en el servidor; si no hay ninguno, la app lo pide una sola vez.
3. **Cuentas propias después de la migración:** sigue abierta (Fase 6).
