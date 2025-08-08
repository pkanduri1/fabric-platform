import { useState, useCallback, useEffect } from 'react';
import { configApi } from '../services/api/configApi';
import { Configuration, FieldMapping, TransactionType } from '../types/configuration';
import { useTypeRegistry } from './useTypeRegistry';

export interface UseConfigurationReturn {
  // State
  configuration: Configuration | null;
  fieldMappings: FieldMapping[];
  isLoading: boolean;
  error: string | null;
  isDirty: boolean;
  
  // Actions
  loadConfiguration: (sourceSystem: string, jobName: string) => Promise<void>;
  saveConfiguration: () => Promise<boolean>;
  updateFieldMapping: (mapping: FieldMapping) => void;
  deleteFieldMapping: (fieldName: string, transactionType?: TransactionType) => void;
  addFieldMapping: (mapping: Omit<FieldMapping, 'id'>) => void;
  reorderFieldMappings: (fromIndex: number, toIndex: number) => void;
  resetConfiguration: () => void;
  setTransactionType: (transactionType: TransactionType) => void;
  
  // Dynamic type management
  getAvailableTransactionTypes: () => TransactionType[];
  addTransactionType: (code: string, name: string, description?: string) => Promise<boolean>;
  getAvailableDataTypes: () => string[];
  
  // Computed
  getCurrentTransactionMappings: () => FieldMapping[];
  hasUnsavedChanges: () => boolean;
}

export const useConfiguration = (
  initialSourceSystem?: string,
  initialJobName?: string
): UseConfigurationReturn => {
  const [configuration, setConfiguration] = useState<Configuration | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isDirty, setIsDirty] = useState(false);
  const [originalConfiguration, setOriginalConfiguration] = useState<Configuration | null>(null);

  // Use type registry for dynamic types
  const typeRegistry = useTypeRegistry();

  // Load configuration from API
  const loadConfiguration = useCallback(async (sourceSystem: string, jobName: string) => {
    setIsLoading(true);
    setError(null);
    
    try {
      // Try to get all mappings first
      const mappings = await configApi.getFieldMappings(sourceSystem, jobName);
      
      // Convert array of configurations to single configuration
      // Use the first one or create a default configuration
      let loadedConfig: Configuration;
      
      if (mappings && mappings.length > 0) {
        loadedConfig = mappings[0];
        // Ensure fieldMappings array exists
        if (!loadedConfig.fieldMappings) {
          loadedConfig.fieldMappings = [];
        }
      } else {
        // Create default configuration
        loadedConfig = {
          fileType: '',
          transactionType: 'default',
          sourceSystem,
          jobName,
          fieldMappings: [],
          fields: {}
        };
      }
      
      setConfiguration(loadedConfig);
      setOriginalConfiguration(JSON.parse(JSON.stringify(loadedConfig))); // Deep copy
      setIsDirty(false);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load configuration';
      setError(errorMessage);
      console.error('Failed to load configuration:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Save configuration to API
  const saveConfiguration = useCallback(async (): Promise<boolean> => {
    if (!configuration) {
      setError('No configuration to save');
      return false;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await configApi.saveConfiguration(configuration);
      
      // Handle different response formats
      let savedConfig: Configuration;
      if (response.success && response.data) {
        savedConfig = response.data;
      } else if (response.data) {
        savedConfig = response.data;
      } else {
        savedConfig = configuration; // Use original if no data returned
      }
      
      setConfiguration(savedConfig);
      setOriginalConfiguration(JSON.parse(JSON.stringify(savedConfig))); // Deep copy
      setIsDirty(false);
      return true;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to save configuration';
      setError(errorMessage);
      console.error('Failed to save configuration:', err);
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [configuration]);

  // Update a field mapping
  const updateFieldMapping = useCallback((updatedMapping: FieldMapping) => {
    if (!configuration) return;

    setConfiguration(prev => {
      if (!prev) return prev;

      const updatedMappings = prev.fieldMappings.map(mapping =>
        mapping.fieldName === updatedMapping.fieldName && 
        mapping.transactionType === updatedMapping.transactionType
          ? updatedMapping
          : mapping
      );

      return {
        ...prev,
        fieldMappings: updatedMappings
      };
    });
    
    setIsDirty(true);
  }, [configuration]);

  // Delete a field mapping
  const deleteFieldMapping = useCallback((fieldName: string, transactionType?: TransactionType) => {
    if (!configuration) return;

    setConfiguration(prev => {
      if (!prev) return prev;

      const filteredMappings = prev.fieldMappings.filter(mapping =>
        !(mapping.fieldName === fieldName && 
          (transactionType === undefined || mapping.transactionType === transactionType))
      );

      return {
        ...prev,
        fieldMappings: filteredMappings
      };
    });
    
    setIsDirty(true);
  }, [configuration]);

  // Add a new field mapping
  const addFieldMapping = useCallback((mapping: Omit<FieldMapping, 'id'>) => {
    if (!configuration) return;

    const newMapping: FieldMapping = {
      ...mapping,
      id: `mapping_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    };

    setConfiguration(prev => {
      if (!prev) return prev;

      return {
        ...prev,
        fieldMappings: [...prev.fieldMappings, newMapping]
      };
    });
    
    setIsDirty(true);
  }, [configuration]);

  // Reorder field mappings
  const reorderFieldMappings = useCallback((fromIndex: number, toIndex: number) => {
    if (!configuration) return;

    setConfiguration(prev => {
      if (!prev) return prev;

      const newMappings = [...prev.fieldMappings];
      const [movedMapping] = newMappings.splice(fromIndex, 1);
      newMappings.splice(toIndex, 0, movedMapping);

      // Update target positions
      newMappings.forEach((mapping, index) => {
        mapping.targetPosition = index + 1;
      });

      return {
        ...prev,
        fieldMappings: newMappings
      };
    });
    
    setIsDirty(true);
  }, [configuration]);

  // Reset configuration to original state
  const resetConfiguration = useCallback(() => {
    if (originalConfiguration) {
      setConfiguration(JSON.parse(JSON.stringify(originalConfiguration)));
      setIsDirty(false);
      setError(null);
    }
  }, [originalConfiguration]);

  // Set transaction type (for multi-transaction configurations)
  const setTransactionType = useCallback((transactionType: TransactionType) => {
    if (!configuration) return;

    setConfiguration(prev => {
      if (!prev) return prev;

      return {
        ...prev,
        currentTransactionType: transactionType
      };
    });
  }, [configuration]);

  // Get mappings for current transaction type
  const getCurrentTransactionMappings = useCallback((): FieldMapping[] => {
    if (!configuration) return [];

    const currentType = configuration.currentTransactionType || 'default';
    return configuration.fieldMappings.filter(mapping => 
      mapping.transactionType === currentType
    );
  }, [configuration]);

  // Check if there are unsaved changes
  const hasUnsavedChanges = useCallback((): boolean => {
    if (!configuration || !originalConfiguration) return false;
    
    return JSON.stringify(configuration) !== JSON.stringify(originalConfiguration);
  }, [configuration, originalConfiguration]);

  // Dynamic type management functions
  const getAvailableTransactionTypes = useCallback((): TransactionType[] => {
    if (configuration?.availableTransactionTypes) {
      return configuration.availableTransactionTypes;
    }
    return typeRegistry.getTransactionTypes().map(t => t.code);
  }, [configuration, typeRegistry]);

  const addTransactionType = useCallback(async (code: string, name: string, description?: string): Promise<boolean> => {
    const success = await typeRegistry.addTransactionType({ code, name, description });
    
    if (success && configuration) {
      // Update configuration to include new transaction type
      setConfiguration(prev => ({
        ...prev!,
        availableTransactionTypes: [...(prev!.availableTransactionTypes || []), code]
      }));
      setIsDirty(true);
    }
    
    return success;
  }, [typeRegistry, configuration]);

  const getAvailableDataTypes = useCallback((): string[] => {
    return typeRegistry.getDataTypes().map(t => t.code);
  }, [typeRegistry]);

  // Load initial configuration if provided
  useEffect(() => {
    if (initialSourceSystem && initialJobName) {
      loadConfiguration(initialSourceSystem, initialJobName);
    }
  }, [initialSourceSystem, initialJobName, loadConfiguration]);

  return {
    // State
    configuration,
    fieldMappings: configuration?.fieldMappings || [],
    isLoading,
    error,
    isDirty,
    
    // Actions
    loadConfiguration,
    saveConfiguration,
    updateFieldMapping,
    deleteFieldMapping,
    addFieldMapping,
    reorderFieldMappings,
    resetConfiguration,
    setTransactionType,
    
    // Dynamic type management
    getAvailableTransactionTypes,
    addTransactionType,
    getAvailableDataTypes,
    
    // Computed
    getCurrentTransactionMappings,
    hasUnsavedChanges
  };
};