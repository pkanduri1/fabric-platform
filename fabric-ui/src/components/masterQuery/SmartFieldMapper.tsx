// src/components/masterQuery/SmartFieldMapper.tsx
/**
 * Smart Field Mapper Component - Banking Domain Intelligence
 * Features: Intelligent field pattern recognition, Banking compliance, Smart suggestions
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
  Button,
  IconButton,
  Tooltip,
  Alert,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  TextField,
  Autocomplete,
  Grid,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemSecondaryAction,
  Switch,
  FormControlLabel,
  Rating,
  LinearProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tabs,
  Tab,
  Badge,
  CircularProgress
} from '@mui/material';
import {
  Psychology,
  Security,
  TrendingUp,
  CheckCircle,
  Warning,
  Error,
  Info,
  ExpandMore,
  AutoFixHigh,
  Visibility,
  VisibilityOff,
  ContentCopy,
  Download,
  Upload,
  Settings,
  Help,
  AccountBalance,
  CreditCard,
  Receipt,
  Person,
  CalendarToday,
  AttachMoney,
  Code,
  Assessment,
  Shield,
  Gavel,
  Speed,
  SaveAlt,
  Refresh
} from '@mui/icons-material';
import { useBankingIntelligence } from '../../contexts/MasterQueryContext';
import { 
  SmartFieldMapping, 
  BankingFieldPattern, 
  ColumnMetadata 
} from '../../types/masterQuery';

// Component props
interface SmartFieldMapperProps {
  columns: ColumnMetadata[];
  onMappingsChange?: (mappings: SmartFieldMapping[]) => void;
  onApplyMappings?: (mappings: SmartFieldMapping[]) => void;
  readOnly?: boolean;
  showConfidenceScores?: boolean;
  autoGenerateOnChange?: boolean;
}

// Field type icons mapping
const FIELD_TYPE_ICONS: Record<string, React.ReactElement> = {
  'ACCOUNT_NUMBER': <AccountBalance color="primary" />,
  'ROUTING_NUMBER': <AccountBalance color="secondary" />,
  'TRANSACTION_ID': <Receipt color="info" />,
  'AMOUNT': <AttachMoney color="success" />,
  'DATE': <CalendarToday color="action" />,
  'CURRENCY': <AttachMoney color="warning" />,
  'CUSTOMER_ID': <Person color="primary" />
};

// Compliance requirement colors
const COMPLIANCE_COLORS: Record<string, string> = {
  'PCI_DSS': '#f44336',
  'PII_PROTECTION': '#e91e63',
  'SOX': '#9c27b0',
  'GDPR': '#673ab7',
  'CCPA': '#3f51b5',
  'BASEL_III': '#2196f3'
};

// Data classification colors
const DATA_CLASSIFICATION_COLORS: Record<string, string> = {
  'SENSITIVE': '#f44336',
  'INTERNAL': '#ff9800',
  'PUBLIC': '#4caf50'
};

// Banking concepts for suggestions
const BANKING_CONCEPTS = [
  'Account Management',
  'Transaction Processing',
  'Customer Information',
  'Risk Assessment',
  'Regulatory Reporting',
  'Payment Processing',
  'Fraud Detection',
  'Credit Analysis',
  'Loan Management',
  'Investment Tracking'
];

// Target field templates
const TARGET_FIELD_TEMPLATES = [
  { category: 'Account', fields: ['account-number', 'account-type', 'account-status', 'account-balance'] },
  { category: 'Transaction', fields: ['transaction-id', 'transaction-type', 'transaction-amount', 'transaction-date'] },
  { category: 'Customer', fields: ['customer-id', 'customer-name', 'customer-type', 'customer-status'] },
  { category: 'Payment', fields: ['payment-id', 'payment-method', 'payment-amount', 'payment-date'] },
  { category: 'Risk', fields: ['risk-score', 'risk-category', 'risk-level', 'risk-assessment-date'] }
];

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
      id={`mapper-tabpanel-${index}`}
      aria-labelledby={`mapper-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 2 }}>{children}</Box>}
    </div>
  );
}

export const SmartFieldMapper: React.FC<SmartFieldMapperProps> = ({
  columns,
  onMappingsChange,
  onApplyMappings,
  readOnly = false,
  showConfidenceScores = true,
  autoGenerateOnChange = true
}) => {
  // Context state
  const {
    generateSmartMappings,
    detectBankingPatterns,
    smartMappings,
    detectedPatterns,
    isAnalyzing
  } = useBankingIntelligence();
  
  // Local state
  const [currentMappings, setCurrentMappings] = useState<SmartFieldMapping[]>([]);
  const [activeTab, setActiveTab] = useState(0);
  const [selectedMapping, setSelectedMapping] = useState<SmartFieldMapping | null>(null);
  const [showAdvancedOptions, setShowAdvancedOptions] = useState(false);
  const [confidenceThreshold, setConfidenceThreshold] = useState(0.7);
  const [autoApplyHighConfidence, setAutoApplyHighConfidence] = useState(true);
  const [showOnlyHighConfidence, setShowOnlyHighConfidence] = useState(false);
  const [customMappings, setCustomMappings] = useState<SmartFieldMapping[]>([]);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [mappingStats, setMappingStats] = useState({
    total: 0,
    highConfidence: 0,
    mediumConfidence: 0,
    lowConfidence: 0,
    sensitive: 0,
    compliant: 0
  });
  
  // Generate mappings when columns change
  useEffect(() => {
    if (columns.length > 0 && autoGenerateOnChange) {
      handleGenerateMappings();
    }
  }, [columns, autoGenerateOnChange]);
  
  // Update current mappings from context
  useEffect(() => {
    if (smartMappings.length > 0) {
      setCurrentMappings(smartMappings);
      calculateStats(smartMappings);
    }
  }, [smartMappings]);
  
  // Calculate mapping statistics
  const calculateStats = useCallback((mappings: SmartFieldMapping[]) => {
    const stats = {
      total: mappings.length,
      highConfidence: mappings.filter(m => m.confidence >= 0.8).length,
      mediumConfidence: mappings.filter(m => m.confidence >= 0.6 && m.confidence < 0.8).length,
      lowConfidence: mappings.filter(m => m.confidence < 0.6).length,
      sensitive: mappings.filter(m => m.dataClassification === 'SENSITIVE').length,
      compliant: mappings.filter(m => m.complianceRequirements && m.complianceRequirements.length > 0).length
    };
    
    setMappingStats(stats);
  }, []);
  
  // Handle generate mappings
  const handleGenerateMappings = useCallback(async () => {
    try {
      await Promise.all([
        generateSmartMappings(columns),
        detectBankingPatterns(columns)
      ]);
    } catch (error) {
      console.error('Failed to generate smart mappings:', error);
    }
  }, [columns, generateSmartMappings, detectBankingPatterns]);
  
  // Handle mapping edit
  const handleEditMapping = useCallback((mapping: SmartFieldMapping) => {
    setSelectedMapping(mapping);
    setEditDialogOpen(true);
  }, []);
  
  // Handle mapping update
  const handleUpdateMapping = useCallback((updatedMapping: SmartFieldMapping) => {
    const newMappings = currentMappings.map(m =>
      m.sourceColumn === updatedMapping.sourceColumn ? updatedMapping : m
    );
    
    setCurrentMappings(newMappings);
    calculateStats(newMappings);
    
    if (onMappingsChange) {
      onMappingsChange(newMappings);
    }
    
    setEditDialogOpen(false);
    setSelectedMapping(null);
  }, [currentMappings, onMappingsChange, calculateStats]);
  
  // Handle mapping removal
  const handleRemoveMapping = useCallback((sourceColumn: string) => {
    const newMappings = currentMappings.filter(m => m.sourceColumn !== sourceColumn);
    setCurrentMappings(newMappings);
    calculateStats(newMappings);
    
    if (onMappingsChange) {
      onMappingsChange(newMappings);
    }
  }, [currentMappings, onMappingsChange, calculateStats]);
  
  // Handle apply all mappings
  const handleApplyMappings = useCallback(() => {
    if (onApplyMappings) {
      onApplyMappings(currentMappings);
    }
  }, [currentMappings, onApplyMappings]);
  
  // Filter mappings based on confidence threshold
  const filteredMappings = useMemo(() => {
    let mappings = currentMappings;
    
    if (showOnlyHighConfidence) {
      mappings = mappings.filter(m => m.confidence >= confidenceThreshold);
    }
    
    return mappings.sort((a, b) => b.confidence - a.confidence);
  }, [currentMappings, showOnlyHighConfidence, confidenceThreshold]);
  
  // Get confidence color
  const getConfidenceColor = useCallback((confidence: number): string => {
    if (confidence >= 0.8) return '#4caf50'; // Green
    if (confidence >= 0.6) return '#ff9800'; // Orange
    return '#f44336'; // Red
  }, []);
  
  // Get confidence label
  const getConfidenceLabel = useCallback((confidence: number): string => {
    if (confidence >= 0.8) return 'High';
    if (confidence >= 0.6) return 'Medium';
    return 'Low';
  }, []);
  
  // Export mappings
  const handleExportMappings = useCallback(() => {
    const exportData = {
      mappings: currentMappings,
      statistics: mappingStats,
      metadata: {
        exportedAt: new Date().toISOString(),
        columnsCount: columns.length,
        version: '1.0'
      }
    };
    
    const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `smart_field_mappings_${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    
    URL.revokeObjectURL(url);
  }, [currentMappings, mappingStats, columns.length]);
  
  // Render mapping row
  const renderMappingRow = useCallback((mapping: SmartFieldMapping) => {
    const confidenceColor = getConfidenceColor(mapping.confidence);
    const confidenceLabel = getConfidenceLabel(mapping.confidence);
    
    return (
      <TableRow key={mapping.sourceColumn} hover>
        <TableCell>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2" fontFamily="monospace">
              {mapping.sourceColumn}
            </Typography>
            {mapping.detectedPattern && (
              <Tooltip title={mapping.detectedPattern.description}>
                {FIELD_TYPE_ICONS[mapping.detectedPattern.fieldType] || <Code />}
              </Tooltip>
            )}
          </Box>
        </TableCell>
        
        <TableCell>
          <Typography variant="body2">
            {mapping.targetField}
          </Typography>
        </TableCell>
        
        {showConfidenceScores && (
          <TableCell>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <LinearProgress
                variant="determinate"
                value={mapping.confidence * 100}
                sx={{
                  width: 60,
                  height: 8,
                  borderRadius: 4,
                  '& .MuiLinearProgress-bar': {
                    backgroundColor: confidenceColor
                  }
                }}
              />
              <Typography
                variant="caption"
                sx={{ color: confidenceColor, fontWeight: 'bold', minWidth: 40 }}
              >
                {(mapping.confidence * 100).toFixed(0)}%
              </Typography>
              <Chip
                label={confidenceLabel}
                size="small"
                sx={{
                  bgcolor: confidenceColor,
                  color: 'white',
                  fontSize: '0.6rem',
                  height: 20
                }}
              />
            </Box>
          </TableCell>
        )}
        
        <TableCell>
          <Chip
            label={mapping.dataClassification}
            size="small"
            sx={{
              bgcolor: DATA_CLASSIFICATION_COLORS[mapping.dataClassification || 'INTERNAL'] || '#9e9e9e',
              color: 'white',
              fontSize: '0.7rem'
            }}
          />
          {mapping.dataClassification === 'SENSITIVE' && (
            <Security fontSize="small" color="error" sx={{ ml: 1 }} />
          )}
        </TableCell>
        
        <TableCell>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
            {mapping.complianceRequirements?.map(req => (
              <Chip
                key={req}
                label={req}
                size="small"
                sx={{
                  bgcolor: COMPLIANCE_COLORS[req as string] || '#757575',
                  color: 'white',
                  fontSize: '0.6rem',
                  height: 18
                }}
              />
            )) || <Typography variant="caption" color="text.secondary">None</Typography>}
          </Box>
        </TableCell>
        
        <TableCell>
          <Typography variant="body2" color="text.secondary">
            {mapping.businessConcept || 'N/A'}
          </Typography>
        </TableCell>
        
        {!readOnly && (
          <TableCell>
            <Box sx={{ display: 'flex', gap: 0.5 }}>
              <Tooltip title="Edit mapping">
                <IconButton
                  size="small"
                  onClick={() => handleEditMapping(mapping)}
                >
                  <Settings />
                </IconButton>
              </Tooltip>
              
              <Tooltip title="Remove mapping">
                <IconButton
                  size="small"
                  onClick={() => handleRemoveMapping(mapping.sourceColumn)}
                  color="error"
                >
                  <Error />
                </IconButton>
              </Tooltip>
            </Box>
          </TableCell>
        )}
      </TableRow>
    );
  }, [
    showConfidenceScores,
    readOnly,
    getConfidenceColor,
    getConfidenceLabel,
    handleEditMapping,
    handleRemoveMapping
  ]);
  
  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardHeader
        title={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Psychology color="primary" />
            <Typography variant="h6" component="h3">
              Smart Field Mapper
            </Typography>
            <Badge badgeContent={mappingStats.total} color="primary" />
          </Box>
        }
        action={
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              startIcon={<AutoFixHigh />}
              onClick={handleGenerateMappings}
              disabled={isAnalyzing || columns.length === 0}
              variant="outlined"
              size="small"
            >
              {isAnalyzing ? 'Analyzing...' : 'Generate'}
            </Button>
            
            <Button
              startIcon={<Download />}
              onClick={handleExportMappings}
              disabled={currentMappings.length === 0}
              variant="outlined"
              size="small"
            >
              Export
            </Button>
            
            {onApplyMappings && (
              <Button
                startIcon={<SaveAlt />}
                onClick={handleApplyMappings}
                disabled={currentMappings.length === 0}
                variant="contained"
                color="primary"
                size="small"
              >
                Apply Mappings
              </Button>
            )}
          </Box>
        }
      />
      
      {/* Statistics Dashboard */}
      <CardContent sx={{ pb: 1 }}>
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={6} sm={3}>
            <Paper sx={{ p: 1, textAlign: 'center' }}>
              <Typography variant="h6" color="primary">
                {mappingStats.total}
              </Typography>
              <Typography variant="caption">Total Mappings</Typography>
            </Paper>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Paper sx={{ p: 1, textAlign: 'center' }}>
              <Typography variant="h6" color="success.main">
                {mappingStats.highConfidence}
              </Typography>
              <Typography variant="caption">High Confidence</Typography>
            </Paper>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Paper sx={{ p: 1, textAlign: 'center' }}>
              <Typography variant="h6" color="error.main">
                {mappingStats.sensitive}
              </Typography>
              <Typography variant="caption">Sensitive Data</Typography>
            </Paper>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Paper sx={{ p: 1, textAlign: 'center' }}>
              <Typography variant="h6" color="warning.main">
                {mappingStats.compliant}
              </Typography>
              <Typography variant="caption">Compliance Required</Typography>
            </Paper>
          </Grid>
        </Grid>
        
        {/* Advanced Options */}
        <Accordion expanded={showAdvancedOptions} onChange={() => setShowAdvancedOptions(!showAdvancedOptions)}>
          <AccordionSummary expandIcon={<ExpandMore />}>
            <Typography variant="subtitle2">Advanced Options</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth size="small">
                  <InputLabel>Confidence Threshold</InputLabel>
                  <Select
                    value={confidenceThreshold}
                    onChange={(e) => setConfidenceThreshold(Number(e.target.value))}
                    label="Confidence Threshold"
                  >
                    <MenuItem value={0.5}>50% - Low</MenuItem>
                    <MenuItem value={0.7}>70% - Medium</MenuItem>
                    <MenuItem value={0.8}>80% - High</MenuItem>
                    <MenuItem value={0.9}>90% - Very High</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                  <FormControlLabel
                    control={
                      <Switch
                        checked={showOnlyHighConfidence}
                        onChange={(e) => setShowOnlyHighConfidence(e.target.checked)}
                        size="small"
                      />
                    }
                    label="Show Only High Confidence"
                  />
                  
                  <FormControlLabel
                    control={
                      <Switch
                        checked={autoApplyHighConfidence}
                        onChange={(e) => setAutoApplyHighConfidence(e.target.checked)}
                        size="small"
                      />
                    }
                    label="Auto Apply High Confidence"
                  />
                </Box>
              </Grid>
            </Grid>
          </AccordionDetails>
        </Accordion>
      </CardContent>
      
      {/* Main Content Tabs */}
      <Tabs
        value={activeTab}
        onChange={(_, newValue) => setActiveTab(newValue)}
        aria-label="Smart mapper tabs"
        sx={{ borderBottom: 1, borderColor: 'divider' }}
      >
        <Tab
          label={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <TrendingUp />
              Smart Mappings
              {filteredMappings.length > 0 && (
                <Chip label={filteredMappings.length} size="small" />
              )}
            </Box>
          }
          id="mapper-tab-0"
          aria-controls="mapper-tabpanel-0"
        />
        <Tab
          label={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Assessment />
              Detected Patterns
              {detectedPatterns.length > 0 && (
                <Chip label={detectedPatterns.length} size="small" />
              )}
            </Box>
          }
          id="mapper-tab-1"
          aria-controls="mapper-tabpanel-1"
        />
        <Tab
          label={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Shield />
              Compliance View
            </Box>
          }
          id="mapper-tab-2"
          aria-controls="mapper-tabpanel-2"
        />
      </Tabs>
      
      {/* Smart Mappings Tab */}
      <TabPanel value={activeTab} index={0}>
        <Box sx={{ height: 400, overflow: 'auto' }}>
          {isAnalyzing ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : filteredMappings.length > 0 ? (
            <TableContainer>
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell><strong>Source Column</strong></TableCell>
                    <TableCell><strong>Target Field</strong></TableCell>
                    {showConfidenceScores && <TableCell><strong>Confidence</strong></TableCell>}
                    <TableCell><strong>Data Classification</strong></TableCell>
                    <TableCell><strong>Compliance</strong></TableCell>
                    <TableCell><strong>Business Concept</strong></TableCell>
                    {!readOnly && <TableCell><strong>Actions</strong></TableCell>}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredMappings.map(renderMappingRow)}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <Typography variant="body1" color="text.secondary" gutterBottom>
                No smart mappings available
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Click "Generate" to analyze columns and create intelligent field mappings
              </Typography>
            </Box>
          )}
        </Box>
      </TabPanel>
      
      {/* Detected Patterns Tab */}
      <TabPanel value={activeTab} index={1}>
        <Box sx={{ height: 400, overflow: 'auto' }}>
          {detectedPatterns.length > 0 ? (
            <List>
              {detectedPatterns.map((pattern, index) => (
                <ListItem key={index} sx={{ border: 1, borderColor: 'divider', borderRadius: 1, mb: 1 }}>
                  <ListItemIcon>
                    {FIELD_TYPE_ICONS[pattern.fieldType] || <Code />}
                  </ListItemIcon>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="subtitle2">
                          {pattern.fieldType.replace('_', ' ')}
                        </Typography>
                        {pattern.maskingRequired && (
                          <Chip label="Masking Required" color="error" size="small" />
                        )}
                      </Box>
                    }
                    secondary={
                      <Box>
                        <Typography variant="body2" color="text.secondary">
                          {pattern.description}
                        </Typography>
                        <Typography variant="caption" color="text.secondary" fontFamily="monospace">
                          Pattern: {pattern.pattern.source}
                        </Typography>
                      </Box>
                    }
                  />
                </ListItem>
              ))}
            </List>
          ) : (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <Typography variant="body1" color="text.secondary">
                No banking patterns detected in the current columns
              </Typography>
            </Box>
          )}
        </Box>
      </TabPanel>
      
      {/* Compliance View Tab */}
      <TabPanel value={activeTab} index={2}>
        <Box sx={{ height: 400, overflow: 'auto' }}>
          {currentMappings.filter(m => m.complianceRequirements && m.complianceRequirements.length > 0).length > 0 ? (
            <Grid container spacing={2}>
              {Object.entries(
                currentMappings
                  .filter(m => m.complianceRequirements && m.complianceRequirements.length > 0)
                  .reduce((acc, mapping) => {
                    mapping.complianceRequirements?.forEach(req => {
                      if (!acc[req]) acc[req] = [];
                      acc[req].push(mapping);
                    });
                    return acc;
                  }, {} as Record<string, SmartFieldMapping[]>)
              ).map(([requirement, mappings]) => (
                <Grid item xs={12} sm={6} key={requirement}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="h6" gutterBottom sx={{ color: COMPLIANCE_COLORS[requirement as string] }}>
                      <Gavel sx={{ mr: 1, verticalAlign: 'middle' }} />
                      {requirement}
                    </Typography>
                    <List dense>
                      {mappings.map(mapping => (
                        <ListItem key={mapping.sourceColumn}>
                          <ListItemText
                            primary={mapping.sourceColumn}
                            secondary={mapping.businessConcept}
                          />
                          <ListItemSecondaryAction>
                            <Chip
                              label={mapping.dataClassification}
                              size="small"
                              color={mapping.dataClassification === 'SENSITIVE' ? 'error' : 'default'}
                            />
                          </ListItemSecondaryAction>
                        </ListItem>
                      ))}
                    </List>
                  </Paper>
                </Grid>
              ))}
            </Grid>
          ) : (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <Typography variant="body1" color="text.secondary">
                No compliance requirements detected for current mappings
              </Typography>
            </Box>
          )}
        </Box>
      </TabPanel>
      
      {/* Edit Mapping Dialog */}
      <Dialog
        open={editDialogOpen}
        onClose={() => setEditDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Edit Field Mapping</DialogTitle>
        <DialogContent>
          {selectedMapping && (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Source Column"
                  value={selectedMapping.sourceColumn}
                  disabled
                  variant="outlined"
                />
              </Grid>
              
              <Grid item xs={12}>
                <Autocomplete
                  options={TARGET_FIELD_TEMPLATES.flatMap(t => t.fields)}
                  value={selectedMapping.targetField}
                  onChange={(_, value) => {
                    if (value && selectedMapping) {
                      setSelectedMapping({
                        ...selectedMapping,
                        targetField: value
                      });
                    }
                  }}
                  renderInput={(params) => (
                    <TextField {...params} label="Target Field" />
                  )}
                />
              </Grid>
              
              <Grid item xs={12}>
                <FormControl fullWidth>
                  <InputLabel>Data Classification</InputLabel>
                  <Select
                    value={selectedMapping.dataClassification}
                    onChange={(e) => {
                      if (selectedMapping) {
                        setSelectedMapping({
                          ...selectedMapping,
                          dataClassification: e.target.value as any
                        });
                      }
                    }}
                    label="Data Classification"
                  >
                    <MenuItem value="PUBLIC">Public</MenuItem>
                    <MenuItem value="INTERNAL">Internal</MenuItem>
                    <MenuItem value="SENSITIVE">Sensitive</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              
              <Grid item xs={12}>
                <Autocomplete
                  options={BANKING_CONCEPTS}
                  value={selectedMapping.businessConcept || ''}
                  onChange={(_, value) => {
                    if (selectedMapping) {
                      setSelectedMapping({
                        ...selectedMapping,
                        businessConcept: value || undefined
                      });
                    }
                  }}
                  renderInput={(params) => (
                    <TextField {...params} label="Business Concept" />
                  )}
                />
              </Grid>
            </Grid>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={() => selectedMapping && handleUpdateMapping(selectedMapping)}
            variant="contained"
          >
            Update
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
};

export default SmartFieldMapper;