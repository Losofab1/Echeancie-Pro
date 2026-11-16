<#
.SYNOPSIS
    Exporte la base PostgreSQL locale (echean) vers Neon, en une seule commande.

.EXAMPLE
    .\scripts\export-local-to-neon.ps1
    Lit l'URI Neon dans .env et demande le mot de passe PostgreSQL local.
#>
param(
    [string] $LocalHost = "localhost",
    [int]    $LocalPort = 5432,
    [string] $LocalUser = "postgres",
    [string] $LocalDb   = "echean",
    [string] $LocalPassword,

    [string] $NeonUrl,
    [string] $DumpFile = "echean-neon-$(Get-Date -Format 'yyyyMMdd-HHmm').dump",

    [switch] $DumpOnly,
    [switch] $RestoreOnly
)

$ErrorActionPreference = "Stop"

# URI Neon : paramètre ou lecture du fichier .env généré par setup-env.ps1
if (-not $NeonUrl) {
    $envPath = Join-Path (Split-Path $PSScriptRoot -Parent) ".env"
    if (-not (Test-Path $envPath)) {
        throw "Lancez d'abord : .\scripts\setup-env.ps1 -NeonUrl 'votre URI Neon'"
    }
    $line = Get-Content $envPath | Where-Object { $_ -match '^\s*ECHEAN_PSQL_URL\s*=\s*"?(.*?)"?\s*$' } | Select-Object -First 1
    if (-not $line) { throw "ECHEAN_PSQL_URL absent du .env" }
    $NeonUrl = ($line -replace '^\s*ECHEAN_PSQL_URL\s*=\s*"?', '' -replace '"?\s*$', '')
}

if (-not $RestoreOnly) {
    if (-not $LocalPassword) {
        $LocalPassword = Read-Host "Mot de passe PostgreSQL LOCAL (postgres) :" -MaskInput
    }
    $env:PGPASSWORD = $LocalPassword
    Write-Host "1/2 Export de '$LocalDb' depuis $LocalHost`:$LocalPort ..."
    & pg_dump -h $LocalHost -p $LocalPort -U $LocalUser -d $LocalDb -Fc -f $DumpFile
    if ($LASTEXITCODE -ne 0) { throw "pg_dump a échoué." }
    Write-Host "   Dump : $DumpFile"
}

if (-not $DumpOnly) {
    if (-not (Test-Path $DumpFile)) { throw "Fichier introuvable : $DumpFile" }
    $env:PGPASSWORD = ""
    Write-Host "2/2 Restauration vers Neon ..."
    & pg_restore -d $NeonUrl --clean --if-exists --no-owner --no-privileges --exit-on-error $DumpFile
    if ($LASTEXITCODE -ne 0) { throw "pg_restore a échoué." }
    Write-Host "   Terminé."
}
