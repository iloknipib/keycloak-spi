FROM maven:3.6.0-jdk-11-slim AS build
COPY dehaat-keycloak-spi/src /home/app/src
COPY dehaat-keycloak-spi/pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package


FROM jboss/keycloak:15.0.1
LABEL maintainer="Subhash Samota <subhash.samota@agrevolution.in>"
COPY --from=build /home/app/target/dehaat-keycloak-spi-1.0.jar /opt/jboss/keycloak/standalone/deployments/
RUN curl -L https://github.com/aerogear/keycloak-metrics-spi/releases/download/1.0.4/keycloak-metrics-spi-1.0.4.jar --output /opt/jboss/keycloak/standalone/deployments/keycloak-metrics-spi-1.0.4.jar
