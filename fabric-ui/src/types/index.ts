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

// Export everything from template except PaginatedResponse (which is defined in monitoring)
export type {
  FieldTemplate,
  FileType,
  FileTypeTemplate,
  CreateFieldRequest,
  UpdateFieldRequest,
  CreateFileTypeRequest,
  TemplateImportRequest,
  TemplateImportResult,
  TemplateExportOptions,
  TemplateValidationResult,
  FieldValidationRule,
  FieldMappingConfig,
  TemplateMetadata,
  TemplateToConfigurationResult,
  TemplateStatistics,
  TemplateUsageStats,
  TemplateHistoryEntry,
  TemplateChange,
  TemplateEditSession,
  FieldEditState,
  TemplateFormData,
  FieldFormData,
  TemplateSortOptions,
  TemplateFilterOptions,
  BulkFieldOperation,
  BulkOperationResult,
  TemplateComparison,
  TemplateDifference,
  ApiResponse,
  TemplateError
} from './template';

// Export constants and functions (not types)
export {
  DATA_TYPES,
  REQUIRED_OPTIONS,
  ENABLED_OPTIONS,
  isFieldTemplate,
  isFileType
} from './template';

export * from './monitoring';