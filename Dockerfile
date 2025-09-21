# Multi-stage Dockerfile for Spring Boot application

# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Install maven
RUN apk add --no-cache maven

# Copy Maven configuration and source
COPY pom.xml .
COPY src src

# Build the application
RUN mvn clean package -DskipTests --no-transfer-progress

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership of the app directory to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose the port the app runs on
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]