# ---- STAGE 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
    
# Copy Gradle wrapper files first to maximise cache hits
COPY gradlew .
COPY gradle/ ./gradle/
    
# Copy the rest of the source
COPY . .
    
# Convert Windows CRLF → LF and make the wrapper executable
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
    
# Build the application (skip tests for faster image builds)
RUN ./gradlew clean build -x test --no-daemon
    
# ---- STAGE 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
    
# Copy the fat jar from the builder stage
COPY --from=builder /app/build/libs/*.jar apigateway.jar
    
# Service port
EXPOSE 8080
    
# Start the application
ENTRYPOINT ["java", "-jar", "/app/apigateway.jar"]
    