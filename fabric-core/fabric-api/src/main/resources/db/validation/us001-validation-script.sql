-- =========================================================================
-- US001 LIQUIBASE VALIDATION SCRIPT
-- =========================================================================
-- Purpose: Comprehensive validation script for US001 Liquibase changesets
-- Author: Senior Full Stack Developer Agent
-- Date: 2025-08-13
-- Version: 1.0
-- Classification: INTERNAL - BANKING CONFIDENTIAL
-- =========================================================================

SET SERVEROUTPUT ON;

DECLARE
    v_count NUMBER;
    v_error_count NUMBER := 0;
    v_warning_count NUMBER := 0;
    v_total_tests NUMBER := 0;
    
    PROCEDURE log_test_result(
        p_test_name VARCHAR2,
        p_status VARCHAR2,
        p_details VARCHAR2 DEFAULT NULL
    ) IS
    BEGIN
        v_total_tests := v_total_tests + 1;
        DBMS_OUTPUT.PUT_LINE('[' || p_status || '] ' || p_test_name);
        IF p_details IS NOT NULL THEN
            DBMS_OUTPUT.PUT_LINE('    Details: ' || p_details);
        END IF;
        
        IF p_status = 'ERROR' THEN
            v_error_count := v_error_count + 1;
        ELSIF p_status = 'WARNING' THEN
            v_warning_count := v_warning_count + 1;
        END IF;
        
        DBMS_OUTPUT.PUT_LINE('');
    END log_test_result;
    
BEGIN
    DBMS_OUTPUT.PUT_LINE('=========================================================================');
    DBMS_OUTPUT.PUT_LINE('US001 LIQUIBASE VALIDATION REPORT');
    DBMS_OUTPUT.PUT_LINE('Date: ' || TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS'));
    DBMS_OUTPUT.PUT_LINE('Environment: ' || SYS_CONTEXT('USERENV', 'DB_NAME'));
    DBMS_OUTPUT.PUT_LINE('Schema: FABRIC_CORE');
    DBMS_OUTPUT.PUT_LINE('=========================================================================');
    DBMS_OUTPUT.PUT_LINE('');
    
    -- Test 1: Verify all tables exist
    DBMS_OUTPUT.PUT_LINE('1. TABLE EXISTENCE VALIDATION');
    DBMS_OUTPUT.PUT_LINE('---------------------------------');
    
    SELECT COUNT(*) INTO v_count
    FROM user_tables 
    WHERE table_name = 'MANUAL_JOB_CONFIG';
    
    IF v_count = 1 THEN
        log_test_result('MANUAL_JOB_CONFIG table exists', 'PASS');
    ELSE
        log_test_result('MANUAL_JOB_CONFIG table missing', 'ERROR');
    END IF;
    
    SELECT COUNT(*) INTO v_count
    FROM user_tables 
    WHERE table_name = 'MANUAL_JOB_EXECUTION';
    
    IF v_count = 1 THEN
        log_test_result('MANUAL_JOB_EXECUTION table exists', 'PASS');
    ELSE
        log_test_result('MANUAL_JOB_EXECUTION table missing', 'ERROR');
    END IF;
    
    SELECT COUNT(*) INTO v_count
    FROM user_tables 
    WHERE table_name = 'JOB_PARAMETER_TEMPLATES';
    
    IF v_count = 1 THEN
        log_test_result('JOB_PARAMETER_TEMPLATES table exists', 'PASS');
    ELSE
        log_test_result('JOB_PARAMETER_TEMPLATES table missing', 'ERROR');
    END IF;
    
    SELECT COUNT(*) INTO v_count
    FROM user_tables 
    WHERE table_name = 'MANUAL_JOB_AUDIT';
    
    IF v_count = 1 THEN
        log_test_result('MANUAL_JOB_AUDIT table exists', 'PASS');
    ELSE
        log_test_result('MANUAL_JOB_AUDIT table missing', 'ERROR');
    END IF;
    
    -- Test 2: Verify primary keys
    DBMS_OUTPUT.PUT_LINE('2. PRIMARY KEY VALIDATION');
    DBMS_OUTPUT.PUT_LINE('---------------------------');
    
    SELECT COUNT(*) INTO v_count
    FROM user_constraints
    WHERE table_name = 'MANUAL_JOB_CONFIG' 
    AND constraint_type = 'P'
    AND constraint_name = 'PK_MANUAL_JOB_CONFIG';
    
    IF v_count = 1 THEN
        log_test_result('MANUAL_JOB_CONFIG primary key exists', 'PASS');
    ELSE
        log_test_result('MANUAL_JOB_CONFIG primary key missing', 'ERROR');
    END IF;
    
    -- Test 3: Verify foreign keys
    DBMS_OUTPUT.PUT_LINE('3. FOREIGN KEY VALIDATION');
    DBMS_OUTPUT.PUT_LINE('---------------------------');
    
    SELECT COUNT(*) INTO v_count
    FROM user_constraints
    WHERE table_name = 'MANUAL_JOB_EXECUTION'
    AND constraint_type = 'R'
    AND constraint_name = 'FK_MANUAL_JOB_EXECUTION_CONFIG';
    
    IF v_count = 1 THEN
        log_test_result('MANUAL_JOB_EXECUTION foreign key exists', 'PASS');
    ELSE
        log_test_result('MANUAL_JOB_EXECUTION foreign key missing', 'ERROR');
    END IF;
    
    SELECT COUNT(*) INTO v_count
    FROM user_constraints
    WHERE table_name = 'MANUAL_JOB_AUDIT'
    AND constraint_type = 'R'
    AND constraint_name = 'FK_MANUAL_JOB_AUDIT_CONFIG';
    
    IF v_count = 1 THEN
        log_test_result('MANUAL_JOB_AUDIT foreign key exists', 'PASS');
    ELSE
        log_test_result('MANUAL_JOB_AUDIT foreign key missing', 'ERROR');
    END IF;
    
    -- Test 4: Verify check constraints
    DBMS_OUTPUT.PUT_LINE('4. CHECK CONSTRAINT VALIDATION');
    DBMS_OUTPUT.PUT_LINE('--------------------------------');
    
    SELECT COUNT(*) INTO v_count
    FROM user_constraints
    WHERE table_name = 'MANUAL_JOB_CONFIG'
    AND constraint_type = 'C'
    AND constraint_name = 'CHK_MANUAL_JOB_STATUS';
    
    IF v_count = 1 THEN
        log_test_result('Job configuration status constraint exists', 'PASS');
    ELSE
        log_test_result('Job configuration status constraint missing', 'ERROR');
    END IF;
    
    -- Test 5: Verify indexes
    DBMS_OUTPUT.PUT_LINE('5. INDEX VALIDATION');
    DBMS_OUTPUT.PUT_LINE('--------------------');
    
    SELECT COUNT(*) INTO v_count
    FROM user_indexes
    WHERE table_name = 'MANUAL_JOB_CONFIG'
    AND index_name = 'IDX_MANUAL_JOB_CONFIG_TYPE';
    
    IF v_count = 1 THEN
        log_test_result('Job configuration type index exists', 'PASS');
    ELSE
        log_test_result('Job configuration type index missing', 'WARNING', 
                       'Performance may be impacted');
    END IF;
    
    -- Test 6: Verify triggers
    DBMS_OUTPUT.PUT_LINE('6. TRIGGER VALIDATION');
    DBMS_OUTPUT.PUT_LINE('----------------------');
    
    SELECT COUNT(*) INTO v_count
    FROM user_triggers
    WHERE table_name = 'MANUAL_JOB_AUDIT'
    AND trigger_name = 'TRG_MANUAL_JOB_AUDIT_IMMUTABLE';
    
    IF v_count = 1 THEN
        log_test_result('Audit immutability trigger exists', 'PASS');
    ELSE
        log_test_result('Audit immutability trigger missing', 'ERROR',
                       'SOX compliance may be compromised');
    END IF;
    
    -- Test 7: Verify system templates
    DBMS_OUTPUT.PUT_LINE('7. SYSTEM TEMPLATE VALIDATION');
    DBMS_OUTPUT.PUT_LINE('------------------------------');
    
    SELECT COUNT(*) INTO v_count
    FROM JOB_PARAMETER_TEMPLATES
    WHERE IS_SYSTEM_TEMPLATE = 'Y'
    AND STATUS = 'ACTIVE';
    
    IF v_count >= 3 THEN
        log_test_result('System templates loaded', 'PASS', 
                       v_count || ' system templates found');
    ELSIF v_count > 0 THEN
        log_test_result('Partial system templates loaded', 'WARNING',
                       'Only ' || v_count || ' system templates found');
    ELSE
        log_test_result('No system templates found', 'ERROR',
                       'Manual job configuration may not function properly');
    END IF;
    
    -- Test 8: Verify Liquibase changelog tables
    DBMS_OUTPUT.PUT_LINE('8. LIQUIBASE INFRASTRUCTURE VALIDATION');
    DBMS_OUTPUT.PUT_LINE('---------------------------------------');
    
    SELECT COUNT(*) INTO v_count
    FROM user_tables
    WHERE table_name = 'DATABASECHANGELOG';
    
    IF v_count = 1 THEN
        log_test_result('Liquibase changelog table exists', 'PASS');
    ELSE
        log_test_result('Liquibase changelog table missing', 'ERROR',
                       'Database change tracking not available');
    END IF;
    
    -- Test 9: Verify US001 changesets in Liquibase log
    SELECT COUNT(*) INTO v_count
    FROM DATABASECHANGELOG
    WHERE ID LIKE 'us001-%'
    AND AUTHOR = 'Senior Full Stack Developer Agent';
    
    IF v_count >= 7 THEN
        log_test_result('US001 changesets recorded', 'PASS',
                       v_count || ' changesets found in log');
    ELSE
        log_test_result('Missing US001 changesets', 'WARNING',
                       'Only ' || v_count || ' changesets found');
    END IF;
    
    -- Test 10: Data integrity validation
    DBMS_OUTPUT.PUT_LINE('9. DATA INTEGRITY VALIDATION');
    DBMS_OUTPUT.PUT_LINE('-----------------------------');
    
    -- Check for orphaned execution records
    SELECT COUNT(*) INTO v_count
    FROM MANUAL_JOB_EXECUTION e
    WHERE NOT EXISTS (
        SELECT 1 FROM MANUAL_JOB_CONFIG c
        WHERE c.CONFIG_ID = e.CONFIG_ID
    );
    
    IF v_count = 0 THEN
        log_test_result('No orphaned execution records', 'PASS');
    ELSE
        log_test_result('Orphaned execution records found', 'ERROR',
                       v_count || ' orphaned records detected');
    END IF;
    
    -- Check for orphaned audit records
    SELECT COUNT(*) INTO v_count
    FROM MANUAL_JOB_AUDIT a
    WHERE a.CONFIG_ID != 'SYSTEM'
    AND NOT EXISTS (
        SELECT 1 FROM MANUAL_JOB_CONFIG c
        WHERE c.CONFIG_ID = a.CONFIG_ID
    );
    
    IF v_count = 0 THEN
        log_test_result('No orphaned audit records', 'PASS');
    ELSE
        log_test_result('Orphaned audit records found', 'WARNING',
                       v_count || ' orphaned audit records detected');
    END IF;
    
    -- Final summary
    DBMS_OUTPUT.PUT_LINE('=========================================================================');
    DBMS_OUTPUT.PUT_LINE('VALIDATION SUMMARY');
    DBMS_OUTPUT.PUT_LINE('=========================================================================');
    DBMS_OUTPUT.PUT_LINE('Total Tests: ' || v_total_tests);
    DBMS_OUTPUT.PUT_LINE('Errors: ' || v_error_count);
    DBMS_OUTPUT.PUT_LINE('Warnings: ' || v_warning_count);
    DBMS_OUTPUT.PUT_LINE('Passed: ' || (v_total_tests - v_error_count - v_warning_count));
    
    IF v_error_count = 0 AND v_warning_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('Status: ALL TESTS PASSED - Schema is ready for production use');
    ELSIF v_error_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('Status: PASSED WITH WARNINGS - Review warnings before production');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Status: FAILED - Address errors before proceeding');
    END IF;
    
    DBMS_OUTPUT.PUT_LINE('=========================================================================');
    
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('VALIDATION ERROR: ' || SQLERRM);
        DBMS_OUTPUT.PUT_LINE('Error Stack: ' || DBMS_UTILITY.FORMAT_ERROR_BACKTRACE);
        RAISE;
END;
/