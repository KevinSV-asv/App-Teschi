# Avance del 01 de Septiembre de 2026

## Resumen ejecutivo

Se resolvió la causa raíz del bloqueo de la API con SQL Server: el proyecto apuntaba a una instancia llamada `SQLEXPRESS`, pero el equipo tenía una instancia local activa de SQL Server con el nombre por defecto `VICTUS` / `MSSQLSERVER`. La API quedó configurada para apuntar a la instancia real y se validó la conexión con un usuario dedicado para la aplicación.

## Diagnóstico realizado

- Se verificó que el servicio activo era `MSSQLSERVER`.
- Se comprobó que `SQLBrowser` estaba detenido, lo que impedía resolver instancias con nombre como `localhost\SQLEXPRESS` de forma consistente.
- Se validó la conexión directa con `sqlcmd -S localhost -E` y la base `AppTeschiDB` estaba disponible.
- Se creó un usuario SQL dedicado llamado `appteschi_api` con permisos de lectura y escritura en la base de datos.

## Correcciones aplicadas

### Configuración de la API

Se dejó la configuración de entorno como:

```env
PORT=4000
DB_SERVER=localhost
DB_DATABASE=AppTeschiDB
DB_USERNAME=appteschi_api
DB_PASSWORD=<DB_PASSWORD>
DB_ENCRYPT=false
API_KEY=<API_KEY>
```

Esto elimina la dependencia de una instancia con nombre inexistente o inaccesible y usa la instancia real del equipo.

### Verificación funcional

Se ejecutó la API en local y se validó lo siguiente:

1. `GET http://localhost:4000/health` respondió correctamente con:

```json
{"ok":true,"database":"AppTeschiDB"}
```

2. `POST http://localhost:4000/api/usuarios/sync` aceptó un payload real y devolvió:

```json
{"ok":true,"inserted":1}
```

3. La consulta directa en SQL Server mostró que el registro quedó persistido en `dbo.Usuarios`.

## Registros en base de datos

Se insertó un registro de prueba válido para confirmar el ciclo completo:

- Matricula: `20240099`
- Nombre: `Usuario Prueba API`
- Carrera: `ALUMNO`

La prueba quedó registrada en la tabla `dbo.Usuarios`.

## Estado actual

La API ya está funcionando correctamente en local y la capa de persistencia hacia SQL Server quedó validada.

## Siguiente paso recomendado

Continuar con la integración real de la app Android hacia la API, validando el flujo:

- Registro del usuario en la app
- Guardado local si hay conexión pobre o nula
- Envío a `POST /api/usuarios/sync`
- Persistencia final en `AppTeschiDB`

## Relación con la bóveda

Este avance corresponde con la documentación técnica del backend, la base de datos y la integración del proyecto AppTeschi con SQL Server.
