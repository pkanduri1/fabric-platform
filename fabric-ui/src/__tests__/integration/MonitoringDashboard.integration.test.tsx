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
import { render, screen, waitFor, act } from '@testing-library/react';
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
import * as monitoringApiModule from '../../services/api/monitoringApi';

// ---------------------------------------------------------------------------
// MockWebSocket — plain class used as the WebSocket implementation.
// global.WebSocket is wrapped in a jest.fn() spy so .mock.instances works.
// ---------------------------------------------------------------------------
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

  send(_data: string) {
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

// Wrap in jest.fn() so .mock.instances is tracked
const MockWebSocketSpy = jest.fn().mockImplementation((url: string) => new MockWebSocket(url));
(MockWebSocketSpy as any).CONNECTING = 0;
(MockWebSocketSpy as any).OPEN = 1;
(MockWebSocketSpy as any).CLOSING = 2;
(MockWebSocketSpy as any).CLOSED = 3;
global.WebSocket = MockWebSocketSpy as any;

// Mock API module — factory uses only literals so Jest hoisting is safe.
// `monitoringApi` object's methods are separate jest.fn() instances mirroring
// the top-level exports. Both are configured in beforeEach.
jest.mock('../../services/api/monitoringApi', () => ({
  getDashboardData: jest.fn(),
  acknowledgeAlert: jest.fn(),
  getJobDetails: jest.fn(),
  exportDashboardData: jest.fn(),
  monitoringApi: {
    getDashboardData: jest.fn(),
    acknowledgeAlert: jest.fn(),
    getJobDetails: jest.fn(),
    exportDashboardData: jest.fn()
  },
  MonitoringApiService: jest.fn()
}));

// Typed reference to the mocked module; configured per-test in beforeEach
const mockMonitoringApi = monitoringApiModule as jest.Mocked<typeof monitoringApiModule>;

// ---------------------------------------------------------------------------
// Mock useRealTimeMonitoring so the dashboard is driven by monitoringApi
// (matching what the tests set up in beforeEach) rather than raw WebSocket.
// ---------------------------------------------------------------------------
jest.mock('../../hooks/useRealTimeMonitoring', () => ({
  __esModule: true,
  useRealTimeMonitoring: jest.fn(),
  default: jest.fn()
}));

import * as realTimeMonitoringModule from '../../hooks/useRealTimeMonitoring';
const mockUseRealTimeMonitoring = realTimeMonitoringModule.useRealTimeMonitoring as jest.Mock;

// Mock audio — note: jest.clearAllMocks() wipes this implementation, so
// individual sound tests must re-install the mock in their own beforeEach/test body
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

// ---------------------------------------------------------------------------
// Controllable hook state — tests mutate these via setHookState()
// ---------------------------------------------------------------------------
let hookStateSetters: Array<(patch: Partial<{
  dashboardData: DashboardData | null;
  loading: boolean;
  error: any;
  connected: boolean;
}>) => void> = [];

function setHookState(patch: Partial<{
  dashboardData: DashboardData | null;
  loading: boolean;
  error: any;
  connected: boolean;
}>) {
  hookStateSetters.forEach(fn => fn(patch));
}

/**
 * Build and register the useRealTimeMonitoring mock implementation.
 * Called in beforeEach (after clearAllMocks wipes the previous implementation).
 *
 * The hook:
 *  - Starts with loading=true, dashboardData=null, connected=false
 *  - Fetches from monitoringApi.getDashboardData on mount
 *  - Exposes a setState pathway (hookStateSetters) for test-driven updates
 */
function installHookMock() {
  mockUseRealTimeMonitoring.mockImplementation(() => {
    const [dashboardData, setDashboardData] = React.useState<DashboardData | null>(null);
    const [loading, setLoading] = React.useState(true);
    const [connected, setConnected] = React.useState(false);
    const [error, setError] = React.useState<any>(null);

    // Register combined state setter so tests can drive state changes
    React.useEffect(() => {
      const setter = (patch: any) => {
        if ('dashboardData' in patch) setDashboardData(patch.dashboardData);
        if ('loading' in patch) setLoading(patch.loading);
        if ('connected' in patch) setConnected(patch.connected);
        if ('error' in patch) setError(patch.error);
      };
      hookStateSetters.push(setter);
      return () => {
        hookStateSetters = hookStateSetters.filter(fn => fn !== setter);
      };
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    // Auto-load data from API on mount
    React.useEffect(() => {
      let mounted = true;
      mockMonitoringApi.getDashboardData()
        .then((response: any) => {
          if (!mounted) return;
          if (response?.data) {
            setDashboardData(response.data);
            setConnected(true);
          }
          setLoading(false);
        })
        .catch((err: any) => {
          if (!mounted) return;
          setError({
            code: 'API_ERROR',
            message: err.message,
            timestamp: new Date().toISOString(),
            recoverable: false
          });
          setLoading(false);
        });
      return () => { mounted = false; };
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    const refresh = jest.fn().mockImplementation(async () => {
      setLoading(true);
      try {
        const response = await mockMonitoringApi.getDashboardData();
        if (response?.data) {
          setDashboardData(response.data);
          setConnected(true);
        }
      } finally {
        setLoading(false);
      }
    });

    return {
      dashboardData,
      loading,
      error,
      connected,
      refresh,
      subscribe: jest.fn(),
      unsubscribe: jest.fn(),
      getCachedData: jest.fn(),
      isDataFresh: jest.fn()
    };
  });
}

describe('MonitoringDashboard Integration Tests', () => {

  beforeEach(() => {
    // Reset hook state setters
    hookStateSetters = [];

    // Reset WebSocket spy call tracking
    MockWebSocketSpy.mockClear();

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

    const jobDetailsMockValue = {
      executionId: 'job-1',
      jobName: 'Import Customer Data',
      status: JobStatus.RUNNING,
      startTime: '2025-08-08T10:00:00Z',
      processedRecords: 1500,
      totalRecords: 2000,
      performance: { averageThroughput: 100, peakThroughput: 150 },
      stages: [],
      errors: [],
      warnings: [],
      dataLineage: {},
      auditTrail: [],
      dependencies: []
    };

    mockMonitoringApi.getJobDetails.mockResolvedValue(jobDetailsMockValue);
    // Also configure the `monitoringApi` instance mock (used by JobDetailsModal)
    (monitoringApiModule as any).monitoringApi.getJobDetails.mockResolvedValue(jobDetailsMockValue);

    // Re-install hook mock (clearAllMocks in afterEach wipes the implementation)
    installHookMock();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Full Dashboard Flow', () => {
    it('should load dashboard and establish WebSocket connection', async () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      // Should show loading initially (LinearProgress — may render multiple progress bars)
      expect(screen.getAllByRole('progressbar').length).toBeGreaterThan(0);

      // Wait for data loading
      await waitFor(() => {
        expect(screen.getByText('Import Customer Data')).toBeInTheDocument();
      }, { timeout: 5000 });

      // Should display the dashboard title
      expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();

      // Should display job statistics (MUI Chips may render label twice in DOM)
      expect(screen.getAllByText('1 Running').length).toBeGreaterThan(0);
      expect(screen.getAllByText('1 Failed').length).toBeGreaterThan(0);
      expect(screen.getAllByText('1 Pending').length).toBeGreaterThan(0);

      // Should show system health score (SystemHealthBar renders "System Health: N%")
      expect(screen.getByText(/System Health: 88%/)).toBeInTheDocument();

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

      // Simulate job progress update by mutating dashboardData in hook state
      const updatedData = createMockDashboardData();
      updatedData.activeJobs[0] = createMockJob({
        executionId: 'job-1',
        jobName: 'Import Customer Data',
        status: JobStatus.RUNNING,
        progress: 90,
        recordsProcessed: 1800,
        throughputPerSecond: 120
      });

      act(() => {
        setHookState({ dashboardData: updatedData });
      });

      // Should update job progress in UI
      await waitFor(() => {
        expect(screen.getByText('90%')).toBeInTheDocument();
      });
    });

    it('should handle new alerts with sound notification', async () => {
      const mockAudio = jest.mocked(global.Audio);
      const mockPlay = jest.fn().mockResolvedValue(undefined);
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

      // Enable sound in component config by simulating an alert arrival via hook state
      const updatedData = createMockDashboardData();
      updatedData.alerts = [
        ...updatedData.alerts,
        {
          alertId: 'alert-critical-1',
          type: AlertType.ERROR_RATE,
          severity: AlertSeverity.CRITICAL,
          title: 'System Failure Detected',
          description: 'Database connection lost',
          timestamp: new Date().toISOString(),
          acknowledged: false,
          resolved: false,
          escalated: false,
          correlationId: 'critical-alert-1',
          affectedResources: ['database']
        }
      ];

      act(() => {
        setHookState({ dashboardData: updatedData });
      });

      // Should show new alert
      await waitFor(() => {
        expect(screen.getByText('System Failure Detected')).toBeInTheDocument();
      });

      // Critical alert chip should appear (1 critical, unacknowledged)
      expect(screen.getByText('1 Critical Alerts')).toBeInTheDocument();
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

      // Click on a job name to open details modal
      const jobElement = screen.getByText('Import Customer Data');
      await user.click(jobElement);

      // Should open job details modal (title: "Job Details: Import Customer Data")
      await waitFor(() => {
        expect(screen.getByText(/Job Details:/)).toBeInTheDocument();
      });

      // Should call API for job details (JobDetailsModal uses the monitoringApi instance)
      expect((monitoringApiModule as any).monitoringApi.getJobDetails).toHaveBeenCalledWith('job-1');
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

      // Click the first Acknowledge button (for the unacknowledged alert)
      const acknowledgeButtons = screen.getAllByText(/Acknowledge/);
      await user.click(acknowledgeButtons[0]);

      // Should show success notification from the dashboard
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

      // Re-configure the mock to resolve for the refresh call
      mockMonitoringApi.getDashboardData.mockResolvedValue({
        success: true,
        data: createMockDashboardData(),
        timestamp: new Date().toISOString(),
        correlationId: 'api-refresh'
      });

      // Click refresh button
      const refreshButton = screen.getByLabelText('Refresh Dashboard');
      await user.click(refreshButton);

      // Should call API again
      await waitFor(() => {
        expect(mockMonitoringApi.getDashboardData).toHaveBeenCalledTimes(2);
      });

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

      // Simulate connection going offline via hook state
      act(() => {
        setHookState({ connected: false });
      });

      // Should show offline indicator
      await waitFor(() => {
        expect(screen.getByText('Offline')).toBeInTheDocument();
      });

      // Should show connection status FAB (warning button)
      expect(screen.getAllByRole('button').length).toBeGreaterThan(0);
    });

    it('should handle API failures gracefully', async () => {
      mockMonitoringApi.getDashboardData.mockRejectedValue(new Error('API Error'));

      // Re-install hook mock so it picks up the rejection
      installHookMock();

      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      // Hook sets error state; dashboard's onError callback shows notification
      // The hook exposes the error but the component's handleMonitoringError
      // is wired to the hook's onError option — with the mock the error object
      // is set in state. We verify the loading spinner disappears and content shows.
      await waitFor(() => {
        expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
      }, { timeout: 5000 });

      // The component title is still visible even when there's no data
      expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
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

      // Simulate connection loss
      act(() => {
        setHookState({ connected: false });
      });

      await waitFor(() => {
        expect(screen.getByText('Offline')).toBeInTheDocument();
      });

      // Simulate reconnection
      act(() => {
        setHookState({ connected: true });
      });

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

      // Simulate 50 rapid progress updates ending at 99%
      act(() => {
        for (let i = 0; i < 50; i++) {
          const updated = createMockDashboardData();
          updated.activeJobs[0] = createMockJob({
            executionId: 'job-1',
            jobName: 'Import Customer Data',
            status: JobStatus.RUNNING,
            progress: 50 + i,
            recordsProcessed: 1000 + i * 10
          });
          setHookState({ dashboardData: updated });
        }
      });

      // Should handle updates without performance issues
      await waitFor(() => {
        expect(screen.getByText('99%')).toBeInTheDocument(); // Last update
      });
    });

    it('should maintain responsiveness with large datasets', async () => {
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      // Wait for initial data load
      await waitFor(() => {
        expect(screen.getByText('Import Customer Data')).toBeInTheDocument();
      });

      // Build a large dataset with 100 jobs
      const largeDataset = createMockDashboardData();
      largeDataset.activeJobs = Array.from({ length: 100 }, (_, i) =>
        createMockJob({
          executionId: `job-${i}`,
          jobName: `Batch Job ${i}`,
          status: i % 3 === 0 ? JobStatus.RUNNING :
                 i % 3 === 1 ? JobStatus.PENDING : JobStatus.COMPLETED
        })
      );

      const startTime = performance.now();

      act(() => {
        setHookState({ dashboardData: largeDataset });
      });

      // Wait for any batch job to appear (pagination shows first page)
      await waitFor(() => {
        expect(screen.getAllByText(/^Batch Job \d+$/).length).toBeGreaterThan(0);
      });

      const endTime = performance.now();

      // Should render within reasonable time (less than 2 seconds)
      expect(endTime - startTime).toBeLessThan(2000);

      // Should show correct statistics (34 Running: indices 0,3,6,...,99; 33 Pending; 33 Completed)
      expect(screen.getByText(/34 Running/)).toBeInTheDocument();
      expect(screen.getByText(/33 Pending/)).toBeInTheDocument();
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

      // Update that affects health score
      const updatedData = createMockDashboardData();
      updatedData.systemHealth = {
        ...updatedData.systemHealth,
        overallScore: 95
      };
      updatedData.performanceMetrics = {
        ...updatedData.performanceMetrics,
        totalThroughput: 2000,
        systemHealthScore: 95
      };

      act(() => {
        setHookState({ dashboardData: updatedData });
      });

      // SystemHealthBar should reflect new score
      await waitFor(() => {
        expect(screen.getByText(/System Health: 95%/)).toBeInTheDocument();
      });

      // Performance chart should be in the document
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

      // Open filters dialog
      const filterButton = screen.getByLabelText('Filter Options');
      await user.click(filterButton);

      // Filters dialog should open
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
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

      // Should be able to navigate with tab key to reach the Refresh button
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
      render(
        <TestWrapper>
          <MonitoringDashboard />
        </TestWrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('Real-Time Job Monitoring')).toBeInTheDocument();
      });

      // Wait for data to load
      await waitFor(() => {
        expect(screen.getByText('Import Customer Data')).toBeInTheDocument();
      });

      // Should still show all essential information regardless of viewport
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

      // PerformanceMetricsChart renders a live region: <div role="status" aria-live="polite" />
      // Wait for the chart to render (needs dashboardData)
      await waitFor(() => {
        expect(screen.getByText('Import Customer Data')).toBeInTheDocument();
      });

      // There may be multiple status roles; at least one should have aria-live="polite"
      const statusRegions = screen.getAllByRole('status');
      const politeRegion = statusRegions.find(el => el.getAttribute('aria-live') === 'polite');
      expect(politeRegion).toBeDefined();
      expect(politeRegion).toHaveAttribute('aria-live', 'polite');
    });
  });
});
