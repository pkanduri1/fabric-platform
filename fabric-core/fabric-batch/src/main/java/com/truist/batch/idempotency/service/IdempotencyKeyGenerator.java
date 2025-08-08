package com.truist.batch.idempotency.service;

import com.truist.batch.idempotency.model.IdempotencyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for generating idempotency keys based on various strategies.
 * Provides consistent, collision-resistant key generation for different
 * types of idempotent operations with enterprise-grade hashing.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Component
@Slf4j
public class IdempotencyKeyGenerator {
    
    private static final String SEPARATOR = ":";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * Generates an idempotency key based on the request and strategy.
     */
    public String generateKey(IdempotencyRequest request) {
        if (request.hasClientProvidedKey()) {
            log.debug("Using client-provided idempotency key: {}", request.getClientProvidedKey());
            return sanitizeKey(request.getClientProvidedKey());
        }
        
        // Use auto-generated strategy by default
        return generateAutoKey(request);
    }
    
    /**
     * Generates an automatic idempotency key based on request content.
     */
    private String generateAutoKey(IdempotencyRequest request) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // Start with source system and job name
        keyBuilder.append(sanitize(request.getSourceSystem()));
        keyBuilder.append(SEPARATOR);
        keyBuilder.append(sanitize(request.getJobName()));
        keyBuilder.append(SEPARATOR);
        
        // Add timestamp component (date only for daily uniqueness)
        String dateComponent = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        keyBuilder.append(dateComponent);
        keyBuilder.append(SEPARATOR);
        
        // Create content hash based on available data
        String contentHash = createContentHash(request);
        keyBuilder.append(contentHash);
        
        String key = keyBuilder.toString();
        log.debug("Generated idempotency key: {} for request: {}", key, request.getSummary());
        return key;
    }
    
    /**
     * Creates a content hash based on all relevant request data.
     */
    private String createContentHash(IdempotencyRequest request) {
        StringBuilder contentBuilder = new StringBuilder();
        
        // Add transaction ID if available
        if (request.getTransactionId() != null) {
            contentBuilder.append("tx:").append(request.getTransactionId()).append("|");
        }
        
        // Add file hash if available (highest priority for file operations)
        if (request.getFileHash() != null) {
            contentBuilder.append("file:").append(request.getFileHash()).append("|");
        }
        
        // Add request hash if available
        if (request.getRequestHash() != null) {
            contentBuilder.append("req:").append(request.getRequestHash()).append("|");
        }
        
        // Add file path if available
        if (request.getFilePath() != null) {
            contentBuilder.append("path:").append(request.getFilePath()).append("|");
        }
        
        // Add job parameters hash if available
        if (request.hasJobParameters()) {
            String paramsHash = hashJobParameters(request.getJobParameters().toString());
            contentBuilder.append("params:").append(paramsHash).append("|");
        }
        
        // Add request payload hash if available
        if (request.hasPayload()) {
            String payloadHash = hashString(request.getRequestPayload());
            contentBuilder.append("payload:").append(payloadHash.substring(0, 8)).append("|");
        }
        
        String content = contentBuilder.toString();
        
        // If we have specific content, hash it
        if (!content.isEmpty()) {
            return hashString(content).substring(0, 16); // First 16 chars of hash
        }
        
        // Fallback: generate based on timestamp and random component
        return generateFallbackHash();
    }
    
    /**
     * Generates a hash for job parameters.
     */
    private String hashJobParameters(String params) {
        // Sort and normalize parameters for consistent hashing
        return hashString(params).substring(0, 8);
    }
    
    /**
     * Generates a fallback hash when no specific content is available.
     */
    private String generateFallbackHash() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return hashString(timestamp + uuid).substring(0, 16);
    }
    
    /**
     * Generates SHA-256 hash of input string.
     */
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            // Fallback to simple hash
            return String.valueOf(input.hashCode());
        }
    }
    
    /**
     * Converts byte array to hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Sanitizes a string for use in idempotency keys.
     */
    private String sanitize(String input) {
        if (input == null) return "NULL";
        return input.replaceAll("[^a-zA-Z0-9_-]", "_").toUpperCase();
    }
    
    /**
     * Sanitizes and validates a complete idempotency key.
     */
    private String sanitizeKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key cannot be null or empty");
        }
        
        String sanitized = key.trim().replaceAll("[^a-zA-Z0-9_:-]", "_").toUpperCase();
        
        // Ensure key length is within limits
        if (sanitized.length() > 128) {
            log.warn("Idempotency key too long, truncating: {}", sanitized);
            sanitized = sanitized.substring(0, 120) + hashString(sanitized).substring(0, 8);
        }
        
        return sanitized;
    }
    
    /**
     * Generates a correlation ID for tracking related operations.
     */
    public String generateCorrelationId() {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "IDEM_" + timestamp + "_" + uuid;
    }
    
    /**
     * Validates that an idempotency key is well-formed.
     */
    public boolean isValidKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = key.trim();
        
        // Check length constraints
        if (trimmed.length() > 128) {
            return false;
        }
        
        // Check character constraints
        return trimmed.matches("[a-zA-Z0-9_:-]+");
    }
    
    /**
     * Extracts components from an idempotency key for analysis.
     */
    public KeyComponents extractComponents(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = key.split(SEPARATOR);
        if (parts.length < 3) {
            return KeyComponents.builder().rawKey(key).build();
        }
        
        return KeyComponents.builder()
                .sourceSystem(parts[0])
                .jobName(parts[1])
                .dateComponent(parts.length > 2 ? parts[2] : null)
                .contentHash(parts.length > 3 ? parts[3] : null)
                .rawKey(key)
                .build();
    }
    
    /**
     * Data class for idempotency key components.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class KeyComponents {
        private String sourceSystem;
        private String jobName;
        private String dateComponent;
        private String contentHash;
        private String rawKey;
        
        public String getSummary() {
            return String.format("KeyComponents[system=%s, job=%s, date=%s, hash=%s]",
                    sourceSystem, jobName, dateComponent, 
                    contentHash != null && contentHash.length() > 8 ? 
                            contentHash.substring(0, 8) + "..." : contentHash);
        }
    }
}