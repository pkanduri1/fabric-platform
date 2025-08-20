package com.truist.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for source system responses used by the Configuration API endpoint
 * This matches the frontend TypeScript interface for SourceSystem
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigSourceSystemResponse {
    private String id;
    private String name;
    private String description;
    private String systemType; // Frontend expects 'systemType' not 'type'
    private String type; // Also include 'type' for backwards compatibility
    private List<ConfigJobResponse> jobs; // Empty list for now
    private Integer jobCount;
    private Boolean enabled;
    private String lastModified; // Frontend expects string format
    private Map<String, String> connectionProperties;
    private String inputBasePath;
    private String outputBasePath;
    private List<String> supportedFileTypes; // For frontend compatibility
    private List<String> supportedTransactionTypes; // For frontend compatibility
    
    /**
     * Nested class for job responses
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigJobResponse {
        private String name;
        private String sourceSystem;
        private String jobName;
        private String description;
        private List<String> files;
        private Boolean multiTxn;
        private List<String> supportedTransactionTypes;
        private String defaultFileType;
    }
}