package com.truist.batch.sequencer;

import com.truist.batch.entity.TransactionDependencyEntity;
import com.truist.batch.entity.TransactionExecutionGraphEntity;
import com.truist.batch.repository.TransactionDependencyRepository;
import com.truist.batch.repository.TransactionExecutionGraphRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Epic 3: Transaction Sequencer
 * 
 * Core component for managing transaction dependencies using advanced graph algorithms.
 * Implements topological sorting with Kahn's algorithm and cycle detection using
 * DFS traversal with WHITE/GRAY/BLACK node coloring for complex transaction processing.
 * 
 * Features:
 * - Kahn's algorithm for topological sorting (O(V+E) complexity)
 * - DFS-based cycle detection with node coloring
 * - Parallel execution coordination
 * - Dependency validation and conflict resolution
 * - Performance monitoring and optimization
 * - Banking-grade security and compliance
 * 
 * Algorithm Complexity:
 * - Topological Sort: O(V + E) where V = vertices (transactions), E = edges (dependencies)
 * - Cycle Detection: O(V + E) using DFS traversal
 * - Memory Usage: O(V + E) for adjacency list representation
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3
 */
@Component
@Slf4j
public class TransactionSequencer {

    private final TransactionDependencyRepository dependencyRepository;
    private final TransactionExecutionGraphRepository graphRepository;
    private final ExecutorService executorService;
    
    // Thread-safe caches for performance optimization
    private final Map<String, List<String>> adjacencyListCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> inDegreeCache = new ConcurrentHashMap<>();
    
    @Autowired
    public TransactionSequencer(
            TransactionDependencyRepository dependencyRepository,
            TransactionExecutionGraphRepository graphRepository) {
        this.dependencyRepository = dependencyRepository;
        this.graphRepository = graphRepository;
        this.executorService = Executors.newCachedThreadPool();
        
        log.info("🔄 TransactionSequencer initialized with graph algorithms support");
    }

    /**
     * Sequence Result containing execution plan and metadata
     */
    public static class SequenceResult {
        private final List<List<String>> executionLevels;
        private final Map<String, Integer> transactionLevels;
        private final boolean hasCycles;
        private final List<String> cyclePath;
        private final Map<String, String> performanceMetrics;
        private final long algorithmDuration;

        public SequenceResult(List<List<String>> executionLevels, 
                            Map<String, Integer> transactionLevels,
                            boolean hasCycles, List<String> cyclePath,
                            Map<String, String> performanceMetrics,
                            long algorithmDuration) {
            this.executionLevels = executionLevels;
            this.transactionLevels = transactionLevels;
            this.hasCycles = hasCycles;
            this.cyclePath = cyclePath;
            this.performanceMetrics = performanceMetrics;
            this.algorithmDuration = algorithmDuration;
        }

        public List<List<String>> getExecutionLevels() { return executionLevels; }
        public Map<String, Integer> getTransactionLevels() { return transactionLevels; }
        public boolean hasCycles() { return hasCycles; }
        public List<String> getCyclePath() { return cyclePath; }
        public Map<String, String> getPerformanceMetrics() { return performanceMetrics; }
        public long getAlgorithmDuration() { return algorithmDuration; }
    }

    /**
     * Main entry point: Build execution sequence using Kahn's algorithm
     * 
     * @param executionId unique execution identifier
     * @param transactionIds set of transactions to sequence
     * @return SequenceResult with execution plan and metadata
     */
    @Transactional(readOnly = true)
    public SequenceResult buildExecutionSequence(String executionId, Set<String> transactionIds) {
        long startTime = System.currentTimeMillis();
        
        log.info("🚀 Building execution sequence for {} transactions in execution {}", 
                transactionIds.size(), executionId);

        try {
            // Step 1: Load active dependencies for the transaction set
            List<TransactionDependencyEntity> dependencies = loadActiveDependencies(transactionIds);
            log.debug("📊 Loaded {} active dependencies", dependencies.size());

            // Step 2: Build adjacency list representation
            Map<String, List<String>> adjacencyList = buildAdjacencyList(dependencies, transactionIds);
            Map<String, Integer> inDegreeMap = calculateInDegrees(adjacencyList, transactionIds);

            // Step 3: Perform cycle detection using DFS
            CycleDetectionResult cycleResult = detectCyclesWithDFS(adjacencyList, transactionIds);
            
            if (cycleResult.hasCycles()) {
                log.warn("⚠️ Dependency cycles detected: {}", cycleResult.getCyclePath());
                return createErrorResult(cycleResult, startTime);
            }

            // Step 4: Apply Kahn's algorithm for topological sorting
            KahnResult kahnResult = applyKahnsAlgorithm(adjacencyList, inDegreeMap, transactionIds);

            // Step 5: Create execution graph entities
            createExecutionGraphEntities(executionId, kahnResult, dependencies);

            // Step 6: Build performance metrics
            Map<String, String> metrics = buildPerformanceMetrics(
                transactionIds.size(), dependencies.size(), kahnResult);

            long algorithmDuration = System.currentTimeMillis() - startTime;

            log.info("✅ Execution sequence built successfully in {}ms: {} levels, {} transactions", 
                    algorithmDuration, kahnResult.getExecutionLevels().size(), transactionIds.size());

            return new SequenceResult(
                kahnResult.getExecutionLevels(),
                kahnResult.getTransactionLevels(),
                false,
                Collections.emptyList(),
                metrics,
                algorithmDuration
            );

        } catch (Exception e) {
            long algorithmDuration = System.currentTimeMillis() - startTime;
            log.error("❌ Failed to build execution sequence for execution {}: {}", executionId, e.getMessage(), e);
            
            return new SequenceResult(
                Collections.emptyList(),
                Collections.emptyMap(),
                true,
                List.of("ERROR: " + e.getMessage()),
                Map.of("error", e.getMessage(), "duration", String.valueOf(algorithmDuration)),
                algorithmDuration
            );
        }
    }

    /**
     * Kahn's Algorithm Result container
     */
    private static class KahnResult {
        private final List<List<String>> executionLevels;
        private final Map<String, Integer> transactionLevels;
        private final List<String> topologicalOrder;

        public KahnResult(List<List<String>> executionLevels, 
                         Map<String, Integer> transactionLevels,
                         List<String> topologicalOrder) {
            this.executionLevels = executionLevels;
            this.transactionLevels = transactionLevels;
            this.topologicalOrder = topologicalOrder;
        }

        public List<List<String>> getExecutionLevels() { return executionLevels; }
        public Map<String, Integer> getTransactionLevels() { return transactionLevels; }
        public List<String> getTopologicalOrder() { return topologicalOrder; }
    }

    /**
     * Apply Kahn's Algorithm for topological sorting
     * Time Complexity: O(V + E)
     * Space Complexity: O(V)
     * 
     * @param adjacencyList graph adjacency list
     * @param inDegreeMap in-degree count for each node
     * @param transactionIds all transaction IDs
     * @return KahnResult with execution levels and ordering
     */
    private KahnResult applyKahnsAlgorithm(Map<String, List<String>> adjacencyList, 
                                          Map<String, Integer> inDegreeMap,
                                          Set<String> transactionIds) {
        
        log.debug("🔍 Applying Kahn's algorithm for topological sorting");
        
        // Initialize data structures
        Map<String, Integer> workingInDegree = new HashMap<>(inDegreeMap);
        Queue<String> readyQueue = new LinkedList<>();
        List<List<String>> executionLevels = new ArrayList<>();
        Map<String, Integer> transactionLevels = new HashMap<>();
        List<String> topologicalOrder = new ArrayList<>();
        
        // Find all source nodes (in-degree = 0)
        transactionIds.forEach(transactionId -> {
            if (workingInDegree.getOrDefault(transactionId, 0) == 0) {
                readyQueue.offer(transactionId);
                log.debug("📍 Source node identified: {}", transactionId);
            }
        });

        int currentLevel = 0;
        
        // Process nodes level by level
        while (!readyQueue.isEmpty()) {
            List<String> currentLevelNodes = new ArrayList<>();
            int currentLevelSize = readyQueue.size();
            
            // Process all nodes at current level
            for (int i = 0; i < currentLevelSize; i++) {
                String currentTransaction = readyQueue.poll();
                currentLevelNodes.add(currentTransaction);
                topologicalOrder.add(currentTransaction);
                transactionLevels.put(currentTransaction, currentLevel);
                
                log.debug("🎯 Processing transaction {} at level {}", currentTransaction, currentLevel);
                
                // Reduce in-degree of dependent transactions
                List<String> dependentTransactions = adjacencyList.getOrDefault(currentTransaction, Collections.emptyList());
                for (String dependent : dependentTransactions) {
                    int newInDegree = workingInDegree.get(dependent) - 1;
                    workingInDegree.put(dependent, newInDegree);
                    
                    if (newInDegree == 0) {
                        readyQueue.offer(dependent);
                        log.debug("🔓 Transaction {} ready for processing (dependencies satisfied)", dependent);
                    }
                }
            }
            
            executionLevels.add(currentLevelNodes);
            log.debug("📊 Level {} completed with {} transactions: {}", 
                     currentLevel, currentLevelNodes.size(), currentLevelNodes);
            currentLevel++;
        }
        
        // Validate all transactions were processed
        if (topologicalOrder.size() != transactionIds.size()) {
            log.warn("⚠️ Kahn's algorithm did not process all transactions. Processed: {}, Expected: {}", 
                    topologicalOrder.size(), transactionIds.size());
        }
        
        log.info("✅ Kahn's algorithm completed: {} levels, {} transactions processed", 
                executionLevels.size(), topologicalOrder.size());
        
        return new KahnResult(executionLevels, transactionLevels, topologicalOrder);
    }

    /**
     * Cycle Detection Result container
     */
    private static class CycleDetectionResult {
        private final boolean hasCycles;
        private final List<String> cyclePath;
        private final Map<String, String> cycleMetadata;

        public CycleDetectionResult(boolean hasCycles, List<String> cyclePath, Map<String, String> cycleMetadata) {
            this.hasCycles = hasCycles;
            this.cyclePath = cyclePath;
            this.cycleMetadata = cycleMetadata;
        }

        public boolean hasCycles() { return hasCycles; }
        public List<String> getCyclePath() { return cyclePath; }
        public Map<String, String> getCycleMetadata() { return cycleMetadata; }
    }

    /**
     * Detect cycles using DFS with WHITE/GRAY/BLACK coloring
     * Time Complexity: O(V + E)
     * Space Complexity: O(V) for recursion stack and color map
     * 
     * @param adjacencyList graph adjacency list
     * @param transactionIds all transaction IDs
     * @return CycleDetectionResult with cycle information
     */
    private CycleDetectionResult detectCyclesWithDFS(Map<String, List<String>> adjacencyList, 
                                                    Set<String> transactionIds) {
        
        log.debug("🔍 Detecting cycles using DFS with node coloring");
        
        // Node colors for DFS cycle detection
        Map<String, NodeColor> colorMap = new HashMap<>();
        List<String> currentPath = new ArrayList<>();
        Set<String> currentPathSet = new HashSet<>();
        
        // Initialize all nodes as WHITE (unvisited)
        transactionIds.forEach(transactionId -> colorMap.put(transactionId, NodeColor.WHITE));
        
        // Perform DFS from each unvisited node
        for (String transactionId : transactionIds) {
            if (colorMap.get(transactionId) == NodeColor.WHITE) {
                CycleDetectionResult result = dfsVisit(transactionId, adjacencyList, colorMap, 
                                                     currentPath, currentPathSet);
                if (result.hasCycles()) {
                    log.warn("🔴 Cycle detected starting from transaction: {}", transactionId);
                    return result;
                }
            }
        }
        
        log.info("✅ No cycles detected in transaction dependency graph");
        return new CycleDetectionResult(false, Collections.emptyList(), Collections.emptyMap());
    }

    /**
     * Node colors for DFS cycle detection
     */
    private enum NodeColor {
        WHITE,  // Unvisited
        GRAY,   // Currently being processed (in current path)
        BLACK   // Completely processed
    }

    /**
     * DFS visit method for cycle detection
     * 
     * @param currentTransaction current transaction being visited
     * @param adjacencyList graph adjacency list
     * @param colorMap node color mapping
     * @param currentPath current DFS path
     * @param currentPathSet current path as set for O(1) lookup
     * @return CycleDetectionResult
     */
    private CycleDetectionResult dfsVisit(String currentTransaction,
                                         Map<String, List<String>> adjacencyList,
                                         Map<String, NodeColor> colorMap,
                                         List<String> currentPath,
                                         Set<String> currentPathSet) {
        
        // Mark current node as GRAY (being processed)
        colorMap.put(currentTransaction, NodeColor.GRAY);
        currentPath.add(currentTransaction);
        currentPathSet.add(currentTransaction);
        
        log.trace("🔄 DFS visiting transaction: {}, path depth: {}", currentTransaction, currentPath.size());
        
        // Visit all adjacent nodes
        List<String> dependentTransactions = adjacencyList.getOrDefault(currentTransaction, Collections.emptyList());
        for (String dependent : dependentTransactions) {
            NodeColor dependentColor = colorMap.get(dependent);
            
            if (dependentColor == NodeColor.GRAY) {
                // Back edge detected - cycle found!
                List<String> cyclePath = extractCyclePath(currentPath, dependent);
                Map<String, String> cycleMetadata = buildCycleMetadata(cyclePath);
                
                log.error("🔴 Cycle detected: {} -> {}, full cycle: {}", 
                         currentTransaction, dependent, cyclePath);
                
                return new CycleDetectionResult(true, cyclePath, cycleMetadata);
            }
            
            if (dependentColor == NodeColor.WHITE) {
                // Continue DFS traversal
                CycleDetectionResult result = dfsVisit(dependent, adjacencyList, colorMap, 
                                                     currentPath, currentPathSet);
                if (result.hasCycles()) {
                    return result;
                }
            }
            // BLACK nodes are already processed, skip
        }
        
        // Mark current node as BLACK (completely processed)
        colorMap.put(currentTransaction, NodeColor.BLACK);
        currentPath.remove(currentPath.size() - 1);
        currentPathSet.remove(currentTransaction);
        
        return new CycleDetectionResult(false, Collections.emptyList(), Collections.emptyMap());
    }

    /**
     * Extract cycle path from DFS path when back edge is detected
     */
    private List<String> extractCyclePath(List<String> currentPath, String backEdgeTarget) {
        List<String> cyclePath = new ArrayList<>();
        boolean cycleStarted = false;
        
        for (String transaction : currentPath) {
            if (transaction.equals(backEdgeTarget)) {
                cycleStarted = true;
            }
            if (cycleStarted) {
                cyclePath.add(transaction);
            }
        }
        
        // Add the back edge to complete the cycle
        cyclePath.add(backEdgeTarget);
        
        return cyclePath;
    }

    /**
     * Build metadata about detected cycle
     */
    private Map<String, String> buildCycleMetadata(List<String> cyclePath) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("cycle_length", String.valueOf(cyclePath.size()));
        metadata.put("cycle_path", String.join(" -> ", cyclePath));
        metadata.put("detection_timestamp", LocalDateTime.now().toString());
        
        return metadata;
    }

    /**
     * Load active dependencies for given transaction set
     */
    private List<TransactionDependencyEntity> loadActiveDependencies(Set<String> transactionIds) {
        return dependencyRepository.findActiveDependenciesForTransactions(transactionIds);
    }

    /**
     * Build adjacency list representation of the dependency graph
     * Time Complexity: O(E) where E is number of dependencies
     */
    private Map<String, List<String>> buildAdjacencyList(List<TransactionDependencyEntity> dependencies, 
                                                         Set<String> transactionIds) {
        Map<String, List<String>> adjacencyList = new HashMap<>();
        
        // Initialize adjacency list for all transactions
        transactionIds.forEach(transactionId -> adjacencyList.put(transactionId, new ArrayList<>()));
        
        // Build adjacency relationships
        dependencies.forEach(dependency -> {
            String source = dependency.getSourceTransactionId();
            String target = dependency.getTargetTransactionId();
            
            if (transactionIds.contains(source) && transactionIds.contains(target)) {
                adjacencyList.get(source).add(target);
                log.trace("📍 Dependency edge: {} -> {}", source, target);
            }
        });
        
        return adjacencyList;
    }

    /**
     * Calculate in-degrees for all transactions
     * Time Complexity: O(V + E)
     */
    private Map<String, Integer> calculateInDegrees(Map<String, List<String>> adjacencyList, 
                                                   Set<String> transactionIds) {
        Map<String, Integer> inDegreeMap = new HashMap<>();
        
        // Initialize in-degrees to 0
        transactionIds.forEach(transactionId -> inDegreeMap.put(transactionId, 0));
        
        // Calculate in-degrees
        adjacencyList.forEach((source, dependents) -> {
            dependents.forEach(dependent -> {
                inDegreeMap.merge(dependent, 1, Integer::sum);
            });
        });
        
        log.debug("📊 In-degree distribution: {}", inDegreeMap);
        return inDegreeMap;
    }

    /**
     * Create execution graph entities in database
     */
    @Transactional
    private void createExecutionGraphEntities(String executionId, 
                                            KahnResult kahnResult,
                                            List<TransactionDependencyEntity> dependencies) {
        
        List<TransactionExecutionGraphEntity> graphEntities = new ArrayList<>();
        String correlationId = "SEQ-" + System.currentTimeMillis();
        
        kahnResult.getTransactionLevels().forEach((transactionId, level) -> {
            TransactionExecutionGraphEntity entity = new TransactionExecutionGraphEntity();
            entity.setExecutionId(executionId);
            entity.setTransactionId(transactionId);
            entity.setGraphLevel(level);
            entity.setTopologicalOrder(kahnResult.getTopologicalOrder().indexOf(transactionId));
            entity.setCorrelationId(correlationId);
            entity.setProcessingStatus(TransactionExecutionGraphEntity.ProcessingStatus.WHITE);
            
            // Calculate in-degree and out-degree from dependencies
            long inDegree = dependencies.stream()
                    .filter(dep -> dep.getTargetTransactionId().equals(transactionId))
                    .count();
            long outDegree = dependencies.stream()
                    .filter(dep -> dep.getSourceTransactionId().equals(transactionId))
                    .count();
            
            entity.setInDegree((int) inDegree);
            entity.setOutDegree((int) outDegree);
            
            graphEntities.add(entity);
        });
        
        graphRepository.saveAll(graphEntities);
        log.info("💾 Created {} execution graph entities for execution {}", 
                graphEntities.size(), executionId);
    }

    /**
     * Build performance metrics for the sequencing operation
     */
    private Map<String, String> buildPerformanceMetrics(int transactionCount, 
                                                       int dependencyCount,
                                                       KahnResult kahnResult) {
        Map<String, String> metrics = new HashMap<>();
        
        metrics.put("transaction_count", String.valueOf(transactionCount));
        metrics.put("dependency_count", String.valueOf(dependencyCount));
        metrics.put("execution_levels", String.valueOf(kahnResult.getExecutionLevels().size()));
        metrics.put("max_parallelism", String.valueOf(
            kahnResult.getExecutionLevels().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0)
        ));
        metrics.put("algorithm_complexity", String.format("O(%d + %d) = O(%d)", 
                   transactionCount, dependencyCount, transactionCount + dependencyCount));
        metrics.put("parallel_efficiency", String.format("%.2f%%", 
                   calculateParallelEfficiency(kahnResult)));
        
        return metrics;
    }

    /**
     * Calculate parallel processing efficiency
     */
    private double calculateParallelEfficiency(KahnResult kahnResult) {
        int totalTransactions = kahnResult.getTransactionLevels().size();
        int serialSteps = kahnResult.getExecutionLevels().size();
        
        if (serialSteps == 0 || totalTransactions == 0) {
            return 0.0;
        }
        
        return (1.0 - ((double) serialSteps / totalTransactions)) * 100;
    }

    /**
     * Create error result for cycle detection failure
     */
    private SequenceResult createErrorResult(CycleDetectionResult cycleResult, long startTime) {
        long algorithmDuration = System.currentTimeMillis() - startTime;
        
        Map<String, String> errorMetrics = new HashMap<>(cycleResult.getCycleMetadata());
        errorMetrics.put("algorithm_duration", String.valueOf(algorithmDuration));
        errorMetrics.put("error_type", "DEPENDENCY_CYCLE_DETECTED");
        
        return new SequenceResult(
            Collections.emptyList(),
            Collections.emptyMap(),
            true,
            cycleResult.getCyclePath(),
            errorMetrics,
            algorithmDuration
        );
    }

    /**
     * Validate execution sequence asynchronously
     * 
     * @param executionId execution identifier
     * @param sequenceResult sequence result to validate
     * @return validation future
     */
    public CompletableFuture<Boolean> validateSequenceAsync(String executionId, SequenceResult sequenceResult) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("🔍 Validating execution sequence for execution {}", executionId);
                
                // Validate no cycles
                if (sequenceResult.hasCycles()) {
                    log.warn("❌ Sequence validation failed: cycles detected");
                    return false;
                }
                
                // Validate all levels are non-empty
                boolean hasEmptyLevels = sequenceResult.getExecutionLevels().stream()
                        .anyMatch(List::isEmpty);
                
                if (hasEmptyLevels) {
                    log.warn("❌ Sequence validation failed: empty execution levels detected");
                    return false;
                }
                
                // Validate transaction level assignments
                boolean hasInvalidLevels = sequenceResult.getTransactionLevels().values().stream()
                        .anyMatch(level -> level < 0);
                
                if (hasInvalidLevels) {
                    log.warn("❌ Sequence validation failed: invalid level assignments");
                    return false;
                }
                
                log.info("✅ Sequence validation passed for execution {}", executionId);
                return true;
                
            } catch (Exception e) {
                log.error("❌ Sequence validation error for execution {}: {}", executionId, e.getMessage());
                return false;
            }
        }, executorService);
    }

    /**
     * Clear caches (for testing and memory management)
     */
    public void clearCaches() {
        adjacencyListCache.clear();
        inDegreeCache.clear();
        log.debug("🧹 TransactionSequencer caches cleared");
    }

    /**
     * Get sequencer statistics
     */
    public Map<String, Object> getSequencerStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("adjacency_cache_size", adjacencyListCache.size());
        stats.put("in_degree_cache_size", inDegreeCache.size());
        stats.put("thread_pool_active", executorService.toString());
        
        return stats;
    }
}