# Fabric Platform - Enterprise Configuration & Deployment Guide

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Environment Strategy](#environment-strategy)
3. [Infrastructure Prerequisites](#infrastructure-prerequisites)
4. [GitLab CI/CD Configuration](#gitlab-cicd-configuration)
5. [ADS Deployment Setup](#ads-deployment-setup)
6. [Kubernetes Configuration](#kubernetes-configuration)
7. [Database Configuration](#database-configuration)
8. [Security Configuration](#security-configuration)
9. [Monitoring & Logging Setup](#monitoring--logging-setup)
10. [Environment-Specific Configurations](#environment-specific-configurations)
11. [Deployment Procedures](#deployment-procedures)
12. [Troubleshooting Guide](#troubleshooting-guide)
13. [Maintenance Procedures](#maintenance-procedures)

---

## Executive Summary

This document provides comprehensive configuration and deployment instructions for the Fabric Platform across enterprise environments. It covers Infrastructure as Code (IaC) setup, CI/CD pipeline configuration, and environment-specific deployment procedures aligned with enterprise security standards and banking compliance requirements.

### Deployment Methodology
- **Infrastructure as Code**: Terraform for infrastructure provisioning
- **GitOps Approach**: Git-based configuration management with automated deployments
- **Blue-Green Deployment**: Zero-downtime deployment strategy
- **Immutable Infrastructure**: Container-based deployments with version control

### Compliance Framework
- **SOX Compliance**: Automated audit trail and change management
- **Security Standards**: Zero-trust architecture with defense-in-depth
- **Operational Excellence**: Automated monitoring and alerting
- **Business Continuity**: Multi-region disaster recovery capabilities

---

## Environment Strategy

### Environment Topology

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Environment Hierarchy                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                          Development                                    │   │
│  │                                                                         │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐        │   │
│  │  │   Local Dev     │  │  Integration    │  │   Feature       │        │   │
│  │  │                 │  │                 │  │   Branches      │        │   │
│  │  │• Docker Compose │  │• Shared Env     │  │• Isolated Test  │        │   │
│  │  │• Local Database │  │• CI/CD Testing  │  │• PR Validation  │        │   │
│  │  │• Hot Reload     │  │• Integration    │  │• Automated      │        │   │
│  │  │• Debug Mode     │  │  Tests          │  │  Testing        │        │   │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘        │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                   │                                             │
│  ┌─────────────────────────────────▼─────────────────────────────────────┐     │
│  │                            Test                                       │     │
│  │                                                                       │     │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐       │     │
│  │  │   QA Testing    │  │   UAT Testing   │  │  Performance    │       │     │
│  │  │                 │  │                 │  │   Testing       │       │     │
│  │  │• Automated      │  │• Business User  │  │• Load Testing   │       │     │
│  │  │  Testing        │  │  Validation     │  │• Stress Testing │       │     │
│  │  │• Regression     │  │• End-to-End     │  │• Capacity       │       │     │
│  │  │  Suite          │  │  Scenarios      │  │  Planning       │       │     │
│  │  │• Oracle DB      │  │• Production     │  │• Performance    │       │     │
│  │  │• Full Features  │  │  Mirror         │  │  Benchmarks     │       │     │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘       │     │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                   │                                             │
│  ┌─────────────────────────────────▼─────────────────────────────────────┐     │
│  │                         Production                                    │     │
│  │                                                                       │     │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐       │     │
│  │  │   Staging       │  │   Production    │  │   Disaster      │       │     │
│  │  │                 │  │                 │  │   Recovery      │       │     │
│  │  │• Final          │  │• Live System    │  │• Backup Site    │       │     │
│  │  │  Validation     │  │• High Avail.    │  │• RTO < 4 hours  │       │     │
│  │  │• Prod Mirror    │  │• 24/7 Support   │  │• RPO < 15 min   │       │     │
│  │  │• Blue-Green     │  │• Full Security  │  │• Auto Failover  │       │     │
│  │  │• Smoke Tests    │  │• Monitoring     │  │• Data Sync      │       │     │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘       │     │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Environment Configuration Matrix

| Environment | CPU/Memory | Replicas | Database | Monitoring | Security Level |
|-------------|------------|----------|----------|------------|---------------|
| **Local** | 2 CPU / 4GB | 1 | Docker Oracle | Basic | Development |
| **Integration** | 4 CPU / 8GB | 2 | Shared Oracle | Standard | Enhanced |
| **QA** | 8 CPU / 16GB | 3 | Dedicated Oracle | Full | Production-like |
| **UAT** | 8 CPU / 16GB | 3 | Production Mirror | Full | Production |
| **Performance** | 16 CPU / 32GB | 5 | High-Performance | Advanced | Production |
| **Staging** | 16 CPU / 32GB | 3 | Production Mirror | Full | Production |
| **Production** | 32 CPU / 64GB | 5+ | Oracle RAC | Enterprise | Maximum |
| **DR** | 32 CPU / 64GB | 5+ | Oracle Data Guard | Enterprise | Maximum |

---

## Infrastructure Prerequisites

### Hardware Requirements

#### Minimum Production Infrastructure
```yaml
# Production Cluster Specifications
master_nodes:
  count: 3
  instance_type: "m5.xlarge"  # 4 vCPU, 16GB RAM
  storage: "100GB SSD"
  availability_zones: ["us-east-1a", "us-east-1b", "us-east-1c"]

worker_nodes:
  count: 6
  instance_type: "m5.2xlarge"  # 8 vCPU, 32GB RAM
  storage: "200GB SSD"
  auto_scaling:
    min: 6
    max: 20
    target_cpu: 70%

database:
  instance_type: "db.r5.2xlarge"  # 8 vCPU, 64GB RAM
  storage: "1TB SSD"
  backup_retention: 30
  multi_az: true
  read_replicas: 3

load_balancer:
  type: "Application Load Balancer"
  ssl_termination: true
  health_check_interval: 30
  health_check_timeout: 5
```

#### Network Requirements
```yaml
# Network Configuration
vpc_cidr: "10.0.0.0/16"

subnets:
  public:
    - "10.0.1.0/24"  # AZ-1
    - "10.0.2.0/24"  # AZ-2
    - "10.0.3.0/24"  # AZ-3
  
  private:
    - "10.0.10.0/24"  # AZ-1 - Application
    - "10.0.11.0/24"  # AZ-2 - Application
    - "10.0.12.0/24"  # AZ-3 - Application
  
  database:
    - "10.0.20.0/24"  # AZ-1 - Database
    - "10.0.21.0/24"  # AZ-2 - Database
    - "10.0.22.0/24"  # AZ-3 - Database

security_groups:
  alb:
    ingress:
      - port: 443
        protocol: tcp
        cidr: "0.0.0.0/0"
      - port: 80
        protocol: tcp
        cidr: "0.0.0.0/0"
  
  application:
    ingress:
      - port: 8080
        protocol: tcp
        source_sg: "${alb_sg}"
      - port: 3000
        protocol: tcp
        source_sg: "${alb_sg}"
  
  database:
    ingress:
      - port: 1521
        protocol: tcp
        source_sg: "${app_sg}"
```

### Software Prerequisites

#### Required Software Stack
```yaml
# Container Platform
kubernetes_version: "1.28.0"
docker_version: "24.0.5"
containerd_version: "1.7.3"

# Application Runtime
java_version: "17.0.8"
node_version: "18.17.0"
npm_version: "9.6.7"

# Database
oracle_version: "19c Enterprise"
oracle_client_version: "19.19.0"

# Monitoring Stack
prometheus_version: "2.45.0"
grafana_version: "10.0.3"
elasticsearch_version: "8.8.0"
kibana_version: "8.8.0"
logstash_version: "8.8.0"

# Security Tools
vault_version: "1.14.0"
cert_manager_version: "1.12.0"
istio_version: "1.18.0"
```

#### Third-Party Integrations
- **LDAP/Active Directory**: Enterprise authentication
- **Certificate Authority**: PKI infrastructure for SSL/TLS
- **DNS**: Internal and external domain resolution
- **NTP**: Time synchronization for audit trails
- **SMTP**: Email notifications for alerts

---

## GitLab CI/CD Configuration

### GitLab Project Structure

```
fabric-platform/
├── .gitlab-ci.yml                    # Main pipeline configuration
├── infrastructure/
│   ├── terraform/
│   │   ├── modules/
│   │   │   ├── vpc/
│   │   │   ├── eks/
│   │   │   ├── rds/
│   │   │   └── monitoring/
│   │   ├── environments/
│   │   │   ├── dev/
│   │   │   ├── test/
│   │   │   ├── staging/
│   │   │   └── prod/
│   │   └── scripts/
│   └── kubernetes/
│       ├── base/
│       └── overlays/
│           ├── dev/
│           ├── test/
│           ├── staging/
│           └── prod/
├── fabric-core/
│   └── fabric-api/
├── fabric-ui/
├── scripts/
│   ├── build.sh
│   ├── deploy.sh
│   ├── test.sh
│   └── rollback.sh
└── docs/
```

### Main Pipeline Configuration

```yaml
# .gitlab-ci.yml
stages:
  - validate
  - build
  - test
  - security-scan
  - package
  - deploy-dev
  - integration-test
  - deploy-test
  - deploy-staging
  - deploy-production
  - post-deployment

variables:
  DOCKER_REGISTRY: "registry.gitlab.com/fabric-platform"
  KUBE_NAMESPACE: "fabric-platform"
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  NODE_OPTIONS: "--max-old-space-size=4096"

# Cache configuration
cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .m2/repository/
    - node_modules/
    - target/

# Validate stage
validate-terraform:
  stage: validate
  image: hashicorp/terraform:1.5.0
  script:
    - cd infrastructure/terraform
    - terraform init
    - terraform validate
    - terraform plan -out=tfplan
  artifacts:
    paths:
      - infrastructure/terraform/tfplan
    expire_in: 1 hour
  only:
    changes:
      - infrastructure/terraform/**/*

validate-kubernetes:
  stage: validate
  image: bitnami/kubectl:latest
  script:
    - cd infrastructure/kubernetes
    - kubectl --dry-run=client apply -f base/
    - kubectl --dry-run=client apply -f overlays/dev/
  only:
    changes:
      - infrastructure/kubernetes/**/*

# Build stage
build-backend:
  stage: build
  image: maven:3.8-openjdk-17
  script:
    - cd fabric-core/fabric-api
    - mvn clean compile -DskipTests
    - mvn package -DskipTests
    - docker build -t $DOCKER_REGISTRY/fabric-api:$CI_COMMIT_SHA .
    - docker push $DOCKER_REGISTRY/fabric-api:$CI_COMMIT_SHA
  artifacts:
    paths:
      - fabric-core/fabric-api/target/*.jar
    expire_in: 1 hour

build-frontend:
  stage: build
  image: node:18-alpine
  script:
    - cd fabric-ui
    - npm ci
    - npm run build
    - docker build -t $DOCKER_REGISTRY/fabric-ui:$CI_COMMIT_SHA .
    - docker push $DOCKER_REGISTRY/fabric-ui:$CI_COMMIT_SHA
  artifacts:
    paths:
      - fabric-ui/build/
    expire_in: 1 hour

# Test stage
test-backend:
  stage: test
  image: maven:3.8-openjdk-17
  services:
    - name: oracle/database:19.3.0-ee
      alias: oracle-db
  script:
    - cd fabric-core/fabric-api
    - mvn test
    - mvn jacoco:report
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
    reports:
      junit:
        - fabric-core/fabric-api/target/surefire-reports/TEST-*.xml
      coverage_report:
        coverage_format: jacoco
        path: fabric-core/fabric-api/target/site/jacoco/jacoco.xml
    expire_in: 1 week

test-frontend:
  stage: test
  image: node:18-alpine
  script:
    - cd fabric-ui
    - npm ci
    - npm run test -- --coverage --watchAll=false
  coverage: '/All files[^|]*\|[^|]*\s+([\d\.]+)/'
  artifacts:
    reports:
      junit:
        - fabric-ui/junit.xml
      coverage_report:
        coverage_format: cobertura
        path: fabric-ui/coverage/cobertura-coverage.xml
    expire_in: 1 week

# Security scanning
security-scan-backend:
  stage: security-scan
  image: owasp/dependency-check:latest
  script:
    - cd fabric-core/fabric-api
    - dependency-check.sh --project "Fabric API" --scan . --format XML --out target/
  artifacts:
    reports:
      dependency_scanning: fabric-core/fabric-api/target/dependency-check-report.xml
    expire_in: 1 week

security-scan-frontend:
  stage: security-scan
  image: node:18-alpine
  script:
    - cd fabric-ui
    - npm audit --audit-level high
    - npx npm-audit-ci --audit-level high
  allow_failure: true

# Container scanning
container-scan:
  stage: security-scan
  image: aquasec/trivy:latest
  script:
    - trivy image --exit-code 0 --no-progress --format template --template "@contrib/gitlab.tpl" -o gl-container-scanning-report.json $DOCKER_REGISTRY/fabric-api:$CI_COMMIT_SHA
    - trivy image --exit-code 0 --no-progress --format template --template "@contrib/gitlab.tpl" -o gl-container-scanning-report-ui.json $DOCKER_REGISTRY/fabric-ui:$CI_COMMIT_SHA
  artifacts:
    reports:
      container_scanning: 
        - gl-container-scanning-report.json
        - gl-container-scanning-report-ui.json
    expire_in: 1 week

# Package stage
package-helm-charts:
  stage: package
  image: alpine/helm:3.12.0
  script:
    - cd infrastructure/kubernetes
    - helm package .
    - helm push fabric-platform-*.tgz oci://$DOCKER_REGISTRY/helm
  artifacts:
    paths:
      - infrastructure/kubernetes/fabric-platform-*.tgz
    expire_in: 1 week

# Development deployment
deploy-dev:
  stage: deploy-dev
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context $KUBE_CONTEXT_DEV
    - cd infrastructure/kubernetes/overlays/dev
    - kustomize edit set image fabric-api=$DOCKER_REGISTRY/fabric-api:$CI_COMMIT_SHA
    - kustomize edit set image fabric-ui=$DOCKER_REGISTRY/fabric-ui:$CI_COMMIT_SHA
    - kubectl apply -k .
    - kubectl rollout status deployment/fabric-api -n $KUBE_NAMESPACE-dev --timeout=600s
    - kubectl rollout status deployment/fabric-ui -n $KUBE_NAMESPACE-dev --timeout=600s
  environment:
    name: development
    url: https://dev.fabric-platform.truist.com
  only:
    - develop

# Integration tests
integration-test:
  stage: integration-test
  image: newman/newman:latest
  script:
    - newman run tests/postman/integration-tests.json --env-var baseUrl=https://dev.fabric-platform.truist.com
  dependencies:
    - deploy-dev
  only:
    - develop

# Staging deployment
deploy-staging:
  stage: deploy-staging
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context $KUBE_CONTEXT_STAGING
    - cd infrastructure/kubernetes/overlays/staging
    - kustomize edit set image fabric-api=$DOCKER_REGISTRY/fabric-api:$CI_COMMIT_SHA
    - kustomize edit set image fabric-ui=$DOCKER_REGISTRY/fabric-ui:$CI_COMMIT_SHA
    - kubectl apply -k .
    - kubectl rollout status deployment/fabric-api -n $KUBE_NAMESPACE-staging --timeout=600s
    - kubectl rollout status deployment/fabric-ui -n $KUBE_NAMESPACE-staging --timeout=600s
  environment:
    name: staging
    url: https://staging.fabric-platform.truist.com
  when: manual
  only:
    - main

# Production deployment
deploy-production:
  stage: deploy-production
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context $KUBE_CONTEXT_PROD
    - cd infrastructure/kubernetes/overlays/prod
    - kustomize edit set image fabric-api=$DOCKER_REGISTRY/fabric-api:$CI_COMMIT_SHA
    - kustomize edit set image fabric-ui=$DOCKER_REGISTRY/fabric-ui:$CI_COMMIT_SHA
    - kubectl apply -k .
    - kubectl rollout status deployment/fabric-api -n $KUBE_NAMESPACE-prod --timeout=600s
    - kubectl rollout status deployment/fabric-ui -n $KUBE_NAMESPACE-prod --timeout=600s
  environment:
    name: production
    url: https://fabric-platform.truist.com
  when: manual
  only:
    - main

# Post-deployment verification
health-check:
  stage: post-deployment
  image: curlimages/curl:latest
  script:
    - curl -f https://fabric-platform.truist.com/actuator/health
    - curl -f https://fabric-platform.truist.com/api/v2/manual-job-config/statistics
  only:
    - main
```

### GitLab CI/CD Variables Configuration

```yaml
# GitLab CI/CD Variables (Configured in GitLab UI)
# Group Level Variables
DOCKER_REGISTRY: "registry.gitlab.com/fabric-platform"
KUBE_CONFIG_BASE64: "<base64-encoded-kubeconfig>"
VAULT_TOKEN: "<vault-token-for-secrets>"

# Environment-Specific Variables
# Development
KUBE_CONTEXT_DEV: "dev-cluster"
DB_URL_DEV: "jdbc:oracle:thin:@dev-oracle:1521/ORCLPDB1"
DB_USERNAME_DEV: "cm3int_dev"

# Staging
KUBE_CONTEXT_STAGING: "staging-cluster"
DB_URL_STAGING: "jdbc:oracle:thin:@staging-oracle:1521/ORCLPDB1"
DB_USERNAME_STAGING: "cm3int_staging"

# Production
KUBE_CONTEXT_PROD: "prod-cluster"
DB_URL_PROD: "jdbc:oracle:thin:@prod-oracle:1521/ORCLPDB1"
DB_USERNAME_PROD: "cm3int_prod"

# Secrets (Masked)
DB_PASSWORD_DEV: "<dev-database-password>"
DB_PASSWORD_STAGING: "<staging-database-password>"
DB_PASSWORD_PROD: "<production-database-password>"
DOCKER_AUTH_CONFIG: "<docker-registry-auth>"
```

---

## ADS Deployment Setup

### Application Deployment Services (ADS) Configuration

#### ADS Project Structure
```yaml
# ads-config.yml
apiVersion: ads.truist.com/v1
kind: Application
metadata:
  name: fabric-platform
  namespace: fabric-platform-prod
spec:
  description: "Enterprise Batch Processing Platform"
  owner: "Architecture Team"
  technical_contact: "fabric-platform-team@truist.com"
  business_contact: "lending-product-owner@truist.com"
  
  compliance:
    sox_required: true
    pci_scope: false
    data_classification: "Confidential"
    retention_policy: "7-years"
  
  environments:
    - name: development
      cluster: "dev-eks-cluster"
      namespace: "fabric-platform-dev"
      resources:
        cpu_limit: "2000m"
        memory_limit: "4Gi"
        replicas: 2
    
    - name: staging
      cluster: "staging-eks-cluster"
      namespace: "fabric-platform-staging"
      resources:
        cpu_limit: "4000m"
        memory_limit: "8Gi"
        replicas: 3
    
    - name: production
      cluster: "prod-eks-cluster"
      namespace: "fabric-platform-prod"
      resources:
        cpu_limit: "8000m"
        memory_limit: "16Gi"
        replicas: 5
        hpa:
          min_replicas: 5
          max_replicas: 20
          cpu_threshold: 70
          memory_threshold: 80

  security:
    authentication:
      method: "ldap"
      ldap_server: "ldap.truist.com"
      base_dn: "OU=Users,DC=truist,DC=com"
    
    authorization:
      rbac_enabled: true
      roles:
        - name: "JOB_VIEWER"
          permissions: ["read"]
        - name: "JOB_CREATOR"
          permissions: ["read", "create"]
        - name: "JOB_MODIFIER"
          permissions: ["read", "create", "update", "delete"]
        - name: "JOB_EXECUTOR"
          permissions: ["read", "execute"]
    
    network:
      ingress_class: "nginx"
      ssl_termination: true
      certificate_issuer: "truist-ca"
      allowed_cidrs:
        - "10.0.0.0/8"   # Internal network
        - "172.16.0.0/12" # VPN network

  database:
    type: "oracle"
    version: "19c"
    high_availability: true
    backup_retention: 30
    connection_pool:
      initial_size: 5
      max_size: 50
      timeout: 30

  monitoring:
    prometheus_enabled: true
    grafana_dashboard: true
    log_aggregation: "elasticsearch"
    alert_channels:
      - "pagerduty"
      - "slack"
      - "email"
    
    sla:
      availability: "99.9%"
      response_time: "100ms"
      error_rate: "0.1%"

  disaster_recovery:
    rto: "4 hours"
    rpo: "15 minutes"
    backup_schedule: "0 2 * * *"  # Daily at 2 AM
    dr_site: "us-west-2"
```

#### ADS Deployment Scripts

```bash
#!/bin/bash
# deploy-to-ads.sh

set -e

ENVIRONMENT=${1:-staging}
VERSION=${2:-latest}

echo "Deploying Fabric Platform to ADS - Environment: $ENVIRONMENT, Version: $VERSION"

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(dev|staging|prod)$ ]]; then
    echo "Error: Invalid environment. Must be dev, staging, or prod"
    exit 1
fi

# Login to ADS
echo "Authenticating with ADS..."
ads auth login --username "$ADS_USERNAME" --password "$ADS_PASSWORD"

# Set project context
ads project set fabric-platform

# Update application configuration
echo "Updating application configuration..."
ads config update --file ads-config.yml --environment "$ENVIRONMENT"

# Deploy application
echo "Deploying application..."
ads deploy \
    --environment "$ENVIRONMENT" \
    --version "$VERSION" \
    --wait-timeout 600 \
    --health-check-retries 10

# Verify deployment
echo "Verifying deployment..."
ads status --environment "$ENVIRONMENT"

# Run smoke tests
echo "Running smoke tests..."
ads test smoke --environment "$ENVIRONMENT"

echo "Deployment completed successfully!"

# Send notification
ads notify \
    --channel "slack" \
    --message "Fabric Platform $VERSION deployed to $ENVIRONMENT" \
    --recipient "#fabric-platform-alerts"
```

### ADS Resource Quotas and Limits

```yaml
# ads-resource-quotas.yml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: fabric-platform-quota
  namespace: fabric-platform-prod
spec:
  hard:
    # Compute Resources
    requests.cpu: "20"
    requests.memory: "40Gi"
    limits.cpu: "40"
    limits.memory: "80Gi"
    
    # Storage Resources
    requests.storage: "1Ti"
    persistentvolumeclaims: "10"
    
    # Object Counts
    pods: "50"
    services: "20"
    configmaps: "50"
    secrets: "30"
    deployments.apps: "20"
    
    # Network
    services.loadbalancers: "5"
    services.nodeports: "0"

---
apiVersion: v1
kind: LimitRange
metadata:
  name: fabric-platform-limits
  namespace: fabric-platform-prod
spec:
  limits:
    - type: Container
      default:
        cpu: "500m"
        memory: "1Gi"
      defaultRequest:
        cpu: "100m"
        memory: "256Mi"
      max:
        cpu: "4"
        memory: "8Gi"
      min:
        cpu: "50m"
        memory: "128Mi"
    
    - type: PersistentVolumeClaim
      max:
        storage: "100Gi"
      min:
        storage: "1Gi"
```

---

## Kubernetes Configuration

### Base Kubernetes Manifests

#### Namespace Configuration
```yaml
# infrastructure/kubernetes/base/namespace.yml
apiVersion: v1
kind: Namespace
metadata:
  name: fabric-platform
  labels:
    app.kubernetes.io/name: fabric-platform
    app.kubernetes.io/version: "1.0.0"
    compliance/sox: "required"
    security/classification: "confidential"
    
  annotations:
    description: "Enterprise Batch Processing Platform"
    contact: "fabric-platform-team@truist.com"
    cost-center: "LENDING-PLATFORM"

---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: fabric-platform-sa
  namespace: fabric-platform
  annotations:
    eks.amazonaws.com/role-arn: "arn:aws:iam::ACCOUNT:role/fabric-platform-role"
```

#### Backend Deployment
```yaml
# infrastructure/kubernetes/base/backend-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: fabric-api
  namespace: fabric-platform
  labels:
    app: fabric-api
    component: backend
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: fabric-api
  template:
    metadata:
      labels:
        app: fabric-api
        component: backend
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: fabric-platform-sa
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      
      containers:
      - name: fabric-api
        image: registry.gitlab.com/fabric-platform/fabric-api:latest
        imagePullPolicy: Always
        
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        - name: management
          containerPort: 8081
          protocol: TCP
        
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production,kubernetes"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        - name: LDAP_URL
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: ldap.url
        - name: LDAP_BASE_DN
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: ldap.baseDn
        
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 5
          failureThreshold: 3
        
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        startupProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
          failureThreshold: 30
        
        volumeMounts:
        - name: app-config
          mountPath: /app/config
          readOnly: true
        - name: logs
          mountPath: /app/logs
        
      volumes:
      - name: app-config
        configMap:
          name: app-config
      - name: logs
        emptyDir: {}
      
      imagePullSecrets:
      - name: gitlab-registry-secret

---
apiVersion: v1
kind: Service
metadata:
  name: fabric-api-service
  namespace: fabric-platform
  labels:
    app: fabric-api
    component: backend
spec:
  type: ClusterIP
  ports:
  - name: http
    port: 8080
    targetPort: 8080
    protocol: TCP
  - name: management
    port: 8081
    targetPort: 8081
    protocol: TCP
  selector:
    app: fabric-api
```

#### Frontend Deployment
```yaml
# infrastructure/kubernetes/base/frontend-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: fabric-ui
  namespace: fabric-platform
  labels:
    app: fabric-ui
    component: frontend
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: fabric-ui
  template:
    metadata:
      labels:
        app: fabric-ui
        component: frontend
        version: v1
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 101  # nginx user
        fsGroup: 101
      
      containers:
      - name: fabric-ui
        image: registry.gitlab.com/fabric-platform/fabric-ui:latest
        imagePullPolicy: Always
        
        ports:
        - name: http
          containerPort: 80
          protocol: TCP
        
        env:
        - name: REACT_APP_API_BASE_URL
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: api.baseUrl
        - name: REACT_APP_ENVIRONMENT
          value: "production"
        
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        
        livenessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 30
          periodSeconds: 10
        
        readinessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 5
          periodSeconds: 5
        
        volumeMounts:
        - name: nginx-config
          mountPath: /etc/nginx/conf.d
          readOnly: true
        - name: nginx-cache
          mountPath: /var/cache/nginx
        - name: nginx-run
          mountPath: /var/run
      
      volumes:
      - name: nginx-config
        configMap:
          name: nginx-config
      - name: nginx-cache
        emptyDir: {}
      - name: nginx-run
        emptyDir: {}
      
      imagePullSecrets:
      - name: gitlab-registry-secret

---
apiVersion: v1
kind: Service
metadata:
  name: fabric-ui-service
  namespace: fabric-platform
  labels:
    app: fabric-ui
    component: frontend
spec:
  type: ClusterIP
  ports:
  - name: http
    port: 80
    targetPort: 80
    protocol: TCP
  selector:
    app: fabric-ui
```

#### Ingress Configuration
```yaml
# infrastructure/kubernetes/base/ingress.yml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: fabric-platform-ingress
  namespace: fabric-platform
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/cluster-issuer: "truist-ca-issuer"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "600"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
spec:
  tls:
  - hosts:
    - fabric-platform.truist.com
    secretName: fabric-platform-tls
  
  rules:
  - host: fabric-platform.truist.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: fabric-api-service
            port:
              number: 8080
      - path: /actuator
        pathType: Prefix
        backend:
          service:
            name: fabric-api-service
            port:
              number: 8081
      - path: /
        pathType: Prefix
        backend:
          service:
            name: fabric-ui-service
            port:
              number: 80
```

### Environment-Specific Overlays

#### Production Overlay
```yaml
# infrastructure/kubernetes/overlays/prod/kustomization.yml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: fabric-platform-prod

resources:
- ../../base

patchesStrategicMerge:
- deployment-patch.yml
- ingress-patch.yml

configMapGenerator:
- name: app-config
  files:
  - application-prod.properties
  
secretGenerator:
- name: database-secret
  literals:
  - url=jdbc:oracle:thin:@prod-oracle.internal:1521/ORCLPDB1
  - username=cm3int_prod
  - password=OVERRIDE_IN_VAULT

images:
- name: registry.gitlab.com/fabric-platform/fabric-api
  newTag: v1.0.0
- name: registry.gitlab.com/fabric-platform/fabric-ui
  newTag: v1.0.0

replicas:
- name: fabric-api
  count: 5
- name: fabric-ui
  count: 3
```

```yaml
# infrastructure/kubernetes/overlays/prod/deployment-patch.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: fabric-api
spec:
  template:
    spec:
      containers:
      - name: fabric-api
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production,kubernetes,monitoring"
        - name: LOGGING_LEVEL_COM_TRUIST_BATCH
          value: "INFO"

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: fabric-ui
spec:
  template:
    spec:
      containers:
      - name: fabric-ui
        resources:
          requests:
            memory: "512Mi"
            cpu: "200m"
          limits:
            memory: "1Gi"
            cpu: "500m"
```

### Horizontal Pod Autoscaler
```yaml
# infrastructure/kubernetes/base/hpa.yml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: fabric-api-hpa
  namespace: fabric-platform
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: fabric-api
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
```

---

## Database Configuration

### Oracle Database Setup

#### Database Instance Configuration
```sql
-- Production Oracle Database Configuration
-- File: database-init.sql

-- Create tablespace for Fabric Platform
CREATE TABLESPACE FABRIC_DATA
  DATAFILE '/u01/app/oracle/oradata/ORCLPDB1/fabric_data01.dbf' SIZE 10G
  AUTOEXTEND ON NEXT 1G MAXSIZE UNLIMITED
  EXTENT MANAGEMENT LOCAL AUTOALLOCATE
  SEGMENT SPACE MANAGEMENT AUTO;

CREATE TABLESPACE FABRIC_INDEX
  DATAFILE '/u01/app/oracle/oradata/ORCLPDB1/fabric_index01.dbf' SIZE 5G
  AUTOEXTEND ON NEXT 500M MAXSIZE UNLIMITED
  EXTENT MANAGEMENT LOCAL AUTOALLOCATE
  SEGMENT SPACE MANAGEMENT AUTO;

-- Create CM3INT schema user
CREATE USER cm3int_prod IDENTIFIED BY "SecurePassword123!"
  DEFAULT TABLESPACE FABRIC_DATA
  TEMPORARY TABLESPACE TEMP
  QUOTA UNLIMITED ON FABRIC_DATA
  QUOTA UNLIMITED ON FABRIC_INDEX;

-- Grant necessary privileges
GRANT CONNECT, RESOURCE TO cm3int_prod;
GRANT CREATE SESSION TO cm3int_prod;
GRANT CREATE TABLE TO cm3int_prod;
GRANT CREATE SEQUENCE TO cm3int_prod;
GRANT CREATE VIEW TO cm3int_prod;
GRANT CREATE PROCEDURE TO cm3int_prod;
GRANT CREATE TRIGGER TO cm3int_prod;

-- Grant system privileges for monitoring
GRANT SELECT ON V_$SESSION TO cm3int_prod;
GRANT SELECT ON V_$PROCESS TO cm3int_prod;
GRANT SELECT ON V_$DATABASE TO cm3int_prod;

-- Create audit trail configuration
ALTER SYSTEM SET audit_trail=DB,EXTENDED SCOPE=SPFILE;
AUDIT ALL ON cm3int_prod.MANUAL_JOB_CONFIG;
AUDIT ALL ON cm3int_prod.MANUAL_JOB_EXECUTION;
AUDIT ALL ON cm3int_prod.MANUAL_JOB_AUDIT;
```

#### Connection Pool Configuration
```yaml
# database-config.yml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: oracle.jdbc.OracleDriver
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    
    hikari:
      # Connection pool settings
      minimum-idle: 5
      maximum-pool-size: 50
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      
      # Oracle-specific settings
      data-source-properties:
        oracle.jdbc.ReadTimeout: 30000
        oracle.net.CONNECT_TIMEOUT: 10000
        oracle.jdbc.autoCommitSpecCompliant: false
        oracle.jdbc.implicitStatementCacheSize: 25
        oracle.jdbc.defaultRowPrefetch: 20
        oracle.jdbc.enableQueryTimeouts: true
        
      # Connection validation
      connection-test-query: "SELECT 1 FROM DUAL"
      validation-timeout: 5000
      
      # Metrics
      register-mbeans: true
      
      # Pool name for monitoring
      pool-name: "FabricPlatformPool"

  # Transaction management
  transaction:
    default-timeout: 30
    rollback-on-commit-failure: true

# Liquibase configuration
liquibase:
  change-log: classpath:db/changelog/db.changelog-master.xml
  contexts: production
  default-schema: cm3int_prod
  drop-first: false
  
# JMX monitoring
management:
  endpoints:
    jmx:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
      show-components: always
```

#### Database Health Check Configuration
```yaml
# database-health-check.yml
management:
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
      path: /
      threshold: 100MB
    
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      sla:
        http.server.requests: 10ms, 50ms, 100ms, 200ms, 500ms

# Custom health indicators
spring:
  application:
    name: fabric-platform
  
# Database monitoring queries
fabric:
  monitoring:
    database:
      connection-pool:
        query: "SELECT active_connections, idle_connections FROM v$session_pool_stats"
        interval: 30s
      
      table-stats:
        query: "SELECT table_name, num_rows, last_analyzed FROM user_tables WHERE table_name LIKE 'MANUAL_JOB%'"
        interval: 5m
      
      performance:
        query: "SELECT sql_id, executions, elapsed_time FROM v$sql WHERE sql_text LIKE '%MANUAL_JOB%'"
        interval: 1m
```

### Database Migration Scripts

#### Initial Schema Creation
```sql
-- V001__Initial_Schema.sql
-- Liquibase changeset for initial schema creation

--changeset fabric-platform:001
--comment: Create core tables for Fabric Platform

-- Manual Job Configuration table
CREATE TABLE MANUAL_JOB_CONFIG (
    CONFIG_ID VARCHAR2(50) NOT NULL,
    JOB_NAME VARCHAR2(100) NOT NULL,
    JOB_TYPE VARCHAR2(50) NOT NULL,
    SOURCE_SYSTEM VARCHAR2(50) NOT NULL,
    TARGET_SYSTEM VARCHAR2(50) NOT NULL,
    JOB_PARAMETERS CLOB NOT NULL,
    STATUS VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    CREATED_BY VARCHAR2(100) NOT NULL,
    CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    LAST_MODIFIED_BY VARCHAR2(100),
    LAST_MODIFIED_DATE TIMESTAMP,
    VERSION_NUMBER NUMBER(10) DEFAULT 1 NOT NULL,
    MASTER_QUERY_ID NUMBER(19),
    
    CONSTRAINT PK_MANUAL_JOB_CONFIG PRIMARY KEY (CONFIG_ID),
    CONSTRAINT UK_MANUAL_JOB_CONFIG_NAME UNIQUE (JOB_NAME),
    CONSTRAINT CHK_STATUS CHECK (STATUS IN ('ACTIVE', 'INACTIVE', 'DEPRECATED'))
) TABLESPACE FABRIC_DATA;

-- Create indexes
CREATE INDEX IDX_MANUAL_JOB_CONFIG_TYPE ON MANUAL_JOB_CONFIG(JOB_TYPE) TABLESPACE FABRIC_INDEX;
CREATE INDEX IDX_MANUAL_JOB_CONFIG_SOURCE ON MANUAL_JOB_CONFIG(SOURCE_SYSTEM) TABLESPACE FABRIC_INDEX;
CREATE INDEX IDX_MANUAL_JOB_CONFIG_STATUS ON MANUAL_JOB_CONFIG(STATUS) TABLESPACE FABRIC_INDEX;
CREATE INDEX IDX_MANUAL_JOB_CONFIG_CREATED ON MANUAL_JOB_CONFIG(CREATED_DATE) TABLESPACE FABRIC_INDEX;

--rollback DROP TABLE MANUAL_JOB_CONFIG;

-- Manual Job Execution table
CREATE TABLE MANUAL_JOB_EXECUTION (
    EXECUTION_ID VARCHAR2(50) NOT NULL,
    CONFIG_ID VARCHAR2(50) NOT NULL,
    EXECUTION_STATUS VARCHAR2(20) NOT NULL,
    START_TIME TIMESTAMP NOT NULL,
    END_TIME TIMESTAMP,
    EXECUTION_PARAMETERS CLOB,
    ERROR_MESSAGE CLOB,
    EXECUTED_BY VARCHAR2(100) NOT NULL,
    CORRELATION_ID VARCHAR2(50),
    
    CONSTRAINT PK_MANUAL_JOB_EXECUTION PRIMARY KEY (EXECUTION_ID),
    CONSTRAINT FK_EXECUTION_CONFIG FOREIGN KEY (CONFIG_ID) REFERENCES MANUAL_JOB_CONFIG(CONFIG_ID),
    CONSTRAINT CHK_EXECUTION_STATUS CHECK (EXECUTION_STATUS IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED'))
) TABLESPACE FABRIC_DATA;

-- Create indexes for execution table
CREATE INDEX IDX_MANUAL_JOB_EXEC_CONFIG ON MANUAL_JOB_EXECUTION(CONFIG_ID) TABLESPACE FABRIC_INDEX;
CREATE INDEX IDX_MANUAL_JOB_EXEC_STATUS ON MANUAL_JOB_EXECUTION(EXECUTION_STATUS) TABLESPACE FABRIC_INDEX;
CREATE INDEX IDX_MANUAL_JOB_EXEC_START ON MANUAL_JOB_EXECUTION(START_TIME) TABLESPACE FABRIC_INDEX;
CREATE INDEX IDX_MANUAL_JOB_EXEC_CORR ON MANUAL_JOB_EXECUTION(CORRELATION_ID) TABLESPACE FABRIC_INDEX;

--rollback DROP TABLE MANUAL_JOB_EXECUTION;
```

#### Audit Table Creation
```sql
-- V002__Audit_Tables.sql
-- SOX-compliant audit trail tables

--changeset fabric-platform:002
--comment: Create audit trail tables for SOX compliance

-- Manual Job Audit table
CREATE TABLE MANUAL_JOB_AUDIT (
    AUDIT_ID VARCHAR2(50) NOT NULL,
    CONFIG_ID VARCHAR2(50) NOT NULL,
    ACTION_TYPE VARCHAR2(20) NOT NULL,
    OLD_VALUES CLOB,
    NEW_VALUES CLOB,
    CHANGED_BY VARCHAR2(100) NOT NULL,
    CHANGE_TIMESTAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CORRELATION_ID VARCHAR2(50),
    BUSINESS_JUSTIFICATION VARCHAR2(500),
    IP_ADDRESS VARCHAR2(45),
    USER_AGENT VARCHAR2(500),
    SESSION_ID VARCHAR2(100),
    
    CONSTRAINT PK_MANUAL_JOB_AUDIT PRIMARY KEY (AUDIT_ID),
    CONSTRAINT CHK_ACTION_TYPE CHECK (ACTION_TYPE IN ('CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE'))
) TABLESPACE FABRIC_DATA;

-- Create indexes for audit table
CREATE INDEX IDX_MANUAL_JOB_AUDIT_CONFIG ON MANUAL_JOB_AUDIT(CONFIG_ID) TABLESPACE FABRIC_INDEX;
CREATE INDEX IDX_MANUAL_JOB_AUDIT_ACTION ON MANUAL_JOB_AUDIT(ACTION_TYPE) TABLESPACE FABRIC_INDEX;
CREATE INDEX IDX_MANUAL_JOB_AUDIT_TIME ON MANUAL_JOB_AUDIT(CHANGE_TIMESTAMP) TABLESPACE FABRIC_INDEX;
CREATE INDEX IDX_MANUAL_JOB_AUDIT_USER ON MANUAL_JOB_AUDIT(CHANGED_BY) TABLESPACE FABRIC_INDEX;
CREATE INDEX IDX_MANUAL_JOB_AUDIT_CORR ON MANUAL_JOB_AUDIT(CORRELATION_ID) TABLESPACE FABRIC_INDEX;

--rollback DROP TABLE MANUAL_JOB_AUDIT;

-- Create audit trigger for automatic logging
CREATE OR REPLACE TRIGGER TRG_MANUAL_JOB_CONFIG_AUDIT
    AFTER INSERT OR UPDATE OR DELETE ON MANUAL_JOB_CONFIG
    FOR EACH ROW
DECLARE
    v_audit_id VARCHAR2(50);
    v_action_type VARCHAR2(20);
    v_old_values CLOB;
    v_new_values CLOB;
    v_user VARCHAR2(100);
    v_correlation_id VARCHAR2(50);
BEGIN
    -- Generate audit ID
    v_audit_id := 'AUD_' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISS') || '_' || SUBSTR(SYS_GUID(), 1, 8);
    
    -- Get current user
    v_user := COALESCE(SYS_CONTEXT('USERENV', 'PROXY_USER'), USER);
    
    -- Get correlation ID from session context
    v_correlation_id := SYS_CONTEXT('FABRIC_CTX', 'CORRELATION_ID');
    
    -- Determine action type
    IF INSERTING THEN
        v_action_type := 'CREATE';
        v_new_values := JSON_OBJECT(
            'CONFIG_ID' VALUE :NEW.CONFIG_ID,
            'JOB_NAME' VALUE :NEW.JOB_NAME,
            'JOB_TYPE' VALUE :NEW.JOB_TYPE,
            'SOURCE_SYSTEM' VALUE :NEW.SOURCE_SYSTEM,
            'TARGET_SYSTEM' VALUE :NEW.TARGET_SYSTEM,
            'STATUS' VALUE :NEW.STATUS,
            'CREATED_BY' VALUE :NEW.CREATED_BY
        );
    ELSIF UPDATING THEN
        v_action_type := 'UPDATE';
        v_old_values := JSON_OBJECT(
            'CONFIG_ID' VALUE :OLD.CONFIG_ID,
            'JOB_NAME' VALUE :OLD.JOB_NAME,
            'JOB_TYPE' VALUE :OLD.JOB_TYPE,
            'SOURCE_SYSTEM' VALUE :OLD.SOURCE_SYSTEM,
            'TARGET_SYSTEM' VALUE :OLD.TARGET_SYSTEM,
            'STATUS' VALUE :OLD.STATUS,
            'VERSION_NUMBER' VALUE :OLD.VERSION_NUMBER
        );
        v_new_values := JSON_OBJECT(
            'CONFIG_ID' VALUE :NEW.CONFIG_ID,
            'JOB_NAME' VALUE :NEW.JOB_NAME,
            'JOB_TYPE' VALUE :NEW.JOB_TYPE,
            'SOURCE_SYSTEM' VALUE :NEW.SOURCE_SYSTEM,
            'TARGET_SYSTEM' VALUE :NEW.TARGET_SYSTEM,
            'STATUS' VALUE :NEW.STATUS,
            'VERSION_NUMBER' VALUE :NEW.VERSION_NUMBER
        );
    ELSIF DELETING THEN
        v_action_type := 'DELETE';
        v_old_values := JSON_OBJECT(
            'CONFIG_ID' VALUE :OLD.CONFIG_ID,
            'JOB_NAME' VALUE :OLD.JOB_NAME,
            'JOB_TYPE' VALUE :OLD.JOB_TYPE,
            'SOURCE_SYSTEM' VALUE :OLD.SOURCE_SYSTEM,
            'TARGET_SYSTEM' VALUE :OLD.TARGET_SYSTEM,
            'STATUS' VALUE :OLD.STATUS
        );
    END IF;
    
    -- Insert audit record
    INSERT INTO MANUAL_JOB_AUDIT (
        AUDIT_ID,
        CONFIG_ID,
        ACTION_TYPE,
        OLD_VALUES,
        NEW_VALUES,
        CHANGED_BY,
        CHANGE_TIMESTAMP,
        CORRELATION_ID,
        IP_ADDRESS,
        SESSION_ID
    ) VALUES (
        v_audit_id,
        COALESCE(:NEW.CONFIG_ID, :OLD.CONFIG_ID),
        v_action_type,
        v_old_values,
        v_new_values,
        v_user,
        CURRENT_TIMESTAMP,
        v_correlation_id,
        SYS_CONTEXT('USERENV', 'IP_ADDRESS'),
        SYS_CONTEXT('USERENV', 'SESSIONID')
    );
END;
/

--rollback DROP TRIGGER TRG_MANUAL_JOB_CONFIG_AUDIT;
```

---

## Security Configuration

### TLS/SSL Configuration

#### Certificate Management
```yaml
# certificates/cert-manager-issuer.yml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: truist-ca-issuer
spec:
  ca:
    secretName: truist-ca-key-pair

---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: fabric-platform-tls
  namespace: fabric-platform
spec:
  secretName: fabric-platform-tls
  issuerRef:
    name: truist-ca-issuer
    kind: ClusterIssuer
  
  dnsNames:
  - fabric-platform.truist.com
  - fabric-platform-api.truist.com
  - fabric-platform-staging.truist.com
  - fabric-platform-dev.truist.com
  
  subject:
    organizationalUnits:
    - "Lending Technology"
    organizations:
    - "Truist Bank"
    countries:
    - "US"
    localities:
    - "Charlotte"
    provinces:
    - "NC"
  
  duration: 8760h  # 1 year
  renewBefore: 720h  # 30 days
  
  privateKey:
    algorithm: RSA
    size: 2048
    rotationPolicy: Always
  
  usages:
  - digital signature
  - key encipherment
  - server auth
```

### Network Security Policies

#### Kubernetes Network Policies
```yaml
# security/network-policies.yml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: fabric-platform-network-policy
  namespace: fabric-platform
spec:
  podSelector:
    matchLabels:
      app: fabric-platform
  
  policyTypes:
  - Ingress
  - Egress
  
  ingress:
  # Allow traffic from ingress controller
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
    - protocol: TCP
      port: 80
  
  # Allow traffic between application components
  - from:
    - podSelector:
        matchLabels:
          component: frontend
    to:
    - podSelector:
        matchLabels:
          component: backend
    ports:
    - protocol: TCP
      port: 8080
  
  # Allow monitoring traffic
  - from:
    - namespaceSelector:
        matchLabels:
          name: monitoring
    ports:
    - protocol: TCP
      port: 8081  # Management port
  
  egress:
  # Allow DNS resolution
  - to: []
    ports:
    - protocol: UDP
      port: 53
    - protocol: TCP
      port: 53
  
  # Allow database access
  - to:
    - namespaceSelector:
        matchLabels:
          name: database
    ports:
    - protocol: TCP
      port: 1521
  
  # Allow LDAP authentication
  - to: []
    ports:
    - protocol: TCP
      port: 389
    - protocol: TCP
      port: 636  # LDAPS
  
  # Allow HTTPS outbound for external APIs
  - to: []
    ports:
    - protocol: TCP
      port: 443

---
# Deny all other traffic
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: fabric-platform-deny-all
  namespace: fabric-platform
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
```

### RBAC Configuration

#### Kubernetes RBAC
```yaml
# security/rbac.yml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: fabric-platform
  name: fabric-platform-role
rules:
# Allow read access to pods and services
- apiGroups: [""]
  resources: ["pods", "services", "configmaps", "secrets"]
  verbs: ["get", "list", "watch"]

# Allow access to deployments
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets"]
  verbs: ["get", "list", "watch"]

# Allow access to ingress
- apiGroups: ["networking.k8s.io"]
  resources: ["ingresses"]
  verbs: ["get", "list", "watch"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: fabric-platform-rolebinding
  namespace: fabric-platform
subjects:
- kind: ServiceAccount
  name: fabric-platform-sa
  namespace: fabric-platform
roleRef:
  kind: Role
  name: fabric-platform-role
  apiGroup: rbac.authorization.k8s.io

---
# ClusterRole for cross-namespace operations
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: fabric-platform-cluster-role
rules:
# Allow reading nodes for monitoring
- apiGroups: [""]
  resources: ["nodes", "nodes/metrics"]
  verbs: ["get", "list", "watch"]

# Allow reading metrics
- apiGroups: ["metrics.k8s.io"]
  resources: ["nodes", "pods"]
  verbs: ["get", "list"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: fabric-platform-cluster-rolebinding
subjects:
- kind: ServiceAccount
  name: fabric-platform-sa
  namespace: fabric-platform
roleRef:
  kind: ClusterRole
  name: fabric-platform-cluster-role
  apiGroup: rbac.authorization.k8s.io
```

### Pod Security Standards

#### Pod Security Policy
```yaml
# security/pod-security-policy.yml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: fabric-platform-psp
spec:
  privileged: false
  allowPrivilegeEscalation: false
  
  # Required to prevent escalations to root
  requiredDropCapabilities:
    - ALL
  
  # Allow core volume types
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  
  hostNetwork: false
  hostIPC: false
  hostPID: false
  
  runAsUser:
    # Require the container to run without root
    rule: 'MustRunAsNonRoot'
  
  seLinux:
    # This policy assumes the nodes are using AppArmor rather than SELinux
    rule: 'RunAsAny'
  
  fsGroup:
    rule: 'RunAsAny'
  
  # Forbid adding the root group
  supplementalGroups:
    rule: 'MustRunAs'
    ranges:
      # Forbid adding the root group
      - min: 1
        max: 65535

---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: fabric-platform-psp-role
  namespace: fabric-platform
rules:
- apiGroups: ['policy']
  resources: ['podsecuritypolicies']
  verbs: ['use']
  resourceNames:
  - fabric-platform-psp

---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: fabric-platform-psp-rolebinding
  namespace: fabric-platform
roleRef:
  kind: Role
  name: fabric-platform-psp-role
  apiGroup: rbac.authorization.k8s.io
subjects:
- kind: ServiceAccount
  name: fabric-platform-sa
  namespace: fabric-platform
```

---

## Monitoring & Logging Setup

### Prometheus Configuration

#### Prometheus Setup
```yaml
# monitoring/prometheus-config.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
      external_labels:
        cluster: 'fabric-platform-prod'
        environment: 'production'
    
    rule_files:
      - "/etc/prometheus/rules/*.yml"
    
    alerting:
      alertmanagers:
      - static_configs:
        - targets:
          - alertmanager:9093
    
    scrape_configs:
    # Fabric Platform Application Metrics
    - job_name: 'fabric-api'
      kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
          - fabric-platform
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_name]
        action: keep
        regex: fabric-api-service
      - source_labels: [__meta_kubernetes_endpoint_port_name]
        action: keep
        regex: management
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      
      # Custom metrics path for Spring Boot Actuator
      metrics_path: /actuator/prometheus
      scrape_interval: 30s
      scrape_timeout: 10s
    
    # Database Metrics
    - job_name: 'oracle-exporter'
      static_configs:
      - targets: ['oracle-exporter:9161']
      scrape_interval: 60s
      scrape_timeout: 30s
      metrics_path: /metrics
    
    # Kubernetes Cluster Metrics
    - job_name: 'kubernetes-pods'
      kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
          - fabric-platform
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
      - action: labelmap
        regex: __meta_kubernetes_pod_label_(.+)
      - source_labels: [__meta_kubernetes_namespace]
        action: replace
        target_label: kubernetes_namespace
      - source_labels: [__meta_kubernetes_pod_name]
        action: replace
        target_label: kubernetes_pod_name

---
# Prometheus Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus
  template:
    metadata:
      labels:
        app: prometheus
    spec:
      serviceAccountName: prometheus
      containers:
      - name: prometheus
        image: prom/prometheus:v2.45.0
        args:
          - '--config.file=/etc/prometheus/prometheus.yml'
          - '--storage.tsdb.path=/prometheus/'
          - '--web.console.libraries=/etc/prometheus/console_libraries'
          - '--web.console.templates=/etc/prometheus/consoles'
          - '--storage.tsdb.retention.time=30d'
          - '--web.enable-lifecycle'
          - '--storage.tsdb.wal-compression'
        ports:
        - containerPort: 9090
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 1000m
            memory: 2Gi
        volumeMounts:
        - name: prometheus-config-volume
          mountPath: /etc/prometheus/
        - name: prometheus-storage-volume
          mountPath: /prometheus/
        - name: prometheus-rules-volume
          mountPath: /etc/prometheus/rules/
        
        livenessProbe:
          httpGet:
            path: /-/healthy
            port: 9090
          initialDelaySeconds: 30
          timeoutSeconds: 30
        
        readinessProbe:
          httpGet:
            path: /-/ready
            port: 9090
          initialDelaySeconds: 30
          timeoutSeconds: 30
      
      volumes:
      - name: prometheus-config-volume
        configMap:
          defaultMode: 420
          name: prometheus-config
      - name: prometheus-storage-volume
        emptyDir: {}
      - name: prometheus-rules-volume
        configMap:
          name: prometheus-rules
```

#### Alerting Rules
```yaml
# monitoring/prometheus-rules.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-rules
  namespace: monitoring
data:
  fabric-platform-rules.yml: |
    groups:
    - name: fabric-platform.rules
      rules:
      # High error rate alert
      - alert: FabricPlatformHighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.01
        for: 2m
        labels:
          severity: critical
          service: fabric-platform
        annotations:
          summary: "High error rate detected in Fabric Platform"
          description: "Error rate is {{ $value }} errors per second"
      
      # High response time alert
      - alert: FabricPlatformHighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
          service: fabric-platform
        annotations:
          summary: "High response time detected in Fabric Platform"
          description: "95th percentile response time is {{ $value }} seconds"
      
      # Database connection pool exhaustion
      - alert: FabricPlatformDatabasePoolExhaustion
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 2m
        labels:
          severity: critical
          service: fabric-platform
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "{{ $value }}% of database connections are in use"
      
      # Pod CPU usage
      - alert: FabricPlatformHighCPUUsage
        expr: rate(container_cpu_usage_seconds_total{pod=~"fabric-.*"}[5m]) > 0.8
        for: 10m
        labels:
          severity: warning
          service: fabric-platform
        annotations:
          summary: "High CPU usage detected"
          description: "Pod {{ $labels.pod }} CPU usage is {{ $value }}%"
      
      # Pod memory usage
      - alert: FabricPlatformHighMemoryUsage
        expr: container_memory_usage_bytes{pod=~"fabric-.*"} / container_spec_memory_limit_bytes > 0.85
        for: 5m
        labels:
          severity: warning
          service: fabric-platform
        annotations:
          summary: "High memory usage detected"
          description: "Pod {{ $labels.pod }} memory usage is {{ $value }}%"
      
      # Database connectivity
      - alert: FabricPlatformDatabaseDown
        expr: up{job="oracle-exporter"} == 0
        for: 1m
        labels:
          severity: critical
          service: fabric-platform
        annotations:
          summary: "Database connectivity lost"
          description: "Cannot connect to Oracle database"
      
      # Application availability
      - alert: FabricPlatformDown
        expr: up{job="fabric-api"} == 0
        for: 1m
        labels:
          severity: critical
          service: fabric-platform
        annotations:
          summary: "Fabric Platform application is down"
          description: "Cannot scrape metrics from Fabric Platform API"
```

### Grafana Dashboard Configuration

#### Grafana Dashboard
```json
{
  "dashboard": {
    "id": null,
    "title": "Fabric Platform - Production Dashboard",
    "tags": ["fabric-platform", "production"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Application Health",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=\"fabric-api\"}",
            "legendFormat": "API Status"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "green", "value": 1}
              ]
            }
          }
        }
      },
      {
        "id": 2,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])",
            "legendFormat": "Requests/sec"
          }
        ]
      },
      {
        "id": 3,
        "title": "Response Time (95th percentile)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          }
        ]
      },
      {
        "id": 4,
        "title": "Database Connection Pool",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "Active Connections"
          },
          {
            "expr": "hikaricp_connections_idle",
            "legendFormat": "Idle Connections"
          },
          {
            "expr": "hikaricp_connections_max",
            "legendFormat": "Max Connections"
          }
        ]
      },
      {
        "id": 5,
        "title": "JVM Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"}",
            "legendFormat": "Heap Used"
          },
          {
            "expr": "jvm_memory_max_bytes{area=\"heap\"}",
            "legendFormat": "Heap Max"
          }
        ]
      },
      {
        "id": 6,
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{status=~\"4..\"}[5m])",
            "legendFormat": "4xx Errors"
          },
          {
            "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])",
            "legendFormat": "5xx Errors"
          }
        ]
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "30s"
  }
}
```

### ELK Stack Configuration

#### Elasticsearch Configuration
```yaml
# logging/elasticsearch.yml
apiVersion: elasticsearch.k8s.elastic.co/v1
kind: Elasticsearch
metadata:
  name: fabric-platform-logs
  namespace: logging
spec:
  version: 8.8.0
  
  nodeSets:
  - name: master
    count: 3
    config:
      node.roles: ["master"]
      node.store.allow_mmap: false
    volumeClaimTemplates:
    - metadata:
        name: elasticsearch-data
      spec:
        accessModes:
        - ReadWriteOnce
        resources:
          requests:
            storage: 100Gi
        storageClassName: gp3
    
    podTemplate:
      spec:
        containers:
        - name: elasticsearch
          resources:
            requests:
              memory: 2Gi
              cpu: 1000m
            limits:
              memory: 4Gi
              cpu: 2000m
          env:
          - name: ES_JAVA_OPTS
            value: "-Xms2g -Xmx2g"
  
  - name: data
    count: 3
    config:
      node.roles: ["data", "ingest"]
      node.store.allow_mmap: false
    volumeClaimTemplates:
    - metadata:
        name: elasticsearch-data
      spec:
        accessModes:
        - ReadWriteOnce
        resources:
          requests:
            storage: 500Gi
        storageClassName: gp3
    
    podTemplate:
      spec:
        containers:
        - name: elasticsearch
          resources:
            requests:
              memory: 4Gi
              cpu: 2000m
            limits:
              memory: 8Gi
              cpu: 4000m
          env:
          - name: ES_JAVA_OPTS
            value: "-Xms4g -Xmx4g"

  http:
    tls:
      selfSignedCertificate:
        disabled: true
```

#### Logstash Configuration
```yaml
# logging/logstash-config.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: logstash-config
  namespace: logging
data:
  logstash.yml: |
    http.host: "0.0.0.0"
    path.config: /usr/share/logstash/pipeline
    
  pipelines.yml: |
    - pipeline.id: fabric-platform
      path.config: "/usr/share/logstash/pipeline/fabric-platform.conf"
      pipeline.workers: 3
      pipeline.batch.size: 125
      pipeline.batch.delay: 50
      
  fabric-platform.conf: |
    input {
      beats {
        port => 5044
        host => "0.0.0.0"
      }
    }
    
    filter {
      # Parse JSON logs from Spring Boot
      if [fields][app] == "fabric-platform" {
        json {
          source => "message"
        }
        
        # Extract correlation ID
        if [correlation_id] {
          mutate {
            add_field => { "trace_id" => "%{correlation_id}" }
          }
        }
        
        # Parse timestamp
        date {
          match => [ "timestamp", "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ]
          target => "@timestamp"
        }
        
        # Classify log levels
        if [level] == "ERROR" {
          mutate {
            add_tag => [ "error" ]
          }
        }
        
        if [level] == "WARN" {
          mutate {
            add_tag => [ "warning" ]
          }
        }
        
        # Extract user information
        if [user] {
          mutate {
            add_field => { "user_id" => "%{user}" }
          }
        }
        
        # SOX audit trail processing
        if [logger_name] =~ /audit/ {
          mutate {
            add_tag => [ "audit", "sox_compliance" ]
          }
        }
      }
      
      # Parse database logs
      if [fields][component] == "oracle" {
        grok {
          match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} %{WORD:log_level} %{GREEDYDATA:log_message}" }
        }
      }
    }
    
    output {
      elasticsearch {
        hosts => ["fabric-platform-logs-es-http:9200"]
        index => "fabric-platform-%{+YYYY.MM.dd}"
        
        # Template for index mapping
        template_name => "fabric-platform"
        template => "/usr/share/logstash/templates/fabric-platform-template.json"
        template_overwrite => true
      }
      
      # Output to stdout for debugging
      stdout {
        codec => rubydebug
      }
    }

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: logstash
  namespace: logging
spec:
  replicas: 2
  selector:
    matchLabels:
      app: logstash
  template:
    metadata:
      labels:
        app: logstash
    spec:
      containers:
      - name: logstash
        image: docker.elastic.co/logstash/logstash:8.8.0
        ports:
        - containerPort: 5044
        - containerPort: 9600
        resources:
          requests:
            memory: 1Gi
            cpu: 500m
          limits:
            memory: 2Gi
            cpu: 1000m
        env:
        - name: LS_JAVA_OPTS
          value: "-Xmx1g -Xms1g"
        volumeMounts:
        - name: logstash-config
          mountPath: /usr/share/logstash/config/
        - name: logstash-pipeline
          mountPath: /usr/share/logstash/pipeline/
      volumes:
      - name: logstash-config
        configMap:
          name: logstash-config
      - name: logstash-pipeline
        configMap:
          name: logstash-config
```

---

## Environment-Specific Configurations

### Development Environment

#### Development Configuration Files
```yaml
# environments/dev/application-dev.yml
spring:
  application:
    name: fabric-platform-dev
  
  datasource:
    url: jdbc:oracle:thin:@dev-oracle.internal:1521/ORCLPDB1
    username: cm3int_dev
    password: ${DB_PASSWORD_DEV}
    hikari:
      minimum-idle: 2
      maximum-pool-size: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 900000

  profiles:
    active: development,debug

# Logging configuration for development
logging:
  level:
    com.truist.batch: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
    org.springframework.jdbc: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [%X{correlationId}] - %msg%n"

# Development-specific features
fabric:
  security:
    cors:
      allowed-origins: 
        - "http://localhost:3000"
        - "https://dev.fabric-platform.truist.com"
    ldap:
      enabled: false  # Use mock authentication in dev
  
  monitoring:
    enabled: true
    detailed-metrics: true
  
  audit:
    enabled: true
    detailed-logging: true

# Mock data configuration
mock:
  data:
    enabled: true
    users:
      - username: "dev_user"
        roles: ["JOB_VIEWER", "JOB_CREATOR", "JOB_MODIFIER", "JOB_EXECUTOR"]
      - username: "qa_user"
        roles: ["JOB_VIEWER", "JOB_EXECUTOR"]
```

### Test Environment

#### Test Configuration Files
```yaml
# environments/test/application-test.yml
spring:
  application:
    name: fabric-platform-test
  
  datasource:
    url: jdbc:oracle:thin:@test-oracle.internal:1521/ORCLPDB1
    username: cm3int_test
    password: ${DB_PASSWORD_TEST}
    hikari:
      minimum-idle: 3
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  profiles:
    active: test,monitoring

# Test-specific logging
logging:
  level:
    com.truist.batch: INFO
    org.springframework.security: WARN
    org.springframework.test: DEBUG
  file:
    name: /var/log/fabric-platform/application.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30

# Performance testing configuration
fabric:
  performance:
    testing:
      enabled: true
      max-concurrent-requests: 100
      test-data-size: 10000
  
  security:
    cors:
      allowed-origins: 
        - "https://test.fabric-platform.truist.com"
        - "https://qa-tools.truist.com"
    ldap:
      enabled: true
      server: "ldap://test-ldap.truist.com:389"
      base-dn: "OU=TestUsers,DC=test,DC=truist,DC=com"

# Integration test configuration
integration:
  tests:
    database:
      cleanup-after-test: true
      use-test-schema: true
    
    external-services:
      mock-enabled: true
      timeout: 30000
```

### Staging Environment

#### Staging Configuration Files
```yaml
# environments/staging/application-staging.yml
spring:
  application:
    name: fabric-platform-staging
  
  datasource:
    url: jdbc:oracle:thin:@staging-oracle.internal:1521/ORCLPDB1
    username: cm3int_staging
    password: ${DB_PASSWORD_STAGING}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 30
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000

  profiles:
    active: staging,monitoring,security

# Production-like logging
logging:
  level:
    com.truist.batch: INFO
    org.springframework.security: WARN
    org.springframework.web: WARN
  file:
    name: /var/log/fabric-platform/application.log
  logback:
    rollingpolicy:
      max-file-size: 500MB
      max-history: 60

# Staging-specific configuration
fabric:
  security:
    cors:
      allowed-origins: 
        - "https://staging.fabric-platform.truist.com"
    ldap:
      enabled: true
      server: "ldaps://ldap.truist.com:636"
      base-dn: "OU=Users,DC=truist,DC=com"
      ssl-enabled: true
  
  monitoring:
    enabled: true
    detailed-metrics: false
    performance-tracking: true
  
  audit:
    enabled: true
    detailed-logging: false
    compliance-mode: true

# Blue-green deployment configuration
deployment:
  strategy: blue-green
  health-check:
    timeout: 300
    retries: 10
  
  canary:
    enabled: true
    percentage: 10
    duration: 300  # 5 minutes
```

### Production Environment

#### Production Configuration Files
```yaml
# environments/prod/application-prod.yml
spring:
  application:
    name: fabric-platform
  
  datasource:
    url: jdbc:oracle:thin:@prod-oracle-cluster.internal:1521/ORCLPDB1
    username: cm3int_prod
    password: ${DB_PASSWORD_PROD}
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      register-mbeans: true

  profiles:
    active: production,monitoring,security,audit

# Production logging
logging:
  level:
    root: WARN
    com.truist.batch: INFO
    org.springframework.security: ERROR
  file:
    name: /var/log/fabric-platform/application.log
  logback:
    rollingpolicy:
      max-file-size: 1GB
      max-history: 90
      total-size-cap: 20GB

# Production security configuration
fabric:
  security:
    cors:
      allowed-origins: 
        - "https://fabric-platform.truist.com"
    ldap:
      enabled: true
      server: "ldaps://ldap.truist.com:636"
      base-dn: "OU=Users,DC=truist,DC=com"
      ssl-enabled: true
      connection-pool:
        initial-size: 5
        max-size: 20
    
    session:
      timeout: 1800  # 30 minutes
      max-concurrent-sessions: 1
    
    rate-limiting:
      enabled: true
      requests-per-minute: 60
      burst-capacity: 10

  monitoring:
    enabled: true
    detailed-metrics: false
    performance-tracking: true
    health-check-interval: 30
  
  audit:
    enabled: true
    detailed-logging: false
    compliance-mode: true
    retention-days: 2555  # 7 years

# Production performance tuning
performance:
  jvm:
    heap-size: "4g"
    gc-algorithm: "G1GC"
    gc-options: "-XX:MaxGCPauseMillis=200"
  
  connection-pool:
    validation-query: "SELECT 1 FROM DUAL"
    test-on-borrow: true
    test-while-idle: true
  
  cache:
    enabled: true
    ttl: 300  # 5 minutes
    max-size: 10000

# Disaster recovery configuration
disaster-recovery:
  backup:
    enabled: true
    schedule: "0 2 * * *"  # Daily at 2 AM
    retention-days: 30
  
  replication:
    enabled: true
    target-region: "us-west-2"
    sync-interval: 300  # 5 minutes
```

---

## Deployment Procedures

### Automated Deployment Process

#### GitLab CI/CD Deployment Script
```bash
#!/bin/bash
# scripts/deploy.sh

set -euo pipefail

# Configuration
ENVIRONMENT=${1:-staging}
VERSION=${2:-latest}
NAMESPACE="fabric-platform"
DEPLOYMENT_TIMEOUT=600
HEALTH_CHECK_RETRIES=10

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a deployment.log
}

# Error handling
error_exit() {
    log "ERROR: $1"
    exit 1
}

# Validate inputs
validate_inputs() {
    log "Validating deployment inputs..."
    
    if [[ ! "$ENVIRONMENT" =~ ^(dev|test|staging|prod)$ ]]; then
        error_exit "Invalid environment: $ENVIRONMENT. Must be dev, test, staging, or prod."
    fi
    
    if [[ -z "$VERSION" ]]; then
        error_exit "Version must be specified"
    fi
    
    log "Inputs validated successfully"
}

# Pre-deployment checks
pre_deployment_checks() {
    log "Running pre-deployment checks..."
    
    # Check Kubernetes connectivity
    kubectl cluster-info || error_exit "Cannot connect to Kubernetes cluster"
    
    # Check namespace exists
    kubectl get namespace $NAMESPACE-$ENVIRONMENT || error_exit "Namespace $NAMESPACE-$ENVIRONMENT does not exist"
    
    # Check if images exist
    docker manifest inspect $DOCKER_REGISTRY/fabric-api:$VERSION || error_exit "Backend image not found"
    docker manifest inspect $DOCKER_REGISTRY/fabric-ui:$VERSION || error_exit "Frontend image not found"
    
    # Check database connectivity
    kubectl exec -n $NAMESPACE-$ENVIRONMENT deployment/fabric-api -- \
        curl -f http://localhost:8081/actuator/health/db || error_exit "Database health check failed"
    
    log "Pre-deployment checks completed successfully"
}

# Backup current deployment
backup_deployment() {
    log "Creating backup of current deployment..."
    
    local backup_dir="backups/$(date +%Y%m%d_%H%M%S)_$ENVIRONMENT"
    mkdir -p "$backup_dir"
    
    # Backup current configurations
    kubectl get deployment -n $NAMESPACE-$ENVIRONMENT -o yaml > "$backup_dir/deployments.yaml"
    kubectl get configmap -n $NAMESPACE-$ENVIRONMENT -o yaml > "$backup_dir/configmaps.yaml"
    kubectl get secret -n $NAMESPACE-$ENVIRONMENT -o yaml > "$backup_dir/secrets.yaml"
    kubectl get ingress -n $NAMESPACE-$ENVIRONMENT -o yaml > "$backup_dir/ingress.yaml"
    
    # Store current image versions
    kubectl get deployment fabric-api -n $NAMESPACE-$ENVIRONMENT -o jsonpath='{.spec.template.spec.containers[0].image}' > "$backup_dir/current-backend-version.txt"
    kubectl get deployment fabric-ui -n $NAMESPACE-$ENVIRONMENT -o jsonpath='{.spec.template.spec.containers[0].image}' > "$backup_dir/current-frontend-version.txt"
    
    log "Backup created in $backup_dir"
    echo "$backup_dir" > last_backup.txt
}

# Deploy application
deploy_application() {
    log "Deploying Fabric Platform version $VERSION to $ENVIRONMENT..."
    
    # Update image tags in kustomization
    cd infrastructure/kubernetes/overlays/$ENVIRONMENT
    
    kustomize edit set image fabric-api=$DOCKER_REGISTRY/fabric-api:$VERSION
    kustomize edit set image fabric-ui=$DOCKER_REGISTRY/fabric-ui:$VERSION
    
    # Apply configuration changes
    kubectl apply -k . || error_exit "Failed to apply Kubernetes configurations"
    
    # Wait for rollout to complete
    log "Waiting for deployment rollout to complete..."
    kubectl rollout status deployment/fabric-api -n $NAMESPACE-$ENVIRONMENT --timeout=${DEPLOYMENT_TIMEOUT}s || error_exit "Backend deployment failed"
    kubectl rollout status deployment/fabric-ui -n $NAMESPACE-$ENVIRONMENT --timeout=${DEPLOYMENT_TIMEOUT}s || error_exit "Frontend deployment failed"
    
    log "Deployment rollout completed successfully"
}

# Health checks
health_checks() {
    log "Running post-deployment health checks..."
    
    local retries=0
    local max_retries=$HEALTH_CHECK_RETRIES
    local health_check_passed=false
    
    while [[ $retries -lt $max_retries ]]; do
        log "Health check attempt $((retries + 1))/$max_retries"
        
        # Check application health
        if kubectl exec -n $NAMESPACE-$ENVIRONMENT deployment/fabric-api -- \
            curl -f http://localhost:8081/actuator/health; then
            
            # Check API endpoints
            if kubectl exec -n $NAMESPACE-$ENVIRONMENT deployment/fabric-api -- \
                curl -f http://localhost:8080/api/v2/manual-job-config/statistics; then
                
                health_check_passed=true
                break
            fi
        fi
        
        retries=$((retries + 1))
        sleep 30
    done
    
    if [[ "$health_check_passed" == "false" ]]; then
        error_exit "Health checks failed after $max_retries attempts"
    fi
    
    log "Health checks completed successfully"
}

# Smoke tests
smoke_tests() {
    log "Running smoke tests..."
    
    # Get application URL
    local app_url
    if [[ "$ENVIRONMENT" == "prod" ]]; then
        app_url="https://fabric-platform.truist.com"
    else
        app_url="https://$ENVIRONMENT.fabric-platform.truist.com"
    fi
    
    # Test frontend accessibility
    curl -f "$app_url" || error_exit "Frontend smoke test failed"
    
    # Test API health endpoint
    curl -f "$app_url/actuator/health" || error_exit "API health smoke test failed"
    
    # Test API statistics endpoint
    curl -f "$app_url/api/v2/manual-job-config/statistics" || error_exit "API statistics smoke test failed"
    
    log "Smoke tests completed successfully"
}

# Performance verification
performance_verification() {
    log "Running performance verification..."
    
    # Check response times
    local response_time
    response_time=$(curl -o /dev/null -s -w '%{time_total}' https://$ENVIRONMENT.fabric-platform.truist.com/actuator/health)
    
    if (( $(echo "$response_time > 1.0" | bc -l) )); then
        log "WARNING: Response time is $response_time seconds (> 1.0s threshold)"
    else
        log "Response time check passed: $response_time seconds"
    fi
    
    # Check pod resource usage
    kubectl top pods -n $NAMESPACE-$ENVIRONMENT --containers | tee resource_usage.log
    
    log "Performance verification completed"
}

# Notification
send_notification() {
    local status=$1
    local message=$2
    
    log "Sending notification: $status - $message"
    
    # Send to Slack (if configured)
    if [[ -n "${SLACK_WEBHOOK_URL:-}" ]]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"Fabric Platform Deployment: $status - $message\"}" \
            "$SLACK_WEBHOOK_URL"
    fi
    
    # Send to PagerDuty (for critical failures)
    if [[ "$status" == "FAILED" && -n "${PAGERDUTY_INTEGRATION_KEY:-}" ]]; then
        curl -X POST -H 'Content-Type: application/json' \
            -d "{
                \"routing_key\": \"$PAGERDUTY_INTEGRATION_KEY\",
                \"event_action\": \"trigger\",
                \"payload\": {
                    \"summary\": \"Fabric Platform deployment failed in $ENVIRONMENT\",
                    \"source\": \"GitLab CI/CD\",
                    \"severity\": \"critical\"
                }
            }" \
            https://events.pagerduty.com/v2/enqueue
    fi
}

# Rollback function
rollback() {
    log "Initiating rollback procedure..."
    
    if [[ -f last_backup.txt ]]; then
        local backup_dir=$(cat last_backup.txt)
        log "Rolling back to backup: $backup_dir"
        
        # Restore configurations
        kubectl apply -f "$backup_dir/deployments.yaml"
        kubectl apply -f "$backup_dir/configmaps.yaml"
        kubectl apply -f "$backup_dir/secrets.yaml"
        kubectl apply -f "$backup_dir/ingress.yaml"
        
        # Wait for rollback to complete
        kubectl rollout status deployment/fabric-api -n $NAMESPACE-$ENVIRONMENT --timeout=${DEPLOYMENT_TIMEOUT}s
        kubectl rollout status deployment/fabric-ui -n $NAMESPACE-$ENVIRONMENT --timeout=${DEPLOYMENT_TIMEOUT}s
        
        log "Rollback completed successfully"
    else
        error_exit "No backup found for rollback"
    fi
}

# Main deployment flow
main() {
    log "Starting deployment of Fabric Platform..."
    log "Environment: $ENVIRONMENT, Version: $VERSION"
    
    # Trap errors and perform rollback
    trap 'rollback; send_notification "FAILED" "Deployment failed and rollback completed"' ERR
    
    validate_inputs
    pre_deployment_checks
    backup_deployment
    deploy_application
    health_checks
    smoke_tests
    performance_verification
    
    log "Deployment completed successfully!"
    send_notification "SUCCESS" "Version $VERSION deployed to $ENVIRONMENT successfully"
}

# Execute main function
main "$@"
```

### Manual Deployment Procedures

#### Emergency Deployment Checklist
```markdown
# Emergency Deployment Checklist

## Pre-Deployment (5 minutes)
- [ ] Validate emergency change approval is obtained
- [ ] Confirm deployment artifacts are available and tested
- [ ] Verify database connectivity and health
- [ ] Check current system load and user activity
- [ ] Ensure backup is available for immediate rollback

## Deployment Steps (10 minutes)
- [ ] Enable maintenance mode (if required)
- [ ] Execute automated deployment script
- [ ] Monitor deployment progress in real-time
- [ ] Validate pod startup and health checks
- [ ] Confirm database migrations completed successfully

## Post-Deployment Validation (10 minutes)
- [ ] Execute health checks on all components
- [ ] Verify API endpoints are responding correctly
- [ ] Confirm frontend accessibility and functionality
- [ ] Validate database connection pool status
- [ ] Check error logs for any immediate issues

## Performance Verification (5 minutes)
- [ ] Monitor response times and error rates
- [ ] Verify resource utilization is within normal ranges
- [ ] Confirm no performance degradation
- [ ] Check that all scheduled jobs are working

## Communication (Ongoing)
- [ ] Notify stakeholders of deployment start
- [ ] Provide status updates during deployment
- [ ] Confirm successful completion
- [ ] Document any issues or deviations
- [ ] Update incident ticket (if applicable)

## Rollback Criteria
Initiate immediate rollback if:
- [ ] Health checks fail for > 2 minutes
- [ ] Error rate exceeds 1% for > 5 minutes
- [ ] Response time degradation > 50%
- [ ] Database connectivity issues
- [ ] Critical functionality unavailable

## Success Criteria
Deployment is considered successful when:
- [ ] All health checks pass for 5 consecutive minutes
- [ ] Error rate < 0.1%
- [ ] Response times within SLA (< 100ms)
- [ ] All API endpoints responding correctly
- [ ] Frontend fully functional
- [ ] Database performance normal
```

### Blue-Green Deployment Strategy

#### Blue-Green Deployment Configuration
```yaml
# deployment/blue-green-config.yml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: fabric-platform-rollout
  namespace: fabric-platform
spec:
  replicas: 5
  strategy:
    blueGreen:
      # Active service points to the active version
      activeService: fabric-platform-active
      # Preview service points to the new version during deployment
      previewService: fabric-platform-preview
      
      # Automatic promotion after successful checks
      autoPromotionEnabled: false
      
      # Time to wait before automatically promoting
      scaleDownDelaySeconds: 30
      
      # Time to wait before scaling down old version
      prePromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: fabric-platform-preview
      
      # Post-promotion analysis
      postPromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: fabric-platform-active
  
  selector:
    matchLabels:
      app: fabric-platform
  
  template:
    metadata:
      labels:
        app: fabric-platform
    spec:
      containers:
      - name: fabric-api
        image: registry.gitlab.com/fabric-platform/fabric-api:stable
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 5

---
# Analysis template for automated promotion
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: success-rate
spec:
  args:
  - name: service-name
  metrics:
  - name: success-rate
    interval: 60s
    count: 5
    successCondition: result[0] >= 0.95
    provider:
      prometheus:
        address: http://prometheus:9090
        query: |
          sum(rate(http_requests_total{service="{{args.service-name}}",code!~"5.."}[2m])) /
          sum(rate(http_requests_total{service="{{args.service-name}}"}[2m]))
  
  - name: avg-response-time
    interval: 60s
    count: 5
    successCondition: result[0] <= 0.1
    provider:
      prometheus:
        address: http://prometheus:9090
        query: |
          histogram_quantile(0.95,
            sum by (le) (rate(http_request_duration_seconds_bucket{service="{{args.service-name}}"}[2m]))
          )
```

---

## Troubleshooting Guide

### Common Issues and Solutions

#### Application Won't Start

**Symptoms:**
- Pods in CrashLoopBackOff state
- Health checks failing
- Connection timeouts

**Diagnosis Steps:**
```bash
# Check pod status
kubectl get pods -n fabric-platform

# View pod logs
kubectl logs -f deployment/fabric-api -n fabric-platform

# Describe pod for events
kubectl describe pod -l app=fabric-api -n fabric-platform

# Check resource usage
kubectl top pods -n fabric-platform
```

**Common Solutions:**
1. **Database Connection Issues:**
   ```bash
   # Test database connectivity
   kubectl exec -it deployment/fabric-api -n fabric-platform -- \
     curl http://localhost:8081/actuator/health/db
   
   # Check database credentials
   kubectl get secret database-secret -n fabric-platform -o yaml
   ```

2. **Memory Issues:**
   ```yaml
   # Increase memory limits
   resources:
     limits:
       memory: "4Gi"
     requests:
       memory: "2Gi"
   ```

3. **Configuration Issues:**
   ```bash
   # Check configuration maps
   kubectl get configmap app-config -n fabric-platform -o yaml
   
   # Validate configuration syntax
   kubectl apply --dry-run=client -f deployment.yaml
   ```

#### High Response Times

**Symptoms:**
- API responses taking > 500ms
- Frontend loading slowly
- Database query timeouts

**Diagnosis Steps:**
```bash
# Check application metrics
curl http://fabric-platform.truist.com/actuator/metrics/http.server.requests

# Monitor database connections
kubectl exec -it deployment/fabric-api -n fabric-platform -- \
  curl http://localhost:8081/actuator/metrics/hikaricp.connections

# Check resource utilization
kubectl top pods -n fabric-platform --containers
```

**Solutions:**
1. **Database Connection Pool Tuning:**
   ```yaml
   spring.datasource.hikari:
     maximum-pool-size: 50
     minimum-idle: 10
     connection-timeout: 30000
   ```

2. **JVM Tuning:**
   ```yaml
   env:
   - name: JAVA_OPTS
     value: "-Xmx4g -Xms2g -XX:+UseG1GC"
   ```

3. **Scale Horizontally:**
   ```bash
   kubectl scale deployment fabric-api --replicas=10 -n fabric-platform
   ```

#### Database Connection Pool Exhaustion

**Symptoms:**
- "Connection pool exhausted" errors
- Unable to obtain connection messages
- High database connection count

**Diagnosis Steps:**
```bash
# Check connection pool metrics
curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.active | jq .

# Monitor database sessions
sqlplus cm3int_prod/password@prod-oracle << EOF
SELECT COUNT(*) FROM v$session WHERE username = 'CM3INT_PROD';
EOF
```

**Solutions:**
1. **Increase Pool Size:**
   ```yaml
   spring.datasource.hikari:
     maximum-pool-size: 100
     connection-timeout: 60000
     leak-detection-threshold: 120000
   ```

2. **Optimize Queries:**
   ```java
   // Use try-with-resources
   try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
       // Execute query
   }
   ```

3. **Connection Leak Detection:**
   ```yaml
   spring.datasource.hikari:
     leak-detection-threshold: 60000
     register-mbeans: true
   ```

#### SSL Certificate Issues

**Symptoms:**
- HTTPS connections failing
- Certificate validation errors
- Browser security warnings

**Diagnosis Steps:**
```bash
# Check certificate status
kubectl get certificate -n fabric-platform

# View certificate details
kubectl describe certificate fabric-platform-tls -n fabric-platform

# Test SSL connection
openssl s_client -connect fabric-platform.truist.com:443 -servername fabric-platform.truist.com
```

**Solutions:**
1. **Renew Certificate:**
   ```bash
   kubectl delete certificate fabric-platform-tls -n fabric-platform
   kubectl apply -f certificate.yaml
   ```

2. **Check Issuer:**
   ```bash
   kubectl get clusterissuer truist-ca-issuer -o yaml
   ```

3. **Manual Certificate Update:**
   ```bash
   kubectl create secret tls fabric-platform-tls \
     --cert=fabric-platform.crt \
     --key=fabric-platform.key \
     -n fabric-platform
   ```

### Performance Troubleshooting

#### Memory Leaks

**Detection:**
```bash
# Monitor memory usage over time
kubectl top pods -n fabric-platform --containers

# Check for memory pressure
kubectl describe nodes | grep -A 5 "Memory Pressure"

# Get heap dump
kubectl exec -it deployment/fabric-api -n fabric-platform -- \
  curl -X POST http://localhost:8081/actuator/heapdump
```

**Analysis:**
```bash
# Enable detailed GC logging
env:
- name: JAVA_OPTS
  value: "-XX:+PrintGC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"

# Monitor GC metrics
curl http://localhost:8081/actuator/metrics/jvm.gc.pause
```

#### Database Performance Issues

**Detection:**
```sql
-- Find slow queries
SELECT sql_id, sql_text, executions, elapsed_time, cpu_time
FROM v$sql 
WHERE elapsed_time > 1000000  -- 1 second
ORDER BY elapsed_time DESC;

-- Check for lock contention
SELECT blocking_session, sid, serial#, wait_class, event
FROM v$session 
WHERE blocking_session IS NOT NULL;

-- Monitor table statistics
SELECT table_name, num_rows, last_analyzed
FROM user_tables 
WHERE table_name LIKE 'MANUAL_JOB%';
```

**Optimization:**
```sql
-- Update table statistics
EXEC DBMS_STATS.GATHER_TABLE_STATS('CM3INT_PROD', 'MANUAL_JOB_CONFIG');

-- Analyze slow queries
EXPLAIN PLAN FOR
SELECT * FROM MANUAL_JOB_CONFIG WHERE status = 'ACTIVE';

-- Add missing indexes
CREATE INDEX IDX_MANUAL_JOB_CONFIG_STATUS_CREATED 
ON MANUAL_JOB_CONFIG(STATUS, CREATED_DATE);
```

### Disaster Recovery Procedures

#### Database Recovery

**Backup Restoration:**
```bash
# Connect to Oracle
sqlplus sys/password@prod-oracle as sysdba

# Check available backups
RMAN TARGET sys/password@prod-oracle
LIST BACKUP SUMMARY;

# Restore from backup
RESTORE DATABASE;
RECOVER DATABASE;
ALTER DATABASE OPEN;
```

**Point-in-Time Recovery:**
```bash
# Stop application
kubectl scale deployment fabric-api --replicas=0 -n fabric-platform

# Restore to specific timestamp
RMAN TARGET sys/password@prod-oracle
RUN {
  SET UNTIL TIME "TO_DATE('2025-08-19 14:30:00','YYYY-MM-DD HH24:MI:SS')";
  RESTORE DATABASE;
  RECOVER DATABASE;
}

# Open database
ALTER DATABASE OPEN RESETLOGS;
```

#### Application Recovery

**Complete Environment Recovery:**
```bash
# Restore from infrastructure backup
terraform apply -auto-approve

# Restore Kubernetes configurations
kubectl apply -f backup/namespace.yaml
kubectl apply -f backup/secrets.yaml
kubectl apply -f backup/configmaps.yaml
kubectl apply -f backup/deployments.yaml

# Verify recovery
kubectl get pods -n fabric-platform
curl https://fabric-platform.truist.com/actuator/health
```

---

## Maintenance Procedures

### Regular Maintenance Tasks

#### Daily Maintenance
```bash
#!/bin/bash
# scripts/daily-maintenance.sh

# Check system health
kubectl get pods -n fabric-platform
kubectl top nodes
kubectl top pods -n fabric-platform

# Check certificate expiration
kubectl get certificates -n fabric-platform -o custom-columns=NAME:.metadata.name,READY:.status.conditions[0].status,SECRET:.spec.secretName,ISSUER:.spec.issuerRef.name,EXPIRES:.status.notAfter

# Monitor resource usage
kubectl describe nodes | grep -A 5 "Allocated resources"

# Check application logs for errors
kubectl logs deployment/fabric-api -n fabric-platform --since=24h | grep -i error

# Verify backup completion
kubectl get cronjob -n fabric-platform
kubectl get jobs -n fabric-platform
```

#### Weekly Maintenance
```bash
#!/bin/bash
# scripts/weekly-maintenance.sh

# Update container images for security patches
kubectl set image deployment/fabric-api fabric-api=registry.gitlab.com/fabric-platform/fabric-api:latest -n fabric-platform

# Clean up old resources
kubectl delete pods --field-selector=status.phase=Succeeded -n fabric-platform
kubectl delete jobs --field-selector=status.conditions[0].type=Complete -n fabric-platform

# Database maintenance
sqlplus cm3int_prod/password@prod-oracle << EOF
-- Update table statistics
EXEC DBMS_STATS.GATHER_SCHEMA_STATS('CM3INT_PROD');

-- Purge old audit records (keep 90 days)
DELETE FROM MANUAL_JOB_AUDIT 
WHERE CHANGE_TIMESTAMP < SYSDATE - 90;
COMMIT;

-- Check tablespace usage
SELECT tablespace_name, 
       ROUND(bytes/1024/1024/1024,2) AS gb_total,
       ROUND(free/1024/1024/1024,2) AS gb_free,
       ROUND((bytes-free)/bytes*100,2) AS pct_used
FROM (
  SELECT tablespace_name, bytes, 
         (SELECT SUM(bytes) FROM dba_free_space WHERE tablespace_name = t.tablespace_name) AS free
  FROM dba_tablespaces t, dba_data_files d 
  WHERE t.tablespace_name = d.tablespace_name
  AND t.tablespace_name IN ('FABRIC_DATA', 'FABRIC_INDEX')
);
EOF
```

#### Monthly Maintenance
```bash
#!/bin/bash
# scripts/monthly-maintenance.sh

# Security patch updates
kubectl patch deployment fabric-api -n fabric-platform -p '{"spec":{"template":{"metadata":{"annotations":{"date":"'$(date +%s)'"}}}}}' 

# Performance analysis
kubectl top pods -n fabric-platform --sort-by=cpu
kubectl top pods -n fabric-platform --sort-by=memory

# Certificate renewal check
openssl x509 -in /tmp/fabric-platform.crt -noout -dates

# Database performance tuning
sqlplus cm3int_prod/password@prod-oracle << EOF
-- Analyze database performance
SELECT sql_id, executions, elapsed_time, cpu_time, disk_reads
FROM v$sql 
WHERE executions > 1000
ORDER BY elapsed_time DESC
FETCH FIRST 10 ROWS ONLY;

-- Check for fragmentation
SELECT table_name, 
       ROUND((blocks*8192/1024/1024),2) AS mb_size,
       ROUND((num_rows*avg_row_len/1024/1024),2) AS mb_actual_data,
       ROUND((blocks*8192-num_rows*avg_row_len)/(blocks*8192)*100,2) AS pct_fragmentation
FROM user_tables 
WHERE table_name LIKE 'MANUAL_JOB%'
AND num_rows > 0;

-- Rebuild fragmented indexes if needed
-- ALTER INDEX index_name REBUILD ONLINE;
EOF
```

### Backup and Recovery Procedures

#### Automated Backup Configuration
```yaml
# backup/backup-cronjob.yml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: fabric-platform-backup
  namespace: fabric-platform
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  successfulJobsHistoryLimit: 7
  failedJobsHistoryLimit: 3
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: oracle/database:19.3.0-ee
            command:
            - /bin/bash
            - -c
            - |
              # Database backup
              ORACLE_SID=ORCLPDB1
              export ORACLE_SID
              
              # Create backup directory
              mkdir -p /backup/$(date +%Y%m%d)
              
              # Export schema
              expdp cm3int_prod/password@prod-oracle \
                schemas=cm3int_prod \
                directory=BACKUP_DIR \
                dumpfile=fabric_platform_$(date +%Y%m%d).dmp \
                logfile=fabric_platform_$(date +%Y%m%d).log
              
              # Backup configuration
              kubectl get all -n fabric-platform -o yaml > /backup/$(date +%Y%m%d)/kubernetes-config.yaml
              
              # Upload to S3 (if configured)
              aws s3 cp /backup/$(date +%Y%m%d)/ s3://fabric-platform-backups/$(date +%Y%m%d)/ --recursive
              
              # Clean up old backups (keep 30 days)
              find /backup -type d -mtime +30 -exec rm -rf {} \;
              
            volumeMounts:
            - name: backup-storage
              mountPath: /backup
            env:
            - name: AWS_ACCESS_KEY_ID
              valueFrom:
                secretKeyRef:
                  name: aws-credentials
                  key: access-key
            - name: AWS_SECRET_ACCESS_KEY
              valueFrom:
                secretKeyRef:
                  name: aws-credentials
                  key: secret-key
          
          volumes:
          - name: backup-storage
            persistentVolumeClaim:
              claimName: backup-pvc
          
          restartPolicy: OnFailure
```

### Security Maintenance

#### Security Patch Management
```bash
#!/bin/bash
# scripts/security-maintenance.sh

# Check for security vulnerabilities
kubectl exec deployment/fabric-api -n fabric-platform -- \
  curl http://localhost:8081/actuator/metrics | grep security

# Update base images
docker pull registry.gitlab.com/fabric-platform/fabric-api:latest
docker pull registry.gitlab.com/fabric-platform/fabric-ui:latest

# Scan for vulnerabilities
trivy image registry.gitlab.com/fabric-platform/fabric-api:latest
trivy image registry.gitlab.com/fabric-platform/fabric-ui:latest

# Update certificates if needed
kubectl get certificates -n fabric-platform -o custom-columns=NAME:.metadata.name,EXPIRES:.status.notAfter

# Rotate secrets
kubectl delete secret database-secret -n fabric-platform
kubectl create secret generic database-secret \
  --from-literal=url="jdbc:oracle:thin:@prod-oracle:1521/ORCLPDB1" \
  --from-literal=username="cm3int_prod" \
  --from-literal=password="NewSecurePassword123!" \
  -n fabric-platform

# Restart deployments to pick up new secrets
kubectl rollout restart deployment/fabric-api -n fabric-platform
```

---

**Document Control:**
- **Version**: 1.0.0
- **Last Updated**: August 19, 2025  
- **Document Owner**: Principal Enterprise Architect
- **Review Schedule**: Quarterly
- **Next Review**: November 19, 2025
- **Classification**: Internal - Confidential

**Approval:**
- **Infrastructure Team**: Approved
- **Security Team**: Approved  
- **Operations Team**: Approved
- **Compliance Team**: Approved

---

*This document provides comprehensive configuration and deployment guidance for the Fabric Platform across all enterprise environments. It serves as the definitive reference for infrastructure setup, deployment procedures, and operational maintenance.*