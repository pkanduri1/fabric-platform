# GitLab Migration Guide - Fabric Platform

## Overview

This guide provides step-by-step instructions for migrating the Fabric Platform project from GitHub to GitLab, while preserving all branches, commit history, and project structure.

## Quick Migration (Using the Automated Script)

### 1. Prerequisites

- GitLab account created
- Git CLI installed and configured
- SSH keys added to GitLab (recommended)
- Access to the current fabric-platform repository

### 2. Create GitLab Project

1. Log in to your GitLab instance
2. Click "New Project" → "Create blank project"
3. Set project name: `fabric-platform`
4. Set visibility level (Private recommended for enterprise)
5. **DO NOT** initialize with README (we'll push existing content)
6. Click "Create project"

### 3. Run the Automated Setup Script

```bash
# Make sure you're in the fabric-platform directory
cd fabric-platform

# Run the setup script with your GitLab project URL
./setup-gitlab.sh git@gitlab.com:your-username/fabric-platform.git

# OR with HTTPS (will prompt for credentials)
./setup-gitlab.sh https://gitlab.com/your-username/fabric-platform.git
```

The script will automatically:
- ✅ Backup your current repository state
- ✅ Add GitLab as a remote
- ✅ Push all branches and tags
- ✅ Create GitLab CI/CD pipeline configuration
- ✅ Set up project documentation
- ✅ Configure default remotes

## Manual Migration (Alternative Approach)

If you prefer to migrate manually or need custom configuration:

### Step 1: Add GitLab Remote

```bash
# Add GitLab as a remote
git remote add gitlab git@gitlab.com:your-username/fabric-platform.git

# Verify remotes
git remote -v
```

### Step 2: Push All Branches

```bash
# Push all branches
git push gitlab --all

# Push all tags
git push gitlab --tags
```

### Step 3: Set GitLab as Default (Optional)

```bash
# Backup current origin
git remote rename origin github-backup

# Set GitLab as new origin
git remote rename gitlab origin

# Update tracking branch
git branch --set-upstream-to=origin/fabric-enhancements fabric-enhancements
```

## Project Structure After Migration

```
fabric-platform/
├── fabric-core/                 # Spring Boot backend
│   ├── fabric-api/              # Main API module with US008 WebSocket
│   ├── fabric-batch/            # Batch processing engine
│   ├── fabric-data-loader/      # Data loading utilities
│   └── fabric-utils/            # Common utilities
├── fabric-ui/                   # React frontend
│   ├── src/                     
│   │   ├── pages/MonitoringDashboard/  # US008 Real-time dashboard
│   │   ├── components/monitoring/     # Dashboard components
│   │   ├── hooks/                     # React hooks including WebSocket
│   │   └── services/                  # API services
│   └── __tests__/               # Comprehensive test suite
├── docs/                        # Project documentation
├── scripts/                     # Utility scripts
├── .gitlab-ci.yml              # CI/CD pipeline (auto-generated)
├── docker-compose.yml          # Development environment
├── Dockerfile                  # Production container
└── GITLAB_SETUP.md            # GitLab-specific documentation
```

## GitLab CI/CD Pipeline

The automated setup creates a comprehensive CI/CD pipeline with the following stages:

### 1. Build Stage
- **Backend**: Maven compilation with Java 17
- **Frontend**: npm build with Node.js 18

### 2. Test Stage
- **Backend**: JUnit tests with JaCoCo coverage
- **Frontend**: Jest tests with coverage reports
- **Services**: PostgreSQL 15 and Redis 7 for integration tests

### 3. Security Stage
- **Dependency Scanning**: Maven and npm vulnerability checks
- **Code Quality**: Static analysis and security best practices

### 4. Package Stage
- **JAR Creation**: Executable Spring Boot application
- **Docker Build**: Container image creation (optional)

### 5. Deploy Stage
- **Staging**: Manual deployment from `fabric-enhancements` branch
- **Production**: Manual deployment from `main` branch

## Environment Configuration

### Required CI/CD Variables

Set these in GitLab Project Settings → CI/CD → Variables:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=fabric_platform
DB_USERNAME=fabric_user
DB_PASSWORD=secure_password_here

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# Security (Generate strong values for production)
JWT_SECRET=your-strong-jwt-secret-key
ENCRYPTION_KEY=your-encryption-key-32-chars

# Monitoring
FABRIC_MONITORING_ENABLED=true
WEBSOCKET_SECURITY_ENABLED=true
AUDIT_LOGGING_ENABLED=true

# Optional: Docker Registry
CI_REGISTRY_IMAGE=registry.gitlab.com/your-username/fabric-platform
DOCKER_USERNAME=your-docker-username
DOCKER_PASSWORD=your-docker-password
```

### Branch Protection Rules

Configure in GitLab Project Settings → Repository → Push Rules:

1. **Main Branch Protection**:
   - Require merge requests
   - Require approvals (1-2 reviewers)
   - Require pipeline success
   - No force push

2. **Fabric-Enhancements Branch**:
   - Allow direct pushes for development
   - Require pipeline success for merges to main

## Development Workflow

### 1. Local Development

```bash
# Clone from GitLab
git clone git@gitlab.com:your-username/fabric-platform.git
cd fabric-platform

# Start development environment
docker-compose up -d postgres redis

# Backend development
cd fabric-core
mvn spring-boot:run -Dspring.profiles.active=local

# Frontend development (new terminal)
cd fabric-ui
npm install
npm start
```

### 2. Feature Development

```bash
# Create feature branch
git checkout fabric-enhancements
git pull origin fabric-enhancements
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit -m "feat: add your feature description"

# Push to GitLab
git push origin feature/your-feature-name

# Create merge request in GitLab UI
```

### 3. Testing Locally

```bash
# Run backend tests
cd fabric-core
mvn test

# Run frontend tests
cd fabric-ui
npm test

# Run with coverage
npm run test:coverage

# Integration tests
docker-compose -f docker-compose.test.yml up --abort-on-container-exit
```

## Monitoring and Observability

### Application Monitoring

The project includes comprehensive monitoring capabilities:

- **US008 Dashboard**: Real-time job monitoring with WebSocket connectivity
- **Health Checks**: Spring Boot Actuator endpoints
- **Metrics**: Prometheus-compatible metrics
- **Logging**: Structured logging with correlation IDs

### GitLab Monitoring

- **Pipeline Monitoring**: Track build/test/deploy success rates
- **Merge Request Analytics**: Review cycle times and approval rates
- **Issue Tracking**: Integrated issue management
- **Security Scanning**: Automatic vulnerability detection

## Troubleshooting

### Common Issues

1. **Push Rejected Error**:
   ```bash
   # Update branch and retry
   git pull origin fabric-enhancements --rebase
   git push origin fabric-enhancements
   ```

2. **Pipeline Failures**:
   - Check CI/CD variables are set correctly
   - Verify database/Redis services are accessible
   - Review pipeline logs in GitLab UI

3. **Authentication Issues**:
   ```bash
   # Verify SSH key
   ssh -T git@gitlab.com
   
   # Or use HTTPS with personal access token
   git remote set-url origin https://oauth2:your-token@gitlab.com/your-username/fabric-platform.git
   ```

4. **Missing Dependencies**:
   ```bash
   # Backend
   cd fabric-core && mvn dependency:resolve
   
   # Frontend
   cd fabric-ui && npm install
   ```

### Support Resources

- **GitLab Documentation**: https://docs.gitlab.com/
- **Project Documentation**: See `docs/` directory
- **Issue Tracker**: Use GitLab Issues for bug reports and feature requests
- **CI/CD Troubleshooting**: GitLab CI/CD → Pipelines → View logs

## Migration Verification Checklist

After migration, verify the following:

- [ ] All branches are present in GitLab
- [ ] Latest commits match GitHub repository
- [ ] CI/CD pipeline runs successfully
- [ ] All environment variables are configured
- [ ] Documentation is updated and accessible
- [ ] Local development environment works
- [ ] Merge request workflow functions properly
- [ ] Security scanning reports are generated
- [ ] Monitoring and health checks work
- [ ] US008 real-time monitoring dashboard functional

## Next Steps

1. **Configure Production Environment**:
   - Set up production GitLab runners
   - Configure deployment targets
   - Set up monitoring and alerting

2. **Team Onboarding**:
   - Add team members to GitLab project
   - Configure role-based permissions
   - Train team on GitLab workflows

3. **Security Hardening**:
   - Review and update security scanning rules
   - Configure compliance frameworks
   - Set up audit logging

4. **Performance Optimization**:
   - Optimize CI/CD pipeline performance
   - Configure caching strategies
   - Monitor resource usage

## Success Metrics

Track these metrics to ensure successful migration:

- ✅ Zero data loss during migration
- ✅ All team members can access and contribute
- ✅ CI/CD pipeline success rate > 95%
- ✅ Development velocity maintained or improved
- ✅ Security scanning and compliance active
- ✅ US008 monitoring dashboard fully operational

---

**Migration completed successfully!** 🎉

Your Fabric Platform project is now fully configured in GitLab with enterprise-grade CI/CD, monitoring, and development workflows.