# Backend Spring Boot application Dockerfile (explicit build instead of Jib)
FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/oci-0.0.1-SNAPSHOT.jar
WORKDIR /app
COPY ${JAR_FILE} app.jar
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

