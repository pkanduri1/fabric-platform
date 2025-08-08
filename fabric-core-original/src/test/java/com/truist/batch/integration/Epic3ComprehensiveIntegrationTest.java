package com.truist.batch.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.entity.*;
import com.truist.batch.repository.*;
import com.truist.batch.service.TransactionDependencyService;
import com.truist.batch.staging.TempStagingManager;
import com.truist.batch.template.HeaderFooterGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Epic 3: Comprehensive Integration Test Suite
 * 
 * End-to-end integration tests covering the complete Epic 3 implementation
 * with >85% test coverage target. Tests all components working together
 * in realistic banking scenarios with full security, compliance, and
 * performance validation.
 * 
 * Coverage Areas:
 * - Complete workflow integration from staging creation to cleanup
 * - Performance monitoring and optimization cycles
 * - Header/footer generation with template management
 * - Transaction dependency management integration
 * - Banking compliance and security validation
 * - Error recovery and system resilience
 * - High-volume data processing scenarios
 * - Concurrent operations and resource management
 * 
 * Test Categories:
 * - End-to-End Workflow Tests (Complete business scenarios)
 * - Performance and Scalability Tests (Banking-grade performance)
 * - Security and Compliance Tests (Regulatory compliance)
 * - Error Handling and Recovery Tests (System resilience)
 * - Resource Management Tests (Memory, I/O, concurrency)
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3 - Complex Transaction Processing
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.com.truist.batch=DEBUG",
    "epic3.staging.max-concurrent-operations=10",
    "epic3.staging.default-ttl-hours=2",
    "epic3.staging.performance-monitoring-enabled=true",
    "epic3.staging.auto-optimization-enabled=true"
})
@DisplayName("Epic 3: Comprehensive Integration Test Suite")
public class Epic3ComprehensiveIntegrationTest {

    @Autowired
    private TempStagingManager tempStagingManager;
    
    @Autowired
    private HeaderFooterGenerator headerFooterGenerator;
    
    @Autowired
    private TransactionDependencyService dependencyService;
    
    @Autowired
    private TempStagingDefinitionRepository stagingDefinitionRepository;
    
    @Autowired
    private TempStagingPerformanceRepository stagingPerformanceRepository;
    
    @Autowired
    private OutputTemplateDefinitionRepository templateRepository;
    
    @Autowired
    private TransactionDependencyRepository dependencyRepository;
    
    @Autowired
    private TransactionExecutionGraphRepository graphRepository;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_EXECUTION_ID = "EPIC3-COMPREHENSIVE-TEST";
    private static final String TEST_CORRELATION_ID = "CORR-EPIC3-COMP";
    private static final Long TEST_TRANSACTION_TYPE_ID = 200L;

    @BeforeEach
    void setUp() {
        // Clean up all test data
        stagingPerformanceRepository.deleteAll();
        stagingDefinitionRepository.deleteAll();
        templateRepository.deleteAll();
        graphRepository.deleteAll();
        dependencyRepository.deleteAll();
        
        // Clear caches and reset metrics
        headerFooterGenerator.clearTemplateCache();
        meterRegistry.clear();
    }

    @Nested
    @DisplayName("End-to-End Workflow Tests")
    class EndToEndWorkflowTests {

        @Test
        @DisplayName("Should complete full transaction processing workflow successfully")
        @WithMockUser(roles = {"BATCH_PROCESSOR", "SYSTEM_ADMIN"})
        @Transactional
        void shouldCompleteFullTransactionProcessingWorkflowSuccessfully() throws Exception {
            // PHASE 1: Create staging tables for transaction processing
            
            // Create staging table for transaction data
            String transactionSchema = createTransactionSchema();
            TempStagingManager.StagingTableCreationRequest stagingRequest = 
                new TempStagingManager.StagingTableCreationRequest();
            stagingRequest.setExecutionId(TEST_EXECUTION_ID);
            stagingRequest.setTransactionTypeId(TEST_TRANSACTION_TYPE_ID);
            stagingRequest.setSchemaDefinition(transactionSchema);
            stagingRequest.setExpectedRecordCount(50000);
            stagingRequest.setSecurityRequired(true);
            stagingRequest.setTtlHours(4);
            
            TempStagingManager.StagingTableCreationResult stagingResult = 
                tempStagingManager.createStagingTable(stagingRequest);
            
            assertThat(stagingResult.isSuccessful()).isTrue();
            assertThat(stagingResult.getDefinition()).isNotNull();
            
            String stagingTableName = stagingResult.getDefinition().getStagingTableName();
            
            // PHASE 2: Create output templates for reports
            
            OutputTemplateDefinitionEntity headerTemplate = createComprehensiveHeaderTemplate();
            OutputTemplateDefinitionEntity footerTemplate = createComprehensiveFooterTemplate();
            templateRepository.save(headerTemplate);
            templateRepository.save(footerTemplate);
            
            // PHASE 3: Set up transaction dependencies
            
            Set<String> transactions = Set.of("LOAD_DATA", "VALIDATE_DATA", "TRANSFORM_DATA", 
                                            "GENERATE_REPORT", "CLEANUP_STAGING");
            List<TransactionDependencyEntity> dependencies = createWorkflowDependencies(transactions);
            dependencyRepository.saveAll(dependencies);
            
            // Build execution graph
            TransactionDependencyService.DependencyResolutionResult dependencyResult = 
                dependencyService.buildExecutionGraph(TEST_EXECUTION_ID, transactions, TEST_CORRELATION_ID);
            
            assertThat(dependencyResult.isSuccessful()).isTrue();
            assertThat(dependencyResult.getGraphNodes()).hasSameSizeAs(transactions);
            
            // PHASE 4: Generate headers and footers
            
            Map<String, Object> reportVariables = createReportVariables(stagingTableName, 50000);
            
            HeaderFooterGenerator.GenerationRequest headerRequest = 
                new HeaderFooterGenerator.GenerationRequest(
                    "comprehensive_header", "HEADER", "CSV", reportVariables\n                ).withExecutionId(TEST_EXECUTION_ID)\n                 .withTransactionType(TEST_TRANSACTION_TYPE_ID);\n            \n            HeaderFooterGenerator.GenerationResult headerResult = \n                headerFooterGenerator.generateHeader(headerRequest);\n            \n            assertThat(headerResult.isSuccess()).isTrue();\n            assertThat(headerResult.getGeneratedContent()).contains(\"Transaction Processing Report\");\n            \n            // PHASE 5: Simulate transaction processing with performance monitoring\n            \n            simulateTransactionProcessing(stagingResult.getDefinition().getStagingDefId());\n            \n            // PHASE 6: Generate footer after processing\n            \n            HeaderFooterGenerator.GenerationRequest footerRequest = \n                new HeaderFooterGenerator.GenerationRequest(\n                    \"comprehensive_footer\", \"FOOTER\", \"CSV\", reportVariables\n                ).withExecutionId(TEST_EXECUTION_ID);\n            \n            HeaderFooterGenerator.GenerationResult footerResult = \n                headerFooterGenerator.generateFooter(footerRequest);\n            \n            assertThat(footerResult.isSuccess()).isTrue();\n            assertThat(footerResult.getGeneratedContent()).contains(\"Total Records: 50000\");\n            \n            // PHASE 7: Monitor performance and apply optimizations\n            \n            TempStagingManager.StagingPerformanceOptimizationResult optimizationResult = \n                tempStagingManager.optimizeStagingTable(stagingTableName);\n            \n            assertThat(optimizationResult).isNotNull();\n            assertThat(optimizationResult.getTableName()).isEqualTo(stagingTableName);\n            \n            // PHASE 8: Verify metrics and audit trails\n            \n            verifyComprehensiveMetrics();\n            verifyAuditTrails();\n            \n            // PHASE 9: Cleanup\n            \n            boolean cleanupSuccess = tempStagingManager.dropStagingTable(stagingTableName, \"Test completed\");\n            assertThat(cleanupSuccess).isTrue();\n        }\n\n        @Test\n        @DisplayName(\"Should handle high-volume concurrent transaction processing\")\n        @WithMockUser(roles = {\"BATCH_PROCESSOR\", \"SYSTEM_ADMIN\"})\n        void shouldHandleHighVolumeConcurrentTransactionProcessing() throws Exception {\n            // Given - Multiple concurrent execution streams\n            List<CompletableFuture<Boolean>> futures = new ArrayList<>();\n            int concurrentExecutions = 5;\n            int recordsPerExecution = 10000;\n            \n            for (int i = 0; i < concurrentExecutions; i++) {\n                final int executionNumber = i;\n                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {\n                    try {\n                        return executeHighVolumeProcessing(executionNumber, recordsPerExecution);\n                    } catch (Exception e) {\n                        return false;\n                    }\n                });\n                futures.add(future);\n            }\n            \n            // When - Execute all concurrent processing\n            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))\n                           .get(60, TimeUnit.SECONDS);\n            \n            // Then - Verify all executions completed successfully\n            for (CompletableFuture<Boolean> future : futures) {\n                assertThat(future.get()).isTrue();\n            }\n            \n            // Verify system performance under load\n            assertThat(meterRegistry.counter(\"epic3.staging.tables_created\").count())\n                .isEqualTo(concurrentExecutions);\n            assertThat(meterRegistry.timer(\"epic3.staging.create_time\").max(TimeUnit.MILLISECONDS))\n                .isLessThan(10000); // Less than 10 seconds even under load\n        }\n    }\n\n    @Nested\n    @DisplayName(\"Performance and Scalability Tests\")\n    class PerformanceTests {\n\n        @Test\n        @DisplayName(\"Should maintain performance with large datasets\")\n        @WithMockUser(roles = {\"BATCH_PROCESSOR\"})\n        @Transactional\n        void shouldMaintainPerformanceWithLargeDatasets() {\n            // Given - Large dataset staging table\n            String largeDatasetSchema = createLargeDatasetSchema();\n            TempStagingManager.StagingTableCreationRequest request = \n                new TempStagingManager.StagingTableCreationRequest();\n            request.setExecutionId(TEST_EXECUTION_ID + \"_LARGE\");\n            request.setTransactionTypeId(TEST_TRANSACTION_TYPE_ID);\n            request.setSchemaDefinition(largeDatasetSchema);\n            request.setExpectedRecordCount(1_000_000); // 1 million records\n            request.setPartitionStrategy(TempStagingManager.PartitionStrategy.HASH);\n            \n            // When - Create large staging table\n            long startTime = System.currentTimeMillis();\n            TempStagingManager.StagingTableCreationResult result = \n                tempStagingManager.createStagingTable(request);\n            long endTime = System.currentTimeMillis();\n            \n            // Then - Verify performance meets banking requirements\n            assertThat(result.isSuccessful()).isTrue();\n            assertThat(endTime - startTime).isLessThan(5000); // Less than 5 seconds\n            assertThat(result.getCreationDurationMs()).isLessThan(5000);\n            \n            // Verify partitioning was applied\n            assertThat(result.getDefinition().getPartitionStrategy())\n                .isEqualTo(TempStagingDefinitionEntity.PartitionStrategy.HASH);\n            \n            // Verify performance metrics\n            TempStagingManager.StagingPerformanceMetrics metrics = \n                tempStagingManager.getStagingPerformanceMetrics(TEST_EXECUTION_ID + \"_LARGE\");\n            assertThat(metrics.getTotalTables()).isEqualTo(1);\n        }\n\n        @Test\n        @DisplayName(\"Should optimize resource utilization automatically\")\n        @WithMockUser(roles = {\"BATCH_PROCESSOR\"})\n        @Transactional\n        void shouldOptimizeResourceUtilizationAutomatically() {\n            // Given - Staging table with performance issues\n            String schema = createTransactionSchema();\n            TempStagingManager.StagingTableCreationRequest request = \n                new TempStagingManager.StagingTableCreationRequest();\n            request.setExecutionId(TEST_EXECUTION_ID + \"_OPT\");\n            request.setTransactionTypeId(TEST_TRANSACTION_TYPE_ID);\n            request.setSchemaDefinition(schema);\n            request.setExpectedRecordCount(100000);\n            \n            TempStagingManager.StagingTableCreationResult result = \n                tempStagingManager.createStagingTable(request);\n            assertThat(result.isSuccessful()).isTrue();\n            \n            String tableName = result.getDefinition().getStagingTableName();\n            Long stagingDefId = result.getDefinition().getStagingDefId();\n            \n            // Simulate performance issues\n            createPerformanceIssueMetrics(stagingDefId);\n            \n            // When - Apply optimization\n            TempStagingManager.StagingPerformanceOptimizationResult optimizationResult = \n                tempStagingManager.optimizeStagingTable(tableName);\n            \n            // Then - Verify optimization applied\n            assertThat(optimizationResult).isNotNull();\n            assertThat(optimizationResult.getOptimizationsApplied()).isNotEmpty();\n            assertThat(optimizationResult.getPerformanceImprovement()).isGreaterThanOrEqualTo(0.0);\n            \n            // Verify optimization metrics recorded\n            List<TempStagingPerformanceEntity> optimizationMetrics = \n                stagingPerformanceRepository.findByPerformanceMeasurementTypeOrderByMeasurementTimestampDesc(\n                    TempStagingPerformanceEntity.PerformanceMeasurementType.OPTIMIZATION_APPLIED\n                );\n            assertThat(optimizationMetrics).isNotEmpty();\n        }\n    }\n\n    @Nested\n    @DisplayName(\"Security and Compliance Tests\")\n    class SecurityComplianceTests {\n\n        @Test\n        @DisplayName(\"Should maintain banking-grade audit trails\")\n        @WithMockUser(roles = {\"BATCH_PROCESSOR\"})\n        @Transactional\n        void shouldMaintainBankingGradeAuditTrails() {\n            // Given - High-compliance transaction processing\n            String schema = createTransactionSchema();\n            TempStagingManager.StagingTableCreationRequest request = \n                new TempStagingManager.StagingTableCreationRequest();\n            request.setExecutionId(TEST_EXECUTION_ID + \"_AUDIT\");\n            request.setTransactionTypeId(TEST_TRANSACTION_TYPE_ID);\n            request.setSchemaDefinition(schema);\n            request.setSecurityRequired(true);\n            \n            // When - Create staging table with security requirements\n            TempStagingManager.StagingTableCreationResult result = \n                tempStagingManager.createStagingTable(request);\n            \n            // Then - Verify audit information captured\n            assertThat(result.isSuccessful()).isTrue();\n            assertThat(result.getDefinition().isEncrypted()).isTrue(); // Security applied\n            \n            // Verify performance audit trail\n            List<TempStagingPerformanceEntity> auditRecords = \n                stagingPerformanceRepository.findByStagingDefIdOrderByMeasurementTimestampDesc(\n                    result.getDefinition().getStagingDefId());\n            \n            // Should have creation performance record at minimum\n            assertThat(auditRecords).isNotEmpty();\n            \n            // Verify business date set for compliance reporting\n            assertThat(result.getDefinition().getBusinessDate()).isNotNull();\n        }\n\n        @Test\n        @DisplayName(\"Should enforce data encryption for sensitive transactions\")\n        @WithMockUser(roles = {\"BATCH_PROCESSOR\"})\n        @Transactional\n        void shouldEnforceDataEncryptionForSensitiveTransactions() {\n            // Given - Sensitive financial transaction schema\n            String sensitiveSchema = createSensitiveFinancialSchema();\n            TempStagingManager.StagingTableCreationRequest request = \n                new TempStagingManager.StagingTableCreationRequest();\n            request.setExecutionId(TEST_EXECUTION_ID + \"_ENCRYPT\");\n            request.setTransactionTypeId(TEST_TRANSACTION_TYPE_ID);\n            request.setSchemaDefinition(sensitiveSchema);\n            request.setSecurityRequired(true);\n            \n            // When - Create staging table for sensitive data\n            TempStagingManager.StagingTableCreationResult result = \n                tempStagingManager.createStagingTable(request);\n            \n            // Then - Verify encryption applied\n            assertThat(result.isSuccessful()).isTrue();\n            assertThat(result.getDefinition().getEncryptionApplied()).isEqualTo(\"Y\");\n            assertThat(result.getDefinition().getCompressionLevel())\n                .isNotEqualTo(TempStagingDefinitionEntity.CompressionLevel.NONE);\n        }\n    }\n\n    @Nested\n    @DisplayName(\"Error Handling and Recovery Tests\")\n    class ErrorHandlingTests {\n\n        @Test\n        @DisplayName(\"Should recover gracefully from resource constraints\")\n        @WithMockUser(roles = {\"BATCH_PROCESSOR\"})\n        void shouldRecoverGracefullyFromResourceConstraints() {\n            // Given - Multiple concurrent staging table creation requests\n            List<CompletableFuture<TempStagingManager.StagingTableCreationResult>> futures = new ArrayList<>();\n            \n            // Create more requests than the concurrent limit allows\n            for (int i = 0; i < 15; i++) { // Max is 10 in test properties\n                final int requestNumber = i;\n                CompletableFuture<TempStagingManager.StagingTableCreationResult> future = \n                    CompletableFuture.supplyAsync(() -> {\n                        String schema = createTransactionSchema();\n                        TempStagingManager.StagingTableCreationRequest request = \n                            new TempStagingManager.StagingTableCreationRequest();\n                        request.setExecutionId(TEST_EXECUTION_ID + \"_RESOURCE_\" + requestNumber);\n                        request.setTransactionTypeId(TEST_TRANSACTION_TYPE_ID);\n                        request.setSchemaDefinition(schema);\n                        \n                        return tempStagingManager.createStagingTable(request);\n                    });\n                futures.add(future);\n            }\n            \n            // When - Execute all requests\n            List<TempStagingManager.StagingTableCreationResult> results = futures.stream()\n                .map(CompletableFuture::join)\n                .toList();\n            \n            // Then - Some should succeed, some should fail gracefully due to resource limits\n            long successfulRequests = results.stream()\n                .filter(TempStagingManager.StagingTableCreationResult::isSuccessful)\n                .count();\n            long failedRequests = results.stream()\n                .filter(r -> !r.isSuccessful())\n                .count();\n            \n            // Should have some successes and some controlled failures\n            assertThat(successfulRequests).isGreaterThan(0);\n            assertThat(failedRequests).isGreaterThan(0);\n            assertThat(successfulRequests + failedRequests).isEqualTo(15);\n        }\n\n        @Test\n        @DisplayName(\"Should handle template processing errors gracefully\")\n        @WithMockUser(roles = {\"BATCH_PROCESSOR\"})\n        void shouldHandleTemplateProcessingErrorsGracefully() {\n            // Given - Template with processing issues\n            OutputTemplateDefinitionEntity problematicTemplate = new OutputTemplateDefinitionEntity();\n            problematicTemplate.setTemplateName(\"problematic_template\");\n            problematicTemplate.setTemplateType(OutputTemplateDefinitionEntity.TemplateType.HEADER);\n            problematicTemplate.setOutputFormat(OutputTemplateDefinitionEntity.OutputFormat.CSV);\n            problematicTemplate.setTemplateContent(\"Problematic: ${undefinedVar} and ${anotherUndefined}\");\n            problematicTemplate.setCreatedBy(\"TEST_USER\");\n            problematicTemplate.setActiveFlag(\"Y\");\n            templateRepository.save(problematicTemplate);\n            \n            HeaderFooterGenerator.GenerationRequest request = \n                new HeaderFooterGenerator.GenerationRequest(\n                    \"problematic_template\", \"HEADER\", \"CSV\", Map.of() // No variables provided\n                );\n            \n            // When - Generate template with missing variables\n            HeaderFooterGenerator.GenerationResult result = \n                headerFooterGenerator.generateHeader(request);\n            \n            // Then - Should fail gracefully with informative error\n            assertThat(result.isSuccess()).isFalse();\n            assertThat(result.getErrorMessage()).contains(\"Missing required variables\");\n            assertThat(result.getMetadata()).containsKey(\"error\");\n        }\n    }\n\n    // Helper methods for test data creation and verification\n\n    private String createTransactionSchema() {\n        Map<String, Object> schema = Map.of(\n            \"columns\", List.of(\n                Map.of(\"name\", \"TRANSACTION_ID\", \"type\", \"VARCHAR2(50)\", \"nullable\", false),\n                Map.of(\"name\", \"TRANSACTION_DATE\", \"type\", \"TIMESTAMP\", \"nullable\", false),\n                Map.of(\"name\", \"AMOUNT\", \"type\", \"NUMBER(15,2)\", \"nullable\", false),\n                Map.of(\"name\", \"ACCOUNT_ID\", \"type\", \"VARCHAR2(20)\", \"nullable\", false),\n                Map.of(\"name\", \"TRANSACTION_TYPE\", \"type\", \"VARCHAR2(10)\", \"nullable\", false),\n                Map.of(\"name\", \"STATUS\", \"type\", \"VARCHAR2(20)\", \"nullable\", true),\n                Map.of(\"name\", \"DESCRIPTION\", \"type\", \"VARCHAR2(500)\", \"nullable\", true)\n            )\n        );\n        \n        try {\n            return objectMapper.writeValueAsString(schema);\n        } catch (Exception e) {\n            return \"{\\\"columns\\\": []}\"; // Fallback\n        }\n    }\n\n    private String createLargeDatasetSchema() {\n        List<Map<String, Object>> columns = new ArrayList<>();\n        \n        // Create schema with many columns for large dataset processing\n        columns.add(Map.of(\"name\", \"ID\", \"type\", \"NUMBER(19)\", \"nullable\", false));\n        columns.add(Map.of(\"name\", \"CREATED_DATE\", \"type\", \"TIMESTAMP\", \"nullable\", false));\n        \n        for (int i = 1; i <= 20; i++) {\n            columns.add(Map.of(\"name\", \"DATA_FIELD_\" + i, \"type\", \"VARCHAR2(100)\", \"nullable\", true));\n        }\n        \n        for (int i = 1; i <= 10; i++) {\n            columns.add(Map.of(\"name\", \"AMOUNT_\" + i, \"type\", \"NUMBER(15,2)\", \"nullable\", true));\n        }\n        \n        Map<String, Object> schema = Map.of(\"columns\", columns);\n        \n        try {\n            return objectMapper.writeValueAsString(schema);\n        } catch (Exception e) {\n            return createTransactionSchema(); // Fallback\n        }\n    }\n\n    private String createSensitiveFinancialSchema() {\n        Map<String, Object> schema = Map.of(\n            \"columns\", List.of(\n                Map.of(\"name\", \"ACCOUNT_NUMBER\", \"type\", \"VARCHAR2(20)\", \"nullable\", false),\n                Map.of(\"name\", \"SSN\", \"type\", \"VARCHAR2(11)\", \"nullable\", true),\n                Map.of(\"name\", \"CREDIT_CARD_NUMBER\", \"type\", \"VARCHAR2(20)\", \"nullable\", true),\n                Map.of(\"name\", \"ACCOUNT_BALANCE\", \"type\", \"NUMBER(15,2)\", \"nullable\", false),\n                Map.of(\"name\", \"TRANSACTION_AMOUNT\", \"type\", \"NUMBER(15,2)\", \"nullable\", false),\n                Map.of(\"name\", \"CUSTOMER_NAME\", \"type\", \"VARCHAR2(100)\", \"nullable\", false),\n                Map.of(\"name\", \"PHONE_NUMBER\", \"type\", \"VARCHAR2(15)\", \"nullable\", true)\n            )\n        );\n        \n        try {\n            return objectMapper.writeValueAsString(schema);\n        } catch (Exception e) {\n            return createTransactionSchema(); // Fallback\n        }\n    }\n\n    private OutputTemplateDefinitionEntity createComprehensiveHeaderTemplate() {\n        OutputTemplateDefinitionEntity template = new OutputTemplateDefinitionEntity();\n        template.setTemplateName(\"comprehensive_header\");\n        template.setTemplateType(OutputTemplateDefinitionEntity.TemplateType.HEADER);\n        template.setOutputFormat(OutputTemplateDefinitionEntity.OutputFormat.CSV);\n        template.setTemplateContent(\n            \"Transaction Processing Report\\n\" +\n            \"Generated: ${currentDateTime}\\n\" +\n            \"Execution ID: ${executionId}\\n\" +\n            \"Staging Table: ${stagingTable}\\n\" +\n            \"Expected Records: ${expectedRecords}\\n\" +\n            \"\\n\" +\n            \"Transaction ID,Date,Amount,Account,Type,Status\\n\"\n        );\n        template.setCreatedBy(\"TEST_USER\");\n        template.setActiveFlag(\"Y\");\n        template.setVersionNumber(1);\n        template.setCreatedDate(LocalDateTime.now());\n        return template;\n    }\n\n    private OutputTemplateDefinitionEntity createComprehensiveFooterTemplate() {\n        OutputTemplateDefinitionEntity template = new OutputTemplateDefinitionEntity();\n        template.setTemplateName(\"comprehensive_footer\");\n        template.setTemplateType(OutputTemplateDefinitionEntity.TemplateType.FOOTER);\n        template.setOutputFormat(OutputTemplateDefinitionEntity.OutputFormat.CSV);\n        template.setTemplateContent(\n            \"\\n\" +\n            \"Processing Summary:\\n\" +\n            \"Total Records: ${totalRecords}\\n\" +\n            \"Processing Time: ${processingTimeMs}ms\\n\" +\n            \"Status: ${finalStatus}\\n\" +\n            \"Completed: ${currentDateTime}\\n\"\n        );\n        template.setCreatedBy(\"TEST_USER\");\n        template.setActiveFlag(\"Y\");\n        template.setVersionNumber(1);\n        template.setCreatedDate(LocalDateTime.now());\n        return template;\n    }\n\n    private List<TransactionDependencyEntity> createWorkflowDependencies(Set<String> transactions) {\n        List<TransactionDependencyEntity> dependencies = new ArrayList<>();\n        \n        // Create workflow: LOAD_DATA -> VALIDATE_DATA -> TRANSFORM_DATA -> GENERATE_REPORT -> CLEANUP_STAGING\n        dependencies.add(createDependency(\"LOAD_DATA\", \"VALIDATE_DATA\"));\n        dependencies.add(createDependency(\"VALIDATE_DATA\", \"TRANSFORM_DATA\"));\n        dependencies.add(createDependency(\"TRANSFORM_DATA\", \"GENERATE_REPORT\"));\n        dependencies.add(createDependency(\"GENERATE_REPORT\", \"CLEANUP_STAGING\"));\n        \n        return dependencies;\n    }\n\n    private TransactionDependencyEntity createDependency(String source, String target) {\n        TransactionDependencyEntity dependency = new TransactionDependencyEntity();\n        dependency.setSourceTransactionId(source);\n        dependency.setTargetTransactionId(target);\n        dependency.setDependencyType(TransactionDependencyEntity.DependencyType.SEQUENTIAL);\n        dependency.setPriorityWeight(1);\n        dependency.setMaxWaitTimeSeconds(3600);\n        dependency.setRetryPolicy(\"EXPONENTIAL_BACKOFF\");\n        dependency.setActiveFlag(\"Y\");\n        dependency.setComplianceLevel(\"HIGH\");\n        dependency.setCreatedBy(\"TEST_USER\");\n        dependency.setCreatedDate(LocalDateTime.now());\n        dependency.setBusinessJustification(\"Integration test dependency\");\n        return dependency;\n    }\n\n    private Map<String, Object> createReportVariables(String stagingTableName, int recordCount) {\n        Map<String, Object> variables = new HashMap<>();\n        variables.put(\"executionId\", TEST_EXECUTION_ID);\n        variables.put(\"stagingTable\", stagingTableName);\n        variables.put(\"expectedRecords\", recordCount);\n        variables.put(\"totalRecords\", recordCount);\n        variables.put(\"processingTimeMs\", 2500);\n        variables.put(\"finalStatus\", \"COMPLETED\");\n        return variables;\n    }\n\n    private void simulateTransactionProcessing(Long stagingDefId) {\n        // Create performance metrics simulating transaction processing\n        TempStagingPerformanceEntity loadMetric = new TempStagingPerformanceEntity();\n        loadMetric.setStagingDefId(stagingDefId);\n        loadMetric.setExecutionId(TEST_EXECUTION_ID);\n        loadMetric.setCorrelationId(TEST_CORRELATION_ID);\n        loadMetric.setPerformanceMeasurementType(\n            TempStagingPerformanceEntity.PerformanceMeasurementType.DATA_INSERTION);\n        loadMetric.setDurationMs(2500L);\n        loadMetric.setRecordsProcessed(50000L);\n        loadMetric.setThroughputRecordsPerSec(BigDecimal.valueOf(20000));\n        loadMetric.setMemoryUsedMb(512);\n        loadMetric.setCpuUsagePercent(BigDecimal.valueOf(45.5));\n        stagingPerformanceRepository.save(loadMetric);\n    }\n\n    private void createPerformanceIssueMetrics(Long stagingDefId) {\n        // Create metrics indicating performance issues\n        TempStagingPerformanceEntity slowQuery = new TempStagingPerformanceEntity();\n        slowQuery.setStagingDefId(stagingDefId);\n        slowQuery.setExecutionId(TEST_EXECUTION_ID);\n        slowQuery.setPerformanceMeasurementType(\n            TempStagingPerformanceEntity.PerformanceMeasurementType.QUERY_EXECUTION);\n        slowQuery.setDurationMs(35000L); // 35 seconds - slow\n        slowQuery.setMemoryUsedMb(2048); // High memory usage\n        slowQuery.setCpuUsagePercent(BigDecimal.valueOf(85.0)); // High CPU\n        slowQuery.setBottleneckIdentified(\"Missing index on frequently queried columns\");\n        slowQuery.setRecommendation(\"Add composite index on (STATUS, CREATED_DATE)\");\n        stagingPerformanceRepository.save(slowQuery);\n    }\n\n    private boolean executeHighVolumeProcessing(int executionNumber, int recordCount) {\n        try {\n            String schema = createTransactionSchema();\n            TempStagingManager.StagingTableCreationRequest request = \n                new TempStagingManager.StagingTableCreationRequest();\n            request.setExecutionId(TEST_EXECUTION_ID + \"_VOLUME_\" + executionNumber);\n            request.setTransactionTypeId(TEST_TRANSACTION_TYPE_ID);\n            request.setSchemaDefinition(schema);\n            request.setExpectedRecordCount(recordCount);\n            \n            TempStagingManager.StagingTableCreationResult result = \n                tempStagingManager.createStagingTable(request);\n            \n            if (!result.isSuccessful()) {\n                return false;\n            }\n            \n            // Simulate processing\n            simulateTransactionProcessing(result.getDefinition().getStagingDefId());\n            \n            return true;\n        } catch (Exception e) {\n            return false;\n        }\n    }\n\n    private void verifyComprehensiveMetrics() {\n        // Verify staging table metrics\n        assertThat(meterRegistry.counter(\"epic3.staging.tables_created\").count())\n            .isGreaterThan(0);\n            \n        // Verify performance monitoring metrics\n        assertThat(meterRegistry.timer(\"epic3.staging.create_time\").count())\n            .isGreaterThan(0);\n            \n        // Verify dependency resolution metrics\n        assertThat(meterRegistry.counter(\"epic3.dependency.resolution\").count())\n            .isGreaterThan(0);\n    }\n\n    private void verifyAuditTrails() {\n        // Verify staging definitions have audit information\n        List<TempStagingDefinitionEntity> allDefinitions = stagingDefinitionRepository.findAll();\n        for (TempStagingDefinitionEntity definition : allDefinitions) {\n            assertThat(definition.getCreatedTimestamp()).isNotNull();\n            assertThat(definition.getBusinessDate()).isNotNull();\n            assertThat(definition.getExecutionId()).isNotNull();\n        }\n        \n        // Verify performance records have audit trails\n        List<TempStagingPerformanceEntity> allPerformanceRecords = stagingPerformanceRepository.findAll();\n        for (TempStagingPerformanceEntity record : allPerformanceRecords) {\n            assertThat(record.getMeasurementTimestamp()).isNotNull();\n            assertThat(record.getBusinessDate()).isNotNull();\n            assertThat(record.getExecutionId()).isNotNull();\n        }\n    }\n}