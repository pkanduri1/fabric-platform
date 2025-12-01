# Database-Driven Source Configuration - Usage Guide

## Overview

The Fabric Platform now supports **database-driven source configuration** where each source system (HR, ENCORE, PAYROLL, etc.) can have its own configuration values stored in the `SOURCE_CONFIG` table. This eliminates hardcoded paths and enables multi-tenant configuration management.

## Architecture

### Components

1. **SOURCE_CONFIG Table** - Stores configuration key-value pairs per source system
2. **DatabasePropertySource** - Spring PropertySource that reads from the database
3. **SourceContext** - ThreadLocal context manager for tracking current source
4. **SourceConfigRepository** - Data access layer for configuration CRUD operations

### Property Resolution Flow

```
YAML Config: ${batch.defaults.outputBasePath}/transactions
        ↓
SourceContext: "HR" (set in ThreadLocal)
        ↓
DatabasePropertySource: Lookup "HR" + "batch.defaults.outputBasePath"
        ↓
SOURCE_CONFIG Table: Returns "/data/output/hr"
        ↓
Final Result: "/data/output/hr/transactions"
```

## Database Schema

### SOURCE_CONFIG Table

```sql
CREATE TABLE SOURCE_CONFIG (
    CONFIG_ID          VARCHAR2(50) PRIMARY KEY,
    SOURCE_CODE        VARCHAR2(50) NOT NULL,     -- e.g., 'HR', 'ENCORE'
    CONFIG_KEY         VARCHAR2(200) NOT NULL,    -- e.g., 'batch.defaults.outputBasePath'
    CONFIG_VALUE       VARCHAR2(1000),            -- e.g., '/data/output/hr'
    DESCRIPTION        VARCHAR2(500),
    CREATED_DATE       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    MODIFIED_DATE      TIMESTAMP,
    MODIFIED_BY        VARCHAR2(100),
    ACTIVE             CHAR(1) DEFAULT 'Y',
    CONSTRAINT UK_SOURCE_CONFIG UNIQUE (SOURCE_CODE, CONFIG_KEY)
);
```

### Sample Data (Already Loaded)

| CONFIG_ID | SOURCE_CODE | CONFIG_KEY | CONFIG_VALUE | ACTIVE |
|-----------|-------------|------------|--------------|--------|
| cfg_hr_001 | HR | batch.defaults.outputBasePath | /data/output/hr | Y |
| cfg_hr_002 | HR | batch.defaults.inputBasePath | /data/input/hr | Y |
| cfg_hr_003 | HR | batch.defaults.archivePath | /data/archive/hr | Y |
| cfg_encore_001 | ENCORE | batch.defaults.outputBasePath | /data/output/encore | Y |
| cfg_encore_002 | ENCORE | batch.defaults.inputBasePath | /data/input/encore | Y |
| cfg_encore_003 | ENCORE | batch.defaults.archivePath | /data/archive/encore | Y |
| cfg_encore_004 | ENCORE | batch.defaults.errorPath | /data/error/encore | Y |
| cfg_payroll_001 | PAYROLL | batch.defaults.outputBasePath | /data/output/payroll | Y |
| cfg_payroll_002 | PAYROLL | batch.defaults.inputBasePath | /data/input/payroll | Y |

## Usage Examples

### 1. Basic Usage in Code

```java
import com.truist.batch.context.SourceContext;

public class BatchJobExecutor {

    public void executeJob(String sourceCode) {
        try {
            // Set the source context
            SourceContext.setCurrentSource(sourceCode);

            // Now all property placeholders will use this source's configuration
            // Example: ${batch.defaults.outputBasePath} will resolve to source-specific path

            executeJobLogic();

        } finally {
            // ALWAYS clear context in finally block to prevent memory leaks
            SourceContext.clear();
        }
    }
}
```

### 2. Using Helper Method

```java
import com.truist.batch.context.SourceContext;

public class BatchJobExecutor {

    public void executeJob(String sourceCode) {
        SourceContext.executeWithSource(sourceCode, () -> {
            // Code in this block has the source context set
            executeJobLogic();
            return null;
        });
        // Context automatically cleared after execution
    }
}
```

### 3. In YAML Configuration Files

```yaml
# atoctran-hr-config.yml
batch:
  output:
    # This will resolve to: /data/output/hr/atoctran
    path: ${batch.defaults.outputBasePath}/atoctran

  input:
    # This will resolve to: /data/input/hr/source
    path: ${batch.defaults.inputBasePath}/source

  archive:
    # This will resolve to: /data/archive/hr
    path: ${batch.defaults.archivePath}
```

### 4. Programmatic Property Access

```java
import org.springframework.core.env.Environment;
import com.truist.batch.context.SourceContext;

@Service
public class ConfigService {

    @Autowired
    private Environment environment;

    public String getOutputPath(String sourceCode) {
        try {
            SourceContext.setCurrentSource(sourceCode);

            // Get the source-specific output path
            String outputPath = environment.getProperty("batch.defaults.outputBasePath");
            // For HR: returns "/data/output/hr"
            // For ENCORE: returns "/data/output/encore"

            return outputPath;

        } finally {
            SourceContext.clear();
        }
    }
}
```

### 5. With Job Metadata

```java
import com.truist.batch.context.SourceContext;

public class BatchJobExecutor {

    public void executeJob(String sourceCode, String jobId, String batchDate) {
        try {
            // Set source context with metadata
            SourceContext.setCurrentSource(sourceCode);
            SourceContext.setMetadata("jobId", jobId);
            SourceContext.setMetadata("batchDate", batchDate);

            // Execute job
            executeJobLogic();

        } finally {
            SourceContext.clear();
        }
    }

    public void executeJobLogic() {
        // Can retrieve metadata anywhere in the call stack
        String jobId = (String) SourceContext.getMetadata("jobId");
        String batchDate = (String) SourceContext.getMetadata("batchDate");

        log.info("Executing job {} for batch date {}", jobId, batchDate);
    }
}
```

## Adding New Configuration

### Method 1: Direct SQL Insert

```sql
INSERT INTO SOURCE_CONFIG
(CONFIG_ID, SOURCE_CODE, CONFIG_KEY, CONFIG_VALUE, DESCRIPTION, CREATED_DATE, MODIFIED_BY, ACTIVE)
VALUES
('cfg_newapp_001', 'NEWAPP', 'batch.defaults.outputBasePath', '/data/output/newapp',
 'NEWAPP output path', CURRENT_TIMESTAMP, 'admin', 'Y');
```

### Method 2: Using Repository

```java
import com.truist.batch.repository.SourceConfigRepository;
import com.truist.batch.entity.SourceConfigEntity;

@Service
public class ConfigManagementService {

    @Autowired
    private SourceConfigRepository sourceConfigRepository;

    @Autowired
    private DatabasePropertySourceConfig propertySourceConfig;

    public void addNewSourceConfig(String sourceCode, String key, String value, String description) {
        SourceConfigEntity config = SourceConfigEntity.builder()
                .sourceCode(sourceCode)
                .configKey(key)
                .configValue(value)
                .description(description)
                .modifiedBy("system")
                .active(true)
                .build();

        sourceConfigRepository.create(config);

        // Refresh the property source cache
        propertySourceConfig.refreshPropertySource();

        log.info("Added new configuration for source: {}, key: {}", sourceCode, key);
    }
}
```

## Refreshing Configuration at Runtime

Configuration changes are automatically loaded on application startup. To refresh configuration without restarting:

```java
import com.truist.batch.config.DatabasePropertySourceConfig;

@RestController
@RequestMapping("/api/admin/config")
public class ConfigAdminController {

    @Autowired
    private DatabasePropertySourceConfig propertySourceConfig;

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> refreshConfig() {
        propertySourceConfig.refreshPropertySource();
        return ResponseEntity.ok("Configuration refreshed successfully");
    }
}
```

## Integration with Batch Job Execution

### Example: Manual Job Execution Service

```java
import com.truist.batch.context.SourceContext;
import com.truist.batch.service.BatchJobExecutionService;

@Service
public class ManualJobExecutionService {

    @Autowired
    private BatchJobExecutionService batchExecutionService;

    @Autowired
    private ManualJobConfigRepository jobConfigRepository;

    public JobExecutionResult executeManualJob(String configId, Map<String, Object> parameters) {
        ManualJobConfig jobConfig = jobConfigRepository.findById(configId);

        // Extract source code from job configuration
        String sourceCode = jobConfig.getSourceCode(); // e.g., "HR"

        try {
            // Set source context for property resolution
            SourceContext.setCurrentSource(sourceCode);
            SourceContext.setMetadata("configId", configId);
            SourceContext.setMetadata("batchDate", parameters.get("batchDate"));

            // Execute the batch job - all property placeholders now use source-specific values
            JobExecutionResult result = batchExecutionService.execute(jobConfig, parameters);

            return result;

        } finally {
            SourceContext.clear();
        }
    }
}
```

## Best Practices

### 1. Always Use Try-Finally

```java
try {
    SourceContext.setCurrentSource(sourceCode);
    // Your code here
} finally {
    SourceContext.clear(); // ALWAYS clear to prevent memory leaks
}
```

### 2. Use Helper Methods for Cleaner Code

```java
// Instead of manual try-finally
SourceContext.executeWithSource("HR", () -> {
    doWork();
    return result;
});
```

### 3. Property Naming Convention

- Use hierarchical dot notation: `batch.defaults.outputBasePath`
- Start with domain: `batch.`, `app.`, `system.`
- Be specific: `batch.hr.outputPath` vs generic `outputPath`

### 4. Document Configuration Keys

Create a central registry of all configuration keys:

```java
public class ConfigKeys {
    public static final String OUTPUT_BASE_PATH = "batch.defaults.outputBasePath";
    public static final String INPUT_BASE_PATH = "batch.defaults.inputBasePath";
    public static final String ARCHIVE_PATH = "batch.defaults.archivePath";
    public static final String ERROR_PATH = "batch.defaults.errorPath";
}
```

### 5. Validate Configuration on Startup

```java
@Component
public class ConfigurationValidator {

    @Autowired
    private SourceConfigRepository configRepository;

    @PostConstruct
    public void validateConfiguration() {
        List<String> requiredSources = Arrays.asList("HR", "ENCORE", "PAYROLL");
        List<String> requiredKeys = Arrays.asList(
            "batch.defaults.outputBasePath",
            "batch.defaults.inputBasePath"
        );

        for (String source : requiredSources) {
            for (String key : requiredKeys) {
                if (!configRepository.existsBySourceAndKey(source, key)) {
                    log.warn("Missing configuration: source={}, key={}", source, key);
                }
            }
        }
    }
}
```

## Troubleshooting

### Property Not Resolving

**Symptom**: `${batch.defaults.outputBasePath}` appears literally in output instead of being resolved

**Solutions**:
1. Verify SOURCE_CONFIG table has the entry:
   ```sql
   SELECT * FROM SOURCE_CONFIG
   WHERE SOURCE_CODE = 'HR'
   AND CONFIG_KEY = 'batch.defaults.outputBasePath';
   ```

2. Check if SourceContext is set:
   ```java
   String currentSource = SourceContext.getCurrentSource();
   log.info("Current source: {}", currentSource); // Should not be null
   ```

3. Verify DatabasePropertySource is registered:
   ```java
   @Autowired
   private Environment environment;

   PropertySource<?> ps = ((ConfigurableEnvironment) environment)
       .getPropertySources()
       .get("databasePropertySource");
   log.info("DatabasePropertySource registered: {}", ps != null);
   ```

4. Refresh the property source cache:
   ```java
   propertySourceConfig.refreshPropertySource();
   ```

### Memory Leaks from ThreadLocal

**Symptom**: Application memory grows over time

**Solution**: Always clear SourceContext in finally block:
```java
try {
    SourceContext.setCurrentSource("HR");
    // work
} finally {
    SourceContext.clear(); // CRITICAL
}
```

### Configuration Not Updating

**Symptom**: Changed configuration values in database but application still uses old values

**Solution**: Call refresh endpoint or restart application:
```bash
curl -X POST http://localhost:8080/api/admin/config/refresh \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Monitoring

### Check Current Configuration

```java
@RestController
@RequestMapping("/api/admin/config")
public class ConfigMonitoringController {

    @Autowired
    private SourceConfigRepository configRepository;

    @GetMapping("/source/{sourceCode}")
    public Map<String, String> getSourceConfig(@PathVariable String sourceCode) {
        List<SourceConfigEntity> configs = configRepository.findBySourceCode(sourceCode);
        return configs.stream()
            .filter(SourceConfigEntity::isActive)
            .collect(Collectors.toMap(
                SourceConfigEntity::getConfigKey,
                SourceConfigEntity::getConfigValue
            ));
    }
}
```

### Cache Statistics

```java
@GetMapping("/cache/stats")
public Map<String, Object> getCacheStats() {
    return propertySourceConfig.getCacheStatistics();
}
```

## Security Considerations

1. **Access Control**: Restrict SOURCE_CONFIG table updates to authorized users
2. **Audit Trail**: All changes tracked with MODIFIED_BY and MODIFIED_DATE
3. **Validation**: Validate configuration values before insertion
4. **Encryption**: Consider encrypting sensitive configuration values

## Migration from Hardcoded Values

### Before (Hardcoded):

```yaml
# application.yml
batch:
  defaults:
    outputBasePath: /data/output/hr  # Hardcoded per deployment
```

### After (Database-Driven):

```yaml
# application.yml
batch:
  defaults:
    outputBasePath: ${batch.defaults.outputBasePath}  # Resolved from database
```

```sql
-- Configuration in database
INSERT INTO SOURCE_CONFIG VALUES
('cfg_hr_001', 'HR', 'batch.defaults.outputBasePath', '/data/output/hr', ..., 'Y');
```

## Performance Considerations

- **Caching**: All properties cached in memory on startup
- **Lazy Loading**: Cache loaded on first access
- **Refresh**: Manual refresh required for runtime updates
- **Query Optimization**: Indexed on SOURCE_CODE and ACTIVE for fast lookups

## Summary

The database-driven configuration system provides:

✅ **Flexibility** - Change configuration without code deployment
✅ **Multi-Tenancy** - Isolated configuration per source system
✅ **Audit Trail** - Full history of configuration changes
✅ **Performance** - In-memory caching for fast access
✅ **Spring Integration** - Seamless property placeholder resolution
✅ **Enterprise-Ready** - SOX compliant with security controls

For questions or support, contact the Fabric Platform development team.
