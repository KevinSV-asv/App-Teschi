# Consola Administrativa y Auditoria

- **Fecha:** 27/08/2026
- **Estado:** Primera version funcional en memoria
- **Relacionado con:** [[MAPA_PROYECTO]], [[2FA_Login]], [[DECISIONES_TECNICAS]], [[03_Diseno_Login]]

## Vistas

El dashboard ofrece dos vistas visuales:

- **Vista alumno:** bienvenida, modulos escolares y asistente.
- **Vista administrador:** centro de control con indicadores, filtros de riesgo y eventos detallados.

La vista de alumno conserva el flujo de trabajo mostrado en [[00_Dashboard]].

## Acceso administrativo

En esta versión de demostración, las credenciales `admin` / `admin` llevan directamente a la consola sin OTP. La vista se habilita únicamente cuando `UserSession.esAdministrador` es verdadero; no existe un selector visual que pueda usar un alumno.

Estas credenciales no son adecuadas para producción. Deben sustituirse por autenticación del backend, contraseña almacenada con hash fuerte, MFA para administradores y autorización por operación.

## Registro de Eventos

Cada evento contiene:

| Campo | Descripcion |
|---|---|
| Actor | Usuario o sistema que ejecuto la accion |
| Accion | Operacion realizada |
| Detalle | Contexto legible para el administrador |
| Fecha y hora | Momento local del dispositivo |
| Ubicacion | Ultima coordenada disponible, si el permiso existe |
| Riesgo | Normal, Atencion o Critico |

Actualmente se registran intentos de acceso rechazados, credenciales validadas, cierre de sesion y decisiones sobre el permiso de ubicacion. Se conservan como maximo 100 eventos en memoria.

## Deteccion Inicial de Riesgo

- `Normal`: actividad esperada.
- `Atencion`: permisos de ubicacion concedidos o rechazados y otros eventos sensibles.
- `Critico`: intentos rechazados o bloqueos.

Esta clasificacion es una primera regla local, no sustituye un sistema antifraude. La siguiente etapa debe añadir conteo por usuario/IP, ventanas de tiempo, dispositivo, repeticion de fallos y alertas en servidor.

## Ubicacion y Privacidad

La app solicita `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` mediante el permiso nativo de Android. No se solicita en segundo plano. Si el usuario rechaza el permiso, la auditoria funciona sin coordenadas y registra esa decision.

La ubicacion debe enviarse al backend institucional solo con base legal, aviso de privacidad, retencion definida y control de acceso. No se debe usar la ubicacion para inferir conductas por si sola.

## Limitacion de Autorizacion

El acceso local de esta primera version es una herramienta de prototipo. El rol administrador debe ser entregado por el backend, asociado a la cuenta y validado en cada operacion sensible antes de una puesta en produccion.
