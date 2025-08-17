package com.truist.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * POJO for validation rule to satisfy compilation requirements.
 * This is a temporary implementation until the fabric-data-loader dependency issue is resolved.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRuleEntity {

    private String id;
    
    private String ruleName;
    private String description;
    private String fieldName;
    
    private RuleType ruleType;
    
    private String ruleExpression;
    private String errorMessage;
    private boolean enabled;
    private Integer priority;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime modifiedDate;
    private String modifiedBy;
    
    public enum RuleType {
        REQUIRED,
        FORMAT,
        LENGTH,
        RANGE,
        CUSTOM
    }
}