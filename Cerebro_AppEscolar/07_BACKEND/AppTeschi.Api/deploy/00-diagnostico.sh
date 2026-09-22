#!/usr/bin/env bash
# Solo lectura: no instala ni cambia nada. Córrelo en el servidor y pega la
# salida completa antes de ejecutar 01-instalar.sh.
set -u

echo "== Sistema =="
. /etc/os-release; echo "$PRETTY_NAME"; echo "arquitectura: $(uname -m)"

echo "== RAM / disco =="
free -h | sed -n '1,2p'
df -h / | tail -n 2

echo "== Docker =="
if command -v docker >/dev/null 2>&1; then
  docker --version
  docker compose version 2>&1 | head -1
  docker ps --format '{{.Names}}\t{{.Image}}\t{{.Ports}}' 2>&1
else
  echo "docker: NO instalado"
fi

echo "== Puertos (1433 = SQL Server, 4000 = API) =="
ss -ltnH 2>/dev/null | awk '{print $4}' | grep -E ':(1433|4000)$' || echo "1433 y 4000 libres"

echo "== Utilidades requeridas =="
for c in curl xz openssl; do command -v "$c" >/dev/null 2>&1 && echo "$c: ok" || echo "$c: FALTA"; done

echo "== Node =="
command -v node >/dev/null 2>&1 && node -v || echo "node: no instalado (01-instalar.sh instala v22.16.0 en /opt/node)"

echo "== Firewall =="
sudo -n ufw status 2>/dev/null | head -1 || echo "ufw: (necesita sudo para consultarlo)"

echo "== Red =="
echo "LAN: $(hostname -I)"
command -v tailscale >/dev/null 2>&1 && echo "Tailscale: $(tailscale ip -4 2>/dev/null | head -1)" || echo "tailscale: no instalado"

echo "== Servicios ya corriendo =="
systemctl list-units --type=service --state=running --no-legend 2>/dev/null | awk '{print $1}' \
  | grep -Ei 'n8n|nextcloud|apache|nginx|caddy|docker|tailscale|cloudflared|mysql|maria|postgres|redis' || true
