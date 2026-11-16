#!/bin/bash
# Crée le compte administrateur initial si la base est vide.
# Le hachage bcrypt est produit par pgcrypto (gen_salt('bf')), compatible
# avec com.mycompany.jsf.security.PasswordHasher (jBCrypt, $2a$).
set -euo pipefail

: "${ECHEAN_BOOTSTRAP_ADMIN_EMAIL:?Définissez ECHEAN_BOOTSTRAP_ADMIN_EMAIL dans .env}"
: "${ECHEAN_BOOTSTRAP_ADMIN_PASSWORD:?Définissez ECHEAN_BOOTSTRAP_ADMIN_PASSWORD dans .env (12 caractères minimum)}"

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     -v admin_name="${ECHEAN_BOOTSTRAP_ADMIN_NAME:-Administrateur initial}" \
     -v admin_email="$ECHEAN_BOOTSTRAP_ADMIN_EMAIL" \
     -v admin_password="$ECHEAN_BOOTSTRAP_ADMIN_PASSWORD" <<'SQL'
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (id, name, email, password_hash, role, active, created_at)
SELECT gen_random_uuid(),
       :'admin_name',
       lower(:'admin_email'),
       crypt(:'admin_password', gen_salt('bf', 12)),
       'ADMIN',
       TRUE,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users);
SQL

echo "Compte administrateur initial prêt : ${ECHEAN_BOOTSTRAP_ADMIN_EMAIL}"
