package com.truist.batch.config;

import com.truist.batch.context.SourceContext;
import com.truist.batch.repository.SourceConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.PropertySource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Spring PropertySource that loads configuration properties from the database.
 *
 * This PropertySource implementation enables database-driven configuration by loading
 * properties from the SOURCE_CONFIG table. It supports source-specific property resolution
 * using the SourceContext to determine which source system's configuration to use.
 *
 * Key Features:
 * - Database-driven property resolution
 * - Source-specific configuration isolation
 * - In-memory caching for performance
 * - Integration with Spring's property resolution mechanism
 * - Support for ${} placeholder syntax in YAML/properties files
 *
 * Property Resolution Flow:
 * 1. Application requests property (e.g., ${batch.defaults.outputBasePath})
 * 2. SourceContext provides current source code (e.g., "HR")
 * 3. DatabasePropertySource checks cache for source-specific value
 * 4. If not cached, loads from database and caches result
 * 5. Returns source-specific value (e.g., "/data/output/hr")
 *
 * Usage Example:
 * In YAML configuration:
 * <pre>
 * batch:
 *   output:
 *     path: ${batch.defaults.outputBasePath}/transactions
 * </pre>
 *
 * When SourceContext is set to "HR", the property resolves to:
 * "/data/output/hr/transactions"
 *
 * When SourceContext is set to "ENCORE", the property resolves to:
 * "/data/output/encore/transactions"
 *
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 2 - Database-Driven Source Configuration System
 */
@Slf4j
public class DatabasePropertySource extends PropertySource<SourceConfigRepository> {

    /**
     * Name of this PropertySource for Spring Environment registration.
     */
    public static final String PROPERTY_SOURCE_NAME = "databasePropertySource";

    /**
     * In-memory cache of all active properties.
     * Key: property name (config key)
     * Value: property value (config value)
     * This cache is populated on initialization and used for fast property lookups.
     */
    private final Map<String, String> propertiesCache;

    /**
     * Source-specific property caches.
     * Key: source code (e.g., "HR", "ENCORE")
     * Value: Map of property names to values for that source
     * Enables efficient source-specific property resolution.
     */
    private final Map<String, Map<String, String>> sourceSpecificCache;

    /**
     * Repository for accessing source configuration from database.
     */
    private final SourceConfigRepository repository;

    /**
     * Constructor for DatabasePropertySource.
     *
     * @param name the name of this PropertySource
     * @param repository the SourceConfigRepository for database access
     */
    public DatabasePropertySource(String name, SourceConfigRepository repository) {
        super(name, repository);
        this.repository = repository;
        this.propertiesCache = new ConcurrentHashMap<>();
        this.sourceSpecificCache = new ConcurrentHashMap<>();
        loadAllProperties();
    }

    /**
     * Load all active properties from database into cache.
     * This method is called during initialization to populate the cache.
     * Subsequent property lookups use the cached values for performance.
     */
    private void loadAllProperties() {
        try {
            log.info("Loading all active properties from database into cache...");
            Map<String, String> dbProperties = repository.getAllActivePropertiesMap();
            propertiesCache.putAll(dbProperties);
            log.info("Successfully loaded {} properties from database", dbProperties.size());

            // Log loaded properties for debugging
            if (log.isDebugEnabled()) {
                dbProperties.forEach((key, value) ->
                    log.debug("Loaded property: {} = {}", key, value));
            }
        } catch (Exception e) {
            log.error("Error loading properties from database. Properties will be loaded on-demand.", e);
        }
    }

    /**
     * Get source-specific properties for a given source code.
     * Loads from database if not already cached.
     *
     * @param sourceCode the source system code
     * @return Map of properties for the specified source
     */
    private Map<String, String> getSourceProperties(String sourceCode) {
        return sourceSpecificCache.computeIfAbsent(sourceCode, code -> {
            log.debug("Loading properties for source: {}", code);
            return repository.getActivePropertiesMapForSource(code);
        });
    }

    /**
     * Get a property value by name.
     * Implements Spring PropertySource contract.
     *
     * Property Resolution Strategy:
     * 1. Check if SourceContext has a current source set
     * 2. If yes, look up property in source-specific cache
     * 3. If no source context, look up property in global cache
     * 4. Return null if property not found (Spring will try other PropertySources)
     *
     * @param name the property name to look up
     * @return the property value, or null if not found
     */
    @Override
    public Object getProperty(String name) {
        try {
            // Get current source from SourceContext
            String currentSource = SourceContext.getCurrentSource();

            if (currentSource != null && !currentSource.trim().isEmpty()) {
                // Source-specific property lookup
                Map<String, String> sourceProps = getSourceProperties(currentSource);
                String value = sourceProps.get(name);

                if (value != null) {
                    log.debug("Resolved property '{}' for source '{}': {}", name, currentSource, value);
                    return value;
                } else {
                    log.debug("Property '{}' not found for source '{}'", name, currentSource);
                }
            }

            // Fallback to global property lookup
            String value = propertiesCache.get(name);
            if (value != null) {
                log.debug("Resolved global property '{}': {}", name, value);
                return value;
            }

            // Property not found in database
            log.trace("Property '{}' not found in database PropertySource", name);
            return null;

        } catch (Exception e) {
            log.error("Error retrieving property '{}' from database PropertySource", name, e);
            return null;
        }
    }

    /**
     * Check if this PropertySource contains a specific property.
     *
     * @param name the property name to check
     * @return true if the property exists in cache, false otherwise
     */
    @Override
    public boolean containsProperty(String name) {
        String currentSource = SourceContext.getCurrentSource();

        if (currentSource != null && !currentSource.trim().isEmpty()) {
            Map<String, String> sourceProps = getSourceProperties(currentSource);
            if (sourceProps.containsKey(name)) {
                return true;
            }
        }

        return propertiesCache.containsKey(name);
    }

    /**
     * Refresh the properties cache from database.
     * This method can be called to reload properties after database changes.
     */
    public void refresh() {
        log.info("Refreshing database PropertySource cache...");
        propertiesCache.clear();
        sourceSpecificCache.clear();
        loadAllProperties();
        log.info("Database PropertySource cache refreshed successfully");
    }

    /**
     * Get the current cache size.
     *
     * @return number of cached properties
     */
    public int getCacheSize() {
        return propertiesCache.size();
    }

    /**
     * Get the number of source-specific caches loaded.
     *
     * @return number of source-specific caches
     */
    public int getSourceCacheCount() {
        return sourceSpecificCache.size();
    }

    /**
     * Get all cached properties (for debugging/monitoring).
     *
     * @return unmodifiable view of cached properties
     */
    public Map<String, String> getCachedProperties() {
        return Map.copyOf(propertiesCache);
    }

    /**
     * Get cached properties for a specific source (for debugging/monitoring).
     *
     * @param sourceCode the source system code
     * @return unmodifiable view of source-specific cached properties
     */
    public Map<String, String> getCachedPropertiesForSource(String sourceCode) {
        Map<String, String> sourceProps = sourceSpecificCache.get(sourceCode);
        return sourceProps != null ? Map.copyOf(sourceProps) : Map.of();
    }

    /**
     * Clear cache for a specific source.
     * Useful when source-specific configuration changes.
     *
     * @param sourceCode the source system code
     */
    public void clearSourceCache(String sourceCode) {
        log.info("Clearing cache for source: {}", sourceCode);
        sourceSpecificCache.remove(sourceCode);
    }
}
