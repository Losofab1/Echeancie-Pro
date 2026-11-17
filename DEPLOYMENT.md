# Déploiement Docker — Echean (WildFly 40.0.1 + PostgreSQL Neon)

L'application tourne dans un conteneur WildFly 40.0.1 (Jakarta EE 11) et se connecte à une
**base PostgreSQL managée Neon**. Deux scénarios : partir d'une base Neon vide, ou
migrer votre base locale existante (schéma + données).

> Ne commitez jamais le fichier `.env` : il est exclu par `.gitignore` / `.dockerignore`.

## Démarrage express (4 commandes)

Depuis la racine du projet, dans PowerShell :

```powershell
# 1. Collez ici l'URI copiée dans Neon (onglet "connection string", mot de passe révélé)
.\scripts\setup-env.ps1 -NeonUrl "postgresql://neondb_owner:MOTDEPASSE@ep-xxx.neon.tech/neondb?sslmode=require"

# 2. Exporte la base locale vers Neon (le script demande le mot de passe postgres LOCAL)
.\scripts\export-local-to-neon.ps1

# 3. Démarre l'application
docker compose up -d --build

# 4. Ouvre http://localhost:8080/
```

C'est tout. Les sections ci-dessous expliquent chaque étape en détail si besoin.

## 1. Architecture

| Service | Image | Rôle |
|---|---|---|
| Neon | managé | Base `neondb`, schéma + données, sauvegardes automatiques |
| `app` | build local (Maven → WildFly 40.0.1) | Application JSF, WAR en racine `/`, datasource `java:jboss/datasources/echeanDS` |
| `caddy` *(optionnel)* | `caddy:2-alpine` | Reverse proxy HTTPS automatique (Let's Encrypt) |

Le WAR n'embarque pas le pilote PostgreSQL : l'image WildFly installe le module
`org.postgresql` (42.7.14). La datasource utilise `ECHEAN_DB_URL` avec `?sslmode=require`.

## 2. Prérequis

- Docker Engine + Docker Compose ≥ 2.24 (pour `ports: !reset []` du profil prod)
- Un projet Neon et l'URI de connexion de la branche à utiliser
- `psql` / `pg_dump` / `pg_restore` en local (ou via un conteneur `postgres:18`)
- Un nom de domaine pointant vers le serveur pour le HTTPS

## 3A. Créer la base dans la console Neon

1. Connectez-vous sur **https://console.neon.tech** et créez un projet.
2. Choisissez un nom (ex. `echean`), la **région la plus proche de votre serveur**, et la
   version PostgreSQL la plus récente proposée (17 ou supérieure — le schéma utilise
   uniquement des fonctionnalités standard, compatible dès PostgreSQL 15).
3. Gardez la branche par défaut `main` et la base `neondb` (ou créez une base `echean`).
4. Dans le projet, ouvrez **Connection Details** :
   - sélectionnez votre branche (ex. `production` ou `main`), la base `neondb` et le rôle
     proposé (souvent `neondb_owner`) ;
   - **désactivez le pooling** (« Pooled connection ») pour l'export/restore, ou utilisez
     l'URL directe : le Host ne doit **pas** contenir `-pooler` ;
   - cliquez sur **Show password** pour révéler le mot de passe dans l'URI ;
   - copiez l'URI `postgresql://…` et ajoutez `?sslmode=require` si ce n'est pas déjà là.
5. Si vous avez activé une liste d'IP autorisées dans Neon, ajoutez l'IP publique de la
   machine qui fera l'export (et celle du serveur d'hébergement).
6. Vérifiez dans le **SQL Editor** de Neon : `SELECT current_database(), version();`

> Neon met la base en pause après une période d'inactivité (scale-to-zero) : la première
> connexion après une pause peut prendre quelques secondes. C'est normal.

Deux URLs à renseigner dans `.env` :
- `ECHEAN_PSQL_URL` = l'URI `postgresql://…` copiée ci-dessus (avec mot de passe et sslmode),
  par ex. `postgresql://neondb_owner:<mot-de-passe>@<host>/neondb?sslmode=require`
- `ECHEAN_DB_URL` = la même chose en JDBC :
  `jdbc:postgresql://<host>/<base>?sslmode=require`

Pour l'application, gardez l'URL **directe** (sans `-pooler`) : le pool WildFly est limité
à 10 connexions et évite les problèmes de prepared statements avec le pooler de Neon.
Si vous tenez absolument à utiliser le point d'accès poolé, ajoutez `&prepareThreshold=0`
à l'URL JDBC.

## 3. Configuration `.env`

```bash
cp .env.example .env
nano .env
```

Renseignez **les deux URLs** (même branche Neon) :

| Variable | Forme | Utilisée par |
|---|---|---|
| `ECHEAN_DB_URL` | `jdbc:postgresql://ep-xxx.region.aws.neon.tech/neondb?sslmode=require` | WildFly |
| `ECHEAN_PSQL_URL` | `postgresql://user:pass@ep-xxx.region.aws.neon.tech/neondb?sslmode=require` | scripts `psql` |
| `ECHEAN_DB_USER` / `ECHEAN_DB_PASSWORD` | identifiants Neon | WildFly |
| `ECHEAN_BOOTSTRAP_ADMIN_*` | email / mot de passe du premier admin | script d'init |

Récupérez l'URI dans le dashboard Neon → **Connection Details** → champ *Connection string*
(ajoutez `?sslmode=require` s'il manque).

## 4A. Base Neon vide — initialiser le schéma

```bash
chmod +x scripts/init-db.sh
./scripts/init-db.sh
```

Le script applique dans l'ordre les migrations `docker/db/init/00…06` puis crée le compte
administrateur initial (mot de passe haché en bcrypt via `pgcrypto`, compatible avec
`PasswordHasher`). Il détecte la présence de la table `users` et ignore les migrations
si le schéma existe déjà : vous pouvez donc l'exécuter sans risque après une restauration.

## 4B. Migrer la base locale vers Neon (données incluses)

Depuis votre machine Windows (psql installé) :

```powershell
$env:ECHEAN_DB_PASSWORD = "<mot de passe PostgreSQL local>"
.\scripts\export-local-to-neon.ps1 -NeonUrl "postgresql://user:pass@ep-xxx.../neondb?sslmode=require"
```

Le script :
1. `pg_dump -Fc` de la base locale `echean` → fichier `echean-neon-<date>.dump`
2. `pg_restore --clean --if-exists --no-owner --no-privileges` vers Neon

Options utiles :
- `-DumpOnly` : produit uniquement le dump (pas de restauration)
- `-RestoreOnly -DumpFile echean-neon-....dump` : rejoue un dump déjà créé

Depuis Linux/macOS ou sans psql local, équivalent en une seule commande :

```bash
PGPASSWORD=<mot-de-passe-local> pg_dump -h localhost -U postgres -d echean -Fc -f echean.dump
pg_restore -d "$ECHEAN_PSQL_URL" --clean --if-exists --no-owner --no-privileges --exit-on-error echean.dump
```

> `--no-owner --no-privileges` est indispensable : l'utilisateur Neon n'est pas le même
> que le `postgres` local. Le dump inclut déjà les données et la table
> `flyway_schema_history` : **ne relancez pas les migrations 01-06** (le script
> `init-db.sh` les ignore automatiquement de toute façon).

## 5. Démarrage de l'application

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f app
```

Première connexion : `http://localhost:8080/` avec `ECHEAN_BOOTSTRAP_ADMIN_EMAIL` /
`ECHEAN_BOOTSTRAP_ADMIN_PASSWORD`. Si vous venez de restaurer un dump, utilisez les
identifiants de l'administrateur **déjà présent** dans la base locale (le mot de passe
de l'admin local est conservé tel quel).

## 6. Passage en production (HTTPS)

Renseignez `ECHEAN_DOMAIN`, puis :

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Caddy obtient un certificat Let's Encrypt et l'application n'est plus exposée directement.
Avec un reverse proxy existant, gardez le `docker-compose.yml` seul et pointez-le vers
`127.0.0.1:8080`.

## 7. Sauvegardes et mises à jour

Neon propose des sauvegardes automatiques et la restauration dans le temps (PITR) depuis
son dashboard — c'est le moyen le plus simple. Sauvegarde manuelle complémentaire :

```bash
pg_dump "$ECHEAN_PSQL_URL" -Fc -f backup-echean-$(date +%F).dump
```

Mise à jour de l'application (la base n'est pas touchée) :

```bash
git pull
docker compose up -d --build
```

## 8. Dépannage

| Symptôme | Vérification |
|---|---|
| `password authentication failed` | `ECHEAN_DB_USER` / `ECHEAN_DB_PASSWORD` doivent correspondre à l'URI Neon |
| `SSL connection required` | `?sslmode=require` doit être présent dans `ECHEAN_DB_URL` **et** `ECHEAN_PSQL_URL` |
| `too many connections` (Neon free) | réduire `max-pool-size` dans `docker/wildfly/configure.cli` (10 au lieu de 20) puis rebuild |
| Page blanche / 500 | `docker compose logs -f app` |
| Tables manquantes | relancer `./scripts/init-db.sh` |
| Certificat HTTPS absent | DNS vers le serveur + ports 80/443 ouverts |

## 9. Fichiers préparés

```
Dockerfile                         build Maven + WildFly + module JDBC + datasource
docker-compose.yml                 application seule (base externe Neon)
docker-compose.prod.yml            ajout de Caddy (HTTPS)
docker/caddy/Caddyfile              reverse proxy
docker/wildfly/configure.cli        pilote + datasource WildFly
docker/db/init/00…06_*.sql          schéma complet (V1 → V6)
docker/db/init/07_admin.sql         compte administrateur initial
scripts/init-db.sh                  initialisation / complétion de la base
scripts/export-local-to-neon.ps1    export local → Neon
.env.example, .gitignore, .dockerignore
DEPLOYMENT.md                       ce guide
```
