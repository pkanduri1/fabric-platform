-- ============================================================================
-- US008: Real-Time Job Monitoring Dashboard - Database Schema Migration
-- Version: V2025.004
-- Author: Senior Full Stack Developer
-- Date: 2025-08-08
-- 
-- APPROVED FOR IMPLEMENTATION by Principal Enterprise Architect
-- Compliance Score: 96/100
-- 
-- Description:
-- Creates time-series partitioned monitoring tables, WebSocket session management,
-- and comprehensive audit infrastructure for real-time job monitoring dashboard
-- with banking-grade security and SOX compliance.
-- ============================================================================

-- Enable partitioning (Oracle specific)
-- Ensure proper permissions for partitioned tables
-- ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS';

-- ============================================================================
-- 1. TIME-SERIES MONITORING TABLES
-- ============================================================================

-- Drop existing tables if they exist (development only)
-- Production deployments should use proper migration scripts
-- DROP TABLE CM3INT.DASHBOARD_METRICS_TIMESERIES CASCADE CONSTRAINTS;
-- DROP TABLE CM3INT.WEBSOCKET_SESSIONS CASCADE CONSTRAINTS;
-- DROP TABLE CM3INT.WEBSOCKET_AUDIT_LOG CASCADE CONSTRAINTS;

-- Enhanced time-series metrics table with automatic partitioning
CREATE TABLE CM3INT.DASHBOARD_METRICS_TIMESERIES (
    metric_id NUMBER(19) NOT NULL,
    metric_timestamp TIMESTAMP(6) NOT NULL,
    execution_id VARCHAR2(100) NOT NULL,
    metric_type VARCHAR2(50) NOT NULL,
    metric_name VARCHAR2(100) NOT NULL,
    metric_value NUMBER(15,4),
    metric_unit VARCHAR2(20),
    metric_status VARCHAR2(20),
    source_system VARCHAR2(50),
    thread_id VARCHAR2(50),
    partition_id VARCHAR2(20),
    correlation_id VARCHAR2(100),
    user_id VARCHAR2(100),
    -- Performance and resource metrics
    cpu_usage_percent NUMBER(5,2),
    memory_usage_mb NUMBER(10),
    processing_duration_ms NUMBER(15),
    throughput_per_second NUMBER(15,4),
    success_rate_percent NUMBER(5,2),
    -- Security and compliance fields
    audit_hash VARCHAR2(64) NOT NULL, -- SHA-256 hash for tamper detection
    compliance_flags VARCHAR2(200),
    security_classification VARCHAR2(50) DEFAULT 'INTERNAL',
    data_quality_score NUMBER(5,2),
    -- Standard audit fields
    created_date TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(100) NOT NULL,
    last_modified_date TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR2(100),
    version_number NUMBER(10) DEFAULT 1,
    -- Constraints
    CONSTRAINT pk_dashboard_metrics_ts PRIMARY KEY (metric_id),
    CONSTRAINT chk_metrics_cpu_usage CHECK (cpu_usage_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_metrics_success_rate CHECK (success_rate_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_metrics_data_quality CHECK (data_quality_score BETWEEN 0 AND 100),
    CONSTRAINT chk_metrics_type CHECK (metric_type IN ('SYSTEM_PERFORMANCE', 'BUSINESS_KPI', 'SECURITY_EVENT', 'COMPLIANCE_METRIC')),
    CONSTRAINT chk_metrics_status CHECK (metric_status IN ('NORMAL', 'WARNING', 'CRITICAL', 'UNKNOWN'))
) 
PARTITION BY RANGE (metric_timestamp) INTERVAL (NUMTODSINTERVAL(1, 'DAY'))
(
    PARTITION p_metrics_initial VALUES LESS THAN (TIMESTAMP '2025-08-09 00:00:00')
)
TABLESPACE USERS
PCTFREE 10
PCTUSED 80
ENABLE ROW MOVEMENT;

-- Create sequence for metrics ID
CREATE SEQUENCE CM3INT.SEQ_DASHBOARD_METRICS_TIMESERIES
START WITH 1
INCREMENT BY 1
CACHE 1000
NOCYCLE
NOORDER;

-- Time-series optimized indexes for high-performance queries
CREATE INDEX idx_metrics_ts_realtime 
ON CM3INT.DASHBOARD_METRICS_TIMESERIES(metric_timestamp DESC, metric_type, execution_id)
LOCAL COMPRESS
TABLESPACE USERS
PARALLEL 4;

CREATE INDEX idx_metrics_execution_lookup 
ON CM3INT.DASHBOARD_METRICS_TIMESERIES(execution_id, metric_timestamp DESC, metric_type)
LOCAL COMPRESS
TABLESPACE USERS
PARALLEL 4;

CREATE INDEX idx_metrics_dashboard_query 
ON CM3INT.DASHBOARD_METRICS_TIMESERIES(metric_type, metric_status, metric_timestamp DESC)
LOCAL COMPRESS
TABLESPACE USERS
PARALLEL 4;

-- Composite index for correlation tracking (SOX compliance)
CREATE INDEX idx_metrics_correlation_tracking
ON CM3INT.DASHBOARD_METRICS_TIMESERIES(correlation_id, user_id, created_date DESC)
LOCAL COMPRESS
TABLESPACE USERS
PARALLEL 4;

-- Security index for audit hash validation
CREATE INDEX idx_metrics_security_audit
ON CM3INT.DASHBOARD_METRICS_TIMESERIES(audit_hash, compliance_flags, security_classification)
LOCAL COMPRESS
TABLESPACE USERS;

-- ============================================================================
-- 2. WEBSOCKET SESSION MANAGEMENT
-- ============================================================================

-- WebSocket session tracking for distributed session management
CREATE TABLE CM3INT.WEBSOCKET_SESSIONS (
    session_id VARCHAR2(100) NOT NULL,
    user_id VARCHAR2(100) NOT NULL,
    connection_id VARCHAR2(200) NOT NULL,
    client_ip VARCHAR2(50),
    user_agent VARCHAR2(500),
    origin_url VARCHAR2(200),
    -- Session lifecycle
    connected_at TIMESTAMP(6) NOT NULL,
    last_activity TIMESTAMP(6) NOT NULL,
    disconnected_at TIMESTAMP(6),
    session_status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    -- Authentication and authorization
    jwt_token_id VARCHAR2(200),
    csrf_token VARCHAR2(100),
    user_roles CLOB, -- JSON array of user roles
    permissions CLOB, -- JSON array of permissions
    -- Security tracking
    failed_auth_attempts NUMBER(3) DEFAULT 0,
    rate_limit_violations NUMBER(5) DEFAULT 0,
    security_violations CLOB, -- JSON array of security events
    last_token_rotation TIMESTAMP(6),
    -- Performance and monitoring
    message_count_sent NUMBER(10) DEFAULT 0,
    message_count_received NUMBER(10) DEFAULT 0,
    bytes_sent NUMBER(15) DEFAULT 0,
    bytes_received NUMBER(15) DEFAULT 0,
    connection_quality_score NUMBER(5,2),
    -- Compliance and audit
    correlation_id VARCHAR2(100),
    audit_hash VARCHAR2(64) NOT NULL,
    compliance_flags VARCHAR2(200),
    -- Standard fields
    created_date TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(100) NOT NULL,
    last_modified_date TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR2(100),
    -- Constraints
    CONSTRAINT pk_websocket_sessions PRIMARY KEY (session_id),
    CONSTRAINT chk_ws_session_status CHECK (session_status IN ('ACTIVE', 'INACTIVE', 'DISCONNECTED', 'TERMINATED', 'SUSPENDED')),
    CONSTRAINT chk_ws_failed_auth_attempts CHECK (failed_auth_attempts BETWEEN 0 AND 10),
    CONSTRAINT chk_ws_connection_quality CHECK (connection_quality_score BETWEEN 0 AND 100),
    CONSTRAINT ck_ws_sessions_audit_hash CHECK (LENGTH(audit_hash) = 64)
)
TABLESPACE USERS
PCTFREE 10
PCTUSED 80;

-- Indexes for session management performance
CREATE INDEX idx_ws_sessions_user_active
ON CM3INT.WEBSOCKET_SESSIONS(user_id, session_status, last_activity DESC)
TABLESPACE USERS;

CREATE INDEX idx_ws_sessions_cleanup
ON CM3INT.WEBSOCKET_SESSIONS(session_status, last_activity)
TABLESPACE USERS;

CREATE INDEX idx_ws_sessions_security
ON CM3INT.WEBSOCKET_SESSIONS(client_ip, failed_auth_attempts, rate_limit_violations)
TABLESPACE USERS;

-- ============================================================================
-- 3. COMPREHENSIVE AUDIT FRAMEWORK
-- ============================================================================

-- Enhanced audit log for WebSocket events and security monitoring
CREATE TABLE CM3INT.WEBSOCKET_AUDIT_LOG (
    audit_id NUMBER(19) NOT NULL,
    session_id VARCHAR2(100),
    event_type VARCHAR2(50) NOT NULL,
    event_subtype VARCHAR2(50),
    event_description VARCHAR2(500),
    -- User and security context
    user_id VARCHAR2(100),
    client_ip VARCHAR2(50),
    user_agent VARCHAR2(500),
    origin_url VARCHAR2(200),
    -- Event details
    event_timestamp TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    event_data CLOB, -- JSON event details
    event_severity VARCHAR2(20) DEFAULT 'INFO',
    -- Security classification
    security_classification VARCHAR2(50) DEFAULT 'INTERNAL',
    security_event_flag CHAR(1) DEFAULT 'N' CHECK (security_event_flag IN ('Y', 'N')),
    compliance_event_flag CHAR(1) DEFAULT 'N' CHECK (compliance_event_flag IN ('Y', 'N')),
    -- Performance metrics
    processing_duration_ms NUMBER(10),
    memory_usage_mb NUMBER(10),
    cpu_usage_percent NUMBER(5,2),
    -- Compliance and integrity
    correlation_id VARCHAR2(100),
    audit_hash VARCHAR2(64) NOT NULL, -- Tamper detection
    digital_signature VARCHAR2(500), -- Optional digital signature for critical events
    previous_audit_hash VARCHAR2(64), -- Blockchain-style linking
    -- Business context
    business_function VARCHAR2(100),
    risk_level VARCHAR2(20) DEFAULT 'LOW',
    -- Standard audit fields
    created_date TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(100) NOT NULL,
    -- Constraints
    CONSTRAINT pk_websocket_audit_log PRIMARY KEY (audit_id),
    CONSTRAINT chk_ws_audit_severity CHECK (event_severity IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'CRITICAL')),
    CONSTRAINT chk_ws_audit_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_ws_audit_hash CHECK (LENGTH(audit_hash) = 64),
    CONSTRAINT chk_ws_audit_cpu CHECK (cpu_usage_percent BETWEEN 0 AND 100)
)
PARTITION BY RANGE (event_timestamp) INTERVAL (NUMTODSINTERVAL(1, 'DAY'))
(
    PARTITION p_audit_initial VALUES LESS THAN (TIMESTAMP '2025-08-09 00:00:00')
)
TABLESPACE USERS
PCTFREE 10
PCTUSED 80
ENABLE ROW MOVEMENT;

-- Create sequence for audit log
CREATE SEQUENCE CM3INT.SEQ_WEBSOCKET_AUDIT_LOG
START WITH 1
INCREMENT BY 1
CACHE 1000
NOCYCLE
NOORDER;

-- Audit log indexes for compliance and performance
CREATE INDEX idx_ws_audit_timestamp_type
ON CM3INT.WEBSOCKET_AUDIT_LOG(event_timestamp DESC, event_type, user_id)
LOCAL COMPRESS
TABLESPACE USERS
PARALLEL 4;

CREATE INDEX idx_ws_audit_security_events
ON CM3INT.WEBSOCKET_AUDIT_LOG(security_event_flag, compliance_event_flag, event_timestamp DESC)
LOCAL COMPRESS
TABLESPACE USERS;

CREATE INDEX idx_ws_audit_user_tracking
ON CM3INT.WEBSOCKET_AUDIT_LOG(user_id, event_timestamp DESC, event_type)
LOCAL COMPRESS
TABLESPACE USERS;

CREATE INDEX idx_ws_audit_correlation
ON CM3INT.WEBSOCKET_AUDIT_LOG(correlation_id, audit_hash, event_timestamp DESC)
LOCAL COMPRESS
TABLESPACE USERS;

-- ============================================================================
-- 4. ENHANCE EXISTING BATCH_PROCESSING_STATUS TABLE
-- ============================================================================

-- Add WebSocket monitoring fields to existing table
ALTER TABLE CM3INT.BATCH_PROCESSING_STATUS ADD (
    -- Real-time monitoring configuration
    monitoring_enabled CHAR(1) DEFAULT 'Y' CHECK (monitoring_enabled IN ('Y', 'N')),
    dashboard_display_priority NUMBER(2) DEFAULT 5 CHECK (dashboard_display_priority BETWEEN 1 AND 10),
    websocket_broadcast_enabled CHAR(1) DEFAULT 'Y' CHECK (websocket_broadcast_enabled IN ('Y', 'N')),
    real_time_metrics_enabled CHAR(1) DEFAULT 'Y' CHECK (real_time_metrics_enabled IN ('Y', 'N')),
    
    -- Alert thresholds
    alert_threshold_warning NUMBER(10,2),
    alert_threshold_critical NUMBER(10,2),
    performance_threshold_ms NUMBER(10) DEFAULT 5000,
    memory_threshold_mb NUMBER(10) DEFAULT 1000,
    
    -- Metrics update configuration
    metrics_update_frequency NUMBER(10) DEFAULT 5000, -- milliseconds
    last_metrics_update TIMESTAMP(6),
    last_dashboard_broadcast TIMESTAMP(6),
    
    -- Delta update optimization
    last_delta_hash VARCHAR2(64),
    metrics_change_flag CHAR(1) DEFAULT 'N' CHECK (metrics_change_flag IN ('Y', 'N')),
    
    -- SOX compliance fields
    configuration_approved_by VARCHAR2(100),
    configuration_approved_date TIMESTAMP(6),
    configuration_change_id VARCHAR2(100),
    configuration_change_reason VARCHAR2(500),
    
    -- Additional audit fields
    websocket_session_count NUMBER(5) DEFAULT 0,
    last_websocket_activity TIMESTAMP(6)
);

-- Create index for real-time monitoring queries
CREATE INDEX idx_batch_status_realtime_monitoring 
ON CM3INT.BATCH_PROCESSING_STATUS(
    monitoring_enabled,
    processing_status, 
    last_heartbeat DESC,
    execution_id,
    dashboard_display_priority
) COMPRESS
TABLESPACE USERS;

-- Create index for WebSocket broadcasting
CREATE INDEX idx_batch_status_websocket_broadcast
ON CM3INT.BATCH_PROCESSING_STATUS(
    websocket_broadcast_enabled,
    metrics_change_flag,
    last_dashboard_broadcast,
    execution_id
) COMPRESS
TABLESPACE USERS;

-- ============================================================================
-- 5. STORED PROCEDURES FOR DATA MANAGEMENT
-- ============================================================================

-- Procedure for automatic data retention and cleanup
CREATE OR REPLACE PROCEDURE CM3INT.PURGE_DASHBOARD_METRICS_DATA(
    p_retention_days IN NUMBER DEFAULT 90,
    p_audit_retention_years IN NUMBER DEFAULT 7
) AS
    v_metrics_cutoff TIMESTAMP;
    v_audit_cutoff TIMESTAMP;
    v_deleted_metrics NUMBER;
    v_deleted_audit NUMBER;
BEGIN
    -- Calculate cutoff dates
    v_metrics_cutoff := SYSTIMESTAMP - INTERVAL p_retention_days DAY;
    v_audit_cutoff := SYSTIMESTAMP - INTERVAL p_audit_retention_years YEAR;
    
    -- Purge old metrics data (90 days default)
    DELETE FROM CM3INT.DASHBOARD_METRICS_TIMESERIES 
    WHERE metric_timestamp < v_metrics_cutoff;
    v_deleted_metrics := SQL%ROWCOUNT;
    
    -- Purge old audit logs (7 years for SOX compliance)
    DELETE FROM CM3INT.WEBSOCKET_AUDIT_LOG 
    WHERE event_timestamp < v_audit_cutoff
    AND compliance_event_flag = 'N'; -- Keep compliance events longer
    v_deleted_audit := SQL%ROWCOUNT;
    
    -- Clean up disconnected WebSocket sessions older than 24 hours
    DELETE FROM CM3INT.WEBSOCKET_SESSIONS 
    WHERE session_status IN ('DISCONNECTED', 'TERMINATED')
    AND disconnected_at < SYSTIMESTAMP - INTERVAL '1' DAY;
    
    COMMIT;
    
    -- Log cleanup activity
    INSERT INTO CM3INT.WEBSOCKET_AUDIT_LOG (
        audit_id, event_type, event_description, user_id, 
        event_data, security_classification, created_by, audit_hash
    ) VALUES (
        CM3INT.SEQ_WEBSOCKET_AUDIT_LOG.NEXTVAL,
        'DATA_CLEANUP',
        'Automatic data purge completed',
        'SYSTEM',
        '{"deleted_metrics": ' || v_deleted_metrics || ', "deleted_audit": ' || v_deleted_audit || '}',
        'INTERNAL',
        'SYSTEM_PURGE_JOB',
        DBMS_CRYPTO.HASH(UTL_RAW.CAST_TO_RAW('PURGE_' || SYSTIMESTAMP), DBMS_CRYPTO.HASH_SH256)
    );
    
    COMMIT;
    
    -- Gather statistics for optimal query performance
    DBMS_STATS.GATHER_TABLE_STATS('CM3INT', 'DASHBOARD_METRICS_TIMESERIES', CASCADE => TRUE);
    DBMS_STATS.GATHER_TABLE_STATS('CM3INT', 'WEBSOCKET_AUDIT_LOG', CASCADE => TRUE);
    DBMS_STATS.GATHER_TABLE_STATS('CM3INT', 'BATCH_PROCESSING_STATUS', CASCADE => TRUE);
    
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        -- Log error
        INSERT INTO CM3INT.WEBSOCKET_AUDIT_LOG (
            audit_id, event_type, event_description, event_severity,
            user_id, created_by, audit_hash
        ) VALUES (
            CM3INT.SEQ_WEBSOCKET_AUDIT_LOG.NEXTVAL,
            'DATA_CLEANUP_ERROR',
            'Data purge failed: ' || SQLERRM,
            'ERROR',
            'SYSTEM',
            'SYSTEM_PURGE_JOB',
            DBMS_CRYPTO.HASH(UTL_RAW.CAST_TO_RAW('ERROR_' || SYSTIMESTAMP), DBMS_CRYPTO.HASH_SH256)
        );
        COMMIT;
        RAISE;
END;
/

-- Procedure for WebSocket session cleanup
CREATE OR REPLACE PROCEDURE CM3INT.CLEANUP_WEBSOCKET_SESSIONS AS
    v_cleanup_count NUMBER;
BEGIN
    -- Mark stale sessions as terminated (no activity for 1 hour)
    UPDATE CM3INT.WEBSOCKET_SESSIONS 
    SET session_status = 'TERMINATED',
        disconnected_at = SYSTIMESTAMP,
        last_modified_date = SYSTIMESTAMP,
        last_modified_by = 'SYSTEM_CLEANUP'
    WHERE session_status = 'ACTIVE'
    AND last_activity < SYSTIMESTAMP - INTERVAL '1' HOUR;
    
    v_cleanup_count := SQL%ROWCOUNT;
    
    -- Remove old terminated sessions (older than 24 hours)
    DELETE FROM CM3INT.WEBSOCKET_SESSIONS
    WHERE session_status IN ('TERMINATED', 'DISCONNECTED')
    AND disconnected_at < SYSTIMESTAMP - INTERVAL '1' DAY;
    
    COMMIT;
    
    -- Log cleanup activity
    IF v_cleanup_count > 0 THEN
        INSERT INTO CM3INT.WEBSOCKET_AUDIT_LOG (
            audit_id, event_type, event_description, user_id,
            event_data, created_by, audit_hash
        ) VALUES (
            CM3INT.SEQ_WEBSOCKET_AUDIT_LOG.NEXTVAL,
            'SESSION_CLEANUP',
            'Automatic WebSocket session cleanup',
            'SYSTEM',
            '{"terminated_sessions": ' || v_cleanup_count || '}',
            'SYSTEM_CLEANUP_JOB',
            DBMS_CRYPTO.HASH(UTL_RAW.CAST_TO_RAW('SESSION_CLEANUP_' || SYSTIMESTAMP), DBMS_CRYPTO.HASH_SH256)
        );
        COMMIT;
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/

-- ============================================================================
-- 6. SCHEDULER JOBS FOR AUTOMATED MAINTENANCE
-- ============================================================================

-- Schedule automatic data purging (daily at 2 AM)
BEGIN
    DBMS_SCHEDULER.CREATE_JOB (
        job_name        => 'PURGE_DASHBOARD_METRICS_JOB',
        job_type        => 'PLSQL_BLOCK',
        job_action      => 'BEGIN CM3INT.PURGE_DASHBOARD_METRICS_DATA(90, 7); END;',
        start_date      => SYSTIMESTAMP,
        repeat_interval => 'FREQ=DAILY; BYHOUR=2; BYMINUTE=0; BYSECOND=0',
        enabled         => TRUE,
        comments        => 'US008: Daily cleanup of dashboard metrics and audit data'
    );
END;
/

-- Schedule WebSocket session cleanup (every hour)
BEGIN
    DBMS_SCHEDULER.CREATE_JOB (
        job_name        => 'CLEANUP_WEBSOCKET_SESSIONS_JOB',
        job_type        => 'PLSQL_BLOCK',
        job_action      => 'BEGIN CM3INT.CLEANUP_WEBSOCKET_SESSIONS; END;',
        start_date      => SYSTIMESTAMP,
        repeat_interval => 'FREQ=HOURLY; BYMINUTE=0; BYSECOND=0',
        enabled         => TRUE,
        comments        => 'US008: Hourly cleanup of stale WebSocket sessions'
    );
END;
/

-- ============================================================================
-- 7. GRANTS AND PERMISSIONS
-- ============================================================================

-- Grant necessary permissions to application user
-- Note: Replace 'FABRIC_APP_USER' with actual application database user
-- GRANT SELECT, INSERT, UPDATE, DELETE ON CM3INT.DASHBOARD_METRICS_TIMESERIES TO FABRIC_APP_USER;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON CM3INT.WEBSOCKET_SESSIONS TO FABRIC_APP_USER;
-- GRANT SELECT, INSERT, UPDATE ON CM3INT.WEBSOCKET_AUDIT_LOG TO FABRIC_APP_USER;
-- GRANT SELECT ON CM3INT.SEQ_DASHBOARD_METRICS_TIMESERIES TO FABRIC_APP_USER;
-- GRANT SELECT ON CM3INT.SEQ_WEBSOCKET_AUDIT_LOG TO FABRIC_APP_USER;
-- GRANT EXECUTE ON CM3INT.PURGE_DASHBOARD_METRICS_DATA TO FABRIC_APP_USER;
-- GRANT EXECUTE ON CM3INT.CLEANUP_WEBSOCKET_SESSIONS TO FABRIC_APP_USER;

-- ============================================================================
-- 8. VALIDATION AND TESTING QUERIES
-- ============================================================================

-- Validate table creation
SELECT table_name, num_rows, last_analyzed
FROM user_tables 
WHERE table_name IN ('DASHBOARD_METRICS_TIMESERIES', 'WEBSOCKET_SESSIONS', 'WEBSOCKET_AUDIT_LOG')
ORDER BY table_name;

-- Validate indexes
SELECT index_name, table_name, status, locality
FROM user_indexes 
WHERE table_name IN ('DASHBOARD_METRICS_TIMESERIES', 'WEBSOCKET_SESSIONS', 'WEBSOCKET_AUDIT_LOG')
ORDER BY table_name, index_name;

-- Validate partitions
SELECT table_name, partition_name, high_value, partition_position
FROM user_tab_partitions
WHERE table_name IN ('DASHBOARD_METRICS_TIMESERIES', 'WEBSOCKET_AUDIT_LOG')
ORDER BY table_name, partition_position;

-- Validate scheduled jobs
SELECT job_name, enabled, state, last_start_date, next_run_date
FROM user_scheduler_jobs
WHERE job_name LIKE '%DASHBOARD%' OR job_name LIKE '%WEBSOCKET%'
ORDER BY job_name;

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================

-- Insert completion audit record
INSERT INTO CM3INT.WEBSOCKET_AUDIT_LOG (
    audit_id, event_type, event_description, event_severity,
    user_id, security_classification, created_by, audit_hash
) VALUES (
    CM3INT.SEQ_WEBSOCKET_AUDIT_LOG.NEXTVAL,
    'SCHEMA_MIGRATION',
    'US008 Real-Time Monitoring Database Schema Migration Completed Successfully',
    'INFO',
    'MIGRATION_SCRIPT',
    'INTERNAL',
    'V2025.004_MIGRATION',
    DBMS_CRYPTO.HASH(UTL_RAW.CAST_TO_RAW('US008_SCHEMA_MIGRATION_COMPLETE_' || SYSTIMESTAMP), DBMS_CRYPTO.HASH_SH256)
);

COMMIT;

-- Display completion message
SELECT 'US008 Real-Time Job Monitoring Dashboard - Database Schema Migration Completed Successfully' AS status,
       'Time-series tables, WebSocket session management, and audit framework created' AS details,
       SYSTIMESTAMP AS completed_at
FROM DUAL;

-- ============================================================================
-- END OF MIGRATION SCRIPT
-- ============================================================================