import axios, { AxiosError, AxiosRequestConfig, AxiosResponse } from 'axios';
import { generateDeviceFingerprint } from '../utils/deviceFingerprint';

/**
 * HTTP Client with JWT Authentication Integration
 * 
 * Enhanced HTTP client with comprehensive JWT token management,
 * automatic token refresh, device fingerprinting, and security
 * headers for enterprise-grade API communication.
 * 
 * Security Features:
 * - Automatic JWT token attachment
 * - Token refresh on 401 errors
 * - Device fingerprinting headers
 * - Correlation ID tracking
 * - Request/response logging
 * - Error handling and retry logic
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */

// Token storage keys
const TOKEN_STORAGE_KEY = 'fabric_access_token';
const REFRESH_TOKEN_STORAGE_KEY = 'fabric_refresh_token';

const httpClient = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080',
  timeout: 30000, // Increased timeout for enterprise applications
  headers: {
    'Content-Type': 'application/json',
  },
});

// Track refresh attempts to prevent infinite loops
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: any) => void;
  reject: (error?: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token);
    }
  });
  
  failedQueue = [];
};

// Request interceptor
httpClient.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Add device fingerprint for security tracking
    if (config.headers) {
      config.headers['X-Device-Fingerprint'] = generateDeviceFingerprint();
    }

    // Add correlation ID for request tracing
    if (config.headers) {
      config.headers['X-Correlation-ID'] = generateCorrelationId();
    }

    // Add request timestamp
    if (config.headers) {
      config.headers['X-Request-Time'] = new Date().toISOString();
    }

    console.log(`🌐 ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    console.error('❌ Request error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor with token refresh logic
httpClient.interceptors.response.use(
  (response: AxiosResponse) => {
    console.log(`✅ ${response.status} ${response.config.url}`);
    
    // Log correlation ID if present
    const correlationId = response.headers['x-correlation-id'];
    if (correlationId) {
      console.log(`🔗 Correlation ID: ${correlationId}`);
    }
    
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };
    
    console.error(`❌ ${error.response?.status || 'Network'} ${error.config?.url}:`, error.message);
    
    // Handle 401 Unauthorized errors with token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // If already refreshing, queue this request
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${token}`;
          }
          return httpClient(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
        
        if (!refreshToken) {
          throw new Error('No refresh token available');
        }

        // Attempt to refresh the token
        const response = await axios.post(`${httpClient.defaults.baseURL}/api/auth/refresh`, {
          refreshToken: refreshToken
        });

        const { accessToken } = response.data;
        
        // Update stored token
        localStorage.setItem(TOKEN_STORAGE_KEY, accessToken);
        
        // Update authorization header for the original request
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        }

        // Process queued requests
        processQueue(null, accessToken);
        
        console.log('✅ Token refreshed successfully');
        
        // Retry the original request
        return httpClient(originalRequest);
        
      } catch (refreshError) {
        console.error('❌ Token refresh failed:', refreshError);
        
        // Clear tokens and redirect to login
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
        localStorage.removeItem('fabric_user');
        localStorage.removeItem('fabric_session');
        
        // Process queued requests with error
        processQueue(refreshError, null);
        
        // Redirect to login page
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // Handle other error cases
    if (error.response?.status === 403) {
      console.warn('🚫 Access denied - insufficient permissions');
    } else if (error.response?.status === 429) {
      console.warn('⏳ Rate limit exceeded - please slow down requests');
    } else if (error.response?.status && error.response.status >= 500) {
      console.error('🚨 Server error - please try again later');
    }
    
    return Promise.reject(error);
  }
);

// Utility function to generate correlation IDs
function generateCorrelationId(): string {
  return 'req_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
}

// Export the configured HTTP client
export { httpClient };
export default httpClient;
