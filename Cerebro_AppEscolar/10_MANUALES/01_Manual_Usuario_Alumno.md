# Manual de Usuario — Alumno

> ⚠️ **Desactualizado (12/10/2026, DEC-043):** describe el sistema con backend propio, base de datos y panel de administrador. Hoy la app solo tiene las pantallas del alumno con datos del SIIA (ver [[REGLAS_PROYECTO]] y [[MAPA_PROYECTO]]). Actualizar antes de entregarlo.

> Guía de uso de AppTESCHI para estudiantes. No requiere conocimientos técnicos.
> **Relacionado con:** [[02_Manual_Usuario_Administrador]], [[00_Vision_y_Alcance]]

---

## 1. Crear tu cuenta

1. Abre AppTESCHI y en la pantalla de inicio toca **"Registrarse"**.
2. Captura tu matrícula, nombres, apellido paterno, apellido materno y fecha de nacimiento.
3. Elige tu sistema (escolarizado/otro) y tu carrera de la lista.
4. Marca la casilla de equivalencias solo si aplica a tu caso.
5. Crea una contraseña de al menos 8 caracteres, con una mayúscula, una minúscula y un número (sin espacios ni símbolos).
6. Toca **"Crear cuenta"**. Si tu matrícula ya estaba precargada por la institución, tus datos se completan automáticamente en ese mismo registro — no se crea una cuenta duplicada.

> Tu correo institucional se genera automáticamente como `tu_matricula@teschi.edu.mx`.

## 2. Iniciar sesión

Hay un solo formulario. Puedes entrar con tu usuario y contraseña del **SIIA** (los del portal de la escuela) o con la matrícula y contraseña de tu **cuenta de AppTESCHI** (sección 1); la app reconoce sola cuál es.

**Con tu usuario del SIIA**

1. Escribe tu matrícula y tu contraseña del SIIA, y toca **Iniciar sesión**.
2. Escribe tu **NIP del SIIA** (4 dígitos) y toca **Entrar**.
   - Si todavía no tienes NIP, la app te pide crearlo: escríbelo dos veces y toca **Crear NIP y entrar**. Es el mismo NIP que te pedirá el portal del SIIA.
   - Si lo olvidaste, toca **¿Olvidaste tu NIP?**, escribe tu CURP y el SIIA te lo manda a tu correo institucional.
3. Entras a tu **Inicio**. Con el SIIA no hay código por correo: el NIP ya es tu segundo paso de seguridad.

**Con tu cuenta de AppTESCHI**

1. Escribe tu matrícula y tu contraseña, y toca **Iniciar sesión**.
2. La app te envía sola un código de 6 dígitos y te muestra a qué correo (parcialmente oculto). Revisa también spam; el código vale 10 minutos.
3. Escribe el código. Si no llegó o expiró, toca **Reenviar código**.
4. Entras a tu **Inicio**.

**Mensajes que puedes ver**

| Mensaje | Qué significa |
|---|---|
| "Entraste con tu cuenta del SIIA, pero aún no tienes registro en AppTESCHI" | Entraste bien, pero control escolar todavía no carga tus datos académicos en la app. Pídeselo al administrador; mientras, ves tu nombre, carrera y correo del SIIA |
| "Tu cuenta está pendiente" | Tu carrera todavía no está conectada con el SIIA. Avísale al administrador; en cuanto la conecte podrás entrar |
| "NIP incorrecto" | Revisa tu NIP. Después de 5 intentos tendrás que volver a escribir tu contraseña |
| "Demasiados intentos" | Espera uno o dos minutos antes de volver a intentar |
| "No se pudo conectar con el servidor…" | Revisa tu internet y que Tailscale esté encendido |

> Si olvidaste tu contraseña del SIIA, cámbiala en el portal del SIIA. "¿Olvidaste tu contraseña?" de la app solo sirve para la cuenta de AppTESCHI.

## 3. El Dashboard

Desde el Dashboard puedes:

- Ver tu nombre y cerrar sesión (ícono de perfil, esquina superior derecha).
- Acceder a cada módulo escolar desde la barra de navegación inferior:

| Módulo | Qué hace |
|---|---|
| **Reinscripción** | Consulta tu estatus (regular/irregular) y envía tu solicitud de reinscripción con grupo/turno y comprobante de pago |
| **Tira de Materias** | Muestra las materias que te corresponde cursar este semestre |
| **Calificaciones** | Muestra tus calificaciones y tu promedio del semestre actual |
| **Kardex** | Muestra tu historial completo de materias, tu promedio general y qué porcentaje de tu carrera llevas cubierto; puedes exportarlo a PDF |
| **Intersemestral** | Consulta si hay un periodo intersemestral disponible |

> La información de estos módulos es la que registra control escolar en AppTESCHI (tus calificaciones, tu historial y tu horario reales); no se lee del SIIA.

## 4. Reinscripción

1. Entra a **Reinscripción** desde el Dashboard.
2. La app consulta tu situación:
   - **Regular:** el sistema asigna tu grupo automáticamente, no necesitas elegir nada.
   - **Irregular (elegible):** eliges tu grupo del siguiente periodo.
   - **Bloqueado:** verás un mensaje indicando que debes presentarte con tu director de carrera antes de continuar.
3. Si eliges grupo, adjunta tu comprobante de pago (PDF o imagen) y toca **"Enviar"**.
4. Recibirás un **folio de confirmación** — guárdalo como comprobante de tu solicitud.

## 5. Kardex y exportar a PDF

1. Entra a **Kardex**.
2. Verás tu carrera, periodo de ingreso, promedio general, promedio del último semestre y el porcentaje de tu carrera que ya cubriste.
3. Desplázate para ver el detalle materia por materia (aprobadas, reprobadas y por cursar).
4. Toca el botón de exportar para generar un **PDF** de tu kardex, listo para compartir o imprimir.

## 6. Recuperar tu contraseña

Esta función está en desarrollo. Mientras tanto, si olvidaste tu contraseña, contacta al área de control escolar para que un administrador verifique o actualice tu perfil.

## 7. Preguntas frecuentes

**¿Por qué no me llega el código OTP?**
Revisa la carpeta de spam. Si sigue sin llegar, toca "Reenviar código"; cada intento genera un código nuevo y reemplaza al anterior.

**¿Puedo usar la app sin internet?**
No — todas las operaciones (login, consulta de módulos) requieren conexión.

**¿Qué pasa si mi matrícula ya existe pero nunca me registré?**
Es normal: control escolar puede haber precargado tu perfil. Al registrarte con tu contraseña, completas ese mismo perfil — no se duplica.
