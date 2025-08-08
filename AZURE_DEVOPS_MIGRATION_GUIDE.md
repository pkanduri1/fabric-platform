# Azure DevOps Migration Guide - Fabric Platform

## Overview

This comprehensive guide provides step-by-step instructions for migrating the Fabric Platform project to Azure DevOps, leveraging Azure's enterprise-grade DevOps services and cloud infrastructure.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Migration](#quick-migration)
3. [Manual Migration](#manual-migration)
4. [Azure DevOps Services](#azure-devops-services)
5. [Pipeline Configuration](#pipeline-configuration)
6. [Azure Infrastructure](#azure-infrastructure)
7. [Security & Compliance](#security--compliance)
8. [Cost Optimization](#cost-optimization)
9. [Monitoring & Operations](#monitoring--operations)
10. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Accounts & Tools

- **Azure DevOps Organization**: Create at https://dev.azure.com
- **Azure Subscription**: Active Azure subscription
- **Personal Access Token (PAT)**: Generate with Code (Read & Write) permissions
- **Azure CLI**: Install from https://docs.microsoft.com/cli/azure/install-azure-cli
- **Git**: Version 2.25+ recommended

### Required Permissions

- Azure DevOps: Project Administrator
- Azure Subscription: Contributor role
- Resource Group: Owner or Contributor

## Quick Migration

### Step 1: Create Azure DevOps Project

1. Navigate to https://dev.azure.com/your-organization
2. Click "New Project"
3. Configure:
   - **Project Name**: FabricPlatform
   - **Visibility**: Private (recommended for enterprise)
   - **Version Control**: Git
   - **Work Item Process**: Agile or Scrum

### Step 2: Generate Personal Access Token

1. Click User Settings → Personal Access Tokens
2. Click "New Token"
3. Configure:
   - **Name**: fabric-platform-migration
   - **Expiration**: 90 days (or custom)
   - **Scopes**: 
     - Code: Read & Write
     - Build: Read & Execute
     - Release: Read & Write

### Step 3: Run Migration Script

```bash
# Navigate to project directory
cd fabric-platform

# Run automated setup
./setup-azure-devops.sh https://dev.azure.com/yourorg/FabricPlatform/_git/fabric-platform YOUR_PAT_TOKEN

# Follow the prompts for:
# - Setting Azure DevOps as default remote
# - Creating pipeline configuration
# - Generating ARM templates
```

## Manual Migration

### Option 1: Using Git Commands

```bash
# Add Azure DevOps remote
git remote add azure https://PAT_TOKEN@dev.azure.com/yourorg/FabricPlatform/_git/fabric-platform

# Push all branches
git push azure --all

# Push all tags
git push azure --tags

# Set as default (optional)
git remote rename origin github-backup
git remote rename azure origin
```

### Option 2: Using Azure CLI

```bash
# Login to Azure
az login

# Create Azure DevOps project
az devops project create --name FabricPlatform --org https://dev.azure.com/yourorg

# Configure Git repository
az repos create --name fabric-platform --project FabricPlatform

# Import from existing repository
az repos import create \
  --git-source-url https://github.com/pkanduri1/fabric-platform.git \
  --repository fabric-platform \
  --project FabricPlatform
```

## Azure DevOps Services

### Azure Boards - Work Item Management

#### Setup Work Items

1. **Epic**: Fabric Platform Implementation
   - **Feature 1**: US008 Real-Time Monitoring Dashboard
   - **Feature 2**: Epic 1-3 Batch Processing Engine
   - **Feature 3**: Template Configuration System

2. **User Stories**: Import from existing documentation
   ```bash
   # Use Azure CLI to create work items
   az boards work-item create \
     --title "US008: Real-Time Job Monitoring Dashboard" \
     --type "User Story" \
     --project FabricPlatform \
     --area "FabricPlatform\Backend" \
     --iteration "FabricPlatform\Sprint 1"
   ```

#### Configure Boards

1. **Columns**: New → Active → Resolved → Closed
2. **Swimlanes**: By Priority or Feature
3. **Card Fields**: Effort, Priority, Tags
4. **Card Styles**: Color by state or type

### Azure Repos - Source Control

#### Branch Policies

Configure for `main` and `fabric-enhancements`:

```json
{
  "requiredReviewers": 2,
  "checkForLinkedWorkItems": true,
  "buildValidation": {
    "enabled": true,
    "pipeline": "azure-pipelines.yml"
  },
  "mergeStrategies": ["squash", "rebase"],
  "autoComplete": false
}
```

#### Repository Settings

1. **Security**: Configure RBAC permissions
2. **Policies**: Enable commit message standards
3. **Hooks**: Add pre-commit validation
4. **Tags**: Protect release tags

### Azure Pipelines - CI/CD

#### Pipeline Creation

1. Navigate to Pipelines → New Pipeline
2. Select Azure Repos Git
3. Choose fabric-platform repository
4. Select "Existing Azure Pipelines YAML"
5. Path: `/azure-pipelines.yml`

#### Pipeline Variables

##### Variable Groups

Create in Pipelines → Library:

```yaml
# Development Variables
- group: fabric-dev-vars
  variables:
    environment: development
    logLevel: DEBUG
    
# Staging Variables  
- group: fabric-staging-vars
  variables:
    environment: staging
    logLevel: INFO
    
# Production Variables
- group: fabric-prod-vars
  variables:
    environment: production
    logLevel: WARN
```

##### Secret Management

Link Azure Key Vault:

```bash
# Create Key Vault
az keyvault create \
  --name fabric-platform-kv \
  --resource-group fabric-platform-rg \
  --location eastus

# Add secrets
az keyvault secret set \
  --vault-name fabric-platform-kv \
  --name "database-password" \
  --value "SecurePassword123!"

# Link to pipeline
az pipelines variable-group create \
  --name fabric-secrets \
  --variables foo=bar \
  --authorize true \
  --project FabricPlatform
```

### Azure Test Plans - Test Management

#### Test Configuration

1. **Test Suites**:
   - Unit Tests (900+ test cases)
   - Integration Tests
   - Security Tests
   - Performance Tests
   - US008 Dashboard Tests

2. **Test Cases**:
   ```yaml
   - Test Case: TC001
     Title: WebSocket Connection Security
     Priority: High
     Automated: Yes
     Component: US008 Monitoring
   ```

3. **Test Runs**: Automated via pipeline

### Azure Artifacts - Package Management

#### Feed Creation

```bash
# Create Maven feed
az artifacts feed create \
  --name fabric-maven \
  --project FabricPlatform

# Create npm feed  
az artifacts feed create \
  --name fabric-npm \
  --project FabricPlatform

# Configure permissions
az artifacts feed permission update \
  --feed fabric-maven \
  --role contributor \
  --user "Build Service"
```

## Pipeline Configuration

### Multi-Stage Pipeline Structure

```yaml
stages:
  - Build
    - BuildBackend (Maven)
    - BuildFrontend (npm)
  - Test
    - BackendTests (JUnit + JaCoCo)
    - FrontendTests (Jest + Coverage)
  - Security
    - DependencyCheck (OWASP)
    - CodeAnalysis (SonarQube)
  - Package
    - CreateJAR
    - BuildDocker
  - Deploy
    - DeployStaging
    - DeployProduction
```

### Service Connections

#### Azure Resource Manager

```bash
# Create service principal
az ad sp create-for-rbac \
  --name fabric-platform-sp \
  --role Contributor \
  --scopes /subscriptions/{subscription-id}/resourceGroups/fabric-platform-rg
```

#### Docker Registry

```bash
# Create Azure Container Registry
az acr create \
  --name fabricplatformacr \
  --resource-group fabric-platform-rg \
  --sku Standard

# Get credentials
az acr credential show --name fabricplatformacr
```

## Azure Infrastructure

### Resource Deployment

#### Using ARM Templates

```bash
# Deploy staging environment
az deployment group create \
  --resource-group fabric-platform-staging-rg \
  --template-file azure-templates/azuredeploy.json \
  --parameters @azure-templates/azuredeploy.parameters.json \
  --parameters environmentName=staging

# Deploy production environment
az deployment group create \
  --resource-group fabric-platform-prod-rg \
  --template-file azure-templates/azuredeploy.json \
  --parameters environmentName=production \
  --parameters appServicePlanSku=P2v2
```

#### Using Terraform (Alternative)

```hcl
# main.tf
terraform {
  backend "azurerm" {
    resource_group_name  = "fabric-platform-terraform"
    storage_account_name = "fabricplatformtfstate"
    container_name       = "tfstate"
    key                  = "terraform.tfstate"
  }
}

resource "azurerm_resource_group" "fabric" {
  name     = "fabric-platform-${var.environment}"
  location = var.location
}

resource "azurerm_app_service_plan" "fabric" {
  name                = "fabric-asp-${var.environment}"
  location            = azurerm_resource_group.fabric.location
  resource_group_name = azurerm_resource_group.fabric.name
  kind                = "Linux"
  reserved            = true
  
  sku {
    tier = var.environment == "production" ? "Premium" : "Standard"
    size = var.environment == "production" ? "P2v2" : "S1"
  }
}
```

### Architecture Components

```
┌─────────────────────────────────────────────────────────┐
│                   Azure Architecture                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────┐     ┌──────────────┐                 │
│  │ Traffic      │────▶│ Application  │                 │
│  │ Manager      │     │ Gateway      │                 │
│  └──────────────┘     └──────────────┘                 │
│           │                    │                        │
│           ▼                    ▼                        │
│  ┌──────────────────────────────────┐                  │
│  │     App Service (Spring Boot)     │                  │
│  │  ┌────────────┐  ┌─────────────┐ │                  │
│  │  │ fabric-api │  │ WebSocket   │ │                  │
│  │  │            │  │ Handler     │ │                  │
│  │  └────────────┘  └─────────────┘ │                  │
│  └──────────────────────────────────┘                  │
│           │                    │                        │
│           ▼                    ▼                        │
│  ┌──────────────┐     ┌──────────────┐                │
│  │ Azure SQL    │     │ Redis Cache  │                │
│  │ Database     │     │              │                │
│  └──────────────┘     └──────────────┘                │
│           │                    │                        │
│           ▼                    ▼                        │
│  ┌──────────────────────────────────┐                  │
│  │      Application Insights         │                  │
│  └──────────────────────────────────┘                  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## Security & Compliance

### Azure Security Center

```bash
# Enable Security Center
az security auto-provisioning-setting update \
  --name default \
  --auto-provision on

# Configure security policies
az security policy-set-definition create \
  --name FabricSecurityPolicy \
  --display-name "Fabric Platform Security Policy" \
  --description "Security policies for Fabric Platform"
```

### Managed Identity

```bash
# Enable managed identity for App Service
az webapp identity assign \
  --name fabric-app-production \
  --resource-group fabric-platform-prod-rg

# Grant Key Vault access
az keyvault set-policy \
  --name fabric-platform-kv \
  --object-id <identity-object-id> \
  --secret-permissions get list
```

### Network Security

```bash
# Create Network Security Group
az network nsg create \
  --name fabric-nsg \
  --resource-group fabric-platform-rg

# Add security rules
az network nsg rule create \
  --name AllowHTTPS \
  --nsg-name fabric-nsg \
  --priority 100 \
  --source-address-prefixes Internet \
  --destination-port-ranges 443 \
  --protocol Tcp \
  --access Allow
```

## Cost Optimization

### Cost Management

```bash
# Set up budget alerts
az consumption budget create \
  --budget-name fabric-platform-budget \
  --amount 1000 \
  --time-grain Monthly \
  --start-date 2025-01-01 \
  --end-date 2025-12-31 \
  --category Cost \
  --notifications-enabled true
```

### Auto-Scaling

```bash
# Configure auto-scaling
az monitor autoscale create \
  --resource-group fabric-platform-prod-rg \
  --resource fabric-app-production \
  --resource-type Microsoft.Web/sites \
  --name fabric-autoscale \
  --min-count 2 \
  --max-count 10 \
  --default-count 2

# Add scale rule
az monitor autoscale rule create \
  --resource-group fabric-platform-prod-rg \
  --autoscale-name fabric-autoscale \
  --condition "CpuPercentage > 70 avg 5m" \
  --scale out 1
```

## Monitoring & Operations

### Application Insights

```bash
# Query application logs
az monitor app-insights query \
  --app fabric-insights-production \
  --analytics-query "requests | where timestamp > ago(1h) | summarize count() by bin(timestamp, 5m)"

# Set up alerts
az monitor metrics alert create \
  --name high-response-time \
  --resource-group fabric-platform-prod-rg \
  --scopes /subscriptions/{sub}/resourceGroups/{rg}/providers/Microsoft.Web/sites/fabric-app-production \
  --condition "avg ResponseTime > 1000" \
  --window-size 5m \
  --evaluation-frequency 1m
```

### Log Analytics

```bash
# Create Log Analytics workspace
az monitor log-analytics workspace create \
  --workspace-name fabric-logs \
  --resource-group fabric-platform-rg

# Configure diagnostic settings
az monitor diagnostic-settings create \
  --name fabric-diagnostics \
  --resource /subscriptions/{sub}/resourceGroups/{rg}/providers/Microsoft.Web/sites/fabric-app-production \
  --workspace fabric-logs \
  --logs '[{"category": "AppServiceHTTPLogs", "enabled": true}]' \
  --metrics '[{"category": "AllMetrics", "enabled": true}]'
```

## Troubleshooting

### Common Issues & Solutions

#### 1. Authentication Failures

```bash
# Regenerate PAT token
# Update remote URL
git remote set-url azure https://NEW_PAT@dev.azure.com/org/project/_git/repo

# Clear credential cache
git config --global --unset credential.helper
```

#### 2. Pipeline Failures

```bash
# Check service connections
az devops service-endpoint list --project FabricPlatform

# Validate YAML
az pipelines run --name fabric-platform-ci --debug
```

#### 3. Deployment Issues

```bash
# Check deployment logs
az webapp log tail \
  --name fabric-app-production \
  --resource-group fabric-platform-prod-rg

# Restart application
az webapp restart \
  --name fabric-app-production \
  --resource-group fabric-platform-prod-rg
```

#### 4. Database Connection

```sql
-- Test database connectivity
SELECT name, state_desc FROM sys.databases;
SELECT @@VERSION;

-- Check firewall rules
EXECUTE sp_set_database_firewall_rule 
  @name = N'AllowAzureServices',
  @start_ip_address = '0.0.0.0',
  @end_ip_address = '0.0.0.0';
```

## Migration Verification Checklist

- [ ] All branches migrated to Azure Repos
- [ ] Pipeline runs successfully
- [ ] Work items imported to Azure Boards
- [ ] Service connections configured
- [ ] Key Vault secrets created
- [ ] ARM templates deployed
- [ ] Application deployed to staging
- [ ] Monitoring configured
- [ ] Security policies applied
- [ ] Cost alerts set up
- [ ] Documentation updated
- [ ] Team permissions configured
- [ ] US008 dashboard functional
- [ ] WebSocket connections working
- [ ] Database migrations complete

## Success Metrics

### DevOps Metrics
- Build Success Rate: > 95%
- Deployment Frequency: Daily to staging
- Lead Time: < 2 hours
- MTTR: < 30 minutes

### Application Metrics
- Availability: 99.9%
- Response Time: < 200ms (P95)
- Error Rate: < 0.1%
- WebSocket Latency: < 50ms

### Business Metrics
- Cost per Environment: Within budget
- Security Score: > 85/100
- Compliance: 100% SOX compliant
- Test Coverage: > 90%

## Next Steps

1. **Immediate Actions**:
   - Complete migration verification
   - Configure production environment
   - Set up monitoring dashboards
   - Train team on Azure DevOps

2. **Short-term Goals** (1-2 weeks):
   - Optimize pipeline performance
   - Implement advanced security scanning
   - Configure disaster recovery
   - Set up performance testing

3. **Long-term Goals** (1-3 months):
   - Achieve DevOps maturity level 4
   - Implement chaos engineering
   - Optimize cloud costs by 20%
   - Achieve 99.99% availability

## Support Resources

- **Azure DevOps Documentation**: https://docs.microsoft.com/azure/devops
- **Azure Portal**: https://portal.azure.com
- **Azure Status**: https://status.azure.com
- **Community Forums**: https://developercommunity.visualstudio.com/spaces/21/index.html
- **Support Tickets**: Create via Azure Portal

---

**Migration Complete!** 🎉

Your Fabric Platform is now fully integrated with Azure DevOps, leveraging enterprise-grade CI/CD, Azure cloud services, and comprehensive monitoring capabilities. The US008 Real-Time Job Monitoring Dashboard and all Epic implementations are ready for deployment.