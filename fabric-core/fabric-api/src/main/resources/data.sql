-- Sample data for testing the fabric-platform backend
-- This data will be loaded automatically by Spring Boot for H2 database

-- ============================================================================
-- Sample SOURCE_SYSTEMS data
-- ============================================================================
INSERT INTO CM3INT.SOURCE_SYSTEMS (ID, NAME, TYPE, DESCRIPTION, CONNECTION_STRING, ENABLED, CREATED_DATE, JOB_COUNT)
VALUES 
    ('SYS001', 'Core Banking System', 'DATABASE', 'Primary core banking data source', 'jdbc:oracle:thin:@localhost:1521:xe', 'Y', CURRENT_TIMESTAMP, 5),
    ('SYS002', 'Customer Portal', 'REST_API', 'Customer portal REST API', 'https://customer-portal.truist.com/api/v1', 'Y', CURRENT_TIMESTAMP, 3),
    ('SYS003', 'Legacy Mainframe', 'BATCH_FILE', 'Legacy mainframe batch file system', 'ftp://mainframe.internal/batch/', 'Y', CURRENT_TIMESTAMP, 2),
    ('SYS004', 'Risk Management', 'DATABASE', 'Risk management database', 'jdbc:postgresql://risk-db:5432/riskdb', 'Y', CURRENT_TIMESTAMP, 1),
    ('SYS005', 'Test System', 'DATABASE', 'Testing and development system', 'jdbc:h2:mem:testdb', 'N', CURRENT_TIMESTAMP, 0),
    ('ENCORE', 'ENCORE', 'ORACLE', 'Lightstream loans', 'jdbc:oracle:thin:@localhost:1521/ORCLPDB1', 'Y', CURRENT_TIMESTAMP, 1),
    ('HR', 'HR', 'ORACLE', 'Human resources', 'jdbc:oracle:thin:@localhost:1521/ORCLPDB1', 'Y', CURRENT_TIMESTAMP, 0),
    ('SHAW', 'SHAW', 'ORACLE', 'Loan Origination System', 'jdbc:oracle:thin:@localhost:1521/ORCLPDB1', 'Y', CURRENT_TIMESTAMP, 5);

-- ============================================================================
-- Sample TEMPLATE_SOURCE_MAPPINGS data
-- ============================================================================
INSERT INTO CM3INT.TEMPLATE_SOURCE_MAPPINGS 
(FILE_TYPE, TRANSACTION_TYPE, SOURCE_SYSTEM_ID, JOB_NAME, TARGET_FIELD_NAME, SOURCE_FIELD_NAME, 
 TRANSFORMATION_TYPE, TARGET_POSITION, LENGTH, DATA_TYPE, CREATED_BY, CREATED_DATE, VERSION, ENABLED)
VALUES 
    -- Core Banking mappings
    ('CUSTOMER', 'NEW', 'SYS001', 'customer_onboarding_job', 'customer_id', 'CUST_ID', 'source', 1, 10, 'STRING', 'system_admin', CURRENT_TIMESTAMP, 1, 'Y'),
    ('CUSTOMER', 'NEW', 'SYS001', 'customer_onboarding_job', 'customer_name', 'FULL_NAME', 'source', 2, 50, 'STRING', 'system_admin', CURRENT_TIMESTAMP, 1, 'Y'),
    ('CUSTOMER', 'NEW', 'SYS001', 'customer_onboarding_job', 'account_number', 'ACCT_NUM', 'source', 3, 20, 'STRING', 'system_admin', CURRENT_TIMESTAMP, 1, 'Y'),
    ('CUSTOMER', 'NEW', 'SYS001', 'customer_onboarding_job', 'balance', 'CURRENT_BAL', 'source', 4, 15, 'DECIMAL', 'system_admin', CURRENT_TIMESTAMP, 1, 'Y'),
    
    -- Customer Portal mappings
    ('TRANSACTION', 'DAILY', 'SYS002', 'daily_transactions_job', 'transaction_id', 'txn_id', 'source', 1, 15, 'STRING', 'system_admin', CURRENT_TIMESTAMP, 1, 'Y'),
    ('TRANSACTION', 'DAILY', 'SYS002', 'daily_transactions_job', 'amount', 'transaction_amount', 'source', 2, 12, 'DECIMAL', 'system_admin', CURRENT_TIMESTAMP, 1, 'Y'),
    ('TRANSACTION', 'DAILY', 'SYS002', 'daily_transactions_job', 'transaction_date', 'txn_timestamp', 'transformation', 3, 10, 'DATE', 'system_admin', CURRENT_TIMESTAMP, 1, 'Y'),
    
    -- Legacy Mainframe mappings
    ('BATCH', 'LEGACY', 'SYS003', 'legacy_batch_job', 'record_type', 'REC_TYPE', 'source', 1, 2, 'STRING', 'system_admin', CURRENT_TIMESTAMP, 1, 'Y'),
    ('BATCH', 'LEGACY', 'SYS003', 'legacy_batch_job', 'legacy_id', 'LEGACY_KEY', 'source', 2, 20, 'STRING', 'system_admin', CURRENT_TIMESTAMP, 1, 'Y'),
    ('BATCH', 'LEGACY', 'SYS003', 'legacy_batch_job', 'process_date', 'PROC_DATE', 'transformation', 3, 8, 'DATE', 'system_admin', CURRENT_TIMESTAMP, 1, 'Y');

-- ============================================================================
-- Sample WEBSOCKET_AUDIT_LOG data (for testing)
-- ============================================================================
INSERT INTO WEBSOCKET_AUDIT_LOG 
(event_type, event_subtype, event_description, session_id, user_id, client_ip, 
 event_timestamp, security_event_flag, compliance_event_flag, event_severity, correlation_id)
VALUES 
    ('WEBSOCKET_CONNECTION_ESTABLISHED', 'CONNECTION', 'WebSocket connection established for dashboard monitoring', 'sess_001', 'test_user', '127.0.0.1', CURRENT_TIMESTAMP, 'N', 'N', 'INFO', 'corr_001'),
    ('DASHBOARD_DATA_REQUEST', 'DATA_ACCESS', 'User requested dashboard metrics data', 'sess_001', 'test_user', '127.0.0.1', CURRENT_TIMESTAMP, 'N', 'Y', 'INFO', 'corr_002'),
    ('CONFIGURATION_VIEW', 'ACCESS', 'User accessed configuration management interface', 'sess_002', 'admin_user', '192.168.1.100', CURRENT_TIMESTAMP, 'Y', 'Y', 'INFO', 'corr_003');

-- ============================================================================
-- Sample DASHBOARD_METRICS_TIMESERIES data (for testing)
-- ============================================================================
INSERT INTO DASHBOARD_METRICS_TIMESERIES 
(execution_id, metric_name, metric_type, metric_value, metric_unit, metric_status, 
 metric_timestamp, cpu_usage_percent, memory_usage_mb, processing_duration_ms, correlation_id, source_system)
VALUES 
    ('exec_001', 'job_execution_time', 'SYSTEM_PERFORMANCE', 1250.50, 'MILLISECONDS', 'OK', CURRENT_TIMESTAMP, 25.5, 512, 1250, 'corr_001', 'SYS001'),
    ('exec_001', 'records_processed', 'BUSINESS_KPI', 10000, 'COUNT', 'OK', CURRENT_TIMESTAMP, 25.5, 512, 1250, 'corr_001', 'SYS001'),
    ('exec_002', 'job_execution_time', 'SYSTEM_PERFORMANCE', 2100.75, 'MILLISECONDS', 'WARNING', CURRENT_TIMESTAMP, 45.2, 1024, 2100, 'corr_002', 'SYS002'),
    ('exec_002', 'error_count', 'BUSINESS_KPI', 3, 'COUNT', 'WARNING', CURRENT_TIMESTAMP, 45.2, 1024, 2100, 'corr_002', 'SYS002'),
    ('exec_003', 'throughput', 'SYSTEM_PERFORMANCE', 500.25, 'RECORDS_PER_SECOND', 'OK', CURRENT_TIMESTAMP, 15.8, 256, 800, 'corr_003', 'SYS003');