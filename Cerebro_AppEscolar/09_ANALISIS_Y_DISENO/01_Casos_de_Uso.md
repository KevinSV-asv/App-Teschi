# Casos de Uso — AppTESCHI

> ⚠️ **Desactualizado (12/10/2026, DEC-043):** describe el sistema con backend propio, base de datos y panel de administrador. Hoy la app solo tiene las pantallas del alumno con datos del SIIA (ver [[REGLAS_PROYECTO]] y [[MAPA_PROYECTO]]). Actualizar antes de entregarlo.

> Actores, diagrama de casos de uso y descripción detallada de los flujos más relevantes.
> **Relacionado con:** [[01_Requerimientos_Funcionales]], [[03_Matriz_Trazabilidad]], [[03_Diagramas_Secuencia]]

---

## Actores

| Actor | Descripción |
|---|---|
| **Alumno** | Estudiante inscrito en TESCHI, consume la app desde su celular |
| **Administrador** | Personal de control escolar, con control total sobre alumnos, calificaciones y auditoría |
| **Sistema** | Procesos automáticos del backend (auditoría, envío de OTP, validación de reglas) — actor secundario, no interactivo |
| **SIIA** | Sistema institucional externo. El backend consulta su API oficial (`login.ashx`) solo para validar contraseñas (DEC-035) |
| **Docente** | ⏳ Actor futuro (Fase 3 del [[09_Login_SIIA_por_Tipo]]); hoy no tiene casos de uso |

## Diagrama de casos de uso

```mermaid
flowchart LR
    Alumno((👤 Alumno))
    Admin((👤 Administrador))
    Sistema((⚙️ Sistema))
    Siia((🌐 SIIA))

    subgraph Autenticacion["Autenticación"]
        CU01([CU-01 Iniciar sesión])
        CU02([CU-02 Registrar cuenta])
    end

    subgraph ModulosAlumno["Módulos del alumno"]
        CU03([CU-03 Consultar dashboard])
        CU04([CU-04 Reinscribirse])
        CU05([CU-05 Consultar tira de materias])
        CU06([CU-06 Consultar calificaciones])
        CU07([CU-07 Consultar / exportar kardex])
    end

    subgraph GestionAlumnos["Gestión de alumnos (admin)"]
        CU08([CU-08 Registrar alumno])
        CU09([CU-09 Consultar directorio])
        CU10([CU-10 Editar alumno])
        CU11([CU-11 Eliminar alumno])
        CU12([CU-12 Dar de baja / reactivar])
        CU13([CU-13 Consultar perfil])
        CU14([CU-14 Asignar / avanzar semestre])
    end

    subgraph GestionAcademica["Gestión académica (admin)"]
        CU15([CU-15 Capturar calificación])
        CU19([CU-19 Consultar catálogo de carreras])
        CU20([CU-20 Consultar plan de estudios])
    end

    subgraph AuditoriaEstadisticas["Auditoría y estadísticas"]
        CU16([CU-16 Registrar auditoría])
        CU17([CU-17 Consultar auditoría])
        CU18([CU-18 Consultar estadísticas])
    end

    Alumno --> CU01
    Alumno --> CU02
    Alumno --> CU03
    Alumno --> CU04
    Alumno --> CU05
    Alumno --> CU06
    Alumno --> CU07
    CU01 -.->|extiende| Siia

    Admin --> CU01
    Admin --> CU08
    Admin --> CU09
    Admin --> CU10
    Admin --> CU11
    Admin --> CU12
    Admin --> CU13
    Admin --> CU14
    Admin --> CU15
    Admin --> CU17
    Admin --> CU18
    Admin --> CU19
    Admin --> CU20

    Sistema --> CU16
    CU08 -.->|incluye| CU16
    CU10 -.->|incluye| CU16
    CU11 -.->|incluye| CU16
    CU12 -.->|incluye| CU16
    CU14 -.->|incluye| CU16
    CU15 -.->|incluye| CU16
```

---

## Descripción detallada de casos de uso críticos

### CU-01 — Iniciar sesión

> Actualizado 27/09/2026 (antes describía el scraping del SIIA y el OTP en el teléfono).

- **Actor principal:** Alumno / Administrador
- **Actor secundario:** SIIA (externo, API oficial), Sistema (envío de OTP por Microsoft Graph)
- **Precondiciones:** El usuario existe en AppTESCHI (alumno dado de alta o registrado, o administrador) **o** es alumno con usuario del SIIA; en ese caso entra sin registro y solo ve los datos del SIIA (DEC-040).
- **Flujo principal:**
  1. El usuario captura su matrícula o usuario y su contraseña (de AppTESCHI o del SIIA) en un solo formulario.
  2. Se valida en cascada: `AdministradorCredenciales` → `AlumnoCredenciales` → API oficial del SIIA (y luego se busca la matrícula en `Alumnos`).
  3. Si es válido, el backend emite un ticket de 10 minutos y envía un código de 6 dígitos al correo **guardado** de la cuenta (la app lo muestra enmascarado).
  4. El usuario captura el código; el backend lo valida (10 minutos, 5 intentos).
  5. El backend emite el token de sesión (12 h) y la app navega al dashboard según el rol.
- **Flujos alternativos:**
  - 2a. Credenciales incorrectas → "Usuario o contraseña incorrectos".
  - 2b. (SIIA) La matrícula no existe en AppTESCHI → entra sin registro: ve nombre, carrera y correo del SIIA; los módulos académicos indican que aún no tiene registro (DEC-040). No se crea nada en la base.
  - 2c. (SIIA) La carrera del alumno no está mapeada → aviso de cuenta pendiente (DEC-037).
  - 2d. (SIIA) Tipo docente o administrativo → aún no soportado.
  - 2e. Demasiados intentos → 429, esperar.
  - 3a. La cuenta no tiene correo → la app lo pide; queda guardado al verificar el código (BUG-022).
  - 4a. Código incorrecto o vencido → reintentar o reenviar.
- **Postcondiciones:** Sesión activa con matrícula, nombre, rol y token.

### CU-08 — Registrar alumno (administrador)

- **Actor principal:** Administrador
- **Precondiciones:** El administrador tiene sesión activa; conoce la matrícula, nombre y carrera del alumno.
- **Flujo principal:**
  1. El administrador abre "Alumnos" → "Registrar alumno" y captura matrícula, nombre completo, carrera (del catálogo), correo(s) y, opcionalmente, semestre.
  2. El sistema valida que la matrícula no exista y que la carrera exista en el catálogo.
  3. El sistema inserta el alumno en `Alumnos` (sin credenciales — el propio alumno las crea después al registrarse).
  4. El sistema registra el movimiento en auditoría, enlazado al nuevo alumno.
  5. El directorio se refresca mostrando el alumno nuevo.
- **Flujos alternativos:**
  - 2a. Matrícula duplicada → error 409, no se crea el registro.
  - 2b. Carrera inexistente en el catálogo → error 400.
- **Postcondiciones:** Nuevo registro en `Alumnos`; un nuevo evento en `AuditoriaMovimientos`.

### CU-11 — Eliminar alumno (administrador)

- **Actor principal:** Administrador
- **Precondiciones:** El alumno existe en el directorio.
- **Flujo principal:**
  1. El administrador presiona "Eliminar" sobre un alumno y confirma en el diálogo de advertencia.
  2. El sistema abre una transacción: desvincula al alumno de su historial de auditoría (pone `IdAlumno = NULL`, conservando el texto de los movimientos), borra su historial académico, OTP, sesiones y, por cascada de base de datos, sus credenciales.
  3. El sistema borra el registro de `Alumnos` y confirma la transacción.
  4. El sistema registra un evento de auditoría "Alumno eliminado" (sin enlace a `IdAlumno`, porque ya no existe).
- **Flujos alternativos:**
  - 2a. Cualquier paso de la transacción falla → `rollback` completo, no queda el alumno a medio borrar.
  - 1a. Matrícula inexistente → error 404.
- **Postcondiciones:** El alumno y todo su historial académico desaparecen de forma permanente; su rastro de auditoría previo se conserva sin vínculo.

### CU-14 — Asignar / avanzar semestre (administrador)

- **Actor principal:** Administrador
- **Precondiciones:** El alumno existe.
- **Flujo principal:**
  1. El administrador abre el perfil del alumno y ve la tarjeta de semestre.
  2. Si el alumno no tiene semestre asignado, el sistema ofrece las opciones 1 a 12.
  3. Si el alumno ya tiene un semestre asignado, el sistema **solo** ofrece semestres superiores al actual.
  4. El administrador elige un semestre; el sistema lo guarda y refresca el historial de materias.
- **Flujos alternativos:**
  - 3a. El administrador (o un cliente externo a la app) intenta fijar un semestre igual o menor al actual → el servidor rechaza la solicitud con error 400, independientemente de si la app lo habría permitido.
  - 3b. El alumno ya está en el semestre máximo (12) → no se ofrece ninguna opción.
- **Postcondiciones:** `Alumnos.Semestre` actualizado; evento de auditoría "Semestre actualizado".

### CU-15 — Capturar calificación (administrador)

- **Actor principal:** Administrador
- **Precondiciones:** El alumno tiene una carrera asignada (para poder resolver su plan de estudios).
- **Flujo principal:**
  1. El administrador elige un alumno desde "Calificaciones".
  2. El sistema muestra todas las materias del plan de estudios de su carrera, agrupadas por semestre, con la calificación existente si la hay.
  3. El administrador captura o edita una calificación (0-10) para una materia y confirma.
  4. El sistema calcula el estatus (Aprobada ≥ 6, No aprobó < 6, Por cursar si se deja vacía) si no se especifica uno explícito, y guarda (inserta o actualiza) la fila en `HistorialAcademico`.
  5. El sistema registra el movimiento en auditoría.
- **Flujos alternativos:**
  - 3a. Calificación fuera de 0-10 → error 400, no se guarda.
- **Postcondiciones:** Fila nueva o actualizada en `HistorialAcademico`; evento de auditoría "Calificación actualizada".

### CU-16 / CU-17 — Registrar y consultar auditoría

- **Actor principal (CU-16):** Sistema (se dispara automáticamente dentro de cada endpoint administrativo, no es una acción que el usuario invoque directamente)
- **Actor principal (CU-17):** Administrador
- **Flujo principal (CU-16):** cada endpoint que modifica datos (alta, edición, baja, eliminación, calificación, semestre, sincronización) llama a `writeAudit(actor, accion, detalle, matriculaAfectada)`, que resuelve el `IdAlumno` del afectado por subconsulta y guarda un renglón inmutable con fecha, actor y detalle.
- **Flujo principal (CU-17):** el administrador abre "Auditoría", opcionalmente filtra por matrícula (busca coincidencia como actor **o** como perfil afectado) y consulta los movimientos más recientes.
- **Postcondiciones:** Ninguna acción administrativa relevante queda sin rastro.

---

## Resumen de casos de uso restantes

| CU | Resumen | Complejidad |
|---|---|---|
| CU-02 | Registrar cuenta propia con contraseña; completa un perfil existente si ya lo creó el administrador | Media |
| CU-03 | Ver tarjetas de módulos disponibles en el dashboard del alumno | Baja |
| CU-04 | Elegir periodo/grupo y enviar solicitud de reinscripción | Media |
| CU-05 | Ver materias pendientes del semestre actual | Baja |
| CU-06 | Ver calificaciones y promedio del semestre actual | Baja |
| CU-07 | Ver kardex completo y exportarlo a PDF | Media |
| CU-09 | Buscar/filtrar el directorio completo de alumnos | Baja |
| CU-10 | Editar campos individuales de un alumno sin afectar los demás | Baja |
| CU-12 | Alternar `Activo` de un alumno con confirmación | Baja |
| CU-13 | Ver información consolidada de un alumno (perfil + historial + auditoría) | Media |
| CU-18 | Ver gráficas agregadas de matrícula y calificaciones | Baja |
| CU-19 | Listar carreras del catálogo institucional | Baja |
| CU-20 | Ver materias y grupos reales de una carrera | Baja |
