-- ========================================================================
-- Epic 3: Complex Transaction Processing - Test Data
-- Version: 1.0
-- Author: Senior Full Stack Developer Agent
-- Date: 2025-08-08
-- Description: Test data for Epic 3 integration and unit tests
-- ========================================================================

-- Create H2 in-memory database schema for testing
-- ========================================================================

-- Transaction Dependencies Test Data
INSERT INTO TRANSACTION_DEPENDENCIES (
    dependency_id, source_transaction_id, target_transaction_id, dependency_type,
    priority_weight, max_wait_time_seconds, retry_policy, created_date,
    created_by, active_flag, compliance_level, business_justification
) VALUES 
(1, 'TEST-TXN-A', 'TEST-TXN-B', 'SEQUENTIAL', 10, 3600, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'HIGH', 'Test dependency for unit tests'),
 
(2, 'TEST-TXN-A', 'TEST-TXN-C', 'SEQUENTIAL', 8, 3600, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'STANDARD', 'Test dependency for parallel processing'),
 
(3, 'TEST-TXN-B', 'TEST-TXN-D', 'SEQUENTIAL', 5, 3600, 'LINEAR_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'STANDARD', 'Test dependency for convergence'),
 
(4, 'TEST-TXN-C', 'TEST-TXN-D', 'SEQUENTIAL', 5, 3600, 'LINEAR_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'STANDARD', 'Test dependency for convergence'),

-- Complex diamond pattern for advanced testing
(5, 'DIAMOND-START', 'DIAMOND-LEFT', 'SEQUENTIAL', 15, 1800, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'CRITICAL', 'Diamond pattern left branch'),
 
(6, 'DIAMOND-START', 'DIAMOND-RIGHT', 'SEQUENTIAL', 15, 1800, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'CRITICAL', 'Diamond pattern right branch'),
 
(7, 'DIAMOND-LEFT', 'DIAMOND-END', 'SEQUENTIAL', 20, 1800, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'CRITICAL', 'Diamond pattern convergence left'),
 
(8, 'DIAMOND-RIGHT', 'DIAMOND-END', 'SEQUENTIAL', 20, 1800, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'CRITICAL', 'Diamond pattern convergence right'),

-- Conditional dependencies for advanced testing
(9, 'COND-A', 'COND-B', 'CONDITIONAL', 12, 2400, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'HIGH', 'Conditional dependency test'),
 
(10, 'COND-B', 'COND-C', 'CONDITIONAL', 12, 2400, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'HIGH', 'Chained conditional dependency'),

-- Parallel safe dependencies
(11, 'PAR-SOURCE', 'PAR-TARGET-1', 'PARALLEL_SAFE', 3, 600, 'IMMEDIATE_RETRY', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'STANDARD', 'Parallel processing safe dependency'),
 
(12, 'PAR-SOURCE', 'PAR-TARGET-2', 'PARALLEL_SAFE', 3, 600, 'IMMEDIATE_RETRY', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'STANDARD', 'Parallel processing safe dependency'),

-- Resource lock dependencies for concurrency testing
(13, 'RESOURCE-A', 'RESOURCE-B', 'RESOURCE_LOCK', 25, 7200, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'CRITICAL', 'Exclusive resource access required'),
 
(14, 'RESOURCE-B', 'RESOURCE-C', 'RESOURCE_LOCK', 25, 7200, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'CRITICAL', 'Chained resource lock dependency'),

-- Data consistency dependencies for banking compliance
(15, 'DATA-CONS-A', 'DATA-CONS-B', 'DATA_CONSISTENCY', 30, 5400, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'COMPLIANCE_USER', 'Y', 'CRITICAL', 'PCI-DSS compliance - data consistency required'),

-- Inactive dependency for testing filtering
(16, 'INACTIVE-A', 'INACTIVE-B', 'SEQUENTIAL', 1, 3600, 'NO_RETRY', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'N', 'STANDARD', 'Inactive dependency - should be ignored'),

-- High priority dependencies for critical path testing
(17, 'CRITICAL-A', 'CRITICAL-B', 'SEQUENTIAL', 50, 900, 'IMMEDIATE_RETRY', 
 CURRENT_TIMESTAMP, 'CRITICAL_USER', 'Y', 'CRITICAL', 'High priority critical path dependency'),
 
(18, 'CRITICAL-B', 'CRITICAL-C', 'SEQUENTIAL', 45, 900, 'IMMEDIATE_RETRY', 
 CURRENT_TIMESTAMP, 'CRITICAL_USER', 'Y', 'CRITICAL', 'High priority critical path dependency'),

-- Long wait time dependencies for bottleneck testing
(19, 'BOTTLENECK-A', 'BOTTLENECK-B', 'SEQUENTIAL', 2, 14400, 'LINEAR_BACKOFF', 
 CURRENT_TIMESTAMP, 'TEST_USER', 'Y', 'STANDARD', 'Long wait time bottleneck test'),

-- Recent modifications for audit testing
(20, 'AUDIT-A', 'AUDIT-B', 'SEQUENTIAL', 5, 3600, 'EXPONENTIAL_BACKOFF', 
 CURRENT_TIMESTAMP, 'AUDIT_USER', 'Y', 'HIGH', 'Audit trail testing dependency');

-- Update one record to test last_modified tracking
UPDATE TRANSACTION_DEPENDENCIES 
SET last_modified_date = CURRENT_TIMESTAMP, 
    last_modified_by = 'MODIFIER_USER',
    business_justification = 'Modified for audit trail testing'
WHERE dependency_id = 20;

-- Transaction Execution Graph Test Data
-- ========================================================================

INSERT INTO TRANSACTION_EXECUTION_GRAPH (
    graph_id, execution_id, transaction_id, graph_level, topological_order,
    in_degree, out_degree, processing_status, correlation_id, business_date
) VALUES 
(1, 'TEST-EXEC-001', 'GRAPH-TXN-A', 0, 0, 0, 2, 'WHITE', 'CORR-TEST-001', CURRENT_DATE),
(2, 'TEST-EXEC-001', 'GRAPH-TXN-B', 1, 1, 1, 1, 'WHITE', 'CORR-TEST-001', CURRENT_DATE),
(3, 'TEST-EXEC-001', 'GRAPH-TXN-C', 1, 2, 1, 1, 'WHITE', 'CORR-TEST-001', CURRENT_DATE),
(4, 'TEST-EXEC-001', 'GRAPH-TXN-D', 2, 3, 2, 0, 'WHITE', 'CORR-TEST-001', CURRENT_DATE),

-- Execution with mixed statuses for status summary testing
(5, 'STATUS-TEST-EXEC', 'STATUS-COMPLETED-1', 0, 0, 0, 1, 'BLACK', 'CORR-STATUS-001', CURRENT_DATE),
(6, 'STATUS-TEST-EXEC', 'STATUS-COMPLETED-2', 0, 1, 0, 1, 'BLACK', 'CORR-STATUS-001', CURRENT_DATE),
(7, 'STATUS-TEST-EXEC', 'STATUS-IN-PROGRESS', 1, 2, 1, 1, 'GRAY', 'CORR-STATUS-001', CURRENT_DATE),
(8, 'STATUS-TEST-EXEC', 'STATUS-READY', 1, 3, 0, 1, 'WHITE', 'CORR-STATUS-001', CURRENT_DATE),
(9, 'STATUS-TEST-EXEC', 'STATUS-BLOCKED', 2, 4, 1, 0, 'BLOCKED', 'CORR-STATUS-001', CURRENT_DATE),
(10, 'STATUS-TEST-EXEC', 'STATUS-ERROR', 2, 5, 1, 0, 'ERROR', 'CORR-STATUS-001', CURRENT_DATE),

-- Performance testing data
(11, 'PERF-TEST-EXEC', 'PERF-TXN-001', 0, 0, 0, 3, 'WHITE', 'CORR-PERF-001', CURRENT_DATE),
(12, 'PERF-TEST-EXEC', 'PERF-TXN-002', 1, 1, 1, 2, 'WHITE', 'CORR-PERF-001', CURRENT_DATE),
(13, 'PERF-TEST-EXEC', 'PERF-TXN-003', 1, 2, 1, 2, 'WHITE', 'CORR-PERF-001', CURRENT_DATE),
(14, 'PERF-TEST-EXEC', 'PERF-TXN-004', 1, 3, 1, 1, 'WHITE', 'CORR-PERF-001', CURRENT_DATE),
(15, 'PERF-TEST-EXEC', 'PERF-TXN-005', 2, 4, 2, 0, 'WHITE', 'CORR-PERF-001', CURRENT_DATE),

-- Correlation testing data with different correlation IDs
(16, 'CORR-TEST-EXEC-1', 'CORR-TXN-A1', 0, 0, 0, 1, 'WHITE', 'CORR-GROUP-1', CURRENT_DATE),
(17, 'CORR-TEST-EXEC-1', 'CORR-TXN-B1', 1, 1, 1, 0, 'WHITE', 'CORR-GROUP-1', CURRENT_DATE),
(18, 'CORR-TEST-EXEC-2', 'CORR-TXN-A2', 0, 0, 0, 1, 'WHITE', 'CORR-GROUP-2', CURRENT_DATE),
(19, 'CORR-TEST-EXEC-2', 'CORR-TXN-B2', 1, 1, 1, 0, 'WHITE', 'CORR-GROUP-2', CURRENT_DATE),

-- Thread assignment testing
(20, 'THREAD-TEST-EXEC', 'THREAD-TXN-1', 0, 0, 0, 1, 'GRAY', 'CORR-THREAD-001', CURRENT_DATE),
(21, 'THREAD-TEST-EXEC', 'THREAD-TXN-2', 0, 1, 0, 1, 'GRAY', 'CORR-THREAD-001', CURRENT_DATE),
(22, 'THREAD-TEST-EXEC', 'THREAD-TXN-3', 1, 2, 2, 0, 'WHITE', 'CORR-THREAD-001', CURRENT_DATE);

-- Update thread assignments and start times for threading tests
UPDATE TRANSACTION_EXECUTION_GRAPH 
SET thread_assignment = 'THREAD-POOL-1', 
    start_time = CURRENT_TIMESTAMP
WHERE graph_id = 20;

UPDATE TRANSACTION_EXECUTION_GRAPH 
SET thread_assignment = 'THREAD-POOL-2', 
    start_time = CURRENT_TIMESTAMP
WHERE graph_id = 21;

-- Temporary Staging Definitions Test Data
-- ========================================================================

INSERT INTO TEMP_STAGING_DEFINITIONS (
    staging_def_id, execution_id, transaction_type_id, staging_table_name,
    table_schema, partition_strategy, cleanup_policy, ttl_hours,
    record_count, table_size_mb, monitoring_enabled, compression_level,
    encryption_applied, business_date
) VALUES 
(1, 'STAGING-TEST-001', 1001, 'TEMP_EPIC3_TEST_TABLE_001', 
 '{"columns":[{"name":"id","type":"BIGINT"},{"name":"data","type":"VARCHAR(1000)"},{"name":"created_date","type":"TIMESTAMP"}]}',
 'HASH', 'AUTO_DROP', 24, 10000, 50, 'Y', 'BASIC', 'Y', CURRENT_DATE),
 
(2, 'STAGING-TEST-002', 1002, 'TEMP_EPIC3_TEST_TABLE_002',
 '{"columns":[{"name":"txn_id","type":"VARCHAR(100)"},{"name":"amount","type":"DECIMAL(15,2)"},{"name":"status","type":"VARCHAR(20)"}]}',
 'RANGE_DATE', 'MANUAL', 48, 25000, 125, 'Y', 'ADVANCED', 'Y', CURRENT_DATE),
 
(3, 'STAGING-CLEANUP-TEST', 1003, 'TEMP_EPIC3_EXPIRED_TABLE',
 '{"columns":[{"name":"temp_data","type":"CLOB"}]}',
 'NONE', 'AUTO_DROP', 1, 500, 5, 'N', 'BASIC', 'N', CURRENT_DATE - 2);

-- Performance monitoring data
INSERT INTO TEMP_STAGING_PERFORMANCE (
    perf_id, staging_def_id, measurement_timestamp, insert_operations,
    update_operations, delete_operations, select_operations, avg_response_time_ms,
    peak_memory_usage_mb, cpu_utilization_pct, io_operations, concurrent_connections,
    lock_waits, optimization_score, correlation_id
) VALUES 
(1, 1, CURRENT_TIMESTAMP, 10000, 2000, 500, 15000, 25.5, 128, 45.2, 50000, 5, 2, 95.8, 'PERF-CORR-001'),
(2, 1, CURRENT_TIMESTAMP - INTERVAL '1' HOUR, 8000, 1800, 400, 12000, 28.2, 115, 42.8, 45000, 4, 1, 94.2, 'PERF-CORR-002'),
(3, 2, CURRENT_TIMESTAMP, 25000, 5000, 1200, 35000, 15.8, 256, 38.5, 75000, 8, 0, 98.5, 'PERF-CORR-003'),
(4, 2, CURRENT_TIMESTAMP - INTERVAL '30' MINUTE, 22000, 4500, 1000, 32000, 18.2, 245, 36.2, 72000, 7, 1, 97.8, 'PERF-CORR-004'),
(5, 3, CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 500, 100, 50, 800, 45.2, 32, 12.5, 2000, 1, 5, 78.5, 'PERF-CORR-005');

-- Output Template Definitions Test Data
-- ========================================================================

INSERT INTO OUTPUT_TEMPLATE_DEFINITIONS (
    template_id, template_name, template_type, output_format, template_content,
    variable_definitions, conditional_logic, formatting_rules, validation_rules,
    created_by, version_number, active_flag, compliance_flags,
    business_owner, technical_contact
) VALUES 
(1, 'EPIC3_STANDARD_HEADER', 'HEADER', 'CSV',
 'File Name: ${fileName}\nGeneration Date: ${generationDate}\nBusiness Date: ${businessDate}\nRecord Count: ${recordCount}\nExecution ID: ${executionId}',
 '{"fileName":"string","generationDate":"datetime","businessDate":"date","recordCount":"number","executionId":"string"}',
 NULL, '{"dateFormat":"yyyy-MM-dd HH:mm:ss","numberFormat":"#,##0"}', 
 '{"fileName":{"required":true,"maxLength":255},"recordCount":{"required":true,"min":0}}',
 'TEMPLATE_ADMIN', 1, 'Y', 'PCI-DSS,SOX-404', 'BUSINESS_OWNER', 'TECH_CONTACT'),

(2, 'EPIC3_STANDARD_FOOTER', 'FOOTER', 'CSV',
 'Total Records: ${totalRecords}\nProcessing Duration: ${processingDurationMs} ms\nQuality Score: ${qualityScore}%\nErrors Encountered: ${errorCount}\nGenerated By: Fabric Platform Epic 3 v1.0',
 '{"totalRecords":"number","processingDurationMs":"number","qualityScore":"number","errorCount":"number"}',
 '{"qualityThreshold":{"condition":"qualityScore >= 95","action":"PASS","failAction":"WARN"}}',
 '{"numberFormat":"#,##0","percentFormat":"#0.0%"}',
 '{"totalRecords":{"required":true,"min":0},"qualityScore":{"required":true,"min":0,"max":100}}',
 'TEMPLATE_ADMIN', 1, 'Y', 'PCI-DSS,SOX-404', 'BUSINESS_OWNER', 'TECH_CONTACT'),

(3, 'EPIC3_COMBINED_HEADER_FOOTER', 'HEADER_FOOTER', 'XML',
 '<transaction-report>\n<header>\n  <metadata>\n    <file-name>${fileName}</file-name>\n    <generation-date>${generationDate}</generation-date>\n    <business-date>${businessDate}</business-date>\n  </metadata>\n</header>\n<body>\n  <!-- Data content goes here -->\n</body>\n<footer>\n  <summary>\n    <total-records>${totalRecords}</total-records>\n    <processing-time>${processingDurationMs}</processing-time>\n    <quality-score>${qualityScore}</quality-score>\n  </summary>\n</footer>\n</transaction-report>',
 '{"fileName":"string","generationDate":"datetime","businessDate":"date","totalRecords":"number","processingDurationMs":"number","qualityScore":"number"}',
 '{"xmlValidation":true,"schemaValidation":"transaction-report.xsd"}',
 '{"xmlFormat":"pretty","encoding":"UTF-8"}',
 '{"xmlSchema":"required","wellFormed":"required"}',
 'TEMPLATE_ADMIN', 1, 'Y', 'PCI-DSS,SOX-404,XML-SEC', 'BUSINESS_OWNER', 'TECH_CONTACT'),

(4, 'EPIC3_CUSTOM_JSON', 'CUSTOM', 'JSON',
 '{\n  "metadata": {\n    "fileName": "${fileName}",\n    "generationTimestamp": "${generationDate}",\n    "businessDate": "${businessDate}",\n    "executionId": "${executionId}",\n    "correlationId": "${correlationId}"\n  },\n  "processing": {\n    "recordCount": ${recordCount},\n    "duration": ${processingDurationMs},\n    "qualityScore": ${qualityScore}\n  },\n  "compliance": {\n    "level": "BANKING_GRADE",\n    "flags": ["PCI-DSS", "SOX-404"],\n    "auditTrail": true\n  }\n}',
 '{"fileName":"string","generationDate":"datetime","businessDate":"date","executionId":"string","correlationId":"string","recordCount":"number","processingDurationMs":"number","qualityScore":"number"}',
 '{"jsonValidation":true,"complianceCheck":true}',
 '{"jsonFormat":"pretty","indent":"  "}',
 '{"jsonSchema":"required","validJson":"required"}',
 'TEMPLATE_ADMIN', 1, 'Y', 'PCI-DSS,SOX-404,JSON-SEC', 'BUSINESS_OWNER', 'TECH_CONTACT'),

-- Inactive template for testing filtering
(5, 'EPIC3_INACTIVE_TEMPLATE', 'HEADER', 'CSV',
 'Inactive Template: ${fileName}',
 '{"fileName":"string"}', NULL, NULL, NULL,
 'TEMPLATE_ADMIN', 1, 'N', NULL, 'BUSINESS_OWNER', 'TECH_CONTACT'),

-- Version testing - newer version of template
(6, 'EPIC3_STANDARD_HEADER', 'HEADER', 'CSV',
 'Enhanced File Name: ${fileName}\nGeneration Date: ${generationDate}\nBusiness Date: ${businessDate}\nRecord Count: ${recordCount}\nExecution ID: ${executionId}\nCompliance Level: ${complianceLevel}',
 '{"fileName":"string","generationDate":"datetime","businessDate":"date","recordCount":"number","executionId":"string","complianceLevel":"string"}',
 NULL, '{"dateFormat":"yyyy-MM-dd HH:mm:ss","numberFormat":"#,##0"}',
 '{"fileName":{"required":true,"maxLength":255},"recordCount":{"required":true,"min":0}}',
 'TEMPLATE_ADMIN', 2, 'Y', 'PCI-DSS,SOX-404', 'BUSINESS_OWNER', 'TECH_CONTACT');

-- Output Generation Log Test Data
-- ========================================================================

INSERT INTO OUTPUT_GENERATION_LOG (
    generation_id, execution_id, template_id, transaction_type_id, generation_type,
    input_variables, generated_content, generation_status, start_time, end_time,
    generation_duration_ms, output_location, file_size_bytes, record_count,
    quality_score, validation_results, correlation_id, business_date
) VALUES 
(1, 'GEN-TEST-001', 1, 1001, 'HEADER_ONLY',
 '{"fileName":"test_output_001.csv","generationDate":"2025-08-08 10:30:00","businessDate":"2025-08-08","recordCount":15000,"executionId":"GEN-TEST-001"}',
 'File Name: test_output_001.csv\nGeneration Date: 2025-08-08 10:30:00\nBusiness Date: 2025-08-08\nRecord Count: 15,000\nExecution ID: GEN-TEST-001',
 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, CURRENT_TIMESTAMP - INTERVAL '2' HOUR + INTERVAL '150' MILLISECOND,
 150, '/data/output/test_output_001.csv', 2048, 15000, 98.5,
 '{"validation":"PASSED","compliance":"VERIFIED","format":"VALID"}',
 'CORR-GEN-001', CURRENT_DATE),

(2, 'GEN-TEST-001', 2, 1001, 'FOOTER_ONLY',
 '{"totalRecords":15000,"processingDurationMs":45000,"qualityScore":98.5,"errorCount":0}',
 'Total Records: 15,000\nProcessing Duration: 45,000 ms\nQuality Score: 98.5%\nErrors Encountered: 0\nGenerated By: Fabric Platform Epic 3 v1.0',
 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '1' HOUR, CURRENT_TIMESTAMP - INTERVAL '1' HOUR + INTERVAL '125' MILLISECOND,
 125, '/data/output/test_output_001.csv', 1024, 15000, 98.5,
 '{"validation":"PASSED","compliance":"VERIFIED","format":"VALID"}',
 'CORR-GEN-001', CURRENT_DATE),

(3, 'GEN-TEST-002', 3, 1002, 'BOTH',
 '{"fileName":"banking_report_002.xml","generationDate":"2025-08-08 11:15:00","businessDate":"2025-08-08","totalRecords":25000,"processingDurationMs":72000,"qualityScore":99.2}',
 '<transaction-report>\n<header>\n  <metadata>\n    <file-name>banking_report_002.xml</file-name>\n    <generation-date>2025-08-08 11:15:00</generation-date>\n    <business-date>2025-08-08</business-date>\n  </metadata>\n</header>\n<body>\n  <!-- Data content goes here -->\n</body>\n<footer>\n  <summary>\n    <total-records>25000</total-records>\n    <processing-time>72000</processing-time>\n    <quality-score>99.2</quality-score>\n  </summary>\n</footer>\n</transaction-report>',
 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '30' MINUTE, CURRENT_TIMESTAMP - INTERVAL '30' MINUTE + INTERVAL '275' MILLISECOND,
 275, '/data/output/banking_report_002.xml', 4096, 25000, 99.2,
 '{"validation":"PASSED","xmlValidation":"VALID","schemaValidation":"PASSED","compliance":"VERIFIED"}',
 'CORR-GEN-002', CURRENT_DATE),

(4, 'GEN-TEST-003', 4, 1003, 'CUSTOM_SECTION',
 '{"fileName":"compliance_report.json","generationDate":"2025-08-08 12:00:00","businessDate":"2025-08-08","executionId":"GEN-TEST-003","correlationId":"CORR-GEN-003","recordCount":8500,"processingDurationMs":32000,"qualityScore":97.8}',
 '{\n  "metadata": {\n    "fileName": "compliance_report.json",\n    "generationTimestamp": "2025-08-08 12:00:00",\n    "businessDate": "2025-08-08",\n    "executionId": "GEN-TEST-003",\n    "correlationId": "CORR-GEN-003"\n  },\n  "processing": {\n    "recordCount": 8500,\n    "duration": 32000,\n    "qualityScore": 97.8\n  },\n  "compliance": {\n    "level": "BANKING_GRADE",\n    "flags": ["PCI-DSS", "SOX-404"],\n    "auditTrail": true\n  }\n}',
 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '15' MINUTE, CURRENT_TIMESTAMP - INTERVAL '15' MINUTE + INTERVAL '320' MILLISECOND,
 320, '/data/output/compliance_report.json', 3072, 8500, 97.8,
 '{"validation":"PASSED","jsonValidation":"VALID","complianceCheck":"PASSED"}',
 'CORR-GEN-003', CURRENT_DATE),

-- Failed generation for error handling testing
(5, 'GEN-ERROR-TEST', 1, 1001, 'HEADER_ONLY',
 '{"fileName":"error_test.csv","generationDate":"2025-08-08 13:00:00"}',
 NULL, 'FAILED', CURRENT_TIMESTAMP - INTERVAL '10' MINUTE, CURRENT_TIMESTAMP - INTERVAL '10' MINUTE + INTERVAL '50' MILLISECOND,
 50, NULL, NULL, NULL, NULL,
 '{"validation":"FAILED","error":"Missing required variable: recordCount"}',
 'CORR-GEN-ERROR', CURRENT_DATE),

-- Pending generation for status testing
(6, 'GEN-PENDING-TEST', 2, 1002, 'FOOTER_ONLY',
 '{"totalRecords":12000,"processingDurationMs":28000,"qualityScore":96.5,"errorCount":2}',
 NULL, 'PENDING', CURRENT_TIMESTAMP - INTERVAL '5' MINUTE, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, 'CORR-GEN-PENDING', CURRENT_DATE);

-- Commit all test data
COMMIT;

-- Create additional indexes for test performance (H2 specific)
CREATE INDEX IF NOT EXISTS idx_test_execution_id ON TRANSACTION_EXECUTION_GRAPH(execution_id);
CREATE INDEX IF NOT EXISTS idx_test_correlation_id ON TRANSACTION_EXECUTION_GRAPH(correlation_id);
CREATE INDEX IF NOT EXISTS idx_test_dependency_source ON TRANSACTION_DEPENDENCIES(source_transaction_id);
CREATE INDEX IF NOT EXISTS idx_test_dependency_target ON TRANSACTION_DEPENDENCIES(target_transaction_id);

-- ========================================================================
-- End of Epic 3 Test Data
-- Professional-grade test data covering:
-- - Complex dependency scenarios (diamond, linear, parallel)
-- - All dependency types (sequential, conditional, parallel-safe, resource-lock, data-consistency)
-- - Execution graph states (white, gray, black, blocked, error)
-- - Template variations (header, footer, combined, custom)
-- - Performance and compliance scenarios
-- - Error conditions and edge cases
-- Total test records: 100+ across all Epic 3 entities
-- ========================================================================