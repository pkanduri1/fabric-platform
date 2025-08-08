#!/bin/bash

# 📊 Fabric Platform - System Status Check Script
# Comprehensive health check for all platform components

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8080"
UI_URL="http://localhost:3000"
PROJECT_ROOT="/Users/pavankanduri/claude-ws/fabric-platform"

echo -e "${BLUE}📊 Fabric Platform - System Status Check${NC}"
echo "========================================"

# Function to check service with timeout
check_service() {
    local url="$1"
    local name="$2"
    local timeout="$3"
    
    if timeout "$timeout" curl -s "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ $name is running${NC}"
        return 0
    else
        echo -e "${RED}❌ $name is not accessible${NC}"
        return 1
    fi
}

# Function to check database connectivity (mock)
check_database() {
    echo -e "${YELLOW}Checking Database Connectivity...${NC}"
    
    # This would normally check actual database connection
    # For now, we'll check if the application can connect to DB via health endpoint
    if curl -s "${BASE_URL}/actuator/health" | grep -q "UP" 2>/dev/null; then
        echo -e "${GREEN}✅ Database connectivity appears healthy${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️ Cannot verify database connectivity${NC}"
        return 1
    fi
}

# Function to check file system permissions and directories
check_filesystem() {
    echo -e "${YELLOW}Checking File System Setup...${NC}"
    
    local directories=("data/input" "data/output" "data/archive" "data/error" "data/logs")
    local all_good=true
    
    for dir in "${directories[@]}"; do
        local full_path="${PROJECT_ROOT}/${dir}"
        if [[ -d "$full_path" && -r "$full_path" && -w "$full_path" ]]; then
            echo -e "${GREEN}✅ $dir directory is accessible${NC}"
        else
            echo -e "${RED}❌ $dir directory is missing or not accessible${NC}"
            all_good=false
        fi
    done
    
    # Check test data files
    if [[ -f "${PROJECT_ROOT}/data/input/TCB_SIMPLE_TRANSACTIONS_20250808.dat" ]]; then
        echo -e "${GREEN}✅ Simple transaction test data file exists${NC}"
    else
        echo -e "${YELLOW}⚠️ Simple transaction test data file missing${NC}"
        all_good=false
    fi
    
    if [[ -f "${PROJECT_ROOT}/data/input/TCB_COMPLEX_TRANSACTIONS_20250808.dat" ]]; then
        echo -e "${GREEN}✅ Complex transaction test data file exists${NC}"
    else
        echo -e "${YELLOW}⚠️ Complex transaction test data file missing${NC}"
        all_good=false
    fi
    
    return $($all_good || echo 1)
}

# Function to check backend API endpoints
check_backend_endpoints() {
    echo -e "${YELLOW}Checking Backend API Endpoints...${NC}"
    
    local endpoints=(
        "/actuator/health:Health Check"
        "/actuator/info:Application Info"
        "/api/v1/batch/status:Batch Status"
        "/api/v1/sqlloader/status:SQL*Loader Status"
    )
    
    local auth_token="mock-jwt-token"  # Replace with actual token
    
    for endpoint_info in "${endpoints[@]}"; do
        IFS=':' read -r endpoint name <<< "$endpoint_info"
        local full_url="${BASE_URL}${endpoint}"
        
        if curl -s --max-time 5 "$full_url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $name endpoint is accessible${NC}"
        else
            echo -e "${YELLOW}⚠️ $name endpoint is not accessible${NC}"
        fi
    done
}

# Function to check frontend accessibility
check_frontend_components() {
    echo -e "${YELLOW}Checking Frontend Components...${NC}"
    
    if check_service "$UI_URL" "Frontend React App" "5"; then
        # Check if main UI components are available
        local ui_response=$(curl -s "$UI_URL" 2>/dev/null || echo "")
        
        if [[ "$ui_response" =~ "Fabric Platform" ]] || [[ "$ui_response" =~ "React App" ]]; then
            echo -e "${GREEN}✅ Frontend content appears to be loading${NC}"
        else
            echo -e "${YELLOW}⚠️ Frontend may not be fully loaded${NC}"
        fi
    fi
}

# Function to check build and compilation status
check_build_status() {
    echo -e "${YELLOW}Checking Build Status...${NC}"
    
    cd "$PROJECT_ROOT/fabric-core"
    
    # Check if target directories exist (indicating successful builds)
    local modules=("fabric-utils" "fabric-data-loader" "fabric-batch" "fabric-api")
    local build_ok=true
    
    for module in "${modules[@]}"; do
        if [[ -d "${module}/target" ]]; then
            echo -e "${GREEN}✅ $module build artifacts exist${NC}"
        else
            echo -e "${RED}❌ $module may not be built${NC}"
            build_ok=false
        fi
    done
    
    # Check frontend build
    cd "$PROJECT_ROOT/fabric-ui"
    if [[ -d "node_modules" ]]; then
        echo -e "${GREEN}✅ Frontend dependencies installed${NC}"
    else
        echo -e "${RED}❌ Frontend dependencies not installed (run: npm install)${NC}"
        build_ok=false
    fi
    
    if $build_ok; then
        echo -e "${GREEN}✅ All build artifacts appear to be present${NC}"
    else
        echo -e "${YELLOW}⚠️ Some build artifacts may be missing${NC}"
    fi
}

# Function to check system resources
check_system_resources() {
    echo -e "${YELLOW}Checking System Resources...${NC}"
    
    # Check available memory
    local available_mem=$(vm_stat | grep "Pages free" | awk '{print $3}' | sed 's/\.//')
    if [[ -n "$available_mem" && "$available_mem" -gt 100000 ]]; then
        echo -e "${GREEN}✅ Sufficient memory available${NC}"
    else
        echo -e "${YELLOW}⚠️ Low memory may affect performance${NC}"
    fi
    
    # Check disk space
    local disk_usage=$(df -h "$PROJECT_ROOT" | awk 'NR==2 {print $5}' | sed 's/%//')
    if [[ -n "$disk_usage" && "$disk_usage" -lt 90 ]]; then
        echo -e "${GREEN}✅ Sufficient disk space available (${disk_usage}% used)${NC}"
    else
        echo -e "${YELLOW}⚠️ Disk space may be running low (${disk_usage}% used)${NC}"
    fi
    
    # Check Java version
    if command -v java &> /dev/null; then
        local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        echo -e "${GREEN}✅ Java version: $java_version${NC}"
    else
        echo -e "${RED}❌ Java is not installed or not in PATH${NC}"
    fi
    
    # Check Node.js version
    if command -v node &> /dev/null; then
        local node_version=$(node --version)
        echo -e "${GREEN}✅ Node.js version: $node_version${NC}"
    else
        echo -e "${RED}❌ Node.js is not installed or not in PATH${NC}"
    fi
}

# Function to provide startup recommendations
provide_startup_recommendations() {
    echo -e "${PURPLE}🚀 Startup Recommendations:${NC}"
    echo ""
    
    echo -e "${YELLOW}To start the backend:${NC}"
    echo "cd $PROJECT_ROOT/fabric-core"
    echo "mvn spring-boot:run -pl fabric-api"
    echo ""
    
    echo -e "${YELLOW}To start the frontend:${NC}"
    echo "cd $PROJECT_ROOT/fabric-ui"
    echo "npm start"
    echo ""
    
    echo -e "${YELLOW}To run end-to-end tests:${NC}"
    echo "cd $PROJECT_ROOT"
    echo "./scripts/run-e2e-test.sh"
    echo ""
}

# Main execution
main() {
    echo -e "${BLUE}Running comprehensive system status check...${NC}"
    echo ""
    
    # Check build status
    check_build_status
    echo ""
    
    # Check file system
    check_filesystem
    echo ""
    
    # Check system resources
    check_system_resources
    echo ""
    
    # Check services
    echo -e "${YELLOW}Checking Services...${NC}"
    local backend_ok=false
    local frontend_ok=false
    
    if check_service "$BASE_URL/actuator/health" "Backend API" "10"; then
        backend_ok=true
        check_database
        echo ""
        check_backend_endpoints
    fi
    
    echo ""
    if check_service "$UI_URL" "Frontend React App" "5"; then
        frontend_ok=true
        check_frontend_components
    fi
    
    echo ""
    echo -e "${BLUE}📋 SYSTEM STATUS SUMMARY${NC}"
    echo "========================"
    
    if $backend_ok; then
        echo -e "${GREEN}✅ Backend: RUNNING${NC}"
    else
        echo -e "${RED}❌ Backend: NOT RUNNING${NC}"
    fi
    
    if $frontend_ok; then
        echo -e "${GREEN}✅ Frontend: RUNNING${NC}"
    else
        echo -e "${RED}❌ Frontend: NOT RUNNING${NC}"
    fi
    
    echo ""
    
    if $backend_ok && $frontend_ok; then
        echo -e "${GREEN}🎉 SYSTEM IS READY FOR END-TO-END TESTING!${NC}"
        echo ""
        echo -e "${BLUE}Next Steps:${NC}"
        echo "1. Run: ./scripts/run-e2e-test.sh"
        echo "2. Or follow: docs/END_TO_END_TESTING_GUIDE.md"
    else
        echo -e "${YELLOW}⚠️ SYSTEM NEEDS SETUP BEFORE TESTING${NC}"
        echo ""
        provide_startup_recommendations
    fi
    
    echo ""
    echo -e "${BLUE}For detailed testing instructions, see:${NC}"
    echo "📋 docs/END_TO_END_TESTING_GUIDE.md"
}

# Execute main function
main "$@"