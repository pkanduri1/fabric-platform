/**
 * Test Utilities
 * 
 * Shared utilities and helper functions for testing React components
 * and services in the monitoring dashboard application.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

import React from 'react';
import { render, RenderOptions } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { BrowserRouter } from 'react-router-dom';

import { AuthProvider } from '../../contexts/AuthContext';
import { 
  ActiveJob, 
  Alert, 
  PerformanceMetrics, 
  SystemHealth,
  DashboardData,
  JobStatus,
  JobPriority,
  AlertSeverity,
  AlertType,
  TrendIndicator,
  WebSocketMessage,
  MonitoringError
} from '../../types/monitoring';

// Mock user for testing
export const createMockUser = (overrides: any = {}) => ({
  id: '1',
  username: 'testuser',
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  roles: ['MONITORING_USER'],
  permissions: ['VIEW_MONITORING', 'ACKNOWLEDGE_ALERTS'],
  createdAt: '2025-01-01T00:00:00Z',
  lastLogin: '2025-08-08T10:00:00Z',
  ...overrides
});

// Mock auth context
export const createMockAuthContext = (overrides: any = {}) => ({
  user: createMockUser(),
  accessToken: 'mock-jwt-token',
  refreshToken: 'mock-refresh-token',
  isAuthenticated: true,
  isLoading: false,
  error: null,
  login: jest.fn().mockResolvedValue(undefined),
  logout: jest.fn().mockResolvedValue(undefined),
  refreshAccessToken: jest.fn().mockResolvedValue('new-token'),
  hasRole: jest.fn((role: string) => ['MONITORING_USER', 'OPERATIONS_MANAGER', 'ADMIN'].includes(role)),
  hasPermission: jest.fn().mockReturnValue(true),
  updateProfile: jest.fn().mockResolvedValue(undefined),
  ...overrides
});

// Test data factories
export const createMockJob = (overrides: Partial<ActiveJob> = {}): ActiveJob => ({
  executionId: `job-${Math.random().toString(36).substr(2, 9)}`,
  jobName: 'Test Batch Job',
  sourceSystem: 'TestSystem',
  status: JobStatus.RUNNING,
  priority: JobPriority.NORMAL,
  startTime: '2025-08-08T10:00:00Z',
  estimatedEndTime: '2025-08-08T11:00:00Z',
  progress: 50,
  recordsProcessed: 1000,
  totalRecords: 2000,
  currentStage: 'Processing Records',
  throughputPerSecond: 100,
  errorCount: 0,
  warningCount: 0,
  performanceScore: 85,
  trendIndicator: TrendIndicator.STABLE,
  lastHeartbeat: '2025-08-08T10:05:00Z',
  assignedNode: 'node-1',
  correlationId: `corr-${Math.random().toString(36).substr(2, 9)}`,
  ...overrides
});

export const createMockAlert = (overrides: Partial<Alert> = {}): Alert => ({
  alertId: `alert-${Math.random().toString(36).substr(2, 9)}`,
  type: AlertType.ERROR_RATE,
  severity: AlertSeverity.WARNING,
  title: 'Test Alert',
  description: 'This is a test alert for monitoring',
  jobExecutionId: 'job-123',
  sourceSystem: 'TestSystem',
  threshold: 5.0,
  currentValue: 7.5,
  timestamp: '2025-08-08T10:00:00Z',
  acknowledged: false,
  acknowledgedBy: undefined,
  acknowledgedAt: undefined,
  resolved: false,
  resolvedAt: undefined,
  escalated: false,
  escalationLevel: undefined,
  correlationId: `alert-corr-${Math.random().toString(36).substr(2, 9)}`,
  affectedResources: ['job-123', 'worker-node-1'],
  ...overrides
});

export const createMockPerformanceMetrics = (overrides: Partial<PerformanceMetrics> = {}): PerformanceMetrics => ({
  totalThroughput: 1000,
  averageExecutionTime: 30000,
  successRate: 95.5,
  errorRate: 4.5,
  memoryUsage: 75,
  cpuUsage: 60,
  activeConnections: 25,
  queueDepth: 5,
  systemHealthScore: 85,
  timestamp: '2025-08-08T10:05:00Z',
  ...overrides
});

export const createMockSystemHealth = (overrides: Partial<SystemHealth> = {}): SystemHealth => ({
  overallScore: 85,
  database: {
    status: 'HEALTHY',
    responseTime: 50,
    connectionPool: 10
  },
  webSocket: {
    status: 'HEALTHY',
    activeConnections: 25,
    messageRate: 100
  },
  batchProcessing: {
    status: 'HEALTHY',
    activeJobs: 5,
    queueLength: 2
  },
  memory: {
    status: 'HEALTHY',
    used: 4096,
    available: 8192,
    percentage: 50
  },
  lastCheck: '2025-08-08T10:05:00Z',
  ...overrides
});

export const createMockDashboardData = (overrides: Partial<DashboardData> = {}): DashboardData => ({
  activeJobs: [
    createMockJob({ executionId: 'job-1', jobName: 'Import Job', status: JobStatus.RUNNING }),
    createMockJob({ executionId: 'job-2', jobName: 'Validation Job', status: JobStatus.PENDING }),
    createMockJob({ executionId: 'job-3', jobName: 'Export Job', status: JobStatus.COMPLETED })
  ],
  recentCompletions: [
    createMockJob({ 
      executionId: 'job-completed-1', 
      jobName: 'Completed Job', 
      status: JobStatus.COMPLETED,
      progress: 100
    })
  ],
  performanceMetrics: createMockPerformanceMetrics(),
  systemHealth: createMockSystemHealth(),
  alerts: [
    createMockAlert({ alertId: 'alert-1', severity: AlertSeverity.WARNING }),
    createMockAlert({ alertId: 'alert-2', severity: AlertSeverity.INFO, acknowledged: true })
  ],
  trends: {
    period: 'HOUR',
    startDate: '2025-08-08T09:00:00Z',
    endDate: '2025-08-08T10:00:00Z',
    jobExecutionTrends: Array.from({ length: 10 }, (_, i) => ({
      timestamp: new Date(Date.now() - (10 - i) * 60000).toISOString(),
      value: Math.random() * 100,
      label: `Point ${i}`
    })),
    throughputTrends: Array.from({ length: 10 }, (_, i) => ({
      timestamp: new Date(Date.now() - (10 - i) * 60000).toISOString(),
      value: 800 + Math.random() * 400,
      label: `Point ${i}`
    })),
    errorRateTrends: Array.from({ length: 10 }, (_, i) => ({
      timestamp: new Date(Date.now() - (10 - i) * 60000).toISOString(),
      value: Math.random() * 10,
      label: `Point ${i}`
    })),
    performanceScoreTrends: Array.from({ length: 10 }, (_, i) => ({
      timestamp: new Date(Date.now() - (10 - i) * 60000).toISOString(),
      value: 70 + Math.random() * 30,
      label: `Point ${i}`
    })),
    systemHealthTrends: Array.from({ length: 10 }, (_, i) => ({
      timestamp: new Date(Date.now() - (10 - i) * 60000).toISOString(),
      value: 80 + Math.random() * 20,
      label: `Point ${i}`
    }))
  },
  lastUpdate: '2025-08-08T10:05:00Z',
  correlationId: `dashboard-${Math.random().toString(36).substr(2, 9)}`,
  ...overrides
});

export const createMockWebSocketMessage = (overrides: Partial<WebSocketMessage> = {}): WebSocketMessage => ({
  type: 'DASHBOARD_UPDATE',
  payload: createMockDashboardData(),
  timestamp: new Date().toISOString(),
  correlationId: `ws-msg-${Math.random().toString(36).substr(2, 9)}`,
  ...overrides
});

export const createMockMonitoringError = (overrides: Partial<MonitoringError> = {}): MonitoringError => ({
  code: 'TEST_ERROR',
  message: 'Test error message',
  timestamp: new Date().toISOString(),
  recoverable: true,
  details: { source: 'test' },
  ...overrides
});

// Custom render function with providers
interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  initialEntries?: string[];
  authContext?: any;
  theme?: any;
}

export const renderWithProviders = (
  ui: React.ReactElement,
  {
    initialEntries = ['/'],
    authContext = createMockAuthContext(),
    theme = createTheme(),
    ...renderOptions
  }: CustomRenderOptions = {}
) => {
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <BrowserRouter>
      <ThemeProvider theme={theme}>
        <AuthProvider value={authContext}>
          {children}
        </AuthProvider>
      </ThemeProvider>
    </BrowserRouter>
  );

  return render(ui, { wrapper: Wrapper, ...renderOptions });
};

// Test utilities for WebSocket mocking
export class MockWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  readyState = MockWebSocket.CONNECTING;
  url: string;
  protocols?: string | string[];
  
  onopen: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;

  private eventListeners: { [key: string]: ((event: any) => void)[] } = {};
  private messageQueue: string[] = [];

  constructor(url: string, protocols?: string | string[]) {
    this.url = url;
    this.protocols = protocols;
  }

  send(data: string): void {
    if (this.readyState !== MockWebSocket.OPEN) {
      throw new Error('WebSocket is not open');
    }
    this.messageQueue.push(data);
  }

  close(code?: number, reason?: string): void {
    this.readyState = MockWebSocket.CLOSED;
    const closeEvent = new CloseEvent('close', {
      code: code || 1000,
      reason: reason || '',
      wasClean: code === 1000
    });
    setTimeout(() => this.onclose?.(closeEvent), 0);
  }

  addEventListener(type: string, listener: (event: any) => void): void {
    if (!this.eventListeners[type]) {
      this.eventListeners[type] = [];
    }
    this.eventListeners[type].push(listener);
  }

  removeEventListener(type: string, listener: (event: any) => void): void {
    if (this.eventListeners[type]) {
      const index = this.eventListeners[type].indexOf(listener);
      if (index !== -1) {
        this.eventListeners[type].splice(index, 1);
      }
    }
  }

  dispatchEvent(event: Event): boolean {
    const listeners = this.eventListeners[event.type] || [];
    listeners.forEach(listener => listener(event));
    return true;
  }

  // Test helper methods
  simulateOpen(): void {
    this.readyState = MockWebSocket.OPEN;
    setTimeout(() => this.onopen?.(new Event('open')), 0);
  }

  simulateMessage(data: any): void {
    if (this.readyState === MockWebSocket.OPEN) {
      const messageEvent = new MessageEvent('message', {
        data: typeof data === 'string' ? data : JSON.stringify(data)
      });
      setTimeout(() => this.onmessage?.(messageEvent), 0);
    }
  }

  simulateError(): void {
    setTimeout(() => this.onerror?.(new Event('error')), 0);
  }

  simulateClose(code = 1000, reason = ''): void {
    this.readyState = MockWebSocket.CLOSED;
    const closeEvent = new CloseEvent('close', { code, reason, wasClean: code === 1000 });
    setTimeout(() => this.onclose?.(closeEvent), 0);
  }

  getMessageQueue(): string[] {
    return [...this.messageQueue];
  }

  clearMessageQueue(): void {
    this.messageQueue = [];
  }
}

// Mock API responses
export const createMockApiResponse = (data: any, overrides: any = {}) => ({
  success: true,
  data,
  message: 'Operation completed successfully',
  timestamp: new Date().toISOString(),
  correlationId: `api-${Math.random().toString(36).substr(2, 9)}`,
  ...overrides
});

export const createMockApiError = (message = 'API Error', code = 'API_ERROR') => ({
  success: false,
  error: {
    code,
    message,
    timestamp: new Date().toISOString(),
    details: {}
  }
});

// Global mocks setup function
export const setupGlobalMocks = (): void => {
  // Mock window.matchMedia
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation(query => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: jest.fn(),
      removeListener: jest.fn(),
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      dispatchEvent: jest.fn(),
    })),
  });

  // Mock ResizeObserver
  global.ResizeObserver = jest.fn().mockImplementation(() => ({
    observe: jest.fn(),
    unobserve: jest.fn(),
    disconnect: jest.fn(),
  }));

  // Mock WebSocket
  global.WebSocket = MockWebSocket as any;

  // Mock localStorage
  const localStorageMock = (() => {
    let store: { [key: string]: string } = {};
    return {
      getItem: jest.fn((key: string) => store[key] || null),
      setItem: jest.fn((key: string, value: string) => {
        store[key] = value.toString();
      }),
      removeItem: jest.fn((key: string) => {
        delete store[key];
      }),
      clear: jest.fn(() => {
        store = {};
      })
    };
  })();
  Object.defineProperty(window, 'localStorage', { value: localStorageMock });

  // Mock sessionStorage
  Object.defineProperty(window, 'sessionStorage', { value: localStorageMock });

  // Mock fetch
  global.fetch = jest.fn();

  // Suppress console warnings in tests
  jest.spyOn(console, 'warn').mockImplementation(() => {});
  jest.spyOn(console, 'error').mockImplementation(() => {});
};

// Export all utilities
export * from '@testing-library/react';
export { renderWithProviders as render };