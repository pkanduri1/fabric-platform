/**
 * Global Test Setup
 * 
 * Global configuration and setup for all tests including mocks,
 * polyfills, and environment configuration.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

import { setupGlobalMocks } from '../utils/testUtils';

// Setup global mocks
setupGlobalMocks();

// Mock console methods for cleaner test output
const originalError = console.error;
const originalWarn = console.warn;

beforeAll(() => {
  // Suppress specific console messages during tests
  console.error = (message: any, ...args: any[]) => {
    // Suppress React warnings about act() in tests
    if (
      typeof message === 'string' &&
      (message.includes('Warning: ReactDOM.render is no longer supported') ||
       message.includes('Warning: An invalid form control') ||
       message.includes('Warning: Can\'t perform a React state update'))
    ) {
      return;
    }
    originalError(message, ...args);
  };

  console.warn = (message: any, ...args: any[]) => {
    // Suppress specific warnings
    if (
      typeof message === 'string' &&
      (message.includes('deprecated') ||
       message.includes('findDOMNode is deprecated'))
    ) {
      return;
    }
    originalWarn(message, ...args);
  };
});

afterAll(() => {
  // Restore original console methods
  console.error = originalError;
  console.warn = originalWarn;
});

// Global test utilities
declare global {
  namespace jest {
    interface Matchers<R> {
      toBeWithinRange(floor: number, ceiling: number): R;
      toHaveBeenCalledWithError(errorCode: string): R;
    }
  }
}

// Custom Jest matchers
expect.extend({
  toBeWithinRange(received: number, floor: number, ceiling: number) {
    const pass = received >= floor && received <= ceiling;
    if (pass) {
      return {
        message: () => `expected ${received} not to be within range ${floor} - ${ceiling}`,
        pass: true,
      };
    } else {
      return {
        message: () => `expected ${received} to be within range ${floor} - ${ceiling}`,
        pass: false,
      };
    }
  },

  toHaveBeenCalledWithError(received: jest.Mock, errorCode: string) {
    const calls = received.mock.calls;
    const errorCall = calls.find(call => 
      call[0] === 'error' && 
      call[1] && 
      call[1].code === errorCode
    );

    if (errorCall) {
      return {
        message: () => `expected function not to be called with error code ${errorCode}`,
        pass: true,
      };
    } else {
      return {
        message: () => `expected function to be called with error code ${errorCode}`,
        pass: false,
      };
    }
  }
});

// Mock environment variables
process.env.NODE_ENV = 'test';
process.env.REACT_APP_API_URL = 'http://localhost:8080';
process.env.REACT_APP_WS_URL = 'ws://localhost:8080/ws';
process.env.REACT_APP_VERSION = '1.0.0-test';

// Global test configuration
jest.setTimeout(10000);

// Suppress specific warnings during tests
const originalConsoleWarn = console.warn;
console.warn = (...args: any[]) => {
  if (
    args[0]?.includes?.('React Router') ||
    args[0]?.includes?.('validateDOMNesting') ||
    args[0]?.includes?.('componentWillReceiveProps')
  ) {
    return;
  }
  originalConsoleWarn(...args);
};