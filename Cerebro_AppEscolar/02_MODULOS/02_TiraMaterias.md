# Módulo 2: Tira de Materias

- **Estado:** ✅ Con datos del SIIA desde el 12/10/2026 (DEC-043): materias del periodo actual tomadas de las calificaciones parciales. Lo de abajo de "Cómo funciona hoy" es el **diseño original** (25/08/2026), se conserva como referencia.
- **Ruta en App:** `Routes.TIRA_MATERIAS`
- **Relacionado con:** [[MAPA_PROYECTO]], [[03_Calificaciones]], [[API_SIIA_Oficial]]

---

## Cómo funciona hoy (12/10/2026)

La API del SIIA no tiene un endpoint de tira de materias. Las materias que el alumno cursa en el periodo actual son las mismas que aparecen en sus calificaciones parciales, así que `TiraMateriasViewModel` llama a `parciales(null)` y muestra:

- Periodo, grupo(s) distintos y número de materias.
- Por materia: nombre, profesor y grupo.
- Si el SIIA bloquea la consulta por trámites, el aviso con la lista de pendientes.
- Pie: "Fuente: SIIA (materias de tus calificaciones parciales del periodo)".

No muestra créditos, horario ni aula: el SIIA no los da en ese endpoint.

| Capa | Archivo |
|---|---|
| UI | `ui/modules/TiraMateriasScreen.kt` |
| Estado | `viewmodel/TiraMateriasViewModel.kt` (`MateriaTira`) |
| Pruebas | `TiraMateriasViewModelTest` |

---

## Descripción General

Permite al alumno consultar las materias asignadas para el periodo actual (o un periodo histórico), con su carga horaria, grupo, docente y edificio.

---

## Campos / Datos a Mostrar

| Campo          | Tipo    | Descripción                                    |
|----------------|---------|------------------------------------------------|
| Clave materia  | String  | Clave de la materia (plan de estudios)   |
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

> Diseño original. Los endpoints reales están en [[Base_Datos_Usuarios]] §"Endpoints de `server.js`" y en la colección de Postman ([[01_POSTMAN_API]]).

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
