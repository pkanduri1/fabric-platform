// src/components/configuration/MappingArea/MappingArea.tsx
import React, { useState } from 'react';
import {
  Box,
  Typography,
  IconButton,
  List,
  ListItem,
  Paper,
  Chip,
  Menu,
  MenuItem,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Grid,
  FormControl,
  InputLabel,
  Select
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  MoreVert,
  DragIndicator
} from '@mui/icons-material';
import { Droppable, Draggable, DropResult } from 'react-beautiful-dnd';
import { FieldMapping } from '../../../types/configuration';
import { useConfigurationContext, useSourceSystemsState } from '../../../contexts/ConfigurationContext';

interface MappingAreaProps {
  onMappingSelect?: (mapping: FieldMapping) => void;
}

export const MappingArea: React.FC<MappingAreaProps> = ({ onMappingSelect }) => {
  const { 
    fieldMappings = [], 
    updateFieldMapping, 
    deleteFieldMapping, 
    reorderFieldMappings,
    addFieldMapping 
  } = useConfigurationContext();

  const { sourceFields } = useSourceSystemsState();
  
  const [selectedMapping, setSelectedMapping] = useState<FieldMapping | null>(null);
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [newMappingData, setNewMappingData] = useState({
    fieldName: '',
    targetField: '',
    sourceField: '',
    length: 10,
    dataType: 'string' as const,
    transformationType: 'source' as const
  });

  const handleDragEnd = (result: DropResult) => {
    if (!result.destination) return;

    const { source, destination } = result;

    // Handle reordering within mapping area
    if (source.droppableId === 'mapping-area' && destination.droppableId === 'mapping-area') {
      reorderFieldMappings(source.index, destination.index);
    }
  };

  const handleEditMapping = (mapping: FieldMapping) => {
    setSelectedMapping(mapping);
    onMappingSelect?.(mapping);
    console.log('Edit mapping:', mapping);
  };

  const handleDeleteMapping = (id: string) => {
    const mapping = fieldMappings.find(m => m.id === id);
    if (mapping) {
      deleteFieldMapping(mapping.fieldName, mapping.transactionType || 'default');
    }
  };

  const handleCreateMapping = () => {
    setShowAddDialog(true);
  };

  const handleAddDialogClose = () => {
    setShowAddDialog(false);
    setNewMappingData({
      fieldName: '',
      targetField: '',
      sourceField: '',
      length: 10,
      dataType: 'string',
      transformationType: 'source'
    });
  };

  const handleAddMapping = () => {
    if (!newMappingData.fieldName || !newMappingData.targetField) {
      return; // Basic validation
    }

    const newMapping: Omit<FieldMapping, 'id'> = {
        fieldName: newMappingData.fieldName,
        targetField: newMappingData.targetField,
        sourceField: newMappingData.sourceField || undefined,
        targetPosition: fieldMappings.length + 1,
        length: newMappingData.length,
        dataType: newMappingData.dataType,
        transformationType: newMappingData.transformationType,
        transactionType: 'default',
        required: false,
        expression: undefined
    };

    console.log('Adding new mapping:', newMapping);
    addFieldMapping(newMapping);
    handleAddDialogClose();
  };

  const getTransformationIcon = (type: string) => {
    switch (type) {
      case 'source': return '📥';
      case 'constant': return '📌';
      case 'composite': return '🔗';
      case 'conditional': return '🔀';
      default: return '❓';
    }
  };

  const getTransformationTypeColor = (type: string) => {
    switch (type) {
      case 'source': return 'primary';
      case 'constant': return 'success';
      case 'composite': return 'warning';
      case 'conditional': return 'secondary';
      default: return 'default';
    }
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h6">
            Field Mappings ({fieldMappings.length})
          </Typography>
          <Tooltip title="Add new mapping">
            <IconButton onClick={handleCreateMapping} color="primary">
              <Add />
            </IconButton>
          </Tooltip>
        </Box>
        
        {fieldMappings.length > 0 && (
          <Typography variant="body2" color="text.secondary">
            Total record length: {fieldMappings.reduce((sum, m) => sum + m.length, 0)} characters
          </Typography>
        )}
      </Box>

      {/* Mappings List */}
      <Box sx={{ flex: 1, overflow: 'auto', p: 1 }}>
        {fieldMappings.length === 0 ? (
          <Box sx={{ 
            height: '200px',
            border: 2,
            borderStyle: 'dashed',
            borderColor: 'divider',
            borderRadius: 2,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: 'action.hover'
          }}>
            <Typography variant="h6" color="text.secondary" gutterBottom>
              No Field Mappings
            </Typography>
            <Typography variant="body2" color="text.secondary" textAlign="center">
              Drag source fields here to create mappings
              <br />
              or use the + button to add manually
            </Typography>
          </Box>
        ) : (
          <Droppable droppableId="mapping-area">
            {(provided, snapshot) => (
              <List
                {...provided.droppableProps}
                ref={provided.innerRef}
                sx={{ 
                  p: 0,
                  backgroundColor: snapshot.isDraggingOver ? 'action.hover' : 'transparent',
                  borderRadius: 1,
                  transition: 'background-color 0.2s ease',
                  minHeight: '100px'
                }}
              >
                {fieldMappings
                  .filter(mapping => mapping.id) // Filter out mappings without IDs
                  .sort((a, b) => a.targetPosition - b.targetPosition)
                  .map((mapping, index) => (
                  <Draggable 
                    key={mapping.id} 
                    draggableId={mapping.id!} 
                    index={index}
                  >
                    {(provided, snapshot) => (
                      <ListItem
                        ref={provided.innerRef}
                        {...provided.draggableProps}
                        {...provided.dragHandleProps}
                        sx={{ p: 0, mb: 1 }}
                      >
                        <MappingItem
                          mapping={mapping}
                          index={index}
                          onEdit={() => handleEditMapping(mapping)}
                          onDelete={() => handleDeleteMapping(mapping.id!)}
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

      {/* Add Mapping Dialog */}
      <Dialog open={showAddDialog} onClose={handleAddDialogClose} maxWidth="md" fullWidth>
        <DialogTitle>Add New Field Mapping</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Field Name"
                value={newMappingData.fieldName}
                onChange={(e) => setNewMappingData(prev => ({ ...prev, fieldName: e.target.value }))}
                required
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Target Field"
                value={newMappingData.targetField}
                onChange={(e) => setNewMappingData(prev => ({ ...prev, targetField: e.target.value }))}
                required
              />
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Source Field</InputLabel>
                <Select
                  value={newMappingData.sourceField}
                  onChange={(e) => setNewMappingData(prev => ({ ...prev, sourceField: e.target.value }))}
                  label="Source Field"
                >
                  <MenuItem value="">None (Constant/Composite)</MenuItem>
                  {sourceFields.map(field => (
                    <MenuItem key={field.name} value={field.name}>
                      {field.name} ({field.dataType})
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Length"
                type="number"
                value={newMappingData.length}
                onChange={(e) => setNewMappingData(prev => ({ ...prev, length: parseInt(e.target.value) || 10 }))}
              />
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Data Type</InputLabel>
                <Select
                  value={newMappingData.dataType}
                  onChange={(e) => setNewMappingData(prev => ({ ...prev, dataType: e.target.value as any }))}
                  label="Data Type"
                >
                  <MenuItem value="string">String</MenuItem>
                  <MenuItem value="numeric">Numeric</MenuItem>
                  <MenuItem value="date">Date</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Transformation Type</InputLabel>
                <Select
                  value={newMappingData.transformationType}
                  onChange={(e) => setNewMappingData(prev => ({ ...prev, transformationType: e.target.value as any }))}
                  label="Transformation Type"
                >
                  <MenuItem value="source">Source Field</MenuItem>
                  <MenuItem value="constant">Constant Value</MenuItem>
                  <MenuItem value="composite">Composite</MenuItem>
                  <MenuItem value="conditional">Conditional</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleAddDialogClose}>Cancel</Button>
          <Button 
            onClick={handleAddMapping} 
            variant="contained"
            disabled={!newMappingData.fieldName || !newMappingData.targetField}
          >
            Add Mapping
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

// MappingItem component for individual field mapping display
const MappingItem: React.FC<{
  mapping: FieldMapping;
  index: number;
  onEdit: () => void;
  onDelete: () => void;
  isDragging: boolean;
}> = ({ mapping, index, onEdit, onDelete, isDragging }) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleMenuClick = (event: React.MouseEvent<HTMLElement>) => {
    event.stopPropagation();
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleEdit = () => {
    onEdit();
    handleMenuClose();
  };

  const handleDelete = () => {
    onDelete();
    handleMenuClose();
  };

  const getTransformationIcon = (type: string) => {
    switch (type) {
      case 'source': return '📥';
      case 'constant': return '📌';
      case 'composite': return '🔗';
      case 'conditional': return '🔀';
      default: return '❓';
    }
  };

  const getTransformationTypeColor = (type: string) => {
    switch (type) {
      case 'source': return 'primary';
      case 'constant': return 'success';
      case 'composite': return 'warning';
      case 'conditional': return 'secondary';
      default: return 'default';
    }
  };

  return (
    <Paper
      sx={{
        p: 2,
        width: '100%',
        cursor: 'pointer',
        backgroundColor: isDragging ? 'action.hover' : 'background.paper',
        '&:hover': {
          backgroundColor: 'action.hover'
        }
      }}
      onClick={onEdit}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
        <DragIndicator sx={{ mr: 1, color: 'text.secondary' }} />
        <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>
          {mapping.fieldName} → {mapping.targetField}
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ mr: 1 }}>
          Pos: {mapping.targetPosition}
        </Typography>
        <IconButton size="small" onClick={handleMenuClick}>
          <MoreVert />
        </IconButton>
      </Box>

      <Box>
        {/* Source/Target Row */}
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
          <Box sx={{ flex: 1 }}>
            <Typography variant="caption" color="text.secondary" display="block">
              Source
            </Typography>
            <Typography variant="body2">
              {mapping.transformationType === 'source' && mapping.sourceField
                ? mapping.sourceField
                : mapping.transformationType === 'constant' && mapping.defaultValue
                ? `"${mapping.defaultValue}"`
                : mapping.transformationType === 'composite' && mapping.sources
                ? `${mapping.sources.map(s => s.field).join(' + ')}`
                : 'Not configured'
              }
            </Typography>
          </Box>
          
          <Box sx={{ textAlign: 'center', px: 1 }}>
            <Typography variant="h6">
              {getTransformationIcon(mapping.transformationType)}
            </Typography>
          </Box>
          
          <Box sx={{ flex: 1 }}>
            <Typography variant="caption" color="text.secondary" display="block">
              Target ({mapping.length} chars)
            </Typography>
            <Typography variant="body2">
              {mapping.targetField}
            </Typography>
          </Box>
        </Box>

        {/* Tags Row */}
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Chip
            label={mapping.transformationType}
            size="small"
            color={getTransformationTypeColor(mapping.transformationType) as any}
            variant="outlined"
          />
          <Chip
            label={mapping.dataType}
            size="small"
            variant="outlined"
            color="default"
          />
          {mapping.pad && (
            <Chip
              label={`Pad ${mapping.pad}`}
              size="small"
              variant="outlined"
              color="info"
            />
          )}
          {mapping.conditions && mapping.conditions.length > 0 && (
            <Chip
              label="Conditional"
              size="small"
              variant="outlined"
              color="warning"
            />
          )}
        </Box>
      </Box>

      {/* Actions Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <MenuItem onClick={handleEdit}>
          <Edit fontSize="small" sx={{ mr: 1 }} />
          Edit Mapping
        </MenuItem>
        <MenuItem onClick={handleDelete} sx={{ color: 'error.main' }}>
          <Delete fontSize="small" sx={{ mr: 1 }} />
          Delete Mapping
        </MenuItem>
      </Menu>
    </Paper>
  );
};