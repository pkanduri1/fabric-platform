// src/components/masterQuery/MasterQuerySelector.tsx
/**
 * Master Query Selector Component - Banking Grade Search & Filtering
 * Features: Context API integration, Advanced search, WCAG 2.1 AA compliance
 */

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Autocomplete,
  Chip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Tooltip,
  Badge,
  Collapse,
  Button,
  Alert,
  CircularProgress,
  Grid,
  Paper,
  Divider,
  Switch,
  FormControlLabel,
  Rating,
  Avatar
} from '@mui/material';
import {
  Search,
  FilterList,
  Clear,
  ExpandMore,
  ExpandLess,
  Security,
  Schedule,
  Person,
  Code,
  Assessment,
  Star,
  Visibility,
  Edit,
  Delete,
  PlayArrow,
  GetApp,
  Share,
  BookmarkBorder,
  Bookmark
} from '@mui/icons-material';
import { useMasterQueryContext } from '../../contexts/MasterQueryContext';
import { MasterQuery, MasterQueryFilter } from '../../types/masterQuery';

// Component props interface
interface MasterQuerySelectorProps {
  onQuerySelect?: (query: MasterQuery) => void;
  onQueryExecute?: (query: MasterQuery) => void;
  showActions?: boolean;
  mode?: 'selection' | 'management';
  maxHeight?: number;
  enableVirtualization?: boolean;
  selectedQuery?: MasterQuery | null;
  showBankingIntelligence?: boolean;
}

// Filter chip interface
interface FilterChip {
  key: string;
  label: string;
  color: 'primary' | 'secondary' | 'default';
  icon?: React.ReactElement;
}

export const MasterQuerySelector: React.FC<MasterQuerySelectorProps> = ({
  onQuerySelect,
  onQueryExecute,
  showActions = true,
  mode = 'selection',
  maxHeight = 600,
  enableVirtualization = true,
  selectedQuery: externalSelectedQuery,
  showBankingIntelligence = false
}) => {
  // Context state
  const {
    queries,
    selectedQuery,
    isLoading,
    error,
    filter,
    selectQuery,
    loadQueries,
    updateFilter,
    clearFilter,
    clearError
  } = useMasterQueryContext();
  
  // Local state
  const [searchTerm, setSearchTerm] = useState(filter.searchTerm || '');
  const [expandedFilters, setExpandedFilters] = useState(false);
  const [savedQueries, setSavedQueries] = useState<Set<string>>(new Set());
  const [recentQueries, setRecentQueries] = useState<string[]>([]);
  
  // Security classification options
  const securityClassifications = [
    { value: 'PUBLIC', label: 'Public', color: '#4caf50' },
    { value: 'INTERNAL', label: 'Internal', color: '#ff9800' },
    { value: 'CONFIDENTIAL', label: 'Confidential', color: '#f44336' },
    { value: 'RESTRICTED', label: 'Restricted', color: '#9c27b0' }
  ];
  
  // Data classification options
  const dataClassifications = [
    { value: 'PUBLIC', label: 'Public', color: '#4caf50' },
    { value: 'INTERNAL', label: 'Internal', color: '#ff9800' },
    { value: 'SENSITIVE', label: 'Sensitive', color: '#f44336' }
  ];
  
  // Status options
  const statusOptions = [
    { value: 'ACTIVE', label: 'Active', color: '#4caf50' },
    { value: 'INACTIVE', label: 'Inactive', color: '#9e9e9e' },
    { value: 'DEPRECATED', label: 'Deprecated', color: '#f44336' }
  ];
  
  // Sort options
  const sortOptions = [
    { value: 'name', label: 'Name' },
    { value: 'created', label: 'Created Date' },
    { value: 'modified', label: 'Modified Date' },
    { value: 'usage', label: 'Usage Count' }
  ];
  
  // Load queries on mount and filter changes
  useEffect(() => {
    loadQueries(filter);
  }, [loadQueries, filter]);
  
  // Load recent queries from localStorage
  useEffect(() => {
    const saved = localStorage.getItem('fabric-recent-queries');
    if (saved) {
      try {
        setRecentQueries(JSON.parse(saved));
      } catch (e) {
        console.warn('Failed to parse recent queries from localStorage');
      }
    }
    
    const bookmarked = localStorage.getItem('fabric-saved-queries');
    if (bookmarked) {
      try {
        setSavedQueries(new Set(JSON.parse(bookmarked)));
      } catch (e) {
        console.warn('Failed to parse saved queries from localStorage');
      }
    }
  }, []);
  
  // Handle search with debouncing
  const handleSearchChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    setSearchTerm(value);
    
    // Debounce search
    const timeoutId = setTimeout(() => {
      updateFilter({ searchTerm: value || undefined });
    }, 300);
    
    return () => clearTimeout(timeoutId);
  }, [updateFilter]);
  
  // Handle filter changes
  const handleFilterChange = useCallback((key: keyof MasterQueryFilter, value: any) => {
    updateFilter({ [key]: value });
  }, [updateFilter]);
  
  // Handle query selection
  const handleQuerySelect = useCallback((query: MasterQuery) => {
    selectQuery(query.masterQueryId);
    
    // Add to recent queries
    const newRecent = [
      query.masterQueryId,
      ...recentQueries.filter(id => id !== query.masterQueryId)
    ].slice(0, 10);
    
    setRecentQueries(newRecent);
    localStorage.setItem('fabric-recent-queries', JSON.stringify(newRecent));
    
    // Call external handler
    if (onQuerySelect) {
      onQuerySelect(query);
    }
  }, [selectQuery, recentQueries, onQuerySelect]);
  
  // Handle query execution
  const handleQueryExecute = useCallback((query: MasterQuery) => {
    if (onQueryExecute) {
      onQueryExecute(query);
    }
  }, [onQueryExecute]);
  
  // Toggle saved query
  const toggleSavedQuery = useCallback((queryId: string) => {
    const newSaved = new Set(savedQueries);
    if (newSaved.has(queryId)) {
      newSaved.delete(queryId);
    } else {
      newSaved.add(queryId);
    }
    
    setSavedQueries(newSaved);
    localStorage.setItem('fabric-saved-queries', JSON.stringify(Array.from(newSaved)));
  }, [savedQueries]);
  
  // Clear all filters
  const handleClearFilters = useCallback(() => {
    setSearchTerm('');
    clearFilter();
  }, [clearFilter]);
  
  // Filter and sort queries
  const filteredQueries = useMemo(() => {
    let result = [...queries];
    
    // Apply search filter
    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      result = result.filter(query =>
        query.queryName.toLowerCase().includes(search) ||
        query.queryDescription?.toLowerCase().includes(search) ||
        query.querySql.toLowerCase().includes(search) ||
        query.complianceTags?.some((tag: string) => tag.toLowerCase().includes(search))
      );
    }
    
    // Apply sorting
    result.sort((a, b) => {
      const sortBy = filter.sortBy || 'modified';
      const sortOrder = filter.sortOrder || 'desc';
      
      let compareValue = 0;
      
      switch (sortBy) {
        case 'name':
          compareValue = a.queryName.localeCompare(b.queryName);
          break;
        case 'created':
          compareValue = (a.createdAt?.getTime() || 0) - (b.createdAt?.getTime() || 0);
          break;
        case 'modified':
          compareValue = (a.lastModifiedAt?.getTime() || 0) - (b.lastModifiedAt?.getTime() || 0);
          break;
        case 'usage':
          // TODO: Implement usage tracking
          compareValue = 0;
          break;
      }
      
      return sortOrder === 'desc' ? -compareValue : compareValue;
    });
    
    return result;
  }, [queries, searchTerm, filter.sortBy, filter.sortOrder]);
  
  // Generate active filter chips
  const activeFilterChips = useMemo((): FilterChip[] => {
    const chips: FilterChip[] = [];
    
    if (filter.securityClassification?.length) {
      chips.push({
        key: 'security',
        label: `Security: ${filter.securityClassification.length}`,
        color: 'primary',
        icon: <Security fontSize="small" />
      });
    }
    
    if (filter.dataClassification?.length) {
      chips.push({
        key: 'data',
        label: `Data: ${filter.dataClassification.length}`,
        color: 'secondary',
        icon: <Assessment fontSize="small" />
      });
    }
    
    if (filter.status?.length) {
      chips.push({
        key: 'status',
        label: `Status: ${filter.status.length}`,
        color: 'default'
      });
    }
    
    if (filter.createdBy) {
      chips.push({
        key: 'creator',
        label: `Creator: ${filter.createdBy}`,
        color: 'default',
        icon: <Person fontSize="small" />
      });
    }
    
    if (filter.dateRange) {
      chips.push({
        key: 'date',
        label: 'Date Range',
        color: 'default',
        icon: <Schedule fontSize="small" />
      });
    }
    
    return chips;
  }, [filter]);
  
  // Render query item
  const renderQueryItem = useCallback((query: MasterQuery) => {
    const currentSelectedQuery = externalSelectedQuery || selectedQuery;
    const isSelected = currentSelectedQuery?.masterQueryId === query.masterQueryId;
    const isSaved = savedQueries.has(query.masterQueryId);
    const isRecent = recentQueries.includes(query.masterQueryId);
    
    const securityColor = securityClassifications.find(
      s => s.value === query.securityClassification
    )?.color || '#9e9e9e';
    
    const dataColor = dataClassifications.find(
      d => d.value === query.dataClassification
    )?.color || '#9e9e9e';
    
    return (
      <ListItem
        key={query.masterQueryId}
        component="button"
        selected={isSelected}
        onClick={() => handleQuerySelect(query)}
        sx={{
          mb: 1,
          borderRadius: 1,
          border: isSelected ? 2 : 1,
          borderColor: isSelected ? 'primary.main' : 'divider',
          backgroundColor: isSelected ? 'primary.lighter' : 'background.paper',
          '&:hover': {
            backgroundColor: isSelected ? 'primary.lighter' : 'action.hover'
          }
        }}
        aria-label={`Select query: ${query.queryName}`}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', mr: 1 }}>
          <Avatar
            sx={{
              width: 32,
              height: 32,
              bgcolor: securityColor,
              fontSize: '0.75rem'
            }}
          >
            {query.queryName.charAt(0).toUpperCase()}
          </Avatar>
          {isRecent && (
            <Chip
              size="small"
              label="Recent"
              color="info"
              sx={{ ml: 1, height: 20, fontSize: '0.6rem' }}
            />
          )}
        </Box>
        
        <ListItemText
          primary={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <span style={{ fontWeight: 600, fontSize: '0.875rem' }}>
                {query.queryName}
              </span>
              {query.complianceTags?.map(tag => (
                <Chip
                  key={tag}
                  size="small"
                  label={tag}
                  variant="outlined"
                  sx={{ height: 18, fontSize: '0.6rem' }}
                />
              ))}
            </Box>
          }
          secondary={
            <Box>
              <div style={{ fontSize: '0.875rem', color: 'rgba(0, 0, 0, 0.6)', marginBottom: '4px' }}>
                {query.queryDescription || 'No description available'}
              </div>
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                <Chip
                  size="small"
                  label={query.securityClassification}
                  sx={{
                    bgcolor: securityColor,
                    color: 'white',
                    height: 18,
                    fontSize: '0.6rem'
                  }}
                />
                <Chip
                  size="small"
                  label={query.dataClassification}
                  sx={{
                    bgcolor: dataColor,
                    color: 'white',
                    height: 18,
                    fontSize: '0.6rem'
                  }}
                />
                <span style={{ fontSize: '0.75rem', color: 'rgba(0, 0, 0, 0.6)' }}>
                  Modified: {query.lastModifiedAt?.toLocaleDateString() || 'Unknown'}
                </span>
              </Box>
            </Box>
          }
        />
        
        {showActions && (
          <ListItemSecondaryAction>
            <Box sx={{ display: 'flex', gap: 0.5 }}>
              <Tooltip title={isSaved ? 'Remove bookmark' : 'Bookmark query'}>
                <IconButton
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleSavedQuery(query.masterQueryId);
                  }}
                  aria-label={isSaved ? 'Remove bookmark' : 'Bookmark query'}
                >
                  {isSaved ? <Bookmark color="primary" /> : <BookmarkBorder />}
                </IconButton>
              </Tooltip>
              
              <Tooltip title="Execute query">
                <IconButton
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleQueryExecute(query);
                  }}
                  color="primary"
                  aria-label="Execute query"
                >
                  <PlayArrow />
                </IconButton>
              </Tooltip>
              
              {mode === 'management' && (
                <>
                  <Tooltip title="Edit query">
                    <IconButton
                      size="small"
                      onClick={(e) => e.stopPropagation()}
                      aria-label="Edit query"
                    >
                      <Edit />
                    </IconButton>
                  </Tooltip>
                  
                  <Tooltip title="Export query">
                    <IconButton
                      size="small"
                      onClick={(e) => e.stopPropagation()}
                      aria-label="Export query"
                    >
                      <GetApp />
                    </IconButton>
                  </Tooltip>
                </>
              )}
            </Box>
          </ListItemSecondaryAction>
        )}
      </ListItem>
    );
  }, [
    selectedQuery,
    externalSelectedQuery,
    savedQueries,
    recentQueries,
    showActions,
    mode,
    handleQuerySelect,
    handleQueryExecute,
    toggleSavedQuery,
    securityClassifications,
    dataClassifications
  ]);
  
  return (
    <Card sx={{ height: maxHeight, display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ pb: 1 }}>
        {/* Header */}
        <Typography variant="h6" component="h2" gutterBottom>
          Master Query Library
          <Badge badgeContent={filteredQueries.length} color="primary" sx={{ ml: 2 }} />
        </Typography>
        
        {/* Search Bar */}
        <TextField
          fullWidth
          placeholder="Search queries by name, description, SQL, or compliance tags..."
          value={searchTerm}
          onChange={handleSearchChange}
          InputProps={{
            startAdornment: <Search sx={{ color: 'action.active', mr: 1 }} />,
            endAdornment: searchTerm && (
              <IconButton
                size="small"
                onClick={() => {
                  setSearchTerm('');
                  updateFilter({ searchTerm: undefined });
                }}
                aria-label="Clear search"
              >
                <Clear />
              </IconButton>
            )
          }}
          sx={{ mb: 2 }}
          aria-label="Search master queries"
        />
        
        {/* Filter Controls */}
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
          <Button
            startIcon={<FilterList />}
            endIcon={expandedFilters ? <ExpandLess /> : <ExpandMore />}
            onClick={() => setExpandedFilters(!expandedFilters)}
            variant="outlined"
            size="small"
            aria-label="Toggle filter panel"
            aria-expanded={expandedFilters}
          >
            Filters
            {activeFilterChips.length > 0 && (
              <Badge badgeContent={activeFilterChips.length} color="error" sx={{ ml: 1 }} />
            )}
          </Button>
          
          <Box sx={{ display: 'flex', gap: 1 }}>
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Sort By</InputLabel>
              <Select
                value={filter.sortBy || 'modified'}
                onChange={(e) => handleFilterChange('sortBy', e.target.value)}
                label="Sort By"
              >
                {sortOptions.map(option => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            
            <FormControl size="small" sx={{ minWidth: 100 }}>
              <InputLabel>Order</InputLabel>
              <Select
                value={filter.sortOrder || 'desc'}
                onChange={(e) => handleFilterChange('sortOrder', e.target.value)}
                label="Order"
              >
                <MenuItem value="asc">Ascending</MenuItem>
                <MenuItem value="desc">Descending</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </Box>
        
        {/* Active Filter Chips */}
        {activeFilterChips.length > 0 && (
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 1 }}>
            {activeFilterChips.map(chip => (
              <Chip
                key={chip.key}
                label={chip.label}
                color={chip.color}
                size="small"
                {...(chip.icon && { icon: chip.icon })}
                onDelete={() => {
                  // Clear specific filter
                  const updates: Partial<MasterQueryFilter> = {};
                  switch (chip.key) {
                    case 'security':
                      updates.securityClassification = undefined;
                      break;
                    case 'data':
                      updates.dataClassification = undefined;
                      break;
                    case 'status':
                      updates.status = undefined;
                      break;
                    case 'creator':
                      updates.createdBy = undefined;
                      break;
                    case 'date':
                      updates.dateRange = undefined;
                      break;
                  }
                  updateFilter(updates);
                }}
              />
            ))}
            <Button
              size="small"
              startIcon={<Clear />}
              onClick={handleClearFilters}
              color="inherit"
            >
              Clear All
            </Button>
          </Box>
        )}
        
        {/* Advanced Filters Panel */}
        <Collapse in={expandedFilters}>
          <Paper sx={{ p: 2, mt: 1, bgcolor: 'background.default' }}>
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <Autocomplete
                  multiple
                  options={securityClassifications.map(s => s.value)}
                  value={filter.securityClassification || []}
                  onChange={(_, value) => handleFilterChange('securityClassification', value)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Security Classification"
                      size="small"
                    />
                  )}
                  renderTags={(value, getTagProps) =>
                    value.map((option, index) => {
                      const classification = securityClassifications.find(s => s.value === option);
                      return (
                        <Chip
                          variant="outlined"
                          label={classification?.label || option}
                          size="small"
                          {...getTagProps({ index })}
                          key={option}
                        />
                      );
                    })
                  }
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Autocomplete
                  multiple
                  options={dataClassifications.map(d => d.value)}
                  value={filter.dataClassification || []}
                  onChange={(_, value) => handleFilterChange('dataClassification', value)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Data Classification"
                      size="small"
                    />
                  )}
                  renderTags={(value, getTagProps) =>
                    value.map((option, index) => {
                      const classification = dataClassifications.find(d => d.value === option);
                      return (
                        <Chip
                          variant="outlined"
                          label={classification?.label || option}
                          size="small"
                          {...getTagProps({ index })}
                          key={option}
                        />
                      );
                    })
                  }
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Autocomplete
                  multiple
                  options={statusOptions.map(s => s.value)}
                  value={filter.status || []}
                  onChange={(_, value) => handleFilterChange('status', value)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Status"
                      size="small"
                    />
                  )}
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Created By"
                  value={filter.createdBy || ''}
                  onChange={(e) => handleFilterChange('createdBy', e.target.value || undefined)}
                  size="small"
                />
              </Grid>
            </Grid>
          </Paper>
        </Collapse>
        
        <Divider sx={{ mt: 1 }} />
      </CardContent>
      
      {/* Query List */}
      <Box sx={{ flex: 1, overflow: 'auto', px: 2 }}>
        {error && (
          <Alert
            severity="error"
            onClose={clearError}
            sx={{ mb: 2 }}
          >
            {error}
          </Alert>
        )}
        
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        ) : filteredQueries.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Typography variant="body1" color="text.secondary" gutterBottom>
              No queries found
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Try adjusting your search criteria or filters
            </Typography>
          </Box>
        ) : (
          <List sx={{ py: 0 }}>
            {filteredQueries.map(renderQueryItem)}
          </List>
        )}
      </Box>
    </Card>
  );
};

export default MasterQuerySelector;