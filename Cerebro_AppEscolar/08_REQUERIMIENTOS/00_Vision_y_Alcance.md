# Documento de Visión y Alcance — AppTESCHI

> ⚠️ **Desactualizado (12/10/2026, DEC-043):** describe el sistema con backend propio, base de datos y panel de administrador. Hoy la app solo tiene las pantallas del alumno con datos del SIIA (ver [[REGLAS_PROYECTO]] y [[MAPA_PROYECTO]]). Actualizar antes de entregarlo.

> Documento marco del proyecto: qué es, para quién, por qué existe y hasta dónde llega en su estado actual. Actualizado 27/09/2026.
> **Relacionado con:** [[MAPA_PROYECTO]], [[REGLAS_PROYECTO]], [[01_Requerimientos_Funcionales]], [[02_Requerimientos_No_Funcionales]]

---

## 1. Contexto institucional

El Tecnológico de Estudios Superiores de Chimalhuacán (TESCHI) opera su gestión escolar a través de un portal institucional (SIIA). Desde septiembre de 2026 el SIIA publica una API REST oficial, pero solo cubre el inicio de sesión y la asistencia de docentes (ver [[API_SIIA_Oficial]]). Los trámites que hoy dependen de ese portal (reinscripción, consulta de calificaciones, tira de materias, kardex) están limitados a navegador de escritorio y no ofrecen una experiencia diseñada para dispositivos móviles, ni una vía para que el personal administrativo dé de alta, edite o consulte alumnos sin pasar por el sistema institucional completo.

## 2. Problema a resolver

1. Los alumnos no cuentan con una aplicación móvil para consultar su información escolar ni realizar trámites básicos.
2. El personal administrativo de control escolar no tiene una herramienta ágil para registrar alumnos, capturar calificaciones y consultar su historial sin depender del SIIA institucional.
3. No existe un registro auditable de quién hizo qué cambio administrativo sobre los datos de un alumno.
4. Los datos de plan de estudios, grupos y calificaciones vivían dispersos (hardcodeados en el cliente o inexistentes), sin una fuente de verdad única.

## 3. Objetivo general

Construir una aplicación Android para alumnos y un panel de administración con control total sobre los datos escolares, respaldados por una base de datos normalizada (SQL Server) y una API propia (Node.js/Express), que sirvan como capa complementaria al SIIA institucional mientras se evalúa una integración más profunda.

## 4. Objetivos específicos

- OE-1: Permitir a un alumno iniciar sesión con su usuario del SIIA (API oficial) o con una cuenta propia, siempre que ya exista en AppTESCHI, con verificación en dos pasos (OTP al correo institucional).
- OE-2: Dar al alumno acceso a un dashboard con sus módulos escolares (Reinscripción, Tira de Materias, Calificaciones, Kardex, Intersemestral).
- OE-3: Dar al administrador control total sobre el ciclo de vida de un alumno: alta, consulta, edición, baja (desactivación) y eliminación permanente.
- OE-4: Permitir al administrador capturar y consultar el historial de calificaciones de cualquier alumno, materia por materia.
- OE-5: Garantizar que el semestre de un alumno solo avance, nunca retroceda, como regla de negocio validada en cliente y servidor.
- OE-6: Registrar en una bitácora de auditoría todo movimiento administrativo relevante (alta, edición, baja, eliminación, cambio de calificación, cambio de semestre), enlazado al perfil afectado.
- OE-7: Ofrecer al administrador una vista de estadísticas agregadas (alumnos por carrera/semestre, altas recientes, distribución de calificaciones).
- OE-8: Mantener el catálogo curricular (carreras, materias por semestre, grupos) como datos reales, verificados contra los documentos oficiales de cada carrera, no inventados.

## 5. Alcance

### 5.1 Incluido en el alcance actual

- App Android nativa (Kotlin + Jetpack Compose) para alumno y administrador.
- API REST propia (`AppTeschi.Api`, Node.js/Express) como intermediaria entre la app y SQL Server.
- Base de datos SQL Server Express normalizada (identidad de alumnos, catálogo académico, auditoría), en un servidor Ubuntu propio.
- Autenticación con cuenta propia, cuenta de administrador o usuario del SIIA (validado por el backend con la API oficial), con ticket de login, OTP por correo institucional (Microsoft Graph) y token de sesión propio.
- CRUD completo de alumnos, gestión de calificaciones, auditoría filtrable y estadísticas — todo desde el panel de administrador.
- Catálogo curricular real de 7 carreras (ISC, Animación Digital y Efectos Visuales, Industrial, Mecatrónica, Química, Administración, Gastronomía), validado contra retículas y horarios oficiales.

### 5.2 Explícitamente fuera de alcance (por ahora)

- Traer del SIIA calificaciones, kardex, tira de materias u horarios: su API oficial no los expone. Esos módulos funcionan con los datos reales de `AppTeschiDB` que captura el administrador.
- Asistencia de docentes (la API del SIIA la tiene, pero se decidió no usarla).
- Módulo para docentes (planeado como Fase 3, todavía no iniciado).
- Pasarela de pago para reinscripción.
- Notificaciones push.
- Aplicación para iOS o versión web.
- Autenticación federada / SSO institucional (el login con el SIIA no es SSO: el backend valida la contraseña y luego la sesión es propia).
- Multi-idioma (la app y toda su documentación están en español).

## 6. Actores / Stakeholders

| Actor | Rol |
|---|---|
| Alumno | Usuario final que consulta y gestiona su información escolar desde el celular |
| Administrador escolar | Usuario con control total sobre alumnos, calificaciones, auditoría y estadísticas |
| Sistema (backend/automatizaciones) | Genera auditoría, envía OTP, valida reglas de negocio |
| Equipo de desarrollo | Mantiene app, API y base de datos |

## 7. Supuestos

- El servidor SQL Server y la API corren en un servidor Ubuntu propio, accesible por LAN y Tailscale, mientras no exista hospedaje institucional ni un dominio público (DEC-031).
- El administrador tiene criterio para decidir cuándo dar de baja (desactivar) versus eliminar (borrar permanentemente) a un alumno.
- Los datos curriculares sembrados (materias, créditos, grupos) reflejan la retícula oficial vigente al momento de la captura; cambios curriculares futuros requieren actualizar el catálogo manualmente.

## 8. Restricciones

- La API oficial del SIIA solo ofrece login y asistencia; no hay forma oficial de leer datos académicos del SIIA.
- La app usa exclusivamente Jetpack Compose (sin XML layouts) y el esquema de color institucional fijo (sin modo oscuro).
- El acceso a los endpoints requiere un token de sesión (JWT) de alumno o de administrador; ya no existe la API key compartida (DEC-030).

## 9. Criterios de éxito

- Un administrador puede completar el ciclo alta → asignar semestre → capturar calificaciones → consultar auditoría → estadísticas sin salir de la app.
- Ningún dato curricular sembrado es inventado: todo total de créditos y clave de grupo está validado contra un documento oficial o explícitamente marcado como pendiente de confirmar.
- Toda acción administrativa relevante queda registrada en `AuditoriaMovimientos`, enlazada al alumno afectado cuando aplica.
- El código compila sin errores y la suite de pruebas unitarias pasa al 100 % antes de cada entrega.
