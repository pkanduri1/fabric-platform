package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Epic 3: Transaction Dependency Entity
 * 
 * JPA entity representing transaction dependencies for complex transaction processing.
 * Supports graph-based dependency management with various dependency types including
 * sequential, conditional, parallel-safe, resource locks, and data consistency requirements.
 * 
 * Features:
 * - Graph algorithm support for topological sorting
 * - Cycle detection capabilities
 * - Priority-based dependency resolution
 * - Banking-grade audit trails
 * - Compliance and security controls
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3
 */
@Entity
@Table(name = "TRANSACTION_DEPENDENCIES", schema = "CM3INT", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_trans_dep_pair", 
                           columnNames = {"source_transaction_id", "target_transaction_id"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TransactionDependencyEntity {

    /**
     * Dependency types supported by the system
     */
    public enum DependencyType {
        SEQUENTIAL,         // Must complete in order
        CONDITIONAL,        // Based on condition evaluation
        PARALLEL_SAFE,      // Can run in parallel safely
        RESOURCE_LOCK,      // Requires exclusive resource access
        DATA_CONSISTENCY    // Ensures data integrity across transactions
    }

    @Id
    @Column(name = "dependency_id", nullable = false)
    @SequenceGenerator(name = "seq_transaction_dependencies", 
                      sequenceName = "CM3INT.seq_transaction_dependencies", 
                      allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
                   generator = "seq_transaction_dependencies")
    private Long dependencyId;

    @Column(name = "source_transaction_id", nullable = false, length = 100)
    private String sourceTransactionId;

    @Column(name = "target_transaction_id", nullable = false, length = 100)
    private String targetTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", length = 50)
    private DependencyType dependencyType = DependencyType.SEQUENTIAL;

    @Lob
    @Column(name = "dependency_condition")
    private String dependencyCondition;

    @Column(name = "priority_weight")
    private Integer priorityWeight = 1;

    @Column(name = "max_wait_time_seconds")
    private Integer maxWaitTimeSeconds = 3600;

    @Column(name = "retry_policy", length = 100)
    private String retryPolicy = "EXPONENTIAL_BACKOFF";

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @Column(name = "active_flag", length = 1)
    private String activeFlag = "Y";

    @Column(name = "compliance_level", length = 20)
    private String complianceLevel = "STANDARD";

    @Column(name = "business_justification", length = 1000)
    private String businessJustification;

    /**
     * Pre-persist callback to set created timestamp
     */
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (priorityWeight == null) {
            priorityWeight = 1;
        }
        if (maxWaitTimeSeconds == null) {
            maxWaitTimeSeconds = 3600;
        }
        if (activeFlag == null) {
            activeFlag = "Y";
        }
        if (complianceLevel == null) {
            complianceLevel = "STANDARD";
        }
        if (retryPolicy == null) {
            retryPolicy = "EXPONENTIAL_BACKOFF";
        }
    }

    /**
     * Pre-update callback to set last modified timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }

    /**
     * Business logic helper methods
     */
    
    /**
     * Check if this dependency is active
     * @return true if active
     */
    public boolean isActive() {
        return "Y".equals(activeFlag);
    }

    /**
     * Check if this is a blocking dependency
     * @return true if blocking
     */
    public boolean isBlocking() {
        return dependencyType == DependencyType.SEQUENTIAL || 
               dependencyType == DependencyType.RESOURCE_LOCK ||
               dependencyType == DependencyType.DATA_CONSISTENCY;
    }

    /**
     * Check if this dependency can be processed in parallel
     * @return true if parallel processing is safe
     */
    public boolean isParallelSafe() {
        return dependencyType == DependencyType.PARALLEL_SAFE;
    }

    /**
     * Check if this dependency requires condition evaluation
     * @return true if conditional
     */
    public boolean isConditional() {
        return dependencyType == DependencyType.CONDITIONAL && 
               dependencyCondition != null && !dependencyCondition.trim().isEmpty();
    }

    /**
     * Get priority ranking (higher number = higher priority)
     * @return priority value between 1-100
     */
    public int getPriorityRanking() {
        return priorityWeight != null ? priorityWeight : 1;
    }

    /**
     * Check if dependency requires high compliance controls
     * @return true if high compliance required
     */
    public boolean isHighCompliance() {
        return "HIGH".equals(complianceLevel) || "CRITICAL".equals(complianceLevel);
    }

    /**
     * Get maximum wait time in milliseconds
     * @return wait time in milliseconds
     */
    public long getMaxWaitTimeMillis() {
        return (long) maxWaitTimeSeconds * 1000;
    }

    /**
     * Create dependency edge representation for graph algorithms
     * @return edge string representation
     */
    public String getEdgeRepresentation() {
        return sourceTransactionId + " -> " + targetTransactionId + 
               " [" + dependencyType + ", priority=" + priorityWeight + "]";
    }

    /**
     * Validate dependency configuration
     * @return validation result
     */
    public boolean isValidConfiguration() {
        // Basic validation rules
        if (sourceTransactionId == null || sourceTransactionId.trim().isEmpty()) {
            return false;
        }
        if (targetTransactionId == null || targetTransactionId.trim().isEmpty()) {
            return false;
        }
        if (sourceTransactionId.equals(targetTransactionId)) {
            return false; // Self-dependency not allowed
        }
        if (priorityWeight < 1 || priorityWeight > 100) {
            return false;
        }
        if (maxWaitTimeSeconds < 1 || maxWaitTimeSeconds > 86400) { // Max 24 hours
            return false;
        }
        
        // Conditional dependencies must have conditions
        if (dependencyType == DependencyType.CONDITIONAL) {
            return dependencyCondition != null && !dependencyCondition.trim().isEmpty();
        }
        
        return true;
    }

    @Override
    public String toString() {
        return "TransactionDependencyEntity{" +
                "dependencyId=" + dependencyId +
                ", sourceTransactionId='" + sourceTransactionId + '\'' +
                ", targetTransactionId='" + targetTransactionId + '\'' +
                ", dependencyType=" + dependencyType +
                ", priorityWeight=" + priorityWeight +
                ", activeFlag='" + activeFlag + '\'' +
                '}';
    }
}