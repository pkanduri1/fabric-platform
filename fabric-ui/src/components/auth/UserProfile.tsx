import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Avatar,
  Chip,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  IconButton,
  Tooltip,
  Alert,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions
} from '@mui/material';
import {
  Person,
  Email,
  Business,
  Work,
  Security,
  VpnKey,
  Logout,
  Shield,
  Assignment,
  CheckCircle,
  Warning
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';

/**
 * User Profile Component
 * 
 * Comprehensive user profile display with role/permission information,
 * security status, and session management. Provides users with visibility
 * into their access levels and security settings.
 * 
 * Features:
 * - User information display
 * - Role and permission visualization
 * - Security status indicators
 * - Session information
 * - Logout functionality
 * - MFA status display
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */

interface UserProfileProps {
  onClose?: () => void;
}

export function UserProfile({ onClose }: UserProfileProps) {
  const { user, logout, sessionId, correlationId } = useAuth();
  const [showLogoutDialog, setShowLogoutDialog] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  if (!user) {
    return (
      <Alert severity="error">
        User information not available
      </Alert>
    );
  }

  const handleLogout = async () => {
    try {
      setIsLoggingOut(true);
      await logout();
      onClose?.();
    } catch (error) {
      console.error('Logout failed:', error);
    } finally {
      setIsLoggingOut(false);
      setShowLogoutDialog(false);
    }
  };

  const getRoleColor = (role: string) => {
    switch (role.toLowerCase()) {
      case 'admin': return 'error';
      case 'manager': return 'warning';
      case 'analyst': return 'info';
      case 'operator': return 'primary';
      case 'viewer': return 'default';
      default: return 'default';
    }
  };

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto', p: 2 }}>
      <Card>
        <CardContent>
          {/* Header */}
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
            <Avatar
              sx={{
                width: 64,
                height: 64,
                bgcolor: 'primary.main',
                fontSize: '1.5rem',
                mr: 2
              }}
            >
              {getInitials(user.fullName)}
            </Avatar>
            <Box sx={{ flexGrow: 1 }}>
              <Typography variant="h5" component="h2">
                {user.fullName}
              </Typography>
              <Typography variant="body2" color="textSecondary">
                @{user.username}
              </Typography>
            </Box>
            <Tooltip title="Logout">
              <IconButton
                color="error"
                onClick={() => setShowLogoutDialog(true)}
              >
                <Logout />
              </IconButton>
            </Tooltip>
          </Box>

          {/* Basic Information */}
          <List dense>
            <ListItem>
              <ListItemIcon>
                <Email />
              </ListItemIcon>
              <ListItemText
                primary="Email"
                secondary={user.email}
              />
            </ListItem>
            
            {user.department && (
              <ListItem>
                <ListItemIcon>
                  <Business />
                </ListItemIcon>
                <ListItemText
                  primary="Department"
                  secondary={user.department}
                />
              </ListItem>
            )}
            
            {user.title && (
              <ListItem>
                <ListItemIcon>
                  <Work />
                </ListItemIcon>
                <ListItemText
                  primary="Title"
                  secondary={user.title}
                />
              </ListItem>
            )}
          </List>

          <Divider sx={{ my: 2 }} />

          {/* Security Status */}
          <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Security />
            Security Status
          </Typography>
          
          <Box sx={{ mb: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
              {user.mfaEnabled ? (
                <CheckCircle color="success" fontSize="small" />
              ) : (
                <Warning color="warning" fontSize="small" />
              )}
              <Typography variant="body2">
                Multi-Factor Authentication: {user.mfaEnabled ? 'Enabled' : 'Disabled'}
              </Typography>
            </Box>
            
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              {user.mfaVerified ? (
                <CheckCircle color="success" fontSize="small" />
              ) : (
                <Warning color="warning" fontSize="small" />
              )}
              <Typography variant="body2">
                Current Session MFA: {user.mfaVerified ? 'Verified' : 'Not Verified'}
              </Typography>
            </Box>
          </Box>

          <Divider sx={{ my: 2 }} />

          {/* Roles */}
          <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Shield />
            Roles
          </Typography>
          
          <Box sx={{ mb: 2 }}>
            {user.roles.length > 0 ? (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {user.roles.map((role) => (
                  <Chip
                    key={role}
                    label={role}
                    color={getRoleColor(role) as any}
                    size="small"
                    variant="outlined"
                  />
                ))}
              </Box>
            ) : (
              <Typography variant="body2" color="textSecondary">
                No roles assigned
              </Typography>
            )}
          </Box>

          <Divider sx={{ my: 2 }} />

          {/* Permissions */}
          <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Assignment />
            Permissions
          </Typography>
          
          <Box sx={{ mb: 2 }}>
            {user.permissions.length > 0 ? (
              <Box sx={{ maxHeight: 200, overflow: 'auto' }}>
                {user.permissions.map((permission) => (
                  <Chip
                    key={permission}
                    label={permission}
                    size="small"
                    variant="outlined"
                    sx={{ m: 0.5 }}
                  />
                ))}
              </Box>
            ) : (
              <Typography variant="body2" color="textSecondary">
                No permissions assigned
              </Typography>
            )}
          </Box>

          <Divider sx={{ my: 2 }} />

          {/* Session Information */}
          <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <VpnKey />
            Session Information
          </Typography>
          
          <List dense>
            <ListItem>
              <ListItemText
                primary="User ID"
                secondary={user.userId}
                primaryTypographyProps={{ variant: 'body2' }}
                secondaryTypographyProps={{ variant: 'caption', fontFamily: 'monospace' }}
              />
            </ListItem>
            <ListItem>
              <ListItemText
                primary="Session ID"
                secondary={sessionId}
                primaryTypographyProps={{ variant: 'body2' }}
                secondaryTypographyProps={{ variant: 'caption', fontFamily: 'monospace' }}
              />
            </ListItem>
            <ListItem>
              <ListItemText
                primary="Correlation ID"
                secondary={correlationId}
                primaryTypographyProps={{ variant: 'body2' }}
                secondaryTypographyProps={{ variant: 'caption', fontFamily: 'monospace' }}
              />
            </ListItem>
          </List>
        </CardContent>
      </Card>

      {/* Logout Confirmation Dialog */}
      <Dialog
        open={showLogoutDialog}
        onClose={() => setShowLogoutDialog(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Confirm Logout</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to logout? You will need to sign in again to access the application.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setShowLogoutDialog(false)}
            disabled={isLoggingOut}
          >
            Cancel
          </Button>
          <Button
            onClick={handleLogout}
            color="error"
            variant="contained"
            disabled={isLoggingOut}
          >
            {isLoggingOut ? 'Logging out...' : 'Logout'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}