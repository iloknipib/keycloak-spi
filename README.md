# keycloak-spi
All additional functionality over default keycloak should reside here

## Build

Pre-requisites
Java 8 and Maven-3.5 

```
cd dehaat-keycloak-spi
mvn clean install
```

## Deployment

- Set the value `ENV`, `APP_HASHCODE_DEHAAT_BUSINESS`, `APP_HASHCODE_DEHAAT_FARMER`, `MAILMAN_HOST`, `MAILMAN_SEND_TOKEN` in environment variable
- Find the jar `dehaat-keycloak-spi-1.0.jar` in target folder after compilation
- Copy the jar in `keycloak-15.0.2/standalone/deployments/`
