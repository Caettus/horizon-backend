FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .

# remove CR characters from gradlew
RUN sed -i 's/\r$//' gradlew

RUN chmod +x gradlew
RUN ./gradlew clean build -x test --no-daemon

# ---- STAGE 2: Run ----
FROM --platform=$TARGETPLATFORM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar eventservice.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/eventservice.jar"]