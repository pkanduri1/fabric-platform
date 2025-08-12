/**
 * Monitoring API Service
 * 
 * HTTP API client for job monitoring dashboard functionality.
 * Provides RESTful endpoints for dashboard data, job details,
 * alerts, and configuration management.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 * 
 * Features:
 * - Dashboard data retrieval
 * - Job execution details
 * - Alert management
 * - Historical analytics
 * - Configuration management
 * - Performance metrics
 * - Error handling and retry logic
 * - Request/response correlation
 */

import axios, { AxiosResponse, AxiosError } from 'axios';
import { 
  DashboardData,
  ActiveJob,
  JobExecutionDetails,
  Alert,
  AlertConfiguration,
  AlertFilters,
  PerformanceMetrics,
  SystemHealth,
  HistoricalMetrics,
  MonitoringConfiguration,
  MonitoringApiResponse,
  PaginatedResponse,
  MonitoringError
} from '../../types/monitoring';

// API configuration
const API_BASE_URL = '/api/monitoring';
const API_TIMEOUT = 30000;

// Create axios instance with default configuration
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
    'X-Client-Type': 'fabric-ui',
    'X-Client-Version': '1.0'
  }
});

// Request interceptor for authentication and correlation IDs
apiClient.interceptors.request.use(
  (config) => {
    // Add authentication token
    const token = localStorage.getItem('fabric_access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Add correlation ID for request tracking
    const correlationId = generateCorrelationId();
    config.headers['X-Correlation-ID'] = correlationId;
    
    // Add timestamp
    config.headers['X-Request-Timestamp'] = new Date().toISOString();
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling and response processing
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    // Log successful responses in development
    if (process.env.NODE_ENV === 'development') {
      console.log(`API Success [${response.config.method?.toUpperCase()}] ${response.config.url}:`, response.data);
    }
    return response;
  },
  (error: AxiosError) => {
    // Handle authentication errors
    if (error.response?.status === 401) {
      // Token expired or invalid - trigger re-authentication
      localStorage.removeItem('fabric_access_token');
      window.location.href = '/login';
      return Promise.reject(error);
    }
    
    // Log errors in development
    if (process.env.NODE_ENV === 'development') {
      console.error(`API Error [${error.config?.method?.toUpperCase()}] ${error.config?.url}:`, error.response?.data || error.message);
    }
    
    // Transform to MonitoringError
    const monitoringError: MonitoringError = transformToMonitoringError(error);
    return Promise.reject(monitoringError);
  }
);

/**
 * Transform axios error to MonitoringError
 */
function transformToMonitoringError(error: AxiosError): MonitoringError {
  const response = error.response;
  const correlationId = response?.headers['x-correlation-id'] || 'unknown';
  const responseData = response?.data as any;
  
  return {
    code: responseData?.code || `HTTP_${response?.status}` || 'NETWORK_ERROR',
    message: responseData?.message || error.message || 'An error occurred',
    timestamp: new Date().toISOString(),
    recoverable: response?.status !== 401 && response?.status !== 403,
    details: {
      status: response?.status,
      statusText: response?.statusText,
      url: error.config?.url,
      method: error.config?.method,
      correlationId,
      responseData: response?.data
    }
  };
}

/**
 * Generate correlation ID for request tracking
 */
function generateCorrelationId(): string {
  return `api_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Monitoring API Service Class
 */
export class MonitoringApiService {
  
  /**
   * Get dashboard data with active jobs, metrics, and alerts
   */
  async getDashboardData(): Promise<DashboardData> {
    const response = await apiClient.get<MonitoringApiResponse<DashboardData>>('/dashboard');
    return response.data.data;
  }
  
  /**
   * Get active jobs with optional filtering
   */
  async getActiveJobs(filters?: {
    sourceSystem?: string[];
    jobTypes?: string[];
    status?: string[];
    limit?: number;
  }): Promise<ActiveJob[]> {
    const params = new URLSearchParams();
    
    if (filters?.sourceSystem) {
      filters.sourceSystem.forEach(system => params.append('sourceSystem', system));
    }
    if (filters?.jobTypes) {
      filters.jobTypes.forEach(type => params.append('jobType', type));
    }
    if (filters?.status) {
      filters.status.forEach(status => params.append('status', status));
    }
    if (filters?.limit) {
      params.append('limit', filters.limit.toString());
    }
    
    const response = await apiClient.get<MonitoringApiResponse<ActiveJob[]>>(`/jobs/active?${params}`);
    return response.data.data;
  }
  
  /**
   * Get detailed information about a specific job execution
   */
  async getJobDetails(executionId: string): Promise<JobExecutionDetails> {
    const response = await apiClient.get<MonitoringApiResponse<JobExecutionDetails>>(`/jobs/${executionId}/details`);
    return response.data.data;
  }
  
  /**
   * Get job execution history for a specific job
   */
  async getJobHistory(
    jobName: string, 
    sourceSystem: string, 
    page: number = 0, 
    size: number = 20
  ): Promise<PaginatedResponse<JobExecutionDetails>> {
    const response = await apiClient.get<MonitoringApiResponse<PaginatedResponse<JobExecutionDetails>>>(
      `/jobs/history?jobName=${encodeURIComponent(jobName)}&sourceSystem=${encodeURIComponent(sourceSystem)}&page=${page}&size=${size}`
    );
    return response.data.data;
  }
  
  /**
   * Get current performance metrics
   */
  async getPerformanceMetrics(): Promise<PerformanceMetrics> {
    const response = await apiClient.get<MonitoringApiResponse<PerformanceMetrics>>('/metrics/performance');
    return response.data.data;
  }
  
  /**
   * Get system health information
   */
  async getSystemHealth(): Promise<SystemHealth> {
    const response = await apiClient.get<MonitoringApiResponse<SystemHealth>>('/health/system');
    return response.data.data;
  }
  
  /**
   * Get historical metrics for trending analysis
   */
  async getHistoricalMetrics(
    period: 'HOUR' | 'DAY' | 'WEEK' | 'MONTH',
    startDate: string,
    endDate: string
  ): Promise<HistoricalMetrics> {
    const params = new URLSearchParams({
      period,
      startDate,
      endDate
    });
    
    const response = await apiClient.get<MonitoringApiResponse<HistoricalMetrics>>(`/metrics/historical?${params}`);
    return response.data.data;
  }
  
  /**
   * Get alerts with optional filtering and pagination
   */
  async getAlerts(
    filters?: AlertFilters,
    page: number = 0,
    size: number = 50
  ): Promise<PaginatedResponse<Alert>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString()
    });
    
    if (filters?.severity) {
      filters.severity.forEach(severity => params.append('severity', severity));
    }
    if (filters?.type) {
      filters.type.forEach(type => params.append('type', type));
    }
    if (filters?.sourceSystem) {
      filters.sourceSystem.forEach(system => params.append('sourceSystem', system));
    }
    if (filters?.acknowledged !== undefined) {
      params.append('acknowledged', filters.acknowledged.toString());
    }
    if (filters?.resolved !== undefined) {
      params.append('resolved', filters.resolved.toString());
    }
    if (filters?.dateRange) {
      params.append('startDate', filters.dateRange.start);
      params.append('endDate', filters.dateRange.end);
    }
    
    const response = await apiClient.get<MonitoringApiResponse<PaginatedResponse<Alert>>>(`/alerts?${params}`);
    return response.data.data;
  }
  
  /**
   * Acknowledge an alert
   */
  async acknowledgeAlert(alertId: string, comment?: string): Promise<void> {
    await apiClient.post(`/alerts/${alertId}/acknowledge`, {
      comment,
      timestamp: new Date().toISOString()
    });
  }
  
  /**
   * Resolve an alert
   */
  async resolveAlert(alertId: string, resolution: string): Promise<void> {
    await apiClient.post(`/alerts/${alertId}/resolve`, {
      resolution,
      timestamp: new Date().toISOString()
    });
  }
  
  /**
   * Get alert configurations
   */
  async getAlertConfigurations(): Promise<AlertConfiguration[]> {
    const response = await apiClient.get<MonitoringApiResponse<AlertConfiguration[]>>('/alerts/configurations');
    return response.data.data;
  }
  
  /**
   * Create new alert configuration
   */
  async createAlertConfiguration(config: Omit<AlertConfiguration, 'configId' | 'createdAt' | 'createdBy'>): Promise<AlertConfiguration> {
    const response = await apiClient.post<MonitoringApiResponse<AlertConfiguration>>('/alerts/configurations', config);
    return response.data.data;
  }
  
  /**
   * Update alert configuration
   */
  async updateAlertConfiguration(configId: string, config: Partial<AlertConfiguration>): Promise<AlertConfiguration> {
    const response = await apiClient.put<MonitoringApiResponse<AlertConfiguration>>(`/alerts/configurations/${configId}`, config);
    return response.data.data;
  }
  
  /**
   * Delete alert configuration
   */
  async deleteAlertConfiguration(configId: string): Promise<void> {
    await apiClient.delete(`/alerts/configurations/${configId}`);
  }
  
  /**
   * Get monitoring configuration for current user
   */
  async getMonitoringConfiguration(): Promise<MonitoringConfiguration> {
    const response = await apiClient.get<MonitoringApiResponse<MonitoringConfiguration>>('/configuration');
    return response.data.data;
  }
  
  /**
   * Update monitoring configuration
   */
  async updateMonitoringConfiguration(config: Partial<MonitoringConfiguration>): Promise<MonitoringConfiguration> {
    const response = await apiClient.put<MonitoringApiResponse<MonitoringConfiguration>>('/configuration', config);
    return response.data.data;
  }
  
  /**
   * Cancel a running job
   */
  async cancelJob(executionId: string, reason: string): Promise<void> {
    await apiClient.post(`/jobs/${executionId}/cancel`, {
      reason,
      timestamp: new Date().toISOString()
    });
  }
  
  /**
   * Pause a running job
   */
  async pauseJob(executionId: string, reason: string): Promise<void> {
    await apiClient.post(`/jobs/${executionId}/pause`, {
      reason,
      timestamp: new Date().toISOString()
    });
  }
  
  /**
   * Resume a paused job
   */
  async resumeJob(executionId: string): Promise<void> {
    await apiClient.post(`/jobs/${executionId}/resume`, {
      timestamp: new Date().toISOString()
    });
  }
  
  /**
   * Retry a failed job
   */
  async retryJob(executionId: string): Promise<void> {
    await apiClient.post(`/jobs/${executionId}/retry`, {
      timestamp: new Date().toISOString()
    });
  }
  
  /**
   * Get job logs
   */
  async getJobLogs(
    executionId: string, 
    level?: string, 
    limit?: number,
    offset?: number
  ): Promise<{
    logs: Array<{
      timestamp: string;
      level: string;
      message: string;
      logger: string;
      correlationId?: string;
    }>;
    totalCount: number;
    hasMore: boolean;
  }> {
    const params = new URLSearchParams();
    if (level) params.append('level', level);
    if (limit) params.append('limit', limit.toString());
    if (offset) params.append('offset', offset.toString());
    
    const response = await apiClient.get<MonitoringApiResponse<any>>(`/jobs/${executionId}/logs?${params}`);
    return response.data.data;
  }
  
  /**
   * Export dashboard data to various formats
   */
  async exportDashboardData(
    format: 'CSV' | 'EXCEL' | 'PDF',
    filters?: {
      startDate: string;
      endDate: string;
      includeJobs?: boolean;
      includeAlerts?: boolean;
      includeMetrics?: boolean;
    }
  ): Promise<Blob> {
    const params = new URLSearchParams({ format });
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined) {
          params.append(key, value.toString());
        }
      });
    }
    
    const response = await apiClient.get(`/export?${params}`, {
      responseType: 'blob'
    });
    
    return response.data;
  }
  
  /**
   * Get monitoring statistics
   */
  async getMonitoringStatistics(period: 'TODAY' | 'WEEK' | 'MONTH'): Promise<{
    totalJobs: number;
    successfulJobs: number;
    failedJobs: number;
    averageExecutionTime: number;
    totalRecordsProcessed: number;
    alertsGenerated: number;
    systemUptimePercentage: number;
  }> {
    const response = await apiClient.get<MonitoringApiResponse<any>>(`/statistics?period=${period}`);
    return response.data.data;
  }
  
  /**
   * Test WebSocket connectivity
   */
  async testWebSocketConnection(): Promise<{
    supported: boolean;
    endpoint: string;
    latency?: number;
  }> {
    const response = await apiClient.get<MonitoringApiResponse<any>>('/websocket/test');
    return response.data.data;
  }
}

// Export singleton instance
export const monitoringApi = new MonitoringApiService();

// MonitoringApiService class is already exported above