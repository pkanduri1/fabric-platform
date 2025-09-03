/**
 * Monitoring Dashboard Types
 * 
 * Comprehensive type definitions for real-time job monitoring dashboard.
 * Supports WebSocket-based real-time updates, performance metrics,
 * alerts management, and mobile-responsive design.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 * 
 * Features:
 * - Real-time job status tracking
 * - Performance metrics and analytics
 * - Alert management with configurable thresholds
 * - Historical trend analysis
 * - Mobile-responsive data structures
 * - Security-compliant role-based access
 */

// Paginated response interface for API endpoints
export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  size: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// Job Status Enums
export enum JobStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
  PAUSED = 'PAUSED',
  RETRYING = 'RETRYING'
}

export enum JobPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export enum AlertSeverity {
  INFO = 'INFO',
  WARNING = 'WARNING',
  ERROR = 'ERROR',
  CRITICAL = 'CRITICAL'
}

export enum AlertType {
  PERFORMANCE = 'PERFORMANCE',
  ERROR_RATE = 'ERROR_RATE',
  THRESHOLD_BREACH = 'THRESHOLD_BREACH',
  SYSTEM_HEALTH = 'SYSTEM_HEALTH',
  DEPENDENCY_FAILURE = 'DEPENDENCY_FAILURE'
}

export enum TrendIndicator {
  IMPROVING = 'IMPROVING',
  STABLE = 'STABLE',
  DEGRADING = 'DEGRADING'
}

// Core Job Monitoring Types
export interface ActiveJob {
  executionId: string;
  jobName: string;
  sourceSystem: string;
  status: JobStatus;
  priority: JobPriority;
  startTime: string;
  estimatedEndTime?: string;
  progress: number; // 0-100
  recordsProcessed: number;
  totalRecords?: number;
  currentStage: string;
  throughputPerSecond: number;
  errorCount: number;
  warningCount: number;
  performanceScore: number; // 0-100
  trendIndicator: TrendIndicator;
  lastHeartbeat: string;
  assignedNode?: string;
  correlationId: string;
}

// Performance Metrics
export interface PerformanceMetrics {
  totalThroughput: number;
  averageExecutionTime: number;
  successRate: number;
  errorRate: number;
  memoryUsage: number;
  cpuUsage: number;
  activeConnections: number;
  queueDepth: number;
  systemHealthScore: number;
  timestamp: string;
}

// System Health
export interface SystemHealth {
  overallScore: number; // 0-100
  database: {
    status: 'HEALTHY' | 'DEGRADED' | 'DOWN';
    responseTime: number;
    connectionPool: number;
  };
  webSocket: {
    status: 'HEALTHY' | 'DEGRADED' | 'DOWN';
    activeConnections: number;
    messageRate: number;
  };
  batchProcessing: {
    status: 'HEALTHY' | 'DEGRADED' | 'DOWN';
    activeJobs: number;
    queueLength: number;
  };
  memory: {
    status: 'HEALTHY' | 'WARNING' | 'CRITICAL';
    used: number;
    available: number;
    percentage: number;
  };
  lastCheck: string;
}

// Alert Types
export interface Alert {
  alertId: string;
  type: AlertType;
  severity: AlertSeverity;
  title: string;
  description: string;
  jobExecutionId?: string;
  sourceSystem?: string;
  threshold?: number;
  currentValue?: number;
  timestamp: string;
  acknowledged: boolean;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
  resolved: boolean;
  resolvedAt?: string;
  escalated: boolean;
  escalationLevel?: number;
  correlationId: string;
  affectedResources: string[];
}

export interface AlertFilters {
  severity?: AlertSeverity[];
  type?: AlertType[];
  sourceSystem?: string[];
  acknowledged?: boolean;
  resolved?: boolean;
  dateRange?: {
    start: string;
    end: string;
  };
}

export interface AlertConfiguration {
  configId: string;
  name: string;
  type: AlertType;
  severity: AlertSeverity;
  conditions: AlertCondition[];
  enabled: boolean;
  notificationChannels: string[];
  escalationRules: EscalationRule[];
  createdBy: string;
  createdAt: string;
  modifiedBy?: string;
  modifiedAt?: string;
}

export interface AlertCondition {
  metric: string;
  operator: 'GT' | 'LT' | 'EQ' | 'GTE' | 'LTE' | 'CONTAINS';
  threshold: number | string;
  duration?: number; // in seconds
}

export interface EscalationRule {
  level: number;
  delayMinutes: number;
  recipients: string[];
  channels: string[];
}

// Job Details for Drill-down Analysis
export interface JobExecutionDetails {
  executionId: string;
  jobName: string;
  sourceSystem: string;
  status: JobStatus;
  startTime: string;
  endTime?: string;
  duration?: number;
  totalRecords: number;
  processedRecords: number;
  errorRecords: number;
  skippedRecords: number;
  
  // Performance Details
  performance: {
    averageThroughput: number;
    peakThroughput: number;
    memoryUsage: MemoryUsageHistory[];
    cpuUsage: CpuUsageHistory[];
    ioMetrics: IoMetrics;
    networkMetrics: NetworkMetrics;
  };
  
  // Stage Details
  stages: JobStageDetail[];
  
  // Error Information
  errors: JobError[];
  warnings: JobWarning[];
  
  // Data Lineage
  dataLineage: DataLineageInfo;
  
  // Audit Trail
  auditTrail: AuditEvent[];
  
  // Dependencies
  dependencies: JobDependency[];
}

export interface JobStageDetail {
  stageName: string;
  status: JobStatus;
  startTime: string;
  endTime?: string;
  duration?: number;
  recordsProcessed: number;
  throughput: number;
  memoryUsage: number;
  errorCount: number;
  retryCount: number;
}

export interface JobError {
  errorId: string;
  timestamp: string;
  stage: string;
  errorType: string;
  errorMessage: string;
  stackTrace?: string;
  recordContext?: string;
  recoverable: boolean;
  retryAttempt?: number;
}

export interface JobWarning {
  warningId: string;
  timestamp: string;
  stage: string;
  warningType: string;
  message: string;
  recordContext?: string;
}

export interface MemoryUsageHistory {
  timestamp: string;
  used: number;
  available: number;
  percentage: number;
}

export interface CpuUsageHistory {
  timestamp: string;
  usage: number;
  cores: number;
}

export interface IoMetrics {
  readOperations: number;
  writeOperations: number;
  readThroughput: number;
  writeThroughput: number;
  averageReadTime: number;
  averageWriteTime: number;
}

export interface NetworkMetrics {
  bytesReceived: number;
  bytesSent: number;
  requestCount: number;
  averageResponseTime: number;
  errorRate: number;
}

export interface DataLineageInfo {
  sourceFiles: string[];
  targetFiles: string[];
  transformations: string[];
  dataQualityChecks: DataQualityCheck[];
  checksums: Record<string, string>;
}

export interface DataQualityCheck {
  checkName: string;
  status: 'PASSED' | 'FAILED' | 'WARNING';
  message: string;
  recordsAffected: number;
  timestamp: string;
}

export interface AuditEvent {
  eventId: string;
  timestamp: string;
  eventType: string;
  userId?: string;
  description: string;
  details: Record<string, any>;
  correlationId: string;
}

export interface JobDependency {
  dependencyId: string;
  dependencyType: 'JOB' | 'FILE' | 'DATABASE' | 'SERVICE';
  dependencyName: string;
  status: 'SATISFIED' | 'PENDING' | 'FAILED';
  checkTime: string;
}

// Historical Analytics
export interface TrendData {
  timestamp: string;
  value: number;
  label?: string;
}

export interface HistoricalMetrics {
  period: 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';
  startDate: string;
  endDate: string;
  jobExecutionTrends: TrendData[];
  throughputTrends: TrendData[];
  errorRateTrends: TrendData[];
  performanceScoreTrends: TrendData[];
  systemHealthTrends: TrendData[];
}

// Dashboard Data Structure
export interface DashboardData {
  activeJobs: ActiveJob[];
  recentCompletions: ActiveJob[];
  performanceMetrics: PerformanceMetrics;
  systemHealth: SystemHealth;
  alerts: Alert[];
  trends: HistoricalMetrics;
  lastUpdate: string;
  correlationId: string;
}

// WebSocket Message Types
export interface WebSocketMessage {
  type: 'DASHBOARD_UPDATE' | 'JOB_UPDATE' | 'ALERT' | 'SYSTEM_HEALTH' | 'ERROR' | 'CLIENT_MESSAGE' | 'HEARTBEAT' | 'HEARTBEAT_ACK' | 'SUBSCRIBE' | 'UNSUBSCRIBE' | 'REAUTH';
  payload: any;
  timestamp: string;
  correlationId: string;
}

export interface WebSocketConnectionInfo {
  sessionId: string;
  userId: string;
  connectedAt: string;
  lastActivity: string;
  subscriptions: string[];
}

// Configuration and Preferences
export interface MonitoringConfiguration {
  updateInterval: number; // in seconds
  alertThresholds: Record<string, number>;
  displayPreferences: {
    darkMode: boolean;
    showTrends: boolean;
    showPerformanceCharts: boolean;
    autoRefresh: boolean;
    compactView: boolean;
  };
  notifications: {
    soundEnabled: boolean;
    browserNotifications: boolean;
    emailNotifications: boolean;
  };
  filters: {
    showCompletedJobs: boolean;
    jobTypes: string[];
    sourceSystems: string[];
  };
}

// Mobile-Responsive Types
export interface ResponsiveBreakpoints {
  xs: boolean; // < 600px
  sm: boolean; // 600px - 960px
  md: boolean; // 960px - 1280px
  lg: boolean; // 1280px - 1920px
  xl: boolean; // > 1920px
}

export interface MobileOptimizedJob {
  executionId: string;
  jobName: string;
  status: JobStatus;
  progress: number;
  errorCount: number;
  duration: string;
  priority: JobPriority;
}

// API Response Types
export interface MonitoringApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp: string;
  correlationId: string;
}

// Note: PaginatedResponse is imported from template.ts to avoid duplication

// Chart Data Types
export interface ChartDataPoint {
  x: string | number;
  y: number;
  label?: string;
  color?: string;
}

export interface ChartConfiguration {
  type: 'line' | 'bar' | 'doughnut' | 'gauge' | 'area';
  title: string;
  xAxisLabel?: string;
  yAxisLabel?: string;
  showLegend: boolean;
  responsive: boolean;
  realTimeUpdates: boolean;
}

// Error Handling Types
export interface MonitoringError {
  code: string;
  message: string;
  details?: Record<string, any>;
  timestamp: string;
  recoverable: boolean;
}

// Component Props Types
export interface DashboardProps {
  className?: string;
  onJobSelect?: (jobId: string) => void;
  onAlertAcknowledge?: (alertId: string) => void;
  configuration?: Partial<MonitoringConfiguration>;
}

export interface JobGridProps {
  jobs: ActiveJob[];
  loading?: boolean;
  onJobSelect?: (jobId: string) => void;
  onRefresh?: () => void;
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
  filters?: Record<string, any>;
  compactView?: boolean;
}

export interface AlertPanelProps {
  alerts: Alert[];
  loading?: boolean;
  onAcknowledge?: (alertId: string) => void;
  onResolve?: (alertId: string) => void;
  onConfigure?: () => void;
  filters?: AlertFilters;
  soundEnabled?: boolean;
}

export interface PerformanceChartProps {
  metrics: PerformanceMetrics[];
  type?: 'realtime' | 'historical';
  timeRange?: string;
  refreshInterval?: number;
  onTimeRangeChange?: (range: string) => void;
  trends?: any; // TODO: Define proper trends interface
  height?: number;
  realTime?: boolean;
}

export interface JobDetailsModalProps {
  executionId: string;
  open: boolean;
  onClose: () => void;
  onRefresh?: () => void;
}

// Hook Return Types
export interface UseWebSocketReturn {
  connected: boolean;
  connecting: boolean;
  error: MonitoringError | null;
  send: (message: any) => void;
  connect: () => Promise<void>;
  disconnect: () => void;
  reconnect: () => Promise<void>;
  lastMessage: WebSocketMessage | null;
  subscribe: (topics: string[]) => void;
  unsubscribe: (topics: string[]) => void;
}

export interface UseRealTimeMonitoringReturn {
  dashboardData: DashboardData | null;
  loading: boolean;
  error: MonitoringError | null;
  connected: boolean;
  refresh: () => Promise<void>;
  subscribe: (topics: string[]) => void;
  unsubscribe: (topics: string[]) => void;
  getCachedData: () => DashboardData | null;
  isDataFresh: () => boolean;
}

// Note: All types are already exported as named exports above
// No need for additional export statements since interfaces and types are already exported with their definitions