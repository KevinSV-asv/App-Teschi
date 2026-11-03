# Guía de Postman — AppTESCHI

- **Actualizado:** 02/11/2026: colección nueva del flujo de la app con la API 1.2 (DEC-044).

- **Relacionado con:** [[API_SIIA_Oficial]], [[2FA_Login]], [[00_ARQUITECTURA_API]]

---

## Colección de la app: `SIIA_Alumno.postman_collection.json`

Está en esta misma carpeta. Es exactamente lo que hace la app contra el SIIA, solo consulta y no trae secretos. **Es la que se usa.**

### Cómo usarla

1. En Postman: **Import** → arrastra `01_APIS_POSTMAN/SIIA_Alumno.postman_collection.json`.
2. Abre la colección **SIIA TESCHI - Alumno (API 1.2)** → pestaña **Variables**. Llena `usuario`, `password` y `nip` en la columna de valor **local o actual** (*Current* / *Local value*), **no** en la inicial o compartida (*Initial* / *Shared value*), y guarda.
3. Corre las peticiones **en orden**. La 1 y la 2 guardan solas el token (dura 8 horas). Para pedir otro periodo en la 3 o la 4: pestaña **Params** → activa `periodo` y llena la variable, p. ej. `2026-1`.
4. Revisa la pestaña **Test Results** de cada petición. Los nombres de las pruebas no traen datos personales, así que se pueden compartir para diagnosticar.
5. Para ver el PDF, en la 7 usa la flecha junto a **Send** → **Send and Download**, y guárdalo fuera del repositorio.

| # | Petición | Método y ruta (sobre `{{base_url}}`) | Token |
|---|---|---|---|
| 1 | Login | `POST /login.ashx` | No |
| 2 | Verificar NIP | `POST /nip.ashx` | No (usa `temp_token`) |
| 3 | Calificaciones parciales | `GET /calificaciones/parciales.ashx` | Bearer |
| 4 | **Calificaciones finales** | `GET /calificaciones/finales.ashx` | Bearer |
| 5 | **Kardex** | `GET /calificaciones/kardex.ashx` | Bearer |
| 6 | **Kardex PDF (como la app)** | `GET /calificaciones/kardex-pdf.ashx?base64=1` | Bearer |
| 7 | **Kardex PDF (archivo)** | `GET /calificaciones/kardex-pdf.ashx` | Bearer |

> ⚠️ Las respuestas traen datos reales (la 5 incluye la CURP): no las pegues en el repositorio ni en la bóveda (DEC-032). Tampoco exportes la colección con tus valores llenos.

### Otra forma: importar toda la especificación

**Import** → pega `https://siia.teschi.edu.mx/Teschi/api/swagger/openapi.json`. Crea una petición por cada endpoint, pero sin el guardado automático del token ni las verificaciones, y hay dos cosas que cuidar:

- La especificación trae `servers: ".."`: cambia la variable `baseUrl` a `https://siia.teschi.edu.mx/Teschi/api`.
- Incluye `POST /asistencia/guardar.ashx`, que **escribe asistencia real**: ⛔ no ejecutarlo.

## Colección anterior (`07_BACKEND/AppTeschi.Api/`)

> Histórica: sus carpetas de AppTESCHI llaman a `AppTeschi.Api`, que ya no se usa (DEC-043), y los entornos con `100.111.170.32:4000` (Tailscale) ya no hacen falta. Para probar lo que hace la app, usa la colección de arriba. Se conserva por los endpoints de asistencia de docentes.

## Archivos (en `07_BACKEND/AppTeschi.Api/`)

| Archivo | Para qué |
|---|---|
| `Postman_Collection_AppTeschi.json` | La colección. Se versiona |
| `Postman_Environment_AppTeschi_Servidor.example.json` | Entorno que apunta al servidor real (`http://100.111.170.32:4000`, por Tailscale). Se versiona sin secretos |
| `Postman_Environment_AppTeschi.example.json` | Entorno local (`http://localhost:4000`). Se versiona sin secretos |
| `Postman_Environment_AppTeschi.json` | Tu copia local con valores reales. **No se versiona** (`.gitignore`) |

Importa la colección y uno de los entornos (**Import** → arrastra el `.json`), y elige el entorno arriba a la derecha.

## Carpetas de la colección

### 1. "SIIA - API real (directa)" — habla directo con la escuela

Llama a `https://siia.teschi.edu.mx/Teschi/api` sin pasar por AppTESCHI. Sirve para ver exactamente qué devuelve el SIIA (tipo, `idCarrera`, correo). Detalle de cada endpoint en [[API_SIIA_Oficial]].

| # | Request | Notas |
|---|---|---|
| 1 | Login (SIIA real) | Llena `siia_usuario` / `siia_password`. Guarda el token en `siia_token` y escribe en la consola de Postman `tipo`, `idCarrera`, `area`, etc. |
| 2 | Listar grupos del docente | Necesita `siia_periodo`. Con una cuenta de alumno no se espera información útil |
| 3 | Listar alumnos y asistencia de un grupo | Necesita `siia_grupo`, `siia_idMateria`, `siia_periodo` |
| 4 | Guardar o retirar asistencia | ⛔ **No ejecutar**: escribe asistencia real en el SIIA |

### 2. "SIIA - Login real" — el flujo completo de AppTESCHI

Mismo camino que sigue la app: `POST /api/auth/siia` → `POST /api/otp/enviar` → `POST /api/otp/verificar`. Los scripts de cada request pasan solos `ticket`, `identificador` y `tipo` al siguiente. Después de "Enviar", escribe en `codigo` el código que llegó al correo y ejecuta "Verificar": el token de sesión queda en `alumno_token`.

### 3. Requests sueltas

`Health Check`, `Consultar catalogos de registro`, `Crear cuenta AppTESCHI`, `Iniciar sesión cuenta AppTESCHI` (cuenta propia) y `Consultar usuarios` (requiere el token de un administrador en `admin_token`).

## Variables de la colección

| Variable | Se llena | Uso |
|---|---|---|
| `base_url` | Entorno | Backend de AppTESCHI |
| `siia_base_url` | Ya viene | API oficial del SIIA |
| `siia_usuario`, `siia_password` | A mano | Credenciales del SIIA |
| `siia_token` | Script del login directo | Bearer para asistencia |
| `siia_grupo`, `siia_idMateria`, `siia_periodo` | A mano | Asistencia |
| `ticket`, `identificador`, `tipo` | Scripts del flujo AppTESCHI | Paso a paso del login |
| `codigo` | A mano | OTP recibido por correo |
| `alumno_token`, `admin_token` | Script / a mano | Endpoints protegidos |

## Problemas conocidos

| Síntoma | Causa | Solución |
|---|---|---|
| 422 "Usuario y password son requeridos" aunque las variables estén llenas | Postman no sustituyó `{{siia_usuario}}` / `{{siia_password}}` en el Body de esa request (pasó tras importar la colección varias veces). Con los valores escritos directo en el Body sí responde 200 | Borra la colección duplicada, vuelve a importar una sola vez y **escribe** (no pegues) `{{siia_usuario}}` en el Body; el nombre se pone naranja/verde si Postman lo reconoce |
| 429 del SIIA | Más de 5 intentos por usuario + IP en 60 s | Esperar un minuto |
| 401 en las peticiones 3 a 7 | Falta el token o venció (8 h) | Corre otra vez la 1 y la 2 |
| 401 en la 2 | NIP incorrecto o `temp_token` vencido (10 min) | Revisa `nip` y corre otra vez la 1 |
| 403 con `bloqueado: true` | Trámites pendientes en el SIIA | No es un error de la API: complétalos en el portal |
| `ECONNREFUSED` / timeout contra `100.111.170.32` | Tailscale apagado en la PC | Encender Tailscale |

> ⚠️ Nunca guardes tu contraseña real escrita en el Body de una request: queda dentro de la colección si la exportas. Usa siempre la variable, y en el entorno márcala como tipo *secret*.
