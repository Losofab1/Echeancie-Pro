-- Compte administrateur initial (uniquement si la base est vide).
-- Variables fournies par scripts/init-db.sh :
--   -v admin_name=... -v admin_email=... -v admin_password=...
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
