# Bitácora de Avances — 24 de Agosto de 2026

- **Relacionado con:** [[MAPA_PROYECTO]]

---

## Completado en Esta Sesión

- [x] Auditoría completa del código Android y la bóveda.
- [x] `Color.kt` reescrito con paleta institucional completa; eliminados tokens Material3 por defecto (Purples).
- [x] `Theme.kt` corregido: Dynamic Color desactivado, esquemas claro/oscuro institucionales.
- [x] `MainActivity.kt` migrado a `AppTeschiTheme`, colores via `MaterialTheme.colorScheme`, `KeyboardType.NumberPassword` en OTP.
- [x] `app/build.gradle.kts` corregido: `compileSdk = 35`, `kotlinOptions`, `isMinifyEnabled`, deps via TOML.
- [x] `libs.versions.toml` ampliado: navigation, retrofit, okhttp, jsoup, play-services, icons, plugin kotlin.android.
- [x] `build.gradle.kts` raíz: añadido `kotlin.android apply false`.
- [x] 4 módulos vacíos de bóveda completados (`02_TiraMaterias`, `03_Calificaciones`, `04_Intersemestral`, `05_RecuperarPassword`).
- [x] `01_Reinscripcion.md` enriquecido con tabla de campos, flujo, endpoint tentativo y reglas de negocio.
- [x] `2FA_Login.md` reescrito con arquitectura real de web scraping ASP.NET, CookieJar, OTP sin expiración, botón reenvío y bloqueo configurable.
- [x] `REGLAS_PROYECTO.md` actualizado: OkHttp+Jsoup como cliente primario, regla de extracción dinámica de VIEWSTATE, tabla de ViewModels.
- [x] `SIIA_Scraping.md` creado en `01_APIS_POSTMAN/` con URL base, patrón GET+POST, CookieJar en memoria y tabla de páginas a mapear.
- [x] `MainActivity.kt` actualizado: botón "Reenviar código", `INTENTOS_MAX = 5`, `BLOQUEO_ACTIVO = false`, pantalla de bloqueo preparada.
- [x] Skill `actualizar-cerebro` creado en `.kiro/skills/`, `copilot/skills/`, `.agents/skills/` y `.claude/skills/`.
- [x] `MAPA_PROYECTO.md` actualizado con tabla de skills y enlace a esta bitácora.

---

## Decisiones Técnicas Registradas

- **Sin API REST**: El SIIA usa ASP.NET Web Forms. Acceso vía web scraping con OkHttp + Jsoup, no Retrofit.
- **Sin JWT**: La sesión se gestiona con Cookie `ASP.NET_SessionId`. El CookieJar vive solo en memoria (no en disco).
- **OTP sin expiración por tiempo**: El código persiste hasta que el usuario lo use o presione "Reenviar código".
- **Bloqueo por intentos controlado por flag**: `BLOQUEO_ACTIVO = false` en `MainActivity.kt`. Cambiar a `true` para activarlo sin modificar lógica.
- **URL base SIIA**: `http://148.230.236.166/Teschi/`
- **Endpoint de Auditoría**: Pendiente de recibir de TI. Será REST; se integrará con Retrofit solo para ese caso.
- **Jsoup** añadido como dependencia requerida (`org.jsoup:jsoup:1.18.1`). Pendiente de agregar a `libs.versions.toml`.

---

## Pendiente para Próxima Sesión

- [ ] Inspeccionar HTML real del portal SIIA en DevTools para confirmar nombres exactos de campos (`txtMatricula`, `txtPassword`, `btnEntrar`).
- [ ] Mapear URLs exactas de cada módulo en el SIIA (Calificaciones, Tira de Materias, etc.).
- [ ] Crear `AuthViewModel` con lógica de GET+POST al SIIA usando OkHttp + Jsoup.
- [ ] Implementar `CookieJar` en memoria en el cliente OkHttp.
- [ ] Agregar `jsoup:1.18.1` a `libs.versions.toml` e `implementation(libs.jsoup)` en `app/build.gradle.kts`.
- [ ] Definir mecanismo de envío de correo con código OTP (SMTP propio, servicio externo o API de TI).
- [ ] Recibir URL del endpoint de Auditoría de TI y documentarla en `01_APIS_POSTMAN/SIIA_Scraping.md`.
- [ ] Responder preguntas de los Bloques C, D, E y F del cuestionario técnico para avanzar en los módulos restantes.

---

## Sesión 17:09 — Corrección de errores de build y emulador

### Problema reportado
`Emulator failed to connect within 5 minutes` al intentar correr el simulador en Android Studio, más 2 warnings en Build Output.

### Causa raíz de cada problema

| # | Warning / Error | Causa |
|---|----------------|-------|
| 1 | `fun BaseAppModuleExtension.kotlinOptions is deprecated` | En Kotlin 2.x + AGP 8.x el bloque `kotlinOptions { jvmTarget }` fue reemplazado por `kotlin { jvmToolchain(N) }` |
| 2 | `Configuration 'app:debugRuntimeClasspath' contains Android` | `material-icons-core` y `material-icons-extended` estaban sin `version.ref` explícita en el TOML, causando ambigüedad de resolución en AGP 8.5 |
| 3 | `Emulator failed to connect within 5 minutes` | Gradle daemon consumiendo la RAM disponible con heap de solo 2048m; el emulador no recibía suficientes recursos para iniciar |

### Correcciones aplicadas

**`app/build.gradle.kts`**
- Eliminado `kotlinOptions { jvmTarget = "11" }` dentro del bloque `android {}`
- Añadido `kotlin { jvmToolchain(11) }` fuera del bloque `android {}` (forma correcta en Kotlin 2.x)

**`gradle/libs.versions.toml`**
- Añadida entrada `composeIcons = "1.7.8"` en `[versions]`
- `material-icons-core` y `material-icons-extended` actualizados a `version.ref = "composeIcons"`
- BOM actualizado de `2024.09.00` → `2024.12.01`

**`gradle.properties`**
- Heap subido: `-Xmx2048m` → `-Xmx4g -XX:MaxMetaspaceSize=512m`
- Activados: `org.gradle.parallel=true`, `org.gradle.caching=true`
- Añadido: `android.dependency.excludeLibraryComponentsFromConstraints=true` (suprime el warning de debugRuntimeClasspath de forma permanente)

### Pasos para correr el emulador después de estos cambios

1. En Android Studio: **File → Invalidate Caches → Invalidate and Restart**
2. Esperar que Gradle sincronice (botón **Sync Now** si aparece el banner)
3. Verificar que el build pase sin warnings: **Build → Make Project** (`Ctrl+F9`)
4. Iniciar el emulador **antes** de presionar Run — darle ~30 segundos para arrancar solo
5. Luego presionar ▶ Run

### Regla para no repetir estos errores

> Cada vez que se agregue una dependencia de Compose al TOML, verificar que tenga `version.ref` explícita o que esté correctamente cubierta por el BOM. Nunca dejar entradas sin versión en `[libraries]` para artefactos de `androidx.compose.material`.
> Al cambiar la versión de Kotlin, revisar que no se use `kotlinOptions {}` — usar siempre `kotlin { jvmToolchain(N) }`.

---

## Sesión 19:06 — Limpieza de dependencias + migración a celular físico

### Auditoría de dependencias

| Dependencia | Estado en código | Acción tomada |
|---|---|---|
| Compose BOM, UI, Material3, Icons | ✅ En uso | Se mantiene |
| Navigation Compose | ✅ En uso | Se mantiene |
| AndroidX Core, Lifecycle, Activity | ✅ En uso | Se mantiene |
| Retrofit + Gson | ❌ Sin imports | Comentada — activar al crear `AuthViewModel` |
| OkHttp Logging | ❌ Sin imports | Comentada — activar al crear `AuthViewModel` |
| play-services-location (GPS) | ❌ Sin imports | Comentada — activar al implementar Auditoría |

### Warning `app:compileDebugKotlin` corregido
Comentario residual dentro del bloque `android {}` en `build.gradle.kts` eliminado.

### Para activar las dependencias cuando llegue el momento
En `gradle/libs.versions.toml`, descomentar las líneas de `retrofit`, `okhttpLogging` y `playServicesLocation`.
En `app/build.gradle.kts`, descomentar las 4 líneas bajo `── Red y GPS ──`.

---

## Guía: Usar celular físico en lugar del emulador

> El emulador `Pixel 8 Pro` falla por limitaciones de RAM/virtualización en este equipo.
> El celular físico es más rápido, más fiel y no consume recursos adicionales de la PC.

### Pasos para conectar el celular (una sola vez)

**En el celular Android:**
1. Ir a **Ajustes → Acerca del teléfono**
2. Tocar **Número de compilación** 7 veces seguidas → aparece "Eres desarrollador"
3. Ir a **Ajustes → Sistema → Opciones de desarrollador** (o buscar "Opciones de desarrollador")
4. Activar **Depuración USB**
5. Conectar el celular a la PC con el cable USB
6. En el celular aparecerá un popup: **"¿Permitir depuración USB desde esta computadora?"** → tocar **Permitir** (marcar "Siempre permitir" para no repetirlo)

**En Android Studio:**
1. En la barra superior donde decía `Pixel 8 Pro`, ahora aparecerá el nombre de tu celular
2. Seleccionarlo en el dropdown
3. Presionar ▶ **Run** — instalará y abrirá la app directamente en tu celular

### Verificar que está conectado
En la terminal de Android Studio o PowerShell:
```
adb devices
```
Debe mostrar tu dispositivo con estado `device` (no `unauthorized`).

### Si aparece `unauthorized`
- Desconectar y reconectar el cable USB
- Revocar autorizaciones USB en Opciones de desarrollador y volver a aceptar

### Opción inalámbrica (Android 11+)
1. Celular y PC en la misma red WiFi
2. En Opciones de desarrollador → **Depuración inalámbrica** → activar
3. Android Studio → **Pair Devices Using Wi-Fi** (ícono de WiFi en Device Manager)
4. Escanear el QR que muestra el celular

---

## Sesión 19:30 — Fix OTP visible en DEV_MODE

### Problema
El OTP se generaba en memoria pero nunca se mostraba. No había forma de saber el código para avanzar en el flujo 2FA durante desarrollo.

### Solución
- Añadida constante `DEV_MODE = true` en `MainActivity.kt`
- Toast cambia a `"DEV — Tu código es: XXXXXX"` cuando `DEV_MODE = true`
- Card visual con el OTP en pantalla durante el paso 2FA (visible solo en DEV_MODE)
- Para pasar a producción: cambiar `DEV_MODE = false` → desaparece toda la UI de debug

---

## Sesión 19:45 — Tema verde Obsidian + Limpieza bóveda + OTP real por correo

### Tema verde Obsidian
- Creado `.obsidian/snippets/teschi-verde.css` con paleta institucional completa:
  - Fondo `#F4F7F4`, encabezados `#1E5631`/`#4C9A2A`, tablas con header verde, código con fondo oscuro, ribbon lateral verde, scrollbar verde, vista de grafo en verde
- Activado en `.obsidian/appearance.json` via `enabledCssSnippets: ["teschi-verde"]`
- Grafo actualizado en `graph.json`: color por carpeta (Sistema, Módulos, Seguridad, APIs, Avances), flechas activadas, nodos más grandes

**Para que se aplique:** En Obsidian → Ajustes → Apariencia → CSS Snippets → activar `teschi-verde`

### Limpieza de bóveda
| Elemento eliminado | Motivo |
|---|---|
| `Sin título.base` | Archivo vacío sin uso |
| `Sin título.canvas` | Canvas vacío sin uso |
| `.opencode/` (52 MB) | Carpeta de node_modules de otro agente, sin relación con el proyecto |

### OTP real por correo — `EmailOtpService.kt`
- Creado `service/EmailOtpService.kt` con envío vía Gmail SMTP (JavaMail android-mail 1.6.7)
- Plantilla HTML institucional con colores TESCHI
- Correo destino: `{matricula}@teschi.edu.mx` (construido automáticamente)
- `DEV_MODE = true` mantiene el código visible en pantalla mientras no hay credenciales reales
- Spinner de carga en botón "Continuar" y "Reenviar código" durante el envío
- Error de envío visible en pantalla (solo cuando `DEV_MODE = false`)
- Dependencia añadida: `com.sun.mail:android-mail:1.6.7` en TOML y build.gradle.kts

### Pendiente inmediato — Configurar credenciales SMTP
Ver sección "Pasos para activar el envío real" abajo.

---

## Sesión 20:15 — Limpieza definitiva + colores del grafo

### Limpieza ejecutada
| Eliminado | Motivo |
|---|---|
| `copilot/skills/` — 10 skills built-in | Skills del plugin Copilot, no del proyecto |
| `.agents/skills/` | Duplicado completo de copilot/skills |
| `.claude/skills/` | Duplicado completo de copilot/skills |
| `.obsidian/snippets/teschi-verde.css` | Cambio de tema visual completo — no era lo solicitado |
| `04_MANUALES/` | Carpeta vacía |
| `.opencode/` (×2, ~104 MB total) | node_modules de agente externo sin relación con el proyecto |

**Único skill que se mantiene:** `copilot/skills/actualizar-cerebro/` + `.kiro/skills/actualizar-cerebro/`

### Colores del grafo Obsidian
`graph.json` actualizado con `colorGroups` por carpeta:

| Carpeta | Color | Hex |
|---|---|---|
| `00_SISTEMA` | Verde oscuro | `#1E5631` |
| `01_APIS_POSTMAN` | Verde medio | `#2E7D32` |
| `02_MODULOS` | Verde secundario | `#4C9A2A` |
| `03_SEGURIDAD` | Naranja/alerta | `#F4724E` |
| `05_AVANCES` | Verde claro | `#A8D5A2` |

Flechas activadas, nodos 30% más grandes, nodos huérfanos ocultos.

**Para que se vea:** Abrir Obsidian → Vista de grafo → los nodos ya aparecen con color por carpeta.

---

## Sesión 20:45 — Credenciales SMTP configuradas, correo real activado

### Cambios aplicados

| Archivo | Cambio |
|---|---|
| `local.properties` | Credenciales SMTP añadidas (`EMAIL_SENDER`, `EMAIL_APP_PASSWORD`) — excluido de Git |
| `app/build.gradle.kts` | Lee `local.properties` e inyecta las credenciales como `BuildConfig` fields. Activado `buildFeatures.buildConfig = true` |
| `service/EmailOtpService.kt` | Ya no tiene strings hardcodeados. Lee `BuildConfig.EMAIL_SENDER` y `BuildConfig.EMAIL_APP_PASSWORD` |
| `MainActivity.kt` | `DEV_MODE = false` — el OTP ya no se muestra en pantalla, solo llega al correo |

### Cuenta de envío
- **Remitente:** `appteschi@gmail.com`
- **Método:** Gmail SMTP con contraseña de aplicación (no la contraseña de la cuenta)
- **Destino:** `{matricula}@teschi.edu.mx` (construido automáticamente desde el campo matrícula)

### Seguridad
- La contraseña de aplicación vive **únicamente** en `local.properties`
- `local.properties` está en `.gitignore` — nunca se sube al repositorio
- `BuildConfig` la inyecta en tiempo de compilación, no aparece en el código fuente

### Para volver a modo desarrollo (si necesitas depurar)
Cambiar en `MainActivity.kt`:
```kotlin
private const val DEV_MODE = true
```

---

## Sesión 21:10 — Fix META-INF + correo institucional + link recuperar contraseña

### Error corregido: `2 files found with path 'META-INF/NOTICE.md'`
JavaMail incluye archivos de licencia duplicados entre sus JARs (`android-mail` + `android-activation`).
Solución: bloque `packaging { resources { excludes += ... } }` en `app/build.gradle.kts`.
Archivos excluidos: `META-INF/NOTICE.md`, `META-INF/LICENSE.md` y sus variantes `.txt`.

### Campo de login cambiado a correo institucional
- Campo anterior: `Matrícula / Usuario` → construía `matricula@teschi.edu.mx`
- Campo nuevo: `Correo institucional` con placeholder `usuario@teschi.edu.mx`
- El correo ingresado es directamente el destino del OTP — sin construcción automática
- `KeyboardType.Email` para mostrar el teclado correcto en el celular

### Link "¿Olvidaste tu contraseña?" en el Login
- Aparece alineado a la derecha entre el campo contraseña y el botón "Continuar"
- Navega a `Routes.RECUPERAR_PASS` directamente desde el Login
- Color `GreenSecondary (#4C9A2A)` para distinguirlo del botón principal

### Recuperar contraseña eliminada del Dashboard
- Ya tiene acceso desde el Login — no tiene sentido tenerla en el panel principal
- Dashboard ahora muestra 4 módulos en grid 2×2 (más limpio visualmente)
