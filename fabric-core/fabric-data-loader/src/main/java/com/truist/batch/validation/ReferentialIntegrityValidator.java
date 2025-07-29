package com.truist.batch.validation;

import com.truist.batch.entity.ValidationRuleEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validator for referential integrity checks against database tables.
 * Includes caching mechanisms for performance optimization.
 */
@Slf4j
@Component
public class ReferentialIntegrityValidator {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // Cache for lookup results to improve performance
    private final Map<String, Map<String, Boolean>> lookupCache = new ConcurrentHashMap<>();
    
    // Cache configuration
    private static final int MAX_CACHE_SIZE = 10000;
    private static final long CACHE_EXPIRY_MS = 300000; // 5 minutes
    
    // Cache metadata
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    /**
     * Validate field value against referential integrity constraints.
     */
    public FieldValidationResult validate(String fieldName, String fieldValue, ValidationRuleEntity rule) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setRuleId(rule.getRuleId());
        
        // Skip validation for null/empty values
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            result.setValid(true);
            return result;
        }
        
        try {
            boolean exists = checkReferentialIntegrity(fieldValue, rule);
            
            result.setValid(exists);
            if (!exists) {
                result.setErrorMessage(getErrorMessage(rule, fieldName, fieldValue));
            }
            
        } catch (Exception e) {
            log.error("Error validating referential integrity for field {} with rule {}: {}", 
                     fieldName, rule.getRuleId(), e.getMessage());
            result.setValid(false);
            result.setErrorMessage("Referential integrity check failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Check if value exists in reference table/column.
     */
    private boolean checkReferentialIntegrity(String fieldValue, ValidationRuleEntity rule) throws DataAccessException {
        String referenceTable = rule.getReferenceTable();
        String referenceColumn = rule.getReferenceColumn();
        String lookupSql = rule.getLookupSql();
        
        // Use custom SQL if provided
        if (lookupSql != null && !lookupSql.trim().isEmpty()) {
            return executeCustomLookupSql(fieldValue, lookupSql, rule);
        }
        
        // Use reference table/column
        if (referenceTable != null && referenceColumn != null) {
            return executeTableColumnLookup(fieldValue, referenceTable, referenceColumn, rule);
        }
        
        log.warn("No reference table/column or lookup SQL specified for rule {}", rule.getRuleId());
        return true; // Assume valid if no reference is specified
    }
    
    /**
     * Execute custom lookup SQL.
     */
    private boolean executeCustomLookupSql(String fieldValue, String lookupSql, ValidationRuleEntity rule) {
        try {
            // Check cache first
            String cacheKey = getCacheKey(lookupSql, fieldValue);
            Boolean cachedResult = getCachedResult(cacheKey);
            if (cachedResult != null) {
                log.debug("Cache hit for referential integrity check: {}", cacheKey);
                return cachedResult;
            }
            
            // Replace placeholders in SQL
            String sql = prepareLookupSql(lookupSql, fieldValue);
            
            // Execute query
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            boolean exists = !results.isEmpty();
            
            // Cache result
            cacheResult(cacheKey, exists);
            
            log.debug("Referential integrity check - SQL: {}, Value: {}, Exists: {}", sql, fieldValue, exists);
            return exists;
            
        } catch (DataAccessException e) {
            log.error("Database error during referential integrity check: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error executing custom lookup SQL: {}", e.getMessage());
            throw new RuntimeException("Custom lookup SQL execution failed", e);
        }
    }
    
    /**
     * Execute table/column lookup.
     */
    private boolean executeTableColumnLookup(String fieldValue, String referenceTable, 
                                           String referenceColumn, ValidationRuleEntity rule) {
        try {
            // Check cache first
            String cacheKey = getCacheKey(referenceTable + "." + referenceColumn, fieldValue);
            Boolean cachedResult = getCachedResult(cacheKey);
            if (cachedResult != null) {
                log.debug("Cache hit for table/column lookup: {}", cacheKey);
                return cachedResult;
            }
            
            // Build SQL query
            String sql = buildTableColumnSql(referenceTable, referenceColumn);
            
            // Execute query
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, fieldValue);
            boolean exists = count != null && count > 0;
            
            // Cache result
            cacheResult(cacheKey, exists);
            
            log.debug("Referential integrity check - Table: {}, Column: {}, Value: {}, Exists: {}", 
                     referenceTable, referenceColumn, fieldValue, exists);
            return exists;
            
        } catch (DataAccessException e) {
            log.error("Database error during table/column lookup: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error executing table/column lookup: {}", e.getMessage());
            throw new RuntimeException("Table/column lookup execution failed", e);
        }
    }
    
    /**
     * Prepare lookup SQL by replacing placeholders.
     */
    private String prepareLookupSql(String lookupSql, String fieldValue) {
        // Replace common placeholders
        String sql = lookupSql;
        sql = sql.replace(":FIELD_VALUE", "?");
        sql = sql.replace("${FIELD_VALUE}", "?");
        sql = sql.replace("#{fieldValue}", "?");
        
        // Validate SQL to prevent injection
        validateSql(sql);
        
        return sql;
    }
    
    /**
     * Build SQL for table/column lookup.
     */
    private String buildTableColumnSql(String referenceTable, String referenceColumn) {
        // Validate table and column names to prevent injection
        validateIdentifier(referenceTable, "table");
        validateIdentifier(referenceColumn, "column");
        
        return String.format("SELECT COUNT(*) FROM %s WHERE %s = ?", referenceTable, referenceColumn);
    }
    
    /**
     * Validate SQL query to prevent injection attacks.
     */
    private void validateSql(String sql) {
        String upperSql = sql.toUpperCase();
        
        // Check for potentially dangerous SQL keywords
        String[] dangerousKeywords = {
            "DROP", "DELETE", "UPDATE", "INSERT", "CREATE", "ALTER", 
            "TRUNCATE", "EXEC", "EXECUTE", "MERGE", "GRANT", "REVOKE"
        };
        
        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) {
                throw new IllegalArgumentException("SQL contains potentially dangerous keyword: " + keyword);
            }
        }
        
        // Ensure SQL is a SELECT statement
        if (!upperSql.trim().startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT statements are allowed for referential integrity checks");
        }
    }
    
    /**
     * Validate database identifier (table/column name).
     */
    private void validateIdentifier(String identifier, String type) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException(type + " name cannot be empty");
        }
        
        // Check for valid identifier pattern (letters, numbers, underscores)
        if (!identifier.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid " + type + " name: " + identifier);
        }
        
        // Check length limits
        if (identifier.length() > 30) {
            throw new IllegalArgumentException(type + " name too long: " + identifier);
        }
    }
    
    /**
     * Generate cache key for lookup result.
     */
    private String getCacheKey(String query, String value) {
        return query.hashCode() + ":" + value;
    }
    
    /**
     * Get cached result if available and not expired.
     */
    private Boolean getCachedResult(String cacheKey) {
        // Check if cache entry exists
        Map<String, Boolean> queryCache = lookupCache.get(extractQueryFromKey(cacheKey));
        if (queryCache == null) {
            return null;
        }
        
        // Check if result is cached
        Boolean result = queryCache.get(cacheKey);
        if (result == null) {
            return null;
        }
        
        // Check if cache entry is expired
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) > CACHE_EXPIRY_MS) {
            // Remove expired entry
            queryCache.remove(cacheKey);
            cacheTimestamps.remove(cacheKey);
            return null;
        }
        
        return result;
    }
    
    /**
     * Cache lookup result.
     */
    private void cacheResult(String cacheKey, boolean result) {
        try {
            String query = extractQueryFromKey(cacheKey);
            
            // Get or create query cache
            Map<String, Boolean> queryCache = lookupCache.computeIfAbsent(query, k -> new ConcurrentHashMap<>());
            
            // Check cache size limits
            if (queryCache.size() >= MAX_CACHE_SIZE) {
                // Remove oldest entries (simple FIFO)
                queryCache.clear();
                log.debug("Cleared cache for query due to size limit: {}", query);
            }
            
            // Cache result
            queryCache.put(cacheKey, result);
            cacheTimestamps.put(cacheKey, System.currentTimeMillis());
            
        } catch (Exception e) {
            log.warn("Error caching referential integrity result: {}", e.getMessage());
        }
    }
    
    /**
     * Extract query identifier from cache key.
     */
    private String extractQueryFromKey(String cacheKey) {
        int colonIndex = cacheKey.indexOf(':');
        return colonIndex > 0 ? cacheKey.substring(0, colonIndex) : cacheKey;
    }
    
    /**
     * Get error message for referential integrity failure.
     */
    private String getErrorMessage(ValidationRuleEntity rule, String fieldName, String fieldValue) {
        if (rule.getErrorMessage() != null && !rule.getErrorMessage().trim().isEmpty()) {
            return rule.getErrorMessage();
        }
        
        StringBuilder message = new StringBuilder();
        message.append("Field ").append(fieldName).append(" value '").append(fieldValue).append("'");
        
        if (rule.getReferenceTable() != null && rule.getReferenceColumn() != null) {
            message.append(" does not exist in reference table ")
                   .append(rule.getReferenceTable())
                   .append(" column ")
                   .append(rule.getReferenceColumn());
        } else {
            message.append(" failed referential integrity check");
        }
        
        return message.toString();
    }
    
    /**
     * Clear all cached results.
     */
    public void clearCache() {
        lookupCache.clear();
        cacheTimestamps.clear();
        log.info("Referential integrity validator cache cleared");
    }
    
    /**
     * Get cache statistics for monitoring.
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalEntries = 0;
        for (Map<String, Boolean> queryCache : lookupCache.values()) {
            totalEntries += queryCache.size();
        }
        
        stats.put("queryCaches", lookupCache.size());
        stats.put("totalCachedEntries", totalEntries);
        stats.put("cacheTimestamps", cacheTimestamps.size());
        stats.put("maxCacheSize", MAX_CACHE_SIZE);
        stats.put("cacheExpiryMs", CACHE_EXPIRY_MS);
        
        return stats;
    }
    
    /**
     * Warm up cache with common lookups (optional optimization).
     */
    public void warmUpCache(List<ValidationRuleEntity> referentialRules, List<String> commonValues) {
        log.info("Starting cache warm-up for {} referential rules with {} common values", 
                referentialRules.size(), commonValues.size());
        
        for (ValidationRuleEntity rule : referentialRules) {
            if (rule.getRuleType() == ValidationRuleEntity.RuleType.REFERENTIAL_INTEGRITY) {
                for (String value : commonValues) {
                    try {
                        checkReferentialIntegrity(value, rule);
                    } catch (Exception e) {
                        log.debug("Error during cache warm-up for rule {} and value {}: {}", 
                                 rule.getRuleId(), value, e.getMessage());
                    }
                }
            }
        }
        
        log.info("Cache warm-up completed");
    }
}