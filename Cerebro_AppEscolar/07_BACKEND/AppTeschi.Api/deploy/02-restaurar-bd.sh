#!/usr/bin/env bash
# Restaura AppTeschiDB desde un .bak de tu laptop, crea un login propio para la
# API (ya no se usa sa) y arranca el servicio.
# Uso:  sudo bash 02-restaurar-bd.sh /ruta/AppTeschiDB.bak
# Generar el .bak en la laptop (cmd):
#   sqlcmd -S localhost -E -Q "BACKUP DATABASE AppTeschiDB TO DISK='C:\Temp\AppTeschiDB.bak' WITH INIT, CHECKSUM"
set -euo pipefail

[ "$(id -u)" -eq 0 ] || { echo "Ejecuta con sudo."; exit 1; }
BAK="${1:?Uso: sudo bash 02-restaurar-bd.sh /ruta/AppTeschiDB.bak}"
[ -f "$BAK" ] || { echo "No existe $BAK"; exit 1; }

DEPLOY="$(cd "$(dirname "$0")" && pwd)"
ENV_API=/opt/appteschi/api/.env
[ -f "$ENV_API" ] || { echo "Falta $ENV_API (ver el paso 1 que imprimió 01-instalar.sh)."; exit 1; }
# El .env viene de Windows con saltos de línea CRLF: sin esto cada valor conserva
# un \r final (PORT=4000\r) y, por ejemplo, la URL de la comprobación final sale mal formada.
sed -i 's/\r$//' "$ENV_API"

set -a; . "$DEPLOY/.env"; set +a
SQLCMD_BIN="$(docker exec appteschi-sql sh -c 'ls /opt/mssql-tools18/bin/sqlcmd 2>/dev/null || ls /opt/mssql-tools/bin/sqlcmd')"
sql() { docker exec -i -e SQLCMDPASSWORD="$MSSQL_SA_PASSWORD" appteschi-sql "$SQLCMD_BIN" -C -S localhost -U sa "$@"; }

echo ">> Deteniendo la API (si corre)"
systemctl stop appteschi-api 2>/dev/null || true

echo ">> Restaurando la base"
# cp + chown en vez de `install -o`: el UID 10001 (mssql, dentro del contenedor)
# no existe como usuario del host y `install -o` lo rechaza en Ubuntu 26.04.
cp "$BAK" /opt/appteschi/backups/restore.bak
chown 10001:0 /opt/appteschi/backups/restore.bak
chmod 640 /opt/appteschi/backups/restore.bak
sql -b -Q "RESTORE DATABASE AppTeschiDB FROM DISK='/backups/restore.bak' WITH CHECKSUM, REPLACE, STATS=25, MOVE 'AppTeschiDB' TO '/var/opt/mssql/data/AppTeschiDB.mdf', MOVE 'AppTeschiDB_log' TO '/var/opt/mssql/data/AppTeschiDB_log.ldf'"
rm -f /opt/appteschi/backups/restore.bak   # contiene datos reales de alumnos

echo ">> Login propio para la API (db_owner solo en AppTeschiDB)"
API_PASS="$(openssl rand -base64 30 | tr -dc 'A-Za-z0-9' | head -c 22)Aa1"
sql -b -i /dev/stdin <<SQL
IF EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'appteschi_api') DROP LOGIN appteschi_api;
CREATE LOGIN appteschi_api WITH PASSWORD = '$API_PASS', CHECK_POLICY = OFF;
GO
USE AppTeschiDB;
IF EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'appteschi_api') DROP USER appteschi_api;
CREATE USER appteschi_api FOR LOGIN appteschi_api;
ALTER ROLE db_owner ADD MEMBER appteschi_api;
GO
SQL

set_env() { # KEY VALUE
  if grep -q "^$1=" "$ENV_API"; then sed -i "s|^$1=.*|$1=$2|" "$ENV_API"; else echo "$1=$2" >> "$ENV_API"; fi
}
set_env DB_SERVER 127.0.0.1
set_env DB_DATABASE AppTeschiDB
set_env DB_USERNAME appteschi_api
set_env DB_PASSWORD "$API_PASS"
set_env DB_ENCRYPT false
chown appteschi:appteschi "$ENV_API"; chmod 600 "$ENV_API"

echo ">> Arrancando la API"
systemctl restart appteschi-api
PORT_API="$(grep -E '^PORT=' "$ENV_API" | cut -d= -f2 | tr -d '\r' || true)"
for _ in $(seq 1 10); do
  sleep 2
  if curl -fsS "http://127.0.0.1:${PORT_API:-4000}/health"; then echo; exit 0; fi
done
echo "La API no respondió en 20 s: journalctl -u appteschi-api -n 50 --no-pager"
exit 1
