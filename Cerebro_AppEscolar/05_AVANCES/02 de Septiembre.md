# Avance del 02 de Septiembre de 2026

## Corrección de interfaz

Se mejoró la claridad del formulario de registro de usuarios en la interfaz de administrador.

## Búsqueda y registro completo

- El directorio se mantiene separado del formulario de registro para evitar saturar la pantalla.
- Se añadió búsqueda por matrícula, nombre, correo, correo institucional o carrera.
- El formulario solicita matrícula, nombre completo, correo, correo institucional y carrera, además del tipo de usuario.
- Los campos requeridos muestran un mensaje cuando falta información.

### Cambios realizados

- Se añadió el título `Tipo de usuario`.
- Se mostraron las etiquetas `Alumno` y `Administrador` junto a sus respectivos botones de radio.
- Cada opción conserva su comportamiento de selección de rol.
- En el login, el campo mantiene únicamente la etiqueta `Matrícula`.
- Se eliminó del placeholder el texto `o admin`; ahora solo muestra `Ej. 202400123`.

## Corrección de persistencia y auditoría

- Se corrigió el color de texto de los campos `Usuario o matrícula` y `Nombre completo`; ahora el texto escrito se muestra en color oscuro.
- Se corrigió la distribución del selector de rol para que `Administrador` permanezca en una sola línea.
- Se reemplazó la sincronización simulada por una petición HTTP real a `POST /api/usuarios/sync`.
- El payload ahora incluye `correo`, `correoInstitucional` y `carrera`; la API los escribe en `CorreoOtp`, `CorreoInstitucional` y `Carrera`.
- La cola conserva los registros cuando la API falla y los elimina únicamente después de recibir una respuesta HTTP exitosa.
- La sincronización se inicia inmediatamente después de registrar un usuario cuando existe conexión.
- La petición se ejecuta en segundo plano para que la interfaz no se congele esperando a SQL Server.
- El registro exitoso genera el evento de auditoría `Usuario registrado` con el rol y usuario capturados.

## Validación

Se ejecutó `:app:compileDebugKotlin` en el proyecto Android y terminó con `BUILD SUCCESSFUL`.

El único aviso mostrado corresponde a una API de botones obsoleta en otra sección de `LoginScreen.kt`; no está relacionado con esta corrección.

La API de backend debe estar activa en el equipo de desarrollo. En el emulador Android, la dirección configurada es `http://10.0.2.2:4000` para acceder al servidor local del equipo.

Para el teléfono Android utilizado en las pruebas, la app quedó apuntando a `http://192.168.0.41:4000`, que corresponde a la dirección Wi-Fi actual del equipo de desarrollo. El teléfono y el equipo deben estar en la misma red. Windows no permitió crear automáticamente la regla del Firewall sin permisos elevados; si el teléfono no conecta, se debe permitir manualmente el puerto TCP `4000` en el perfil de red privada.

También se verificó nuevamente la persistencia real desde Postman/API: el usuario `20240002` con nombre `Prueba Persistencia` quedó almacenado en `AppTeschiDB.dbo.Usuarios`.

La prueba final completa almacenó `20240003`, `Alumno Completo`, `alumno@gmail.com`, `20240003@teschi.edu.mx` e `Ingenieria en Sistemas` en sus respectivas columnas.

## Edición y consulta detallada

- Al pulsar un registro del directorio se muestran todos sus datos: ID, matrícula, nombre, correo, correo institucional, carrera, tipo de usuario y estado.
- El botón `Editar` carga todos esos datos en el formulario, no únicamente matrícula y nombre.
- Guardar una edición vuelve a utilizar el endpoint de sincronización y actualiza los datos en SQL Server.
- Se verificó la edición del registro `20240003`: se actualizaron nombre, correo y carrera y los nuevos valores fueron confirmados en `dbo.Usuarios`.

## Persistencia del directorio local

- Se corrigió el origen de los datos vacíos: el directorio se reiniciaba desde una lista en memoria cada vez que se abría la aplicación.
- `AdminDirectory` ahora guarda y restaura matrícula, nombre, correo, correo institucional, carrera, tipo y estado mediante almacenamiento local de Android.
- Los nuevos registros y las ediciones quedan disponibles después de cerrar y volver a abrir la aplicación.
- Los registros antiguos que fueron creados antes de esta corrección y nunca llegaron a SQL Server no pueden recuperar automáticamente sus correos o carrera, porque esos valores nunca fueron persistidos; deben completarse una vez mediante `Editar`.

## Archivos relacionados

- `app/src/main/java/com/example/appteschi/ui/dashboard/AdminDashboardScreen.kt`
- `app/src/main/java/com/example/appteschi/ui/login/LoginScreen.kt`
- `06_BITACORA_TECNICA/CAMBIOS_DE_CODIGO.md`

## Inicio del módulo de reinscripción

- Se reemplazó el placeholder de `Routes.REINSCRIPCION` por un flujo inicial funcional.
- El flujo muestra estado de carga, elegibilidad, formulario, envío, éxito y error.
- Se agregaron periodo, grupo, turno y selector de comprobante PDF o imagen.
- Se genera un folio local y se registra la solicitud en la auditoría local.
- La API real del módulo queda como siguiente trabajo, porque la documentación todavía marca sus endpoints como tentativos y no existe contrato de servidor para estatus, comprobante y reinscripción.

## Registro de cuenta AppTESCHI

- Se creó el modelo normalizado `CatalogoSistemas`, `CatalogoCarreras` y `CuentasRegistro`.
- Se registraron las carreras conocidas sin asignar modalidad hasta contar con la confirmación institucional.
- Se agregó `POST /api/registro` con validación de campos, matrícula única, correo institucional generado por servidor y contraseña protegida con `scrypt`.
- Se agregó `GET /api/catalogos/registro` para consultar sistemas y carreras.
- La ruta `Routes.REGISTRO` dejó de ser un placeholder y ahora utiliza `RegistroScreen` y `RegistroViewModel`.
- La prueba real creó la cuenta `20240060` en SQL Server; el hash guardado mide 64 bytes y el salt 16 bytes.
- La recuperación de contraseña para estas cuentas queda pendiente y no se habilita todavía.

## Ajuste final y acceso de cuenta

- Los campos de registro usan fondo blanco y texto negro explícitos.
- Los sistemas se muestran como `Sistema Escolarizado`, `Sistema Abierto` y `Modelo Dual`.
- Las nueve carreras de la referencia se muestran con su nombre completo, incluidas las dos modalidades a distancia.
- La cuenta `20240060` fue autenticada correctamente mediante la API y devolvió su nombre completo.
- Se generó el APK actualizado tras la integración.

## Auditoria de movimientos y perfil

- La API registra en terminal el inicio, cada matricula procesada, resultado exitoso y errores de sincronizacion.
- Cada movimiento incluye el usuario activo mediante `x-actor-matricula` y `x-actor-nombre`.
- Se anadio la tabla `dbo.AuditoriaMovimientos` y el endpoint protegido `GET /api/auditoria`.
- La aplicacion muestra el perfil activo del usuario administrador y usa su identidad en los movimientos.
- La API crea automaticamente la tabla de auditoria si aun no existe, para evitar que una insercion valida falle solo porque falta esa tabla.

## Diagnostico de API key

- El error `API key invalida` se produce antes de acceder a SQL Server cuando el encabezado `x-api-key` no coincide.
- La API fue endurecida para ignorar espacios y forzar la lectura del `.env` actual al iniciar.
- La coleccion fue validada con `api_key = <API_KEY>` y un entorno independiente `AppTeschi Local`.
- Una peticion autorizada inserto `20240031` y genero su movimiento en `dbo.AuditoriaMovimientos`.
- Una clave incorrecta fue rechazada con `401`, confirmando que la proteccion funciona.

## Correccion de validacion al registrar

- El mensaje anterior `Completa matricula, nombre, correo, correo institucional y carrera` era ambiguo: aparecia tanto por campos vacios como por matriculas duplicadas.
- Ahora la aplicacion identifica el motivo exacto: campo faltante, matricula ya registrada o registro que no se encontro al editar.
- Si todos los campos estan llenos y la matricula ya existe, se debe usar `Editar` en ese registro o escribir una matricula nueva.
- La aplicacion Android compilo correctamente despues de esta correccion.

## Diagnóstico de Postman y registros no visibles

- La colección de Postman estaba desactualizada: enviaba únicamente matrícula, nombre y rol.
- La API actual exige matrícula, nombre, correo, correo institucional y carrera; por eso los envíos incompletos no insertaban datos.
- La colección fue actualizada con el payload completo y validada como JSON.
- El payload actualizado respondió `ok: true, inserted: 2`.
- SQL Server confirmó los registros `20240001` y `admin` con sus correos y carreras.
- Para usar Postman Desktop se debe importar `07_BACKEND/AppTeschi.Api/Postman_Collection_AppTeschi.json`, iniciar el backend con `npm start` y ejecutar las peticiones desde la colección.
