// Core type definitions for the batch configuration tool

export interface FieldMapping {
  fieldName: string;
  sourceField?: string;
  targetField: string;
  targetPosition: number;
  length: number;
  dataType: 'string' | 'numeric' | 'date';
  transformationType: 'constant' | 'source' | 'composite' | 'conditional';
  defaultValue?: string;
  pad?: 'left' | 'right';
  padChar?: string;
  format?: string;
}

export interface Configuration {
  sourceSystem: string;
  jobType: string;
  transactionType: string;
  fields: FieldMapping[];
}

export interface ValidationResult {
  isValid: boolean;
  warnings: string[];
  errors: string[];
  summary?: {
    totalFields: number;
    recordLength: number;
    sourceFieldsUsed: number;
    constantFields: number;
  };
}

export interface SourceSystem {
  id: string;
  name: string;
  description: string;
  jobTypes: string[];
}

export interface SourceField {
  name: string;
  dataType: string;
  maxLength?: number;
  nullable: boolean;
  description?: string;
}

export * from './template';