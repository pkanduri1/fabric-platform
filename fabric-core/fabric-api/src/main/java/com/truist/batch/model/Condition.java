package com.truist.batch.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a conditional logic configuration for field transformations.
 * 
 * This model supports if-then-else logic with multiple else-if conditions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Condition {
    
    /**
     * The if condition expression.
     * Example: "STATUS = 'A'" or "AMOUNT >= 100"
     */
    @JsonProperty("ifExpr")
    private String ifExpr;
    
    /**
     * The value to return if the if condition is true.
     * Can be a literal value or a field reference.
     */
    @JsonProperty("then")
    private String then;
    
    /**
     * The value to return if all conditions are false.
     * Can be a literal value or a field reference.
     */
    @JsonProperty("elseExpr")
    private String elseExpr;
    
    /**
     * List of else-if conditions for complex conditional logic.
     */
    @JsonProperty("elseIfExprs")
    private List<Condition> elseIfExprs;
}