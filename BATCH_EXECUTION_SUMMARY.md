# Batch Execution Flow - Quick Reference Summary

## Primary Integration Points for Phase 2 Transformations

### 1. Entry Point: JobExecutionService
- **File**: `/fabric-core/fabric-api/src/main/java/com/truist/batch/service/JobExecutionService.java`
- **Method**: `executeJob()` (Lines 70-106)
- **Purpose**: Main orchestrator for job execution with validation and audit trail
- **Key Action**: Calls `performActualExecution()` which invokes batch processors

### 2. Core Transformation Engine: YamlMappingService
- **File**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/mapping/YamlMappingService.java`
- **Critical Method**: `transformField()` (Lines 102-148)
- **Role**: Central hub for ALL field transformations
- **Current Capabilities**:
  - `constant`: Fixed values
  - `source`: Extract from input field
  - `blank`: Empty/default values
  - `composite`: SUM and CONCAT operations (expandable)
  - `conditional`: IF-THEN-ELSE logic with simple operators

### 3. Data Processors (Invoke YamlMappingService)
- **GenericProcessor**: `/fabric-core/fabric-batch/src/main/java/com/truist/batch/processor/GenericProcessor.java`
  - Line 64: Calls `yamlMappingService.transformField()`
  - Uses YAML configuration from classpath
  
- **EnhancedGenericProcessor**: `/fabric-core/fabric-batch/src/main/java/com/truist/batch/processor/EnhancedGenericProcessor.java`
  - Lines 91, 94: Calls mapping service based on config source
  - Supports both JSON (database) and YAML (classpath) configurations

### 4. Output Pipeline
- **GenericWriter**: Collects transformed records (Lines 32-43)
- **FixedWidthFileWriter**: Writes to file (Lines 71-75)

---

## WHERE Phase 2 Transformations Go

### Location 1: Composite Field Enhancement
**File**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/mapping/YamlMappingService.java`
**Current Implementation**: Lines 181-210 (`handleComposite()` method)

**What Needs to Be Added**:
```
Current:  SUM, CONCAT only
Add:      AVG, MIN, MAX, string functions, date functions
Location: Same method, expand the switch statement
Integration: Already called from transformField() [Line 121]
```

### Location 2: Conditional Logic Enhancement
**File**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/mapping/YamlMappingService.java`
**Current Implementation**: Lines 257-333 (`evaluateExpression()` method)

**What Needs to Be Added**:
```
Current:  ==, !=, <, >, <=, >=, &&, ||, !
Add:      IN, BETWEEN, LIKE, function calls
Location: Same method, enhance expression parser
Integration: Already called from evaluateConditional() [Line 229]
```

---

## Data Flow: Source to Target

```
1. User Request
   ↓ JobExecutionRequest (configId + parameters)
   
2. Configuration Loading
   ↓ GenericProcessor/EnhancedGenericProcessor.process()
   ↓ Load YamlMapping from classpath or database
   
3. Field Transformation (FOR EACH FIELD IN MAPPING)
   ↓ YamlMappingService.transformField()
   ├─ Check transformation type
   ├─ If "composite": call handleComposite() ← ENHANCEMENT #1
   ├─ If "conditional": call evaluateConditional() → evaluateExpression() ← ENHANCEMENT #2
   └─ Apply padding/formatting
   
4. Output Generation
   ↓ GenericWriter concatenates transformed fields
   ↓ FixedWidthFileWriter writes to file
   
5. Complete
   ↓ Output file with transformed data
```

---

## Three Layers of Transformation

### Layer 1: Orchestration (JobExecutionService)
- Validates request
- Loads configuration
- Creates execution tracking record
- Delegates to batch processor

### Layer 2: Data Processing (GenericProcessor/EnhancedGenericProcessor)
- Iterates through each input record
- Gets field mappings from configuration
- For each field: calls YamlMappingService.transformField()
- Returns transformed record

### Layer 3: Field Transformation (YamlMappingService) ← WHERE PHASE 2 HAPPENS
- Dispatches to handler based on transformationType
- **Constant**: Returns value
- **Source**: Resolves input field
- **Composite**: ← ENHANCEMENT POINT #1
- **Conditional**: ← ENHANCEMENT POINT #2
- **Blank**: Returns empty/default

---

## Models Used in Transformation

### FieldMapping
- **File**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/model/FieldMapping.java`
- **Key Fields**:
  - `transformationType`: Selector (constant|source|composite|conditional)
  - `sourceField`: Source field name
  - `targetField`: Output field name
  - `sources`: List of source fields for composite
  - `transform`: Operation type (sum, concat, etc.)
  - `conditions`: List of Condition objects
  - `length`: Fixed-width length
  - `defaultValue`: Fallback value

### Condition
- **File**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/model/Condition.java`
- **Key Fields**:
  - `ifExpr`: Expression to evaluate
  - `then`: Value if true
  - `elseExpr`: Value if false
  - `elseIfExprs`: Nested conditions

---

## Integration Strategy for Phase 2

### Step 1: Expand Composite Operations
1. Add AVG, MIN, MAX handlers to `handleComposite()`
2. Test with YAML and JSON configurations
3. No changes needed to calling code (already integrated)

### Step 2: Enhance Conditional Expressions
1. Extend `evaluateExpression()` to support IN, BETWEEN, LIKE
2. Add function call support (upper, lower, trim)
3. Improve error handling
4. Test with complex boolean expressions

### Step 3: Create Supporting Services (Optional but Recommended)
1. `CompositeTransformationService`: Pluggable operation handlers
2. `ExpressionEvaluator`: Dedicated expression parser
3. `FieldTransformationCache`: Performance optimization

### Step 4: Database Schema (If Using DB Configuration)
1. Ensure MANUAL_JOB_CONFIG can store complex field definitions
2. Add versioning for transformation rules
3. Add audit table for transformation execution

---

## Testing Areas

### Unit Tests
- `YamlMappingService.transformField()` - All transformation types
- `handleComposite()` - All composite operations
- `evaluateExpression()` - All operators and functions

### Integration Tests
- `GenericProcessor` with composite fields
- `EnhancedGenericProcessor` with JSON config
- End-to-end batch execution with transformations

### Performance Tests
- Large record sets with complex transformations
- Nested composite fields
- Complex conditional logic

---

## File Locations Reference

| Component | File Path |
|-----------|-----------|
| Job Execution Entry | `/fabric-core/fabric-api/src/main/java/com/truist/batch/service/JobExecutionService.java` |
| Core Transformation | `/fabric-core/fabric-utils/src/main/java/com/truist/batch/mapping/YamlMappingService.java` |
| YAML Processor | `/fabric-core/fabric-batch/src/main/java/com/truist/batch/processor/GenericProcessor.java` |
| Dual-Config Processor | `/fabric-core/fabric-batch/src/main/java/com/truist/batch/processor/EnhancedGenericProcessor.java` |
| Output Writer | `/fabric-core/fabric-batch/src/main/java/com/truist/batch/writer/GenericWriter.java` |
| File Writer | `/fabric-core/fabric-batch/src/main/java/com/truist/batch/writer/FixedWidthFileWriter.java` |
| Field Mapping Model | `/fabric-core/fabric-utils/src/main/java/com/truist/batch/model/FieldMapping.java` |
| Condition Model | `/fabric-core/fabric-utils/src/main/java/com/truist/batch/model/Condition.java` |

---

## Key Insights

1. **No Controller Changes Needed**: Batch execution is orchestrated by `JobExecutionService`, not REST controllers
2. **All Transformations Centralized**: Single point of enhancement in `YamlMappingService.transformField()`
3. **Configuration Already Supports**: Field mappings already include composite/conditional properties
4. **Processors Already Integrated**: Both processors already delegate to transformation service
5. **Database-Ready**: JSON configuration in database can immediately use new transformation types
6. **Backward Compatible**: Existing simple transformations continue to work unchanged

---

## Next Actions

1. Read the full analysis: `BATCH_EXECUTION_FLOW_ANALYSIS.md`
2. Review `YamlMappingService.transformField()` (Lines 102-148)
3. Identify quick wins (AVG, MIN, MAX operations)
4. Plan architecture for expression evaluator enhancement
5. Create test cases before implementation

---

**For detailed line-by-line code analysis, see: BATCH_EXECUTION_FLOW_ANALYSIS.md**
