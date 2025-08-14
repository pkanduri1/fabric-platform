package com.truist.batch.qa.domain;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * QA Handover Package Domain Model
 * 
 * Represents the comprehensive handover package provided by development team
 * for QA validation. Contains all necessary information for test planning
 * and execution.
 */
@Data
@Builder
@Jacksonized
public class QAHandoverPackage {
    
    // Project Identification
    private String projectId;
    private String projectName;
    private String phase;
    private String version;
    private LocalDateTime handoverDate;
    private String developmentTeam;
    private String productOwner;
    
    // Deliverables and Components
    private Set<String> deliverables;
    private List<Requirement> requirements;
    private List<AcceptanceCriteria> acceptanceCriteria;
    private Map<String, Object> testData;
    private List<String> deploymentInstructions;
    
    // Technical Details
    private TechnicalArchitecture architecture;
    private List<String> dependencies;
    private Map<String, String> configurationProperties;
    private List<String> databaseChanges;
    private List<String> apiEndpoints;
    
    // Risk and Compliance
    private RiskAssessment riskAssessment;
    private List<ComplianceRequirement> complianceRequirements;
    private List<SecurityRequirement> securityRequirements;
    private AuditTrailRequirements auditRequirements;
    
    // Quality Information
    private CodeQualityMetrics codeQuality;
    private TestCoverageReport testCoverage;
    private PerformanceBenchmarks performanceBenchmarks;
    private List<KnownIssue> knownIssues;
    
    // Banking-Specific Information
    private BankingContext bankingContext;
    private RegulatoryCompliance regulatoryCompliance;
    private DataClassification dataClassification;
    
    // Environment Information
    private List<EnvironmentConfiguration> environments;
    private DeploymentStrategy deploymentStrategy;
    private MonitoringConfiguration monitoring;
    
    /**
     * Check if package contains specific deliverable
     */
    public boolean hasDeliverable(String deliverable) {
        return deliverables != null && deliverables.contains(deliverable);
    }
    
    /**
     * Check if project involves financial data processing
     */
    public boolean involvesFinancialData() {
        return bankingContext != null && bankingContext.isFinancialDataProcessing();
    }
    
    /**
     * Check if project involves customer data
     */
    public boolean involvesCustomerData() {
        return dataClassification != null && 
               (dataClassification.hasPersonallyIdentifiableInfo() || 
                dataClassification.hasCustomerFinancialInfo());
    }
    
    /**
     * Check if project involves audit trail functionality
     */
    public boolean involvesAuditTrail() {
        return auditRequirements != null && auditRequirements.isAuditTrailRequired();
    }
    
    /**
     * Check if project uses new technology or frameworks
     */
    public boolean involvesNewTechnology() {
        return architecture != null && architecture.hasNewTechnologyStack();
    }
    
    /**
     * Check if project has complex integrations
     */
    public boolean hasComplexIntegrations() {
        return architecture != null && architecture.getIntegrationComplexity() == ComplexityLevel.HIGH;
    }
    
    /**
     * Check if this is for production deployment
     */
    public boolean isProductionDeployment() {
        return environments != null && 
               environments.stream().anyMatch(env -> "PRODUCTION".equals(env.getEnvironmentType()));
    }
    
    /**
     * Check if project has regulatory requirements
     */
    public boolean hasRegulatoryRequirements() {
        return regulatoryCompliance != null && regulatoryCompliance.hasRegulatoryRequirements();
    }
    
    /**
     * Get test data for specific test suite
     */
    public Object getTestDataForSuite(String suiteName) {
        return testData != null ? testData.get(suiteName) : null;
    }
    
    /**
     * Get configuration property value
     */
    public String getConfigurationProperty(String key) {
        return configurationProperties != null ? configurationProperties.get(key) : null;
    }
    
    /**
     * Check if handover package is complete and ready for testing
     */
    public boolean isReadyForTesting() {
        return projectId != null && 
               !requirements.isEmpty() && 
               !deliverables.isEmpty() && 
               architecture != null;
    }
    
    /**
     * Get requirements by priority
     */
    public List<Requirement> getRequirementsByPriority(RequirementPriority priority) {
        return requirements.stream()
            .filter(req -> req.getPriority() == priority)
            .toList();
    }
    
    /**
     * Get critical requirements that must be validated
     */
    public List<Requirement> getCriticalRequirements() {
        return getRequirementsByPriority(RequirementPriority.CRITICAL);
    }
    
    /**
     * Check if package has performance requirements
     */
    public boolean hasPerformanceRequirements() {
        return performanceBenchmarks != null && !performanceBenchmarks.isEmpty();
    }
    
    /**
     * Check if package has security requirements
     */
    public boolean hasSecurityRequirements() {
        return securityRequirements != null && !securityRequirements.isEmpty();
    }
    
    /**
     * Get estimated testing effort in hours
     */
    public int getEstimatedTestingEffort() {
        int baseEffort = deliverables.size() * 8; // 8 hours per deliverable
        
        // Add complexity factors
        if (involvesFinancialData()) baseEffort += 16;
        if (involvesCustomerData()) baseEffort += 12;
        if (hasComplexIntegrations()) baseEffort += 20;
        if (hasRegulatoryRequirements()) baseEffort += 24;
        
        return baseEffort;
    }
    
    /**
     * Get recommended test team size
     */
    public int getRecommendedTestTeamSize() {
        int effort = getEstimatedTestingEffort();
        
        if (effort > 200) return 5; // Large team for complex projects
        if (effort > 100) return 3; // Medium team
        if (effort > 40) return 2;  // Small team
        return 1; // Individual tester
    }
    
    /**
     * Validate handover package completeness
     */
    public ValidationResult validateCompleteness() {
        ValidationResult.ValidationResultBuilder result = ValidationResult.builder();
        
        if (projectId == null || projectId.trim().isEmpty()) {
            result.addError("Project ID is required");
        }
        
        if (requirements == null || requirements.isEmpty()) {
            result.addError("Requirements list cannot be empty");
        }
        
        if (deliverables == null || deliverables.isEmpty()) {
            result.addError("Deliverables list cannot be empty");
        }
        
        if (architecture == null) {
            result.addWarning("Technical architecture information is missing");
        }
        
        if (testData == null || testData.isEmpty()) {
            result.addWarning("Test data is not provided");
        }
        
        if (bankingContext == null) {
            result.addWarning("Banking context information is missing");
        }
        
        return result.build();
    }
}

/**
 * Supporting domain classes for QA Handover Package
 */
@Data
@Builder
class TechnicalArchitecture {
    private String frameworkVersion;
    private List<String> technologyStack;
    private ComplexityLevel integrationComplexity;
    private boolean newTechnologyStack;
    private List<String> externalSystems;
    private DatabaseConfiguration databaseConfig;
    
    public boolean hasNewTechnologyStack() {
        return newTechnologyStack;
    }
}

@Data
@Builder
class BankingContext {
    private boolean financialDataProcessing;
    private boolean customerDataAccess;
    private boolean regulatedActivity;
    private List<String> affectedSystems;
    private ComplianceLevel requiredComplianceLevel;
    
    public boolean isFinancialDataProcessing() {
        return financialDataProcessing;
    }
}

@Data
@Builder
class DataClassification {
    private boolean personallyIdentifiableInfo;
    private boolean customerFinancialInfo;
    private boolean internalBankingData;
    private DataSensitivityLevel sensitivityLevel;
    private List<String> dataCategories;
    
    public boolean hasPersonallyIdentifiableInfo() {
        return personallyIdentifiableInfo;
    }
    
    public boolean hasCustomerFinancialInfo() {
        return customerFinancialInfo;
    }
}

@Data
@Builder
class AuditTrailRequirements {
    private boolean auditTrailRequired;
    private List<String> auditedOperations;
    private AuditLevel auditLevel;
    private RetentionPolicy retentionPolicy;
    
    public boolean isAuditTrailRequired() {
        return auditTrailRequired;
    }
}

@Data
@Builder
class RegulatoryCompliance {
    private boolean soxCompliance;
    private boolean pciDssCompliance;
    private boolean gdprCompliance;
    private List<String> applicableRegulations;
    private ComplianceLevel complianceLevel;
    
    public boolean hasRegulatoryRequirements() {
        return soxCompliance || pciDssCompliance || gdprCompliance || 
               (applicableRegulations != null && !applicableRegulations.isEmpty());
    }
}

@Data
@Builder
class CodeQualityMetrics {
    private double codeCoverage;
    private int cyclomaticComplexity;
    private int technicalDebtHours;
    private List<String> qualityGateResults;
    private SonarQubeResults sonarResults;
}

@Data
@Builder
class TestCoverageReport {
    private double lineCoverage;
    private double branchCoverage;
    private double functionCoverage;
    private int totalTests;
    private int passingTests;
    private List<String> uncoveredAreas;
}

@Data
@Builder
class PerformanceBenchmarks {
    private Map<String, Integer> responseTimeTargets; // operation -> milliseconds
    private int maxConcurrentUsers;
    private long maxMemoryUsage;
    private double cpuUtilizationThreshold;
    
    public boolean isEmpty() {
        return responseTimeTargets == null || responseTimeTargets.isEmpty();
    }
}

@Data
@Builder
class EnvironmentConfiguration {
    private String environmentType; // DEV, TEST, STAGING, PRODUCTION
    private String databaseUrl;
    private Map<String, String> environmentProperties;
    private List<String> requiredServices;
    private InfrastructureSpecs infrastructureSpecs;
}

@Data
@Builder
class DeploymentStrategy {
    private DeploymentType deploymentType;
    private boolean blueGreenDeployment;
    private boolean canaryDeployment;
    private RollbackStrategy rollbackStrategy;
    private List<String> preDeploymentChecks;
    private List<String> postDeploymentValidation;
}

@Data
@Builder
class MonitoringConfiguration {
    private List<String> healthCheckEndpoints;
    private List<String> metricsEndpoints;
    private AlertConfiguration alertConfig;
    private LoggingConfiguration loggingConfig;
}

// Enums for type safety
enum ComplexityLevel { LOW, MEDIUM, HIGH, CRITICAL }
enum ComplianceLevel { BASIC, STANDARD, ENHANCED, CRITICAL }
enum DataSensitivityLevel { PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED }
enum AuditLevel { BASIC, DETAILED, COMPREHENSIVE }
enum DeploymentType { ROLLING, BLUE_GREEN, CANARY, RECREATE }
enum RequirementPriority { LOW, MEDIUM, HIGH, CRITICAL }