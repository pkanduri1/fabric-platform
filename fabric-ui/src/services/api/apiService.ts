// src/services/apiService.ts
import axios from 'axios';
import { Configuration, ValidationResult, SourceSystem } from '../../types/index';

const API_BASE_URL = '/api/ui';

export const getSourceSystems = async (): Promise<SourceSystem[]> => {
  const response = await axios.get(`${API_BASE_URL}/source-systems`);
  return response.data;
};

export const getFieldMappings = async (sourceSystem: string, jobName: string): Promise<Configuration[]> => {
  const response = await axios.get(`${API_BASE_URL}/mappings/${sourceSystem}/${jobName}`);
  return response.data;
};

export const validateMapping = async (mapping: Configuration): Promise<ValidationResult> => {
  const response = await axios.post(`${API_BASE_URL}/mappings/validate`, mapping);
  return response.data;
};