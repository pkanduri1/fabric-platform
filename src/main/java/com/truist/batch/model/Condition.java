package com.truist.batch.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class representing a condition with expressions for if, then, else, and optional else-if branches.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Condition {

    private String ifExpr;
    private String then;
    private String elseExpr;
    private List<Condition> elseIfExprs;

}
