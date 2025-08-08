/**
 * JobStatusGrid Component Tests
 * 
 * Test suite for the JobStatusGrid component including job rendering,
 * filtering, sorting, selection, and real-time updates.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

import React from 'react';
import { render, screen, fireEvent, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';

import { JobStatusGrid } from '../JobStatusGrid';
import { 
  ActiveJob, 
  JobStatus, 
  JobPriority, 
  TrendIndicator 
} from '../../../../types/monitoring';

// Mock data factory
const createMockJob = (overrides: Partial<ActiveJob> = {}): ActiveJob => ({
  executionId: 'job-123',
  jobName: 'Test Job',
  sourceSystem: 'TestSystem',
  status: JobStatus.RUNNING,
  priority: JobPriority.NORMAL,
  startTime: '2025-08-08T10:00:00Z',
  estimatedEndTime: '2025-08-08T11:00:00Z',
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
  assignedNode: 'node-1',
  correlationId: 'corr-123',
  ...overrides
});

const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const theme = createTheme();
  return <ThemeProvider theme={theme}>{children}</ThemeProvider>;
};

describe('JobStatusGrid', () => {
  const defaultProps = {
    jobs: [
      createMockJob({ 
        executionId: 'job-1', 
        jobName: 'Data Import Job', 
        status: JobStatus.RUNNING,
        priority: JobPriority.HIGH,
        progress: 75
      }),
      createMockJob({ 
        executionId: 'job-2', 
        jobName: 'Validation Job', 
        status: JobStatus.PENDING,
        priority: JobPriority.NORMAL,
        progress: 0
      }),
      createMockJob({ 
        executionId: 'job-3', 
        jobName: 'Transform Job', 
        status: JobStatus.FAILED,
        priority: JobPriority.CRITICAL,
        progress: 30,
        errorCount: 5
      }),
      createMockJob({ 
        executionId: 'job-4', 
        jobName: 'Export Job', 
        status: JobStatus.COMPLETED,
        priority: JobPriority.LOW,
        progress: 100
      })
    ],
    loading: false,
    onJobSelect: jest.fn(),
    onRefresh: jest.fn(),
    sortBy: 'startTime',
    sortDirection: 'desc' as const,
    filters: {}
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render job list correctly', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText('Data Import Job')).toBeInTheDocument();
      expect(screen.getByText('Validation Job')).toBeInTheDocument();
      expect(screen.getByText('Transform Job')).toBeInTheDocument();
      expect(screen.getByText('Export Job')).toBeInTheDocument();
    });

    it('should show job status badges correctly', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText('RUNNING')).toBeInTheDocument();
      expect(screen.getByText('PENDING')).toBeInTheDocument();
      expect(screen.getByText('FAILED')).toBeInTheDocument();
      expect(screen.getByText('COMPLETED')).toBeInTheDocument();
    });

    it('should display progress bars with correct values', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      const progressBars = screen.getAllByRole('progressbar');
      expect(progressBars).toHaveLength(4);
      
      // Check progress values are displayed
      expect(screen.getByText('75%')).toBeInTheDocument();
      expect(screen.getByText('0%')).toBeInTheDocument();
      expect(screen.getByText('30%')).toBeInTheDocument();
      expect(screen.getByText('100%')).toBeInTheDocument();
    });

    it('should show priority indicators', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText('HIGH')).toBeInTheDocument();
      expect(screen.getByText('NORMAL')).toBeInTheDocument();
      expect(screen.getByText('CRITICAL')).toBeInTheDocument();
      expect(screen.getByText('LOW')).toBeInTheDocument();
    });

    it('should display error counts for failed jobs', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText('5 errors')).toBeInTheDocument();
    });

    it('should show performance scores', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      const performanceScores = screen.getAllByText(/Score: \d+/);
      expect(performanceScores.length).toBeGreaterThan(0);
    });

    it('should display execution duration', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      // Should show duration or elapsed time for running jobs
      const timeElements = screen.getAllByText(/\d+[mhs]/);
      expect(timeElements.length).toBeGreaterThan(0);
    });
  });

  describe('Loading and Empty States', () => {
    it('should show loading skeleton when loading', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} loading={true} />
        </TestWrapper>
      );

      const skeletons = screen.getAllByTestId(/skeleton/i);
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it('should show empty state when no jobs', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} jobs={[]} />
        </TestWrapper>
      );

      expect(screen.getByText(/no jobs/i)).toBeInTheDocument();
      expect(screen.getByText(/try adjusting your filters/i)).toBeInTheDocument();
    });

    it('should show retry option when error occurs', () => {
      const mockRefresh = jest.fn();
      
      render(
        <TestWrapper>
          <JobStatusGrid 
            {...defaultProps} 
            jobs={[]} 
            onRefresh={mockRefresh}
            error="Failed to load jobs"
          />
        </TestWrapper>
      );

      expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
      
      const retryButton = screen.getByText(/retry/i);
      fireEvent.click(retryButton);
      
      expect(mockRefresh).toHaveBeenCalled();
    });
  });

  describe('User Interactions', () => {
    it('should handle job selection', async () => {
      const mockOnJobSelect = jest.fn();
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} onJobSelect={mockOnJobSelect} />
        </TestWrapper>
      );

      const jobCard = screen.getByText('Data Import Job').closest('[role="button"]');
      expect(jobCard).toBeInTheDocument();
      
      await user.click(jobCard!);
      expect(mockOnJobSelect).toHaveBeenCalledWith('job-1');
    });

    it('should handle job actions menu', async () => {
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      const actionButtons = screen.getAllByLabelText(/more actions/i);
      await user.click(actionButtons[0]);

      expect(screen.getByText(/view details/i)).toBeInTheDocument();
      expect(screen.getByText(/cancel job/i)).toBeInTheDocument();
      expect(screen.getByText(/restart job/i)).toBeInTheDocument();
    });

    it('should handle refresh button', async () => {
      const mockOnRefresh = jest.fn();
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} onRefresh={mockOnRefresh} />
        </TestWrapper>
      );

      const refreshButton = screen.getByLabelText(/refresh/i);
      await user.click(refreshButton);
      
      expect(mockOnRefresh).toHaveBeenCalled();
    });

    it('should handle keyboard navigation', async () => {
      const mockOnJobSelect = jest.fn();
      const user = userEvent.setup();
      
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} onJobSelect={mockOnJobSelect} />
        </TestWrapper>
      );

      const firstJob = screen.getByText('Data Import Job').closest('[role="button"]');
      firstJob?.focus();
      
      await user.keyboard('{Enter}');
      expect(mockOnJobSelect).toHaveBeenCalledWith('job-1');
    });
  });

  describe('Filtering and Sorting', () => {
    it('should apply status filters correctly', () => {
      const filteredProps = {
        ...defaultProps,
        filters: { status: [JobStatus.RUNNING, JobStatus.FAILED] }
      };
      
      render(
        <TestWrapper>
          <JobStatusGrid {...filteredProps} />
        </TestWrapper>
      );

      expect(screen.getByText('Data Import Job')).toBeInTheDocument();
      expect(screen.getByText('Transform Job')).toBeInTheDocument();
      expect(screen.queryByText('Validation Job')).not.toBeInTheDocument();
      expect(screen.queryByText('Export Job')).not.toBeInTheDocument();
    });

    it('should apply priority filters correctly', () => {
      const filteredProps = {
        ...defaultProps,
        filters: { priority: [JobPriority.CRITICAL, JobPriority.HIGH] }
      };
      
      render(
        <TestWrapper>
          <JobStatusGrid {...filteredProps} />
        </TestWrapper>
      );

      expect(screen.getByText('Data Import Job')).toBeInTheDocument();
      expect(screen.getByText('Transform Job')).toBeInTheDocument();
      expect(screen.queryByText('Validation Job')).not.toBeInTheDocument();
      expect(screen.queryByText('Export Job')).not.toBeInTheDocument();
    });

    it('should filter by source system', () => {
      const jobsWithDifferentSources = [
        createMockJob({ executionId: 'job-1', sourceSystem: 'System A' }),
        createMockJob({ executionId: 'job-2', sourceSystem: 'System B' }),
        createMockJob({ executionId: 'job-3', sourceSystem: 'System A' })
      ];

      const filteredProps = {
        ...defaultProps,
        jobs: jobsWithDifferentSources,
        filters: { sourceSystem: ['System A'] }
      };
      
      render(
        <TestWrapper>
          <JobStatusGrid {...filteredProps} />
        </TestWrapper>
      );

      expect(screen.getAllByText('System A')).toHaveLength(2);
      expect(screen.queryByText('System B')).not.toBeInTheDocument();
    });

    it('should handle search filtering', () => {
      const searchableProps = {
        ...defaultProps,
        filters: { search: 'Import' }
      };
      
      render(
        <TestWrapper>
          <JobStatusGrid {...searchableProps} />
        </TestWrapper>
      );

      expect(screen.getByText('Data Import Job')).toBeInTheDocument();
      expect(screen.queryByText('Validation Job')).not.toBeInTheDocument();
    });

    it('should handle sorting by different columns', () => {
      const sortedProps = {
        ...defaultProps,
        sortBy: 'jobName',
        sortDirection: 'asc' as const
      };
      
      render(
        <TestWrapper>
          <JobStatusGrid {...sortedProps} />
        </TestWrapper>
      );

      // Should render jobs in alphabetical order
      const jobCards = screen.getAllByRole('button');
      const jobNames = jobCards.map(card => 
        within(card).queryByText(/.*Job/)?.textContent
      ).filter(Boolean);

      expect(jobNames[0]).toBe('Data Import Job');
      expect(jobNames[1]).toBe('Export Job');
      expect(jobNames[2]).toBe('Transform Job');
      expect(jobNames[3]).toBe('Validation Job');
    });
  });

  describe('Real-time Updates', () => {
    it('should update job progress in real-time', () => {
      const { rerender } = render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText('75%')).toBeInTheDocument();

      const updatedJobs = defaultProps.jobs.map(job => 
        job.executionId === 'job-1' ? { ...job, progress: 90 } : job
      );

      rerender(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} jobs={updatedJobs} />
        </TestWrapper>
      );

      expect(screen.getByText('90%')).toBeInTheDocument();
    });

    it('should update job status changes', () => {
      const { rerender } = render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText('PENDING')).toBeInTheDocument();

      const updatedJobs = defaultProps.jobs.map(job => 
        job.executionId === 'job-2' ? { ...job, status: JobStatus.RUNNING } : job
      );

      rerender(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} jobs={updatedJobs} />
        </TestWrapper>
      );

      expect(screen.getAllByText('RUNNING')).toHaveLength(2);
    });

    it('should highlight recently updated jobs', () => {
      const recentlyUpdatedJobs = defaultProps.jobs.map(job => 
        job.executionId === 'job-1' 
          ? { ...job, lastHeartbeat: new Date().toISOString() }
          : job
      );

      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} jobs={recentlyUpdatedJobs} />
        </TestWrapper>
      );

      const updatedJobCard = screen.getByText('Data Import Job').closest('[role="button"]');
      expect(updatedJobCard).toHaveClass(/recently-updated/);
    });
  });

  describe('Compact View', () => {
    it('should render compact view correctly', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} compactView={true} />
        </TestWrapper>
      );

      // In compact view, should show essential information only
      expect(screen.getByText('Data Import Job')).toBeInTheDocument();
      expect(screen.getByText('RUNNING')).toBeInTheDocument();
      expect(screen.getByText('75%')).toBeInTheDocument();
    });

    it('should show more details in expanded view', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} compactView={false} />
        </TestWrapper>
      );

      // Should show additional details like performance scores, throughput, etc.
      expect(screen.getAllByText(/Score:/)).toHaveLength(4);
      expect(screen.getAllByText(/\/sec/)).toHaveLength(4);
    });
  });

  describe('Mobile Responsiveness', () => {
    beforeEach(() => {
      Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation(query => ({
          matches: query.includes('(max-width: 600px)'),
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
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      // Should render in mobile-friendly format
      expect(screen.getByText('Data Import Job')).toBeInTheDocument();
      
      // Mobile view should show stacked layout
      const jobCards = screen.getAllByRole('button');
      expect(jobCards).toHaveLength(4);
    });
  });

  describe('Performance Indicators', () => {
    it('should show trend indicators correctly', () => {
      const jobsWithTrends = [
        createMockJob({ 
          executionId: 'job-1', 
          trendIndicator: TrendIndicator.IMPROVING 
        }),
        createMockJob({ 
          executionId: 'job-2', 
          trendIndicator: TrendIndicator.DEGRADING 
        }),
        createMockJob({ 
          executionId: 'job-3', 
          trendIndicator: TrendIndicator.STABLE 
        })
      ];

      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} jobs={jobsWithTrends} />
        </TestWrapper>
      );

      expect(screen.getByTitle(/improving/i)).toBeInTheDocument();
      expect(screen.getByTitle(/degrading/i)).toBeInTheDocument();
      expect(screen.getByTitle(/stable/i)).toBeInTheDocument();
    });

    it('should display throughput metrics', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getAllByText(/100\/sec/)).toHaveLength(4);
    });
  });

  describe('Accessibility', () => {
    it('should have proper ARIA labels', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByRole('region', { name: /job status grid/i })).toBeInTheDocument();
      expect(screen.getAllByRole('button')).toHaveLength(8); // 4 job cards + 4 action menus
    });

    it('should support keyboard navigation', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      const jobCards = screen.getAllByRole('button');
      jobCards.forEach(card => {
        expect(card).toHaveAttribute('tabindex', '0');
      });
    });

    it('should have proper status announcements for screen readers', () => {
      render(
        <TestWrapper>
          <JobStatusGrid {...defaultProps} />
        </TestWrapper>
      );

      expect(screen.getByText(/4 jobs total/)).toBeInTheDocument();
      expect(screen.getByText(/1 running, 1 failed, 1 pending, 1 completed/)).toBeInTheDocument();
    });
  });
});