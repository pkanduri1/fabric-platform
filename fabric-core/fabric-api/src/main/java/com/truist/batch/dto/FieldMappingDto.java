package com.truist.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for individual field mappings in template-source mapping requests
 * Used in US002 Template Configuration Enhancement
 */
public class FieldMappingDto {
    
    @NotBlank(message = "Target field name is required")
    @Size(max = 50, message = "Target field name must not exceed 50 characters")
    private String targetFieldName;

    @Size(max = 100, message = "Source field name must not exceed 100 characters")
    private String sourceFieldName;

    @Size(max = 20, message = "Transformation type must not exceed 20 characters")
    private String transformationType = "source";

    @Size(max = 1000, message = "Transformation config must not exceed 1000 characters")
    private String transformationConfig;

    private Integer targetPosition;
    private Integer length;

    @Size(max = 20, message = "Data type must not exceed 20 characters")
    private String dataType;

    // Constructors
    public FieldMappingDto() {}

    public FieldMappingDto(String targetFieldName, String sourceFieldName, String transformationType) {
        this.targetFieldName = targetFieldName;
        this.sourceFieldName = sourceFieldName;
        this.transformationType = transformationType;
    }

    public FieldMappingDto(String targetFieldName, String sourceFieldName, String transformationType,
                          Integer targetPosition, Integer length, String dataType) {
        this.targetFieldName = targetFieldName;
        this.sourceFieldName = sourceFieldName;
        this.transformationType = transformationType;
        this.targetPosition = targetPosition;
        this.length = length;
        this.dataType = dataType;
    }

    // Getters and Setters
    public String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldName(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public String getSourceFieldName() {
        return sourceFieldName;
    }

    public void setSourceFieldName(String sourceFieldName) {
        this.sourceFieldName = sourceFieldName;
    }

    public String getTransformationType() {
        return transformationType;
    }

    public void setTransformationType(String transformationType) {
        this.transformationType = transformationType;
    }

    public String getTransformationConfig() {
        return transformationConfig;
    }

    public void setTransformationConfig(String transformationConfig) {
        this.transformationConfig = transformationConfig;
    }

    public Integer getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(Integer targetPosition) {
        this.targetPosition = targetPosition;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        return "FieldMappingDto{" +
                "targetFieldName='" + targetFieldName + '\'' +
                ", sourceFieldName='" + sourceFieldName + '\'' +
                ", transformationType='" + transformationType + '\'' +
                ", targetPosition=" + targetPosition +
                ", length=" + length +
                ", dataType='" + dataType + '\'' +
                '}';
    }
}