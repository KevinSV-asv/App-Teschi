# AppTeschi API

> ⛔ **Histórico (12/10/2026, DEC-043):** la app ya no usa este backend; habla directo con la API del SIIA. Se conserva en el repositorio solo como referencia.

API (Node.js + Express + SQL Server) de la app Android AppTESCHI. Toda la
persistencia pasa por aquí: la app nunca se conecta directo a SQL Server.

## Requisitos

- Node.js 22 (servidor: 22.16.0 en `/opt/node`)
- SQL Server con la base `AppTeschiDB` (scripts en `04_BASE_DATOS/sql/`, en orden)
- Archivo `.env` basado en `.env.example`

## Instalar y ejecutar

```bash
npm ci
npm start
```

Si `JWT_SECRET` falta o tiene menos de 32 caracteres, la API no arranca.

## Autenticación (DEC-030, DEC-035)

1. Validar credenciales y recibir un **ticket** de 10 minutos:
   - `POST /api/auth/cuenta` — alumno con cuenta propia.
   - `POST /api/auth/administrador` — administrador.
   - `POST /api/auth/siia` — alumno con su usuario del SIIA; el backend llama a la
     API oficial (`login.ashx`). Solo entra si su matrícula ya existe en `Alumnos`.
2. `POST /api/otp/enviar` y `POST /api/otp/verificar`: exigen ese ticket en el
   cuerpo (`ticket`). Sin él responden 401. El correo destino lo decide el servidor.
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

Ver `.env.example`. El correo del OTP sale por Microsoft Graph si están las cuatro
`MS_*` (cuenta institucional); si falta alguna, por SMTP (`EMAIL_*`). Las de despliegue detrás de un túnel (`HOST`,
`TRUST_CLOUDFLARE`) solo se activan juntas y solo con la API escuchando en
`127.0.0.1`.

## Despliegue en el servidor

Ver la carpeta `deploy/` (scripts numerados, servicio `systemd`, SQL Server en
Docker y respaldo diario).

## Pruebas manuales

Colección y entornos de Postman en esta carpeta; guía en `01_APIS_POSTMAN/01_POSTMAN_API.md` de la bóveda.
