# Consumer Default ETL - Revised User Stories
## Aligned with Existing Fabric Oracle Template Architecture

Based on the principal enterprise architect's analysis, these revised user stories leverage the existing Fabric platform's Oracle-based template system (field_templates and file_type_templates tables) rather than implementing new microservices.

---

## **US001: Enhanced Template Management for Consumer Default Data**
**Epic:** Consumer Default ETL Integration  
**Priority:** High  
**Risk Level:** Medium

### **Story:**
As a **Lending Product Owner**, I want to **extend the existing Fabric template system to support consumer default data types** so that **we can process default-related files using our proven Oracle-based template infrastructure while maintaining data consistency and regulatory compliance**.

### **Acceptance Criteria:**

#### **AC1: Consumer Default Template Configuration**
- **GIVEN** the existing field_templates and file_type_templates tables in Oracle
- **WHEN** I configure a new consumer default file type (e.g., 'CD01' for consumer defaults)
- **THEN** the system shall:
  - Create entries in file_type_templates for consumer default file specifications
  - Support field mappings in field_templates for default-related fields (account_number, default_date, default_amount, loss_given_default, probability_of_default, recovery_amount, charge_off_date)
  - Maintain existing version control and audit capabilities
  - Enable/disable templates through the existing enabled flag

#### **AC2: Default-Specific Field Validations**
- **GIVEN** consumer default templates are configured
- **WHEN** processing default data files
- **THEN** the system shall:
  - Validate monetary amounts against regulatory thresholds
  - Ensure default dates are within acceptable reporting periods
  - Verify account numbers match existing lending portfolio
  - Apply risk rating validations (A, B, C, D, E rating system)
  - Log validation results in existing audit tables

#### **AC3: Backward Compatibility**
- **GIVEN** existing Fabric templates (e.g., p327 consumer lending)
- **WHEN** implementing consumer default templates
- **THEN** the system shall:
  - Maintain full compatibility with existing template processing
  - Preserve current API contracts and data flows
  - Support parallel processing of legacy and default templates
  - Ensure no disruption to current production workloads

### **Technical Requirements:**
- Extend existing FieldTemplateEntity and FileTypeTemplateEntity classes
- Add consumer default specific validation rules to existing framework
- Leverage existing Spring Boot template services
- Maintain Oracle-based persistence layer
- Ensure transaction isolation for concurrent template updates

### **Definition of Done:**
- [ ] Consumer default templates created in existing Oracle tables
- [ ] Template validation extended for default-specific rules
- [ ] Integration tests pass for both legacy and new templates
- [ ] Performance benchmarks meet existing SLA (>99.9% uptime)
- [ ] Security scan passes with no high-risk vulnerabilities
- [ ] Documentation updated for template configuration procedures

---

## **US002: SQL*Loader Integration Enhancement for Default Processing**
**Epic:** Consumer Default ETL Integration  
**Priority:** High  
**Risk Level:** Medium

### **Story:**
As a **Senior Fullstack Developer**, I want to **enhance the existing SQL*Loader integration to support consumer default file formats** so that **we can leverage our proven bulk loading infrastructure for high-volume default data processing while maintaining performance and data integrity**.

### **Acceptance Criteria:**

#### **AC1: Template-Driven Control File Generation** 
- **GIVEN** consumer default templates in field_templates table
- **WHEN** processing a consumer default file
- **THEN** the system shall:
  - Auto-generate SQL*Loader control files based on template metadata
  - Map source file fields to target staging table columns using template definitions
  - Apply data type conversions specified in template format column
  - Handle fixed-width and delimited file formats as configured in templates
  - Generate appropriate WHEN clauses for conditional data loading

#### **AC2: Enhanced Error Handling**
- **GIVEN** SQL*Loader processing with template validation
- **WHEN** data quality issues occur during load
- **THEN** the system shall:
  - Capture rejected records in .bad files with detailed error descriptions
  - Create comprehensive .log files with processing statistics
  - Store error details in existing audit trail tables
  - Implement retry logic for transient failures
  - Send alerts for critical processing failures via existing notification system

#### **AC3: Performance Optimization**
- **GIVEN** high-volume consumer default files (>500K records)
- **WHEN** processing through enhanced SQL*Loader integration
- **THEN** the system shall:
  - Achieve processing rates of >100,000 records per minute
  - Support parallel processing using existing partition strategies
  - Utilize direct-path loading for maximum throughput
  - Maintain transaction isolation during concurrent loads
  - Provide real-time progress monitoring through existing dashboards

### **Technical Requirements:**
- Extend existing batch processing framework
- Leverage current HikariCP connection pooling
- Integrate with existing Spring Batch job framework
- Maintain Oracle-specific performance optimizations
- Preserve existing monitoring and alerting capabilities

### **Definition of Done:**
- [ ] SQL*Loader control file generation from templates working
- [ ] Batch processing performance meets >100K records/minute target
- [ ] Error handling and recovery procedures tested
- [ ] Integration with existing monitoring systems complete
- [ ] Load testing validates high-volume processing capabilities
- [ ] Production deployment runbook updated

---

## **US003: Business Rules Engine for Default Risk Calculations**
**Epic:** Consumer Default ETL Integration  
**Priority:** High  
**Risk Level:** High

### **Story:**
As a **Lending Product Owner**, I want to **implement a business rules engine within the Fabric platform for default risk calculations** so that **we can automate complex default probability, loss given default, and recovery calculations while maintaining regulatory compliance and auditability**.

### **Acceptance Criteria:**

#### **AC1: Risk Calculation Rules Configuration**
- **GIVEN** the existing Fabric template infrastructure
- **WHEN** configuring default risk calculation rules
- **THEN** the system shall:
  - Store business rules in Oracle tables extending the existing template schema
  - Support configurable PD (Probability of Default) models by credit segment
  - Enable LGD (Loss Given Default) calculations based on collateral type and vintage
  - Allow EAD (Exposure at Default) calculations with regulatory adjustments
  - Maintain version control and approval workflows for rule changes
  - Support A/B testing of new calculation methodologies

#### **AC2: Real-Time Risk Processing**
- **GIVEN** consumer default records in staging tables
- **WHEN** applying business rules during ETL processing  
- **THEN** the system shall:
  - Calculate default probabilities using configured models
  - Apply stress testing scenarios (baseline, adverse, severely adverse)
  - Generate risk-adjusted exposures for regulatory reporting
  - Validate calculations against business rule thresholds
  - Flag outliers and exceptions for manual review
  - Update operational risk metrics in real-time

#### **AC3: Regulatory Compliance Integration**
- **GIVEN** calculated default risk metrics
- **WHEN** generating regulatory reports
- **THEN** the system shall:
  - Support CECL (Current Expected Credit Loss) calculations
  - Generate Basel III capital adequacy reports
  - Maintain audit trails for all risk calculation changes
  - Provide data lineage for regulatory examination
  - Enable stress testing report generation
  - Support what-if analysis for risk scenario planning

### **Technical Requirements:**
- Implement rules engine using existing Spring Boot framework
- Integrate with Oracle-based template system
- Leverage existing transaction management and error handling
- Maintain compatibility with current security framework
- Support existing batch processing patterns

### **Definition of Done:**
- [ ] Business rules engine integrated with template system
- [ ] Risk calculation models validated by Risk Management team
- [ ] Regulatory compliance reports generate successfully
- [ ] Performance testing confirms <2 second response time for calculations  
- [ ] Security review completed with no critical findings
- [ ] Business user acceptance testing passed

---

## **US004: Data Lineage and Audit Enhancement for Default Processing**
**Epic:** Consumer Default ETL Integration  
**Priority:** Medium  
**Risk Level:** Medium

### **Story:**
As a **Principal Enterprise Architect**, I want to **enhance the existing Fabric audit and lineage capabilities for consumer default processing** so that **we can maintain comprehensive data governance, regulatory compliance, and operational transparency throughout the default ETL lifecycle**.

### **Acceptance Criteria:**

#### **AC1: Enhanced Audit Trail Integration**
- **GIVEN** the existing Fabric audit infrastructure
- **WHEN** processing consumer default data
- **THEN** the system shall:
  - Extend existing audit tables to capture default-specific events
  - Record all template changes with approval workflows
  - Track data transformations applied during ETL processing
  - Maintain SOX-compliant audit trails for financial data changes
  - Support regulatory examination data requests
  - Provide audit trail export capabilities for compliance teams

#### **AC2: Data Lineage Tracking Extension**
- **GIVEN** consumer default records flowing through the system
- **WHEN** tracking data lineage from source to target
- **THEN** the system shall:
  - Extend existing lineage tracking to include default processing steps
  - Capture field-level transformations applied via templates
  - Record business rule applications and their impacts
  - Track data quality corrections and their justifications
  - Maintain lineage relationships across batch processing cycles
  - Support impact analysis for upstream system changes

#### **AC3: Operational Monitoring Integration**
- **GIVEN** enhanced audit and lineage capabilities
- **WHEN** monitoring default ETL operations
- **THEN** the system shall:
  - Integrate with existing monitoring dashboards
  - Provide real-time visibility into processing status
  - Generate exception reports for failed or delayed processing
  - Support alerting for data quality threshold breaches
  - Enable drill-down analysis from summary to detail level
  - Maintain processing metrics for SLA compliance reporting

### **Technical Requirements:**
- Extend existing Oracle audit tables with default-specific columns
- Leverage current Spring Boot event publishing framework
- Integrate with existing monitoring and alerting infrastructure
- Maintain compatibility with current data retention policies
- Support existing backup and recovery procedures

### **Definition of Done:**
- [ ] Audit trail enhancements deployed and tested
- [ ] Data lineage tracking validated for end-to-end default processing
- [ ] Monitoring dashboards updated with default processing metrics
- [ ] Regulatory compliance testing completed successfully
- [ ] Performance impact assessment shows <5% overhead
- [ ] Operations team training completed on new capabilities

---

## **Implementation Strategy**

### **Phase 1 (Weeks 1-4): Template Enhancement**
- Extend existing Oracle template tables for consumer default types
- Enhance template validation rules for default-specific requirements
- Update existing Spring Boot services to handle new template types
- Implement comprehensive testing for template backwards compatibility

### **Phase 2 (Weeks 5-8): ETL Processing Enhancement**
- Enhance SQL*Loader integration for default file processing
- Extend existing batch processing framework
- Implement enhanced error handling and recovery procedures
- Performance test with realistic data volumes

### **Phase 3 (Weeks 9-12): Business Rules Integration**
- Implement business rules engine within existing framework
- Configure default risk calculation models
- Integrate with regulatory reporting capabilities
- Complete business user acceptance testing

### **Phase 4 (Weeks 13-16): Audit and Monitoring**
- Enhance existing audit and lineage tracking
- Extend monitoring dashboards for default processing
- Implement regulatory compliance reporting
- Complete end-to-end integration testing

### **Risk Mitigation:**
- **Architecture Risk**: Mitigated by leveraging proven Fabric infrastructure
- **Performance Risk**: Addressed through incremental testing and existing optimization patterns
- **Compliance Risk**: Managed through existing regulatory framework extensions
- **Integration Risk**: Minimized by building on established Oracle-based patterns

### **Success Metrics:**
- **Processing Performance**: >100,000 records/minute (same as existing p327 processing)
- **System Availability**: >99.9% uptime (consistent with current Fabric SLA)
- **Data Quality**: >99.99% accuracy (matching existing template validation performance)
- **Time to Market**: <16 weeks total implementation (4 phases × 4 weeks)

This revised approach leverages the existing Fabric platform's strengths while minimizing architectural complexity and reducing implementation risk. The Oracle-based template system provides a proven foundation for high-volume, high-quality data processing with comprehensive audit and compliance capabilities.