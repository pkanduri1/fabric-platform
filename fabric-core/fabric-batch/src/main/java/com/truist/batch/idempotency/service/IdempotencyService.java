package com.truist.batch.idempotency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.idempotency.entity.FabricIdempotencyKeyEntity;
import com.truist.batch.idempotency.entity.FabricIdempotencyKeyEntity.ProcessingState;
import com.truist.batch.idempotency.entity.FabricIdempotencyAuditEntity;
import com.truist.batch.idempotency.entity.FabricIdempotencyConfigEntity;
import com.truist.batch.idempotency.entity.FabricIdempotencyConfigEntity.ConfigType;
import com.truist.batch.idempotency.exception.IdempotencyException;
import com.truist.batch.idempotency.model.*;
import com.truist.batch.idempotency.repository.FabricIdempotencyKeyRepository;
import com.truist.batch.idempotency.repository.FabricIdempotencyAuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Core service for managing idempotent operations in the Fabric Platform.
 * Provides comprehensive idempotency guarantees for batch jobs and API requests
 * with enterprise-grade state management, audit trails, and performance optimization.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Service
@Transactional
@Slf4j
public class IdempotencyService {
    
    private final FabricIdempotencyKeyRepository idempotencyKeyRepository;
    private final FabricIdempotencyAuditRepository idempotencyAuditRepository;
    private final IdempotencyKeyGenerator keyGenerator;
    private final IdempotencyConfigService configService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    // Configuration constants
    private static final int STALE_TIMEOUT_MINUTES = 30;
    private static final int MAX_OPTIMISTIC_LOCK_RETRIES = 3;
    private static final int PROCESSING_TIMEOUT_MINUTES = 60;
    
    public IdempotencyService(
            FabricIdempotencyKeyRepository idempotencyKeyRepository,
            FabricIdempotencyAuditRepository idempotencyAuditRepository,
            IdempotencyKeyGenerator keyGenerator,
            IdempotencyConfigService configService,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.idempotencyAuditRepository = idempotencyAuditRepository;
        this.keyGenerator = keyGenerator;
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Process request with idempotency guarantee for batch jobs.
     */
    public <T> IdempotencyResult<T> processWithIdempotencyForBatchJob(
            IdempotencyRequest request,
            Supplier<T> businessLogic,
            Class<T> responseType) {
        
        request.validate();
        return processWithIdempotency(ConfigType.BATCH_JOB, request, businessLogic, responseType);
    }
    
    /**
     * Process request with idempotency guarantee for API endpoints.
     */
    public <T> IdempotencyResult<T> processWithIdempotencyForApi(
            IdempotencyRequest request,
            Supplier<T> businessLogic,
            Class<T> responseType) {
        
        request.validate();
        return processWithIdempotency(ConfigType.API_ENDPOINT, request, businessLogic, responseType);
    }
    
    /**
     * Core method for processing requests with idempotency guarantee.
     */
    private <T> IdempotencyResult<T> processWithIdempotency(
            ConfigType configType,
            IdempotencyRequest request,
            Supplier<T> businessLogic,
            Class<T> responseType) {
        
        long startTime = System.currentTimeMillis();
        String correlationId = keyGenerator.generateCorrelationId();
        
        log.debug("Processing idempotent request with correlation ID: {} for target: {}/{}", 
                correlationId, configType, request.getJobName());
        
        try {
            // Check if idempotency is enabled
            if (!configService.isIdempotencyEnabled(configType, request.getJobName())) {
                log.debug("Idempotency disabled for {}/{}, executing directly", 
                        configType, request.getJobName());
                T result = businessLogic.get();
                return createSuccessResult(result, "DIRECT_EXECUTION", correlationId, 
                        System.currentTimeMillis() - startTime);
            }
            
            // Generate or extract idempotency key
            String idempotencyKey = keyGenerator.generateKey(request);
            
            // Check for existing processing
            Optional<FabricIdempotencyKeyEntity> existing = 
                idempotencyKeyRepository.findById(idempotencyKey);
            
            if (existing.isPresent()) {
                return handleExistingRequest(existing.get(), request, responseType, startTime);
            }
            
            // Create new idempotency record
            FabricIdempotencyKeyEntity idempotencyEntity = createIdempotencyRecord(
                idempotencyKey, request, correlationId, configType
            );
            
            // Execute business logic with error handling
            return executeBusinessLogic(idempotencyEntity, businessLogic, responseType, startTime);
            
        } catch (Exception e) {
            log.error("Error in idempotent processing for correlation ID: {} - {}", 
                    correlationId, e.getMessage(), e);
            throw new IdempotencyException(
                    "Idempotent processing failed: " + e.getMessage(), 
                    correlationId, e
            );
        }
    }
    
    /**
     * Handle existing idempotency request based on current state.
     */
    private <T> IdempotencyResult<T> handleExistingRequest(
            FabricIdempotencyKeyEntity existing,
            IdempotencyRequest request,
            Class<T> responseType,
            long startTime) {
        
        log.debug("Found existing idempotency record: {}", existing.getSummary());
        updateLastAccessed(existing.getIdempotencyKey());
        
        switch (existing.getProcessingState()) {
            case COMPLETED:
                log.debug("Returning cached result for key: {}", existing.getIdempotencyKey());
                auditStateAccess(existing, "Cached result returned", getCurrentRequestContext());
                T cachedResult = deserializeResponse(existing.getResponsePayload(), responseType);
                return createCachedResult(cachedResult, existing.getIdempotencyKey(), 
                        existing.getCorrelationId(), System.currentTimeMillis() - startTime);
                
            case IN_PROGRESS:
                if (isRequestStale(existing)) {
                    log.warn("Stale in-progress request detected, attempting recovery: {}", 
                            existing.getIdempotencyKey());
                    return handleStaleRequest(existing, request, responseType, startTime);
                } else {
                    log.debug("Request already in progress: {}", existing.getIdempotencyKey());
                    return createInProgressResult(existing.getIdempotencyKey(), 
                            existing.getCorrelationId());
                }
                
            case FAILED:
                if (canRetry(existing)) {
                    log.debug("Retrying failed request: {}", existing.getIdempotencyKey());
                    return handleRetryRequest(existing, request, responseType, startTime);
                } else {
                    log.debug("Max retries exceeded for: {}", existing.getIdempotencyKey());
                    return createMaxRetriesExceededResult(existing.getErrorDetails(),
                            existing.getIdempotencyKey(), existing.getCorrelationId());
                }
                
            case EXPIRED:
                log.debug("Expired request, creating new processing: {}", existing.getIdempotencyKey());
                return handleExpiredRequest(existing, request, responseType, startTime);
                
            default:
                throw new IdempotencyException(
                    "Unknown processing state: " + existing.getProcessingState(),
                    existing.getCorrelationId()
                );
        }
    }
    
    /**
     * Execute business logic with comprehensive state management.
     */
    private <T> IdempotencyResult<T> executeBusinessLogic(
            FabricIdempotencyKeyEntity idempotencyEntity,
            Supplier<T> businessLogic,
            Class<T> responseType,
            long startTime) {
        
        String idempotencyKey = idempotencyEntity.getIdempotencyKey();
        
        try {
            // Update state to IN_PROGRESS
            updateIdempotencyState(
                idempotencyEntity, 
                ProcessingState.IN_PROGRESS, 
                "Business logic execution started"
            );
            
            log.debug("Executing business logic for key: {}", idempotencyKey);
            
            // Execute business logic
            T result = businessLogic.get();
            
            // Serialize and store result
            String serializedResult = serializeResponse(result);
            
            // Update to completed state with optimistic locking
            boolean updated = updateToCompletedState(idempotencyEntity, serializedResult);
            
            if (!updated) {
                log.warn("Failed to update to completed state due to concurrent modification: {}", 
                        idempotencyKey);
                // Return result anyway - operation succeeded
            }
            
            log.debug("Successfully completed idempotent processing: {}", idempotencyKey);
            
            return createSuccessResult(result, idempotencyKey, 
                    idempotencyEntity.getCorrelationId(), 
                    System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("Business logic execution failed for key: {} - {}", 
                    idempotencyKey, e.getMessage(), e);
            
            // Update state to FAILED
            updateToFailedState(idempotencyEntity, e);
            
            throw new IdempotencyException(
                "Business logic execution failed",
                idempotencyEntity.getCorrelationId(),
                idempotencyKey,
                "BUSINESS_LOGIC_FAILURE",
                true, // retryable
                e
            );
        }
    }
    
    /**
     * Creates a new idempotency record with proper configuration.
     */
    private FabricIdempotencyKeyEntity createIdempotencyRecord(
            String idempotencyKey,
            IdempotencyRequest request,
            String correlationId,
            ConfigType configType) {
        
        FabricIdempotencyConfigEntity config = 
                configService.getConfigForTarget(configType, request.getJobName());
        
        LocalDateTime now = LocalDateTime.now();
        int ttlSeconds = request.getEffectiveTtlSeconds(config.getTtlSeconds());
        
        FabricIdempotencyKeyEntity entity = FabricIdempotencyKeyEntity.builder()
                .idempotencyKey(idempotencyKey)
                .sourceSystem(request.getSourceSystem())
                .jobName(request.getJobName())
                .transactionId(request.getTransactionId())
                .fileHash(request.getFileHash())
                .requestHash(request.getRequestHash())
                .processingState(ProcessingState.STARTED)
                .correlationId(correlationId)
                .ttlSeconds(ttlSeconds)
                .createdDate(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .retryCount(0)
                .maxRetries(request.getEffectiveMaxRetries(config.getMaxRetries()))
                .createdBy(getCurrentUserId())
                .lastAccessed(now)
                .processingNode(getCurrentProcessingNode())
                .lockVersion(0)
                .build();
        
        // Store request payload if configured
        if (config.shouldStoreRequestPayload() && request.hasPayload()) {
            entity.setRequestPayload(request.getRequestPayload());
        }
        
        entity = idempotencyKeyRepository.save(entity);
        
        // Create initial audit entry
        auditStateChange(entity, null, ProcessingState.STARTED, 
                "Idempotency record created", getCurrentRequestContext());
        
        log.debug("Created new idempotency record: {}", entity.getSummary());
        return entity;
    }
    
    /**
     * Update idempotency state with comprehensive audit trail.
     */
    private void updateIdempotencyState(
            FabricIdempotencyKeyEntity entity,
            ProcessingState newState,
            String reason) {
        
        ProcessingState oldState = entity.getProcessingState();
        entity.setProcessingState(newState);
        entity.setLastAccessed(LocalDateTime.now());
        
        if (newState == ProcessingState.COMPLETED || newState == ProcessingState.FAILED) {
            entity.setCompletedDate(LocalDateTime.now());
        }
        
        try {
            idempotencyKeyRepository.save(entity);
            auditStateChange(entity, oldState, newState, reason, getCurrentRequestContext());
            
            // Publish event for monitoring
            publishStateChangeEvent(entity, oldState, newState);
            
        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure updating state for key: {} - concurrent modification", 
                    entity.getIdempotencyKey());
            // Let caller handle this appropriately
            throw e;
        }
    }
    
    /**
     * Update to completed state with optimistic locking and retry logic.
     */
    private boolean updateToCompletedState(FabricIdempotencyKeyEntity entity, String responsePayload) {
        int attempts = 0;
        while (attempts < MAX_OPTIMISTIC_LOCK_RETRIES) {
            try {
                entity.setProcessingState(ProcessingState.COMPLETED);
                entity.setCompletedDate(LocalDateTime.now());
                entity.setResponsePayload(responsePayload);
                entity.setErrorDetails(null); // Clear any previous errors
                
                idempotencyKeyRepository.save(entity);
                
                auditStateChange(entity, ProcessingState.IN_PROGRESS, ProcessingState.COMPLETED,
                        "Business logic execution completed successfully", getCurrentRequestContext());
                
                publishStateChangeEvent(entity, ProcessingState.IN_PROGRESS, ProcessingState.COMPLETED);
                
                return true;
                
            } catch (OptimisticLockingFailureException e) {
                attempts++;
                log.warn("Optimistic locking failure (attempt {}/{}) for key: {}", 
                        attempts, MAX_OPTIMISTIC_LOCK_RETRIES, entity.getIdempotencyKey());
                
                if (attempts >= MAX_OPTIMISTIC_LOCK_RETRIES) {
                    log.error("Failed to update to completed state after {} attempts for key: {}", 
                            MAX_OPTIMISTIC_LOCK_RETRIES, entity.getIdempotencyKey());
                    return false;
                }
                
                // Refresh entity and try again
                Optional<FabricIdempotencyKeyEntity> refreshed = 
                        idempotencyKeyRepository.findById(entity.getIdempotencyKey());
                if (refreshed.isPresent()) {
                    entity = refreshed.get();
                } else {
                    log.error("Entity no longer exists during optimistic lock retry: {}", 
                            entity.getIdempotencyKey());
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * Update to failed state with error details.
     */
    private void updateToFailedState(FabricIdempotencyKeyEntity entity, Exception error) {
        try {
            entity.setProcessingState(ProcessingState.FAILED);
            entity.setCompletedDate(LocalDateTime.now());
            entity.setErrorDetails(formatException(error));
            entity.setResponsePayload(null); // Clear any previous response
            entity.incrementRetryCount();
            
            idempotencyKeyRepository.save(entity);
            
            auditStateChange(entity, ProcessingState.IN_PROGRESS, ProcessingState.FAILED,
                    "Business logic execution failed: " + error.getMessage(), getCurrentRequestContext());
            
            publishStateChangeEvent(entity, ProcessingState.IN_PROGRESS, ProcessingState.FAILED);
            
        } catch (Exception e) {
            log.error("Failed to update entity to failed state: {}", entity.getIdempotencyKey(), e);
            // Continue - the original business logic failure should be reported
        }
    }
    
    // Continued in next part due to length...
    
    // Helper methods
    
    private boolean isRequestStale(FabricIdempotencyKeyEntity entity) {
        return entity.isStale(STALE_TIMEOUT_MINUTES);
    }
    
    private boolean canRetry(FabricIdempotencyKeyEntity entity) {
        return entity.canRetry();
    }
    
    private String getCurrentUserId() {
        // TODO: Implement security context integration
        return "SYSTEM_USER";
    }
    
    private String getCurrentProcessingNode() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "UNKNOWN_NODE";
        }
    }
    
    private RequestContext getCurrentRequestContext() {
        // TODO: Implement proper context extraction
        return RequestContext.forSystemOperation("Idempotency Processing");
    }
    
    private String formatException(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
        if (e.getCause() != null) {
            sb.append(" (Caused by: ").append(e.getCause().getMessage()).append(")");
        }
        return sb.toString();
    }
    
    private <T> String serializeResponse(T response) {
        if (response == null) return null;
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response", e);
            return response.toString();
        }
    }
    
    private <T> T deserializeResponse(String responsePayload, Class<T> responseType) {
        if (responsePayload == null || responseType == null) return null;
        try {
            return objectMapper.readValue(responsePayload, responseType);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize response payload", e);
            return null;
        }
    }
    
    private void updateLastAccessed(String idempotencyKey) {
        try {
            idempotencyKeyRepository.updateLastAccessed(idempotencyKey);
        } catch (Exception e) {
            log.warn("Failed to update last accessed timestamp for key: {}", idempotencyKey, e);
        }
    }
    
    private void auditStateChange(FabricIdempotencyKeyEntity entity, ProcessingState oldState,
                                 ProcessingState newState, String reason, RequestContext context) {
        try {
            FabricIdempotencyAuditEntity audit = FabricIdempotencyAuditEntity.builder()
                .idempotencyKey(entity.getIdempotencyKey())
                .oldState(oldState)
                .newState(newState)
                .stateChangeReason(reason)
                .changedBy(context.getUserId())
                .clientIp(context.getClientIp())
                .userAgent(context.getUserAgent())
                .sessionId(context.getSessionId())
                .businessContext(entity.getJobName())
                .processingContext(createProcessingContext(entity, context))
                .build();
                
            idempotencyAuditRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to create audit trail for key: {}", entity.getIdempotencyKey(), e);
        }
    }
    
    private void auditStateAccess(FabricIdempotencyKeyEntity entity, String reason, RequestContext context) {
        auditStateChange(entity, entity.getProcessingState(), entity.getProcessingState(), reason, context);
    }
    
    private String createProcessingContext(FabricIdempotencyKeyEntity entity, RequestContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    private void publishStateChangeEvent(FabricIdempotencyKeyEntity entity, 
                                       ProcessingState oldState, ProcessingState newState) {
        // TODO: Implement event publishing for monitoring
        log.debug("State change event: {} -> {} for key: {}", oldState, newState, entity.getIdempotencyKey());
    }
    
    // Factory methods for results
    
    private <T> IdempotencyResult<T> createSuccessResult(T data, String idempotencyKey, 
                                                        String correlationId, long durationMs) {
        return IdempotencyResult.<T>builder()
                .data(data)
                .status(IdempotencyStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .processingDurationMs(durationMs)
                .fromCache(false)
                .build();
    }
    
    private <T> IdempotencyResult<T> createCachedResult(T data, String idempotencyKey, 
                                                       String correlationId, long durationMs) {
        return IdempotencyResult.<T>builder()
                .data(data)
                .status(IdempotencyStatus.CACHED_RESULT)
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .processingDurationMs(durationMs)
                .fromCache(true)
                .build();
    }
    
    private <T> IdempotencyResult<T> createInProgressResult(String idempotencyKey, String correlationId) {
        return IdempotencyResult.<T>builder()
                .status(IdempotencyStatus.IN_PROGRESS)
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .fromCache(false)
                .build();
    }
    
    private <T> IdempotencyResult<T> createMaxRetriesExceededResult(String errorDetails,
                                                                   String idempotencyKey, String correlationId) {
        return IdempotencyResult.<T>builder()
                .status(IdempotencyStatus.MAX_RETRIES_EXCEEDED)
                .errorMessage("Maximum retry attempts exceeded")
                .errorDetails(errorDetails)
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .fromCache(false)
                .build();
    }
    
    // Placeholder methods for handling different scenarios (to be implemented in continuation)
    private <T> IdempotencyResult<T> handleStaleRequest(FabricIdempotencyKeyEntity existing,
                                                       IdempotencyRequest request, Class<T> responseType, long startTime) {
        // Implementation placeholder
        throw new UnsupportedOperationException("Stale request handling not yet implemented");
    }
    
    private <T> IdempotencyResult<T> handleRetryRequest(FabricIdempotencyKeyEntity existing,
                                                       IdempotencyRequest request, Class<T> responseType, long startTime) {
        // Implementation placeholder  
        throw new UnsupportedOperationException("Retry request handling not yet implemented");
    }
    
    private <T> IdempotencyResult<T> handleExpiredRequest(FabricIdempotencyKeyEntity existing,
                                                         IdempotencyRequest request, Class<T> responseType, long startTime) {
        // Implementation placeholder
        throw new UnsupportedOperationException("Expired request handling not yet implemented");
    }
}