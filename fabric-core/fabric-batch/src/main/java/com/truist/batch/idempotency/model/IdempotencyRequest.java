package com.truist.batch.idempotency.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Model representing an idempotency request for batch jobs or API calls.
 * Contains all necessary information to uniquely identify and process
 * idempotent operations with comprehensive context tracking.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRequest {
    
    /**
     * Source system identifier (e.g., HR, MTG, SHAW)
     */
    private String sourceSystem;
    
    /**
     * Job name or API endpoint identifier
     */
    private String jobName;
    
    /**
     * Transaction identifier (often filename or request ID)
     */
    private String transactionId;
    
    /**
     * File path for file-based operations
     */
    private String filePath;
    
    /**
     * SHA-256 hash of the file content (for file-based operations)
     */
    private String fileHash;
    
    /**
     * SHA-256 hash of the request parameters
     */
    private String requestHash;
    
    /**
     * Request payload (JSON string or serialized parameters)
     */
    private String requestPayload;
    
    /**
     * Job parameters for batch jobs
     */
    private Map<String, Object> jobParameters;
    
    /**
     * HTTP headers for API requests
     */
    private Map<String, String> httpHeaders;
    
    /**
     * Request context information
     */
    private RequestContext requestContext;
    
    /**
     * Custom attributes for extensibility
     */
    private Map<String, Object> customAttributes;
    
    /**
     * Client-provided idempotency key (optional)
     */
    private String clientProvidedKey;
    
    /**
     * TTL override in seconds (optional)
     */
    private Integer ttlOverrideSeconds;
    
    /**
     * Max retries override (optional)
     */
    private Integer maxRetriesOverride;
    
    /**
     * Processing priority (optional)
     */
    private ProcessingPriority priority;
    
    /**
     * Processing priority levels
     */
    public enum ProcessingPriority {
        LOW(1),
        NORMAL(5),
        HIGH(10),
        CRITICAL(20);
        
        private final int value;
        
        ProcessingPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    // Utility methods
    
    /**
     * Creates a unique identifier for this request based on core attributes.
     */
    public String createUniqueIdentifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceSystem != null ? sourceSystem : "UNKNOWN");
        sb.append(":");
        sb.append(jobName != null ? jobName : "UNKNOWN");
        sb.append(":");
        sb.append(transactionId != null ? transactionId : "NONE");
        
        if (fileHash != null && !fileHash.isEmpty()) {
            sb.append(":FILE_").append(fileHash.substring(0, Math.min(8, fileHash.length())));
        }
        
        if (requestHash != null && !requestHash.isEmpty()) {
            sb.append(":REQ_").append(requestHash.substring(0, Math.min(8, requestHash.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * Checks if this request has file-based content.
     */
    public boolean hasFileContent() {
        return filePath != null && !filePath.trim().isEmpty();
    }
    
    /**
     * Checks if this request has a payload.
     */
    public boolean hasPayload() {
        return requestPayload != null && !requestPayload.trim().isEmpty();
    }
    
    /**
     * Checks if this request has job parameters.
     */
    public boolean hasJobParameters() {
        return jobParameters != null && !jobParameters.isEmpty();
    }
    
    /**
     * Checks if this request has HTTP headers.
     */
    public boolean hasHttpHeaders() {
        return httpHeaders != null && !httpHeaders.isEmpty();
    }
    
    /**
     * Checks if this request has custom attributes.
     */
    public boolean hasCustomAttributes() {
        return customAttributes != null && !customAttributes.isEmpty();
    }
    
    /**
     * Checks if client provided their own idempotency key.
     */
    public boolean hasClientProvidedKey() {
        return clientProvidedKey != null && !clientProvidedKey.trim().isEmpty();
    }
    
    /**
     * Gets processing priority, defaulting to NORMAL if not specified.
     */
    public ProcessingPriority getEffectivePriority() {
        return priority != null ? priority : ProcessingPriority.NORMAL;
    }
    
    /**
     * Gets TTL in seconds, using override if provided.
     */
    public Integer getEffectiveTtlSeconds(Integer defaultTtl) {
        return ttlOverrideSeconds != null ? ttlOverrideSeconds : defaultTtl;
    }
    
    /**
     * Gets max retries, using override if provided.
     */
    public Integer getEffectiveMaxRetries(Integer defaultMaxRetries) {
        return maxRetriesOverride != null ? maxRetriesOverride : defaultMaxRetries;
    }
    
    /**
     * Creates a summary string for logging.
     */
    public String getSummary() {
        return String.format("IdempotencyRequest[%s/%s/%s, priority=%s]",
                sourceSystem, jobName, transactionId, getEffectivePriority());
    }
    
    /**
     * Validates that required fields are present.
     */
    public void validate() {
        if (sourceSystem == null || sourceSystem.trim().isEmpty()) {
            throw new IllegalArgumentException("Source system is required");
        }
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new IllegalArgumentException("Job name is required");
        }
        // transactionId can be null for some use cases
    }
    
    /**
     * Factory method for batch job requests.
     */
    public static IdempotencyRequest forBatchJob(String sourceSystem, String jobName, 
                                               String transactionId, String filePath) {
        return IdempotencyRequest.builder()
                .sourceSystem(sourceSystem)
                .jobName(jobName)
                .transactionId(transactionId)
                .filePath(filePath)
                .priority(ProcessingPriority.NORMAL)
                .build();
    }
    
    /**
     * Factory method for API requests.
     */
    public static IdempotencyRequest forApiRequest(String sourceSystem, String endpoint, 
                                                 String requestId, String requestPayload) {
        return IdempotencyRequest.builder()
                .sourceSystem(sourceSystem)
                .jobName(endpoint)
                .transactionId(requestId)
                .requestPayload(requestPayload)
                .priority(ProcessingPriority.NORMAL)
                .build();
    }
}