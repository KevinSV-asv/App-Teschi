#!/usr/bin/env bash
# Instalación inicial en el servidor (idempotente: se puede repetir).
# Uso:  sudo bash /opt/appteschi/api/deploy/01-instalar.sh
# NO toca el firewall ni servicios existentes (n8n, Nextcloud, etc.).
set -euo pipefail

[ "$(id -u)" -eq 0 ] || { echo "Ejecuta con sudo."; exit 1; }
[ "$(uname -m)" = "x86_64" ] || {
  echo "Arquitectura $(uname -m): SQL Server en Docker solo corre en x86_64. Detente y avisa."; exit 1; }
command -v docker >/dev/null 2>&1 || {
  echo "Falta Docker: https://docs.docker.com/engine/install/ubuntu/"; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "Falta el plugin 'docker compose'."; exit 1; }
for c in curl xz openssl; do
  command -v "$c" >/dev/null 2>&1 || { echo "Falta $c: sudo apt install -y curl xz-utils openssl"; exit 1; }
done

BASE=/opt/appteschi
DEPLOY="$BASE/api/deploy"
NODE_VERSION=22.16.0   # misma versión con la que se desarrolló y probó
[ -f "$DEPLOY/docker-compose.sql.yml" ] || { echo "No encuentro $DEPLOY. Sube el código primero con deploy-desde-windows.ps1."; exit 1; }

echo ">> Usuario y carpetas"
id appteschi >/dev/null 2>&1 || useradd --system --home "$BASE" --shell /usr/sbin/nologin appteschi
mkdir -p "$BASE/backups" /opt/node
# SQL Server corre dentro del contenedor como UID 10001: debe poder escribir aquí.
chown 10001:0 "$BASE/backups"; chmod 770 "$BASE/backups"

echo ">> Node $NODE_VERSION (binario oficial verificado con SHA256)"
if [ ! -x /opt/node/bin/node ] || [ "$(/opt/node/bin/node -v)" != "v$NODE_VERSION" ]; then
  tmp="$(mktemp -d)"
  f="node-v$NODE_VERSION-linux-x64.tar.xz"
  curl -fsSL "https://nodejs.org/dist/v$NODE_VERSION/$f" -o "$tmp/$f"
  curl -fsSL "https://nodejs.org/dist/v$NODE_VERSION/SHASUMS256.txt" -o "$tmp/SHASUMS256.txt"
  (cd "$tmp" && grep " $f\$" SHASUMS256.txt | sha256sum -c -)
  rm -rf /opt/node/*
  tar -xJf "$tmp/$f" -C /opt/node --strip-components=1
  rm -rf "$tmp"
fi

echo ">> Contraseña de sa (solo se crea si no existe)"
if [ ! -f "$DEPLOY/.env" ]; then
  pass="$(openssl rand -base64 30 | tr -dc 'A-Za-z0-9' | head -c 22)Aa1"
  printf 'MSSQL_SA_PASSWORD=%s\n' "$pass" > "$DEPLOY/.env"
  echo "   Generada y guardada en $DEPLOY/.env (solo root). Respáldala en tu gestor de contraseñas."
fi
chown root:root "$DEPLOY/.env"; chmod 600 "$DEPLOY/.env"

echo ">> SQL Server 2022 Express (Docker)"
docker compose -f "$DEPLOY/docker-compose.sql.yml" --project-directory "$DEPLOY" up -d
for _ in $(seq 1 24); do
  [ "$(docker inspect -f '{{.State.Health.Status}}' appteschi-sql 2>/dev/null)" = healthy ] && break
  sleep 5
done
echo "   Estado: $(docker inspect -f '{{.State.Health.Status}}' appteschi-sql)"

echo ">> Permisos y dependencias de la API (npm ci)"
bash "$DEPLOY/aplicar.sh"

echo ">> Servicio systemd y respaldo diario"
install -m 644 "$DEPLOY/appteschi-api.service" /etc/systemd/system/appteschi-api.service
systemctl daemon-reload
systemctl enable appteschi-api >/dev/null 2>&1
cat > /etc/cron.d/appteschi-backup <<EOF
# Respaldo diario de AppTeschiDB a las 03:15 (conserva 14 días)
15 3 * * * root /bin/bash $DEPLOY/backup-sql.sh >> /var/log/appteschi-backup.log 2>&1
EOF
chmod 644 /etc/cron.d/appteschi-backup

cat <<MSG

Listo. Falta (en este orden):
  1. Copia el .env del backend:
       scp .env USUARIO@SERVIDOR:/tmp/api.env
       sudo install -o appteschi -g appteschi -m 600 /tmp/api.env $BASE/api/.env && rm /tmp/api.env
  2. Copia el respaldo de la base (.bak) y restáuralo:
       sudo bash $DEPLOY/02-restaurar-bd.sh /ruta/AppTeschiDB.bak
     (ese script ajusta DB_* del .env y arranca el servicio)
  3. Firewall (NO se aplicó nada automáticamente). Si usas ufw, solo tu LAN y Tailscale:
       sudo ufw allow from 192.168.0.0/24 to any port 4000 proto tcp
       sudo ufw allow in on tailscale0 to any port 4000 proto tcp
MSG
