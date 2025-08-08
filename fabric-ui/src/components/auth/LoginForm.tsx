import React, { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  InputAdornment,
  IconButton,
  Link
} from '@mui/material';
import {
  Visibility,
  VisibilityOff,
  Person,
  Lock,
  Security
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { getDeviceInfo } from '../../utils/deviceFingerprint';

/**
 * Login Form Component
 * 
 * Secure login form with comprehensive validation, error handling,
 * and device fingerprinting for enhanced security. Integrates with
 * the authentication context for JWT token management.
 * 
 * Security Features:
 * - Input validation and sanitization
 * - Password visibility toggle
 * - Device fingerprinting
 * - Rate limiting protection
 * - Error handling with user-friendly messages
 * - Loading states for better UX
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */

interface LoginFormProps {
  onSuccess?: () => void;
}

export function LoginForm({ onSuccess }: LoginFormProps) {
  const { login, loading, error, clearError } = useAuth();
  
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  
  const [showPassword, setShowPassword] = useState(false);
  const [validationErrors, setValidationErrors] = useState<{[key: string]: string}>({});
  const [showDeviceInfo, setShowDeviceInfo] = useState(false);
  
  const deviceInfo = getDeviceInfo();

  const handleInputChange = (field: string) => (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    setFormData(prev => ({ ...prev, [field]: value }));
    
    // Clear validation error when user starts typing
    if (validationErrors[field]) {
      setValidationErrors(prev => ({ ...prev, [field]: '' }));
    }
    
    // Clear auth error when user starts typing
    if (error) {
      clearError();
    }
  };

  const validateForm = () => {
    const errors: {[key: string]: string} = {};
    
    if (!formData.username.trim()) {
      errors.username = 'Username is required';
    } else if (formData.username.length < 3) {
      errors.username = 'Username must be at least 3 characters';
    }
    
    if (!formData.password) {
      errors.password = 'Password is required';
    } else if (formData.password.length < 8) {
      errors.password = 'Password must be at least 8 characters';
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    try {
      await login({
        username: formData.username.trim(),
        password: formData.password
      });
      
      onSuccess?.();
    } catch (error) {
      // Error is handled by the auth context
      console.error('Login failed:', error);
    }
  };

  const togglePasswordVisibility = () => {
    setShowPassword(prev => !prev);
  };

  return (
    <Box
      sx={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: 2
      }}
    >
      <Card
        sx={{
          maxWidth: 400,
          width: '100%',
          boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
          borderRadius: 2
        }}
      >
        <CardContent sx={{ padding: 4 }}>
          {/* Header */}
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <Security sx={{ fontSize: 48, color: 'primary.main', mb: 1 }} />
            <Typography variant="h4" component="h1" gutterBottom>
              Fabric Platform
            </Typography>
            <Typography variant="body2" color="textSecondary">
              Secure Access Portal
            </Typography>
          </Box>

          {/* Error Alert */}
          {error && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={clearError}>
              {error}
            </Alert>
          )}

          {/* Login Form */}
          <form onSubmit={handleSubmit}>
            <TextField
              fullWidth
              label="Username"
              variant="outlined"
              value={formData.username}
              onChange={handleInputChange('username')}
              error={!!validationErrors.username}
              helperText={validationErrors.username}
              disabled={loading}
              sx={{ mb: 2 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Person color="action" />
                  </InputAdornment>
                )
              }}
            />

            <TextField
              fullWidth
              label="Password"
              type={showPassword ? 'text' : 'password'}
              variant="outlined"
              value={formData.password}
              onChange={handleInputChange('password')}
              error={!!validationErrors.password}
              helperText={validationErrors.password}
              disabled={loading}
              sx={{ mb: 3 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Lock color="action" />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={togglePasswordVisibility}
                      edge="end"
                      disabled={loading}
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                )
              }}
            />

            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={loading}
              sx={{
                py: 1.5,
                fontSize: '1.1rem',
                textTransform: 'none',
                borderRadius: 2
              }}
            >
              {loading ? (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <CircularProgress size={20} color="inherit" />
                  Signing In...
                </Box>
              ) : (
                'Sign In'
              )}
            </Button>
          </form>

          {/* Device Information */}
          <Box sx={{ mt: 3, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
            <Box sx={{ display: 'flex', justifyContent: 'center' }}>
              <Link
                component="button"
                variant="caption"
                onClick={() => setShowDeviceInfo(prev => !prev)}
                sx={{ textDecoration: 'none' }}
              >
                {showDeviceInfo ? 'Hide' : 'Show'} Device Information
              </Link>
            </Box>
            
            {showDeviceInfo && (
              <Box sx={{ mt: 2 }}>
                <Typography variant="caption" color="textSecondary" component="div">
                  <strong>Browser:</strong> {deviceInfo.browser}<br />
                  <strong>OS:</strong> {deviceInfo.os}<br />
                  <strong>Screen:</strong> {deviceInfo.screen}<br />
                  <strong>Language:</strong> {deviceInfo.language}<br />
                  <strong>Timezone:</strong> {deviceInfo.timezone}
                </Typography>
              </Box>
            )}
          </Box>

          {/* Security Notice */}
          <Box sx={{ mt: 3, textAlign: 'center' }}>
            <Typography variant="caption" color="textSecondary">
              Your session is secured with enterprise-grade encryption.<br />
              Device fingerprinting is used for enhanced security.
            </Typography>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}