# Consumer Default ETL Pipeline - Enterprise Architecture Design

## Executive Summary

This document presents the comprehensive technical implementation plan for a consumer default ETL pipeline designed for enterprise banking environments. The solution addresses four critical user stories through a microservices-based architecture that ensures regulatory compliance, high performance, and operational excellence.

## 1. High-Level Architecture Design

### 1.1 System Overview

The Consumer Default ETL Pipeline follows a microservices architecture pattern with clear separation of concerns, ensuring scalability, maintainability, and regulatory compliance.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            API Gateway Layer                                    │
│                     (Spring Cloud Gateway + OAuth2)                            │
└─────────────────────────────────────┬───────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────┴───────────────────────────────────────────┐
│                        Microservices Layer                                     │
├─────────────────┬─────────────────┬─────────────────┬─────────────────────────┤
│   File Ingestion│  Configuration  │  Transformation │    Data Quality         │
│    Service      │    Service      │    Service      │    Service              │
│                 │                 │                 │                         │
│ - SQL*Loader    │ - Field Mapping │ - Business Rules│ - Validation Rules      │
│ - Control Files │ - Versioning    │ - Calculations  │ - Error Handling        │
│ - File Metadata │ - Runtime Config│ - Risk Engine   │ - Audit Logging         │
└─────────────────┴─────────────────┴─────────────────┴─────────────────────────┘
                                      │
┌─────────────────────────────────────┴───────────────────────────────────────────┐
│                         Event Streaming Layer                                  │
│                    (Apache Kafka + Schema Registry)                           │
└─────────────────────────────────────┬───────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────┴───────────────────────────────────────────┐
│                          Data Layer                                            │
├─────────────────┬─────────────────┬─────────────────┬─────────────────────────┤
│   Staging DB    │   Operational   │   Data Lineage  │    Configuration        │
│   (Oracle)      │   Data Store    │   Store         │    Store                │
│                 │   (Oracle)      │   (MongoDB)     │    (MongoDB)            │
│ - Raw Data      │ - Processed     │ - Audit Trail   │ - Field Mappings        │
│ - Partitioned   │ - Default Risk  │ - Data Flow     │ - Business Rules        │
│ - Indexed       │ - Compliance    │ - Lineage Graph │ - Transformation Logic  │
└─────────────────┴─────────────────┴─────────────────┴─────────────────────────┘
```

### 1.2 Technology Stack Alignment

**Core Framework:**
- Java 21 LTS (Latest enterprise standard)
- Spring Boot 3.2+ with Spring Cloud 2023.0.x
- Spring Batch 5.x for ETL processing
- Spring Security 6.x with OAuth2/JWT

**Data Platform:**
- Oracle Database 23c (Primary data store)
- MongoDB 7.0 (Configuration and lineage metadata)
- Apache Kafka 3.6+ (Event streaming)
- Redis 7.x (Caching and session management)

**Infrastructure:**
- RHEL 9.x (Operating system)
- Kubernetes 1.28+ (Container orchestration)
- AWS EKS (Managed Kubernetes)
- HashiCorp Vault (Secrets management)

### 1.3 Integration Patterns

**Event-Driven Architecture:**
- Asynchronous processing using Apache Kafka
- Event sourcing for audit trail compliance
- CQRS pattern for read/write separation

**API-First Design:**
- RESTful APIs with OpenAPI 3.0 specifications
- GraphQL for complex data queries
- gRPC for internal service communication

**Circuit Breaker Pattern:**
- Resilience4j for fault tolerance
- Timeout and retry mechanisms
- Graceful degradation strategies

## 2. Database Architecture

### 2.1 Staging Database Schema Design

**Core Staging Tables:**

```sql
-- Consumer Default Staging Table with Partitioning
CREATE TABLE CONSUMER_DEFAULT_STAGING (
    STAGING_ID          NUMBER(19) PRIMARY KEY,
    BATCH_ID            VARCHAR2(50) NOT NULL,
    SOURCE_SYSTEM       VARCHAR2(50) NOT NULL,
    FILE_NAME           VARCHAR2(255) NOT NULL,
    RECORD_SEQUENCE     NUMBER(10) NOT NULL,
    CONSUMER_ID         VARCHAR2(50),
    ACCOUNT_NUMBER      VARCHAR2(50),
    DEFAULT_DATE        DATE,
    DEFAULT_AMOUNT      NUMBER(15,2),
    DEFAULT_TYPE        VARCHAR2(20),
    RECOVERY_STATUS     VARCHAR2(20),
    RISK_RATING         VARCHAR2(10),
    RAW_DATA            CLOB,
    CREATED_TIMESTAMP   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PROCESSED_FLAG      CHAR(1) DEFAULT 'N',
    ERROR_FLAG          CHAR(1) DEFAULT 'N',
    ERROR_MESSAGE       VARCHAR2(4000),
    DATA_LINEAGE_ID     VARCHAR2(50)
) PARTITION BY RANGE (CREATED_TIMESTAMP) (
    PARTITION P_202501 VALUES LESS THAN (DATE '2025-02-01'),
    PARTITION P_202502 VALUES LESS THAN (DATE '2025-03-01'),
    PARTITION P_202503 VALUES LESS THAN (DATE '2025-04-01')
    -- Automated monthly partitioning
);

-- Field Mapping Configuration Table
CREATE TABLE FIELD_MAPPING_CONFIG (
    MAPPING_ID          NUMBER(19) PRIMARY KEY,
    SOURCE_SYSTEM       VARCHAR2(50) NOT NULL,
    FILE_TYPE           VARCHAR2(50) NOT NULL,
    VERSION             VARCHAR2(20) NOT NULL,
    SOURCE_FIELD        VARCHAR2(100) NOT NULL,
    TARGET_FIELD        VARCHAR2(100) NOT NULL,
    DATA_TYPE           VARCHAR2(50),
    TRANSFORMATION_RULE VARCHAR2(4000),
    VALIDATION_RULE     VARCHAR2(4000),
    IS_MANDATORY        CHAR(1) DEFAULT 'N',
    DEFAULT_VALUE       VARCHAR2(255),
    EFFECTIVE_DATE      DATE NOT NULL,
    EXPIRY_DATE         DATE,
    CREATED_BY          VARCHAR2(50) NOT NULL,
    CREATED_TIMESTAMP   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    MODIFIED_BY         VARCHAR2(50),
    MODIFIED_TIMESTAMP  TIMESTAMP
);

-- Data Quality Validation Results
CREATE TABLE DATA_QUALITY_RESULTS (
    VALIDATION_ID       NUMBER(19) PRIMARY KEY,
    STAGING_ID          NUMBER(19) REFERENCES CONSUMER_DEFAULT_STAGING(STAGING_ID),
    RULE_NAME           VARCHAR2(100) NOT NULL,
    RULE_TYPE           VARCHAR2(50) NOT NULL,
    VALIDATION_STATUS   VARCHAR2(20) NOT NULL,
    ERROR_CODE          VARCHAR2(50),
    ERROR_DESCRIPTION   VARCHAR2(4000),
    FIELD_NAME          VARCHAR2(100),
    FIELD_VALUE         VARCHAR2(4000),
    SUGGESTED_VALUE     VARCHAR2(4000),
    CREATED_TIMESTAMP   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Audit Trail Table for Regulatory Compliance
CREATE TABLE ETL_AUDIT_TRAIL (
    AUDIT_ID            NUMBER(19) PRIMARY KEY,
    BATCH_ID            VARCHAR2(50) NOT NULL,
    OPERATION_TYPE      VARCHAR2(50) NOT NULL,
    ENTITY_TYPE         VARCHAR2(50) NOT NULL,
    ENTITY_ID           VARCHAR2(50),
    OLD_VALUES          CLOB,
    NEW_VALUES          CLOB,
    CHANGED_BY          VARCHAR2(50) NOT NULL,
    CHANGE_TIMESTAMP    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    IP_ADDRESS          VARCHAR2(45),
    SESSION_ID          VARCHAR2(100),
    REGULATORY_FLAG     CHAR(1) DEFAULT 'Y'
);
```

### 2.2 Partitioning Strategy

**Time-Based Partitioning:**
- Monthly partitions for staging tables
- Automatic partition maintenance using Oracle Scheduler
- Partition pruning for query optimization
- Compressed older partitions for storage efficiency

**Hash Partitioning for High-Volume Tables:**
- Consumer ID-based hash partitioning for parallel processing
- Subpartitioning by date for optimal query performance

### 2.3 Indexing Strategy

```sql
-- Performance Indexes
CREATE INDEX IDX_STAGING_BATCH_ID ON CONSUMER_DEFAULT_STAGING(BATCH_ID);
CREATE INDEX IDX_STAGING_CONSUMER_ID ON CONSUMER_DEFAULT_STAGING(CONSUMER_ID);
CREATE INDEX IDX_STAGING_DEFAULT_DATE ON CONSUMER_DEFAULT_STAGING(DEFAULT_DATE);
CREATE INDEX IDX_STAGING_PROCESSED ON CONSUMER_DEFAULT_STAGING(PROCESSED_FLAG, ERROR_FLAG);

-- Composite Index for Complex Queries
CREATE INDEX IDX_STAGING_COMPOSITE ON CONSUMER_DEFAULT_STAGING(
    SOURCE_SYSTEM, PROCESSED_FLAG, CREATED_TIMESTAMP
) LOCAL;

-- Function-Based Index for JSON Queries
CREATE INDEX IDX_STAGING_JSON_DATA ON CONSUMER_DEFAULT_STAGING(
    JSON_VALUE(RAW_DATA, '$.accountType')
);
```

### 2.4 Data Lineage Architecture

**MongoDB Collections for Metadata:**

```javascript
// Data Lineage Collection
{
  "_id": ObjectId("..."),
  "lineageId": "DL_20250101_001",
  "batchId": "BATCH_20250101_001",
  "sourceSystem": "CORE_BANKING",
  "fileName": "consumer_defaults_20250101.dat",
  "recordCount": 150000,
  "dataFlow": [
    {
      "stage": "INGESTION",
      "timestamp": ISODate("2025-01-01T10:00:00Z"),
      "service": "file-ingestion-service",
      "status": "COMPLETED",
      "recordsProcessed": 150000,
      "errors": 0
    },
    {
      "stage": "VALIDATION",
      "timestamp": ISODate("2025-01-01T10:15:00Z"),
      "service": "data-quality-service",
      "status": "COMPLETED",
      "recordsProcessed": 150000,
      "errors": 245,
      "warnings": 1023
    }
  ],
  "transformations": [
    {
      "fieldName": "defaultAmount",
      "sourceValue": "1500.00",
      "targetValue": "1500.00",
      "transformationRule": "CURRENCY_STANDARDIZATION",
      "appliedAt": ISODate("2025-01-01T10:20:00Z")
    }
  ],
  "complianceMarkers": {
    "soxCompliant": true,
    "pciCompliant": true,
    "dataRetentionPeriod": "7_YEARS",
    "encryptionApplied": true
  }
}
```

## 3. Microservices Design

### 3.1 Service Boundaries and Responsibilities

**File Ingestion Service (US003)**
- **Responsibility:** SQL*Loader integration and file processing
- **Key Features:**
  - Automated control file generation
  - File format detection and validation
  - Batch processing orchestration
  - Error handling and retry mechanisms

**Configuration Service (US002)**
- **Responsibility:** Dynamic field mapping and transformation rules
- **Key Features:**
  - Runtime configuration management
  - Version control for mapping rules
  - A/B testing for configuration changes
  - Configuration validation and deployment

**Transformation Service (US004)**
- **Responsibility:** Business rule engine and data transformations
- **Key Features:**
  - Complex default calculations
  - Risk assessment algorithms
  - Integration with core banking systems
  - Regulatory compliance calculations

**Data Quality Service**
- **Responsibility:** Data validation and quality assurance
- **Key Features:**
  - Multi-level validation rules
  - Data cleansing and standardization
  - Quality metrics and reporting
  - Anomaly detection

### 3.2 API Design Patterns and Contracts

**File Ingestion Service API:**

```yaml
openapi: 3.0.3
info:
  title: File Ingestion Service API
  version: 1.0.0
  description: Consumer Default File Ingestion Service
paths:
  /api/v1/files/ingest:
    post:
      summary: Initiate file ingestion process
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                fileName:
                  type: string
                  example: "consumer_defaults_20250101.dat"
                sourceSystem:
                  type: string
                  example: "CORE_BANKING"
                fileFormat:
                  type: string
                  enum: [FIXED_WIDTH, DELIMITED, JSON, XML]
                priority:
                  type: string
                  enum: [LOW, MEDIUM, HIGH, CRITICAL]
                batchId:
                  type: string
                  example: "BATCH_20250101_001"
      responses:
        '202':
          description: File ingestion initiated
          content:
            application/json:
              schema:
                type: object
                properties:
                  jobId:
                    type: string
                  status:
                    type: string
                    enum: [INITIATED, IN_PROGRESS, COMPLETED, FAILED]
                  estimatedCompletionTime:
                    type: string
                    format: date-time

  /api/v1/jobs/{jobId}/status:
    get:
      summary: Get job status
      parameters:
        - name: jobId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Job status information
          content:
            application/json:
              schema:
                type: object
                properties:
                  jobId:
                    type: string
                  status:
                    type: string
                  recordsProcessed:
                    type: integer
                  recordsTotal:
                    type: integer
                  errorCount:
                    type: integer
                  startTime:
                    type: string
                    format: date-time
                  completionTime:
                    type: string
                    format: date-time
```

**Configuration Service API:**

```yaml
  /api/v1/mappings/{sourceSystem}/{fileType}:
    get:
      summary: Get field mappings for source system and file type
      parameters:
        - name: sourceSystem
          in: path
          required: true
          schema:
            type: string
        - name: fileType
          in: path
          required: true
          schema:
            type: string
        - name: version
          in: query
          schema:
            type: string
            default: "LATEST"
      responses:
        '200':
          description: Field mapping configuration
          content:
            application/json:
              schema:
                type: object
                properties:
                  mappingId:
                    type: string
                  version:
                    type: string
                  effectiveDate:
                    type: string
                    format: date
                  fieldMappings:
                    type: array
                    items:
                      type: object
                      properties:
                        sourceField:
                          type: string
                        targetField:
                          type: string
                        dataType:
                          type: string
                        transformationRule:
                          type: string
                        validationRule:
                          type: string
                        isMandatory:
                          type: boolean
                        defaultValue:
                          type: string
```

### 3.3 Inter-Service Communication Patterns

**Event-Driven Communication:**

```java
// Kafka Event Schema for File Processing
@Component
public class FileProcessingEventPublisher {
    
    @Autowired
    private KafkaTemplate<String, FileProcessingEvent> kafkaTemplate;
    
    public void publishFileIngestionStarted(String jobId, String fileName, String sourceSystem) {
        FileProcessingEvent event = FileProcessingEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("FILE_INGESTION_STARTED")
            .timestamp(Instant.now())
            .jobId(jobId)
            .fileName(fileName)
            .sourceSystem(sourceSystem)
            .build();
            
        kafkaTemplate.send("file-processing-events", jobId, event);
    }
    
    public void publishDataQualityResults(String jobId, DataQualityResults results) {
        FileProcessingEvent event = FileProcessingEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("DATA_QUALITY_COMPLETED")
            .timestamp(Instant.now())
            .jobId(jobId)
            .payload(results)
            .build();
            
        kafkaTemplate.send("data-quality-events", jobId, event);
    }
}
```

**Synchronous Communication with Circuit Breaker:**

```java
@Component
public class ConfigurationServiceClient {
    
    @Autowired
    private WebClient webClient;
    
    @CircuitBreaker(name = "configuration-service", fallbackMethod = "getFallbackMapping")
    @Retry(name = "configuration-service")
    @TimeLimiter(name = "configuration-service")
    public CompletableFuture<FieldMappingConfig> getFieldMapping(
            String sourceSystem, String fileType, String version) {
        
        return webClient.get()
            .uri("/api/v1/mappings/{sourceSystem}/{fileType}", sourceSystem, fileType)
            .header("X-Request-ID", UUID.randomUUID().toString())
            .header("X-Version", version)
            .retrieve()
            .bodyToMono(FieldMappingConfig.class)
            .toFuture();
    }
    
    public CompletableFuture<FieldMappingConfig> getFallbackMapping(
            String sourceSystem, String fileType, String version, Exception ex) {
        
        // Return cached or default mapping configuration
        return CompletableFuture.completedFuture(getDefaultMapping(sourceSystem, fileType));
    }
}
```

### 3.4 Transaction Management and Data Consistency

**Saga Pattern Implementation:**

```java
@Component
public class FileProcessingSaga {
    
    @SagaOrchestrationStart
    public void processFile(FileProcessingCommand command) {
        // Step 1: Ingest file
        sagaOrchestrator.choreography()
            .step("ingest-file")
            .invokeParticipant(FileIngestionService.class)
            .withCompensation(FileIngestionService.class, "rollbackIngestion")
            
            // Step 2: Validate data quality
            .step("validate-data")
            .invokeParticipant(DataQualityService.class)
            .withCompensation(DataQualityService.class, "rollbackValidation")
            
            // Step 3: Transform data
            .step("transform-data")
            .invokeParticipant(TransformationService.class)
            .withCompensation(TransformationService.class, "rollbackTransformation")
            
            // Step 4: Update operational store
            .step("update-operational-store")
            .invokeParticipant(OperationalDataService.class)
            .withCompensation(OperationalDataService.class, "rollbackOperationalUpdate")
            
            .execute();
    }
}
```

**Event Sourcing for Audit Trail:**

```java
@Entity
@Table(name = "EVENT_STORE")
public class EventStore {
    @Id
    private String eventId;
    
    @Column(name = "AGGREGATE_ID")
    private String aggregateId;
    
    @Column(name = "EVENT_TYPE")
    private String eventType;
    
    @Column(name = "EVENT_DATA")
    private String eventData;
    
    @Column(name = "EVENT_TIMESTAMP")
    private Instant timestamp;
    
    @Column(name = "EVENT_VERSION")
    private Long version;
    
    @Column(name = "CORRELATION_ID")
    private String correlationId;
    
    // Regulatory compliance fields
    @Column(name = "USER_ID")
    private String userId;
    
    @Column(name = "IP_ADDRESS")
    private String ipAddress;
    
    @Column(name = "SESSION_ID")
    private String sessionId;
}
```

## 4. Configuration Management Strategy

### 4.1 Dynamic Field Mapping Configuration Approach

**Configuration-as-Code with Runtime Flexibility:**

```java
@Configuration
@EnableConfigurationProperties(FieldMappingProperties.class)
public class DynamicFieldMappingConfiguration {
    
    @Autowired
    private FieldMappingProperties properties;
    
    @Autowired
    private ConfigurationRepository configurationRepository;
    
    @Bean
    @RefreshScope
    public FieldMappingManager fieldMappingManager() {
        return new FieldMappingManager(
            configurationRepository,
            properties.getCacheExpirationMinutes(),
            properties.getRefreshIntervalMinutes()
        );
    }
}

@Component
public class FieldMappingManager {
    
    private final ConfigurationRepository configurationRepository;
    private final Cache<String, FieldMappingConfig> mappingCache;
    private final ApplicationEventPublisher eventPublisher;
    
    public FieldMappingConfig getMappingConfig(String sourceSystem, 
                                             String fileType, 
                                             String version) {
        String cacheKey = buildCacheKey(sourceSystem, fileType, version);
        
        return mappingCache.get(cacheKey, () -> {
            FieldMappingConfig config = loadMappingFromDatabase(sourceSystem, fileType, version);
            
            // Validate configuration integrity
            validateMappingConfig(config);
            
            // Publish configuration loaded event
            eventPublisher.publishEvent(new ConfigurationLoadedEvent(config));
            
            return config;
        });
    }
    
    @EventListener
    public void handleConfigurationUpdate(ConfigurationUpdateEvent event) {
        // Invalidate cache for updated configuration
        String cacheKey = buildCacheKey(
            event.getSourceSystem(), 
            event.getFileType(), 
            event.getVersion()
        );
        mappingCache.invalidate(cacheKey);
        
        // Notify dependent services
        notifyConfigurationChange(event);
    }
    
    private void validateMappingConfig(FieldMappingConfig config) {
        // Validate mandatory field mappings
        Set<String> mandatoryFields = config.getFieldMappings().stream()
            .filter(FieldMapping::isMandatory)
            .map(FieldMapping::getSourceField)
            .collect(Collectors.toSet());
            
        if (mandatoryFields.isEmpty()) {
            throw new ConfigurationValidationException(
                "At least one mandatory field mapping is required"
            );
        }
        
        // Validate transformation rules syntax
        config.getFieldMappings().forEach(this::validateTransformationRule);
        
        // Validate data type compatibility
        config.getFieldMappings().forEach(this::validateDataTypeMapping);
    }
}
```

### 4.2 Configuration Versioning and Deployment

**Version Control Strategy:**

```java
@Entity
@Table(name = "CONFIGURATION_VERSION")
public class ConfigurationVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "CONFIG_TYPE")
    private String configType;
    
    @Column(name = "SOURCE_SYSTEM")
    private String sourceSystem;
    
    @Column(name = "FILE_TYPE")
    private String fileType;
    
    @Column(name = "VERSION")
    private String version;
    
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private ConfigurationStatus status; // DRAFT, APPROVED, ACTIVE, DEPRECATED
    
    @Column(name = "CONFIG_HASH")
    private String configHash;
    
    @Column(name = "ROLLBACK_VERSION")
    private String rollbackVersion;
    
    @Column(name = "EFFECTIVE_DATE")
    private LocalDateTime effectiveDate;
    
    @Column(name = "EXPIRY_DATE")
    private LocalDateTime expiryDate;
    
    @Column(name = "APPROVAL_WORKFLOW_ID")
    private String approvalWorkflowId;
    
    @Column(name = "CREATED_BY")
    private String createdBy;
    
    @Column(name = "APPROVED_BY")
    private String approvedBy;
    
    @Column(name = "DEPLOYMENT_NOTES")
    private String deploymentNotes;
}

@Component
public class ConfigurationDeploymentManager {
    
    @Autowired
    private ConfigurationVersionRepository versionRepository;
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    @Transactional
    public DeploymentResult deployConfiguration(ConfigurationDeploymentRequest request) {
        
        // Validate deployment prerequisites
        validateDeploymentRequest(request);
        
        // Create new version
        ConfigurationVersion newVersion = createConfigurationVersion(request);
        
        // Initiate approval workflow for production deployments
        if (request.getEnvironment().equals("PRODUCTION")) {
            String workflowId = workflowEngine.startApprovalWorkflow(
                newVersion, 
                request.getApprovers()
            );
            newVersion.setApprovalWorkflowId(workflowId);
        }
        
        // Deploy to target environment
        DeploymentResult result = performDeployment(newVersion, request);
        
        // Update version status
        updateVersionStatus(newVersion, result);
        
        return result;
    }
    
    @Transactional
    public RollbackResult rollbackConfiguration(String sourceSystem, 
                                               String fileType, 
                                               String targetVersion) {
        
        ConfigurationVersion currentVersion = versionRepository
            .findActiveVersion(sourceSystem, fileType);
            
        ConfigurationVersion rollbackVersion = versionRepository
            .findByVersion(sourceSystem, fileType, targetVersion);
            
        if (rollbackVersion == null) {
            throw new ConfigurationException("Rollback version not found: " + targetVersion);
        }
        
        // Perform rollback
        RollbackResult result = performRollback(currentVersion, rollbackVersion);
        
        // Update version statuses
        currentVersion.setStatus(ConfigurationStatus.DEPRECATED);
        rollbackVersion.setStatus(ConfigurationStatus.ACTIVE);
        
        versionRepository.saveAll(Arrays.asList(currentVersion, rollbackVersion));
        
        return result;
    }
}
```

### 4.3 Runtime Configuration Management

**Dynamic Configuration Updates:**

```java
@RestController
@RequestMapping("/api/v1/admin/configuration")
@PreAuthorize("hasRole('CONFIG_ADMIN')")
public class ConfigurationAdminController {
    
    @Autowired
    private ConfigurationDeploymentManager deploymentManager;
    
    @Autowired
    private ConfigurationValidationService validationService;
    
    @PostMapping("/deploy")
    public ResponseEntity<DeploymentResult> deployConfiguration(
            @RequestBody @Valid ConfigurationDeploymentRequest request) {
        
        // Pre-deployment validation
        ValidationResult validation = validationService.validateConfiguration(request);
        if (!validation.isValid()) {
            return ResponseEntity.badRequest()
                .body(DeploymentResult.failed(validation.getErrors()));
        }
        
        // Deploy configuration
        DeploymentResult result = deploymentManager.deployConfiguration(request);
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/rollback")
    public ResponseEntity<RollbackResult> rollbackConfiguration(
            @RequestBody @Valid ConfigurationRollbackRequest request) {
        
        RollbackResult result = deploymentManager.rollbackConfiguration(
            request.getSourceSystem(),
            request.getFileType(),
            request.getTargetVersion()
        );
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/versions/{sourceSystem}/{fileType}")
    public ResponseEntity<List<ConfigurationVersion>> getConfigurationVersions(
            @PathVariable String sourceSystem,
            @PathVariable String fileType) {
        
        List<ConfigurationVersion> versions = deploymentManager
            .getConfigurationVersions(sourceSystem, fileType);
            
        return ResponseEntity.ok(versions);
    }
    
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateConfiguration(
            @RequestBody ConfigurationValidationRequest request) {
        
        ValidationResult result = validationService.validateConfiguration(request);
        return ResponseEntity.ok(result);
    }
}

@Component
public class ConfigurationValidationService {
    
    @Autowired
    private List<ConfigurationValidator> validators;
    
    public ValidationResult validateConfiguration(ConfigurationValidationRequest request) {
        ValidationResult result = new ValidationResult();
        
        for (ConfigurationValidator validator : validators) {
            if (validator.supports(request.getConfigType())) {
                ValidationResult validatorResult = validator.validate(request);
                result.merge(validatorResult);
            }
        }
        
        return result;
    }
}

@Component
public class FieldMappingValidator implements ConfigurationValidator {
    
    @Override
    public boolean supports(String configType) {
        return "FIELD_MAPPING".equals(configType);
    }
    
    @Override
    public ValidationResult validate(ConfigurationValidationRequest request) {
        ValidationResult result = new ValidationResult();
        
        FieldMappingConfig config = (FieldMappingConfig) request.getConfiguration();
        
        // Validate field mapping rules
        validateFieldMappings(config.getFieldMappings(), result);
        
        // Validate transformation expressions
        validateTransformationRules(config.getFieldMappings(), result);
        
        // Validate data type compatibility
        validateDataTypeCompatibility(config.getFieldMappings(), result);
        
        // Validate business rules
        validateBusinessRules(config.getBusinessRules(), result);
        
        return result;
    }
    
    private void validateFieldMappings(List<FieldMapping> mappings, ValidationResult result) {
        Set<String> sourceFields = new HashSet<>();
        Set<String> targetFields = new HashSet<>();
        
        for (FieldMapping mapping : mappings) {
            // Check for duplicate source fields
            if (!sourceFields.add(mapping.getSourceField())) {
                result.addError("Duplicate source field: " + mapping.getSourceField());
            }
            
            // Check for duplicate target fields
            if (!targetFields.add(mapping.getTargetField())) {
                result.addError("Duplicate target field: " + mapping.getTargetField());
            }
            
            // Validate field names
            if (!isValidFieldName(mapping.getSourceField())) {
                result.addError("Invalid source field name: " + mapping.getSourceField());
            }
            
            if (!isValidFieldName(mapping.getTargetField())) {
                result.addError("Invalid target field name: " + mapping.getTargetField());
            }
        }
    }
    
    private void validateTransformationRules(List<FieldMapping> mappings, ValidationResult result) {
        for (FieldMapping mapping : mappings) {
            if (StringUtils.hasText(mapping.getTransformationRule())) {
                try {
                    // Validate expression syntax using SpEL parser
                    ExpressionParser parser = new SpelExpressionParser();
                    parser.parseExpression(mapping.getTransformationRule());
                } catch (ParseException e) {
                    result.addError("Invalid transformation rule for field " + 
                        mapping.getSourceField() + ": " + e.getMessage());
                }
            }
        }
    }
}
```

## 5. Data Processing Architecture

### 5.1 SQL*Loader Integration Patterns

**Automated Control File Generation:**

```java
@Component
public class SqlLoaderControlFileGenerator {
    
    @Autowired
    private FieldMappingManager fieldMappingManager;
    
    @Autowired
    private FileMetadataAnalyzer metadataAnalyzer;
    
    public ControlFileConfiguration generateControlFile(FileIngestionRequest request) {
        
        // Analyze file structure and metadata
        FileMetadata metadata = metadataAnalyzer.analyzeFile(request.getFilePath());
        
        // Get field mapping configuration
        FieldMappingConfig mappingConfig = fieldMappingManager.getMappingConfig(
            request.getSourceSystem(),
            metadata.getFileType(),
            request.getVersion()
        );
        
        // Generate control file content
        String controlFileContent = buildControlFileContent(metadata, mappingConfig, request);
        
        // Create control file configuration
        return ControlFileConfiguration.builder()
            .controlFileContent(controlFileContent)
            .controlFileName(generateControlFileName(request))
            .logFileName(generateLogFileName(request))
            .badFileName(generateBadFileName(request))
            .discardFileName(generateDiscardFileName(request))
            .build();
    }
    
    private String buildControlFileContent(FileMetadata metadata, 
                                         FieldMappingConfig mappingConfig, 
                                         FileIngestionRequest request) {
        
        StringBuilder controlFile = new StringBuilder();
        
        // Control file header
        controlFile.append("LOAD DATA\n");
        controlFile.append("INFILE '").append(request.getFilePath()).append("'\n");
        controlFile.append("BADFILE '").append(generateBadFileName(request)).append("'\n");
        controlFile.append("DISCARDFILE '").append(generateDiscardFileName(request)).append("'\n");
        
        // Append/Insert/Replace mode
        controlFile.append("APPEND\n");
        
        // Table specification
        controlFile.append("INTO TABLE ").append(request.getTargetTable()).append("\n");
        
        // Field termination for delimited files
        if (metadata.getFileFormat() == FileFormat.DELIMITED) {
            controlFile.append("FIELDS TERMINATED BY '").append(metadata.getDelimiter()).append("'\n");
            if (metadata.hasTextQualifier()) {
                controlFile.append("OPTIONALLY ENCLOSED BY '").append(metadata.getTextQualifier()).append("'\n");
            }
        }
        
        // Trailing nullcols for handling missing values
        controlFile.append("TRAILING NULLCOLS\n");
        
        // Field mappings
        controlFile.append("(\n");
        
        List<FieldMapping> fieldMappings = mappingConfig.getFieldMappings();
        for (int i = 0; i < fieldMappings.size(); i++) {
            FieldMapping mapping = fieldMappings.get(i);
            controlFile.append("  ").append(buildFieldSpecification(mapping, metadata));
            
            if (i < fieldMappings.size() - 1) {
                controlFile.append(",");
            }
            controlFile.append("\n");
        }
        
        controlFile.append(")\n");
        
        return controlFile.toString();
    }
    
    private String buildFieldSpecification(FieldMapping mapping, FileMetadata metadata) {
        StringBuilder fieldSpec = new StringBuilder();
        
        fieldSpec.append(mapping.getTargetField());
        
        // Position specification for fixed-width files
        if (metadata.getFileFormat() == FileFormat.FIXED_WIDTH) {
            FieldPosition position = metadata.getFieldPosition(mapping.getSourceField());
            fieldSpec.append(" POSITION(").append(position.getStart())
                    .append(":").append(position.getEnd()).append(")");
        }
        
        // Data type specification
        if (StringUtils.hasText(mapping.getDataType())) {
            fieldSpec.append(" ").append(mapDataTypeToSqlLoader(mapping.getDataType()));
        }
        
        // Transformation expression
        if (StringUtils.hasText(mapping.getTransformationRule())) {
            fieldSpec.append(" \"").append(mapping.getTransformationRule()).append("\"");
        }
        
        // Default value for missing data
        if (StringUtils.hasText(mapping.getDefaultValue())) {
            fieldSpec.append(" DEFAULTIF ").append(mapping.getSourceField()).append("=BLANKS");
        }
        
        return fieldSpec.toString();
    }
}

@Component
public class SqlLoaderExecutionService {
    
    @Autowired
    private SqlLoaderControlFileGenerator controlFileGenerator;
    
    @Autowired
    private ProcessExecutor processExecutor;
    
    @Autowired
    private FileProcessingMetrics metrics;
    
    @Async("sqlLoaderExecutor")
    public CompletableFuture<SqlLoaderResult> executeSqlLoader(FileIngestionRequest request) {
        
        try {
            // Generate control file
            ControlFileConfiguration controlConfig = controlFileGenerator.generateControlFile(request);
            
            // Write control file to filesystem
            String controlFilePath = writeControlFile(controlConfig);
            
            // Build SQL*Loader command
            List<String> command = buildSqlLoaderCommand(request, controlFilePath);
            
            // Execute SQL*Loader
            ProcessResult processResult = processExecutor.execute(command, request.getTimeoutMinutes());
            
            // Parse results
            SqlLoaderResult result = parseSqlLoaderOutput(processResult, controlConfig);
            
            // Update metrics
            updateProcessingMetrics(request, result);
            
            // Cleanup temporary files
            cleanupTemporaryFiles(controlConfig);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            SqlLoaderResult errorResult = SqlLoaderResult.builder()
                .status(ProcessingStatus.FAILED)
                .errorMessage(e.getMessage())
                .build();
                
            return CompletableFuture.completedFuture(errorResult);
        }
    }
    
    private List<String> buildSqlLoaderCommand(FileIngestionRequest request, String controlFilePath) {
        List<String> command = new ArrayList<>();
        
        command.add("sqlldr");
        command.add("userid=" + buildConnectionString(request.getDatabaseConfig()));
        command.add("control=" + controlFilePath);
        command.add("parallel=true");
        command.add("direct=true");
        command.add("skip_index_maintenance=true");
        
        // Performance tuning parameters
        command.add("rows=" + request.getCommitInterval());
        command.add("bindsize=" + request.getBindSize());
        command.add("readsize=" + request.getReadSize());
        
        // Error handling parameters
        command.add("errors=" + request.getMaxErrors());
        command.add("skip=" + request.getSkipRecords());
        
        return command;
    }
    
    private SqlLoaderResult parseSqlLoaderOutput(ProcessResult processResult, 
                                               ControlFileConfiguration controlConfig) {
        
        // Parse log file for processing statistics
        String logContent = readLogFile(controlConfig.getLogFileName());
        
        return SqlLoaderResult.builder()
            .status(processResult.getExitCode() == 0 ? ProcessingStatus.COMPLETED : ProcessingStatus.FAILED)
            .recordsLoaded(extractRecordsLoaded(logContent))
            .recordsRejected(extractRecordsRejected(logContent))
            .recordsDiscarded(extractRecordsDiscarded(logContent))
            .executionTimeMillis(processResult.getExecutionTimeMillis())
            .logFilePath(controlConfig.getLogFileName())
            .badFilePath(controlConfig.getBadFileName())
            .errorMessages(extractErrorMessages(logContent))
            .build();
    }
}
```

### 5.2 File Processing Workflows

**Spring Batch Job Configuration:**

```java
@Configuration
@EnableBatchProcessing
public class FileProcessingJobConfiguration {
    
    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job consumerDefaultFileProcessingJob() {
        return jobBuilderFactory.get("consumerDefaultFileProcessingJob")
            .incrementer(new RunIdIncrementer())
            .validator(fileProcessingJobParametersValidator())
            .listener(fileProcessingJobListener())
            .start(fileValidationStep())
            .next(fileIngestionStep())
            .next(dataQualityValidationStep())
            .next(businessRuleTransformationStep())
            .next(operationalDataUpdateStep())
            .build();
    }
    
    @Bean
    public Step fileValidationStep() {
        return stepBuilderFactory.get("fileValidationStep")
            .tasklet(fileValidationTasklet())
            .listener(stepExecutionListener())
            .build();
    }
    
    @Bean
    public Step fileIngestionStep() {
        return stepBuilderFactory.get("fileIngestionStep")
            .tasklet(sqlLoaderIngestionTasklet())
            .listener(stepExecutionListener())
            .build();
    }
    
    @Bean
    public Step dataQualityValidationStep() {
        return stepBuilderFactory.get("dataQualityValidationStep")
            .<ConsumerDefaultRecord, DataQualityResult>chunk(1000)
            .reader(stagingDataReader())
            .processor(dataQualityProcessor())
            .writer(dataQualityResultWriter())
            .faultTolerant()
            .skipLimit(100)
            .skip(DataQualityException.class)
            .listener(chunkListener())
            .build();
    }
    
    @Bean
    public Step businessRuleTransformationStep() {
        return stepBuilderFactory.get("businessRuleTransformationStep")
            .<ConsumerDefaultRecord, TransformedConsumerDefaultRecord>chunk(500)
            .reader(validatedDataReader())
            .processor(businessRuleProcessor())
            .writer(transformedDataWriter())
            .faultTolerant()
            .retryLimit(3)
            .retry(TransformationException.class)
            .listener(chunkListener())
            .build();
    }
    
    @Bean
    public Tasklet sqlLoaderIngestionTasklet() {
        return new SqlLoaderIngestionTasklet();
    }
}

@Component
public class SqlLoaderIngestionTasklet implements Tasklet {
    
    @Autowired
    private SqlLoaderExecutionService sqlLoaderService;
    
    @Autowired
    private DataLineageService lineageService;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        
        JobParameters jobParameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
        
        // Build ingestion request from job parameters
        FileIngestionRequest request = FileIngestionRequest.builder()
            .filePath(jobParameters.getString("filePath"))
            .sourceSystem(jobParameters.getString("sourceSystem"))
            .fileType(jobParameters.getString("fileType"))
            .batchId(jobParameters.getString("batchId"))
            .targetTable("CONSUMER_DEFAULT_STAGING")
            .build();
        
        // Execute SQL*Loader
        CompletableFuture<SqlLoaderResult> future = sqlLoaderService.executeSqlLoader(request);
        SqlLoaderResult result = future.get();
        
        // Update step execution context
        ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
        executionContext.put("recordsLoaded", result.getRecordsLoaded());
        executionContext.put("recordsRejected", result.getRecordsRejected());
        executionContext.put("executionTimeMillis", result.getExecutionTimeMillis());
        
        // Record data lineage
        lineageService.recordIngestionEvent(request.getBatchId(), result);
        
        // Handle processing results
        if (result.getStatus() == ProcessingStatus.FAILED) {
            throw new SqlLoaderException("SQL*Loader execution failed: " + result.getErrorMessage());
        }
        
        if (result.getRecordsRejected() > request.getMaxErrors()) {
            throw new DataQualityException("Too many rejected records: " + result.getRecordsRejected());
        }
        
        return RepeatStatus.FINISHED;
    }
}
```

### 5.3 Error Handling and Data Quality Framework

**Comprehensive Error Handling:**

```java
@Component
public class DataQualityProcessor implements ItemProcessor<ConsumerDefaultRecord, DataQualityResult> {
    
    @Autowired
    private List<DataQualityRule> qualityRules;
    
    @Autowired
    private DataQualityMetrics metrics;
    
    @Override
    public DataQualityResult process(ConsumerDefaultRecord record) throws Exception {
        
        DataQualityResult result = new DataQualityResult();
        result.setStagingId(record.getStagingId());
        result.setRecordSequence(record.getRecordSequence());
        
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        // Apply all quality rules
        for (DataQualityRule rule : qualityRules) {
            if (rule.appliesTo(record)) {
                RuleExecutionResult ruleResult = rule.execute(record);
                
                switch (ruleResult.getSeverity()) {
                    case ERROR:
                        errors.add(new ValidationError(rule.getRuleName(), ruleResult.getMessage()));
                        break;
                    case WARNING:
                        warnings.add(new ValidationWarning(rule.getRuleName(), ruleResult.getMessage()));
                        break;
                    case CORRECTED:
                        // Apply data correction
                        applyDataCorrection(record, ruleResult.getCorrection());
                        warnings.add(new ValidationWarning(rule.getRuleName(), "Data corrected: " + ruleResult.getMessage()));
                        break;
                }
            }
        }
        
        result.setErrors(errors);
        result.setWarnings(warnings);
        result.setOverallStatus(errors.isEmpty() ? ValidationStatus.PASSED : ValidationStatus.FAILED);
        
        // Update metrics
        updateQualityMetrics(result);
        
        return result;
    }
    
    private void updateQualityMetrics(DataQualityResult result) {
        metrics.incrementTotalRecords();
        
        if (result.getOverallStatus() == ValidationStatus.PASSED) {
            metrics.incrementPassedRecords();
        } else {
            metrics.incrementFailedRecords();
        }
        
        metrics.addErrorCount(result.getErrors().size());
        metrics.addWarningCount(result.getWarnings().size());
    }
}

@Component
public class MandatoryFieldValidationRule implements DataQualityRule {
    
    @Autowired
    private FieldMappingManager fieldMappingManager;
    
    @Override
    public String getRuleName() {
        return "MANDATORY_FIELD_VALIDATION";
    }
    
    @Override
    public boolean appliesTo(ConsumerDefaultRecord record) {
        return true; // Apply to all records
    }
    
    @Override
    public RuleExecutionResult execute(ConsumerDefaultRecord record) {
        
        // Get mandatory fields from configuration
        FieldMappingConfig config = fieldMappingManager.getMappingConfig(
            record.getSourceSystem(),
            record.getFileType(),
            "LATEST"
        );
        
        List<String> mandatoryFields = config.getFieldMappings().stream()
            .filter(FieldMapping::isMandatory)
            .map(FieldMapping::getTargetField)
            .collect(Collectors.toList());
        
        // Check for missing mandatory fields
        List<String> missingFields = new ArrayList<>();
        
        for (String field : mandatoryFields) {
            Object value = getFieldValue(record, field);
            if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                missingFields.add(field);
            }
        }
        
        if (!missingFields.isEmpty()) {
            return RuleExecutionResult.builder()
                .severity(RuleSeverity.ERROR)
                .message("Missing mandatory fields: " + String.join(", ", missingFields))
                .build();
        }
        
        return RuleExecutionResult.passed();
    }
}

@Component
public class BusinessRuleProcessor implements ItemProcessor<ConsumerDefaultRecord, TransformedConsumerDefaultRecord> {
    
    @Autowired
    private BusinessRuleEngine ruleEngine;
    
    @Autowired
    private TransformationService transformationService;
    
    @Override
    public TransformedConsumerDefaultRecord process(ConsumerDefaultRecord record) throws Exception {
        
        TransformedConsumerDefaultRecord transformed = new TransformedConsumerDefaultRecord(record);
        
        // Apply business transformation rules
        BusinessRuleContext context = BusinessRuleContext.builder()
            .record(record)
            .sourceSystem(record.getSourceSystem())
            .processingDate(LocalDate.now())
            .build();
        
        // Execute risk calculation rules
        RiskCalculationResult riskResult = ruleEngine.calculateRisk(context);
        transformed.setRiskRating(riskResult.getRiskRating());
        transformed.setRiskScore(riskResult.getRiskScore());
        transformed.setRiskFactors(riskResult.getRiskFactors());
        
        // Execute default calculation rules
        DefaultCalculationResult defaultResult = ruleEngine.calculateDefaultMetrics(context);
        transformed.setDefaultProbability(defaultResult.getDefaultProbability());
        transformed.setExpectedLoss(defaultResult.getExpectedLoss());
        transformed.setRecoveryRate(defaultResult.getRecoveryRate());
        
        // Apply data standardization transformations
        transformationService.standardizeData(transformed);
        
        // Record transformation lineage
        recordTransformationLineage(record, transformed);
        
        return transformed;
    }
    
    private void recordTransformationLineage(ConsumerDefaultRecord source, 
                                           TransformedConsumerDefaultRecord target) {
        
        TransformationLineage lineage = TransformationLineage.builder()
            .sourceRecordId(source.getStagingId())
            .targetRecordId(target.getId())
            .transformationType("BUSINESS_RULE_TRANSFORMATION")
            .transformationTimestamp(Instant.now())
            .appliedRules(target.getAppliedRules())
            .build();
            
        // Publish lineage event
        eventPublisher.publishEvent(new TransformationLineageCreatedEvent(lineage));
    }
}
```

## 6. Security and Compliance Framework

### 6.1 Data Encryption Strategy

**Encryption at Rest and in Transit:**

```java
@Configuration
@EnableEncryptableProperties
public class SecurityConfiguration {
    
    @Bean
    public EncryptorService encryptorService() {
        return new AESEncryptorService();
    }
    
    @Bean
    public DataEncryptionManager dataEncryptionManager() {
        return new DataEncryptionManager(encryptorService());
    }
}

@Component
public class DataEncryptionManager {
    
    private final EncryptorService encryptorService;
    private final KeyManagementService keyManagementService;
    
    public EncryptedData encryptSensitiveData(String data, String dataClassification) {
        
        // Determine encryption strength based on data classification
        EncryptionStrength strength = determineEncryptionStrength(dataClassification);
        
        // Get appropriate encryption key
        EncryptionKey key = keyManagementService.getEncryptionKey(dataClassification, strength);
        
        // Encrypt data
        String encryptedValue = encryptorService.encrypt(data, key);
        
        return EncryptedData.builder()
            .encryptedValue(encryptedValue)
            .keyId(key.getKeyId())
            .algorithm(key.getAlgorithm())
            .dataClassification(dataClassification)
            .encryptionTimestamp(Instant.now())
            .build();
    }
    
    private EncryptionStrength determineEncryptionStrength(String dataClassification) {
        switch (dataClassification.toUpperCase()) {
            case "PCI":
            case "PII":
                return EncryptionStrength.AES_256;
            case "SENSITIVE":
                return EncryptionStrength.AES_192;
            case "INTERNAL":
                return EncryptionStrength.AES_128;
            default:
                return EncryptionStrength.AES_128;
        }
    }
}

@Entity
@Table(name = "ENCRYPTED_DATA_REGISTRY")
public class EncryptedDataRegistry {
    
    @Id
    private String registryId;
    
    @Column(name = "TABLE_NAME")
    private String tableName;
    
    @Column(name = "COLUMN_NAME")
    private String columnName;
    
    @Column(name = "DATA_CLASSIFICATION")
    private String dataClassification;
    
    @Column(name = "ENCRYPTION_KEY_ID")
    private String encryptionKeyId;
    
    @Column(name = "ENCRYPTION_ALGORITHM")
    private String encryptionAlgorithm;
    
    @Column(name = "KEY_ROTATION_SCHEDULE")
    private String keyRotationSchedule;
    
    @Column(name = "COMPLIANCE_REQUIREMENT")
    private String complianceRequirement;
    
    @Column(name = "CREATED_TIMESTAMP")
    private Instant createdTimestamp;
    
    @Column(name = "LAST_KEY_ROTATION")
    private Instant lastKeyRotation;
}
```

### 6.2 Access Control and Audit Logging

**Role-Based Access Control:**

```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class AccessControlConfiguration extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private CustomAuthenticationProvider authenticationProvider;
    
    @Autowired
    private AuditEventListener auditEventListener;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .antMatchers("/api/v1/config/**").hasRole("CONFIG_MANAGER")
                .antMatchers("/api/v1/files/ingest").hasRole("DATA_PROCESSOR")
                .antMatchers("/api/v1/files/status/**").hasAnyRole("DATA_PROCESSOR", "MONITOR")
                .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
                .jwt()
                .jwtDecoder(jwtDecoder())
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(auditFilter(), UsernamePasswordAuthenticationFilter.class);
    }
    
    @Bean
    public AuditFilter auditFilter() {
        return new AuditFilter(auditEventListener);
    }
}

@Component
public class AuditEventListener {
    
    @Autowired
    private AuditTrailRepository auditTrailRepository;
    
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        recordAuditEvent(
            AuditEventType.AUTHENTICATION_SUCCESS,
            event.getAuthentication().getName(),
            "User successfully authenticated",
            getCurrentRequest()
        );
    }
    
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        recordAuditEvent(
            AuditEventType.AUTHENTICATION_FAILURE,
            event.getAuthentication().getName(),
            "Authentication failed: " + event.getException().getMessage(),
            getCurrentRequest()
        );
    }
    
    @EventListener
    public void handleDataAccess(DataAccessEvent event) {
        recordAuditEvent(
            AuditEventType.DATA_ACCESS,
            event.getUserId(),
            "Data accessed: " + event.getResourceIdentifier(),
            event.getRequest()
        );
    }
    
    private void recordAuditEvent(AuditEventType eventType, 
                                String userId, 
                                String description, 
                                HttpServletRequest request) {
        
        AuditTrailEntry entry = AuditTrailEntry.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(eventType)
            .userId(userId)
            .description(description)
            .ipAddress(getClientIpAddress(request))
            .userAgent(request.getHeader("User-Agent"))
            .sessionId(request.getSession().getId())
            .timestamp(Instant.now())
            .complianceFlag(isComplianceEvent(eventType))
            .build();
            
        auditTrailRepository.save(entry);
    }
}

@Component
public class DataMaskingService {
    
    @Autowired
    private DataClassificationService classificationService;
    
    public String maskSensitiveData(String data, String fieldName, String userRole) {
        
        DataClassification classification = classificationService.getClassification(fieldName);
        
        if (!hasAccessPermission(userRole, classification)) {
            return maskData(data, classification.getMaskingStrategy());
        }
        
        return data;
    }
    
    private String maskData(String data, MaskingStrategy strategy) {
        switch (strategy) {
            case FULL_MASK:
                return "*".repeat(data.length());
            case PARTIAL_MASK:
                return maskPartially(data);
            case HASH_MASK:
                return hashData(data);
            case TOKENIZE:
                return tokenizeData(data);
            default:
                return data;
        }
    }
    
    private String maskPartially(String data) {
        if (data.length() <= 4) {
            return "*".repeat(data.length());
        }
        
        // Show first 2 and last 2 characters
        return data.substring(0, 2) + "*".repeat(data.length() - 4) + data.substring(data.length() - 2);
    }
}
```

### 6.3 Regulatory Compliance Implementation

**SOX Compliance Framework:**

```java
@Component
public class SoxComplianceService {
    
    @Autowired
    private ComplianceAuditRepository auditRepository;
    
    @Autowired
    private ChangeManagementService changeManagementService;
    
    @EventListener
    public void handleDataModification(DataModificationEvent event) {
        
        // SOX requires detailed audit trail for financial data changes
        if (isFinancialData(event.getEntityType())) {
            recordSoxAuditEvent(event);
        }
        
        // Validate change authorization
        validateChangeAuthorization(event);
    }
    
    private void recordSoxAuditEvent(DataModificationEvent event) {
        
        SoxAuditEntry entry = SoxAuditEntry.builder()
            .auditId(UUID.randomUUID().toString())
            .entityType(event.getEntityType())
            .entityId(event.getEntityId())
            .operationType(event.getOperationType())
            .oldValues(event.getOldValues())
            .newValues(event.getNewValues())
            .changedBy(event.getUserId())
            .changeTimestamp(event.getTimestamp())
            .businessJustification(event.getBusinessJustification())
            .approvalReference(event.getApprovalReference())
            .ipAddress(event.getIpAddress())
            .sessionId(event.getSessionId())
            .complianceStatus(ComplianceStatus.COMPLIANT)
            .build();
            
        auditRepository.save(entry);
    }
    
    private void validateChangeAuthorization(DataModificationEvent event) {
        
        if (isHighRiskChange(event)) {
            // High-risk changes require pre-approval
            if (!hasPreApproval(event)) {
                throw new ComplianceViolationException(
                    "High-risk change requires pre-approval: " + event.getChangeDescription()
                );
            }
        }
        
        // Validate segregation of duties
        validateSegregationOfDuties(event);
    }
    
    private void validateSegregationOfDuties(DataModificationEvent event) {
        
        // Users cannot approve their own changes
        if (event.getUserId().equals(event.getApprovalUserId())) {
            throw new ComplianceViolationException(
                "Segregation of duties violation: User cannot approve their own changes"
            );
        }
        
        // Validate role-based authorization
        UserRole userRole = getUserRole(event.getUserId());
        if (!canPerformOperation(userRole, event.getOperationType(), event.getEntityType())) {
            throw new ComplianceViolationException(
                "User role does not have permission for this operation"
            );
        }
    }
}

/**
 * PCI-DSS Compliance for Credit Card Data
 */
@Component
public class PciComplianceService {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private TokenizationService tokenizationService;
    
    public void processCreditCardData(CreditCardData cardData) {
        
        // PCI-DSS Requirement 3: Protect stored cardholder data
        if (requiresStorage(cardData)) {
            // Tokenize PAN (Primary Account Number)
            String token = tokenizationService.tokenize(cardData.getPan());
            cardData.setPanToken(token);
            cardData.setPan(null); // Remove PAN from memory
        }
        
        // PCI-DSS Requirement 4: Encrypt transmission of cardholder data
        if (requiresTransmission(cardData)) {
            EncryptedData encryptedData = encryptionService.encrypt(
                cardData.toJson(), 
                EncryptionStrength.AES_256
            );
            cardData = CreditCardData.fromEncrypted(encryptedData);
        }
        
        // PCI-DSS Requirement 2: Do not use vendor-supplied defaults
        validateSecurityConfiguration();
        
        // Record compliance event
        recordPciComplianceEvent(cardData);
    }
    
    private void recordPciComplianceEvent(CreditCardData cardData) {
        
        PciComplianceEvent event = PciComplianceEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("CARDHOLDER_DATA_PROCESSING")
            .maskedPan(maskPan(cardData.getPanToken()))
            .processingTimestamp(Instant.now())
            .complianceRequirement("PCI-DSS-3.2.1")
            .encryptionApplied(true)
            .tokenizationApplied(true)
            .dataRetentionPeriod(getDataRetentionPeriod())
            .build();
            
        // Store in secure compliance log
        complianceEventRepository.save(event);
    }
}

/**
 * Basel III Capital Adequacy Compliance
 */
@Component
public class BaselIIIComplianceService {
    
    @Autowired
    private RiskCalculationEngine riskEngine;
    
    @Autowired
    private CapitalAdequacyService capitalService;
    
    public void validateBaselIIICompliance(ConsumerDefaultRecord record) {
        
        // Calculate risk-weighted assets
        RiskWeightedAssets rwa = riskEngine.calculateRiskWeightedAssets(record);
        
        // Validate capital adequacy ratios
        CapitalAdequacyRatios ratios = capitalService.calculateCapitalRatios(rwa);
        
        // Basel III minimum requirements
        validateMinimumCapitalRequirements(ratios);
        
        // Countercyclical capital buffer
        validateCountercyclicalBuffer(ratios, getCurrentEconomicCycle());
        
        // Record compliance assessment
        recordBaselIIIComplianceAssessment(record, rwa, ratios);
    }
    
    private void validateMinimumCapitalRequirements(CapitalAdequacyRatios ratios) {
        
        // Common Equity Tier 1 (CET1) minimum 4.5%
        if (ratios.getCet1Ratio().compareTo(BigDecimal.valueOf(0.045)) < 0) {
            throw new ComplianceViolationException(
                "CET1 ratio below Basel III minimum: " + ratios.getCet1Ratio()
            );
        }
        
        // Tier 1 capital ratio minimum 6%
        if (ratios.getTier1Ratio().compareTo(BigDecimal.valueOf(0.06)) < 0) {
            throw new ComplianceViolationException(
                "Tier 1 ratio below Basel III minimum: " + ratios.getTier1Ratio()
            );
        }
        
        // Total capital ratio minimum 8%
        if (ratios.getTotalCapitalRatio().compareTo(BigDecimal.valueOf(0.08)) < 0) {
            throw new ComplianceViolationException(
                "Total capital ratio below Basel III minimum: " + ratios.getTotalCapitalRatio()
            );
        }
    }
}
```

## 7. Performance and Scalability Architecture

### 7.1 High-Volume Data Processing Strategies

**Horizontal Scaling Patterns:**

```yaml
# Kubernetes Deployment Configuration
apiVersion: apps/v1
kind: Deployment
metadata:
  name: file-ingestion-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: file-ingestion-service
  template:
    metadata:
      labels:
        app: file-ingestion-service
    spec:
      containers:
      - name: file-ingestion-service
        image: file-ingestion-service:latest
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: SQL_LOADER_PARALLEL_DEGREE
          value: "4"
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: file-ingestion-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: file-ingestion-service
  minReplicas: 3
  maxReplicas: 10
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
```

**Database Performance Optimization:**

```sql
-- Partition Management for High-Volume Tables
BEGIN
  -- Create annual partitions with monthly subpartitions
  FOR i IN 1..12 LOOP
    EXECUTE IMMEDIATE 'ALTER TABLE CONSUMER_DEFAULT_STAGING 
      ADD PARTITION P_2025' || LPAD(i, 2, '0') || ' 
      VALUES LESS THAN (DATE ''2025-' || LPAD(i+1, 2, '0') || '-01'')
      TABLESPACE TBS_DEFAULT_DATA_' || LPAD(i, 2, '0');
  END LOOP;
END;
/

-- Parallel processing hints for large batch operations
SELECT /*+ PARALLEL(cds, 8) */ 
  COUNT(*) as total_records,
  COUNT(CASE WHEN error_flag = 'Y' THEN 1 END) as error_records,
  COUNT(CASE WHEN processed_flag = 'Y' THEN 1 END) as processed_records
FROM CONSUMER_DEFAULT_STAGING cds
WHERE batch_id = :batchId
  AND created_timestamp >= TRUNC(SYSDATE);

-- Materialized views for frequent aggregations
CREATE MATERIALIZED VIEW MV_DAILY_PROCESSING_SUMMARY
BUILD IMMEDIATE
REFRESH FAST ON COMMIT
AS
SELECT 
  TRUNC(created_timestamp) as processing_date,
  source_system,
  COUNT(*) as total_records,
  COUNT(CASE WHEN error_flag = 'Y' THEN 1 END) as error_count,
  COUNT(CASE WHEN processed_flag = 'Y' THEN 1 END) as processed_count,
  AVG(CASE WHEN processed_flag = 'Y' THEN 
    EXTRACT(EPOCH FROM (processed_timestamp - created_timestamp)) END) as avg_processing_time_seconds
FROM CONSUMER_DEFAULT_STAGING
GROUP BY TRUNC(created_timestamp), source_system;
```

### 7.2 Caching and Optimization Approaches

**Multi-Level Caching Strategy:**

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
            
        return builder.build();
    }
    
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(60))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}

@Component
public class PerformanceOptimizedConfigurationManager {
    
    // L1 Cache: Local application cache
    @Cacheable(value = "fieldMappings", key = "#sourceSystem + '_' + #fileType + '_' + #version")
    public FieldMappingConfig getFieldMapping(String sourceSystem, String fileType, String version) {
        return loadFromDatabase(sourceSystem, fileType, version);
    }
    
    // L2 Cache: Distributed Redis cache
    @Cacheable(value = "businessRules", key = "#ruleType + '_' + #version")
    public List<BusinessRule> getBusinessRules(String ruleType, String version) {
        return businessRuleRepository.findByTypeAndVersion(ruleType, version);
    }
    
    // Cache warming strategy
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCache() {
        // Pre-load frequently accessed configurations
        List<String> frequentSystems = Arrays.asList("CORE_BANKING", "LENDING_SYSTEM", "RISK_ENGINE");
        
        frequentSystems.parallelStream().forEach(system -> {
            try {
                getFieldMapping(system, "CONSUMER_DEFAULT", "LATEST");
                log.info("Warmed up cache for system: {}", system);
            } catch (Exception e) {
                log.warn("Failed to warm up cache for system: {}", system, e);
            }
        });
    }
}

@Component
public class DatabaseConnectionPoolOptimization {
    
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(primaryDbUrl);
        config.setUsername(primaryDbUsername);
        config.setPassword(primaryDbPassword);
        
        // Performance tuning
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(900000);
        config.setLeakDetectionThreshold(60000);
        
        // Oracle-specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        return new HikariDataSource(config);
    }
}
```

### 7.3 Monitoring and Observability

**Comprehensive Monitoring Stack:**

```java
@Component
public class PerformanceMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final Timer fileProcessingTimer;
    private final Counter recordsProcessedCounter;
    private final Gauge activeJobsGauge;
    
    public PerformanceMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.fileProcessingTimer = Timer.builder("file.processing.duration")
            .description("Time taken to process files")
            .tag("service", "file-ingestion")
            .register(meterRegistry);
            
        this.recordsProcessedCounter = Counter.builder("records.processed.total")
            .description("Total number of records processed")
            .register(meterRegistry);
            
        this.activeJobsGauge = Gauge.builder("jobs.active.current")
            .description("Current number of active processing jobs")
            .register(meterRegistry, this, PerformanceMetricsCollector::getActiveJobCount);
    }
    
    @EventListener
    public void handleFileProcessingCompleted(FileProcessingCompletedEvent event) {
        fileProcessingTimer.record(event.getProcessingDuration(), TimeUnit.MILLISECONDS);
        recordsProcessedCounter.increment(event.getRecordsProcessed());
        
        // Custom business metrics
        meterRegistry.counter("files.processed.by.source", 
            "source_system", event.getSourceSystem(),
            "status", event.getStatus().toString())
            .increment();
    }
    
    private double getActiveJobCount() {
        // Implementation to get current active job count
        return jobExecutionRepository.countByStatus(JobStatus.RUNNING);
    }
}

// Application Performance Monitoring Configuration
@Configuration
public class ObservabilityConfiguration {
    
    @Bean
    public TracingConfiguration tracingConfiguration() {
        return TracingConfiguration.builder()
            .serviceName("consumer-default-etl")
            .serviceVersion("1.0.0")
            .spanProcessor(BatchSpanProcessor.builder(
                JaegerGrpcSpanExporter.builder()
                    .setEndpoint("http://jaeger-collector:14250")
                    .build())
                .build())
            .build();
    }
    
    @Bean
    public HealthIndicator fileIngestionHealthIndicator() {
        return new FileIngestionHealthIndicator();
    }
    
    @Bean
    public HealthIndicator databaseHealthIndicator() {
        return new DatabaseHealthIndicator();
    }
}
```

## 8. Implementation Strategy

### 8.1 Development Phases and Dependencies

**Phase 1: Foundation (Weeks 1-4)**
- Core infrastructure setup (Kubernetes, databases, messaging)
- Base microservice templates with Spring Boot
- Security framework implementation (OAuth2, JWT)
- Basic monitoring and logging infrastructure

**Phase 2: Core Services (Weeks 5-8)**
- Configuration Service implementation
- File Ingestion Service with SQL*Loader integration
- Data Quality Service with validation framework
- Basic API Gateway and service mesh setup

**Phase 3: Business Logic (Weeks 9-12)**
- Transformation Service with business rules engine
- Event sourcing and audit trail implementation
- Advanced data lineage tracking
- Comprehensive error handling and recovery

**Phase 4: Integration and Testing (Weeks 13-16)**
- End-to-end integration testing
- Performance testing and optimization
- Security penetration testing
- Regulatory compliance validation

**Phase 5: Production Readiness (Weeks 17-20)**
- Production deployment automation
- Disaster recovery procedures
- Operational monitoring and alerting
- User training and documentation

### 8.2 Risk Mitigation Strategies

**Technical Risks:**
- **Database Performance Issues:** Implement comprehensive performance testing with realistic data volumes
- **Integration Complexity:** Use API contracts and consumer-driven contract testing
- **Security Vulnerabilities:** Regular security scans and penetration testing
- **Scalability Limitations:** Performance testing with 10x expected load

**Operational Risks:**
- **Skills Gap:** Dedicated training programs for Kubernetes, microservices, and Spring Boot
- **Deployment Complexity:** Automated CI/CD pipelines with rollback capabilities
- **Data Quality Issues:** Comprehensive data validation and monitoring
- **Regulatory Compliance:** Regular compliance audits and automated compliance checks

### 8.3 Success Metrics and KPIs

**Performance Metrics:**
- File processing throughput: >100,000 records/minute
- API response time: <500ms for 95% of requests
- System availability: >99.9% uptime
- Data processing accuracy: >99.99%

**Business Metrics:**
- Time-to-market for new data sources: <2 weeks
- Configuration change deployment time: <30 minutes
- Regulatory audit preparation time: <2 days
- Operational cost reduction: 20% vs. current system

This comprehensive architecture provides a robust, scalable, and compliant foundation for consumer default ETL processing in enterprise banking environments. The solution addresses all four user stories while ensuring regulatory compliance, operational excellence, and future extensibility.

The complete implementation includes detailed code examples, database schemas, security frameworks, and operational procedures necessary for successful enterprise deployment.
