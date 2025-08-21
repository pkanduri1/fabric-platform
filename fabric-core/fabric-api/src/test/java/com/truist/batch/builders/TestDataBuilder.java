package com.truist.batch.builders;

import com.github.javafaker.Faker;
import com.truist.batch.dto.*;
import com.truist.batch.entity.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Test data builder for creating realistic test data using JavaFaker
 * Provides fluent API for building test entities and DTOs
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */
public class TestDataBuilder {
    
    private static final Faker faker = new Faker();
    private static final Random random = new Random();
    
    // Template Configuration Builders
    public static class TemplateConfigurationBuilder {
        private String templateId = generateTemplateId();
        private String templateName = faker.company().name() + " Template";
        private String sourceSystem = faker.options().option("HR", "MTG", "ENCORE", "SHAW");
        private String transactionCode = String.valueOf(faker.number().numberBetween(100, 999));
        private String description = faker.lorem().sentence();
        private String createdBy = faker.name().username();
        private LocalDateTime createdAt = LocalDateTime.now();
        
        public TemplateConfigurationBuilder withTemplateId(String templateId) {
            this.templateId = templateId;
            return this;
        }
        
        public TemplateConfigurationBuilder withTemplateName(String templateName) {
            this.templateName = templateName;
            return this;
        }
        
        public TemplateConfigurationBuilder withSourceSystem(String sourceSystem) {
            this.sourceSystem = sourceSystem;
            return this;
        }
        
        public TemplateConfigurationBuilder withTransactionCode(String transactionCode) {
            this.transactionCode = transactionCode;
            return this;
        }
        
        public TemplateConfigurationBuilder withDescription(String description) {
            this.description = description;
            return this;
        }
        
        public TemplateConfigurationBuilder withCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        // This method would build the actual entity when we have the complete entity class
        public Object build() {
            // Return a mock object or map for now until we have the actual entity
            return new Object() {
                public String getTemplateId() { return templateId; }
                public String getTemplateName() { return templateName; }
                public String getSourceSystem() { return sourceSystem; }
                public String getTransactionCode() { return transactionCode; }
                public String getDescription() { return description; }
                public String getCreatedBy() { return createdBy; }
                public LocalDateTime getCreatedAt() { return createdAt; }
            };
        }
    }
    
    // Field Mapping Builders
    public static class FieldMappingBuilder {
        private String fieldId = UUID.randomUUID().toString();
        private String sourceFieldName = faker.lorem().word();
        private String targetFieldName = faker.lorem().word();
        private String dataType = faker.options().option("STRING", "INTEGER", "DECIMAL", "DATE");
        private Integer length = faker.number().numberBetween(1, 255);
        private Boolean required = faker.bool().bool();
        private String defaultValue = faker.lorem().word();
        
        public FieldMappingBuilder withFieldId(String fieldId) {
            this.fieldId = fieldId;
            return this;
        }
        
        public FieldMappingBuilder withSourceFieldName(String sourceFieldName) {
            this.sourceFieldName = sourceFieldName;
            return this;
        }
        
        public FieldMappingBuilder withTargetFieldName(String targetFieldName) {
            this.targetFieldName = targetFieldName;
            return this;
        }
        
        public FieldMappingBuilder withDataType(String dataType) {
            this.dataType = dataType;
            return this;
        }
        
        public FieldMappingBuilder withLength(Integer length) {
            this.length = length;
            return this;
        }
        
        public FieldMappingBuilder withRequired(Boolean required) {
            this.required = required;
            return this;
        }
        
        public FieldMappingDto build() {
            FieldMappingDto dto = new FieldMappingDto();
            dto.setFieldId(fieldId);
            dto.setSourceFieldName(sourceFieldName);
            dto.setTargetFieldName(targetFieldName);
            dto.setDataType(dataType);
            dto.setLength(length);
            dto.setRequired(required);
            dto.setDefaultValue(defaultValue);
            return dto;
        }
    }
    
    // Manual Job Configuration Builders
    public static class ManualJobConfigBuilder {
        private String configId = generateConfigId();
        private String jobName = faker.job().title() + " Job";
        private String description = faker.lorem().sentence();
        private String templateId = generateTemplateId();
        private String masterQueryId = generateQueryId();
        private String sourceSystem = faker.options().option("HR", "MTG", "ENCORE", "SHAW");
        private String transactionCode = String.valueOf(faker.number().numberBetween(100, 999));
        private String status = faker.options().option("ACTIVE", "INACTIVE", "DRAFT");
        private String createdBy = faker.name().username();
        private LocalDateTime createdAt = LocalDateTime.now();
        
        public ManualJobConfigBuilder withConfigId(String configId) {
            this.configId = configId;
            return this;
        }
        
        public ManualJobConfigBuilder withJobName(String jobName) {
            this.jobName = jobName;
            return this;
        }
        
        public ManualJobConfigBuilder withDescription(String description) {
            this.description = description;
            return this;
        }
        
        public ManualJobConfigBuilder withTemplateId(String templateId) {
            this.templateId = templateId;
            return this;
        }
        
        public ManualJobConfigBuilder withMasterQueryId(String masterQueryId) {
            this.masterQueryId = masterQueryId;
            return this;
        }
        
        public ManualJobConfigBuilder withSourceSystem(String sourceSystem) {
            this.sourceSystem = sourceSystem;
            return this;
        }
        
        public ManualJobConfigBuilder withTransactionCode(String transactionCode) {
            this.transactionCode = transactionCode;
            return this;
        }
        
        public ManualJobConfigBuilder withStatus(String status) {
            this.status = status;
            return this;
        }
        
        public ManualJobConfigRequest buildRequest() {
            ManualJobConfigRequest request = new ManualJobConfigRequest();
            request.setJobName(jobName);
            request.setDescription(description);
            request.setTemplateId(templateId);
            request.setMasterQueryId(masterQueryId);
            request.setSourceSystem(sourceSystem);
            request.setTransactionCode(transactionCode);
            request.setStatus(status);
            return request;
        }
        
        public ManualJobConfigResponse buildResponse() {
            ManualJobConfigResponse response = new ManualJobConfigResponse();
            response.setConfigId(configId);
            response.setJobName(jobName);
            response.setDescription(description);
            response.setTemplateId(templateId);
            response.setMasterQueryId(masterQueryId);
            response.setSourceSystem(sourceSystem);
            response.setTransactionCode(transactionCode);
            response.setStatus(status);
            response.setCreatedBy(createdBy);
            response.setCreatedAt(createdAt);
            return response;
        }
    }
    
    // Source System Builders
    public static class SourceSystemBuilder {
        private String systemId = generateSystemId();
        private String systemName = faker.company().name();
        private String systemCode = faker.options().option("HR", "MTG", "ENCORE", "SHAW");
        private String description = faker.lorem().sentence();
        private String connectionString = faker.internet().url();
        private Boolean active = true;
        
        public SourceSystemBuilder withSystemId(String systemId) {
            this.systemId = systemId;
            return this;
        }
        
        public SourceSystemBuilder withSystemName(String systemName) {
            this.systemName = systemName;
            return this;
        }
        
        public SourceSystemBuilder withSystemCode(String systemCode) {
            this.systemCode = systemCode;
            return this;
        }
        
        public SourceSystemBuilder withDescription(String description) {
            this.description = description;
            return this;
        }
        
        public SourceSystemBuilder withActive(Boolean active) {
            this.active = active;
            return this;
        }
        
        public SourceSystemInfo build() {
            SourceSystemInfo info = new SourceSystemInfo();
            info.setSystemId(systemId);
            info.setSystemName(systemName);
            info.setSystemCode(systemCode);
            info.setDescription(description);
            info.setActive(active);
            return info;
        }
    }
    
    // Utility Methods
    public static String generateTemplateId() {
        return "tpl_" + faker.lorem().word() + "_" + faker.number().numberBetween(100, 999);
    }
    
    public static String generateConfigId() {
        return "cfg_" + faker.lorem().word() + "_" + faker.number().numberBetween(100, 999);
    }
    
    public static String generateQueryId() {
        return "qry_" + faker.lorem().word() + "_" + faker.number().numberBetween(100, 999);
    }
    
    public static String generateSystemId() {
        return "sys_" + faker.lorem().word() + "_" + faker.number().numberBetween(100, 999);
    }
    
    public static String generateCorrelationId() {
        return "test_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
    }
    
    public static List<FieldMappingDto> generateFieldMappings(int count) {
        List<FieldMappingDto> mappings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            mappings.add(new FieldMappingBuilder().build());
        }
        return mappings;
    }
    
    public static List<SourceSystemInfo> generateSourceSystems(int count) {
        List<SourceSystemInfo> systems = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            systems.add(new SourceSystemBuilder().build());
        }
        return systems;
    }
    
    // Factory Methods for Common Test Data
    public static TemplateConfigurationBuilder templateConfiguration() {
        return new TemplateConfigurationBuilder();
    }
    
    public static FieldMappingBuilder fieldMapping() {
        return new FieldMappingBuilder();
    }
    
    public static ManualJobConfigBuilder manualJobConfig() {
        return new ManualJobConfigBuilder();
    }
    
    public static SourceSystemBuilder sourceSystem() {
        return new SourceSystemBuilder();
    }
}