package com.truist.batch.entity;

import lombok.Builder;
import lombok.Data;

/**
 * Stub entity class for SQL*Loader field configuration.
 * This would be implemented in the fabric-data-loader module.
 * For Phase 1.2 testing purposes, we provide stub implementation.
 */
@Data
@Builder(toBuilder = true)
public class SqlLoaderFieldConfigEntity {
    
    private String fieldId;
    private String fieldName;
    private String columnName;
    private String sourceField;
    private Integer fieldOrder;
    private Integer fieldPosition;
    private Integer fieldLength;
    private DataType dataType;
    private Integer maxLength;
    private String formatMask;
    private String nullable;
    private String defaultValue;
    private String encrypted;
    private String encryptionFunction;
    private String containsPii;
    
    public enum DataType {
        VARCHAR, CHAR, NUMBER, DATE, TIMESTAMP, BLOB, CLOB
    }
}