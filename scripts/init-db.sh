#!/usr/bin/env bash
# Initialise ou complète une base (locale ou Neon) avec le schéma Echean.
# Si la table "users" existe déjà (base restaurée depuis un dump), les migrations
# 00 à 06 sont ignorées pour éviter les erreurs "relation already exists".
set -euo pipefail

: "${ECHEAN_PSQL_URL:?Définissez ECHEAN_PSQL_URL (URI psql, ex. postgresql://user:pass@host/db?sslmode=require)}"
: "${ECHEAN_BOOTSTRAP_ADMIN_EMAIL:?Définissez ECHEAN_BOOTSTRAP_ADMIN_EMAIL}"
: "${ECHEAN_BOOTSTRAP_ADMIN_PASSWORD:?Définissez ECHEAN_BOOTSTRAP_ADMIN_PASSWORD}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INIT="$ROOT/docker/db/init"

psql "$ECHEAN_PSQL_URL" -v ON_ERROR_STOP=1 -q -c 'SELECT 1' >/dev/null
echo "Connexion à la base OK."

if psql "$ECHEAN_PSQL_URL" -tAc "SELECT to_regclass('public.users')" | grep -q users; then
    echo "Schéma déjà présent : migrations 00-06 ignorées (base restaurée ou déjà initialisée)."
else
    for f in 00_flyway_history.sql 01_schema.sql 02_email_outbox.sql \
             03_client_creation_requests.sql 04_public_client_requests.sql \
             05_password_hash.sql 06_payment_flow.sql; do
        echo "Application de $f ..."
        psql "$ECHEAN_PSQL_URL" -v ON_ERROR_STOP=1 -q -f "$INIT/$f"
    done
fi

echo "Vérification / création du compte administrateur initial ..."
psql "$ECHEAN_PSQL_URL" -v ON_ERROR_STOP=1 -q \
     -v admin_name="${ECHEAN_BOOTSTRAP_ADMIN_NAME:-Administrateur initial}" \
     -v admin_email="$ECHEAN_BOOTSTRAP_ADMIN_EMAIL" \
     -v admin_password="$ECHEAN_BOOTSTRAP_ADMIN_PASSWORD" \
     -f "$INIT/07_admin.sql"

echo "Base prête."
