# Matriz de Trazabilidad — Requerimientos → Diseño → Implementación → Prueba

> Conecta cada requerimiento funcional con el caso de uso que lo detalla, la pantalla/servicio que lo implementa, el endpoint y tabla de base de datos involucrados, y el caso de prueba que lo verifica.
> **Relacionado con:** [[01_Requerimientos_Funcionales]], [[02_Casos_de_Uso]], [[02_Catalogo_Casos_de_Prueba]]

| RF | Caso de uso | Pantalla / Servicio (Android) | Endpoint (API) | Tabla(s) BD | Caso de prueba |
|---|---|---|---|---|---|
| RF-001 | CU-01 Iniciar sesión | `LoginScreen`, `AuthViewModel`, `AuthRepository` | `POST /api/auth/cuenta` (+ scraping SIIA) | `Alumnos`, `AlumnoCredenciales` | CP-01 |
| RF-002/003 | CU-01 Iniciar sesión | `AuthViewModel.enviarCodigoVerificacion/verificarOtp` | — (correo SMTP vía `EmailOtpService`) | — | CP-02 |
| RF-006/007 | CU-02 Registrar cuenta | `RegistroScreen`, `RegistroViewModel` | `POST /api/registro` | `Alumnos`, `AlumnoCredenciales` | CP-03 |
| RF-008 | — | `RECUPERAR_PASS` (placeholder) | — | — | Pendiente |
| RF-010 | CU-03 Consultar dashboard | `DashboardScreen` | `GET /api/usuarios` (indirecto vía sesión) | `Alumnos` | CP-04 |
| RF-011 | CU-04 Reinscribirse | `ReinscripcionScreen`, `ReinscripcionViewModel` | — (mock) | — | Pendiente (mock) |
| RF-012 | CU-05 Consultar tira de materias | `TiraMateriasScreen`, `TiraMateriasViewModel` | — (mock, `HistorialAcademico.kt`) | — | Pendiente (mock) |
| RF-013 | CU-06 Consultar calificaciones (alumno) | `CalificacionesScreen`, `CalificacionesViewModel` | — (mock) | — | Pendiente (mock) |
| RF-014/015 | CU-07 Consultar y exportar kardex | `KardexScreen`, `KardexViewModel`, `PdfKardexGenerator` | — (mock) | — | Pendiente (mock) |
| RF-020/021 | CU-08 Registrar alumno | `AdminAlumnosScreen`, `AdminUsersService.crear` | `POST /api/usuarios` | `Alumnos`, `CatalogoCarreras` | CP-10, CP-11, CP-12 |
| RF-022 | CU-09 Consultar directorio de alumnos | `AdminAlumnosScreen` | `GET /api/usuarios` | `Alumnos`, `CatalogoCarreras`, `AlumnoCredenciales` | CP-13 |
| RF-023 | CU-10 Editar alumno | `AdminAlumnosScreen`, `AdminProfileScreen`, `AdminUsersService.actualizarPerfil` | `PUT /api/usuarios/:matricula` | `Alumnos` | CP-14, CP-15 |
| RF-024 | CU-11 Eliminar alumno | `AdminAlumnosScreen`, `AdminUsersService.eliminar` | `DELETE /api/usuarios/:matricula` | `Alumnos`, `AlumnoCredenciales`, `HistorialAcademico`, `OtpHistorial`, `SesionesLogin`, `AuditoriaMovimientos` | CP-16, CP-17 |
| RF-025 | CU-12 Dar de baja / reactivar alumno | `AdminProfileScreen` (`BajaCard`) | `PUT /api/usuarios/:matricula` (`activo`) | `Alumnos` | CP-18 |
| RF-026 | CU-13 Consultar perfil de alumno | `AdminProfileScreen` | `GET /api/usuarios`, `GET /api/auditoria`, `GET /api/calificaciones/:matricula` | `Alumnos`, `AuditoriaMovimientos`, `HistorialAcademico` | CP-19 |
| RF-027 | CU-14 Asignar/avanzar semestre | `AdminProfileScreen` (`SemestreCard`) | `PUT /api/usuarios/:matricula` (`semestre`) | `Alumnos` | CP-20, CP-21, CP-22 |
| RF-030/031/032/033 | CU-15 Capturar calificación | `AdminCalificacionesScreen`, `CalificacionesAdminService` | `GET/PUT /api/calificaciones/:matricula[/:idMateria]` | `HistorialAcademico`, `PlanEstudioMaterias`, `CatalogoEstatusMateria` | CP-23, CP-24, CP-25 |
| RF-040/041/043 | CU-16 Registrar auditoría (automático) | `writeAudit(...)` en cada endpoint administrativo | (todos los `POST`/`PUT`/`DELETE` administrativos) | `AuditoriaMovimientos` | CP-26 |
| RF-042 | CU-17 Consultar auditoría | `AdminAuditoriaScreen`, `AdminAuditService` | `GET /api/auditoria?matricula=` | `AuditoriaMovimientos`, `Alumnos` | CP-27 |
| RF-050 → RF-054 | CU-18 Consultar estadísticas | `AdminEstadisticasScreen`, `EstadisticasService` | `GET /api/estadisticas` | `Alumnos`, `CatalogoCarreras`, `HistorialAcademico`, `CatalogoEstatusMateria` | CP-28 |
| RF-060 | CU-19 Consultar catálogo de carreras | `AdminAlumnosScreen` (selector), `PlanEstudiosService.carreras` | `GET /api/catalogos/registro` | `CatalogoCarreras` | CP-29 |
| RF-061 | CU-20 Consultar plan de estudios de una carrera | `PlanEstudiosService.materias` | `GET /api/plan-estudios/:clave` | `PlanEstudioMaterias`, `CatalogoCarreras` | CP-30 |
| RF-062 | CU-20 Consultar plan de estudios de una carrera | `PlanEstudiosService.grupos` | `GET /api/grupos/:clave` | `Grupos`, `CatalogoTurnos`, `CatalogoPeriodos` | CP-31 |

---

## Cobertura

- **RF con endpoint real y prueba manual verificada:** 27 de 33 RF marcados ✅ en [[01_Requerimientos_Funcionales]].
- **RF mock (sin backend real):** Reinscripción, Tira de Materias, Calificaciones (alumno) y Kardex — comparten la causa raíz: dependen de scraping real del SIIA que todavía no existe (ver [[00_Vision_y_Alcance]] §5.2).
- **RF pendiente:** Recuperar contraseña, Intersemestral.
