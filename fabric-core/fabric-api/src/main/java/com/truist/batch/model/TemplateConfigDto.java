package com.truist.batch.model;

import java.util.List;

/**
 * DTO for saving the complete template configuration from Template Studio.
 * Includes Job Configuration, Field Mappings, and Master Query details.
 */
public class TemplateConfigDto {
    private String sourceSystem;
    private String jobName;
    private String fileType;
    private String transactionType;
    private String description;
    private String createdBy;
    private List<FieldTemplate> fields;
    private String masterQuery;

    public static class MasterQueryConfig {
        private String querySql;
        private String queryName;
        private String queryDescription;

        public String getQuerySql() {
            return querySql;
        }

        public void setQuerySql(String querySql) {
            this.querySql = querySql;
        }

        public String getQueryName() {
            return queryName;
        }

        public void setQueryName(String queryName) {
            this.queryName = queryName;
        }

        public String getQueryDescription() {
            return queryDescription;
        }

        public void setQueryDescription(String queryDescription) {
            this.queryDescription = queryDescription;
        }
    }

    // Getters and Setters
    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public List<FieldTemplate> getFields() {
        return fields;
    }

    public void setFields(List<FieldTemplate> fields) {
        this.fields = fields;
    }

    public String getMasterQuery() {
        return masterQuery;
    }

    public void setMasterQuery(String masterQuery) {
        this.masterQuery = masterQuery;
    }
}
