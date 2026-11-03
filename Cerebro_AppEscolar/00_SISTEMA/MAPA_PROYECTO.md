# MAPA DEL PROYECTO: AppTESCHI

> Documento central de navegación: toda nota del proyecto está enlazada aquí.
> Última actualización: **12/10/2026**

---

## Estado en una mirada

| Pieza | Estado |
|---|---|
| App Android 2.4 (solo alumnos) | ✅ Habla **directo con la API oficial del SIIA** (DEC-043, 12/10/2026): sin servidor propio, sin base de datos, sin Tailscale |
| Login | ✅ Usuario + contraseña del SIIA y después su **NIP** de 4 dígitos (crear / recuperar por CURP). Docentes y administrativos: "solo alumnos por ahora" |
| Datos | ✅ Calificaciones parciales y finales, Kardex con los datos del reporte oficial y su PDF (DEC-044, DEC-045), Tira de materias y resumen en Inicio. Reinscripción e Intersemestral: "Estamos trabajando en este apartado" (sin API) |
| Backend `AppTeschi.Api` + `AppTeschiDB` + servidor Ubuntu | ⛔ Histórico: se conserva en el repo, la app ya no lo usa |
| Docentes | ⏳ Sin pantallas (el SIIA tiene endpoints de asistencia, fuera de alcance) |

**Próximos pasos:** probar finales y Kardex con una cuenta real (colección de Postman y APK 2.4), pedir a la escuela la API de cambio de contraseña, reinscripción e intersemestral, apagar el servidor, actualizar la documentación formal. Detalle en [[02 de Noviembre]].

---

## Sistema

| Nota | Contenido |
|---|---|
| [[REGLAS_PROYECTO]] | Stack, arquitectura, reglas de código y de la bóveda |
| [[Glosario]] | Términos del negocio escolar y técnicos |

## Autenticación y seguridad

| Nota | Estado |
|---|---|
| [[2FA_Login]] | ✅ Cómo se entra: contraseña + NIP directo al SIIA, sesión con su token |
| [[09_Login_SIIA_por_Tipo]] | Plan del login por tipo de usuario (alumnos listos; las fases con backend son históricas) |
| [[04_Auditoria_Administrativa]] | ⛔ Histórico: auditoría del panel de administrador |
| [[Aviso_de_Privacidad]] | 📝 Borrador técnico, desactualizado por DEC-043 — falta revisión legal |

## APIs

| Nota | Contenido |
|---|---|
| [[API_SIIA_Oficial]] | API REST de la escuela (v1.2): la única fuente de datos de la app (login, NIP, calificaciones parciales y finales, kardex, PDF) |
| [[01_POSTMAN_API]] | Cómo probar con Postman; `SIIA_Alumno.postman_collection.json` es lo que hace la app |
| [[00_ARQUITECTURA_API]] | ⛔ Histórico: cómo estaba hecho `AppTeschi.Api` |

## Módulos de la app

| # | Nota / Pantalla | Ruta | Estado |
|---|---|---|---|
| — | [[2FA_Login]] `LoginScreen` | `Routes.LOGIN` | ✅ Contraseña + NIP del SIIA |
| — | [[00_Dashboard]] (tab "Inicio") | `Routes.DASHBOARD` | ✅ Datos del SIIA y resumen del periodo; barra de 5 pestañas |
| 2 | [[02_TiraMaterias]] | `Routes.TIRA_MATERIAS` | ✅ Materias del periodo actual (de las parciales del SIIA) |
| 3 | [[03_Calificaciones]] | `Routes.CALIFICACIONES` | ✅ Parciales y finales del SIIA por periodo |
| 1 | [[01_Reinscripcion]] | `Routes.REINSCRIPCION` | 🔴 "Estamos trabajando en este apartado": la API no tiene datos (DEC-046) |
| 4 | [[04_Intersemestral]] | Dentro de Reinscripción | 🔴 "Estamos trabajando en este apartado" |
| 10 | [[10_Kardex]] | `Routes.KARDEX` | ✅ Historial del SIIA con el diseño de Stitch, los datos del reporte oficial y su PDF |
| 7 | [[07_PerfilAlumno]] | `Routes.PERFIL_ALUMNO` | ✅ Foto de perfil (solo en el teléfono), datos del SIIA, Seguridad de la Cuenta (NIP funcional; contraseña en el portal), cerrar sesión |
| 9 | [[09_Login_SIIA_por_Tipo]] | `Routes.LOGIN` | ✅ Alumnos · docentes y administrativos sin acceso |
| 5 | [[05_RecuperarPassword]] | — | ⛔ Quitado: la contraseña es la del SIIA |
| 6 | [[06_PanelAdministrador]] | — | ⛔ Quitado (DEC-043) |
| 8 | [[08_Horarios]] | — | ⛔ Quitado (DEC-043) |

## Base de datos

| Nota | Contenido |
|---|---|
| [[Base_Datos_Usuarios]] | ⛔ Histórico: esquema V2 de `AppTeschiDB` y sus scripts SQL |

## Documentación formal

| Nota | Contenido |
|---|---|
| [[00_Vision_y_Alcance]] | Problema, objetivos, alcance, stakeholders |
| [[01_Requerimientos_Funcionales]] · [[02_Requerimientos_No_Funcionales]] · [[03_Matriz_Trazabilidad]] | Requerimientos y trazabilidad |
| [[01_Casos_de_Uso]] · [[02_Diagrama_Clases]] · [[03_Diagramas_Secuencia]] | Análisis y diseño |
| [[04_Diagrama_Componentes]] · [[05_Diagrama_Despliegue]] · [[06_Diagramas_Estados_y_Actividades]] | Arquitectura y comportamiento |
| [[07_Prompt_Stitch_Kardex]] · `08_Kardex_Propuesta_Stitch.html` | **Diseño del Kardex** (ya en la app, DEC-044): propuesta elegida (diseño de Stitch, datos ficticios) y su prompt. `08_Kardex_Propuesta.html` es la alternativa hecha a mano |
| [[01_Manual_Usuario_Alumno]] · [[02_Manual_Usuario_Administrador]] | Manuales de usuario |
| [[03_Manual_Tecnico_Instalacion]] · [[04_Manual_Mantenimiento]] | Instalación, servidor (§8), mantenimiento, `sqlcmd` |
| [[01_Plan_de_Pruebas]] · [[02_Catalogo_Casos_de_Prueba]] | Pruebas |

> ⚠️ Requerimientos, diseño, manuales y pruebas describen el sistema **antes de DEC-043** (con backend, base y panel de administrador) y están marcados como desactualizados. `Documentacion_Word/` es aún más vieja (12/09/2026). Actualizar y regenerar antes de entregar.

## Bitácora técnica

| Nota | Contenido |
|---|---|
| [[DECISIONES_TECNICAS]] | DEC-001 → DEC-048, con su vigencia |
| [[BUGS_Y_ERRORES]] | BUG-001 → BUG-024 |
| [[CAMBIOS_DE_CODIGO]] | Versiones v0.1 → v0.39 |

## Avances por sesión

| Nota | Resumen |
|---|---|
| [[21 de Agosto]] | Estructura base, paleta, NavHost inicial |
| [[24 de Agosto]] | Correcciones de build, OTP real, tema verde |
| [[27 de Agosto]] | Login SIIA por scraping (ya retirado), UI de login, SQL Express |
| [[28 de Agosto]] | Acceso administrativo y directorio de usuarios |
| [[01 de Septiembre]] | API conectada a la instancia real de SQL Server |
| [[02 de Septiembre]] | Directorio de usuarios: búsqueda, registro completo, edición |
| [[06 de Septiembre]] | Panel admin completo, rutas admin, fondo negro, Reinscripción conectada |
| [[09 de Septiembre]] | Panel admin real (DEC-017) y documentación formal completa |
| [[23 de Septiembre]] | Login SIIA fases 1–2 (primer intento) y migraciones en el servidor |
| [[27 de Septiembre]] | Login SIIA real, sin alta automática, correo institucional, mapeo de carreras, bóveda ordenada |
| [[28 de Septiembre]] | Logo oficial, botón "Descargar PDF" del Kardex, cualquier alumno del SIIA entra sin registro (DEC-040), BUG-022/023 |
| [[29 de Septiembre]] | API del SIIA 1.1 analizada; login del SIIA con NIP (DEC-041) |
| [[30 de Septiembre]] | Calificaciones parciales del SIIA en la app (DEC-042) |
| [[05 de Octubre]] | El SIIA corrigió su endpoint de calificaciones; ajuste a la respuesta real y Postman con el flujo NIP |
| [[12 de Octubre]] | Propuesta de Kardex (Stitch); la app habla directo con el SIIA, sin servidor (DEC-043), app 2.0 |
| [[02 de Noviembre]] | API 1.2 del SIIA: calificaciones finales, Kardex con los datos del original y PDF (DEC-044, DEC-045), colección de Postman, pestaña Reinscripción (DEC-046), Seguridad de la Cuenta (DEC-047), foto de perfil (DEC-048), app 2.4 |

## Herramientas de IA

| Carpeta | Para qué |
|---|---|
| `copilot/skills/`, `.kiro/skills/` | Skills del plugin Copilot de Obsidian y de Kiro (`actualizar-cerebro` actualiza la bóveda al cerrar una sesión) |
