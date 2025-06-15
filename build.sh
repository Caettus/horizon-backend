#!/bin/bash

# Enable Docker BuildKit
export DOCKER_BUILDKIT=1

# Set the registry and image names
REGISTRY="ghcr.io/caettus/horizon-backend"
KEYCLOAK_IMAGE="keycloak"
EVENTSERVICE_IMAGE="eventservice"

# Build and push Keycloak image
echo "Building Keycloak image..."
docker buildx build --platform linux/amd64,linux/arm64 \
  -t ${REGISTRY}/${KEYCLOAK_IMAGE}:latest \
  -f keycloak/Dockerfile \
  --push \
  keycloak/

# Build and push Event Service image
echo "Building Event Service image..."
docker buildx build --platform linux/amd64,linux/arm64 \
  -t ${REGISTRY}/${EVENTSERVICE_IMAGE}:latest \
  -f eventservice/Dockerfile \
  --push \
  eventservice/

echo "Build complete!" 