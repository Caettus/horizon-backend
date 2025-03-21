# ------------------------
# STAGE 1: Build
# ------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

# Maak een map en kopieer projectbestanden
WORKDIR /app
COPY . /app

# Set JAVA_HOME explicitly
ENV JAVA_HOME /opt/java/openjdk
RUN export JAVA_HOME

# Geef gradlew uitvoerrechten (fix mogelijke rechtenfout)
RUN chmod +x /app/gradlew

# Bouw de jar
RUN ./gradlew clean build -x test --no-daemon

# ------------------------
# STAGE 2: Run
# ------------------------
FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp

# Neem de jar uit de builder-stage
COPY --from=builder /app/build/libs/*.jar userservice.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/userservice.jar"]
