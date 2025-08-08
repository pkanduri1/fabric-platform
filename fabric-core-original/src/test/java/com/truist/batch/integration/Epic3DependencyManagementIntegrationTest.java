package com.truist.batch.integration;

import com.truist.batch.entity.TransactionDependencyEntity;
import com.truist.batch.entity.TransactionExecutionGraphEntity;
import com.truist.batch.repository.TransactionDependencyRepository;
import com.truist.batch.repository.TransactionExecutionGraphRepository;
import com.truist.batch.service.TransactionDependencyService;
import com.truist.batch.sequencer.TransactionSequencer;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Epic 3: Dependency Management Integration Tests
 * 
 * End-to-end integration tests for complex transaction dependency management system.
 * Tests the complete workflow from dependency definition through execution graph
 * construction and parallel processing coordination.
 * 
 * Test Scenarios:
 * - Complete dependency resolution workflow
 * - Real database operations with test data
 * - Performance validation with large datasets
 * - Banking compliance and security validation
 * - Error handling and recovery scenarios
 * - Concurrent processing coordination
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.com.truist.batch=DEBUG"
})
@Sql(scripts = "/sql/epic3-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Epic 3: Dependency Management Integration Tests")
public class Epic3DependencyManagementIntegrationTest {

    @Autowired
    private TransactionDependencyService dependencyService;
    
    @Autowired
    private TransactionSequencer transactionSequencer;
    
    @Autowired
    private TransactionDependencyRepository dependencyRepository;
    
    @Autowired
    private TransactionExecutionGraphRepository graphRepository;
    
    @Autowired
    private MeterRegistry meterRegistry;

    private static final String TEST_EXECUTION_ID = "EPIC3-INTEGRATION-TEST";
    private static final String TEST_CORRELATION_ID = "CORR-EPIC3-INTEG";

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        graphRepository.deleteAll();
        dependencyRepository.deleteAll();
        
        // Reset metrics
        meterRegistry.clear();
    }

    @Nested
    @DisplayName("End-to-End Workflow Tests")
    class EndToEndWorkflowTests {

        @Test
        @DisplayName("Should complete full dependency resolution workflow successfully")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        @Transactional
        void shouldCompleteFullDependencyResolutionWorkflow() {
            // Given - Complex dependency graph with multiple levels
            Set<String> transactions = createComplexTransactionSet();
            List<TransactionDependencyEntity> dependencies = createComplexDependencySet(transactions);
            dependencyRepository.saveAll(dependencies);

            // When - Build execution graph
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(TEST_EXECUTION_ID, transactions, TEST_CORRELATION_ID);

            // Then - Verify successful resolution
            assertThat(result).isNotNull();
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getGraphNodes()).hasSameSizeAs(transactions);
            assertThat(result.getTopologicalOrder()).containsExactlyInAnyOrderElementsOf(transactions);

            // Verify database persistence
            List<TransactionExecutionGraphEntity> persistedNodes = 
                graphRepository.findByExecutionIdOrderByTopologicalOrder(TEST_EXECUTION_ID);
            assertThat(persistedNodes).hasSameSizeAs(transactions);

            // Verify topological order is maintained in database
            for (int i = 1; i < persistedNodes.size(); i++) {
                assertThat(persistedNodes.get(i).getTopologicalOrder())
                    .isGreaterThan(persistedNodes.get(i-1).getTopologicalOrder());
            }

            // Verify metrics were recorded
            assertThat(meterRegistry.counter("epic3.dependency.resolution").count()).isEqualTo(1);
            assertThat(meterRegistry.timer("epic3.dependency.processing_time").count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle concurrent execution graph builds correctly")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldHandleConcurrentExecutionGraphBuildsCorrectly() throws Exception {
            // Given - Multiple concurrent executions
            Set<String> transactions1 = Set.of("CONC-TXN-A1", "CONC-TXN-B1", "CONC-TXN-C1");
            Set<String> transactions2 = Set.of("CONC-TXN-A2", "CONC-TXN-B2", "CONC-TXN-C2");
            Set<String> transactions3 = Set.of("CONC-TXN-A3", "CONC-TXN-B3", "CONC-TXN-C3");

            List<TransactionDependencyEntity> deps1 = createLinearDependencies(transactions1);
            List<TransactionDependencyEntity> deps2 = createLinearDependencies(transactions2);
            List<TransactionDependencyEntity> deps3 = createLinearDependencies(transactions3);

            dependencyRepository.saveAll(deps1);
            dependencyRepository.saveAll(deps2);
            dependencyRepository.saveAll(deps3);

            // When - Execute concurrent builds
            CompletableFuture<TransactionDependencyService.DependencyResolutionResult> future1 = 
                CompletableFuture.supplyAsync(() -> 
                    dependencyService.buildExecutionGraph("EXEC-1", transactions1, "CORR-1"));
                    
            CompletableFuture<TransactionDependencyService.DependencyResolutionResult> future2 = 
                CompletableFuture.supplyAsync(() -> 
                    dependencyService.buildExecutionGraph("EXEC-2", transactions2, "CORR-2"));
                    
            CompletableFuture<TransactionDependencyService.DependencyResolutionResult> future3 = 
                CompletableFuture.supplyAsync(() -> 
                    dependencyService.buildExecutionGraph("EXEC-3", transactions3, "CORR-3"));

            // Then - All should complete successfully
            CompletableFuture.allOf(future1, future2, future3).get(10, TimeUnit.SECONDS);

            TransactionDependencyService.DependencyResolutionResult result1 = future1.get();
            TransactionDependencyService.DependencyResolutionResult result2 = future2.get();
            TransactionDependencyService.DependencyResolutionResult result3 = future3.get();

            assertThat(result1.isSuccessful()).isTrue();
            assertThat(result2.isSuccessful()).isTrue();
            assertThat(result3.isSuccessful()).isTrue();

            // Verify no cross-contamination between executions
            assertThat(graphRepository.findByExecutionIdOrderByTopologicalOrder("EXEC-1"))
                .hasSize(transactions1.size());
            assertThat(graphRepository.findByExecutionIdOrderByTopologicalOrder("EXEC-2"))
                .hasSize(transactions2.size());
            assertThat(graphRepository.findByExecutionIdOrderByTopologicalOrder("EXEC-3"))
                .hasSize(transactions3.size());
        }

        @Test
        @DisplayName("Should coordinate parallel transaction processing")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        @Transactional
        void shouldCoordinateParallelTransactionProcessing() {
            // Given - Parallel execution scenario: A -> (B,C) -> D
            Set<String> transactions = Set.of("PAR-A", "PAR-B", "PAR-C", "PAR-D");
            List<TransactionDependencyEntity> dependencies = List.of(
                createDependency("PAR-A", "PAR-B"),
                createDependency("PAR-A", "PAR-C"),
                createDependency("PAR-B", "PAR-D"),
                createDependency("PAR-C", "PAR-D")
            );
            dependencyRepository.saveAll(dependencies);

            // When - Build execution graph
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(TEST_EXECUTION_ID, transactions, TEST_CORRELATION_ID);

            // Then - Verify parallel processing capability
            assertThat(result.isSuccessful()).isTrue();

            // Step 1: Initially only PAR-A should be ready (in-degree = 0)
            List<TransactionExecutionGraphEntity> readyTransactions = 
                dependencyService.getReadyTransactions(TEST_EXECUTION_ID);
            assertThat(readyTransactions).hasSize(1);
            assertThat(readyTransactions.get(0).getTransactionId()).isEqualTo("PAR-A");

            // Step 2: Mark PAR-A as completed
            dependencyService.markTransactionStarted(TEST_EXECUTION_ID, "PAR-A", "THREAD-1", TEST_CORRELATION_ID);
            dependencyService.markTransactionCompleted(TEST_EXECUTION_ID, "PAR-A", TEST_CORRELATION_ID);

            // Step 3: Now PAR-B and PAR-C should be ready for parallel processing
            readyTransactions = dependencyService.getReadyTransactions(TEST_EXECUTION_ID);
            assertThat(readyTransactions).hasSize(2);
            assertThat(readyTransactions)
                .extracting(TransactionExecutionGraphEntity::getTransactionId)
                .containsExactlyInAnyOrder("PAR-B", "PAR-C");

            // Step 4: Complete both parallel transactions
            dependencyService.markTransactionStarted(TEST_EXECUTION_ID, "PAR-B", "THREAD-2", TEST_CORRELATION_ID);
            dependencyService.markTransactionStarted(TEST_EXECUTION_ID, "PAR-C", "THREAD-3", TEST_CORRELATION_ID);
            dependencyService.markTransactionCompleted(TEST_EXECUTION_ID, "PAR-B", TEST_CORRELATION_ID);
            dependencyService.markTransactionCompleted(TEST_EXECUTION_ID, "PAR-C", TEST_CORRELATION_ID);

            // Step 5: Finally PAR-D should be ready
            readyTransactions = dependencyService.getReadyTransactions(TEST_EXECUTION_ID);
            assertThat(readyTransactions).hasSize(1);
            assertThat(readyTransactions.get(0).getTransactionId()).isEqualTo("PAR-D");

            // Verify execution status
            TransactionDependencyService.ExecutionStatusSummary status = 
                dependencyService.getExecutionStatus(TEST_EXECUTION_ID);
            assertThat(status.getCompletedNodes()).isEqualTo(2);
            assertThat(status.getReadyNodes()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Performance and Scalability Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle large transaction sets efficiently")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        @Transactional
        void shouldHandleLargeTransactionSetsEfficiently() {
            // Given - Large transaction set (500 transactions)
            Set<String> largeTransactionSet = createLargeTransactionSet(500);
            List<TransactionDependencyEntity> largeDependencySet = 
                createScalableRandomDependencies(largeTransactionSet, 0.1); // 10% dependency ratio
            
            dependencyRepository.saveAll(largeDependencySet);

            // When - Measure execution time
            long startTime = System.nanoTime();
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(TEST_EXECUTION_ID, largeTransactionSet, TEST_CORRELATION_ID);
            long endTime = System.nanoTime();

            // Then - Verify successful processing and performance
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getGraphNodes()).hasSameSizeAs(largeTransactionSet);

            long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            assertThat(durationMs).isLessThan(10000); // Should complete within 10 seconds

            // Verify database performance
            List<TransactionExecutionGraphEntity> persistedNodes = 
                graphRepository.findByExecutionIdOrderByTopologicalOrder(TEST_EXECUTION_ID);
            assertThat(persistedNodes).hasSameSizeAs(largeTransactionSet);

            // Verify metrics show good performance
            assertThat(meterRegistry.timer("epic3.dependency.processing_time").max(TimeUnit.MILLISECONDS))
                .isLessThan(10000);
        }

        @Test
        @DisplayName("Should maintain performance under memory pressure")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        @Transactional
        void shouldMaintainPerformanceUnderMemoryPressure() {
            // Given - Multiple large transaction sets processed sequentially
            int iterations = 5;
            int transactionsPerIteration = 200;
            
            for (int i = 0; i < iterations; i++) {
                String executionId = "MEMORY-TEST-" + i;
                Set<String> transactions = createLargeTransactionSet(transactionsPerIteration, "MEM-" + i + "-");
                List<TransactionDependencyEntity> dependencies = 
                    createScalableRandomDependencies(transactions, 0.05); // 5% dependency ratio
                
                dependencyRepository.saveAll(dependencies);

                // When - Process each iteration
                long iterationStart = System.nanoTime();
                TransactionDependencyService.DependencyResolutionResult result = 
                    dependencyService.buildExecutionGraph(executionId, transactions, TEST_CORRELATION_ID);
                long iterationEnd = System.nanoTime();

                // Then - Each iteration should succeed
                assertThat(result.isSuccessful()).isTrue();
                
                long iterationDurationMs = TimeUnit.NANOSECONDS.toMillis(iterationEnd - iterationStart);
                assertThat(iterationDurationMs).isLessThan(8000); // Performance should not degrade significantly
                
                // Clean up to simulate memory pressure
                if (i % 2 == 0) {
                    dependencyService.clearExecutionGraph(executionId);
                }
            }
        }
    }

    @Nested
    @DisplayName("Error Handling and Recovery Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle and recover from dependency cycles")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        @Transactional
        void shouldHandleAndRecoverFromDependencyCycles() {
            // Given - Cyclic dependencies
            Set<String> transactions = Set.of("CYC-A", "CYC-B", "CYC-C");
            List<TransactionDependencyEntity> cyclicDependencies = List.of(
                createDependency("CYC-A", "CYC-B"),
                createDependency("CYC-B", "CYC-C"),
                createDependency("CYC-C", "CYC-A") // Creates cycle
            );
            dependencyRepository.saveAll(cyclicDependencies);

            // When - Attempt to build execution graph
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(TEST_EXECUTION_ID, transactions, TEST_CORRELATION_ID);

            // Then - Should detect cycles and fail gracefully
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getCycleResult()).isNotNull();
            assertThat(result.getCycleResult().hasCycles()).isTrue();
            assertThat(result.getCycleResult().getCyclePaths()).isNotEmpty();

            // Verify no partial data was persisted
            List<TransactionExecutionGraphEntity> persistedNodes = 
                graphRepository.findByExecutionIdOrderByTopologicalOrder(TEST_EXECUTION_ID);
            assertThat(persistedNodes).isEmpty();

            // Verify error metrics were recorded
            assertThat(meterRegistry.counter("epic3.dependency.cycle_detection").count()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle database constraint violations gracefully")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        @Transactional
        void shouldHandleDatabaseConstraintViolationsGracefully() {
            // Given - Attempt to create duplicate dependencies
            Set<String> transactions = Set.of("DUP-A", "DUP-B");
            TransactionDependencyEntity dependency1 = createDependency("DUP-A", "DUP-B");
            TransactionDependencyEntity dependency2 = createDependency("DUP-A", "DUP-B"); // Duplicate
            
            dependencyRepository.save(dependency1);

            // When & Then - Duplicate should be handled gracefully
            assertThatThrownBy(() -> dependencyRepository.save(dependency2))
                .isInstanceOf(Exception.class); // Constraint violation expected

            // Verify system remains in consistent state
            List<TransactionDependencyEntity> allDependencies = dependencyRepository.findAll();
            assertThat(allDependencies).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Banking Compliance and Security Tests")
    class ComplianceTests {

        @Test
        @DisplayName("Should maintain audit trails for all operations")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        @Transactional
        void shouldMaintainAuditTrailsForAllOperations() {
            // Given
            Set<String> transactions = Set.of("AUDIT-A", "AUDIT-B", "AUDIT-C");
            List<TransactionDependencyEntity> dependencies = createLinearDependencies(transactions);
            
            // Set compliance metadata
            dependencies.forEach(dep -> {
                dep.setComplianceLevel("HIGH");
                dep.setBusinessJustification("Banking regulation compliance test");
                dep.setCreatedBy("COMPLIANCE_USER");
            });
            
            dependencyRepository.saveAll(dependencies);

            // When - Perform operations
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(TEST_EXECUTION_ID, transactions, TEST_CORRELATION_ID);

            // Then - Verify audit information is preserved
            assertThat(result.isSuccessful()).isTrue();
            
            List<TransactionExecutionGraphEntity> graphNodes = 
                graphRepository.findByExecutionIdOrderByTopologicalOrder(TEST_EXECUTION_ID);
            
            // Verify correlation ID is maintained for audit tracking
            assertThat(graphNodes).allMatch(node -> 
                TEST_CORRELATION_ID.equals(node.getCorrelationId()));
            
            // Verify business date is set for compliance reporting
            assertThat(graphNodes).allMatch(node -> node.getBusinessDate() != null);
        }

        @Test
        @DisplayName("Should enforce access controls for sensitive operations")
        @WithMockUser(roles = {"READ_ONLY"})
        void shouldEnforceAccessControlsForSensitiveOperations() {
            // Given
            Set<String> transactions = Set.of("SEC-A", "SEC-B");

            // When & Then - Should deny access without proper roles
            assertThatThrownBy(() -> 
                dependencyService.buildExecutionGraph(TEST_EXECUTION_ID, transactions, TEST_CORRELATION_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should validate compliance level requirements")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        @Transactional
        void shouldValidateComplianceLevelRequirements() {
            // Given - High compliance transactions
            Set<String> transactions = Set.of("COMP-A", "COMP-B", "COMP-C");
            List<TransactionDependencyEntity> dependencies = createLinearDependencies(transactions);
            
            dependencies.forEach(dep -> {
                dep.setComplianceLevel("CRITICAL");
                dep.setBusinessJustification("PCI-DSS Level 1 compliance required");
            });
            
            dependencyRepository.saveAll(dependencies);

            // When
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(TEST_EXECUTION_ID, transactions, TEST_CORRELATION_ID);

            // Then - Should handle high compliance requirements
            assertThat(result.isSuccessful()).isTrue();
            
            // Verify compliance metadata is preserved in execution graph
            List<TransactionExecutionGraphEntity> graphNodes = result.getGraphNodes();
            assertThat(graphNodes).isNotEmpty();
            
            // All nodes should have proper audit trail
            assertThat(graphNodes).allMatch(node -> 
                node.getCorrelationId() != null && 
                node.getBusinessDate() != null);
        }
    }

    // Helper methods for test data creation

    private Set<String> createComplexTransactionSet() {
        return Set.of("COMP-START", "COMP-A1", "COMP-A2", "COMP-B1", "COMP-B2", 
                     "COMP-MERGE", "COMP-C", "COMP-END");
    }

    private List<TransactionDependencyEntity> createComplexDependencySet(Set<String> transactions) {
        return List.of(
            // Diamond dependency pattern
            createDependency("COMP-START", "COMP-A1"),
            createDependency("COMP-START", "COMP-A2"),
            createDependency("COMP-A1", "COMP-B1"),
            createDependency("COMP-A2", "COMP-B2"),
            createDependency("COMP-B1", "COMP-MERGE"),
            createDependency("COMP-B2", "COMP-MERGE"),
            createDependency("COMP-MERGE", "COMP-C"),
            createDependency("COMP-C", "COMP-END")
        );
    }

    private Set<String> createLargeTransactionSet(int size) {
        return createLargeTransactionSet(size, "LARGE-TXN-");
    }

    private Set<String> createLargeTransactionSet(int size, String prefix) {
        Set<String> transactions = new HashSet<>();
        for (int i = 0; i < size; i++) {
            transactions.add(String.format("%s%04d", prefix, i));
        }
        return transactions;
    }

    private List<TransactionDependencyEntity> createScalableRandomDependencies(
            Set<String> transactions, double dependencyRatio) {
        List<TransactionDependencyEntity> dependencies = new ArrayList<>();
        List<String> transactionList = new ArrayList<>(transactions);
        Random random = new Random(12345); // Fixed seed for reproducible tests
        
        int targetDependencies = (int) (transactions.size() * dependencyRatio);
        
        for (int i = 0; i < targetDependencies; i++) {
            String source = transactionList.get(random.nextInt(transactionList.size()));
            String target = transactionList.get(random.nextInt(transactionList.size()));
            
            if (!source.equals(target)) { // Avoid self-dependencies
                dependencies.add(createDependency(source, target));
            }
        }
        
        return dependencies;
    }

    private List<TransactionDependencyEntity> createLinearDependencies(Set<String> transactions) {
        List<TransactionDependencyEntity> dependencies = new ArrayList<>();
        List<String> transactionList = new ArrayList<>(transactions);
        Collections.sort(transactionList); // Ensure consistent ordering
        
        for (int i = 0; i < transactionList.size() - 1; i++) {
            dependencies.add(createDependency(transactionList.get(i), transactionList.get(i + 1)));
        }
        
        return dependencies;
    }

    private TransactionDependencyEntity createDependency(String source, String target) {
        TransactionDependencyEntity dependency = new TransactionDependencyEntity();
        dependency.setSourceTransactionId(source);
        dependency.setTargetTransactionId(target);
        dependency.setDependencyType(TransactionDependencyEntity.DependencyType.SEQUENTIAL);
        dependency.setPriorityWeight(1);
        dependency.setMaxWaitTimeSeconds(3600);
        dependency.setRetryPolicy("EXPONENTIAL_BACKOFF");
        dependency.setActiveFlag("Y");
        dependency.setComplianceLevel("STANDARD");
        dependency.setCreatedBy("TEST_USER");
        dependency.setCreatedDate(LocalDateTime.now());
        dependency.setBusinessJustification("Integration test dependency");
        return dependency;
    }
}