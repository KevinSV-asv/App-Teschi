# AppTESCHI

App Android para los alumnos del **Tecnológico de Estudios Superiores de Chimalhuacán (TESCHI)**: reúne en un solo lugar su información escolar, con la misma cuenta del SIIA.

La app se conecta directamente a la API oficial del SIIA por HTTPS. No necesita servidor propio ni base de datos.

**Versión actual:** 2.4 (noviembre de 2026). Desarrollada por un equipo de dos integrantes.

## Módulos

| Módulo | Qué hace | Estado |
|---|---|---|
| Inicio de sesión | Contraseña y NIP de 4 dígitos del SIIA; crear y recuperar el NIP. Solo para alumnos | ✅ |
| Inicio | Datos del alumno y resumen del periodo actual | ✅ |
| Tira de materias | Materias, docentes y grupo del periodo | ✅ |
| Calificaciones | Parciales y finales (1ra y 2da vuelta) por periodo | ✅ |
| Kardex | Historial completo por semestre, créditos, promedios y descarga del PDF oficial | ✅ |
| Mi perfil | Foto de perfil, datos del alumno y cambio de NIP | ✅ |
| Cambio de contraseña | El formulario está listo, pero la API del SIIA aún no lo permite: lleva al portal | ⏳ |
| Reinscripción e Intersemestral | Pantallas en construcción: la API del SIIA aún no tiene estos datos | ⏳ |

La foto de perfil se guarda solo en el teléfono, porque la API no permite subirla.

## Tecnología

- **Lenguaje e interfaz:** Kotlin 2.0, Jetpack Compose y Material 3.
- **Arquitectura:** MVVM, con un servicio y una interfaz por cada fuente de datos.
- **Red:** OkHttp contra la API del SIIA, solo por HTTPS.
- **Android:** mínimo 7.0 (API 24), objetivo API 35.
- **Pruebas:** JUnit, coroutines-test y MockWebServer.

## Compilar y probar

Abre el proyecto en Android Studio, o desde la terminal:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. No hace falta configurar llaves ni servidores. Para entrar a la app se necesita una cuenta de alumno del SIIA.

La calidad se verifica con 155 pruebas automáticas y en emulador contra un SIIA simulado con datos ficticios.

## Estructura

| Carpeta | Contenido |
|---|---|
| `app/` | Código de la app y sus pruebas |
| `Cerebro_AppEscolar/` | Documentación del proyecto: empieza por [`00_SISTEMA/MAPA_PROYECTO.md`](Cerebro_AppEscolar/00_SISTEMA/MAPA_PROYECTO.md) |
| `Documentacion de Residencias/` | Documentos de la residencia profesional |

Para probar la API con Postman, sigue [`01_POSTMAN_API.md`](Cerebro_AppEscolar/01_APIS_POSTMAN/01_POSTMAN_API.md). Las decisiones técnicas están en [`DECISIONES_TECNICAS.md`](Cerebro_AppEscolar/06_BITACORA_TECNICA/DECISIONES_TECNICAS.md).

> **Sobre la documentación:** el 12/10/2026 la app pasó a conectarse directo al SIIA (DEC-043). Las notas de requerimientos, diseño, manuales y pruebas, y los Word de `Documentacion_Word`, describen la versión anterior, con servidor propio. El backend `AppTeschi.Api` y la base de datos se conservan solo como histórico.

## Datos personales

Este repositorio es público y no debe contener datos reales de alumnos. Las pruebas y ejemplos usan datos ficticios.
