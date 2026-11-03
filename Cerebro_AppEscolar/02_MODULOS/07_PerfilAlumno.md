# Módulo 7: Perfil del Alumno

- **Estado:** ✅ Foto de perfil (DEC-048), datos del SIIA y "Seguridad de la Cuenta" con el diseño del portal (DEC-047, 02/11/2026): el NIP se cambia en la app; la contraseña, en el portal, porque la API no lo permite. ⏳ Falta probar el cambio de NIP con una cuenta real. El perfil anterior (correo de recuperación y cambio de contraseña de la cuenta propia, DEC-027) se quitó con el backend.
- **Ruta en App:** `Routes.PERFIL_ALUMNO` (botón "Mi perfil" en Inicio)
- **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]], [[API_SIIA_Oficial]]

---

## Qué tiene

| Sección | Contenido |
|---|---|
| Encabezado | Foto, nombre completo, matrícula y carrera (`area` del SIIA) |
| Foto de perfil | Tocar la foto o la cámara abre el selector de Android. Con foto, un menú deja cambiarla o quitarla. **Solo existe en ese teléfono**: la API no permite subirla. Es por cuenta y cerrar sesión no la borra |
| Datos | Correo institucional. Solo lectura: si algo está mal, se corrige en Servicios Escolares |
| Seguridad de la Cuenta | Selector Ver todo / Solo contraseña / Solo NIP |
| Contraseña de Acceso | Formulario **apagado**: la API no permite cambiarla. Un aviso lo explica y un botón abre el portal (`ApiConfig.SIIA_PORTAL_URL`) |
| NIP de Seguridad | Contraseña actual + NIP actual + nuevo + confirmación. Valida antes los datos, comprueba la contraseña con `login.ashx` y cambia con `nip.ashx` (`accion: cambiar`, `Bearer`). El cambio también aplica en el portal del SIIA |
| Cerrar sesión | Borra la sesión del teléfono y vuelve al login |

Un 401 al cambiar el NIP **no** cierra la sesión: puede significar "NIP actual incorrecto"; se muestra el mensaje del SIIA. Tampoco la cierra una contraseña incorrecta.

Los secretos solo viven en la pantalla mientras se escriben; al cambiar el NIP se limpian.

## Archivos

| Capa | Archivo |
|---|---|
| UI | `ui/dashboard/PerfilAlumnoScreen.kt` (`FotoAlumno`, `TarjetaContrasena`, `TarjetaNip`, `CampoSeguridad`) |
| Estado | `viewmodel/PerfilAlumnoViewModel.kt` (`cambiarFoto`, `quitarFoto`, `seleccionarSeccion`, `cambiarNip`, `cerrarSesion`) |
| Foto | `data/FotoPerfil.kt` (`FotoPerfilStore`, `FotoPerfilArchivos`) |
| Red | `service/SiiaAuthService.kt` (`login`, `cambiarNip`) |
| Pruebas | `PerfilAlumnoViewModelTest`, `FotoPerfilTest`, `SiiaHttpAuthServiceTest` |

## Cuando exista el endpoint de contraseña

Agregarlo a `SiiaEndpoints` y `SiiaAuthService`, encender los campos de `TarjetaContrasena`, conectar "Actualizar Contraseña" y quitar el aviso.
