
import axios from 'axios';

import { Configuration } from '../../types/configuration';
import { ValidationResult } from '../../types/configuration';
import { SourceSystem } from '../../types/configuration';
import { JobConfigResponse} from '../../types/configuration';
import { ApiResponse } from '../../types/configuration';
import { SourceField} from '../../types/configuration';


const API_BASE_URL = 'http://localhost:8080/api/config'; // Update with your actual API base URL




// Configure axios defaults
axios.defaults.timeout = 10000;
axios.defaults.headers.common['Content-Type'] = 'application/json';

// Request interceptor for logging
axios.interceptors.request.use(
  (config) => {
    console.log(`🌐 API Request: ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    console.error('🚨 API Request Error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
axios.interceptors.response.use(
  (response) => {
    console.log('Raw API response for source systems:', response.data);
    console.log(`✅ API Response: ${response.status} ${response.config.url}`);
    return response;
  },
  (error) => {
    console.error('🚨 API Response Error:', error.response?.status, error.message);
    
    if (error.response?.status === 404) {
      throw new Error('Resource not found');
    } else if (error.response?.status === 500) {
      throw new Error('Server error occurred');
    } else if (error.code === 'ECONNABORTED') {
      throw new Error('Request timeout');
    }
    
    throw error;
  }
);

export const configApi = {
  // Source Systems
  getSourceSystems: async (): Promise<SourceSystem[]> => {
    try {
      const response = await axios.get<SourceSystem[]>(`${API_BASE_URL}/source-systems`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch source systems:', error);
      throw new Error('Failed to load source systems');
    }
  },

  getJobsForSourceSystem: async (sourceSystem: string): Promise<JobConfigResponse[]> => {
  try {
    const response = await axios.get<JobConfigResponse[]>(`${API_BASE_URL}/source-systems/${sourceSystem}/jobs`);
    return response.data;
  } catch (error) {
    console.error(`Failed to fetch jobs for ${sourceSystem}:`, error);
    throw new Error(`Failed to load jobs for ${sourceSystem}`);
  }
},

  // Field Mappings
  // src/services/api/configApi.ts - Updated getFieldMappings with mock fallback
// Replace the existing getFieldMappings function with this version:

  // Field Mappings
  getFieldMappings: async (sourceSystem: string, jobName: string): Promise<Configuration[]> => {
    try {
      const response = await axios.get<Configuration[]>(`${API_BASE_URL}/mappings/${sourceSystem}/${jobName}`);
      return response.data;
    } catch (error) {
      console.warn('API failed, using mock configuration:', error);
      
      // Mock configuration fallback
      const mockConfiguration: Configuration = {
        fileType: 'p327',
        transactionType: '200',
        sourceSystem,
        jobName,
        fieldMappings: [
          {
            id: 'field_1',
            fieldName: 'emp_id',
            value: undefined,
            sourceField: 'employee_id',
            targetField: 'EMPLOYEE_ID',
            length: 10,
            pad: 'right',
            padChar: ' ',
            sources: undefined,
            transform: '',
            delimiter: undefined,
            format: '',
            sourceFormat: undefined,
            targetFormat: undefined,
            transformationType: 'source',
            conditions: [],
            targetPosition: 1,
            dataType: 'string',
            defaultValue: '',
            required: false,
            expression: undefined
          },
          {
            id: 'field_2',
            fieldName: 'location-code',
            value: undefined,
            sourceField: '',
            targetField: 'LOCATION-CODE',
            length: 6,
            pad: 'right',
            padChar: ' ',
            sources: undefined,
            transform: '',
            delimiter: undefined,
            format: '',
            sourceFormat: undefined,
            targetFormat: undefined,
            transformationType: 'constant',
            conditions: [{
              ifExpr: '',
              then: undefined,
              elseExpr: undefined,
              elseIfExprs: undefined
            }],
            targetPosition: 2,
            dataType: 'string',
            defaultValue: '100020',
            required: false,
            expression: undefined
          },
          {
            id: 'field_3',
            fieldName: 'full_name',
            value: undefined,
            sourceField: '',
            targetField: 'FULL_NAME',
            length: 50,
            pad: 'right',
            padChar: ' ',
            sources: [
              { field: 'first_name' },
              { field: 'last_name' }
            ],
            transform: 'concat',
            delimiter: ' ',
            format: '',
            sourceFormat: undefined,
            targetFormat: undefined,
            transformationType: 'composite',
            conditions: [],
            targetPosition: 3,
            dataType: 'string',
            defaultValue: '',
            required: false,
            expression: undefined
          }
        ],
        fields: {},
        availableTransactionTypes: ['200', '900', 'default']
      };
      
      return [mockConfiguration];
    }
  },

  getSpecificMapping: async (sourceSystem: string, jobName: string, transactionType: string): Promise<Configuration> => {
    try {
      const response = await axios.get<Configuration>(`${API_BASE_URL}/mappings/${sourceSystem}/${jobName}/${transactionType}`);
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch mapping for ${sourceSystem}.${jobName}.${transactionType}:`, error);
      throw new Error(`Failed to load mapping for ${sourceSystem}.${jobName}.${transactionType}`);
    }
  },

  saveConfiguration: async (configuration: Configuration): Promise<ApiResponse<Configuration>> => {
    try {
      const response = await axios.post<ApiResponse<Configuration>>(`${API_BASE_URL}/mappings/save`, configuration);
      return response.data;
    } catch (error) {
      console.error('Failed to save configuration:', error);
      throw new Error('Failed to save configuration');
    }
  },

  // Validation
  validateMapping: async (mapping: Configuration): Promise<ValidationResult> => {
    try {
      const response = await axios.post<ValidationResult>(`${API_BASE_URL}/mappings/validate`, mapping);
      return response.data;
    } catch (error) {
      console.error('Failed to validate mapping:', error);
      throw new Error('Failed to validate mapping configuration');
    }
  },

  // YAML Generation
  generateYaml: async (mapping: Configuration): Promise<{ yamlContent: string }> => {
    try {
      const response = await axios.post<{ yamlContent: string }>(`${API_BASE_URL}/mappings/generate-yaml`, mapping);
      return response.data;
    } catch (error) {
      console.error('Failed to generate YAML:', error);
      throw new Error('Failed to generate YAML from mapping');
    }
  },

  // Source Fields
  getSourceFields: async (sourceSystem: string): Promise<SourceField[]> => {
    try {
      const response = await axios.get<SourceField[]>(`${API_BASE_URL}/source-systems/${sourceSystem}/fields`);
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch source fields for ${sourceSystem}:`, error);
      throw new Error(`Failed to load source fields for ${sourceSystem}`);
    }
  },

  // Preview Output
  previewOutput: async (mapping: Configuration, sampleData?: Record<string, any>[]): Promise<{ preview: string[] }> => {
    try {
      const response = await axios.post<{ preview: string[] }>(`${API_BASE_URL}/mappings/preview`, {
        mapping,
        sampleData
      });
      return response.data;
    } catch (error) {
      console.error('Failed to generate output preview:', error);
      throw new Error('Failed to generate output preview');
    }
  },

  // Test Configuration
  testConfiguration: async (sourceSystem: string, jobName: string): Promise<{ success: boolean; message: string }> => {
    try {
      const response = await axios.post<{ success: boolean; message: string }>(`${API_BASE_URL}/test/${sourceSystem}/${jobName}`);
      return response.data;
    } catch (error) {
      console.error(`Failed to test configuration for ${sourceSystem}.${jobName}:`, error);
      throw new Error('Failed to test configuration');
    }
  }
};

// Utility functions
export const apiUtils = {
  formatError: (error: any): string => {
    if (error.response?.data?.message) {
      return error.response.data.message;
    }
    return error.message || 'An unexpected error occurred';
  },

  isNetworkError: (error: any): boolean => {
    return !error.response || error.code === 'NETWORK_ERROR';
  },

  retryWithBackoff: async <T>(fn: () => Promise<T>, maxRetries = 3, delay = 1000): Promise<T> => {
    for (let i = 0; i < maxRetries; i++) {
      try {
        return await fn();
      } catch (error) {
        if (i === maxRetries - 1) throw error;
        
        const waitTime = delay * Math.pow(2, i);
        console.log(`Retry ${i + 1}/${maxRetries} in ${waitTime}ms...`);
        await new Promise(resolve => setTimeout(resolve, waitTime));
      }
    }
    throw new Error('Max retries exceeded');
  }
};

// Test API connection
export const testConnection = async (): Promise<{ success: boolean; message: string }> => {
  try {
    await configApi.getSourceSystems();
    return { success: true, message: 'API connection successful' };
  } catch (error) {
    return { success: false, message: apiUtils.formatError(error) };
  }
};