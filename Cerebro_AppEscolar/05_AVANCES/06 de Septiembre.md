# Bitácora de Avances — 06 de Septiembre de 2026

- **Relacionado con:** [[MAPA_PROYECTO]]

---

## Completado en Esta Sesión

- [x] **BUG-007 diagnosticado:** `Argument type mismatch: kotlin.Result<kotlin.Unit>` en `EmailOtpService` — causa era caché de AGP 8.13.2, no el código. Solución: Invalidate Caches en Android Studio.
- [x] **BUG-008 resuelto:** Fondo negro en pantallas cuando el celular usa modo oscuro del sistema. `Theme.kt` reescrito con único `lightColorScheme` institucional; `darkColorScheme` eliminado; `surfaceVariant` y `onSurfaceVariant` explícitos.
- [x] **Rutas admin añadidas al NavHost:** `Routes.ADMIN_DASHBOARD` y `Routes.ADMIN_PROFILE` con argumento `{matricula}`. Helper `Routes.adminProfile(matricula)`.
- [x] **Navegación post-login bifurcada:** `LoginScreen` navega a `ADMIN_DASHBOARD` si el usuario es administrador, o a `DASHBOARD` si es alumno — via `LaunchedEffect(uiState.irAlDashboard)`.
- [x] **`AdminDashboardScreen` reescrito:** Scaffold + TopAppBar verde institucional, fondo `LoginCanvas`, botón "Ver perfil" en cada fila de usuario, vista rápida de últimos 3 movimientos expandible, link a perfil completo.
- [x] **Registros completos en bóveda:** DEC-011/012/013, BUG-007/008, v0.6 en CAMBIOS_DE_CODIGO, MAPA_PROYECTO actualizado.

---

## Decisiones Técnicas de Esta Sesión

- **Un solo esquema de color (DEC-013):** Eliminado completamente el modo oscuro de Material3. La identidad institucional es fija. Los usuarios con tema oscuro en el sistema ven el tema claro — comportamiento correcto y esperado.
- **Navegación bifurcada (DEC-012):** Admin → `ADMIN_DASHBOARD`; Alumno → `DASHBOARD`. El flag `irAlDashboard` en `AuthUiState` controla el disparo desde el ViewModel.
- **Panel admin con rutas parametrizadas (DEC-011):** `admin_profile/{matricula}` permite navegar al perfil de cualquier usuario sin pasar objetos por estado compartido.

---

## Estado Actual de la App (6/09/2026)

| Funcionalidad | Estado |
|---|---|
| Login matrícula + contraseña SIIA | ✅ Funcional (scraping ASP.NET) |
| 2FA por correo (OTP) | ✅ Funcional (cualquier correo) |
| Dashboard alumno (4 módulos) | ✅ UI lista, módulos en placeholder |
| Panel administrador (directorio + CRUD) | ✅ Funcional |
| Vista perfil individual + auditoría | ✅ Funcional (requiere API local) |
| Login admin sin OTP (bypass dev) | ✅ matrícula `admin` / clave `admin` |
| Fondo negro en modo oscuro | ✅ Resuelto |
| Correo a dominio `@teschi.edu.mx` | 🟡 Pendiente SMTP institucional |
| Módulos 1-5 (Reinscripción, etc.) | 🔴 Placeholder — próxima prioridad |
| Auditoría GPS + IP | 🔴 Pendiente |

---

## Pendiente para Próxima Sesión

- [ ] Implementar el primer módulo funcional real (Calificaciones o Tira de Materias — consulta scraping SIIA)
- [ ] Conectar el `CookieJar` de `SiiaAuthService` con las pantallas de módulos del Dashboard
- [ ] Resolver BUG-005: recibir cuenta SMTP institucional de TI
- [ ] Exponer la API de auditoría con IP pública o VPN para pruebas fuera de la red local

---

## Sesión tarde — ReinscripcionScreen + DatePicker + sin equivalencias

### Completado

- [x] **BUG-012 resuelto:** `ReinscripcionScreen` existía desde el 03/09 pero estaba desconectada del NavHost. Conectada en `MainActivity.kt` con su import correcto.
- [x] **DatePicker nativo:** Campo de fecha de nacimiento en `RegistroScreen` reemplazado por `DatePickerDialog` de Android. Año por defecto: hoy − 18 años. No permite fechas futuras. Tap en ícono o en el campo abre el selector.
- [x] **Equivalencias eliminadas:** Checkbox, estado `equivalencias` y parámetro del ViewModel eliminados de `RegistroScreen` y `RegistroViewModel`. El payload JSON ya no envía ese campo.
- [x] **Bóveda actualizada:** BUG-012/013, v0.6.3 en CAMBIOS_DE_CODIGO con tabla de estado de todas las rutas, MAPA_PROYECTO, `01_Reinscripcion.md` con estado actual del módulo.

### Estado de rutas en MainActivity (cierre de día)

| Ruta | Pantalla real | Estado |
|---|---|---|
| `LOGIN` | `LoginScreen` | ✅ |
| `DASHBOARD` | `DashboardScreen` | ✅ |
| `ADMIN_DASHBOARD` | `AdminDashboardScreen` | ✅ |
| `ADMIN_PROFILE` | `AdminProfileScreen` | ✅ |
| `REGISTRO` | `RegistroScreen` | ✅ |
| `REINSCRIPCION` | `ReinscripcionScreen` | ✅ |
| `TIRA_MATERIAS` | placeholder | 🔴 |
| `CALIFICACIONES` | placeholder | 🔴 |
| `INTERSEMESTRAL` | placeholder | 🔴 |
| `RECUPERAR_PASS` | placeholder | 🔴 |

### Regla derivada (refuerzo)
Antes de reescribir `MainActivity.kt`, verificar TODAS las rutas en [[CAMBIOS_DE_CODIGO]] sección "Estado de rutas" y preservarlas. Este fue el tercer incidente del mismo tipo (BUG-009, BUG-012).
