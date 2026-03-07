import { useState, useEffect, useCallback, useRef } from 'react';
import { DashboardData, MonitoringError, UseRealTimeMonitoringReturn } from '../types/monitoring';
import axios from 'axios';

const POLL_INTERVAL = 15000; // 15 seconds

export const useRealTimeMonitoring = (options?: any): UseRealTimeMonitoringReturn => {
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<MonitoringError | null>(null);
  const pollTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchDashboard = useCallback(async () => {
    try {
      const token = localStorage.getItem('fabric_access_token');
      const headers: Record<string, string> = { 'Content-Type': 'application/json' };
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const response = await axios.get('/api/monitoring/dashboard', { headers, timeout: 10000 });
      const payload = response.data;
      const data: DashboardData = payload.data || payload;
      setDashboardData(data);
      setError(null);
    } catch (err: any) {
      console.warn('Monitoring dashboard fetch failed:', err.message);
      setError({
        code: 'FETCH_ERROR',
        message: err.message || 'Failed to fetch dashboard data',
        timestamp: new Date().toISOString(),
        recoverable: true
      });
    } finally {
      setLoading(false);
    }
  }, []);

  // Initial fetch + polling
  useEffect(() => {
    fetchDashboard();
    pollTimerRef.current = setInterval(fetchDashboard, POLL_INTERVAL);
    return () => {
      if (pollTimerRef.current) clearInterval(pollTimerRef.current);
    };
  }, [fetchDashboard]);

  const getCachedData = useCallback(() => dashboardData, [dashboardData]);

  const isDataFresh = useCallback(() => {
    if (!dashboardData?.lastUpdate) return false;
    return Date.now() - new Date(dashboardData.lastUpdate).getTime() < 30000;
  }, [dashboardData]);

  const refresh = useCallback(async () => {
    setLoading(true);
    await fetchDashboard();
  }, [fetchDashboard]);

  const subscribe = useCallback((_topics: string[]) => {}, []);
  const unsubscribe = useCallback((_topics: string[]) => {}, []);

  return {
    dashboardData,
    loading,
    error,
    connected: !error,
    refresh,
    subscribe,
    unsubscribe,
    getCachedData,
    isDataFresh
  };
};

export default useRealTimeMonitoring;