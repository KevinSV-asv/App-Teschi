# Reglas y Arquitectura del Proyecto AppTESCHI

> Fuente de verdad técnica. Actualizada el 02/11/2026 (DEC-048). Desde DEC-043 la app habla directo con la API oficial del SIIA; el backend `AppTeschi.Api` y `AppTeschiDB` quedan solo como histórico.
> **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]], [[API_SIIA_Oficial]], [[DECISIONES_TECNICAS]]

---

## Tech stack

| Capa | Tecnología |
|---|---|
| App | Kotlin 2.0 · Jetpack Compose (Material3, BOM 2024.12) · MVVM · Navigation Compose 2.8 · `core-splashscreen` · `minSdk 24` / `targetSdk 35` |
| Red (app) | OkHttp 4.12 a través de `service/ClienteHttp.kt` (errores de red traducidos, DEC-033); JSON con `org.json` |
| Fuente de datos | API oficial del SIIA v1.2 (`https://siia.teschi.edu.mx/Teschi/api`): `login.ashx`, `nip.ashx`, `calificaciones/parciales.ashx`, `finales.ashx`, `kardex.ashx` y `kardex-pdf.ashx` (DEC-043, DEC-044) |
| Sesión (app) | Token final del SIIA (480 min) + datos de la cuenta en `UserSession` (SharedPreferences fuera del respaldo) |
| Pruebas | JUnit 4 · `kotlinx-coroutines-test` · MockWebServer (SIIA simulado) |
| Histórico, sin uso | `AppTeschi.Api` (Node.js + Express), SQL Server `AppTeschiDB`, servidor Ubuntu con Tailscale (DEC-031), correo por Microsoft Graph (DEC-034) |

## Arquitectura

```
App Android ──HTTPS──► API oficial del SIIA
                        ├─ POST login.ashx                  (usuario + contraseña → tempToken)
                        ├─ POST nip.ashx                    (verificar / configurar / recuperar / cambiar NIP → token)
                        ├─ GET  calificaciones/parciales.ashx (Bearer token)
                        ├─ GET  calificaciones/finales.ashx   (Bearer token)
                        ├─ GET  calificaciones/kardex.ashx    (Bearer token)
                        └─ GET  calificaciones/kardex-pdf.ashx?base64=1 (Bearer token)
```

- No hay servidor propio ni base de datos: todo lo que muestra la app sale del SIIA, salvo la foto de perfil, que solo existe en el teléfono (DEC-048).
- Solo entran alumnos (`tipo = E`); docentes y administrativos quedan para después.
- Lo que el SIIA no expone se dice con claridad: Reinscripción e Intersemestral muestran "Estamos trabajando en este apartado", y el cambio de contraseña lleva al portal. Nada simula funcionar.
- La app valida lo que puede antes de llamar (NIP de 4 dígitos, CURP de 18), pero el SIIA es quien decide.

## Paleta de colores institucionales

| Token | Hex | Uso |
|---|---|---|
| `GreenPrimary` | `#1E5631` | Botones principales, TopAppBar |
| `GreenSecondary` | `#4C9A2A` | Pestañas activas, acentos |
| `GreenTertiary` | `#2E7D32` | Hover / focus |
| `BackgroundLight` | `#F4F7F4` | Fondo general |
| `SurfaceWhite` | `#FFFFFF` | Cards y superficies |
| `DarkSurface` | `#0C2B14` | Texto sobre fondo claro |
| `ErrorRed` | `#B00020` | Errores |
| `SecurityTeal`, `SecurityBlueContainer`, `OnSecurityBlue`, `SlateBorder`, `SlateSurface` | — | Seguridad de la Cuenta (Mi perfil) |
| `LoginCanvas`, `LoginSurface`, `LoginInk`, `LoginAccent`, `LoginFieldBg`, `LoginGreenBright`, `LoginGreenDark` | — | Pantalla de login |

> Todos en `ui/theme/Color.kt`; nunca hex fuera de ese archivo. Un solo esquema claro, sin Dynamic Color ni modo oscuro (DEC-013).

## Reglas de código

1. UI solo con Jetpack Compose (sin XML).
2. La lógica vive en el ViewModel; el Composable solo observa estado y llama funciones.
3. Cada pantalla maneja `Loading`, `Success`, `Error` (y `Empty` cuando aplique).
4. Toda llamada HTTP pasa por `ClienteHttp` y un `*Service`; las URLs salen de `SiiaEndpoints` (`core/config/ApiConfig.kt`), nunca escritas en otro lado.
5. Solo HTTPS (`usesCleartextTraffic="false"`). El token del SIIA nunca se escribe en logs, nunca va en una URL (solo en el encabezado) ni sale del teléfono a otro lado que no sea el SIIA.
6. Cada servicio tiene una interfaz (`SiiaAuthService`, `CalificacionesSiiaApi`, `KardexSiiaApi`) para probar los ViewModels con fakes; el HTTP se prueba contra MockWebServer.
7. Dependencias en `gradle/libs.versions.toml`.
8. Nunca datos reales de personas en el repositorio (DEC-032): datos ficticios en pruebas (`2099000001`, "Alumno de Prueba Uno").
9. Scripts `.sh` siempre con saltos LF (`.gitattributes`, BUG-018).

## Estructura de módulos

| Módulo | ViewModel / pantalla | Ruta | Fuente |
|---|---|---|---|
| Autenticación | `AuthViewModel` / `LoginScreen` | `Routes.LOGIN` | `login.ashx` + `nip.ashx` |
| Inicio | `InicioViewModel` / `DashboardScreen` | `Routes.DASHBOARD` | Sesión + resumen de parciales |
| Tira de materias | `TiraMateriasViewModel` | `Routes.TIRA_MATERIAS` | Materias de las parciales del periodo actual |
| Calificaciones | `CalificacionesViewModel` | `Routes.CALIFICACIONES` | `calificaciones/parciales.ashx` y `finales.ashx` |
| Reinscripción (con Intersemestral dentro) | `ReinscripcionScreen` | `Routes.REINSCRIPCION` | — (en construcción) |
| Kardex | `KardexViewModel` / `KardexScreen` | `Routes.KARDEX` | `calificaciones/kardex.ashx` y `kardex-pdf.ashx` |
| Perfil del alumno | `PerfilAlumnoViewModel` | `Routes.PERFIL_ALUMNO` | Sesión + `login.ashx` (autorizar) + `nip.ashx` (`cambiar`) |

## Convenciones

- **Kotlin:** archivos en `PascalCase` (`LoginScreen.kt`); rutas como constantes en `object Routes` (`MainActivity.kt`).
- **Commits:** `feat:` / `fix:` / `docs:` / `refactor:` + descripción en español.
- **SQL (histórico):** los scripts de `04_BASE_DATOS/sql/` ya no se aplican en ningún lado (DEC-043).

## Reglas de la bóveda

- Hay **una sola bóveda**: `Cerebro_AppEscolar/` dentro del repositorio `App-Teschi`. La copia vieja en Descargas es solo respaldo; no se edita.
- Todo en español.
- Punto de entrada: [[MAPA_PROYECTO]]. Toda nota nueva se enlaza ahí.
- Cada sesión deja su nota en `05_AVANCES/` (nombre `DD de Mes.md`).
- Decisiones en [[DECISIONES_TECNICAS]] (DEC-xxx) y bugs en [[BUGS_Y_ERRORES]] (BUG-xxx). No se borran: cuando algo deja de aplicar se marca su **vigencia**.
- Una nota que describe algo que ya no existe se borra o se marca como histórica en la primera línea; nunca se deja describiendo como vigente algo que no lo es.
- Nada de secretos (`.env`, contraseñas, tokens) ni datos reales de alumnos en la bóveda versionada.
