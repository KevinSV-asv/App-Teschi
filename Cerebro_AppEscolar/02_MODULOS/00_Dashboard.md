# Diseño de Referencia — Dashboard / Inicio

- **Estado:** ✅ Inicio con datos del SIIA y resumen del periodo actual (DEC-043, 12/10/2026). Barra inferior flotante de 5 pestañas (DEC-028). El diseño v1 de abajo (grid de tarjetas) se conserva como referencia histórica.
- **Referencia web:** `teschi.teschi.edu.mx` — plataforma Servicio Social y Residencias Profesionales
- **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]], [[REGLAS_PROYECTO]]

---

## Diseño real (actualizado 12/10/2026)

`DashboardScreen.kt` es la pantalla "Inicio":

- **Header** blanco con el logo y el botón "Mi perfil" ([[07_PerfilAlumno]]).
- **Hero** verde: chips "Alumno" y "Periodo 2026-2", "¡Hola, Nombre!", la carrera del SIIA y la matrícula.
- **Tu periodo actual:** promedio parcial, materias y evaluadas, tomados de las calificaciones parciales del SIIA (`InicioViewModel` → `CalificacionesSiiaService.parciales(null)`), con botón "Ver mis calificaciones". Si el SIIA bloquea la consulta por trámites lo dice; si falla, muestra el error con "Reintentar" sin ocultar el saludo.
- Ya **no** hay horario de hoy: el SIIA no expone horarios (DEC-043).

Barra inferior flotante (`BottomNavBar.kt`, DEC-028):

- **5 tabs:** Inicio, Tira de Materias ("Materias"), Calificaciones ("Calif."), Reinscripción ("Reinscrip.", con Intersemestral dentro, DEC-046) y Kardex.
- **Navegación:** patrón estándar de Navigation-Compose para bottom nav (`popUpTo(Routes.DASHBOARD){ saveState = true }` + `launchSingleTop = true` + `restoreState = true`): cambiar de tab no crece el back stack ni pierde el estado.
- **Animación:** entre dos tabs la transición desliza en la dirección real del cambio (orden de `TABS_DASHBOARD`); para lo demás, fundido con escala.

---

## Fuente de inspiración

El usuario compartió captura del portal web institucional post-login. Elementos clave a replicar en AppTESCHI:

| Elemento web | Adaptación en app Android |
|---|---|
| Header blanco con logo **TESChi** | `DashboardHeader` — logo verde + campana + pill de usuario |
| Pills de navegación (Inicio, SS, Residencias) | Sustituido por grid de **módulos escolares** (4 cards) |
| Hero con foto del campus + título grande | `HeroBienvenida` — gradiente verde + card blanca informativa |
| Cards horizontales con icono circular verde | Grid 2×2 con iconos en círculo verde claro |
| FAB "Asistente disponible" | `ExtendedFloatingActionButton` verde (pendiente lógica) |
| Pill usuario con nombre (ej. ALUMNO DE PRUEBA) | `UserSession.nombreMostrar` desde matrícula autenticada |

---

## Módulos del Dashboard (AppTESCHI)

| Card | Ruta | Estado |
|---|---|---|
| Reinscripción | `Routes.REINSCRIPCION` | Placeholder |
| Tira de Materias | `Routes.TIRA_MATERIAS` | Placeholder |
| Calificaciones | `Routes.CALIFICACIONES` | Placeholder |
| Intersemestral | Dentro de `Routes.REINSCRIPCION` | Placeholder |

---

## Sesión de usuario

Tras OTP exitoso, `UserSession` guarda en memoria:
- `matricula`, `nombreCompleto`, rol y el **token de sesión** (`alumnoToken`, DEC-030)

Se limpia al cerrar sesión desde [[07_PerfilAlumno]] o cuando el backend responde que la sesión venció. *(Esta sección es del diseño v1; ver el estado arriba.)*

---

## Pendiente

- [ ] Imagen real del campus en hero (asset drawable)
- [ ] Notificaciones funcionales (badge dinámico)
- [ ] Asistente / chatbot
- [ ] Registrar usuario en SQL Server tras login (`sp_UpsertUsuario`)
- [ ] Mostrar nombre completo desde BD en lugar de matrícula

---

## Paleta usada

| Token | Hex | Uso en dashboard |
|---|---|---|
| `GreenPrimary` | `#1E5631` | Títulos, logo |
| `LoginGreenBright` | `#4CAF50` | Pill usuario, FAB |
| Hero gradient | `#2E6B3E` → `#1A4D2E` | Banner bienvenida |
