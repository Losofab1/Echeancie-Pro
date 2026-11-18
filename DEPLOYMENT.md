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
à 5 connexions et évite les problèmes de prepared statements avec le pooler de Neon.
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

> **Déploiement Render / hébergeur sans `.env`** : définissez les variables dans le tableau
> de bord du service. L'image accepte **soit** `ECHEAN_DB_URL` (URL JDBC attendue par
> WildFly), **soit** `ECHEAN_PSQL_URL` (l'URI Neon `postgresql://…`) — dans ce dernier cas,
> l'entrypoint convertit automatiquement l'URI en JDBC et en déduit
> `ECHEAN_DB_USER` / `ECHEAN_DB_PASSWORD` **uniquement si elles ne sont pas déjà définies**.
> Si `ECHEAN_DB_URL` est fournie, elle est utilisée telle quelle et l'URI `ECHEAN_PSQL_URL`
> est ignorée pour la datasource : vérifiez donc que les deux pointent sur le même hône et
> que `ECHEAN_DB_USER` / `ECHEAN_DB_PASSWORD` correspondent bien au mot de passe de cet hôte.
> Le défaut `db:5432` a été supprimé de l'image : si aucune variable n'est fournie, le
> démarrage échouera explicitement plutôt que de se connecter à un hôte inexistant.

## 3B. Tenir dans les 512 Mo de RAM du plan gratuit Render

Le problème sur Render n'est pas l'espace disque (le WAR ne fait que ~33 Mo et n'est
pas écrit dans un volume persistant), mais la **mémoire vive**. Trois leviers sont
appliqués dans l'image.

### 1. JVM réduite

Valeurs retenues dans le `Dockerfile` après un premier test sur Render :

```
-Xms64m -Xmx160m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=320m
-Xss512k -XX:ActiveProcessorCount=1
-XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=64m
-XX:+UseSerialGC -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true
```

> **Retour d'expérience** : un premier déploiement avec `MaxMetaspaceSize=128m` a
> échoué avec `OutOfMemoryError: Metaspace` pendant la validation des *named queries*
> Hibernate (à ~08:36 dans les logs), puis WildFly a tourné en boucle sur des OOM
> pendant 10 minutes. 128 Mo ne suffisent pas à WildFly 40 + Hibernate 7 ; le tas a
> donc été réduit à 160 Mo pour laisser de la place à la metaspace dans les 512 Mo.

Si vous préférez piloter ce réglage depuis Render : **Dashboard → votre service →
Environment → Add Environment Variable** :

| Clé | Valeur |
|---|---|
| `JAVA_OPTS` | la chaîne complète ci-dessus |

Une variable définie dans Render **remplace** la valeur du `Dockerfile`. L'entrypoint
ajoute ensuite automatiquement `-Djboss.http.port=${PORT}`, ne l'enlevez pas.
`-XX:ActiveProcessorCount=1` évite que WildFly dimensionne ses pools comme s'il
disposait des 8 processeurs visibles dans le conteneur Render (les journaux
montraient 16 threads I/O et 128 threads de tâches). Les flags
`-Djava.net.preferIPv4Stack=true` et `-Djava.awt.headless=true` sont
importants : ils font partie des valeurs par défaut du script `standalone.sh` de
WildFly, perdues dès que `JAVA_OPTS` est surchargé (IPv4 pour le health-check Render,
mode headless pour JasperReports).

### 2. Sous-systèmes WildFly inutilisés retirés

`docker/wildfly/configure.cli` supprime au build : `batch-jberet`, `webservices`,
`mail`, `resource-adapters`, `jdr`, `pojo`, `sar`, `jakarta-data`,
`elytron-oidc-client`, `microprofile-jwt`, `metrics` et `health` (aucun n'est utilisé
par Echean). La datasource H2 d'exemple et son pilote sont aussi retirés, et le pool
JDBC passe de 10 à 5 connexions.

`persistence.xml` désactive en plus le cache Hibernate de 2e niveau : l'infrastructure
Infinispan/JGroups n'est ainsi pas initialisée pour l'application.

À noter : le `standalone.xml` fourni par WildFly 40 **ne contient déjà pas**
`iiop-openjdk` ni `messaging-activemq` (contrairement aux versions précédentes).
Les commandes correspondantes sont donc protégées par un test d’existence dans le
fichier CLI : le build reste compatible si l’image de base change de configuration.

### 3. Démarrer en local

Après avoir renseigné `.env` et préparé la base, démarrez l'application :

```powershell
docker compose up -d --build
docker compose logs -f app
```

Ouvrez `http://localhost:8080/`. Pour arrêter les conteneurs :

```powershell
docker compose down
```

Les données restent dans Neon et ne sont pas supprimées par cette commande.

### 4. Déploiement avec HTTPS

Pointez les enregistrements DNS `A` et, si nécessaire, `AAAA` de `ECHEAN_DOMAIN`
vers l'adresse publique du serveur. Ouvrez les ports TCP 80 et 443 dans le pare-feu,
puis démarrez les deux fichiers Compose :

```powershell
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Caddy obtient et renouvelle le certificat TLS automatiquement. L'application n'est
alors accessible que par Caddy sur le serveur. Consultez les journaux avec
`docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f`.

### 5. Initialiser une base vide

Pour créer le schéma et le premier administrateur dans une base Neon vide, renseignez
`ECHEAN_PSQL_URL`, `ECHEAN_BOOTSTRAP_ADMIN_EMAIL` et
`ECHEAN_BOOTSTRAP_ADMIN_PASSWORD` dans `.env`, puis exécutez depuis Bash ou WSL :

```bash
bash scripts/init-db.sh
```

Le script crée l'administrateur uniquement si la table `users` ne contient encore
aucun compte. Sur une base restaurée, il conserve les comptes existants. Si le schéma
est présent mais doit recevoir une nouvelle migration, appliquez cette migration
explicitement avant de relancer l'application.

### 6. Sauvegarde et diagnostic

Pour exporter une sauvegarde PostgreSQL au format custom :

```powershell
pg_dump --format=custom --no-owner --no-privileges --file=echean-backup.dump "$env:ECHEAN_PSQL_URL"
```

Conservez les sauvegardes et le fichier `.env` dans un emplacement privé et distinct
du dépôt. Pour diagnostiquer un démarrage, vérifiez d'abord les journaux du conteneur
`app`, la disponibilité de Neon et la correspondance entre les identifiants de
`ECHEAN_PSQL_URL` et ceux de `ECHEAN_DB_URL` / `ECHEAN_DB_USER` / `ECHEAN_DB_PASSWORD`.
