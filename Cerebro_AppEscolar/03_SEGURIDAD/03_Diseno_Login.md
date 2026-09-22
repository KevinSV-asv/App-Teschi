# Diseño de Login Minimalista

- **Fecha:** 27/08/2026
- **Estado:** Implementado
- **Relacionado con:** [[2FA_Login]], [[MAPA_PROYECTO]], [[DECISIONES_TECNICAS]]

## Dirección Visual

La pantalla de acceso usa una composición minimalista y moderna orientada a la lectura rápida:

- Fondo claro `LoginCanvas` para separar la pantalla del contenido del sistema.
- Marca compacta con monograma TESCHI y nombre del portal en la cabecera.
- Una sola tarjeta funcional con bordes suaves y elevación discreta.
- Acento lima `LoginAccent` para dar contraste a la identidad verde institucional.
- Texto principal oscuro `LoginInk` y campos con contenido negro para asegurar legibilidad.
- Registro como acción secundaria debajo de la tarjeta, sin competir con el acceso.

## Componentes

| Componente | Función |
|---|---|
| Cabecera de marca | Identificar AppTESCHI sin ocupar espacio excesivo |
| Tarjeta de autenticación | Mostrar credenciales o verificación 2FA según el estado |
| `LoginField` | Campo reutilizable para matrícula, contraseña, correo y OTP |
| Acción de registro | Navegar a `Routes.REGISTRO` |

## Reglas de Uso

- El rediseño es exclusivamente visual; no cambia el flujo SIIA → OTP → Dashboard.
- La pantalla debe conservar scroll vertical para celulares pequeños.
- Los colores nuevos deben declararse en `ui/theme/Color.kt`.
- No agregar texto de ayuda dentro de la interfaz si el control ya es autoexplicativo.

## Validación

La compilación `:app:compileDebugKotlin` fue exitosa el 27/08/2026. La prueba visual en dispositivo físico queda pendiente antes de iniciar los módulos académicos.
