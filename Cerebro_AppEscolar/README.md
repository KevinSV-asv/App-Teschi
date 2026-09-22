# Cerebro AppEscolar

Bóveda de Obsidian con toda la documentación y el backend de **AppTESCHI**, la app
Android para alumnos del TESCHI. La app vive en su propio repositorio
([App-Teschi](https://github.com/KevinSV-asv/App-Teschi)).

## Cómo abrirla

1. Clona el repositorio.
2. En Obsidian: *Abrir carpeta como bóveda* y elige la carpeta clonada.
3. Empieza por `00_SISTEMA/MAPA_PROYECTO.md`, que enlaza todas las notas.

## Contenido

| Carpeta | Qué hay |
|---|---|
| `00_SISTEMA` | Mapa del proyecto, reglas y prompts |
| `01_APIS_POSTMAN`, `07_BACKEND` | Documentación de la API y el código del backend (Node + Express + SQL Server) |
| `02_MODULOS`, `03_SEGURIDAD` | Módulos de la app y diseño de autenticación |
| `04_BASE_DATOS` | Modelo de datos y scripts SQL (ejecutar en orden numérico) |
| `05_AVANCES`, `06_BITACORA_TECNICA` | Bitácora de trabajo, decisiones técnicas (DEC-xxx) y bugs |
| `08_REQUERIMIENTOS` a `12_GLOSARIO` | Requerimientos, análisis y diseño, manuales, pruebas y glosario |
| `Documentacion_Word` | Versión .docx de la documentación formal |

## Levantar el backend

```bash
cd 07_BACKEND/AppTeschi.Api
cp .env.example .env      # completa tus propios valores; el .env nunca se sube
npm ci
npm start
```

Detalles en `10_MANUALES/03_Manual_Tecnico_Instalacion.md`. Para Postman, importa la
colección y copia `Postman_Environment_AppTeschi.example.json` a
`Postman_Environment_AppTeschi.json` con tu API key.

## Reglas para colaborar

- **Nunca** subas `.env`, tokens, contraseñas ni datos reales de alumnos. Usa datos
  ficticios (`2024PRUEBA1`, `alumno@teschi.edu.mx`).
- Los cambios relevantes se registran en `06_BITACORA_TECNICA`.
