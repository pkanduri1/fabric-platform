#!/bin/bash

# =====================================================
# Fabric Platform - Backend Startup Script
# =====================================================
# Starts the Spring Boot backend application with
# proper environment configuration and monitoring

set -e  # Exit on any error

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_ROOT/fabric-core/fabric-api"
LOG_DIR="$PROJECT_ROOT/logs"
PID_FILE="$LOG_DIR/fabric-backend.pid"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
DEFAULT_PROFILE="dev"
DEFAULT_PORT="8080"
DEFAULT_MEMORY="2g"

# Help function
show_help() {
    cat << EOF
Fabric Platform Backend Startup Script

Usage: $0 [OPTIONS]

OPTIONS:
    -p, --profile PROFILE     Spring profile to use (default: dev)
    -P, --port PORT          Server port (default: 8080)
    -m, --memory MEMORY      JVM memory allocation (default: 2g)
    -b, --build             Build before starting
    -d, --daemon            Run as daemon (background)
    -w, --wait              Wait for startup completion
    -h, --help              Show this help message

PROFILES:
    dev         Development environment (default)
    test        Testing environment  
    staging     Staging environment
    prod        Production environment
    mongodb     MongoDB configuration
    redis       Redis configuration
    aws         AWS RDS configuration

EXAMPLES:
    $0                           # Start with development profile
    $0 -p prod -P 8443 -m 4g    # Start production with 4GB memory on port 8443
    $0 -b -d -w                 # Build, start as daemon, and wait for startup
    $0 -p staging --build       # Build and start with staging profile

ENVIRONMENT VARIABLES:
    SPRING_PROFILES_ACTIVE      Override profile selection
    SERVER_PORT                 Override server port
    JAVA_OPTS                   Additional JVM options
    DB_URL                      Database connection URL
    DB_USERNAME                 Database username
    DB_PASSWORD                 Database password

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
PROFILE="$DEFAULT_PROFILE"
PORT="$DEFAULT_PORT"
MEMORY="$DEFAULT_MEMORY"
BUILD=false
DAEMON=false
WAIT=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--profile)
            PROFILE="$2"
            shift 2
            ;;
        -P|--port)
            PORT="$2"
            shift 2
            ;;
        -m|--memory)
            MEMORY="$2"
            shift 2
            ;;
        -b|--build)
            BUILD=true
            shift
            ;;
        -d|--daemon)
            DAEMON=true
            shift
            ;;
        -w|--wait)
            WAIT=true
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

# Override with environment variables if set
PROFILE="${SPRING_PROFILES_ACTIVE:-$PROFILE}"
PORT="${SERVER_PORT:-$PORT}"

# Validate profile
case $PROFILE in
    dev|test|staging|prod|mongodb|redis|aws)
        ;;
    *)
        log_error "Invalid profile: $PROFILE"
        log_info "Valid profiles: dev, test, staging, prod, mongodb, redis, aws"
        exit 1
        ;;
esac

# Create logs directory
mkdir -p "$LOG_DIR"

# Function to check if backend is already running
is_running() {
    if [[ -f "$PID_FILE" ]]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            return 0
        else
            rm -f "$PID_FILE"
            return 1
        fi
    fi
    return 1
}

# Function to wait for application startup
wait_for_startup() {
    local max_attempts=60
    local attempt=1
    
    log_info "Waiting for application startup..."
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s "http://localhost:$PORT/api/actuator/health" > /dev/null 2>&1; then
            log_success "Application is ready at http://localhost:$PORT/api"
            return 0
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    echo ""
    log_error "Application failed to start within $(($max_attempts * 2)) seconds"
    return 1
}

# Function to build the application
build_application() {
    log_info "Building application..."
    
    cd "$PROJECT_ROOT/fabric-core"
    
    if mvn clean package -DskipTests -q; then
        log_success "Build completed successfully"
    else
        log_error "Build failed"
        exit 1
    fi
}

# Function to start the application
start_application() {
    local jar_file
    
    # Find the JAR file
    jar_file=$(find "$BACKEND_DIR/target" -name "fabric-api-*.jar" -not -name "*-sources.jar" | head -1)
    
    if [[ ! -f "$jar_file" ]]; then
        log_error "JAR file not found. Please build the application first."
        log_info "Run: $0 --build"
        exit 1
    fi
    
    # Prepare JVM options
    local jvm_opts="-Xmx$MEMORY -Xms${MEMORY%g}g"
    jvm_opts="$jvm_opts -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    jvm_opts="$jvm_opts -Dspring.profiles.active=$PROFILE"
    jvm_opts="$jvm_opts -Dserver.port=$PORT"
    
    # Add user-specified JVM options
    if [[ -n "$JAVA_OPTS" ]]; then
        jvm_opts="$jvm_opts $JAVA_OPTS"
    fi
    
    # Log file
    local log_file="$LOG_DIR/fabric-backend-$PROFILE.log"
    
    log_info "Starting Fabric Platform Backend..."
    log_info "Profile: $PROFILE"
    log_info "Port: $PORT"
    log_info "Memory: $MEMORY"
    log_info "JAR: $(basename "$jar_file")"
    log_info "Log: $log_file"
    
    if [[ "$DAEMON" == true ]]; then
        # Start as daemon
        nohup java $jvm_opts -jar "$jar_file" > "$log_file" 2>&1 &
        local pid=$!
        echo $pid > "$PID_FILE"
        
        log_success "Backend started as daemon with PID: $pid"
        
        if [[ "$WAIT" == true ]]; then
            wait_for_startup
        else
            log_info "Use 'tail -f $log_file' to monitor logs"
            log_info "Use '$SCRIPT_DIR/stop-backend.sh' to stop"
        fi
    else
        # Start in foreground
        log_info "Starting in foreground mode (Ctrl+C to stop)..."
        java $jvm_opts -jar "$jar_file"
    fi
}

# Main execution
main() {
    log_info "Fabric Platform Backend Startup"
    log_info "================================"
    
    # Check if already running
    if is_running; then
        local pid=$(cat "$PID_FILE")
        log_warning "Backend is already running with PID: $pid"
        log_info "Use '$SCRIPT_DIR/stop-backend.sh' to stop first"
        exit 1
    fi
    
    # Change to backend directory
    if [[ ! -d "$BACKEND_DIR" ]]; then
        log_error "Backend directory not found: $BACKEND_DIR"
        exit 1
    fi
    
    cd "$BACKEND_DIR"
    
    # Build if requested
    if [[ "$BUILD" == true ]]; then
        build_application
    fi
    
    # Start the application
    start_application
}

# Trap Ctrl+C for cleanup
cleanup() {
    log_info "Received interrupt signal, shutting down..."
    if [[ -f "$PID_FILE" ]]; then
        local pid=$(cat "$PID_FILE")
        kill -TERM "$pid" 2>/dev/null || true
        rm -f "$PID_FILE"
    fi
    exit 0
}

trap cleanup SIGINT SIGTERM

# Run main function
main