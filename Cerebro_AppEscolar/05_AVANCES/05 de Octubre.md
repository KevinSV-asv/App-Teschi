# Bitácora de Avances — 05 de Octubre de 2026

- **Relacionado con:** [[MAPA_PROYECTO]], [[03_Calificaciones]], [[API_SIIA_Oficial]], [[01_POSTMAN_API]], [[CAMBIOS_DE_CODIGO]] (v0.34), [[BUGS_Y_ERRORES]] (BUG-024)

---

## Completado

- [x] **BUG-024 resuelto por la escuela:** el endpoint de calificaciones del SIIA ya responde 200 con datos (probado en Postman con `periodo=2025-2`).
- [x] `server.js` ajustado a la respuesta real (no exige `ok`; respeta `bloqueado` con 200 o 403). Prueba con SIIA simulado igual al real: 24/24; NIP 27/27.
- [x] Colección de Postman con el flujo actual (login → NIP → calificaciones) y el nuevo `login-nip.ashx` del SIIA.

---

## Pendiente

- [ ] Desplegar `server.js` y ver Calificaciones en el teléfono con una cuenta del SIIA.
- [ ] Decidir si se usa `login-nip.ashx` (entrar solo con matrícula + NIP, sin contraseña).
