# 🚀 Batch Processing Module - Implementation Plan

## Executive Summary
Implementation of a generic, configuration-driven Spring Batch processor that transforms staged data from SQL*Loader into output files with support for parallel simple transactions and sequenced complex transactions.

---

## 📋 Implementation Roadmap

### **Phase 1: Foundation (Weeks 1-2)**

#### Week 1: Core Infrastructure
- **Database Schema Creation**
  - ✅ Configuration tables for job configs, transaction types, field mappings
  - ✅ Runtime tables for execution tracking and temporary staging
  - ✅ Audit tables for comprehensive logging
  
- **Spring Batch Core Components**
  - Generic ItemReader for staging tables
  - Configuration-driven ItemProcessor
  - Dynamic ItemWriter for multiple output formats
  - Job configuration factory

#### Week 2: Configuration Management
- **Configuration Service Layer**
  - CRUD operations for batch job configurations
  - Field mapping management
  - Transaction type definitions
  - Header/footer template management

- **REST APIs**
  - Job configuration endpoints
  - Field mapping endpoints
  - Execution monitoring endpoints

---

### **Phase 2: Simple Transaction Processing (Weeks 3-4)**

#### Week 3: Parallel Processing Framework
```java
@Component
public class ParallelTransactionProcessor {
    
    @Autowired
    private TaskExecutor taskExecutor;
    
    public CompletableFuture<List<TransactionResult>> processTransactions(
            String executionId, 
            List<TransactionType> transactionTypes) {
        
        List<CompletableFuture<TransactionResult>> futures = 
            transactionTypes.stream()
                .filter(t -> t.isParallelEligible())
                .map(transType -> CompletableFuture.supplyAsync(() -> 
                    processTransactionType(executionId, transType), taskExecutor))
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
}
```

#### Week 4: Merge Logic Implementation
- **Transaction Merging**
  - Combine parallel transaction results
  - Maintain transaction ordering
  - Handle merge conflicts
  - Generate unified output

---

### **Phase 3: Complex Transaction Processing (Weeks 5-6)**

#### Week 5: Dependency Management
```java
@Component
public class ComplexTransactionSequencer {
    
    public List<TransactionRecord> sequenceTransactions(
            String executionId,
            List<TransactionType> transactionTypes) {
        
        // Step 1: Process to temporary staging
        List<TempStagingRecord> stagingRecords = 
            writeToTempStaging(executionId, transactionTypes);
        
        // Step 2: Resolve dependencies
        Map<String, List<TempStagingRecord>> dependencyMap = 
            resolveDependencies(stagingRecords);
        
        // Step 3: Generate sequenced output
        return generateSequencedOutput(dependencyMap);
    }
    
    private Map<String, List<TempStagingRecord>> resolveDependencies(
            List<TempStagingRecord> records) {
        // Topological sort for dependency resolution
        DependencyGraph graph = buildDependencyGraph(records);
        return graph.topologicalSort();
    }
}
```

#### Week 6: Header/Footer Generation
- **Dynamic Header Generation**
  - Template-based headers
  - Dynamic field substitution
  - Date/time stamps
  - File identifiers

- **Summary Footer Generation**
  - Record counts by transaction type
  - Hash totals for numeric fields
  - Control totals
  - File checksums

---

### **Phase 4: Integration & Testing (Weeks 7-8)**

#### Week 7: End-to-End Integration
- **SQL*Loader Integration**
  - Trigger batch jobs after SQL*Loader completion
  - Read from staging tables
  - Configuration synchronization

- **File Management**
  - Output file generation
  - File archival
  - Error file handling

#### Week 8: Comprehensive Testing
- **Test Scenarios**
  - Simple parallel transactions
  - Complex dependent transactions
  - Large volume processing
  - Error recovery scenarios
  - Performance benchmarking

---

## 🏗️ Technical Architecture

### Core Components

#### 1. Generic Field Mapping Processor
```java
@Component
public class GenericFieldMappingProcessor implements ItemProcessor<Map<String, Object>, OutputRecord> {
    
    @Autowired
    private FieldMappingService fieldMappingService;
    
    @Autowired
    private TransformationEngine transformationEngine;
    
    @Override
    public OutputRecord process(Map<String, Object> item) throws Exception {
        String transactionType = (String) item.get("TRANSACTION_TYPE");
        List<FieldMapping> mappings = fieldMappingService.getMappings(transactionType);
        
        OutputRecord output = new OutputRecord();
        for (FieldMapping mapping : mappings) {
            Object value = item.get(mapping.getSourceColumn());
            
            // Apply transformation
            if (mapping.hasTransformation()) {
                value = transformationEngine.transform(value, mapping.getTransformationRule());
            }
            
            // Apply validation
            if (mapping.hasValidation()) {
                validateField(value, mapping.getValidationRule());
            }
            
            output.addField(mapping.getTargetFieldName(), value);
        }
        
        return output;
    }
}
```

#### 2. Dynamic Job Builder
```java
@Component
public class DynamicBatchJobBuilder {
    
    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    
    public Job buildJob(BatchJobConfig config) {
        JobBuilder jobBuilder = jobBuilderFactory.get(config.getJobName())
            .incrementer(new RunIdIncrementer())
            .listener(new BatchJobExecutionListener());
        
        // Add steps based on configuration
        if (config.isSimpleProcessing()) {
            jobBuilder.start(buildParallelStep(config));
        } else {
            jobBuilder.start(buildComplexStep(config));
        }
        
        // Add header/footer steps if configured
        if (config.isHeaderEnabled()) {
            jobBuilder.next(buildHeaderStep(config));
        }
        
        if (config.isFooterEnabled()) {
            jobBuilder.next(buildFooterStep(config));
        }
        
        return jobBuilder.build();
    }
}
```

#### 3. Transformation Engine
```java
@Component
public class TransformationEngine {
    
    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    
    public Object transform(Object input, String transformationRule) {
        if (transformationRule == null || transformationRule.isEmpty()) {
            return input;
        }
        
        // Support SQL-like expressions
        if (transformationRule.startsWith("SQL:")) {
            return executeSqlTransformation(input, transformationRule.substring(4));
        }
        
        // Support JavaScript expressions for complex transformations
        if (transformationRule.startsWith("JS:")) {
            return executeJavaScriptTransformation(input, transformationRule.substring(3));
        }
        
        // Default string manipulations
        return executeStringTransformation(input, transformationRule);
    }
    
    private Object executeSqlTransformation(Object input, String sql) {
        // Parse and execute SQL-like transformations
        // Examples: UPPER(), SUBSTR(), TO_DATE(), etc.
        return SqlTransformationParser.parse(sql).apply(input);
    }
}
```

---

## 📊 Database Design Details

### Configuration Tables Structure

#### batch_job_configs
- Stores main job configuration
- Defines processing type (SIMPLE/COMPLEX)
- Controls parallelization settings
- Manages output format specifications

#### batch_transaction_types
- Defines transaction types within a job
- Specifies processing order and dependencies
- Controls parallel eligibility
- Maps to source staging tables

#### batch_field_mappings
- Field-level transformation rules
- Data type conversions
- Validation rules
- Output formatting specifications

#### batch_temp_staging
- Temporary storage for complex transactions
- Maintains sequence numbers
- Tracks dependency resolution
- Enables transaction reordering

---

## 🔄 Processing Flows

### Simple Transaction Flow (Parallel)
```
1. Read Configuration
   ↓
2. Identify Transaction Types
   ↓
3. Fork Parallel Processes
   ├── Transaction Type A → Process → Results A
   ├── Transaction Type B → Process → Results B
   └── Transaction Type C → Process → Results C
   ↓
4. Merge Results
   ↓
5. Write Output File
```

### Complex Transaction Flow (Sequential)
```
1. Read Configuration
   ↓
2. Process to Temp Staging
   ↓
3. Resolve Dependencies
   ↓
4. Generate Header
   ↓
5. Process Sequenced Transactions
   ↓
6. Calculate Summary
   ↓
7. Generate Footer
   ↓
8. Write Output File
```

---

## 🎯 Success Metrics

### Performance Targets
- **Simple Transactions**: 100,000 records/minute
- **Complex Transactions**: 50,000 records/minute
- **Parallel Processing**: Up to 10 concurrent threads
- **Memory Usage**: < 2GB for standard processing

### Quality Metrics
- **Data Accuracy**: 100% field mapping accuracy
- **Error Rate**: < 0.01% processing errors
- **Recovery Time**: < 5 minutes for job restart
- **Audit Coverage**: 100% transaction traceability

---

## 🚦 Risk Mitigation

### Technical Risks
1. **Memory Overflow**
   - Mitigation: Chunk-based processing
   - Monitoring: Memory usage alerts

2. **Dependency Deadlocks**
   - Mitigation: Timeout mechanisms
   - Recovery: Automatic retry logic

3. **Data Inconsistency**
   - Mitigation: Transactional boundaries
   - Validation: Pre/post processing checks

### Operational Risks
1. **Configuration Errors**
   - Mitigation: Configuration validation
   - Testing: Dry-run capability

2. **Performance Degradation**
   - Mitigation: Resource pooling
   - Monitoring: Performance metrics

---

## 📅 Delivery Timeline

| Phase | Duration | Deliverables | Status |
|-------|----------|--------------|--------|
| **Phase 1** | 2 weeks | Database schema, Core components | Pending |
| **Phase 2** | 2 weeks | Simple transaction processing | Pending |
| **Phase 3** | 2 weeks | Complex transaction handling | Pending |
| **Phase 4** | 2 weeks | Integration & Testing | Pending |

**Total Duration**: 8 weeks

---

## 🔗 Integration Points

### With SQL*Loader Module
- Reads from staging tables populated by SQL*Loader
- Uses same configuration schema
- Shares audit framework

### With Configuration UI
- REST APIs for job configuration
- Real-time execution monitoring
- Error reporting and recovery

### With File System
- Output file generation
- Archive management
- Error file handling

---

## 📝 Next Steps

1. **Immediate Actions (Week 1)**
   - [ ] Create database schema
   - [ ] Set up Spring Batch project structure
   - [ ] Implement basic ItemReader/Processor/Writer
   - [ ] Create configuration service layer

2. **Short-term Goals (Weeks 2-4)**
   - [ ] Implement parallel processing framework
   - [ ] Build merge logic for simple transactions
   - [ ] Create REST APIs for configuration
   - [ ] Develop basic monitoring dashboard

3. **Medium-term Goals (Weeks 5-8)**
   - [ ] Implement complex transaction sequencing
   - [ ] Build header/footer generation
   - [ ] Complete integration testing
   - [ ] Performance optimization

---

## 📞 Support & Documentation

- **Technical Documentation**: Will be created during implementation
- **API Documentation**: Swagger/OpenAPI specifications
- **User Guide**: Configuration and operation procedures
- **Troubleshooting Guide**: Common issues and solutions

---

**Document Version**: 1.0  
**Created**: August 2025  
**Status**: Ready for Implementation

© 2025 Truist Financial Corporation. All rights reserved.