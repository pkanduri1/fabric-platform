// src/components/masterQuery/QueryPreviewComponent.tsx
/**
 * Query Preview Component - SQL Syntax Highlighting & Analysis
 * Features: Real-time syntax highlighting, Query validation, Performance analysis
 */

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Typography,
  TextField,
  Button,
  IconButton,
  Tooltip,
  Alert,
  Collapse,
  Chip,
  Grid,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Divider,
  Switch,
  FormControlLabel,
  Paper,
  CircularProgress,
  Tabs,
  Tab,
  useTheme
} from '@mui/material';
import {
  Code,
  ExpandMore,
  PlayArrow,
  CheckCircle,
  Error,
  Warning,
  Info,
  Speed,
  Security,
  Assessment,
  Visibility,
  VisibilityOff,
  ContentCopy,
  Download,
  Fullscreen,
  FullscreenExit,
  FormatAlignLeft,
  BugReport,
  TrendingUp,
  TableChart,
  Schema
} from '@mui/icons-material';
import { useMasterQueryContext } from '../../contexts/MasterQueryContext';
import { MasterQueryRequest, QueryValidationResult, QueryAnalysis } from '../../types/masterQuery';

// SQL Keywords for syntax highlighting
const SQL_KEYWORDS = [
  'SELECT', 'FROM', 'WHERE', 'JOIN', 'INNER', 'LEFT', 'RIGHT', 'OUTER', 'FULL',
  'ON', 'GROUP', 'BY', 'HAVING', 'ORDER', 'ASC', 'DESC', 'LIMIT', 'OFFSET',
  'INSERT', 'UPDATE', 'DELETE', 'CREATE', 'ALTER', 'DROP', 'INDEX', 'TABLE',
  'VIEW', 'FUNCTION', 'PROCEDURE', 'TRIGGER', 'DATABASE', 'SCHEMA',
  'AND', 'OR', 'NOT', 'IN', 'EXISTS', 'BETWEEN', 'LIKE', 'IS', 'NULL',
  'COUNT', 'SUM', 'AVG', 'MIN', 'MAX', 'DISTINCT', 'AS', 'CASE', 'WHEN',
  'THEN', 'ELSE', 'END', 'UNION', 'ALL', 'WITH', 'RECURSIVE'
];

const SQL_FUNCTIONS = [
  'ABS', 'CEIL', 'FLOOR', 'ROUND', 'SQRT', 'POWER', 'MOD',
  'UPPER', 'LOWER', 'LENGTH', 'SUBSTR', 'TRIM', 'LTRIM', 'RTRIM',
  'CONCAT', 'REPLACE', 'COALESCE', 'NULLIF', 'CAST', 'CONVERT',
  'DATE', 'TIMESTAMP', 'NOW', 'SYSDATE', 'TO_DATE', 'TO_CHAR',
  'DECODE', 'NVL', 'NVL2', 'ROWNUM', 'ROW_NUMBER', 'RANK', 'DENSE_RANK'
];

// Component props
interface QueryPreviewComponentProps {
  query?: MasterQueryRequest;
  onQueryChange?: (query: MasterQueryRequest) => void;
  onValidate?: (query: MasterQueryRequest) => void;
  onExecute?: (query: MasterQueryRequest) => void;
  readOnly?: boolean;
  showAnalysis?: boolean;
  maxHeight?: number;
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
      id={`query-tabpanel-${index}`}
      aria-labelledby={`query-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 2 }}>{children}</Box>}
    </div>
  );
}

export const QueryPreviewComponent: React.FC<QueryPreviewComponentProps> = ({
  query,
  onQueryChange,
  onValidate,
  onExecute,
  readOnly = false,
  showAnalysis = true,
  maxHeight = 800
}) => {
  const theme = useTheme();
  
  // Context state
  const {
    validateQuery,
    validationResult,
    isValidating,
    queryAnalysis,
    error,
    clearError
  } = useMasterQueryContext();
  
  // Local state
  const [currentQuery, setCurrentQuery] = useState<MasterQueryRequest>(
    query || {
      masterQueryId: '',
      queryName: '',
      querySql: '',
      securityClassification: 'INTERNAL',
      dataClassification: 'INTERNAL'
    }
  );
  const [sqlContent, setSqlContent] = useState(currentQuery.querySql);
  const [activeTab, setActiveTab] = useState(0);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showLineNumbers, setShowLineNumbers] = useState(true);
  const [autoValidate, setAutoValidate] = useState(true);
  const [syntaxErrors, setSyntaxErrors] = useState<string[]>([]);
  const [highlightedSql, setHighlightedSql] = useState('');
  
  // Update query when prop changes
  useEffect(() => {
    if (query && query !== currentQuery) {
      setCurrentQuery(query);
      setSqlContent(query.querySql);
    }
  }, [query, currentQuery]);
  
  // SQL syntax highlighting
  const highlightSql = useCallback((sql: string): string => {
    if (!sql) return '';
    
    let highlighted = sql;
    
    // Highlight keywords
    SQL_KEYWORDS.forEach(keyword => {
      const regex = new RegExp(`\\b${keyword}\\b`, 'gi');
      highlighted = highlighted.replace(regex, `<span style="color: ${theme.palette.primary.main}; font-weight: bold;">$&</span>`);
    });
    
    // Highlight functions
    SQL_FUNCTIONS.forEach(func => {
      const regex = new RegExp(`\\b${func}\\b(?=\\s*\\()`, 'gi');
      highlighted = highlighted.replace(regex, `<span style="color: ${theme.palette.secondary.main}; font-weight: bold;">$&</span>`);
    });
    
    // Highlight strings
    highlighted = highlighted.replace(/'([^']*)'/g, `<span style="color: #4caf50;">'$1'</span>`);
    
    // Highlight numbers
    highlighted = highlighted.replace(/\b\d+(\.\d+)?\b/g, `<span style="color: #ff9800;">$&</span>`);
    
    // Highlight comments
    highlighted = highlighted.replace(/--.*$/gm, `<span style="color: #9e9e9e; font-style: italic;">$&</span>`);
    highlighted = highlighted.replace(/\/\*[\s\S]*?\*\//g, `<span style="color: #9e9e9e; font-style: italic;">$&</span>`);
    
    return highlighted;
  }, [theme.palette.primary.main, theme.palette.secondary.main]);
  
  // Update highlighted SQL when content changes
  useEffect(() => {
    setHighlightedSql(highlightSql(sqlContent));
  }, [sqlContent, highlightSql]);
  
  // Basic SQL syntax validation
  const validateSqlSyntax = useCallback((sql: string): string[] => {
    const errors: string[] = [];
    
    if (!sql.trim()) {
      errors.push('SQL query cannot be empty');
      return errors;
    }
    
    // Check for basic SQL structure
    const upperSql = sql.toUpperCase();
    if (!upperSql.includes('SELECT') && !upperSql.includes('WITH')) {
      errors.push('Query must contain SELECT or WITH statement');
    }
    
    // Check for balanced parentheses
    const openParens = (sql.match(/\(/g) || []).length;
    const closeParens = (sql.match(/\)/g) || []).length;
    if (openParens !== closeParens) {
      errors.push('Unbalanced parentheses in SQL query');
    }
    
    // Check for balanced quotes
    const singleQuotes = (sql.match(/'/g) || []).length;
    if (singleQuotes % 2 !== 0) {
      errors.push('Unbalanced single quotes in SQL query');
    }
    
    // Check for dangerous operations (since this is read-only)
    const dangerousKeywords = ['DROP', 'DELETE', 'UPDATE', 'INSERT', 'CREATE', 'ALTER', 'TRUNCATE'];
    dangerousKeywords.forEach(keyword => {
      if (upperSql.includes(keyword)) {
        errors.push(`Dangerous operation detected: ${keyword}. Only SELECT and WITH queries are allowed.`);
      }
    });
    
    return errors;
  }, []);
  
  // Handle SQL content change
  const handleSqlChange = useCallback((event: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newSql = event.target.value;
    setSqlContent(newSql);
    
    // Validate syntax
    const errors = validateSqlSyntax(newSql);
    setSyntaxErrors(errors);
    
    // Update current query
    const updatedQuery = { ...currentQuery, querySql: newSql };
    setCurrentQuery(updatedQuery);
    
    // Notify parent
    if (onQueryChange) {
      onQueryChange(updatedQuery);
    }
    
    // Auto-validate if enabled
    if (autoValidate && errors.length === 0 && newSql.trim()) {
      const timeoutId = setTimeout(() => {
        validateQuery(updatedQuery);
      }, 1000);
      
      return () => clearTimeout(timeoutId);
    }
  }, [currentQuery, onQueryChange, autoValidate, validateQuery, validateSqlSyntax]);
  
  // Handle manual validation
  const handleValidate = useCallback(() => {
    if (onValidate) {
      onValidate(currentQuery);
    } else {
      validateQuery(currentQuery);
    }
  }, [currentQuery, onValidate, validateQuery]);
  
  // Handle query execution
  const handleExecute = useCallback(() => {
    if (onExecute) {
      onExecute(currentQuery);
    }
  }, [currentQuery, onExecute]);
  
  // Format SQL
  const formatSql = useCallback(() => {
    // Basic SQL formatting
    let formatted = sqlContent
      .replace(/\s+/g, ' ')
      .replace(/,/g, ',\n  ')
      .replace(/\bFROM\b/gi, '\nFROM')
      .replace(/\bWHERE\b/gi, '\nWHERE')
      .replace(/\bJOIN\b/gi, '\nJOIN')
      .replace(/\bGROUP BY\b/gi, '\nGROUP BY')
      .replace(/\bORDER BY\b/gi, '\nORDER BY')
      .replace(/\bHAVING\b/gi, '\nHAVING')
      .trim();
    
    setSqlContent(formatted);
  }, [sqlContent]);
  
  // Copy SQL to clipboard
  const copySql = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(sqlContent);
      // TODO: Show success notification
    } catch (err) {
      console.error('Failed to copy SQL:', err);
    }
  }, [sqlContent]);
  
  // Get validation status
  const validationStatus = useMemo(() => {
    if (syntaxErrors.length > 0) {
      return { severity: 'error' as const, message: 'Syntax errors detected' };
    }
    
    if (validationResult) {
      if (validationResult.valid) {
        return { severity: 'success' as const, message: 'Query is valid' };
      } else {
        return { severity: 'error' as const, message: 'Validation failed' };
      }
    }
    
    return null;
  }, [syntaxErrors, validationResult]);
  
  // Render line numbers
  const renderLineNumbers = useCallback(() => {
    if (!showLineNumbers) return null;
    
    const lines = sqlContent.split('\n');
    return (
      <Box
        sx={{
          width: 40,
          bgcolor: 'grey.100',
          borderRight: 1,
          borderColor: 'divider',
          py: 1,
          px: 0.5,
          fontFamily: 'monospace',
          fontSize: '0.875rem',
          lineHeight: 1.5,
          color: 'text.secondary',
          userSelect: 'none'
        }}
      >
        {lines.map((_, index) => (
          <div key={index} style={{ textAlign: 'right' }}>
            {index + 1}
          </div>
        ))}
      </Box>
    );
  }, [showLineNumbers, sqlContent]);
  
  return (
    <Card sx={{ height: maxHeight, display: 'flex', flexDirection: 'column' }}>
      <CardHeader
        title={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Code />
            <Typography variant="h6" component="h3">
              SQL Query Preview
            </Typography>
            {currentQuery.queryName && (
              <Chip label={currentQuery.queryName} size="small" />
            )}
          </Box>
        }
        action={
          <Box sx={{ display: 'flex', gap: 1 }}>
            <FormControlLabel
              control={
                <Switch
                  size="small"
                  checked={showLineNumbers}
                  onChange={(e) => setShowLineNumbers(e.target.checked)}
                />
              }
              label="Line Numbers"
              sx={{ mr: 1 }}
            />
            
            <FormControlLabel
              control={
                <Switch
                  size="small"
                  checked={autoValidate}
                  onChange={(e) => setAutoValidate(e.target.checked)}
                />
              }
              label="Auto Validate"
              sx={{ mr: 1 }}
            />
            
            <Tooltip title="Format SQL">
              <IconButton onClick={formatSql} size="small">
                <FormatAlignLeft />
              </IconButton>
            </Tooltip>
            
            <Tooltip title="Copy SQL">
              <IconButton onClick={copySql} size="small">
                <ContentCopy />
              </IconButton>
            </Tooltip>
            
            <Tooltip title={isFullscreen ? 'Exit Fullscreen' : 'Fullscreen'}>
              <IconButton
                onClick={() => setIsFullscreen(!isFullscreen)}
                size="small"
              >
                {isFullscreen ? <FullscreenExit /> : <Fullscreen />}
              </IconButton>
            </Tooltip>
          </Box>
        }
      />
      
      <Tabs
        value={activeTab}
        onChange={(_, newValue) => setActiveTab(newValue)}
        aria-label="Query preview tabs"
        sx={{ borderBottom: 1, borderColor: 'divider' }}
      >
        <Tab label="SQL Editor" id="query-tab-0" aria-controls="query-tabpanel-0" />
        <Tab label="Validation" id="query-tab-1" aria-controls="query-tabpanel-1" />
        {showAnalysis && (
          <Tab label="Analysis" id="query-tab-2" aria-controls="query-tabpanel-2" />
        )}
      </Tabs>
      
      {/* SQL Editor Tab */}
      <TabPanel value={activeTab} index={0}>
        <Box sx={{ display: 'flex', height: 400 }}>
          {renderLineNumbers()}
          
          <Box sx={{ flex: 1, position: 'relative' }}>
            <TextField
              multiline
              fullWidth
              value={sqlContent}
              onChange={handleSqlChange}
              placeholder="Enter your SQL query here..."
              variant="outlined"
              disabled={readOnly}
              sx={{
                '& .MuiOutlinedInput-root': {
                  fontFamily: 'monospace',
                  fontSize: '0.875rem',
                  padding: 0,
                  height: '100%',
                  '& fieldset': { border: 'none' },
                  '& textarea': {
                    height: '100% !important',
                    overflow: 'auto !important',
                    padding: '8px 12px',
                    lineHeight: 1.5,
                    resize: 'none'
                  }
                }
              }}
              aria-label="SQL query editor"
            />
            
            {/* Syntax highlighting overlay (for display purposes) */}
            {highlightedSql && (
              <Box
                sx={{
                  position: 'absolute',
                  top: 8,
                  left: 12,
                  right: 12,
                  bottom: 8,
                  fontFamily: 'monospace',
                  fontSize: '0.875rem',
                  lineHeight: 1.5,
                  pointerEvents: 'none',
                  color: 'transparent',
                  whiteSpace: 'pre-wrap',
                  overflow: 'hidden'
                }}
                dangerouslySetInnerHTML={{ __html: highlightedSql }}
              />
            )}
          </Box>
        </Box>
        
        {/* Action Buttons */}
        <Box sx={{ display: 'flex', gap: 1, mt: 2, justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              startIcon={<CheckCircle />}
              onClick={handleValidate}
              disabled={isValidating || !sqlContent.trim()}
              variant="outlined"
            >
              {isValidating ? 'Validating...' : 'Validate'}
            </Button>
            
            {onExecute && (
              <Button
                startIcon={<PlayArrow />}
                onClick={handleExecute}
                disabled={syntaxErrors.length > 0 || !sqlContent.trim()}
                variant="contained"
                color="primary"
              >
                Execute
              </Button>
            )}
          </Box>
          
          {/* Validation Status */}
          {validationStatus && (
            <Alert
              severity={validationStatus.severity}
              sx={{ alignItems: 'center' }}
              variant="outlined"
            >
              {validationStatus.message}
            </Alert>
          )}
        </Box>
      </TabPanel>
      
      {/* Validation Tab */}
      <TabPanel value={activeTab} index={1}>
        <Box sx={{ maxHeight: 400, overflow: 'auto' }}>
          {/* Syntax Errors */}
          {syntaxErrors.length > 0 && (
            <Alert severity="error" sx={{ mb: 2 }}>
              <Typography variant="subtitle2" gutterBottom>
                Syntax Errors:
              </Typography>
              <List dense>
                {syntaxErrors.map((error, index) => (
                  <ListItem key={index}>
                    <ListItemIcon>
                      <Error color="error" />
                    </ListItemIcon>
                    <ListItemText primary={error} />
                  </ListItem>
                ))}
              </List>
            </Alert>
          )}
          
          {/* Validation Result */}
          {validationResult && (
            <Accordion defaultExpanded>
              <AccordionSummary expandIcon={<ExpandMore />}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  {validationResult.valid ? (
                    <CheckCircle color="success" />
                  ) : (
                    <Error color="error" />
                  )}
                  <Typography variant="h6">
                    Validation Result
                  </Typography>
                </Box>
              </AccordionSummary>
              <AccordionDetails>
                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <Typography variant="body2" color="text.secondary">
                      {validationResult.message}
                    </Typography>
                  </Grid>
                  
                  {validationResult.businessRules && (
                    <Grid item xs={12}>
                      <Typography variant="subtitle2" gutterBottom>
                        Business Rules:
                      </Typography>
                      <List dense>
                        <ListItem>
                          <ListItemIcon>
                            {validationResult.businessRules.complexityCheck ? (
                              <CheckCircle color="success" />
                            ) : (
                              <Error color="error" />
                            )}
                          </ListItemIcon>
                          <ListItemText primary="Complexity Check" />
                        </ListItem>
                        <ListItem>
                          <ListItemIcon>
                            {validationResult.businessRules.parameterConsistency ? (
                              <CheckCircle color="success" />
                            ) : (
                              <Error color="error" />
                            )}
                          </ListItemIcon>
                          <ListItemText primary="Parameter Consistency" />
                        </ListItem>
                      </List>
                    </Grid>
                  )}
                  
                  {validationResult.errors && validationResult.errors.length > 0 && (
                    <Grid item xs={12}>
                      <Typography variant="subtitle2" color="error" gutterBottom>
                        Errors:
                      </Typography>
                      <List dense>
                        {validationResult.errors.map((error, index) => (
                          <ListItem key={index}>
                            <ListItemIcon>
                              <Error color="error" />
                            </ListItemIcon>
                            <ListItemText primary={error} />
                          </ListItem>
                        ))}
                      </List>
                    </Grid>
                  )}
                  
                  {validationResult.warnings && validationResult.warnings.length > 0 && (
                    <Grid item xs={12}>
                      <Typography variant="subtitle2" color="warning.main" gutterBottom>
                        Warnings:
                      </Typography>
                      <List dense>
                        {validationResult.warnings.map((warning, index) => (
                          <ListItem key={index}>
                            <ListItemIcon>
                              <Warning color="warning" />
                            </ListItemIcon>
                            <ListItemText primary={warning} />
                          </ListItem>
                        ))}
                      </List>
                    </Grid>
                  )}
                </Grid>
              </AccordionDetails>
            </Accordion>
          )}
          
          {isValidating && (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          )}
        </Box>
      </TabPanel>
      
      {/* Analysis Tab */}
      {showAnalysis && (
        <TabPanel value={activeTab} index={2}>
          <Box sx={{ maxHeight: 400, overflow: 'auto' }}>
            {queryAnalysis ? (
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="h6" gutterBottom>
                      Query Complexity
                    </Typography>
                    <Chip
                      label={queryAnalysis.complexity}
                      color={
                        queryAnalysis.complexity === 'LOW' ? 'success' :
                        queryAnalysis.complexity === 'MEDIUM' ? 'warning' : 'error'
                      }
                    />
                  </Paper>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="h6" gutterBottom>
                      Performance Estimates
                    </Typography>
                    <Typography variant="body2">
                      Estimated Rows: {queryAnalysis.estimatedRows.toLocaleString()}
                    </Typography>
                    <Typography variant="body2">
                      Estimated Time: {queryAnalysis.estimatedExecutionTime}ms
                    </Typography>
                  </Paper>
                </Grid>
                
                {queryAnalysis.riskFactors.length > 0 && (
                  <Grid item xs={12}>
                    <Paper sx={{ p: 2 }}>
                      <Typography variant="h6" gutterBottom color="error">
                        Risk Factors
                      </Typography>
                      <List dense>
                        {queryAnalysis.riskFactors.map((risk, index) => (
                          <ListItem key={index}>
                            <ListItemIcon>
                              <Warning color="warning" />
                            </ListItemIcon>
                            <ListItemText primary={risk} />
                          </ListItem>
                        ))}
                      </List>
                    </Paper>
                  </Grid>
                )}
                
                {queryAnalysis.recommendations.length > 0 && (
                  <Grid item xs={12}>
                    <Paper sx={{ p: 2 }}>
                      <Typography variant="h6" gutterBottom color="primary">
                        Recommendations
                      </Typography>
                      <List dense>
                        {queryAnalysis.recommendations.map((rec, index) => (
                          <ListItem key={index}>
                            <ListItemIcon>
                              <TrendingUp color="primary" />
                            </ListItemIcon>
                            <ListItemText primary={rec} />
                          </ListItem>
                        ))}
                      </List>
                    </Paper>
                  </Grid>
                )}
              </Grid>
            ) : (
              <Box sx={{ textAlign: 'center', py: 4 }}>
                <Typography variant="body1" color="text.secondary">
                  No analysis available. Execute or validate the query to see analysis.
                </Typography>
              </Box>
            )}
          </Box>
        </TabPanel>
      )}
      
      {/* Error Display */}
      {error && (
        <Alert
          severity="error"
          onClose={clearError}
          sx={{ m: 2 }}
        >
          {error}
        </Alert>
      )}
    </Card>
  );
};

export default QueryPreviewComponent;