# Diagramas de Estados y de Actividades — AppTESCHI

> Ciclo de vida de las entidades clave y flujo paso a paso de los procesos más relevantes.
> **Relacionado con:** [[01_Casos_de_Uso]], [[03_Diagramas_Secuencia]]

---

## Diagramas de estados

### Ciclo de vida de un alumno (`Alumnos.Activo` + `Alumnos.Semestre`)

```mermaid
stateDiagram-v2
    [*] --> SinSemestre : alta (POST /api/usuarios)
    SinSemestre --> Activo_ConSemestre : administrador asigna semestre
    SinSemestre --> Inactivo_SinSemestre : dar de baja

    Activo_ConSemestre --> Activo_ConSemestre : avanzar semestre\n(solo hacia adelante)
    Activo_ConSemestre --> Inactivo_ConSemestre : dar de baja
    Inactivo_ConSemestre --> Activo_ConSemestre : reactivar
    Inactivo_SinSemestre --> SinSemestre : reactivar

    SinSemestre --> [*] : eliminar (permanente)
    Activo_ConSemestre --> [*] : eliminar (permanente)
    Inactivo_ConSemestre --> [*] : eliminar (permanente)
    Inactivo_SinSemestre --> [*] : eliminar (permanente)

    note right of Activo_ConSemestre
        El semestre nunca retrocede:
        el servidor rechaza cualquier
        intento de bajar o repetir
        el valor actual (DEC-018)
    end note
    note right of [*]
        Eliminar borra también
        HistorialAcademico,
        AlumnoCredenciales, OTP
        y sesiones — es irreversible
    end note
```

### Ciclo de vida de una materia cursada (`HistorialAcademico`)

```mermaid
stateDiagram-v2
    [*] --> PorCursar : el alumno tiene la materia en su plan de estudios,\nsin fila en HistorialAcademico todavía
    PorCursar --> Aprobada : el administrador captura calificación >= 6
    PorCursar --> NoAprobada : el administrador captura calificación < 6
    Aprobada --> Aprobada : editar calificación (sigue >= 6)
    Aprobada --> NoAprobada : editar calificación (baja de 6)
    NoAprobada --> Aprobada : editar calificación (sube a >= 6)
    NoAprobada --> NoAprobada : editar calificación (sigue < 6)
    Aprobada --> PorCursar : quitar calificación (calificacion = null)
    NoAprobada --> PorCursar : quitar calificación (calificacion = null)
```

### Ciclo de vida de una sesión de inicio de sesión

```mermaid
stateDiagram-v2
    [*] --> CapturandoCredenciales
    CapturandoCredenciales --> Rechazado : ninguna fuente acepta las credenciales
    CapturandoCredenciales --> SesionAdminSinOtp : bypass administrador
    CapturandoCredenciales --> RequiereOtp : cuenta propia o SIIA válidos

    RequiereOtp --> EsperandoCodigo : OTP enviado por correo
    EsperandoCodigo --> EsperandoCodigo : código incorrecto (intento fallido)
    EsperandoCodigo --> Bloqueado : intentos fallidos >= umbral
    EsperandoCodigo --> RequiereOtp : código expirado (>10 min) / reenviar
    EsperandoCodigo --> SesionActiva : código correcto

    SesionAdminSinOtp --> SesionActiva
    SesionActiva --> [*] : cerrar sesión
    Rechazado --> CapturandoCredenciales : reintentar
    Bloqueado --> [*]
```

---

## Diagramas de actividades

### Alta de alumno por el administrador

```mermaid
flowchart TD
    Inicio([Inicio]) --> Captura[Administrador captura matrícula,\nnombre, carrera, correo, semestre]
    Captura --> ValidaLocal{¿Campos obligatorios\ncompletos?}
    ValidaLocal -- No --> ErrorLocal[Mostrar error de formulario] --> Captura
    ValidaLocal -- Sí --> Envia[Enviar POST /api/usuarios]
    Envia --> ExisteCarrera{¿Carrera existe\nen el catálogo?}
    ExisteCarrera -- No --> Error400[400: carrera inválida] --> Captura
    ExisteCarrera -- Sí --> ExisteMatricula{¿Matrícula ya\nregistrada?}
    ExisteMatricula -- Sí --> Error409[409: matrícula duplicada] --> Captura
    ExisteMatricula -- No --> Inserta[INSERT en Alumnos]
    Inserta --> Audita[INSERT en AuditoriaMovimientos]
    Audita --> Refresca[Refrescar directorio en la app]
    Refresca --> Fin([Fin])
```

### Captura de calificación por el administrador

```mermaid
flowchart TD
    Inicio([Inicio]) --> Elige[Administrador elige alumno]
    Elige --> Consulta[GET /api/calificaciones/:matricula]
    Consulta --> TieneCarrera{¿Alumno tiene\ncarrera asignada?}
    TieneCarrera -- No --> Vacio[Mostrar: sin materias] --> Fin([Fin])
    TieneCarrera -- Sí --> Muestra[Mostrar materias agrupadas\npor semestre, con calificación si existe]
    Muestra --> Captura[Administrador captura calificación 0-10]
    Captura --> Rango{¿Calificación\nentre 0 y 10?}
    Rango -- No --> ErrorRango[Mostrar error: fuera de rango] --> Captura
    Rango -- Sí --> Envia[PUT /api/calificaciones/:matricula/:idMateria]
    Envia --> DerivaEstatus[Backend deriva estatus:\nAprobada / No aprobó]
    DerivaEstatus --> ExisteFila{¿Ya existía\ncalificación previa?}
    ExisteFila -- Sí --> Update[UPDATE HistorialAcademico]
    ExisteFila -- No --> Insert[INSERT HistorialAcademico]
    Update --> Audita[INSERT AuditoriaMovimientos]
    Insert --> Audita
    Audita --> Refresca[Refrescar vista de calificaciones]
    Refresca --> Fin
```
