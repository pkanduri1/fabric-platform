#!/bin/bash

# =====================================================
# Fabric Platform - Frontend Startup Script
# =====================================================
# Starts the React frontend application with
# proper environment configuration and build options

set -e  # Exit on any error

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$PROJECT_ROOT/fabric-ui"
LOG_DIR="$PROJECT_ROOT/logs"
PID_FILE="$LOG_DIR/fabric-frontend.pid"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
DEFAULT_PORT="3000"
DEFAULT_MODE="development"
DEFAULT_API_URL="http://localhost:8080/api"

# Help function
show_help() {
    cat << EOF
Fabric Platform Frontend Startup Script

Usage: $0 [OPTIONS]

OPTIONS:
    -p, --port PORT          Frontend port (default: 3000)
    -m, --mode MODE          Build mode: development|production (default: development)
    -a, --api-url URL        Backend API URL (default: http://localhost:8080/api)
    -b, --build             Build before starting
    -d, --daemon            Run as daemon (background)
    -s, --serve             Serve production build instead of dev server
    -w, --wait              Wait for startup completion
    -i, --install           Install dependencies first
    -h, --help              Show this help message

MODES:
    development     Development server with hot reload (default)
    production      Production build served with static server

EXAMPLES:
    $0                              # Start development server on port 3000
    $0 -p 3001 -a http://prod:8080/api  # Custom port and API URL
    $0 -b -s                        # Build and serve production version
    $0 -i -b -d -w                  # Install, build, start as daemon, wait for startup
    $0 -m production -s             # Serve production build

ENVIRONMENT VARIABLES:
    PORT                    Override frontend port
    REACT_APP_API_URL       Backend API URL for React app
    NODE_ENV                Node environment (development|production)
    BROWSER                 Browser to open (none to disable)

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
PORT="$DEFAULT_PORT"
MODE="$DEFAULT_MODE"
API_URL="$DEFAULT_API_URL"
BUILD=false
DAEMON=false
SERVE=false
WAIT=false
INSTALL=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--port)
            PORT="$2"
            shift 2
            ;;
        -m|--mode)
            MODE="$2"
            shift 2
            ;;
        -a|--api-url)
            API_URL="$2"
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
        -s|--serve)
            SERVE=true
            shift
            ;;
        -w|--wait)
            WAIT=true
            shift
            ;;
        -i|--install)
            INSTALL=true
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
PORT="${PORT:-$DEFAULT_PORT}"
API_URL="${REACT_APP_API_URL:-$API_URL}"

# Validate mode
case $MODE in
    development|production)
        ;;
    *)
        log_error "Invalid mode: $MODE"
        log_info "Valid modes: development, production"
        exit 1
        ;;
esac

# Create logs directory
mkdir -p "$LOG_DIR"

# Function to check if frontend is already running
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
    local max_attempts=30
    local attempt=1
    
    log_info "Waiting for frontend startup..."
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s "http://localhost:$PORT" > /dev/null 2>&1; then
            log_success "Frontend is ready at http://localhost:$PORT"
            return 0
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    echo ""
    log_error "Frontend failed to start within $(($max_attempts * 2)) seconds"
    return 1
}

# Function to check if Node.js and npm are installed
check_prerequisites() {
    if ! command -v node &> /dev/null; then
        log_error "Node.js is not installed"
        log_info "Please install Node.js 18+ from https://nodejs.org/"
        exit 1
    fi
    
    if ! command -v npm &> /dev/null; then
        log_error "npm is not installed"
        log_info "Please install npm or use Node.js installer"
        exit 1
    fi
    
    local node_version=$(node --version | cut -d'v' -f2 | cut -d'.' -f1)
    if [[ $node_version -lt 18 ]]; then
        log_warning "Node.js version $node_version detected. Recommended: 18+"
    fi
}

# Function to install dependencies
install_dependencies() {
    log_info "Installing dependencies..."
    
    if [[ -f "package-lock.json" ]]; then
        npm ci
    else
        npm install
    fi
    
    log_success "Dependencies installed successfully"
}

# Function to build the application
build_application() {
    log_info "Building frontend application..."
    
    # Set environment variables for build
    export NODE_ENV="production"
    export REACT_APP_API_URL="$API_URL"
    export GENERATE_SOURCEMAP="false"
    
    if npm run build; then
        log_success "Build completed successfully"
        log_info "Build output: $(du -sh build 2>/dev/null | cut -f1 || echo 'Unknown')"
    else
        log_error "Build failed"
        exit 1
    fi
}

# Function to start development server
start_dev_server() {
    log_info "Starting development server..."
    
    # Set environment variables
    export PORT="$PORT"
    export REACT_APP_API_URL="$API_URL"
    export BROWSER="none"  # Don't auto-open browser
    
    local log_file="$LOG_DIR/fabric-frontend-dev.log"
    
    if [[ "$DAEMON" == true ]]; then
        # Start as daemon
        nohup npm start > "$log_file" 2>&1 &
        local pid=$!
        echo $pid > "$PID_FILE"
        
        log_success "Development server started as daemon with PID: $pid"
        log_info "Log: $log_file"
        
        if [[ "$WAIT" == true ]]; then
            wait_for_startup
        fi
    else
        # Start in foreground
        log_info "Starting in foreground mode (Ctrl+C to stop)..."
        npm start
    fi
}

# Function to serve production build
serve_production() {
    log_info "Serving production build..."
    
    # Check if build directory exists
    if [[ ! -d "build" ]]; then
        log_error "Build directory not found. Run with --build first."
        exit 1
    fi
    
    # Check if serve is installed globally
    if ! command -v serve &> /dev/null; then
        log_info "Installing 'serve' globally..."
        npm install -g serve
    fi
    
    local log_file="$LOG_DIR/fabric-frontend-prod.log"
    
    if [[ "$DAEMON" == true ]]; then
        # Start as daemon
        nohup serve -s build -l "$PORT" > "$log_file" 2>&1 &
        local pid=$!
        echo $pid > "$PID_FILE"
        
        log_success "Production server started as daemon with PID: $pid"
        log_info "Log: $log_file"
        
        if [[ "$WAIT" == true ]]; then
            wait_for_startup
        fi
    else
        # Start in foreground
        log_info "Starting in foreground mode (Ctrl+C to stop)..."
        serve -s build -l "$PORT"
    fi
}

# Function to start the application
start_application() {
    if [[ "$SERVE" == true ]] || [[ "$MODE" == "production" ]]; then
        serve_production
    else
        start_dev_server
    fi
}

# Main execution
main() {
    log_info "Fabric Platform Frontend Startup"
    log_info "================================="
    
    # Check if already running
    if is_running; then
        local pid=$(cat "$PID_FILE")
        log_warning "Frontend is already running with PID: $pid"
        log_info "Use '$SCRIPT_DIR/stop-frontend.sh' to stop first"
        exit 1
    fi
    
    # Check prerequisites
    check_prerequisites
    
    # Check if frontend directory exists
    if [[ ! -d "$FRONTEND_DIR" ]]; then
        log_warning "Frontend directory not found: $FRONTEND_DIR"
        log_info "This script assumes React frontend exists in fabric-ui directory"
        log_info "If frontend is integrated in backend, use backend startup script only"
        exit 1
    fi
    
    # Change to frontend directory
    cd "$FRONTEND_DIR"
    
    # Install dependencies if requested
    if [[ "$INSTALL" == true ]]; then
        install_dependencies
    fi
    
    # Build if requested
    if [[ "$BUILD" == true ]]; then
        build_application
    fi
    
    log_info "Configuration:"
    log_info "  Port: $PORT"
    log_info "  Mode: $MODE"
    log_info "  API URL: $API_URL"
    log_info "  Serve: $SERVE"
    
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