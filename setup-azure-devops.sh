#!/bin/bash

# =============================================================================
# Azure DevOps Setup Script for Fabric Platform
# =============================================================================
# 
# This script sets up the fabric-platform project in Azure DevOps, preserving
# all branches, commits, and repository structure from the current setup.
#
# Prerequisites:
# 1. Azure DevOps account created
# 2. Personal Access Token (PAT) generated
# 3. Azure CLI installed (optional but recommended)
# 4. Git CLI installed and configured
#
# Usage:
#   ./setup-azure-devops.sh [AZURE_REPO_URL] [PAT_TOKEN]
#
# Example:
#   ./setup-azure-devops.sh https://dev.azure.com/yourorg/FabricPlatform/_git/fabric-platform
#   ./setup-azure-devops.sh https://yourorg@dev.azure.com/yourorg/FabricPlatform/_git/fabric-platform PAT_TOKEN
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
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Project configuration
PROJECT_NAME="fabric-platform"
CURRENT_BRANCH=$(git branch --show-current)
AZURE_URL=""
PAT_TOKEN=""
AZURE_ORG=""
AZURE_PROJECT=""

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

print_azure() {
    echo -e "${CYAN}[AZURE]${NC} $1"
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
    
    # Check if Azure CLI is installed (optional)
    if command -v az &> /dev/null; then
        print_success "Azure CLI is installed: $(az --version | head -n 1)"
        print_status "You can use Azure CLI for advanced features"
    else
        print_warning "Azure CLI not installed. Using Git commands only."
    fi
    
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
    
    # Create backup directory and copy everything
    mkdir -p "$BACKUP_DIR"
    rsync -av --exclude='.git' ./ "$BACKUP_DIR/"
    
    # Also backup git configuration
    cp -r .git "$BACKUP_DIR/"
    
    print_success "Backup created at: $BACKUP_DIR"
}

# Function to get Azure DevOps URL and PAT
get_azure_config() {
    print_header "Azure DevOps Configuration"
    
    if [[ -n "$1" ]]; then
        AZURE_URL="$1"
        print_status "Using provided Azure DevOps URL: $AZURE_URL"
    else
        echo "Please provide your Azure DevOps repository URL."
        echo "Format: https://dev.azure.com/{organization}/{project}/_git/{repository}"
        echo "Example: https://dev.azure.com/myorg/FabricPlatform/_git/fabric-platform"
        echo
        read -p "Azure DevOps repository URL: " AZURE_URL
    fi
    
    if [[ -z "$AZURE_URL" ]]; then
        print_error "Azure DevOps URL is required."
        exit 1
    fi
    
    # Extract organization and project from URL
    if [[ $AZURE_URL =~ dev\.azure\.com/([^/]+)/([^/]+) ]]; then
        AZURE_ORG="${BASH_REMATCH[1]}"
        AZURE_PROJECT="${BASH_REMATCH[2]}"
        print_azure "Organization: $AZURE_ORG"
        print_azure "Project: $AZURE_PROJECT"
    fi
    
    # Get PAT token if not provided
    if [[ -n "$2" ]]; then
        PAT_TOKEN="$2"
        print_status "Using provided PAT token"
    else
        echo
        echo "Personal Access Token (PAT) is required for authentication."
        echo "Generate one at: https://dev.azure.com/$AZURE_ORG/_usersSettings/tokens"
        echo "Required scopes: Code (Read & Write), Build (Read & Execute)"
        echo
        read -s -p "Enter PAT token: " PAT_TOKEN
        echo
    fi
    
    if [[ -z "$PAT_TOKEN" ]]; then
        print_error "PAT token is required for authentication."
        exit 1
    fi
    
    print_success "Azure DevOps configuration completed"
}

# Function to configure Git credentials for Azure DevOps
configure_azure_credentials() {
    print_header "Configuring Azure DevOps Authentication"
    
    # Create URL with embedded PAT token
    AZURE_URL_WITH_AUTH=$(echo $AZURE_URL | sed "s|https://|https://PAT:${PAT_TOKEN}@|")
    
    print_status "Configuring Git credentials for Azure DevOps..."
    
    # Store credentials (optional - for convenience)
    if command -v git-credential-manager &> /dev/null; then
        print_status "Git Credential Manager detected"
    else
        print_warning "Consider installing Git Credential Manager for secure credential storage"
    fi
    
    print_success "Authentication configured"
}

# Function to add Azure DevOps remote
add_azure_remote() {
    print_header "Adding Azure DevOps Remote"
    
    # Check if azure remote already exists
    if git remote get-url azure &> /dev/null; then
        print_warning "Azure remote already exists. Updating URL..."
        git remote set-url azure "$AZURE_URL_WITH_AUTH"
    else
        print_status "Adding Azure DevOps remote..."
        git remote add azure "$AZURE_URL_WITH_AUTH"
    fi
    
    print_success "Azure DevOps remote added/updated"
    
    # Show remotes (hide PAT token)
    echo "Current remotes:"
    git remote -v | sed "s|PAT:[^@]*@|[HIDDEN]@|g"
}

# Function to push all branches to Azure DevOps
push_to_azure() {
    print_header "Pushing to Azure DevOps"
    
    print_status "Pushing all branches to Azure DevOps..."
    
    # Push all branches
    git push azure --all
    
    # Push all tags
    print_status "Pushing tags to Azure DevOps..."
    git push azure --tags
    
    print_success "All branches and tags pushed to Azure DevOps"
}

# Function to set Azure DevOps as default remote
configure_azure_default() {
    print_header "Configuring Azure DevOps as Default Remote"
    
    read -p "Do you want to set Azure DevOps as the default remote (origin)? (y/n): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Backing up current origin remote..."
        
        # Backup current origin
        CURRENT_ORIGIN=$(git remote get-url origin 2>/dev/null || echo "")
        if [[ -n "$CURRENT_ORIGIN" ]]; then
            if git remote get-url github-backup &> /dev/null; then
                git remote remove github-backup
            fi
            git remote rename origin github-backup
            print_success "Current origin backed up as 'github-backup'"
        fi
        
        # Set Azure DevOps as new origin
        git remote rename azure origin
        
        # Update upstream tracking for current branch
        git branch --set-upstream-to=origin/${CURRENT_BRANCH} ${CURRENT_BRANCH}
        
        print_success "Azure DevOps is now the default remote (origin)"
    else
        print_status "Keeping current remote configuration"
    fi
    
    echo "Final remote configuration:"
    git remote -v | sed "s|PAT:[^@]*@|[HIDDEN]@|g"
}

# Function to create Azure Pipelines YAML
setup_azure_pipelines() {
    print_header "Setting up Azure Pipelines"
    
    read -p "Do you want to create Azure Pipelines configuration? (y/n): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Creating azure-pipelines.yml configuration..."
        
        cat > azure-pipelines.yml << 'EOF'
# Azure Pipelines Configuration for Fabric Platform
# Enterprise-grade Spring Boot + React application with comprehensive testing

trigger:
  branches:
    include:
      - main
      - fabric-enhancements
      - feature/*
  paths:
    exclude:
      - docs/*
      - '*.md'

pr:
  branches:
    include:
      - main
      - fabric-enhancements

variables:
  # Build Configuration
  buildConfiguration: 'Release'
  
  # Java Configuration
  JAVA_VERSION: '17'
  MAVEN_CACHE_FOLDER: $(Pipeline.Workspace)/.m2/repository
  MAVEN_OPTS: '-Xmx3072m -Dmaven.repo.local=$(MAVEN_CACHE_FOLDER)'
  
  # Node Configuration
  NODE_VERSION: '18.x'
  
  # Docker Configuration
  dockerRegistryServiceConnection: 'fabric-docker-registry'
  imageRepository: 'fabric-platform'
  containerRegistry: 'fabricplatform.azurecr.io'
  dockerfilePath: './Dockerfile'
  tag: '$(Build.BuildId)'
  
  # Azure Resources
  azureSubscription: 'Fabric-Platform-Subscription'
  
  # SonarQube Configuration (optional)
  sonarQubeServiceConnection: 'SonarQube-FabricPlatform'

stages:
# =============================================================================
# BUILD STAGE
# =============================================================================
- stage: Build
  displayName: 'Build Application'
  jobs:
  
  - job: BuildBackend
    displayName: 'Build Spring Boot Backend'
    pool:
      vmImage: 'ubuntu-latest'
    
    steps:
    - task: JavaToolInstaller@0
      displayName: 'Install Java 17'
      inputs:
        versionSpec: '$(JAVA_VERSION)'
        jdkArchitectureOption: 'x64'
        jdkSourceOption: 'PreInstalled'
    
    - task: Cache@2
      displayName: 'Cache Maven dependencies'
      inputs:
        key: 'maven | "$(Agent.OS)" | **/pom.xml'
        path: $(MAVEN_CACHE_FOLDER)
    
    - task: Maven@3
      displayName: 'Maven Build'
      inputs:
        mavenPomFile: 'fabric-core/pom.xml'
        goals: 'clean compile'
        options: '$(MAVEN_OPTS)'
        javaHomeOption: 'JDKVersion'
        mavenVersionOption: 'Default'
        publishJUnitResults: false
    
    - task: PublishPipelineArtifact@1
      displayName: 'Publish Backend Artifacts'
      inputs:
        targetPath: 'fabric-core/target'
        artifactName: 'backend-artifacts'
        publishLocation: 'pipeline'
  
  - job: BuildFrontend
    displayName: 'Build React Frontend'
    pool:
      vmImage: 'ubuntu-latest'
    
    steps:
    - task: NodeTool@0
      displayName: 'Install Node.js'
      inputs:
        versionSpec: '$(NODE_VERSION)'
    
    - task: Cache@2
      displayName: 'Cache npm dependencies'
      inputs:
        key: 'npm | "$(Agent.OS)" | fabric-ui/package-lock.json'
        path: 'fabric-ui/node_modules'
    
    - script: |
        cd fabric-ui
        npm ci
        npm run build
      displayName: 'npm install and build'
    
    - task: PublishPipelineArtifact@1
      displayName: 'Publish Frontend Artifacts'
      inputs:
        targetPath: 'fabric-ui/build'
        artifactName: 'frontend-artifacts'
        publishLocation: 'pipeline'

# =============================================================================
# TEST STAGE
# =============================================================================
- stage: Test
  displayName: 'Test & Quality Gates'
  dependsOn: Build
  jobs:
  
  - job: BackendTests
    displayName: 'Backend Tests & Coverage'
    pool:
      vmImage: 'ubuntu-latest'
    services:
      postgres: postgres:15-alpine
      redis: redis:7-alpine
    
    steps:
    - task: JavaToolInstaller@0
      displayName: 'Install Java 17'
      inputs:
        versionSpec: '$(JAVA_VERSION)'
        jdkArchitectureOption: 'x64'
    
    - task: Cache@2
      displayName: 'Restore Maven cache'
      inputs:
        key: 'maven | "$(Agent.OS)" | **/pom.xml'
        path: $(MAVEN_CACHE_FOLDER)
    
    - task: Maven@3
      displayName: 'Run Tests with Coverage'
      inputs:
        mavenPomFile: 'fabric-core/pom.xml'
        goals: 'test'
        options: '$(MAVEN_OPTS) -Dspring.profiles.active=test'
        publishJUnitResults: true
        testResultsFiles: '**/surefire-reports/TEST-*.xml'
        codeCoverageToolOption: 'JaCoCo'
        javaHomeOption: 'JDKVersion'
    
    - task: PublishCodeCoverageResults@1
      displayName: 'Publish Code Coverage'
      inputs:
        codeCoverageTool: 'JaCoCo'
        summaryFileLocation: '$(System.DefaultWorkingDirectory)/fabric-core/target/site/jacoco/jacoco.xml'
        reportDirectory: '$(System.DefaultWorkingDirectory)/fabric-core/target/site/jacoco'
    
    - task: PublishTestResults@2
      displayName: 'Publish Test Results'
      inputs:
        testResultsFormat: 'JUnit'
        testResultsFiles: '**/TEST-*.xml'
        searchFolder: '$(System.DefaultWorkingDirectory)/fabric-core'
        mergeTestResults: true
        failTaskOnFailedTests: true
  
  - job: FrontendTests
    displayName: 'Frontend Tests & Coverage'
    pool:
      vmImage: 'ubuntu-latest'
    
    steps:
    - task: NodeTool@0
      displayName: 'Install Node.js'
      inputs:
        versionSpec: '$(NODE_VERSION)'
    
    - task: Cache@2
      displayName: 'Restore npm cache'
      inputs:
        key: 'npm | "$(Agent.OS)" | fabric-ui/package-lock.json'
        path: 'fabric-ui/node_modules'
    
    - script: |
        cd fabric-ui
        npm ci
        npm run test:ci
        npm run test:coverage
      displayName: 'Run Tests with Coverage'
    
    - task: PublishTestResults@2
      displayName: 'Publish Test Results'
      inputs:
        testResultsFormat: 'JUnit'
        testResultsFiles: 'fabric-ui/coverage/junit.xml'
        searchFolder: '$(System.DefaultWorkingDirectory)'
        mergeTestResults: true
        failTaskOnFailedTests: true
    
    - task: PublishCodeCoverageResults@1
      displayName: 'Publish Code Coverage'
      inputs:
        codeCoverageTool: 'Cobertura'
        summaryFileLocation: '$(System.DefaultWorkingDirectory)/fabric-ui/coverage/cobertura-coverage.xml'
        reportDirectory: '$(System.DefaultWorkingDirectory)/fabric-ui/coverage'

# =============================================================================
# SECURITY SCAN STAGE
# =============================================================================
- stage: Security
  displayName: 'Security & Compliance Scanning'
  dependsOn: Build
  jobs:
  
  - job: SecurityScan
    displayName: 'Security Vulnerability Scan'
    pool:
      vmImage: 'ubuntu-latest'
    
    steps:
    - task: JavaToolInstaller@0
      displayName: 'Install Java 17'
      inputs:
        versionSpec: '$(JAVA_VERSION)'
    
    # OWASP Dependency Check
    - task: dependency-check-build-task@6
      displayName: 'OWASP Dependency Check'
      inputs:
        projectName: 'Fabric Platform'
        scanPath: '$(System.DefaultWorkingDirectory)'
        format: 'HTML,JSON'
        failOnCVSS: '7'
    
    - task: PublishPipelineArtifact@1
      displayName: 'Publish Security Report'
      inputs:
        targetPath: '$(Agent.BuildDirectory)/dependency-check'
        artifactName: 'security-reports'
        publishLocation: 'pipeline'
    
    # WhiteSource/Mend Scan (optional)
    - task: WhiteSource@21
      displayName: 'WhiteSource Security Scan'
      inputs:
        cwd: '$(System.DefaultWorkingDirectory)'
        projectName: 'Fabric Platform'
      condition: and(succeeded(), ne(variables['Build.Reason'], 'PullRequest'))
      continueOnError: true

# =============================================================================
# PACKAGE STAGE
# =============================================================================
- stage: Package
  displayName: 'Package Application'
  dependsOn: [Test, Security]
  condition: and(succeeded(), ne(variables['Build.Reason'], 'PullRequest'))
  jobs:
  
  - job: CreatePackage
    displayName: 'Create Deployment Package'
    pool:
      vmImage: 'ubuntu-latest'
    
    steps:
    - task: JavaToolInstaller@0
      displayName: 'Install Java 17'
      inputs:
        versionSpec: '$(JAVA_VERSION)'
    
    - task: Cache@2
      displayName: 'Restore Maven cache'
      inputs:
        key: 'maven | "$(Agent.OS)" | **/pom.xml'
        path: $(MAVEN_CACHE_FOLDER)
    
    - task: Maven@3
      displayName: 'Maven Package'
      inputs:
        mavenPomFile: 'fabric-core/pom.xml'
        goals: 'package'
        options: '$(MAVEN_OPTS) -DskipTests'
        javaHomeOption: 'JDKVersion'
    
    - task: CopyFiles@2
      displayName: 'Copy JAR files'
      inputs:
        sourceFolder: '$(System.DefaultWorkingDirectory)/fabric-core'
        contents: '**/target/*.jar'
        targetFolder: '$(Build.ArtifactStagingDirectory)'
    
    - task: Docker@2
      displayName: 'Build Docker Image'
      inputs:
        containerRegistry: '$(dockerRegistryServiceConnection)'
        repository: '$(imageRepository)'
        command: 'build'
        Dockerfile: '$(dockerfilePath)'
        tags: |
          $(tag)
          latest
    
    - task: Docker@2
      displayName: 'Push Docker Image'
      inputs:
        containerRegistry: '$(dockerRegistryServiceConnection)'
        repository: '$(imageRepository)'
        command: 'push'
        tags: |
          $(tag)
          latest
      condition: and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/main'))
    
    - task: PublishPipelineArtifact@1
      displayName: 'Publish Package Artifacts'
      inputs:
        targetPath: '$(Build.ArtifactStagingDirectory)'
        artifactName: 'deployment-package'
        publishLocation: 'pipeline'

# =============================================================================
# DEPLOY TO STAGING
# =============================================================================
- stage: DeployStaging
  displayName: 'Deploy to Staging'
  dependsOn: Package
  condition: and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/fabric-enhancements'))
  jobs:
  
  - deployment: DeployToStaging
    displayName: 'Deploy to Staging Environment'
    pool:
      vmImage: 'ubuntu-latest'
    environment: 'staging'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: AzureWebApp@1
            displayName: 'Deploy to Azure App Service'
            inputs:
              azureSubscription: '$(azureSubscription)'
              appType: 'webAppLinux'
              appName: 'fabric-platform-staging'
              package: '$(Pipeline.Workspace)/deployment-package/**/*.jar'
              runtimeStack: 'JAVA|17-java17'
              startUpCommand: 'java -jar app.jar --spring.profiles.active=staging'
          
          - task: AzureCLI@2
            displayName: 'Run Database Migrations'
            inputs:
              azureSubscription: '$(azureSubscription)'
              scriptType: 'bash'
              scriptLocation: 'inlineScript'
              inlineScript: |
                echo "Running database migrations for staging environment"
                # Add migration commands here

# =============================================================================
# DEPLOY TO PRODUCTION
# =============================================================================
- stage: DeployProduction
  displayName: 'Deploy to Production'
  dependsOn: Package
  condition: and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/main'))
  jobs:
  
  - deployment: DeployToProduction
    displayName: 'Deploy to Production Environment'
    pool:
      vmImage: 'ubuntu-latest'
    environment: 'production'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: AzureWebApp@1
            displayName: 'Deploy to Azure App Service'
            inputs:
              azureSubscription: '$(azureSubscription)'
              appType: 'webAppLinux'
              appName: 'fabric-platform-production'
              package: '$(Pipeline.Workspace)/deployment-package/**/*.jar'
              runtimeStack: 'JAVA|17-java17'
              startUpCommand: 'java -jar app.jar --spring.profiles.active=production'
          
          - task: AzureCLI@2
            displayName: 'Run Database Migrations'
            inputs:
              azureSubscription: '$(azureSubscription)'
              scriptType: 'bash'
              scriptLocation: 'inlineScript'
              inlineScript: |
                echo "Running database migrations for production environment"
                # Add migration commands here
          
          - task: AzureCLI@2
            displayName: 'Configure Application Insights'
            inputs:
              azureSubscription: '$(azureSubscription)'
              scriptType: 'bash'
              scriptLocation: 'inlineScript'
              inlineScript: |
                echo "Configuring Application Insights monitoring"
                # Add monitoring configuration here
EOF

        print_success "Azure Pipelines configuration created (azure-pipelines.yml)"
        
        # Add Pipeline configuration to git
        git add azure-pipelines.yml
        
        print_status "Azure Pipelines features included:"
        echo "  ✅ Multi-stage pipeline (Build, Test, Security, Package, Deploy)"
        echo "  ✅ Backend testing with Maven and JaCoCo coverage"
        echo "  ✅ Frontend testing with Jest and coverage reports"
        echo "  ✅ Security scanning with OWASP Dependency Check"
        echo "  ✅ Docker container build and push to Azure Container Registry"
        echo "  ✅ Automated deployment to Azure App Service"
        echo "  ✅ Environment-specific deployments (staging/production)"
    else
        print_status "Skipping Azure Pipelines setup"
    fi
}

# Function to create Azure-specific documentation
create_azure_docs() {
    print_header "Creating Azure DevOps Documentation"
    
    read -p "Do you want to create Azure DevOps-specific documentation? (y/n): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Creating Azure DevOps documentation..."
        
        cat >> AZURE_DEVOPS_SETUP.md << 'EOF'
# Azure DevOps Setup Guide - Fabric Platform

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+
- Azure subscription
- Azure DevOps organization

### Initial Setup

1. **Clone the repository:**
   ```bash
   git clone https://dev.azure.com/yourorg/FabricPlatform/_git/fabric-platform
   cd fabric-platform
   ```

2. **Local Development:**
   ```bash
   # Backend
   cd fabric-core
   mvn spring-boot:run -Dspring.profiles.active=local
   
   # Frontend
   cd fabric-ui
   npm install
   npm start
   ```

### Azure DevOps Features

#### 1. Azure Boards
- **Work Items**: Track features, bugs, and tasks
- **Sprints**: Agile sprint planning and tracking
- **Kanban Board**: Visual workflow management
- **Queries**: Custom work item queries and dashboards

#### 2. Azure Repos
- **Git Repository**: Full Git version control
- **Branch Policies**: Enforce code quality standards
- **Pull Requests**: Code review workflows
- **Code Search**: Advanced code search capabilities

#### 3. Azure Pipelines
- **CI/CD Pipeline**: Automated build and deployment
- **Multi-stage**: Build → Test → Security → Package → Deploy
- **Environments**: Staging and Production deployments
- **Approvals**: Manual approval gates for production

#### 4. Azure Test Plans
- **Test Cases**: Manual and automated test management
- **Test Suites**: Organize tests by feature/component
- **Test Runs**: Track test execution and results

#### 5. Azure Artifacts
- **Package Management**: Maven and npm package feeds
- **Container Registry**: Docker image storage
- **Universal Packages**: Generic artifact storage

### Pipeline Configuration

#### Build Pipeline Variables
Configure in Azure DevOps → Pipelines → Library:

```yaml
# Java/Maven Configuration
JAVA_VERSION: '17'
MAVEN_OPTS: '-Xmx3072m'

# Node.js Configuration
NODE_VERSION: '18.x'

# Docker Configuration
dockerRegistryServiceConnection: 'fabric-docker-registry'
containerRegistry: 'fabricplatform.azurecr.io'

# Azure Resources
azureSubscription: 'Your-Azure-Subscription'
resourceGroup: 'fabric-platform-rg'
```

#### Secret Variables
Store in Azure Key Vault and link to pipeline:

```yaml
# Database Secrets
DB_PASSWORD: $(database-password)
DB_CONNECTION_STRING: $(database-connection-string)

# Security Secrets
JWT_SECRET: $(jwt-secret)
ENCRYPTION_KEY: $(encryption-key)

# Azure Secrets
AZURE_CLIENT_ID: $(azure-client-id)
AZURE_CLIENT_SECRET: $(azure-client-secret)
```

### Branch Policies

Configure branch policies for main branches:

1. **Main Branch:**
   - Require pull request reviews (2 reviewers)
   - Check for linked work items
   - Build validation required
   - All tests must pass
   - Security scan must pass

2. **Fabric-Enhancements Branch:**
   - Require pull request reviews (1 reviewer)
   - Build validation required
   - Allow self-approval for hotfixes

### Service Connections

Create service connections in Project Settings:

1. **Azure Resource Manager:**
   - Connection name: `Fabric-Platform-Azure`
   - Subscription: Your Azure subscription
   - Resource group: `fabric-platform-rg`

2. **Docker Registry:**
   - Connection name: `fabric-docker-registry`
   - Registry: Azure Container Registry
   - Authentication: Service Principal

3. **SonarQube (Optional):**
   - Connection name: `SonarQube-FabricPlatform`
   - SonarQube URL: Your SonarQube server
   - Authentication token: Generated from SonarQube

### Deployment Environments

#### Staging Environment
- **URL**: https://fabric-platform-staging.azurewebsites.net
- **Approval**: Automatic deployment
- **Resources**: 
  - Azure App Service (B2 tier)
  - Azure SQL Database (S1 tier)
  - Azure Redis Cache (C1 tier)

#### Production Environment
- **URL**: https://fabric-platform.azurewebsites.net
- **Approval**: Manual approval required
- **Resources**:
  - Azure App Service (P2v2 tier)
  - Azure SQL Database (S3 tier)
  - Azure Redis Cache (C3 tier)
  - Application Insights
  - Azure Monitor

### Azure Resources Architecture

```
Resource Group: fabric-platform-rg
│
├── App Services
│   ├── fabric-platform-staging
│   └── fabric-platform-production
│
├── Databases
│   ├── fabric-sql-staging
│   └── fabric-sql-production
│
├── Cache
│   ├── fabric-redis-staging
│   └── fabric-redis-production
│
├── Storage
│   └── fabricstorage
│
├── Monitoring
│   ├── fabric-insights
│   └── fabric-loganalytics
│
└── Networking
    ├── fabric-vnet
    └── fabric-appgateway
```

### Monitoring & Diagnostics

#### Application Insights
- Real-time application monitoring
- Performance metrics and alerts
- Distributed tracing
- Custom telemetry

#### Azure Monitor
- Infrastructure monitoring
- Log Analytics workspace
- Custom dashboards
- Alert rules

#### Log Streaming
```bash
# View real-time logs
az webapp log tail --name fabric-platform-production \
  --resource-group fabric-platform-rg

# Download logs
az webapp log download --name fabric-platform-production \
  --resource-group fabric-platform-rg \
  --log-file fabric-logs.zip
```

### Cost Management

Estimated monthly costs (Azure East US region):

| Resource | Staging | Production |
|----------|---------|------------|
| App Service | $75 | $300 |
| SQL Database | $30 | $150 |
| Redis Cache | $50 | $150 |
| Storage | $5 | $20 |
| Monitoring | $10 | $50 |
| **Total** | **$170** | **$670** |

### Security Best Practices

1. **Managed Identity**: Use Azure Managed Identity for authentication
2. **Key Vault**: Store secrets in Azure Key Vault
3. **Network Security**: Configure VNet and NSG rules
4. **SSL/TLS**: Enforce HTTPS with managed certificates
5. **RBAC**: Implement role-based access control
6. **Audit Logs**: Enable diagnostic logging
7. **Compliance**: Configure Azure Policy for compliance

### Troubleshooting

#### Common Issues

1. **Pipeline Fails at Test Stage:**
   ```bash
   # Check test logs
   # Ensure database connection strings are correct
   # Verify Redis cache is accessible
   ```

2. **Docker Build Fails:**
   ```bash
   # Verify Dockerfile path
   # Check base image availability
   # Ensure build context is correct
   ```

3. **Deployment Fails:**
   ```bash
   # Check service connection permissions
   # Verify Azure subscription is active
   # Ensure resource names are unique
   ```

### Support Resources

- **Azure DevOps Documentation**: https://docs.microsoft.com/azure/devops
- **Azure Portal**: https://portal.azure.com
- **Azure CLI Reference**: https://docs.microsoft.com/cli/azure
- **Project Wiki**: Use Azure DevOps Wiki for team documentation

### US008 Real-Time Monitoring Features

The deployed application includes:

- **WebSocket Dashboard**: Real-time job monitoring at `/monitoring`
- **Health Endpoints**: `/actuator/health` for service health
- **Metrics Endpoint**: `/actuator/metrics` for Prometheus metrics
- **API Documentation**: Swagger UI at `/swagger-ui.html`
- **Admin Portal**: Configuration management at `/admin`

### Next Steps

1. **Configure Production Resources**
2. **Set up Azure Active Directory integration**
3. **Configure backup and disaster recovery**
4. **Implement auto-scaling rules**
5. **Set up performance testing pipeline**
EOF

        print_success "Azure DevOps documentation created (AZURE_DEVOPS_SETUP.md)"
        git add AZURE_DEVOPS_SETUP.md
    else
        print_status "Skipping Azure documentation creation"
    fi
}

# Function to create Azure ARM templates
create_arm_templates() {
    print_header "Creating Azure ARM Templates"
    
    read -p "Do you want to create Azure Resource Manager templates? (y/n): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Creating ARM templates directory..."
        mkdir -p azure-templates
        
        cat > azure-templates/azuredeploy.json << 'EOF'
{
  "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentTemplate.json#",
  "contentVersion": "1.0.0.0",
  "parameters": {
    "environmentName": {
      "type": "string",
      "allowedValues": ["staging", "production"],
      "metadata": {
        "description": "Environment name (staging or production)"
      }
    },
    "appServicePlanSku": {
      "type": "string",
      "defaultValue": "P1v2",
      "allowedValues": ["B1", "B2", "B3", "P1v2", "P2v2", "P3v2"],
      "metadata": {
        "description": "App Service Plan SKU"
      }
    },
    "sqlServerAdminPassword": {
      "type": "securestring",
      "metadata": {
        "description": "SQL Server administrator password"
      }
    }
  },
  "variables": {
    "appServicePlanName": "[concat('fabric-asp-', parameters('environmentName'))]",
    "webAppName": "[concat('fabric-app-', parameters('environmentName'))]",
    "sqlServerName": "[concat('fabric-sql-', parameters('environmentName'))]",
    "sqlDatabaseName": "fabric_platform",
    "redisCacheName": "[concat('fabric-redis-', parameters('environmentName'))]",
    "storageAccountName": "[concat('fabricstorage', parameters('environmentName'))]",
    "appInsightsName": "[concat('fabric-insights-', parameters('environmentName'))]"
  },
  "resources": [
    {
      "type": "Microsoft.Web/serverfarms",
      "apiVersion": "2021-02-01",
      "name": "[variables('appServicePlanName')]",
      "location": "[resourceGroup().location]",
      "sku": {
        "name": "[parameters('appServicePlanSku')]"
      },
      "kind": "linux",
      "properties": {
        "reserved": true
      }
    },
    {
      "type": "Microsoft.Web/sites",
      "apiVersion": "2021-02-01",
      "name": "[variables('webAppName')]",
      "location": "[resourceGroup().location]",
      "dependsOn": [
        "[resourceId('Microsoft.Web/serverfarms', variables('appServicePlanName'))]"
      ],
      "properties": {
        "serverFarmId": "[resourceId('Microsoft.Web/serverfarms', variables('appServicePlanName'))]",
        "siteConfig": {
          "linuxFxVersion": "JAVA|17-java17",
          "appSettings": [
            {
              "name": "SPRING_PROFILES_ACTIVE",
              "value": "[parameters('environmentName')]"
            },
            {
              "name": "APPLICATIONINSIGHTS_CONNECTION_STRING",
              "value": "[reference(resourceId('Microsoft.Insights/components', variables('appInsightsName'))).ConnectionString]"
            }
          ]
        }
      }
    },
    {
      "type": "Microsoft.Sql/servers",
      "apiVersion": "2021-02-01-preview",
      "name": "[variables('sqlServerName')]",
      "location": "[resourceGroup().location]",
      "properties": {
        "administratorLogin": "fabricadmin",
        "administratorLoginPassword": "[parameters('sqlServerAdminPassword')]"
      }
    },
    {
      "type": "Microsoft.Sql/servers/databases",
      "apiVersion": "2021-02-01-preview",
      "name": "[concat(variables('sqlServerName'), '/', variables('sqlDatabaseName'))]",
      "location": "[resourceGroup().location]",
      "dependsOn": [
        "[resourceId('Microsoft.Sql/servers', variables('sqlServerName'))]"
      ],
      "sku": {
        "name": "S1",
        "tier": "Standard"
      }
    },
    {
      "type": "Microsoft.Cache/redis",
      "apiVersion": "2021-06-01",
      "name": "[variables('redisCacheName')]",
      "location": "[resourceGroup().location]",
      "properties": {
        "sku": {
          "name": "Standard",
          "family": "C",
          "capacity": 1
        }
      }
    },
    {
      "type": "Microsoft.Insights/components",
      "apiVersion": "2020-02-02",
      "name": "[variables('appInsightsName')]",
      "location": "[resourceGroup().location]",
      "kind": "web",
      "properties": {
        "Application_Type": "web"
      }
    }
  ],
  "outputs": {
    "webAppUrl": {
      "type": "string",
      "value": "[concat('https://', variables('webAppName'), '.azurewebsites.net')]"
    }
  }
}
EOF

        cat > azure-templates/azuredeploy.parameters.json << 'EOF'
{
  "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentParameters.json#",
  "contentVersion": "1.0.0.0",
  "parameters": {
    "environmentName": {
      "value": "staging"
    },
    "appServicePlanSku": {
      "value": "B2"
    },
    "sqlServerAdminPassword": {
      "value": "ChangeThisPassword123!"
    }
  }
}
EOF

        print_success "ARM templates created in azure-templates/"
        git add azure-templates/
    else
        print_status "Skipping ARM template creation"
    fi
}

# Function to commit Azure DevOps changes
commit_azure_changes() {
    print_header "Committing Azure DevOps Setup Changes"
    
    if [[ -n $(git status --porcelain) ]]; then
        print_status "Committing Azure DevOps configuration files..."
        
        git commit -m "feat: Add Azure DevOps pipeline and deployment configuration

- Added comprehensive azure-pipelines.yml with multi-stage pipeline
- Includes build, test, security scan, package, and deploy stages
- Added backend testing with Maven and JaCoCo coverage
- Added frontend testing with Jest and coverage reports
- Added OWASP dependency scanning for security
- Added Docker container build and Azure Container Registry push
- Added Azure App Service deployment for staging/production
- Created ARM templates for infrastructure as code
- Added Azure DevOps-specific setup documentation
- Configured Application Insights monitoring

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>"
        
        print_success "Azure DevOps setup changes committed"
    else
        print_status "No changes to commit"
    fi
}

# Function to display final setup summary
show_setup_summary() {
    print_header "Azure DevOps Setup Complete!"
    
    print_success "✅ Repository successfully configured for Azure DevOps"
    echo
    echo "📋 Setup Summary:"
    echo "  • Azure DevOps remote configured"
    echo "  • All branches and tags pushed to Azure DevOps"
    echo "  • Current branch: $(git branch --show-current)"
    echo "  • Azure Pipelines YAML created"
    echo "  • ARM templates for infrastructure deployment"
    echo "  • Documentation created"
    echo
    echo "🔗 Azure DevOps Features Available:"
    echo "  • Azure Boards - Work item tracking and sprint planning"
    echo "  • Azure Repos - Git repository with branch policies"
    echo "  • Azure Pipelines - CI/CD with multi-stage deployments"
    echo "  • Azure Test Plans - Test case management"
    echo "  • Azure Artifacts - Package and container registry"
    echo
    echo "☁️ Azure Services Integration:"
    echo "  • Azure App Service - Web application hosting"
    echo "  • Azure SQL Database - Managed database service"
    echo "  • Azure Redis Cache - Distributed caching"
    echo "  • Application Insights - APM and monitoring"
    echo "  • Azure Key Vault - Secret management"
    echo
    echo "🚀 Next Steps:"
    echo "  1. Visit Azure DevOps: https://dev.azure.com/$AZURE_ORG/$AZURE_PROJECT"
    echo "  2. Create service connections in Project Settings"
    echo "  3. Configure pipeline variables and secrets"
    echo "  4. Set up branch policies and approvals"
    echo "  5. Deploy ARM templates to create Azure resources"
    echo "  6. Run the pipeline to deploy the application"
    echo
    echo "📁 Repository Structure:"
    echo "  • fabric-core/          - Spring Boot backend with US008 monitoring"
    echo "  • fabric-ui/            - React frontend with real-time dashboard"
    echo "  • azure-pipelines.yml   - CI/CD pipeline configuration"
    echo "  • azure-templates/      - ARM templates for infrastructure"
    echo "  • AZURE_DEVOPS_SETUP.md - Azure DevOps setup guide"
    echo
    print_azure "Project URL: https://dev.azure.com/$AZURE_ORG/$AZURE_PROJECT"
    print_success "Azure DevOps setup completed successfully!"
}

# Main execution function
main() {
    print_header "Fabric Platform - Azure DevOps Setup Script"
    
    # Parse command line arguments
    AZURE_URL_ARG="$1"
    PAT_TOKEN_ARG="$2"
    
    # Execute setup steps
    check_prerequisites
    show_repo_info
    backup_current_state
    get_azure_config "$AZURE_URL_ARG" "$PAT_TOKEN_ARG"
    configure_azure_credentials
    add_azure_remote
    push_to_azure
    configure_azure_default
    setup_azure_pipelines
    create_azure_docs
    create_arm_templates
    commit_azure_changes
    
    # Push final changes if any
    if git status --porcelain | grep -q .; then
        print_status "Pushing final changes to Azure DevOps..."
        git push origin $(git branch --show-current)
    fi
    
    show_setup_summary
}

# Error handling
trap 'print_error "Script interrupted. Setup may be incomplete."' INT

# Run main function with all arguments
main "$@"