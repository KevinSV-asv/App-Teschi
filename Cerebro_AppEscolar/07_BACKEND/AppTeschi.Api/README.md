# AppTeschi API

API (Node.js + Express + SQL Server) de la app Android AppTESCHI. Toda la
persistencia pasa por aquí: la app nunca se conecta directo a SQL Server.

## Requisitos

- Node.js 22 (se desarrolla y prueba con 22.16.0)
- SQL Server con la base `AppTeschiDB` (scripts en `04_BASE_DATOS/sql/`, en orden)
- Archivo `.env` basado en `.env.example`

## Instalar y ejecutar

```bash
npm ci
npm start
```

Si `JWT_SECRET` falta o tiene menos de 32 caracteres, la API no arranca.

## Autenticación (DEC-030)

1. `POST /api/auth/cuenta` (alumno) o `POST /api/auth/administrador`: valida
   usuario y contraseña y responde con un **ticket** de 10 minutos.
2. `POST /api/otp/enviar` y `POST /api/otp/verificar`: exigen ese ticket en el
   cuerpo (`ticket`). Sin él responden 401.
3. `POST /api/otp/verificar` correcto responde con un **token de sesión**
   (`token`, 12 h) que se manda como `Authorization: Bearer <token>`.

Hay tres tipos de token con audiencia distinta (ticket, administrador, alumno):
uno nunca sirve donde se espera otro.

| Quién | Endpoints | Protección |
|---|---|---|
| Alumno | `/api/mi-perfil`, `/api/mi-historial`, `/api/mi-horario`, `/api/reinscripcion/estatus` y `/solicitud` | `requireAlumno()`: solo su propia matrícula, otra responde 403 |
| Administrador | `/api/usuarios`, `/api/calificaciones`, `/api/horarios`, `/api/observaciones`, `/api/auditoria`, `/api/estadisticas`, `/api/administradores`… | `requireAdmin()` (algunos solo `SUPERADMIN`) |
| Público | `/health`, catálogos (`/api/catalogos/registro`, `/api/plan-estudios`, `/api/grupos`), `/api/registro`, `/api/recuperar-password/*`, `/api/auth/*` | límite de intentos por IP |

## Variables de entorno

Ver `.env.example`. Las de despliegue detrás de un túnel (`HOST`,
`TRUST_CLOUDFLARE`) solo se activan juntas y solo con la API escuchando en
`127.0.0.1`.

## Despliegue en el servidor

Ver la carpeta `deploy/` (scripts numerados, servicio `systemd`, SQL Server en
Docker y respaldo diario).

## Notas

- `Postman_Collection_AppTeschi.json` está **desactualizado** (todavía describe
  la `x-api-key` y `/api/usuarios/sync`, que ya no existen). Hay que regenerarlo
  antes de usarlo.
