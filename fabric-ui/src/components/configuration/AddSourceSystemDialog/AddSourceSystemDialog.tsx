// src/components/configuration/AddSourceSystemDialog/AddSourceSystemDialog.tsx
import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Grid,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Typography,
  Box,
  Chip,
  IconButton,
  Alert
} from '@mui/material';
import { Add, Delete } from '@mui/icons-material';
import { SourceSystem, JobConfig } from '../../../types/configuration';

interface AddSourceSystemDialogProps {
  open: boolean;
  onClose: () => void;
  onAdd: (sourceSystem: Omit<SourceSystem, 'id'>) => void;
}

export const AddSourceSystemDialog: React.FC<AddSourceSystemDialogProps> = ({
  open,
  onClose,
  onAdd
}) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    systemType: 'oracle',
    inputBasePath: '',
    outputBasePath: '',
    jobs: [] as Omit<JobConfig, 'files'>[]
  });

  const [newJob, setNewJob] = useState({
    name: '',
    description: '',
    multiTxn: false
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleClose = () => {
    // Reset form
    setFormData({
      name: '',
      description: '',
      systemType: 'oracle',
      inputBasePath: '',
      outputBasePath: '',
      jobs: []
    });
    setNewJob({
      name: '',
      description: '',
      multiTxn: false
    });
    setErrors({});
    onClose();
  };

  const handleAddJob = () => {
    if (!newJob.name.trim()) {
      setErrors(prev => ({ ...prev, jobName: 'Job name is required' }));
      return;
    }

    if (formData.jobs.some(job => job.name === newJob.name)) {
      setErrors(prev => ({ ...prev, jobName: 'Job name already exists' }));
      return;
    }

    const jobToAdd: Omit<JobConfig, 'files'> = {
      name: newJob.name,
      sourceSystem: formData.name,
      jobName: newJob.name,
      description: newJob.description,
      multiTxn: newJob.multiTxn
    };

    setFormData(prev => ({
      ...prev,
      jobs: [...prev.jobs, jobToAdd]
    }));

    setNewJob({
      name: '',
      description: '',
      multiTxn: false
    });

    setErrors(prev => ({ ...prev, jobName: '' }));
  };

  const handleRemoveJob = (index: number) => {
    setFormData(prev => ({
      ...prev,
      jobs: prev.jobs.filter((_, i) => i !== index)
    }));
  };

  const handleSubmit = () => {
    const newErrors: Record<string, string> = {};

    // Validation
    if (!formData.name.trim()) {
      newErrors.name = 'Source system name is required';
    }

    if (!formData.description.trim()) {
      newErrors.description = 'Description is required';
    }

    if (formData.jobs.length === 0) {
      newErrors.jobs = 'At least one job is required';
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    // Create the source system
    const newSourceSystem: Omit<SourceSystem, 'id'> = {
      name: formData.name,
      description: formData.description,
      systemType: formData.systemType,
      inputBasePath: formData.inputBasePath,
      outputBasePath: formData.outputBasePath,
      jobs: formData.jobs.map(job => ({
        ...job,
        files: [] // Initialize with empty files array
      })),
      supportedFileTypes: ['CSV', 'EXCEL', 'PIPE_DELIMITED'],
      supportedTransactionTypes: ['default', '200', '900']
    };

    onAdd(newSourceSystem);
    handleClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>Add New Source System</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 1 }}>
          {/* Basic Information */}
          <Grid item xs={12}>
            <Typography variant="h6" gutterBottom>Basic Information</Typography>
          </Grid>
          
          <Grid item xs={6}>
            <TextField
              fullWidth
              label="Source System Name"
              value={formData.name}
              onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
              error={!!errors.name}
              helperText={errors.name}
              required
            />
          </Grid>
          
          <Grid item xs={6}>
            <FormControl fullWidth>
              <InputLabel>System Type</InputLabel>
              <Select
                value={formData.systemType}
                onChange={(e) => setFormData(prev => ({ ...prev, systemType: e.target.value }))}
                label="System Type"
              >
                <MenuItem value="oracle">Oracle Database</MenuItem>
                <MenuItem value="mssql">SQL Server</MenuItem>
                <MenuItem value="mysql">MySQL</MenuItem>
                <MenuItem value="file">File System</MenuItem>
                <MenuItem value="api">REST API</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Description"
              value={formData.description}
              onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
              error={!!errors.description}
              helperText={errors.description}
              multiline
              rows={2}
              required
            />
          </Grid>

          {/* Path Configuration */}
          <Grid item xs={12}>
            <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>Path Configuration</Typography>
          </Grid>
          
          <Grid item xs={6}>
            <TextField
              fullWidth
              label="Input Base Path"
              value={formData.inputBasePath}
              onChange={(e) => setFormData(prev => ({ ...prev, inputBasePath: e.target.value }))}
              placeholder="/path/to/input/files"
            />
          </Grid>
          
          <Grid item xs={6}>
            <TextField
              fullWidth
              label="Output Base Path"
              value={formData.outputBasePath}
              onChange={(e) => setFormData(prev => ({ ...prev, outputBasePath: e.target.value }))}
              placeholder="/path/to/output/files"
            />
          </Grid>

          {/* Jobs Configuration */}
          <Grid item xs={12}>
            <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>Jobs Configuration</Typography>
            {errors.jobs && (
              <Alert severity="error" sx={{ mb: 2 }}>{errors.jobs}</Alert>
            )}
          </Grid>

          {/* Existing Jobs */}
          {formData.jobs.length > 0 && (
            <Grid item xs={12}>
              <Typography variant="subtitle2" gutterBottom>Configured Jobs:</Typography>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
                {formData.jobs.map((job, index) => (
                  <Chip
                    key={index}
                    label={`${job.name} ${job.multiTxn ? '(Multi-Txn)' : ''}`}
                    onDelete={() => handleRemoveJob(index)}
                    deleteIcon={<Delete />}
                    variant="outlined"
                  />
                ))}
              </Box>
            </Grid>
          )}

          {/* Add New Job */}
          <Grid item xs={4}>
            <TextField
              fullWidth
              label="Job Name"
              value={newJob.name}
              onChange={(e) => setNewJob(prev => ({ ...prev, name: e.target.value }))}
              error={!!errors.jobName}
              helperText={errors.jobName}
            />
          </Grid>
          
          <Grid item xs={5}>
            <TextField
              fullWidth
              label="Job Description"
              value={newJob.description}
              onChange={(e) => setNewJob(prev => ({ ...prev, description: e.target.value }))}
            />
          </Grid>
          
          <Grid item xs={2}>
            <FormControl fullWidth>
              <InputLabel>Multi-Txn</InputLabel>
              <Select
                value={newJob.multiTxn ? 'true' : 'false'}
                onChange={(e) => setNewJob(prev => ({ ...prev, multiTxn: e.target.value === 'true' }))}
                label="Multi-Txn"
              >
                <MenuItem value="false">No</MenuItem>
                <MenuItem value="true">Yes</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={1}>
            <Button
              variant="contained"
              onClick={handleAddJob}
              sx={{ height: '56px' }}
              disabled={!newJob.name.trim()}
            >
              <Add />
            </Button>
          </Grid>
        </Grid>
      </DialogContent>
      
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button 
          onClick={handleSubmit} 
          variant="contained"
          disabled={!formData.name || !formData.description || formData.jobs.length === 0}
        >
          Add Source System
        </Button>
      </DialogActions>
    </Dialog>
  );
};