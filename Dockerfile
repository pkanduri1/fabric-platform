# =============================================================================
# Multi-stage Dockerfile for Fabric Platform
# =============================================================================
# 
# This Dockerfile builds both the React frontend and Spring Boot backend
# into a single optimized container for production deployment.
#
# Build stages:
# 1. frontend-build: Builds React application
# 2. backend-build: Builds Spring Boot application
# 3. runtime: Final production image
#
# Usage:
#   docker build -t fabric-platform:latest .
#   docker run -p 8080:8080 fabric-platform:latest
#
# =============================================================================

# Stage 1: Build React Frontend
FROM node:18-alpine AS frontend-build

WORKDIR /app/frontend

# Copy package files for dependency installation
COPY fabric-ui/package*.json ./
RUN npm ci --only=production

# Copy source code and build
COPY fabric-ui/ ./
RUN npm run build

# Stage 2: Build Spring Boot Backend
FROM maven:3.8.6-openjdk-17 AS backend-build

WORKDIR /app/backend

# Copy Maven configuration
COPY fabric-core/pom.xml ./
COPY fabric-core/fabric-api/pom.xml ./fabric-api/
COPY fabric-core/fabric-batch/pom.xml ./fabric-batch/
COPY fabric-core/fabric-data-loader/pom.xml ./fabric-data-loader/
COPY fabric-core/fabric-utils/pom.xml ./fabric-utils/

# Download dependencies (this layer will be cached)
RUN mvn dependency:go-offline -B

# Copy source code
COPY fabric-core/ ./

# Copy frontend build artifacts to serve from Spring Boot
COPY --from=frontend-build /app/frontend/build ./fabric-api/src/main/resources/static/

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 3: Runtime Image
FROM eclipse-temurin:17-jre-alpine AS runtime

# Create non-root user for security
RUN addgroup -S fabric && adduser -S fabric -G fabric

# Install required packages
RUN apk add --no-cache \
    curl \
    dumb-init \
    && rm -rf /var/cache/apk/*

# Set working directory
WORKDIR /app

# Copy application jar
COPY --from=backend-build /app/backend/fabric-api/target/fabric-api-*.jar app.jar

# Create necessary directories
RUN mkdir -p /app/logs /app/data /app/temp && \
    chown -R fabric:fabric /app

# Switch to non-root user
USER fabric

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose ports
EXPOSE 8080

# Set JVM options for container environment
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication"

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start the application
CMD ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=docker -jar app.jar"]

# Metadata
LABEL maintainer="Fabric Platform Team"
LABEL version="1.0"
LABEL description="Fabric Platform - Enterprise Batch Processing with Real-Time Monitoring"
LABEL component="fabric-platform"
LABEL tier="backend"