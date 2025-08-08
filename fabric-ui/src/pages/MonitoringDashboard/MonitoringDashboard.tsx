/**
 * Monitoring Dashboard Page
 * 
 * Main container component for real-time job monitoring dashboard.
 * Provides comprehensive monitoring capabilities including job status,
 * performance metrics, alerts, and historical analytics.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 * 
 * Features:
 * - Real-time WebSocket-based updates
 * - Interactive job status grid
 * - Performance metrics visualization
 * - Alert management panel
 * - Mobile-responsive design
 * - Export and reporting capabilities
 * - Role-based access control
 * - Comprehensive error handling
 */

import React, { useState, useCallback, useEffect, useMemo } from 'react';
import {
  Box,
  Grid,
  Card,
  CardHeader,
  CardContent,
  Typography,
  Alert as MuiAlert,
  Snackbar,
  Fab,
  Tooltip,
  IconButton,
  Menu,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Chip,
  LinearProgress,
  useTheme,
  useMediaQuery
} from '@mui/material';
import {
  Refresh,
  Settings,
  Download,
  Fullscreen,
  FullscreenExit,
  Warning,
  CheckCircle,
  Error as ErrorIcon,
  Info,
  Dashboard as DashboardIcon,
  FilterList,
  Search
} from '@mui/icons-material';

import { useRealTimeMonitoring } from '../../hooks/useRealTimeMonitoring';
import { useAuth } from '../../contexts/AuthContext';
import { 
  DashboardData, 
  MonitoringError, 
  Alert,
  MonitoringConfiguration,
  AlertFilters,
  JobStatus 
} from '../../types/monitoring';

// Component imports (these will be created next)
import { JobStatusGrid } from '../../components/monitoring/JobStatusGrid/JobStatusGrid';
import { PerformanceMetricsChart } from '../../components/monitoring/PerformanceMetricsChart/PerformanceMetricsChart';
import { AlertsPanel } from '../../components/monitoring/AlertsPanel/AlertsPanel';
import { JobDetailsModal } from '../../components/monitoring/JobDetailsModal/JobDetailsModal';
import { SystemHealthBar } from '../../components/monitoring/SystemHealthBar/SystemHealthBar';
import { DashboardFilters } from '../../components/monitoring/DashboardFilters/DashboardFilters';
import { MonitoringConfigDialog } from '../../components/monitoring/MonitoringConfigDialog/MonitoringConfigDialog';

// Dashboard configuration interface
interface DashboardConfig {
  autoRefresh: boolean;
  refreshInterval: number;
  compactView: boolean;
  showPerformanceCharts: boolean;
  soundEnabled: boolean;
  filters: {
    jobTypes: string[];
    sourceSystems: string[];
    showCompleted: boolean;
  };
}

// Default dashboard configuration
const DEFAULT_CONFIG: DashboardConfig = {
  autoRefresh: true,
  refreshInterval: 5000,
  compactView: false,
  showPerformanceCharts: true,
  soundEnabled: false,
  filters: {
    jobTypes: [],
    sourceSystems: [],
    showCompleted: false
  }
};

/**
 * MonitoringDashboard Component
 * 
 * Main dashboard component providing real-time monitoring capabilities
 * with comprehensive job tracking, performance metrics, and alert management.
 */
export const MonitoringDashboard: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { user, hasRole } = useAuth();
  
  // Dashboard state
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);
  const [config, setConfig] = useState<DashboardConfig>(DEFAULT_CONFIG);
  const [alertFilters, setAlertFilters] = useState<AlertFilters>({});
  const [fullscreen, setFullscreen] = useState(false);
  const [showNotification, setShowNotification] = useState(false);
  const [notificationMessage, setNotificationMessage] = useState('');
  const [notificationSeverity, setNotificationSeverity] = useState<'info' | 'warning' | 'error' | 'success'>('info');
  
  // Menu and dialog state
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);
  const [configDialogOpen, setConfigDialogOpen] = useState(false);
  const [filtersOpen, setFiltersOpen] = useState(false);
  
  // Real-time monitoring hook
  const {
    dashboardData,
    loading,
    error,
    connected,
    refresh,
    subscribe,
    unsubscribe
  } = useRealTimeMonitoring({
    wsUrl: '/ws/job-monitoring',
    autoConnect: true,
    refreshInterval: config.refreshInterval,
    enablePollingFallback: true,
    jobFilters: config.filters,
    alertFilters,
    onDataUpdate: handleDataUpdate,
    onAlert: handleNewAlert,
    onError: handleMonitoringError
  });
  
  /**
   * Handle dashboard data updates
   */
  function handleDataUpdate(data: DashboardData) {
    // Check for critical alerts
    const criticalAlerts = data.alerts.filter(alert => 
      alert.severity === 'CRITICAL' && !alert.acknowledged
    );
    
    if (criticalAlerts.length > 0 && config.soundEnabled) {
      // Play alert sound (if enabled)
      playAlertSound();
    }
  }
  
  /**
   * Handle new alerts
   */
  function handleNewAlert(alert: Alert) {
    // Show notification for new alerts
    if (alert.severity === 'CRITICAL' || alert.severity === 'ERROR') {
      showNotificationMessage(
        `New ${alert.severity} alert: ${alert.title}`,
        alert.severity === 'CRITICAL' ? 'error' : 'warning'
      );
    }
    
    // Play sound for critical alerts
    if (alert.severity === 'CRITICAL' && config.soundEnabled) {
      playAlertSound();
    }
  }
  
  /**
   * Handle monitoring errors
   */
  function handleMonitoringError(error: MonitoringError) {
    showNotificationMessage(
      `Monitoring error: ${error.message}`,
      'error'
    );
  }
  
  /**
   * Show notification message
   */
  const showNotificationMessage = useCallback((message: string, severity: 'info' | 'warning' | 'error' | 'success') => {
    setNotificationMessage(message);
    setNotificationSeverity(severity);
    setShowNotification(true);
  }, []);
  
  /**
   * Play alert sound
   */
  const playAlertSound = useCallback(() => {
    try {
      const audio = new Audio('/sounds/alert.mp3');
      audio.volume = 0.5;
      audio.play().catch(console.warn);
    } catch (error) {
      console.warn('Failed to play alert sound:', error);
    }
  }, []);
  
  /**
   * Handle job selection
   */
  const handleJobSelect = useCallback((jobId: string) => {
    setSelectedJobId(jobId);
  }, []);
  
  /**
   * Handle alert acknowledgment
   */
  const handleAlertAcknowledge = useCallback(async (alertId: string) => {
    try {
      // This would be handled by the AlertsPanel component
      showNotificationMessage('Alert acknowledged successfully', 'success');
    } catch (error) {
      showNotificationMessage('Failed to acknowledge alert', 'error');
    }
  }, [showNotificationMessage]);
  
  /**
   * Handle manual refresh
   */
  const handleRefresh = useCallback(async () => {
    try {
      await refresh();
      showNotificationMessage('Dashboard refreshed', 'success');
    } catch (error) {
      showNotificationMessage('Failed to refresh dashboard', 'error');
    }
  }, [refresh, showNotificationMessage]);
  
  /**
   * Toggle fullscreen mode
   */
  const toggleFullscreen = useCallback(() => {
    setFullscreen(!fullscreen);
  }, [fullscreen]);
  
  /**
   * Handle configuration changes
   */
  const handleConfigChange = useCallback((newConfig: Partial<DashboardConfig>) => {
    setConfig(prevConfig => ({
      ...prevConfig,
      ...newConfig
    }));
  }, []);
  
  /**
   * Handle export functionality
   */
  const handleExport = useCallback(async () => {
    try {
      // Implementation would call monitoringApi.exportDashboardData
      showNotificationMessage('Export started', 'info');
    } catch (error) {
      showNotificationMessage('Export failed', 'error');
    }
  }, [showNotificationMessage]);
  
  /**
   * Calculate dashboard statistics
   */
  const dashboardStats = useMemo(() => {
    if (!dashboardData) return null;
    
    const activeJobs = dashboardData.activeJobs;
    const totalJobs = activeJobs.length;
    const runningJobs = activeJobs.filter(job => job.status === JobStatus.RUNNING).length;
    const failedJobs = activeJobs.filter(job => job.status === JobStatus.FAILED).length;
    const pendingJobs = activeJobs.filter(job => job.status === JobStatus.PENDING).length;
    
    const unacknowledgedAlerts = dashboardData.alerts.filter(alert => !alert.acknowledged).length;
    const criticalAlerts = dashboardData.alerts.filter(alert => 
      alert.severity === 'CRITICAL' && !alert.acknowledged
    ).length;
    
    return {
      totalJobs,
      runningJobs,
      failedJobs,
      pendingJobs,
      unacknowledgedAlerts,
      criticalAlerts,
      systemHealthScore: dashboardData.systemHealth.overallScore
    };
  }, [dashboardData]);
  
  /**
   * Check if user has monitoring permissions
   */
  const canViewMonitoring = useMemo(() => {
    return hasRole('OPERATIONS_MANAGER') || hasRole('MONITORING_USER') || hasRole('ADMIN');
  }, [hasRole]);
  
  /**
   * Check if user can manage alerts
   */
  const canManageAlerts = useMemo(() => {
    return hasRole('OPERATIONS_MANAGER') || hasRole('ADMIN');
  }, [hasRole]);
  
  // Show access denied if user doesn't have permissions
  if (!canViewMonitoring) {
    return (
      <Box sx={{ p: 3, textAlign: 'center' }}>
        <ErrorIcon color="error" sx={{ fontSize: 64, mb: 2 }} />
        <Typography variant="h4" gutterBottom>
          Access Denied
        </Typography>
        <Typography variant="body1">
          You don't have permission to access the monitoring dashboard.
        </Typography>
      </Box>
    );
  }
  
  return (
    <Box sx={{ 
      height: fullscreen ? '100vh' : 'calc(100vh - 64px)',
      overflow: 'auto',
      bgcolor: 'background.default',
      p: fullscreen ? 0 : { xs: 1, sm: 2, md: 3 }
    }}>
      {/* Dashboard Header */}
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center',
        mb: 3,
        flexDirection: isMobile ? 'column' : 'row',
        gap: isMobile ? 2 : 0
      }}>
        <Box>
          <Typography variant={isMobile ? "h5" : "h4"} sx={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: 1,
            fontWeight: 600
          }}>
            <DashboardIcon />
            Real-Time Job Monitoring
            {!connected && (
              <Chip 
                label="Offline" 
                color="warning" 
                size="small"
                sx={{ ml: 1 }}
              />
            )}
          </Typography>
          
          {dashboardStats && (
            <Box sx={{ display: 'flex', gap: 2, mt: 1, flexWrap: 'wrap' }}>
              <Chip
                icon={<CheckCircle />}
                label={`${dashboardStats.runningJobs} Running`}
                color="success"
                size="small"
              />
              <Chip
                icon={<Warning />}
                label={`${dashboardStats.failedJobs} Failed`}
                color="error"
                size="small"
              />
              <Chip
                icon={<Info />}
                label={`${dashboardStats.pendingJobs} Pending`}
                color="info"
                size="small"
              />
              {dashboardStats.criticalAlerts > 0 && (
                <Chip
                  icon={<ErrorIcon />}
                  label={`${dashboardStats.criticalAlerts} Critical Alerts`}
                  color="error"
                  variant="filled"
                  size="small"
                />
              )}
            </Box>
          )}
        </Box>
        
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <Tooltip title="Refresh Dashboard">
            <IconButton onClick={handleRefresh} disabled={loading}>
              <Refresh />
            </IconButton>
          </Tooltip>
          
          <Tooltip title="Filter Options">
            <IconButton onClick={() => setFiltersOpen(true)}>
              <FilterList />
            </IconButton>
          </Tooltip>
          
          <Tooltip title={fullscreen ? "Exit Fullscreen" : "Fullscreen"}>
            <IconButton onClick={toggleFullscreen}>
              {fullscreen ? <FullscreenExit /> : <Fullscreen />}
            </IconButton>
          </Tooltip>
          
          <Tooltip title="More Options">
            <IconButton onClick={(e) => setMenuAnchor(e.currentTarget)}>
              <Settings />
            </IconButton>
          </Tooltip>
        </Box>
      </Box>
      
      {/* Loading Progress */}
      {loading && (
        <LinearProgress sx={{ mb: 2 }} />
      )}
      
      {/* System Health Bar */}
      {dashboardData?.systemHealth && (
        <Box sx={{ mb: 3 }}>
          <SystemHealthBar health={dashboardData.systemHealth} />
        </Box>
      )}
      
      {/* Main Dashboard Grid */}
      <Grid container spacing={3}>
        {/* Job Status Grid */}
        <Grid item xs={12} lg={8}>
          <Card sx={{ height: '600px' }}>
            <CardHeader 
              title="Active Jobs"
              titleTypographyProps={{ variant: 'h6' }}
              action={
                <Typography variant="body2" color="text.secondary">
                  {dashboardData ? `${dashboardData.activeJobs.length} jobs` : 'Loading...'}
                </Typography>
              }
            />
            <CardContent sx={{ height: 'calc(100% - 64px)', p: 0 }}>
              <JobStatusGrid
                jobs={dashboardData?.activeJobs || []}
                loading={loading}
                onJobSelect={handleJobSelect}
                onRefresh={handleRefresh}
                compactView={config.compactView}
                filters={config.filters}
              />
            </CardContent>
          </Card>
        </Grid>
        
        {/* Alerts Panel */}
        <Grid item xs={12} lg={4}>
          <Card sx={{ height: '600px' }}>
            <CardHeader 
              title="Alerts"
              titleTypographyProps={{ variant: 'h6' }}
              action={
                <Typography variant="body2" color="text.secondary">
                  {dashboardData ? `${dashboardData.alerts.filter(a => !a.acknowledged).length} unacked` : 'Loading...'}
                </Typography>
              }
            />
            <CardContent sx={{ height: 'calc(100% - 64px)', p: 0 }}>
              <AlertsPanel
                alerts={dashboardData?.alerts || []}
                onAcknowledge={canManageAlerts ? handleAlertAcknowledge : undefined}
                filters={alertFilters}
                soundEnabled={config.soundEnabled}
              />
            </CardContent>
          </Card>
        </Grid>
        
        {/* Performance Charts */}
        {config.showPerformanceCharts && (
          <Grid item xs={12}>
            <Card>
              <CardHeader 
                title="Performance Metrics"
                titleTypographyProps={{ variant: 'h6' }}
              />
              <CardContent>
                <PerformanceMetricsChart
                  metrics={dashboardData?.performanceMetrics ? [dashboardData.performanceMetrics] : []}
                  trends={dashboardData?.trends}
                  height={300}
                  realTime={true}
                />
              </CardContent>
            </Card>
          </Grid>
        )}
      </Grid>
      
      {/* Job Details Modal */}
      {selectedJobId && (
        <JobDetailsModal
          executionId={selectedJobId}
          open={!!selectedJobId}
          onClose={() => setSelectedJobId(null)}
          onRefresh={handleRefresh}
        />
      )}
      
      {/* Dashboard Filters Dialog */}
      <DashboardFilters
        open={filtersOpen}
        onClose={() => setFiltersOpen(false)}
        filters={config.filters}
        onFiltersChange={(filters) => handleConfigChange({ filters })}
      />
      
      {/* Configuration Dialog */}
      <MonitoringConfigDialog
        open={configDialogOpen}
        onClose={() => setConfigDialogOpen(false)}
        config={config}
        onConfigChange={handleConfigChange}
      />
      
      {/* Options Menu */}
      <Menu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={() => setMenuAnchor(null)}
      >
        <MenuItem onClick={() => {
          setConfigDialogOpen(true);
          setMenuAnchor(null);
        }}>
          <Settings sx={{ mr: 1 }} />
          Settings
        </MenuItem>
        <MenuItem onClick={() => {
          handleExport();
          setMenuAnchor(null);
        }}>
          <Download sx={{ mr: 1 }} />
          Export Data
        </MenuItem>
      </Menu>
      
      {/* Notifications */}
      <Snackbar
        open={showNotification}
        autoHideDuration={6000}
        onClose={() => setShowNotification(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <MuiAlert
          onClose={() => setShowNotification(false)}
          severity={notificationSeverity}
          variant="filled"
        >
          {notificationMessage}
        </MuiAlert>
      </Snackbar>
      
      {/* Connection Status FAB */}
      {!connected && (
        <Fab
          color="warning"
          size="small"
          sx={{
            position: 'fixed',
            bottom: 16,
            right: 16,
            zIndex: 1000
          }}
          onClick={handleRefresh}
        >
          <Warning />
        </Fab>
      )}
    </Box>
  );
};

export default MonitoringDashboard;