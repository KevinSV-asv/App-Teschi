# Manual de Usuario — Administrador

> Guía de uso del panel de administración de AppTESCHI: control total sobre alumnos, calificaciones, auditoría y estadísticas.
> **Relacionado con:** [[01_Manual_Usuario_Alumno]], [[01_Casos_de_Uso]]

---

## 1. Iniciar sesión como administrador

Captura tu matrícula/usuario de administrador y contraseña. Si tus credenciales son válidas como administrador, entras directamente al **Centro de control**, sin verificación por OTP.

## 2. Centro de control (pantalla principal)

Al entrar verás:

- **Sesión activa** — tu usuario, matrícula y rol actual.
- Cuatro secciones, cada una en su propia pantalla:

| Sección | Para qué sirve |
|---|---|
| **Alumnos** | Alta, edición, baja y eliminación de alumnos |
| **Calificaciones** | Capturar/editar la calificación de cualquier alumno en cualquier materia |
| **Auditoría** | Ver el historial completo de movimientos administrativos |
| **Estadísticas** | Ver gráficas de matrícula, avance y calificaciones |

Debajo encontrarás el permiso de ubicación (para enriquecer la auditoría con coordenadas) y un registro de actividad local de este dispositivo (permisos, sesión) — distinto del historial de Auditoría, que es el que refleja movimientos reales sobre la base de datos.

---

## 3. Sección Alumnos

### 3.1 Registrar un alumno nuevo

1. Toca el ícono **"+"** en la barra superior.
2. Captura matrícula, nombre completo, correo, correo institucional (opcional — se genera solo si lo dejas vacío) y elige la carrera del catálogo.
3. Opcionalmente, asigna el semestre (1 a 12).
4. Toca **"Registrar"**.

> El alumno no queda con contraseña — la crea él mismo cuando se registra en la app con su matrícula.

### 3.2 Buscar un alumno

Usa el buscador para filtrar por matrícula, nombre, correo o carrera.

### 3.3 Editar un alumno

Toca **"Editar"** sobre la tarjeta del alumno, modifica los campos que necesites y toca **"Guardar cambios"**. Solo se actualizan los campos que cambiaste.

### 3.4 Eliminar un alumno (permanente)

Toca **"Eliminar"**. Aparecerá una confirmación explicando que se borrará también su historial académico y credenciales — **esta acción no se puede deshacer**. Úsala solo para correcciones de captura, no para el flujo normal de un alumno que deja la institución (para eso usa "Dar de baja", dentro de su perfil).

### 3.5 Ver el perfil completo de un alumno

Toca **"Ver perfil"**. Ahí encontrarás:

- **Información del perfil** — datos generales y estado (Activo / Dado de baja).
- **Semestre** — asigna el semestre inicial o avánzalo. El desplegable **solo muestra semestres superiores** al actual; no es posible regresar a uno anterior.
- **Historial de materias** — todas las materias de su plan de estudios, agrupadas por semestre, con su calificación (si ya se capturó) y su estatus (Aprobada / No aprobó / Por cursar).
- **Baja de alumno** — para desactivar (o reactivar) sin borrar su información. Un alumno dado de baja conserva todo su historial y puede reactivarse cuando quieras.
- **Movimientos de este perfil** — auditoría específica de ese alumno.

---

## 4. Sección Calificaciones

1. Entra a **Calificaciones** y busca/elige un alumno.
2. Verás sus materias agrupadas por semestre. Las que ya tienen calificación muestran su nota y su estatus con color (verde = aprobada, rojo = no aprobó, amarillo = por cursar).
3. Para capturar o editar una calificación, escribe un número de **0 a 10** en el campo junto a la materia y toca **"Guardar"**.
4. El estatus (Aprobada/No aprobó) se calcula solo según la calificación — no necesitas elegirlo manualmente.

---

## 5. Sección Auditoría

1. Entra a **Auditoría** para ver el historial completo de movimientos: altas, ediciones, bajas, eliminaciones, cambios de calificación y de semestre.
2. Usa el campo de filtro para buscar solo los movimientos relacionados con una matrícula específica (ya sea porque esa persona los hizo, o porque fue la afectada).
3. Cada movimiento muestra: acción, fecha, detalle, y quién lo realizó.

---

## 6. Sección Estadísticas

Consulta de un vistazo:

- **Activos / Inactivos / Sin semestre** — conteo general de la matrícula.
- **Alumnos por carrera** — cuántos alumnos activos hay en cada una de las 7 carreras.
- **Alumnos por semestre** — distribución de la matrícula activa.
- **Altas de los últimos meses** — cuántos alumnos se registraron recientemente.
- **Calificaciones registradas** — cuántas están aprobadas, no aprobadas o pendientes.

---

## 7. Buenas prácticas

- Prefiere **"Dar de baja"** sobre **"Eliminar"** salvo que estés corrigiendo un error de captura — la baja es reversible, la eliminación no.
- Antes de avanzar el semestre de un alumno, revisa su **Historial de materias** para confirmar que ya cursó lo que corresponde.
- Usa el filtro de **Auditoría** por matrícula cuando necesites reconstruir qué pasó con un alumno en particular.
