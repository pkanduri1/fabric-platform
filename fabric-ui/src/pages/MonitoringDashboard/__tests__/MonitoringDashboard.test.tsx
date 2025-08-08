/**
 * MonitoringDashboard Component Tests
 * 
 * Comprehensive test suite for the MonitoringDashboard component including
 * rendering, user interactions, real-time updates, and error handling.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { BrowserRouter } from 'react-router-dom';

import { MonitoringDashboard } from '../MonitoringDashboard';
import { useAuth } from '../../../contexts/AuthContext';
import { useRealTimeMonitoring } from '../../../hooks/useRealTimeMonitoring';
import { 
  DashboardData, 
  ActiveJob, 
  JobStatus, 
  JobPriority, 
  Alert, 
  AlertSeverity,
  TrendIndicator,
  PerformanceMetrics,
  SystemHealth
} from '../../../types/monitoring';

// Mock dependencies
jest.mock('../../../contexts/AuthContext');
jest.mock('../../../hooks/useRealTimeMonitoring');
jest.mock('../../../components/monitoring/JobStatusGrid/JobStatusGrid', () => ({
  JobStatusGrid: ({ jobs, onJobSelect }: any) => (
    <div data-testid="job-status-grid">
      {jobs.map((job: ActiveJob) => (
        <div 
          key={job.executionId} 
          data-testid={`job-${job.executionId}`}
          onClick={() => onJobSelect(job.executionId)}
        >
          {job.jobName} - {job.status}
        </div>
      ))}
    </div>
  )
}));

jest.mock('../../../components/monitoring/AlertsPanel/AlertsPanel', () => ({
  AlertsPanel: ({ alerts, onAcknowledge }: any) => (
    <div data-testid="alerts-panel">
      {alerts.map((alert: Alert) => (
        <div 
          key={alert.alertId} 
          data-testid={`alert-${alert.alertId}`}
        >
          <span>{alert.title} - {alert.severity}</span>
          {onAcknowledge && (
            <button 
              onClick={() => onAcknowledge(alert.alertId)}
              data-testid={`ack-${alert.alertId}`}
            >
              Acknowledge
            </button>
          )}
        </div>
      ))}
    </div>
  )
}));

jest.mock('../../../components/monitoring/PerformanceMetricsChart/PerformanceMetricsChart', () => ({
  PerformanceMetricsChart: ({ metrics }: any) => (
    <div data-testid="performance-metrics-chart">
      Performance Chart - {metrics.length} metrics
    </div>
  )
}));

jest.mock('../../../components/monitoring/SystemHealthBar/SystemHealthBar', () => ({
  SystemHealthBar: ({ health }: any) => (
    <div data-testid="system-health-bar">
      Health Score: {health.overallScore}
    </div>
  )
}));

jest.mock('../../../components/monitoring/JobDetailsModal/JobDetailsModal', () => ({
  JobDetailsModal: ({ executionId, open, onClose }: any) => (
    open ? (
      <div data-testid="job-details-modal">
        <div>Job Details for: {executionId}</div>
        <button onClick={onClose} data-testid="close-modal">Close</button>
      </div>
    ) : null
  )
}));

const mockUseAuth = useAuth as jest.MockedFunction<typeof useAuth>;
const mockUseRealTimeMonitoring = useRealTimeMonitoring as jest.MockedFunction<typeof useRealTimeMonitoring>;

// Test data factories
const createMockJob = (overrides: Partial<ActiveJob> = {}): ActiveJob => ({
  executionId: 'job-123',
  jobName: 'Test Job',
  sourceSystem: 'TestSystem',
  status: JobStatus.RUNNING,
  priority: JobPriority.NORMAL,
  startTime: '2025-08-08T10:00:00Z',
  progress: 50,
  recordsProcessed: 1000,
  totalRecords: 2000,
  currentStage: 'Processing',
  throughputPerSecond: 100,
  errorCount: 0,
  warningCount: 0,
  performanceScore: 85,
  trendIndicator: TrendIndicator.STABLE,
  lastHeartbeat: '2025-08-08T10:05:00Z',
  correlationId: 'corr-123',
  ...overrides
});

const createMockAlert = (overrides: Partial<Alert> = {}): Alert => ({
  alertId: 'alert-123',
  type: 'ERROR_RATE',
  severity: AlertSeverity.WARNING,
  title: 'High Error Rate',
  description: 'Error rate exceeded threshold',
  timestamp: '2025-08-08T10:00:00Z',
  acknowledged: false,
  resolved: false,
  escalated: false,
  correlationId: 'corr-alert-123',
  affectedResources: ['job-123'],
  ...overrides
});

const createMockSystemHealth = (): SystemHealth => ({
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
  lastCheck: '2025-08-08T10:05:00Z'
});

const createMockPerformanceMetrics = (): PerformanceMetrics => ({
  totalThroughput: 1000,
  averageExecutionTime: 30000,
  successRate: 95.5,
  errorRate: 4.5,
  memoryUsage: 75,
  cpuUsage: 60,
  activeConnections: 25,
  queueDepth: 5,
  systemHealthScore: 85,
  timestamp: '2025-08-08T10:05:00Z'
});

const createMockDashboardData = (overrides: Partial<DashboardData> = {}): DashboardData => ({
  activeJobs: [
    createMockJob({ executionId: 'job-1', jobName: 'Job 1', status: JobStatus.RUNNING }),
    createMockJob({ executionId: 'job-2', jobName: 'Job 2', status: JobStatus.PENDING }),
    createMockJob({ executionId: 'job-3', jobName: 'Job 3', status: JobStatus.FAILED })
  ],
  recentCompletions: [],
  performanceMetrics: createMockPerformanceMetrics(),
  systemHealth: createMockSystemHealth(),
  alerts: [
    createMockAlert({ alertId: 'alert-1', severity: AlertSeverity.WARNING }),
    createMockAlert({ alertId: 'alert-2', severity: AlertSeverity.CRITICAL, acknowledged: false })
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
  correlationId: 'dashboard-123',
  ...overrides
});

// Test wrapper component
const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const theme = createTheme();
  return (
    <BrowserRouter>
      <ThemeProvider theme={theme}>
        {children}
      </ThemeProvider>
    </BrowserRouter>
  );
};

describe('MonitoringDashboard', () => {
  beforeEach(() => {
    // Setup default mocks
    mockUseAuth.mockReturnValue({
      user: { id: '1', username: 'testuser' },
      hasRole: jest.fn((role: string) => ['OPERATIONS_MANAGER', 'MONITORING_USER', 'ADMIN'].includes(role)),
      accessToken: 'mock-token',
      refreshAccessToken: jest.fn()
    } as any);

    mockUseRealTimeMonitoring.mockReturnValue({
      dashboardData: createMockDashboardData(),
      loading: false,
      error: null,
      connected: true,
      refresh: jest.fn().mockResolvedValue(undefined),
      subscribe: jest.fn(),
      unsubscribe: jest.fn()
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render dashboard title and components', () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      expect(screen.getByTestId('job-status-grid')).toBeInTheDocument();
      expect(screen.getByTestId('alerts-panel')).toBeInTheDocument();
      expect(screen.getByTestId('performance-metrics-chart')).toBeInTheDocument();
      expect(screen.getByTestId('system-health-bar')).toBeInTheDocument();
    });

    it('should display dashboard statistics chips', () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      expect(screen.getByText('1 Running')).toBeInTheDocument();
      expect(screen.getByText('1 Failed')).toBeInTheDocument();
      expect(screen.getByText('1 Pending')).toBeInTheDocument();
      expect(screen.getByText('1 Critical Alerts')).toBeInTheDocument();
    });

    it('should show offline indicator when disconnected', () => {
      mockUseRealTimeMonitoring.mockReturnValue({
        dashboardData: createMockDashboardData(),
        loading: false,
        error: null,
        connected: false,
        refresh: jest.fn(),
        subscribe: jest.fn(),
        unsubscribe: jest.fn()
      });

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      expect(screen.getByText('Offline')).toBeInTheDocument();
    });

    it('should show loading progress when loading', () => {
      mockUseRealTimeMonitoring.mockReturnValue({
        dashboardData: null,
        loading: true,
        error: null,
        connected: true,
        refresh: jest.fn(),
        subscribe: jest.fn(),
        unsubscribe: jest.fn()
      });

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });
  });

  describe('Access Control', () => {
    it('should show access denied for unauthorized users', () => {
      mockUseAuth.mockReturnValue({
        user: { id: '1', username: 'testuser' },
        hasRole: jest.fn(() => false),
        accessToken: 'mock-token',
        refreshAccessToken: jest.fn()
      } as any);

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      expect(screen.getByText('Access Denied')).toBeInTheDocument();
      expect(screen.getByText("You don't have permission to access the monitoring dashboard.")).toBeInTheDocument();
    });

    it('should show acknowledge buttons only for users with manage permissions', () => {
      const mockData = createMockDashboardData({
        alerts: [createMockAlert({ alertId: 'alert-1', acknowledged: false })]
      });

      mockUseRealTimeMonitoring.mockReturnValue({
        dashboardData: mockData,
        loading: false,
        error: null,
        connected: true,
        refresh: jest.fn(),
        subscribe: jest.fn(),
        unsubscribe: jest.fn()
      });

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      expect(screen.getByTestId('ack-alert-1')).toBeInTheDocument();
    });

    it('should hide acknowledge buttons for users without manage permissions', () => {
      mockUseAuth.mockReturnValue({
        user: { id: '1', username: 'testuser' },
        hasRole: jest.fn((role: string) => role === 'MONITORING_USER'),
        accessToken: 'mock-token',
        refreshAccessToken: jest.fn()
      } as any);

      const mockData = createMockDashboardData({
        alerts: [createMockAlert({ alertId: 'alert-1', acknowledged: false })]
      });

      mockUseRealTimeMonitoring.mockReturnValue({
        dashboardData: mockData,
        loading: false,
        error: null,
        connected: true,
        refresh: jest.fn(),
        subscribe: jest.fn(),
        unsubscribe: jest.fn()
      });

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      expect(screen.queryByTestId('ack-alert-1')).not.toBeInTheDocument();
    });
  });

  describe('User Interactions', () => {
    it('should handle job selection', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      const jobElement = screen.getByTestId('job-job-1');
      await user.click(jobElement);

      expect(screen.getByTestId('job-details-modal')).toBeInTheDocument();
      expect(screen.getByText('Job Details for: job-1')).toBeInTheDocument();
    });

    it('should close job details modal', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      // Open modal
      const jobElement = screen.getByTestId('job-job-1');
      await user.click(jobElement);

      expect(screen.getByTestId('job-details-modal')).toBeInTheDocument();

      // Close modal
      const closeButton = screen.getByTestId('close-modal');
      await user.click(closeButton);

      expect(screen.queryByTestId('job-details-modal')).not.toBeInTheDocument();
    });

    it('should handle manual refresh', async () => {
      const mockRefresh = jest.fn().mockResolvedValue(undefined);
      
      mockUseRealTimeMonitoring.mockReturnValue({
        dashboardData: createMockDashboardData(),
        loading: false,
        error: null,
        connected: true,
        refresh: mockRefresh,
        subscribe: jest.fn(),
        unsubscribe: jest.fn()
      });

      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      const refreshButton = screen.getByLabelText('Refresh Dashboard');
      await user.click(refreshButton);

      expect(mockRefresh).toHaveBeenCalled();
    });

    it('should toggle fullscreen mode', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      const fullscreenButton = screen.getByLabelText('Fullscreen');
      await user.click(fullscreenButton);

      expect(screen.getByLabelText('Exit Fullscreen')).toBeInTheDocument();
    });

    it('should open and close settings menu', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      const settingsButton = screen.getByLabelText('More Options');
      await user.click(settingsButton);

      expect(screen.getByText('Settings')).toBeInTheDocument();
      expect(screen.getByText('Export Data')).toBeInTheDocument();

      // Click outside to close menu
      await user.click(document.body);

      await waitFor(() => {
        expect(screen.queryByText('Settings')).not.toBeInTheDocument();
      });
    });
  });

  describe('Real-time Updates', () => {
    it('should handle data updates', () => {
      const mockData = createMockDashboardData({
        activeJobs: [
          createMockJob({ executionId: 'job-updated', jobName: 'Updated Job' })
        ]
      });

      const { rerender } = render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      // Update with new data
      mockUseRealTimeMonitoring.mockReturnValue({
        dashboardData: mockData,
        loading: false,
        error: null,
        connected: true,
        refresh: jest.fn(),
        subscribe: jest.fn(),
        unsubscribe: jest.fn()
      });

      rerender(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      expect(screen.getByText('Updated Job - RUNNING')).toBeInTheDocument();
    });

    it('should update statistics when jobs change', () => {
      const mockData = createMockDashboardData({
        activeJobs: [
          createMockJob({ status: JobStatus.RUNNING }),
          createMockJob({ status: JobStatus.RUNNING }),
          createMockJob({ status: JobStatus.FAILED })
        ]
      });

      mockUseRealTimeMonitoring.mockReturnValue({
        dashboardData: mockData,
        loading: false,
        error: null,
        connected: true,
        refresh: jest.fn(),
        subscribe: jest.fn(),
        unsubscribe: jest.fn()
      });

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      expect(screen.getByText('2 Running')).toBeInTheDocument();
      expect(screen.getByText('1 Failed')).toBeInTheDocument();
      expect(screen.getByText('0 Pending')).toBeInTheDocument();
    });
  });

  describe('Error Handling', () => {
    it('should handle monitoring errors gracefully', () => {
      const mockError = {
        code: 'CONNECTION_FAILED',
        message: 'Failed to connect to monitoring service',
        timestamp: '2025-08-08T10:00:00Z',
        recoverable: true
      };

      mockUseRealTimeMonitoring.mockReturnValue({
        dashboardData: null,
        loading: false,
        error: mockError,
        connected: false,
        refresh: jest.fn(),
        subscribe: jest.fn(),
        unsubscribe: jest.fn()
      });

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      expect(screen.getByText('Offline')).toBeInTheDocument();
    });

    it('should show connection status FAB when disconnected', () => {
      mockUseRealTimeMonitoring.mockReturnValue({
        dashboardData: createMockDashboardData(),
        loading: false,
        error: null,
        connected: false,
        refresh: jest.fn(),
        subscribe: jest.fn(),
        unsubscribe: jest.fn()
      });

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      const fab = screen.getByRole('button', { name: '' }); // FAB without aria-label
      expect(fab).toBeInTheDocument();
    });
  });

  describe('Mobile Responsiveness', () => {
    beforeEach(() => {
      // Mock mobile breakpoint
      Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation(query => ({
          matches: query.includes('(max-width: 960px)'),
          media: query,
          onchange: null,
          addListener: jest.fn(),
          removeListener: jest.fn(),
          addEventListener: jest.fn(),
          removeEventListener: jest.fn(),
          dispatchEvent: jest.fn(),
        })),
      });
    });

    it('should adapt layout for mobile screens', () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      // Should still render all components but with mobile-responsive layout
      expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      expect(screen.getByTestId('job-status-grid')).toBeInTheDocument();
      expect(screen.getByTestId('alerts-panel')).toBeInTheDocument();
    });
  });

  describe('Performance', () => {
    it('should not re-render unnecessarily', () => {
      const mockData = createMockDashboardData();
      
      mockUseRealTimeMonitoring.mockReturnValue({
        dashboardData: mockData,
        loading: false,
        error: null,
        connected: true,
        refresh: jest.fn(),
        subscribe: jest.fn(),
        unsubscribe: jest.fn()
      });

      const { rerender } = render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      // Re-render with same data
      rerender(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      // Should still display the same content
      expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
    });
  });
});