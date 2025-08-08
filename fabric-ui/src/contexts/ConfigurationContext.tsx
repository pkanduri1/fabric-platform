// src/contexts/ConfigurationContext.tsx
import React, { createContext, useContext,useEffect, ReactNode, useState, useCallback } from 'react';
import { 
  Configuration, 
  FieldMapping, 
  SourceSystem, 
  SourceField, 
  ValidationResult,
  TransactionType,
  JobConfig 
} from '../types/configuration';
import { useConfiguration } from '../hooks/useConfiguration';
import { useSourceSystems } from '../hooks/useSourceSystems';
import { useValidation } from '../hooks/useValidation';
import { useTypeRegistry } from '../hooks/useTypeRegistry';

// Context state interface
export interface ConfigurationContextState {
  // Configuration Management
  configuration: Configuration | null;
  fieldMappings: FieldMapping[];
  isDirty: boolean;
  isLoading: boolean;
  error: string | null;
  
  // Source Systems & Jobs
  sourceSystems: SourceSystem[];
  selectedSourceSystem: SourceSystem | null;
  selectedJob: JobConfig | null;
  sourceFields: SourceField[];
  loadJobsForSystem: (systemId: string) => Promise<void>;
  
  // Validation
  validationResult: ValidationResult | null;
  isValidating: boolean;
  
  // UI State
  currentTransactionType: TransactionType;
  availableTransactionTypes: TransactionType[];
  
  // Actions
  loadConfiguration: (sourceSystem: string, jobName: string) => Promise<void>;
  saveConfiguration: () => Promise<boolean>;
  updateFieldMapping: (mapping: FieldMapping) => void;
  deleteFieldMapping: (fieldName: string, transactionType?: TransactionType) => void;
  addFieldMapping: (mapping: Omit<FieldMapping, 'id'>) => void;
  reorderFieldMappings: (fromIndex: number, toIndex: number) => void;
  resetConfiguration: () => void;
  
  // Source System Actions
  selectSourceSystem: (systemId: string) => Promise<void>;
  selectJob: (jobName: string) => Promise<void>;
  refreshSourceSystems: () => Promise<void>;
  
  // Validation Actions
  validateConfiguration: () => Promise<void>;
  
  // Transaction Type Actions
  setCurrentTransactionType: (transactionType: TransactionType) => void;
  addTransactionType: (code: string, name: string, description?: string) => Promise<boolean>;
  
  // Utility
  hasUnsavedChanges: () => boolean;
  getCurrentTransactionMappings: () => FieldMapping[];
}

// Create context
const ConfigurationContext = createContext<ConfigurationContextState | null>(null);

// Context provider props
interface ConfigurationProviderProps {
  children: ReactNode;
}

// Context provider component
export const ConfigurationProvider: React.FC<ConfigurationProviderProps> = ({ children }) => {
  // State for selected systems/jobs
  const [selectedSourceSystem, setSelectedSourceSystem] = useState<SourceSystem | null>(null);
  const [selectedJob, setSelectedJob] = useState<JobConfig | null>(null);
  const [currentTransactionType, setCurrentTransactionType] = useState<TransactionType>('default');
  
  // Use existing hooks
  const configHook = useConfiguration();
  const { 
    sourceSystems, 
    sourceFields, 
    isLoading: systemsLoading, 
    error: systemsError,
    loadSourceSystems,
    loadSourceFields,
    loadJobsForSystem 
  } = useSourceSystems();
  
  const { 
    validationResult, 
    isValidating, 
    validateConfiguration: validateConfig 
  } = useValidation();
  
  const { getTransactionTypes } = useTypeRegistry();
  
  // Select source system and load its jobs
const selectSourceSystem = useCallback(async (systemId: string) => {
  const system = sourceSystems.find(s => s.id === systemId);
  
  if (!system) {
    throw new Error(`Source system ${systemId} not found`);
  }
  
  setSelectedSourceSystem(system);
  setSelectedJob(null); // Clear job selection
  
  // Persist selection in localStorage for navigation
  localStorage.setItem('selectedSourceSystem', systemId);
  
  // Load source fields for this system
  try {
    await loadSourceFields(systemId);
  } catch (error) {
    console.error('Failed to load source fields:', error);
  }
}, [sourceSystems, loadSourceFields]);

// Add useEffect to restore selection on page load
useEffect(() => {
    console.log('Source systems updated:', sourceSystems);
  const savedSystemId = localStorage.getItem('selectedSourceSystem');
  if (savedSystemId && sourceSystems.length > 0 && !selectedSourceSystem) {
    const system = sourceSystems.find(s => s.id === savedSystemId);
    if (system) {
      setSelectedSourceSystem(system);
    }
  }
}, [sourceSystems, selectedSourceSystem]);
  
  // Select job and load its configuration
 const selectJob = useCallback(async (jobName: string) => {
  if (!selectedSourceSystem) {
    throw new Error('No source system selected');
  }
  
  // Since jobs array might be empty from API, create a mock job
  let job = selectedSourceSystem.jobs?.find(j => j.jobName === jobName);
  
  if (!job) {
    // Create a temporary job if not found
    job = {
      name: jobName,
      jobName: jobName,
      sourceSystem: selectedSourceSystem.id,
      description: '${jobName} job',
      files: []
    };
    console.log('Job not found, creating temporary job:', job);
  }
  
  setSelectedJob(job);
}, [selectedSourceSystem]);
  
  // Refresh source systems
  const refreshSourceSystems = useCallback(async () => {
    try {
      await loadSourceSystems();
    } catch (error) {
      console.error('Failed to refresh source systems:', error);
    }
  }, [loadSourceSystems]);

  
  
  // Validate current configuration
  const validateConfiguration = useCallback(async () => {
    if (!configHook.configuration) return;
    
    try {
      await validateConfig(configHook.configuration);
    } catch (error) {
      console.error('Validation failed:', error);
    }
  }, [configHook.configuration, validateConfig]);
  
  // Set current transaction type with validation
  const setCurrentTransactionTypeWrapped = useCallback((transactionType: TransactionType) => {
    setCurrentTransactionType(transactionType);
    configHook.setTransactionType(transactionType);
  }, [configHook]);
  
  // Get available transaction types
  const availableTransactionTypes = getTransactionTypes().map(t => t.code);
  
  // Combine loading states
  const isLoading = configHook.isLoading || systemsLoading;
  
  // Combine errors
  const error = configHook.error || systemsError;
  
  // Context value
  const contextValue: ConfigurationContextState = {
    // Configuration state
    configuration: configHook.configuration,
    fieldMappings: configHook.fieldMappings,
    isDirty: configHook.isDirty,
    isLoading,
    error,
    
    // Source systems
    loadJobsForSystem,
    sourceSystems,
    selectedSourceSystem,
    selectedJob,
    sourceFields,
    
    // Validation
    validationResult,
    isValidating,
    
    // UI state
    currentTransactionType,
    availableTransactionTypes,
    
    // Configuration actions
    loadConfiguration: configHook.loadConfiguration,
    saveConfiguration: configHook.saveConfiguration,
    updateFieldMapping: configHook.updateFieldMapping,
    deleteFieldMapping: configHook.deleteFieldMapping,
    addFieldMapping: configHook.addFieldMapping,
    reorderFieldMappings: configHook.reorderFieldMappings,
    resetConfiguration: configHook.resetConfiguration,
    
    // Source system actions
    selectSourceSystem,
    selectJob,
    refreshSourceSystems,
    
    // Validation actions
    validateConfiguration,
    
    // Transaction type actions
    setCurrentTransactionType: setCurrentTransactionTypeWrapped,
    addTransactionType: configHook.addTransactionType,
    
    // Utility
    hasUnsavedChanges: configHook.hasUnsavedChanges,
    getCurrentTransactionMappings: configHook.getCurrentTransactionMappings
  };
  
  return (
    <ConfigurationContext.Provider value={contextValue}>
      {children}
    </ConfigurationContext.Provider>
  );
};

// Hook to use the configuration context
export const useConfigurationContext = (): ConfigurationContextState => {
  const context = useContext(ConfigurationContext);
  if (!context) {
    throw new Error('useConfigurationContext must be used within a ConfigurationProvider');
  }
  return context;
};

// Convenience hooks for specific parts of the context
export const useConfigurationState = () => {
  const { configuration, fieldMappings, isDirty, isLoading, error } = useConfigurationContext();
  return { configuration, fieldMappings, isDirty, isLoading, error };
};

export const useSourceSystemsState = () => {
  const { 
    sourceSystems, 
    selectedSourceSystem, 
    selectedJob, 
    sourceFields,
    selectSourceSystem,
    selectJob,
    refreshSourceSystems 
  } = useConfigurationContext();
  
  return { 
    sourceSystems, 
    selectedSourceSystem, 
    selectedJob, 
    sourceFields,
    selectSourceSystem,
    selectJob,
    refreshSourceSystems 
  };
};

export const useValidationState = () => {
  const { validationResult, isValidating, validateConfiguration } = useConfigurationContext();
  return { validationResult, isValidating, validateConfiguration };
};