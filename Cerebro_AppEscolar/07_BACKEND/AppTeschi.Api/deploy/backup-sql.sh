#!/usr/bin/env bash
# Respaldo diario de AppTeschiDB (lo programa 01-instalar.sh en /etc/cron.d).
# Verifica el respaldo, lo comprime y conserva 14 días en /opt/appteschi/backups.
# OJO: esto vive en el mismo servidor. Falta una copia FUERA de él (Nextcloud,
# otro equipo por Tailscale o un disco externo) para sobrevivir a un fallo de disco.
set -euo pipefail

DEPLOY="$(cd "$(dirname "$0")" && pwd)"
set -a; . "$DEPLOY/.env"; set +a

DEST=/opt/appteschi/backups
FILE="AppTeschiDB_$(date +%Y%m%d_%H%M%S).bak"
SQLCMD_BIN="$(docker exec appteschi-sql sh -c 'ls /opt/mssql-tools18/bin/sqlcmd 2>/dev/null || ls /opt/mssql-tools/bin/sqlcmd')"
sql() { docker exec -e SQLCMDPASSWORD="$MSSQL_SA_PASSWORD" appteschi-sql "$SQLCMD_BIN" -C -S localhost -U sa -b "$@"; }

sql -Q "BACKUP DATABASE AppTeschiDB TO DISK='/backups/$FILE' WITH INIT, CHECKSUM"
sql -Q "RESTORE VERIFYONLY FROM DISK='/backups/$FILE' WITH CHECKSUM"
gzip -f "$DEST/$FILE"
find "$DEST" -name 'AppTeschiDB_*.bak.gz' -mtime +14 -delete
echo "$(date -Is) OK $FILE.gz"
