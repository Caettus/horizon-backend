FROM quay.io/keycloak/keycloak:26.2

COPY keycloak-custom-realm-issuer-url.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]



