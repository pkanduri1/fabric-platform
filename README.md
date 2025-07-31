# 🏗️ Fabric Platform

**Enterprise Data Loading and Batch Processing Platform**

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](./fabric-core)
[![Java Version](https://img.shields.io/badge/java-17%2B-blue)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.4.x-brightgreen)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/react-18.x-blue)](https://reactjs.org/)
[![License](https://img.shields.io/badge/license-Proprietary-red)](#)

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Environment Setup](#environment-setup)
- [Starting & Stopping](#starting--stopping)
- [Database Configuration](#database-configuration)
- [Development](#development)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## 🎯 Overview

The Fabric Platform is an enterprise-grade data loading and batch processing system designed for high-volume financial data processing. It provides secure, scalable, and compliant data transformation capabilities with comprehensive audit trails and real-time monitoring.

### Key Features

- 🔐 **Enterprise Security**: JWT authentication, LDAP integration, role-based access control
- 📊 **Batch Processing**: High-performance Spring Batch with parallel processing
- 🗄️ **Multi-Database Support**: Oracle, PostgreSQL, MySQL, MongoDB, Redis
- ☁️ **Cloud Ready**: AWS RDS, S3, ElastiCache integration
- 📈 **Monitoring**: Prometheus metrics, health checks, distributed tracing
- 🎨 **Modern UI**: React 18 with Material-UI and drag-and-drop configuration
- 📝 **Audit Compliance**: Comprehensive audit trails for regulatory compliance

## 🏛️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   React UI      │◄──►│   Spring Boot    │◄──►│   Oracle DB     │
│   - Config Mgmt │    │   - REST API     │    │   - Data Store  │
│   - Monitoring  │    │   - Batch Jobs   │    │   - Audit Logs  │
│   - Auth        │    │   - Security     │    │   - User Mgmt   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌──────────────────┐
                    │   Redis Cache    │
                    │   - Sessions     │
                    │   - Rate Limits  │
                    │   - Job Queue    │
                    └──────────────────┘
```

### Module Structure

- **fabric-core**: Parent module with shared configuration
- **fabric-api**: REST API and web controllers
- **fabric-batch**: Spring Batch processing engine
- **fabric-data-loader**: Data loading and validation framework
- **fabric-utils**: Common utilities and models
- **fabric-ui**: React frontend application

## 🚀 Quick Start

### Prerequisites

- **Java**: 17 or higher
- **Maven**: 3.8.x or higher
- **Node.js**: 18.x or higher
- **Database**: Oracle 19c+ / PostgreSQL 13+ / MySQL 8+
- **Redis**: 6.x or higher (optional)

### 1. Clone and Build

```bash
# Clone the repository
git clone <repository-url>
cd fabric-platform

# Build the backend
cd fabric-core
mvn clean install

# Build the frontend (if exists)
cd ../fabric-ui
npm install
npm run build
```

### 2. Database Setup

```bash
# Oracle (Default)
export DB_URL="jdbc:oracle:thin:@localhost:1521:XE"
export DB_USERNAME="fabric_user"
export DB_PASSWORD="your_password"

# Run database migrations
cd fabric-core/fabric-api
mvn flyway:migrate
```

### 3. Start the Application

```bash
# Start backend (Development mode)
cd fabric-core/fabric-api
mvn spring-boot:run

# Start frontend (if separate)
cd fabric-ui
npm start
```

### 4. Access the Application

- **Backend API**: http://localhost:8080/api
- **Frontend UI**: http://localhost:3000
- **API Documentation**: http://localhost:8080/api/swagger-ui.html
- **Health Check**: http://localhost:8080/api/actuator/health

## 🔧 Environment Setup

### Environment Variables

Create a `.env` file or set system environment variables:

```bash
# Database Configuration
export DB_URL="jdbc:oracle:thin:@localhost:1521:XE"
export DB_USERNAME="fabric_user"
export DB_PASSWORD="secure_password"

# Security Configuration
export JWT_SECRET="your-256-bit-secret-key-here"
export LDAP_URLS="ldap://localhost:389"
export LDAP_BASE_DN="dc=company,dc=com"

# Redis Configuration (Optional)
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_PASSWORD=""

# Application Configuration
export SPRING_PROFILES_ACTIVE="dev"
export SERVER_PORT="8080"
export LOG_LEVEL_ROOT="INFO"
```

### Profile Selection

The application supports multiple profiles for different environments:

```bash
# Development (Default)
export SPRING_PROFILES_ACTIVE="dev"

# Testing
export SPRING_PROFILES_ACTIVE="test"

# Staging
export SPRING_PROFILES_ACTIVE="staging"

# Production
export SPRING_PROFILES_ACTIVE="prod"

# Database-specific profiles
export SPRING_PROFILES_ACTIVE="aws,postgresql"  # AWS RDS PostgreSQL
export SPRING_PROFILES_ACTIVE="mongodb"         # MongoDB document store
export SPRING_PROFILES_ACTIVE="dev,redis"       # Development with Redis
```

## 🚦 Starting & Stopping

### Backend (Spring Boot)

#### Start Backend

```bash
# Method 1: Maven (Development)
cd fabric-core/fabric-api
mvn spring-boot:run

# Method 2: Java JAR (Production)
cd fabric-core/fabric-api
mvn clean package
java -jar target/fabric-api-*.jar

# Method 3: With specific profile
java -jar target/fabric-api-*.jar --spring.profiles.active=prod

# Method 4: With environment variables
export SPRING_PROFILES_ACTIVE=staging
export DB_URL=jdbc:oracle:thin:@prod-db:1521:PROD
java -jar target/fabric-api-*.jar

# Method 5: Docker (if available)
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:oracle:thin:@db:1521:PROD \
  fabric-platform:latest
```

#### Stop Backend

```bash
# Method 1: Graceful shutdown (Ctrl+C in terminal)
# Press Ctrl+C to initiate graceful shutdown

# Method 2: Kill process by port
lsof -ti :8080 | xargs kill -TERM

# Method 3: Kill Java process
pkill -f "fabric-api"

# Method 4: Docker stop
docker stop fabric-platform-container
```

### Frontend (React)

#### Start Frontend

```bash
# Method 1: Development server
cd fabric-ui
npm start
# Starts on http://localhost:3000

# Method 2: Production build + serve
npm run build
npm install -g serve
serve -s build -l 3000

# Method 3: With environment variables
REACT_APP_API_URL=http://localhost:8080/api npm start

# Method 4: Docker
docker run -p 3000:3000 fabric-ui:latest
```

#### Stop Frontend

```bash
# Method 1: Stop development server (Ctrl+C)
# Press Ctrl+C in the terminal running npm start

# Method 2: Kill by port
lsof -ti :3000 | xargs kill -TERM

# Method 3: Stop all Node processes (careful!)
pkill -f "node"
```

### Complete System Start/Stop

#### Start Complete System

```bash
# Option 1: Manual start (recommended for development)
# Terminal 1 - Backend
cd fabric-core/fabric-api
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run

# Terminal 2 - Frontend (if separate)
cd fabric-ui
npm start

# Option 2: Using scripts (create these scripts)
./scripts/start-all.sh

# Option 3: Docker Compose (if available)
docker-compose up -d
```

#### Stop Complete System

```bash
# Option 1: Manual stop
# Press Ctrl+C in both terminals

# Option 2: Kill by ports
lsof -ti :8080 | xargs kill -TERM  # Backend
lsof -ti :3000 | xargs kill -TERM  # Frontend

# Option 3: Using scripts
./scripts/stop-all.sh

# Option 4: Docker Compose
docker-compose down
```

## 🗄️ Database Configuration

### Supported Databases

#### Oracle Database (Default)

```yaml
# application-dev.yml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:oracle:thin:@localhost:1521:XE
    username: fabric_dev
    password: fabric_dev_pass
    driver-class-name: oracle.jdbc.OracleDriver
```

#### PostgreSQL

```bash
# Start with PostgreSQL profile
export SPRING_PROFILES_ACTIVE="aws,postgresql"
export AWS_RDS_URL="jdbc:postgresql://localhost:5432/fabric"
export AWS_RDS_USERNAME="fabric_user"
export AWS_RDS_PASSWORD="secure_password"
```

#### MongoDB

```bash
# Start with MongoDB profile
export SPRING_PROFILES_ACTIVE="mongodb"
export MONGODB_URI="mongodb://localhost:27017/fabric_platform"
```

#### MySQL

```bash
# Start with MySQL profile
export SPRING_PROFILES_ACTIVE="aws,mysql"
export AWS_RDS_MYSQL_URL="jdbc:mysql://localhost:3306/fabric"
export AWS_RDS_MYSQL_USERNAME="fabric_user"
export AWS_RDS_MYSQL_PASSWORD="secure_password"
```

### Database Setup Scripts

```bash
# Oracle setup
sqlplus sys/password@localhost:1521/XE as sysdba @fabric-core/fabric-api/src/main/resources/sql/ddl/

# PostgreSQL setup
psql -h localhost -U postgres -d fabric -f fabric-core/fabric-api/src/main/resources/sql/ddl/postgresql-schema.sql

# MySQL setup
mysql -h localhost -u root -p fabric < fabric-core/fabric-api/src/main/resources/sql/ddl/mysql-schema.sql
```

## 💻 Development

### Development Environment Setup

```bash
# 1. Set up development database
export SPRING_PROFILES_ACTIVE="dev"
export DB_URL="jdbc:oracle:thin:@localhost:1521:XE"
export DB_USERNAME="fabric_dev"
export DB_PASSWORD="fabric_dev_pass"

# 2. Enable debug logging
export LOG_LEVEL_FABRIC="DEBUG"
export LOG_LEVEL_SECURITY="DEBUG"

# 3. Disable CSRF for development
export CSRF_ENABLED="false"

# 4. Start with hot reload
cd fabric-core/fabric-api
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"
```

### IDE Configuration

#### IntelliJ IDEA

1. Import as Maven project
2. Set Project SDK to Java 17+
3. Enable annotation processing for Lombok
4. Configure run configuration:
   - Main class: `com.truist.batch.InterfaceBatchApplication`
   - VM options: `-Dspring.profiles.active=dev`
   - Environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

#### VS Code

1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Create `.vscode/launch.json`:

```json
{
    "type": "java",
    "name": "Fabric Platform",
    "request": "launch",
    "mainClass": "com.truist.batch.InterfaceBatchApplication",
    "projectName": "fabric-api",
    "env": {
        "SPRING_PROFILES_ACTIVE": "dev",
        "DB_URL": "jdbc:oracle:thin:@localhost:1521:XE"
    }
}
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test profile
mvn test -Dspring.profiles.active=test

# Run integration tests with Oracle
mvn test -Dspring.profiles.active=test,oracle-integration

# Run with coverage
mvn test jacoco:report
```

## 🚀 Deployment

### Production Deployment

#### Environment Variables (Required)

```bash
# Database (Required)
export DB_URL="jdbc:oracle:thin:@prod-oracle:1521:PROD"
export DB_USERNAME="fabric_prod"
export DB_PASSWORD="encrypted_password"

# Security (Required)
export JWT_SECRET="512-bit-production-secret-key"
export LDAP_URLS="ldaps://prod-ldap:636"
export LDAP_BASE_DN="dc=company,dc=com"
export LDAP_BIND_DN="cn=service,ou=accounts,dc=company,dc=com"
export LDAP_BIND_PASSWORD="encrypted_ldap_password"

# SSL (Required for production)
export SSL_ENABLED="true"
export SSL_KEYSTORE_PATH="/app/certs/keystore.p12"
export SSL_KEYSTORE_PASSWORD="keystore_password"

# Application
export SPRING_PROFILES_ACTIVE="prod"
export SERVER_PORT="8443"
```

#### Deployment Steps

```bash
# 1. Build production artifact
mvn clean package -Pprod

# 2. Create deployment directory
mkdir -p /app/fabric-platform
cp target/fabric-api-*.jar /app/fabric-platform/
cp -r config/ /app/fabric-platform/

# 3. Set up systemd service (Linux)
sudo cp scripts/fabric-platform.service /etc/systemd/system/
sudo systemctl enable fabric-platform
sudo systemctl start fabric-platform

# 4. Verify deployment
curl -k https://localhost:8443/api/actuator/health
```

#### Docker Deployment

```dockerfile
# Dockerfile
FROM openjdk:17-jre-slim
VOLUME /tmp
COPY target/fabric-api-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
# Build and run
docker build -t fabric-platform:latest .
docker run -d -p 8443:8443 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:oracle:thin:@db:1521:PROD \
  --name fabric-platform \
  fabric-platform:latest
```

### Kubernetes Deployment

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: fabric-platform
spec:
  replicas: 3
  selector:
    matchLabels:
      app: fabric-platform
  template:
    metadata:
      labels:
        app: fabric-platform
    spec:
      containers:
      - name: fabric-platform
        image: fabric-platform:latest
        ports:
        - containerPort: 8443
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_URL
          valueFrom:
            secretKeyRef:
              name: fabric-secrets
              key: db-url
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Database Connection Issues

```bash
# Check database connectivity
telnet localhost 1521

# Verify credentials
sqlplus username/password@localhost:1521/XE

# Check application logs
tail -f logs/fabric-platform.log | grep -i "database\\|connection"
```

#### 2. Authentication Problems

```bash
# Check LDAP connectivity
ldapsearch -x -H ldap://localhost:389 -D "cn=admin" -W

# Verify JWT configuration
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```

#### 3. Memory Issues

```bash
# Increase JVM memory
export JAVA_OPTS="-Xmx2g -Xms1g"
java $JAVA_OPTS -jar target/fabric-api-*.jar

# Monitor memory usage
jstat -gc -t $(jps | grep fabric | cut -d' ' -f1) 5s
```

#### 4. Port Conflicts

```bash
# Check what's using port 8080
lsof -i :8080
netstat -tulpn | grep :8080

# Use different port
export SERVER_PORT=8081
java -jar target/fabric-api-*.jar
```

### Logging and Monitoring

```bash
# Enable debug logging
export LOG_LEVEL_ROOT="DEBUG"

# View application logs
tail -f logs/fabric-platform.log

# Check health endpoint
curl http://localhost:8080/api/actuator/health

# View metrics
curl http://localhost:8080/api/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/api/actuator/prometheus
```

### Performance Tuning

```bash
# Database connection pool tuning
export HIKARI_MAXIMUM_POOL_SIZE=50
export HIKARI_MINIMUM_IDLE=10

# Batch processing tuning  
export BATCH_CHUNK_SIZE=5000
export DATA_LOADER_MAX_THREADS=16

# JVM tuning
export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

## 🤝 Contributing

### Development Workflow

1. Create feature branch: `git checkout -b feature/new-feature`
2. Make changes and test locally
3. Run tests: `mvn test`
4. Build: `mvn clean package`
5. Create pull request

### Code Standards

- Java 17+ features
- Spring Boot best practices
- Comprehensive unit tests (>80% coverage)
- Security-first development
- Proper error handling and logging

### Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=JwtTokenServiceTest

# Run integration tests
mvn verify -Pfailsafe
```

---

## 📞 Support

For support and questions:

- **Internal Wiki**: [Fabric Platform Documentation](https://wiki.company.com/fabric)
- **Issue Tracking**: [JIRA Project](https://jira.company.com/FABRIC)
- **Team Contact**: fabric-platform-team@company.com

---

**© 2025 Truist Financial Corporation. All rights reserved.**