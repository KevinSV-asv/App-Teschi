# =========================================================================
# Instalador de skills/plugins de Claude Code para el proyecto AppTeschi
# Generado por Claude - ejecutar dentro de la carpeta del proyecto:
#   C:\Users\kevin\AndroidStudioProjects\AppTeschi
# =========================================================================
# Uso:
#   1) Abre PowerShell en esa carpeta (clic derecho -> "Abrir en Terminal")
#   2) Ejecuta:  powershell -ExecutionPolicy Bypass -File .\instalar-skills-claude-code.ps1
# =========================================================================

Write-Host "== 1) Supabase (plugin oficial: skills + servidor MCP) ==" -ForegroundColor Cyan
claude plugin marketplace add supabase/agent-skills
claude plugin install supabase@supabase-agent-skills
claude plugin install postgres-best-practices@supabase-agent-skills
Write-Host "Supabase listo. La primera vez que Claude use el servidor MCP te pedira iniciar sesion (OAuth) en tu cuenta de Supabase." -ForegroundColor Green

Write-Host ""
Write-Host "== 2) skillui (extractor de sistemas de diseno) ==" -ForegroundColor Cyan
Write-Host "No requiere 'instalacion' previa: se ejecuta con npx bajo demanda." -ForegroundColor Yellow
Write-Host "Ejemplo de uso una vez que quieras extraer un diseno:"
Write-Host '   npx skillui --url https://ejemplo.com'
Write-Host "Para que Claude Code reconozca la skill de forma permanente en este proyecto, abre Claude Code aqui y pidele:"
Write-Host '   "Instala la skill skillui desde https://github.com/Roentek/Claude_Code_Boilerplate_Framework/archive/main.zip#skillui en .claude/skills/"'

Write-Host ""
Write-Host "== 3) Strix (pentesting automatizado con IA) ==" -ForegroundColor Cyan
Write-Host "Requisitos: Docker Desktop corriendo + una API key de tu LLM." -ForegroundColor Yellow
Write-Host "Si aun no lo instalas como herramienta CLI, corre:"
Write-Host '   curl -sSL https://strix.ai/install | bash'
Write-Host '   $env:STRIX_LLM = "tu-modelo-preferido"'
Write-Host '   $env:LLM_API_KEY = "tu-api-key"'
Write-Host "Nota: ya existe una carpeta .strix en tu usuario de Windows, es posible que ya lo hayas instalado antes - revisa con 'strix --version'."
Write-Host ""
Write-Host "Para agregar las 9 skills de Strix a Claude Code (para que Claude sepa invocarlo):"
Write-Host '   npx skills add usestrix/strix'

Write-Host ""
Write-Host "== Verificacion final ==" -ForegroundColor Cyan
Write-Host "Dentro de Claude Code, corre /plugin  para confirmar que 'supabase' y 'postgres-best-practices' aparecen instalados."
Write-Host "Revisa que exista la carpeta .claude\skills\ con las skills nuevas."
