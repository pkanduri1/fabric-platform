# 🧪 Fabric Platform - End-to-End Testing Guide

## Overview

This document provides a complete end-to-end testing scenario that validates the entire Fabric Platform workflow from UI configuration to batch processing execution. The test covers all three implemented epics (Idempotency Framework, Simple Transaction Processing, and Complex Transaction Processing).

## Test Scenario Summary

1. **Source System Configuration** via React UI
2. **SQL*Loader Control File Configuration** via React UI  
3. **API-triggered SQL*Loader File Processing**
4. **Spring Batch Simple Transaction Processing** (Epic 2)
5. **Spring Batch Complex Transaction Processing** (Epic 3)

---

## 🛠️ Prerequisites

### System Requirements
- Java 17+
- Node.js 16+
- Oracle Database 19c+ (running and accessible)
- Maven 3.8+

### Environment Setup
```bash
# Start the backend
cd /Users/pavankanduri/claude-ws/fabric-platform/fabric-core
mvn spring-boot:run -pl fabric-api

# Start the frontend (new terminal)
cd /Users/pavankanduri/claude-ws/fabric-platform/fabric-ui
npm start
```

### Test Data Preparation
Create the following test files in your data directory:

**Location:** `/Users/pavankanduri/claude-ws/fabric-platform/data/input/`

---

## 📋 Step 1: Configure Source System from UI

### Objective
Configure a new source system through the React UI to define the data source for our batch processing.

### Test Steps

1. **Access the UI**
   ```
   URL: http://localhost:3000
   ```

2. **Navigate to Source System Configuration**
   - Click on **"Configuration"** in the sidebar
   - Select **"Source Systems"** tab

3. **Create New Source System**
   ```json
   {
     "sourceSystemName": "TRUIST_CORE_BANKING",
     "sourceSystemCode": "TCB",
     "description": "Truist Core Banking System - Daily Transaction Feed",
     "businessOwner": "Data Engineering Team",
     "technicalContact": "batch-admin@truist.com",
     "dataClassification": "HIGHLY_CONFIDENTIAL",
     "complianceLevel": "SOX_PCI_DSS",
     "activeFlag": "Y"
   }
   ```

4. **Configure Connection Details**
   ```json
   {
     "connectionType": "FILE_BASED",
     "inputDirectory": "/Users/pavankanduri/claude-ws/fabric-platform/data/input",
     "outputDirectory": "/Users/pavankanduri/claude-ws/fabric-platform/data/output",
     "archiveDirectory": "/Users/pavankanduri/claude-ws/fabric-platform/data/archive",
     "errorDirectory": "/Users/pavankanduri/claude-ws/fabric-platform/data/error",
     "filePattern": "TCB_TRANSACTIONS_*.dat",
     "processingSchedule": "DAILY_2AM"
   }
   ```

5. **Set Security Configuration**
   ```json
   {
     "encryptionRequired": true,
     "auditLevel": "DETAILED",
     "retentionPolicyDays": 2555,
     "piiDataPresent": true,
     "dataLineageRequired": true
   }
   ```

### Expected Results
- ✅ Source system successfully created
- ✅ System generates unique `source_system_id`
- ✅ Audit trail entry created in `SOURCE_SYSTEM_AUDIT` table
- ✅ Configuration appears in source system dropdown for subsequent steps

---

## 📋 Step 2: Configure SQL*Loader CTL Files from UI

### Objective
Create and configure SQL*Loader control files through the UI for processing transaction files.

### Test Steps

1. **Navigate to SQL*Loader Configuration**
   - Click on **"SQL*Loader"** in the main navigation
   - Select **"Control File Configuration"**

2. **Create Simple Transaction CTL File**

   **Configuration Name:** `tcb_simple_transactions.ctl`
   
   ```sql
   LOAD DATA
   CHARACTERSET UTF8
   INFILE 'TCB_SIMPLE_TRANSACTIONS_*.dat'
   BADFILE 'TCB_SIMPLE_TRANSACTIONS_*.bad'
   DISCARDFILE 'TCB_SIMPLE_TRANSACTIONS_*.dsc'
   APPEND
   INTO TABLE BATCH_STAGING_SIMPLE_TXN
   FIELDS TERMINATED BY '|' OPTIONALLY ENCLOSED BY '"'
   TRAILING NULLCOLS
   (
       TRANSACTION_ID              CHAR(50),
       TRANSACTION_DATE            DATE "YYYY-MM-DD HH24:MI:SS",
       ACCOUNT_NUMBER              CHAR(20),
       TRANSACTION_TYPE            CHAR(10),
       AMOUNT                      DECIMAL EXTERNAL,
       CURRENCY_CODE               CHAR(3) DEFAULT 'USD',
       DESCRIPTION                 CHAR(500),
       REFERENCE_NUMBER            CHAR(50),
       BRANCH_CODE                 CHAR(10),
       PROCESSING_DATE             DATE "YYYY-MM-DD" DEFAULT SYSDATE,
       CREATED_BY                  CONSTANT 'SQL_LOADER',
       CREATED_TIMESTAMP           TIMESTAMP DEFAULT SYSTIMESTAMP
   )
   ```

3. **Create Complex Transaction CTL File**

   **Configuration Name:** `tcb_complex_transactions.ctl`
   
   ```sql
   LOAD DATA
   CHARACTERSET UTF8
   INFILE 'TCB_COMPLEX_TRANSACTIONS_*.dat'
   BADFILE 'TCB_COMPLEX_TRANSACTIONS_*.bad'
   DISCARDFILE 'TCB_COMPLEX_TRANSACTIONS_*.dsc'
   APPEND
   INTO TABLE BATCH_STAGING_COMPLEX_TXN
   FIELDS TERMINATED BY '|' OPTIONALLY ENCLOSED BY '"'
   TRAILING NULLCOLS
   (
       TRANSACTION_ID              CHAR(50),
       PARENT_TRANSACTION_ID       CHAR(50),
       TRANSACTION_DATE            DATE "YYYY-MM-DD HH24:MI:SS",
       ACCOUNT_NUMBER              CHAR(20),
       TRANSACTION_TYPE            CHAR(10),
       AMOUNT                      DECIMAL EXTERNAL,
       CURRENCY_CODE               CHAR(3) DEFAULT 'USD',
       DESCRIPTION                 CHAR(500),
       REFERENCE_NUMBER            CHAR(50),
       DEPENDENCY_TYPE             CHAR(20),
       SEQUENCE_ORDER              INTEGER EXTERNAL,
       PROCESSING_PRIORITY         INTEGER EXTERNAL DEFAULT 1,
       BRANCH_CODE                 CHAR(10),
       PROCESSING_DATE             DATE "YYYY-MM-DD" DEFAULT SYSDATE,
       CREATED_BY                  CONSTANT 'SQL_LOADER',
       CREATED_TIMESTAMP           TIMESTAMP DEFAULT SYSTIMESTAMP
   )
   ```

4. **Configure SQL*Loader Parameters**
   ```json
   {
     "sourceSystemId": "[FROM_STEP_1]",
     "maxErrors": 100,
     "maxDiscards": 50,
     "commitFrequency": 1000,
     "parallelLoads": 2,
     "directPath": true,
     "skipIndexMaintenance": false,
     "preProcessingScript": "BEGIN UPDATE BATCH_PROCESSING_STATUS SET STATUS='LOADING' WHERE EXECUTION_ID=:execution_id; END;",
     "postProcessingScript": "BEGIN UPDATE BATCH_PROCESSING_STATUS SET STATUS='LOADED' WHERE EXECUTION_ID=:execution_id; END;"
   }
   ```

### Expected Results
- ✅ Control files created and stored in database
- ✅ Configuration validated for syntax and table existence
- ✅ CTL files available for API trigger in next step
- ✅ Audit entries created for configuration changes

---

## 📋 Step 3: Trigger API to SQL*Loader File Processing

### Objective
Use REST API to trigger SQL*Loader processing for both simple and complex transaction files.

### Test Data Files

First, create the test data files:

**File:** `TCB_SIMPLE_TRANSACTIONS_20250808.dat`
```
TXN001|2025-08-08 09:15:30|1234567890123456|DEBIT|1250.50|USD|ATM Withdrawal - Main St|REF001|B001
TXN002|2025-08-08 09:30:45|1234567890123456|CREDIT|2000.00|USD|Direct Deposit - Salary|REF002|B001
TXN003|2025-08-08 10:15:20|2345678901234567|DEBIT|45.75|USD|POS Purchase - Coffee Shop|REF003|B002
TXN004|2025-08-08 10:45:10|2345678901234567|TRANSFER|500.00|USD|Online Transfer to Savings|REF004|B002
TXN005|2025-08-08 11:20:35|3456789012345678|CREDIT|150.25|USD|Mobile Check Deposit|REF005|B003
```

**File:** `TCB_COMPLEX_TRANSACTIONS_20250808.dat`
```
CTXN001||2025-08-08 14:30:00|1234567890123456|WIRE_INIT|5000.00|USD|International Wire - Setup|WREF001|SEQUENTIAL|1|1|B001
CTXN002|CTXN001|2025-08-08 14:30:01|1234567890123456|WIRE_VALIDATE|5000.00|USD|International Wire - Validation|WREF001|SEQUENTIAL|2|1|B001
CTXN003|CTXN002|2025-08-08 14:30:02|1234567890123456|WIRE_EXECUTE|5000.00|USD|International Wire - Execution|WREF001|SEQUENTIAL|3|1|B001
CTXN004|CTXN003|2025-08-08 14:30:03|1234567890123456|WIRE_CONFIRM|5000.00|USD|International Wire - Confirmation|WREF001|SEQUENTIAL|4|1|B001
CTXN005||2025-08-08 15:45:00|2345678901234567|LOAN_INIT|25000.00|USD|Mortgage Loan - Setup|LREF001|PARALLEL|1|2|B002
CTXN006|CTXN005|2025-08-08 15:45:01|2345678901234567|LOAN_VALIDATE|25000.00|USD|Mortgage Loan - Credit Check|LREF001|PARALLEL|1|2|B002
```

### API Test Steps

1. **Initialize Execution Context**
   ```bash
   curl -X POST http://localhost:8080/api/v1/batch/execution/initialize \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]" \
     -d '{
       "executionId": "E2E_TEST_20250808_001",
       "sourceSystemId": "[FROM_STEP_1]",
       "correlationId": "E2E_CORR_001",
       "businessDate": "2025-08-08",
       "requestedBy": "test_user"
     }'
   ```

2. **Trigger Simple Transaction SQL*Loader**
   ```bash
   curl -X POST http://localhost:8080/api/v1/sqlloader/execute \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]" \
     -d '{
       "executionId": "E2E_TEST_20250808_001",
       "controlFileName": "tcb_simple_transactions.ctl",
       "dataFileName": "TCB_SIMPLE_TRANSACTIONS_20250808.dat",
       "sourceSystemId": "[FROM_STEP_1]",
       "processingMode": "IMMEDIATE",
       "validateOnly": false
     }'
   ```

3. **Trigger Complex Transaction SQL*Loader**
   ```bash
   curl -X POST http://localhost:8080/api/v1/sqlloader/execute \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]" \
     -d '{
       "executionId": "E2E_TEST_20250808_002",
       "controlFileName": "tcb_complex_transactions.ctl", 
       "dataFileName": "TCB_COMPLEX_TRANSACTIONS_20250808.dat",
       "sourceSystemId": "[FROM_STEP_1]",
       "processingMode": "IMMEDIATE",
       "validateOnly": false
     }'
   ```

4. **Monitor SQL*Loader Status**
   ```bash
   # Check simple transaction loading status
   curl -X GET http://localhost:8080/api/v1/sqlloader/status/E2E_TEST_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"

   # Check complex transaction loading status  
   curl -X GET http://localhost:8080/api/v1/sqlloader/status/E2E_TEST_20250808_002 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"
   ```

### Expected Results
- ✅ API returns 202 (Accepted) for execution requests
- ✅ SQL*Loader processes execute successfully
- ✅ Data loaded into `BATCH_STAGING_SIMPLE_TXN` and `BATCH_STAGING_COMPLEX_TXN` tables
- ✅ Load statistics captured in `SQL_LOADER_EXECUTION` table
- ✅ Status endpoints return detailed execution progress
- ✅ Bad/discard files created (should be empty for clean test data)

---

## 📋 Step 4: Spring Batch Simple Transaction Processing (Epic 2)

### Objective
Execute Epic 2 parallel transaction processing on the loaded simple transaction data.

### Test Steps

1. **Configure Transaction Types for Parallel Processing**
   ```bash
   curl -X POST http://localhost:8080/api/v1/batch/transaction-types \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]" \
     -d '{
       "transactionType": "SIMPLE_PROCESSING",
       "description": "Simple Transaction Parallel Processing",
       "parallelThreads": 4,
       "chunkSize": 1000,
       "isolationLevel": "READ_COMMITTED",
       "complianceLevel": "SOX_PCI_DSS",
       "enableIdempotency": true,
       "sourceTableName": "BATCH_STAGING_SIMPLE_TXN",
       "targetTableName": "PROCESSED_SIMPLE_TRANSACTIONS"
     }'
   ```

2. **Trigger Epic 2 Parallel Processing Job**
   ```bash
   curl -X POST http://localhost:8080/api/v1/batch/jobs/epic2/execute \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]" \
     -d '{
       "jobName": "simpleTransactionProcessingJob",
       "executionId": "EPIC2_E2E_20250808_001",
       "transactionTypeId": "[FROM_TRANSACTION_TYPE_CREATION]",
       "sourceExecutionId": "E2E_TEST_20250808_001",
       "parameters": {
         "businessDate": "2025-08-08",
         "processingMode": "PARALLEL",
         "enablePerformanceMonitoring": "true",
         "auditLevel": "DETAILED",
         "correlationId": "E2E_EPIC2_CORR_001"
       }
     }'
   ```

3. **Monitor Epic 2 Job Execution**
   ```bash
   # Check job status
   curl -X GET http://localhost:8080/api/v1/batch/jobs/status/EPIC2_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"

   # Check performance metrics
   curl -X GET http://localhost:8080/api/v1/batch/monitoring/epic2/metrics/EPIC2_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"

   # Check parallel processing status
   curl -X GET http://localhost:8080/api/v1/batch/processing/status/EPIC2_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"
   ```

4. **Validate Processing Results**
   ```bash
   # Get processing summary
   curl -X GET http://localhost:8080/api/v1/batch/results/summary/EPIC2_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"

   # Get audit trail
   curl -X GET http://localhost:8080/api/v1/batch/audit/execution/EPIC2_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"
   ```

### Expected Results
- ✅ Job executes with parallel processing across 4 threads
- ✅ All 5 simple transactions processed successfully
- ✅ Processing time shows 70%+ improvement over single-threaded processing
- ✅ Performance metrics captured for parallel processing efficiency
- ✅ Idempotency keys generated and duplicate prevention working
- ✅ Comprehensive audit trail created with banking-grade compliance
- ✅ Data integrity maintained across parallel partitions

---

## 📋 Step 5: Spring Batch Complex Transaction Processing (Epic 3)

### Objective
Execute Epic 3 complex transaction processing with dependency management and staging on the loaded complex transaction data.

### Test Steps

1. **Configure Complex Transaction Dependencies**
   ```bash
   curl -X POST http://localhost:8080/api/v1/batch/dependencies \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]" \
     -d '{
       "dependencies": [
         {
           "sourceTransactionId": "WIRE_INIT",
           "targetTransactionId": "WIRE_VALIDATE", 
           "dependencyType": "SEQUENTIAL",
           "priorityWeight": 1,
           "maxWaitTimeSeconds": 300
         },
         {
           "sourceTransactionId": "WIRE_VALIDATE",
           "targetTransactionId": "WIRE_EXECUTE",
           "dependencyType": "SEQUENTIAL", 
           "priorityWeight": 1,
           "maxWaitTimeSeconds": 300
         },
         {
           "sourceTransactionId": "WIRE_EXECUTE",
           "targetTransactionId": "WIRE_CONFIRM",
           "dependencyType": "SEQUENTIAL",
           "priorityWeight": 1, 
           "maxWaitTimeSeconds": 300
         },
         {
           "sourceTransactionId": "LOAN_INIT",
           "targetTransactionId": "LOAN_VALIDATE",
           "dependencyType": "PARALLEL",
           "priorityWeight": 2,
           "maxWaitTimeSeconds": 600
         }
       ]
     }'
   ```

2. **Create Temporary Staging Table**
   ```bash
   curl -X POST http://localhost:8080/api/v1/batch/staging/create \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]" \
     -d '{
       "executionId": "EPIC3_E2E_20250808_001",
       "transactionTypeId": 200,
       "schemaDefinition": {
         "columns": [
           {"name": "TRANSACTION_ID", "type": "VARCHAR2(50)", "nullable": false},
           {"name": "PARENT_TRANSACTION_ID", "type": "VARCHAR2(50)", "nullable": true},
           {"name": "TRANSACTION_DATE", "type": "TIMESTAMP", "nullable": false},
           {"name": "ACCOUNT_NUMBER", "type": "VARCHAR2(20)", "nullable": false},
           {"name": "TRANSACTION_TYPE", "type": "VARCHAR2(10)", "nullable": false},
           {"name": "AMOUNT", "type": "NUMBER(15,2)", "nullable": false},
           {"name": "DEPENDENCY_TYPE", "type": "VARCHAR2(20)", "nullable": true},
           {"name": "SEQUENCE_ORDER", "type": "NUMBER(10)", "nullable": true},
           {"name": "PROCESSING_STATUS", "type": "VARCHAR2(20)", "nullable": true}
         ]
       },
       "expectedRecordCount": 10000,
       "securityRequired": true,
       "ttlHours": 24
     }'
   ```

3. **Configure Header/Footer Templates**
   ```bash
   # Create header template
   curl -X POST http://localhost:8080/api/v1/batch/templates \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]" \
     -d '{
       "templateName": "complex_transaction_header",
       "templateType": "HEADER",
       "outputFormat": "CSV",
       "templateContent": "Complex Transaction Processing Report\\nGenerated: ${currentDateTime}\\nExecution ID: ${executionId}\\nTotal Records: ${totalRecords}\\n\\nTransaction ID,Parent ID,Type,Amount,Status,Processing Order\\n",
       "activeFlag": "Y"
     }'

   # Create footer template  
   curl -X POST http://localhost:8080/api/v1/batch/templates \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]" \
     -d '{
       "templateName": "complex_transaction_footer",
       "templateType": "FOOTER",
       "outputFormat": "CSV", 
       "templateContent": "\\nProcessing Summary:\\nTotal Records Processed: ${totalRecords}\\nSuccessful: ${successfulRecords}\\nFailed: ${failedRecords}\\nProcessing Time: ${processingTimeMs}ms\\nCompleted: ${currentDateTime}\\n",
       "activeFlag": "Y"
     }'
   ```

4. **Trigger Epic 3 Complex Processing Job**
   ```bash
   curl -X POST http://localhost:8080/api/v1/batch/jobs/epic3/execute \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]" \
     -d '{
       "jobName": "complexTransactionProcessingJob",
       "executionId": "EPIC3_E2E_20250808_001",
       "sourceExecutionId": "E2E_TEST_20250808_002",
       "parameters": {
         "businessDate": "2025-08-08",
         "processingMode": "DEPENDENCY_AWARE",
         "enableTempStaging": "true",
         "enableHeaderFooter": "true",
         "headerTemplate": "complex_transaction_header",
         "footerTemplate": "complex_transaction_footer",
         "auditLevel": "COMPREHENSIVE",
         "correlationId": "E2E_EPIC3_CORR_001"
       }
     }'
   ```

5. **Monitor Epic 3 Execution Progress**
   ```bash
   # Check dependency resolution status
   curl -X GET http://localhost:8080/api/v1/batch/dependencies/status/EPIC3_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"

   # Check staging table status
   curl -X GET http://localhost:8080/api/v1/batch/staging/status/EPIC3_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"

   # Check job execution status
   curl -X GET http://localhost:8080/api/v1/batch/jobs/status/EPIC3_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"

   # Monitor performance metrics
   curl -X GET http://localhost:8080/api/v1/batch/monitoring/epic3/metrics/EPIC3_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"
   ```

6. **Validate Complex Processing Results**
   ```bash
   # Get dependency execution graph
   curl -X GET http://localhost:8080/api/v1/batch/dependencies/graph/EPIC3_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"

   # Get generated header/footer content
   curl -X GET http://localhost:8080/api/v1/batch/templates/generated/EPIC3_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"

   # Get comprehensive processing summary
   curl -X GET http://localhost:8080/api/v1/batch/results/comprehensive/EPIC3_E2E_20250808_001 \
     -H "Authorization: Bearer [YOUR_JWT_TOKEN]"
   ```

### Expected Results
- ✅ Dependency graph successfully built and validated for transaction sequences
- ✅ Wire transactions process in sequential order: INIT → VALIDATE → EXECUTE → CONFIRM
- ✅ Loan transactions process in parallel with independent validation
- ✅ Temporary staging table created, used, and cleaned up automatically
- ✅ Header/footer templates processed with dynamic variable substitution
- ✅ Sub-second dependency resolution for complex transaction graphs
- ✅ All compliance and security requirements maintained
- ✅ Performance targets met (dependency resolution <10s, achieved <5s)

---

## 🔍 Overall Test Validation

### Success Criteria Checklist

#### **Epic 1: Idempotency Framework**
- [ ] All operations have unique idempotency keys
- [ ] Duplicate requests properly prevented
- [ ] Safe job restart capabilities demonstrated
- [ ] Comprehensive audit trails maintained

#### **Epic 2: Simple Transaction Processing**  
- [ ] Parallel processing achieves 70%+ performance improvement
- [ ] All partitions process successfully with data integrity
- [ ] Performance monitoring captures detailed metrics
- [ ] Banking-grade security and compliance maintained

#### **Epic 3: Complex Transaction Processing**
- [ ] Dependency management handles sequential and parallel flows
- [ ] Temporary staging lifecycle managed automatically  
- [ ] Header/footer generation with template variables
- [ ] Sub-second dependency resolution achieved

#### **End-to-End Integration**
- [ ] UI configuration flows seamlessly to API execution
- [ ] SQL*Loader integration works with Spring Batch processing
- [ ] Data flows correctly through all system components
- [ ] All audit trails and monitoring data captured
- [ ] Performance targets met across all processing stages

### Performance Benchmarks

| Component | Target | Expected Result |
|-----------|--------|-----------------|
| SQL*Loader Execution | <30s for 10K records | Should achieve <15s |
| Epic 2 Parallel Processing | 70% improvement | Should achieve 73%+ |  
| Epic 3 Dependency Resolution | <10s | Should achieve <5s |
| End-to-End Processing | <5 minutes total | Should achieve <3 minutes |
| Data Integrity | 100% accuracy | Zero data corruption |

---

## 🚨 Troubleshooting Guide

### Common Issues and Solutions

1. **UI Configuration Not Saving**
   - Check browser console for JavaScript errors
   - Verify backend API endpoints are accessible
   - Ensure proper authentication tokens

2. **SQL*Loader Execution Failures**
   - Verify Oracle database connectivity
   - Check control file syntax and table existence
   - Confirm file permissions and directory access

3. **Spring Batch Job Failures**
   - Check job parameters and configuration
   - Verify database schema and table structures  
   - Review application logs for detailed error messages

4. **Performance Issues**
   - Monitor system resources (CPU, memory, I/O)
   - Check database connection pool settings
   - Verify optimal chunk sizes and thread counts

### Log File Locations
- **Backend Logs:** `/Users/pavankanduri/claude-ws/fabric-platform/fabric-core/fabric-api/logs/`
- **Frontend Logs:** Browser developer console
- **SQL*Loader Logs:** `/Users/pavankanduri/claude-ws/fabric-platform/data/logs/`
- **Database Logs:** Oracle alert.log and trace files

---

## 📊 Test Execution Summary Template

```markdown
## E2E Test Execution Report - [DATE]

### Test Environment
- Backend Version: [VERSION]
- Frontend Version: [VERSION]  
- Database Version: Oracle 19c
- Java Version: 17

### Execution Results
- [ ] Step 1: Source System Configuration - PASS/FAIL
- [ ] Step 2: SQL*Loader CTL Configuration - PASS/FAIL
- [ ] Step 3: API SQL*Loader Processing - PASS/FAIL
- [ ] Step 4: Epic 2 Simple Processing - PASS/FAIL
- [ ] Step 5: Epic 3 Complex Processing - PASS/FAIL

### Performance Results
| Metric | Target | Actual | Status |
|--------|--------|---------|---------|
| Total E2E Time | <5 min | [ACTUAL] | PASS/FAIL |
| Epic 2 Improvement | 70% | [ACTUAL]% | PASS/FAIL |
| Epic 3 Dependency Res | <10s | [ACTUAL]s | PASS/FAIL |

### Issues Encountered
[LIST ANY ISSUES AND THEIR RESOLUTIONS]

### Overall Result: PASS/FAIL
```

This comprehensive end-to-end testing guide validates the complete Fabric Platform implementation from UI configuration through complex batch processing execution! 🎯