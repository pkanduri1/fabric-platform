/**
 * System Health Bar Component
 * 
 * Displays system health status with visual indicators
 * for database, WebSocket, batch processing, and memory.
 */

import React from 'react';
import { Box, LinearProgress, Typography, Chip, Grid } from '@mui/material';
import { SystemHealth } from '../../../types/monitoring';

interface SystemHealthBarProps {
  health: SystemHealth;
}

export const SystemHealthBar: React.FC<SystemHealthBarProps> = ({ health }) => {
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'HEALTHY': return 'success';
      case 'DEGRADED': return 'warning';
      case 'DOWN': return 'error';
      default: return 'default';
    }
  };

  return (
    <Box sx={{ p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6" sx={{ mr: 2 }}>
          System Health: {health.overallScore}%
        </Typography>
        <LinearProgress
          variant="determinate"
          value={health.overallScore}
          sx={{ flex: 1, height: 8, borderRadius: 4 }}
          color={health.overallScore >= 80 ? 'success' : health.overallScore >= 60 ? 'warning' : 'error'}
        />
      </Box>
      <Grid container spacing={2}>
        <Grid item xs={3}>
          <Chip
            label={`DB: ${health.database.status}`}
            color={getStatusColor(health.database.status) as any}
            size="small"
          />
        </Grid>
        <Grid item xs={3}>
          <Chip
            label={`WS: ${health.webSocket.status}`}
            color={getStatusColor(health.webSocket.status) as any}
            size="small"
          />
        </Grid>
        <Grid item xs={3}>
          <Chip
            label={`Batch: ${health.batchProcessing.status}`}
            color={getStatusColor(health.batchProcessing.status) as any}
            size="small"
          />
        </Grid>
        <Grid item xs={3}>
          <Chip
            label={`Mem: ${health.memory.percentage}%`}
            color={getStatusColor(health.memory.status) as any}
            size="small"
          />
        </Grid>
      </Grid>
    </Box>
  );
};