package com.truist.batch.validation;

import com.truist.batch.entity.ValidationRuleEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Validator for data type validations including numeric, date, string, and custom type validations.
 */
@Slf4j
@Component
public class DataTypeValidator {
    
    // Common patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(\\+?1[-. ]?)?\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})$");
    
    private static final Pattern SSN_PATTERN = Pattern.compile(
        "^\\d{3}-\\d{2}-\\d{4}$|^\\d{9}$");
    
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile(
        "^[0-9]{8,20}$");
    
    /**
     * Validate field value against data type rules.
     */
    public FieldValidationResult validate(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        // Skip validation for null/empty values unless required
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            result.setValid(true);
            return result;
        }
        
        String dataType = rule.getDataType();
        if (dataType == null) {
            result.setValid(true);
            return result;
        }
        
        try {
            switch (dataType.toUpperCase()) {
                case "STRING":
                case "VARCHAR":
                case "CHAR":
                    result = validateStringType(fieldName, fieldValue, rule);
                    break;
                    
                case "INTEGER":
                case "INT":
                    result = validateIntegerType(fieldName, fieldValue, rule);
                    break;
                    
                case "DECIMAL":
                case "NUMBER":
                case "NUMERIC":
                    result = validateDecimalType(fieldName, fieldValue, rule);
                    break;
                    
                case "DATE":
                    result = validateDateType(fieldName, fieldValue, rule);
                    break;
                    
                case "DATETIME":
                case "TIMESTAMP":
                    result = validateDateTimeType(fieldName, fieldValue, rule);
                    break;
                    
                case "BOOLEAN":
                    result = validateBooleanType(fieldName, fieldValue, rule);
                    break;
                    
                case "EMAIL":
                    result = validateEmailType(fieldName, fieldValue, rule);
                    break;
                    
                case "PHONE":
                    result = validatePhoneType(fieldName, fieldValue, rule);
                    break;
                    
                case "SSN":
                    result = validateSSNType(fieldName, fieldValue, rule);
                    break;
                    
                case "ACCOUNT_NUMBER":
                    result = validateAccountNumberType(fieldName, fieldValue, rule);
                    break;
                    
                default:
                    result = validateCustomType(fieldName, fieldValue, rule);
                    break;
            }
            
        } catch (Exception e) {
            log.error("Error validating data type {} for field {}: {}", dataType, fieldName, e.getMessage());
            result.setValid(false);
            result.setErrorMessage("Data type validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate string/character data type.
     */
    private FieldValidationResult validateStringType(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        result.setValid(true);
        
        // String values are generally always valid, length validation is handled separately
        // Check for any specific format requirements
        if (rule.getFormat() != null && !rule.getFormat().trim().isEmpty()) {
            try {
                Pattern pattern = Pattern.compile(rule.getFormat());
                if (!pattern.matcher(fieldValue).matches()) {
                    result.setValid(false);
                    result.setErrorMessage(getErrorMessage(rule, 
                        "Field " + fieldName + " does not match required string format: " + rule.getFormat()));
                }
            } catch (Exception e) {
                log.warn("Invalid regex pattern for string validation: {}", rule.getFormat());
            }
        }
        
        return result;
    }
    
    /**
     * Validate integer data type.
     */
    private FieldValidationResult validateIntegerType(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        try {
            // Remove any thousand separators
            String cleanValue = fieldValue.replaceAll("[,\\s]", "");
            
            // Check for decimal point (not allowed in integer)
            if (cleanValue.contains(".")) {
                result.setValid(false);
                result.setErrorMessage(getErrorMessage(rule, 
                    "Field " + fieldName + " contains decimal point but integer expected"));
                return result;
            }
            
            // Parse as long to handle large integers
            long longValue = Long.parseLong(cleanValue);
            
            // Check range if specified
            if (rule.getValidationExpression() != null) {
                if (!validateNumericRange(longValue, rule.getValidationExpression())) {
                    result.setValid(false);
                    result.setErrorMessage(getErrorMessage(rule, 
                        "Field " + fieldName + " value " + longValue + " is outside allowed range"));
                    return result;
                }
            }
            
            result.setValid(true);
            
        } catch (NumberFormatException e) {
            result.setValid(false);
            result.setErrorMessage(getErrorMessage(rule, 
                "Field " + fieldName + " value '" + fieldValue + "' is not a valid integer"));
        }
        
        return result;
    }
    
    /**
     * Validate decimal/numeric data type.
     */
    private FieldValidationResult validateDecimalType(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        try {
            // Remove any thousand separators but keep decimal point
            String cleanValue = fieldValue.replaceAll("[,\\s]", "");
            
            BigDecimal decimalValue = new BigDecimal(cleanValue);
            
            // Check range if specified
            if (rule.getValidationExpression() != null) {
                if (!validateNumericRange(decimalValue.doubleValue(), rule.getValidationExpression())) {
                    result.setValid(false);
                    result.setErrorMessage(getErrorMessage(rule, 
                        "Field " + fieldName + " value " + decimalValue + " is outside allowed range"));
                    return result;
                }
            }
            
            // Check precision and scale if specified in format
            if (rule.getFormat() != null && rule.getFormat().contains(",")) {
                String[] precisionScale = rule.getFormat().split(",");
                if (precisionScale.length == 2) {
                    try {
                        int precision = Integer.parseInt(precisionScale[0].trim());
                        int scale = Integer.parseInt(precisionScale[1].trim());
                        
                        if (decimalValue.precision() > precision) {
                            result.setValid(false);
                            result.setErrorMessage(getErrorMessage(rule, 
                                "Field " + fieldName + " precision exceeds maximum allowed (" + precision + ")"));
                            return result;
                        }
                        
                        if (decimalValue.scale() > scale) {
                            result.setValid(false);
                            result.setErrorMessage(getErrorMessage(rule, 
                                "Field " + fieldName + " scale exceeds maximum allowed (" + scale + ")"));
                            return result;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Invalid precision/scale format: {}", rule.getFormat());
                    }
                }
            }
            
            result.setValid(true);
            
        } catch (NumberFormatException e) {
            result.setValid(false);
            result.setErrorMessage(getErrorMessage(rule, 
                "Field " + fieldName + " value '" + fieldValue + "' is not a valid decimal number"));
        }
        
        return result;
    }
    
    /**
     * Validate date data type.
     */
    private FieldValidationResult validateDateType(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        String dateFormat = rule.getFormat() != null ? rule.getFormat() : "yyyy-MM-dd";
        
        try {
            // Try Java 8 LocalDate first
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
            LocalDate date = LocalDate.parse(fieldValue, formatter);
            
            // Additional date validations can be added here
            result.setValid(true);
            
        } catch (DateTimeParseException e) {
            // Try legacy SimpleDateFormat as fallback
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                sdf.setLenient(false); // Strict parsing
                sdf.parse(fieldValue);
                result.setValid(true);
            } catch (ParseException pe) {
                result.setValid(false);
                result.setErrorMessage(getErrorMessage(rule, 
                    "Field " + fieldName + " value '" + fieldValue + "' is not a valid date (expected format: " + dateFormat + ")"));
            }
        }
        
        return result;
    }
    
    /**
     * Validate datetime/timestamp data type.
     */
    private FieldValidationResult validateDateTimeType(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        String dateTimeFormat = rule.getFormat() != null ? rule.getFormat() : "yyyy-MM-dd HH:mm:ss";
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat);
            LocalDateTime dateTime = LocalDateTime.parse(fieldValue, formatter);
            
            result.setValid(true);
            
        } catch (DateTimeParseException e) {
            result.setValid(false);
            result.setErrorMessage(getErrorMessage(rule, 
                "Field " + fieldName + " value '" + fieldValue + "' is not a valid datetime (expected format: " + dateTimeFormat + ")"));
        }
        
        return result;
    }
    
    /**
     * Validate boolean data type.
     */
    private FieldValidationResult validateBooleanType(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        String lowerValue = fieldValue.toLowerCase().trim();
        
        if ("true".equals(lowerValue) || "false".equals(lowerValue) || 
            "1".equals(lowerValue) || "0".equals(lowerValue) ||
            "y".equals(lowerValue) || "n".equals(lowerValue) ||
            "yes".equals(lowerValue) || "no".equals(lowerValue)) {
            result.setValid(true);
        } else {
            result.setValid(false);
            result.setErrorMessage(getErrorMessage(rule, 
                "Field " + fieldName + " value '" + fieldValue + "' is not a valid boolean (expected: true/false, 1/0, Y/N, Yes/No)"));
        }
        
        return result;
    }
    
    /**
     * Validate email format.
     */
    private FieldValidationResult validateEmailType(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        if (EMAIL_PATTERN.matcher(fieldValue).matches()) {
            result.setValid(true);
        } else {
            result.setValid(false);
            result.setErrorMessage(getErrorMessage(rule, 
                "Field " + fieldName + " value '" + fieldValue + "' is not a valid email address"));
        }
        
        return result;
    }
    
    /**
     * Validate phone number format.
     */
    private FieldValidationResult validatePhoneType(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        if (PHONE_PATTERN.matcher(fieldValue).matches()) {
            result.setValid(true);
        } else {
            result.setValid(false);
            result.setErrorMessage(getErrorMessage(rule, 
                "Field " + fieldName + " value '" + fieldValue + "' is not a valid phone number"));
        }
        
        return result;
    }
    
    /**
     * Validate SSN format.
     */
    private FieldValidationResult validateSSNType(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        if (SSN_PATTERN.matcher(fieldValue).matches()) {
            result.setValid(true);
        } else {
            result.setValid(false);
            result.setErrorMessage(getErrorMessage(rule, 
                "Field " + fieldName + " value '" + fieldValue + "' is not a valid SSN format (expected: XXX-XX-XXXX or XXXXXXXXX)"));
        }
        
        return result;
    }
    
    /**
     * Validate account number format.
     */
    private FieldValidationResult validateAccountNumberType(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        if (ACCOUNT_NUMBER_PATTERN.matcher(fieldValue).matches()) {
            result.setValid(true);
        } else {
            result.setValid(false);
            result.setErrorMessage(getErrorMessage(rule, 
                "Field " + fieldName + " value '" + fieldValue + "' is not a valid account number (expected: 8-20 digits)"));
        }
        
        return result;
    }
    
    /**
     * Validate custom data type using validation expression.
     */
    private FieldValidationResult validateCustomType(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        if (rule.getValidationExpression() != null) {
            try {
                Pattern pattern = Pattern.compile(rule.getValidationExpression());
                if (pattern.matcher(fieldValue).matches()) {
                    result.setValid(true);
                } else {
                    result.setValid(false);
                    result.setErrorMessage(getErrorMessage(rule, 
                        "Field " + fieldName + " does not match custom validation pattern"));
                }
            } catch (Exception e) {
                log.error("Error executing custom validation pattern: {}", rule.getValidationExpression(), e);
                result.setValid(false);
                result.setErrorMessage("Custom validation pattern error: " + e.getMessage());
            }
        } else {
            result.setValid(true); // No validation expression means anything is valid
        }
        
        return result;
    }
    
    /**
     * Validate numeric range.
     */
    private boolean validateNumericRange(double value, String rangeExpression) {
        if (rangeExpression == null || rangeExpression.trim().isEmpty()) {
            return true;
        }
        
        try {
            // Parse range expression (e.g., "0,100" or "MIN:0,MAX:100")
            String[] parts = rangeExpression.split(",");
            if (parts.length == 2) {
                double min = Double.parseDouble(parts[0].trim());
                double max = Double.parseDouble(parts[1].trim());
                return value >= min && value <= max;
            }
        } catch (Exception e) {
            log.warn("Invalid range expression: {}", rangeExpression);
        }
        
        return true;
    }
    
    /**
     * Get error message from rule or use default.
     */
    private String getErrorMessage(ValidationRuleEntity rule, String defaultMessage) {
        return rule.getErrorMessage() != null && !rule.getErrorMessage().trim().isEmpty() 
            ? rule.getErrorMessage() 
            : defaultMessage;
    }
}