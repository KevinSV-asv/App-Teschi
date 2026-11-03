# Bitácora de Avances — 12 de Octubre de 2026

- **Relacionado con:** [[MAPA_PROYECTO]], [[DECISIONES_TECNICAS]] (DEC-043), [[CAMBIOS_DE_CODIGO]] (v0.35), [[2FA_Login]], [[07_Prompt_Stitch_Kardex]]

---

## Completado

- [x] **Propuesta de Kardex para el profesor.** Prompt de Stitch con la paleta de la app ([[07_Prompt_Stitch_Kardex]]). Stitch pedía iniciar sesión, así que se hizo además una propuesta a mano (`08_Kardex_Propuesta.html`). El alumno eligió el diseño que generó Stitch: quedó en `09_ANALISIS_Y_DISENO/08_Kardex_Propuesta_Stitch.html` (solo datos ficticios).
- [x] **DEC-043 — La app habla directo con el SIIA, sin servidor.** Pedido del alumno: "Ya no quiero que llegue al servidor… elimina cualquier dependencia". Sus elecciones: quitar el registro, quitar de la app los módulos sin datos en el SIIA, contraseña + NIP para entrar, y el Kardex "en construcción".
  - Login: `login.ashx` → `nip.ashx` (verificar / crear / recuperar por CURP). Solo alumnos.
  - Sesión con el token del SIIA en el teléfono (fuera del respaldo); con el token vigente abre en Inicio.
  - Calificaciones parciales, Tira de materias (de las parciales) y resumen del periodo en Inicio, todo directo al SIIA.
  - Perfil: datos del SIIA y cambio de NIP.
  - Kardex e Intersemestral: "Estamos trabajando en este apartado".
  - Quitados: panel de administrador, Reinscripción, Registro, recuperar contraseña, OTP, horarios, usuarios de prueba, diálogo del servidor (55 archivos). Barra de 5 pestañas. App **2.0**.
- [x] Pruebas: **86/86** (incluye SIIA simulado con MockWebServer) y APK de depuración.
- [x] Bóveda: DEC-043 y vigencia de las decisiones que sustituye, v0.35, [[REGLAS_PROYECTO]], notas de módulos, [[API_SIIA_Oficial]]. Las notas del backend, la base, el panel de administrador, Reinscripción, horarios y recuperar contraseña quedaron marcadas como **históricas**; la documentación formal (requerimientos, diseño, manuales, pruebas) quedó marcada como **desactualizada**.

---

## Pendiente

- [ ] Instalar el APK 2.0 y probar con una cuenta real del SIIA (contraseña + NIP, Calificaciones, Materias, cambio de NIP, cerrar sesión).
- [ ] Presentar al profesor la propuesta de Kardex (`08_Kardex_Propuesta_Stitch.html`) y pedir a la escuela un endpoint de kardex con calificaciones finales.
- [ ] Apagar el servidor de casa (`appteschi-api` y SQL Server) si ya no se usa para nada más.
- [ ] Actualizar la documentación formal y `Documentacion_Word/` a la arquitectura nueva antes de entregarla.
- [ ] Opcional: borrar `API_BASE_URL` de `local.properties` (ya no se lee).
