package com.truist.batch.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Temporary ConfigurationAudit model to resolve startup dependency issues.
 * This is a minimal implementation to allow the application to start
 * for testing the master query APIs.
 */
@Data
public class ConfigurationAudit {
    private Long id;
    private String configId;
    private String action;
    private String oldValue;
    private String newValue;
    private String changedBy;
    private String reason;
    private LocalDateTime changeDate;
    
    public ConfigurationAudit() {
        this.changeDate = LocalDateTime.now();
    }
}