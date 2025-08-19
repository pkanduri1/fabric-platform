// src/types/masterQuery.ts
/**
 * Master Query Types - Banking Grade Security and Intelligence
 * Phase 2 Frontend Implementation
 */

export interface MasterQuery {
  masterQueryId: string;
  queryName: string;
  querySql: string;
  queryDescription?: string;
  queryParameters?: Record<string, any>;
  maxExecutionTimeSeconds?: number;
  maxResultRows?: number;
  securityClassification: 'INTERNAL' | 'CONFIDENTIAL' | 'RESTRICTED' | 'PUBLIC';
  dataClassification: 'SENSITIVE' | 'INTERNAL' | 'PUBLIC';
  businessJustification?: string;
  complianceTags?: string[];
  createdBy?: string;
  createdAt?: Date;
  lastModifiedBy?: string;
  lastModifiedAt?: Date;
  status?: 'ACTIVE' | 'INACTIVE' | 'DEPRECATED';
  version?: number;
}

export interface MasterQueryRequest {
  masterQueryId: string;
  queryName: string;
  querySql: string;
  queryDescription?: string;
  queryParameters?: Record<string, any>;
  maxExecutionTimeSeconds?: number;
  maxResultRows?: number;
  securityClassification: 'INTERNAL' | 'CONFIDENTIAL' | 'RESTRICTED' | 'PUBLIC';
  dataClassification: 'SENSITIVE' | 'INTERNAL' | 'PUBLIC';
  businessJustification?: string;
  complianceTags?: string[];
}

export interface MasterQueryResponse {
  masterQueryId: string;
  executionStatus: 'SUCCESS' | 'FAILED' | 'VALIDATION_FAILED' | 'TIMEOUT';
  queryName?: string;
  results?: any[];
  rowCount?: number;
  executionTimeMs?: number;
  executedBy?: string;
  userRole?: string;
  correlationId?: string;
  securityClassification?: string;
  dataClassification?: string;
  errorInfo?: Record<string, any>;
}

export interface ColumnMetadata {
  name: string;
  type: string;
  length?: number;
  precision?: number;
  scale?: number;
  nullable?: boolean;
  order: number;
  description?: string;
  dataClassification?: 'SENSITIVE' | 'INTERNAL' | 'PUBLIC';
  businessConcept?: string;
}

export interface QueryValidationResult {
  valid: boolean;
  correlationId?: string;
  message?: string;
  validatedAt?: Date;
  validatedBy?: string;
  syntaxValid?: boolean;
  businessRules?: {
    complexityCheck?: boolean;
    parameterConsistency?: boolean;
    securityClassification?: string;
  };
  errors?: string[];
  warnings?: string[];
}

export interface DatabaseSchema {
  schemaName: string;
  description?: string;
  accessLevel: 'READ_ONLY' | 'READ_WRITE' | 'ADMIN';
  tables: string[];
}

export interface SchemaInfo {
  schemas: DatabaseSchema[];
  userRole: string;
  retrievedAt: Date;
}

export interface QueryExecutionStatistics {
  period: {
    startTime: Date;
    endTime: Date;
  };
  totalQueries: number;
  successfulQueries: number;
  failedQueries: number;
  averageExecutionTime: number;
  topQueries: Array<{
    queryId: string;
    queryName: string;
    executionCount: number;
    averageTime: number;
  }>;
  errorSummary: Array<{
    errorType: string;
    count: number;
  }>;
}

export interface ConnectivityTest {
  healthy: boolean;
  responseTimeMs?: number;
  testedAt: Date;
  connectionType?: 'READ_ONLY' | 'READ_WRITE';
  maxTimeout?: number;
  maxRows?: number;
  error?: string;
}

export interface BankingFieldPattern {
  pattern: RegExp;
  fieldType: 'ACCOUNT_NUMBER' | 'ROUTING_NUMBER' | 'TRANSACTION_ID' | 'AMOUNT' | 'DATE' | 'CURRENCY' | 'CUSTOMER_ID';
  description: string;
  validation?: (value: string) => boolean;
  format?: (value: string) => string;
  maskingRequired?: boolean;
}

export interface SmartFieldMapping {
  sourceColumn: string;
  targetField: string;
  confidence: number; // 0-1 scale
  detectedPattern?: BankingFieldPattern;
  suggestedTransformation?: string;
  businessConcept?: string;
  dataClassification?: 'SENSITIVE' | 'INTERNAL' | 'PUBLIC';
  complianceRequirements?: string[];
}

export interface QueryAnalysis {
  complexity: 'LOW' | 'MEDIUM' | 'HIGH';
  estimatedRows: number;
  estimatedExecutionTime: number;
  tablesInvolved: string[];
  joinCount: number;
  hasAggregations: boolean;
  hasSubqueries: boolean;
  riskFactors: string[];
  recommendations: string[];
}

export interface MasterQueryFilter {
  searchTerm?: string;
  securityClassification?: string[];
  dataClassification?: string[];
  complianceTags?: string[];
  status?: string[];
  createdBy?: string;
  dateRange?: {
    start: Date;
    end: Date;
  };
  sortBy?: 'name' | 'created' | 'modified' | 'usage';
  sortOrder?: 'asc' | 'desc';
}

export interface QueryTemplate {
  templateId: string;
  templateName: string;
  templateSql: string;
  description: string;
  category: 'TRANSACTION' | 'CUSTOMER' | 'ACCOUNT' | 'REGULATORY' | 'RISK';
  parameters: Array<{
    name: string;
    type: 'string' | 'number' | 'date' | 'boolean';
    required: boolean;
    defaultValue?: any;
    description?: string;
  }>;
  compliance: string[];
  businessUseCase: string;
}