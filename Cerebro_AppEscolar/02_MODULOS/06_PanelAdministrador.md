# Módulo 6: Panel de Administrador

- **Estado:** ✅ Funcional — CRUD real contra `AppTeschiDB`, sin datos simulados
- **Ruta en App:** `Routes.ADMIN_DASHBOARD` (hub) + `ADMIN_ALUMNOS`, `ADMIN_CALIFICACIONES`, `ADMIN_AUDITORIA`, `ADMIN_ESTADISTICAS`, `ADMIN_PROFILE/{matricula}`
- **Relacionado con:** [[MAPA_PROYECTO]], [[DECISIONES_TECNICAS]] DEC-017/DEC-018, [[02_Manual_Usuario_Administrador]], [[Base_Datos_Usuarios]]

---

## Descripción General

Da al administrador control total sobre el ciclo de vida de un alumno (alta, consulta, edición, baja, eliminación), su historial de calificaciones, la bitácora de auditoría y estadísticas agregadas — organizado en 4 secciones independientes en vez de una sola pantalla saturada.

## Pantallas y responsabilidad de cada una

| Pantalla | Responsabilidad |
|---|---|
| `AdminDashboardScreen` | *Hub* de navegación — sesión activa, permiso de ubicación, log de seguridad local, tarjetas hacia las 4 secciones |
| `AdminAlumnosScreen` | Alta, búsqueda, edición y eliminación (con confirmación) de alumnos |
| `AdminProfileScreen` | Perfil de un alumno: datos, semestre (solo avanza), historial de materias con calificación, baja/reactivación, auditoría de ese perfil |
| `AdminCalificacionesScreen` | Selección de alumno → captura/edición de calificaciones por materia |
| `AdminAuditoriaScreen` | Historial completo de movimientos, filtrable por matrícula |
| `AdminEstadisticasScreen` | Gráficas: alumnos por carrera/semestre, altas recientes, distribución de calificaciones |

## Endpoints consumidos

Ver tabla completa en [[Base_Datos_Usuarios]] §"Endpoints de `server.js`". En resumen: `POST/GET/PUT/DELETE /api/usuarios[...]`, `GET/PUT /api/calificaciones/...`, `GET /api/auditoria`, `GET /api/estadisticas`, `GET /api/catalogos/registro`, `GET /api/plan-estudios/:clave`.

## Reglas de negocio implementadas

- El semestre de un alumno **nunca retrocede** — validado en la app (el desplegable solo ofrece semestres superiores) y en el backend (rechaza con 400 aunque no venga de la app).
- **Dar de baja** (`Activo = 0`) es reversible y conserva todo el historial; **eliminar** borra permanentemente al alumno y su historial académico, en una transacción atómica.
- Toda alta, edición, baja, eliminación, cambio de calificación o de semestre queda registrada en `AuditoriaMovimientos`, enlazada al alumno afectado.
- La calificación de una materia deriva su estatus automáticamente (Aprobada ≥ 6, No aprobó < 6) si no se especifica uno explícito.

## Notas de implementación

- Estas pantallas **no usan `ViewModel`** — siguen el patrón `remember`/`mutableStateOf` directo en el Composable, consistente con el resto del panel de administrador (a diferencia de los módulos del alumno, que sí usan MVVM completo).
- El directorio local (`AdminDirectory`, `SharedPreferences`) y su cola de sincronización offline (`PendingUserSync`) se retiraron por completo en DEC-017 — todo el CRUD es contra la base de datos real, sin capa intermedia local.
- Datos de prueba disponibles con prefijo `2024PRUEBA*` para explorar el panel sin usar información de alumnos reales (ver [[CAMBIOS_DE_CODIGO]] v0.19).

## Historial

| Fecha | Cambio |
|---|---|
| 08-09/09/2026 | Construcción completa: CRUD real, calificaciones, auditoría enlazada, estadísticas (DEC-017) |
| 09/09/2026 | Ajuste de perfil: historial de materias en vez de carga esperada, semestre solo hacia adelante, baja de alumno (DEC-018) |
