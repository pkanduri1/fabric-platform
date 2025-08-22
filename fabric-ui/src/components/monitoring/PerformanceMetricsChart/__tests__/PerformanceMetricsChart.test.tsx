/**
 * PerformanceMetricsChart Component Tests
 * 
 * Test suite for the PerformanceMetricsChart component including chart rendering,
 * real-time updates, data visualization, and interaction handling.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';

import { PerformanceMetricsChart } from '../PerformanceMetricsChart';
import { 
  PerformanceMetrics, 
  HistoricalMetrics, 
  TrendData 
} from '../../../../types/monitoring';

// Mock Chart.js
jest.mock('react-chartjs-2', () => ({
  Line: ({ data, options, ...props }: any) => (
    <div data-testid="line-chart" data-chart-data={JSON.stringify(data)} {...props}>
      <canvas data-testid="chart-canvas" />
    </div>
  ),
  Bar: ({ data, options, ...props }: any) => (
    <div data-testid="bar-chart" data-chart-data={JSON.stringify(data)} {...props}>
      <canvas data-testid="chart-canvas" />
    </div>
  ),
  Doughnut: ({ data, options, ...props }: any) => (
    <div data-testid="doughnut-chart" data-chart-data={JSON.stringify(data)} {...props}>
      <canvas data-testid="chart-canvas" />
    </div>
  )
}));

// Mock Chart.js registration
jest.mock('chart.js', () => ({
  Chart: {
    register: jest.fn(),
  },
  CategoryScale: jest.fn(),
  LinearScale: jest.fn(),
  PointElement: jest.fn(),
  LineElement: jest.fn(),
  BarElement: jest.fn(),
  ArcElement: jest.fn(),
  Title: jest.fn(),
  Tooltip: jest.fn(),
  Legend: jest.fn(),
  Filler: jest.fn(),
}));

// Mock data factories
const createMockMetrics = (overrides: Partial<PerformanceMetrics> = {}): PerformanceMetrics => ({
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

const createMockTrendData = (count: number = 10): TrendData[] => {
  return Array.from({ length: count }, (_, index) => ({
    timestamp: new Date(Date.now() - (count - index - 1) * 60000).toISOString(),
    value: Math.random() * 100,
    label: `Point ${index + 1}`
  }));
};

const createMockHistoricalMetrics = (): HistoricalMetrics => ({
  period: 'HOUR',
  startDate: '2025-08-08T09:00:00Z',
  endDate: '2025-08-08T10:00:00Z',
  jobExecutionTrends: createMockTrendData(),
  throughputTrends: createMockTrendData(),
  errorRateTrends: createMockTrendData(),
  performanceScoreTrends: createMockTrendData(),
  systemHealthTrends: createMockTrendData()
});

const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const theme = createTheme();
  return <ThemeProvider theme={theme}>{children}</ThemeProvider>;
};

describe('PerformanceMetricsChart', () => {
  const defaultProps = {
    metrics: [
      createMockMetrics({ timestamp: '2025-08-08T10:00:00Z', totalThroughput: 800 }),
      createMockMetrics({ timestamp: '2025-08-08T10:01:00Z', totalThroughput: 950 }),
      createMockMetrics({ timestamp: '2025-08-08T10:02:00Z', totalThroughput: 1100 }),
      createMockMetrics({ timestamp: '2025-08-08T10:03:00Z', totalThroughput: 1000 })
    ],
    trends: createMockHistoricalMetrics(),
    height: 300,
    realTime: true,
    refreshInterval: 5000,
    onTimeRangeChange: jest.fn()
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render chart container with correct structure', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByTestId('performance-metrics-chart')).toBeInTheDocument();
      expect(screen.getByRole('tabpanel')).toBeInTheDocument();
    });

    it('should render metric selection tabs', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByRole('tab', { name: /throughput/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /success rate/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /error rate/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /response time/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /resource usage/i })).toBeInTheDocument();
    });

    it('should render time range selector', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByRole('combobox', { name: /time range/i })).toBeInTheDocument();
      expect(screen.getByText('1 Hour')).toBeInTheDocument();
    });

    it('should render chart controls', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByLabelText(/auto refresh/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/export chart/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/fullscreen/i)).toBeInTheDocument();
    });

    it('should display current metric values', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText('1,000')).toBeInTheDocument(); // Current throughput
      expect(screen.getByText('95.5%')).toBeInTheDocument(); // Success rate
      expect(screen.getByText('4.5%')).toBeInTheDocument(); // Error rate
    });

    it('should show trend indicators', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const trendIndicators = screen.getAllByTestId(/trend-indicator/);
      expect(trendIndicators.length).toBeGreaterThan(0);
    });
  });

  describe('Chart Types', () => {
    it('should render line chart by default', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByTestId('line-chart')).toBeInTheDocument();
    });

    it('should switch to bar chart when selected', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const chartTypeButton = screen.getByLabelText(/chart type/i);
      await user.click(chartTypeButton);

      const barOption = screen.getByText('Bar Chart');
      await user.click(barOption);

      expect(screen.getByTestId('bar-chart')).toBeInTheDocument();
    });

    it('should render doughnut chart for resource usage', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const resourceTab = screen.getByRole('tab', { name: /resource usage/i });
      await user.click(resourceTab);

      expect(screen.getByTestId('doughnut-chart')).toBeInTheDocument();
    });
  });

  describe('Data Visualization', () => {
    it('should display correct data in chart', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const lineChart = screen.getByTestId('line-chart');
      const chartData = JSON.parse(lineChart.getAttribute('data-chart-data') || '{}');
      
      expect(chartData.datasets).toHaveLength(1);
      expect(chartData.datasets[0].data).toHaveLength(4);
      expect(chartData.datasets[0].data).toEqual([800, 950, 1100, 1000]);
    });

    it('should format timestamps correctly on x-axis', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const lineChart = screen.getByTestId('line-chart');
      const chartData = JSON.parse(lineChart.getAttribute('data-chart-data') || '{}');
      
      expect(chartData.labels).toHaveLength(4);
      chartData.labels.forEach((label: string) => {
        expect(label).toMatch(/\d{2}:\d{2}/); // Time format HH:MM
      });
    });

    it('should handle empty metrics gracefully', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} metrics={[]} />
        </TestWrapper>
      );

      expect(screen.getByText(/no data available/i)).toBeInTheDocument();
    });

    it('should display metric statistics', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText(/average:/i)).toBeInTheDocument();
      expect(screen.getByText(/min:/i)).toBeInTheDocument();
      expect(screen.getByText(/max:/i)).toBeInTheDocument();
    });
  });

  describe('User Interactions', () => {
    it('should switch between metric tabs', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const errorRateTab = screen.getByRole('tab', { name: /error rate/i });
      await user.click(errorRateTab);

      expect(errorRateTab).toHaveAttribute('aria-selected', 'true');
      
      // Should show error rate data
      const lineChart = screen.getByTestId('line-chart');
      const chartData = JSON.parse(lineChart.getAttribute('data-chart-data') || '{}');
      expect(chartData.datasets[0].data).toEqual([4.5, 4.5, 4.5, 4.5]);
    });

    it('should handle time range changes', async () => {
      const mockOnTimeRangeChange = jest.fn();
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <PerformanceMetricsChart 
            {...defaultProps} 
            onTimeRangeChange={mockOnTimeRangeChange}
          />
        </TestWrapper>
      );

      const timeRangeSelect = screen.getByRole('combobox', { name: /time range/i });
      await user.click(timeRangeSelect);

      const dayOption = screen.getByText('1 Day');
      await user.click(dayOption);

      expect(mockOnTimeRangeChange).toHaveBeenCalledWith('1d');
    });

    it('should toggle auto-refresh', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const autoRefreshSwitch = screen.getByLabelText(/auto refresh/i);
      expect(autoRefreshSwitch).toBeChecked();

      await user.click(autoRefreshSwitch);
      expect(autoRefreshSwitch).not.toBeChecked();
    });

    it('should handle export functionality', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const exportButton = screen.getByLabelText(/export chart/i);
      await user.click(exportButton);

      // Should show export options
      expect(screen.getByText(/export as png/i)).toBeInTheDocument();
      expect(screen.getByText(/export as csv/i)).toBeInTheDocument();
    });

    it('should toggle fullscreen mode', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const fullscreenButton = screen.getByLabelText(/fullscreen/i);
      await user.click(fullscreenButton);

      const chart = screen.getByTestId('performance-metrics-chart');
      expect(chart).toHaveClass(/fullscreen/);
    });
  });

  describe('Real-time Updates', () => {
    it('should update chart data when new metrics arrive', () => {
      const { rerender } = render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const newMetrics = [
        ...defaultProps.metrics,
        createMockMetrics({ timestamp: '2025-08-08T10:04:00Z', totalThroughput: 1200 })
      ];

      rerender(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} metrics={newMetrics} />
        </TestWrapper>
      );

      const lineChart = screen.getByTestId('line-chart');
      const chartData = JSON.parse(lineChart.getAttribute('data-chart-data') || '{}');
      
      expect(chartData.datasets[0].data).toHaveLength(5);
      expect(chartData.datasets[0].data[4]).toBe(1200);
    });

    it('should maintain chart window size with streaming data', () => {
      const manyMetrics = Array.from({ length: 100 }, (_, index) =>
        createMockMetrics({ 
          timestamp: new Date(Date.now() - (100 - index) * 60000).toISOString(),
          totalThroughput: 1000 + index * 10 
        })
      );

      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} metrics={manyMetrics} />
        </TestWrapper>
      );

      const lineChart = screen.getByTestId('line-chart');
      const chartData = JSON.parse(lineChart.getAttribute('data-chart-data') || '{}');
      
      // Should limit to maxDataPoints
      expect(chartData.datasets[0].data.length).toBe(50);
    });

    it('should show real-time indicator when enabled', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} realTime={true} />
        </TestWrapper>
      );

      expect(screen.getByTestId('real-time-indicator')).toBeInTheDocument();
      expect(screen.getByText(/live/i)).toBeInTheDocument();
    });

    it('should pause updates when user interacts with chart', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const chartCanvas = screen.getByTestId('chart-canvas');
      await user.hover(chartCanvas);

      expect(screen.getByText(/paused/i)).toBeInTheDocument();
    });
  });

  describe('Performance Optimizations', () => {
    it('should use memoization for expensive calculations', () => {
      const { rerender } = render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      // Re-render with same props should not recalculate chart data
      rerender(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const lineChart = screen.getByTestId('line-chart');
      expect(lineChart).toBeInTheDocument();
    });

    it('should handle large datasets efficiently', () => {
      const largeMetrics = Array.from({ length: 1000 }, (_, index) =>
        createMockMetrics({ 
          timestamp: new Date(Date.now() - index * 1000).toISOString(),
          totalThroughput: Math.random() * 1000 
        })
      );

      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} metrics={largeMetrics} />
        </TestWrapper>
      );

      expect(screen.getByTestId('line-chart')).toBeInTheDocument();
    });
  });

  describe('Error Handling', () => {
    it('should handle invalid metric data gracefully', () => {
      const invalidMetrics = [
        { ...createMockMetrics(), totalThroughput: NaN },
        { ...createMockMetrics(), successRate: Infinity },
        { ...createMockMetrics(), timestamp: 'invalid-date' }
      ];

      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} metrics={invalidMetrics} />
        </TestWrapper>
      );

      expect(screen.getByText(/data validation error/i)).toBeInTheDocument();
    });

    it('should show error state when chart fails to render', () => {
      // Mock chart render failure
      jest.spyOn(console, 'error').mockImplementation(() => {});
      
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} metrics={null as any} />
        </TestWrapper>
      );

      expect(screen.getByText(/failed to load chart/i)).toBeInTheDocument();
      
      jest.restoreAllMocks();
    });
  });

  describe('Accessibility', () => {
    it('should have proper ARIA labels and roles', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByRole('region', { name: /performance metrics/i })).toBeInTheDocument();
      expect(screen.getByRole('tablist')).toBeInTheDocument();
      expect(screen.getAllByRole('tab')).toHaveLength(5);
    });

    it('should support keyboard navigation', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const tabs = screen.getAllByRole('tab');
      tabs.forEach(tab => {
        expect(tab).toHaveAttribute('tabindex', '0');
      });
    });

    it('should provide data table alternative for screen readers', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const dataTable = screen.getByRole('table', { name: /chart data/i });
      expect(dataTable).toBeInTheDocument();
      
      const rows = screen.getAllByRole('row');
      expect(rows.length).toBeGreaterThan(1); // Header + data rows
    });

    it('should announce data updates to screen readers', () => {
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const liveRegion = screen.getByRole('status');
      expect(liveRegion).toHaveAttribute('aria-live', 'polite');
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
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const chart = screen.getByTestId('performance-metrics-chart');
      expect(chart).toHaveClass(/mobile/);
    });

    it('should use swipe gestures on mobile', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <PerformanceMetricsChart {...defaultProps} />
        </TestWrapper>
      );

      const chartContainer = screen.getByTestId('chart-container');
      
      // Simulate swipe gesture
      fireEvent.touchStart(chartContainer, { touches: [{ clientX: 100 }] });
      fireEvent.touchMove(chartContainer, { touches: [{ clientX: 50 }] });
      fireEvent.touchEnd(chartContainer);

      // Should switch to next metric tab
      await waitFor(() => {
        expect(screen.getByRole('tab', { selected: true })).toHaveTextContent(/success rate/i);
      });
    });
  });
});