# Módulo 3: Calificaciones

- **Estado:** En diseño
- **Ruta en App:** `Routes.CALIFICACIONES`
- **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]]

---

## Descripción General

Muestra el historial de calificaciones del alumno por materia y periodo. Puede incluir calificaciones parciales, ordinario, extraordinario y equivalencia, según lo que exponga el SIIA.

---

## Campos / Datos a Mostrar

| Campo              | Tipo    | Descripción                                        |
|--------------------|---------|----------------------------------------------------|
| Periodo            | String  | Clave del semestre (ej. `2024-A`)                  |
| Clave materia      | String  | Identificador en SIIA                              |
| Nombre materia     | String  | Nombre completo                                    |
| Calificación       | Float   | Valor numérico (escala a confirmar: 0-10 / 0-100)  |
| Tipo evaluación    | Enum    | Parcial 1, Parcial 2, Ordinario, Extraordinario    |
| Estatus            | Enum    | Aprobado / Reprobado / Sin calificar               |

---

## Flujo de Pantalla

1. Usuario accede desde el Dashboard.
2. App obtiene lista de periodos disponibles.
3. Usuario selecciona el periodo (o se muestra el vigente por defecto).
4. Se listan las materias con sus calificaciones.

---

## Endpoint API

> ⚠️ **PENDIENTE** — Ver [[01_APIS_POSTMAN]] cuando se documente.

```
GET /api/alumnos/{matricula}/calificaciones?periodo={clave_periodo}
Authorization: Bearer {jwt_token}
```

**Respuesta esperada:** `200 OK` con array de calificaciones por materia.

---

## Estados de UI

- `Loading` — Skeleton / CircularProgressIndicator
- `Success` — LazyColumn agrupada por periodo
- `Empty`   — Mensaje "No hay calificaciones registradas para este periodo"
- `Error`   — Snackbar con opción de reintento

---

## Notas de Implementación

- Lógica en `CalificacionesViewModel` (MVVM).
- Confirmar escala de calificación (0-10 vs 0-100) con el área escolar.
- Confirmar si se exponen calificaciones de extraordinario en el mismo endpoint.
