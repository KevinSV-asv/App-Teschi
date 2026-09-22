# Bitácora de Avances — 28 de Agosto de 2026

- **Relacionado con:** [[MAPA_PROYECTO]]

## Sesión — Acceso administrativo y directorio de usuarios

### Implementación

- Se añadió acceso administrativo local con usuario `admin` y contraseña `admin`.
- El administrador entra directamente al dashboard administrativo, sin verificación OTP.
- Los alumnos continúan usando validación SIIA y segundo factor por correo.
- El dashboard determina la vista por el rol de `UserSession`; el alumno no puede activar visualmente la consola administrativa.
- Se creó `AdminDirectory` con operaciones de registrar, consultar, actualizar y eliminar alumnos y administradores.
- La cuenta local `admin` no puede eliminarse desde el directorio.
- Cada operación CRUD administrativa se registra en `AuditTrail`.

### Seguridad y pendientes

- `admin/admin` es una credencial de demostración local y debe reemplazarse antes de producción.
- El directorio y la auditoría viven en memoria; falta conectarlos con API y SQL Server.
- El backend debe entregar el rol administrador y aplicar autorización en cada operación sensible.
- El permiso de ubicación continúa siendo explícito y opcional.

### Validación

- `:app:compileDebugKotlin` exitoso.
