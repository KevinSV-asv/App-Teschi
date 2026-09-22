# Diagramas de Secuencia — AppTESCHI

> Interacción temporal entre componentes para los flujos más críticos del sistema.
> **Relacionado con:** [[01_Casos_de_Uso]], [[04_Diagrama_Componentes]]

---

## 1. Iniciar sesión (cascada de autenticación + OTP) — CU-01

```mermaid
sequenceDiagram
    actor U as Usuario
    participant LS as LoginScreen
    participant VM as AuthViewModel
    participant AR as AuthRepository
    participant LA as LocalAccountAuthService
    participant API as AppTeschi.Api
    participant DB as SQL Server
    participant SIIA as SIIA (scraping)
    participant OTP as EmailOtpService

    U->>LS: captura matrícula + contraseña
    LS->>VM: validarCredencialesSiia()
    VM->>AR: iniciarSesion(matricula, password)

    alt bypass administrador de desarrollo
        AR-->>VM: AdministradorSinOtp
        VM-->>LS: irAlDashboard = true
    else cuenta propia válida
        AR->>LA: autenticar(matricula, password)
        LA->>API: POST /api/auth/cuenta
        API->>DB: SELECT AlumnoCredenciales JOIN Alumnos
        DB-->>API: hash + salt
        API->>API: verifyPassword(hash, salt)
        API-->>LA: 200 ok (nombre)
        LA-->>AR: success
        AR-->>VM: CuentaPropiaRequiereOtp
        VM-->>LS: paso = VERIFICACION
    else credenciales solo válidas en el SIIA
        AR->>LA: autenticar(...)
        LA-->>AR: failure (401)
        AR->>SIIA: GET Login.aspx (extrae VIEWSTATE)
        AR->>SIIA: POST credenciales
        SIIA-->>AR: HTML de sesión válida
        AR-->>VM: SiiaRequiereOtp
        VM-->>LS: paso = VERIFICACION
    else credenciales inválidas en todas las fuentes
        AR-->>VM: Rechazado(mensaje)
        VM-->>LS: errorLogin
    end

    opt requiere OTP
        U->>LS: captura correo de contacto
        LS->>VM: enviarCodigoVerificacion(correo)
        VM->>AR: generarYEnviarOtp(correo)
        AR->>OTP: enviarOtp(correo, código)
        OTP-->>AR: ok
        AR-->>VM: (código, resultado)
        U->>LS: captura código de 6 dígitos
        LS->>VM: verificarOtp(código)
        VM->>VM: compara contra código en memoria (vigencia 10 min)
        VM-->>LS: onExito() / onFallo()
    end

    LS->>LS: navController.navigate(Dashboard | AdminDashboard)
```

---

## 2. Registrar alumno (administrador) — CU-08

```mermaid
sequenceDiagram
    actor Admin
    participant Pantalla as AdminAlumnosScreen
    participant Svc as AdminUsersService
    participant API as AppTeschi.Api
    participant DB as SQL Server

    Admin->>Pantalla: captura matrícula, nombre, carrera, semestre
    Pantalla->>Svc: crear(matricula, nombreCompleto, claveCarrera, ...)
    Svc->>API: POST /api/usuarios (x-api-key)
    API->>API: hasValidApiKey()?
    API->>DB: SELECT IdCarrera, Existe (subconsultas)
    alt carrera no existe
        DB-->>API: IdCarrera = NULL
        API-->>Svc: 400 "La carrera no existe"
    else matrícula duplicada
        DB-->>API: Existe > 0
        API-->>Svc: 409 "Ya existe un alumno con esa matrícula"
    else válido
        API->>DB: INSERT INTO Alumnos (...)
        API->>DB: INSERT INTO AuditoriaMovimientos (Alumno creado)
        API-->>Svc: 201 { ok: true, matricula }
    end
    Svc-->>Pantalla: Result
    Pantalla->>Pantalla: recarga++ (refresca directorio)
```

---

## 3. Eliminar alumno (transacción con limpieza en cascada) — CU-11

```mermaid
sequenceDiagram
    actor Admin
    participant Pantalla as AdminAlumnosScreen
    participant Svc as AdminUsersService
    participant API as AppTeschi.Api
    participant DB as SQL Server

    Admin->>Pantalla: "Eliminar" + confirmar en diálogo
    Pantalla->>Svc: eliminar(matricula)
    Svc->>API: DELETE /api/usuarios/:matricula
    API->>DB: SELECT IdAlumno, NombreCompleto WHERE Matricula = ...
    alt no existe
        DB-->>API: (vacío)
        API-->>Svc: 404
    else existe
        API->>DB: BEGIN TRANSACTION
        API->>DB: UPDATE AuditoriaMovimientos SET IdAlumno = NULL
        API->>DB: DELETE FROM HistorialAcademico
        API->>DB: DELETE FROM OtpHistorial
        API->>DB: DELETE FROM SesionesLogin
        API->>DB: DELETE FROM Alumnos (cascada borra AlumnoCredenciales)
        alt todo exitoso
            API->>DB: COMMIT
            API->>DB: INSERT AuditoriaMovimientos (Alumno eliminado)
            API-->>Svc: 200 { ok: true }
        else error en cualquier paso
            API->>DB: ROLLBACK
            API-->>Svc: 500 error
        end
    end
    Svc-->>Pantalla: Result
    Pantalla->>Pantalla: recarga++ (refresca directorio)
```

---

## 4. Avanzar semestre (regla "nunca retrocede") — CU-14

```mermaid
sequenceDiagram
    actor Admin
    participant Pantalla as AdminProfileScreen
    participant Svc as AdminUsersService
    participant API as AppTeschi.Api
    participant DB as SQL Server

    Note over Pantalla: el desplegable ya solo muestra<br/>semestres > semestre actual
    Admin->>Pantalla: elige "Semestre 6" (actual: 4)
    Pantalla->>Svc: actualizarPerfil(matricula, {semestre: 6})
    Svc->>API: PUT /api/usuarios/:matricula { semestre: 6 }
    API->>DB: SELECT Semestre FROM Alumnos WHERE Matricula = ...
    DB-->>API: Semestre actual = 4
    alt nuevo <= actual
        API-->>Svc: 400 "El semestre no puede retroceder (actual: 4)"
    else nuevo > actual
        API->>DB: UPDATE Alumnos SET Semestre = 6
        API->>DB: INSERT AuditoriaMovimientos (Semestre actualizado)
        API-->>Svc: 200 { ok: true }
    end
    Svc-->>Pantalla: Result
    Pantalla->>Pantalla: recarga historial de materias
```

---

## 5. Capturar calificación — CU-15

```mermaid
sequenceDiagram
    actor Admin
    participant Pantalla as AdminCalificacionesScreen
    participant Svc as CalificacionesAdminService
    participant API as AppTeschi.Api
    participant DB as SQL Server

    Admin->>Pantalla: elige alumno
    Pantalla->>Svc: obtener(matricula)
    Svc->>API: GET /api/calificaciones/:matricula
    API->>DB: SELECT Alumno + carrera
    API->>DB: SELECT PlanEstudioMaterias LEFT JOIN HistorialAcademico
    DB-->>API: materias + calificación (si existe)
    API-->>Svc: { alumno, materias[] }
    Svc-->>Pantalla: lista agrupada por semestre

    Admin->>Pantalla: captura calificación 8.5 en una materia
    Pantalla->>Svc: actualizar(matricula, idMateria, 8.5)
    Svc->>API: PUT /api/calificaciones/:matricula/:idMateria { calificacion: 8.5 }
    API->>API: valida rango 0-10
    API->>DB: SELECT IdEstatus WHERE Codigo = 'AP' (derivado: 8.5 >= 6)
    API->>DB: SELECT 1 FROM HistorialAcademico WHERE IdAlumno + IdMateria + EVALUACION
    alt ya existe fila
        API->>DB: UPDATE HistorialAcademico SET Calificacion, IdEstatus, FechaActualizacion
    else no existe
        API->>DB: INSERT INTO HistorialAcademico (...)
    end
    API->>DB: INSERT AuditoriaMovimientos (Calificación actualizada)
    API-->>Svc: 200 { ok: true }
    Svc-->>Pantalla: Result
    Pantalla->>Pantalla: recarga++ (refresca calificación mostrada)
```
