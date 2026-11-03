# Bitácora de Avances — 02 de Noviembre de 2026

- **Relacionado con:** [[MAPA_PROYECTO]], [[DECISIONES_TECNICAS]] (DEC-044 a DEC-048), [[CAMBIOS_DE_CODIGO]] (v0.36 a v0.39), [[API_SIIA_Oficial]], [[01_POSTMAN_API]], [[10_Kardex]], [[03_Calificaciones]]

---

## Completado

- [x] **Análisis de la API 1.2 del SIIA** (especificación del 29/10/2026). Nuevos para el alumno: `calificaciones/finales.ashx`, `calificaciones/kardex.ashx` y `calificaciones/kardex-pdf.ashx`. Los tres existen en el servidor real (401 sin token).
- [x] **Colección de Postman** `01_APIS_POSTMAN/SIIA_Alumno.postman_collection.json`: el flujo de la app, con el token automático y verificaciones por petición. Probada con Newman contra un SIIA falso local (26/26).
- [x] **Calificaciones > Finales:** selector Parciales / Finales, 1ra y 2da vuelta, calificación final, promedio y dictamen.
- [x] **Kardex real** con el diseño de Stitch: alumno, promedio global, créditos, contadores, gráfica, filtro por semestre, oportunidades de cada materia y simbología.
- [x] **Descargar PDF:** el alumno elige dónde guardarlo y puede abrirlo al terminar.
- [x] Pruebas: **134/134**, y recorrido en emulador contra un SIIA falso con datos ficticios (login, Finales, Kardex, PDF, bloqueo, error y sesión vencida). App **2.1**.
- [x] Bóveda: DEC-044, v0.36, [[API_SIIA_Oficial]], [[01_POSTMAN_API]], [[REGLAS_PROYECTO]], [[MAPA_PROYECTO]], [[03_Calificaciones]], [[10_Kardex]], [[07_Prompt_Stitch_Kardex]].

- [x] **Kardex con los mismos datos que el original** (DEC-045), a pedido del alumno: encabezado institucional, fecha de elaboración, CURP, grupo, reprobadas, por aprobar, total de materias, créditos por cursar, promedio del semestre anterior y clave oficial de cada materia. Mismo diseño. **138/138** pruebas. App **2.2**.

- [x] **Pestaña Reinscripción** en lugar de Intersemestral, con Intersemestral dentro (DEC-046). Ambas "en construcción": la API no tiene datos.
- [x] **Seguridad de la Cuenta** en Mi perfil, con el diseño del portal (DEC-047). El NIP se cambia en la app, autorizado con la contraseña. La contraseña sigue en el portal: la API no lo permite. **142/142** pruebas. App **2.3**.

- [x] **Foto de perfil** (DEC-048): se elige de la galería y solo se guarda en el teléfono, porque la API no permite subirla. Se quitó la insignia "SIIA · Sesión activa". **155/155** pruebas. App **2.4**.

- [x] **Publicación en `main` para el profesor:** README principal; se quitaron del repositorio el enlace roto `App Teschi` y `instalar-skills-claude-code.ps1` (sigue en la PC); la matrícula de ejemplo de dos Word se cambió por la ficticia. Se borraron las ramas de trabajo.

## Sin cambios, a propósito

- **Login:** sigue con contraseña + NIP (DEC-043). `login-nip.ashx` no se usa.
- **Asistencia, `GET nip.ashx` y los alias de kardex:** no se integran; la app no los necesita.

---

## Pendiente

- [ ] Decidir si se limpia el historial público: versiones anteriores de dos Word (`12 - Documentacion de Base de Datos`, `19 - Glosario`) conservan una matrícula real usada como ejemplo. Reescribirlo requiere `git push --force`: es decisión del dueño.
- [ ] Correr la colección de Postman con una cuenta real y revisar **Test Results**: la forma real de las respuestas aún no se ha visto.
- [ ] Pedir a la escuela endpoints para **cambiar la contraseña**, reinscripción e intersemestrales.
- [ ] Probar el cambio de NIP con una cuenta real (comprobar que `login.ashx` no invalide la sesión).
- [ ] Instalar el APK 2.4 y comparar con una cuenta real contra el PDF completo: solo se vio el encabezado del reporte oficial. Probar también Finales de un periodo cerrado y la descarga del PDF.
- [ ] Apagar el servidor de casa si ya no se usa para nada más.
- [ ] Actualizar la documentación formal y `Documentacion_Word/` a la arquitectura nueva antes de entregarla.
