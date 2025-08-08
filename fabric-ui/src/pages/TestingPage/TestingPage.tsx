// src/pages/TestingPage/TestingPage.tsx
import React from 'react';
import { Box, Typography, Alert } from '@mui/material';

export const TestingPage: React.FC = () => {
  return (
    <Box sx={{ p: 4 }}>
      <Typography variant="h4" gutterBottom>
        Configuration Testing
      </Typography>
      <Alert severity="info">
        Testing functionality coming soon...
      </Alert>
    </Box>
  );
};