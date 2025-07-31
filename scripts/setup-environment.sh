#!/bin/bash

# =====================================================
# Fabric Platform - Environment Setup Script
# =====================================================
# Sets up development environment for Fabric Platform
# including database, dependencies, and configuration

set -e  # Exit on any error

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONFIG_DIR="$PROJECT_ROOT/config"
LOG_DIR="$PROJECT_ROOT/logs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
DEFAULT_PROFILE="dev"
SETUP_DATABASE=false
SETUP_REDIS=false
CREATE_DIRS=true
INSTALL_DEPS=true

# Help function
show_help() {
    cat << EOF
Fabric Platform Environment Setup Script

Usage: $0 [OPTIONS]

OPTIONS:
    -p, --profile PROFILE   Environment profile to setup (default: dev)
    -d, --database         Setup database schema and users
    -r, --redis            Setup Redis configuration
    -n, --no-dirs          Skip directory creation
    -s, --skip-deps        Skip dependency installation
    -c, --clean            Clean existing setup before starting
    -h, --help             Show this help message

PROFILES:
    dev         Development environment setup
    test        Testing environment setup
    staging     Staging environment setup
    prod        Production environment setup

EXAMPLES:
    $0                      # Basic development setup
    $0 -p dev -d -r         # Full development setup with database and Redis
    $0 -p test --database   # Test environment with database setup
    $0 --clean              # Clean and setup development environment

DESCRIPTION:
    This script sets up the Fabric Platform development environment:
    1. Creates necessary directories
    2. Installs Java/Maven/Node.js dependencies
    3. Configures environment variables
    4. Sets up database schema (optional)
    5. Configures Redis (optional)
    6. Creates local configuration files

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

while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--profile)
            PROFILE="$2"
            shift 2
            ;;
        -d|--database)
            SETUP_DATABASE=true
            shift
            ;;
        -r|--redis)
            SETUP_REDIS=true
            shift
            ;;
        -n|--no-dirs)
            CREATE_DIRS=false
            shift
            ;;
        -s|--skip-deps)
            INSTALL_DEPS=false
            shift
            ;;
        -c|--clean)
            CLEAN_SETUP=true
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

# Function to check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    local missing_tools=()
    
    # Check Java
    if ! command -v java &> /dev/null; then
        missing_tools+=("java")
    else
        local java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [[ $java_version -lt 17 ]]; then
            log_warning "Java version $java_version detected. Recommended: 17+"
        fi
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        missing_tools+=("maven")
    fi
    
    # Check Node.js (for frontend)
    if ! command -v node &> /dev/null; then
        log_warning "Node.js not found. Frontend setup will be skipped."
    else
        local node_version=$(node --version | cut -d'v' -f2 | cut -d'.' -f1)
        if [[ $node_version -lt 18 ]]; then
            log_warning "Node.js version $node_version detected. Recommended: 18+"
        fi
    fi
    
    # Check Git
    if ! command -v git &> /dev/null; then
        missing_tools+=("git")
    fi
    
    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_info "Please install missing tools before running setup"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Function to create directory structure
create_directories() {
    if [[ "$CREATE_DIRS" == false ]]; then
        return
    fi
    
    log_info "Creating directory structure..."
    
    local directories=(
        "$LOG_DIR"
        "$CONFIG_DIR"
        "$CONFIG_DIR/local"
        "$PROJECT_ROOT/data/input"
        "$PROJECT_ROOT/data/output"
        "$PROJECT_ROOT/data/archive"
        "$PROJECT_ROOT/data/error"
        "$PROJECT_ROOT/temp"
        "$PROJECT_ROOT/certs"
        "$PROJECT_ROOT/backups"
    )
    
    for dir in "${directories[@]}"; do
        if [[ ! -d "$dir" ]]; then
            mkdir -p "$dir"
            log_info "Created: $dir"
        fi
    done
    
    # Create .gitkeep files for empty directories
    for dir in "${directories[@]}"; do
        if [[ ! -f "$dir/.gitkeep" ]] && [[ -z "$(ls -A "$dir" 2>/dev/null)" ]]; then
            touch "$dir/.gitkeep"
        fi
    done
    
    log_success "Directory structure created"
}

# Function to install dependencies
install_dependencies() {
    if [[ "$INSTALL_DEPS" == false ]]; then
        return
    fi
    
    log_info "Installing dependencies..."
    
    # Install backend dependencies
    log_info "Installing backend dependencies..."
    cd "$PROJECT_ROOT/fabric-core"
    mvn dependency:resolve -q
    log_success "Backend dependencies installed"
    
    # Install frontend dependencies (if frontend exists)
    if [[ -d "$PROJECT_ROOT/fabric-ui" ]] && command -v npm &> /dev/null; then
        log_info "Installing frontend dependencies..."
        cd "$PROJECT_ROOT/fabric-ui"
        if [[ -f "package-lock.json" ]]; then
            npm ci
        else
            npm install
        fi
        log_success "Frontend dependencies installed"
    fi
}

# Function to create environment configuration
create_env_config() {
    log_info "Creating environment configuration..."
    
    local env_file="$CONFIG_DIR/local/.env.$PROFILE"
    
    cat > "$env_file" << EOF
# =====================================================
# Fabric Platform - $PROFILE Environment Configuration
# =====================================================
# Generated on $(date)

# Database Configuration
DB_URL=jdbc:oracle:thin:@localhost:1521:XE
DB_USERNAME=fabric_${PROFILE}
DB_PASSWORD=fabric_${PROFILE}_pass

# Security Configuration
JWT_SECRET=fabric-${PROFILE}-jwt-secret-key-minimum-256-bits-required-for-hmac-sha-development-$(date +%s)
LDAP_URLS=ldap://localhost:389
LDAP_BASE_DN=dc=company,dc=com
LDAP_BIND_DN=cn=admin,dc=company,dc=com
LDAP_BIND_PASSWORD=admin_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# Application Configuration
SPRING_PROFILES_ACTIVE=$PROFILE
SERVER_PORT=8080
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_FABRIC=DEBUG

# File Processing Directories
INPUT_DIR=$PROJECT_ROOT/data/input
OUTPUT_DIR=$PROJECT_ROOT/data/output
ARCHIVE_DIR=$PROJECT_ROOT/data/archive
ERROR_DIR=$PROJECT_ROOT/data/error
TEMP_DIR=$PROJECT_ROOT/temp

# Frontend Configuration (if applicable)
REACT_APP_API_URL=http://localhost:8080/api
PORT=3000

# Development specific
CSRF_ENABLED=false
API_DOCS_ENABLED=true
SWAGGER_UI_ENABLED=true
HEALTH_SHOW_DETAILS=always
ACTUATOR_ENDPOINTS=health,info,metrics,configprops,env

EOF
    
    log_success "Environment configuration created: $env_file"
    log_info "Source this file in your shell: source $env_file"
}

# Function to setup database
setup_database() {
    if [[ "$SETUP_DATABASE" == false ]]; then
        return
    fi
    
    log_info "Setting up database..."
    
    # Check if Oracle client is available
    if ! command -v sqlplus &> /dev/null; then
        log_warning "Oracle SQL*Plus not found. Skipping database setup."
        log_info "Please install Oracle client or setup database manually"
        return
    fi
    
    log_info "Creating database user and schema..."
    
    # This is a template - actual implementation would depend on your database setup
    cat > "$CONFIG_DIR/local/setup-db.sql" << EOF
-- Database setup for $PROFILE environment
-- Run this as SYSDBA

-- Create user
CREATE USER fabric_${PROFILE} IDENTIFIED BY fabric_${PROFILE}_pass
  DEFAULT TABLESPACE users
  TEMPORARY TABLESPACE temp;

-- Grant permissions
GRANT CONNECT, RESOURCE TO fabric_${PROFILE};
GRANT CREATE VIEW, CREATE SEQUENCE TO fabric_${PROFILE};
GRANT UNLIMITED TABLESPACE TO fabric_${PROFILE};

-- For batch processing
GRANT CREATE JOB TO fabric_${PROFILE};

EOF
    
    log_success "Database setup script created: $CONFIG_DIR/local/setup-db.sql"
    log_info "Run: sqlplus sys/password@localhost:1521/XE as sysdba @$CONFIG_DIR/local/setup-db.sql"
}

# Function to setup Redis
setup_redis() {
    if [[ "$SETUP_REDIS" == false ]]; then
        return
    fi
    
    log_info "Setting up Redis configuration..."
    
    # Check if Redis is available
    if ! command -v redis-cli &> /dev/null; then
        log_warning "Redis CLI not found. Skipping Redis setup."
        log_info "Please install Redis or configure manually"
        return
    fi
    
    # Test Redis connection
    if redis-cli ping > /dev/null 2>&1; then
        log_success "Redis is running and accessible"
        
        # Set up Redis configurations for Fabric Platform
        redis-cli config set maxmemory-policy allkeys-lru
        redis-cli config set save "900 1 300 10 60 10000"
        
        log_success "Redis configuration completed"
    else
        log_warning "Redis is not running. Please start Redis service."
        log_info "Ubuntu/Debian: sudo systemctl start redis-server"
        log_info "macOS: brew services start redis"
    fi
}

# Function to create local configuration files
create_local_configs() {
    log_info "Creating local configuration files..."
    
    # Create local application.yml override
    cat > "$CONFIG_DIR/local/application-local.yml" << EOF
# Local development overrides
# This file is ignored by git

spring:
  profiles:
    active: $PROFILE
  
  # Database override for local development
  datasource:
    url: \${DB_URL:jdbc:oracle:thin:@localhost:1521:XE}
    username: \${DB_USERNAME:fabric_${PROFILE}}
    password: \${DB_PASSWORD:fabric_${PROFILE}_pass}
    
  # Redis override for local development
  data:
    redis:
      host: \${REDIS_HOST:localhost}
      port: \${REDIS_PORT:6379}
      password: \${REDIS_PASSWORD:}

# Fabric configuration overrides
fabric:
  security:
    jwt:
      secret: \${JWT_SECRET:fabric-local-jwt-secret-key-minimum-256-bits-required}
    cors:
      allowed-origins: \${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:3001}
    csrf:
      enabled: \${CSRF_ENABLED:false}

# Logging configuration for development
logging:
  level:
    com.truist.batch: \${LOG_LEVEL_FABRIC:DEBUG}
    org.springframework.security: DEBUG
  file:
    name: $LOG_DIR/fabric-$PROFILE.log

EOF
    
    log_success "Local configuration created: $CONFIG_DIR/local/application-local.yml"
}

# Function to clean existing setup
clean_setup() {
    if [[ "$CLEAN_SETUP" != true ]]; then
        return
    fi
    
    log_info "Cleaning existing setup..."
    
    # Stop any running services
    if [[ -f "$SCRIPT_DIR/stop-all.sh" ]]; then
        "$SCRIPT_DIR/stop-all.sh" --force || true
    fi
    
    # Clean build artifacts
    if [[ -d "$PROJECT_ROOT/fabric-core" ]]; then
        cd "$PROJECT_ROOT/fabric-core"
        mvn clean -q || true
    fi
    
    # Clean frontend build
    if [[ -d "$PROJECT_ROOT/fabric-ui" ]]; then
        cd "$PROJECT_ROOT/fabric-ui"
        rm -rf build node_modules || true
    fi
    
    # Clean logs
    rm -rf "$LOG_DIR"/*.log || true
    
    # Clean temp files
    rm -rf "$PROJECT_ROOT/temp"/* || true
    
    log_success "Cleanup completed"
}

# Function to validate setup
validate_setup() {
    log_info "Validating setup..."
    
    local validation_errors=()
    
    # Check if backend can compile
    cd "$PROJECT_ROOT/fabric-core"
    if ! mvn compile -q; then
        validation_errors+=("Backend compilation failed")
    fi
    
    # Check if frontend can be prepared (if exists)
    if [[ -d "$PROJECT_ROOT/fabric-ui" ]] && command -v npm &> /dev/null; then
        cd "$PROJECT_ROOT/fabric-ui"
        if [[ ! -d "node_modules" ]]; then
            validation_errors+=("Frontend dependencies not installed")
        fi
    fi
    
    # Check configuration files
    if [[ ! -f "$CONFIG_DIR/local/.env.$PROFILE" ]]; then
        validation_errors+=("Environment configuration file missing")
    fi
    
    if [[ ${#validation_errors[@]} -eq 0 ]]; then
        log_success "Setup validation passed"
        return 0
    else
        log_error "Setup validation failed:"
        for error in "${validation_errors[@]}"; do
            log_error "  - $error"
        done
        return 1
    fi
}

# Function to show next steps
show_next_steps() {
    log_success "Environment setup completed!"
    
    echo ""
    log_info "Next steps:"
    echo "1. Source environment variables:"
    echo "   source $CONFIG_DIR/local/.env.$PROFILE"
    echo ""
    echo "2. Setup database (if not done automatically):"
    echo "   sqlplus sys/password@localhost:1521/XE as sysdba @$CONFIG_DIR/local/setup-db.sql"
    echo ""
    echo "3. Start services:"
    echo "   $SCRIPT_DIR/start-backend.sh -p $PROFILE"
    if [[ -d "$PROJECT_ROOT/fabric-ui" ]]; then
        echo "   $SCRIPT_DIR/start-frontend.sh"
    fi
    echo ""
    echo "4. Access the application:"
    echo "   Backend: http://localhost:8080/api"
    echo "   Health: http://localhost:8080/api/actuator/health"
    echo "   Swagger: http://localhost:8080/api/swagger-ui.html"
    if [[ -d "$PROJECT_ROOT/fabric-ui" ]]; then
        echo "   Frontend: http://localhost:3000"
    fi
    echo ""
    echo "5. View logs:"
    echo "   tail -f $LOG_DIR/fabric-$PROFILE.log"
    echo ""
    log_info "For more information, see README.md and DATABASE-SETUP.md"
}

# Main execution
main() {
    log_info "Fabric Platform Environment Setup"
    log_info "================================="
    log_info "Profile: $PROFILE"
    log_info "Database setup: $SETUP_DATABASE"
    log_info "Redis setup: $SETUP_REDIS"
    echo ""
    
    # Clean if requested
    clean_setup
    
    # Check prerequisites
    check_prerequisites
    
    # Create directories
    create_directories
    
    # Install dependencies
    install_dependencies
    
    # Create environment configuration
    create_env_config
    
    # Setup database
    setup_database
    
    # Setup Redis
    setup_redis
    
    # Create local configuration files
    create_local_configs
    
    # Validate setup
    if validate_setup; then
        show_next_steps
    else
        log_error "Setup completed with validation errors"
        exit 1
    fi
}

# Run main function
main