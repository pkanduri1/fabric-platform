# Requirements Document

## Introduction

This feature implements a Spring Boot API service that provides configurable file system monitoring with pluggable file processors. The initial implementation focuses on SQL script execution, with extensibility for additional processors like SQL*Loader log processing and audit logging. The service monitors multiple configured folders, processes files through appropriate processors, and moves completed files to archive folders. This enables automated file-based processing workflows for various operational tasks.

## Requirements

### Requirement 1

**User Story:** As a system administrator, I want the system to automatically detect new files in multiple configured watched folders, so that I can process different types of files through appropriate processors.

#### Acceptance Criteria

1. WHEN a new file matching configured patterns is placed in any watched folder THEN the system SHALL detect the file within the configured polling interval
2. WHEN multiple files are detected simultaneously in the same folder THEN the system SHALL process them in alphabetical order
3. WHEN a watched folder does not exist THEN the system SHALL create it automatically on startup
4. IF a file has a .tmp or .processing extension THEN the system SHALL ignore it during scanning
5. WHEN multiple watch configurations are defined THEN the system SHALL monitor all enabled configurations simultaneously

### Requirement 2

**User Story:** As a database administrator, I want SQL files to be executed automatically against the database, so that schema changes and data updates are applied without manual intervention.

#### Acceptance Criteria

1. WHEN a valid SQL file is detected THEN the system SHALL execute its contents against the configured database
2. WHEN executing DDL statements (CREATE, ALTER, DROP) THEN the system SHALL handle them with appropriate transaction management
3. WHEN executing DML statements (INSERT, UPDATE, DELETE) THEN the system SHALL wrap them in transactions with rollback capability
4. IF a SQL file contains multiple statements THEN the system SHALL execute them sequentially within the same transaction
5. WHEN a SQL execution fails THEN the system SHALL log the error and move the file to an error folder

### Requirement 3

**User Story:** As a database administrator, I want successfully executed SQL files to be moved to a completed folder, so that I can track which scripts have been processed and avoid reprocessing.

#### Acceptance Criteria

1. WHEN a SQL file executes successfully THEN the system SHALL move it to a 'completed' subfolder
2. WHEN moving completed files THEN the system SHALL preserve the original filename with a timestamp suffix
3. WHEN a file move operation fails THEN the system SHALL log the error but not re-execute the SQL
4. IF the completed folder does not exist THEN the system SHALL create it automatically

### Requirement 4

**User Story:** As a database administrator, I want failed SQL executions to be handled gracefully, so that I can identify and fix problematic scripts without affecting other operations.

#### Acceptance Criteria

1. WHEN a SQL execution fails THEN the system SHALL move the file to an 'error' subfolder
2. WHEN moving error files THEN the system SHALL append error details to the filename
3. WHEN a database connection fails THEN the system SHALL retry up to 3 times with exponential backoff
4. WHEN critical errors occur THEN the system SHALL continue monitoring without stopping the service

### Requirement 5

**User Story:** As a system administrator, I want comprehensive logging and monitoring of the file watching service, so that I can troubleshoot issues and monitor system health.

#### Acceptance Criteria

1. WHEN files are detected THEN the system SHALL log the detection with timestamp and filename
2. WHEN SQL execution begins THEN the system SHALL log the start time and file being processed
3. WHEN SQL execution completes THEN the system SHALL log success/failure status and execution time
4. WHEN errors occur THEN the system SHALL log detailed error messages with stack traces
5. WHEN the service starts THEN the system SHALL log configuration details including watched folder path

### Requirement 6

**User Story:** As a developer, I want the file watching service to be configurable through application properties, so that it can be adapted to different environments and support multiple processing types without code changes.

#### Acceptance Criteria

1. WHEN the application starts THEN the system SHALL read multiple watch configurations from application properties
2. WHEN the application starts THEN the system SHALL read database connection details from configuration
3. WHEN the application starts THEN the system SHALL read polling intervals per watch configuration with sensible defaults
4. IF configuration values are missing THEN the system SHALL use sensible defaults and log warnings
5. WHEN configuration changes THEN the system SHALL support hot reloading without restart
6. WHEN new processor types are added THEN the system SHALL support them through configuration without code changes to the core watcher

### Requirement 7

**User Story:** As a system administrator, I want the file watcher service to support different types of file processors, so that I can handle various file processing workflows through a single service.

#### Acceptance Criteria

1. WHEN the system starts THEN it SHALL register all available file processors (SQL script, SQL*Loader log, etc.)
2. WHEN a file is detected THEN the system SHALL route it to the appropriate processor based on configuration
3. WHEN a new processor type is implemented THEN it SHALL integrate with the existing file watcher without modifying core logic
4. WHEN a processor is not available for a file type THEN the system SHALL log an error and move the file to the error folder
5. WHEN multiple processors support the same file type THEN the system SHALL use the processor specified in the watch configuration

### Requirement 8

**User Story:** As a database administrator, I want SQL*Loader log files to be processed and their information stored in an audit table, so that I can track data loading operations and their outcomes.

#### Acceptance Criteria

1. WHEN a SQL*Loader log file is detected THEN the system SHALL parse it and extract load statistics
2. WHEN log parsing is successful THEN the system SHALL write audit information to the configured audit table
3. WHEN log parsing fails THEN the system SHALL log the error and move the file to the error folder
4. WHEN audit information is written THEN it SHALL include load start/end times, record counts, and status
5. WHEN multiple log files are processed THEN each SHALL create a separate audit record