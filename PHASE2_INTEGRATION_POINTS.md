# Phase 2 Field Transformation Integration - Visual Guide

## Complete Batch Execution Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        BATCH EXECUTION ARCHITECTURE                      │
└─────────────────────────────────────────────────────────────────────────┘

        ┌──────────────────────────────────────────────────────┐
        │  Frontend / API Request                              │
        │  POST /api/v2/manual-job-execution/execute/{configId}│
        └────────────────────┬─────────────────────────────────┘
                             │
                             ↓
    ┌────────────────────────────────────────────────────────────┐
    │ JobExecutionService (Orchestrator Layer)                   │
    │ ─ validateExecutionRequest()                               │
    │ ─ createExecutionRecord()                                  │
    │ ─ performActualExecution() ← Delegates to batch framework │
    └────────────────┬──────────────────────────────────────────┘
                     │
         ┌───────────┴──────────────┐
         │                          │
         ↓                          ↓
    ┌─────────────────┐      ┌────────────────────┐
    │  YAML Config    │      │  JSON Config       │
    │  (Classpath)    │      │  (Database)        │
    └────────┬────────┘      └────────┬───────────┘
             │                        │
             └────────────┬───────────┘
                          │
                          ↓
    ┌────────────────────────────────────────────────────────────┐
    │ Spring Batch Configuration                                  │
    │ ┌──────────────────────────────────────────────────────┐   │
    │ │ GenericProcessor / EnhancedGenericProcessor          │   │
    │ │ ─ For each input record: call mappingService         │   │
    │ │ ─ Returns transformed record Map                     │   │
    │ └────────────────────┬─────────────────────────────────┘   │
    └─────────────────────┼──────────────────────────────────────┘
                          │
                          ↓
    ╔════════════════════════════════════════════════════════════╗
    ║ PHASE 2 INTEGRATION POINT 1: YamlMappingService            ║
    ║ ────────────────────────────────────────────────────────── ║
    ║ public String transformField(Map row, FieldMapping)        ║
    ║                                                              ║
    ║ For each source field in record:                           ║
    ║   switch(transformationType) {                             ║
    ║     case "constant" → getValue() or defaultValue()         ║
    ║     case "source" → resolveValue() from input              ║
    ║     case "composite" → handleComposite() ← ENHANCE #1      ║
    ║     case "conditional" → evaluateConditional() ← ENHANCE #2║
    ║     case "blank" → defaultValue()                          ║
    ║   }                                                         ║
    ║   Apply padding/formatting and return                      ║
    ║                                                              ║
    ║ Location: /fabric-core/fabric-utils/...                   ║
    ║           /mapping/YamlMappingService.java                ║
    ║ Lines: 102-148 (transformField)                           ║
    ╚════════════════════════════════════════════════════════════╝
         │                                │
         ├─────────────────┬──────────────┤
         │                 │              │
         ↓                 ↓              ↓
    ┌─────────────┐  ┌──────────────┐  ┌──────────────┐
    │  CONSTANT   │  │   SOURCE     │  │    BLANK     │
    │  Returns    │  │  Extracts    │  │  Returns     │
    │  fixed      │  │  from input  │  │  empty or    │
    │  value      │  │  field       │  │  default     │
    └─────────────┘  └──────────────┘  └──────────────┘
                             │
         ┌───────────────────┴────────────────────┐
         │                                        │
         ↓                                        ↓
    ╔══════════════════════════════════╗   ╔═══════════════════════════╗
    ║ ENHANCEMENT #1: COMPOSITE FIELDS ║   ║ ENHANCEMENT #2: CONDITIONAL║
    ║ ─────────────────────────────────║   ║ ──────────────────────────║
    ║ handleComposite()                ║   ║ evaluateConditional()     ║
    ║ Lines: 181-210                   ║   ║ Lines: 216-333            ║
    ║                                  ║   ║                            ║
    ║ Current Support:                 ║   ║ Current Support:          ║
    ║ • SUM([field1, field2])          ║   ║ • IF-THEN-ELSE logic     ║
    ║ • CONCAT([f1, f2], delim)        ║   ║ • ELSE-IF chains         ║
    ║                                  ║   ║ • Operators:              ║
    ║ NEW Support:                     ║   ║   ==, !=, <, >, <=, >=   ║
    ║ • AVG([f1, f2, ...])             ║   ║ • Logic: &&, ||, !        ║
    ║ • MIN/MAX([f1, f2, ...])         ║   ║                            ║
    ║ • String functions               ║   ║ NEW Support:              ║
    ║   (upper, lower, trim)           ║   ║ • IN operator             ║
    ║ • Date functions                 ║   ║ • BETWEEN operator        ║
    ║ • Nested composites              ║   ║ • LIKE operator           ║
    ║                                  ║   ║ • Function calls          ║
    ║ Implementation Location:         ║   ║ • evaluateExpression()    ║
    ║ Same file, expand switch         ║   ║   Lines: 257-333          ║
    ║ statement in handleComposite()   ║   ║                            ║
    ╚══════════════════════════════════╝   ╚═══════════════════════════╝
         │                                        │
         └────────────────┬─────────────────────┘
                          │
                 Apply padding/formatting
                 (FormatterUtil.pad)
                          │
                          ↓
    ┌────────────────────────────────────────────────────────────┐
    │ GenericWriter                                               │
    │ ─ Collects transformed fields                              │
    │ ─ Concatenates to fixed-width string                       │
    └────────────────────┬───────────────────────────────────────┘
                         │
                         ↓
    ┌────────────────────────────────────────────────────────────┐
    │ FixedWidthFileWriter                                        │
    │ ─ Writes formatted records to output file                  │
    └────────────────────┬───────────────────────────────────────┘
                         │
                         ↓
                    Output File
              (Fixed-Width Format)
```

---

## Integration Point Details

### Enhancement #1: Composite Field Transformation

**Current Code Location**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/mapping/YamlMappingService.java`
**Lines**: 181-210

```java
private String handleComposite(List<Map<String, String>> sources, 
                              Map<String, Object> row, 
                              String transform, String delimiter, 
                              String defaultValue) {
    
    // CURRENT SWITCH (Lines 187-209)
    if ("sum".equalsIgnoreCase(transform)) {
        // Implementation
    } else if ("concat".equalsIgnoreCase(transform)) {
        // Implementation
    }
    
    // ENHANCEMENT REQUIRED: Add more cases
    // if ("avg".equalsIgnoreCase(transform)) { }
    // if ("min".equalsIgnoreCase(transform)) { }
    // if ("max".equalsIgnoreCase(transform)) { }
    // if ("upper".equalsIgnoreCase(transform)) { }
    // if ("lower".equalsIgnoreCase(transform)) { }
    // etc.
}
```

**Usage in YAML**:
```yaml
fields:
  total_amount:
    fieldName: total_amount
    transformationType: composite
    sources:
      - sourceField: principal
      - sourceField: interest
      - sourceField: fees
    transform: sum              # ← Change to: avg, min, max
    length: 12
    pad: right
    padChar: " "
```

**Usage in JSON/Database**:
```json
{
  "targetField": "total_amount",
  "transformationType": "composite",
  "sources": [
    {"sourceField": "principal"},
    {"sourceField": "interest"},
    {"sourceField": "fees"}
  ],
  "transform": "sum",
  "length": 12
}
```

---

### Enhancement #2: Conditional Expression Evaluation

**Current Code Location**: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/mapping/YamlMappingService.java`
**Lines**: 257-333 (evaluateExpression method)

```java
private boolean evaluateExpression(String expression, Map<String, Object> row) {
    
    // CURRENT PATTERN MATCHING (Line 277)
    // Pattern.compile("([^!=<>()\\s]+)\\s*(==|=|!=|>=|<=|<|>)\\s*...")
    
    // CURRENT OPERATORS SUPPORTED:
    // ==, !=, <, >, <=, >=
    
    // ENHANCEMENT REQUIRED:
    // Add support for:
    // - IN operator: "status IN ('ACTIVE', 'PENDING')"
    // - BETWEEN operator: "amount BETWEEN 100 AND 1000"
    // - LIKE operator: "name LIKE 'pattern%'"
    // - Function calls: "upper(status) == 'ACTIVE'"
    // - NULL checks: "field IS NULL"
}
```

**Usage in YAML**:
```yaml
fields:
  risk_level:
    fieldName: risk_level
    transformationType: conditional
    conditions:
      - ifExpr: "amount > 1000000 AND status IN ('ACTIVE', 'PENDING')"  # ← NEW
        then: "HIGH"
      - ifExpr: "amount BETWEEN 100000 AND 1000000"                     # ← NEW
        then: "MEDIUM"
      - else: "LOW"
```

**Usage in JSON/Database**:
```json
{
  "targetField": "risk_level",
  "transformationType": "conditional",
  "conditions": [
    {
      "ifExpr": "amount > 1000000 AND status IN ('ACTIVE', 'PENDING')",
      "then": "HIGH"
    },
    {
      "ifExpr": "amount BETWEEN 100000 AND 1000000",
      "then": "MEDIUM"
    },
    {
      "else": "LOW"
    }
  ]
}
```

---

## Call Stack Trace

```
HTTP Request
    ↓
JobExecutionController.execute(configId)
    ↓
JobExecutionService.executeJob(request, user)
    │
    ├─ validateExecutionRequest()
    ├─ configRepository.findById()
    ├─ createExecutionRecord()
    └─ performActualExecution()
        │
        └─ Spring Batch Framework
            │
            ├─ Reader: Read source data
            │
            ├─ Processor: GenericProcessor.process(record)
            │   │
            │   └─ For each FieldMapping:
            │       │
            │       └─ YamlMappingService.transformField(row, mapping)  ← KEY POINT
            │           │
            │           └─ switch(transformationType)
            │               ├─ case "composite"
            │               │   └─ handleComposite() ← ENHANCEMENT #1
            │               │
            │               └─ case "conditional"
            │                   └─ evaluateConditional()
            │                       └─ evaluateExpression() ← ENHANCEMENT #2
            │
            └─ Writer: Write to file
```

---

## File Modification Map

### Files Requiring Modification:

1. **PRIMARY**: `YamlMappingService.java`
   - File: `/fabric-core/fabric-utils/src/main/java/com/truist/batch/mapping/YamlMappingService.java`
   - Modify: `handleComposite()` method (Lines 181-210)
   - Modify: `evaluateExpression()` method (Lines 257-333)
   - Effort: Medium
   - Impact: High (affects all transformations)

2. **OPTIONAL**: Create new service files
   - `CompositeTransformationService.java` - Pluggable operations
   - `ExpressionEvaluator.java` - Dedicated parser
   - `FieldTransformationCache.java` - Performance optimization

### Files NOT Requiring Changes:

- `GenericProcessor.java` - Already delegates to service
- `EnhancedGenericProcessor.java` - Already delegates to service
- `GenericWriter.java` - Works with transformed data
- `FixedWidthFileWriter.java` - Just writes output
- `JobExecutionService.java` - Orchestration already complete
- Database schemas - Already support complex configs

---

## Testing Strategy

### Unit Tests to Create

```java
// Test file location: fabric-core/fabric-utils/src/test/java/com/truist/batch/mapping/

1. CompositeTransformationTests
   - testSumOperation()
   - testConcatOperation()
   - testAverageOperation()        ← NEW
   - testMinMaxOperation()          ← NEW
   - testStringFunctions()          ← NEW
   - testNestedComposites()         ← NEW

2. ConditionalEvaluationTests
   - testBasicIfThenElse()
   - testElseIfChains()
   - testLogicalOperators()
   - testInOperator()               ← NEW
   - testBetweenOperator()          ← NEW
   - testLikeOperator()             ← NEW
   - testFunctionCalls()            ← NEW

3. IntegrationTests
   - testEndToEndWithComposites()
   - testEndToEndWithConditionals()
   - testCombinedTransformations()
```

---

## Dependency Map

```
YamlMappingService (No dependencies on transformation logic)
├── Uses: List<Map<String,String>> (sources)
├── Uses: Map<String,Object> (row data)
├── Uses: String (expressions)
└── Produces: String (transformed values)

No external dependencies required for:
- Composite operations (uses Java Stream API)
- Conditional evaluation (uses Java Regex)
- Expression parsing (uses Java String operations)
```

---

## Performance Considerations

### Enhancement #1: Composite Fields
- **Current**: Streams through sources (O(n))
- **Impact**: Minimal - no change in complexity
- **Optimization**: Cache aggregations for repeated use

### Enhancement #2: Conditional Expressions
- **Current**: Regex matching for each condition (O(n*m))
- **Impact**: Low - expressions are typically short
- **Optimization**: Compile regex patterns once, cache results

### Recommended Cache Layer
```java
public class FieldTransformationCache {
    private Map<String, String> compositeCache;      // Field ID → Result
    private Map<String, Boolean> expressionCache;    // Expression → Result
    
    public void invalidate(String configId) { }
    public void clear() { }
}
```

---

## Database Configuration Examples

### AtocTran ENCORE Job with Composite Fields

```json
{
  "sourceSystem": "ENCORE",
  "jobName": "atoctran_encore_200_job",
  "transactionType": "200",
  "fieldMappings": [
    {
      "targetField": "ACCT_NUM",
      "transformationType": "source",
      "sourceField": "acct_num",
      "length": 12,
      "defaultValue": ""
    },
    {
      "targetField": "TOTAL_AMOUNT",
      "transformationType": "composite",
      "sources": [
        {"sourceField": "principal"},
        {"sourceField": "interest"},
        {"sourceField": "fees"}
      ],
      "transform": "sum",
      "length": 15,
      "defaultValue": "0"
    },
    {
      "targetField": "RISK_FLAG",
      "transformationType": "conditional",
      "conditions": [
        {
          "ifExpr": "principal > 1000000 AND status == 'ACTIVE'",
          "then": "HIGH"
        },
        {
          "ifExpr": "principal > 100000",
          "then": "MEDIUM"
        },
        {
          "else": "LOW"
        }
      ],
      "length": 1,
      "defaultValue": "L"
    }
  ]
}
```

---

## Success Criteria for Phase 2

- [x] Can define composite fields with SUM, CONCAT
- [ ] Can define composite fields with AVG, MIN, MAX
- [ ] Can define composite fields with string functions
- [ ] Can define composite fields with date functions
- [x] Can define conditional fields with basic operators
- [ ] Can define conditional fields with IN operator
- [ ] Can define conditional fields with BETWEEN operator
- [ ] Can define conditional fields with LIKE operator
- [ ] Can define conditional fields with function calls
- [ ] Can execute batch jobs using new transformation types
- [ ] Can persist complex configurations in database
- [ ] Comprehensive test coverage (unit + integration)
- [ ] Performance benchmarks show acceptable latency
- [ ] Documentation complete with examples

---

## Related Documentation

- **Full Analysis**: `BATCH_EXECUTION_FLOW_ANALYSIS.md` (28 KB)
- **Quick Reference**: `BATCH_EXECUTION_SUMMARY.md`
- **CLAUDE.md**: Project-wide guidelines and standards
- **Phase 1 Docs**: US001 Manual Job Configuration Interface
- **Phase 3 Docs**: Manual Batch Execution Interface

---

**For implementation questions, refer to the senior developer or review the full analysis document.**
