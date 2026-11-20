# ---- Étape 1 : compilation du WAR et récupération du pilote PostgreSQL ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package \
 && mvn -q dependency:get -Dartifact=org.postgresql:postgresql:42.7.14 \
 && cp /root/.m2/repository/org/postgresql/postgresql/42.7.14/postgresql-42.7.14.jar /tmp/postgresql.jar

# ---- Étape 2 : image WildFly 40.0.1 (Jakarta EE 11 sur Java 17) ----
FROM quay.io/wildfly/wildfly:40.0.1.Final-jdk17

# Profil mémoire contraint pour Render Free (512 Mo).
# Totaux visés : heap 192 Mo + metaspace 160 Mo + code cache 48 Mo
# + mémoire directe 32 Mo + threads (~2 Mo) + natif/divers (~40 à 60 Mo)
# ≈ 454 à 494 Mo de marge sous 512 Mo.
# JAVA_OPTS reste surchargeable par l'environnement de la plateforme.
ENV JAVA_OPTS="-Xms64m -Xmx192m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Africa/Porto-Novo -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true"

# Module JDBC PostgreSQL (le WAR n'embarque pas le pilote : il est fourni par le serveur).
COPY --from=build /tmp/postgresql.jar /tmp/postgresql.jar
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --command="module add --name=org.postgresql --resources=/tmp/postgresql.jar --dependencies=java.logging,java.sql,java.xml"

# Source de données java:jboss/datasources/echeanDS (valeurs injectées par les variables d'environnement).
COPY docker/wildfly/configure.cli /tmp/configure.cli
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/configure.cli

# Déploiement enraciné : l'application répond sur / au lieu de /JSF-1.0-SNAPSHOT.
COPY --chown=jboss:root --from=build /build/target/JSF-1.0-SNAPSHOT.war \
     /opt/jboss/wildfly/standalone/deployments/ROOT.war

# Normalise la DB au démarrage : accepte ECHEAN_DB_URL (JDBC) ou l'URI Neon
# ECHEAN_PSQL_URL (postgresql://user:pass@host/db?sslmode=require).
COPY --chown=jboss:jboss --chmod=0755 docker/wildfly/entrypoint.sh /opt/jboss/entrypoint.sh
ENTRYPOINT ["/opt/jboss/entrypoint.sh"]
# -b 0.0.0.0             : application sur IPv4 (détectée par le health-check Render)
# -bmanagement 127.0.0.1 : console HAL jamais exposée publiquement
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "127.0.0.1"]

EXPOSE 10000
