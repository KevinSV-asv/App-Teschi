# Diagrama de Clases — AppTESCHI

> Modelo de objetos del lado de la aplicación Android (Kotlin). Complementa el diagrama entidad-relación de [[Base_Datos_Usuarios]], que modela la persistencia; este diagrama modela cómo la app representa y mueve esos datos en memoria.
> **Relacionado con:** [[Base_Datos_Usuarios]], [[04_Diagrama_Componentes]]

---

## Dominio de administración (alumnos, calificaciones, auditoría, estadísticas)

```mermaid
classDiagram
    class ManagedUser {
        +Int id
        +String username
        +String name
        +UserRole role
        +String correo
        +String correoInstitucional
        +String carrera
        +String claveCarrera
        +Int? semestre
        +Boolean active
    }
    class UserRole {
        <<enumeration>>
        ALUMNO
        ADMINISTRADOR
    }
    class MateriaCalificacion {
        +Int idMateria
        +String nombre
        +Int creditos
        +Int semestre
        +Double? calificacion
        +String? estatusCodigo
        +String? estatusNombre
    }
    class AlumnoCalificaciones {
        +String matricula
        +String nombreCompleto
        +Int? semestre
        +String carrera
        +String claveCarrera
    }
    class CarreraCatalogo {
        +String clave
        +String nombre
    }
    class MateriaPlan {
        +String nombre
        +Int creditos
        +Int semestre
    }
    class GrupoInfo {
        +String clave
        +Int semestre
        +String turno
        +Int numero
        +String periodo
    }
    class RemoteAuditEvent {
        +String actorMatricula
        +String actorNombre
        +String perfilMatricula
        +String accion
        +String detalle
        +String fecha
    }
    class EstadisticasResumen {
        +List~ConteoCarrera~ porCarrera
        +List~ConteoSemestre~ porSemestre
        +Int activos
        +Int inactivos
        +Int sinSemestre
        +List~ConteoMes~ altasPorMes
        +List~ConteoEstatus~ calificaciones
    }

    class AdminUsersService {
        <<object>>
        +fetch() Result~List~
        +crear(matricula, nombreCompleto, claveCarrera, ...) Result~Unit~
        +actualizarPerfil(matricula, cambios) Result~Unit~
        +eliminar(matricula) Result~Unit~
        +actualizarSemestre(matricula, semestre) Result~Unit~
    }
    class CalificacionesAdminService {
        <<object>>
        +obtener(matricula) Result~Pair~
        +actualizar(matricula, idMateria, calificacion) Result~Unit~
    }
    class AdminAuditService {
        <<object>>
        +fetch(perfilMatricula) Result~List~
    }
    class EstadisticasService {
        <<object>>
        +obtener() Result~EstadisticasResumen~
    }
    class PlanEstudiosService {
        <<object>>
        +carreras() Result~List~
        +materias(clave) Result~List~
        +grupos(clave) Result~List~
    }

    AdminUsersService ..> ManagedUser : produce
    CalificacionesAdminService ..> AlumnoCalificaciones : produce
    CalificacionesAdminService ..> MateriaCalificacion : produce
    AdminAuditService ..> RemoteAuditEvent : produce
    EstadisticasService ..> EstadisticasResumen : produce
    PlanEstudiosService ..> CarreraCatalogo : produce
    PlanEstudiosService ..> MateriaPlan : produce
    PlanEstudiosService ..> GrupoInfo : produce
    ManagedUser --> UserRole

    class AdminAlumnosScreen { <<Composable>> }
    class AdminCalificacionesScreen { <<Composable>> }
    class AdminProfileScreen { <<Composable>> }
    class AdminAuditoriaScreen { <<Composable>> }
    class AdminEstadisticasScreen { <<Composable>> }

    AdminAlumnosScreen ..> AdminUsersService : usa
    AdminAlumnosScreen ..> PlanEstudiosService : usa
    AdminCalificacionesScreen ..> CalificacionesAdminService : usa
    AdminCalificacionesScreen ..> AdminUsersService : usa
    AdminProfileScreen ..> AdminUsersService : usa
    AdminProfileScreen ..> CalificacionesAdminService : usa
    AdminProfileScreen ..> AdminAuditService : usa
    AdminAuditoriaScreen ..> AdminAuditService : usa
    AdminEstadisticasScreen ..> EstadisticasService : usa
```

> Todos los `*Service` son `object` de Kotlin (singleton), implementados sobre `OkHttpClient` + `org.json`, sin capa de mapeo adicional — el JSON se parsea directo a estas data classes.

---

## Dominio de autenticación (estrategia en cascada)

```mermaid
classDiagram
    class AuthViewModel {
        -AuthRepository authRepository
        +uiState StateFlow~AuthUiState~
        +validarCredencialesSiia(matricula, password)
        +enviarCodigoVerificacion(correoOtp, onEnviado)
        +verificarOtp(codigo, ...)
    }
    class AuthRepository {
        -CuentaAuthService cuentaAuthService
        -SiiaAuth siiaAuthService
        -OtpSender otpSender
        +iniciarSesion(matricula, password) LoginResultado
        +generarYEnviarOtp(correo) Pair
    }
    class LoginResultado {
        <<sealed class>>
    }
    class AdministradorSinOtp {
        +String matricula
        +String nombre
    }
    class AlumnoPruebaSinOtp {
        +String matricula
        +String nombre
        +Int? semestreSimulado
    }
    class CuentaPropiaRequiereOtp {
        +String matricula
        +String nombre
    }
    class SiiaRequiereOtp {
        +String matricula
    }
    class Rechazado {
        +String mensaje
    }
    LoginResultado <|-- AdministradorSinOtp
    LoginResultado <|-- AlumnoPruebaSinOtp
    LoginResultado <|-- CuentaPropiaRequiereOtp
    LoginResultado <|-- SiiaRequiereOtp
    LoginResultado <|-- Rechazado

    class CuentaAuthService {
        <<interface>>
        +autenticar(matricula, password) Result
    }
    class LocalAccountAuthService {
        <<object>>
    }
    class SiiaAuth {
        <<interface>>
        +validarCredenciales(matricula, password) Result
    }
    class SiiaAuthService {
        <<object>>
    }
    class OtpSender {
        <<interface>>
        +enviarOtp(correo, codigo) Result
    }
    class EmailOtpService {
        <<object>>
    }

    CuentaAuthService <|.. LocalAccountAuthService
    SiiaAuth <|.. SiiaAuthService
    OtpSender <|.. EmailOtpService

    AuthViewModel --> AuthRepository
    AuthRepository --> CuentaAuthService
    AuthRepository --> SiiaAuth
    AuthRepository --> OtpSender
    AuthRepository ..> LoginResultado : produce
```

> `CuentaAuthService`, `SiiaAuth` y `OtpSender` son interfaces (patrón *Strategy*) inyectadas por constructor en `AuthRepository` — permite sustituir cada fuente por un *fake* en pruebas unitarias (`AuthViewModelTest`) sin tocar red real.
