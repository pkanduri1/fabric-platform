-- ============================================================================
-- Fabric Platform - CM3INT Schema DDL
-- ============================================================================
-- Database: Oracle 19c+ (localhost:1521/ORCLPDB1)
-- Schema: CM3INT
-- Generated: October 5, 2025 17:41:17 EDT
-- Source: Live database snapshot from application-local.properties
-- Version: 1.0
-- ============================================================================

-- This DDL script contains the complete schema definition for the CM3INT
-- database schema used by the Fabric Platform batch processing system.
--
-- The schema includes:
-- - Core Configuration Tables (3 tables)
-- - Execution & Monitoring Tables (2 tables)
-- - Template & Mapping Tables (5 tables)
-- - Audit & Compliance Tables (3 tables)
-- - Spring Batch Framework Tables (6 tables)
-- - Reference Data Tables (3 tables)
-- ============================================================================

-- ============================================================================
-- SECTION 1: CORE CONFIGURATION TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- BATCH_CONFIGURATIONS
-- Purpose: Core job configuration storage with JSON-based field mappings
-- Rows: 7 | Last Analyzed: 2025-09-08 22:52:46
-- ----------------------------------------------------------------------------
CREATE TABLE BATCH_CONFIGURATIONS (
    ID                  VARCHAR2(100)   NOT NULL,
    SOURCE_SYSTEM       VARCHAR2(50)    NOT NULL,
    JOB_NAME            VARCHAR2(50)    NOT NULL,
    TRANSACTION_TYPE    VARCHAR2(20)    NULL,
    DESCRIPTION         VARCHAR2(500)   NULL,
    CONFIGURATION_JSON  CLOB            NOT NULL,
    CREATED_BY          VARCHAR2(50)    NOT NULL,
    CREATED_DATE        TIMESTAMP(6)    NULL,
    MODIFIED_BY         VARCHAR2(50)    NULL,
    MODIFIED_DATE       TIMESTAMP(6)    NULL,
    VERSION             NUMBER          NULL,
    ENABLED             VARCHAR2(1)     NULL,

    CONSTRAINT PK_BATCH_CONFIGURATIONS PRIMARY KEY (ID),
    CONSTRAINT UK_BATCH_CONFIG_UNIQUE UNIQUE (SOURCE_SYSTEM, JOB_NAME, TRANSACTION_TYPE)
);

-- Indexes
CREATE INDEX IDX_BATCH_CONFIG_SYSTEM_JOB ON BATCH_CONFIGURATIONS(SOURCE_SYSTEM, JOB_NAME);

COMMENT ON TABLE BATCH_CONFIGURATIONS IS 'Core batch job configurations with JSON-based field mappings';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.ID IS 'Primary key - format: cfg_{sourceSystem}_{jobName}_{transactionType}';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.SOURCE_SYSTEM IS 'Source system identifier (e.g., MTG, ENCORE, DDA)';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.JOB_NAME IS 'Unique job name within the source system';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.TRANSACTION_TYPE IS 'Transaction type code (e.g., 200, 300, 400)';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.DESCRIPTION IS 'Human-readable description of the job configuration';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.CONFIGURATION_JSON IS 'Complete job configuration including field mappings, transformations, and output specifications';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.CREATED_BY IS 'User who created this configuration';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.CREATED_DATE IS 'Timestamp when configuration was created';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.MODIFIED_BY IS 'User who last modified this configuration';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.MODIFIED_DATE IS 'Timestamp of last modification';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.VERSION IS 'Optimistic locking version number';
COMMENT ON COLUMN BATCH_CONFIGURATIONS.ENABLED IS 'Y/N flag indicating if configuration is active';

-- ----------------------------------------------------------------------------
-- MANUAL_JOB_CONFIG
-- Purpose: Manual job configuration for US001 feature
-- Rows: 4 | Last Analyzed: 2025-08-25 22:00:03
-- ----------------------------------------------------------------------------
CREATE TABLE MANUAL_JOB_CONFIG (
    CONFIG_ID               VARCHAR2(50)    NOT NULL,
    JOB_NAME                VARCHAR2(100)   NOT NULL,
    JOB_TYPE                VARCHAR2(50)    NOT NULL,
    SOURCE_SYSTEM           VARCHAR2(50)    NOT NULL,
    TARGET_SYSTEM           VARCHAR2(50)    NOT NULL,
    JOB_PARAMETERS          CLOB            NOT NULL,
    SCHEDULE_EXPRESSION     VARCHAR2(100)   NULL,
    STATUS                  VARCHAR2(20)    NOT NULL,
    VALIDATION_RULES        CLOB            NULL,
    ERROR_THRESHOLD         NUMBER(5,2)     NOT NULL,
    RETRY_COUNT             NUMBER(2,0)     NOT NULL,
    NOTIFICATION_CONFIG     CLOB            NULL,
    CREATED_BY              VARCHAR2(50)    NOT NULL,
    CREATED_DATE            TIMESTAMP(6)    NOT NULL,
    UPDATED_BY              VARCHAR2(50)    NULL,
    UPDATED_DATE            TIMESTAMP(6)    NULL,
    VERSION_DECIMAL         NUMBER(10,0)    NOT NULL,
    MASTER_QUERY_ID         NUMBER(19,0)    NULL,

    CONSTRAINT PK_MANUAL_JOB_CONFIG PRIMARY KEY (CONFIG_ID),
    CONSTRAINT CHK_MANUAL_JOB_STATUS CHECK (STATUS IN ('ACTIVE', 'INACTIVE', 'DRAFT', 'ARCHIVED')),
    CONSTRAINT CHK_MANUAL_JOB_ERROR_THRESHOLD CHECK (ERROR_THRESHOLD >= 0 AND ERROR_THRESHOLD <= 100),
    CONSTRAINT CHK_MANUAL_JOB_RETRY_COUNT CHECK (RETRY_COUNT >= 0 AND RETRY_COUNT <= 10)
);

-- Indexes
CREATE INDEX IDX_MANUAL_JOB_CONFIG_STATUS ON MANUAL_JOB_CONFIG(STATUS);
CREATE INDEX IDX_MANUAL_JOB_CONFIG_SYSTEM ON MANUAL_JOB_CONFIG(SOURCE_SYSTEM);
CREATE INDEX IDX_MANUAL_JOB_CONFIG_TYPE ON MANUAL_JOB_CONFIG(JOB_TYPE);
CREATE INDEX IDX_MANUAL_JOB_CONFIG_CREATED ON MANUAL_JOB_CONFIG(CREATED_DATE);
CREATE UNIQUE INDEX UK_MANUAL_JOB_CONFIG_ACTIVE_NAME ON MANUAL_JOB_CONFIG(JOB_NAME, STATUS) WHERE STATUS = 'ACTIVE';

COMMENT ON TABLE MANUAL_JOB_CONFIG IS 'Manual job configuration management with RBAC support';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.CONFIG_ID IS 'Primary key - unique configuration identifier';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.JOB_NAME IS 'Unique job name for manual execution';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.JOB_TYPE IS 'Type of job (e.g., BATCH_EXPORT, DATA_TRANSFORM)';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.SOURCE_SYSTEM IS 'Source system identifier (FK to SOURCE_SYSTEMS)';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.TARGET_SYSTEM IS 'Target system identifier';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.JOB_PARAMETERS IS 'JSON configuration parameters for job execution';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.SCHEDULE_EXPRESSION IS 'Cron expression for scheduled execution (optional)';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.STATUS IS 'Configuration status: ACTIVE, INACTIVE, DRAFT, ARCHIVED';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.VALIDATION_RULES IS 'JSON validation rules for job parameters';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.ERROR_THRESHOLD IS 'Maximum error percentage before job fails (0-100)';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.RETRY_COUNT IS 'Number of retry attempts on failure (0-10)';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.NOTIFICATION_CONFIG IS 'JSON notification configuration for job events';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.CREATED_BY IS 'User who created this configuration';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.CREATED_DATE IS 'Timestamp when configuration was created';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.UPDATED_BY IS 'User who last updated this configuration';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.UPDATED_DATE IS 'Timestamp of last update';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.VERSION_DECIMAL IS 'Version number for optimistic locking';
COMMENT ON COLUMN MANUAL_JOB_CONFIG.MASTER_QUERY_ID IS 'Foreign key to MASTER_QUERY_CONFIG';

-- ----------------------------------------------------------------------------
-- MASTER_QUERY_CONFIG
-- Purpose: SQL query storage with versioning for batch jobs
-- Rows: 4 | Last Analyzed: 2025-08-26 23:32:43
-- ----------------------------------------------------------------------------
CREATE TABLE MASTER_QUERY_CONFIG (
    ID              NUMBER(19,0)    NOT NULL,
    SOURCE_SYSTEM   VARCHAR2(50)    NOT NULL,
    JOB_NAME        VARCHAR2(100)   NOT NULL,
    QUERY_TEXT      CLOB            NOT NULL,
    VERSION         NUMBER(10,0)    NOT NULL,
    IS_ACTIVE       VARCHAR2(1)     NOT NULL,
    CREATED_BY      VARCHAR2(50)    NOT NULL,
    CREATED_DATE    TIMESTAMP(6)    NOT NULL,
    MODIFIED_BY     VARCHAR2(50)    NULL,
    MODIFIED_DATE   TIMESTAMP(6)    NULL,

    CONSTRAINT PK_MASTER_QUERY_CONFIG PRIMARY KEY (ID),
    CONSTRAINT UK_MASTER_QUERY UNIQUE (SOURCE_SYSTEM, JOB_NAME, VERSION)
);

-- Indexes
CREATE INDEX IDX_MASTER_QUERY_ACTIVE ON MASTER_QUERY_CONFIG(SOURCE_SYSTEM, JOB_NAME, IS_ACTIVE);

COMMENT ON TABLE MASTER_QUERY_CONFIG IS 'Master query definitions with version control';
COMMENT ON COLUMN MASTER_QUERY_CONFIG.ID IS 'Primary key - unique query identifier';
COMMENT ON COLUMN MASTER_QUERY_CONFIG.SOURCE_SYSTEM IS 'Source system identifier';
COMMENT ON COLUMN MASTER_QUERY_CONFIG.JOB_NAME IS 'Associated job name';
COMMENT ON COLUMN MASTER_QUERY_CONFIG.QUERY_TEXT IS 'SQL query text with parameter placeholders';
COMMENT ON COLUMN MASTER_QUERY_CONFIG.VERSION IS 'Query version number for change tracking';
COMMENT ON COLUMN MASTER_QUERY_CONFIG.IS_ACTIVE IS 'Y/N flag indicating if this version is active';
COMMENT ON COLUMN MASTER_QUERY_CONFIG.CREATED_BY IS 'User who created this query version';
COMMENT ON COLUMN MASTER_QUERY_CONFIG.CREATED_DATE IS 'Timestamp when query was created';
COMMENT ON COLUMN MASTER_QUERY_CONFIG.MODIFIED_BY IS 'User who last modified this query';
COMMENT ON COLUMN MASTER_QUERY_CONFIG.MODIFIED_DATE IS 'Timestamp of last modification';

-- ============================================================================
-- SECTION 2: EXECUTION & MONITORING TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- BATCH_EXECUTION_RESULTS
-- Purpose: Track batch job execution results and metrics
-- Rows: 0 | Last Analyzed: 2025-08-15 22:00:05
-- ----------------------------------------------------------------------------
CREATE TABLE BATCH_EXECUTION_RESULTS (
    ID                  NUMBER(19,0)    NOT NULL,
    JOB_CONFIG_ID       VARCHAR2(50)    NOT NULL,
    EXECUTION_ID        VARCHAR2(100)   NOT NULL,
    SOURCE_SYSTEM       VARCHAR2(50)    NOT NULL,
    RECORDS_PROCESSED   NUMBER(10,0)    NULL,
    OUTPUT_FILE_NAME    VARCHAR2(255)   NULL,
    OUTPUT_FILE_PATH    VARCHAR2(500)   NULL,
    STATUS              VARCHAR2(20)    NULL,
    START_TIME          TIMESTAMP(6)    NULL,
    END_TIME            TIMESTAMP(6)    NULL,
    ERROR_MESSAGE       CLOB            NULL,
    CORRELATION_ID      VARCHAR2(100)   NULL,

    CONSTRAINT PK_BATCH_EXECUTION_RESULTS PRIMARY KEY (ID),
    CONSTRAINT UK_BATCH_EXEC_ID UNIQUE (EXECUTION_ID)
);

-- Indexes
CREATE INDEX IDX_BATCH_EXEC_CONFIG ON BATCH_EXECUTION_RESULTS(JOB_CONFIG_ID);
CREATE INDEX IDX_BATCH_EXEC_STATUS ON BATCH_EXECUTION_RESULTS(STATUS, START_TIME);

COMMENT ON TABLE BATCH_EXECUTION_RESULTS IS 'Batch job execution tracking with file generation metrics';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.ID IS 'Primary key - auto-generated execution result ID';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.JOB_CONFIG_ID IS 'Foreign key to batch configuration';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.EXECUTION_ID IS 'Unique execution identifier - format: exec_{jobName}_{timestamp}';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.SOURCE_SYSTEM IS 'Source system identifier';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.RECORDS_PROCESSED IS 'Total number of records processed';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.OUTPUT_FILE_NAME IS 'Generated output file name';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.OUTPUT_FILE_PATH IS 'Full path to generated output file';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.STATUS IS 'Execution status: PENDING, RUNNING, COMPLETED, FAILED';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.START_TIME IS 'Execution start timestamp';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.END_TIME IS 'Execution end timestamp';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.ERROR_MESSAGE IS 'Error message if execution failed';
COMMENT ON COLUMN BATCH_EXECUTION_RESULTS.CORRELATION_ID IS 'Correlation ID for distributed tracing';

-- ----------------------------------------------------------------------------
-- MANUAL_JOB_EXECUTION
-- Purpose: Manual job execution history with comprehensive metrics
-- Rows: 0 | Last Analyzed: 2025-08-14 22:00:03
-- ----------------------------------------------------------------------------
CREATE TABLE MANUAL_JOB_EXECUTION (
    EXECUTION_ID                VARCHAR2(50)    NOT NULL,
    CONFIG_ID                   VARCHAR2(50)    NOT NULL,
    JOB_NAME                    VARCHAR2(100)   NOT NULL,
    EXECUTION_TYPE              VARCHAR2(20)    NOT NULL,
    TRIGGER_SOURCE              VARCHAR2(50)    NOT NULL,
    STATUS                      VARCHAR2(20)    NOT NULL,
    START_TIME                  TIMESTAMP(6)    NOT NULL,
    END_TIME                    TIMESTAMP(6)    NULL,
    DURATION_SECONDS            NUMBER(10,2)    NULL,
    RECORDS_PROCESSED           NUMBER(15,0)    NULL,
    RECORDS_SUCCESS             NUMBER(15,0)    NULL,
    RECORDS_ERROR               NUMBER(15,0)    NULL,
    ERROR_PERCENTAGE            NUMBER(5,2)     NULL,
    ERROR_MESSAGE               VARCHAR2(4000)  NULL,
    ERROR_STACK_TRACE           CLOB            NULL,
    RETRY_COUNT                 NUMBER(2,0)     NOT NULL,
    EXECUTION_PARAMETERS        CLOB            NULL,
    EXECUTION_LOG               CLOB            NULL,
    OUTPUT_LOCATION             VARCHAR2(500)   NULL,
    CORRELATION_ID              VARCHAR2(100)   NULL,
    MONITORING_ALERTS_SENT      CHAR(1)         NOT NULL,
    EXECUTED_BY                 VARCHAR2(50)    NOT NULL,
    EXECUTION_HOST              VARCHAR2(100)   NULL,
    EXECUTION_ENVIRONMENT       VARCHAR2(20)    NOT NULL,
    CREATED_DATE                TIMESTAMP(6)    NOT NULL,

    CONSTRAINT PK_MANUAL_JOB_EXECUTION PRIMARY KEY (EXECUTION_ID),
    CONSTRAINT FK_MANUAL_JOB_EXECUTION_CONFIG FOREIGN KEY (CONFIG_ID) REFERENCES MANUAL_JOB_CONFIG(CONFIG_ID),
    CONSTRAINT CHK_MANUAL_EXECUTION_TYPE CHECK (EXECUTION_TYPE IN ('MANUAL', 'SCHEDULED', 'RETRY', 'RERUN')),
    CONSTRAINT CHK_MANUAL_EXECUTION_STATUS CHECK (STATUS IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT CHK_MANUAL_EXECUTION_ALERTS CHECK (MONITORING_ALERTS_SENT IN ('Y', 'N')),
    CONSTRAINT CHK_MANUAL_EXECUTION_ERROR_PCT CHECK (ERROR_PERCENTAGE >= 0 AND ERROR_PERCENTAGE <= 100)
);

-- Indexes
CREATE INDEX IDX_MANUAL_JOB_EXEC_STATUS ON MANUAL_JOB_EXECUTION(STATUS);
CREATE INDEX IDX_MANUAL_JOB_EXEC_START_TIME ON MANUAL_JOB_EXECUTION(START_TIME);
CREATE INDEX IDX_MANUAL_JOB_EXEC_CORRELATION ON MANUAL_JOB_EXECUTION(CORRELATION_ID);
CREATE INDEX IDX_MANUAL_JOB_EXEC_NAME ON MANUAL_JOB_EXECUTION(JOB_NAME);
CREATE INDEX IDX_MANUAL_JOB_EXEC_ENV ON MANUAL_JOB_EXECUTION(EXECUTION_ENVIRONMENT);
CREATE INDEX IDX_MANUAL_JOB_EXEC_ACTIVE ON MANUAL_JOB_EXECUTION(STATUS, START_TIME) WHERE STATUS IN ('PENDING', 'RUNNING');

COMMENT ON TABLE MANUAL_JOB_EXECUTION IS 'Manual job execution history with comprehensive performance metrics';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.EXECUTION_ID IS 'Primary key - unique execution identifier';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.CONFIG_ID IS 'Foreign key to MANUAL_JOB_CONFIG';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.JOB_NAME IS 'Job name for easier querying';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.EXECUTION_TYPE IS 'Execution type: MANUAL, SCHEDULED, RETRY, RERUN';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.TRIGGER_SOURCE IS 'What triggered this execution (e.g., UI, API, SCHEDULER)';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.STATUS IS 'Execution status: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.START_TIME IS 'Execution start timestamp';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.END_TIME IS 'Execution end timestamp';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.DURATION_SECONDS IS 'Total execution duration in seconds';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.RECORDS_PROCESSED IS 'Total number of records processed';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.RECORDS_SUCCESS IS 'Number of successfully processed records';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.RECORDS_ERROR IS 'Number of records that failed processing';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.ERROR_PERCENTAGE IS 'Calculated error percentage';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.ERROR_MESSAGE IS 'Primary error message if execution failed';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.ERROR_STACK_TRACE IS 'Full error stack trace for debugging';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.RETRY_COUNT IS 'Number of retry attempts made';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.EXECUTION_PARAMETERS IS 'JSON execution parameters (e.g., batchDate)';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.EXECUTION_LOG IS 'Detailed execution log';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.OUTPUT_LOCATION IS 'Location of generated output files';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.CORRELATION_ID IS 'Correlation ID for distributed tracing';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.MONITORING_ALERTS_SENT IS 'Y/N flag indicating if monitoring alerts were sent';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.EXECUTED_BY IS 'User who executed this job';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.EXECUTION_HOST IS 'Server hostname where job executed';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.EXECUTION_ENVIRONMENT IS 'Environment: DEV, QA, UAT, PROD';
COMMENT ON COLUMN MANUAL_JOB_EXECUTION.CREATED_DATE IS 'Record creation timestamp';

-- ============================================================================
-- SECTION 3: TEMPLATE & MAPPING TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- FIELD_TEMPLATES
-- Purpose: Reusable field definitions for output file generation
-- Rows: 366 | Last Analyzed: 2025-08-05 22:00:06
-- ----------------------------------------------------------------------------
CREATE TABLE FIELD_TEMPLATES (
    FILE_TYPE           VARCHAR2(10)    NOT NULL,
    TRANSACTION_TYPE    VARCHAR2(10)    NOT NULL,
    FIELD_NAME          VARCHAR2(50)    NOT NULL,
    TARGET_POSITION     NUMBER          NOT NULL,
    LENGTH              NUMBER          NULL,
    DATA_TYPE           VARCHAR2(20)    NULL,
    FORMAT              VARCHAR2(50)    NULL,
    REQUIRED            VARCHAR2(1)     NULL,
    DESCRIPTION         VARCHAR2(500)   NULL,
    CREATED_BY          VARCHAR2(50)    NOT NULL,
    CREATED_DATE        TIMESTAMP(6)    NULL,
    MODIFIED_BY         VARCHAR2(50)    NULL,
    MODIFIED_DATE       TIMESTAMP(6)    NULL,
    VERSION             NUMBER          NULL,
    ENABLED             VARCHAR2(1)     NULL,

    CONSTRAINT PK_FIELD_TEMPLATES PRIMARY KEY (FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME)
);

COMMENT ON TABLE FIELD_TEMPLATES IS 'Reusable field template definitions for fixed-width file generation';
COMMENT ON COLUMN FIELD_TEMPLATES.FILE_TYPE IS 'File type identifier (e.g., ATOCTRAN, LNMASTER)';
COMMENT ON COLUMN FIELD_TEMPLATES.TRANSACTION_TYPE IS 'Transaction type code (e.g., 200, 300, 400)';
COMMENT ON COLUMN FIELD_TEMPLATES.FIELD_NAME IS 'Unique field name within the template';
COMMENT ON COLUMN FIELD_TEMPLATES.TARGET_POSITION IS 'Field position in output file (1-based)';
COMMENT ON COLUMN FIELD_TEMPLATES.LENGTH IS 'Field length in characters';
COMMENT ON COLUMN FIELD_TEMPLATES.DATA_TYPE IS 'Data type: STRING, NUMBER, DATE, BOOLEAN';
COMMENT ON COLUMN FIELD_TEMPLATES.FORMAT IS 'Format specification (e.g., YYYYMMDD for dates)';
COMMENT ON COLUMN FIELD_TEMPLATES.REQUIRED IS 'Y/N flag indicating if field is mandatory';
COMMENT ON COLUMN FIELD_TEMPLATES.DESCRIPTION IS 'Field description and usage notes';
COMMENT ON COLUMN FIELD_TEMPLATES.CREATED_BY IS 'User who created this template field';
COMMENT ON COLUMN FIELD_TEMPLATES.CREATED_DATE IS 'Timestamp when field was created';
COMMENT ON COLUMN FIELD_TEMPLATES.MODIFIED_BY IS 'User who last modified this field';
COMMENT ON COLUMN FIELD_TEMPLATES.MODIFIED_DATE IS 'Timestamp of last modification';
COMMENT ON COLUMN FIELD_TEMPLATES.VERSION IS 'Version number for change tracking';
COMMENT ON COLUMN FIELD_TEMPLATES.ENABLED IS 'Y/N flag indicating if field is active';

-- ----------------------------------------------------------------------------
-- FILE_TYPE_TEMPLATES
-- Purpose: File type template definitions
-- Rows: 8 | Last Analyzed: 2025-08-11 22:00:05
-- ----------------------------------------------------------------------------
CREATE TABLE FILE_TYPE_TEMPLATES (
    FILE_TYPE       VARCHAR2(50)    NOT NULL,
    DESCRIPTION     VARCHAR2(200)   NULL,
    TOTAL_FIELDS    NUMBER          NULL,
    RECORD_LENGTH   NUMBER          NULL,
    CREATED_BY      VARCHAR2(50)    NOT NULL,
    CREATED_DATE    TIMESTAMP(6)    NULL,
    MODIFIED_BY     VARCHAR2(50)    NULL,
    MODIFIED_DATE   TIMESTAMP(6)    NULL,
    VERSION         NUMBER          NULL,
    ENABLED         VARCHAR2(1)     NULL,

    CONSTRAINT PK_FILE_TYPE_TEMPLATES PRIMARY KEY (FILE_TYPE)
);

-- Indexes
CREATE INDEX IDX_FILE_TYPE_TEMPLATES_ENABLED ON FILE_TYPE_TEMPLATES(ENABLED);

COMMENT ON TABLE FILE_TYPE_TEMPLATES IS 'File type template metadata and specifications';
COMMENT ON COLUMN FILE_TYPE_TEMPLATES.FILE_TYPE IS 'Primary key - unique file type identifier';
COMMENT ON COLUMN FILE_TYPE_TEMPLATES.DESCRIPTION IS 'File type description and purpose';
COMMENT ON COLUMN FILE_TYPE_TEMPLATES.TOTAL_FIELDS IS 'Total number of fields in this file type';
COMMENT ON COLUMN FILE_TYPE_TEMPLATES.RECORD_LENGTH IS 'Total record length in characters';
COMMENT ON COLUMN FILE_TYPE_TEMPLATES.CREATED_BY IS 'User who created this template';
COMMENT ON COLUMN FILE_TYPE_TEMPLATES.CREATED_DATE IS 'Timestamp when template was created';
COMMENT ON COLUMN FILE_TYPE_TEMPLATES.MODIFIED_BY IS 'User who last modified this template';
COMMENT ON COLUMN FILE_TYPE_TEMPLATES.MODIFIED_DATE IS 'Timestamp of last modification';
COMMENT ON COLUMN FILE_TYPE_TEMPLATES.VERSION IS 'Version number for change tracking';
COMMENT ON COLUMN FILE_TYPE_TEMPLATES.ENABLED IS 'Y/N flag indicating if template is active';

-- ----------------------------------------------------------------------------
-- TEMPLATE_SOURCE_MAPPINGS
-- Purpose: Template-based source-to-target field mappings
-- Rows: 45 | Last Analyzed: 2025-09-08 22:52:48
-- ----------------------------------------------------------------------------
CREATE TABLE TEMPLATE_SOURCE_MAPPINGS (
    ID                      NUMBER          NOT NULL,
    FILE_TYPE               VARCHAR2(50)    NOT NULL,
    TRANSACTION_TYPE        VARCHAR2(10)    NOT NULL,
    SOURCE_SYSTEM_ID        VARCHAR2(50)    NOT NULL,
    JOB_NAME                VARCHAR2(100)   NOT NULL,
    TARGET_FIELD_NAME       VARCHAR2(50)    NOT NULL,
    SOURCE_FIELD_NAME       VARCHAR2(100)   NULL,
    TRANSFORMATION_TYPE     VARCHAR2(20)    NULL,
    TRANSFORMATION_CONFIG   VARCHAR2(1000)  NULL,
    TARGET_POSITION         NUMBER          NULL,
    LENGTH                  NUMBER          NULL,
    DATA_TYPE               VARCHAR2(20)    NULL,
    CREATED_BY              VARCHAR2(50)    NOT NULL,
    CREATED_DATE            TIMESTAMP(6)    NULL,
    MODIFIED_BY             VARCHAR2(50)    NULL,
    MODIFIED_DATE           TIMESTAMP(6)    NULL,
    VERSION                 NUMBER          NULL,
    ENABLED                 VARCHAR2(1)     NULL,
    VALUE                   VARCHAR2(1000)  NULL,
    DEFAULT_VALUE           VARCHAR2(1000)  NULL,

    CONSTRAINT PK_TEMPLATE_SOURCE_MAPPINGS PRIMARY KEY (ID)
);

COMMENT ON TABLE TEMPLATE_SOURCE_MAPPINGS IS 'Template-based field mappings with transformation rules';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.ID IS 'Primary key - auto-generated mapping ID';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.FILE_TYPE IS 'Target file type';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.TRANSACTION_TYPE IS 'Transaction type code';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.SOURCE_SYSTEM_ID IS 'Source system identifier';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.JOB_NAME IS 'Associated job name';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.TARGET_FIELD_NAME IS 'Target field name in output file';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.SOURCE_FIELD_NAME IS 'Source field name from database query';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.TRANSFORMATION_TYPE IS 'Transformation type: source, constant, composite, conditional, blank';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.TRANSFORMATION_CONFIG IS 'JSON transformation configuration';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.TARGET_POSITION IS 'Field position in output file';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.LENGTH IS 'Field length in characters';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.DATA_TYPE IS 'Data type: STRING, NUMBER, DATE';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.CREATED_BY IS 'User who created this mapping';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.CREATED_DATE IS 'Timestamp when mapping was created';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.MODIFIED_BY IS 'User who last modified this mapping';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.MODIFIED_DATE IS 'Timestamp of last modification';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.VERSION IS 'Version number for change tracking';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.ENABLED IS 'Y/N flag indicating if mapping is active';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.VALUE IS 'Constant value for transformation_type=constant fields';
COMMENT ON COLUMN TEMPLATE_SOURCE_MAPPINGS.DEFAULT_VALUE IS 'Fallback value when source is null';

-- ----------------------------------------------------------------------------
-- TEMPLATE_MASTER_QUERY_MAPPING
-- Purpose: Links templates to master queries with security controls
-- Rows: 0 | Last Analyzed: 2025-08-19 22:00:03
-- ----------------------------------------------------------------------------
CREATE TABLE TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID                      VARCHAR2(50)    NOT NULL,
    CONFIG_ID                       VARCHAR2(50)    NOT NULL,
    MASTER_QUERY_ID                 VARCHAR2(50)    NOT NULL,
    QUERY_NAME                      VARCHAR2(100)   NOT NULL,
    QUERY_SQL                       CLOB            NOT NULL,
    QUERY_DESCRIPTION               VARCHAR2(500)   NULL,
    QUERY_TYPE                      VARCHAR2(50)    NOT NULL,
    MAX_EXECUTION_TIME_SECONDS      NUMBER(3,0)     NOT NULL,
    MAX_RESULT_ROWS                 NUMBER(5,0)     NOT NULL,
    QUERY_PARAMETERS                CLOB            NULL,
    PARAMETER_VALIDATION_RULES      CLOB            NULL,
    STATUS                          VARCHAR2(20)    NOT NULL,
    IS_READ_ONLY                    CHAR(1)         NOT NULL,
    SECURITY_CLASSIFICATION         VARCHAR2(20)    NOT NULL,
    REQUIRES_APPROVAL               CHAR(1)         NOT NULL,
    CREATED_BY                      VARCHAR2(50)    NOT NULL,
    CREATED_DATE                    TIMESTAMP(6)    NOT NULL,
    UPDATED_BY                      VARCHAR2(50)    NULL,
    UPDATED_DATE                    TIMESTAMP(6)    NULL,
    CORRELATION_ID                  VARCHAR2(50)    NULL,

    CONSTRAINT PK_TEMPLATE_MASTER_QUERY_MAPPING PRIMARY KEY (MAPPING_ID)
);

COMMENT ON TABLE TEMPLATE_MASTER_QUERY_MAPPING IS 'Template-to-master-query mappings with security and governance';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.MAPPING_ID IS 'Primary key - unique mapping identifier';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.CONFIG_ID IS 'Configuration identifier';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.MASTER_QUERY_ID IS 'Foreign key to MASTER_QUERY_CONFIG';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.QUERY_NAME IS 'Query name for identification';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.QUERY_SQL IS 'SQL query text';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.QUERY_DESCRIPTION IS 'Query purpose and usage description';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.QUERY_TYPE IS 'Query type: SELECT, INSERT, UPDATE, DELETE';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.MAX_EXECUTION_TIME_SECONDS IS 'Maximum allowed execution time in seconds';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.MAX_RESULT_ROWS IS 'Maximum number of result rows allowed';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.QUERY_PARAMETERS IS 'JSON query parameter definitions';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.PARAMETER_VALIDATION_RULES IS 'JSON parameter validation rules';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.STATUS IS 'Query status: ACTIVE, INACTIVE, DEPRECATED';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.IS_READ_ONLY IS 'Y/N flag enforcing read-only execution';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.SECURITY_CLASSIFICATION IS 'Security level: PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.REQUIRES_APPROVAL IS 'Y/N flag requiring approval before execution';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.CREATED_BY IS 'User who created this mapping';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.CREATED_DATE IS 'Timestamp when mapping was created';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.UPDATED_BY IS 'User who last updated this mapping';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.UPDATED_DATE IS 'Timestamp of last update';
COMMENT ON COLUMN TEMPLATE_MASTER_QUERY_MAPPING.CORRELATION_ID IS 'Correlation ID for tracing';

-- ----------------------------------------------------------------------------
-- MASTER_QUERY_COLUMNS
-- Purpose: Column metadata for master queries
-- Rows: 0 | Last Analyzed: 2025-08-19 22:00:03
-- ----------------------------------------------------------------------------
CREATE TABLE MASTER_QUERY_COLUMNS (
    COLUMN_ID               VARCHAR2(50)    NOT NULL,
    MASTER_QUERY_ID         VARCHAR2(50)    NOT NULL,
    COLUMN_NAME             VARCHAR2(100)   NOT NULL,
    COLUMN_ALIAS            VARCHAR2(100)   NULL,
    COLUMN_TYPE             VARCHAR2(50)    NOT NULL,
    COLUMN_LENGTH           NUMBER(10,0)    NULL,
    COLUMN_PRECISION        NUMBER(5,0)     NULL,
    COLUMN_SCALE            NUMBER(5,0)     NULL,
    IS_NULLABLE             CHAR(1)         NOT NULL,
    IS_PRIMARY_KEY          CHAR(1)         NOT NULL,
    COLUMN_ORDER            NUMBER(3,0)     NOT NULL,
    VALIDATION_RULES        CLOB            NULL,
    DISPLAY_FORMAT          VARCHAR2(100)   NULL,
    COLUMN_DESCRIPTION      VARCHAR2(500)   NULL,
    IS_SENSITIVE_DATA       CHAR(1)         NOT NULL,
    DATA_CLASSIFICATION     VARCHAR2(20)    NOT NULL,
    CREATED_BY              VARCHAR2(50)    NOT NULL,
    CREATED_DATE            TIMESTAMP(6)    NOT NULL,
    UPDATED_BY              VARCHAR2(50)    NULL,
    UPDATED_DATE            TIMESTAMP(6)    NULL,

    CONSTRAINT PK_MASTER_QUERY_COLUMNS PRIMARY KEY (COLUMN_ID)
);

COMMENT ON TABLE MASTER_QUERY_COLUMNS IS 'Column metadata and data classification for master queries';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.COLUMN_ID IS 'Primary key - unique column identifier';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.MASTER_QUERY_ID IS 'Foreign key to MASTER_QUERY_CONFIG';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.COLUMN_NAME IS 'Database column name';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.COLUMN_ALIAS IS 'Column alias in query result';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.COLUMN_TYPE IS 'Column data type';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.COLUMN_LENGTH IS 'Maximum column length';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.COLUMN_PRECISION IS 'Numeric precision';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.COLUMN_SCALE IS 'Numeric scale';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.IS_NULLABLE IS 'Y/N flag indicating if column allows nulls';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.IS_PRIMARY_KEY IS 'Y/N flag indicating if column is primary key';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.COLUMN_ORDER IS 'Column order in result set';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.VALIDATION_RULES IS 'JSON validation rules for column values';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.DISPLAY_FORMAT IS 'Display format for UI rendering';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.COLUMN_DESCRIPTION IS 'Column description and usage notes';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.IS_SENSITIVE_DATA IS 'Y/N flag for PII/sensitive data';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.DATA_CLASSIFICATION IS 'Data classification: PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.CREATED_BY IS 'User who created this column definition';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.CREATED_DATE IS 'Timestamp when column was created';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.UPDATED_BY IS 'User who last updated this column';
COMMENT ON COLUMN MASTER_QUERY_COLUMNS.UPDATED_DATE IS 'Timestamp of last update';

-- ============================================================================
-- SECTION 4: AUDIT & COMPLIANCE TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- CONFIGURATION_AUDIT
-- Purpose: SOX-compliant audit trail for configuration changes
-- Rows: 0 | Last Analyzed: 2025-08-01 22:14:12
-- ----------------------------------------------------------------------------
CREATE TABLE CONFIGURATION_AUDIT (
    AUDIT_ID        NUMBER          NOT NULL,
    CONFIG_ID       VARCHAR2(100)   NOT NULL,
    ACTION          VARCHAR2(20)    NOT NULL,
    OLD_VALUE       CLOB            NULL,
    NEW_VALUE       CLOB            NULL,
    CHANGED_BY      VARCHAR2(50)    NOT NULL,
    CHANGE_DATE     TIMESTAMP(6)    NULL,
    CHANGE_REASON   VARCHAR2(500)   NULL,

    CONSTRAINT PK_CONFIGURATION_AUDIT PRIMARY KEY (AUDIT_ID),
    CONSTRAINT FK_CONFIG_AUDIT_CONFIG FOREIGN KEY (CONFIG_ID) REFERENCES BATCH_CONFIGURATIONS(ID)
);

-- Indexes
CREATE INDEX IDX_CONFIG_AUDIT_CONFIG_ID ON CONFIGURATION_AUDIT(CONFIG_ID);
CREATE INDEX IDX_CONFIG_AUDIT_DATE ON CONFIGURATION_AUDIT(CHANGE_DATE);

COMMENT ON TABLE CONFIGURATION_AUDIT IS 'SOX-compliant audit trail for all configuration changes';
COMMENT ON COLUMN CONFIGURATION_AUDIT.AUDIT_ID IS 'Primary key - auto-generated audit record ID';
COMMENT ON COLUMN CONFIGURATION_AUDIT.CONFIG_ID IS 'Foreign key to BATCH_CONFIGURATIONS';
COMMENT ON COLUMN CONFIGURATION_AUDIT.ACTION IS 'Action performed: CREATE, UPDATE, DELETE, ENABLE, DISABLE';
COMMENT ON COLUMN CONFIGURATION_AUDIT.OLD_VALUE IS 'Previous configuration JSON before change';
COMMENT ON COLUMN CONFIGURATION_AUDIT.NEW_VALUE IS 'New configuration JSON after change';
COMMENT ON COLUMN CONFIGURATION_AUDIT.CHANGED_BY IS 'User who made the change';
COMMENT ON COLUMN CONFIGURATION_AUDIT.CHANGE_DATE IS 'Timestamp of the change';
COMMENT ON COLUMN CONFIGURATION_AUDIT.CHANGE_REASON IS 'Business reason for the change';

-- ----------------------------------------------------------------------------
-- MANUAL_JOB_AUDIT
-- Purpose: Comprehensive SOX audit trail with tamper detection
-- Rows: 0 | Last Analyzed: 2025-08-14 22:00:03
-- ----------------------------------------------------------------------------
CREATE TABLE MANUAL_JOB_AUDIT (
    AUDIT_ID                    VARCHAR2(50)    NOT NULL,
    CONFIG_ID                   VARCHAR2(50)    NOT NULL,
    OPERATION_TYPE              VARCHAR2(20)    NOT NULL,
    OPERATION_DESCRIPTION       VARCHAR2(500)   NULL,
    OLD_VALUES                  CLOB            NULL,
    NEW_VALUES                  CLOB            NULL,
    CHANGED_FIELDS              VARCHAR2(1000)  NULL,
    CHANGED_BY                  VARCHAR2(50)    NOT NULL,
    CHANGE_DATE                 TIMESTAMP(6)    NOT NULL,
    USER_ROLE                   VARCHAR2(50)    NOT NULL,
    CHANGE_REASON               VARCHAR2(500)   NULL,
    BUSINESS_JUSTIFICATION      VARCHAR2(1000)  NULL,
    TICKET_REFERENCE            VARCHAR2(100)   NULL,
    SESSION_ID                  VARCHAR2(100)   NULL,
    IP_ADDRESS                  VARCHAR2(45)    NULL,
    USER_AGENT                  VARCHAR2(500)   NULL,
    CLIENT_FINGERPRINT          VARCHAR2(100)   NULL,
    APPROVAL_STATUS             VARCHAR2(20)    NOT NULL,
    APPROVED_BY                 VARCHAR2(50)    NULL,
    APPROVED_DATE               TIMESTAMP(6)    NULL,
    APPROVAL_COMMENTS           VARCHAR2(1000)  NULL,
    SOX_COMPLIANCE_FLAG         CHAR(1)         NOT NULL,
    RISK_ASSESSMENT             VARCHAR2(20)    NULL,
    REGULATORY_IMPACT           VARCHAR2(500)   NULL,
    CHECKSUM                    VARCHAR2(64)    NULL,
    DIGITAL_SIGNATURE           VARCHAR2(512)   NULL,
    CORRELATION_ID              VARCHAR2(100)   NULL,
    PARENT_AUDIT_ID             VARCHAR2(50)    NULL,
    ENVIRONMENT                 VARCHAR2(20)    NOT NULL,
    APPLICATION_VERSION         VARCHAR2(20)    NULL,
    RETENTION_DATE              DATE            NULL,
    ARCHIVED_FLAG               CHAR(1)         NOT NULL,

    CONSTRAINT PK_MANUAL_JOB_AUDIT PRIMARY KEY (AUDIT_ID),
    CONSTRAINT FK_MANUAL_JOB_AUDIT_CONFIG FOREIGN KEY (CONFIG_ID) REFERENCES MANUAL_JOB_CONFIG(CONFIG_ID),
    CONSTRAINT FK_MANUAL_JOB_AUDIT_PARENT FOREIGN KEY (PARENT_AUDIT_ID) REFERENCES MANUAL_JOB_AUDIT(AUDIT_ID),
    CONSTRAINT CHK_MANUAL_AUDIT_OPERATION CHECK (OPERATION_TYPE IN ('CREATE', 'UPDATE', 'DELETE', 'EXECUTE', 'APPROVE', 'REJECT')),
    CONSTRAINT CHK_MANUAL_AUDIT_APPROVAL CHECK (APPROVAL_STATUS IN ('PENDING', 'APPROVED', 'REJECTED', 'NOT_REQUIRED')),
    CONSTRAINT CHK_MANUAL_AUDIT_SOX CHECK (SOX_COMPLIANCE_FLAG IN ('Y', 'N')),
    CONSTRAINT CHK_MANUAL_AUDIT_ARCHIVED CHECK (ARCHIVED_FLAG IN ('Y', 'N')),
    CONSTRAINT CHK_MANUAL_AUDIT_RISK CHECK (RISK_ASSESSMENT IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

COMMENT ON TABLE MANUAL_JOB_AUDIT IS 'SOX-compliant audit trail with tamper detection and digital signatures';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.AUDIT_ID IS 'Primary key - unique audit record identifier';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.CONFIG_ID IS 'Foreign key to MANUAL_JOB_CONFIG';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.OPERATION_TYPE IS 'Operation: CREATE, UPDATE, DELETE, EXECUTE, APPROVE, REJECT';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.OPERATION_DESCRIPTION IS 'Detailed description of the operation';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.OLD_VALUES IS 'JSON snapshot of values before change';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.NEW_VALUES IS 'JSON snapshot of values after change';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.CHANGED_FIELDS IS 'Comma-separated list of changed field names';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.CHANGED_BY IS 'User who performed the operation';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.CHANGE_DATE IS 'Timestamp of the operation';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.USER_ROLE IS 'Role of the user at time of operation';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.CHANGE_REASON IS 'Business reason for the change';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.BUSINESS_JUSTIFICATION IS 'Detailed business justification';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.TICKET_REFERENCE IS 'Reference to JIRA/ServiceNow ticket';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.SESSION_ID IS 'User session identifier';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.IP_ADDRESS IS 'IP address of the user';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.USER_AGENT IS 'Browser user agent string';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.CLIENT_FINGERPRINT IS 'Client device fingerprint for security';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.APPROVAL_STATUS IS 'Approval status: PENDING, APPROVED, REJECTED, NOT_REQUIRED';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.APPROVED_BY IS 'User who approved the change';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.APPROVED_DATE IS 'Timestamp of approval';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.APPROVAL_COMMENTS IS 'Approver comments';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.SOX_COMPLIANCE_FLAG IS 'Y/N flag indicating SOX compliance requirements';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.RISK_ASSESSMENT IS 'Risk level: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.REGULATORY_IMPACT IS 'Description of regulatory impact';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.CHECKSUM IS 'SHA-256 checksum for tamper detection';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.DIGITAL_SIGNATURE IS 'Digital signature for non-repudiation';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.CORRELATION_ID IS 'Correlation ID for distributed tracing';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.PARENT_AUDIT_ID IS 'Foreign key to parent audit record for change chains';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.ENVIRONMENT IS 'Environment: DEV, QA, UAT, PROD';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.APPLICATION_VERSION IS 'Application version at time of change';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.RETENTION_DATE IS 'Date when record can be archived';
COMMENT ON COLUMN MANUAL_JOB_AUDIT.ARCHIVED_FLAG IS 'Y/N flag indicating if record is archived';

-- ----------------------------------------------------------------------------
-- PIPELINE_AUDIT_LOG
-- Purpose: Data pipeline audit trail for lineage tracking
-- Rows: 0 | Last Analyzed: 2025-09-15 22:09:10
-- ----------------------------------------------------------------------------
CREATE TABLE PIPELINE_AUDIT_LOG (
    AUDIT_ID            VARCHAR2(36)    NOT NULL,
    CORRELATION_ID      VARCHAR2(36)    NOT NULL,
    SOURCE_SYSTEM       VARCHAR2(50)    NOT NULL,
    MODULE_NAME         VARCHAR2(100)   NULL,
    PROCESS_NAME        VARCHAR2(100)   NULL,
    SOURCE_ENTITY       VARCHAR2(200)   NULL,
    DESTINATION_ENTITY  VARCHAR2(200)   NULL,
    KEY_IDENTIFIER      VARCHAR2(100)   NULL,
    CHECKPOINT_STAGE    VARCHAR2(50)    NOT NULL,
    STATUS              VARCHAR2(20)    NOT NULL,
    EVENT_TIMESTAMP     TIMESTAMP(6)    NOT NULL,
    MESSAGE             VARCHAR2(1000)  NULL,
    DETAILS_JSON        CLOB            NULL,

    CONSTRAINT PK_PIPELINE_AUDIT_LOG PRIMARY KEY (AUDIT_ID),
    CONSTRAINT CHK_AUDIT_STATUS CHECK (STATUS IN ('SUCCESS', 'WARNING', 'ERROR', 'INFO')),
    CONSTRAINT CHK_CHECKPOINT_STAGE CHECK (CHECKPOINT_STAGE IN ('START', 'EXTRACT', 'TRANSFORM', 'LOAD', 'VALIDATE', 'COMPLETE', 'ERROR'))
);

-- Indexes
CREATE INDEX IDX_AUDIT_CORRELATION_ID ON PIPELINE_AUDIT_LOG(CORRELATION_ID);
CREATE INDEX IDX_AUDIT_SOURCE_SYSTEM ON PIPELINE_AUDIT_LOG(SOURCE_SYSTEM);
CREATE INDEX IDX_AUDIT_MODULE_NAME ON PIPELINE_AUDIT_LOG(MODULE_NAME);
CREATE INDEX IDX_AUDIT_EVENT_TIMESTAMP ON PIPELINE_AUDIT_LOG(EVENT_TIMESTAMP);
CREATE INDEX IDX_AUDIT_KEY_IDENTIFIER ON PIPELINE_AUDIT_LOG(KEY_IDENTIFIER);
CREATE INDEX IDX_AUDIT_CORR_STATUS ON PIPELINE_AUDIT_LOG(CORRELATION_ID, STATUS);

COMMENT ON TABLE PIPELINE_AUDIT_LOG IS 'Data pipeline execution audit trail for lineage and checkpoint tracking';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.AUDIT_ID IS 'Primary key - UUID audit record identifier';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.CORRELATION_ID IS 'UUID correlation ID for tracking related events';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.SOURCE_SYSTEM IS 'Source system identifier';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.MODULE_NAME IS 'Application module name';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.PROCESS_NAME IS 'Process or batch job name';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.SOURCE_ENTITY IS 'Source entity/table name';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.DESTINATION_ENTITY IS 'Destination entity/table name';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.KEY_IDENTIFIER IS 'Business key or record identifier';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.CHECKPOINT_STAGE IS 'Pipeline stage: START, EXTRACT, TRANSFORM, LOAD, VALIDATE, COMPLETE, ERROR';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.STATUS IS 'Event status: SUCCESS, WARNING, ERROR, INFO';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.EVENT_TIMESTAMP IS 'Event occurrence timestamp';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.MESSAGE IS 'Event message';
COMMENT ON COLUMN PIPELINE_AUDIT_LOG.DETAILS_JSON IS 'JSON detailed event information';

-- ============================================================================
-- SECTION 5: SPRING BATCH FRAMEWORK TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- BATCH_JOB_INSTANCE
-- Purpose: Spring Batch job instance metadata
-- Rows: 0 | Last Analyzed: 2025-08-15 22:00:05
-- ----------------------------------------------------------------------------
CREATE TABLE BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID     NUMBER(19,0)    NOT NULL,
    VERSION             NUMBER(19,0)    NULL,
    JOB_NAME            VARCHAR2(100)   NOT NULL,
    JOB_KEY             VARCHAR2(32)    NOT NULL,

    CONSTRAINT PK_BATCH_JOB_INSTANCE PRIMARY KEY (JOB_INSTANCE_ID),
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
);

COMMENT ON TABLE BATCH_JOB_INSTANCE IS 'Spring Batch job instance metadata';
COMMENT ON COLUMN BATCH_JOB_INSTANCE.JOB_INSTANCE_ID IS 'Primary key - unique job instance ID';
COMMENT ON COLUMN BATCH_JOB_INSTANCE.VERSION IS 'Optimistic locking version';
COMMENT ON COLUMN BATCH_JOB_INSTANCE.JOB_NAME IS 'Spring Batch job name';
COMMENT ON COLUMN BATCH_JOB_INSTANCE.JOB_KEY IS 'MD5 hash of job parameters for uniqueness';

-- ----------------------------------------------------------------------------
-- BATCH_JOB_EXECUTION
-- Purpose: Spring Batch job execution tracking
-- Rows: 0 | Last Analyzed: 2025-08-15 22:00:05
-- ----------------------------------------------------------------------------
CREATE TABLE BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID    NUMBER(19,0)    NOT NULL,
    VERSION             NUMBER(19,0)    NULL,
    JOB_INSTANCE_ID     NUMBER(19,0)    NOT NULL,
    CREATE_TIME         TIMESTAMP(6)    NOT NULL,
    START_TIME          TIMESTAMP(6)    NULL,
    END_TIME            TIMESTAMP(6)    NULL,
    STATUS              VARCHAR2(10)    NULL,
    EXIT_CODE           VARCHAR2(2500)  NULL,
    EXIT_MESSAGE        VARCHAR2(2500)  NULL,
    LAST_UPDATED        TIMESTAMP(6)    NULL,

    CONSTRAINT PK_BATCH_JOB_EXECUTION PRIMARY KEY (JOB_EXECUTION_ID),
    CONSTRAINT JOB_EXEC_INST_FK FOREIGN KEY (JOB_INSTANCE_ID) REFERENCES BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
);

-- Indexes
CREATE INDEX JOB_INST_EXEC_FK ON BATCH_JOB_EXECUTION(JOB_INSTANCE_ID);
CREATE INDEX IDX_JOB_EXEC_STATUS ON BATCH_JOB_EXECUTION(STATUS);
CREATE INDEX IDX_JOB_EXEC_CREATE_TIME ON BATCH_JOB_EXECUTION(CREATE_TIME);

COMMENT ON TABLE BATCH_JOB_EXECUTION IS 'Spring Batch job execution tracking';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.JOB_EXECUTION_ID IS 'Primary key - unique job execution ID';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.VERSION IS 'Optimistic locking version';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.JOB_INSTANCE_ID IS 'Foreign key to BATCH_JOB_INSTANCE';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.CREATE_TIME IS 'Execution creation timestamp';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.START_TIME IS 'Execution start timestamp';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.END_TIME IS 'Execution end timestamp';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.STATUS IS 'Execution status: STARTING, STARTED, STOPPING, STOPPED, FAILED, COMPLETED, ABANDONED';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.EXIT_CODE IS 'Exit code from job execution';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.EXIT_MESSAGE IS 'Exit message from job execution';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.LAST_UPDATED IS 'Last update timestamp';

-- ----------------------------------------------------------------------------
-- BATCH_JOB_EXECUTION_PARAMS
-- Purpose: Spring Batch job execution parameters
-- Rows: 0 | Last Analyzed: 2025-08-15 22:00:05
-- ----------------------------------------------------------------------------
CREATE TABLE BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID    NUMBER(19,0)    NOT NULL,
    PARAMETER_NAME      VARCHAR2(100)   NOT NULL,
    PARAMETER_TYPE      VARCHAR2(100)   NOT NULL,
    PARAMETER_VALUE     VARCHAR2(2500)  NULL,
    IDENTIFYING         CHAR(1)         NOT NULL,

    CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

-- Indexes
CREATE INDEX JOB_EXEC_PARAMS_FK ON BATCH_JOB_EXECUTION_PARAMS(JOB_EXECUTION_ID);

COMMENT ON TABLE BATCH_JOB_EXECUTION_PARAMS IS 'Spring Batch job execution parameters';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_PARAMS.JOB_EXECUTION_ID IS 'Foreign key to BATCH_JOB_EXECUTION';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_PARAMS.PARAMETER_NAME IS 'Parameter name';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_PARAMS.PARAMETER_TYPE IS 'Parameter type: STRING, LONG, DATE, DOUBLE';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_PARAMS.PARAMETER_VALUE IS 'Parameter value as string';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_PARAMS.IDENTIFYING IS 'Y/N flag indicating if parameter identifies unique job instance';

-- ----------------------------------------------------------------------------
-- BATCH_JOB_EXECUTION_CONTEXT
-- Purpose: Spring Batch job execution context storage
-- Rows: 0 | Last Analyzed: 2025-08-15 22:00:05
-- ----------------------------------------------------------------------------
CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID    NUMBER(19,0)    NOT NULL,
    SHORT_CONTEXT       VARCHAR2(2500)  NOT NULL,
    SERIALIZED_CONTEXT  CLOB            NULL,

    CONSTRAINT PK_BATCH_JOB_EXECUTION_CONTEXT PRIMARY KEY (JOB_EXECUTION_ID),
    CONSTRAINT JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

COMMENT ON TABLE BATCH_JOB_EXECUTION_CONTEXT IS 'Spring Batch job execution context storage';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_CONTEXT.JOB_EXECUTION_ID IS 'Primary key and foreign key to BATCH_JOB_EXECUTION';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_CONTEXT.SHORT_CONTEXT IS 'Short string representation of context';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_CONTEXT.SERIALIZED_CONTEXT IS 'Full serialized execution context';

-- ----------------------------------------------------------------------------
-- BATCH_STEP_EXECUTION
-- Purpose: Spring Batch step execution tracking
-- Rows: 0 | Last Analyzed: 2025-08-15 22:00:05
-- ----------------------------------------------------------------------------
CREATE TABLE BATCH_STEP_EXECUTION (
    STEP_EXECUTION_ID   NUMBER(19,0)    NOT NULL,
    VERSION             NUMBER(19,0)    NOT NULL,
    STEP_NAME           VARCHAR2(100)   NOT NULL,
    JOB_EXECUTION_ID    NUMBER(19,0)    NOT NULL,
    CREATE_TIME         TIMESTAMP(6)    NOT NULL,
    START_TIME          TIMESTAMP(6)    NULL,
    END_TIME            TIMESTAMP(6)    NULL,
    STATUS              VARCHAR2(10)    NULL,
    COMMIT_COUNT        NUMBER(19,0)    NULL,
    READ_COUNT          NUMBER(19,0)    NULL,
    FILTER_COUNT        NUMBER(19,0)    NULL,
    WRITE_COUNT         NUMBER(19,0)    NULL,
    READ_SKIP_COUNT     NUMBER(19,0)    NULL,
    WRITE_SKIP_COUNT    NUMBER(19,0)    NULL,
    PROCESS_SKIP_COUNT  NUMBER(19,0)    NULL,
    ROLLBACK_COUNT      NUMBER(19,0)    NULL,
    EXIT_CODE           VARCHAR2(2500)  NULL,
    EXIT_MESSAGE        VARCHAR2(2500)  NULL,
    LAST_UPDATED        TIMESTAMP(6)    NULL,

    CONSTRAINT PK_BATCH_STEP_EXECUTION PRIMARY KEY (STEP_EXECUTION_ID)
);

-- Indexes
CREATE INDEX JOB_EXEC_STEP_FK ON BATCH_STEP_EXECUTION(JOB_EXECUTION_ID);
CREATE INDEX IDX_STEP_EXEC_STATUS ON BATCH_STEP_EXECUTION(STATUS);

COMMENT ON TABLE BATCH_STEP_EXECUTION IS 'Spring Batch step execution tracking';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.STEP_EXECUTION_ID IS 'Primary key - unique step execution ID';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.VERSION IS 'Optimistic locking version';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.STEP_NAME IS 'Step name within the job';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.JOB_EXECUTION_ID IS 'Foreign key to BATCH_JOB_EXECUTION';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.CREATE_TIME IS 'Step creation timestamp';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.START_TIME IS 'Step start timestamp';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.END_TIME IS 'Step end timestamp';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.STATUS IS 'Step status: STARTING, STARTED, STOPPING, STOPPED, FAILED, COMPLETED, ABANDONED';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.COMMIT_COUNT IS 'Number of commits performed';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.READ_COUNT IS 'Number of items read';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.FILTER_COUNT IS 'Number of items filtered';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.WRITE_COUNT IS 'Number of items written';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.READ_SKIP_COUNT IS 'Number of items skipped on read';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.WRITE_SKIP_COUNT IS 'Number of items skipped on write';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.PROCESS_SKIP_COUNT IS 'Number of items skipped during processing';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.ROLLBACK_COUNT IS 'Number of rollbacks performed';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.EXIT_CODE IS 'Exit code from step execution';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.EXIT_MESSAGE IS 'Exit message from step execution';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.LAST_UPDATED IS 'Last update timestamp';

-- ----------------------------------------------------------------------------
-- BATCH_STEP_EXECUTION_CONTEXT
-- Purpose: Spring Batch step execution context storage
-- Rows: 0 | Last Analyzed: 2025-08-15 22:00:05
-- ----------------------------------------------------------------------------
CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID   NUMBER(19,0)    NOT NULL,
    SHORT_CONTEXT       VARCHAR2(2500)  NOT NULL,
    SERIALIZED_CONTEXT  CLOB            NULL,

    CONSTRAINT PK_BATCH_STEP_EXECUTION_CONTEX PRIMARY KEY (STEP_EXECUTION_ID),
    CONSTRAINT STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID) REFERENCES BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
);

COMMENT ON TABLE BATCH_STEP_EXECUTION_CONTEXT IS 'Spring Batch step execution context storage';
COMMENT ON COLUMN BATCH_STEP_EXECUTION_CONTEXT.STEP_EXECUTION_ID IS 'Primary key and foreign key to BATCH_STEP_EXECUTION';
COMMENT ON COLUMN BATCH_STEP_EXECUTION_CONTEXT.SHORT_CONTEXT IS 'Short string representation of context';
COMMENT ON COLUMN BATCH_STEP_EXECUTION_CONTEXT.SERIALIZED_CONTEXT IS 'Full serialized step context';

-- ============================================================================
-- SECTION 6: REFERENCE DATA TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- SOURCE_SYSTEMS
-- Purpose: Source system registry and metadata
-- Rows: 4 | Last Analyzed: 2025-09-01 22:00:04
-- ----------------------------------------------------------------------------
CREATE TABLE SOURCE_SYSTEMS (
    ID                  VARCHAR2(50)    NOT NULL,
    NAME                VARCHAR2(100)   NOT NULL,
    TYPE                VARCHAR2(20)    NOT NULL,
    DESCRIPTION         VARCHAR2(500)   NULL,
    CONNECTION_STRING   VARCHAR2(1000)  NULL,
    ENABLED             VARCHAR2(1)     NULL,
    CREATED_DATE        TIMESTAMP(6)    NULL,
    JOB_COUNT           NUMBER          NULL,

    CONSTRAINT PK_SOURCE_SYSTEMS PRIMARY KEY (ID)
);

COMMENT ON TABLE SOURCE_SYSTEMS IS 'Source system registry with connection metadata';
COMMENT ON COLUMN SOURCE_SYSTEMS.ID IS 'Primary key - unique source system identifier (e.g., MTG, ENCORE, DDA)';
COMMENT ON COLUMN SOURCE_SYSTEMS.NAME IS 'Source system display name';
COMMENT ON COLUMN SOURCE_SYSTEMS.TYPE IS 'System type: DATABASE, FILE, API, MAINFRAME';
COMMENT ON COLUMN SOURCE_SYSTEMS.DESCRIPTION IS 'System description and purpose';
COMMENT ON COLUMN SOURCE_SYSTEMS.CONNECTION_STRING IS 'Connection string or endpoint URL';
COMMENT ON COLUMN SOURCE_SYSTEMS.ENABLED IS 'Y/N flag indicating if system is active';
COMMENT ON COLUMN SOURCE_SYSTEMS.CREATED_DATE IS 'Timestamp when system was registered';
COMMENT ON COLUMN SOURCE_SYSTEMS.JOB_COUNT IS 'Number of jobs configured for this system';

-- ----------------------------------------------------------------------------
-- ENCORE_TEST_DATA
-- Purpose: Test data for ENCORE source system batch processing
-- Rows: 7 | Last Analyzed: 2025-08-15 22:00:05
-- ----------------------------------------------------------------------------
CREATE TABLE ENCORE_TEST_DATA (
    ID                  NUMBER(19,0)    NOT NULL,
    ACCT_NUM            VARCHAR2(18)    NOT NULL,
    BATCH_DATE          DATE            NOT NULL,
    CCI                 VARCHAR2(1)     NOT NULL,
    CONTACT_ID          VARCHAR2(18)    NOT NULL,
    CREATED_DATE        TIMESTAMP(6)    NOT NULL,
    TRANSACTION_AMT     NUMBER(10,2)    NULL,

    CONSTRAINT PK_ENCORE_TEST_DATA PRIMARY KEY (ID),
    CONSTRAINT UK_ENCORE_TEST UNIQUE (ACCT_NUM, BATCH_DATE, CONTACT_ID)
);

-- Indexes
CREATE INDEX IDX_ENCORE_ACCT ON ENCORE_TEST_DATA(ACCT_NUM);
CREATE INDEX IDX_ENCORE_BATCH_DATE ON ENCORE_TEST_DATA(BATCH_DATE);

COMMENT ON TABLE ENCORE_TEST_DATA IS 'Test data for ENCORE source system batch processing and validation';
COMMENT ON COLUMN ENCORE_TEST_DATA.ID IS 'Primary key - auto-generated record ID';
COMMENT ON COLUMN ENCORE_TEST_DATA.ACCT_NUM IS 'Account number (18 digits)';
COMMENT ON COLUMN ENCORE_TEST_DATA.BATCH_DATE IS 'Batch processing date';
COMMENT ON COLUMN ENCORE_TEST_DATA.CCI IS 'Credit card indicator (single character)';
COMMENT ON COLUMN ENCORE_TEST_DATA.CONTACT_ID IS 'Contact identifier (18 digits)';
COMMENT ON COLUMN ENCORE_TEST_DATA.CREATED_DATE IS 'Record creation timestamp';
COMMENT ON COLUMN ENCORE_TEST_DATA.TRANSACTION_AMT IS 'Transaction amount (decimal 10,2)';

-- ----------------------------------------------------------------------------
-- JOB_DEFINITIONS
-- Purpose: Job definition registry
-- Rows: 0 | Last Analyzed: 2025-08-01 22:14:12
-- ----------------------------------------------------------------------------
CREATE TABLE JOB_DEFINITIONS (
    ID                  VARCHAR2(100)   NOT NULL,
    SOURCE_SYSTEM_ID    VARCHAR2(50)    NOT NULL,
    JOB_NAME            VARCHAR2(50)    NOT NULL,
    DESCRIPTION         VARCHAR2(500)   NULL,
    INPUT_PATH          VARCHAR2(1000)  NULL,
    OUTPUT_PATH         VARCHAR2(1000)  NULL,
    QUERY_SQL           CLOB            NULL,
    ENABLED             VARCHAR2(1)     NULL,
    CREATED_DATE        TIMESTAMP(6)    NULL,
    TRANSACTION_TYPES   VARCHAR2(200)   NULL,

    CONSTRAINT PK_JOB_DEFINITIONS PRIMARY KEY (ID),
    CONSTRAINT FK_JOB_DEF_SOURCE_SYSTEM FOREIGN KEY (SOURCE_SYSTEM_ID) REFERENCES SOURCE_SYSTEMS(ID),
    CONSTRAINT UK_JOB_DEF_NAME UNIQUE (SOURCE_SYSTEM_ID, JOB_NAME)
);

-- Indexes
CREATE INDEX IDX_JOB_DEFINITIONS_SYSTEM ON JOB_DEFINITIONS(SOURCE_SYSTEM_ID);

COMMENT ON TABLE JOB_DEFINITIONS IS 'Job definition registry with source system associations';
COMMENT ON COLUMN JOB_DEFINITIONS.ID IS 'Primary key - unique job definition identifier';
COMMENT ON COLUMN JOB_DEFINITIONS.SOURCE_SYSTEM_ID IS 'Foreign key to SOURCE_SYSTEMS';
COMMENT ON COLUMN JOB_DEFINITIONS.JOB_NAME IS 'Unique job name within source system';
COMMENT ON COLUMN JOB_DEFINITIONS.DESCRIPTION IS 'Job description and purpose';
COMMENT ON COLUMN JOB_DEFINITIONS.INPUT_PATH IS 'Input file path or data source location';
COMMENT ON COLUMN JOB_DEFINITIONS.OUTPUT_PATH IS 'Output file path or destination location';
COMMENT ON COLUMN JOB_DEFINITIONS.QUERY_SQL IS 'SQL query for data extraction';
COMMENT ON COLUMN JOB_DEFINITIONS.ENABLED IS 'Y/N flag indicating if job is active';
COMMENT ON COLUMN JOB_DEFINITIONS.CREATED_DATE IS 'Timestamp when job was created';
COMMENT ON COLUMN JOB_DEFINITIONS.TRANSACTION_TYPES IS 'Comma-separated list of supported transaction types';

-- ============================================================================
-- SECTION 7: LIQUIBASE SCHEMA MANAGEMENT TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- DATABASECHANGELOG
-- Purpose: Liquibase change tracking
-- Rows: 25 | Last Analyzed: 2025-08-20 22:00:08
-- ----------------------------------------------------------------------------
CREATE TABLE DATABASECHANGELOG (
    ID              VARCHAR2(255)   NOT NULL,
    AUTHOR          VARCHAR2(255)   NOT NULL,
    FILENAME        VARCHAR2(255)   NOT NULL,
    DATEEXECUTED    TIMESTAMP(6)    NOT NULL,
    ORDEREXECUTED   NUMBER          NOT NULL,
    EXECTYPE        VARCHAR2(10)    NOT NULL,
    MD5SUM          VARCHAR2(35)    NULL,
    DESCRIPTION     VARCHAR2(255)   NULL,
    COMMENTS        VARCHAR2(255)   NULL,
    TAG             VARCHAR2(255)   NULL,
    LIQUIBASE       VARCHAR2(20)    NULL,
    CONTEXTS        VARCHAR2(255)   NULL,
    LABELS          VARCHAR2(255)   NULL,
    DEPLOYMENT_ID   VARCHAR2(10)    NULL
);

COMMENT ON TABLE DATABASECHANGELOG IS 'Liquibase change tracking for database schema versioning';
COMMENT ON COLUMN DATABASECHANGELOG.ID IS 'Changeset identifier';
COMMENT ON COLUMN DATABASECHANGELOG.AUTHOR IS 'Changeset author';
COMMENT ON COLUMN DATABASECHANGELOG.FILENAME IS 'Changelog file path';
COMMENT ON COLUMN DATABASECHANGELOG.DATEEXECUTED IS 'Execution timestamp';
COMMENT ON COLUMN DATABASECHANGELOG.ORDEREXECUTED IS 'Execution order sequence';
COMMENT ON COLUMN DATABASECHANGELOG.EXECTYPE IS 'Execution type: EXECUTED, FAILED, SKIPPED, RERAN, MARK_RAN';
COMMENT ON COLUMN DATABASECHANGELOG.MD5SUM IS 'MD5 checksum of changeset';
COMMENT ON COLUMN DATABASECHANGELOG.DESCRIPTION IS 'Changeset description';
COMMENT ON COLUMN DATABASECHANGELOG.COMMENTS IS 'Changeset comments';
COMMENT ON COLUMN DATABASECHANGELOG.TAG IS 'Version tag';
COMMENT ON COLUMN DATABASECHANGELOG.LIQUIBASE IS 'Liquibase version';
COMMENT ON COLUMN DATABASECHANGELOG.CONTEXTS IS 'Execution contexts';
COMMENT ON COLUMN DATABASECHANGELOG.LABELS IS 'Execution labels';
COMMENT ON COLUMN DATABASECHANGELOG.DEPLOYMENT_ID IS 'Deployment identifier';

-- ----------------------------------------------------------------------------
-- DATABASECHANGELOGLOCK
-- Purpose: Liquibase locking mechanism
-- Rows: 1 | Last Analyzed: 2025-09-15 22:09:11
-- ----------------------------------------------------------------------------
CREATE TABLE DATABASECHANGELOGLOCK (
    ID          NUMBER          NOT NULL,
    LOCKED      NUMBER(1,0)     NOT NULL,
    LOCKGRANTED TIMESTAMP(6)    NULL,
    LOCKEDBY    VARCHAR2(255)   NULL,

    CONSTRAINT PK_DATABASECHANGELOGLOCK PRIMARY KEY (ID)
);

COMMENT ON TABLE DATABASECHANGELOGLOCK IS 'Liquibase locking mechanism to prevent concurrent updates';
COMMENT ON COLUMN DATABASECHANGELOGLOCK.ID IS 'Primary key - lock identifier (always 1)';
COMMENT ON COLUMN DATABASECHANGELOGLOCK.LOCKED IS 'Lock status: 1=locked, 0=unlocked';
COMMENT ON COLUMN DATABASECHANGELOGLOCK.LOCKGRANTED IS 'Timestamp when lock was acquired';
COMMENT ON COLUMN DATABASECHANGELOGLOCK.LOCKEDBY IS 'Host/process that acquired the lock';

-- ============================================================================
-- SECTION 8: FOREIGN KEY CONSTRAINTS
-- ============================================================================

-- Manual Job Config Foreign Keys
ALTER TABLE MANUAL_JOB_CONFIG
ADD CONSTRAINT FK_JOB_CONFIG_MASTER_QUERY
FOREIGN KEY (MASTER_QUERY_ID) REFERENCES MASTER_QUERY_CONFIG(ID);

-- BATCH_JOB_AUDIT Foreign Keys (for older schema, if needed)
-- Note: BATCH_JOB_AUDIT table doesn't exist in current schema but kept for reference

-- ============================================================================
-- SECTION 9: SEQUENCES
-- ============================================================================

-- Sequence for BATCH_EXECUTION_RESULTS
CREATE SEQUENCE SEQ_BATCH_EXECUTION_RESULTS
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

-- Sequence for CONFIGURATION_AUDIT
CREATE SEQUENCE SEQ_CONFIGURATION_AUDIT
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

-- Sequence for Spring Batch sequences
CREATE SEQUENCE BATCH_JOB_SEQ
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

-- Sequence for MASTER_QUERY_CONFIG
CREATE SEQUENCE SEQ_MASTER_QUERY_CONFIG
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

-- Sequence for ENCORE_TEST_DATA
CREATE SEQUENCE SEQ_ENCORE_TEST_DATA
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

-- ============================================================================
-- END OF SCHEMA DDL
-- ============================================================================

-- Schema Statistics Summary:
-- Total Tables: 25 (22 active + 3 system tables)
-- Total Rows: 441
-- Largest Tables:
--   - FIELD_TEMPLATES: 366 rows
--   - TEMPLATE_SOURCE_MAPPINGS: 45 rows
--   - DATABASECHANGELOG: 25 rows
--   - BATCH_CONFIGURATIONS: 7 rows
--   - ENCORE_TEST_DATA: 7 rows

-- Generated from live database at: jdbc:oracle:thin:@localhost:1521/ORCLPDB1
-- Schema: CM3INT
-- Timestamp: 2025-10-05 17:41:17 EDT
