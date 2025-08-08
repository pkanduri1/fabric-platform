import React, { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Box, CircularProgress, Alert } from '@mui/material';
import { useAuth } from '../../contexts/AuthContext';

/**
 * Protected Route Component
 * 
 * Route guard component that protects pages requiring authentication
 * and/or specific roles/permissions. Provides loading states and
 * automatic redirects for unauthorized access attempts.
 * 
 * Security Features:
 * - Authentication verification
 * - Role-based access control
 * - Permission-based access control
 * - Automatic redirect to login
 * - Loading states during auth checks
 * - Error handling for access denied
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */

interface ProtectedRouteProps {
  children: ReactNode;
  requireAuth?: boolean;
  requiredRoles?: string[];
  requiredPermissions?: string[];
  requireAllRoles?: boolean;
  requireAllPermissions?: boolean;
  fallbackPath?: string;
}

export function ProtectedRoute({
  children,
  requireAuth = true,
  requiredRoles = [],
  requiredPermissions = [],
  requireAllRoles = false,
  requireAllPermissions = false,
  fallbackPath = '/login'
}: ProtectedRouteProps) {
  const { isAuthenticated, user, loading, hasAnyRole, hasRole, hasAnyPermission, hasPermission } = useAuth();
  const location = useLocation();

  // Show loading spinner while auth state is being determined
  if (loading) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh'
        }}
      >
        <CircularProgress size={40} />
      </Box>
    );
  }

  // Redirect to login if authentication is required but user is not authenticated
  if (requireAuth && !isAuthenticated) {
    return <Navigate to={fallbackPath} state={{ from: location }} replace />;
  }

  // Check role requirements
  if (requiredRoles.length > 0 && user) {
    const hasRequiredRoles = requireAllRoles
      ? requiredRoles.every(role => hasRole(role))
      : hasAnyRole(requiredRoles);

    if (!hasRequiredRoles) {
      return (
        <Box sx={{ p: 3 }}>
          <Alert severity="error">
            <strong>Access Denied</strong><br />
            You don't have the required role(s) to access this page.<br />
            Required roles: {requiredRoles.join(', ')}<br />
            Your roles: {user.roles.join(', ') || 'None'}
          </Alert>
        </Box>
      );
    }
  }

  // Check permission requirements
  if (requiredPermissions.length > 0 && user) {
    const hasRequiredPermissions = requireAllPermissions
      ? requiredPermissions.every(permission => hasPermission(permission))
      : hasAnyPermission(requiredPermissions);

    if (!hasRequiredPermissions) {
      return (
        <Box sx={{ p: 3 }}>
          <Alert severity="error">
            <strong>Access Denied</strong><br />
            You don't have the required permission(s) to access this page.<br />
            Required permissions: {requiredPermissions.join(', ')}<br />
            Your permissions: {user.permissions.join(', ') || 'None'}
          </Alert>
        </Box>
      );
    }
  }

  // All checks passed, render the protected content
  return <>{children}</>;
}

/**
 * Higher-order component wrapper for protected routes
 */
export function withProtectedRoute<P extends object>(
  Component: React.ComponentType<P>,
  protectionOptions: Omit<ProtectedRouteProps, 'children'>
) {
  return function ProtectedComponent(props: P) {
    return (
      <ProtectedRoute {...protectionOptions}>
        <Component {...props} />
      </ProtectedRoute>
    );
  };
}

/**
 * Hook for checking permissions within components
 */
export function usePermissionCheck() {
  const { hasRole, hasPermission, hasAnyRole, hasAnyPermission, user } = useAuth();

  const checkAccess = (options: {
    roles?: string[];
    permissions?: string[];
    requireAllRoles?: boolean;
    requireAllPermissions?: boolean;
  }) => {
    const { roles = [], permissions = [], requireAllRoles = false, requireAllPermissions = false } = options;

    if (!user) return false;

    // Check roles
    if (roles.length > 0) {
      const hasRequiredRoles = requireAllRoles
        ? roles.every(role => hasRole(role))
        : hasAnyRole(roles);
      
      if (!hasRequiredRoles) return false;
    }

    // Check permissions
    if (permissions.length > 0) {
      const hasRequiredPermissions = requireAllPermissions
        ? permissions.every(permission => hasPermission(permission))
        : hasAnyPermission(permissions);
      
      if (!hasRequiredPermissions) return false;
    }

    return true;
  };

  return {
    checkAccess,
    hasRole,
    hasPermission,
    hasAnyRole,
    hasAnyPermission,
    user
  };
}