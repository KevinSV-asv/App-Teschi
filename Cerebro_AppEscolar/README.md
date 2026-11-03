# Cerebro AppEscolar

Bóveda de Obsidian con toda la documentación y el backend de **AppTESCHI**, la app Android para alumnos del TESCHI. Vive dentro del repositorio [App-Teschi](https://github.com/KevinSV-asv/App-Teschi), junto al código de la app.

> **Desde el 12/10/2026 (DEC-043)** la app habla directo con la API oficial del SIIA. El backend (`07_BACKEND`) y la base (`04_BASE_DATOS`) se conservan solo como **histórico**: la app ya no los usa.

## Cómo abrirla

1. Clona el repositorio (o usa tu copia en `AndroidStudioProjects/AppTeschi`).
2. En Obsidian: *Abrir carpeta como bóveda* → elige la carpeta `Cerebro_AppEscolar`.
3. Empieza por `00_SISTEMA/MAPA_PROYECTO.md`.

> Esta es la **única** bóveda. La copia vieja en `Descargas/Cerebro_AppEscolar` quedó como respaldo del 25/09/2026 y ya no se actualiza.

## Contenido

| Carpeta | Qué hay |
|---|---|
| `00_SISTEMA` | Mapa del proyecto y reglas |
| `01_APIS_POSTMAN` | API oficial del SIIA y guía de Postman |
| `02_MODULOS`, `03_SEGURIDAD` | Módulos de la app, autenticación, privacidad |
| `04_BASE_DATOS` | Histórico: modelo de datos y scripts SQL de `AppTeschiDB` |
| `05_AVANCES`, `06_BITACORA_TECNICA` | Bitácora por sesión, decisiones (DEC-xxx), bugs y cambios |
| `07_BACKEND` | Histórico: arquitectura y código del backend (`AppTeschi.Api`: Node + Express + SQL Server) |
| `08_REQUERIMIENTOS` a `12_GLOSARIO` | Requerimientos, análisis y diseño, manuales, pruebas y glosario |
| `Documentacion_Word` | Versión `.docx` de la documentación formal (al 12/09/2026) |

## Levantar el backend en local (histórico, la app ya no lo usa)

```bash
cd 07_BACKEND/AppTeschi.Api
cp .env.example .env      # completa tus valores; el .env nunca se sube
npm ci
npm start
```

El backend real corre en el servidor Ubuntu: ver `10_MANUALES/03_Manual_Tecnico_Instalacion.md` §8. Para Postman: `01_APIS_POSTMAN/01_POSTMAN_API.md`.

## Reglas para colaborar

- **Nunca** subas `.env`, tokens, contraseñas ni datos reales de alumnos. Usa datos ficticios (`2024PRUEBA1`, `alumno@teschi.edu.mx`).
- Los cambios relevantes se registran en `06_BITACORA_TECNICA` y en una nota de `05_AVANCES`.
