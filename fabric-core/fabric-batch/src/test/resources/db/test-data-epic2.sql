-- ========================================================================
-- Epic 2: Test Data Setup for Integration Tests
-- Purpose: Provides test data for Epic 2 parallel processing integration tests
-- ========================================================================

-- Test Transaction Types
INSERT INTO BATCH_TRANSACTION_TYPES (
    transaction_type_id, job_config_id, transaction_type, processing_order,
    parallel_threads, chunk_size, isolation_level, timeout_seconds,
    created_date, created_by, compliance_level, active_flag
) VALUES 
(1, 1, 'HIGH_SECURITY', 1, 5, 1000, 'read_committed', 300, CURRENT_TIMESTAMP, 'TEST_SETUP', 'HIGH', 'Y'),
(2, 1, 'STANDARD', 2, 3, 2000, 'read_committed', 300, CURRENT_TIMESTAMP, 'TEST_SETUP', 'STANDARD', 'Y'),
(3, 1, 'LOW_VOLUME', 3, 2, 500, 'read_committed', 600, CURRENT_TIMESTAMP, 'TEST_SETUP', 'STANDARD', 'Y');

-- Test Field Mappings for HIGH_SECURITY transaction type
INSERT INTO FIELD_MAPPINGS (
    mapping_id, transaction_type_id, field_name, source_field, target_field,
    target_position, field_length, transformation_type, default_value,
    encryption_level, pii_classification, sequence_order, active_flag,
    created_date, created_by, compliance_level
) VALUES 
(1, 1, 'accountNumber', 'account_number', 'ACCOUNT_NUM', 1, 12, 'source', '', 'HIGH', 'CONFIDENTIAL', 1, 'Y', CURRENT_TIMESTAMP, 'TEST_SETUP', 'HIGH'),
(2, 1, 'customerName', 'customer_name', 'CUST_NAME', 2, 50, 'source', '', 'NONE', 'INTERNAL', 2, 'Y', CURRENT_TIMESTAMP, 'TEST_SETUP', 'STANDARD'),
(3, 1, 'ssn', 'social_security_number', 'SSN', 3, 11, 'source', '', 'CRITICAL', 'RESTRICTED', 3, 'Y', CURRENT_TIMESTAMP, 'TEST_SETUP', 'CRITICAL'),
(4, 1, 'amount', 'transaction_amount', 'TXN_AMOUNT', 4, 15, 'source', '0.00', 'NONE', 'INTERNAL', 4, 'Y', CURRENT_TIMESTAMP, 'TEST_SETUP', 'STANDARD'),
(5, 1, 'transactionDate', 'txn_date', 'TXN_DATE', 5, 10, 'source', '', 'NONE', 'PUBLIC', 5, 'Y', CURRENT_TIMESTAMP, 'TEST_SETUP', 'STANDARD'),
(6, 1, 'cardNumber', 'card_number', 'CARD_NUM', 6, 16, 'source', '', 'CRITICAL', 'CARDHOLDER_DATA', 6, 'Y', CURRENT_TIMESTAMP, 'TEST_SETUP', 'CRITICAL');

-- Test Field Mappings for STANDARD transaction type
INSERT INTO FIELD_MAPPINGS (
    mapping_id, transaction_type_id, field_name, source_field, target_field,
    target_position, field_length, transformation_type, default_value,
    encryption_level, pii_classification, sequence_order, active_flag,
    created_date, created_by, compliance_level
) VALUES 
(7, 2, 'id', 'record_id', 'ID', 1, 10, 'source', '', 'NONE', 'PUBLIC', 1, 'Y', CURRENT_TIMESTAMP, 'TEST_SETUP', 'STANDARD'),
(8, 2, 'description', 'txn_description', 'DESCRIPTION', 2, 100, 'source', '', 'NONE', 'INTERNAL', 2, 'Y', CURRENT_TIMESTAMP, 'TEST_SETUP', 'STANDARD'),
(9, 2, 'status', 'processing_status', 'STATUS', 3, 10, 'constant', 'ACTIVE', 'NONE', 'PUBLIC', 3, 'Y', CURRENT_TIMESTAMP, 'TEST_SETUP', 'STANDARD');

-- Test Batch Configuration (if not exists from Epic 1)
INSERT INTO BATCH_CONFIGURATIONS (
    config_id, source_system, job_name, transaction_type, release_version,
    effective_date, deployment_status, configuration_level, created_date, created_by,
    parallel_enabled, max_parallel_threads, chunk_size, encryption_required, pci_data_flag
) VALUES 
(1, 'EPIC2_TEST', 'parallel_transaction_test', 'HIGH_SECURITY', '2.0.0', 
 CURRENT_TIMESTAMP, 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, 'TEST_SETUP',
 'Y', 10, 1000, 'Y', 'Y')
ON CONFLICT (config_id) DO NOTHING;

-- Test Execution Audit Records
INSERT INTO EXECUTION_AUDIT (
    execution_audit_id, execution_id, event_type, event_description,
    event_timestamp, user_id, source_ip, success_flag, compliance_flags,
    correlation_id, business_date
) VALUES 
(1, 'TEST_EPIC2_EXECUTION_001', 'BATCH_START', 'Test batch processing started', 
 CURRENT_TIMESTAMP, 'test_user', '127.0.0.1', 'Y', 'TEST', 
 'test-correlation-001', CURRENT_DATE),
(2, 'TEST_EPIC2_EXECUTION_001', 'SECURITY_EVENT', 'Field encryption enabled', 
 CURRENT_TIMESTAMP, 'test_user', '127.0.0.1', 'Y', 'ENCRYPTION,PCI_DSS', 
 'test-correlation-002', CURRENT_DATE),
(3, 'TEST_EPIC2_EXECUTION_001', 'TRANSACTION_START', 'Parallel processing initiated', 
 CURRENT_TIMESTAMP, 'test_user', '127.0.0.1', 'Y', 'PARALLEL_PROCESSING', 
 'test-correlation-003', CURRENT_DATE);

-- Test Processing Status Records
INSERT INTO BATCH_PROCESSING_STATUS (
    status_id, execution_id, transaction_type_id, processing_status,
    start_time, records_processed, records_failed, thread_id, last_heartbeat
) VALUES 
(1, 'TEST_EPIC2_EXECUTION_001', 1, 'RUNNING', CURRENT_TIMESTAMP, 1000, 5, 'test-thread-1', CURRENT_TIMESTAMP),
(2, 'TEST_EPIC2_EXECUTION_001', 2, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '1 hour', 2000, 10, 'test-thread-2', CURRENT_TIMESTAMP - INTERVAL '5 minutes'),
(3, 'TEST_EPIC2_EXECUTION_001', 3, 'INITIALIZED', CURRENT_TIMESTAMP, 0, 0, 'test-thread-3', CURRENT_TIMESTAMP);

-- Test Staging Records
INSERT INTO BATCH_TEMP_STAGING (
    staging_id, execution_id, transaction_type_id, sequence_number,
    source_data, processing_status, correlation_id, created_timestamp, business_date
) VALUES 
(1, 'TEST_EPIC2_EXECUTION_001', 1, 1, '{"id":1,"account":"123456","amount":100.00}', 'PENDING', 'corr-001', CURRENT_TIMESTAMP, CURRENT_DATE),
(2, 'TEST_EPIC2_EXECUTION_001', 1, 2, '{"id":2,"account":"789012","amount":200.00}', 'COMPLETED', 'corr-002', CURRENT_TIMESTAMP, CURRENT_DATE),
(3, 'TEST_EPIC2_EXECUTION_001', 2, 3, '{"id":3,"description":"Test transaction"}', 'FAILED', 'corr-003', CURRENT_TIMESTAMP, CURRENT_DATE);

-- Test Data Lineage Records
INSERT INTO DATA_LINEAGE (
    lineage_id, execution_id, source_system, source_file, source_record_count,
    target_system, target_file, target_record_count, data_quality_score,
    compliance_validated, created_timestamp, correlation_id, business_date
) VALUES 
(1, 'TEST_EPIC2_EXECUTION_001', 'EPIC2_TEST', 'test_input.csv', 1000,
 'TARGET_SYSTEM', 'test_output.dat', 995, 98.5, 'Y', 
 CURRENT_TIMESTAMP, 'lineage-001', CURRENT_DATE),
(2, 'TEST_EPIC2_EXECUTION_001', 'EPIC2_TEST', 'test_input_2.csv', 2000,
 'TARGET_SYSTEM', 'test_output_2.dat', 1998, 99.2, 'Y', 
 CURRENT_TIMESTAMP, 'lineage-002', CURRENT_DATE);

COMMIT;

-- Verify test data
SELECT 'BATCH_TRANSACTION_TYPES' as table_name, COUNT(*) as record_count FROM BATCH_TRANSACTION_TYPES WHERE created_by = 'TEST_SETUP'
UNION ALL
SELECT 'FIELD_MAPPINGS', COUNT(*) FROM FIELD_MAPPINGS WHERE created_by = 'TEST_SETUP'
UNION ALL
SELECT 'EXECUTION_AUDIT', COUNT(*) FROM EXECUTION_AUDIT WHERE execution_id LIKE 'TEST_EPIC2%'
UNION ALL
SELECT 'BATCH_PROCESSING_STATUS', COUNT(*) FROM BATCH_PROCESSING_STATUS WHERE execution_id LIKE 'TEST_EPIC2%'
UNION ALL
SELECT 'BATCH_TEMP_STAGING', COUNT(*) FROM BATCH_TEMP_STAGING WHERE execution_id LIKE 'TEST_EPIC2%'
UNION ALL
SELECT 'DATA_LINEAGE', COUNT(*) FROM DATA_LINEAGE WHERE execution_id LIKE 'TEST_EPIC2%';