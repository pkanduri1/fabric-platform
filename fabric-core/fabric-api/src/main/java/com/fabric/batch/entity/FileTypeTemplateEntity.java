package com.fabric.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * POJO for file type template to satisfy compilation requirements.
 * This is a temporary implementation until the fabric-data-loader dependency issue is resolved.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTypeTemplateEntity {

    private String fileType;
    
    private String description;
    private Integer totalFields;
    private Integer recordLength;
    private String enabled;
    private Integer version;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime modifiedDate;
    private String modifiedBy;
}