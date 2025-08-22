/**
 * Master Query CRUD API Service - Banking Grade Operations
 * 
 * Enterprise-grade API service for master query create, read, update, delete operations
 * with comprehensive error handling, type safety, and banking compliance features.
 * 
 * Features:
 * - Full CRUD operations with validation
 * - Template management and retrieval
 * - Real-time SQL validation
 * - Audit trail integration
 * - Error handling with correlation IDs
 * - Type-safe interfaces and responses
 * 
 * Security:
 * - JWT token authentication
 * - Role-based authorization checks
 * - Request/response validation
 * - Secure error messages
 * - Banking compliance enforcement
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Master Query CRUD Implementation
 */

import { ApiResponse, createApiService } from './apiClient';

// Master Query interfaces
export interface MasterQueryCreateRequest {
  sourceSystem: string;
  queryName: string;
  description?: string;
  queryType: 'SELECT' | 'WITH';
  querySql: string;
  dataClassification?: 'PUBLIC' | 'INTERNAL' | 'SENSITIVE' | 'CONFIDENTIAL';
  securityClassification?: 'PUBLIC' | 'INTERNAL' | 'CONFIDENTIAL' | 'RESTRICTED';
  businessJustification: string;
  complianceTags?: string[];
  expectedParameters?: QueryParameterMetadata[];
  templateCategory?: string;
  templateName?: string;
  metadata?: Record<string, any>;
}

export interface MasterQueryUpdateRequest {
  id: number;
  currentVersion: number;
  sourceSystem?: string;
  queryName?: string;
  description?: string;
  queryType?: 'SELECT' | 'WITH';
  querySql?: string;
  isActive?: 'Y' | 'N';
  dataClassification?: 'PUBLIC' | 'INTERNAL' | 'SENSITIVE' | 'CONFIDENTIAL';
  securityClassification?: 'PUBLIC' | 'INTERNAL' | 'CONFIDENTIAL' | 'RESTRICTED';
  changeJustification: string;
  changeSummary?: string;
  complianceTags?: string[];
  expectedParameters?: QueryParameterMetadata[];
  metadata?: Record<string, any>;
  createNewVersion?: boolean;
  preserveOldVersion?: boolean;
}

export interface QueryParameterMetadata {
  name: string;
  type: string;
  required?: boolean;
  description?: string;
  defaultValue?: string;
  validationPattern?: string;
}

export interface MasterQueryConfigResponse {
  id: number;
  sourceSystem: string;
  queryName: string;
  queryType: string;
  querySql: string;
  version: number;
  isActive: string;
  createdBy: string;
  createdDate: string;
  modifiedBy?: string;
  modifiedDate?: string;
  displayName: string;
  dataClassification: string;
  securityClassification?: string;
  statusIndicator: string;
  complexityLevel: string;
  parameterCount: number;
  complianceRequirements: string[];
  description?: string;
}

export interface ValidationResult {
  valid: boolean;
  securityRisk: 'MINIMAL' | 'LOW' | 'MEDIUM' | 'HIGH';
  errors: ValidationError[];
  warnings: ValidationWarning[];
  checks: ValidationCheck[];
  parameterCount?: number;
  complexityLevel?: string;
  correlationId: string;
  validatedAt: string;
  validatedBy: string;
}

export interface ValidationError {
  type: string;
  message: string;
}

export interface ValidationWarning {
  type: string;
  message: string;
}

export interface ValidationCheck {
  type: string;
  message: string;
}

export interface SqlValidationRequest {
  querySql: string;
  queryType?: string;
  sourceSystem?: string;
}

export interface QueryTemplate {
  name: string;
  sql: string;
  parameters: string[];
  description: string;
}

export interface TemplateCategory {
  name: string;
  description: string;
  templates: QueryTemplate[];
}

export interface TemplatesResponse {
  categories: TemplateCategory[];
  totalTemplates: number;
  lastUpdated: string;
  version: string;
}

export interface DeleteResult {
  deleted: boolean;
  id: number;
  queryName: string;
  sourceSystem: string;
  deletedBy: string;
  deletedAt: string;
  correlationId: string;
  auditTrail: string;
}

// API Service Class
class MasterQueryCrudApiService {
  private baseUrl = '/api/v2/master-query';

  /**
   * Create a new master query configuration
   */
  async createMasterQuery(request: MasterQueryCreateRequest): Promise<MasterQueryConfigResponse> {
    const apiService = createApiService();
    const response = await apiService.post<MasterQueryConfigResponse>(
      this.baseUrl,
      request
    );
    return response.data;
  }

  /**
   * Update an existing master query configuration
   */
  async updateMasterQuery(request: MasterQueryUpdateRequest): Promise<MasterQueryConfigResponse> {
    const apiService = createApiService();
    const response = await apiService.put<MasterQueryConfigResponse>(
      `${this.baseUrl}/${request.id}`,
      request
    );
    return response.data;
  }

  /**
   * Soft delete a master query configuration
   */
  async deleteMasterQuery(id: number, deleteJustification: string): Promise<DeleteResult> {
    const apiService = createApiService();
    const response = await apiService.delete<DeleteResult>(
      `${this.baseUrl}/${id}`,
      {
        params: { deleteJustification }
      }
    );
    return response.data;
  }

  /**
   * Get a specific master query by ID
   */
  async getMasterQueryById(id: number): Promise<MasterQueryConfigResponse> {
    const apiService = createApiService();
    const response = await apiService.get<MasterQueryConfigResponse>(
      `${this.baseUrl}/${id}`
    );
    return response.data;
  }

  /**
   * Get all master query configurations
   */
  async getAllMasterQueries(): Promise<MasterQueryConfigResponse[]> {
    const apiService = createApiService();
    const response = await apiService.get<MasterQueryConfigResponse[]>(
      this.baseUrl
    );
    return response.data;
  }

  /**
   * Validate SQL query without creating or updating
   */
  async validateSqlQuery(request: SqlValidationRequest): Promise<ValidationResult> {
    const apiService = createApiService();
    const response = await apiService.post<ValidationResult>(
      `${this.baseUrl}/validate-sql`,
      request
    );
    return response.data;
  }

  /**
   * Get available query templates
   */
  async getQueryTemplates(): Promise<TemplatesResponse> {
    const apiService = createApiService();
    const response = await apiService.get<TemplatesResponse>(
      `${this.baseUrl}/templates`
    );
    return response.data;
  }

  /**
   * Test database connectivity
   */
  async testConnectivity(): Promise<Record<string, any>> {
    const apiService = createApiService();
    const response = await apiService.get<Record<string, any>>(
      `${this.baseUrl}/connectivity/test`
    );
    return response.data;
  }

  /**
   * Get execution statistics (admin only)
   */
  async getExecutionStatistics(startTime: string, endTime: string): Promise<Record<string, any>> {
    const apiService = createApiService();
    const response = await apiService.get<Record<string, any>>(
      `${this.baseUrl}/statistics`,
      {
        params: { startTime, endTime }
      }
    );
    return response.data;
  }

  /**
   * Get available database schemas
   */
  async getAvailableSchemas(): Promise<Record<string, any>> {
    const apiService = createApiService();
    const response = await apiService.get<Record<string, any>>(
      `${this.baseUrl}/schemas`
    );
    return response.data;
  }

  /**
   * Get banking field patterns for smart mapping
   */
  async getBankingFieldPatterns(): Promise<Record<string, any>> {
    const apiService = createApiService();
    const response = await apiService.get<Record<string, any>>(
      `${this.baseUrl}/patterns/banking`
    );
    return response.data;
  }

  /**
   * Generate smart field mappings for a master query
   */
  async generateSmartFieldMappings(
    masterQueryId: string, 
    targetSchema?: string
  ): Promise<any[]> {
    const apiService = createApiService();
    const response = await apiService.post<any[]>(
      `${this.baseUrl}/${masterQueryId}/smart-mapping`,
      { targetSchema }
    );
    return response.data;
  }

  /**
   * Validate field mappings
   */
  async validateFieldMappings(
    masterQueryId: string, 
    mappings: any[]
  ): Promise<any[]> {
    const apiService = createApiService();
    const response = await apiService.post<any[]>(
      `${this.baseUrl}/${masterQueryId}/validate-mappings`,
      { mappings }
    );
    return response.data;
  }

  /**
   * Get health status of master query service
   */
  async getHealthStatus(): Promise<Record<string, any>> {
    const apiService = createApiService();
    const response = await apiService.get<Record<string, any>>(
      `${this.baseUrl}/health`
    );
    return response.data;
  }
}

// Export singleton instance
export const masterQueryCrudApi = new MasterQueryCrudApiService();

// Named exports for convenience
export const {
  createMasterQuery,
  updateMasterQuery,
  deleteMasterQuery,
  getMasterQueryById,
  getAllMasterQueries,
  validateSqlQuery,
  getQueryTemplates,
  testConnectivity,
  getExecutionStatistics,
  getAvailableSchemas,
  getBankingFieldPatterns,
  generateSmartFieldMappings,
  validateFieldMappings,
  getHealthStatus
} = masterQueryCrudApi;

export default masterQueryCrudApi;