# Documento de Visión y Alcance — AppTESCHI

> Documento marco del proyecto: qué es, para quién, por qué existe y hasta dónde llega en su estado actual.
> **Relacionado con:** [[MAPA_PROYECTO]], [[REGLAS_PROYECTO]], [[01_Requerimientos_Funcionales]], [[02_Requerimientos_No_Funcionales]]

---

## 1. Contexto institucional

El Tecnológico de Estudios Superiores de Chimalhuacán (TESCHI) opera su gestión escolar a través de un portal institucional (SIIA) construido en ASP.NET Web Forms, sin API pública. Los trámites que hoy dependen de ese portal (reinscripción, consulta de calificaciones, tira de materias, kardex) están limitados a navegador de escritorio y no ofrecen una experiencia diseñada para dispositivos móviles, ni una vía para que el personal administrativo dé de alta, edite o consulte alumnos sin pasar por el sistema institucional completo.

## 2. Problema a resolver

1. Los alumnos no cuentan con una aplicación móvil para consultar su información escolar ni realizar trámites básicos.
2. El personal administrativo de control escolar no tiene una herramienta ágil para registrar alumnos, capturar calificaciones y consultar su historial sin depender del SIIA institucional.
3. No existe un registro auditable de quién hizo qué cambio administrativo sobre los datos de un alumno.
4. Los datos de plan de estudios, grupos y calificaciones vivían dispersos (hardcodeados en el cliente o inexistentes), sin una fuente de verdad única.

## 3. Objetivo general

Construir una aplicación Android para alumnos y un panel de administración con control total sobre los datos escolares, respaldados por una base de datos normalizada (SQL Server) y una API propia (Node.js/Express), que sirvan como capa complementaria al SIIA institucional mientras se evalúa una integración más profunda.

## 4. Objetivos específicos

- OE-1: Permitir a un alumno iniciar sesión (contra el SIIA real, contra una cuenta propia registrada en la base de datos, o mediante bypass de desarrollo) con verificación en dos pasos (OTP por correo).
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
- Base de datos SQL Server Express normalizada (identidad de alumnos, catálogo académico, auditoría).
- Autenticación en cascada (admin de desarrollo → cuenta propia → SIIA) con verificación OTP por correo.
- CRUD completo de alumnos, gestión de calificaciones, auditoría filtrable y estadísticas — todo desde el panel de administrador.
- Catálogo curricular real de 7 carreras (ISC, Animación Digital y Efectos Visuales, Industrial, Mecatrónica, Química, Administración, Gastronomía), validado contra retículas y horarios oficiales.

### 5.2 Explícitamente fuera de alcance (por ahora)

- Integración real por scraping con el SIIA para Reinscripción, Tira de Materias, Calificaciones (alumno) y Kardex — estos módulos son funcionales en UI pero **operan con datos simulados** (mock) hasta que exista scraping real o el catálogo se llene con datos del SIIA.
- Pasarela de pago para reinscripción.
- Notificaciones push.
- Aplicación para iOS o versión web.
- Autenticación federada / SSO institucional.
- Multi-idioma (la app y toda su documentación están en español).

## 6. Actores / Stakeholders

| Actor | Rol |
|---|---|
| Alumno | Usuario final que consulta y gestiona su información escolar desde el celular |
| Administrador escolar | Usuario con control total sobre alumnos, calificaciones, auditoría y estadísticas |
| Sistema (backend/automatizaciones) | Genera auditoría, envía OTP, valida reglas de negocio |
| Equipo de desarrollo | Mantiene app, API y base de datos |

## 7. Supuestos

- El servidor SQL Server y la API corren en un equipo con conectividad expuesta hacia internet mediante un túnel (Cloudflare) mientras no exista hospedaje institucional definitivo.
- El administrador tiene criterio para decidir cuándo dar de baja (desactivar) versus eliminar (borrar permanentemente) a un alumno.
- Los datos curriculares sembrados (materias, créditos, grupos) reflejan la retícula oficial vigente al momento de la captura; cambios curriculares futuros requieren actualizar el catálogo manualmente.

## 8. Restricciones

- El SIIA no expone una API REST — cualquier integración real con él requiere web scraping (OkHttp + Jsoup), no un cliente REST convencional.
- La app usa exclusivamente Jetpack Compose (sin XML layouts) y el esquema de color institucional fijo (sin modo oscuro).
- El acceso a los endpoints administrativos requiere una API key compartida (`x-api-key`) — no hay todavía un esquema de autenticación por token de sesión para la API.

## 9. Criterios de éxito

- Un administrador puede completar el ciclo alta → asignar semestre → capturar calificaciones → consultar auditoría → estadísticas sin salir de la app.
- Ningún dato curricular sembrado es inventado: todo total de créditos y clave de grupo está validado contra un documento oficial o explícitamente marcado como pendiente de confirmar.
- Toda acción administrativa relevante queda registrada en `AuditoriaMovimientos`, enlazada al alumno afectado cuando aplica.
- El código compila sin errores y la suite de pruebas unitarias pasa al 100 % antes de cada entrega.
