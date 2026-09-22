---
name: actualizar-cerebro
description: |
  Al finalizar cualquier sesión de trabajo en AppTESCHI, actualiza automáticamente
  la bóveda Cerebro_AppEscolar: registra los archivos modificados en la bitácora de
  avances, actualiza el estado de los módulos en MAPA_PROYECTO.md y documenta
  cualquier decisión técnica nueva en la nota correspondiente.
  Úsalo cuando el usuario diga "actualiza el cerebro", "registra los cambios" o al
  cerrar una sesión de trabajo relevante.
license: MIT
metadata:
  kiro-enabled-agents: kiro
---

# Skill: Actualizar Cerebro AppTESCHI

Ejecuta estos pasos en orden al finalizar una sesión de trabajo o cuando el usuario
pida actualizar la bóveda.

## Rutas clave

| Elemento              | Ruta                                                                         |
|-----------------------|------------------------------------------------------------------------------|
| Raíz de la bóveda     | `c:\Users\kevin\Downloads\Cerebro_AppEscolar\Cerebro_AppEscolar\`           |
| Mapa del proyecto     | `00_SISTEMA\MAPA_PROYECTO.md`                                               |
| Reglas técnicas       | `00_SISTEMA\REGLAS_PROYECTO.md`                                             |
| Carpeta de avances    | `05_AVANCES\`                                                               |
| Carpeta de módulos    | `02_MODULOS\`                                                               |
| Seguridad             | `03_SEGURIDAD\`                                                             |
| APIs / Scraping       | `01_APIS_POSTMAN\`                                                          |

## Paso 1 — Crear o actualizar la nota de bitácora del día

1. Determinar la fecha actual (`{DD} de {Mes} de {YYYY}`).
2. Verificar si existe `05_AVANCES\{DD} de {Mes}.md`.
   - Si **no existe**: crearlo con la plantilla de abajo.
   - Si **ya existe**: agregar una nueva sección `## Sesión {HH:MM}` al final.

### Plantilla de bitácora

```markdown
# Bitácora de Avances — {DD} de {Mes} de {YYYY}

- **Relacionado con:** [[MAPA_PROYECTO]]

---

## Completado en Esta Sesión

{lista de tareas completadas — usar [ x ] con descripción concisa}

---

## Decisiones Técnicas Registradas

{lista de decisiones o cambios de arquitectura tomados en la sesión}

---

## Pendiente para Próxima Sesión

{lista de tareas abiertas o TODOs detectados en el código}
```

## Paso 2 — Actualizar el estado de los módulos en MAPA_PROYECTO.md

1. Leer el archivo `00_SISTEMA\MAPA_PROYECTO.md`.
2. En la tabla de módulos, actualizar la columna **Estado** de cualquier módulo
   que haya cambiado durante la sesión. Los estados válidos son:

   | Estado        | Significado                                        |
   |---------------|----------------------------------------------------|
   | `En diseño`   | Solo documentación, sin código funcional           |
   | `En desarrollo` | ViewModel o pantalla iniciados                   |
   | `Implementado (mock)` | UI completa, datos locales/simulados       |
   | `Integrado`   | Conectado al SIIA / endpoint real funcionando      |
   | `Completo`    | Probado, sin TODOs abiertos                        |

3. Actualizar también la columna de estado del módulo de **Autenticación** si
   aplica.

## Paso 3 — Propagar decisiones técnicas a las notas de módulo

Si durante la sesión se tomó una decisión que afecta a un módulo específico
(ej. cambio de endpoint, nuevo campo, regla de negocio confirmada):

1. Localizar la nota correspondiente en `02_MODULOS\`.
2. Agregar la información bajo la sección existente más relevante
   (`## Endpoint API`, `## Reglas de Negocio`, `## Notas de Implementación`).
3. Nunca borrar información anterior; solo complementar o corregir.

## Paso 4 — Registrar cambios de arquitectura en REGLAS_PROYECTO.md

Si cambia el tech stack, se agrega una dependencia, se modifica la paleta o se
establece una convención nueva:

1. Actualizar la tabla correspondiente en `00_SISTEMA\REGLAS_PROYECTO.md`.
2. Añadir la entrada a la sección correcta; no duplicar filas existentes.

## Reglas de escritura

- Usar **WikiLinks** `[[NombreNota]]` para todas las referencias internas.
- Nunca inventar datos de negocio, endpoints ni valores del SIIA.
- Mantener la jerarquía de encabezados: `#` título, `##` sección, `###` subsección.
- Las tablas deben mantenerse alineadas y con encabezado en negrita.
- Los estados pendientes se marcan con `⚠️ Pendiente` o `TODO:`.
- Las tareas completadas usan `✅` o `- [x]`.
