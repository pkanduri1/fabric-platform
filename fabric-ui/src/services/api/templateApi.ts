// src/services/api/templateApi.ts
import axios from 'axios';
import { 
  FileType,
  FileTypeTemplate, 
  FieldTemplate, 
  TemplateImportRequest, 
  TemplateImportResult,
  TemplateValidationResult,
  FieldMappingConfig,
  TemplateToConfigurationResult,
  CreateFieldRequest,
  UpdateFieldRequest,
  CreateFileTypeRequest,
  TemplateMetadata
} from '../../types/template';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';

// Create axios instance with interceptors
const templateApi = axios.create({
  baseURL: `${API_BASE_URL}/api/admin/templates`,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'Cache-Control': 'no-cache, no-store, must-revalidate',
    'Pragma': 'no-cache',
    'Expires': '0'
  },
});

// Request interceptor for logging and cache busting
templateApi.interceptors.request.use(
  (config) => {
    // Add cache-busting timestamp to all GET requests
    if (config.method === 'get') {
      config.params = { 
        ...config.params, 
        _t: Date.now() 
      };
    }
    console.log(`🔄 Template API Request: ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    console.error('❌ Template API Request Error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor for logging and error handling
templateApi.interceptors.response.use(
  (response) => {
    console.log(`✅ Template API Response: ${response.status} ${response.config.url}`);
    return response;
  },
  (error) => {
    console.error('❌ Template API Response Error:', error.response?.status, error.response?.data);
    return Promise.reject(error);
  }
);

export const templateApiService = {
  // File Type operations
  async getFileTypes(): Promise<FileType[]> {
    try {
      const response = await templateApi.get<FileType[]>('/file-types');
      return response.data;
    } catch (error) {
      console.error('Failed to fetch file types:', error);
      throw new Error('Failed to load template file types');
    }
  },

  async getFileType(fileType: string): Promise<FileType> {
    try {
      const response = await templateApi.get<FileType>(`/file-types/${fileType}`);
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch file type ${fileType}:`, error);
      throw new Error(`Failed to load file type: ${fileType}`);
    }
  },

  async createFileType(request: CreateFileTypeRequest): Promise<FileType> {
    try {
      const response = await templateApi.post<FileType>('/file-types', request);
      return response.data;
    } catch (error) {
      console.error('Failed to create file type:', error);
      throw new Error('Failed to create file type');
    }
  },

  async updateFileType(fileType: string, request: Partial<CreateFileTypeRequest>): Promise<FileType> {
    try {
      const response = await templateApi.put<FileType>(`/file-types/${fileType}`, request);
      return response.data;
    } catch (error) {
      console.error(`Failed to update file type ${fileType}:`, error);
      throw new Error(`Failed to update file type: ${fileType}`);
    }
  },

  async deleteFileType(fileType: string): Promise<void> {
    try {
      await templateApi.delete(`/file-types/${fileType}`);
    } catch (error) {
      console.error(`Failed to delete file type ${fileType}:`, error);
      throw new Error(`Failed to delete file type: ${fileType}`);
    }
  },

  // Template fields operations
  async getTemplateFields(fileType: string, transactionType: string = 'default'): Promise<FieldTemplate[]> {
    try {
      const response = await templateApi.get<FieldTemplate[]>(`/${fileType}/${transactionType}/fields`);
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch fields for ${fileType}/${transactionType}:`, error);
      throw new Error(`Failed to load template fields: ${fileType}/${transactionType}`);
    }
  },

  async createField(fileType: string, request: CreateFieldRequest): Promise<FieldTemplate> {
    try {
      const response = await templateApi.post<FieldTemplate>(`/${fileType}/fields`, request);
      return response.data;
    } catch (error) {
      console.error('Failed to create field:', error);
      throw new Error('Failed to create field');
    }
  },

  async updateField(fileType: string, fieldName: string, request: UpdateFieldRequest): Promise<FieldTemplate> {
    try {
      const response = await templateApi.put<FieldTemplate>(`/${fileType}/fields/${fieldName}`, request);
      return response.data;
    } catch (error) {
      console.error(`Failed to update field ${fieldName}:`, error);
      throw new Error(`Failed to update field: ${fieldName}`);
    }
  },

  async deleteField(fileType: string, fieldName: string): Promise<void> {
    try {
      await templateApi.delete(`/${fileType}/fields/${fieldName}`);
    } catch (error) {
      console.error(`Failed to delete field ${fieldName}:`, error);
      throw new Error(`Failed to delete field: ${fieldName}`);
    }
  },

  async duplicateField(fileType: string, fieldName: string, newFieldName: string, newPosition: number): Promise<FieldTemplate> {
    try {
      const response = await templateApi.post<FieldTemplate>(`/${fileType}/fields/${fieldName}/duplicate`, {
        newFieldName,
        newPosition
      });
      return response.data;
    } catch (error) {
      console.error(`Failed to duplicate field ${fieldName}:`, error);
      throw new Error(`Failed to duplicate field: ${fieldName}`);
    }
  },

  // Bulk operations
  async bulkUpdateFields(fileType: string, fields: FieldTemplate[]): Promise<FieldTemplate[]> {
    try {
      const response = await templateApi.put<FieldTemplate[]>(`/${fileType}/fields/bulk`, { fields });
      return response.data;
    } catch (error) {
      console.error('Failed to bulk update fields:', error);
      throw new Error('Failed to bulk update fields');
    }
  },

  async reorderFields(fileType: string, fieldOrders: { fieldName: string; newPosition: number }[]): Promise<FieldTemplate[]> {
    try {
      const response = await templateApi.post<FieldTemplate[]>(`/${fileType}/fields/reorder`, { fieldOrders });
      return response.data;
    } catch (error) {
      console.error('Failed to reorder fields:', error);
      throw new Error('Failed to reorder fields');
    }
  },

  // Import/Export operations
  async importFromExcel(file: File, fileType: string, options?: { replaceExisting?: boolean }): Promise<TemplateImportResult> {
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('fileType', fileType);
      if (options?.replaceExisting) {
        formData.append('replaceExisting', 'true');
      }

      const response = await templateApi.post<TemplateImportResult>('/import/excel', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        timeout: 60000, // Longer timeout for file uploads
      });
      return response.data;
    } catch (error) {
      console.error('Failed to import from Excel:', error);
      throw new Error('Excel import failed');
    }
  },

  async importFromJson(request: TemplateImportRequest): Promise<TemplateImportResult> {
    try {
      const response = await templateApi.post<TemplateImportResult>('/import/json', request);
      return response.data;
    } catch (error) {
      console.error('Failed to import JSON template:', error);
      throw new Error('JSON import failed');
    }
  },

  async exportToExcel(fileType: string, transactionType: string = 'default'): Promise<Blob> {
    try {
      const response = await templateApi.get(`/${fileType}/${transactionType}/export/excel`, {
        responseType: 'blob',
      });
      return response.data;
    } catch (error) {
      console.error('Failed to export to Excel:', error);
      throw new Error('Excel export failed');
    }
  },

  async exportToJson(fileType: string, transactionType: string = 'default'): Promise<FileTypeTemplate> {
    try {
      const response = await templateApi.get<FileTypeTemplate>(`/${fileType}/${transactionType}/export/json`);
      return response.data;
    } catch (error) {
      console.error('Failed to export to JSON:', error);
      throw new Error('JSON export failed');
    }
  },

  // Validation operations
  async validateTemplate(fileType: string, fields: FieldTemplate[]): Promise<TemplateValidationResult> {
    try {
      const response = await templateApi.post<TemplateValidationResult>(`/${fileType}/validate`, { fields });
      return response.data;
    } catch (error) {
      console.error('Failed to validate template:', error);
      throw new Error('Template validation failed');
    }
  },

  async validateField(fileType: string, field: FieldTemplate): Promise<{ isValid: boolean; errors: string[] }> {
    try {
      const response = await templateApi.post<{ isValid: boolean; errors: string[] }>(`/${fileType}/validate-field`, field);
      return response.data;
    } catch (error) {
      console.error('Failed to validate field:', error);
      throw new Error('Field validation failed');
    }
  },

  // Configuration generation
  async createConfigurationFromTemplate(
    fileType: string, 
    transactionType: string, 
    sourceSystem: string, 
    jobName: string
  ): Promise<FieldMappingConfig> {
    try {
      const response = await templateApi.post<FieldMappingConfig>(
        `/${fileType}/${transactionType}/create-config`,
        null,
        {
          params: { sourceSystem, jobName, createdBy: 'ui-user' }
        }
      );
      return response.data;
    } catch (error) {
      console.error('Failed to create configuration from template:', error);
      throw new Error('Failed to generate configuration from template');
    }
  },

  async createConfigurationFromTemplateWithMetadata(
    fileType: string, 
    transactionType: string, 
    sourceSystem: string, 
    jobName: string
  ): Promise<TemplateToConfigurationResult> {
    try {
      // First try the metadata endpoint (if it exists in future)
      const response = await templateApi.post<TemplateToConfigurationResult>(
        `/${fileType}/${transactionType}/create-config-with-metadata`,
        null,
        {
          params: { sourceSystem, jobName, createdBy: 'ui-user' }
        }
      );
      return response.data;
    } catch (error) {
      console.warn('Metadata endpoint not available, using standard endpoint');
      
      // Fallback to standard endpoint and add metadata manually
      try {
        const config = await this.createConfigurationFromTemplate(
          fileType, transactionType, sourceSystem, jobName
        );
        
        // Add template metadata manually
        const templateMetadata: TemplateMetadata = {
          fileType,
          transactionType,
          templateVersion: 1,
          fieldsFromTemplate: config.fields?.length || 0,
          totalFields: config.fields?.length || 0,
          generatedAt: new Date().toISOString(),
          generatedBy: 'ui-user'
        };
        
        return {
          ...config,
          templateMetadata
        } as TemplateToConfigurationResult;
      } catch (fallbackError) {
        console.error('Both endpoints failed:', fallbackError);
        throw new Error('Failed to generate configuration from template');
      }
    }
  },

  // Transaction types
  async getTransactionTypes(fileType: string): Promise<string[]> {
    try {
      const response = await templateApi.get<string[]>(`/${fileType}/transaction-types`);
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch transaction types for ${fileType}:`, error);
      return ['default']; // Fallback
    }
  },

  // Template metadata and statistics
  async getTemplateStatistics(fileType: string): Promise<{
    totalFields: number;
    requiredFields: number;
    optionalFields: number;
    totalLength: number;
    lastModified: string;
    version: number;
  }> {
    try {
      const response = await templateApi.get(`/${fileType}/statistics`);
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch statistics for ${fileType}:`, error);
      throw new Error(`Failed to load statistics for: ${fileType}`);
    }
  },

  async getTemplateHistory(fileType: string): Promise<{
    version: number;
    modifiedBy: string;
    modifiedDate: string;
    changes: string[];
  }[]> {
    try {
      const response = await templateApi.get(`/${fileType}/history`);
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch history for ${fileType}:`, error);
      throw new Error(`Failed to load history for: ${fileType}`);
    }
  },

  // Health check
  async healthCheck(): Promise<{ status: string; timestamp: string }> {
    try {
      const response = await templateApi.get('/health');
      return response.data;
    } catch (error) {
      console.error('Template API health check failed:', error);
      throw new Error('Template API is unavailable');
    }
  }
};

export default templateApiService;