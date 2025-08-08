import { useState, useCallback, useEffect, useMemo } from 'react';
import { SourceSystem, JobConfig, SourceField } from '../types/configuration';
import { configApi } from '../services/api/configApi';
import { mockSourceSystems, mockSourceFields } from '../data/mockData';


export interface UseSourceSystemsReturn {
  // State
  sourceSystems: SourceSystem[];
  selectedSystem: SourceSystem | null;
  selectedJob: JobConfig | null;
  sourceFields: SourceField[];
  isLoading: boolean;
  error: string | null;
  
  // Actions
  loadSourceSystems: () => Promise<void>;
  selectSystem: (systemId: string) => void;
  selectJob: (jobName: string) => void;
  loadSourceFields: (systemId: string) => Promise<void>;
  refreshData: () => Promise<void>;
  loadJobsForSystem: (systemId: string) => Promise<void>;
  
  // Computed
  getSystemById: (systemId: string) => SourceSystem | undefined;
  getJobByName: (jobName: string) => JobConfig | undefined;
  getAvailableJobs: () => JobConfig[];
  getFieldsByType: (dataType: string) => SourceField[];
}

interface SystemCache {
  systems: SourceSystem[];
  fields: { [systemId: string]: SourceField[] };
  lastUpdated: number;
}

const CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

export const useSourceSystems = (
  autoLoad: boolean = true,
  cacheEnabled: boolean = true
): UseSourceSystemsReturn => {
  const [sourceSystems, setSourceSystems] = useState<SourceSystem[]>([]);
  const [selectedSystem, setSelectedSystem] = useState<SourceSystem | null>(null);
  const [selectedJob, setSelectedJob] = useState<JobConfig | null>(null);
  const [sourceFields, setSourceFields] = useState<SourceField[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cache, setCache] = useState<SystemCache>({
    systems: [],
    fields: {},
    lastUpdated: 0
  });

  // Check if cache is valid
  const isCacheValid = useCallback(() => {
    if (!cacheEnabled) return false;
    return Date.now() - cache.lastUpdated < CACHE_DURATION;
  }, [cache.lastUpdated, cacheEnabled]);

  // Load all source systems
  const loadSourceSystems = useCallback(async () => {
    // Return cached data if valid
    if (isCacheValid() && cache.systems.length > 0) {
      setSourceSystems(cache.systems);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const systems = await configApi.getSourceSystems();
      
      // Convert to expected format if needed
      const normalizedSystems: SourceSystem[] = systems.map(system => ({
        ...system,
        jobs: Array.isArray(system.jobs) 
          ? system.jobs.map(job => typeof job === 'string' 
              ? { 
                  name: job, 
                  sourceSystem: system.id, 
                  jobName: job,
                  files: [] // Required property
                } 
              : job)
          : []
      }));
      
      setSourceSystems(normalizedSystems);
      
      // Update cache
      if (cacheEnabled) {
        setCache(prev => ({
          ...prev,
          systems: normalizedSystems,
          lastUpdated: Date.now()
        }));
      }
    } catch (err) {
        console.warn('API failed, using mock source fields:', err);
        setSourceSystems(mockSourceSystems);
        setError(null);
      //const errorMessage = err instanceof Error ? err.message : 'Failed to load source systems';
      //setError(errorMessage);
      //console.error('Failed to load source systems:', err);
      
    } finally {
      setIsLoading(false);
    }
  }, [isCacheValid, cache.systems, cacheEnabled]);

  // Select a source system
  const selectSystem = useCallback((systemId: string) => {
    const system = sourceSystems.find(s => s.id === systemId);
    if (system) {
      setSelectedSystem(system);
      setSelectedJob(null); // Reset job selection
      setSourceFields([]); // Clear fields
      
      // Auto-load fields for the selected system
      loadSourceFields(systemId);
    } else {
      setError(`Source system not found: ${systemId}`);
    }
  }, [sourceSystems]);

  // Select a job within the current system
  const selectJob = useCallback((jobName: string) => {
    if (!selectedSystem) {
      setError('No source system selected');
      return;
    }

    const job = selectedSystem.jobs.find(j => j.name === jobName);
    if (job) {
      setSelectedJob(job);
      setError(null);
    } else {
      setError(`Job not found: ${jobName}`);
    }
  }, [selectedSystem]);

  // Load source fields for a system
  const loadSourceFields = useCallback(async (systemId: string) => {
  // Return cached data if valid
  if (isCacheValid() && cache.fields[systemId]) {
    setSourceFields(cache.fields[systemId]);
    return;
  }

  setIsLoading(true);
  setError(null);

  try {
    const fields = await configApi.getSourceFields(systemId);
    
    // Map API response to frontend format
    const mappedFields = fields.map(field => ({
      name: field.name,
      dataType: field.dataType || 'string', // Map 'type' to 'dataType' with fallback
      description: field.description,
      nullable: field.nullable,
      maxLength: field.maxLength
    }));
    
    setSourceFields(mappedFields);
    
    // Update cache with mapped fields
    if (cacheEnabled) {
      setCache(prev => ({
        ...prev,
        fields: {
          ...prev.fields,
          [systemId]: mappedFields
        },
        lastUpdated: Date.now()
      }));
    }
  } catch (err) {
    console.warn('Using mock source fields:', err);
    setSourceFields(mockSourceFields);
    setError(null);
  } finally {
    setIsLoading(false);
  }
}, [isCacheValid, cache.fields, cacheEnabled]);

// In useSourceSystems.ts, add this function
// Update loadJobsForSystem in useSourceSystems.ts
const loadJobsForSystem = useCallback(async (systemId: string) => {
  try {
    console.log('Loading jobs for system:', systemId);
    const jobConfigs = await configApi.getJobsForSourceSystem(systemId);
    
    // Map API response to expected format
    const jobs = jobConfigs.map(jobConfig => ({
      name: jobConfig.jobName,        // Map jobName to name
      jobName: jobConfig.jobName,     // Keep jobName
      sourceSystem: systemId,
      description: jobConfig.description,
      files: []
    }));
    
    // Update the source system with loaded jobs
    setSourceSystems(prev => prev.map(system => 
      system.id === systemId 
        ? { ...system, jobs: jobs }
        : system
    ));
    
    console.log('Jobs loaded for', systemId, ':', jobs);
  } catch (error) {
    console.error(`Failed to load jobs for ${systemId}:`, error);
  }
}, []);

  // Refresh all data
  const refreshData = useCallback(async () => {
    // Clear cache
    setCache({
      systems: [],
      fields: {},
      lastUpdated: 0
    });

    // Reload systems
    await loadSourceSystems();

    // Reload fields if system is selected
    if (selectedSystem) {
      await loadSourceFields(selectedSystem.id);
    }
  }, [loadSourceSystems, loadSourceFields, selectedSystem]);

  // Get system by ID
  const getSystemById = useCallback((systemId: string): SourceSystem | undefined => {
    return sourceSystems.find(system => system.id === systemId);
  }, [sourceSystems]);

  // Get job by name within selected system
  const getJobByName = useCallback((jobName: string): JobConfig | undefined => {
    if (!selectedSystem) return undefined;
    return selectedSystem.jobs.find(job => job.name === jobName);
  }, [selectedSystem]);

  // Get available jobs for selected system
  const getAvailableJobs = useCallback((): JobConfig[] => {
    return selectedSystem?.jobs || [];
  }, [selectedSystem]);

  // Get fields filtered by data type
  const getFieldsByType = useCallback((dataType: string): SourceField[] => {
    return sourceFields.filter(field => field.dataType === dataType);
  }, [sourceFields]);

  // Computed values
  const systemOptions = useMemo(() => {
    return sourceSystems.map(system => ({
      id: system.id,
      name: system.name,
      description: system.description
    }));
  }, [sourceSystems]);

  const jobOptions = useMemo(() => {
    return getAvailableJobs().map(job => ({
      name: job.name,
      description: job.description
    }));
  }, [getAvailableJobs]);

  // Auto-load systems on mount
  useEffect(() => {
    if (autoLoad) {
      loadSourceSystems();
    }
  }, [autoLoad, loadSourceSystems]);

  // Auto-select first system if only one exists
  useEffect(() => {
    if (sourceSystems.length === 1 && !selectedSystem) {
      selectSystem(sourceSystems[0].id);
    }
  }, [sourceSystems, selectedSystem, selectSystem]);

  return {
    // State
    sourceSystems,
    selectedSystem,
    selectedJob,
    sourceFields,
    isLoading,
    error,
    
    // Actions
    loadSourceSystems,
    selectSystem,
    selectJob,
    loadSourceFields,
    loadJobsForSystem,
    refreshData,
    
    // Computed
    getSystemById,
    getJobByName,
    getAvailableJobs,
    getFieldsByType
  };
};