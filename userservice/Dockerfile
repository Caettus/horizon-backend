# ------------------------
# STAGE 1: Build
# ------------------------
FROM gradle:7.6-jdk21-alpine AS builder

# Maak een map en kopieer projectbestanden
WORKDIR /app
COPY . /app

# Bouw de jar
RUN gradle clean build -x test --no-daemon

# ------------------------
# STAGE 2: Run
# ------------------------
FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp

# Neem de jar uit de builder-stage
COPY --from=builder /app/build/libs/*.jar userservice.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/userservice.jar"]
