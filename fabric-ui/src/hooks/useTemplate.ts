import { useState, useCallback } from 'react';
import { templateApiService } from '../services/api/templateApi';
import { 
  FileTypeTemplate, 
  FieldTemplate, 
  TemplateImportResult,
  TemplateValidationResult 
} from '../types/template';

export interface UseTemplateReturn {
  // State
  fileTypes: FileTypeTemplate[];
  selectedFileType: string | null;
  templateFields: FieldTemplate[];
  transactionTypes: string[];
  loading: boolean;
  error: string | null;
  
  // Actions
  loadFileTypes: () => Promise<void>;
  selectFileType: (fileType: string) => Promise<void>;
  loadTemplateFields: (fileType: string, transactionType: string) => Promise<void>;
  loadTransactionTypes: (fileType: string) => Promise<void>;
  importFromExcel: (file: File, fileType: string) => Promise<TemplateImportResult>;
  validateTemplate: (template: FileTypeTemplate) => Promise<TemplateValidationResult>;
  saveTemplate: (template: FileTypeTemplate) => Promise<void>;
  clearError: () => void;
}

export const useTemplate = (): UseTemplateReturn => {
  const [fileTypes, setFileTypes] = useState<FileTypeTemplate[]>([]);
  const [selectedFileType, setSelectedFileType] = useState<string | null>(null);
  const [templateFields, setTemplateFields] = useState<FieldTemplate[]>([]);
  const [transactionTypes, setTransactionTypes] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const loadFileTypes = useCallback(async () => {
    try {
      setLoading(true);
            const types = await templateApiService.getFileTypes();
            // Map FileType[] to FileTypeTemplate[] by adding required properties
            const templateTypes: FileTypeTemplate[] = types.map(type => ({
              ...type,
              recordLength: (type as any).recordLength ?? 0, // Default to 0 if missing
            }));
            setFileTypes(templateTypes);
    } catch (err) {
      setError('Failed to load file types');
      console.error('Load file types error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  const selectFileType = useCallback(async (fileType: string) => {
    try {
      setLoading(true);
      setError(null);
      setSelectedFileType(fileType);
      
      // Load transaction types for the selected file type
      const txnTypes = await templateApiService.getTransactionTypes(fileType);
      setTransactionTypes(txnTypes);
      
      // If only one transaction type, load its fields
      if (txnTypes.length === 1) {
        const fields = await templateApiService.getTemplateFields(fileType, txnTypes[0]);
        setTemplateFields(fields);
      } else {
        setTemplateFields([]);
      }
    } catch (err) {
      setError(`Failed to load template: ${fileType}`);
      console.error('Select file type error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadTransactionTypes = useCallback(async (fileType: string) => {
    try {
      setLoading(true);
      setError(null);
      const data = await templateApiService.getTransactionTypes(fileType);
      setTransactionTypes(data);
    } catch (err) {
      setError(`Failed to load transaction types for ${fileType}`);
      console.error('Load transaction types error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadTemplateFields = useCallback(async (fileType: string, transactionType: string) => {
    try {
      setLoading(true);
      setError(null);
      const data = await templateApiService.getTemplateFields(fileType, transactionType);
      setTemplateFields(data);
    } catch (err) {
      setError(`Failed to load fields for ${fileType}/${transactionType}`);
      console.error('Load template fields error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  const importFromExcel = useCallback(async (file: File, fileType: string): Promise<TemplateImportResult> => {
    try {
      setLoading(true);
      setError(null);
      const result = await templateApiService.importFromExcel(file, fileType);
      
      if (result.success) {
        // Reload fields if this file type is currently selected
        if (selectedFileType === fileType && transactionTypes.length > 0) {
          await loadTemplateFields(fileType, transactionTypes[0]);
        }
      }
      
      return result;
    } catch (err) {
      setError('Excel import failed');
      console.error('Excel import error:', err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [selectedFileType, transactionTypes, loadTemplateFields]);

  const validateTemplate = useCallback(async (template: FileTypeTemplate): Promise<TemplateValidationResult> => {
    try {
      setLoading(true);
      setError(null);
      return await templateApiService.validateTemplate(template.fileType, template.fields || []);
    } catch (err) {
      setError('Template validation failed');
      console.error('Template validation error:', err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const saveTemplate = useCallback(async (template: FileTypeTemplate) => {
    try {
      setLoading(true);
      setError(null);
      
      // Create import request
      const importRequest = {
        fileType: template.fileType,
        description: template.description,
        createdBy: 'ui-user',
        replaceExisting: true,
        fields: template.fields || []
      };
      
      const result = await templateApiService.importFromJson(importRequest);
      
      if (!result.success) {
        throw new Error(result.message);
      }
      
      // Reload file types to reflect changes
      await loadFileTypes();
      
      // If this is the currently selected file type, reload its fields
      if (selectedFileType === template.fileType && transactionTypes.length > 0) {
        await loadTemplateFields(template.fileType, transactionTypes[0]);
      }
    } catch (err) {
      setError('Failed to save template');
      console.error('Save template error:', err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [selectedFileType, transactionTypes, loadFileTypes, loadTemplateFields]);

  return {
    // State
    fileTypes,
    selectedFileType,
    templateFields,
    transactionTypes,
    loading,
    error,
    
    // Actions
    loadFileTypes,
    selectFileType,
    loadTemplateFields,
    loadTransactionTypes,
    importFromExcel,
    validateTemplate,
    saveTemplate,
    clearError
  };
};

export default useTemplate;