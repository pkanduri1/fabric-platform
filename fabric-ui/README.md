# Fabric UI - Enterprise Batch Configuration Frontend

React-based frontend application for the Fabric Platform, providing enterprise-grade user interface for batch processing and job configuration management with banking-level security and compliance.

## Table of Contents

- [System Overview](#system-overview)
- [Architecture](#architecture)
- [Manual Job Configuration Interface](#manual-job-configuration-interface)
- [Backend Integration](#backend-integration)
- [Setup and Installation](#setup-and-installation)
- [Development Guidelines](#development-guidelines)
- [Component Architecture](#component-architecture)
- [Security and Authentication](#security-and-authentication)
- [Available Scripts](#available-scripts)
- [Proxy Configuration](#proxy-configuration)
- [Testing](#testing)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

## System Overview

The Fabric UI is a modern React application built with TypeScript and Material-UI, designed to provide operations teams with an intuitive interface for managing batch processing jobs. The application integrates seamlessly with the Fabric API backend through REST API calls.

### Key Features

- **Manual Job Configuration Interface (US001)** - Complete CRUD operations for job configurations
- **Role-Based Access Control** - Dynamic UI rendering based on user permissions
- **Material-UI Design System** - Enterprise-grade responsive design
- **Real-time Data Management** - Live updates and notifications
- **Advanced Filtering & Pagination** - Efficient data browsing and management
- **Comprehensive Form Validation** - Client-side and server-side validation
- **Audit Trail Integration** - SOX-compliant change tracking

### Technology Stack

- **React 18.2** - Modern functional components with hooks
- **TypeScript 4.9** - Type-safe development
- **Material-UI 5.x** - Google's Material Design components
- **React Router 6.x** - Client-side routing
- **Axios** - HTTP client for API communication
- **React Hook Form** - Efficient form management
- **Monaco Editor** - Advanced code/JSON editing capabilities

## Architecture

### Application Structure

```
src/
├── components/
│   ├── layout/
│   │   └── Sidebar/           # Navigation sidebar
│   ├── JobConfigurationForm/  # CRUD form components
│   └── JobConfigurationList/  # Data table components
├── contexts/
│   └── AuthContext.tsx        # Authentication state management
├── pages/
│   └── ManualJobConfigurationPage/  # Main feature page
├── router/
│   └── AppRouter.tsx          # Application routing
├── services/
│   └── api/
│       ├── authApi.ts         # Authentication API calls
│       └── manualJobConfigApi.ts  # Job config API calls
└── App.tsx                    # Root application component
```

### Frontend-Backend Communication

```
┌─────────────────────────────────────────┐
│              React Frontend             │
│         http://localhost:3000           │
└─────────────────────────────────────────┘
                     │ HTTP/REST
                     │ Proxy: "http://localhost:8080"
┌─────────────────────────────────────────┐
│             Spring Boot API             │
│         http://localhost:8080           │
│       /api/v2/manual-job-config/*      │
└─────────────────────────────────────────┘
```

## Manual Job Configuration Interface

### Core Features

1. **Configuration List View**
   - Paginated data table with sorting and filtering
   - Real-time search across job names and types
   - Status-based filtering (Active, Inactive, Deprecated)
   - Bulk operations and export capabilities

2. **Configuration Form**
   - Modal-based form for create/edit/view operations
   - Advanced JSON editor for job parameters
   - Real-time validation with error highlighting
   - Role-based field accessibility

3. **Dashboard Overview**
   - System statistics and health metrics
   - Recent activity and audit trail
   - User permissions and role display

### User Roles and Permissions

| Role | Create | Read | Update | Delete | Execute |
|------|--------|------|--------|--------|---------|
| `JOB_VIEWER` | ❌ | ✅ | ❌ | ❌ | ❌ |
| `JOB_CREATOR` | ✅ | ✅ | ❌ | ❌ | ❌ |
| `JOB_MODIFIER` | ✅ | ✅ | ✅ | ✅ | ❌ |
| `JOB_EXECUTOR` | ❌ | ✅ | ❌ | ❌ | ✅ |

## Backend Integration

### API Service Layer

The application uses a dedicated API service layer for backend communication:

```typescript
// Example API service usage
const configurations = await ManualJobConfigApiService.getAllJobConfigurations({
  page: 0,
  size: 20,
  sortBy: 'createdDate',
  sortDir: 'desc',
  jobType: 'ETL_BATCH',
  status: 'ACTIVE'
});
```

### Authentication Flow

1. **Login** - User credentials sent to `/api/auth/login`
2. **JWT Token** - Received and stored in AuthContext
3. **API Calls** - Token included in Authorization header
4. **Role Extraction** - User roles determined from JWT claims
5. **UI Rendering** - Components conditionally rendered based on roles

### Error Handling

- **Network Errors** - Automatic retry with exponential backoff
- **Authentication Errors** - Automatic redirect to login
- **Validation Errors** - Real-time form field highlighting
- **Server Errors** - User-friendly error messages with correlation IDs

## Setup and Installation

### Prerequisites

- **Node.js 16+** - JavaScript runtime
- **npm 8+** or **yarn 1.22+** - Package manager
- **Backend API** - Fabric API running on http://localhost:8080

### Local Development Setup

1. **Clone Repository**
   ```bash
   git clone <repository-url>
   cd fabric-platform-new/fabric-ui
   ```

2. **Install Dependencies**
   ```bash
   npm install
   # or
   yarn install
   ```

3. **Environment Configuration**
   ```bash
   # Check .env file (already configured)
   cat .env
   
   # Contents should include:
   REACT_APP_API_BASE_URL=http://localhost:8080
   REACT_APP_ENVIRONMENT=development
   ```

4. **Start Development Server**
   ```bash
   npm start
   # or
   yarn start
   ```

5. **Verify Application**
   - Open http://localhost:3000 in your browser
   - Verify backend connectivity through proxy
   - Test authentication flow

### Backend Dependency

**IMPORTANT**: The frontend requires the Fabric API backend to be running.

```bash
# Start backend first (in fabric-api directory)
cd ../fabric-core/fabric-api
mvn spring-boot:run

# Then start frontend (in fabric-ui directory)
cd ../../fabric-ui
npm start
```

## Development Guidelines

### Code Standards

- **TypeScript** - Use strict typing for all components and services
- **Functional Components** - Prefer hooks over class components
- **Material-UI** - Use consistent design system components
- **Responsive Design** - Ensure compatibility across devices
- **Accessibility** - Follow WCAG 2.1 guidelines

### Component Guidelines

```typescript
// Example component structure
import React, { useState, useEffect } from 'react';
import { Box, Typography, Button } from '@mui/material';
import { useAuth } from '../../contexts/AuthContext';

interface ComponentProps {
  title: string;
  onSave: (data: any) => void;
}

export const ExampleComponent: React.FC<ComponentProps> = ({ title, onSave }) => {
  const { hasRole } = useAuth();
  const [data, setData] = useState(null);

  // Component implementation...

  return (
    <Box>
      <Typography variant="h6">{title}</Typography>
      {hasRole('JOB_MODIFIER') && (
        <Button onClick={() => onSave(data)}>Save</Button>
      )}
    </Box>
  );
};
```

### State Management

- **Local State** - Use `useState` for component-specific state
- **Context API** - Use for shared state (authentication, theme)
- **Custom Hooks** - Extract reusable logic into custom hooks
- **API State** - Manage loading, error, and data states consistently

## Component Architecture

### Core Components

#### ManualJobConfigurationPage
Main page component orchestrating the entire job configuration interface.

**Features:**
- Tab-based navigation (List, Dashboard, Health)
- Role-based access control
- Real-time data management
- Comprehensive error handling

#### JobConfigurationForm
Modal-based form component for CRUD operations.

**Features:**
- Create, edit, and view modes
- Advanced JSON parameter editor
- Real-time validation
- Role-based field accessibility

#### JobConfigurationList
Data table component for displaying and managing configurations.

**Features:**
- Pagination and sorting
- Advanced filtering
- Bulk operations
- Export functionality

### Authentication Context

```typescript
// AuthContext usage example
const { user, hasRole, hasAnyRole, login, logout } = useAuth();

// Check specific role
if (hasRole('JOB_MODIFIER')) {
  // Show edit controls
}

// Check multiple roles
if (hasAnyRole(['JOB_CREATOR', 'JOB_MODIFIER'])) {
  // Show create button
}
```

## Security and Authentication

### JWT Token Management

- **Storage** - Tokens stored in memory (AuthContext state)
- **Expiration** - Automatic token refresh and logout on expiry
- **API Integration** - Automatic token inclusion in API calls
- **Security** - No token storage in localStorage for enhanced security

### Role-Based UI Rendering

```typescript
// Conditional rendering based on roles
{canCreateConfigurations && (
  <Button onClick={handleCreate}>Create Configuration</Button>
)}

// Menu items based on permissions
{hasRole('JOB_MODIFIER') && (
  <MenuItem onClick={handleEdit}>Edit</MenuItem>
)}
```

### Input Validation

- **Client-side** - Real-time validation with React Hook Form
- **Server-side** - Backend validation with error display
- **Sanitization** - Input sanitization for security
- **Type Safety** - TypeScript for compile-time validation

## Available Scripts

### Development Scripts

```bash
# Start development server with hot reload
npm start

# Build production bundle
npm run build

# Run test suite
npm test

# Run tests with coverage
npm test -- --coverage

# Type checking without emit
npm run type-check

# Lint TypeScript files
npm run lint

# Format code with Prettier
npm run format
```

### Advanced Scripts

```bash
# Analyze bundle size
npm run build && npx serve -s build

# Update dependencies
npm update

# Security audit
npm audit

# Fix security vulnerabilities
npm audit fix
```

## Proxy Configuration

### Development Proxy Setup

The application uses Create React App's built-in proxy for backend communication:

```json
// package.json
{
  "proxy": "http://localhost:8080"
}
```

### How Proxy Works

1. **Frontend Request** - `/api/v2/manual-job-config/`
2. **Proxy Intercept** - CRA development server intercepts the request
3. **Backend Forward** - Request forwarded to `http://localhost:8080/api/v2/manual-job-config/`
4. **Response Relay** - Backend response relayed back to frontend

### CORS Configuration

The backend is configured to accept requests from the frontend origin:

```properties
# Backend application.properties
fabric.cors.allowed-origins=http://localhost:3000
```

### Production Configuration

For production deployment, configure a proper reverse proxy (nginx, Apache) instead of the development proxy:

```nginx
# nginx configuration example
location /api/ {
    proxy_pass http://backend-service:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

## Testing

### Test Strategy

- **Unit Tests** - Individual component testing with Jest and React Testing Library
- **Integration Tests** - API integration and user flow testing
- **E2E Tests** - Full application workflow testing (future)

### Running Tests

```bash
# Run all tests
npm test

# Run tests in watch mode
npm test -- --watch

# Run tests with coverage report
npm test -- --coverage

# Run specific test file
npm test ManualJobConfigurationPage.test.tsx
```

### Test Examples

```typescript
// Component test example
import { render, screen, fireEvent } from '@testing-library/react';
import { ManualJobConfigurationPage } from './ManualJobConfigurationPage';
import { AuthContext } from '../../contexts/AuthContext';

test('renders job configuration page for authorized user', () => {
  const mockAuthValue = {
    user: { username: 'test', roles: ['JOB_VIEWER'] },
    hasRole: (role: string) => role === 'JOB_VIEWER',
    hasAnyRole: (roles: string[]) => roles.includes('JOB_VIEWER')
  };

  render(
    <AuthContext.Provider value={mockAuthValue}>
      <ManualJobConfigurationPage />
    </AuthContext.Provider>
  );

  expect(screen.getByText('Manual Job Configuration Management')).toBeInTheDocument();
});
```

## Deployment

### Production Build

```bash
# Create optimized production build
npm run build

# Serve production build locally for testing
npx serve -s build
```

### Environment Configuration

```bash
# Production environment variables
REACT_APP_API_BASE_URL=https://api.fabric-platform.com
REACT_APP_ENVIRONMENT=production
REACT_APP_VERSION=1.0.0
```

### Docker Deployment

```dockerfile
# Multi-stage Dockerfile
FROM node:16-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### CDN and Static Asset Optimization

- Configure CDN for static assets
- Enable gzip compression
- Set appropriate cache headers
- Optimize bundle splitting

## Troubleshooting

### Common Issues

#### Backend Connection Issues

```bash
# Check if backend is running
curl http://localhost:8080/actuator/health

# Verify proxy configuration
# Check package.json proxy setting
# Ensure no conflicting proxy middleware
```

#### Authentication Issues

```bash
# Check JWT token in browser DevTools
# Application -> Local Storage / Session Storage
# Network tab -> Check Authorization headers
# Console -> Check for authentication errors
```

#### Build Issues

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Clear React cache
npm start -- --reset-cache

# Type checking issues
npm run type-check
```

### Performance Optimization

- **Code Splitting** - Implement lazy loading for routes
- **Memoization** - Use React.memo for expensive components
- **Bundle Analysis** - Regular bundle size monitoring
- **API Optimization** - Implement request caching and debouncing

### Debugging

```bash
# Enable detailed logging
REACT_APP_LOG_LEVEL=debug npm start

# React Developer Tools
# Install browser extension for component debugging

# Network debugging
# Use browser DevTools Network tab
# Check API requests/responses
```

## Browser Support

- **Chrome** - Latest 2 versions
- **Firefox** - Latest 2 versions
- **Safari** - Latest 2 versions
- **Edge** - Latest 2 versions
- **Mobile** - iOS Safari, Android Chrome

## Performance Metrics

- **Initial Load** - < 3 seconds on 3G
- **Interaction** - < 100ms response time
- **Bundle Size** - < 1MB gzipped
- **Accessibility** - WCAG 2.1 AA compliance

---

## Support

For technical support or questions:
- **Development Team**: Senior Full Stack Developer Agent
- **UI/UX Review**: Frontend Architecture Team
- **Product Owner**: Lending Product Owner

**Last Updated**: August 14, 2025  
**Version**: 1.0.0  
**Documentation Generated**: Claude Code (claude.ai/code)