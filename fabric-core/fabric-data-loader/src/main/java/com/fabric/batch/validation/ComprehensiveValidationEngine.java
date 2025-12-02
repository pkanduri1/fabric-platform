package com.fabric.batch.validation;

import com.fabric.batch.entity.ValidationRuleEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Comprehensive validation engine implementing user requirements:
 * 1. Field length validation
 * 2. Required field validation  
 * 3. Data type validation
 * Supports configurable error thresholds and detailed audit logging.
 */
@Slf4j
@Component
public class ComprehensiveValidationEngine {
    
    @Autowired
    private ReferentialIntegrityValidator referentialIntegrityValidator;
    
    // Common regex patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$");
    
    private static final Pattern SSN_PATTERN = Pattern.compile(
        "^\\d{3}-?\\d{2}-?\\d{4}$");
    
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
        "^-?\\d+(\\.\\d+)?$");
    
    private static final Pattern INTEGER_PATTERN = Pattern.compile(
        "^-?\\d+$");
    
    /**
     * Validate a single field against all applicable rules.
     */
    public List<FieldValidationResult> validateField(String fieldName, String fieldValue, 
                                                   List<ValidationRuleEntity> rules) {
        List<FieldValidationResult> results = new ArrayList<>();
        
        if (rules == null || rules.isEmpty()) {
            results.add(FieldValidationResult.success(fieldName, fieldValue));
            return results;
        }
        
        log.debug("Validating field '{}' with value '{}' against {} rules", 
                 fieldName, fieldValue, rules.size());
        
        // Sort rules by execution order
        rules.sort((r1, r2) -> Integer.compare(r1.getExecutionOrder(), r2.getExecutionOrder()));
        
        for (ValidationRuleEntity rule : rules) {
            if (!"Y".equals(rule.getEnabled())) {
                continue;
            }
            
            FieldValidationResult result = validateFieldAgainstRule(fieldName, fieldValue, rule);
            results.add(result);
            
            // Stop on first critical error if configured
            if (!result.isValid() && "CRITICAL".equals(rule.getSeverity())) {
                log.error("Critical validation error for field {}: {}", fieldName, result.getErrorMessage());
                break;
            }
        }
        
        return results;
    }
    
    /**
     * Validate a field against a specific rule.
     */
    public FieldValidationResult validateFieldAgainstRule(String fieldName, String fieldValue, 
                                                         ValidationRuleEntity rule) {
        try {
            FieldValidationResult result = new FieldValidationResult();
            result.setFieldName(fieldName);
            result.setFieldValue(fieldValue);
            result.setRuleId(rule.getRuleId());
            result.setValidationType(rule.getRuleType().toString());
            result.setSeverity(rule.getSeverity());
            
            switch (rule.getRuleType()) {
                case REQUIRED_FIELD_VALIDATION:
                    return validateRequiredField(fieldName, fieldValue, rule);
                    
                case LENGTH_VALIDATION:
                    return validateFieldLength(fieldName, fieldValue, rule);
                    
                case DATA_TYPE_VALIDATION:
                    return validateDataType(fieldName, fieldValue, rule);
                    
                case PATTERN_VALIDATION:
                    return validatePattern(fieldName, fieldValue, rule);
                    
                case EMAIL_VALIDATION:
                    return validateEmail(fieldName, fieldValue, rule);
                    
                case PHONE_VALIDATION:
                    return validatePhone(fieldName, fieldValue, rule);
                    
                case SSN_VALIDATION:
                    return validateSSN(fieldName, fieldValue, rule);
                    
                case NUMERIC_VALIDATION:
                    return validateNumeric(fieldName, fieldValue, rule);
                    
                case DATE_FORMAT_VALIDATION:
                    return validateDateFormat(fieldName, fieldValue, rule);
                    
                case RANGE_VALIDATION:
                    return validateRange(fieldName, fieldValue, rule);
                    
                case REFERENTIAL_INTEGRITY:
                    return referentialIntegrityValidator.validate(fieldName, fieldValue, rule);
                    
                case UNIQUE_FIELD_VALIDATION:
                    return validateUniqueField(fieldName, fieldValue, rule);
                    
                case ACCOUNT_NUMBER_VALIDATION:
                    return validateAccountNumber(fieldName, fieldValue, rule);
                    
                case CUSTOM_SQL_VALIDATION:
                    return validateCustomSQL(fieldName, fieldValue, rule);
                    
                default:
                    log.warn("Unsupported validation rule type: {}", rule.getRuleType());
                    result.setValid(true);
                    result.setWarningMessage("Unsupported validation rule type: " + rule.getRuleType());
                    return result;
            }
            
        } catch (Exception e) {
            log.error("Error validating field {} against rule {}: {}", 
                     fieldName, rule.getRuleId(), e.getMessage());
            
            FieldValidationResult errorResult = new FieldValidationResult();
            errorResult.setFieldName(fieldName);
            errorResult.setFieldValue(fieldValue);
            errorResult.setRuleId(rule.getRuleId());
            errorResult.setValid(false);
            errorResult.setErrorMessage("Validation error: " + e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * Validate required field - implements user requirement "Required field validation".
     */
    private FieldValidationResult validateRequiredField(String fieldName, String fieldValue, 
                                                       ValidationRuleEntity rule) {
        boolean isRequired = "Y".equals(rule.getRequiredField());
        boolean isEmpty = fieldValue == null || fieldValue.trim().isEmpty();
        
        if (isRequired && isEmpty) {
            return FieldValidationResult.failure(fieldName, fieldValue, 
                getErrorMessage(rule, fieldName + " is required"));
        }
        
        return FieldValidationResult.success(fieldName, fieldValue);
    }
    
    /**
     * Validate field length - implements user requirement "Field length validation".
     */
    private FieldValidationResult validateFieldLength(String fieldName, String fieldValue, 
                                                     ValidationRuleEntity rule) {
        if (fieldValue == null) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        int length = fieldValue.length();
        Integer maxLength = rule.getMaxLength();
        Integer minLength = rule.getMinLength();
        
        if (maxLength != null && length > maxLength) {
            return FieldValidationResult.failure(fieldName, fieldValue,
                getErrorMessage(rule, String.format("%s exceeds maximum length of %d characters (actual: %d)",
                    fieldName, maxLength, length)));
        }
        
        if (minLength != null && length < minLength) {
            return FieldValidationResult.failure(fieldName, fieldValue,
                getErrorMessage(rule, String.format("%s is below minimum length of %d characters (actual: %d)",
                    fieldName, minLength, length)));
        }
        
        return FieldValidationResult.success(fieldName, fieldValue);
    }
    
    /**
     * Validate data type - implements user requirement "Data type validation".
     */
    private FieldValidationResult validateDataType(String fieldName, String fieldValue, 
                                                  ValidationRuleEntity rule) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        String dataType = rule.getDataType();
        if (dataType == null) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        try {
            switch (dataType.toUpperCase()) {
                case "INTEGER":
                case "INT":
                    Integer.parseInt(fieldValue.trim());
                    break;
                    
                case "LONG":
                    Long.parseLong(fieldValue.trim());
                    break;
                    
                case "DECIMAL":
                case "DOUBLE":
                case "FLOAT":
                    new BigDecimal(fieldValue.trim());
                    break;
                    
                case "DATE":
                    // Try common date formats
                    try {
                        LocalDate.parse(fieldValue.trim());
                    } catch (DateTimeParseException e1) {
                        try {
                            LocalDate.parse(fieldValue.trim(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                        } catch (DateTimeParseException e2) {
                            LocalDate.parse(fieldValue.trim(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                        }
                    }
                    break;
                    
                case "DATETIME":
                case "TIMESTAMP":
                    LocalDateTime.parse(fieldValue.trim());
                    break;
                    
                case "BOOLEAN":
                    String boolValue = fieldValue.trim().toLowerCase();
                    if (!boolValue.equals("true") && !boolValue.equals("false") && 
                        !boolValue.equals("1") && !boolValue.equals("0") &&
                        !boolValue.equals("y") && !boolValue.equals("n")) {
                        throw new IllegalArgumentException("Invalid boolean value");
                    }
                    break;
                    
                case "STRING":
                case "VARCHAR":
                case "TEXT":
                    // String validation always passes
                    break;
                    
                default:
                    log.warn("Unknown data type for validation: {}", dataType);
                    return FieldValidationResult.success(fieldName, fieldValue);
            }
            
            return FieldValidationResult.success(fieldName, fieldValue);
            
        } catch (Exception e) {
            return FieldValidationResult.failure(fieldName, fieldValue,
                getErrorMessage(rule, String.format("%s must be a valid %s (actual: %s)",
                    fieldName, dataType, fieldValue)));
        }
    }
    
    /**
     * Validate against regex pattern.
     */
    private FieldValidationResult validatePattern(String fieldName, String fieldValue, 
                                                 ValidationRuleEntity rule) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        String pattern = rule.getPattern();
        if (pattern == null || pattern.trim().isEmpty()) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        try {
            Pattern compiledPattern = Pattern.compile(pattern);
            if (!compiledPattern.matcher(fieldValue).matches()) {
                return FieldValidationResult.failure(fieldName, fieldValue,
                    getErrorMessage(rule, String.format("%s does not match required pattern", fieldName)));
            }
        } catch (Exception e) {
            log.error("Invalid regex pattern for rule {}: {}", rule.getRuleId(), pattern);
            return FieldValidationResult.failure(fieldName, fieldValue,
                "Invalid validation pattern configured");
        }
        
        return FieldValidationResult.success(fieldName, fieldValue);
    }
    
    /**
     * Validate email format.
     */
    private FieldValidationResult validateEmail(String fieldName, String fieldValue, 
                                               ValidationRuleEntity rule) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        if (!EMAIL_PATTERN.matcher(fieldValue.trim()).matches()) {
            return FieldValidationResult.failure(fieldName, fieldValue,
                getErrorMessage(rule, fieldName + " must be a valid email address"));
        }
        
        return FieldValidationResult.success(fieldName, fieldValue);
    }
    
    /**
     * Validate phone number format.
     */
    private FieldValidationResult validatePhone(String fieldName, String fieldValue, 
                                               ValidationRuleEntity rule) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        // Remove common phone number separators for validation
        String cleanPhone = fieldValue.replaceAll("[\\s\\-\\(\\)\\.]", "");
        
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            return FieldValidationResult.failure(fieldName, fieldValue,
                getErrorMessage(rule, fieldName + " must be a valid phone number"));
        }
        
        return FieldValidationResult.success(fieldName, fieldValue);
    }
    
    /**
     * Validate SSN format.
     */
    private FieldValidationResult validateSSN(String fieldName, String fieldValue, 
                                             ValidationRuleEntity rule) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        if (!SSN_PATTERN.matcher(fieldValue.trim()).matches()) {
            return FieldValidationResult.failure(fieldName, fieldValue,
                getErrorMessage(rule, fieldName + " must be a valid SSN format (XXX-XX-XXXX)"));
        }
        
        return FieldValidationResult.success(fieldName, fieldValue);
    }
    
    /**
     * Validate numeric format.
     */
    private FieldValidationResult validateNumeric(String fieldName, String fieldValue, 
                                                 ValidationRuleEntity rule) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        if (!NUMERIC_PATTERN.matcher(fieldValue.trim()).matches()) {
            return FieldValidationResult.failure(fieldName, fieldValue,
                getErrorMessage(rule, fieldName + " must be a valid number"));
        }
        
        return FieldValidationResult.success(fieldName, fieldValue);
    }
    
    /**
     * Validate date format.
     */
    private FieldValidationResult validateDateFormat(String fieldName, String fieldValue, 
                                                    ValidationRuleEntity rule) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        String pattern = rule.getPattern();
        if (pattern == null || pattern.trim().isEmpty()) {
            pattern = "yyyy-MM-dd"; // Default pattern
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            LocalDate.parse(fieldValue.trim(), formatter);
            return FieldValidationResult.success(fieldName, fieldValue);
        } catch (DateTimeParseException e) {
            return FieldValidationResult.failure(fieldName, fieldValue,
                getErrorMessage(rule, String.format("%s must be in format %s", fieldName, pattern)));
        }
    }
    
    /**
     * Validate numeric range.
     */
    private FieldValidationResult validateRange(String fieldName, String fieldValue, 
                                               ValidationRuleEntity rule) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        try {
            BigDecimal value = new BigDecimal(fieldValue.trim());
            String validationExpression = rule.getValidationExpression();
            
            if (validationExpression != null && !validationExpression.trim().isEmpty()) {
                // Parse range expression like "1-100" or ">0" or ">=1,<=100"
                if (validationExpression.contains("-") && !validationExpression.startsWith("-")) {
                    String[] range = validationExpression.split("-");
                    BigDecimal min = new BigDecimal(range[0].trim());
                    BigDecimal max = new BigDecimal(range[1].trim());
                    
                    if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
                        return FieldValidationResult.failure(fieldName, fieldValue,
                            getErrorMessage(rule, String.format("%s must be between %s and %s", 
                                fieldName, min, max)));
                    }
                }
                // Add more range validation logic as needed
            }
            
            return FieldValidationResult.success(fieldName, fieldValue);
            
        } catch (NumberFormatException e) {
            return FieldValidationResult.failure(fieldName, fieldValue,
                getErrorMessage(rule, fieldName + " must be a valid number for range validation"));
        }
    }
    
    /**
     * Validate unique field (placeholder - would need database check).
     */
    private FieldValidationResult validateUniqueField(String fieldName, String fieldValue, 
                                                     ValidationRuleEntity rule) {
        // This would require a database check to ensure uniqueness
        // For now, return success as placeholder
        log.debug("Unique field validation not fully implemented for field: {}", fieldName);
        return FieldValidationResult.success(fieldName, fieldValue);
    }
    
    /**
     * Validate account number format.
     */
    private FieldValidationResult validateAccountNumber(String fieldName, String fieldValue, 
                                                       ValidationRuleEntity rule) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return FieldValidationResult.success(fieldName, fieldValue);
        }
        
        // Basic account number validation - alphanumeric
        if (!fieldValue.matches("^[A-Za-z0-9]+$")) {
            return FieldValidationResult.failure(fieldName, fieldValue,
                getErrorMessage(rule, fieldName + " must contain only letters and numbers"));
        }
        
        // Apply length validation if specified
        return validateFieldLength(fieldName, fieldValue, rule);
    }
    
    /**
     * Validate using custom SQL (placeholder).
     */
    private FieldValidationResult validateCustomSQL(String fieldName, String fieldValue, 
                                                   ValidationRuleEntity rule) {
        // This would require custom SQL execution
        // For now, return success as placeholder
        log.debug("Custom SQL validation not fully implemented for field: {}", fieldName);
        return FieldValidationResult.success(fieldName, fieldValue);
    }
    
    /**
     * Get error message from rule or use default.
     */
    private String getErrorMessage(ValidationRuleEntity rule, String defaultMessage) {
        String message = rule.getErrorMessage();
        return (message != null && !message.trim().isEmpty()) ? message : defaultMessage;
    }
    
    /**
     * Validate multiple fields with configurable error threshold.
     */
    public ValidationSummary validateFields(Map<String, String> fieldValues, 
                                          Map<String, List<ValidationRuleEntity>> fieldRules,
                                          int errorThreshold) {
        ValidationSummary summary = new ValidationSummary();
        int errorCount = 0;
        int warningCount = 0;
        
        for (Map.Entry<String, String> field : fieldValues.entrySet()) {
            String fieldName = field.getKey();
            String fieldValue = field.getValue();
            List<ValidationRuleEntity> rules = fieldRules.get(fieldName);
            
            List<FieldValidationResult> results = validateField(fieldName, fieldValue, rules);
            summary.addFieldResults(fieldName, results);
            
            for (FieldValidationResult result : results) {
                if (!result.isValid()) {
                    errorCount++;
                } else if (result.hasWarnings()) {
                    warningCount++;
                }
            }
            
            // Check error threshold
            if (errorThreshold > 0 && errorCount >= errorThreshold) {
                summary.setThresholdExceeded(true);
                log.warn("Validation error threshold exceeded: {} errors >= {} threshold", 
                        errorCount, errorThreshold);
                break;
            }
        }
        
        summary.setTotalErrors(errorCount);
        summary.setTotalWarnings(warningCount);
        summary.setTotalFields(fieldValues.size());
        
        return summary;
    }
}