FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .

# install bash & sed (sed comes with busybox so you can skip this one)
RUN apk add --no-cache bash

# remove CR characters from gradlew
RUN sed -i 's/\r$//' gradlew

RUN chmod +x gradlew
RUN ./gradlew clean build -x test --no-daemon



# ---- STAGE 2: Run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar eventservice.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/userservice.jar"]