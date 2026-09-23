# Diseño de Referencia — Dashboard / Inicio

- **Estado:** ✅ Rediseñado (16/09/2026, ver DEC-028) — el diseño original de abajo (v1, grid de tarjetas) quedó reemplazado por una barra de navegación inferior flotante. Se conserva como referencia histórica.
- **Referencia web:** `teschi.teschi.edu.mx` — plataforma Servicio Social y Residencias Profesionales
- **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]], [[REGLAS_PROYECTO]]

---

## Diseño real (16/09/2026)

`DashboardScreen.kt` ya no tiene el grid de tarjetas de módulos — ahora es solo la pantalla general "Inicio" (header + hero de bienvenida). Los módulos (Reinscripción, Tira de Materias, Calificaciones, Intersemestral, Kardex) son destinos de una barra de navegación inferior flotante y persistente, `BottomNavBar.kt` (nueva), inspirada en un diseño de referencia que el alumno compartió (pill blanca flotante, ícono + etiqueta, verde para el tab activo).

- **6 tabs:** Inicio, Reinscripción, Tira de Materias (etiqueta "Materias"), Calificaciones ("Calif."), Intersemestral ("Intersem."), Kardex. Las etiquetas largas se abrevian para que las 6 quepan en una sola fila sin encimarse.
- **Navegación:** se usa el patrón estándar de Navigation-Compose para bottom nav (`popUpTo(Routes.DASHBOARD){ saveState = true }` + `launchSingleTop = true` + `restoreState = true`) — cambiar de tab nunca crece el back stack ni pierde el estado de scroll de un tab visitado antes.
- **Animación:** el `NavHost` de `MainActivity.kt` define `enterTransition`/`exitTransition` a nivel global — entre dos tabs de la barra, la transición desliza en la dirección real del cambio (según el orden de `TABS_DASHBOARD`); para cualquier otra navegación (login, admin, perfil) usa un fundido con escala. El ítem seleccionado de la barra también anima su color e ícono (ligero aumento de escala) al cambiar.
- Las pantallas de cada módulo (`ReinscripcionScreen`, `TiraMateriasScreen`, etc.) no se tocaron por dentro — conservan su propio `Scaffold`/`TopAppBar` con flecha de "regresar"; como ahora son tabs hermanos (no pantallas apiladas), esa flecha siempre termina en Inicio, nunca saca al alumno de la sesión.

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
| Intersemestral | `Routes.INTERSEMESTRAL` | Placeholder |

---

## Sesión de usuario

Tras OTP exitoso, `UserSession` guarda en memoria:
- `matricula` — validada contra SIIA
- `correoOtp` — correo donde se envió el código 2FA

Se limpia al tocar el pill de usuario (cerrar sesión).

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
