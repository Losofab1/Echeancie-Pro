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

# Module JDBC PostgreSQL (le WAR n'embarque pas le pilote : il est fourni par le serveur).
COPY --from=build /tmp/postgresql.jar /tmp/postgresql.jar
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --command="module add --name=org.postgresql --resources=/tmp/postgresql.jar --dependencies=java.logging,java.sql,java.xml"

# Source de données java:jboss/datasources/echeanDS (valeurs injectées par les variables d'environnement).
COPY docker/wildfly/configure.cli /tmp/configure.cli
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/configure.cli

# Déploiement enraciné : l'application répond sur / au lieu de /JSF-1.0-SNAPSHOT.
COPY --chown=jboss:root --from=build /build/target/JSF-1.0-SNAPSHOT.war \
     /opt/jboss/wildfly/standalone/deployments/ROOT.war

ENV ECHEAN_DB_URL=jdbc:postgresql://db:5432/echean \
    ECHEAN_DB_USER=echean \
    ECHEAN_DB_PASSWORD=change-me

EXPOSE 8080
