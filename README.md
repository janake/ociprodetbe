# OCI Prodet Backend

A Spring Boot application with Keycloak OAuth2 integration and comprehensive CI/CD pipeline.

## Features

- Spring Boot 3.5.6 with Java 17
- OAuth2 authentication with Keycloak
- REST API with security
- Docker containerization
- Automated CI/CD pipeline

## Development

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Docker (optional)

### Running locally

```bash
# Build and run tests
./mvnw clean test

# Run the application
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package
java -jar target/*.jar
```

### Configuration

The application requires Keycloak configuration. See `application.properties` for details:

- `KEYCLOAK_CLIENT_ID`: OAuth2 client ID (default: ociprodet)
- `KEYCLOAK_CLIENT_AUTH_METHOD`: Authentication method (default: none)

## Docker

### Build and run with Docker

```bash
# Build Docker image
docker build -t ociprodet-backend .

# Run with Docker Compose
docker-compose up -d
```

## CI/CD Pipeline

The project includes a comprehensive GitHub Actions CI/CD pipeline with the following stages:

### 1. Test and Build
- Runs on all pushes and pull requests
- Java 17 setup with Maven caching
- Executes tests and generates reports
- Builds application JAR
- Uploads build artifacts

### 2. Security Scan
- OWASP dependency vulnerability scanning
- Uploads security scan results
- Configurable to fail on high-severity vulnerabilities

### 3. Docker Build
- Builds and pushes Docker images to GitHub Container Registry
- Only on master/main branch pushes
- Multi-platform support with caching
- Automated image tagging

### 4. Deployment
- **Staging**: Deploys on develop/master/main branch pushes
- **Production**: Deploys only on master/main branch pushes
- Uses GitHub Environments for approval workflows
- Deployment scripts in `scripts/deploy.sh`

### Pipeline Configuration

The pipeline supports several customization options:

- **Security scanning**: Configure vulnerability threshold in `pom.xml`
- **Environments**: Set up GitHub Environments for staging/production
- **Secrets**: Configure deployment secrets in repository settings
- **Notifications**: Add Slack/Teams notifications as needed

### Triggering the Pipeline

The pipeline runs automatically on:
- Push to master, main, or develop branches
- Pull requests to master or main
- Manual trigger via GitHub Actions UI

## API Endpoints

- `GET /api/teszt` - Test endpoint (requires authentication)
- `GET /actuator/health` - Health check endpoint
- `POST /logout` - Logout endpoint

## Security

- OAuth2 with Keycloak integration
- JWT token validation
- CORS configuration for frontend integration
- Security tests included

## Monitoring

- Spring Boot Actuator endpoints
- Docker health checks
- Application logging with request filtering