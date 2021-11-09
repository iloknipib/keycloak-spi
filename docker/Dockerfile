FROM maven:3.6.0-jdk-11-slim AS build
COPY dehaat-keycloak-spi/src /home/app/src
COPY dehaat-keycloak-spi/pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package


FROM jboss/keycloak:15.0.1
LABEL maintainer="Subhash Samota <subhash.samota@agrevolution.in>"
RUN touch /opt/jboss/keycloak/standalone/deployments/dehaat.dodeploy
COPY --from=build /home/app/target/dehaat-keycloak-spi-1.0.jar /opt/jboss/keycloak/standalone/deployments/dehaat.jar
