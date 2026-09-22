# Módulo 2: Tira de Materias

- **Estado:** En diseño
- **Ruta en App:** `Routes.TIRA_MATERIAS`
- **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]]

---

## Descripción General

Permite al alumno consultar las materias asignadas para el periodo actual (o un periodo histórico), con su carga horaria, grupo, docente y edificio.

---

## Campos / Datos a Mostrar

| Campo          | Tipo    | Descripción                                    |
|----------------|---------|------------------------------------------------|
| Clave materia  | String  | Identificador único de la materia en el SIIA   |
| Nombre materia | String  | Nombre completo de la asignatura               |
| Docente        | String  | Nombre del profesor asignado                   |
| Grupo          | String  | Clave de grupo (ej. `3A`, `2B`)               |
| Turno          | Enum    | Matutino / Vespertino                          |
| Edificio/Aula  | String  | Ubicación física                               |
| Créditos       | Int     | Valor crediticio de la materia                 |
| Horario        | String  | Días y horas de clase                          |

---

## Flujo de Pantalla

1. Usuario accede desde el Dashboard.
2. App llama al endpoint de tira de materias con el token JWT activo.
3. Se muestra lista de materias del periodo vigente.
4. Opcionalmente, selector de periodo para consulta histórica.

---

## Endpoint API

> ⚠️ **PENDIENTE** — Ver [[01_APIS_POSTMAN]] cuando se documente.

```
GET /api/alumnos/{matricula}/tira-materias?periodo={clave_periodo}
Authorization: Bearer {jwt_token}
```

**Respuesta esperada:** `200 OK` con array de materias. Ver cuestionario técnico.

---

## Estados de UI

- `Loading` — Skeleton / CircularProgressIndicator
- `Success` — LazyColumn con cards por materia
- `Empty`   — Mensaje "No tienes materias asignadas para este periodo"
- `Error`   — Snackbar con opción de reintento

---

## Notas de Implementación

- Lógica en `TiraMateriasViewModel` (MVVM).
- No almacenar en local sin cifrado (datos académicos sensibles).
- El interceptor de Auditoría debe registrar esta consulta.
