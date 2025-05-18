# ---- STAGE 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# kopieert gradle stuff
COPY gradlew .
COPY gradle/ ./gradle/

# kopieert source files
COPY . .

# install bash & sed (sed comes with busybox so you can skip this one)
# RUN apk add --no-cache bash # bash is not strictly needed if not using bash-specific commands

# remove CR characters from gradlew
RUN sed -i 's/\r$//' gradlew

# maakt de gradlew script uitvoerbaar
RUN chmod +x gradlew

# runt gradle build
RUN ./gradlew clean build -x test --no-daemon

# ---- STAGE 2: Run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# kopieert jar file van build stage
COPY --from=builder /app/build/libs/*.jar rsvpservice.jar

# exposed poort waar de service op draait
EXPOSE 8084

# start de jar file
ENTRYPOINT ["java", "-jar", "/app/rsvpservice.jar"] 