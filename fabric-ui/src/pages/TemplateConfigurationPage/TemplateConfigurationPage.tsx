// src/pages/TemplateConfigurationPage/TemplateConfigurationPage.tsx
/**
 * Enhanced Template Configuration Page with Master Query Integration
 * Phase 2 Frontend Implementation with Banking Domain Intelligence
 */
import React, { useState, useEffect } from 'react';
import {
    Box,
    Card,
    CardContent,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Button,
    Typography,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    TextField,
    Chip,
    Alert,
    CircularProgress,
    Container,
    Stepper,
    Step,
    StepLabel,
    Grid,
    Paper,
    Autocomplete,
    Accordion,
    AccordionSummary,
    AccordionDetails,
    IconButton,
    Tooltip,
    Divider,
    Tabs,
    Tab,
    Switch,
    FormControlLabel,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions
} from '@mui/material';
import { 
    Download, Upload, Save, Settings, OpenInNew, ExpandMore, Help, Preview, Code, Psychology,
    DataUsage, Assessment, Security, PlayArrow, TableChart, Storage, Add
} from '@mui/icons-material';
import { useConfigurationContext } from '../../contexts/ConfigurationContext';
import { MasterQueryProvider, useMasterQueryContext } from '../../contexts/MasterQueryContext';
import { templateApiService } from '../../services/api/templateApi';
import { FileTypeTemplate, FieldTemplate, FieldMappingConfig, TemplateToConfigurationResult } from '../../types/template';
import { SourceSystem } from '../../types/configuration';
import { MasterQueryRequest, MasterQuery, ColumnMetadata } from '../../types/masterQuery';
import { useNavigate } from 'react-router-dom';
import { useSourceSystems } from '../../hooks/useSourceSystems';
import { configApi } from '../../services/api/configApi';

// Import master query components
import MasterQuerySelector from '../../components/masterQuery/MasterQuerySelector';
import QueryPreviewComponent from '../../components/masterQuery/QueryPreviewComponent';
import QueryTesterComponent from '../../components/masterQuery/QueryTesterComponent';
import SmartFieldMapper from '../../components/masterQuery/SmartFieldMapper';
import QueryColumnAnalyzer from '../../components/masterQuery/QueryColumnAnalyzer';

const steps = [
    'Select Template', 
    'Master Query Selection', 
    'Query Analysis & Testing', 
    'Smart Field Mapping (Future)', 
    'Configure Mappings', 
    'Generate & Save'
];

// Internal component to access MasterQueryContext
const TemplateConfigurationPageContent: React.FC = () => {
    const [activeStep, setActiveStep] = useState(0);
    const [fileTypes, setFileTypes] = useState<FileTypeTemplate[]>([]);
    const [selectedFileType, setSelectedFileType] = useState('');
    const [transactionTypes, setTransactionTypes] = useState<string[]>([]);
    const [selectedTransactionType, setSelectedTransactionType] = useState('');
    const [templateFields, setTemplateFields] = useState<FieldTemplate[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [templateJobName, setTemplateJobName] = useState('');
    const [success, setSuccess] = useState<string | null>(null);
    const [generatedConfig, setGeneratedConfig] = useState<any>(null);
    const [localSelectedSourceSystem, setLocalSelectedSourceSystem] = useState<SourceSystem | null>(null);
    const [uploadedFileName, setUploadedFileName] = useState<string | null>(null);
    const [availableSourceFields, setAvailableSourceFields] = useState<Array<{ name: string; type: string; sample?: string }>>([]);
    const [conditionSuggestions, setConditionSuggestions] = useState<string[]>([]);
    
    // Master Query Integration State
    const [selectedMasterQuery, setSelectedMasterQuery] = useState<MasterQuery | null>(null);
    const [queryRequest, setQueryRequest] = useState<MasterQueryRequest | null>(null);
    const [extractedColumns, setExtractedColumns] = useState<ColumnMetadata[]>([]);
    const [smartMappingsEnabled, setSmartMappingsEnabled] = useState(false); // TODO: Future roadmap feature
    const [bankingIntelligenceEnabled, setBankingIntelligenceEnabled] = useState(false); // TODO: Future roadmap feature
    const [currentAnalysisTab, setCurrentAnalysisTab] = useState(0);
    
    // New Source System Creation State
    const [showNewSourceSystemDialog, setShowNewSourceSystemDialog] = useState(false);
    const [newSourceSystemData, setNewSourceSystemData] = useState({
        name: '',
        description: '',
        systemType: 'Oracle'
    });
    
    // Master Query Context
    const masterQueryContext = useMasterQueryContext();

    const navigate = useNavigate();
    
    const { sourceSystems, isLoading: sourceSystemsLoading, refreshData } = useSourceSystems();

    const {
        selectedSourceSystem,
        selectedJob,
        saveConfiguration,
        selectSourceSystem
    } = useConfigurationContext();

    // Initialize local source system with context value
    useEffect(() => {
        if (selectedSourceSystem && !localSelectedSourceSystem) {
            setLocalSelectedSourceSystem(selectedSourceSystem);
        }
    }, [selectedSourceSystem, localSelectedSourceSystem]);

    // Load file types on mount
    useEffect(() => {
        fetchFileTypes();
    }, []);

    // Load transaction types when file type changes
    useEffect(() => {
        if (selectedFileType) {
            fetchTransactionTypes(selectedFileType);
        }
    }, [selectedFileType]);

    // Load template fields when both are selected
    useEffect(() => {
        if (selectedFileType && selectedTransactionType) {
            fetchTemplateFields(selectedFileType, selectedTransactionType);
            fetchAvailableSourceFields();
            setActiveStep(1);
        }
    }, [selectedFileType, selectedTransactionType]);

    // Load available source fields when source system changes
    useEffect(() => {
        if (localSelectedSourceSystem) {
            fetchAvailableSourceFields();
        }
    }, [localSelectedSourceSystem]);

    // Auto-generate job name when template selection changes
    useEffect(() => {
        if (selectedFileType && selectedTransactionType) {
            const autoGeneratedJobName = `${selectedFileType}-${selectedTransactionType}`;
            setTemplateJobName(autoGeneratedJobName);
            console.log('Auto-generated job name:', autoGeneratedJobName);
        } else {
            setTemplateJobName('');
        }
    }, [selectedFileType, selectedTransactionType]);

    const fetchFileTypes = async () => {
        try {
            setLoading(true);
            const types = await templateApiService.getFileTypes();
            setFileTypes(
                types.map((t: any) => ({
                    ...t,
                    recordLength: t.recordLength ?? 0
                }))
            );
        } catch (error) {
            setError('Failed to load file types');
        } finally {
            setLoading(false);
        }
    };

    const fetchTransactionTypes = async (fileType: string) => {
        try {
            const data = await templateApiService.getTransactionTypes(fileType);
            setTransactionTypes(data);
            if (data.length === 1) {
                setSelectedTransactionType(data[0]);
            }
        } catch (error) {
            console.error('Error fetching transaction types:', error);
        }
    };

    const fetchTemplateFields = async (fileType: string, transactionType: string) => {
        setLoading(true);
        try {
            const data = await templateApiService.getTemplateFields(fileType, transactionType);
            setTemplateFields(data);
            setError(null);
        } catch (error) {
            setError('Failed to load template fields');
            console.error('Error fetching template fields:', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchAvailableSourceFields = async () => {
        if (!localSelectedSourceSystem) {
            setAvailableSourceFields([]);
            return;
        }

        try {
            // Mock source fields - in real implementation, this would fetch from the source system schema
            const mockSourceFields = [
                { name: 'employee_id', type: 'string', sample: '12345' },
                { name: 'first_name', type: 'string', sample: 'John' },
                { name: 'last_name', type: 'string', sample: 'Doe' },
                { name: 'department', type: 'string', sample: 'IT' },
                { name: 'status_code', type: 'string', sample: 'A' },
                { name: 'hire_date', type: 'date', sample: '2020-01-15' },
                { name: 'annual_salary', type: 'number', sample: '75000' },
                { name: 'performance_rating', type: 'number', sample: '4.2' },
                { name: 'manager_flag', type: 'boolean', sample: 'Y' },
                { name: 'years_experience', type: 'number', sample: '5' },
                { name: 'termination_date', type: 'date', sample: undefined },
                { name: 'dept_code', type: 'string', sample: 'IT' },
                { name: 'office_location', type: 'string', sample: 'New York' }
            ];
            
            setAvailableSourceFields(mockSourceFields);
            generateConditionSuggestions(mockSourceFields);
        } catch (error) {
            console.error('Error fetching source fields:', error);
        }
    };

    // Master Query Event Handlers
    const handleMasterQuerySelect = async (query: MasterQuery) => {
        try {
            setSelectedMasterQuery(query);
            
            // Create query request for testing and analysis
            const request: MasterQueryRequest = {
                masterQueryId: query.masterQueryId,
                queryName: query.queryName,
                querySql: query.querySql,
                queryDescription: query.queryDescription,
                securityClassification: query.securityClassification,
                dataClassification: query.dataClassification,
                businessJustification: query.businessJustification,
                complianceTags: query.complianceTags
            };
            
            setQueryRequest(request);
            
            // Advance to analysis step
            setActiveStep(2);
            
        } catch (error) {
            console.error('Failed to select master query:', error);
            setError('Failed to process selected master query');
        }
    };
    
    const handleQueryExecution = async () => {
        if (!queryRequest) return;
        
        try {
            // Execute query to get column metadata
            const metadata = await masterQueryContext.getColumnMetadata(queryRequest);
            setExtractedColumns(metadata);
            
            // Generate smart mappings if enabled
            if (smartMappingsEnabled) {
                await masterQueryContext.generateSmartMappings(metadata);
            }
            
            // Advance to smart mapping step
            setActiveStep(3);
            
        } catch (error) {
            console.error('Query execution failed:', error);
            setError('Failed to execute query and extract metadata');
        }
    };
    
    const handleSmartMappingsComplete = (mappings: any[]) => {
        // Convert smart mappings to template field mappings
        const templateMappings = mappings.map(mapping => ({
            sourceField: mapping.sourceColumn,
            targetField: mapping.targetField,
            confidence: mapping.confidence,
            businessConcept: mapping.businessConcept,
            dataClassification: mapping.dataClassification,
            complianceRequirements: mapping.complianceRequirements
        }));
        
        // Update available source fields with smart mapping data
        const enhancedSourceFields = extractedColumns.map(col => ({
            name: col.name,
            type: col.type.toLowerCase(),
            sample: `Sample ${col.name}`,
            businessConcept: col.businessConcept,
            dataClassification: col.dataClassification
        }));
        
        setAvailableSourceFields(enhancedSourceFields);
        
        // Advance to configure mappings step
        setActiveStep(4);
    };
    
    const canAdvanceFromStep = (step: number): boolean => {
        switch (step) {
            case 0: // Select Template
                return selectedFileType !== '' && selectedTransactionType !== '';
            case 1: // Master Query Selection  
                return selectedMasterQuery !== null;
            case 2: // Query Analysis & Testing
                return extractedColumns.length > 0;
            case 3: // Smart Field Mapping (Future) - Always allow advance
                return true; // TODO: Smart Field Mapping feature deferred to future roadmap
            case 4: // Configure Mappings
                return templateFields.length > 0;
            default:
                return true;
        }
    };
    
    const handleStepNavigation = (step: number) => {
        if (step > activeStep && !canAdvanceFromStep(activeStep)) {
            setError(`Please complete the current step before proceeding`);
            return;
        }
        
        setActiveStep(step);
        setError(null);
    };

    const generateConditionSuggestions = (sourceFields: Array<{ name: string; type: string; sample?: string }>) => {
        const suggestions: string[] = [];
        
        sourceFields.forEach(field => {
            switch (field.type) {
                case 'string':
                    suggestions.push(`${field.name} == "${field.sample || 'value'}"`);        
                    suggestions.push(`${field.name} != "${field.sample || 'value'}"`);                    
                    suggestions.push(`${field.name}.contains("substring")`);                    
                    suggestions.push(`${field.name} == null || ${field.name} == ""`);                                     
                    break;
                case 'number':
                    suggestions.push(`${field.name} >= ${field.sample || '0'}`);
                    suggestions.push(`${field.name} <= ${field.sample || '100'}`);
                    suggestions.push(`${field.name} > ${field.sample || '0'}`);
                    suggestions.push(`${field.name} < ${field.sample || '100'}`);
                    break;
                case 'date':
                    suggestions.push(`${field.name} >= "2020-01-01"`);
                    suggestions.push(`${field.name} <= "2024-12-31"`);
                    suggestions.push(`${field.name} == null`);
                    break;
                case 'boolean':
                    suggestions.push(`${field.name} == "Y"`);
                    suggestions.push(`${field.name} == "N"`);
                    suggestions.push(`${field.name} == true`);
                    suggestions.push(`${field.name} == false`);
                    break;
            }
        });
        
        // Add complex conditions
        if (sourceFields.length >= 2) {
            const field1 = sourceFields[0];
            const field2 = sourceFields[1];
            suggestions.push(`${field1.name} == "${field1.sample}" && ${field2.name} >= ${field2.sample || '0'}`);
            suggestions.push(`${field1.name} != null && ${field2.name} != null`);
        }
        
        setConditionSuggestions(suggestions.slice(0, 20)); // Limit to 20 suggestions
    };

    const testCondition = (conditionExpr: string, fieldIndex: number, condIndex: number) => {
        if (!conditionExpr || availableSourceFields.length === 0) {
            alert('Please enter a condition expression first');
            return;
        }

        try {
            // Create mock data using sample values from available source fields
            const mockData: Record<string, any> = {};
            availableSourceFields.forEach(field => {
                switch (field.type) {
                    case 'string':
                        mockData[field.name] = field.sample || 'sample_value';
                        break;
                    case 'number':
                        mockData[field.name] = parseFloat(field.sample || '0');
                        break;
                    case 'date':
                        mockData[field.name] = field.sample;
                        break;
                    case 'boolean':
                        mockData[field.name] = field.sample === 'Y' || field.sample === 'true';
                        break;
                    default:
                        mockData[field.name] = field.sample;
                }
            });

            // Simple condition evaluation (in production, use a proper expression evaluator)
            let result = false;
            try {
                // Replace field names with actual values for testing
                let testExpression = conditionExpr;
                Object.keys(mockData).forEach(fieldName => {
                    const value = mockData[fieldName];
                    const valueStr = typeof value === 'string' ? `"${value}"` : String(value);
                    testExpression = testExpression.replace(new RegExp(`\\b${fieldName}\\b`, 'g'), valueStr);
                });
                
                // Simple evaluation (note: in production, use a safe expression evaluator)
                result = eval(testExpression);
                
                const mockDataStr = Object.entries(mockData)
                    .map(([key, val]) => `${key}: ${val}`)
                    .join('\n');
                
                alert(`Condition Test Result:\n\nCondition: ${conditionExpr}\n\nMock Data:\n${mockDataStr}\n\nResult: ${result ? '✅ TRUE' : '❌ FALSE'}\n\nThis means the condition would ${result ? 'MATCH' : 'NOT MATCH'} with this sample data.`);
            } catch (evalError) {
                alert(`Condition syntax error: ${evalError}\n\nPlease check your condition syntax.`);
            }
        } catch (error) {
            console.error('Error testing condition:', error);
            alert('Error testing condition. Please check the syntax.');
        }
    };

    const handleSourceFieldChange = (fieldIndex: number, sourceField: string) => {
        const updated = [...templateFields];
        updated[fieldIndex].sourceField = sourceField;
        setTemplateFields(updated);
    };

    const handleTransformationChange = (fieldIndex: number, transformationType: string) => {
        const updated = [...templateFields];
        updated[fieldIndex].transformationType = transformationType as any;
        
        // Reset transformation-specific fields when type changes
        if (transformationType === 'source') {
            updated[fieldIndex].value = undefined;
            updated[fieldIndex].sources = undefined;
            updated[fieldIndex].conditions = [];
        } else if (transformationType === 'constant') {
            updated[fieldIndex].sources = undefined;
            updated[fieldIndex].conditions = [];
        } else if (transformationType === 'composite') {
            updated[fieldIndex].value = undefined;
            updated[fieldIndex].conditions = [];
            if (!updated[fieldIndex].sources) {
                updated[fieldIndex].sources = [{ field: '' }];
            }
        } else if (transformationType === 'conditional') {
            updated[fieldIndex].value = undefined;
            updated[fieldIndex].sources = undefined;
            if (!updated[fieldIndex].conditions || updated[fieldIndex].conditions?.length === 0) {
                updated[fieldIndex].conditions = [{ ifExpr: '', then: '', elseExpr: '' }];
            }
        }
        
        setTemplateFields(updated);
    };

    const handleSourceSystemChange = (sourceSystemId: string) => {
        const sourceSystem = sourceSystems.find(s => s.id === sourceSystemId);
        if (sourceSystem) {
            setLocalSelectedSourceSystem(sourceSystem);
            selectSourceSystem(sourceSystemId);
        }
    };

    const handleCreateNewSourceSystem = async () => {
        try {
            setLoading(true);
            setError(null);
            
            const newSourceSystem = await configApi.addSourceSystem({
                name: newSourceSystemData.name,
                description: newSourceSystemData.description,
                systemType: newSourceSystemData.systemType,
                inputBasePath: `/data/${newSourceSystemData.name.toLowerCase()}/input`,
                outputBasePath: `/data/${newSourceSystemData.name.toLowerCase()}/output`,
                jobs: [],
                supportedFileTypes: ['p327', 'atoctran', 'default'],
                supportedTransactionTypes: ['200', '300', '900', 'default']
            });
            
            setSuccess(`Successfully created source system: ${newSourceSystem.name}`);
            setShowNewSourceSystemDialog(false);
            setNewSourceSystemData({ name: '', description: '', systemType: 'Oracle' });
            
            // Refresh source systems list
            await refreshData();
            
        } catch (error) {
            console.error('Error creating source system:', error);
            const errorMessage = error instanceof Error ? error.message : 'Failed to create source system';
            setError(`Failed to create source system: ${errorMessage}`);
        } finally {
            setLoading(false);
        }
    };

    const generateConfiguration = async () => {
        // Validation checks
        if (!selectedFileType) {
            setError('Please select a file type');
            return;
        }

        if (!selectedTransactionType) {
            setError('Please select a transaction type');
            return;
        }

        if (!localSelectedSourceSystem) {
            setError('Please select a source system');
            return;
        }

        const jobName = templateJobName || (selectedJob?.name || selectedJob?.jobName);
        if (!jobName) {
            setError('Please ensure a job name is available');
            return;
        }

        try {
            setLoading(true);
            setError(null);

            console.log('🚀 Starting configuration generation...', {
                fileType: selectedFileType,
                transactionType: selectedTransactionType,
                sourceSystem: localSelectedSourceSystem.id,
                jobName: jobName
            });

            // Step 1: Generate configuration from template
            let configWithMetadata;

            try {
                configWithMetadata = await templateApiService.createConfigurationFromTemplateWithMetadata(
                    selectedFileType,
                    selectedTransactionType,
                    localSelectedSourceSystem.id,
                    jobName
                );

                console.log('✅ Template configuration generated:', configWithMetadata);
            } catch (templateError) {
                console.error('❌ Template generation failed:', templateError);
                const errorMsg = templateError instanceof Error ? templateError.message : 'Unknown error';
                throw new Error(`Failed to generate template configuration: ${errorMsg}`);
            }

            // Step 2: Merge user customizations from the UI
            const enhancedFields = configWithMetadata.fields.map((fieldMapping: any, index: number) => {
                const userCustomization = templateFields[index];

                return {
                    ...fieldMapping,
                    sourceField: userCustomization?.sourceField || fieldMapping.sourceField || '',
                    transformationType: userCustomization?.transformationType || fieldMapping.transformationType || 'source'
                };
            });

            const finalConfiguration = {
                ...configWithMetadata,
                fields: enhancedFields
            };

            console.log('✅ Final configuration prepared:', finalConfiguration);
            setGeneratedConfig(finalConfiguration);

            // Step 3: Simple save attempt
            try {
    console.log('🔄 Saving template configuration...', finalConfiguration);
    
    // Create configuration in the format expected by backend
    const configForBackend = {
        sourceSystem: finalConfiguration.sourceSystem,
        jobName: finalConfiguration.jobName,
        transactionType: finalConfiguration.transactionType, // "200"
        description: finalConfiguration.description || `Generated from ${selectedFileType}/${selectedTransactionType} template`,
        fieldMappings: finalConfiguration.fields.map((field: any) => ({
            fieldName: field.fieldName,
            sourceField: field.sourceField || '',
            targetField: field.targetField,
            targetPosition: field.targetPosition,
            length: field.length,
            dataType: field.dataType,
            transformationType: field.transformationType,
            transactionType: field.transactionType || finalConfiguration.transactionType,
            value: field.value,
            defaultValue: field.defaultValue,
            format: field.format
        })),
        createdBy: 'template-user',
        version: 1
    };

    console.log('📦 Sending to backend:', configForBackend);

    // Call the backend API directly instead of context save
    const response = await fetch('http://localhost:8080/ui/mappings/save', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(configForBackend)
    });

    if (response.ok) {
        const result = await response.text();
        console.log('✅ Configuration saved successfully! ID:', result);
        
        setActiveStep(steps.length - 1); // Move to final step after successful generation
        const { templateMetadata } = configWithMetadata;
        const successMessage = `✅ Configuration saved successfully!

Template: ${templateMetadata.fileType}/${templateMetadata.transactionType}
Job Name: ${jobName}
Transaction Type: ${finalConfiguration.transactionType}
Fields saved: ${finalConfiguration.fields.length}
Configuration ID: ${result}

🎉 Configuration saved to database!`;

        setSuccess(successMessage);
        setError(null);
    } else {
        throw new Error(`Backend save failed: ${response.status} ${response.statusText}`);
    }

            } catch (saveError) {
                console.warn('⚠️ Save operation failed:', saveError);
                
                // Show template generation success with next steps
                setActiveStep(steps.length - 1);
                const { templateMetadata } = configWithMetadata;
                const successMessage = `✅ Template configuration generated successfully!

Template: ${templateMetadata.fileType}/${templateMetadata.transactionType}
Job Name: ${jobName}
Source System: ${localSelectedSourceSystem.name}
Fields prepared: ${enhancedFields.length}

📋 Next Steps:
1. Click "Navigate to Manual Config" below to set up the job
2. The template structure is ready to be applied
3. You can copy the field mappings shown below

💡 Template generation was successful - just need to set up the job context for saving.`;

                setSuccess(successMessage);
                setError(null);
            }

        } catch (error) {
            console.error('❌ Configuration generation failed:', error);
            
            const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
            setError(`Failed to generate configuration: ${errorMessage}`);
            setActiveStep(4); // Return to configure mappings step on error
            
        } finally {
            setLoading(false);
            console.log('🏁 Configuration generation process completed');
        }
    };

    const navigateToManualConfig = () => {
        if (localSelectedSourceSystem && templateJobName) {
            navigate(`/configuration/${localSelectedSourceSystem.id}/${templateJobName}`);
        }
    };

    const copyConfigToClipboard = () => {
        if (generatedConfig) {
            const configText = JSON.stringify(generatedConfig, null, 2);
            navigator.clipboard.writeText(configText).then(() => {
                alert('Configuration copied to clipboard!');
            }).catch(() => {
                alert('Failed to copy to clipboard');
            });
        }
    };

    const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file) return;

        setLoading(true);
        setError(null);
        setUploadedFileName(file.name);

        try {
            // Validate file type
            const allowedTypes = ['.xlsx', '.xls', '.csv'];
            const fileExtension = file.name.toLowerCase().substring(file.name.lastIndexOf('.'));
            
            if (!allowedTypes.includes(fileExtension)) {
                throw new Error('Invalid file type. Please upload Excel (.xlsx, .xls) or CSV (.csv) files only.');
            }

            // Validate file size (max 5MB)
            if (file.size > 5 * 1024 * 1024) {
                throw new Error('File size too large. Please upload files smaller than 5MB.');
            }

            // Parse the file based on type
            let parsedData: any[] = [];
            
            if (fileExtension === '.csv') {
                parsedData = await parseCSVFile(file);
            } else {
                parsedData = await parseExcelFile(file);
            }

            // Validate parsed data
            if (!parsedData || parsedData.length === 0) {
                throw new Error('No data found in the uploaded file.');
            }

            // Map the parsed data to template fields
            const mappedFields = mapUploadedDataToFields(parsedData);
            
            if (mappedFields.length > 0) {
                // Update template fields with uploaded configuration
                const updatedFields = templateFields.map((field, index) => {
                    const mappedField = mappedFields.find(m => 
                        m.fieldName?.toLowerCase() === field.fieldName?.toLowerCase() ||
                        m.targetField?.toLowerCase() === field.fieldName?.toLowerCase()
                    );
                    
                    if (mappedField) {
                        return {
                            ...field,
                            sourceField: mappedField.sourceField || field.sourceField,
                            transformationType: mappedField.transformationType || field.transformationType,
                            value: mappedField.value || field.value,
                            defaultValue: mappedField.defaultValue || field.defaultValue,
                            delimiter: mappedField.delimiter || field.delimiter,
                            sources: mappedField.sources || field.sources,
                            conditions: mappedField.conditions || field.conditions
                        };
                    }
                    return field;
                });
                
                setTemplateFields(updatedFields);
                setSuccess(`Successfully imported configuration from ${file.name}. ${mappedFields.length} field mappings were applied.`);
            } else {
                setError('No matching fields found in the uploaded file. Please ensure the file contains columns like "fieldName", "sourceField", "transformationType", etc.');
            }

        } catch (error) {
            console.error('File upload error:', error);
            const errorMessage = error instanceof Error ? error.message : 'Failed to parse uploaded file';
            setError(`Upload failed: ${errorMessage}`);
            setUploadedFileName(null);
        } finally {
            setLoading(false);
            // Reset file input
            event.target.value = '';
        }
    };

    const parseCSVFile = (file: File): Promise<any[]> => {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (e) => {
                try {
                    const text = e.target?.result as string;
                    const lines = text.split('\n').filter(line => line.trim());
                    
                    if (lines.length < 2) {
                        reject(new Error('CSV file must contain at least a header row and one data row.'));
                        return;
                    }

                    const headers = lines[0].split(',').map(h => h.trim().replace(/"/g, ''));
                    const data = lines.slice(1).map(line => {
                        const values = line.split(',').map(v => v.trim().replace(/"/g, ''));
                        const row: any = {};
                        headers.forEach((header, index) => {
                            row[header] = values[index] || '';
                        });
                        return row;
                    });
                    
                    resolve(data);
                } catch (error) {
                    reject(new Error('Failed to parse CSV file. Please ensure it is properly formatted.'));
                }
            };
            reader.onerror = () => reject(new Error('Failed to read CSV file.'));
            reader.readAsText(file);
        });
    };

    const parseExcelFile = (file: File): Promise<any[]> => {
        return new Promise((resolve, reject) => {
            // For now, we'll show an informative error since we need a library like xlsx to parse Excel
            reject(new Error('Excel file parsing requires additional library. Please use CSV format for now, or implement xlsx library support.'));
        });
    };

    const mapUploadedDataToFields = (data: any[]): any[] => {
        const mappedFields: any[] = [];
        
        data.forEach(row => {
            // Common column name variations
            const fieldName = row.fieldName || row.field_name || row.FieldName || row['Field Name'] || '';
            const sourceField = row.sourceField || row.source_field || row.SourceField || row['Source Field'] || '';
            const transformationType = row.transformationType || row.transformation_type || row.TransformationType || row['Transformation Type'] || row.type || 'source';
            const value = row.value || row.constantValue || row.constant_value || row.Value || row['Constant Value'] || '';
            const defaultValue = row.defaultValue || row.default_value || row.DefaultValue || row['Default Value'] || '';
            
            // Enhanced: Handle transformation-specific fields
            const delimiter = row.delimiter || row.Delimiter || '';
            const conditionIf = row.condition_if || row.conditionIf || row.ConditionIf || row['Condition If'] || '';
            const conditionThen = row.condition_then || row.conditionThen || row.ConditionThen || row['Condition Then'] || '';
            const conditionElse = row.condition_else || row.conditionElse || row.ConditionElse || row['Condition Else'] || '';
            
            if (fieldName) {
                const mappedField: any = {
                    fieldName,
                    sourceField,
                    transformationType: transformationType.toLowerCase(),
                    value,
                    defaultValue
                };
                
                // Add transformation-specific configurations
                if (transformationType.toLowerCase() === 'composite') {
                    mappedField.delimiter = delimiter || ' ';
                    if (sourceField) {
                        // Parse comma-separated source fields for composite
                        mappedField.sources = sourceField.split(',').map((field: string) => ({ field: field.trim() }));
                    }
                }
                
                if (transformationType.toLowerCase() === 'conditional') {
                    if (conditionIf && conditionThen) {
                        mappedField.conditions = [{
                            ifExpr: conditionIf,
                            then: conditionThen,
                            elseExpr: conditionElse || ''
                        }];
                    }
                }
                
                mappedFields.push(mappedField);
            }
        });
        
        return mappedFields;
    };

    const exportCurrentTemplate = () => {
        if (!templateFields || templateFields.length === 0) {
            setError('No template fields available to export.');
            return;
        }

        const csvContent = generateCSVFromTemplate(templateFields);
        downloadCSV(csvContent, `template_${selectedFileType}_${selectedTransactionType}_${new Date().toISOString().split('T')[0]}.csv`);
    };

    const downloadSampleCSV = () => {
        const sampleData = [
            {
                fieldName: 'emp_id',
                sourceField: 'employee_id',
                transformationType: 'source',
                value: '',
                defaultValue: '',
                delimiter: '',
                condition_if: '',
                condition_then: '',
                condition_else: '',
                description: 'Direct mapping from source field'
            },
            {
                fieldName: 'location_code',
                sourceField: '',
                transformationType: 'constant',
                value: '100020',
                defaultValue: '100020',
                delimiter: '',
                condition_if: '',
                condition_then: '',
                condition_else: '',
                description: 'Fixed constant value'
            },
            {
                fieldName: 'full_name',
                sourceField: 'first_name,last_name',
                transformationType: 'composite',
                value: '',
                defaultValue: '',
                delimiter: ' ',
                condition_if: '',
                condition_then: '',
                condition_else: '',
                description: 'Combine first_name and last_name with space delimiter'
            },
            {
                fieldName: 'employee_status',
                sourceField: 'status_code',
                transformationType: 'conditional',
                value: '',
                defaultValue: 'UNKNOWN',
                delimiter: '',
                condition_if: 'status_code == "A"',
                condition_then: 'ACTIVE',
                condition_else: 'INACTIVE',
                description: 'Simple if-then-else: Active if status_code is A, otherwise Inactive'
            },
            {
                fieldName: 'department_name',
                sourceField: 'dept_id',
                transformationType: 'conditional',
                value: '',
                defaultValue: 'OTHER',
                delimiter: '',
                condition_if: 'dept_id == "HR" ? "Human Resources" : (dept_id == "IT" ? "Information Technology" : (dept_id == "FIN" ? "Finance" : "Other Department"))',
                condition_then: '',
                condition_else: '',
                description: 'Complex nested if-then-else: Map department codes to full names'
            },
            {
                fieldName: 'full_address',
                sourceField: 'street,city,state,zip',
                transformationType: 'composite',
                value: '',
                defaultValue: '',
                delimiter: ', ',
                condition_if: '',
                condition_then: '',
                condition_else: '',
                description: 'Combine address fields with comma-space delimiter'
            },
            {
                fieldName: 'salary_grade',
                sourceField: 'annual_salary',
                transformationType: 'conditional',
                value: '',
                defaultValue: 'ENTRY',
                delimiter: '',
                condition_if: 'annual_salary >= 100000 ? "SENIOR" : (annual_salary >= 75000 ? "MID" : (annual_salary >= 50000 ? "JUNIOR" : "ENTRY"))',
                condition_then: '',
                condition_else: '',
                description: 'Multi-level salary grading based on annual salary ranges'
            }
        ];
        
        const csvContent = generateCSVFromData(sampleData);
        downloadCSV(csvContent, 'sample_field_mapping_with_examples.csv');
    };

    const generateCSVFromTemplate = (fields: FieldTemplate[]): string => {
        const headers = ['fieldName', 'sourceField', 'transformationType', 'value', 'defaultValue', 'length', 'dataType', 'required'];
        const rows = fields.map(field => [
            field.fieldName || '',
            field.sourceField || '',
            field.transformationType || 'source',
            field.value || '',
            field.defaultValue || '',
            field.length || '',
            field.dataType || '',
            field.required || 'N'
        ]);
        
        return [headers, ...rows].map(row => 
            row.map(cell => `"${cell}"`).join(',')
        ).join('\n');
    };

    const generateCSVFromData = (data: any[]): string => {
        if (data.length === 0) return '';
        
        const headers = Object.keys(data[0]);
        const rows = data.map(item => 
            headers.map(header => `"${item[header] || ''}"`).join(',')
        );
        
        return [headers.map(h => `"${h}"`).join(','), ...rows].join('\n');
    };

    const downloadCSV = (content: string, filename: string) => {
        const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        
        if (link.download !== undefined) {
            const url = URL.createObjectURL(blob);
            link.setAttribute('href', url);
            link.setAttribute('download', filename);
            link.style.visibility = 'hidden';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }
    };

    return (
        <Container maxWidth="xl" sx={{ py: 3 }}>
            <Typography variant="h4" gutterBottom sx={{ color: 'primary.main', mb: 3 }}>
                Template-Based Configuration
            </Typography>

            {/* Stepper */}
            <Paper sx={{ p: 2, mb: 3 }}>
                <Stepper activeStep={activeStep} alternativeLabel>
                    {steps.map((label) => (
                        <Step key={label}>
                            <StepLabel>{label}</StepLabel>
                        </Step>
                    ))}
                </Stepper>
            </Paper>

            {/* Error Alert */}
            {error && (
                <Alert
                    severity="error"
                    sx={{ mb: 3 }}
                    onClose={() => setError(null)}
                    action={
                        <Button color="inherit" size="small" onClick={() => setError(null)}>
                            DISMISS
                        </Button>
                    }
                >
                    <div style={{ fontSize: '0.875rem' }}>
                        <strong>Error:</strong> {error}
                    </div>
                    <div style={{ fontSize: '0.75rem', marginTop: '8px', opacity: 0.8 }}>
                        Check the browser console for detailed error information.
                    </div>
                </Alert>
            )}

            {/* Success Alert */}
            {success && (
                <Alert
                    severity="success"
                    sx={{ mb: 3 }}
                    onClose={() => setSuccess(null)}
                    action={
                        <Box sx={{ display: 'flex', gap: 1 }}>
                            {generatedConfig && (
                                <>
                                    <Button 
                                        color="inherit" 
                                        size="small" 
                                        onClick={navigateToManualConfig}
                                        startIcon={<OpenInNew />}
                                    >
                                        Navigate to Manual Config
                                    </Button>
                                    <Button 
                                        color="inherit" 
                                        size="small" 
                                        onClick={copyConfigToClipboard}
                                    >
                                        Copy Config
                                    </Button>
                                </>
                            )}
                            <Button color="inherit" size="small" onClick={() => setSuccess(null)}>
                                DISMISS
                            </Button>
                        </Box>
                    }
                >
                    <div style={{ fontSize: '0.875rem', whiteSpace: 'pre-line' }}>
                        {success}
                    </div>
                </Alert>
            )}

            {/* Step 1: Template Selection */}
            <Card sx={{ mb: 3 }}>
                <CardContent>
                    <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Settings /> 1. Select Template
                    </Typography>

                    <Grid container spacing={2}>
                        <Grid item xs={12} md={3}>
                            <FormControl fullWidth>
                                <InputLabel>File Type</InputLabel>
                                <Select
                                    value={selectedFileType}
                                    onChange={(e) => setSelectedFileType(e.target.value)}
                                    label="File Type"
                                    disabled={loading}
                                >
                                    {fileTypes.map((ft) => (
                                        <MenuItem key={ft.fileType} value={ft.fileType}>
                                            {ft.fileType} - {ft.description}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Grid>

                        <Grid item xs={12} md={3}>
                            <FormControl fullWidth>
                                <InputLabel>Transaction Type</InputLabel>
                                <Select
                                    value={selectedTransactionType}
                                    onChange={(e) => setSelectedTransactionType(e.target.value)}
                                    label="Transaction Type"
                                    disabled={!selectedFileType || loading}
                                >
                                    {transactionTypes.map((tt) => (
                                        <MenuItem key={tt} value={tt}>
                                            {tt}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Grid>

                        <Grid item xs={12} md={3}>
                            <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-end' }}>
                                <FormControl fullWidth>
                                    <InputLabel>Source System</InputLabel>
                                    <Select
                                        value={localSelectedSourceSystem?.id || ''}
                                        onChange={(e) => {
                                            if (e.target.value === '__ADD_NEW__') {
                                                setShowNewSourceSystemDialog(true);
                                            } else {
                                                handleSourceSystemChange(e.target.value);
                                            }
                                        }}
                                        label="Source System"
                                        disabled={sourceSystemsLoading}
                                    >
                                        {sourceSystems.map((system) => (
                                            <MenuItem key={system.id} value={system.id}>
                                                {system.name} - {system.systemType}
                                            </MenuItem>
                                        ))}
                                        <Divider />
                                        <MenuItem value="__ADD_NEW__" sx={{ color: 'primary.main', fontWeight: 'bold' }}>
                                            <Add sx={{ mr: 1 }} />
                                            Add New Source System
                                        </MenuItem>
                                    </Select>
                                </FormControl>
                            </Box>
                        </Grid>

                        <Grid item xs={12} md={3}>
                            <TextField
                                fullWidth
                                size="small"
                                label="Job Name"
                                value={templateJobName}
                                onChange={(e) => setTemplateJobName(e.target.value)}
                                disabled={!selectedFileType || !selectedTransactionType}
                                helperText={
                                    selectedFileType && selectedTransactionType
                                        ? "Auto-generated from template (editable)"
                                        : "Select template to generate job name"
                                }
                            />
                        </Grid>
                    </Grid>

                    {selectedFileType && selectedTransactionType && templateJobName && (
                        <Box sx={{ mt: 2, p: 2, bgcolor: 'primary.main', color: 'primary.contrastText', borderRadius: 1 }}>
                            <Typography variant="body2">
                                ✓ Selected: {selectedFileType} - {selectedTransactionType} → Job: {templateJobName}
                            </Typography>
                        </Box>
                    )}
                </CardContent>
            </Card>

            {/* Step 2: Master Query Selection */}
            {selectedFileType && selectedTransactionType && activeStep >= 1 && (
                <Card sx={{ mb: 3 }}>
                    <CardContent>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                            <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <Storage /> 2. Select Master Query
                            </Typography>
                            <Box sx={{ display: 'flex', gap: 1 }}>
                                <FormControlLabel
                                    control={
                                        <Switch
                                            checked={smartMappingsEnabled}
                                            onChange={(e) => setSmartMappingsEnabled(e.target.checked)}
                                            size="small"
                                            disabled={true} // TODO: Future roadmap feature
                                        />
                                    }
                                    label="Smart Mappings (Future)"
                                />
                                <FormControlLabel
                                    control={
                                        <Switch
                                            checked={bankingIntelligenceEnabled}
                                            onChange={(e) => setBankingIntelligenceEnabled(e.target.checked)}
                                            size="small"
                                            disabled={true} // TODO: Future roadmap feature
                                        />
                                    }
                                    label="Banking Intelligence (Future)"
                                />
                            </Box>
                        </Box>

                        <Alert severity="info" sx={{ mb: 2 }}>
                            Select a master query to extract column metadata for manual field mapping configuration. 
                            Smart mapping and banking intelligence features are planned for future releases.
                        </Alert>

                        <MasterQuerySelector
                            onQuerySelect={handleMasterQuerySelect}
                            selectedQuery={selectedMasterQuery}
                            showBankingIntelligence={bankingIntelligenceEnabled}
                            maxHeight={400}
                        />

                        {selectedMasterQuery && (
                            <Box sx={{ mt: 2, p: 2, bgcolor: 'success.main', color: 'success.contrastText', borderRadius: 1 }}>
                                <Typography variant="body2">
                                    ✓ Selected Query: {selectedMasterQuery.queryName} - {selectedMasterQuery.queryDescription}
                                </Typography>
                            </Box>
                        )}

                        <Box sx={{ display: 'flex', gap: 2, mt: 2, justifyContent: 'flex-end' }}>
                            <Button
                                variant="outlined"
                                onClick={() => handleStepNavigation(activeStep - 1)}
                                disabled={activeStep === 0}
                            >
                                Previous
                            </Button>
                            <Button
                                variant="contained"
                                onClick={() => handleStepNavigation(activeStep + 1)}
                                disabled={!canAdvanceFromStep(activeStep)}
                            >
                                Next: Analyze Query
                            </Button>
                        </Box>
                    </CardContent>
                </Card>
            )}

            {/* Step 3: Query Analysis & Testing */}
            {selectedMasterQuery && activeStep >= 2 && (
                <Card sx={{ mb: 3 }}>
                    <CardContent>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                            <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <Assessment /> 3. Query Analysis & Testing
                            </Typography>
                            <Chip
                                label={extractedColumns.length > 0 ? `${extractedColumns.length} columns detected` : 'Not analyzed'}
                                color={extractedColumns.length > 0 ? 'success' : 'default'}
                                variant="outlined"
                            />
                        </Box>

                        <Tabs
                            value={currentAnalysisTab}
                            onChange={(_, newValue) => setCurrentAnalysisTab(newValue)}
                            sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}
                        >
                            <Tab
                                label={
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                        <Code />
                                        SQL Preview
                                    </Box>
                                }
                            />
                            <Tab
                                label={
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                        <PlayArrow />
                                        Query Tester
                                    </Box>
                                }
                            />
                            <Tab
                                label={
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                        <TableChart />
                                        Column Analyzer
                                    </Box>
                                }
                            />
                        </Tabs>

                        {currentAnalysisTab === 0 && queryRequest && (
                            <QueryPreviewComponent
                                query={queryRequest}
                                onQueryChange={setQueryRequest}
                                onExecute={handleQueryExecution}
                                showAnalysis={true}
                                maxHeight={500}
                            />
                        )}

                        {currentAnalysisTab === 1 && queryRequest && (
                            <QueryTesterComponent
                                query={queryRequest}
                                onExecutionComplete={(response) => {
                                    console.log('Query execution completed:', response);
                                    if (response.executionStatus === 'SUCCESS') {
                                        setActiveStep(3); // Advance to smart mapping
                                    }
                                }}
                                showHistory={true}
                                maxRows={100}
                            />
                        )}

                        {currentAnalysisTab === 2 && (
                            <QueryColumnAnalyzer
                                query={queryRequest || undefined}
                                columns={extractedColumns}
                                showDataProfiling={true}
                                showSecurityAnalysis={true}
                                autoAnalyze={true}
                            />
                        )}

                        <Box sx={{ display: 'flex', gap: 2, mt: 2, justifyContent: 'space-between' }}>
                            <Button
                                variant="outlined"
                                onClick={() => handleStepNavigation(activeStep - 1)}
                            >
                                Previous
                            </Button>
                            <Box sx={{ display: 'flex', gap: 2 }}>
                                <Button
                                    variant="outlined"
                                    startIcon={<PlayArrow />}
                                    onClick={handleQueryExecution}
                                    disabled={!queryRequest}
                                >
                                    Execute & Extract Metadata
                                </Button>
                                <Button
                                    variant="outlined"
                                    onClick={() => handleStepNavigation(4)} // Skip to Configure Mappings
                                    disabled={!canAdvanceFromStep(activeStep)}
                                    color="success"
                                >
                                    Skip to Manual Configuration
                                </Button>
                                <Button
                                    variant="contained"
                                    onClick={() => handleStepNavigation(activeStep + 1)}
                                    disabled={!canAdvanceFromStep(activeStep)}
                                >
                                    Next: Smart Mapping (Future)
                                </Button>
                            </Box>
                        </Box>
                    </CardContent>
                </Card>
            )}

            {/* Step 4: Smart Field Mapping (Future Roadmap) */}
            {extractedColumns.length > 0 && activeStep >= 3 && (
                <Card sx={{ mb: 3, bgcolor: 'grey.50', border: '2px dashed', borderColor: 'warning.main' }}>
                    <CardContent>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                            <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <Psychology sx={{ color: 'grey.500' }} /> 4. Smart Field Mapping (Future Feature)
                            </Typography>
                            <Chip
                                label="Roadmap Feature"
                                color="warning"
                                variant="outlined"
                            />
                        </Box>

                        <Alert severity="warning" sx={{ mb: 2 }}>
                            <strong>🚧 Future Roadmap Feature</strong>
                            <br />
                            Smart Field Mapping with Banking Intelligence is currently under development and will be available in a future release.
                            This feature will include:
                            <ul style={{ margin: '8px 0', paddingLeft: '20px' }}>
                                <li>Intelligent field pattern recognition</li>
                                <li>Banking compliance awareness</li>
                                <li>Automated mapping suggestions</li>
                                <li>PII and sensitive data detection</li>
                            </ul>
                            For now, please proceed to manual field mapping configuration.
                        </Alert>

                        {/* Placeholder for Future Smart Mapping Component */}
                        <Box sx={{ 
                            p: 4, 
                            textAlign: 'center', 
                            bgcolor: 'grey.100', 
                            borderRadius: 2,
                            border: '1px dashed',
                            borderColor: 'grey.400'
                        }}>
                            <Psychology sx={{ fontSize: 48, color: 'grey.400', mb: 2 }} />
                            <Typography variant="h6" color="text.secondary" gutterBottom>
                                Smart Field Mapping Coming Soon
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                This area will contain intelligent field mapping tools with banking domain expertise.
                                <br />
                                Currently you can proceed to manual configuration below.
                            </Typography>
                        </Box>

                        <Box sx={{ display: 'flex', gap: 2, mt: 3, justifyContent: 'space-between' }}>
                            <Button
                                variant="outlined"
                                onClick={() => handleStepNavigation(activeStep - 1)}
                            >
                                Previous
                            </Button>
                            <Box sx={{ display: 'flex', gap: 2 }}>
                                <Button
                                    variant="outlined"
                                    onClick={() => handleStepNavigation(activeStep + 1)}
                                    color="warning"
                                >
                                    Skip Smart Mapping
                                </Button>
                                <Button
                                    variant="contained"
                                    onClick={() => handleStepNavigation(activeStep + 1)}
                                >
                                    Continue to Manual Configuration
                                </Button>
                            </Box>
                        </Box>
                    </CardContent>
                </Card>
            )}

            {/* Step 5: Configure Field Mappings */}
            {selectedFileType && selectedTransactionType && activeStep >= 4 && (
                <Card sx={{ mb: 3 }}>
                    <CardContent>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                            <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <DataUsage /> 5. Configure Field Mappings
                            </Typography>
                            <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                                <Chip
                                    label={`${templateFields.length} fields`}
                                    color="primary"
                                    variant="outlined"
                                />
                                <Chip
                                    label="Manual Configuration"
                                    color="info"
                                    variant="filled"
                                    size="small"
                                />
                                {templateFields.some(f => f.sourceField || f.value || f.transformationType !== 'source') && (
                                    <Chip
                                        label="Has Mappings"
                                        color="success"
                                        variant="outlined"
                                        size="small"
                                    />
                                )}
                            </Box>
                        </Box>

                        <Alert severity="info" sx={{ mb: 2 }}>
                            <strong>Manual Field Mapping Configuration</strong>
                            <br />
                            Target structure is pre-configured from template. Specify source fields and transformation logic for each target field.
                            This is the current working method for field mapping configuration.
                        </Alert>
                        
                        {/* Save Configuration Button for Manual Mappings */}
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                            <Typography variant="body2" color="text.secondary">
                                Configure your field mappings below, then save the configuration when ready.
                            </Typography>
                            <Button
                                variant="contained"
                                color="primary"
                                size="medium"
                                startIcon={loading ? <CircularProgress size={16} /> : <Save />}
                                onClick={generateConfiguration}
                                disabled={loading || !localSelectedSourceSystem || !templateJobName || templateFields.length === 0}
                                sx={{ minWidth: '180px' }}
                            >
                                {loading ? 'Saving...' : 'Save Configuration'}
                            </Button>
                        </Box>
                        
                        {uploadedFileName && (
                            <Alert severity="success" sx={{ mb: 2 }}>
                                📁 Configuration imported from: <strong>{uploadedFileName}</strong>
                            </Alert>
                        )}
                        
                        <Alert severity="info" sx={{ mb: 2 }}>
                            <div style={{ fontSize: '0.875rem' }}>
                                💡 <strong>Pro Tip:</strong> You can import field mappings from CSV files. 
                                <strong>Click "Download Sample CSV"</strong> to see examples including:
                                <br />
                                • <strong>Composite:</strong> Combine multiple fields (e.g., first_name + last_name)
                                <br />
                                • <strong>Simple Conditional:</strong> if status_code == "A" then "ACTIVE" else "INACTIVE"
                                <br />
                                • <strong>Complex Conditional:</strong> Multi-level if-then-else chains for mappings
                                <br />
                                • <strong>Constant:</strong> Fixed values for all records
                            </div>
                        </Alert>

                        {loading ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                                <CircularProgress />
                            </Box>
                        ) : (
                            <Box sx={{ overflowX: 'auto' }}>
                                <Table size="small">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Position</TableCell>
                                            <TableCell>Target Field</TableCell>
                                            <TableCell>Length</TableCell>
                                            <TableCell>Data Type</TableCell>
                                            <TableCell>Format</TableCell>
                                            <TableCell>Source Field</TableCell>
                                            <TableCell>Transformation</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {templateFields.map((field, index) => (
                                            <TableRow key={field.fieldName} hover>
                                                <TableCell align="center">
                                                    <Chip size="small" label={field.targetPosition} />
                                                </TableCell>
                                                <TableCell>
                                                    <Typography variant="body2" fontWeight="bold">
                                                        {field.fieldName}
                                                    </Typography>
                                                    {field.required === 'Y' && (
                                                        <Chip size="small" label="Required" color="error" sx={{ ml: 1 }} />
                                                    )}
                                                </TableCell>
                                                <TableCell>{field.length}</TableCell>
                                                <TableCell>
                                                    <Chip size="small" label={field.dataType} variant="outlined" />
                                                </TableCell>
                                                <TableCell>{field.format || '-'}</TableCell>
                                                <TableCell>
                                                    <TextField
                                                        size="small"
                                                        placeholder="Source field name"
                                                        value={field.sourceField || ''}
                                                        onChange={(e) => handleSourceFieldChange(index, e.target.value)}
                                                        sx={{ width: 150 }}
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, minWidth: 200 }}>
                                                        <Select
                                                            size="small"
                                                            value={field.transformationType || 'source'}
                                                            onChange={(e) => handleTransformationChange(index, e.target.value)}
                                                            sx={{ width: 120 }}
                                                        >
                                                            <MenuItem value="source">Source</MenuItem>
                                                            <MenuItem value="constant">Constant</MenuItem>
                                                            <MenuItem value="composite">Composite</MenuItem>
                                                            <MenuItem value="conditional">Conditional</MenuItem>
                                                        </Select>
                                                        
                                                        {/* Constant Value Configuration */}
                                                        {field.transformationType === 'constant' && (
                                                            <TextField
                                                                size="small"
                                                                placeholder="Constant value"
                                                                value={field.value || field.defaultValue || ''}
                                                                onChange={(e) => {
                                                                    const updated = [...templateFields];
                                                                    updated[index].value = e.target.value;
                                                                    setTemplateFields(updated);
                                                                }}
                                                                sx={{ width: 120 }}
                                                                label="Value"
                                                            />
                                                        )}
                                                        
                                                        {/* Composite Configuration */}
                                                        {field.transformationType === 'composite' && (
                                                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                                                                <Typography variant="caption" color="text.secondary">
                                                                    Source Fields:
                                                                </Typography>
                                                                {(field.sources || [{ field: '' }]).map((source: { field: string }, sourceIndex: number) => (
                                                                    <Box key={sourceIndex} sx={{ display: 'flex', gap: 0.5, alignItems: 'center' }}>
                                                                        <TextField
                                                                            size="small"
                                                                            placeholder={`Field ${sourceIndex + 1}`}
                                                                            value={source.field || ''}
                                                                            onChange={(e) => {
                                                                                const updated = [...templateFields];
                                                                                if (!updated[index].sources) updated[index].sources = [];
                                                                                updated[index].sources![sourceIndex] = { field: e.target.value };
                                                                                setTemplateFields(updated);
                                                                            }}
                                                                            sx={{ width: 100 }}
                                                                        />
                                                                        <Button
                                                                            size="small"
                                                                            variant="outlined"
                                                                            onClick={() => {
                                                                                const updated = [...templateFields];
                                                                                if (!updated[index].sources) updated[index].sources = [];
                                                                                updated[index].sources!.push({ field: '' });
                                                                                setTemplateFields(updated);
                                                                            }}
                                                                            sx={{ minWidth: 'auto', px: 1 }}
                                                                        >
                                                                            +
                                                                        </Button>
                                                                        {field.sources && field.sources.length > 1 && (
                                                                            <Button
                                                                                size="small"
                                                                                variant="outlined"
                                                                                color="error"
                                                                                onClick={() => {
                                                                                    const updated = [...templateFields];
                                                                                    if (updated[index].sources) {
                                                                                        updated[index].sources!.splice(sourceIndex, 1);
                                                                                    }
                                                                                    setTemplateFields(updated);
                                                                                }}
                                                                                sx={{ minWidth: 'auto', px: 1 }}
                                                                            >
                                                                                -
                                                                            </Button>
                                                                        )}
                                                                    </Box>
                                                                ))}
                                                                <TextField
                                                                    size="small"
                                                                    placeholder="Delimiter (e.g., ' ', '_', ',')"
                                                                    value={field.delimiter || ''}
                                                                    onChange={(e) => {
                                                                        const updated = [...templateFields];
                                                                        updated[index].delimiter = e.target.value;
                                                                        setTemplateFields(updated);
                                                                    }}
                                                                    sx={{ width: 120 }}
                                                                    label="Delimiter"
                                                                />
                                                            </Box>
                                                        )}
                                                        
                                                        {/* Enhanced Conditional Configuration */}
                                                        {field.transformationType === 'conditional' && (
                                                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                                                                <Accordion defaultExpanded sx={{ boxShadow: 1 }}>
                                                                    <AccordionSummary expandIcon={<ExpandMore />} sx={{ bgcolor: 'primary.main', color: 'white' }}>
                                                                        <Typography variant="subtitle2">
                                                                            🧠 Smart Conditional Logic Builder
                                                                        </Typography>
                                                                    </AccordionSummary>
                                                                    <AccordionDetails sx={{ bgcolor: 'grey.50' }}>
                                                                        {/* Available Source Fields */}
                                                                        <Box sx={{ mb: 2 }}>
                                                                            <Typography variant="caption" color="text.secondary" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                                                                <Code fontSize="small" /> Available Source Fields:
                                                                            </Typography>
                                                                            <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 0.5 }}>
                                                                                {availableSourceFields.map((sourceField, sfIndex) => (
                                                                                    <Tooltip key={sfIndex} title={`Type: ${sourceField.type}, Sample: ${sourceField.sample || 'N/A'}`}>
                                                                                        <Chip 
                                                                                            size="small" 
                                                                                            label={sourceField.name}
                                                                                            variant="outlined"
                                                                                            color={sourceField.type === 'string' ? 'primary' : sourceField.type === 'number' ? 'secondary' : 'default'}
                                                                                            onClick={() => {
                                                                                                // Add field to condition input
                                                                                                const updated = [...templateFields];
                                                                                                if (!updated[index].conditions) updated[index].conditions = [{ ifExpr: '', then: '', elseExpr: '' }];
                                                                                                const currentCondition = updated[index].conditions![0].ifExpr || '';
                                                                                                updated[index].conditions![0].ifExpr = currentCondition + (currentCondition ? ' && ' : '') + sourceField.name;
                                                                                                setTemplateFields(updated);
                                                                                            }}
                                                                                        />
                                                                                    </Tooltip>
                                                                                ))}
                                                                            </Box>
                                                                        </Box>
                                                                        
                                                                        {/* Condition Suggestions */}
                                                                        <Box sx={{ mb: 2 }}>
                                                                            <Typography variant="caption" color="text.secondary" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                                                                <Psychology fontSize="small" /> Smart Suggestions:
                                                                            </Typography>
                                                                            <Autocomplete
                                                                                size="small"
                                                                                options={conditionSuggestions}
                                                                                freeSolo
                                                                                renderInput={(params) => (
                                                                                    <TextField 
                                                                                        {...params} 
                                                                                        label="Quick condition templates"
                                                                                        placeholder="Type or select a condition template"
                                                                                    />
                                                                                )}
                                                                                onChange={(event, value) => {
                                                                                    if (value) {
                                                                                        const updated = [...templateFields];
                                                                                        if (!updated[index].conditions) updated[index].conditions = [{ ifExpr: '', then: '', elseExpr: '' }];
                                                                                        updated[index].conditions![0].ifExpr = value;
                                                                                        setTemplateFields(updated);
                                                                                    }
                                                                                }}
                                                                                sx={{ mt: 0.5 }}
                                                                            />
                                                                        </Box>
                                                                        
                                                                        <Divider sx={{ my: 1 }} />
                                                                        
                                                                        {/* Condition Builder */}
                                                                        <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                                                                            Build Your Conditions:
                                                                        </Typography>
                                                                        {(field.conditions || [{ ifExpr: '', then: '', elseExpr: '' }]).map((condition: { ifExpr: string; then: string; elseExpr?: string }, condIndex: number) => (
                                                                            <Box key={condIndex} sx={{ display: 'flex', flexDirection: 'column', gap: 1, p: 2, border: '2px solid #e0e0e0', borderRadius: 2, bgcolor: 'white', mb: 1 }}>
                                                                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                                                    <Typography variant="body2" fontWeight="bold" color="primary.main">
                                                                                        Condition #{condIndex + 1}
                                                                                    </Typography>
                                                                                    <Tooltip title="Test this condition with sample data">
                                                                                        <IconButton size="small" onClick={() => testCondition(condition.ifExpr, index, condIndex)}>
                                                                                            <Preview fontSize="small" />
                                                                                        </IconButton>
                                                                                    </Tooltip>
                                                                                </Box>
                                                                                
                                                                                <TextField
                                                                                    size="small"
                                                                                    placeholder="If condition (e.g., status_code == 'A' && department == 'IT')"
                                                                                    value={condition.ifExpr || ''}
                                                                                    onChange={(e) => {
                                                                                        const updated = [...templateFields];
                                                                                        if (!updated[index].conditions) updated[index].conditions = [];
                                                                                        updated[index].conditions![condIndex] = {
                                                                                            ...updated[index].conditions![condIndex],
                                                                                            ifExpr: e.target.value
                                                                                        };
                                                                                        setTemplateFields(updated);
                                                                                    }}
                                                                                    label="If Expression"
                                                                                    multiline
                                                                                    rows={2}
                                                                                    fullWidth
                                                                                    helperText="Use source field names in your condition (e.g., field_name == 'value')"
                                                                                />
                                                                                
                                                                                <Box sx={{ display: 'flex', gap: 1 }}>
                                                                                    <TextField
                                                                                        size="small"
                                                                                        placeholder="Value if TRUE"
                                                                                        value={condition.then || ''}
                                                                                        onChange={(e) => {
                                                                                            const updated = [...templateFields];
                                                                                            if (!updated[index].conditions) updated[index].conditions = [];
                                                                                            updated[index].conditions![condIndex] = {
                                                                                                ...updated[index].conditions![condIndex],
                                                                                                then: e.target.value
                                                                                            };
                                                                                            setTemplateFields(updated);
                                                                                        }}
                                                                                        label="Then (True Result)"
                                                                                        sx={{ flex: 1 }}
                                                                                        helperText="Output when condition is TRUE"
                                                                                    />
                                                                                    
                                                                                    <TextField
                                                                                        size="small"
                                                                                        placeholder="Value if FALSE (optional)"
                                                                                        value={condition.elseExpr || ''}
                                                                                        onChange={(e) => {
                                                                                            const updated = [...templateFields];
                                                                                            if (!updated[index].conditions) updated[index].conditions = [];
                                                                                            updated[index].conditions![condIndex] = {
                                                                                                ...updated[index].conditions![condIndex],
                                                                                                elseExpr: e.target.value
                                                                                            };
                                                                                            setTemplateFields(updated);
                                                                                        }}
                                                                                        label="Else (False Result)"
                                                                                        sx={{ flex: 1 }}
                                                                                        helperText="Output when condition is FALSE"
                                                                                    />
                                                                                </Box>
                                                                                
                                                                                {/* Condition Preview */}
                                                                                {condition.ifExpr && (
                                                                                    <Box sx={{ p: 1, bgcolor: 'info.main', color: 'white', borderRadius: 1, fontSize: '0.75rem' }}>
                                                                                        <Typography variant="caption">
                                                                                            🔍 Preview: IF ({condition.ifExpr}) THEN "{condition.then || 'not set'}" ELSE "{condition.elseExpr || field.defaultValue || 'not set'}"
                                                                                        </Typography>
                                                                                    </Box>
                                                                                )}
                                                                                
                                                                                {field.conditions && field.conditions.length > 1 && (
                                                                                    <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                                                                                        <Button
                                                                                            size="small"
                                                                                            variant="outlined"
                                                                                            color="error"
                                                                                            onClick={() => {
                                                                                                const updated = [...templateFields];
                                                                                                if (updated[index].conditions) {
                                                                                                    updated[index].conditions!.splice(condIndex, 1);
                                                                                                }
                                                                                                setTemplateFields(updated);
                                                                                            }}
                                                                                        >
                                                                                            Remove Condition
                                                                                        </Button>
                                                                                    </Box>
                                                                                )}
                                                                            </Box>
                                                                        ))}
                                                                        
                                                                        <Button
                                                                            size="small"
                                                                            variant="outlined"
                                                                            onClick={() => {
                                                                                const updated = [...templateFields];
                                                                                if (!updated[index].conditions) updated[index].conditions = [];
                                                                                updated[index].conditions!.push({ ifExpr: '', then: '', elseExpr: '' });
                                                                                setTemplateFields(updated);
                                                                            }}
                                                                            sx={{ width: 'fit-content' }}
                                                                        >
                                                                            + Add Another Condition
                                                                        </Button>
                                                                    </AccordionDetails>
                                                                </Accordion>
                                                            </Box>
                                                        )}
                                                    </Box>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </Box>
                        )}

                        {/* Bottom Save Configuration Button */}
                        {templateFields.length > 0 && (
                            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3, pt: 2, borderTop: 1, borderColor: 'divider' }}>
                                <Button
                                    variant="contained"
                                    color="success"
                                    size="large"
                                    startIcon={loading ? <CircularProgress size={20} /> : <Save />}
                                    onClick={generateConfiguration}
                                    disabled={loading || !localSelectedSourceSystem || !templateJobName}
                                    sx={{ 
                                        minWidth: '200px',
                                        py: 1.5,
                                        fontSize: '1rem',
                                        fontWeight: 'bold'
                                    }}
                                >
                                    {loading ? 'Saving Configuration...' : 'Save Field Mappings'}
                                </Button>
                            </Box>
                        )}

                        <Box sx={{ display: 'flex', gap: 2, mt: 3, justifyContent: 'space-between' }}>
                            <Button
                                variant="outlined"
                                onClick={() => handleStepNavigation(activeStep - 1)}
                            >
                                Previous
                            </Button>
                            <Button
                                variant="contained"
                                onClick={() => handleStepNavigation(activeStep + 1)}
                                disabled={!canAdvanceFromStep(activeStep)}
                            >
                                Next: Generate & Save
                            </Button>
                        </Box>
                    </CardContent>
                </Card>
            )}

            {/* Step 6: Generate & Save */}
            {templateFields.length > 0 && activeStep >= 5 && (
                <Card>
                    <CardContent>
                        <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Save /> 6. Generate & Save Configuration
                        </Typography>

                        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 2 }}>
                            <Button
                                variant="contained"
                                startIcon={loading ? <CircularProgress size={20} /> : <Save />}
                                onClick={generateConfiguration}
                                disabled={loading || !localSelectedSourceSystem || !templateJobName}
                            >
                                Generate & Save Configuration
                            </Button>

                            <Button
                                variant="outlined"
                                startIcon={<Download />}
                                disabled={loading}
                                onClick={exportCurrentTemplate}
                            >
                                Export Template
                            </Button>
                            
                            <Button
                                variant="outlined"
                                startIcon={<Download />}
                                disabled={loading}
                                onClick={downloadSampleCSV}
                                color="secondary"
                            >
                                Download Sample CSV
                            </Button>

                            <input
                                accept=".xlsx,.xls,.csv"
                                style={{ display: 'none' }}
                                id="config-upload-button"
                                type="file"
                                onChange={handleFileUpload}
                            />
                            <label htmlFor="config-upload-button">
                                <Button
                                    variant="outlined"
                                    component="span"
                                    startIcon={<Upload />}
                                    disabled={loading}
                                >
                                    Import Configuration
                                </Button>
                            </label>
                        </Box>

                        {(!localSelectedSourceSystem || !templateJobName) && (
                            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                                {!localSelectedSourceSystem
                                    ? "Please select a source system from the dropdown above."
                                    : "Please select a template to generate a job name."
                                }
                            </Typography>
                        )}

                        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'space-between' }}>
                            <Button
                                variant="outlined"
                                onClick={() => handleStepNavigation(activeStep - 1)}
                            >
                                Previous
                            </Button>
                            <Button
                                variant="contained"
                                color="success"
                                onClick={generateConfiguration}
                                disabled={loading || !localSelectedSourceSystem || !templateJobName}
                                startIcon={<Save />}
                            >
                                Complete Configuration
                            </Button>
                        </Box>
                    </CardContent>
                </Card>
            )}

            {/* Generated Configuration Display */}
            {generatedConfig && activeStep === (steps.length - 1) && (
                <Card sx={{ mt: 3 }}>
                    <CardContent>
                        <Typography variant="h6" gutterBottom>
                            Generated Configuration Summary
                        </Typography>
                        
                        <Box sx={{ bgcolor: 'grey.100', p: 2, borderRadius: 1, mb: 2 }}>
                            <Typography variant="body2">
                                <strong>Job:</strong> {generatedConfig.jobName}<br/>
                                <strong>Source System:</strong> {generatedConfig.sourceSystem}<br/>
                                <strong>Transaction Type:</strong> {generatedConfig.transactionType}<br/>
                                <strong>Fields:</strong> {generatedConfig.fields?.length || 0}
                            </Typography>
                        </Box>

                        <Typography variant="subtitle2" gutterBottom>
                            Field Mappings:
                        </Typography>
                        
                        {generatedConfig.fields?.slice(0, 5).map((field: any, index: number) => (
                            <Typography key={index} variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                                {field.targetPosition}. {field.fieldName} → {field.sourceField || '(no source)'} 
                                ({field.transformationType})
                            </Typography>
                        ))}
                        
                        {generatedConfig.fields?.length > 5 && (
                            <Typography variant="body2" color="text.secondary">
                                ... and {generatedConfig.fields.length - 5} more fields
                            </Typography>
                        )}
                    </CardContent>
                </Card>
            )}

            {/* New Source System Dialog */}
            <Dialog 
                open={showNewSourceSystemDialog} 
                onClose={() => setShowNewSourceSystemDialog(false)}
                maxWidth="sm"
                fullWidth
            >
                <DialogTitle>Add New Source System</DialogTitle>
                <DialogContent>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
                        <TextField
                            fullWidth
                            label="System Name"
                            value={newSourceSystemData.name}
                            onChange={(e) => setNewSourceSystemData(prev => ({ ...prev, name: e.target.value }))}
                            placeholder="e.g., CUSTOMER, LOANS, PAYMENTS"
                            disabled={loading}
                        />
                        <TextField
                            fullWidth
                            label="Description"
                            value={newSourceSystemData.description}
                            onChange={(e) => setNewSourceSystemData(prev => ({ ...prev, description: e.target.value }))}
                            placeholder="Brief description of the source system"
                            multiline
                            rows={2}
                            disabled={loading}
                        />
                        <FormControl fullWidth>
                            <InputLabel>System Type</InputLabel>
                            <Select
                                value={newSourceSystemData.systemType}
                                onChange={(e) => setNewSourceSystemData(prev => ({ ...prev, systemType: e.target.value }))}
                                label="System Type"
                                disabled={loading}
                            >
                                <MenuItem value="Oracle">Oracle Database</MenuItem>
                                <MenuItem value="MSSQL">SQL Server</MenuItem>
                                <MenuItem value="MySQL">MySQL</MenuItem>
                                <MenuItem value="File">File System</MenuItem>
                                <MenuItem value="API">REST API</MenuItem>
                            </Select>
                        </FormControl>
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button 
                        onClick={() => setShowNewSourceSystemDialog(false)}
                        disabled={loading}
                    >
                        Cancel
                    </Button>
                    <Button 
                        onClick={handleCreateNewSourceSystem}
                        variant="contained"
                        disabled={loading || !newSourceSystemData.name || !newSourceSystemData.description}
                        startIcon={loading ? <CircularProgress size={20} /> : <Add />}
                    >
                        Create Source System
                    </Button>
                </DialogActions>
            </Dialog>
        </Container>
    );
};

// Main export component wrapped with MasterQueryProvider
const TemplateConfigurationPage: React.FC = () => {
    return (
        <MasterQueryProvider>
            <TemplateConfigurationPageContent />
        </MasterQueryProvider>
    );
};

export default TemplateConfigurationPage;