// src/components/configuration/SourceFieldList/SourceFieldList.tsx
import React, { useState, useMemo } from 'react';
import {
  Box,
  TextField,
  InputAdornment,
  Typography,
  Paper,
  Chip,
  IconButton,
  Tooltip,
  List,
  ListItem
} from '@mui/material';
import {
  Search,
  DragIndicator,
  Info,
  FilterList,
  Clear
} from '@mui/icons-material';
import { DragDropContext, Droppable, Draggable, DropResult } from 'react-beautiful-dnd';
import { SourceField } from '../../../types/configuration';

interface SourceFieldListProps {
  sourceFields: SourceField[];
  onFieldDragEnd?: (result: DropResult) => void;
}

interface FieldItemProps {
  field: SourceField;
  index: number;
  isDragging?: boolean;
}

const FieldItem: React.FC<FieldItemProps> = ({ field, index, isDragging }) => {
  const getDataTypeColor = (dataType: string | undefined): string => {
  if (!dataType) {
    return '#gray'; // Default color for undefined dataType
  }
  
  switch (dataType.toLowerCase()) {
    case 'string':
    case 'varchar':
    case 'text':
      return '#1976d2'; // blue
    case 'number':
    case 'integer':
    case 'decimal':
      return '#388e3c'; // green
    case 'date':
    case 'datetime':
      return '#f57c00'; // orange
    case 'boolean':
      return '#7b1fa2'; // purple
    default:
      return '#616161'; // gray
  }
};

  return (
    <Paper
      elevation={isDragging ? 8 : 1}
      sx={{
        p: 2,
        mb: 1,
        cursor: 'grab',
        border: 1,
        borderColor: isDragging ? 'primary.main' : 'divider',
        backgroundColor: isDragging ? 'action.selected' : 'background.paper',
        transform: isDragging ? 'rotate(2deg)' : 'none',
        transition: 'all 0.2s ease',
        '&:hover': {
          backgroundColor: 'action.hover',
          borderColor: 'primary.light'
        },
        '&:active': {
          cursor: 'grabbing'
        }
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
        <DragIndicator 
          sx={{ 
            color: 'text.secondary', 
            mt: 0.5,
            cursor: 'grab'
          }} 
        />
        
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
            <Typography 
              variant="body2" 
              fontWeight="medium"
              sx={{ 
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap'
              }}
            >
              {field.name}
            </Typography>
            
            {field.description && (
              <Tooltip title={field.description} arrow>
                <IconButton size="small" sx={{ p: 0.25 }}>
                  <Info fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
          </Box>
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
            <Chip
  label={field.dataType}
  size="small"
  variant="outlined"
  color="default"
/>
            
            {field.maxLength && (
              <Chip
                label={`Max: ${field.maxLength}`}
                size="small"
                variant="outlined"
                color="default"
              />
            )}
            
            {field.nullable && (
              <Chip
                label="Nullable"
                size="small"
                variant="outlined"
                color="info"
              />
            )}
          </Box>
        </Box>
      </Box>
    </Paper>
  );
};

export const SourceFieldList: React.FC<SourceFieldListProps> = ({ 
  sourceFields, 
  onFieldDragEnd 
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('');

  // Get unique data types for filtering
  const uniqueDataTypes = useMemo(() => {
    const types = new Set(sourceFields.map(field => field.dataType));
    return Array.from(types).sort();
  }, [sourceFields]);

  // Filter fields based on search and type filter
  const filteredFields = useMemo(() => {
    return sourceFields.filter(field => {
      const matchesSearch = !searchTerm || 
        field.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (field.description && field.description.toLowerCase().includes(searchTerm.toLowerCase()));
      
      const matchesType = !typeFilter || field.dataType === typeFilter;
      
      return matchesSearch && matchesType;
    });
  }, [sourceFields, searchTerm, typeFilter]);

  const handleDragEnd = (result: DropResult) => {
    // This is now handled by parent ConfigurationPage
  };

  const clearFilters = () => {
    setSearchTerm('');
    setTypeFilter('');
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Search and Filter Header */}
      <Box sx={{ p: 2, pb: 1 }}>
        <TextField
          fullWidth
          size="small"
          placeholder="Search fields..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <Search fontSize="small" />
              </InputAdornment>
            ),
            endAdornment: searchTerm && (
              <InputAdornment position="end">
                <IconButton size="small" onClick={() => setSearchTerm('')}>
                  <Clear fontSize="small" />
                </IconButton>
              </InputAdornment>
            )
          }}
          sx={{ mb: 1 }}
        />
        
        {/* Type Filter */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
          <FilterList fontSize="small" color="action" />
          <Typography variant="caption" color="text.secondary">
            Type:
          </Typography>
          {uniqueDataTypes.map(type => (
            <Chip
    key={type}
    label={type}
    size="small"
    variant={typeFilter === type ? "filled" : "outlined"}
    color="default"  // Change from dynamic color to "default"
    onClick={() => setTypeFilter(typeFilter === type ? '' : type)}
    sx={{ cursor: 'pointer' }}
  />
          ))}
          {(searchTerm || typeFilter) && (
            <Chip
  label="Clear"
  size="small"
  variant="outlined"
  color="default"  // Change from "secondary" to "default"
  onClick={clearFilters}
  sx={{ cursor: 'pointer' }}
/>
          )}
        </Box>
      </Box>

      {/* Results Summary */}
      <Box sx={{ px: 2, pb: 1 }}>
        <Typography variant="caption" color="text.secondary">
          {filteredFields.length} of {sourceFields.length} fields
          {(searchTerm || typeFilter) && ' (filtered)'}
        </Typography>
      </Box>

      {/* Draggable Fields List */}
      <Box sx={{ flex: 1, overflow: 'auto', px: 1 }}>
        {filteredFields.length === 0 ? (
          <Box sx={{ p: 2, textAlign: 'center' }}>
            <Typography variant="body2" color="text.secondary">
              {sourceFields.length === 0 
                ? 'No source fields available'
                : 'No fields match the current filters'
              }
            </Typography>
          </Box>
        ) : (
          <Droppable droppableId="source-fields">
            {(provided, snapshot) => (
              <List
                {...provided.droppableProps}
                ref={provided.innerRef}
                sx={{ 
                  p: 0,
                  backgroundColor: snapshot.isDraggingOver ? 'action.hover' : 'transparent',
                  borderRadius: 1,
                  transition: 'background-color 0.2s ease'
                }}
              >
                {filteredFields.map((field, index) => (
                  <Draggable 
                    key={field.name} 
                    draggableId={field.name} 
                    index={index}
                  >
                    {(provided, snapshot) => (
                      <ListItem
                        ref={provided.innerRef}
                        {...provided.draggableProps}
                        {...provided.dragHandleProps}
                        sx={{ p: 0, mb: 1 }}
                      >
                        <FieldItem
                          field={field}
                          index={index}
                          isDragging={snapshot.isDragging}
                        />
                      </ListItem>
                    )}
                  </Draggable>
                ))}
                {provided.placeholder}
              </List>
            )}
          </Droppable>
        )}
      </Box>
    </Box>
  );
};