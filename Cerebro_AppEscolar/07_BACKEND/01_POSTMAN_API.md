# Guía de Postman para AppTeschi API

## Objetivo

Esta guía explica cómo crear y probar la API en Postman sin confusiones. La intención es que cualquiera del equipo pueda levantar la API, enviar datos y verificar que están llegando a la base de datos correctamente.

## 1. Prerrequisitos

- Tener Node.js instalado.
- Tener la API levantada localmente.
- Tener SQL Server Express con la base AppTeschiDB creada.
- Tener Postman instalado.

## 2. Levantar la API

Desde la carpeta del backend:

```bash
cd C:\Users\kevin\Downloads\Cerebro_AppEscolar\Cerebro_AppEscolar\07_BACKEND\AppTeschi.Api
npm install
npm start
```

Si todo funciona, la consola debe mostrar algo como:

```text
API AppTeschi escuchando en http://localhost:4000
```

## 3. Configurar la colección en Postman

### Importar el entorno recomendado

Importa también `07_BACKEND/AppTeschi.Api/Postman_Environment_AppTeschi.json` desde **Import** y selecciona el entorno **AppTeschi Local** en la esquina superior derecha. Debe mostrar:

```text
base_url = http://localhost:4000
api_key = <API_KEY>
```

Si existe otro entorno activo con una variable `api_key`, desactívalo: Postman puede sobrescribir el valor de la colección y provocar `API key inválida`.

### Crear una nueva colección

1. Abrir Postman.
2. Dar clic en "New Collection".
3. Poner el nombre: AppTeschi API.
4. Guardarla como colección local del proyecto.

### Crear variables de entorno

En la colección o entorno, crear estas variables:

- base_url = http://localhost:4000
- api_key = <API_KEY>

## 4. Crear las peticiones

### A. HEALTH CHECK

- Método: GET
- URL: {{base_url}}/health
- Headers: no requiere

Resultado esperado:

```json
{
  "ok": true,
  "database": "AppTeschiDB"
}
```

### B. SINCRONIZAR USUARIOS PENDIENTES

- Método: POST
- URL: {{base_url}}/api/usuarios/sync
- Headers:
  - Content-Type: application/json
  - x-api-key: {{api_key}}

Cuerpo JSON:

```json
[
  {
    "username": "20240001",
    "name": "José García López",
    "correo": "alumno@gmail.com",
    "correoInstitucional": "20240001@teschi.edu.mx",
    "carrera": "Ingeniería en Sistemas",
    "role": "ALUMNO"
  },
  {
    "username": "admin",
    "name": "Administrador local",
    "correo": "admin@gmail.com",
    "correoInstitucional": "admin@teschi.edu.mx",
    "carrera": "Administración",
    "role": "ADMINISTRADOR"
  }
]
```

Resultado esperado:

```json
{
  "ok": true,
  "inserted": 2
}
```

### C. CONSULTAR USUARIOS

- Método: GET
- URL: {{base_url}}/api/usuarios
- Headers:
  - x-api-key: {{api_key}}

Resultado esperado:

```json
{
  "ok": true,
  "data": [
    {
      "IdUsuario": 1,
      "Matricula": "20240001",
      "NombreCompleto": "José García López",
      "Carrera": "ALUMNO",
      "Activo": true
    }
  ]
}
```

## 5. Validación en SQL Server

### D. CREAR CUENTA APP TESCHI

- Método: POST
- URL: `{{base_url}}/api/registro`
- Headers: `Content-Type: application/json`
- Body: matrícula, nombres, apellidos, fecha, sistema, carrera, equivalencias y contraseña.

### E. INICIAR SESIÓN APP TESCHI

- Método: POST
- URL: `{{base_url}}/api/auth/cuenta`
- Headers: `Content-Type: application/json`
- Body:

```json
{
  "matricula": "20240060",
  "password": "Registro2026"
}
```

Respuesta correcta:

```json
{
  "ok": true,
  "matricula": "20240060",
  "nombre": "Ana Maria Lopez Garcia",
  "esAdministrador": false
}
```

Ejecutar esta consulta:

```sql
USE AppTeschiDB;
SELECT * FROM dbo.Usuarios;
```

Si la petición fue exitosa, los registros deben aparecer ahí.

## 6. Recomendaciones de uso

- Mantener una colección separada para pruebas y otra para producción.
- Guardar siempre la API key como variable de entorno.
- Validar respuesta JSON antes de continuar.
- En cada registro son obligatorios matrícula, nombre, correo, correo institucional y carrera.
- Registrar errores en la bóveda con la fecha y el motivo.

## 7. Importante

No se debe enviar desde la app a SQL Server de forma directa. La app debe enviar la información a esta API, y la API debe ser la encargada de guardar los datos en la base.

## Archivo relacionado

- [07_BACKEND/AppTeschi.Api/Postman_Collection_AppTeschi.json](07_BACKEND/AppTeschi.Api/Postman_Collection_AppTeschi.json)
