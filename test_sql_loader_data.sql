-- ============================================================================
-- SQL*LOADER TEST DATA FOR FRONTEND TESTING
-- Run this after Phase 1.1 database migration to prepare test data
-- ============================================================================

-- Test Configuration 1: HR Employee Data
INSERT INTO CM3INT.sql_loader_configs (
    config_id, job_name, source_system, target_table, 
    control_file_template, created_by, created_date
) VALUES (
    'HR-EMPLOYEE-001', 'hr_employee_load', 'hr', 'EMPLOYEE_STAGING',
    'LOAD DATA INFILE * INTO TABLE employee_staging FIELDS TERMINATED BY "|"',
    'test_user', CURRENT_TIMESTAMP
);

-- Test Field Configurations for HR Employee
INSERT INTO CM3INT.sql_loader_field_configs (
    field_id, config_id, field_name, column_name, field_position,
    data_type, nullable, created_by
) VALUES 
(1, 'HR-EMPLOYEE-001', 'employee_id', 'EMPLOYEE_ID', 1, 'NUMBER', 'N', 'test_user'),
(2, 'HR-EMPLOYEE-001', 'first_name', 'FIRST_NAME', 2, 'VARCHAR2', 'N', 'test_user'),
(3, 'HR-EMPLOYEE-001', 'last_name', 'LAST_NAME', 3, 'VARCHAR2', 'N', 'test_user'),
(4, 'HR-EMPLOYEE-001', 'email', 'EMAIL', 4, 'VARCHAR2', 'Y', 'test_user'),
(5, 'HR-EMPLOYEE-001', 'hire_date', 'HIRE_DATE', 5, 'DATE', 'N', 'test_user');

-- Test Configuration 2: Finance Transaction Data
INSERT INTO CM3INT.sql_loader_configs (
    config_id, job_name, source_system, target_table,
    control_file_template, created_by, created_date
) VALUES (
    'FIN-TRANS-001', 'finance_transaction_load', 'finance', 'TRANSACTION_STAGING',
    'LOAD DATA INFILE * INTO TABLE transaction_staging FIELDS TERMINATED BY "|"',
    'test_user', CURRENT_TIMESTAMP
);

-- Test Execution Log
INSERT INTO CM3INT.sql_loader_executions (
    execution_id, config_id, correlation_id, start_time, end_time,
    status, total_records, successful_records, rejected_records,
    created_by
) VALUES (
    'EXEC-001', 'HR-EMPLOYEE-001', 'CORR-001', 
    CURRENT_TIMESTAMP - 1, CURRENT_TIMESTAMP,
    'SUCCESS', 1000, 995, 5, 'test_user'
);

COMMIT;

-- Verification queries for frontend testing
SELECT 'SQL*Loader Configurations' as TEST_TYPE, COUNT(*) as COUNT 
FROM CM3INT.sql_loader_configs
UNION ALL
SELECT 'Field Configurations', COUNT(*) 
FROM CM3INT.sql_loader_field_configs
UNION ALL  
SELECT 'Execution Logs', COUNT(*) 
FROM CM3INT.sql_loader_executions;