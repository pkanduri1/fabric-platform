import { useState, useCallback, useEffect, useMemo } from 'react';
import { Configuration, ValidationResult, FieldMapping, ValidationError } from '@/types/configuration';
import { configApi } from '../services/api/configApi';

export interface UseValidationReturn {
  // State
  validationResult: ValidationResult | null;
  isValidating: boolean;
  validationErrors: ValidationError[];
  isValid: boolean;
  yamlOutput: string | null;
  
  // Actions
  validateConfiguration: (config: Configuration) => Promise<ValidationResult>;
  validateFieldMapping: (mapping: FieldMapping) => ValidationError[];
  generateYaml: (config: Configuration) => Promise<string>;
  clearValidation: () => void;
  
  // Computed
  getFieldErrors: (fieldName: string) => ValidationError[];
  getErrorCount: () => number;
  getWarningCount: () => number;
}

interface ValidationCache {
  [key: string]: ValidationResult;
}

export const useValidation = (
  autoValidate: boolean = true,
  debounceMs: number = 500
): UseValidationReturn => {
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);
  const [isValidating, setIsValidating] = useState(false);
  const [yamlOutput, setYamlOutput] = useState<string | null>(null);
  const [validationCache, setValidationCache] = useState<ValidationCache>({});
  const [validationTimeout, setValidationTimeout] = useState<NodeJS.Timeout | null>(null);

  // Validate configuration with API
  const validateConfiguration = useCallback(async (config: Configuration): Promise<ValidationResult> => {
    const configKey = JSON.stringify(config);
    
    // Check cache first
    if (validationCache[configKey]) {
      const cachedResult = validationCache[configKey];
      setValidationResult(cachedResult);
      return cachedResult;
    }

    setIsValidating(true);

    try {
      const result = await configApi.validateMapping(config);
      
      // Convert old format to new format if needed
      const normalizedResult: ValidationResult = {
        isValid: result.valid ?? result.isValid ?? false,
        errors: result.errors?.map(error => ({
          fieldName: 'configuration',
          errorType: 'VALIDATION_ERROR',
          message: typeof error === 'string' ? error : error.message || 'Validation error',
          severity: 'error' as const
        })) || [],
        warnings: result.warnings?.map(warning => ({
          fieldName: 'configuration',
          errorType: 'VALIDATION_WARNING',
          message: typeof warning === 'string' ? warning : warning.message || 'Validation warning',
          severity: 'warning' as const
        })) || []
      };
      
      // Cache the result
      setValidationCache(prev => ({
        ...prev,
        [configKey]: normalizedResult
      }));
      
      setValidationResult(normalizedResult);
      return normalizedResult;
    } catch (error) {
      const errorResult: ValidationResult = {
        isValid: false,
        errors: [{
          fieldName: 'configuration',
          errorType: 'VALIDATION_ERROR',
          message: error instanceof Error ? error.message : 'Validation failed',
          severity: 'error'
        }],
        warnings: []
      };
      
      setValidationResult(errorResult);
      return errorResult;
    } finally {
      setIsValidating(false);
    }
  }, [validationCache]);

  // Validate individual field mapping
  const validateFieldMapping = useCallback((mapping: FieldMapping): ValidationError[] => {
    const errors: ValidationError[] = [];

    // Required field validation
    if (!mapping.fieldName?.trim()) {
      errors.push({
        fieldName: mapping.fieldName || 'unknown',
        errorType: 'REQUIRED_FIELD',
        message: 'Field name is required',
        severity: 'error'
      });
    }

    if (!mapping.transformationType) {
      errors.push({
        fieldName: mapping.fieldName || 'unknown',
        errorType: 'REQUIRED_FIELD',
        message: 'Transformation type is required',
        severity: 'error'
      });
    }

    // Transformation-specific validation
    switch (mapping.transformationType) {
      case 'constant':
        if (!mapping.value && mapping.value !== '0') {
          errors.push({
            fieldName: mapping.fieldName,
            errorType: 'MISSING_VALUE',
            message: 'Constant value is required',
            severity: 'error'
          });
        }
        break;

      case 'source':
        if (!mapping.sourceField?.trim()) {
          errors.push({
            fieldName: mapping.fieldName,
            errorType: 'MISSING_SOURCE',
            message: 'Source field is required',
            severity: 'error'
          });
        }
        break;

      case 'composite':
        if (!mapping.format?.trim()) {
          errors.push({
            fieldName: mapping.fieldName,
            errorType: 'MISSING_FORMAT',
            message: 'Format template is required for composite fields',
            severity: 'error'
          });
        }
        break;

      case 'conditional':
        if (!mapping.conditions || mapping.conditions.length === 0) {
          errors.push({
            fieldName: mapping.fieldName,
            errorType: 'MISSING_CONDITIONS',
            message: 'At least one condition is required',
            severity: 'error'
          });
        }
        break;
    }

    // Data type validation
    if (mapping.dataType && mapping.value) {
      switch (mapping.dataType) {
        case 'Integer':
          if (isNaN(Number(mapping.value))) {
            errors.push({
              fieldName: mapping.fieldName,
              errorType: 'TYPE_MISMATCH',
              message: 'Value must be a valid integer',
              severity: 'error'
            });
          }
          break;

        case 'Decimal':
          if (isNaN(parseFloat(mapping.value))) {
            errors.push({
              fieldName: mapping.fieldName,
              errorType: 'TYPE_MISMATCH',
              message: 'Value must be a valid decimal number',
              severity: 'error'
            });
          }
          break;
      }
    }

    // Length validation
    if (mapping.length && mapping.value && mapping.value.length > mapping.length) {
      errors.push({
        fieldName: mapping.fieldName,
        errorType: 'LENGTH_EXCEEDED',
        message: `Value exceeds maximum length of ${mapping.length}`,
        severity: 'warning'
      });
    }

    // Position validation
    if (mapping.targetPosition && mapping.targetPosition < 1) {
      errors.push({
        fieldName: mapping.fieldName,
        errorType: 'INVALID_POSITION',
        message: 'Target position must be greater than 0',
        severity: 'error'
      });
    }

    return errors;
  }, []);

  // Generate YAML output
  const generateYaml = useCallback(async (config: Configuration): Promise<string> => {
    try {
      setIsValidating(true);
      const yamlResponse = await configApi.generateYaml(config);
      
      // Extract YAML content from response
      const yamlContent = typeof yamlResponse === 'string' 
        ? yamlResponse 
        : yamlResponse.yamlContent || JSON.stringify(yamlResponse, null, 2);
      
      setYamlOutput(yamlContent);
      return yamlContent;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to generate YAML';
      throw new Error(errorMessage);
    } finally {
      setIsValidating(false);
    }
  }, []);

  // Clear validation state
  const clearValidation = useCallback(() => {
    setValidationResult(null);
    setYamlOutput(null);
    setValidationCache({});
    
    if (validationTimeout) {
      clearTimeout(validationTimeout);
      setValidationTimeout(null);
    }
  }, [validationTimeout]);

  // Get errors for specific field
  const getFieldErrors = useCallback((fieldName: string): ValidationError[] => {
    if (!validationResult) return [];
    
    return [
      ...validationResult.errors.filter(error => error.fieldName === fieldName),
      ...validationResult.warnings.filter(warning => warning.fieldName === fieldName)
    ];
  }, [validationResult]);

  // Computed values
  const validationErrors = useMemo(() => {
    if (!validationResult) return [];
    return [...validationResult.errors, ...validationResult.warnings];
  }, [validationResult]);

  const isValid = useMemo(() => {
    return validationResult?.isValid ?? false;
  }, [validationResult]);

  const getErrorCount = useCallback(() => {
    return validationResult?.errors.length ?? 0;
  }, [validationResult]);

  const getWarningCount = useCallback(() => {
    return validationResult?.warnings.length ?? 0;
  }, [validationResult]);

  // Auto-validation with debouncing
  const debouncedValidation = useCallback((config: Configuration) => {
    if (validationTimeout) {
      clearTimeout(validationTimeout);
    }

    const timeout = setTimeout(() => {
      if (autoValidate) {
        validateConfiguration(config);
      }
    }, debounceMs);

    setValidationTimeout(timeout);
  }, [autoValidate, debounceMs, validateConfiguration, validationTimeout]);

  // Cleanup timeout on unmount
  useEffect(() => {
    return () => {
      if (validationTimeout) {
        clearTimeout(validationTimeout);
      }
    };
  }, [validationTimeout]);

  return {
    // State
    validationResult,
    isValidating,
    validationErrors,
    isValid,
    yamlOutput,
    
    // Actions
    validateConfiguration,
    validateFieldMapping,
    generateYaml,
    clearValidation,
    
    // Computed
    getFieldErrors,
    getErrorCount,
    getWarningCount
  };
};