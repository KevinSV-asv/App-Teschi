# Manual de Mantenimiento — AppTESCHI

> ⚠️ **Desactualizado (12/10/2026, DEC-043):** describe el sistema con backend propio, base de datos y panel de administrador. Hoy la app solo tiene las pantallas del alumno con datos del SIIA (ver [[REGLAS_PROYECTO]] y [[MAPA_PROYECTO]]). Actualizar antes de entregarlo.

> Procedimientos recurrentes para evolucionar el sistema sin romper lo que ya funciona. Actualizado 27/09/2026.
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

1. Define el contrato: método HTTP, ruta, qué protección lleva (`requireAlumno()`, `requireAdmin()` o pública con límite de intentos), payload de entrada, forma de la respuesta.
2. Si escribe datos, decide si necesita registrar auditoría — si sí, llama `writeAudit(actor, accion, detalle, matriculaAfectada)` después de que la operación tenga éxito.
3. Valida toda entrada del usuario **antes** de tocar la base de datos (tipos, rangos, existencia de FKs referenciadas).
4. Usa siempre parámetros (`.input(...)`) — nunca interpolar valores del usuario directamente en el texto SQL.
5. Prueba el endpoint con `curl` contra la base de datos real: caso válido, caso de error de validación, caso "no existe", caso sin token (401) y, para endpoints del alumno, con el token de **otro** alumno (403).
6. Agrega el endpoint a la tabla de [[Base_Datos_Usuarios]] §"Endpoints de `server.js`" y, si el cliente Android lo consume, crea o amplía su `*Service` sobre `ClienteHttp`.
7. Actualiza la colección de Postman (`Postman_Collection_AppTeschi.json`) si el equipo la usa para pruebas manuales.

## 3. Aplicar un cambio de esquema (migración nueva)

1. Crea un script numerado nuevo en `04_BASE_DATOS/sql/` (siguiente número libre; hoy el siguiente es `26_`). Si todavía no se va a aplicar, déjalo sin número en `sql/pendientes/`.
2. Empieza con `SET ANSI_NULLS ON; SET QUOTED_IDENTIFIER ON; GO` y hazlo **idempotente**: usa `IF COL_LENGTH(...) IS NULL`, `IF OBJECT_ID(...) IS NULL` o `IF NOT EXISTS (...)` antes de cualquier `CREATE`/`ALTER`.
3. Si agregas una columna y la vas a usar en el mismo script, sepárala en su propio `GO` — SQL Server no la reconoce hasta el siguiente batch.
4. Ejecuta el script contra una copia de prueba primero si el cambio es riesgoso (borra o transforma datos existentes).
5. Ejecuta contra la base real del servidor como se explica en §7.
6. Actualiza [[Base_Datos_Usuarios]] (diagrama ER, tabla de scripts, orden de ejecución) y agrega una entrada en [[DECISIONES_TECNICAS]] si el cambio tiene una razón de negocio detrás.

## 4. Actualizar el backend del servidor

1. Haz commit y `git push` de los cambios.
2. Despliega con `deploy-desde-windows.ps1` ([[03_Manual_Tecnico_Instalacion]] §8.2).
3. Comprueba en el servidor: `sudo systemctl status appteschi-api --no-pager` y `sudo journalctl -u appteschi-api -n 50`.
4. Prueba `/health` y el endpoint que cambiaste (Postman con el entorno "Servidor").

> El túnel rápido de Cloudflare que se rotaba aquí ya no se usa (histórico, ver [[03_Manual_Tecnico_Instalacion]] §4).

## 5. Diagnosticar un problema común

| Síntoma | Causa probable | Solución |
|---|---|---|
| La app no carga ningún dato remoto | Tailscale apagado en el teléfono, o `API_BASE_URL` / "⚙ Servidor" apuntando a otro lado | Encender Tailscale; revisar la dirección que muestra el login (DEC-031, DEC-033) |
| La app regresa al login con "sesión vencida" | Token de sesión vencido (12 h) o firmado con otro `JWT_SECRET` | Volver a iniciar sesión |
| El alumno entra por el SIIA pero ve "aún no tienes registro en AppTESCHI" en Kardex, Calificaciones u horario | No existe en `Alumnos`: entró sin registro (DEC-040) | Darlo de alta desde el panel con la misma matrícula del SIIA; en su siguiente inicio de sesión ya ve sus datos |
| Un alumno sin registro pide el código y luego dice "No hay un código pendiente" | Se reinició la API: sus códigos viven solo en memoria (DEC-040) | Que vuelva a iniciar sesión |
| Login SIIA: aviso de "cuenta pendiente" | Está registrado sin carrera y su carrera no está mapeada a un `idCarrera` del SIIA | Asignarle la carrera en el panel o mapearla (§7.2) |
| Login SIIA: 429 | Más de 5 intentos en 60 s en el SIIA, u 8 en 5 min en AppTESCHI | Esperar |
| La API responde con código viejo | Proceso `node` manual ocupando el puerto 4000 (BUG-019) | [[03_Manual_Tecnico_Instalacion]] §8.3 |
| Acentos corruptos en nombres de carrera/materia (`Ã­`, `Ã³`) | Script `.sql` ejecutado sin `-f 65001` | Reejecutar el script con la codificación correcta; ver bloque defensivo en `03_MigracionV2_Normalizacion.sql` §2.5 |
| `PRINT CONCAT(...)` falla con "subquery no permitida" | Subconsulta usada directo como argumento de `CONCAT` en T-SQL | Precomputar en una variable `DECLARE @x = (SELECT ...)` antes del `PRINT` |
| Error de `QUOTED_IDENTIFIER` al crear índices filtrados o columnas calculadas | Falta `SET QUOTED_IDENTIFIER ON` al inicio del script | Agregar `SET ANSI_NULLS ON; SET QUOTED_IDENTIFIER ON;` cerca del encabezado |
| `DELETE FROM Alumnos` falla por restricción de clave foránea | Existen filas en `HistorialAcademico`/`OtpHistorial`/`SesionesLogin`/`AuditoriaMovimientos` que referencian al alumno | Usar el endpoint `DELETE /api/usuarios/:matricula` (ya hace la limpieza en cascada dentro de una transacción), no borrar `Alumnos` manualmente |
| El semestre de un alumno no se puede editar aunque el valor "parezca" mayor | Se está comparando contra el semestre ya guardado en `Alumnos`, no contra el mostrado en una pantalla desactualizada | Refrescar el perfil antes de reintentar |

## 6. Antes de cada entrega (checklist rápido)

Ver [[03_Manual_Tecnico_Instalacion]] §7 — compilar, probar, generar APK y documentar son pasos obligatorios, no opcionales, en cada iteración de este proyecto.

## 7. Base de datos del servidor

### 7.1 Correr SQL con `sqlcmd`

Se corre **en el servidor** (por SSH), no en la PC: la base está en el contenedor `appteschi-sql`. Usuario, contraseña y base están en el `.env` del servidor:

```bash
sudo grep -E "^DB_" /opt/appteschi/api/.env
```

Para correr un script, súbelo primero (`scp archivo.sql kevin_sanvar@100.111.170.32:/tmp/`) y luego:

```bash
sqlcmd -S 127.0.0.1 -U appteschi_api -P '<DB_PASSWORD>' -d AppTeschiDB -C -f 65001 -i /tmp/archivo.sql
```

Para una consulta suelta:

```bash
sqlcmd -S 127.0.0.1 -U appteschi_api -P '<DB_PASSWORD>' -d AppTeschiDB -C -Q "SET QUOTED_IDENTIFIER ON; SELECT Clave, IdCarreraSiia FROM dbo.CatalogoCarreras;"
```

- `-C` confía en el certificado autofirmado del contenedor (sin él: `certificate verify failed`).
- `SET QUOTED_IDENTIFIER ON` es obligatorio para cualquier `INSERT`/`UPDATE`/`CREATE INDEX` sobre `Alumnos` o `CatalogoCarreras` (tienen índices filtrados). Los scripts ya lo traen al inicio.
- `-f 65001` para que los acentos no se corrompan.
- Un `UPDATE …` escrito directo en la terminal da `UPDATE: command not found`: el SQL siempre va dentro de `sqlcmd`.

### 7.2 Mapear una carrera del SIIA

1. Cuando un alumno registrado sin carrera, de una carrera sin mapear, entra por el SIIA, la app le muestra "cuenta pendiente" y el servidor deja en el log:
   ```bash
   sudo journalctl -u appteschi-api | grep "AUTH-SIIA\]\[PENDIENTE"
   ```
   Ahí viene `idCarreraSiia=N` y `area="…"` (el nombre de la carrera). (También se ve con el login directo al SIIA en Postman.)
2. Agrega una línea a `04_BASE_DATOS/sql/25_MapeoCarrerasSiia.sql`:
   ```sql
   UPDATE dbo.CatalogoCarreras SET IdCarreraSiia = N WHERE Clave = N'CLAVE' AND (IdCarreraSiia IS NULL OR IdCarreraSiia <> N);
   ```
3. Córrelo en el servidor (§7.1) y actualiza la tabla de [[API_SIIA_Oficial]].
4. La próxima vez que ese alumno entre, su carrera se enlaza sola (DEC-037).
