import axios from 'axios';
import { 
  TypeRegistry, 
  TransactionTypeDefinition, 
  FileTypeDefinition, 
  SourceSystemTypeDefinition,
  DataTypeDefinition,
  ApiResponse
} from '@/types/configuration';

const API_BASE_URL = '/api/ui/types';

// Configure axios for type management
const typeApi = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

export const typeRegistryApi = {
  // Get complete type registry
  getTypeRegistry: async (): Promise<TypeRegistry> => {
    try {
      const response = await typeApi.get<ApiResponse<TypeRegistry>>('/registry');
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to load type registry');
    } catch (error) {
      console.error('Failed to fetch type registry:', error);
      
      // Return default registry if API fails (for development)
      return getDefaultRegistry();
    }
  },

  // Transaction Type operations
  getTransactionTypes: async (): Promise<TransactionTypeDefinition[]> => {
    try {
      const response = await typeApi.get<ApiResponse<TransactionTypeDefinition[]>>('/transaction-types');
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to load transaction types');
    } catch (error) {
      console.error('Failed to fetch transaction types:', error);
      throw error;
    }
  },

  addTransactionType: async (type: Omit<TransactionTypeDefinition, 'code'> & { code?: string }): Promise<TransactionTypeDefinition> => {
    try {
      const response = await typeApi.post<ApiResponse<TransactionTypeDefinition>>('/transaction-types', type);
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to add transaction type');
    } catch (error) {
      console.error('Failed to add transaction type:', error);
      throw error;
    }
  },

  updateTransactionType: async (code: string, updates: Partial<TransactionTypeDefinition>): Promise<TransactionTypeDefinition> => {
    try {
      const response = await typeApi.put<ApiResponse<TransactionTypeDefinition>>(`/transaction-types/${code}`, updates);
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to update transaction type');
    } catch (error) {
      console.error('Failed to update transaction type:', error);
      throw error;
    }
  },

  deleteTransactionType: async (code: string): Promise<boolean> => {
    try {
      const response = await typeApi.delete<ApiResponse<boolean>>(`/transaction-types/${code}`);
      return response.data.success;
    } catch (error) {
      console.error('Failed to delete transaction type:', error);
      throw error;
    }
  },

  // File Type operations
  getFileTypes: async (): Promise<FileTypeDefinition[]> => {
    try {
      const response = await typeApi.get<ApiResponse<FileTypeDefinition[]>>('/file-types');
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to load file types');
    } catch (error) {
      console.error('Failed to fetch file types:', error);
      throw error;
    }
  },

  addFileType: async (type: Omit<FileTypeDefinition, 'code'> & { code?: string }): Promise<FileTypeDefinition> => {
    try {
      const response = await typeApi.post<ApiResponse<FileTypeDefinition>>('/file-types', type);
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to add file type');
    } catch (error) {
      console.error('Failed to add file type:', error);
      throw error;
    }
  },

  updateFileType: async (code: string, updates: Partial<FileTypeDefinition>): Promise<FileTypeDefinition> => {
    try {
      const response = await typeApi.put<ApiResponse<FileTypeDefinition>>(`/file-types/${code}`, updates);
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to update file type');
    } catch (error) {
      console.error('Failed to update file type:', error);
      throw error;
    }
  },

  deleteFileType: async (code: string): Promise<boolean> => {
    try {
      const response = await typeApi.delete<ApiResponse<boolean>>(`/file-types/${code}`);
      return response.data.success;
    } catch (error) {
      console.error('Failed to delete file type:', error);
      throw error;
    }
  },

  // Source System Type operations
  getSourceSystemTypes: async (): Promise<SourceSystemTypeDefinition[]> => {
    try {
      const response = await typeApi.get<ApiResponse<SourceSystemTypeDefinition[]>>('/source-system-types');
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to load source system types');
    } catch (error) {
      console.error('Failed to fetch source system types:', error);
      throw error;
    }
  },

  addSourceSystemType: async (type: Omit<SourceSystemTypeDefinition, 'code'> & { code?: string }): Promise<SourceSystemTypeDefinition> => {
    try {
      const response = await typeApi.post<ApiResponse<SourceSystemTypeDefinition>>('/source-system-types', type);
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to add source system type');
    } catch (error) {
      console.error('Failed to add source system type:', error);
      throw error;
    }
  },

  updateSourceSystemType: async (code: string, updates: Partial<SourceSystemTypeDefinition>): Promise<SourceSystemTypeDefinition> => {
    try {
      const response = await typeApi.put<ApiResponse<SourceSystemTypeDefinition>>(`/source-system-types/${code}`, updates);
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to update source system type');
    } catch (error) {
      console.error('Failed to update source system type:', error);
      throw error;
    }
  },

  deleteSourceSystemType: async (code: string): Promise<boolean> => {
    try {
      const response = await typeApi.delete<ApiResponse<boolean>>(`/source-system-types/${code}`);
      return response.data.success;
    } catch (error) {
      console.error('Failed to delete source system type:', error);
      throw error;
    }
  },

  // Data Type operations
  getDataTypes: async (): Promise<DataTypeDefinition[]> => {
    try {
      const response = await typeApi.get<ApiResponse<DataTypeDefinition[]>>('/data-types');
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to load data types');
    } catch (error) {
      console.error('Failed to fetch data types:', error);
      throw error;
    }
  },

  addDataType: async (type: Omit<DataTypeDefinition, 'code'> & { code?: string }): Promise<DataTypeDefinition> => {
    try {
      const response = await typeApi.post<ApiResponse<DataTypeDefinition>>('/data-types', type);
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to add data type');
    } catch (error) {
      console.error('Failed to add data type:', error);
      throw error;
    }
  },

  updateDataType: async (code: string, updates: Partial<DataTypeDefinition>): Promise<DataTypeDefinition> => {
    try {
      const response = await typeApi.put<ApiResponse<DataTypeDefinition>>(`/data-types/${code}`, updates);
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.message || 'Failed to update data type');
    } catch (error) {
      console.error('Failed to update data type:', error);
      throw error;
    }
  },

  deleteDataType: async (code: string): Promise<boolean> => {
    try {
      const response = await typeApi.delete<ApiResponse<boolean>>(`/data-types/${code}`);
      return response.data.success;
    } catch (error) {
      console.error('Failed to delete data type:', error);
      throw error;
    }
  }
};

// Default registry for development/fallback
function getDefaultRegistry(): TypeRegistry {
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
}