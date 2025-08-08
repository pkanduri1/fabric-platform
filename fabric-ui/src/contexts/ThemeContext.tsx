// src/contexts/ThemeContext.tsx
import React, { createContext, useContext, ReactNode, useState, useEffect } from 'react';
import { ThemeProvider, createTheme, Theme } from '@mui/material/styles';
import { CssBaseline } from '@mui/material';

// Theme configuration types
export type ThemeMode = 'light' | 'dark';
export type ThemeColor = 'blue' | 'green' | 'purple' | 'orange';

export interface ThemeSettings {
  mode: ThemeMode;
  primaryColor: ThemeColor;
  fontSize: 'small' | 'medium' | 'large';
  compactMode: boolean;
  animations: boolean;
}

export interface ThemeContextState {
  // Current theme settings
  settings: ThemeSettings;
  theme: Theme;
  
  // Actions
  toggleTheme: () => void;
  setThemeMode: (mode: ThemeMode) => void;
  setPrimaryColor: (color: ThemeColor) => void;
  setFontSize: (size: 'small' | 'medium' | 'large') => void;
  setCompactMode: (compact: boolean) => void;
  setAnimations: (enabled: boolean) => void;
  resetTheme: () => void;
  
  // Utilities
  isDarkMode: boolean;
  isLightMode: boolean;
}

// Default theme settings
const defaultSettings: ThemeSettings = {
  mode: 'light',
  primaryColor: 'blue',
  fontSize: 'medium',
  compactMode: false,
  animations: true,
};

// Color palette mapping
const colorPalettes = {
  blue: {
    50: '#e3f2fd',
    100: '#bbdefb',
    200: '#90caf9',
    300: '#64b5f6',
    400: '#42a5f5',
    500: '#2196f3',
    600: '#1e88e5',
    700: '#1976d2',
    800: '#1565c0',
    900: '#0d47a1',
  },
  green: {
    50: '#e8f5e8',
    100: '#c8e6c9',
    200: '#a5d6a7',
    300: '#81c784',
    400: '#66bb6a',
    500: '#4caf50',
    600: '#43a047',
    700: '#388e3c',
    800: '#2e7d32',
    900: '#1b5e20',
  },
  purple: {
    50: '#f3e5f5',
    100: '#e1bee7',
    200: '#ce93d8',
    300: '#ba68c8',
    400: '#ab47bc',
    500: '#9c27b0',
    600: '#8e24aa',
    700: '#7b1fa2',
    800: '#6a1b9a',
    900: '#4a148c',
  },
  orange: {
    50: '#fff3e0',
    100: '#ffe0b2',
    200: '#ffcc80',
    300: '#ffb74d',
    400: '#ffa726',
    500: '#ff9800',
    600: '#fb8c00',
    700: '#f57c00',
    800: '#ef6c00',
    900: '#e65100',
  },
};

// Create theme based on settings
const createAppTheme = (settings: ThemeSettings): Theme => {
  const { mode, primaryColor, fontSize, compactMode, animations } = settings;
  
  const palette = colorPalettes[primaryColor];
  const isDark = mode === 'dark';
  
  return createTheme({
    palette: {
      mode,
      primary: {
        main: palette[500],
        light: palette[300],
        dark: palette[700],
      },
      secondary: {
        main: isDark ? palette[200] : palette[600],
      },
      background: {
        default: isDark ? '#121212' : '#fafafa',
        paper: isDark ? '#1e1e1e' : '#ffffff',
      },
      text: {
        primary: isDark ? '#ffffff' : '#212121',
        secondary: isDark ? '#b3b3b3' : '#666666',
      },
    },
    typography: {
      fontSize: fontSize === 'small' ? 12 : fontSize === 'large' ? 16 : 14,
      h1: {
        fontSize: fontSize === 'small' ? '1.8rem' : fontSize === 'large' ? '2.4rem' : '2.1rem',
      },
      h2: {
        fontSize: fontSize === 'small' ? '1.5rem' : fontSize === 'large' ? '2rem' : '1.7rem',
      },
      h3: {
        fontSize: fontSize === 'small' ? '1.2rem' : fontSize === 'large' ? '1.6rem' : '1.4rem',
      },
      body1: {
        fontSize: fontSize === 'small' ? '0.8rem' : fontSize === 'large' ? '1.1rem' : '0.95rem',
      },
      body2: {
        fontSize: fontSize === 'small' ? '0.75rem' : fontSize === 'large' ? '1rem' : '0.875rem',
      },
    },
    spacing: compactMode ? 6 : 8,
    components: {
      MuiCard: {
        styleOverrides: {
          root: {
            boxShadow: isDark 
              ? '0 2px 8px rgba(0,0,0,0.4)' 
              : '0 2px 8px rgba(0,0,0,0.1)',
            transition: animations ? 'all 0.3s ease' : 'none',
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: compactMode ? 4 : 8,
            textTransform: 'none',
            fontWeight: 500,
            padding: compactMode ? '6px 12px' : '8px 16px',
            transition: animations ? 'all 0.2s ease' : 'none',
          },
        },
      },
      MuiTextField: {
        styleOverrides: {
          root: {
            '& .MuiOutlinedInput-root': {
              borderRadius: compactMode ? 4 : 8,
              transition: animations ? 'all 0.2s ease' : 'none',
            },
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: {
            borderRadius: compactMode ? 4 : 8,
            transition: animations ? 'all 0.3s ease' : 'none',
          },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            boxShadow: isDark 
              ? '0 2px 4px rgba(0,0,0,0.5)' 
              : '0 2px 4px rgba(0,0,0,0.1)',
          },
        },
      },
    },
  });
};

// Local storage key
const STORAGE_KEY = 'batch-config-ui-theme';

// Create context
const ThemeContext = createContext<ThemeContextState | null>(null);

// Context provider
export const AppThemeProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [settings, setSettings] = useState<ThemeSettings>(() => {
    // Load from localStorage
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? { ...defaultSettings, ...JSON.parse(saved) } : defaultSettings;
    } catch {
      return defaultSettings;
    }
  });
  
  // Create theme based on current settings
  const theme = createAppTheme(settings);
  
  // Save settings to localStorage
  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    } catch (error) {
      console.warn('Failed to save theme settings:', error);
    }
  }, [settings]);
  
  // Toggle between light and dark mode
  const toggleTheme = () => {
    setSettings(prev => ({
      ...prev,
      mode: prev.mode === 'light' ? 'dark' : 'light',
    }));
  };
  
  // Set specific theme mode
  const setThemeMode = (mode: ThemeMode) => {
    setSettings(prev => ({ ...prev, mode }));
  };
  
  // Set primary color
  const setPrimaryColor = (primaryColor: ThemeColor) => {
    setSettings(prev => ({ ...prev, primaryColor }));
  };
  
  // Set font size
  const setFontSize = (fontSize: 'small' | 'medium' | 'large') => {
    setSettings(prev => ({ ...prev, fontSize }));
  };
  
  // Set compact mode
  const setCompactMode = (compactMode: boolean) => {
    setSettings(prev => ({ ...prev, compactMode }));
  };
  
  // Set animations
  const setAnimations = (animations: boolean) => {
    setSettings(prev => ({ ...prev, animations }));
  };
  
  // Reset to default theme
  const resetTheme = () => {
    setSettings(defaultSettings);
  };
  
  const contextValue: ThemeContextState = {
    settings,
    theme,
    toggleTheme,
    setThemeMode,
    setPrimaryColor,
    setFontSize,
    setCompactMode,
    setAnimations,
    resetTheme,
    isDarkMode: settings.mode === 'dark',
    isLightMode: settings.mode === 'light',
  };
  
  return (
    <ThemeContext.Provider value={contextValue}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </ThemeContext.Provider>
  );
};

// Hook to use theme context
export const useTheme = (): ThemeContextState => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within AppThemeProvider');
  }
  return context;
};

// Convenience hooks
export const useThemeMode = () => {
  const { settings, toggleTheme, setThemeMode, isDarkMode, isLightMode } = useTheme();
  return { 
    mode: settings.mode, 
    toggleTheme, 
    setThemeMode, 
    isDarkMode, 
    isLightMode 
  };
};

export const useThemeColor = () => {
  const { settings, setPrimaryColor } = useTheme();
  return { 
    primaryColor: settings.primaryColor, 
    setPrimaryColor 
  };
};

export const useThemeSettings = () => {
  const { 
    settings, 
    setFontSize, 
    setCompactMode, 
    setAnimations, 
    resetTheme 
  } = useTheme();
  
  return {
    settings,
    setFontSize,
    setCompactMode,
    setAnimations,
    resetTheme
  };
};