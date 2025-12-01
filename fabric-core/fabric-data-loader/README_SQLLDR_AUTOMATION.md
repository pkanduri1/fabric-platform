# SQL*Loader Automation - Complete Guide

## Overview

This comprehensive SQL*Loader automation system provides database-driven, enterprise-grade data loading capabilities for the FABRIC platform. The system dynamically generates control files from database configurations, executes SQL*Loader with full lifecycle management, and tracks all operations with SOX-compliant audit trails.

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Installation](#installation)
5. [Quick Start](#quick-start)
6. [Usage Guide](#usage-guide)
7. [Configuration](#configuration)
8. [Automation Workflows](#automation-workflows)
9. [Monitoring and Reporting](#monitoring-and-reporting)
10. [Troubleshooting](#troubleshooting)
11. [Best Practices](#best-practices)

---

## Features

### Core Capabilities
- **Database-Driven Configuration**: All SQL*Loader configurations stored in database tables
- **Dynamic Control File Generation**: Auto-generates control files from database metadata
- **Complete Lifecycle Management**: Handles execution, monitoring, error recovery, and archiving
- **Performance Optimization**: Supports direct path loading, parallel execution, and tuning
- **Security & Compliance**: SOX-compliant audit trails, encryption support, PII handling
- **Error Handling**: Comprehensive error detection, retry logic, and notification
- **Real-Time Monitoring**: Execution tracking with performance metrics
- **Automated Reporting**: Built-in views for configuration, security, and performance analysis

### Enterprise Features
- SOX-compliant audit trail for all configuration changes
- PII field identification and encryption support
- Regulatory compliance tracking (SOX, PCI-DSS, GDPR)
- Performance baseline tracking and alerting
- Security event logging and monitoring
- Automated cleanup and archiving
- Email notifications for execution results
- Comprehensive execution statistics

---

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                  FABRIC Data Loader System                   │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐      ┌──────────────────┐            │
│  │  Configuration   │      │  Control File    │            │
│  │  Management      │─────>│  Generator       │            │
│  │  (Database)      │      │  (Shell Script)  │            │
│  └──────────────────┘      └──────────────────┘            │
│           │                          │                       │
│           │                          ↓                       │
│           │              ┌──────────────────┐              │
│           │              │   SQL*Loader     │              │
│           │              │   Execution      │              │
│           │              └──────────────────┘              │
│           │                          │                       │
│           ↓                          ↓                       │
│  ┌──────────────────┐      ┌──────────────────┐            │
│  │  Execution       │<─────│  Performance     │            │
│  │  Tracking        │      │  Metrics         │            │
│  │  & Audit         │      │  Collection      │            │
│  └──────────────────┘      └──────────────────┘            │
│           │                                                   │
│           ↓                                                   │
│  ┌──────────────────────────────────────────┐              │
│  │         Reporting & Monitoring            │              │
│  │  - Config Summary  - Security Audit       │              │
│  │  - Performance     - Compliance Reports   │              │
│  └──────────────────────────────────────────┘              │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Database Schema

**Core Tables:**
- `sql_loader_configs` - SQL*Loader configuration metadata
- `sql_loader_field_configs` - Field-level configuration and transformations
- `sql_loader_executions` - Execution tracking and results
- `sql_loader_security_audit` - Security and compliance audit trail
- `sql_loader_performance_baselines` - Performance monitoring

**Views:**
- `v_sql_loader_config_summary` - Configuration and execution summary
- `v_sql_loader_security_summary` - Security events and compliance
- `v_sql_loader_performance` - Performance metrics and trends

---

## Prerequisites

### Required Software
- **Oracle Database**: 12c or higher
- **Oracle SQL*Loader**: Installed with Oracle Client
- **Shell**: Bash 4.0+ (macOS/Linux)
- **Utilities**: `sqlplus`, `mail` (for notifications)

### Database Setup
```bash
# Ensure Liquibase migration has been applied
cd fabric-platform-new/fabric-core/fabric-api
mvn liquibase:update
```

### Environment Variables
```bash
export ORACLE_HOME=/path/to/oracle/client
export PATH=$ORACLE_HOME/bin:$PATH
export DB_HOST=localhost
export DB_PORT=1521
export DB_SERVICE=ORCLPDB1
export DB_USER=cm3int
export DB_PASS=MySecurePass123
```

---

## Installation

### 1. Clone/Navigate to Project
```bash
cd fabric-platform-new/fabric-core/fabric-data-loader
```

### 2. Make Scripts Executable
```bash
chmod +x scripts/sqlldr_automation.sh
```

### 3. Create Required Directories
```bash
mkdir -p logs config control_files
mkdir -p /tmp/data/{input,output,archive,bad,discard}
```

### 4. Verify Database Schema
```bash
sqlplus ${DB_USER}/${DB_PASS}@${DB_HOST}:${DB_PORT}/${DB_SERVICE} <<EOF
SELECT table_name FROM user_tables
WHERE table_name LIKE 'SQL_LOADER%'
ORDER BY table_name;
EXIT;
EOF
```

Expected output:
```
SQL_LOADER_CONFIGS
SQL_LOADER_EXECUTIONS
SQL_LOADER_FIELD_CONFIGS
SQL_LOADER_PERFORMANCE_BASELINES
SQL_LOADER_SECURITY_AUDIT
```

---

## Quick Start

### Example 1: Load Customer Data

**Step 1: Configure SQL*Loader Job**
```sql
-- Insert configuration
INSERT INTO CM3INT.sql_loader_configs (
    config_id, job_name, source_system, target_table,
    load_method, direct_path, max_errors, field_delimiter,
    created_by, enabled
) VALUES (
    'HR-CUSTOMER-LOAD-001', 'customer_load', 'HR', 'CM3INT.CUSTOMERS',
    'APPEND', 'Y', 1000, '|',
    'admin', 'Y'
);

-- Configure fields
INSERT INTO CM3INT.sql_loader_field_configs (
    config_id, field_name, column_name, data_type,
    nullable, field_order, created_by, enabled
) VALUES
('HR-CUSTOMER-LOAD-001', 'CUSTOMER_ID', 'CUSTOMER_ID', 'NUMBER', 'N', 1, 'admin', 'Y'),
('HR-CUSTOMER-LOAD-001', 'FIRST_NAME', 'FIRST_NAME', 'VARCHAR2(50)', 'N', 2, 'admin', 'Y'),
('HR-CUSTOMER-LOAD-001', 'LAST_NAME', 'LAST_NAME', 'VARCHAR2(50)', 'N', 3, 'admin', 'Y'),
('HR-CUSTOMER-LOAD-001', 'EMAIL', 'EMAIL', 'VARCHAR2(100)', 'Y', 4, 'admin', 'Y'),
('HR-CUSTOMER-LOAD-001', 'CREATED_DATE', 'CREATED_DATE', 'DATE', 'N', 5, 'admin', 'Y');

COMMIT;
```

**Step 2: Prepare Data File**
```bash
# Create sample data file
cat > /tmp/data/input/customers.dat <<EOF
1|John|Doe|john.doe@example.com|2025-01-15
2|Jane|Smith|jane.smith@example.com|2025-01-16
3|Bob|Johnson|bob.johnson@example.com|2025-01-17
EOF
```

**Step 3: Run Automation**
```bash
cd fabric-core/fabric-data-loader
./scripts/sqlldr_automation.sh HR-CUSTOMER-LOAD-001 /tmp/data/input/customers.dat
```

**Step 4: Verify Results**
```sql
-- Check execution results
SELECT execution_id, execution_status, total_records,
       successful_records, rejected_records, duration_ms
FROM CM3INT.sql_loader_executions
WHERE config_id = 'HR-CUSTOMER-LOAD-001'
ORDER BY started_date DESC;

-- Verify loaded data
SELECT * FROM CM3INT.CUSTOMERS;
```

---

## Usage Guide

### Basic Command Structure
```bash
./scripts/sqlldr_automation.sh <CONFIG_ID> <DATA_FILE> [OPTIONS]
```

### Command-Line Arguments

| Argument | Required | Description | Example |
|----------|----------|-------------|---------|
| CONFIG_ID | Yes | Configuration identifier from database | HR-CUSTOMER-LOAD-001 |
| DATA_FILE | Yes | Full path to input data file | /tmp/data/input/customers.dat |

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DB_HOST | localhost | Oracle database hostname |
| DB_PORT | 1521 | Oracle database port |
| DB_SERVICE | ORCLPDB1 | Oracle service name |
| DB_USER | cm3int | Database username |
| DB_PASS | MySecurePass123 | Database password |
| DEBUG | false | Enable debug logging |
| ORACLE_HOME | (required) | Oracle client installation directory |

### Debug Mode
```bash
DEBUG=true ./scripts/sqlldr_automation.sh HR-CUSTOMER-LOAD-001 /tmp/data/input/customers.dat
```

---

## Configuration

### 1. SQL*Loader Configuration (`sql_loader_configs`)

**Required Fields:**
- `config_id` - Unique identifier (e.g., HR-CUSTOMER-LOAD-001)
- `job_name` - Job name for reference
- `source_system` - Source system code (e.g., HR, FINANCE)
- `target_table` - Fully qualified target table name
- `created_by` - User who created the configuration

**Key Parameters:**
- `load_method` - INSERT, APPEND, REPLACE, TRUNCATE
- `direct_path` - Y/N for direct path loading
- `parallel_degree` - Number of parallel streams
- `max_errors` - Maximum allowable errors before aborting
- `field_delimiter` - Field separator character
- `skip_rows` - Number of header rows to skip

**Example:**
```sql
INSERT INTO CM3INT.sql_loader_configs (
    config_id, job_name, source_system, target_table,
    load_method, direct_path, parallel_degree, max_errors,
    field_delimiter, skip_rows, bind_size, read_size,
    character_set, encryption_required, pii_fields,
    data_classification, regulatory_compliance,
    notification_emails, created_by, enabled
) VALUES (
    'FIN-TRANSACTION-LOAD-001',
    'daily_transactions',
    'FINANCE',
    'CM3INT.TRANSACTIONS',
    'APPEND',  -- Load method
    'Y',       -- Direct path enabled
    4,         -- 4 parallel streams
    1000,      -- Max 1000 errors
    '|',       -- Pipe delimiter
    1,         -- Skip 1 header row
    512000,    -- Bind size
    2097152,   -- Read size (2MB)
    'UTF8',    -- Character set
    'Y',       -- Encryption required
    'ACCOUNT_NUMBER,SSN',  -- PII fields
    'CONFIDENTIAL',  -- Data classification
    'SOX,PCI-DSS',   -- Compliance requirements
    'data-team@company.com',  -- Notifications
    'admin',
    'Y'
);
```

### 2. Field Configuration (`sql_loader_field_configs`)

**Required Fields:**
- `config_id` - Links to parent configuration
- `field_name` - Source field name
- `column_name` - Target column name
- `data_type` - Oracle data type
- `field_order` - Position in file (1-based)

**Advanced Options:**
- `format_mask` - Date/number format (e.g., 'YYYY-MM-DD HH24:MI:SS')
- `sql_expression` - Custom SQL transformation
- `null_if_condition` - When to treat as NULL
- `trim_field` - Trim whitespace (Y/N)
- `case_sensitive` - PRESERVE, UPPER, LOWER
- `encrypted` - Encrypt field (Y/N)

**Example with Transformations:**
```sql
-- Standard field
INSERT INTO CM3INT.sql_loader_field_configs (
    config_id, field_name, column_name, data_type,
    nullable, field_order, created_by, enabled
) VALUES (
    'FIN-TRANSACTION-LOAD-001', 'TRANSACTION_ID', 'TRANSACTION_ID',
    'NUMBER', 'N', 1, 'admin', 'Y'
);

-- Date field with format
INSERT INTO CM3INT.sql_loader_field_configs (
    config_id, field_name, column_name, data_type,
    format_mask, nullable, field_order, created_by, enabled
) VALUES (
    'FIN-TRANSACTION-LOAD-001', 'TRANSACTION_DATE', 'TRANSACTION_DATE',
    'DATE', 'YYYY-MM-DD HH24:MI:SS', 'N', 2, 'admin', 'Y'
);

-- Field with SQL transformation
INSERT INTO CM3INT.sql_loader_field_configs (
    config_id, field_name, column_name, data_type,
    sql_expression, nullable, field_order, created_by, enabled
) VALUES (
    'FIN-TRANSACTION-LOAD-001', 'EMAIL', 'EMAIL_NORMALIZED',
    'VARCHAR2(100)',
    'LOWER(LTRIM(RTRIM(:EMAIL)))',  -- Normalize email
    'Y', 3, 'admin', 'Y'
);

-- Encrypted PII field
INSERT INTO CM3INT.sql_loader_field_configs (
    config_id, field_name, column_name, data_type,
    encrypted, encryption_function, audit_field,
    nullable, field_order, created_by, enabled
) VALUES (
    'FIN-TRANSACTION-LOAD-001', 'SSN', 'SSN_ENCRYPTED',
    'VARCHAR2(11)',
    'Y', 'ENCRYPT_SSN', 'Y',
    'Y', 4, 'admin', 'Y'
);
```

---

## Automation Workflows

### Workflow 1: Daily Batch Data Load

**Scenario**: Load daily transaction files automatically via cron

**Cron Setup:**
```bash
# Run daily at 2:00 AM
0 2 * * * /path/to/fabric-data-loader/scripts/sqlldr_automation.sh FIN-TRANSACTION-LOAD-001 /data/daily/transactions_$(date +\%Y\%m\%d).dat >> /var/log/sqlldr_daily.log 2>&1
```

### Workflow 2: File Watcher Integration

**Script: `scripts/file_watcher_loader.sh`**
```bash
#!/bin/bash
# Watch directory for new files and load them automatically

WATCH_DIR="/data/incoming"
CONFIG_MAP="/path/to/config_map.txt"  # File pattern -> Config ID mapping

inotifywait -m -e create -e moved_to "${WATCH_DIR}" |
while read -r directory action filename; do
    echo "Detected new file: ${filename}"

    # Lookup configuration based on filename pattern
    config_id=$(grep "^${filename%%_*}" "${CONFIG_MAP}" | cut -d'|' -f2)

    if [[ -n "${config_id}" ]]; then
        echo "Loading ${filename} using config ${config_id}"
        /path/to/sqlldr_automation.sh "${config_id}" "${directory}${filename}"
    else
        echo "No configuration found for ${filename}"
    fi
done
```

### Workflow 3: Integration with Spring Batch

**Java Service:**
```java
@Service
public class SqlLoaderService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void executeSqlLoader(String configId, String dataFile) {
        String script = "/path/to/sqlldr_automation.sh";

        ProcessBuilder pb = new ProcessBuilder(script, configId, dataFile);
        pb.environment().put("DB_HOST", dbHost);
        pb.environment().put("DB_USER", dbUser);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            log.info("SQL*Loader completed successfully");
        } else {
            log.error("SQL*Loader failed with exit code: " + exitCode);
        }
    }
}
```

---

## Monitoring and Reporting

### Real-Time Execution Monitoring

**Check Current Executions:**
```sql
SELECT execution_id, config_id, file_name, execution_status,
       total_records, successful_records, rejected_records,
       ROUND(duration_ms/1000, 2) as duration_sec,
       started_date
FROM CM3INT.sql_loader_executions
WHERE execution_status IN ('SUBMITTED', 'GENERATING_CONTROL', 'EXECUTING')
ORDER BY started_date DESC;
```

**Execution Summary:**
```sql
SELECT * FROM CM3INT.v_sql_loader_config_summary
WHERE enabled = 'Y'
ORDER BY last_execution_date DESC NULLS LAST;
```

### Performance Analysis

**Performance Metrics (Last 30 Days):**
```sql
SELECT * FROM CM3INT.v_sql_loader_performance
WHERE executions_last_30_days > 0
ORDER BY avg_execution_time_ms DESC;
```

**Identify Slow Loads:**
```sql
SELECT config_id, job_name, source_system,
       avg_execution_time_ms,
       p95_execution_time_ms,
       avg_throughput_records_per_sec
FROM CM3INT.v_sql_loader_performance
WHERE p95_execution_time_ms > 60000  -- Over 1 minute
ORDER BY p95_execution_time_ms DESC;
```

### Security and Compliance

**Security Events (Last 24 Hours):**
```sql
SELECT audit_timestamp, security_event_type, severity,
       event_description, user_id, compliance_status
FROM CM3INT.sql_loader_security_audit
WHERE audit_timestamp >= SYSTIMESTAMP - INTERVAL '24' HOUR
ORDER BY severity DESC, audit_timestamp DESC;
```

**PII Access Audit:**
```sql
SELECT sle.execution_id, sle.config_id, sle.file_name,
       slc.pii_fields, sle.created_by, sle.started_date
FROM CM3INT.sql_loader_executions sle
JOIN CM3INT.sql_loader_configs slc ON sle.config_id = slc.config_id
WHERE slc.pii_fields IS NOT NULL
  AND sle.started_date >= TRUNC(SYSDATE) - 7
ORDER BY sle.started_date DESC;
```

---

## Troubleshooting

### Common Issues

#### Issue 1: "Configuration not found or disabled"

**Symptom:**
```
[ERROR] Configuration not found or disabled: HR-CUSTOMER-LOAD-001
```

**Solution:**
```sql
-- Check if configuration exists and is enabled
SELECT config_id, enabled FROM CM3INT.sql_loader_configs
WHERE config_id = 'HR-CUSTOMER-LOAD-001';

-- Enable if disabled
UPDATE CM3INT.sql_loader_configs
SET enabled = 'Y'
WHERE config_id = 'HR-CUSTOMER-LOAD-001';
COMMIT;
```

#### Issue 2: "sqlldr: command not found"

**Symptom:**
```
sqlldr: command not found
```

**Solution:**
```bash
# Verify Oracle client installation
which sqlldr

# If not found, set ORACLE_HOME
export ORACLE_HOME=/path/to/oracle/client
export PATH=$ORACLE_HOME/bin:$PATH

# Verify
sqlldr -help
```

#### Issue 3: High Rejection Rate

**Symptom:**
```
Rejected Records: 500 / 1000
```

**Solution:**
```bash
# Check bad file for rejected records
cat /tmp/data/bad/customers.bad

# Check discard file
cat /tmp/data/discard/customers.dsc

# Review SQL*Loader log
cat /path/to/logs/sqlldr_*.log
```

Common rejection causes:
- Data type mismatch
- Constraint violations
- Invalid date formats
- Field length exceeded
- Null values in NOT NULL columns

#### Issue 4: Performance Degradation

**Symptom:**
Slow load times compared to baseline

**Diagnosis:**
```sql
SELECT config_id, job_name,
       avg_execution_time_ms as current_avg,
       max_execution_time_ms as current_max,
       slpb.avg_execution_time_ms as baseline_avg
FROM CM3INT.v_sql_loader_performance vslp
LEFT JOIN CM3INT.sql_loader_performance_baselines slpb
  ON vslp.config_id = slpb.config_id
WHERE vslp.avg_execution_time_ms > slpb.avg_execution_time_ms * 1.5
ORDER BY vslp.avg_execution_time_ms DESC;
```

**Solutions:**
1. **Enable Direct Path Loading:**
```sql
UPDATE CM3INT.sql_loader_configs
SET direct_path = 'Y'
WHERE config_id = 'YOUR-CONFIG-ID';
```

2. **Increase Parallelism:**
```sql
UPDATE CM3INT.sql_loader_configs
SET parallel_degree = 4
WHERE config_id = 'YOUR-CONFIG-ID';
```

3. **Tune Buffer Sizes:**
```sql
UPDATE CM3INT.sql_loader_configs
SET bind_size = 1048576,  -- 1MB
    read_size = 4194304   -- 4MB
WHERE config_id = 'YOUR-CONFIG-ID';
```

### Debug Logging

**Enable Debug Mode:**
```bash
DEBUG=true ./scripts/sqlldr_automation.sh HR-CUSTOMER-LOAD-001 /tmp/data/input/customers.dat
```

**Check Logs:**
```bash
# Main log
tail -f logs/sqlldr_automation_*.log

# Error log
tail -f logs/sqlldr_errors_*.log

# SQL*Loader log
tail -f logs/sqlldr_HR-CUSTOMER-LOAD-001_*.log
```

---

## Best Practices

### 1. Configuration Management
- Use consistent naming conventions for config_id (e.g., `SYSTEM-JOB-PURPOSE-NNN`)
- Document all configurations with meaningful descriptions
- Version control your SQL configuration scripts
- Regularly review and cleanup unused configurations

### 2. Security
- Always mark PII fields in `pii_fields` column
- Set appropriate `data_classification` levels
- Enable encryption for sensitive fields
- Review security audit logs regularly
- Implement least-privilege access to data files

### 3. Performance
- Use direct path loading for large files (> 100k records)
- Enable parallel loading for multi-million record files
- Tune bind_size and read_size based on file characteristics
- Monitor performance baselines and adjust configurations
- Disable indexes/constraints before large loads, rebuild after

### 4. Error Handling
- Set reasonable `max_errors` thresholds
- Always review bad files after execution
- Implement notification emails for critical loads
- Set up retry logic for transient failures
- Archive bad/discard files for analysis

### 5. Testing
- Test configurations in dev/test environments first
- Use sample data files to validate control files
- Verify data transformations with small datasets
- Check constraint violations before production loads
- Perform end-to-end testing with realistic data volumes

### 6. Monitoring
- Review execution summaries daily
- Monitor performance trends weekly
- Audit security events continuously
- Track compliance metrics monthly
- Set up alerts for failures and anomalies

### 7. Maintenance
- Archive old execution logs regularly (retention policy)
- Purge execution history older than retention period
- Update performance baselines quarterly
- Review and optimize slow-running configurations
- Keep control file templates current

---

## Support and Contact

For questions or issues, contact:
- **Development Team**: fabric-dev-team@company.com
- **Database Team**: fabric-dba-team@company.com
- **Security Team**: fabric-security@company.com

**Documentation Version**: 1.0
**Last Updated**: 2025-11-28
**Maintained By**: Senior Full Stack Developer Agent
