# Arquitectura de la API de AppTeschi

> ⛔ **Histórico (12/10/2026, DEC-043):** la app ya no usa `AppTeschi.Api`; habla directo con la API del SIIA. Se conserva en el repositorio solo como referencia.

> Actualizado 27/09/2026 (autenticación por JWT, SIIA y Microsoft Graph). Reescrito el 09/09/2026: antes describía una cola offline que ya no existe (DEC-017). Para el panorama completo del sistema (app + backend + base de datos + servicios externos), ver [[04_Diagrama_Componentes]] y [[05_Diagrama_Despliegue]].
> **Relacionado con:** [[Base_Datos_Usuarios]], [[2FA_Login]], [[API_SIIA_Oficial]], [[01_POSTMAN_API]], [[DECISIONES_TECNICAS]]

## Objetivo

`AppTeschi.Api` es la única capa autorizada para hablar con SQL Server. La app Android **nunca se conecta directamente a la base de datos** — todo pasa por esta API. Tampoco habla con el SIIA: es el backend quien llama a su API oficial, solo para validar contraseñas (ver [[API_SIIA_Oficial]]).

## Principios de diseño

- Toda escritura pasa por validación explícita antes de tocar la base de datos (existencia de FKs referenciadas, rangos numéricos, campos obligatorios).
- Las operaciones administrativas requieren un token de administrador (`Authorization: Bearer`, `requireAdmin()`, algunas solo `SUPERADMIN`); las del alumno, su propio token (`requireAlumno()`, solo su matrícula). Ya no existe la `x-api-key` (DEC-030).
- Toda operación que borra o modifica datos relevantes queda registrada en `AuditoriaMovimientos` mediante `writeAudit(...)`.
- Las operaciones que tocan varias tablas relacionadas (por ejemplo, eliminar un alumno) se ejecutan dentro de una transacción SQL — o se aplican por completo, o no se aplica nada.
- SQL Server es la única fuente de verdad; la app no mantiene una copia local que deba sincronizarse después.

## Flujo actual (petición síncrona, sin cola)

1. La app arma una solicitud HTTP (JSON) hacia la URL configurada en `ApiConfig.kt`.
2. `server.js` valida el token (cuando aplica) y el cuerpo de la solicitud.
3. `server.js` ejecuta la operación contra `AppTeschiDB` usando el pool de conexiones de `mssql`.
4. Si la operación tiene efecto administrativo relevante, se registra en `AuditoriaMovimientos` antes de responder.
5. `server.js` responde con `{ ok: true, ... }` o `{ ok: false, error: "..." }` y el código HTTP correspondiente.
6. La app traduce la respuesta a `Result.success`/`Result.failure` y actualiza su estado de UI.

> No existe ninguna cola local ni reintento automático de sincronización — si la petición falla (sin red, timeout, error del servidor), la app muestra el error y el usuario debe reintentar manualmente. Esto es una simplificación deliberada: la complejidad de una cola offline no se justificaba una vez que el panel de administrador dejó de depender de sincronización diferida (ver DEC-017).

## Componentes

### App móvil

- Arma las solicitudes HTTP con el cliente común `ClienteHttp` (DEC-033) desde objetos `*Service` (autenticación, OTP, perfil, historial, horario, reinscripción y los del panel de administrador).
- Traduce cada respuesta a `Result<T>` y a *data classes* Kotlin — ver [[02_Diagrama_Clases]].
- No guarda copia local persistente de estos datos; solo la sesión (`UserSession`).

### API (`server.js`)

- Recibe solicitudes HTTP (Express).
- Valida el token de sesión (`requireAdmin()` / `requireAlumno()`, audiencias separadas) y el cuerpo de la solicitud; límites de intentos por IP (`excedeLimite`).
- Ejecuta la lógica de negocio y las consultas SQL parametrizadas.
- Registra auditoría cuando aplica.
- Devuelve confirmación o error con código HTTP semántico (200/201, 400, 401, 404, 409, 500).

### Base de datos

- Esquema normalizado (3FN) — ver [[Base_Datos_Usuarios]] para el modelo completo.
- Tablas principales: `Alumnos`, `AlumnoCredenciales`, `Administradores`, `AdministradorCredenciales`, catálogos (`CatalogoCarreras`, `CatalogoSistemas`, `CatalogoPeriodos`, `CatalogoTurnos`, `CatalogoEstatusMateria`), dominio académico (`PlanEstudioMaterias`, `Grupos`, `HistorialAcademico`, `Horarios`, `SolicitudesReinscripcion`, observaciones de reglamento), auditoría (`AuditoriaMovimientos`), sesión/OTP (`OtpHistorial`, `SesionesLogin`).

## Integraciones externas

| Servicio | Para qué | Dónde |
|---|---|---|
| API oficial del SIIA (`login.ashx`) | Validar usuario y contraseña del SIIA en `/api/auth/siia` | `loginSiiaReal()`, `mapTipoSiia()` en `server.js` · [[API_SIIA_Oficial]] |
| Microsoft Graph | Enviar el correo del OTP desde la cuenta institucional | `correo-graph.js` (DEC-034) |
| SMTP (Gmail) | Respaldo del correo si faltan las variables `MS_*` | `nodemailer` en `server.js` |

## Regla de negocio general

La API debe aceptar únicamente payloads que pasen su propia validación (tipos correctos, rangos válidos, referencias existentes) — nunca confía en que el cliente ya validó, aunque la app también valide localmente para dar retroalimentación inmediata al usuario.

## Referencia completa de endpoints

Ver [[Base_Datos_Usuarios]] §"Endpoints de `server.js`" — tabla completa y actualizada de todos los endpoints, agrupados por dominio (autenticación, alumnos, calificaciones, auditoría, estadísticas, catálogo curricular).

## Módulos relacionados

- [07_BACKEND/AppTeschi.Api/server.js](../07_BACKEND/AppTeschi.Api/server.js)
- [04_BASE_DATOS/sql/](../04_BASE_DATOS/sql/) — todos los scripts de esquema y siembra, en orden de ejecución
