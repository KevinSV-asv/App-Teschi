# Módulo de Autenticación y 2FA

- **Estado:** ✅ Integrado y real — **actualizado 15/09/2026 (DEC-026): el SIIA ya no forma parte del login.** Todo lo de abajo sobre `SiiaAuthService`/scraping del SIIA es histórico — se retiró por completo a petición explícita del alumno ("no quiero que dependa del SIIA mi aplicación"). El login ahora solo tiene dos fuentes: bypass de prueba (debug) y cuenta propia de AppTESCHI.
- **Actualización 18/09/2026 (DEC-030):** el OTP ya no se puede pedir ni verificar sin haber validado la contraseña: `/api/auth/*` emite un ticket de 10 min que `/api/otp/*` exige, y al verificar el OTP el alumno también recibe un token de sesión propio (antes solo el administrador). Ver DEC-030 en [[DECISIONES_TECNICAS]].
- **Ruta en App:** `Routes.LOGIN`
- **Relacionado con:** [[MAPA_PROYECTO]], [[REGLAS_PROYECTO]], [[05_RecuperarPassword]]

---

## Arquitectura Real de Sesión (histórico — ya no aplica, ver nota de Estado arriba)

> ⚠️ El SIIA **no expone una API REST**. Utiliza **ASP.NET Web Forms** con sesiones por Cookie.
> No hay JWT ni Refresh Token. La app debe simular un navegador.

| Elemento               | Valor / Descripción                                        |
|------------------------|------------------------------------------------------------|
| URL base               | `http://148.230.236.166/Teschi/`                          |
| Gestión de sesión      | Cookie `ASP.NET_SessionId`                                 |
| Anti-CSRF              | `__VIEWSTATE` + `__EVENTVALIDATION` (campos ocultos HTML)  |
| Autenticación          | POST al formulario de login del portal ASP.NET             |
| Parsing de respuesta   | Web Scraping del HTML con **Jsoup**                        |
| Librería HTTP          | **OkHttp** (gestión manual de cookies con `CookieJar`)     |

---

## Flujo Técnico Completo

```
[Paso 1 — GET página de login]
  GET http://148.230.236.166/Teschi/Login.aspx
  → Parsear HTML: extraer __VIEWSTATE, __EVENTVALIDATION
       │
       ▼
[Paso 2 — POST credenciales al SIIA]
  POST http://148.230.236.166/Teschi/default.aspx?ReturnUrl=%2fTeschi%2fLogin.aspx
  Campos: txtUsuario (matrícula), txtPass, btnAceptar=Iniciar Sesión
       │
       ├─ Respuesta con alert('los datos son errores') → Credenciales inválidas (no avanza)
       └─ Redirección / página interior → Credenciales válidas
              │
              ▼
[Paso 3 — 2FA: Generación y envío de OTP]
  App genera código OTP de 6 dígitos (aleatorio local)
  App envía OTP al correo indicado por el usuario (cualquier dominio — temporal)
       │
       ▼
[Paso 4 — Verificación OTP]
  Usuario ingresa código
  App valida contra el valor generado en memoria
  ┌─ Sin límite de expiración por tiempo ──────────────┐
  │  Botón "Reenviar código" genera un nuevo OTP       │
  │  y reemplaza el anterior en memoria                │
  └────────────────────────────────────────────────────┘
  Intentos fallidos: contador preparado (umbral configurable,
  actualmente sin bloqueo — ver INTENTOS_MAX en AuthViewModel)
       │
       ▼
[Paso 5 — Auditoría]
  App captura: Coordenadas GPS + IP del dispositivo
  POST {URL_AUDITORIA} — endpoint pendiente de recibir de TI
       │
       ▼
[Paso 6 — Sesión activa]
  Cookie ASP.NET_SessionId persiste en CookieJar durante la sesión
  Navegar a Dashboard
```

---

## Campos del Formulario ASP.NET (a confirmar con inspección real)

| Campo              | Tipo   | Descripción                                    |
|--------------------|--------|------------------------------------------------|
| `__VIEWSTATE`      | Hidden | Estado del formulario (extraer del HTML)       |
| `__EVENTVALIDATION`| Hidden | Token anti-CSRF (extraer del HTML)             |
| `__VIEWSTATEGENERATOR` | Hidden | Generador de viewstate (extraer del HTML)  |
| `txtUsuario`       | Text   | Matrícula o usuario SIIA ✅ confirmado         |
| `txtPass`          | Password | Contraseña SIIA ✅ confirmado                |
| `btnAceptar`       | Submit | Valor: `Iniciar Sesión` ✅ confirmado          |

> ✅ Campos confirmados inspeccionando `Login.aspx` el 27/08/2026.
> Error de credenciales inválidas: `alert('los datos son errores')` en la respuesta HTML.

---

## Dependencias Requeridas en `build.gradle.kts`

```kotlin
// Ya incluidas vía TOML:
implementation(libs.okhttp.logging.interceptor)  // OkHttp

// Añadir:
implementation("org.jsoup:jsoup:1.18.1")         // Parsing HTML
```

---

## Estado Actual en el Código

| Elemento                    | Estado                                             |
|-----------------------------|----------------------------------------------------|
| UI Login + 2FA              | ✅ Implementada en `MainActivity.kt`               |
| Botón "Reenviar código"     | ✅ Implementado en `MainActivity.kt`               |
| Contador de intentos fallidos | ✅ Preparado (`maxIntentos` configurable, sin bloqueo activo) |
| GET + POST al SIIA (scraping) | ✅ `SiiaAuthService.kt` — valida matrícula/contraseña |
| CookieJar para sesión ASP.NET | ✅ `InMemoryCookieJar.kt` (solo login por ahora)  |
| AuthViewModel               | ✅ `AuthViewModel.kt` — orquesta SIIA + OTP        |
| Envío de correo con OTP     | ✅ `EmailOtpService.kt` — cualquier correo (temporal) |
| Captura GPS                 | ⚠️ Pendiente — TODO en `LoginScreen`             |
| Endpoint de Auditoría       | ⚠️ Pendiente — URL por recibir de TI             |

---

## Reglas de Seguridad

- La Cookie `ASP.NET_SessionId` se almacena solo en memoria (`CookieJar` en RAM), no en disco.
- El OTP generado vive solo en `AuthViewModel` (memoria de la app), nunca en logs ni preferencias.
- El campo OTP usa `KeyboardType.NumberPassword` (sin sugerencias del sistema).
- Bloqueo por intentos: constante `INTENTOS_MAX = 5` lista en `AuthViewModel`; activar con un solo flag cuando se decida.
