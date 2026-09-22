# Manual de Mantenimiento — AppTESCHI

> Procedimientos recurrentes para evolucionar el sistema sin romper lo que ya funciona.
> **Relacionado con:** [[03_Manual_Tecnico_Instalacion]], [[REGLAS_PROYECTO]], [[DECISIONES_TECNICAS]]

---

## 1. Agregar una carrera nueva al catálogo curricular

1. Consigue la retícula oficial y los horarios/grupos reales de la carrera — **nunca inventar créditos ni claves de grupo**.
2. Inserta la carrera en `CatalogoCarreras` si no existe (clave corta, nombre completo).
3. Crea un script `NN_SeedMaterias<Carrera>.sql` con el `INSERT INTO PlanEstudioMaterias` de cada materia (nombre, créditos, semestre), siguiendo el patrón de los scripts existentes (`04_SeedMateriasAnimacion.sql`, etc.).
4. Valida el total de créditos capturados contra el total oficial de la retícula antes de darlo por bueno (técnica de suma cruzada — ver DEC-014/019 y el propio script como ejemplo).
5. Crea un script `NN_SeedGrupos<Carrera>.sql` con los grupos reales (`Clave`, `Semestre`, `Turno`, `Numero`, `Periodo`), siguiendo el formato `{semestre}{claveCarrera}{turno}{numero}`.
6. Ejecuta ambos scripts con `sqlcmd -f 65001` (ver [[03_Manual_Tecnico_Instalacion]] §2.1).
7. Verifica con `GET /api/plan-estudios/<clave>` y `GET /api/grupos/<clave>` que los datos se sirven correctamente.
8. Documenta la carrera nueva en [[Base_Datos_Usuarios]] y en [[CAMBIOS_DE_CODIGO]].

## 2. Agregar un endpoint nuevo al backend

1. Define el contrato: método HTTP, ruta, si requiere `x-api-key`, payload de entrada, forma de la respuesta.
2. Si escribe datos, decide si necesita registrar auditoría — si sí, llama `writeAudit(actor, accion, detalle, matriculaAfectada)` después de que la operación tenga éxito.
3. Valida toda entrada del usuario **antes** de tocar la base de datos (tipos, rangos, existencia de FKs referenciadas).
4. Usa siempre parámetros (`.input(...)`) — nunca interpolar valores del usuario directamente en el texto SQL.
5. Prueba el endpoint con `curl` contra la base de datos real: caso válido, caso de error de validación, caso "no existe", caso sin `x-api-key` si aplica.
6. Agrega el endpoint a la tabla de [[Base_Datos_Usuarios]] §"Endpoints de `server.js`" y, si el cliente Android lo consume, al helper correspondiente en `ApiConfig.kt`.
7. Actualiza la colección de Postman (`Postman_Collection_AppTeschi.json`) si el equipo la usa para pruebas manuales.

## 3. Aplicar un cambio de esquema (migración nueva)

1. Crea un script numerado nuevo en `database/sql/` (siguiente número disponible), copiado a `04_BASE_DATOS/sql/`.
2. Hazlo **idempotente**: usa `IF COL_LENGTH(...) IS NULL`, `IF OBJECT_ID(...) IS NULL` o `IF NOT EXISTS (...)` antes de cualquier `CREATE`/`ALTER`.
3. Si agregas una columna y la vas a usar en el mismo script, sepárala en su propio `GO` — SQL Server no la reconoce hasta el siguiente batch.
4. Ejecuta el script contra una copia de prueba primero si el cambio es riesgoso (borra o transforma datos existentes).
5. Ejecuta contra la base real con `sqlcmd -f 65001`.
6. Actualiza [[Base_Datos_Usuarios]] (diagrama ER, tabla de scripts, orden de ejecución) y agrega una entrada en [[DECISIONES_TECNICAS]] si el cambio tiene una razón de negocio detrás.

## 4. Rotar el túnel de desarrollo

Cuando el túnel de Cloudflare deje de responder (`curl .../health` falla):

1. Verifica si el proceso `cloudflared` sigue vivo; si no, vuelve a lanzarlo: `cloudflared tunnel --url http://localhost:4000`.
2. Copia la URL nueva que imprime.
3. Verifica con `curl <url-nueva>/health`.
4. Actualiza `API_BASE_URL` en `local.properties`.
5. Recompila el APK (`./gradlew.bat :app:assembleDebug`) — la URL queda **compilada dentro del APK**, no se puede cambiar en tiempo de ejecución.
6. Reinstala/reenvía el APK nuevo.

## 5. Diagnosticar un problema común

| Síntoma | Causa probable | Solución |
|---|---|---|
| La app no carga ningún dato remoto | Túnel caído o `API_BASE_URL` desactualizada | Ver §4 |
| `401 API key inválida` | `API_KEY` distinta entre `.env` del backend y `local.properties` de la app | Igualar ambos valores y recompilar |
| Acentos corruptos en nombres de carrera/materia (`Ã­`, `Ã³`) | Script `.sql` ejecutado sin `-f 65001` | Reejecutar el script con la codificación correcta; ver bloque defensivo en `03_MigracionV2_Normalizacion.sql` §2.5 |
| `PRINT CONCAT(...)` falla con "subquery no permitida" | Subconsulta usada directo como argumento de `CONCAT` en T-SQL | Precomputar en una variable `DECLARE @x = (SELECT ...)` antes del `PRINT` |
| Error de `QUOTED_IDENTIFIER` al crear índices filtrados o columnas calculadas | Falta `SET QUOTED_IDENTIFIER ON` al inicio del script | Agregar `SET ANSI_NULLS ON; SET QUOTED_IDENTIFIER ON;` cerca del encabezado |
| `DELETE FROM Alumnos` falla por restricción de clave foránea | Existen filas en `HistorialAcademico`/`OtpHistorial`/`SesionesLogin`/`AuditoriaMovimientos` que referencian al alumno | Usar el endpoint `DELETE /api/usuarios/:matricula` (ya hace la limpieza en cascada dentro de una transacción), no borrar `Alumnos` manualmente |
| El semestre de un alumno no se puede editar aunque el valor "parezca" mayor | Se está comparando contra el semestre ya guardado en `Alumnos`, no contra el mostrado en una pantalla desactualizada | Refrescar el perfil antes de reintentar |

## 6. Antes de cada entrega (checklist rápido)

Ver [[03_Manual_Tecnico_Instalacion]] §6 — compilar, probar, generar APK y documentar son pasos obligatorios, no opcionales, en cada iteración de este proyecto.
