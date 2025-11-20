// src/components/masterQuery/QueryColumnAnalyzer.tsx
/**
 * Query Column Analyzer Component - Column Metadata Analysis & Insights
 * Features: Detailed column analysis, Data profiling, Security assessment
 */

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Grid,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Alert,
  LinearProgress,
  Tooltip,
  IconButton,
  Button,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tabs,
  Tab,
  Badge,
  CircularProgress,
  Divider
} from '@mui/material';
import {
  Assessment,
  Security,
  Storage,
  Visibility,
  ExpandMore,
  Info,
  Warning,
  Error,
  CheckCircle,
  TrendingUp,
  DataUsage,
  Schema,
  TableChart,
  Code,
  Shield,
  Speed,
  Psychology,
  Settings,
  Download,
  Refresh,
  FilterList,
  Search,
  Sort,
  BarChart,
  PieChart,
  Timeline
} from '@mui/icons-material';
import { useMasterQueryContext } from '../../contexts/MasterQueryContext';
import { ColumnMetadata, MasterQueryRequest } from '../../types/masterQuery';

// Component props
interface QueryColumnAnalyzerProps {
  query?: MasterQueryRequest;
  columns?: ColumnMetadata[];
  onColumnSelect?: (column: ColumnMetadata) => void;
  showDataProfiling?: boolean;
  showSecurityAnalysis?: boolean;
  autoAnalyze?: boolean;
}

// Column analysis result interface
interface ColumnAnalysis {
  column: ColumnMetadata;
  insights: {
    dataPattern: string;
    nullabilityRisk: 'LOW' | 'MEDIUM' | 'HIGH';
    securityRisk: 'LOW' | 'MEDIUM' | 'HIGH';
    complianceImpact: string[];
    performanceImpact: 'LOW' | 'MEDIUM' | 'HIGH';
    businessCriticality: 'LOW' | 'MEDIUM' | 'HIGH';
  };
  recommendations: string[];
  warnings: string[];
}

// Data type classification
const DATA_TYPE_CATEGORIES = {
  'VARCHAR2': 'String',
  'CHAR': 'String',
  'CLOB': 'Text',
  'NUMBER': 'Numeric',
  'INTEGER': 'Numeric',
  'DECIMAL': 'Numeric',
  'DATE': 'Date/Time',
  'TIMESTAMP': 'Date/Time',
  'BOOLEAN': 'Boolean',
  'BLOB': 'Binary'
};

// Security risk levels
const SECURITY_RISK_COLORS = {
  'LOW': '#4caf50',
  'MEDIUM': '#ff9800',
  'HIGH': '#f44336'
};

// Business criticality colors
const CRITICALITY_COLORS = {
  'LOW': '#9e9e9e',
  'MEDIUM': '#2196f3',
  'HIGH': '#e91e63'
};

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
      id={`analyzer-tabpanel-${index}`}
      aria-labelledby={`analyzer-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 2 }}>{children}</Box>}
    </div>
  );
}

export const QueryColumnAnalyzer: React.FC<QueryColumnAnalyzerProps> = ({
  query,
  columns: propColumns,
  onColumnSelect,
  showDataProfiling = true,
  showSecurityAnalysis = true,
  autoAnalyze = true
}) => {
  // Context state
  const {
    getColumnMetadata,
    columnMetadata,
    isLoadingMetadata,
    error,
    clearError
  } = useMasterQueryContext();
  
  // Local state
  const [activeTab, setActiveTab] = useState(0);
  const [currentColumns, setCurrentColumns] = useState<ColumnMetadata[]>(propColumns || []);
  const [columnAnalyses, setColumnAnalyses] = useState<ColumnAnalysis[]>([]);
  const [selectedColumn, setSelectedColumn] = useState<ColumnMetadata | null>(null);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [filterType, setFilterType] = useState<string>('ALL');
  const [filterSecurity, setFilterSecurity] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'name' | 'type' | 'risk' | 'order'>('order');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [analysisStats, setAnalysisStats] = useState({
    totalColumns: 0,
    sensitiveColumns: 0,
    highRiskColumns: 0,
    nullableColumns: 0,
    indexableColumns: 0
  });
  
  // Update columns when props change
  useEffect(() => {
    if (propColumns) {
      setCurrentColumns(propColumns);
    } else if (columnMetadata) {
      setCurrentColumns(columnMetadata);
    }
  }, [propColumns, columnMetadata]);
  
  // Auto-analyze when columns change
  useEffect(() => {
    if (currentColumns.length > 0 && autoAnalyze) {
      performColumnAnalysis();
    }
  }, [currentColumns, autoAnalyze]);
  
  // Load metadata if query is provided
  useEffect(() => {
    if (query && !propColumns && autoAnalyze) {
      getColumnMetadata(query);
    }
  }, [query, propColumns, autoAnalyze, getColumnMetadata]);
  
  // Perform column analysis
  const performColumnAnalysis = useCallback(() => {
    const analyses: ColumnAnalysis[] = currentColumns.map(column => {
      const analysis = analyzeColumn(column);
      return analysis;
    });
    
    setColumnAnalyses(analyses);
    
    // Calculate statistics
    const stats = {
      totalColumns: analyses.length,
      sensitiveColumns: analyses.filter(a => a.column.dataClassification === 'SENSITIVE').length,
      highRiskColumns: analyses.filter(a => a.insights.securityRisk === 'HIGH').length,
      nullableColumns: analyses.filter(a => a.column.nullable === true).length,
      indexableColumns: analyses.filter(a => shouldBeIndexed(a.column)).length
    };
    
    setAnalysisStats(stats);
  }, [currentColumns]);
  
  // Analyze individual column
  const analyzeColumn = useCallback((column: ColumnMetadata): ColumnAnalysis => {
    const insights = {
      dataPattern: detectDataPattern(column),
      nullabilityRisk: assessNullabilityRisk(column),
      securityRisk: assessSecurityRisk(column),
      complianceImpact: getComplianceImpact(column),
      performanceImpact: assessPerformanceImpact(column),
      businessCriticality: assessBusinessCriticality(column)
    };
    
    const recommendations = generateRecommendations(column, insights);
    const warnings = generateWarnings(column, insights);
    
    return {
      column,
      insights,
      recommendations,
      warnings
    };
  }, []);
  
  // Detect data pattern
  const detectDataPattern = useCallback((column: ColumnMetadata): string => {
    const name = column.name.toLowerCase();
    
    if (name.includes('id') || name.includes('key')) {
      return 'Identifier';
    }
    if (name.includes('date') || name.includes('time')) {
      return 'Temporal';
    }
    if (name.includes('amount') || name.includes('balance') || name.includes('total')) {
      return 'Monetary';
    }
    if (name.includes('name') || name.includes('desc')) {
      return 'Descriptive';
    }
    if (name.includes('code') || name.includes('type')) {
      return 'Categorical';
    }
    if (name.includes('flag') || name.includes('status')) {
      return 'Status';
    }
    
    return DATA_TYPE_CATEGORIES[column.type as keyof typeof DATA_TYPE_CATEGORIES] || 'Generic';
  }, []);
  
  // Assess nullability risk
  const assessNullabilityRisk = useCallback((column: ColumnMetadata): 'LOW' | 'MEDIUM' | 'HIGH' => {
    if (!column.nullable) return 'LOW';
    
    const name = column.name.toLowerCase();
    if (name.includes('id') || name.includes('key')) {
      return 'HIGH'; // Identifiers should rarely be null
    }
    if (name.includes('amount') || name.includes('balance')) {
      return 'MEDIUM'; // Financial values need careful null handling
    }
    
    return 'LOW';
  }, []);
  
  // Assess security risk
  const assessSecurityRisk = useCallback((column: ColumnMetadata): 'LOW' | 'MEDIUM' | 'HIGH' => {
    if (column.dataClassification === 'SENSITIVE') {
      return 'HIGH';
    }
    
    const name = column.name.toLowerCase();
    const criticalPatterns = ['account', 'customer', 'ssn', 'tax', 'credit', 'routing'];
    
    if (criticalPatterns.some(pattern => name.includes(pattern))) {
      return 'HIGH';
    }
    
    if (column.dataClassification === 'INTERNAL') {
      return 'MEDIUM';
    }
    
    return 'LOW';
  }, []);
  
  // Get compliance impact
  const getComplianceImpact = useCallback((column: ColumnMetadata): string[] => {
    const impacts: string[] = [];
    const name = column.name.toLowerCase();
    
    if (column.dataClassification === 'SENSITIVE') {
      impacts.push('PCI_DSS', 'PII_PROTECTION');
    }
    
    if (name.includes('account') || name.includes('balance')) {
      impacts.push('SOX', 'BASEL_III');
    }
    
    if (name.includes('customer') || name.includes('person')) {
      impacts.push('GDPR', 'CCPA');
    }
    
    return impacts;
  }, []);
  
  // Assess performance impact
  const assessPerformanceImpact = useCallback((column: ColumnMetadata): 'LOW' | 'MEDIUM' | 'HIGH' => {
    if (column.type === 'CLOB' || column.type === 'BLOB') {
      return 'HIGH'; // Large objects impact performance
    }
    
    if (column.length && column.length > 1000) {
      return 'MEDIUM'; // Long strings can impact performance
    }
    
    return 'LOW';
  }, []);
  
  // Assess business criticality
  const assessBusinessCriticality = useCallback((column: ColumnMetadata): 'LOW' | 'MEDIUM' | 'HIGH' => {
    const name = column.name.toLowerCase();
    const criticalPatterns = ['account', 'balance', 'amount', 'customer', 'transaction'];
    
    if (criticalPatterns.some(pattern => name.includes(pattern))) {
      return 'HIGH';
    }
    
    if (name.includes('id') || name.includes('key') || name.includes('date')) {
      return 'MEDIUM';
    }
    
    return 'LOW';
  }, []);
  
  // Generate recommendations
  const generateRecommendations = useCallback((column: ColumnMetadata, insights: ColumnAnalysis['insights']): string[] => {
    const recommendations: string[] = [];
    
    if (insights.securityRisk === 'HIGH') {
      recommendations.push('Consider data masking or encryption for this sensitive column');
    }
    
    if (insights.nullabilityRisk === 'HIGH') {
      recommendations.push('Add NOT NULL constraint or default value handling');
    }
    
    if (insights.performanceImpact === 'HIGH') {
      recommendations.push('Consider indexing strategy for large data types');
    }
    
    if (shouldBeIndexed(column)) {
      recommendations.push('This column appears to be a good candidate for indexing');
    }
    
    if (insights.complianceImpact.length > 0) {
      recommendations.push('Ensure compliance controls are in place for regulatory requirements');
    }
    
    return recommendations;
  }, []);
  
  // Generate warnings
  const generateWarnings = useCallback((column: ColumnMetadata, insights: ColumnAnalysis['insights']): string[] => {
    const warnings: string[] = [];
    
    if (insights.securityRisk === 'HIGH' && !column.dataClassification) {
      warnings.push('High-risk column lacks proper data classification');
    }
    
    if (column.nullable && insights.businessCriticality === 'HIGH') {
      warnings.push('Business-critical column allows NULL values');
    }
    
    if (insights.performanceImpact === 'HIGH') {
      warnings.push('Column may cause performance issues in large datasets');
    }
    
    return warnings;
  }, []);
  
  // Check if column should be indexed
  const shouldBeIndexed = useCallback((column: ColumnMetadata): boolean => {
    const name = column.name.toLowerCase();
    return name.includes('id') || name.includes('key') || name.includes('code');
  }, []);
  
  // Filter and sort columns
  const filteredAndSortedColumns = useMemo(() => {
    let filtered = columnAnalyses;
    
    // Apply filters
    if (filterType !== 'ALL') {
      filtered = filtered.filter(analysis => 
        DATA_TYPE_CATEGORIES[analysis.column.type as keyof typeof DATA_TYPE_CATEGORIES] === filterType
      );
    }
    
    if (filterSecurity !== 'ALL') {
      filtered = filtered.filter(analysis => 
        analysis.insights.securityRisk === filterSecurity
      );
    }
    
    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filtered = filtered.filter(analysis =>
        analysis.column.name.toLowerCase().includes(search) ||
        analysis.column.description?.toLowerCase().includes(search) ||
        analysis.insights.dataPattern.toLowerCase().includes(search)
      );
    }
    
    // Apply sorting
    filtered.sort((a, b) => {
      let compareValue = 0;
      
      switch (sortBy) {
        case 'name':
          compareValue = a.column.name.localeCompare(b.column.name);
          break;
        case 'type':
          compareValue = a.column.type.localeCompare(b.column.type);
          break;
        case 'risk':
          const riskValues = { 'LOW': 1, 'MEDIUM': 2, 'HIGH': 3 };
          compareValue = riskValues[a.insights.securityRisk] - riskValues[b.insights.securityRisk];
          break;
        case 'order':
          compareValue = a.column.order - b.column.order;
          break;
      }
      
      return sortDirection === 'desc' ? -compareValue : compareValue;
    });
    
    return filtered;
  }, [columnAnalyses, filterType, filterSecurity, searchTerm, sortBy, sortDirection]);
  
  // Handle column selection
  const handleColumnSelect = useCallback((column: ColumnMetadata) => {
    setSelectedColumn(column);
    setDetailDialogOpen(true);
    
    if (onColumnSelect) {
      onColumnSelect(column);
    }
  }, [onColumnSelect]);
  
  // Export analysis results
  const handleExportAnalysis = useCallback(() => {
    const exportData = {
      analysis: columnAnalyses,
      statistics: analysisStats,
      metadata: {
        exportedAt: new Date().toISOString(),
        queryId: query?.masterQueryId,
        version: '1.0'
      }
    };
    
    const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `column_analysis_${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    
    URL.revokeObjectURL(url);
  }, [columnAnalyses, analysisStats, query]);
  
  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardHeader
        title={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Assessment color="primary" />
            <Typography variant="h6" component="h3">
              Column Analyzer
            </Typography>
            <Badge badgeContent={analysisStats.totalColumns} color="primary" />
          </Box>
        }
        action={
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              startIcon={<Refresh />}
              onClick={performColumnAnalysis}
              disabled={currentColumns.length === 0}
              variant="outlined"
              size="small"
            >
              Analyze
            </Button>
            
            <Button
              startIcon={<Download />}
              onClick={handleExportAnalysis}
              disabled={columnAnalyses.length === 0}
              variant="outlined"
              size="small"
            >
              Export
            </Button>
          </Box>
        }
      />
      
      {/* Statistics Dashboard */}
      <CardContent sx={{ pb: 1 }}>
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={6} sm={2.4}>
            <Paper sx={{ p: 1, textAlign: 'center' }}>
              <Typography variant="h6" color="primary">
                {analysisStats.totalColumns}
              </Typography>
              <Typography variant="caption">Total Columns</Typography>
            </Paper>
          </Grid>
          <Grid item xs={6} sm={2.4}>
            <Paper sx={{ p: 1, textAlign: 'center' }}>
              <Typography variant="h6" color="error.main">
                {analysisStats.sensitiveColumns}
              </Typography>
              <Typography variant="caption">Sensitive</Typography>
            </Paper>
          </Grid>
          <Grid item xs={6} sm={2.4}>
            <Paper sx={{ p: 1, textAlign: 'center' }}>
              <Typography variant="h6" color="warning.main">
                {analysisStats.highRiskColumns}
              </Typography>
              <Typography variant="caption">High Risk</Typography>
            </Paper>
          </Grid>
          <Grid item xs={6} sm={2.4}>
            <Paper sx={{ p: 1, textAlign: 'center' }}>
              <Typography variant="h6" color="info.main">
                {analysisStats.nullableColumns}
              </Typography>
              <Typography variant="caption">Nullable</Typography>
            </Paper>
          </Grid>
          <Grid item xs={6} sm={2.4}>
            <Paper sx={{ p: 1, textAlign: 'center' }}>
              <Typography variant="h6" color="success.main">
                {analysisStats.indexableColumns}
              </Typography>
              <Typography variant="caption">Indexable</Typography>
            </Paper>
          </Grid>
        </Grid>
        
        {/* Filters */}
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={12} sm={3}>
            <TextField
              fullWidth
              size="small"
              placeholder="Search columns..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              InputProps={{
                startAdornment: <Search sx={{ color: 'action.active', mr: 1 }} />
              }}
            />
          </Grid>
          
          <Grid item xs={12} sm={3}>
            <FormControl fullWidth size="small">
              <InputLabel>Data Type</InputLabel>
              <Select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                label="Data Type"
              >
                <MenuItem value="ALL">All Types</MenuItem>
                {Object.values(DATA_TYPE_CATEGORIES).map(type => (
                  <MenuItem key={type} value={type}>{type}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12} sm={3}>
            <FormControl fullWidth size="small">
              <InputLabel>Security Risk</InputLabel>
              <Select
                value={filterSecurity}
                onChange={(e) => setFilterSecurity(e.target.value)}
                label="Security Risk"
              >
                <MenuItem value="ALL">All Levels</MenuItem>
                <MenuItem value="HIGH">High Risk</MenuItem>
                <MenuItem value="MEDIUM">Medium Risk</MenuItem>
                <MenuItem value="LOW">Low Risk</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12} sm={3}>
            <FormControl fullWidth size="small">
              <InputLabel>Sort By</InputLabel>
              <Select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                label="Sort By"
              >
                <MenuItem value="order">Order</MenuItem>
                <MenuItem value="name">Name</MenuItem>
                <MenuItem value="type">Type</MenuItem>
                <MenuItem value="risk">Risk Level</MenuItem>
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </CardContent>
      
      {/* Main Content */}
      <Tabs
        value={activeTab}
        onChange={(_, newValue) => setActiveTab(newValue)}
        aria-label="Column analyzer tabs"
        sx={{ borderBottom: 1, borderColor: 'divider' }}
      >
        <Tab
          label={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <TableChart />
              Column Details
            </Box>
          }
          id="analyzer-tab-0"
          aria-controls="analyzer-tabpanel-0"
        />
        <Tab
          label={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Security />
              Security Analysis
            </Box>
          }
          id="analyzer-tab-1"
          aria-controls="analyzer-tabpanel-1"
        />
        <Tab
          label={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Speed />
              Performance Impact
            </Box>
          }
          id="analyzer-tab-2"
          aria-controls="analyzer-tabpanel-2"
        />
      </Tabs>
      
      {/* Column Details Tab */}
      <TabPanel value={activeTab} index={0}>
        <Box sx={{ height: 400, overflow: 'auto' }}>
          {error && (
            <Alert severity="error" onClose={clearError} sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          
          {isLoadingMetadata ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : filteredAndSortedColumns.length > 0 ? (
            <TableContainer>
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell><strong>Column</strong></TableCell>
                    <TableCell><strong>Type</strong></TableCell>
                    <TableCell><strong>Pattern</strong></TableCell>
                    <TableCell><strong>Security Risk</strong></TableCell>
                    <TableCell><strong>Business Impact</strong></TableCell>
                    <TableCell><strong>Recommendations</strong></TableCell>
                    <TableCell><strong>Actions</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredAndSortedColumns.map((analysis) => (
                    <TableRow key={analysis.column.name} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                          <Typography variant="body2" fontWeight="bold">
                            {analysis.column.name}
                          </Typography>
                          {analysis.column.description && (
                            <Typography variant="caption" color="text.secondary">
                              {analysis.column.description}
                            </Typography>
                          )}
                        </Box>
                      </TableCell>
                      
                      <TableCell>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                          <Chip
                            label={analysis.column.type}
                            size="small"
                            variant="outlined"
                          />
                          {analysis.column.length && (
                            <Typography variant="caption">
                              Length: {analysis.column.length}
                            </Typography>
                          )}
                          {analysis.column.nullable && (
                            <Chip label="Nullable" size="small" color="warning" />
                          )}
                        </Box>
                      </TableCell>
                      
                      <TableCell>
                        <Chip
                          label={analysis.insights.dataPattern}
                          size="small"
                          color="info"
                        />
                      </TableCell>
                      
                      <TableCell>
                        <Chip
                          label={analysis.insights.securityRisk}
                          size="small"
                          sx={{
                            bgcolor: SECURITY_RISK_COLORS[analysis.insights.securityRisk],
                            color: 'white'
                          }}
                        />
                        {analysis.column.dataClassification && (
                          <Chip
                            label={analysis.column.dataClassification}
                            size="small"
                            sx={{ ml: 0.5 }}
                            color={analysis.column.dataClassification === 'SENSITIVE' ? 'error' : 'default'}
                          />
                        )}
                      </TableCell>
                      
                      <TableCell>
                        <Chip
                          label={analysis.insights.businessCriticality}
                          size="small"
                          sx={{
                            bgcolor: CRITICALITY_COLORS[analysis.insights.businessCriticality],
                            color: 'white'
                          }}
                        />
                      </TableCell>
                      
                      <TableCell>
                        <Box sx={{ maxWidth: 200 }}>
                          {analysis.recommendations.slice(0, 2).map((rec, index) => (
                            <Typography key={index} variant="caption" display="block">
                              • {rec}
                            </Typography>
                          ))}
                          {analysis.recommendations.length > 2 && (
                            <Typography variant="caption" color="primary">
                              +{analysis.recommendations.length - 2} more
                            </Typography>
                          )}
                        </Box>
                      </TableCell>
                      
                      <TableCell>
                        <IconButton
                          size="small"
                          onClick={() => handleColumnSelect(analysis.column)}
                        >
                          <Visibility />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <Typography variant="body1" color="text.secondary" gutterBottom>
                No column data available for analysis
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Load a query or provide column metadata to begin analysis
              </Typography>
            </Box>
          )}
        </Box>
      </TabPanel>
      
      {/* Security Analysis Tab */}
      <TabPanel value={activeTab} index={1}>
        <Box sx={{ height: 400, overflow: 'auto' }}>
          <Grid container spacing={2}>
            {['HIGH', 'MEDIUM', 'LOW'].map(riskLevel => {
              const columnsAtRisk = filteredAndSortedColumns.filter(
                analysis => analysis.insights.securityRisk === riskLevel
              );
              
              return (
                <Grid item xs={12} md={4} key={riskLevel}>
                  <Paper sx={{ p: 2, height: '100%' }}>
                    <Typography
                      variant="h6"
                      gutterBottom
                      sx={{ color: SECURITY_RISK_COLORS[riskLevel as keyof typeof SECURITY_RISK_COLORS] }}
                    >
                      <Shield sx={{ mr: 1, verticalAlign: 'middle' }} />
                      {riskLevel} Risk ({columnsAtRisk.length})
                    </Typography>
                    
                    <List dense>
                      {columnsAtRisk.map(analysis => (
                        <ListItem
                          key={analysis.column.name}
                          onClick={() => handleColumnSelect(analysis.column)}
                          sx={{ cursor: 'pointer', '&:hover': { backgroundColor: 'action.hover' } }}
                        >
                          <ListItemIcon>
                            {analysis.column.dataClassification === 'SENSITIVE' ? (
                              <Security color="error" />
                            ) : (
                              <Schema />
                            )}
                          </ListItemIcon>
                          <ListItemText
                            primary={analysis.column.name}
                            secondary={
                              <Box>
                                <Typography variant="caption" display="block">
                                  {analysis.insights.dataPattern}
                                </Typography>
                                {analysis.insights.complianceImpact.length > 0 && (
                                  <Box sx={{ mt: 0.5 }}>
                                    {analysis.insights.complianceImpact.map(impact => (
                                      <Chip
                                        key={impact}
                                        label={impact}
                                        size="small"
                                        sx={{ mr: 0.5, mb: 0.5, fontSize: '0.6rem', height: 16 }}
                                      />
                                    ))}
                                  </Box>
                                )}
                              </Box>
                            }
                          />
                        </ListItem>
                      ))}
                    </List>
                  </Paper>
                </Grid>
              );
            })}
          </Grid>
        </Box>
      </TabPanel>
      
      {/* Performance Impact Tab */}
      <TabPanel value={activeTab} index={2}>
        <Box sx={{ height: 400, overflow: 'auto' }}>
          <List>
            {filteredAndSortedColumns
              .filter(analysis => analysis.insights.performanceImpact !== 'LOW')
              .map(analysis => (
                <ListItem key={analysis.column.name} sx={{ border: 1, borderColor: 'divider', borderRadius: 1, mb: 1 }}>
                  <ListItemIcon>
                    <Speed color={analysis.insights.performanceImpact === 'HIGH' ? 'error' : 'warning'} />
                  </ListItemIcon>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="subtitle2">
                          {analysis.column.name}
                        </Typography>
                        <Chip
                          label={`${analysis.insights.performanceImpact} Impact`}
                          size="small"
                          color={analysis.insights.performanceImpact === 'HIGH' ? 'error' : 'warning'}
                        />
                      </Box>
                    }
                    secondary={
                      <Box sx={{ mt: 1 }}>
                        <Typography variant="body2" color="text.secondary">
                          Type: {analysis.column.type}
                          {analysis.column.length && ` (Length: ${analysis.column.length})`}
                        </Typography>
                        {analysis.recommendations.length > 0 && (
                          <Box sx={{ mt: 1 }}>
                            <Typography variant="caption" fontWeight="bold">
                              Recommendations:
                            </Typography>
                            {analysis.recommendations.map((rec, index) => (
                              <Typography key={index} variant="caption" display="block" sx={{ ml: 1 }}>
                                • {rec}
                              </Typography>
                            ))}
                          </Box>
                        )}
                      </Box>
                    }
                  />
                </ListItem>
              ))}
          </List>
          
          {filteredAndSortedColumns.filter(analysis => analysis.insights.performanceImpact !== 'LOW').length === 0 && (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <Typography variant="body1" color="text.secondary">
                No significant performance impact detected
              </Typography>
            </Box>
          )}
        </Box>
      </TabPanel>
      
      {/* Column Detail Dialog */}
      <Dialog
        open={detailDialogOpen}
        onClose={() => setDetailDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          Column Analysis: {selectedColumn?.name}
        </DialogTitle>
        <DialogContent>
          {selectedColumn && (
            <Box sx={{ mt: 1 }}>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="h6" gutterBottom>
                      Column Information
                    </Typography>
                    <List dense>
                      <ListItem>
                        <ListItemText primary="Name" secondary={selectedColumn.name} />
                      </ListItem>
                      <ListItem>
                        <ListItemText primary="Type" secondary={selectedColumn.type} />
                      </ListItem>
                      <ListItem>
                        <ListItemText primary="Nullable" secondary={selectedColumn.nullable ? 'Yes' : 'No'} />
                      </ListItem>
                      {selectedColumn.length && (
                        <ListItem>
                          <ListItemText primary="Length" secondary={selectedColumn.length} />
                        </ListItem>
                      )}
                      {selectedColumn.description && (
                        <ListItem>
                          <ListItemText primary="Description" secondary={selectedColumn.description} />
                        </ListItem>
                      )}
                    </List>
                  </Paper>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="h6" gutterBottom>
                      Analysis Results
                    </Typography>
                    {columnAnalyses.find(a => a.column.name === selectedColumn.name) && (
                      <Box>
                        {columnAnalyses.find(a => a.column.name === selectedColumn.name)!.recommendations.length > 0 && (
                          <Alert severity="info" sx={{ mb: 2 }}>
                            <Typography variant="subtitle2" gutterBottom>
                              Recommendations:
                            </Typography>
                            {columnAnalyses.find(a => a.column.name === selectedColumn.name)!.recommendations.map((rec, index) => (
                              <Typography key={index} variant="body2">
                                • {rec}
                              </Typography>
                            ))}
                          </Alert>
                        )}
                        
                        {columnAnalyses.find(a => a.column.name === selectedColumn.name)!.warnings.length > 0 && (
                          <Alert severity="warning">
                            <Typography variant="subtitle2" gutterBottom>
                              Warnings:
                            </Typography>
                            {columnAnalyses.find(a => a.column.name === selectedColumn.name)!.warnings.map((warning, index) => (
                              <Typography key={index} variant="body2">
                                • {warning}
                              </Typography>
                            ))}
                          </Alert>
                        )}
                      </Box>
                    )}
                  </Paper>
                </Grid>
              </Grid>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
};

export default QueryColumnAnalyzer;