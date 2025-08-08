package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Epic 3: Transaction Execution Graph Entity
 * 
 * JPA entity representing the execution graph for transaction processing.
 * Supports graph algorithms including topological sorting, cycle detection,
 * and distributed processing coordination.
 * 
 * Features:
 * - DFS-based cycle detection with WHITE/GRAY/BLACK coloring
 * - Topological ordering support
 * - In-degree and out-degree tracking
 * - Performance monitoring and resource tracking
 * - Thread assignment and parallel processing coordination
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3
 */
@Entity
@Table(name = "TRANSACTION_EXECUTION_GRAPH", schema = "CM3INT",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_exec_trans_graph", 
                           columnNames = {"execution_id", "transaction_id"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TransactionExecutionGraphEntity {

    /**
     * Processing status for DFS cycle detection algorithm
     * WHITE: Unvisited node
     * GRAY: Currently being processed (in the current path)
     * BLACK: Completely processed
     * BLOCKED: Waiting for dependencies
     * ERROR: Processing failed
     */
    public enum ProcessingStatus {
        WHITE,   // Unvisited
        GRAY,    // In progress
        BLACK,   // Completed
        BLOCKED, // Waiting
        ERROR    // Failed
    }

    @Id
    @Column(name = "graph_id", nullable = false)
    @SequenceGenerator(name = "seq_transaction_execution_graph", 
                      sequenceName = "CM3INT.seq_transaction_execution_graph", 
                      allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
                   generator = "seq_transaction_execution_graph")
    private Long graphId;

    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "graph_level")
    private Integer graphLevel = 0;

    @Column(name = "topological_order")
    private Integer topologicalOrder;

    @Column(name = "in_degree")
    private Integer inDegree = 0;

    @Column(name = "out_degree")
    private Integer outDegree = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", length = 50)
    private ProcessingStatus processingStatus = ProcessingStatus.WHITE;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "thread_assignment", length = 100)
    private String threadAssignment;

    @Lob
    @Column(name = "resource_locks")
    private String resourceLocks;

    @Lob
    @Column(name = "dependency_chain")
    private String dependencyChain;

    @Lob
    @Column(name = "cycle_detection_data")
    private String cycleDetectionData;

    @Lob
    @Column(name = "performance_stats")
    private String performanceStats;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "business_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private java.util.Date businessDate = new java.util.Date();

    /**
     * Pre-persist callback
     */
    @PrePersist
    protected void onCreate() {
        if (graphLevel == null) {
            graphLevel = 0;
        }
        if (inDegree == null) {
            inDegree = 0;
        }
        if (outDegree == null) {
            outDegree = 0;
        }
        if (processingStatus == null) {
            processingStatus = ProcessingStatus.WHITE;
        }
        if (businessDate == null) {
            businessDate = new java.util.Date();
        }
    }

    /**
     * Business logic helper methods for graph algorithms
     */

    /**
     * Check if this node is ready for processing (all dependencies satisfied)
     * @return true if ready
     */
    public boolean isReadyForProcessing() {
        return inDegree == 0 && processingStatus == ProcessingStatus.WHITE;
    }

    /**
     * Check if this node is currently being processed
     * @return true if in progress
     */
    public boolean isInProgress() {
        return processingStatus == ProcessingStatus.GRAY;
    }

    /**
     * Check if this node has completed processing
     * @return true if completed
     */
    public boolean isCompleted() {
        return processingStatus == ProcessingStatus.BLACK;
    }

    /**
     * Check if this node is blocked by dependencies
     * @return true if blocked
     */
    public boolean isBlocked() {
        return processingStatus == ProcessingStatus.BLOCKED || inDegree > 0;
    }

    /**
     * Check if this node encountered an error
     * @return true if error state
     */
    public boolean hasError() {
        return processingStatus == ProcessingStatus.ERROR;
    }

    /**
     * Mark node as started (GRAY state for cycle detection)
     */
    public void markAsStarted() {
        this.processingStatus = ProcessingStatus.GRAY;
        this.startTime = LocalDateTime.now();
    }

    /**
     * Mark node as completed (BLACK state)
     */
    public void markAsCompleted() {
        this.processingStatus = ProcessingStatus.BLACK;
        this.endTime = LocalDateTime.now();
    }

    /**
     * Mark node as blocked
     */
    public void markAsBlocked() {
        this.processingStatus = ProcessingStatus.BLOCKED;
    }

    /**
     * Mark node as error
     */
    public void markAsError() {
        this.processingStatus = ProcessingStatus.ERROR;
        this.endTime = LocalDateTime.now();
    }

    /**
     * Reset node to unvisited state (WHITE)
     */
    public void resetToUnvisited() {
        this.processingStatus = ProcessingStatus.WHITE;
        this.startTime = null;
        this.endTime = null;
        this.threadAssignment = null;
    }

    /**
     * Decrement in-degree (when a dependency is satisfied)
     */
    public void decrementInDegree() {
        if (inDegree > 0) {
            inDegree--;
        }
    }

    /**
     * Increment in-degree (when a new dependency is added)
     */
    public void incrementInDegree() {
        inDegree++;
    }

    /**
     * Increment out-degree (when this node becomes a dependency for another)
     */
    public void incrementOutDegree() {
        outDegree++;
    }

    /**
     * Check if this node is a source node (no incoming dependencies)
     * @return true if source node
     */
    public boolean isSourceNode() {
        return inDegree == 0;
    }

    /**
     * Check if this node is a sink node (no outgoing dependencies)
     * @return true if sink node
     */
    public boolean isSinkNode() {
        return outDegree == 0;
    }

    /**
     * Get processing duration in milliseconds
     * @return duration or -1 if not completed
     */
    public long getProcessingDurationMillis() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return -1;
    }

    /**
     * Get current execution state for monitoring
     * @return state description
     */
    public String getExecutionState() {
        StringBuilder state = new StringBuilder();
        state.append("Status: ").append(processingStatus);
        state.append(", Level: ").append(graphLevel);
        state.append(", InDegree: ").append(inDegree);
        state.append(", OutDegree: ").append(outDegree);
        
        if (topologicalOrder != null) {
            state.append(", TopOrder: ").append(topologicalOrder);
        }
        
        if (threadAssignment != null) {
            state.append(", Thread: ").append(threadAssignment);
        }
        
        return state.toString();
    }

    /**
     * Check if node can be processed in parallel with others at same level
     * @return true if parallel processing is safe
     */
    public boolean canProcessInParallel() {
        return processingStatus == ProcessingStatus.WHITE && 
               inDegree == 0 && 
               resourceLocks == null;
    }

    /**
     * Validate graph node consistency
     * @return true if valid
     */
    public boolean isValidGraphNode() {
        if (executionId == null || executionId.trim().isEmpty()) {
            return false;
        }
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return false;
        }
        if (correlationId == null || correlationId.trim().isEmpty()) {
            return false;
        }
        if (graphLevel < 0) {
            return false;
        }
        if (inDegree < 0 || outDegree < 0) {
            return false;
        }
        
        // Validate state transitions
        if (processingStatus == ProcessingStatus.BLACK && endTime == null) {
            return false; // Completed nodes must have end time
        }
        if (processingStatus == ProcessingStatus.GRAY && startTime == null) {
            return false; // Started nodes must have start time
        }
        
        return true;
    }

    /**
     * Create graph node representation for algorithms
     * @return node string representation
     */
    public String getGraphNodeRepresentation() {
        return String.format("Node[%s] Level=%d, InDeg=%d, OutDeg=%d, Status=%s", 
                           transactionId, graphLevel, inDegree, outDegree, processingStatus);
    }

    @Override
    public String toString() {
        return "TransactionExecutionGraphEntity{" +
                "graphId=" + graphId +
                ", executionId='" + executionId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", graphLevel=" + graphLevel +
                ", topologicalOrder=" + topologicalOrder +
                ", processingStatus=" + processingStatus +
                ", inDegree=" + inDegree +
                ", outDegree=" + outDegree +
                '}';
    }
}