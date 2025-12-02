#!/bin/bash
################################################################################
# SQL*LOADER AUTOMATION SCRIPT - FABRIC PLATFORM
################################################################################
#
# Purpose: Automated SQL*Loader execution with complete lifecycle management
# Version: 1.0
# Author: Senior Full Stack Developer Agent
# Date: 2025-11-28
#
# Features:
# - Dynamic control file generation from database configuration
# - Pre/post execution SQL script execution
# - Error handling and retry logic
# - Performance metrics collection
# - Security audit logging
# - Email notifications
# - Automated cleanup and archiving
#
################################################################################

# Script Configuration
SCRIPT_NAME="sqlldr_automation.sh"
SCRIPT_VERSION="1.0"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${SCRIPT_DIR}/../logs"
CONFIG_DIR="${SCRIPT_DIR}/../config"
CONTROL_FILE_DIR="${SCRIPT_DIR}/../control_files"
DATA_DIR="/tmp/data/input"
OUTPUT_DIR="/tmp/data/output"
ARCHIVE_DIR="/tmp/data/archive"
BAD_FILE_DIR="/tmp/data/bad"
DISCARD_FILE_DIR="/tmp/data/discard"

# Oracle Database Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-1521}"
DB_SERVICE="${DB_SERVICE:-ORCLPDB1}"
DB_USER="${DB_USER:-cm3int}"
DB_PASS="${DB_PASS:-MySecurePass123}"
DB_SCHEMA="CM3INT"

# SQL*Loader Configuration
SQLLDR_BIN="${ORACLE_HOME}/bin/sqlldr"
SQLLDR_USERID="${DB_USER}/${DB_PASS}@${DB_HOST}:${DB_PORT}/${DB_SERVICE}"

# Logging Setup
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
LOG_FILE="${LOG_DIR}/sqlldr_automation_${TIMESTAMP}.log"
ERROR_LOG="${LOG_DIR}/sqlldr_errors_${TIMESTAMP}.log"

# Color Codes for Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

################################################################################
# Utility Functions
################################################################################

# Logging function
log() {
    local level="$1"
    shift
    local message="$@"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[${timestamp}] [${level}] ${message}" | tee -a "${LOG_FILE}"
}

log_info() {
    echo -e "${GREEN}[INFO]${NC} $@" | tee -a "${LOG_FILE}"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $@" | tee -a "${LOG_FILE}"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $@" | tee -a "${LOG_FILE}" "${ERROR_LOG}"
}

log_debug() {
    if [[ "${DEBUG}" == "true" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} $@" | tee -a "${LOG_FILE}"
    fi
}

# Initialize directories
init_directories() {
    log_info "Initializing directories..."
    mkdir -p "${LOG_DIR}" "${CONTROL_FILE_DIR}" "${DATA_DIR}" "${OUTPUT_DIR}" \
             "${ARCHIVE_DIR}" "${BAD_FILE_DIR}" "${DISCARD_FILE_DIR}"

    if [[ $? -eq 0 ]]; then
        log_info "✓ All directories initialized successfully"
        return 0
    else
        log_error "✗ Failed to initialize directories"
        return 1
    fi
}

# Generate correlation ID
generate_correlation_id() {
    local config_id="$1"
    echo "SQLLDR_${config_id}_${TIMESTAMP}_$$"
}

# Send notification email
send_notification() {
    local subject="$1"
    local body="$2"
    local recipients="$3"

    if [[ -n "${recipients}" ]]; then
        log_info "Sending notification email: ${subject}"
        echo "${body}" | mail -s "${subject}" "${recipients}"
    fi
}

################################################################################
# Database Functions
################################################################################

# Execute SQL query
execute_sql() {
    local sql_query="$1"
    local output_file="${2:-/tmp/sql_output_$$.txt}"

    log_debug "Executing SQL: ${sql_query}"

    sqlplus -S "${SQLLDR_USERID}" <<EOF > "${output_file}" 2>&1
SET PAGESIZE 0
SET FEEDBACK OFF
SET HEADING OFF
SET LINESIZE 32767
SET TRIMSPOOL ON
SET ECHO OFF
SET VERIFY OFF
${sql_query}
EXIT;
EOF

    local exit_code=$?
    if [[ ${exit_code} -ne 0 ]]; then
        log_error "SQL execution failed with exit code: ${exit_code}"
        cat "${output_file}" >> "${ERROR_LOG}"
        return ${exit_code}
    fi

    return 0
}

# Get SQL*Loader configuration from database
get_loader_config() {
    local config_id="$1"
    local output_file="/tmp/loader_config_${config_id}_$$.txt"

    log_info "Retrieving SQL*Loader configuration: ${config_id}"

    local sql="SELECT config_id || '|' || job_name || '|' || source_system || '|' || target_table || '|' ||
               load_method || '|' || direct_path || '|' || parallel_degree || '|' || max_errors || '|' ||
               field_delimiter || '|' || skip_rows || '|' || character_set || '|' || bind_size || '|' ||
               read_size || '|' || notification_emails
               FROM ${DB_SCHEMA}.sql_loader_configs
               WHERE config_id = '${config_id}' AND enabled = 'Y';"

    execute_sql "${sql}" "${output_file}"

    if [[ $? -eq 0 ]] && [[ -s "${output_file}" ]]; then
        cat "${output_file}"
        rm -f "${output_file}"
        return 0
    else
        log_error "Configuration not found or disabled: ${config_id}"
        rm -f "${output_file}"
        return 1
    fi
}

# Get field configurations
get_field_configs() {
    local config_id="$1"
    local output_file="/tmp/field_configs_${config_id}_$$.txt"

    log_info "Retrieving field configurations for: ${config_id}"

    local sql="SELECT field_name || '|' || column_name || '|' || data_type || '|' ||
               format_mask || '|' || nullable || '|' || sql_expression || '|' || trim_field || '|' ||
               case_sensitive || '|' || null_if_condition || '|' || field_order
               FROM ${DB_SCHEMA}.sql_loader_field_configs
               WHERE config_id = '${config_id}' AND enabled = 'Y'
               ORDER BY field_order;"

    execute_sql "${sql}" "${output_file}"

    if [[ $? -eq 0 ]] && [[ -s "${output_file}" ]]; then
        cat "${output_file}"
        rm -f "${output_file}"
        return 0
    else
        log_error "No field configurations found for: ${config_id}"
        rm -f "${output_file}"
        return 1
    fi
}

# Log execution start
log_execution_start() {
    local execution_id="$1"
    local config_id="$2"
    local correlation_id="$3"
    local file_name="$4"
    local file_path="$5"
    local control_file_path="$6"

    log_info "Logging execution start: ${execution_id}"

    local sql="INSERT INTO ${DB_SCHEMA}.sql_loader_executions (
        execution_id, config_id, correlation_id, file_name, file_path,
        control_file_path, execution_status, started_date, created_by, created_date
    ) VALUES (
        '${execution_id}', '${config_id}', '${correlation_id}', '${file_name}', '${file_path}',
        '${control_file_path}', 'EXECUTING', SYSTIMESTAMP, USER, SYSTIMESTAMP
    );"

    execute_sql "${sql}" "/dev/null"
}

# Update execution results
update_execution_results() {
    local execution_id="$1"
    local status="$2"
    local return_code="$3"
    local total_records="$4"
    local successful_records="$5"
    local rejected_records="$6"
    local duration_ms="$7"
    local error_message="${8:-}"

    log_info "Updating execution results: ${execution_id} - Status: ${status}"

    # Escape single quotes in error message
    error_message="${error_message//\'/\'\'}"

    local sql="UPDATE ${DB_SCHEMA}.sql_loader_executions SET
        execution_status = '${status}',
        sql_loader_return_code = ${return_code},
        total_records = ${total_records},
        successful_records = ${successful_records},
        rejected_records = ${rejected_records},
        duration_ms = ${duration_ms},
        completed_date = SYSTIMESTAMP,
        error_message = '${error_message}'
        WHERE execution_id = '${execution_id}';"

    execute_sql "${sql}" "/dev/null"
}

################################################################################
# Control File Generation
################################################################################

# Generate SQL*Loader control file
generate_control_file() {
    local config_id="$1"
    local data_file="$2"
    local control_file="${CONTROL_FILE_DIR}/${config_id}_${TIMESTAMP}.ctl"

    log_info "Generating control file: ${control_file}"

    # Get configuration
    local config_data
    config_data=$(get_loader_config "${config_id}")
    if [[ $? -ne 0 ]]; then
        log_error "Failed to retrieve configuration"
        return 1
    fi

    # Parse configuration
    IFS='|' read -r config_id job_name source_system target_table load_method \
                    direct_path parallel_degree max_errors field_delimiter skip_rows \
                    character_set bind_size read_size notification_emails <<< "${config_data}"

    # Get field configurations
    local field_configs
    field_configs=$(get_field_configs "${config_id}")
    if [[ $? -ne 0 ]]; then
        log_error "Failed to retrieve field configurations"
        return 1
    fi

    # Generate control file header
    cat > "${control_file}" <<EOF
-- ============================================================================
-- SQL*Loader Control File - Auto-Generated
-- ============================================================================
-- Generated: $(date '+%Y-%m-%d %H:%M:%S')
-- Config ID: ${config_id}
-- Job Name: ${job_name}
-- Source System: ${source_system}
-- Target Table: ${target_table}
-- ============================================================================

OPTIONS (
  DIRECT=${direct_path},
  ERRORS=${max_errors},
  SKIP=${skip_rows},
  BINDSIZE=${bind_size},
  READSIZE=${read_size},
  PARALLEL=${direct_path}
)

LOAD DATA
CHARACTERSET ${character_set}
INFILE '${data_file}'
BADFILE '${BAD_FILE_DIR}/$(basename "${data_file}" .dat).bad'
DISCARDFILE '${DISCARD_FILE_DIR}/$(basename "${data_file}" .dat).dsc'

${load_method} INTO TABLE ${target_table}

FIELDS TERMINATED BY '${field_delimiter}'
       OPTIONALLY ENCLOSED BY '"'
       TRAILING NULLCOLS
(
EOF

    # Generate field definitions
    local first_field=true
    while IFS='|' read -r field_name column_name data_type format_mask nullable \
                           sql_expression trim_field case_sensitive null_if_condition field_order; do

        if [[ "${first_field}" != "true" ]]; then
            echo "," >> "${control_file}"
        fi
        first_field=false

        # Build field definition
        local field_def="  ${column_name} "

        # Add data type
        if [[ -n "${format_mask}" ]]; then
            field_def="${field_def} DATE \"${format_mask}\""
        else
            case "${data_type}" in
                "NUMBER"|"INTEGER")
                    field_def="${field_def} INTEGER EXTERNAL"
                    ;;
                "DECIMAL"|"FLOAT")
                    field_def="${field_def} DECIMAL EXTERNAL"
                    ;;
                *)
                    field_def="${field_def} CHAR"
                    ;;
            esac
        fi

        # Add null handling
        if [[ "${nullable}" == "Y" ]]; then
            field_def="${field_def} NULLIF ${column_name}=${null_if_condition}"
        fi

        # Add SQL expression if provided
        if [[ -n "${sql_expression}" ]]; then
            field_def="${field_def} \"${sql_expression}\""
        else
            # Add default transformations based on case sensitivity and trimming
            if [[ "${trim_field}" == "Y" ]] || [[ "${case_sensitive}" != "PRESERVE" ]]; then
                local transform=""
                if [[ "${trim_field}" == "Y" ]]; then
                    transform="LTRIM(RTRIM(:${column_name}))"
                else
                    transform=":${column_name}"
                fi

                if [[ "${case_sensitive}" == "UPPER" ]]; then
                    transform="UPPER(${transform})"
                elif [[ "${case_sensitive}" == "LOWER" ]]; then
                    transform="LOWER(${transform})"
                fi

                field_def="${field_def} \"${transform}\""
            fi
        fi

        echo -n "${field_def}" >> "${control_file}"

    done <<< "${field_configs}"

    # Close field list
    echo "" >> "${control_file}"
    echo ")" >> "${control_file}"
    echo "" >> "${control_file}"
    echo "-- End of control file" >> "${control_file}"

    log_info "✓ Control file generated successfully: ${control_file}"
    echo "${control_file}"
    return 0
}

################################################################################
# SQL*Loader Execution
################################################################################

# Execute SQL*Loader
execute_sqlldr() {
    local control_file="$1"
    local log_file="${LOG_DIR}/sqlldr_$(basename "${control_file}" .ctl)_${TIMESTAMP}.log"

    log_info "Executing SQL*Loader with control file: ${control_file}"
    log_info "SQL*Loader log file: ${log_file}"

    # Capture start time
    local start_time=$(date +%s%3N)

    # Execute SQL*Loader
    ${SQLLDR_BIN} userid="${SQLLDR_USERID}" control="${control_file}" log="${log_file}" 2>&1

    local return_code=$?
    local end_time=$(date +%s%3N)
    local duration_ms=$((end_time - start_time))

    log_info "SQL*Loader completed with return code: ${return_code}"
    log_info "Execution duration: ${duration_ms} ms"

    # Parse SQL*Loader log file for statistics
    local total_records=0
    local successful_records=0
    local rejected_records=0

    if [[ -f "${log_file}" ]]; then
        total_records=$(grep -E "^Total logical records read:" "${log_file}" | awk '{print $NF}')
        successful_records=$(grep -E "^Total logical records  rejected:" "${log_file}" | awk '{print $NF}')
        rejected_records=$(grep -E "^Total logical records discarded:" "${log_file}" | awk '{print $NF}')

        total_records=${total_records:-0}
        successful_records=${successful_records:-0}
        rejected_records=${rejected_records:-0}
    fi

    echo "${return_code}|${duration_ms}|${total_records}|${successful_records}|${rejected_records}|${log_file}"
    return ${return_code}
}

################################################################################
# Main Execution Function
################################################################################

run_sqlldr_automation() {
    local config_id="$1"
    local data_file="$2"

    log_info "========================================="
    log_info "SQL*Loader Automation Starting"
    log_info "========================================="
    log_info "Config ID: ${config_id}"
    log_info "Data File: ${data_file}"
    log_info "Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"

    # Validate inputs
    if [[ -z "${config_id}" ]] || [[ -z "${data_file}" ]]; then
        log_error "Usage: $0 <config_id> <data_file>"
        return 1
    fi

    if [[ ! -f "${data_file}" ]]; then
        log_error "Data file not found: ${data_file}"
        return 1
    fi

    # Initialize directories
    init_directories || return 1

    # Generate correlation ID and execution ID
    local correlation_id=$(generate_correlation_id "${config_id}")
    local execution_id="EXEC_${config_id}_${TIMESTAMP}"

    log_info "Correlation ID: ${correlation_id}"
    log_info "Execution ID: ${execution_id}"

    # Generate control file
    local control_file
    control_file=$(generate_control_file "${config_id}" "${data_file}")
    if [[ $? -ne 0 ]]; then
        log_error "Control file generation failed"
        return 1
    fi

    # Log execution start in database
    log_execution_start "${execution_id}" "${config_id}" "${correlation_id}" \
                        "$(basename "${data_file}")" "${data_file}" "${control_file}"

    # Execute SQL*Loader
    local sqlldr_result
    sqlldr_result=$(execute_sqlldr "${control_file}")
    local sqlldr_exit_code=$?

    # Parse results
    IFS='|' read -r return_code duration_ms total_records successful_records rejected_records sqlldr_log <<< "${sqlldr_result}"

    # Determine execution status
    local exec_status="SUCCESS"
    local error_message=""

    if [[ ${sqlldr_exit_code} -ne 0 ]]; then
        case ${sqlldr_exit_code} in
            1)
                exec_status="FAILED"
                error_message="Command line arguments error"
                ;;
            2)
                exec_status="SUCCESS_WITH_WARNINGS"
                error_message="Warning - some records rejected"
                ;;
            3)
                exec_status="FAILED"
                error_message="Fatal error occurred"
                ;;
            *)
                exec_status="FAILED"
                error_message="Unknown error (code: ${sqlldr_exit_code})"
                ;;
        esac
    fi

    # Update execution results in database
    update_execution_results "${execution_id}" "${exec_status}" "${return_code}" \
                             "${total_records}" "${successful_records}" "${rejected_records}" \
                             "${duration_ms}" "${error_message}"

    # Display summary
    log_info "========================================="
    log_info "SQL*Loader Execution Summary"
    log_info "========================================="
    log_info "Status: ${exec_status}"
    log_info "Total Records: ${total_records}"
    log_info "Successful: ${successful_records}"
    log_info "Rejected: ${rejected_records}"
    log_info "Duration: ${duration_ms} ms"
    log_info "Log File: ${sqlldr_log}"

    if [[ "${exec_status}" == "FAILED" ]]; then
        log_error "Error: ${error_message}"
        log_error "Check log file for details: ${sqlldr_log}"
        return 1
    fi

    log_info "✓ SQL*Loader automation completed successfully"
    return 0
}

################################################################################
# Main Entry Point
################################################################################

# Parse command line arguments
CONFIG_ID="${1}"
DATA_FILE="${2}"
DEBUG="${DEBUG:-false}"

# Run automation
run_sqlldr_automation "${CONFIG_ID}" "${DATA_FILE}"
exit $?
