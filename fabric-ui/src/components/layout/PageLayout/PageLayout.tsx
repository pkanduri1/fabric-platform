// src/components/layout/PageLayout/PageLayout.tsx
import React, { useState } from 'react';
import { Box, Toolbar } from '@mui/material';
import { Header } from '../Header/Header';
import { Sidebar } from '../Sidebar/Sidebar';

interface PageLayoutProps {
  children: React.ReactNode;
}

const DRAWER_WIDTH = 280;

export const PageLayout: React.FC<PageLayoutProps> = ({ children }) => {
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  return (
    <Box sx={{ display: 'flex' }}>
      <Header onMenuToggle={handleDrawerToggle} menuOpen={mobileOpen} />
      
      <Sidebar
        open={mobileOpen}
        onClose={handleDrawerToggle}
        drawerWidth={DRAWER_WIDTH}
      />

      <Box
        sx={{
    flexGrow: 1,
    width: { sm: 'calc(100% - ${DRAWER_WIDTH}px)' },
    ml: { sm: '${DRAWER_WIDTH}px' },
    minHeight: '100vh',
    bgcolor: 'background.default'
  }}
      >
        <Toolbar />
        {children}
      </Box>
    </Box>
  );
};