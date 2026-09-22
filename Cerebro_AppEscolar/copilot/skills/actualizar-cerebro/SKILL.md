---
name: actualizar-cerebro
description: |
  Al finalizar cualquier sesión de trabajo en AppTESCHI, actualiza la bóveda
  Cerebro_AppEscolar: registra archivos modificados en la bitácora de avances,
  actualiza estados de módulos en MAPA_PROYECTO.md, y propaga decisiones
  técnicas a las notas correspondientes. Úsalo al cerrar una sesión de trabajo
  o cuando el usuario pida "actualiza el cerebro" o "registra los cambios".
license: MIT
metadata:
  copilot-enabled-agents: claude, codex, opencode, kiro
  copilot-builtin-version: "1"
---

# Skill: Actualizar Cerebro AppTESCHI

Ejecuta estos pasos en orden al finalizar una sesión de trabajo o cuando el
usuario lo solicite explícitamente.

## Rutas clave de la bóveda

~~~
Raíz:        c:\Users\kevin\Downloads\Cerebro_AppEscolar\Cerebro_AppEscolar\
Mapa:        00_SISTEMA\MAPA_PROYECTO.md
Reglas:      00_SISTEMA\REGLAS_PROYECTO.md
Avances:     05_AVANCES\
Módulos:     02_MODULOS\
Seguridad:   03_SEGURIDAD\
APIs:        01_APIS_POSTMAN\
~~~

## Paso 1 — Bitácora del día

Determinar la fecha actual. Verificar si existe `05_AVANCES\{DD} de {Mes}.md`.

- Si **no existe** → crear con esta plantilla:

~~~markdown
# Bitácora de Avances — {DD} de {Mes} de {YYYY}

- **Relacionado con:** [[MAPA_PROYECTO]]

---

## Completado en Esta Sesión

- [x] {descripción concisa de cada tarea terminada}

---

## Decisiones Técnicas Registradas

- {decisión o cambio de arquitectura tomado}

---

## Pendiente para Próxima Sesión

- [ ] {TODO detectado en código o documentación}
~~~

- Si **ya existe** → agregar sección `## Sesión {HH:MM}` al final del archivo.

## Paso 2 — Actualizar tabla de módulos en MAPA_PROYECTO.md

Editar la columna **Estado** de los módulos que cambiaron. Estados válidos:

| Estado                  | Significado                                  |
|-------------------------|----------------------------------------------|
| `En diseño`             | Solo doc, sin código funcional               |
| `En desarrollo`         | ViewModel o pantalla iniciados               |
| `Implementado (mock)`   | UI completa con datos simulados              |
| `Integrado`             | Conectado al SIIA real                       |
| `Completo`              | Probado y sin TODOs abiertos                 |

## Paso 3 — Propagar decisiones a notas de módulo

Si se confirmó un endpoint, campo, regla de negocio o cambio de flujo:

1. Localizar la nota en `02_MODULOS\`.
2. Agregar bajo la sección correcta (`## Endpoint API`, `## Reglas de Negocio`,
   `## Notas de Implementación`).
3. Nunca borrar contenido previo; solo complementar o corregir.

## Paso 4 — Actualizar REGLAS_PROYECTO.md si cambia el stack

Si se agregó una dependencia, se modificó la paleta o se estableció una
convención nueva, actualizar la tabla correspondiente sin duplicar filas.

## Reglas de escritura

- Referencias internas: WikiLinks `[[NombreNota]]`, nunca rutas de filesystem.
- No inventar datos de negocio, endpoints ni valores del SIIA.
- Jerarquía: `#` título → `##` sección → `###` subsección.
- Pendientes: `⚠️ Pendiente` o `TODO:`. Completados: `✅` o `- [x]`.

