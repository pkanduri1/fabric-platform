package com.truist.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * POJO for data load configuration to satisfy compilation requirements.
 * This is a temporary implementation until the fabric-data-loader dependency issue is resolved.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataLoadConfigEntity {

    private String id;
    
    private String configurationId;
    private String name;
    private String description;
    private String sourceSystem;
    private String targetTable;
    private boolean enabled;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime modifiedDate;
    private String modifiedBy;
}