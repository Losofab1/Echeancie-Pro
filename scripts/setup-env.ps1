<#
.SYNOPSIS
    Écrit le fichier .env à partir de l'URI de connexion copiée dans Neon.

.EXAMPLE
    .\scripts\setup-env.ps1 -NeonUrl "postgresql://neondb_owner:motdepasse@ep-xxx.neon.tech/neondb?sslmode=require"
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $NeonUrl,

    [string] $AdminEmail = "admin@votre-domaine.com",
    [string] $AdminPassword = "ChangezMoi1234!"
)

$ErrorActionPreference = "Stop"

# Décompose l'URI Neon : postgresql://USER:PASSWORD@HOST/BASE?sslmode=require
if ($NeonUrl -notmatch '^postgres(?:ql)?://([^:]+):([^@]+)@([^/]+)/([^?]+)') {
    throw "URI Neon invalide. Elle doit ressembler à : postgresql://user:pass@host/db?sslmode=require"
}
$user = $Matches[1]
$pass = $Matches[2]
$dbHost = $Matches[3]
$db   = $Matches[4]

if ($NeonUrl -notmatch 'sslmode=') {
    $NeonUrl += "?sslmode=require"
}
$jdbc = "jdbc:postgresql://" + $dbHost + "/" + $db + "?sslmode=require"

$content = @"
# Genere par scripts/setup-env.ps1 - ne pas committer
ECHEAN_PSQL_URL="$NeonUrl"
ECHEAN_DB_URL="$jdbc"
ECHEAN_DB_USER="$user"
ECHEAN_DB_PASSWORD="$pass"
ECHEAN_BOOTSTRAP_ADMIN_NAME="Administrateur initial"
ECHEAN_BOOTSTRAP_ADMIN_EMAIL="$AdminEmail"
ECHEAN_BOOTSTRAP_ADMIN_PASSWORD="$AdminPassword"
ECHEAN_DOMAIN="echean.votre-domaine.com"
ECHEAN_SMTP_HOST=""
ECHEAN_SMTP_PORT=587
ECHEAN_SMTP_USER=""
ECHEAN_SMTP_PASSWORD=""
ECHEAN_SMTP_FROM=""
"@

$envPath = Join-Path (Split-Path $PSScriptRoot -Parent) ".env"
[System.IO.File]::WriteAllText($envPath, $content, (New-Object System.Text.UTF8Encoding($false)))

Write-Host "Fichier .env créé : $envPath"
Write-Host "  Utilisateur Neon : $user"
Write-Host "  Host Neon        : $dbHost"
Write-Host "  Base             : $db"
Write-Host "  Admin (si base vide) : $AdminEmail"
Write-Host "Pensez à changer l'email/mot de passe admin si besoin : notepad .env"
