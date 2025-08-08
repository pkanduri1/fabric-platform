/**
 * Jest Configuration for React Testing
 * 
 * Comprehensive Jest configuration for testing React components,
 * hooks, services, and integration scenarios in the monitoring dashboard.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

const path = require('path');

module.exports = {
  // Test environment
  testEnvironment: 'jsdom',

  // Setup files
  setupFilesAfterEnv: [
    '<rootDir>/src/setupTests.ts',
    '<rootDir>/src/__tests__/setup/globalSetup.ts'
  ],

  // Module name mapping for path aliases and assets
  moduleNameMapping: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '^@components/(.*)$': '<rootDir>/src/components/$1',
    '^@pages/(.*)$': '<rootDir>/src/pages/$1',
    '^@services/(.*)$': '<rootDir>/src/services/$1',
    '^@hooks/(.*)$': '<rootDir>/src/hooks/$1',
    '^@contexts/(.*)$': '<rootDir>/src/contexts/$1',
    '^@types/(.*)$': '<rootDir>/src/types/$1',
    '^@utils/(.*)$': '<rootDir>/src/utils/$1',
    '^@assets/(.*)$': '<rootDir>/src/assets/$1',
    '^@tests/(.*)$': '<rootDir>/src/__tests__/$1',
    
    // Mock static assets
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
    '\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$': '<rootDir>/src/__tests__/mocks/fileMock.js'
  },

  // Transform files
  transform: {
    '^.+\\.(ts|tsx)$': ['ts-jest', {
      tsconfig: {
        jsx: 'react-jsx'
      }
    }],
    '^.+\\.(js|jsx)$': ['babel-jest', {
      presets: [
        ['@babel/preset-env', { targets: { node: 'current' } }],
        ['@babel/preset-react', { runtime: 'automatic' }],
        '@babel/preset-typescript'
      ]
    }]
  },

  // Files to ignore during transformation
  transformIgnorePatterns: [
    'node_modules/(?!(@testing-library|@mui|@emotion|react-chartjs-2|chart.js)/)'
  ],

  // Test file patterns
  testMatch: [
    '<rootDir>/src/**/__tests__/**/*.(test|spec).(ts|tsx|js|jsx)',
    '<rootDir>/src/**/*.(test|spec).(ts|tsx|js|jsx)'
  ],

  // Files to ignore
  testPathIgnorePatterns: [
    '<rootDir>/node_modules/',
    '<rootDir>/build/',
    '<rootDir>/src/__tests__/setup/',
    '<rootDir>/src/__tests__/mocks/',
    '<rootDir>/src/__tests__/utils/'
  ],

  // Coverage configuration
  collectCoverage: true,
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
    '!src/index.tsx',
    '!src/reportWebVitals.ts',
    '!src/setupTests.ts',
    '!src/**/__tests__/**',
    '!src/**/*.stories.*',
    '!src/**/*.test.*',
    '!src/**/*.spec.*'
  ],

  coverageReporters: [
    'text',
    'text-summary',
    'html',
    'lcov',
    'json-summary'
  ],

  coverageDirectory: '<rootDir>/coverage',

  // Coverage thresholds
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80
    },
    // Higher thresholds for critical components
    './src/services/websocket/': {
      branches: 90,
      functions: 90,
      lines: 90,
      statements: 90
    },
    './src/hooks/useWebSocket.ts': {
      branches: 85,
      functions: 85,
      lines: 85,
      statements: 85
    },
    './src/pages/MonitoringDashboard/': {
      branches: 85,
      functions: 85,
      lines: 85,
      statements: 85
    }
  },

  // Global test configuration
  globals: {
    'ts-jest': {
      tsconfig: '<rootDir>/tsconfig.json'
    }
  },

  // Test timeout
  testTimeout: 10000,

  // Verbose output for debugging
  verbose: false,

  // Clear mocks between tests
  clearMocks: true,
  resetMocks: true,
  restoreMocks: true,

  // Handle async operations
  maxWorkers: '50%',

  // Error handling
  errorOnDeprecated: true,

  // Watch plugins for development
  watchPlugins: [
    'jest-watch-typeahead/filename',
    'jest-watch-typeahead/testname'
  ],

  // Custom reporters
  reporters: [
    'default',
    ['jest-junit', {
      outputDirectory: '<rootDir>/test-results',
      outputName: 'junit.xml',
      suiteName: 'React Monitoring Dashboard Tests'
    }],
    ['jest-html-reporters', {
      publicPath: '<rootDir>/test-results',
      filename: 'test-report.html',
      expand: true,
      hideIcon: false,
      pageTitle: 'US008 Frontend Test Report'
    }]
  ],

  // Module directories
  moduleDirectories: [
    'node_modules',
    '<rootDir>/src'
  ],

  // File extensions to consider
  moduleFileExtensions: [
    'ts',
    'tsx',
    'js',
    'jsx',
    'json'
  ],

  // Test environment options
  testEnvironmentOptions: {
    url: 'http://localhost:3000'
  },

  // Snapshot serializers
  snapshotSerializers: [
    '@emotion/jest/serializer'
  ],

  // Mock WebSocket globally
  setupFiles: [
    '<rootDir>/src/__tests__/setup/websocketMock.js'
  ]
};