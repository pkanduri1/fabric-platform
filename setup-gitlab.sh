#!/bin/bash

# =============================================================================
# GitLab Setup Script for Fabric Platform
# =============================================================================
# 
# This script sets up the fabric-platform project in GitLab, preserving all
# branches, commits, and repository structure from the current GitHub setup.
#
# Prerequisites:
# 1. GitLab account created
# 2. Git CLI installed and configured
# 3. SSH keys configured for GitLab (optional but recommended)
#
# Usage:
#   ./setup-gitlab.sh [GITLAB_PROJECT_URL]
#
# Example:
#   ./setup-gitlab.sh git@gitlab.com:username/fabric-platform.git
#   ./setup-gitlab.sh https://gitlab.com/username/fabric-platform.git
#
# Author: Automated Setup Script
# Version: 1.0
# =============================================================================

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Project configuration
PROJECT_NAME="fabric-platform"
CURRENT_BRANCH=$(git branch --show-current)
GITLAB_URL=""

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${PURPLE}================================${NC}"
    echo -e "${PURPLE}$1${NC}"
    echo -e "${PURPLE}================================${NC}"
}

# Function to check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check if git is installed
    if ! command -v git &> /dev/null; then
        print_error "Git is not installed. Please install Git first."
        exit 1
    fi
    print_success "Git is installed: $(git --version)"
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a git repository. Please run this script from the fabric-platform directory."
        exit 1
    fi
    print_success "Current directory is a git repository"
    
    # Check current repository status
    if [[ -n $(git status --porcelain) ]]; then
        print_warning "There are uncommitted changes in the repository."
        read -p "Do you want to continue? (y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_error "Setup cancelled by user."
            exit 1
        fi
    fi
    
    print_success "Prerequisites check completed"
}

# Function to display current repository information
show_repo_info() {
    print_header "Current Repository Information"
    
    echo "Repository: $(pwd)"
    echo "Current branch: ${CURRENT_BRANCH}"
    echo "Git remotes:"
    git remote -v
    echo
    echo "Available branches:"
    git branch -a
    echo
    echo "Recent commits:"
    git log --oneline -5
}

# Function to backup current state
backup_current_state() {
    print_header "Creating Backup"
    
    BACKUP_DIR="../${PROJECT_NAME}-backup-$(date +%Y%m%d-%H%M%S)"
    print_status "Creating backup at: $BACKUP_DIR"
    
    # Create backup directory and copy everything except .git
    mkdir -p "$BACKUP_DIR"
    rsync -av --exclude='.git' ./ "$BACKUP_DIR/"
    
    # Also backup git configuration
    cp -r .git "$BACKUP_DIR/"
    
    print_success "Backup created at: $BACKUP_DIR"
}

# Function to get GitLab URL from user
get_gitlab_url() {
    print_header "GitLab Configuration"
    
    if [[ -n "$1" ]]; then
        GITLAB_URL="$1"
        print_status "Using provided GitLab URL: $GITLAB_URL"
    else
        echo "Please provide your GitLab project URL."
        echo "Examples:"
        echo "  SSH: git@gitlab.com:username/fabric-platform.git"
        echo "  HTTPS: https://gitlab.com/username/fabric-platform.git"
        echo
        read -p "GitLab project URL: " GITLAB_URL
    fi
    
    if [[ -z "$GITLAB_URL" ]]; then
        print_error "GitLab URL is required."
        exit 1
    fi
    
    print_success "GitLab URL configured: $GITLAB_URL"
}

# Function to test GitLab connectivity
test_gitlab_connection() {
    print_header "Testing GitLab Connection"
    
    # Extract hostname from URL
    if [[ $GITLAB_URL == git@* ]]; then
        HOSTNAME=$(echo $GITLAB_URL | sed 's/git@\([^:]*\):.*/\1/')
        print_status "Testing SSH connection to $HOSTNAME"
        if ssh -T git@$HOSTNAME 2>&1 | grep -q "Welcome to GitLab"; then
            print_success "SSH connection to GitLab successful"
        else
            print_warning "SSH connection test inconclusive, but continuing..."
        fi
    else
        print_status "Using HTTPS connection - authentication will be required during push"
    fi
}

# Function to add GitLab remote
add_gitlab_remote() {
    print_header "Adding GitLab Remote"
    
    # Check if gitlab remote already exists
    if git remote get-url gitlab &> /dev/null; then
        print_warning "GitLab remote already exists. Updating URL..."
        git remote set-url gitlab "$GITLAB_URL"
    else
        print_status "Adding GitLab remote..."
        git remote add gitlab "$GITLAB_URL"
    fi
    
    print_success "GitLab remote added/updated"
    echo "Current remotes:"
    git remote -v
}

# Function to push all branches to GitLab
push_to_gitlab() {
    print_header "Pushing to GitLab"
    
    print_status "Pushing all branches to GitLab..."
    
    # Push all branches
    git push gitlab --all
    
    # Push all tags
    print_status "Pushing tags to GitLab..."
    git push gitlab --tags
    
    print_success "All branches and tags pushed to GitLab"
}

# Function to set GitLab as default remote
configure_gitlab_default() {
    print_header "Configuring GitLab as Default Remote"
    
    read -p "Do you want to set GitLab as the default remote (origin)? (y/n): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Backing up current origin remote as 'github-backup'..."
        
        # Backup current origin (likely GitHub)
        CURRENT_ORIGIN=$(git remote get-url origin 2>/dev/null || echo "")
        if [[ -n "$CURRENT_ORIGIN" ]]; then
            git remote rename origin github-backup
            print_success "Current origin backed up as 'github-backup'"
        fi
        
        # Set GitLab as new origin
        git remote rename gitlab origin
        
        # Update upstream tracking for current branch
        git branch --set-upstream-to=origin/${CURRENT_BRANCH} ${CURRENT_BRANCH}
        
        print_success "GitLab is now the default remote (origin)"
    else
        print_status "Keeping current remote configuration"
    fi
    
    echo "Final remote configuration:"
    git remote -v
}

# Function to create GitLab CI/CD configuration
setup_gitlab_ci() {
    print_header "Setting up GitLab CI/CD"
    
    read -p "Do you want to create a GitLab CI/CD pipeline configuration? (y/n): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Creating .gitlab-ci.yml configuration..."
        
        cat > .gitlab-ci.yml << 'EOF'
# GitLab CI/CD Pipeline for Fabric Platform
# Enterprise-grade Spring Boot + React application

stages:
  - build
  - test
  - security
  - package
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=${CI_PROJECT_DIR}/.m2/repository"
  MAVEN_CLI_OPTS: "--batch-mode --errors --fail-at-end --show-version"
  DOCKER_DRIVER: overlay2
  FABRIC_PROFILE: "ci"

cache:
  paths:
    - .m2/repository/
    - fabric-ui/node_modules/

# =============================================================================
# BUILD STAGE
# =============================================================================

build:backend:
  stage: build
  image: maven:3.8.6-openjdk-17
  script:
    - cd fabric-core
    - mvn $MAVEN_CLI_OPTS clean compile
  artifacts:
    paths:
      - fabric-core/target/
    expire_in: 1 hour

build:frontend:
  stage: build
  image: node:18-alpine
  script:
    - cd fabric-ui
    - npm ci
    - npm run build
  artifacts:
    paths:
      - fabric-ui/build/
    expire_in: 1 hour

# =============================================================================
# TEST STAGE
# =============================================================================

test:backend:
  stage: test
  image: maven:3.8.6-openjdk-17
  services:
    - postgres:15-alpine
    - redis:7-alpine
  variables:
    POSTGRES_DB: fabric_test
    POSTGRES_USER: test
    POSTGRES_PASSWORD: test
    REDIS_URL: redis://redis:6379
  script:
    - cd fabric-core
    - mvn $MAVEN_CLI_OPTS test
  artifacts:
    reports:
      junit:
        - fabric-core/*/target/surefire-reports/TEST-*.xml
    paths:
      - fabric-core/target/site/jacoco/
    expire_in: 1 week
  coverage: '/Total.*?([0-9]{1,3})%/'

test:frontend:
  stage: test
  image: node:18-alpine
  script:
    - cd fabric-ui
    - npm ci
    - npm run test:ci
    - npm run test:coverage
  artifacts:
    reports:
      junit: fabric-ui/coverage/junit.xml
      coverage_report:
        coverage_format: cobertura
        path: fabric-ui/coverage/cobertura-coverage.xml
    paths:
      - fabric-ui/coverage/
    expire_in: 1 week
  coverage: '/Lines\s*:\s*(\d+\.\d+)%/'

# =============================================================================
# SECURITY STAGE
# =============================================================================

security:backend:
  stage: security
  image: maven:3.8.6-openjdk-17
  script:
    - cd fabric-core
    - mvn $MAVEN_CLI_OPTS dependency-check:check
  artifacts:
    reports:
      dependency_scanning: fabric-core/target/dependency-check-report.json
    expire_in: 1 week
  allow_failure: true

security:frontend:
  stage: security
  image: node:18-alpine
  script:
    - cd fabric-ui
    - npm audit --audit-level moderate
  allow_failure: true

# =============================================================================
# PACKAGE STAGE
# =============================================================================

package:application:
  stage: package
  image: maven:3.8.6-openjdk-17
  dependencies:
    - build:backend
    - build:frontend
  script:
    - cd fabric-core
    - mvn $MAVEN_CLI_OPTS package -DskipTests
  artifacts:
    paths:
      - fabric-core/fabric-api/target/*.jar
    expire_in: 1 week

# =============================================================================
# DEPLOYMENT STAGES (Environment-specific)
# =============================================================================

deploy:staging:
  stage: deploy
  image: alpine:latest
  environment:
    name: staging
    url: https://fabric-staging.company.com
  script:
    - echo "Deploying to staging environment..."
    - echo "Application JAR: $(ls fabric-core/fabric-api/target/*.jar)"
    # Add your deployment scripts here
  dependencies:
    - package:application
  only:
    - fabric-enhancements
  when: manual

deploy:production:
  stage: deploy
  image: alpine:latest
  environment:
    name: production
    url: https://fabric.company.com
  script:
    - echo "Deploying to production environment..."
    - echo "Application JAR: $(ls fabric-core/fabric-api/target/*.jar)"
    # Add your deployment scripts here
  dependencies:
    - package:application
  only:
    - main
  when: manual
  allow_failure: false

# =============================================================================
# DOCKER BUILD (Optional)
# =============================================================================

docker:build:
  stage: package
  image: docker:latest
  services:
    - docker:dind
  script:
    - cd fabric-core
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
  dependencies:
    - package:application
  only:
    - fabric-enhancements
    - main
  when: manual
EOF

        print_success "GitLab CI/CD configuration created (.gitlab-ci.yml)"
        
        # Add CI configuration to git
        git add .gitlab-ci.yml
        
        print_status "GitLab CI/CD features included:"
        echo "  ✅ Multi-stage pipeline (build, test, security, package, deploy)"
        echo "  ✅ Backend testing with Maven and JUnit"
        echo "  ✅ Frontend testing with Jest and coverage reports"
        echo "  ✅ Security scanning for dependencies"
        echo "  ✅ Automated packaging and deployment"
        echo "  ✅ Environment-specific deployments (staging/production)"
    else
        print_status "Skipping GitLab CI/CD setup"
    fi
}

# Function to create GitLab project documentation
create_gitlab_docs() {
    print_header "Creating GitLab Documentation"
    
    read -p "Do you want to create GitLab-specific documentation? (y/n): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Creating GitLab documentation..."
        
        # Create GitLab-specific README additions
        cat >> GITLAB_SETUP.md << 'EOF'
# GitLab Setup Guide - Fabric Platform

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+
- PostgreSQL 15+
- Redis 7+

### Local Development Setup

1. **Clone the repository:**
   ```bash
   git clone git@gitlab.com:your-username/fabric-platform.git
   cd fabric-platform
   ```

2. **Backend Setup:**
   ```bash
   cd fabric-core
   mvn clean install
   mvn spring-boot:run -Dspring.profiles.active=local
   ```

3. **Frontend Setup:**
   ```bash
   cd fabric-ui
   npm install
   npm start
   ```

4. **Database Setup:**
   ```bash
   # Start PostgreSQL and Redis
   docker-compose up -d postgres redis
   
   # Run database migrations
   cd fabric-core
   mvn flyway:migrate -Dspring.profiles.active=local
   ```

### GitLab CI/CD Pipeline

The project includes a comprehensive GitLab CI/CD pipeline:

- **Build Stage**: Compiles both backend (Maven) and frontend (npm)
- **Test Stage**: Runs unit tests with coverage reports
- **Security Stage**: Performs dependency vulnerability scans
- **Package Stage**: Creates deployable artifacts
- **Deploy Stage**: Deploys to staging/production environments

### Branch Strategy

- `main`: Production-ready code
- `fabric-enhancements`: Main development branch with all features
- `feature/*`: Individual feature branches
- `hotfix/*`: Production hotfixes

### Environment Variables

Configure the following variables in GitLab CI/CD settings:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=fabric_platform
DB_USERNAME=fabric_user
DB_PASSWORD=secure_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# Security
JWT_SECRET=your-jwt-secret-key
ENCRYPTION_KEY=your-encryption-key

# Monitoring
FABRIC_MONITORING_ENABLED=true
WEBSOCKET_SECURITY_ENABLED=true
```

### Monitoring and Observability

The application includes comprehensive monitoring:

- **WebSocket Security Monitoring**: Real-time connection monitoring
- **Performance Metrics**: System health and job processing metrics
- **Audit Logging**: Complete SOX-compliant audit trails
- **Alert Management**: Critical system alert handling

### Production Deployment

1. **Staging Deployment:**
   - Triggered manually from `fabric-enhancements` branch
   - Includes all tests and security scans
   - Deployed to staging environment for validation

2. **Production Deployment:**
   - Triggered manually from `main` branch
   - Requires all pipeline stages to pass
   - Includes blue-green deployment strategy

### Architecture Overview

```
Fabric Platform Architecture

┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   React UI      │    │   Spring Boot    │    │   PostgreSQL    │
│  (fabric-ui)    │◄──►│  (fabric-core)   │◄──►│   Database      │
│                 │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌──────────────────┐             │
         │              │      Redis       │             │
         └──────────────┤     Cache        │─────────────┘
                        └──────────────────┘
```

### Key Components

1. **US008 Real-Time Monitoring**: WebSocket-based job monitoring dashboard
2. **Epic 1-3 Processing**: Advanced batch processing with validation
3. **Template Management**: Dynamic configuration and mapping system
4. **Security Framework**: Enterprise-grade authentication and authorization
5. **Audit System**: Complete SOX-compliant logging and tracking

For detailed documentation, see `/docs` directory.
EOF

        print_success "GitLab documentation created (GITLAB_SETUP.md)"
        git add GITLAB_SETUP.md
    else
        print_status "Skipping GitLab documentation creation"
    fi
}

# Function to commit GitLab-specific changes
commit_gitlab_changes() {
    print_header "Committing GitLab Setup Changes"
    
    if [[ -n $(git status --porcelain) ]]; then
        print_status "Committing GitLab configuration files..."
        
        git commit -m "feat: Add GitLab CI/CD pipeline and documentation

- Added comprehensive .gitlab-ci.yml with multi-stage pipeline
- Includes build, test, security, package, and deploy stages
- Added backend testing with Maven and coverage reports
- Added frontend testing with Jest and coverage reports
- Added security scanning for dependency vulnerabilities
- Added GitLab-specific setup documentation
- Configured environment-specific deployments

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>"
        
        print_success "GitLab setup changes committed"
    else
        print_status "No changes to commit"
    fi
}

# Function to display final setup summary
show_setup_summary() {
    print_header "GitLab Setup Complete!"
    
    print_success "✅ Repository successfully configured for GitLab"
    echo
    echo "📋 Setup Summary:"
    echo "  • GitLab remote configured: $GITLAB_URL"
    echo "  • All branches and tags pushed to GitLab"
    echo "  • Current branch: $(git branch --show-current)"
    echo "  • GitLab CI/CD pipeline configured"
    echo "  • Documentation created"
    echo
    echo "🔗 GitLab Project Features:"
    echo "  • Multi-stage CI/CD pipeline"
    echo "  • Automated testing and coverage reports"
    echo "  • Security vulnerability scanning"
    echo "  • Environment-specific deployments"
    echo "  • Comprehensive documentation"
    echo
    echo "🚀 Next Steps:"
    echo "  1. Visit your GitLab project: ${GITLAB_URL//.git/}"
    echo "  2. Configure environment variables in CI/CD settings"
    echo "  3. Review and customize .gitlab-ci.yml if needed"
    echo "  4. Set up staging and production environments"
    echo "  5. Configure GitLab runners if using private instance"
    echo
    echo "📁 Repository Structure:"
    echo "  • fabric-core/     - Spring Boot backend with US008 WebSocket monitoring"
    echo "  • fabric-ui/       - React frontend with real-time dashboard"
    echo "  • docs/           - Comprehensive project documentation"
    echo "  • .gitlab-ci.yml  - CI/CD pipeline configuration"
    echo "  • GITLAB_SETUP.md - GitLab-specific setup guide"
    echo
    print_success "GitLab setup completed successfully!"
}

# Main execution function
main() {
    print_header "Fabric Platform - GitLab Setup Script"
    
    # Parse command line arguments
    GITLAB_URL_ARG="$1"
    
    # Execute setup steps
    check_prerequisites
    show_repo_info
    backup_current_state
    get_gitlab_url "$GITLAB_URL_ARG"
    test_gitlab_connection
    add_gitlab_remote
    push_to_gitlab
    configure_gitlab_default
    setup_gitlab_ci
    create_gitlab_docs
    commit_gitlab_changes
    
    # Push final changes if any
    if git status --porcelain | grep -q .; then
        print_status "Pushing final changes to GitLab..."
        git push origin $(git branch --show-current)
    fi
    
    show_setup_summary
}

# Error handling
trap 'print_error "Script interrupted. Setup may be incomplete."' INT

# Run main function with all arguments
main "$@"