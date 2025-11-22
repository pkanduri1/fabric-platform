package com.truist.batch.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity representing batch configuration stored in BATCH_CONFIGURATIONS table.
 * This table is used by the batch processing module to read field mapping configurations.
 *
 * @author Claude Code
 * @since Phase 2 - Batch Module Separation
 */
@Data
public class BatchConfigurationEntity {

    private String id;
    private String sourceSystem;
    private String jobName;
    private String transactionType;
    private String description;
    private String configurationJson;  // CLOB containing field mappings JSON
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
    private Integer version;
    private String enabled;  // 'Y' or 'N'

    /**
     * Check if this configuration is enabled
     */
    public boolean isEnabled() {
        return "Y".equalsIgnoreCase(enabled);
    }
}
