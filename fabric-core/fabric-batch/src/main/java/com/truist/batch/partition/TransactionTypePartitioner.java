package com.truist.batch.partition;

import com.truist.batch.entity.BatchTransactionTypeEntity;
import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FileConfig;
import com.truist.batch.repository.BatchTransactionTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Epic 2: Advanced partitioner that extends GenericPartitioner functionality
 * to support transaction-type-based parallel processing with database-driven configuration.
 * 
 * This partitioner creates execution contexts for each transaction type defined in the
 * database configuration, enabling true parallel processing with proper resource allocation
 * and banking-grade audit trail capabilities.
 * 
 * Key Features:
 * - Database-driven transaction type configuration
 * - Dynamic thread pool allocation based on transaction complexity
 * - Chunk size optimization for different transaction types  
 * - Correlation ID generation for complete audit trails
 * - Performance monitoring and metrics collection
 * - Banking compliance and security integration
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 2 - Simple Transaction Processing
 */
@Slf4j
@Component
public class TransactionTypePartitioner implements Partitioner {

    @Autowired
    private YamlMappingService yamlMappingService;
    
    @Autowired
    private BatchTransactionTypeRepository transactionTypeRepository;

    private final Map<String, Object> systemConfig;
    private final Map<String, Object> jobConfig;
    private final String sourceSystem;
    private final String jobName;
    private final String executionId;
    private final AtomicInteger partitionCounter = new AtomicInteger(0);

    /**
     * Constructor for dependency injection and configuration
     */
    public TransactionTypePartitioner(Map<String, Object> systemConfig,
                                    Map<String, Object> jobConfig,
                                    String sourceSystem,
                                    String jobName) {
        this.systemConfig = systemConfig;
        this.jobConfig = jobConfig;
        this.sourceSystem = sourceSystem;
        this.jobName = jobName;
        this.executionId = generateExecutionId();
        
        log.info("🎯 Initializing TransactionTypePartitioner for {}.{} with executionId: {}", 
                sourceSystem, jobName, executionId);
    }

    /**
     * Creates partition contexts based on database-configured transaction types.
     * Each partition represents a specific transaction type that can be processed in parallel.
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        log.info("🔄 Starting Epic 2 transaction-type-based partitioning for {}.{}", 
                sourceSystem, jobName);
        
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        try {
            // 1. Load database-configured transaction types for this job
            List<BatchTransactionTypeEntity> transactionTypes = 
                loadTransactionTypesFromDatabase();
            
            if (transactionTypes.isEmpty()) {
                log.warn("⚠️ No transaction types found in database, falling back to file-based partitioning");
                return createFileBasedPartitions(gridSize);
            }
            
            // 2. Create partitions for each active transaction type
            for (BatchTransactionTypeEntity transactionType : transactionTypes) {
                if (!transactionType.isActive()) {
                    log.debug("⏸️ Skipping inactive transaction type: {}", 
                            transactionType.getTransactionType());
                    continue;
                }
                
                ExecutionContext partitionContext = createPartitionContext(transactionType);
                String partitionKey = generatePartitionKey(transactionType);
                
                partitions.put(partitionKey, partitionContext);
                
                log.info("✅ Created partition: {} for transaction type: {} with {} threads, chunk size: {}", 
                        partitionKey, 
                        transactionType.getTransactionType(),
                        transactionType.getParallelThreads(),
                        transactionType.getChunkSize());
            }
            
            // 3. Log partition summary
            logPartitioningSummary(partitions, transactionTypes);
            
            return partitions;
            
        } catch (Exception e) {
            log.error("❌ Failed to create transaction type partitions for {}.{}", 
                    sourceSystem, jobName, e);
            throw new RuntimeException("Transaction type partitioning failed: " + e.getMessage(), e);
        }
    }

    /**
     * Loads transaction types from database for the current job configuration
     */
    private List<BatchTransactionTypeEntity> loadTransactionTypesFromDatabase() {
        try {
            // Find job configuration ID (this would be enhanced with proper lookup)
            Long jobConfigId = findJobConfigurationId();
            
            if (jobConfigId == null) {
                log.warn("⚠️ Job configuration not found for {}.{}, cannot load transaction types", 
                        sourceSystem, jobName);
                return List.of();
            }
            
            List<BatchTransactionTypeEntity> transactionTypes = 
                transactionTypeRepository.findByJobConfigIdAndActiveFlag(jobConfigId, "Y");
            
            // Sort by processing order for deterministic execution
            transactionTypes.sort((t1, t2) -> 
                Integer.compare(t1.getProcessingOrder(), t2.getProcessingOrder()));
            
            log.info("📊 Loaded {} active transaction types from database for job config ID: {}", 
                    transactionTypes.size(), jobConfigId);
            
            return transactionTypes;
            
        } catch (Exception e) {
            log.error("❌ Failed to load transaction types from database", e);
            return List.of();
        }
    }

    /**
     * Creates execution context for a specific transaction type
     */
    private ExecutionContext createPartitionContext(BatchTransactionTypeEntity transactionType) {
        ExecutionContext context = new ExecutionContext();
        
        // Core identification
        context.put("executionId", executionId);
        context.put("correlationId", generateCorrelationId());
        context.put("partitionId", partitionCounter.incrementAndGet());
        context.put("sourceSystem", sourceSystem);
        context.put("jobName", jobName);
        
        // Transaction type configuration
        context.put("transactionTypeId", transactionType.getTransactionTypeId());
        context.put("transactionType", transactionType.getTransactionType());
        context.put("processingOrder", transactionType.getProcessingOrder());
        
        // Parallel processing configuration
        context.put("parallelThreads", transactionType.getParallelThreads());
        context.put("chunkSize", transactionType.getChunkSize());
        context.put("isolationLevel", transactionType.getIsolationLevel());
        context.put("timeoutSeconds", transactionType.getTimeoutSeconds());
        
        // Retry and resilience configuration
        BatchTransactionTypeEntity.RetryConfiguration retryConfig = 
            transactionType.getRetryConfiguration();
        context.put("retryConfig", retryConfig);
        
        // Security and compliance
        context.put("complianceLevel", transactionType.getComplianceLevel());
        context.put("encryptionRequired", transactionType.requiresEncryption());
        context.put("encryptionFields", transactionType.getEncryptionFields());
        
        // Performance monitoring
        context.put("startTime", Instant.now());
        context.put("threadId", Thread.currentThread().getName());
        
        // File configuration (backward compatibility)
        FileConfig fileConfig = createFileConfigFromTransactionType(transactionType);
        context.put("fileConfig", fileConfig);
        
        return context;
    }

    /**
     * Creates FileConfig object for backward compatibility with existing processors
     */
    private FileConfig createFileConfigFromTransactionType(BatchTransactionTypeEntity transactionType) {
        FileConfig fileConfig = new FileConfig();
        fileConfig.setSourceSystem(sourceSystem);
        fileConfig.setJobName(jobName);
        fileConfig.setTransactionType(transactionType.getTransactionType());
        
        // Set template path based on existing convention
        String templatePath = String.format("%s/%s/%s.yml", 
                sourceSystem.toLowerCase(), 
                jobName.toLowerCase(),
                transactionType.getTransactionType().toLowerCase());
        fileConfig.setTemplate(templatePath);
        
        // Add Epic 2 specific parameters
        Map<String, String> params = new HashMap<>();
        params.put("parallelThreads", String.valueOf(transactionType.getParallelThreads()));
        params.put("chunkSize", String.valueOf(transactionType.getChunkSize()));
        params.put("isolationLevel", transactionType.getIsolationLevel());
        params.put("complianceLevel", transactionType.getComplianceLevel());
        fileConfig.setParams(params);
        
        return fileConfig;
    }

    /**
     * Fallback to file-based partitioning when database configuration is not available
     */
    private Map<String, ExecutionContext> createFileBasedPartitions(int gridSize) {
        log.info("🔄 Falling back to file-based partitioning for {}.{}", sourceSystem, jobName);
        
        // Create legacy GenericPartitioner for backward compatibility
        GenericPartitioner legacyPartitioner = new GenericPartitioner(
                yamlMappingService, systemConfig, jobConfig, sourceSystem, jobName);
        
        Map<String, ExecutionContext> partitions = legacyPartitioner.partition(gridSize);
        
        // Enhance legacy partitions with Epic 2 metadata
        partitions.forEach((key, context) -> {
            context.put("executionId", executionId);
            context.put("correlationId", generateCorrelationId());
            context.put("epic2Fallback", true);
            context.put("startTime", Instant.now());
        });
        
        return partitions;
    }

    /**
     * Generates unique partition key for a transaction type
     */
    private String generatePartitionKey(BatchTransactionTypeEntity transactionType) {
        return String.format("epic2_partition_%d_%s_%s_%s", 
                partitionCounter.get(),
                sourceSystem.toLowerCase(),
                jobName.toLowerCase(),
                transactionType.getTransactionType().toLowerCase());
    }

    /**
     * Generates unique execution ID for this partitioning session
     */
    private String generateExecutionId() {
        return String.format("%s_%s_%d", 
                sourceSystem.toUpperCase(),
                jobName.toUpperCase(),
                System.currentTimeMillis());
    }

    /**
     * Generates correlation ID for tracing
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Placeholder for job configuration ID lookup
     * In a real implementation, this would query BATCH_CONFIGURATIONS table
     */
    private Long findJobConfigurationId() {
        // This is a placeholder - in real implementation would query:
        // SELECT config_id FROM BATCH_CONFIGURATIONS 
        // WHERE source_system = ? AND job_name = ? AND deployment_status = 'ACTIVE'
        
        // For now, return a mock ID
        return 1L; // This should be replaced with actual database lookup
    }

    /**
     * Logs comprehensive partitioning summary for monitoring and debugging
     */
    private void logPartitioningSummary(Map<String, ExecutionContext> partitions, 
                                      List<BatchTransactionTypeEntity> transactionTypes) {
        
        int totalThreads = transactionTypes.stream()
                .mapToInt(BatchTransactionTypeEntity::getParallelThreads)
                .sum();
        
        int avgChunkSize = (int) transactionTypes.stream()
                .mapToInt(BatchTransactionTypeEntity::getChunkSize)
                .average()
                .orElse(1000);
        
        log.info("""
                📊 Epic 2 Partitioning Summary:
                🎯 Execution ID: {}
                🏢 Source System: {}
                📋 Job Name: {}
                🔢 Total Partitions: {}
                🧵 Total Thread Allocation: {}
                📦 Average Chunk Size: {}
                🔒 High Compliance Types: {}
                ⚙️ Transaction Types: {}
                """,
                executionId,
                sourceSystem,
                jobName,
                partitions.size(),
                totalThreads,
                avgChunkSize,
                transactionTypes.stream().mapToLong(t -> t.isHighComplianceLevel() ? 1 : 0).sum(),
                transactionTypes.stream()
                        .map(BatchTransactionTypeEntity::getTransactionType)
                        .toList());
    }

    /**
     * Performance optimization method to determine optimal grid size
     */
    public static int calculateOptimalGridSize(List<BatchTransactionTypeEntity> transactionTypes,
                                             long estimatedRecordCount) {
        if (transactionTypes.isEmpty()) {
            return 1;
        }
        
        // Calculate based on total thread capacity and estimated workload
        int totalThreads = transactionTypes.stream()
                .mapToInt(BatchTransactionTypeEntity::getParallelThreads)
                .sum();
        
        int avgChunkSize = (int) transactionTypes.stream()
                .mapToInt(BatchTransactionTypeEntity::getChunkSize)
                .average()
                .orElse(1000);
        
        // Optimal grid size should allow for efficient thread utilization
        int optimalSize = Math.max(1, 
                Math.min(totalThreads, (int) (estimatedRecordCount / avgChunkSize)));
        
        log.info("📐 Calculated optimal grid size: {} for {} records across {} threads",
                optimalSize, estimatedRecordCount, totalThreads);
        
        return optimalSize;
    }
}