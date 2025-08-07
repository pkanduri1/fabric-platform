# User Story US002: Template Configuration Source System Enhancement

## **User Story**

**As a** Fabric Platform user configuring consumer default templates  
**I want** to select and manage source systems directly within the Template Configuration page  
**So that** I can create template-to-source system associations without context switching and configure field-level mappings efficiently

## **Current Problem**

The existing Template Configuration page at `http://localhost:3000/template-configuration` has these limitations:
- Source System field is disabled with text "Shaw System (Selected from navigation)"
- Users must remember to select source system in sidebar before navigating to template configuration
- No ability to add new source systems
- No visibility into which source systems are already configured for templates
- Context switching between sidebar navigation and template configuration causes user errors

## **Acceptance Criteria**

### **AC1: Enhanced Source System Selection**
- **GIVEN** I am on the Template Configuration page
- **WHEN** I reach Step 1 "Select Template"  
- **THEN** I should see an active "Source System" dropdown instead of the disabled field
- **AND** the dropdown should show all available source systems (HR System, DDA System, Shaw System, etc.)
- **AND** each source system should indicate if it already has configurations for the selected template

### **AC2: Add New Source System Capability**
- **GIVEN** I am selecting a source system in Step 1
- **WHEN** I click the "Add New System" button next to the source system dropdown
- **THEN** a modal dialog should open allowing me to create a new source system
- **AND** I should be able to enter: System ID, System Name, Description, Connection Type
- **AND** the system should validate that the System ID is unique
- **AND** after successful creation, the new system should appear in the dropdown

### **AC3: Enhanced Field-Level Mapping with Source Context**
- **GIVEN** I have selected a template and source system
- **WHEN** I reach Step 2 "Configure Field Mappings"
- **THEN** the field mapping table should show the selected source system name in the header
- **AND** source field input placeholders should reference the selected source system
- **AND** I should see a preview of each field mapping (source field → target field)
- **AND** I should have enhanced transformation options (Direct, Constant, Formula, Lookup, Conditional)

### **AC4: Template-Source Association Storage**
- **GIVEN** I have configured field mappings for a template and source system
- **WHEN** I click "Generate & Save Configuration" in Step 3
- **THEN** the system should save both the batch configuration AND the template-source field mappings
- **AND** I should see success confirmation showing template, source system, job name, and field count
- **AND** future visits to this template-source combination should load the saved field mappings

### **AC5: Template Admin Page Unchanged**
- **GIVEN** I navigate to Template Admin page
- **WHEN** I view or edit any template
- **THEN** all existing functionality should work exactly as before
- **AND** no new UI elements or changes should be visible
- **AND** all existing APIs should continue to work without modification

## **Technical Requirements**

### **Backend Components**
1. **SourceSystemController** - New REST controller for source system management
2. **TemplateSourceMappingController** - New REST controller for template-source associations
3. **SourceSystemService** - Business logic for source system operations
4. **TemplateSourceMappingService** - Business logic for template-source mappings
5. **Database Tables** - New tables for source systems and template-source mappings

### **Frontend Components**
1. **Enhanced TemplateConfigurationPage** - Modified existing page with source system dropdown
2. **AddSourceSystemModal** - New modal component for creating source systems
3. **Enhanced Field Mapping Table** - Modified existing table with source system context
4. **Source System API Service** - New API service for source system operations

### **Database Schema**
1. **FABRIC_SOURCE_SYSTEMS** - Store source system definitions
2. **FABRIC_TEMPLATE_SOURCE_MAPPINGS** - Store template-source field mappings

## **Definition of Done**

- [ ] All acceptance criteria are met and tested
- [ ] Backend APIs are implemented with proper error handling
- [ ] Frontend components are implemented with responsive design
- [ ] Database schema is created with proper indexes and constraints
- [ ] Integration tests cover happy path and error scenarios
- [ ] Code review completed and approved
- [ ] Documentation updated
- [ ] No regressions in existing Template Admin functionality
- [ ] Performance testing shows no degradation
- [ ] User acceptance testing completed successfully

## **Dependencies**

- **Prerequisite**: US001 (Template Management System) must be completed
- **Database Access**: Oracle database schema modification permissions
- **Testing Environment**: Local development environment with both frontend and backend running

## **Risks and Mitigations**

| **Risk** | **Impact** | **Mitigation** |
|----------|------------|----------------|
| Database performance with new tables | Medium | Implement proper indexing and query optimization |
| User adoption of new workflow | Low | Maintain familiar 3-step process with enhancements |
| Integration complexity | Medium | Additive changes only, no modifications to existing APIs |
| Data consistency between old and new systems | High | Implement proper validation and rollback mechanisms |

## **Success Metrics**

- Template configuration time reduced by 50%
- User errors in template-source association reduced by 80%
- 100% of users can complete template configuration without sidebar dependency
- Zero regressions in existing Template Admin functionality
- New source system creation time under 2 minutes

## **Future Enhancements (Out of Scope)**

- Bulk template-source configuration
- Advanced transformation logic editor
- Template usage analytics dashboard
- Source system connection testing
- Template versioning and history

---

**Epic**: Consumer Default ETL Platform  
**Priority**: High  
**Estimated Effort**: 5-8 weeks  
**Assigned Team**: Fabric Platform Development Team