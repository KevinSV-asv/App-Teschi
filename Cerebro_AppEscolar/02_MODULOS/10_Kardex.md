# Módulo 10: Kardex

- **Estado:** ✅ Historial académico del SIIA con los mismos datos que el reporte oficial, y PDF oficial, directo desde la app (DEC-044, DEC-045, 02/11/2026). ⏳ Falta comparar con una cuenta real contra el PDF completo.
- **Ruta en App:** `Routes.KARDEX`
- **Relacionado con:** [[MAPA_PROYECTO]], [[API_SIIA_Oficial]], [[03_Calificaciones]], [[07_Prompt_Stitch_Kardex]], [[DECISIONES_TECNICAS]] (DEC-044, DEC-045)

---

## Qué ve el alumno

`GET calificaciones/kardex.ashx` con `Authorization: Bearer <token>`, con el diseño de Stitch:

- **Encabezado institucional:** logos, TESCHI, Organismo, Dirección Académica, Departamento de Control Escolar, "Historial académico" y fecha de elaboración (el día de la consulta).
- **Alumno:** nombre, matrícula, CURP, situación, carrera, especialidad, plan de estudios, período de ingreso, último período y grupo.
- **Indicadores:** promedio global y anillo de créditos cursados contra los de la carrera.
- **Contadores:** aprobadas, reprobadas, por aprobar y por cursar.
- **Resumen:** total de materias, créditos por cursar y promedio del semestre anterior; parcial y descuento si el SIIA los da.
- **Gráfica:** promedio por semestre, de 0 a 100. Tocar una barra filtra la lista por ese semestre.
- **Filtro:** Todos o un semestre.
- **Materias por semestre:** número, clave, créditos, calificación definitiva, clave de cursada (AP / NA / PC) y cada oportunidad (Ordinario, Recurse, Especial, Global) con su periodo.
- **Simbología** del SIIA y la nota "Esta vista es informativa. El documento oficial es el PDF."

## Descargar PDF

1. El botón pide `GET calificaciones/kardex-pdf.ashx?base64=1`, con el token en el encabezado.
2. Se valida que sea un PDF (`%PDF`).
3. Android abre su selector para que el alumno elija dónde guardarlo; no hace falta permiso de almacenamiento.
4. Al terminar sale "Kardex guardado" con la acción **Abrir**.

## Privacidad

- La CURP se muestra porque está en el original (DEC-045). La foto que puede mandar el SIIA no se usa.
- Nada del kardex se guarda en el teléfono: vive en memoria mientras se ve la pantalla, salvo el PDF donde el alumno decida.
- La app no manda `matricula`: el SIIA toma al alumno del token.

## Casos especiales

| Caso | Qué hace la app |
|---|---|
| `bloqueado: true` (403 o 200) | Aviso "Kardex bloqueado en el SIIA" con los trámites pendientes; sin botón de PDF |
| 401 | Token vencido: se borra la sesión y la app manda al login |
| 404 / 429 | El mensaje del SIIA, o uno propio si no trae |
| Error del SIIA o HTML | "El SIIA no pudo entregar tu kardex. Intenta más tarde." con **Reintentar** |
| El PDF no es válido | "El SIIA no entregó un PDF válido. Intenta más tarde." |
| El alumno cancela el selector | No pasa nada |
| Sin app para abrir PDF | Aviso; el archivo ya quedó guardado |

## Archivos

| Capa | Archivo |
|---|---|
| UI | `ui/modules/KardexScreen.kt` |
| Estado | `viewmodel/KardexViewModel.kt` (`filtrarSemestre`, `descargarPdf`, `guardarPdf`, `reintentar`) |
| Datos | `service/KardexSiiaService.kt` (`KardexSiiaApi`, `parsearKardex`, `interpretarKardexPdf`) |
| Pruebas | `KardexSiiaHttpTest`, `KardexSiiaParserTest`, `KardexViewModelTest` |
