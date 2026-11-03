# Registro de Decisiones Técnicas

> Decisiones de arquitectura, stack y diseño tomadas durante el proyecto.
> Documenta el **qué**, el **por qué** y las **alternativas descartadas**.
> **Relacionado con:** [[MAPA_PROYECTO]], [[REGLAS_PROYECTO]]
> Las decisiones son históricas: no se borran. Cuando una deja de aplicar, se marca con **Vigencia: ⛔ Sustituida** y a cuál la reemplaza.

---

## DEC-001 — Web Scraping en lugar de API REST
- **Fecha:** 24/08/2026
- **Vigencia:** ⛔ Sustituida — scraping retirado en DEC-026; el login con el SIIA ahora usa su API REST oficial (DEC-035).
- **Decisión:** Acceder al SIIA mediante web scraping (OkHttp + Jsoup) simulando formularios ASP.NET
- **Por qué:** El SIIA no expone endpoints REST. Usa ASP.NET Web Forms con `__VIEWSTATE` y cookies de sesión
- **Alternativas descartadas:** Retrofit (requiere API REST), API propia (requiere backend TI)
- **Impacto:** Toda llamada al SIIA requiere GET previo para extraer tokens, luego POST con `application/x-www-form-urlencoded`

---

## DEC-002 — Sin JWT, sesión por Cookie ASP.NET
- **Fecha:** 24/08/2026
- **Vigencia:** ⛔ Sustituida — sesión con JWT propio desde DEC-020/DEC-030.
- **Decisión:** Gestionar sesión con `CookieJar` de OkHttp en memoria RAM
- **Por qué:** El SIIA usa `ASP.NET_SessionId`, no tokens JWT
- **Impacto:** La cookie se pierde al cerrar la app. No hay "recordar sesión" sin persistencia cifrada

---

## DEC-003 — OTP generado localmente, enviado por Gmail SMTP
- **Fecha:** 24/08/2026
- **Vigencia:** ⛔ Sustituida — el OTP se genera en el servidor (DEC-019) y sale por Microsoft Graph (DEC-034).
- **Decisión:** Generar el OTP en la app y enviarlo por Gmail SMTP con JavaMail
- **Por qué:** No hay endpoint de autenticación 2FA en el SIIA. Gmail SMTP es la opción más rápida sin backend propio
- **Cuenta usada:** `appteschi@gmail.com` con contraseña de aplicación
- **Riesgo activo:** El dominio `@teschi.edu.mx` podría bloquear correos externos (ver [[BUGS_Y_ERRORES]] BUG-005)
- **Alternativa futura:** Backend propio con endpoint `/api/auth/enviar-otp` usando SMTP de la institución

---

## DEC-004 — Credenciales en local.properties + BuildConfig
- **Fecha:** 24/08/2026
- **Vigencia:** ⛔ Sustituida — ya no hay credenciales SMTP ni API key en la app (DEC-019, DEC-030).
- **Decisión:** Almacenar credenciales SMTP en `local.properties` e inyectarlas como `BuildConfig` fields
- **Por qué:** `local.properties` está en `.gitignore` por defecto — nunca se sube al repositorio
- **Impacto:** Cada desarrollador necesita su propio `local.properties` con las credenciales

---

## DEC-005 — DEV_MODE flag en MainActivity
- **Fecha:** 26/08/2026
- **Vigencia:** ⛔ Sustituida — ya no hay OTP por correo (DEC-043).
- **Decisión:** Constante `DEV_MODE` en `MainActivity.kt` para mostrar OTP en pantalla durante desarrollo
- **Por qué:** Permite probar el flujo completo sin necesitar un correo funcional
- **Estado actual:** `DEV_MODE` eliminado — OTP solo por correo (27/08/2026)
- **Producción:** Sin código visible en pantalla

---

## DEC-006 — Recuperar Contraseña accesible desde Login, no desde Dashboard
- **Fecha:** 26/08/2026
- **Vigencia:** ⛔ Sustituida — la contraseña es la del SIIA y se recupera en su portal; la app solo recupera el NIP (DEC-043).
- **Decisión:** Link "¿Olvidaste tu contraseña?" en la pantalla de Login en lugar del Dashboard
- **Por qué:** El usuario que olvidó su contraseña no puede llegar al Dashboard — el acceso lógico es desde el Login
- **Impacto:** Dashboard queda con 4 módulos en grid 2×2 (más limpio)

---

## DEC-007 — OTP temporal a cualquier correo
- **Fecha:** 27/08/2026
- **Vigencia:** ⛔ Sustituida — el correo del OTP lo decide el servidor (DEC-025, DEC-036).
- **Decisión:** Permitir envío de OTP a cualquier correo válido (Gmail personal, etc.)
- **Por qué:** `@teschi.edu.mx` no recibe Gmail externo; TI aún no entrega SMTP institucional
- **Temporal:** Restringir a `@teschi.edu.mx` cuando TI entregue cuenta SMTP
- **Impacto:** Campo "Correo para código 2FA" separado de matrícula SIIA

---

## DEC-008 — Validación SIIA obligatoria antes del OTP
- **Fecha:** 27/08/2026
- **Vigencia:** ⛔ Sustituida — retirada en DEC-026; la validación con el SIIA volvió por su API oficial y desde el backend (DEC-035).
- **Decisión:** `SiiaAuthService.validarCredenciales()` debe exitar antes de generar y enviar OTP
- **Por qué:** Evitar acceso sin credenciales reales del portal escolar
- **Implementación:** GET `Login.aspx` → tokens → POST `default.aspx` con `txtUsuario` + `txtPass`
- **Detección de error:** `alert('los datos son errores')` o formulario de login persistente
- **Impacto:** Requiere internet y acceso a `148.230.236.166`; `usesCleartextTraffic=true` en manifest

---

## DEC-009 — UI de login inspirada en portal web TESCHI
- **Fecha:** 27/08/2026
- **Vigencia:** ✅ Vigente, sin el botón "Registrarse" desde DEC-043.
- **Decisión:** Pantalla de login con card blanca superior + sección verde inferior (logo TESChi, Registrarse)
- **Referencia:** Diseño de `teschi.teschi.edu.mx` (Servicio Social web)
- **Archivo:** `ui/login/LoginScreen.kt`
- **Impacto:** Login en 2 pasos visuales — credenciales SIIA primero, correo/OTP después

---

## DEC-010 — SQL Server Express para perfiles de usuario
- **Fecha:** 27/08/2026
- **Vigencia:** ⛔ Sustituida — la app ya no usa base de datos (DEC-043); AppTeschiDB queda como histórico.
- **Decisión:** Base `AppTeschiDB` con tablas `Usuarios`, `OtpHistorial`, `SesionesLogin`
- **Por qué:** Centralizar perfiles y auditoría fuera del dispositivo
- **Seguridad:** Contraseña SIIA **no** se almacena; OTP solo como hash SHA-256
- **Estado:** Scripts listos; integración con app pendiente de API REST
- **Ver:** [[Base_Datos_Usuarios]]

---

## DEC-011 — Panel de administrador con acceso a todos los perfiles y auditoría
- **Fecha:** 06/09/2026
- **Vigencia:** ⛔ Sustituida — el panel de administrador se quitó de la app (DEC-043).
- **Decisión:** El administrador tiene una ruta dedicada (`Routes.ADMIN_DASHBOARD`) con lista completa de usuarios, acceso a cada perfil individual (`Routes.ADMIN_PROFILE/{matricula}`) y vista de todos sus movimientos de auditoría
- **Por qué:** El administrador necesita vigilar toda la actividad de la plataforma; la vista de alumno solo muestra sus propios módulos
- **Implementación:**
  - `AdminDashboardScreen` — directorio completo, CRUD de usuarios, resumen de seguridad, log de auditoría local
  - `AdminProfileScreen` — datos del perfil + historial completo de movimientos remotos (`AdminAuditService`)
  - `Routes.adminProfile(matricula)` — helper para construir la ruta con parámetro
- **Fuente de datos:** API local `http://192.168.0.41:4000/api/usuarios` y `/api/auditoria?matricula=…` con `x-api-key`
- **Fallback:** Si la API no responde, `AdminDirectory` (SharedPreferences local) sirve como respaldo

---

## DEC-012 — Navegación post-login bifurcada (admin vs alumno)
- **Fecha:** 06/09/2026
- **Vigencia:** ⛔ Sustituida — solo hay alumnos: el login siempre lleva a Inicio (DEC-043).
- **Decisión:** Al completar el login, `LoginScreen` lee `UserSession.esAdministrador` y navega a `ADMIN_DASHBOARD` o `DASHBOARD` según corresponda
- **Por qué:** Los administradores no deben ver el panel del alumno; los alumnos no deben acceder al centro de control
- **Implementación:** `LaunchedEffect(uiState.irAlDashboard)` en `LoginScreen.kt` — se dispara cuando `AuthViewModel` activa el flag `irAlDashboard`
- **Administrador local de bypass:** matrícula `admin` + contraseña `admin` → bypass directo sin OTP para desarrollo

---

## DEC-013 — Un único esquema de color (sin modo oscuro)
- **Fecha:** 06/09/2026
- **Decisión:** `AppTeschiTheme` usa exclusivamente `lightColorScheme` institucional, ignorando el modo oscuro del sistema
- **Por qué:** La identidad visual institucional debe ser fija. El fondo negro en modo oscuro no es parte del diseño TESCHI
- **Implementación técnica:** Se eliminó `darkColorScheme`. Se declararon explícitamente `surfaceVariant` y `onSurfaceVariant` para evitar que Material3 tome valores de su paleta oscura por defecto
- **Trade-off asumido:** Usuarios con modo oscuro en el celular verán el tema claro en la app — comportamiento esperado y documentado

---

## DEC-014 — Normalización del esquema de base de datos (V1 → V2)
- **Fecha:** 08/09/2026
- **Vigencia:** ⛔ Histórica — AppTeschiDB ya no se usa (DEC-043).
- **Decisión:** Fusionar `Usuarios` y `CuentasRegistro` en una sola tabla `Alumnos` (+ `AlumnoCredenciales` para la contraseña local, cardinalidad 1:0..1), quitar la columna `Matricula` redundante de `OtpHistorial`/`SesionesLogin` (ya tenían `IdUsuario`), normalizar `Carrera` como FK en todas partes, y agregar el dominio académico (`PlanEstudioMaterias`, `Grupos`, `HistorialAcademico`, catálogos de Periodo/Estatus/Turno)
- **Por qué:** `Usuarios` y `CuentasRegistro` representaban al mismo alumno en dos tablas sin relación entre sí — se confirmó con datos reales que la matrícula `20240001` tenía nombres distintos en cada una. El resto de los cambios elimina dependencias transitivas (3FN) y una representación de carrera como texto libre en paralelo a la versión con catálogo
- **Efecto colateral encontrado:** los nombres de `CatalogoCarreras` tenían los acentos corruptos en la base de datos real (bug de codificación de una ejecución anterior de `01_CrearBaseDatos.sql`) — se corrigió como parte de la migración
- **Implementación:** `database/sql/03_MigracionV2_Normalizacion.sql` (migra datos existentes sin perderlos — las tablas viejas se renombran a `_ObsoletoV1_*`, no se borran), `database/sql/02_ProcedimientosUsuarios.sql` actualizado, `server.js` actualizado para leer/escribir `Alumnos`/`AlumnoCredenciales` manteniendo el mismo contrato JSON con la app Android
- **Alcance:** el dominio académico nuevo (materias/grupos/historial) queda con el catálogo sembrado (53 materias ISC, 20 grupos) pero **sin alumnos** todavía — el backend no tiene endpoints que lo usen; Kardex/Tira/Calificaciones en la app siguen siendo datos locales en Kotlin hasta que exista integración real con el SIIA
- **Ver:** [[Base_Datos_Usuarios]]

---

## DEC-015 — Módulo "Plan de Estudios": primer consumo real del catálogo académico desde la app
- **Fecha:** 08/09/2026
- **Vigencia:** ⛔ Sustituida — ya la había retirado DEC-016; desde DEC-043 no hay catálogo propio.
- **Decisión:** Exponer `PlanEstudioMaterias` y `Grupos` vía dos endpoints públicos nuevos (`GET /api/plan-estudios/:clave`, `GET /api/grupos/:clave`) y agregar un módulo nuevo en el Dashboard del alumno donde se elige cualquiera de las 7 carreras sembradas y se ve su plan de estudios por semestre + grupos reales del ciclo
- **Por qué:** Después de sembrar el catálogo académico completo (ISC, Animación, Industrial, Mecatrónica, Química, Administración, Gastronomía — ver DEC-014 y [[Base_Datos_Usuarios]]), el alumno pidió que esa información fuera visible dentro de la app, no solo en la base de datos
- **Alcance deliberadamente acotado:** este módulo es de **consulta general del catálogo institucional** (cualquier alumno puede ver el plan de cualquier carrera) — es independiente de Kardex/Tira de Materias/Calificaciones, que siguen mostrando el historial *real* de un alumno (datos locales en Kotlin, `HistorialAcademico.kt`). No se fusionaron ambos flujos: mezclar el catálogo genérico con el historial personal real hubiera sido un cambio mucho más grande y arriesgado para una sesión de trabajo
- **Implementación:**
  - Backend: dos endpoints nuevos en `server.js`, sin `x-api-key` (igual que `/api/catalogos/registro`: es catálogo curricular público, no dato personal)
  - App: `PlanEstudiosService.kt` (interfaz `PlanEstudiosApi` + implementación real por OkHttp, mismo patrón que `AdminUsersService.kt`/`LocalAccountAuthService.kt`), `PlanEstudiosViewModel.kt` (con inyección por constructor para pruebas), `PlanEstudiosScreen.kt`, ruta `Routes.PLAN_ESTUDIOS`, tarjeta nueva en `DashboardScreen.kt`
  - Pruebas: `PlanEstudiosViewModelTest.kt` con `FakePlanEstudiosApi` (mismo patrón que los fakes de autenticación)
- **Ver:** [[Base_Datos_Usuarios]]

---

## DEC-016 — El catálogo académico es para el administrador, no para que el alumno navegue otras carreras
- **Fecha:** 08/09/2026
- **Vigencia:** ⛔ Sustituida — sin catálogo propio desde DEC-043.
- **Decisión:** Se retiró el módulo "Plan de Estudios" del Dashboard del alumno (DEC-015) y se reemplazó por una sección "Carga académica esperada" dentro de `AdminProfileScreen` — el administrador asigna el semestre del alumno y el sistema muestra, según su carrera y ese semestre, qué materias de `PlanEstudioMaterias` le corresponden
- **Por qué:** El alumno ya sabe a qué carrera está inscrito — no necesita navegar el plan de otras carreras. El propósito real de sembrar el catálogo académico completo (7 carreras) era que el **administrador** pudiera verificar, al registrar o revisar a un alumno, qué materias debería tener inscritas ese ciclo — para cotejar contra el SIIA real
- **Cambio de esquema:** se agregó `Alumnos.Semestre` (`TINYINT NULL`, `CHECK` 1-12) — no existía ninguna forma de saber en qué semestre iba un alumno. Es nullable porque no todo alumno lo tiene asignado todavía; lo asigna el administrador manualmente (no viene del SIIA)
- **Implementación:**
  - `database/sql/16_AgregarSemestreAlumnos.sql`
  - `server.js`: `/api/usuarios` ahora incluye `ClaveCarrera` y `Semestre`; nuevo `PUT /api/usuarios/:matricula/semestre` (admin-only, valida 1-12, deja auditoría)
  - `ManagedUser` (Kotlin): campos nuevos `claveCarrera`, `semestre`
  - `AdminUsersService.actualizarSemestre(...)`
  - `AdminProfileScreen.kt`: sección `CargaAcademicaCard` — campo de semestre + botón guardar, y lista de materias esperadas (reutiliza `PlanEstudiosService.materias(clave)` de DEC-015, filtrado por semestre)
- **Archivos retirados:** `PlanEstudiosScreen.kt`, `PlanEstudiosViewModel.kt`, `PlanEstudiosViewModelTest.kt`, `testutil/PlanEstudiosFakes.kt` — la capa de red (`PlanEstudiosService`/`PlanEstudiosApi`, `CarreraCatalogo`/`GrupoInfo`) se conservó porque se reutiliza en `AdminProfileScreen`
- **Ver:** [[Base_Datos_Usuarios]]

---

## DEC-017 — Panel de administrador: CRUD real de alumnos, calificaciones, auditoría enlazada y estadísticas
- **Fecha:** 09/09/2026
- **Vigencia:** ⛔ Sustituida — el panel de administrador se quitó de la app (DEC-043).
- **Decisión:** El administrador pidió control total sobre la base de datos ("quiero que pueda hacer registros, consultas, eliminación, actualización de alumnos, tenga acceso a las calificaciones de quien el quiera... gráficos... auditoría"), cada cosa en su propio apartado. Se reemplazó el CRUD que existía (`AdminDirectory`, local en `SharedPreferences`, sin tocar la base de datos real salvo por un *upsert* aditivo vía `/api/usuarios/sync`) por operaciones reales contra `AppTeschiDB`, y se dividió `AdminDashboardScreen` (antes un único scroll con formulario + lista + auditoría + log local) en un *hub* con 4 secciones independientes
- **Por qué el CRUD anterior no servía:** `UserDirectoryEditor` en `AdminDashboardScreen` guardaba en `AdminDirectory` (memoria + `SharedPreferences` del propio dispositivo) y encolaba el registro en `PendingUserSync` para subirlo por `/api/usuarios/sync` — un endpoint pensado para sincronizar perfiles ya existentes desde el SIIA, no para altas/bajas administrativas reales. No había ningún endpoint de `DELETE`, y la "edición" nunca tocaba `IdCarrera`/`Semestre`. Es decir: el botón "Eliminar" del panel nunca borraba nada de SQL Server.
- **Descubrimiento durante la implementación:** la tabla `HistorialAcademico` (Kardex/calificaciones) ya existía desde DEC-014, completamente sembrada de estructura pero sin ningún endpoint que la usara — el módulo "Calificaciones" del alumno (`CalificacionesViewModel.kt`) es un *mock* declarado como tal en su propio comentario, independiente de esta tabla. No hizo falta ninguna tabla nueva para el requisito de calificaciones, solo exponerla. También se encontró que `AuditoriaMovimientos.IdAlumno` (columna agregada en la migración V2, sección 5) nunca se llenaba desde `writeAudit()` — el filtro `?matricula=` que ya mandaba `AdminAuditService.kt` desde el perfil del alumno no tenía ningún efecto en el backend.
- **Implementación — Backend (`server.js`):**
  - `writeAudit(actor, action, detail, matriculaAfectada)` ahora resuelve `IdAlumno` por subconsulta y lo guarda — cada movimiento queda enlazado al perfil sobre el que se actuó, no solo a quién lo hizo
  - `GET /api/auditoria` acepta `?matricula=` (filtra por actor **o** perfil afectado, vía `LEFT JOIN Alumnos`) y `?limit=`
  - `POST /api/usuarios` — alta de alumno por el administrador (sin contraseña; el alumno la crea al registrarse con `/api/registro`, que ya sabía completar un perfil existente)
  - `PUT /api/usuarios/:matricula` — edición parcial de perfil (nombre, correos, carrera, semestre, activo), solo actualiza los campos presentes en el body
  - `DELETE /api/usuarios/:matricula` — baja real, en transacción: limpia `HistorialAcademico`/`OtpHistorial`/`SesionesLogin`, desvincula `AuditoriaMovimientos.IdAlumno` (para no perder el historial), y borra `Alumnos` (`AlumnoCredenciales` se va sola por `ON DELETE CASCADE`)
  - `GET /api/calificaciones/:matricula` — todas las materias del plan de estudios de la carrera del alumno con su calificación si existe (`LEFT JOIN HistorialAcademico`); `PUT /api/calificaciones/:matricula/:idMateria` — captura/edita una calificación (0-10), deriva el estatus (Aprobada/No aprobó/Por cursar) automáticamente si no se manda uno explícito
  - `GET /api/estadisticas` — agregados para las gráficas: alumnos por carrera, por semestre, activos/inactivos/sin semestre, altas de los últimos 6 meses, distribución de calificaciones
  - Los 20 casos de prueba (alta, duplicado, carrera inválida, edición, 404, borrado en cascada, calificación fuera de rango, filtro de auditoría, etc.) se verificaron con `curl` contra la base de datos real antes de tocar la app
- **Implementación — Android:** se movieron `ManagedUser`/`UserRole` a `data/ManagedUser.kt` (antes vivían dentro de `AdminDirectory.kt`) y se eliminaron `AdminDirectory.kt` y `PendingUserSync.kt` (mock local y su cola de sincronización offline, ya sin uso). `AdminUsersService.kt` gana `crear()`, `actualizarPerfil()`, `eliminar()`. Servicios nuevos: `CalificacionesAdminService.kt`, `EstadisticasService.kt`. `AdminDashboardScreen.kt` pasa a ser un *hub* con 4 tarjetas de navegación (conserva la tarjeta de sesión, el permiso de ubicación y el log de auditoría **local** del dispositivo, que es un log de seguridad distinto al de la base de datos). Pantallas nuevas: `AdminAlumnosScreen.kt` (alta/edición/baja con confirmación), `AdminCalificacionesScreen.kt` (elegir alumno → editar materias por semestre), `AdminAuditoriaScreen.kt` (historial completo filtrable), `AdminEstadisticasScreen.kt` (barras de progreso proporcionales — sin librería externa de gráficas)
- **Rutas nuevas:** `ADMIN_ALUMNOS`, `ADMIN_CALIFICACIONES`, `ADMIN_AUDITORIA`, `ADMIN_ESTADISTICAS`. `ADMIN_PROFILE` (perfil + carga académica esperada, DEC-016) se mantiene y ahora se llega a él también desde `AdminAlumnosScreen`
- **Ver:** [[Base_Datos_Usuarios]]

---

## DEC-018 — Perfil del alumno: historial en vez de carga esperada, semestre solo hacia adelante, y baja
- **Fecha:** 09/09/2026
- **Vigencia:** ⛔ Sustituida — el perfil muestra los datos del SIIA (DEC-043).
- **Decisión:** Retroalimentación directa tras probar DEC-017: la sección "Carga académica esperada" (DEC-016, un cálculo de qué materias *debería* tener un alumno según su semestre) se reemplazó en `AdminProfileScreen` por el **historial real** de materias con sus calificaciones (lo que ya construyó DEC-017 para `AdminCalificacionesScreen`, ahora también visible — de solo lectura — al ver el perfil). Además: (1) una vez que un alumno tiene semestre asignado, **no puede retroceder** — solo se ofrecen semestres superiores al actual; y (2) se agregó una sección de **baja** (desactivar/reactivar) directamente en el perfil, distinta de la eliminación permanente de `AdminAlumnosScreen`
- **Por qué "historial" y no "carga esperada":** la carga esperada era una herramienta de validación (¿qué le toca cursar?) útil solo antes de que existiera un registro real de calificaciones. Con `HistorialAcademico` ya expuesto (DEC-017), lo que el administrador necesita ver en el perfil es lo que el alumno **ya cursó y cómo le fue**, no una proyección
- **Por qué el semestre no retrocede:** es una regla académica real — un alumno no regresa a un semestre anterior una vez que avanzó. Se implementó en dos capas: la app solo ofrece semestres superiores en el desplegable (`SemestreCard`, opciones = `(actual+1)..12`), y el backend rechaza cualquier intento de bajar o repetir el semestre actual aunque no venga de la app (`PUT /api/usuarios/:matricula` valida contra el valor guardado antes de aplicar el cambio)
- **Por qué "baja" es distinta de "eliminar":** eliminar (ya existente en `AdminAlumnosScreen`, DEC-017) borra el alumno y su historial permanentemente — pensado para correcciones de captura, no para el flujo normal de un alumno que deja la institución. Dar de baja solo cambia `Alumnos.Activo` a `0` (columna que ya existía desde la migración V2): el alumno desaparece del filtro de activos pero su historial y calificaciones se conservan, y se puede reactivar
- **Implementación:**
  - `server.js`: `PUT /api/usuarios/:matricula` — antes de aplicar `Semestre`, consulta el valor actual y responde 400 si el nuevo es menor o igual
  - `AdminProfileScreen.kt`: se quitó `CargaAcademicaCard` (y sus efectos/estado asociados); se agregaron `SemestreCard` (desplegable solo con semestres superiores, guarda al elegir), `HistorialMateriasCard` (lectura de `CalificacionesAdminService.obtener(matricula)`, agrupado por semestre) y `BajaCard` (con diálogo de confirmación, alterna `Alumnos.Activo` vía `AdminUsersService.actualizarPerfil`)
  - No se agregaron endpoints nuevos para la baja — reutiliza `PUT /api/usuarios/:matricula` con `{"activo": false}`, ya existente desde DEC-017
- **Datos de prueba:** se sembraron 5 alumnos de ejemplo (uno por cada una de varias carreras, semestres distintos) con calificaciones aleatorias realistas en sus materias de semestres ya cursados, para poder ver el panel con datos representativos sin exponer alumnos reales
- **Ver:** [[Base_Datos_Usuarios]]

---

## DEC-019 — Cuentas reales de administrador, OTP y correo movidos al servidor

- **Fecha:** 09/09/2026
- **Vigencia:** ⛔ Sustituida — sin cuentas de administrador ni OTP por correo (DEC-043).
- **Decisión:** A petición del usuario de llevar el proyecto a "estándar universitario", se auditó la autenticación completa y se corrigieron tres hallazgos, todos con la misma causa raíz (el cliente tenía la última palabra sobre algo que debía decidir el servidor):
  1. El bypass de administrador (`admin`/`admin`) no estaba protegido por `BuildConfig.DEBUG` — funcionaba igual en un build de release. Se corrigió moviéndolo dentro del mismo bloque `if (BuildConfig.DEBUG)` que ya protegía a los usuarios de prueba de alumno.
  2. **No existía ninguna cuenta de administrador real.** `POST /api/auth/cuenta` devolvía `esAdministrador` siempre en `false` — el bypass de desarrollo era, en la práctica, la única forma de entrar al panel. Se crearon `Administradores`/`AdministradorCredenciales` (mismo patrón que `Alumnos`/`AlumnoCredenciales`, pero como entidad separada: un administrador no es un alumno) y `POST /api/auth/administrador`.
  3. **El OTP se generaba y verificaba enteramente en la app**, comparando contra un valor guardado en memoria del `ViewModel`. Cualquiera que controlara el cliente (un APK modificado, o simplemente automatizando la UI) podía saltarse la verificación sin que el servidor se enterara. Además, el envío de correo usaba credenciales SMTP (`EMAIL_APP_PASSWORD`, una contraseña de aplicación de Gmail real) **compiladas dentro del APK**, extraíbles por cualquiera que lo descompilara.
- **Por qué las tres se resuelven juntas:** las tres comparten la misma corrección — mover la fuente de verdad del cliente al servidor. Separarlas habría dejado el sistema a medias (por ejemplo, cuentas de administrador reales pero todavía con OTP falsificable no habría cerrado nada).
- **Implementación:**
  - `database/sql/17_TablaAdministradores.sql` — `Administradores` (`Usuario`, `NombreCompleto`, `Rol` con `CHECK IN ('SUPERADMIN','OPERADOR')`, `Activo`) y `AdministradorCredenciales` (mismas columnas que `AlumnoCredenciales`).
  - `database/sql/18_OtpHistorialParaAdministradores.sql` — `OtpHistorial.IdAlumno` pasa a nullable, se agrega `IdAdministrador` nullable + `CHECK` "exactamente uno de los dos", y `Intentos TINYINT` para bloquear fuerza bruta sobre el código *en el servidor* (antes el conteo de intentos vivía solo en la app y no protegía nada frente a una llamada directa a la API).
  - `07_BACKEND/AppTeschi.Api/scripts/crear_administrador.js` — utilidad de línea de comandos para crear/resetear administradores: genera una contraseña aleatoria seria (nunca inventada a mano) y la imprime una sola vez.
  - `server.js`: `POST /api/auth/administrador` (valida contra la tabla nueva); `POST /api/otp/enviar` (genera el código de 6 dígitos, lo guarda hasheado con SHA-256, lo manda por correo con `nodemailer` — ahora usando las variables `EMAIL_*` del propio backend, ya no las de la app); `POST /api/otp/verificar` (compara contra el hash, respeta expiración de 10 minutos y bloquea tras 5 intentos fallidos); límite de intentos en memoria (`excedeLimite`) aplicado a `/api/auth/cuenta`, `/api/auth/administrador`, `/api/otp/enviar` y `/api/otp/verificar`.
  - App Android: `AdminAccountAuthService.kt` (nuevo), `OtpService.kt` (nuevo, reemplaza a `EmailOtpService.kt` — eliminado junto con la dependencia `javamail-android` y los `buildConfigField` de `EMAIL_*`), `AuthRepository.kt` reescrito (cascada: bypasses de debug → administrador real → cuenta de alumno → SIIA; el envío/verificación de OTP ahora son llamadas de red, no lógica local), `AuthViewModel.kt` reescrito (ya no guarda el código OTP ni calcula su expiración — solo refleja lo que el backend responde), `LoginScreen.kt` corregido para que un administrador que entra por OTP también navegue a `ADMIN_DASHBOARD` (antes ese camino no existía: la única ruta que llegaba al panel de administrador era el bypass).
  - Se creó la primera cuenta real (`admin`, rol `SUPERADMIN`) con `crear_administrador.js` — contraseña generada y entregada una sola vez al usuario en el chat, fuera del código.
- **Deliberadamente fuera de alcance esta vez** (ver conversación con el usuario): reemplazar la `x-api-key` estática compartida por sesiones/JWT por administrador — hoy la auditoría ya identifica correctamente quién actuó porque existe una cuenta real detrás del login, pero la propia API todavía no verifica esa identidad en cada llamada, solo la clave compartida. También queda pendiente un sistema de permisos granular por rol (`SUPERADMIN`/`OPERADOR` ya existen en el esquema, pero ningún endpoint todavía distingue entre ellos).
- **Ver:** [[Base_Datos_Usuarios]], [[02_Requerimientos_No_Funcionales]]

---

## DEC-020 — Sesiones de administrador con JWT y permisos reales por rol

- **Fecha:** 09/09/2026
- **Vigencia:** ⛔ Sustituida — sin sesiones de administrador (DEC-043).
- **Decisión:** Se resolvieron los dos pendientes que quedaron explícitamente abiertos en DEC-019: (1) la `x-api-key` estática compartida y los headers `x-actor-matricula`/`x-actor-nombre` (que el propio cliente declaraba sin que nada los verificara) se reemplazaron por un token de sesión firmado (JWT) que el backend emite al terminar de verificar el OTP de un administrador; (2) los roles `SUPERADMIN`/`OPERADOR` (que ya existían en el esquema desde DEC-019 pero ningún endpoint distinguía) ahora sí limitan qué puede hacer cada uno.
- **Por qué juntas:** sin identidad verificada por el servidor, un sistema de roles es cosmético — cualquiera con la `x-api-key` podía declararse "el actor que quisiera" en los headers. El JWT es lo que hace que "OPERADOR no puede eliminar alumnos" sea una regla real del servidor y no una convención de la UI.
- **Reparto de permisos:** un OPERADOR tiene control total del día a día — alta/edición de alumnos, calificaciones, consulta de auditoría y estadísticas. Quedan exclusivas de SUPERADMIN las dos acciones de mayor riesgo: `DELETE /api/usuarios/:matricula` (eliminación permanente e irreversible) y toda la gestión de cuentas de administrador (`/api/administradores`). Un SUPERADMIN no puede desactivarse ni quitarse el rol a sí mismo (evita quedarse sin ningún superadmin activo).
- **Sin esto no era posible crear un OPERADOR real** — antes la única vía para crear cualquier administrador era el script de línea de comandos `crear_administrador.js`. Se agregó `POST /api/administradores` (SUPERADMIN-only) y su pantalla en la app, para que un SUPERADMIN pueda dar de alta personal de control escolar sin tocar la terminal.
- **Implementación:**
  - `server.js`: `firmarTokenAdministrador(admin)` (JWT, expira en 12h) se emite dentro de `POST /api/otp/verificar` cuando `tipo === 'ADMINISTRADOR'` y el código es correcto. `requireAdmin(rolesPermitidos?)` es un middleware que exige `Authorization: Bearer <token>`, lo verifica, y opcionalmente exige un rol — se aplicó a los 9 endpoints del panel de administrador (antes usaban `hasValidApiKey`). Cada `writeAudit(...)` ahora usa `req.admin.usuario`/`req.admin.nombre` (identidad verificada), no los headers que el cliente declaraba.
  - `POST/GET/PUT /api/administradores` (nuevos, SUPERADMIN-only): listar, crear (contraseña generada igual que `crear_administrador.js`, entregada una sola vez en la respuesta) y editar rol/estado.
  - `.env`: nueva variable `JWT_SECRET` (aleatoria, 48 bytes).
  - App Android: `AdminAccountsService.kt` (nuevo), `AdminAdministradoresScreen.kt` (nueva, solo visible en el hub si `UserSession.rol == "SUPERADMIN"`), `conSesionAdmin()` (extensión compartida en `AdminAuthHeader.kt` que agrega el header `Authorization`) reemplaza la `x-api-key`/headers de actor en `AdminUsersService`, `AdminAuditService`, `CalificacionesAdminService`, `EstadisticasService`. `UserSession` gana `adminToken`/`rol`. `OtpService.verificar` ahora devuelve el token (`Result<String?>`) en vez de `Result<Unit>`. El botón "Eliminar" de `AdminAlumnosScreen` se oculta si el rol no es `SUPERADMIN`. El botón de "cerrar sesión" del panel ahora sí limpia `UserSession` (antes dejaba el token en memoria).
- **Verificado en vivo:** login + OTP de un SUPERADMIN y de un OPERADOR de prueba; endpoint protegido sin token (401), con token inválido (401), con token válido (200); auditoría usando la identidad real del token aunque se manden headers `x-actor-*` falsos (se ignoran); OPERADOR bloqueado con 403 en `DELETE /api/usuarios`, `GET/POST /api/administradores`; SUPERADMIN no puede desactivarse a sí mismo.
- **Ver:** [[Base_Datos_Usuarios]], [[02_Requerimientos_No_Funcionales]]

---

## DEC-021 — Retirado el bypass de administrador (dejaba de funcionar tras DEC-020)

- **Fecha:** 09/09/2026
- **Vigencia:** ⛔ Histórica — desde DEC-043 tampoco hay usuarios de prueba.
- **Decisión:** Se eliminó por completo el atajo de desarrollo `admin`/`admin` (`LoginResultado.AdministradorSinOtp`, `AuthRepository.esAdminLocal`, `AuthViewModel.entrarComoAdministrador`).
- **Por qué — bug real detectado por el usuario:** al probar el APK de DEC-020, cada pantalla del panel mostraba "Falta la sesión de administrador" / "No se pudieron cargar los administradores", etc. Causa: el bypass seguía marcando `esAdministrador = true` y llevando al Centro de control, pero **nunca pasaba por `/api/otp/verificar`**, que es el único lugar donde el backend emite el token de sesión (JWT, DEC-020). El usuario quedaba "adentro" con una sesión que parecía válida pero sin ningún token — todo endpoint del panel la rechazaba. Antes de DEC-020 esto no pasaba porque los endpoints solo pedían la `x-api-key` estática, que sí estaba disponible sin pasar por OTP.
- **Por qué se retira en vez de arreglarse:** parchear el bypass para que también obtenga un token exigiría hacerlo hablar con el backend igual que una cuenta real — en ese punto ya no es un atajo de nada. La cuenta real de administrador (`admin`, creada en DEC-019) ya pasa por el flujo completo (login + OTP) y funciona; no hace falta un segundo camino que además queda roto cada vez que un endpoint nuevo empiece a exigir sesión.
- **Impacto:** el bypass de alumno de prueba (`alumno`/`alumno`, `sem1`..`sem9`) **no se tocó** — esos módulos son mock, no llaman a ningún endpoint protegido por JWT, así que nunca tuvieron este problema.

## DEC-022 — Un código OTP se puede volver a verificar dentro de sus 10 minutos, aunque ya se haya usado

- **Fecha:** 15/09/2026
- **Vigencia:** ⛔ Sustituida — ya no hay OTP por correo; el segundo paso es el NIP del SIIA (DEC-041, DEC-043).
- **Decisión:** `POST /api/otp/verificar` ya no exige que el código esté en estado `PENDIENTE` — también acepta uno en estado `VERIFICADO` (ya usado con éxito antes), siempre que no haya expirado. Solo cambia la condición de entrada (`server.js`, endpoint de verificación); la comparación de hash, el límite de intentos fallidos y la expiración siguen igual.
- **Por qué — pedido explícito del alumno:** escenario real — el alumno pide el código, lo recibe, lo verifica y entra con éxito; después la app se cierra por algo externo al proyecto (Android mata el proceso, error del teléfono, etc.). Antes, el código ya estaba marcado `VERIFICADO` y quedaba inservible, así que para volver a entrar había que pedir un correo nuevo — el alumno no quiere esperar otro correo si el que ya tiene sigue dentro de sus 10 minutos de vigencia.
- **Trade-off de seguridad, aceptado explícitamente:** un código deja de ser estrictamente de un solo uso — cualquiera que lo intercepte dentro de la ventana de 10 minutos podría reutilizarlo más de una vez (antes solo podía usarlo una vez). Se acepta porque la ventana sigue siendo corta y el límite de intentos fallidos (`OTP_MAX_INTENTOS`) sigue protegiendo contra fuerza bruta igual que antes.
- **No cambia:** un código ya `EXPIRADO` o `FALLIDO` (por exceder intentos) sigue sin poder reutilizarse bajo ninguna circunstancia.

## DEC-023 — Kardex/Tira de Materias/Calificaciones del alumno conectados a la base de datos real

- **Fecha:** 15/09/2026
- **Vigencia:** ⛔ Sustituida — Calificaciones y Tira de materias salen del SIIA y el Kardex está en construcción (DEC-043).
- **Decisión:** Se retiró el historial real de un alumno, hardcodeado en `HistorialAcademico.kt` (el mock `transcriptReal`/`materiasReales`). Los tres módulos ahora consultan `GET /api/mi-historial/{matricula}` (endpoint nuevo, protegido con `x-api-key` — el alumno no tiene JWT propio, solo el admin la tiene desde DEC-020) a través de `HistorialAcademicoService`, la misma consulta SQL que ya usaba `/api/calificaciones/{matricula}` del lado administrador — ambos lados quedan garantizados a mostrar exactamente lo mismo, porque leen la misma fuente.
- **Los usuarios de prueba sem1..sem9 no se tocaron:** `HistorialAcademico.materiasSimuladas(semestre)` sigue existiendo — son alumnos que no existen en la base de datos, así que `historialDelAlumnoEnSesion()` los detecta por `UserSession.semestreSimulado != null` y nunca llama al backend para ellos.
- **Huecos de datos aceptados, no resueltos en esta pasada:**
  - `dbo.HistorialAcademico` no guarda el período por materia — `KardexUiState.periodoIngreso`/`ultimoPeriodo` se quedan con los valores fijos de siempre ("2022-2"/"2026-1"), no vienen de la base de datos para ningún alumno.
  - `promedioUltimoSemestre` ya no se calcula por período (no existe ese dato) — ahora es el promedio de las materias del semestre inmediato anterior al actual (`semestre == alumno.semestre - 1`). Para el alumno de prueba usado al verificarlo dio el mismo resultado que antes, porque su semestre 8 fue, de hecho, su último período completo.
  - Tira de Materias deriva el grupo real tomando el primero que corresponda al semestre del alumno (mismo criterio ya aceptado para Reinscripción Fase 1, ver 02_MODULOS/01_Reinscripcion.md) — sigue sin existir ningún campo que registre a qué turno pertenece cada alumno.
- **Se migró el historial real de un alumno a la base de datos** (script fuera de este repositorio — ver DEC-032) (ver también Reinscripción Fase 1) — sin esa migración, `/api/mi-historial/{matricula}` habría regresado 53 materias sin calificación.

## DEC-024 — Reinscripción Fase 2: bloqueo por observación de reglamento + panel del director

- **Fecha:** 15/09/2026
- **Vigencia:** ⛔ Sustituida — Reinscripción se quitó de la app (DEC-043).
- **Decisión:** Nueva tabla `dbo.ObservacionesReglamento` y 5 endpoints (`GET`/`POST /api/observaciones`, `PUT /api/observaciones/:id/resolver`, `GET /api/reinscripcion/solicitudes`, `PUT .../confirmar`, todos `requireAdmin()`). `GET /api/reinscripcion/estatus/:matricula` ahora revisa primero si hay una observación `PENDIENTE` antes de calcular regular/irregular; `POST /api/reinscripcion/solicitud` también la rechaza con 409 como defensa adicional del servidor.
- **Por qué ahora era seguro construirlo:** en la Fase 1 se dejó `ReinscripcionEstado.BLOQUEADO` ya definido en el código pero inalcanzable — no existía ninguna fuente de datos para activarlo. Esta fase le da esa fuente real.
- **Decisión de diseño — quién puede quedar "denegado":** una observación resuelta como `RECHAZADA` dejó de bloquear al alumno en la app (mismo tratamiento que `AUTORIZADA`) — el texto real del SIIA solo describe el caso de éxito ("el director... te autoriza la reinscripción si aun puedes"), no un flujo formal de denegación dentro de la app; se optó por no inventar ese comportamiento y dejar la decisión de "no autorizada" como un registro histórico que la institución maneja fuera de la app, no como un bloqueo permanente adicional.
- **App:** `AdminProfileScreen` gana una tarjeta "Observaciones de reglamento" (listar, registrar, autorizar/rechazar) por alumno — es donde tiene sentido, ya que el director revisa el caso de un alumno específico. Pantalla nueva `AdminReinscripcionesScreen` (sección "Reinscripciones" del Centro de Control) para que control escolar confirme las solicitudes pendientes de procesar presencialmente.
- **Verificado end-to-end con curl** antes de tocar la app: crear observación → GET estatus regresa BLOQUEADO con el motivo real → resolver (AUTORIZADA) → GET estatus regresa a REGULAR/IRREGULAR normalmente. 76/76 pruebas unitarias en verde.

## DEC-025 — Recuperar contraseña: el correo de destino nunca lo elige quien hace la solicitud

- **Fecha:** 15/09/2026
- **Vigencia:** ⛔ Sustituida — recuperar contraseña se quitó de la app (DEC-043).
- **Decisión:** `POST /api/recuperar-password/solicitar` recibe solo `matricula` — nunca un correo. El backend siempre manda el código al `Alumnos.CorreoOtp` ya guardado para esa cuenta, y solo regresa una versión enmascarada (`k***n@gmail.com`) para que la app la muestre. `POST /api/recuperar-password/confirmar` valida el código (misma tabla `dbo.OtpHistorial`, mismas reglas de 10 min / 5 intentos / DEC-022) y actualiza `dbo.AlumnoCredenciales` con `hashPassword` (scrypt), igual que `/api/registro`.
- **Por qué es distinto del OTP de login:** en el login, el alumno ya demostró conocer la contraseña (o pasó la validación SIIA) *antes* de llegar al paso de OTP — ahí sí es seguro dejarlo escribir cualquier correo, porque el OTP es un segundo factor sobre algo que ya se probó. En recuperación de contraseña el punto de partida es "olvidé mi contraseña" — si se dejara escribir el correo libremente, conocer solo la matrícula de alguien más (dato mucho menos secreto) bastaría para robarle la cuenta mandándose el código a la dirección propia.
- **Alcance:** solo alumnos con cuenta propia (`AlumnoCredenciales.Estado = 'REGISTRADO'`) — quienes solo entran vía SIIA no tienen contraseña que este backend controle. Si la matrícula no tiene cuenta propia (404) o no tiene `CorreoOtp` guardado (409, típico de un alumno sincronizado del SIIA que nunca puso correo), el endpoint lo rechaza con un mensaje claro en vez de fallar en silencio.
- **Verificado end-to-end con curl**: solicitar → confirmar con código incorrecto (rechazado) → confirmar con código correcto → login real con la contraseña nueva funciona → matrícula sin cuenta propia da el error correcto. 85/85 pruebas unitarias en verde.

## DEC-026 — AppTESCHI deja de depender del SIIA real: se retira por completo del login

- **Fecha:** 15/09/2026
- **Vigencia:** ⛔ Sustituida — desde DEC-043 la app depende por completo del SIIA, a pedido del alumno.
- **Decisión:** Se eliminó `SiiaAuthService.kt` (scraping del portal real vía Jsoup/OkHttp) y todo su uso. La cascada de login (`AuthRepository.iniciarSesion`) pasa de 4 pasos (bypass de prueba → admin real → cuenta propia → SIIA) a 3: bypass de prueba → admin real → cuenta propia. Si la cuenta propia falla, se rechaza directo — ya no hay ningún intento contra un sistema externo. `LoginResultado.SiiaRequiereOtp` se eliminó del sealed class. También se quitó la dependencia de Jsoup del proyecto (`build.gradle.kts`, `libs.versions.toml`) — nada más la usaba.
- **Por qué — pedido explícito del alumno:** "no quiero que dependa de el siia mi aplicación, quiero que mi aplicación sea independiente con registros míos, creados por mí, hechos por mí y todo por mí". La pregunta que lo originó fue el propio alumno notando que, aunque el resto de AppTESCHI ya era 100% real contra su propia base de datos, el login todavía tenía un camino que llamaba al sitio real de la escuela.
- **Consecuencia real para los usuarios:** un alumno que **no** tenga una cuenta propia registrada en AppTESCHI (vía `/api/registro`) ya no puede iniciar sesión con sus credenciales reales del SIIA — tiene que registrarse primero dentro de la app. Esto es intencional: es justo el punto de independencia que se pidió.
- **No cambia:** el backend nunca scrapeaba el SIIA (eso vivía enteramente en el cliente Android) — no hubo cambios en `server.js` por esta decisión. El resto del sistema (Reinscripción, Calificaciones, Kardex, panel de administrador) ya no tenía ninguna dependencia del SIIA desde antes de esta decisión.
- **Documentación:** la nota `SIIA_Scraping` (borrada el 27/09/2026 al ordenar la bóveda) y la sección "Arquitectura Real de Sesión" de [[2FA_Login]] se marcaron como históricas — se conservan como referencia de cómo funcionaba, no describen nada activo. 83/83 pruebas unitarias en verde tras el retiro.

## DEC-027 — Perfil del alumno: solo el correo de recuperación es autoeditable, la contraseña exige la actual

- **Fecha:** 16/09/2026
- **Vigencia:** ⛔ Sustituida — el perfil solo permite cambiar el NIP del SIIA (DEC-043).
- **Decisión:** Nueva pantalla `PerfilAlumnoScreen` (accesible desde el chip de arriba a la derecha del Dashboard, que antes solo cerraba sesión). Tres endpoints nuevos en `server.js`, protegidos con `x-api-key` igual que `/api/mi-historial` (el alumno no tiene JWT propio, ver DEC-020): `GET /api/mi-perfil/:matricula` (datos completos, solo lectura), `PUT /api/mi-perfil/:matricula` (solo acepta `correoOtp`), `POST /api/mi-perfil/cambiar-password` (exige `passwordActual` + `passwordNueva`, valida la actual con `verifyPassword` antes de actualizar).
- **Por qué solo el correo es editable:** nombre completo, carrera y semestre son datos oficiales que ya controla el director vía `PUT /api/usuarios/:matricula` (panel de administrador) — dejar que el propio alumno los cambie permitiría que se "renombrara" o se cambiara de carrera desde la app. El correo de recuperación no tiene ese riesgo (solo afecta a dónde le llega su propio código de recuperación), así que sí se dejó autoeditable.
- **Por qué cambiar la contraseña pide la actual en vez de un código:** a diferencia de `/api/recuperar-password` (DEC-025, pensado para cuando el alumno *no puede* entrar), aquí el alumno ya está autenticado dentro de una sesión — pedirle un código por correo sería fricción innecesaria. Conocer la contraseña actual es prueba suficiente de identidad en este caso, igual que en cualquier "cambiar contraseña estando logueado" estándar.
- **Verificado end-to-end con curl** antes de tocar la app, usando una cuenta de prueba desechable (`TESTPERFIL1`, creada y eliminada en la misma sesión): GET perfil inicial → PUT correo válido → GET refleja el cambio → cambiar contraseña con la actual correcta → login real con la contraseña nueva funciona → login con la vieja falla. También validados los rechazos: sin `x-api-key` (401), correo con formato inválido (400), contraseña actual incorrecta (401).
- **App:** `PerfilAlumnoService` (nuevo), `PerfilAlumnoViewModel` (nuevo, mismo patrón que `RecuperarPasswordViewModel`), `PerfilAlumnoScreen` (nueva, `Routes.PERFIL_ALUMNO`). El botón "Cerrar sesión" se movió del chip del Dashboard a esta pantalla.

## DEC-028 — Barra de navegación inferior flotante para los módulos del alumno

- **Fecha:** 16/09/2026
- **Vigencia:** ✅ Vigente, con 5 pestañas desde DEC-043 (sin "Reinscrip.").
- **Decisión:** El Dashboard dejó de mostrar los módulos como una rejilla de tarjetas — ahora es solo la pantalla general "Inicio" (header + hero de bienvenida). Reinscripción, Tira de Materias, Calificaciones, Intersemestral y Kardex pasaron a ser destinos de una barra de navegación inferior flotante y persistente (`BottomNavBar.kt`, nuevo), visible en los 6 tabs. `MainActivity.kt` envuelve el `NavHost` único de la app en un `Scaffold` externo cuyo `bottomBar` solo se muestra cuando la ruta activa está en `TABS_DASHBOARD` — el resto de pantallas (login, admin, perfil, recuperar contraseña, registro) no la ven.
- **Por qué — pedido explícito del alumno:** compartió una captura de referencia de otra app con una barra inferior tipo "pill" flotante y pidió el mismo estilo, pero con sus propios 6 módulos en vez de los de la referencia, más una pantalla de Inicio general y "una animación de transición muy fluida" al cambiar de tab.
- **Navegación sin perder estado:** se usó el patrón estándar de Navigation-Compose para bottom nav — `navController.navigate(destino) { popUpTo(Routes.DASHBOARD){ saveState = true }; launchSingleTop = true; restoreState = true }`. Esto evita que el back stack crezca sin límite al saltar entre tabs (siempre queda como máximo `[DASHBOARD, tabActual]`) y conserva el estado (scroll, etc.) de un tab ya visitado.
- **Animación:** el `NavHost` (uno solo para toda la app, no uno anidado) define `enterTransition`/`exitTransition`/`popEnterTransition`/`popExitTransition` a nivel global. Una función `direccionTransicionTabs()` compara la posición de la ruta de origen y destino dentro de `TABS_DASHBOARD`: si ambas son tabs de la barra, la transición desliza horizontalmente en la dirección real del cambio (izquierda↔derecha) combinada con un fundido; para cualquier otra navegación (login → dashboard, dashboard → perfil, flujos de admin) usa un fundido con ligera escala. Dentro de `BottomNavBar`, el ítem seleccionado también anima su color (`animateColorAsState`) y un ligero aumento de escala del ícono (`animateFloatAsState` + `graphicsLayer`).
- **Por qué no se tocó el interior de cada pantalla de módulo:** `ReinscripcionScreen`, `TiraMateriasScreen`, `CalificacionesScreen`, `KardexScreen` conservan su propio `Scaffold`/`TopAppBar` con flecha de "regresar" tal cual estaban — no fue necesario adaptarlas. Se verificó que esa flecha (`navController.popBackStack()`) sigue siendo segura: como el patrón de navegación de arriba deja el back stack siempre como `[DASHBOARD, tabActual]`, "regresar" desde cualquier tab termina en Inicio, nunca saca al alumno de la sesión hacia Login.
- **Etiquetas abreviadas:** con 6 tabs en una sola fila (vs. 5 en la referencia), "Reinscripción", "Calificaciones" e "Intersemestral" se abreviaron a "Reinscrip.", "Calif." e "Intersem." para que quepan en una línea sin encimarse — abreviaturas comunes en apps escolares mexicanas, no invención arbitraria.
- **Verificado visualmente en el dispositivo real:** capturas de pantalla vía `adb screencap` confirmaron el renderizado final (barra flotante con sombra, tab activo en verde negrita, transición de fundido capturada a medio cuadro confirmando que anima) antes de reportar la tarea como terminada. 85/85 pruebas unitarias en verde (este cambio es puramente de UI/navegación, sin lógica nueva que cubrir con pruebas).

## DEC-029 — Horario de clases real: importación por Excel desde el panel de administrador

- **Fecha:** 16/09/2026
- **Vigencia:** ⛔ Sustituida — sin horarios: el SIIA no los expone (DEC-043).
- **Decisión:** Nueva tabla `dbo.Horarios` (IdCarrera, Semestre, Grupo opcional, NombreMateria, DiaSemana 1-7, HoraInicio, HoraFin, Profesor, Aula, Modalidad, PeriodoEtiqueta, CargadoPor). Se llena exclusivamente subiendo un Excel desde `AdminHorariosScreen` (`POST /api/horarios/importar`, `multipart/form-data`, campo `archivo` + `claveCarrera`/`semestre`/`grupo`/`periodoEtiqueta`) — nunca capturando materias a mano ni generando datos de ejemplo. Columnas esperadas en la fila 1 del Excel: `Materia`, `Dia`, `HoraInicio`, `HoraFin`, `Profesor`, `Aula`, `Modalidad` (esta última opcional; detección de encabezados sin distinguir mayúsculas/acentos). Volver a subir un Excel reemplaza por completo el horario anterior de esa carrera+semestre+grupo (`DELETE` seguido de `INSERT` dentro de una transacción).
- **Por qué — pedido explícito del alumno:** al pedir que Inicio mostrara una sección "Materias y Horario de Hoy" (inspirada en una captura de referencia que compartió), se le preguntó explícitamente cómo resolver que no existía ninguna fuente real de día/hora/profesor/aula en la base de datos — su respuesta fue pedir un mecanismo real de carga desde el panel de administrador ("que se puedan subir los horarios, se analicen y se puedan ver"), no una simulación.
- **Librerías nuevas del backend:** `multer` (subida de archivo en memoria, nunca a disco, límite 5 MB) y `exceljs` (parseo del `.xlsx`). Se evaluó `xlsx` (SheetJS) primero pero tiene una vulnerabilidad alta sin parche disponible en npm (prototype pollution + ReDoS); `exceljs` no tiene vulnerabilidades de severidad alta y es la elección más segura para procesar un archivo subido por un usuario (aunque sea un admin autenticado).
- **`GET /api/mi-horario/:matricula`** (protegido con `x-api-key`, igual que `/api/mi-historial` — ver DEC-020) resuelve la carrera+semestre reales del alumno y regresa las clases del día de hoy con un campo `estado` calculado en el servidor (`EN_CURSO`/`PROXIMA`/`TERMINADA`, comparando la hora actual contra `HoraInicio`/`HoraFin`) para que la app no tenga que duplicar esa lógica de horas.
- **Limitación conocida, aceptada explícitamente:** el filtro es solo por carrera+semestre, no por grupo específico — igual que Reinscripción y Tira de Materias (ver 02_MODULOS/01_Reinscripcion.md), `Alumnos` no guarda a qué grupo pertenece cada alumno. Si algún día hay más de un grupo por semestre con horarios distintos, este endpoint los mezclaría.
- **App:** `HorarioAdminService.kt` (primer y único lugar de la app que sube un archivo — `MultipartBody` en vez de JSON), `HorarioService.kt` (alumno, `horarioDeHoyDelAlumnoEnSesion()` sigue el mismo patrón que `historialDelAlumnoEnSesion()` — los usuarios de prueba sem1..sem9 ven el horario vacío, nunca llaman al backend), `AdminHorariosScreen.kt` (nueva sección "Horarios" del Centro de Control: selector de carrera/semestre, selector de archivo vía `ActivityResultContracts.GetContent()`, lista del horario cargado con opción de eliminar por fila).
- **Verificado end-to-end con curl** antes de tocar la app: generación de un Excel de prueba con `exceljs`, importación real (3 filas), verificación de la lista de admin, verificación de `/api/mi-horario` regresando vacío en un día sin clases, reemplazo al volver a subir, y limpieza completa de los datos de prueba (igual que otras veces en este proyecto, nunca se deja información fabricada en la base real). Validaciones probadas: sin archivo (400), sin carrera/semestre (400), carrera inexistente (400), sin token de admin (401).

## DEC-030 — Sesión propia del alumno, ticket de login y cierre de dos agujeros de autenticación

- **Fecha:** 18/09/2026
- **Vigencia:** ⛔ Sustituida — la sesión es el token del SIIA guardado en el teléfono (DEC-043).
- **Contexto:** al preparar el despliegue en un servidor con salida a internet se revisó qué protegía realmente cada endpoint y aparecieron dos fallas que ya existían en el código (no eran nuevas de esta sesión):
  1. **OTP sin prueba de contraseña.** `POST /api/otp/enviar` aceptaba cualquier `matricula`/`usuario` y un `correo` elegido por quien llamaba, y `POST /api/otp/verificar` emitía el token de administrador sin que el servidor supiera si la contraseña se había validado (solo la app lo hacía, antes). Conociendo únicamente el usuario `admin`, cualquiera podía recibir el código en su propio correo y obtener un token de SUPERADMIN.
  2. **`POST /api/usuarios/sync`** (resto de la época del SIIA; la app ya no lo llamaba desde DEC-026) permitía, solo con la `x-api-key`, cambiar el correo y el nombre de cualquier alumno. Combinado con "recuperar contraseña" (que manda el código al correo guardado) era toma de cuenta.
  Además, los endpoints del alumno (`/api/mi-*`, `/api/reinscripcion/*`) se protegían con una `x-api-key` fija compilada en el APK y recibían la matrícula por parámetro: quien conociera una matrícula podía leer o modificar los datos de ese alumno.
- **Decisión:**
  - **Ticket de login:** `/api/auth/cuenta` y `/api/auth/administrador` responden además con un `ticket` (JWT, 10 min, audiencia propia). `/api/otp/enviar` y `/api/otp/verificar` lo exigen y comprueban que sea del mismo tipo e identificador; sin él, 401.
  - **Token de sesión del alumno:** `/api/otp/verificar` ahora emite también un token para el alumno (antes solo para el administrador), 12 h por defecto (`JWT_ALUMNO_EXPIRA_EN`). El middleware `requireAlumno()` protege `/api/mi-perfil`, `/api/mi-historial`, `/api/mi-horario` y `/api/reinscripcion/estatus|solicitud`: si la matrícula pedida (ruta o cuerpo) no es la de la sesión responde 403.
  - **Audiencias separadas:** ticket, administrador y alumno llevan `aud` distinta. Antes `requireAdmin()` aceptaba **cualquier** token firmado con el secreto (salvo que se pidiera un rol), así que un token de alumno habría abierto el panel de administración. Consecuencia aceptada: los tokens de administrador emitidos antes del cambio (máx. 12 h) dejan de valer y se vuelve a iniciar sesión una vez.
  - **Se elimina `/api/usuarios/sync`**, la `x-api-key` (servidor, `BuildConfig`, `ApiConfig`, `local.properties`) y `hasValidApiKey()`: ya no protegen nada y dejar una clave dentro del APK daba una falsa sensación de seguridad.
  - **IP real detrás de un túnel:** `clientIp(req)` usa `CF-Connecting-IP` solo si `TRUST_CLOUDFLARE=true`; y `HOST` permite escuchar solo en 127.0.0.1. Las dos van juntas: con la API alcanzable directo, ese header se puede falsificar. Sin esto, tras el túnel todos los usuarios comparten la IP 127.0.0.1 y el límite de intentos deja de servir.
  - **Arranque seguro:** la API no inicia si `JWT_SECRET` falta o tiene menos de 32 caracteres.
- **App Android:** `LocalAccountAuthService`/`AdminAccountAuthService` devuelven el ticket; `OtpService`, `AuthRepository` y `AuthViewModel` lo llevan hasta enviar/verificar; `UserSession.alumnoToken` (persistido) y `conSesionAlumno()` reemplazan el header `x-api-key` en los cuatro servicios del alumno. `SesionAlumno` (cliente HTTP compartido) detecta 401 con el header `X-Sesion-Invalida` — que solo manda `requireAlumno()`, no otros 401 como "contraseña actual incorrecta" — borra la sesión local y `AppNavigation` manda al login con un aviso; sin esto, con el token vencido cada pantalla quedaba en "error + Reintentar" sin salida (Perfil ni siquiera mostraba "Cerrar sesión" en ese estado). Los usuarios de prueba sem1..sem9/alumno no tienen token: sus llamadas al backend responden 401 y el interceptor no las trata como sesión vencida.
- **Verificado:** instancia aparte del backend (puerto 4100, SMTP apuntando a un puerto muerto para que no salga ningún correo; el código se recuperó de su hash en la base) con 37 comprobaciones, todas en verde: OTP y verificación sin ticket → 401; ticket de otra cuenta / de otro tipo / vencido → 401; `sync` → 404; vieja `x-api-key` → 401; sesión de un alumno pidiendo otra matrícula → 403 en perfil, historial, horario, estatus, edición de correo y cambio de contraseña; token de alumno en endpoints de admin y viceversa → 401; token de admin sin audiencia (formato anterior) → 401; contraseña actual incorrecta → 401 **sin** `X-Sesion-Invalida`; flujo completo de administrador. Datos de prueba eliminados al final y `UltimoAcceso` del administrador restaurado. 104/104 pruebas unitarias en verde (4 nuevas: el ticket viaja a enviar y verificar tanto de alumno como de administrador, el token del alumno llega a `onExito`, y `volverAlLogin` descarta el ticket).
- **Pendiente / riesgo conocido, sin resolver aquí:** `POST /api/registro` es público y, si la matrícula ya existe como perfil creado por un administrador sin cuenta todavía, completa sus datos y le asigna la contraseña que envíe quien llama — cualquiera que conozca (o adivine) una matrícula sin reclamar puede quedarse con esa cuenta. Antes de abrir la API a internet hace falta una prueba de identidad adicional (por ejemplo, fecha de nacimiento ya registrada por el administrador o un código de alta). Tampoco hay límite por matrícula independiente de la IP en `/api/auth/*`, y `Postman_Collection_AppTeschi.json` está desactualizado.

## DEC-031 — Backend y base de datos pasan a un servidor Ubuntu en casa (acceso privado por LAN y Tailscale)

- **Fecha:** 19/09/2026
- **Vigencia:** ⛔ Sustituida — la app ya no usa servidor; se puede apagar (DEC-043).
- **Decisión:** el backend (`AppTeschi.Api`) y la base `AppTeschiDB` dejan de vivir en la laptop de desarrollo y pasan a un servidor Ubuntu Server 26.04.1 LTS (7 GB de RAM, comparte máquina con n8n y Nextcloud). La API corre como servicio `systemd` (`appteschi-api`, usuario sin login, Node 22.16.0 en `/opt/node`); SQL Server 2022 Express corre en Docker publicado solo en `127.0.0.1`. Por ahora **no hay acceso desde internet**: solo LAN (`192.168.0.252`) y Tailscale (`100.111.170.32`). La laptop sigue como entorno de desarrollo con su propia base, no sincronizada.
- **Por qué:** el backend dependía de que la laptop estuviera prendida, de un túnel de Cloudflare efímero que cambiaba de URL (y fallaba por el DNS de la red) y de estar en la misma Wi-Fi.
- **SQL Server en Docker y no nativo:** la documentación de Microsoft solo llega a Ubuntu 24.04 (SQL Server 2025) y no menciona 26.04; el contenedor no depende del host. Se eligió 2022 Express (gratis y permitido en producción; el 2019 de la laptop es Developer, que no lo es). Restaurar el `.bak` de 2019 en 2022 convierte la base sola (versión interna 904 → 957).
- **La API ya no usa `sa`:** `02-restaurar-bd.sh` crea el login `appteschi_api` con `db_owner` solo en `AppTeschiDB`; la contraseña se genera al vuelo y se escribe en el `.env` del servidor (modo 600).
- **Respaldos:** `cron` diario 03:15 (`BACKUP … WITH CHECKSUM` + `RESTORE VERIFYONLY`, comprimido, 14 días). Decisión del alumno: copiarlos además a otro equipo por Tailscale — **pendiente**. Hoy están en el mismo disco.
- **Firewall inactivo a propósito:** se propuso permitir solo la LAN y Tailscale al puerto 4000, pero con `ufw` inactivo esas reglas no hacen nada, y activarlo en un servidor con SSH, n8n y Nextcloud puede dejar al dueño sin acceso; además `ufw` no controla los puertos que publica Docker. El puerto solo es alcanzable desde la LAN y Tailscale porque el router no redirige puertos. Al abrirlo a internet la API escuchará solo en `127.0.0.1` (ver DEC-030).
- **Fase pública (siguiente):** dominio propio + Cloudflare Tunnel con nombre. Antes hay que cerrar `POST /api/registro` (DEC-030). El alumno creará la cuenta de Cloudflare y comprará el dominio.
- **Kit:** `07_BACKEND/AppTeschi.Api/deploy/` (ver [[03_Manual_Tecnico_Instalacion]] §8). Se probó de punta a punta y aparecieron tres fallos, ya corregidos en el kit: `install -o` rechazó el UID numérico de `mssql` (probablemente por los coreutils de Ubuntu 26.04), nunca se había corrido `npm ci` en el servidor (la API entraba en bucle con `Cannot find module 'express'`) y el `.env` venía con saltos CRLF de Windows (`PORT=4000\r` rompía la comprobación final). Ninguno afectó a los datos.
- **Verificado:** con la API en el servidor, desde la laptop por LAN y por Tailscale: `/health` ok, `GET /api/mi-perfil/<matrícula>` sin sesión → 401 con el mensaje de DEC-030, `POST /api/usuarios/sync` → 404 y el catálogo público devuelve las 9 carreras de la base restaurada. El alumno confirmó después que la app "conectada al servidor" funcionaba con normalidad — pero al revisarlo (19/09/2026) se vio que **la app seguía apuntando a la laptop**: el APK traía compilada `http://192.168.0.41:4000`, no había ninguna URL guardada en "⚙ Servidor" y el backend de la laptop seguía corriendo, así que "funcionaba" contra la base local y en la escuela no habría llegado a ningún lado. Corrección: `API_BASE_URL` de fábrica = `http://100.111.170.32:4000` (IP de Tailscale del servidor: sirve en casa y fuera de casa mientras el teléfono tenga Tailscale encendido); se recompiló e instaló, y se detuvo el backend de la laptop. Verificado: la única dirección :4000 dentro del APK instalado es la del servidor, y **desde el propio teléfono** (`nc` por adb, interfaz `tun1` de Tailscale) el servidor respondió `/health` con la base de datos. Lección: una app "que funciona" no prueba a qué backend está hablando — comprobar la URL efectiva y apagar el backend anterior. Nota: lo hecho desde el respaldo (18/09, 21:58) contra la laptop quedó solo en la base de la laptop.
- **Arranque automático de la laptop desactivado (18/09/2026):** un acceso directo en la carpeta de Inicio de Windows (`arrancar-backend-teschi.bat - Acceso directo.lnk`) abría una consola con `npm start` en cada encendido, es decir, la laptop volvía a levantar un backend contra su SQL Server local. Se movió (no se borró) a `%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup_desactivados\` y se detuvo el proceso que corría. Para reactivarlo basta devolver el acceso directo a `Startup`; para levantar el backend de desarrollo a mano, ejecutar el `.bat` (queda intacto en la raíz del proyecto Android). No había servicio de Windows ni tarea programada equivalente; el servidor sigue respondiendo `/health` con `AppTeschiDB`.

## DEC-032 — Datos personales fuera del repositorio; los datos reales viven solo en la base del servidor

- **Fecha:** 19/09/2026
- **Vigencia:** ✅ Vigente. Desde DEC-043 no hay base propia: los datos reales viven en el SIIA y en el teléfono del alumno; el repositorio sigue sin datos personales.
- **Contexto:** al preparar el primer commit grande se vio que el repositorio de GitHub (`KevinSV-asv/App-Teschi`) es **público** y que el commit inicial (ya subido) contenía la matrícula, el nombre completo y las calificaciones reales de un alumno (`HistorialAcademico.kt`, `KardexPdfFormatoTest.kt`). Además, en el árbol de trabajo había un volcado de la sesión del teléfono (`sesion.xml`) con un **token de sesión real**, más capturas con datos académicos: eran archivos temporales de pruebas por `adb`, nunca se subieron.
- **Decisión:**
  1. Los tests usan un alumno **ficticio** (matrícula `2099000001`, "Alumno de Prueba Uno") con calificaciones sintéticas sobre las 53 materias del plan de estudios (información pública). Los promedios esperados se calculan a partir del fixture en vez de fijarse al Kardex oficial de una persona.
  2. El script `20_HistorialAcademicoRealKevin.sql` sale del repositorio y se guarda en `04_BASE_DATOS/privado_no_subir_a_git/` del vault. Los datos reales ya viven en la base del servidor (comprobado el 19/09/2026 vía API: 53 materias y el perfil del alumno, igual que las 53 filas del script). El respaldo diario 03:15 los protege.
  3. `.gitignore` ahora excluye `sesion.xml`, `ui_*.xml`, `*_check.png` y los scripts de arranque locales (`arrancar-backend-teschi.bat`, `iniciar-appteschi.ps1`, con rutas de esta PC y la IP de la laptop). El valor de respaldo de `API_BASE_URL` pasa de la IP de la laptop a `10.0.2.2` (loopback del emulador); el real sigue en `local.properties`.
  4. Regla de trabajo: nunca guardar volcados de sesión ni capturas con datos reales dentro del proyecto; usar el directorio temporal de la sesión.
- **Verificado:** 104/104 pruebas unitarias, compilación debug correcta y escaneo del commit (`862234d`) sin nombre, matrícula, correo, JWT, contraseñas ni IPs.
- **Pendiente (decisión del dueño):** el commit inicial público sigue teniendo la matrícula, el nombre y las calificaciones. Quitarlos de verdad exige o bien volver **privado** el repositorio (no borra lo que ya se vio) o reescribir el historial y forzar el envío (`git push --force`), lo que sobrescribe el remoto. No se hizo sin autorización. Un fork o clon existente conservaría los datos.

## DEC-033 — Errores de red comprensibles y cliente HTTP común

- **Fecha:** 21/09/2026
- **Vigencia:** ✅ Vigente; desde DEC-043 los mensajes hablan del SIIA y ya no mencionan Tailscale.
- **Contexto:** estando fuera de casa, el login mostró el texto crudo de Java (`failed to connect to /100.111.170.32 (port 4000) from /10.90.105.0 … after 10000ms`). La causa real no era la app ni el servidor (que respondía `/health` con `AppTeschiDB`, comprobado desde la laptop por Tailscale): el Tailscale del teléfono llevaba 2 días desconectado (`offline, last seen 2d ago` en `tailscale status`), así que la IP privada del servidor no era alcanzable desde datos móviles.
- **Decisión:** nuevo `service/ClienteHttp.kt` con un interceptor (`TraduceErroresDeRed`) que convierte los `IOException` de red en mensajes para el alumno: timeout/conexión rechazada → "No se pudo conectar con el servidor. Revisa tu conexión a internet y que Tailscale esté activo…" (la mención de Tailscale solo aparece si el host está en 100.64.0.0/10); host que no resuelve → "No se encontró el servidor…"; cualquier otro → "Se perdió la conexión…". Los 12 servicios, `SesionAlumno` y `RegistroViewModel` usan ese cliente. Se conserva la causa original en la excepción.
- **Verificado:** 109/109 pruebas (5 nuevas en `ClienteHttpTest`) y compilación debug correcta. Pendiente instalarlo en el teléfono (no estaba conectado por adb).
- **Nota operativa:** mientras el acceso sea solo por Tailscale, la app **requiere** que la VPN del teléfono esté activa fuera de casa. En Android conviene activar VPN siempre activa para Tailscale y quitarle la optimización de batería para que el sistema no la cierre; la solución de fondo es la fase pública con Cloudflare Tunnel (DEC-031).

## DEC-034 — Correo del OTP por Microsoft Graph (cuenta institucional), con SMTP de respaldo

- **Fecha:** 21/09/2026
- **Vigencia:** ⛔ Sustituida — sin OTP por correo (DEC-043).
- **Contexto:** la institución entregó una app registrada en Microsoft Entra para enviar el correo del OTP desde una cuenta `@teschi.edu.mx` en lugar del Gmail personal. Solo se recibieron el valor del secreto y el "Secret ID". El tenant de `teschi.edu.mx` es público (`dcec6cc1-cc36-4bdf-a82c-eaddb2de00ea`, por `openid-configuration`; el MX apunta a Exchange Online). Se comprobó contra Microsoft que el "Secret ID" **no** es el client ID (`AADSTS700016`, aplicación no encontrada): sigue faltando el **Application (client) ID**, que no se puede deducir, y el buzón remitente.
- **Decisión:** nuevo `correo-graph.js` (sin dependencias nuevas, usa `fetch` de Node 22) con el flujo *client credentials* (`/oauth2/v2.0/token` → `POST /users/{buzón}/sendMail`), token en caché hasta 1 min antes de vencer, y errores que traen el código de Microsoft pero nunca el secreto. `server.js` usa Graph solo si están las cuatro variables `MS_TENANT_ID`, `MS_CLIENT_ID`, `MS_CLIENT_SECRET`, `MS_SENDER`; con alguna faltante avisa en el arranque y sigue por SMTP (Gmail) como hasta ahora. Variables documentadas en `.env.example`. El secreto vive solo en el `.env` del servidor (modo 600); no está en el repositorio (público) ni en la app.
- **Verificado:** `node --check`, 5 pruebas de `scripts/test_correo_graph.js` (formato del token y de `sendMail`, caché del token, error sin secreto, 403 por falta de `Mail.Send`) y arranque del backend en los tres modos (sin variables → SMTP, parcial → aviso + SMTP, completo → Graph). **No** se envió ningún correo real por Graph: faltan el client ID y el buzón. Tampoco está desplegado en el servidor.
- **Requisitos del lado de Microsoft (a confirmar con quien entregó la app):** permiso *Microsoft Graph → Application → Mail.Send* con consentimiento de administrador; buzón con licencia de Exchange Online; idealmente una *Application Access Policy* que limite la app a ese único buzón (sin ella, `Mail.Send` de aplicación permite enviar como cualquier usuario del tenant); fecha de caducidad del secreto (renovarlo antes).

## DEC-035 — Login con la API oficial del SIIA, desde el backend; asistencia fuera de alcance

- **Fecha:** 22/09/2026 (planeada) · 25/09/2026 (en producción para alumnos)
- **Vigencia:** ⚠️ Modificada por DEC-041 (29/09/2026, NIP de 4 dígitos), DEC-042 (30/09/2026, calificaciones parciales) y DEC-043 (12/10/2026: el login va directo del teléfono al SIIA, ya no por el backend).
- **Contexto:** la escuela publicó una API REST oficial (`https://siia.teschi.edu.mx/Teschi/api`, con Swagger). Ya no hace falta scrapear el portal: `POST /login.ashx` valida usuario y contraseña y devuelve los datos de la persona (`tipo`, `idCarrera`, correo…). Detalle en [[API_SIIA_Oficial]].
- **Decisión:** `POST /api/auth/siia` llama a `login.ashx` **desde el servidor** (nunca desde el teléfono), clasifica por `tipo` y, si procede, emite el mismo ticket de login de DEC-030. De ahí en adelante el flujo es el de siempre (OTP → JWT propio). El token y la contraseña del SIIA no se guardan.
- **Alcance:** solo `login.ashx`. La API también trae endpoints de asistencia de docentes, pero el alumno decidió dejarlos fuera ("eso de la asistencia no es necesario"). Tampoco puede reemplazar a `AppTeschiDB`: no expone calificaciones, kardex, horarios ni reinscripción.
- **Modifica:** DEC-026 solo en el login.
- **Primer intento descartado (23/09/2026):** `server.js` seguía comparando contra una columna local (`Alumnos.UsuarioSiia`) en vez de llamar al SIIA, así que todo login respondía "usuario o contraseña incorrectos" (BUG-015). Se reescribió con `https` nativo (`loginSiiaReal()`).

## DEC-036 — El SIIA no da de alta cuentas: solo entran alumnos que ya existen en AppTESCHI

- **Fecha:** 25/09/2026
- **Vigencia:** ⛔ Sustituida — sin registro local (DEC-043).
- **Decisión:** si el SIIA valida a un alumno pero su matrícula no está en `Alumnos`, el backend responde **404** "Tu matrícula todavía no está registrada en AppTESCHI. Contacta al administrador…" y lo deja en el log (`[AUTH-SIIA][NO-REGISTRADO]`). Si sí existe, solo se guarda `Alumnos.UsuarioSiia` en su primer login.
- **Por qué — pedido explícito del alumno:** "No quiero que le des de alta si no existe; según yo las APIs deben de funcionar con los datos reales del SIIA". La versión del 23/09 creaba el registro automáticamente con los datos del SIIA (`OrigenCuenta = 'SIIA'`); se retiró antes de usarse con nadie.
- **Consecuencia:** el administrador sigue siendo quien decide quién tiene acceso (alta desde el panel, DEC-017). `Alumnos.OrigenCuenta` queda en el esquema sin uso.
- **Correo del OTP:** en el mismo cambio, `/api/otp/enviar` pasó a usar primero el correo guardado en el servidor (`CorreoOtp` / `CorreoInstitucional`); el del cliente solo cuenta si la cuenta aún no tiene ninguno.

## DEC-037 — Carrera del alumno enlazada con el `idCarrera` del SIIA

- **Fecha:** 27/09/2026
- **Vigencia:** ⛔ Sustituida — sin base local; la carrera se muestra tal como la da el SIIA (DEC-043).
- **Decisión:** cuando un alumno sin carrera entra por el SIIA, el backend busca su `idCarrera` en `CatalogoCarreras.IdCarreraSiia`; si la carrera está mapeada, la asigna sola (`[AUTH-SIIA][CARRERA-ENLAZADA]`); si no, responde `cuentaPendiente` y registra `[AUTH-SIIA][PENDIENTE] … idCarreraSiia=N area="…"` para poder mapearla después.
- **Por qué:** el SIIA no tiene catálogo de carreras; el único modo de saber qué número usa para cada una es verlo en el login de alguien que la cursa. Así el mapeo se completa carrera por carrera sin pedirle a nadie que vuelva a intentar.
- **Estado:** ISC = 4 (confirmado con un login real). Las otras 8 carreras, pendientes. Código en el commit `acd8ffa`, **todavía sin desplegar** en el servidor al 27/09/2026.
- **SQL:** `25_MapeoCarrerasSiia.sql`. La columna ya la había creado `24_VinculoSiia.sql`; al correr la primera versión del script (entonces `24_MapeoCarrerasSiia.sql`) se creó un índice único duplicado, que la versión actual quita.

## DEC-038 — Un solo formulario de login (sin pestañas "Cuenta AppTESCHI" / "SIIA")

- **Fecha:** 28/09/2026
- **Vigencia:** ✅ Vigente: un solo formulario, ahora solo para la cuenta del SIIA (DEC-043).
- **Decisión:** `LoginScreen` deja de tener dos pestañas. Un solo campo "Matrícula o usuario" + contraseña; `AuthRepository.iniciarSesion` prueba en cascada administrador → cuenta propia → SIIA (vía `/api/auth/siia`).
- **Por qué — pedido explícito del alumno:** "que ya solo sea uno solo, conservando las funciones".
- **Reglas de la cascada:** sin conexión con el servidor no se intenta el SIIA; si el SIIA solo dice "credenciales incorrectas", se muestra el mensaje de AppTESCHI; cualquier otro mensaje del SIIA se muestra tal cual. Una cuenta de administrador válida nunca llega al SIIA.
- **Costo aceptado:** un alumno que solo tiene usuario del SIIA hace primero dos consultas que fallan (administrador y cuenta propia) contra el propio servidor; son locales y rápidas. Un error de contraseña de un administrador también consume un intento en el SIIA (5 por minuto por usuario + IP).
- **Pruebas:** 5 nuevas en `AuthRepositoryTest`; ningún test hace llamadas reales al SIIA (fake inyectado).

## DEC-039 — Alta automática de alumnos del SIIA en su primer login

- **Fecha:** 28/09/2026
- **Vigencia:** ⛔ Revertida el mismo día, antes de desplegarse. Lo que el alumno quería quedó en DEC-040.
- **Qué se hizo:** `/api/auth/siia` creaba en `Alumnos` a todo alumno que el SIIA validara y no existiera, y una carrera sin mapear dejaba de bloquear el acceso.
- **Por qué se revirtió — aclaración del alumno:** "no quiero que se cree en automático porque se van a crear varios registros que no quiero… en caso de que no esté en mi AppTeschi, no se pueda logear, se quede en que no está registrado y un administrador después tendrá que [darlo de alta]". El pedido original ("que funcione con alumnos registrados en el SIIA") describía el aviso de "no registrado" que ve un alumno real no dado de alta, que es justo el comportamiento de DEC-036.
- **Lo que sí se conservó:** la corrección de BUG-022 (alumno registrado sin correo), independiente del alta automática.
- **Dato útil que quedó confirmado:** la API oficial del SIIA solo tiene `login.ashx` y asistencia (Swagger revisado el 28/09/2026): no expone calificaciones, kardex, semestre ni horario.

## DEC-040 — Cualquier alumno del SIIA entra, sin que se cree ningún registro suyo

- **Fecha:** 28/09/2026
- **Vigencia:** ⛔ Sustituida — sin registro local, todo alumno del SIIA entra (DEC-043).
- **Modifica:** DEC-036 (el no registrado ya no se rechaza; la parte de "nunca crear registros" sigue).
- **Decisión:** si `login.ashx` acepta a un alumno (`tipo = E`) que no está en `Alumnos`, `/api/auth/siia` responde con ticket, `correoEnmascarado` (del correo del SIIA; vacío si no trae) y `sinRegistro: true`, y **no escribe nada en la base**. Sus datos del SIIA (nombre, correo, carrera) y su código OTP viven solo en memoria del backend (`sesionesSinRegistro`), con las mismas reglas de 10 min, 5 intentos y reuso del código (DEC-019, DEC-022). Al verificar recibe un token de alumno con `sinRegistro: true`, `idAlumno: null` y esos datos. La carrera es el nombre del catálogo si su `idCarrera` ya está mapeado (DEC-037) o, si no, el `area` que da el SIIA.
- **Qué ve el alumno:** Inicio y Perfil con su nombre, matrícula, carrera y correo del SIIA. Kardex, Calificaciones, Tira de materias, horario y Reinscripción responden 409 "Entraste con tu cuenta del SIIA, pero aún no tienes registro en AppTESCHI: tu información académica aparecerá cuando control escolar te dé de alta". Cambiar correo o contraseña y pedir reinscripción también responden 409: nada de lo que haga escribe en la base.
- **Por qué — pedido explícito del alumno:** quería que el login funcionara con cualquier usuario real del SIIA ("a eso me refería que adaptaras la aplicación a las APIs") pero sin altas automáticas ("se van a crear varios registros que no quiero", DEC-039). Se le preguntaron las tres opciones con sus consecuencias y eligió "entra sin crear registro".
- **Límite que no depende de AppTESCHI:** la API del SIIA no da calificaciones, kardex, semestre ni horario (Swagger revisado el 28/09/2026); esos datos solo existen cuando el administrador da de alta al alumno. Si lo da de alta, su siguiente inicio de sesión ya es normal.
- **Seguridad:** el token sin registro usa la audiencia de alumno, así que solo sirve en `requireAlumno()` y siempre para su propia matrícula (403 en otra); no abre el panel de administrador. Los datos del SIIA no viajan en el ticket (se quedan en el servidor hasta verificar el código). Si el backend se reinicia, esas sesiones pendientes se pierden y el alumno vuelve a iniciar sesión.
- **Nota (29/09/2026):** con el NIP de DEC-041 el alumno sin registro ya no pasa por el código por correo; su código en memoria solo se usa si el SIIA no pide NIP.
- **Verificado:** prueba local con SIIA simulado (HTTPS propio), la base de desarrollo y el envío de correo interceptado (ningún correo real): 26/26. Incluye que no se crea ninguna fila en `Alumnos` ni en `OtpHistorial`, el código incorrecto/correcto, "ya tengo un código" tras volver a entrar, los 409 de todos los módulos, 403 con otra matrícula, 401 en el panel, alumno sin correo en el SIIA, y que los alumnos registrados siguen igual (incluido `cuentaPendiente`). Datos de prueba borrados. Desplegado por el alumno el 28/09/2026.

## DEC-041 — Login del SIIA con NIP de 4 dígitos (API 1.1); el NIP sustituye al código por correo

- **Fecha:** 29/09/2026
- **Vigencia:** ✅ Vigente; desde DEC-043 el NIP se manda directo al SIIA desde el teléfono.
- **Modifica:** DEC-030 y DEC-035 solo para usuarios del SIIA: el segundo factor pasa a ser el NIP del SIIA en lugar del código por correo de AppTESCHI. Administradores y cuentas propias siguen con código por correo.
- **Contexto:** la API del SIIA pasó a la versión 1.1.0. `login.ashx` sin NIP ya no entrega el token final: valida la contraseña y devuelve `requiereNip`, `tieneNip` y un `tempToken` de 10 min que se completa en el nuevo `/nip.ashx` (verificar, configurar la primera vez, cambiar, recuperar por CURP). Detalle en [[API_SIIA_Oficial]].
- **Decisión:**
  - `/api/auth/siia`: si el SIIA pide NIP, guarda en memoria del backend el `tempToken` (máx. 10 min) y responde `{ requiereNip, tieneNip, ticketNip }`. `ticketNip` es un identificador aleatorio: el `tempToken` del SIIA nunca llega al teléfono.
  - `POST /api/auth/siia/nip`: `{ ticketNip, nip }` verifica; `{ ticketNip, nuevoNip, confirmarNip }` crea el NIP en el SIIA. Si el SIIA lo acepta, el backend clasifica al usuario con sus datos (`usuarioInfo`) igual que antes (registrado, sin registro de DEC-040, cuenta pendiente, docente) y **entrega directo el token de sesión de AppTESCHI**, sin código por correo. El token final del SIIA se descarta (DEC-035 sigue: no se guarda).
  - `POST /api/auth/siia/nip/recuperar`: `{ ticketNip, curp }` pide al SIIA que mande el NIP al correo institucional. Solo muestra el correo enmascarado si la CURP es de quien inició sesión.
  - Si el SIIA no pidiera NIP (respuesta estilo 1.0), sigue el flujo anterior con código por correo.
  - `tipo = P` es docente según el Swagger 1.1 (antes el código suponía `D`).
- **Por qué — pedido explícito del alumno:** el SIIA real ya pide NIP y la app no lo soportaba. Se le presentaron tres recomendaciones y eligió aplicar la primera ("hagamos el punto 1 por mientras"): pedir el NIP y quitar el código por correo para quienes entran por el SIIA. Las otras dos (calificaciones parciales del SIIA y guardar su token en memoria) quedan pendientes de su decisión.
- **Límites:** 5 NIP incorrectos por inicio de sesión (luego hay que volver a escribir la contraseña); 10 intentos cada 5 min y 3 recuperaciones cada 10 min por IP + usuario (no solo por IP, para no bloquear a todos los alumnos detrás de la misma red de la escuela). El SIIA aplica además sus propios límites (429).
- **Sin confirmar con una cuenta real:** si el paso 1 del SIIA trae el bloque `usuario`. El backend funciona en ambos casos (usa `usuarioInfo` del paso NIP) y lo registra en el log como `usuarioEnPaso1=…`.
- **Verificado:** prueba local con un SIIA simulado que imita la API 1.1, la base de desarrollo y el correo interceptado: 27/27 (NIP correcto e incorrecto, crear NIP con y sin coincidencia, 409 si ya tenía, alumno registrado / sin registro / sin carrera / docente `P`, límite de 5 intentos, ticket de un solo uso, recuperar por CURP, flujo sin NIP con código por correo, que ni el `tempToken` ni el token final del SIIA salgan al teléfono o al log). App: 125/125 pruebas (7 nuevas). Datos de prueba borrados. **Falta desplegar y probar con una cuenta real.**
- **Nota (30/09/2026):** el token final del SIIA ya no se descarta: se guarda en memoria para las calificaciones parciales (DEC-042).

## DEC-042 — Calificaciones parciales del SIIA en el módulo Calificaciones; su token se guarda solo en memoria

- **Fecha:** 30/09/2026
- **Vigencia:** ⚠️ Modificada por DEC-043: el token del SIIA se guarda en el teléfono y la consulta va directo al SIIA.
- **Modifica:** DEC-035 ("el token del SIIA no se guarda") y la nota de DEC-041 que lo descartaba.
- **Decisión:** al verificar el NIP (DEC-041), el backend guarda el token final del SIIA **solo en memoria** (`sesionesSiia`, hasta los 480 min que da el SIIA) y firma el token de sesión de AppTESCHI con un `sid` aleatorio que lo referencia. Nuevo `GET /api/mi-calificaciones-siia?periodo=` (`requireAlumno()`): consulta `GET /calificaciones/parciales.ashx` del SIIA con ese token y devuelve periodo, periodos disponibles, resumen y, por materia, profesor, grupo, parciales 1–3, promedio parcial y estatus. En la app, **Calificaciones** muestra esas parciales (con selector de periodo) cuando el alumno entró con el SIIA (`UserSession.sesionSiia`); si entró con cuenta propia, muestra las de AppTeschiDB como antes.
- **Por qué — pedido explícito del alumno:** "adapta el API de calificaciones a mi aplicación". El endpoint del SIIA solo acepta el token final del propio alumno, que se obtiene después del NIP; sin guardarlo no hay forma de consultarlo después del login. Se le había presentado como recomendación el 29/09/2026 (guardar el token del SIIA solo en memoria del servidor durante la sesión).
- **Casos:**
  - Bloqueo por trámites (403 del SIIA): la app muestra "Calificaciones bloqueadas en el SIIA" con la lista de trámites pendientes; no es un error.
  - Token del SIIA vencido o API reiniciada: 401 con `X-Sesion-Invalida` y la app manda a iniciar sesión otra vez.
  - Sesión sin `sid` (cuenta propia): 409 `SIN_SIIA` y la app usa las calificaciones de AppTeschiDB, sin cerrar sesión.
  - También funciona para alumnos sin registro local (DEC-040): ven sus parciales reales aunque no estén dados de alta.
- **Seguridad:** el token del SIIA nunca sale del servidor ni se escribe en la base o el log; al teléfono solo le llega `sid`, dentro de su JWT firmado. El `sid` solo sirve para la matrícula con la que se creó. El `periodo` se valida antes de mandarlo al SIIA. Límite: 30 consultas por minuto por IP + matrícula.
- **Límite conocido:** son parciales por periodo, no el kardex oficial; Kardex sigue saliendo de AppTeschiDB. Cerrar sesión en la app no borra el token del servidor (vence solo a las 8 h).
- **Verificado:** prueba local con SIIA simulado (login, NIP y calificaciones) y la base de desarrollo: 15/15 (parciales, cambio de periodo, periodo inválido, alumno sin registro, bloqueado, token vencido y borrado de memoria, sesión sin SIIA, `sid` ajeno, sin token, nada de tokens del SIIA en respuestas ni log) y la prueba de NIP (27/27) sigue pasando. App: 133/133 pruebas (8 nuevas). **Falta desplegar y verlo con una cuenta real.**

---

## DEC-043 — La app habla directo con el SIIA: sin backend propio ni base de datos

- **Fecha:** 12/10/2026
- **Vigencia:** ✅ Vigente, salvo el Kardex "en construcción" (la API 1.2 ya lo da, DEC-044) y la pestaña de Reinscripción, que volvió (DEC-046).
- **Sustituye:** DEC-031 (servidor en casa con Tailscale) y todo lo que dependía de `AppTeschi.Api` / `AppTeschiDB`: cuentas propias, administradores, OTP por correo, registro, recuperar contraseña, Reinscripción, horarios y el Kardex de la base. **Modifica:** DEC-035, DEC-041 y DEC-042 (las mismas llamadas al SIIA, ahora desde el teléfono).
- **Decisión:** la app Android llama directo a la API oficial del SIIA (`https://siia.teschi.edu.mx/Teschi/api`) y deja de usar el backend y la base.
  - **Login:** usuario + contraseña a `login.ashx`; después el NIP en `nip.ashx` (`verificar`, `configurar` la primera vez y `recuperar` con la CURP). Solo entra `tipo = E` (alumno); docentes y administrativos ven "Por ahora AppTESCHI es solo para alumnos…".
  - **Sesión:** el token final del SIIA (480 min) y los datos de la cuenta se guardan en el teléfono (`UserSession`, en SharedPreferences excluidas del respaldo). Con el token vigente la app abre directo en Inicio; si el SIIA responde 401, la sesión se borra y la app manda al login.
  - **Calificaciones:** `calificaciones/parciales.ashx` con `Bearer`, con las mismas reglas que tenía el backend: si el SIIA falla con el periodo actual se prueban los recientes, y el bloqueo por trámites se muestra como aviso.
  - **Tira de materias:** se arma con las materias de las parciales del periodo actual (materia, profesor, grupo), porque el SIIA no tiene un endpoint de tira.
  - **Inicio:** saludo con los datos del SIIA y el resumen del periodo actual (promedio parcial, materias, evaluadas). Ya no hay horario de hoy.
  - **Perfil:** datos del SIIA (solo lectura), cambio de NIP (`nip.ashx`, `cambiar`) y cerrar sesión.
  - **Kardex e Intersemestral:** pantalla "Estamos trabajando en este apartado". La propuesta de Kardex para el profesor es el diseño de Stitch (`09_ANALISIS_Y_DISENO/08_Kardex_Propuesta_Stitch.html`).
  - **Quitados de la app:** panel de administrador completo, Reinscripción, Registro, recuperar contraseña, OTP por correo, cuentas propias, usuarios de prueba (`alumno`, `sem1`…`sem9`), diálogo "⚙ Servidor", ruta de docente y PDF del Kardex. La barra inferior queda con 5 pestañas.
- **Por qué — pedido explícito del alumno (12/10/2026):** "Ya no quiero que llegue al servidor, ya no se va a ocupar ya que solo se va a trabajar con APIs reales del SIIA Original, entonces elimina cualquier dependencia". En la misma sesión eligió quitar el registro público, quitar de la app los módulos sin datos en el SIIA, entrar con contraseña + NIP, y dejar el Kardex "en construcción" con la propuesta de Stitch.
- **Alternativas descartadas:** Cloudflare Tunnel con dominio propio (seguía necesitando el servidor); `login-nip.ashx` (matrícula + NIP, sin contraseña): el alumno prefirió contraseña + NIP.
- **Consecuencias:**
  - Ya no hace falta Tailscale ni tener el servidor encendido: la app funciona con cualquier conexión a internet.
  - Lo que solo existía en AppTeschiDB (historial capturado por el administrador, Reinscripción, horarios, auditoría) ya no se ve en la app.
  - El token del SIIA vive en el teléfono (antes solo en memoria del servidor, DEC-042). Se excluye del respaldo en la nube y de la copia entre teléfonos, y cerrar sesión lo borra.
  - `usesCleartextTraffic="false"` (solo HTTPS); se quitaron los permisos de ubicación y el `FileProvider` del PDF.
- **Qué pasa con el backend:** el código de `07_BACKEND/AppTeschi.Api/`, los scripts de `04_BASE_DATOS/` y sus notas se conservan en el repositorio **solo como histórico**; nada de la app los usa. El servidor de casa se puede apagar.
- **Verificado:** 86/86 pruebas unitarias, incluidas pruebas HTTP contra un SIIA simulado (MockWebServer): cuerpos de `login.ashx` y `nip.ashx`, `Bearer`, reintento de periodos y 401 que cierra la sesión. `assembleDebug` correcto. El SIIA responde directo desde internet (401 "Token requerido…" sin token, comprobado el 12/10/2026). **Falta:** probarlo en el teléfono con una cuenta real (no había dispositivo conectado).

---

## DEC-044 — Calificaciones finales y Kardex del SIIA en la app (API 1.2)

- **Fecha:** 02/11/2026
- **Vigencia:** ✅ Vigente, salvo "la CURP no se muestra": el alumno pidió los mismos datos del original (DEC-045).
- **Modifica:** DEC-043 (Kardex "Estamos trabajando en este apartado"). El contrato del login de DEC-041 y DEC-043 **no cambia**.
- **Decisión:** la app usa tres endpoints nuevos de la API 1.2 (especificación del 29/10/2026), directo al SIIA con `Bearer`:
  - **Calificaciones > Finales** (`GET calificaciones/finales.ashx?periodo=`): selector Parciales / Finales dentro del módulo Calificaciones; por materia, 1ra vuelta, 2da vuelta, calificación final y estatus; resumen con promedio final y dictamen (Aprobatorio / Regularizar / En Curso). Se piden hasta abrir esa vista y el periodo elegido se conserva al cambiar entre Parciales y Finales.
  - **Kardex** (`GET calificaciones/kardex.ashx`): pantalla con el diseño de Stitch que eligió el alumno ([[07_Prompt_Stitch_Kardex]]): datos del alumno, promedio global, anillo de créditos, contadores, gráfica de promedio por semestre, filtro por semestre y materias con cada oportunidad (Ordinario, Recurse, Especial, Global).
  - **Descargar PDF** (`GET calificaciones/kardex-pdf.ashx?base64=1`): el alumno elige dónde guardarlo con el selector de Android y puede abrirlo al terminar.
- **Por qué:** pedido del alumno (02/11/2026): "ya me dieron mas apis que se deben de meter al sistema". Eran justo lo que faltaba desde DEC-043.
- **Decisiones de seguridad:**
  - El token va **siempre en el encabezado**. El SIIA también acepta `?token=` en la URL de `kardex-pdf`, pero ahí quedaría en registros e historiales.
  - La app nunca manda `matricula`: el SIIA toma al alumno del token.
  - La **CURP** y la foto que trae el kardex no se leen ni se muestran. Nada del kardex se guarda en el teléfono, salvo el PDF donde el alumno decida.
  - El contenido del PDF se valida (`%PDF`) y el nombre de archivo que manda el SIIA se limpia antes de proponerlo.
- **Sin el sello del diseño:** el "Documento Oficial Validado" de Stitch no se puso: la app no certifica nada. La pantalla dice "Esta vista es informativa. El documento oficial es el PDF."
- **Gráfica:** escala de 0 a 100 y un solo color; el diseño de Stitch alternaba colores sin significado y recortaba la escala, lo que exageraba las diferencias.
- **Alternativas descartadas:**
  - `login-nip.ashx` (matrícula + NIP, sin contraseña): se conserva contraseña + NIP, como eligió el alumno en DEC-043. No se ofreció cambiarlo aquí porque revertiría esa decisión.
  - `GET nip.ashx`: el paso 1 del login ya dice `tieneNip`.
  - Alias `kardex.ashx` y `kardex-pdf.ashx` en la raíz: hacen lo mismo que los de `calificaciones/`.
  - Asistencia: sigue fuera de alcance (solo alumnos).
- **Consecuencias:** sin permisos nuevos ni `FileProvider` (el selector de Android da el permiso sobre ese archivo). App **2.1** (código 4).
- **Verificado:**
  - 134/134 pruebas unitarias, con un SIIA simulado (MockWebServer).
  - Colección de Postman con Newman contra un SIIA falso local: 26/26 verificaciones.
  - App en emulador (Android 17) contra ese SIIA falso, con datos ficticios: login, Finales, Kardex, filtro, guardar y abrir el PDF, bloqueo por trámites, error con Reintentar y sesión vencida.
  - Endpoints reales: existen y responden 401 sin token.
- **⏳ Sin verificar con datos reales:** la forma de las respuestas sale de la especificación; falta una cuenta real (colección de Postman, [[01_POSTMAN_API]]). El lector acepta los nombres alternos del SIIA para tolerar diferencias.

---

## DEC-045 — El Kardex muestra los mismos datos que el historial académico oficial, incluida la CURP

- **Fecha:** 02/11/2026
- **Vigencia:** ✅ Vigente.
- **Modifica:** DEC-044 (la app no leía la CURP). El diseño de Stitch se conserva.
- **Por qué:** pedido del alumno, con una captura del encabezado del reporte oficial: "quiero que tenga exactamente los mismos datos que el original, el orden te lo dejo a tu eleccion". La captura trae datos reales: no se versiona.
- **Referencias:** el encabezado de esa captura y la lista de datos que la especificación 1.2 da como "tal cual el reporte oficial". **Solo se vio el encabezado del reporte**, no el resto de sus páginas.
- **Decisión:** se agregan a la pantalla:
  - Encabezado institucional: logos, nombre del TESCHI, Organismo, Dirección Académica, Departamento de Control Escolar, "Historial académico" y fecha de elaboración.
  - CURP y grupo en la tarjeta del alumno. Se usa "Período", como en el original.
  - Materias reprobadas y por aprobar en los contadores.
  - Total de materias, créditos por cursar y promedio del semestre anterior. Parcial y descuento solo si el SIIA los da.
  - En cada materia, su número en el reporte y la clave oficial (AP / NA / PC).
- **Supuesto:** la API no manda la fecha de elaboración; se usa el día de la consulta, que es lo que hace el reporte. Mismo formato: día-mes-año.
- **Privacidad:** la CURP se ve en pantalla, pero solo vive en memoria: no se guarda en el teléfono. La foto (`fotoUrl`) no se usa: no aparece en el encabezado del original.
- **Se mantiene:** sin sello de "documento oficial" y con la nota "Esta vista es informativa. El documento oficial es el PDF."
- **Logo:** `logo_teschi_encabezado.png` es el `logo_teschi.png` sin su fondo gris y a 480 px; el original no se tocó.
- **Verificado:** 138/138 pruebas; colección de Postman con Newman contra el SIIA falso, 27/27 (ahora comprueba que lleguen estos campos); pantalla revisada en emulador con datos ficticios.
- **⏳ Sin verificar con datos reales:** igual que en DEC-044, falta comprobar con una cuenta que lleguen estos campos y compararlos contra el PDF completo.

---

## DEC-046 — La pestaña Reinscripción sustituye a Intersemestral, y lleva Intersemestral dentro

- **Fecha:** 02/11/2026
- **Vigencia:** ✅ Vigente.
- **Modifica:** DEC-043 (Reinscripción se había quitado de la app).
- **Por qué:** se lo solicitaron al alumno (02/11/2026): "se tiene que cambiar intersemestral por reinscripción y meter intersemestral a reinscripción".
- **Decisión:**
  - En la barra inferior, "Reinscrip." toma el lugar exacto de "Intersem.": Inicio, Materias, Calif., **Reinscrip.**, Kardex.
  - La pantalla tiene un selector Reinscripción / Intersemestral, igual al de Calificaciones.
  - `Routes.REINSCRIPCION` sustituye a `Routes.INTERSEMESTRAL`. `ReinscripcionScreen.kt` sustituye a `EnConstruccionScreen.kt`.
- **Limitación:** la API 1.2 **no tiene datos de reinscripción ni de intersemestrales**, así que las dos secciones dicen "Estamos trabajando en este apartado". No se recuperó el flujo anterior: dependía de `AppTeschiDB`, que ya no se usa (DEC-043).
- **Verificado:** en emulador, las dos secciones y la pestaña.

---

## DEC-047 — "Seguridad de la Cuenta" en Mi perfil, con el diseño del portal del SIIA

- **Fecha:** 02/11/2026
- **Vigencia:** ✅ Vigente. Desde DEC-048 sí se puede cambiar la foto, y se quitó la insignia "SIIA · Sesión activa".
- **Por qué:** pedido del alumno, con capturas del portal del SIIA: "necesito que metas eso en mi perfil, ya que lo que se tiene actualmente esta horrible". Las capturas traen datos reales: no se versionan.
- **Decisión:** nuevo diseño de Mi perfil: encabezado verde con el alumno, correo, "Seguridad de la Cuenta" con selector Ver todo / Solo contraseña / Solo NIP, una tarjeta por formulario con requisitos, y cerrar sesión.
- **NIP — funciona:** `nip.ashx` (`cambiar`) no pide la contraseña, pero el diseño la pide "para autorizar". Para que el campo sea real, antes se comprueba con `login.ashx`; si es incorrecta, no se intenta cambiar el NIP.
- **Contraseña — no se puede todavía:** la API 1.2 **no tiene endpoint para cambiarla**. El formulario se ve como en el portal, pero **apagado**, para que nadie escriba su contraseña en un campo que no hace nada; un aviso lo explica y un botón abre el portal (`ApiConfig.SIIA_PORTAL_URL`). Cuando la escuela publique el endpoint, solo hay que encender los campos y conectar el botón.
- **Adaptado o fuera del diseño:**
  - Una columna, porque es teléfono, y "Solo NIP" en vez de "Solo NIP de Seguridad", por espacio.
  - Sin el campo sin etiqueta (ícono de persona) que sale en el portal entre "Contraseña actual" y "Nueva contraseña": no dice para qué es.
  - Sin la etiqueta "Si ya cuentas con uno" en el NIP actual: quien ya entró a la app tiene NIP y la API lo exige.
  - Sin menú lateral (la app usa su barra) ni cámara de la foto: la API no permite cambiarla.
- **Alternativa descartada:** llamar a un endpoint no documentado del portal para cambiar la contraseña. Debe salir de la API oficial; se le pide a la escuela.
- **Verificado:** 142/142 pruebas. En emulador contra un SIIA falso: contraseña incorrecta (no llega a `nip.ashx`), NIP incorrecto, cambio correcto (limpia los campos), secciones y botón del portal.
- **⏳ Sin verificar con datos reales:** falta comprobar con una cuenta que llamar a `login.ashx` antes de `cambiar` no invalide el token de la sesión. Si pasara, se quitaría esa comprobación.

---

## DEC-048 — Foto de perfil guardada en el teléfono; sin la insignia "SIIA · Sesión activa"

- **Fecha:** 02/11/2026
- **Vigencia:** ✅ Vigente.
- **Por qué:** pedido del alumno: "quiero que el usuario pueda cambiar su foto de perfil" y "elimines la parte de SIIA Sesion activa, eso no es necesario que lo sepan mis usuarios".
- **Decisión:** en Mi perfil, tocar la foto o la cámara abre el selector de fotos de Android. Con foto puesta, un menú deja cambiarla o quitarla.
- **Límite importante:** la API 1.2 **no permite subir fotos**, así que la foto **solo existe en ese teléfono**: no aparece en el portal del SIIA, y se pierde al desinstalar la app o cambiar de teléfono. Para compartirla con el SIIA haría falta un endpoint de la escuela.
- **Cómo se guarda:** `data/FotoPerfil.kt`, en el almacenamiento privado de la app: JPEG cuadrado de 512 px, centrado y enderezado según la orientación de la cámara.
- **Privacidad:**
  - Selector de Android: sin permisos de almacenamiento; la app solo accede a la imagen elegida.
  - Se vuelve a dibujar: no conserva metadatos del original, como la ubicación GPS.
  - Excluida del respaldo en la nube y de la copia entre teléfonos.
  - Nunca sale del teléfono.
  - **Por cuenta:** el nombre del archivo es el hash de la matrícula. Cerrar sesión no la borra, así que reaparece al volver a entrar, y otro alumno en el mismo teléfono no la ve.
- **Alternativa descartada:** agregar una librería de imágenes: basta con lo que trae Android.
- **Sin cambio:** tomar la foto con la cámara no se incluyó; solo se elige de la galería.
- **Verificado:** 155/155 pruebas y recorrido en emulador con imágenes ficticias (detalle en v0.39). No depende del SIIA, así que no queda nada por comprobar con una cuenta real, salvo probarla en un teléfono físico.
