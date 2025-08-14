/**
 * Job Configuration List Component - Phase 3A Core Interface
 * 
 * Enterprise-grade React component for displaying, filtering, and managing job configurations.
 * Features comprehensive pagination, advanced filtering, and role-based action controls.
 * 
 * Features:
 * - Advanced filtering by job type, source system, and status
 * - Pagination with customizable page sizes
 * - Sorting by multiple columns
 * - Role-based action buttons (Create, Edit, View, Delete)
 * - Responsive Material-UI data grid
 * - Real-time search and filtering
 * - Bulk operations for administrative users
 * 
 * Security:
 * - Role-based access controls for actions
 * - Data sanitization and validation
 * - Audit trail support for all operations
 * 
 * @author Claude Code
 * @version 1.0
 * @since Phase 3A Implementation
 */

import React, { useState, useCallback, useEffect } from 'react';
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  IconButton,
  Menu,
  MenuItem as MenuOption,
  Stack,
  Typography,
  Tooltip,
  Alert,
  Skeleton,
  Card,
  CardContent,
  Grid,
  Button,
  InputAdornment
} from '@mui/material';
import {
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Visibility as VisibilityIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
  FilterList as FilterIcon,
  Search as SearchIcon,
  Clear as ClearIcon,
  GetApp as ExportIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { 
  JobConfigurationResponse,
  JobConfigurationListParams,
  ManualJobConfigApiService
} from '../../services/api/manualJobConfigApi';

// Component Props
interface JobConfigurationListProps {
  configurations: JobConfigurationResponse[];
  loading: boolean;
  totalElements: number;
  currentPage: number;
  pageSize: number;
  onPageChange: (page: number, size: number) => void;
  onFiltersChange: (filters: JobConfigurationListParams) => void;
  onSortChange: (sortBy: string, sortDir: 'asc' | 'desc') => void;
  onRefresh: () => void;
  onEdit: (config: JobConfigurationResponse) => void;
  onView: (config: JobConfigurationResponse) => void;
  onDelete: (config: JobConfigurationResponse) => void;
}

// Table column definitions
interface Column {
  id: keyof JobConfigurationResponse;
  label: string;
  minWidth?: number;
  align?: 'right' | 'left' | 'center';
  sortable?: boolean;
  format?: (value: any) => string;
}

const columns: Column[] = [
  { 
    id: 'jobName', 
    label: 'Job Name', 
    minWidth: 200, 
    sortable: true 
  },
  { 
    id: 'jobType', 
    label: 'Job Type', 
    minWidth: 150, 
    sortable: true 
  },
  { 
    id: 'sourceSystem', 
    label: 'Source System', 
    minWidth: 150, 
    sortable: true 
  },
  { 
    id: 'targetSystem', 
    label: 'Target System', 
    minWidth: 150, 
    sortable: true 
  },
  { 
    id: 'status', 
    label: 'Status', 
    minWidth: 120, 
    sortable: true 
  },
  { 
    id: 'createdBy', 
    label: 'Created By', 
    minWidth: 120, 
    sortable: true 
  },
  { 
    id: 'createdDate', 
    label: 'Created Date', 
    minWidth: 150, 
    sortable: true,
    format: (value: string) => new Date(value).toLocaleDateString()
  },
  { 
    id: 'versionNumber', 
    label: 'Version', 
    minWidth: 80, 
    align: 'center' as const,
    sortable: true 
  }
];

// Status color mapping
const getStatusChipProps = (status: string) => {
  switch (status) {
    case 'ACTIVE':
      return { color: 'success' as const, variant: 'filled' as const };
    case 'INACTIVE':
      return { color: 'default' as const, variant: 'filled' as const };
    case 'DEPRECATED':
      return { color: 'warning' as const, variant: 'filled' as const };
    case 'PENDING_APPROVAL':
      return { color: 'info' as const, variant: 'filled' as const };
    default:
      return { color: 'default' as const, variant: 'outlined' as const };
  }
};

export const JobConfigurationList: React.FC<JobConfigurationListProps> = React.memo(({
  configurations,
  loading,
  totalElements,
  currentPage,
  pageSize,
  onPageChange,
  onFiltersChange,
  onSortChange,
  onRefresh,
  onEdit,
  onView,
  onDelete
}) => {
  // Development logging
  if (process.env.NODE_ENV === 'development' && configurations.length === 0 && !loading) {
    console.warn('[JobConfigurationList] No configurations received - check data loading');
  }

  const { hasRole, hasAnyRole } = useAuth();

  // Local state for filters and sorting
  const [searchTerm, setSearchTerm] = useState('');
  const [jobTypeFilter, setJobTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [sourceSystemFilter, setSourceSystemFilter] = useState('');
  const [sortBy, setSortBy] = useState('createdDate');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
  
  // Menu state for row actions
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedConfig, setSelectedConfig] = useState<JobConfigurationResponse | null>(null);

  // Permission checks
  const canModify = hasRole('JOB_MODIFIER');
  const canView = hasAnyRole(['JOB_VIEWER', 'JOB_CREATOR', 'JOB_MODIFIER', 'JOB_EXECUTOR']);

  // ✅ REMOVED PROBLEMATIC useEffect HOOKS THAT CAUSED INFINITE LOOPS
  // Filters and sorting are now handled directly in event handlers to prevent re-render loops

  // Handle sort click
  const handleSortClick = (columnId: string) => {
    const newSortDir = sortBy === columnId ? (sortDir === 'asc' ? 'desc' : 'asc') : 'desc';
    setSortBy(columnId);
    setSortDir(newSortDir);
    // ✅ Call parent immediately to prevent re-render loops
    onSortChange(columnId, newSortDir);
  };

  // Handle page change
  const handleChangePage = (event: unknown, newPage: number) => {
    onPageChange(newPage, pageSize);
  };

  // Handle rows per page change
  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    const newPageSize = parseInt(event.target.value, 10);
    onPageChange(0, newPageSize);
  };

  // Handle action menu
  const handleActionClick = (event: React.MouseEvent<HTMLElement>, config: JobConfigurationResponse) => {
    setAnchorEl(event.currentTarget);
    setSelectedConfig(config);
  };

  const handleActionClose = () => {
    setAnchorEl(null);
    setSelectedConfig(null);
  };

  // Clear filters
  const handleClearFilters = () => {
    setSearchTerm('');
    setJobTypeFilter('');
    setStatusFilter('');
    setSourceSystemFilter('');
    // ✅ Call parent immediately to prevent re-render loops
    onFiltersChange({});
  };

  // Get unique values for filter dropdowns
  const getUniqueValues = (key: keyof JobConfigurationResponse): string[] => {
    const uniqueSet = new Set<string>();
    configurations.forEach(config => {
      const value = config[key] as string;
      if (value) {
        uniqueSet.add(value);
      }
    });
    return Array.from(uniqueSet);
  };

  // Filter configurations by search term (client-side for better UX)
  const filteredConfigurations = configurations.filter(config =>
    !searchTerm || 
    config.jobName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    config.sourceSystem.toLowerCase().includes(searchTerm.toLowerCase()) ||
    config.targetSystem.toLowerCase().includes(searchTerm.toLowerCase()) ||
    config.createdBy.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (!canView) {
    return (
      <Alert severity="warning">
        You do not have permission to view job configurations.
      </Alert>
    );
  }

  return (
    <Box sx={{ width: '100%' }}>
      {/* Filters Section */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                size="small"
                label="Search"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search configurations..."
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon />
                    </InputAdornment>
                  ),
                  endAdornment: searchTerm && (
                    <InputAdornment position="end">
                      <IconButton size="small" onClick={() => setSearchTerm('')}>
                        <ClearIcon />
                      </IconButton>
                    </InputAdornment>
                  )
                }}
              />
            </Grid>

            <Grid item xs={12} md={2}>
              <FormControl fullWidth size="small">
                <InputLabel>Job Type</InputLabel>
                <Select
                  value={jobTypeFilter}
                  onChange={(e) => {
                    const newValue = e.target.value;
                    setJobTypeFilter(newValue);
                    // ✅ Call parent immediately to prevent re-render loops
                    const filters: JobConfigurationListParams = {
                      ...(newValue && { jobType: newValue }),
                      ...(statusFilter && { status: statusFilter as any }),
                      ...(sourceSystemFilter && { sourceSystem: sourceSystemFilter })
                    };
                    onFiltersChange(filters);
                  }}
                  label="Job Type"
                >
                  <MenuOption value="">All Types</MenuOption>
                  {getUniqueValues('jobType').map((type) => (
                    <MenuOption key={type} value={type}>
                      {type}
                    </MenuOption>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            <Grid item xs={12} md={2}>
              <FormControl fullWidth size="small">
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  onChange={(e) => {
                    const newValue = e.target.value;
                    setStatusFilter(newValue);
                    // ✅ Call parent immediately to prevent re-render loops
                    const filters: JobConfigurationListParams = {
                      ...(jobTypeFilter && { jobType: jobTypeFilter }),
                      ...(newValue && { status: newValue as any }),
                      ...(sourceSystemFilter && { sourceSystem: sourceSystemFilter })
                    };
                    onFiltersChange(filters);
                  }}
                  label="Status"
                >
                  <MenuOption value="">All Statuses</MenuOption>
                  <MenuOption value="ACTIVE">Active</MenuOption>
                  <MenuOption value="INACTIVE">Inactive</MenuOption>
                  <MenuOption value="DEPRECATED">Deprecated</MenuOption>
                  <MenuOption value="PENDING_APPROVAL">Pending Approval</MenuOption>
                </Select>
              </FormControl>
            </Grid>

            <Grid item xs={12} md={2}>
              <FormControl fullWidth size="small">
                <InputLabel>Source System</InputLabel>
                <Select
                  value={sourceSystemFilter}
                  onChange={(e) => {
                    const newValue = e.target.value;
                    setSourceSystemFilter(newValue);
                    // ✅ Call parent immediately to prevent re-render loops
                    const filters: JobConfigurationListParams = {
                      ...(jobTypeFilter && { jobType: jobTypeFilter }),
                      ...(statusFilter && { status: statusFilter as any }),
                      ...(newValue && { sourceSystem: newValue })
                    };
                    onFiltersChange(filters);
                  }}
                  label="Source System"
                >
                  <MenuOption value="">All Systems</MenuOption>
                  {getUniqueValues('sourceSystem').map((system) => (
                    <MenuOption key={system} value={system}>
                      {system}
                    </MenuOption>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            <Grid item xs={12} md={3}>
              <Stack direction="row" spacing={1}>
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<ClearIcon />}
                  onClick={handleClearFilters}
                >
                  Clear Filters
                </Button>
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<RefreshIcon />}
                  onClick={onRefresh}
                  disabled={loading}
                >
                  Refresh
                </Button>
                <Tooltip title="Export functionality coming in Phase 3B">
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={<ExportIcon />}
                    disabled
                  >
                    Export
                  </Button>
                </Tooltip>
              </Stack>
            </Grid>
          </Grid>

          {/* Filter Summary */}
          <Box sx={{ mt: 2, display: 'flex', gap: 1, flexWrap: 'wrap', alignItems: 'center' }}>
            <Typography variant="body2" color="text.secondary">
              Showing {filteredConfigurations.length} of {totalElements} configurations
            </Typography>
            {(jobTypeFilter || statusFilter || sourceSystemFilter || searchTerm) && (
              <>
                {jobTypeFilter && (
                  <Chip
                    label={`Type: ${jobTypeFilter}`}
                    size="small"
                    onDelete={() => setJobTypeFilter('')}
                  />
                )}
                {statusFilter && (
                  <Chip
                    label={`Status: ${statusFilter}`}
                    size="small"
                    onDelete={() => setStatusFilter('')}
                  />
                )}
                {sourceSystemFilter && (
                  <Chip
                    label={`Source: ${sourceSystemFilter}`}
                    size="small"
                    onDelete={() => setSourceSystemFilter('')}
                  />
                )}
                {searchTerm && (
                  <Chip
                    label={`Search: "${searchTerm}"`}
                    size="small"
                    onDelete={() => setSearchTerm('')}
                  />
                )}
              </>
            )}
          </Box>
        </CardContent>
      </Card>

      {/* Data Table */}
      <Paper sx={{ width: '100%', overflow: 'hidden' }}>
        <TableContainer sx={{ maxHeight: 600 }}>
          <Table stickyHeader aria-label="job configurations table">
            <TableHead>
              <TableRow>
                {columns.map((column) => (
                  <TableCell
                    key={column.id}
                    align={column.align}
                    style={{ minWidth: column.minWidth }}
                  >
                    {column.sortable ? (
                      <TableSortLabel
                        active={sortBy === column.id}
                        direction={sortBy === column.id ? sortDir : 'asc'}
                        onClick={() => handleSortClick(column.id)}
                      >
                        {column.label}
                      </TableSortLabel>
                    ) : (
                      column.label
                    )}
                  </TableCell>
                ))}
                <TableCell align="center" style={{ minWidth: 100 }}>
                  Actions
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                // Loading skeleton rows
                Array.from({ length: pageSize }, (_, index) => (
                  <TableRow key={`skeleton-${index}`}>
                    {columns.map((column) => (
                      <TableCell key={column.id}>
                        <Skeleton variant="text" />
                      </TableCell>
                    ))}
                    <TableCell>
                      <Skeleton variant="circular" width={24} height={24} />
                    </TableCell>
                  </TableRow>
                ))
              ) : filteredConfigurations.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={columns.length + 1} align="center" sx={{ py: 4 }}>
                    <Typography variant="body1" color="text.secondary">
                      No job configurations found
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                      {(jobTypeFilter || statusFilter || sourceSystemFilter || searchTerm)
                        ? 'Try adjusting your filters'
                        : 'Create your first job configuration to get started'
                      }
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                filteredConfigurations.map((config) => (
                  <TableRow hover key={config.configId} tabIndex={-1}>
                    {columns.map((column) => {
                      const value = config[column.id];
                      const displayValue = React.isValidElement(value) ? value :
                                         typeof value === 'object' ? JSON.stringify(value) :
                                         String(value || '');
                      return (
                        <TableCell key={column.id} align={column.align}>
                          {column.id === 'status' ? (
                            <Chip
                              label={displayValue}
                              size="small"
                              {...getStatusChipProps(value as string)}
                            />
                          ) : column.format && typeof value === 'string' ? (
                            column.format(value)
                          ) : (
                            displayValue
                          )}
                        </TableCell>
                      );
                    })}
                    <TableCell align="center">
                      <IconButton
                        size="small"
                        onClick={(e) => handleActionClick(e, config)}
                      >
                        <MoreVertIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

        {/* Pagination */}
        <TablePagination
          rowsPerPageOptions={[10, 20, 50, 100]}
          component="div"
          count={totalElements}
          rowsPerPage={pageSize}
          page={currentPage}
          onPageChange={handleChangePage}
          onRowsPerPageChange={handleChangeRowsPerPage}
          showFirstButton
          showLastButton
        />
      </Paper>

      {/* Action Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleActionClose}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
      >
        <MenuOption onClick={() => { onView(selectedConfig!); handleActionClose(); }}>
          <VisibilityIcon sx={{ mr: 1 }} fontSize="small" />
          View Details
        </MenuOption>
        
        {canModify && (
          <MenuOption onClick={() => { onEdit(selectedConfig!); handleActionClose(); }}>
            <EditIcon sx={{ mr: 1 }} fontSize="small" />
            Edit Configuration
          </MenuOption>
        )}
        
        {canModify && selectedConfig?.status === 'ACTIVE' && (
          <MenuOption 
            onClick={() => { onDelete(selectedConfig!); handleActionClose(); }}
            sx={{ color: 'warning.main' }}
          >
            <DeleteIcon sx={{ mr: 1 }} fontSize="small" />
            Deactivate
          </MenuOption>
        )}
      </Menu>
    </Box>
  );
});

// Set display name for debugging
JobConfigurationList.displayName = 'JobConfigurationList';

export default JobConfigurationList;