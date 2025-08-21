/**
 * Template Configuration Page Object for Fabric Platform E2E Testing
 * Handles template configuration workflow and field mapping interactions
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */

const BasePage = require('../BasePage');

class TemplateConfigurationPage extends BasePage {
    constructor() {
        super();
        this.url = '/template-configuration';
    }

    // Page Elements - Selectors
    get pageTitle() { return $('h1, [data-testid="page-title"]'); }
    get breadcrumb() { return $('[data-testid="breadcrumb"]'); }
    
    // Template Selection
    get templateDropdown() { return $('[data-testid="template-selector"]'); }
    get templateOptions() { return $$('[data-testid="template-option"]'); }
    get selectedTemplate() { return $('[data-testid="selected-template"]'); }
    
    // Source System Selection
    get sourceSystemDropdown() { return $('[data-testid="source-system-selector"]'); }
    get sourceSystemOptions() { return $$('[data-testid="source-system-option"]'); }
    get addSourceSystemButton() { return $('[data-testid="add-source-system"]'); }
    
    // Master Query Section
    get masterQuerySection() { return $('[data-testid="master-query-section"]'); }
    get masterQueryDropdown() { return $('[data-testid="master-query-selector"]'); }
    get masterQueryOptions() { return $$('[data-testid="master-query-option"]'); }
    get masterQueryPreview() { return $('[data-testid="master-query-preview"]'); }
    get testQueryButton() { return $('[data-testid="test-query"]'); }
    
    // Field Mapping Section
    get fieldMappingSection() { return $('[data-testid="field-mapping-section"]'); }
    get fieldMappingTable() { return $('[data-testid="field-mapping-table"]'); }
    get sourceFieldsPanel() { return $('[data-testid="source-fields-panel"]'); }
    get targetFieldsPanel() { return $('[data-testid="target-fields-panel"]'); }
    get addFieldMappingButton() { return $('[data-testid="add-field-mapping"]'); }
    
    // Field Mapping Rows
    get fieldMappingRows() { return $$('[data-testid="field-mapping-row"]'); }
    get sourceFieldInputs() { return $$('[data-testid="source-field-input"]'); }
    get targetFieldInputs() { return $$('[data-testid="target-field-input"]'); }
    get dataTypeSelects() { return $$('[data-testid="data-type-select"]'); }
    get lengthInputs() { return $$('[data-testid="length-input"]'); }
    get requiredCheckboxes() { return $$('[data-testid="required-checkbox"]'); }
    get deleteFieldButtons() { return $$('[data-testid="delete-field"]'); }
    
    // Configuration Actions
    get saveConfigButton() { return $('[data-testid="save-config"]'); }
    get previewConfigButton() { return $('[data-testid="preview-config"]'); }
    get resetConfigButton() { return $('[data-testid="reset-config"]'); }
    get exportConfigButton() { return $('[data-testid="export-config"]'); }
    
    // Preview and Validation
    get configPreviewDialog() { return $('[data-testid="config-preview-dialog"]'); }
    get yamlPreviewPanel() { return $('[data-testid="yaml-preview-panel"]'); }
    get validationErrorsPanel() { return $('[data-testid="validation-errors"]'); }
    get validationSuccessMessage() { return $('[data-testid="validation-success"]'); }
    
    // Loading and Status
    get loadingSpinner() { return $('[data-testid="loading-spinner"]'); }
    get statusMessage() { return $('[data-testid="status-message"]'); }
    get errorMessage() { return $('[data-testid="error-message"]'); }
    get successMessage() { return $('[data-testid="success-message"]'); }

    // Page Navigation Methods
    navigateToPage() {
        this.open(this.url);
        this.waitForPageLoad();
        this.waitForLoadingComplete();
    }

    waitForPageReady() {
        this.waitForElement(this.pageTitle);
        this.waitForElement(this.templateDropdown);
        this.waitForLoadingComplete();
    }

    // Template Selection Methods
    selectTemplate(templateName) {
        this.waitForClickable(this.templateDropdown);
        this.safeClick(this.templateDropdown);
        
        const templateOption = this.templateOptions.find(option => 
            option.getText().includes(templateName)
        );
        
        if (!templateOption) {
            throw new Error(`Template "${templateName}" not found`);
        }
        
        this.safeClick(templateOption);
        this.waitForLoadingComplete();
    }

    getSelectedTemplate() {
        return this.getElementText(this.selectedTemplate);
    }

    // Source System Methods
    selectSourceSystem(sourceSystemName) {
        this.waitForClickable(this.sourceSystemDropdown);
        this.safeClick(this.sourceSystemDropdown);
        
        const sourceSystemOption = this.sourceSystemOptions.find(option =>
            option.getText().includes(sourceSystemName)
        );
        
        if (!sourceSystemOption) {
            throw new Error(`Source System "${sourceSystemName}" not found`);
        }
        
        this.safeClick(sourceSystemOption);
        this.waitForLoadingComplete();
    }

    addNewSourceSystem(systemName, systemCode, description) {
        this.safeClick(this.addSourceSystemButton);
        
        // Handle add source system dialog (assuming it opens)
        const dialogNameInput = $('[data-testid="source-system-name-input"]');
        const dialogCodeInput = $('[data-testid="source-system-code-input"]');
        const dialogDescInput = $('[data-testid="source-system-description-input"]');
        const dialogSaveButton = $('[data-testid="source-system-save"]');
        
        this.waitForElement(dialogNameInput);
        this.safeInput(dialogNameInput, systemName);
        this.safeInput(dialogCodeInput, systemCode);
        this.safeInput(dialogDescInput, description);
        this.safeClick(dialogSaveButton);
        
        this.waitForLoadingComplete();
    }

    // Master Query Methods
    selectMasterQuery(queryName) {
        this.waitForClickable(this.masterQueryDropdown);
        this.safeClick(this.masterQueryDropdown);
        
        const queryOption = this.masterQueryOptions.find(option =>
            option.getText().includes(queryName)
        );
        
        if (!queryOption) {
            throw new Error(`Master Query "${queryName}" not found`);
        }
        
        this.safeClick(queryOption);
        this.waitForLoadingComplete();
    }

    previewMasterQuery() {
        this.safeClick(this.testQueryButton);
        this.waitForElement(this.masterQueryPreview);
        this.waitForLoadingComplete();
    }

    getMasterQueryPreview() {
        return this.getElementText(this.masterQueryPreview);
    }

    // Field Mapping Methods
    addFieldMapping(sourceField, targetField, dataType = 'STRING', length = 255, required = false) {
        this.safeClick(this.addFieldMappingButton);
        this.waitForLoadingComplete();
        
        // Get the last row (newly added)
        const rows = this.fieldMappingRows;
        const lastRow = rows[rows.length - 1];
        
        // Fill in field mapping details
        const sourceInput = lastRow.$('[data-testid="source-field-input"]');
        const targetInput = lastRow.$('[data-testid="target-field-input"]');
        const dataTypeSelect = lastRow.$('[data-testid="data-type-select"]');
        const lengthInput = lastRow.$('[data-testid="length-input"]');
        const requiredCheckbox = lastRow.$('[data-testid="required-checkbox"]');
        
        this.safeInput(sourceInput, sourceField);
        this.safeInput(targetInput, targetField);
        
        // Select data type
        this.safeClick(dataTypeSelect);
        const dataTypeOption = $(`[data-value="${dataType}"]`);
        this.safeClick(dataTypeOption);
        
        this.safeInput(lengthInput, length.toString());
        
        if (required) {
            this.safeClick(requiredCheckbox);
        }
    }

    updateFieldMapping(rowIndex, field, value) {
        const row = this.fieldMappingRows[rowIndex];
        const input = row.$(`[data-testid="${field}-input"]`);
        this.safeInput(input, value);
    }

    deleteFieldMapping(rowIndex) {
        const deleteButton = this.deleteFieldButtons[rowIndex];
        this.safeClick(deleteButton);
        this.handleAlert(true); // Accept confirmation
        this.waitForLoadingComplete();
    }

    getFieldMappingCount() {
        return this.fieldMappingRows.length;
    }

    validateFieldMappings() {
        const validationErrors = [];
        
        this.fieldMappingRows.forEach((row, index) => {
            const sourceField = row.$('[data-testid="source-field-input"]').getValue();
            const targetField = row.$('[data-testid="target-field-input"]').getValue();
            
            if (!sourceField.trim()) {
                validationErrors.push(`Row ${index + 1}: Source field is required`);
            }
            
            if (!targetField.trim()) {
                validationErrors.push(`Row ${index + 1}: Target field is required`);
            }
        });
        
        return validationErrors;
    }

    // Configuration Actions Methods
    saveConfiguration() {
        this.safeClick(this.saveConfigButton);
        this.waitForLoadingComplete();
        
        // Wait for success/error message
        browser.waitUntil(() => {
            return this.elementExists('[data-testid="success-message"]') ||
                   this.elementExists('[data-testid="error-message"]');
        }, { timeout: 10000 });
    }

    previewConfiguration() {
        this.safeClick(this.previewConfigButton);
        this.waitForElement(this.configPreviewDialog);
        this.waitForLoadingComplete();
    }

    resetConfiguration() {
        this.safeClick(this.resetConfigButton);
        this.handleAlert(true); // Accept confirmation
        this.waitForLoadingComplete();
    }

    exportConfiguration() {
        this.safeClick(this.exportConfigButton);
        // Handle file download
        browser.pause(2000); // Wait for download
    }

    closePreviewDialog() {
        const closeButton = this.configPreviewDialog.$('[data-testid="close-dialog"]');
        this.safeClick(closeButton);
    }

    // Validation and Status Methods
    getValidationErrors() {
        if (!this.elementExists('[data-testid="validation-errors"]')) {
            return [];
        }
        
        const errorElements = this.validationErrorsPanel.$$('[data-testid="validation-error-item"]');
        return errorElements.map(element => element.getText());
    }

    isValidationSuccessful() {
        return this.elementExists('[data-testid="validation-success"]');
    }

    getStatusMessage() {
        if (this.elementExists('[data-testid="success-message"]')) {
            return this.getElementText(this.successMessage);
        }
        
        if (this.elementExists('[data-testid="error-message"]')) {
            return this.getElementText(this.errorMessage);
        }
        
        return null;
    }

    isConfigurationSaved() {
        return this.elementExists('[data-testid="success-message"]') &&
               this.getStatusMessage().includes('saved successfully');
    }

    // Complete Workflow Methods
    configureTemplateBasic(templateName, sourceSystem, masterQuery) {
        this.navigateToPage();
        this.waitForPageReady();
        
        this.selectTemplate(templateName);
        this.selectSourceSystem(sourceSystem);
        this.selectMasterQuery(masterQuery);
        
        return this;
    }

    configureCompleteTemplate(config) {
        const {
            templateName,
            sourceSystem,
            masterQuery,
            fieldMappings = []
        } = config;
        
        this.configureTemplateBasic(templateName, sourceSystem, masterQuery);
        
        // Add field mappings
        fieldMappings.forEach(mapping => {
            this.addFieldMapping(
                mapping.sourceField,
                mapping.targetField,
                mapping.dataType || 'STRING',
                mapping.length || 255,
                mapping.required || false
            );
        });
        
        // Validate and save
        const errors = this.validateFieldMappings();
        if (errors.length > 0) {
            throw new Error(`Validation errors: ${errors.join(', ')}`);
        }
        
        this.saveConfiguration();
        
        if (!this.isConfigurationSaved()) {
            const statusMessage = this.getStatusMessage();
            throw new Error(`Configuration save failed: ${statusMessage}`);
        }
        
        return this;
    }
}

module.exports = TemplateConfigurationPage;