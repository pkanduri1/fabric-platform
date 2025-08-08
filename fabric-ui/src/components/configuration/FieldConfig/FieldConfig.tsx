// src/components/configuration/FieldConfig/FieldConfig.tsx
// Updated to include ConditionalBuilder integration

import React, { useState, useEffect } from 'react';
import {
  Box,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Typography,
  Button,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Grid,
  Chip,
  IconButton,
  Alert,
  Divider,
  Switch,
  FormControlLabel,
  SelectChangeEvent
} from '@mui/material';
import {
  ExpandMore,
  Add,
  Delete,
  Save,
  Cancel
} from '@mui/icons-material';
import { FieldMapping, CompositeSource, Condition } from '../../../types/configuration';
import { useConfigurationContext, useSourceSystemsState } from '../../../contexts/ConfigurationContext';
import ConditionalBuilder from '../ConditionalBuilder/ConditionalBuilder';

interface FieldConfigProps {
  selectedMapping?: FieldMapping | null;
  onClose?: () => void;
  onSave?: (mapping: FieldMapping) => void;
}

const FieldConfig: React.FC<FieldConfigProps> = ({
  selectedMapping,
  onClose,
  onSave
}) => {
  const [formData, setFormData] = useState<FieldMapping>({
    fieldName: '',
    targetField: '',
    targetPosition: 1,
    length: 10,
    dataType: 'string',
    transformationType: 'source',
    required: false,
    expression: ''
  });
  const [isDirty, setIsDirty] = useState(false);

  const { sourceFields } = useSourceSystemsState();

  useEffect(() => {
    if (selectedMapping) {
      setFormData({ ...selectedMapping });
      setIsDirty(false);
    }
  }, [selectedMapping]);

  const handleInputChange = (field: keyof FieldMapping, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    setIsDirty(true);
  };

  const handleSave = () => {
    if (onSave) {
      onSave(formData);
    }
    setIsDirty(false);
    onClose?.();
  };

  const handleCancel = () => {
    if (selectedMapping) {
      setFormData({ ...selectedMapping });
    }
    setIsDirty(false);
    onClose?.();
  };

  const addCompositeSource = () => {
    const newSources = [...(formData.sources || []), { field: '' }];
    handleInputChange('sources', newSources);
  };

  const updateCompositeSource = (index: number, field: string) => {
    const newSources = [...(formData.sources || [])];
    newSources[index] = { field };
    handleInputChange('sources', newSources);
  };

  const removeCompositeSource = (index: number) => {
    const newSources = [...(formData.sources || [])];
    newSources.splice(index, 1);
    handleInputChange('sources', newSources);
  };

  const handleConditionalChange = (condition: Condition) => {
    // Store the condition - convert single condition to array for storage
    handleInputChange('conditions', [condition]);
  };

  if (!selectedMapping && !formData.fieldName) {
    return (
      <Box sx={{ p: 2, textAlign: 'center' }}>
        <Alert severity="info">
          Select a field mapping to edit its configuration
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 2, height: '100%', overflow: 'auto' }}>
      {/* Header */}
      <Box sx={{ mb: 2 }}>
        <Typography variant="h6">
          {selectedMapping ? 'Edit Field Mapping' : 'New Field Mapping'}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Configure field transformation properties
        </Typography>
      </Box>

      {/* Basic Properties */}
      <Accordion defaultExpanded>
        <AccordionSummary expandIcon={<ExpandMore />}>
          <Typography variant="subtitle1">Basic Properties</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Field Name"
                value={formData.fieldName || ''}
                onChange={(e) => handleInputChange('fieldName', e.target.value)}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Target Field"
                value={formData.targetField || ''}
                onChange={(e) => handleInputChange('targetField', e.target.value)}
                required
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Position"
                type="number"
                value={formData.targetPosition || 1}
                onChange={(e) => handleInputChange('targetPosition', parseInt(e.target.value))}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Length"
                type="number"
                value={formData.length || 10}
                onChange={(e) => handleInputChange('length', parseInt(e.target.value))}
              />
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Data Type</InputLabel>
                <Select
                  value={formData.dataType || 'string'}
                  onChange={(e) => handleInputChange('dataType', e.target.value)}
                  label="Data Type"
                >
                  <MenuItem value="string">String</MenuItem>
                  <MenuItem value="numeric">Numeric</MenuItem>
                  <MenuItem value="date">Date</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Transformation Type</InputLabel>
                <Select
                  value={formData.transformationType || 'source'}
                  onChange={(e) => handleInputChange('transformationType', e.target.value)}
                  label="Transformation Type"
                >
                  <MenuItem value="source">Source Field</MenuItem>
                  <MenuItem value="constant">Constant Value</MenuItem>
                  <MenuItem value="composite">Composite</MenuItem>
                  <MenuItem value="conditional">Conditional</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            {/* Source Field Selection */}
            {formData.transformationType === 'source' && (
              <Grid item xs={12}>
                <FormControl fullWidth>
                  <InputLabel>Source Field</InputLabel>
                  <Select
                    value={formData.sourceField || ''}
                    onChange={(e) => handleInputChange('sourceField', e.target.value)}
                    label="Source Field"
                  >
                    {sourceFields.map(field => (
                      <MenuItem key={field.name} value={field.name}>
                        {field.name} ({field.dataType})
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
            )}

            {/* Constant Value */}
            {formData.transformationType === 'constant' && (
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Default Value"
                  value={formData.defaultValue || ''}
                  onChange={(e) => handleInputChange('defaultValue', e.target.value)}
                />
              </Grid>
            )}

            {/* Composite Sources */}
            {formData.transformationType === 'composite' && (
              <Grid item xs={12}>
                <Box>
                  <Typography variant="subtitle2" gutterBottom>
                    Source Fields
                  </Typography>
                  {(formData.sources || []).map((source, index) => (
                    <Box key={index} sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                      <FormControl fullWidth sx={{ mr: 1 }}>
                        <Select
                          value={source.field}
                          onChange={(e) => updateCompositeSource(index, e.target.value)}
                          displayEmpty
                        >
                          <MenuItem value="">Select field...</MenuItem>
                          {sourceFields.map(field => (
                            <MenuItem key={field.name} value={field.name}>
                              {field.name}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                      <IconButton onClick={() => removeCompositeSource(index)}>
                        <Delete />
                      </IconButton>
                    </Box>
                  ))}
                  <Button
                    startIcon={<Add />}
                    onClick={addCompositeSource}
                    variant="outlined"
                    size="small"
                  >
                    Add Source Field
                  </Button>
                </Box>
              </Grid>
            )}
          </Grid>
        </AccordionDetails>
      </Accordion>

      {/* Conditional Logic Configuration */}
      {formData.transformationType === 'conditional' && (
        <Accordion>
          <AccordionSummary expandIcon={<ExpandMore />}>
            <Typography variant="subtitle1">Conditional Logic</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Box sx={{ width: '100%' }}>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Define IF-THEN-ELSE logic for field transformation
              </Typography>
              
              <ConditionalBuilder
                condition={formData.conditions?.[0]} // For now, use first condition
                sourceFields={sourceFields}
                onConditionChange={(condition) => {
                  // Store as array for future multi-condition support
                  handleConditionalChange(condition);
                }}
                onValidate={async (condition) => {
                  // Add validation logic here
                  return true;
                }}
              />
            </Box>
          </AccordionDetails>
        </Accordion>
      )}

      {/* Advanced Properties */}
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMore />}>
          <Typography variant="subtitle1">Advanced Properties</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Padding</InputLabel>
                <Select
                  value={formData.pad || 'right'}
                  onChange={(e) => handleInputChange('pad', e.target.value)}
                  label="Padding"
                >
                  <MenuItem value="left">Left</MenuItem>
                  <MenuItem value="right">Right</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Pad Character"
                value={formData.padChar || ' '}
                onChange={(e) => handleInputChange('padChar', e.target.value)}
                inputProps={{ maxLength: 1 }}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Format"
                value={formData.format || ''}
                onChange={(e) => handleInputChange('format', e.target.value)}
                helperText="Optional formatting pattern (e.g., date format)"
              />
            </Grid>
          </Grid>
        </AccordionDetails>
      </Accordion>

      {/* Action Buttons */}
      <Box sx={{ display: 'flex', gap: 1, mt: 3, pt: 2, borderTop: 1, borderColor: 'divider' }}>
        <Button
          variant="contained"
          startIcon={<Save />}
          onClick={handleSave}
          disabled={!isDirty}
        >
          Save Changes
        </Button>
        <Button
          variant="outlined"
          startIcon={<Cancel />}
          onClick={handleCancel}
        >
          Cancel
        </Button>
      </Box>
    </Box>
  );
};

export default FieldConfig;
export { FieldConfig }; // Add named export for compatibility