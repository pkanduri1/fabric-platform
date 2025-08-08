// Core type definitions for the batch configuration tool

// Base types - keep simple string types for flexibility
export type TransactionType = string;
export type DataType = string;
export type TransformationType = 'constant' | 'source' | 'composite' | 'conditional' | 'blank';
export type PadType = 'left' | 'right';
export type ValidationSeverity = 'error' | 'warning' | 'info';

// Type definition interfaces for dynamic configuration
export interface TransactionTypeDefinition {
  code: string;
  name: string;
  description?: string;
  isDefault?: boolean;
  sortOrder?: number;
}

export interface FileTypeDefinition {
  code: string;
  name: string;
  description?: string;
  extension?: string;
  template?: string;
  sortOrder?: number;
}

export interface SourceSystemTypeDefinition {
  code: string;
  name: string;
  description?: string;
  defaultSettings?: Record<string, any>;
  sortOrder?: number;
}

export interface DataTypeDefinition {
  code: string;
  name: string;
  description?: string;
  validationPattern?: string;
  defaultLength?: number;
  allowsLength?: boolean;
  sortOrder?: number;
}

// Registry interface for managing type definitions
export interface TypeRegistry {
  transactionTypes: TransactionTypeDefinition[];
  fileTypes: FileTypeDefinition[];
  sourceSystemTypes: SourceSystemTypeDefinition[];
  dataTypes: DataTypeDefinition[];
  transformationTypes: TransformationTypeDefinition[];
}

export interface TransformationTypeDefinition {
  code: TransformationType;
  name: string;
  description?: string;
  requiresValue?: boolean;
  requiresSourceField?: boolean;
  requiresConditions?: boolean;
  allowsFormat?: boolean;
  sortOrder?: number;
}

export interface FieldMapping {
  required: boolean;
  expression: any;
  id?: string; // Added for React list keys
  fieldName: string;
  sourceField?: string;
  targetField: string;
  targetPosition: number;
  length: number;
  dataType: DataType;
  transformationType: TransformationType;
  defaultValue?: string;
  pad?: PadType;
  padChar?: string;
  format?: string;
  //template integration
  isFromTemplate?: boolean;
  templateFieldName?: string;

  sourceFormat?: string;
  targetFormat?: string;
  value?: string; // For constant transformations
  
  // Conditional logic
  conditions?: Condition[];
  
  // Composite field sources
  sources?: CompositeSource[];
  transform?: string; // For composite: 'sum' | 'concat'
  delimiter?: string; // For composite concat
  
  // Transaction type for multi-transaction configs
  transactionType?: TransactionType;
}

export interface FieldMappingConfig {
  sourceSystem: string;
  jobName: string;
  transactionType: string;
  description?: string;
  fields: FieldMapping[];
  createdDate?: string;
  createdBy?: string;
  version?: number;
}

export interface JobConfigResponse {
  id: string;
  sourceSystemId: string;
  jobName: string;
  description: string;
  inputPath: string;
  outputPath: string;
  querySql: string;
  enabled: boolean;
  created: string;
  transactionTypes: string[];
}

export interface Condition {
  ifExpr: string;
  then?: string;
  elseExpr?: string;
  elseIfExprs?: Condition[];
}

export interface CompositeSource {
  field: string;
  form?: string;
}

export interface Configuration {
  fileType: string;
  transactionType: TransactionType;
  sourceSystem: string;
  jobName: string;
  fields?: Record<string, FieldMapping>; // Made optional for compatibility
  fieldMappings: FieldMapping[]; // Added for hook compatibility

  // Add template support
  templateSource?: {
    fileType: string;
    transactionType: string;
    isTemplateGenerated: boolean;
  };

  currentTransactionType?: TransactionType; // For UI state
  
  // Metadata about available types for this configuration
  availableTransactionTypes?: TransactionType[];
  supportedFileTypes?: string[];
}

export interface ValidationError {
  fieldName: string;
  errorType: string;
  message: string;
  severity: ValidationSeverity;
}

export interface ValidationResult {
  isValid: boolean;
  valid?: boolean; // Keep for API compatibility
  message?: string;
  warnings: ValidationError[];
  errors: ValidationError[];
  summary?: {
    totalFields: number;
    recordLength: number;
    sourceFieldsUsed: number;
    constantFields: number;
    transactionTypes: number;
  };
}

export interface SourceSystem {
  id: string;
  name: string;
  description: string;
  systemType: string; // References SourceSystemTypeDefinition.code
  type?: string; // Add for API response
  jobs: JobConfig[]; // Changed from string[] to JobConfig[]
   jobCount?: number; // Add this property
  enabled?: boolean; // Add this property
  lastModified?: string; // Add this property
  connectionProperties?: any; // Add this property
  inputBasePath?: string;
  outputBasePath?: string;
  
  // Dynamic configuration based on system type
  settings?: Record<string, any>;
  supportedFileTypes?: string[];
  supportedTransactionTypes?: TransactionType[];
}

export interface SourceField {
  name: string;
  dataType: DataType;
  maxLength?: number;
  nullable: boolean;
  description?: string;
}

export interface JobConfig {
  name: string; // Added for job identification
  sourceSystem: string;
  jobName: string;
  description?: string; // Added for UI display
  files: FileConfig[];
  multiTxn?: boolean;
  
  // Job-specific type constraints
  supportedTransactionTypes?: TransactionType[];
  defaultFileType?: string;
}

export interface FileConfig {
  inputPath?: string;
  transactionTypes?: TransactionType[];
  template?: string;
  target?: string;
  params?: Record<string, string>;
  sourceSystem?: string;
  jobName?: string;
  transactionType?: TransactionType;
}

// API Response types
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  errors?: string[];
}

// UI State types
export interface ConfigurationState {
  currentConfiguration: Configuration | null;
  sourceFields: SourceField[];
  validationResult: ValidationResult | null;
  isLoading: boolean;
  isDirty: boolean;
  typeRegistry?: TypeRegistry; // Add registry to state
}

export interface DropResult {
  source: {
    droppableId: string;
    index: number;
  };
  destination?: {
    droppableId: string;
    index: number;
  } | null;
  draggableId: string;
}

// CRUD operations for type management
export interface TypeManagementOperations {
  // Transaction Types
  getTransactionTypes: () => Promise<TransactionTypeDefinition[]>;
  addTransactionType: (type: Omit<TransactionTypeDefinition, 'code'> & { code?: string }) => Promise<TransactionTypeDefinition>;
  updateTransactionType: (code: string, updates: Partial<TransactionTypeDefinition>) => Promise<TransactionTypeDefinition>;
  deleteTransactionType: (code: string) => Promise<boolean>;
  
  // File Types
  getFileTypes: () => Promise<FileTypeDefinition[]>;
  addFileType: (type: Omit<FileTypeDefinition, 'code'> & { code?: string }) => Promise<FileTypeDefinition>;
  updateFileType: (code: string, updates: Partial<FileTypeDefinition>) => Promise<FileTypeDefinition>;
  deleteFileType: (code: string) => Promise<boolean>;
  
  // Source System Types
  getSourceSystemTypes: () => Promise<SourceSystemTypeDefinition[]>;
  addSourceSystemType: (type: Omit<SourceSystemTypeDefinition, 'code'> & { code?: string }) => Promise<SourceSystemTypeDefinition>;
  updateSourceSystemType: (code: string, updates: Partial<SourceSystemTypeDefinition>) => Promise<SourceSystemTypeDefinition>;
  deleteSourceSystemType: (code: string) => Promise<boolean>;
  
  // Data Types
  getDataTypes: () => Promise<DataTypeDefinition[]>;
  addDataType: (type: Omit<DataTypeDefinition, 'code'> & { code?: string }) => Promise<DataTypeDefinition>;
  updateDataType: (code: string, updates: Partial<DataTypeDefinition>) => Promise<DataTypeDefinition>;
  deleteDataType: (code: string) => Promise<boolean>;
  
  // Get complete registry
  getTypeRegistry: () => Promise<TypeRegistry>;
}