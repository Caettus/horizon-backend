# ---- STAGE 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean build -x test --no-daemon

# ---- STAGE 2: Run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar userservice.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/userservice.jar"]
