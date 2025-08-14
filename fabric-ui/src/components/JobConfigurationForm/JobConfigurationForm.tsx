/**
 * Job Configuration Form Component - Phase 3A Core Interface
 * 
 * Enterprise-grade React form component for creating and editing manual job configurations.
 * Features comprehensive validation, Material-UI integration, and banking-grade security controls.
 * 
 * Features:
 * - Complete form validation with real-time feedback
 * - Support for create, edit, and view modes
 * - Integration with react-hook-form for performance and validation
 * - Material-UI components with consistent design system
 * - Role-based field access and modification controls
 * - Secure parameter handling with encryption detection
 * - SOX-compliant audit trail support
 * 
 * Security:
 * - Input sanitization and validation
 * - Sensitive data detection and warning
 * - Role-based field access controls
 * - Parameter encryption recommendations
 * 
 * @author Claude Code
 * @version 1.0
 * @since Phase 3A Implementation
 */

import React, { useEffect, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormHelperText,
  Grid,
  Typography,
  Box,
  Chip,
  Stack,
  Alert,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  IconButton,
  Tooltip,
  Divider,
  Card,
  CardContent,
  FormControlLabel,
  Switch,
  Autocomplete,
  CircularProgress
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  Help as HelpIcon,
  Warning as WarningIcon,
  Security as SecurityIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  Visibility as VisibilityIcon,
  Edit as EditIcon,
  Add as AddIcon,
  Delete as DeleteIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { 
  JobConfigurationRequest, 
  JobConfigurationResponse,
  ManualJobConfigApiService 
} from '../../services/api/manualJobConfigApi';

// Form validation schema
interface FormData extends JobConfigurationRequest {
  // Additional UI-specific fields for parameter management
  parameterEntries: Array<{
    key: string;
    value: any;
    type: 'string' | 'number' | 'boolean' | 'json';
    isSecret: boolean;
  }>;
}

// Component props interface
interface JobConfigurationFormProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (config: JobConfigurationResponse) => void;
  mode: 'create' | 'edit' | 'view';
  initialData?: JobConfigurationResponse | null;
}

// Predefined options for dropdowns
const JOB_TYPES = [
  { value: 'ETL_BATCH', label: 'ETL Batch Processing' },
  { value: 'DATA_MIGRATION', label: 'Data Migration' },
  { value: 'REPORT_GENERATION', label: 'Report Generation' },
  { value: 'FILE_PROCESSING', label: 'File Processing' },
  { value: 'API_SYNC', label: 'API Synchronization' }
];

const PRIORITY_LEVELS = [
  { value: 'LOW', label: 'Low Priority' },
  { value: 'MEDIUM', label: 'Medium Priority' },
  { value: 'HIGH', label: 'High Priority' },
  { value: 'CRITICAL', label: 'Critical Priority' }
];

const SECURITY_CLASSIFICATIONS = [
  { value: 'PUBLIC', label: 'Public' },
  { value: 'INTERNAL', label: 'Internal' },
  { value: 'CONFIDENTIAL', label: 'Confidential' },
  { value: 'RESTRICTED', label: 'Restricted' }
];

const DATA_CLASSIFICATIONS = [
  { value: 'PUBLIC', label: 'Public Data' },
  { value: 'INTERNAL', label: 'Internal Data' },
  { value: 'SENSITIVE', label: 'Sensitive Data' },
  { value: 'HIGHLY_SENSITIVE', label: 'Highly Sensitive Data' }
];

const ENVIRONMENT_OPTIONS = [
  'DEV', 'TEST', 'UAT', 'STAGING', 'PROD'
];

const COMPLIANCE_TAG_OPTIONS = [
  'SOX', 'PCI_DSS', 'BASEL_III', 'GDPR', 'CCPA', 'HIPAA'
];

// Default form values
const DEFAULT_VALUES: Partial<FormData> = {
  priority: 'MEDIUM',
  securityClassification: 'INTERNAL',
  dataClassification: 'INTERNAL',
  expectedRuntimeMinutes: 30,
  maxExecutionTimeMinutes: 180,
  parameterEntries: [{ key: '', value: '', type: 'string', isSecret: false }]
};

export const JobConfigurationForm: React.FC<JobConfigurationFormProps> = React.memo(({
  open,
  onClose,
  onSuccess,
  mode,
  initialData
}) => {
  const { user, hasRole } = useAuth();
  const [loading, setLoading] = useState(false);
  const [sensitiveDataWarning, setSensitiveDataWarning] = useState(false);
  
  // Form setup with react-hook-form
  const {
    control,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isValid, isDirty }
  } = useForm<FormData>({
    defaultValues: DEFAULT_VALUES,
    mode: 'onChange'
  });

  // Watch for changes in parameter entries to detect sensitive data
  const parameterEntries = watch('parameterEntries');

  // Permission checks
  const canModify = mode === 'create' || (mode === 'edit' && hasRole('JOB_MODIFIER'));
  const isReadOnly = mode === 'view' || !canModify;

  // Initialize form with existing data
  useEffect(() => {
    if (initialData && open) {
      // Convert existing data to form format
      const formData: Partial<FormData> = {
        ...initialData,
        jobType: initialData.jobType as FormData['jobType'],
        priority: (initialData.priority as FormData['priority']) || 'MEDIUM',
        securityClassification: (initialData.securityClassification as FormData['securityClassification']) || 'INTERNAL',
        dataClassification: (initialData.dataClassification as FormData['dataClassification']) || 'INTERNAL',
        environmentTargets: (initialData.environmentTargets as FormData['environmentTargets']) || [],
        complianceTags: (initialData.complianceTags as FormData['complianceTags']) || [],
        parameterEntries: Object.entries(initialData.jobParameters || {}).map(([key, value]) => ({
          key,
          value: typeof value === 'object' ? JSON.stringify(value) : value,
          type: (typeof value === 'number' ? 'number' : 
              typeof value === 'boolean' ? 'boolean' :
              typeof value === 'object' ? 'json' : 'string') as 'string' | 'number' | 'boolean' | 'json',
          isSecret: isLikelySensitiveKey(key)
        }))
      };
      reset(formData);
    } else if (mode === 'create' && open) {
      reset(DEFAULT_VALUES);
    }
  }, [initialData, open, mode, reset]);

  // Check for sensitive data in parameter keys
  useEffect(() => {
    const hasSensitiveData = parameterEntries?.some(entry => 
      entry.isSecret || isLikelySensitiveKey(entry.key)
    );
    setSensitiveDataWarning(hasSensitiveData);
  }, [parameterEntries]);

  // Helper function to detect sensitive parameter keys
  const isLikelySensitiveKey = (key: string): boolean => {
    const sensitiveKeywords = ['password', 'secret', 'key', 'token', 'credential', 'auth'];
    return sensitiveKeywords.some(keyword => key.toLowerCase().includes(keyword));
  };

  // Form submission handler
  const onSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      // Convert parameter entries back to object format
      const jobParameters = data.parameterEntries.reduce((acc, entry) => {
        if (entry.key.trim()) {
          let value: any = entry.value;
          
          // Parse value based on type
          switch (entry.type) {
            case 'number':
              value = Number(entry.value);
              break;
            case 'boolean':
              value = Boolean(entry.value);
              break;
            case 'json':
              try {
                value = JSON.parse(entry.value);
              } catch (e) {
                // Keep as string if JSON parsing fails
                value = entry.value;
              }
              break;
            default:
              value = String(entry.value);
          }
          
          acc[entry.key] = value;
        }
        return acc;
      }, {} as Record<string, any>);

      const requestData: JobConfigurationRequest = {
        ...data,
        jobParameters,
        // Remove UI-specific fields
        parameterEntries: undefined
      } as JobConfigurationRequest;

      let response: JobConfigurationResponse;

      if (mode === 'create') {
        response = await ManualJobConfigApiService.createJobConfiguration(requestData);
      } else if (mode === 'edit' && initialData) {
        response = await ManualJobConfigApiService.updateJobConfiguration(
          initialData.configId, 
          requestData
        );
      } else {
        throw new Error('Invalid operation mode');
      }

      onSuccess(response);
      onClose();
    } catch (error: any) {
      console.error(`[JobConfigForm] ${mode} failed:`, error);
      // Error handling would typically show a snackbar or error dialog
      alert(`Failed to ${mode} job configuration: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  // Parameter management handlers
  const addParameter = () => {
    const currentParams = watch('parameterEntries') || [];
    setValue('parameterEntries', [
      ...currentParams,
      { key: '', value: '', type: 'string', isSecret: false }
    ]);
  };

  const removeParameter = (index: number) => {
    const currentParams = watch('parameterEntries') || [];
    setValue('parameterEntries', currentParams.filter((_, i) => i !== index));
  };

  const getDialogTitle = () => {
    switch (mode) {
      case 'create': return 'Create Job Configuration';
      case 'edit': return 'Edit Job Configuration';
      case 'view': return 'View Job Configuration';
      default: return 'Job Configuration';
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="lg"
      fullWidth
      PaperProps={{
        sx: { minHeight: '80vh' }
      }}
    >
      <DialogTitle sx={{ pb: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="h6" component="div">
            {getDialogTitle()}
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {mode === 'view' && <VisibilityIcon color="action" />}
            {mode === 'edit' && <EditIcon color="action" />}
            {mode === 'create' && <AddIcon color="action" />}
          </Box>
        </Box>
        {initialData && (
          <Typography variant="body2" color="text.secondary">
            Config ID: {initialData.configId} | Version: {initialData.versionNumber}
          </Typography>
        )}
      </DialogTitle>

      <form onSubmit={handleSubmit(onSubmit)}>
        <DialogContent dividers sx={{ p: 3 }}>
          {/* Sensitive data warning */}
          {sensitiveDataWarning && (
            <Alert 
              severity="warning" 
              icon={<SecurityIcon />}
              sx={{ mb: 3 }}
            >
              <Typography variant="body2">
                <strong>Security Notice:</strong> Sensitive parameters detected. 
                Consider using encrypted parameter storage for production environments.
              </Typography>
            </Alert>
          )}

          {/* Basic Configuration Section */}
          <Accordion defaultExpanded>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography variant="h6">Basic Configuration</Typography>
            </AccordionSummary>
            <AccordionDetails>
              <Grid container spacing={3}>
                <Grid item xs={12} md={6}>
                  <Controller
                    name="jobName"
                    control={control}
                    rules={{
                      required: 'Job name is required',
                      pattern: {
                        value: /^[A-Z][A-Z0-9_]{2,99}$/,
                        message: 'Job name must start with uppercase letter, contain only uppercase letters, numbers, and underscores (3-100 characters)'
                      }
                    }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Job Name"
                        fullWidth
                        required
                        disabled={isReadOnly}
                        error={!!errors.jobName}
                        helperText={errors.jobName?.message || 'Example: DAILY_TRANSACTION_LOADER'}
                        placeholder="DAILY_TRANSACTION_LOADER"
                      />
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <Controller
                    name="jobType"
                    control={control}
                    rules={{ required: 'Job type is required' }}
                    render={({ field }) => (
                      <FormControl fullWidth required error={!!errors.jobType}>
                        <InputLabel>Job Type</InputLabel>
                        <Select
                          {...field}
                          label="Job Type"
                          disabled={isReadOnly}
                        >
                          {JOB_TYPES.map((type) => (
                            <MenuItem key={type.value} value={type.value}>
                              {type.label}
                            </MenuItem>
                          ))}
                        </Select>
                        <FormHelperText>
                          {errors.jobType?.message || 'Select the type of job processing'}
                        </FormHelperText>
                      </FormControl>
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <Controller
                    name="sourceSystem"
                    control={control}
                    rules={{
                      required: 'Source system is required',
                      pattern: {
                        value: /^[A-Z][A-Z0-9_]{2,49}$/,
                        message: 'Source system must start with uppercase letter (3-50 characters)'
                      }
                    }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Source System"
                        fullWidth
                        required
                        disabled={isReadOnly}
                        error={!!errors.sourceSystem}
                        helperText={errors.sourceSystem?.message || 'Example: CORE_BANKING'}
                        placeholder="CORE_BANKING"
                      />
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <Controller
                    name="targetSystem"
                    control={control}
                    rules={{
                      required: 'Target system is required',
                      pattern: {
                        value: /^[A-Z][A-Z0-9_]{2,49}$/,
                        message: 'Target system must start with uppercase letter (3-50 characters)'
                      }
                    }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Target System"
                        fullWidth
                        required
                        disabled={isReadOnly}
                        error={!!errors.targetSystem}
                        helperText={errors.targetSystem?.message || 'Example: DATA_WAREHOUSE'}
                        placeholder="DATA_WAREHOUSE"
                      />
                    )}
                  />
                </Grid>

                <Grid item xs={12}>
                  <Controller
                    name="description"
                    control={control}
                    rules={{
                      maxLength: {
                        value: 500,
                        message: 'Description cannot exceed 500 characters'
                      }
                    }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Description"
                        fullWidth
                        multiline
                        rows={3}
                        disabled={isReadOnly}
                        error={!!errors.description}
                        helperText={errors.description?.message || 'Brief description of the job configuration'}
                        placeholder="Daily transaction data loader for core banking integration..."
                      />
                    )}
                  />
                </Grid>
              </Grid>
            </AccordionDetails>
          </Accordion>

          {/* Job Parameters Section */}
          <Accordion defaultExpanded sx={{ mt: 2 }}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography variant="h6">Job Parameters</Typography>
              <Tooltip title="Configure job execution parameters">
                <HelpIcon sx={{ ml: 1, fontSize: 20, color: 'text.secondary' }} />
              </Tooltip>
            </AccordionSummary>
            <AccordionDetails>
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  Configure key-value parameters for job execution. Sensitive parameters will be automatically detected.
                </Typography>
              </Box>
              
              <Controller
                name="parameterEntries"
                control={control}
                render={({ field: { value = [] } }) => (
                  <Stack spacing={2}>
                    {value.map((param, index) => (
                      <Card key={index} variant="outlined">
                        <CardContent sx={{ pb: '16px !important' }}>
                          <Grid container spacing={2} alignItems="center">
                            <Grid item xs={12} sm={3}>
                              <TextField
                                label="Parameter Key"
                                value={param.key}
                                onChange={(e) => {
                                  const newParams = [...value];
                                  newParams[index] = { 
                                    ...newParams[index], 
                                    key: e.target.value,
                                    isSecret: isLikelySensitiveKey(e.target.value)
                                  };
                                  setValue('parameterEntries', newParams);
                                }}
                                fullWidth
                                size="small"
                                disabled={isReadOnly}
                                placeholder="batchSize"
                              />
                            </Grid>
                            <Grid item xs={12} sm={3}>
                              <TextField
                                label="Parameter Value"
                                value={param.value}
                                onChange={(e) => {
                                  const newParams = [...value];
                                  newParams[index] = { ...newParams[index], value: e.target.value };
                                  setValue('parameterEntries', newParams);
                                }}
                                fullWidth
                                size="small"
                                disabled={isReadOnly}
                                type={param.isSecret ? 'password' : 'text'}
                                placeholder="1000"
                              />
                            </Grid>
                            <Grid item xs={12} sm={2}>
                              <FormControl fullWidth size="small">
                                <InputLabel>Type</InputLabel>
                                <Select
                                  value={param.type}
                                  onChange={(e) => {
                                    const newParams = [...value];
                                    newParams[index] = { ...newParams[index], type: e.target.value as any };
                                    setValue('parameterEntries', newParams);
                                  }}
                                  label="Type"
                                  disabled={isReadOnly}
                                >
                                  <MenuItem value="string">String</MenuItem>
                                  <MenuItem value="number">Number</MenuItem>
                                  <MenuItem value="boolean">Boolean</MenuItem>
                                  <MenuItem value="json">JSON</MenuItem>
                                </Select>
                              </FormControl>
                            </Grid>
                            <Grid item xs={12} sm={2}>
                              <FormControlLabel
                                control={
                                  <Switch
                                    checked={param.isSecret}
                                    onChange={(e) => {
                                      const newParams = [...value];
                                      newParams[index] = { ...newParams[index], isSecret: e.target.checked };
                                      setValue('parameterEntries', newParams);
                                    }}
                                    disabled={isReadOnly}
                                    size="small"
                                  />
                                }
                                label="Secret"
                              />
                            </Grid>
                            <Grid item xs={12} sm={2}>
                              {!isReadOnly && (
                                <IconButton
                                  onClick={() => removeParameter(index)}
                                  disabled={value.length <= 1}
                                  color="error"
                                  size="small"
                                >
                                  <DeleteIcon />
                                </IconButton>
                              )}
                            </Grid>
                          </Grid>
                        </CardContent>
                      </Card>
                    ))}
                    
                    {!isReadOnly && (
                      <Button
                        onClick={addParameter}
                        variant="outlined"
                        startIcon={<AddIcon />}
                        sx={{ mt: 1 }}
                      >
                        Add Parameter
                      </Button>
                    )}
                  </Stack>
                )}
              />
            </AccordionDetails>
          </Accordion>

          {/* Advanced Settings Section */}
          <Accordion sx={{ mt: 2 }}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography variant="h6">Advanced Settings</Typography>
            </AccordionSummary>
            <AccordionDetails>
              <Grid container spacing={3}>
                <Grid item xs={12} md={4}>
                  <Controller
                    name="priority"
                    control={control}
                    render={({ field }) => (
                      <FormControl fullWidth>
                        <InputLabel>Priority</InputLabel>
                        <Select
                          {...field}
                          label="Priority"
                          disabled={isReadOnly}
                        >
                          {PRIORITY_LEVELS.map((priority) => (
                            <MenuItem key={priority.value} value={priority.value}>
                              {priority.label}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={4}>
                  <Controller
                    name="expectedRuntimeMinutes"
                    control={control}
                    rules={{
                      min: { value: 1, message: 'Runtime must be at least 1 minute' },
                      max: { value: 1440, message: 'Runtime cannot exceed 24 hours' }
                    }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Expected Runtime (minutes)"
                        type="number"
                        fullWidth
                        disabled={isReadOnly}
                        error={!!errors.expectedRuntimeMinutes}
                        helperText={errors.expectedRuntimeMinutes?.message}
                        inputProps={{ min: 1, max: 1440 }}
                      />
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={4}>
                  <Controller
                    name="maxExecutionTimeMinutes"
                    control={control}
                    rules={{
                      min: { value: 1, message: 'Max execution time must be at least 1 minute' },
                      max: { value: 2880, message: 'Max execution time cannot exceed 48 hours' }
                    }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Max Execution Time (minutes)"
                        type="number"
                        fullWidth
                        disabled={isReadOnly}
                        error={!!errors.maxExecutionTimeMinutes}
                        helperText={errors.maxExecutionTimeMinutes?.message}
                        inputProps={{ min: 1, max: 2880 }}
                      />
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <Controller
                    name="securityClassification"
                    control={control}
                    render={({ field }) => (
                      <FormControl fullWidth>
                        <InputLabel>Security Classification</InputLabel>
                        <Select
                          {...field}
                          label="Security Classification"
                          disabled={isReadOnly}
                        >
                          {SECURITY_CLASSIFICATIONS.map((classification) => (
                            <MenuItem key={classification.value} value={classification.value}>
                              {classification.label}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <Controller
                    name="dataClassification"
                    control={control}
                    render={({ field }) => (
                      <FormControl fullWidth>
                        <InputLabel>Data Classification</InputLabel>
                        <Select
                          {...field}
                          label="Data Classification"
                          disabled={isReadOnly}
                        >
                          {DATA_CLASSIFICATIONS.map((classification) => (
                            <MenuItem key={classification.value} value={classification.value}>
                              {classification.label}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <Controller
                    name="environmentTargets"
                    control={control}
                    render={({ field }) => (
                      <Autocomplete
                        {...field}
                        multiple
                        options={ENVIRONMENT_OPTIONS}
                        disabled={isReadOnly}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            label="Environment Targets"
                            placeholder="Select environments"
                          />
                        )}
                        renderTags={(value, getTagProps) =>
                          value.map((option, index) => (
                            <Chip
                              variant="outlined"
                              label={option}
                              {...getTagProps({ index })}
                              key={option}
                            />
                          ))
                        }
                        onChange={(_, value) => field.onChange(value)}
                      />
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <Controller
                    name="complianceTags"
                    control={control}
                    render={({ field }) => (
                      <Autocomplete
                        {...field}
                        multiple
                        options={COMPLIANCE_TAG_OPTIONS}
                        disabled={isReadOnly}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            label="Compliance Tags"
                            placeholder="Select compliance requirements"
                          />
                        )}
                        renderTags={(value, getTagProps) =>
                          value.map((option, index) => (
                            <Chip
                              variant="outlined"
                              label={option}
                              {...getTagProps({ index })}
                              key={option}
                              color="secondary"
                            />
                          ))
                        }
                        onChange={(_, value) => field.onChange(value)}
                      />
                    )}
                  />
                </Grid>
              </Grid>
            </AccordionDetails>
          </Accordion>
        </DialogContent>

        <DialogActions sx={{ p: 2, justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {mode !== 'view' && (
              <Typography variant="caption" color="text.secondary">
                {isDirty ? '• Unsaved changes' : '• No changes'}
              </Typography>
            )}
          </Box>
          
          <Stack direction="row" spacing={1}>
            <Button onClick={onClose} disabled={loading}>
              <CancelIcon sx={{ mr: 1 }} />
              {mode === 'view' ? 'Close' : 'Cancel'}
            </Button>
            
            {!isReadOnly && (
              <Button
                type="submit"
                variant="contained"
                disabled={loading || !isValid || !isDirty}
                startIcon={loading ? <CircularProgress size={20} /> : <SaveIcon />}
              >
                {loading ? 'Saving...' : (mode === 'create' ? 'Create' : 'Update')}
              </Button>
            )}
          </Stack>
        </DialogActions>
      </form>
    </Dialog>
  );
});

// Set display name for debugging
JobConfigurationForm.displayName = 'JobConfigurationForm';

export default JobConfigurationForm;