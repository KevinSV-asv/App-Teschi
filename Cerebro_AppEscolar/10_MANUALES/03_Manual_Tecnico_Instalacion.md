# Manual Técnico de Instalación y Configuración — AppTESCHI

> Guía para levantar el entorno completo desde cero: base de datos, backend, app Android y túnel de desarrollo.
> **Relacionado con:** [[05_Diagrama_Despliegue]], [[Base_Datos_Usuarios]], [[04_Manual_Mantenimiento]]

---

## 1. Requisitos previos

| Herramienta | Versión mínima | Uso |
|---|---|---|
| Android Studio | Ladybug o superior | Compilar/ejecutar la app |
| JDK | 17 (el que trae Android Studio) | Compilación Gradle |
| SQL Server Express | 2019+ | Base de datos `AppTeschiDB` |
| SQL Server Management Studio (SSMS) | 19+ | Administrar la base de datos, ver el diagrama ER |
| Node.js | 18+ | Ejecutar `AppTeschi.Api` |
| `sqlcmd` | Incluido con SQL Server / herramientas de línea de comandos | Ejecutar los scripts `.sql` |
| `cloudflared` | Cualquiera reciente | Exponer el backend local a internet durante desarrollo |

---

## 2. Base de datos

### 2.1 Crear la base y aplicar las migraciones

Los scripts viven en `database/sql/` (repo) y están espejados en `04_BASE_DATOS/sql/` (bóveda). **Deben ejecutarse en este orden exacto** — cada uno es idempotente (se puede volver a correr sin duplicar datos):

```
01_CrearBaseDatos.sql
02_ProcedimientosUsuarios.sql
03_MigracionV2_Normalizacion.sql
04_SeedMateriasAnimacion.sql
05_SeedGruposAnimacion.sql
06_SeedGruposIndustrial.sql
07_SeedMateriasIndustrial.sql
08_SeedMateriasMecatronica.sql
09_SeedGruposMecatronica.sql
10_SeedMateriasQuimica.sql
11_SeedGruposQuimica.sql
12_SeedMateriasAdministracion.sql
13_SeedGruposAdministracion.sql
14_SeedMateriasGastronomia.sql
15_SeedGruposGastronomia.sql
16_AgregarSemestreAlumnos.sql
```

Ejecución recomendada (Windows Authentication, usuario con privilegios de `sysadmin`; `-f 65001` es obligatorio para que los acentos no se corrompan):

```bash
sqlcmd -S localhost -d AppTeschiDB -E -f 65001 -i "database/sql/01_CrearBaseDatos.sql"
# repetir para cada script, en orden
```

> ⚠️ No ejecutes estos scripts con una herramienta que no respete UTF-8 — ya se corrigió una corrupción de acentos causada por eso (ver DEC-014). Si vuelves a ver nombres como `IngenierÃ­a`, revisa la codificación con la que se ejecutó el script.

### 2.2 Crear el login de la aplicación

El backend se conecta con un login de SQL Server **sin privilegios de DDL** (solo `CONNECT` + DML). Créalo una vez, con permisos mínimos:

```sql
CREATE LOGIN appteschi_api WITH PASSWORD = '<contraseña segura>';
USE AppTeschiDB;
CREATE USER appteschi_api FOR LOGIN appteschi_api;
ALTER ROLE db_datareader ADD MEMBER appteschi_api;
ALTER ROLE db_datawriter ADD MEMBER appteschi_api;
GRANT EXECUTE TO appteschi_api;
```

### 2.3 Verificar visualmente el diagrama

En SSMS: clic derecho en `AppTeschiDB` → `Database Diagrams` → `New Database Diagram` → agrega todas las tablas. Ver [[Base_Datos_Usuarios]] para el diagrama entidad-relación de referencia (mermaid) y la explicación de cardinalidad de cada relación.

---

## 3. Backend (`AppTeschi.Api`)

> Vive dentro de la bóveda, no en el repo de Android: `07_BACKEND/AppTeschi.Api/`
> (ruta completa en esta máquina: `C:\Users\kevin\Downloads\Cerebro_AppEscolar\Cerebro_AppEscolar\07_BACKEND\AppTeschi.Api`).

### 3.1 Instalar dependencias

```bash
cd "07_BACKEND/AppTeschi.Api"
npm install
```

### 3.2 Configurar variables de entorno

Copia `.env.example` a `.env` y completa:

```ini
PORT=4000
DB_SERVER=localhost
DB_DATABASE=AppTeschiDB
DB_USERNAME=appteschi_api
DB_PASSWORD=<la contraseña que creaste en 2.2>
DB_ENCRYPT=false
JWT_SECRET=<mínimo 32 caracteres; sin él la API no arranca — ver .env.example>
EMAIL_SENDER=...
EMAIL_APP_PASSWORD=...
EMAIL_SMTP_HOST=smtp.gmail.com
EMAIL_SMTP_PORT=587
```

### 3.3 Ejecutar

```bash
node server.js
```

Verifica que responda:

```bash
curl http://localhost:4000/health
# {"ok":true,"database":"AppTeschiDB"}
```

### 3.4 Referencia de endpoints

Ver la tabla completa en [[Base_Datos_Usuarios]] §"Endpoints de `server.js`" — incluye todos los endpoints de autenticación, CRUD de alumnos, calificaciones, auditoría, estadísticas y catálogo curricular. Para probarlos sin la app, usa la colección de Postman en `07_BACKEND/AppTeschi.Api/Postman_Collection_AppTeschi.json`.

---

## 4. Túnel de desarrollo (exponer el backend a internet)

Mientras no exista un dominio institucional fijo, el backend local se expone con un túnel rápido de Cloudflare. Desde el 14/09/2026 `cloudflared` está instalado de forma permanente (`winget install Cloudflare.cloudflared`) — ya no depende de `npx` ni de internet para descargarlo cada vez.

```bash
cloudflared tunnel --url http://localhost:4000
```

Esto imprime una URL del tipo `https://<palabras-aleatorias>.trycloudflare.com`. Esa URL:

1. Se prueba con `curl https://<url>/health` antes de usarla.
2. Se copia a `local.properties` → `API_BASE_URL=https://<url>`.
3. **Es temporal** — si el proceso `cloudflared` se cierra o reinicia, cambia. Hay que repetir los pasos 1-2 y **recompilar el APK** cada vez.

> El script `iniciar-appteschi.ps1` (raíz del repo Android) automatiza el backend + el túnel + la actualización de `local.properties` en un solo paso — ver §7.

### 4.1 Problema conocido: DNS local no resuelve túneles nuevos

Detectado el 14/09/2026 en esta máquina: el DNS principal de la red Wi-Fi (`dns-chimalhuacan`, `10.10.16.23`) responde "dominio no existe" para subdominios de `trycloudflare.com` recién creados, aunque el túnel sí está activo y otros resolutores (`1.1.1.1`, el DNS secundario del ISP) sí lo resuelven. Como el resolutor primario da una respuesta negativa (no un timeout), Windows no intenta el secundario, y `curl`/la app fallan aunque el túnel funcione.

**Fix permanente (una sola vez, como Administrador):**

```powershell
Set-DnsClientServerAddress -InterfaceAlias "Wi-Fi" -ServerAddresses ("1.1.1.1","8.8.8.8")
```

**Mientras tanto / alternativa sin depender de DNS:** usar la IP local del backend en la misma Wi-Fi (ver §5.1 y `local.properties` — es la opción `API_BASE_URL=http://192.168.0.41:4000`, ya verificada y **activa por defecto** desde el 14/09/2026).

---

## 5. App Android

### 5.1 Configurar `local.properties`

No se versiona (está en `.gitignore`). Cada desarrollador define el suyo:

```ini
sdk.dir=<ruta al Android SDK>

API_BASE_URL=https://<url-del-tunel-o-servidor>
```

Ya no hay `API_KEY` ni credenciales SMTP aquí: la app se identifica con su token de sesión (DEC-030) y el correo lo envía el backend (DEC-019).

Estas claves llegan a Kotlin vía `BuildConfig` (ver `ApiConfig.kt`) — **nunca hardcodear la URL en el código**.

### 5.2 Compilar y probar

```bash
./gradlew.bat :app:compileDebugKotlin   # compilación rápida, sin generar APK
./gradlew.bat :app:testDebugUnitTest    # suite de pruebas unitarias
./gradlew.bat :app:assembleDebug        # genera app/build/outputs/apk/debug/app-debug.apk
```

El APK resultante se instala manualmente en un dispositivo o emulador (`adb install -r app-debug.apk`), o se distribuye directamente para pruebas.

---

## 6. Arranque en un solo paso (`iniciar-appteschi.ps1`)

Script en la raíz del repo Android (`iniciar-appteschi.ps1`) que automatiza los pasos 3 y 4 (backend + túnel) y deja `local.properties` con la URL del túnel ya actualizada:

```powershell
powershell -ExecutionPolicy Bypass -File .\iniciar-appteschi.ps1
```

Qué hace, en orden: verifica/arranca el servicio de SQL Server → arranca el backend en una ventana aparte (o detecta que ya está corriendo) → arranca `cloudflared` → espera la URL pública y la valida con `/health` → actualiza `API_BASE_URL` en `local.properties`. Si el túnel no resuelve por el problema de DNS de §4.1, el script lo avisa explícitamente y no toca `local.properties` (para no dejarlo apuntando a una URL que no sirve) — en ese caso usa la IP local o corrige el DNS primero.

Después de correrlo, si vas a instalar un APK nuevo, sigue haciendo falta recompilar (§5.2) porque la URL queda fija dentro del APK — el script no reemplaza ese paso.

---

## 7. Checklist de una entrega completa

1. Backend actualizado y reiniciado; endpoints nuevos probados con `curl` o Postman contra la base de datos real.
2. Backend accesible: en el servidor (§8) `/health` responde por LAN/Tailscale; en desarrollo, túnel de Cloudflare o IP local (§4.1).
3. `local.properties` actualizado con la URL vigente.
4. `./gradlew.bat :app:compileDebugKotlin` sin errores.
5. `./gradlew.bat :app:testDebugUnitTest` en 100 % verde.
6. `./gradlew.bat :app:assembleDebug` genera el APK.
7. Cambios documentados en [[CAMBIOS_DE_CODIGO]] y, si hubo una decisión de arquitectura, en [[DECISIONES_TECNICAS]].

---

## 8. Servidor Ubuntu (despliegue actual, desde 19/09/2026)

El backend y la base de datos corren en un servidor Ubuntu Server 26.04.1 LTS en casa, detrás de un router sin puertos redirigidos: solo se llega por la **LAN** (`192.168.0.252`) o por **Tailscale** (`100.111.170.32`). Comparte máquina con n8n y Nextcloud. La laptop queda como entorno de desarrollo con su propia base local. Decisiones y razones: DEC-031.

| Pieza | Cómo corre |
|---|---|
| API (`AppTeschi.Api`) | Servicio `systemd` `appteschi-api`, usuario `appteschi`, código en `/opt/appteschi/api`, Node 22.16.0 en `/opt/node` (binario oficial verificado con SHA256) |
| SQL Server 2022 Express | Contenedor Docker `appteschi-sql`, publicado **solo en `127.0.0.1:1433`**, datos en un volumen nombrado, límite de 2 GB de RAM |
| Login de la API | `appteschi_api` con `db_owner` solo en `AppTeschiDB` (la API ya no usa `sa`) |
| Respaldos | `cron` diario a las 03:15 → `/opt/appteschi/backups`, verificados (`RESTORE VERIFYONLY`), comprimidos, 14 días de retención |

**Por qué SQL Server en Docker y no nativo:** la documentación de Microsoft solo llega hasta Ubuntu 24.04 (SQL Server 2025) y no menciona 26.04; el contenedor no depende de la versión del host.

### 8.1 El kit (`07_BACKEND/AppTeschi.Api/deploy/`)

| Archivo | Para qué |
|---|---|
| `00-diagnostico.sh` | Solo lectura: arquitectura, Docker, puertos, utilidades, servicios que ya corren |
| `docker-compose.sql.yml` | SQL Server 2022 Express |
| `01-instalar.sh` | Usuario, carpetas, Node, contraseña de `sa` (se genera sola), contenedor, servicio `systemd`, cron de respaldos, `npm ci`. No toca el firewall ni otros servicios |
| `02-restaurar-bd.sh` | Restaura un `.bak`, crea el login de la API, ajusta `DB_*` del `.env` y arranca la API |
| `aplicar.sh` | Permisos, `npm ci`, quita los `\r` del `.env` y reinicia la API |
| `backup-sql.sh` | Respaldo diario |
| `appteschi-api.service` | Unidad `systemd` (endurecida: `ProtectSystem=strict`, `NoNewPrivileges`, `MemoryMax=512M`) |
| `deploy-desde-windows.ps1` | Sube el código por `scp` y llama a `aplicar.sh`; no sube `node_modules`, `.env` ni `.bak` |

### 8.2 Instalación desde cero (en este orden)

1. **PowerShell local:** `.\deploy\deploy-desde-windows.ps1 -Servidor usuario@ip-tailscale` (la primera vez solo sube el código).
2. **Servidor:** `bash /opt/appteschi/api/deploy/00-diagnostico.sh` — debe ser `x86_64` (SQL Server no corre en ARM).
3. **Servidor:** `sudo bash /opt/appteschi/api/deploy/01-instalar.sh`.
4. **Copiar el `.env`** del backend: `scp .env usuario@servidor:/tmp/api.env` y luego `sudo install -o appteschi -g appteschi -m 600 /tmp/api.env /opt/appteschi/api/.env && rm /tmp/api.env`. Debe tener `JWT_SECRET` de al menos 32 caracteres; la API no arranca sin él.
5. **Respaldo de la base:** en la laptop, `BACKUP DATABASE AppTeschiDB TO DISK = ... WITH INIT, CHECKSUM` y `RESTORE VERIFYONLY`; `scp` al servidor; comprobar `sha256sum` en ambos lados; `sudo bash /opt/appteschi/api/deploy/02-restaurar-bd.sh /tmp/AppTeschiDB.bak`. Restaurar de SQL Server 2019 a 2022 convierte la base automáticamente. **Borrar los `.bak` (laptop y servidor) solo después de comprobar los conteos** — traen datos reales de alumnos.
6. **Probar:** `curl http://IP:4000/health` por LAN y por Tailscale; sin sesión, `GET /api/mi-perfil/<matrícula>` debe dar 401.
7. **App:** la dirección de fábrica (`API_BASE_URL` en `local.properties`) es la IP de Tailscale del servidor, `http://100.111.170.32:4000`: funciona en casa y fuera de ella si el teléfono tiene Tailscale encendido. Sin Tailscale y en casa, "⚙ Servidor" en el login permite fijar `http://192.168.0.252:4000` sin recompilar. **Comprobar a qué servidor apunta de verdad:** el texto "⚙ Servidor: …" del login muestra la dirección activa; y apagar el backend de la laptop, que es el error que ya pasó (DEC-031).

**Actualizar el backend después:** `.\deploy\deploy-desde-windows.ps1 -Servidor usuario@ip` (pide la contraseña de `sudo`).

### 8.3 Problemas que aparecieron y su solución

| Síntoma | Causa | Solución |
|---|---|---|
| `install: usuario inválido: '10001'` | `install -o` rechazó un UID numérico que no existe como usuario del host (UID de `mssql` dentro del contenedor); probablemente por los coreutils de Ubuntu 26.04 | `cp` + `chown 10001:0` + `chmod` (ya corregido en `02-restaurar-bd.sh`) |
| API en bucle de reinicios: `Cannot find module 'express'` | Nunca se corrió `npm ci` en el servidor: al subir el código por primera vez aún no existía el usuario `appteschi` | `01-instalar.sh` ahora llama a `aplicar.sh`; ejecutarlo o volver a desplegar |
| `curl: (3) URL rejected: Malformed input` al final de la restauración | El `.env` viene de Windows con saltos CRLF: `PORT=4000\r` | `aplicar.sh` y `02-restaurar-bd.sh` quitan los `\r`; la comprobación final ahora reintenta 20 s |

Ninguno de los tres afectaba a los datos: la restauración terminó bien antes de que fallara la comprobación.

### 8.4 Firewall

`ufw` está **inactivo** y se dejó así a propósito: activarlo en un servidor con SSH, n8n y Nextcloud puede dejarte sin acceso si falta una regla, y `ufw` no controla los puertos que publica Docker. El puerto 4000 solo es alcanzable desde la LAN y Tailscale porque el router no redirige puertos. Al abrirlo a internet con un túnel de Cloudflare la API escuchará solo en `127.0.0.1` (`HOST`, `TRUST_CLOUDFLARE`; ver `.env.example`).

### 8.5 Pendiente

- Copia de los respaldos **fuera** del servidor (decisión: otro equipo por Tailscale). Hoy viven en el mismo disco que la base.
- Probar una restauración real de un respaldo diario (`02-restaurar-bd.sh` sirve para eso).
- Fase pública: dominio + Cloudflare Tunnel con nombre, y antes cerrar `POST /api/registro` (ver DEC-030).
