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

---

## BUG-015 — Login por SIIA siempre "Usuario o contraseña del SIIA incorrectos"

- **Fecha:** 24/09/2026
- **Síntoma:** cualquier usuario y contraseña del SIIA, aunque fueran correctos, eran rechazados.
- **Causa:** `/api/auth/siia` no llamaba al SIIA: comparaba contra la columna local `Alumnos.UsuarioSiia`, que estaba vacía.
- **Solución:** llamada real a `login.ashx` con `https` nativo (`loginSiiaReal()`), ver DEC-035.
- **Estado:** ✅ Resuelto

---

## BUG-016 — "Este tipo de cuenta del SIIA no está soportado todavía"

- **Fecha:** 24/09/2026
- **Síntoma:** con credenciales correctas de alumno, el backend respondía 403.
- **Causa:** el SIIA manda `tipo` como una letra (`"E"` para estudiante) y el código esperaba palabras como `ALUMNO`.
- **Solución:** `TIPOS_SIIA_CONOCIDOS = { E: 'ALUMNO', D: 'DOCENTE', A: 'ADMINISTRADOR' }` en `mapTipoSiia()`. Solo `E` está confirmado.
- **Estado:** ✅ Resuelto (`D` y `A` sin confirmar)

---

## BUG-017 — "Matrícula/usuario es obligatorio" al pedir el código tras un login SIIA correcto

- **Fecha:** 25/09/2026
- **Síntoma:** el login SIIA pasaba, pero al tocar "Enviar código" aparecía ese error.
- **Causa (dos a la vez):** (1) `SiiaAuthService.kt` leía `matricula`/`usuario` de la respuesta, pero el backend manda `identificador`, así que la app se quedaba sin matrícula; (2) el backend exigía un correo escrito por el cliente, que el flujo SIIA nunca pide.
- **Solución:** la app lee `identificador` (con respaldo a los nombres viejos); `AuthViewModel` no valida el correo en el flujo SIIA; `/api/otp/enviar` usa el correo guardado en el servidor.
- **Estado:** ✅ Resuelto — probado de punta a punta con un alumno real en el teléfono.

---

## BUG-018 — Scripts de despliegue fallan en Ubuntu: `set: pipefail\r: invalid option name`

- **Fecha:** 24/09/2026
- **Síntoma:** `bash deploy/aplicar.sh` (y los demás `.sh`) fallaban en la primera línea.
- **Causa:** los `.sh` tenían saltos de línea CRLF de Windows.
- **Solución:** se convirtieron a LF y `.gitattributes` fija `*.sh text eol=lf` para que no vuelva a pasar.
- **Estado:** ✅ Resuelto

---

## BUG-019 — El servidor seguía respondiendo con código viejo después de desplegar

- **Fecha:** 24/09/2026
- **Síntoma:** tras desplegar, la API respondía mensajes que ya no existían en el código.
- **Causa:** un `node server.js` lanzado a mano el 23/09 con `nohup` (PID 694322) seguía ocupando el puerto 4000, así que el servicio `appteschi-api` de systemd no podía arrancar el código nuevo.
- **Solución:** `sudo ss -lptn 'sport = :4000'` para ver quién tiene el puerto, matar ese proceso y dejar solo el servicio de systemd. **Regla:** nunca arrancar el backend del servidor a mano; siempre `sudo systemctl restart appteschi-api`.
- **Estado:** ✅ Resuelto

---

## BUG-020 — `sqlcmd` en el servidor: certificado autofirmado y `QUOTED_IDENTIFIER`

- **Fecha:** 27/09/2026
- **Síntoma 1:** `SSL Provider: certificate verify failed: self-signed certificate`. **Causa:** el driver ODBC 18 exige TLS válido y el SQL Server del contenedor usa un certificado autofirmado. **Solución:** agregar `-C` (confiar en el certificado del servidor).
- **Síntoma 2:** `CREATE INDEX failed` / `UPDATE failed because … 'QUOTED_IDENTIFIER'`. **Causa:** `sqlcmd` arranca con `QUOTED_IDENTIFIER OFF` y las tablas con índices filtrados (`Alumnos`, `CatalogoCarreras`) lo exigen incluso para un `UPDATE`. **Solución:** empezar cada script o consulta con `SET QUOTED_IDENTIFIER ON;`.
- **Estado:** ✅ Documentado en [[04_Manual_Mantenimiento]] §7

---

## BUG-021 — "Matrícula/usuario es obligatorio" otra vez, usando la app fuera de casa

- **Fecha:** 28/09/2026
- **Síntoma:** login por el SIIA correcto (se ve el correo enmascarado), pero al tocar "Enviar código" aparece "Matrícula/usuario es obligatorio".
- **Diagnóstico:** el mensaje lo manda `/api/otp/enviar` cuando la app no envía matrícula. Se revisó todo: el servidor corre el código de `3bbb1a9` (mismo tamaño de `server.js`, 114 150 bytes) y sí responde `identificador`; el código actual de la app y la última compilación (24/09, 20:11) sí lo leen (BUG-017). Nada en el código depende de estar en casa o fuera. Conclusión más probable: el teléfono tiene instalada una compilación anterior al arreglo de BUG-017. No había forma de saber qué versión tenía instalada.
- **Solución:**
  - `AuthRepository.loginSiia`: si la respuesta no trae la matrícula, usa el usuario que escribió el alumno (en el SIIA es su matrícula).
  - `AuthViewModel`: si falta matrícula o ticket, no llama al backend y muestra "Se perdió la sesión de verificación. Toca Regresar e inicia sesión de nuevo."
  - El login muestra la versión y la fecha de compilación (`v1.1 · compilada dd/MM/yyyy HH:mm`) para saber siempre qué APK tiene el teléfono.
  - 2 pruebas nuevas en `AuthViewModelTest`; 6 pruebas existentes ahora pasan por el login antes de pedir el código (el caso real).
- **Estado:** ⏳ Código listo; falta compilar e instalar la app nueva en el teléfono y probar fuera de casa.

---

## BUG-022 — Alumno del SIIA sin correo: se quedaba en el login sin poder pedir el código

- **Fecha:** 28/09/2026
- **Síntoma:** (encontrado al revisar el código, sin reporte previo) si el SIIA no trae correo y la cuenta registrada en AppTESCHI no tiene ninguno guardado, el alumno no podía completar el inicio de sesión.
- **Causa (tres a la vez):** (1) la respuesta `requiereCorreo` de `/api/auth/siia` no incluía el ticket de login, que `/api/otp/enviar` exige desde DEC-030; (2) la app marcaba `siiaRequiereCorreo` pero la pantalla no lo usaba, así que se quedaba en el formulario sin mensaje; (3) el correo que escribía el alumno solo se usaba para ese envío y nunca se guardaba, así que lo habría pedido en cada login aunque [[2FA_Login]] decía "una sola vez".
- **Solución:** el backend responde siempre con ticket y `correoEnmascarado` vacío cuando no hay correo; la app ya sabía manejar eso (pasa a la verificación y pide el correo). `/api/otp/verificar` guarda ese correo como `CorreoOtp` al verificar el código, solo si la cuenta no tenía ninguno.
- **Estado:** ✅ Probado en local con un SIIA simulado (v0.29); falta desplegar.

---

## BUG-023 — La fecha de compilación del login no cambiaba entre builds

- **Fecha:** 28/09/2026
- **Síntoma:** un APK compilado a las 12:41 mostraba "compilada 28/09/2026 11:40". Justo lo contrario de lo que buscaba BUG-021 (saber qué APK tiene el teléfono).
- **Causa:** `FECHA_COMPILACION` se calculaba con `LocalDateTime.now()` dentro de `defaultConfig`, que corre en la fase de configuración de Gradle. Con `org.gradle.configuration-cache=true`, si solo cambió código Kotlin se reutiliza la configuración guardada y la fecha queda congelada en la de ese momento.
- **Solución:** la fecha sale de un `ValueSource` (`FechaCompilacion` en `app/build.gradle.kts`) conectado con `androidComponents.onVariants { buildConfigFields }`: Gradle lo recalcula en cada build aunque reutilice la caché.
- **Verificado:** dos builds con un minuto de diferencia y la caché reutilizada ("Reusing configuration cache") dan 12:42 y 12:43; el login del teléfono muestra "compilada 28/09/2026 12:43".
- **Estado:** ✅ Resuelto

---

## BUG-024 — Calificaciones: "No fue posible procesar la solicitud."

- **Fecha:** 30/09/2026
- **Síntoma:** con el backend de DEC-042 desplegado, el alumno entra con usuario + NIP del SIIA (el login funciona) y al abrir Calificaciones ve "No fue posible procesar la solicitud." con el botón Reintentar.
- **Qué se sabe:** ese texto no existe en AppTESCHI: es el `mensaje` que devolvió `calificaciones/parciales.ashx` del SIIA, reenviado tal cual por `/api/mi-calificaciones-siia`. No es un problema de token: con un token inválido el SIIA responde 401 "Token requerido o inválido…" (comprobado), y ese caso la app lo trata como sesión vencida. O sea, el SIIA aceptó el token y falló al procesar la consulta.
- **Hipótesis principal:** el backend (Node) llamaba al SIIA **sin `User-Agent`**, a diferencia de Postman o un navegador; hay servidores ASP.NET que fallan con un error genérico cuando falta. Otras posibles: error del SIIA al calcular el periodo actual o las verificaciones de trámites de esa cuenta (del lado de la escuela).
- **Cambios:** todas las llamadas al SIIA mandan `User-Agent: AppTESCHI-API/1.1`; el error ahora dice que fue el SIIA ("El SIIA no pudo entregar tus calificaciones (el SIIA respondió: …)") y el log guarda el status y la respuesta del SIIA (`[CALIF-SIIA][ERROR-SIIA] … status=… respuesta=…`); nueva petición "5. Calificaciones parciales" en Postman (y `nip` en el login directo) para probar el SIIA sin pasar por AppTESCHI. Prueba local 18/18.
- **Resultado del primer cambio (30/09/2026, 10:00):** desplegado; el mensaje ya dice "El SIIA no pudo entregar tus calificaciones (el SIIA respondió: …)", así que el `User-Agent` **no** era la causa. El SIIA acepta el token y falla al procesar.
- **Segundo cambio — reintento por periodo:** hipótesis nueva: el SIIA falla al calcular "el periodo actual" cuando no se le manda `periodo` (p. ej. un alumno de noveno que en el periodo en curso solo tiene Residencias). Si la consulta sin periodo da un error genérico (5xx, 400 o 200 con `ok: false`), el backend prueba con los tres periodos más recientes (`periodosRecientes()`: hoy 2026-2, 2026-1, 2025-2; supuesto: "-1" = febrero–julio, "-2" = agosto–enero) y muestra el primero que responda, con el aviso "El SIIA no pudo mostrar el periodo actual; se muestra el periodo …". Cada intento queda en el log (`[CALIF-SIIA][REINTENTO]`). Un periodo elegido a mano no se reintenta. En la app: el aviso se muestra arriba de la lista, y si falla un periodo elegido se conservan las parciales que se veían con un aviso de error (antes la pantalla se quedaba en error). Pruebas: backend 23/23, app 135/135.
- **Confirmado directo contra el SIIA (30/09/2026, sin pasar por AppTESCHI):** en Postman, `GET {{siia_base_url}}/calificaciones/parciales.ashx` con un token válido (login + NIP correctos) da **500** `{"ok": false, "mensaje": "No fue posible procesar la solicitud."}` en los 5 casos probados: sin `periodo`, `2026-2`, `2026-1`, `2025-1` y `2025-2`. Mismo resultado con dos cuentas de alumno distintas. No es cuestión de periodo — el reintento de `server.js` no va a encontrar ninguno que funcione. Encabezados de la respuesta: `Server: Microsoft-IIS/8.0`, `X-Powered-By: ASP.NET`, `X-Aspnet-Version: 4.0.30319`.
- **Estado:** ✅ Resuelto por la escuela (05/10/2026). El alumno lo reportó y los administradores del SIIA lo corrigieron; en Postman `calificaciones/parciales.ashx?periodo=2025-2` ya responde **200** con `periodosDisponibles` de 2022-2 a 2026-2, `alumno`, `verificaciones` y las materias. Confirma además el supuesto de periodos de `periodosRecientes()` (el actual es 2026-2).
- **Ajuste por la respuesta real (v0.34):** la respuesta de calificaciones empieza con `"bloqueado"` y el `"ok": true` no aparece al inicio como en login/NIP (puede faltar). `server.js` ahora toma un 200 como éxito salvo que diga `ok: false`, y trata `bloqueado: true` igual venga con 403 o con 200. El reintento por periodo se queda como respaldo.
