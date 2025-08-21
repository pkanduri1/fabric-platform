-- Test Data Cleanup Script for Fabric Platform Integration Tests
-- This script removes all test data before running integration tests
-- Author: Fabric Platform Testing Framework
-- Version: 1.0.0

-- Disable foreign key constraints temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Clean up manual job configurations (test data only)
DELETE FROM MANUAL_JOB_CONFIG 
WHERE CONFIG_ID LIKE 'cfg_test_%' 
   OR JOB_NAME LIKE '%Integration Test%'
   OR JOB_NAME LIKE '%Unit Test%'
   OR CREATED_BY LIKE 'test-%';

-- Clean up template configurations (test data only)  
DELETE FROM TEMPLATE_CONFIGURATIONS
WHERE TEMPLATE_ID LIKE 'tpl_test_%'
   OR TEMPLATE_NAME LIKE '%Test Template%'
   OR CREATED_BY LIKE 'test-%';

-- Clean up field mappings (test data only)
DELETE FROM FIELD_MAPPING_CONFIG
WHERE FIELD_ID LIKE 'field_test_%'
   OR CONFIG_ID IN (
       SELECT CONFIG_ID FROM MANUAL_JOB_CONFIG 
       WHERE CONFIG_ID LIKE 'cfg_test_%'
   );

-- Clean up source systems (test data only)
DELETE FROM SOURCE_SYSTEMS
WHERE SYSTEM_ID LIKE 'sys_test_%'
   OR SYSTEM_NAME LIKE '%Test System%'
   OR SYSTEM_CODE IN ('TEST', 'MOCK');

-- Clean up master queries (test data only)
DELETE FROM MASTER_QUERY_CONFIG
WHERE QUERY_ID LIKE 'qry_test_%'
   OR QUERY_NAME LIKE '%Test Query%'
   OR CREATED_BY LIKE 'test-%';

-- Clean up batch execution results (test data only)
DELETE FROM BATCH_EXECUTION_RESULTS
WHERE EXECUTION_ID LIKE 'exec_test_%'
   OR CONFIG_ID LIKE 'cfg_test_%';

-- Clean up configuration audit logs (test data only)
DELETE FROM CONFIGURATION_AUDIT_LOG
WHERE CONFIG_ID LIKE 'cfg_test_%'
   OR USER_ID LIKE 'test-%'
   OR CORRELATION_ID LIKE 'test-%';

-- Clean up batch configuration data (test data only)
DELETE FROM BATCH_CONFIGURATIONS
WHERE CONFIG_ID LIKE 'cfg_test_%'
   OR JOB_NAME LIKE '%Test%'
   OR CREATED_BY LIKE 'test-%';

-- Clean up execution audit logs (test data only)
DELETE FROM EXECUTION_AUDIT_LOG
WHERE EXECUTION_ID LIKE 'exec_test_%'
   OR USER_ID LIKE 'test-%';

-- Clean up WebSocket audit logs (test data only)
DELETE FROM WEBSOCKET_AUDIT_LOG
WHERE SESSION_ID LIKE 'test-%'
   OR USER_ID LIKE 'test-%';

-- Clean up dashboard metrics (test data only)
DELETE FROM DASHBOARD_METRICS_TIMESERIES
WHERE METRIC_NAME LIKE 'test_%'
   OR CREATED_BY LIKE 'test-%';

-- Clean up template source mappings (test data only)
DELETE FROM TEMPLATE_SOURCE_MAPPING
WHERE MAPPING_ID LIKE 'map_test_%'
   OR TEMPLATE_ID LIKE 'tpl_test_%';

-- Clean up SQL loader field configurations (test data only)
DELETE FROM SQL_LOADER_FIELD_CONFIG
WHERE FIELD_ID LIKE 'field_test_%'
   OR CONFIG_ID LIKE 'cfg_test_%';

-- Re-enable foreign key constraints
SET FOREIGN_KEY_CHECKS = 1;

-- Reset sequences/auto-increment values if needed
-- ALTER TABLE MANUAL_JOB_CONFIG AUTO_INCREMENT = 1000;
-- ALTER TABLE TEMPLATE_CONFIGURATIONS AUTO_INCREMENT = 1000;

-- Commit the cleanup transaction
COMMIT;

-- Log cleanup completion
INSERT INTO CONFIGURATION_AUDIT_LOG (
    CONFIG_ID,
    ACTION_TYPE,
    USER_ID,
    CORRELATION_ID,
    CHANGE_DESCRIPTION,
    TIMESTAMP
) VALUES (
    'CLEANUP_TEST_DATA',
    'CLEANUP',
    'SYSTEM',
    'test-data-cleanup-' || EXTRACT(EPOCH FROM NOW()),
    'Cleaned up all test data before integration test execution',
    NOW()
);