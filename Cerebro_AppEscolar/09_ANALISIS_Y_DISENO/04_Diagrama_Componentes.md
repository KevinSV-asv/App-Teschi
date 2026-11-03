# Diagrama de Componentes — AppTESCHI

> ⚠️ **Desactualizado (12/10/2026, DEC-043):** describe el sistema con backend propio, base de datos y panel de administrador. Hoy la app solo tiene las pantallas del alumno con datos del SIIA (ver [[REGLAS_PROYECTO]] y [[MAPA_PROYECTO]]). Actualizar antes de entregarlo.

> Cómo se dividen las responsabilidades dentro de cada aplicación y cómo se comunican entre sí. Actualizado 27/09/2026.
> **Relacionado con:** [[05_Diagrama_Despliegue]], [[03_Diagramas_Secuencia]], [[REGLAS_PROYECTO]]

```mermaid
flowchart TB
    subgraph Android["App Android — Kotlin + Jetpack Compose (MVVM)"]
        direction TB
        UI["Capa UI<br/>Composables — LoginScreen, DashboardScreen + BottomNavBar,<br/>Reinscripcion/TiraMaterias/Calificaciones/Kardex/PerfilAlumno,<br/>Admin*Screen (alumnos, calificaciones, auditoría, estadísticas,<br/>administradores, reinscripciones, horarios)"]
        VM["Capa ViewModel<br/>AuthViewModel, InicioViewModel, ReinscripcionViewModel,<br/>TiraMateriasViewModel, CalificacionesViewModel, KardexViewModel,<br/>PerfilAlumnoViewModel, RegistroViewModel, RecuperarPasswordViewModel"]
        SVC["Capa Servicio / Repositorio (sobre ClienteHttp)<br/>AuthRepository, LocalAccountAuthService, AdminAccountAuthService,<br/>SiiaAuthService, OtpService, PerfilAlumnoService, HistorialAcademicoService,<br/>HorarioService, ReinscripcionService, Admin*Service, EstadisticasService"]
        LOCAL["Datos locales<br/>UserSession (token de sesión), AuditTrail,<br/>SharedPreferences"]
        UI --> VM
        UI -.->|pantallas admin sin ViewModel, llaman servicio directo| SVC
        VM --> SVC
        VM --> LOCAL
        UI --> LOCAL
    end

    subgraph Backend["AppTeschi.Api — Node.js + Express"]
        direction TB
        ROUTES["Rutas REST<br/>/api/auth/cuenta|administrador|siia, /api/otp/*,<br/>/api/mi-*, /api/reinscripcion/*, /api/registro,<br/>/api/usuarios (+CRUD), /api/calificaciones, /api/horarios,<br/>/api/auditoria, /api/estadisticas, /api/administradores,<br/>/api/plan-estudios, /api/grupos, /api/catalogos"]
        AUTH["Validación<br/>requireAlumno()/requireAdmin() (JWT), tickets de login,<br/>hashPassword()/verifyPassword() (scrypt), excedeLimite()"]
        EXT["Integraciones<br/>loginSiiaReal() · correo-graph.js"]
        AUDIT["writeAudit(...)<br/>registra cada movimiento administrativo"]
        POOL["Pool de conexiones<br/>mssql / tedious"]
        ROUTES --> AUTH
        ROUTES --> AUDIT
        ROUTES --> EXT
        ROUTES --> POOL
        AUDIT --> POOL
    end

    subgraph DB["SQL Server 2022 Express — AppTeschiDB"]
        direction TB
        IDENT["Identidad<br/>Alumnos, AlumnoCredenciales,<br/>Administradores, AdministradorCredenciales"]
        CAT["Catálogos<br/>CatalogoCarreras, CatalogoSistemas,<br/>CatalogoPeriodos, CatalogoTurnos, CatalogoEstatusMateria"]
        ACAD["Dominio académico<br/>PlanEstudioMaterias, Grupos, HistorialAcademico,<br/>Horarios, SolicitudesReinscripcion"]
        AUD["Auditoría<br/>AuditoriaMovimientos"]
        SESSION["Sesión / OTP<br/>OtpHistorial, SesionesLogin"]
    end

    subgraph Externos["Sistemas externos"]
        direction TB
        SIIA["API oficial del SIIA<br/>POST /login.ashx"]
        GRAPH["Microsoft Graph<br/>correo institucional (OTP)"]
        SMTP["SMTP Gmail<br/>respaldo"]
    end

    SVC -->|HTTP + Bearer token<br/>LAN / Tailscale| ROUTES
    EXT -->|HTTPS| SIIA
    EXT -->|HTTPS| GRAPH
    EXT -.->|SMTP| SMTP
    POOL --> IDENT
    POOL --> CAT
    POOL --> ACAD
    POOL --> AUD
    POOL --> SESSION
```

## Responsabilidades por componente

| Componente | Responsabilidad | No hace |
|---|---|---|
| Capa UI (Composables) | Renderizar estado, capturar entrada del usuario | No contiene lógica de negocio ni llamadas HTTP directas |
| Capa ViewModel | Orquestar estado de UI (`StateFlow`), invocar servicios, exponer resultado | No conoce detalles de HTTP/SQL; depende de interfaces inyectables |
| Capa Servicio (Android) | Construir requests HTTP, serializar/deserializar JSON, mapear a *data classes* | No decide reglas de negocio (las valida el backend) |
| Rutas REST (backend) | Validar entrada, aplicar reglas de negocio, orquestar consultas SQL | No conoce nada de Compose/Android |
| `writeAudit` | Registrar cada movimiento administrativo de forma consistente | No decide qué acciones auditar (cada ruta lo invoca explícitamente) |
| SQL Server | Persistir y garantizar integridad referencial (FKs, `CHECK`, transacciones) | No contiene lógica de aplicación (solo un par de vistas de conveniencia) |
