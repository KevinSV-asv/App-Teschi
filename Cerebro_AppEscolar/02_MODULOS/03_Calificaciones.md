# Módulo 3: Calificaciones

- **Estado:** ✅ Calificaciones parciales y finales del SIIA, consultadas directo desde la app (DEC-042, DEC-043, DEC-044). Ya no hay historial de `AppTeschiDB`.
- **Ruta en App:** `Routes.CALIFICACIONES`
- **Relacionado con:** [[MAPA_PROYECTO]], [[API_SIIA_Oficial]], [[02_TiraMaterias]], [[10_Kardex]], [[DECISIONES_TECNICAS]] (DEC-042, DEC-043, DEC-044), [[BUGS_Y_ERRORES]] (BUG-024)

---

## Qué ve el alumno

`GET calificaciones/parciales.ashx?periodo=` del SIIA con `Authorization: Bearer <token>`:

- Selector de periodo con los `periodosDisponibles` (de más reciente a más antiguo).
- Resumen: promedio parcial, materias evaluadas de las totales, aprobatorias y reprobatorias.
- Por materia: profesor, grupo, Parcial 1/2/3, promedio parcial y estatus (Aprobatoria / Reprobatoria / Sin calificar). Las calificaciones usan el texto del SIIA (`cal1Str`, p. ej. "NP"); si no viene, el número; si tampoco, "—".

Un selector arriba cambia entre **Parciales** y **Finales**; el periodo elegido se conserva al cambiar.

### Finales (DEC-044)

`GET calificaciones/finales.ashx?periodo=`, se pide hasta abrir la vista:

- Resumen: promedio final, materias evaluadas, aprobatorias y reprobatorias, y el dictamen del SIIA (Aprobatorio / Regularizar / En Curso).
- Por materia: profesor, grupo, 1ra vuelta (ordinario), 2da vuelta (extraordinario), calificación final y estatus.
- En un periodo en curso las materias salen "Sin calificar" y el promedio "—".

## Casos especiales (antes los resolvía el backend)

Valen igual para parciales y finales.

| Caso | Qué hace la app |
|---|---|
| 200 sin campo `ok` | Es éxito (la respuesta real empieza con `bloqueado`) |
| `bloqueado: true` (403 o 200) | Aviso "Calificaciones bloqueadas en el SIIA" con los trámites de `verificaciones.pendientes`; no es error |
| Error genérico del SIIA sin periodo elegido | Prueba los periodos recientes (actual y dos anteriores, "-1" = feb–jul, "-2" = ago–ene) y avisa "se muestra el periodo X" (BUG-024) |
| 401 | Token vencido: se borra la sesión y la app manda al login |
| 404 | "El SIIA no encontró tu registro de alumno." |
| Falla al cambiar de periodo | Se quedan las parciales que ya se veían y se avisa arriba |

## Estados de UI

- `Loading` — "Consultando calificaciones…"
- `Success` — resumen + materias, o el aviso de bloqueo
- `Empty` — "El SIIA no tiene materias registradas en este periodo." / "El SIIA no tiene calificaciones finales en este periodo."
- `Error` — mensaje con botón **Reintentar**

## Archivos

| Capa | Archivo |
|---|---|
| UI | `ui/modules/CalificacionesScreen.kt` (`ParcialesSiia`, `FinalesSiia`, `MateriaParcialCard`, `MateriaFinalCard`) |
| Estado | `viewmodel/CalificacionesViewModel.kt` (`seleccionarVista`, `seleccionarPeriodo`, `reintentar`) |
| Red | `service/CalificacionesSiiaService.kt` (`CalificacionesSiiaHttp`, `interpretarParciales`, `periodosRecientes`) |
| Pruebas | `CalificacionesSiiaHttpTest` (SIIA simulado), `CalificacionesSiiaParserTest`, `CalificacionesViewModelTest` |

## Pendiente

- Son parciales por periodo, no el kardex oficial: el Kardex está "en construcción" hasta que el SIIA exponga calificaciones finales.
- Calificaciones finales y extraordinarios: la API del SIIA no los da todavía.
