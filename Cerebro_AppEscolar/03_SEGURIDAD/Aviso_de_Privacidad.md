# Aviso de Privacidad — AppTESCHI

> ⚠️ **Borrador técnico, no un documento legal terminado.** Está redactado con la estructura que exige la Ley Federal de Protección de Datos Personales en Posesión de los Particulares (LFPDPPP) y su Reglamento, a partir de los datos que **realmente** recolecta el sistema (verificado contra el código, no supuesto). Antes de publicarlo o entregarlo a un alumno, debe revisarlo el área jurídica o de protección de datos de la institución — nombra un responsable, domicilio y mecanismos de contacto reales, y confirma que el tratamiento descrito coincide con la política institucional vigente.
> **Relacionado con:** [[Base_Datos_Usuarios]], [[DECISIONES_TECNICAS]] (DEC-014, DEC-019), [[00_Vision_y_Alcance]]

---

## 1. Responsable del tratamiento

**[Nombre de la institución — completar]**, con domicilio en **[domicilio — completar]**, es responsable del tratamiento de los datos personales que se recaban a través de AppTESCHI.

## 2. Datos personales que se recaban

| Categoría | Datos concretos | Tabla / origen |
|---|---|---|
| Identificación | Matrícula, nombres, apellidos, fecha de nacimiento | `Alumnos` |
| Contacto | Correo personal (usado para OTP), correo institucional | `Alumnos` |
| Académicos | Carrera, sistema, semestre, calificaciones, estatus por materia, historial académico completo | `Alumnos`, `PlanEstudioMaterias`, `HistorialAcademico` |
| Acceso a la cuenta | Contraseña (nunca en texto plano — se guarda un hash irreversible con sal, algoritmo scrypt) | `AlumnoCredenciales` |
| Verificación en dos pasos | Código de un solo uso (hash, nunca el código en claro), correo al que se envió, fecha de envío/expiración | `OtpHistorial` |
| Sesión y auditoría | Matrícula, nombre, acción realizada, fecha, y — solo si el usuario otorga el permiso de ubicación del dispositivo — coordenadas GPS | `SesionesLogin`, `AuditoriaMovimientos`, bitácora local del dispositivo |
| Técnicos | Dirección IP (para limitar intentos de acceso, no se almacena de forma permanente) | Memoria del servidor, no persistida |

**No se recaban:** fotografías, datos biométricos, información financiera, datos de salud, ni ningún dato considerado sensible por la LFPDPPP.

## 3. Finalidades del tratamiento

### Finalidades primarias (necesarias para el servicio)

- Identificar al alumno y verificar su identidad al iniciar sesión (incluida la verificación en dos pasos por correo).
- Administrar su expediente académico: carrera, semestre, materias y calificaciones.
- Permitir trámites escolares (reinscripción, consulta de kardex, tira de materias).
- Generar el registro de auditoría necesario para la seguridad del sistema y la rendición de cuentas del personal administrativo.

### Finalidades secundarias (no necesarias para el servicio; requieren consentimiento explícito adicional)

- Ninguna identificada actualmente. El sistema no envía publicidad, no comparte datos con terceros con fines comerciales, y no usa la información con propósitos distintos a los descritos arriba.

## 4. Fundamento y base legal

El tratamiento se realiza con fundamento en la relación educativa entre el alumno y la institución, y es necesario para la prestación del servicio escolar solicitado (LFPDPPP, art. 8 y 9 — excepción de consentimiento cuando el tratamiento es necesario para cumplir obligaciones derivadas de una relación jurídica entre el titular y el responsable).

## 5. Transferencias de datos

- **Correo electrónico:** los códigos de verificación se envían a través de un proveedor de correo (actualmente Gmail/Google Workspace) exclusivamente como medio de entrega del mensaje — Google no recibe ni almacena el historial académico del alumno, solo transporta el correo.
- **No se transfieren datos a ningún otro tercero.** El sistema no comparte información con aseguradoras, empresas de mercadotecnia, ni ningún otro tercero ajeno a la institución.

## 6. Derechos ARCO

El titular de los datos (el alumno) tiene derecho a **A**cceder, **R**ectificar, **C**ancelar u **O**ponerse al tratamiento de sus datos personales, así como a revocar el consentimiento otorgado, en los términos de la LFPDPPP.

Para ejercer estos derechos, el alumno puede:

- Acudir directamente con el área de Control Escolar de la institución.
- **[Completar con el medio de contacto oficial que la institución determine — correo, formulario, oficina física, etc.]**

## 7. Medidas de seguridad implementadas

| Medida | Cómo se implementa |
|---|---|
| Contraseñas | Nunca se almacenan en texto plano — hash scrypt con sal única por cuenta |
| Verificación en dos pasos | Código de un solo uso, vigencia de 10 minutos, máximo 5 intentos, generado y verificado en el servidor (nunca en el dispositivo del usuario) |
| Comunicación | Todo el tráfico entre la app y el servidor viaja cifrado (HTTPS) |
| Acceso administrativo | Requiere cuenta propia con contraseña y verificación en dos pasos — ver DEC-019 |
| Auditoría | Toda alta, baja, edición o consulta relevante de datos de un alumno queda registrada con quién la hizo y cuándo |
| Eliminación de datos | Un administrador puede eliminar permanentemente el expediente de un alumno (borrado real, no solo ocultamiento) cuando así se solicite |

## 8. Conservación de los datos

Los datos académicos se conservan mientras dure la relación del alumno con la institución y durante el plazo adicional que exija la normatividad educativa aplicable. Los códigos de verificación (OTP) se conservan únicamente como bitácora de seguridad; el código en sí nunca es recuperable (se guarda como hash irreversible).

## 9. Cambios a este aviso

Cualquier cambio a este aviso de privacidad se publicará en este mismo documento y, cuando el cambio sea sustancial, se notificará a los alumnos por el medio de contacto registrado.

---

## Pendiente antes de publicar

- [ ] Completar nombre legal y domicilio de la institución.
- [ ] Definir y completar el mecanismo real de ejercicio de derechos ARCO.
- [ ] Revisión por el área jurídica/de protección de datos de la institución.
- [ ] Decidir si se requiere una pantalla de consentimiento explícito al registrarse (checkbox "He leído el aviso de privacidad") — hoy `RegistroScreen` no muestra este aviso.
- [ ] Confirmar el proveedor de correo definitivo (hoy es una cuenta de Gmail, no un correo institucional en Google Workspace) antes de publicar la sección de transferencias.
