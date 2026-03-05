// src/router/AppRouter.tsx
import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { HomePage } from '../pages/HomePage';
import { ConfigurationPage } from '../pages/ConfigurationPage';
import { YamlPreviewPage } from '../pages/YamlPreviewPage';
import { TestingPage } from '../pages/TestingPage';
import { PageLayout } from '../components/layout/PageLayout';
import TemplateConfigurationPage from '../pages/TemplateConfigurationPage/TemplateConfigurationPage';
import TemplateStudioPage from '../pages/TemplateStudioPage/TemplateStudioPage';
import TemplateAdminPage from '../pages/TemplateAdminPage/TemplateAdminPage';
import { MonitoringDashboard } from '../pages/MonitoringDashboard/MonitoringDashboard';
import { ManualJobConfigurationPage } from '../pages/ManualJobConfigurationPage';
import { LoginPage } from '../pages/LoginPage/LoginPage';
import { useAuth } from '../contexts/AuthContext';
import { useLocation } from 'react-router-dom';

/** Redirect unauthenticated users to /login, show a blank screen while auth initializes */
const ProtectedLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isInitialized } = useAuth();
  if (!isInitialized) return null;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
};

export const AppRouter: React.FC = () => {

    const location = useLocation();
    // Log the current path for debugging
    console.log('AppRouter - Current location:', location.pathname);
    console.log('AppRouter - Rendering routes');
    return (
        <Routes>
            {/* Public route — no PageLayout, no auth guard */}
            <Route path="/login" element={<LoginPage />} />

            {/* All protected routes wrapped in PageLayout + auth guard */}
            <Route path="*" element={
                <ProtectedLayout>
                    <PageLayout>
                        <Routes>
                            <Route index element={<Navigate to="/dashboard" replace />} />
                            <Route path="dashboard" element={<HomePage />} />
                            {/* <Route path="configuration" element={<ConfigurationPage />} />
                <Route path="configuration/:systemId/:jobName" element={<ConfigurationPage />} /> */}
                            <Route path="configuration/:systemId/:jobName" element={<ConfigurationPage />} />
                            <Route path="configuration" element={<ConfigurationPage />} />

                            {/* New Template-Based Configuration Routes */}
                            <Route path="template-configuration" element={<TemplateConfigurationPage />} />
                            <Route path="template-configuration/:systemId/:jobName" element={<TemplateConfigurationPage />} />

                            {/* Template Administration Routes */}
                            <Route path="admin/templates" element={<TemplateAdminPage />} />
                            <Route path="template-studio" element={<TemplateStudioPage />} />

                            {/* Monitoring Dashboard Routes */}
                            <Route path="monitoring" element={<MonitoringDashboard />} />

                            {/* Manual Job Configuration Routes - Phase 3A */}
                            <Route path="manual-job-config" element={<ManualJobConfigurationPage />} />

                            <Route path="yaml-preview" element={<YamlPreviewPage />} />
                            <Route path="testing" element={<TestingPage />} />
                            <Route path="*" element={<Navigate to="/dashboard" replace />} />
                        </Routes>
                    </PageLayout>
                </ProtectedLayout>
            } />
        </Routes>
    );
};