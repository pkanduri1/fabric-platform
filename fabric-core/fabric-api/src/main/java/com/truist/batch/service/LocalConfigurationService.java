package com.truist.batch.service;

import com.truist.batch.model.FieldMappingConfig;
import com.truist.batch.model.JobConfig;
import com.truist.batch.model.SourceField;
import com.truist.batch.model.SourceSystem;
import com.truist.batch.model.TestResult;
import com.truist.batch.model.ValidationResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Stub implementation of ConfigurationService for local development.
 * This allows the backend to start successfully for manual job configuration testing.
 */
@Service
@Profile("local")
public class LocalConfigurationService implements ConfigurationService {

    @Override
    public List<SourceSystem> getAllSourceSystems() {
        List<SourceSystem> systems = new ArrayList<>();
        systems.add(new SourceSystem("hr", "HR System", "ORACLE", "Human Resources", true, 2, LocalDateTime.now(), null));
        systems.add(new SourceSystem("dda", "DDA System", "ORACLE", "Demand Deposit Accounts", true, 3, LocalDateTime.now(), null));
        systems.add(new SourceSystem("shaw", "Shaw System", "ORACLE", "Shaw Cable Data", true, 1, LocalDateTime.now(), null));
        return systems;
    }

    @Override
    public SourceSystem getSourceSystem(String systemId) {
        switch (systemId) {
            case "hr":
                return new SourceSystem("hr", "HR System", "ORACLE", "Human Resources", true, 2, LocalDateTime.now(), null);
            case "dda":
                return new SourceSystem("dda", "DDA System", "ORACLE", "Demand Deposit Accounts", true, 3, LocalDateTime.now(), null);
            case "shaw":
                return new SourceSystem("shaw", "Shaw System", "ORACLE", "Shaw Cable Data", true, 1, LocalDateTime.now(), null);
            default:
                return null;
        }
    }

    @Override
    public List<JobConfig> getJobsForSystem(String systemId) {
        List<JobConfig> jobs = new ArrayList<>();
        switch (systemId) {
            case "hr":
                jobs.add(new JobConfig("hr-p327", systemId, "p327", "P327 HR Processing", 
                    "/data/hr", "/output/hr", "SELECT * FROM hr_staging", true, 
                    LocalDateTime.now(), Arrays.asList("default", "credit", "debit")));
                break;
            case "dda":
                jobs.add(new JobConfig("dda-p327", systemId, "p327", "P327 DDA Processing", 
                    "/data/dda", "/output/dda", "SELECT * FROM dda_staging", true, 
                    LocalDateTime.now(), Arrays.asList("default", "savings", "checking")));
                break;
            case "shaw":
                jobs.add(new JobConfig("shaw-p327", systemId, "p327", "P327 Shaw Processing", 
                    "/data/shaw", "/output/shaw", "SELECT * FROM shaw_staging", true, 
                    LocalDateTime.now(), Arrays.asList("default")));
                break;
        }
        return jobs;
    }

    @Override
    public List<SourceField> getSourceFields(String systemId, String jobName) {
        List<SourceField> fields = new ArrayList<>();
        fields.add(new SourceField("acct_num", "STRING", "Account number", false, 18, "staging_table", "acct_num"));
        fields.add(new SourceField("location_code", "STRING", "Location code", true, 6, "staging_table", "location_code"));
        fields.add(new SourceField("balance_amt", "NUMBER", "Balance amount", true, 12, "staging_table", "balance_amt"));
        fields.add(new SourceField("last_payment_date", "DATE", "Last payment date", true, 8, "staging_table", "last_payment_date"));
        fields.add(new SourceField("status_code", "STRING", "Account status", true, 2, "staging_table", "status_code"));
        return fields;
    }

    @Override
    public FieldMappingConfig getFieldMappings(String sourceSystem, String jobName) {
        return getFieldMappings(sourceSystem, jobName, "default");
    }

    @Override
    public FieldMappingConfig getFieldMappings(String sourceSystem, String jobName, String transactionType) {
        FieldMappingConfig config = new FieldMappingConfig();
        config.setSourceSystem(sourceSystem);
        config.setJobName(jobName);
        config.setTransactionType(transactionType);
        config.setFieldMappings(new ArrayList<>());
        config.setLastModified(LocalDateTime.now());
        config.setVersion(1);
        return config;
    }

    @Override
    public String saveConfiguration(FieldMappingConfig config) {
        return "local-config-" + UUID.randomUUID().toString();
    }

    @Override
    public ValidationResult validateConfiguration(FieldMappingConfig config) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        result.setErrors(new ArrayList<>());
        return result;
    }

    @Override
    public String generateYaml(FieldMappingConfig config) {
        return "# Local stub YAML configuration\nversion: 1.0\nsourceSystem: " + config.getSourceSystem() + "\njobName: " + config.getJobName() + "\n";
    }

    @Override
    public List<String> generatePreview(FieldMappingConfig mapping, List<Map<String, Object>> sampleData) {
        return Arrays.asList("Preview line 1 (local stub)", "Preview line 2 (local stub)");
    }

    @Override
    public TestResult testConfiguration(String sourceSystem, String jobName) {
        return new TestResult(true, "Local test passed", 
            Arrays.asList("Test output line 1", "Test output line 2"), 100L);
    }

    @Override
    public List<FieldMappingConfig> getAllTransactionTypesForJob(String sourceSystem, String jobName) {
        List<FieldMappingConfig> configs = new ArrayList<>();
        configs.add(getFieldMappings(sourceSystem, jobName, "default"));
        return configs;
    }
}