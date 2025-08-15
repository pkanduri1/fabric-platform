# Fabric API - Enterprise Batch Processing Platform

Spring Boot backend application for the Fabric Platform, providing enterprise-grade batch processing and job configuration management with banking-level security and compliance.

## Table of Contents

- [System Overview](#system-overview)
- [Architecture](#architecture)
- [Manual Job Configuration Feature](#manual-job-configuration-feature)
- [Database Schema](#database-schema)
- [API Documentation](#api-documentation)
- [Setup and Installation](#setup-and-installation)
- [Configuration](#configuration)
- [Security](#security)
- [Monitoring and Observability](#monitoring-and-observability)
- [Development Guidelines](#development-guidelines)
- [Troubleshooting](#troubleshooting)

## System Overview

The Fabric API is a Spring Boot application designed for enterprise batch processing with comprehensive manual job configuration capabilities. Built with banking-grade security, SOX compliance, and enterprise standards.

### Key Features

- **Manual Job Configuration Interface (US001)** - Complete CRUD operations for job configurations
- **Manual Batch Execution Interface (Phase 3)** - JSON-to-YAML conversion and batch job triggering
- **Enterprise Security** - JWT authentication, RBAC authorization, LDAP integration
- **Oracle Database Integration** - JdbcTemplate-based data access with CM3INT schema
- **RESTful APIs** - OpenAPI 3.0 documented endpoints with comprehensive validation
- **Audit Trail** - SOX-compliant audit logging and data lineage tracking
- **ENCORE Test Data Simulation** - Complete testing framework for batch jobs
- **Fixed-Width File Generation** - Database-driven file creation for mainframe integration
- **High Availability** - Production-ready with monitoring and health checks

### Technology Stack

- **Spring Boot 3.x** - Main application framework
- **Spring Security** - Authentication and authorization  
- **Pure JdbcTemplate** - Database access without JPA/Hibernate for optimal performance
- **Oracle Database** - Primary data store (CM3INT schema)
- **OpenAPI 3.0** - API documentation and testing
- **Liquibase** - Database change management
- **Maven** - Dependency management and build tool

## Architecture

### Application Layers

```
┌─────────────────────────────────────────┐
│              Frontend (React)           │
│         http://localhost:3000           │
└─────────────────────────────────────────┘
                     │ HTTP/REST
┌─────────────────────────────────────────┐
│             Controller Layer            │
│     /api/v2/manual-job-config/*        │
└─────────────────────────────────────────┘
                     │
┌─────────────────────────────────────────┐
│              Service Layer              │
│      Business Logic & Validation       │
└─────────────────────────────────────────┘
                     │
┌─────────────────────────────────────────┐
│            Repository Layer             │
│          JdbcTemplate DAO              │
└─────────────────────────────────────────┘
                     │
┌─────────────────────────────────────────┐
│           Oracle Database               │
│        CM3INT Schema                    │
│     jdbc:oracle:thin:@localhost:1521   │
└─────────────────────────────────────────┘
```

### Security Architecture

- **JWT Authentication** - Token-based stateless authentication
- **Role-Based Access Control (RBAC)** - Method-level security annotations
- **CORS Configuration** - Frontend-backend communication setup
- **Input Validation** - Comprehensive request validation and sanitization

## Manual Job Configuration Feature

### Business Context

Implements US001 - Manual Job Configuration Interface for operations teams to configure and manage batch processing jobs.

### Core Functionality

1. **Create Job Configurations** - Define new batch job parameters and settings
2. **View Job Configurations** - Browse and search existing configurations with pagination
3. **Update Job Configurations** - Modify existing configurations with version control
4. **Deactivate Job Configurations** - Soft delete with audit trail preservation
5. **System Statistics** - Real-time monitoring of configuration health

### Security Roles

| Role | Permissions |
|------|-------------|
| `JOB_VIEWER` | Read-only access to all configurations |
| `JOB_CREATOR` | Create and read configurations |
| `JOB_MODIFIER` | Full CRUD operations on configurations |
| `JOB_EXECUTOR` | Execute jobs and read configurations |

## Manual Batch Execution Feature

### Business Context

Implements Phase 3 - Manual Batch Execution capabilities for operations teams to trigger batch processing jobs using JSON configurations stored in the database. Includes JSON-to-YAML conversion, master query management, and ENCORE test data simulation.

### Core Functionality

1. **JSON Configuration Processing** - Convert stored JSON job parameters to YAML format for Spring Batch
2. **Master Query Execution** - Execute SQL queries from `MASTER_QUERY_CONFIG` table
3. **ENCORE Test Data** - Simulated source system data for testing and development
4. **Fixed-Width File Generation** - Create mainframe-compatible output files
5. **Execution Tracking** - Monitor job progress and results in `BATCH_EXECUTION_RESULTS`
6. **AtocTran Job Support** - Complete implementation for transaction code 200 processing

### API Endpoints

#### Execute Batch Job
```http
POST /api/v2/manual-job-execution/execute/{configId}
Content-Type: application/json
Authorization: Bearer <token>

{
  "batchDate": "2025-08-14",
  "additionalParam": "value"
}
```

**Response:**
```json
{
  "executionId": "exec_atoctran_encore_200_job_20250814_143025_a1b2c3d4",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "startTime": "2025-08-14T14:30:25",
  "configId": "cfg_encore_1691234567890_xyz",
  "jobName": "atoctran_encore_200_job",
  "message": "Batch job execution initiated successfully"
}
```

#### Get Execution Status
```http
GET /api/v2/manual-job-execution/status/{executionId}
Authorization: Bearer <token>
```

#### Get Master Query
```http
GET /api/v2/manual-job-execution/query/{sourceSystem}/{jobName}
Authorization: Bearer <token>

# Example:
GET /api/v2/manual-job-execution/query/ENCORE/atoctran_encore_200_job
```

### Sample AtocTran Configuration

Example JSON configuration for AtocTran transaction code 200:

```json
{
  "sourceSystem": "ENCORE",
  "jobName": "atoctran_encore_200_job",
  "transactionType": "200",
  "outputFileName": "ATOCTRAN_ENCORE_200_{batchDate}.dat",
  "fields": {
    "location_code": {
      "transformationType": "constant",
      "value": "100030",
      "length": 6,
      "targetPosition": 1
    },
    "acct_num": {
      "transformationType": "source",
      "sourceField": "acct_num",
      "length": 18,
      "targetPosition": 2
    },
    "transaction_code": {
      "transformationType": "constant",
      "value": "200",
      "length": 3,
      "targetPosition": 3
    },
    "transaction_date": {
      "transformationType": "source",
      "sourceField": "batch_date",
      "format": "YYYYMMDD",
      "length": 8,
      "targetPosition": 4
    },
    "previous_cci": {
      "transformationType": "source",
      "sourceField": "cci",
      "length": 1,
      "targetPosition": 5
    },
    "portfolio_location_cd": {
      "transformationType": "constant",
      "value": "200000",
      "length": 6,
      "targetPosition": 6
    },
    "customer_portfolio_id": {
      "transformationType": "source",
      "sourceField": "contact_id",
      "length": 18,
      "targetPosition": 7
    },
    "portfolio_contact_id": {
      "transformationType": "source",
      "sourceField": "contact_id",
      "length": 18,
      "targetPosition": 8
    }
  }
}
```

### Expected Output Format

Fixed-width file format for AtocTran transaction code 200:

```
Position  Field                    Length  Format    Example
1-6       location-code            6       Constant  100030
7-24      acct-num                18       Source    100234567890123456
25-27     transaction-code         3       Constant  200
28-35     transaction-date         8       YYYYMMDD  20250814
36        previous-cci             1       Source    A
37-42     portfolio-location-cd    6       Constant  200000
43-60     customer-portfolio-id   18       Source    CONT001234567890
61-78     portfolio-contact-id    18       Source    CONT001234567890
```

**Sample Output Line:**
```
100030100234567890123456200     20250814A200000CONT001234567890  CONT001234567890
```

### Test Data and QA

#### ENCORE Test Data
The system includes comprehensive test data in `ENCORE_TEST_DATA` table:

- **5 records for 2025-08-14**: Accounts 100234567890123456 through 500678901234567890
- **2 records for 2025-08-15**: Accounts 600789012345678901 through 700890123456789012
- **CCI values**: Rotating A, B, C pattern for testing different customer classifications
- **Contact IDs**: Systematic CONT-prefixed identifiers

#### QA Testing Instructions

1. **Configuration Test**: Create job configuration using sample JSON above
2. **Execution Test**: Execute job with `POST /api/v2/manual-job-execution/execute/{configId}`
3. **Status Verification**: Monitor execution with `GET /api/v2/manual-job-execution/status/{executionId}`
4. **Output Validation**: Verify fixed-width file generation in `/tmp/batch/output`
5. **Data Lineage**: Confirm audit trail in `BATCH_EXECUTION_RESULTS` table

#### Sample cURL Commands

```bash
# Execute AtocTran job for today
curl -X POST http://localhost:8080/api/v2/manual-job-execution/execute/cfg_encore_1691234567890_xyz \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"batchDate": "2025-08-14"}'

# Check execution status
curl -X GET http://localhost:8080/api/v2/manual-job-execution/status/exec_atoctran_encore_200_job_20250814_143025_a1b2c3d4 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Get master query
curl -X GET http://localhost:8080/api/v2/manual-job-execution/query/ENCORE/atoctran_encore_200_job \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Recent Technical Achievements

### Backend Modernization (August 2025)

✅ **JPA/Hibernate Removal Completed**
- Migrated from Spring Data JPA to pure JdbcTemplate for enhanced performance
- Eliminated ORM overhead and complex dependency management issues
- Improved startup time and reduced memory footprint
- Better control over SQL queries and database operations

✅ **Spring Batch Dependencies Resolved**
- Fixed metadata scanning issues that prevented application startup
- Implemented strategic ComponentScan configuration for local development
- Created profile-based service loading for optimal development experience
- Application now starts successfully with local profile

✅ **Database Access Optimization**
- All repositories converted to JdbcTemplate implementations
- Enhanced transaction management with DataSourceTransactionManager
- Maintained existing functionality while improving performance
- Comprehensive row mapping for complex data structures

✅ **Development Experience Improvements**
- Local profile configuration for simplified development setup
- Strategic service inclusion/exclusion for faster startup
- Comprehensive debugging and troubleshooting documentation
- Ready for end-to-end functionality verification

### Current Status: Ready for Production Testing
- ✅ Backend starts successfully on port 8080
- ✅ Oracle database connectivity established
- ✅ Security configuration active
- ✅ Manual job configuration API ready
- ✅ Manual batch execution framework implemented
- 🔄 End-to-end functionality verification in progress

## Database Schema

### Core Tables

#### MANUAL_JOB_CONFIG
Primary table for job configuration storage.

```sql
-- Core configuration storage
CREATE TABLE MANUAL_JOB_CONFIG (
    CONFIG_ID VARCHAR2(50) PRIMARY KEY,
    JOB_NAME VARCHAR2(100) NOT NULL UNIQUE,
    JOB_TYPE VARCHAR2(50) NOT NULL,
    SOURCE_SYSTEM VARCHAR2(50) NOT NULL,
    TARGET_SYSTEM VARCHAR2(50) NOT NULL,
    JOB_PARAMETERS CLOB NOT NULL,
    STATUS VARCHAR2(20) DEFAULT 'ACTIVE',
    CREATED_BY VARCHAR2(100) NOT NULL,
    CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    LAST_MODIFIED_BY VARCHAR2(100),
    LAST_MODIFIED_DATE TIMESTAMP,
    VERSION_NUMBER NUMBER(10) DEFAULT 1
);
```

#### MANUAL_JOB_EXECUTION
Tracks job execution history and status.

```sql
-- Job execution tracking
CREATE TABLE MANUAL_JOB_EXECUTION (
    EXECUTION_ID VARCHAR2(50) PRIMARY KEY,
    CONFIG_ID VARCHAR2(50) NOT NULL,
    EXECUTION_STATUS VARCHAR2(20) NOT NULL,
    START_TIME TIMESTAMP NOT NULL,
    END_TIME TIMESTAMP,
    EXECUTION_PARAMETERS CLOB,
    ERROR_MESSAGE CLOB,
    EXECUTED_BY VARCHAR2(100) NOT NULL,
    CORRELATION_ID VARCHAR2(50),
    CONSTRAINT FK_EXECUTION_CONFIG FOREIGN KEY (CONFIG_ID) 
        REFERENCES MANUAL_JOB_CONFIG(CONFIG_ID)
);
```

#### MANUAL_JOB_AUDIT
SOX-compliant audit trail for all configuration changes.

```sql
-- SOX-compliant audit trail
CREATE TABLE MANUAL_JOB_AUDIT (
    AUDIT_ID VARCHAR2(50) PRIMARY KEY,
    CONFIG_ID VARCHAR2(50) NOT NULL,
    ACTION_TYPE VARCHAR2(20) NOT NULL,
    OLD_VALUES CLOB,
    NEW_VALUES CLOB,
    CHANGED_BY VARCHAR2(100) NOT NULL,
    CHANGE_TIMESTAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CORRELATION_ID VARCHAR2(50),
    BUSINESS_JUSTIFICATION VARCHAR2(500)
);
```

#### MASTER_QUERY_CONFIG
Master SQL queries for batch job execution.

```sql
-- Master query storage for batch jobs
CREATE TABLE MASTER_QUERY_CONFIG (
    ID NUMBER(19) PRIMARY KEY,
    SOURCE_SYSTEM VARCHAR2(50) NOT NULL,
    JOB_NAME VARCHAR2(100) NOT NULL,
    QUERY_TEXT CLOB NOT NULL,
    VERSION NUMBER(10) DEFAULT 1,
    IS_ACTIVE VARCHAR2(1) DEFAULT 'Y',
    CREATED_BY VARCHAR2(50) NOT NULL,
    CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    MODIFIED_BY VARCHAR2(50),
    MODIFIED_DATE TIMESTAMP,
    CONSTRAINT UK_MASTER_QUERY UNIQUE (SOURCE_SYSTEM, JOB_NAME, VERSION),
    CONSTRAINT CHK_ACTIVE CHECK (IS_ACTIVE IN ('Y', 'N'))
);
```

#### ENCORE_TEST_DATA
Simulated source system data for testing and development.

```sql
-- Test data simulation for ENCORE source system
CREATE TABLE ENCORE_TEST_DATA (
    ID NUMBER(19) PRIMARY KEY,
    ACCT_NUM VARCHAR2(18) NOT NULL,
    BATCH_DATE DATE NOT NULL,
    CCI VARCHAR2(1) NOT NULL,
    CONTACT_ID VARCHAR2(18) NOT NULL,
    CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT UK_ENCORE_TEST UNIQUE (ACCT_NUM, BATCH_DATE)
);
```

#### BATCH_EXECUTION_RESULTS
Tracks batch job execution results and status.

```sql
-- Batch job execution tracking
CREATE TABLE BATCH_EXECUTION_RESULTS (
    ID NUMBER(19) PRIMARY KEY,
    JOB_CONFIG_ID VARCHAR2(50) NOT NULL,
    EXECUTION_ID VARCHAR2(100) NOT NULL UNIQUE,
    SOURCE_SYSTEM VARCHAR2(50) NOT NULL,
    RECORDS_PROCESSED NUMBER(10) DEFAULT 0,
    OUTPUT_FILE_NAME VARCHAR2(255),
    OUTPUT_FILE_PATH VARCHAR2(500),
    STATUS VARCHAR2(20) DEFAULT 'PENDING',
    START_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    END_TIME TIMESTAMP,
    ERROR_MESSAGE CLOB,
    CORRELATION_ID VARCHAR2(100),
    CONSTRAINT CHK_STATUS CHECK (STATUS IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED'))
);
```

### Database Connection

- **Schema**: CM3INT
- **Connection**: `jdbc:oracle:thin:@localhost:1521/ORCLPDB1`
- **Driver**: `oracle.jdbc.OracleDriver`
- **Connection Pool**: HikariCP (configured by Spring Boot)

## API Documentation

### Base URLs
```
# Manual Job Configuration
http://localhost:8080/api/v2/manual-job-config

# Manual Batch Execution  
http://localhost:8080/api/v2/manual-job-execution
```

### Authentication
All endpoints require JWT Bearer token:
```
Authorization: Bearer <jwt-token>
```

### Core Endpoints

#### Create Job Configuration
```http
POST /api/v2/manual-job-config
Content-Type: application/json
Authorization: Bearer <token>

{
  "jobName": "Daily Customer Balance ETL",
  "jobType": "ETL_BATCH",
  "sourceSystem": "CORE_BANKING",
  "targetSystem": "DATA_WAREHOUSE",
  "jobParameters": {
    "inputPath": "/data/customer_balances",
    "outputPath": "/processed/balances",
    "batchSize": 1000,
    "retryAttempts": 3
  }
}
```

#### Get Job Configuration
```http
GET /api/v2/manual-job-config/{configId}
Authorization: Bearer <token>
```

#### List Job Configurations with Filtering
```http
GET /api/v2/manual-job-config?page=0&size=20&sortBy=createdDate&sortDir=desc&jobType=ETL_BATCH&sourceSystem=CORE_BANKING&status=ACTIVE
Authorization: Bearer <token>
```

#### Update Job Configuration
```http
PUT /api/v2/manual-job-config/{configId}
Content-Type: application/json
Authorization: Bearer <token>

{
  "jobName": "Updated Job Name",
  "jobType": "ETL_BATCH",
  "sourceSystem": "CORE_BANKING",
  "targetSystem": "DATA_WAREHOUSE",
  "jobParameters": {
    "inputPath": "/data/updated_path",
    "batchSize": 2000
  }
}
```

#### Deactivate Job Configuration
```http
DELETE /api/v2/manual-job-config/{configId}?reason=No longer needed
Authorization: Bearer <token>
```

#### Get System Statistics
```http
GET /api/v2/manual-job-config/statistics
Authorization: Bearer <token>
```

#### Execute Batch Job (New in Phase 3)
```http
POST /api/v2/manual-job-execution/execute/{configId}
Content-Type: application/json
Authorization: Bearer <token>

{
  "batchDate": "2025-08-14",
  "outputDirectory": "/custom/output/path"
}
```

#### Get Batch Execution Status (New in Phase 3)
```http
GET /api/v2/manual-job-execution/status/{executionId}
Authorization: Bearer <token>
```

#### Get Master Query (New in Phase 3)
```http
GET /api/v2/manual-job-execution/query/{sourceSystem}/{jobName}
Authorization: Bearer <token>
```

### Response Format

All responses include correlation IDs for tracing:

```json
{
  "configId": "cfg_core_banking_1691234567890_a1b2c3d4",
  "jobName": "Daily Customer Balance ETL",
  "jobType": "ETL_BATCH",
  "sourceSystem": "CORE_BANKING",
  "targetSystem": "DATA_WAREHOUSE",
  "jobParameters": {
    "inputPath": "/data/customer_balances",
    "outputPath": "/processed/balances",
    "batchSize": 1000
  },
  "status": "ACTIVE",
  "createdBy": "operations_user",
  "createdDate": "2024-08-14T10:30:00Z",
  "versionNumber": 1,
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### OpenAPI Documentation

Interactive API documentation available at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## Setup and Installation

### Prerequisites

- **Java 17+** - OpenJDK or Oracle JDK
- **Maven 3.8+** - Build tool
- **Oracle Database** - 12c or higher with CM3INT schema
- **Git** - Version control

### Local Development Setup

1. **Clone Repository**
   ```bash
   git clone <repository-url>
   cd fabric-platform-new/fabric-core/fabric-api
   ```

2. **Configure Database**
   - Ensure Oracle database is running on localhost:1521
   - Verify CM3INT schema exists with proper permissions
   - Run database schema creation scripts (see Database Schema section)

3. **Configure Application Properties**
   ```bash
   # Copy and modify application.properties
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   
   # Update database credentials if needed
   vi src/main/resources/application.properties
   ```

4. **Build and Run**
   ```bash
   # Clean build (skipping tests for faster startup)
   mvn clean compile -DskipTests
   
   # Run application with local profile (recommended)
   SPRING_PROFILES_ACTIVE=local mvn spring-boot:run -DskipTests
   
   # Alternative: Run with development profile
   mvn spring-boot:run -Dspring-boot.run.profiles=development
   ```

5. **Verify Application**
   ```bash
   # Check health endpoint
   curl http://localhost:8080/actuator/health
   
   # Check API documentation
   open http://localhost:8080/swagger-ui.html
   ```

### Docker Setup (Alternative)

```bash
# Build Docker image
docker build -t fabric-api:latest .

# Run with Docker Compose
docker-compose up -d
```

## Configuration

### Application Properties

Key configuration properties in `application.properties`:

```properties
# Application settings
spring.application.name=fabric-platform
server.port=8080

# Database configuration
spring.datasource.url=jdbc:oracle:thin:@localhost:1521/ORCLPDB1
spring.datasource.username=cm3int
spring.datasource.password=${DB_PASSWORD:MySecurePass123}
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

# Database settings (Pure JdbcTemplate - No JPA/Hibernate)
# JPA/Hibernate dependencies have been removed for optimal performance
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5

# Security settings
fabric.security.ldap.enabled=false
fabric.security.redis.enabled=false

# OpenAPI documentation
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html

# CORS configuration for frontend
fabric.cors.allowed-origins=http://localhost:3000
```

### Environment Variables

```bash
# Database connection
export DB_URL=jdbc:oracle:thin:@localhost:1521/ORCLPDB1
export DB_USERNAME=cm3int
export DB_PASSWORD=MySecurePass123

# Application environment
export SPRING_PROFILES_ACTIVE=development

# Logging levels
export LOGGING_LEVEL_ROOT=INFO
export LOGGING_LEVEL_COM_TRUIST_BATCH=DEBUG
```

### Profile-Specific Configuration

Create environment-specific property files:

- `application-development.properties` - Local development
- `application-test.properties` - Testing environment
- `application-staging.properties` - Staging environment
- `application-production.properties` - Production environment

## Security

### Authentication Flow

1. **JWT Token Generation** - Via `/api/auth/login` endpoint
2. **Token Validation** - On every protected endpoint
3. **Role Extraction** - From JWT claims for authorization
4. **Security Context** - Populated for downstream processing

### Security Configuration

```java
// Method-level security examples
@PreAuthorize("hasRole('JOB_CREATOR') or hasRole('JOB_MODIFIER')")
public ResponseEntity<?> createJobConfiguration(...)

@PreAuthorize("hasAnyRole('JOB_VIEWER', 'JOB_CREATOR', 'JOB_MODIFIER', 'JOB_EXECUTOR')")
public ResponseEntity<?> getJobConfiguration(...)
```

### CORS Configuration

Frontend-backend communication enabled for:
- `http://localhost:3000` (React development server)
- Additional origins configurable via `fabric.cors.allowed-origins`

## Monitoring and Observability

### Logging

Structured logging with correlation IDs for distributed tracing:

```java
log.info("Creating job configuration: {} by user: {} [correlationId: {}]", 
         request.getJobName(), username, correlationId);
```

### Health Checks

Spring Boot Actuator endpoints:
- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

### Audit Trail

All configuration changes logged to `MANUAL_JOB_AUDIT` table with:
- Action type (CREATE, UPDATE, DELETE)
- Old and new values
- User information
- Timestamp and correlation ID
- Business justification

## Development Guidelines

### Code Standards

- **Java 17+ features** - Use modern Java syntax and features
- **Spring Boot conventions** - Follow Spring Boot best practices
- **Security first** - Validate all inputs, use parameterized queries
- **Configuration-driven** - Externalize all configurable parameters
- **Comprehensive testing** - Unit and integration tests for all components

### Database Guidelines

- **JdbcTemplate preferred** - Over JPA for better control and performance
- **Parameterized queries** - Prevent SQL injection
- **Transaction management** - Use `@Transactional` appropriately
- **Connection pooling** - HikariCP for production performance

### API Design Principles

- **RESTful conventions** - Proper HTTP methods and status codes
- **Comprehensive validation** - Input validation with meaningful error messages
- **Pagination support** - For large datasets
- **Correlation IDs** - For request tracing and debugging
- **OpenAPI documentation** - Keep API docs current and comprehensive

## Troubleshooting

### Common Issues

#### Database Connection Issues

```bash
# Check Oracle database connectivity
sqlplus cm3int/MySecurePass123@localhost:1521/ORCLPDB1

# Verify schema exists
SELECT table_name FROM user_tables WHERE table_name LIKE 'MANUAL_JOB%';
```

#### Application Startup Issues

```bash
# Check logs for detailed error information
tail -f logs/application.log

# Common issues:
# 1. Port 8080 already in use
# 2. Database connection failure
# 3. Missing environment variables
```

#### Frontend-Backend Connection Issues

```bash
# Verify CORS configuration
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS \
     http://localhost:8080/api/v2/manual-job-config
```

### Performance Optimization

- **Database Indexing** - Ensure proper indexes on frequently queried columns
- **Connection Pooling** - Tune HikariCP settings for your environment
- **JVM Tuning** - Configure heap size and garbage collection for production

### Debugging

Enable debug logging for troubleshooting:

```properties
# Application debug logging
logging.level.com.truist.batch=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG

# Database query logging
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

## Production Deployment

### Build for Production

```bash
# Create production build
mvn clean package -Pproduction

# Build Docker image
docker build -t fabric-api:1.0.0 .
```

### Environment Configuration

- Use environment-specific property files
- Configure external configuration server (Spring Cloud Config)
- Set up proper logging aggregation (ELK stack)
- Configure monitoring and alerting

### Security Considerations

- Use secrets management for database credentials
- Enable HTTPS with proper certificates
- Configure firewall rules for database access
- Set up regular security scanning and updates

---

## Support

For technical support or questions:
- **Development Team**: Senior Full Stack Developer Agent
- **Architecture Review**: Principal Enterprise Architect
- **Product Owner**: Lending Product Owner

**Last Updated**: August 15, 2025
**Version**: 1.1.0 - Backend Modernization Complete
**Documentation Generated**: Claude Code (claude.ai/code)