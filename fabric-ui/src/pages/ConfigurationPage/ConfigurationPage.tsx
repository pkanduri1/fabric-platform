// src/pages/ConfigurationPage/ConfigurationPage.tsx
import React, { useEffect } from 'react';
import { 
  Box, 
  Typography, 
  Alert, 
  Paper, 
  Divider,
  Chip,
  CircularProgress 
} from '@mui/material';
import { useParams } from 'react-router-dom';
import { useConfigurationContext } from '../../contexts/ConfigurationContext';
import { SourceFieldList } from '../../components/configuration/SourceFieldList/SourceFieldList';
import { MappingArea } from '../../components/configuration/MappingArea/MappingArea';
import { FieldConfig } from '../../components/configuration/FieldConfig/FieldConfig';
import { DragDropContext, DropResult } from 'react-beautiful-dnd';
import { FieldMapping } from '../../types/configuration';

const ConfigurationPage: React.FC = () => {
  const { systemId, jobName } = useParams();
  const { 
  selectedSourceSystem, 
  selectedJob, 
  selectSourceSystem, 
  selectJob,
  sourceSystems,
  sourceFields,
  isLoading, 
  error, 
  addFieldMapping,
  fieldMappings,
  reorderFieldMappings 
} = useConfigurationContext();
  
//   const { 
//     isLoading, 
//     error, 
//     addFieldMapping,
//     fieldMappings,
//     reorderFieldMappings 
//   } = useConfigurationContext();
  
  const [selectedMapping, setSelectedMapping] = React.useState<FieldMapping | null>(null);

  // Load system and job on mount
  useEffect(() => {
    if (systemId && !selectedSourceSystem) {
      const system = sourceSystems.find(s => s.id === systemId);
      if (system) {
        selectSourceSystem(system.id);
      }
    }
  }, [systemId, selectedSourceSystem, sourceSystems, selectSourceSystem]);

  useEffect(() => {
  if (jobName && selectedSourceSystem && !selectedJob) {
    // Since jobs array is empty, just set the job name directly
    selectJob(jobName);
  }
}, [jobName, selectedSourceSystem, selectedJob, selectJob]);

  // Handle all drag and drop operations
  const handleDragEnd = (result: DropResult) => {
    if (!result.destination) {
      console.log('No destination, drag cancelled');
      return;
    }
    
    const { source, destination } = result;
    console.log('Drag ended:', { source, destination });
    
    // Handle reordering within mapping area
    if (source.droppableId === 'mapping-area' && destination.droppableId === 'mapping-area') {
      console.log('Reordering mappings:', source.index, '→', destination.index);
      reorderFieldMappings(source.index, destination.index);
      return;
    }
    
    // Handle dropping source field into mapping area (create new mapping)
    if (source.droppableId === 'source-fields' && destination.droppableId === 'mapping-area') {
      const draggedFieldName = result.draggableId;
      const sourceField = sourceFields.find(field => field.name === draggedFieldName);
      
      if (sourceField) {
        console.log('Creating new mapping for field:', sourceField.name);
        
        // Create a new field mapping
        const newMapping: Omit<FieldMapping, 'id'> = {
            fieldName: sourceField.name,
            sourceField: sourceField.name,
            targetField: sourceField.name,
            targetPosition: fieldMappings.length + 1,
            length: sourceField.maxLength || 10,
            dataType: sourceField.dataType,
            transformationType: 'source',
            transactionType: 'default',
            required: false,
            expression: undefined   
        };
        
        addFieldMapping(newMapping);
        
        // Select the new mapping for editing
        setTimeout(() => {
          const newMappingWithId = fieldMappings.find(m => m.sourceField === sourceField.name);
          if (newMappingWithId) {
            setSelectedMapping(newMappingWithId);
          }
        }, 100);
      }
    }
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 2 }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  if (!selectedSourceSystem || !selectedJob) {
    return (
      <Box sx={{ p: 2 }}>
        <Alert severity="info">
          Select a source system and job to begin configuration
        </Alert>
      </Box>
    );
  }

  return (
    <DragDropContext onDragEnd={handleDragEnd}>
      <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        {/* Header */}
        <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
          <Typography variant="h5" gutterBottom>
            {/* Configuration: {selectedSourceSystem.name} - {selectedJob.jobName} */}
          </Typography>
          
<Box sx={{ display: 'flex', gap: 1 }}>
  <span>{selectedSourceSystem.type || selectedSourceSystem.systemType || 'Unknown'}</span>
  <span>{sourceFields.length} source fields</span>
  <span>{fieldMappings.length} mappings</span>
</Box>
        </Box>

        {/* 3-Panel Layout */}
        <Box sx={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
          {/* Left Panel - Source Fields */}
          <Paper sx={{ width: '300px', display: 'flex', flexDirection: 'column' }}>
            <SourceFieldList sourceFields={sourceFields} />
          </Paper>

          <Divider orientation="vertical" flexItem />

          {/* Center Panel - Field Mappings */}
          <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
            <MappingArea onMappingSelect={setSelectedMapping} />
          </Box>

          <Divider orientation="vertical" flexItem />

          {/* Right Panel - Field Configuration */}
          <Paper sx={{ width: '400px', display: 'flex', flexDirection: 'column' }}>
            <FieldConfig 
              selectedMapping={selectedMapping}
              onClose={() => setSelectedMapping(null)}
              onSave={(mapping) => {
                console.log('Saving mapping:', mapping);
                // The save will be handled by the FieldConfig component
                setSelectedMapping(null);
              }}
            />
          </Paper>
        </Box>
      </Box>
    </DragDropContext>
  );
};

export default ConfigurationPage;