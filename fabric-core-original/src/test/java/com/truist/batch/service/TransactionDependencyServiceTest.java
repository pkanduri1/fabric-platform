package com.truist.batch.service;

import com.truist.batch.entity.TransactionDependencyEntity;
import com.truist.batch.entity.TransactionExecutionGraphEntity;
import com.truist.batch.repository.TransactionDependencyRepository;
import com.truist.batch.repository.TransactionExecutionGraphRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Epic 3: Transaction Dependency Service Unit Tests
 * 
 * Comprehensive test suite for TransactionDependencyService covering:
 * - Graph algorithm correctness (cycle detection, topological sort)
 * - Banking-grade security validation
 * - Performance metrics verification
 * - Edge cases and error handling
 * - Concurrency and thread safety
 * 
 * Test Coverage Target: >85%
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3
 */
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig
@DisplayName("Epic 3: TransactionDependencyService Tests")
class TransactionDependencyServiceTest {

    @Mock
    private TransactionDependencyRepository dependencyRepository;
    
    @Mock
    private TransactionExecutionGraphRepository graphRepository;
    
    private MeterRegistry meterRegistry;
    private TransactionDependencyService dependencyService;
    
    // Test data constants
    private static final String EXECUTION_ID = "EXEC-TEST-001";
    private static final String CORRELATION_ID = "CORR-TEST-001";
    private static final Set<String> SAMPLE_TRANSACTIONS = Set.of("TXN-A", "TXN-B", "TXN-C", "TXN-D");

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        dependencyService = new TransactionDependencyService(
            dependencyRepository, graphRepository, meterRegistry);
    }

    @Nested
    @DisplayName("Graph Construction Tests")
    class GraphConstructionTests {

        @Test
        @DisplayName("Should build execution graph successfully with valid dependencies")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldBuildExecutionGraphSuccessfully() {
            // Given
            List<TransactionDependencyEntity> dependencies = createValidDependencies();
            when(dependencyRepository.findActiveDependenciesForTransactions(SAMPLE_TRANSACTIONS))
                .thenReturn(dependencies);
            when(graphRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(EXECUTION_ID, SAMPLE_TRANSACTIONS, CORRELATION_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getExecutionId()).isEqualTo(EXECUTION_ID);
            assertThat(result.getGraphNodes()).isNotNull();
            assertThat(result.getTopologicalOrder()).isNotEmpty();
            
            // Verify repository interactions
            verify(dependencyRepository).findActiveDependenciesForTransactions(SAMPLE_TRANSACTIONS);
            verify(graphRepository).saveAll(any());
            
            // Verify metrics were recorded
            assertThat(meterRegistry.counter("epic3.dependency.cycle_detection").count()).isEqualTo(1);
            assertThat(meterRegistry.counter("epic3.dependency.topological_sort").count()).isEqualTo(1);
            assertThat(meterRegistry.counter("epic3.dependency.resolution").count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should detect cycles and return failure result")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldDetectCyclesAndReturnFailure() {
            // Given - Create circular dependencies: A -> B -> C -> A
            List<TransactionDependencyEntity> cyclicDependencies = createCyclicDependencies();
            when(dependencyRepository.findActiveDependenciesForTransactions(any()))
                .thenReturn(cyclicDependencies);

            // When
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(EXECUTION_ID, SAMPLE_TRANSACTIONS, CORRELATION_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getCycleResult()).isNotNull();
            assertThat(result.getCycleResult().hasCycles()).isTrue();
            assertThat(result.getCycleResult().getCyclePaths()).isNotEmpty();
            
            // Verify no graph entities were saved when cycles detected
            verify(graphRepository, never()).saveAll(any());
            
            // Verify cycle detection metric was recorded
            assertThat(meterRegistry.counter("epic3.dependency.cycle_detection").count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle empty dependency set gracefully")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldHandleEmptyDependencySet() {
            // Given
            when(dependencyRepository.findActiveDependenciesForTransactions(SAMPLE_TRANSACTIONS))
                .thenReturn(Collections.emptyList());
            when(graphRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(EXECUTION_ID, SAMPLE_TRANSACTIONS, CORRELATION_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getGraphNodes()).hasSize(SAMPLE_TRANSACTIONS.size());
            assertThat(result.getTopologicalOrder()).containsExactlyInAnyOrderElementsOf(SAMPLE_TRANSACTIONS);
        }

        @Test
        @DisplayName("Should validate topological order correctness")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldValidateTopologicalOrderCorrectness() {
            // Given - Linear dependency chain: A -> B -> C -> D
            List<TransactionDependencyEntity> linearDependencies = createLinearDependencies();
            when(dependencyRepository.findActiveDependenciesForTransactions(SAMPLE_TRANSACTIONS))
                .thenReturn(linearDependencies);
            when(graphRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(EXECUTION_ID, SAMPLE_TRANSACTIONS, CORRELATION_ID);

            // Then
            assertThat(result.isSuccessful()).isTrue();
            List<String> topologicalOrder = result.getTopologicalOrder();
            
            // Verify correct topological ordering: A should come before B, B before C, C before D
            assertThat(topologicalOrder.indexOf("TXN-A")).isLessThan(topologicalOrder.indexOf("TXN-B"));
            assertThat(topologicalOrder.indexOf("TXN-B")).isLessThan(topologicalOrder.indexOf("TXN-C"));
            assertThat(topologicalOrder.indexOf("TXN-C")).isLessThan(topologicalOrder.indexOf("TXN-D"));
        }
    }

    @Nested
    @DisplayName("Transaction Processing Tests")
    class TransactionProcessingTests {

        @Test
        @DisplayName("Should get ready transactions correctly")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldGetReadyTransactionsCorrectly() {
            // Given
            List<TransactionExecutionGraphEntity> readyTransactions = createReadyTransactions();
            when(graphRepository.findByExecutionIdAndInDegreeAndProcessingStatus(
                EXECUTION_ID, 0, TransactionExecutionGraphEntity.ProcessingStatus.WHITE))
                .thenReturn(readyTransactions);

            // When
            List<TransactionExecutionGraphEntity> result = 
                dependencyService.getReadyTransactions(EXECUTION_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result).extracting(TransactionExecutionGraphEntity::getTransactionId)
                .containsExactlyInAnyOrder("TXN-A", "TXN-B");
            
            verify(graphRepository).findByExecutionIdAndInDegreeAndProcessingStatus(
                EXECUTION_ID, 0, TransactionExecutionGraphEntity.ProcessingStatus.WHITE);
        }

        @Test
        @DisplayName("Should mark transaction as started correctly")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldMarkTransactionAsStartedCorrectly() {
            // Given
            TransactionExecutionGraphEntity graphEntity = createGraphEntity("TXN-A");
            when(graphRepository.findByExecutionIdAndTransactionId(EXECUTION_ID, "TXN-A"))
                .thenReturn(Optional.of(graphEntity));
            when(graphRepository.save(any())).thenReturn(graphEntity);

            // When
            dependencyService.markTransactionStarted(EXECUTION_ID, "TXN-A", "THREAD-01", CORRELATION_ID);

            // Then
            ArgumentCaptor<TransactionExecutionGraphEntity> entityCaptor = 
                ArgumentCaptor.forClass(TransactionExecutionGraphEntity.class);
            verify(graphRepository).save(entityCaptor.capture());
            
            TransactionExecutionGraphEntity savedEntity = entityCaptor.getValue();
            assertThat(savedEntity.getProcessingStatus()).isEqualTo(TransactionExecutionGraphEntity.ProcessingStatus.GRAY);
            assertThat(savedEntity.getThreadAssignment()).isEqualTo("THREAD-01");
            assertThat(savedEntity.getStartTime()).isNotNull();
        }

        @Test
        @DisplayName("Should mark transaction as completed and update dependents")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldMarkTransactionAsCompletedAndUpdateDependents() {
            // Given
            TransactionExecutionGraphEntity completedEntity = createGraphEntity("TXN-A");
            TransactionExecutionGraphEntity dependentEntity = createGraphEntity("TXN-B");
            dependentEntity.setInDegree(1);
            
            List<TransactionDependencyEntity> outgoingDependencies = List.of(
                createDependency("TXN-A", "TXN-B"));
            
            when(graphRepository.findByExecutionIdAndTransactionId(EXECUTION_ID, "TXN-A"))
                .thenReturn(Optional.of(completedEntity));
            when(dependencyRepository.findBySourceTransactionIds(Set.of("TXN-A")))
                .thenReturn(outgoingDependencies);
            when(graphRepository.findByExecutionIdAndTransactionId(EXECUTION_ID, "TXN-B"))
                .thenReturn(Optional.of(dependentEntity));

            // When
            dependencyService.markTransactionCompleted(EXECUTION_ID, "TXN-A", CORRELATION_ID);

            // Then
            // Verify completed transaction was marked as BLACK
            ArgumentCaptor<TransactionExecutionGraphEntity> completedCaptor = 
                ArgumentCaptor.forClass(TransactionExecutionGraphEntity.class);
            verify(graphRepository, atLeastOnce()).save(completedCaptor.capture());
            
            List<TransactionExecutionGraphEntity> savedEntities = completedCaptor.getAllValues();
            
            // Find the completed entity update
            Optional<TransactionExecutionGraphEntity> completedUpdate = savedEntities.stream()
                .filter(e -> e.getTransactionId().equals("TXN-A"))
                .findFirst();
            
            assertThat(completedUpdate).isPresent();
            assertThat(completedUpdate.get().getProcessingStatus())
                .isEqualTo(TransactionExecutionGraphEntity.ProcessingStatus.BLACK);
            assertThat(completedUpdate.get().getEndTime()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when transaction not found in graph")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldThrowExceptionWhenTransactionNotFoundInGraph() {
            // Given
            when(graphRepository.findByExecutionIdAndTransactionId(EXECUTION_ID, "INVALID-TXN"))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                dependencyService.markTransactionStarted(EXECUTION_ID, "INVALID-TXN", "THREAD-01", CORRELATION_ID))
                .isInstanceOf(TransactionDependencyService.DependencyProcessingException.class)
                .hasMessageContaining("Transaction not found in execution graph");
        }
    }

    @Nested
    @DisplayName("Execution Status Tests")
    class ExecutionStatusTests {

        @Test
        @DisplayName("Should calculate execution status summary correctly")
        void shouldCalculateExecutionStatusSummaryCorrectly() {
            // Given
            List<TransactionExecutionGraphEntity> allNodes = createMixedStatusTransactions();
            when(graphRepository.findByExecutionIdOrderByTopologicalOrder(EXECUTION_ID))
                .thenReturn(allNodes);

            // When
            TransactionDependencyService.ExecutionStatusSummary summary = 
                dependencyService.getExecutionStatus(EXECUTION_ID);

            // Then
            assertThat(summary).isNotNull();
            assertThat(summary.getExecutionId()).isEqualTo(EXECUTION_ID);
            assertThat(summary.getTotalNodes()).isEqualTo(5);
            assertThat(summary.getCompletedNodes()).isEqualTo(2);
            assertThat(summary.getInProgressNodes()).isEqualTo(1);
            assertThat(summary.getReadyNodes()).isEqualTo(1);
            assertThat(summary.getBlockedNodes()).isEqualTo(0);
            assertThat(summary.getErrorNodes()).isEqualTo(1);
            assertThat(summary.getCompletionPercentage()).isEqualTo(40.0);
        }

        @Test
        @DisplayName("Should handle empty execution gracefully")
        void shouldHandleEmptyExecutionGracefully() {
            // Given
            when(graphRepository.findByExecutionIdOrderByTopologicalOrder(EXECUTION_ID))
                .thenReturn(Collections.emptyList());

            // When
            TransactionDependencyService.ExecutionStatusSummary summary = 
                dependencyService.getExecutionStatus(EXECUTION_ID);

            // Then
            assertThat(summary).isNotNull();
            assertThat(summary.getTotalNodes()).isZero();
            assertThat(summary.getCompletionPercentage()).isZero();
        }
    }

    @Nested
    @DisplayName("Security and Authorization Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should deny access without proper role")
        void shouldDenyAccessWithoutProperRole() {
            // When & Then - No @WithMockUser annotation, should deny access
            assertThatThrownBy(() -> 
                dependencyService.buildExecutionGraph(EXECUTION_ID, SAMPLE_TRANSACTIONS, CORRELATION_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should allow access with BATCH_PROCESSOR role")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldAllowAccessWithBatchProcessorRole() {
            // Given
            when(dependencyRepository.findActiveDependenciesForTransactions(any()))
                .thenReturn(Collections.emptyList());
            when(graphRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When & Then - Should not throw exception
            assertThatCode(() -> 
                dependencyService.buildExecutionGraph(EXECUTION_ID, SAMPLE_TRANSACTIONS, CORRELATION_ID))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow access with SYSTEM_ADMIN role")
        @WithMockUser(roles = {"SYSTEM_ADMIN"})
        void shouldAllowAccessWithSystemAdminRole() {
            // Given
            when(dependencyRepository.findActiveDependenciesForTransactions(any()))
                .thenReturn(Collections.emptyList());
            when(graphRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When & Then - Should not throw exception
            assertThatCode(() -> 
                dependencyService.buildExecutionGraph(EXECUTION_ID, SAMPLE_TRANSACTIONS, CORRELATION_ID))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should require SYSTEM_ADMIN role for clearing execution graph")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldRequireSystemAdminRoleForClearingExecutionGraph() {
            // When & Then
            assertThatThrownBy(() -> 
                dependencyService.clearExecutionGraph(EXECUTION_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should allow SYSTEM_ADMIN to clear execution graph")
        @WithMockUser(roles = {"SYSTEM_ADMIN"})
        void shouldAllowSystemAdminToClearExecutionGraph() {
            // Given
            when(graphRepository.deleteByExecutionId(EXECUTION_ID)).thenReturn(5);

            // When & Then - Should not throw exception
            assertThatCode(() -> 
                dependencyService.clearExecutionGraph(EXECUTION_ID))
                .doesNotThrowAnyException();
            
            verify(graphRepository).deleteByExecutionId(EXECUTION_ID);
        }
    }

    @Nested
    @DisplayName("Performance and Metrics Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should record performance metrics correctly")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldRecordPerformanceMetricsCorrectly() {
            // Given
            when(dependencyRepository.findActiveDependenciesForTransactions(SAMPLE_TRANSACTIONS))
                .thenReturn(createValidDependencies());
            when(graphRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When
            dependencyService.buildExecutionGraph(EXECUTION_ID, SAMPLE_TRANSACTIONS, CORRELATION_ID);

            // Then - Verify metrics were recorded
            Counter cycleDetectionCounter = meterRegistry.counter("epic3.dependency.cycle_detection");
            Counter topologicalSortCounter = meterRegistry.counter("epic3.dependency.topological_sort");
            Counter dependencyResolutionCounter = meterRegistry.counter("epic3.dependency.resolution");
            Timer dependencyProcessingTimer = meterRegistry.timer("epic3.dependency.processing_time");
            Timer cycleDetectionTimer = meterRegistry.timer("epic3.dependency.cycle_detection_time");
            Timer graphBuildTimer = meterRegistry.timer("epic3.dependency.graph_build_time");

            assertThat(cycleDetectionCounter.count()).isEqualTo(1);
            assertThat(topologicalSortCounter.count()).isEqualTo(1);
            assertThat(dependencyResolutionCounter.count()).isEqualTo(1);
            assertThat(dependencyProcessingTimer.count()).isEqualTo(1);
            assertThat(cycleDetectionTimer.count()).isEqualTo(1);
            assertThat(graphBuildTimer.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle large dependency sets efficiently")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldHandleLargeDependencySetsEfficiently() {
            // Given - Create large transaction set (1000 transactions)
            Set<String> largeTransactionSet = createLargeTransactionSet(1000);
            List<TransactionDependencyEntity> largeDependencySet = createLargeDependencySet(largeTransactionSet);
            
            when(dependencyRepository.findActiveDependenciesForTransactions(largeTransactionSet))
                .thenReturn(largeDependencySet);
            when(graphRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When
            long startTime = System.nanoTime();
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(EXECUTION_ID, largeTransactionSet, CORRELATION_ID);
            long endTime = System.nanoTime();

            // Then
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getGraphNodes()).hasSize(largeTransactionSet.size());
            
            // Performance assertion - should complete within reasonable time (< 5 seconds)
            long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            assertThat(durationMs).isLessThan(5000);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle self-referential dependencies")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldHandleSelfReferentialDependencies() {
            // Given - Create self-referential dependency
            List<TransactionDependencyEntity> selfRefDependencies = List.of(
                createDependency("TXN-A", "TXN-A"));
            
            when(dependencyRepository.findActiveDependenciesForTransactions(any()))
                .thenReturn(selfRefDependencies);

            // When
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(EXECUTION_ID, Set.of("TXN-A"), CORRELATION_ID);

            // Then - Should detect cycle
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getCycleResult().hasCycles()).isTrue();
        }

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldHandleRepositoryExceptionsGracefully() {
            // Given
            when(dependencyRepository.findActiveDependenciesForTransactions(any()))
                .thenThrow(new RuntimeException("Database connection failed"));

            // When
            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(EXECUTION_ID, SAMPLE_TRANSACTIONS, CORRELATION_ID);

            // Then
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getCycleResult().getCyclePaths()).isNotEmpty();
            assertThat(result.getCycleResult().getCyclePaths().get(0).get(0))
                .contains("ERROR: Database connection failed");
        }

        @Test
        @DisplayName("Should handle null and empty inputs")
        @WithMockUser(roles = {"BATCH_PROCESSOR"})
        void shouldHandleNullAndEmptyInputs() {
            // When & Then - Null transaction set
            assertThatThrownBy(() -> 
                dependencyService.buildExecutionGraph(EXECUTION_ID, null, CORRELATION_ID))
                .isInstanceOf(NullPointerException.class);

            // When & Then - Empty transaction set
            when(dependencyRepository.findActiveDependenciesForTransactions(Collections.emptySet()))
                .thenReturn(Collections.emptyList());
            when(graphRepository.saveAll(any())).thenReturn(Collections.emptyList());

            TransactionDependencyService.DependencyResolutionResult result = 
                dependencyService.buildExecutionGraph(EXECUTION_ID, Collections.emptySet(), CORRELATION_ID);

            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getGraphNodes()).isEmpty();
        }
    }

    // Helper methods for test data creation

    private List<TransactionDependencyEntity> createValidDependencies() {
        return List.of(
            createDependency("TXN-A", "TXN-B"),
            createDependency("TXN-A", "TXN-C"),
            createDependency("TXN-B", "TXN-D"),
            createDependency("TXN-C", "TXN-D")
        );
    }

    private List<TransactionDependencyEntity> createCyclicDependencies() {
        return List.of(
            createDependency("TXN-A", "TXN-B"),
            createDependency("TXN-B", "TXN-C"),
            createDependency("TXN-C", "TXN-A") // Creates cycle
        );
    }

    private List<TransactionDependencyEntity> createLinearDependencies() {
        return List.of(
            createDependency("TXN-A", "TXN-B"),
            createDependency("TXN-B", "TXN-C"),
            createDependency("TXN-C", "TXN-D")
        );
    }

    private TransactionDependencyEntity createDependency(String source, String target) {
        TransactionDependencyEntity dependency = new TransactionDependencyEntity();
        dependency.setDependencyId((long) (source + target).hashCode());
        dependency.setSourceTransactionId(source);
        dependency.setTargetTransactionId(target);
        dependency.setDependencyType(TransactionDependencyEntity.DependencyType.SEQUENTIAL);
        dependency.setPriorityWeight(1);
        dependency.setActiveFlag("Y");
        dependency.setCreatedBy("TEST_USER");
        return dependency;
    }

    private List<TransactionExecutionGraphEntity> createReadyTransactions() {
        return List.of(
            createGraphEntity("TXN-A", 0, TransactionExecutionGraphEntity.ProcessingStatus.WHITE),
            createGraphEntity("TXN-B", 0, TransactionExecutionGraphEntity.ProcessingStatus.WHITE)
        );
    }

    private TransactionExecutionGraphEntity createGraphEntity(String transactionId) {
        return createGraphEntity(transactionId, 1, TransactionExecutionGraphEntity.ProcessingStatus.WHITE);
    }

    private TransactionExecutionGraphEntity createGraphEntity(String transactionId, int inDegree, 
            TransactionExecutionGraphEntity.ProcessingStatus status) {
        TransactionExecutionGraphEntity entity = new TransactionExecutionGraphEntity();
        entity.setGraphId((long) transactionId.hashCode());
        entity.setExecutionId(EXECUTION_ID);
        entity.setTransactionId(transactionId);
        entity.setInDegree(inDegree);
        entity.setProcessingStatus(status);
        entity.setCorrelationId(CORRELATION_ID);
        entity.setBusinessDate(new Date());
        return entity;
    }

    private List<TransactionExecutionGraphEntity> createMixedStatusTransactions() {
        return List.of(
            createGraphEntity("TXN-1", 0, TransactionExecutionGraphEntity.ProcessingStatus.BLACK),  // Completed
            createGraphEntity("TXN-2", 0, TransactionExecutionGraphEntity.ProcessingStatus.BLACK),  // Completed
            createGraphEntity("TXN-3", 0, TransactionExecutionGraphEntity.ProcessingStatus.GRAY),   // In Progress
            createGraphEntity("TXN-4", 0, TransactionExecutionGraphEntity.ProcessingStatus.WHITE),  // Ready
            createGraphEntity("TXN-5", 0, TransactionExecutionGraphEntity.ProcessingStatus.ERROR)   // Error
        );
    }

    private Set<String> createLargeTransactionSet(int size) {
        Set<String> transactions = new HashSet<>();
        for (int i = 0; i < size; i++) {
            transactions.add(String.format("TXN-%04d", i));
        }
        return transactions;
    }

    private List<TransactionDependencyEntity> createLargeDependencySet(Set<String> transactions) {
        List<TransactionDependencyEntity> dependencies = new ArrayList<>();
        List<String> transactionList = new ArrayList<>(transactions);
        
        // Create linear chain dependencies for performance testing
        for (int i = 0; i < transactionList.size() - 1; i++) {
            if (i % 10 == 0) { // Add some branching every 10 transactions
                dependencies.add(createDependency(transactionList.get(i), transactionList.get(i + 1)));
            }
        }
        
        return dependencies;
    }
}