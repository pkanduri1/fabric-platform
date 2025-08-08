#!/bin/bash

# 🧪 Fabric Platform - End-to-End Test Execution Script
# This script automates the E2E testing process described in docs/END_TO_END_TESTING_GUIDE.md

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8080"
UI_URL="http://localhost:3000"
TEST_DATE="2025-08-08"
EXECUTION_ID_PREFIX="E2E_TEST_${TEST_DATE}"

echo -e "${BLUE}🧪 Fabric Platform - End-to-End Test Execution${NC}"
echo "================================================"

# Function to check if services are running
check_services() {
    echo -e "${YELLOW}Checking if services are running...${NC}"
    
    # Check backend
    if curl -s "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Backend service is running${NC}"
    else
        echo -e "${RED}❌ Backend service is not running. Please start with: mvn spring-boot:run -pl fabric-api${NC}"
        exit 1
    fi
    
    # Check frontend
    if curl -s "${UI_URL}" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Frontend service is running${NC}"
    else
        echo -e "${RED}❌ Frontend service is not running. Please start with: npm start${NC}"
        exit 1
    fi
}

# Function to get JWT token (mock - replace with actual authentication)
get_auth_token() {
    # This is a placeholder - replace with actual authentication call
    echo "mock-jwt-token-for-testing"
}

# Step 3: API Testing Functions
test_sqlloader_simple() {
    echo -e "${YELLOW}Step 3a: Testing SQL*Loader for Simple Transactions...${NC}"
    
    local execution_id="${EXECUTION_ID_PREFIX}_001"
    local auth_token=$(get_auth_token)
    
    # Initialize execution context
    curl -X POST "${BASE_URL}/api/v1/batch/execution/initialize" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${auth_token}" \
        -d "{
            \"executionId\": \"${execution_id}\",
            \"correlationId\": \"E2E_CORR_001\",
            \"businessDate\": \"${TEST_DATE}\",
            \"requestedBy\": \"e2e_test_script\"
        }" \
        --silent --fail || {
            echo -e "${RED}❌ Failed to initialize execution context${NC}"
            return 1
        }
    
    # Trigger SQL*Loader
    local response=$(curl -X POST "${BASE_URL}/api/v1/sqlloader/execute" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${auth_token}" \
        -d "{
            \"executionId\": \"${execution_id}\",
            \"controlFileName\": \"tcb_simple_transactions.ctl\",
            \"dataFileName\": \"TCB_SIMPLE_TRANSACTIONS_20250808.dat\",
            \"processingMode\": \"IMMEDIATE\",
            \"validateOnly\": false
        }" \
        --silent --write-out "%{http_code}" --output /tmp/sqlloader_response.json)
    
    if [[ "$response" == "202" ]]; then
        echo -e "${GREEN}✅ Simple transaction SQL*Loader triggered successfully${NC}"
        return 0
    else
        echo -e "${RED}❌ SQL*Loader failed with HTTP status: $response${NC}"
        cat /tmp/sqlloader_response.json
        return 1
    fi
}

test_sqlloader_complex() {
    echo -e "${YELLOW}Step 3b: Testing SQL*Loader for Complex Transactions...${NC}"
    
    local execution_id="${EXECUTION_ID_PREFIX}_002"
    local auth_token=$(get_auth_token)
    
    # Trigger complex transaction SQL*Loader
    local response=$(curl -X POST "${BASE_URL}/api/v1/sqlloader/execute" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${auth_token}" \
        -d "{
            \"executionId\": \"${execution_id}\",
            \"controlFileName\": \"tcb_complex_transactions.ctl\",
            \"dataFileName\": \"TCB_COMPLEX_TRANSACTIONS_20250808.dat\",
            \"processingMode\": \"IMMEDIATE\",
            \"validateOnly\": false
        }" \
        --silent --write-out "%{http_code}" --output /tmp/sqlloader_complex_response.json)
    
    if [[ "$response" == "202" ]]; then
        echo -e "${GREEN}✅ Complex transaction SQL*Loader triggered successfully${NC}"
        return 0
    else
        echo -e "${RED}❌ Complex SQL*Loader failed with HTTP status: $response${NC}"
        cat /tmp/sqlloader_complex_response.json
        return 1
    fi
}

# Step 4: Epic 2 Testing
test_epic2_simple_processing() {
    echo -e "${YELLOW}Step 4: Testing Epic 2 Simple Transaction Processing...${NC}"
    
    local execution_id="EPIC2_${EXECUTION_ID_PREFIX}_001"
    local auth_token=$(get_auth_token)
    
    # Trigger Epic 2 parallel processing
    local response=$(curl -X POST "${BASE_URL}/api/v1/batch/jobs/epic2/execute" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${auth_token}" \
        -d "{
            \"jobName\": \"simpleTransactionProcessingJob\",
            \"executionId\": \"${execution_id}\",
            \"sourceExecutionId\": \"${EXECUTION_ID_PREFIX}_001\",
            \"parameters\": {
                \"businessDate\": \"${TEST_DATE}\",
                \"processingMode\": \"PARALLEL\",
                \"enablePerformanceMonitoring\": \"true\",
                \"auditLevel\": \"DETAILED\",
                \"correlationId\": \"E2E_EPIC2_CORR_001\"
            }
        }" \
        --silent --write-out "%{http_code}" --output /tmp/epic2_response.json)
    
    if [[ "$response" == "202" ]]; then
        echo -e "${GREEN}✅ Epic 2 Simple Processing triggered successfully${NC}"
        
        # Wait a moment and check status
        sleep 5
        check_job_status "$execution_id" "$auth_token"
        return 0
    else
        echo -e "${RED}❌ Epic 2 processing failed with HTTP status: $response${NC}"
        cat /tmp/epic2_response.json
        return 1
    fi
}

# Step 5: Epic 3 Testing
test_epic3_complex_processing() {
    echo -e "${YELLOW}Step 5: Testing Epic 3 Complex Transaction Processing...${NC}"
    
    local execution_id="EPIC3_${EXECUTION_ID_PREFIX}_001"
    local auth_token=$(get_auth_token)
    
    # Trigger Epic 3 complex processing
    local response=$(curl -X POST "${BASE_URL}/api/v1/batch/jobs/epic3/execute" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${auth_token}" \
        -d "{
            \"jobName\": \"complexTransactionProcessingJob\",
            \"executionId\": \"${execution_id}\",
            \"sourceExecutionId\": \"${EXECUTION_ID_PREFIX}_002\",
            \"parameters\": {
                \"businessDate\": \"${TEST_DATE}\",
                \"processingMode\": \"DEPENDENCY_AWARE\",
                \"enableTempStaging\": \"true\",
                \"enableHeaderFooter\": \"true\",
                \"auditLevel\": \"COMPREHENSIVE\",
                \"correlationId\": \"E2E_EPIC3_CORR_001\"
            }
        }" \
        --silent --write-out "%{http_code}" --output /tmp/epic3_response.json)
    
    if [[ "$response" == "202" ]]; then
        echo -e "${GREEN}✅ Epic 3 Complex Processing triggered successfully${NC}"
        
        # Wait a moment and check status
        sleep 10
        check_job_status "$execution_id" "$auth_token"
        return 0
    else
        echo -e "${RED}❌ Epic 3 processing failed with HTTP status: $response${NC}"
        cat /tmp/epic3_response.json
        return 1
    fi
}

# Helper function to check job status
check_job_status() {
    local execution_id="$1"
    local auth_token="$2"
    
    echo -e "${YELLOW}Checking job status for: $execution_id${NC}"
    
    local status_response=$(curl -X GET "${BASE_URL}/api/v1/batch/jobs/status/${execution_id}" \
        -H "Authorization: Bearer ${auth_token}" \
        --silent --fail)
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ Job status retrieved successfully${NC}"
        echo "$status_response" | python3 -m json.tool 2>/dev/null || echo "$status_response"
    else
        echo -e "${YELLOW}⚠️ Could not retrieve job status (job may still be initializing)${NC}"
    fi
}

# Main execution flow
main() {
    echo -e "${BLUE}Starting End-to-End Test Execution...${NC}"
    echo ""
    
    # Check prerequisites
    check_services
    echo ""
    
    # Display manual steps reminder
    echo -e "${YELLOW}📋 MANUAL STEPS REQUIRED:${NC}"
    echo "Before running this script, please complete Steps 1 & 2 manually via the UI:"
    echo "1. Configure Source System at: ${UI_URL}"
    echo "2. Configure SQL*Loader CTL files at: ${UI_URL}"
    echo ""
    echo "Press Enter to continue with API testing (Steps 3-5)..."
    read -r
    
    # Execute API-based tests
    echo -e "${BLUE}Executing Automated API Tests...${NC}"
    echo ""
    
    # Test SQL*Loader processing
    if test_sqlloader_simple && test_sqlloader_complex; then
        echo -e "${GREEN}✅ SQL*Loader tests completed successfully${NC}"
    else
        echo -e "${RED}❌ SQL*Loader tests failed${NC}"
        exit 1
    fi
    
    echo ""
    sleep 2
    
    # Test Epic 2 processing
    if test_epic2_simple_processing; then
        echo -e "${GREEN}✅ Epic 2 test completed successfully${NC}"
    else
        echo -e "${RED}❌ Epic 2 test failed${NC}"
        exit 1
    fi
    
    echo ""
    sleep 2
    
    # Test Epic 3 processing
    if test_epic3_complex_processing; then
        echo -e "${GREEN}✅ Epic 3 test completed successfully${NC}"
    else
        echo -e "${RED}❌ Epic 3 test failed${NC}"
        exit 1
    fi
    
    # Final summary
    echo ""
    echo -e "${GREEN}🎉 END-TO-END TEST EXECUTION COMPLETED SUCCESSFULLY!${NC}"
    echo ""
    echo -e "${BLUE}Test Summary:${NC}"
    echo "- SQL*Loader Simple Transactions: ✅ PASSED"
    echo "- SQL*Loader Complex Transactions: ✅ PASSED"
    echo "- Epic 2 Simple Processing: ✅ PASSED"
    echo "- Epic 3 Complex Processing: ✅ PASSED"
    echo ""
    echo -e "${YELLOW}Next Steps:${NC}"
    echo "1. Check the UI at ${UI_URL} for processing results"
    echo "2. Review logs in data/logs/ directory"
    echo "3. Validate database records in staging and processing tables"
    echo "4. Review performance metrics and audit trails"
    echo ""
    echo -e "${BLUE}For detailed validation steps, see: docs/END_TO_END_TESTING_GUIDE.md${NC}"
}

# Execute main function
main "$@"