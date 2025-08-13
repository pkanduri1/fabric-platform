package com.fabric.watcher.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing file watcher configurations.
 * Handles validation, default values, and configuration access.
 */
@Service
@EnableConfigurationProperties(FileWatcherProperties.class)
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    private final FileWatcherProperties properties;
    private List<WatchConfig> validatedConfigs;

    public ConfigurationService(FileWatcherProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validateAndInitialize() {
        logger.info("Initializing file watcher configurations...");
        
        this.validatedConfigs = new ArrayList<>();
        
        if (properties.getWatchConfigs().isEmpty()) {
            logger.warn("No watch configurations found. Service will start but no folders will be monitored.");
            return;
        }

        for (Map.Entry<String, WatchConfigProperties> entry : properties.getWatchConfigs().entrySet()) {
            String configKey = entry.getKey();
            WatchConfigProperties configProps = entry.getValue();
            
            try {
                WatchConfig config = validateAndCreateConfig(configKey, configProps);
                validatedConfigs.add(config);
                logger.info("Successfully validated configuration: {}", config.getName());
            } catch (Exception e) {
                logger.error("Failed to validate configuration '{}': {}", configKey, e.getMessage());
                // Continue with other configurations rather than failing completely
            }
        }
        
        logger.info("Initialized {} valid watch configurations out of {} total", 
                   validatedConfigs.size(), properties.getWatchConfigs().size());
    }

    /**
     * Validates a single watch configuration and creates a WatchConfig object.
     */
    private WatchConfig validateAndCreateConfig(String configKey, WatchConfigProperties configProps) {
        // Set name from key if not explicitly set
        if (configProps.getName() == null || configProps.getName().trim().isEmpty()) {
            configProps.setName(configKey);
        }

        // Validate required fields
        if (configProps.getProcessorType() == null || configProps.getProcessorType().trim().isEmpty()) {
            throw new IllegalArgumentException("Processor type is required for configuration: " + configKey);
        }

        if (configProps.getWatchFolder() == null || configProps.getWatchFolder().trim().isEmpty()) {
            throw new IllegalArgumentException("Watch folder is required for configuration: " + configKey);
        }

        if (configProps.getFilePatterns() == null || configProps.getFilePatterns().isEmpty()) {
            logger.warn("No file patterns specified for configuration '{}', defaulting to ['*']", configKey);
            configProps.setFilePatterns(List.of("*"));
        }

        // Validate polling interval
        if (configProps.getPollingInterval() < 1000) {
            logger.warn("Polling interval for configuration '{}' is less than 1000ms, setting to 1000ms", configKey);
            configProps.setPollingInterval(1000);
        }

        WatchConfig config = configProps.toWatchConfig();
        
        // Validate paths exist or can be created
        validateOrCreateDirectory(config.getWatchFolder(), "watch folder for " + configKey);
        validateOrCreateDirectory(config.getCompletedFolder(), "completed folder for " + configKey);
        validateOrCreateDirectory(config.getErrorFolder(), "error folder for " + configKey);

        return config;
    }

    /**
     * Validates that a directory exists or can be created.
     */
    private void validateOrCreateDirectory(Path directory, String description) {
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                logger.info("Created directory: {} ({})", directory, description);
            } else if (!Files.isDirectory(directory)) {
                throw new IllegalArgumentException("Path exists but is not a directory: " + directory);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create or access " + description + " at " + directory + ": " + e.getMessage());
        }
    }

    /**
     * Returns all validated watch configurations.
     */
    public List<WatchConfig> getAllConfigurations() {
        return new ArrayList<>(validatedConfigs);
    }

    /**
     * Returns all enabled watch configurations.
     */
    public List<WatchConfig> getEnabledConfigurations() {
        return validatedConfigs.stream()
                .filter(WatchConfig::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Returns a specific configuration by name.
     */
    public WatchConfig getConfiguration(String name) {
        return validatedConfigs.stream()
                .filter(config -> config.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns global configuration settings.
     */
    public FileWatcherProperties.GlobalConfig getGlobalConfig() {
        return properties.getGlobal();
    }

    /**
     * Returns the number of valid configurations.
     */
    public int getConfigurationCount() {
        return validatedConfigs.size();
    }

    /**
     * Returns the number of enabled configurations.
     */
    public int getEnabledConfigurationCount() {
        return (int) validatedConfigs.stream().filter(WatchConfig::isEnabled).count();
    }
}