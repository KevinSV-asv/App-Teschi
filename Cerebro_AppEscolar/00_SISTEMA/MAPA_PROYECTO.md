# MAPA DEL PROYECTO: AppTESCHI

> Documento central de navegación. Toda nota del proyecto está enlazada aquí.
> Última actualización: **19/09/2026**

---

## Documentación de proyecto (visión, requisitos, diseño, manuales, pruebas)

| Carpeta | Contenido |
|---|---|
| [[00_Vision_y_Alcance]] | Problema, objetivos, alcance incluido/excluido, stakeholders, criterios de éxito |
| [[01_Requerimientos_Funcionales]] | RF-001 → RF-062, por actor y módulo, con estado real (✅/⚠️ mock/🔴 pendiente) |
| [[02_Requerimientos_No_Funcionales]] | RNF por categoría ISO 25010 (rendimiento, seguridad, usabilidad, mantenibilidad, ...) |
| [[03_Matriz_Trazabilidad]] | RF → caso de uso → pantalla/servicio → endpoint → tabla → caso de prueba |
| [[01_Casos_de_Uso]] | Actores, diagrama de casos de uso, descripción detallada de los flujos críticos |
| [[02_Diagrama_Clases]] | Modelo de objetos (dominio admin + dominio de autenticación) |
| [[03_Diagramas_Secuencia]] | Login+OTP, alta/edición/eliminación de alumno, semestre, calificación |
| [[04_Diagrama_Componentes]] | División de responsabilidades entre app, backend y base de datos |
| [[05_Diagrama_Despliegue]] | Topología física actual (servidor Ubuntu en casa, acceso por LAN/Tailscale; laptop solo para desarrollo) y camino a producción |
| [[06_Diagramas_Estados_y_Actividades]] | Ciclo de vida del alumno, de una materia cursada, de una sesión; flujos de alta y captura de calificación |
| [[01_Manual_Usuario_Alumno]] | Guía de uso para estudiantes |
| [[02_Manual_Usuario_Administrador]] | Guía de uso del panel de administración |
| [[03_Manual_Tecnico_Instalacion]] | Cómo levantar el entorno completo desde cero, incluido el servidor Ubuntu (§8) |
| [[04_Manual_Mantenimiento]] | Procedimientos recurrentes: nueva carrera, nuevo endpoint, migraciones, troubleshooting |
| [[01_Plan_de_Pruebas]] | Estrategia, niveles de prueba, inventario de pruebas unitarias, brechas conocidas |
| [[02_Catalogo_Casos_de_Prueba]] | CP-01 → CP-31, con resultado esperado vs. obtenido |
| [[Glosario]] | Términos del negocio escolar y términos técnicos |

---

## Sistema y Reglas

| Nota | Contenido |
|---|---|
| [[REGLAS_PROYECTO]] | Stack técnico, arquitectura MVVM, paleta, convenciones |
| [[PROMPTS]] | Contexto base para sesiones con el agente de IA |

---

## Seguridad y Autenticación

| Nota | Estado |
|---|---|
| [[2FA_Login]] | ✅ Integrado — cascada admin/cuenta propia + OTP real generado y verificado en el servidor (DEC-019), con ticket de login y sesión propia del alumno (DEC-030). Ya no incluye SIIA (DEC-026) |
| [[Aviso_de_Privacidad]] | 📝 Borrador técnico — falta revisión legal antes de publicarse |

---

## Módulos de la App

| # | Nota / Pantalla | Ruta en código | Estado |
|---|---|---|---|
| — | [[2FA_Login]] `LoginScreen` | `Routes.LOGIN` | ✅ Funcional |
| — | [[00_Dashboard]] (tab "Inicio") | `Routes.DASHBOARD` | ✅ Funcional — barra de navegación inferior flotante (6 tabs), hero con estatus/semestre/carrera real y horario de hoy real, ver DEC-028/DEC-029 |
| 6 | [[06_PanelAdministrador]] | `Routes.ADMIN_DASHBOARD` (+ 4 secciones) | ✅ Funcional — CRUD real, calificaciones, auditoría enlazada, estadísticas |
| — | Perfil de alumno (admin) | `Routes.ADMIN_PROFILE/{matricula}` | ✅ Funcional — datos, semestre, historial de materias, baja |
| 7 | [[07_PerfilAlumno]] | `Routes.PERFIL_ALUMNO` | ✅ Real — el alumno ve sus datos, edita su correo de recuperación y cambia su contraseña dando la actual |
| 1 | [[01_Reinscripcion]] | `Routes.REINSCRIPCION` (+ `ADMIN_REINSCRIPCIONES`) | ✅ Fases 1 y 2 completas — regular/irregular/bloqueado real, panel del director real |
| 2 | [[02_TiraMaterias]] | `Routes.TIRA_MATERIAS` | ✅ Real — `GET /api/mi-historial/{matricula}` + grupo real vía `/api/grupos` |
| 3 | [[03_Calificaciones]] | `Routes.CALIFICACIONES` | ✅ Real de ambos lados — alumno vía `/api/mi-historial`, administrador vía `/api/calificaciones` (misma tabla, siempre sincronizados) |
| 4 | [[04_Intersemestral]] | `Routes.INTERSEMESTRAL` | 🔴 Placeholder |
| 5 | [[05_RecuperarPassword]] | `Routes.RECUPERAR_PASS` | ✅ Real — solo cuentas propias, código al correo ya registrado (nunca a uno escrito en el momento) |
| 8 | [[08_Horarios]] | `Routes.ADMIN_HORARIOS` (admin) | ✅ Real — carga por Excel, ver DEC-029 |
| — | Kardex (alumno) | `Routes.KARDEX` | ✅ Real — mismo `/api/mi-historial`; exportación a PDF real |

---

## APIs

| Nota | Contenido |
|---|---|
| [[SIIA_Scraping]] | 🔴 Histórico — dejó de usarse en DEC-026, AppTESCHI ya no depende del SIIA para nada |
| [[00_ARQUITECTURA_API]] | Arquitectura de `AppTeschi.Api` — flujo síncrono, sin cola offline |

---

## Base de Datos

| Nota | Contenido |
|---|---|
| [[Base_Datos_Usuarios]] | SQL Server Express — esquema V2 normalizado: `Alumnos`, `AlumnoCredenciales`, `OtpHistorial`, `SesionesLogin`, `AuditoriaMovimientos` + dominio académico (`PlanEstudioMaterias`, `Grupos`, `HistorialAcademico`) — 7 carreras sembradas con datos curriculares reales |

---

## Bitácora Técnica

| Nota | Contenido |
|---|---|
| [[CAMBIOS_DE_CODIGO]] | Historial de versiones v0.1 → v0.19 y modificaciones al código |
| [[BUGS_Y_ERRORES]] | BUG-001 → BUG-008 — causa raíz y estado de cada uno |
| [[DECISIONES_TECNICAS]] | DEC-001 → DEC-031 — decisiones de arquitectura y por qué se tomaron |

---

## Avances por Sesión

| Nota | Resumen |
|---|---|
| [[05_AVANCES/21 de Agosto]] | Estructura base, paleta, NavHost inicial |
| [[05_AVANCES/24 de Agosto]] | Build fixes, OTP real, botones de regreso, tema verde Obsidian |
| [[05_AVANCES/27 de Agosto]] | Login SIIA real, AuthViewModel, UI login estilo web TESCHI, SQL Express |
| [[05_AVANCES/06 de Septiembre]] | Fix fondo negro, panel admin completo, rutas admin, ReinscripcionScreen conectada, DatePicker, sin equivalencias |

---

## Skill de Agente

| Skill | Función |
|---|---|
| `copilot/skills/actualizar-cerebro` | Actualiza bóveda al finalizar cada sesión de trabajo |
