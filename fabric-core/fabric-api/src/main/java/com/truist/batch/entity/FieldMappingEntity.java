package com.truist.batch.entity;

/**
 * Entity for field mapping configurations
 * Stub implementation to satisfy dependency injection
 */
public class FieldMappingEntity {
    
    private String id;
    private String sourceField;
    private String targetField;
    private String fieldType;
    private String mappingRule;
    private String transformation;
    private String isRequired;
    private String defaultValue;
    private String createdBy;
    private String createdDate;
    
    // Default constructor
    public FieldMappingEntity() {}
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSourceField() { return sourceField; }
    public void setSourceField(String sourceField) { this.sourceField = sourceField; }
    
    public String getTargetField() { return targetField; }
    public void setTargetField(String targetField) { this.targetField = targetField; }
    
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    
    public String getMappingRule() { return mappingRule; }
    public void setMappingRule(String mappingRule) { this.mappingRule = mappingRule; }
    
    public String getTransformation() { return transformation; }
    public void setTransformation(String transformation) { this.transformation = transformation; }
    
    public String getIsRequired() { return isRequired; }
    public void setIsRequired(String isRequired) { this.isRequired = isRequired; }
    
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
}