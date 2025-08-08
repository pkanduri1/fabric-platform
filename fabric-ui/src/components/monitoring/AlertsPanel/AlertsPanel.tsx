/**
 * Alerts Panel Component
 * 
 * Real-time alerts management panel with filtering, acknowledgment,
 * and notification capabilities. Displays critical system alerts
 * with appropriate visual indicators and actions.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-08
 */

import React, { useState, useMemo, useCallback } from 'react';
import {
  Box,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  ListItemSecondaryAction,
  IconButton,
  Chip,
  Typography,
  Badge,
  Tooltip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Divider,
  Alert as MuiAlert,
  useTheme
} from '@mui/material';
import {
  Error as ErrorIcon,
  Warning,
  Info,
  CheckCircle,
  Check,
  Close,
  FilterList,
  VolumeUp,
  VolumeOff
} from '@mui/icons-material';

import { 
  Alert, 
  AlertSeverity, 
  AlertType, 
  AlertFilters,
  AlertPanelProps 
} from '../../../types/monitoring';
import { formatTimestamp } from '../../../utils/formatters';

// Get severity icon and color
function getSeverityIcon(severity: AlertSeverity) {
  switch (severity) {
    case 'CRITICAL':
      return <ErrorIcon color="error" />;
    case 'ERROR':
      return <ErrorIcon color="error" />;
    case 'WARNING':
      return <Warning color="warning" />;
    case 'INFO':
    default:
      return <Info color="info" />;
  }
}

function getSeverityColor(severity: AlertSeverity): 'error' | 'warning' | 'info' | 'success' {
  switch (severity) {
    case 'CRITICAL':
    case 'ERROR':
      return 'error';
    case 'WARNING':
      return 'warning';
    case 'INFO':
    default:
      return 'info';
  }
}

/**
 * AlertsPanel Component
 */
export const AlertsPanel: React.FC<AlertPanelProps> = ({
  alerts,
  onAcknowledge,
  onResolve,
  onConfigure,
  filters = {},
  soundEnabled = false
}) => {
  const theme = useTheme();
  
  // Component state
  const [localFilters, setLocalFilters] = useState<AlertFilters>(filters);
  const [selectedAlert, setSelectedAlert] = useState<Alert | null>(null);
  const [ackDialogOpen, setAckDialogOpen] = useState(false);
  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
  const [ackComment, setAckComment] = useState('');
  const [resolution, setResolution] = useState('');
  
  /**
   * Filter alerts based on current filters
   */
  const filteredAlerts = useMemo(() => {
    return alerts.filter(alert => {
      // Severity filter
      if (localFilters.severity && localFilters.severity.length > 0) {
        if (!localFilters.severity.includes(alert.severity)) return false;
      }
      
      // Type filter
      if (localFilters.type && localFilters.type.length > 0) {
        if (!localFilters.type.includes(alert.type)) return false;
      }
      
      // Source system filter
      if (localFilters.sourceSystem && localFilters.sourceSystem.length > 0) {
        if (!alert.sourceSystem || !localFilters.sourceSystem.includes(alert.sourceSystem)) return false;
      }
      
      // Acknowledged filter
      if (localFilters.acknowledged !== undefined) {
        if (alert.acknowledged !== localFilters.acknowledged) return false;
      }
      
      // Resolved filter
      if (localFilters.resolved !== undefined) {
        if (alert.resolved !== localFilters.resolved) return false;
      }
      
      // Date range filter
      if (localFilters.dateRange) {
        const alertDate = new Date(alert.timestamp);
        const startDate = new Date(localFilters.dateRange.start);
        const endDate = new Date(localFilters.dateRange.end);
        
        if (alertDate < startDate || alertDate > endDate) return false;
      }
      
      return true;
    });
  }, [alerts, localFilters]);
  
  /**
   * Get alert statistics
   */
  const alertStats = useMemo(() => {
    const total = filteredAlerts.length;
    const unacknowledged = filteredAlerts.filter(a => !a.acknowledged).length;
    const critical = filteredAlerts.filter(a => a.severity === 'CRITICAL').length;
    const errors = filteredAlerts.filter(a => a.severity === 'ERROR').length;
    const warnings = filteredAlerts.filter(a => a.severity === 'WARNING').length;
    
    return { total, unacknowledged, critical, errors, warnings };
  }, [filteredAlerts]);
  
  /**
   * Handle alert acknowledgment
   */
  const handleAcknowledge = useCallback(() => {
    if (selectedAlert && onAcknowledge) {
      onAcknowledge(selectedAlert.alertId);
      setAckDialogOpen(false);
      setSelectedAlert(null);
      setAckComment('');
    }
  }, [selectedAlert, onAcknowledge]);
  
  /**
   * Handle alert resolution
   */
  const handleResolve = useCallback(() => {
    if (selectedAlert && onResolve) {
      onResolve(selectedAlert.alertId);
      setResolveDialogOpen(false);
      setSelectedAlert(null);
      setResolution('');
    }
  }, [selectedAlert, onResolve]);
  
  /**
   * Render alert item
   */
  const renderAlertItem = (alert: Alert) => (
    <ListItem
      key={alert.alertId}
      divider
      sx={{
        bgcolor: alert.acknowledged ? 'transparent' : 
                alert.severity === 'CRITICAL' ? 'error.light' : 
                alert.severity === 'ERROR' ? 'error.light' : 'transparent',
        opacity: alert.acknowledged ? 0.7 : 1,
        '&:hover': {
          bgcolor: 'action.hover'
        }
      }}
    >
      <ListItemIcon>
        <Badge
          color={alert.escalated ? "error" : "default"}
          variant={alert.escalated ? "dot" : undefined}
        >
          {getSeverityIcon(alert.severity)}
        </Badge>
      </ListItemIcon>
      
      <ListItemText
        primary={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2" fontWeight="medium">
              {alert.title}
            </Typography>
            <Chip
              label={alert.severity}
              color={getSeverityColor(alert.severity)}
              size="small"
            />
            {alert.sourceSystem && (
              <Chip
                label={alert.sourceSystem}
                size="small"
                variant="outlined"
              />
            )}
          </Box>
        }
        secondary={
          <Box sx={{ mt: 0.5 }}>
            <Typography variant="caption" color="text.secondary" display="block">
              {alert.description}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {formatTimestamp(alert.timestamp)}
            </Typography>
            {alert.acknowledgedBy && (
              <Typography variant="caption" color="success.main" display="block">
                Acknowledged by {alert.acknowledgedBy}
              </Typography>
            )}
          </Box>
        }
      />
      
      <ListItemSecondaryAction>
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          {!alert.acknowledged && onAcknowledge && (
            <Tooltip title="Acknowledge">
              <IconButton
                size="small"
                onClick={() => {
                  setSelectedAlert(alert);
                  setAckDialogOpen(true);
                }}
              >
                <Check />
              </IconButton>
            </Tooltip>
          )}
          
          {alert.acknowledged && !alert.resolved && onResolve && (
            <Tooltip title="Resolve">
              <IconButton
                size="small"
                color="success"
                onClick={() => {
                  setSelectedAlert(alert);
                  setResolveDialogOpen(true);
                }}
              >
                <CheckCircle />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      </ListItemSecondaryAction>
    </ListItem>
  );
  
  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header with stats */}
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="h6">
            Alerts ({alertStats.total})
          </Typography>
          <Box sx={{ display: 'flex', gap: 0.5 }}>
            <Tooltip title={soundEnabled ? "Mute Alerts" : "Enable Alert Sounds"}>
              <IconButton size="small">
                {soundEnabled ? <VolumeUp /> : <VolumeOff />}
              </IconButton>
            </Tooltip>
            <Tooltip title="Filter Alerts">
              <IconButton size="small">
                <FilterList />
              </IconButton>
            </Tooltip>
          </Box>
        </Box>
        
        {/* Alert stats chips */}
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          {alertStats.unacknowledged > 0 && (
            <Chip
              label={`${alertStats.unacknowledged} Unacknowledged`}
              color="warning"
              size="small"
            />
          )}
          {alertStats.critical > 0 && (
            <Chip
              label={`${alertStats.critical} Critical`}
              color="error"
              size="small"
            />
          )}
          {alertStats.errors > 0 && (
            <Chip
              label={`${alertStats.errors} Errors`}
              color="error"
              variant="outlined"
              size="small"
            />
          )}
          {alertStats.warnings > 0 && (
            <Chip
              label={`${alertStats.warnings} Warnings`}
              color="warning"
              variant="outlined"
              size="small"
            />
          )}
        </Box>
      </Box>
      
      {/* Alerts list */}
      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {filteredAlerts.length === 0 ? (
          <Box sx={{ 
            display: 'flex', 
            flexDirection: 'column',
            alignItems: 'center', 
            justifyContent: 'center', 
            height: '100%',
            p: 3 
          }}>
            <CheckCircle color="success" sx={{ fontSize: 48, mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              No Alerts
            </Typography>
            <Typography variant="body2" color="text.secondary">
              All systems are running normally
            </Typography>
          </Box>
        ) : (
          <List sx={{ p: 0 }}>
            {filteredAlerts.map(renderAlertItem)}
          </List>
        )}
      </Box>
      
      {/* Acknowledge Dialog */}
      <Dialog open={ackDialogOpen} onClose={() => setAckDialogOpen(false)}>
        <DialogTitle>Acknowledge Alert</DialogTitle>
        <DialogContent>
          {selectedAlert && (
            <>
              <Typography variant="body1" gutterBottom>
                {selectedAlert.title}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {selectedAlert.description}
              </Typography>
              <TextField
                autoFocus
                margin="dense"
                label="Comment (Optional)"
                fullWidth
                multiline
                rows={3}
                value={ackComment}
                onChange={(e) => setAckComment(e.target.value)}
                placeholder="Add a comment about this acknowledgment..."
              />
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAckDialogOpen(false)}>
            Cancel
          </Button>
          <Button onClick={handleAcknowledge} variant="contained">
            Acknowledge
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Resolve Dialog */}
      <Dialog open={resolveDialogOpen} onClose={() => setResolveDialogOpen(false)}>
        <DialogTitle>Resolve Alert</DialogTitle>
        <DialogContent>
          {selectedAlert && (
            <>
              <Typography variant="body1" gutterBottom>
                {selectedAlert.title}
              </Typography>
              <TextField
                autoFocus
                margin="dense"
                label="Resolution Details"
                fullWidth
                multiline
                rows={4}
                value={resolution}
                onChange={(e) => setResolution(e.target.value)}
                placeholder="Describe how this alert was resolved..."
                required
              />
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setResolveDialogOpen(false)}>
            Cancel
          </Button>
          <Button 
            onClick={handleResolve} 
            variant="contained"
            disabled={!resolution.trim()}
          >
            Resolve
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AlertsPanel;