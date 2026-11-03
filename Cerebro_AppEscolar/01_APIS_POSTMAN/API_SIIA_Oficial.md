# API oficial del SIIA (TESCHI)

- **Estado:** ✅ En uso **directo desde la app** desde el 12/10/2026 (DEC-043): `POST /login.ashx`, `POST /nip.ashx` (verificar, configurar, recuperar, cambiar), `GET /calificaciones/parciales.ashx` y, desde el 02/11/2026 (DEC-044), `finales.ashx`, `kardex.ashx` y `kardex-pdf.ashx`. Antes se llamaban desde el backend (DEC-035, DEC-041, DEC-042). Versión de la API: **1.2.0**.
- **Relacionado con:** [[2FA_Login]], [[09_Login_SIIA_por_Tipo]], [[01_POSTMAN_API]], [[DECISIONES_TECNICAS]]

---

## Qué es

El SIIA expone una API REST oficial (antes solo existía el portal ASP.NET, que se scrapeaba desde el teléfono; eso se retiró en DEC-026 y ya no existe en el código).

| Dato | Valor |
|---|---|
| URL base | `https://siia.teschi.edu.mx/Teschi/api` |
| Documentación | `…/api/swagger/index.html` (Swagger UI) · `…/api/swagger/openapi.json` (especificación) |
| Versión | 1.2.0 (especificación del 29/10/2026, revisada el 02/11/2026). La 1.1 no tenía finales ni kardex; la 1.0 solo tenía login y asistencia |
| Autenticación | `Bearer <token>` final (480 min), que solo se obtiene **después del NIP** |
| Acceso | Público por HTTPS: el teléfono lo alcanza con cualquier conexión a internet, sin VPN ni servidor propio |

## Endpoints (v1.2.0)

| Endpoint | Grupo | Requiere token | Usado por AppTESCHI |
|---|---|---|---|
| `POST /login.ashx` | Autenticación | No | ✅ Paso 1 del login (usuario + contraseña); también autoriza el cambio de NIP en Perfil (DEC-047) |
| `POST /login-nip.ashx` | Autenticación | No | ❌ Login solo con `matricula` + `nip`, sin contraseña. Descartado: el alumno eligió contraseña + NIP (12/10/2026) |
| `GET /nip.ashx` | NIP | No (`usuario` o `tempToken`) | ❌ No hace falta: el paso 1 del login ya dice `tieneNip` |
| `POST /nip.ashx` | NIP | Solo `cambiar` | ✅ `verificar`, `configurar` y `recuperar` en el login; `cambiar` en Perfil |
| `GET /calificaciones/parciales.ashx` | Calificaciones | Sí | ✅ Calificaciones > Parciales, Tira de materias y el resumen de Inicio |
| `GET /calificaciones/finales.ashx` | Calificaciones | Sí | ✅ Calificaciones > Finales (DEC-044) |
| `GET /calificaciones/kardex.ashx` | Calificaciones | Sí | ✅ Kardex (DEC-044) |
| `GET /calificaciones/kardex-pdf.ashx` | Calificaciones | Sí | ✅ "Descargar PDF" del Kardex, con `base64=1` (DEC-044) |
| `GET /kardex.ashx`, `GET /kardex-pdf.ashx` | Calificaciones | Sí | ❌ Alias de los dos anteriores |
| `GET /asistencia/grupos.ashx` | Asistencia | Sí (docente) | ❌ Fuera de alcance (DEC-035) |
| `GET /asistencia/alumnos.ashx` | Asistencia | Sí (docente) | ❌ Fuera de alcance |
| `POST /asistencia/guardar.ashx` | Asistencia | Sí (docente) | ⛔ No ejecutar: escribe datos reales |

## Login en dos pasos con NIP (cambio de la 1.1)

`POST /login.ashx` — cuerpo `{ "usuario", "password", "nip"? }`

- **Con `nip`** (4 dígitos): valida todo y devuelve el `token` final de 480 min en un paso.
- **Sin `nip`**: valida usuario y contraseña y devuelve `requiereNip`, `tieneNip` y un `tempToken` de 10 min para terminar en `/nip.ashx`.

Respuesta (`LoginResponse`): `ok`, `mensaje`, `requiereNip`, `tieneNip`, `tempToken`, `token`, `expiraEnMinutos`, `usuario` (ver abajo).

> ⚠️ **Sin confirmar:** la especificación no dice si el paso 1 (sin NIP) trae el bloque `usuario` lleno. La app usa `usuarioInfo` del paso NIP y, si no viene, el `usuario` del paso 1; sin ninguno de los dos no entra.

Error de credenciales comprobado el 29/09/2026: `401` con `{ ok: false, mensaje: "Usuario o password incorrectos.", usuario: null, … }`.

### `usuario` (esquema `Usuario`)

`usuario`, `numUsuario`, `nombre`, `paterno`, `materno`, `nombreCompleto`, `correo`, `tipo`, `idArea`, `area`, `idCarrera`, `tipoCarrera`, `permisos` (texto), `cambioPw` (número), `puedeAsistencia` (booleano).

### Valores de `tipo`

| `tipo` | Significado | Fuente | Qué hace AppTESCHI |
|---|---|---|---|
| `E` | Alumno | ✅ Login real (25/09/2026) y Swagger 1.1 | Entra |
| `P` | Docente | ✅ Swagger 1.1 | "Por ahora AppTESCHI es solo para alumnos…" |
| `A` | Administrativo | ✅ Swagger 1.1 | "Por ahora AppTESCHI es solo para alumnos…" |

La regla vive en `CuentaSiia.esAlumno` (app).

## NIP (`/nip.ashx`, nuevo en 1.1)

Segundo factor propio del SIIA: 4 dígitos numéricos.

| Acción | Cómo | Resultado |
|---|---|---|
| Consultar estado | `GET ?usuario=` o `?tempToken=` | `tieneNip` |
| `verificar` | `POST { accion, tempToken, nip }` | Token final + `usuarioInfo` |
| `configurar` (primera vez) | `POST { accion, tempToken, nuevoNip, confirmarNip }` | Token final. 409 si ya tenía NIP |
| `cambiar` | `POST { accion, nipActual, nuevoNip, confirmarNip }` + Bearer | NIP actualizado |
| `recuperar` | `POST { accion, curp }` | El SIIA manda el NIP al correo institucional; responde el correo enmascarado |

Errores: 401 (NIP o token incorrecto), 404 (usuario o CURP), 409, 422 (NIPs no coinciden, NIP no configurado), 429.

## Calificaciones parciales (`GET /calificaciones/parciales.ashx`, nuevo en 1.1)

Primer endpoint con datos académicos del alumno.

- **Requiere el token final** (después del NIP). Un alumno solo ve su propia matrícula (la toma del token). Personal docente o administrativo puede pasar `matricula`.
- `periodo` opcional (`2026-1`); sin él usa el periodo actual.
- **Respuesta:** `periodo`, `periodosDisponibles` (periodos que cursó), `alumno` (`matricula`, `nombre`, `carrera`, `idCarrera`), `resumen` (total, evaluadas, aprobadas, reprobadas, promedio parcial) y `calificaciones[]` con `materia`, `profesor`, `grupo`, `cal1`/`cal2`/`cal3`, `promedioParcial` y `estatus` (`Aprobatoria` / `Reprobatoria` / `Sin calificar`).
- **403 `bloqueado`:** el alumno tiene trámites de inicio de ciclo pendientes; `verificaciones` dice cuáles (`cambioPassword`, `datosPersonales`, `seguroSocial`, `encuestaServicio`, `pendientes[]`).
- Son **parciales** (1, 2 y 3) por periodo; desde la 1.2 también trae los datos de finales (ver abajo).
- En AppTESCHI: el módulo Calificaciones las muestra con selector de periodo; el aviso de bloqueo lista los trámites pendientes ([[03_Calificaciones]]).
- **Respuesta real (05/10/2026):** empieza con `"bloqueado"`, luego `periodo`, `periodosDisponibles` (de más reciente a más antiguo, p. ej. `2026-2` … `2022-2`), `alumno`, `verificaciones`… El `"ok": true` no aparece al inicio como en login/NIP, así que no hay que exigirlo. Del 30/09 al 05/10/2026 este endpoint respondía 500 para todos (BUG-024); lo corrigió la escuela.

## Calificaciones finales (`GET /calificaciones/finales.ashx`, nuevo en 1.2)

- `periodo` opcional; sin él usa el actual. Un alumno solo ve lo suyo: `matricula` es solo para docentes y administrativos, y la app no la manda.
- **Respuesta:** además de `periodo`, `periodosDisponibles`, `alumno`, `verificaciones` y `bloqueado`:
  - `finales[]`: `materia`, `profesor`, `grupo`, `primeraVuelta` (= `ordinario` = `calPromedio`), `segundaVuelta` (= `extraordinario` = `segVuelta`), `calFinal` (= `final`), cada uno con su `…Str`, y `estatus` / `estatusFinal` (`Aprobatoria`, `Reprobatoria`, `Sin calificar`).
  - `resumenFinal`: `totalMaterias`, `materiasEvaluadas`, `materiasAprobadas`, `materiasReprobadas`, `promedio` / `promedioFinal` (con `…Str`) y `estatus` (`Aprobatorio`, `Regularizar`, `En Curso`).
- `parciales.ashx` trae los mismos datos en `calificaciones[]` y `resumen`; la app usa el endpoint propio y acepta ambos.
- **403 `bloqueado`** igual que en parciales; 404 si no existe el alumno.

## Kardex (`GET /calificaciones/kardex.ashx`, nuevo en 1.2)

Historial completo (`sp_HisAcaDatos` + `sp_HisAca`). Con `formato=pdf` o `excel` descarga el reporte; la app usa el default, `json`.

- `datosPersonales` (= `resumen`): `matricula`, `alumno`, `curp`, `carrera`, `especialidad`, `planEstudios`, `situacion`, `grupo`, `periodoIngreso`, `ultimoPeriodo`, `creditosCarrera`, `creditosCursados`, `creditosPorCursar`, `porcentajeStr`, `materiasAprobadas`, `materiasReprobadas`, `materiasPorCursar`, `promedioGlobal`, `promedioLetra`, entre otros. La app muestra los mismos datos que el reporte oficial, incluida la CURP (DEC-045); no usa `fotoUrl`. La fecha de elaboración no viene en la respuesta: se usa el día de la consulta.
- `kardex[]` (= `materias[]`) y `semestres[]` (`semestre`, `nombreSemestre`, `promedioSemestre`, `materias[]`). Por materia: `clave`, `nombre`, `creditos`, `semestre`, oportunidades (`calOrd`/`periodoOrd`, `calRec`/`periodoRec`, `calEspecial`/`periodoEsp`, `calGlobal`/`periodoGlo`), `calificacion` definitiva y `cursada` (`AP` aprobada, `NA` no aprobó, `PC` por cursar).
- `calOrd` es texto: puede ser `ACA` (actividades complementarias). `periodoOrd` puede traer un sufijo, p. ej. `EQ`.
- `simbologia`: `AP`, `PC`, `NA`, `AAA`, `ACA`.
- **403 `bloqueado`** con `verificaciones.pendientes`; 404 si no existe el alumno.

## Kardex en PDF (`GET /calificaciones/kardex-pdf.ashx`, nuevo en 1.2)

Documento oficial generado por SQL Server Reporting Services.

| Parámetro | Efecto | En la app |
|---|---|---|
| `base64=1` | JSON con `fileName`, `mimeType`, `sizeBytes` y `base64` | ✅ El que se usa |
| (ninguno) | Archivo PDF como descarga | Solo en Postman |
| `inline=1` | Archivo PDF para mostrarse en el navegador | ❌ |
| `formato=EXCEL` | `.xlsx` | ❌ |
| `token=` | Token en la URL | ⛔ **No usar:** quedaría en registros e historiales |

## Sin confirmar con una respuesta real

Todo lo de finales, kardex y PDF sale de la especificación 1.2: los tres endpoints existen (401 sin token, 02/11/2026), pero falta verlos con una cuenta. Por experiencia (BUG-024, `ok` ausente) la respuesta real puede diferir; la colección [[01_POSTMAN_API]] lo comprueba sin mostrar datos personales en sus resultados.

## Qué no da el SIIA (y por eso no está en la app)

Horario, semestre actual, reinscripción, intersemestrales, **cambio de contraseña** y **subir la foto de perfil** (la de la app queda solo en el teléfono, DEC-048). Reinscripción e Intersemestral muestran "Estamos trabajando en este apartado" (DEC-046); la contraseña se cambia en el portal (DEC-047); el horario se quitó (DEC-043). La equivalencia de carreras (`idCarrera` ↔ `CatalogoCarreras`, DEC-037) era de `AppTeschiDB` y ya no se usa: la app muestra la carrera tal como la da el SIIA (`area`).
