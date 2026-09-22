# Registro de Cambios de Código

> Historial de modificaciones relevantes al código fuente.
> Para el detalle completo de cada sesión ver [[05_AVANCES/24 de Agosto]].
> **Relacionado con:** [[MAPA_PROYECTO]]

---

## v0.1 — 21/08/2026 · Estructura base

| Archivo | Cambio |
|---|---|
| `MainActivity.kt` | Login + 2FA + NavHost + Dashboard con 5 módulos placeholder |
| `Color.kt` | Paleta institucional verde TESCHI |
| `Theme.kt` | `AppTeschiTheme` con esquema claro/oscuro institucional |
| `build.gradle.kts` | Dependencias Compose, Navigation, Retrofit (comentado), GPS (comentado) |

---

## v0.2 — 24/08/2026 · Correcciones de build + OTP real

| Archivo | Cambio |
|---|---|
| `build.gradle.kts` | Fix `compileSdk`, `kotlin.jvmToolchain(11)`, BOM 2024.12.01, `packaging excludes` META-INF |
| `libs.versions.toml` | Añadidas entradas de navigation, icons, javamail |
| `gradle.properties` | Heap 4g, parallel, caching, `excludeLibraryComponentsFromConstraints` |
| `Color.kt` | Tokens completos + esquema oscuro |
| `Theme.kt` | Dynamic Color desactivado |
| `service/EmailOtpService.kt` | Creado — Gmail SMTP con JavaMail, plantilla HTML institucional |
| `local.properties` | Credenciales SMTP (excluidas de Git) |

---

## v0.3 — 26/08/2026 · UX + diagnóstico correo

| Archivo | Cambio |
|---|---|
| `MainActivity.kt` | Campo login → correo institucional completo (`KeyboardType.Email`) |
| `MainActivity.kt` | Link "¿Olvidaste tu contraseña?" entre contraseña y botón Continuar |
| `MainActivity.kt` | Botón "Cambiar correo" en paso 2FA para regresar al paso 1 |
| `MainActivity.kt` | Botón back (←) en `DashboardScreen` TopAppBar |
| `MainActivity.kt` | `DEV_MODE = true` reactivado para mostrar error SMTP exacto |
| `MainActivity.kt` | Recuperar Pass eliminado del Dashboard (acceso solo desde Login) |
| `service/EmailOtpService.kt` | Validaciones previas, logging detallado, mensajes de error legibles |

---

## v0.4 — 27/08/2026 · Login SIIA real + OTP a cualquier correo

| Archivo | Cambio |
|---|---|
| `service/SiiaAuthService.kt` | **Nuevo** — validación de credenciales contra portal ASP.NET |
| `network/InMemoryCookieJar.kt` | **Nuevo** — CookieJar en memoria RAM |
| `viewmodel/AuthViewModel.kt` | **Nuevo** — MVVM para flujo login + 2FA |
| `service/EmailOtpService.kt` | OTP a cualquier correo; fix `Result<Unit>` |
| `MainActivity.kt` | 3 campos: matrícula, contraseña SIIA, correo OTP |
| `app/build.gradle.kts` | OkHttp, Jsoup, ViewModel Compose activados |
| `gradle/libs.versions.toml` | Entradas okhttp + jsoup |
| `AndroidManifest.xml` | `usesCleartextTraffic=true` |

---

## v0.5 — 27/08/2026 · UI login web TESCHI + SQL Express

| Archivo | Cambio |
|---|---|
| `ui/login/LoginScreen.kt` | **Nuevo** — diseño blanco/verde como referencia web |
| `viewmodel/AuthViewModel.kt` | Flujo 2 pasos; DEV_MODE eliminado |
| `ui/theme/Color.kt` | Tokens login (`LoginGreenBright`, `LoginFieldBg`) |
| `database/sql/*.sql` | **Nuevo** — esquema SQL Server Express |
| `04_BASE_DATOS/Base_Datos_Usuarios.md` | Documentación BD en bóveda |

### Módulo Reinscripción — implementado en esta versión

| Archivo | Estado |
|---|---|
| `ui/modules/ReinscripcionScreen.kt` | **Creado** — 6 estados (LOADING/BLOCKED/FORM/SENDING/SUCCESS/ERROR), file picker para comprobante, folio UUID local, registro en AuditTrail |
| `MainActivity.kt` | ⚠️ Ruta no conectada hasta v0.6.3 (ver BUG-012) |

- [ ] `AuthViewModel` — ~~lógica de login contra SIIA~~ ✅ hecho 27/08/2026
- [ ] `CookieJar` — ~~implementar cliente OkHttp~~ ✅ hecho; falta reutilizar en módulos del Dashboard
- [ ] Módulos 1-5 — implementar pantallas reales (actualmente placeholders)
- [ ] Auditoría — captura GPS + IP al verificar OTP
- [ ] Resolver BUG-005 correo institucional (ver [[BUGS_Y_ERRORES]])

---

## v0.6 — 06/09/2026 · Panel admin completo + fondo negro + rutas admin

### Problema detectado en sesión
Build fallaba con `Argument type mismatch: kotlin.Result<kotlin.Unit>` — causa: caché AGP. Sin cambio de código; solución: Invalidate Caches en Android Studio.

### Cambios aplicados

| Archivo | Tipo | Cambio |
|---|---|---|
| `ui/theme/Theme.kt` | Modificado | Eliminado `darkColorScheme` completo. Único esquema `InstitutionalColorScheme` (lightColorScheme). `surfaceVariant` y `onSurfaceVariant` explícitos con `BackgroundLight`/`LoginInk`. Parámetro `darkTheme` suprimido (`@Suppress`). Fix definitivo del fondo negro. |
| `ui/theme/Color.kt` | Modificado | Eliminado `InstitutionalDarkColorScheme`. Comentario actualizado: esquema oscuro solo como referencia de API. |
| `MainActivity.kt` | Modificado | Añadidas rutas `ADMIN_DASHBOARD` y `ADMIN_PROFILE = "admin_profile/{matricula}"`. Añadido helper `Routes.adminProfile(matricula)`. `composable` con `navArgument("matricula")` para pasar la matrícula como parámetro de ruta. Imports de `AdminDashboardScreen` y `AdminProfileScreen`. |
| `ui/login/LoginScreen.kt` | Modificado | Añadido `LaunchedEffect(uiState.irAlDashboard)` — navega a `ADMIN_DASHBOARD` si `UserSession.esAdministrador`, o `DASHBOARD` si es alumno. Elimina la duplicación anterior donde siempre iba al dashboard de alumno. |
| `ui/dashboard/AdminDashboardScreen.kt` | Modificado | Reescrito completo: `Scaffold` con `TopAppBar` verde institucional. Fondo `LoginCanvas` en toda la pantalla. Botón "Ver perfil" en cada `UserRow` que navega a `admin_profile/{matricula}`. Vista rápida de últimos 3 movimientos expandible en la fila. Link "ver más" si hay más de 3. Import explícito de `RemoteAuditEvent` desde `service`. |

### Lo que puede hacer el administrador ahora

1. **Login** con matrícula `admin` + contraseña `admin` → bypass sin OTP (desarrollo)
2. **`AdminDashboardScreen`** → ve todos los usuarios de la BD (API) o locales (fallback)
3. **Tap en "Ver perfil"** → navega a `AdminProfileScreen` con la matrícula del usuario
4. **`AdminProfileScreen`** → muestra datos completos del perfil + **todos** sus movimientos de auditoría (remotos via `AdminAuditService`)
5. **Filtros de riesgo** (Normal / Atención / Crítico) en el log general
6. **CRUD** de usuarios desde el panel (registrar, editar, eliminar)

---

## Pendientes actualizados (06/09/2026)

- [ ] `AdminProfileScreen` — conectar a SQL Server cuando esté expuesto via API (actualmente usa `http://192.168.0.41:4000`)
- [ ] Módulos 1-5 del alumno — implementar pantallas reales
- [ ] Auditoría GPS + IP al verificar OTP
- [ ] Resolver BUG-005 correo institucional cuando TI entregue SMTP
- [ ] Reutilizar `CookieJar` de `SiiaAuthService` en los módulos del Dashboard

---

## Hotfix v0.6.1 — 06/09/2026 · Restaurar ruta de Registro

| Archivo | Cambio |
|---|---|
| `MainActivity.kt` | Import añadido: `RegistroScreen`. Ruta `Routes.REGISTRO` restaurada a `RegistroScreen(navController)` en lugar del placeholder. |

**Causa del bug:** Reescritura completa de `MainActivity.kt` en v0.6 reemplazó la ruta existente con un placeholder. Ver BUG-009.

---

## Hotfix v0.6.2 — 06/09/2026 · Teclado matrícula + diagnóstico registro

| Archivo | Cambio |
|---|---|
| `ui/login/LoginScreen.kt` | `keyboardType = KeyboardType.Number` → `KeyboardType.Text` en campo Matrícula. Placeholder actualizado. |
| `viewmodel/RegistroViewModel.kt` | Reescrito: `OkHttpClient` con timeouts explícitos (15s). `catch` específico para `ConnectException`, `SocketTimeoutException` y JSON inválido. Mensajes de error detallados en pantalla. `Log.d/e` con TAG para diagnóstico en Logcat. |

---

## Hotfix v0.6.3 — 06/09/2026 · ReinscripcionScreen + DatePicker + sin equivalencias

| Archivo | Tipo | Cambio |
|---|---|---|
| `MainActivity.kt` | Modificado | Import añadido: `ReinscripcionScreen`. Ruta `Routes.REINSCRIPCION` → `ReinscripcionScreen(navController)` (ya no placeholder). Mismo patrón que BUG-009. |
| `ui/login/RegistroScreen.kt` | Reescrito | `DatePickerDialog` nativo reemplaza el campo de texto manual. Año default = hoy − 18 años. `maxDate` = fecha actual (no permite fechas futuras). Campo `readOnly` con ícono `CalendarMonth`; tap en el campo también abre el picker. Eliminado: import `Checkbox`, estado `equivalencias`, composable `RowWithCheckbox`. |
| `viewmodel/RegistroViewModel.kt` | Modificado | Eliminado parámetro `equivalencias: Boolean` de `registrar()` y `sendRegistration()`. Eliminado `put("equivalencias", equivalencias)` del payload JSON enviado a la API. |

### Estado de los módulos en MainActivity tras este hotfix

| Ruta | Composable | Estado |
|---|---|---|
| `Routes.LOGIN` | `LoginScreen` | ✅ Real |
| `Routes.DASHBOARD` | `DashboardScreen` | ✅ Real |
| `Routes.ADMIN_DASHBOARD` | `AdminDashboardScreen` | ✅ Real |
| `Routes.ADMIN_PROFILE` | `AdminProfileScreen` | ✅ Real |
| `Routes.REGISTRO` | `RegistroScreen` | ✅ Real |
| `Routes.REINSCRIPCION` | `ReinscripcionScreen` | ✅ Real |
| `Routes.TIRA_MATERIAS` | `ModuleDetailScreen` | 🔴 Placeholder |
| `Routes.CALIFICACIONES` | `ModuleDetailScreen` | 🔴 Placeholder |
| `Routes.INTERSEMESTRAL` | `ModuleDetailScreen` | 🔴 Placeholder |
| `Routes.RECUPERAR_PASS` | `ModuleDetailScreen` | 🔴 Placeholder |

---

## v0.7 — 08/09/2026 · Normalización de la base de datos (esquema V2)

Ver [[DECISIONES_TECNICAS]] DEC-014 y [[Base_Datos_Usuarios]] para el detalle completo del rediseño (ER, cardinalidad, justificación de cada tabla).

### Cambios aplicados

| Archivo | Tipo | Cambio |
|---|---|---|
| `database/sql/03_MigracionV2_Normalizacion.sql` | Nuevo | Migración completa: crea `Alumnos`, `AlumnoCredenciales`, catálogos `CatalogoPeriodos`/`CatalogoEstatusMateria`/`CatalogoTurnos`, dominio académico `PlanEstudioMaterias`/`Grupos`/`HistorialAcademico` (+ vista `vw_HistorialAcademico`); migra datos de `Usuarios`+`CuentasRegistro` a `Alumnos` (fusiona la matrícula duplicada `20240001`); renombra las tablas viejas a `_ObsoletoV1_*` en vez de borrarlas |
| `database/sql/02_ProcedimientosUsuarios.sql` | Reescrito | `sp_UpsertUsuario`→`sp_UpsertAlumno`; los 4 SPs actualizados para usar `Alumnos`/`IdAlumno` en vez de `Usuarios`/`Matricula` redundante |
| `database/sql/01_CrearBaseDatos.sql` | Modificado | Comentario de cabecera aclarando que es el esquema V1 histórico, superado por 03 |
| `07_BACKEND/AppTeschi.Api/server.js` | Modificado | `/api/registro`, `/api/auth/cuenta`, `/api/usuarios/sync`, `/api/usuarios` reescritos para leer/escribir `Alumnos`+`AlumnoCredenciales`; `/api/usuarios/sync` ahora mapea la carrera de texto libre del SIIA al catálogo (ignorando acentos); `/api/usuarios` mantiene los nombres de campo `IdUsuario`/`Carrera` en el JSON de salida para no romper `AdminUsersService.kt` |

### Bug encontrado y corregido de paso

`CatalogoCarreras.Nombre` tenía los acentos corruptos en la base de datos real (ejecución previa de `01_CrearBaseDatos.sql` con codificación incorrecta) — afectaba el nombre de carrera que ve el alumno en el registro. Corregido directamente en los datos y con una detección/corrección automática agregada a la migración por si vuelve a pasar.

### Verificación

- Migración probada de punta a punta contra la base de datos real de desarrollo (15 alumnos migrados, 7 con credenciales, 53 materias y 20 grupos sembrados — cifras verificadas contra el Kardex/Tira reales)
- Backend reiniciado y probado en vivo: registro nuevo, login correcto, login con contraseña incorrecta (rechazado), registro duplicado (rechazado 409), sincronización de perfil SIIA para alumno con y sin cuenta local, listado de alumnos, auditoría — todo contra el esquema nuevo
- Sin cambios en la app Android: el contrato JSON de los endpoints no cambió
- Diagrama entidad-relación generado y verificado visualmente en SQL Server Management Studio (Database Diagrams) sobre `AppTeschiDB`, confirmando las cardinalidades documentadas

---

## v0.8 — 08/09/2026 · Plan de estudios de Animación Digital y Efectos Visuales

| Archivo | Tipo | Cambio |
|---|---|---|
| `database/sql/04_SeedMateriasAnimacion.sql` | Nuevo | Siembra las 48 materias reales del plan de estudios de Ingeniería en Animación Digital y Efectos Visuales en `PlanEstudioMaterias` (235 créditos totales) — mismo esquema que ya usaba ISC, sin ningún cambio de tabla; demuestra que el diseño soporta agregar el plan completo de otra carrera sin tocar relaciones |

### Verificación

- Ejecutado contra la base de datos real: 48 materias insertadas, créditos por semestre verificados uno por uno contra el plan de estudios proporcionado
- Confirmada la idempotencia (reejecutar el script no duplica filas)

---

## v0.9 — 08/09/2026 · Grupos reales de Animación Digital y Efectos Visuales

| Archivo | Tipo | Cambio |
|---|---|---|
| `database/sql/05_SeedGruposAnimacion.sql` | Nuevo | Siembra los 21 grupos reales de Animación en `Grupos`, extraídos de "Horarios Animacion.pdf" (Asignación de Horario 2026-2) con `pdftotext` |

### Dato importante encontrado

La clave real que usa el SIIA para esta carrera en los códigos de grupo es **`ADYEV`** (ej. `1ADYEV11`), no `ANIMACION` — se guardó tal cual viene en el documento oficial, sin traducirla ni inventar un código propio. El turno (matutino/vespertino) se confirmó cruzando cada grupo contra su franja de horario real en el PDF, igual que se hizo para los grupos de ISC.

No existe grupo de 9° semestre en el documento (igual que ISC: "Motion Graphics Avanzado" no tiene horario de aula regular asignado).

### Verificación

- Ejecutado contra la base de datos real: 21 grupos insertados, turno y semestre verificados uno por uno contra el PDF
- Confirmada la idempotencia

---

## v0.10 — 08/09/2026 · Grupos reales de Ingeniería Industrial

| Archivo | Tipo | Cambio |
|---|---|---|
| `database/sql/06_SeedGruposIndustrial.sql` | Nuevo | Siembra los 16 grupos reales de Ingeniería Industrial en `Grupos`, extraídos de "Horarios_Industrial.pdf" (HORARIOS IIND 2026-2) con `pdftotext` |

### Datos importantes encontrados

- Clave real del SIIA para esta carrera: **`II`** (ej. `1II11`), confirmada además contra los códigos de materia visibles en el mismo documento (`ACC-0906`, etc.), que coinciden con el plan de estudios oficial TecNM IIND-2010-227 que compartió el alumno.
- A diferencia de ISC y Animación, Industrial **sí tiene grupos de 9° semestre** (`9II21`, `9II22`) — el documento muestra clases de aula regular ahí (p. ej. Ingeniería de Calidad), no solo residencia profesional.
- **Plan de estudios (`PlanEstudioMaterias`) pendiente**: el alumno compartió los nombres de las 48 materias y el mapa curricular oficial TecNM (que sí trae créditos por materia), pero indicó explícitamente que todavía no tiene confirmados los créditos — se sembrarán en un script aparte cuando los confirme, para no meter un dato no verificado por él directamente.

### Verificación

- Ejecutado contra la base de datos real: 16 grupos insertados, turno y semestre verificados uno por uno contra el PDF
- Confirmada la idempotencia

---

## v0.11 — 08/09/2026 · Plan de estudios de Ingeniería Industrial (parcial)

| Archivo | Tipo | Cambio |
|---|---|---|
| `database/sql/07_SeedMateriasIndustrial.sql` | Nuevo | Siembra 47 de las 53 materias de Ingeniería Industrial en `PlanEstudioMaterias` (212 créditos), transcritas del mapa curricular oficial TecNM IIND-2010-227 que compartió el alumno |

### Por qué quedó parcial

6 materias de 8°/9° semestre (Productividad Humana, Temas Selectos de Ingeniería Industrial, Medición y mejoramiento de la Productividad, Gestión de los Sistemas de Calidad Aplicados, Productividad Aplicada, Ingeniería de Calidad) no se pudieron leer con confianza en esa esquina de la imagen del mapa curricular. Se le mostró al alumno la tabla completa de lo que sí se pudo confirmar antes de guardar nada, y decidió dejarlo así por ahora en vez de reintentar la lectura — quedan documentadas en [[Base_Datos_Usuarios]] para agregarlas después.

### Verificación

- Ejecutado contra la base de datos real: 47 materias insertadas, créditos por semestre verificados
- Confirmada la idempotencia

---

## v0.12 — 08/09/2026 · Plan de estudios y grupos de Ingeniería Mecatrónica

| Archivo | Tipo | Cambio |
|---|---|---|
| `database/sql/08_SeedMateriasMecatronica.sql` | Nuevo | Siembra las 52 materias de Ingeniería Mecatrónica en `PlanEstudioMaterias` (260 créditos) |
| `database/sql/09_SeedGruposMecatronica.sql` | Nuevo | Siembra los 14 grupos reales en `Grupos`, extraídos de "Horario Mecatronica grupos 26-2.pdf" con `pdftotext` |

### Validación de esta carga

A diferencia de Industrial, aquí el alumno sí proporcionó créditos por materia en la lista de nombres — se sumaron los 52 valores y dieron **exactamente 260**, coincidiendo con el total institucional oficial de la retícula IMCT-2010-229 que también compartió (Genérica 210 + Especialidad 25 + Residencias 10 + Servicio Social 10 + Complementarias 5). Esa coincidencia exacta confirma que la lista está completa y correcta, sin necesidad de transcribir la retícula (que además es la variante "Especialidad: Robótica", no el plan genérico).

Clave real del SIIA para grupos: **`IM`** (ej. `1IM11`). Igual que Industrial, sí tiene grupo de 9° semestre (`9IM21`).

### Verificación

- Ejecutado contra la base de datos real: 52 materias (260 créditos) y 14 grupos insertados, verificados por semestre
- Confirmada la idempotencia en ambos scripts

---

## v0.13 — 08/09/2026 · Plan de estudios y grupos de Ingeniería Química

| Archivo | Tipo | Cambio |
|---|---|---|
| `database/sql/10_SeedMateriasQuimica.sql` | Nuevo | Siembra las 52 materias de Ingeniería Química en `PlanEstudioMaterias` (260 créditos) |
| `database/sql/11_SeedGruposQuimica.sql` | Nuevo | Siembra los 13 grupos reales en `Grupos`, extraídos de "Horarios Quimica.pdf" |

### Cómo se leyó el PDF (caso especial: sin capa de texto)

A diferencia de los horarios anteriores, "Horarios Quimica.pdf" es un escaneo sin texto extraíble — `pdftotext` devolvió 0 líneas. Se instaló `pymupdf` (`pip install --user pymupdf`) para renderizar cada una de las 14 páginas como imagen y leerlas visualmente. Cada página trae, además del horario, una tabla oficial "CLAVE / ASIGNATURA / HORAS" por grupo — se usó esa tabla como fuente de créditos en vez de la retícula (que es densa y con la letra pequeña, mayor riesgo de error de lectura). Cada valor de crédito viene impreso explícitamente; la suma de los 52 dio **260**, coincidiendo exactamente con el total institucional oficial.

Las materias que el alumno listó como "Especialidad I" a "Especialidad VI" resultaron tener nombres reales propios en los horarios (ej. "Especialidad I" = Microbiología Ambiental) — se sembraron con el nombre genérico que dio el alumno, para no inventar una asociación materia-nombre no confirmada por él, pero con el crédito real de cada una.

Clave real del SIIA para grupos: **`IQ`** (ej. `1IQ11`). Igual que ISC y Animación, no tiene grupo de 9° semestre con horario de aula regular.

### Verificación

- Ejecutado contra la base de datos real: 52 materias (260 créditos) y 13 grupos insertados, verificados por semestre
- Confirmada la idempotencia en ambos scripts

---

## v0.14 — 08/09/2026 · Plan de estudios y grupos de Licenciatura en Administración

| Archivo | Tipo | Cambio |
|---|---|---|
| `database/sql/12_SeedMateriasAdministracion.sql` | Nuevo | Siembra las 54 materias de Licenciatura en Administración en `PlanEstudioMaterias` (260 créditos) |
| `database/sql/13_SeedGruposAdministracion.sql` | Nuevo | Siembra los 26 grupos reales en `Grupos`, extraídos de "HORARIOS Administracion 2026-2.pdf" con `pdftotext` |

### Validación de esta carga

El alumno proporcionó créditos por materia en la lista de nombres. Se sumaron por semestre y **cada uno de los 9 totales coincidió exactamente** con el total de créditos por columna impreso al pie de la retícula oficial LADM-2010-234 (ej. semestre 1 = 27, semestre 9 = 33), y la suma general dio 260. Es la validación más completa lograda hasta ahora — permite alta confianza sin necesidad de leer celda por celda la retícula.

Clave real del SIIA para grupos: **`LA`** (ej. `1LA11`). Es la carrera con más grupos de 9° semestre encontrada hasta ahora: `9LA21`, `9LA22`, `9LA23` (los tres vespertinos).

### Verificación

- Ejecutado contra la base de datos real: 54 materias (260 créditos) y 26 grupos insertados, verificados por semestre contra los totales oficiales
- Confirmada la idempotencia en ambos scripts

---

## v0.15 — 08/09/2026 · Plan de estudios y grupos de Licenciatura en Gastronomía

| Archivo | Tipo | Cambio |
|---|---|---|
| `database/sql/14_SeedMateriasGastronomia.sql` | Nuevo | Siembra las 53 materias de Licenciatura en Gastronomía en `PlanEstudioMaterias` (260 créditos) |
| `database/sql/15_SeedGruposGastronomia.sql` | Nuevo | Siembra los 28 grupos reales en `Grupos`, extraídos de "HORARIOS DE GRUPOS GASTRONOMIA 2026-2.pdf" |

### Ajuste hecho sobre la lista del alumno

La lista de materias por semestre que compartió el alumno no incluía "Servicio Social" en ningún semestre (sí incluía Residencias Profesionales y Actividades Complementarias). Sumando únicamente lo que él dio se llega a 250 créditos; la retícula oficial GAST-2010-215 sí marca "Servicio Social 10" como categoría del plan, y 250 + 10 = 260, el total institucional exacto. Se agregó como materia de 8° semestre (mismo lugar que en ISC/Química/Mecatrónica) para no dejar la carrera 10 créditos por debajo del total oficial.

Clave real del SIIA para grupos: **`LG`** (ej. `1LG11`). Es la carrera con más grupos totales encontrada hasta ahora (28) y no tiene grupo de 9° semestre con horario regular.

### Verificación

- Ejecutado contra la base de datos real: 53 materias (260 créditos) y 28 grupos insertados
- Confirmada la idempotencia en ambos scripts

---

## v0.16 — 08/09/2026 · Módulo "Plan de Estudios" — el catálogo académico ya se ve en la app

Ver [[DECISIONES_TECNICAS]] DEC-015 para el detalle completo.

### Cambios aplicados

| Archivo | Tipo | Cambio |
|---|---|---|
| `07_BACKEND/AppTeschi.Api/server.js` | Modificado | Endpoints nuevos `GET /api/plan-estudios/:clave` y `GET /api/grupos/:clave` (públicos, sin API key) |
| `core/config/ApiConfig.kt` | Modificado | Helpers `planEstudios(clave)`, `grupos(clave)` y `catalogosRegistro` |
| `data/PlanEstudiosRemoto.kt` | Nuevo | `CarreraCatalogo`, `GrupoInfo` |
| `service/PlanEstudiosService.kt` | Nuevo | Interfaz `PlanEstudiosApi` + implementación real (OkHttp) |
| `viewmodel/PlanEstudiosViewModel.kt` | Nuevo | Carga carreras al iniciar, selecciona la primera, permite cambiar de carrera |
| `ui/modules/PlanEstudiosScreen.kt` | Nuevo | Selector de carrera + lista de materias agrupadas por semestre, con los grupos reales de cada semestre |
| `MainActivity.kt` | Modificado | Ruta `Routes.PLAN_ESTUDIOS` |
| `ui/dashboard/DashboardScreen.kt` | Modificado | Tarjeta nueva "Plan de Estudios" (6ª tarjeta, grid sigue en 3 filas × 2 columnas) |
| `testutil/PlanEstudiosFakes.kt`, `viewmodel/PlanEstudiosViewModelTest.kt` | Nuevo | Pruebas con fake sin red real |

### Verificación

- Backend reiniciado y probado en vivo: `/api/plan-estudios/ISC` (53 materias), `/api/plan-estudios/ANIMACION` (48 materias), carrera inexistente devuelve lista vacía sin error
- 63/63 pruebas unitarias pasando
- APK compilado y enviado; túnel de Cloudflare verificado sirviendo los endpoints nuevos antes de enviar

---

## v0.17 — 08/09/2026 · El catálogo académico se movió al lado del administrador

Ver [[DECISIONES_TECNICAS]] DEC-016. El alumno ya sabe su propia carrera — el módulo de v0.16 no era lo que hacía falta; se corrigió hacia el caso de uso real: que el administrador vea, al revisar a un alumno, qué materias le corresponden.

### Cambios aplicados

| Archivo | Tipo | Cambio |
|---|---|---|
| `database/sql/16_AgregarSemestreAlumnos.sql` | Nuevo | `ALTER TABLE Alumnos ADD Semestre TINYINT NULL` + `CHECK` 1-12 |
| `07_BACKEND/AppTeschi.Api/server.js` | Modificado | `/api/usuarios` incluye `ClaveCarrera`/`Semestre`; nuevo `PUT /api/usuarios/:matricula/semestre` (admin-only, con auditoría) |
| `data/AdminDirectory.kt` | Modificado | `ManagedUser` +`claveCarrera`, +`semestre` |
| `service/AdminUsersService.kt` | Modificado | `actualizarSemestre(matricula, semestre)` |
| `ui/dashboard/AdminProfileScreen.kt` | Modificado | Sección nueva "Carga académica esperada" (`CargaAcademicaCard`): asignar semestre + ver materias esperadas de `PlanEstudioMaterias` filtradas por carrera+semestre |
| `ui/dashboard/DashboardScreen.kt`, `MainActivity.kt` | Modificado | Se retiró la tarjeta/ruta "Plan de Estudios" del lado del alumno |
| `ui/modules/PlanEstudiosScreen.kt`, `viewmodel/PlanEstudiosViewModel.kt`, sus pruebas | Eliminado | Ya no aplican — `PlanEstudiosService`/`PlanEstudiosApi` se conservan porque los reutiliza `AdminProfileScreen` |

### Verificación

- Backend probado en vivo: `PUT .../semestre` con valor válido (200), inválido fuera de 1-12 (400), matrícula inexistente (404), sin API key (401)
- 60/60 pruebas unitarias pasando (AdminProfileScreen no usa ViewModel, igual que el resto del panel de administrador — no se agregaron pruebas nuevas ahí, mismo criterio que ya aplicaba a esa pantalla)
- APK compilado y enviado; túnel verificado antes de mandarlo

---

## v0.18 — 09/09/2026 · Panel de administrador: CRUD real, calificaciones, auditoría enlazada y estadísticas

Ver [[DECISIONES_TECNICAS]] DEC-017. Se reemplazó el CRUD local (`AdminDirectory`, que nunca tocaba SQL Server) por operaciones reales, y se dividió el panel en 4 apartados independientes.

### Cambios aplicados

| Archivo | Tipo | Cambio |
|---|---|---|
| `07_BACKEND/AppTeschi.Api/server.js` | Modificado | `writeAudit(...)` ahora enlaza `IdAlumno`; `GET /api/auditoria` acepta `?matricula=`/`?limit=`; nuevos `POST /api/usuarios`, `PUT /api/usuarios/:matricula` (edición parcial), `DELETE /api/usuarios/:matricula` (baja en cascada), `GET/PUT /api/calificaciones/:matricula[/:idMateria]`, `GET /api/estadisticas` |
| `core/config/ApiConfig.kt` | Modificado | `estadisticas`, `calificaciones(matricula)`, `calificacion(matricula, idMateria)` |
| `data/ManagedUser.kt` | Nuevo | `ManagedUser`/`UserRole` — movidos fuera de `AdminDirectory.kt` antes de borrarlo |
| `data/AdminDirectory.kt`, `data/PendingUserSync.kt` | Eliminado | CRUD local en `SharedPreferences` + cola de sincronización offline, sin uso real desde que hay CRUD real |
| `service/AdminUsersService.kt` | Modificado | `crear()`, `actualizarPerfil(matricula, cambios)`, `eliminar()` |
| `service/CalificacionesAdminService.kt` | Nuevo | `obtener(matricula)`, `actualizar(matricula, idMateria, calificacion)` |
| `service/EstadisticasService.kt` | Nuevo | `obtener()` — agregados para las gráficas |
| `ui/dashboard/AdminDashboardScreen.kt` | Reescrito | Pasa de pantalla única a *hub* con 4 tarjetas de navegación; conserva sesión activa, permiso de ubicación y log de auditoría local del dispositivo |
| `ui/dashboard/AdminAlumnosScreen.kt` | Nuevo | Alta/edición/baja de alumnos (con diálogo de confirmación para eliminar), búsqueda, selector de carrera |
| `ui/dashboard/AdminCalificacionesScreen.kt` | Nuevo | Elegir alumno → materias agrupadas por semestre con calificación editable |
| `ui/dashboard/AdminAuditoriaScreen.kt` | Nuevo | Historial completo de movimientos, filtrable por matrícula |
| `ui/dashboard/AdminEstadisticasScreen.kt` | Nuevo | Gráficas de barras (alumnos por carrera/semestre, altas recientes, calificaciones) — barras de progreso proporcionales, sin librería externa |
| `MainActivity.kt` | Modificado | Rutas `ADMIN_ALUMNOS`, `ADMIN_CALIFICACIONES`, `ADMIN_AUDITORIA`, `ADMIN_ESTADISTICAS`; se quitó la inicialización de `AdminDirectory`/`PendingUserSync` |

### Verificación

- Backend: 20 casos probados en vivo con `curl` antes de tocar la app — alta (201), duplicado (409), carrera inválida (400), edición parcial (200), sin campos (400), matrícula inexistente (404), calificación fuera de rango (400), calificación válida con estatus derivado automáticamente, baja con limpieza en cascada confirmada en `HistorialAcademico`, auditoría filtrada por matrícula (incluida la del alumno ya eliminado, con `IdAlumno` liberado), estadísticas con datos reales
- 60/60 pruebas unitarias pasando
- APK compilado y enviado; túnel de Cloudflare caído al reanudar la sesión — se relanzó con un binario `cloudflared.exe` disponible localmente, se verificó `/health` en la URL nueva y se actualizó `local.properties` antes de compilar

---

## v0.19 — 09/09/2026 · Perfil del alumno: historial en vez de carga esperada, semestre solo hacia adelante, baja

Ver [[DECISIONES_TECNICAS]] DEC-018. Ajuste directo tras probar v0.18: la "carga académica esperada" no era lo que hacía falta ver en el perfil.

### Cambios aplicados

| Archivo | Tipo | Cambio |
|---|---|---|
| `07_BACKEND/AppTeschi.Api/server.js` | Modificado | `PUT /api/usuarios/:matricula` rechaza (400) bajar o repetir el semestre actual cuando ya tiene uno asignado |
| `ui/dashboard/AdminProfileScreen.kt` | Reescrito | Se quitó `CargaAcademicaCard`; se agregaron `SemestreCard` (solo semestres superiores), `HistorialMateriasCard` (calificaciones reales agrupadas por semestre) y `BajaCard` (activar/desactivar con confirmación) |

### Verificación

- Backend probado en vivo: crear alumno en semestre 4, intentar bajar a 2 (400), intentar quedarse en 4 (400 — no repite), avanzar a 6 (200)
- 60/60 pruebas unitarias pasando
- Se sembraron 5 alumnos de prueba (`2024PRUEBA1`-`5`, distintas carreras y semestres) con calificaciones realistas en sus materias ya cursadas, para probar la vista con datos representativos

---

## v0.20 — 09/09/2026 · Limpieza de espacio: artefactos de build y código muerto

A petición del usuario, para liberar espacio del proyecto y de la bóveda.

### Cambios aplicados

| Elemento | Tipo | Detalle |
|---|---|---|
| `app/build/`, `build/reports/` (AppTeschi) | Eliminado | Artefactos de compilación/pruebas — regenerables con `./gradlew build`/`test` (~133 MB) |
| `.gradle/` (AppTeschi, caché local del proyecto) | Eliminado | Regenerable automáticamente en el siguiente `./gradlew` (~8.5 MB) — no es el caché global `~/.gradle`, ese no se tocó |
| `core/config/SupabaseClientProvider.kt` | Eliminado | Sin ninguna referencia en el resto del código ni mención en `DECISIONES_TECNICAS.md` — a diferencia de `AppError.kt` (que sí documenta explícitamente ser infraestructura para una "Fase B" futura), esto no tenía ningún rastro de intención de uso |
| `libs.supabase.*`, `libs.ktor.client.android`, `libs.android.desugar.jdk.libs` y su `coreLibraryDesugaring` | Eliminado | Dependencias y configuración de compilación que solo existían para soportar Supabase; sin Supabase, tampoco hacía falta el desugaring que forzaba |
| `.opencode/node_modules/` (bóveda) | Eliminado | Dependencias de una herramienta de IA distinta apuntada a la bóveda, sin relación con la documentación — regenerable con `npm install` en `.opencode/` si se vuelve a usar esa herramienta (~61 MB) |
| `.copilot/model-catalog-cache.json` (bóveda) | Eliminado | Caché, regenerable (~4.4 MB) |

### Deliberadamente NO tocado

- `07_BACKEND/AppTeschi.Api/node_modules` — en uso activo por el backend corriendo.
- `AppError.kt` — infraestructura documentada para una fase futura, no código muerto.
- `.obsidian/`, `.claude/`, `.agents/`, `.kiro/`, `copilot/` (bóveda) — configuración activa de herramientas de IA/Obsidian; tamaño insignificante.
- `.idea/`, `.kotlin/` (AppTeschi) — estado del IDE, tamaño insignificante.

### Verificación

- `./gradlew.bat :app:compileDebugKotlin` sin errores tras quitar Supabase
- 60/60 pruebas unitarias pasando
- ~207 MB liberados en total (133 MB proyecto Android + ~65 MB bóveda)

---

## v0.21 — 09/09/2026 · Seguridad: cuentas de administrador reales, OTP y correo en el servidor

Ver [[DECISIONES_TECNICAS]] DEC-019. A petición del usuario de llevar la app a estándar universitario — auditoría de seguridad que encontró que el bypass de administrador no distinguía debug/release, que no existía ninguna cuenta de administrador real, y que el OTP (código y envío de correo con credenciales SMTP reales) vivía enteramente en el cliente.

### Cambios aplicados

| Archivo | Tipo | Cambio |
|---|---|---|
| `database/sql/17_TablaAdministradores.sql` | Nuevo | `Administradores` + `AdministradorCredenciales` |
| `database/sql/18_OtpHistorialParaAdministradores.sql` | Nuevo | `OtpHistorial` admite administradores (`IdAdministrador` nullable + `CHECK`) y cuenta `Intentos` |
| `07_BACKEND/AppTeschi.Api/scripts/crear_administrador.js` | Nuevo | CLI para crear/resetear administradores con contraseña aleatoria generada, nunca inventada |
| `07_BACKEND/AppTeschi.Api/server.js` | Modificado | `POST /api/auth/administrador`, `POST /api/otp/enviar`, `POST /api/otp/verificar`; límite de intentos en memoria en los 4 endpoints de autenticación/OTP; envío de correo con `nodemailer` usando las variables `EMAIL_*` del propio backend |
| `07_BACKEND/AppTeschi.Api/.env`, `.env.example` | Modificado | Variables `EMAIL_SENDER`/`EMAIL_APP_PASSWORD`/`EMAIL_SMTP_HOST`/`EMAIL_SMTP_PORT` (migradas desde `local.properties` de la app) |
| `service/AdminAccountAuthService.kt` | Nuevo | Login de administrador real contra el backend |
| `service/OtpService.kt` | Nuevo | Envío/verificación de OTP remotos — reemplaza a `EmailOtpService.kt` (eliminado) |
| `service/LocalAccountAuthService.kt` | Modificado | `Account` ya no carga `esAdministrador` (siempre era `false`; los administradores tienen su propio servicio) |
| `data/repository/AuthRepository.kt` | Reescrito | Cascada: bypasses de debug (ahora **todos**, incluido el de administrador, protegidos por `BuildConfig.DEBUG`) → administrador real → cuenta de alumno → SIIA |
| `viewmodel/AuthViewModel.kt` | Reescrito | Ya no genera ni guarda el código OTP ni calcula su expiración — solo refleja la respuesta del backend; nuevo estado `tipoPendiente` (ALUMNO/ADMINISTRADOR) |
| `ui/login/LoginScreen.kt` | Modificado | El administrador que entra por OTP ahora sí navega a `ADMIN_DASHBOARD` (antes solo el bypass llegaba ahí); panel de intentos fallidos ya no está detrás de una bandera muerta |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | Modificado | Se quitó la dependencia `javamail-android` y los `buildConfigField` de `EMAIL_*` |
| `local.properties` | Modificado | Ya no contiene credenciales SMTP |
| Pruebas (`AuthFakes.kt`, `AuthRepositoryTest.kt`, `AuthViewModelTest.kt`) | Reescrito | Nuevos fakes (`FakeAdminAuthService`, `FakeRemoteOtpService`); las pruebas de OTP ya no simulan un reloj local — verifican que el ViewModel reacciona bien a lo que el backend (fake) responde |

### Verificación

- Backend probado en vivo (local y a través del túnel público): login de administrador válido/inválido/inexistente, envío de OTP, código correcto, código incorrecto con contador de intentos restantes, 5 intentos fallidos → bloqueo real (`429`), límite de 5 envíos de OTP por 10 minutos
- 63/63 pruebas unitarias pasando
- `./gradlew.bat :app:compileDebugKotlin` y `:app:assembleDebug` sin errores
- Primera cuenta real de administrador creada (`admin`, rol `SUPERADMIN`) — contraseña entregada una sola vez al usuario fuera del código

---

## v0.22 — 09/09/2026 · Sesiones JWT y permisos reales por rol (SUPERADMIN/OPERADOR)

Ver [[DECISIONES_TECNICAS]] DEC-020. Cierra los dos pendientes que quedaron abiertos en v0.21.

### Cambios aplicados

| Archivo | Tipo | Cambio |
|---|---|---|
| `07_BACKEND/AppTeschi.Api/server.js` | Modificado | `firmarTokenAdministrador(...)`, middleware `requireAdmin(rolesPermitidos?)` aplicado a los 9 endpoints del panel de administrador (reemplaza `hasValidApiKey`); `POST /api/otp/verificar` emite el token cuando el administrador se verifica; `writeAudit(...)` usa `req.admin` en vez de headers `x-actor-*`; nuevos `GET/POST/PUT /api/administradores` (SUPERADMIN-only) |
| `07_BACKEND/AppTeschi.Api/.env`, `.env.example` | Modificado | Nueva variable `JWT_SECRET` |
| `service/AdminAuthHeader.kt` | Nuevo | `conSesionAdmin()` — agrega `Authorization: Bearer <token>` |
| `service/AdminAccountsService.kt` | Nuevo | Listar/crear/editar administradores |
| `service/AdminUsersService.kt`, `AdminAuditService.kt`, `CalificacionesAdminService.kt`, `EstadisticasService.kt` | Modificado | `x-api-key` + headers de actor → `conSesionAdmin()` |
| `service/OtpService.kt` | Modificado | `verificar(...)` devuelve `Result<String?>` (el token, cuando aplica) en vez de `Result<Unit>` |
| `data/UserSession.kt` | Modificado | Nuevos campos `adminToken`, `rol` |
| `data/repository/AuthRepository.kt`, `viewmodel/AuthViewModel.kt` | Modificado | Propagan el token hasta `AuthUiState.tokenAdmin` |
| `ui/login/LoginScreen.kt` | Modificado | Pasa `adminToken`/`rol` a `UserSession.establecer(...)` |
| `ui/dashboard/AdminAdministradoresScreen.kt` | Nuevo | Gestión de cuentas de administrador (solo visible para SUPERADMIN) |
| `ui/dashboard/AdminDashboardScreen.kt` | Modificado | 5ª tarjeta condicional ("Administradores"); "cerrar sesión" ahora limpia `UserSession` |
| `ui/dashboard/AdminAlumnosScreen.kt` | Modificado | Botón "Eliminar" oculto si el rol no es SUPERADMIN |
| Pruebas (`AuthFakes.kt`, `AuthViewModelTest.kt`) | Modificado | `FakeRemoteOtpService` actualizado al nuevo tipo de retorno; prueba nueva que confirma que el token queda en `uiState.tokenAdmin` |

### Verificación

- Flujo completo en vivo (local y por túnel público): login → OTP → token; endpoint protegido sin token (401), con token inválido (401), con token válido (200)
- Auditoría usa la identidad real del token aunque se manden headers `x-actor-*` falsificados — se ignoran por completo
- Cuenta OPERADOR de prueba creada y verificada: puede alumnos/calificaciones/auditoría/estadísticas (200), bloqueada con 403 en eliminar alumno y en gestión de administradores
- SUPERADMIN no puede desactivarse ni degradarse a sí mismo
- 63/63 pruebas unitarias pasando; compilación y `assembleDebug` sin errores

---

## v0.23 — 09/09/2026 · Fix: bypass de administrador retirado (rompía todo el panel tras v0.22)

Ver [[DECISIONES_TECNICAS]] DEC-021. Bug reportado por el usuario al probar el APK de v0.22: entrar con `admin`/`admin` dejaba cada pantalla del panel mostrando "Falta la sesión de administrador".

### Cambios aplicados

| Archivo | Tipo | Cambio |
|---|---|---|
| `data/repository/AuthRepository.kt` | Modificado | Se quitó `LoginResultado.AdministradorSinOtp` y `esAdminLocal(...)` — `admin`/`admin` ahora pasa por la cascada real (cuenta de administrador → cuenta de alumno → SIIA) como cualquier otro intento |
| `viewmodel/AuthViewModel.kt` | Modificado | Se quitó `entrarComoAdministrador(...)` y su rama en el `when` |
| Pruebas (`AuthRepositoryTest.kt`, `AuthViewModelTest.kt`) | Modificado | Las pruebas del bypass se reemplazaron por pruebas que confirman que `admin`/`admin` se rechaza si no hay una cuenta real detrás |

### Por qué pasó

Los endpoints del panel ahora exigen un token JWT (DEC-020) que solo se emite al verificar el OTP de una cuenta real. El bypass marcaba `esAdministrador = true` y navegaba al panel sin pasar por OTP — nunca obtenía token, así que cada llamada fallaba. Antes de DEC-020 esto no pasaba porque bastaba la `x-api-key` estática.

### Verificación

- Compilación y 63/63 pruebas unitarias pasando
- APK reconstruido; para entrar como administrador ahora hace falta la cuenta real (`admin`, ver DEC-019) con su OTP — el bypass de alumno de prueba sigue funcionando igual, no se tocó

---

## v0.24 — 09/09/2026 · UX: "Ya tengo un código" en la verificación 2FA

Pedido del usuario: si el código de un envío anterior todavía es válido (dentro de sus 10 minutos), la pantalla solo mostraba el campo para escribirlo *después* de tocar "Enviar código" — obligando a pedir uno nuevo (e invalidando el que ya tenían en el correo) solo para que el campo apareciera.

### Cambios aplicados

| Archivo | Tipo | Cambio |
|---|---|---|
| `ui/login/LoginScreen.kt` | Modificado | Nuevo estado local `tengoCodigo`; `VerificacionSection` gana el parámetro `mostrarCampoCodigo` (true si se envió en esta pantalla **o** si el usuario tocó "Ya tengo un código") y un botón de texto correspondiente, visible solo mientras el campo de código no se ha revelado |

### Verificación

- Compilación, 63/63 pruebas unitarias, `assembleDebug` sin errores
- El botón no llama a `/api/otp/enviar` — solo revela el campo local; verificar sigue funcionando igual porque el backend ya validaba por matrícula/usuario + código, nunca dependió de que la app "recordara" haber enviado el código en esa sesión de pantalla

---

## v0.25 — 09/09/2026 · Fix: "Ya tengo un código" seguía bloqueado por un candado del cliente

Bug reportado por el usuario al probar el APK de v0.24: el botón "Ya tengo un código" sí revelaba el campo, pero al tocar "Verificar" la app respondía "Primero envía el código a tu correo" — el mismo error que la función estaba pensada para evitar.

### Causa

`v0.24` solo tocó la UI (`LoginScreen.kt`). `AuthViewModel.verificarOtp()` conservaba un candado anterior a DEC-019, de cuando el OTP todavía se generaba y verificaba en el cliente: exigía `_uiState.value.otpEnviado == true` (es decir, haber llamado a `enviarCodigoVerificacion()` en esa misma sesión de pantalla) antes de permitir verificar. Ese candado nunca se quitó al mover el OTP al backend, y bloqueaba exactamente el caso que "Ya tengo un código" habilita: verificar un código de un envío anterior sin volver a pedir uno nuevo.

### Cambios aplicados

| Archivo | Tipo | Cambio |
|---|---|---|
| `viewmodel/AuthViewModel.kt` | Modificado | Se quitó el `if (!_uiState.value.otpEnviado) { onFallo("Primero envía el código a tu correo"); return }` de `verificarOtp()`. Ahora solo valida localmente que el código tenga 6 dígitos; la validez real (código correcto, dentro de los 10 minutos, sin exceder los 5 intentos) la decide el backend, como ya ocurría en la práctica desde DEC-019 |
| `viewmodel/AuthViewModelTest.kt` | Modificado | Se reemplazó la prueba que afirmaba el candado viejo (`no se puede verificar sin antes enviar el codigo`) por dos pruebas: verificar con un código de un envío anterior (sin llamar a `enviarCodigoVerificacion()` en la sesión) tiene éxito, y un código con longitud inválida se rechaza localmente sin llamar al backend |

### Verificación

- Compilación y 64/64 pruebas unitarias pasando
- Túnel Cloudflare verificado (`/health` → 200) antes de compilar
- APK reconstruido (`assembleDebug`) y enviado al usuario
- Cambio puramente de cliente — no requirió tocar `server.js` ni reiniciar el backend
