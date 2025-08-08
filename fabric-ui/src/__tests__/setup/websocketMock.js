/**
 * WebSocket Mock Setup
 * 
 * Global WebSocket mock for consistent testing across all test suites.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

// Basic WebSocket mock for global availability
class MockWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  constructor(url, protocols) {
    this.url = url;
    this.protocols = protocols;
    this.readyState = MockWebSocket.CONNECTING;
    this.onopen = null;
    this.onclose = null;
    this.onmessage = null;
    this.onerror = null;

    // Simulate connection opening
    setTimeout(() => {
      this.readyState = MockWebSocket.OPEN;
      if (this.onopen) {
        this.onopen(new Event('open'));
      }
    }, 0);
  }

  send(data) {
    if (this.readyState !== MockWebSocket.OPEN) {
      throw new Error('WebSocket is not open');
    }
  }

  close(code = 1000, reason = '') {
    this.readyState = MockWebSocket.CLOSED;
    if (this.onclose) {
      this.onclose(new CloseEvent('close', { code, reason, wasClean: true }));
    }
  }

  addEventListener(type, listener) {
    // Basic event listener support
  }

  removeEventListener(type, listener) {
    // Basic event listener support
  }
}

// Set global WebSocket mock
global.WebSocket = MockWebSocket;