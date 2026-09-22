#!/usr/bin/env bash
# Lo llama deploy-desde-windows.ps1 después de subir el código (corre como root)
# y también 01-instalar.sh. Ajusta permisos, instala dependencias y reinicia la API.
set -euo pipefail

API=/opt/appteschi/api
if ! id appteschi >/dev/null 2>&1; then
  echo "Primera vez: ejecuta  sudo bash $API/deploy/01-instalar.sh"
  exit 0
fi

chown -R appteschi:appteschi "$API"
[ -f "$API/deploy/.env" ] && { chown root:root "$API/deploy/.env"; chmod 600 "$API/deploy/.env"; }
[ -f "$API/.env" ] && { sed -i 's/\r$//' "$API/.env"; chmod 600 "$API/.env"; }   # el .env viene de Windows (CRLF)

# El usuario appteschi no tiene HOME escribible: npm necesita uno propio.
NPM_HOME=/var/tmp/appteschi-home
mkdir -p "$NPM_HOME"; chown appteschi:appteschi "$NPM_HOME"

cd "$API"
runuser -u appteschi -- env HOME="$NPM_HOME" PATH=/opt/node/bin:/usr/bin:/bin \
  npm ci --omit=dev --no-audit --no-fund --cache "$NPM_HOME/.npm"

# Sin esto la API entra en bucle de reinicios con "Cannot find module".
[ -d "$API/node_modules/express" ] || { echo "npm ci terminó sin instalar express: revisa el error de arriba."; exit 1; }

if [ -f "$API/.env" ]; then
  systemctl restart appteschi-api
  sleep 2
  echo "appteschi-api: $(systemctl is-active appteschi-api)"
else
  echo "Falta $API/.env; no reinicio la API."
fi
