import React, { useState, useEffect } from 'react';
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
    CardContent
} from '@mui/material';
import { DataGrid, GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import Editor from '@monaco-editor/react';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SaveIcon from '@mui/icons-material/Save';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { useSourceSystems } from '../../hooks/useSourceSystems';
import { templateApiService } from '../../services/api/templateApi';
import { FieldTemplate, FileTypeTemplate, FileType, TemplateConfigDto, QueryPreviewResponse } from '../../types/template';
import { useConfigurationContext } from '../../contexts/ConfigurationContext';
import { MasterQueryProvider } from '../../contexts/MasterQueryContext';
import MasterQuerySelector from '../../components/masterQuery/MasterQuerySelector';
import { MasterQuery } from '../../types/masterQuery';
import { configApi } from '../../services/api/configApi';
import { SourceSystem } from '../../types/configuration';

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
            if (!jobName) {
                setJobName(`${selectedFileType}-${selectedTransactionType}`);
            }
        }
    }, [selectedFileType, selectedTransactionType]);

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
            headerName: 'Source / Value',
            width: 200,
            renderCell: (params: GridRenderCellParams) => {
                const row = params.row as FieldTemplate;
                if (row.transformationType === 'constant') return row.value;
                if (row.transformationType === 'source') return row.sourceField;
                return '(Logic)';
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

    const theme = useTheme();

    return (
        <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column', bgcolor: 'background.default', color: 'text.primary' }}>

            {/* 1. Selection Toolbar */}
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
                    <Grid item xs={12} md={3}>
                        <TextField
                            fullWidth
                            size="small"
                            label="Job Name"
                            value={jobName}
                            onChange={(e) => setJobName(e.target.value)}
                            sx={{ bgcolor: 'action.hover', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                        />
                    </Grid>
                    <Grid item xs={12} md={2} sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                        <Button
                            variant="contained"
                            color="primary"
                            startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <SaveIcon />}
                            onClick={handleSave}
                            disabled={loading}
                        >
                            {loading ? 'Saving...' : 'Save'}
                        </Button>
                    </Grid>
                </Grid>
            </Paper>

            {/* 2. Main Content Area */}
            <Grid container sx={{ flexGrow: 1, overflow: 'hidden' }}>

                {/* Left Pane: Query & Grid */}
                <Grid item xs={12} md={8} sx={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 120px)', borderRight: 1, borderColor: 'divider' }}>

                    {/* Query Editor */}
                    <Box sx={{ height: '30%', borderBottom: 1, borderColor: 'divider', display: 'flex', flexDirection: 'column' }}>
                        <Box sx={{ p: 1, bgcolor: 'background.paper', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                                <Typography variant="subtitle2" sx={{ color: 'text.secondary' }}>Source Query (SQL)</Typography>
                                <Button
                                    size="small"
                                    startIcon={<Box component="span" sx={{ fontSize: 16 }}>🔍</Box>}
                                    onClick={() => setIsQuerySelectorOpen(true)}
                                    sx={{ color: 'primary.light', textTransform: 'none' }}
                                >
                                    Select Master Query
                                </Button>
                            </Box>
                            <Button
                                size="small"
                                startIcon={isQueryRunning ? <CircularProgress size={16} /> : <PlayArrowIcon />}
                                onClick={handleRunPreview}
                                disabled={isQueryRunning || !masterQuerySql}
                                sx={{ color: 'success.main' }}
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
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1, flexShrink: 0 }}>
                            <Typography variant="h6" sx={{ color: 'text.primary' }}>Field Mappings</Typography>
                            <Button startIcon={<AddIcon />} onClick={handleAddField} variant="outlined" size="small">
                                Add Field
                            </Button>
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
                                    {console.log('📊 Rendering DataGrid with', templateFields.length, 'rows, loading:', loading)}
                                    <DataGrid
                                        rows={templateFields}
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
                                        sx={{
                                            bgcolor: 'background.paper',
                                            color: 'text.primary',
                                            border: 'none',
                                            height: '100%',
                                            '& .MuiDataGrid-cell': { borderBottom: 1, borderColor: 'divider' },
                                            '& .MuiDataGrid-columnHeaders': { borderBottom: 1, borderColor: 'divider', bgcolor: 'background.default' },
                                            '& .MuiDataGrid-row:hover': { bgcolor: 'action.hover' },
                                            '& .MuiDataGrid-virtualScroller': { minHeight: '300px' },
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
                <Grid item xs={12} md={4} sx={{ bgcolor: 'background.default', p: 2, overflow: 'auto', borderLeft: 1, borderColor: 'divider' }}>
                    <Typography variant="h6" sx={{ mb: 2, color: 'text.primary' }}>Field Properties</Typography>

                    {getSelectedField() ? (
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                            <TextField
                                label="Field Name"
                                fullWidth
                                value={getSelectedField()?.fieldName || ''}
                                onChange={(e) => handlePropertyChange('fieldName', e.target.value)}
                                InputLabelProps={{ shrink: true }}
                                sx={{ bgcolor: 'background.paper', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                            />

                            <FormControl fullWidth>
                                <InputLabel sx={{ color: 'text.secondary' }}>Data Type</InputLabel>
                                <Select
                                    value={getSelectedField()?.dataType || 'String'}
                                    label="Data Type"
                                    onChange={(e) => handlePropertyChange('dataType', e.target.value)}
                                    sx={{ color: 'text.primary', bgcolor: 'background.paper', '.MuiOutlinedInput-notchedOutline': { borderColor: 'divider' } }}
                                >
                                    <MenuItem value="String">String</MenuItem>
                                    <MenuItem value="Number">Number (Integer)</MenuItem>
                                    <MenuItem value="Decimal">Number (Decimal)</MenuItem>
                                    <MenuItem value="Date">Date</MenuItem>
                                    <MenuItem value="Boolean">Boolean</MenuItem>
                                </Select>
                            </FormControl>

                            <FormControlLabel
                                control={
                                    <Switch
                                        checked={getSelectedField()?.required === 'Y'}
                                        onChange={(e) => handlePropertyChange('required', e.target.checked ? 'Y' : 'N')}
                                        color="primary"
                                    />
                                }
                                label="Required Field"
                                sx={{ color: 'text.primary' }}
                            />

                            <FormControl fullWidth>
                                <InputLabel sx={{ color: 'text.secondary' }}>Transformation Type</InputLabel>
                                <Select
                                    value={getSelectedField()?.transformationType || 'source'}
                                    label="Transformation Type"
                                    onChange={(e) => handlePropertyChange('transformationType', e.target.value)}
                                    sx={{ color: 'text.primary', bgcolor: 'background.paper', '.MuiOutlinedInput-notchedOutline': { borderColor: 'divider' } }}
                                >
                                    <MenuItem value="source">Source Column</MenuItem>
                                    <MenuItem value="constant">Constant Value</MenuItem>
                                    <MenuItem value="composite">Composite (Join Fields)</MenuItem>
                                    <MenuItem value="conditional">Conditional Logic</MenuItem>
                                </Select>
                            </FormControl>

                            {getSelectedField()?.transformationType === 'source' && (
                                <Autocomplete
                                    fullWidth
                                    freeSolo
                                    options={availableSourceFields.map(f => f.name)}
                                    value={getSelectedField()?.sourceField || ''}
                                    onChange={(event, newValue) => handlePropertyChange('sourceField', newValue || '')}
                                    onInputChange={(event, newValue) => handlePropertyChange('sourceField', newValue)}
                                    renderInput={(params) => (
                                        <TextField
                                            {...params}
                                            label="Source Column"
                                            helperText="Select or type column name from SQL query"
                                            sx={{ bgcolor: 'background.paper', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                                        />
                                    )}
                                />
                            )}

                            {getSelectedField()?.transformationType === 'constant' && (
                                <TextField
                                    label="Constant Value"
                                    fullWidth
                                    value={getSelectedField()?.value || ''}
                                    onChange={(e) => handlePropertyChange('value', e.target.value)}
                                    sx={{ bgcolor: 'background.paper', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                                />
                            )}

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
                                            Conditional Logic Builder
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

                            <Divider sx={{ my: 2, borderColor: 'divider' }} />

                            {/* Transformation Summary */}
                            {getSelectedField()?.transformationType && (
                                <Paper sx={{ p: 2, mb: 2, bgcolor: 'primary.lighter', border: '1px solid', borderColor: 'primary.main' }}>
                                    <Typography variant="caption" sx={{ color: 'primary.dark', fontWeight: 'bold', display: 'block', mb: 1 }}>
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
                                </Paper>
                            )}

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

                            <TextField
                                label="Description"
                                fullWidth
                                multiline
                                rows={3}
                                value={getSelectedField()?.description || ''}
                                onChange={(e) => handlePropertyChange('description', e.target.value)}
                                sx={{ bgcolor: 'background.paper', input: { color: 'text.primary' }, label: { color: 'text.secondary' } }}
                            />

                        </Box>
                    ) : (
                        <Typography sx={{ color: 'text.secondary', fontStyle: 'italic' }}>
                            Select a field from the grid to edit properties.
                        </Typography>
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
