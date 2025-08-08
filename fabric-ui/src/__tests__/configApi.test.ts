jest.mock('axios', () => ({
  get: jest.fn(() => Promise.resolve({ data: {} })),
  post: jest.fn(() => Promise.resolve({ data: {} })),
  defaults: { headers: { common: {} } },
  interceptors: {
    request: { use: jest.fn() },
    response: { use: jest.fn() }
  }
}));


import axios from 'axios';
import { configApi, apiUtils, testConnection } from '../services/api/configApi';
import { Configuration, SourceSystem, ValidationResult } from '../types/configuration';

// Mock axios
jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('configApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getSourceSystems', () => {
    it('should fetch source systems successfully', async () => {
      const mockSystems: SourceSystem[] = [
        { id: 'hr', name: 'HR System', description: 'Human Resources', jobs: ['p327'] }
      ];
      mockedAxios.get.mockResolvedValue({ data: mockSystems });

      const result = await configApi.getSourceSystems();

      expect(mockedAxios.get).toHaveBeenCalledWith('/api/ui/source-systems');
      expect(result).toEqual(mockSystems);
    });

    it('should handle errors when fetching source systems', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Network error'));

      await expect(configApi.getSourceSystems()).rejects.toThrow('Failed to load source systems');
    });
  });

  describe('getJobsForSourceSystem', () => {
    it('should fetch jobs for source system successfully', async () => {
      const mockJobs = ['p327', 'atoctran'];
      mockedAxios.get.mockResolvedValue({ data: mockJobs });

      const result = await configApi.getJobsForSourceSystem('hr');

      expect(mockedAxios.get).toHaveBeenCalledWith('/api/ui/source-systems/hr/jobs');
      expect(result).toEqual(mockJobs);
    });

    it('should handle errors when fetching jobs', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Not found'));

      await expect(configApi.getJobsForSourceSystem('hr')).rejects.toThrow('Failed to load jobs for hr');
    });
  });

  describe('getFieldMappings', () => {
    it('should fetch field mappings successfully', async () => {
      const mockMappings: Configuration[] = [{
        fileType: 'p327',
        transactionType: 'default',
        sourceSystem: 'hr',
        jobName: 'p327',
        fields: {}
      }];
      mockedAxios.get.mockResolvedValue({ data: mockMappings });

      const result = await configApi.getFieldMappings('hr', 'p327');

      expect(mockedAxios.get).toHaveBeenCalledWith('/api/ui/mappings/hr/p327');
      expect(result).toEqual(mockMappings);
    });

    it('should handle errors when fetching mappings', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Server error'));

      await expect(configApi.getFieldMappings('hr', 'p327')).rejects.toThrow('Failed to load mappings for hr.p327');
    });
  });

  describe('getSpecificMapping', () => {
    it('should fetch specific mapping successfully', async () => {
      const mockMapping: Configuration = {
        fileType: 'p327',
        transactionType: '200',
        sourceSystem: 'hr',
        jobName: 'p327',
        fields: {}
      };
      mockedAxios.get.mockResolvedValue({ data: mockMapping });

      const result = await configApi.getSpecificMapping('hr', 'p327', '200');

      expect(mockedAxios.get).toHaveBeenCalledWith('/api/ui/mappings/hr/p327/200');
      expect(result).toEqual(mockMapping);
    });
  });

  describe('saveConfiguration', () => {
    it('should save configuration successfully', async () => {
      const mockConfig: Configuration = {
        fileType: 'p327',
        transactionType: 'default',
        sourceSystem: 'hr',
        jobName: 'p327',
        fields: {}
      };
      const mockResponse = { success: true, data: mockConfig };
      mockedAxios.post.mockResolvedValue({ data: mockResponse });

      const result = await configApi.saveConfiguration(mockConfig);

      expect(mockedAxios.post).toHaveBeenCalledWith('/api/ui/mappings/save', mockConfig);
      expect(result).toEqual(mockResponse);
    });

    it('should handle save errors', async () => {
      const mockConfig: Configuration = {
        fileType: 'p327',
        transactionType: 'default',
        sourceSystem: 'hr',
        jobName: 'p327',
        fields: {}
      };
      mockedAxios.post.mockRejectedValue(new Error('Save failed'));

      await expect(configApi.saveConfiguration(mockConfig)).rejects.toThrow('Failed to save configuration');
    });
  });

  describe('validateMapping', () => {
    it('should validate mapping successfully', async () => {
      const mockValidation: ValidationResult = {
        valid: true,
        warnings: [],
        errors: []
      };
      mockedAxios.post.mockResolvedValue({ data: mockValidation });

      const mockConfig: Configuration = {
        fileType: 'p327',
        transactionType: 'default',
        sourceSystem: 'hr',
        jobName: 'p327',
        fields: {}
      };

      const result = await configApi.validateMapping(mockConfig);

      expect(mockedAxios.post).toHaveBeenCalledWith('/api/ui/mappings/validate', mockConfig);
      expect(result).toEqual(mockValidation);
    });

    it('should handle validation errors', async () => {
      mockedAxios.post.mockRejectedValue(new Error('Validation failed'));

      const mockConfig: Configuration = {
        fileType: 'p327',
        transactionType: 'default',
        sourceSystem: 'hr',
        jobName: 'p327',
        fields: {}
      };

      await expect(configApi.validateMapping(mockConfig)).rejects.toThrow('Failed to validate mapping configuration');
    });
  });

  describe('generateYaml', () => {
    it('should generate YAML successfully', async () => {
      const mockYaml = { yamlContent: 'fileType: p327\nfields: {}' };
      mockedAxios.post.mockResolvedValue({ data: mockYaml });

      const mockConfig: Configuration = {
        fileType: 'p327',
        transactionType: 'default',
        sourceSystem: 'hr',
        jobName: 'p327',
        fields: {}
      };

      const result = await configApi.generateYaml(mockConfig);

      expect(mockedAxios.post).toHaveBeenCalledWith('/api/ui/mappings/generate-yaml', mockConfig);
      expect(result).toEqual(mockYaml);
    });
  });

  describe('getSourceFields', () => {
    it('should fetch source fields successfully', async () => {
      const mockFields = [
        { name: 'acct_num', dataType: 'string', nullable: false }
      ];
      mockedAxios.get.mockResolvedValue({ data: mockFields });

      const result = await configApi.getSourceFields('hr');

      expect(mockedAxios.get).toHaveBeenCalledWith('/api/ui/source-systems/hr/fields');
      expect(result).toEqual(mockFields);
    });
  });

  describe('previewOutput', () => {
    it('should generate output preview successfully', async () => {
      const mockPreview = { preview: ['100020000000012345678', '100020000000087654321'] };
      mockedAxios.post.mockResolvedValue({ data: mockPreview });

      const mockConfig: Configuration = {
        fileType: 'p327',
        transactionType: 'default',
        sourceSystem: 'hr',
        jobName: 'p327',
        fields: {}
      };

      const result = await configApi.previewOutput(mockConfig);

      expect(mockedAxios.post).toHaveBeenCalledWith('/api/ui/mappings/preview', {
        mapping: mockConfig,
        sampleData: undefined
      });
      expect(result).toEqual(mockPreview);
    });
  });

  describe('testConfiguration', () => {
    it('should test configuration successfully', async () => {
      const mockResult = { success: true, message: 'Configuration test passed' };
      mockedAxios.post.mockResolvedValue({ data: mockResult });

      const result = await configApi.testConfiguration('hr', 'p327');

      expect(mockedAxios.post).toHaveBeenCalledWith('/api/ui/test/hr/p327');
      expect(result).toEqual(mockResult);
    });
  });
});

describe('apiUtils', () => {
  describe('formatError', () => {
    it('should format error with response message', () => {
      const error = {
        response: {
          data: {
            message: 'Validation failed'
          }
        }
      };

      const result = apiUtils.formatError(error);
      expect(result).toBe('Validation failed');
    });

    it('should format error with error message', () => {
      const error = {
        message: 'Network error'
      };

      const result = apiUtils.formatError(error);
      expect(result).toBe('Network error');
    });

    it('should provide default message for unknown error', () => {
      const error = {};

      const result = apiUtils.formatError(error);
      expect(result).toBe('An unexpected error occurred');
    });
  });

  describe('isNetworkError', () => {
    it('should identify network errors', () => {
      const networkError = { code: 'NETWORK_ERROR' };
      expect(apiUtils.isNetworkError(networkError)).toBe(true);

      const noResponseError = {};
      expect(apiUtils.isNetworkError(noResponseError)).toBe(true);
    });

    it('should identify non-network errors', () => {
      const httpError = { response: { status: 404 } };
      expect(apiUtils.isNetworkError(httpError)).toBe(false);
    });
  });

  describe('retryWithBackoff', () => {
    it('should succeed on first try', async () => {
      const successFn = jest.fn().mockResolvedValue('success');

      const result = await apiUtils.retryWithBackoff(successFn);

      expect(result).toBe('success');
      expect(successFn).toHaveBeenCalledTimes(1);
    });

    it('should retry on failure and eventually succeed', async () => {
      const retryFn = jest.fn()
        .mockRejectedValueOnce(new Error('fail 1'))
        .mockRejectedValueOnce(new Error('fail 2'))
        .mockResolvedValue('success');

      const result = await apiUtils.retryWithBackoff(retryFn, 3, 10);

      expect(result).toBe('success');
      expect(retryFn).toHaveBeenCalledTimes(3);
    });

    it('should throw error after max retries', async () => {
      const failFn = jest.fn().mockRejectedValue(new Error('always fails'));

      await expect(apiUtils.retryWithBackoff(failFn, 2, 10)).rejects.toThrow('always fails');
      expect(failFn).toHaveBeenCalledTimes(2);
    });
  });
});

describe('testConnection', () => {
  it('should return success when connection works', async () => {
    mockedAxios.get.mockResolvedValue({ data: [] });

    const result = await testConnection();

    expect(result).toEqual({ success: true, message: 'API connection successful' });
  });

  describe('testConnection', () => {
  it('should return failure when connection fails', async () => {
    // Suppress console.error for this test
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
    
    mockedAxios.get.mockRejectedValue(new Error('Connection failed'));

    const result = await testConnection();

    expect(result).toEqual({ success: false, message: 'Connection failed' });
    
    // Restore console.error
    consoleSpy.mockRestore();
  });
});

  it('should return failure when connection fails', async () => {
    mockedAxios.get.mockRejectedValue(new Error('Connection failed'));

    const result = await testConnection();

    expect(result).toEqual({ success: false, message: 'Connection failed' });
  });
});