import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppRouter } from './router/AppRouter';
import { ConfigurationProvider } from './contexts/ConfigurationContext';
import { AuthProvider } from './contexts/AuthContext';
import { AppThemeProvider } from './contexts/ThemeContext';

function App() {
  return (
    <AppThemeProvider>
      <AuthProvider>
        <ConfigurationProvider>
         <BrowserRouter>
            <AppRouter />
          </BrowserRouter>
        </ConfigurationProvider>
      </AuthProvider>
    </AppThemeProvider>
  );
}

export default App;
