# Fabric Platform - Data Dictionary

**Schema:** CM3INT
**Database:** Oracle 19c+ (localhost:1521/ORCLPDB1)
**Last Updated:** October 5, 2025
**Database Snapshot:** October 5, 2025 17:41:17 EDT
**Version:** 1.3.0

---

## Database Status

**Connection:** jdbc:oracle:thin:@localhost:1521/ORCLPDB1
**Schema User:** cm3int
**Total Tables:** 25 (22 active + 3 system tables)

### Current Data Volumes

| Table | Row Count | Last Analyzed |
|-------|-----------|---------------|
| BATCH_CONFIGURATIONS | 7 | 2025-09-08 22:52:46 |
| BATCH_EXECUTION_RESULTS | 0 | 2025-08-15 22:00:05 |
| BATCH_JOB_AUDIT | 0 | 2025-08-01 22:14:12 |
| BATCH_JOB_EXECUTION | 0 | 2025-08-15 22:00:05 |
| BATCH_JOB_EXECUTION_CONTEXT | 0 | 2025-08-15 22:00:05 |
| BATCH_JOB_EXECUTION_PARAMS | 0 | 2025-08-15 22:00:05 |
| BATCH_JOB_INSTANCE | 0 | 2025-08-15 22:00:05 |
| BATCH_STEP_EXECUTION | 0 | 2025-08-15 22:00:05 |
| BATCH_STEP_EXECUTION_CONTEXT | 0 | 2025-08-15 22:00:05 |
| CONFIGURATION_AUDIT | 0 | 2025-08-01 22:14:12 |
| DATABASECHANGELOG | 25 | 2025-08-20 22:00:08 |
| DATABASECHANGELOGLOCK | 1 | 2025-09-15 22:09:11 |
| ENCORE_TEST_DATA | 7 | 2025-08-15 22:00:05 |
| FIELD_TEMPLATES | 366 | 2025-08-05 22:00:06 |
| FILE_TYPE_TEMPLATES | 8 | 2025-08-11 22:00:05 |
| JOB_DEFINITIONS | 0 | 2025-08-01 22:14:12 |
| MANUAL_JOB_AUDIT | 0 | 2025-08-14 22:00:03 |
| MANUAL_JOB_CONFIG | 4 | 2025-08-25 22:00:03 |
| MANUAL_JOB_EXECUTION | 0 | 2025-08-14 22:00:03 |
| MASTER_QUERY_COLUMNS | 0 | 2025-08-19 22:00:03 |
| MASTER_QUERY_CONFIG | 4 | 2025-08-26 23:32:43 |
| PIPELINE_AUDIT_LOG | 0 | 2025-09-15 22:09:10 |
| SOURCE_SYSTEMS | 4 | 2025-09-01 22:00:04 |
| TEMPLATE_MASTER_QUERY_MAPPING | 0 | 2025-08-19 22:00:03 |
| TEMPLATE_SOURCE_MAPPINGS | 45 | 2025-09-08 22:52:48 |

---

## Table of Contents

1. [Overview](#overview)
2. [Core Configuration Tables](#core-configuration-tables)
3. [Execution & Monitoring Tables](#execution--monitoring-tables)
4. [Template & Mapping Tables](#template--mapping-tables)
5. [Audit & Compliance Tables](#audit--compliance-tables)
6. [Spring Batch Framework Tables](#spring-batch-framework-tables)
7. [Reference Data Tables](#reference-data-tables)
8. [Data Types & Conventions](#data-types--conventions)
9. [Indexes & Constraints](#indexes--constraints)

---

## Overview

The Fabric Platform data dictionary documents all database objects in the CM3INT schema. This enterprise-grade system supports:

- **Batch Job Configuration Management** - US001 Manual Job Configuration
- **Batch Execution Tracking** - Phase 3 Manual Batch Execution
- **Template-Based Configuration** - Reusable job templates
- **SOX Compliance** - Complete audit trail and change management
- **Spring Batch Integration** - Native Spring Batch framework support

### Schema Summary

| Category | Tables | Purpose |
|----------|--------|---------|
| Core Configuration | 3 | Job and batch configurations |
| Execution & Monitoring | 2 | Job execution tracking and results |
| Templates & Mappings | 5 | Template definitions and field mappings |
| Audit & Compliance | 3 | SOX-compliant audit trails |
| Spring Batch Framework | 6 | Spring Batch metadata |
| Reference Data | 3 | Source systems and test data |
| **Total** | **22** | Complete platform support |

---

## Core Configuration Tables

### BATCH_CONFIGURATIONS

**Purpose:** Stores batch job configurations in JSON format for dynamic job execution.

**Usage:** Configuration-driven batch processing where configurations are stored as JSON and transformed to YAML for execution.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| ID | VARCHAR2(100) | No | - | Primary key, format: `{SOURCE_SYSTEM}_{JOB_NAME}_{TRANSACTION_TYPE}` |
| SOURCE_SYSTEM | VARCHAR2(50) | No | - | Source system identifier (e.g., MTG, ENCORE, SHAW) |
| JOB_NAME | VARCHAR2(50) | No | - | Job template name (e.g., atoctran-900) |
| TRANSACTION_TYPE | VARCHAR2(20) | No | '200' | Transaction code (200, 300, 900, etc.) |
| DESCRIPTION | VARCHAR2(500) | Yes | - | Human-readable configuration description |
| CONFIGURATION_JSON | CLOB | No | - | Complete field mappings and transformation rules in JSON format |
| CREATED_BY | VARCHAR2(50) | No | - | User who created the configuration |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Configuration creation timestamp |
| MODIFIED_BY | VARCHAR2(50) | Yes | - | Last user to modify configuration |
| MODIFIED_DATE | TIMESTAMP(6) | Yes | - | Last modification timestamp |
| VERSION | NUMBER | No | 1 | Configuration version number for optimistic locking |
| ENABLED | VARCHAR2(1) | No | 'Y' | Active status flag (Y/N) |

**Key Relationships:**
- Referenced by `CONFIGURATION_AUDIT.CONFIG_ID`
- Referenced by `BATCH_EXECUTION_RESULTS.JOB_CONFIG_ID`

**Configuration JSON Structure:**
```json
{
  "id": "MTG_atoctran-900_900",
  "sourceSystem": "MTG",
  "jobName": "atoctran-900",
  "transactionType": "900",
  "fieldMappings": [
    {
      "fieldName": "location-code",
      "sourceField": "location_code",
      "targetPosition": 1,
      "length": 6,
      "dataType": "string",
      "transformationType": "source"
    }
  ]
}
```

**Indexes:**
- `SYS_C007851` - UNIQUE on (ID)
- `SYS_C007852` - UNIQUE on (SOURCE_SYSTEM, JOB_NAME, TRANSACTION_TYPE)
- `IDX_BATCH_CONFIG_SYSTEM_JOB` - Non-unique on (SOURCE_SYSTEM, JOB_NAME)

---

### MANUAL_JOB_CONFIG

**Purpose:** US001 Manual Job Configuration - stores manually configured batch job definitions with parameters and validation rules.

**Usage:** Primary table for manual job configuration management with comprehensive job control settings.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| CONFIG_ID | VARCHAR2(50) | No | - | Primary key, format: `cfg_{system}_{sequence}_{date}` |
| JOB_NAME | VARCHAR2(100) | No | - | Unique job name identifier |
| JOB_TYPE | VARCHAR2(50) | No | - | Job category (ETL_BATCH, FILE_TRANSFER, etc.) |
| SOURCE_SYSTEM | VARCHAR2(50) | No | - | Source system identifier |
| TARGET_SYSTEM | VARCHAR2(50) | No | - | Target system identifier |
| JOB_PARAMETERS | CLOB | No | - | JSON-formatted job parameters (encrypted for sensitive data) |
| SCHEDULE_EXPRESSION | VARCHAR2(100) | Yes | - | Cron expression for scheduled jobs |
| STATUS | VARCHAR2(20) | No | 'ACTIVE' | Job status (ACTIVE, INACTIVE, SUSPENDED) |
| VALIDATION_RULES | CLOB | Yes | - | JSON validation rules for job execution |
| ERROR_THRESHOLD | NUMBER(5,2) | No | 5.0 | Maximum error percentage allowed (0-100) |
| RETRY_COUNT | NUMBER(2,0) | No | 3 | Number of retry attempts on failure |
| NOTIFICATION_CONFIG | CLOB | Yes | - | JSON notification configuration |
| CREATED_BY | VARCHAR2(50) | No | - | User who created the job |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Job creation timestamp |
| UPDATED_BY | VARCHAR2(50) | Yes | - | Last user to update job |
| UPDATED_DATE | TIMESTAMP(6) | Yes | - | Last update timestamp |
| VERSION_DECIMAL | NUMBER(10,0) | No | 1 | Version number for optimistic locking |
| MASTER_QUERY_ID | NUMBER(19,0) | Yes | - | FK to MASTER_QUERY_CONFIG for SQL-based jobs |

**Key Relationships:**
- References `MASTER_QUERY_CONFIG.ID` via MASTER_QUERY_ID
- Referenced by `MANUAL_JOB_EXECUTION.CONFIG_ID`
- Referenced by `MANUAL_JOB_AUDIT.CONFIG_ID`

---

### JOB_DEFINITIONS

**Purpose:** Defines job metadata including input/output paths and SQL queries.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| ID | VARCHAR2(100) | No | - | Primary key |
| SOURCE_SYSTEM_ID | VARCHAR2(50) | No | - | FK to SOURCE_SYSTEMS |
| JOB_NAME | VARCHAR2(50) | No | - | Job identifier |
| DESCRIPTION | VARCHAR2(500) | Yes | - | Job description |
| INPUT_PATH | VARCHAR2(1000) | Yes | - | Input file path or location |
| OUTPUT_PATH | VARCHAR2(1000) | Yes | - | Output file path or location |
| QUERY_SQL | CLOB | Yes | - | SQL query for data extraction |
| ENABLED | VARCHAR2(1) | No | 'Y' | Active status flag |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Creation timestamp |
| TRANSACTION_TYPES | VARCHAR2(200) | Yes | - | Comma-separated transaction types |

**Key Relationships:**
- References `SOURCE_SYSTEMS.ID` via SOURCE_SYSTEM_ID

---

## Execution & Monitoring Tables

### BATCH_EXECUTION_RESULTS

**Purpose:** Tracks batch job execution results including performance metrics and output file information.

**Usage:** Real-time monitoring and historical analysis of batch job executions.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| ID | NUMBER(19,0) | No | Identity | Primary key (auto-increment) |
| JOB_CONFIG_ID | VARCHAR2(50) | Yes | - | FK to configuration (BATCH_CONFIGURATIONS or MANUAL_JOB_CONFIG) |
| EXECUTION_ID | VARCHAR2(100) | Yes | - | Unique execution identifier |
| SOURCE_SYSTEM | VARCHAR2(50) | Yes | - | Source system for the execution |
| RECORDS_PROCESSED | NUMBER(10,0) | No | 0 | Total number of records processed |
| OUTPUT_FILE_NAME | VARCHAR2(255) | Yes | - | Generated output file name |
| OUTPUT_FILE_PATH | VARCHAR2(500) | Yes | - | Full path to output file |
| STATUS | VARCHAR2(20) | No | 'PENDING' | Execution status (PENDING, RUNNING, COMPLETED, FAILED) |
| START_TIME | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Execution start time |
| END_TIME | TIMESTAMP(6) | Yes | - | Execution end time |
| ERROR_MESSAGE | CLOB | Yes | - | Error message if execution failed |
| CORRELATION_ID | VARCHAR2(100) | Yes | - | Correlation ID for distributed tracing |

**Key Metrics:**
- **RECORDS_PROCESSED:** Track data volume
- **Duration:** END_TIME - START_TIME
- **Success Rate:** (COMPLETED / TOTAL) * 100

**Index:**
- `PK_BATCH_EXECUTION_RESULTS` - UNIQUE on (ID)

---

### MANUAL_JOB_EXECUTION

**Purpose:** Comprehensive execution tracking for US001 manual jobs with detailed performance metrics and monitoring capabilities.

**Usage:** SOX-compliant execution audit trail with performance monitoring and alerting integration.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| EXECUTION_ID | VARCHAR2(50) | No | - | Primary key, format: `exec_{jobName}_{timestamp}` |
| CONFIG_ID | VARCHAR2(50) | No | - | FK to MANUAL_JOB_CONFIG |
| JOB_NAME | VARCHAR2(100) | No | - | Job name for quick lookup |
| EXECUTION_TYPE | VARCHAR2(20) | No | 'MANUAL' | Execution trigger type (MANUAL, SCHEDULED, API) |
| TRIGGER_SOURCE | VARCHAR2(50) | Yes | - | Source that triggered execution (UI, API, Scheduler) |
| STATUS | VARCHAR2(20) | No | 'STARTED' | Current status (STARTED, RUNNING, COMPLETED, FAILED, CANCELLED) |
| START_TIME | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Execution start timestamp |
| END_TIME | TIMESTAMP(6) | Yes | - | Execution end timestamp |
| DURATION_SECONDS | NUMBER(10,2) | Yes | - | Calculated execution duration |
| RECORDS_PROCESSED | NUMBER(15,0) | Yes | - | Total records processed |
| RECORDS_SUCCESS | NUMBER(15,0) | Yes | - | Successfully processed records |
| RECORDS_ERROR | NUMBER(15,0) | Yes | - | Failed/errored records |
| ERROR_PERCENTAGE | NUMBER(5,2) | Yes | - | Calculated error rate percentage |
| ERROR_MESSAGE | VARCHAR2(4000) | Yes | - | Primary error message (truncated) |
| ERROR_STACK_TRACE | CLOB | Yes | - | Full stack trace for debugging |
| RETRY_COUNT | NUMBER(2,0) | No | 0 | Number of retry attempts |
| EXECUTION_PARAMETERS | CLOB | Yes | - | JSON execution parameters |
| EXECUTION_LOG | CLOB | Yes | - | Detailed execution log |
| OUTPUT_LOCATION | VARCHAR2(500) | Yes | - | Output file or data location |
| CORRELATION_ID | VARCHAR2(100) | Yes | - | Distributed tracing correlation ID |
| MONITORING_ALERTS_SENT | CHAR(1) | No | 'N' | Alert notification flag (Y/N) |
| EXECUTED_BY | VARCHAR2(50) | Yes | - | User who executed the job |
| EXECUTION_HOST | VARCHAR2(100) | Yes | - | Server hostname where job executed |
| EXECUTION_ENVIRONMENT | VARCHAR2(20) | Yes | - | Environment (DEV, QA, PROD) |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Record creation timestamp |

**Performance Metrics:**
- **Success Rate:** `(RECORDS_SUCCESS / RECORDS_PROCESSED) * 100`
- **Error Rate:** `ERROR_PERCENTAGE`
- **Average Duration:** `AVG(DURATION_SECONDS)` by job
- **Throughput:** `RECORDS_PROCESSED / DURATION_SECONDS` records/sec

---

## Template & Mapping Tables

### FIELD_TEMPLATES

**Purpose:** Defines reusable field templates for file types and transaction types.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| FILE_TYPE | VARCHAR2(10) | No | - | File type identifier (part of composite key) |
| TRANSACTION_TYPE | VARCHAR2(10) | No | - | Transaction code (part of composite key) |
| FIELD_NAME | VARCHAR2(50) | No | - | Field name (part of composite key) |
| TARGET_POSITION | NUMBER | No | - | Position in output file (1-based) |
| LENGTH | NUMBER | No | - | Field length in characters |
| DATA_TYPE | VARCHAR2(20) | No | - | Data type (string, number, date, decimal) |
| FORMAT | VARCHAR2(50) | Yes | - | Format specification (e.g., YYYYMMDD, 9(12)V9(6)) |
| REQUIRED | VARCHAR2(1) | No | 'N' | Mandatory field flag (Y/N) |
| DESCRIPTION | VARCHAR2(500) | Yes | - | Field description |
| CREATED_BY | VARCHAR2(50) | Yes | - | Template creator |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Creation timestamp |
| MODIFIED_BY | VARCHAR2(50) | Yes | - | Last modifier |
| MODIFIED_DATE | TIMESTAMP(6) | Yes | - | Last modification timestamp |
| VERSION | NUMBER | No | 1 | Template version |
| ENABLED | VARCHAR2(1) | No | 'Y' | Active status flag |

**Composite Primary Key:** (FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME)

---

### FILE_TYPE_TEMPLATES

**Purpose:** Defines file type metadata and specifications.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| FILE_TYPE | VARCHAR2(50) | No | - | Primary key, file type identifier |
| DESCRIPTION | VARCHAR2(200) | Yes | - | File type description |
| TOTAL_FIELDS | NUMBER | Yes | - | Total number of fields in file |
| RECORD_LENGTH | NUMBER | Yes | - | Fixed-width record length |
| CREATED_BY | VARCHAR2(50) | Yes | - | Creator username |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Creation timestamp |
| MODIFIED_BY | VARCHAR2(50) | Yes | - | Last modifier |
| MODIFIED_DATE | TIMESTAMP(6) | Yes | - | Modification timestamp |
| VERSION | NUMBER | No | 1 | Version number |
| ENABLED | VARCHAR2(1) | No | 'Y' | Active status |

---

### TEMPLATE_SOURCE_MAPPINGS

**Purpose:** Maps source fields to target fields with transformation rules for template-based configurations.

**Usage:** Stores field-level transformation mappings for batch jobs.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| ID | NUMBER | No | Identity | Primary key (auto-generated) |
| FILE_TYPE | VARCHAR2(50) | Yes | - | File type reference |
| TRANSACTION_TYPE | VARCHAR2(10) | Yes | - | Transaction code |
| SOURCE_SYSTEM_ID | VARCHAR2(50) | Yes | - | Source system identifier |
| JOB_NAME | VARCHAR2(100) | Yes | - | Associated job name |
| TARGET_FIELD_NAME | VARCHAR2(50) | Yes | - | Output field name |
| SOURCE_FIELD_NAME | VARCHAR2(100) | Yes | - | Input field name or expression |
| TRANSFORMATION_TYPE | VARCHAR2(20) | No | 'source' | Transformation type (source, constant, composite, conditional, blank) |
| TRANSFORMATION_CONFIG | VARCHAR2(1000) | Yes | - | JSON configuration for complex transformations |
| TARGET_POSITION | NUMBER | Yes | - | Position in output (1-based) |
| LENGTH | NUMBER | Yes | - | Field length |
| DATA_TYPE | VARCHAR2(20) | Yes | - | Data type |
| CREATED_BY | VARCHAR2(50) | Yes | - | Creator |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Creation timestamp |
| MODIFIED_BY | VARCHAR2(50) | Yes | - | Modifier |
| MODIFIED_DATE | TIMESTAMP(6) | Yes | - | Modification timestamp |
| VERSION | NUMBER | No | 1 | Version |
| ENABLED | VARCHAR2(1) | No | 'Y' | Status flag |

**Transformation Types:**
- **source:** Direct field mapping from source to target
- **constant:** Fixed value (stored in TRANSFORMATION_CONFIG or as literal)
- **composite:** Combine multiple fields (concat, sum)
- **conditional:** IF-THEN-ELSE logic
- **blank:** Output blank/empty value

---

### MASTER_QUERY_CONFIG

**Purpose:** Stores master SQL queries for data extraction from source systems.

**Usage:** Centralized query management with version control and security classification.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| ID | NUMBER(19,0) | No | Identity | Primary key (auto-generated) |
| SOURCE_SYSTEM | VARCHAR2(50) | Yes | - | Source system identifier |
| JOB_NAME | VARCHAR2(100) | Yes | - | Associated job name |
| QUERY_TEXT | CLOB | Yes | - | SQL query text |
| VERSION | NUMBER(10,0) | No | 1 | Query version for change tracking |
| IS_ACTIVE | VARCHAR2(1) | No | 'Y' | Active query flag (Y/N) |
| CREATED_BY | VARCHAR2(50) | Yes | - | Query author |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Creation timestamp |
| MODIFIED_BY | VARCHAR2(50) | Yes | - | Last modifier |
| MODIFIED_DATE | TIMESTAMP(6) | Yes | - | Last update timestamp |

**Key Relationships:**
- Referenced by `MANUAL_JOB_CONFIG.MASTER_QUERY_ID`
- Referenced by `TEMPLATE_MASTER_QUERY_MAPPING.MASTER_QUERY_ID`

---

### TEMPLATE_MASTER_QUERY_MAPPING

**Purpose:** Associates templates with master queries and defines query execution parameters and security controls.

**Usage:** Links job configurations to approved SQL queries with security validation.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| MAPPING_ID | VARCHAR2(50) | No | - | Primary key |
| CONFIG_ID | VARCHAR2(50) | Yes | - | FK to configuration |
| MASTER_QUERY_ID | VARCHAR2(50) | Yes | - | FK to MASTER_QUERY_CONFIG |
| QUERY_NAME | VARCHAR2(100) | Yes | - | Friendly query name |
| QUERY_SQL | CLOB | Yes | - | SQL query text (denormalized for performance) |
| QUERY_DESCRIPTION | VARCHAR2(500) | Yes | - | Query purpose description |
| QUERY_TYPE | VARCHAR2(50) | No | 'SELECT' | Query type (SELECT, INSERT, UPDATE) |
| MAX_EXECUTION_TIME_SECONDS | NUMBER(3,0) | No | 30 | Query timeout in seconds |
| MAX_RESULT_ROWS | NUMBER(5,0) | No | 100 | Maximum rows to return |
| QUERY_PARAMETERS | CLOB | Yes | - | JSON parameter definitions |
| PARAMETER_VALIDATION_RULES | CLOB | Yes | - | JSON validation rules for parameters |
| STATUS | VARCHAR2(20) | No | 'ACTIVE' | Mapping status |
| IS_READ_ONLY | CHAR(1) | No | 'Y' | Read-only enforcement flag |
| SECURITY_CLASSIFICATION | VARCHAR2(20) | No | 'INTERNAL' | Data classification (INTERNAL, CONFIDENTIAL, RESTRICTED) |
| REQUIRES_APPROVAL | CHAR(1) | No | 'Y' | Approval requirement flag |
| CREATED_BY | VARCHAR2(50) | Yes | - | Creator |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Creation timestamp |
| UPDATED_BY | VARCHAR2(50) | Yes | - | Modifier |
| UPDATED_DATE | TIMESTAMP(6) | Yes | - | Update timestamp |
| CORRELATION_ID | VARCHAR2(50) | Yes | - | Distributed tracing ID |

**Security Controls:**
- `IS_READ_ONLY='Y'` prevents data modification
- `SECURITY_CLASSIFICATION` enforces access controls
- `REQUIRES_APPROVAL='Y'` mandates approval workflow
- `MAX_EXECUTION_TIME_SECONDS` prevents resource exhaustion

---

### MASTER_QUERY_COLUMNS

**Purpose:** Metadata about columns returned by master queries.

**Usage:** Schema definition and validation for query results.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| COLUMN_ID | VARCHAR2(50) | No | - | Primary key |
| MASTER_QUERY_ID | VARCHAR2(50) | Yes | - | FK to MASTER_QUERY_CONFIG |
| COLUMN_NAME | VARCHAR2(100) | Yes | - | Column name in result set |
| COLUMN_ALIAS | VARCHAR2(100) | Yes | - | Column alias if different |
| COLUMN_TYPE | VARCHAR2(50) | Yes | - | Data type (VARCHAR2, NUMBER, DATE, etc.) |
| COLUMN_LENGTH | NUMBER(10,0) | Yes | - | Column length |
| COLUMN_PRECISION | NUMBER(5,0) | Yes | - | Numeric precision |
| COLUMN_SCALE | NUMBER(5,0) | Yes | - | Numeric scale |
| IS_NULLABLE | CHAR(1) | No | 'Y' | Nullable flag (Y/N) |
| IS_PRIMARY_KEY | CHAR(1) | No | 'N' | Primary key flag (Y/N) |
| COLUMN_ORDER | NUMBER(3,0) | Yes | - | Column position in result set |
| VALIDATION_RULES | CLOB | Yes | - | JSON validation rules |
| DISPLAY_FORMAT | VARCHAR2(100) | Yes | - | Display format specification |
| COLUMN_DESCRIPTION | VARCHAR2(500) | Yes | - | Column description |
| IS_SENSITIVE_DATA | CHAR(1) | No | 'N' | PII/sensitive data flag |
| DATA_CLASSIFICATION | VARCHAR2(20) | No | 'INTERNAL' | Data classification level |
| CREATED_BY | VARCHAR2(50) | Yes | - | Creator |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Creation timestamp |
| UPDATED_BY | VARCHAR2(50) | Yes | - | Modifier |
| UPDATED_DATE | TIMESTAMP(6) | Yes | - | Update timestamp |

---

## Audit & Compliance Tables

### MANUAL_JOB_AUDIT

**Purpose:** SOX-compliant audit trail for all manual job configuration changes.

**Usage:** Immutable audit records supporting regulatory compliance with tamper detection.

**SOX Compliance Features:**
- Immutable records (no UPDATE/DELETE)
- Digital signatures for tamper detection
- Complete before/after value tracking
- Approval workflow integration
- Correlation ID for change tracking

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| AUDIT_ID | VARCHAR2(50) | No | - | Primary key, unique audit record ID |
| CONFIG_ID | VARCHAR2(50) | Yes | - | FK to MANUAL_JOB_CONFIG |
| OPERATION_TYPE | VARCHAR2(20) | Yes | - | Operation (CREATE, UPDATE, DELETE, APPROVE) |
| OPERATION_DESCRIPTION | VARCHAR2(500) | Yes | - | Human-readable operation description |
| OLD_VALUES | CLOB | Yes | - | JSON of values before change (may be encrypted) |
| NEW_VALUES | CLOB | Yes | - | JSON of values after change (may be encrypted) |
| CHANGED_FIELDS | VARCHAR2(1000) | Yes | - | Comma-separated list of changed field names |
| CHANGED_BY | VARCHAR2(50) | Yes | - | User who made the change |
| CHANGE_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Change timestamp |
| USER_ROLE | VARCHAR2(50) | Yes | - | Role of user making change |
| CHANGE_REASON | VARCHAR2(500) | Yes | - | Reason for change |
| BUSINESS_JUSTIFICATION | VARCHAR2(1000) | Yes | - | Business justification |
| TICKET_REFERENCE | VARCHAR2(100) | Yes | - | JIRA/ServiceNow ticket reference |
| SESSION_ID | VARCHAR2(100) | Yes | - | User session ID |
| IP_ADDRESS | VARCHAR2(45) | Yes | - | Client IP address (IPv4/IPv6) |
| USER_AGENT | VARCHAR2(500) | Yes | - | Browser/client user agent |
| CLIENT_FINGERPRINT | VARCHAR2(100) | Yes | - | Client device fingerprint |
| APPROVAL_STATUS | VARCHAR2(20) | No | 'PENDING' | Approval workflow status (PENDING, APPROVED, REJECTED) |
| APPROVED_BY | VARCHAR2(50) | Yes | - | Approver username |
| APPROVED_DATE | TIMESTAMP(6) | Yes | - | Approval timestamp |
| APPROVAL_COMMENTS | VARCHAR2(1000) | Yes | - | Approver comments |
| SOX_COMPLIANCE_FLAG | CHAR(1) | No | 'Y' | SOX compliance indicator |
| RISK_ASSESSMENT | VARCHAR2(20) | Yes | - | Risk level (LOW, MEDIUM, HIGH, CRITICAL) |
| REGULATORY_IMPACT | VARCHAR2(500) | Yes | - | Regulatory impact description |
| CHECKSUM | VARCHAR2(64) | Yes | - | SHA-256 checksum for tamper detection |
| DIGITAL_SIGNATURE | VARCHAR2(512) | Yes | - | Digital signature of audit record |
| CORRELATION_ID | VARCHAR2(100) | Yes | - | Correlation ID for related changes |
| PARENT_AUDIT_ID | VARCHAR2(50) | Yes | - | FK to parent audit record for hierarchical changes |
| ENVIRONMENT | VARCHAR2(20) | Yes | - | Environment where change occurred |
| APPLICATION_VERSION | VARCHAR2(20) | Yes | - | Application version at time of change |
| RETENTION_DATE | DATE | Yes | - | Date when record can be archived |
| ARCHIVED_FLAG | CHAR(1) | No | 'N' | Archive status flag |

**Table Comment:** *SOX-compliant audit trail for manual job configuration changes - immutable records supporting regulatory compliance*

**Column Comments:**
- `OLD_VALUES`: JSON representation of configuration values before change - may be encrypted for sensitive data
- `NEW_VALUES`: JSON representation of configuration values after change - may be encrypted for sensitive data
- `CHECKSUM`: SHA-256 checksum of audit record for tamper detection

---

### CONFIGURATION_AUDIT

**Purpose:** Simplified audit trail for batch configuration changes.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| AUDIT_ID | NUMBER | No | Identity | Primary key (auto-generated) |
| CONFIG_ID | VARCHAR2(100) | Yes | - | FK to BATCH_CONFIGURATIONS |
| ACTION | VARCHAR2(20) | Yes | - | Action type (SAVE, UPDATE, DELETE) |
| OLD_VALUE | CLOB | Yes | - | Previous configuration value |
| NEW_VALUE | CLOB | Yes | - | New configuration value |
| CHANGED_BY | VARCHAR2(50) | Yes | - | User who made change |
| CHANGE_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Change timestamp |
| CHANGE_REASON | VARCHAR2(500) | Yes | - | Reason for change |

---

### BATCH_JOB_AUDIT

**Purpose:** Audit trail for batch job executions (simplified).

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| AUDIT_ID | NUMBER | No | Identity | Primary key |
| JOB_NAME | VARCHAR2(100) | Yes | - | Job name |
| STATUS | VARCHAR2(20) | Yes | - | Execution status |
| READ_COUNT | NUMBER | No | 0 | Records read |
| WRITE_COUNT | NUMBER | No | 0 | Records written |
| SKIP_COUNT | NUMBER | No | 0 | Records skipped |
| START_TIME | TIMESTAMP(6) | Yes | - | Start timestamp |
| END_TIME | TIMESTAMP(6) | Yes | - | End timestamp |
| CREATED_DATE | DATE | No | SYSDATE | Audit record creation date |

---

## Spring Batch Framework Tables

The following tables are managed by Spring Batch framework for job execution metadata.

### BATCH_JOB_INSTANCE

**Purpose:** Represents a unique batch job instance identified by job parameters.

| Column | Data Type | Description |
|--------|-----------|-------------|
| JOB_INSTANCE_ID | NUMBER(19,0) | Primary key (identity) |
| VERSION | NUMBER(19,0) | Optimistic locking version |
| JOB_NAME | VARCHAR2(100) | Spring Batch job name |
| JOB_KEY | VARCHAR2(32) | MD5 hash of job parameters |

---

### BATCH_JOB_EXECUTION

**Purpose:** Tracks individual job execution attempts.

| Column | Data Type | Description |
|--------|-----------|-------------|
| JOB_EXECUTION_ID | NUMBER(19,0) | Primary key (identity) |
| VERSION | NUMBER(19,0) | Optimistic locking version |
| JOB_INSTANCE_ID | NUMBER(19,0) | FK to BATCH_JOB_INSTANCE |
| CREATE_TIME | TIMESTAMP(6) | Job creation timestamp |
| START_TIME | TIMESTAMP(6) | Job start timestamp |
| END_TIME | TIMESTAMP(6) | Job end timestamp |
| STATUS | VARCHAR2(10) | Job status (STARTED, COMPLETED, FAILED, etc.) |
| EXIT_CODE | VARCHAR2(2500) | Exit code |
| EXIT_MESSAGE | VARCHAR2(2500) | Exit message |
| LAST_UPDATED | TIMESTAMP(6) | Last update timestamp |

---

### BATCH_JOB_EXECUTION_PARAMS

**Purpose:** Stores job execution parameters.

| Column | Data Type | Description |
|--------|-----------|-------------|
| JOB_EXECUTION_ID | NUMBER(19,0) | FK to BATCH_JOB_EXECUTION |
| PARAMETER_NAME | VARCHAR2(100) | Parameter name |
| PARAMETER_TYPE | VARCHAR2(100) | Parameter type (STRING, DATE, LONG, DOUBLE) |
| PARAMETER_VALUE | VARCHAR2(2500) | Parameter value |
| IDENTIFYING | CHAR(1) | Whether parameter identifies job instance |

---

### BATCH_JOB_EXECUTION_CONTEXT

**Purpose:** Stores job execution context for restart capability.

| Column | Data Type | Description |
|--------|-----------|-------------|
| JOB_EXECUTION_ID | NUMBER(19,0) | FK to BATCH_JOB_EXECUTION |
| SHORT_CONTEXT | VARCHAR2(2500) | Short context (limited size) |
| SERIALIZED_CONTEXT | CLOB | Full serialized execution context |

---

### BATCH_STEP_EXECUTION

**Purpose:** Tracks individual step executions within a job.

| Column | Data Type | Description |
|--------|-----------|-------------|
| STEP_EXECUTION_ID | NUMBER(19,0) | Primary key (identity) |
| VERSION | NUMBER(19,0) | Optimistic locking version |
| STEP_NAME | VARCHAR2(100) | Step name |
| JOB_EXECUTION_ID | NUMBER(19,0) | FK to BATCH_JOB_EXECUTION |
| CREATE_TIME | TIMESTAMP(6) | Step creation timestamp |
| START_TIME | TIMESTAMP(6) | Step start timestamp |
| END_TIME | TIMESTAMP(6) | Step end timestamp |
| STATUS | VARCHAR2(10) | Step status |
| COMMIT_COUNT | NUMBER(19,0) | Number of commits |
| READ_COUNT | NUMBER(19,0) | Items read |
| FILTER_COUNT | NUMBER(19,0) | Items filtered |
| WRITE_COUNT | NUMBER(19,0) | Items written |
| READ_SKIP_COUNT | NUMBER(19,0) | Read skips |
| WRITE_SKIP_COUNT | NUMBER(19,0) | Write skips |
| PROCESS_SKIP_COUNT | NUMBER(19,0) | Process skips |
| ROLLBACK_COUNT | NUMBER(19,0) | Rollback count |
| EXIT_CODE | VARCHAR2(2500) | Exit code |
| EXIT_MESSAGE | VARCHAR2(2500) | Exit message |
| LAST_UPDATED | TIMESTAMP(6) | Last update timestamp |

---

### BATCH_STEP_EXECUTION_CONTEXT

**Purpose:** Stores step execution context for restart capability.

| Column | Data Type | Description |
|--------|-----------|-------------|
| STEP_EXECUTION_ID | NUMBER(19,0) | FK to BATCH_STEP_EXECUTION |
| SHORT_CONTEXT | VARCHAR2(2500) | Short context |
| SERIALIZED_CONTEXT | CLOB | Full serialized step context |

---

## Reference Data Tables

### SOURCE_SYSTEMS

**Purpose:** Catalog of source systems integrated with the platform.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| ID | VARCHAR2(50) | No | - | Primary key, system identifier (MTG, ENCORE, SHAW) |
| NAME | VARCHAR2(100) | Yes | - | System display name |
| TYPE | VARCHAR2(20) | Yes | - | System type (DATABASE, FILE, API, QUEUE) |
| DESCRIPTION | VARCHAR2(500) | Yes | - | System description |
| CONNECTION_STRING | VARCHAR2(1000) | Yes | - | Connection details (encrypted) |
| ENABLED | VARCHAR2(1) | No | 'Y' | Active status flag |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Creation timestamp |
| JOB_COUNT | NUMBER | No | 0 | Number of associated jobs |

**Example Systems:**
- **MTG** - Mortgage system
- **ENCORE** - Core banking system
- **SHAW** - Legacy mainframe system

---

### ENCORE_TEST_DATA

**Purpose:** Test data for ENCORE source system batch processing.

**Usage:** Simulated ENCORE data for development and testing purposes.

| Column | Data Type | Nullable | Default | Description |
|--------|-----------|----------|---------|-------------|
| ID | NUMBER(19,0) | No | Identity | Primary key |
| ACCT_NUM | VARCHAR2(18) | Yes | - | Account number |
| BATCH_DATE | DATE | Yes | - | Batch processing date |
| CCI | VARCHAR2(1) | Yes | - | Customer classification indicator |
| CONTACT_ID | VARCHAR2(18) | Yes | - | Customer contact ID |
| CREATED_DATE | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Record creation timestamp |

**Sample Data:** Contains 7+ test records for transaction codes 200, 300, 900 across multiple batch dates.

---

### DATABASECHANGELOG / DATABASECHANGELOGLOCK

**Purpose:** Liquibase database change management tables.

**DATABASECHANGELOG Columns:**
- ID, AUTHOR, FILENAME - Change set identification
- DATEEXECUTED, ORDEREXECUTED - Execution tracking
- EXECTYPE - Execution type (EXECUTED, RERAN, SKIPPED)
- MD5SUM - Checksum for change validation
- DESCRIPTION, COMMENTS - Change documentation
- TAG, LIQUIBASE, CONTEXTS, LABELS - Metadata
- DEPLOYMENT_ID - Deployment tracking

**DATABASECHANGELOGLOCK Columns:**
- ID - Lock ID
- LOCKED - Lock status flag
- LOCKGRANTED - Lock timestamp
- LOCKEDBY - User holding lock

---

## Data Types & Conventions

### Standard Data Types

| Type | Oracle Type | Usage | Example |
|------|-------------|-------|---------|
| ID/Primary Key | VARCHAR2(50-100) | Composite identifiers | `MTG_atoctran-900_900` |
| Auto-ID | NUMBER(19,0) Identity | Sequential IDs | 1, 2, 3... |
| Name/Code | VARCHAR2(50-100) | Names, codes | `Daily Balance ETL` |
| Description | VARCHAR2(500) | Short descriptions | Business purpose text |
| Large Text | CLOB | JSON, SQL, logs | Configuration JSON, queries |
| Timestamp | TIMESTAMP(6) | All timestamps | `2025-10-03 20:00:00.123456` |
| Date | DATE | Date-only values | `2025-10-03` |
| Flag | CHAR(1) or VARCHAR2(1) | Boolean flags | `Y`, `N` |
| Decimal | NUMBER(p,s) | Numeric with precision | `NUMBER(5,2)` for percentages |
| Large Integer | NUMBER(19,0) | Counts, IDs | `99999999999999` |

### Naming Conventions

**Tables:**
- Uppercase with underscores: `BATCH_CONFIGURATIONS`
- Plural names for collections: `SOURCE_SYSTEMS`
- Descriptive prefixes: `BATCH_`, `MANUAL_JOB_`, `TEMPLATE_`

**Columns:**
- Uppercase with underscores: `CREATED_DATE`
- Suffix patterns:
  - `_ID` for identifiers
  - `_DATE` for dates
  - `_FLAG` or plain for boolean
  - `_BY` for users
  - `_COUNT` for counters

**Keys:**
- Primary Keys: Usually `ID` or `{TABLE}_ID`
- Foreign Keys: `{REFERENCED_TABLE}_ID`
- Composite Keys: Multiple columns forming uniqueness

### Common Patterns

**Audit Columns (Standard Set):**
```sql
CREATED_BY      VARCHAR2(50)
CREATED_DATE    TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP
MODIFIED_BY     VARCHAR2(50)
MODIFIED_DATE   TIMESTAMP(6)
VERSION         NUMBER DEFAULT 1
```

**Status/Flag Columns:**
```sql
ENABLED         VARCHAR2(1) DEFAULT 'Y'
STATUS          VARCHAR2(20) DEFAULT 'ACTIVE'
IS_ACTIVE       VARCHAR2(1) DEFAULT 'Y'
```

**LOB Storage:**
- All CLOB columns use SECUREFILE storage
- Storage settings: `ENABLE STORAGE IN ROW CHUNK 8192`
- Logging: `NOCACHE LOGGING NOCOMPRESS KEEP_DUPLICATES`

---

## Indexes & Constraints

### Primary Keys

| Table | Primary Key | Type |
|-------|-------------|------|
| BATCH_CONFIGURATIONS | ID | Unique Index SYS_C007851 |
| BATCH_EXECUTION_RESULTS | ID | Unique Index PK_BATCH_EXECUTION_RESULTS |
| MANUAL_JOB_CONFIG | CONFIG_ID | Primary Key Constraint |
| MANUAL_JOB_EXECUTION | EXECUTION_ID | Primary Key Constraint |
| MASTER_QUERY_CONFIG | ID | Primary Key Constraint |
| SOURCE_SYSTEMS | ID | Primary Key Constraint |

### Unique Constraints

| Table | Columns | Index Name |
|-------|---------|------------|
| BATCH_CONFIGURATIONS | (SOURCE_SYSTEM, JOB_NAME, TRANSACTION_TYPE) | SYS_C007852 |

### Non-Unique Indexes

| Table | Columns | Index Name | Purpose |
|-------|---------|------------|---------|
| BATCH_CONFIGURATIONS | (SOURCE_SYSTEM, JOB_NAME) | IDX_BATCH_CONFIG_SYSTEM_JOB | Job lookup optimization |

### Foreign Key Relationships

**MANUAL_JOB_CONFIG:**
- `MASTER_QUERY_ID` → `MASTER_QUERY_CONFIG.ID`

**MANUAL_JOB_EXECUTION:**
- `CONFIG_ID` → `MANUAL_JOB_CONFIG.CONFIG_ID`

**MANUAL_JOB_AUDIT:**
- `CONFIG_ID` → `MANUAL_JOB_CONFIG.CONFIG_ID`
- `PARENT_AUDIT_ID` → `MANUAL_JOB_AUDIT.AUDIT_ID`

**JOB_DEFINITIONS:**
- `SOURCE_SYSTEM_ID` → `SOURCE_SYSTEMS.ID`

**Spring Batch Framework:**
- `BATCH_JOB_EXECUTION.JOB_INSTANCE_ID` → `BATCH_JOB_INSTANCE.JOB_INSTANCE_ID`
- `BATCH_JOB_EXECUTION_PARAMS.JOB_EXECUTION_ID` → `BATCH_JOB_EXECUTION.JOB_EXECUTION_ID`
- `BATCH_JOB_EXECUTION_CONTEXT.JOB_EXECUTION_ID` → `BATCH_JOB_EXECUTION.JOB_EXECUTION_ID`
- `BATCH_STEP_EXECUTION.JOB_EXECUTION_ID` → `BATCH_JOB_EXECUTION.JOB_EXECUTION_ID`
- `BATCH_STEP_EXECUTION_CONTEXT.STEP_EXECUTION_ID` → `BATCH_STEP_EXECUTION.STEP_EXECUTION_ID`

---

## Security & Permissions

### Application User: FABRIC_APP

The following grants are configured for the application user:

**Permissions Granted:**
- ALTER, DELETE, INDEX, INSERT, SELECT, UPDATE
- REFERENCES, READ, FLASHBACK, DEBUG
- ON COMMIT REFRESH, QUERY REWRITE

**Tables with FABRIC_APP Grants:**
- BATCH_JOB_EXECUTION
- BATCH_JOB_EXECUTION_CONTEXT
- BATCH_JOB_EXECUTION_PARAMS
- BATCH_JOB_INSTANCE
- BATCH_STEP_EXECUTION
- BATCH_STEP_EXECUTION_CONTEXT
- MANUAL_JOB_AUDIT
- MANUAL_JOB_CONFIG
- MANUAL_JOB_EXECUTION

### Application Role: FABRIC_APP_ROLE

**Limited Permissions:**
- INSERT, SELECT (read-write)
- Granted on:
  - MANUAL_JOB_AUDIT
  - MANUAL_JOB_CONFIG
  - MANUAL_JOB_EXECUTION

---

## Appendix A: Data Flow

### Configuration Creation Flow

```
1. User creates configuration via UI
2. POST /api/config/mappings/save
3. ConfigurationController.saveConfiguration()
4. ConfigurationServiceImpl.saveToDatabase()
5. INSERT INTO BATCH_CONFIGURATIONS
6. INSERT INTO CONFIGURATION_AUDIT (audit trail)
7. Generate YAML file from JSON configuration
```

### Batch Execution Flow

```
1. User triggers batch execution via UI
2. POST /api/v2/manual-job-execution/execute/{configId}
3. Retrieve configuration from BATCH_CONFIGURATIONS
4. INSERT INTO BATCH_EXECUTION_RESULTS (status='PENDING')
5. Spring Batch creates BATCH_JOB_INSTANCE
6. Spring Batch creates BATCH_JOB_EXECUTION
7. YamlMappingService reads configuration
8. Process records using field mappings
9. UPDATE BATCH_EXECUTION_RESULTS (status='COMPLETED')
10. INSERT INTO MANUAL_JOB_EXECUTION (complete metrics)
```

---

## Appendix B: Query Examples

### Find All Configurations for a Source System

```sql
SELECT *
FROM BATCH_CONFIGURATIONS
WHERE SOURCE_SYSTEM = 'MTG'
  AND ENABLED = 'Y'
ORDER BY JOB_NAME, TRANSACTION_TYPE;
```

### Get Execution History with Success Rate

```sql
SELECT
    CONFIG_ID,
    JOB_NAME,
    COUNT(*) as TOTAL_EXECUTIONS,
    SUM(CASE WHEN STATUS = 'COMPLETED' THEN 1 ELSE 0 END) as SUCCESSFUL,
    ROUND(SUM(CASE WHEN STATUS = 'COMPLETED' THEN 1 ELSE 0 END) / COUNT(*) * 100, 2) as SUCCESS_RATE_PCT,
    AVG(DURATION_SECONDS) as AVG_DURATION_SEC,
    SUM(RECORDS_PROCESSED) as TOTAL_RECORDS
FROM MANUAL_JOB_EXECUTION
WHERE CREATED_DATE >= TRUNC(SYSDATE) - 30
GROUP BY CONFIG_ID, JOB_NAME
ORDER BY SUCCESS_RATE_PCT DESC;
```

### Audit Trail for Configuration Changes

```sql
SELECT
    a.AUDIT_ID,
    a.CONFIG_ID,
    a.OPERATION_TYPE,
    a.CHANGED_BY,
    a.CHANGE_DATE,
    a.CHANGE_REASON,
    a.APPROVAL_STATUS,
    a.APPROVED_BY
FROM MANUAL_JOB_AUDIT a
WHERE a.CONFIG_ID = 'cfg_mtg_12345'
ORDER BY a.CHANGE_DATE DESC;
```

### Active Master Queries by Source System

```sql
SELECT
    m.ID,
    m.SOURCE_SYSTEM,
    m.JOB_NAME,
    m.VERSION,
    m.CREATED_DATE,
    COUNT(tm.MAPPING_ID) as MAPPING_COUNT
FROM MASTER_QUERY_CONFIG m
LEFT JOIN TEMPLATE_MASTER_QUERY_MAPPING tm ON m.ID = TO_CHAR(tm.MASTER_QUERY_ID)
WHERE m.IS_ACTIVE = 'Y'
GROUP BY m.ID, m.SOURCE_SYSTEM, m.JOB_NAME, m.VERSION, m.CREATED_DATE
ORDER BY m.SOURCE_SYSTEM, m.JOB_NAME;
```

---

## Appendix C: Schema Regeneration

This data dictionary was generated from the live Oracle database using the following process:

### Regenerate Database Snapshot

To update this document with the latest database schema metadata, run:

```bash
# Option 1: Use the standalone schema extractor
cd /tmp
javac -cp .:~/.m2/repository/com/oracle/database/jdbc/ojdbc8/21.9.0.0/ojdbc8-21.9.0.0.jar SchemaExtractorStandalone.java
java -cp .:~/.m2/repository/com/oracle/database/jdbc/ojdbc8/21.9.0.0/ojdbc8-21.9.0.0.jar SchemaExtractorStandalone

# Output will be in /tmp/schema_snapshot.txt
cat /tmp/schema_snapshot.txt
```

The schema extractor queries the following Oracle system tables:
- `USER_TABLES` - Table names and row counts
- `USER_TAB_COLUMNS` - Column definitions and data types
- `USER_CONSTRAINTS` - Primary keys, foreign keys, and check constraints
- `USER_CONS_COLUMNS` - Constraint column mappings
- `USER_INDEXES` - Index definitions
- `USER_IND_COLUMNS` - Index column mappings

### Schema Extractor Location

**Java Utility:** `/tmp/SchemaExtractorStandalone.java`
**Database Connection:** Uses application-local.properties configuration
- URL: `jdbc:oracle:thin:@localhost:1521/ORCLPDB1`
- Schema: `cm3int`

### Last Database Snapshot

- **Generated:** October 5, 2025 17:41:17 EDT
- **Tables Analyzed:** 25
- **Connection String:** jdbc:oracle:thin:@localhost:1521/ORCLPDB1
- **Total Rows:** 441 across all tables
  - FIELD_TEMPLATES: 366 rows (largest table)
  - TEMPLATE_SOURCE_MAPPINGS: 45 rows
  - DATABASECHANGELOG: 25 rows
  - BATCH_CONFIGURATIONS: 7 rows
  - ENCORE_TEST_DATA: 7 rows
  - FILE_TYPE_TEMPLATES: 8 rows
  - MANUAL_JOB_CONFIG: 4 rows
  - MASTER_QUERY_CONFIG: 4 rows
  - SOURCE_SYSTEMS: 4 rows
  - DATABASECHANGELOGLOCK: 1 row

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2025-08-14 | System | Initial data dictionary creation |
| 1.1.0 | 2025-09-02 | System | Added Phase 3 batch execution tables |
| 1.2.0 | 2025-10-03 | Claude Code | Complete data dictionary with all 22 tables, relationships, and usage examples |
| 1.3.0 | 2025-10-05 | Claude Code | Updated with live database snapshot from application-local.properties connection; added current row counts and schema regeneration instructions |

---

**© 2025 Truist Financial Corporation. All rights reserved.**

*This document contains confidential and proprietary information. Distribution is restricted to authorized personnel only.*
