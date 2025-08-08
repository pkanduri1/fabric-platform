// src/components/configuration/ConditionalBuilder/ConditionalBuilder.tsx
import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Typography,
  Button,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Grid,
  Chip,
  IconButton,
  Alert,
  Stack
} from '@mui/material';
import {
  ExpandMore,
  Add,
  Delete
} from '@mui/icons-material';
import { Condition, SourceField } from '../../../types/configuration';
import { useSourceSystemsState } from '../../../contexts/ConfigurationContext';

interface ConditionalBuilderProps {
  condition?: Condition;
  sourceFields?: SourceField[];
  onConditionChange?: (condition: Condition) => void;
  onValidate?: (condition: Condition) => Promise<boolean>;
  readOnly?: boolean;
}

const ConditionalBuilder: React.FC<ConditionalBuilderProps> = ({
  condition,
  sourceFields = [],
  onConditionChange,
  readOnly = false
}) => {
  // Initialize with a basic condition structure
  const [localCondition, setLocalCondition] = useState<Condition>({
    ifExpr: '',
    then: '',
    elseExpr: '',
    elseIfExprs: []
  });

  const { sourceFields: contextFields } = useSourceSystemsState();
  const availableFields = sourceFields.length > 0 ? sourceFields : contextFields;

  // Sync with parent condition
  useEffect(() => {
    if (condition) {
      setLocalCondition(condition);
    }
  }, [condition]);

  // Notify parent of changes
  useEffect(() => {
    onConditionChange?.(localCondition);
  }, [localCondition, onConditionChange]);

  const updateCondition = (updates: Partial<Condition>) => {
    console.log('Updating condition:', updates); // Debug log
    setLocalCondition(prev => ({ ...prev, ...updates }));
  };

  const handleExpressionChange = (field: keyof Condition, value: string) => {
    console.log(`Changing ${field} to:`, value); // Debug log
    updateCondition({ [field]: value });
  };

  const addElseIf = () => {
    console.log('Adding ELSE IF'); // Debug log
    const newElseIf: Condition = { ifExpr: '', then: '' };
    const currentElseIfs = localCondition.elseIfExprs || [];
    updateCondition({
      elseIfExprs: [...currentElseIfs, newElseIf]
    });
  };

  const updateElseIf = (index: number, updates: Partial<Condition>) => {
    console.log(`Updating ELSE IF ${index}:`, updates); // Debug log
    const currentElseIfs = [...(localCondition.elseIfExprs || [])];
    currentElseIfs[index] = { ...currentElseIfs[index], ...updates };
    updateCondition({ elseIfExprs: currentElseIfs });
  };

  const removeElseIf = (index: number) => {
    console.log(`Removing ELSE IF ${index}`); // Debug log
    const currentElseIfs = localCondition.elseIfExprs || [];
    updateCondition({
      elseIfExprs: currentElseIfs.filter((_, i) => i !== index)
    });
  };

  const insertFieldReference = (fieldName: string, targetField: keyof Condition) => {
    const currentValue = (localCondition[targetField] as string) || '';
    const newValue = currentValue + `{${fieldName}}`;
    handleExpressionChange(targetField, newValue);
  };

  return (
    <Box sx={{ width: '100%' }}>
      {/* Debug Info */}
      <Alert severity="info" sx={{ mb: 2 }}>
        <Typography variant="caption">
          Debug: ELSE IFs count: {localCondition.elseIfExprs?.length || 0}
        </Typography>
      </Alert>

      {/* Main IF Condition */}
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            IF Condition
          </Typography>
          
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="IF Expression"
                value={localCondition.ifExpr}
                onChange={(e) => handleExpressionChange('ifExpr', e.target.value)}
                placeholder="e.g., {amount} > 1000"
                helperText="Use {fieldName} for field references"
                disabled={readOnly}
                multiline
                rows={2}
              />
            </Grid>
            
            <Grid item xs={12}>
              <Typography variant="caption" color="text.secondary">
                Fields:
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mb: 1 }}>
                {availableFields.slice(0, 6).map((field) => (
                  <Chip
                    key={field.name}
                    label={field.name}
                    size="small"
                    variant="outlined"
                    onClick={() => insertFieldReference(field.name, 'ifExpr')}
                    disabled={readOnly}
                    sx={{ cursor: readOnly ? 'default' : 'pointer' }}
                  />
                ))}
              </Stack>
              
              <Typography variant="caption" color="text.secondary">
                Operators:
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mb: 2 }}>
                {['==', '!=', '>', '<', '>=', '<=', 'CONTAINS', 'IS_EMPTY'].map((op) => (
                  <Chip
                    key={op}
                    label={op}
                    size="small"
                    color="secondary"
                    variant="outlined"
                    onClick={() => {
                      const currentValue = localCondition.ifExpr || '';
                      const newValue = currentValue + ` ${op} `;
                      handleExpressionChange('ifExpr', newValue);
                    }}
                    disabled={readOnly}
                    sx={{ cursor: readOnly ? 'default' : 'pointer' }}
                  />
                ))}
              </Stack>
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="THEN Value"
                value={localCondition.then || ''}
                onChange={(e) => handleExpressionChange('then', e.target.value)}
                placeholder="e.g., HIGH_AMOUNT"
                disabled={readOnly}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* ELSE IF Conditions */}
      {localCondition.elseIfExprs && localCondition.elseIfExprs.length > 0 && (
        <Box sx={{ mb: 2 }}>
          <Typography variant="h6" gutterBottom>
            ELSE IF Conditions
          </Typography>
          
          {localCondition.elseIfExprs.map((elseIf, index) => (
            <Card key={index} variant="outlined" sx={{ mb: 2 }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <Typography variant="subtitle1" sx={{ flexGrow: 1 }}>
                    ELSE IF #{index + 1}
                  </Typography>
                  <IconButton 
                    size="small" 
                    onClick={() => removeElseIf(index)}
                    disabled={readOnly}
                    color="error"
                  >
                    <Delete />
                  </IconButton>
                </Box>

                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      label="IF Expression"
                      value={elseIf.ifExpr || ''}
                      onChange={(e) => updateElseIf(index, { ifExpr: e.target.value })}
                      placeholder="e.g., {amount} > 500"
                      disabled={readOnly}
                      multiline
                      rows={2}
                    />
                  </Grid>
                  
                  <Grid item xs={12}>
                    <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mb: 2 }}>
                      {availableFields.slice(0, 6).map((field) => (
                        <Chip
                          key={field.name}
                          label={field.name}
                          size="small"
                          variant="outlined"
                          onClick={() => {
                            const currentValue = elseIf.ifExpr || '';
                            const newValue = currentValue + `{${field.name}}`;
                            updateElseIf(index, { ifExpr: newValue });
                          }}
                          disabled={readOnly}
                          sx={{ cursor: readOnly ? 'default' : 'pointer' }}
                        />
                      ))}
                    </Stack>
                  </Grid>

                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      label="THEN Value"
                      value={elseIf.then || ''}
                      onChange={(e) => updateElseIf(index, { then: e.target.value })}
                      placeholder="e.g., MEDIUM_AMOUNT"
                      disabled={readOnly}
                    />
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          ))}
        </Box>
      )}

      {/* ELSE Condition */}
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            ELSE (Default)
          </Typography>
          
          <TextField
            fullWidth
            label="ELSE Value"
            value={localCondition.elseExpr || ''}
            onChange={(e) => handleExpressionChange('elseExpr', e.target.value)}
            placeholder="e.g., LOW_AMOUNT"
            disabled={readOnly}
          />
        </CardContent>
      </Card>

      {/* Actions */}
      <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={addElseIf}
          disabled={readOnly}
        >
          Add ELSE IF
        </Button>
      </Box>
    </Box>
  );
};

export default ConditionalBuilder;