/**
 * Monitoring Configuration Dialog Component
 * 
 * Dialog for configuring monitoring dashboard settings
 * including refresh intervals, display preferences, and notifications.
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
  FormControlLabel,
  Switch,
  Slider,
  Typography,
  Box,
  Divider
} from '@mui/material';

interface MonitoringConfigDialogProps {
  open: boolean;
  onClose: () => void;
  config: {
    autoRefresh: boolean;
    refreshInterval: number;
    compactView: boolean;
    showPerformanceCharts: boolean;
    soundEnabled: boolean;
  };
  onConfigChange: (config: any) => void;
}

export const MonitoringConfigDialog: React.FC<MonitoringConfigDialogProps> = ({
  open,
  onClose,
  config,
  onConfigChange
}) => {
  const handleConfigUpdate = (key: string, value: any) => {
    onConfigChange({ [key]: value });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Dashboard Settings</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, mt: 1 }}>
          <Box>
            <Typography variant="h6" gutterBottom>
              Refresh Settings
            </Typography>
            <FormControlLabel
              control={
                <Switch
                  checked={config.autoRefresh}
                  onChange={(e) => handleConfigUpdate('autoRefresh', e.target.checked)}
                />
              }
              label="Auto Refresh"
            />
            
            <Box sx={{ mt: 2 }}>
              <Typography gutterBottom>
                Refresh Interval: {config.refreshInterval / 1000}s
              </Typography>
              <Slider
                value={config.refreshInterval}
                onChange={(e, value) => handleConfigUpdate('refreshInterval', value)}
                min={1000}
                max={30000}
                step={1000}
                marks={[
                  { value: 1000, label: '1s' },
                  { value: 5000, label: '5s' },
                  { value: 10000, label: '10s' },
                  { value: 30000, label: '30s' }
                ]}
              />
            </Box>
          </Box>
          
          <Divider />
          
          <Box>
            <Typography variant="h6" gutterBottom>
              Display Settings
            </Typography>
            <FormControlLabel
              control={
                <Switch
                  checked={config.compactView}
                  onChange={(e) => handleConfigUpdate('compactView', e.target.checked)}
                />
              }
              label="Compact View"
            />
            
            <FormControlLabel
              control={
                <Switch
                  checked={config.showPerformanceCharts}
                  onChange={(e) => handleConfigUpdate('showPerformanceCharts', e.target.checked)}
                />
              }
              label="Show Performance Charts"
            />
          </Box>
          
          <Divider />
          
          <Box>
            <Typography variant="h6" gutterBottom>
              Notifications
            </Typography>
            <FormControlLabel
              control={
                <Switch
                  checked={config.soundEnabled}
                  onChange={(e) => handleConfigUpdate('soundEnabled', e.target.checked)}
                />
              }
              label="Alert Sounds"
            />
          </Box>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={onClose} variant="contained">Save</Button>
      </DialogActions>
    </Dialog>
  );
};