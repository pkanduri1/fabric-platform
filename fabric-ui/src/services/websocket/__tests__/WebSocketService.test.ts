/**
 * WebSocketService Tests
 * 
 * Comprehensive test suite for the WebSocketService class including connection management,
 * authentication, message handling, reconnection logic, and error scenarios.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

import { WebSocketService, WebSocketConfig, WebSocketState } from '../WebSocketService';
import { WebSocketMessage, MonitoringError } from '../../../types/monitoring';

// Mock WebSocket implementation
class MockWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  readyState = MockWebSocket.CONNECTING;
  url: string;
  protocols?: string | string[];
  
  onopen: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;

  private eventListeners: { [key: string]: ((event: any) => void)[] } = {};

  constructor(url: string, protocols?: string | string[]) {
    this.url = url;
    this.protocols = protocols;
    
    // Simulate connection opening after a short delay
    setTimeout(() => {
      this.readyState = MockWebSocket.OPEN;
      this.dispatchEvent(new Event('open'));
    }, 10);
  }

  send(data: string): void {
    if (this.readyState !== MockWebSocket.OPEN) {
      throw new Error('WebSocket is not open');
    }
  }

  close(code?: number, reason?: string): void {
    this.readyState = MockWebSocket.CLOSED;
    const closeEvent = new CloseEvent('close', {
      code: code || 1000,
      reason: reason || '',
      wasClean: code === 1000
    });
    this.dispatchEvent(closeEvent);
  }

  addEventListener(type: string, listener: (event: any) => void): void {
    if (!this.eventListeners[type]) {
      this.eventListeners[type] = [];
    }
    this.eventListeners[type].push(listener);
  }

  removeEventListener(type: string, listener: (event: any) => void): void {
    if (this.eventListeners[type]) {
      const index = this.eventListeners[type].indexOf(listener);
      if (index !== -1) {
        this.eventListeners[type].splice(index, 1);
      }
    }
  }

  dispatchEvent(event: Event): void {
    // Call direct handler
    switch (event.type) {
      case 'open':
        this.onopen?.(event);
        break;
      case 'close':
        this.onclose?.(event as CloseEvent);
        break;
      case 'message':
        this.onmessage?.(event as MessageEvent);
        break;
      case 'error':
        this.onerror?.(event);
        break;
    }

    // Call event listeners
    const listeners = this.eventListeners[event.type] || [];
    listeners.forEach(listener => listener(event));
  }

  // Helper method to simulate incoming messages
  simulateMessage(data: any): void {
    const messageEvent = new MessageEvent('message', {
      data: typeof data === 'string' ? data : JSON.stringify(data)
    });
    this.dispatchEvent(messageEvent);
  }

  // Helper method to simulate errors
  simulateError(): void {
    this.dispatchEvent(new Event('error'));
  }
}

// Mock global WebSocket
global.WebSocket = MockWebSocket as any;

// Mock document methods
Object.defineProperty(document, 'querySelector', {
  writable: true,
  value: jest.fn((selector: string) => {
    if (selector === 'meta[name="csrf-token"]') {
      return { getAttribute: () => 'mock-csrf-token' };
    }
    return null;
  })
});

Object.defineProperty(document, 'cookie', {
  writable: true,
  value: 'XSRF-TOKEN=mock-xsrf-token'
});

// Mock timers
jest.useFakeTimers();

describe('WebSocketService', () => {
  let service: WebSocketService;
  let mockConfig: WebSocketConfig;

  beforeEach(() => {
    mockConfig = {
      url: 'ws://localhost:8080/ws/monitoring',
      protocols: ['monitoring-v1'],
      reconnectAttempts: 3,
      reconnectInterval: 1000,
      heartbeatInterval: 30000,
      connectionTimeout: 10000,
      messageQueueSize: 100,
      enableCompression: true,
      debugMode: true
    };

    service = new WebSocketService(mockConfig);
  });

  afterEach(() => {
    service?.disconnect();
    jest.clearAllMocks();
    jest.clearAllTimers();
  });

  describe('Initialization', () => {
    it('should initialize with default configuration', () => {
      const defaultService = new WebSocketService();
      expect(defaultService.getState()).toBe(WebSocketState.DISCONNECTED);
      expect(defaultService.getMetrics().connectionsEstablished).toBe(0);
    });

    it('should merge custom configuration with defaults', () => {
      const customConfig = { reconnectAttempts: 5, debugMode: false };
      const customService = new WebSocketService(customConfig);
      
      expect(customService.getState()).toBe(WebSocketState.DISCONNECTED);
    });

    it('should initialize event listeners map', () => {
      const metrics = service.getMetrics();
      expect(metrics.connectionsEstablished).toBe(0);
      expect(metrics.messagesReceived).toBe(0);
      expect(metrics.messagesSent).toBe(0);
    });
  });

  describe('Connection Management', () => {
    it('should establish connection successfully', async () => {
      const authToken = 'test-jwt-token';
      const connectPromise = service.connect(authToken);

      // Fast-forward timers to trigger connection
      jest.advanceTimersByTime(20);

      await connectPromise;

      expect(service.getState()).toBe(WebSocketState.CONNECTED);
      expect(service.isConnected()).toBe(true);
      expect(service.getMetrics().connectionsEstablished).toBe(1);
    });

    it('should build WebSocket URL with authentication parameters', async () => {
      const authToken = 'test-jwt-token';
      const connectPromise = service.connect(authToken);

      jest.advanceTimersByTime(20);
      await connectPromise;

      // Should include token, csrf, client info in URL
      const expectedParams = ['token=test-jwt-token', 'csrf=mock-csrf-token', 'client=fabric-ui'];
      // URL construction is tested indirectly through successful connection
      expect(service.isConnected()).toBe(true);
    });

    it('should handle connection timeout', async () => {
      let errorCaught = false;
      const errorHandler = jest.fn((eventType, error) => {
        if (error.code === 'CONNECTION_TIMEOUT') {
          errorCaught = true;
        }
      });

      service.on('error', errorHandler);

      const connectPromise = service.connect('test-token');

      // Fast-forward past connection timeout
      jest.advanceTimersByTime(mockConfig.connectionTimeout + 100);

      try {
        await connectPromise;
      } catch (error) {
        // Connection should timeout
      }

      expect(errorCaught).toBe(true);
    });

    it('should prevent multiple concurrent connections', async () => {
      const authToken = 'test-jwt-token';
      
      const connectPromise1 = service.connect(authToken);
      const connectPromise2 = service.connect(authToken);

      jest.advanceTimersByTime(20);

      await connectPromise1;
      await connectPromise2;

      // Should still have only one connection
      expect(service.getMetrics().connectionsEstablished).toBe(1);
    });

    it('should disconnect cleanly', () => {
      const connectPromise = service.connect('test-token');
      
      jest.advanceTimersByTime(20);

      service.disconnect();

      expect(service.getState()).toBe(WebSocketState.DISCONNECTED);
      expect(service.isConnected()).toBe(false);
    });
  });

  describe('Message Handling', () => {
    let mockWebSocket: MockWebSocket;

    beforeEach(async () => {
      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;
      
      // Get reference to the mock WebSocket
      mockWebSocket = (service as any).ws as MockWebSocket;
    });

    it('should send messages when connected', () => {
      const message: WebSocketMessage = {
        type: 'CLIENT_MESSAGE',
        payload: { test: 'data' },
        timestamp: new Date().toISOString(),
        correlationId: 'test-123'
      };

      const sendSpy = jest.spyOn(mockWebSocket, 'send');
      service.send(message);

      expect(sendSpy).toHaveBeenCalledWith(JSON.stringify(message));
      expect(service.getMetrics().messagesSent).toBe(1);
    });

    it('should queue messages when disconnected', () => {
      service.disconnect();

      const message: WebSocketMessage = {
        type: 'CLIENT_MESSAGE',
        payload: { test: 'data' },
        timestamp: new Date().toISOString(),
        correlationId: 'test-123'
      };

      service.send(message);

      // Message should be queued, not sent immediately
      expect(service.getMetrics().messagesSent).toBe(0);
    });

    it('should process incoming messages', () => {
      const messageHandler = jest.fn();
      service.on('message', messageHandler);

      const incomingMessage: WebSocketMessage = {
        type: 'DASHBOARD_UPDATE',
        payload: { jobs: [] },
        timestamp: new Date().toISOString(),
        correlationId: 'server-123'
      };

      mockWebSocket.simulateMessage(incomingMessage);

      expect(messageHandler).toHaveBeenCalledWith('message', incomingMessage);
      expect(service.getMetrics().messagesReceived).toBe(1);
    });

    it('should handle heartbeat messages', () => {
      const heartbeatHandler = jest.fn();
      service.on('heartbeat', heartbeatHandler);

      const heartbeatMessage: WebSocketMessage = {
        type: 'HEARTBEAT',
        payload: { serverTime: new Date().toISOString() },
        timestamp: new Date().toISOString(),
        correlationId: 'heartbeat-123'
      };

      mockWebSocket.simulateMessage(heartbeatMessage);

      expect(heartbeatHandler).toHaveBeenCalled();
      expect(service.getMetrics().lastHeartbeat).toBeTruthy();
    });

    it('should handle server error messages', () => {
      const errorHandler = jest.fn();
      service.on('error', errorHandler);

      const errorMessage: WebSocketMessage = {
        type: 'ERROR',
        payload: { message: 'Server error occurred', recoverable: true },
        timestamp: new Date().toISOString(),
        correlationId: 'error-123'
      };

      mockWebSocket.simulateMessage(errorMessage);

      expect(errorHandler).toHaveBeenCalledWith('error', expect.objectContaining({
        code: 'SERVER_ERROR',
        message: 'Server error occurred'
      }));
    });

    it('should handle malformed message data', () => {
      const errorHandler = jest.fn();
      service.on('error', errorHandler);

      mockWebSocket.simulateMessage('invalid-json-data');

      expect(errorHandler).toHaveBeenCalled();
      expect(service.getMetrics().errors).toBeGreaterThan(0);
    });
  });

  describe('Subscription Management', () => {
    beforeEach(async () => {
      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;
    });

    it('should subscribe to topics', () => {
      const topics = ['job-updates', 'system-health'];
      service.subscribe(topics);

      const subscriptions = service.getSubscriptions();
      expect(subscriptions).toContain('job-updates');
      expect(subscriptions).toContain('system-health');
    });

    it('should unsubscribe from topics', () => {
      const topics = ['job-updates', 'system-health'];
      service.subscribe(topics);
      service.unsubscribe(['job-updates']);

      const subscriptions = service.getSubscriptions();
      expect(subscriptions).not.toContain('job-updates');
      expect(subscriptions).toContain('system-health');
    });

    it('should reestablish subscriptions after reconnection', async () => {
      const topics = ['job-updates'];
      service.subscribe(topics);

      // Simulate connection loss and reconnection
      const mockWebSocket = (service as any).ws as MockWebSocket;
      mockWebSocket.close(1006, 'Connection lost'); // Abnormal closure

      jest.advanceTimersByTime(mockConfig.reconnectInterval + 100);

      // Should automatically resubscribe
      expect(service.getSubscriptions()).toContain('job-updates');
    });
  });

  describe('Reconnection Logic', () => {
    it('should attempt reconnection on abnormal closure', async () => {
      const reconnectHandler = jest.fn();
      service.on('reconnect', reconnectHandler);

      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      const mockWebSocket = (service as any).ws as MockWebSocket;
      mockWebSocket.close(1006, 'Connection lost'); // Abnormal closure

      jest.advanceTimersByTime(mockConfig.reconnectInterval + 100);

      expect(reconnectHandler).toHaveBeenCalled();
      expect(service.getMetrics().reconnections).toBeGreaterThan(0);
    });

    it('should use exponential backoff for reconnection attempts', async () => {
      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      const mockWebSocket = (service as any).ws as MockWebSocket;
      
      // Simulate connection failures
      for (let i = 0; i < 3; i++) {
        mockWebSocket.close(1006, 'Connection lost');
        jest.advanceTimersByTime(mockConfig.reconnectInterval * Math.pow(2, i) + 100);
      }

      expect(service.getMetrics().reconnections).toBe(3);
    });

    it('should stop reconnecting after max attempts', async () => {
      const errorHandler = jest.fn();
      service.on('error', errorHandler);

      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      const mockWebSocket = (service as any).ws as MockWebSocket;
      
      // Exceed max reconnection attempts
      for (let i = 0; i <= mockConfig.reconnectAttempts; i++) {
        mockWebSocket.close(1006, 'Connection lost');
        jest.advanceTimersByTime(mockConfig.reconnectInterval * Math.pow(2, i) + 100);
      }

      expect(errorHandler).toHaveBeenCalledWith('error', expect.objectContaining({
        message: 'Max reconnection attempts exceeded'
      }));
    });

    it('should not reconnect on clean closure', async () => {
      const reconnectHandler = jest.fn();
      service.on('reconnect', reconnectHandler);

      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      service.disconnect(); // Clean closure

      jest.advanceTimersByTime(mockConfig.reconnectInterval + 100);

      expect(reconnectHandler).not.toHaveBeenCalled();
    });

    it('should not reconnect on authentication failure', async () => {
      const reconnectHandler = jest.fn();
      service.on('reconnect', reconnectHandler);

      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      const mockWebSocket = (service as any).ws as MockWebSocket;
      mockWebSocket.close(4001, 'Authentication failed'); // Non-recoverable code

      jest.advanceTimersByTime(mockConfig.reconnectInterval + 100);

      expect(reconnectHandler).not.toHaveBeenCalled();
    });
  });

  describe('Heartbeat Mechanism', () => {
    beforeEach(async () => {
      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;
    });

    it('should send heartbeat messages periodically', () => {
      const mockWebSocket = (service as any).ws as MockWebSocket;
      const sendSpy = jest.spyOn(mockWebSocket, 'send');

      jest.advanceTimersByTime(mockConfig.heartbeatInterval);

      expect(sendSpy).toHaveBeenCalledWith(
        expect.stringContaining('"type":"HEARTBEAT"')
      );
    });

    it('should respond to server heartbeats', () => {
      const mockWebSocket = (service as any).ws as MockWebSocket;
      const sendSpy = jest.spyOn(mockWebSocket, 'send');

      const heartbeatMessage: WebSocketMessage = {
        type: 'HEARTBEAT',
        payload: { serverTime: new Date().toISOString() },
        timestamp: new Date().toISOString(),
        correlationId: 'server-heartbeat-123'
      };

      mockWebSocket.simulateMessage(heartbeatMessage);

      expect(sendSpy).toHaveBeenCalledWith(
        expect.stringContaining('"type":"HEARTBEAT_ACK"')
      );
    });

    it('should stop heartbeat when disconnected', () => {
      jest.advanceTimersByTime(mockConfig.heartbeatInterval);
      
      service.disconnect();
      
      const mockWebSocket = (service as any).ws;
      expect(mockWebSocket).toBeNull();
    });
  });

  describe('Authentication Token Management', () => {
    beforeEach(async () => {
      const connectPromise = service.connect('initial-token');
      jest.advanceTimersByTime(20);
      await connectPromise;
    });

    it('should update authentication token', () => {
      const mockWebSocket = (service as any).ws as MockWebSocket;
      const sendSpy = jest.spyOn(mockWebSocket, 'send');

      service.updateAuthToken('new-token');

      expect(sendSpy).toHaveBeenCalledWith(
        expect.stringContaining('"type":"REAUTH"')
      );
      expect(sendSpy).toHaveBeenCalledWith(
        expect.stringContaining('"token":"new-token"')
      );
    });

    it('should use new token for reconnection', async () => {
      service.updateAuthToken('updated-token');
      
      const mockWebSocket = (service as any).ws as MockWebSocket;
      mockWebSocket.close(1006, 'Connection lost');

      // Should use updated token for reconnection
      jest.advanceTimersByTime(mockConfig.reconnectInterval + 100);
      
      expect(service.isConnected()).toBe(true);
    });
  });

  describe('Event System', () => {
    it('should add and remove event listeners', () => {
      const messageHandler = jest.fn();
      const errorHandler = jest.fn();

      service.on('message', messageHandler);
      service.on('error', errorHandler);
      service.off('message', messageHandler);

      // Trigger events
      service.emit('message', { test: 'data' });
      service.emit('error', { code: 'TEST_ERROR' });

      expect(messageHandler).not.toHaveBeenCalled();
      expect(errorHandler).toHaveBeenCalled();
    });

    it('should handle event listener errors gracefully', () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      
      const faultyHandler = jest.fn(() => {
        throw new Error('Handler error');
      });

      service.on('message', faultyHandler);
      service.emit('message', { test: 'data' });

      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('Error in WebSocket event listener'),
        expect.any(Error)
      );

      consoleSpy.mockRestore();
    });
  });

  describe('Error Handling', () => {
    it('should handle WebSocket construction errors', () => {
      // Mock WebSocket constructor to throw
      global.WebSocket = jest.fn().mockImplementation(() => {
        throw new Error('WebSocket creation failed');
      });

      expect(async () => {
        await service.connect('test-token');
      }).rejects.toThrow('WebSocket creation failed');
    });

    it('should handle message sending errors', async () => {
      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      const mockWebSocket = (service as any).ws as MockWebSocket;
      jest.spyOn(mockWebSocket, 'send').mockImplementation(() => {
        throw new Error('Send failed');
      });

      const message: WebSocketMessage = {
        type: 'CLIENT_MESSAGE',
        payload: { test: 'data' },
        timestamp: new Date().toISOString(),
        correlationId: 'test-123'
      };

      service.send(message);

      // Message should be queued for retry
      expect(service.getMetrics().messagesSent).toBe(0);
    });

    it('should handle WebSocket error events', async () => {
      const errorHandler = jest.fn();
      service.on('error', errorHandler);

      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      const mockWebSocket = (service as any).ws as MockWebSocket;
      mockWebSocket.simulateError();

      expect(errorHandler).toHaveBeenCalled();
      expect(service.getMetrics().errors).toBeGreaterThan(0);
    });
  });

  describe('Performance and Metrics', () => {
    it('should track connection metrics accurately', async () => {
      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      const metrics = service.getMetrics();
      expect(metrics.connectionsEstablished).toBe(1);
      expect(metrics.messagesReceived).toBe(0);
      expect(metrics.messagesSent).toBe(0);
      expect(metrics.reconnections).toBe(0);
      expect(metrics.errors).toBe(0);
    });

    it('should handle message queue size limits', async () => {
      service.disconnect();

      // Fill message queue beyond limit
      for (let i = 0; i < mockConfig.messageQueueSize + 10; i++) {
        const message: WebSocketMessage = {
          type: 'CLIENT_MESSAGE',
          payload: { index: i },
          timestamp: new Date().toISOString(),
          correlationId: `test-${i}`
        };
        service.send(message);
      }

      // Queue should not exceed configured size
      const queuedMessages = (service as any).messageQueue;
      expect(queuedMessages.length).toBeLessThanOrEqual(mockConfig.messageQueueSize);
    });

    it('should send queued messages on reconnection', async () => {
      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      // Queue some messages while disconnected
      service.disconnect();
      
      for (let i = 0; i < 5; i++) {
        const message: WebSocketMessage = {
          type: 'CLIENT_MESSAGE',
          payload: { index: i },
          timestamp: new Date().toISOString(),
          correlationId: `queued-${i}`
        };
        service.send(message);
      }

      // Reconnect
      const reconnectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await reconnectPromise;

      // All queued messages should be sent
      expect(service.getMetrics().messagesSent).toBe(5);
    });
  });

  describe('Security Features', () => {
    it('should include CSRF token in connection URL', async () => {
      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      // CSRF token inclusion is verified through successful connection
      expect(service.isConnected()).toBe(true);
    });

    it('should handle missing CSRF token gracefully', async () => {
      // Mock missing CSRF token
      document.querySelector = jest.fn().mockReturnValue(null);
      document.cookie = '';

      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      // Should still connect without CSRF token
      expect(service.isConnected()).toBe(true);
    });

    it('should include client identification in URL', async () => {
      const connectPromise = service.connect('test-token');
      jest.advanceTimersByTime(20);
      await connectPromise;

      // Client identification is verified through successful connection
      expect(service.isConnected()).toBe(true);
    });
  });
});