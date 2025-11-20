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
    useTheme
} from '@mui/material';
import { DataGrid, GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import Editor from '@monaco-editor/react';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SaveIcon from '@mui/icons-material/Save';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import { useSourceSystems } from '../../hooks/useSourceSystems';
import { templateApiService } from '../../services/api/templateApi';
import { FieldTemplate, FileTypeTemplate, FileType, TemplateConfigDto } from '../../types/template';
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

    // Suppress ResizeObserver errors by wrapping the constructor
    useEffect(() => {
        const resizeObserverErr = window.console.error;
        window.console.error = (...args: any[]) => {
            if (args[0]?.toString().includes('ResizeObserver loop')) {
                return;
            }
            resizeObserverErr(...args);
        };

        return () => {
            window.console.error = resizeObserverErr;
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

    // 1. Sync local state with context
    useEffect(() => {
        if (selectedSourceSystem) {
            setLocalSelectedSourceSystem(selectedSourceSystem);
        }
    }, [selectedSourceSystem]);

    // 2. Fetch File Types when Source System selected
    useEffect(() => {
        if (localSelectedSourceSystem) {
            fetchAvailableSourceFields();
        }
    }, [localSelectedSourceSystem]);

    const fetchTemplateFields = async (fileType: string, transactionType: string) => {
        setLoading(true);
        try {
            const data = await templateApiService.getTemplateFields(fileType, transactionType);

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
            setTemplateFields(fieldsWithIds);
        } catch (err) {
            console.error('Failed to load template fields:', err);
            showNotification('Failed to load template fields', 'error');
        } finally {
            setLoading(false);
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
        if (!selectedSourceSystem || !jobName || !selectedFileType || !selectedTransactionType) {
            showNotification('Please complete all selection fields', 'error');
            return;
        }

        setLoading(true);
        try {
            const config: TemplateConfigDto = {
                jobName,
                sourceSystem: selectedSourceSystem.name,
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
                            <InputLabel sx={{ color: 'text.secondary' }}>Source System</InputLabel>
                            <Select
                                value={selectedSourceSystem?.id || ''}
                                label="Source System"
                                onChange={(e) => selectSourceSystem(e.target.value)}
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
                <Grid item xs={12} md={8} sx={{ display: 'flex', flexDirection: 'column', borderRight: 1, borderColor: 'divider' }}>

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
                            <Button size="small" startIcon={<PlayArrowIcon />} sx={{ color: 'success.main' }}>
                                Run Preview
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

                    {/* Field Grid */}
                    <Box sx={{ flexGrow: 1, p: 2, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1, flexShrink: 0 }}>
                            <Typography variant="h6" sx={{ color: 'text.primary' }}>Field Mappings</Typography>
                            <Button startIcon={<AddIcon />} onClick={handleAddField} variant="outlined" size="small">
                                Add Field
                            </Button>
                        </Box>
                        <Box sx={{ flexGrow: 1, width: '100%', overflow: 'hidden' }}>
                            <DataGrid
                                rows={templateFields}
                                columns={columns}
                                onRowClick={(params) => setSelectedFieldId(params.row.id)}
                                sx={{
                                    bgcolor: 'background.paper',
                                    color: 'text.primary',
                                    border: 'none',
                                    '& .MuiDataGrid-cell': { borderBottom: 1, borderColor: 'divider' },
                                    '& .MuiDataGrid-columnHeaders': { borderBottom: 1, borderColor: 'divider', bgcolor: 'background.default' },
                                    '& .MuiDataGrid-row:hover': { bgcolor: 'action.hover' },
                                }}
                                getRowId={(row) => row.id}
                            />
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
                                <InputLabel sx={{ color: 'text.secondary' }}>Transformation Type</InputLabel>
                                <Select
                                    value={getSelectedField()?.transformationType || 'source'}
                                    label="Transformation Type"
                                    onChange={(e) => handlePropertyChange('transformationType', e.target.value)}
                                    sx={{ color: 'text.primary', bgcolor: 'background.paper', '.MuiOutlinedInput-notchedOutline': { borderColor: 'divider' } }}
                                >
                                    <MenuItem value="source">Source Column</MenuItem>
                                    <MenuItem value="constant">Constant Value</MenuItem>
                                    <MenuItem value="conditional">Conditional Logic</MenuItem>
                                </Select>
                            </FormControl>

                            {getSelectedField()?.transformationType === 'source' && (
                                <TextField
                                    label="Source Column"
                                    fullWidth
                                    value={getSelectedField()?.sourceField || ''}
                                    onChange={(e) => handlePropertyChange('sourceField', e.target.value)}
                                    helperText="Column name from SQL query result"
                                    sx={{ bgcolor: 'background.paper', input: { color: 'text.primary' }, label: { color: 'text.secondary' }, helperText: { color: 'text.secondary' } }}
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

                            {getSelectedField()?.transformationType === 'conditional' && (
                                <Paper sx={{ p: 2, bgcolor: 'background.paper' }}>
                                    <Typography variant="subtitle2" sx={{ mb: 1, color: 'text.secondary' }}>Logic Builder</Typography>
                                    <Alert severity="info" sx={{ mb: 1 }}>
                                        Visual builder coming soon. Using JSON config for now.
                                    </Alert>
                                    <Editor
                                        height="200px"
                                        defaultLanguage="json"
                                        theme={theme.palette.mode === 'dark' ? "vs-dark" : "light"}
                                        value={getSelectedField()?.transformationConfig || '{\n  "conditions": []\n}'}
                                        onChange={(val) => handlePropertyChange('transformationConfig', val)}
                                        options={{ minimap: { enabled: false }, fontSize: 12 }}
                                    />
                                </Paper>
                            )}

                            <Divider sx={{ my: 2, borderColor: 'divider' }} />

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
