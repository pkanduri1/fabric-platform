/**
 * MonitoringDashboard Integration Tests
 * 
 * End-to-end integration tests for the MonitoringDashboard including
 * WebSocket connectivity, real-time updates, and cross-component interactions.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

import React from 'react';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { BrowserRouter } from 'react-router-dom';

import { MonitoringDashboard } from '../../pages/MonitoringDashboard/MonitoringDashboard';
import { AuthProvider } from '../../contexts/AuthContext';
import { 
  DashboardData, 
  ActiveJob, 
  JobStatus, 
  JobPriority,
  TrendIndicator,
  AlertType,
  AlertSeverity,
  WebSocketMessage 
} from '../../types/monitoring';

// Mock WebSocket for integration testing
class MockWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  readyState = MockWebSocket.CONNECTING;
  url: string;
  onopen: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;

  constructor(url: string) {
    this.url = url;
    
    // Simulate connection establishment
    setTimeout(() => {
      this.readyState = MockWebSocket.OPEN;
      this.onopen?.(new Event('open'));
    }, 100);
  }

  send(data: string) {
    // Mock successful send
  }

  close() {
    this.readyState = MockWebSocket.CLOSED;
    this.onclose?.(new CloseEvent('close', { code: 1000, wasClean: true }));
  }

  // Test helpers
  simulateMessage(data: any) {
    const event = new MessageEvent('message', {
      data: JSON.stringify(data)
    });
    this.onmessage?.(event);
  }

  simulateError() {
    this.onerror?.(new Event('error'));
  }
}

global.WebSocket = MockWebSocket as any;

// Mock API calls
const mockMonitoringApi = {
  getDashboardData: jest.fn(),
  acknowledgeAlert: jest.fn(),
  getJobDetails: jest.fn(),
  exportDashboardData: jest.fn()
};

jest.mock('../../services/api/monitoringApi', () => mockMonitoringApi);

// Mock audio
global.Audio = jest.fn().mockImplementation(() => ({
  play: jest.fn().mockResolvedValue(undefined),
  volume: 0.5
}));

// Test data factories
const createMockJob = (overrides: Partial<ActiveJob> = {}): ActiveJob => ({
  executionId: 'job-123',
  jobName: 'Test Batch Job',
  sourceSystem: 'TestSystem',
  status: JobStatus.RUNNING,
  priority: JobPriority.NORMAL,
  startTime: '2025-08-08T10:00:00Z',
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
  correlationId: 'corr-123',
  ...overrides
});

const createMockDashboardData = (): DashboardData => ({
  activeJobs: [
    createMockJob({ 
      executionId: 'job-1', 
      jobName: 'Import Customer Data', 
      status: JobStatus.RUNNING,
      progress: 75 
    }),
    createMockJob({ 
      executionId: 'job-2', 
      jobName: 'Validate Transactions', 
      status: JobStatus.PENDING,
      progress: 0 
    }),
    createMockJob({ 
      executionId: 'job-3', 
      jobName: 'Generate Reports', 
      status: JobStatus.FAILED,
      progress: 30,
      errorCount: 5 
    })
  ],
  recentCompletions: [],
  performanceMetrics: {
    totalThroughput: 1500,
    averageExecutionTime: 45000,
    successRate: 92.5,
    errorRate: 7.5,
    memoryUsage: 68,
    cpuUsage: 55,
    activeConnections: 12,
    queueDepth: 3,
    systemHealthScore: 88,
    timestamp: '2025-08-08T10:05:00Z'
  },
  systemHealth: {
    overallScore: 88,
    database: {
      status: 'HEALTHY',
      responseTime: 45,
      connectionPool: 15
    },
    webSocket: {
      status: 'HEALTHY',
      activeConnections: 12,
      messageRate: 150
    },
    batchProcessing: {
      status: 'HEALTHY',
      activeJobs: 3,
      queueLength: 2
    },
    memory: {
      status: 'HEALTHY',
      used: 6144,
      available: 16384,
      percentage: 68
    },
    lastCheck: '2025-08-08T10:05:00Z'
  },
  alerts: [
    {
      alertId: 'alert-1',
      type: AlertType.ERROR_RATE,
      severity: AlertSeverity.WARNING,
      title: 'High Error Rate in Job Processing',
      description: 'Error rate has exceeded 5% threshold',
      timestamp: '2025-08-08T10:03:00Z',
      acknowledged: false,
      resolved: false,
      escalated: false,
      correlationId: 'alert-corr-1',
      affectedResources: ['job-3']
    },
    {
      alertId: 'alert-2',
      type: AlertType.PERFORMANCE,
      severity: AlertSeverity.INFO,
      title: 'Performance Degradation Detected',
      description: 'System performance has decreased by 10%',
      timestamp: '2025-08-08T10:01:00Z',
      acknowledged: true,
      acknowledgedBy: 'operator',
      resolved: false,
      escalated: false,
      correlationId: 'alert-corr-2',
      affectedResources: ['system']
    }
  ],
  trends: {
    period: 'HOUR',
    startDate: '2025-08-08T09:00:00Z',
    endDate: '2025-08-08T10:00:00Z',
    jobExecutionTrends: [],
    throughputTrends: [],
    errorRateTrends: [],
    performanceScoreTrends: [],
    systemHealthTrends: []
  },
  lastUpdate: '2025-08-08T10:05:00Z',
  correlationId: 'dashboard-update-123'
});

// Test wrapper with providers
const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const theme = createTheme();
  
  const mockUser = {
    id: '1',
    username: 'testuser',
    email: 'test@example.com',
    roles: ['OPERATIONS_MANAGER']
  };

  const mockAuthValue = {
    user: mockUser,
    accessToken: 'mock-jwt-token',
    refreshToken: 'mock-refresh-token',
    isAuthenticated: true,
    isLoading: false,
    error: null,
    login: jest.fn(),
    logout: jest.fn(),
    refreshAccessToken: jest.fn(),
    hasRole: (role: string) => ['OPERATIONS_MANAGER', 'MONITORING_USER', 'ADMIN'].includes(role),
    hasPermission: jest.fn().mockReturnValue(true)
  };

  return (
    <BrowserRouter>
      <ThemeProvider theme={theme}>
        <AuthProvider value={mockAuthValue as any}>
          {children}
        </AuthProvider>
      </ThemeProvider>
    </BrowserRouter>
  );
};

describe('MonitoringDashboard Integration Tests', () => {
  let mockWebSocket: MockWebSocket;

  beforeEach(() => {
    // Setup API mocks
    mockMonitoringApi.getDashboardData.mockResolvedValue({
      success: true,
      data: createMockDashboardData(),
      timestamp: new Date().toISOString(),
      correlationId: 'api-123'
    });

    mockMonitoringApi.acknowledgeAlert.mockResolvedValue({
      success: true,
      message: 'Alert acknowledged successfully'
    });

    mockMonitoringApi.getJobDetails.mockResolvedValue({
      success: true,
      data: {
        executionId: 'job-1',
        jobName: 'Import Customer Data',
        status: JobStatus.RUNNING,
        startTime: '2025-08-08T10:00:00Z',
        performance: {},
        stages: [],
        errors: [],
        warnings: [],
        dataLineage: {},
        auditTrail: [],
        dependencies: []
      }
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
    if (mockWebSocket) {
      mockWebSocket.close();
    }
  });

  describe('Full Dashboard Flow', () => {
    it('should load dashboard and establish WebSocket connection', async () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      // Should show loading initially
      expect(screen.getByRole('progressbar')).toBeInTheDocument();

      // Wait for WebSocket connection and data loading
      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      }, { timeout: 5000 });

      // Should display job statistics
      expect(screen.getByText('1 Running')).toBeInTheDocument();
      expect(screen.getByText('1 Failed')).toBeInTheDocument();
      expect(screen.getByText('1 Pending')).toBeInTheDocument();

      // Should show system health
      expect(screen.getByText(/Health Score: 88/)).toBeInTheDocument();

      // Should display active jobs
      expect(screen.getByText('Import Customer Data')).toBeInTheDocument();
      expect(screen.getByText('Validate Transactions')).toBeInTheDocument();
      expect(screen.getByText('Generate Reports')).toBeInTheDocument();

      // Should show alerts
      expect(screen.getByText('High Error Rate in Job Processing')).toBeInTheDocument();
      expect(screen.getByText('1 unacked')).toBeInTheDocument();
    });

    it('should handle real-time job updates via WebSocket', async () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      // Wait for initial load
      await waitFor(() => {
        expect(screen.getByText('Import Customer Data')).toBeInTheDocument();
      });

      // Get WebSocket instance
      mockWebSocket = (global.WebSocket as any).mock.instances[0];

      // Simulate job progress update
      const progressUpdate: WebSocketMessage = {
        type: 'JOB_UPDATE',
        payload: {
          executionId: 'job-1',
          progress: 90,
          recordsProcessed: 1800,
          throughputPerSecond: 120
        },
        timestamp: new Date().toISOString(),
        correlationId: 'ws-update-1'
      };

      act(() => {
        mockWebSocket.simulateMessage(progressUpdate);
      });

      // Should update job progress in UI
      await waitFor(() => {
        expect(screen.getByText('90%')).toBeInTheDocument();
      });
    });

    it('should handle new alerts with sound notification', async () => {
      const mockAudio = jest.mocked(global.Audio);
      const mockPlay = jest.fn();
      mockAudio.mockImplementation(() => ({
        play: mockPlay,
        volume: 0.5
      } as any));

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      });

      mockWebSocket = (global.WebSocket as any).mock.instances[0];

      // Simulate critical alert
      const criticalAlert: WebSocketMessage = {
        type: 'ALERT',
        payload: {
          alertId: 'alert-critical-1',
          type: 'SYSTEM_HEALTH',
          severity: AlertSeverity.CRITICAL,
          title: 'System Failure Detected',
          description: 'Database connection lost',
          timestamp: new Date().toISOString(),
          acknowledged: false,
          resolved: false,
          escalated: false,
          correlationId: 'critical-alert-1',
          affectedResources: ['database']
        },
        timestamp: new Date().toISOString(),
        correlationId: 'ws-alert-1'
      };

      act(() => {
        mockWebSocket.simulateMessage(criticalAlert);
      });

      // Should show new alert
      await waitFor(() => {
        expect(screen.getByText('System Failure Detected')).toBeInTheDocument();
      });

      // Should update statistics
      expect(screen.getByText('2 Critical Alerts')).toBeInTheDocument();

      // Should play alert sound
      expect(mockPlay).toHaveBeenCalled();
    });
  });

  describe('User Interactions', () => {
    it('should handle job selection and details modal', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Import Customer Data')).toBeInTheDocument();
      });

      // Click on a job
      const jobElement = screen.getByText('Import Customer Data');
      await user.click(jobElement);

      // Should open job details modal
      await waitFor(() => {
        expect(screen.getByText('Job Details for: job-1')).toBeInTheDocument();
      });

      // Should call API for job details
      expect(mockMonitoringApi.getJobDetails).toHaveBeenCalledWith('job-1');
    });

    it('should handle alert acknowledgment', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('High Error Rate in Job Processing')).toBeInTheDocument();
      });

      // Click acknowledge button
      const acknowledgeButton = screen.getByText(/acknowledge/i);
      await user.click(acknowledgeButton);

      // Should call API
      expect(mockMonitoringApi.acknowledgeAlert).toHaveBeenCalledWith('alert-1');

      // Should show success notification
      await waitFor(() => {
        expect(screen.getByText('Alert acknowledged successfully')).toBeInTheDocument();
      });
    });

    it('should handle dashboard refresh', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      });

      // Clear previous API calls
      jest.clearAllMocks();

      // Click refresh button
      const refreshButton = screen.getByLabelText('Refresh Dashboard');
      await user.click(refreshButton);

      // Should call API again
      expect(mockMonitoringApi.getDashboardData).toHaveBeenCalled();

      // Should show success notification
      await waitFor(() => {
        expect(screen.getByText('Dashboard refreshed')).toBeInTheDocument();
      });
    });
  });

  describe('Error Handling', () => {
    it('should handle WebSocket connection failures', async () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      });

      // Simulate WebSocket error
      mockWebSocket = (global.WebSocket as any).mock.instances[0];
      act(() => {
        mockWebSocket.simulateError();
      });

      // Should show offline indicator
      await waitFor(() => {
        expect(screen.getByText('Offline')).toBeInTheDocument();
      });

      // Should show connection status FAB
      expect(screen.getByRole('button', { name: '' })).toBeInTheDocument(); // Warning FAB
    });

    it('should handle API failures gracefully', async () => {
      mockMonitoringApi.getDashboardData.mockRejectedValue(new Error('API Error'));

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      // Should show error notification
      await waitFor(() => {
        expect(screen.getByText(/Monitoring error/)).toBeInTheDocument();
      });
    });

    it('should recover from connection loss', async () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      });

      mockWebSocket = (global.WebSocket as any).mock.instances[0];

      // Simulate connection loss
      act(() => {
        mockWebSocket.close();
      });

      await waitFor(() => {
        expect(screen.getByText('Offline')).toBeInTheDocument();
      });

      // Wait for reconnection attempt
      await waitFor(() => {
        expect(screen.queryByText('Offline')).not.toBeInTheDocument();
      }, { timeout: 5000 });
    });
  });

  describe('Performance and Scaling', () => {
    it('should handle high-frequency updates efficiently', async () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      });

      mockWebSocket = (global.WebSocket as any).mock.instances[0];

      // Send multiple rapid updates
      const updates = Array.from({ length: 50 }, (_, i) => ({
        type: 'JOB_UPDATE',
        payload: {
          executionId: 'job-1',
          progress: 50 + i,
          recordsProcessed: 1000 + i * 10
        },
        timestamp: new Date().toISOString(),
        correlationId: `update-${i}`
      }));

      act(() => {
        updates.forEach(update => {
          mockWebSocket.simulateMessage(update);
        });
      });

      // Should handle updates without performance issues
      await waitFor(() => {
        expect(screen.getByText('99%')).toBeInTheDocument(); // Last update
      });
    });

    it('should maintain responsiveness with large datasets', async () => {
      const largeDataset = createMockDashboardData();
      
      // Create 100 active jobs
      largeDataset.activeJobs = Array.from({ length: 100 }, (_, i) =>
        createMockJob({
          executionId: `job-${i}`,
          jobName: `Batch Job ${i}`,
          status: i % 3 === 0 ? JobStatus.RUNNING : 
                 i % 3 === 1 ? JobStatus.PENDING : JobStatus.COMPLETED
        })
      );

      mockMonitoringApi.getDashboardData.mockResolvedValue({
        success: true,
        data: largeDataset,
        timestamp: new Date().toISOString(),
        correlationId: 'large-dataset'
      });

      const startTime = performance.now();
      
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Batch Job 0')).toBeInTheDocument();
      });

      const endTime = performance.now();
      
      // Should render within reasonable time (less than 2 seconds)
      expect(endTime - startTime).toBeLessThan(2000);

      // Should show correct statistics
      expect(screen.getByText(/33 Running/)).toBeInTheDocument();
      expect(screen.getByText(/33 Pending/)).toBeInTheDocument();
      expect(screen.getByText(/34 Completed/)).toBeInTheDocument(); // 100/3 rounded
    });
  });

  describe('Cross-Component Integration', () => {
    it('should synchronize data between components', async () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      });

      mockWebSocket = (global.WebSocket as any).mock.instances[0];

      // Update that affects both job status and performance metrics
      const systemUpdate: WebSocketMessage = {
        type: 'DASHBOARD_UPDATE',
        payload: {
          ...createMockDashboardData(),
          performanceMetrics: {
            ...createMockDashboardData().performanceMetrics,
            totalThroughput: 2000,
            systemHealthScore: 95
          },
          systemHealth: {
            ...createMockDashboardData().systemHealth,
            overallScore: 95
          }
        },
        timestamp: new Date().toISOString(),
        correlationId: 'system-update'
      };

      act(() => {
        mockWebSocket.simulateMessage(systemUpdate);
      });

      // Should update multiple components
      await waitFor(() => {
        expect(screen.getByText('Health Score: 95')).toBeInTheDocument();
      });

      // Performance chart should reflect new data
      expect(screen.getByTestId('performance-metrics-chart')).toBeInTheDocument();
    });

    it('should handle filtering across components', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      });

      // Open filters
      const filterButton = screen.getByLabelText('Filter Options');
      await user.click(filterButton);

      // Apply status filter
      const statusFilter = screen.getByLabelText(/status filter/i);
      await user.click(statusFilter);

      const runningOption = screen.getByText('Running Only');
      await user.click(runningOption);

      // Should filter jobs in grid
      await waitFor(() => {
        expect(screen.getByText('Import Customer Data')).toBeInTheDocument();
        expect(screen.queryByText('Validate Transactions')).not.toBeInTheDocument();
      });

      // Should update statistics
      expect(screen.getByText('1 Running')).toBeInTheDocument();
      expect(screen.queryByText('1 Pending')).not.toBeInTheDocument();
    });
  });

  describe('Accessibility and Mobile', () => {
    it('should support keyboard navigation', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      });

      // Should be able to navigate with tab key
      await user.tab();
      expect(screen.getByLabelText('Refresh Dashboard')).toHaveFocus();

      await user.tab();
      expect(screen.getByLabelText('Filter Options')).toHaveFocus();

      // Should be able to activate with Enter
      await user.keyboard('{Enter}');
      
      // Filter dialog should open
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
    });

    it('should work on mobile viewports', async () => {
      // Mock mobile viewport
      Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation(query => ({
          matches: query.includes('(max-width: 960px)'),
          media: query,
          onchange: null,
          addListener: jest.fn(),
          removeListener: jest.fn(),
        })),
      });

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      });

      // Should render with mobile-responsive layout
      const dashboard = screen.getByTestId('monitoring-dashboard');
      expect(dashboard).toHaveClass(/mobile/);

      // Should still show all essential information
      expect(screen.getByText('Import Customer Data')).toBeInTheDocument();
      expect(screen.getByText('High Error Rate in Job Processing')).toBeInTheDocument();
    });

    it('should announce updates to screen readers', async () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      });

      // Should have live region for announcements
      const liveRegion = screen.getByRole('status');
      expect(liveRegion).toHaveAttribute('aria-live', 'polite');

      mockWebSocket = (global.WebSocket as any).mock.instances[0];

      // Simulate job completion
      const completionUpdate: WebSocketMessage = {
        type: 'JOB_UPDATE',
        payload: {
          executionId: 'job-1',
          status: JobStatus.COMPLETED,
          progress: 100
        },
        timestamp: new Date().toISOString(),
        correlationId: 'completion-update'
      };

      act(() => {
        mockWebSocket.simulateMessage(completionUpdate);
      });

      // Should announce the update
      await waitFor(() => {
        expect(liveRegion).toHaveTextContent(/job completed/i);
      });
    });
  });
});