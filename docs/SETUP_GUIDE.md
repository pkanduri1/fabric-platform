# Fabric Platform - Development Setup Guide

## Prerequisites

| Software | Version | Purpose |
|----------|---------|---------|
| Java JDK | 17+ | Backend runtime |
| Maven | 3.8+ | Backend build tool |
| Node.js | 18+ | Frontend runtime |
| npm | 9+ | Frontend package manager |
| Docker | 20+ | Oracle DB and OpenLDAP containers |
| Git | 2.30+ | Version control |

## 1. Oracle Database Setup

The platform requires Oracle Database. Use the Docker image for local development.

```bash
# Pull and run Oracle Free container
docker run -d \
  --name fabric-oracle-free \
  -p 1522:1521 \
  -e ORACLE_PWD=MySecurePass123 \
  container-registry.oracle.com/database/free:latest

# Wait for container to be ready (~2 minutes)
docker logs -f fabric-oracle-free
# Look for: "DATABASE IS READY TO USE!"

# Create the CM3INT schema (first time only)
docker exec -i fabric-oracle-free sqlplus sys/MySecurePass123@//localhost:1521/FREEPDB1 as sysdba <<EOF
CREATE USER cm3int IDENTIFIED BY MySecurePass123;
GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE TRIGGER TO cm3int;
ALTER USER cm3int QUOTA UNLIMITED ON USERS;
EOF
```

**Connection Details:**

| Property | Value |
|----------|-------|
| Host | localhost |
| Port | 1522 |
| Service | FREEPDB1 |
| Username | cm3int |
| JDBC URL | `jdbc:oracle:thin:@localhost:1522/FREEPDB1` |

## 2. Backend Setup

```bash
# Navigate to the backend parent module
cd fabric-core

# Run Liquibase migrations to create/update schema
mvn liquibase:update -pl fabric-api

# Build all modules (skip tests for first-time setup)
mvn clean install -DskipTests

# Run the Spring Boot application with local profile
mvn spring-boot:run -pl fabric-api -Dspring-boot.run.profiles=local
```

The backend starts on **port 8080**.

**Verify:**
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

## 3. Frontend Setup

```bash
# Navigate to frontend directory
cd fabric-ui

# Install dependencies
npm install

# Install Playwright browsers (for E2E testing)
npx playwright install chromium

# Start development server
npm start
```

The frontend starts on **port 3000** and proxies API calls to `localhost:8080`.

## 4. Environment Configuration

### Backend (`fabric-core/fabric-api/src/main/resources/application-local.properties`)

This file is gitignored. Create it with:

```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1522/FREEPDB1
spring.datasource.username=cm3int
spring.datasource.password=MySecurePass123
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.primary.url=jdbc:oracle:thin:@localhost:1522/FREEPDB1
spring.datasource.primary.username=cm3int
spring.datasource.primary.password=MySecurePass123
spring.datasource.readonly.url=jdbc:oracle:thin:@localhost:1522/FREEPDB1
spring.datasource.readonly.username=cm3int
spring.datasource.readonly.password=MySecurePass123
spring.liquibase.enabled=true
fabric.security.ldap.enabled=false
fabric.security.csrf.enabled=false
```

### Frontend (`fabric-ui/.env.playwright`)

Required for E2E tests:

```
BASE_URL=http://localhost:3000
E2E_USERNAME=testuser
E2E_PASSWORD=testpass1234
```

## 5. Running Tests

### Backend Tests (requires Oracle running)

```bash
cd fabric-core

# Run all tests
mvn test -pl fabric-api

# Run a specific test class
mvn test -pl fabric-api -Dtest="MonitoringDashboardIntegrationTest"

# Run with coverage gate (>=80% line coverage required)
mvn verify -pl fabric-api
```

### Frontend E2E Tests (requires both backend and frontend running)

```bash
cd fabric-ui

# Run all Playwright tests
npx playwright test

# Run a specific spec
npx playwright test e2e/tests/monitoring.spec.ts

# Run with headed browser (for debugging)
npx playwright test --headed

# View test report
npx playwright show-report
```

### Current Test Coverage

| Suite | Tests | Status |
|-------|-------|--------|
| Backend (JUnit + Oracle) | 85 | All pass |
| JaCoCo coverage gate | >=80% | Pass |
| Playwright E2E | 29 | 28 pass, 1 skipped |

## 6. Application URLs

| URL | Description |
|-----|-------------|
| http://localhost:3000 | Frontend UI |
| http://localhost:3000/login | Login page |
| http://localhost:3000/dashboard | Batch Configuration Dashboard |
| http://localhost:3000/monitoring | Monitoring Dashboard |
| http://localhost:3000/configuration/:systemId/:jobName | Configuration page |
| http://localhost:8080/swagger-ui.html | API documentation (Swagger) |
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/api/monitoring/dashboard | Monitoring API |

## 7. User Roles

| Role | Access |
|------|--------|
| `ROLE_ADMIN` | Full system access |
| `ROLE_OPERATIONS_MANAGER` | Monitoring dashboard, job management |
| `ROLE_JOB_VIEWER` | Read-only access to jobs and monitoring |
| `ROLE_JOB_CREATOR` | Create new job configurations |
| `ROLE_JOB_MODIFIER` | Update/delete job configurations |
| `ROLE_JOB_EXECUTOR` | Execute jobs, access monitoring |
| `ROLE_API_EXECUTOR` | Execute jobs via REST API |

## 8. Troubleshooting

### Oracle container not starting
```bash
docker logs fabric-oracle-free
# Check for port conflicts or resource issues
```

### Backend `ORA-04098` trigger errors
```bash
# Drop invalid triggers directly
docker exec fabric-oracle-free bash -c \
  "echo 'DROP TRIGGER TRG_MANUAL_JOB_CONFIG_AUDIT;' | \
   sqlplus -s cm3int/MySecurePass123@//localhost:1521/FREEPDB1"
```

### Frontend proxy issues
Ensure `"proxy": "http://localhost:8080"` is in `fabric-ui/package.json` and the backend is running.

### Playwright tests fail to authenticate
1. Verify backend is running on port 8080
2. Verify frontend is running on port 3000
3. Check `.env.playwright` has correct credentials
4. Delete `e2e/.auth/` and re-run to regenerate auth state

---

Last Updated: 2026-03-07
