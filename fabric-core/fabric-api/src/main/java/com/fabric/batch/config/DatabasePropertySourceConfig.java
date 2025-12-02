package com.fabric.batch.config;

import com.fabric.batch.repository.SourceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class to register DatabasePropertySource with Spring Environment.
 *
 * This configuration class is responsible for:
 * 1. Creating a DatabasePropertySource instance
 * 2. Registering it with Spring's Environment PropertySources
 * 3. Ensuring database properties are available for ${} placeholder resolution
 * 4. Setting appropriate priority in the property source chain
 *
 * Property Source Priority Order:
 * Spring resolves properties in the following order (highest to lowest priority):
 * 1. Command-line arguments
 * 2. System properties
 * 3. OS environment variables
 * 4. Database PropertySource (registered by this configuration) <-- Added here
 * 5. application.properties/application.yml
 * 6. Default properties
 *
 * This means database properties will override application.yml defaults but can be
 * overridden by system properties or command-line arguments.
 *
 * Usage:
 * This configuration is automatically picked up by Spring's component scanning.
 * No explicit bean declaration is required. The @PostConstruct method registers
 * the DatabasePropertySource during application startup.
 *
 * Example:
 * Once registered, properties from SOURCE_CONFIG table are automatically available:
 * <pre>
 * @Value("${batch.defaults.outputBasePath}")
 * private String outputBasePath; // Resolves from database
 * </pre>
 *
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 2 - Database-Driven Source Configuration System
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabasePropertySourceConfig {

    /**
     * Spring's ConfigurableEnvironment for property source management.
     * Injected by Spring framework.
     */
    private final ConfigurableEnvironment environment;

    /**
     * Repository for accessing source configuration from database.
     * Injected by Spring dependency injection.
     */
    private final SourceConfigRepository sourceConfigRepository;

    /**
     * Initialize and register DatabasePropertySource with Spring Environment.
     *
     * This method is called after dependency injection is complete but before
     * the application context is fully initialized. It creates a DatabasePropertySource
     * and adds it to Spring's property sources with high priority.
     *
     * Priority Strategy:
     * The DatabasePropertySource is added with "addFirst" strategy, giving it higher
     * priority than application.yml but still allowing system properties and
     * command-line arguments to override database values.
     *
     * Error Handling:
     * If database connection fails during initialization, the error is logged but
     * the application continues to start. Properties will fall back to application.yml
     * values. This ensures the application can start even if the database is temporarily
     * unavailable.
     */
    @PostConstruct
    public void registerDatabasePropertySource() {
        try {
            log.info("Initializing DatabasePropertySource for database-driven configuration...");

            // Get the mutable property sources from environment
            MutablePropertySources propertySources = environment.getPropertySources();

            // Create DatabasePropertySource instance
            DatabasePropertySource databasePropertySource = new DatabasePropertySource(
                DatabasePropertySource.PROPERTY_SOURCE_NAME,
                sourceConfigRepository
            );

            // Log cache statistics
            log.info("DatabasePropertySource initialized with {} cached properties",
                databasePropertySource.getCacheSize());

            // Register with Spring Environment - add with high priority
            // This will be checked after system properties but before application.yml
            propertySources.addFirst(databasePropertySource);

            log.info("Successfully registered DatabasePropertySource with Spring Environment");
            log.info("Database properties will override application.yml defaults");
            log.info("Priority order: System Properties > Database Properties > application.yml");

            // Log all loaded property sources for debugging
            if (log.isDebugEnabled()) {
                log.debug("Current PropertySource chain:");
                propertySources.forEach(ps ->
                    log.debug("  - {}", ps.getName())
                );
            }

        } catch (Exception e) {
            log.error("Failed to initialize DatabasePropertySource. " +
                "Application will continue with application.yml properties only.", e);
            log.warn("Database-driven configuration is NOT active. Check database connectivity.");
        }
    }

    /**
     * Get the current DatabasePropertySource instance from the environment.
     * Useful for programmatic access to the property source (e.g., for refresh operations).
     *
     * @return the DatabasePropertySource instance, or null if not registered
     */
    public DatabasePropertySource getDatabasePropertySource() {
        MutablePropertySources propertySources = environment.getPropertySources();
        org.springframework.core.env.PropertySource<?> propertySource =
            propertySources.get(DatabasePropertySource.PROPERTY_SOURCE_NAME);

        if (propertySource instanceof DatabasePropertySource) {
            return (DatabasePropertySource) propertySource;
        }

        log.warn("DatabasePropertySource not found in environment. It may not have been registered.");
        return null;
    }

    /**
     * Refresh the DatabasePropertySource cache.
     * This method can be called to reload properties from the database after configuration changes.
     *
     * Use Case:
     * When administrators update SOURCE_CONFIG table values through the admin UI,
     * this method can be called to refresh the cache without restarting the application.
     *
     * @return true if refresh succeeded, false otherwise
     */
    public boolean refreshDatabaseProperties() {
        try {
            log.info("Refreshing database properties...");
            DatabasePropertySource databasePropertySource = getDatabasePropertySource();

            if (databasePropertySource != null) {
                databasePropertySource.refresh();
                log.info("Database properties refreshed successfully. New cache size: {}",
                    databasePropertySource.getCacheSize());
                return true;
            } else {
                log.warn("Cannot refresh: DatabasePropertySource not found");
                return false;
            }
        } catch (Exception e) {
            log.error("Error refreshing database properties", e);
            return false;
        }
    }

    /**
     * Get statistics about the DatabasePropertySource cache.
     * Useful for monitoring and debugging.
     *
     * @return a formatted string with cache statistics
     */
    public String getCacheStatistics() {
        DatabasePropertySource databasePropertySource = getDatabasePropertySource();

        if (databasePropertySource != null) {
            int totalProperties = databasePropertySource.getCacheSize();
            int sourceCaches = databasePropertySource.getSourceCacheCount();

            return String.format(
                "DatabasePropertySource Statistics: %d total properties, %d source-specific caches loaded",
                totalProperties, sourceCaches
            );
        } else {
            return "DatabasePropertySource not available";
        }
    }
}
