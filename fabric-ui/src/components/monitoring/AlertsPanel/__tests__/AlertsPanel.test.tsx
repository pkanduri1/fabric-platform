/**
 * AlertsPanel Component Tests
 * 
 * Test suite for the AlertsPanel component including alert rendering,
 * acknowledgment, filtering, real-time updates, and sound notifications.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';

import { AlertsPanel } from '../AlertsPanel';
import { 
  Alert, 
  AlertSeverity, 
  AlertType, 
  AlertFilters 
} from '../../../../types/monitoring';

// Mock audio API
global.Audio = jest.fn().mockImplementation(() => ({
  play: jest.fn().mockResolvedValue(undefined),
  pause: jest.fn(),
  volume: 0.5,
  currentTime: 0
}));

// Mock data factory
const createMockAlert = (overrides: Partial<Alert> = {}): Alert => ({
  alertId: 'alert-123',
  type: AlertType.ERROR_RATE,
  severity: AlertSeverity.WARNING,
  title: 'High Error Rate Detected',
  description: 'Error rate has exceeded the threshold of 5%',
  jobExecutionId: 'job-123',
  sourceSystem: 'TestSystem',
  threshold: 5.0,
  currentValue: 7.2,
  timestamp: '2025-08-08T10:00:00Z',
  acknowledged: false,
  resolved: false,
  escalated: false,
  correlationId: 'corr-alert-123',
  affectedResources: ['job-123', 'worker-node-1'],
  ...overrides
});

const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const theme = createTheme();
  return <ThemeProvider theme={theme}>{children}</ThemeProvider>;
};

describe('AlertsPanel', () => {
  const defaultProps = {
    alerts: [
      createMockAlert({
        alertId: 'alert-1',
        severity: AlertSeverity.CRITICAL,
        title: 'System Down',
        acknowledged: false
      }),
      createMockAlert({
        alertId: 'alert-2',
        severity: AlertSeverity.WARNING,
        title: 'High CPU Usage',
        acknowledged: true,
        acknowledgedBy: 'admin',
        acknowledgedAt: '2025-08-08T10:01:00Z'
      }),
      createMockAlert({
        alertId: 'alert-3',
        severity: AlertSeverity.INFO,
        title: 'Job Completed',
        acknowledged: false,
        resolved: true,
        resolvedAt: '2025-08-08T10:02:00Z'
      }),
      createMockAlert({
        alertId: 'alert-4',
        severity: AlertSeverity.ERROR,
        title: 'Database Connection Failed',
        acknowledged: false,
        escalated: true,
        escalationLevel: 2
      })
    ],
    onAcknowledge: jest.fn(),
    onResolve: jest.fn(),
    onConfigure: jest.fn(),
    filters: {},
    soundEnabled: true
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render alerts list correctly', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText('System Down')).toBeInTheDocument();
      expect(screen.getByText('High CPU Usage')).toBeInTheDocument();
      expect(screen.getByText('Job Completed')).toBeInTheDocument();
      expect(screen.getByText('Database Connection Failed')).toBeInTheDocument();
    });

    it('should display alert severity badges correctly', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText('CRITICAL')).toBeInTheDocument();
      expect(screen.getByText('WARNING')).toBeInTheDocument();
      expect(screen.getByText('INFO')).toBeInTheDocument();
      expect(screen.getByText('ERROR')).toBeInTheDocument();
    });

    it('should show acknowledged status', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText(/acknowledged by admin/i)).toBeInTheDocument();
    });

    it('should show resolved status', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText(/resolved/i)).toBeInTheDocument();
    });

    it('should show escalation indicators', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText(/escalated/i)).toBeInTheDocument();
      expect(screen.getByText(/level 2/i)).toBeInTheDocument();
    });

    it('should display alert timestamps', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const timestamps = screen.getAllByText(/10:0[0-2]:00/);
      expect(timestamps.length).toBeGreaterThan(0);
    });

    it('should show affected resources', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText(/job-123/)).toBeInTheDocument();
      expect(screen.getByText(/worker-node-1/)).toBeInTheDocument();
    });
  });

  describe('Empty and Loading States', () => {
    it('should show empty state when no alerts', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} alerts={[]} />
        </TestWrapper>
      );

      expect(screen.getByText(/no alerts/i)).toBeInTheDocument();
      expect(screen.getByText(/all systems are running normally/i)).toBeInTheDocument();
    });

    it('should show loading skeleton when loading', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} loading={true} />
        </TestWrapper>
      );

      const skeletons = screen.getAllByTestId(/skeleton/i);
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it('should show filtered empty state', () => {
      const filters: AlertFilters = {
        severity: [AlertSeverity.CRITICAL],
        acknowledged: true
      };

      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} filters={filters} />
        </TestWrapper>
      );

      expect(screen.getByText(/no alerts match your filters/i)).toBeInTheDocument();
    });
  });

  describe('User Interactions', () => {
    it('should handle alert acknowledgment', async () => {
      const mockOnAcknowledge = jest.fn();
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} onAcknowledge={mockOnAcknowledge} />
        </TestWrapper>
      );

      const acknowledgeButton = screen.getAllByText(/acknowledge/i)[0];
      await user.click(acknowledgeButton);

      expect(mockOnAcknowledge).toHaveBeenCalledWith('alert-1');
    });

    it('should handle alert resolution', async () => {
      const mockOnResolve = jest.fn();
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} onResolve={mockOnResolve} />
        </TestWrapper>
      );

      const moreActionsButton = screen.getAllByLabelText(/more actions/i)[0];
      await user.click(moreActionsButton);

      const resolveButton = screen.getByText(/resolve/i);
      await user.click(resolveButton);

      expect(mockOnResolve).toHaveBeenCalledWith('alert-1');
    });

    it('should handle bulk acknowledgment', async () => {
      const mockOnAcknowledge = jest.fn();
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} onAcknowledge={mockOnAcknowledge} />
        </TestWrapper>
      );

      // Select multiple alerts
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[0]); // Select first unacknowledged alert
      await user.click(checkboxes[2]); // Select third unacknowledged alert

      const bulkAckButton = screen.getByText(/acknowledge selected/i);
      await user.click(bulkAckButton);

      expect(mockOnAcknowledge).toHaveBeenCalledTimes(2);
    });

    it('should expand/collapse alert details', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const expandButton = screen.getAllByLabelText(/expand details/i)[0];
      await user.click(expandButton);

      expect(screen.getByText(/error rate has exceeded/i)).toBeInTheDocument();
      expect(screen.getByText(/threshold: 5.0/i)).toBeInTheDocument();
      expect(screen.getByText(/current: 7.2/i)).toBeInTheDocument();
    });

    it('should handle alert filtering', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const severityFilter = screen.getByRole('combobox', { name: /severity/i });
      await user.click(severityFilter);

      const criticalOption = screen.getByText('Critical Only');
      await user.click(criticalOption);

      expect(screen.getByText('System Down')).toBeInTheDocument();
      expect(screen.queryByText('High CPU Usage')).not.toBeInTheDocument();
    });
  });

  describe('Alert Filtering', () => {
    it('should filter by severity', () => {
      const filters: AlertFilters = {
        severity: [AlertSeverity.CRITICAL, AlertSeverity.ERROR]
      };

      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} filters={filters} />
        </TestWrapper>
      );

      expect(screen.getByText('System Down')).toBeInTheDocument();
      expect(screen.getByText('Database Connection Failed')).toBeInTheDocument();
      expect(screen.queryByText('High CPU Usage')).not.toBeInTheDocument();
      expect(screen.queryByText('Job Completed')).not.toBeInTheDocument();
    });

    it('should filter by acknowledgment status', () => {
      const filters: AlertFilters = {
        acknowledged: false
      };

      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} filters={filters} />
        </TestWrapper>
      );

      expect(screen.getByText('System Down')).toBeInTheDocument();
      expect(screen.getByText('Job Completed')).toBeInTheDocument();
      expect(screen.getByText('Database Connection Failed')).toBeInTheDocument();
      expect(screen.queryByText('High CPU Usage')).not.toBeInTheDocument();
    });

    it('should filter by alert type', () => {
      const filters: AlertFilters = {
        type: [AlertType.ERROR_RATE, AlertType.SYSTEM_HEALTH]
      };

      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} filters={filters} />
        </TestWrapper>
      );

      // Should only show alerts of specified types
      const visibleAlerts = screen.getAllByTestId(/alert-item/);
      expect(visibleAlerts.length).toBeLessThan(4);
    });

    it('should filter by date range', () => {
      const filters: AlertFilters = {
        dateRange: {
          start: '2025-08-08T09:00:00Z',
          end: '2025-08-08T10:01:00Z'
        }
      };

      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} filters={filters} />
        </TestWrapper>
      );

      // Should only show alerts within date range
      const visibleAlerts = screen.getAllByTestId(/alert-item/);
      expect(visibleAlerts.length).toBeGreaterThan(0);
    });

    it('should combine multiple filters', () => {
      const filters: AlertFilters = {
        severity: [AlertSeverity.CRITICAL],
        acknowledged: false,
        resolved: false
      };

      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} filters={filters} />
        </TestWrapper>
      );

      expect(screen.getByText('System Down')).toBeInTheDocument();
      expect(screen.queryByText('High CPU Usage')).not.toBeInTheDocument();
      expect(screen.queryByText('Job Completed')).not.toBeInTheDocument();
    });
  });

  describe('Sound Notifications', () => {
    it('should play sound for critical alerts when enabled', () => {
      const mockAudio = jest.mocked(global.Audio);
      const mockPlay = jest.fn();
      mockAudio.mockImplementation(() => ({
        play: mockPlay,
        pause: jest.fn(),
        volume: 0.5,
        currentTime: 0
      } as any));

      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} soundEnabled={true} />
        </TestWrapper>
      );

      expect(mockAudio).toHaveBeenCalledWith('/sounds/alert-critical.mp3');
      expect(mockPlay).toHaveBeenCalled();
    });

    it('should not play sound when disabled', () => {
      const mockAudio = jest.mocked(global.Audio);
      
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} soundEnabled={false} />
        </TestWrapper>
      );

      expect(mockAudio).not.toHaveBeenCalled();
    });

    it('should use different sounds for different severity levels', () => {
      const mockAudio = jest.mocked(global.Audio);
      
      const alertsWithDifferentSeverities = [
        createMockAlert({ severity: AlertSeverity.CRITICAL }),
        createMockAlert({ severity: AlertSeverity.ERROR }),
        createMockAlert({ severity: AlertSeverity.WARNING })
      ];

      render(
        <TestWrapper>
          <AlertsPanel 
            {...defaultProps} 
            alerts={alertsWithDifferentSeverities}
            soundEnabled={true} 
          />
        </TestWrapper>
      );

      expect(mockAudio).toHaveBeenCalledWith('/sounds/alert-critical.mp3');
      expect(mockAudio).toHaveBeenCalledWith('/sounds/alert-error.mp3');
      expect(mockAudio).toHaveBeenCalledWith('/sounds/alert-warning.mp3');
    });

    it('should handle audio play failures gracefully', () => {
      const mockAudio = jest.mocked(global.Audio);
      const mockPlay = jest.fn().mockRejectedValue(new Error('Audio play failed'));
      mockAudio.mockImplementation(() => ({
        play: mockPlay,
        pause: jest.fn(),
        volume: 0.5,
        currentTime: 0
      } as any));

      const consoleSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} soundEnabled={true} />
        </TestWrapper>
      );

      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('Failed to play alert sound')
      );

      consoleSpy.mockRestore();
    });
  });

  describe('Real-time Updates', () => {
    it('should update alert list when new alerts arrive', () => {
      const { rerender } = render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const newAlert = createMockAlert({
        alertId: 'alert-5',
        title: 'New Critical Issue',
        severity: AlertSeverity.CRITICAL
      });

      rerender(
        <TestWrapper>
          <AlertsPanel {...defaultProps} alerts={[...defaultProps.alerts, newAlert]} />
        </TestWrapper>
      );

      expect(screen.getByText('New Critical Issue')).toBeInTheDocument();
    });

    it('should update acknowledgment status in real-time', () => {
      const { rerender } = render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const updatedAlerts = defaultProps.alerts.map(alert =>
        alert.alertId === 'alert-1'
          ? { ...alert, acknowledged: true, acknowledgedBy: 'operator' }
          : alert
      );

      rerender(
        <TestWrapper>
          <AlertsPanel {...defaultProps} alerts={updatedAlerts} />
        </TestWrapper>
      );

      expect(screen.getByText(/acknowledged by operator/i)).toBeInTheDocument();
    });

    it('should show new alert animations', () => {
      const { rerender } = render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const newAlert = createMockAlert({
        alertId: 'alert-new',
        title: 'New Alert',
        timestamp: new Date().toISOString()
      });

      rerender(
        <TestWrapper>
          <AlertsPanel {...defaultProps} alerts={[newAlert, ...defaultProps.alerts]} />
        </TestWrapper>
      );

      const newAlertElement = screen.getByText('New Alert').closest('[data-testid="alert-item"]');
      expect(newAlertElement).toHaveClass(/new-alert/);
    });
  });

  describe('Alert Actions', () => {
    it('should show acknowledge button only for unacknowledged alerts', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const acknowledgeButtons = screen.getAllByText(/acknowledge/i);
      // Should only show for unacknowledged alerts (3 out of 4)
      expect(acknowledgeButtons).toHaveLength(3);
    });

    it('should hide action buttons when user lacks permissions', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} onAcknowledge={undefined} />
        </TestWrapper>
      );

      expect(screen.queryByText(/acknowledge/i)).not.toBeInTheDocument();
    });

    it('should handle alert snoozing', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const moreActionsButton = screen.getAllByLabelText(/more actions/i)[0];
      await user.click(moreActionsButton);

      const snoozeButton = screen.getByText(/snooze/i);
      await user.click(snoozeButton);

      const snoozeDialog = screen.getByRole('dialog', { name: /snooze alert/i });
      expect(snoozeDialog).toBeInTheDocument();
    });

    it('should handle alert escalation', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const moreActionsButton = screen.getAllByLabelText(/more actions/i)[0];
      await user.click(moreActionsButton);

      const escalateButton = screen.getByText(/escalate/i);
      await user.click(escalateButton);

      expect(screen.getByText(/escalation confirmation/i)).toBeInTheDocument();
    });
  });

  describe('Alert Grouping', () => {
    it('should group alerts by source system', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const groupByButton = screen.getByLabelText(/group by/i);
      await user.click(groupByButton);

      const sourceSystemOption = screen.getByText(/source system/i);
      await user.click(sourceSystemOption);

      expect(screen.getByText(/testsystem/i)).toBeInTheDocument();
    });

    it('should group alerts by severity', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const groupByButton = screen.getByLabelText(/group by/i);
      await user.click(groupByButton);

      const severityOption = screen.getByText(/severity/i);
      await user.click(severityOption);

      expect(screen.getByText(/critical \(1\)/i)).toBeInTheDocument();
      expect(screen.getByText(/warning \(1\)/i)).toBeInTheDocument();
      expect(screen.getByText(/error \(1\)/i)).toBeInTheDocument();
      expect(screen.getByText(/info \(1\)/i)).toBeInTheDocument();
    });
  });

  describe('Accessibility', () => {
    it('should have proper ARIA labels and roles', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByRole('region', { name: /alerts panel/i })).toBeInTheDocument();
      expect(screen.getByRole('list', { name: /alerts list/i })).toBeInTheDocument();
    });

    it('should support keyboard navigation', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const acknowledgeButtons = screen.getAllByText(/acknowledge/i);
      acknowledgeButtons.forEach(button => {
        expect(button).toHaveAttribute('tabindex', '0');
      });
    });

    it('should announce alert updates to screen readers', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const liveRegion = screen.getByRole('status');
      expect(liveRegion).toHaveAttribute('aria-live', 'polite');
    });

    it('should provide alert summary for screen readers', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText(/4 alerts total/i)).toBeInTheDocument();
      expect(screen.getByText(/3 unacknowledged/i)).toBeInTheDocument();
    });
  });

  describe('Mobile Responsiveness', () => {
    beforeEach(() => {
      Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation(query => ({
          matches: query.includes('(max-width: 768px)'),
          media: query,
          onchange: null,
          addListener: jest.fn(),
          removeListener: jest.fn(),
        })),
      });
    });

    it('should adapt layout for mobile screens', () => {
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const alertsPanel = screen.getByTestId('alerts-panel');
      expect(alertsPanel).toHaveClass(/mobile/);
    });

    it('should use swipe gestures for alert actions on mobile', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <AlertsPanel {...defaultProps} />
        </TestWrapper>
      );

      const alertItem = screen.getAllByTestId(/alert-item/)[0];
      
      // Simulate swipe gesture
      fireEvent.touchStart(alertItem, { touches: [{ clientX: 0 }] });
      fireEvent.touchMove(alertItem, { touches: [{ clientX: -100 }] });
      fireEvent.touchEnd(alertItem);

      await waitFor(() => {
        expect(screen.getByText(/acknowledge/i)).toBeInTheDocument();
      });
    });
  });
});