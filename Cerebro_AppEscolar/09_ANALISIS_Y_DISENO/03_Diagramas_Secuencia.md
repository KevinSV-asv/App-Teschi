# Diagramas de Secuencia — AppTESCHI

> ⚠️ **Desactualizado (12/10/2026, DEC-043):** describe el sistema con backend propio, base de datos y panel de administrador. Hoy la app solo tiene las pantallas del alumno con datos del SIIA (ver [[REGLAS_PROYECTO]] y [[MAPA_PROYECTO]]). Actualizar antes de entregarlo.

> Interacción temporal entre componentes para los flujos más críticos del sistema.
> **Relacionado con:** [[01_Casos_de_Uso]], [[04_Diagrama_Componentes]]

---

## 1. Iniciar sesión (ticket + OTP en el servidor) — CU-01

> Actualizado 27/09/2026. El diagrama anterior mostraba el scraping del SIIA y el OTP generado en el teléfono; ambos se retiraron (DEC-019, DEC-026). Hoy el SIIA se consulta desde el servidor con su API oficial (DEC-035).

### 1a. Paso SIIA de la cascada (cuando la cuenta de AppTESCHI no coincide)

```mermaid
sequenceDiagram
    actor U as Alumno
    participant LS as LoginScreen
    participant VM as AuthViewModel
    participant AR as AuthRepository / SiiaAuthService
    participant API as AppTeschi.Api
    participant SIIA as API oficial del SIIA
    participant DB as SQL Server
    participant MG as Microsoft Graph

    U->>LS: usuario SIIA + contraseña
    LS->>VM: loginSiia(usuario, password)
    VM->>AR: loginSiia(...)
    AR->>API: POST /api/auth/siia
    API->>SIIA: POST /login.ashx
    alt credenciales incorrectas
        SIIA-->>API: 401
        API-->>AR: 401 "usuario o contraseña incorrectos"
    else válidas (tipo = E)
        SIIA-->>API: 200 { usuario: numUsuario, tipo, idCarrera, ... }
        API->>DB: SELECT Alumnos WHERE Matricula = numUsuario
        alt no existe en AppTESCHI
            API->>API: guarda datos del SIIA solo en memoria (nada en la base, DEC-040)
            API-->>AR: ticket + correoEnmascarado + sinRegistro = true
            AR-->>VM: SiiaRequiereOtp
            VM-->>LS: paso = VERIFICACION (el código también vive en memoria)
        else existe
            API->>DB: guarda UsuarioSiia (1a vez) · enlaza carrera por IdCarreraSiia (DEC-037)
            alt carrera sin mapear
                API-->>AR: cuentaPendiente = true
            else todo listo
                API-->>AR: ticket (10 min) + correoEnmascarado (vacío si no hay correo, BUG-022)
                AR-->>VM: SiiaRequiereOtp
                VM-->>LS: paso = VERIFICACION (correo no editable, o campo para escribirlo)
            end
        end
    end

    U->>LS: "Enviar código"
    LS->>VM: enviarCodigoVerificacion()
    VM->>API: POST /api/otp/enviar (ticket)
    API->>DB: guarda hash del código (OtpHistorial)
    API->>MG: sendMail al CorreoOtp guardado
    U->>LS: código de 6 dígitos
    LS->>VM: verificarOtp(código)
    VM->>API: POST /api/otp/verificar (ticket + código)
    API-->>VM: token de sesión de alumno (12 h)
    LS->>LS: navigate(Dashboard)
```

### 1b. Cuenta de AppTESCHI (administrador o cuenta propia) — primeros pasos de la cascada

```mermaid
sequenceDiagram
    actor U as Usuario
    participant VM as AuthViewModel
    participant AR as AuthRepository
    participant API as AppTeschi.Api
    participant DB as SQL Server

    U->>VM: matrícula/usuario + contraseña
    VM->>AR: iniciarSesion(...)
    alt usuario de prueba (solo debug)
        AR-->>VM: entra sin backend
    else administrador
        AR->>API: POST /api/auth/administrador
        API->>DB: Administradores + AdministradorCredenciales (scrypt)
        API-->>AR: ticket
    else alumno con cuenta propia
        AR->>API: POST /api/auth/cuenta
        API->>DB: Alumnos + AlumnoCredenciales (scrypt)
        API-->>AR: ticket
    end
    Note over VM,API: mismo paso de OTP que en 1a → token de alumno o de administrador
    VM-->>U: Dashboard o AdminDashboard según el rol
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
    Svc->>API: POST /api/usuarios (Authorization: Bearer token de admin)
    API->>API: requireAdmin()?
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
