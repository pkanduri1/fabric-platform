package com.truist.batch.validation;

import com.truist.batch.entity.ValidationRuleEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Validator for business rule validations using custom Java classes and methods.
 */
@Slf4j
@Component
public class BusinessRuleValidator {
    
    // Cache for loaded business rule classes
    private final Map<String, Class<?>> classCache = new HashMap<>();
    private final Map<String, Object> instanceCache = new HashMap<>();
    
    /**
     * Validate field value against business rule.
     */
    public FieldValidationResult validate(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        if (rule.getBusinessRuleClass() == null || rule.getBusinessRuleClass().trim().isEmpty()) {
            result.setValid(true);
            return result;
        }
        
        try {
            // Load and execute business rule
            BusinessRuleResult ruleResult = executeBusinessRule(fieldName, fieldValue, rule);
            
            result.setValid(ruleResult.isValid());
            if (!ruleResult.isValid()) {
                result.setErrorMessage(ruleResult.getErrorMessage());
            }
            if (ruleResult.getWarningMessage() != null) {
                result.setWarningMessage(ruleResult.getWarningMessage());
            }
            if (ruleResult.getTransformedValue() != null) {
                result.setTransformedValue(ruleResult.getTransformedValue());
            }
            
        } catch (Exception e) {
            log.error("Error executing business rule {} for field {}: {}", 
                     rule.getBusinessRuleClass(), fieldName, e.getMessage());
            result.setValid(false);
            result.setErrorMessage("Business rule execution error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Execute business rule using reflection.
     */
    private BusinessRuleResult executeBusinessRule(String fieldName, String fieldValue, ValidationRuleEntity rule) 
            throws Exception {
        
        String className = rule.getBusinessRuleClass();
        
        // Load class (with caching)
        Class<?> ruleClass = loadBusinessRuleClass(className);
        
        // Get instance (with caching)
        Object ruleInstance = getBusinessRuleInstance(ruleClass);
        
        // Determine method to call
        Method validationMethod = findValidationMethod(ruleClass);
        
        if (validationMethod == null) {
            throw new IllegalArgumentException("No suitable validation method found in business rule class: " + className);
        }
        
        // Prepare parameters
        Object[] parameters = prepareMethodParameters(validationMethod, fieldName, fieldValue, rule);
        
        // Execute method
        Object methodResult = validationMethod.invoke(ruleInstance, parameters);
        
        // Convert result to BusinessRuleResult
        return convertMethodResult(methodResult, fieldName, fieldValue);
    }
    
    /**
     * Load business rule class with caching.
     */
    private Class<?> loadBusinessRuleClass(String className) throws ClassNotFoundException {
        Class<?> ruleClass = classCache.get(className);
        if (ruleClass == null) {
            ruleClass = Class.forName(className);
            classCache.put(className, ruleClass);
            log.debug("Loaded business rule class: {}", className);
        }
        return ruleClass;
    }
    
    /**
     * Get business rule instance with caching.
     */
    private Object getBusinessRuleInstance(Class<?> ruleClass) throws Exception {
        String className = ruleClass.getName();
        Object instance = instanceCache.get(className);
        if (instance == null) {
            instance = ruleClass.getDeclaredConstructor().newInstance();
            instanceCache.put(className, instance);
            log.debug("Created business rule instance: {}", className);
        }
        return instance;
    }
    
    /**
     * Find appropriate validation method in the business rule class.
     */
    private Method findValidationMethod(Class<?> ruleClass) {
        Method[] methods = ruleClass.getDeclaredMethods();
        
        // Look for specific method signatures in order of preference
        for (Method method : methods) {
            String methodName = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();
            
            // 1. validate(String fieldName, String fieldValue, Map<String, Object> parameters)
            if ("validate".equals(methodName) && paramTypes.length == 3 &&
                String.class.equals(paramTypes[0]) && String.class.equals(paramTypes[1]) &&
                Map.class.isAssignableFrom(paramTypes[2])) {
                return method;
            }
            
            // 2. validate(String fieldName, String fieldValue)
            if ("validate".equals(methodName) && paramTypes.length == 2 &&
                String.class.equals(paramTypes[0]) && String.class.equals(paramTypes[1])) {
                return method;
            }
            
            // 3. validate(String fieldValue)
            if ("validate".equals(methodName) && paramTypes.length == 1 &&
                String.class.equals(paramTypes[0])) {
                return method;
            }
        }
        
        // Look for any method named "execute" or "apply"
        for (Method method : methods) {
            String methodName = method.getName();
            if ("execute".equals(methodName) || "apply".equals(methodName)) {
                return method;
            }
        }
        
        return null;
    }
    
    /**
     * Prepare method parameters based on method signature.
     */
    private Object[] prepareMethodParameters(Method method, String fieldName, String fieldValue, ValidationRuleEntity rule) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] parameters = new Object[paramTypes.length];
        
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            
            if (String.class.equals(paramType)) {
                if (i == 0) {
                    // First string parameter is typically field name or field value
                    parameters[i] = method.getName().contains("fieldName") || paramTypes.length > 2 ? fieldName : fieldValue;
                } else if (i == 1) {
                    // Second string parameter is typically field value
                    parameters[i] = fieldValue;
                } else {
                    parameters[i] = fieldValue;
                }
            } else if (Map.class.isAssignableFrom(paramType)) {
                // Prepare parameters map
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("fieldName", fieldName);
                paramMap.put("fieldValue", fieldValue);
                paramMap.put("ruleId", rule.getRuleId());
                paramMap.put("dataType", rule.getDataType());
                paramMap.put("validationExpression", rule.getValidationExpression());
                paramMap.put("pattern", rule.getPattern());
                paramMap.put("maxLength", rule.getMaxLength());
                paramMap.put("minLength", rule.getMinLength());
                paramMap.put("referenceTable", rule.getReferenceTable());
                paramMap.put("referenceColumn", rule.getReferenceColumn());
                
                // Add business rule parameters if available
                if (rule.getBusinessRuleClass() != null) {
                    paramMap.put("businessRuleClass", rule.getBusinessRuleClass());
                }
                
                parameters[i] = paramMap;
            } else {
                // Default to field value for other types
                parameters[i] = fieldValue;
            }
        }
        
        return parameters;
    }
    
    /**
     * Convert method result to BusinessRuleResult.
     */
    private BusinessRuleResult convertMethodResult(Object methodResult, String fieldName, String fieldValue) {
        BusinessRuleResult result = new BusinessRuleResult();
        
        if (methodResult == null) {
            result.setValid(false);
            result.setErrorMessage("Business rule returned null result");
            return result;
        }
        
        if (methodResult instanceof Boolean) {
            result.setValid((Boolean) methodResult);
            if (!result.isValid()) {
                result.setErrorMessage("Business rule validation failed for field " + fieldName);
            }
        } else if (methodResult instanceof BusinessRuleResult) {
            return (BusinessRuleResult) methodResult;
        } else if (methodResult instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) methodResult;
            
            result.setValid(Boolean.TRUE.equals(resultMap.get("valid")));
            result.setErrorMessage((String) resultMap.get("errorMessage"));
            result.setWarningMessage((String) resultMap.get("warningMessage"));
            result.setTransformedValue((String) resultMap.get("transformedValue"));
        } else if (methodResult instanceof String) {
            // Assume string result means validation failed with error message
            result.setValid(false);
            result.setErrorMessage((String) methodResult);
        } else {
            // For other types, assume success if not null
            result.setValid(true);
            if (!fieldValue.equals(methodResult.toString())) {
                result.setTransformedValue(methodResult.toString());
            }
        }
        
        return result;
    }
    
    /**
     * Clear class and instance caches (useful for testing or reloading).
     */
    public void clearCache() {
        classCache.clear();
        instanceCache.clear();
        log.info("Business rule validator cache cleared");
    }
    
    /**
     * Get cache statistics for monitoring.
     */
    public Map<String, Integer> getCacheStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("cachedClasses", classCache.size());
        stats.put("cachedInstances", instanceCache.size());
        return stats;
    }
    
    /**
     * Result object for business rule execution.
     */
    public static class BusinessRuleResult {
        private boolean valid = true;
        private String errorMessage;
        private String warningMessage;
        private String transformedValue;
        private Map<String, Object> additionalData = new HashMap<>();
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getWarningMessage() { return warningMessage; }
        public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
        
        public String getTransformedValue() { return transformedValue; }
        public void setTransformedValue(String transformedValue) { this.transformedValue = transformedValue; }
        
        public Map<String, Object> getAdditionalData() { return additionalData; }
        public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }
        
        public void addData(String key, Object value) {
            additionalData.put(key, value);
        }
    }
}