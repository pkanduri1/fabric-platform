/**
 * Dashboard Filters Component
 * 
 * Dialog for configuring dashboard filters including
 * job types, source systems, and status filters.
 */

import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Checkbox,
  FormControlLabel,
  Box
} from '@mui/material';

interface DashboardFiltersProps {
  open: boolean;
  onClose: () => void;
  filters: {
    jobTypes: string[];
    sourceSystems: string[];
    showCompleted: boolean;
  };
  onFiltersChange: (filters: any) => void;
}

export const DashboardFilters: React.FC<DashboardFiltersProps> = ({
  open,
  onClose,
  filters,
  onFiltersChange
}) => {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Dashboard Filters</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <FormControl fullWidth>
            <InputLabel>Job Types</InputLabel>
            <Select
              multiple
              value={filters.jobTypes}
              onChange={(e) => onFiltersChange({ ...filters, jobTypes: e.target.value })}
              label="Job Types"
            >
              <MenuItem value="P327">P327</MenuItem>
              <MenuItem value="ATOCTRAN">ATOCTRAN</MenuItem>
              <MenuItem value="BATCH">Batch Jobs</MenuItem>
            </Select>
          </FormControl>
          
          <FormControl fullWidth>
            <InputLabel>Source Systems</InputLabel>
            <Select
              multiple
              value={filters.sourceSystems}
              onChange={(e) => onFiltersChange({ ...filters, sourceSystems: e.target.value })}
              label="Source Systems"
            >
              <MenuItem value="HR">HR</MenuItem>
              <MenuItem value="MTG">MTG</MenuItem>
              <MenuItem value="ENCORE">ENCORE</MenuItem>
              <MenuItem value="SHAW">SHAW</MenuItem>
            </Select>
          </FormControl>
          
          <FormControlLabel
            control={
              <Checkbox
                checked={filters.showCompleted}
                onChange={(e) => onFiltersChange({ ...filters, showCompleted: e.target.checked })}
              />
            }
            label="Show Completed Jobs"
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={onClose} variant="contained">Apply</Button>
      </DialogActions>
    </Dialog>
  );
};