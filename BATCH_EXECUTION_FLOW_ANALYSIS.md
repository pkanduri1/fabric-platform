# Batch Execution Flow Analysis - Integration Points for Phase 2 Field Transformations

**Document Status**: Comprehensive Architecture Analysis  
**Last Updated**: 2025-11-21  
**Scope**: Identify batch execution pipeline and field transformation integration points for composite/conditional logic (Phase 2)

---

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [Batch Execution Entry Points](#batch-execution-entry-points)
3. [Data Processing Pipeline](#data-processing-pipeline)
4. [Field Transformation Architecture](#field-transformation-architecture)
5. [Current Transformation Capabilities](#current-transformation-capabilities)
6. [Phase 2 Integration Points](#phase-2-integration-points)
7. [Key Integration Recommendations](#key-integration-recommendations)

---

## Executive Summary

The Fabric Platform implements a **Spring Batch-driven job execution system** with a **three-layer transformation pipeline**:

```
User Request (JobExecutionRequest)
    ↓
JobExecutionService (Orchestration Layer)
    ↓
GenericProcessor + EnhancedGenericProcessor (Data Transformation Layer)
    ↓
YamlMappingService (Field Transformation Layer) ← **Phase 2 Integration Point**
    ↓
GenericWriter → FixedWidthFileWriter (Output Layer)
    ↓
Output File (Fixed-Width Format)
```

**Current State**: Basic field transformations (constant, source, blank) + composite and conditional logic infrastructure exists but **is not fully implemented in execution flow**

**Phase 2 Opportunity**: Enhance YamlMappingService to fully realize composite and conditional field transformations during batch execution

---

## Batch Execution Entry Points

### 1. JobExecutionService (Primary Orchestrator)
**Location**: `/fabric-core/fabric-api/src/main/java/com/truist/batch/service/JobExecutionService.java`  
**Lines**: 22-633  
**Role**: Enterprise-grade job execution management with security and audit trail

#### Key Methods:

| Method | Lines | Purpose |
|--------|-------|---------|
| `executeJob()` | 70-106 | Main entry point for job execution with comprehensive validation |
| `performActualExecution()` | 405-459 | Delegates to batch processing framework |
| `createExecutionRecord()` | 316-353 | Creates tracking record in database |

#### Execution Flow:
```
executeJob(request, user)
├── validateExecutionRequest(request, user)         [Line 79]
├── configRepository.findById(configId)             [Line 82]
├── createExecutionRecord(config, request, ...)     [Line 88]
├── performPreExecutionSetup(config, request, ...)  [Line 91]
├── if dryRun: performDryRunValidation()            [Line 95]
└── else: performActualExecution()                  [Line 98]
    └── **INTEGRATION POINT**: Actual batch processing starts here
```

**Key Finding**: `performActualExecution()` contains TODO comment at line 416:
```java
// TODO: Implement actual job execution logic
// This would integrate with the batch processing framework
// For now, simulate successful execution
Thread.sleep(1000); // Simulate processing time
```

This is where batch processors are invoked.

---

### 2. Job Execution Request DTO
**Location**: `/fabric-core/fabric-api/src/main/java/com/truist/batch/dto/JobExecutionRequest.java`  
**Lines**: 14-233  
**Role**: Standardized request contract with enterprise security features

#### Critical Fields for Phase 2:
```java
@NotBlank
private String configId;                    // Configuration to execute (Line 49)

private Map<String, Object> executionParameters;  // Runtime overrides (Line 75)

private Boolean dryRun = false;             // Validation-only mode (Line 172)

private Boolean enableDataLineage = true;   // Tracking flag (Line 161)

// Business context
private String businessJustification;       // SOX compliance (Line 83)
private Map<String, Object> resourceLimits; // Performance controls (Line 151)
```

---

## Data Processing Pipeline

### Pipeline Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                    BATCH EXECUTION PIPELINE                       │
└──────────────────────────────────────────────────────────────────┘

LAYER 1: REQUEST & ORCHESTRATION
    JobExecutionRequest
    ↓
    JobExecutionService.executeJob()
    ↓
    ManualJobConfigRepository.findById(configId)

LAYER 2: CONFIGURATION LOADING
    ManualJobConfigEntity (Database Configuration)
    ↓
    JsonMappingService.processJobConfiguration()
    ↓
    FieldMappingConfig Model

LAYER 3: DATA ACQUISITION
    MasterQueryService.executeQuery(masterQueryId)
    ↓
    JDBC Execute SQL → Source Data (Map<String,Object>)
    ↓
    For each row: [FIELD1: value1, FIELD2: value2, ...]

LAYER 4: FIELD TRANSFORMATION (Phase 2 Focus)
    GenericProcessor / EnhancedGenericProcessor
    ↓
    For each FieldMapping:
        YamlMappingService.transformField()
        ├── Resolve Source Values
        ├── Apply Composite Logic (if needed)
        ├── Evaluate Conditional Logic (if needed)
        ├── Apply Formatting/Padding
        └── Return Transformed Value
    ↓
    Output: Map<String,Object> with transformed fields

LAYER 5: OUTPUT GENERATION
    GenericWriter / FixedWidthFileWriter
    ↓
    Format as Fixed-Width String
    ↓
    Write to Output File
```

---

## Field Transformation Architecture

### YamlMappingService - Core Transformation Engine
**Location**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/mapping/YamlMappingService.java`  
**Lines**: 27-357  
**Role**: Central hub for all field transformations with support for multiple types

#### Main Transformation Method (CRITICAL FOR PHASE 2)

**Method**: `transformField()`  
**Lines**: 102-148  
**Signature**: `public String transformField(Map<String, Object> row, FieldMapping mapping)`

```java
// Lines 102-148: Transformation Type Dispatch
public String transformField(Map<String, Object> row, FieldMapping mapping) {
    String value = "";
    String transformationType = Optional.ofNullable(mapping.getTransformationType())
        .orElse("").toLowerCase();
    
    switch (transformationType) {
        case "constant":           // Line 108
            value = mapping.getValue();
            if (value == null) value = mapping.getDefaultValue();
            break;
            
        case "source":             // Line 116
            value = resolveValue(mapping.getSourceField(), row, 
                                mapping.getDefaultValue());
            break;
            
        case "composite":          // Line 120 - **PHASE 2 FOCUS**
            value = handleComposite(mapping.getSources(), row, 
                                   mapping.getTransform(), 
                                   mapping.getDelimiter(), 
                                   mapping.getDefaultValue());
            break;
            
        case "conditional":        // Line 125 - **PHASE 2 FOCUS**
            value = evaluateConditional(mapping.getConditions(), row, 
                                       mapping.getDefaultValue());
            break;
            
        case "blank":              // Line 129
            value = mapping.getDefaultValue();
            break;
            
        default:
            value = mapping.getDefaultValue();
    }
    
    // Apply padding if field has fixed length
    if (mapping.getLength() > 0) {
        return FormatterUtil.pad(value, mapping);  // Line 144
    }
    
    return value;
}
```

---

### Transformation Type 1: COMPOSITE Fields
**Method**: `handleComposite()`  
**Lines**: 181-210

#### Current Implementation:
```java
// Lines 187-209: Supports SUM and CONCAT
private String handleComposite(List<Map<String, String>> sources, 
                              Map<String, Object> row, 
                              String transform, String delimiter, 
                              String defaultValue) {
    
    if ("sum".equalsIgnoreCase(transform)) {
        // Sum numeric values from multiple source fields
        double sum = sources.stream()
            .mapToDouble(s -> {
                String fieldName = s.get("sourceField");
                Object val = row.get(fieldName.trim());
                try {
                    return val != null ? Double.parseDouble(val.toString()) : 0.0;
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }).sum();
        return String.valueOf(sum);
        
    } else if ("concat".equalsIgnoreCase(transform)) {
        // Concatenate values with optional delimiter
        String actualDelimiter = (delimiter != null) ? delimiter : "";
        return sources.stream()
            .map(s -> {
                String fieldName = s.get("sourceField");
                return Optional.ofNullable(row.get(fieldName.trim()))
                    .map(Object::toString).orElse("");
            })
            .collect(Collectors.joining(actualDelimiter));
    }
    
    return defaultValue != null ? defaultValue : "";
}
```

**Limitations**:
- Only supports SUM and CONCAT
- No average, min, max operations
- No string manipulation (uppercase, substring, etc.)
- No formatting options for composite results

---

### Transformation Type 2: CONDITIONAL Fields
**Method**: `evaluateConditional()`  
**Lines**: 216-251

#### Current Implementation:
```java
// Lines 216-251: IF-THEN-ELSE with ELSE-IF support
private String evaluateConditional(List<Condition> conditions, 
                                  Map<String, Object> row, 
                                  String defaultValue) {
    if (conditions == null || conditions.isEmpty()) {
        return defaultValue != null ? defaultValue : "";
    }
    
    Condition mainCondition = conditions.get(0);
    
    // 1. Check main IF condition
    if (ifExpr != null && !ifExpr.isEmpty() && 
        evaluateExpression(ifExpr, row)) {
        return resolveValue(thenVal, row, thenVal);
    }
    
    // 2. Check ELSE-IF conditions
    if (elseIfConditions != null && !elseIfConditions.isEmpty()) {
        for (Condition elseIfCondition : elseIfConditions) {
            if (evaluateExpression(elseIfCondition.getIfExpr(), row)) {
                return resolveValue(elseIfCondition.getThen(), row, ...);
            }
        }
    }
    
    // 3. Apply ELSE value
    if (elseVal != null && !elseVal.isEmpty()) {
        return resolveValue(elseVal, row, elseVal);
    }
    
    return defaultValue != null ? defaultValue : "";
}
```

**Expression Evaluation**: `evaluateExpression()`  
**Lines**: 257-333

Supports simple logical expressions:
- **Operators**: `==`, `!=`, `<`, `>`, `<=`, `>=`
- **Logical Operators**: `&&` (AND), `||` (OR)
- **Unary Operator**: `!` (NOT)
- **Format**: `FIELD operator 'value'` or `FIELD operator "value"`
- **Examples**:
  ```
  status == 'ACTIVE'
  amount > 1000
  type != 'PENDING' || priority == 'HIGH'
  ```

---

## Processing Flow: Processors

### 1. GenericProcessor (Original)
**Location**: `/fabric-core/fabric-batch/src/main/java/com/truist/batch/processor/GenericProcessor.java`  
**Lines**: 21-71  
**Role**: Spring Batch ItemProcessor for YAML-configured jobs

```java
// Lines 49-70: Process method
public Map<String, Object> process(Map<String, Object> item) throws Exception {
    String txnType = fileConfig.getTransactionType();
    
    // Get YAML mapping for transaction type
    YamlMapping mapping = mappingService.getMapping(
        fileConfig.getTemplate(), txnType);  // Line 55
    
    // Sort field mappings by target position
    List<FieldMapping> fields = mapping.getFields().entrySet().stream()
        .map(Map.Entry::getValue)
        .sorted(Comparator.comparingInt(FieldMapping::getTargetPosition))
        .collect(Collectors.toList());
    
    // Transform each field
    Map<String, Object> output = new LinkedHashMap<>();
    for (FieldMapping m : fields) {
        String value = mappingService.transformField(item, m);  // **Key Line 64**
        output.put(m.getTargetField(), 
                   value != null ? value : m.getDefaultValue());
    }
    return output;
}
```

**Key Integration Point**: Line 64  
- **YAML Configuration**: Loaded from classpath files
- **Field Transformation**: Delegated to YamlMappingService.transformField()
- **Output Format**: Preserves order via LinkedHashMap

---

### 2. EnhancedGenericProcessor (Database-Aware)
**Location**: `/fabric-core/fabric-batch/src/main/java/com/truist/batch/processor/EnhancedGenericProcessor.java`  
**Lines**: 20-115  
**Role**: Extended processor supporting both YAML and JSON database configurations

```java
// Lines 52-95: Process method with dual source support
public Map<String, Object> process(Map<String, Object> item) throws Exception {
    
    YamlMapping mapping;
    
    // Use either JSON (database) or YAML (classpath) configuration
    if (configId != null && !configId.isEmpty() && !"null".equals(configId)) {
        // JSON from database
        mapping = jsonMappingService.getYamlMappingFromJson(
            configId, transactionType);  // Line 64
        usingJsonConfig = true;
    } else if (fileConfig != null && fileConfig.getTemplate() != null) {
        // YAML from classpath
        String txnType = transactionType != null ? transactionType : 
                        fileConfig.getTransactionType();
        mapping = yamlMappingService.getMapping(
            fileConfig.getTemplate(), txnType);  // Line 70
    }
    
    // Transform fields (same logic as GenericProcessor)
    List<FieldMapping> fields = mapping.getFields().entrySet().stream()
        .map(Map.Entry::getValue)
        .sorted(Comparator.comparingInt(FieldMapping::getTargetPosition))
        .collect(Collectors.toList());
    
    Map<String, Object> output = new LinkedHashMap<>();
    for (FieldMapping fieldMapping : fields) {
        String value;
        
        if (usingJsonConfig) {
            value = jsonMappingService.transformField(item, fieldMapping);  // Line 91
        } else {
            value = yamlMappingService.transformField(item, fieldMapping);  // Line 94
        }
        
        output.put(fieldMapping.getTargetField(), value);
    }
    return output;
}
```

**Phase 2 Advantage**: Database-driven configuration enables storing and versioning composite/conditional rules

---

## Output Writing Pipeline

### GenericWriter (Transformation Output Handler)
**Location**: `/fabric-core/fabric-batch/src/main/java/com/truist/batch/writer/GenericWriter.java`  
**Lines**: 24-147  
**Role**: Collects transformed records and passes to file writer

```java
// Lines 32-43: Write method
public void write(Chunk<? extends Map<String, Object>> chunk) throws Exception {
    List<String> lines = chunk.getItems().stream().map(record -> {
        StringBuilder sb = new StringBuilder();
        // Concatenate all transformed field values in order
        for (Map.Entry<String, FieldMapping> entry : mappings) {
            String value = (String) record.get(entry.getValue().getTargetField());
            sb.append(value != null ? value : "");
        }
        return sb.toString();
    }).collect(Collectors.toList());
    
    // Pass pre-formatted fixed-width lines to delegate writer
    Chunk<String> strChunk = new Chunk<>(lines);
    delegate.write(strChunk);  // Line 42
}
```

### FixedWidthFileWriter (Output File Handler)
**Location**: `/fabric-core/fabric-batch/src/main/java/com/truist/batch/writer/FixedWidthFileWriter.java`  
**Lines**: 19-100  
**Role**: Writes formatted fixed-width records to file

```java
// Lines 71-75: Write to file
public void write(Chunk<? extends String> chunk) throws Exception {
    delegate.write(chunk);
    log.debug("📝 Wrote {} records to: {}", chunk.size(), outputPath);
}
```

---

## Field Mapping Model

### FieldMapping Entity
**Location**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/model/FieldMapping.java`  
**Lines**: 14-64

```java
@Data
public class FieldMapping implements Serializable {
    
    // Basic mapping
    private String fieldName;              // Field identifier
    private String sourceField;            // Source data field
    private String targetField;            // Output field name
    private String value;                  // Constant value
    
    // Transformation type selector
    private String transformationType;     // "constant|source|composite|conditional"
    
    // Composite fields (Phase 2)
    private boolean composite;
    private List<Map<String, String>> sources;  // [{"sourceField": "field1"}, ...]
    private String transform;              // "sum|concat"
    private String delimiter;              // For concat operations
    
    // Conditional fields (Phase 2)
    private List<Condition> conditions;    // IF-THEN-ELSE logic
    
    // Output formatting
    private int length;                    // Fixed-width length
    private String pad;                    // "left|right"
    private String padChar;                // Padding character
    private String format;                 // Date/time/number format
    private String dataType;               // Field data type
    
    // Metadata
    private int targetPosition;            // Order in output
    private String defaultValue;           // Fallback value
    
    // Compliance & Security (Epic 2)
    private String encryptionLevel = "NONE";
    private String piiClassification = "NONE";
    private boolean validationRequired;
    private String complianceLevel;
}
```

### Condition Model
**Location**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/model/Condition.java`  
**Lines**: 10-23

```java
@Data
public class Condition implements Serializable {
    
    private String ifExpr;                 // Expression to evaluate: "field op value"
    private String then;                   // Value if true
    private String elseExpr;               // Value if false
    private List<Condition> elseIfExprs;   // Nested else-if conditions
}
```

---

## Current Transformation Capabilities

### Transformation Type Support Matrix

| Type | Support | Location | Notes |
|------|---------|----------|-------|
| **constant** | ✅ Full | Line 108-113 | Returns fixed value or default |
| **source** | ✅ Full | Line 116-118 | Extracts value from input field |
| **blank** | ✅ Full | Line 129-131 | Returns default value (usually empty) |
| **composite** | ⚠️ Partial | Line 120-122 | Only SUM and CONCAT implemented |
| **conditional** | ⚠️ Partial | Line 125-127 | Basic IF-THEN-ELSE; no complex logic |

### Composite Transformation Gaps

**Currently Implemented**:
- ✅ SUM: `sum([field1, field2]) → total`
- ✅ CONCAT: `concat([field1, field2], delimiter) → joined`

**Missing for Phase 2**:
- ❌ AVERAGE: `avg([field1, field2, ...])`
- ❌ MIN/MAX: `min/max([field1, field2, ...])`
- ❌ STRING_FUNCTIONS: `substring()`, `uppercase()`, `lowercase()`, `trim()`, `replace()`
- ❌ DATE_FUNCTIONS: `date_format()`, `date_add()`, `date_diff()`
- ❌ CONDITIONAL_COMPOSITES: `sum([field1, field2]) IF condition ELSE 0`
- ❌ NESTED_COMPOSITES: `concat(sum([a,b]), concat([c,d]))`

### Conditional Transformation Gaps

**Currently Implemented**:
- ✅ Basic IF-THEN-ELSE
- ✅ ELSE-IF chains
- ✅ Simple comparison operators: `==`, `!=`, `<`, `>`, `<=`, `>=`
- ✅ Logical operators: `&&`, `||`, `!`

**Missing for Phase 2**:
- ❌ IN operator: `field IN ('val1', 'val2', 'val3')`
- ❌ BETWEEN operator: `field BETWEEN 100 AND 200`
- ❌ LIKE operator: `field LIKE 'pattern%'`
- ❌ NULL checks: `field IS NULL`
- ❌ Function-based conditions: `if upper(field) == 'ACTIVE'`
- ❌ Cross-field comparisons: `field1 > field2`

---

## Phase 2 Integration Points

### Integration Point 1: Enhanced Composite Transformation
**File**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/mapping/YamlMappingService.java`  
**Current**: Lines 181-210  
**Action**: Expand `handleComposite()` method

**Required Enhancements**:
```java
private String handleComposite(...) {
    // Current: "sum", "concat"
    // Add: "avg", "min", "max"
    // Add: String functions ("upper", "lower", "trim", "substring")
    // Add: Date functions ("format_date", "add_days")
    // Add: Conditional composite
}
```

**Implementation Approach**:
1. Add new enum: `CompositeOperationType` (SUM, AVG, MIN, MAX, CONCAT, etc.)
2. Add service: `CompositeTransformationService` with pluggable handlers
3. Extend FieldMapping model with operation-specific parameters
4. Support nested composite declarations in YAML/JSON

---

### Integration Point 2: Enhanced Conditional Logic
**File**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/mapping/YamlMappingService.java`  
**Current**: Lines 257-333 (evaluateExpression)  
**Action**: Extend expression evaluator

**Required Enhancements**:
```java
private boolean evaluateExpression(String expression, Map<String, Object> row) {
    // Current: ==, !=, <, >, <=, >=, &&, ||, !
    // Add: IN, BETWEEN, LIKE
    // Add: Function calls: upper(), lower(), trim()
    // Add: Type coercion and null safety
    // Add: Error handling for invalid expressions
}
```

**Implementation Approach**:
1. Create expression parser using regex or simple tokenizer
2. Add operator enum: `ConditionalOperator` (EQ, NE, GT, LT, GTE, LTE, IN, BETWEEN, LIKE, etc.)
3. Create expression evaluator class: `ExpressionEvaluator`
4. Support function decorators: `@function(...)`

---

### Integration Point 3: JsonMappingService Enhancement
**File**: `/fabric-core/fabric-api/src/main/java/com/truist/batch/service/JsonMappingService.java`  
**Current**: Lines 46-293  
**Action**: Ensure JSON configuration fully supports composite/conditional definitions

**Required Additions**:
1. Add methods to validate composite field definitions
2. Add methods to validate conditional expression syntax
3. Add methods to test field mappings (dry-run preview)
4. Add audit logging for field transformation execution

---

### Integration Point 4: Database Schema Extensions
**Location**: Liquibase migration scripts in `/fabric-core/fabric-api/src/main/resources/db/changelog/`

**Tables to Enhance**:
1. `MANUAL_JOB_CONFIG` - Add composite/conditional definition versioning
2. New table: `FIELD_TRANSFORMATION_CACHE` - Cache composite/conditional results
3. New table: `FIELD_TRANSFORMATION_AUDIT` - Audit trail for transformations

---

### Integration Point 5: Spring Batch Configuration
**Files**: 
- `/fabric-core/fabric-batch/src/main/java/com/truist/batch/processor/GenericProcessor.java`
- `/fabric-core/fabric-batch/src/main/java/com/truist/batch/processor/EnhancedGenericProcessor.java`

**Action**: No changes needed - processors already delegate to YamlMappingService

**Verification**:
- ✅ GenericProcessor.process() calls yamlMappingService.transformField() [Line 64]
- ✅ EnhancedGenericProcessor supports both YAML and JSON configs

---

## Key Integration Recommendations

### 1. Transformation Service Architecture
```
YamlMappingService (Orchestrator)
├── CompositeTransformationService
│   ├── SumTransformer
│   ├── ConcatTransformer
│   ├── AverageTransformer      ← NEW
│   ├── MinMaxTransformer        ← NEW
│   └── StringFunctionTransformer ← NEW
│
├── ConditionalEvaluationService ← ENHANCE
│   ├── ExpressionParser
│   ├── OperatorResolver
│   └── FunctionEvaluator        ← NEW
│
└── FieldTransformationCache     ← NEW (for performance)
```

### 2. Phase 2 YAML Configuration Examples

**Composite SUM with Enhancement**:
```yaml
targetField:
  transformationType: composite
  sources:
    - sourceField: amount1
    - sourceField: amount2
  transform: sum
  # NEW: Support aggregate functions
  aggregateFunction: avg  # sum, avg, min, max
  precision: 2
  defaultValue: "0.00"
```

**Composite STRING with Functions**:
```yaml
fullName:
  transformationType: composite
  sources:
    - sourceField: firstName
      function: upper
    - sourceField: lastName
      function: trim
  transform: concat
  delimiter: " "
```

**Enhanced CONDITIONAL**:
```yaml
riskLevel:
  transformationType: conditional
  conditions:
    - ifExpr: "amount > 1000000 AND status IN ('ACTIVE', 'PENDING')"
      then: "HIGH"
    - ifExpr: "amount BETWEEN 100000 AND 1000000"
      then: "MEDIUM"
    - else: "LOW"
```

### 3. Database Configuration Example

**JSON Configuration for Database Storage**:
```json
{
  "sourceSystem": "ENCORE",
  "jobName": "atoctran_200",
  "fieldMappings": [
    {
      "targetField": "calculated_amount",
      "transformationType": "composite",
      "sources": [
        {"sourceField": "principal", "function": null},
        {"sourceField": "interest", "function": null}
      ],
      "transform": "sum",
      "length": 15,
      "pad": "right",
      "padChar": " ",
      "defaultValue": "0"
    },
    {
      "targetField": "status_indicator",
      "transformationType": "conditional",
      "conditions": [
        {
          "ifExpr": "amount > 100000",
          "then": "LARGE"
        }
      ]
    }
  ]
}
```

### 4. Testing Strategy for Phase 2

**Unit Test Files to Create**:
1. `CompositeTransformationServiceTest.java`
   - Test SUM, AVG, MIN, MAX operations
   - Test nested composite fields
   - Test error handling for invalid sources

2. `ConditionalEvaluationEnhancementTest.java`
   - Test IN, BETWEEN, LIKE operators
   - Test function-based conditions
   - Test complex boolean logic

3. `EnhancedGenericProcessorTest.java`
   - Integration test with composite + conditional fields
   - Performance test with large datasets
   - Error recovery scenarios

---

## File Location Summary

| Component | File | Lines | Purpose |
|-----------|------|-------|---------|
| **Execution Orchestration** | JobExecutionService.java | 70-106 | Entry point & orchestration |
| **Request Contract** | JobExecutionRequest.java | 14-233 | API request DTO |
| **Field Transformation (Core)** | YamlMappingService.java | 102-333 | Primary transformation engine |
| **Composite Logic** | YamlMappingService.java | 181-210 | Current composite implementation |
| **Conditional Logic** | YamlMappingService.java | 216-333 | Current conditional implementation |
| **Processor (YAML)** | GenericProcessor.java | 49-70 | Spring Batch processor |
| **Processor (Dual)** | EnhancedGenericProcessor.java | 52-95 | Enhanced with JSON support |
| **Writer** | GenericWriter.java | 32-43 | Transformation output |
| **File Writer** | FixedWidthFileWriter.java | 71-75 | File output |
| **Field Mapping Model** | FieldMapping.java | 14-64 | Field definition model |
| **Condition Model** | Condition.java | 10-23 | Conditional expression model |

---

## Next Steps for Phase 2 Development

1. **Quick Wins** (1-2 sprints):
   - Add missing composite operations (AVG, MIN, MAX)
   - Add IN and BETWEEN operators to conditional evaluation
   - Add string function support (upper, lower, trim)

2. **Core Enhancements** (2-3 sprints):
   - Create CompositeTransformationService
   - Create ExpressionEvaluator with full operator support
   - Enhance database schema for complex rules
   - Add field mapping validation service

3. **Advanced Features** (3-4 sprints):
   - Support nested composite fields
   - Add function-based conditions
   - Implement transformation caching
   - Add expression builder UI component

4. **Production Hardening** (2 sprints):
   - Comprehensive error handling and recovery
   - Performance optimization and benchmarking
   - Security audit for expression evaluation
   - Full test coverage (unit, integration, E2E)

---

## Appendix: Code References

### A. Field Transformation Dispatch
**YamlMappingService.java, Lines 102-148**

### B. Current Composite Implementation
**YamlMappingService.java, Lines 181-210**

### C. Current Conditional Implementation
**YamlMappingService.java, Lines 216-333**

### D. Expression Evaluation
**YamlMappingService.java, Lines 257-333**

### E. Generic Processor
**GenericProcessor.java, Lines 49-70**

### F. Enhanced Generic Processor
**EnhancedGenericProcessor.java, Lines 52-95**

---

**Document prepared for Phase 2 Field Transformation Integration**  
**Ready for architecture review and development planning**
