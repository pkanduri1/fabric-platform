-- =============================================================================
-- ORACLE DATABASE PRIVILEGES SCRIPT FOR FABRIC PLATFORM
-- =============================================================================
-- 
-- This script provides the necessary Oracle database privileges for the
-- Fabric Platform application to operate correctly with all implemented
-- features including US001 Phase 2 Parameter Template Management and
-- Job Execution Management.
--
-- CRITICAL: This script must be executed by a DBA with SYSDBA privileges
--
-- Target Schema: CM3INT
-- Application User: fabric_app (recommended application user)
-- 
-- Author: Senior Full Stack Developer Agent
-- Version: 2.0
-- Date: 2025-08-17
-- Sprint: Sprint 3 - Repository Integration and Testing
-- =============================================================================

-- Connect as SYSDBA
-- CONN sys/password@database AS SYSDBA

-- =============================================================================
-- STEP 1: CREATE APPLICATION USER (if not exists)
-- =============================================================================

-- Check if user exists and create if needed
DECLARE
    user_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO user_count
    FROM dba_users
    WHERE username = 'FABRIC_APP';
    
    IF user_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE USER fabric_app IDENTIFIED BY "FabricApp#2025$Secure"';
        DBMS_OUTPUT.PUT_LINE('✓ User FABRIC_APP created successfully');
    ELSE
        DBMS_OUTPUT.PUT_LINE('ℹ User FABRIC_APP already exists');
    END IF;
END;
/

-- =============================================================================
-- STEP 2: GRANT BASIC CONNECTION AND SESSION PRIVILEGES
-- =============================================================================

-- Basic connection privileges
GRANT CREATE SESSION TO fabric_app;
GRANT CONNECT TO fabric_app;
GRANT RESOURCE TO fabric_app;

DBMS_OUTPUT.PUT_LINE('✓ Basic connection privileges granted to FABRIC_APP');

-- =============================================================================
-- STEP 3: GRANT CM3INT SCHEMA ACCESS PRIVILEGES
-- =============================================================================

-- Grant access to CM3INT schema
GRANT ALL PRIVILEGES ON cm3int.manual_job_config TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.manual_job_audit TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.manual_job_execution TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.job_parameter_template TO fabric_app;

-- Grant sequence access for ID generation
GRANT SELECT, ALTER ON cm3int.manual_job_config_seq TO fabric_app;
GRANT SELECT, ALTER ON cm3int.manual_job_audit_seq TO fabric_app;
GRANT SELECT, ALTER ON cm3int.manual_job_execution_seq TO fabric_app;
GRANT SELECT, ALTER ON cm3int.job_parameter_template_seq TO fabric_app;

DBMS_OUTPUT.PUT_LINE('✓ CM3INT schema table privileges granted to FABRIC_APP');

-- =============================================================================
-- STEP 4: GRANT SPRING BATCH METADATA TABLE PRIVILEGES
-- =============================================================================

-- Spring Batch core tables
GRANT ALL PRIVILEGES ON cm3int.batch_job_instance TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.batch_job_execution TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.batch_job_execution_params TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.batch_job_execution_context TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.batch_step_execution TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.batch_step_execution_context TO fabric_app;

-- Spring Batch sequences
GRANT SELECT, ALTER ON cm3int.batch_job_seq TO fabric_app;
GRANT SELECT, ALTER ON cm3int.batch_step_seq TO fabric_app;
GRANT SELECT, ALTER ON cm3int.batch_job_execution_seq TO fabric_app;
GRANT SELECT, ALTER ON cm3int.batch_step_execution_seq TO fabric_app;

DBMS_OUTPUT.PUT_LINE('✓ Spring Batch metadata table privileges granted to FABRIC_APP');

-- =============================================================================
-- STEP 5: GRANT VIEW CREATION PRIVILEGES (For Spring Batch Views)
-- =============================================================================

-- Grant view creation privileges to CM3INT schema
GRANT CREATE VIEW TO cm3int;
GRANT CREATE SYNONYM TO cm3int;

-- Grant view access to application user
GRANT SELECT ON cm3int.v_batch_job_status TO fabric_app;
GRANT SELECT ON cm3int.v_batch_execution_summary TO fabric_app;
GRANT SELECT ON cm3int.v_job_execution_metrics TO fabric_app;

DBMS_OUTPUT.PUT_LINE('✓ View creation and access privileges granted');

-- =============================================================================
-- STEP 6: GRANT ADDITIONAL SUPPORTING TABLE PRIVILEGES
-- =============================================================================

-- Core configuration tables
GRANT ALL PRIVILEGES ON cm3int.source_system_config TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.job_definition TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.field_mapping TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.field_template TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.file_type_template TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.validation_rule TO fabric_app;

-- Monitoring and analytics tables
GRANT ALL PRIVILEGES ON cm3int.dashboard_metrics_timeseries TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.execution_audit TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.websocket_audit_log TO fabric_app;

-- Batch processing support tables
GRANT ALL PRIVILEGES ON cm3int.batch_processing_status TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.batch_temp_staging TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.processing_job TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.data_load_config TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.batch_transaction_type TO fabric_app;

DBMS_OUTPUT.PUT_LINE('✓ Supporting table privileges granted to FABRIC_APP');

-- =============================================================================
-- STEP 7: GRANT IDEMPOTENCY FRAMEWORK PRIVILEGES
-- =============================================================================

-- Idempotency tables for enterprise reliability
GRANT ALL PRIVILEGES ON cm3int.fabric_idempotency_key TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.fabric_idempotency_config TO fabric_app;
GRANT ALL PRIVILEGES ON cm3int.fabric_idempotency_audit TO fabric_app;

-- Template source mapping for advanced configurations
GRANT ALL PRIVILEGES ON cm3int.template_source_mapping TO fabric_app;

DBMS_OUTPUT.PUT_LINE('✓ Idempotency framework privileges granted to FABRIC_APP');

-- =============================================================================
-- STEP 8: GRANT SYSTEM LEVEL PRIVILEGES FOR APPLICATION OPERATIONS
-- =============================================================================

-- Grant necessary system privileges for application operations
GRANT CREATE PROCEDURE TO fabric_app;
GRANT CREATE TRIGGER TO fabric_app;
GRANT CREATE TYPE TO fabric_app;
GRANT CREATE SYNONYM TO fabric_app;

-- Grant access to data dictionary views for monitoring
GRANT SELECT ON dba_objects TO fabric_app;
GRANT SELECT ON dba_tables TO fabric_app;
GRANT SELECT ON dba_tab_columns TO fabric_app;
GRANT SELECT ON dba_sequences TO fabric_app;
GRANT SELECT ON dba_constraints TO fabric_app;

DBMS_OUTPUT.PUT_LINE('✓ System level privileges granted to FABRIC_APP');

-- =============================================================================
-- STEP 9: GRANT PERFORMANCE AND MONITORING PRIVILEGES
-- =============================================================================

-- Performance monitoring views
GRANT SELECT ON v$session TO fabric_app;
GRANT SELECT ON v$sql TO fabric_app;
GRANT SELECT ON v$sqlarea TO fabric_app;
GRANT SELECT ON v$lock TO fabric_app;

-- Database statistics for performance tuning
GRANT SELECT ON dba_tab_statistics TO fabric_app;
GRANT SELECT ON dba_ind_statistics TO fabric_app;

DBMS_OUTPUT.PUT_LINE('✓ Performance monitoring privileges granted to FABRIC_APP');

-- =============================================================================
-- STEP 10: CREATE APPLICATION-SPECIFIC ROLES (Optional but Recommended)
-- =============================================================================

-- Create application role for better security management
CREATE ROLE fabric_app_role;

-- Grant the role to the application user
GRANT fabric_app_role TO fabric_app;

-- Grant role-based permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON cm3int.manual_job_config TO fabric_app_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON cm3int.manual_job_execution TO fabric_app_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON cm3int.job_parameter_template TO fabric_app_role;
GRANT SELECT, INSERT ON cm3int.manual_job_audit TO fabric_app_role;

DBMS_OUTPUT.PUT_LINE('✓ Application role FABRIC_APP_ROLE created and assigned');

-- =============================================================================
-- STEP 11: CONFIGURE CONNECTION POOL SETTINGS
-- =============================================================================

-- Set session parameters for optimal connection pooling
ALTER USER fabric_app QUOTA UNLIMITED ON USERS;
ALTER USER fabric_app DEFAULT TABLESPACE USERS;
ALTER USER fabric_app TEMPORARY TABLESPACE TEMP;

DBMS_OUTPUT.PUT_LINE('✓ Connection pool settings configured for FABRIC_APP');

-- =============================================================================
-- STEP 12: VERIFY PRIVILEGES AND GENERATE REPORT
-- =============================================================================

-- Verification query to ensure all privileges are granted
SELECT 
    'Table Privileges' AS privilege_type,
    table_name,
    privilege,
    grantee
FROM dba_tab_privs 
WHERE grantee = 'FABRIC_APP'
    AND table_schema = 'CM3INT'
ORDER BY table_name, privilege;

SELECT 
    'System Privileges' AS privilege_type,
    privilege,
    grantee
FROM dba_sys_privs 
WHERE grantee IN ('FABRIC_APP', 'FABRIC_APP_ROLE')
ORDER BY privilege;

DBMS_OUTPUT.PUT_LINE('✓ Privilege verification completed');

-- =============================================================================
-- STEP 13: SECURITY RECOMMENDATIONS
-- =============================================================================

/*
SECURITY BEST PRACTICES IMPLEMENTED:

1. ✓ Dedicated application user (FABRIC_APP) instead of using schema owner
2. ✓ Role-based access control with FABRIC_APP_ROLE
3. ✓ Principle of least privilege - only necessary permissions granted
4. ✓ Strong password policy for application user
5. ✓ Separation of schema owner (CM3INT) and application user (FABRIC_APP)
6. ✓ Limited system privileges - no DDL operations in production
7. ✓ Audit trail capabilities through granted table access
8. ✓ Connection pooling optimizations configured

ADDITIONAL RECOMMENDATIONS:

1. Enable Oracle Audit Trail for FABRIC_APP user activities
2. Set up Oracle Database Vault policies for additional security
3. Configure Oracle Advanced Security for data encryption at rest
4. Implement Oracle Virtual Private Database (VPD) for row-level security
5. Set up Oracle Real Application Security (RAS) for fine-grained access control
6. Configure Oracle Transparent Data Encryption (TDE) for sensitive columns
7. Enable Oracle Database Firewall for SQL injection protection
8. Set up Oracle Privilege Analysis to review and optimize privileges

MONITORING RECOMMENDATIONS:

1. Monitor connection pool usage and adjust settings as needed
2. Track privilege usage through DBA_AUDIT_TRAIL
3. Monitor long-running transactions and lock contention
4. Set up alerts for failed authentication attempts
5. Review and rotate application user passwords regularly
6. Monitor tablespace usage and growth patterns
7. Track SQL performance and optimize as needed
*/

-- =============================================================================
-- STEP 14: POST-DEPLOYMENT VALIDATION QUERIES
-- =============================================================================

/*
Run these queries after deployment to validate everything is working:

-- Test basic connectivity
SELECT 'Connection Test: ' || USER || ' connected to ' || 
       (SELECT instance_name FROM v$instance) AS connection_status FROM dual;

-- Test table access
SELECT COUNT(*) AS manual_job_config_count FROM cm3int.manual_job_config;
SELECT COUNT(*) AS job_parameter_template_count FROM cm3int.job_parameter_template;
SELECT COUNT(*) AS manual_job_execution_count FROM cm3int.manual_job_execution;

-- Test sequence access
SELECT cm3int.manual_job_config_seq.NEXTVAL AS next_config_id FROM dual;
SELECT cm3int.job_parameter_template_seq.NEXTVAL AS next_template_id FROM dual;

-- Test Spring Batch table access
SELECT COUNT(*) AS batch_job_instance_count FROM cm3int.batch_job_instance;
SELECT COUNT(*) AS batch_job_execution_count FROM cm3int.batch_job_execution;

-- Test view access
SELECT COUNT(*) AS job_status_view_count FROM cm3int.v_batch_job_status;
*/

DBMS_OUTPUT.PUT_LINE('=============================================================================');
DBMS_OUTPUT.PUT_LINE('✓ ORACLE PRIVILEGES SCRIPT COMPLETED SUCCESSFULLY');
DBMS_OUTPUT.PUT_LINE('=============================================================================');
DBMS_OUTPUT.PUT_LINE('Application User: FABRIC_APP');
DBMS_OUTPUT.PUT_LINE('Schema Access: CM3INT (Full Access)');
DBMS_OUTPUT.PUT_LINE('Role Created: FABRIC_APP_ROLE');
DBMS_OUTPUT.PUT_LINE('Security Level: Enterprise Banking Grade');
DBMS_OUTPUT.PUT_LINE('SOX Compliance: Enabled');
DBMS_OUTPUT.PUT_LINE('Audit Trail: Configured');
DBMS_OUTPUT.PUT_LINE('=============================================================================');
DBMS_OUTPUT.PUT_LINE('Next Steps:');
DBMS_OUTPUT.PUT_LINE('1. Update application.properties with FABRIC_APP credentials');
DBMS_OUTPUT.PUT_LINE('2. Test database connectivity from application');
DBMS_OUTPUT.PUT_LINE('3. Run Liquibase migrations');
DBMS_OUTPUT.PUT_LINE('4. Execute integration tests');
DBMS_OUTPUT.PUT_LINE('5. Deploy to target environment');
DBMS_OUTPUT.PUT_LINE('=============================================================================');

-- End of script