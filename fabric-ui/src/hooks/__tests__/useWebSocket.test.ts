/**
 * useWebSocket Hook Tests
 * 
 * Test suite for the useWebSocket custom React hook including connection management,
 * state synchronization, error handling, and cleanup behaviors.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

import { renderHook, act, waitFor } from '@testing-library/react';
import { useWebSocket, UseWebSocketOptions } from '../useWebSocket';
import { WebSocketService, WebSocketState } from '../../services/websocket/WebSocketService';
import { WebSocketMessage, MonitoringError } from '../../types/monitoring';

// Mock the WebSocketService
jest.mock('../../services/websocket/WebSocketService');

// Mock AuthContext
const mockAuthContext = {
  accessToken: 'test-jwt-token',
  refreshAccessToken: jest.fn().mockResolvedValue('new-token')
};

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => mockAuthContext
}));

const MockWebSocketService = WebSocketService as jest.MockedClass<typeof WebSocketService>;

describe('useWebSocket', () => {
  let mockServiceInstance: jest.Mocked<WebSocketService>;
  const defaultUrl = 'ws://localhost:8080/ws/monitoring';

  beforeEach(() => {
    // Create a fresh mock instance for each test
    mockServiceInstance = {
      connect: jest.fn().mockResolvedValue(undefined),
      disconnect: jest.fn(),
      send: jest.fn(),
      subscribe: jest.fn(),
      unsubscribe: jest.fn(),
      updateAuthToken: jest.fn(),
      isConnected: jest.fn().mockReturnValue(false),
      getState: jest.fn().mockReturnValue(WebSocketState.DISCONNECTED),
      getMetrics: jest.fn().mockReturnValue({
        connectionsEstablished: 0,
        messagesReceived: 0,
        messagesSent: 0,
        reconnections: 0,
        errors: 0,
        lastHeartbeat: null,
        connectionDuration: 0,
        averageLatency: 0
      }),
      getSubscriptions: jest.fn().mockReturnValue([]),
      on: jest.fn(),
      off: jest.fn()
    } as any;

    MockWebSocketService.mockImplementation(() => mockServiceInstance);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Hook Initialization', () => {
    it('should initialize with default state', () => {
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      expect(result.current.connected).toBe(false);
      expect(result.current.connecting).toBe(false);
      expect(result.current.error).toBe(null);
      expect(result.current.lastMessage).toBe(null);
    });

    it('should create WebSocketService instance with correct configuration', () => {
      const options: UseWebSocketOptions = {
        reconnectAttempts: 5,
        heartbeatInterval: 30000,
        debugMode: true,
        subscriptions: ['job-updates', 'alerts']
      };

      renderHook(() => useWebSocket(defaultUrl, options));

      expect(MockWebSocketService).toHaveBeenCalledWith({
        url: defaultUrl,
        protocols: undefined,
        reconnectAttempts: 5,
        reconnectInterval: 1000,
        heartbeatInterval: 30000,
        connectionTimeout: 10000,
        messageQueueSize: 100,
        enableCompression: true,
        debugMode: true
      });
    });

    it('should set up event listeners on service instance', () => {
      renderHook(() => useWebSocket(defaultUrl));

      expect(mockServiceInstance.on).toHaveBeenCalledWith('connect', expect.any(Function));
      expect(mockServiceInstance.on).toHaveBeenCalledWith('disconnect', expect.any(Function));
      expect(mockServiceInstance.on).toHaveBeenCalledWith('message', expect.any(Function));
      expect(mockServiceInstance.on).toHaveBeenCalledWith('error', expect.any(Function));
      expect(mockServiceInstance.on).toHaveBeenCalledWith('reconnect', expect.any(Function));
    });
  });

  describe('Auto-Connection', () => {
    it('should auto-connect when enabled and token available', async () => {
      mockAuthContext.accessToken = 'test-token';

      renderHook(() => useWebSocket(defaultUrl, { autoConnect: true }));

      await waitFor(() => {
        expect(mockServiceInstance.connect).toHaveBeenCalledWith('test-token');
      });
    });

    it('should not auto-connect when disabled', () => {
      mockAuthContext.accessToken = 'test-token';

      renderHook(() => useWebSocket(defaultUrl, { autoConnect: false }));

      expect(mockServiceInstance.connect).not.toHaveBeenCalled();
    });

    it('should not auto-connect when no token available', () => {
      (mockAuthContext.accessToken as string | null) = null;

      renderHook(() => useWebSocket(defaultUrl, { autoConnect: true }));

      expect(mockServiceInstance.connect).not.toHaveBeenCalled();
    });

    it('should set error when token is missing', async () => {
      (mockAuthContext.accessToken as string | null) = null;

      const { result } = renderHook(() => useWebSocket(defaultUrl));

      act(() => {
        result.current.connect();
      });

      await waitFor(() => {
        expect(result.current.error).toEqual(expect.objectContaining({
          code: 'AUTH_TOKEN_MISSING',
          message: 'Authentication token not available'
        }));
      });
    });
  });

  describe('Connection State Management', () => {
    it('should update state on successful connection', async () => {
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      // Simulate connection success
      act(() => {
        const connectHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'connect'
        )?.[1];
        connectHandler?.();
      });

      expect(result.current.connected).toBe(true);
      expect(result.current.connecting).toBe(false);
      expect(result.current.error).toBe(null);
    });

    it('should update state on disconnection', () => {
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      // First connect
      act(() => {
        const connectHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'connect'
        )?.[1];
        connectHandler?.();
      });

      // Then disconnect
      act(() => {
        const disconnectHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'disconnect'
        )?.[1];
        disconnectHandler?.({ code: 1000, reason: 'Normal closure' });
      });

      expect(result.current.connected).toBe(false);
      expect(result.current.connecting).toBe(false);
    });

    it('should update state on reconnection attempts', () => {
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      act(() => {
        const reconnectHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'reconnect'
        )?.[1];
        reconnectHandler?.({ attempt: 1 });
      });

      expect(result.current.connecting).toBe(true);
      expect(result.current.error).toBe(null);
    });

    it('should handle connection errors', () => {
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      const mockError: MonitoringError = {
        code: 'CONNECTION_FAILED',
        message: 'Failed to establish connection',
        timestamp: new Date().toISOString(),
        recoverable: true
      };

      act(() => {
        const errorHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'error'
        )?.[1];
        errorHandler?.(mockError);
      });

      expect(result.current.error).toEqual(mockError);
      expect(result.current.connecting).toBe(false);
    });
  });

  describe('Message Handling', () => {
    it('should update lastMessage on incoming messages', () => {
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      const mockMessage: WebSocketMessage = {
        type: 'DASHBOARD_UPDATE',
        payload: { jobs: [] },
        timestamp: new Date().toISOString(),
        correlationId: 'test-123'
      };

      act(() => {
        const messageHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'message'
        )?.[1];
        messageHandler?.(mockMessage);
      });

      expect(result.current.lastMessage).toEqual(mockMessage);
    });

    it('should call onMessage callback when provided', () => {
      const onMessage = jest.fn();
      renderHook(() => useWebSocket(defaultUrl, { onMessage }));

      const mockMessage: WebSocketMessage = {
        type: 'DASHBOARD_UPDATE',
        payload: { jobs: [] },
        timestamp: new Date().toISOString(),
        correlationId: 'test-123'
      };

      act(() => {
        const messageHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'message'
        )?.[1];
        messageHandler?.(mockMessage);
      });

      expect(onMessage).toHaveBeenCalledWith(mockMessage);
    });

    it('should handle message buffering', () => {
      const { result } = renderHook(() => 
        useWebSocket(defaultUrl, { messageBufferSize: 3 })
      );

      const messages: WebSocketMessage[] = [
        { type: 'DASHBOARD_UPDATE' as const, payload: {}, timestamp: '2025-08-08T10:00:00Z', correlationId: '1' },
        { type: 'JOB_UPDATE' as const, payload: {}, timestamp: '2025-08-08T10:00:01Z', correlationId: '2' },
        { type: 'ALERT' as const, payload: {}, timestamp: '2025-08-08T10:00:02Z', correlationId: '3' }
      ];

      const messageHandler = mockServiceInstance.on.mock.calls.find(
        call => call[0] === 'message'
      )?.[1];

      act(() => {
        messages.forEach(message => messageHandler?.(message));
      });

      // Should only show the latest message
      expect(result.current.lastMessage).toEqual(messages[2]);
    });
  });

  describe('Manual Actions', () => {
    it('should manually connect when requested', async () => {
      mockAuthContext.accessToken = 'test-token';
      const { result } = renderHook(() => 
        useWebSocket(defaultUrl, { autoConnect: false })
      );

      await act(async () => {
        await result.current.connect();
      });

      expect(mockServiceInstance.connect).toHaveBeenCalledWith('test-token');
    });

    it('should manually disconnect', () => {
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      act(() => {
        result.current.disconnect();
      });

      expect(mockServiceInstance.disconnect).toHaveBeenCalled();
      expect(result.current.connected).toBe(false);
      expect(result.current.connecting).toBe(false);
      expect(result.current.error).toBe(null);
    });

    it('should manually reconnect', async () => {
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      await act(async () => {
        await result.current.reconnect();
      });

      expect(mockServiceInstance.disconnect).toHaveBeenCalled();
      expect(mockServiceInstance.connect).toHaveBeenCalled();
    });

    it('should send messages', () => {
      mockServiceInstance.isConnected.mockReturnValue(true);
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      const testPayload = { test: 'data' };

      act(() => {
        result.current.send(testPayload);
      });

      expect(mockServiceInstance.send).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'CLIENT_MESSAGE',
          payload: testPayload,
          timestamp: expect.any(String),
          correlationId: expect.any(String)
        })
      );
    });

    it('should handle send when not connected', () => {
      mockServiceInstance.isConnected.mockReturnValue(false);
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      act(() => {
        result.current.send({ test: 'data' });
      });

      expect(result.current.error).toEqual(expect.objectContaining({
        code: 'NOT_CONNECTED',
        message: 'WebSocket is not connected'
      }));
    });
  });

  describe('Subscription Management', () => {
    it('should subscribe to topics on connection', () => {
      const subscriptions = ['job-updates', 'alerts'];
      renderHook(() => useWebSocket(defaultUrl, { subscriptions }));

      act(() => {
        const connectHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'connect'
        )?.[1];
        connectHandler?.();
      });

      expect(mockServiceInstance.subscribe).toHaveBeenCalledWith(subscriptions);
    });

    it('should manually subscribe to additional topics', () => {
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      act(() => {
        result.current.subscribe(['new-topic']);
      });

      expect(mockServiceInstance.subscribe).toHaveBeenCalledWith(['new-topic']);
    });

    it('should manually unsubscribe from topics', () => {
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      act(() => {
        result.current.unsubscribe(['old-topic']);
      });

      expect(mockServiceInstance.unsubscribe).toHaveBeenCalledWith(['old-topic']);
    });

    it('should resubscribe on reconnection', () => {
      const subscriptions = ['job-updates'];
      renderHook(() => useWebSocket(defaultUrl, { subscriptions }));

      // Connect initially
      act(() => {
        const connectHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'connect'
        )?.[1];
        connectHandler?.();
      });

      // Clear previous calls
      jest.clearAllMocks();

      // Reconnect
      act(() => {
        const connectHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'connect'
        )?.[1];
        connectHandler?.();
      });

      expect(mockServiceInstance.subscribe).toHaveBeenCalledWith(subscriptions);
    });
  });

  describe('Token Management', () => {
    it('should update token on refresh when reconnectOnTokenRefresh is enabled', () => {
      mockServiceInstance.isConnected.mockReturnValue(true);
      
      renderHook(() => 
        useWebSocket(defaultUrl, { reconnectOnTokenRefresh: true })
      );

      // Update token in auth context
      act(() => {
        mockAuthContext.accessToken = 'new-token';
      });

      expect(mockServiceInstance.updateAuthToken).toHaveBeenCalledWith('new-token');
    });

    it('should not update token when reconnectOnTokenRefresh is disabled', () => {
      mockServiceInstance.isConnected.mockReturnValue(true);
      
      renderHook(() => 
        useWebSocket(defaultUrl, { reconnectOnTokenRefresh: false })
      );

      act(() => {
        mockAuthContext.accessToken = 'new-token';
      });

      expect(mockServiceInstance.updateAuthToken).not.toHaveBeenCalled();
    });
  });

  describe('Callback Handling', () => {
    it('should call onConnect callback', () => {
      const onConnect = jest.fn();
      renderHook(() => useWebSocket(defaultUrl, { onConnect }));

      act(() => {
        const connectHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'connect'
        )?.[1];
        connectHandler?.();
      });

      expect(onConnect).toHaveBeenCalled();
    });

    it('should call onDisconnect callback', () => {
      const onDisconnect = jest.fn();
      renderHook(() => useWebSocket(defaultUrl, { onDisconnect }));

      act(() => {
        const disconnectHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'disconnect'
        )?.[1];
        disconnectHandler?.({ code: 1000, reason: 'Normal closure' });
      });

      expect(onDisconnect).toHaveBeenCalledWith({ code: 1000, reason: 'Normal closure' });
    });

    it('should call onError callback', () => {
      const onError = jest.fn();
      renderHook(() => useWebSocket(defaultUrl, { onError }));

      const mockError: MonitoringError = {
        code: 'TEST_ERROR',
        message: 'Test error',
        timestamp: new Date().toISOString(),
        recoverable: true
      };

      act(() => {
        const errorHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'error'
        )?.[1];
        errorHandler?.(mockError);
      });

      expect(onError).toHaveBeenCalledWith(mockError);
    });

    it('should call onReconnect callback', () => {
      const onReconnect = jest.fn();
      renderHook(() => useWebSocket(defaultUrl, { onReconnect }));

      act(() => {
        const reconnectHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'reconnect'
        )?.[1];
        reconnectHandler?.({ attempt: 2 });
      });

      expect(onReconnect).toHaveBeenCalledWith(2);
    });
  });

  describe('Cleanup and Memory Management', () => {
    it('should cleanup on unmount', () => {
      const { unmount } = renderHook(() => useWebSocket(defaultUrl));

      unmount();

      expect(mockServiceInstance.disconnect).toHaveBeenCalled();
    });

    it('should clear timers on unmount', () => {
      jest.spyOn(global, 'clearTimeout');
      
      const { unmount } = renderHook(() => 
        useWebSocket(defaultUrl, { messageBufferSize: 5 })
      );

      unmount();

      expect(clearTimeout).toHaveBeenCalled();
    });

    it('should not update state after unmount', () => {
      const { result, unmount } = renderHook(() => useWebSocket(defaultUrl));

      const messageHandler = mockServiceInstance.on.mock.calls.find(
        call => call[0] === 'message'
      )?.[1];

      unmount();

      // Try to update state after unmount
      act(() => {
        messageHandler?.({
          type: 'UPDATE',
          payload: {},
          timestamp: new Date().toISOString(),
          correlationId: 'post-unmount'
        });
      });

      // State should not have been updated
      expect(result.current.lastMessage).toBe(null);
    });
  });

  describe('Error Recovery', () => {
    it('should handle connection failures gracefully', async () => {
      mockServiceInstance.connect.mockRejectedValue(new Error('Connection failed'));
      
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      await act(async () => {
        await result.current.connect().catch(() => {}); // Catch to prevent test failure
      });

      expect(result.current.error).toEqual(expect.objectContaining({
        code: 'CONNECTION_FAILED',
        message: 'Connection failed'
      }));
      expect(result.current.connecting).toBe(false);
    });

    it('should reset error state on successful reconnection', () => {
      const { result } = renderHook(() => useWebSocket(defaultUrl));

      // Set error state
      const mockError: MonitoringError = {
        code: 'TEST_ERROR',
        message: 'Test error',
        timestamp: new Date().toISOString(),
        recoverable: true
      };

      act(() => {
        const errorHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'error'
        )?.[1];
        errorHandler?.(mockError);
      });

      expect(result.current.error).toEqual(mockError);

      // Successful connection should clear error
      act(() => {
        const connectHandler = mockServiceInstance.on.mock.calls.find(
          call => call[0] === 'connect'
        )?.[1];
        connectHandler?.();
      });

      expect(result.current.error).toBe(null);
    });
  });

  describe('Performance Optimizations', () => {
    it('should use stable references for callbacks', () => {
      const onMessage = jest.fn();
      const { rerender } = renderHook(
        ({ callback }) => useWebSocket(defaultUrl, { onMessage: callback }),
        { initialProps: { callback: onMessage } }
      );

      const firstRenderRefs = {
        connect: renderHook(() => useWebSocket(defaultUrl)).result.current.connect,
        disconnect: renderHook(() => useWebSocket(defaultUrl)).result.current.disconnect
      };

      rerender({ callback: onMessage });

      const secondRenderRefs = {
        connect: renderHook(() => useWebSocket(defaultUrl)).result.current.connect,
        disconnect: renderHook(() => useWebSocket(defaultUrl)).result.current.disconnect
      };

      // References should remain stable across re-renders
      expect(firstRenderRefs.connect).toBe(secondRenderRefs.connect);
      expect(firstRenderRefs.disconnect).toBe(secondRenderRefs.disconnect);
    });

    it('should not recreate service instance unnecessarily', () => {
      const { rerender } = renderHook(
        (props) => useWebSocket(defaultUrl, props),
        { initialProps: { debugMode: true } }
      );

      const initialCallCount = MockWebSocketService.mock.calls.length;

      // Re-render with same config
      rerender({ debugMode: true });

      expect(MockWebSocketService.mock.calls.length).toBe(initialCallCount);
    });
  });
});