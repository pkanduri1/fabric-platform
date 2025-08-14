package com.truist.batch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Enterprise OpenAPI/Swagger Configuration for Fabric Platform API Documentation.
 * 
 * Comprehensive API documentation configuration implementing:
 * - Banking-grade security documentation with JWT authentication
 * - Role-based access control (RBAC) documentation
 * - Comprehensive endpoint documentation with examples
 * - Error response documentation and handling
 * - Regulatory compliance documentation (SOX, PCI-DSS)
 * 
 * Documentation Standards:
 * - OpenAPI 3.0.3 specification compliance
 * - Enterprise API documentation best practices
 * - Security-first documentation approach
 * - Banking regulatory requirements documentation
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - API Documentation Implementation
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fabricOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fabric Platform Manual Job Configuration API")
                        .description(getApiDescription())
                        .version("v2.0")
                        .contact(new Contact()
                                .name("Fabric Platform Development Team")
                                .email("fabric-platform-team@truist.com")
                                .url("https://confluence.truist.com/fabric-platform"))
                        .license(new License()
                                .name("Truist Financial Corporation - Internal Use Only")
                                .url("https://truist.com/internal-license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Environment"),
                        new Server()
                                .url("https://fabric-api-dev.truist.com")
                                .description("Development Environment"),
                        new Server()
                                .url("https://fabric-api-staging.truist.com")
                                .description("Staging Environment"),
                        new Server()
                                .url("https://fabric-api.truist.com")
                                .description("Production Environment")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Authentication token required for all API operations. " +
                                           "Token must be obtained from the authentication endpoint and include appropriate roles.")))
                .tags(List.of(
                        new Tag()
                                .name("Manual Job Configuration")
                                .description("Enterprise API for manual job configuration management with banking-grade security and compliance"),
                        new Tag()
                                .name("Authentication")
                                .description("JWT authentication and token management operations"),
                        new Tag()
                                .name("Authorization")
                                .description("Role-based access control (RBAC) and permission management"),
                        new Tag()
                                .name("Audit & Compliance")
                                .description("SOX-compliant audit logging and regulatory reporting endpoints"),
                        new Tag()
                                .name("System Health")
                                .description("System monitoring, health checks, and performance metrics")
                ));
    }

    private String getApiDescription() {
        return """
                # Fabric Platform Manual Job Configuration API v2.0
                
                ## Overview
                Enterprise-grade REST API for manual job configuration management in the Fabric Platform. 
                This API provides comprehensive CRUD operations with banking-grade security, compliance, 
                and audit capabilities.
                
                ## Key Features
                - **JWT Authentication**: Bearer token-based authentication for all operations
                - **Role-Based Access Control (RBAC)**: Four-tier permission system with granular access control
                - **Banking Compliance**: SOX-compliant audit logging and PCI-DSS secure parameter handling
                - **High Performance**: Sub-200ms response times with support for 1000+ concurrent users
                - **Comprehensive Validation**: Input validation, business rule enforcement, and data integrity checks
                - **Data Lineage**: Complete audit trail and data tracking for regulatory compliance
                
                ## Security Model
                
                ### Authentication
                All API endpoints require a valid JWT bearer token in the Authorization header:
                ```
                Authorization: Bearer <jwt-token>
                ```
                
                ### Role-Based Access Control (RBAC)
                The API implements a four-tier RBAC system:
                
                - **JOB_VIEWER**: Read-only access to job configurations and system statistics
                - **JOB_CREATOR**: Create and read job configurations
                - **JOB_MODIFIER**: Create, read, update, and deactivate job configurations
                - **JOB_EXECUTOR**: Execute jobs and read configurations (execution permissions)
                
                ### Security Features
                - Parameter encryption for sensitive data (passwords, API keys, etc.)
                - Rate limiting to prevent abuse (configurable per role)
                - Request/response audit logging with correlation IDs
                - IP address tracking and session management
                - Data masking for sensitive information in API responses
                
                ## Data Model
                
                ### Manual Job Configuration
                Core entity representing a job configuration with the following key attributes:
                - **configId**: Unique identifier (format: cfg_{system}_{timestamp}_{hash})
                - **jobName**: Human-readable job name (must be unique)
                - **jobType**: Category of job (ETL_BATCH, DATA_SYNC, FILE_PROCESSING, etc.)
                - **sourceSystem**: Source system identifier (CORE_BANKING, LOAN_SYSTEM, etc.)
                - **targetSystem**: Target system identifier (DATA_WAREHOUSE, REPORTING_DB, etc.)
                - **jobParameters**: JSON object containing job-specific parameters
                - **status**: Current status (ACTIVE, INACTIVE, DEPRECATED, PENDING_APPROVAL)
                
                ## Compliance & Audit
                
                ### SOX Compliance
                - All operations logged with complete audit trail
                - Change tracking with before/after values
                - User attribution and timestamp tracking
                - Correlation IDs for end-to-end traceability
                
                ### PCI-DSS Compliance
                - Sensitive parameter encryption at rest
                - Data masking in API responses
                - Secure parameter handling throughout the lifecycle
                - Regular security assessments and validations
                
                ## Performance Characteristics
                - **Response Time**: < 200ms average for all operations
                - **Throughput**: Support for 1000+ concurrent users
                - **Availability**: 99.9% uptime SLA
                - **Data Consistency**: ACID compliance with Oracle database
                
                ## Error Handling
                The API uses standard HTTP status codes and provides detailed error information:
                - **400 Bad Request**: Invalid request data or parameters
                - **401 Unauthorized**: Missing or invalid authentication token
                - **403 Forbidden**: Insufficient permissions for the operation
                - **404 Not Found**: Requested resource not found
                - **409 Conflict**: Resource conflict (e.g., duplicate job name)
                - **429 Too Many Requests**: Rate limit exceeded
                - **500 Internal Server Error**: Unexpected server error
                
                All error responses include:
                - Error code and description
                - Correlation ID for troubleshooting
                - Timestamp and request context
                - Suggested remediation actions where applicable
                
                ## Rate Limiting
                API calls are rate-limited based on user role and endpoint:
                - **JOB_VIEWER**: 100 requests per minute
                - **JOB_CREATOR**: 50 requests per minute
                - **JOB_MODIFIER**: 30 requests per minute
                - **JOB_EXECUTOR**: 200 requests per minute (for execution operations)
                
                ## Getting Started
                
                1. **Authentication**: Obtain a JWT token from the authentication endpoint
                2. **Authorization**: Ensure your token includes the appropriate roles
                3. **API Calls**: Include the token in the Authorization header for all requests
                4. **Error Handling**: Implement proper error handling for all response codes
                5. **Rate Limiting**: Respect rate limits and implement exponential backoff
                
                ## Support & Documentation
                - **API Documentation**: This interactive documentation
                - **Developer Portal**: https://developer.truist.com/fabric-platform
                - **Support**: fabric-platform-support@truist.com
                - **Status Page**: https://status.truist.com/fabric-platform
                
                ## Changelog
                - **v2.0**: Enhanced security, RBAC implementation, performance improvements
                - **v1.0**: Initial API release with basic CRUD operations
                """;
    }
}