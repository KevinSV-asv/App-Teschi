// Envío de correo con Microsoft Graph (Microsoft 365 / Exchange Online) usando una app
// registrada en Microsoft Entra con permiso de aplicación Mail.Send.
//
// Flujo "client credentials": el backend pide un token a Entra con el tenant, el client id y el
// secreto de la app, y con él llama a POST /users/{buzón}/sendMail. No hay contraseñas de
// usuario ni SMTP de por medio. El secreto solo vive en el .env del servidor.
//
// Variables (todas obligatorias para activar este modo; si falta alguna se usa el SMTP):
//   MS_TENANT_ID      Directory (tenant) ID. Para teschi.edu.mx es público (openid-configuration).
//   MS_CLIENT_ID      Application (client) ID de la app registrada — NO es el "Secret ID".
//   MS_CLIENT_SECRET  VALOR del secreto de cliente (se muestra una sola vez al crearlo).
//   MS_SENDER         Buzón desde el que se envía (ej. appteschi@teschi.edu.mx).

const MARGEN_TOKEN_MS = 60 * 1000;

/** Devuelve las variables MS_* que faltan (vacío si el modo Graph está completamente configurado). */
function variablesGraphFaltantes(env = process.env) {
  return ['MS_TENANT_ID', 'MS_CLIENT_ID', 'MS_CLIENT_SECRET', 'MS_SENDER'].filter((k) => !String(env[k] || '').trim());
}

/**
 * @param {{tenantId:string, clientId:string, clientSecret:string, remitente:string,
 *          fetchImpl?:typeof fetch, ahora?:()=>number}} opciones
 */
function crearCorreoGraph({ tenantId, clientId, clientSecret, remitente, fetchImpl = fetch, ahora = Date.now }) {
  let token = null;
  let venceEn = 0;

  async function leerJson(respuesta) {
    try { return await respuesta.json(); } catch { return null; }
  }

  async function obtenerToken() {
    if (token && ahora() < venceEn - MARGEN_TOKEN_MS) return token;
    const respuesta = await fetchImpl(`https://login.microsoftonline.com/${encodeURIComponent(tenantId)}/oauth2/v2.0/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        client_id: clientId,
        client_secret: clientSecret,
        scope: 'https://graph.microsoft.com/.default',
        grant_type: 'client_credentials'
      })
    });
    const json = await leerJson(respuesta);
    if (!respuesta.ok || !json?.access_token) {
      // La descripción trae el código AADSTS (útil para diagnosticar) y nunca incluye el secreto.
      const detalle = String(json?.error_description || json?.error || `HTTP ${respuesta.status}`).split(/\r?\n/)[0];
      throw new Error(`Microsoft Entra rechazó las credenciales de la app: ${detalle}`);
    }
    token = json.access_token;
    venceEn = ahora() + Number(json.expires_in || 3600) * 1000;
    return token;
  }

  async function enviar({ to, subject, html }) {
    const accessToken = await obtenerToken();
    const respuesta = await fetchImpl(`https://graph.microsoft.com/v1.0/users/${encodeURIComponent(remitente)}/sendMail`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: {
          subject,
          body: { contentType: 'HTML', content: html },
          toRecipients: [{ emailAddress: { address: to } }]
        },
        saveToSentItems: false
      })
    });
    if (respuesta.status === 202) return;
    if (respuesta.status === 401) token = null; // token inválido: que el próximo intento pida uno nuevo
    const json = await leerJson(respuesta);
    const codigo = json?.error?.code || `HTTP ${respuesta.status}`;
    const mensaje = json?.error?.message || '';
    throw new Error(`Microsoft Graph no pudo enviar el correo (${codigo}): ${mensaje}`.trim());
  }

  return { enviar };
}

module.exports = { crearCorreoGraph, variablesGraphFaltantes };
