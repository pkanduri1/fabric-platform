import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
    Box,
    Grid,
    Paper,
    Typography,
    Button,
    TextField,
    Select,
    MenuItem,
    FormControl,
    InputLabel,
    IconButton,
    Divider,
    Alert,
    Snackbar,
    CircularProgress,
    Dialog, DialogTitle, DialogContent, DialogActions,
    useTheme,
    Accordion,
    AccordionSummary,
    AccordionDetails,
    Autocomplete,
    Switch,
    FormControlLabel,
    Chip,
    Card,
    CardContent,
    InputAdornment,
    Stack
} from '@mui/material';
import { DataGrid, GridColDef, GridRenderCellParams, GridRowModel } from '@mui/x-data-grid';
import Editor from '@monaco-editor/react';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SaveIcon from '@mui/icons-material/Save';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SearchIcon from '@mui/icons-material/Search';
import DownloadIcon from '@mui/icons-material/Download';
import UploadIcon from '@mui/icons-material/Upload';
import ScienceIcon from '@mui/icons-material/Science';
import { useSourceSystems } from '../../hooks/useSourceSystems';
import { templateApiService } from '../../services/api/templateApi';
import { FieldTemplate, FileTypeTemplate, FileType, TemplateConfigDto, QueryPreviewResponse } from '../../types/template';
import { useConfigurationContext } from '../../contexts/ConfigurationContext';
import { MasterQueryProvider } from '../../contexts/MasterQueryContext';
import MasterQuerySelector from '../../components/masterQuery/MasterQuerySelector';
import { MasterQuery } from '../../types/masterQuery';
import { configApi } from '../../services/api/configApi';
import { SourceSystem } from '../../types/configuration';
import * as XLSX from 'xlsx';

// Local interface for DataGrid
interface GridFieldTemplate extends FieldTemplate {
    id: string;
}



const TemplateStudioPageContent: React.FC = () => {
    const { selectedSourceSystem, selectSourceSystem } = useConfigurationContext();
    const { sourceSystems, isLoading: isLoadingSourceSystems } = useSourceSystems();

    // Suppress ResizeObserver errors (both console and runtime errors)
    useEffect(() => {
        // 1. Suppress console.error for ResizeObserver warnings
        const resizeObserverErr = window.console.error;
        window.console.error = (...args: any[]) => {
            if (args[0]?.toString().includes('ResizeObserver loop')) {
                return;
            }
            resizeObserverErr(...args);
        };

        // 2. Suppress runtime errors thrown by ResizeObserver
        const handleGlobalError = (event: ErrorEvent) => {
            if (event.message && event.message.includes('ResizeObserver loop')) {
                event.stopImmediatePropagation();
                event.preventDefault();
                return true;
            }
            return false;
        };

        // Add global error listener with capture phase to catch errors before React's error boundary
        window.addEventListener('error', handleGlobalError, true);

        return () => {
            window.console.error = resizeObserverErr;
            window.removeEventListener('error', handleGlobalError, true);
        };
    }, []);

    const [jobName, setJobName] = useState('');
    const [selectedFileType, setSelectedFileType] = useState('');
    const [selectedTransactionType, setSelectedTransactionType] = useState('');

    // Dynamic options state
    const [fileTypes, setFileTypes] = useState<FileType[]>([]);
    const [transactionTypes, setTransactionTypes] = useState<string[]>([]);
    const [isLoadingFileTypes, setIsLoadingFileTypes] = useState(false);
    const [isLoadingTransactionTypes, setIsLoadingTransactionTypes] = useState(false);


    // Data State
    const [conditionSuggestions, setConditionSuggestions] = useState<string[]>([]);
    const [availableSourceFields, setAvailableSourceFields] = useState<Array<{ name: string; type: string; sample?: string }>>([]);
    const [localSelectedSourceSystem, setLocalSelectedSourceSystem] = useState<SourceSystem | null>(null);
    const [templateFields, setTemplateFields] = useState<GridFieldTemplate[]>([]);
    const [masterQuerySql, setMasterQuerySql] = useState('-- Select a Master Query or write your own SQL\nSELECT * FROM TABLE_NAME');
    const [selectedFieldId, setSelectedFieldId] = useState<string | null>(null);

    // UI State
    const [loading, setLoading] = useState(false);
    const [notification, setNotification] = useState<{ open: boolean, message: string, severity: 'success' | 'error' }>({
        open: false, message: '', severity: 'success'
    });

    // Query Preview State
    const [queryResults, setQueryResults] = useState<QueryPreviewResponse | null>(null);
    const [isQueryRunning, setIsQueryRunning] = useState(false);
    const [showResults, setShowResults] = useState(false);

    // Feature 1: Field Search State
    const [searchText, setSearchText] = useState('');

    // Feature 2: Transformation Testing State
    const [isTestDialogOpen, setIsTestDialogOpen] = useState(false);
    const [testInputs, setTestInputs] = useState<Record<string, string>>({});
    const [testResult, setTestResult] = useState<string>('');

    // --- Effects ---

    // 1. Fetch File Types on Mount
    useEffect(() => {
        fetchFileTypes();
    }, []);

    const fetchFileTypes = async () => {
        try {
            const types = await templateApiService.getFileTypes();
            setFileTypes(types);
        } catch (err) {
            console.error('Failed to load file types:', err);
            showNotification('Failed to load file types', 'error');
        }
    };

    // 2. Fetch Transaction Types when File Type changes
    useEffect(() => {
        if (selectedFileType) {
            fetchTransactionTypes(selectedFileType);
            setTemplateFields([]);
            setSelectedTransactionType('');
        }
    }, [selectedFileType]);

    const fetchTransactionTypes = async (fileType: string) => {
        try {
            const types = await templateApiService.getTransactionTypes(fileType);
            setTransactionTypes(types);
        } catch (err) {
            console.error('Failed to load transaction types:', err);
        }
    };

    // 3. Fetch Template Fields when both types selected
    useEffect(() => {
        if (selectedFileType && selectedTransactionType) {
            fetchTemplateFields(selectedFileType, selectedTransactionType);
            // Auto-generate job name whenever source system, file type, or transaction type changes
            if (localSelectedSourceSystem) {
                setJobName(`${localSelectedSourceSystem.id}-${selectedFileType}-${selectedTransactionType}`);
            } else {
                setJobName(`${selectedFileType}-${selectedTransactionType}`);
            }
        }
    }, [selectedFileType, selectedTransactionType, localSelectedSourceSystem]);

    // DO NOT auto-sync from context - user must explicitly select source system
    // Template Studio should always start with no source system selected

    // 2. Fetch File Types when Source System selected
    useEffect(() => {
        if (localSelectedSourceSystem) {
            fetchAvailableSourceFields();
        }
    }, [localSelectedSourceSystem]);

    const fetchTemplateFields = async (fileType: string, transactionType: string) => {
        setLoading(true);
        try {
            console.log('📥 Fetching template fields:', fileType, transactionType);
            const data = await templateApiService.getTemplateFields(fileType, transactionType);
            console.log('📊 Received data:', data?.length, 'fields');

            if (!data || !Array.isArray(data)) {
                console.warn('No fields data returned or invalid format:', data);
                setTemplateFields([]);
                return;
            }

            // Ensure unique IDs for DataGrid
            const fieldsWithIds: GridFieldTemplate[] = data.map((f, index) => ({
                ...f,
                id: f.fieldName || `field_${index}`, // Fallback ID
                transformationType: (f.transformationType?.toLowerCase() || 'source') as any
            }));
            console.log('✅ Processed fields with IDs:', fieldsWithIds.length);
            console.log('🔍 First 3 fields:', fieldsWithIds.slice(0, 3));
            setTemplateFields(fieldsWithIds);
            console.log('💾 State updated with', fieldsWithIds.length, 'fields');
        } catch (err) {
            console.error('❌ Failed to load template fields:', err);
            showNotification('Failed to load template fields', 'error');
        } finally {
            setLoading(false);
            console.log('🏁 Loading complete');
        }
    };

    // --- Handlers ---

    const handleSourceSystemChange = (systemId: string) => {
        selectSourceSystem(systemId);
    };

    const handleAddField = () => {
        const newField: GridFieldTemplate = {
            id: `new_${Date.now()}`,
            fieldName: 'NEW_FIELD',
            dataType: 'String',
            length: 10,
            required: 'N',
            transformationType: 'source',
            targetPosition: templateFields.length + 1,
            fileType: selectedFileType,
            transactionType: selectedTransactionType
        };
        setTemplateFields([...templateFields, newField]);
        setSelectedFieldId(newField.id);
    };

    const handlePropertyChange = (field: keyof FieldTemplate, value: any) => {
        if (!selectedFieldId) return;
        setTemplateFields(prev => prev.map(f =>
            f.id === selectedFieldId ? { ...f, [field]: value } : f
        ));
    };

    const handleDeleteField = (id: string) => {
        setTemplateFields(prev => prev.filter(f => f.id !== id));
        if (selectedFieldId === id) setSelectedFieldId(null);
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

    const fetchAvailableSourceFields = async () => {
        if (!localSelectedSourceSystem) {
            setAvailableSourceFields([]);
            return;
        }

        try {
            const sourceFields = await configApi.getSourceFields(localSelectedSourceSystem.id);

            const mappedFields = sourceFields.map(field => ({
                name: field.name,
                type: field.dataType || 'string',
                sample: undefined // Backend doesn't provide sample data yet
            }));

            setAvailableSourceFields(mappedFields);
            generateConditionSuggestions(mappedFields);
        } catch (error) {
            console.error('Error fetching source fields:', error);
            setAvailableSourceFields([]);
        }
    };

    const handleSave = async () => {
        if (!localSelectedSourceSystem || !jobName || !selectedFileType || !selectedTransactionType) {
            showNotification('Please complete all selection fields', 'error');
            return;
        }

        setLoading(true);
        try {
            const config: TemplateConfigDto = {
                jobName,
                sourceSystem: localSelectedSourceSystem.name,
                fileType: selectedFileType,
                transactionType: selectedTransactionType,
                masterQuery: masterQuerySql,
                fields: templateFields,
                createdBy: 'ui-user'
            };

            await templateApiService.saveTemplateConfiguration(config);
            showNotification('Configuration saved successfully', 'success');
        } catch (error) {
            console.error('Save error:', error);
            showNotification('Failed to save configuration', 'error');
        } finally {
            setLoading(false);
        }
    };

    const showNotification = (message: string, severity: 'success' | 'error') => {
        setNotification({ open: true, message, severity });
    };

    const handleRunPreview = async () => {
        if (!masterQuerySql || masterQuerySql.trim() === '' || masterQuerySql.trim().startsWith('--')) {
            showNotification('Please enter a SQL query', 'error');
            return;
        }

        setIsQueryRunning(true);
        try {
            const response = await templateApiService.previewQuery(masterQuerySql, 10);
            setQueryResults(response);
            setShowResults(true);

            if (response.success) {
                // Update availableSourceFields from query columns
                const fields = response.columns.map(col => ({
                    name: col,
                    type: 'string', // Default to string
                    sample: undefined
                }));
                setAvailableSourceFields(fields);
                generateConditionSuggestions(fields);

                showNotification(`Query executed successfully: ${response.rowCount} rows returned in ${response.executionTimeMs}ms`, 'success');
            } else {
                showNotification(response.message || 'Query execution failed', 'error');
            }
        } catch (error) {
            console.error('Query preview error:', error);
            showNotification('Failed to execute query preview', 'error');
        } finally {
            setIsQueryRunning(false);
        }
    };

    // Keyboard shortcut for running query (Ctrl/Cmd + Enter)
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                handleRunPreview();
            }
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [masterQuerySql]); // Dependency on masterQuerySql to ensure latest value

    const getSelectedField = () => templateFields.find(f => f.id === selectedFieldId);

    // Feature 1: Filtered fields with search
    const filteredFields = useMemo(() => {
        if (!searchText.trim()) return templateFields;

        const searchLower = searchText.toLowerCase();
        return templateFields.filter(field =>
            field.fieldName?.toLowerCase().includes(searchLower) ||
            field.dataType?.toLowerCase().includes(searchLower) ||
            field.transformationType?.toLowerCase().includes(searchLower) ||
            field.sourceField?.toLowerCase().includes(searchLower) ||
            field.value?.toLowerCase().includes(searchLower) ||
            field.description?.toLowerCase().includes(searchLower)
        );
    }, [templateFields, searchText]);

    // Feature 2: Transformation Testing Logic
    const handleOpenTestDialog = useCallback(() => {
        setTestInputs({});
        setTestResult('');
        setIsTestDialogOpen(true);
    }, []);

    const evaluateTransformation = useCallback(() => {
        const field = getSelectedField();
        if (!field) return;

        try {
            let result = '';

            switch (field.transformationType) {
                case 'source':
                    // For source transformation, just return the input value
                    result = testInputs['sourceValue'] || '';
                    break;

                case 'constant':
                    // For constant, return the constant value
                    result = field.value || '';
                    break;

                case 'composite':
                    // Join multiple source fields with delimiter
                    const parts = field.sources?.map(src => testInputs[src.field] || '') || [];
                    result = parts.join(field.delimiter || '');
                    break;

                case 'conditional':
                    // Evaluate IF-THEN-ELSE logic
                    if (field.conditions && field.conditions.length > 0) {
                        const condition = field.conditions[0];

                        // Evaluate IF condition
                        if (evaluateCondition(condition.ifExpr, testInputs)) {
                            result = condition.then;
                        } else {
                            // Check ELSE-IF conditions
                            let matched = false;
                            if (condition.elseIfExprs && condition.elseIfExprs.length > 0) {
                                for (const elseIf of condition.elseIfExprs) {
                                    if (evaluateCondition(elseIf.condition, testInputs)) {
                                        result = elseIf.value;
                                        matched = true;
                                        break;
                                    }
                                }
                            }

                            // Use ELSE value if no conditions matched
                            if (!matched) {
                                result = condition.elseExpr || '';
                            }
                        }
                    }
                    break;

                default:
                    result = 'Unknown transformation type';
            }

            setTestResult(result);
        } catch (error) {
            setTestResult('Error evaluating transformation: ' + (error as Error).message);
        }
    }, [testInputs]);

    // Simple condition evaluator
    const evaluateCondition = (expr: string, inputs: Record<string, string>): boolean => {
        if (!expr) return false;

        try {
            // Replace variable names with their values
            let evaluatedExpr = expr;
            Object.entries(inputs).forEach(([key, value]) => {
                // Handle string comparisons
                const regex = new RegExp(key, 'g');
                evaluatedExpr = evaluatedExpr.replace(regex, `"${value}"`);
            });

            // Basic evaluation for common operators
            // This is a simplified evaluator - production code would need a proper parser
            if (evaluatedExpr.includes('==')) {
                const [left, right] = evaluatedExpr.split('==').map(s => s.trim().replace(/"/g, ''));
                return left === right;
            } else if (evaluatedExpr.includes('!=')) {
                const [left, right] = evaluatedExpr.split('!=').map(s => s.trim().replace(/"/g, ''));
                return left !== right;
            } else if (evaluatedExpr.includes('>=')) {
                const [left, right] = evaluatedExpr.split('>=').map(s => s.trim());
                return parseFloat(left) >= parseFloat(right);
            } else if (evaluatedExpr.includes('<=')) {
                const [left, right] = evaluatedExpr.split('<=').map(s => s.trim());
                return parseFloat(left) <= parseFloat(right);
            } else if (evaluatedExpr.includes('>')) {
                const [left, right] = evaluatedExpr.split('>').map(s => s.trim());
                return parseFloat(left) > parseFloat(right);
            } else if (evaluatedExpr.includes('<')) {
                const [left, right] = evaluatedExpr.split('<').map(s => s.trim());
                return parseFloat(left) < parseFloat(right);
            } else if (evaluatedExpr.includes('.contains')) {
                // Handle .contains() method
                const match = evaluatedExpr.match(/^"([^"]*)"\.contains\("([^"]*)"\)$/);
                if (match) {
                    return match[1].includes(match[2]);
                }
            }

            return false;
        } catch (error) {
            console.error('Error evaluating condition:', error);
            return false;
        }
    };

    // Feature 3: Export to Excel
    const handleExportExcel = useCallback(() => {
        try {
            const exportData = templateFields.map(field => ({
                'Field Name': field.fieldName,
                'Data Type': field.dataType,
                'Length': field.length,
                'Required': field.required,
                'Transformation Type': field.transformationType,
                'Source Field': field.sourceField || '',
                'Value': field.value || '',
                'Position': field.targetPosition,
                'Description': field.description || ''
            }));

            const worksheet = XLSX.utils.json_to_sheet(exportData);
            const workbook = XLSX.utils.book_new();
            XLSX.utils.book_append_sheet(workbook, worksheet, 'Template Fields');

            const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
            const filename = `template-fields-${jobName || 'export'}-${timestamp}.xlsx`;

            XLSX.writeFile(workbook, filename);
            showNotification('Excel file exported successfully', 'success');
        } catch (error) {
            console.error('Export error:', error);
            showNotification('Failed to export Excel file', 'error');
        }
    }, [templateFields, jobName]);

    // Feature 3: Import from Excel
    const handleImportExcel = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const data = new Uint8Array(e.target?.result as ArrayBuffer);
                const workbook = XLSX.read(data, { type: 'array' });
                const worksheet = workbook.Sheets[workbook.SheetNames[0]];
                const jsonData = XLSX.utils.sheet_to_json(worksheet) as any[];

                const importedFields: GridFieldTemplate[] = jsonData.map((row, index) => ({
                    id: `imported_${Date.now()}_${index}`,
                    fieldName: row['Field Name'] || `FIELD_${index}`,
                    dataType: row['Data Type'] || 'String',
                    length: row['Length'] || 10,
                    required: row['Required'] || 'N',
                    transformationType: (row['Transformation Type'] || 'source') as any,
                    sourceField: row['Source Field'],
                    value: row['Value'],
                    targetPosition: row['Position'] || index + 1,
                    description: row['Description'],
                    fileType: selectedFileType,
                    transactionType: selectedTransactionType
                }));

                setTemplateFields(importedFields);
                showNotification(`Successfully imported ${importedFields.length} fields from Excel`, 'success');
            } catch (error) {
                console.error('Import error:', error);
                showNotification('Failed to import Excel file. Please check the file format.', 'error');
            }
        };

        reader.readAsArrayBuffer(file);
        event.target.value = ''; // Reset file input
    }, [selectedFileType, selectedTransactionType]);

    // --- Columns ---
    const columns: GridColDef[] = [
        { field: 'targetPosition', headerName: 'Pos', width: 70, type: 'number' },
        { field: 'fieldName', headerName: 'Target Field', width: 200 },
        { field: 'length', headerName: 'Len', width: 70, type: 'number' },
        { field: 'dataType', headerName: 'Type', width: 100 },
        {
            field: 'transformationType',
            headerName: 'Transform',
            width: 130,
            editable: true,
            type: 'singleSelect',
            valueOptions: ['source', 'constant', 'composite', 'conditional'],
            renderCell: (params: GridRenderCellParams) => (
                <Box sx={{
                    color: params.value === 'source' ? 'text.secondary' : 'primary.main',
                    fontWeight: params.value !== 'source' ? 'bold' : 'normal'
                }}>
                    {params.value || 'source'}
                </Box>
            )
        },
        {
            field: 'sourceField',
            headerName: 'Source Field',
            width: 180,
            editable: true,
            renderCell: (params: GridRenderCellParams) => {
                const row = params.row as FieldTemplate;
                return row.sourceField || '';
            }
        },
        {
            field: 'value',
            headerName: 'Value',
            width: 150,
            editable: true,
            renderCell: (params: GridRenderCellParams) => {
                const row = params.row as FieldTemplate;
                return row.value || '';
            }
        },
        {
            field: 'actions', headerName: '', width: 50,
            renderCell: (params) => (
                <IconButton size="small" onClick={(e) => {
                    e.stopPropagation();
                    handleDeleteField(params.row.id);
                }}>
                    <DeleteIcon fontSize="small" />
                </IconButton>
            )
        }
    ];

    const [isQuerySelectorOpen, setIsQuerySelectorOpen] = useState(false);

    const handleQuerySelect = (query: MasterQuery) => {
        setMasterQuerySql(query.querySql);
        setIsQuerySelectorOpen(false);
        showNotification(`Selected query: ${query.queryName}`, 'success');
    };

    // Handle inline editing of DataGrid rows
    const processRowUpdate = useCallback((newRow: GridRowModel, oldRow: GridRowModel) => {
        // Update the templateFields state with the edited row
        setTemplateFields(prev => prev.map(field =>
            field.id === newRow.id ? { ...field, ...newRow } as GridFieldTemplate : field
        ));

        showNotification('Field updated successfully', 'success');
        return newRow;
    }, []);

    const handleProcessRowUpdateError = useCallback((error: Error) => {
        console.error('Error updating row:', error);
        showNotification('Failed to update field', 'error');
    }, []);

    const theme = useTheme();

    return (
        <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column', bgcolor: 'background.default', color: 'text.primary' }}>

            {/* 1. Selection Toolbar - Row 1 */}
            <Paper square elevation={1} sx={{ p: 2, bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider', zIndex: 10 }}>
                <Grid container spacing={2} alignItems="center">
                    <Grid item xs={12} md={3}>
                        <FormControl fullWidth size="small">
                            <Select
                                value={localSelectedSourceSystem?.id || ''}
                                onChange={(e) => {
                                    const selectedId = e.target.value;
                                    const selected = sourceSystems.find(sys => sys.id === selectedId);
                                    setLocalSelectedSourceSystem(selected || null);
                                    if (selectedId) {
                                        selectSourceSystem(selectedId);
                                    }
                                }}
                                renderValue={(value) => {
                                    if (!value) {
                                        return <span style={{ color: '#999' }}>Select Source System</span>;
                                    }
                                    const selected = sourceSystems.find(sys => sys.id === value);
                                    return selected?.name || '';
                                }}
                                sx={{ color: 'text.primary', '.MuiOutlinedInput-notchedOutline': { borderColor: 'divider' } }}
                                displayEmpty
                            >
                                <MenuItem value="" disabled>
                                    <em>Select Source System</em>
                                </MenuItem>
                                {sourceSystems.map(sys => (
                                    <MenuItem key={sys.id} value={sys.id}>{sys.name}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12} md={2}>
                        <FormControl fullWidth size="small">
                            <InputLabel sx={{ color: 'text.secondary' }}>File Type</InputLabel>
                            <Select
                                value={selectedFileType}
                                label="File Type"
                                onChange={(e) => setSelectedFileType(e.target.value)}
                                sx={{ color: 'text.primary', '.MuiOutlinedInput-notchedOutline': { borderColor: 'divider' } }}
                            >
                                {fileTypes.map(ft => (
                                    <MenuItem key={ft.fileType} value={ft.fileType}>{ft.fileType}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12} md={2}>
                        <FormControl fullWidth size="small">
                            <InputLabel sx={{ color: 'text.secondary' }}>Transaction Type</InputLabel>
                            <Select
                                value={selectedTransactionType}
                                label="Transaction Type"
                                onChange={(e) => setSelectedTransactionType(e.target.value)}
                                disabled={!selectedFileType}
                                sx={{ color: 'text.primary', '.MuiOutlinedInput-notchedOutline': { borderColor: 'divider' } }}
                            >
                                {transactionTypes.map(tt => (
                                    <MenuItem key={tt} value={tt}>{tt}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12} md={5}>
                        <TextField
                            fullWidth
                            size="small"
                            label="Job Name"
                            value={jobName}
                            onChange={(e) => setJobName(e.target.value)}
                            sx={{ bgcolor: 'action.hover', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                        />
                    </Grid>
                </Grid>
            </Paper>

            {/* 2. Actions Toolbar - Row 2 */}
            <Box sx={{ p: 1.5, bgcolor: 'background.default', borderBottom: 1, borderColor: 'divider' }}>
                <Stack direction="row" spacing={2} justifyContent="space-between" alignItems="center">
                    <Stack direction="row" spacing={1} divider={<Divider orientation="vertical" flexItem />}>
                        <input
                            accept=".xlsx,.xls"
                            style={{ display: 'none' }}
                            id="import-excel-file"
                            type="file"
                            onChange={handleImportExcel}
                        />
                        <label htmlFor="import-excel-file">
                            <Button
                                variant="outlined"
                                component="span"
                                startIcon={<UploadIcon />}
                                size="small"
                                title="Import from Excel"
                            >
                                Import
                            </Button>
                        </label>
                        <Button
                            variant="outlined"
                            startIcon={<DownloadIcon />}
                            onClick={handleExportExcel}
                            disabled={templateFields.length === 0}
                            size="small"
                            title="Export to Excel"
                        >
                            Export
                        </Button>
                    </Stack>
                    <Button
                        variant="contained"
                        color="primary"
                        startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <SaveIcon />}
                        onClick={handleSave}
                        disabled={loading}
                    >
                        {loading ? 'Saving...' : 'Save'}
                    </Button>
                </Stack>
            </Box>

            {/* 3. Main Content Area */}
            <Grid container sx={{ flexGrow: 1, overflow: 'hidden' }}>

                {/* Left Pane: Query & Grid */}
                <Grid item xs={12} md={8} sx={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 180px)', borderRight: 1, borderColor: 'divider' }}>

                    {/* Query Editor */}
                    <Box sx={{ height: '30%', borderBottom: 1, borderColor: 'divider', display: 'flex', flexDirection: 'column' }}>
                        <Box sx={{ p: 1.5, bgcolor: 'background.paper', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: 1, borderColor: 'divider' }}>
                            <Stack direction="row" spacing={2} alignItems="center">
                                <Typography variant="subtitle2" sx={{ color: 'text.secondary', fontWeight: 600 }}>Source Query (SQL)</Typography>
                                <Button
                                    size="small"
                                    variant="outlined"
                                    startIcon={<Box component="span" sx={{ fontSize: 16 }}>🔍</Box>}
                                    onClick={() => setIsQuerySelectorOpen(true)}
                                    sx={{ textTransform: 'none' }}
                                >
                                    Select Master Query
                                </Button>
                            </Stack>
                            <Button
                                size="small"
                                variant="contained"
                                color="success"
                                startIcon={isQueryRunning ? <CircularProgress size={16} color="inherit" /> : <PlayArrowIcon />}
                                onClick={handleRunPreview}
                                disabled={isQueryRunning || !masterQuerySql}
                            >
                                {isQueryRunning ? 'Running...' : 'Run Preview'}
                            </Button>
                        </Box>
                        <Box sx={{ flexGrow: 1, overflow: 'hidden' }}>
                            <Editor
                                height="100%"
                                defaultLanguage="sql"
                                theme={theme.palette.mode === 'dark' ? "vs-dark" : "light"}
                                value={masterQuerySql}
                                onChange={(val) => setMasterQuerySql(val || '')}
                                options={{ minimap: { enabled: false }, fontSize: 13 }}
                            />
                        </Box>
                    </Box>

                    {/* Query Results Panel */}
                    {showResults && queryResults && (
                        <Accordion expanded={showResults} onChange={() => setShowResults(!showResults)}>
                            <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ bgcolor: 'background.paper' }}>
                                <Typography variant="subtitle2" sx={{ color: 'text.primary' }}>
                                    Query Results {queryResults.success && `(${queryResults.rowCount} rows in ${queryResults.executionTimeMs}ms)`}
                                </Typography>
                            </AccordionSummary>
                            <AccordionDetails sx={{ p: 0 }}>
                                <Box sx={{ height: 300, width: '100%' }}>
                                    {queryResults.success ? (
                                        <DataGrid
                                            rows={queryResults.rows.map((row, idx) => {
                                                const rowData: any = { id: idx };
                                                row.forEach((val, colIdx) => {
                                                    rowData[`col${colIdx}`] = val;
                                                });
                                                return rowData;
                                            })}
                                            columns={queryResults.columns.map((col, idx) => ({
                                                field: `col${idx}`,
                                                headerName: col,
                                                width: 150,
                                                flex: 1
                                            }))}
                                            sx={{
                                                bgcolor: 'background.paper',
                                                border: 'none',
                                                '& .MuiDataGrid-cell': { borderBottom: 1, borderColor: 'divider' },
                                                '& .MuiDataGrid-columnHeaders': { borderBottom: 1, borderColor: 'divider', bgcolor: 'background.default' },
                                                '& .MuiDataGrid-row:hover': { bgcolor: 'action.hover' }
                                            }}
                                        />
                                    ) : (
                                        <Alert severity="error" sx={{ m: 2 }}>{queryResults.message}</Alert>
                                    )}
                                </Box>
                            </AccordionDetails>
                        </Accordion>
                    )}

                    {/* Field Grid */}
                    <Box sx={{ flexGrow: 1, p: 2, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                        {/* Field Mappings Header */}
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexShrink: 0 }}>
                            <Typography variant="h6" sx={{ color: 'text.primary' }}>Field Mappings</Typography>
                            <Button
                                startIcon={<AddIcon />}
                                onClick={handleAddField}
                                variant="contained"
                                size="small"
                                color="primary"
                            >
                                Add Field
                            </Button>
                        </Box>

                        {/* Feature 1: Search Field */}
                        <Box sx={{ mb: 2, flexShrink: 0 }}>
                            <TextField
                                fullWidth
                                size="small"
                                placeholder="Search fields by name, type, transformation, or source..."
                                value={searchText}
                                onChange={(e) => setSearchText(e.target.value)}
                                InputProps={{
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <SearchIcon sx={{ color: 'text.secondary' }} />
                                        </InputAdornment>
                                    )
                                }}
                                sx={{
                                    bgcolor: 'background.paper',
                                    '& .MuiOutlinedInput-root': {
                                        '& fieldset': { borderColor: 'divider' }
                                    }
                                }}
                            />
                            <Typography variant="caption" sx={{ color: 'text.secondary', mt: 0.5, display: 'block' }}>
                                Showing {filteredFields.length} of {templateFields.length} fields
                            </Typography>
                        </Box>
                        <Box sx={{ flexGrow: 1, width: '100%', height: '100%', minHeight: 400, overflow: 'hidden' }}>
                            {loading && templateFields.length === 0 ? (
                                <>
                                    {console.log('🔄 Showing loading spinner')}
                                    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                                        <CircularProgress />
                                    </Box>
                                </>
                            ) : (
                                <>
                                    {console.log('📊 Rendering DataGrid with', filteredFields.length, 'rows, loading:', loading)}
                                    <DataGrid
                                        rows={filteredFields}
                                        columns={columns}
                                        onRowClick={(params) => setSelectedFieldId(params.row.id)}
                                        loading={loading}
                                        pagination
                                        pageSizeOptions={[25, 50, 100]}
                                        initialState={{
                                            pagination: { paginationModel: { pageSize: 50 } }
                                        }}
                                        disableRowSelectionOnClick
                                        density="compact"
                                        processRowUpdate={processRowUpdate}
                                        onProcessRowUpdateError={handleProcessRowUpdateError}
                                        sx={{
                                            bgcolor: 'background.paper',
                                            color: 'text.primary',
                                            border: 'none',
                                            height: '100%',
                                            '& .MuiDataGrid-cell': { borderBottom: 1, borderColor: 'divider' },
                                            '& .MuiDataGrid-columnHeaders': { borderBottom: 1, borderColor: 'divider', bgcolor: 'background.default' },
                                            '& .MuiDataGrid-row:hover': { bgcolor: 'action.hover' },
                                            '& .MuiDataGrid-virtualScroller': { minHeight: '300px' },
                                            '& .MuiDataGrid-cell--editable': {
                                                bgcolor: 'action.hover',
                                                '&:hover': {
                                                    bgcolor: 'action.selected',
                                                }
                                            }
                                        }}
                                        getRowId={(row) => row.id}
                                        rowHeight={52}
                                    />
                                </>
                            )}
                        </Box>
                    </Box>
                </Grid>

                {/* Right Pane: Properties */}
                <Grid item xs={12} md={4} sx={{
                    bgcolor: 'background.default',
                    borderLeft: 1,
                    borderColor: 'divider',
                    height: 'calc(100vh - 180px)',
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden'
                }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 2, pb: 1, flexShrink: 0 }}>
                        <Typography variant="h6" sx={{ color: 'text.primary' }}>Field Properties</Typography>
                        {getSelectedField() && (
                            <Button
                                size="small"
                                variant="outlined"
                                startIcon={<ScienceIcon />}
                                onClick={handleOpenTestDialog}
                                sx={{ textTransform: 'none' }}
                            >
                                Test
                            </Button>
                        )}
                    </Box>

                    {getSelectedField() ? (
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, overflow: 'auto', p: 2, pt: 1, flexGrow: 1 }}>

                            {/* TRANSFORMATION CONFIGURATION - Only show for complex transformations */}
                            {(getSelectedField()?.transformationType === 'composite' || getSelectedField()?.transformationType === 'conditional') && (
                                <Accordion defaultExpanded sx={{ bgcolor: 'background.paper' }}>
                                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, width: '100%' }}>
                                            <Typography variant="subtitle2" fontWeight="bold">
                                                {getSelectedField()?.transformationType === 'composite' ? 'Composite Configuration' : 'Conditional Logic'}
                                            </Typography>
                                            <Chip
                                                label={getSelectedField()?.transformationType?.toUpperCase()}
                                                size="small"
                                                color="primary"
                                            />
                                        </Box>
                                    </AccordionSummary>
                                    <AccordionDetails>
                                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                            {getSelectedField()?.transformationType === 'composite' && (
                                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                                    <Autocomplete
                                                        multiple
                                                        fullWidth
                                                        options={availableSourceFields.map(f => f.name)}
                                                        value={getSelectedField()?.sources?.map(s => s.field) || []}
                                                        onChange={(event, newValue) => {
                                                            handlePropertyChange('sources', newValue.map(field => ({ field })));
                                                        }}
                                                        renderTags={(value, getTagProps) =>
                                                            value.map((option, index) => (
                                                                <Chip
                                                                    label={option}
                                                                    {...getTagProps({ index })}
                                                                    size="small"
                                                                    color="primary"
                                                                />
                                                            ))
                                                        }
                                                        renderInput={(params) => (
                                                            <TextField
                                                                {...params}
                                                                label="Source Fields to Combine"
                                                                placeholder="Select fields..."
                                                                helperText="Select fields in the order they should be combined"
                                                                sx={{ bgcolor: 'background.paper', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                                                            />
                                                        )}
                                                    />

                                                    <TextField
                                                        label="Delimiter"
                                                        fullWidth
                                                        value={getSelectedField()?.delimiter || ''}
                                                        onChange={(e) => handlePropertyChange('delimiter', e.target.value)}
                                                        placeholder="e.g., | or , or (space)"
                                                        helperText="Character(s) to join fields with"
                                                        sx={{ bgcolor: 'background.paper', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                                                    />

                                                    {getSelectedField()?.sources && getSelectedField()!.sources!.length > 0 && (
                                                        <Alert severity="info" sx={{ bgcolor: 'info.lighter' }}>
                                                            <Typography variant="caption" sx={{ fontWeight: 'bold' }}>Preview:</Typography>
                                                            <Typography variant="body2" sx={{ mt: 0.5, fontFamily: 'monospace' }}>
                                                                {getSelectedField()!.sources!.map(s => s.field).join(getSelectedField()?.delimiter || '')}
                                                            </Typography>
                                                        </Alert>
                                                    )}
                                                </Box>
                                            )}

                                            {getSelectedField()?.transformationType === 'conditional' && (
                                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                        <Typography variant="subtitle2" sx={{ color: 'text.secondary' }}>
                                                            IF-THEN-ELSE Logic Builder
                                                        </Typography>
                                                        <Button
                                                            size="small"
                                                            startIcon={<AddIcon />}
                                                            onClick={() => {
                                                                const currentConditions = getSelectedField()?.conditions || [];
                                                                handlePropertyChange('conditions', [
                                                                    ...currentConditions,
                                                                    { ifExpr: '', then: '', elseExpr: '', elseIfExprs: [] }
                                                                ]);
                                                            }}
                                                        >
                                                            Add Condition
                                                        </Button>
                                                    </Box>

                                                    {(!getSelectedField()?.conditions || getSelectedField()!.conditions!.length === 0) && (
                                                        <Alert severity="info" sx={{ bgcolor: 'info.lighter' }}>
                                                            Click "Add Condition" to create IF-THEN-ELSE logic
                                                        </Alert>
                                                    )}

                                                    {getSelectedField()?.conditions?.map((condition, condIndex) => (
                                                        <Card key={condIndex} sx={{ bgcolor: 'background.paper', border: '1px solid', borderColor: 'divider' }}>
                                                            <CardContent>
                                                                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                                                                    <Typography variant="subtitle2" sx={{ color: 'primary.main', fontWeight: 'bold' }}>
                                                                        Condition {condIndex + 1}
                                                                    </Typography>
                                                                    <IconButton
                                                                        size="small"
                                                                        onClick={() => {
                                                                            const newConditions = [...(getSelectedField()?.conditions || [])];
                                                                            newConditions.splice(condIndex, 1);
                                                                            handlePropertyChange('conditions', newConditions);
                                                                        }}
                                                                    >
                                                                        <DeleteIcon fontSize="small" />
                                                                    </IconButton>
                                                                </Box>

                                                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                                                    {/* IF Expression */}
                                                                    <TextField
                                                                        label="IF Condition"
                                                                        fullWidth
                                                                        value={condition.ifExpr}
                                                                        onChange={(e) => {
                                                                            const newConditions = [...(getSelectedField()?.conditions || [])];
                                                                            newConditions[condIndex] = { ...newConditions[condIndex], ifExpr: e.target.value };
                                                                            handlePropertyChange('conditions', newConditions);
                                                                        }}
                                                                        placeholder="e.g., STATUS = 'A' or AMOUNT > 1000"
                                                                        helperText="Enter the condition expression"
                                                                        sx={{ bgcolor: 'background.default', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                                                                    />

                                                                    {/* THEN Value */}
                                                                    <TextField
                                                                        label="THEN Value"
                                                                        fullWidth
                                                                        value={condition.then}
                                                                        onChange={(e) => {
                                                                            const newConditions = [...(getSelectedField()?.conditions || [])];
                                                                            newConditions[condIndex] = { ...newConditions[condIndex], then: e.target.value };
                                                                            handlePropertyChange('conditions', newConditions);
                                                                        }}
                                                                        placeholder="Value when condition is true"
                                                                        helperText="Value to use when the IF condition is true"
                                                                        sx={{ bgcolor: 'background.default', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                                                                    />

                                                                    {/* ELSE-IF Expressions */}
                                                                    {condition.elseIfExprs && condition.elseIfExprs.length > 0 && (
                                                                        <Box sx={{ pl: 2, borderLeft: '2px solid', borderColor: 'warning.main' }}>
                                                                            <Typography variant="caption" sx={{ color: 'warning.main', fontWeight: 'bold', mb: 1, display: 'block' }}>
                                                                                ELSE-IF Conditions
                                                                            </Typography>
                                                                            {condition.elseIfExprs.map((elseIf, elseIfIndex) => (
                                                                                <Box key={elseIfIndex} sx={{ display: 'flex', gap: 1, mb: 1, alignItems: 'flex-start' }}>
                                                                                    <TextField
                                                                                        label={`ELSE-IF ${elseIfIndex + 1} Condition`}
                                                                                        size="small"
                                                                                        sx={{ flex: 1, bgcolor: 'background.default' }}
                                                                                        value={elseIf.condition}
                                                                                        onChange={(e) => {
                                                                                            const newConditions = [...(getSelectedField()?.conditions || [])];
                                                                                            const newElseIfs = [...(newConditions[condIndex].elseIfExprs || [])];
                                                                                            newElseIfs[elseIfIndex] = { ...newElseIfs[elseIfIndex], condition: e.target.value };
                                                                                            newConditions[condIndex] = { ...newConditions[condIndex], elseIfExprs: newElseIfs };
                                                                                            handlePropertyChange('conditions', newConditions);
                                                                                        }}
                                                                                    />
                                                                                    <TextField
                                                                                        label="Value"
                                                                                        size="small"
                                                                                        sx={{ flex: 1, bgcolor: 'background.default' }}
                                                                                        value={elseIf.value}
                                                                                        onChange={(e) => {
                                                                                            const newConditions = [...(getSelectedField()?.conditions || [])];
                                                                                            const newElseIfs = [...(newConditions[condIndex].elseIfExprs || [])];
                                                                                            newElseIfs[elseIfIndex] = { ...newElseIfs[elseIfIndex], value: e.target.value };
                                                                                            newConditions[condIndex] = { ...newConditions[condIndex], elseIfExprs: newElseIfs };
                                                                                            handlePropertyChange('conditions', newConditions);
                                                                                        }}
                                                                                    />
                                                                                    <IconButton
                                                                                        size="small"
                                                                                        onClick={() => {
                                                                                            const newConditions = [...(getSelectedField()?.conditions || [])];
                                                                                            const newElseIfs = [...(newConditions[condIndex].elseIfExprs || [])];
                                                                                            newElseIfs.splice(elseIfIndex, 1);
                                                                                            newConditions[condIndex] = { ...newConditions[condIndex], elseIfExprs: newElseIfs };
                                                                                            handlePropertyChange('conditions', newConditions);
                                                                                        }}
                                                                                    >
                                                                                        <DeleteIcon fontSize="small" />
                                                                                    </IconButton>
                                                                                </Box>
                                                                            ))}
                                                                        </Box>
                                                                    )}

                                                                    {/* Add ELSE-IF Button */}
                                                                    <Button
                                                                        size="small"
                                                                        startIcon={<AddIcon />}
                                                                        onClick={() => {
                                                                            const newConditions = [...(getSelectedField()?.conditions || [])];
                                                                            const currentElseIfs = newConditions[condIndex].elseIfExprs || [];
                                                                            newConditions[condIndex] = {
                                                                                ...newConditions[condIndex],
                                                                                elseIfExprs: [...currentElseIfs, { condition: '', value: '' }]
                                                                            };
                                                                            handlePropertyChange('conditions', newConditions);
                                                                        }}
                                                                        sx={{ alignSelf: 'flex-start' }}
                                                                    >
                                                                        Add ELSE-IF
                                                                    </Button>

                                                                    {/* ELSE Value */}
                                                                    <TextField
                                                                        label="ELSE Value (Default)"
                                                                        fullWidth
                                                                        value={condition.elseExpr || ''}
                                                                        onChange={(e) => {
                                                                            const newConditions = [...(getSelectedField()?.conditions || [])];
                                                                            newConditions[condIndex] = { ...newConditions[condIndex], elseExpr: e.target.value };
                                                                            handlePropertyChange('conditions', newConditions);
                                                                        }}
                                                                        placeholder="Default value when all conditions are false"
                                                                        helperText="Value to use when all conditions are false"
                                                                        sx={{ bgcolor: 'background.default', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                                                                    />
                                                                </Box>

                                                                {/* Preview */}
                                                                {condition.ifExpr && condition.then && (
                                                                    <Alert severity="success" sx={{ mt: 2, bgcolor: 'success.lighter' }}>
                                                                        <Typography variant="caption" sx={{ fontWeight: 'bold', display: 'block', mb: 0.5 }}>
                                                                            Logic Preview:
                                                                        </Typography>
                                                                        <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                                                                            IF ({condition.ifExpr}) THEN "{condition.then}"
                                                                            {condition.elseIfExprs && condition.elseIfExprs.length > 0 && (
                                                                                <>
                                                                                    {condition.elseIfExprs.map((elseIf, idx) => (
                                                                                        <span key={idx}>
                                                                                            <br />ELSE IF ({elseIf.condition}) THEN "{elseIf.value}"
                                                                                        </span>
                                                                                    ))}
                                                                                </>
                                                                            )}
                                                                            {condition.elseExpr && (
                                                                                <>
                                                                                    <br />ELSE "{condition.elseExpr}"
                                                                                </>
                                                                            )}
                                                                        </Typography>
                                                                    </Alert>
                                                                )}
                                                            </CardContent>
                                                        </Card>
                                                    ))}
                                                </Box>
                                            )}
                                        </Box>
                                    </AccordionDetails>
                                </Accordion>
                            )}

                            {/* TRANSFORMATION SUMMARY CARD */}
                            {getSelectedField()?.transformationType && (
                                <Card variant="outlined" sx={{ bgcolor: 'primary.lighter', borderColor: 'primary.main' }}>
                                    <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                                        <Typography variant="caption" color="primary.dark" fontWeight="bold" display="block" gutterBottom>
                                            Transformation Summary
                                        </Typography>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                            <Chip
                                                label={getSelectedField()?.transformationType?.toUpperCase()}
                                                color="primary"
                                                size="small"
                                                sx={{ fontWeight: 'bold' }}
                                            />
                                            <Typography variant="body2" sx={{ color: 'text.primary' }}>
                                                {getSelectedField()?.transformationType === 'source' && getSelectedField()?.sourceField && (
                                                    <>→ Map from: <strong>{getSelectedField()?.sourceField}</strong></>
                                                )}
                                                {getSelectedField()?.transformationType === 'constant' && getSelectedField()?.value && (
                                                    <>→ Constant value: <strong>"{getSelectedField()?.value}"</strong></>
                                                )}
                                                {getSelectedField()?.transformationType === 'composite' && getSelectedField()?.sources && getSelectedField()!.sources!.length > 0 && (
                                                    <>→ Combine: <strong>{getSelectedField()!.sources!.map(s => s.field).join(` ${getSelectedField()?.delimiter || ''} `)}</strong></>
                                                )}
                                                {getSelectedField()?.transformationType === 'conditional' && getSelectedField()?.conditions && getSelectedField()!.conditions!.length > 0 && (
                                                    <>→ Conditional logic with <strong>{getSelectedField()!.conditions!.length}</strong> condition(s)</>
                                                )}
                                                {getSelectedField()?.transformationType === 'source' && !getSelectedField()?.sourceField && (
                                                    <em style={{ color: 'gray' }}>No source field selected</em>
                                                )}
                                                {getSelectedField()?.transformationType === 'constant' && !getSelectedField()?.value && (
                                                    <em style={{ color: 'gray' }}>No constant value set</em>
                                                )}
                                                {getSelectedField()?.transformationType === 'composite' && (!getSelectedField()?.sources || getSelectedField()!.sources!.length === 0) && (
                                                    <em style={{ color: 'gray' }}>No source fields selected</em>
                                                )}
                                                {getSelectedField()?.transformationType === 'conditional' && (!getSelectedField()?.conditions || getSelectedField()!.conditions!.length === 0) && (
                                                    <em style={{ color: 'gray' }}>No conditions defined</em>
                                                )}
                                            </Typography>
                                        </Box>
                                        {getSelectedField()?.defaultValue && (
                                            <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mt: 1 }}>
                                                Default: <strong>{getSelectedField()?.defaultValue}</strong>
                                            </Typography>
                                        )}
                                    </CardContent>
                                </Card>
                            )}

                            {/* 4. FIELD SPECIFICATIONS ACCORDION */}
                            <Accordion sx={{ bgcolor: 'background.paper' }}>
                                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                                    <Typography variant="subtitle2" fontWeight="bold">Field Specifications</Typography>
                                </AccordionSummary>
                                <AccordionDetails>
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                        <Grid container spacing={2}>
                                            <Grid item xs={6}>
                                                <TextField
                                                    label="Start Pos"
                                                    type="number"
                                                    fullWidth
                                                    value={getSelectedField()?.targetPosition}
                                                    onChange={(e) => handlePropertyChange('targetPosition', parseInt(e.target.value))}
                                                    sx={{ bgcolor: 'background.paper', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                                                />
                                            </Grid>
                                            <Grid item xs={6}>
                                                <TextField
                                                    label="Length"
                                                    type="number"
                                                    fullWidth
                                                    value={getSelectedField()?.length}
                                                    onChange={(e) => handlePropertyChange('length', parseInt(e.target.value))}
                                                    sx={{ bgcolor: 'background.paper', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                                                />
                                            </Grid>
                                        </Grid>
                                    </Box>
                                </AccordionDetails>
                            </Accordion>

                            {/* 5. DESCRIPTION ACCORDION */}
                            <Accordion sx={{ bgcolor: 'background.paper' }}>
                                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                                    <Typography variant="subtitle2" fontWeight="bold">Description & Notes</Typography>
                                </AccordionSummary>
                                <AccordionDetails>
                                    <TextField
                                        label="Description"
                                        fullWidth
                                        multiline
                                        rows={3}
                                        value={getSelectedField()?.description || ''}
                                        onChange={(e) => handlePropertyChange('description', e.target.value)}
                                        sx={{ bgcolor: 'background.paper', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                                    />
                                </AccordionDetails>
                            </Accordion>

                        </Box>
                    ) : (
                        <Box sx={{ p: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', flexGrow: 1 }}>
                            <Typography sx={{ color: 'text.secondary', fontStyle: 'italic' }}>
                                Select a field from the grid to edit properties.
                            </Typography>
                        </Box>
                    )}
                </Grid>
            </Grid>

            <Snackbar
                open={notification.open}
                autoHideDuration={6000}
                onClose={() => setNotification({ ...notification, open: false })}
            >
                <Alert severity={notification.severity} sx={{ width: '100%' }}>
                    {notification.message}
                </Alert>
            </Snackbar>

            {/* Feature 2: Transformation Testing Dialog */}
            <Dialog
                open={isTestDialogOpen}
                onClose={() => setIsTestDialogOpen(false)}
                maxWidth="md"
                fullWidth
            >
                <DialogTitle>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <ScienceIcon color="primary" />
                        <Typography variant="h6">Test Transformation</Typography>
                    </Box>
                </DialogTitle>
                <DialogContent>
                    {getSelectedField() && (
                        <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 3 }}>
                            {/* Field Info */}
                            <Alert severity="info" sx={{ mb: 2 }}>
                                <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                                    Field: {getSelectedField()?.fieldName}
                                </Typography>
                                <Typography variant="caption">
                                    Type: {getSelectedField()?.transformationType?.toUpperCase()}
                                </Typography>
                            </Alert>

                            {/* Input Fields based on transformation type */}
                            {getSelectedField()?.transformationType === 'source' && (
                                <TextField
                                    fullWidth
                                    label="Sample Input Value"
                                    placeholder="Enter sample data..."
                                    value={testInputs['sourceValue'] || ''}
                                    onChange={(e) => setTestInputs({ ...testInputs, sourceValue: e.target.value })}
                                    helperText="Enter a sample value to see how it would appear"
                                />
                            )}

                            {getSelectedField()?.transformationType === 'constant' && (
                                <Alert severity="success">
                                    <Typography variant="body2">
                                        Constant value: <strong>{getSelectedField()?.value || '(not set)'}</strong>
                                    </Typography>
                                    <Typography variant="caption" sx={{ display: 'block', mt: 1 }}>
                                        This field always returns the same value regardless of input.
                                    </Typography>
                                </Alert>
                            )}

                            {getSelectedField()?.transformationType === 'composite' && (
                                <Box>
                                    <Typography variant="subtitle2" sx={{ mb: 2, color: 'text.secondary' }}>
                                        Enter sample values for each source field:
                                    </Typography>
                                    {getSelectedField()?.sources?.map((src, idx) => (
                                        <TextField
                                            key={idx}
                                            fullWidth
                                            label={src.field}
                                            placeholder={`Enter value for ${src.field}...`}
                                            value={testInputs[src.field] || ''}
                                            onChange={(e) => setTestInputs({ ...testInputs, [src.field]: e.target.value })}
                                            sx={{ mb: 2 }}
                                        />
                                    ))}
                                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                                        Delimiter: "{getSelectedField()?.delimiter || '(none)'}"
                                    </Typography>
                                </Box>
                            )}

                            {getSelectedField()?.transformationType === 'conditional' && (
                                <Box>
                                    <Typography variant="subtitle2" sx={{ mb: 2, color: 'text.secondary' }}>
                                        Enter test values for condition variables:
                                    </Typography>
                                    {getSelectedField()?.conditions?.[0] && (() => {
                                        const currentCondition = getSelectedField()?.conditions?.[0];
                                        const allExprs = [
                                            currentCondition?.ifExpr,
                                            ...(currentCondition?.elseIfExprs?.map(e => e.condition) || [])
                                        ].filter(Boolean);

                                        // Extract unique variable names (simple approach)
                                        const variables = new Set<string>();
                                        allExprs.forEach(expr => {
                                            const matches = expr?.match(/[A-Z_][A-Z0-9_]*/g);
                                            matches?.forEach(v => {
                                                if (!['AND', 'OR', 'NOT', 'NULL', 'TRUE', 'FALSE'].includes(v)) {
                                                    variables.add(v);
                                                }
                                            });
                                        });

                                        return (
                                            <>
                                                {Array.from(variables).map(varName => (
                                                    <TextField
                                                        key={varName}
                                                        fullWidth
                                                        label={varName}
                                                        placeholder={`Enter value for ${varName}...`}
                                                        value={testInputs[varName] || ''}
                                                        onChange={(e) => setTestInputs({ ...testInputs, [varName]: e.target.value })}
                                                        sx={{ mb: 2 }}
                                                    />
                                                ))}
                                                <Alert severity="info" sx={{ mt: 2 }}>
                                                    <Typography variant="caption" sx={{ fontFamily: 'monospace', fontSize: '0.7rem' }}>
                                                        IF ({currentCondition?.ifExpr}) THEN "{currentCondition?.then}"
                                                        {currentCondition?.elseIfExprs?.map((elseIf, idx) => (
                                                            <span key={idx}>
                                                                <br />ELSE IF ({elseIf.condition}) THEN "{elseIf.value}"
                                                            </span>
                                                        ))}
                                                        {currentCondition?.elseExpr && (
                                                            <>
                                                                <br />ELSE "{currentCondition.elseExpr}"
                                                            </>
                                                        )}
                                                    </Typography>
                                                </Alert>
                                            </>
                                        );
                                    })()}
                                </Box>
                            )}

                            {/* Test Button */}
                            <Button
                                variant="contained"
                                color="primary"
                                onClick={evaluateTransformation}
                                startIcon={<PlayArrowIcon />}
                                fullWidth
                            >
                                Evaluate Transformation
                            </Button>

                            {/* Result Display */}
                            {testResult !== '' && (
                                <Paper sx={{ p: 3, bgcolor: 'success.lighter', border: '2px solid', borderColor: 'success.main' }}>
                                    <Typography variant="caption" sx={{ color: 'success.dark', fontWeight: 'bold', display: 'block', mb: 1 }}>
                                        RESULT:
                                    </Typography>
                                    <Typography variant="h6" sx={{ color: 'text.primary', fontFamily: 'monospace', wordBreak: 'break-all' }}>
                                        {testResult || '(empty)'}
                                    </Typography>
                                </Paper>
                            )}
                        </Box>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setIsTestDialogOpen(false)}>Close</Button>
                </DialogActions>
            </Dialog>

            {/* Master Query Selector Dialog */}
            <Dialog
                open={isQuerySelectorOpen}
                onClose={() => setIsQuerySelectorOpen(false)}
                maxWidth="lg"
                fullWidth
            >
                <DialogTitle>Select Master Query</DialogTitle>
                <DialogContent>
                    <MasterQuerySelector
                        onQuerySelect={handleQuerySelect}
                        showActions={false}
                        maxHeight={500}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setIsQuerySelectorOpen(false)}>Cancel</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

const TemplateStudioPage = () => {
    return (
        <MasterQueryProvider>
            <TemplateStudioPageContent />
        </MasterQueryProvider>
    );
};

export default TemplateStudioPage;
