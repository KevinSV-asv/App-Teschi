# Bitácora de Avances — 27 de Agosto de 2026

- **Relacionado con:** [[MAPA_PROYECTO]]

---

## Completado en Esta Sesión

- [x] Fix compilación `EmailOtpService.kt` — `Log.d()` devolvía `Result<Int>` en lugar de `Result<Unit>`.
- [x] OTP permitido a **cualquier correo válido** (temporal, hasta cuenta SMTP institucional).
- [x] Validación de contraseña **real contra el SIIA** vía web scraping (`SiiaAuthService`).
- [x] Creado `AuthViewModel` — flujo login: SIIA → OTP por correo → 2FA.
- [x] Creado `InMemoryCookieJar` — sesión ASP.NET solo en RAM.
- [x] Login rediseñado: Matrícula + Contraseña SIIA + Correo para código 2FA.
- [x] Dependencias activadas: OkHttp 4.12.0, Jsoup 1.18.1, ViewModel Compose.
- [x] `AndroidManifest.xml` — `usesCleartextTraffic=true` para HTTP del SIIA.
- [x] UI login rediseñada estilo web TESCHI (`ui/login/LoginScreen.kt`).
- [x] OTP oculto en pantalla — solo por correo.
- [x] SQL Server Express: esquema `AppTeschiDB` + procedimientos almacenados.
- [x] Carpeta `04_BASE_DATOS/` creada en bóveda con scripts y documentación.

---

## Decisiones Técnicas Registradas

- **DEC-007** — OTP a cualquier correo mientras TI no entrega SMTP institucional (ver [[DECISIONES_TECNICAS]]).
- **DEC-008** — Login SIIA antes de enviar OTP; sin credenciales válidas no avanza al paso 2FA.
- **DEC-009** — UI login inspirada en portal web TESCHI (ver [[DECISIONES_TECNICAS]]).
- **DEC-010** — Base de datos SQL Server Express para perfiles de usuario (ver [[Base_Datos_Usuarios]]).

---

## Pendiente para Próxima Sesión

- [ ] Obtener cuenta SMTP institucional de TI y restringir OTP a `@teschi.edu.mx`.
- [ ] Probar envío OTP a Gmail personal en celular físico.
- [ ] Conectar app Android a SQL Server Express vía API REST.
- [x] Base de datos `AppTeschiDB` creada en servidor VICTUS (27/08/2026).
- [x] Dashboard rediseñado según referencia web `teschi.teschi.edu.mx`.
- [ ] Implementar pantalla de Registro (`Routes.REGISTRO`).
- [ ] Activar auditoría GPS al verificar OTP.

---

## Sesión 12:38 — OTP libre + contraseña real SIIA

### Problema reportado
1. El código OTP no llegaba al correo institucional.
2. La app dejaba entrar con cualquier contraseña.
3. Solicitud de registrar todos los cambios en la bóveda Obsidian.

### Solución aplicada

| Archivo | Cambio |
|---|---|
| `service/EmailOtpService.kt` | Quitada restricción `@teschi.edu.mx`; acepta cualquier email válido |
| `service/SiiaAuthService.kt` | **Nuevo** — GET+POST a Login.aspx / default.aspx con tokens ASP.NET |
| `network/InMemoryCookieJar.kt` | **Nuevo** — CookieJar en memoria |
| `viewmodel/AuthViewModel.kt` | **Nuevo** — orquesta SIIA + OTP + estado 2FA |
| `MainActivity.kt` | 3 campos en login; usa AuthViewModel; ya no avanza sin SIIA |
| `app/build.gradle.kts` | OkHttp, Jsoup, lifecycle-viewmodel-compose |
| `gradle/libs.versions.toml` | Entradas okhttp + jsoup |
| `AndroidManifest.xml` | `usesCleartextTraffic=true` |

### Flujo actual de login

```
[Matrícula + Contraseña SIIA + Correo OTP]
        │
        ▼ POST al SIIA (credenciales reales)
   ¿Válidas? ──No──► Error "Usuario o contraseña incorrectos"
        │
       Sí
        ▼
   Enviar OTP al correo indicado (cualquier dominio)
        │
        ▼
   Paso 2FA — verificar código de 6 dígitos
        │
        ▼
   Dashboard
```

### Cómo probar

1. Sync Gradle en Android Studio.
2. Run en celular con internet (WiFi o datos).
3. Ingresar **matrícula y contraseña reales del SIIA**.
4. En "Correo para código 2FA" usar tu Gmail personal u otro correo accesible.
5. ~~Con `DEV_MODE = true` el código también aparece en pantalla.~~ **Actualizado 13:01** — OTP ya no se muestra en pantalla.

---

## Sesión 13:01 — UI login estilo web TESCHI + SQL Express

### Solicitud del usuario
1. Ocultar el código OTP de la pantalla (modo producción).
2. Rediseñar login similar a la referencia web `teschi.teschi.edu.mx`.
3. Crear SQL para almacenar usuarios en SQL Server Express.
4. Registrar todo en la bóveda.

### Cambios aplicados

| Archivo | Cambio |
|---|---|
| `ui/login/LoginScreen.kt` | **Nuevo** — UI con sección blanca + footer verde TESChi |
| `viewmodel/AuthViewModel.kt` | Flujo en 2 pasos: credenciales SIIA → verificación correo/OTP |
| `ui/theme/Color.kt` | Tokens `LoginGreenBright`, `LoginFieldBg`, `TextMuted` |
| `MainActivity.kt` | Login extraído; ruta `REGISTRO` añadida |
| `database/sql/01_CrearBaseDatos.sql` | **Nuevo** — BD `AppTeschiDB`, tablas Usuarios/OtpHistorial/SesionesLogin |
| `database/sql/02_ProcedimientosUsuarios.sql` | **Nuevo** — SPs upsert, OTP, sesión |
| `04_BASE_DATOS/Base_Datos_Usuarios.md` | **Nuevo** en bóveda — documentación de la BD |

### Diseño del login (referencia imagen)

- **Superior blanco:** título, matrícula, contraseña, ¿Olvidaste tu contraseña?, botón verde "Iniciar Sesión"
- **Inferior verde:** logo TESChi, ¡Bienvenido!, ¿NO TIENES UNA CUENTA?, botón Registrarse
- **Paso 2FA:** correo + enviar código + verificar (sin mostrar OTP en pantalla)

### DEV_MODE

- Eliminado de `MainActivity.kt` — el OTP **solo llega por correo**.

---

## Sesión 13:16 — BD en VICTUS + dashboard estilo web

### Confirmación del usuario
- `AppTeschiDB` creada en SSMS (servidor **VICTUS**, SQL Server 15.0).
- Tablas: `Usuarios`, `OtpHistorial`, `SesionesLogin` + procedimientos almacenados.
- Compartió captura del dashboard web `teschi.teschi.edu.mx` como referencia post-login.

### Implementado en app

| Archivo | Cambio |
|---|---|
| `ui/dashboard/DashboardScreen.kt` | Header TESChi, hero bienvenida, grid módulos, FAB asistente |
| `data/UserSession.kt` | Matrícula y correo OTP en memoria tras login |
| `02_MODULOS/00_Dashboard.md` | Documentación de diseño en bóveda |

### Nota sobre nombre de tabla
Si en SSMS aparece `OtpHitorial` (sin la **s**), renombrar a `OtpHistorial` para coincidir con los scripts.
