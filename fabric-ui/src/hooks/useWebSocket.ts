/**
 * useWebSocket Hook
 * 
 * React hook for managing WebSocket connections with automatic cleanup,
 * state management, and error handling. Integrates seamlessly with the
 * WebSocketService for enterprise-grade real-time functionality.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 * 
 * Features:
 * - Automatic connection management
 * - State synchronization with React
 * - Error boundary integration
 * - Connection lifecycle management
 * - Authentication token handling
 * - Message subscription management
 */

import { useEffect, useRef, useCallback, useState } from 'react';
import { WebSocketService, WebSocketState, WebSocketConfig } from '../services/websocket/WebSocketService';
import { WebSocketMessage, MonitoringError, UseWebSocketReturn } from '../types/monitoring';
import { useAuth } from '../contexts/AuthContext';

// Hook options interface
export interface UseWebSocketOptions extends Partial<WebSocketConfig> {
  // Connection options
  autoConnect?: boolean;
  reconnectOnTokenRefresh?: boolean;
  
  // Event callbacks
  onConnect?: () => void;
  onDisconnect?: (event: any) => void;
  onMessage?: (message: WebSocketMessage) => void;
  onError?: (error: MonitoringError) => void;
  onReconnect?: (attempt: number) => void;
  
  // Subscription management
  subscriptions?: string[];
  
  // Performance options
  messageBufferSize?: number;
  connectionTimeout?: number;
}

// Default options
const DEFAULT_OPTIONS: UseWebSocketOptions = {
  autoConnect: true,
  reconnectOnTokenRefresh: true,
  reconnectAttempts: 5,
  reconnectInterval: 1000,
  heartbeatInterval: 30000,
  connectionTimeout: 10000,
  messageQueueSize: 100,
  subscriptions: [],
  debugMode: process.env.NODE_ENV === 'development'
};

/**
 * useWebSocket Hook
 * 
 * Custom React hook for managing WebSocket connections with comprehensive
 * state management, authentication, and error handling.
 * 
 * @param url - WebSocket server URL
 * @param options - Configuration options for the WebSocket connection
 * @returns UseWebSocketReturn object with connection state and methods
 */
export function useWebSocket(
  url: string, 
  options: UseWebSocketOptions = {}
): UseWebSocketReturn {
  
  // Merge options with defaults
  const config = { ...DEFAULT_OPTIONS, ...options };
  
  // Authentication context
  const { accessToken, refreshAccessToken } = useAuth();
  
  // WebSocket service reference
  const wsServiceRef = useRef<WebSocketService | null>(null);
  
  // Component state
  const [connected, setConnected] = useState(false);
  const [connecting, setConnecting] = useState(false);
  const [error, setError] = useState<MonitoringError | null>(null);
  const [lastMessage, setLastMessage] = useState<WebSocketMessage | null>(null);
  
  // Message buffer for high-frequency updates
  const messageBufferRef = useRef<WebSocketMessage[]>([]);
  const bufferFlushTimer = useRef<NodeJS.Timeout | null>(null);
  
  // Track component mount state for cleanup
  const isMountedRef = useRef(true);
  
  /**
   * Initialize WebSocket service
   */
  const initializeWebSocketService = useCallback(() => {
    if (wsServiceRef.current) {
      return wsServiceRef.current;
    }
    
    const wsConfig: WebSocketConfig = {
      url,
      protocols: config.protocols,
      reconnectAttempts: config.reconnectAttempts || 5,
      reconnectInterval: config.reconnectInterval || 1000,
      heartbeatInterval: config.heartbeatInterval || 30000,
      connectionTimeout: config.connectionTimeout || 10000,
      messageQueueSize: config.messageQueueSize || 100,
      enableCompression: config.enableCompression !== false,
      debugMode: config.debugMode || false
    };
    
    const service = new WebSocketService(wsConfig);
    
    // Set up event listeners
    service.on('connect', handleConnect);
    service.on('disconnect', handleDisconnect);
    service.on('message', handleMessage);
    service.on('error', handleError);
    service.on('reconnect', handleReconnect);
    
    wsServiceRef.current = service;
    return service;
  }, [url, config]);
  
  /**
   * Handle WebSocket connection established
   */
  const handleConnect = useCallback(() => {
    if (!isMountedRef.current) return;
    
    setConnected(true);
    setConnecting(false);
    setError(null);
    
    // Subscribe to configured topics
    if (config.subscriptions && config.subscriptions.length > 0) {
      wsServiceRef.current?.subscribe(config.subscriptions);
    }
    
    // Call user-defined callback
    config.onConnect?.();
    
    if (config.debugMode) {
      console.log('WebSocket connected successfully');
    }
  }, [config]);
  
  /**
   * Handle WebSocket connection closed
   */
  const handleDisconnect = useCallback((data: any) => {
    if (!isMountedRef.current) return;
    
    setConnected(false);
    setConnecting(false);
    
    // Clear message buffer
    messageBufferRef.current = [];
    if (bufferFlushTimer.current) {
      clearTimeout(bufferFlushTimer.current);
      bufferFlushTimer.current = null;
    }
    
    // Call user-defined callback
    config.onDisconnect?.(data);
    
    if (config.debugMode) {
      console.log('WebSocket disconnected:', data);
    }
  }, [config]);
  
  /**
   * Handle incoming WebSocket messages
   */
  const handleMessage = useCallback((message: WebSocketMessage) => {
    if (!isMountedRef.current) return;
    
    // Use message buffering for high-frequency updates
    if (config.messageBufferSize && config.messageBufferSize > 1) {
      messageBufferRef.current.push(message);
      
      if (messageBufferRef.current.length >= config.messageBufferSize) {
        flushMessageBuffer();
      } else if (!bufferFlushTimer.current) {
        bufferFlushTimer.current = setTimeout(flushMessageBuffer, 100);
      }
    } else {
      // Process message immediately
      processMessage(message);
    }
  }, [config]);
  
  /**
   * Process individual message
   */
  const processMessage = useCallback((message: WebSocketMessage) => {
    setLastMessage(message);
    config.onMessage?.(message);
  }, [config]);
  
  /**
   * Flush buffered messages
   */
  const flushMessageBuffer = useCallback(() => {
    if (messageBufferRef.current.length > 0) {
      const messages = [...messageBufferRef.current];
      messageBufferRef.current = [];
      
      // Process the latest message (most recent state)
      const latestMessage = messages[messages.length - 1];
      processMessage(latestMessage);
    }
    
    if (bufferFlushTimer.current) {
      clearTimeout(bufferFlushTimer.current);
      bufferFlushTimer.current = null;
    }
  }, [processMessage]);
  
  /**
   * Handle WebSocket errors
   */
  const handleError = useCallback((error: MonitoringError) => {
    if (!isMountedRef.current) return;
    
    setError(error);
    setConnecting(false);
    
    // Call user-defined callback
    config.onError?.(error);
    
    if (config.debugMode) {
      console.error('WebSocket error:', error);
    }
  }, [config]);
  
  /**
   * Handle reconnection attempts
   */
  const handleReconnect = useCallback((data: { attempt: number }) => {
    if (!isMountedRef.current) return;
    
    setConnecting(true);
    setError(null);
    
    // Call user-defined callback
    config.onReconnect?.(data.attempt);
    
    if (config.debugMode) {
      console.log(`WebSocket reconnection attempt ${data.attempt}`);
    }
  }, [config]);
  
  /**
   * Connect to WebSocket server
   */
  const connect = useCallback(async () => {
    if (!accessToken) {
      const authError: MonitoringError = {
        code: 'AUTH_TOKEN_MISSING',
        message: 'Authentication token not available',
        timestamp: new Date().toISOString(),
        recoverable: true
      };
      setError(authError);
      return;
    }
    
    try {
      setConnecting(true);
      setError(null);
      
      const service = initializeWebSocketService();
      await service.connect(accessToken);
      
    } catch (error: any) {
      const wsError: MonitoringError = {
        code: 'CONNECTION_FAILED',
        message: error.message || 'Failed to connect to WebSocket server',
        timestamp: new Date().toISOString(),
        recoverable: true,
        details: { error }
      };
      setError(wsError);
      setConnecting(false);
    }
  }, [accessToken, initializeWebSocketService]);
  
  /**
   * Disconnect from WebSocket server
   */
  const disconnect = useCallback(() => {
    wsServiceRef.current?.disconnect();
    setConnected(false);
    setConnecting(false);
    setError(null);
  }, []);
  
  /**
   * Reconnect to WebSocket server
   */
  const reconnect = useCallback(async () => {
    disconnect();
    await new Promise(resolve => setTimeout(resolve, 1000)); // Brief delay
    await connect();
  }, [disconnect, connect]);
  
  /**
   * Send message to WebSocket server
   */
  const send = useCallback((message: any) => {
    if (!wsServiceRef.current?.isConnected()) {
      const error: MonitoringError = {
        code: 'NOT_CONNECTED',
        message: 'WebSocket is not connected',
        timestamp: new Date().toISOString(),
        recoverable: true
      };
      setError(error);
      return;
    }
    
    const wsMessage: WebSocketMessage = {
      type: 'CLIENT_MESSAGE',
      payload: message,
      timestamp: new Date().toISOString(),
      correlationId: generateCorrelationId()
    };
    
    wsServiceRef.current.send(wsMessage);
  }, []);
  
  /**
   * Subscribe to additional topics
   */
  const subscribe = useCallback((topics: string[]) => {
    wsServiceRef.current?.subscribe(topics);
  }, []);
  
  /**
   * Unsubscribe from topics
   */
  const unsubscribe = useCallback((topics: string[]) => {
    wsServiceRef.current?.unsubscribe(topics);
  }, []);
  
  /**
   * Generate correlation ID for message tracking
   */
  const generateCorrelationId = useCallback(() => {
    return `hook_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }, []);
  
  /**
   * Effect for auto-connection
   */
  useEffect(() => {
    if (config.autoConnect && accessToken && !connected && !connecting) {
      connect();
    }
  }, [config.autoConnect, accessToken, connected, connecting, connect]);
  
  /**
   * Effect for token refresh handling
   */
  useEffect(() => {
    if (config.reconnectOnTokenRefresh && accessToken && wsServiceRef.current?.isConnected()) {
      wsServiceRef.current.updateAuthToken(accessToken);
    }
  }, [accessToken, config.reconnectOnTokenRefresh]);
  
  /**
   * Effect for subscription management
   */
  useEffect(() => {
    if (connected && config.subscriptions && config.subscriptions.length > 0) {
      subscribe(config.subscriptions);
    }
  }, [connected, config.subscriptions, subscribe]);
  
  /**
   * Cleanup effect
   */
  useEffect(() => {
    return () => {
      isMountedRef.current = false;
      
      // Clear timers
      if (bufferFlushTimer.current) {
        clearTimeout(bufferFlushTimer.current);
      }
      
      // Disconnect WebSocket
      if (wsServiceRef.current) {
        wsServiceRef.current.disconnect();
        wsServiceRef.current = null;
      }
    };
  }, []);
  
  /**
   * Return hook interface
   */
  return {
    connected,
    connecting,
    error,
    send,
    connect,
    disconnect,
    reconnect,
    lastMessage,
    subscribe,
    unsubscribe
  };
}

export default useWebSocket;