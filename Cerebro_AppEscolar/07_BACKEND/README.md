# Backend y sincronización

## Propósito

Este módulo crea la capa de backend API para AppTeschi con el objetivo de:

- recibir usuarios pendientes desde Android
- validar autenticación por API key
- escribir en `AppTeschiDB`
- servir datos de consulta para la app

## Estructura

- `AppTeschi.Api/` — API Express para sincronización
- `AppTeschi.Api/server.js` — servidor principal
- `AppTeschi.Api/.env.example` — configuración de entorno

## Reglas de decisión

- La app Android no debe conectarse directamente a SQL Server.
- La app solo debe guardar copias locales y disparar sincronización.
- El backend es responsable de la escritura final en base de datos.
- Todo cambio relevante debe registrarse en la bóveda bajo `06_BITACORA_TECNICA`.
