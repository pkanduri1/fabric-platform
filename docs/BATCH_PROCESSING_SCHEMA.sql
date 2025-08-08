-- ================================================================
-- BATCH PROCESSING DATABASE SCHEMA
-- Version: 1.0
-- Description: Configuration and staging tables for generic batch processing
-- ================================================================

-- ================================================================
-- CONFIGURATION TABLES
-- ================================================================

-- Main batch job configuration
CREATE TABLE CM3INT.batch_job_configs (
    job_config_id         VARCHAR2(100) PRIMARY KEY,
    job_name             VARCHAR2(100) NOT NULL,
    source_system        VARCHAR2(50) NOT NULL,
    file_type           VARCHAR2(50) NOT NULL,
    processing_type     VARCHAR2(20) CHECK (processing_type IN ('SIMPLE', 'COMPLEX')),
    output_format       VARCHAR2(20) CHECK (output_format IN ('DELIMITED', 'FIXED', 'XML', 'JSON')),
    output_delimiter    VARCHAR2(10) DEFAULT '|',
    parallel_enabled    CHAR(1) DEFAULT 'N' CHECK (parallel_enabled IN ('Y', 'N')),
    parallel_threads    NUMBER(2) DEFAULT 1,
    chunk_size         NUMBER(10) DEFAULT 1000,
    header_enabled     CHAR(1) DEFAULT 'N' CHECK (header_enabled IN ('Y', 'N')),
    footer_enabled     CHAR(1) DEFAULT 'N' CHECK (footer_enabled IN ('Y', 'N')),
    active_flag        CHAR(1) DEFAULT 'Y' CHECK (active_flag IN ('Y', 'N')),
    created_by         VARCHAR2(100) NOT NULL,
    created_date       TIMESTAMP DEFAULT SYSTIMESTAMP,
    modified_by        VARCHAR2(100),
    modified_date      TIMESTAMP,
    CONSTRAINT uk_batch_job_name UNIQUE (job_name, source_system)
);

-- Transaction type configuration for parallel processing
CREATE TABLE CM3INT.batch_transaction_types (
    transaction_type_id   VARCHAR2(100) PRIMARY KEY,
    job_config_id        VARCHAR2(100) NOT NULL,
    transaction_code     VARCHAR2(50) NOT NULL,
    transaction_name     VARCHAR2(200) NOT NULL,
    processing_order     NUMBER(5) NOT NULL,
    source_table        VARCHAR2(100) NOT NULL,
    filter_condition    VARCHAR2(4000),
    grouping_fields     VARCHAR2(500),
    sorting_fields      VARCHAR2(500),
    parallel_eligible   CHAR(1) DEFAULT 'Y' CHECK (parallel_eligible IN ('Y', 'N')),
    dependency_type_id  VARCHAR2(100), -- Reference to dependent transaction
    active_flag         CHAR(1) DEFAULT 'Y' CHECK (active_flag IN ('Y', 'N')),
    created_by          VARCHAR2(100) NOT NULL,
    created_date        TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_batch_trans_job FOREIGN KEY (job_config_id) 
        REFERENCES batch_job_configs(job_config_id),
    CONSTRAINT fk_batch_trans_dep FOREIGN KEY (dependency_type_id) 
        REFERENCES batch_transaction_types(transaction_type_id)
);

-- Field mapping configuration
CREATE TABLE CM3INT.batch_field_mappings (
    mapping_id           VARCHAR2(100) PRIMARY KEY,
    transaction_type_id  VARCHAR2(100) NOT NULL,
    source_column       VARCHAR2(100) NOT NULL,
    target_field_name   VARCHAR2(100) NOT NULL,
    field_position      NUMBER(5) NOT NULL,
    data_type          VARCHAR2(50) NOT NULL,
    field_length       NUMBER(10),
    decimal_places     NUMBER(2),
    date_format        VARCHAR2(50),
    default_value      VARCHAR2(500),
    transformation_rule VARCHAR2(4000), -- SQL expression for transformation
    validation_rule    VARCHAR2(4000),
    mandatory_flag     CHAR(1) DEFAULT 'N' CHECK (mandatory_flag IN ('Y', 'N')),
    padding_char       VARCHAR2(1),
    padding_direction  VARCHAR2(10) CHECK (padding_direction IN ('LEFT', 'RIGHT')),
    trim_flag         CHAR(1) DEFAULT 'Y' CHECK (trim_flag IN ('Y', 'N')),
    active_flag       CHAR(1) DEFAULT 'Y' CHECK (active_flag IN ('Y', 'N')),
    created_by        VARCHAR2(100) NOT NULL,
    created_date      TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_batch_field_trans FOREIGN KEY (transaction_type_id) 
        REFERENCES batch_transaction_types(transaction_type_id)
);

-- Header configuration
CREATE TABLE CM3INT.batch_header_configs (
    header_config_id    VARCHAR2(100) PRIMARY KEY,
    job_config_id      VARCHAR2(100) NOT NULL,
    header_type        VARCHAR2(20) CHECK (header_type IN ('STATIC', 'DYNAMIC')),
    header_template    VARCHAR2(4000),
    field_mappings     CLOB, -- JSON structure for dynamic fields
    active_flag        CHAR(1) DEFAULT 'Y' CHECK (active_flag IN ('Y', 'N')),
    created_by         VARCHAR2(100) NOT NULL,
    created_date       TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_batch_header_job FOREIGN KEY (job_config_id) 
        REFERENCES batch_job_configs(job_config_id)
);

-- Footer configuration
CREATE TABLE CM3INT.batch_footer_configs (
    footer_config_id    VARCHAR2(100) PRIMARY KEY,
    job_config_id      VARCHAR2(100) NOT NULL,
    footer_type        VARCHAR2(20) CHECK (footer_type IN ('STATIC', 'SUMMARY')),
    footer_template    VARCHAR2(4000),
    summary_fields     CLOB, -- JSON structure for summary calculations
    active_flag        CHAR(1) DEFAULT 'Y' CHECK (active_flag IN ('Y', 'N')),
    created_by         VARCHAR2(100) NOT NULL,
    created_date       TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_batch_footer_job FOREIGN KEY (job_config_id) 
        REFERENCES batch_job_configs(job_config_id)
);

-- ================================================================
-- RUNTIME TABLES
-- ================================================================

-- Batch execution tracking
CREATE TABLE CM3INT.batch_job_executions (
    execution_id        VARCHAR2(100) PRIMARY KEY,
    job_config_id      VARCHAR2(100) NOT NULL,
    batch_date         DATE NOT NULL,
    start_time         TIMESTAMP NOT NULL,
    end_time          TIMESTAMP,
    status            VARCHAR2(20) CHECK (status IN ('STARTED', 'RUNNING', 'COMPLETED', 'FAILED', 'STOPPED')),
    total_records     NUMBER(15),
    processed_records NUMBER(15),
    error_records     NUMBER(15),
    output_file_path  VARCHAR2(500),
    output_file_size  NUMBER(20),
    error_message     VARCHAR2(4000),
    execution_params  CLOB, -- JSON structure for runtime parameters
    created_by        VARCHAR2(100) NOT NULL,
    created_date      TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_batch_exec_job FOREIGN KEY (job_config_id) 
        REFERENCES batch_job_configs(job_config_id)
);

-- Parallel processing tracking
CREATE TABLE CM3INT.batch_parallel_tasks (
    task_id            VARCHAR2(100) PRIMARY KEY,
    execution_id       VARCHAR2(100) NOT NULL,
    transaction_type_id VARCHAR2(100) NOT NULL,
    thread_number      NUMBER(3),
    start_time        TIMESTAMP NOT NULL,
    end_time          TIMESTAMP,
    status            VARCHAR2(20) CHECK (status IN ('STARTED', 'RUNNING', 'COMPLETED', 'FAILED')),
    records_processed NUMBER(15),
    error_count       NUMBER(10),
    error_message     VARCHAR2(4000),
    created_date      TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_batch_task_exec FOREIGN KEY (execution_id) 
        REFERENCES batch_job_executions(execution_id),
    CONSTRAINT fk_batch_task_trans FOREIGN KEY (transaction_type_id) 
        REFERENCES batch_transaction_types(transaction_type_id)
);

-- ================================================================
-- STAGING TABLES
-- ================================================================

-- Temporary staging for complex transactions
CREATE TABLE CM3INT.batch_temp_staging (
    staging_id         VARCHAR2(100) PRIMARY KEY,
    execution_id       VARCHAR2(100) NOT NULL,
    transaction_type   VARCHAR2(50) NOT NULL,
    sequence_number    NUMBER(15) NOT NULL,
    record_data        CLOB NOT NULL,
    dependencies_met   CHAR(1) DEFAULT 'N' CHECK (dependencies_met IN ('Y', 'N')),
    processed_flag     CHAR(1) DEFAULT 'N' CHECK (processed_flag IN ('Y', 'N')),
    error_flag        CHAR(1) DEFAULT 'N' CHECK (error_flag IN ('Y', 'N')),
    error_message     VARCHAR2(4000),
    created_date      TIMESTAMP DEFAULT SYSTIMESTAMP,
    processed_date    TIMESTAMP,
    CONSTRAINT fk_batch_temp_exec FOREIGN KEY (execution_id) 
        REFERENCES batch_job_executions(execution_id)
);

-- File summary for footer generation
CREATE TABLE CM3INT.batch_file_summary (
    summary_id         VARCHAR2(100) PRIMARY KEY,
    execution_id       VARCHAR2(100) NOT NULL,
    summary_type      VARCHAR2(50) NOT NULL,
    summary_key       VARCHAR2(200),
    summary_value     VARCHAR2(500),
    numeric_value     NUMBER(20,4),
    created_date      TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_batch_summary_exec FOREIGN KEY (execution_id) 
        REFERENCES batch_job_executions(execution_id)
);

-- ================================================================
-- AUDIT TABLES
-- ================================================================

-- Batch processing audit trail
CREATE TABLE CM3INT.batch_audit_log (
    audit_id          VARCHAR2(100) PRIMARY KEY,
    execution_id      VARCHAR2(100),
    event_type       VARCHAR2(50) NOT NULL,
    event_timestamp  TIMESTAMP DEFAULT SYSTIMESTAMP,
    event_source     VARCHAR2(100),
    event_message    VARCHAR2(4000),
    record_count     NUMBER(15),
    processing_time_ms NUMBER(15),
    user_id          VARCHAR2(100),
    session_id       VARCHAR2(100),
    additional_info  CLOB -- JSON structure for additional audit data
);

-- ================================================================
-- INDEXES FOR PERFORMANCE
-- ================================================================

CREATE INDEX idx_batch_exec_status ON batch_job_executions(status, batch_date);
CREATE INDEX idx_batch_exec_job ON batch_job_executions(job_config_id, batch_date);
CREATE INDEX idx_batch_task_exec ON batch_parallel_tasks(execution_id, status);
CREATE INDEX idx_batch_temp_stage ON batch_temp_staging(execution_id, processed_flag);
CREATE INDEX idx_batch_temp_seq ON batch_temp_staging(execution_id, sequence_number);
CREATE INDEX idx_batch_audit_exec ON batch_audit_log(execution_id, event_timestamp);
CREATE INDEX idx_batch_trans_job ON batch_transaction_types(job_config_id, processing_order);
CREATE INDEX idx_batch_field_trans ON batch_field_mappings(transaction_type_id, field_position);

-- ================================================================
-- SEQUENCES
-- ================================================================

CREATE SEQUENCE CM3INT.seq_batch_execution_id START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE CM3INT.seq_batch_task_id START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE CM3INT.seq_batch_staging_id START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE CM3INT.seq_batch_audit_id START WITH 1 INCREMENT BY 1;

-- ================================================================
-- GRANTS
-- ================================================================

GRANT SELECT, INSERT, UPDATE ON CM3INT.batch_job_configs TO fabric_app_user;
GRANT SELECT, INSERT, UPDATE ON CM3INT.batch_transaction_types TO fabric_app_user;
GRANT SELECT, INSERT, UPDATE ON CM3INT.batch_field_mappings TO fabric_app_user;
GRANT SELECT, INSERT, UPDATE ON CM3INT.batch_header_configs TO fabric_app_user;
GRANT SELECT, INSERT, UPDATE ON CM3INT.batch_footer_configs TO fabric_app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON CM3INT.batch_job_executions TO fabric_app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON CM3INT.batch_parallel_tasks TO fabric_app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON CM3INT.batch_temp_staging TO fabric_app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON CM3INT.batch_file_summary TO fabric_app_user;
GRANT SELECT, INSERT ON CM3INT.batch_audit_log TO fabric_app_user;

GRANT SELECT ON CM3INT.seq_batch_execution_id TO fabric_app_user;
GRANT SELECT ON CM3INT.seq_batch_task_id TO fabric_app_user;
GRANT SELECT ON CM3INT.seq_batch_staging_id TO fabric_app_user;
GRANT SELECT ON CM3INT.seq_batch_audit_id TO fabric_app_user;

-- ================================================================
-- END OF SCHEMA
-- ================================================================