# Módulo 8: Horarios (carga por Excel)

- **Estado:** ✅ Implementado y real — ver DEC-029.
- **Ruta en App:** `Routes.ADMIN_HORARIOS` (admin) · sección "Materias y Horario de Hoy" en `Routes.DASHBOARD` (alumno, tab Inicio)
- **Relacionado con:** [[MAPA_PROYECTO]], [[00_Dashboard]], [[06_PanelAdministrador]]

---

## Descripción General

Antes de este módulo no existía ninguna fuente real de horario de clases (día, hora, profesor, aula) — nada de eso vivía en la base de datos. Se agregó bajo un principio estricto: nunca inventar esa información. El horario solo puede entrar al sistema de una forma: un administrador sube un Excel real desde el panel de administración.

---

## Flujo

```
[Admin] AdminHorariosScreen
   │  Elige Carrera + Semestre (+ Grupo y Periodo opcionales)
   │  Selecciona un archivo .xlsx del dispositivo
   ▼
POST /api/horarios/importar (multipart, requireAdmin)
   │  Reemplaza por completo el horario anterior de esa carrera+semestre+grupo
   ▼
dbo.Horarios (SQL Server)
   │
   ▼
GET /api/mi-horario/{matricula} (x-api-key)
   │  Resuelve carrera+semestre reales del alumno, filtra por el día de hoy,
   │  calcula estado (EN_CURSO/PROXIMA/TERMINADA) contra la hora actual
   ▼
[Alumno] Inicio → "Materias y Horario de Hoy"
```

---

## Formato del Excel esperado

Fila 1 = encabezados (sin distinguir mayúsculas/acentos):

| Materia | Dia | HoraInicio | HoraFin | Profesor | Aula | Modalidad |
|---|---|---|---|---|---|---|
| Residencias Profesionales | Martes | 16:00 | 18:00 | Mtro. E. Vázquez | Cubículo D-04 | Presencial |

- `Materia`, `Dia`, `HoraInicio`, `HoraFin` son obligatorias — sin ellas el Excel se rechaza (400) antes de tocar la base de datos.
- `Profesor`, `Aula`, `Modalidad` son opcionales (`Modalidad` por defecto `PRESENCIAL`).
- `Dia` acepta Lunes..Domingo (texto). Horas aceptan formato `HH:MM`, celda de Excel con formato de hora, o número (fracción del día).
- Filas con día u hora inválidos se ignoran y se reportan en la respuesta (`erroresFilas`) — nunca detienen la importación completa.

---

## Endpoints

```
POST   /api/horarios/importar            (requireAdmin, multipart) → { filasImportadas, erroresFilas }
GET    /api/horarios?claveCarrera=&semestre=  (requireAdmin) → lista completa (para revisar/borrar)
DELETE /api/horarios/:id                 (requireAdmin)
GET    /api/mi-horario/:matricula        (x-api-key) → { dia, clases: [{ materia, horaInicio, horaFin, profesor, aula, modalidad, estado }] }
```

---

## Pruebas

`InicioViewModelTest.kt` cubre el consumo de `GET /api/mi-horario` con `FakeHorarioApi` (día/clases reales, error del backend, usuarios de prueba sem1..sem9 que nunca llaman al backend). La subida por Excel (`HorarioAdminService`) se verificó end-to-end con curl contra el backend real (ver DEC-029) — no tiene prueba unitaria propia porque, igual que el resto de los `*Service` de la app, no es inyectable con un servidor de prueba sin tocar `ApiConfig` (mismo criterio ya aplicado a `PerfilAlumnoService`, `ReinscripcionService`, etc.).

## Limitación conocida

El filtro del lado del alumno es solo por carrera+semestre, no por grupo específico — `dbo.Alumnos` no guarda a qué grupo pertenece cada alumno (mismo límite ya aceptado en Reinscripción y Tira de Materias). Si en algún semestre hay más de un grupo con horarios distintos, este endpoint los mezclaría todos.
