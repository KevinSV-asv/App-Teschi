# Diagrama de Componentes — AppTESCHI

> Cómo se dividen las responsabilidades dentro de cada aplicación y cómo se comunican entre sí.
> **Relacionado con:** [[05_Diagrama_Despliegue]], [[03_Diagramas_Secuencia]], [[REGLAS_PROYECTO]]

```mermaid
flowchart TB
    subgraph Android["App Android — Kotlin + Jetpack Compose (MVVM)"]
        direction TB
        UI["Capa UI<br/>Composables — LoginScreen, DashboardScreen,<br/>AdminAlumnosScreen, AdminCalificacionesScreen,<br/>AdminAuditoriaScreen, AdminEstadisticasScreen, AdminProfileScreen"]
        VM["Capa ViewModel<br/>AuthViewModel, ReinscripcionViewModel,<br/>TiraMateriasViewModel, KardexViewModel,<br/>CalificacionesViewModel (mock)"]
        SVC["Capa Servicio / Repositorio<br/>AdminUsersService, CalificacionesAdminService,<br/>AdminAuditService, EstadisticasService,<br/>PlanEstudiosService, AuthRepository,<br/>LocalAccountAuthService, SiiaAuthService, EmailOtpService"]
        LOCAL["Datos locales<br/>UserSession, AuditTrail, HistorialAcademico.kt (mock),<br/>SharedPreferences"]
        UI --> VM
        UI -.->|pantallas admin sin ViewModel, llaman servicio directo| SVC
        VM --> SVC
        VM --> LOCAL
        UI --> LOCAL
    end

    subgraph Backend["AppTeschi.Api — Node.js + Express"]
        direction TB
        ROUTES["Rutas REST<br/>/api/registro, /api/auth/cuenta,<br/>/api/usuarios (+CRUD), /api/calificaciones,<br/>/api/auditoria, /api/estadisticas,<br/>/api/plan-estudios, /api/grupos, /api/catalogos"]
        AUTH["Validación<br/>hasValidApiKey(), hashPassword()/verifyPassword() (scrypt)"]
        AUDIT["writeAudit(...)<br/>registra cada movimiento administrativo"]
        POOL["Pool de conexiones<br/>mssql / tedious"]
        ROUTES --> AUTH
        ROUTES --> AUDIT
        ROUTES --> POOL
        AUDIT --> POOL
    end

    subgraph DB["SQL Server Express — AppTeschiDB"]
        direction TB
        IDENT["Identidad<br/>Alumnos, AlumnoCredenciales"]
        CAT["Catálogos<br/>CatalogoCarreras, CatalogoSistemas,<br/>CatalogoPeriodos, CatalogoTurnos, CatalogoEstatusMateria"]
        ACAD["Dominio académico<br/>PlanEstudioMaterias, Grupos, HistorialAcademico"]
        AUD["Auditoría<br/>AuditoriaMovimientos"]
        SESSION["Sesión / OTP<br/>OtpHistorial, SesionesLogin"]
    end

    subgraph Externos["Sistemas externos"]
        direction TB
        SIIA["SIIA institucional<br/>ASP.NET Web Forms (scraping)"]
        SMTP["Servidor SMTP<br/>envío de OTP por correo"]
    end

    SVC -->|HTTPS + x-api-key<br/>vía túnel Cloudflare| ROUTES
    SVC -->|scraping OkHttp + Jsoup| SIIA
    SVC -->|SMTP| SMTP
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
