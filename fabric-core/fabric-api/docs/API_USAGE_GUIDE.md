# Fabric Platform Manual Job Configuration API - Usage Guide

## Overview

This guide provides comprehensive examples and best practices for using the Fabric Platform Manual Job Configuration API v2.0. The API provides enterprise-grade CRUD operations with banking-grade security, compliance, and audit capabilities.

## Table of Contents

1. [Authentication](#authentication)
2. [Role-Based Access Control](#role-based-access-control)
3. [API Endpoints](#api-endpoints)
4. [Request/Response Examples](#requestresponse-examples)
5. [Error Handling](#error-handling)
6. [Security Best Practices](#security-best-practices)
7. [Performance Guidelines](#performance-guidelines)
8. [Troubleshooting](#troubleshooting)

## Authentication

All API endpoints require JWT authentication. Include the Bearer token in the Authorization header:

```http
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

### Obtaining JWT Token

```bash
# Example token request (replace with actual authentication endpoint)
curl -X POST https://auth.truist.com/api/v1/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your.username@truist.com",
    "password": "your_password",
    "domain": "TRUIST"
  }'
```

### Token Response

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "JOB_CREATOR JOB_VIEWER",
  "user": {
    "username": "your.username@truist.com",
    "roles": ["JOB_CREATOR", "JOB_VIEWER"]
  }
}
```

## Role-Based Access Control

The API implements four distinct roles with specific permissions:

### JOB_VIEWER
- **Permissions**: Read-only access
- **Use Cases**: Monitoring, reporting, compliance audits
- **Endpoints**: GET operations only

### JOB_CREATOR
- **Permissions**: Create and read configurations
- **Use Cases**: Business analysts, configuration designers
- **Endpoints**: POST (create), GET (read)

### JOB_MODIFIER
- **Permissions**: Full CRUD operations
- **Use Cases**: Senior developers, configuration managers
- **Endpoints**: POST (create), GET (read), PUT (update), DELETE (deactivate)

### JOB_EXECUTOR
- **Permissions**: Read configurations and execute jobs
- **Use Cases**: Automated systems, batch processing
- **Endpoints**: GET (read), POST (execution endpoints)

## API Endpoints

### Base URL
- **Development**: `https://fabric-api-dev.truist.com`
- **Staging**: `https://fabric-api-staging.truist.com`
- **Production**: `https://fabric-api.truist.com`

### Endpoint Summary

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| POST | `/api/v2/manual-job-config` | Create job configuration | JOB_CREATOR, JOB_MODIFIER |
| GET | `/api/v2/manual-job-config/{id}` | Get specific configuration | Any role |
| GET | `/api/v2/manual-job-config` | List all configurations | Any role |
| PUT | `/api/v2/manual-job-config/{id}` | Update configuration | JOB_MODIFIER |
| DELETE | `/api/v2/manual-job-config/{id}` | Deactivate configuration | JOB_MODIFIER |
| GET | `/api/v2/manual-job-config/statistics` | Get system statistics | Any role |

## Request/Response Examples

### 1. Create Job Configuration

**Request:**
```bash
curl -X POST https://fabric-api.truist.com/api/v2/manual-job-config \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "DAILY_ACCOUNT_RECONCILIATION",
    "jobType": "ETL_BATCH",
    "sourceSystem": "CORE_BANKING",
    "targetSystem": "DATA_WAREHOUSE",
    "description": "Daily reconciliation of account balances between core banking and data warehouse",
    "priority": "HIGH",
    "businessJustification": "Critical for regulatory reporting and customer account accuracy",
    "jobParameters": {
      "batchSize": 5000,
      "connectionTimeout": 60,
      "retryCount": 3,
      "enableEncryption": true,
      "scheduleExpression": "0 0 2 * * ?",
      "notificationEmail": "ops-team@truist.com"
    }
  }'
```

**Response (201 Created):**
```json
{
  "configId": "cfg_core_banking_1691234567890_a1b2c3d4",
  "jobName": "DAILY_ACCOUNT_RECONCILIATION",
  "jobType": "ETL_BATCH",
  "sourceSystem": "CORE_BANKING",
  "targetSystem": "DATA_WAREHOUSE",
  "description": "Daily reconciliation of account balances between core banking and data warehouse",
  "priority": "HIGH",
  "businessJustification": "Critical for regulatory reporting and customer account accuracy",
  "jobParameters": {
    "batchSize": 5000,
    "connectionTimeout": 60,
    "retryCount": 3,
    "enableEncryption": true,
    "scheduleExpression": "0 0 2 * * ?",
    "notificationEmail": "ops-team@truist.com"
  },
  "status": "ACTIVE",
  "createdBy": "john.doe@truist.com",
  "createdDate": "2024-08-13T10:30:45.123Z",
  "versionNumber": 1,
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 2. Get Specific Configuration

**Request:**
```bash
curl -X GET https://fabric-api.truist.com/api/v2/manual-job-config/cfg_core_banking_1691234567890_a1b2c3d4 \
  -H "Authorization: Bearer <jwt-token>"
```

**Response (200 OK):**
```json
{
  "configId": "cfg_core_banking_1691234567890_a1b2c3d4",
  "jobName": "DAILY_ACCOUNT_RECONCILIATION",
  "jobType": "ETL_BATCH",
  "sourceSystem": "CORE_BANKING",
  "targetSystem": "DATA_WAREHOUSE",
  "description": "Daily reconciliation of account balances between core banking and data warehouse",
  "priority": "HIGH",
  "businessJustification": "Critical for regulatory reporting and customer account accuracy",
  "jobParameters": {
    "batchSize": 5000,
    "connectionTimeout": 60,
    "retryCount": 3,
    "enableEncryption": true,
    "scheduleExpression": "0 0 2 * * ?",
    "notificationEmail": "ops-team@truist.com",
    "databasePassword": "***MASKED***"
  },
  "status": "ACTIVE",
  "createdBy": "john.doe@truist.com",
  "createdDate": "2024-08-13T10:30:45.123Z",
  "updatedBy": null,
  "updatedDate": null,
  "versionNumber": 1,
  "correlationId": "550e8400-e29b-41d4-a716-446655440001"
}
```

### 3. List Configurations with Filtering

**Request:**
```bash
curl -X GET "https://fabric-api.truist.com/api/v2/manual-job-config?page=0&size=20&sortBy=jobName&sortDir=asc&jobType=ETL_BATCH&status=ACTIVE" \
  -H "Authorization: Bearer <jwt-token>"
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "configId": "cfg_core_banking_1691234567890_a1b2c3d4",
      "jobName": "DAILY_ACCOUNT_RECONCILIATION",
      "jobType": "ETL_BATCH",
      "sourceSystem": "CORE_BANKING",
      "targetSystem": "DATA_WAREHOUSE",
      "status": "ACTIVE",
      "createdBy": "john.doe@truist.com",
      "createdDate": "2024-08-13T10:30:45.123Z",
      "versionNumber": 1
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20,
  "correlationId": "550e8400-e29b-41d4-a716-446655440002"
}
```

### 4. Update Configuration

**Request:**
```bash
curl -X PUT https://fabric-api.truist.com/api/v2/manual-job-config/cfg_core_banking_1691234567890_a1b2c3d4 \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "DAILY_ACCOUNT_RECONCILIATION",
    "jobType": "ETL_BATCH",
    "sourceSystem": "CORE_BANKING",
    "targetSystem": "DATA_WAREHOUSE",
    "description": "Updated: Daily reconciliation with enhanced error handling",
    "priority": "HIGH",
    "businessJustification": "Critical for regulatory reporting and customer account accuracy",
    "jobParameters": {
      "batchSize": 7500,
      "connectionTimeout": 90,
      "retryCount": 5,
      "enableEncryption": true,
      "scheduleExpression": "0 0 2 * * ?",
      "notificationEmail": "ops-team@truist.com",
      "errorThreshold": 0.01
    }
  }'
```

**Response (200 OK):**
```json
{
  "configId": "cfg_core_banking_1691234567890_a1b2c3d4",
  "jobName": "DAILY_ACCOUNT_RECONCILIATION",
  "jobType": "ETL_BATCH",
  "sourceSystem": "CORE_BANKING",
  "targetSystem": "DATA_WAREHOUSE",
  "description": "Updated: Daily reconciliation with enhanced error handling",
  "priority": "HIGH",
  "businessJustification": "Critical for regulatory reporting and customer account accuracy",
  "jobParameters": {
    "batchSize": 7500,
    "connectionTimeout": 90,
    "retryCount": 5,
    "enableEncryption": true,
    "scheduleExpression": "0 0 2 * * ?",
    "notificationEmail": "ops-team@truist.com",
    "errorThreshold": 0.01
  },
  "status": "ACTIVE",
  "createdBy": "john.doe@truist.com",
  "createdDate": "2024-08-13T10:30:45.123Z",
  "updatedBy": "jane.smith@truist.com",
  "updatedDate": "2024-08-13T14:22:10.456Z",
  "versionNumber": 2,
  "correlationId": "550e8400-e29b-41d4-a716-446655440003"
}
```

### 5. Deactivate Configuration

**Request:**
```bash
curl -X DELETE "https://fabric-api.truist.com/api/v2/manual-job-config/cfg_core_banking_1691234567890_a1b2c3d4?reason=Replaced%20by%20new%20automated%20process" \
  -H "Authorization: Bearer <jwt-token>"
```

**Response (200 OK):**
```json
{
  "configId": "cfg_core_banking_1691234567890_a1b2c3d4",
  "status": "DEACTIVATED",
  "deactivatedBy": "jane.smith@truist.com",
  "deactivatedAt": "2024-08-13T16:45:30.789Z",
  "reason": "Replaced by new automated process",
  "correlationId": "550e8400-e29b-41d4-a716-446655440004"
}
```

### 6. Get System Statistics

**Request:**
```bash
curl -X GET https://fabric-api.truist.com/api/v2/manual-job-config/statistics \
  -H "Authorization: Bearer <jwt-token>"
```

**Response (200 OK):**
```json
{
  "activeConfigurations": 125,
  "inactiveConfigurations": 23,
  "deprecatedConfigurations": 8,
  "totalConfigurations": 156,
  "lastUpdated": "2024-08-13T16:50:15.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440005"
}
```

## Error Handling

### Standard Error Response Format

```json
{
  "timestamp": "2024-08-13T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid job name format",
  "details": {
    "field": "jobName",
    "rejectedValue": "",
    "reason": "Job name cannot be empty and must follow naming conventions"
  },
  "path": "/api/v2/manual-job-config",
  "correlationId": "550e8400-e29b-41d4-a716-446655440006",
  "suggestions": [
    "Use alphanumeric characters and underscores only",
    "Maximum length is 100 characters",
    "Ensure job name is unique within the system"
  ]
}
```

### Common Error Scenarios

#### 401 Unauthorized - Missing Token
```json
{
  "timestamp": "2024-08-13T10:30:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token is required",
  "path": "/api/v2/manual-job-config",
  "correlationId": "550e8400-e29b-41d4-a716-446655440007"
}
```

#### 403 Forbidden - Insufficient Permissions
```json
{
  "timestamp": "2024-08-13T10:30:45.123Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions for this operation",
  "details": {
    "requiredRole": "JOB_MODIFIER",
    "userRoles": ["JOB_VIEWER"],
    "operation": "UPDATE"
  },
  "path": "/api/v2/manual-job-config/cfg_core_banking_1691234567890_a1b2c3d4",
  "correlationId": "550e8400-e29b-41d4-a716-446655440008"
}
```

#### 409 Conflict - Duplicate Resource
```json
{
  "timestamp": "2024-08-13T10:30:45.123Z",
  "status": 409,
  "error": "Conflict",
  "message": "Job configuration with this name already exists",
  "details": {
    "field": "jobName",
    "conflictingValue": "DAILY_ACCOUNT_RECONCILIATION",
    "existingConfigId": "cfg_core_banking_1691234567890_existing"
  },
  "path": "/api/v2/manual-job-config",
  "correlationId": "550e8400-e29b-41d4-a716-446655440009"
}
```

#### 429 Too Many Requests - Rate Limited
```json
{
  "timestamp": "2024-08-13T10:30:45.123Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded",
  "details": {
    "limit": 50,
    "window": "60 seconds",
    "retryAfter": 45
  },
  "path": "/api/v2/manual-job-config",
  "correlationId": "550e8400-e29b-41d4-a716-446655440010"
}
```

## Security Best Practices

### 1. Token Management
- Store JWT tokens securely (never in local storage)
- Implement token refresh before expiration
- Clear tokens on logout or session timeout

### 2. Sensitive Data Handling
- Never log sensitive parameters
- Use HTTPS for all communications
- Implement proper input validation

### 3. Error Handling
- Don't expose sensitive information in error messages
- Log security events with correlation IDs
- Implement rate limiting and circuit breakers

### 4. Audit Compliance
- Include correlation IDs in all requests
- Log all operations with user attribution
- Maintain audit trails for regulatory compliance

## Performance Guidelines

### 1. Response Time Optimization
- Use pagination for large result sets
- Implement caching where appropriate
- Use filtering to reduce payload size

### 2. Rate Limiting
- Implement exponential backoff for retry logic
- Monitor rate limit headers in responses
- Use bulk operations when available

### 3. Connection Management
- Reuse HTTP connections
- Implement connection pooling
- Set appropriate timeouts

## Troubleshooting

### Common Issues

#### Issue: 401 Unauthorized despite valid credentials
**Solution**: Check token expiration and refresh if needed

#### Issue: 403 Forbidden with correct token
**Solution**: Verify user has required role permissions

#### Issue: 429 Rate Limit Exceeded
**Solution**: Implement exponential backoff and reduce request frequency

#### Issue: 500 Internal Server Error
**Solution**: Check correlation ID in logs and contact support

### Support Contacts

- **Technical Support**: fabric-platform-support@truist.com
- **API Documentation**: https://developer.truist.com/fabric-platform
- **Status Page**: https://status.truist.com/fabric-platform
- **Emergency Escalation**: +1-800-TRUIST-1

### Useful Headers for Debugging

```http
X-Correlation-ID: <uuid>  # For request tracking
X-Request-ID: <uuid>      # For request identification  
X-User-Agent: <client-info> # For client identification
```

## SDK and Client Libraries

### Java Spring Boot Client Example

```java
@Service
public class FabricPlatformClient {
    
    private final WebClient webClient;
    private final JwtTokenService tokenService;
    
    public FabricPlatformClient(WebClient.Builder webClientBuilder, 
                               JwtTokenService tokenService) {
        this.webClient = webClientBuilder
            .baseUrl("https://fabric-api.truist.com")
            .build();
        this.tokenService = tokenService;
    }
    
    public Mono<ManualJobConfigResponse> createJobConfiguration(ManualJobConfigRequest request) {
        return webClient
            .post()
            .uri("/api/v2/manual-job-config")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getToken())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatus::isError, this::handleError)
            .bodyToMono(ManualJobConfigResponse.class);
    }
    
    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(body -> Mono.error(new FabricPlatformException(
                response.statusCode(), body)));
    }
}
```

This completes the comprehensive API usage guide with detailed examples, error handling, security best practices, and troubleshooting information.