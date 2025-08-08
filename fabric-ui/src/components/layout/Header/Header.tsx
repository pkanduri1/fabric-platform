// src/components/layout/Header/Header.tsx
import React from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Box,
  Chip,
  Button,
  Menu,
  MenuItem,
  Avatar
} from '@mui/material';
import {
  Menu as MenuIcon,
  DarkMode,
  LightMode,
  Settings,
  AccountCircle,
  Notifications,
  Save,
  Refresh
} from '@mui/icons-material';
import { useTheme } from '../../../contexts/ThemeContext';
import { useConfigurationContext } from '../../../contexts/ConfigurationContext';

interface HeaderProps {
  onMenuToggle: () => void;
  menuOpen: boolean;
}

export const Header: React.FC<HeaderProps> = ({ onMenuToggle, menuOpen }) => {
  const { isDarkMode, toggleTheme } = useTheme();
  const { 
    selectedSourceSystem, 
    selectedJob, 
    isDirty, 
    saveConfiguration,
    refreshSourceSystems 
  } = useConfigurationContext();

  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const handleProfileMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleSave = async () => {
    try {
      await saveConfiguration();
    } catch (error) {
      console.error('Save failed:', error);
    }
  };

  return (
    <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
      <Toolbar>
        <IconButton
          color="inherit"
          onClick={onMenuToggle}
          edge="start"
          sx={{ mr: 2 }}
        >
          <MenuIcon />
        </IconButton>

        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          Batch Configuration Tool
        </Typography>

        {/* Current Context */}
        {selectedSourceSystem && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mr: 2 }}>
            <Chip
              label={selectedSourceSystem.name}
              size="small"
              variant="outlined"
              sx={{ color: 'white', borderColor: 'white' }}
            />
            {selectedJob && (
              <Chip
                label={selectedJob.name}
                size="small"
                variant="filled"
                sx={{ bgcolor: 'rgba(255,255,255,0.2)' }}
              />
            )}
          </Box>
        )}

        {/* Action Buttons */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {isDirty && (
            <Button
              color="inherit"
              startIcon={<Save />}
              onClick={handleSave}
              size="small"
            >
              Save
            </Button>
          )}

          <IconButton color="inherit" onClick={refreshSourceSystems}>
            <Refresh />
          </IconButton>

          <IconButton color="inherit">
            <Notifications />
          </IconButton>

          <IconButton color="inherit" onClick={toggleTheme}>
            {isDarkMode ? <LightMode /> : <DarkMode />}
          </IconButton>

          <IconButton color="inherit" onClick={handleProfileMenu}>
            <AccountCircle />
          </IconButton>
        </Box>

        {/* Profile Menu */}
        <Menu
          anchorEl={anchorEl}
          open={Boolean(anchorEl)}
          onClose={handleClose}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
          transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        >
          <MenuItem onClick={handleClose}>
            <Settings sx={{ mr: 1 }} />
            Settings
          </MenuItem>
          <MenuItem onClick={handleClose}>
            <AccountCircle sx={{ mr: 1 }} />
            Profile
          </MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );
};