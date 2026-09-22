# Plan de Pruebas — AppTESCHI

> Estrategia, alcance, tipos de prueba y herramientas usadas para verificar el sistema.
> **Relacionado con:** [[02_Catalogo_Casos_de_Prueba]], [[03_Matriz_Trazabilidad]]

---

## 1. Objetivo

Garantizar que cada requerimiento funcional implementado se comporta como se espera, que las reglas de negocio críticas (semestre solo hacia adelante, borrado en cascada, validación de rangos) no se puedan saltar, y que ningún cambio de código rompa lo que ya funcionaba.

## 2. Alcance

| Dentro del alcance | Fuera del alcance (por ahora) |
|---|---|
| Lógica de `ViewModel` (Android) | Pruebas de UI automatizadas (Compose UI testing / Espresso) |
| Repositorios/servicios de red con *fakes* | Pruebas end-to-end automatizadas contra el backend real |
| Endpoints del backend (verificación manual con datos reales) | Pruebas de carga / rendimiento |
| Reglas de negocio del backend (validaciones, transacciones) | Pruebas de penetración / seguridad ofensiva |

## 3. Niveles y tipos de prueba

### 3.1 Pruebas unitarias (Android)

- **Herramienta:** JUnit4 + `kotlinx-coroutines-test` (`StandardTestDispatcher`, `Dispatchers.setMain/resetMain`).
- **Qué se prueba:** `ViewModel`s con dependencias inyectadas por constructor, sustituidas por *fakes* (nunca red real).
- **Cómo correrlas:** `./gradlew.bat :app:testDebugUnitTest`.
- **Cobertura actual:** 60 pruebas en 11 archivos (ver §5).

### 3.2 Pruebas manuales de backend (integración contra base de datos real)

- **Herramienta:** `curl` (línea de comandos) contra el backend local (`http://localhost:4000`) apuntando a la base de datos real de desarrollo.
- **Qué se prueba:** cada endpoint nuevo o modificado, cubriendo caso válido, casos de validación (400), caso "no existe" (404), duplicados (409) y autenticación (401).
- **Por qué no son automatizadas todavía:** el backend no tiene una suite de pruebas de integración (ej. Jest + una base de datos de prueba aislada) — es la brecha más importante identificada en este plan (ver §6).
- **Registro:** cada tanda de pruebas manuales queda documentada en [[CAMBIOS_DE_CODIGO]] junto con la versión que la introdujo.

### 3.3 Verificación de compilación

- `./gradlew.bat :app:compileDebugKotlin` debe terminar sin errores antes de cualquier entrega.
- `./gradlew.bat :app:assembleDebug` debe generar el APK sin advertencias nuevas relevantes.

### 3.4 Verificación end-to-end manual (humano)

- Instalar el APK generado, con el túnel de desarrollo activo, y ejercitar el flujo completo en el dispositivo real antes de considerar una entrega terminada (ver checklist en [[03_Manual_Tecnico_Instalacion]] §6).

## 4. Ambientes

| Ambiente | Base de datos | Backend | Notas |
|---|---|---|---|
| Desarrollo | `AppTeschiDB` en SQL Server Express local | `node server.js` en `localhost:4000`, expuesto por túnel Cloudflare | Único ambiente que existe actualmente |
| Producción | No existe todavía | No existe todavía | Ver [[05_Diagrama_Despliegue]] §"Camino a producción" |

> No existe un ambiente de *staging* separado — las pruebas manuales de backend se ejecutan contra la misma base de datos de desarrollo, usando matrículas de prueba con prefijos reconocibles (`TESTCRUD*`, `TESTSEM*`, `2024PRUEBA*`) que se eliminan o se dejan como datos de ejemplo intencionalmente.

## 5. Inventario de pruebas unitarias

| Archivo | Qué cubre | # pruebas |
|---|---|---|
| `AuthViewModelTest` | Cascada de login, envío/verificación de OTP, bloqueo por intentos | 13 |
| `RegistroViewModelTest` | Validación de formulario de registro | 7 |
| `ReinscripcionViewModelTest` | Estados de reinscripción (regular/irregular/bloqueado), envío | 5 |
| `TiraMateriasViewModelTest` | Filtrado de materias por semestre actual | 5 |
| `CalificacionesViewModelTest` | Cálculo de promedio, filtrado por semestre | 4 |
| `KardexViewModelTest` | Cálculo de promedio global, porcentaje cubierto, conteos | 5 |
| `AuthRepositoryTest` | Cascada de autenticación con *fakes* de las 3 fuentes | 10 |
| `GruposIscTest` | Derivación de turno/semestre desde la clave del grupo (mock local) | 3 |
| `PlanDeEstudiosIscTest` | Integridad del catálogo local mock de ISC | 4 |
| `KardexPdfFormatoTest` | Formato de datos antes de generar el PDF | 1 |
| `ExampleUnitTest` | Prueba de plantilla de Android Studio | 1 |
| **Total** | | **60** (0 fallos) |

## 6. Brechas conocidas

1. **Sin pruebas automatizadas de backend** — toda la verificación de `server.js` es manual con `curl`. Recomendado: suite con Jest + `supertest` contra una base de datos de prueba desechable.
2. **Sin pruebas de UI automatizadas** — no hay Compose UI Testing; toda verificación visual es manual sobre el APK instalado.
3. **Sin pruebas de los módulos mock del alumno** más allá de la lógica de sus `ViewModel`s — no aplica todavía una prueba de "flujo real" porque no hay backend real detrás de Reinscripción/Tira/Calificaciones/Kardex del lado del alumno.
4. **Sin pruebas de concurrencia** — no se ha verificado el comportamiento ante dos administradores editando al mismo alumno simultáneamente.
