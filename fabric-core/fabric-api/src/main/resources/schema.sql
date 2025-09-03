-- Database schema for fabric-platform backend
-- Converted from JPA auto-DDL to manual SQL scripts

-- Create schemas (for H2 testing, schema creation is optional)
CREATE SCHEMA IF NOT EXISTS CM3INT;

-- ============================================================================
-- SOURCE_SYSTEMS table
-- ============================================================================
CREATE TABLE CM3INT.SOURCE_SYSTEMS (
    ID VARCHAR(50) PRIMARY KEY,
    NAME VARCHAR(100) NOT NULL,
    TYPE VARCHAR(20) NOT NULL,
    DESCRIPTION VARCHAR(500),
    CONNECTION_STRING VARCHAR(1000),
    ENABLED VARCHAR(1) DEFAULT 'Y',
    CREATED_DATE TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    JOB_COUNT INTEGER DEFAULT 0
);

-- ============================================================================
-- TEMPLATE_SOURCE_MAPPINGS table
-- ============================================================================
CREATE TABLE CM3INT.TEMPLATE_SOURCE_MAPPINGS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    FILE_TYPE VARCHAR(50) NOT NULL,
    TRANSACTION_TYPE VARCHAR(10) NOT NULL,
    SOURCE_SYSTEM_ID VARCHAR(50) NOT NULL,
    JOB_NAME VARCHAR(100) NOT NULL,
    TARGET_FIELD_NAME VARCHAR(50) NOT NULL,
    SOURCE_FIELD_NAME VARCHAR(100),
    TRANSFORMATION_TYPE VARCHAR(20) DEFAULT 'source',
    TRANSFORMATION_CONFIG VARCHAR(1000),
    VALUE VARCHAR(1000),
    DEFAULT_VALUE VARCHAR(1000),
    TARGET_POSITION INTEGER,
    LENGTH INTEGER,
    DATA_TYPE VARCHAR(20),
    CREATED_BY VARCHAR(50) NOT NULL,
    CREATED_DATE TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFIED_BY VARCHAR(50),
    MODIFIED_DATE TIMESTAMP,
    VERSION INTEGER DEFAULT 1,
    ENABLED VARCHAR(1) DEFAULT 'Y',
    FOREIGN KEY (SOURCE_SYSTEM_ID) REFERENCES CM3INT.SOURCE_SYSTEMS(ID)
);

-- ============================================================================
-- WEBSOCKET_AUDIT_LOG table (simplified for basic functionality)
-- ============================================================================
CREATE TABLE WEBSOCKET_AUDIT_LOG (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    event_subtype VARCHAR(50),
    event_description VARCHAR(500),
    session_id VARCHAR(100),
    user_id VARCHAR(100),
    client_ip VARCHAR(50),
    user_agent VARCHAR(500),
    origin_url VARCHAR(200),
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    security_event_flag VARCHAR(1) DEFAULT 'N',
    compliance_event_flag VARCHAR(1) DEFAULT 'N',
    event_severity VARCHAR(20),
    risk_level VARCHAR(20),
    business_function VARCHAR(100),
    correlation_id VARCHAR(100),
    audit_hash VARCHAR(256),
    previous_audit_hash VARCHAR(256),
    digital_signature VARCHAR(1000),
    security_classification VARCHAR(50),
    source_system VARCHAR(100),
    thread_id VARCHAR(100),
    server_hostname VARCHAR(100),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- DASHBOARD_METRICS_TIMESERIES table (simplified for basic functionality)
-- ============================================================================
CREATE TABLE DASHBOARD_METRICS_TIMESERIES (
    metrics_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(100),
    metric_name VARCHAR(100),
    metric_type VARCHAR(50),
    metric_value DECIMAL(19,4),
    metric_unit VARCHAR(20),
    metric_status VARCHAR(20),
    metric_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cpu_usage_percent DECIMAL(5,2),
    memory_usage_mb BIGINT,
    processing_duration_ms BIGINT,
    throughput_per_second DECIMAL(19,4),
    success_rate_percent DECIMAL(5,2),
    correlation_id VARCHAR(100),
    source_system VARCHAR(100),
    user_id VARCHAR(100),
    thread_id VARCHAR(100),
    audit_hash VARCHAR(256),
    compliance_flags VARCHAR(500),
    security_classification VARCHAR(50),
    data_quality_score DECIMAL(5,2),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    partition_id VARCHAR(50)
);

-- ============================================================================
-- Indexes for performance
-- ============================================================================

-- SOURCE_SYSTEMS indexes
CREATE INDEX idx_source_systems_enabled ON CM3INT.SOURCE_SYSTEMS(ENABLED);
CREATE INDEX idx_source_systems_type ON CM3INT.SOURCE_SYSTEMS(TYPE);
CREATE INDEX idx_source_systems_job_count ON CM3INT.SOURCE_SYSTEMS(JOB_COUNT DESC);

-- TEMPLATE_SOURCE_MAPPINGS indexes
CREATE INDEX idx_template_mappings_lookup ON CM3INT.TEMPLATE_SOURCE_MAPPINGS(FILE_TYPE, TRANSACTION_TYPE, SOURCE_SYSTEM_ID, ENABLED);
CREATE INDEX idx_template_mappings_source_system ON CM3INT.TEMPLATE_SOURCE_MAPPINGS(SOURCE_SYSTEM_ID, ENABLED);
CREATE INDEX idx_template_mappings_job ON CM3INT.TEMPLATE_SOURCE_MAPPINGS(JOB_NAME, ENABLED);

-- WEBSOCKET_AUDIT_LOG indexes
CREATE INDEX idx_ws_audit_timestamp_type ON WEBSOCKET_AUDIT_LOG(event_timestamp DESC, event_type, user_id);
CREATE INDEX idx_ws_audit_security_events ON WEBSOCKET_AUDIT_LOG(security_event_flag, compliance_event_flag, event_timestamp DESC);
CREATE INDEX idx_ws_audit_user_tracking ON WEBSOCKET_AUDIT_LOG(user_id, event_timestamp DESC, event_type);
CREATE INDEX idx_ws_audit_correlation ON WEBSOCKET_AUDIT_LOG(correlation_id, audit_hash, event_timestamp DESC);
CREATE INDEX idx_ws_audit_session ON WEBSOCKET_AUDIT_LOG(session_id, event_timestamp DESC);

-- DASHBOARD_METRICS_TIMESERIES indexes
CREATE INDEX idx_metrics_ts_realtime ON DASHBOARD_METRICS_TIMESERIES(metric_timestamp DESC, metric_type, execution_id);
CREATE INDEX idx_metrics_execution_lookup ON DASHBOARD_METRICS_TIMESERIES(execution_id, metric_timestamp DESC, metric_type);
CREATE INDEX idx_metrics_dashboard_query ON DASHBOARD_METRICS_TIMESERIES(metric_type, metric_status, metric_timestamp DESC);
CREATE INDEX idx_metrics_correlation_tracking ON DASHBOARD_METRICS_TIMESERIES(correlation_id, user_id, created_date DESC);
CREATE INDEX idx_metrics_security_audit ON DASHBOARD_METRICS_TIMESERIES(audit_hash, compliance_flags, security_classification);