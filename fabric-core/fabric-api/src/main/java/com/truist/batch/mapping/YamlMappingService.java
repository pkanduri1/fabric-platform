package com.truist.batch.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.truist.batch.model.Condition;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.YamlMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for loading and processing YAML mapping files and transforming field data.
 * 
 * This service handles:
 * 1. Loading YAML configuration files from classpath
 * 2. Parsing multi-document YAML files into YamlMapping objects
 * 3. Field transformation logic including conditional processing
 * 4. Integration with the existing Spring Batch framework
 * 
 * Used by GenericProcessor and other batch processing components.
 */
@Slf4j
@Service
public class YamlMappingService {

    private final ObjectMapper yamlMapper;

    /**
     * Default constructor that initializes YAML ObjectMapper.
     */
    public YamlMappingService() {
        YAMLFactory yamlFactory = new YAMLFactory();
        this.yamlMapper = new ObjectMapper(yamlFactory);
    }

    /**
     * Loads YAML mappings from a classpath resource.
     * 
     * @param resourcePath Path to the YAML file (relative to classpath)
     * @return List of YamlMapping objects (supports multi-document YAML)
     */
    public List<YamlMapping> loadYamlMappings(String resourcePath) {
        log.info("🔄 Loading YAML mappings from: {}", resourcePath);
        
        try {
            Resource resource = new ClassPathResource(resourcePath);
            
            if (!resource.exists()) {
                log.error("❌ YAML file not found: {}", resourcePath);
                throw new IllegalArgumentException("YAML file not found: " + resourcePath);
            }
            
            try (InputStream inputStream = resource.getInputStream()) {
                String yamlContent = new String(inputStream.readAllBytes());
                return parseMultiDocumentYaml(yamlContent);
            }
            
        } catch (IOException e) {
            log.error("❌ Failed to load YAML mappings from {}: {}", resourcePath, e.getMessage(), e);
            throw new RuntimeException("Failed to load YAML mappings from " + resourcePath, e);
        }
    }

    /**
     * Parses multi-document YAML content into a list of YamlMapping objects.
     * 
     * @param yamlContent YAML content string (may contain multiple documents)
     * @return List of parsed YamlMapping objects
     */
    private List<YamlMapping> parseMultiDocumentYaml(String yamlContent) {
        log.debug("🔄 Parsing multi-document YAML content");
        
        List<YamlMapping> mappings = new ArrayList<>();
        
        // Split by document separator "---"
        String[] documents = yamlContent.split("(?m)^---\\s*$");
        
        for (int i = 0; i < documents.length; i++) {
            String document = documents[i].trim();
            
            if (document.isEmpty()) {
                continue;
            }
            
            try {
                YamlMapping mapping = yamlMapper.readValue(document, YamlMapping.class);
                mappings.add(mapping);
                log.debug("✅ Parsed YAML document {}: {} fields", i, 
                         mapping.getFields() != null ? mapping.getFields().size() : 0);
                
            } catch (IOException e) {
                log.error("❌ Failed to parse YAML document {}: {}", i, e.getMessage());
                throw new RuntimeException("Failed to parse YAML document " + i, e);
            }
        }
        
        log.info("✅ Successfully parsed {} YAML documents", mappings.size());
        return mappings;
    }

    /**
     * Gets a specific YAML mapping by transaction type from a resource path.
     * 
     * @param resourcePath Path to the YAML file (relative to classpath)
     * @param transactionType Transaction type identifier (e.g., "default", "credit", "debit")
     * @return YamlMapping object for the specified transaction type
     * @throws IllegalArgumentException if transaction type is not found
     */
    public YamlMapping getMapping(String resourcePath, String transactionType) {
        log.debug("🔄 Getting mapping for transaction type '{}' from: {}", transactionType, resourcePath);
        
        List<YamlMapping> mappings = loadYamlMappings(resourcePath);
        
        for (YamlMapping mapping : mappings) {
            if (transactionType.equals(mapping.getTransactionType())) {
                log.debug("✅ Found mapping for transaction type: {}", transactionType);
                return mapping;
            }
        }
        
        log.error("❌ Mapping not found for transaction type: {}", transactionType);
        throw new IllegalArgumentException("Mapping not found for transaction type: " + transactionType);
    }

    /**
     * Transforms a field value based on the field mapping configuration.
     * 
     * @param row Data row containing source field values
     * @param mapping Field mapping configuration
     * @return Transformed field value
     */
    public String transformField(Map<String, Object> row, FieldMapping mapping) {
        log.debug("🔄 Transforming field: {}", mapping.getFieldName());
        
        String result = null;
        
        try {
            // Apply transformation based on type
            switch (mapping.getTransformationType().toLowerCase()) {
                case "source":
                    result = transformSourceField(row, mapping);
                    break;
                case "constant":
                    result = transformConstantField(mapping);
                    break;
                case "conditional":
                    result = transformConditionalField(row, mapping);
                    break;
                case "composite":
                    result = transformCompositeField(row, mapping);
                    break;
                default:
                    log.warn("⚠️ Unknown transformation type: {}", mapping.getTransformationType());
                    result = mapping.getDefaultValue();
                    break;
            }
            
            // Apply formatting and padding
            result = applyFormattingAndPadding(result, mapping);
            
            log.debug("✅ Transformed field {} -> '{}'", mapping.getFieldName(), result);
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error transforming field {}: {}", mapping.getFieldName(), e.getMessage());
            return applyFormattingAndPadding(mapping.getDefaultValue(), mapping);
        }
    }

    /**
     * Transforms a source field by extracting value from the data row.
     */
    private String transformSourceField(Map<String, Object> row, FieldMapping mapping) {
        String sourceField = mapping.getSourceField();
        
        if (sourceField == null || sourceField.trim().isEmpty()) {
            log.warn("⚠️ Source field not specified for source transformation");
            return mapping.getDefaultValue();
        }
        
        Object value = row.get(sourceField);
        return value != null ? value.toString() : mapping.getDefaultValue();
    }

    /**
     * Transforms a constant field by returning the configured constant value.
     */
    private String transformConstantField(FieldMapping mapping) {
        String value = mapping.getValue();
        return value != null ? value : mapping.getDefaultValue();
    }

    /**
     * Transforms a conditional field by evaluating conditions and returning appropriate value.
     */
    private String transformConditionalField(Map<String, Object> row, FieldMapping mapping) {
        List<Condition> conditions = mapping.getConditions();
        
        if (conditions == null || conditions.isEmpty()) {
            log.debug("No conditions specified for conditional field");
            return mapping.getDefaultValue();
        }
        
        // Evaluate the main condition
        Condition mainCondition = conditions.get(0);
        return evaluateCondition(row, mainCondition, mapping.getDefaultValue());
    }

    /**
     * Evaluates a single condition and returns the appropriate result.
     */
    private String evaluateCondition(Map<String, Object> row, Condition condition, String defaultValue) {
        // Evaluate the main IF expression
        if (evaluateExpression(row, condition.getIfExpr())) {
            return resolveValue(row, condition.getThen());
        }
        
        // Evaluate ELSE-IF expressions
        if (condition.getElseIfExprs() != null) {
            for (Condition elseIf : condition.getElseIfExprs()) {
                if (evaluateExpression(row, elseIf.getIfExpr())) {
                    return resolveValue(row, elseIf.getThen());
                }
            }
        }
        
        // Evaluate ELSE expression
        if (condition.getElseExpr() != null && !condition.getElseExpr().trim().isEmpty()) {
            return resolveValue(row, condition.getElseExpr());
        }
        
        // Return default value if no conditions matched
        return defaultValue;
    }

    /**
     * Evaluates a boolean expression against the data row.
     */
    private boolean evaluateExpression(Map<String, Object> row, String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Simple expression evaluation - can be enhanced with a proper expression engine
            return evaluateSimpleExpression(row, expression.trim());
            
        } catch (Exception e) {
            log.error("❌ Error evaluating expression '{}': {}", expression, e.getMessage());
            return false;
        }
    }

    /**
     * Evaluates simple expressions like "FIELD = 'VALUE'" or "FIELD >= 100".
     */
    private boolean evaluateSimpleExpression(Map<String, Object> row, String expression) {
        // Handle null checks
        if (expression.contains("== null")) {
            String fieldName = expression.replaceAll("\\s*==\\s*null", "").trim();
            return row.get(fieldName) == null;
        }
        
        if (expression.contains("!= null")) {
            String fieldName = expression.replaceAll("\\s*!=\\s*null", "").trim();
            return row.get(fieldName) != null;
        }
        
        // Handle string equality
        Pattern stringPattern = Pattern.compile("(\\w+)\\s*=\\s*'([^']*)'");
        Matcher stringMatcher = stringPattern.matcher(expression);
        if (stringMatcher.matches()) {
            String fieldName = stringMatcher.group(1);
            String expectedValue = stringMatcher.group(2);
            Object actualValue = row.get(fieldName);
            return expectedValue.equals(actualValue != null ? actualValue.toString() : "");
        }
        
        // Handle numeric comparisons
        Pattern numericPattern = Pattern.compile("(\\w+)\\s*(>=|<=|>|<|=)\\s*(\\d+(?:\\.\\d+)?)");
        Matcher numericMatcher = numericPattern.matcher(expression);
        if (numericMatcher.matches()) {
            String fieldName = numericMatcher.group(1);
            String operator = numericMatcher.group(2);
            double expectedValue = Double.parseDouble(numericMatcher.group(3));
            
            Object actualValue = row.get(fieldName);
            if (actualValue == null) {
                return false;
            }
            
            try {
                double actual = Double.parseDouble(actualValue.toString());
                switch (operator) {
                    case ">=": return actual >= expectedValue;
                    case "<=": return actual <= expectedValue;
                    case ">": return actual > expectedValue;
                    case "<": return actual < expectedValue;
                    case "=": return actual == expectedValue;
                    default: return false;
                }
            } catch (NumberFormatException e) {
                log.warn("⚠️ Cannot parse numeric value for comparison: {}", actualValue);
                return false;
            }
        }
        
        log.warn("⚠️ Unsupported expression format: {}", expression);
        return false;
    }

    /**
     * Resolves a value that could be either a literal string or a field reference.
     */
    private String resolveValue(Map<String, Object> row, String value) {
        if (value == null) {
            return null;
        }
        
        // If the value exists as a field in the row, use that value
        if (row.containsKey(value)) {
            Object fieldValue = row.get(value);
            return fieldValue != null ? fieldValue.toString() : null;
        }
        
        // Otherwise, treat it as a literal value
        return value;
    }

    /**
     * Transforms a composite field by combining multiple source fields.
     */
    private String transformCompositeField(Map<String, Object> row, FieldMapping mapping) {
        if (!mapping.isComposite() || mapping.getSources() == null || mapping.getSources().isEmpty()) {
            log.warn("⚠️ Composite field configuration is invalid");
            return mapping.getDefaultValue();
        }
        
        String delimiter = mapping.getDelimiter() != null ? mapping.getDelimiter() : "";
        
        List<String> values = new ArrayList<>();
        for (String sourceField : mapping.getSources()) {
            Object value = row.get(sourceField);
            if (value != null) {
                values.add(value.toString());
            }
        }
        
        return String.join(delimiter, values);
    }

    /**
     * Applies formatting and padding to the field value.
     */
    private String applyFormattingAndPadding(String value, FieldMapping mapping) {
        if (value == null) {
            value = "";
        }
        
        // Apply length constraints and padding if specified
        if (mapping.getLength() > 0) {
            String padChar = mapping.getPadChar() != null ? mapping.getPadChar() : " ";
            String padDirection = mapping.getPad();
            
            if ("right".equalsIgnoreCase(padDirection)) {
                // Pad on the right (left-align)
                value = String.format("%-" + mapping.getLength() + "s", value).replace(' ', padChar.charAt(0));
            } else if ("left".equalsIgnoreCase(padDirection)) {
                // Pad on the left (right-align)
                value = String.format("%" + mapping.getLength() + "s", value).replace(' ', padChar.charAt(0));
            }
            
            // Truncate if too long
            if (value.length() > mapping.getLength()) {
                value = value.substring(0, mapping.getLength());
            }
        }
        
        return value;
    }
}