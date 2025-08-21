// src/services/api/masterQueryApi.ts

import { httpClient } from '../httpClient';
import {
  MasterQuery,
  MasterQueryRequest,
  MasterQueryResponse,
  ColumnMetadata,
  QueryValidationResult,
  SchemaInfo,
  QueryExecutionStatistics,
  ConnectivityTest,
  SmartFieldMapping,
  QueryAnalysis,
  MasterQueryFilter,
  QueryTemplate
} from '../../types/masterQuery';

/**
 * Master Query API Service
 * 
 * Comprehensive API service for master query management with banking-grade
 * security, JWT authentication, and enterprise compliance features.
 * 
 * Features:
 * - JWT-authenticated API calls
 * - Real-time query execution with security validation
 * - Smart field mapping with AI-powered suggestions
 * - Comprehensive query analysis and validation
 * - SOX-compliant audit logging
 * - Banking-specific data classification
 * - Performance monitoring and statistics
 * 
 * Security:
 * - All operations require valid JWT tokens
 * - Role-based access control enforcement
 * - Data classification and PII protection
 * - Query validation and SQL injection prevention
 * - Correlation ID tracking for audit trails
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0.0
 * @since 2025-08-20
 */

const API_BASE_URL = '/api/v2/master-query';

// =========================================================================
// CORE MASTER QUERY OPERATIONS
// =========================================================================

/**
 * Retrieve all master queries with optional filtering
 */
export const getMasterQueries = async (filter?: MasterQueryFilter): Promise<MasterQuery[]> => {
  try {
    const params = new URLSearchParams();
    
    if (filter) {
      if (filter.searchTerm) params.append('search', filter.searchTerm);
      if (filter.securityClassification) {
        filter.securityClassification.forEach(sc => params.append('securityClassification', sc));
      }
      if (filter.dataClassification) {
        filter.dataClassification.forEach(dc => params.append('dataClassification', dc));
      }
      if (filter.status) {
        filter.status.forEach(s => params.append('status', s));
      }
      if (filter.sortBy) params.append('sortBy', filter.sortBy);
      if (filter.sortOrder) params.append('sortOrder', filter.sortOrder);
    }
    
    const response = await httpClient.get(`${API_BASE_URL}?${params.toString()}`);
    return response.data;
  } catch (error) {
    console.error('Failed to fetch master queries:', error);
    throw error;
  }
};

/**
 * Retrieve a specific master query by ID
 */
export const getMasterQuery = async (masterQueryId: string): Promise<MasterQuery> => {
  try {
    const response = await httpClient.get(`${API_BASE_URL}/${masterQueryId}`);
    return response.data;
  } catch (error) {
    console.error(`Failed to fetch master query ${masterQueryId}:`, error);
    throw error;
  }
};

/**
 * Create a new master query with security validation
 */
export const createMasterQuery = async (request: MasterQueryRequest): Promise<MasterQuery> => {
  try {
    const response = await httpClient.post(API_BASE_URL, request);
    return response.data;
  } catch (error) {
    console.error('Failed to create master query:', error);
    throw error;
  }
};

/**
 * Update an existing master query
 */
export const updateMasterQuery = async (
  masterQueryId: string, 
  request: MasterQueryRequest
): Promise<MasterQuery> => {
  try {
    const response = await httpClient.put(`${API_BASE_URL}/${masterQueryId}`, request);
    return response.data;
  } catch (error) {
    console.error(`Failed to update master query ${masterQueryId}:`, error);
    throw error;
  }
};

/**
 * Delete a master query (soft delete for audit compliance)
 */
export const deleteMasterQuery = async (masterQueryId: string): Promise<void> => {
  try {
    await httpClient.delete(`${API_BASE_URL}/${masterQueryId}`);
  } catch (error) {
    console.error(`Failed to delete master query ${masterQueryId}:`, error);
    throw error;
  }
};

// =========================================================================
// QUERY EXECUTION AND VALIDATION
// =========================================================================

/**
 * Execute a master query with parameters and security validation
 */
export const executeMasterQuery = async (
  masterQueryId: string,
  parameters?: Record<string, any>
): Promise<MasterQueryResponse> => {
  try {
    const response = await httpClient.post(`${API_BASE_URL}/${masterQueryId}/execute`, {
      parameters: parameters || {}
    });
    return response.data;
  } catch (error) {
    console.error(`Failed to execute master query ${masterQueryId}:`, error);
    throw error;
  }
};

/**
 * Validate a master query SQL without execution
 */
export const validateMasterQuery = async (
  masterQueryId: string,
  querySql?: string
): Promise<QueryValidationResult> => {
  try {
    const payload = querySql ? { querySql } : {};
    const response = await httpClient.post(`${API_BASE_URL}/${masterQueryId}/validate`, payload);
    return response.data;
  } catch (error) {
    console.error(`Failed to validate master query ${masterQueryId}:`, error);
    throw error;
  }
};

/**
 * Test query execution with limited results
 */
export const testMasterQuery = async (
  masterQueryId: string,
  parameters?: Record<string, any>,
  maxRows: number = 10
): Promise<MasterQueryResponse> => {
  try {
    const response = await httpClient.post(`${API_BASE_URL}/${masterQueryId}/test`, {
      parameters: parameters || {},
      maxRows
    });
    return response.data;
  } catch (error) {
    console.error(`Failed to test master query ${masterQueryId}:`, error);
    throw error;
  }
};

// =========================================================================
// METADATA AND COLUMN ANALYSIS
// =========================================================================

/**
 * Get column metadata for a master query
 */
export const getMasterQueryColumns = async (masterQueryId: string): Promise<ColumnMetadata[]> => {
  try {
    const response = await httpClient.get(`${API_BASE_URL}/${masterQueryId}/columns`);
    return response.data;
  } catch (error) {
    console.error(`Failed to fetch columns for master query ${masterQueryId}:`, error);
    throw error;
  }
};

/**
 * Update column metadata for a master query
 */
export const updateMasterQueryColumns = async (
  masterQueryId: string,
  columns: ColumnMetadata[]
): Promise<ColumnMetadata[]> => {
  try {
    const response = await httpClient.put(`${API_BASE_URL}/${masterQueryId}/columns`, { columns });
    return response.data;
  } catch (error) {
    console.error(`Failed to update columns for master query ${masterQueryId}:`, error);
    throw error;
  }
};

/**
 * Analyze master query complexity and performance characteristics
 */
export const analyzeMasterQuery = async (masterQueryId: string): Promise<QueryAnalysis> => {
  try {
    const response = await httpClient.get(`${API_BASE_URL}/${masterQueryId}/analyze`);
    return response.data;
  } catch (error) {
    console.error(`Failed to analyze master query ${masterQueryId}:`, error);
    throw error;
  }
};

// =========================================================================
// SMART FIELD MAPPING AND AI SUGGESTIONS
// =========================================================================

/**
 * Generate smart field mappings based on query results and banking patterns
 */
export const generateSmartFieldMappings = async (
  masterQueryId: string,
  targetSchema?: string
): Promise<SmartFieldMapping[]> => {
  try {
    const payload = targetSchema ? { targetSchema } : {};
    const response = await httpClient.post(`${API_BASE_URL}/${masterQueryId}/smart-mapping`, payload);
    return response.data;
  } catch (error) {
    console.error(`Failed to generate smart mappings for master query ${masterQueryId}:`, error);
    throw error;
  }
};

/**
 * Validate and score field mappings
 */
export const validateFieldMappings = async (
  masterQueryId: string,
  mappings: SmartFieldMapping[]
): Promise<SmartFieldMapping[]> => {
  try {
    const response = await httpClient.post(`${API_BASE_URL}/${masterQueryId}/validate-mappings`, {
      mappings
    });
    return response.data;
  } catch (error) {
    console.error(`Failed to validate field mappings for master query ${masterQueryId}:`, error);
    throw error;
  }
};

/**
 * Get banking field patterns for smart mapping
 */
export const getBankingFieldPatterns = async (): Promise<Record<string, any>> => {
  try {
    const response = await httpClient.get(`${API_BASE_URL}/patterns/banking`);
    return response.data;
  } catch (error) {
    console.error('Failed to fetch banking field patterns:', error);
    throw error;
  }
};

// =========================================================================
// SYSTEM INFORMATION AND CONNECTIVITY
// =========================================================================

/**
 * Get available database schemas and access levels
 */
export const getSchemaInfo = async (): Promise<SchemaInfo> => {
  try {
    const response = await httpClient.get(`${API_BASE_URL}/schema-info`);
    return response.data;
  } catch (error) {
    console.error('Failed to fetch schema information:', error);
    throw error;
  }
};

/**
 * Test database connectivity and performance
 */
export const testDatabaseConnectivity = async (): Promise<ConnectivityTest> => {
  try {
    const response = await httpClient.get(`${API_BASE_URL}/connectivity-test`);
    return response.data;
  } catch (error) {
    console.error('Failed to test database connectivity:', error);
    throw error;
  }
};

/**
 * Get system configuration and security settings
 */
export const getSystemConfiguration = async (): Promise<Record<string, any>> => {
  try {
    const response = await httpClient.get(`${API_BASE_URL}/system-config`);
    return response.data;
  } catch (error) {
    console.error('Failed to fetch system configuration:', error);
    throw error;
  }
};

// =========================================================================
// STATISTICS AND MONITORING
// =========================================================================

/**
 * Get query execution statistics and performance metrics
 */
export const getQueryExecutionStatistics = async (
  timeRange: { start: Date; end: Date }
): Promise<QueryExecutionStatistics> => {
  try {
    const params = new URLSearchParams();
    params.append('startDate', timeRange.start.toISOString());
    params.append('endDate', timeRange.end.toISOString());
    
    const response = await httpClient.get(`${API_BASE_URL}/statistics?${params.toString()}`);
    return response.data;
  } catch (error) {
    console.error('Failed to fetch query execution statistics:', error);
    throw error;
  }
};

/**
 * Get audit trail for a specific master query
 */
export const getMasterQueryAuditTrail = async (
  masterQueryId: string,
  limit: number = 100
): Promise<Record<string, any>[]> => {
  try {
    const response = await httpClient.get(
      `${API_BASE_URL}/${masterQueryId}/audit-trail?limit=${limit}`
    );
    return response.data;
  } catch (error) {
    console.error(`Failed to fetch audit trail for master query ${masterQueryId}:`, error);
    throw error;
  }
};

// =========================================================================
// TEMPLATE MANAGEMENT
// =========================================================================

/**
 * Get available query templates by category
 */
export const getQueryTemplates = async (category?: string): Promise<QueryTemplate[]> => {
  try {
    const params = category ? `?category=${category}` : '';
    const response = await httpClient.get(`${API_BASE_URL}/templates${params}`);
    return response.data;
  } catch (error) {
    console.error('Failed to fetch query templates:', error);
    throw error;
  }
};

/**
 * Create a master query from a template
 */
export const createFromTemplate = async (
  templateId: string,
  parameters: Record<string, any>
): Promise<MasterQuery> => {
  try {
    const response = await httpClient.post(`${API_BASE_URL}/templates/${templateId}/create`, {
      parameters
    });
    return response.data;
  } catch (error) {
    console.error(`Failed to create master query from template ${templateId}:`, error);
    throw error;
  }
};

// =========================================================================
// BULK OPERATIONS
// =========================================================================

/**
 * Import multiple master queries from a configuration file
 */
export const importMasterQueries = async (
  configData: Record<string, any>
): Promise<{ imported: number; errors: string[] }> => {
  try {
    const response = await httpClient.post(`${API_BASE_URL}/import`, configData);
    return response.data;
  } catch (error) {
    console.error('Failed to import master queries:', error);
    throw error;
  }
};

/**
 * Export master queries configuration
 */
export const exportMasterQueries = async (
  masterQueryIds?: string[]
): Promise<Record<string, any>> => {
  try {
    const payload = masterQueryIds ? { masterQueryIds } : {};
    const response = await httpClient.post(`${API_BASE_URL}/export`, payload);
    return response.data;
  } catch (error) {
    console.error('Failed to export master queries:', error);
    throw error;
  }
};

// =========================================================================
// UTILITY FUNCTIONS
// =========================================================================

/**
 * Generate unique correlation ID for request tracking
 */
export const generateCorrelationId = (): string => {
  return `mq_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
};

/**
 * Format query execution time for display
 */
export const formatExecutionTime = (timeMs: number): string => {
  if (timeMs < 1000) {
    return `${timeMs}ms`;
  } else if (timeMs < 60000) {
    return `${(timeMs / 1000).toFixed(1)}s`;
  } else {
    const minutes = Math.floor(timeMs / 60000);
    const seconds = ((timeMs % 60000) / 1000).toFixed(1);
    return `${minutes}m ${seconds}s`;
  }
};

/**
 * Validate security classification levels
 */
export const isValidSecurityClassification = (classification: string): boolean => {
  return ['PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED'].includes(classification);
};

/**
 * Check if user has required permissions for security classification
 */
export const hasPermissionForClassification = (
  userRole: string,
  classification: string
): boolean => {
  const roleHierarchy = {
    'SUPER_USER': ['PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED'],
    'ADMIN': ['PUBLIC', 'INTERNAL', 'CONFIDENTIAL'],
    'POWER_USER': ['PUBLIC', 'INTERNAL'],
    'USER': ['PUBLIC']
  };
  
  return roleHierarchy[userRole as keyof typeof roleHierarchy]?.includes(classification) ?? false;
};

// Export all functions as default object for easier imports
export default {
  // Core operations
  getMasterQueries,
  getMasterQuery,
  createMasterQuery,
  updateMasterQuery,
  deleteMasterQuery,
  
  // Execution and validation
  executeMasterQuery,
  validateMasterQuery,
  testMasterQuery,
  
  // Metadata and analysis
  getMasterQueryColumns,
  updateMasterQueryColumns,
  analyzeMasterQuery,
  
  // Smart mapping
  generateSmartFieldMappings,
  validateFieldMappings,
  getBankingFieldPatterns,
  
  // System information
  getSchemaInfo,
  testDatabaseConnectivity,
  getSystemConfiguration,
  
  // Statistics and monitoring
  getQueryExecutionStatistics,
  getMasterQueryAuditTrail,
  
  // Template management
  getQueryTemplates,
  createFromTemplate,
  
  // Bulk operations
  importMasterQueries,
  exportMasterQueries,
  
  // Utilities
  generateCorrelationId,
  formatExecutionTime,
  isValidSecurityClassification,
  hasPermissionForClassification
};