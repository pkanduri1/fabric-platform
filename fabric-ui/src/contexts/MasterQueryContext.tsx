// src/contexts/MasterQueryContext.tsx
/**
 * Master Query Context - Banking Grade Intelligence & Performance
 * Phase 2 Frontend Implementation with Context API and Banking Domain Intelligence
 */

import React, { createContext, useContext, useEffect, ReactNode, useState, useCallback } from 'react';
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
  QueryTemplate,
  BankingFieldPattern
} from '../types/masterQuery';

// IndexedDB Cache Interface
interface CacheEntry<T> {
  data: T;
  timestamp: number;
  expiresAt: number;
}

// Context state interface
export interface MasterQueryContextState {
  // Query Management
  queries: MasterQuery[];
  selectedQuery: MasterQuery | null;
  queryResults: any[] | null;
  columnMetadata: ColumnMetadata[] | null;
  validationResult: QueryValidationResult | null;
  queryAnalysis: QueryAnalysis | null;
  
  // Execution State
  isExecuting: boolean;
  isValidating: boolean;
  isLoadingMetadata: boolean;
  executionHistory: MasterQueryResponse[];
  
  // Schema Information
  schemaInfo: SchemaInfo | null;
  isLoadingSchemas: boolean;
  
  // UI State
  error: string | null;
  isLoading: boolean;
  filter: MasterQueryFilter;
  selectedTemplate: QueryTemplate | null;
  
  // Banking Intelligence
  smartMappings: SmartFieldMapping[];
  detectedPatterns: BankingFieldPattern[];
  isAnalyzing: boolean;
  
  // Performance
  connectivity: ConnectivityTest | null;
  statistics: QueryExecutionStatistics | null;
  
  // Actions - Query Management
  loadQueries: (filter?: MasterQueryFilter) => Promise<void>;
  selectQuery: (queryId: string) => void;
  createQuery: (request: MasterQueryRequest) => Promise<boolean>;
  updateQuery: (queryId: string, request: MasterQueryRequest) => Promise<boolean>;
  deleteQuery: (queryId: string) => Promise<boolean>;
  
  // Actions - Query Execution
  executeQuery: (request: MasterQueryRequest) => Promise<MasterQueryResponse>;
  validateQuery: (request: MasterQueryRequest) => Promise<QueryValidationResult>;
  getColumnMetadata: (request: MasterQueryRequest) => Promise<ColumnMetadata[]>;
  analyzeQuery: (sql: string) => Promise<QueryAnalysis>;
  
  // Actions - Schema Operations
  loadSchemas: () => Promise<void>;
  refreshConnectivity: () => Promise<void>;
  getStatistics: (startTime: Date, endTime: Date) => Promise<void>;
  
  // Actions - Banking Intelligence
  generateSmartMappings: (columns: ColumnMetadata[]) => Promise<SmartFieldMapping[]>;
  detectBankingPatterns: (columns: ColumnMetadata[]) => Promise<BankingFieldPattern[]>;
  
  // Actions - Templates
  loadTemplates: () => Promise<QueryTemplate[]>;
  selectTemplate: (template: QueryTemplate) => void;
  
  // Actions - Filtering & Search
  updateFilter: (filter: Partial<MasterQueryFilter>) => void;
  clearFilter: () => void;
  
  // Utility
  clearError: () => void;
  resetState: () => void;
  exportQuery: (queryId: string) => void;
  importQuery: (queryData: any) => Promise<boolean>;
}

// Banking Field Patterns for Intelligence
const BANKING_PATTERNS: BankingFieldPattern[] = [
  {
    pattern: /^(acct|account)[_\-]?(num|number|id)?$/i,
    fieldType: 'ACCOUNT_NUMBER',
    description: 'Bank account number field',
    maskingRequired: true,
    validation: (value: string) => /^\d{8,17}$/.test(value)
  },
  {
    pattern: /^(routing|rt|aba)[_\-]?(num|number)?$/i,
    fieldType: 'ROUTING_NUMBER',
    description: 'Bank routing number field',
    validation: (value: string) => /^\d{9}$/.test(value)
  },
  {
    pattern: /^(txn|trans|transaction)[_\-]?(id|number)?$/i,
    fieldType: 'TRANSACTION_ID',
    description: 'Transaction identifier field',
    validation: (value: string) => value.length > 0
  },
  {
    pattern: /^(amt|amount|balance|total)[_\-]?(usd|cents)?$/i,
    fieldType: 'AMOUNT',
    description: 'Monetary amount field',
    validation: (value: string) => /^\d+(\.\d{1,2})?$/.test(value),
    format: (value: string) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(parseFloat(value))
  },
  {
    pattern: /^(date|dt|time)[_\-]?(created|modified|processed)?$/i,
    fieldType: 'DATE',
    description: 'Date/timestamp field',
    validation: (value: string) => !isNaN(Date.parse(value))
  },
  {
    pattern: /^(curr|currency)[_\-]?(code)?$/i,
    fieldType: 'CURRENCY',
    description: 'Currency code field',
    validation: (value: string) => /^[A-Z]{3}$/.test(value)
  },
  {
    pattern: /^(cust|customer)[_\-]?(id|number)?$/i,
    fieldType: 'CUSTOMER_ID',
    description: 'Customer identifier field',
    maskingRequired: true,
    validation: (value: string) => value.length > 0
  }
];

// Create context
const MasterQueryContext = createContext<MasterQueryContextState | null>(null);

// IndexedDB Cache Manager
class CacheManager {
  private dbName = 'fabric-master-query-cache';
  private dbVersion = 1;
  private db: IDBDatabase | null = null;

  async init(): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.dbName, this.dbVersion);
      
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        this.db = request.result;
        resolve();
      };
      
      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;
        
        // Create stores
        if (!db.objectStoreNames.contains('queries')) {
          db.createObjectStore('queries', { keyPath: 'key' });
        }
        if (!db.objectStoreNames.contains('metadata')) {
          db.createObjectStore('metadata', { keyPath: 'key' });
        }
        if (!db.objectStoreNames.contains('schemas')) {
          db.createObjectStore('schemas', { keyPath: 'key' });
        }
      };
    });
  }

  async get<T>(store: string, key: string): Promise<T | null> {
    if (!this.db) return null;
    
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([store], 'readonly');
      const objectStore = transaction.objectStore(store);
      const request = objectStore.get(key);
      
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const result = request.result as CacheEntry<T>;
        if (!result || Date.now() > result.expiresAt) {
          resolve(null);
        } else {
          resolve(result.data);
        }
      };
    });
  }

  async set<T>(store: string, key: string, data: T, ttlMs: number = 5 * 60 * 1000): Promise<void> {
    if (!this.db) return;
    
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([store], 'readwrite');
      const objectStore = transaction.objectStore(store);
      const entry: CacheEntry<T> = {
        data,
        timestamp: Date.now(),
        expiresAt: Date.now() + ttlMs
      };
      
      const request = objectStore.put({ key, ...entry });
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve();
    });
  }
}

// Context provider props
interface MasterQueryProviderProps {
  children: ReactNode;
}

// Context provider component
export const MasterQueryProvider: React.FC<MasterQueryProviderProps> = ({ children }) => {
  // Core state
  const [queries, setQueries] = useState<MasterQuery[]>([]);
  const [selectedQuery, setSelectedQuery] = useState<MasterQuery | null>(null);
  const [queryResults, setQueryResults] = useState<any[] | null>(null);
  const [columnMetadata, setColumnMetadata] = useState<ColumnMetadata[] | null>(null);
  const [validationResult, setValidationResult] = useState<QueryValidationResult | null>(null);
  const [queryAnalysis, setQueryAnalysis] = useState<QueryAnalysis | null>(null);
  
  // Execution state
  const [isExecuting, setIsExecuting] = useState(false);
  const [isValidating, setIsValidating] = useState(false);
  const [isLoadingMetadata, setIsLoadingMetadata] = useState(false);
  const [executionHistory, setExecutionHistory] = useState<MasterQueryResponse[]>([]);
  
  // Schema state
  const [schemaInfo, setSchemaInfo] = useState<SchemaInfo | null>(null);
  const [isLoadingSchemas, setIsLoadingSchemas] = useState(false);
  
  // UI state
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<QueryTemplate | null>(null);
  
  // Banking intelligence state
  const [smartMappings, setSmartMappings] = useState<SmartFieldMapping[]>([]);
  const [detectedPatterns, setDetectedPatterns] = useState<BankingFieldPattern[]>([]);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  
  // Performance state
  const [connectivity, setConnectivity] = useState<ConnectivityTest | null>(null);
  const [statistics, setStatistics] = useState<QueryExecutionStatistics | null>(null);
  
  // Filter state
  const [filter, setFilter] = useState<MasterQueryFilter>({
    sortBy: 'modified',
    sortOrder: 'desc'
  });
  
  // Cache manager
  const [cacheManager] = useState(() => new CacheManager());
  
  // Initialize cache on mount
  useEffect(() => {
    cacheManager.init().catch(console.error);
  }, [cacheManager]);
  
  // Load queries with caching and filtering
  const loadQueries = useCallback(async (newFilter?: MasterQueryFilter) => {
    const effectiveFilter = newFilter || filter;
    const cacheKey = `queries_${JSON.stringify(effectiveFilter)}`;
    
    try {
      setIsLoading(true);
      setError(null);
      
      // Try cache first
      const cachedQueries = await cacheManager.get<MasterQuery[]>('queries', cacheKey);
      if (cachedQueries) {
        setQueries(cachedQueries);
        return;
      }
      
      // TODO: Replace with actual API call
      // const response = await masterQueryApi.getQueries(effectiveFilter);
      
      // Mock data for now
      const mockQueries: MasterQuery[] = [
        {
          masterQueryId: 'mq_txn_summary_001',
          queryName: 'Daily Transaction Summary',
          querySql: 'SELECT account_id, SUM(amount) as total FROM transactions WHERE batch_date = :batchDate GROUP BY account_id',
          queryDescription: 'Summarizes daily transaction amounts by account',
          securityClassification: 'INTERNAL',
          dataClassification: 'SENSITIVE',
          businessJustification: 'Required for daily reconciliation',
          complianceTags: ['SOX', 'PCI_DSS'],
          status: 'ACTIVE',
          createdAt: new Date('2025-08-01'),
          lastModifiedAt: new Date('2025-08-15')
        }
      ];
      
      setQueries(mockQueries);
      await cacheManager.set('queries', cacheKey, mockQueries);
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load queries');
    } finally {
      setIsLoading(false);
    }
  }, [filter, cacheManager]);
  
  // Select query
  const selectQuery = useCallback((queryId: string) => {
    const query = queries.find(q => q.masterQueryId === queryId);
    setSelectedQuery(query || null);
    
    // Clear previous results
    setQueryResults(null);
    setColumnMetadata(null);
    setValidationResult(null);
    setQueryAnalysis(null);
  }, [queries]);
  
  // Execute query with performance tracking
  const executeQuery = useCallback(async (request: MasterQueryRequest): Promise<MasterQueryResponse> => {
    try {
      setIsExecuting(true);
      setError(null);
      
      const startTime = performance.now();
      
      // TODO: Replace with actual API call
      // const response = await masterQueryApi.executeQuery(request);
      
      // Mock response
      const response: MasterQueryResponse = {
        masterQueryId: request.masterQueryId,
        executionStatus: 'SUCCESS',
        queryName: request.queryName,
        results: [
          { account_id: 'ACC001', total: 1500.00 },
          { account_id: 'ACC002', total: 2300.50 }
        ],
        rowCount: 2,
        executionTimeMs: performance.now() - startTime,
        executedBy: 'current_user',
        userRole: 'JOB_EXECUTOR',
        correlationId: `corr_${Date.now()}`,
        securityClassification: request.securityClassification,
        dataClassification: request.dataClassification
      };
      
      setQueryResults(response.results || []);
      setExecutionHistory(prev => [response, ...prev.slice(0, 9)]); // Keep last 10
      
      return response;
      
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Query execution failed';
      setError(errorMessage);
      throw err;
    } finally {
      setIsExecuting(false);
    }
  }, []);
  
  // Validate query
  const validateQuery = useCallback(async (request: MasterQueryRequest): Promise<QueryValidationResult> => {
    try {
      setIsValidating(true);
      setError(null);
      
      // TODO: Replace with actual API call
      // const result = await masterQueryApi.validateQuery(request);
      
      // Mock validation
      const result: QueryValidationResult = {
        valid: true,
        correlationId: `corr_${Date.now()}`,
        message: 'Query validation passed',
        validatedAt: new Date(),
        validatedBy: 'current_user',
        syntaxValid: true,
        businessRules: {
          complexityCheck: true,
          parameterConsistency: true,
          securityClassification: request.securityClassification
        }
      };
      
      setValidationResult(result);
      return result;
      
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Query validation failed';
      setError(errorMessage);
      throw err;
    } finally {
      setIsValidating(false);
    }
  }, []);
  
  // Get column metadata with caching
  const getColumnMetadata = useCallback(async (request: MasterQueryRequest): Promise<ColumnMetadata[]> => {
    try {
      setIsLoadingMetadata(true);
      setError(null);
      
      const cacheKey = `metadata_${request.masterQueryId}`;
      
      // Try cache first
      const cachedMetadata = await cacheManager.get<ColumnMetadata[]>('metadata', cacheKey);
      if (cachedMetadata) {
        setColumnMetadata(cachedMetadata);
        return cachedMetadata;
      }
      
      // TODO: Replace with actual API call
      // const metadata = await masterQueryApi.getColumnMetadata(request);
      
      // Mock metadata
      const metadata: ColumnMetadata[] = [
        {
          name: 'account_id',
          type: 'VARCHAR2',
          length: 20,
          nullable: false,
          order: 1,
          dataClassification: 'SENSITIVE',
          businessConcept: 'Account Identifier'
        },
        {
          name: 'total',
          type: 'NUMBER',
          precision: 15,
          scale: 2,
          nullable: true,
          order: 2,
          dataClassification: 'INTERNAL',
          businessConcept: 'Transaction Amount'
        }
      ];
      
      setColumnMetadata(metadata);
      await cacheManager.set('metadata', cacheKey, metadata);
      
      return metadata;
      
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load metadata';
      setError(errorMessage);
      throw err;
    } finally {
      setIsLoadingMetadata(false);
    }
  }, [cacheManager]);
  
  // Generate smart mappings with banking intelligence
  const generateSmartMappings = useCallback(async (columns: ColumnMetadata[]): Promise<SmartFieldMapping[]> => {
    try {
      setIsAnalyzing(true);
      
      const mappings: SmartFieldMapping[] = [];
      
      for (const column of columns) {
        for (const pattern of BANKING_PATTERNS) {
          if (pattern.pattern.test(column.name)) {
            mappings.push({
              sourceColumn: column.name,
              targetField: pattern.fieldType.toLowerCase().replace('_', '-'),
              confidence: 0.85 + Math.random() * 0.15, // 85-100%
              detectedPattern: pattern,
              suggestedTransformation: pattern.format ? 'format' : 'direct',
              businessConcept: column.businessConcept || pattern.description,
              dataClassification: column.dataClassification || (pattern.maskingRequired ? 'SENSITIVE' : 'INTERNAL'),
              complianceRequirements: pattern.maskingRequired ? ['PCI_DSS', 'PII_PROTECTION'] : []
            });
            break; // Take first match
          }
        }
      }
      
      setSmartMappings(mappings);
      return mappings;
      
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Smart mapping generation failed';
      setError(errorMessage);
      throw err;
    } finally {
      setIsAnalyzing(false);
    }
  }, []);
  
  // Detect banking patterns
  const detectBankingPatterns = useCallback(async (columns: ColumnMetadata[]): Promise<BankingFieldPattern[]> => {
    const detected = BANKING_PATTERNS.filter(pattern =>
      columns.some(col => pattern.pattern.test(col.name))
    );
    
    setDetectedPatterns(detected);
    return detected;
  }, []);
  
  // Load schemas with caching
  const loadSchemas = useCallback(async () => {
    try {
      setIsLoadingSchemas(true);
      setError(null);
      
      const cacheKey = 'schemas_info';
      
      // Try cache first
      const cachedSchemas = await cacheManager.get<SchemaInfo>('schemas', cacheKey);
      if (cachedSchemas) {
        setSchemaInfo(cachedSchemas);
        return;
      }
      
      // TODO: Replace with actual API call
      // const schemas = await masterQueryApi.getSchemas();
      
      // Mock schema info
      const schemas: SchemaInfo = {
        schemas: [
          {
            schemaName: 'CM3INT',
            description: 'Core banking integration schema',
            accessLevel: 'READ_ONLY',
            tables: ['MANUAL_JOB_CONFIG', 'MANUAL_JOB_EXECUTION', 'MASTER_QUERY_CONFIG']
          }
        ],
        userRole: 'JOB_EXECUTOR',
        retrievedAt: new Date()
      };
      
      setSchemaInfo(schemas);
      await cacheManager.set('schemas', cacheKey, schemas, 10 * 60 * 1000); // 10 min cache
      
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load schemas';
      setError(errorMessage);
    } finally {
      setIsLoadingSchemas(false);
    }
  }, [cacheManager]);
  
  // Update filter
  const updateFilter = useCallback((newFilter: Partial<MasterQueryFilter>) => {
    setFilter(prev => ({ ...prev, ...newFilter }));
  }, []);
  
  // Clear filter
  const clearFilter = useCallback(() => {
    setFilter({ sortBy: 'modified', sortOrder: 'desc' });
  }, []);
  
  // Utility functions
  const clearError = useCallback(() => setError(null), []);
  const resetState = useCallback(() => {
    setSelectedQuery(null);
    setQueryResults(null);
    setColumnMetadata(null);
    setValidationResult(null);
    setQueryAnalysis(null);
    setSmartMappings([]);
    setDetectedPatterns([]);
    setError(null);
  }, []);
  
  // Placeholder implementations for remaining actions
  const createQuery = useCallback(async (request: MasterQueryRequest): Promise<boolean> => {
    // TODO: Implement
    return false;
  }, []);
  
  const updateQuery = useCallback(async (queryId: string, request: MasterQueryRequest): Promise<boolean> => {
    // TODO: Implement
    return false;
  }, []);
  
  const deleteQuery = useCallback(async (queryId: string): Promise<boolean> => {
    // TODO: Implement
    return false;
  }, []);
  
  const analyzeQuery = useCallback(async (sql: string): Promise<QueryAnalysis> => {
    // TODO: Implement intelligent query analysis
    throw new Error('Not implemented');
  }, []);
  
  const refreshConnectivity = useCallback(async () => {
    // TODO: Implement
  }, []);
  
  const getStatistics = useCallback(async (startTime: Date, endTime: Date) => {
    // TODO: Implement
  }, []);
  
  const loadTemplates = useCallback(async (): Promise<QueryTemplate[]> => {
    // TODO: Implement
    return [];
  }, []);
  
  const selectTemplate = useCallback((template: QueryTemplate) => {
    setSelectedTemplate(template);
  }, []);
  
  const exportQuery = useCallback((queryId: string) => {
    // TODO: Implement
  }, []);
  
  const importQuery = useCallback(async (queryData: any): Promise<boolean> => {
    // TODO: Implement
    return false;
  }, []);
  
  // Context value
  const contextValue: MasterQueryContextState = {
    // State
    queries,
    selectedQuery,
    queryResults,
    columnMetadata,
    validationResult,
    queryAnalysis,
    isExecuting,
    isValidating,
    isLoadingMetadata,
    executionHistory,
    schemaInfo,
    isLoadingSchemas,
    error,
    isLoading,
    filter,
    selectedTemplate,
    smartMappings,
    detectedPatterns,
    isAnalyzing,
    connectivity,
    statistics,
    
    // Actions
    loadQueries,
    selectQuery,
    createQuery,
    updateQuery,
    deleteQuery,
    executeQuery,
    validateQuery,
    getColumnMetadata,
    analyzeQuery,
    loadSchemas,
    refreshConnectivity,
    getStatistics,
    generateSmartMappings,
    detectBankingPatterns,
    loadTemplates,
    selectTemplate,
    updateFilter,
    clearFilter,
    clearError,
    resetState,
    exportQuery,
    importQuery
  };
  
  return (
    <MasterQueryContext.Provider value={contextValue}>
      {children}
    </MasterQueryContext.Provider>
  );
};

// Hook to use the master query context
export const useMasterQueryContext = (): MasterQueryContextState => {
  const context = useContext(MasterQueryContext);
  if (!context) {
    throw new Error('useMasterQueryContext must be used within a MasterQueryProvider');
  }
  return context;
};

// Convenience hooks for specific parts of the context
export const useMasterQueryState = () => {
  const { queries, selectedQuery, isLoading, error } = useMasterQueryContext();
  return { queries, selectedQuery, isLoading, error };
};

export const useQueryExecution = () => {
  const { 
    executeQuery, 
    validateQuery, 
    isExecuting, 
    isValidating, 
    queryResults, 
    validationResult,
    executionHistory,
    getColumnMetadata,
    columnMetadata,
    isLoadingMetadata,
    error,
    clearError
  } = useMasterQueryContext();
  
  return { 
    executeQuery, 
    validateQuery, 
    isExecuting, 
    isValidating, 
    queryResults, 
    validationResult,
    executionHistory,
    getColumnMetadata,
    columnMetadata,
    isLoadingMetadata,
    error,
    clearError
  };
};

export const useBankingIntelligence = () => {
  const { 
    generateSmartMappings, 
    detectBankingPatterns, 
    smartMappings, 
    detectedPatterns, 
    isAnalyzing 
  } = useMasterQueryContext();
  
  return { 
    generateSmartMappings, 
    detectBankingPatterns, 
    smartMappings, 
    detectedPatterns, 
    isAnalyzing 
  };
};

export const useSchemaInformation = () => {
  const { 
    schemaInfo, 
    isLoadingSchemas, 
    loadSchemas, 
    connectivity, 
    refreshConnectivity 
  } = useMasterQueryContext();
  
  return { 
    schemaInfo, 
    isLoadingSchemas, 
    loadSchemas, 
    connectivity, 
    refreshConnectivity 
  };
};