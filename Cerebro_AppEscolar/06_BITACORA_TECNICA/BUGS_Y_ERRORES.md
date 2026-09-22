# Registro de Bugs y Errores

> Historial de errores encontrados, su causa raíz y la solución aplicada.
> **Relacionado con:** [[MAPA_PROYECTO]]

---

## BUG-001 — Emulador no arranca
- **Fecha:** 26/08/2026
- **Síntoma:** `Emulator failed to connect within 5 minutes`
- **Causa:** Gradle daemon consumía toda la RAM con heap de 2048m. El emulador no recibía recursos para iniciar.
- **Solución:** Subir heap a `-Xmx4g` en `gradle.properties`. Activar `org.gradle.parallel=true`.
- **Estado:** ✅ Resuelto — se optó por celular físico (más eficiente)

---

## BUG-002 — kotlinOptions deprecated
- **Fecha:** 26/08/2026
- **Síntoma:** Warning `fun BaseAppModuleExtension.kotlinOptions is deprecated` en build
- **Causa:** En Kotlin 2.x + AGP 8.x el bloque `kotlinOptions { jvmTarget }` fue reemplazado
- **Solución:** Reemplazar por `kotlin { jvmToolchain(11) }` fuera del bloque `android {}`
- **Estado:** ✅ Resuelto

---

## BUG-003 — debugRuntimeClasspath warning
- **Fecha:** 26/08/2026
- **Síntoma:** `Configuration 'app:debugRuntimeClasspath' contains Android...`
- **Causa:** `material-icons-core/extended` sin `version.ref` explícita en TOML
- **Solución:** Añadir `composeIcons = "1.7.8"` al TOML y referenciar explícitamente. Añadir `android.dependency.excludeLibraryComponentsFromConstraints=true` en `gradle.properties`
- **Estado:** ✅ Resuelto

---

## BUG-004 — META-INF/NOTICE.md duplicado
- **Fecha:** 26/08/2026
- **Síntoma:** `2 files found with path 'META-INF/NOTICE.md' from inputs` al compilar
- **Causa:** JavaMail (`android-mail` + `android-activation`) incluye archivos de licencia duplicados
- **Solución:** Bloque `packaging { resources { excludes += setOf("META-INF/NOTICE.md", ...) } }` en `build.gradle.kts`
- **Estado:** ✅ Resuelto

---

## BUG-005 — OTP no llega al correo institucional
- **Fecha:** 26/08/2026
- **Síntoma:** El código de verificación no llega al correo `@teschi.edu.mx`
- **Causa:** Gmail externo puede ser filtrado por el dominio institucional; SMTP institucional aún no disponible
- **Mitigación temporal (27/08/2026):** OTP permitido a **cualquier correo válido** mientras TI entrega cuenta SMTP
- **Diagnóstico activo:** `DEV_MODE = true` muestra OTP en pantalla y errores SMTP exactos
- **Estado:** 🟡 Mitigado temporalmente — pendiente SMTP institucional de TI
- **Ver:** [[05_AVANCES/27 de Agosto]], DEC-007 en [[DECISIONES_TECNICAS]]

---

## BUG-006 — Login aceptaba cualquier contraseña
- **Fecha:** 27/08/2026
- **Síntoma:** La app avanzaba al paso 2FA sin validar credenciales contra el SIIA
- **Causa:** El flujo solo verificaba que los campos no estuvieran vacíos
- **Solución:** `SiiaAuthService` + `AuthViewModel` — POST real al portal SIIA antes de enviar OTP
- **Estado:** ✅ Resuelto

---

## Plantilla para nuevos bugs

```
## BUG-XXX — Título
- **Fecha:**
- **Síntoma:**
- **Causa:**
- **Solución:**
- **Estado:** 🔴 Abierto / 🟡 En progreso / ✅ Resuelto
```

---

## BUG-007 — Error de compilación `Argument type mismatch: kotlin.Result<kotlin.Unit>`
- **Fecha:** 06/09/2026
- **Síntoma:** Build fallaba en `EmailOtpService.kt` con `Argument type mismatch: actual type is 'kotlin.Result<kotlin.Unit>'`
- **Causa raíz:** El archivo en disco ya tenía `try/catch` correcto. El error era **caché de compilación de AGP 8.13.2** con un snapshot viejo que todavía tenía `runCatching { ... return@withContext Result.success(Unit) }`. Mezclar `runCatching` con `return@withContext` dentro hace que el lambda devuelva `Result<Unit>` donde Kotlin espera `Unit`.
- **Solución:** `File → Invalidate Caches → Invalidate and Restart` en Android Studio. Sin cambio de código necesario.
- **Regla:** Nunca usar `return@withContext Result.success(...)` dentro de `runCatching { }`. Usar siempre `try/catch` puro dentro de `withContext`.
- **Estado:** ✅ Resuelto

---

## BUG-008 — Fondo negro en pantallas con modo oscuro del sistema
- **Fecha:** 06/09/2026
- **Síntoma:** Algunas pantallas (especialmente Scaffold, superficies secundarias) mostraban fondo negro cuando el celular tenía activado el tema oscuro del sistema
- **Causa:** `Theme.kt` tenía `InstitutionalDarkColorScheme` con `surface = DarkSurface` (`#0C2B14`). Aunque el `darkTheme` param era `false`, `isSystemInDarkTheme()` lo podía activar en ciertos Composables que lo llamaban directamente. `surfaceVariant` y `onSurfaceVariant` no estaban explícitamente declarados y tomaban los valores oscuros del `darkColorScheme` base.
- **Solución:** `Theme.kt` reescrito con un único `InstitutionalColorScheme` (solo `lightColorScheme`). Eliminado `darkColorScheme` completamente. Declarados explícitamente `surfaceVariant = BackgroundLight` y `onSurfaceVariant = LoginInk`. Parámetro `darkTheme` conservado en la firma con `@Suppress` pero ignorado.
- **Estado:** ✅ Resuelto

---

## BUG-009 — Módulo de Registro desconectado por reescritura de MainActivity
- **Fecha:** 06/09/2026
- **Síntoma:** El formulario de registro de usuarios (`RegistroScreen`) dejó de funcionar — la ruta `Routes.REGISTRO` mostraba el placeholder genérico "Módulo en desarrollo" en lugar del formulario real
- **Causa:** Al reescribir `MainActivity.kt` para añadir las rutas admin, la línea `composable(Routes.REGISTRO) { RegistroScreen(navController) }` fue reemplazada por `ModuleDetailScreen(...)`. `RegistroScreen.kt` nunca se borró — solo quedó desconectada del NavHost.
- **Solución:**
  1. Añadir import `import com.example.appteschi.ui.login.RegistroScreen` en `MainActivity.kt`
  2. Cambiar `composable(Routes.REGISTRO) { ModuleDetailScreen(...) }` por `composable(Routes.REGISTRO) { RegistroScreen(navController) }`
- **Estado:** ✅ Resuelto
- **Regla derivada:** Antes de reescribir `MainActivity.kt` completo, verificar TODOS los `composable(Routes.X)` que ya existen en la bóveda ([[CAMBIOS_DE_CODIGO]]) y conservarlos intactos. Solo añadir rutas nuevas, nunca reemplazar rutas existentes con placeholders.

---

## BUG-010 — Teclado numérico en campo Matrícula impide escribir "admin"
- **Fecha:** 06/09/2026
- **Síntoma:** El campo "Matrícula" del Login mostraba teclado numérico (`KeyboardType.Number`), bloqueando el ingreso de letras y haciendo imposible escribir el usuario administrador `admin`
- **Causa:** `LoginScreen.kt` línea del `LoginField` de matrícula usaba `keyboardType = KeyboardType.Number`
- **Solución:** Cambiar a `KeyboardType.Text`. El placeholder actualizado a `"Ej. 202400123 o admin"`
- **Archivo:** `app/src/main/java/com/example/appteschi/ui/login/LoginScreen.kt`
- **Estado:** ✅ Resuelto

---

## BUG-011 — Registro de usuarios no llega a la base de datos
- **Fecha:** 06/09/2026
- **Síntoma:** El formulario de registro enviaba los datos pero no aparecían en la BD. El error genérico `"No se pudo conectar con la API"` no permitía diagnosticar la causa real.
- **Causa conocida:** `RegistroViewModel` capturaba TODAS las excepciones en un solo `catch (_: Exception)` y devolvía siempre el mismo mensaje genérico, ocultando el error real.
- **Causas posibles del fallo:**
  1. El servidor Node.js en `192.168.0.41:4000` no está corriendo
  2. El celular y la PC están en redes WiFi diferentes
  3. El campo `fechaNacimiento` no tiene el formato `AAAA-MM-DD` esperado por la API
  4. La API devuelve un error JSON con campo `"error"` que no se mostraba
- **Solución aplicada:** `RegistroViewModel` reescrito con:
  - `ConnectException` → mensaje específico: "No se pudo conectar... verifica que el servidor Node.js esté activo"
  - `SocketTimeoutException` → mensaje específico con tiempo
  - JSON inválido (API devuelve HTML) → muestra las primeras 150 chars de la respuesta
  - Error de servidor → muestra el campo `"error"` exacto que devuelve la API
  - `Log.d/e(TAG, ...)` en cada paso para ver en Logcat el payload enviado y la respuesta recibida
- **Estado:** 🟡 Diagnóstico habilitado — el error exacto ahora se muestra en pantalla
- **Próximo paso:** Ejecutar el registro, leer el mensaje en pantalla y resolver según el error específico que aparezca

---

## BUG-012 — ReinscripcionScreen existía pero no estaba conectada al NavHost
- **Fecha:** 06/09/2026
- **Síntoma:** El módulo de Reinscripción del Dashboard mostraba "Módulo en desarrollo" en lugar del formulario real
- **Causa:** `MainActivity.kt` mapeaba `Routes.REINSCRIPCION` a `ModuleDetailScreen(...)` aunque `ReinscripcionScreen.kt` ya existía en `ui/modules/`
- **Raíz del problema:** Mismo patrón que BUG-009 — la reescritura de `MainActivity.kt` (v0.6) no conservó las rutas existentes
- **Solución:**
  1. Import `com.example.appteschi.ui.modules.ReinscripcionScreen`
  2. `composable(Routes.REINSCRIPCION) { ReinscripcionScreen(navController) }`
- **Estado:** ✅ Resuelto
- **Regla reiterada:** Antes de reescribir `MainActivity.kt`, listar TODAS las rutas existentes en [[CAMBIOS_DE_CODIGO]] y preservarlas. Ver también BUG-009.

---

## BUG-013 — Campo fecha de nacimiento requería escritura manual en formato AAAA-MM-DD
- **Fecha:** 06/09/2026
- **Síntoma:** El campo de fecha en `RegistroScreen` era un `OutlinedTextField` libre que el usuario tenía que completar escribiendo `AAAA-MM-DD` a mano — propenso a errores y mala UX
- **Solución:** Reemplazado por `DatePickerDialog` nativo de Android. El campo queda `readOnly`; el usuario toca el ícono de calendario o el campo para abrir el selector visual. Año por defecto: `hoy − 18 años`. `maxDate = hoy` para impedir fechas futuras.
- **Estado:** ✅ Resuelto

---

## BUG-014 — 5 alumnos de prueba con nombres corruptos ("Mar?a" en vez de "María")

- **Fecha:** 15/09/2026
- **Síntoma:** En el directorio de Alumnos del panel de administrador, los nombres de `2024PRUEBA1` a `2024PRUEBA5` mostraban un glifo de reemplazo (rombo/círculo con "?") en lugar de la vocal acentuada — ej. "Mar�a Fernanda Torres" en vez de "María Fernanda Torres".
- **Diagnóstico:** Se confirmó con `CAST(NombreCompleto AS VARBINARY)` que el valor guardado en `dbo.Alumnos` contenía literalmente el carácter de reemplazo Unicode (U+FFFD), no un problema de renderizado en la app. Una búsqueda `LIKE` normal daba falsos positivos por la collation por defecto — hubo que forzar `COLLATE Latin1_General_BIN` para encontrar las filas realmente afectadas (solo esas 5).
- **Se descartó que fuera un bug activo:** se insertó un alumno de prueba con acentos directamente por el mismo camino que usa el backend (`mssql` + `sql.NVarChar`, el mismo patrón de `POST /api/usuarios`) y los acentos se guardaron y leyeron perfectamente. También se revisaron `Administradores`, `PlanEstudioMaterias`, `CatalogoCarreras` y `CatalogoSistemas` — ninguna otra tabla tiene el problema.
- **Causa real:** Los 5 registros no vienen de ningún script versionado en `database/sql/` — se insertaron por fuera (probablemente con una herramienta que no respetó UTF-8 al escribir el INSERT), mismo tipo de causa que BUG documentado en DEC-014 pero para estos registros puntuales.
- **Solución:** `database/sql/21_CorregirNombresPruebaCorruptos.sql` — `UPDATE` con los 5 nombres correctos, usando literales `N'...'` y ejecutado con `-f 65001`. Verificado a nivel de bytes tras la corrección.
- **Estado:** ✅ Resuelto — sin cambios de código, solo datos.
