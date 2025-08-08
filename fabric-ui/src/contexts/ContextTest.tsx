// src/components/ContextTest.tsx
import React from 'react';
import { Box, Button, Typography, Paper } from '@mui/material';
import { useConfigurationContext } from '../contexts/ConfigurationContext';
import { useTheme } from '../contexts/ThemeContext';

export const ContextTest: React.FC = () => {
  const { 
    sourceSystems, 
    isLoading, 
    error,
    refreshSourceSystems,
    currentTransactionType,
    availableTransactionTypes 
  } = useConfigurationContext();
  
  const { 
    settings, 
    toggleTheme, 
    setPrimaryColor,
    isDarkMode 
  } = useTheme();

  return (
    <Paper sx={{ p: 3, m: 2 }}>
      <Typography variant="h6" gutterBottom>
        Context Validation Test
      </Typography>
      
      {/* Configuration Context Test */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="subtitle1" gutterBottom>
          Configuration Context:
        </Typography>
        <Typography variant="body2">
          Loading: {isLoading ? 'Yes' : 'No'}
        </Typography>
        <Typography variant="body2">
          Error: {error || 'None'}
        </Typography>
        <Typography variant="body2">
          Source Systems: {sourceSystems.length}
        </Typography>
        <Typography variant="body2">
          Current Transaction Type: {currentTransactionType}
        </Typography>
        <Typography variant="body2">
          Available Transaction Types: {availableTransactionTypes.length}
        </Typography>
        <Button 
          variant="outlined" 
          size="small" 
          onClick={refreshSourceSystems}
          sx={{ mt: 1 }}
        >
          Test Refresh
        </Button>
      </Box>

      {/* Theme Context Test */}
      <Box>
        <Typography variant="subtitle1" gutterBottom>
          Theme Context:
        </Typography>
        <Typography variant="body2">
          Mode: {settings.mode} (Dark: {isDarkMode ? 'Yes' : 'No'})
        </Typography>
        <Typography variant="body2">
          Primary Color: {settings.primaryColor}
        </Typography>
        <Typography variant="body2">
          Font Size: {settings.fontSize}
        </Typography>
        <Box sx={{ mt: 1, gap: 1, display: 'flex' }}>
          <Button 
            variant="outlined" 
            size="small" 
            onClick={toggleTheme}
          >
            Toggle Theme
          </Button>
          <Button 
            variant="outlined" 
            size="small" 
            onClick={() => setPrimaryColor('green')}
          >
            Green Theme
          </Button>
        </Box>
      </Box>
    </Paper>
  );
};