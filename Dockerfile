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

# Profil mémoire pour le plan gratuit Render (512 Mo RAM au total).
# Ces valeurs sont des DEFAUTS : une variable JAVA_OPTS définie dans le
# tableau de bord Render les remplace automatiquement.
#   -Xms64m / -Xmx160m : tas volontairement réduit (WildFly + Hibernate 7
#                        sont gourmands en metaspace, pas en tas ici)
#   Metaspace 320m : mesure réelle sur Render : 128m -> OutOfMemoryError
#                    pendant la validation des named queries Hibernate
#   -Xss512k          : piles de threads réduites
#   -XX:ActiveProcessorCount=1 : Render expose 8 processeurs à la JVM, ce qui
#                        crée inutilement 16 threads I/O et 128 threads de tâches
#   CodeCache 64m / DirectMemory 64m : bornes natives, sinon JVM réserve plus
#   UseSerialGC       : 1 vCPU sur Render → moins de threads/overhead que G1
#   Les 2 derniers flags reproduisent les défauts WildFly écrasés par JAVA_OPTS
#   (IPv4 obligatoire pour le health-check Render, headless pour JasperReports).
ENV JAVA_OPTS="-Xms64m -Xmx160m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=320m -Xss512k -XX:ActiveProcessorCount=1 -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=64m -XX:+UseSerialGC -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true"

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

EXPOSE 8080
