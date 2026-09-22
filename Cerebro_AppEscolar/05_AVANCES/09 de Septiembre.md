# Bitácora de Avances — 09 de Septiembre de 2026

- **Relacionado con:** [[MAPA_PROYECTO]]

---

## Completado en Esta Sesión

- [x] **Panel de administrador completo (DEC-017):** CRUD real de alumnos (alta, edición, baja, eliminación), calificaciones por administrador sobre `HistorialAcademico`, auditoría enlazada al alumno afectado (`IdAlumno` antes existía en el esquema pero nunca se llenaba), estadísticas agregadas. Se retiró el CRUD local (`AdminDirectory`, `PendingUserSync`) que nunca tocaba SQL Server de verdad.
- [x] **Ajuste de perfil (DEC-018):** se quitó "Carga académica esperada" del perfil del alumno y se reemplazó por el historial real de materias con calificaciones; el semestre ahora solo avanza (validado en app y en servidor); se agregó una sección de baja/reactivación independiente de la eliminación permanente.
- [x] **5 alumnos de prueba sembrados** (`2024PRUEBA1`-`5`) con calificaciones realistas, para explorar el panel sin usar datos de alumnos reales.
- [x] **Documentación de nivel empresarial construida desde cero** — a petición explícita del usuario ("englobar absolutamente todo lo que conlleva una buena documentación de un proyecto de programación"):
  - `08_REQUERIMIENTOS/`: Visión y alcance, Requerimientos Funcionales (RF-001→RF-062), Requerimientos No Funcionales (por categoría ISO 25010), Matriz de trazabilidad.
  - `09_ANALISIS_Y_DISENO/`: Casos de uso (con diagrama), Diagrama de clases (dominio admin + dominio de autenticación), 5 Diagramas de secuencia, Diagrama de componentes, Diagrama de despliegue, Diagramas de estados y de actividades.
  - `10_MANUALES/`: Manual de usuario (alumno), Manual de usuario (administrador), Manual técnico de instalación, Manual de mantenimiento.
  - `11_PRUEBAS/`: Plan de pruebas, Catálogo de casos de prueba (CP-01→CP-31).
  - `12_GLOSARIO/`: Glosario de términos de negocio y técnicos.
  - `02_MODULOS/06_PanelAdministrador.md` nuevo, para que el panel de administrador quede documentado con el mismo formato que los módulos del alumno.
  - `07_BACKEND/00_ARQUITECTURA_API.md` **reescrito**: describía una arquitectura de cola offline que ya no existe desde DEC-017.
  - `MAPA_PROYECTO.md` y `00_Indice_Boveda.md` actualizados con todos los enlaces nuevos y el estado real (varios módulos del alumno estaban marcados como "placeholder" cuando en realidad ya tienen UI funcional con datos simulados).

---

## Hallazgo relevante durante la documentación

Al escribir los diagramas de secuencia de login se confirmó que la autenticación real es una **cascada de 3 fuentes** (bypass admin de desarrollo → cuenta propia en `AlumnoCredenciales` → SIIA por scraping), no solo SIIA como describía `2FA_Login.md` hasta ahora. Documentado en [[02_Diagrama_Clases]] y [[03_Diagramas_Secuencia]].

---

## Pendiente para próxima sesión

- [ ] Actualizar `03_SEGURIDAD/2FA_Login.md` para reflejar la cascada de autenticación completa (hoy solo describe la rama SIIA).
- [ ] Evaluar automatizar las pruebas de backend (Jest + `supertest`) — hoy toda la verificación de `server.js` es manual con `curl` (ver [[01_Plan_de_Pruebas]] §6).
- [ ] Seguir iterando el panel de administrador según retroalimentación del usuario.
