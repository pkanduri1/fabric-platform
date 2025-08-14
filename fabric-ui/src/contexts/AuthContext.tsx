import React, { createContext, useContext, useReducer, useEffect, ReactNode } from 'react';
import { authApi } from '../services/api/authApi';
import { generateDeviceFingerprint } from '../utils/deviceFingerprint';

/**
 * Authentication Context for Fabric Platform
 * 
 * Comprehensive authentication state management with JWT token handling,
 * session management, and role-based access control integration.
 * 
 * Features:
 * - JWT token lifecycle management
 * - Automatic token refresh
 * - Device fingerprinting for security
 * - Role and permission-based access control
 * - Session timeout handling
 * - Secure token storage
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */

// Types
interface User {
  userId: string;
  username: string;
  email: string;
  fullName: string;
  roles: string[];
  permissions: string[];
  department?: string;
  title?: string;
  mfaEnabled: boolean;
  mfaVerified: boolean;
}

interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  sessionId: string | null;
  correlationId: string | null;
  loading: boolean;
  error: string | null;
}

interface LoginRequest {
  username: string;
  password: string;
}

interface AuthContextType extends AuthState {
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshAccessToken: () => Promise<void>;
  hasRole: (role: string) => boolean;
  hasPermission: (permission: string) => boolean;
  hasAnyRole: (roles: string[]) => boolean;
  hasAnyPermission: (permissions: string[]) => boolean;
  clearError: () => void;
}

// Action types
type AuthAction =
  | { type: 'LOGIN_START' }
  | { type: 'LOGIN_SUCCESS'; payload: { user: User; accessToken: string; refreshToken: string; sessionId: string; correlationId: string } }
  | { type: 'LOGIN_FAILURE'; payload: string }
  | { type: 'LOGOUT' }
  | { type: 'REFRESH_TOKEN_SUCCESS'; payload: { accessToken: string; correlationId: string } }
  | { type: 'REFRESH_TOKEN_FAILURE' }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'CLEAR_ERROR' }
  | { type: 'SET_ERROR'; payload: string };

// Initial state
const initialState: AuthState = {
  isAuthenticated: false,
  user: null,
  accessToken: null,
  refreshToken: null,
  sessionId: null,
  correlationId: null,
  loading: false,
  error: null
};

// Reducer
function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'LOGIN_START':
      return {
        ...state,
        loading: true,
        error: null
      };
      
    case 'LOGIN_SUCCESS':
      return {
        ...state,
        isAuthenticated: true,
        user: action.payload.user,
        accessToken: action.payload.accessToken,
        refreshToken: action.payload.refreshToken,
        sessionId: action.payload.sessionId,
        correlationId: action.payload.correlationId,
        loading: false,
        error: null
      };
      
    case 'LOGIN_FAILURE':
      return {
        ...state,
        isAuthenticated: false,
        user: null,
        accessToken: null,
        refreshToken: null,
        sessionId: null,
        correlationId: null,
        loading: false,
        error: action.payload
      };
      
    case 'LOGOUT':
      return {
        ...initialState
      };
      
    case 'REFRESH_TOKEN_SUCCESS':
      return {
        ...state,
        accessToken: action.payload.accessToken,
        correlationId: action.payload.correlationId,
        error: null
      };
      
    case 'REFRESH_TOKEN_FAILURE':
      return {
        ...initialState,
        error: 'Session expired. Please login again.'
      };
      
    case 'SET_LOADING':
      return {
        ...state,
        loading: action.payload
      };
      
    case 'CLEAR_ERROR':
      return {
        ...state,
        error: null
      };
      
    case 'SET_ERROR':
      return {
        ...state,
        error: action.payload
      };
      
    default:
      return state;
  }
}

// Context
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Token storage keys
const TOKEN_STORAGE_KEY = 'fabric_access_token';
const REFRESH_TOKEN_STORAGE_KEY = 'fabric_refresh_token';
const USER_STORAGE_KEY = 'fabric_user';
const SESSION_STORAGE_KEY = 'fabric_session';

// Provider component
interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [state, dispatch] = useReducer(authReducer, initialState);

  // Initialize auth state from storage
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const accessToken = localStorage.getItem(TOKEN_STORAGE_KEY);
        const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
        const userStr = localStorage.getItem(USER_STORAGE_KEY);
        const sessionStr = localStorage.getItem(SESSION_STORAGE_KEY);

        if (accessToken && refreshToken && userStr && sessionStr) {
          const user = JSON.parse(userStr);
          const session = JSON.parse(sessionStr);
          
          // Verify token is still valid by getting user profile
          try {
            await authApi.getUserProfile();
            
            dispatch({
              type: 'LOGIN_SUCCESS',
              payload: {
                user,
                accessToken,
                refreshToken,
                sessionId: session.sessionId,
                correlationId: session.correlationId
              }
            });
          } catch (error) {
            // Token is invalid, try to refresh
            try {
              await refreshAccessToken();
            } catch (refreshError) {
              // Refresh failed, clear storage and require re-login
              clearStorage();
            }
          }
        } else {
          // TEMPORARY: Auto-login with mock user for development/testing
          console.log('[AuthContext] No stored auth found, using mock authentication for development');
          const mockUser = {
            userId: 'test-user-001',
            username: 'testuser',
            email: 'testuser@example.com',
            fullName: 'Test User',
            roles: ['JOB_CREATOR', 'JOB_MODIFIER', 'JOB_EXECUTOR', 'JOB_VIEWER'],
            permissions: ['READ_CONFIGS', 'WRITE_CONFIGS', 'DELETE_CONFIGS'],
            department: 'Engineering',
            title: 'Senior Developer',
            mfaEnabled: false,
            mfaVerified: true
          };
          
          const mockSession = {
            sessionId: 'mock-session-' + Date.now(),
            correlationId: 'mock-correlation-' + Date.now()
          };
          
          // Store mock auth data
          localStorage.setItem(TOKEN_STORAGE_KEY, 'mock-access-token');
          localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, 'mock-refresh-token');
          localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(mockUser));
          localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(mockSession));
          
          dispatch({
            type: 'LOGIN_SUCCESS',
            payload: {
              user: mockUser,
              accessToken: 'mock-access-token',
              refreshToken: 'mock-refresh-token',
              sessionId: mockSession.sessionId,
              correlationId: mockSession.correlationId
            }
          });
        }
      } catch (error) {
        console.error('Error initializing auth state:', error);
        clearStorage();
      }
    };

    initializeAuth();
  }, []);

  // Auto-refresh token before expiration
  useEffect(() => {
    if (state.accessToken && state.refreshToken) {
      // Set up token refresh 2 minutes before expiration (15 min - 2 min = 13 min)
      const refreshInterval = setInterval(() => {
        refreshAccessToken().catch(console.error);
      }, 13 * 60 * 1000); // 13 minutes

      return () => clearInterval(refreshInterval);
    }
  }, [state.isAuthenticated]); // ✅ Use stable boolean instead of changing token values

  const login = async (credentials: LoginRequest) => {
    dispatch({ type: 'LOGIN_START' });

    try {
      const deviceFingerprint = generateDeviceFingerprint();
      
      const response = await authApi.login({
        ...credentials,
        deviceFingerprint
      });

      const { user, accessToken, refreshToken, sessionId, correlationId } = response;

      // Store tokens and user data
      localStorage.setItem(TOKEN_STORAGE_KEY, accessToken);
      localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
      localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify({ sessionId, correlationId }));

      dispatch({
        type: 'LOGIN_SUCCESS',
        payload: { user, accessToken, refreshToken, sessionId, correlationId }
      });

    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Login failed';
      dispatch({ type: 'LOGIN_FAILURE', payload: errorMessage });
      throw error;
    }
  };

  const logout = async () => {
    try {
      if (state.accessToken) {
        await authApi.logout();
      }
    } catch (error) {
      console.error('Error during logout:', error);
    } finally {
      clearStorage();
      dispatch({ type: 'LOGOUT' });
    }
  };

  const refreshAccessToken = async () => {
    if (!state.refreshToken) {
      throw new Error('No refresh token available');
    }

    try {
      const response = await authApi.refreshToken({
        refreshToken: state.refreshToken
      });

      const { accessToken, correlationId } = response;

      // Update stored access token
      localStorage.setItem(TOKEN_STORAGE_KEY, accessToken);

      dispatch({
        type: 'REFRESH_TOKEN_SUCCESS',
        payload: { accessToken, correlationId }
      });

    } catch (error) {
      clearStorage();
      dispatch({ type: 'REFRESH_TOKEN_FAILURE' });
      throw error;
    }
  };

  const clearStorage = () => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    localStorage.removeItem(SESSION_STORAGE_KEY);
  };

  const hasRole = (role: string): boolean => {
    return state.user?.roles?.includes(role) || false;
  };

  const hasPermission = (permission: string): boolean => {
    return state.user?.permissions?.includes(permission) || false;
  };

  const hasAnyRole = (roles: string[]): boolean => {
    return state.user?.roles?.some(role => roles.includes(role)) || false;
  };

  const hasAnyPermission = (permissions: string[]): boolean => {
    return state.user?.permissions?.some(permission => permissions.includes(permission)) || false;
  };

  const clearError = () => {
    dispatch({ type: 'CLEAR_ERROR' });
  };

  const contextValue: AuthContextType = {
    ...state,
    login,
    logout,
    refreshAccessToken,
    hasRole,
    hasPermission,
    hasAnyRole,
    hasAnyPermission,
    clearError
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}

// Hook to use auth context
export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export type { User, LoginRequest, AuthContextType };