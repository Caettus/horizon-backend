FROM quay.io/keycloak/keycloak:26.2

# Zet je custom mapper in de juiste folder
COPY custom-mappers/keycloak-custom-realm-issuer-url.jar /opt/keycloak/providers/

# Belangrijk: opnieuw builden van providers
RUN /opt/keycloak/bin/kc.sh build

# Development mode starten zoals je gewend bent
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]

