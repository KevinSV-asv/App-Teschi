# Autenticación: contraseña + NIP del SIIA

- **Estado:** ✅ Directo contra la API oficial del SIIA desde el 12/10/2026 (DEC-043), sin backend propio. Reescrita ese día: el flujo anterior (cuenta propia, administrador, ticket + código por correo y el login del SIIA pasando por `AppTeschi.Api`) está en DEC-019, DEC-020, DEC-030, DEC-035 y DEC-041, y en el historial de git.
- **Ruta en la app:** `Routes.LOGIN`
- **Relacionado con:** [[API_SIIA_Oficial]], [[09_Login_SIIA_por_Tipo]], [[DECISIONES_TECNICAS]] (DEC-041, DEC-043)

---

## Quién puede entrar

Solo alumnos del SIIA (`tipo = E`), con su usuario (matrícula), su contraseña del SIIA y su NIP de 4 dígitos. Docentes (`P`) y administrativos (`A`) ven "Por ahora AppTESCHI es solo para alumnos…". Ya no hay cuentas propias, administradores, registro ni usuarios de prueba.

## Flujo

```
usuario + contraseña ─► SIIA POST login.ashx ─► { requiereNip, tieneNip, tempToken (10 min), usuario? }
                                                    │
NIP (o crear NIP)    ─► SIIA POST nip.ashx  ─► { token (480 min), usuarioInfo }
   accion: verificar | configurar                   │
                                                    ▼
                         UserSession: token + vencimiento + datos de la cuenta ─► Inicio
```

- **Tiene NIP:** la app pide "NIP del SIIA". **No tiene:** pide crearlo (nuevo + confirmación) con `configurar`; sirve también en el portal del SIIA.
- **"¿Olvidaste tu NIP?":** CURP → `nip.ashx` con `accion: recuperar`. El SIIA manda el NIP al correo institucional; la app muestra el correo enmascarado solo si la CURP es de quien está entrando.
- Si el paso 1 ya trae `usuario` con un tipo que no es alumno, se rechaza antes del NIP. Si no lo trae, se revisa con `usuarioInfo` del paso NIP.
- Si el SIIA diera el token sin pedir NIP, se entra directo.
- Los límites de intentos los pone el SIIA (429 con su mensaje).

## Sesión

- `UserSession` guarda el token final del SIIA, cuándo vence (480 min, con 1 min de margen) y los datos de la cuenta (`usuario`, `numUsuario`, `nombreCompleto`, `correo`, `area` como carrera, `idCarrera`).
- Está en SharedPreferences `appteschi_sesion`, **excluidas** del respaldo en la nube y de la copia entre teléfonos (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`).
- Con el token vigente la app abre directo en Inicio.
- Si una consulta con el token recibe 401, `SesionAlumno` borra la sesión y la app muestra "Tu sesión del SIIA expiró" y manda al login.
- "Cerrar sesión" (Perfil) borra todo del teléfono.
- La contraseña y el NIP **nunca se guardan**; el `tempToken` solo vive en memoria durante el paso NIP.

## Cambiar el NIP

Desde Perfil: NIP actual + nuevo + confirmación → `nip.ashx` con `accion: cambiar` y `Authorization: Bearer <token>`. Aquí un 401 puede ser "NIP actual incorrecto", así que **no** cierra la sesión.

## Contraseña

Es la del SIIA: se cambia y se recupera en el portal del SIIA. La app solo lo indica con un texto.

## Archivos

| Capa | Archivos |
|---|---|
| UI | `ui/login/LoginScreen.kt` (`CredencialesSection`, `NipSection`) |
| Estado | `viewmodel/AuthViewModel.kt` (`validarCredenciales`, `verificarNip`, `crearNip`, `recuperarNip`) |
| Datos | `data/repository/AuthRepository.kt`, `data/UserSession.kt`, `data/CuentaSiia.kt` |
| Red | `service/SiiaAuthService.kt` (`SiiaHttpAuthService`), `service/AlumnoAuthHeader.kt` (`SesionAlumno`), `core/config/ApiConfig.kt` (`SiiaEndpoints`) |
| Pruebas | `SiiaHttpAuthServiceTest` (SIIA simulado), `AuthRepositoryTest`, `AuthViewModelTest`, `UserSessionTest` |

## Diseño visual del login

- Tarjeta blanca con el formulario (usuario + contraseña, luego NIP) y sección verde inferior con el logo, "¡Bienvenido!", "Usa la misma cuenta y el mismo NIP que en el SIIA." y la versión con fecha de compilación.
- Colores de `ui/theme/Color.kt` (`LoginInk`, `LoginFieldBg`, `LoginGreenBright`…), nunca hex sueltos.
- Desplazamiento vertical para celulares pequeños; respeta barras del sistema y teclado.
