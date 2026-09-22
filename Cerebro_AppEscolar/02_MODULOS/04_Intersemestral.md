# Módulo 4: Intersemestral

- **Estado:** En diseño
- **Ruta en App:** `Routes.INTERSEMESTRAL`
- **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]]

---

## Descripción General

Permite al alumno consultar la oferta de materias intersemestrales disponibles, inscribirse a una o varias (según las reglas de negocio de la institución) y revisar el estatus de su inscripción.

---

## Campos / Datos a Mostrar

| Campo             | Tipo    | Descripción                                          |
|-------------------|---------|------------------------------------------------------|
| Clave materia     | String  | Identificador en SIIA                                |
| Nombre materia    | String  | Nombre completo                                      |
| Docente           | String  | Nombre del profesor asignado                         |
| Capacidad máxima  | Int     | Cupo total del grupo intersemestral                  |
| Lugares restantes | Int     | Cupo disponible al momento de la consulta            |
| Fecha inicio      | Date    | Inicio del periodo intersemestral                    |
| Fecha fin         | Date    | Fin del periodo intersemestral                       |
| Costo             | Float   | Costo de inscripción (si aplica)                     |
| Requisito previo  | String  | Materia o condición requerida para inscribirse       |

---

## Flujo de Pantalla

1. Usuario accede desde el Dashboard.
2. App consulta oferta de materias intersemestrales vigentes.
3. Se muestra lista de materias con cupo disponible.
4. Usuario selecciona materia(s) y confirma inscripción.
5. Se muestra confirmación con número de folio / comprobante.

---

## Endpoint API

> ⚠️ **PENDIENTE** — Ver [[01_APIS_POSTMAN]] cuando se documente.

```
GET  /api/intersemestral/oferta
POST /api/intersemestral/inscripcion
Authorization: Bearer {jwt_token}
```

**Payload POST:** A confirmar (ver cuestionario técnico).

---

## Estados de UI

- `Loading`   — Skeleton / CircularProgressIndicator
- `Success`   — LazyColumn con materias disponibles
- `Empty`     — Mensaje "No hay oferta intersemestral activa"
- `Inscrito`  — Banner de confirmación con folio
- `Error`     — Snackbar con opción de reintento

---

## Reglas de Negocio Pendientes de Confirmar

- ¿Cuántas materias puede inscribir un alumno por periodo intersemestral?
- ¿Existen restricciones por adeudos o estatus académico?
- ¿La inscripción requiere validación de pago previo?

---

## Notas de Implementación

- Lógica en `IntersemestralViewModel` (MVVM).
- El interceptor de Auditoría debe registrar la acción de inscripción.
