// src/pages/HomePage/HomePage.tsx
import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Grid,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
  Fab,
  Tooltip,
  Alert,
  CircularProgress
} from '@mui/material';
import { Add, Settings, PlayArrow, Description } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useSourceSystemsState } from '../../contexts/ConfigurationContext';
import { AddSourceSystemDialog } from '../../components/configuration/AddSourceSystemDialog/AddSourceSystemDialog';
import { SourceSystem } from '../../types/configuration';

export const HomePage: React.FC = () => {
  const navigate = useNavigate();
  const { 
    sourceSystems,
    selectSourceSystem,
    refreshSourceSystems
  } = useSourceSystemsState();
  
  // Local state for loading and error handling
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const [showAddDialog, setShowAddDialog] = useState(false);

  // Load source systems on mount
  useEffect(() => {
    const loadSystems = async () => {
      try {
        setIsLoading(true);
        setError(null);
        await refreshSourceSystems();
      } catch (err) {
        setError('Failed to load source systems');
        console.error('Error loading source systems:', err);
      } finally {
        setIsLoading(false);
      }
    };

    if (sourceSystems.length === 0) {
      loadSystems();
    }
  }, [refreshSourceSystems, sourceSystems.length]);

  const handleAddSourceSystem = async (newSystem: Omit<SourceSystem, 'id'>) => {
    try {
      setIsLoading(true);
      setError(null);
      
      // Generate a unique ID
      const systemWithId: SourceSystem = {
        ...newSystem,
        id: `system_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
      };
      
      // TODO: Call your API to add the source system
      // await configApi.addSourceSystem(systemWithId);
      
      // For now, we'll refresh the source systems to get updated data
      await refreshSourceSystems();
      
      console.log('Added new source system:', systemWithId);
    } catch (err) {
      setError('Failed to add source system');
      console.error('Error adding source system:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleConfigureSystem = (systemId: string, jobName: string) => {
    navigate(`/configuration/${systemId}/${jobName}`);
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '400px' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" gutterBottom>
          Batch Configuration Dashboard
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Manage and configure batch processing systems and job mappings
        </Typography>
      </Box>

      {/* Statistics Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="primary">
                {sourceSystems.length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Source Systems
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="success.main">
                {sourceSystems.reduce((total, system) => total + system.jobs.length, 0)}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Jobs
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="warning.main">
                {sourceSystems.filter(s => s.systemType === 'oracle').length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Database Sources
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="info.main">
                {sourceSystems.filter(s => s.systemType === 'file').length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                File Sources
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Source Systems Grid */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h5" gutterBottom>
          Source Systems
        </Typography>
        <Grid container spacing={3}>
          {sourceSystems.map((system) => (
            <Grid item xs={12} sm={6} md={4} key={system.id}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                    <Typography variant="h6" component="h2">
                      {system.name}
                    </Typography>
                    <Chip 
                      label={system.systemType} 
                      size="small" 
                      color="primary" 
                      variant="outlined" 
                    />
                  </Box>
                  
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    {system.description}
                  </Typography>

                  <Box sx={{ mb: 2 }}>
                    <Typography variant="caption" color="text.secondary" display="block">
                      Jobs ({system.jobs.length})
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 0.5 }}>
                      {system.jobs.slice(0, 3).map((job) => (
                        <Chip
                          key={job.jobName}
                          label={job.jobName}
                          size="small"
                          variant="outlined"
                          onClick={() => handleConfigureSystem(system.id, job.jobName)}
                          sx={{ cursor: 'pointer' }}
                        />
                      ))}
                      {system.jobs.length > 3 && (
                        <Chip
                          label={`+${system.jobs.length - 3} more`}
                          size="small"
                          variant="outlined"
                          color="default"
                        />
                      )}
                    </Box>
                  </Box>
                </CardContent>

                <CardActions>
                  <Button 
                    size="small" 
                    startIcon={<Settings />}
                    onClick={() => {
                      if (system.jobs.length > 0) {
                        handleConfigureSystem(system.id, system.jobs[0].jobName);
                      }
                    }}
                    disabled={system.jobs.length === 0}
                  >
                    Configure
                  </Button>
                  <Button 
                    size="small" 
                    startIcon={<PlayArrow />}
                    color="success"
                  >
                    Run Job
                  </Button>
                  <Button 
                    size="small" 
                    startIcon={<Description />}
                  >
                    View YAML
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          ))}

          {/* Add New System Card */}
          <Grid item xs={12} sm={6} md={4}>
            <Card 
              sx={{ 
                height: '100%', 
                display: 'flex', 
                flexDirection: 'column',
                border: 2,
                borderStyle: 'dashed',
                borderColor: 'divider',
                cursor: 'pointer',
                '&:hover': {
                  borderColor: 'primary.main',
                  backgroundColor: 'action.hover'
                }
              }}
              onClick={() => setShowAddDialog(true)}
            >
              <CardContent sx={{ 
                flexGrow: 1, 
                display: 'flex', 
                flexDirection: 'column', 
                alignItems: 'center', 
                justifyContent: 'center',
                textAlign: 'center'
              }}>
                <Add sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                <Typography variant="h6" color="text.secondary" gutterBottom>
                  Add New Source System
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Configure a new source system and its batch jobs
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>

      {/* Quick Actions */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h5" gutterBottom>
          Quick Actions
        </Typography>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6} md={3}>
            <Button
              variant="outlined"
              fullWidth
              startIcon={<Add />}
              onClick={() => setShowAddDialog(true)}
            >
              Add Source System
            </Button>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Button
              variant="outlined"
              fullWidth
              startIcon={<Settings />}
            >
              System Settings
            </Button>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Button
              variant="outlined"
              fullWidth
              startIcon={<PlayArrow />}
            >
              Run All Jobs
            </Button>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Button
              variant="outlined"
              fullWidth
              startIcon={<Description />}
            >
              Export Configuration
            </Button>
          </Grid>
        </Grid>
      </Box>

      {/* Floating Action Button */}
      <Tooltip title="Add Source System">
        <Fab 
          color="primary" 
          sx={{ position: 'fixed', bottom: 16, right: 16 }}
          onClick={() => setShowAddDialog(true)}
        >
          <Add />
        </Fab>
      </Tooltip>

      {/* Add Source System Dialog */}
      <AddSourceSystemDialog
        open={showAddDialog}
        onClose={() => setShowAddDialog(false)}
        onAdd={handleAddSourceSystem}
      />
    </Box>
  );
};