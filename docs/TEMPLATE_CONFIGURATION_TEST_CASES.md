# Template Configuration Test Cases Document

**Document Version:** 1.0  
**Created Date:** August 22, 2025  
**Test Framework:** Banking/Financial Industry Standard  
**Application:** Fabric Platform - Template Configuration Module  
**Environment:** Enterprise Banking Batch Processing System  

## Document Overview

This document provides comprehensive test cases for the Template Configuration page functionality covering all 17 major functional areas. The test cases are designed to meet banking industry standards and SOX compliance requirements.

## Test Environment Setup

### Prerequisites
- Backend API running on http://localhost:8080
- Oracle Database with CM3INT schema
- Authentication system with RBAC enabled
- Master Query framework available
- Source systems configured

### Test Data Requirements
- Sample file types: p327, atoctran, default
- Sample transaction types: 200, 300, 900, default
- Master queries with various column types
- Source systems: HR, SHAW, ENCORE, CUSTOMER
- Field templates with different transformation types

---

## Test Case Categories

## 1. CORE TEMPLATE MANAGEMENT

### TC-TEMP-001: Load Available File Types
**Priority:** Critical  
**Module:** Core Template Management  
**Test Objective:** Verify system loads all available file types correctly

**Prerequisites:**
- Backend API accessible
- Database contains file type templates

**Test Steps:**
1. Navigate to Template Configuration page
2. Observe file type dropdown population
3. Verify loading indicator appears during fetch
4. Check dropdown contains expected file types

**Expected Results:**
- File types load within 3 seconds
- Dropdown shows: p327, atoctran, default (minimum)
- Each file type shows description
- No error messages displayed
- Loading indicator disappears after successful load

**Test Data:**
```
File Types Expected:
- p327: "P327 Transaction Format"
- atoctran: "ATOC Transaction Format" 
- default: "Default Template Format"
```

---

### TC-TEMP-002: File Type Selection Validation
**Priority:** Critical  
**Module:** Core Template Management  
**Test Objective:** Validate file type selection triggers transaction type loading

**Prerequisites:**
- File types loaded successfully
- Transaction types available for selected file type

**Test Steps:**
1. Select file type "p327" from dropdown
2. Verify transaction type dropdown becomes enabled
3. Check transaction types populate correctly
4. Verify placeholder text shows "Select Transaction Type" when multiple options exist
5. Verify auto-selection if only one transaction type exists

**Expected Results:**
- Transaction type dropdown becomes enabled immediately
- Placeholder "Select..." displays when multiple options available
- Previous transaction type selection clears when switching file types
- Available transaction types display correctly
- Auto-selection occurs for single option (no placeholder shown)
- Job name field shows auto-generated value after selection

**Additional Test Scenario - File Type Switching:**
1. Select "atoctran" file type with multiple transaction types
2. Select a transaction type (e.g., "200")
3. Switch to "tranert" file type (also has multiple transaction types)
4. Verify transaction type dropdown resets to show "Select..." placeholder
5. Verify previous selection is cleared
6. Verify template fields are cleared

---

### TC-TEMP-003: Template Field Loading
**Priority:** Critical  
**Module:** Core Template Management  
**Test Objective:** Verify template fields load correctly based on file type and transaction type

**Prerequisites:**
- File type and transaction type selected
- Template fields exist in database

**Test Steps:**
1. Select file type "atoctran"
2. Select transaction type "200"
3. Wait for template fields to load
4. Verify field table populates with correct data
5. Check field properties are displayed accurately

**Expected Results:**
- Template fields load within 5 seconds
- All required fields present: fieldName, targetPosition, length, dataType
- Field ordering matches targetPosition
- Required fields marked with "Required" chip
- No duplicate field names or positions

**Test Data:**
```
Expected Fields for atoctran/200:
- Field 1: record_type (position 1, length 3, String, Required)
- Field 2: transaction_code (position 4, length 3, String, Required)
- Field 3: account_number (position 7, length 10, String, Required)
```

---

### TC-TEMP-004: Template Field Validation
**Priority:** High  
**Module:** Core Template Management  
**Test Objective:** Verify template field data validation

**Prerequisites:**
- Template fields loaded
- Various field types available for testing

**Test Steps:**
1. Examine loaded template fields
2. Verify field position sequence has no gaps
3. Check field length values are positive integers
4. Validate data types are from allowed list
5. Confirm required fields are properly marked

**Expected Results:**
- No position gaps in field sequence
- All lengths are positive integers
- Data types are valid: String, Number, Date, Boolean, Decimal, Integer
- Required field indicators accurate
- Field names follow naming conventions

---

## 2. SIX-STEP WORKFLOW PROCESS

### TC-TEMP-005: Workflow Step Navigation Forward
**Priority:** Critical  
**Module:** Six-Step Workflow Process  
**Test Objective:** Verify forward navigation through workflow steps

**Prerequisites:**
- Template Configuration page loaded
- All required data available

**Test Steps:**
1. Complete Step 1: Select Template (file type + transaction type)
2. Click "Next" or observe auto-advance to Step 2
3. Complete Step 2: Master Query Selection
4. Continue through each step sequentially
5. Verify each step validates before advancing

**Expected Results:**
- Stepper shows current active step clearly
- Forward navigation only enabled when step is complete
- Each step validates required inputs
- Visual progress indication accurate
- Cannot skip incomplete steps

**Validation Criteria:**
- Step 1: Requires file type AND transaction type
- Step 2: Requires master query selection
- Step 3: Requires query execution completion
- Step 4: Always allows advance (future feature)
- Step 5: Requires field mappings configuration
- Step 6: Allows final configuration generation

---

### TC-TEMP-006: Workflow Step Navigation Backward
**Priority:** High  
**Module:** Six-Step Workflow Process  
**Test Objective:** Verify backward navigation preserves data

**Prerequisites:**
- Multiple workflow steps completed
- Various data entered in each step

**Test Steps:**
1. Complete steps 1-3 with valid data
2. Navigate to step 5 (Configure Mappings)
3. Enter field mapping configurations
4. Navigate back to step 2
5. Verify previous selections preserved
6. Navigate forward again to step 5
7. Confirm field mappings remain intact

**Expected Results:**
- Backward navigation always allowed
- Previous step data preserved completely
- Forward navigation maintains entered data
- No data loss during navigation
- Error states cleared when navigating backward

---

### TC-TEMP-007: Workflow Validation Error Handling
**Priority:** High  
**Module:** Six-Step Workflow Process  
**Test Objective:** Verify error handling prevents invalid workflow advancement

**Prerequisites:**
- Template Configuration page loaded
- Invalid data scenarios prepared

**Test Steps:**
1. Attempt to advance from Step 1 without selecting file type
2. Try to advance from Step 2 without master query selection
3. Attempt Step 3 advancement without query execution
4. Verify error messages display appropriately
5. Confirm navigation blocked until validation passes

**Expected Results:**
- Clear error messages display for each validation failure
- Navigation buttons disabled until requirements met
- Error messages dismissible but re-appear if still invalid
- User guidance provided for completing requirements

**Error Message Examples:**
```
Step 1: "Please complete the current step before proceeding"
Step 2: "Please select a master query to continue"
Step 3: "Please execute query and extract metadata"
```

---

### TC-TEMP-008: Workflow Progress Persistence
**Priority:** Medium  
**Module:** Six-Step Workflow Process  
**Test Objective:** Verify workflow progress persists during session

**Prerequisites:**
- Active user session
- Workflow partially completed

**Test Steps:**
1. Complete steps 1-3 of workflow
2. Navigate away from Template Configuration page
3. Return to Template Configuration page
4. Verify workflow state preserved (within session limits)
5. Test browser refresh behavior

**Expected Results:**
- Workflow state preserved during same session
- Current step and entered data maintained
- Browser refresh may reset (acceptable for MVP)
- Clear indication if state was lost

---

## 3. SOURCE SYSTEM MANAGEMENT

### TC-TEMP-009: Source System Selection
**Priority:** Critical  
**Module:** Source System Management  
**Test Objective:** Verify source system dropdown functions correctly

**Prerequisites:**
- Source systems configured in database
- Template selection completed

**Test Steps:**
1. Locate source system dropdown in Step 1
2. Open dropdown and view available options
3. Select a source system (e.g., "HR - Oracle")
4. Verify selection updates local state
5. Confirm job name updates appropriately

**Expected Results:**
- Dropdown loads within 2 seconds
- Shows system name and type format: "HR - Oracle"
- Selection updates immediately
- Available source fields refresh for selected system

**Test Data:**
```
Expected Source Systems:
- HR - Oracle
- SHAW - Oracle  
- ENCORE - Oracle
- CUSTOMER - File
```

---

### TC-TEMP-010: Add New Source System
**Priority:** High  
**Module:** Source System Management  
**Test Objective:** Verify new source system creation functionality

**Prerequisites:**
- User has permissions to create source systems
- Source system dropdown loaded

**Test Steps:**
1. Open source system dropdown
2. Click "Add New Source System" option
3. Fill out source system creation dialog:
   - Name: "TESTBANK"
   - Description: "Test Banking System"
   - System Type: "Oracle"
4. Submit the form
5. Verify new source system appears in dropdown
6. Confirm success message displayed

**Expected Results:**
- Dialog opens correctly with all required fields
- Form validation prevents submission with incomplete data
- Success message displays upon creation
- New source system immediately available for selection
- Dropdown refreshes to include new system

**Validation Rules:**
- Name: Required, alphanumeric only
- Description: Required, minimum 10 characters
- System Type: Required, from predefined list

---

### TC-TEMP-011: Source System Creation Validation
**Priority:** High  
**Module:** Source System Management  
**Test Objective:** Verify source system creation form validation

**Prerequisites:**
- Add source system dialog open

**Test Steps:**
1. Attempt to submit form with empty name field
2. Try to submit with name containing special characters
3. Submit with empty description
4. Test with description under 10 characters
5. Verify all validation errors display correctly

**Expected Results:**
- Empty name shows error: "System name is required"
- Invalid characters show error: "Only alphanumeric characters allowed"
- Empty description shows error: "Description is required"
- Short description shows error: "Minimum 10 characters required"
- Submit button disabled until all validation passes

---

### TC-TEMP-012: Source System Error Handling
**Priority:** Medium  
**Module:** Source System Management  
**Test Objective:** Verify error handling for source system operations

**Prerequisites:**
- Backend API accessible but may return errors

**Test Steps:**
1. Simulate network error during source system loading
2. Attempt to create duplicate source system name
3. Test with backend API temporarily unavailable
4. Verify appropriate error messages display
5. Confirm user can retry operations

**Expected Results:**
- Network errors show user-friendly messages
- Duplicate name errors prevent creation
- API unavailable shows clear error message
- Retry options available where appropriate
- Error messages are dismissible

---

## 4. MASTER QUERY INTEGRATION

### TC-TEMP-013: Master Query Selector Loading
**Priority:** Critical  
**Module:** Master Query Integration  
**Test Objective:** Verify master query selector displays available queries

**Prerequisites:**
- Master queries configured in database
- Step 2 of workflow reached

**Test Steps:**
1. Navigate to Step 2: Master Query Selection
2. Observe master query selector component loading
3. Verify queries display with correct metadata
4. Check filtering and search capabilities
5. Confirm query details are accurate

**Expected Results:**
- Master queries load within 3 seconds
- Each query shows: name, description, classification
- Queries are paginated if large dataset
- Search/filter functionality works correctly
- Security classifications visible (if applicable)

**Test Data:**
```
Expected Master Queries:
- EMPLOYEE_EXTRACT: "Employee data extraction query"
- TRANSACTION_SUMMARY: "Daily transaction summary"
- CUSTOMER_PROFILE: "Customer profile data"
```

---

### TC-TEMP-014: Master Query Selection
**Priority:** Critical  
**Module:** Master Query Integration  
**Test Objective:** Verify master query selection process

**Prerequisites:**
- Master queries loaded successfully
- Various query types available

**Test Steps:**
1. Select master query "EMPLOYEE_EXTRACT"
2. Verify query details display correctly
3. Confirm selection updates workflow state
4. Check if SQL preview becomes available
5. Verify advancement to next step enabled

**Expected Results:**
- Query selection highlights the chosen query
- Query details panel shows complete information
- SQL preview displays (if available)
- Success message confirms selection
- Next step becomes accessible

**Query Details Include:**
- Query Name and Description
- SQL Statement Preview
- Security Classification
- Data Classification
- Business Justification
- Compliance Tags

---

### TC-TEMP-015: Master Query Context Integration
**Priority:** High  
**Module:** Master Query Integration  
**Test Objective:** Verify master query context provides required functionality

**Prerequisites:**
- Master query selected successfully
- Master query context initialized

**Test Steps:**
1. Verify master query context is accessible
2. Test query execution capabilities
3. Confirm column metadata extraction works
4. Check smart mapping generation (if enabled)
5. Validate context state management

**Expected Results:**
- Master query context properly initialized
- Query execution methods available
- Column metadata extraction functional
- Smart mapping integration works (future feature)
- Context state persists throughout workflow

---

### TC-TEMP-016: Master Query Error Handling
**Priority:** Medium  
**Module:** Master Query Integration  
**Test Objective:** Verify error handling in master query operations

**Prerequisites:**
- Master query selector displayed
- Various error scenarios possible

**Test Steps:**
1. Simulate master query loading failure
2. Test selection of invalid/corrupted query
3. Attempt to execute malformed SQL query
4. Verify timeout handling for long-running queries
5. Check error recovery mechanisms

**Expected Results:**
- Loading failures show appropriate errors
- Invalid queries are marked and non-selectable
- SQL execution errors display clear messages
- Query timeouts handled gracefully
- Users can retry failed operations

---

## 5. FIELD MAPPING CONFIGURATION

### TC-TEMP-017: Field Mapping Table Display
**Priority:** Critical  
**Module:** Field Mapping Configuration  
**Test Objective:** Verify field mapping configuration table displays correctly

**Prerequisites:**
- Template fields loaded successfully
- Step 5 reached in workflow

**Test Steps:**
1. Navigate to Step 5: Configure Field Mappings
2. Observe field mapping table rendering
3. Verify all template fields are displayed
4. Check table columns are properly formatted
5. Confirm interactive elements work correctly

**Expected Results:**
- All template fields display in table format
- Columns: Position, Target Field, Length, Data Type, Format, Source Field, Transformation
- Required fields marked with red "Required" chip
- Table is scrollable if many fields
- All interactive elements are functional

**Table Structure Validation:**
- Position numbers sequential and consistent
- Target field names unique and properly formatted
- Length values positive integers
- Data types from valid enumeration
- Format strings appropriate for data type

---

### TC-TEMP-018: Source Field Mapping
**Priority:** Critical  
**Module:** Field Mapping Configuration  
**Test Objective:** Verify source field assignment functionality

**Prerequisites:**
- Field mapping table displayed
- Available source fields loaded

**Test Steps:**
1. Locate source field input for first template field
2. Enter source field name "employee_id"
3. Verify input accepts the value
4. Move to next field and repeat process
5. Check that mappings are preserved during navigation

**Expected Results:**
- Source field inputs accept text values
- Entered values persist correctly
- No character restrictions except reasonable limits
- Field mappings preserved during step navigation
- Visual indication of mapped vs unmapped fields

**Valid Source Field Examples:**
```
employee_id, first_name, last_name, department
hire_date, annual_salary, status_code, manager_flag
```

---

### TC-TEMP-019: Transformation Type Selection
**Priority:** Critical  
**Module:** Field Mapping Configuration  
**Test Objective:** Verify transformation type dropdown and configuration

**Prerequisites:**
- Field mapping table displayed
- Template fields available for configuration

**Test Steps:**
1. Click transformation type dropdown for a field
2. Verify available options: Source, Constant, Composite, Conditional
3. Select "Constant" transformation type
4. Confirm constant value input field appears
5. Test each transformation type configuration

**Expected Results:**
- Dropdown shows all four transformation types
- Selection immediately updates UI configuration
- Appropriate input fields appear based on selection
- Previous configuration cleared when type changes
- All transformation types function correctly

**Transformation Types:**
- **Source:** Direct field mapping (default)
- **Constant:** Fixed value for all records
- **Composite:** Combine multiple source fields
- **Conditional:** If-then-else logic implementation

---

### TC-TEMP-020: Constant Value Configuration
**Priority:** High  
**Module:** Field Mapping Configuration  
**Test Objective:** Verify constant value transformation setup

**Prerequisites:**
- Field mapping table displayed
- Transformation type set to "Constant"

**Test Steps:**
1. Select "Constant" transformation type
2. Enter constant value "100020" in value field
3. Verify value persists when navigating away and back
4. Test with various data types (string, number, date)
5. Check validation for appropriate data type formats

**Expected Results:**
- Constant value field appears immediately
- Entered values are preserved correctly
- Field accepts values appropriate for target data type
- Visual confirmation of constant transformation
- Value validation matches target field data type

**Test Values by Data Type:**
```
String: "ACTIVE", "NY", "DEFAULT"
Number: "100", "1500.50", "0"
Date: "2025-01-01", "2025-12-31"
Boolean: "Y", "N", "true", "false"
```

---

### TC-TEMP-021: Composite Field Configuration
**Priority:** High  
**Module:** Field Mapping Configuration  
**Test Objective:** Verify composite field transformation setup

**Prerequisites:**
- Field mapping table displayed
- Multiple source fields available

**Test Steps:**
1. Select "Composite" transformation type
2. Add first source field "first_name"
3. Click "+" button to add second source field
4. Add "last_name" as second source field
5. Set delimiter to " " (space)
6. Verify composite configuration displays correctly

**Expected Results:**
- Source field inputs appear for composite configuration
- "+" button adds additional source fields
- "-" button removes source fields (when multiple exist)
- Delimiter field accepts various separator characters
- Preview shows expected composite result format

**Composite Configuration Examples:**
```
Fields: first_name, last_name | Delimiter: " " | Result: "John Doe"
Fields: street, city, state | Delimiter: ", " | Result: "123 Main St, New York, NY"
Fields: area_code, phone_number | Delimiter: "-" | Result: "555-1234"
```

---

### TC-TEMP-022: Composite Field Source Management
**Priority:** Medium  
**Module:** Field Mapping Configuration  
**Test Objective:** Verify adding/removing composite source fields

**Prerequisites:**
- Composite transformation type selected
- Multiple source fields configured

**Test Steps:**
1. Configure composite field with 2 source fields
2. Click "+" to add third source field
3. Enter value for third field
4. Click "-" on middle field to remove it
5. Verify remaining fields adjust correctly
6. Test with maximum number of source fields

**Expected Results:**
- "+" button always available to add more fields
- "-" button only appears when multiple fields exist
- Field removal doesn't affect other field values
- No limit on number of source fields (within reason)
- Field order maintained correctly

---

## 6. ADVANCED CONDITIONAL LOGIC BUILDER

### TC-TEMP-023: Conditional Logic Interface
**Priority:** Critical  
**Module:** Advanced Conditional Logic Builder  
**Test Objective:** Verify conditional logic builder interface loads correctly

**Prerequisites:**
- Field mapping table displayed
- Transformation type set to "Conditional"

**Test Steps:**
1. Select "Conditional" transformation type
2. Verify Smart Conditional Logic Builder accordion appears
3. Expand accordion and check all components present
4. Verify available source fields are displayed
5. Check condition suggestions are loaded

**Expected Results:**
- Accordion titled "🧠 Smart Conditional Logic Builder" appears
- Available source fields display as clickable chips
- Condition suggestions dropdown is populated
- Condition builder form is functional
- All UI elements are properly styled and accessible

**Interface Components:**
- Available Source Fields section with colored chips
- Smart Suggestions autocomplete field
- Condition builder with If/Then/Else inputs
- Test condition button functionality
- Condition preview display

---

### TC-TEMP-024: Source Field Selection for Conditions
**Priority:** High  
**Module:** Advanced Conditional Logic Builder  
**Test Objective:** Verify source field selection aids condition building

**Prerequisites:**
- Conditional logic builder displayed
- Available source fields loaded

**Test Steps:**
1. Observe available source fields displayed as chips
2. Click on a source field chip (e.g., "status_code")
3. Verify field name is added to condition input
4. Click additional fields to build compound conditions
5. Check tooltip information displays field details

**Expected Results:**
- Source fields display as colored chips by data type
- Clicking chip adds field name to condition expression
- Multiple field clicks create compound conditions with AND operator
- Tooltips show field type and sample values
- Field names are properly formatted in condition

**Source Field Chip Colors:**
- Blue: String fields (status_code, department)
- Purple: Number fields (annual_salary, years_experience)
- Grey: Date fields (hire_date, termination_date)
- Default: Boolean fields (manager_flag)

---

### TC-TEMP-025: Smart Condition Suggestions
**Priority:** High  
**Module:** Advanced Conditional Logic Builder  
**Test Objective:** Verify smart condition suggestions functionality

**Prerequisites:**
- Conditional logic builder displayed
- Source fields available for suggestion generation

**Test Steps:**
1. Open smart suggestions autocomplete field
2. Verify condition templates are displayed
3. Select a suggestion like "status_code == \"A\""
4. Confirm suggestion populates condition input
5. Test with different field types for varied suggestions

**Expected Results:**
- Autocomplete shows 15-20 relevant condition suggestions
- Suggestions include examples for different data types
- Selected suggestion immediately populates if-expression field
- Suggestions use actual source field names and sample values
- Complex condition examples are included

**Suggestion Examples:**
```
String Fields:
- status_code == "A"
- department.contains("IT")
- first_name == null || first_name == ""

Number Fields:
- annual_salary >= 75000
- years_experience > 5

Date Fields:
- hire_date >= "2020-01-01"
- termination_date == null

Boolean Fields:
- manager_flag == "Y"
- manager_flag == true
```

---

### TC-TEMP-026: Condition Expression Building
**Priority:** Critical  
**Module:** Advanced Conditional Logic Builder  
**Test Objective:** Verify condition expression input and validation

**Prerequisites:**
- Conditional logic builder active
- Source fields and suggestions available

**Test Steps:**
1. Enter simple condition: status_code == "A"
2. Set "Then" value to "ACTIVE"
3. Set "Else" value to "INACTIVE"
4. Verify condition preview displays correctly
5. Test complex nested conditions

**Expected Results:**
- If expression field accepts conditional logic
- Then and Else fields accept result values
- Condition preview updates in real-time
- Complex expressions are supported
- Field validation prevents obviously invalid syntax

**Simple Condition Example:**
```
If: status_code == "A"
Then: "ACTIVE"
Else: "INACTIVE"
Preview: IF (status_code == "A") THEN "ACTIVE" ELSE "INACTIVE"
```

**Complex Condition Example:**
```
If: annual_salary >= 100000 ? "SENIOR" : (annual_salary >= 75000 ? "MID" : "JUNIOR")
Then: [Not applicable for ternary]
Else: [Not applicable for ternary]
```

---

### TC-TEMP-027: Condition Testing Functionality
**Priority:** High  
**Module:** Advanced Conditional Logic Builder  
**Test Objective:** Verify condition testing with sample data

**Prerequisites:**
- Condition expression entered
- Available source fields with sample data

**Test Steps:**
1. Enter condition: status_code == "A"
2. Click the Preview/Test button (eye icon)
3. Verify test dialog shows mock data and result
4. Test with condition that evaluates to FALSE
5. Check error handling for invalid expressions

**Expected Results:**
- Test dialog displays mock data values clearly
- Condition evaluation result shown as TRUE or FALSE
- Mock data uses sample values from source fields
- Test explains whether condition would MATCH or NOT MATCH
- Invalid syntax errors are caught and displayed appropriately

**Test Dialog Content:**
```
Condition Test Result:

Condition: status_code == "A"

Mock Data:
employee_id: 12345
status_code: A
department: IT
annual_salary: 75000

Result: ✅ TRUE

This means the condition would MATCH with this sample data.
```

---

### TC-TEMP-028: Multiple Conditions Management
**Priority:** Medium  
**Module:** Advanced Conditional Logic Builder  
**Test Objective:** Verify adding and managing multiple conditions

**Prerequisites:**
- Conditional logic builder active
- First condition configured

**Test Steps:**
1. Configure first condition completely
2. Click "+ Add Another Condition" button
3. Configure second condition with different logic
4. Verify both conditions display correctly
5. Test removing conditions with multiple present

**Expected Results:**
- Additional conditions can be added without limit
- Each condition has independent configuration
- Remove button only appears when multiple conditions exist
- Condition numbering updates automatically
- All conditions are saved and preserved

---

## 7. IMPORT/EXPORT CAPABILITIES

### TC-TEMP-029: Sample CSV Download
**Priority:** High  
**Module:** Import/Export Capabilities  
**Test Objective:** Verify sample CSV file download functionality

**Prerequisites:**
- Template Configuration page loaded
- Step 6 reached or export functions visible

**Test Steps:**
1. Locate "Download Sample CSV" button
2. Click the button to initiate download
3. Verify CSV file downloads successfully
4. Open downloaded file and examine content
5. Confirm sample data includes all transformation types

**Expected Results:**
- CSV file downloads immediately without errors
- File named: "sample_field_mapping_with_examples.csv"
- Contains header row with all required columns
- Includes 7+ sample records demonstrating different transformations
- File is properly formatted and opens in Excel/text editor

**CSV Content Validation:**
```
Headers: fieldName, sourceField, transformationType, value, defaultValue, delimiter, condition_if, condition_then, condition_else, description

Sample Records:
1. Direct mapping (source)
2. Constant value 
3. Composite field combination
4. Simple conditional logic
5. Complex nested conditional
6. Multi-field composite with different delimiter
7. Salary grading conditional example
```

---

### TC-TEMP-030: Configuration Import from CSV
**Priority:** High  
**Module:** Import/Export Capabilities  
**Test Objective:** Verify CSV file import functionality

**Prerequisites:**
- Valid CSV file prepared
- Template fields loaded in configuration table

**Test Steps:**
1. Click "Import Configuration" button
2. Select prepared CSV file through file dialog
3. Wait for file processing to complete
4. Verify success message displays
5. Check that field mappings are applied correctly

**Expected Results:**
- File dialog opens correctly for .csv, .xlsx, .xls files
- File upload processes within 10 seconds
- Success message shows number of mappings applied
- Field mapping table updates with imported values
- Imported configurations match CSV content exactly

**Success Message Example:**
```
"Successfully imported configuration from test_mapping.csv. 
12 field mappings were applied."
```

---

### TC-TEMP-031: Import File Validation
**Priority:** High  
**Module:** Import/Export Capabilities  
**Test Objective:** Verify file validation during import process

**Prerequisites:**
- Various test files prepared (valid and invalid)

**Test Steps:**
1. Attempt to import file larger than 5MB
2. Try to import unsupported file type (.txt, .pdf)
3. Upload CSV with missing required columns
4. Test with corrupted/malformed CSV file
5. Verify appropriate error messages for each scenario

**Expected Results:**
- Large files rejected with size limit error
- Unsupported file types rejected immediately
- Missing columns show specific error message
- Corrupted files handled gracefully
- Clear error messages explain the issue and provide guidance

**Error Message Examples:**
```
File Size: "File size too large. Please upload files smaller than 5MB."
File Type: "Invalid file type. Please upload Excel (.xlsx, .xls) or CSV (.csv) files only."
Missing Data: "No data found in the uploaded file."
Format Error: "No matching fields found in the uploaded file. Please ensure the file contains columns like 'fieldName', 'sourceField', 'transformationType', etc."
```

---

### TC-TEMP-032: Export Current Template
**Priority:** Medium  
**Module:** Import/Export Capabilities  
**Test Objective:** Verify current template export functionality

**Prerequisites:**
- Template fields loaded with some configurations applied
- Export functions accessible

**Test Steps:**
1. Configure several field mappings with different transformation types
2. Click "Export Template" button
3. Verify CSV file downloads with current configuration
4. Open exported file and validate content
5. Confirm exported file can be re-imported successfully

**Expected Results:**
- Export generates CSV file immediately
- File name includes file type, transaction type, and date
- Exported data matches current field configuration exactly
- File format compatible with import function
- Round-trip export/import preserves all data

**Export File Naming:**
```
Format: template_{fileType}_{transactionType}_{YYYY-MM-DD}.csv
Example: template_atoctran_200_2025-08-22.csv
```

---

### TC-TEMP-033: Excel File Handling
**Priority:** Low  
**Module:** Import/Export Capabilities  
**Test Objective:** Verify Excel file import attempt handling

**Prerequisites:**
- Excel file (.xlsx) prepared for testing

**Test Steps:**
1. Attempt to import Excel file through import dialog
2. Verify system response to Excel file selection
3. Check error message is informative
4. Confirm user is guided to use CSV format instead

**Expected Results:**
- Excel file selection is allowed (file dialog accepts .xlsx)
- Clear error message explains Excel parsing limitation
- User guidance provided for CSV conversion
- No system errors or crashes occur

**Expected Error Message:**
```
"Excel file parsing requires additional library. Please use CSV format for now, or implement xlsx library support."
```

---

## 8. DATA VALIDATION & TESTING

### TC-TEMP-034: Template Field Data Validation
**Priority:** Critical  
**Module:** Data Validation & Testing  
**Test Objective:** Verify template field data validation rules

**Prerequisites:**
- Template fields loaded
- Various field configurations available

**Test Steps:**
1. Examine loaded template fields for data consistency
2. Verify position numbers are sequential and unique
3. Check length values are positive integers
4. Validate data types are from approved list
5. Confirm required field designations are accurate

**Expected Results:**
- No duplicate position numbers exist
- Position sequence has no gaps (1, 2, 3, 4...)
- All length values are positive integers (> 0)
- Data types are valid: String, Number, Date, Boolean, Decimal, Integer
- Required field indicators (Y/N) are consistent

**Validation Rules:**
```
Position: Must be unique positive integer, sequential preferred
Length: Must be positive integer (1-9999)
Data Type: Must be from enumerated list
Required: Must be 'Y' or 'N'
Field Name: Must be unique within template
```

---

### TC-TEMP-035: Source Field Mapping Validation
**Priority:** High  
**Module:** Data Validation & Testing  
**Test Objective:** Verify source field mapping validation

**Prerequisites:**
- Field mapping configuration active
- Source fields entered for various template fields

**Test Steps:**
1. Enter valid source field names in mapping inputs
2. Test with invalid characters or formats
3. Check validation for composite field source lists
4. Verify conditional expression validation
5. Test field mapping consistency checks

**Expected Results:**
- Valid source field names accepted without error
- Invalid formats show appropriate warnings
- Composite field source validation works correctly
- Conditional expressions are syntax-checked
- Consistency validation prevents conflicts

**Source Field Validation:**
- Alphanumeric characters and underscores allowed
- No spaces or special characters (except underscore)
- Case-insensitive handling
- Length limits reasonable (1-50 characters)

---

### TC-TEMP-036: Transformation Configuration Validation
**Priority:** High  
**Module:** Data Validation & Testing  
**Test Objective:** Verify transformation configuration validation

**Prerequisites:**
- Various transformation types configured
- Different field data types available

**Test Steps:**
1. Configure constant transformation with invalid value for target data type
2. Set up composite transformation with empty source fields
3. Create conditional transformation with invalid syntax
4. Test delimiter validation for composite fields
5. Verify data type compatibility checks

**Expected Results:**
- Invalid constant values show data type validation errors
- Empty composite source fields are flagged
- Invalid conditional syntax is detected and reported
- Delimiter validation ensures appropriate separators
- Data type compatibility is enforced

**Data Type Validation Examples:**
```
String Target: Any value accepted
Number Target: Validates numeric format
Date Target: Validates date format (YYYY-MM-DD)
Boolean Target: Validates Y/N or true/false
```

---

### TC-TEMP-037: Configuration Completeness Validation
**Priority:** High  
**Module:** Data Validation & Testing  
**Test Objective:** Verify overall configuration completeness validation

**Prerequisites:**
- Template configuration partially completed
- Required fields identified

**Test Steps:**
1. Attempt to save configuration with unmapped required fields
2. Test with all required fields mapped but optional fields empty
3. Verify validation for incomplete transformation configurations
4. Check source system and job name validation
5. Test overall configuration integrity

**Expected Results:**
- Required field validation prevents saving incomplete configurations
- Optional fields can remain unmapped without error
- Incomplete transformations are identified and reported
- Source system and job name validation enforced
- Clear feedback provided for missing requirements

**Completeness Requirements:**
- All required template fields must have source mapping or constant value
- Source system must be selected
- Job name must be provided
- Transformation configurations must be complete (no partial setups)

---

## 9. UI/UX FEATURES

### TC-TEMP-038: Responsive Design Validation
**Priority:** Medium  
**Module:** UI/UX Features  
**Test Objective:** Verify responsive design works across different screen sizes

**Prerequisites:**
- Template Configuration page loaded
- Browser with responsive testing capabilities

**Test Steps:**
1. Test page layout on desktop resolution (1920x1080)
2. Resize browser to tablet dimensions (768x1024)
3. Test mobile view (375x667)
4. Verify component responsiveness at each size
5. Check horizontal scrolling behavior for wide tables

**Expected Results:**
- Layout adapts appropriately to different screen sizes
- All interactive elements remain accessible
- Text remains readable at all sizes
- Tables provide horizontal scrolling when necessary
- Mobile view maintains full functionality

**Responsive Breakpoints:**
- Desktop: ≥1200px - Full layout with all components visible
- Tablet: 768px-1199px - Adapted layout with possible stacking
- Mobile: <768px - Stacked layout with scrollable tables

---

### TC-TEMP-039: Loading Indicators and States
**Priority:** Medium  
**Module:** UI/UX Features  
**Test Objective:** Verify loading indicators display correctly during async operations

**Prerequisites:**
- Network conditions that allow observation of loading states

**Test Steps:**
1. Navigate to Template Configuration page and observe initial loading
2. Select file type and watch transaction type loading
3. Initiate master query loading and observe indicators
4. Test configuration saving with loading state
5. Verify error states replace loading indicators appropriately

**Expected Results:**
- Spinner indicators appear immediately for async operations
- Loading text accompanies visual indicators where appropriate
- Loading states prevent user interaction during processing
- Loading indicators disappear when operations complete
- Error states properly replace loading indicators

**Loading Indicator Locations:**
- File type dropdown during initial load
- Transaction type dropdown during fetch
- Template fields table during load
- Master query selector during fetch
- Save operation button during processing

---

### TC-TEMP-040: Error Message Display and Management
**Priority:** High  
**Module:** UI/UX Features  
**Test Objective:** Verify error message display and user interaction

**Prerequisites:**
- Various error conditions can be triggered

**Test Steps:**
1. Trigger network error during file type loading
2. Attempt invalid operation to generate validation error
3. Test error message dismissal functionality
4. Verify error message persistence and clarity
5. Check error message accessibility and styling

**Expected Results:**
- Error messages display prominently with clear styling
- Messages are specific and actionable
- Dismiss functionality works correctly
- Multiple errors can be displayed simultaneously
- Error messages are accessible to screen readers

**Error Message Features:**
- Red alert styling for visibility
- Specific error text with guidance
- "DISMISS" button for user control
- Icon indicators for error type
- Non-blocking (user can continue working)

---

### TC-TEMP-041: Success Message and Feedback
**Priority:** Medium  
**Module:** UI/UX Features  
**Test Objective:** Verify success message display and interaction

**Prerequisites:**
- Operations that generate success feedback

**Test Steps:**
1. Complete template configuration successfully
2. Verify success message displays with appropriate content
3. Test additional action buttons in success message
4. Check success message timing and persistence
5. Verify success message accessibility

**Expected Results:**
- Success messages display with green styling
- Content is specific and informative
- Action buttons (Navigate, Copy Config) function correctly
- Messages persist until user dismisses them
- Success feedback is clear and satisfying

**Success Message Features:**
- Green alert styling for positive feedback
- Detailed success information
- Action buttons for next steps
- Multiline text support for detailed messages
- User-controlled dismissal

---

### TC-TEMP-042: Interactive Element Accessibility
**Priority:** Medium  
**Module:** UI/UX Features  
**Test Objective:** Verify accessibility features for interactive elements

**Prerequisites:**
- Screen reader testing capability
- Keyboard navigation testing

**Test Steps:**
1. Navigate entire workflow using only keyboard
2. Test screen reader compatibility with form elements
3. Verify ARIA labels and descriptions are appropriate
4. Check color contrast for visual elements
5. Test focus management throughout workflow

**Expected Results:**
- All interactive elements accessible via keyboard
- Tab order follows logical workflow progression
- Screen readers can interpret all content correctly
- Color contrast meets accessibility standards (WCAG AA)
- Focus indicators are clearly visible

**Accessibility Features:**
- Tab navigation through all interactive elements
- ARIA labels for complex components
- High contrast mode compatibility
- Screen reader friendly text
- Keyboard shortcuts where appropriate

---

## 10. CONFIGURATION GENERATION

### TC-TEMP-043: Basic Configuration Generation
**Priority:** Critical  
**Module:** Configuration Generation  
**Test Objective:** Verify basic configuration generation from template

**Prerequisites:**
- Complete template selection and field mapping
- Valid source system and job name

**Test Steps:**
1. Complete all required workflow steps
2. Click "Save Configuration" button
3. Verify configuration generation process initiates
4. Check success message displays with configuration details
5. Validate generated configuration structure

**Expected Results:**
- Configuration generation completes within 15 seconds
- Success message shows specific configuration details
- Generated configuration ID is provided
- Field mappings are correctly translated to backend format
- No data loss during generation process

**Generated Configuration Details:**
```
Template: atoctran/200
Job Name: atoctran-200
Transaction Type: 200
Fields saved: 15
Configuration ID: cfg_123456
```

---

### TC-TEMP-044: Configuration with Mixed Transformation Types
**Priority:** High  
**Module:** Configuration Generation  
**Test Objective:** Verify configuration generation with various transformation types

**Prerequisites:**
- Template with fields configured using all transformation types

**Test Steps:**
1. Configure fields with source, constant, composite, and conditional transformations
2. Ensure each transformation type is properly configured
3. Generate configuration and verify all transformations are preserved
4. Check that transformation-specific properties are maintained
5. Validate backend configuration format

**Expected Results:**
- All transformation types are correctly processed
- Source mappings preserved exactly
- Constant values included in configuration
- Composite field configurations maintained with delimiters and source lists
- Conditional expressions preserved with if-then-else logic

**Transformation Preservation:**
```
Source: sourceField property populated
Constant: value property contains constant
Composite: sources array and delimiter preserved
Conditional: conditions array with expressions preserved
```

---

### TC-TEMP-045: Configuration Metadata Generation
**Priority:** Medium  
**Module:** Configuration Generation  
**Test Objective:** Verify configuration metadata is generated correctly

**Prerequisites:**
- Template configuration ready for generation

**Test Steps:**
1. Generate configuration with all metadata tracking
2. Verify template metadata is included in result
3. Check timestamps and user attribution
4. Validate field count and template version information
5. Confirm metadata accuracy

**Expected Results:**
- Template metadata included in generated configuration
- Timestamps accurate for generation time
- User attribution correctly assigned
- Field counts match actual configuration
- Template version information preserved

**Metadata Structure:**
```
templateMetadata: {
  fileType: "atoctran",
  transactionType: "200", 
  templateVersion: 1,
  fieldsFromTemplate: 15,
  totalFields: 15,
  generatedAt: "2025-08-22T...",
  generatedBy: "ui-user"
}
```

---

### TC-TEMP-046: Configuration Save Error Handling
**Priority:** High  
**Module:** Configuration Generation  
**Test Objective:** Verify error handling during configuration save operation

**Prerequisites:**
- Configuration ready for generation
- Backend API may return errors

**Test Steps:**
1. Attempt configuration save with backend API unavailable
2. Test with invalid configuration data
3. Simulate network timeout during save operation
4. Verify user receives appropriate error feedback
5. Check that partial configurations are handled properly

**Expected Results:**
- Backend unavailability shows clear error message
- Invalid data errors provide specific validation feedback
- Timeout errors allow retry attempts
- User guidance provided for resolving errors
- No partial or corrupted configurations saved

**Error Handling Features:**
- Graceful degradation when save fails
- Template generation success acknowledged even if save fails
- User guidance for completing save operation
- Retry mechanisms available
- Clear error messages with next steps

---

## 11. INTEGRATION FEATURES

### TC-TEMP-047: Master Query Context Integration
**Priority:** High  
**Module:** Integration Features  
**Test Objective:** Verify master query context integration works correctly

**Prerequisites:**
- Master query context provider active
- Master query selected and available

**Test Steps:**
1. Verify master query context is properly initialized
2. Test query execution through context
3. Check column metadata extraction functionality
4. Validate context state management throughout workflow
5. Confirm integration with smart mapping (when available)

**Expected Results:**
- Master query context provides required functionality
- Query execution methods accessible and functional
- Column metadata extraction works correctly
- Context state persists throughout user session
- Integration points are well-defined and stable

**Context Integration Points:**
- Query selection and storage
- Query execution and result handling
- Column metadata extraction and caching
- Smart mapping integration hooks
- Error handling and user feedback

---

### TC-TEMP-048: Configuration Context Integration
**Priority:** High  
**Module:** Integration Features  
**Test Objective:** Verify configuration context integration

**Prerequisites:**
- Configuration context provider active
- Source systems and configuration data available

**Test Steps:**
1. Verify configuration context initialization
2. Test source system selection through context
3. Check configuration save functionality
4. Validate context state synchronization
5. Confirm data persistence throughout workflow

**Expected Results:**
- Configuration context properly initialized
- Source system selection updates context correctly
- Save functionality accessible through context
- State synchronization works across components
- Data persistence maintained during session

---

### TC-TEMP-049: API Service Integration
**Priority:** Critical  
**Module:** Integration Features  
**Test Objective:** Verify template API service integration

**Prerequisites:**
- Backend API accessible
- Template API service configured

**Test Steps:**
1. Test file type loading through template API service
2. Verify transaction type fetching functionality
3. Check template field loading integration
4. Test configuration generation API calls
5. Validate error handling in API integration

**Expected Results:**
- All API calls complete successfully
- Response data is properly formatted and handled
- Error responses are caught and processed appropriately
- API timeouts are handled gracefully
- Request/response logging works correctly

**API Integration Points:**
- GET /admin/templates/file-types
- GET /admin/templates/{fileType}/transaction-types
- GET /admin/templates/{fileType}/{transactionType}/fields
- POST /admin/templates/{fileType}/{transactionType}/create-config
- Error handling and retry logic

---

### TC-TEMP-050: Navigation Integration
**Priority:** Medium  
**Module:** Integration Features  
**Test Objective:** Verify navigation integration with other parts of application

**Prerequisites:**
- Full application navigation available
- Template configuration accessible from main navigation

**Test Steps:**
1. Navigate to Template Configuration from main menu
2. Test breadcrumb navigation functionality
3. Verify "Navigate to Manual Config" button functionality
4. Check navigation state preservation
5. Test deep linking to configuration steps

**Expected Results:**
- Navigation to Template Configuration works smoothly
- Breadcrumbs show correct path and are functional
- Manual config navigation passes correct parameters
- Navigation state is preserved appropriately
- Deep linking works for supported routes

---

## 12. SMART FEATURES (PLANNED/FUTURE)

### TC-TEMP-051: Smart Mapping Feature Placeholder
**Priority:** Low  
**Module:** Smart Features (Planned/Future)  
**Test Objective:** Verify smart mapping feature placeholder displays correctly

**Prerequisites:**
- Template configuration workflow active
- Step 4 (Smart Field Mapping) reached

**Test Steps:**
1. Navigate to Step 4: Smart Field Mapping
2. Verify future feature placeholder displays
3. Check warning message about feature availability
4. Test smart mapping toggle (should be disabled)
5. Confirm user can skip this step

**Expected Results:**
- Clear indication that feature is "Coming Soon"
- Warning message explains current status
- Toggle switches are disabled with appropriate styling
- Step can be skipped without issues
- Future roadmap information is provided

**Placeholder Content:**
```
"🚧 Future Roadmap Feature
Smart Field Mapping with Banking Intelligence is currently under development..."

Features listed:
- Intelligent field pattern recognition
- Banking compliance awareness  
- Automated mapping suggestions
- PII and sensitive data detection
```

---

### TC-TEMP-052: Banking Intelligence Toggle
**Priority:** Low  
**Module:** Smart Features (Planned/Future)  
**Test Objective:** Verify banking intelligence toggle behavior

**Prerequisites:**
- Master query selection step reached
- Banking intelligence toggle visible

**Test Steps:**
1. Locate banking intelligence toggle switch
2. Attempt to enable the toggle
3. Verify toggle remains disabled
4. Check tooltip or help text explains status
5. Confirm toggle state doesn't affect workflow

**Expected Results:**
- Toggle switch is clearly disabled
- Attempt to enable shows appropriate feedback
- Help text explains feature is planned for future
- Toggle state doesn't impact current functionality
- Visual styling indicates disabled state clearly

---

### TC-TEMP-053: Smart Mapping Integration Points
**Priority:** Low  
**Module:** Smart Features (Planned/Future)  
**Test Objective:** Verify smart mapping integration points are prepared

**Prerequisites:**
- Column metadata extracted from master query
- Smart mapping context available

**Test Steps:**
1. Verify smart mapping integration hooks exist
2. Check that column metadata is properly structured for future smart mapping
3. Test smart mapping generation method (should be no-op currently)
4. Confirm integration points don't cause errors
5. Validate future extensibility

**Expected Results:**
- Integration hooks exist and are accessible
- Column metadata structure supports smart mapping needs
- Smart mapping methods exist but don't perform operations yet
- No errors generated by disabled features
- Code structure supports future enhancement

---

## 13. FIELD TRANSFORMATION DETAILS

### TC-TEMP-054: Source Transformation Configuration
**Priority:** Critical  
**Module:** Field Transformation Details  
**Test Objective:** Verify source field transformation setup and validation

**Prerequisites:**
- Field mapping table displayed
- Source transformation type selected

**Test Steps:**
1. Select "Source" transformation type (default)
2. Enter source field name in source field input
3. Verify configuration is preserved during navigation
4. Test with various source field name formats
5. Check that no additional configuration fields appear

**Expected Results:**
- Source field input accepts alphanumeric and underscore characters
- Entered source field name is preserved correctly
- No additional configuration options appear for source transformation
- Field validation ensures reasonable field name format
- Configuration correctly maps to source field in target

**Source Field Validation:**
- Accepts: letters, numbers, underscores
- Rejects: spaces, special characters (except underscore)
- Case-insensitive handling
- Length: 1-50 characters

---

### TC-TEMP-055: Constant Transformation Validation
**Priority:** High  
**Module:** Field Transformation Details  
**Test Objective:** Verify constant value transformation with data type validation

**Prerequisites:**
- Field mapping table displayed
- Template fields with different data types available

**Test Steps:**
1. Select String field and set constant transformation
2. Enter string constant value and verify acceptance
3. Select Number field and test numeric constant validation
4. Test Date field with date format validation
5. Check Boolean field with Y/N and true/false values

**Expected Results:**
- String constants accept any text value
- Number constants validate numeric format
- Date constants validate date format (YYYY-MM-DD preferred)
- Boolean constants accept Y/N, true/false variations
- Invalid formats show appropriate validation errors

**Data Type Validation Examples:**
```
String: "ACTIVE", "DEFAULT", "N/A" (any text)
Number: "100", "1500.50", "0", "-25" (numeric format)
Date: "2025-01-01", "2025-12-31" (YYYY-MM-DD)
Boolean: "Y", "N", "true", "false", "1", "0"
Decimal: "100.25", "0.00", "999.99" (decimal format)
Integer: "100", "0", "-5" (whole numbers only)
```

---

### TC-TEMP-056: Composite Transformation Advanced Testing
**Priority:** High  
**Module:** Field Transformation Details  
**Test Objective:** Verify complex composite field transformations

**Prerequisites:**
- Multiple source fields available
- Composite transformation selected

**Test Steps:**
1. Configure composite with 4+ source fields
2. Test various delimiter options (space, comma, dash, pipe)
3. Verify field order is maintained correctly
4. Test with empty source fields in the list
5. Check delimiter escaping and special characters

**Expected Results:**
- Supports unlimited number of source fields (within reason)
- All standard delimiters work correctly: " ", ",", "-", "|", "_"
- Field order maintained as entered by user
- Empty source fields are handled gracefully (skipped or shown as empty)
- Special characters in delimiters are properly escaped

**Composite Configuration Examples:**
```
Name Combination:
  Fields: first_name, middle_initial, last_name
  Delimiter: " "
  Result: "John M Smith"

Address Combination:
  Fields: street, city, state, zip
  Delimiter: ", "
  Result: "123 Main St, New York, NY, 10001"

Account Number:
  Fields: bank_code, branch_code, account_number
  Delimiter: "-"
  Result: "001-025-123456789"

Pipe Delimited:
  Fields: field1, field2, field3
  Delimiter: "|"
  Result: "value1|value2|value3"
```

---

### TC-TEMP-057: Conditional Transformation Complex Logic
**Priority:** High  
**Module:** Field Transformation Details  
**Test Objective:** Verify complex conditional transformation logic

**Prerequisites:**
- Conditional transformation selected
- Multiple source fields available with different data types

**Test Steps:**
1. Create simple if-then-else condition
2. Test complex nested ternary conditions
3. Configure multiple conditions for single field
4. Test conditions with multiple field references
5. Verify logical operators (&&, ||, !) work correctly

**Expected Results:**
- Simple conditions work reliably
- Nested ternary operators are supported
- Multiple conditions can be configured independently
- Compound conditions with multiple fields are supported
- Logical operators function correctly

**Conditional Logic Examples:**
```
Simple Condition:
  If: status_code == "A"
  Then: "ACTIVE"
  Else: "INACTIVE"

Nested Ternary:
  If: annual_salary >= 100000 ? "SENIOR" : (annual_salary >= 75000 ? "MID" : "JUNIOR")

Multiple Field Condition:
  If: status_code == "A" && department == "IT"
  Then: "ACTIVE_IT"
  Else: "OTHER"

Complex Logic:
  If: (annual_salary >= 75000 && years_experience >= 3) || manager_flag == "Y"
  Then: "ELIGIBLE"
  Else: "NOT_ELIGIBLE"
```

---

## 14. DATA SOURCE FEATURES

### TC-TEMP-058: Available Source Fields Loading
**Priority:** High  
**Module:** Data Source Features  
**Test Objective:** Verify available source fields are loaded correctly

**Prerequisites:**
- Source system selected
- Source fields configured for selected system

**Test Steps:**
1. Select source system "HR"
2. Verify source fields are loaded automatically
3. Check that fields include name, type, and sample values
4. Test with different source systems
5. Verify field information is accurate and complete

**Expected Results:**
- Source fields load within 3 seconds of source system selection
- Each field includes: name, data type, sample value
- Field list is comprehensive for selected source system
- Different source systems show different field sets
- Sample values are realistic and helpful

**Expected Source Fields (HR System):**
```
employee_id (string): "12345"
first_name (string): "John"
last_name (string): "Doe"
department (string): "IT"
status_code (string): "A"
hire_date (date): "2020-01-15"
annual_salary (number): "75000"
performance_rating (number): "4.2"
manager_flag (boolean): "Y"
years_experience (number): "5"
```

---

### TC-TEMP-059: Source Field Type Recognition
**Priority:** Medium  
**Module:** Data Source Features  
**Test Objective:** Verify source field type recognition and display

**Prerequisites:**
- Source fields loaded with various data types

**Test Steps:**
1. Examine source field chips in conditional logic builder
2. Verify color coding matches data types correctly
3. Check tooltip information shows field details
4. Test field type validation in conditional expressions
5. Confirm type information is used appropriately

**Expected Results:**
- Field chips are color-coded by data type consistently
- Tooltips show complete field information (type, sample)
- Field types are validated in conditional expressions
- Type information guides condition suggestions
- Visual indicators help users understand field characteristics

**Color Coding Standards:**
- Blue: String fields
- Purple: Number/Decimal/Integer fields
- Grey: Date fields
- Default: Boolean fields

---

### TC-TEMP-060: Source System Switching
**Priority:** Medium  
**Module:** Data Source Features  
**Test Objective:** Verify behavior when switching source systems

**Prerequisites:**
- Multiple source systems available
- Initial source system selected with configuration

**Test Steps:**
1. Configure field mappings with initial source system
2. Switch to different source system
3. Verify source fields update correctly
4. Check that previous field mappings are preserved
5. Test condition suggestions update for new source fields

**Expected Results:**
- Source field list updates immediately when source system changes
- Previous field mappings are preserved (may become invalid)
- Condition suggestions refresh for new available fields
- User is notified if mappings become invalid due to missing fields
- System maintains configuration integrity

---

## 15. ADDITIONAL UTILITIES

### TC-TEMP-061: Auto-Generated Job Name
**Priority:** Medium  
**Module:** Additional Utilities  
**Test Objective:** Verify auto-generated job name functionality

**Prerequisites:**
- File type and transaction type selection available

**Test Steps:**
1. Select file type "atoctran"
2. Select transaction type "200"
3. Verify job name auto-generates as "atoctran-200"
4. Test with different file type/transaction type combinations
5. Verify job name can be manually edited

**Expected Results:**
- Job name generates immediately upon template selection
- Format follows pattern: {fileType}-{transactionType}
- Job name field is editable by user
- Manual edits are preserved
- Job name validation ensures valid format

**Auto-Generation Examples:**
```
p327 + 200 = "p327-200"
atoctran + 300 = "atoctran-300"
default + default = "default-default"
```

---

### TC-TEMP-062: Configuration Summary Display
**Priority:** Medium  
**Module:** Additional Utilities  
**Test Objective:** Verify configuration summary displays correctly

**Prerequisites:**
- Configuration generation completed successfully
- Generated configuration available

**Test Steps:**
1. Complete configuration generation process
2. Verify configuration summary card displays
3. Check all summary information is accurate
4. Test field mapping preview functionality
5. Verify summary updates with configuration changes

**Expected Results:**
- Summary card displays immediately after generation
- All information is accurate: job name, source system, transaction type, field count
- Field mapping preview shows first 5 fields with correct details
- "... and X more fields" message for large configurations
- Summary reflects current configuration state

**Summary Content:**
```
Job: atoctran-200
Source System: HR
Transaction Type: 200
Fields: 15

Field Mappings:
1. record_type → employee_type (source)
2. transaction_code → "200" (constant)
3. account_number → employee_id (source)
4. full_name → first_name,last_name (composite)
5. status_indicator → status_code conditional (conditional)
... and 10 more fields
```

---

### TC-TEMP-063: Clipboard Copy Functionality
**Priority:** Low  
**Module:** Additional Utilities  
**Test Objective:** Verify configuration copy to clipboard functionality

**Prerequisites:**
- Configuration generated successfully
- Modern browser with clipboard API support

**Test Steps:**
1. Generate configuration successfully
2. Click "Copy Config" button in success message
3. Verify clipboard copy operation succeeds
4. Paste clipboard content and verify format
5. Test copy functionality across different browsers

**Expected Results:**
- Copy operation completes immediately
- Clipboard contains properly formatted JSON configuration
- Success feedback provided to user
- JSON format is valid and readable
- Cross-browser compatibility maintained

**Clipboard Content Format:**
```json
{
  "jobName": "atoctran-200",
  "sourceSystem": "HR",
  "transactionType": "200",
  "fields": [
    {
      "fieldName": "record_type",
      "sourceField": "employee_type",
      "transformationType": "source"
    }
  ]
}
```

---

### TC-TEMP-064: Navigation to Manual Configuration
**Priority:** Medium  
**Module:** Additional Utilities  
**Test Objective:** Verify navigation to manual configuration functionality

**Prerequisites:**
- Configuration generated successfully
- Manual configuration page available

**Test Steps:**
1. Complete configuration generation
2. Click "Navigate to Manual Config" button
3. Verify navigation occurs to correct page
4. Check that source system and job name are passed correctly
5. Confirm manual configuration page loads with context

**Expected Results:**
- Navigation occurs immediately upon button click
- Target URL includes source system ID and job name
- Manual configuration page loads successfully
- Context information is passed correctly
- User can continue configuration in manual interface

**Navigation URL Format:**
```
/configuration/{sourceSystemId}/{jobName}
Example: /configuration/hr_system_001/atoctran-200
```

---

## 16. ERROR HANDLING

### TC-TEMP-065: Network Connectivity Error Handling
**Priority:** High  
**Module:** Error Handling  
**Test Objective:** Verify error handling when network connectivity is lost

**Prerequisites:**
- Template Configuration page loaded
- Ability to simulate network disconnection

**Test Steps:**
1. Disconnect network connection
2. Attempt to load file types
3. Try to select transaction types
4. Attempt configuration save operation
5. Verify error messages and retry mechanisms

**Expected Results:**
- Network errors are detected quickly (< 5 seconds)
- Clear error messages explain connectivity issues
- Retry mechanisms are provided where appropriate
- User interface remains functional despite errors
- Offline behavior is graceful

**Network Error Messages:**
```
"Failed to load template file types"
"Failed to load transaction types for p327" 
"Failed to generate configuration from template"
```

---

### TC-TEMP-066: Backend API Error Handling
**Priority:** High  
**Module:** Error Handling  
**Test Objective:** Verify handling of backend API errors

**Prerequisites:**
- Backend API accessible but may return error responses

**Test Steps:**
1. Trigger 404 error (non-existent file type)
2. Simulate 500 internal server error
3. Test 403 authorization error
4. Verify timeout handling for slow responses
5. Check error message content and user guidance

**Expected Results:**
- Different HTTP error codes handled appropriately
- Error messages are user-friendly and actionable
- Technical details are logged but not shown to user
- Retry options provided where appropriate
- User workflow can continue after error resolution

**API Error Handling:**
- 404: "Template not found" - check selection
- 500: "Server error occurred" - try again later
- 403: "Access denied" - check permissions
- Timeout: "Request timed out" - retry option provided

---

### TC-TEMP-067: Data Validation Error Handling
**Priority:** High  
**Module:** Error Handling  
**Test Objective:** Verify data validation error handling and user feedback

**Prerequisites:**
- Template configuration with various validation scenarios

**Test Steps:**
1. Enter invalid conditional expression syntax
2. Configure composite transformation with invalid delimiter
3. Set constant value incompatible with target data type
4. Test required field validation errors
5. Verify multiple error handling simultaneously

**Expected Results:**
- Validation errors are caught and displayed immediately
- Error messages are specific and helpful
- Multiple errors can be displayed without interface issues
- Users can correct errors and continue without page refresh
- Validation errors don't prevent other valid operations

**Validation Error Examples:**
```
Conditional: "Invalid condition syntax: missing closing parenthesis"
Data Type: "Constant value '2025-13-01' is not a valid date format"
Required: "Source field is required for transformation type 'source'"
Composite: "At least one source field is required for composite transformation"
```

---

### TC-TEMP-068: Recovery and Retry Mechanisms
**Priority:** Medium  
**Module:** Error Handling  
**Test Objective:** Verify error recovery and retry mechanisms

**Prerequisites:**
- Error scenarios that support retry operations

**Test Steps:**
1. Trigger temporary network error during file type loading
2. Use retry mechanism to reload
3. Test automatic retry for transient errors
4. Verify manual retry options for user-initiated operations
5. Check that retry attempts don't compound errors

**Expected Results:**
- Retry mechanisms are available for appropriate error types
- Automatic retries occur for transient network issues
- Manual retry buttons function correctly
- Retry attempts respect reasonable limits (3-5 attempts)
- Success after retry clears error states completely

**Retry Scenarios:**
- File type loading: Automatic retry after 2 seconds
- Configuration save: Manual retry button available
- Master query loading: Automatic retry with exponential backoff
- Template field loading: Manual retry with clear button

---

## 17. SUCCESS WORKFLOWS

### TC-TEMP-069: Complete End-to-End Success Workflow
**Priority:** Critical  
**Module:** Success Workflows  
**Test Objective:** Verify complete successful workflow from start to finish

**Prerequisites:**
- All systems operational
- Valid test data available

**Test Steps:**
1. Navigate to Template Configuration page
2. Complete Step 1: Select file type "atoctran", transaction type "200", source system "HR"
3. Complete Step 2: Select master query for employee data
4. Complete Step 3: Execute query and analyze columns
5. Skip Step 4: Smart mapping (future feature)
6. Complete Step 5: Configure field mappings with various transformation types
7. Complete Step 6: Generate and save configuration
8. Verify final success state

**Expected Results:**
- Each step completes successfully without errors
- Progress indicator shows current step accurately
- All entered data is preserved throughout workflow
- Final configuration generation succeeds
- Success message provides comprehensive completion summary

**Success Criteria:**
- No error messages displayed during workflow
- All steps advance properly with validation
- Configuration saves successfully to backend
- User receives clear completion confirmation
- Generated configuration is accessible

---

### TC-TEMP-070: Partial Configuration Success Handling
**Priority:** Medium  
**Module:** Success Workflows  
**Test Objective:** Verify handling when configuration generation succeeds but save fails

**Prerequisites:**
- Template configuration ready for generation
- Backend save operation may fail

**Test Steps:**
1. Complete all workflow steps successfully
2. Trigger configuration generation
3. Allow template generation to succeed but save to fail
4. Verify graceful handling of partial success
5. Check user guidance for completing save operation

**Expected Results:**
- Template generation success is acknowledged
- Save failure is clearly communicated
- User guidance provided for next steps
- Generated configuration is available for copy/export
- User can retry save operation or navigate to manual config

**Partial Success Message:**
```
"✅ Template configuration generated successfully!

Template: atoctran/200
Job Name: atoctran-200
Source System: HR
Fields prepared: 15

📋 Next Steps:
1. Click "Navigate to Manual Config" below to set up the job
2. The template structure is ready to be applied
3. You can copy the field mappings shown below"
```

---

### TC-TEMP-071: Configuration Import Success Workflow
**Priority:** Medium  
**Module:** Success Workflows  
**Test Objective:** Verify successful configuration import workflow

**Prerequisites:**
- Valid CSV configuration file prepared
- Template configuration page loaded

**Test Steps:**
1. Prepare valid CSV file with field mappings
2. Use import functionality to upload file
3. Verify successful processing and application
4. Check that imported configurations are correctly applied
5. Confirm success feedback and next steps

**Expected Results:**
- File import processes without errors
- Success message shows number of mappings applied
- Field mapping table updates with imported values
- Imported configurations are immediately usable
- User can proceed with workflow after import

**Import Success Message:**
```
"Successfully imported configuration from field_mappings.csv. 
12 field mappings were applied."
```

---

### TC-TEMP-072: Source System Creation Success Workflow
**Priority:** Medium  
**Module:** Success Workflows  
**Test Objective:** Verify successful source system creation workflow

**Prerequisites:**
- User has permissions to create source systems
- Valid source system data prepared

**Test Steps:**
1. Open "Add New Source System" dialog
2. Enter valid source system information
3. Submit creation request
4. Verify successful creation and immediate availability
5. Confirm success feedback and continued workflow

**Expected Results:**
- Source system creation completes successfully
- New source system immediately appears in dropdown
- Success message confirms creation
- User can immediately select and use new source system
- Source system list refreshes automatically

**Creation Success Message:**
```
"Successfully created source system: TESTBANK"
```

---

---

## Performance Test Cases

### TC-TEMP-073: Page Load Performance
**Priority:** Medium  
**Module:** Performance Testing  
**Test Objective:** Verify Template Configuration page loads within performance targets

**Prerequisites:**
- Performance monitoring tools available
- Baseline performance metrics established

**Test Steps:**
1. Navigate to Template Configuration page with cleared cache
2. Measure initial page load time
3. Test subsequent loads with browser cache
4. Monitor resource loading (CSS, JS, API calls)
5. Verify performance meets banking industry standards

**Expected Results:**
- Initial page load completes within 3 seconds
- Cached page loads complete within 1 second
- API calls complete within 2 seconds each
- Total page weight under 2MB
- Performance metrics meet enterprise requirements

**Performance Targets:**
- Time to First Contentful Paint: < 1.5 seconds
- Time to Interactive: < 3 seconds
- Total Blocking Time: < 300ms
- Cumulative Layout Shift: < 0.1

---

### TC-TEMP-074: Large Dataset Performance
**Priority:** Medium  
**Module:** Performance Testing  
**Test Objective:** Verify performance with large template configurations

**Prerequisites:**
- Template with 50+ fields available
- Large master query datasets

**Test Steps:**
1. Load template with 50+ fields
2. Configure mappings for all fields
3. Test scrolling and interaction performance
4. Measure configuration generation time
5. Verify UI responsiveness throughout

**Expected Results:**
- Large templates load within 5 seconds
- UI remains responsive during configuration
- Scrolling is smooth with 50+ field rows
- Configuration generation completes within 30 seconds
- Memory usage remains within acceptable limits

---

## Security Test Cases

### TC-TEMP-075: Input Sanitization
**Priority:** High  
**Module:** Security Testing  
**Test Objective:** Verify input sanitization prevents XSS and injection attacks

**Prerequisites:**
- Template configuration fields accessible for input
- Security testing tools available

**Test Steps:**
1. Enter potential XSS payloads in source field inputs
2. Test SQL injection patterns in field names
3. Try script injection in constant values
4. Test path traversal patterns in file names
5. Verify all inputs are properly sanitized

**Expected Results:**
- XSS payloads are sanitized and rendered as text
- SQL injection patterns are escaped properly
- Script injection attempts are blocked
- Path traversal patterns are neutralized
- No executable code is processed from user inputs

**Test Payloads:**
```
XSS: <script>alert('xss')</script>
SQL: '; DROP TABLE users; --
Script: javascript:alert('test')
Path: ../../../etc/passwd
```

---

### TC-TEMP-076: Authorization Validation
**Priority:** High  
**Module:** Security Testing  
**Test Objective:** Verify proper authorization controls

**Prerequisites:**
- Different user roles available for testing
- RBAC system configured

**Test Steps:**
1. Test access with user lacking template permissions
2. Verify source system creation permissions
3. Check configuration save authorization
4. Test API endpoint access controls
5. Validate role-based feature visibility

**Expected Results:**
- Unauthorized users cannot access template configuration
- Source system creation requires appropriate permissions
- Configuration save operations are properly authorized
- API endpoints enforce authentication requirements
- UI features are hidden based on user roles

---

## Regression Test Suite

### TC-TEMP-077: Core Functionality Regression
**Priority:** Critical  
**Module:** Regression Testing  
**Test Objective:** Verify core functionality remains intact after updates

**Prerequisites:**
- Previous version baseline available
- Full test environment setup

**Test Steps:**
1. Execute all critical path test cases
2. Verify file type and transaction type loading
3. Test template field configuration
4. Check configuration generation and save
5. Validate all transformation types function correctly

**Expected Results:**
- All critical functionality operates as expected
- No regression in core workflow performance
- Existing configurations remain compatible
- API integrations continue to function
- User interface behaves consistently

---

### TC-TEMP-078: Integration Point Regression
**Priority:** High  
**Module:** Regression Testing  
**Test Objective:** Verify integration points remain stable

**Prerequisites:**
- All integrated systems available
- Integration test data prepared

**Test Steps:**
1. Test master query integration functionality
2. Verify configuration context integration
3. Check navigation integration points
4. Test API service integration stability
5. Validate cross-component data flow

**Expected Results:**
- Master query integration works correctly
- Configuration context maintains state properly
- Navigation between pages functions smoothly
- API services respond correctly
- Data flows correctly between components

---

## Test Data Requirements

### Sample File Types
```
p327: "P327 Transaction Format"
- Transaction Types: 200, 300, 900
- Fields: 25+ with mixed data types

atoctran: "ATOC Transaction Format"  
- Transaction Types: 200, 300
- Fields: 15+ with transformations

default: "Default Template Format"
- Transaction Types: default
- Fields: 10+ basic fields
```

### Sample Source Systems
```
HR: Oracle Database
- Fields: employee_id, first_name, last_name, department, etc.
- Sample data with realistic values

SHAW: Oracle Database
- Fields: account_number, customer_id, transaction_amount, etc.
- Financial transaction sample data

ENCORE: Oracle Database
- Fields: loan_id, borrower_name, loan_amount, etc.
- Lending system sample data
```

### Sample Master Queries
```
EMPLOYEE_EXTRACT:
- Description: "Employee data extraction query"
- Columns: employee_id, name, department, salary
- Security: Internal Use
- Data Classification: Confidential

TRANSACTION_SUMMARY:
- Description: "Daily transaction summary"
- Columns: transaction_id, amount, date, type
- Security: Restricted
- Data Classification: Highly Confidential
```

## Test Environment Configuration

### Database Setup
- Oracle Database with CM3INT schema
- Template configuration tables populated
- Source system definitions loaded
- Master query definitions available
- Test user accounts with appropriate permissions

### API Configuration
- Backend API running on http://localhost:8080
- Template API endpoints functional
- Authentication system active
- CORS properly configured
- Request/response logging enabled

### Frontend Setup
- React application running on http://localhost:3000
- Proxy configuration for backend API
- All required dependencies installed
- Browser debugging tools available
- Performance monitoring enabled

## Test Execution Guidelines

### Test Environment
- Use dedicated test environment separate from production
- Ensure all dependent services are available
- Verify test data is properly seeded
- Configure appropriate logging levels
- Enable performance monitoring

### Test Data Management
- Use consistent test data across test runs
- Reset data between major test suites
- Maintain separate datasets for different scenarios
- Document test data dependencies
- Implement data cleanup procedures

### Defect Management
- Log all defects with clear reproduction steps
- Include screenshots and browser console logs
- Categorize defects by severity and priority
- Track defect resolution and retesting
- Maintain defect metrics and trends

### Test Reporting
- Generate comprehensive test execution reports
- Include pass/fail metrics by functionality area
- Document performance metrics and trends
- Provide detailed failure analysis
- Include recommendations for improvement

---

**Document Control:**
- **Version:** 1.0
- **Last Updated:** August 22, 2025
- **Created By:** Senior Full Stack Developer Agent
- **Reviewed By:** Principal Enterprise Architect (Pending)
- **Approved By:** Lending Product Owner (Pending)

**Change History:**
- v1.0: Initial comprehensive test cases document created with 78 detailed test cases covering all 17 functionality areas

**Test Case Summary:**
- **Total Test Cases:** 78
- **Critical Priority:** 23 test cases
- **High Priority:** 31 test cases  
- **Medium Priority:** 19 test cases
- **Low Priority:** 5 test cases

**Coverage Areas:**
1. Core Template Management (4 test cases)
2. Six-Step Workflow Process (4 test cases)
3. Source System Management (4 test cases)
4. Master Query Integration (4 test cases)
5. Field Mapping Configuration (6 test cases)
6. Advanced Conditional Logic Builder (6 test cases)
7. Import/Export Capabilities (5 test cases)
8. Data Validation & Testing (4 test cases)
9. UI/UX Features (5 test cases)
10. Configuration Generation (4 test cases)
11. Integration Features (4 test cases)
12. Smart Features (3 test cases)
13. Field Transformation Details (4 test cases)
14. Data Source Features (3 test cases)
15. Additional Utilities (4 test cases)
16. Error Handling (4 test cases)
17. Success Workflows (4 test cases)
18. Performance Testing (2 test cases)
19. Security Testing (2 test cases)
20. Regression Testing (2 test cases)

This comprehensive test document ensures thorough validation of all Template Configuration functionality while meeting banking industry standards for quality assurance and compliance.