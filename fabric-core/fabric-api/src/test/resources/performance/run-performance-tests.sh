#!/bin/bash

# Fabric Platform - Manual Job Configuration API Performance Test Runner
# Enterprise-grade performance testing script for banking applications
# 
# Requirements:
# - Apache JMeter 5.4+ installed and in PATH
# - Java 11+ runtime environment
# - Spring Boot application running on specified endpoint
# - Valid JWT token for authentication
#
# Usage:
#   ./run-performance-tests.sh [environment] [threads] [duration]
#
# Examples:
#   ./run-performance-tests.sh local 100 300     # Local test with 100 threads for 5 minutes
#   ./run-performance-tests.sh dev 1000 600      # Dev test with 1000 threads for 10 minutes
#   ./run-performance-tests.sh staging 2000 900  # Staging test with 2000 threads for 15 minutes

set -e  # Exit on any error

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$(dirname "$(dirname "$SCRIPT_DIR")")")")"
RESULTS_DIR="${SCRIPT_DIR}/results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Default values
ENVIRONMENT="${1:-local}"
THREAD_COUNT="${2:-1000}"
TEST_DURATION="${3:-600}"  # 10 minutes default
RAMP_UP_PERIOD=$((THREAD_COUNT / 10))  # 10 threads per second ramp-up

# Environment-specific configurations
case "${ENVIRONMENT}" in
    "local")
        BASE_URL="http://localhost:8080"
        JWT_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwZXJmb3JtYW5jZS50ZXN0QGJhbmsuY29tIiwicm9sZXMiOlsiSk9CX0NSRUFUT1IiXSwiaWF0IjoxNjkxMjM0NTY3LCJleHAiOjk5OTk5OTk5OTl9.test"
        ;;
    "dev")
        BASE_URL="https://fabric-api-dev.truist.com"
        JWT_TOKEN="${DEV_JWT_TOKEN:-$JWT_TOKEN}"
        ;;
    "staging")
        BASE_URL="https://fabric-api-staging.truist.com"
        JWT_TOKEN="${STAGING_JWT_TOKEN:-$JWT_TOKEN}"
        ;;
    "prod")
        echo "WARNING: Performance testing against production is not recommended!"
        echo "Please use staging environment for performance validation."
        exit 1
        ;;
    *)
        echo "ERROR: Unknown environment '${ENVIRONMENT}'"
        echo "Supported environments: local, dev, staging"
        exit 1
        ;;
esac

# Validate JMeter installation
if ! command -v jmeter &> /dev/null; then
    echo "ERROR: JMeter is not installed or not in PATH"
    echo "Please install Apache JMeter 5.4+ and ensure it's in your PATH"
    exit 1
fi

# Validate JWT token
if [[ -z "${JWT_TOKEN}" ]]; then
    echo "ERROR: JWT token is required for authentication"
    echo "Please set the appropriate JWT token for environment: ${ENVIRONMENT}"
    exit 1
fi

# Create results directory
mkdir -p "${RESULTS_DIR}"

# Test parameters
JMX_FILE="${SCRIPT_DIR}/Manual-Job-Config-Performance-Test.jmx"
TEST_DATA_FILE="${SCRIPT_DIR}/performance-test-data.csv"
RESULTS_FILE="${RESULTS_DIR}/performance-test-results_${ENVIRONMENT}_${TIMESTAMP}.csv"
AGGREGATE_FILE="${RESULTS_DIR}/aggregate-report_${ENVIRONMENT}_${TIMESTAMP}.csv"
RESPONSE_TIMES_FILE="${RESULTS_DIR}/response-times_${ENVIRONMENT}_${TIMESTAMP}.csv"
LOG_FILE="${RESULTS_DIR}/jmeter-log_${ENVIRONMENT}_${TIMESTAMP}.log"

# Validate test files exist
if [[ ! -f "${JMX_FILE}" ]]; then
    echo "ERROR: JMeter test plan not found: ${JMX_FILE}"
    exit 1
fi

if [[ ! -f "${TEST_DATA_FILE}" ]]; then
    echo "ERROR: Test data file not found: ${TEST_DATA_FILE}"
    exit 1
fi

echo "=================================================================="
echo "Fabric Platform - Performance Test Execution"
echo "=================================================================="
echo "Environment: ${ENVIRONMENT}"
echo "Base URL: ${BASE_URL}"
echo "Thread Count: ${THREAD_COUNT}"
echo "Test Duration: ${TEST_DURATION} seconds"
echo "Ramp-up Period: ${RAMP_UP_PERIOD} seconds"
echo "Results Directory: ${RESULTS_DIR}"
echo "Timestamp: ${TIMESTAMP}"
echo "=================================================================="

# Pre-test validation - check if the application is running
echo "Validating application availability..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health" || echo "000")

if [[ "${HTTP_STATUS}" != "200" ]]; then
    echo "WARNING: Application health check failed (HTTP ${HTTP_STATUS})"
    echo "Please ensure the Fabric Platform application is running at: ${BASE_URL}"
    read -p "Continue with performance test anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Performance test cancelled."
        exit 1
    fi
fi

# System resource check
echo "Checking system resources..."
AVAILABLE_MEMORY=$(free -m | awk 'NR==2{printf "%.1f", $7/1024}')
CPU_CORES=$(nproc)

echo "Available Memory: ${AVAILABLE_MEMORY} GB"
echo "CPU Cores: ${CPU_CORES}"

if (( $(echo "${AVAILABLE_MEMORY} < 4.0" | bc -l) )); then
    echo "WARNING: Low available memory (${AVAILABLE_MEMORY} GB). Recommended: 8GB+"
fi

if (( THREAD_COUNT > CPU_CORES * 200 )); then
    echo "WARNING: Thread count (${THREAD_COUNT}) is very high for ${CPU_CORES} CPU cores"
fi

# JVM tuning for JMeter
JMETER_HEAP_SIZE="${JMETER_HEAP_SIZE:-4g}"
JMETER_NEW_SIZE="${JMETER_NEW_SIZE:-2g}"

export JVM_ARGS="-Xms${JMETER_HEAP_SIZE} -Xmx${JMETER_HEAP_SIZE} -XX:NewSize=${JMETER_NEW_SIZE} -XX:MaxNewSize=${JMETER_NEW_SIZE} -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:G1ReservePercent=20"

echo "JMeter JVM Settings: ${JVM_ARGS}"

# Create performance test report header
cat > "${RESULTS_DIR}/test-summary_${ENVIRONMENT}_${TIMESTAMP}.txt" << EOF
Fabric Platform - Manual Job Configuration API Performance Test Results
========================================================================

Test Configuration:
- Environment: ${ENVIRONMENT}
- Base URL: ${BASE_URL}
- Timestamp: $(date)
- Thread Count: ${THREAD_COUNT}
- Test Duration: ${TEST_DURATION} seconds
- Ramp-up Period: ${RAMP_UP_PERIOD} seconds
- JMeter Version: $(jmeter -v 2>&1 | head -n 1)
- Java Version: $(java -version 2>&1 | head -n 1)

System Information:
- OS: $(uname -a)
- CPU Cores: ${CPU_CORES}
- Available Memory: ${AVAILABLE_MEMORY} GB
- JMeter Heap Size: ${JMETER_HEAP_SIZE}

Performance Requirements:
- API Response Time: < 200ms average
- Concurrent Users: Support 1000+ users
- Success Rate: > 95%
- Error Rate: < 5%
- Throughput: > 100 requests/second

========================================================================

EOF

# Execute JMeter performance test
echo "Starting JMeter performance test..."
echo "This may take a while depending on the test duration and thread count..."

jmeter \
    -n \
    -t "${JMX_FILE}" \
    -l "${RESULTS_FILE}" \
    -j "${LOG_FILE}" \
    -Jbase.url="${BASE_URL}" \
    -Jjwt.token="${JWT_TOKEN}" \
    -Jthreads="${THREAD_COUNT}" \
    -Jrampup="${RAMP_UP_PERIOD}" \
    -Jloops="1" \
    -Jtestdata.file="${TEST_DATA_FILE}" \
    -Jresults.file="${RESULTS_FILE}" \
    -Jaggregate.file="${AGGREGATE_FILE}" \
    -Jresponse.times.file="${RESPONSE_TIMES_FILE}" \
    -Jsummariser.interval=30 \
    -Jsummariser.out=true

TEST_EXIT_CODE=$?

echo "JMeter test execution completed with exit code: ${TEST_EXIT_CODE}"

# Process and analyze results
if [[ -f "${RESULTS_FILE}" ]]; then
    echo "Analyzing performance test results..."
    
    # Calculate basic statistics
    TOTAL_REQUESTS=$(tail -n +2 "${RESULTS_FILE}" | wc -l)
    SUCCESSFUL_REQUESTS=$(tail -n +2 "${RESULTS_FILE}" | awk -F',' '$8=="true" {count++} END {print count+0}')
    FAILED_REQUESTS=$((TOTAL_REQUESTS - SUCCESSFUL_REQUESTS))
    
    if [[ ${TOTAL_REQUESTS} -gt 0 ]]; then
        SUCCESS_RATE=$(echo "scale=2; ${SUCCESSFUL_REQUESTS} * 100 / ${TOTAL_REQUESTS}" | bc)
        ERROR_RATE=$(echo "scale=2; ${FAILED_REQUESTS} * 100 / ${TOTAL_REQUESTS}" | bc)
        
        # Calculate response time statistics
        AVG_RESPONSE_TIME=$(tail -n +2 "${RESULTS_FILE}" | awk -F',' '{sum+=$2; count++} END {print sum/count}')
        
        # Get min/max response times
        MIN_RESPONSE_TIME=$(tail -n +2 "${RESULTS_FILE}" | awk -F',' 'NR==1 {min=$2} {if($2<min) min=$2} END {print min}')
        MAX_RESPONSE_TIME=$(tail -n +2 "${RESULTS_FILE}" | awk -F',' '{if($2>max) max=$2} END {print max}')
        
        # Calculate throughput
        TEST_START=$(tail -n +2 "${RESULTS_FILE}" | head -n 1 | awk -F',' '{print $1}')
        TEST_END=$(tail -n +2 "${RESULTS_FILE}" | tail -n 1 | awk -F',' '{print $1}')
        TEST_DURATION_ACTUAL=$(echo "scale=2; (${TEST_END} - ${TEST_START}) / 1000" | bc)
        THROUGHPUT=$(echo "scale=2; ${TOTAL_REQUESTS} / ${TEST_DURATION_ACTUAL}" | bc)
        
        # Append results to summary
        cat >> "${RESULTS_DIR}/test-summary_${ENVIRONMENT}_${TIMESTAMP}.txt" << EOF
Test Results Summary:
--------------------
Total Requests: ${TOTAL_REQUESTS}
Successful Requests: ${SUCCESSFUL_REQUESTS}
Failed Requests: ${FAILED_REQUESTS}
Success Rate: ${SUCCESS_RATE}%
Error Rate: ${ERROR_RATE}%

Response Time Statistics:
------------------------
Average Response Time: ${AVG_RESPONSE_TIME} ms
Minimum Response Time: ${MIN_RESPONSE_TIME} ms
Maximum Response Time: ${MAX_RESPONSE_TIME} ms

Performance Metrics:
-------------------
Test Duration (Actual): ${TEST_DURATION_ACTUAL} seconds
Throughput: ${THROUGHPUT} requests/second

Performance Requirements Validation:
-----------------------------------
EOF
        
        # Validate against requirements
        if (( $(echo "${AVG_RESPONSE_TIME} < 200" | bc -l) )); then
            echo "✓ Average Response Time: PASS (${AVG_RESPONSE_TIME}ms < 200ms)" >> "${RESULTS_DIR}/test-summary_${ENVIRONMENT}_${TIMESTAMP}.txt"
        else
            echo "✗ Average Response Time: FAIL (${AVG_RESPONSE_TIME}ms >= 200ms)" >> "${RESULTS_DIR}/test-summary_${ENVIRONMENT}_${TIMESTAMP}.txt"
        fi
        
        if (( $(echo "${SUCCESS_RATE} > 95" | bc -l) )); then
            echo "✓ Success Rate: PASS (${SUCCESS_RATE}% > 95%)" >> "${RESULTS_DIR}/test-summary_${ENVIRONMENT}_${TIMESTAMP}.txt"
        else
            echo "✗ Success Rate: FAIL (${SUCCESS_RATE}% <= 95%)" >> "${RESULTS_DIR}/test-summary_${ENVIRONMENT}_${TIMESTAMP}.txt"
        fi
        
        if (( $(echo "${ERROR_RATE} < 5" | bc -l) )); then
            echo "✓ Error Rate: PASS (${ERROR_RATE}% < 5%)" >> "${RESULTS_DIR}/test-summary_${ENVIRONMENT}_${TIMESTAMP}.txt"
        else
            echo "✗ Error Rate: FAIL (${ERROR_RATE}% >= 5%)" >> "${RESULTS_DIR}/test-summary_${ENVIRONMENT}_${TIMESTAMP}.txt"
        fi
        
        if (( $(echo "${THROUGHPUT} > 100" | bc -l) )); then
            echo "✓ Throughput: PASS (${THROUGHPUT} req/sec > 100 req/sec)" >> "${RESULTS_DIR}/test-summary_${ENVIRONMENT}_${TIMESTAMP}.txt"
        else
            echo "✗ Throughput: FAIL (${THROUGHPUT} req/sec <= 100 req/sec)" >> "${RESULTS_DIR}/test-summary_${ENVIRONMENT}_${TIMESTAMP}.txt"
        fi
        
        # Display summary
        echo "=================================================================="
        echo "PERFORMANCE TEST RESULTS SUMMARY"
        echo "=================================================================="
        echo "Total Requests: ${TOTAL_REQUESTS}"
        echo "Success Rate: ${SUCCESS_RATE}%"
        echo "Error Rate: ${ERROR_RATE}%"
        echo "Average Response Time: ${AVG_RESPONSE_TIME}ms"
        echo "Throughput: ${THROUGHPUT} req/sec"
        echo "=================================================================="
        
    else
        echo "ERROR: No test results found in results file"
    fi
    
else
    echo "ERROR: Results file not found: ${RESULTS_FILE}"
fi

# Generate HTML report if JMeter supports it
if command -v jmeter &> /dev/null; then
    HTML_REPORT_DIR="${RESULTS_DIR}/html-report_${ENVIRONMENT}_${TIMESTAMP}"
    echo "Generating HTML performance report..."
    
    jmeter -g "${RESULTS_FILE}" -o "${HTML_REPORT_DIR}" 2>/dev/null || {
        echo "HTML report generation failed or not supported in this JMeter version"
    }
    
    if [[ -d "${HTML_REPORT_DIR}" ]]; then
        echo "HTML Performance Report: ${HTML_REPORT_DIR}/index.html"
    fi
fi

# Cleanup (remove temporary files if needed)
# Uncomment the following line if you want to clean up large result files after processing
# find "${RESULTS_DIR}" -name "*.jtl" -size +100M -delete

echo "=================================================================="
echo "Performance test completed!"
echo "Results available in: ${RESULTS_DIR}"
echo "Summary report: ${RESULTS_DIR}/test-summary_${ENVIRONMENT}_${TIMESTAMP}.txt"
echo "Raw results: ${RESULTS_FILE}"
echo "JMeter log: ${LOG_FILE}"
echo "=================================================================="

exit ${TEST_EXIT_CODE}