# Catálogo de Casos de Prueba — AppTESCHI

> Casos de prueba referenciados desde [[03_Matriz_Trazabilidad]]. Los CP-10 en adelante corresponden a verificaciones manuales con `curl` contra la base de datos real, ejecutadas durante la construcción del panel de administrador (DEC-017/018).
> **Relacionado con:** [[01_Plan_de_Pruebas]], [[03_Matriz_Trazabilidad]]

## Autenticación y registro

| ID | Descripción | Resultado esperado | Estado |
|---|---|---|---|
| CP-01 | Iniciar sesión con credenciales de cuenta propia válidas | Pide correo de contacto y avanza a verificación OTP | ✅ Cubierto por `AuthRepositoryTest`/`AuthViewModelTest` |
| CP-02 | Verificar OTP correcto / incorrecto / expirado | Correcto → sesión activa; incorrecto → cuenta intento fallido; expirado (>10 min) → exige reenvío | ✅ Cubierto por `AuthViewModelTest` |
| CP-03 | Registrar cuenta con matrícula ya sincronizada desde el SIIA (sin contraseña) | Completa el perfil existente, no crea uno duplicado | ✅ Verificado en la implementación inicial de `POST /api/registro` |
| CP-04 | Cargar dashboard tras login exitoso | Muestra tarjetas de todos los módulos del alumno | ✅ Manual (UI) |

## CRUD de alumnos (verificado con `curl`, base de datos real)

| ID | Descripción | Pasos | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| CP-10 | Alta de alumno válida | `POST /api/usuarios` con matrícula, nombre, carrera ISC | `201`, `{ok:true}` | ✅ Igual al esperado |
| CP-11 | Alta con matrícula duplicada | Repetir CP-10 con la misma matrícula | `409` | ✅ Igual al esperado |
| CP-12 | Alta con carrera inexistente | `claveCarrera: "NOEXISTE"` | `400` | ✅ Igual al esperado |
| CP-13 | Consultar directorio completo | `GET /api/usuarios` | `200`, incluye `ClaveCarrera` y `Semestre` | ✅ Igual al esperado |
| CP-14 | Editar perfil (nombre + semestre) | `PUT /api/usuarios/:matricula` con campos parciales | `200`, solo cambian los campos enviados | ✅ Igual al esperado |
| CP-15 | Editar sin campos / matrícula inexistente | `PUT` con body `{}` / matrícula que no existe | `400` / `404` respectivamente | ✅ Igual al esperado |
| CP-16 | Eliminar alumno existente | `DELETE /api/usuarios/:matricula` | `200`; desaparece de `GET /api/usuarios`; su `HistorialAcademico` queda en 0 filas | ✅ Igual al esperado |
| CP-17 | Eliminar alumno inexistente | `DELETE` con matrícula que no existe | `404` | ✅ Igual al esperado |
| CP-18 | Dar de baja / reactivar | `PUT /api/usuarios/:matricula` `{activo: false}` luego `{activo: true}` | `200` en ambos; el alumno cambia de estado sin perder datos | ✅ Igual al esperado |
| CP-19 | Consultar perfil completo | `GET /api/usuarios` + `GET /api/auditoria?matricula=` + `GET /api/calificaciones/:matricula` | Los tres responden `200` con datos consistentes del mismo alumno | ✅ Igual al esperado |

## Regla de semestre (DEC-018)

| ID | Descripción | Pasos | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| CP-20 | Retroceder de semestre | Alumno en semestre 4, `PUT {semestre: 2}` | `400` "El semestre no puede retroceder (actual: 4)" | ✅ Igual al esperado |
| CP-21 | Repetir el semestre actual | Alumno en semestre 4, `PUT {semestre: 4}` | `400` (mismo mensaje — no se permite igualar) | ✅ Igual al esperado |
| CP-22 | Avanzar de semestre | Alumno en semestre 4, `PUT {semestre: 6}` | `200` | ✅ Igual al esperado |

## Calificaciones

| ID | Descripción | Pasos | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| CP-23 | Consultar calificaciones de un alumno | `GET /api/calificaciones/:matricula` | `200`, lista completa de materias de su carrera con calificación si existe | ✅ Igual al esperado (53 materias para un alumno de ISC) |
| CP-24 | Capturar calificación válida | `PUT /api/calificaciones/:matricula/:idMateria {calificacion: 8.5}` | `200`; estatus derivado automáticamente a "Aprobada" (`AP`) | ✅ Igual al esperado |
| CP-25 | Capturar calificación fuera de rango | `{calificacion: 15}` | `400` | ✅ Igual al esperado |

## Auditoría

| ID | Descripción | Pasos | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| CP-26 | Cada operación administrativa genera auditoría | Ejecutar CP-10, CP-14, CP-18, CP-24 y revisar `AuditoriaMovimientos` | Un renglón nuevo por cada operación, con `Accion`/`Detalle` legible | ✅ Igual al esperado |
| CP-27 | Filtrar auditoría por matrícula | `GET /api/auditoria?matricula=X` | Solo movimientos donde `X` es actor o perfil afectado (`PerfilMatricula`) | ✅ Igual al esperado — incluye el rastro de un alumno ya eliminado |

## Estadísticas y catálogo

| ID | Descripción | Pasos | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| CP-28 | Consultar estadísticas agregadas | `GET /api/estadisticas` | `200` con `porCarrera`, `porSemestre`, `estado`, `altasPorMes`, `calificaciones` | ✅ Igual al esperado |
| CP-29 | Consultar catálogo de carreras | `GET /api/catalogos/registro` | `200`, 9 carreras (7 activas + 2 modalidad a distancia) | ✅ Igual al esperado |
| CP-30 | Consultar plan de estudios de una carrera | `GET /api/plan-estudios/ISC` | `200`, 53 materias | ✅ Igual al esperado |
| CP-31 | Consultar grupos reales de una carrera | `GET /api/grupos/ISC` | `200`, grupos con clave/semestre/turno/número/periodo | ✅ Igual al esperado |

---

## Notas de ejecución

- Todas las pruebas CP-10 a CP-31 se ejecutaron contra la base de datos de desarrollo real (`AppTeschiDB`), nunca contra datos simulados en memoria — es intencional: valida el contrato real entre backend y SQL Server, no solo la lógica de JavaScript.
- Los alumnos de prueba usados (`TESTCRUD001`, `TESTSEM001`, etc.) se eliminaron al terminar cada tanda, salvo los prefijados `2024PRUEBA*`, dejados deliberadamente como datos de ejemplo para exploración manual del panel (ver [[CAMBIOS_DE_CODIGO]] v0.19).
