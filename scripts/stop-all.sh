#!/bin/bash

# =====================================================
# Fabric Platform - Stop All Services Script
# =====================================================
# Stops all Fabric Platform services (backend, frontend)
# with graceful shutdown and cleanup

set -e  # Exit on any error

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LOG_DIR="$PROJECT_ROOT/logs"

# PID files
BACKEND_PID_FILE="$LOG_DIR/fabric-backend.pid"
FRONTEND_PID_FILE="$LOG_DIR/fabric-frontend.pid"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
FORCE=false
WAIT_TIMEOUT=30

# Help function
show_help() {
    cat << EOF
Fabric Platform Stop All Services Script

Usage: $0 [OPTIONS]

OPTIONS:
    -f, --force             Force kill processes if graceful shutdown fails
    -t, --timeout SECONDS   Wait timeout for graceful shutdown (default: 30)
    -p, --processes         Stop by process name patterns (fallback method)
    -k, --kill-ports        Kill processes using default ports (8080, 3000)
    -h, --help              Show this help message

EXAMPLES:
    $0                      # Graceful shutdown with 30s timeout
    $0 -f                   # Force shutdown if graceful fails
    $0 -t 60                # Wait up to 60 seconds for graceful shutdown
    $0 -p                   # Stop by process patterns (fabric-api, node)
    $0 -k                   # Kill by ports 8080 and 3000

DESCRIPTION:
    This script attempts to stop all Fabric Platform services gracefully:
    1. Sends TERM signal to processes
    2. Waits for graceful shutdown
    3. If timeout exceeded, sends KILL signal (with --force)
    4. Cleans up PID files and temporary resources

EOF
}

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--force)
            FORCE=true
            shift
            ;;
        -t|--timeout)
            WAIT_TIMEOUT="$2"
            shift 2
            ;;
        -p|--processes)
            STOP_BY_PROCESSES=true
            shift
            ;;
        -k|--kill-ports)
            KILL_BY_PORTS=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Function to check if process is running
is_process_running() {
    local pid="$1"
    ps -p "$pid" > /dev/null 2>&1
}

# Function to wait for process to stop
wait_for_process_stop() {
    local pid="$1"
    local timeout="$2"
    local process_name="${3:-process}"
    
    log_info "Waiting for $process_name (PID: $pid) to stop..."
    
    local count=0
    while [[ $count -lt $timeout ]] && is_process_running "$pid"; do
        echo -n "."
        sleep 1
        ((count++))
    done
    
    echo ""
    
    if is_process_running "$pid"; then
        return 1  # Process still running
    else
        return 0  # Process stopped
    fi
}

# Function to stop process gracefully
stop_process_graceful() {
    local pid="$1"
    local process_name="${2:-process}"
    
    if ! is_process_running "$pid"; then
        log_info "$process_name is not running"
        return 0
    fi
    
    log_info "Stopping $process_name gracefully (PID: $pid)..."
    
    # Send TERM signal
    kill -TERM "$pid" 2>/dev/null || {
        log_warning "Failed to send TERM signal to $process_name"
        return 1
    }
    
    # Wait for graceful shutdown
    if wait_for_process_stop "$pid" "$WAIT_TIMEOUT" "$process_name"; then
        log_success "$process_name stopped gracefully"
        return 0
    else
        log_warning "$process_name did not stop within $WAIT_TIMEOUT seconds"
        
        if [[ "$FORCE" == true ]]; then
            log_info "Force killing $process_name..."
            kill -KILL "$pid" 2>/dev/null || {
                log_error "Failed to force kill $process_name"
                return 1
            }
            
            sleep 2
            
            if is_process_running "$pid"; then
                log_error "Failed to stop $process_name even with force"
                return 1
            else
                log_success "$process_name force killed"
                return 0
            fi
        else
            log_error "$process_name is still running. Use --force to kill it."
            return 1
        fi
    fi
}

# Function to stop backend service
stop_backend() {
    if [[ -f "$BACKEND_PID_FILE" ]]; then
        local pid=$(cat "$BACKEND_PID_FILE")
        
        if stop_process_graceful "$pid" "Backend"; then
            rm -f "$BACKEND_PID_FILE"
        else
            return 1
        fi
    else
        log_info "Backend PID file not found"
        
        # Try to find backend process by pattern
        local backend_pids=$(pgrep -f "fabric-api.*\.jar" || true)
        if [[ -n "$backend_pids" ]]; then
            log_info "Found backend processes by pattern: $backend_pids"
            for pid in $backend_pids; do
                stop_process_graceful "$pid" "Backend (pattern match)"
            done
        fi
    fi
}

# Function to stop frontend service
stop_frontend() {
    if [[ -f "$FRONTEND_PID_FILE" ]]; then
        local pid=$(cat "$FRONTEND_PID_FILE")
        
        if stop_process_graceful "$pid" "Frontend"; then
            rm -f "$FRONTEND_PID_FILE"
        else
            return 1
        fi
    else
        log_info "Frontend PID file not found"
        
        # Try to find frontend processes by pattern
        local frontend_pids=$(pgrep -f "node.*start" || pgrep -f "serve.*build" || true)
        if [[ -n "$frontend_pids" ]]; then
            log_info "Found frontend processes by pattern: $frontend_pids"
            for pid in $frontend_pids; do
                stop_process_graceful "$pid" "Frontend (pattern match)"
            done
        fi
    fi
}

# Function to stop processes by name patterns
stop_by_processes() {
    log_info "Stopping processes by name patterns..."
    
    # Backend patterns
    local backend_patterns=("fabric-api" "java.*fabric" "InterfaceBatchApplication")
    for pattern in "${backend_patterns[@]}"; do
        local pids=$(pgrep -f "$pattern" || true)
        if [[ -n "$pids" ]]; then
            log_info "Found processes matching '$pattern': $pids"
            for pid in $pids; do
                stop_process_graceful "$pid" "Process ($pattern)"
            done
        fi
    done
    
    # Frontend patterns
    local frontend_patterns=("node.*start" "serve.*build" "react-scripts")
    for pattern in "${frontend_patterns[@]}"; do
        local pids=$(pgrep -f "$pattern" || true)
        if [[ -n "$pids" ]]; then
            log_info "Found processes matching '$pattern': $pids"
            for pid in $pids; do
                stop_process_graceful "$pid" "Process ($pattern)"
            done
        fi
    done
}

# Function to kill processes by ports
kill_by_ports() {
    log_info "Killing processes using default ports..."
    
    local ports=(8080 3000 8443)
    for port in "${ports[@]}"; do
        local pids=$(lsof -ti :$port 2>/dev/null || true)
        if [[ -n "$pids" ]]; then
            log_info "Found processes using port $port: $pids"
            for pid in $pids; do
                local process_name=$(ps -p $pid -o comm= 2>/dev/null || echo "unknown")
                stop_process_graceful "$pid" "Process on port $port ($process_name)"
            done
        else
            log_info "No processes found using port $port"
        fi
    done
}

# Function to cleanup resources
cleanup_resources() {
    log_info "Cleaning up resources..."
    
    # Remove PID files
    rm -f "$BACKEND_PID_FILE" "$FRONTEND_PID_FILE"
    
    # Clean up temporary files
    local temp_patterns=(
        "/tmp/fabric-*"
        "/tmp/spring-boot-*fabric*"
        "/tmp/tomcat-*fabric*"
    )
    
    for pattern in "${temp_patterns[@]}"; do
        find /tmp -name "$(basename "$pattern")" -type d -user "$(whoami)" -exec rm -rf {} + 2>/dev/null || true
    done
    
    log_success "Resource cleanup completed"
}

# Function to show running status
show_status() {
    log_info "Checking for remaining Fabric Platform processes..."
    
    # Check by PID files
    local still_running=false
    
    if [[ -f "$BACKEND_PID_FILE" ]]; then
        local pid=$(cat "$BACKEND_PID_FILE")
        if is_process_running "$pid"; then
            log_warning "Backend still running (PID: $pid)"
            still_running=true
        fi
    fi
    
    if [[ -f "$FRONTEND_PID_FILE" ]]; then
        local pid=$(cat "$FRONTEND_PID_FILE")
        if is_process_running "$pid"; then
            log_warning "Frontend still running (PID: $pid)"
            still_running=true
        fi
    fi
    
    # Check by process patterns
    local fabric_processes=$(pgrep -f "fabric" || true)
    if [[ -n "$fabric_processes" ]]; then
        log_warning "Found processes with 'fabric' in name: $fabric_processes"
        still_running=true
    fi
    
    # Check by ports
    local port_processes=$(lsof -ti :8080,:3000,:8443 2>/dev/null || true)
    if [[ -n "$port_processes" ]]; then
        log_warning "Found processes using default ports: $port_processes"
        still_running=true
    fi
    
    if [[ "$still_running" == false ]]; then
        log_success "All Fabric Platform services have been stopped"
    else
        log_warning "Some processes may still be running"
        log_info "Use --force option for forceful shutdown"
        log_info "Use --processes option to stop by process patterns"
        log_info "Use --kill-ports option to kill by port usage"
    fi
}

# Main execution
main() {
    log_info "Fabric Platform - Stop All Services"
    log_info "===================================="
    
    local success=true
    
    # Handle special modes
    if [[ "$STOP_BY_PROCESSES" == true ]]; then
        stop_by_processes
        cleanup_resources
        show_status
        return
    fi
    
    if [[ "$KILL_BY_PORTS" == true ]]; then
        kill_by_ports
        cleanup_resources
        show_status
        return
    fi
    
    # Standard shutdown process
    log_info "Configuration:"
    log_info "  Force: $FORCE"
    log_info "  Timeout: ${WAIT_TIMEOUT}s"
    
    # Stop backend
    log_info "Stopping backend service..."
    if ! stop_backend; then
        log_error "Failed to stop backend service"
        success=false
    fi
    
    # Stop frontend
    log_info "Stopping frontend service..."
    if ! stop_frontend; then
        log_error "Failed to stop frontend service"
        success=false
    fi
    
    # Cleanup resources
    cleanup_resources
    
    # Show final status
    show_status
    
    if [[ "$success" == true ]]; then
        log_success "All services stopped successfully"
        exit 0
    else
        log_error "Some services failed to stop"
        exit 1
    fi
}

# Run main function
main