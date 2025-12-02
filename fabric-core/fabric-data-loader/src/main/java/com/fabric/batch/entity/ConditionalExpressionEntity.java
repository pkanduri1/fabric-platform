package com.fabric.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for storing conditional expressions used in field transformations
 * Supports complex business rules and conditional logic
 */
@Entity
@Table(name = "conditional_expressions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConditionalExpressionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conditional_expr_seq")
    @SequenceGenerator(name = "conditional_expr_seq", sequenceName = "conditional_expr_seq", allocationSize = 1)
    @Column(name = "expression_id")
    private Long expressionId;
    
    @Column(name = "rule_id", nullable = false)
    private String ruleId;
    
    @Column(name = "condition_type", length = 50, nullable = false)
    private String conditionType; // EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, CONTAINS, REGEX
    
    @Column(name = "field_name", length = 100)
    private String fieldName;
    
    @Column(name = "comparison_value", length = 500)
    private String comparisonValue;
    
    @Column(name = "logical_operator", length = 10)
    private String logicalOperator; // AND, OR, NOT
    
    @Column(name = "expression_order")
    private Integer expressionOrder;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate = LocalDateTime.now();
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    @Column(name = "modified_by", length = 100)
    private String modifiedBy;
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
    }
}