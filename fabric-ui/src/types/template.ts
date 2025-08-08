// src/types/template.ts
import { FieldMapping } from './configuration';

// Core field template interface
export interface FieldTemplate {
  fileType: string;
  transactionType: string;
  fieldName: string;
  targetPosition: number;
  length: number;
  dataType: 'String' | 'Number' | 'Date' | 'Boolean' | 'Decimal' | 'Integer';
  format?: string;
  required: 'Y' | 'N';
  description?: string;
  version?: number;
  enabled?: 'Y' | 'N';
  sourceField?: string;
  transformationType?: 'source' | 'constant' | 'composite' | 'conditional';
  createdBy?: string;
  createdDate?: string;
  modifiedBy?: string;
  modifiedDate?: string;
}

// Simplified file type for dropdowns and basic operations
export interface FileType {
  fileType: string;
  description: string;
  enabled: 'Y' | 'N';
  maxFields?: number;
  validationRules?: string;
  totalFields?: number;
  lastModified?: string;
  version?: number;
}

// Full file type template with fields
export interface FileTypeTemplate extends FileType {
  recordLength: number;
  fields?: FieldTemplate[];
  createdBy?: string;
  createdDate?: string;
  modifiedBy?: string;
  modifiedDate?: string;
}

// Request interfaces for creating/updating
export interface CreateFieldRequest {
  transactionType?: string;
  fieldName: string;
  targetPosition: number;
  length: number;
  dataType: FieldTemplate['dataType'];
  format?: string;
  required: 'Y' | 'N';
  description?: string;
  enabled?: 'Y' | 'N';
  createdBy?: string;
}

export interface UpdateFieldRequest {
  fieldName?: string;
  targetPosition?: number;
  length?: number;
  dataType?: FieldTemplate['dataType'];
  format?: string;
  required?: 'Y' | 'N';
  description?: string;
  enabled?: 'Y' | 'N';
  modifiedBy?: string;
}

export interface CreateFileTypeRequest {
  fileType: string;
  description: string;
  maxFields?: number;
  validationRules?: string;
  enabled?: 'Y' | 'N';
  createdBy?: string;
}

// Import/Export interfaces
export interface TemplateImportRequest {
  fileType: string;
  description: string;
  createdBy: string;
  replaceExisting: boolean;
  fields: FieldTemplate[];
}

export interface TemplateImportResult {
  success: boolean;
  fileType: string;
  fieldsImported: number;
  fieldsSkipped: number;
  fieldsUpdated?: number;
  errors: string[];
  warnings: string[];
  message: string;
  importId?: string;
  timestamp?: string;
}

export interface TemplateExportOptions {
  includeInactive?: boolean;
  includeMetadata?: boolean;
  format?: 'excel' | 'json' | 'csv';
}

// Validation interfaces
export interface TemplateValidationResult {
  isValid: boolean;
  errors: string[];
  warnings: string[];
  duplicatePositions?: number[];
  duplicateFieldNames?: string[];
  missingRequiredFields?: string[];
  conflictingFields?: string[];
  positionGaps?: number[];
  lengthOverlaps?: Array<{
    field1: string;
    field2: string;
    position1: number;
    position2: number;
  }>;
}

export interface FieldValidationRule {
  field: keyof FieldTemplate;
  rule: 'required' | 'unique' | 'min' | 'max' | 'pattern' | 'custom';
  value?: any;
  message: string;
  severity: 'error' | 'warning';
}

// Configuration generation interfaces
export interface FieldMappingConfig {
  sourceSystem: string;
  jobName: string;
  transactionType: string;
  description?: string;
  fields: FieldMapping[];
  createdDate?: string;
  createdBy?: string;
  version?: number;
  templateMetadata?: TemplateMetadata;
}

export interface TemplateMetadata {
  fileType: string;
  transactionType: string;
  templateVersion?: number;
  fieldsFromTemplate: number;
  totalFields: number;
  generatedAt: string;
  generatedBy: string;
}

export interface TemplateToConfigurationResult extends FieldMappingConfig {
  templateMetadata: TemplateMetadata;
}

// Statistics and analytics
export interface TemplateStatistics {
  totalFields: number;
  requiredFields: number;
  optionalFields: number;
  totalLength: number;
  averageFieldLength: number;
  dataTypeDistribution: Record<string, number>;
  lastModified: string;
  version: number;
  usageCount?: number;
  configurationCount?: number;
}

export interface TemplateUsageStats {
  fileType: string;
  configurationCount: number;
  lastUsed?: string;
  mostUsedFields: Array<{
    fieldName: string;
    usageCount: number;
    percentage: number;
  }>;
  averageFieldsMapped: number;
}

// History and audit trail
export interface TemplateHistoryEntry {
  version: number;
  modifiedBy: string;
  modifiedDate: string;
  changes: TemplateChange[];
  comment?: string;
  rollbackable: boolean;
}

export interface TemplateChange {
  type: 'add' | 'update' | 'delete' | 'reorder';
  field?: string;
  property?: string;
  oldValue?: any;
  newValue?: any;
  description: string;
}

// UI-specific interfaces
export interface TemplateEditSession {
  fileType: string;
  isModified: boolean;
  unsavedChanges: TemplateChange[];
  lastSaved?: string;
  autoSaveEnabled: boolean;
}

export interface FieldEditState {
  index: number;
  isEditing: boolean;
  originalData?: FieldTemplate;
  validationErrors?: string[];
  isDirty: boolean;
}

export interface TemplateFormData {
  fileType: string;
  description: string;
  maxFields: number;
  validationRules: string;
  enabled: 'Y' | 'N';
}

export interface FieldFormData {
  fieldName: string;
  targetPosition: number;
  length: number;
  dataType: FieldTemplate['dataType'];
  format: string;
  required: 'Y' | 'N';
  description: string;
  enabled: 'Y' | 'N';
}

// Sorting and filtering
export interface TemplateSortOptions {
  field: keyof FieldTemplate;
  direction: 'asc' | 'desc';
}

export interface TemplateFilterOptions {
  showInactive?: boolean;
  dataTypes?: string[];
  required?: 'Y' | 'N' | 'all';
  searchTerm?: string;
  positionRange?: {
    min: number;
    max: number;
  };
}

// Bulk operations
export interface BulkFieldOperation {
  type: 'update' | 'delete' | 'enable' | 'disable' | 'reorder';
  fieldNames: string[];
  updates?: Partial<FieldTemplate>;
  newPositions?: Record<string, number>;
}

export interface BulkOperationResult {
  success: boolean;
  processedCount: number;
  failedCount: number;
  errors: Array<{
    fieldName: string;
    error: string;
  }>;
  warnings: string[];
}

// Template comparison
export interface TemplateComparison {
  fileType1: string;
  fileType2: string;
  differences: TemplateDifference[];
  similarity: number;
  recommendations: string[];
}

export interface TemplateDifference {
  type: 'missing' | 'extra' | 'different';
  field: string;
  property?: string;
  value1?: any;
  value2?: any;
  severity: 'high' | 'medium' | 'low';
}

// API response wrappers
export interface ApiResponse<T> {
  data: T;
  success: boolean;
  message?: string;
  timestamp: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  size: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// Error handling
export interface TemplateError {
  code: string;
  message: string;
  details?: Record<string, any>;
  timestamp: string;
  requestId?: string;
}

// Constants for validation
export const DATA_TYPES = [
  'String',
  'Number', 
  'Date',
  'Boolean',
  'Decimal',
  'Integer'
] as const;

export const REQUIRED_OPTIONS = [
  { value: 'Y', label: 'Required' },
  { value: 'N', label: 'Optional' }
] as const;

export const ENABLED_OPTIONS = [
  { value: 'Y', label: 'Active' },
  { value: 'N', label: 'Inactive' }
] as const;

// Type guards
export const isFieldTemplate = (obj: any): obj is FieldTemplate => {
  return obj && 
    typeof obj.fieldName === 'string' &&
    typeof obj.targetPosition === 'number' &&
    typeof obj.length === 'number' &&
    typeof obj.dataType === 'string' &&
    ['Y', 'N'].includes(obj.required);
};

export const isFileType = (obj: any): obj is FileType => {
  return obj &&
    typeof obj.fileType === 'string' &&
    typeof obj.description === 'string' &&
    ['Y', 'N'].includes(obj.enabled);
};

// Utility types
export type TemplateField = keyof FieldTemplate;
export type SortableTemplateField = Extract<TemplateField, 'fieldName' | 'targetPosition' | 'length' | 'dataType' | 'required'>;
export type EditableTemplateField = Exclude<TemplateField, 'createdBy' | 'createdDate' | 'version'>;

export default {
  DATA_TYPES,
  REQUIRED_OPTIONS,
  ENABLED_OPTIONS,
  isFieldTemplate,
  isFileType
};