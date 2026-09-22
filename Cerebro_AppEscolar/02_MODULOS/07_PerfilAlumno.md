# Módulo 7: Perfil del Alumno

- **Estado:** ✅ Implementado y real.
- **Ruta en App:** `Routes.PERFIL_ALUMNO`
- **Relacionado con:** [[MAPA_PROYECTO]], [[05_RecuperarPassword]], [[06_PanelAdministrador]]

---

## Descripción General

Pantalla del alumno ya logueado para ver sus datos y hacer dos cosas sin salir de la sesión: actualizar su correo de recuperación y cambiar su contraseña dando la actual (distinto del flujo de "olvidé mi contraseña", pensado para cuando *no puede* entrar).

Se llega desde el chip con el nombre del alumno, arriba a la derecha del Dashboard — antes ese chip solo cerraba sesión; ahora abre el perfil, y "Cerrar sesión" vive como botón dentro de esta pantalla.

---

## Datos mostrados

| Campo | Editable | Fuente |
|---|---|---|
| Nombre completo | No | `Alumnos.NombreCompleto` — lo controla el director desde el panel de administración |
| Matrícula | No | `Alumnos.Matricula` |
| Carrera | No | `CatalogoCarreras` vía `Alumnos.IdCarrera` |
| Semestre | No | `Alumnos.Semestre` — lo controla el director |
| Correo institucional | No | `Alumnos.CorreoInstitucional` |
| Correo de recuperación | **Sí** | `Alumnos.CorreoOtp` — a dónde le llega el código si algún día olvida su contraseña |

---

## Endpoints API

```
GET  /api/mi-perfil/:matricula              → datos completos (x-api-key)
PUT  /api/mi-perfil/:matricula               → { correoOtp } (x-api-key)
POST /api/mi-perfil/cambiar-password         → { matricula, passwordActual, passwordNueva } (x-api-key)
```

- `PUT` valida formato de correo (`400` si no es válido) y solo toca la columna `CorreoOtp` — ningún otro campo se puede editar por esta vía.
- `cambiar-password` verifica `passwordActual` contra el hash real (`verifyPassword`, scrypt) antes de aceptar la nueva; `401` si no coincide. La nueva contraseña exige la misma política que el resto del sistema (8 caracteres, mayúscula, minúscula, número). Limitado por `excedeLimite` (8 intentos / 10 min por IP+matrícula) igual que el resto de los endpoints sensibles.

Ver DEC-027 en [[DECISIONES_TECNICAS]] para el razonamiento de diseño completo.

---

## App

- `PerfilAlumnoService.kt` — `PerfilAlumnoApi` con `obtener`, `actualizarCorreo`, `cambiarPassword`.
- `PerfilAlumnoViewModel.kt` — mismo patrón que `RecuperarPasswordViewModel`; valida correo y contraseña en cliente antes de llamar al backend.
- `PerfilAlumnoScreen.kt` — tres tarjetas: datos generales (solo lectura), correo de recuperación (editable), cambiar contraseña; botón de cerrar sesión al final.
- `PerfilAlumnoViewModelTest.kt` — 12 pruebas unitarias con `FakePerfilAlumnoApi` (carga, error del backend, correo inválido, correo actualizado, contraseña débil/no coincide/actual incorrecta, éxito).
