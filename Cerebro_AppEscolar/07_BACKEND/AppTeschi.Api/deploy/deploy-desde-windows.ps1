<#
Sube el backend al servidor (por Tailscale o LAN) y reinicia la API.
Uso:  .\deploy\deploy-desde-windows.ps1 -Servidor usuario@nombre-o-ip-del-servidor
Usa ssh/scp/tar que ya trae Windows 10+. No sube node_modules, .env ni .bak.
Pide tu contraseña de sudo en el servidor.
#>
param([Parameter(Mandatory)][string]$Servidor)
$ErrorActionPreference = 'Stop'

$api = Split-Path -Parent $PSScriptRoot
$tgz = Join-Path $env:TEMP 'appteschi-api.tgz'

tar -czf $tgz --exclude=node_modules --exclude=.env --exclude='*.bak' -C $api .
if ($LASTEXITCODE -ne 0) { throw 'tar falló' }

scp $tgz "${Servidor}:/tmp/appteschi-api.tgz"
if ($LASTEXITCODE -ne 0) { throw 'scp falló' }
Remove-Item $tgz

ssh -t $Servidor "sudo mkdir -p /opt/appteschi/api && sudo tar --no-same-owner -xzf /tmp/appteschi-api.tgz -C /opt/appteschi/api && rm /tmp/appteschi-api.tgz && sudo bash /opt/appteschi/api/deploy/aplicar.sh"
if ($LASTEXITCODE -ne 0) { throw 'La actualización remota falló' }
