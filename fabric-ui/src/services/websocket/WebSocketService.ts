/**
 * WebSocket Service for Real-Time Job Monitoring
 * 
 * Enterprise-grade WebSocket client implementation with comprehensive
 * security, reliability, and performance features for real-time job monitoring.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 * 
 * Features:
 * - JWT-authenticated WebSocket connections
 * - Automatic reconnection with exponential backoff
 * - Message routing and subscription management
 * - Connection health monitoring
 * - CSRF protection and security headers
 * - Heartbeat and keepalive mechanisms
 * - Performance monitoring and metrics
 * - Error handling and recovery
 */

import { WebSocketMessage, MonitoringError } from '../../types/monitoring';

// Configuration interface
export interface WebSocketConfig {
  url: string;
  protocols?: string[];
  reconnectAttempts: number;
  reconnectInterval: number;
  heartbeatInterval: number;
  connectionTimeout: number;
  messageQueueSize: number;
  enableCompression: boolean;
  debugMode: boolean;
}

// Connection states
export enum WebSocketState {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTING = 'DISCONNECTING',
  DISCONNECTED = 'DISCONNECTED',
  RECONNECTING = 'RECONNECTING',
  ERROR = 'ERROR'
}

// Event types
export type WebSocketEventType = 
  | 'connect'
  | 'disconnect' 
  | 'message'
  | 'error'
  | 'reconnect'
  | 'heartbeat';

// Event listener type
export type WebSocketEventListener = (data?: any) => void;

// Default configuration
const DEFAULT_CONFIG: WebSocketConfig = {
  url: '',
  protocols: ['monitoring-v1'],
  reconnectAttempts: 5,
  reconnectInterval: 1000,
  heartbeatInterval: 30000,
  connectionTimeout: 10000,
  messageQueueSize: 100,
  enableCompression: true,
  debugMode: false
};

/**
 * WebSocket Service Class
 * 
 * Manages WebSocket connections with enterprise-grade features including
 * authentication, automatic reconnection, message queuing, and health monitoring.
 */
export class WebSocketService {
  private ws: WebSocket | null = null;
  private config: WebSocketConfig;
  private state: WebSocketState = WebSocketState.DISCONNECTED;
  private reconnectCount = 0;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private heartbeatTimer: NodeJS.Timeout | null = null;
  private connectionTimer: NodeJS.Timeout | null = null;
  
  // Event listeners
  private eventListeners: Map<WebSocketEventType, WebSocketEventListener[]> = new Map();
  
  // Message queue for offline scenarios
  private messageQueue: WebSocketMessage[] = [];
  
  // Subscription management
  private subscriptions: Set<string> = new Set();
  
  // Performance metrics
  private metrics = {
    connectionsEstablished: 0,
    messagesReceived: 0,
    messagesSent: 0,
    reconnections: 0,
    errors: 0,
    lastHeartbeat: null as Date | null,
    connectionDuration: 0,
    averageLatency: 0
  };
  
  // Authentication token
  private authToken: string | null = null;
  
  constructor(config: Partial<WebSocketConfig> = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config };
    this.initializeEventListeners();
    
    if (this.config.debugMode) {
      console.log('WebSocketService initialized with config:', this.config);
    }
  }
  
  /**
   * Initialize default event listeners
   */
  private initializeEventListeners(): void {
    // Set up default event listener maps
    Object.values(['connect', 'disconnect', 'message', 'error', 'reconnect', 'heartbeat']).forEach(event => {
      this.eventListeners.set(event as WebSocketEventType, []);
    });
  }
  
  /**
   * Connect to WebSocket server with JWT authentication
   */
  public async connect(authToken: string): Promise<void> {
    if (this.state === WebSocketState.CONNECTED || this.state === WebSocketState.CONNECTING) {
      if (this.config.debugMode) {
        console.log('WebSocket already connected or connecting');
      }
      return;
    }
    
    this.authToken = authToken;
    this.state = WebSocketState.CONNECTING;
    
    try {
      // Construct WebSocket URL with authentication
      const wsUrl = this.buildWebSocketUrl();
      
      // Create WebSocket connection
      this.ws = new WebSocket(wsUrl, this.config.protocols);
      
      // Set up connection timeout
      this.connectionTimer = setTimeout(() => {
        if (this.state === WebSocketState.CONNECTING) {
          this.handleConnectionTimeout();
        }
      }, this.config.connectionTimeout);
      
      // Set up event handlers
      this.setupWebSocketHandlers();
      
    } catch (error) {
      this.handleError('Connection failed', error);
      throw error;
    }
  }
  
  /**
   * Build WebSocket URL with authentication parameters
   */
  private buildWebSocketUrl(): string {
    const baseUrl = this.config.url;
    const params = new URLSearchParams();
    
    if (this.authToken) {
      params.append('token', this.authToken);
    }
    
    // Add CSRF protection
    const csrfToken = this.getCSRFToken();
    if (csrfToken) {
      params.append('csrf', csrfToken);
    }
    
    // Add client identification
    params.append('client', 'fabric-ui');
    params.append('version', '1.0');
    params.append('timestamp', Date.now().toString());
    
    return `${baseUrl}?${params.toString()}`;
  }
  
  /**
   * Get CSRF token from meta tag or cookie
   */
  private getCSRFToken(): string | null {
    // Try to get CSRF token from meta tag
    const metaTag = document.querySelector('meta[name="csrf-token"]');
    if (metaTag) {
      return metaTag.getAttribute('content');
    }
    
    // Fallback to cookie
    const cookieMatch = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
    return cookieMatch ? decodeURIComponent(cookieMatch[1]) : null;
  }
  
  /**
   * Set up WebSocket event handlers
   */
  private setupWebSocketHandlers(): void {
    if (!this.ws) return;
    
    this.ws.onopen = (event) => {
      this.handleConnectionOpen(event);
    };
    
    this.ws.onmessage = (event) => {
      this.handleMessage(event);
    };
    
    this.ws.onclose = (event) => {
      this.handleConnectionClose(event);
    };
    
    this.ws.onerror = (event) => {
      this.handleWebSocketError(event);
    };
  }
  
  /**
   * Handle successful connection
   */
  private handleConnectionOpen(event: Event): void {
    this.clearConnectionTimer();
    this.state = WebSocketState.CONNECTED;
    this.reconnectCount = 0;
    this.metrics.connectionsEstablished++;
    
    if (this.config.debugMode) {
      console.log('WebSocket connected successfully');
    }
    
    // Start heartbeat
    this.startHeartbeat();
    
    // Send queued messages
    this.sendQueuedMessages();
    
    // Re-establish subscriptions
    this.reestablishSubscriptions();
    
    // Emit connect event
    this.emit('connect', { event, connectionId: this.generateConnectionId() });
  }
  
  /**
   * Handle incoming messages
   */
  private handleMessage(event: MessageEvent): void {
    try {
      const message: WebSocketMessage = JSON.parse(event.data);
      
      this.metrics.messagesReceived++;
      
      // Handle special message types
      if (message.type === 'HEARTBEAT') {
        this.handleHeartbeat(message);
        return;
      }
      
      if (message.type === 'ERROR') {
        this.handleServerError(message);
        return;
      }
      
      // Emit message event
      this.emit('message', message);
      
      if (this.config.debugMode) {
        console.log('Received WebSocket message:', message);
      }
      
    } catch (error) {
      this.handleError('Failed to parse message', error);
    }
  }
  
  /**
   * Handle connection close
   */
  private handleConnectionClose(event: CloseEvent): void {
    this.clearConnectionTimer();
    this.stopHeartbeat();
    
    const wasConnected = this.state === WebSocketState.CONNECTED;
    this.state = WebSocketState.DISCONNECTED;
    
    if (this.config.debugMode) {
      console.log('WebSocket connection closed:', event.code, event.reason);
    }
    
    // Emit disconnect event
    this.emit('disconnect', { 
      event, 
      code: event.code, 
      reason: event.reason,
      wasClean: event.wasClean 
    });
    
    // Attempt reconnection if not a clean close
    if (wasConnected && !event.wasClean && this.shouldReconnect(event.code)) {
      this.attemptReconnection();
    }
  }
  
  /**
   * Handle WebSocket errors
   */
  private handleWebSocketError(event: Event): void {
    this.metrics.errors++;
    this.state = WebSocketState.ERROR;
    
    const error: MonitoringError = {
      code: 'WEBSOCKET_ERROR',
      message: 'WebSocket error occurred',
      timestamp: new Date().toISOString(),
      recoverable: true,
      details: { event }
    };
    
    this.emit('error', error);
    
    if (this.config.debugMode) {
      console.error('WebSocket error:', event);
    }
  }
  
  /**
   * Handle connection timeout
   */
  private handleConnectionTimeout(): void {
    if (this.ws) {
      this.ws.close();
    }
    
    const error: MonitoringError = {
      code: 'CONNECTION_TIMEOUT',
      message: 'WebSocket connection timed out',
      timestamp: new Date().toISOString(),
      recoverable: true
    };
    
    this.handleError('Connection timeout', error);
  }
  
  /**
   * Handle server-side errors
   */
  private handleServerError(message: WebSocketMessage): void {
    const error: MonitoringError = {
      code: 'SERVER_ERROR',
      message: message.payload?.message || 'Server error',
      timestamp: message.timestamp,
      recoverable: message.payload?.recoverable !== false,
      details: message.payload
    };
    
    this.emit('error', error);
  }
  
  /**
   * Handle heartbeat messages
   */
  private handleHeartbeat(message: WebSocketMessage): void {
    this.metrics.lastHeartbeat = new Date();
    this.emit('heartbeat', message);
    
    // Respond with heartbeat acknowledgment
    this.sendHeartbeatAck(message.correlationId);
  }
  
  /**
   * Send heartbeat acknowledgment
   */
  private sendHeartbeatAck(correlationId: string): void {
    const heartbeatAck: WebSocketMessage = {
      type: 'HEARTBEAT_ACK',
      payload: { timestamp: new Date().toISOString() },
      timestamp: new Date().toISOString(),
      correlationId
    };
    
    this.send(heartbeatAck);
  }
  
  /**
   * Start heartbeat timer
   */
  private startHeartbeat(): void {
    this.heartbeatTimer = setInterval(() => {
      if (this.state === WebSocketState.CONNECTED) {
        const heartbeat: WebSocketMessage = {
          type: 'HEARTBEAT',
          payload: { clientTime: new Date().toISOString() },
          timestamp: new Date().toISOString(),
          correlationId: this.generateCorrelationId()
        };
        
        this.send(heartbeat);
      }
    }, this.config.heartbeatInterval);
  }
  
  /**
   * Stop heartbeat timer
   */
  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }
  
  /**
   * Clear connection timer
   */
  private clearConnectionTimer(): void {
    if (this.connectionTimer) {
      clearTimeout(this.connectionTimer);
      this.connectionTimer = null;
    }
  }
  
  /**
   * Determine if reconnection should be attempted
   */
  private shouldReconnect(closeCode: number): boolean {
    // Don't reconnect for authentication failures or policy violations
    const nonRecoverableCodes = [1008, 1011, 4001, 4002, 4003];
    return !nonRecoverableCodes.includes(closeCode);
  }
  
  /**
   * Attempt reconnection with exponential backoff
   */
  private attemptReconnection(): void {
    if (this.reconnectCount >= this.config.reconnectAttempts) {
      this.handleError('Max reconnection attempts exceeded', null);
      return;
    }
    
    this.state = WebSocketState.RECONNECTING;
    this.reconnectCount++;
    this.metrics.reconnections++;
    
    const backoffDelay = Math.min(
      this.config.reconnectInterval * Math.pow(2, this.reconnectCount - 1),
      30000 // Max 30 seconds
    );
    
    if (this.config.debugMode) {
      console.log(`Attempting reconnection ${this.reconnectCount}/${this.config.reconnectAttempts} in ${backoffDelay}ms`);
    }
    
    this.reconnectTimer = setTimeout(() => {
      if (this.authToken) {
        this.connect(this.authToken).catch(error => {
          console.error('Reconnection failed:', error);
        });
      }
    }, backoffDelay);
    
    this.emit('reconnect', { attempt: this.reconnectCount, delay: backoffDelay });
  }
  
  /**
   * Send message to WebSocket server
   */
  public send(message: WebSocketMessage): void {
    if (this.state !== WebSocketState.CONNECTED || !this.ws) {
      // Queue message for later sending
      if (this.messageQueue.length < this.config.messageQueueSize) {
        this.messageQueue.push(message);
      }
      return;
    }
    
    try {
      const messageString = JSON.stringify(message);
      this.ws.send(messageString);
      this.metrics.messagesSent++;
      
      if (this.config.debugMode) {
        console.log('Sent WebSocket message:', message);
      }
    } catch (error) {
      this.handleError('Failed to send message', error);
      
      // Queue message for retry
      if (this.messageQueue.length < this.config.messageQueueSize) {
        this.messageQueue.push(message);
      }
    }
  }
  
  /**
   * Send queued messages
   */
  private sendQueuedMessages(): void {
    while (this.messageQueue.length > 0 && this.state === WebSocketState.CONNECTED) {
      const message = this.messageQueue.shift();
      if (message) {
        this.send(message);
      }
    }
  }
  
  /**
   * Subscribe to monitoring topics
   */
  public subscribe(topics: string[]): void {
    topics.forEach(topic => this.subscriptions.add(topic));
    
    const subscribeMessage: WebSocketMessage = {
      type: 'SUBSCRIBE',
      payload: { topics },
      timestamp: new Date().toISOString(),
      correlationId: this.generateCorrelationId()
    };
    
    this.send(subscribeMessage);
  }
  
  /**
   * Unsubscribe from monitoring topics
   */
  public unsubscribe(topics: string[]): void {
    topics.forEach(topic => this.subscriptions.delete(topic));
    
    const unsubscribeMessage: WebSocketMessage = {
      type: 'UNSUBSCRIBE',
      payload: { topics },
      timestamp: new Date().toISOString(),
      correlationId: this.generateCorrelationId()
    };
    
    this.send(unsubscribeMessage);
  }
  
  /**
   * Re-establish subscriptions after reconnection
   */
  private reestablishSubscriptions(): void {
    if (this.subscriptions.size > 0) {
      this.subscribe(Array.from(this.subscriptions));
    }
  }
  
  /**
   * Disconnect from WebSocket server
   */
  public disconnect(): void {
    this.state = WebSocketState.DISCONNECTING;
    
    // Clear timers
    this.clearConnectionTimer();
    this.stopHeartbeat();
    
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    
    // Close WebSocket connection
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect');
      this.ws = null;
    }
    
    // Clear subscriptions
    this.subscriptions.clear();
    
    // Clear message queue
    this.messageQueue = [];
    
    this.state = WebSocketState.DISCONNECTED;
  }
  
  /**
   * Add event listener
   */
  public on(eventType: WebSocketEventType, listener: WebSocketEventListener): void {
    const listeners = this.eventListeners.get(eventType) || [];
    listeners.push(listener);
    this.eventListeners.set(eventType, listeners);
  }
  
  /**
   * Remove event listener
   */
  public off(eventType: WebSocketEventType, listener: WebSocketEventListener): void {
    const listeners = this.eventListeners.get(eventType) || [];
    const index = listeners.indexOf(listener);
    if (index !== -1) {
      listeners.splice(index, 1);
      this.eventListeners.set(eventType, listeners);
    }
  }
  
  /**
   * Emit event to all listeners
   */
  private emit(eventType: WebSocketEventType, data?: any): void {
    const listeners = this.eventListeners.get(eventType) || [];
    listeners.forEach(listener => {
      try {
        listener(data);
      } catch (error) {
        console.error(`Error in WebSocket event listener:`, error);
      }
    });
  }
  
  /**
   * Handle errors
   */
  private handleError(message: string, error: any): void {
    const monitoringError: MonitoringError = {
      code: 'WEBSOCKET_SERVICE_ERROR',
      message,
      timestamp: new Date().toISOString(),
      recoverable: true,
      details: { error }
    };
    
    this.emit('error', monitoringError);
    
    if (this.config.debugMode) {
      console.error(message, error);
    }
  }
  
  /**
   * Get current connection state
   */
  public getState(): WebSocketState {
    return this.state;
  }
  
  /**
   * Get connection metrics
   */
  public getMetrics(): typeof this.metrics {
    return { ...this.metrics };
  }
  
  /**
   * Get active subscriptions
   */
  public getSubscriptions(): string[] {
    return Array.from(this.subscriptions);
  }
  
  /**
   * Check if connected
   */
  public isConnected(): boolean {
    return this.state === WebSocketState.CONNECTED;
  }
  
  /**
   * Generate unique connection ID
   */
  private generateConnectionId(): string {
    return `conn_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
  
  /**
   * Generate correlation ID for request tracking
   */
  private generateCorrelationId(): string {
    return `corr_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
  
  /**
   * Update authentication token
   */
  public updateAuthToken(token: string): void {
    this.authToken = token;
    
    // If connected, we might need to re-authenticate
    if (this.state === WebSocketState.CONNECTED) {
      const authMessage: WebSocketMessage = {
        type: 'REAUTH',
        payload: { token },
        timestamp: new Date().toISOString(),
        correlationId: this.generateCorrelationId()
      };
      
      this.send(authMessage);
    }
  }
}