# Template Configuration Page Enhancement Plan

## 🎯 **CURRENT STATE ANALYSIS**

Based on the screenshot and code review of `http://localhost:3000/template-configuration`:

### **Current Template Configuration Page:**
- **Navigation**: Separate sidebar link (Template Configuration)
- **Workflow**: 3-step process (Select Template → Configure Mappings → Generate & Save)
- **Current Issue**: Depends on sidebar source system selection ("Shaw System" selected from navigation)
- **Field Mapping**: Focuses on source field mapping to template fields
- **Limitation**: No ability to add new source systems or see all available source systems

## 🔄 **PROPOSED ENHANCEMENTS**

### **1. Keep Template Admin Page Unchanged ✅**
- No changes to existing Template Admin functionality
- Preserve current template creation/editing workflow

### **2. Enhance Template Configuration Page**

#### **A. Source System Management Integration**
```
Current: "Source System: Shaw System (Selected from navigation)"
Enhanced: "Source System: [Dropdown with all systems] [+ Add New System]"
```

#### **B. Add New Source System Capability**
- Modal dialog for creating new source systems
- Fields: System ID, Name, Description, Connection details
- Integration with existing source system backend

#### **C. Enhanced Field-Level Mapping**
- Show all available source systems
- Configure field mappings per source system
- Save template-to-source associations

## 📋 **DETAILED IMPLEMENTATION PLAN**

### **PHASE 1: Source System Management (2-3 weeks)**

#### **1.1 Backend Enhancements**

**New API Endpoints:**
```java
@RestController
@RequestMapping("/admin/source-systems")
public class SourceSystemController {
    
    // Get all source systems with template association info
    @GetMapping
    public ResponseEntity<List<SourceSystemWithTemplateInfo>> getAllSourceSystems() {
        // Returns: HR System (3 templates), DDA System (1 template), etc.
    }
    
    // Create new source system
    @PostMapping
    public ResponseEntity<SourceSystem> createSourceSystem(@RequestBody CreateSourceSystemRequest request) {
        // Creates new source system entry
    }
    
    // Get source systems available for a specific template
    @GetMapping("/for-template/{fileType}/{transactionType}")
    public ResponseEntity<List<SourceSystemAvailability>> getSourceSystemsForTemplate(
        @PathVariable String fileType, @PathVariable String transactionType) {
        // Returns which systems can use this template + existing configurations
    }
}
```

**New Data Models:**
```java
public class SourceSystemWithTemplateInfo {
    private String id;
    private String name;
    private String description;
    private int templateCount;
    private List<String> associatedTemplates;
    private LocalDateTime lastUsed;
}

public class CreateSourceSystemRequest {
    private String id;
    private String name; 
    private String description;
    private String connectionType; // REST, Database, File, etc.
    private Map<String, Object> connectionProperties;
}

public class SourceSystemAvailability {
    private String id;
    private String name;
    private boolean hasExistingConfiguration;
    private String existingJobName;
    private LocalDateTime lastConfigured;
}
```

#### **1.2 Frontend Component Updates**

**Enhanced Template Configuration Page Structure:**
```tsx
// Updated TemplateConfigurationPage.tsx
export const TemplateConfigurationPage: React.FC = () => {
    // New state for source system management
    const [availableSourceSystems, setAvailableSourceSystems] = useState<SourceSystemAvailability[]>([]);
    const [selectedSourceSystemFromDropdown, setSelectedSourceSystemFromDropdown] = useState('');
    const [showAddSourceSystemModal, setShowAddSourceSystemModal] = useState(false);
    
    // Enhanced Step 1: Template + Source System Selection
    return (
        <Container maxWidth="xl" sx={{ py: 3 }}>
            {/* Step 1: Enhanced Template and Source System Selection */}
            <Card sx={{ mb: 3 }}>
                <CardContent>
                    <Typography variant="h6" gutterBottom>
                        1. Select Template and Source System
                    </Typography>
                    
                    <Grid container spacing={2}>
                        {/* Existing file type and transaction type dropdowns */}
                        <Grid item xs={12} md={3}>
                            <FormControl fullWidth>
                                <InputLabel>File Type</InputLabel>
                                <Select value={selectedFileType} onChange={handleFileTypeChange}>
                                    {/* Existing file type options */}
                                </Select>
                            </FormControl>
                        </Grid>
                        
                        <Grid item xs={12} md={3}>
                            <FormControl fullWidth>
                                <InputLabel>Transaction Type</InputLabel>
                                <Select value={selectedTransactionType} onChange={handleTransactionTypeChange}>
                                    {/* Existing transaction type options */}
                                </Select>
                            </FormControl>
                        </Grid>
                        
                        {/* NEW: Source System Dropdown */}
                        <Grid item xs={12} md={4}>
                            <FormControl fullWidth>
                                <InputLabel>Source System</InputLabel>
                                <Select 
                                    value={selectedSourceSystemFromDropdown} 
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
                        
                        {/* NEW: Add Source System Button */}
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
                    
                    {/* Existing job name field */}
                    <Grid container spacing={2} sx={{ mt: 1 }}>
                        <Grid item xs={12} md={6}>
                            <TextField
                                fullWidth
                                label="Job Name"
                                value={templateJobName}
                                onChange={(e) => setTemplateJobName(e.target.value)}
                                helperText="Auto-generated from template (editable)"
                            />
                        </Grid>
                    </Grid>
                </CardContent>
            </Card>
            
            {/* Add New Source System Modal */}
            <AddSourceSystemModal
                open={showAddSourceSystemModal}
                onClose={() => setShowAddSourceSystemModal(false)}
                onSourceSystemCreated={handleNewSourceSystemCreated}
            />
            
            {/* Existing Step 2: Field Mappings (Enhanced) */}
            {/* Existing Step 3: Generate & Save */}
        </Container>
    );
};
```

#### **1.3 Add Source System Modal Component**

**New Component: AddSourceSystemModal.tsx**
```tsx
interface AddSourceSystemModalProps {
    open: boolean;
    onClose: () => void;
    onSourceSystemCreated: (newSystem: SourceSystem) => void;
}

export const AddSourceSystemModal: React.FC<AddSourceSystemModalProps> = ({
    open, onClose, onSourceSystemCreated
}) => {
    const [formData, setFormData] = useState({
        id: '',
        name: '',
        description: '',
        connectionType: 'REST',
        connectionProperties: {}
    });
    
    const handleSubmit = async () => {
        try {
            // Validate system ID is unique
            const response = await sourceSystemApi.createSourceSystem(formData);
            onSourceSystemCreated(response);
            onClose();
            // Show success message
        } catch (error) {
            // Handle error (duplicate ID, validation, etc.)
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
                            helperText="Unique identifier (e.g., CRM, LOAN, etc.)"
                            required
                        />
                    </Grid>
                    <Grid item xs={12} md={6}>
                        <TextField
                            fullWidth
                            label="System Name"
                            value={formData.name}
                            onChange={(e) => setFormData({...formData, name: e.target.value})}
                            helperText="Display name (e.g., CRM System, Loan Origination)"
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

### **PHASE 2: Enhanced Field Mapping (2-3 weeks)**

#### **2.1 Multi-Source System Field Mapping**

**Enhanced Field Mapping Table:**
```tsx
// Enhanced field mapping to show source system context
const FieldMappingTable: React.FC = () => {
    return (
        <Table size="small">
            <TableHead>
                <TableRow>
                    <TableCell>Position</TableCell>
                    <TableCell>Target Field</TableCell>
                    <TableCell>Length</TableCell>
                    <TableCell>Data Type</TableCell>
                    <TableCell>Source Field ({selectedSourceSystem.name})</TableCell>
                    <TableCell>Transformation</TableCell>
                    <TableCell>Preview</TableCell>
                </TableRow>
            </TableHead>
            <TableBody>
                {templateFields.map((field, index) => (
                    <TableRow key={field.fieldName}>
                        {/* Existing cells */}
                        <TableCell>
                            <TextField
                                size="small"
                                placeholder={`Source field from ${selectedSourceSystem.name}`}
                                value={field.sourceField || ''}
                                onChange={(e) => handleSourceFieldChange(index, e.target.value)}
                            />
                        </TableCell>
                        <TableCell>
                            {/* Enhanced transformation options */}
                            <Select size="small" value={field.transformationType}>
                                <MenuItem value="source">Direct Mapping</MenuItem>
                                <MenuItem value="constant">Constant Value</MenuItem>
                                <MenuItem value="formula">Formula/Expression</MenuItem>
                                <MenuItem value="lookup">Lookup Table</MenuItem>
                                <MenuItem value="conditional">Conditional Logic</MenuItem>
                            </Select>
                        </TableCell>
                        <TableCell>
                            {/* Preview of mapping result */}
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
    );
};
```

#### **2.2 Template-Source System Association Storage**

**Backend Enhancement:**
```java
// New table for template-source system associations
@Entity
@Table(name = "FABRIC_TEMPLATE_SOURCE_MAPPINGS")
public class TemplateSourceMappingEntity {
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(name = "FILE_TYPE")
    private String fileType;
    
    @Column(name = "TRANSACTION_TYPE") 
    private String transactionType;
    
    @Column(name = "SOURCE_SYSTEM_ID")
    private String sourceSystemId;
    
    @Column(name = "JOB_NAME")
    private String jobName;
    
    @Column(name = "FIELD_MAPPINGS", columnDefinition = "CLOB")
    private String fieldMappingsJson; // JSON representation of field mappings
    
    @Column(name = "CREATED_BY")
    private String createdBy;
    
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;
    
    @Column(name = "STATUS")
    private String status; // ACTIVE, INACTIVE, DRAFT
}

// Service to manage template-source mappings
@Service
public class TemplateSourceMappingService {
    
    public void saveTemplateSourceMapping(String fileType, String transactionType, 
                                        String sourceSystemId, List<FieldMapping> fieldMappings) {
        // Save the complete mapping configuration
    }
    
    public List<TemplateSourceMapping> getTemplateSourceMappings(String fileType, String transactionType) {
        // Get all source systems configured for this template
    }
    
    public TemplateSourceMapping getTemplateSourceMapping(String fileType, String transactionType, 
                                                        String sourceSystemId) {
        // Get specific mapping for template-source combination
    }
}
```

### **PHASE 3: Integration & Testing (1-2 weeks)**

#### **3.1 Updated Workflow**

**New Enhanced Flow:**
```
1. User navigates to Template Configuration (separate sidebar link)
2. Step 1: Select Template + Source System
   - Choose File Type: CD01
   - Choose Transaction Type: DEFAULT  
   - Choose Source System: [HR System] [+ Add New System]
   - Job Name: Auto-generated/editable
3. Step 2: Configure Field Mappings (source system specific)
   - Map CD01 template fields to HR System source fields
   - Set transformation logic per field
   - Preview mapping results
4. Step 3: Generate & Save Configuration
   - Save to FABRIC_TEMPLATE_SOURCE_MAPPINGS table
   - Create batch configuration entry
   - Provide success feedback
```

#### **3.2 Source System Context Management**

**Integration Points:**
- Remove dependency on sidebar source system selection
- Source system selection moved to Template Configuration page
- Support for multiple source systems per template
- Add new source system capability integrated

## 🎯 **KEY FEATURES ADDRESSED**

### **✅ 1. Add New Source System**
- Modal dialog for creating new source systems
- Form validation and duplicate checking
- Integration with existing source system backend

### **✅ 2. Keep Template Creation Page Unchanged**
- No modifications to existing Template Admin functionality
- Preserves current template management workflow

### **✅ 3. Enhanced Template Configuration Page**
- Shows all available source systems in dropdown
- Associates templates to source systems
- Configures field-level mappings per source system

### **✅ 4. Field-Level Source-to-Template Mapping**
- Map source system fields to template fields
- Set transformation logic (direct, constant, formula, conditional)
- Preview mapping results before saving

## 📊 **IMPLEMENTATION TIMELINE**

| **Phase** | **Duration** | **Key Deliverables** |
|-----------|--------------|---------------------|
| **Phase 1** | 2-3 weeks | Source system management, Add new system modal |
| **Phase 2** | 2-3 weeks | Enhanced field mapping, Template-source associations |
| **Phase 3** | 1-2 weeks | Integration testing, UI polish, documentation |
| **TOTAL** | **5-8 weeks** | **Complete enhanced Template Configuration page** |

## 💡 **QUESTIONS FOR CLARIFICATION**

1. **Source System Properties**: What specific properties should be captured when creating a new source system? (Connection details, authentication, etc.)

2. **Field Mapping Complexity**: Do you need advanced transformation logic (formulas, lookups) or primarily direct field mapping?

3. **Template Reusability**: Should the same template be configurable for multiple source systems simultaneously, or one at a time?

4. **Validation Rules**: What validation should occur when mapping source fields to template fields? (Data type compatibility, required field checks, etc.)

5. **Historical Tracking**: Should we maintain history of template-source system associations and changes over time?

This approach keeps your existing Template Admin page unchanged while significantly enhancing the Template Configuration page with source system management and field-level mapping capabilities.