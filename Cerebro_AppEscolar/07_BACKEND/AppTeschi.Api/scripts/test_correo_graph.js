// Prueba de correo-graph.js con un fetch falso (no toca la red). Uso: node scripts/test_correo_graph.js
const assert = require('assert');
const { crearCorreoGraph, variablesGraphFaltantes } = require('../correo-graph');

const respuesta = (status, json) => ({ status, ok: status >= 200 && status < 300, json: async () => json });
let pruebas = 0;
async function prueba(nombre, fn) { await fn(); pruebas++; console.log('  ok -', nombre); }

(async () => {
  await prueba('detecta las variables MS_* que faltan', async () => {
    assert.deepStrictEqual(variablesGraphFaltantes({}), ['MS_TENANT_ID', 'MS_CLIENT_ID', 'MS_CLIENT_SECRET', 'MS_SENDER']);
    assert.deepStrictEqual(variablesGraphFaltantes({ MS_TENANT_ID: 'a', MS_CLIENT_ID: 'b', MS_CLIENT_SECRET: 'c', MS_SENDER: 'd@x.mx' }), []);
    assert.deepStrictEqual(variablesGraphFaltantes({ MS_TENANT_ID: 'a', MS_CLIENT_ID: '  ', MS_CLIENT_SECRET: 'c', MS_SENDER: 'd' }), ['MS_CLIENT_ID']);
  });

  await prueba('pide token con client credentials y envía por sendMail con el formato de Graph', async () => {
    const llamadas = [];
    const fetchFalso = async (url, opciones) => {
      llamadas.push({ url, opciones });
      return url.includes('/oauth2/v2.0/token')
        ? respuesta(200, { access_token: 'TOKEN-FALSO', expires_in: 3600 })
        : respuesta(202, null);
    };
    const graph = crearCorreoGraph({ tenantId: 'tenant-1', clientId: 'cliente-1', clientSecret: 'secreto-1', remitente: 'app@teschi.edu.mx', fetchImpl: fetchFalso });
    await graph.enviar({ to: 'alumno@teschi.edu.mx', subject: 'Asunto', html: '<b>hola</b>' });

    assert.strictEqual(llamadas.length, 2);
    assert.strictEqual(llamadas[0].url, 'https://login.microsoftonline.com/tenant-1/oauth2/v2.0/token');
    const form = new URLSearchParams(llamadas[0].opciones.body);
    assert.strictEqual(form.get('grant_type'), 'client_credentials');
    assert.strictEqual(form.get('client_id'), 'cliente-1');
    assert.strictEqual(form.get('client_secret'), 'secreto-1');
    assert.strictEqual(form.get('scope'), 'https://graph.microsoft.com/.default');

    assert.strictEqual(llamadas[1].url, 'https://graph.microsoft.com/v1.0/users/app%40teschi.edu.mx/sendMail');
    assert.strictEqual(llamadas[1].opciones.headers.Authorization, 'Bearer TOKEN-FALSO');
    const cuerpo = JSON.parse(llamadas[1].opciones.body);
    assert.strictEqual(cuerpo.message.subject, 'Asunto');
    assert.deepStrictEqual(cuerpo.message.body, { contentType: 'HTML', content: '<b>hola</b>' });
    assert.strictEqual(cuerpo.message.toRecipients[0].emailAddress.address, 'alumno@teschi.edu.mx');
    assert.strictEqual(cuerpo.saveToSentItems, false);
  });

  await prueba('reutiliza el token mientras no venza y pide otro al vencer', async () => {
    let tokens = 0, reloj = 1_000_000;
    const fetchFalso = async (url) => {
      if (url.includes('/oauth2/')) { tokens++; return respuesta(200, { access_token: 'T' + tokens, expires_in: 3600 }); }
      return respuesta(202, null);
    };
    const graph = crearCorreoGraph({ tenantId: 't', clientId: 'c', clientSecret: 's', remitente: 'a@b.mx', fetchImpl: fetchFalso, ahora: () => reloj });
    await graph.enviar({ to: 'x@y.mx', subject: 's', html: 'h' });
    await graph.enviar({ to: 'x@y.mx', subject: 's', html: 'h' });
    assert.strictEqual(tokens, 1);
    reloj += 3600 * 1000;
    await graph.enviar({ to: 'x@y.mx', subject: 's', html: 'h' });
    assert.strictEqual(tokens, 2);
  });

  await prueba('si Entra rechaza las credenciales, el error trae el código AADSTS y NO el secreto', async () => {
    const fetchFalso = async () => respuesta(401, { error: 'invalid_client', error_description: 'AADSTS7000215: Invalid client secret provided.\r\nTrace ID: abc' });
    const graph = crearCorreoGraph({ tenantId: 't', clientId: 'c', clientSecret: 'SECRETO-SUPER-PRIVADO', remitente: 'a@b.mx', fetchImpl: fetchFalso });
    await assert.rejects(() => graph.enviar({ to: 'x@y.mx', subject: 's', html: 'h' }), (e) => {
      assert.match(e.message, /AADSTS7000215/);
      assert.doesNotMatch(e.message, /SECRETO-SUPER-PRIVADO/);
      assert.doesNotMatch(e.message, /Trace ID/);
      return true;
    });
  });

  await prueba('si Graph responde 403 (falta Mail.Send o el consentimiento) el error lo dice', async () => {
    const fetchFalso = async (url) => url.includes('/oauth2/')
      ? respuesta(200, { access_token: 'T', expires_in: 3600 })
      : respuesta(403, { error: { code: 'ErrorAccessDenied', message: 'Access is denied.' } });
    const graph = crearCorreoGraph({ tenantId: 't', clientId: 'c', clientSecret: 's', remitente: 'a@b.mx', fetchImpl: fetchFalso });
    await assert.rejects(() => graph.enviar({ to: 'x@y.mx', subject: 's', html: 'h' }), /ErrorAccessDenied.*Access is denied/);
  });

  console.log(`\n${pruebas} pruebas correctas`);
})().catch((e) => { console.error('FALLÓ:', e.message); process.exit(1); });
