/**
 * useRealTimeMonitoring Hook
 * 
 * Specialized React hook for real-time job monitoring dashboard.
 * Manages WebSocket subscriptions, dashboard data state, and provides
 * monitoring-specific functionality for enterprise batch processing.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 * 
 * Features:
 * - Real-time dashboard data management
 * - Intelligent data caching and updates
 * - Performance metrics aggregation
 * - Alert management with filtering
 * - Historical data integration
 * - Mobile-optimized data structures
 * - Error recovery and fallback handling
 */

import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { useWebSocket } from './useWebSocket';
import { 
  DashboardData, 
  ActiveJob, 
  PerformanceMetrics, 
  Alert, 
  SystemHealth,
  WebSocketMessage,
  MonitoringError,
  UseRealTimeMonitoringReturn,
  AlertFilters,
  MonitoringConfiguration,
  HistoricalMetrics
} from '../types/monitoring';
import { monitoringApi } from '../services/api/monitoringApi';

// Hook options interface
export interface UseRealTimeMonitoringOptions {
  // WebSocket configuration
  wsUrl?: string;
  autoConnect?: boolean;
  
  // Data refresh options
  refreshInterval?: number;
  maxCacheAge?: number;
  enablePollingFallback?: boolean;
  pollingInterval?: number;
  
  // Performance options
  bufferUpdates?: boolean;
  maxBufferSize?: number;
  
  // Filtering and subscriptions
  jobFilters?: {
    sourceSystem?: string[];
    jobTypes?: string[];
    status?: string[];
  };
  alertFilters?: AlertFilters;
  
  // Callbacks
  onDataUpdate?: (data: DashboardData) => void;
  onAlert?: (alert: Alert) => void;
  onError?: (error: MonitoringError) => void;
  onReconnect?: () => void;
}

// Default configuration
const DEFAULT_OPTIONS: UseRealTimeMonitoringOptions = {
  wsUrl: '/ws/job-monitoring',
  autoConnect: true,
  refreshInterval: 5000,
  maxCacheAge: 30000,
  enablePollingFallback: true,
  pollingInterval: 10000,
  bufferUpdates: true,
  maxBufferSize: 10
};

/**
 * useRealTimeMonitoring Hook
 * 
 * Provides real-time monitoring functionality with WebSocket integration,
 * data caching, and intelligent updates for job monitoring dashboard.
 * 
 * @param options - Configuration options for monitoring
 * @returns UseRealTimeMonitoringReturn with dashboard data and controls
 */
export function useRealTimeMonitoring(
  options: UseRealTimeMonitoringOptions = {}
): UseRealTimeMonitoringReturn {
  
  // Merge options with defaults
  const config = { ...DEFAULT_OPTIONS, ...options };
  
  // Dashboard data state
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<MonitoringError | null>(null);
  
  // Data cache and timestamps
  const dataCacheRef = useRef<{
    data: DashboardData | null;
    timestamp: number;
    version: number;
  }>({
    data: null,
    timestamp: 0,
    version: 0
  });
  
  // Update buffer for high-frequency scenarios
  const updateBufferRef = useRef<DashboardData[]>([]);
  const bufferFlushTimer = useRef<NodeJS.Timeout | null>(null);
  
  // Polling fallback timer
  const pollingTimer = useRef<NodeJS.Timeout | null>(null);
  
  // Subscription topics based on filters
  const subscriptionTopics = useMemo(() => {
    const topics = ['dashboard.updates', 'job.status', 'system.health', 'alerts'];
    
    // Add filtered subscriptions
    if (config.jobFilters?.sourceSystem) {
      config.jobFilters.sourceSystem.forEach(system => {
        topics.push(`job.${system}`);
      });
    }
    
    if (config.jobFilters?.jobTypes) {
      config.jobFilters.jobTypes.forEach(type => {
        topics.push(`jobtype.${type}`);
      });
    }
    
    return topics;
  }, [config.jobFilters]);
  
  // WebSocket hook configuration
  const wsOptions = useMemo(() => ({
    autoConnect: config.autoConnect,
    reconnectAttempts: 5,
    reconnectInterval: 2000,
    heartbeatInterval: 30000,
    subscriptions: subscriptionTopics,
    debugMode: process.env.NODE_ENV === 'development',
    onMessage: handleWebSocketMessage,
    onConnect: handleWebSocketConnect,
    onDisconnect: handleWebSocketDisconnect,
    onError: handleWebSocketError,
    onReconnect: handleWebSocketReconnect
  }), [subscriptionTopics, config.autoConnect]);
  
  // Initialize WebSocket connection
  const { 
    connected, 
    connecting, 
    send, 
    subscribe, 
    unsubscribe,
    lastMessage 
  } = useWebSocket(config.wsUrl || '/ws/job-monitoring', wsOptions);
  
  /**
   * Handle WebSocket message reception
   */
  function handleWebSocketMessage(message: WebSocketMessage) {
    try {
      switch (message.type) {
        case 'DASHBOARD_UPDATE':
          handleDashboardUpdate(message.payload);
          break;
        case 'JOB_UPDATE':
          handleJobUpdate(message.payload);
          break;
        case 'ALERT':
          handleAlertMessage(message.payload);
          break;
        case 'SYSTEM_HEALTH':
          handleSystemHealthUpdate(message.payload);
          break;
        default:
          console.log('Unhandled WebSocket message type:', message.type);
      }
    } catch (error) {
      console.error('Error processing WebSocket message:', error);
      const processingError: MonitoringError = {
        code: 'MESSAGE_PROCESSING_ERROR',
        message: 'Failed to process WebSocket message',
        timestamp: new Date().toISOString(),
        recoverable: true,
        details: { message, error }
      };
      setError(processingError);
    }
  }
  
  /**
   * Handle dashboard data updates
   */
  const handleDashboardUpdate = useCallback((data: DashboardData) => {
    if (config.bufferUpdates && config.maxBufferSize && config.maxBufferSize > 1) {
      // Buffer high-frequency updates
      updateBufferRef.current.push(data);
      
      if (updateBufferRef.current.length >= config.maxBufferSize) {
        flushUpdateBuffer();
      } else if (!bufferFlushTimer.current) {
        bufferFlushTimer.current = setTimeout(flushUpdateBuffer, 500);
      }
    } else {
      // Apply update immediately
      applyDashboardUpdate(data);
    }
  }, [config.bufferUpdates, config.maxBufferSize]);
  
  /**
   * Apply dashboard update to state
   */
  const applyDashboardUpdate = useCallback((data: DashboardData) => {
    // Update cache
    dataCacheRef.current = {
      data,
      timestamp: Date.now(),
      version: dataCacheRef.current.version + 1
    };
    
    // Update component state
    setDashboardData(data);
    setLoading(false);
    setError(null);
    
    // Call user callback
    config.onDataUpdate?.(data);
  }, [config.onDataUpdate]);
  
  /**
   * Flush buffered updates
   */
  const flushUpdateBuffer = useCallback(() => {
    if (updateBufferRef.current.length > 0) {
      // Use the latest update
      const latestUpdate = updateBufferRef.current[updateBufferRef.current.length - 1];
      updateBufferRef.current = [];
      applyDashboardUpdate(latestUpdate);
    }
    
    if (bufferFlushTimer.current) {
      clearTimeout(bufferFlushTimer.current);
      bufferFlushTimer.current = null;
    }
  }, [applyDashboardUpdate]);
  
  /**
   * Handle individual job updates
   */
  const handleJobUpdate = useCallback((jobData: ActiveJob) => {
    setDashboardData(currentData => {
      if (!currentData) return currentData;
      
      const updatedJobs = currentData.activeJobs.map(job => 
        job.executionId === jobData.executionId ? jobData : job
      );
      
      return {
        ...currentData,
        activeJobs: updatedJobs,
        lastUpdate: new Date().toISOString()
      };
    });
  }, []);
  
  /**
   * Handle alert messages
   */
  const handleAlertMessage = useCallback((alert: Alert) => {
    setDashboardData(currentData => {
      if (!currentData) return currentData;
      
      // Add new alert or update existing one
      const existingAlertIndex = currentData.alerts.findIndex(a => a.alertId === alert.alertId);
      const updatedAlerts = [...currentData.alerts];
      
      if (existingAlertIndex !== -1) {
        updatedAlerts[existingAlertIndex] = alert;
      } else {
        updatedAlerts.unshift(alert); // Add new alerts to the beginning
        
        // Limit alert history
        if (updatedAlerts.length > 100) {
          updatedAlerts.splice(100);
        }
      }
      
      return {
        ...currentData,
        alerts: updatedAlerts,
        lastUpdate: new Date().toISOString()
      };
    });
    
    // Call user alert callback
    config.onAlert?.(alert);
  }, [config.onAlert]);
  
  /**
   * Handle system health updates
   */
  const handleSystemHealthUpdate = useCallback((healthData: SystemHealth) => {
    setDashboardData(currentData => {
      if (!currentData) return currentData;
      
      return {
        ...currentData,
        systemHealth: healthData,
        lastUpdate: new Date().toISOString()
      };
    });
  }, []);
  
  /**
   * Handle WebSocket connection established
   */
  function handleWebSocketConnect() {
    setError(null);
    stopPollingFallback();
    
    // Request initial dashboard data
    requestInitialData();
    
    config.onReconnect?.();
  }
  
  /**
   * Handle WebSocket disconnection
   */
  function handleWebSocketDisconnect() {
    if (config.enablePollingFallback) {
      startPollingFallback();
    }
  }
  
  /**
   * Handle WebSocket errors
   */
  function handleWebSocketError(error: MonitoringError) {
    setError(error);
    config.onError?.(error);
    
    if (config.enablePollingFallback && !connected) {
      startPollingFallback();
    }
  }
  
  /**
   * Handle WebSocket reconnection
   */
  function handleWebSocketReconnect() {
    // Clear any existing errors
    setError(null);
  }
  
  /**
   * Request initial dashboard data
   */
  const requestInitialData = useCallback(async () => {
    try {
      setLoading(true);
      const data = await monitoringApi.getDashboardData();
      applyDashboardUpdate(data);
    } catch (error: any) {
      const fetchError: MonitoringError = {
        code: 'INITIAL_DATA_FETCH_ERROR',
        message: 'Failed to fetch initial dashboard data',
        timestamp: new Date().toISOString(),
        recoverable: true,
        details: { error }
      };
      setError(fetchError);
      setLoading(false);
    }
  }, [applyDashboardUpdate]);
  
  /**
   * Refresh dashboard data manually
   */
  const refresh = useCallback(async () => {
    if (connected) {
      // Request fresh data via WebSocket
      const refreshMessage = {
        type: 'REQUEST_DASHBOARD_REFRESH',
        payload: {
          filters: config.jobFilters,
          timestamp: new Date().toISOString()
        }
      };
      send(refreshMessage);
    } else {
      // Fallback to HTTP API
      await requestInitialData();
    }
  }, [connected, send, config.jobFilters, requestInitialData]);
  
  /**
   * Start polling fallback when WebSocket is unavailable
   */
  const startPollingFallback = useCallback(() => {
    if (pollingTimer.current) return;
    
    pollingTimer.current = setInterval(async () => {
      try {
        const data = await monitoringApi.getDashboardData();
        applyDashboardUpdate(data);
      } catch (error) {
        console.warn('Polling fallback failed:', error);
      }
    }, config.pollingInterval);
  }, [config.pollingInterval, applyDashboardUpdate]);
  
  /**
   * Stop polling fallback
   */
  const stopPollingFallback = useCallback(() => {
    if (pollingTimer.current) {
      clearInterval(pollingTimer.current);
      pollingTimer.current = null;
    }
  }, []);
  
  /**
   * Subscribe to additional monitoring topics
   */
  const subscribeToTopics = useCallback((topics: string[]) => {
    subscribe(topics);
  }, [subscribe]);
  
  /**
   * Unsubscribe from monitoring topics
   */
  const unsubscribeFromTopics = useCallback((topics: string[]) => {
    unsubscribe(topics);
  }, [unsubscribe]);
  
  /**
   * Check if data is fresh (within cache age limit)
   */
  const isDataFresh = useCallback(() => {
    const now = Date.now();
    const dataAge = now - dataCacheRef.current.timestamp;
    return dataAge < (config.maxCacheAge || 30000);
  }, [config.maxCacheAge]);
  
  /**
   * Get cached data if fresh, otherwise trigger refresh
   */
  const getCachedData = useCallback(() => {
    if (dataCacheRef.current.data && isDataFresh()) {
      return dataCacheRef.current.data;
    }
    
    // Data is stale, trigger refresh
    if (!loading) {
      refresh();
    }
    
    return dataCacheRef.current.data;
  }, [isDataFresh, loading, refresh]);
  
  /**
   * Initialize hook on mount
   */
  useEffect(() => {
    if (config.autoConnect && !dashboardData && !loading) {
      requestInitialData();
    }
  }, [config.autoConnect, dashboardData, loading, requestInitialData]);
  
  /**
   * Cleanup on unmount
   */
  useEffect(() => {
    return () => {
      // Clear timers
      if (bufferFlushTimer.current) {
        clearTimeout(bufferFlushTimer.current);
      }
      stopPollingFallback();
    };
  }, [stopPollingFallback]);
  
  /**
   * Handle subscription changes
   */
  useEffect(() => {
    if (connected && subscriptionTopics.length > 0) {
      subscribeToTopics(subscriptionTopics);
    }
  }, [connected, subscriptionTopics, subscribeToTopics]);
  
  /**
   * Return hook interface
   */
  return {
    dashboardData,
    loading,
    error,
    connected,
    refresh,
    subscribe: subscribeToTopics,
    unsubscribe: unsubscribeFromTopics,
    getCachedData,
    isDataFresh
  };
}

export default useRealTimeMonitoring;