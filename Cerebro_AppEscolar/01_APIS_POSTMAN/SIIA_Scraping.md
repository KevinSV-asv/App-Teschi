# SIIA — Estrategia de Acceso y Web Scraping

- **Estado:** 🔴 Histórico — retirado por completo el 15/09/2026 (DEC-026). AppTESCHI ya no scrapea ni depende del SIIA para nada; `SiiaAuthService.kt` se eliminó. Esta nota se conserva como referencia de cómo funcionaba el portal real, no describe nada activo en el código actual.
- **Relacionado con:** [[MAPA_PROYECTO]], [[REGLAS_PROYECTO]], [[2FA_Login]]

---

## Contexto

El portal SIIA de TESCHI **no expone una API REST**. Funciona como una aplicación **ASP.NET Web Forms**.
La app accede a él simulando un navegador: extrae tokens del HTML y envía POSTs de formulario.

---

## URL Base

```
http://148.230.236.166/Teschi/
```

---

## Librerías Utilizadas

| Librería | Versión  | Función                                     |
|----------|----------|---------------------------------------------|
| OkHttp   | 4.12.0   | Cliente HTTP, gestión de cookies, interceptores |
| Jsoup    | 1.18.1   | Parsing del HTML de las páginas ASP.NET     |
| Gson     | (via BOM) | Serialización para mocks de Postman         |

> Añadir a `libs.versions.toml`:
> ```toml
> jsoup = "1.18.1"
> [libraries]
> jsoup = { group = "org.jsoup", name = "jsoup", version.ref = "jsoup" }
> ```

---

## Patrón General de Llamada al SIIA

Cada acción sigue este patrón de 2 pasos:

### Paso 1 — GET para obtener tokens anti-CSRF
```
GET http://148.230.236.166/Teschi/{Pagina}.aspx
```
Parsear con Jsoup:
```kotlin
val doc = Jsoup.parse(responseBody)
val viewState       = doc.select("input[name=__VIEWSTATE]").attr("value")
val eventValidation = doc.select("input[name=__EVENTVALIDATION]").attr("value")
```

### Paso 2 — POST simulando el formulario
```
POST http://148.230.236.166/Teschi/{Pagina}.aspx
Content-Type: application/x-www-form-urlencoded
Cookie: ASP.NET_SessionId={valor_del_cookiejar}

__VIEWSTATE={valor}&__EVENTVALIDATION={valor}&{campos_del_formulario}
```

---

## Páginas del SIIA (a mapear)

| Página                  | URL tentativa                         | Estado         |
|-------------------------|---------------------------------------|----------------|
| Login                   | `/Teschi/Login.aspx`                  | ✅ Confirmado   |
| Dashboard / Home        | `/Teschi/Default.aspx`                | ⚠️ Confirmar   |
| Tira de Materias        | ⚠️ A identificar con DevTools         | Pendiente      |
| Calificaciones          | ⚠️ A identificar con DevTools         | Pendiente      |
| Reinscripción           | ⚠️ A identificar con DevTools         | Pendiente      |
| Intersemestral          | ⚠️ A identificar con DevTools         | Pendiente      |
| Recuperar Contraseña    | ⚠️ A identificar con DevTools         | Pendiente      |

> Para identificar las URLs exactas: abrir el portal en Chrome → DevTools → Network →
> filtrar por `Doc` o `aspx` → realizar la acción en el portal → copiar la URL y el payload.

> ✅ Campos confirmados el 27/08/2026:
> - POST destino: `default.aspx?ReturnUrl=%2fTeschi%2fLogin.aspx`
> - `txtUsuario`, `txtPass`, `btnAceptar=Iniciar Sesión`
> - Error credenciales: `alert('los datos son errores')`

---

## Gestión de Cookies ASP.NET

```kotlin
// CookieJar en memoria (no persiste en disco)
val cookieJar = object : CookieJar {
    private val store = mutableMapOf<String, List<Cookie>>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store[url.host] = cookies
    }
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return store[url.host] ?: emptyList()
    }
}

val client = OkHttpClient.Builder()
    .cookieJar(cookieJar)
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .build()
```

---

## Endpoint de Auditoría (pendiente)

- URL: **pendiente de recibir de TI**
- Método: `POST`
- Payload esperado:
```json
{
  "matricula": "12345678",
  "accion": "LOGIN",
  "latitud": 19.4326,
  "longitud": -99.1332,
  "ip_dispositivo": "192.168.1.10",
  "timestamp": "2024-08-21T10:30:00Z"
}
```
- Este endpoint sí será REST; se integrará con Retrofit cuando TI lo entregue.

---

## Mock en Postman

Crear una colección con las siguientes carpetas:
1. `Auth` — POST Login (simular respuesta de redirección exitosa)
2. `Calificaciones` — GET con array de materias y notas
3. `Tira de Materias` — GET con lista de materias del periodo
4. `Reinscripción` — GET estatus + POST inscripción
5. `Intersemestral` — GET oferta + POST inscripción
6. `Auditoría` — POST registro de evento
