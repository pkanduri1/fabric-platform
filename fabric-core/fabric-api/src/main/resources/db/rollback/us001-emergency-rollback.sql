-- =========================================================================
-- US001 EMERGENCY ROLLBACK SCRIPT
-- =========================================================================
-- Purpose: Emergency manual rollback of all US001 database changes
-- Author: Senior Full Stack Developer Agent
-- Date: 2025-08-13
-- Version: 1.0
-- Classification: INTERNAL - BANKING CONFIDENTIAL
-- 
-- CRITICAL WARNING: 
-- This script will completely remove all US001 database objects and data.
-- Only execute this script in emergency situations with proper authorization.
-- Ensure database backup is verified before execution.
-- =========================================================================

SET ECHO ON;
SET FEEDBACK ON;
SET TIMING ON;
SET SERVEROUTPUT ON SIZE 1000000;

WHENEVER SQLERROR EXIT SQL.SQLCODE;

-- Create rollback log for audit purposes
SPOOL us001_emergency_rollback.log;

PROMPT =========================================================================;
PROMPT EMERGENCY ROLLBACK STARTING - US001 MANUAL JOB CONFIGURATION INTERFACE
PROMPT =========================================================================;
PROMPT Timestamp: &_DATE &_TIME;
PROMPT User: &_USER;
PROMPT Database: &_CONNECT_IDENTIFIER;
PROMPT =========================================================================;

-- Step 1: Stop any running processes that might interfere
PROMPT Step 1: Checking for active sessions using US001 tables...;

SELECT 
    s.sid,
    s.serial#,
    s.username,
    s.program,
    o.object_name
FROM v$session s, v$locked_object lo, dba_objects o
WHERE s.sid = lo.session_id
AND lo.object_id = o.object_id
AND o.object_name IN ('MANUAL_JOB_CONFIG', 'MANUAL_JOB_EXECUTION', 'JOB_PARAMETER_TEMPLATES', 'MANUAL_JOB_AUDIT');

-- Step 2: Log current state before rollback
PROMPT Step 2: Documenting current state...;

CREATE TABLE ROLLBACK_LOG_US001 (
    log_id NUMBER PRIMARY KEY,
    table_name VARCHAR2(100),
    record_count NUMBER,
    log_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DECLARE
    v_count NUMBER;
    v_log_id NUMBER := 1;
BEGIN
    -- Log current record counts
    BEGIN
        SELECT COUNT(*) INTO v_count FROM FABRIC_CORE.MANUAL_JOB_CONFIG;
        INSERT INTO ROLLBACK_LOG_US001 VALUES (v_log_id, 'MANUAL_JOB_CONFIG', v_count, CURRENT_TIMESTAMP);
        v_log_id := v_log_id + 1;
        DBMS_OUTPUT.PUT_LINE('MANUAL_JOB_CONFIG records: ' || v_count);
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('MANUAL_JOB_CONFIG table not found or accessible');
    END;
    
    BEGIN
        SELECT COUNT(*) INTO v_count FROM FABRIC_CORE.MANUAL_JOB_EXECUTION;
        INSERT INTO ROLLBACK_LOG_US001 VALUES (v_log_id, 'MANUAL_JOB_EXECUTION', v_count, CURRENT_TIMESTAMP);
        v_log_id := v_log_id + 1;
        DBMS_OUTPUT.PUT_LINE('MANUAL_JOB_EXECUTION records: ' || v_count);
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('MANUAL_JOB_EXECUTION table not found or accessible');
    END;
    
    BEGIN
        SELECT COUNT(*) INTO v_count FROM FABRIC_CORE.JOB_PARAMETER_TEMPLATES;
        INSERT INTO ROLLBACK_LOG_US001 VALUES (v_log_id, 'JOB_PARAMETER_TEMPLATES', v_count, CURRENT_TIMESTAMP);
        v_log_id := v_log_id + 1;
        DBMS_OUTPUT.PUT_LINE('JOB_PARAMETER_TEMPLATES records: ' || v_count);
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('JOB_PARAMETER_TEMPLATES table not found or accessible');
    END;
    
    BEGIN
        SELECT COUNT(*) INTO v_count FROM FABRIC_CORE.MANUAL_JOB_AUDIT;
        INSERT INTO ROLLBACK_LOG_US001 VALUES (v_log_id, 'MANUAL_JOB_AUDIT', v_count, CURRENT_TIMESTAMP);
        v_log_id := v_log_id + 1;
        DBMS_OUTPUT.PUT_LINE('MANUAL_JOB_AUDIT records: ' || v_count);
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('MANUAL_JOB_AUDIT table not found or accessible');
    END;
    
    COMMIT;
END;
/

-- Step 3: Drop triggers first to prevent constraint violations
PROMPT Step 3: Dropping triggers...;

BEGIN
    EXECUTE IMMEDIATE 'DROP TRIGGER FABRIC_CORE.TRG_MANUAL_JOB_CONFIG_AUDIT';
    DBMS_OUTPUT.PUT_LINE('Dropped trigger: TRG_MANUAL_JOB_CONFIG_AUDIT');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop TRG_MANUAL_JOB_CONFIG_AUDIT - ' || SQLERRM);
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TRIGGER FABRIC_CORE.TRG_MANUAL_JOB_AUDIT_IMMUTABLE';
    DBMS_OUTPUT.PUT_LINE('Dropped trigger: TRG_MANUAL_JOB_AUDIT_IMMUTABLE');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop TRG_MANUAL_JOB_AUDIT_IMMUTABLE - ' || SQLERRM);
END;
/

-- Step 4: Drop indexes for performance (optional but recommended)
PROMPT Step 4: Dropping indexes...;

DECLARE
    CURSOR idx_cursor IS
        SELECT index_name 
        FROM user_indexes 
        WHERE table_name IN ('MANUAL_JOB_CONFIG', 'MANUAL_JOB_EXECUTION', 'JOB_PARAMETER_TEMPLATES', 'MANUAL_JOB_AUDIT')
        AND index_name NOT IN (
            SELECT constraint_name FROM user_constraints WHERE constraint_type = 'P'
        );
BEGIN
    FOR idx_rec IN idx_cursor LOOP
        BEGIN
            EXECUTE IMMEDIATE 'DROP INDEX FABRIC_CORE.' || idx_rec.index_name;
            DBMS_OUTPUT.PUT_LINE('Dropped index: ' || idx_rec.index_name);
        EXCEPTION
            WHEN OTHERS THEN
                DBMS_OUTPUT.PUT_LINE('Warning: Could not drop index ' || idx_rec.index_name || ' - ' || SQLERRM);
        END;
    END LOOP;
END;
/

-- Step 5: Drop foreign key constraints
PROMPT Step 5: Dropping foreign key constraints...;

DECLARE
    CURSOR fk_cursor IS
        SELECT constraint_name, table_name
        FROM user_constraints
        WHERE constraint_type = 'R'
        AND (r_constraint_name IN (
            SELECT constraint_name FROM user_constraints 
            WHERE table_name IN ('MANUAL_JOB_CONFIG', 'MANUAL_JOB_EXECUTION', 'JOB_PARAMETER_TEMPLATES', 'MANUAL_JOB_AUDIT')
            AND constraint_type = 'P'
        ) OR table_name IN ('MANUAL_JOB_CONFIG', 'MANUAL_JOB_EXECUTION', 'JOB_PARAMETER_TEMPLATES', 'MANUAL_JOB_AUDIT'));
BEGIN
    FOR fk_rec IN fk_cursor LOOP
        BEGIN
            EXECUTE IMMEDIATE 'ALTER TABLE FABRIC_CORE.' || fk_rec.table_name || ' DROP CONSTRAINT ' || fk_rec.constraint_name;
            DBMS_OUTPUT.PUT_LINE('Dropped foreign key: ' || fk_rec.constraint_name || ' from ' || fk_rec.table_name);
        EXCEPTION
            WHEN OTHERS THEN
                DBMS_OUTPUT.PUT_LINE('Warning: Could not drop constraint ' || fk_rec.constraint_name || ' - ' || SQLERRM);
        END;
    END LOOP;
END;
/

-- Step 6: Drop tables in reverse dependency order
PROMPT Step 6: Dropping tables...;

-- Drop audit table first (highest dependency)
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE FABRIC_CORE.MANUAL_JOB_AUDIT CASCADE CONSTRAINTS PURGE';
    DBMS_OUTPUT.PUT_LINE('Dropped table: MANUAL_JOB_AUDIT');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop MANUAL_JOB_AUDIT - ' || SQLERRM);
END;
/

-- Drop execution table
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE FABRIC_CORE.MANUAL_JOB_EXECUTION CASCADE CONSTRAINTS PURGE';
    DBMS_OUTPUT.PUT_LINE('Dropped table: MANUAL_JOB_EXECUTION');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop MANUAL_JOB_EXECUTION - ' || SQLERRM);
END;
/

-- Drop templates table (independent)
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE FABRIC_CORE.JOB_PARAMETER_TEMPLATES CASCADE CONSTRAINTS PURGE';
    DBMS_OUTPUT.PUT_LINE('Dropped table: JOB_PARAMETER_TEMPLATES');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop JOB_PARAMETER_TEMPLATES - ' || SQLERRM);
END;
/

-- Drop main configuration table (base dependency)
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE FABRIC_CORE.MANUAL_JOB_CONFIG CASCADE CONSTRAINTS PURGE';
    DBMS_OUTPUT.PUT_LINE('Dropped table: MANUAL_JOB_CONFIG');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop MANUAL_JOB_CONFIG - ' || SQLERRM);
END;
/

-- Drop system error log table
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE FABRIC_CORE.SYSTEM_ERROR_LOG CASCADE CONSTRAINTS PURGE';
    DBMS_OUTPUT.PUT_LINE('Dropped table: SYSTEM_ERROR_LOG');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop SYSTEM_ERROR_LOG - ' || SQLERRM);
END;
/

-- Step 7: Drop stored procedures and functions
PROMPT Step 7: Dropping stored procedures and functions...;

BEGIN
    EXECUTE IMMEDIATE 'DROP PROCEDURE FABRIC_CORE.COLLECT_US001_STATISTICS';
    DBMS_OUTPUT.PUT_LINE('Dropped procedure: COLLECT_US001_STATISTICS');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop COLLECT_US001_STATISTICS - ' || SQLERRM);
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP PROCEDURE FABRIC_CORE.UPDATE_TEMPLATE_USAGE';
    DBMS_OUTPUT.PUT_LINE('Dropped procedure: UPDATE_TEMPLATE_USAGE');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop UPDATE_TEMPLATE_USAGE - ' || SQLERRM);
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP FUNCTION FABRIC_CORE.MASK_SENSITIVE_DATA';
    DBMS_OUTPUT.PUT_LINE('Dropped function: MASK_SENSITIVE_DATA');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop MASK_SENSITIVE_DATA - ' || SQLERRM);
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP FUNCTION FABRIC_CORE.GET_USER_ENVIRONMENT';
    DBMS_OUTPUT.PUT_LINE('Dropped function: GET_USER_ENVIRONMENT');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop GET_USER_ENVIRONMENT - ' || SQLERRM);
END;
/

-- Step 8: Drop views
PROMPT Step 8: Dropping views...;

BEGIN
    EXECUTE IMMEDIATE 'DROP VIEW FABRIC_CORE.MANUAL_JOB_CONFIG_MASKED';
    DBMS_OUTPUT.PUT_LINE('Dropped view: MANUAL_JOB_CONFIG_MASKED');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not drop MANUAL_JOB_CONFIG_MASKED - ' || SQLERRM);
END;
/

-- Step 9: Clean up Liquibase changelog entries
PROMPT Step 9: Cleaning up Liquibase changelog...;

DECLARE
    v_count NUMBER;
BEGIN
    -- Remove US001 changesets from Liquibase changelog
    DELETE FROM DATABASECHANGELOG WHERE ID LIKE 'us001-%';
    v_count := SQL%ROWCOUNT;
    DBMS_OUTPUT.PUT_LINE('Removed ' || v_count || ' US001 changesets from DATABASECHANGELOG');
    
    -- Remove US001 tag
    DELETE FROM DATABASECHANGELOG WHERE TAG = 'US001_MANUAL_JOB_CONFIG_v1.0';
    v_count := SQL%ROWCOUNT;
    DBMS_OUTPUT.PUT_LINE('Removed ' || v_count || ' US001 tag entries from DATABASECHANGELOG');
    
    COMMIT;
END;
/

-- Step 10: Verification
PROMPT Step 10: Verifying rollback completion...;

DECLARE
    v_count NUMBER;
    v_rollback_success BOOLEAN := TRUE;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Verification Results:');
    DBMS_OUTPUT.PUT_LINE('====================');
    
    -- Check for remaining tables
    SELECT COUNT(*) INTO v_count
    FROM user_tables 
    WHERE table_name IN ('MANUAL_JOB_CONFIG', 'MANUAL_JOB_EXECUTION', 'JOB_PARAMETER_TEMPLATES', 'MANUAL_JOB_AUDIT', 'SYSTEM_ERROR_LOG');
    
    DBMS_OUTPUT.PUT_LINE('Remaining US001 tables: ' || v_count);
    IF v_count > 0 THEN
        v_rollback_success := FALSE;
        FOR rec IN (SELECT table_name FROM user_tables 
                    WHERE table_name IN ('MANUAL_JOB_CONFIG', 'MANUAL_JOB_EXECUTION', 'JOB_PARAMETER_TEMPLATES', 'MANUAL_JOB_AUDIT', 'SYSTEM_ERROR_LOG')) LOOP
            DBMS_OUTPUT.PUT_LINE('  - ' || rec.table_name || ' still exists');
        END LOOP;
    END IF;
    
    -- Check for remaining constraints
    SELECT COUNT(*) INTO v_count
    FROM user_constraints
    WHERE constraint_name LIKE '%MANUAL_JOB%' OR constraint_name LIKE '%JOB_TEMPLATE%';
    
    DBMS_OUTPUT.PUT_LINE('Remaining US001 constraints: ' || v_count);
    IF v_count > 0 THEN
        v_rollback_success := FALSE;
    END IF;
    
    -- Check for remaining indexes
    SELECT COUNT(*) INTO v_count
    FROM user_indexes
    WHERE index_name LIKE '%MANUAL_JOB%' OR index_name LIKE '%JOB_TEMPLATE%';
    
    DBMS_OUTPUT.PUT_LINE('Remaining US001 indexes: ' || v_count);
    IF v_count > 0 THEN
        v_rollback_success := FALSE;
    END IF;
    
    -- Check for remaining triggers
    SELECT COUNT(*) INTO v_count
    FROM user_triggers
    WHERE trigger_name LIKE '%MANUAL_JOB%';
    
    DBMS_OUTPUT.PUT_LINE('Remaining US001 triggers: ' || v_count);
    IF v_count > 0 THEN
        v_rollback_success := FALSE;
    END IF;
    
    -- Check for remaining changelog entries
    SELECT COUNT(*) INTO v_count
    FROM DATABASECHANGELOG
    WHERE ID LIKE 'us001-%';
    
    DBMS_OUTPUT.PUT_LINE('Remaining US001 changelog entries: ' || v_count);
    IF v_count > 0 THEN
        v_rollback_success := FALSE;
    END IF;
    
    DBMS_OUTPUT.PUT_LINE('====================');
    
    IF v_rollback_success THEN
        DBMS_OUTPUT.PUT_LINE('ROLLBACK COMPLETED SUCCESSFULLY');
        DBMS_OUTPUT.PUT_LINE('All US001 database objects have been removed');
    ELSE
        DBMS_OUTPUT.PUT_LINE('ROLLBACK INCOMPLETE - MANUAL CLEANUP REQUIRED');
        DBMS_OUTPUT.PUT_LINE('Some US001 objects remain in the database');
    END IF;
END;
/

-- Step 11: Create rollback completion audit record
PROMPT Step 11: Creating audit record...;

INSERT INTO ROLLBACK_LOG_US001 (log_id, table_name, record_count, log_timestamp)
VALUES (999, 'ROLLBACK_COMPLETION', 1, CURRENT_TIMESTAMP);

COMMIT;

PROMPT =========================================================================;
PROMPT EMERGENCY ROLLBACK COMPLETED - US001 MANUAL JOB CONFIGURATION INTERFACE
PROMPT =========================================================================;
PROMPT Completion Time: &_DATE &_TIME;
PROMPT 
PROMPT IMPORTANT NOTES:
PROMPT 1. Review the rollback log above for any warnings or errors
PROMPT 2. Verify application functionality after rollback
PROMPT 3. Check for any dependent systems that may be affected
PROMPT 4. Update change management systems with rollback completion
PROMPT 5. Notify all stakeholders of rollback completion
PROMPT 
PROMPT Rollback log table ROLLBACK_LOG_US001 contains pre-rollback state information
PROMPT This table should be preserved for audit and recovery purposes
PROMPT =========================================================================;

SPOOL OFF;

-- Final commit
COMMIT;

-- Re-enable error handling
WHENEVER SQLERROR CONTINUE;