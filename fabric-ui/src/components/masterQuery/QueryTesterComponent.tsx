// src/components/masterQuery/QueryTesterComponent.tsx
/**
 * Query Tester Component - Live Query Execution & Results
 * Features: Real-time execution, Parameter handling, Results visualization
 */

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Typography,
  Button,
  IconButton,
  Tooltip,
  Alert,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Grid,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Collapse,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  List,
  ListItem,
  ListItemText,
  CircularProgress,
  LinearProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Switch,
  FormControlLabel,
  Tabs,
  Tab,
  useTheme
} from '@mui/material';
import {
  PlayArrow,
  Stop,
  Refresh,
  ExpandMore,
  GetApp,
  Visibility,
  TableChart,
  Assessment,
  Speed,
  Security,
  Error,
  Warning,
  CheckCircle,
  Timer,
  Storage,
  TrendingUp,
  FilterList,
  Search,
  Settings,
  History,
  Share,
  BookmarkBorder,
  Bookmark
} from '@mui/icons-material';
import { useMasterQueryContext, useQueryExecution } from '../../contexts/MasterQueryContext';
import { MasterQueryRequest, MasterQueryResponse, ColumnMetadata } from '../../types/masterQuery';

// Component props
interface QueryTesterComponentProps {
  query?: MasterQueryRequest;
  onExecutionComplete?: (response: MasterQueryResponse) => void;
  autoExecute?: boolean;
  showHistory?: boolean;
  maxRows?: number;
}

// Parameter input interface
interface QueryParameter {
  name: string;
  type: 'string' | 'number' | 'date' | 'boolean';
  value: any;
  required: boolean;
  description?: string;
  defaultValue?: any;
}

// Tab panel component
interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`tester-tabpanel-${index}`}
      aria-labelledby={`tester-tab-${index}`}
      {...other}
    >
      {value === index && <Box>{children}</Box>}
    </div>
  );
}

export const QueryTesterComponent: React.FC<QueryTesterComponentProps> = ({
  query,
  onExecutionComplete,
  autoExecute = false,
  showHistory = true,
  maxRows = 1000
}) => {
  const theme = useTheme();
  
  // Context state
  const {
    executeQuery,
    isExecuting,
    queryResults,
    executionHistory,
    getColumnMetadata,
    columnMetadata,
    isLoadingMetadata,
    error,
    clearError
  } = useQueryExecution();
  
  // Local state
  const [currentQuery, setCurrentQuery] = useState<MasterQueryRequest | null>(query || null);
  const [parameters, setParameters] = useState<QueryParameter[]>([]);
  const [executionTime, setExecutionTime] = useState<number>(0);
  const [lastResponse, setLastResponse] = useState<MasterQueryResponse | null>(null);
  const [activeTab, setActiveTab] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(25);
  const [showParameters, setShowParameters] = useState(true);
  const [showExecutionDetails, setShowExecutionDetails] = useState(false);
  const [filterValue, setFilterValue] = useState('');
  const [sortColumn, setSortColumn] = useState<string | null>(null);
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [savedExecutions, setSavedExecutions] = useState<Set<string>>(new Set());
  
  // Update query when prop changes
  useEffect(() => {
    if (query && query !== currentQuery) {
      setCurrentQuery(query);
      extractParameters(query);
    }
  }, [query, currentQuery]);
  
  // Load saved executions from localStorage
  useEffect(() => {
    const saved = localStorage.getItem('fabric-saved-executions');
    if (saved) {
      try {
        setSavedExecutions(new Set(JSON.parse(saved)));
      } catch (e) {
        console.warn('Failed to parse saved executions from localStorage');
      }
    }
  }, []);
  
  // Auto-execute if enabled and query is ready
  useEffect(() => {
    if (autoExecute && currentQuery && parameters.every(p => !p.required || p.value)) {
      handleExecute();
    }
  }, [autoExecute, currentQuery, parameters]);
  
  // Extract parameters from SQL query
  const extractParameters = useCallback((query: MasterQueryRequest) => {
    const sql = query.querySql;
    const paramRegex = /:(\w+)/g;
    const foundParams: Set<string> = new Set();
    let match;
    
    while ((match = paramRegex.exec(sql)) !== null) {
      foundParams.add(match[1]);
    }
    
    const newParameters: QueryParameter[] = Array.from(foundParams).map(name => ({
      name,
      type: inferParameterType(name),
      value: getDefaultValue(name),
      required: true,
      description: getParameterDescription(name)
    }));
    
    setParameters(newParameters);
  }, []);
  
  // Infer parameter type from name
  const inferParameterType = useCallback((name: string): QueryParameter['type'] => {
    const lowerName = name.toLowerCase();
    
    if (lowerName.includes('date') || lowerName.includes('time')) {
      return 'date';
    }
    if (lowerName.includes('amount') || lowerName.includes('count') || lowerName.includes('id')) {
      return 'number';
    }
    if (lowerName.includes('flag') || lowerName.includes('enabled') || lowerName.includes('active')) {
      return 'boolean';
    }
    
    return 'string';
  }, []);
  
  // Get default value for parameter
  const getDefaultValue = useCallback((name: string) => {
    const lowerName = name.toLowerCase();
    
    if (lowerName.includes('date')) {
      return new Date().toISOString().split('T')[0]; // YYYY-MM-DD format
    }
    if (lowerName.includes('batchdate')) {
      return new Date().toISOString().split('T')[0];
    }
    if (lowerName.includes('amount')) {
      return 100;
    }
    
    return '';
  }, []);
  
  // Get parameter description
  const getParameterDescription = useCallback((name: string): string => {
    const lowerName = name.toLowerCase();
    
    if (lowerName.includes('batchdate')) {
      return 'Batch processing date (YYYY-MM-DD)';
    }
    if (lowerName.includes('accountid')) {
      return 'Bank account identifier';
    }
    if (lowerName.includes('customerid')) {
      return 'Customer identifier';
    }
    if (lowerName.includes('amount')) {
      return 'Monetary amount';
    }
    if (lowerName.includes('startdate')) {
      return 'Start date for date range';
    }
    if (lowerName.includes('enddate')) {
      return 'End date for date range';
    }
    
    return `Query parameter: ${name}`;
  }, []);
  
  // Handle parameter value change
  const handleParameterChange = useCallback((paramName: string, value: any) => {
    setParameters(prev => prev.map(param =>
      param.name === paramName ? { ...param, value } : param
    ));
  }, []);
  
  // Build query parameters object
  const buildQueryParameters = useCallback((): Record<string, any> => {
    const params: Record<string, any> = {};
    
    parameters.forEach(param => {
      if (param.value !== undefined && param.value !== '') {
        params[param.name] = param.value;
      }
    });
    
    return params;
  }, [parameters]);
  
  // Handle query execution
  const handleExecute = useCallback(async () => {
    if (!currentQuery) return;
    
    try {
      const startTime = performance.now();
      
      // Build request with parameters
      const request: MasterQueryRequest = {
        ...currentQuery,
        queryParameters: buildQueryParameters()
      };
      
      // Execute query
      const response = await executeQuery(request);
      
      const endTime = performance.now();
      setExecutionTime(endTime - startTime);
      setLastResponse(response);
      
      // Load column metadata if not already loaded
      if (!columnMetadata && response.executionStatus === 'SUCCESS') {
        getColumnMetadata(request);
      }
      
      // Reset pagination
      setPage(0);
      
      // Notify parent
      if (onExecutionComplete) {
        onExecutionComplete(response);
      }
      
    } catch (err) {
      console.error('Query execution failed:', err);
    }
  }, [currentQuery, buildQueryParameters, executeQuery, columnMetadata, getColumnMetadata, onExecutionComplete]);
  
  // Handle column sort
  const handleSort = useCallback((column: string) => {
    if (sortColumn === column) {
      setSortDirection(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortColumn(column);
      setSortDirection('asc');
    }
  }, [sortColumn]);
  
  // Filter and sort results
  const processedResults = useMemo(() => {
    if (!queryResults) return [];
    
    let results = [...queryResults];
    
    // Apply filter
    if (filterValue) {
      const filter = filterValue.toLowerCase();
      results = results.filter(row =>
        Object.values(row).some(value =>
          String(value).toLowerCase().includes(filter)
        )
      );
    }
    
    // Apply sort
    if (sortColumn) {
      results.sort((a, b) => {
        const aVal = a[sortColumn];
        const bVal = b[sortColumn];
        
        if (aVal === bVal) return 0;
        
        const comparison = aVal < bVal ? -1 : 1;
        return sortDirection === 'asc' ? comparison : -comparison;
      });
    }
    
    return results;
  }, [queryResults, filterValue, sortColumn, sortDirection]);
  
  // Get paginated results
  const paginatedResults = useMemo(() => {
    const startIndex = page * rowsPerPage;
    return processedResults.slice(startIndex, startIndex + rowsPerPage);
  }, [processedResults, page, rowsPerPage]);
  
  // Toggle saved execution
  const toggleSavedExecution = useCallback((correlationId: string) => {
    const newSaved = new Set(savedExecutions);
    if (newSaved.has(correlationId)) {
      newSaved.delete(correlationId);
    } else {
      newSaved.add(correlationId);
    }
    
    setSavedExecutions(newSaved);
    localStorage.setItem('fabric-saved-executions', JSON.stringify(Array.from(newSaved)));
  }, [savedExecutions]);
  
  // Export results to CSV
  const exportResults = useCallback(() => {
    if (!queryResults || !columnMetadata) return;
    
    const headers = columnMetadata.map((col: ColumnMetadata) => col.name).join(',');
    const rows = queryResults.map((row: any) =>
      columnMetadata.map((col: ColumnMetadata) => {
        const value = row[col.name];
        return typeof value === 'string' && value.includes(',') 
          ? `"${value}"` 
          : String(value || '');
      }).join(',')
    ).join('\n');
    
    const csv = `${headers}\n${rows}`;
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `query_results_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    
    URL.revokeObjectURL(url);
  }, [queryResults, columnMetadata]);
  
  // Render parameter input
  const renderParameterInput = useCallback((param: QueryParameter) => {
    const baseProps = {
      fullWidth: true,
      size: 'small' as const,
      label: param.name,
      helperText: param.description,
      required: param.required
    };
    
    switch (param.type) {
      case 'date':
        return (
          <TextField
            {...baseProps}
            type="date"
            value={param.value || ''}
            onChange={(e) => handleParameterChange(param.name, e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
        );
      
      case 'number':
        return (
          <TextField
            {...baseProps}
            type="number"
            value={param.value || ''}
            onChange={(e) => handleParameterChange(param.name, parseFloat(e.target.value) || '')}
          />
        );
      
      case 'boolean':
        return (
          <FormControl {...baseProps}>
            <InputLabel>{param.name}</InputLabel>
            <Select
              value={param.value || false}
              onChange={(e) => handleParameterChange(param.name, e.target.value)}
              label={param.name}
            >
              <MenuItem value="true">True</MenuItem>
              <MenuItem value="false">False</MenuItem>
            </Select>
          </FormControl>
        );
      
      default:
        return (
          <TextField
            {...baseProps}
            value={param.value || ''}
            onChange={(e) => handleParameterChange(param.name, e.target.value)}
          />
        );
    }
  }, [handleParameterChange]);
  
  if (!currentQuery) {
    return (
      <Card>
        <CardContent>
          <Typography variant="body1" color="text.secondary" align="center">
            No query selected for testing
          </Typography>
        </CardContent>
      </Card>
    );
  }
  
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Header */}
      <Card sx={{ mb: 2 }}>
        <CardHeader
          title={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <PlayArrow />
              <Typography variant="h6" component="h3">
                Query Tester
              </Typography>
              <Chip label={currentQuery.queryName} size="small" />
            </Box>
          }
          action={
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button
                startIcon={isExecuting ? <Stop /> : <PlayArrow />}
                onClick={handleExecute}
                disabled={isExecuting || parameters.some(p => p.required && !p.value)}
                variant="contained"
                color="primary"
              >
                {isExecuting ? 'Executing...' : 'Execute Query'}
              </Button>
              
              {queryResults && (
                <Button
                  startIcon={<GetApp />}
                  onClick={exportResults}
                  variant="outlined"
                >
                  Export CSV
                </Button>
              )}
            </Box>
          }
        />
        
        {/* Parameters Section */}
        {parameters.length > 0 && (
          <CardContent sx={{ pt: 0 }}>
            <Accordion expanded={showParameters} onChange={() => setShowParameters(!showParameters)}>
              <AccordionSummary expandIcon={<ExpandMore />}>
                <Typography variant="subtitle1">
                  Query Parameters ({parameters.length})
                </Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Grid container spacing={2}>
                  {parameters.map(param => (
                    <Grid item xs={12} sm={6} md={4} key={param.name}>
                      {renderParameterInput(param)}
                    </Grid>
                  ))}
                </Grid>
              </AccordionDetails>
            </Accordion>
          </CardContent>
        )}
        
        {/* Execution Progress */}
        {isExecuting && (
          <Box sx={{ px: 2, pb: 2 }}>
            <LinearProgress />
            <Typography variant="caption" color="text.secondary" sx={{ mt: 1 }}>
              Executing query...
            </Typography>
          </Box>
        )}
      </Card>
      
      {/* Results Section */}
      <Card sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <Tabs
          value={activeTab}
          onChange={(_, newValue) => setActiveTab(newValue)}
          aria-label="Query results tabs"
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          <Tab
            label={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <TableChart />
                Results
                {queryResults && (
                  <Chip label={queryResults.length} size="small" />
                )}
              </Box>
            }
            id="tester-tab-0"
            aria-controls="tester-tabpanel-0"
          />
          <Tab
            label={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Assessment />
                Execution Details
              </Box>
            }
            id="tester-tab-1"
            aria-controls="tester-tabpanel-1"
          />
          {showHistory && (
            <Tab
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <History />
                  History
                  {executionHistory.length > 0 && (
                    <Chip label={executionHistory.length} size="small" />
                  )}
                </Box>
              }
              id="tester-tab-2"
              aria-controls="tester-tabpanel-2"
            />
          )}
        </Tabs>
        
        {/* Results Tab */}
        <TabPanel value={activeTab} index={0}>
          <Box sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
            {error && (
              <Alert severity="error" onClose={clearError} sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}
            
            {lastResponse && lastResponse.executionStatus !== 'SUCCESS' && (
              <Alert severity="error" sx={{ mb: 2 }}>
                Query execution failed: {lastResponse.errorInfo?.errorMessage || 'Unknown error'}
              </Alert>
            )}
            
            {queryResults ? (
              <>
                {/* Results Header */}
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Typography variant="h6">
                      Results ({processedResults.length} rows)
                    </Typography>
                    {executionTime > 0 && (
                      <Chip
                        icon={<Timer />}
                        label={`${executionTime.toFixed(2)}ms`}
                        size="small"
                        color="info"
                      />
                    )}
                  </Box>
                  
                  <TextField
                    size="small"
                    placeholder="Filter results..."
                    value={filterValue}
                    onChange={(e) => setFilterValue(e.target.value)}
                    InputProps={{
                      startAdornment: <Search sx={{ color: 'action.active', mr: 1 }} />
                    }}
                    sx={{ width: 250 }}
                  />
                </Box>
                
                {/* Results Table */}
                <TableContainer component={Paper} sx={{ flex: 1, overflow: 'auto' }}>
                  <Table stickyHeader size="small">
                    <TableHead>
                      <TableRow>
                        {columnMetadata?.map((col: ColumnMetadata) => (
                          <TableCell
                            key={col.name}
                            onClick={() => handleSort(col.name)}
                            sx={{ 
                              cursor: 'pointer',
                              fontWeight: 'bold',
                              bgcolor: sortColumn === col.name ? 'action.selected' : 'inherit'
                            }}
                          >
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              {col.name}
                              <Chip
                                label={col.type}
                                size="small"
                                variant="outlined"
                                sx={{ height: 20, fontSize: '0.6rem' }}
                              />
                              {col.dataClassification === 'SENSITIVE' && (
                                <Security fontSize="small" color="error" />
                              )}
                            </Box>
                          </TableCell>
                        )) || Object.keys(queryResults[0] || {}).map(key => (
                          <TableCell
                            key={key}
                            onClick={() => handleSort(key)}
                            sx={{ 
                              cursor: 'pointer',
                              fontWeight: 'bold',
                              bgcolor: sortColumn === key ? 'action.selected' : 'inherit'
                            }}
                          >
                            {key}
                          </TableCell>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {paginatedResults.map((row, index) => (
                        <TableRow key={index} hover>
                          {columnMetadata?.map((col: ColumnMetadata) => (
                            <TableCell key={col.name}>
                              {col.dataClassification === 'SENSITIVE' ? 
                                '***masked***' : 
                                String(row[col.name] || '')
                              }
                            </TableCell>
                          )) || Object.values(row).map((value, idx) => (
                            <TableCell key={idx}>
                              {String(value || '')}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
                
                {/* Pagination */}
                <TablePagination
                  component="div"
                  count={processedResults.length}
                  page={page}
                  onPageChange={(_, newPage) => setPage(newPage)}
                  rowsPerPage={rowsPerPage}
                  onRowsPerPageChange={(e) => setRowsPerPage(parseInt(e.target.value, 10))}
                  rowsPerPageOptions={[25, 50, 100, 250]}
                />
              </>
            ) : (
              <Box sx={{ textAlign: 'center', py: 8 }}>
                <Typography variant="body1" color="text.secondary">
                  No results to display. Execute a query to see results.
                </Typography>
              </Box>
            )}
          </Box>
        </TabPanel>
        
        {/* Execution Details Tab */}
        <TabPanel value={activeTab} index={1}>
          <Box sx={{ p: 2 }}>
            {lastResponse ? (
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="h6" gutterBottom>
                      Execution Summary
                    </Typography>
                    <List dense>
                      <ListItem>
                        <ListItemText
                          primary="Status"
                          secondary={
                            <Chip
                              label={lastResponse.executionStatus}
                              color={lastResponse.executionStatus === 'SUCCESS' ? 'success' : 'error'}
                              size="small"
                            />
                          }
                        />
                      </ListItem>
                      <ListItem>
                        <ListItemText
                          primary="Execution Time"
                          secondary={`${lastResponse.executionTimeMs || 0}ms`}
                        />
                      </ListItem>
                      <ListItem>
                        <ListItemText
                          primary="Row Count"
                          secondary={lastResponse.rowCount || 0}
                        />
                      </ListItem>
                      <ListItem>
                        <ListItemText
                          primary="Executed By"
                          secondary={lastResponse.executedBy || 'Unknown'}
                        />
                      </ListItem>
                      <ListItem>
                        <ListItemText
                          primary="Correlation ID"
                          secondary={lastResponse.correlationId || 'N/A'}
                        />
                      </ListItem>
                    </List>
                  </Paper>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="h6" gutterBottom>
                      Security & Compliance
                    </Typography>
                    <List dense>
                      <ListItem>
                        <ListItemText
                          primary="Security Classification"
                          secondary={
                            <Chip
                              label={lastResponse.securityClassification}
                              color="primary"
                              size="small"
                            />
                          }
                        />
                      </ListItem>
                      <ListItem>
                        <ListItemText
                          primary="Data Classification"
                          secondary={
                            <Chip
                              label={lastResponse.dataClassification}
                              color="secondary"
                              size="small"
                            />
                          }
                        />
                      </ListItem>
                      <ListItem>
                        <ListItemText
                          primary="User Role"
                          secondary={lastResponse.userRole || 'N/A'}
                        />
                      </ListItem>
                    </List>
                  </Paper>
                </Grid>
                
                {lastResponse.errorInfo && (
                  <Grid item xs={12}>
                    <Paper sx={{ p: 2 }}>
                      <Typography variant="h6" color="error" gutterBottom>
                        Error Information
                      </Typography>
                      <Alert severity="error">
                        <Typography variant="body2">
                          <strong>Error Code:</strong> {lastResponse.errorInfo.errorCode}
                        </Typography>
                        <Typography variant="body2">
                          <strong>Message:</strong> {lastResponse.errorInfo.errorMessage}
                        </Typography>
                      </Alert>
                    </Paper>
                  </Grid>
                )}
              </Grid>
            ) : (
              <Box sx={{ textAlign: 'center', py: 8 }}>
                <Typography variant="body1" color="text.secondary">
                  No execution details available. Execute a query to see details.
                </Typography>
              </Box>
            )}
          </Box>
        </TabPanel>
        
        {/* History Tab */}
        {showHistory && (
          <TabPanel value={activeTab} index={2}>
            <Box sx={{ p: 2 }}>
              {executionHistory.length > 0 ? (
                <List>
                  {executionHistory.map((execution: any, index: number) => {
                    const isSaved = savedExecutions.has(execution.correlationId || '');
                    
                    return (
                      <ListItem
                        key={index}
                        sx={{
                          border: 1,
                          borderColor: 'divider',
                          borderRadius: 1,
                          mb: 1
                        }}
                      >
                        <ListItemText
                          primary={
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <Typography variant="subtitle2">
                                {execution.queryName || 'Unnamed Query'}
                              </Typography>
                              <Chip
                                label={execution.executionStatus}
                                color={execution.executionStatus === 'SUCCESS' ? 'success' : 'error'}
                                size="small"
                              />
                              {execution.executionTimeMs && (
                                <Chip
                                  icon={<Timer />}
                                  label={`${execution.executionTimeMs}ms`}
                                  size="small"
                                  color="info"
                                />
                              )}
                            </Box>
                          }
                          secondary={
                            <Typography variant="body2" color="text.secondary">
                              Rows: {execution.rowCount || 0} | 
                              User: {execution.executedBy} | 
                              ID: {execution.correlationId}
                            </Typography>
                          }
                        />
                        
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          <Tooltip title={isSaved ? 'Remove bookmark' : 'Bookmark execution'}>
                            <IconButton
                              size="small"
                              onClick={() => toggleSavedExecution(execution.correlationId || '')}
                            >
                              {isSaved ? <Bookmark color="primary" /> : <BookmarkBorder />}
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </ListItem>
                    );
                  })}
                </List>
              ) : (
                <Box sx={{ textAlign: 'center', py: 8 }}>
                  <Typography variant="body1" color="text.secondary">
                    No execution history available.
                  </Typography>
                </Box>
              )}
            </Box>
          </TabPanel>
        )}
      </Card>
    </Box>
  );
};

export default QueryTesterComponent;