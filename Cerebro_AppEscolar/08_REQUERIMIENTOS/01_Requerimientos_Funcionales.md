# Requerimientos Funcionales — AppTESCHI

> ⚠️ **Desactualizado (12/10/2026, DEC-043):** describe el sistema con backend propio, base de datos y panel de administrador. Hoy la app solo tiene las pantallas del alumno con datos del SIIA (ver [[REGLAS_PROYECTO]] y [[MAPA_PROYECTO]]). Actualizar antes de entregarlo.

> Cada requerimiento tiene un identificador único (`RF-###`) usado por [[03_Matriz_Trazabilidad]] y por [[01_Casos_de_Uso]].
> **Estado:** ✅ Implementado y real · ⚠️ Implementado con datos simulados (mock) · 🔴 Pendiente
> **Relacionado con:** [[00_Vision_y_Alcance]], [[02_Requerimientos_No_Funcionales]]

---

## Autenticación y sesión (actor: Alumno / Administrador)

| ID | Descripción | Prioridad | Estado |
|---|---|---|---|
| RF-001 | El sistema debe permitir iniciar sesión con matrícula y contraseña de una cuenta propia (`AlumnoCredenciales`) o de administrador (`AdministradorCredenciales`) | Alta | ✅ |
| RF-009 | El sistema debe permitir iniciar sesión con usuario y contraseña del SIIA, validados por el backend contra la API oficial del SIIA, a cualquier alumno válido en el SIIA; si no está dado de alta en AppTESCHI entra solo con los datos del SIIA y **nunca** se crea un registro automáticamente (DEC-040) | Alta | ✅ (alumnos registrados, 25/09/2026); sin registro ⏳ sin desplegar; docentes y administrativos pendientes |
| RF-002 | El sistema debe exigir verificación en dos pasos (OTP de 6 dígitos generado en el servidor y enviado al correo guardado de la cuenta) antes de conceder acceso a una cuenta propia o validada por el SIIA | Alta | ✅ |
| RF-003 | El código OTP debe expirar 10 minutos después de generado y debe poder reenviarse | Alta | ✅ |
| RF-004 | El sistema debe bloquear el inicio de sesión tras superar un número máximo de intentos fallidos de OTP configurable | Media | ✅ (umbral configurable, sin bloqueo forzado por defecto) |
| RF-005 | El sistema debe distinguir el rol (Alumno / Administrador) y navegar al dashboard correspondiente tras el login | Alta | ✅ |
| RF-006 | El alumno debe poder registrar una cuenta propia (matrícula, nombre, fecha de nacimiento, sistema, carrera, contraseña) | Alta | ✅ |
| RF-007 | Si la matrícula ya existe como perfil creado por el administrador (sin contraseña), el registro debe completarlo en vez de crear un perfil duplicado | Alta | ✅ |
| RF-008 | El sistema debe ofrecer un flujo de recuperación de contraseña para cuentas propias | Media | ✅ (DEC-025) |

## Dashboard y módulos del alumno

| ID | Descripción | Prioridad | Estado |
|---|---|---|---|
| RF-010 | El alumno debe ver un dashboard con acceso a sus módulos escolares | Alta | ✅ |
| RF-011 | El alumno debe poder consultar y enviar su reinscripción (periodo, grupo, comprobante) | Alta | ✅ real (DEC-024) |
| RF-012 | El alumno debe poder consultar su tira de materias del semestre actual | Alta | ✅ real — `/api/mi-historial` (DEC-023) |
| RF-013 | El alumno debe poder consultar sus calificaciones del semestre actual con su promedio | Alta | ✅ real (DEC-023) |
| RF-014 | El alumno debe poder consultar su kardex completo (materias, créditos, promedio global, porcentaje cubierto) | Alta | ✅ real (DEC-023) |
| RF-015 | El alumno debe poder generar y descargar su kardex en PDF | Media | ✅ |
| RF-016 | El alumno debe poder consultar el periodo intersemestral disponible | Baja | 🔴 (placeholder) |

## Gestión de alumnos (actor: Administrador)

| ID | Descripción | Prioridad | Estado |
|---|---|---|---|
| RF-020 | El administrador debe poder dar de alta un alumno nuevo (matrícula, nombre completo, carrera, correo, semestre opcional), sin necesidad de contraseña | Alta | ✅ |
| RF-021 | El sistema debe rechazar el alta si la matrícula ya existe (409) o si la carrera no existe en el catálogo (400) | Alta | ✅ |
| RF-022 | El administrador debe poder consultar el directorio completo de alumnos, con búsqueda por matrícula, nombre, correo o carrera | Alta | ✅ |
| RF-023 | El administrador debe poder editar el perfil de un alumno (nombre, correos, carrera) de forma parcial — solo los campos que decide cambiar | Alta | ✅ |
| RF-024 | El administrador debe poder eliminar permanentemente a un alumno, incluyendo su historial académico, credenciales, OTP y sesiones asociadas | Alta | ✅ (con diálogo de confirmación) |
| RF-025 | El administrador debe poder dar de baja (desactivar) o reactivar a un alumno sin borrar su información | Alta | ✅ |
| RF-026 | El administrador debe poder ver el perfil completo de un alumno: datos, semestre, historial de materias y auditoría de ese perfil | Alta | ✅ |
| RF-027 | El administrador debe poder asignar o avanzar el semestre de un alumno; el sistema debe impedir retroceder a un semestre igual o menor al actual | Alta | ✅ (validado en cliente y servidor) |

## Gestión de calificaciones (actor: Administrador)

| ID | Descripción | Prioridad | Estado |
|---|---|---|---|
| RF-030 | El administrador debe poder consultar todas las materias del plan de estudios de un alumno (según su carrera), con su calificación si existe | Alta | ✅ |
| RF-031 | El administrador debe poder capturar o editar la calificación de una materia concreta de cualquier alumno (escala 0-10) | Alta | ✅ |
| RF-032 | El sistema debe derivar automáticamente el estatus de una materia (Aprobada / No aprobó / Por cursar) a partir de la calificación capturada, si no se especifica uno explícito | Media | ✅ |
| RF-033 | El sistema debe rechazar calificaciones fuera del rango 0-10 | Alta | ✅ |

## Auditoría (actor: Administrador / Sistema)

| ID | Descripción | Prioridad | Estado |
|---|---|---|---|
| RF-040 | El sistema debe registrar automáticamente cada alta, edición, baja, eliminación de alumno y cada cambio de calificación o semestre, con actor, acción, detalle y fecha | Alta | ✅ |
| RF-041 | Cada movimiento de auditoría debe poder enlazarse al alumno afectado (no solo a quién lo realizó) | Alta | ✅ |
| RF-042 | El administrador debe poder consultar el historial completo de movimientos, filtrable por matrícula | Alta | ✅ |
| RF-043 | El historial de auditoría de un alumno eliminado debe conservarse aunque el perfil ya no exista | Media | ✅ |

## Estadísticas (actor: Administrador)

| ID | Descripción | Prioridad | Estado |
|---|---|---|---|
| RF-050 | El administrador debe poder consultar cuántos alumnos activos hay por carrera | Media | ✅ |
| RF-051 | El administrador debe poder consultar cuántos alumnos hay por semestre | Media | ✅ |
| RF-052 | El administrador debe poder consultar altas de alumnos de los últimos 6 meses | Baja | ✅ |
| RF-053 | El administrador debe poder consultar la distribución de calificaciones registradas (aprobadas / no aprobadas / pendientes) | Baja | ✅ |
| RF-054 | El administrador debe poder ver cuántos alumnos están activos, inactivos o sin semestre asignado | Media | ✅ |

## Catálogo académico (actor: Sistema)

| ID | Descripción | Prioridad | Estado |
|---|---|---|---|
| RF-060 | El sistema debe exponer el catálogo de carreras disponibles | Alta | ✅ |
| RF-061 | El sistema debe exponer el plan de estudios (materias, créditos, semestre) de una carrera dada | Alta | ✅ |
| RF-062 | El sistema debe exponer los grupos reales sembrados de una carrera dada | Media | ✅ |

---

## Resumen por estado

| Estado | Cantidad aproximada |
|---|---|
| ✅ Implementado y real | 33 |
| ⚠️ Implementado con datos simulados | 5 |
| 🔴 Pendiente | 3 |
