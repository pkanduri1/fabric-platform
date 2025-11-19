package com.truist.batch.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobLocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import com.truist.batch.model.FieldMappingConfig;
import com.truist.batch.service.ConfigurationService;
import com.truist.batch.service.TemplateService;
import com.truist.batch.dao.BatchConfigurationDao;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v2/diagnostic")
@CrossOrigin(origins = { "http://localhost:3000", "https://localhost:3000" }, allowedHeaders = "*", methods = {
                RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS })
public class DiagnosticController {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private ConfigurationService configurationService;

        @Autowired
        private TemplateService templateService;

        @Autowired
        private BatchConfigurationDao batchConfigurationDao;

        /**
         * Test Oracle connectivity and check database tables
         */
        @GetMapping("/oracle-status")
        public ResponseEntity<Map<String, Object>> getOracleStatus() {
                Map<String, Object> status = new HashMap<>();

                try {
                        // Test basic Oracle connectivity
                        String version = jdbcTemplate.queryForObject(
                                        "SELECT BANNER FROM V$VERSION WHERE BANNER LIKE 'Oracle%'",
                                        String.class);
                        status.put("oracle_connected", true);
                        status.put("oracle_version", version);

                        // Check current schema
                        String currentSchema = jdbcTemplate
                                        .queryForObject("SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM DUAL",
                                                        String.class);
                        status.put("current_schema", currentSchema);

                        // Check if MANUAL_JOB_CONFIG table exists
                        Integer manualJobConfigExists = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = 'MANUAL_JOB_CONFIG'",
                                        Integer.class);
                        status.put("manual_job_config_exists", manualJobConfigExists > 0);

                        // Check if FIELD_TEMPLATES table exists
                        Integer fieldTemplatesExists = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = 'FIELD_TEMPLATES'",
                                        Integer.class);
                        status.put("field_templates_exists", fieldTemplatesExists > 0);

                        // Count existing configurations
                        if (manualJobConfigExists > 0) {
                                Integer configCount = jdbcTemplate.queryForObject(
                                                "SELECT COUNT(*) FROM MANUAL_JOB_CONFIG",
                                                Integer.class);
                                status.put("config_count", configCount);

                                // Get recent configurations
                                List<Map<String, Object>> recentConfigs = jdbcTemplate.queryForList(
                                                "SELECT CONFIG_ID, JOB_NAME, SOURCE_SYSTEM, CREATED_BY, CREATED_DATE FROM MANUAL_JOB_CONFIG ORDER BY CREATED_DATE DESC");
                                status.put("recent_configs",
                                                recentConfigs.size() > 5 ? recentConfigs.subList(0, 5) : recentConfigs);
                        }

                        // Count existing field templates
                        if (fieldTemplatesExists > 0) {
                                Integer templateCount = jdbcTemplate.queryForObject(
                                                "SELECT COUNT(*) FROM FIELD_TEMPLATES",
                                                Integer.class);
                                status.put("template_count", templateCount);

                                // Get recent templates
                                List<Map<String, Object>> recentTemplates = jdbcTemplate.queryForList(
                                                "SELECT FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME FROM FIELD_TEMPLATES WHERE ROWNUM <= 5 ORDER BY CREATED_DATE DESC");
                                status.put("recent_templates", recentTemplates);
                        }

                        status.put("success", true);

                } catch (Exception e) {
                        log.error("Error checking Oracle status", e);
                        status.put("oracle_connected", false);
                        status.put("error", e.getMessage());
                        status.put("success", false);
                }

                return ResponseEntity.ok(status);
        }

        /**
         * Test the complete flow: create configuration from template and save it
         */
        @PostMapping("/test-template-to-config-save")
        public ResponseEntity<Map<String, Object>> testTemplateToConfigSave(
                        @RequestParam String fileType,
                        @RequestParam String transactionType,
                        @RequestParam String sourceSystem,
                        @RequestParam String jobName) {

                Map<String, Object> result = new HashMap<>();

                try {
                        log.info("🔍 Testing complete flow: template -> config -> save for {}/{}/{}/{}",
                                        fileType, transactionType, sourceSystem, jobName);

                        // Step 1: Create configuration from template (this is what the
                        // TemplateController does)
                        FieldMappingConfig config = templateService.createConfigurationFromTemplate(
                                        fileType, transactionType, sourceSystem, jobName, "diagnostic-test");

                        result.put("step1_template_to_config", "SUCCESS");
                        result.put("field_mappings_count",
                                        config.getFieldMappings() != null ? config.getFieldMappings().size() : 0);

                        // Step 2: Save the configuration to Oracle (this is the missing step!)
                        String savedConfigId = configurationService.saveConfiguration(config);

                        result.put("step2_save_to_oracle", "SUCCESS");
                        result.put("saved_config_id", savedConfigId);

                        // Step 3: Verify it was actually saved to Oracle
                        Integer verificationCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID = ?",
                                        Integer.class, savedConfigId);

                        result.put("step3_verification",
                                        verificationCount > 0 ? "FOUND_IN_ORACLE" : "NOT_FOUND_IN_ORACLE");
                        result.put("verification_count", verificationCount);

                        // Step 4: Retrieve the saved configuration to confirm
                        if (verificationCount > 0) {
                                Map<String, Object> savedConfig = jdbcTemplate.queryForMap(
                                                "SELECT CONFIG_ID, JOB_NAME, SOURCE_SYSTEM, CREATED_BY, CREATED_DATE FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID = ?",
                                                savedConfigId);
                                result.put("saved_config_details", savedConfig);
                        }

                        result.put("overall_success", true);
                        result.put("message",
                                        "Configuration successfully created from template and saved to Oracle database");

                } catch (Exception e) {
                        log.error("Error in test flow", e);
                        result.put("overall_success", false);
                        result.put("error", e.getMessage());
                        result.put("error_type", e.getClass().getSimpleName());
                }

                return ResponseEntity.ok(result);
        }

        /**
         * Test saving a simple configuration directly
         */
        @PostMapping("/test-direct-save")
        public ResponseEntity<Map<String, Object>> testDirectSave() {
                Map<String, Object> result = new HashMap<>();

                try {
                        // Create a minimal test configuration
                        FieldMappingConfig testConfig = new FieldMappingConfig();
                        testConfig.setSourceSystem("DIAGNOSTIC");
                        testConfig.setJobName("TEST_SAVE");
                        testConfig.setTransactionType("200");
                        testConfig.setDescription("Diagnostic test configuration");
                        testConfig.setVersion(1);

                        // Save it
                        String configId = configurationService.saveConfiguration(testConfig);

                        result.put("save_success", true);
                        result.put("config_id", configId);

                        // Verify in database
                        Integer count = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID = ?",
                                        Integer.class, configId);

                        result.put("found_in_database", count > 0);
                        result.put("verification_count", count);

                } catch (Exception e) {
                        log.error("Error in direct save test", e);
                        result.put("save_success", false);
                        result.put("error", e.getMessage());
                }

                return ResponseEntity.ok(result);
        }

        @Autowired
        private org.springframework.batch.core.launch.JobLauncher jobLauncher;

        @Autowired
        private org.springframework.batch.core.configuration.JobLocator jobLocator;

        /**
         * Launch a Spring Batch job manually
         * POST
         * /api/v2/diagnostic/launch-job?sourceSystem=SHAW&jobName=atoctran_shaw_200_job
         */
        @PostMapping("/launch-job")
        public ResponseEntity<Map<String, Object>> launchJob(
                        @RequestParam String sourceSystem,
                        @RequestParam String jobName) {

                Map<String, Object> result = new HashMap<>();

                try {
                        log.info("🚀 Manually launching job: {}.{}", sourceSystem, jobName);

                        JobParameters jobParameters = new JobParametersBuilder()
                                        .addString("sourceSystem", sourceSystem)
                                        .addString("jobName", jobName)
                                        .addLong("timestamp", System.currentTimeMillis())
                                        .toJobParameters();

                        org.springframework.batch.core.Job job = jobLocator.getJob("genericJob");
                        JobExecution execution = jobLauncher.run(job, jobParameters);

                        result.put("success", true);
                        result.put("jobId", execution.getJobId());
                        result.put("status", execution.getStatus().toString());
                        result.put("exitStatus", execution.getExitStatus().getExitCode());

                } catch (Exception e) {
                        log.error("Error launching job", e);
                        result.put("success", false);
                        result.put("error", e.getMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
                }

                return ResponseEntity.ok(result);
        }

        /**
         * Check saved configurations in batch_configurations table
         * GET /api/v2/diagnostic/batch-configurations
         */
        @GetMapping("/batch-configurations")
        public ResponseEntity<Map<String, Object>> getBatchConfigurations() {
                Map<String, Object> result = new HashMap<>();

                try {
                        // Count total configurations
                        Integer totalCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM CM3INT.BATCH_CONFIGURATIONS", Integer.class);
                        result.put("total_configurations", totalCount);

                        // Get recent configurations
                        List<Map<String, Object>> recentConfigs = jdbcTemplate.queryForList(
                                        "SELECT ID, SOURCE_SYSTEM, JOB_NAME, TRANSACTION_TYPE, CREATED_BY, CREATED_DATE "
                                                        +
                                                        "FROM CM3INT.BATCH_CONFIGURATIONS " +
                                                        "ORDER BY CREATED_DATE DESC " +
                                                        "FETCH FIRST 10 ROWS ONLY");
                        result.put("recent_configurations", recentConfigs);

                        // Check for specific test configurations
                        Integer testCount = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM CM3INT.BATCH_CONFIGURATIONS " +
                                                        "WHERE SOURCE_SYSTEM = 'encore' AND JOB_NAME LIKE '%test%'",
                                        Integer.class);
                        result.put("test_configurations_count", testCount);

                        result.put("status", "success");

                } catch (Exception e) {
                        log.error("Error querying batch_configurations table", e);
                        result.put("status", "error");
                        result.put("error", e.getMessage());
                }

                return ResponseEntity.ok(result);
        }
}