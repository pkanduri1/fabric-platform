# Phase 1 Master Query Integration - Comprehensive Test Plan

## Executive Summary

This test plan provides detailed testing procedures for Phase 1 Master Query Integration implementation, coordinated by the Scrum Master to ensure banking-grade quality standards and regulatory compliance.

**Test Plan Version**: 1.0  
**Feature**: Master Query Integration - Phase 1  
**Target Release**: Phase 1 Backend Implementation  
**Testing Period**: August 20 - September 3, 2025  

---

## 1. Test Strategy Overview

### 1.1 Testing Approach

Our testing strategy follows a comprehensive multi-layered approach aligned with banking industry standards:

- **Unit Testing**: Individual component validation
- **Integration Testing**: Cross-component interaction validation
- **Security Testing**: Banking-grade security compliance
- **Performance Testing**: Load and stress testing under banking workloads
- **Compliance Testing**: SOX and regulatory requirement validation

### 1.2 Testing Scope

**In Scope:**
- All 7 Master Query API endpoints
- Database schema and data integrity
- Security controls and access management
- Performance benchmarks and scalability
- Audit trail and compliance logging

**Out of Scope:**
- Frontend UI components (Phase 2)
- End-to-end user workflows (Phase 2)
- Production deployment procedures

---

## 2. Test Environment Setup

### 2.1 Environment Configuration

```bash
# Database Setup
Database: Oracle 19c
Schema: CM3INT
Connection Pool: Read-only dedicated pool
Max Connections: 10
Timeout: 30 seconds

# Application Setup
Spring Boot: 3.x
Port: 8080
Profile: test
JWT Authentication: Enabled
Audit Logging: Enabled
```

### 2.2 Test Data Requirements

**Core Test Data:**
```sql
-- Sample master query mapping
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, 
    QUERY_SQL, QUERY_DESCRIPTION, CREATED_BY
) VALUES (
    'tmq_test_001', 'cfg_test_001', 'mq_test_001',
    'Test Transaction Summary',
    'SELECT account_id, SUM(amount) as total FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate GROUP BY account_id',
    'Test query for transaction summary',
    'qa_test_user'
);

-- Sample column metadata
INSERT INTO MASTER_QUERY_COLUMNS (
    COLUMN_ID, MASTER_QUERY_ID, COLUMN_NAME, COLUMN_TYPE,
    COLUMN_ORDER, CREATED_BY
) VALUES (
    'col_test_001', 'mq_test_001', 'account_id', 'VARCHAR2',
    1, 'qa_test_user'
);
```

---

## 3. Functional Test Cases

### 3.1 Query Execution Tests

#### TC-001: Execute Valid SELECT Query
```json
{
  "testId": "TC-001",
  "title": "Execute Valid SELECT Query",
  "priority": "High",
  "endpoint": "POST /api/v2/master-query/execute",
  "preconditions": [
    "Valid JWT token with JOB_EXECUTOR role",
    "Test data populated in ENCORE_TEST_DATA",
    "Valid query mapping exists"
  ],
  "testData": {
    "masterQueryId": "mq_test_001",
    "queryName": "Test Transaction Summary",
    "querySql": "SELECT account_id, SUM(amount) as total FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate GROUP BY account_id",
    "queryParameters": {
      "batchDate": "2025-08-18"
    },
    "maxExecutionTimeSeconds": 30,
    "maxResultRows": 100
  },
  "expectedResults": {
    "httpStatus": 200,
    "responseFields": [
      "masterQueryId",
      "executionStatus: SUCCESS",
      "results",
      "rowCount",
      "executionTimeMs",
      "correlationId"
    ],
    "validations": [
      "Query execution completes within 30 seconds",
      "Results contain valid data from ENCORE_TEST_DATA",
      "Correlation ID is present and unique",
      "Audit log entry created"
    ]
  }
}
```

#### TC-002: Execute Query with Parameters
```json
{
  "testId": "TC-002",
  "title": "Execute Query with Multiple Parameters",
  "priority": "High",
  "endpoint": "POST /api/v2/master-query/execute",
  "testData": {
    "masterQueryId": "mq_test_002",
    "querySql": "SELECT * FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate AND amount >= :minAmount AND amount <= :maxAmount",
    "queryParameters": {
      "batchDate": "2025-08-18",
      "minAmount": 100.00,
      "maxAmount": 5000.00
    }
  },
  "expectedResults": {
    "httpStatus": 200,
    "validations": [
      "Parameters properly substituted in query",
      "Results filtered by all parameter conditions",
      "No SQL injection vulnerabilities"
    ]
  }
}
```

#### TC-003: Execute Complex WITH Query
```json
{
  "testId": "TC-003",
  "title": "Execute Complex WITH Query",
  "priority": "Medium",
  "testData": {
    "querySql": "WITH summary AS (SELECT account_id, SUM(amount) as total FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate GROUP BY account_id) SELECT * FROM summary WHERE total > :threshold ORDER BY total DESC"
  },
  "expectedResults": {
    "httpStatus": 200,
    "validations": [
      "WITH clause properly executed",
      "Results ordered correctly",
      "Performance within acceptable limits"
    ]
  }
}
```

### 3.2 Query Validation Tests

#### TC-004: Validate SQL Syntax
```json
{
  "testId": "TC-004",
  "title": "Validate Query Syntax",
  "priority": "High",
  "endpoint": "POST /api/v2/master-query/validate",
  "testData": {
    "querySql": "SELECT account_id, amount FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate"
  },
  "expectedResults": {
    "httpStatus": 200,
    "responseFields": [
      "valid: true",
      "syntaxValid: true",
      "correlationId"
    ]
  }
}
```

#### TC-005: Detect Invalid SQL
```json
{
  "testId": "TC-005",
  "title": "Detect Invalid SQL Syntax",
  "priority": "High",
  "testData": {
    "querySql": "SELECT account_id amount FROM ENCORE_TEST_DATA WHERE batch_date = batchDate"
  },
  "expectedResults": {
    "httpStatus": 400,
    "responseFields": [
      "valid: false",
      "syntaxValid: false",
      "errorMessage"
    ]
  }
}
```

### 3.3 Column Metadata Tests

#### TC-006: Get Query Metadata
```json
{
  "testId": "TC-006",
  "title": "Retrieve Query Column Metadata",
  "priority": "Medium",
  "endpoint": "POST /api/v2/master-query/metadata",
  "expectedResults": {
    "httpStatus": 200,
    "responseStructure": [
      {
        "name": "string",
        "type": "string",
        "length": "number",
        "nullable": "boolean",
        "order": "number"
      }
    ]
  }
}
```

---

## 4. Security Test Cases

### 4.1 Authentication Tests

#### TC-007: Access Without JWT Token
```json
{
  "testId": "TC-007",
  "title": "Unauthorized Access Without Token",
  "priority": "Critical",
  "preconditions": ["No Authorization header"],
  "expectedResults": {
    "httpStatus": 401,
    "validations": ["Request rejected", "No sensitive data exposed"]
  }
}
```

#### TC-008: Access with Invalid Token
```json
{
  "testId": "TC-008",
  "title": "Access with Invalid JWT Token",
  "priority": "Critical",
  "testData": {
    "authHeader": "Bearer invalid.jwt.token"
  },
  "expectedResults": {
    "httpStatus": 401,
    "validations": ["Token validation fails", "Access denied"]
  }
}
```

### 4.2 Authorization Tests

#### TC-009: Role-Based Access Control
```json
{
  "testId": "TC-009",
  "title": "Insufficient Role Access",
  "priority": "Critical",
  "testData": {
    "userRole": "JOB_VIEWER",
    "endpoint": "POST /api/v2/master-query/execute"
  },
  "expectedResults": {
    "httpStatus": 403,
    "validations": ["Access denied for insufficient role"]
  }
}
```

### 4.3 SQL Injection Tests

#### TC-010: SQL Injection Attempt - Union Attack
```json
{
  "testId": "TC-010",
  "title": "SQL Injection - Union Attack",
  "priority": "Critical",
  "testData": {
    "querySql": "SELECT account_id FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate UNION SELECT password FROM users"
  },
  "expectedResults": {
    "httpStatus": 400,
    "validations": [
      "Query rejected as malicious",
      "Security event logged",
      "No sensitive data exposed"
    ]
  }
}
```

#### TC-011: SQL Injection Attempt - DML Operations
```json
{
  "testId": "TC-011",
  "title": "SQL Injection - DML Operations",
  "priority": "Critical",
  "testData": {
    "querySql": "SELECT * FROM ENCORE_TEST_DATA; DROP TABLE users; --"
  },
  "expectedResults": {
    "httpStatus": 400,
    "validations": [
      "DML operations detected and blocked",
      "Query validation fails",
      "Audit trail captures attempt"
    ]
  }
}
```

#### TC-012: Parameter Injection Attack
```json
{
  "testId": "TC-012",
  "title": "Parameter-based SQL Injection",
  "priority": "Critical",
  "testData": {
    "queryParameters": {
      "batchDate": "2025-08-18'; DROP TABLE users; --"
    }
  },
  "expectedResults": {
    "httpStatus": 400,
    "validations": [
      "Parameter injection detected",
      "Parameterized query protection active"
    ]
  }
}
```

---

## 5. Performance Test Cases

### 5.1 Response Time Tests

#### TC-013: Query Execution Performance
```json
{
  "testId": "TC-013",
  "title": "Query Response Time Validation",
  "priority": "High",
  "testData": {
    "querySql": "SELECT * FROM ENCORE_TEST_DATA WHERE batch_date = :batchDate",
    "expectedRowCount": 1000
  },
  "performanceCriteria": {
    "maxResponseTime": "5 seconds",
    "avgResponseTime": "< 2 seconds",
    "maxMemoryUsage": "< 100MB"
  }
}
```

#### TC-014: Query Timeout Enforcement
```json
{
  "testId": "TC-014",
  "title": "30-Second Timeout Enforcement",
  "priority": "High",
  "testData": {
    "querySql": "SELECT * FROM (SELECT LEVEL FROM DUAL CONNECT BY LEVEL <= 1000000)",
    "maxExecutionTimeSeconds": 30
  },
  "expectedResults": {
    "queryTerminated": true,
    "timeoutEnforced": "at 30 seconds",
    "errorMessage": "Query execution timeout"
  }
}
```

### 5.2 Load Testing

#### TC-015: Concurrent Query Execution
```json
{
  "testId": "TC-015",
  "title": "Concurrent User Load Test",
  "priority": "Medium",
  "testScenario": {
    "concurrentUsers": 10,
    "queriesPerUser": 10,
    "executionPattern": "simultaneous"
  },
  "performanceCriteria": {
    "successRate": "> 95%",
    "avgResponseTime": "< 5 seconds",
    "connectionPoolUtilization": "< 80%"
  }
}
```

### 5.3 Resource Limits

#### TC-016: Result Set Limit Enforcement
```json
{
  "testId": "TC-016",
  "title": "100-Row Result Limit",
  "priority": "High",
  "testData": {
    "querySql": "SELECT * FROM ENCORE_TEST_DATA",
    "expectedRows": "> 100 available",
    "maxResultRows": 100
  },
  "expectedResults": {
    "returnedRows": 100,
    "limitEnforced": true,
    "paginationApplied": true
  }
}
```

---

## 6. Integration Test Cases

### 6.1 Database Integration

#### TC-017: Database Connectivity
```json
{
  "testId": "TC-017",
  "title": "Database Connection Pool Integration",
  "priority": "High",
  "endpoint": "GET /api/v2/master-query/connectivity/test",
  "expectedResults": {
    "httpStatus": 200,
    "responseFields": [
      "healthy: true",
      "connectionType: READ_ONLY",
      "responseTimeMs"
    ]
  }
}
```

#### TC-018: Connection Pool Exhaustion
```json
{
  "testId": "TC-018",
  "title": "Connection Pool Exhaustion Handling",
  "priority": "Medium",
  "testScenario": "Exhaust all available connections",
  "expectedResults": {
    "gracefulDegradation": true,
    "errorHandling": "appropriate",
    "recovery": "automatic"
  }
}
```

### 6.2 Audit Integration

#### TC-019: Audit Trail Generation
```json
{
  "testId": "TC-019",
  "title": "SOX-Compliant Audit Trail",
  "priority": "Critical",
  "validations": [
    "Correlation ID propagated to audit logs",
    "User attribution captured",
    "Execution time logged",
    "Query parameters logged (sanitized)",
    "Result count logged"
  ]
}
```

---

## 7. Compliance Test Cases

### 7.1 SOX Compliance

#### TC-020: Change Tracking
```json
{
  "testId": "TC-020",
  "title": "SOX-Compliant Change Tracking",
  "priority": "Critical",
  "testScenario": "Create, modify, execute query",
  "validations": [
    "All changes logged with timestamps",
    "User attribution for all operations",
    "Business justification captured",
    "Approval workflow (if required)"
  ]
}
```

### 7.2 PCI-DSS Compliance

#### TC-021: Sensitive Data Protection
```json
{
  "testId": "TC-021",
  "title": "Sensitive Data Classification",
  "priority": "Critical",
  "testData": {
    "querySql": "SELECT account_id, ssn FROM sensitive_data WHERE account_id = :accountId"
  },
  "validations": [
    "Sensitive data flagged appropriately",
    "Encryption applied where required",
    "Access logged for sensitive queries"
  ]
}
```

---

## 8. Error Handling Test Cases

### 8.1 Input Validation

#### TC-022: Invalid Query Structure
```json
{
  "testId": "TC-022",
  "title": "Invalid Request Structure",
  "priority": "Medium",
  "testData": {
    "malformedJson": "{ invalid json structure"
  },
  "expectedResults": {
    "httpStatus": 400,
    "errorMessage": "appropriate validation error"
  }
}
```

### 8.2 Database Errors

#### TC-023: Database Connection Failure
```json
{
  "testId": "TC-023",
  "title": "Database Unavailable Scenario",
  "priority": "High",
  "testScenario": "Simulate database outage",
  "expectedResults": {
    "httpStatus": 503,
    "errorHandling": "graceful",
    "userMessage": "service temporarily unavailable"
  }
}
```

---

## 9. Test Data Management

### 9.1 Test Data Requirements

**ENCORE_TEST_DATA Sample:**
```sql
-- Minimum 1000 records for performance testing
-- Various batch dates for date filtering
-- Different transaction amounts for range testing
-- Multiple account IDs for grouping operations

INSERT INTO ENCORE_TEST_DATA VALUES 
('ACC001', '2025-08-18', 1500.00, 'TXN001', '200'),
('ACC002', '2025-08-18', 2300.50, 'TXN002', '200'),
-- ... additional records
```

### 9.2 Data Cleanup Procedures

```sql
-- Post-test cleanup
DELETE FROM MASTER_QUERY_COLUMNS WHERE COLUMN_ID LIKE 'col_test_%';
DELETE FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE MAPPING_ID LIKE 'tmq_test_%';
COMMIT;
```

---

## 10. Test Execution Schedule

### 10.1 Daily Testing Schedule

| Time | Activity | Owner | Duration |
|------|----------|-------|----------|
| 09:00-09:30 | Daily Standup & Planning | Scrum Master | 30 min |
| 09:30-12:00 | Functional Testing | QA Engineers | 2.5 hours |
| 13:00-15:00 | Security Testing | Security QA | 2 hours |
| 15:00-17:00 | Performance Testing | Performance QA | 2 hours |
| 17:00-17:30 | Results Review & Planning | QA Lead | 30 min |

### 10.2 Weekly Milestones

| Week | Focus Area | Deliverables |
|------|------------|--------------|
| **Week 1** | Functional & Integration | Core functionality validated |
| **Week 2** | Security & Performance | Security certification, performance benchmarks |

---

## 11. Defect Management

### 11.1 Defect Reporting Template

```json
{
  "defectId": "DEF-XXXX",
  "title": "Brief description",
  "severity": "Critical/High/Medium/Low",
  "priority": "1/2/3/4",
  "testCase": "TC-XXX",
  "environment": "Test environment details",
  "stepsToReproduce": [
    "Step 1",
    "Step 2",
    "Step 3"
  ],
  "expectedResult": "What should happen",
  "actualResult": "What actually happened",
  "attachments": ["logs", "screenshots"],
  "assignedTo": "Developer name",
  "correlationId": "For audit trail tracking"
}
```

### 11.2 Defect Severity Guidelines

- **Critical**: Security vulnerability, data corruption, system crash
- **High**: Core functionality broken, performance degradation > 50%
- **Medium**: Feature partially working, minor performance issues
- **Low**: Documentation errors, cosmetic issues

---

## 12. Test Reporting

### 12.1 Daily Test Report Format

```json
{
  "date": "2025-08-XX",
  "testProgress": {
    "totalTestCases": 25,
    "executed": 15,
    "passed": 12,
    "failed": 2,
    "blocked": 1,
    "skipped": 0
  },
  "defectsSummary": {
    "newDefects": 2,
    "resolvedDefects": 1,
    "totalOpenDefects": 3
  },
  "riskAssessment": "Low/Medium/High",
  "blockers": ["Database connectivity issue"],
  "nextDayPlan": ["Focus on security testing"]
}
```

### 12.2 Final Test Report Structure

1. **Executive Summary**
2. **Test Execution Results**
3. **Defect Analysis**
4. **Performance Results**
5. **Security Validation**
6. **Compliance Certification**
7. **Risk Assessment**
8. **Go/No-Go Recommendation**

---

## 13. Tools & Automation

### 13.1 Testing Tools

- **API Testing**: Postman, Swagger UI
- **Performance Testing**: JMeter, LoadRunner
- **Security Testing**: OWASP ZAP, Burp Suite
- **Database Testing**: Oracle SQL Developer
- **Monitoring**: Application logs, database metrics

### 13.2 Test Automation

**Automated Test Categories:**
- Regression tests for API endpoints
- Performance baseline validation
- Security vulnerability scanning
- Database integrity checks

---

## 14. Risk Mitigation

### 14.1 High-Risk Scenarios

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Database Corruption** | Critical | Use dedicated test database, backup procedures |
| **Security Breach** | Critical | Isolated test environment, security reviews |
| **Performance Degradation** | High | Baseline measurements, performance monitoring |
| **Resource Unavailability** | Medium | Cross-training, parallel testing tracks |

---

## 15. Success Criteria

### 15.1 Acceptance Criteria

**Must Have:**
- All critical and high-priority test cases pass
- Zero critical security vulnerabilities
- Performance benchmarks met
- SOX compliance validated

**Should Have:**
- All medium-priority test cases pass
- Documentation complete and accurate
- Automation framework established

**Could Have:**
- Additional performance optimizations
- Enhanced monitoring capabilities

---

## 16. Test Environment Teardown

### 16.1 Post-Testing Activities

1. **Data Cleanup**: Remove all test data
2. **Environment Reset**: Restore to baseline configuration
3. **Documentation**: Archive test results and artifacts
4. **Knowledge Transfer**: Share lessons learned with development team

---

**Test Plan Prepared By**: Scrum Master  
**Approved By**: QA Lead, Technical Lead  
**Effective Date**: August 20, 2025  
**Review Date**: August 27, 2025  

This comprehensive test plan ensures thorough validation of Phase 1 Master Query Integration while maintaining our banking-grade quality standards and regulatory compliance requirements.