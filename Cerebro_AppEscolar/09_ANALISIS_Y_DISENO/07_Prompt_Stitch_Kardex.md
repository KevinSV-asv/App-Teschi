# Prompt de Stitch: nuevo diseño del Kardex

> **Ya está en la app (02/11/2026, DEC-044, [[10_Kardex]]):** el diseño que generó Stitch con este prompt, guardado en `08_Kardex_Propuesta_Stitch.html` (misma carpeta), se pasó a `ui/modules/KardexScreen.kt` con datos reales de la API 1.2 del SIIA.
>
> Diferencias con el diseño:
> - Las claves de materia (p. ej. `ACF-0901`), el plan `ISIC-2010-224` y los promedios del diseño son **inventados por Stitch**; en la app salen del SIIA.
> - Sin el sello "Documento Oficial Validado": la app no certifica documentos; lo oficial es el PDF.
> - Barra inferior de 5 pestañas, no 6 (DEC-043).
> - La gráfica va de 0 a 100 y usa un solo color, para no exagerar las diferencias entre semestres.
> - Sin campana de notificaciones ni etiqueta "Excelente": en la API no hay datos para ellas.
>
> `08_Kardex_Propuesta.html` es una alternativa hecha a mano antes (Stitch pedía iniciar sesión); el alumno eligió la de Stitch.

- **Fecha:** 11/10/2026
- **Para qué:** el profesor pidió cambiar la presentación del Kardex porque la del SIIA es una vista web (tabla ancha) que no se adapta a la app. Este prompt genera en [Stitch](https://stitch.withgoogle.com/) (modo **App**) una propuesta móvil con el estilo de AppTESCHI.
- **Relacionado con:** [[REGLAS_PROYECTO]] (paleta), [[MAPA_PROYECTO]]
- **Datos:** alumno ficticio (Stitch es un servicio externo; nunca datos reales, DEC-032).

---

## Prompt (copiar tal cual)

```
Pantalla móvil Android "Kardex" (historial académico) para la app universitaria AppTESCHI, en Material Design 3, toda la interfaz en español. Debe verse como el resto de la app, respeta EXACTAMENTE este estilo: fondo general #F4F7F4; tarjetas blancas #FFFFFF con esquinas redondeadas de 12-16dp y sombra suave; color principal verde #1E5631 (barra superior y números destacados); verde secundario #4C9A2A (acentos y pestaña activa); texto principal #1A2E22; texto secundario gris #757575; acento lima #C6F27A solo en detalles pequeños. Estados: Aprobada fondo #E8F5E9 texto #1E5631; Por cursar fondo #F0F0F0 texto #616161; No aprobada fondo #FFEBEE texto #B3261E. Tipografía Roboto, títulos en negrita. Sin modo oscuro, sin degradados morados.

Estructura de arriba abajo:
1) Barra superior verde #1E5631 con flecha de regresar blanca, título "Kardex" blanco y a la derecha un botón tipo píldora blanco con ícono de descarga y texto verde "Descargar PDF".
2) Tarjeta del alumno: "Alumno de Prueba Uno", matrícula 2099000001, "Ingeniería en Sistemas Computacionales", plan ISIC-2010-224, ingreso 2022-2, último periodo 2026-1.
3) Fila de indicadores: tarjeta grande "Promedio global 88.63" en verde #1E5631 muy grande; tarjeta con anillo de progreso "Créditos 250 / 260" y "96.2% de avance".
4) Tres chips contadores: 52 Aprobadas, 1 Por cursar, 0 No aprobadas.
5) Pequeña gráfica de barras verdes "Promedio por semestre" (semestres 1 a 8, valores entre 82 y 95).
6) Filtro horizontal de chips por semestre: Todos, 1, 2, 3 ... 9 (Todos seleccionado en verde).
7) Lista agrupada por semestre con encabezado "Semestre 1 · 6 materias · Promedio 85.3" y tarjetas de materia: nombre de la materia, clave (ej. ACF-0901), créditos, periodo (2022-2), tipo de evaluación (Ordinario / Extraordinario / Equivalencia), calificación grande a la derecha y la etiqueta de estatus de color. Materias de ejemplo: Cálculo Diferencial 85, Fundamentos de Investigación 95, Fundamentos de Programación 93, Matemáticas Discretas 88, Taller de Administración 90, Taller de Ética 92. En semestre 9 mostrar "Residencias Profesionales" como Por cursar sin calificación.
8) Barra de navegación inferior flotante tipo píldora blanca con sombra, 6 pestañas con ícono y texto: Inicio, Reinscrip., Materias, Calif., Intersem., Kardex (Kardex activa en verde y negrita).

Debe ser 100% nativa para celular (nada de tablas anchas tipo página web), fácil de leer y con buena jerarquía visual.
```

## De dónde sale cada valor

| Elemento | Valor | Fuente en la app |
|---|---|---|
| Fondo | `#F4F7F4` | `BackgroundLight` (`ui/theme/Color.kt`) |
| Verde principal | `#1E5631` | `GreenPrimary` (TopAppBar, botones, números) |
| Verde secundario | `#4C9A2A` | `GreenSecondary` (acentos, pestaña activa) |
| Texto | `#1A2E22` / `#757575` | `LoginInk` / `TextMuted` |
| Acento | `#C6F27A` | `LoginAccent` |
| Estados | `#E8F5E9`, `#F0F0F0`, `#FFEBEE` | `SuccessContainer`, `NeutralContainer`, `ErrorContainer` |
| Tipografía | Roboto | `FontFamily.Default` de Android |
| Navegación | Barra flotante tipo píldora, 6 pestañas | `BottomNavBar.kt` (DEC-028) |
| Descarga | Píldora blanca "Descargar PDF" | `KardexScreen.kt` (v0.28) |

## Qué agrega respecto al Kardex actual

- Tarjeta del alumno con plan e ingreso.
- Anillo de avance de créditos en lugar de la barra lineal.
- Gráfica de promedio por semestre.
- Filtro por semestre en chips.
- Encabezado de cada semestre con número de materias y promedio.
- Por materia: clave, periodo y tipo de evaluación, además de calificación y estatus.

> Ojo al pasarlo a código: `dbo.HistorialAcademico` todavía no guarda periodo ni tipo de evaluación por materia (DEC-023). Para mostrarlos hay que agregarlos a la base primero.
