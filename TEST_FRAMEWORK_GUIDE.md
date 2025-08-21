# Fabric Platform Testing Framework Guide

## Overview

This comprehensive testing framework provides automated testing capabilities for the Fabric Platform, including unit tests, integration tests, security tests, and end-to-end testing with performance and accessibility validation.

## Table of Contents

1. [Testing Architecture](#testing-architecture)
2. [Backend Testing (TestNG)](#backend-testing-testng)
3. [Frontend E2E Testing (Selenium/WebdriverIO)](#frontend-e2e-testing-seleniumwebdriverio)
4. [Performance Testing](#performance-testing)
5. [Accessibility Testing](#accessibility-testing)
6. [Security Testing](#security-testing)
7. [Test Execution](#test-execution)
8. [CI/CD Integration](#cicd-integration)
9. [Test Data Management](#test-data-management)
10. [Reporting and Analysis](#reporting-and-analysis)
11. [Best Practices](#best-practices)
12. [Troubleshooting](#troubleshooting)

## Testing Architecture

```
fabric-platform-testing/
├── Backend Testing (TestNG)
│   ├── Unit Tests (Service, DAO, Utils)
│   ├── Integration Tests (API, Database)
│   ├── Security Tests (JWT, RBAC)
│   └── Performance Tests (Load, Stress)
├── Frontend Testing (Selenium/WebdriverIO)
│   ├── E2E Tests (User Workflows)
│   ├── Page Object Model
│   ├── Cross-Browser Testing
│   └── Component Integration Tests
├── Specialized Testing
│   ├── Performance Testing (Lighthouse)
│   ├── Accessibility Testing (axe-core)
│   └── Security Testing (OWASP)
└── Test Infrastructure
    ├── Test Data Management
    ├── Environment Configuration
    ├── Reporting & Analytics
    └── CI/CD Integration
```

## Backend Testing (TestNG)

### Directory Structure

```
src/test/java/com/truist/batch/
├── unit/
│   ├── service/           # Service layer tests
│   ├── dao/              # Data access tests
│   ├── controller/       # Controller tests
│   ├── security/         # Security unit tests
│   └── utils/            # Utility tests
├── integration/
│   ├── api/              # REST API tests
│   ├── database/         # Database integration
│   ├── websocket/        # WebSocket tests
│   └── batch/            # Batch processing tests
├── security/
│   ├── authentication/   # JWT, LDAP tests
│   ├── authorization/    # RBAC tests
│   └── encryption/       # Data encryption tests
├── performance/          # Performance tests
├── testutils/           # Test utilities
├── fixtures/            # Test data fixtures
└── builders/            # Test data builders
```

### Running Backend Tests

```bash
# Run all unit tests
cd fabric-core/fabric-api
mvn test

# Run specific test groups
mvn test -Dgroups="unit,service"
mvn test -Dgroups="integration,api"
mvn test -Dgroups="security,authentication"

# Run with specific profiles
mvn test -Dspring.profiles.active=test
mvn test -Dspring.profiles.active=integration-test

# Run integration tests
mvn verify -Pfailsafe

# Generate test reports
mvn test jacoco:report
```

### Test Configuration

#### Unit Test Configuration (`application-test.yml`)
- H2 in-memory database
- Mock external dependencies
- Disabled security for faster execution
- Test-specific logging configuration

#### Integration Test Configuration (`application-integration-test.yml`)
- Testcontainers Oracle database
- Real security configuration
- External service integration
- Performance monitoring enabled

### Sample Test Implementation

```java
@Test(groups = {"unit", "service", "configuration"})
public class ConfigurationServiceTest extends BaseTestNGTest {
    
    @Mock
    private BatchConfigurationDao batchConfigurationDao;
    
    @InjectMocks
    private ConfigurationService configurationService;
    
    @Test(description = "Should create new configuration successfully")
    public void testCreateConfiguration_Success() {
        // Given
        ManualJobConfigRequest request = TestDataBuilder.manualJobConfig()
            .withJobName("Test Configuration")
            .withSourceSystem("HR")
            .buildRequest();
        
        // When
        String configId = configurationService.createConfiguration(
            request, "test-user", "correlation-123");
        
        // Then
        assertThat(configId).isNotNull();
        verify(batchConfigurationDao).createConfiguration(
            eq(request), eq("test-user"), eq("correlation-123"));
    }
}
```

## Frontend E2E Testing (Selenium/WebdriverIO)

### Directory Structure

```
fabric-ui/test/
├── e2e/
│   ├── specs/               # Test specifications
│   └── features/            # Feature files (optional)
├── pageobjects/
│   ├── pages/              # Page object classes
│   ├── components/         # Component objects
│   └── BasePage.js         # Base page functionality
├── utils/                  # Test utilities
├── fixtures/               # Test data
└── reports/               # Test reports
    ├── screenshots/       # Failure screenshots
    ├── allure-results/    # Allure test results
    └── junit/            # JUnit XML reports
```

### Running Frontend E2E Tests

```bash
# Install dependencies
cd fabric-ui
npm install

# Update WebDriver
npm run webdriver:update

# Run E2E tests
npm run test:e2e

# Run with specific browsers
npm run test:e2e:chrome
npm run test:e2e:firefox

# Run headless
npm run test:e2e:headless

# Run accessibility tests
npm run test:accessibility

# Run performance tests
npm run test:performance

# Full test suite
npm run test:full
```

### WebdriverIO Configuration

The `wdio.conf.js` file provides:
- Multi-browser support (Chrome, Firefox, Edge)
- Parallel test execution
- Comprehensive reporting (Allure, JUnit)
- Screenshot capture on failures
- Custom timeouts and retry logic

### Page Object Model Example

```javascript
class TemplateConfigurationPage extends BasePage {
    get templateDropdown() { 
        return $('[data-testid="template-selector"]'); 
    }
    
    selectTemplate(templateName) {
        this.waitForClickable(this.templateDropdown);
        this.safeClick(this.templateDropdown);
        
        const option = this.templateOptions.find(opt => 
            opt.getText().includes(templateName)
        );
        this.safeClick(option);
        this.waitForLoadingComplete();
    }
}
```

## Performance Testing

### Lighthouse Integration

The performance testing framework uses Google Lighthouse for comprehensive performance analysis:

```bash
# Run performance tests
cd fabric-ui
npm run test:performance

# Performance test with custom configuration
node scripts/performance-test.js
```

### Performance Metrics Tracked

1. **Core Web Vitals**
   - First Contentful Paint (FCP)
   - Largest Contentful Paint (LCP)
   - Cumulative Layout Shift (CLS)
   - Time to Interactive (TTI)

2. **Load Performance**
   - Page load time
   - Bundle size analysis
   - Network resource optimization
   - Caching effectiveness

3. **Thresholds**
   - Performance Score: ≥85
   - Accessibility Score: ≥90
   - Best Practices Score: ≥85
   - SEO Score: ≥80

### Performance Report Example

```json
{
  "url": "http://localhost:3000/template-configuration",
  "scores": {
    "performance": 89,
    "accessibility": 94,
    "bestPractices": 87,
    "seo": 82
  },
  "metrics": {
    "firstContentfulPaint": 1200,
    "largestContentfulPaint": 2100,
    "timeToInteractive": 3400,
    "totalLoadTime": 2800
  }
}
```

## Accessibility Testing

### axe-core Integration

Comprehensive accessibility testing using industry-standard axe-core library:

```bash
# Run accessibility tests
npm run test:accessibility

# Custom accessibility test
node scripts/accessibility-test.js
```

### Accessibility Standards

- **WCAG 2.1 AA Compliance**
- **Section 508 Compliance**
- **Best Practice Guidelines**

### Accessibility Checks

1. **Automated Testing**
   - Color contrast validation
   - Keyboard navigation testing
   - Screen reader compatibility
   - ARIA attribute validation

2. **Manual Testing Guidelines**
   - Tab order verification
   - Focus management
   - Alternative text validation
   - Form label associations

### Accessibility Report Structure

```html
<div class="violation critical">
    <h4>Form elements must have labels</h4>
    <p>Rule: label</p>
    <p>Impact: critical</p>
    <p>Affected Elements: 3</p>
    <p>Fix: Ensure all form elements have associated labels</p>
</div>
```

## Security Testing

### JWT Authentication Testing

```java
@Test(groups = {"security", "authentication", "jwt"})
public class JwtAuthenticationTest extends BaseTestNGTest {
    
    @Test(description = "Should generate valid JWT access token")
    public void testGenerateAccessToken_Success() {
        // Test JWT token generation, validation, and expiration
    }
    
    @Test(description = "Should reject expired JWT token")
    public void testValidateToken_Expired() {
        // Test token expiration handling
    }
}
```

### Security Test Coverage

1. **Authentication Tests**
   - JWT token validation
   - Token expiration handling
   - Refresh token functionality
   - Session management

2. **Authorization Tests**
   - Role-based access control (RBAC)
   - Resource-level permissions
   - API endpoint security
   - Data access restrictions

3. **Input Validation Tests**
   - SQL injection prevention
   - XSS protection
   - Input sanitization
   - Parameter validation

## Test Execution

### Local Development

```bash
# Backend tests
cd fabric-core/fabric-api
mvn clean test

# Frontend E2E tests
cd fabric-ui
npm run test:e2e

# Full test suite (backend + frontend)
./scripts/run-full-test-suite.sh
```

### Test Profiles

#### Backend Test Profiles
- `test`: Unit tests with H2 database
- `integration-test`: Integration tests with Testcontainers
- `performance-test`: Performance and load testing

#### Frontend Test Environments
- `development`: Local development testing
- `staging`: Pre-production testing
- `ci`: Continuous integration testing

### Test Data Management

#### Test Data Builders

```java
// Java Test Data Builder
ManualJobConfigRequest request = TestDataBuilder.manualJobConfig()
    .withJobName("Test Configuration")
    .withSourceSystem("HR")
    .withTransactionCode("200")
    .withStatus("ACTIVE")
    .buildRequest();
```

#### Test Fixtures

```javascript
// JavaScript Test Fixtures
const testConfiguration = {
    templateName: 'HR Employee Template',
    sourceSystem: 'HR',
    masterQuery: 'Employee Data Query',
    fieldMappings: [
        {
            sourceField: 'emp_id',
            targetField: 'employee_id',
            dataType: 'STRING',
            required: true
        }
    ]
};
```

## CI/CD Integration

### Jenkins Pipeline Integration

```groovy
pipeline {
    agent any
    
    stages {
        stage('Backend Tests') {
            steps {
                sh 'cd fabric-core/fabric-api && mvn clean test'
                publishTestResults testResultsPattern: '**/target/surefire-reports/*.xml'
            }
        }
        
        stage('Frontend E2E Tests') {
            steps {
                sh 'cd fabric-ui && npm run test:e2e:headless'
                publishTestResults testResultsPattern: '**/test/reports/junit/*.xml'
            }
        }
        
        stage('Performance Tests') {
            steps {
                sh 'cd fabric-ui && npm run test:performance'
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'fabric-ui/test/reports/performance',
                    reportFiles: '*.html',
                    reportName: 'Performance Report'
                ])
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: '**/test/reports/**/*', allowEmptyArchive: true
            publishTestResults testResultsPattern: '**/test-results.xml'
        }
    }
}
```

### GitHub Actions Integration

```yaml
name: Comprehensive Test Suite

on: [push, pull_request]

jobs:
  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Backend Tests
        run: |
          cd fabric-core/fabric-api
          mvn clean test
      - name: Publish Test Results
        uses: dorny/test-reporter@v1
        with:
          name: Backend Test Results
          path: '**/target/surefire-reports/*.xml'
          reporter: java-junit

  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Install Dependencies
        run: |
          cd fabric-ui
          npm ci
      - name: Run E2E Tests
        run: |
          cd fabric-ui
          npm run test:e2e:headless
      - name: Upload Screenshots
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: test-screenshots
          path: fabric-ui/test/reports/screenshots/
```

## Reporting and Analysis

### Test Reports Generated

1. **Backend Reports**
   - JaCoCo Code Coverage Reports
   - TestNG HTML Reports
   - Surefire XML Reports
   - Performance Metrics

2. **Frontend Reports**
   - Allure Test Reports
   - WebDriver Screenshots
   - Performance Lighthouse Reports
   - Accessibility axe Reports

3. **Consolidated Reports**
   - Test Execution Summary
   - Code Coverage Dashboard
   - Performance Benchmarks
   - Accessibility Compliance

### Report Locations

```
test/reports/
├── backend/
│   ├── jacoco/           # Code coverage
│   ├── testng/           # TestNG reports
│   └── performance/      # Backend performance
├── frontend/
│   ├── allure-results/   # Allure raw data
│   ├── allure-report/    # Allure HTML report
│   ├── screenshots/      # Test screenshots
│   ├── performance/      # Lighthouse reports
│   └── accessibility/    # axe reports
└── consolidated/
    ├── test-summary.html # Overall summary
    └── metrics.json      # Test metrics
```

## Best Practices

### Test Organization

1. **Follow the Test Pyramid**
   - 70% Unit Tests (Fast, Isolated)
   - 20% Integration Tests (Database, API)
   - 10% E2E Tests (Full User Workflows)

2. **Test Naming Conventions**
   ```java
   // Format: testMethodName_Scenario_ExpectedResult
   public void testCreateConfiguration_ValidRequest_ReturnsConfigId()
   public void testCreateConfiguration_NullRequest_ThrowsException()
   ```

3. **Test Data Management**
   - Use test data builders for consistent object creation
   - Implement proper test data cleanup
   - Isolate test data between test methods

4. **Assertion Strategy**
   ```java
   // Use descriptive assertions
   assertThat(result)
       .isNotNull()
       .extracting("configId", "jobName", "status")
       .containsExactly(expectedConfigId, expectedJobName, "ACTIVE");
   ```

### Performance Best Practices

1. **Test Execution Speed**
   - Parallel test execution where possible
   - Mock external dependencies in unit tests
   - Use lightweight test databases (H2)
   - Optimize test data setup/teardown

2. **Resource Management**
   - Proper cleanup of test resources
   - Connection pool management
   - Memory usage monitoring
   - Browser session management

### Security Testing Best Practices

1. **Authentication Testing**
   - Test all authentication flows
   - Validate token expiration handling
   - Test session management
   - Verify logout functionality

2. **Authorization Testing**
   - Test role-based access control
   - Validate resource-level permissions
   - Test privilege escalation prevention
   - Verify data access restrictions

## Troubleshooting

### Common Issues and Solutions

#### Backend Test Issues

1. **Database Connection Problems**
   ```bash
   # Solution: Check test profile configuration
   mvn test -Dspring.profiles.active=test
   ```

2. **Test Dependencies**
   ```bash
   # Solution: Clean and rebuild
   mvn clean install -DskipTests
   mvn test
   ```

3. **Testcontainers Issues**
   ```bash
   # Solution: Ensure Docker is running
   docker --version
   mvn test -Dspring.profiles.active=integration-test
   ```

#### Frontend Test Issues

1. **WebDriver Issues**
   ```bash
   # Solution: Update WebDriver
   npm run webdriver:update
   
   # Check browser versions
   google-chrome --version
   firefox --version
   ```

2. **Test Timeout Issues**
   ```javascript
   // Increase timeout in wdio.conf.js
   waitforTimeout: 30000,
   connectionRetryTimeout: 120000
   ```

3. **Element Not Found**
   ```javascript
   // Use explicit waits
   element.waitForExist({ timeout: 10000 });
   element.waitForClickable({ timeout: 10000 });
   ```

#### Performance Test Issues

1. **Lighthouse Failures**
   ```bash
   # Solution: Check if app is running
   curl http://localhost:3000
   
   # Run with debug logging
   DEBUG=* npm run test:performance
   ```

2. **Memory Issues**
   ```bash
   # Solution: Increase Node memory limit
   NODE_OPTIONS="--max-old-space-size=4096" npm run test:performance
   ```

### Debug Mode

#### Backend Debugging
```bash
# Enable debug logging
mvn test -Dlogging.level.com.truist.batch=DEBUG

# Debug specific test
mvn test -Dtest=ConfigurationServiceTest#testCreateConfiguration_Success -Dmaven.surefire.debug
```

#### Frontend Debugging
```bash
# Run tests in debug mode
npm run test:e2e -- --debug

# Keep browser open after test
npm run test:e2e -- --debug --bail
```

### Performance Monitoring

1. **Test Execution Time Monitoring**
   - Track test execution duration
   - Identify slow tests
   - Optimize test performance

2. **Resource Usage Monitoring**
   - Memory consumption during tests
   - Database connection usage
   - Browser resource utilization

## Conclusion

This comprehensive testing framework provides robust coverage for the Fabric Platform, ensuring high-quality, secure, and performant software delivery. Regular execution of this test suite helps maintain code quality, catch regressions early, and ensure compliance with accessibility and security standards.

For questions or contributions to the testing framework, please refer to the project documentation or contact the development team.

---

**Generated by Fabric Platform Testing Framework v1.0.0**  
**Last Updated: {{ current_date }}**  
**Maintained by: Senior Full Stack Developer Agent**