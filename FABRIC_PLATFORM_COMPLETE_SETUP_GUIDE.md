# FABRIC PLATFORM - COMPLETE SETUP GUIDE

**Document Control**
- **Project**: Fabric Platform - Enterprise Batch Processing System
- **Version**: 1.3.0
- **Date**: August 21, 2025
- **Classification**: INTERNAL - BANKING CONFIDENTIAL
- **Target Audience**: Developers (Beginners to Spring Boot/React)

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites & System Requirements](#prerequisites--system-requirements)
3. [Environment Setup](#environment-setup)
4. [Project Setup](#project-setup)
5. [Configuration Profiles](#configuration-profiles)
6. [Database Setup](#database-setup)
7. [Running the Application](#running-the-application)
8. [Verification Steps](#verification-steps)
9. [Development Workflow](#development-workflow)
10. [Known Issues & Current Status](#known-issues--current-status)
11. [Common Issues & Troubleshooting](#common-issues--troubleshooting)
12. [IDE Recommendations](#ide-recommendations)
13. [Best Practices](#best-practices)

---

## Overview

The Fabric Platform is an enterprise-grade batch processing and job configuration management system designed for financial institutions. It consists of:

- **Spring Boot Backend (fabric-api)**: REST API with JWT authentication, RBAC authorization, and SOX-compliant audit trails
- **React Frontend (fabric-ui)**: Material-UI based interface with real-time monitoring and role-based access control
- **Oracle Database (CM3INT Schema)**: Enterprise database with 15+ tables for configuration, execution tracking, and audit

### Current Implementation Status

✅ **COMPLETED FEATURES:**
- US001: Manual Job Configuration Interface (Backend + Frontend)
- Phase 3: Manual Batch Execution Interface (JSON-to-YAML conversion)
- Master Query Integration (Phase 1)
- Full backend-to-frontend connectivity
- JWT Authentication with RBAC
- SOX-compliant audit trails
- Real-time WebSocket monitoring

⚠️ **KNOWN ISSUES (TO BE ADDRESSED):**
- Master query associations not saving to `TEMPLATE_MASTER_QUERY_MAPPING`
- `TEMPLATE_SOURCE_MAPPINGS` table not being used for field mappings by source
- Configuration saving to `batch_configurations` but missing source mappings

---

## Prerequisites & System Requirements

### Required Software

| Component | Version | Purpose |
|-----------|---------|---------|
| **Java** | 17 or 21 | Spring Boot backend runtime |
| **Node.js** | 16.x or 18.x | React frontend development |
| **npm** | 8.x+ | Frontend package management |
| **Oracle Database** | 12c+ | Primary data storage |
| **Maven** | 3.8+ | Backend build tool |
| **Git** | 2.x+ | Version control |

### Hardware Requirements

| Environment | RAM | Disk Space | CPU |
|-------------|-----|------------|-----|
| **Development** | 8GB minimum | 10GB | 4 cores |
| **Recommended** | 16GB | 20GB | 8 cores |

### Operating System Support

- **Primary**: macOS (current development environment)
- **Supported**: Windows 10/11, Linux (Ubuntu 20.04+)

---

## Environment Setup

### 1. Java Installation

#### For macOS (using Homebrew):
```bash
# Install Java 17
brew install openjdk@17

# Set JAVA_HOME
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Verify installation
java -version
javac -version
```

#### For Windows:
1. Download OpenJDK 17 from [Adoptium](https://adoptium.net/)
2. Install using the MSI installer
3. Set `JAVA_HOME` environment variable
4. Add `%JAVA_HOME%\bin` to PATH

#### For Linux (Ubuntu):
```bash
sudo apt update
sudo apt install openjdk-17-jdk
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

### 2. Node.js Installation

#### Using Node Version Manager (Recommended):
```bash
# Install nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc

# Install and use Node.js 18
nvm install 18
nvm use 18
nvm alias default 18

# Verify installation
node --version
npm --version
```

#### Direct Installation:
- Download from [nodejs.org](https://nodejs.org/)
- Install LTS version (18.x recommended)

### 3. Maven Installation

#### For macOS:
```bash
brew install maven
mvn --version
```

#### For Windows/Linux:
- Download from [maven.apache.org](https://maven.apache.org/)
- Extract and add to PATH
- Set `MAVEN_HOME` environment variable

### 4. Oracle Database Setup

#### Option A: Local Oracle Database
1. Download Oracle Database 19c or 21c
2. Install with default settings
3. Create pluggable database `ORCLPDB1`
4. Create user `cm3int` with appropriate privileges

#### Option B: Docker Oracle (For Development)
```bash
# Pull Oracle Docker image
docker pull container-registry.oracle.com/database/express:21.3.0-xe

# Run Oracle container
docker run -d \
  --name oracle-xe \
  -p 1521:1521 \
  -p 5500:5500 \
  -e ORACLE_PWD=MySecurePass123 \
  -e ORACLE_CHARACTERSET=AL32UTF8 \
  container-registry.oracle.com/database/express:21.3.0-xe

# Connect and create user
sqlplus sys/MySecurePass123@localhost:1521/XE as sysdba
```

---

## Project Setup

### 1. Clone Repository

```bash
# Clone the repository
git clone <repository-url>
cd fabric-platform-new

# Verify project structure
ls -la
# Should show: fabric-core/, fabric-ui/, docs/, etc.
```

### 2. Backend Setup (Spring Boot)

```bash
# Navigate to backend
cd fabric-core/fabric-api

# Clean and compile (skip tests for initial setup)
mvn clean compile -DskipTests

# Verify dependencies download
ls target/classes
```

### 3. Frontend Setup (React)

```bash
# Navigate to frontend
cd ../../fabric-ui

# Install dependencies
npm install

# Verify installation
npm list --depth=0
```

---

## Configuration Profiles

The application supports multiple profiles for different environments:

### Profile Overview

| Profile | Purpose | Database | Logging | Security |
|---------|---------|----------|---------|----------|
| **local** | Local development with Oracle | Full Oracle setup | DEBUG | Disabled LDAP |
| **development** | Team development | Shared dev DB | DEBUG | Basic auth |
| **test** | Automated testing | Test schema | INFO | Mock security |
| **production** | Live environment | Production DB | WARN | Full security |

### Profile Configuration Files

```
fabric-core/fabric-api/src/main/resources/
├── application.yml                    # Main configuration
├── application-local.yml             # Local overrides
├── application-development.yml       # Development settings
├── application-test.yml              # Test environment
└── application-production.yml        # Production settings
```

### Setting Active Profile

#### Method 1: Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=local
```

#### Method 2: Command Line
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

#### Method 3: IDE Configuration
- **IntelliJ IDEA**: Run Configuration → Environment Variables → `SPRING_PROFILES_ACTIVE=local`
- **Eclipse**: Run Configuration → Arguments → VM arguments: `-Dspring.profiles.active=local`

---

## Database Setup

### 1. Create CM3INT User and Schema

```sql
-- Connect as SYSDBA
sqlplus sys/MySecurePass123@localhost:1521/ORCLPDB1 as sysdba

-- Create user
CREATE USER cm3int IDENTIFIED BY MySecurePass123;

-- Grant privileges
GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE TO cm3int;
GRANT UNLIMITED TABLESPACE TO cm3int;
GRANT CREATE SESSION TO cm3int;
GRANT CREATE TABLE TO cm3int;
GRANT CREATE INDEX TO cm3int;

-- Additional privileges for enterprise features
GRANT SELECT ANY DICTIONARY TO cm3int;
GRANT CREATE PROCEDURE TO cm3int;
GRANT CREATE TRIGGER TO cm3int;

-- Verify user creation
SELECT username FROM dba_users WHERE username = 'CM3INT';
```

### 2. Create Database Schema

The schema includes 15+ tables for complete functionality:

#### Core Tables:
- `MANUAL_JOB_CONFIG`: Job configuration storage
- `BATCH_CONFIGURATIONS`: Batch processing configurations
- `BATCH_EXECUTION_RESULTS`: Execution tracking
- `MANUAL_JOB_AUDIT`: SOX-compliant audit trail

#### Master Query Tables:
- `MASTER_QUERY_CONFIG`: SQL query storage
- `MASTER_QUERY_COLUMNS`: Query column metadata
- `TEMPLATE_MASTER_QUERY_MAPPING`: Template-query associations ⚠️ **ISSUE**
- `TEMPLATE_SOURCE_MAPPINGS`: Field mappings by source ⚠️ **ISSUE**

#### Test Data Tables:
- `ENCORE_TEST_DATA`: Sample source system data
- `SHAW_TEST_DATA`: Alternative source data

### 3. Schema Creation Options

#### Option A: Automated (Recommended)
```bash
# Navigate to backend directory
cd fabric-core/fabric-api

# Run schema creation script
sqlplus cm3int/MySecurePass123@localhost:1521/ORCLPDB1 @../../docs/cm3int_tables_ddl_82025.sql
```

#### Option B: Manual Liquibase (Advanced)
```bash
# Enable Liquibase in application.yml (set enabled: true)
mvn liquibase:update

# Rollback if needed
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

### 4. Verify Schema Creation

```sql
-- Connect as cm3int user
sqlplus cm3int/MySecurePass123@localhost:1521/ORCLPDB1

-- Check table creation
SELECT table_name FROM user_tables ORDER BY table_name;

-- Verify key tables exist
SELECT COUNT(*) as table_count FROM user_tables 
WHERE table_name IN (
    'MANUAL_JOB_CONFIG',
    'BATCH_CONFIGURATIONS', 
    'MASTER_QUERY_CONFIG',
    'TEMPLATE_MASTER_QUERY_MAPPING',
    'TEMPLATE_SOURCE_MAPPINGS'
);
-- Should return 5 or more
```

---

## Running the Application

### 1. Start Backend (Spring Boot)

#### Option A: Maven (Recommended for beginners)
```bash
# Navigate to backend
cd fabric-core/fabric-api

# Start with local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Alternative: Skip tests and start
mvn clean compile spring-boot:run -DskipTests -Dspring-boot.run.profiles=local
```

#### Option B: JAR Execution (Production-like)
```bash
# Build JAR
mvn clean package -DskipTests

# Run JAR
java -jar target/fabric-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

#### Success Indicators:
```
✅ Started InterfaceBatchApplication in X.XXX seconds
✅ Tomcat started on port(s): 8080 (http)
✅ HikariCP - Database connection pool initialized
✅ OpenAPI documentation available at: http://localhost:8080/swagger-ui.html
```

### 2. Start Frontend (React)

```bash
# Navigate to frontend (new terminal)
cd fabric-ui

# Start development server
npm start

# Alternative with verbose logging
npm start -- --verbose
```

#### Success Indicators:
```
✅ webpack compiled successfully
✅ Local: http://localhost:3000
✅ Proxy configured for backend: http://localhost:8080
✅ Hot reloading enabled
```

### 3. Both Applications Running

After successful startup, you should have:

| Service | URL | Purpose |
|---------|-----|---------|
| **Frontend** | http://localhost:3000 | Main application UI |
| **Backend API** | http://localhost:8080 | REST API endpoints |
| **API Documentation** | http://localhost:8080/swagger-ui.html | Interactive API docs |
| **Health Check** | http://localhost:8080/actuator/health | System health status |

---

## Verification Steps

### 1. Backend Health Check

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Expected response:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 2. Database Connectivity Test

```bash
# Test database endpoint
curl http://localhost:8080/api/v2/manual-job-config/list

# Expected: JSON array (may be empty initially)
[]
```

### 3. Frontend-Backend Integration

1. Open browser: http://localhost:3000
2. Navigate to "Manual Job Configuration" page
3. Verify page loads without errors
4. Check browser console for any JavaScript errors
5. Test form submission (creates test record)

### 4. Authentication Test

```bash
# Test authentication endpoint
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "test", "password": "test"}'

# Expected: JWT token response
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "username": "test",
    "roles": ["JOB_VIEWER", "JOB_CREATOR"]
  }
}
```

---

## Development Workflow

### 1. Making Code Changes

#### Backend Changes:
```bash
# Backend automatically reloads on save (Spring Boot DevTools)
# If not working, restart manually:
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

#### Frontend Changes:
```bash
# React hot reloading works automatically
# If issues occur, restart:
npm start
```

### 2. Testing Changes

#### Backend Testing:
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ManualJobConfigServiceTest

# Skip tests during development
mvn clean compile -DskipTests
```

#### Frontend Testing:
```bash
# Run all tests
npm test

# Run specific test file
npm test -- ManualJobConfiguration.test.tsx

# Run tests in CI mode
npm test -- --coverage --watchAll=false
```

### 3. Database Changes

#### Add New Tables:
1. Create SQL script in `src/main/resources/db/changelog/`
2. Update `db.changelog-master.xml`
3. Run Liquibase update: `mvn liquibase:update`

#### Data Changes:
```sql
-- Connect to database
sqlplus cm3int/MySecurePass123@localhost:1521/ORCLPDB1

-- Make changes
INSERT INTO manual_job_config (...) VALUES (...);

-- Verify changes
SELECT * FROM manual_job_config WHERE id = 'test-config';
```

---

## Known Issues & Current Status

### ⚠️ Critical Issues to Address

#### 1. Master Query Association Storage
**Issue**: Master query associations not being saved to `TEMPLATE_MASTER_QUERY_MAPPING` table.

**Impact**: 
- Master queries can be tested and previewed
- Associations with templates are not persisted
- Cannot reload saved configurations with master query mappings

**Current Workaround**: Manual database insertion
```sql
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    template_id, master_query_id, source_system, created_by, created_date
) VALUES (
    'template-123', 'query-456', 'ENCORE', 'system', CURRENT_TIMESTAMP
);
```

#### 2. TEMPLATE_SOURCE_MAPPINGS Table Usage
**Issue**: Field mappings are not being stored in `TEMPLATE_SOURCE_MAPPINGS` table by source system.

**Impact**:
- Field mappings are stored generically
- Source-specific mapping configurations lost
- Cannot handle different field mappings per source system

**Expected Behavior**: Each source system should have its own field mapping configuration.

#### 3. Configuration Save Process
**Issue**: Configuration saving to `batch_configurations` but missing source mappings integration.

**Impact**:
- Basic configuration saves work
- Missing relationship data between configurations and source mappings
- Incomplete data model implementation

### 🔄 Planned Fixes

These issues are documented for the next development phase and will be addressed in the upcoming sprint:

1. **TEMPLATE_MASTER_QUERY_MAPPING Integration**
   - Update `TemplateConfigurationService` to save master query associations
   - Implement proper foreign key relationships
   - Add rollback procedures for failed saves

2. **TEMPLATE_SOURCE_MAPPINGS Implementation**
   - Modify field mapping save process to use source-specific table
   - Update frontend to handle source-specific configurations
   - Implement migration from generic to source-specific mappings

3. **End-to-End Save Process**
   - Complete integration between all configuration tables
   - Implement transaction management for multi-table saves
   - Add comprehensive validation and error handling

---

## Common Issues & Troubleshooting

### Backend Issues

#### Issue: "Cannot connect to database"
**Symptoms**: Application fails to start with database connection errors.

**Solutions**:
```bash
# 1. Check Oracle database is running
lsnrctl status

# 2. Test connection manually
sqlplus cm3int/MySecurePass123@localhost:1521/ORCLPDB1

# 3. Verify application.yml database configuration
# 4. Check firewall settings (port 1521)
# 5. Verify Oracle service status
```

#### Issue: "Port 8080 already in use"
**Solutions**:
```bash
# Find process using port 8080
lsof -i :8080

# Kill process (replace PID)
kill -9 <PID>

# Alternative: Use different port
mvn spring-boot:run -Dserver.port=8081
```

#### Issue: "Bean definition conflicts"
**Symptoms**: Spring Boot fails to start with bean definition errors.

**Solutions**:
1. Check for duplicate `@Component` annotations
2. Verify profile-specific configurations
3. Clean and rebuild: `mvn clean compile`
4. Check for circular dependencies

### Frontend Issues

#### Issue: "npm install fails"
**Solutions**:
```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Use specific Node version
nvm use 18
npm install
```

#### Issue: "Proxy errors - cannot reach backend"
**Symptoms**: Frontend loads but API calls fail with 5xx errors.

**Solutions**:
1. Verify backend is running on port 8080
2. Check `package.json` proxy configuration
3. Restart both frontend and backend
4. Check CORS configuration in Spring Boot

#### Issue: "Build fails with TypeScript errors"
**Solutions**:
```bash
# Check TypeScript configuration
npm run type-check

# Fix common issues
# 1. Update @types packages
npm update @types/react @types/react-dom

# 2. Resolve type conflicts
# Check component props and interfaces
```

### Database Issues

#### Issue: "Table not found errors"
**Solutions**:
```sql
-- Verify table existence
SELECT table_name FROM user_tables WHERE table_name LIKE '%CONFIG%';

-- Check current schema
SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM dual;

-- Verify user privileges
SELECT * FROM user_sys_privs WHERE privilege LIKE '%CREATE%';
```

#### Issue: "ORA-00942: table or view does not exist"
**Solutions**:
1. Run schema creation script again
2. Check table name spelling (case sensitive)
3. Verify user has appropriate privileges
4. Check if connected to correct database/schema

### Authentication Issues

#### Issue: "JWT token validation fails"
**Solutions**:
1. Check JWT secret key configuration
2. Verify token expiration settings
3. Clear browser storage/cookies
4. Check server time synchronization

#### Issue: "RBAC permissions denied"
**Solutions**:
1. Verify user roles in JWT token
2. Check method-level security annotations
3. Review role-based access control configuration
4. Test with different user roles

---

## IDE Recommendations

### 1. IntelliJ IDEA (Recommended)

**Advantages**:
- Excellent Spring Boot support
- Integrated Maven and npm support
- Superior Java debugging
- Built-in database tools
- React/TypeScript support

**Setup**:
1. Install IntelliJ IDEA Ultimate
2. Install plugins:
   - Spring Boot
   - Database Tools
   - JavaScript and TypeScript
   - React

**Configuration**:
```
File → Settings → Build → Build Tools → Maven
✓ Import Maven projects automatically
✓ Download sources
✓ Download documentation

File → Settings → Languages → TypeScript
✓ Enable TypeScript service
```

### 2. Visual Studio Code (Lightweight Option)

**Advantages**:
- Excellent for React/TypeScript development
- Lightweight and fast
- Great extension ecosystem
- Good Java support with extensions

**Required Extensions**:
```
- Extension Pack for Java
- Spring Boot Extension Pack
- ES7+ React/Redux/React-Native snippets
- TypeScript Importer
- Oracle Developer Tools for VS Code
```

### 3. Eclipse (Enterprise Alternative)

**Advantages**:
- Free and open source
- Excellent Maven integration
- Good debugging capabilities
- Enterprise development features

**Required Plugins**:
- Spring Tools 4
- TypeScript IDE
- Database Development
- Maven Integration

### IDE-Specific Configurations

#### IntelliJ IDEA Run Configurations:

**Backend Configuration**:
```
Name: Fabric API Local
Main class: com.truist.batch.InterfaceBatchApplication
VM options: -Dspring.profiles.active=local
Environment variables: SPRING_PROFILES_ACTIVE=local
Working directory: fabric-core/fabric-api
```

**Frontend Configuration**:
```
Name: Fabric UI
Configuration type: npm
Package.json: fabric-ui/package.json
Command: start
```

#### VS Code Launch Configuration:
Create `.vscode/launch.json`:
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Fabric API Local",
            "request": "launch",
            "mainClass": "com.truist.batch.InterfaceBatchApplication",
            "projectName": "fabric-api",
            "env": {
                "SPRING_PROFILES_ACTIVE": "local"
            }
        }
    ]
}
```

---

## Best Practices

### Development Best Practices

#### 1. Profile Management
```bash
# Always specify profile when running
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Never commit sensitive data in configuration files
# Use environment variables for passwords
export DB_PASSWORD=MySecurePass123
```

#### 2. Database Management
```sql
-- Always backup before schema changes
CREATE TABLE backup_table AS SELECT * FROM original_table;

-- Use transactions for data changes
BEGIN
    INSERT INTO manual_job_config (...) VALUES (...);
    -- Verify before commit
    SELECT * FROM manual_job_config WHERE id = 'new-id';
COMMIT;
```

#### 3. Code Quality
```bash
# Backend: Run tests before commits
mvn clean test

# Frontend: Use TypeScript strict mode
npm run type-check

# Format code consistently
npm run format
```

### Security Best Practices

#### 1. Database Security
- Never commit database passwords
- Use read-only connections for queries
- Implement proper audit trails
- Regular security reviews

#### 2. Application Security
- Always use HTTPS in production
- Implement proper CORS policies
- Validate all inputs
- Use parameterized queries

### Performance Best Practices

#### 1. Backend Optimization
```java
// Use connection pooling
@Configuration
public class DatabaseConfig {
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        // ... other settings
        return new HikariDataSource(config);
    }
}
```

#### 2. Frontend Optimization
```javascript
// Use React.memo for expensive components
const ExpensiveComponent = React.memo(({ data }) => {
    return <div>{/* expensive rendering */}</div>;
});

// Implement proper error boundaries
class ErrorBoundary extends React.Component {
    // Error boundary implementation
}
```

---

## Support and Resources

### Documentation Links
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [React Documentation](https://reactjs.org/docs/getting-started.html)
- [Material-UI Documentation](https://mui.com/getting-started/installation/)
- [Oracle Database Documentation](https://docs.oracle.com/en/database/)

### Internal Resources
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Project Architecture**: `/docs/architecture.md`
- **Database Schema**: `/docs/cm3int_tables_ddl_82025.sql`
- **User Stories**: `/docs/stories/`

### Getting Help

1. **Check Known Issues**: Review this document's "Known Issues" section
2. **Search Logs**: Check application logs for specific error messages
3. **Database Verification**: Run verification queries to check data state
4. **Environment Check**: Verify all prerequisites are properly installed

---

## Conclusion

The Fabric Platform is a comprehensive enterprise solution with robust architecture and security features. While there are some known issues with master query associations and source mappings, the core functionality is operational and ready for development.

**Next Steps**:
1. Complete environment setup following this guide
2. Verify all components are working correctly
3. Review known issues for upcoming development priorities
4. Begin development work on addressing current limitations

**Document Maintenance**:
This setup guide will be updated as issues are resolved and new features are added. Always refer to the latest version for current setup procedures.

---

**Document Classification**: INTERNAL - BANKING CONFIDENTIAL  
**Last Updated**: August 21, 2025  
**Next Review**: September 21, 2025  
**Contact**: Senior Full Stack Developer Agent