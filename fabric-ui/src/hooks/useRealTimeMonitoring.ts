import { useState, useEffect, useCallback } from 'react';
import { useWebSocket } from './useWebSocket';
import { DashboardData, MonitoringError, UseRealTimeMonitoringReturn } from '../types/monitoring';

const DEFAULT_DASHBOARD_DATA: DashboardData = {
  activeJobs: [],
  recentCompletions: [],
  performanceMetrics: {
    totalThroughput: 0,
    averageExecutionTime: 0,
    successRate: 100,
    errorRate: 0,
    memoryUsage: 50,
    cpuUsage: 30,
    activeConnections: 10,
    queueDepth: 0,
    systemHealthScore: 95,
    timestamp: new Date().toISOString()
  },
  alerts: [],
  systemHealth: {
    overallScore: 95,
    database: {
      status: 'HEALTHY',
      responseTime: 50,
      connectionPool: 10
    },
    webSocket: {
      status: 'HEALTHY',
      activeConnections: 5,
      messageRate: 10
    },
    batchProcessing: {
      status: 'HEALTHY',
      activeJobs: 3,
      queueLength: 0
    },
    memory: {
      status: 'HEALTHY',
      used: 50,
      available: 50,
      percentage: 50
    },
    lastCheck: new Date().toISOString()
  },
  trends: {
    period: 'DAY',
    startDate: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
    endDate: new Date().toISOString(),
    jobExecutionTrends: [],
    throughputTrends: [],
    errorRateTrends: [],
    performanceScoreTrends: [],
    systemHealthTrends: []
  },
  lastUpdate: new Date().toISOString(),
  correlationId: `corr_${Date.now()}`
};

export const useRealTimeMonitoring = (options?: any): UseRealTimeMonitoringReturn => {
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(DEFAULT_DASHBOARD_DATA);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<MonitoringError | null>(null);
  const [lastUpdate, setLastUpdate] = useState<Date | null>(new Date());
  const [cache, setCache] = useState<Map<string, any>>(new Map());

  const { connected: isConnected, reconnect } = useWebSocket(
    '/api/v2/monitoring/websocket',
    {
      onMessage: (message) => {
        try {
          const data = JSON.parse(message.payload || '{}') as DashboardData;
          setDashboardData(data);
          setLastUpdate(new Date());
          setError(null);
        } catch (err) {
          console.error('Failed to parse monitoring data:', err);
          setError({
            code: 'PARSE_ERROR',
            message: 'Failed to parse monitoring data',
            timestamp: new Date().toISOString(),
            recoverable: false
          });
        }
      },
      onError: (error) => {
        setError(error);
      },
      onDisconnect: () => {
        setError({
          code: 'CONNECTION_LOST',
          message: 'Connection lost',
          timestamp: new Date().toISOString(),
          recoverable: true
        });
      }
    }
  );

  // Get cached data
  const getCachedData = useCallback(() => {
    return dashboardData;
  }, [dashboardData]);

  // Check if data is fresh
  const isDataFresh = useCallback(() => {
    if (!dashboardData?.lastUpdate) return false;
    return Date.now() - new Date(dashboardData.lastUpdate).getTime() < 30000;
  }, [dashboardData]);

  // Refresh data
  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      await reconnect();
    } finally {
      setLoading(false);
    }
  }, [reconnect]);

  // Subscribe/unsubscribe functions (placeholder)
  const subscribe = useCallback((topics: string[]) => {
    console.log('Subscribing to topics:', topics);
  }, []);

  const unsubscribe = useCallback((topics: string[]) => {
    console.log('Unsubscribing from topics:', topics);
  }, []);

  // Update cache when data changes
  useEffect(() => {
    if (dashboardData) {
      setCache(prev => new Map(prev.set('dashboard_data', dashboardData)));
    }
  }, [dashboardData]);

  return {
    dashboardData,
    loading,
    error,
    connected: isConnected,
    refresh,
    subscribe,
    unsubscribe,
    getCachedData,
    isDataFresh
  };
};

export default useRealTimeMonitoring;