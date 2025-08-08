// src/components/layout/Sidebar/Sidebar.tsx
import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Drawer,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemButton,
  Collapse,
  Typography,
  Box,
  Divider,
  Chip,
  IconButton
} from '@mui/material';
import {
  Dashboard,
  Storage,
  Settings,
  Code,
  PlayArrow,
  ExpandLess,
  ExpandMore,
  FolderOpen,
  Work,
  Close,
  AdminPanelSettings,
  Description,
  DynamicForm
} from '@mui/icons-material';
import { useConfigurationContext } from '../../../contexts/ConfigurationContext';

interface SidebarProps {
  open: boolean;
  onClose: () => void;
  drawerWidth: number;
}

export const Sidebar: React.FC<SidebarProps> = ({ open, onClose, drawerWidth }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const {
    loadJobsForSystem,
    sourceSystems,
    selectedSourceSystem,
    selectedJob,
    selectSourceSystem,
    selectJob
  } = useConfigurationContext();

  const [expandedSystems, setExpandedSystems] = React.useState<string[]>([]);

  const toggleSystemExpanded = (systemId: string) => {
    setExpandedSystems(prev =>
      prev.includes(systemId)
        ? prev.filter(id => id !== systemId)
        : [...prev, systemId]
    );
  };

 const handleSystemSelect = async (systemId: string) => {
  try {
    console.log('Selecting system:', systemId);
    await selectSourceSystem(systemId);
    
    // Load jobs from API when expanding
    if (!expandedSystems.includes(systemId)) {
      toggleSystemExpanded(systemId);
      await loadJobsForSystem(systemId); // Use API call
    }
  } catch (error) {
    console.error('Failed to select system:', error);
  }
};

  const handleJobSelect = async (systemId: string, jobName: string) => {
    try {
      await selectJob(jobName);
      navigate(`/configuration/${systemId}/${jobName}`);
    } catch (error) {
      console.error('Failed to select job:', error);
    }
  };

  const handleJobClick = (systemId: string, jobName: string) => {
  navigate(`/configuration/${systemId}/${jobName}`);
};

  const handleTemplateJobSelect = async (systemId: string, jobName: string) => {
    try {
      await selectJob(jobName);
      navigate(`/template-configuration/${systemId}/${jobName}`);
    } catch (error) {
      console.error('Failed to select job:', error);
    }
  };

  const navigationItems = [
    { path: '/dashboard', label: 'Dashboard', icon: <Dashboard /> },
    { path: '/configuration', label: 'Manual Configuration', icon: <Settings /> },
    { path: '/template-configuration', label: 'Template Configuration', icon: <DynamicForm /> },
    { path: '/admin/templates', label: 'Template Admin', icon: <AdminPanelSettings /> },
    { path: '/yaml-preview', label: 'YAML Preview', icon: <Code /> },
    { path: '/testing', label: 'Testing', icon: <PlayArrow /> }
  ];

  const drawer = (
    <Box sx={{ overflow: 'auto', height: '100%' }}>
      {/* Header */}
      <Box sx={{ p: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="h6" noWrap>
          Navigation
        </Typography>
        <IconButton size="small" onClick={onClose} sx={{ display: { sm: 'none' } }}>
          <Close />
        </IconButton>
      </Box>
      
      <Divider />

      {/* Main Navigation */}
      <List>
        {navigationItems.map((item) => (
          <ListItemButton
            key={item.path}
            selected={location.pathname === item.path || location.pathname.startsWith(item.path)}
            onClick={() => navigate(item.path)}
          >
            <ListItemIcon>
              {item.icon}
            </ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
        {/* test button */}
  {/* <ListItemButton onClick={() => navigate('/configuration/hr/p327')}>
    <ListItemIcon>
      <Settings />
    </ListItemIcon>
    <ListItemText primary="TEST: Force Navigate" />
  </ListItemButton> */}
      </List>

      <Divider />

      {/* Source Systems */}
      <Box sx={{ p: 2 }}>
        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
          Source Systems ({sourceSystems.length})
        </Typography>
      </Box>

      <List dense>
        {sourceSystems.map((system) => (
          <React.Fragment key={system.id}>
            <ListItem disablePadding>
              <ListItemButton
                selected={selectedSourceSystem?.id === system.id}
                onClick={() => handleSystemSelect(system.id)}
              >
                <ListItemIcon>
                  <Storage />
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography variant="body2" noWrap>
                        {system.name}
                      </Typography>
                      <span style={{ fontSize: '0.7rem' }}>({system.jobs?.length || 0})</span>
                    </Box>
                  }
                  secondary={
                    <Typography variant="caption" color="text.secondary" noWrap>
                      {system.systemType}
                    </Typography>
                  }
                />
                {system.jobs && system.jobs.length > 0 && (
                  <IconButton
                    size="small"
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleSystemExpanded(system.id);
                    }}
                  >
                    {expandedSystems.includes(system.id) ? <ExpandLess /> : <ExpandMore />}
                  </IconButton>
                )}
              </ListItemButton>
            </ListItem>

            {/* Jobs List */}
            {system.jobs && (
              <Collapse in={expandedSystems.includes(system.id)} timeout="auto" unmountOnExit>
                <List component="div" disablePadding>
                  {system.jobs.map((job) => (
                    <React.Fragment key={job.name || job.jobName}>
                      {/* Manual Configuration Option */}
                      <ListItemButton
                        sx={{ pl: 6 }}
                        selected={selectedJob?.name === (job.name || job.jobName) && location.pathname.includes('/configuration/')}
                        onClick={() => handleJobSelect(system.id, job.name || job.jobName)}
                      >
                        <ListItemIcon sx={{ minWidth: 32 }}>
                            
                          <Settings fontSize="small" />
                        </ListItemIcon>
                        <ListItemText
                          primary={
                            <Typography variant="body2" noWrap>
                              {job.name || job.jobName} (Manual)
                            </Typography>
                          }
                        />
                      </ListItemButton>

                      {/* Template Configuration Option */}
                      <ListItemButton
                        sx={{ pl: 6 }}
                        selected={selectedJob?.name === (job.name || job.jobName) && location.pathname.includes('/template-configuration/')}
                        onClick={() => handleTemplateJobSelect(system.id, job.name || job.jobName)}
                      >
                        <ListItemIcon sx={{ minWidth: 32 }}>
                          <Description fontSize="small" />
                        </ListItemIcon>
                        <ListItemText
                          primary={
                            <Typography variant="body2" noWrap>
                              {job.name || job.jobName} (Template)
                            </Typography>
                          }
                        />
                      </ListItemButton>
                    </React.Fragment>
                  ))}
                </List>
              </Collapse>
            )}
          </React.Fragment>
        ))}
      </List>

      {/* Template Info */}
      <Divider sx={{ mt: 2 }} />
      <Box sx={{ p: 2, bgcolor: 'primary.main', color: 'primary.contrastText', m: 2, borderRadius: 1 }}>
        <Typography variant="caption" fontWeight="bold">
          💡 Template Mode
        </Typography>
        <Typography variant="body2" sx={{ fontSize: '0.7rem', mt: 0.5 }}>
          Use Template Configuration for 90% faster setup with pre-configured target fields.
        </Typography>
      </Box>
    </Box>
  );

  return (
  <Box
    component="nav"
    sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}
    aria-label="navigation"
  >
    {/* Mobile drawer */}
    <Drawer
      variant="temporary"
      open={open}
      onClose={onClose}
      ModalProps={{
        keepMounted: true,
      }}
      sx={{
        display: { xs: 'block', sm: 'none' },
        '& .MuiDrawer-paper': { 
          boxSizing: 'border-box', 
          width: drawerWidth,
          marginTop: '64px',
          height: 'calc(100vh - 64px)'
        },
      }}
    >
      {drawer}
    </Drawer>

    {/* Desktop permanent drawer */}
    <Drawer
      variant="permanent"
      sx={{
        display: { xs: 'none', sm: 'block' },
        '& .MuiDrawer-paper': { 
          boxSizing: 'border-box', 
          width: drawerWidth,
          marginTop: '64px',
          height: 'calc(100vh - 64px)',
          position: 'relative'
        },
      }}
      open
    >
      {drawer}
    </Drawer>
  </Box>
);
}



export default Sidebar;