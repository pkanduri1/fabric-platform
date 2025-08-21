# QA Environment Setup Guide - Phase 1 Master Query Integration

## Overview

This guide provides step-by-step instructions for setting up the QA testing environment for Phase 1 Master Query Integration, coordinated by the Scrum Master to ensure consistent and reliable testing conditions.

**Target Audience**: QA Engineers, DevOps, Database Administrators  
**Setup Time**: 2-4 hours  
**Prerequisites**: Oracle Database, Java 17+, Maven 3.6+  

---

## 1. Database Environment Setup

### 1.1 Oracle Database Configuration

**Required Oracle Version**: 19c or higher  
**Schema**: CM3INT  
**Connection Type**: Read-only pool for master query operations  

#### Create QA Database User
```sql
-- Create dedicated QA user
CREATE USER cm3int_qa IDENTIFIED BY QATestPass123;

-- Grant necessary privileges
GRANT CONNECT, RESOURCE TO cm3int_qa;
GRANT SELECT ANY TABLE TO cm3int_qa;
GRANT CREATE SESSION TO cm3int_qa;

-- Grant access to main schema objects
GRANT SELECT, INSERT, UPDATE, DELETE ON CM3INT.MANUAL_JOB_CONFIG TO cm3int_qa;
GRANT SELECT, INSERT, UPDATE, DELETE ON CM3INT.TEMPLATE_MASTER_QUERY_MAPPING TO cm3int_qa;
GRANT SELECT, INSERT, UPDATE, DELETE ON CM3INT.MASTER_QUERY_COLUMNS TO cm3int_qa;
GRANT SELECT ON CM3INT.ENCORE_TEST_DATA TO cm3int_qa;

-- Create synonyms for easier access
CREATE SYNONYM cm3int_qa.MANUAL_JOB_CONFIG FOR CM3INT.MANUAL_JOB_CONFIG;
CREATE SYNONYM cm3int_qa.TEMPLATE_MASTER_QUERY_MAPPING FOR CM3INT.TEMPLATE_MASTER_QUERY_MAPPING;
CREATE SYNONYM cm3int_qa.MASTER_QUERY_COLUMNS FOR CM3INT.MASTER_QUERY_COLUMNS;
CREATE SYNONYM cm3int_qa.ENCORE_TEST_DATA FOR CM3INT.ENCORE_TEST_DATA;
```

#### Connection Pool Configuration
```properties
# QA Database Configuration
spring.datasource.qa.url=jdbc:oracle:thin:@localhost:1521/ORCLPDB1
spring.datasource.qa.username=cm3int_qa
spring.datasource.qa.password=QATestPass123
spring.datasource.qa.driver-class-name=oracle.jdbc.OracleDriver

# Connection Pool Settings
spring.datasource.qa.hikari.maximum-pool-size=10
spring.datasource.qa.hikari.minimum-idle=2
spring.datasource.qa.hikari.connection-timeout=30000
spring.datasource.qa.hikari.idle-timeout=600000
spring.datasource.qa.hikari.max-lifetime=1800000
spring.datasource.qa.hikari.leak-detection-threshold=60000
```

### 1.2 Database Schema Validation

**Verify Required Tables Exist:**
```sql
-- Check if Liquibase migrations have been applied
SELECT * FROM DATABASECHANGELOG 
WHERE ID LIKE 'us001-010%' 
ORDER BY DATEEXECUTED DESC;

-- Verify table structures
DESCRIBE TEMPLATE_MASTER_QUERY_MAPPING;
DESCRIBE MASTER_QUERY_COLUMNS;

-- Check foreign key constraints
SELECT constraint_name, table_name, column_name 
FROM user_cons_columns 
WHERE table_name IN ('TEMPLATE_MASTER_QUERY_MAPPING', 'MASTER_QUERY_COLUMNS');

-- Verify indexes
SELECT index_name, table_name, column_name 
FROM user_ind_columns 
WHERE table_name IN ('TEMPLATE_MASTER_QUERY_MAPPING', 'MASTER_QUERY_COLUMNS')
ORDER BY table_name, index_name, column_position;
```

---

## 2. Test Data Setup

### 2.1 Core Test Data Script

Create file: `/src/test/resources/qa-test-data.sql`

```sql
-- =========================================================================
-- QA TEST DATA SETUP SCRIPT
-- Phase 1 Master Query Integration
-- =========================================================================

-- Clean up existing test data
DELETE FROM MASTER_QUERY_COLUMNS WHERE COLUMN_ID LIKE 'col_qa_%';
DELETE FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE MAPPING_ID LIKE 'tmq_qa_%';
DELETE FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID LIKE 'cfg_qa_%';

-- Insert test job configurations
INSERT INTO MANUAL_JOB_CONFIG (
    CONFIG_ID, JOB_NAME, JOB_DESCRIPTION, SOURCE_SYSTEM, 
    STATUS, CREATED_BY, CREATED_DATE
) VALUES (
    'cfg_qa_001', 'QA Test Job 1', 'Test configuration for master query integration',
    'ENCORE', 'ACTIVE', 'qa_test_user', SYSDATE
);

INSERT INTO MANUAL_JOB_CONFIG (
    CONFIG_ID, JOB_NAME, JOB_DESCRIPTION, SOURCE_SYSTEM, 
    STATUS, CREATED_BY, CREATED_DATE
) VALUES (
    'cfg_qa_002', 'QA Test Job 2', 'Second test configuration for validation',
    'SHAW', 'ACTIVE', 'qa_test_user', SYSDATE
);

-- Insert test master query mappings
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL,
    QUERY_DESCRIPTION, QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CREATED_DATE, CORRELATION_ID
) VALUES (
    'tmq_qa_001', 'cfg_qa_001', 'mq_qa_simple_select', 'Simple Transaction Query',
    'SELECT account_id, transaction_id, amount, batch_date FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate ORDER BY account_id',
    'Simple SELECT query for testing basic functionality',
    'SELECT', 30, 100, 'ACTIVE', 'Y', 'INTERNAL', 'N',
    'qa_test_user', SYSDATE, 'corr_qa_test_001'
);

INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL,
    QUERY_DESCRIPTION, QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CREATED_DATE, CORRELATION_ID
) VALUES (
    'tmq_qa_002', 'cfg_qa_001', 'mq_qa_parameterized', 'Parameterized Transaction Summary',
    'SELECT account_id, SUM(amount) as total_amount, COUNT(*) as transaction_count FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate AND amount >= :minAmount GROUP BY account_id HAVING COUNT(*) >= :minTransactionCount ORDER BY total_amount DESC',
    'Parameterized query with multiple parameters for advanced testing',
    'SELECT', 30, 50, 
    '{"batchDate": {"type": "date", "required": true}, "minAmount": {"type": "decimal", "required": true, "default": 100.00}, "minTransactionCount": {"type": "integer", "required": false, "default": 1}}',
    'ACTIVE', 'Y', 'INTERNAL', 'Y',
    'qa_test_user', SYSDATE, 'corr_qa_test_002'
);

INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL,
    QUERY_DESCRIPTION, QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CREATED_DATE, CORRELATION_ID
) VALUES (
    'tmq_qa_003', 'cfg_qa_002', 'mq_qa_complex_with', 'Complex WITH Query',
    'WITH account_summary AS (SELECT account_id, SUM(amount) as total FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate GROUP BY account_id), high_value AS (SELECT * FROM account_summary WHERE total > :threshold) SELECT hv.account_id, hv.total, COUNT(etd.transaction_id) as transaction_count FROM high_value hv JOIN ENCORE_TEST_DATA etd ON hv.account_id = etd.account_id WHERE etd.batch_date = :batchDate GROUP BY hv.account_id, hv.total ORDER BY hv.total DESC',
    'Complex WITH query for advanced SQL testing',
    'WITH_SELECT', 30, 25,
    'ACTIVE', 'Y', 'CONFIDENTIAL', 'Y',
    'qa_test_user', SYSDATE, 'corr_qa_test_003'
);

-- Insert test query column metadata
INSERT INTO MASTER_QUERY_COLUMNS (
    COLUMN_ID, MASTER_QUERY_ID, COLUMN_NAME, COLUMN_ALIAS, COLUMN_TYPE,
    COLUMN_LENGTH, COLUMN_PRECISION, COLUMN_SCALE, IS_NULLABLE, IS_PRIMARY_KEY,
    COLUMN_ORDER, VALIDATION_RULES, DISPLAY_FORMAT, COLUMN_DESCRIPTION,
    IS_SENSITIVE_DATA, DATA_CLASSIFICATION, CREATED_BY, CREATED_DATE
) VALUES (
    'col_qa_001', 'mq_qa_simple_select', 'account_id', 'Account ID', 'VARCHAR2',
    20, NULL, NULL, 'N', 'Y', 1,
    '{"minLength": 5, "maxLength": 20, "pattern": "^[A-Z0-9]+$"}', 'UPPERCASE',
    'Unique account identifier', 'N', 'INTERNAL', 'qa_test_user', SYSDATE
);

INSERT INTO MASTER_QUERY_COLUMNS (
    COLUMN_ID, MASTER_QUERY_ID, COLUMN_NAME, COLUMN_ALIAS, COLUMN_TYPE,
    COLUMN_LENGTH, COLUMN_PRECISION, COLUMN_SCALE, IS_NULLABLE, IS_PRIMARY_KEY,
    COLUMN_ORDER, VALIDATION_RULES, DISPLAY_FORMAT, COLUMN_DESCRIPTION,
    IS_SENSITIVE_DATA, DATA_CLASSIFICATION, CREATED_BY, CREATED_DATE
) VALUES (
    'col_qa_002', 'mq_qa_simple_select', 'transaction_id', 'Transaction ID', 'VARCHAR2',
    30, NULL, NULL, 'N', 'N', 2,
    '{"minLength": 10, "maxLength": 30}', 'UPPERCASE',
    'Unique transaction identifier', 'N', 'INTERNAL', 'qa_test_user', SYSDATE
);

INSERT INTO MASTER_QUERY_COLUMNS (
    COLUMN_ID, MASTER_QUERY_ID, COLUMN_NAME, COLUMN_ALIAS, COLUMN_TYPE,
    COLUMN_LENGTH, COLUMN_PRECISION, COLUMN_SCALE, IS_NULLABLE, IS_PRIMARY_KEY,
    COLUMN_ORDER, VALIDATION_RULES, DISPLAY_FORMAT, COLUMN_DESCRIPTION,
    IS_SENSITIVE_DATA, DATA_CLASSIFICATION, CREATED_BY, CREATED_DATE
) VALUES (
    'col_qa_003', 'mq_qa_simple_select', 'amount', 'Transaction Amount', 'NUMBER',
    NULL, 15, 2, 'N', 'N', 3,
    '{"min": 0.01, "max": 999999.99}', 'CURRENCY',
    'Transaction amount in USD', 'Y', 'CONFIDENTIAL', 'qa_test_user', SYSDATE
);

INSERT INTO MASTER_QUERY_COLUMNS (
    COLUMN_ID, MASTER_QUERY_ID, COLUMN_NAME, COLUMN_ALIAS, COLUMN_TYPE,
    COLUMN_LENGTH, COLUMN_PRECISION, COLUMN_SCALE, IS_NULLABLE, IS_PRIMARY_KEY,
    COLUMN_ORDER, VALIDATION_RULES, DISPLAY_FORMAT, COLUMN_DESCRIPTION,
    IS_SENSITIVE_DATA, DATA_CLASSIFICATION, CREATED_BY, CREATED_DATE
) VALUES (
    'col_qa_004', 'mq_qa_simple_select', 'batch_date', 'Batch Date', 'DATE',
    NULL, NULL, NULL, 'N', 'N', 4,
    '{"format": "YYYY-MM-DD"}', 'DATE',
    'Processing batch date', 'N', 'INTERNAL', 'qa_test_user', SYSDATE
);

-- Insert additional test data for performance testing
INSERT INTO ENCORE_TEST_DATA (account_id, batch_date, amount, transaction_id, transaction_code)
SELECT 
    'QA_ACC' || LPAD(LEVEL, 4, '0'),
    DATE '2025-08-18',
    ROUND(DBMS_RANDOM.VALUE(10, 5000), 2),
    'QA_TXN' || LPAD(LEVEL, 8, '0'),
    '200'
FROM DUAL 
CONNECT BY LEVEL <= 1000;

-- Insert test data for multiple batch dates
INSERT INTO ENCORE_TEST_DATA (account_id, batch_date, amount, transaction_id, transaction_code)
SELECT 
    'QA_ACC' || LPAD(MOD(LEVEL, 100) + 1, 4, '0'),
    DATE '2025-08-17',
    ROUND(DBMS_RANDOM.VALUE(50, 3000), 2),
    'QA_TXN_17_' || LPAD(LEVEL, 6, '0'),
    '200'
FROM DUAL 
CONNECT BY LEVEL <= 500;

INSERT INTO ENCORE_TEST_DATA (account_id, batch_date, amount, transaction_id, transaction_code)
SELECT 
    'QA_ACC' || LPAD(MOD(LEVEL, 150) + 1, 4, '0'),
    DATE '2025-08-19',
    ROUND(DBMS_RANDOM.VALUE(25, 4000), 2),
    'QA_TXN_19_' || LPAD(LEVEL, 6, '0'),
    '200'
FROM DUAL 
CONNECT BY LEVEL <= 750;

COMMIT;

-- Verify test data insertion
SELECT 'MANUAL_JOB_CONFIG' as table_name, COUNT(*) as record_count 
FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID LIKE 'cfg_qa_%'
UNION ALL
SELECT 'TEMPLATE_MASTER_QUERY_MAPPING', COUNT(*) 
FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE MAPPING_ID LIKE 'tmq_qa_%'
UNION ALL
SELECT 'MASTER_QUERY_COLUMNS', COUNT(*) 
FROM MASTER_QUERY_COLUMNS WHERE COLUMN_ID LIKE 'col_qa_%'
UNION ALL
SELECT 'ENCORE_TEST_DATA (QA)', COUNT(*) 
FROM ENCORE_TEST_DATA WHERE account_id LIKE 'QA_ACC%';
```

### 2.2 Security Test Data

Create file: `/src/test/resources/qa-security-test-data.sql`

```sql
-- =========================================================================
-- QA SECURITY TEST DATA
-- Malicious queries and injection attempts for security testing
-- =========================================================================

-- Test queries for SQL injection detection
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL,
    QUERY_DESCRIPTION, STATUS, IS_READ_ONLY, CREATED_BY, CREATED_DATE
) VALUES (
    'tmq_qa_security_001', 'cfg_qa_001', 'mq_qa_injection_test', 'SQL Injection Test Query',
    'SELECT account_id FROM ENCORE_TEST_DATA WHERE account_id = :accountId; DROP TABLE users; --',
    'Malicious query for injection testing - should be rejected',
    'INACTIVE', 'Y', 'qa_security_user', SYSDATE
);

INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL,
    QUERY_DESCRIPTION, STATUS, IS_READ_ONLY, CREATED_BY, CREATED_DATE
) VALUES (
    'tmq_qa_security_002', 'cfg_qa_001', 'mq_qa_union_attack', 'Union Injection Test',
    'SELECT account_id FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate UNION SELECT password FROM users',
    'Union-based injection attempt - should be rejected',
    'INACTIVE', 'Y', 'qa_security_user', SYSDATE
);

INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL,
    QUERY_DESCRIPTION, STATUS, IS_READ_ONLY, CREATED_BY, CREATED_DATE
) VALUES (
    'tmq_qa_security_003', 'cfg_qa_001', 'mq_qa_dml_attempt', 'DML Operation Test',
    'UPDATE ENCORE_TEST_DATA SET amount = 0 WHERE account_id = :accountId',
    'DML operation attempt - should be rejected',
    'INACTIVE', 'Y', 'qa_security_user', SYSDATE
);

COMMIT;
```

### 2.3 Performance Test Data

Create file: `/src/test/resources/qa-performance-test-data.sql`

```sql
-- =========================================================================
-- QA PERFORMANCE TEST DATA
-- Large dataset for performance and load testing
-- =========================================================================

-- Create large dataset for performance testing (10,000 records)
DECLARE
    v_batch_date DATE;
    v_account_id VARCHAR2(20);
    v_amount NUMBER(15,2);
    v_transaction_id VARCHAR2(30);
BEGIN
    FOR i IN 1..10000 LOOP
        v_batch_date := DATE '2025-08-18' + MOD(i, 7); -- 7 different dates
        v_account_id := 'PERF_ACC' || LPAD(MOD(i, 1000) + 1, 6, '0');
        v_amount := ROUND(DBMS_RANDOM.VALUE(1, 10000), 2);
        v_transaction_id := 'PERF_TXN' || LPAD(i, 10, '0');
        
        INSERT INTO ENCORE_TEST_DATA (
            account_id, batch_date, amount, transaction_id, transaction_code
        ) VALUES (
            v_account_id, v_batch_date, v_amount, v_transaction_id, '200'
        );
        
        -- Commit every 1000 records
        IF MOD(i, 1000) = 0 THEN
            COMMIT;
        END IF;
    END LOOP;
    COMMIT;
END;
/

-- Create performance test query mappings
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL,
    QUERY_DESCRIPTION, STATUS, IS_READ_ONLY, CREATED_BY, CREATED_DATE
) VALUES (
    'tmq_qa_perf_001', 'cfg_qa_001', 'mq_qa_large_result', 'Large Result Set Query',
    'SELECT account_id, amount, transaction_id FROM ENCORE_TEST_DATA WHERE account_id LIKE ''PERF_ACC%'' ORDER BY amount DESC',
    'Query returning large result set for performance testing',
    'ACTIVE', 'Y', 'qa_perf_user', SYSDATE
);

INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL,
    QUERY_DESCRIPTION, STATUS, IS_READ_ONLY, CREATED_BY, CREATED_DATE
) VALUES (
    'tmq_qa_perf_002', 'cfg_qa_001', 'mq_qa_complex_aggregation', 'Complex Aggregation Query',
    'SELECT account_id, AVG(amount) as avg_amount, SUM(amount) as total_amount, COUNT(*) as transaction_count, MIN(amount) as min_amount, MAX(amount) as max_amount FROM ENCORE_TEST_DATA WHERE account_id LIKE ''PERF_ACC%'' AND batch_date >= :startDate AND batch_date <= :endDate GROUP BY account_id HAVING COUNT(*) > :minTransactions ORDER BY total_amount DESC',
    'Complex aggregation for performance testing',
    'ACTIVE', 'Y', 'qa_perf_user', SYSDATE
);

COMMIT;

-- Verify performance test data
SELECT 'Performance Test Data Created' as status, COUNT(*) as record_count 
FROM ENCORE_TEST_DATA WHERE account_id LIKE 'PERF_ACC%';
```

---

## 3. Application Configuration

### 3.1 QA Application Properties

Create file: `/src/test/resources/application-qa.yml`

```yaml
# QA Environment Configuration
spring:
  profiles:
    active: qa
  
  datasource:
    url: jdbc:oracle:thin:@localhost:1521/ORCLPDB1
    username: cm3int_qa
    password: QATestPass123
    driver-class-name: oracle.jdbc.OracleDriver
    
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      pool-name: QA-MasterQuery-Pool

  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    contexts: qa,development
    enabled: true

# Master Query Configuration
master-query:
  max-execution-time-seconds: 30
  max-result-rows: 100
  read-only-mode: true
  audit-enabled: true
  
# Security Configuration
security:
  jwt:
    secret: qa-test-secret-key-for-testing-only
    expiration: 3600000 # 1 hour
  
  audit:
    enabled: true
    correlation-id-header: X-Correlation-ID
    
# Logging Configuration
logging:
  level:
    com.truist.batch: DEBUG
    org.springframework.security: INFO
    org.springframework.jdbc: DEBUG
    org.springframework.web: INFO
  
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
  
  file:
    name: logs/fabric-api-qa.log
    max-size: 100MB
    max-history: 30

# Management and Monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /
```

### 3.2 Test JWT Token Generation

Create file: `/src/test/resources/generate-test-tokens.sh`

```bash
#!/bin/bash

# Generate JWT tokens for different roles for QA testing
# Usage: ./generate-test-tokens.sh

echo "Generating QA Test JWT Tokens..."

# Base64 encode function
base64_encode() {
    echo -n "$1" | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n'
}

# JWT Header
HEADER='{"alg":"HS256","typ":"JWT"}'
HEADER_B64=$(base64_encode "$HEADER")

# Secret key (must match application-qa.yml)
SECRET="qa-test-secret-key-for-testing-only"

# Generate tokens for different roles
declare -A ROLES=(
    ["JOB_VIEWER"]="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
    ["JOB_CREATOR"]="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
    ["JOB_MODIFIER"]="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
    ["JOB_EXECUTOR"]="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
    ["ADMIN"]="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
)

echo "QA Test Tokens (Valid for 1 hour):"
echo "================================="

for role in "${!ROLES[@]}"; do
    # Create payload
    EXP=$(($(date +%s) + 3600)) # 1 hour expiration
    PAYLOAD="{\"sub\":\"qa_test_user\",\"role\":\"$role\",\"exp\":$EXP,\"iat\":$(date +%s)}"
    PAYLOAD_B64=$(base64_encode "$PAYLOAD")
    
    # Create signature (simplified for testing - use proper JWT library in production)
    SIGNATURE=$(echo -n "${HEADER_B64}.${PAYLOAD_B64}" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n')
    
    TOKEN="${HEADER_B64}.${PAYLOAD_B64}.${SIGNATURE}"
    
    echo "$role: Bearer $TOKEN"
    echo ""
done

echo "Usage in curl commands:"
echo "curl -H \"Authorization: Bearer <token>\" ..."
```

---

## 4. Testing Tools Setup

### 4.1 Postman Collection

Create file: `/src/test/resources/Master_Query_QA_Collection.postman_collection.json`

```json
{
  "info": {
    "name": "Master Query QA Testing Collection",
    "description": "Comprehensive API testing collection for Phase 1 Master Query Integration",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080",
      "type": "string"
    },
    {
      "key": "jwtToken",
      "value": "{{jwt_token_executor}}",
      "type": "string"
    },
    {
      "key": "correlationId",
      "value": "{{$randomUUID}}",
      "type": "string"
    }
  ],
  "item": [
    {
      "name": "Query Execution Tests",
      "item": [
        {
          "name": "Execute Simple Query",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{jwtToken}}",
                "type": "text"
              },
              {
                "key": "Content-Type",
                "value": "application/json",
                "type": "text"
              },
              {
                "key": "X-Correlation-ID",
                "value": "{{correlationId}}",
                "type": "text"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"masterQueryId\": \"mq_qa_simple_select\",\n  \"queryName\": \"Simple Transaction Query\",\n  \"querySql\": \"SELECT account_id, transaction_id, amount, batch_date FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate ORDER BY account_id\",\n  \"queryParameters\": {\n    \"batchDate\": \"2025-08-18\"\n  },\n  \"maxExecutionTimeSeconds\": 30,\n  \"maxResultRows\": 100\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/api/v2/master-query/execute",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v2", "master-query", "execute"]
            }
          }
        }
      ]
    },
    {
      "name": "Security Tests",
      "item": [
        {
          "name": "SQL Injection Attempt",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{jwtToken}}",
                "type": "text"
              },
              {
                "key": "Content-Type",
                "value": "application/json",
                "type": "text"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"masterQueryId\": \"mq_qa_injection_test\",\n  \"queryName\": \"SQL Injection Test\",\n  \"querySql\": \"SELECT account_id FROM ENCORE_TEST_DATA WHERE account_id = :accountId; DROP TABLE users; --\",\n  \"queryParameters\": {\n    \"accountId\": \"QA_ACC0001\"\n  }\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/api/v2/master-query/execute",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v2", "master-query", "execute"]
            }
          }
        }
      ]
    }
  ]
}
```

### 4.2 JMeter Performance Test Plan

Create file: `/src/test/resources/Master_Query_Performance_Test.jmx`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.4.1">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Master Query Performance Test" enabled="true">
      <stringProp name="TestPlan.comments">Performance testing for Master Query API endpoints</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.tearDown_on_shutdown">true</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
        <collectionProp name="Arguments.arguments">
          <elementProp name="baseUrl" elementType="Argument">
            <stringProp name="Argument.name">baseUrl</stringProp>
            <stringProp name="Argument.value">http://localhost:8080</stringProp>
          </elementProp>
          <elementProp name="jwtToken" elementType="Argument">
            <stringProp name="Argument.name">jwtToken</stringProp>
            <stringProp name="Argument.value">Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
      <stringProp name="TestPlan.user_define_classpath"></stringProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Concurrent Query Execution" enabled="true">
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControllerGui" testclass="LoopController" testname="Loop Controller" enabled="true">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">10</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">10</stringProp>
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>
        <boolProp name="ThreadGroup.scheduler">false</boolProp>
        <stringProp name="ThreadGroup.duration"></stringProp>
        <stringProp name="ThreadGroup.delay"></stringProp>
        <boolProp name="ThreadGroup.same_user_on_next_iteration">true</boolProp>
      </ThreadGroup>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

---

## 5. Environment Verification

### 5.1 Health Check Script

Create file: `/src/test/resources/qa-health-check.sh`

```bash
#!/bin/bash

# QA Environment Health Check Script
echo "========================================="
echo "QA Environment Health Check"
echo "========================================="

# Check database connectivity
echo "1. Testing Database Connectivity..."
sqlplus -s cm3int_qa/QATestPass123@localhost:1521/ORCLPDB1 << EOF
SET PAGESIZE 0
SET FEEDBACK OFF
SELECT 'Database Connection: SUCCESS' FROM DUAL;
EXIT;
EOF

if [ $? -eq 0 ]; then
    echo "✓ Database connection successful"
else
    echo "✗ Database connection failed"
    exit 1
fi

# Check application startup
echo "2. Testing Application Startup..."
curl -s http://localhost:8080/actuator/health > /dev/null
if [ $? -eq 0 ]; then
    echo "✓ Application is running"
else
    echo "✗ Application is not responding"
    exit 1
fi

# Check API endpoints
echo "3. Testing API Endpoints..."
HEALTH_RESPONSE=$(curl -s -H "Authorization: Bearer $JWT_TOKEN" http://localhost:8080/api/v2/master-query/health)
if [[ $HEALTH_RESPONSE == *"UP"* ]]; then
    echo "✓ Master Query API is healthy"
else
    echo "✗ Master Query API health check failed"
fi

# Check test data
echo "4. Verifying Test Data..."
sqlplus -s cm3int_qa/QATestPass123@localhost:1521/ORCLPDB1 << EOF
SET PAGESIZE 0
SET FEEDBACK OFF
SELECT 'Test Data Count: ' || COUNT(*) FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE MAPPING_ID LIKE 'tmq_qa_%';
EXIT;
EOF

echo "========================================="
echo "QA Environment Health Check Complete"
echo "========================================="
```

### 5.2 Test Data Validation

Create file: `/src/test/resources/validate-test-data.sql`

```sql
-- Validate QA test data setup
SET PAGESIZE 50
SET LINESIZE 120
COLUMN table_name FORMAT A30
COLUMN record_count FORMAT 999999

PROMPT ========================================
PROMPT QA Test Data Validation Report
PROMPT ========================================

PROMPT
PROMPT 1. Core Configuration Tables:
SELECT 'MANUAL_JOB_CONFIG (QA)' as table_name, COUNT(*) as record_count 
FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID LIKE 'cfg_qa_%'
UNION ALL
SELECT 'TEMPLATE_MASTER_QUERY_MAPPING (QA)', COUNT(*) 
FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE MAPPING_ID LIKE 'tmq_qa_%'
UNION ALL
SELECT 'MASTER_QUERY_COLUMNS (QA)', COUNT(*) 
FROM MASTER_QUERY_COLUMNS WHERE COLUMN_ID LIKE 'col_qa_%';

PROMPT
PROMPT 2. Test Transaction Data:
SELECT 'ENCORE_TEST_DATA (QA)', COUNT(*) 
FROM ENCORE_TEST_DATA WHERE account_id LIKE 'QA_ACC%'
UNION ALL
SELECT 'ENCORE_TEST_DATA (PERF)', COUNT(*) 
FROM ENCORE_TEST_DATA WHERE account_id LIKE 'PERF_ACC%';

PROMPT
PROMPT 3. Test Data by Batch Date:
SELECT 'Batch Date: ' || TO_CHAR(batch_date, 'YYYY-MM-DD') as table_name, COUNT(*) as record_count
FROM ENCORE_TEST_DATA 
WHERE account_id LIKE 'QA_ACC%' OR account_id LIKE 'PERF_ACC%'
GROUP BY batch_date
ORDER BY batch_date;

PROMPT
PROMPT 4. Query Type Distribution:
SELECT 'Query Type: ' || query_type as table_name, COUNT(*) as record_count
FROM TEMPLATE_MASTER_QUERY_MAPPING 
WHERE MAPPING_ID LIKE 'tmq_qa_%'
GROUP BY query_type;

PROMPT
PROMPT 5. Security Classification:
SELECT 'Security: ' || security_classification as table_name, COUNT(*) as record_count
FROM TEMPLATE_MASTER_QUERY_MAPPING 
WHERE MAPPING_ID LIKE 'tmq_qa_%'
GROUP BY security_classification;

PROMPT ========================================
PROMPT Validation Complete
PROMPT ========================================
```

---

## 6. Common Issues and Troubleshooting

### 6.1 Database Connection Issues

**Problem**: Connection timeout or authentication failure
```bash
# Solution: Verify Oracle listener and database status
lsnrctl status
sqlplus sys/password@localhost:1521/ORCLPDB1 as sysdba
```

**Problem**: Table not found errors
```sql
-- Solution: Check if Liquibase migrations were applied
SELECT * FROM DATABASECHANGELOG WHERE ID LIKE 'us001-010%';

-- If missing, run migrations manually
liquibase update
```

### 6.2 Application Startup Issues

**Problem**: JWT authentication configuration errors
```yaml
# Solution: Verify application-qa.yml JWT settings
security:
  jwt:
    secret: qa-test-secret-key-for-testing-only
    expiration: 3600000
```

**Problem**: Connection pool exhaustion
```yaml
# Solution: Adjust Hikari settings
spring:
  datasource:
    hikari:
      maximum-pool-size: 15
      minimum-idle: 3
      leak-detection-threshold: 30000
```

### 6.3 Test Execution Issues

**Problem**: Test data conflicts
```sql
-- Solution: Clean and reload test data
DELETE FROM MASTER_QUERY_COLUMNS WHERE COLUMN_ID LIKE 'col_qa_%';
DELETE FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE MAPPING_ID LIKE 'tmq_qa_%';
-- Re-run test data scripts
```

---

## 7. Environment Cleanup

### 7.1 Post-Testing Cleanup Script

Create file: `/src/test/resources/qa-cleanup.sh`

```bash
#!/bin/bash

echo "Starting QA Environment Cleanup..."

# Clean test data
sqlplus -s cm3int_qa/QATestPass123@localhost:1521/ORCLPDB1 << EOF
DELETE FROM MASTER_QUERY_COLUMNS WHERE COLUMN_ID LIKE 'col_qa_%';
DELETE FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE MAPPING_ID LIKE 'tmq_qa_%';
DELETE FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID LIKE 'cfg_qa_%';
DELETE FROM ENCORE_TEST_DATA WHERE account_id LIKE 'QA_ACC%';
DELETE FROM ENCORE_TEST_DATA WHERE account_id LIKE 'PERF_ACC%';
COMMIT;
EXIT;
EOF

# Clean application logs
rm -f logs/fabric-api-qa.log*

# Reset application state
echo "QA Environment Cleanup Complete"
```

---

## 8. Support and Escalation

### 8.1 Contact Information

**Scrum Master**: Available for process facilitation and issue escalation  
**Development Team**: Available for technical support and bug fixes  
**Database Administrator**: Available for database-related issues  
**DevOps Team**: Available for environment and infrastructure support  

### 8.2 Escalation Matrix

| Issue Type | Primary Contact | Secondary Contact | Response Time |
|------------|-----------------|-------------------|---------------|
| Environment Setup | DevOps Team | Database Administrator | 2 hours |
| Test Data Issues | Development Team | Database Administrator | 4 hours |
| Application Bugs | Development Team | Technical Lead | 8 hours |
| Security Issues | Security Team | Technical Lead | 1 hour |

---

**Setup Guide Version**: 1.0  
**Last Updated**: August 18, 2025  
**Next Review**: Post-QA completion  
**Status**: Ready for QA environment setup  

This comprehensive setup guide ensures QA teams have all necessary resources and procedures for thorough testing of Phase 1 Master Query Integration while maintaining banking-grade quality standards.