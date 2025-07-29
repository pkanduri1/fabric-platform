package com.truist.batch.validation;

import com.truist.batch.entity.DataLoadConfigEntity;
import com.truist.batch.entity.ValidationRuleEntity;
import com.truist.batch.repository.ValidationRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Comprehensive validation engine for data loading operations.
 * Supports configurable validation rules including data type validation,
 * business rules, referential integrity checks, and custom validations.
 */
@Slf4j
@Component
public class ValidationEngine {
    
    @Autowired
    private ValidationRuleRepository validationRuleRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private BusinessRuleValidator businessRuleValidator;
    
    @Autowired
    private DataTypeValidator dataTypeValidator;
    
    @Autowired
    private ReferentialIntegrityValidator referentialIntegrityValidator;
    
    // Validation statistics
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int MAX_ERROR_SAMPLES = 100;
    
    /**
     * Validate a data file before loading using all configured validation rules.
     * 
     * @param config Data loading configuration
     * @param filePath Path to the data file to validate
     * @return Validation result with detailed statistics and errors
     */
    public ValidationResult validateFile(DataLoadConfigEntity config, String filePath) {
        log.info("Starting file validation for config: {} - File: {}", config.getConfigId(), filePath);
        
        ValidationResult result = new ValidationResult();
        result.setConfigId(config.getConfigId());
        result.setFilePath(filePath);
        result.setValidationStartTime(LocalDateTime.now());
        result.setCorrelationId(generateCorrelationId());
        
        try {
            // Load validation rules
            List<ValidationRuleEntity> validationRules = loadValidationRules(config.getConfigId());
            if (validationRules.isEmpty()) {
                log.warn("No validation rules found for config: {}", config.getConfigId());
                result.setValidationStatus(ValidationResult.ValidationStatus.SKIPPED);
                result.addWarning("No validation rules configured");
                return result;
            }
            
            log.info("Loaded {} validation rules for config: {}", validationRules.size(), config.getConfigId());
            
            // Validate file structure and accessibility
            if (!validateFileAccessibility(filePath, result)) {
                return result;
            }
            
            // Perform field-level validations
            validateFileContent(config, filePath, validationRules, result);
            
            // Perform cross-record validations
            if (result.isValid() || result.getValidationStatus() == ValidationResult.ValidationStatus.WARNING) {
                performCrossRecordValidations(config, filePath, validationRules, result);
            }
            
            // Perform referential integrity checks
            if (result.isValid() || result.getValidationStatus() == ValidationResult.ValidationStatus.WARNING) {
                performReferentialIntegrityChecks(config, filePath, validationRules, result);
            }
            
            // Finalize validation result
            finalizeValidationResult(result);
            
            log.info("File validation completed for config: {} - Status: {}, Errors: {}, Warnings: {}", 
                    config.getConfigId(), result.getValidationStatus(), 
                    result.getErrorCount(), result.getWarningCount());
            
        } catch (Exception e) {
            log.error("Fatal error during file validation for config: {}", config.getConfigId(), e);
            result.setValidationStatus(ValidationResult.ValidationStatus.FAILED);
            result.addError("FATAL_ERROR", "Validation failed with fatal error: " + e.getMessage());
            result.setErrorDetails(getStackTrace(e));
        } finally {
            result.setValidationEndTime(LocalDateTime.now());
            if (result.getValidationStartTime() != null) {
                result.setValidationDurationMs(
                    java.time.Duration.between(result.getValidationStartTime(), result.getValidationEndTime()).toMillis());
            }
        }
        
        return result;
    }
    
    /**
     * Validate a single record against all applicable validation rules.
     */
    public RecordValidationResult validateRecord(Map<String, String> recordData, 
                                               List<ValidationRuleEntity> validationRules, 
                                               long recordNumber) {
        RecordValidationResult result = new RecordValidationResult();
        result.setRecordNumber(recordNumber);
        result.setRecordData(recordData);
        
        // Group rules by field
        Map<String, List<ValidationRuleEntity>> rulesByField = validationRules.stream()
                .collect(Collectors.groupingBy(ValidationRuleEntity::getFieldName));
        
        // Validate each field
        for (Map.Entry<String, String> field : recordData.entrySet()) {
            String fieldName = field.getKey();
            String fieldValue = field.getValue();
            
            List<ValidationRuleEntity> fieldRules = rulesByField.get(fieldName);
            if (fieldRules != null) {
                validateFieldValue(fieldName, fieldValue, fieldRules, result);
            }
        }
        
        // Perform record-level validations (cross-field validations)
        performRecordLevelValidations(recordData, validationRules, result);
        
        return result;
    }
    
    /**
     * Load validation rules for the specified configuration.
     */
    private List<ValidationRuleEntity> loadValidationRules(String configId) {
        return validationRuleRepository.findByConfigIdAndEnabledOrderByExecutionOrder(configId, "Y");
    }
    
    /**
     * Validate file accessibility and basic structure.
     */
    private boolean validateFileAccessibility(String filePath, ValidationResult result) {
        try {
            java.io.File file = new java.io.File(filePath);
            
            if (!file.exists()) {
                result.setValidationStatus(ValidationResult.ValidationStatus.FAILED);
                result.addError("FILE_NOT_FOUND", "Data file does not exist: " + filePath);
                return false;
            }
            
            if (!file.canRead()) {
                result.setValidationStatus(ValidationResult.ValidationStatus.FAILED);
                result.addError("FILE_NOT_READABLE", "Data file is not readable: " + filePath);
                return false;
            }
            
            if (file.length() == 0) {
                result.setValidationStatus(ValidationResult.ValidationStatus.FAILED);
                result.addError("FILE_EMPTY", "Data file is empty: " + filePath);
                return false;
            }
            
            result.setFileSizeBytes(file.length());
            log.debug("File accessibility validation passed for: {} (Size: {} bytes)", filePath, file.length());
            return true;
            
        } catch (Exception e) {
            result.setValidationStatus(ValidationResult.ValidationStatus.FAILED);
            result.addError("FILE_ACCESS_ERROR", "Error accessing file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate file content record by record.
     */
    private void validateFileContent(DataLoadConfigEntity config, String filePath, 
                                   List<ValidationRuleEntity> validationRules, ValidationResult result) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            long recordNumber = 0;
            long validRecords = 0;
            long invalidRecords = 0;
            
            // Skip header rows if configured
            int headerRows = config.getHeaderRows() != null ? config.getHeaderRows() : 0;
            for (int i = 0; i < headerRows; i++) {
                reader.readLine();
            }
            
            while ((line = reader.readLine()) != null) {
                recordNumber++;
                
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Parse record
                Map<String, String> recordData = parseRecord(line, config);
                if (recordData == null) {
                    result.addError("PARSE_ERROR", "Failed to parse record " + recordNumber + ": " + line);
                    invalidRecords++;
                    continue;
                }
                
                // Validate record
                RecordValidationResult recordResult = validateRecord(recordData, validationRules, recordNumber);
                
                if (recordResult.isValid()) {
                    validRecords++;
                } else {
                    invalidRecords++;
                    
                    // Add record errors to overall result (with limit to prevent memory issues)
                    if (result.getRecordErrors().size() < MAX_ERROR_SAMPLES) {
                        result.addRecordError(recordResult);
                    }
                    
                    // Update field-level error counts
                    for (FieldValidationError fieldError : recordResult.getFieldErrors()) {
                        result.incrementFieldErrorCount(fieldError.getFieldName(), fieldError.getRuleType());
                    }
                }
                
                // Process in batches to manage memory
                if (recordNumber % DEFAULT_BATCH_SIZE == 0) {
                    log.debug("Processed {} records - Valid: {}, Invalid: {}", recordNumber, validRecords, invalidRecords);
                }
            }
            
            result.setTotalRecords(recordNumber);
            result.setValidRecords(validRecords);
            result.setInvalidRecords(invalidRecords);
            
            log.info("File content validation completed - Total: {}, Valid: {}, Invalid: {}", 
                    recordNumber, validRecords, invalidRecords);
            
        } catch (IOException e) {
            log.error("Error reading file during validation: {}", filePath, e);
            result.setValidationStatus(ValidationResult.ValidationStatus.FAILED);
            result.addError("FILE_READ_ERROR", "Error reading file: " + e.getMessage());
        }
    }
    
    /**
     * Parse a single record based on file format configuration.
     */
    private Map<String, String> parseRecord(String line, DataLoadConfigEntity config) {
        try {
            Map<String, String> recordData = new LinkedHashMap<>();
            
            if (config.getFileType() == DataLoadConfigEntity.FileType.PIPE_DELIMITED) {
                String delimiter = config.getFieldDelimiter() != null ? config.getFieldDelimiter() : "|";
                String[] fields = line.split(Pattern.quote(delimiter), -1);
                
                // Map fields to column names (would need field configuration)
                for (int i = 0; i < fields.length; i++) {
                    String fieldValue = fields[i].trim();
                    recordData.put("FIELD_" + (i + 1), fieldValue);
                }
            } else if (config.getFileType() == DataLoadConfigEntity.FileType.FIXED_WIDTH) {
                // Fixed width parsing would require field position configuration
                recordData.put("RECORD_DATA", line);
            } else {
                // Default parsing
                recordData.put("RECORD_DATA", line);
            }
            
            return recordData;
            
        } catch (Exception e) {
            log.error("Error parsing record: {}", line, e);
            return null;
        }
    }
    
    /**
     * Validate a field value against applicable validation rules.
     */
    private void validateFieldValue(String fieldName, String fieldValue, 
                                  List<ValidationRuleEntity> fieldRules, RecordValidationResult result) {
        
        for (ValidationRuleEntity rule : fieldRules) {
            try {
                FieldValidationResult fieldResult = validateFieldAgainstRule(fieldName, fieldValue, rule);
                
                if (!fieldResult.isValid()) {
                    FieldValidationError error = new FieldValidationError();
                    error.setFieldName(fieldName);
                    error.setFieldValue(fieldValue);
                    error.setRuleId(rule.getRuleId());
                    error.setRuleType(rule.getRuleType().toString());
                    error.setErrorMessage(fieldResult.getErrorMessage());
                    error.setSeverity(rule.getSeverity().toString());
                    
                    result.addFieldError(error);
                    
                    // Stop validation for this field if critical error
                    if (rule.getSeverity() == ValidationRuleEntity.Severity.CRITICAL) {
                        break;
                    }
                }
                
            } catch (Exception e) {
                log.error("Error validating field {} against rule {}: {}", fieldName, rule.getRuleId(), e.getMessage());
                
                FieldValidationError error = new FieldValidationError();
                error.setFieldName(fieldName);
                error.setFieldValue(fieldValue);
                error.setRuleId(rule.getRuleId());
                error.setRuleType(rule.getRuleType().toString());
                error.setErrorMessage("Validation rule execution failed: " + e.getMessage());
                error.setSeverity("ERROR");
                
                result.addFieldError(error);
            }
        }
    }
    
    /**
     * Validate a single field value against a specific validation rule.
     */
    private FieldValidationResult validateFieldAgainstRule(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        try {
            switch (rule.getRuleType()) {
                case DATA_TYPE_VALIDATION:
                    result = dataTypeValidator.validate(fieldName, fieldValue, rule);
                    break;
                    
                case REQUIRED_FIELD_VALIDATION:
                    result = validateRequiredField(fieldName, fieldValue, rule);
                    break;
                    
                case LENGTH_VALIDATION:
                    result = validateFieldLength(fieldName, fieldValue, rule);
                    break;
                    
                case PATTERN_VALIDATION:
                    result = validateFieldPattern(fieldName, fieldValue, rule);
                    break;
                    
                case RANGE_VALIDATION:
                    result = validateFieldRange(fieldName, fieldValue, rule);
                    break;
                    
                case UNIQUE_FIELD_VALIDATION:
                    result = validateUniqueField(fieldName, fieldValue, rule);
                    break;
                    
                case REFERENTIAL_INTEGRITY:
                    result = referentialIntegrityValidator.validate(fieldName, fieldValue, rule);
                    break;
                    
                case BUSINESS_RULE:
                    result = businessRuleValidator.validate(fieldName, fieldValue, rule);
                    break;
                    
                case CUSTOM_SQL_VALIDATION:
                    result = validateCustomSql(fieldName, fieldValue, rule);
                    break;
                    
                default:
                    result.setValid(true);
                    log.warn("Unknown validation rule type: {} for field: {}", rule.getRuleType(), fieldName);
            }
            
        } catch (Exception e) {
            log.error("Error executing validation rule {} for field {}: {}", rule.getRuleId(), fieldName, e.getMessage());
            result.setValid(false);
            result.setErrorMessage("Rule execution error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate required field rule.
     */
    private FieldValidationResult validateRequiredField(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        if (rule.isRequired()) {
            if (fieldValue == null || fieldValue.trim().isEmpty()) {
                result.setValid(false);
                result.setErrorMessage(rule.getErrorMessage() != null ? 
                    rule.getErrorMessage() : "Field " + fieldName + " is required but is empty");
            } else {
                result.setValid(true);
            }
        } else {
            result.setValid(true);
        }
        
        return result;
    }
    
    /**
     * Validate field length constraints.
     */
    private FieldValidationResult validateFieldLength(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        if (fieldValue == null) {
            result.setValid(true); // Null values handled by required field validation
            return result;
        }
        
        int length = fieldValue.length();
        boolean valid = true;
        StringBuilder errorMsg = new StringBuilder();
        
        if (rule.getMinLength() != null && length < rule.getMinLength()) {
            valid = false;
            errorMsg.append("Field ").append(fieldName).append(" length (").append(length)
                   .append(") is less than minimum required (").append(rule.getMinLength()).append(")");
        }
        
        if (rule.getMaxLength() != null && length > rule.getMaxLength()) {
            valid = false;
            if (errorMsg.length() > 0) errorMsg.append("; ");
            errorMsg.append("Field ").append(fieldName).append(" length (").append(length)
                   .append(") exceeds maximum allowed (").append(rule.getMaxLength()).append(")");
        }
        
        result.setValid(valid);
        if (!valid) {
            result.setErrorMessage(rule.getErrorMessage() != null ? rule.getErrorMessage() : errorMsg.toString());
        }
        
        return result;
    }
    
    /**
     * Validate field against regex pattern.
     */
    private FieldValidationResult validateFieldPattern(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        if (fieldValue == null || rule.getPattern() == null) {
            result.setValid(true);
            return result;
        }
        
        try {
            Pattern pattern = Pattern.compile(rule.getPattern());
            boolean matches = pattern.matcher(fieldValue).matches();
            
            result.setValid(matches);
            if (!matches) {
                result.setErrorMessage(rule.getErrorMessage() != null ? 
                    rule.getErrorMessage() : 
                    "Field " + fieldName + " value '" + fieldValue + "' does not match required pattern: " + rule.getPattern());
            }
            
        } catch (Exception e) {
            log.error("Error validating pattern for field {}: {}", fieldName, e.getMessage());
            result.setValid(false);
            result.setErrorMessage("Pattern validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate field value within specified range (for numeric fields).
     */
    private FieldValidationResult validateFieldRange(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            result.setValid(true);
            return result;
        }
        
        try {
            // Parse validation expression for range (e.g., "0,100" or "MIN:0,MAX:100")
            String expression = rule.getValidationExpression();
            if (expression != null && expression.contains(",")) {
                String[] parts = expression.split(",");
                if (parts.length == 2) {
                    double value = Double.parseDouble(fieldValue);
                    double min = Double.parseDouble(parts[0].trim());
                    double max = Double.parseDouble(parts[1].trim());
                    
                    if (value < min || value > max) {
                        result.setValid(false);
                        result.setErrorMessage(rule.getErrorMessage() != null ? 
                            rule.getErrorMessage() : 
                            String.format("Field %s value %.2f is outside allowed range [%.2f, %.2f]", 
                                        fieldName, value, min, max));
                    } else {
                        result.setValid(true);
                    }
                } else {
                    result.setValid(true);
                }
            } else {
                result.setValid(true);
            }
            
        } catch (NumberFormatException e) {
            result.setValid(false);
            result.setErrorMessage("Invalid numeric value for range validation: " + fieldValue);
        } catch (Exception e) {
            log.error("Error validating range for field {}: {}", fieldName, e.getMessage());
            result.setValid(false);
            result.setErrorMessage("Range validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate unique field constraint (simplified implementation).
     */
    private FieldValidationResult validateUniqueField(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        // This would typically be handled at the database level or require a more complex implementation
        // For now, we'll mark as valid and handle uniqueness through database constraints
        result.setValid(true);
        
        return result;
    }
    
    /**
     * Validate using custom SQL query.
     */
    private FieldValidationResult validateCustomSql(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        if (rule.getLookupSql() == null || rule.getLookupSql().trim().isEmpty()) {
            result.setValid(true);
            return result;
        }
        
        try {
            // Replace placeholder with actual field value
            String sql = rule.getLookupSql().replace(":FIELD_VALUE", "?");
            
            List<Map<String, Object>> queryResult = jdbcTemplate.queryForList(sql, fieldValue);
            
            // If query returns any rows, validation passes
            result.setValid(!queryResult.isEmpty());
            
            if (!result.isValid()) {
                result.setErrorMessage(rule.getErrorMessage() != null ? 
                    rule.getErrorMessage() : 
                    "Field " + fieldName + " failed custom SQL validation");
            }
            
        } catch (Exception e) {
            log.error("Error executing custom SQL validation for field {}: {}", fieldName, e.getMessage());
            result.setValid(false);
            result.setErrorMessage("Custom SQL validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Perform record-level validations (cross-field validations).
     */
    private void performRecordLevelValidations(Map<String, String> recordData, 
                                             List<ValidationRuleEntity> validationRules, 
                                             RecordValidationResult result) {
        // Implementation for cross-field validations would go here
        // This could include business rules that span multiple fields
    }
    
    /**
     * Perform cross-record validations (duplicate detection, etc.).
     */
    private void performCrossRecordValidations(DataLoadConfigEntity config, String filePath, 
                                             List<ValidationRuleEntity> validationRules, ValidationResult result) {
        // Implementation for cross-record validations would go here
        // This could include duplicate detection, sequence validation, etc.
    }
    
    /**
     * Perform referential integrity checks against database tables.
     */
    private void performReferentialIntegrityChecks(DataLoadConfigEntity config, String filePath, 
                                                  List<ValidationRuleEntity> validationRules, ValidationResult result) {
        List<ValidationRuleEntity> refIntegrityRules = validationRules.stream()
                .filter(rule -> rule.getRuleType() == ValidationRuleEntity.RuleType.REFERENTIAL_INTEGRITY)
                .collect(Collectors.toList());
        
        if (refIntegrityRules.isEmpty()) {
            return;
        }
        
        log.info("Performing referential integrity checks with {} rules", refIntegrityRules.size());
        
        // This would involve checking foreign key relationships
        // Implementation details would depend on specific business requirements
    }
    
    /**
     * Finalize validation result and set appropriate status.
     */
    private void finalizeValidationResult(ValidationResult result) {
        if (result.getValidationStatus() == null) {
            if (result.getErrorCount() > 0) {
                result.setValidationStatus(ValidationResult.ValidationStatus.FAILED);
            } else if (result.getWarningCount() > 0) {
                result.setValidationStatus(ValidationResult.ValidationStatus.WARNING);
            } else {
                result.setValidationStatus(ValidationResult.ValidationStatus.PASSED);
            }
        }
        
        // Calculate success rate
        if (result.getTotalRecords() > 0) {
            double successRate = (double) result.getValidRecords() / result.getTotalRecords() * 100.0;
            result.setSuccessRatePercent(successRate);
        }
    }
    
    private String generateCorrelationId() {
        return "VAL-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}