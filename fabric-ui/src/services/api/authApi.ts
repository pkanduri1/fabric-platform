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
    const response = await httpClient.post<LoginResponse>('/api/auth/login', credentials);
    return response.data;
  },

  /**
   * User logout
   */
  async logout(): Promise<LogoutResponse> {
    const response = await httpClient.post<LogoutResponse>('/api/auth/logout');
    return response.data;
  },

  /**
   * Refresh access token
   */
  async refreshToken(request: RefreshTokenRequest): Promise<RefreshTokenResponse> {
    const response = await httpClient.post<RefreshTokenResponse>('/api/auth/refresh', request);
    return response.data;
  },

  /**
   * Get current user profile
   */
  async getUserProfile(): Promise<UserProfile> {
    const response = await httpClient.get<UserProfile>('/api/auth/profile');
    return response.data;
  }
};