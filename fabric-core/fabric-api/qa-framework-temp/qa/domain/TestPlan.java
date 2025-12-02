package com.fabric.batch.qa.domain;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive Test Plan Domain Model for Banking QA
 * 
 * Represents the complete test plan generated for a banking project,
 * including test strategy, risk assessment, and quality gates.
 */
@Data
@Builder
@Jacksonized
public class TestPlan {
    
    // Plan Identification
    private String projectId;
    private String phase;
    private String planVersion;
    private LocalDateTime createdDate;
    private String createdBy;
    
    // Test Strategy
    private BankingTestStrategy testStrategy;
    private RiskLevel riskLevel;
    private TestApproach testApproach;
    private List<TestObjective> testObjectives;
    
    // Test Suites and Cases
    private List<TestSuite> testSuites;
    private int totalTestCases;
    private int criticalTestCases;
    private int estimatedDurationMinutes;
    
    // Acceptance Criteria
    private List<AcceptanceCriteria> acceptanceCriteria;
    private List<QualityGate> qualityGates;
    
    // Risk Management
    private RiskAssessment riskAssessment;
    private List<MitigationStrategy> mitigationStrategies;
    
    // Resource Planning
    private ResourceRequirements resourceRequirements;
    private List<TestEnvironment> requiredEnvironments;
    private List<String> prerequisites;
    
    // Banking-Specific Requirements
    private ComplianceRequirements complianceRequirements;
    private SecurityRequirements securityRequirements;
    private AuditRequirements auditRequirements;
    
    // Success Criteria
    private double minimumPassRate;
    private List<String> exitCriteria;
    private List<String> suspensionCriteria;
    
    /**
     * Get critical test suites that must pass
     */
    public List<TestSuite> getCriticalTestSuites() {
        return testSuites.stream()
            .filter(suite -> suite.getPriority() == TestPriority.CRITICAL)
            .toList();
    }
    
    /**
     * Get estimated total testing effort in hours
     */
    public double getEstimatedEffortHours() {
        return estimatedDurationMinutes / 60.0;
    }
    
    /**
     * Check if plan meets banking standards
     */
    public boolean meetsBankingStandards() {
        return hasSOXComplianceTests() && 
               hasSecurityTests() && 
               hasAuditTrailTests() && 
               hasPerformanceTests();
    }
    
    /**
     * Check if plan includes SOX compliance tests
     */
    public boolean hasSOXComplianceTests() {
        return testSuites.stream()
            .anyMatch(suite -> suite.getTestType() == TestType.SOX_COMPLIANCE);
    }
    
    /**
     * Check if plan includes security tests
     */
    public boolean hasSecurityTests() {
        return testSuites.stream()
            .anyMatch(suite -> suite.getTestType() == TestType.SECURITY);
    }
    
    /**
     * Check if plan includes audit trail tests
     */
    public boolean hasAuditTrailTests() {
        return testSuites.stream()
            .anyMatch(suite -> suite.getTestType() == TestType.AUDIT_TRAIL);
    }
    
    /**
     * Check if plan includes performance tests
     */
    public boolean hasPerformanceTests() {
        return testSuites.stream()
            .anyMatch(suite -> suite.getTestType() == TestType.PERFORMANCE);
    }
    
    /**
     * Get blocking quality gates
     */
    public List<QualityGate> getBlockingQualityGates() {
        return qualityGates.stream()
            .filter(QualityGate::isBlocking)
            .toList();
    }
    
    /**
     * Validate test plan completeness
     */
    public ValidationResult validateCompleteness() {
        ValidationResult.ValidationResultBuilder result = ValidationResult.builder();
        
        if (testSuites == null || testSuites.isEmpty()) {
            result.addError("Test plan must contain at least one test suite");
        }
        
        if (!hasSOXComplianceTests()) {
            result.addError("SOX compliance tests are mandatory for banking applications");
        }
        
        if (!hasSecurityTests()) {
            result.addError("Security tests are mandatory for banking applications");
        }
        
        if (riskAssessment == null) {
            result.addWarning("Risk assessment is missing");
        }
        
        if (qualityGates == null || qualityGates.isEmpty()) {
            result.addWarning("No quality gates defined");
        }
        
        return result.build();
    }
}

/**
 * Test Suite Domain Model
 */
@Data
@Builder
@Jacksonized
public class TestSuite {
    
    private String name;
    private String description;
    private TestType testType;
    private TestPriority priority;
    private List<TestCase> testCases;
    private int estimatedDuration; // minutes
    private List<String> dependencies;
    private Map<String, Object> configuration;
    private List<String> tags;
    
    // Banking-specific attributes
    private boolean complianceRequired;
    private List<String> regulatoryRequirements;
    private SecurityLevel securityLevel;
    
    /**
     * Get critical test cases
     */
    public List<TestCase> getCriticalTestCases() {
        return testCases.stream()
            .filter(TestCase::isCritical)
            .toList();
    }
    
    /**
     * Check if suite is ready for execution
     */
    public boolean isReadyForExecution() {
        return testCases != null && !testCases.isEmpty() &&
               testCases.stream().allMatch(TestCase::isExecutable);
    }
}

/**
 * Test Case Domain Model
 */
@Data
@Builder
@Jacksonized
public class TestCase {
    
    private String id;
    private String name;
    private String description;
    private TestType testType;
    private TestPriority priority;
    private boolean critical;
    private List<String> preconditions;
    private List<TestStep> testSteps;
    private List<String> expectedResults;
    private Map<String, Object> testData;
    private List<String> tags;
    
    // Banking-specific attributes
    private boolean soxRelevant;
    private boolean pciRelevant;
    private List<String> auditRequirements;
    private SecurityClassification securityClassification;
    
    /**
     * Factory method for critical test cases
     */
    public static TestCase critical(String id, String name, String description) {
        return TestCase.builder()
            .id(id)
            .name(name)
            .description(description)
            .priority(TestPriority.CRITICAL)
            .critical(true)
            .build();
    }
    
    /**
     * Factory method for standard test cases
     */
    public static TestCase standard(String id, String name, String description) {
        return TestCase.builder()
            .id(id)
            .name(name)
            .description(description)
            .priority(TestPriority.HIGH)
            .critical(false)
            .build();
    }
    
    /**
     * Check if test case is executable
     */
    public boolean isExecutable() {
        return name != null && !name.trim().isEmpty() &&
               testSteps != null && !testSteps.isEmpty();
    }
    
    /**
     * Check if test case requires manual execution
     */
    public boolean isManualTest() {
        return testSteps != null && testSteps.stream()
            .anyMatch(step -> step.getExecutionType() == ExecutionType.MANUAL);
    }
    
    /**
     * Get estimated execution time in minutes
     */
    public int getEstimatedExecutionTime() {
        if (testSteps == null) return 5; // default
        
        return testSteps.stream()
            .mapToInt(TestStep::getEstimatedDuration)
            .sum();
    }
}

/**
 * Test Step Domain Model
 */
@Data
@Builder
@Jacksonized
public class TestStep {
    
    private int stepNumber;
    private String action;
    private String expectedResult;
    private ExecutionType executionType;
    private int estimatedDuration; // minutes
    private Map<String, Object> parameters;
    private List<String> validationPoints;
    
    /**
     * Check if step is automated
     */
    public boolean isAutomated() {
        return executionType == ExecutionType.AUTOMATED;
    }
}

/**
 * Quality Gate Domain Model
 */
@Data
@Builder
@Jacksonized
public class QualityGate {
    
    private String name;
    private String description;
    private List<String> criteria;
    private boolean blocking;
    private double threshold;
    private QualityMetric metric;
    private List<String> stakeholders;
    
    /**
     * Check if quality gate is met
     */
    public boolean isMet(double actualValue) {
        return actualValue >= threshold;
    }
}

/**
 * Risk Assessment Domain Model
 */
@Data
@Builder
@Jacksonized
public class RiskAssessment {
    
    private RiskLevel riskLevel;
    private List<IdentifiedRisk> identifiedRisks;
    private List<MitigationStrategy> mitigationStrategies;
    private RiskMatrix riskMatrix;
    private double overallRiskScore;
    
    /**
     * Get high-risk items
     */
    public List<IdentifiedRisk> getHighRisks() {
        return identifiedRisks.stream()
            .filter(risk -> risk.getRiskLevel() == RiskLevel.HIGH || 
                           risk.getRiskLevel() == RiskLevel.CRITICAL)
            .toList();
    }
}

/**
 * Identified Risk Domain Model
 */
@Data
@Builder
@Jacksonized
public class IdentifiedRisk {
    
    private String riskId;
    private String description;
    private RiskLevel riskLevel;
    private RiskCategory category;
    private double probability;
    private double impact;
    private String owner;
    private List<String> mitigationActions;
    
    /**
     * Calculate risk score
     */
    public double getRiskScore() {
        return probability * impact;
    }
}

/**
 * Supporting Enums and Classes
 */
enum BankingTestStrategy {
    ENTERPRISE_BANKING,
    FINANCIAL_TRANSACTIONS,
    CUSTOMER_DATA_PROCESSING,
    AUDIT_COMPLIANCE,
    REGULATORY_COMPLIANCE
}

enum TestType {
    DATABASE_SCHEMA,
    JPA_ENTITY,
    REPOSITORY_LAYER,
    SERVICE_LAYER,
    INTEGRATION,
    SOX_COMPLIANCE,
    PCI_DSS,
    AUDIT_TRAIL,
    SECURITY,
    PERFORMANCE,
    LOAD_TESTING,
    DISASTER_RECOVERY,
    USER_ACCEPTANCE
}

enum TestPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum TestApproach {
    RISK_BASED,
    COMPLIANCE_DRIVEN,
    PERFORMANCE_FOCUSED,
    SECURITY_FIRST
}

enum ExecutionType {
    MANUAL,
    AUTOMATED,
    SEMI_AUTOMATED
}

enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum RiskCategory {
    TECHNICAL,
    FUNCTIONAL,
    PERFORMANCE,
    SECURITY,
    COMPLIANCE,
    OPERATIONAL
}

enum SecurityLevel {
    PUBLIC,
    INTERNAL,
    CONFIDENTIAL,
    RESTRICTED
}

enum SecurityClassification {
    UNCLASSIFIED,
    INTERNAL_USE,
    CONFIDENTIAL,
    HIGHLY_CONFIDENTIAL
}

enum QualityMetric {
    PASS_RATE,
    CODE_COVERAGE,
    PERFORMANCE_SCORE,
    SECURITY_SCORE,
    COMPLIANCE_SCORE
}