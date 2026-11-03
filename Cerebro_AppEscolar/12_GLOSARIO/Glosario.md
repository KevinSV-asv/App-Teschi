# Glosario — AppTESCHI

> Términos del negocio escolar y términos técnicos usados a lo largo de toda la documentación.
> **Relacionado con:** [[MAPA_PROYECTO]]

---

## Términos del negocio escolar

| Término | Definición |
|---|---|
| **Matrícula** | Identificador único de un alumno dentro de la institución (ej. `2099000001`). Equivale a la clave primaria de negocio de `Alumnos`. |
| **SIIA** | Sistema Institucional de Información Académica del TESCHI — donde vive oficialmente la información escolar. Tiene una API REST oficial que AppTESCHI usa solo para validar el inicio de sesión (ver [[API_SIIA_Oficial]]). |
| **Retícula** | Documento oficial que define qué materias corresponden a cada semestre de una carrera, con sus créditos. Fuente de verdad para sembrar `PlanEstudioMaterias`. |
| **Reinscripción** | Trámite semestral mediante el cual un alumno confirma su continuidad, elige grupo/turno (si es irregular) y sube su comprobante de pago. |
| **Tira de materias** | Documento/listado de las materias que un alumno debe cursar en el semestre actual. |
| **Kardex** | Historial académico completo de un alumno: todas las materias cursadas, su calificación, estatus y promedio. |
| **Intersemestral** | Periodo corto entre semestres regulares donde un alumno puede cursar o recursar una materia. |
| **Alumno regular / irregular** | Regular: va al corriente con su plan de estudios, el sistema le asigna grupo automáticamente. Irregular: tiene materias pendientes o adelantadas y debe elegir su grupo. |
| **Carrera** | Programa educativo (ej. Ingeniería en Sistemas Computacionales). Tiene una `Clave` corta (`ISC`) y un `Nombre` completo. |
| **Grupo** | Conjunto de alumnos que cursan juntos un semestre/turno de una carrera en un periodo dado. Clave con formato `{semestre}{carrera}{turno}{numero}` (ej. `9ISC23`). |
| **Turno** | Matutino o vespertino — parte de la clave del grupo. |
| **Periodo** | Ciclo escolar identificado por año + número (ej. 2026, periodo 2). |
| **Estatus de materia** | Aprobada (`AP`), No aprobó (`NA`) o Por cursar (`PC`) — deriva de si existe calificación y si es ≥ 6. |
| **Dar de baja** | Desactivar a un alumno (`Activo = 0`) sin borrar su información — reversible. |
| **Eliminar (alumno)** | Borrar permanentemente al alumno y todo su historial asociado — irreversible. |
| **OTP** | *One-Time Password* — código numérico de un solo uso enviado por correo como segundo factor de autenticación. |
| **Tipo (SIIA)** | Letra con la que el SIIA clasifica a una persona en su login: `E` = estudiante (confirmado); docente y administrativo por confirmar. |
| **`idCarrera` (SIIA)** | Número con el que el SIIA identifica una carrera; se guarda en `CatalogoCarreras.IdCarreraSiia` (ISC = 4). |

## Términos técnicos

| Término | Definición |
|---|---|
| **MVVM** | *Model-View-ViewModel* — patrón de arquitectura de la app: la Vista (Composable) observa estado del ViewModel, que orquesta la lógica sin conocer detalles de UI. |
| **Jetpack Compose** | Framework declarativo de UI de Android usado en todo el proyecto (sin XML layouts). |
| **`ViewModel`** | Clase que sobrevive a recomposiciones/rotaciones, expone estado observable (`StateFlow`) y orquesta llamadas a servicios. |
| **`Result<T>`** | Tipo de Kotlin usado en toda la capa de servicios para representar éxito (`Result.success`) o fallo (`Result.failure`) sin lanzar excepciones hacia la UI. |
| **Mock / dato simulado** | Información generada en el propio cliente (no viene de la base de datos real) — usado en los módulos del alumno que todavía no tienen backend real. |
| **Endpoint** | Ruta HTTP expuesta por `AppTeschi.Api` (ej. `POST /api/usuarios`). |
| **Ticket de login** | JWT de 10 minutos que el backend entrega al validar la contraseña; sin él no se puede pedir ni verificar el OTP (DEC-030). |
| **Token de sesión** | JWT de 12 horas (alumno o administrador) que la app manda como `Authorization: Bearer` en cada petición. |
| **Tailscale** | VPN privada con la que el teléfono y la PC llegan al servidor de casa (`100.111.170.32`) desde cualquier red. |
| **Microsoft Graph** | API de Microsoft 365 con la que el backend envía el correo del OTP desde la cuenta institucional (DEC-034). |
| **systemd / `appteschi-api`** | Servicio de Linux que mantiene corriendo el backend en el servidor y lo reinicia si se cae. |
| **`x-api-key`** | *(Histórico)* Cabecera con una clave fija compilada en la app; se eliminó en DEC-030 y se reemplazó por tokens de sesión. |
| **Transacción (SQL)** | Conjunto de operaciones que se aplican todas o ninguna (`BEGIN TRANSACTION` / `COMMIT` / `ROLLBACK`) — usado en la eliminación de alumnos para no dejar datos huérfanos. |
| **Cascada (`ON DELETE CASCADE`)** | Regla de la base de datos que borra automáticamente filas dependientes cuando se borra la fila principal (ej. `AlumnoCredenciales` al borrar un `Alumno`). |
| **Normalización (3FN)** | Diseño de base de datos sin datos duplicados ni dependencias transitivas — cada dato vive en un solo lugar. Ver DEC-014. |
| **Clave foránea (FK)** | Columna que referencia la clave primaria de otra tabla, garantizando integridad referencial. |
| **Idempotente** | Una operación que produce el mismo resultado sin importar cuántas veces se ejecute — todos los scripts SQL de este proyecto están diseñados así. |
| **Scrypt** | Algoritmo de derivación de claves usado para convertir una contraseña en un hash irreversible antes de guardarla (`hashPassword`/`verifyPassword` en `server.js`). |
| **Túnel (Cloudflare)** | Proceso que expone un servidor local (`localhost:4000`) bajo una URL pública HTTPS, sin necesidad de configurar un dominio o abrir puertos del router. Hoy no se usa: el backend vive en el servidor Ubuntu (DEC-031); se planea un túnel con nombre para la fase pública. |
| **API key vs. JWT** | La API key era una clave estática compartida; el JWT es un token firmado por usuario, con audiencia y expiración. AppTESCHI usa JWT desde DEC-020/DEC-030. |
| **`BuildConfig`** | Clase generada por Gradle que expone valores de `local.properties` (URLs, claves) al código Kotlin en tiempo de compilación. |
| **Fake (prueba)** | Implementación simplificada de una interfaz (ej. `FakeCuentaAuthService`) usada solo en pruebas unitarias, para no depender de red real. |
| **RF / RNF** | Requerimiento Funcional / Requerimiento No Funcional — ver [[01_Requerimientos_Funcionales]] y [[02_Requerimientos_No_Funcionales]]. |
| **CU** | Caso de Uso — ver [[01_Casos_de_Uso]]. |
| **CP** | Caso de Prueba — ver [[02_Catalogo_Casos_de_Prueba]]. |
| **DEC-NNN** | Identificador de una Decisión Técnica documentada en [[DECISIONES_TECNICAS]]. |
