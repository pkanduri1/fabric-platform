package com.truist.batch.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/**
 * Service for importing and managing field mappings from Excel files.
 * Simplified version for compilation - to be enhanced in future iterations.
 */
public interface ExcelMappingImportService {
    
    /**
     * Import field mappings from Excel file
     */
    ImportResult importMappingsFromExcel(MultipartFile file, String fileType, String createdBy);
    
    /**
     * Preview mappings from Excel file without importing
     */
    MappingPreview previewMappingsFromExcel(MultipartFile file, String fileType);
    
    /**
     * Validate Excel file structure
     */
    boolean validateExcelStructure(MultipartFile file);
    
    /**
     * Get supported Excel templates
     */
    List<String> getSupportedTemplates();
}

/**
 * Simple import result model
 */
class ImportResult {
    private boolean success;
    private String message;
    private int importedCount;
    
    public ImportResult(boolean success, String message, int importedCount) {
        this.success = success;
        this.message = message;
        this.importedCount = importedCount;
    }
    
    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }
}

/**
 * Simple mapping preview model
 */
class MappingPreview {
    private List<String> fieldNames;
    private int fieldCount;
    private boolean valid;
    
    public MappingPreview(List<String> fieldNames, int fieldCount, boolean valid) {
        this.fieldNames = fieldNames;
        this.fieldCount = fieldCount;
        this.valid = valid;
    }
    
    // Getters and setters
    public List<String> getFieldNames() { return fieldNames; }
    public void setFieldNames(List<String> fieldNames) { this.fieldNames = fieldNames; }
    public int getFieldCount() { return fieldCount; }
    public void setFieldCount(int fieldCount) { this.fieldCount = fieldCount; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
}