import React from 'react';
import { BrowserRouter, BrowserRouter as Router } from 'react-router-dom';
import { AppRouter } from './router/AppRouter';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { createTheme } from '@mui/material/styles';
import { Container, Typography, Box, Card, CardContent, Button } from '@mui/material';
import { Build, Code, Visibility } from '@mui/icons-material';
import { ConfigurationProvider } from './contexts/ConfigurationContext';
import { AppThemeProvider } from './contexts/ThemeContext';
import { ContextTest } from './contexts/ContextTest';
import { HomePage } from './pages/HomePage';
import { PageLayout } from './components/layout/PageLayout';

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
    background: {
      default: '#f5f5f5',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
});

function App() {
  return (
    <AppThemeProvider>
      <ConfigurationProvider>
       <BrowserRouter>
          <AppRouter />
        </BrowserRouter>
      </ConfigurationProvider>
    </AppThemeProvider>
  );
}

export default App;
