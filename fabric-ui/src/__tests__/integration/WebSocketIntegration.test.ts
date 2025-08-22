/**
 * WebSocket Integration Tests
 * 
 * Integration tests for WebSocket connectivity, authentication, message handling,
 * and real-time data synchronization between frontend and backend.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

import { WebSocketService, WebSocketState } from '../../services/websocket/WebSocketService';
import { useWebSocket } from '../../hooks/useWebSocket';
import { renderHook, act, waitFor } from '@testing-library/react';
import { WebSocketMessage, MonitoringError } from '../../types/monitoring';

// Mock server responses for integration testing
const mockServerResponses = {
  heartbeat: {
    type: 'HEARTBEAT',
    payload: { serverTime: '2025-08-08T10:00:00Z' },
    timestamp: '2025-08-08T10:00:00Z',
    correlationId: 'heartbeat-123'
  },
  dashboardUpdate: {
    type: 'DASHBOARD_UPDATE',
    payload: {
      activeJobs: [
        {
          executionId: 'job-123',
          jobName: 'Test Job',
          status: 'RUNNING',
          progress: 75
        }
      ],
      alerts: [],
      systemHealth: { overallScore: 95 }
    },
    timestamp: '2025-08-08T10:00:01Z',
    correlationId: 'dashboard-123'
  },
  error: {
    type: 'ERROR',
    payload: { 
      message: 'Authentication failed',
      code: 'AUTH_ERROR',
      recoverable: false
    },
    timestamp: '2025-08-08T10:00:02Z',
    correlationId: 'error-123'
  }
};

// Enhanced MockWebSocket for integration testing
class IntegrationMockWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  readyState = IntegrationMockWebSocket.CONNECTING;
  url: string;
  protocols?: string | string[];
  
  onopen: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;

  private messageQueue: string[] = [];
  private connectionDelay = 100;
  private simulateServerBehavior = true;

  constructor(url: string, protocols?: string | string[]) {
    this.url = url;
    this.protocols = protocols;
    
    // Parse URL to validate authentication parameters
    const urlObj = new URL(url);
    const token = urlObj.searchParams.get('token');
    const csrf = urlObj.searchParams.get('csrf');
    
    if (this.simulateServerBehavior) {
      this.simulateConnection(token, csrf);
    }
  }

  private simulateConnection(token: string | null, csrf: string | null) {
    setTimeout(() => {
      // Simulate authentication validation
      if (!token) {
        this.simulateError('Missing authentication token');
        return;
      }

      if (token === 'invalid-token') {
        this.simulateClose(4001, 'Authentication failed');
        return;
      }

      // Successful connection
      this.readyState = IntegrationMockWebSocket.OPEN;
      this.onopen?.(new Event('open'));

      // Start server heartbeat
      this.startHeartbeat();

      // Send initial dashboard data
      setTimeout(() => {
        this.simulateIncomingMessage(mockServerResponses.dashboardUpdate);
      }, 50);

    }, this.connectionDelay);
  }

  private startHeartbeat() {
    const heartbeatInterval = setInterval(() => {
      if (this.readyState === IntegrationMockWebSocket.OPEN) {
        this.simulateIncomingMessage(mockServerResponses.heartbeat);
      } else {
        clearInterval(heartbeatInterval);
      }
    }, 30000); // 30 second heartbeat
  }

  send(data: string): void {
    if (this.readyState !== IntegrationMockWebSocket.OPEN) {
      throw new Error('WebSocket is not open');
    }

    this.messageQueue.push(data);

    if (this.simulateServerBehavior) {
      this.processMessage(data);
    }
  }

  private processMessage(data: string) {
    try {
      const message = JSON.parse(data);
      
      // Simulate server responses based on message type
      switch (message.type) {
        case 'HEARTBEAT_ACK':
          // Server received heartbeat acknowledgment
          break;
          
        case 'SUBSCRIBE':
          this.simulateSubscriptionResponse(message);
          break;
          
        case 'UNSUBSCRIBE':
          this.simulateUnsubscriptionResponse(message);
          break;
          
        case 'CLIENT_MESSAGE':
          this.simulateServerEcho(message);
          break;
          
        case 'REAUTH':
          this.simulateReauthResponse(message);
          break;
          
        default:
          console.warn('Unknown message type:', message.type);
      }
    } catch (error) {
      this.simulateError('Invalid message format');
    }
  }

  private simulateSubscriptionResponse(message: any) {
    const response = {
      type: 'SUBSCRIPTION_CONFIRMED',
      payload: {
        topics: message.payload.topics,
        subscriptionId: `sub-${Date.now()}`
      },
      timestamp: new Date().toISOString(),
      correlationId: message.correlationId
    };

    setTimeout(() => {
      this.simulateIncomingMessage(response);
    }, 10);
  }

  private simulateUnsubscriptionResponse(message: any) {
    const response = {
      type: 'UNSUBSCRIPTION_CONFIRMED',
      payload: {
        topics: message.payload.topics
      },
      timestamp: new Date().toISOString(),
      correlationId: message.correlationId
    };

    setTimeout(() => {
      this.simulateIncomingMessage(response);
    }, 10);
  }

  private simulateServerEcho(message: any) {
    const echo = {
      type: 'SERVER_ECHO',
      payload: {
        originalMessage: message.payload,
        serverProcessed: true
      },
      timestamp: new Date().toISOString(),
      correlationId: message.correlationId
    };

    setTimeout(() => {
      this.simulateIncomingMessage(echo);
    }, 20);
  }

  private simulateReauthResponse(message: any) {
    const token = message.payload.token;
    
    if (token === 'invalid-refresh-token') {
      this.simulateError('Token refresh failed');
      return;
    }

    const response = {
      type: 'REAUTH_SUCCESS',
      payload: {
        tokenRefreshed: true,
        expiresAt: new Date(Date.now() + 3600000).toISOString() // 1 hour
      },
      timestamp: new Date().toISOString(),
      correlationId: message.correlationId
    };

    setTimeout(() => {
      this.simulateIncomingMessage(response);
    }, 100);
  }

  close(code?: number, reason?: string): void {
    this.simulateClose(code || 1000, reason || 'Normal closure');
  }

  public simulateClose(code: number, reason: string) {
    this.readyState = IntegrationMockWebSocket.CLOSED;
    const closeEvent = new CloseEvent('close', {
      code,
      reason,
      wasClean: code === 1000
    });
    setTimeout(() => {
      this.onclose?.(closeEvent);
    }, 10);
  }

  private simulateError(reason: string) {
    console.error('WebSocket error:', reason);
    setTimeout(() => {
      this.onerror?.(new Event('error'));
    }, 10);
  }

  // Test utilities
  simulateIncomingMessage(data: any) {
    if (this.readyState === IntegrationMockWebSocket.OPEN) {
      const messageEvent = new MessageEvent('message', {
        data: JSON.stringify(data)
      });
      setTimeout(() => {
        this.onmessage?.(messageEvent);
      }, 5);
    }
  }

  simulateConnectionLoss() {
    this.simulateClose(1006, 'Connection lost');
  }

  simulateServerRestart() {
    this.simulateClose(1012, 'Server restart');
    
    // Reconnect after server restart
    setTimeout(() => {
      this.readyState = IntegrationMockWebSocket.CONNECTING;
      this.simulateConnection('reconnect-token', 'csrf-token');
    }, 1000);
  }

  getMessageQueue() {
    return [...this.messageQueue];
  }

  clearMessageQueue() {
    this.messageQueue = [];
  }

  setConnectionDelay(delay: number) {
    this.connectionDelay = delay;
  }

  disableServerBehavior() {
    this.simulateServerBehavior = false;
  }
}

global.WebSocket = IntegrationMockWebSocket as any;

describe('WebSocket Integration Tests', () => {
  let service: WebSocketService;
  let mockWebSocket: IntegrationMockWebSocket;

  beforeEach(() => {
    service = new WebSocketService({
      url: 'ws://localhost:8080/ws/monitoring',
      protocols: ['monitoring-v1'],
      reconnectAttempts: 3,
      reconnectInterval: 1000,
      heartbeatInterval: 30000,
      connectionTimeout: 5000,
      debugMode: true
    });
  });

  afterEach(() => {
    service?.disconnect();
    jest.clearAllTimers();
  });

  describe('Authentication Flow', () => {
    it('should establish authenticated connection', async () => {
      const connectPromise = service.connect('valid-jwt-token');

      await waitFor(() => {
        expect(service.getState()).toBe(WebSocketState.CONNECTED);
      });

      mockWebSocket = (global.WebSocket as any).mock.instances[0];
      expect(mockWebSocket.url).toContain('token=valid-jwt-token');
      expect(service.isConnected()).toBe(true);
    });

    it('should handle authentication failure', async () => {
      const errorHandler = jest.fn();
      service.on('error', errorHandler);

      try {
        await service.connect('invalid-token');
      } catch (error) {
        // Connection should fail
      }

      await waitFor(() => {
        expect(service.getState()).toBe(WebSocketState.DISCONNECTED);
      });

      expect(service.isConnected()).toBe(false);
    });

    it('should handle token refresh during connection', async () => {
      await service.connect('valid-token');
      
      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
      });

      mockWebSocket = (global.WebSocket as any).mock.instances[0];
      const messageQueue = mockWebSocket.getMessageQueue();
      const initialMessageCount = messageQueue.length;

      // Update token
      service.updateAuthToken('refreshed-token');

      await waitFor(() => {
        const newMessageQueue = mockWebSocket.getMessageQueue();
        expect(newMessageQueue.length).toBeGreaterThan(initialMessageCount);
      });

      // Should have sent REAUTH message
      const reauthMessage = mockWebSocket.getMessageQueue()
        .map(msg => JSON.parse(msg))
        .find(msg => msg.type === 'REAUTH');
      
      expect(reauthMessage).toBeTruthy();
      expect(reauthMessage.payload.token).toBe('refreshed-token');
    });
  });

  describe('Real-time Data Flow', () => {
    beforeEach(async () => {
      await service.connect('valid-token');
      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
      });
      mockWebSocket = (global.WebSocket as any).mock.instances[0];
    });

    it('should receive and process dashboard updates', async () => {
      const messageHandler = jest.fn();
      service.on('message', messageHandler);

      // Server automatically sends dashboard update after connection
      await waitFor(() => {
        expect(messageHandler).toHaveBeenCalledWith(
          'message',
          expect.objectContaining({
            type: 'DASHBOARD_UPDATE',
            payload: expect.objectContaining({
              activeJobs: expect.any(Array),
              systemHealth: expect.objectContaining({
                overallScore: 95
              })
            })
          })
        );
      });
    });

    it('should handle subscription management', async () => {
      const topics = ['job-updates', 'system-alerts', 'performance-metrics'];
      service.subscribe(topics);

      await waitFor(() => {
        const subscriptionMessage = mockWebSocket.getMessageQueue()
          .map(msg => JSON.parse(msg))
          .find(msg => msg.type === 'SUBSCRIBE');
        
        expect(subscriptionMessage).toBeTruthy();
        expect(subscriptionMessage.payload.topics).toEqual(topics);
      });

      expect(service.getSubscriptions()).toEqual(topics);

      // Unsubscribe from some topics
      service.unsubscribe(['performance-metrics']);

      await waitFor(() => {
        const unsubscribeMessage = mockWebSocket.getMessageQueue()
          .map(msg => JSON.parse(msg))
          .find(msg => msg.type === 'UNSUBSCRIBE');
        
        expect(unsubscribeMessage).toBeTruthy();
        expect(unsubscribeMessage.payload.topics).toEqual(['performance-metrics']);
      });

      expect(service.getSubscriptions()).toEqual(['job-updates', 'system-alerts']);
    });

    it('should maintain heartbeat communication', async () => {
      const heartbeatHandler = jest.fn();
      service.on('heartbeat', heartbeatHandler);

      // Wait for server heartbeat
      await waitFor(() => {
        expect(heartbeatHandler).toHaveBeenCalled();
      }, { timeout: 35000 }); // Wait longer than heartbeat interval

      // Should have sent heartbeat acknowledgment
      const heartbeatAck = mockWebSocket.getMessageQueue()
        .map(msg => JSON.parse(msg))
        .find(msg => msg.type === 'HEARTBEAT_ACK');
      
      expect(heartbeatAck).toBeTruthy();
      expect(service.getMetrics().lastHeartbeat).toBeTruthy();
    });

    it('should handle bidirectional messaging', async () => {
      const testMessage: WebSocketMessage = {
        type: 'CLIENT_MESSAGE',
        payload: { action: 'test', data: { foo: 'bar' } },
        timestamp: new Date().toISOString(),
        correlationId: 'test-123'
      };

      const messageHandler = jest.fn();
      service.on('message', messageHandler);

      service.send(testMessage);

      // Should receive server echo
      await waitFor(() => {
        expect(messageHandler).toHaveBeenCalledWith(
          'message',
          expect.objectContaining({
            type: 'SERVER_ECHO',
            payload: expect.objectContaining({
              originalMessage: testMessage.payload,
              serverProcessed: true
            })
          })
        );
      });
    });
  });

  describe('Connection Resilience', () => {
    beforeEach(async () => {
      await service.connect('valid-token');
      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
      });
      mockWebSocket = (global.WebSocket as any).mock.instances[0];
    });

    it('should handle connection loss and reconnection', async () => {
      const reconnectHandler = jest.fn();
      const connectHandler = jest.fn();
      
      service.on('reconnect', reconnectHandler);
      service.on('connect', connectHandler);

      // Simulate connection loss
      mockWebSocket.simulateConnectionLoss();

      await waitFor(() => {
        expect(service.getState()).toBe(WebSocketState.DISCONNECTED);
      });

      // Should attempt reconnection
      await waitFor(() => {
        expect(reconnectHandler).toHaveBeenCalled();
        expect(service.getState()).toBe(WebSocketState.RECONNECTING);
      });

      // Should eventually reconnect
      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
        expect(connectHandler).toHaveBeenCalledTimes(2); // Initial + reconnection
      }, { timeout: 5000 });

      expect(service.getMetrics().reconnections).toBeGreaterThan(0);
    });

    it('should reestablish subscriptions after reconnection', async () => {
      const topics = ['job-updates', 'alerts'];
      service.subscribe(topics);

      await waitFor(() => {
        expect(service.getSubscriptions()).toEqual(topics);
      });

      mockWebSocket.clearMessageQueue();

      // Simulate connection loss and reconnection
      mockWebSocket.simulateConnectionLoss();

      await waitFor(() => {
        expect(service.getState()).toBe(WebSocketState.DISCONNECTED);
      });

      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
      }, { timeout: 5000 });

      // Should resubscribe to topics
      await waitFor(() => {
        const newMockWebSocket = (global.WebSocket as any).mock.instances.slice(-1)[0];
        const resubscribeMessage = newMockWebSocket.getMessageQueue()
          .map((msg: string) => JSON.parse(msg))
          .find((msg: any) => msg.type === 'SUBSCRIBE');
        
        expect(resubscribeMessage).toBeTruthy();
        expect(resubscribeMessage.payload.topics).toEqual(topics);
      });
    });

    it('should queue messages during disconnection', async () => {
      const testMessages: WebSocketMessage[] = [
        { type: 'CLIENT_MESSAGE' as const, payload: { id: 1 }, timestamp: new Date().toISOString(), correlationId: 'msg-1' },
        { type: 'CLIENT_MESSAGE' as const, payload: { id: 2 }, timestamp: new Date().toISOString(), correlationId: 'msg-2' },
        { type: 'CLIENT_MESSAGE' as const, payload: { id: 3 }, timestamp: new Date().toISOString(), correlationId: 'msg-3' }
      ];

      // Disconnect
      service.disconnect();

      await waitFor(() => {
        expect(service.isConnected()).toBe(false);
      });

      // Send messages while disconnected
      testMessages.forEach(msg => service.send(msg));

      expect(service.getMetrics().messagesSent).toBe(0); // Not sent yet

      // Reconnect
      await service.connect('valid-token');

      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
      });

      // Messages should be sent after reconnection
      await waitFor(() => {
        expect(service.getMetrics().messagesSent).toBe(3);
      });

      const newMockWebSocket = (global.WebSocket as any).mock.instances.slice(-1)[0];
      const sentMessages = newMockWebSocket.getMessageQueue()
        .map((msg: string) => JSON.parse(msg))
        .filter((msg: any) => msg.type === 'CLIENT_MESSAGE');

      expect(sentMessages).toHaveLength(3);
      expect(sentMessages.map((msg: any) => msg.payload.id)).toEqual([1, 2, 3]);
    });

    it('should handle server restart gracefully', async () => {
      const disconnectHandler = jest.fn();
      const reconnectHandler = jest.fn();
      
      service.on('disconnect', disconnectHandler);
      service.on('reconnect', reconnectHandler);

      // Simulate server restart
      mockWebSocket.simulateServerRestart();

      // Should detect disconnection
      await waitFor(() => {
        expect(disconnectHandler).toHaveBeenCalledWith(
          'disconnect',
          expect.objectContaining({
            code: 1012, // Server restart code
            reason: 'Server restart'
          })
        );
      });

      // Should attempt reconnection
      await waitFor(() => {
        expect(reconnectHandler).toHaveBeenCalled();
      });

      // Should eventually reconnect
      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
      }, { timeout: 10000 });
    });
  });

  describe('Hook Integration', () => {
    const mockAuthContext = {
      accessToken: 'test-token',
      refreshAccessToken: jest.fn().mockResolvedValue('new-token')
    };

    beforeEach(() => {
      jest.doMock('../../contexts/AuthContext', () => ({
        useAuth: () => mockAuthContext
      }));
    });

    it('should integrate WebSocketService with React hook', async () => {
      const { result } = renderHook(() =>
        useWebSocket('ws://localhost:8080/ws/monitoring', {
          autoConnect: true,
          subscriptions: ['job-updates']
        })
      );

      // Should start connecting
      await waitFor(() => {
        expect(result.current.connecting).toBe(true);
      });

      // Should eventually connect
      await waitFor(() => {
        expect(result.current.connected).toBe(true);
        expect(result.current.connecting).toBe(false);
      });

      expect(result.current.error).toBeNull();
    });

    it('should handle real-time updates through hook', async () => {
      const onMessage = jest.fn();
      
      const { result } = renderHook(() =>
        useWebSocket('ws://localhost:8080/ws/monitoring', {
          autoConnect: true,
          onMessage
        })
      );

      await waitFor(() => {
        expect(result.current.connected).toBe(true);
      });

      // Should receive dashboard update
      await waitFor(() => {
        expect(onMessage).toHaveBeenCalledWith(
          expect.objectContaining({
            type: 'DASHBOARD_UPDATE'
          })
        );
      });

      expect(result.current.lastMessage).toBeTruthy();
    });

    it('should handle errors through hook', async () => {
      mockAuthContext.accessToken = 'invalid-token';
      const onError = jest.fn();
      
      const { result } = renderHook(() =>
        useWebSocket('ws://localhost:8080/ws/monitoring', {
          autoConnect: true,
          onError
        })
      );

      await waitFor(() => {
        expect(result.current.error).toBeTruthy();
        expect(result.current.connected).toBe(false);
      });

      expect(onError).toHaveBeenCalled();
    });
  });

  describe('Performance and Load Testing', () => {
    it('should handle high message throughput', async () => {
      await service.connect('valid-token');
      
      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
      });

      mockWebSocket = (global.WebSocket as any).mock.instances[0];
      const messageHandler = jest.fn();
      service.on('message', messageHandler);

      // Send 1000 messages rapidly
      const messages = Array.from({ length: 1000 }, (_, i) => ({
        type: 'JOB_UPDATE',
        payload: { jobId: `job-${i}`, progress: i % 100 },
        timestamp: new Date().toISOString(),
        correlationId: `update-${i}`
      }));

      messages.forEach(message => {
        mockWebSocket.simulateIncomingMessage(message);
      });

      // Should handle all messages
      await waitFor(() => {
        expect(service.getMetrics().messagesReceived).toBe(1001); // +1 for initial dashboard update
      }, { timeout: 10000 });

      expect(messageHandler).toHaveBeenCalledTimes(1001);
    });

    it('should maintain connection stability under load', async () => {
      const errorHandler = jest.fn();
      service.on('error', errorHandler);

      await service.connect('valid-token');
      
      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
      });

      // Send messages continuously for 5 seconds
      const startTime = Date.now();
      const messageInterval = setInterval(() => {
        if (Date.now() - startTime < 5000) {
          service.send({
            type: 'CLIENT_MESSAGE',
            payload: { timestamp: Date.now() },
            timestamp: new Date().toISOString(),
            correlationId: `load-test-${Date.now()}`
          });
        } else {
          clearInterval(messageInterval);
        }
      }, 10); // Send every 10ms

      await new Promise(resolve => setTimeout(resolve, 6000));

      // Should maintain connection
      expect(service.isConnected()).toBe(true);
      expect(errorHandler).not.toHaveBeenCalled();
      expect(service.getMetrics().messagesSent).toBeGreaterThan(400); // Should have sent many messages
    });
  });

  describe('Security Integration', () => {
    it('should include proper authentication headers in connection', async () => {
      const token = 'secure-jwt-token';
      await service.connect(token);

      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
      });

      mockWebSocket = (global.WebSocket as any).mock.instances[0];
      
      expect(mockWebSocket.url).toContain(`token=${token}`);
      expect(mockWebSocket.url).toContain('client=fabric-ui');
      expect(mockWebSocket.url).toContain('timestamp=');
    });

    it('should handle authorization failures properly', async () => {
      const errorHandler = jest.fn();
      service.on('error', errorHandler);

      // Simulate server sending authorization error
      await service.connect('valid-token');
      
      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
      });

      mockWebSocket = (global.WebSocket as any).mock.instances[0];
      
      // Server sends auth error
      mockWebSocket.simulateIncomingMessage(mockServerResponses.error);

      await waitFor(() => {
        expect(errorHandler).toHaveBeenCalledWith(
          'error',
          expect.objectContaining({
            code: 'SERVER_ERROR',
            message: 'Authentication failed'
          })
        );
      });
    });

    it('should not reconnect on security violations', async () => {
      const reconnectHandler = jest.fn();
      service.on('reconnect', reconnectHandler);

      await service.connect('valid-token');
      
      await waitFor(() => {
        expect(service.isConnected()).toBe(true);
      });

      mockWebSocket = (global.WebSocket as any).mock.instances[0];
      
      // Simulate security violation close code
      mockWebSocket.simulateClose(4003, 'Security violation');

      await waitFor(() => {
        expect(service.getState()).toBe(WebSocketState.DISCONNECTED);
      });

      // Wait to ensure no reconnection attempt
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      expect(reconnectHandler).not.toHaveBeenCalled();
      expect(service.getMetrics().reconnections).toBe(0);
    });
  });
});