# Requerimientos No Funcionales — AppTESCHI

> Cualidades del sistema, no funciones puntuales. Organizados por categoría ISO/IEC 25010 (calidad de producto de software).
> **Relacionado con:** [[00_Vision_y_Alcance]], [[01_Requerimientos_Funcionales]], [[03_Matriz_Trazabilidad]]

---

## Rendimiento (Performance Efficiency)

| ID | Descripción | Criterio de aceptación |
|---|---|---|
| RNF-001 | Las pantallas de la app deben mostrar un estado de carga (`CircularProgressIndicator`) mientras esperan respuesta de la API | Ninguna pantalla que consuma red queda en blanco durante la espera |
| RNF-002 | Las consultas al directorio de alumnos y a estadísticas deben responder en un tiempo razonable para una base de datos de tamaño institucional (miles de registros) | `GET /api/usuarios` y `GET /api/estadisticas` usan `GROUP BY`/`JOIN` indexados, no procesamiento en el cliente |
| RNF-003 | El pool de conexiones a SQL Server debe reutilizarse entre solicitudes, no abrir una conexión nueva por request | `getPool()` en `server.js` usa un pool único (`max: 10`) |

## Seguridad

| ID | Descripción | Criterio de aceptación |
|---|---|---|
| RNF-010 | Las contraseñas de cuentas propias nunca se almacenan en texto plano | `AlumnoCredenciales` guarda `ContrasenaHash`/`ContrasenaSalt` (scrypt), nunca la contraseña |
| RNF-011 | Todo endpoint del panel de administrador debe verificar la identidad real de quien llama, no solo una clave compartida | `requireAdmin(...)` valida un JWT firmado por el servidor (DEC-020); `writeAudit` usa esa identidad verificada, no headers declarados por el cliente |
| RNF-012 | La sesión ASP.NET del SIIA debe mantenerse solo en memoria, nunca persistirse en disco sin cifrar | `InMemoryCookieJar.kt` |
| RNF-013 | El código OTP debe generarse y verificarse en el servidor, nunca confiando en que el cliente lo compare correctamente | `POST /api/otp/enviar`/`verificar` (ver DEC-019) — la app nunca conoce el código correcto |
| RNF-014 | El campo de captura de OTP debe usar un teclado que no sugiera autocompletado del sistema | `KeyboardType.NumberPassword` |
| RNF-015 | Toda acción administrativa debe quedar auditada de forma que no pueda desactivarse desde la app | `writeAudit(...)` se ejecuta en el propio backend, no es opcional desde el cliente |
| RNF-016 | El bypass de administrador de desarrollo (`admin`/`admin`) debe estar excluido de cualquier build de release, no solo "documentado" | `if (BuildConfig.DEBUG)` en `AuthRepository.iniciarSesion` — corregido en DEC-019, antes funcionaba también en release |
| RNF-017 | Debe existir al menos una cuenta real de administrador respaldada por la base de datos, no solo un bypass local | `Administradores`/`AdministradorCredenciales`, `POST /api/auth/administrador` (DEC-019) |
| RNF-018 | El administrador debe pasar por el mismo segundo factor (OTP) que un alumno — ninguna cuenta real omite la verificación | `LoginResultado.AdministradorRequiereOtp` siempre exige OTP; solo el bypass de debug lo omite |
| RNF-019 | Las credenciales de servicios externos (SMTP) nunca deben distribuirse dentro del artefacto instalable (APK) | Envío de correo movido al backend (DEC-019); `local.properties` ya no contiene `EMAIL_APP_PASSWORD` |
| RNF-020 | Los endpoints de autenticación y de envío/verificación de OTP deben limitar el número de intentos por origen | `excedeLimite(...)` en `/api/auth/cuenta`, `/api/auth/administrador`, `/api/otp/enviar`, `/api/otp/verificar` — límite en memoria, documentado como insuficiente para más de una instancia del backend |
| RNF-021 | Los permisos de un administrador deben depender de un rol verificado por el servidor, no de lo que la interfaz decida mostrar u ocultar | `requireAdmin(['SUPERADMIN'])` en `DELETE /api/usuarios/:matricula` y en `/api/administradores`; la app solo oculta botones como conveniencia, el servidor rechaza igual aunque se llame directo (DEC-020) |
| RNF-022 | Debe existir una forma de dar de alta administradores adicionales sin depender de acceso a la terminal del servidor | `POST /api/administradores` (SUPERADMIN-only) + `AdminAdministradoresScreen` (DEC-020) |

## Usabilidad (Usability)

| ID | Descripción | Criterio de aceptación |
|---|---|---|
| RNF-020 | La interfaz completa debe estar en español | Todos los textos de UI, mensajes de error y documentación están en español |
| RNF-021 | Cada operación destructiva (eliminar alumno, dar de baja) debe pedir confirmación explícita antes de ejecutarse | `AlertDialog` de confirmación en `AdminAlumnosScreen` y `AdminProfileScreen` |
| RNF-022 | Los errores de red o validación deben mostrarse en el idioma y contexto del usuario, no como códigos HTTP crudos | Cada `Result.failure` se traduce a un mensaje legible antes de mostrarse |
| RNF-023 | El panel de administrador debe organizar sus funciones en secciones independientes (Alumnos, Calificaciones, Auditoría, Estadísticas) en vez de una sola pantalla saturada | `AdminDashboardScreen` como *hub* de navegación (ver DEC-017) |

## Disponibilidad y confiabilidad (Reliability)

| ID | Descripción | Criterio de aceptación |
|---|---|---|
| RNF-030 | Una eliminación de alumno debe ser atómica: si falla cualquier paso de la limpieza en cascada, no debe quedar el alumno a medio borrar | `DELETE /api/usuarios/:matricula` usa una transacción SQL con `rollback` ante error |
| RNF-031 | El sistema debe seguir funcionando (con datos parciales) si uno de dos servicios remotos falla — por ejemplo, mostrar el perfil aunque la auditoría no responda | `AdminProfileScreen` distingue error de perfil vs. error de auditoría y no bloquea la pantalla completa |

## Mantenibilidad (Maintainability)

| ID | Descripción | Criterio de aceptación |
|---|---|---|
| RNF-040 | La base de datos debe estar normalizada (3FN), sin datos duplicados ni redundantes | Ver DEC-014 y [[Base_Datos_Usuarios]] |
| RNF-041 | Toda decisión de arquitectura relevante debe documentarse con su razonamiento | [[DECISIONES_TECNICAS]] (DEC-001 → DEC-018 y siguientes) |
| RNF-042 | Todo cambio de código relevante debe quedar versionado en una bitácora legible | [[CAMBIOS_DE_CODIGO]] (v0.1 → v0.19 y siguientes) |
| RNF-043 | La lógica de red debe estar detrás de interfaces inyectables para permitir pruebas sin red real | `PlanEstudiosApi`, `CuentaAuthService`, `SiiaAuth` como interfaces con implementación real + *fakes* de prueba |
| RNF-044 | Los scripts SQL deben ser idempotentes (poder ejecutarse más de una vez sin error) | Todos los scripts en `database/sql/` usan `IF NOT EXISTS`/`IF OBJECT_ID(...) IS NULL` |

## Portabilidad (Portability)

| ID | Descripción | Criterio de aceptación |
|---|---|---|
| RNF-050 | La URL base y la API key del backend deben configurarse por entorno, sin hardcodear en el código Kotlin | `ApiConfig.kt` lee de `BuildConfig`, generado desde `local.properties` |
| RNF-051 | El backend debe poder apuntar a un servidor SQL distinto solo cambiando variables de entorno | `.env` con `DB_SERVER`, `DB_DATABASE`, `DB_USERNAME`, `DB_PASSWORD` |

## Escalabilidad (Scalability)

| ID | Descripción | Criterio de aceptación |
|---|---|---|
| RNF-060 | El esquema de base de datos debe soportar agregar nuevas carreras o materias sin cambios estructurales | `PlanEstudioMaterias`/`Grupos` referencian `CatalogoCarreras` por FK; sembrar una carrera nueva no requiere `ALTER TABLE` |
| RNF-061 | El sistema debe soportar múltiples administradores concurrentes sin condiciones de carrera visibles al usuario | El pool de conexiones y las transacciones de SQL Server garantizan consistencia; no hay estado compartido en el cliente |

## Auditabilidad y trazabilidad

| ID | Descripción | Criterio de aceptación |
|---|---|---|
| RNF-070 | Todo movimiento de auditoría debe conservar quién lo hizo (`ActorMatricula`/`ActorNombre`) como una fotografía de texto, no una referencia que cambie si el nombre del actor cambia después | Columnas de texto explícitas en `AuditoriaMovimientos`, ver DEC-014/017 |

## Verificabilidad / calidad de datos

| ID | Descripción | Criterio de aceptación |
|---|---|---|
| RNF-080 | Ningún dato curricular (créditos, materias, grupos) debe capturarse sin validarlo contra una fuente oficial | Metodología "no inventar" aplicada durante la siembra de las 7 carreras — ver [[DECISIONES_TECNICAS]] y comentarios en `database/sql/07_SeedMateriasIndustrial.sql` (materias con crédito no confirmado, documentadas explícitamente) |
| RNF-081 | Toda entrega de código debe compilar sin errores y pasar el 100 % de la suite de pruebas unitarias antes de generar el APK | Práctica seguida en cada iteración de este proyecto (ver [[CAMBIOS_DE_CODIGO]]) |
