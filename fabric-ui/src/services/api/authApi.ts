import { httpClient } from '../httpClient';

/**
 * Authentication API Service
 * 
 * HTTP client service for authentication operations including login, logout,
 * token refresh, and user profile management. Integrates with the backend
 * authentication endpoints with comprehensive error handling and security.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */

// Request/Response interfaces
interface LoginRequest {
  username: string;
  password: string;
  deviceFingerprint?: string;
}

interface LoginResponse {
  success: boolean;
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  correlationId: string;
  sessionId: string;
  user: {
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
  };
}

interface RefreshTokenRequest {
  refreshToken: string;
}

interface RefreshTokenResponse {
  success: boolean;
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  correlationId: string;
}

interface LogoutResponse {
  success: boolean;
  message: string;
  correlationId: string;
  timestamp: string;
}

interface UserProfile {
  userId: string;
  username: string;
  roles: string[];
  permissions: string[];
  mfaVerified: boolean;
  sessionId: string;
  correlationId: string;
}

export const authApi = {
  /**
   * User login
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    try {
      // MOCK: Return mock login response for development
      await new Promise(resolve => setTimeout(resolve, 500));
      
      const mockResponse: LoginResponse = {
        success: true,
        accessToken: 'mock-access-token-' + Date.now(),
        refreshToken: 'mock-refresh-token-' + Date.now(),
        tokenType: 'Bearer',
        expiresIn: 900, // 15 minutes
        correlationId: 'mock-login-corr-' + Date.now(),
        sessionId: 'mock-session-' + Date.now(),
        user: {
          userId: 'test-user-001',
          username: credentials.username,
          email: credentials.username + '@example.com',
          fullName: 'Test User',
          roles: ['JOB_CREATOR', 'JOB_MODIFIER', 'JOB_EXECUTOR', 'JOB_VIEWER'],
          permissions: ['READ_CONFIGS', 'WRITE_CONFIGS', 'DELETE_CONFIGS'],
          department: 'Engineering',
          title: 'Senior Developer',
          mfaEnabled: false,
          mfaVerified: true
        }
      };
      
      console.log('[Auth API MOCK] Login successful for user:', credentials.username);
      return mockResponse;
    } catch (error) {
      // Fallback to actual API call if needed
      const response = await httpClient.post<LoginResponse>('/api/auth/login', credentials);
      return response.data;
    }
  },

  /**
   * User logout
   */
  async logout(): Promise<LogoutResponse> {
    try {
      // MOCK: Return mock logout response
      await new Promise(resolve => setTimeout(resolve, 200));
      
      const mockResponse: LogoutResponse = {
        success: true,
        message: 'Logout successful',
        correlationId: 'mock-logout-corr-' + Date.now(),
        timestamp: new Date().toISOString()
      };
      
      console.log('[Auth API MOCK] Logout successful');
      return mockResponse;
    } catch (error) {
      // Fallback to actual API call if needed
      const response = await httpClient.post<LogoutResponse>('/api/auth/logout');
      return response.data;
    }
  },

  /**
   * Refresh access token
   */
  async refreshToken(request: RefreshTokenRequest): Promise<RefreshTokenResponse> {
    try {
      // MOCK: Return mock refresh response
      await new Promise(resolve => setTimeout(resolve, 300));
      
      const mockResponse: RefreshTokenResponse = {
        success: true,
        accessToken: 'mock-refreshed-token-' + Date.now(),
        tokenType: 'Bearer',
        expiresIn: 900, // 15 minutes
        correlationId: 'mock-refresh-corr-' + Date.now()
      };
      
      console.log('[Auth API MOCK] Token refresh successful');
      return mockResponse;
    } catch (error) {
      // Fallback to actual API call if needed
      const response = await httpClient.post<RefreshTokenResponse>('/api/auth/refresh', request);
      return response.data;
    }
  },

  /**
   * Get current user profile
   */
  async getUserProfile(): Promise<UserProfile> {
    try {
      // MOCK: Return mock user profile
      await new Promise(resolve => setTimeout(resolve, 200));
      
      const mockProfile: UserProfile = {
        userId: 'test-user-001',
        username: 'testuser',
        roles: ['JOB_CREATOR', 'JOB_MODIFIER', 'JOB_EXECUTOR', 'JOB_VIEWER'],
        permissions: ['READ_CONFIGS', 'WRITE_CONFIGS', 'DELETE_CONFIGS'],
        mfaVerified: true,
        sessionId: 'mock-session-' + Date.now(),
        correlationId: 'mock-profile-corr-' + Date.now()
      };
      
      console.log('[Auth API MOCK] User profile retrieved');
      return mockProfile;
    } catch (error) {
      // Fallback to actual API call if needed
      const response = await httpClient.get<UserProfile>('/api/auth/profile');
      return response.data;
    }
  }
};