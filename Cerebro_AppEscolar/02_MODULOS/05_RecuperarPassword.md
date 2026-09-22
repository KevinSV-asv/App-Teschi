# Módulo 5: Recuperar Contraseña

- **Estado:** ✅ Implementado y real (ver "Implementación real" al final) — el diseño original de abajo quedó desactualizado en varios puntos (endpoints, alcance).
- **Ruta en App:** `Routes.RECUPERAR_PASS`
- **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]]

---

## Descripción General

Permite al alumno restablecer su contraseña del SIIA cuando no puede iniciar sesión. El flujo se basa en verificación por correo institucional.

---

## Flujo de Pantalla

```
[Pantalla Login]
     │
     ▼  Tap "¿Olvidaste tu contraseña?"
[Paso 1 — Identificación]
  Usuario ingresa: Matrícula
     │
     ▼  App llama: POST /api/auth/recuperar → envía código al correo institucional
[Paso 2 — Verificación]
  Usuario ingresa: Código de 6 dígitos recibido por correo
     │
     ▼  App llama: POST /api/auth/verificar-codigo
[Paso 3 — Nueva Contraseña]
  Usuario ingresa: Nueva contraseña + Confirmación
     │
     ▼  App llama: PUT /api/auth/nueva-contrasena
[Confirmación] → Redirige a Login
```

---

## Campos por Paso

### Paso 1
| Campo     | Tipo   | Validación                  |
|-----------|--------|-----------------------------|
| Matrícula | String | No vacío, formato a definir |

### Paso 2
| Campo   | Tipo   | Validación         |
|---------|--------|--------------------|
| Código  | String | Exactamente 6 dígitos numéricos |

### Paso 3
| Campo                  | Tipo   | Validación                        |
|------------------------|--------|-----------------------------------|
| Nueva contraseña       | String | Mínimo 8 caracteres (política a confirmar) |
| Confirmar contraseña   | String | Debe coincidir con el campo anterior |

---

## Endpoints API

> ⚠️ **PENDIENTE** — Ver [[01_APIS_POSTMAN]] cuando se documente.

```
POST /api/auth/recuperar          → Paso 1: solicitar código
POST /api/auth/verificar-codigo   → Paso 2: validar código OTP
PUT  /api/auth/nueva-contrasena   → Paso 3: establecer nueva contraseña
```

> Nota: las cuentas nuevas se almacenan actualmente en `CuentasRegistro`. La recuperación de contraseña para esta tabla todavía debe implementarse antes de habilitarla en producción.

---

## Estados de UI

- `Loading` — CircularProgressIndicator en botón de acción
- `Success` — Banner de confirmación + botón "Ir a Login"
- `Error`   — Mensaje inline bajo el campo fallido (código incorrecto, matrícula no encontrada, etc.)

---

## Reglas de Negocio Pendientes de Confirmar

- ¿Cuántos minutos es válido el código OTP?
- ¿Cuántos intentos fallidos bloquean el proceso?
- ¿La política de contraseña requiere mayúsculas / caracteres especiales?
- ¿Se puede recuperar si el correo institucional no está activo?

---

## Notas de Implementación

- Lógica en `RecuperarPassViewModel` (MVVM).
- Usar `PasswordVisualTransformation` en los campos de nueva contraseña.
- El código OTP comparte lógica con el campo 2FA del Login; considerar componente reutilizable `OtpTextField`.

---

## Implementación real (15/09/2026)

**Alcance definido:** solo aplica a alumnos con **cuenta propia** de AppTESCHI
(los que se registraron con matrícula + contraseña vía `/api/registro`) — no
a alumnos que solo entran por el SIIA, porque esa contraseña vive en el
portal real, fuera de nuestro control.

**Decisión de seguridad (ver DEC-025):** a diferencia del OTP de login, aquí
**el correo nunca lo escribe quien hace la solicitud**. El backend siempre
manda el código al `CorreoOtp` que ya está guardado para esa matrícula, y
la app solo recibe la versión enmascarada (`k***n@gmail.com`) para
mostrarla, nunca el correo completo. Si se dejara escribir el correo
libremente en este flujo (como sí es válido hacerlo en el login, que ya
exige conocer la contraseña antes de llegar al OTP), cualquiera que supiera
una matrícula ajena podría robar esa cuenta mandándose el código a sí
mismo.

**Endpoints reales:**

```
POST /api/recuperar-password/solicitar   { matricula } → { correo: "k***n@gmail.com" }
POST /api/recuperar-password/confirmar   { matricula, codigo, nuevaPassword }
```

- Reutiliza `dbo.OtpHistorial` (misma tabla que el OTP de login) y las
  mismas reglas: 10 minutos de vigencia, máximo 5 intentos fallidos, y
  desde DEC-022 un código se puede volver a verificar dentro de su ventana
  aunque ya se haya usado (por si la app se cierra justo después).
- `solicitar` responde 404 si la matrícula no tiene cuenta propia
  registrada, y 409 si no tiene ningún `CorreoOtp` guardado (caso: alumno
  sincronizado del SIIA que nunca puso un correo — no hay a dónde mandar el
  código, tiene que contactar a control escolar).
- `confirmar` valida la nueva contraseña con la misma política que
  `/api/registro` (8 caracteres, mayúscula, minúscula, número) y actualiza
  `dbo.AlumnoCredenciales` con el mismo `hashPassword`/scrypt que usa el
  resto del backend — nunca se guarda en texto plano.
- Verificado de punta a punta con curl antes de tocar la app: solicitar →
  código incorrecto rechazado → código correcto cambia la contraseña →
  login real con la contraseña nueva funciona → caso sin cuenta propia da
  el error correcto.

**App:** `RecuperarPasswordScreen` (3 pasos: matrícula → código + nueva
contraseña → éxito), `RecuperarPasswordViewModel` con validación de
contraseña igual a `RegistroViewModel`, `RecuperarPasswordService`. Ya
conectado desde el enlace "¿Olvidaste tu contraseña?" de `LoginScreen`
(existía desde antes, apuntaba a un placeholder). 9 pruebas unitarias
nuevas con fake del servicio — 85/85 en verde en toda la suite.
