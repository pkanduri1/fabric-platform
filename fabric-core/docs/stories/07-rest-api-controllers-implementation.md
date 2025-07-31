# User Story: REST API Controllers Implementation

## Story Details
- **Story ID**: FAB-007
- **Title**: Complete REST API Controllers for Data Loading Management
- **Epic**: API Development
- **Status**: Ready for Implementation
- **Sprint**: Sprint 1
- **Story Points**: 8

## User Persona
**API Consumer / Frontend Developer** - Needs programmatic access to data loading functionality through well-designed REST APIs.

## User Story
**As an** API Consumer  
**I want** comprehensive REST API endpoints for managing data loading operations  
**So that** I can integrate data loading functionality into external applications and user interfaces

## Business Value
- **High** - Enables integration with external systems and UI development
- **Integration**: Supports enterprise integration patterns and microservice architecture
- **User Experience**: Enables development of user-friendly management interfaces

## Implementation Status: READY FOR IMPLEMENTATION 🔨

### Required Features
- 🔨 Complete BatchController implementation with data loading endpoints
- 🔨 ConfigurationController with CRUD operations for configurations
- 🔨 TemplateController for template management
- 🔨 Monitoring and status endpoints
- 🔨 Validation and error handling middleware
- 🔨 API documentation with OpenAPI/Swagger
- 🔨 Security integration (authentication/authorization)
- 🔨 Request/response DTO models
- 🔨 Pagination and filtering support
- 🔨 Rate limiting and throttling

## Acceptance Criteria

### AC1: Batch Operations API ✍️
- **Given** a need to manage batch data loading operations
- **When** API endpoints are called
- **Then** users can submit, monitor, and manage batch jobs
- **Endpoints Needed**:
  - `POST /api/v1/batch/jobs` - Submit new data loading job
  - `GET /api/v1/batch/jobs/{jobId}` - Get job status and details
  - `GET /api/v1/batch/jobs` - List jobs with filtering and pagination
  - `DELETE /api/v1/batch/jobs/{jobId}` - Cancel running job
  - `POST /api/v1/batch/jobs/{jobId}/restart` - Restart failed job

### AC2: Configuration Management API ✍️
- **Given** need to manage data loading configurations
- **When** configuration API endpoints are called
- **Then** users can perform CRUD operations on configurations
- **Endpoints Needed**:
  - `GET /api/v1/configs` - List all configurations
  - `GET /api/v1/configs/{configId}` - Get specific configuration
  - `POST /api/v1/configs` - Create new configuration
  - `PUT /api/v1/configs/{configId}` - Update configuration
  - `DELETE /api/v1/configs/{configId}` - Delete configuration
  - `GET /api/v1/configs/{configId}/validation-rules` - Get validation rules

### AC3: Template Management API ✍️
- **Given** need to manage configuration templates
- **When** template API endpoints are called
- **Then** users can manage reusable configuration templates
- **Endpoints Needed**:
  - `GET /api/v1/templates` - List available templates
  - `GET /api/v1/templates/{templateId}` - Get template details
  - `POST /api/v1/templates` - Create new template
  - `POST /api/v1/templates/{templateId}/generate` - Generate configuration from template
  - `POST /api/v1/templates/import` - Import template from file

### AC4: Monitoring and Status API ✍️
- **Given** need to monitor system health and performance
- **When** monitoring endpoints are called
- **Then** system provides comprehensive status information
- **Endpoints Needed**:
  - `GET /api/v1/health` - System health check
  - `GET /api/v1/metrics` - Performance metrics
  - `GET /api/v1/audit/{correlationId}` - Get audit trail
  - `GET /api/v1/thresholds/{configId}` - Get threshold statistics

### AC5: Error Handling and Validation ✍️
- **Given** invalid requests or system errors
- **When** API endpoints are called
- **Then** appropriate error responses are returned with helpful messages
- **Requirements**:
  - Standardized error response format
  - Input validation with detailed field-level errors
  - HTTP status codes following REST conventions
  - Error correlation IDs for tracking

## Technical Implementation Plan

### 1. BatchController Implementation
```java
@RestController
@RequestMapping("/api/v1/batch")
@Validated
public class BatchController {
    
    @Autowired
    private DataLoadOrchestrator orchestrator;
    
    @PostMapping("/jobs")
    public ResponseEntity<BatchJobResponse> submitJob(@Valid @RequestBody BatchJobRequest request) {
        // Submit new data loading job
        // Return job ID and initial status
    }
    
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<BatchJobStatus> getJobStatus(@PathVariable String jobId) {
        // Get current job status and progress
        // Include processing statistics
    }
    
    @GetMapping("/jobs")
    public ResponseEntity<Page<BatchJobSummary>> listJobs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String configId) {
        // List jobs with filtering and pagination
    }
}
```

### 2. ConfigurationController Implementation
```java
@RestController
@RequestMapping("/api/v1/configs")
@Validated
public class ConfigurationController {
    
    @Autowired
    private ConfigurationService configurationService;
    
    @GetMapping
    public ResponseEntity<Page<ConfigurationSummary>> listConfigurations(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String sourceSystem) {
        // List configurations with pagination and filtering
    }
    
    @PostMapping
    public ResponseEntity<ConfigurationResponse> createConfiguration(
        @Valid @RequestBody CreateConfigurationRequest request) {
        // Create new configuration with validation
    }
    
    @PutMapping("/{configId}")
    public ResponseEntity<ConfigurationResponse> updateConfiguration(
        @PathVariable String configId,
        @Valid @RequestBody UpdateConfigurationRequest request) {
        // Update existing configuration
    }
}
```

### 3. Request/Response DTO Models
```java
// Request DTOs
@Data
@Valid
public class BatchJobRequest {
    @NotBlank
    private String configId;
    
    @NotBlank
    private String fileName;
    
    @NotBlank
    private String filePath;
    
    private Map<String, String> parameters;
}

@Data
@Valid
public class CreateConfigurationRequest {
    @NotBlank
    @Size(max = 50)
    private String configId;
    
    @NotBlank
    private String sourceSystem;
    
    @NotBlank
    private String targetTable;
    
    @NotNull
    private String fileFormat;
    
    private String fieldDelimiter;
    private Integer headerRows;
    private Integer maxErrors;
    private String validationEnabled;
}

// Response DTOs
@Data
public class BatchJobResponse {
    private String jobExecutionId;
    private String correlationId;
    private String status;
    private LocalDateTime submittedAt;
    private String message;
}

@Data
public class BatchJobStatus {
    private String jobExecutionId;
    private String correlationId;
    private String configId;
    private String fileName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalRecords;
    private Long processedRecords;
    private Long errorRecords;
    private Double progressPercentage;
    private List<String> currentSteps;
    private String errorMessage;
}
```

### 4. Exception Handling
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message(ex.getMessage())
            .path(getRequestPath())
            .correlationId(generateCorrelationId())
            .fieldErrors(ex.getFieldErrors())
            .build();
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(ConfigurationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConfigurationNotFound(ConfigurationNotFoundException ex) {
        // Handle configuration not found scenarios
    }
    
    @ExceptionHandler(DataLoadException.class)
    public ResponseEntity<ErrorResponse> handleDataLoadException(DataLoadException ex) {
        // Handle data loading specific exceptions
    }
}
```

### 5. API Documentation Configuration
```java
@Configuration
@EnableOpenApi
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Fabric Data Loading Platform API")
                .version("1.0.0")
                .description("REST API for enterprise data loading and validation"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }
}
```

### 6. Security Configuration
```java
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf().disable()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/health").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll())
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            .build();
    }
}
```

## API Design Standards

### REST Conventions
- Use HTTP methods appropriately (GET, POST, PUT, DELETE)
- Follow RESTful resource naming conventions
- Use appropriate HTTP status codes
- Include correlation IDs in responses
- Support content negotiation (JSON primary)

### Request/Response Format
```json
// Standard Error Response
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/configs",
  "correlationId": "req-12345678",
  "fieldErrors": [
    {
      "field": "configId",
      "rejectedValue": "",
      "message": "Configuration ID is required"
    }
  ]
}

// Standard Success Response
{
  "data": { /* response data */ },
  "metadata": {
    "correlationId": "req-12345678",
    "timestamp": "2024-01-15T10:30:00Z",
    "version": "1.0.0"
  }
}
```

### Pagination Format
```json
{
  "content": [ /* page data */ ],
  "pageable": {
    "page": 0,
    "size": 20,
    "sort": "createdDate,desc"
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false,
  "numberOfElements": 20
}
```

## Testing Strategy

### Unit Tests
- Controller method testing with MockMvc
- Request/response DTO validation testing
- Exception handler testing
- Security configuration testing

### Integration Tests
- End-to-end API testing with TestRestTemplate
- Database integration testing
- Security integration testing
- Performance testing under load

### API Documentation Testing
- Swagger/OpenAPI specification validation
- Contract testing with consumer teams
- API versioning compatibility testing

## Performance Requirements
- **Response Time**: < 200ms for simple CRUD operations
- **Throughput**: Support 1000+ concurrent API calls
- **Availability**: 99.9% uptime with proper error handling
- **Rate Limiting**: 1000 requests/minute per client

## Security Requirements
- OAuth 2.0/JWT authentication
- Role-based authorization (RBAC)
- Input validation and sanitization
- SQL injection prevention
- Cross-site scripting (XSS) protection
- Rate limiting and DDoS protection

## Tasks/Subtasks

### Sprint 1 Tasks
1. **Implement BatchController** (3 points)
   - Submit job endpoint
   - Job status and monitoring endpoints
   - Job management operations

2. **Implement ConfigurationController** (3 points)
   - CRUD operations for configurations
   - Validation rule management
   - Configuration validation

3. **Implement TemplateController** (2 points)
   - Template CRUD operations
   - Template generation functionality
   - Import/export capabilities

4. **API Documentation and Security** (2 points)
   - OpenAPI/Swagger configuration
   - Security implementation
   - Error handling framework

### Sprint 2 Tasks (Future)
1. Advanced filtering and search capabilities
2. Bulk operations support
3. Real-time status updates with WebSocket
4. Advanced monitoring and metrics endpoints

## Dependencies
- Spring Boot Web framework
- Spring Security for authentication/authorization
- SpringDoc OpenAPI for API documentation
- Jackson for JSON processing
- Spring Data for pagination support

## Files to Create/Modify
- `/fabric-api/src/main/java/com/truist/batch/controller/BatchController.java`
- `/fabric-api/src/main/java/com/truist/batch/controller/ConfigurationController.java`
- `/fabric-api/src/main/java/com/truist/batch/controller/TemplateController.java`
- DTO classes in `/fabric-api/src/main/java/com/truist/batch/dto/`
- Exception handling classes
- Security configuration classes
- OpenAPI configuration

---
**Story Status**: Ready for Implementation
**Estimated Effort**: 2 sprints (8 story points primary + 5 story points enhancements)
**Priority**: High - Required for system usability and integration