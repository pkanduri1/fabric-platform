/**
 * Manual Job Configuration Page - Phase 3A Core Interface
 * 
 * Enterprise-grade React component implementing comprehensive CRUD operations
 * for manual job configuration management. Features role-based access controls,
 * JWT authentication integration, and banking-grade security compliance.
 * 
 * Features:
 * - Complete CRUD operations (Create, Read, Update, Delete)
 * - Role-based UI rendering and access controls
 * - Advanced filtering, pagination, and sorting
 * - Real-time form validation with comprehensive error handling
 * - Responsive Material-UI design for operations teams
 * - Integration with Phase 2 REST APIs
 * - SOX-compliant audit trail support
 * 
 * Security:
 * - JWT token-based authentication
 * - Role-based component rendering (JOB_CREATOR, JOB_MODIFIER, JOB_EXECUTOR, JOB_VIEWER)
 * - Input sanitization and validation
 * - Secure API communication with correlation IDs
 * 
 * @author Claude Code
 * @version 1.0
 * @since Phase 3A Implementation
 */

import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Container,
  Typography,
  Paper,
  Tab,
  Tabs,
  Fab,
  Snackbar,
  Alert,
  CircularProgress,
  Backdrop,
  Breadcrumbs,
  Link,
  Chip,
  Stack
} from '@mui/material';
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
  Dashboard as DashboardIcon,
  Work as WorkIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { 
  ManualJobConfigApiService, 
  JobConfigurationResponse, 
  JobConfigurationListParams,
  SystemStatistics 
} from '../../services/api/manualJobConfigApi';
import JobConfigurationForm from '../../components/JobConfigurationForm';
import JobConfigurationList from '../../components/JobConfigurationList';

// Tab panel component for organized content display
interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel({ children, value, index }: TabPanelProps) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`job-config-tabpanel-${index}`}
      aria-labelledby={`job-config-tab-${index}`}
    >
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );
}

// Main component
export const ManualJobConfigurationPage: React.FC = () => {
  // Authentication and authorization
  const { user, hasRole, hasAnyRole } = useAuth();

  // Component state
  const [activeTab, setActiveTab] = useState(0);
  const [configurations, setConfigurations] = useState<JobConfigurationResponse[]>([]);
  const [systemStats, setSystemStats] = useState<SystemStatistics | null>(null);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedConfig, setSelectedConfig] = useState<JobConfigurationResponse | null>(null);
  const [isFormDialogOpen, setIsFormDialogOpen] = useState(false);
  const [formMode, setFormMode] = useState<'create' | 'edit' | 'view'>('create');
  
  // Pagination and filtering state
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [filters, setFilters] = useState<JobConfigurationListParams>({});
  const [sortBy, setSortBy] = useState('createdDate');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');

  // Notification state
  const [notification, setNotification] = useState<{
    open: boolean;
    message: string;
    severity: 'success' | 'error' | 'warning' | 'info';
  }>({
    open: false,
    message: '',
    severity: 'info'
  });

  // Check if user has required permissions to view this page
  const canViewConfigurations = hasAnyRole(['JOB_VIEWER', 'JOB_CREATOR', 'JOB_MODIFIER', 'JOB_EXECUTOR']);
  const canCreateConfigurations = hasAnyRole(['JOB_CREATOR', 'JOB_MODIFIER']);
  const canModifyConfigurations = hasRole('JOB_MODIFIER');

  // Load configurations and system statistics
  const loadConfigurations = useCallback(async () => {
    if (!canViewConfigurations) {
      console.warn('[ManualJobConfig Page] User lacks permission to view configurations');
      return;
    }

    setLoading(true);
    try {
      const params: JobConfigurationListParams = {
        page: currentPage,
        size: pageSize,
        sortBy,
        sortDir,
        ...filters
      };

      const response = await ManualJobConfigApiService.getAllJobConfigurations(params);
      setConfigurations(response.content);
      setTotalElements(response.totalElements);

      console.log(`[ManualJobConfig Page] Loaded ${response.content.length} of ${response.totalElements} configurations`);
    } catch (error: any) {
      console.error('[ManualJobConfig Page] Failed to load configurations:', error);
      showNotification('Failed to load job configurations', 'error');
    } finally {
      setLoading(false);
    }
  }, [canViewConfigurations, currentPage, pageSize, sortBy, sortDir, filters]);

  const loadSystemStatistics = useCallback(async () => {
    if (!canViewConfigurations) return;

    try {
      const stats = await ManualJobConfigApiService.getSystemStatistics();
      setSystemStats(stats);
    } catch (error: any) {
      console.error('[ManualJobConfig Page] Failed to load statistics:', error);
      // Don't show error for stats as it's secondary data
    }
  }, [canViewConfigurations]);

  // Initial data loading
  useEffect(() => {
    loadConfigurations();
    loadSystemStatistics();
  }, [loadConfigurations, loadSystemStatistics]);

  // Notification helper
  const showNotification = (message: string, severity: 'success' | 'error' | 'warning' | 'info') => {
    setNotification({ open: true, message, severity });
  };

  const handleCloseNotification = () => {
    setNotification({ ...notification, open: false });
  };

  // Tab change handler
  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  // Refresh data handler
  const handleRefresh = async () => {
    setRefreshing(true);
    await Promise.all([loadConfigurations(), loadSystemStatistics()]);
    setRefreshing(false);
    showNotification('Data refreshed successfully', 'success');
  };

  // Configuration action handlers
  const handleCreateConfiguration = () => {
    if (!canCreateConfigurations) {
      showNotification('Insufficient permissions to create configurations', 'warning');
      return;
    }
    setSelectedConfig(null);
    setFormMode('create');
    setIsFormDialogOpen(true);
  };

  const handleEditConfiguration = (config: JobConfigurationResponse) => {
    if (!canModifyConfigurations) {
      showNotification('Insufficient permissions to modify configurations', 'warning');
      return;
    }
    setSelectedConfig(config);
    setFormMode('edit');
    setIsFormDialogOpen(true);
  };

  const handleViewConfiguration = (config: JobConfigurationResponse) => {
    setSelectedConfig(config);
    setFormMode('view');
    setIsFormDialogOpen(true);
  };

  const handleDeleteConfiguration = async (config: JobConfigurationResponse) => {
    if (!canModifyConfigurations) {
      showNotification('Insufficient permissions to delete configurations', 'warning');
      return;
    }

    const confirmed = window.confirm(
      `Are you sure you want to deactivate "${config.jobName}"?\n\nThis action will mark the configuration as inactive but preserve it for audit purposes.`
    );

    if (!confirmed) return;

    try {
      await ManualJobConfigApiService.deactivateJobConfiguration(
        config.configId,
        'User initiated deactivation via UI'
      );

      showNotification(`Configuration "${config.jobName}" has been deactivated successfully`, 'success');
      await loadConfigurations(); // Reload the list
    } catch (error: any) {
      console.error('[ManualJobConfig Page] Failed to deactivate configuration:', error);
      showNotification(`Failed to deactivate configuration: ${error.message}`, 'error');
    }
  };

  const handleFormSuccess = (config: JobConfigurationResponse) => {
    const action = formMode === 'create' ? 'created' : 'updated';
    showNotification(`Configuration "${config.jobName}" has been ${action} successfully`, 'success');
    loadConfigurations(); // Reload the list
  };

  const handleFormClose = () => {
    setIsFormDialogOpen(false);
    setSelectedConfig(null);
  };

  // List management handlers - MEMOIZED to prevent infinite re-renders
  const handlePageChange = useCallback((page: number, size: number) => {
    setCurrentPage(page);
    setPageSize(size);
  }, []);

  const handleFiltersChange = useCallback((newFilters: JobConfigurationListParams) => {
    setFilters(newFilters);
    setCurrentPage(0); // Reset to first page when filters change
  }, []);

  const handleSortChange = useCallback((newSortBy: string, newSortDir: 'asc' | 'desc') => {
    setSortBy(newSortBy);
    setSortDir(newSortDir);
  }, []);

  // Check permissions and render appropriate UI
  if (!canViewConfigurations) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <WorkIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h5" component="h1" gutterBottom>
            Access Denied
          </Typography>
          <Typography variant="body1" color="text.secondary">
            You do not have the required permissions to view job configurations.
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Required roles: JOB_VIEWER, JOB_CREATOR, JOB_MODIFIER, or JOB_EXECUTOR
          </Typography>
        </Paper>
      </Container>
    );
  }

  // Component render state logging
  if (process.env.NODE_ENV === 'development') {
    console.log(`[ManualJobConfig Page] Rendering - ${configurations.length} configurations, loading: ${loading}`);
  }

  return (
    <Container maxWidth="xl" sx={{ mt: 2, mb: 4 }}>
      {/* Loading backdrop */}
      <Backdrop sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }} open={loading}>
        <CircularProgress color="inherit" />
      </Backdrop>

      {/* Page Header */}
      <Box sx={{ mb: 4 }}>
        {/* Breadcrumbs */}
        <Breadcrumbs aria-label="breadcrumb" sx={{ mb: 2 }}>
          <Link 
            underline="hover" 
            color="inherit" 
            href="/dashboard"
            sx={{ display: 'flex', alignItems: 'center' }}
          >
            <DashboardIcon sx={{ mr: 0.5 }} fontSize="inherit" />
            Dashboard
          </Link>
          <Typography color="text.primary" sx={{ display: 'flex', alignItems: 'center' }}>
            <WorkIcon sx={{ mr: 0.5 }} fontSize="inherit" />
            Manual Job Configuration
          </Typography>
        </Breadcrumbs>

        {/* Page Title and Actions */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Box>
            <Typography variant="h4" component="h1" gutterBottom>
              Manual Job Configuration Management
            </Typography>
            <Typography variant="subtitle1" color="text.secondary" gutterBottom>
              Configure and manage batch processing jobs with enterprise-grade controls
            </Typography>
            
            {/* User permissions display */}
            <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
              <Chip 
                size="small" 
                label={`User: ${user?.username}`} 
                color="primary" 
                variant="outlined" 
              />
              {user?.roles?.map((role) => (
                <Chip 
                  key={role} 
                  size="small" 
                  label={role} 
                  color="secondary" 
                  variant="outlined" 
                />
              ))}
            </Stack>
          </Box>

          {/* Action buttons */}
          <Stack direction="row" spacing={2}>
            <Fab
              color="primary"
              variant="extended"
              onClick={handleRefresh}
              disabled={refreshing}
              size="medium"
            >
              <RefreshIcon sx={{ mr: 1 }} />
              {refreshing ? 'Refreshing...' : 'Refresh'}
            </Fab>
            
            {canCreateConfigurations && (
              <Fab
                color="secondary"
                variant="extended"
                onClick={handleCreateConfiguration}
                size="medium"
              >
                <AddIcon sx={{ mr: 1 }} />
                Create Configuration
              </Fab>
            )}
          </Stack>
        </Box>

        {/* System Statistics Summary */}
        {systemStats && (
          <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
            <Chip 
              label={`Active: ${systemStats.activeConfigurations}`} 
              color="success" 
              variant="filled" 
            />
            <Chip 
              label={`Inactive: ${systemStats.inactiveConfigurations}`} 
              color="default" 
              variant="filled" 
            />
            <Chip 
              label={`Total: ${systemStats.totalConfigurations}`} 
              color="primary" 
              variant="filled" 
            />
            <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto', alignSelf: 'center' }}>
              Last Updated: {new Date(systemStats.lastUpdated).toLocaleString()}
            </Typography>
          </Box>
        )}
      </Box>

      {/* Main Content Tabs */}
      <Paper sx={{ width: '100%' }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs
            value={activeTab}
            onChange={handleTabChange}
            aria-label="job configuration management tabs"
            variant="fullWidth"
          >
            <Tab 
              label="Configuration List" 
              id="job-config-tab-0"
              aria-controls="job-config-tabpanel-0"
            />
            <Tab 
              label="Dashboard & Analytics" 
              id="job-config-tab-1"
              aria-controls="job-config-tabpanel-1"
              disabled // Will be enabled in future phases
            />
            <Tab 
              label="System Health" 
              id="job-config-tab-2"
              aria-controls="job-config-tabpanel-2"
              disabled // Will be enabled in future phases
            />
          </Tabs>
        </Box>

        {/* Tab Panels */}
        <TabPanel value={activeTab} index={0}>
          <JobConfigurationList
            configurations={configurations}
            loading={loading}
            totalElements={totalElements}
            currentPage={currentPage}
            pageSize={pageSize}
            onPageChange={handlePageChange}
            onFiltersChange={handleFiltersChange}
            onSortChange={handleSortChange}
            onRefresh={handleRefresh}
            onEdit={handleEditConfiguration}
            onView={handleViewConfiguration}
            onDelete={handleDeleteConfiguration}
          />
        </TabPanel>

        <TabPanel value={activeTab} index={1}>
          <Box sx={{ p: 3, textAlign: 'center' }}>
            <Typography variant="h6" color="text.secondary">
              Dashboard & Analytics
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Advanced analytics and monitoring dashboard (Phase 3B)
            </Typography>
          </Box>
        </TabPanel>

        <TabPanel value={activeTab} index={2}>
          <Box sx={{ p: 3, textAlign: 'center' }}>
            <Typography variant="h6" color="text.secondary">
              System Health Monitoring
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Real-time system health and performance metrics (Phase 3C)
            </Typography>
          </Box>
        </TabPanel>
      </Paper>

      {/* Job Configuration Form Dialog */}
      <JobConfigurationForm
        open={isFormDialogOpen}
        onClose={handleFormClose}
        onSuccess={handleFormSuccess}
        mode={formMode}
        initialData={selectedConfig}
      />

      {/* Notification Snackbar */}
      <Snackbar 
        open={notification.open} 
        autoHideDuration={6000} 
        onClose={handleCloseNotification}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert 
          onClose={handleCloseNotification} 
          severity={notification.severity} 
          sx={{ width: '100%' }}
          variant="filled"
        >
          {notification.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default ManualJobConfigurationPage;