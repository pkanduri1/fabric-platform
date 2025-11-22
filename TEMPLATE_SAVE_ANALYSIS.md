# Template Configuration Save Functionality - Complete Analysis

## Overview
This document provides a comprehensive analysis of the existing template configuration save functionality in the Fabric Platform. It documents the exact patterns used to save batch configurations, field mappings, and related data to understand how to replicate this for Template Studio's save functionality.

---

## 1. Controller Layer - Save Endpoints

### Location
`/Users/pavankanduri/claude-ws/fabric-platform-new/fabric-core/fabric-api/src/main/java/com/truist/batch/controller/TemplateController.java`

### Key Endpoint: Save Template Configuration

**Endpoint**: `POST /api/admin/templates/save-config`

**Method Signature**:
```java
@PostMapping("/save-config")
public ResponseEntity<Map<String, Object>> saveTemplateConfiguration(
        @RequestBody com.truist.batch.model.TemplateConfigDto config) {
    try {
        String configId = templateService.saveTemplateConfiguration(config);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("configId", configId);
        response.put("message", "Configuration saved successfully");

        log.info("Saved template configuration for job: {}, ID: {}", config.getJobName(), configId);
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        log.error("Error saving template configuration", e);
        return ResponseEntity.internalServerError().build();
    }
}
```

### Related Endpoint: Create Configuration From Template

**Endpoint**: `POST /api/admin/templates/{fileType}/{transactionType}/create-config`

**Method Signature**:
```java
@PostMapping("/{fileType}/{transactionType}/create-config")
@Transactional
public ResponseEntity<FieldMappingConfig> createConfigurationFromTemplate(
        @PathVariable String fileType,
        @PathVariable String transactionType,
        @RequestParam String sourceSystem,
        @RequestParam String jobName,
        @RequestParam(defaultValue = "system") String createdBy)
```

**Flow**:
1. Creates `FieldMappingConfig` from template
2. Saves configuration to BATCH_CONFIGURATIONS table via `configurationService.saveConfiguration(config)`
3. Returns the created configuration

---

## 2. Service Layer - Save Logic

### TemplateService Interface
`/Users/pavankanduri/claude-ws/fabric-platform-new/fabric-core/fabric-api/src/main/java/com/truist/batch/service/TemplateService.java`

Key method:
```java
/**
 * Save complete template configuration (Job + Fields + Query)
 */
String saveTemplateConfiguration(com.truist.batch.model.TemplateConfigDto config);
```

### TemplateServiceImpl - Implementation Details
`/Users/pavankanduri/claude-ws/fabric-platform-new/fabric-core/fabric-api/src/main/java/com/truist/batch/service/impl/TemplateServiceImpl.java`

**Method**: `saveTemplateConfiguration` (lines 71-119)

```java
@Override
@Transactional
public String saveTemplateConfiguration(TemplateConfigDto config) {
    log.info("Saving template configuration for fileType: {}, transactionType: {}",
        config.getFileType(), config.getTransactionType());

    // Template Studio saves/updates field template transformations only
    // NOT job configurations (those are created later when using the template)

    int updatedCount = 0;
    for (FieldTemplate field : config.getFields()) {
        // Update each field template with transformation settings
        // Only update the new Phase 2 columns we just added via Liquibase
        String updateSql =
            "UPDATE CM3INT.FIELD_TEMPLATES SET " +
            "  SOURCE_FIELD = ?, " +
            "  TRANSFORMATION_TYPE = ?, " +
            "  VALUE = ?, " +
            "  TRANSFORMATION_CONFIG = ?, " +
            "  MODIFIED_BY = ?, " +
            "  MODIFIED_DATE = SYSDATE " +
            "WHERE FILE_TYPE = ? " +
            "  AND TRANSACTION_TYPE = ? " +
            "  AND FIELD_NAME = ?";

        int rowsAffected = jdbcTemplate.update(updateSql,
            field.getSourceField(),
            field.getTransformationType(),
            field.getValue(),
            field.getTransformationConfig(),
            config.getCreatedBy(),
            config.getFileType(),
            config.getTransactionType(),
            field.getFieldName()
        );

        if (rowsAffected > 0) {
            updatedCount++;
            log.debug("Updated field template: {}/{}/{}",
                config.getFileType(), config.getTransactionType(), field.getFieldName());
        }
    }

    log.info("Updated {} field templates for {}/{}",
        updatedCount, config.getFileType(), config.getTransactionType());

    // Return a success indicator
    return config.getFileType() + "_" + config.getTransactionType() + "_" + updatedCount;
}
```

### ConfigurationService - Configuration Save Logic
`/Users/pavankanduri/claude-ws/fabric-platform-new/fabric-core/fabric-api/src/main/java/com/truist/batch/service/ConfigurationService.java` (Interface)

`/Users/pavankanduri/claude-ws/fabric-platform-new/fabric-core/fabric-api/src/main/java/com/truist/batch/service/impl/ConfigurationServiceImpl.java` (Implementation)

**Method**: `saveConfiguration` (lines 192-235)

```java
@Override
@Transactional
public String saveConfiguration(FieldMappingConfig config) {
    log.info("💾 Saving configuration for {}/{}", config.getSourceSystem(), config.getJobName());

    try {
        // 1. Validate configuration before saving
        ValidationResult validation = validateConfiguration(config);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Configuration validation failed: " + 
                String.join(", ", validation.getErrors()));
        }

        // 2. Save to database with audit trail
        BatchConfiguration entity = saveToDatabase(config, "system");

        // 3. Save master query association to TEMPLATE_MASTER_QUERY_MAPPING
        saveMasterQueryAssociation(config, entity.getId(), "system");

        // 4. Save field mappings to TEMPLATE_SOURCE_MAPPINGS
        saveFieldMappingsToTemplateTable(config, "system");

        // 5. Generate and save YAML file
        String yamlContent = yamlGenerationService.generateYamlFromConfiguration(config);
        String yamlFilePath = saveYamlToFile(config, yamlContent);

        // 6. Create audit entry (temporarily disabled for testing)
        try {
            createAuditEntry(entity.getId(), "SAVE", null, 
                objectMapper.writeValueAsString(config), "system");
        } catch (Exception auditError) {
            log.warn("⚠️ Audit entry creation failed but main save succeeded: {}", auditError.getMessage());
            // Continue without failing the main operation
        }

        log.info("✅ Configuration saved successfully: DB ID={}, YAML Path={}", 
                entity.getId(), yamlFilePath);

        return "Configuration saved successfully";

    } catch (Exception e) {
        log.error("❌ Failed to save configuration: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to save configuration", e);
    }
}
```

#### Helper Methods in ConfigurationServiceImpl

**1. Save to Database** (lines 365-377):
```java
private BatchConfiguration saveToDatabase(FieldMappingConfig config, String userId) throws Exception {
    BatchConfiguration entity = new BatchConfiguration();
    entity.setId(generateConfigId(config));
    entity.setSourceSystem(config.getSourceSystem());
    entity.setJobName(config.getJobName());
    entity.setTransactionType(config.getTransactionType() != null ? config.getTransactionType() : "200");
    entity.setConfigurationJson(objectMapper.writeValueAsString(config));
    entity.setCreatedBy(userId);
    entity.setCreatedDate(LocalDateTime.now());
    entity.setVersion("1");

    return configDao.save(entity);
}
```

**2. Save Field Mappings to Template Table** (lines 597-666):
```java
private void saveFieldMappingsToTemplateTable(FieldMappingConfig config, String userId) {
    if (config.getFieldMappings() == null || config.getFieldMappings().isEmpty()) {
        log.debug("⏭️ No field mappings provided, skipping template source mappings save");
        return;
    }
    
    log.info("💾 Saving {} field mappings to TEMPLATE_SOURCE_MAPPINGS for {}/{}", 
            config.getFieldMappings().size(), config.getSourceSystem(), config.getJobName());
    
    try {
        // Delete existing mappings for this configuration
        templateSourceMappingRepository.deleteByFileTypeAndTransactionTypeAndSourceSystemIdAndJobName(
            config.getJobName(), // fileType maps to jobName
            config.getTransactionType() != null ? config.getTransactionType() : "200",
            config.getSourceSystem(),
            config.getJobName()
        );
        
        // Create new mappings for each field
        List<TemplateSourceMappingEntity> mappingEntities = new ArrayList<>();
        
        for (FieldMapping fieldMapping : config.getFieldMappings()) {
            TemplateSourceMappingEntity entity = new TemplateSourceMappingEntity(
                config.getJobName(), // fileType
                config.getTransactionType() != null ? config.getTransactionType() : "200", // transactionType
                config.getSourceSystem(), // sourceSystemId
                config.getJobName(), // jobName
                fieldMapping.getTargetField(), // targetFieldName
                fieldMapping.getSourceField(), // sourceFieldName
                userId // createdBy
            );
            
            // Set additional field properties
            entity.setTransformationType(fieldMapping.getTransformationType());
            entity.setTargetPosition(fieldMapping.getTargetPosition());
            entity.setLength(fieldMapping.getLength());
            entity.setDataType(fieldMapping.getDataType());
            
            // Set transformation configuration if available
            if (fieldMapping.getValue() != null || fieldMapping.getDefaultValue() != null) {
                Map<String, Object> transformConfig = new HashMap<>();
                if (fieldMapping.getValue() != null) {
                    transformConfig.put("value", fieldMapping.getValue());
                }
                if (fieldMapping.getDefaultValue() != null) {
                    transformConfig.put("defaultValue", fieldMapping.getDefaultValue());
                }
                try {
                    entity.setTransformationConfig(objectMapper.writeValueAsString(transformConfig));
                } catch (Exception jsonError) {
                    log.warn("⚠️ Failed to serialize transformation config for field {}: {}", 
                           fieldMapping.getTargetField(), jsonError.getMessage());
                }
            }
            
            mappingEntities.add(entity);
        }
        
        // Save all mappings
        templateSourceMappingRepository.saveAll(mappingEntities);
        
        log.info("✅ Successfully saved {} field mappings to TEMPLATE_SOURCE_MAPPINGS", mappingEntities.size());
        
    } catch (Exception e) {
        log.error("❌ Failed to save field mappings to TEMPLATE_SOURCE_MAPPINGS for {}/{}: {}", 
                 config.getSourceSystem(), config.getJobName(), e.getMessage(), e);
        // Don't fail the main operation for field mapping errors
        log.warn("⚠️ Continuing with configuration save despite field mapping save failure");
    }
}
```

**3. Save Master Query Association** (lines 559-592):
```java
private void saveMasterQueryAssociation(FieldMappingConfig config, String configId, String userId) {
    if (config.getMasterQueryId() == null) {
        log.debug("⏭️ No master query ID provided, skipping master query association save");
        return;
    }
    
    log.info("💾 Saving master query association for config {} with master query {}", configId, config.getMasterQueryId());
    
    try {
        // Create the master query mapping entity
        TemplateMasterQueryMappingEntity mapping = new TemplateMasterQueryMappingEntity(
            configId,
            config.getMasterQueryId().toString(),
            config.getJobName() + "_query", // Default query name
            "", // Query SQL will be populated from MASTER_QUERY_CONFIG
            userId
        );
        
        // Set additional properties
        mapping.setQueryDescription("Auto-generated mapping for " + config.getSourceSystem() + "/" + config.getJobName());
        mapping.setSecurityClassification("INTERNAL");
        mapping.setCorrelationId(generateCorrelationId(config));
        
        // Save to database
        templateMasterQueryMappingDao.save(mapping);
        
        log.info("✅ Master query association saved successfully: mapping ID = {}", mapping.getMappingId());
        
    } catch (Exception e) {
        log.error("❌ Failed to save master query association for config {}: {}", configId, e.getMessage(), e);
        // Don't fail the main operation for master query association errors
        log.warn("⚠️ Continuing with configuration save despite master query association failure");
    }
}
```

---

## 3. Data Models and DTOs

### TemplateConfigDto
**Location**: `/Users/pavankanduri/claude-ws/fabric-platform-new/fabric-core/fabric-api/src/main/java/com/truist/batch/model/TemplateConfigDto.java`

```java
public class TemplateConfigDto {
    private String sourceSystem;
    private String jobName;
    private String fileType;
    private String transactionType;
    private String description;
    private String createdBy;
    private List<FieldTemplate> fields;
    private String masterQuery;
    
    // With nested MasterQueryConfig class
    public static class MasterQueryConfig {
        private String querySql;
        private String queryName;
        private String queryDescription;
        // ... getters/setters
    }
}
```

**Usage**: Contains complete template configuration including fields and query metadata.

### FieldMappingConfig
**Location**: `/Users/pavankanduri/claude-ws/fabric-platform-new/fabric-core/fabric-api/src/main/java/com/truist/batch/model/FieldMappingConfig.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldMappingConfig {
    private String id;
    private String sourceSystem;
    private String jobName;
    private String transactionType;
    private List<FieldMapping> fieldMappings;
    private String description;
    private String templateId;
    private Long masterQueryId;
    private String masterQuery;  // Added: Stores actual SQL query text from Template Studio
    private boolean active = true;
    private int version;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime lastModified;
    private String modifiedBy;
}
```

**Usage**: Used to save field mappings and configuration details to BATCH_CONFIGURATIONS table as JSON.

**Bug Fix (Nov 22, 2025)**: Added `masterQuery` String field to properly store SQL query text entered in Template Studio. Previously, the query was being dropped due to an attempt to parse SQL text as Long ID.

### FieldMapping
**Location**: `/Users/pavankanduri/claude-ws/fabric-platform-new/fabric-core/fabric-api/src/main/java/com/truist/batch/model/FieldMapping.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldMapping {
    @JsonProperty("fieldName")
    private String fieldName;
    
    @JsonProperty("targetField")
    private String targetField;
    
    @JsonProperty("sourceField")
    private String sourceField;
    
    @JsonProperty("from")
    private String from;
    
    @JsonProperty("targetPosition")
    private int targetPosition;
    
    @JsonProperty("length")
    private int length;
    
    @JsonProperty("dataType")
    private String dataType;
    
    @JsonProperty("format")
    private String format;
    
    @JsonProperty("sourceFormat")
    private String sourceFormat;
    
    @JsonProperty("targetFormat")
    private String targetFormat;
    
    @JsonProperty("transformationType")
    private String transformationType;
    
    @JsonProperty("value")
    private String value;
    
    @JsonProperty("defaultValue")
    private String defaultValue;
    
    @JsonProperty("transform")
    private String transform;
    
    @JsonProperty("pad")
    private String pad;
    
    @JsonProperty("padChar")
    private String padChar;
    
    @JsonProperty("conditions")
    private List<Condition> conditions;
    
    @JsonProperty("composite")
    private boolean composite;
    
    @JsonProperty("sources")
    private List<String> sources;
    
    @JsonProperty("delimiter")
    private String delimiter;
    
    @JsonProperty("expression")
    private String expression;
}
```

**Usage**: Represents individual field mappings with transformation details, stored both in FieldMappingConfig and TEMPLATE_SOURCE_MAPPINGS table.

### FieldTemplate
**Location**: `/Users/pavankanduri/claude-ws/fabric-platform-new/fabric-core/fabric-api/src/main/java/com/truist/batch/model/FieldTemplate.java`

```java
public class FieldTemplate {
    private String fileType;
    private String transactionType;
    private String fieldName;
    private Integer targetPosition;
    private Integer length;
    private String dataType;
    private String format;
    private String required;
    private String description;
    private String enabled;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private String lastModifiedBy;
    private String modifiedDate;
    private LocalDateTime lastModifiedDate;
    private String defaultValue;
    private String validationRule;
    private Integer version;
    private String transformationType;
    private String transformationConfig;
    private String value;
    private String sourceField;
}
```

**Usage**: DTO for field template data transfer, contains transformation metadata.

---

## 4. Database Tables Involved

### BATCH_CONFIGURATIONS Table
**Purpose**: Stores complete job configurations as JSON

**Key Fields**:
- ID: Configuration identifier
- SOURCE_SYSTEM: Source system name
- JOB_NAME: Job identifier
- TRANSACTION_TYPE: Type of transaction
- CONFIGURATION_JSON: Complete FieldMappingConfig serialized as JSON
- CREATED_BY: Creator username
- CREATED_DATE: Creation timestamp
- VERSION: Configuration version

**Saved via**: `configDao.save(entity)` in `saveToDatabase()`

### FIELD_TEMPLATES Table
**Purpose**: Stores template field definitions with transformation metadata

**Key Fields**:
- FILE_TYPE: File type identifier
- TRANSACTION_TYPE: Transaction type
- FIELD_NAME: Field name (composite primary key)
- TARGET_POSITION: Field position in output
- LENGTH: Field length
- DATA_TYPE: Field data type
- FORMAT: Format specification
- REQUIRED: Required flag
- DESCRIPTION: Field description
- SOURCE_FIELD: Source field name (Phase 2)
- TRANSFORMATION_TYPE: Type of transformation (Phase 2)
- VALUE: Constant value or default (Phase 2)
- TRANSFORMATION_CONFIG: JSON configuration (Phase 2)
- CREATED_BY: Creator
- CREATED_DATE: Creation date
- MODIFIED_BY: Last modifier
- MODIFIED_DATE: Last modification date
- VERSION: Version number
- ENABLED: Enabled flag

**Updated via**: Direct JDBC UPDATE in `TemplateServiceImpl.saveTemplateConfiguration()`

### TEMPLATE_SOURCE_MAPPINGS Table
**Purpose**: Stores field-level source-to-target mappings and transformation details

**Key Fields**:
- FILE_TYPE: File type
- TRANSACTION_TYPE: Transaction type
- SOURCE_SYSTEM_ID: Source system
- JOB_NAME: Job name
- TARGET_FIELD_NAME: Target field name
- SOURCE_FIELD_NAME: Source field name
- TRANSFORMATION_TYPE: Transformation type
- TARGET_POSITION: Field position
- LENGTH: Field length
- DATA_TYPE: Data type
- TRANSFORMATION_CONFIG: JSON transformation configuration
- CREATED_BY: Creator
- CREATED_DATE: Creation date
- MODIFIED_BY: Last modifier
- MODIFIED_DATE: Last modification date

**Saved via**: `templateSourceMappingRepository.saveAll(mappingEntities)` in `saveFieldMappingsToTemplateTable()`

### TEMPLATE_MASTER_QUERY_MAPPING Table
**Purpose**: Links templates to master queries for dynamic data retrieval

**Key Fields**:
- MAPPING_ID: Mapping identifier
- CONFIG_ID: Configuration identifier
- MASTER_QUERY_ID: Master query identifier
- QUERY_NAME: Query name
- QUERY_SQL: Query SQL
- QUERY_DESCRIPTION: Query description
- SECURITY_CLASSIFICATION: Security level
- CORRELATION_ID: Audit correlation ID
- CREATED_BY: Creator
- CREATED_DATE: Creation date

**Saved via**: `templateMasterQueryMappingDao.save(mapping)` in `saveMasterQueryAssociation()`

---

## 5. Key Patterns and Conventions

### Pattern 1: Complete Save Flow
1. **Validate** - Check configuration validity before saving
2. **Save Main Config** - Store to BATCH_CONFIGURATIONS as JSON
3. **Save Field Mappings** - Update FIELD_TEMPLATES with transformation details
4. **Save Source Mappings** - Create entries in TEMPLATE_SOURCE_MAPPINGS
5. **Save Master Query Association** - Link to master query if provided
6. **Generate YAML** - Create YAML files for batch framework
7. **Audit** - Log the operation (non-critical failure allowed)

### Pattern 2: JSON Serialization of FieldMappingConfig
- Use `objectMapper.writeValueAsString(config)` to serialize
- Stored in BATCH_CONFIGURATIONS.CONFIGURATION_JSON
- Complete field mappings with transformation types preserved
- Allows round-trip deserialization

### Pattern 3: Field Mapping Storage
**In BATCH_CONFIGURATIONS (JSON)**:
```json
{
  "sourceSystem": "ENCORE",
  "jobName": "atoctran",
  "transactionType": "200",
  "fieldMappings": [
    {
      "fieldName": "record_type",
      "targetField": "record_type",
      "sourceField": null,
      "targetPosition": 1,
      "length": 2,
      "dataType": "string",
      "transformationType": "constant",
      "value": "00",
      "defaultValue": null,
      "pad": "right"
    },
    {
      "fieldName": "account_number",
      "targetField": "account_number",
      "sourceField": "account_number",
      "targetPosition": 3,
      "length": 12,
      "dataType": "string",
      "transformationType": "source",
      "value": null,
      "defaultValue": null,
      "pad": "right"
    }
  ]
}
```

**In TEMPLATE_SOURCE_MAPPINGS (relational)**:
```
FILE_TYPE | TRANSACTION_TYPE | SOURCE_SYSTEM | JOB_NAME | TARGET_FIELD | SOURCE_FIELD | TRANSFORMATION_TYPE | TARGET_POSITION | LENGTH | DATA_TYPE | TRANSFORMATION_CONFIG
```

### Pattern 4: Transformation Configuration
Stored as JSON when value or defaultValue exist:
```java
Map<String, Object> transformConfig = new HashMap<>();
if (fieldMapping.getValue() != null) {
    transformConfig.put("value", fieldMapping.getValue());
}
if (fieldMapping.getDefaultValue() != null) {
    transformConfig.put("defaultValue", fieldMapping.getDefaultValue());
}
entity.setTransformationConfig(objectMapper.writeValueAsString(transformConfig));
```

Result in database:
```json
{
  "value": "200",
  "defaultValue": null
}
```

### Pattern 5: Error Handling
- Non-critical operations (master query, audit, field mappings) do not fail the main save
- Log warnings but continue processing
- Main configuration save is critical

### Pattern 6: Audit Trail
- `generateCorrelationId(config)` creates unique correlation IDs
- Format: `cfg_{sourceSystem}_{jobName}_{timestamp}`
- Audit entries logged with action type, old/new values, and user

---

## 6. Spring Configuration and Annotations

### Required Annotations
```java
@Transactional          // For transaction management
@Service                // Service component annotation
@Slf4j                  // Lombok logging annotation

// In controller
@PostMapping("/endpoint")
@RequestBody            // For request body mapping
@PathVariable           // For URL path variables
@RequestParam           // For query parameters
@Transactional          // For transactional endpoints
```

### Dependencies Used
```java
// ObjectMapper for JSON serialization
com.fasterxml.jackson.databind.ObjectMapper

// JdbcTemplate for direct SQL
org.springframework.jdbc.core.JdbcTemplate

// Transaction management
org.springframework.transaction.annotation.Transactional

// Logging
lombok.extern.slf4j.Slf4j
```

---

## 7. Validation Strategy

### Pre-Save Validation
```java
ValidationResult validation = validateConfiguration(config);
if (!validation.isValid()) {
    throw new IllegalArgumentException("Configuration validation failed: " + 
        String.join(", ", validation.getErrors()));
}
```

### YamlGenerationService Validation
- Used for comprehensive validation before YAML generation
- Checks all field mappings and transformation types
- Validates total record length doesn't exceed limits

---

## 8. Response Format

### Successful Save Response
```json
{
  "success": true,
  "configId": "encore_atoctran_200",
  "message": "Configuration saved successfully"
}
```

### From Template Service
```
"fileType_transactionType_updatedCount"
```

---

## 9. Implementation Pattern for New Features

### To replicate this pattern for Template Studio:

1. **Create DTO** (if not already exists)
   - Input: Template configuration data
   - Include: fileType, transactionType, fields, masterQuery, etc.

2. **Create Controller Endpoint**
   ```java
   @PostMapping("/your-save-endpoint")
   @Transactional
   public ResponseEntity<Map<String, Object>> saveConfiguration(@RequestBody YourConfigDto config) {
       try {
           String result = service.save(config);
           Map<String, Object> response = new HashMap<>();
           response.put("success", true);
           response.put("id", result);
           response.put("message", "Saved successfully");
           return ResponseEntity.ok(response);
       } catch (Exception e) {
           log.error("Error saving", e);
           return ResponseEntity.internalServerError().build();
       }
   }
   ```

3. **Implement Service Method**
   - Validate input
   - Save to primary table (BATCH_CONFIGURATIONS)
   - Save to related tables (FIELD_TEMPLATES, TEMPLATE_SOURCE_MAPPINGS)
   - Handle optional operations gracefully
   - Generate YAML if needed
   - Log audit trail

4. **Use JdbcTemplate for Database Access**
   ```java
   String updateSql = "UPDATE TABLE SET col = ? WHERE key = ?";
   jdbcTemplate.update(updateSql, value, key);
   ```

5. **Serialize Complex Objects to JSON**
   ```java
   String json = objectMapper.writeValueAsString(config);
   entity.setConfigJson(json);
   ```

6. **Handle Transformation Metadata**
   ```java
   Map<String, Object> transformConfig = new HashMap<>();
   transformConfig.put("value", fieldMapping.getValue());
   transformConfig.put("defaultValue", fieldMapping.getDefaultValue());
   entity.setTransformationConfig(objectMapper.writeValueAsString(transformConfig));
   ```

---

## Summary Table of Key Methods

| Component | Location | Method | Purpose |
|-----------|----------|--------|---------|
| Controller | TemplateController | saveTemplateConfiguration | Entry point for save request |
| TemplateService | TemplateServiceImpl | saveTemplateConfiguration | Update FIELD_TEMPLATES with transformation data |
| ConfigService | ConfigurationServiceImpl | saveConfiguration | Main save logic (orchestrator) |
| ConfigService | ConfigurationServiceImpl | saveToDatabase | Save to BATCH_CONFIGURATIONS |
| ConfigService | ConfigurationServiceImpl | saveFieldMappingsToTemplateTable | Save to TEMPLATE_SOURCE_MAPPINGS |
| ConfigService | ConfigurationServiceImpl | saveMasterQueryAssociation | Save to TEMPLATE_MASTER_QUERY_MAPPING |
| DAO | BatchConfigurationDao | save | Insert/update batch configuration |
| Repository | TemplateSourceMappingRepository | saveAll | Bulk save source mappings |
| Repository | TemplateSourceMappingRepository | deleteByFileTypeAndTransactionType... | Delete old mappings |

