# Updated Template Configuration Enhancement Plan

## 🎯 **USER REQUIREMENTS ANALYSIS**

Based on your clarifications and the screenshot at `http://localhost:3000/template-configuration`:

### **Key Requirements:**
1. **Template Configuration Page** is SEPARATE from Template Admin (different sidebar link)
2. **Keep Template Admin unchanged** - no modifications to existing template creation/editing
3. **Enhance Template Configuration page** to show all available source systems in dropdown
4. **Add "Add New Source System"** functionality for creating entirely new source systems
5. **Field-level mappings** between source and template for each source system
6. **Current 3-step process** should be enhanced, not replaced

## 🔄 **CURRENT VS ENHANCED WORKFLOW**

### **CURRENT Template Configuration Page:**
```
Step 1: Select Template
- File Type: [Dropdown] 
- Transaction Type: [Dropdown]
- Source System: "Shaw System (Selected from navigation)" [DISABLED]
- Job Name: [Auto-generated]

Step 2: Configure Field Mappings  
- Target structure from template
- Source Field mapping (user input)
- Transformation logic

Step 3: Generate & Save Configuration
- Calls backend API to create configuration
```

### **ENHANCED Template Configuration Page:**
```
Step 1: Select Template + Source System (ENHANCED)
- File Type: [Dropdown]
- Transaction Type: [Dropdown] 
- Source System: [Dropdown with all systems] [+ Add New System] ← NEW
- Job Name: [Auto-generated/editable]

Step 2: Configure Field Mappings (ENHANCED)
- Show source system context in field mapping
- Enhanced field-level mappings per source system
- Preview mapping results

Step 3: Generate & Save Configuration (ENHANCED)
- Save template-source associations
- Create batch configuration
- Update source system usage analytics
```

## 📋 **DETAILED IMPLEMENTATION PLAN**

### **PHASE 1: Source System Management (2-3 weeks)**

#### **1.1 Backend - Source System Management APIs**

**New Controller: SourceSystemController.java**
```java
@RestController
@RequestMapping("/api/admin/source-systems")
public class SourceSystemController {
    
    // Get all available source systems for dropdown
    @GetMapping
    public ResponseEntity<List<SourceSystemInfo>> getAllSourceSystems() {
        // Returns: [
        //   { "id": "hr", "name": "HR System", "description": "Human Resources" },
        //   { "id": "dda", "name": "DDA System", "description": "Deposit Account" },
        //   { "id": "shaw", "name": "Shaw System", "description": "Shaw Integration" }
        // ]
    }
    
    // Create new source system
    @PostMapping
    public ResponseEntity<SourceSystemInfo> createSourceSystem(@RequestBody CreateSourceSystemRequest request) {
        // Validates unique ID, creates new source system entry
        // Returns created source system info
    }
    
    // Get source systems with template usage info
    @GetMapping("/with-usage/{fileType}/{transactionType}")
    public ResponseEntity<List<SourceSystemWithUsage>> getSourceSystemsWithUsage(
        @PathVariable String fileType, @PathVariable String transactionType) {
        // Returns which systems have existing configurations for this template
    }
}
```

**New Data Models:**
```java
public class SourceSystemInfo {
    private String id;
    private String name;
    private String description;
    private String connectionType; // REST, DATABASE, FILE, etc.
    private LocalDateTime createdDate;
    private String status; // ACTIVE, INACTIVE
}

public class CreateSourceSystemRequest {
    private String id; // Unique identifier (e.g., "LOAN", "CRM")
    private String name; // Display name (e.g., "Loan Origination System")  
    private String description;
    private String connectionType;
}

public class SourceSystemWithUsage {
    private String id;
    private String name;
    private String description;
    private boolean hasExistingConfiguration;
    private String existingJobName;
    private LocalDateTime lastConfigured;
}
```

#### **1.2 Frontend - Enhanced Template Configuration Page**

**Updated TemplateConfigurationPage.tsx (Step 1 Enhancement):**
```tsx
// Enhanced Step 1: Template and Source System Selection
<Card sx={{ mb: 3 }}>
    <CardContent>
        <Typography variant="h6" gutterBottom>
            1. Select Template and Source System
        </Typography>
        
        <Grid container spacing={2}>
            {/* Existing File Type and Transaction Type dropdowns */}
            <Grid item xs={12} md={3}>
                <FormControl fullWidth>
                    <InputLabel>File Type</InputLabel>
                    <Select value={selectedFileType} onChange={handleFileTypeChange}>
                        {/* Existing options */}
                    </Select>
                </FormControl>
            </Grid>
            
            <Grid item xs={12} md={3}>
                <FormControl fullWidth>
                    <InputLabel>Transaction Type</InputLabel>
                    <Select value={selectedTransactionType} onChange={handleTransactionTypeChange}>
                        {/* Existing options */}
                    </Select>
                </FormControl>
            </Grid>
            
            {/* NEW: Source System Dropdown - replaces disabled field */}
            <Grid item xs={12} md={4}>
                <FormControl fullWidth>
                    <InputLabel>Source System</InputLabel>
                    <Select 
                        value={selectedSourceSystemId} 
                        onChange={handleSourceSystemChange}
                    >
                        {availableSourceSystems.map((system) => (
                            <MenuItem key={system.id} value={system.id}>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
                                    <span>{system.name}</span>
                                    {system.hasExistingConfiguration && (
                                        <Chip size="small" label="Configured" color="success" />
                                    )}
                                </Box>
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
            </Grid>
            
            {/* NEW: Add New Source System Button */}
            <Grid item xs={12} md={2}>
                <Button
                    variant="outlined"
                    startIcon={<Add />}
                    onClick={() => setShowAddSourceSystemModal(true)}
                    sx={{ height: '56px', width: '100%' }}
                >
                    Add New System
                </Button>
            </Grid>
        </Grid>
        
        {/* Existing Job Name field */}
        <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
                <TextField
                    fullWidth
                    label="Job Name"
                    value={templateJobName}
                    onChange={(e) => setTemplateJobName(e.target.value)}
                    helperText="Auto-generated from template and source (editable)"
                />
            </Grid>
        </Grid>
    </CardContent>
</Card>

{/* NEW: Add Source System Modal */}
<AddSourceSystemModal
    open={showAddSourceSystemModal}
    onClose={() => setShowAddSourceSystemModal(false)}
    onSourceSystemCreated={handleNewSourceSystemCreated}
/>
```

**New Component: AddSourceSystemModal.tsx**
```tsx
interface AddSourceSystemModalProps {
    open: boolean;
    onClose: () => void;
    onSourceSystemCreated: (newSystem: SourceSystemInfo) => void;
}

export const AddSourceSystemModal: React.FC<AddSourceSystemModalProps> = ({
    open, onClose, onSourceSystemCreated
}) => {
    const [formData, setFormData] = useState({
        id: '',
        name: '',
        description: '',
        connectionType: 'REST'
    });
    
    const handleSubmit = async () => {
        try {
            const response = await sourceSystemApi.createSourceSystem(formData);
            onSourceSystemCreated(response);
            onClose();
            // Show success message
        } catch (error) {
            // Handle validation errors (duplicate ID, etc.)
        }
    };
    
    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>Add New Source System</DialogTitle>
            <DialogContent>
                <Grid container spacing={2} sx={{ mt: 1 }}>
                    <Grid item xs={12} md={6}>
                        <TextField
                            fullWidth
                            label="System ID"
                            value={formData.id}
                            onChange={(e) => setFormData({...formData, id: e.target.value.toUpperCase()})}
                            helperText="Unique identifier (e.g., LOAN, CRM, BILLING)"
                            required
                        />
                    </Grid>
                    <Grid item xs={12} md={6}>
                        <TextField
                            fullWidth
                            label="System Name"
                            value={formData.name}
                            onChange={(e) => setFormData({...formData, name: e.target.value})}
                            helperText="Display name (e.g., Loan Origination System)"
                            required
                        />
                    </Grid>
                    <Grid item xs={12}>
                        <TextField
                            fullWidth
                            label="Description"
                            value={formData.description}
                            onChange={(e) => setFormData({...formData, description: e.target.value})}
                            multiline
                            rows={2}
                            helperText="Brief description of the source system"
                        />
                    </Grid>
                    <Grid item xs={12} md={6}>
                        <FormControl fullWidth>
                            <InputLabel>Connection Type</InputLabel>
                            <Select
                                value={formData.connectionType}
                                onChange={(e) => setFormData({...formData, connectionType: e.target.value})}
                            >
                                <MenuItem value="REST">REST API</MenuItem>
                                <MenuItem value="DATABASE">Database</MenuItem>
                                <MenuItem value="FILE">File System</MenuItem>
                                <MenuItem value="SFTP">SFTP</MenuItem>
                                <MenuItem value="BATCH">Batch Processing</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>
                </Grid>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancel</Button>
                <Button onClick={handleSubmit} variant="contained">
                    Create Source System
                </Button>
            </DialogActions>
        </Dialog>
    );
};
```

### **PHASE 2: Enhanced Field-Level Mapping (2-3 weeks)**

#### **2.1 Source System Context in Field Mapping**

**Enhanced Step 2: Configure Field Mappings**
```tsx
// Updated field mapping table to show source system context
<Card sx={{ mb: 3 }}>
    <CardContent>
        <Typography variant="h6" gutterBottom>
            2. Configure Field Mappings for {selectedSourceSystemName}
        </Typography>
        
        <Alert severity="info" sx={{ mb: 2 }}>
            Mapping {selectedFileType}/{selectedTransactionType} template fields to {selectedSourceSystemName} source fields
        </Alert>
        
        <Table size="small">
            <TableHead>
                <TableRow>
                    <TableCell>Position</TableCell>
                    <TableCell>Target Field</TableCell>
                    <TableCell>Length</TableCell>
                    <TableCell>Data Type</TableCell>
                    <TableCell>Source Field ({selectedSourceSystemName})</TableCell>
                    <TableCell>Transformation</TableCell>
                    <TableCell>Preview</TableCell>
                </TableRow>
            </TableHead>
            <TableBody>
                {templateFields.map((field, index) => (
                    <TableRow key={field.fieldName}>
                        {/* Existing cells for position, target field, length, data type */}
                        
                        {/* Enhanced Source Field mapping */}
                        <TableCell>
                            <TextField
                                size="small"
                                placeholder={`Source field from ${selectedSourceSystemName}`}
                                value={field.sourceField || ''}
                                onChange={(e) => handleSourceFieldChange(index, e.target.value)}
                                helperText={`Maps to ${field.fieldName}`}
                            />
                        </TableCell>
                        
                        {/* Enhanced Transformation options */}
                        <TableCell>
                            <Select 
                                size="small" 
                                value={field.transformationType || 'source'}
                                onChange={(e) => handleTransformationChange(index, e.target.value)}
                            >
                                <MenuItem value="source">Direct Mapping</MenuItem>
                                <MenuItem value="constant">Constant Value</MenuItem>
                                <MenuItem value="formula">Formula/Expression</MenuItem>
                                <MenuItem value="lookup">Lookup Table</MenuItem>
                                <MenuItem value="conditional">Conditional Logic</MenuItem>
                            </Select>
                        </TableCell>
                        
                        {/* NEW: Preview mapping result */}
                        <TableCell>
                            <Chip 
                                size="small" 
                                label={field.sourceField ? `${field.sourceField} → ${field.fieldName}` : 'Not mapped'}
                                color={field.sourceField ? 'success' : 'default'}
                            />
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    </CardContent>
</Card>
```

#### **2.2 Template-Source System Association Storage**

**New Backend API:**
```java
@RestController
@RequestMapping("/api/admin/template-source-mappings")
public class TemplateSourceMappingController {
    
    // Save template-source system association
    @PostMapping
    public ResponseEntity<String> saveTemplateSourceMapping(@RequestBody TemplateSourceMappingRequest request) {
        // Save field-level mappings between template and source system
        // Store in new FABRIC_TEMPLATE_SOURCE_MAPPINGS table
    }
    
    // Get existing mappings for template-source combination
    @GetMapping("/{fileType}/{transactionType}/{sourceSystemId}")
    public ResponseEntity<TemplateSourceMapping> getTemplateSourceMapping(
        @PathVariable String fileType,
        @PathVariable String transactionType, 
        @PathVariable String sourceSystemId) {
        // Return existing field mappings if available
    }
}
```

**New Database Table:**
```sql
CREATE TABLE FABRIC_TEMPLATE_SOURCE_MAPPINGS (
    ID NUMBER PRIMARY KEY,
    FILE_TYPE VARCHAR2(50) NOT NULL,
    TRANSACTION_TYPE VARCHAR2(50) NOT NULL,
    SOURCE_SYSTEM_ID VARCHAR2(50) NOT NULL,
    JOB_NAME VARCHAR2(100) NOT NULL,
    FIELD_MAPPINGS CLOB, -- JSON representation of field mappings
    CREATED_BY VARCHAR2(50),
    CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    STATUS VARCHAR2(20) DEFAULT 'ACTIVE',
    CONSTRAINT FK_TEMPLATE_MAPPING FOREIGN KEY (FILE_TYPE, TRANSACTION_TYPE) 
        REFERENCES FILE_TYPE_TEMPLATES (FILE_TYPE, TRANSACTION_TYPE)
);
```

### **PHASE 3: Integration & Testing (1-2 weeks)**

#### **3.1 Updated Generate & Save Configuration**

**Enhanced Step 3:**
```tsx
// Updated configuration generation to include source system association
const generateConfiguration = async () => {
    try {
        // Step 1: Create batch configuration (existing API)
        const configResponse = await templateApiService.createConfigurationFromTemplateWithMetadata(
            selectedFileType,
            selectedTransactionType,
            selectedSourceSystemId, // Now from dropdown instead of context
            templateJobName
        );
        
        // Step 2: Save template-source field mappings (NEW)
        const fieldMappings = templateFields.map(field => ({
            targetField: field.fieldName,
            sourceField: field.sourceField,
            transformationType: field.transformationType,  
            targetPosition: field.targetPosition,
            length: field.length,
            dataType: field.dataType
        }));
        
        await templateSourceMappingApi.saveTemplateSourceMapping({
            fileType: selectedFileType,
            transactionType: selectedTransactionType,
            sourceSystemId: selectedSourceSystemId,
            jobName: templateJobName,
            fieldMappings: fieldMappings
        });
        
        // Step 3: Show success with enhanced details
        setSuccess(`Configuration saved successfully!
        Template: ${selectedFileType}/${selectedTransactionType}
        Source System: ${selectedSourceSystemName}
        Job Name: ${templateJobName}
        Field Mappings: ${fieldMappings.length} configured`);
        
    } catch (error) {
        setError(`Configuration failed: ${error.message}`);
    }
};
```

## 🎯 **KEY CHANGES SUMMARY**

### **✅ What Gets Enhanced:**
1. **Template Configuration Page** - Enhanced dropdown for source system selection
2. **Add New Source System** - Modal for creating entirely new source systems
3. **Field-Level Mapping** - Enhanced with source system context and previews
4. **Template-Source Associations** - Persistent storage of field mappings

### **✅ What Stays Unchanged:**
1. **Template Admin Page** - No modifications to existing template creation/editing
2. **Existing APIs** - Current template management APIs remain untouched
3. **Current 3-Step Process** - Enhanced but maintains same workflow structure
4. **Database Schema** - Existing template tables unchanged, only additive new tables

## 📊 **IMPLEMENTATION TIMELINE**

| **Phase** | **Duration** | **Key Deliverables** |
|-----------|--------------|---------------------|
| **Phase 1** | 2-3 weeks | Source system management APIs, Enhanced Template Configuration page |
| **Phase 2** | 2-3 weeks | Field-level mapping enhancements, Template-source associations |
| **Phase 3** | 1-2 weeks | Integration testing, UI polish, End-to-end validation |
| **TOTAL** | **5-8 weeks** | **Complete enhanced Template Configuration page** |

## 💡 **NEXT STEPS**

1. **Confirm this updated plan** matches your requirements
2. **Begin Phase 1** with source system management backend APIs
3. **Create AddSourceSystemModal** component
4. **Enhance Template Configuration page** Step 1 with source system dropdown
5. **Test source system creation** end-to-end

This plan specifically addresses your requirements to keep Template Admin unchanged while enhancing the separate Template Configuration page with source system management and field-level mapping capabilities.