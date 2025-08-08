// src/pages/YamlPreviewPage/YamlPreviewPage.tsx
import React from 'react';
import { Box, Typography, Alert } from '@mui/material';

export const YamlPreviewPage: React.FC = () => {
  return (
    <Box sx={{ p: 4 }}>
      <Typography variant="h4" gutterBottom>
        YAML Preview
      </Typography>
      <Alert severity="info">
        YAML preview functionality coming soon...
      </Alert>
    </Box>
  );
};