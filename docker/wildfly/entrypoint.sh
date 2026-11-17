#!/bin/sh
# Normalise la configuration de la base au démarrage du conteneur.
#
# WildFly attend une URL JDBC dans ECHEAN_DB_URL (voir docker/wildfly/configure.cli).
# Sur certaines plateformes (Render, etc.) seule l'URI Neon est fournie :
#   ECHEAN_PSQL_URL=postgresql://user:pass@host/db?sslmode=require
# Dans ce cas, on dérive l'URL JDBC et les identifiants avant de démarrer le serveur.

set -e

if [ -z "${ECHEAN_DB_URL:-}" ] && [ -n "${ECHEAN_PSQL_URL:-}" ]; then
    uri=${ECHEAN_PSQL_URL}

    # Retire le schéma : postgresql:// ou postgres://
    rest=${uri#*://}

    case "$rest" in
        *@*) ;;
        *)
            echo "ECHEAN_PSQL_URL invalide : attendu postgresql://user:pass@host/db" >&2
            exit 1
            ;;
    esac

    # user:pass@host:port/db?params
    userinfo=${rest%%@*}
    hostpart=${rest#*@}

    # host:port/db et query string
    hostdb=${hostpart%%\?*}
    query=${hostpart#*\?}
    if [ "$query" = "$hostpart" ]; then
        query=""
    fi

    # pgjdbc attend « channelBinding » (camelCase) et rejette le paramètre
    # « channel_binding » utilisé par psql : on le retire de l'URL JDBC.
    query=$(printf '%s' "$query" \
        | sed 's/&channel_binding=[^&]*//; s/^channel_binding=[^&]*&//; s/^channel_binding=[^&]*$//')

    hostport=${hostdb%/*}
    database=${hostdb##*/}

    if [ -z "${ECHEAN_DB_USER:-}" ]; then
        ECHEAN_DB_USER=${userinfo%%:*}
    fi
    if [ -z "${ECHEAN_DB_PASSWORD:-}" ]; then
        ECHEAN_DB_PASSWORD=${userinfo#*:}
    fi

    if [ -n "$query" ]; then
        ECHEAN_DB_URL="jdbc:postgresql://${hostport}/${database}?${query}"
    else
        ECHEAN_DB_URL="jdbc:postgresql://${hostport}/${database}"
    fi

    export ECHEAN_DB_URL ECHEAN_DB_USER ECHEAN_DB_PASSWORD
fi

# Render (et d'autres plateformes) injecte PORT : WildFly doit écouter dessus.
# standalone.xml utilise ${jboss.http.port:8080} pour le socket-binding http.
if [ -n "${PORT:-}" ]; then
    JAVA_OPTS="${JAVA_OPTS:-} -Djboss.http.port=${PORT}"
    export JAVA_OPTS
    echo "Binding HTTP sur le port ${PORT} (variable PORT)."
fi

# Chaîne l'entrypoint de l'image de base (import des certificats CA, nécessaire
# pour les connexions TLS vers Neon). Il se termine lui-même par un « exec "$@" ».
if [ -x /__cacert_entrypoint.sh ]; then
    /__cacert_entrypoint.sh "$@"
else
    exec "$@"
fi
