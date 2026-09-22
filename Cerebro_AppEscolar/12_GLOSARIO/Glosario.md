# Glosario — AppTESCHI

> Términos del negocio escolar y términos técnicos usados a lo largo de toda la documentación.
> **Relacionado con:** [[MAPA_PROYECTO]]

---

## Términos del negocio escolar

| Término | Definición |
|---|---|
| **Matrícula** | Identificador único de un alumno dentro de la institución (ej. `2022452166`). Equivale a la clave primaria de negocio de `Alumnos`. |
| **SIIA** | Sistema Institucional de Información Académica del TESCHI — portal ASP.NET donde vive oficialmente la información escolar. No expone API REST. |
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

## Términos técnicos

| Término | Definición |
|---|---|
| **MVVM** | *Model-View-ViewModel* — patrón de arquitectura de la app: la Vista (Composable) observa estado del ViewModel, que orquesta la lógica sin conocer detalles de UI. |
| **Jetpack Compose** | Framework declarativo de UI de Android usado en todo el proyecto (sin XML layouts). |
| **`ViewModel`** | Clase que sobrevive a recomposiciones/rotaciones, expone estado observable (`StateFlow`) y orquesta llamadas a servicios. |
| **`Result<T>`** | Tipo de Kotlin usado en toda la capa de servicios para representar éxito (`Result.success`) o fallo (`Result.failure`) sin lanzar excepciones hacia la UI. |
| **Mock / dato simulado** | Información generada en el propio cliente (no viene de la base de datos real) — usado en los módulos del alumno que todavía no tienen backend real. |
| **Endpoint** | Ruta HTTP expuesta por `AppTeschi.Api` (ej. `POST /api/usuarios`). |
| **`x-api-key`** | Cabecera HTTP compartida entre la app y el backend para autorizar operaciones administrativas. No es un esquema de sesión por usuario. |
| **Transacción (SQL)** | Conjunto de operaciones que se aplican todas o ninguna (`BEGIN TRANSACTION` / `COMMIT` / `ROLLBACK`) — usado en la eliminación de alumnos para no dejar datos huérfanos. |
| **Cascada (`ON DELETE CASCADE`)** | Regla de la base de datos que borra automáticamente filas dependientes cuando se borra la fila principal (ej. `AlumnoCredenciales` al borrar un `Alumno`). |
| **Normalización (3FN)** | Diseño de base de datos sin datos duplicados ni dependencias transitivas — cada dato vive en un solo lugar. Ver DEC-014. |
| **Clave foránea (FK)** | Columna que referencia la clave primaria de otra tabla, garantizando integridad referencial. |
| **Idempotente** | Una operación que produce el mismo resultado sin importar cuántas veces se ejecute — todos los scripts SQL de este proyecto están diseñados así. |
| **Scrypt** | Algoritmo de derivación de claves usado para convertir una contraseña en un hash irreversible antes de guardarla (`hashPassword`/`verifyPassword` en `server.js`). |
| **Web scraping** | Técnica de extraer datos de un sitio web simulando un navegador (peticiones HTTP + parseo de HTML) — usada para interactuar con el SIIA, que no tiene API. |
| **`CookieJar`** | Componente que guarda las cookies de sesión entre peticiones HTTP — en este proyecto, solo en memoria (`InMemoryCookieJar.kt`), nunca en disco. |
| **`__VIEWSTATE` / `__EVENTVALIDATION`** | Campos ocultos que usa ASP.NET Web Forms como estado de formulario y protección anti-CSRF — deben extraerse del HTML antes de cada `POST` al SIIA. |
| **Túnel (Cloudflare)** | Proceso que expone un servidor local (`localhost:4000`) bajo una URL pública HTTPS, sin necesidad de configurar un dominio o abrir puertos del router. |
| **API key vs. JWT** | La API key es una clave estática compartida (usada hoy); un JWT sería un token firmado por usuario con expiración — mejora de seguridad pendiente para producción. |
| **`BuildConfig`** | Clase generada por Gradle que expone valores de `local.properties` (URLs, claves) al código Kotlin en tiempo de compilación. |
| **Fake (prueba)** | Implementación simplificada de una interfaz (ej. `FakeCuentaAuthService`) usada solo en pruebas unitarias, para no depender de red real. |
| **RF / RNF** | Requerimiento Funcional / Requerimiento No Funcional — ver [[01_Requerimientos_Funcionales]] y [[02_Requerimientos_No_Funcionales]]. |
| **CU** | Caso de Uso — ver [[01_Casos_de_Uso]]. |
| **CP** | Caso de Prueba — ver [[02_Catalogo_Casos_de_Prueba]]. |
| **DEC-NNN** | Identificador de una Decisión Técnica documentada en [[DECISIONES_TECNICAS]]. |
