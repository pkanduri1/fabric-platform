import { useState, useCallback, useEffect, useMemo } from 'react';
import { 
  TypeRegistry, 
  TransactionTypeDefinition, 
  FileTypeDefinition, 
  SourceSystemTypeDefinition,
  DataTypeDefinition,
  TransformationTypeDefinition
} from '../types/configuration';

// Import the actual API - you'll need to create this file
// import { typeRegistryApi } from '@/services/api/typeRegistryApi';

// Temporary mock API for development
const typeRegistryApi = {
  getTypeRegistry: async (): Promise<TypeRegistry> => {
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 500));
    
    return {
      transactionTypes: [
        { code: 'default', name: 'Default', description: 'Default transaction type', isDefault: true, sortOrder: 1 },
        { code: '200', name: 'Transaction 200', description: 'Standard transaction', sortOrder: 2 },
        { code: '900', name: 'Transaction 900', description: 'Adjustment transaction', sortOrder: 3 },
        { code: 'TXN_A', name: 'Transaction A', description: 'Type A transaction', sortOrder: 4 },
        { code: 'TXN_B', name: 'Transaction B', description: 'Type B transaction', sortOrder: 5 }
      ],
      fileTypes: [
        { code: 'atoctran', name: 'ATOC Transaction', description: 'ATOC transaction file format', extension: '.dat', sortOrder: 1 },
        { code: 'cdstrans', name: 'CDS Transaction', description: 'CDS transaction file format', extension: '.dat', sortOrder: 2 },
        { code: 'trainers', name: 'Trainers', description: 'Training data file format', extension: '.dat', sortOrder: 3 }
      ],
      sourceSystemTypes: [
        { code: 'shaw', name: 'Shaw System', description: 'Shaw communication system', sortOrder: 1 },
        { code: 'legacy', name: 'Legacy System', description: 'Legacy mainframe system', sortOrder: 2 },
        { code: 'modern', name: 'Modern System', description: 'Modern web-based system', sortOrder: 3 }
      ],
      dataTypes: [
        { code: 'string', name: 'String', description: 'Text data', allowsLength: true, sortOrder: 1 },
        { code: 'String', name: 'String (Java)', description: 'Java String type', allowsLength: true, sortOrder: 2 },
        { code: 'numeric', name: 'Numeric', description: 'Numeric data', allowsLength: true, sortOrder: 3 },
        { code: 'Integer', name: 'Integer', description: 'Whole numbers', allowsLength: true, sortOrder: 4 },
        { code: 'Decimal', name: 'Decimal', description: 'Decimal numbers', allowsLength: true, sortOrder: 5 },
        { code: 'date', name: 'Date', description: 'Date values', allowsLength: false, sortOrder: 6 }
      ],
      transformationTypes: [
        { code: 'constant', name: 'Constant', description: 'Fixed value', requiresValue: true, sortOrder: 1 },
        { code: 'source', name: 'Source Field', description: 'Copy from source field', requiresSourceField: true, sortOrder: 2 },
        { code: 'composite', name: 'Composite', description: 'Combine multiple fields', allowsFormat: true, sortOrder: 3 },
        { code: 'conditional', name: 'Conditional', description: 'IF-THEN-ELSE logic', requiresConditions: true, sortOrder: 4 },
        { code: 'blank', name: 'Blank', description: 'Empty value', sortOrder: 5 }
      ]
    };
  },
  
  addTransactionType: async (type: TransactionTypeDefinition): Promise<TransactionTypeDefinition> => {
    await new Promise(resolve => setTimeout(resolve, 300));
    return type;
  },
  
  updateTransactionType: async (code: string, updates: Partial<TransactionTypeDefinition>): Promise<TransactionTypeDefinition> => {
    await new Promise(resolve => setTimeout(resolve, 300));
    return { code, name: 'Updated', ...updates } as TransactionTypeDefinition;
  },
  
  deleteTransactionType: async (code: string): Promise<boolean> => {
    await new Promise(resolve => setTimeout(resolve, 300));
    return true;
  }
};

export interface UseTypeRegistryReturn {
  // State
  registry: TypeRegistry | null;
  isLoading: boolean;
  error: string | null;
  
  // Actions
  loadRegistry: () => Promise<void>;
  refreshRegistry: () => Promise<void>;
  
  // Transaction Types
  addTransactionType: (type: Omit<TransactionTypeDefinition, 'code'> & { code?: string }) => Promise<boolean>;
  updateTransactionType: (code: string, updates: Partial<TransactionTypeDefinition>) => Promise<boolean>;
  deleteTransactionType: (code: string) => Promise<boolean>;
  
  // File Types  
  addFileType: (type: Omit<FileTypeDefinition, 'code'> & { code?: string }) => Promise<boolean>;
  updateFileType: (code: string, updates: Partial<FileTypeDefinition>) => Promise<boolean>;
  deleteFileType: (code: string) => Promise<boolean>;
  
  // Source System Types
  addSourceSystemType: (type: Omit<SourceSystemTypeDefinition, 'code'> & { code?: string }) => Promise<boolean>;
  updateSourceSystemType: (code: string, updates: Partial<SourceSystemTypeDefinition>) => Promise<boolean>;
  deleteSourceSystemType: (code: string) => Promise<boolean>;
  
  // Data Types
  addDataType: (type: Omit<DataTypeDefinition, 'code'> & { code?: string }) => Promise<boolean>;
  updateDataType: (code: string, updates: Partial<DataTypeDefinition>) => Promise<boolean>;
  deleteDataType: (code: string) => Promise<boolean>;
  
  // Getters
  getTransactionTypes: () => TransactionTypeDefinition[];
  getFileTypes: () => FileTypeDefinition[];
  getSourceSystemTypes: () => SourceSystemTypeDefinition[];
  getDataTypes: () => DataTypeDefinition[];
  getTransformationTypes: () => TransformationTypeDefinition[];
  
  // Utilities
  getTransactionTypeByCode: (code: string) => TransactionTypeDefinition | undefined;
  getFileTypeByCode: (code: string) => FileTypeDefinition | undefined;
  getSourceSystemTypeByCode: (code: string) => SourceSystemTypeDefinition | undefined;
  getDataTypeByCode: (code: string) => DataTypeDefinition | undefined;
  
  // Validation
  isValidTransactionType: (code: string) => boolean;
  isValidFileType: (code: string) => boolean;
  isValidSourceSystemType: (code: string) => boolean;
  isValidDataType: (code: string) => boolean;
}

export const useTypeRegistry = (autoLoad: boolean = true): UseTypeRegistryReturn => {
  const [registry, setRegistry] = useState<TypeRegistry | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load complete registry
  const loadRegistry = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const loadedRegistry = await typeRegistryApi.getTypeRegistry();
      setRegistry(loadedRegistry);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load type registry';
      setError(errorMessage);
      console.error('Failed to load type registry:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Refresh registry
  const refreshRegistry = useCallback(async () => {
    await loadRegistry();
  }, [loadRegistry]);

  // Transaction Type operations
  const addTransactionType = useCallback(async (type: Omit<TransactionTypeDefinition, 'code'> & { code?: string }): Promise<boolean> => {
    try {
      const code = type.code || type.name.toLowerCase().replace(/\s+/g, '_');
      const newType = await typeRegistryApi.addTransactionType({ ...type, code });
      
      setRegistry(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          transactionTypes: [...prev.transactionTypes, newType].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
        };
      });
      
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add transaction type');
      return false;
    }
  }, []);

  const updateTransactionType = useCallback(async (code: string, updates: Partial<TransactionTypeDefinition>): Promise<boolean> => {
    try {
      const updatedType = await typeRegistryApi.updateTransactionType(code, updates);
      
      setRegistry(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          transactionTypes: prev.transactionTypes.map(t => t.code === code ? updatedType : t)
        };
      });
      
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update transaction type');
      return false;
    }
  }, []);

  const deleteTransactionType = useCallback(async (code: string): Promise<boolean> => {
    try {
      await typeRegistryApi.deleteTransactionType(code);
      
      setRegistry(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          transactionTypes: prev.transactionTypes.filter(t => t.code !== code)
        };
      });
      
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete transaction type');
      return false;
    }
  }, []);

  // File Type operations (similar pattern)
  const addFileType = useCallback(async (type: Omit<FileTypeDefinition, 'code'> & { code?: string }): Promise<boolean> => {
    try {
      const code = type.code || type.name.toLowerCase().replace(/\s+/g, '_');
      const newType = { ...type, code } as FileTypeDefinition;
      
      setRegistry(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          fileTypes: [...prev.fileTypes, newType].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
        };
      });
      
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add file type');
      return false;
    }
  }, []);

  const updateFileType = useCallback(async (code: string, updates: Partial<FileTypeDefinition>): Promise<boolean> => {
    try {
      setRegistry(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          fileTypes: prev.fileTypes.map(t => t.code === code ? { ...t, ...updates } : t)
        };
      });
      
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update file type');
      return false;
    }
  }, []);

  const deleteFileType = useCallback(async (code: string): Promise<boolean> => {
    try {
      setRegistry(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          fileTypes: prev.fileTypes.filter(t => t.code !== code)
        };
      });
      
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete file type');
      return false;
    }
  }, []);

  // Source System Type operations (similar pattern)
  const addSourceSystemType = useCallback(async (type: Omit<SourceSystemTypeDefinition, 'code'> & { code?: string }): Promise<boolean> => {
    // Implementation similar to addFileType
    return true;
  }, []);

  const updateSourceSystemType = useCallback(async (code: string, updates: Partial<SourceSystemTypeDefinition>): Promise<boolean> => {
    // Implementation similar to updateFileType
    return true;
  }, []);

  const deleteSourceSystemType = useCallback(async (code: string): Promise<boolean> => {
    // Implementation similar to deleteFileType
    return true;
  }, []);

  // Data Type operations (similar pattern)
  const addDataType = useCallback(async (type: Omit<DataTypeDefinition, 'code'> & { code?: string }): Promise<boolean> => {
    // Implementation similar to addFileType
    return true;
  }, []);

  const updateDataType = useCallback(async (code: string, updates: Partial<DataTypeDefinition>): Promise<boolean> => {
    // Implementation similar to updateFileType
    return true;
  }, []);

  const deleteDataType = useCallback(async (code: string): Promise<boolean> => {
    // Implementation similar to deleteFileType
    return true;
  }, []);

  // Getter functions
  const getTransactionTypes = useCallback(() => registry?.transactionTypes || [], [registry]);
  const getFileTypes = useCallback(() => registry?.fileTypes || [], [registry]);
  const getSourceSystemTypes = useCallback(() => registry?.sourceSystemTypes || [], [registry]);
  const getDataTypes = useCallback(() => registry?.dataTypes || [], [registry]);
  const getTransformationTypes = useCallback(() => registry?.transformationTypes || [], [registry]);

  // Utility functions
  const getTransactionTypeByCode = useCallback((code: string) => 
    registry?.transactionTypes.find(t => t.code === code), [registry]);

  const getFileTypeByCode = useCallback((code: string) => 
    registry?.fileTypes.find(t => t.code === code), [registry]);

  const getSourceSystemTypeByCode = useCallback((code: string) => 
    registry?.sourceSystemTypes.find(t => t.code === code), [registry]);

  const getDataTypeByCode = useCallback((code: string) => 
    registry?.dataTypes.find(t => t.code === code), [registry]);

  // Validation functions
  const isValidTransactionType = useCallback((code: string) => 
    registry?.transactionTypes.some(t => t.code === code) || false, [registry]);

  const isValidFileType = useCallback((code: string) => 
    registry?.fileTypes.some(t => t.code === code) || false, [registry]);

  const isValidSourceSystemType = useCallback((code: string) => 
    registry?.sourceSystemTypes.some(t => t.code === code) || false, [registry]);

  const isValidDataType = useCallback((code: string) => 
    registry?.dataTypes.some(t => t.code === code) || false, [registry]);

  // Auto-load registry on mount
  useEffect(() => {
    if (autoLoad) {
      loadRegistry();
    }
  }, [autoLoad, loadRegistry]);

  return {
    // State
    registry,
    isLoading,
    error,
    
    // Actions
    loadRegistry,
    refreshRegistry,
    
    // Transaction Types
    addTransactionType,
    updateTransactionType,
    deleteTransactionType,
    
    // File Types
    addFileType,
    updateFileType,
    deleteFileType,
    
    // Source System Types
    addSourceSystemType,
    updateSourceSystemType,
    deleteSourceSystemType,
    
    // Data Types
    addDataType,
    updateDataType,
    deleteDataType,
    
    // Getters
    getTransactionTypes,
    getFileTypes,
    getSourceSystemTypes,
    getDataTypes,
    getTransformationTypes,
    
    // Utilities
    getTransactionTypeByCode,
    getFileTypeByCode,
    getSourceSystemTypeByCode,
    getDataTypeByCode,
    
    // Validation
    isValidTransactionType,
    isValidFileType,
    isValidSourceSystemType,
    isValidDataType
  };
};