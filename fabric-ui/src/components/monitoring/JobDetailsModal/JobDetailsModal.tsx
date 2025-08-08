/**
 * Job Details Modal Component
 * 
 * Modal dialog for displaying detailed job execution information
 * including performance metrics, stages, errors, and audit trail.
 */

import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Tabs,
  Tab,
  CircularProgress,
  Alert
} from '@mui/material';
import { JobDetailsModalProps, JobExecutionDetails } from '../../../types/monitoring';
import { monitoringApi } from '../../../services/api/monitoringApi';

export const JobDetailsModal: React.FC<JobDetailsModalProps> = ({
  executionId,
  open,
  onClose,
  onRefresh
}) => {
  const [tabValue, setTabValue] = useState(0);
  const [jobDetails, setJobDetails] = useState<JobExecutionDetails | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open && executionId) {
      loadJobDetails();
    }
  }, [open, executionId]);

  const loadJobDetails = async () => {
    setLoading(true);
    setError(null);
    try {
      const details = await monitoringApi.getJobDetails(executionId);
      setJobDetails(details);
    } catch (err: any) {
      setError(err.message || 'Failed to load job details');
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>
        Job Details: {jobDetails?.jobName || executionId}
      </DialogTitle>
      <DialogContent>
        {loading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress />
          </Box>
        )}
        
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        
        {jobDetails && (
          <>
            <Tabs value={tabValue} onChange={handleTabChange} sx={{ mb: 2 }}>
              <Tab label="Overview" />
              <Tab label="Performance" />
              <Tab label="Stages" />
              <Tab label="Errors" />
              <Tab label="Audit Trail" />
            </Tabs>
            
            {tabValue === 0 && (
              <Box>
                <Typography variant="body1">
                  Status: {jobDetails.status}
                </Typography>
                <Typography variant="body1">
                  Records Processed: {jobDetails.processedRecords.toLocaleString()}
                </Typography>
                <Typography variant="body1">
                  Total Records: {jobDetails.totalRecords.toLocaleString()}
                </Typography>
                {/* Add more overview details */}
              </Box>
            )}
            
            {tabValue === 1 && (
              <Box>
                <Typography variant="body1">
                  Average Throughput: {jobDetails.performance.averageThroughput}
                </Typography>
                <Typography variant="body1">
                  Peak Throughput: {jobDetails.performance.peakThroughput}
                </Typography>
                {/* Add performance charts */}
              </Box>
            )}
            
            {/* Add other tab content */}
          </>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onRefresh}>Refresh</Button>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};