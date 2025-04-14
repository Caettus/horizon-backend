# ---- STAGE 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper files to ensure they exist
COPY gradlew .
COPY gradle/ ./gradle/

# Now copy the rest of your project source files
# (This assumes your Docker build context is set to the project root)
COPY . .

# If you only need application.properties earlier, you can leave it,
# but generally copying everything simplifies the build:
# COPY src/main/resources/application.properties /app/application.properties

# Make the gradlew script executable
RUN chmod +x gradlew

# Run the Gradle build (excluding tests for speed/if desired)
RUN ./gradlew clean build -x test --no-daemon

# ---- STAGE 2: Run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Note: Adjust the jar filename if it’s not exactly matching *.jar
COPY --from=builder /app/build/libs/*.jar userservice.jar

# Expose port 8081 since your server.port is set to 8081 in application.properties
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/userservice.jar"]
