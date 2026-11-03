# 23 de Septiembre de 2026

> ⚠️ **Nota del 27/09/2026:** parte de lo de este día se cambió después. El alta automática de alumnos se retiró (DEC-036), el `siia-api.js` no se usa (el login real vive en `server.js`, DEC-035), el backend ya no corre con `nohup` sino con el servicio de systemd (BUG-019) y el script de migración de docentes/administradores quedó en `04_BASE_DATOS/sql/pendientes/`. Ver [[27 de Septiembre]].

## Sesión de trabajo

- **Objetivo del día:** Completar Fase 1 del módulo 9 (Login unificado SIIA por tipo) y avanzar fases restantes.
- **Módulo:** [[09_Login_SIIA_por_Tipo]]
- **Referencia:** DEC-035 (login con SIIA oficial, asistencia fuera de alcance)

---

## ✅ Fase 1 completada — Conexión base (backend)

### Archivos nuevos
- **`07_BACKEND/AppTeschi.Api/siia-api.js`** — módulo cliente del SIIA.
  - Exporta `loginSiia(usuario, password)`.
  - Usa `fetch` nativo (Node 18+), sin dependencias nuevas — igual que `correo-graph.js`.
  - Timeout de 10 s con `AbortController`.
  - Maneja: credenciales inválidas (`CREDENCIALES_INVALIDAS`), SIIA no disponible/lento (`SIIA_NO_DISPONIBLE`), rate-limit del SIIA con `Retry-After` (`SIIA_RATE_LIMIT`) y respuesta inesperada (`SIIA_INESPERADO`).
  - Nunca guarda el token del SIIA ni expone datos sensibles en los mensajes de error.

### Archivos modificados
- **`server.js`**:
  - `require('./siia-api')` añadido al inicio.
  - Nueva constante `AUD_DOCENTE = 'appteschi:docente'`.
  - Nueva constante `JWT_DOCENTE_EXPIRA_EN` (env `JWT_DOCENTE_EXPIRA_EN`, default `12h`).
  - Nueva función `firmarTokenDocente(docente)`.
  - Nuevo middleware `requireDocente()` (igual en estructura que `requireAlumno()`).
  - Nuevo endpoint `POST /api/auth/siia`:
    - Límite propio: 5 intentos / 60 s por usuario+IP.
    - Llama a `loginSiia()` y clasifica por `tipo` del SIIA.
    - **Alumno:** busca en `Alumnos` por matrícula → token AUD_ALUMNO, o `cuentaPendiente: true`.
    - **Docente:** busca en `Docentes` (try/catch — tabla no existe hasta Fase 3) → token AUD_DOCENTE, o `cuentaPendiente: true`.
    - **Administrativo:** busca `Administradores.UsuarioSiia` (try/catch — columna no existe hasta Fase 5) → token AUD_ADMIN, o 403.
    - Reenvía el 429 del SIIA con `retryAfter`.
- **`.env.example`**: añadida sección SIIA con `SIIA_API_URL=https://siia.teschi.edu.mx/Teschi/api`.
- **`Postman_Collection_AppTeschi.json`**: nueva carpeta "SIIA — Autenticación unificada" con el request de `POST /api/auth/siia`.

### Próximo paso antes de Fase 2
Agregar `SIIA_API_URL` al `.env` real del servidor Ubuntu y reiniciar el backend con `deploy-desde-windows.ps1`. Luego probar con Postman las 3 cuentas de prueba.

---

## ⏳ Siguiente: Fase 2 — Alumnos entran con el SIIA

Ver [[09_Login_SIIA_por_Tipo#Fase 2 — Alumnos entran con el SIIA]].

Scripts pendientes:
- `04_BASE_DATOS/sql/24_VinculoSiia.sql`
- Actualizar backend y app Android.

---

## ✅ Pruebas de Fase 1 — 23/09/2026

Se ejecutaron **38 verificaciones estáticas y unitarias** desde la carpeta `AppTeschi.Api`. Todas pasaron.

### Grupo 1: `siia-api.js` — módulo cliente del SIIA (4/4 ✅)
| # | Prueba | Resultado |
|---|---|---|
| 1 | `SIIA_API_URL` apunta al servidor correcto | ✅ |
| 2 | `loginSiia` es una función exportada | ✅ |
| 3 | Error `SIIA_NO_DISPONIBLE` cuando el host no existe | ✅ |
| 4 | Error `SIIA_NO_DISPONIBLE` cuando `SIIA_API_URL` está vacía | ✅ |

### Grupo 2: JWT y constantes (4/4 ✅)
| # | Prueba | Resultado |
|---|---|---|
| 5 | `JWT_SECRET` configurado | ✅ |
| 6 | Token `AUD_DOCENTE` se firma y verifica correctamente | ✅ |
| 7 | Token de docente rechazado al verificar como alumno | ✅ |
| 8 | Token `AUD_ALUMNO` se firma y verifica correctamente | ✅ |

### Grupo 3: Endpoint `POST /api/auth/siia` en `server.js` (15/15 ✅)
| # | Prueba | Resultado |
|---|---|---|
| 9 | `siia-api.js` importado | ✅ |
| 10 | Constante `AUD_DOCENTE` definida | ✅ |
| 11 | Función `firmarTokenDocente` definida | ✅ |
| 12 | Middleware `requireDocente` definido | ✅ |
| 13 | Endpoint `POST /api/auth/siia` registrado | ✅ |
| 14 | `loginSiia()` es invocado en el endpoint | ✅ |
| 15 | Manejo de `CREDENCIALES_INVALIDAS` | ✅ |
| 16 | Manejo de `SIIA_RATE_LIMIT` (429 → retryAfter) | ✅ |
| 17 | Manejo de `SIIA_NO_DISPONIBLE` | ✅ |
| 18 | Campo `retryAfter` propagado al cliente | ✅ |
| 19 | Clasificación por tipo `alumno` | ✅ |
| 20 | Clasificación por tipo `docente` | ✅ |
| 21 | Clasificación por tipo `administrativo` | ✅ |
| 22 | Campo `cuentaPendiente` en respuesta | ✅ |
| 23 | Campo `cambioPw` / `requiereCambioPw` propagado | ✅ |

### Grupo 4: SQL scripts de migración (11/11 ✅)
| # | Prueba | Resultado |
|---|---|---|
| 24 | `24_VinculoSiia.sql`: columna `OrigenCuenta` en `Alumnos` | ✅ |
| 25 | `24_VinculoSiia.sql`: columna `UsuarioSiia` en `Alumnos` | ✅ |
| 26 | `24_VinculoSiia.sql`: columna `IdCarreraSiia` en `CatalogoCarreras` | ✅ |
| 27 | `24_VinculoSiia.sql`: idempotente (IF NOT EXISTS) | ✅ |
| 28 | `25_Docentes.sql`: tabla `Docentes` creada | ✅ |
| 29 | `25_Docentes.sql`: columna `UsuarioSiia` | ✅ |
| 30 | `25_Docentes.sql`: columna `NombreEnHorario` | ✅ |
| 31 | `25_Docentes.sql`: FK `OtpHistorial → Docentes` | ✅ |
| 32 | `25_Docentes.sql`: idempotente | ✅ |
| 33 | `26_AdministradoresSiia.sql`: columna `UsuarioSiia` | ✅ |
| 34 | `26_AdministradoresSiia.sql`: idempotente | ✅ |

### Grupo 5: Configuración `.env` (8/8 ✅)
| # | Prueba | Resultado |
|---|---|---|
| 35 | `SIIA_API_URL` presente en `.env` | ✅ |
| 36 | `SIIA_API_URL` apunta al servidor correcto | ✅ |
| 37 | `JWT_SECRET` presente | ✅ |
| 38 | `PORT`, `DB_SERVER`, `EMAIL_SENDER` presentes | ✅ |

### Nota sobre pruebas de integración en vivo
Las pruebas con cuentas reales del SIIA (grupos `alumno`, `docente`, `administrativo`) no se pudieron correr desde el entorno de Claude porque:
1. El SIIA solo es accesible desde la red de TESCHI / servidor local (la política de egreso bloquea la conexión directa).
2. El backend corre en el servidor Ubuntu vía Tailscale (IP privada no accesible desde la nube).

**Para correrlas:** desde el servidor Ubuntu, con el backend activo y las cuentas de prueba configuradas en `test-siia.js`:
```bash
cd AppTeschi.Api
node test-siia.js
```

### ✅ Conclusión Fase 1
La implementación está completa y verificada estáticamente. El backend está listo para recibir llamadas al endpoint `POST /api/auth/siia`. El siguiente paso es la **Fase 2: Alumnos entran con el SIIA**.

---

## Fase 2: Alumnos entran con el SIIA

### Cambios aplicados a `server.js` (commit `1900ebc`)

| # | Cambio | Estado |
|---|---|---|
| 1 | `firmarTicketLogin` acepta `tipo='DOCENTE'` (comentario JSDoc) | ✅ |
| 2 | `POST /api/auth/siia` alumno: auto-crear si `IdCarreraSiia` mapeado | ✅ |
| 3 | `POST /api/auth/siia` alumno: vincular `UsuarioSiia` si ya existe | ✅ |
| 4 | `POST /api/auth/siia` alumno: emite `ticket` (no token directo) | ✅ |
| 5 | `POST /api/auth/siia` alumno: `cuentaPendiente` si carrera sin mapear | ✅ |
| 6 | `POST /api/auth/siia` alumno: `requiereCorreo` si sin correo | ✅ |
| 7 | `POST /api/auth/siia` docente: emite `ticket` (no token directo) | ✅ |
| 8 | `POST /api/auth/siia` docente: `correoEnmascarado` en respuesta | ✅ |
| 9 | `POST /api/otp/enviar`: acepta `tipo=DOCENTE` | ✅ |
| 10 | `POST /api/otp/enviar`: correo docente viene de `Docentes.Correo` (BD) | ✅ |
| 11 | `POST /api/otp/enviar`: `IdDocente` en `OtpHistorial` | ✅ |
| 12 | `POST /api/otp/enviar`: responde `correoEnmascarado` | ✅ |
| 13 | `POST /api/otp/verificar`: acepta `tipo=DOCENTE` | ✅ |
| 14 | `POST /api/otp/verificar`: emite `firmarTokenDocente` para docentes | ✅ |
| 15 | `POST /api/otp/verificar`: actualiza `Docentes.UltimoAcceso` | ✅ |

### Pendiente antes de reiniciar backend

- [x] Ejecutar `24_VinculoSiia.sql` en AppTeschiDB (agrega `OrigenCuenta`, `UsuarioSiia`, `IdCarreraSiia`)
- [x] Copiar `server.js` al servidor Ubuntu (scp manual)
- [x] Reiniciar el proceso Node.js en el servidor Ubuntu (nohup, PID 694322, puerto 4000)

### Pendiente Fase 2 — App Android
- [ ] `LoginScreen`: flujo unificado SIIA (usuario + contraseña → ticket → OTP → JWT)
- [ ] `CuentaPendienteScreen`: mensaje cuando `cuentaPendiente=true`
- [ ] `CambioPasswordRequeridoScreen`: advertencia cuando `cambioPw=true`
- [ ] Pantalla OTP: mostrar `correoEnmascarado` (ya viene del servidor)
- [ ] `AuthRepository`: actualizar para manejar `ticket` en respuesta
- [ ] `MainActivity`: routing por rol (`Alumno`, `Docente`, `Administrador`)

---

## ✅ Fase 2 completada — Despliegue en Ubuntu (23/09/2026)

### Infraestructura instalada en el servidor
- **Node.js 20.20.2** instalado via NodeSource (el servidor no tenía Node).
- **mssql-tools18** instalado para ejecutar `sqlcmd` desde la terminal.
- **Permisos `.env`** corregidos: `sudo chmod 644 .env` (el archivo pertenecía al usuario `appteschi` y `dotenv` no podía leerlo).
- Backend iniciado con `nohup node server.js > logs/server.log 2>&1 &` (directorio `logs/` creado primero).

### Migraciones SQL ejecutadas en AppTeschiDB
La DB en Ubuntu tenía una versión anterior — las columnas de Fase 2 no existían. Se ejecutaron manualmente con `sqlcmd`:

| Objeto | Estado |
|---|---|
| `Alumnos.OrigenCuenta` NVARCHAR(10) DEFAULT 'PROPIA' | ✅ Agregado |
| `Alumnos.UsuarioSiia` NVARCHAR(50) NULL | ✅ Agregado |
| `CatalogoCarreras.IdCarreraSiia` INT NULL | ✅ Agregado |
| `UX_Alumnos_UsuarioSiia` (índice único parcial) | ✅ Creado |
| `UX_CatalogoCarreras_IdCarreraSiia` (índice único parcial) | ✅ Creado |

> **Nota:** Se requirió `SET QUOTED_IDENTIFIER ON` antes de los `ALTER TABLE` por configuración del usuario SQL.

### Estado final
- Backend Fase 2 corriendo en `http://0.0.0.0:4000` ✅
- BD con esquema Fase 2 aplicado ✅
- Código confirmado: `Alumnos` emite TICKET, OTP soporta DOCENTE ✅

### Próximos pasos
- Mapear carreras: llenar `CatalogoCarreras.IdCarreraSiia` con los IDs del SIIA para que el auto-create de alumnos funcione.
- **Fase 2 Android**: `LoginScreen`, `CuentaPendienteScreen`, OTP con `correoEnmascarado`, routing por rol.
- **Fase 3**: Ejecutar `25_Docentes.sql`, agregar `GET /api/docente/mi-perfil`, pantallas de docente.
