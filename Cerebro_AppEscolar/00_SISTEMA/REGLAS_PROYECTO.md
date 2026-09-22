# Reglas y Arquitectura del Proyecto AppTESCHI

> Fuente de verdad técnica. Toda decisión de stack y arquitectura se rige por estas reglas.

---

## Tech Stack

| Capa              | Tecnología                                              |
|-------------------|---------------------------------------------------------|
| Lenguaje          | Kotlin                                                  |
| UI Framework      | Jetpack Compose (Material3)                             |
| Arquitectura      | MVVM (Model – View – ViewModel)                         |
| Navegación        | Navigation Compose 2.8.0                                |
| HTTP / Scraping   | **OkHttp 4.12.0** (directo, con `CookieJar` manual)    |
| Parsing HTML      | **Jsoup 1.18.1** (web scraping del portal ASP.NET)     |
| Serialización     | Gson (para mocks/Postman; no hay API REST oficial)      |
| Localización GPS  | FusedLocationProviderClient                             |
| Autenticación     | Cookies ASP.NET (`ASP.NET_SessionId`) + 2FA OTP         |

> **No se usa Retrofit como cliente primario** porque el SIIA no expone endpoints REST.
> Retrofit puede usarse únicamente para el endpoint de Auditoría cuando TI lo entregue.
> Todo acceso al SIIA se hace mediante **POST de formularios ASP.NET** parseando HTML con Jsoup.

---

## Conexión al SIIA

| Parámetro          | Valor                                     |
|--------------------|-------------------------------------------|
| URL base           | `http://148.230.236.166/Teschi/`          |
| Protocolo sesión   | Cookie `ASP.NET_SessionId`                |
| Anti-CSRF          | `__VIEWSTATE` + `__EVENTVALIDATION`       |
| Estrategia         | Web Scraping + simulación de formularios  |
| Mocking/Testing    | Colección Postman con respuestas simuladas|

---

## Paleta de Colores Institucionales

| Token             | Valor Hex | Uso                            |
|-------------------|-----------|--------------------------------|
| `GreenPrimary`    | `#1E5631` | Botones principales, TopAppBar |
| `GreenSecondary`  | `#4C9A2A` | Pestañas activas, acentos      |
| `GreenTertiary`   | `#2E7D32` | Estados hover / focus          |
| `BackgroundLight` | `#F4F7F4` | Fondo general                  |
| `SurfaceWhite`    | `#FFFFFF` | Cards y superficies            |
| `DarkSurface`     | `#0C2B14` | Texto sobre fondo claro        |
| `LoginFieldBg`    | `#F0F2F0` | Fondo de campos en pantalla login |
| `LoginGreenBright`| `#4CAF50` | Sección inferior y botón principal login |
| `TextMuted`       | `#757575` | Subtítulos en login               |
| `ErrorRed`        | `#B00020` | Estados de error                  |

> Definidos en `ui/theme/Color.kt`. Nunca hardcodear hex fuera de ese archivo.
> `AppTeschiTheme` en `Theme.kt` — Dynamic Color desactivado (paleta institucional fija).

---

## Reglas de Generación de Código

1. Usar **exclusivamente Jetpack Compose** para UI; no XML layouts.
2. La **lógica de negocio vive en el ViewModel**; el Composable solo observa estado y llama funciones.
3. Todo Composable de pantalla recibe `navController` + `ViewModel`; sin lógica directa.
4. Manejar siempre: `Loading`, `Success` y `Error` (+ `Empty` cuando aplique).
5. Toda acción del usuario que toque el SIIA pasa por el **interceptor de Auditoría** (GPS + IP).
6. Layouts responsivos: celular (360 dp) y tablet (600 dp+).
7. Dependencias en `gradle/libs.versions.toml`; no usar strings hardcoded en `build.gradle.kts`.
8. **Dynamic Color (Material You) desactivado** — paleta institucional fija.
9. La Cookie `ASP.NET_SessionId` se mantiene en un `CookieJar` en memoria; **nunca** se persiste en disco sin cifrado.
10. Los nombres de campos del formulario ASP.NET (`__VIEWSTATE`, `__EVENTVALIDATION`, inputs) deben extraerse dinámicamente del HTML antes de cada POST, no hardcodearse.

---

## Estructura de Módulos y ViewModels

| # | Módulo               | ViewModel                  | Ruta                     |
|---|----------------------|----------------------------|--------------------------|
| — | Autenticación        | `AuthViewModel`            | `Routes.LOGIN`           |
| 1 | Reinscripción        | `ReinscripcionViewModel`   | `Routes.REINSCRIPCION`   |
| 2 | Tira de Materias     | `TiraMateriasViewModel`    | `Routes.TIRA_MATERIAS`   |
| 3 | Calificaciones       | `CalificacionesViewModel`  | `Routes.CALIFICACIONES`  |
| 4 | Intersemestral       | `IntersemestralViewModel`  | `Routes.INTERSEMESTRAL`  |
| 5 | Recuperar Contraseña | `RecuperarPassViewModel`   | `Routes.RECUPERAR_PASS`  |

---

## Convenciones de Nomenclatura

- **Archivos Kotlin:** `PascalCase` (ej. `LoginScreen.kt`, `AuthViewModel.kt`)
- **Rutas de navegación:** `snake_case` constantes en el objeto `Routes`
- **Notas de bóveda:** `NN_NombreModulo.md` con prefijo numérico de dos dígitos
- **Commits:** prefijo `feat:` / `fix:` / `docs:` / `refactor:` + descripción en español

---

## Relacionado con

- [[MAPA_PROYECTO]]
- [[PROMPTS]]
- [[2FA_Login]]
