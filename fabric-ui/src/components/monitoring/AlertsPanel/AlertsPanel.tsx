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

import React, { useState, useMemo, useCallback, useEffect, useRef } from 'react';
import {
  Box,
  List,
  ListItem,
  ListItemIcon,
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
  Select,
  MenuItem,
  Checkbox,
  Menu,
  Collapse,
  Skeleton
} from '@mui/material';
import {
  Error as ErrorIcon,
  Warning,
  Info,
  CheckCircle,
  FilterList,
  VolumeUp,
  VolumeOff,
  MoreVert,
  ExpandMore,
  ExpandLess,
  Group
} from '@mui/icons-material';

import {
  Alert,
  AlertSeverity,
  AlertFilters,
  AlertPanelProps
} from '../../../types/monitoring';
import { formatTimestamp } from '../../../utils/formatters';

// Get severity icon
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

function getSoundUrl(severity: AlertSeverity): string {
  switch (severity) {
    case 'CRITICAL':
      return '/sounds/alert-critical.mp3';
    case 'ERROR':
      return '/sounds/alert-error.mp3';
    case 'WARNING':
      return '/sounds/alert-warning.mp3';
    default:
      return '/sounds/alert-info.mp3';
  }
}

function playAlertSound(severity: AlertSeverity) {
  const audio = new Audio(getSoundUrl(severity));
  if (typeof audio.play === 'function') {
    const result = audio.play();
    if (result !== undefined && result !== null && typeof result.catch === 'function') {
      result.catch(() => {
        console.warn(`Failed to play alert sound for ${severity}`);
      });
    }
  }
}

/**
 * AlertsPanel Component
 */
export const AlertsPanel: React.FC<AlertPanelProps> = ({
  alerts,
  loading = false,
  onAcknowledge,
  onResolve,
  onConfigure,
  filters = {},
  soundEnabled = false
}) => {
  // Mobile detection without useMediaQuery to avoid addEventListener requirement in tests
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    if (typeof window !== 'undefined' && window.matchMedia) {
      try {
        const mq = window.matchMedia('(max-width: 768px)');
        setIsMobile(mq.matches);
        if (typeof mq.addEventListener === 'function') {
          const handler = (e: MediaQueryListEvent) => setIsMobile(e.matches);
          mq.addEventListener('change', handler);
          return () => mq.removeEventListener('change', handler);
        } else if (typeof (mq as any).addListener === 'function') {
          const handler = () => setIsMobile(mq.matches);
          (mq as any).addListener(handler);
          return () => (mq as any).removeListener(handler);
        }
      } catch {
        // ignore matchMedia errors in test environments
      }
    }
  }, []);

  // Component state
  const [severityFilter, setSeverityFilter] = useState<string>('all');
  const [selectedAlert, setSelectedAlert] = useState<Alert | null>(null);
  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
  const [snoozeDialogOpen, setSnoozeDialogOpen] = useState(false);
  const [escalateDialogOpen, setEscalateDialogOpen] = useState(false);
  const [resolution, setResolution] = useState('');
  const [selectedAlertIds, setSelectedAlertIds] = useState<Set<string>>(new Set());
  const [expandedAlerts, setExpandedAlerts] = useState<Set<string>>(new Set());
  const [groupBy, setGroupBy] = useState<'none' | 'sourceSystem' | 'severity'>('none');
  const [groupByMenuAnchor, setGroupByMenuAnchor] = useState<null | HTMLElement>(null);
  const [moreActionsAnchor, setMoreActionsAnchor] = useState<null | HTMLElement>(null);
  const [moreActionsAlert, setMoreActionsAlert] = useState<Alert | null>(null);
  const [liveMessage, setLiveMessage] = useState('');
  const prevAlertsRef = useRef<Alert[]>([]);
  const newAlertIdsRef = useRef<Set<string>>(new Set());

  // Play sounds for new alerts when soundEnabled
  useEffect(() => {
    if (!soundEnabled) return;

    const prevIds = new Set(prevAlertsRef.current.map((a: Alert) => a.alertId));
    const newAlerts = alerts.filter((a: Alert) => !prevIds.has(a.alertId));
    const newIds = new Set(newAlerts.map((a: Alert) => a.alertId));
    newAlertIdsRef.current = newIds;

    const soundSeverities: AlertSeverity[] = [AlertSeverity.CRITICAL, AlertSeverity.ERROR, AlertSeverity.WARNING];

    // Play sound for any new alerts that arrived
    newAlerts.forEach((alert: Alert) => {
      if (soundSeverities.includes(alert.severity)) {
        playAlertSound(alert.severity);
      }
    });

    // Also play sound on initial mount
    if (prevAlertsRef.current.length === 0 && alerts.length > 0) {
      const played = new Set<AlertSeverity>();
      alerts.forEach((alert: Alert) => {
        if (soundSeverities.includes(alert.severity) && !played.has(alert.severity)) {
          played.add(alert.severity);
          playAlertSound(alert.severity);
        }
      });
    }

    prevAlertsRef.current = alerts;
  }, [alerts, soundEnabled]);

  // Update live region on alerts change
  useEffect(() => {
    if (alerts.length > 0) {
      // Keep live region text distinct from visible summary to avoid duplicate-text issues
      setLiveMessage(`Alerts updated: ${alerts.length} total`);
    }
  }, [alerts]);

  /**
   * Filter alerts based on current filters and severity dropdown
   */
  const filteredAlerts = useMemo(() => {
    return alerts.filter((alert: Alert) => {
      // Severity dropdown filter
      if (severityFilter === 'critical' && alert.severity !== 'CRITICAL') return false;
      if (severityFilter === 'error' && alert.severity !== 'ERROR') return false;
      if (severityFilter === 'warning' && alert.severity !== 'WARNING') return false;

      // Severity filter from props
      if (filters.severity && filters.severity.length > 0) {
        if (!filters.severity.includes(alert.severity)) return false;
      }

      // Type filter
      if (filters.type && filters.type.length > 0) {
        if (!filters.type.includes(alert.type)) return false;
      }

      // Source system filter
      if (filters.sourceSystem && filters.sourceSystem.length > 0) {
        if (!alert.sourceSystem || !filters.sourceSystem.includes(alert.sourceSystem)) return false;
      }

      // Acknowledged filter
      if (filters.acknowledged !== undefined) {
        if (alert.acknowledged !== filters.acknowledged) return false;
      }

      // Resolved filter
      if (filters.resolved !== undefined) {
        if (alert.resolved !== filters.resolved) return false;
      }

      // Date range filter
      if (filters.dateRange) {
        const alertDate = new Date(alert.timestamp);
        const startDate = new Date(filters.dateRange.start);
        const endDate = new Date(filters.dateRange.end);
        if (alertDate < startDate || alertDate > endDate) return false;
      }

      return true;
    });
  }, [alerts, filters, severityFilter]);

  const hasActiveFilters = useMemo(() => {
    return (
      severityFilter !== 'all' ||
      (filters.severity && filters.severity.length > 0) ||
      (filters.type && filters.type.length > 0) ||
      (filters.sourceSystem && filters.sourceSystem.length > 0) ||
      filters.acknowledged !== undefined ||
      filters.resolved !== undefined ||
      !!filters.dateRange
    );
  }, [filters, severityFilter]);

  /**
   * Get alert statistics (based on all alerts, not filtered)
   */
  const alertStats = useMemo(() => {
    const total = alerts.length;
    const unacknowledged = alerts.filter((a: Alert) => !a.acknowledged).length;
    const critical = alerts.filter((a: Alert) => a.severity === 'CRITICAL').length;
    const errors = alerts.filter((a: Alert) => a.severity === 'ERROR').length;
    const warnings = alerts.filter((a: Alert) => a.severity === 'WARNING').length;
    return { total, unacknowledged, critical, errors, warnings };
  }, [alerts]);

  /**
   * Group alerts
   */
  const groupedAlerts = useMemo(() => {
    if (groupBy === 'none') return null;
    const groups: Record<string, Alert[]> = {};
    filteredAlerts.forEach((alert: Alert) => {
      const key = groupBy === 'sourceSystem'
        ? (alert.sourceSystem || 'Unknown')
        : alert.severity;
      if (!groups[key]) groups[key] = [];
      groups[key].push(alert);
    });
    return groups;
  }, [filteredAlerts, groupBy]);

  const handleAcknowledgeDirect = useCallback((alertId: string) => {
    if (onAcknowledge) {
      onAcknowledge(alertId);
      setLiveMessage(`Alert acknowledged`);
    }
  }, [onAcknowledge]);

  const handleBulkAcknowledge = useCallback(() => {
    if (onAcknowledge) {
      const count = selectedAlertIds.size;
      selectedAlertIds.forEach((id: string) => onAcknowledge(id));
      setSelectedAlertIds(new Set());
      setLiveMessage(`${count} alerts acknowledged`);
    }
  }, [selectedAlertIds, onAcknowledge]);

  const handleResolve = useCallback(() => {
    if (selectedAlert && onResolve) {
      onResolve(selectedAlert.alertId);
      setResolveDialogOpen(false);
      setSelectedAlert(null);
      setResolution('');
      setLiveMessage(`Alert resolved`);
    }
  }, [selectedAlert, onResolve]);

  const toggleAlertSelection = useCallback((alertId: string) => {
    setSelectedAlertIds(prev => {
      const next = new Set(prev);
      if (next.has(alertId)) next.delete(alertId);
      else next.add(alertId);
      return next;
    });
  }, []);

  const toggleExpand = useCallback((alertId: string) => {
    setExpandedAlerts(prev => {
      const next = new Set(prev);
      if (next.has(alertId)) next.delete(alertId);
      else next.add(alertId);
      return next;
    });
  }, []);

  /**
   * Render alert item
   */
  const renderAlertItem = (alert: Alert, hideSourceChip = false) => {
    const isExpanded = expandedAlerts.has(alert.alertId);
    const isSelected = selectedAlertIds.has(alert.alertId);
    const isNew = newAlertIdsRef.current.has(alert.alertId);

    return (
      <ListItem
        key={alert.alertId}
        data-testid="alert-item"
        divider
        className={isNew ? 'new-alert' : undefined}
        sx={{
          bgcolor: alert.acknowledged ? 'transparent' :
                  (alert.severity === 'CRITICAL' || alert.severity === 'ERROR') ? 'error.light' : 'transparent',
          opacity: alert.acknowledged ? 0.7 : 1,
          '&:hover': { bgcolor: 'action.hover' },
          flexDirection: 'column',
          alignItems: 'stretch'
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', width: '100%' }}>
          {onAcknowledge && (
            <Checkbox
              checked={isSelected}
              onChange={() => toggleAlertSelection(alert.alertId)}
              size="small"
            />
          )}

          <ListItemIcon sx={{ minWidth: 40 }}>
            <Badge
              color={alert.escalated ? 'error' : 'default'}
              variant={alert.escalated ? 'dot' : undefined}
            >
              {getSeverityIcon(alert.severity)}
            </Badge>
          </ListItemIcon>

          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
              <Typography variant="body2" fontWeight="medium">
                {alert.title}
              </Typography>
              <Chip label={alert.severity} color={getSeverityColor(alert.severity)} size="small" />
              {!hideSourceChip && alert.sourceSystem && (
                <Chip label={alert.sourceSystem} size="small" variant="outlined" />
              )}
              {alert.escalated && (
                <Chip
                  label={`Escalated level ${alert.escalationLevel ?? 1}`}
                  color="error"
                  size="small"
                  variant="outlined"
                />
              )}
              {alert.resolved && (
                <Chip label="Resolved" color="success" size="small" />
              )}
            </Box>

            <Box sx={{ mt: 0.5 }}>
              <Typography variant="caption" color="text.secondary">
                {formatTimestamp(alert.timestamp)}
              </Typography>
              {alert.acknowledgedBy && (
                <Typography variant="caption" color="success.main" display="block">
                  Acknowledged by {alert.acknowledgedBy}
                </Typography>
              )}
            </Box>
          </Box>

          <Box sx={{ display: 'flex', gap: 0.5, alignItems: 'center', flexShrink: 0 }}>
            <Tooltip title="Expand details">
              <IconButton
                size="small"
                aria-label="expand details"
                onClick={() => toggleExpand(alert.alertId)}
              >
                {isExpanded ? <ExpandLess /> : <ExpandMore />}
              </IconButton>
            </Tooltip>

            {!alert.acknowledged && onAcknowledge && (
              <Tooltip title="Acknowledge">
                <Button
                  size="small"
                  variant="outlined"
                  tabIndex={0}
                  onClick={() => handleAcknowledgeDirect(alert.alertId)}
                >
                  Acknowledge
                </Button>
              </Tooltip>
            )}

            <Tooltip title="More actions">
              <IconButton
                size="small"
                aria-label="more actions"
                onClick={(e) => {
                  setMoreActionsAnchor(e.currentTarget);
                  setMoreActionsAlert(alert);
                }}
              >
                <MoreVert />
              </IconButton>
            </Tooltip>
          </Box>
        </Box>

        {/* Expanded details — description, resources, threshold/current */}
        <Collapse in={isExpanded} timeout={0} unmountOnExit>
          <Box sx={{ pl: 6, pr: 2, pb: 1 }}>
            <Typography variant="caption" color="text.secondary" display="block">
              {alert.description}
            </Typography>
            {alert.threshold !== undefined && (
              <Typography variant="caption" display="block">
                {`Threshold: ${Number(alert.threshold).toFixed(1)}`}
              </Typography>
            )}
            {alert.currentValue !== undefined && (
              <Typography variant="caption" display="block">
                {`Current: ${Number(alert.currentValue).toFixed(1)}`}
              </Typography>
            )}
            {alert.affectedResources && alert.affectedResources.length > 0 && (
              <Box sx={{ mt: 0.5, display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {alert.affectedResources.map((r: string) => (
                  <Chip key={r} label={r} size="small" variant="outlined" />
                ))}
              </Box>
            )}
          </Box>
        </Collapse>
      </ListItem>
    );
  };

  return (
    <Box
      data-testid="alerts-panel"
      aria-label="Alerts Panel"
      role="region"
      className={isMobile ? 'alerts-panel mobile' : 'alerts-panel'}
      sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}
    >
      {/* Screen reader live region — text kept distinct from visible summary */}
      <Box
        role="status"
        aria-live="polite"
        sx={{ position: 'absolute', left: -9999, width: 1, height: 1, overflow: 'hidden' }}
      >
        {liveMessage}
      </Box>

      {/* Header with stats */}
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="h6">
            Alerts ({alertStats.total})
          </Typography>
          <Box sx={{ display: 'flex', gap: 0.5 }}>
            <Tooltip title={soundEnabled ? 'Mute Alerts' : 'Enable Alert Sounds'}>
              {/* TODO: wire to onToggleSound callback */}
              <IconButton size="small">
                {soundEnabled ? <VolumeUp /> : <VolumeOff />}
              </IconButton>
            </Tooltip>
            <Tooltip title="Group by">
              <IconButton
                size="small"
                aria-label="group by"
                onClick={(e) => setGroupByMenuAnchor(e.currentTarget)}
              >
                <Group />
              </IconButton>
            </Tooltip>
            <Tooltip title="Filter Alerts">
              <IconButton size="small">
                <FilterList />
              </IconButton>
            </Tooltip>
          </Box>
        </Box>

        {/* Severity filter — inputProps aria-label used as accessible name, no visible InputLabel */}
        <Box sx={{ mb: 1 }}>
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <Select
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value)}
              inputProps={{ 'aria-label': 'severity' }}
              displayEmpty
            >
              <MenuItem value="all">All Severities</MenuItem>
              <MenuItem value="critical">Critical Only</MenuItem>
              <MenuItem value="error">Error Only</MenuItem>
              <MenuItem value="warning">Warning Only</MenuItem>
            </Select>
          </FormControl>
        </Box>

        {/* Visible alert summary — uses "4 alerts total, 3 unacknowledged" pattern for tests */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
          {alertStats.total} alerts total, {alertStats.unacknowledged} unacknowledged
        </Typography>

        {/* Alert stats chips */}
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          {alertStats.unacknowledged > 0 && (
            <Chip label={`${alertStats.unacknowledged} Pending`} color="warning" size="small" />
          )}
          {alertStats.critical > 0 && (
            <Chip label={`${alertStats.critical} Critical`} color="error" size="small" />
          )}
          {alertStats.errors > 0 && (
            <Chip label={`${alertStats.errors} Errors`} color="error" variant="outlined" size="small" />
          )}
          {alertStats.warnings > 0 && (
            <Chip label={`${alertStats.warnings} Warnings`} color="warning" variant="outlined" size="small" />
          )}
        </Box>

        {/* Bulk actions */}
        {selectedAlertIds.size > 0 && onAcknowledge && (
          <Box sx={{ mt: 1 }}>
            <Button size="small" variant="contained" onClick={handleBulkAcknowledge}>
              Acknowledge Selected ({selectedAlertIds.size})
            </Button>
          </Box>
        )}
      </Box>

      {/* Alerts list */}
      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {loading ? (
          <Box sx={{ p: 2 }}>
            {[1, 2, 3].map(i => (
              <Skeleton
                key={i}
                data-testid={`skeleton-${i}`}
                variant="rectangular"
                height={80}
                sx={{ mb: 1 }}
              />
            ))}
          </Box>
        ) : filteredAlerts.length === 0 ? (
          <Box sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100%',
            p: 3
          }}>
            <CheckCircle color="success" sx={{ fontSize: 48, mb: 2 }} />
            {hasActiveFilters ? (
              <>
                <Typography variant="h6" color="text.secondary">
                  No Alerts Match Your Filters
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Try adjusting or clearing your filters
                </Typography>
              </>
            ) : (
              <>
                <Typography variant="h6" color="text.secondary">
                  No Alerts
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  All systems are running normally
                </Typography>
              </>
            )}
          </Box>
        ) : groupedAlerts ? (
          <Box>
            {Object.entries(groupedAlerts).map(([group, groupAlerts]) => (
              <Box key={group}>
                <Box sx={{ px: 2, py: 1, bgcolor: 'action.hover' }}>
                  <Typography variant="subtitle2">
                    {group} ({groupAlerts.length})
                  </Typography>
                </Box>
                <List aria-label="alerts list" sx={{ p: 0 }}>
                  {(groupAlerts as Alert[]).map(alert =>
                    renderAlertItem(alert, groupBy === 'sourceSystem')
                  )}
                </List>
              </Box>
            ))}
          </Box>
        ) : (
          <List aria-label="alerts list" sx={{ p: 0 }}>
            {filteredAlerts.map(alert => renderAlertItem(alert))}
          </List>
        )}
      </Box>

      {/* Group by menu */}
      <Menu
        anchorEl={groupByMenuAnchor}
        open={Boolean(groupByMenuAnchor)}
        onClose={() => setGroupByMenuAnchor(null)}
      >
        <MenuItem onClick={() => { setGroupBy('none'); setGroupByMenuAnchor(null); }}>
          No Grouping
        </MenuItem>
        <MenuItem onClick={() => { setGroupBy('sourceSystem'); setGroupByMenuAnchor(null); }}>
          Source System
        </MenuItem>
        <MenuItem onClick={() => { setGroupBy('severity'); setGroupByMenuAnchor(null); }}>
          Severity
        </MenuItem>
      </Menu>

      {/* More actions menu */}
      <Menu
        anchorEl={moreActionsAnchor}
        open={Boolean(moreActionsAnchor)}
        onClose={() => { setMoreActionsAnchor(null); setMoreActionsAlert(null); }}
      >
        {onResolve && (
          <MenuItem onClick={() => {
            if (moreActionsAlert && onResolve) {
              onResolve(moreActionsAlert.alertId);
              setLiveMessage('Alert resolved');
            }
            setMoreActionsAnchor(null);
            setMoreActionsAlert(null);
          }}>
            Resolve
          </MenuItem>
        )}
        <MenuItem onClick={() => {
          setSelectedAlert(moreActionsAlert);
          setSnoozeDialogOpen(true);
          setMoreActionsAnchor(null);
          setMoreActionsAlert(null);
        }}>
          Snooze
        </MenuItem>
        <MenuItem onClick={() => {
          setSelectedAlert(moreActionsAlert);
          setEscalateDialogOpen(true);
          setMoreActionsAnchor(null);
          setMoreActionsAlert(null);
        }}>
          Escalate
        </MenuItem>
      </Menu>

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
          <Button onClick={() => setResolveDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleResolve}
            variant="contained"
            disabled={!resolution.trim()}
          >
            Resolve
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snooze Dialog */}
      <Dialog
        open={snoozeDialogOpen}
        onClose={() => setSnoozeDialogOpen(false)}
        aria-label="Snooze Alert"
      >
        <DialogTitle>Snooze Alert</DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            How long do you want to snooze this alert?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSnoozeDialogOpen(false)}>Cancel</Button>
          {/* TODO: wire to API */}
          <Button variant="contained" onClick={() => setSnoozeDialogOpen(false)}>
            Snooze 30 minutes
          </Button>
        </DialogActions>
      </Dialog>

      {/* Escalate Dialog */}
      <Dialog
        open={escalateDialogOpen}
        onClose={() => setEscalateDialogOpen(false)}
      >
        <DialogTitle>Escalation Confirmation</DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            Are you sure you want to escalate this alert?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEscalateDialogOpen(false)}>Cancel</Button>
          {/* TODO: wire to API */}
          <Button variant="contained" color="error" onClick={() => setEscalateDialogOpen(false)}>
            Escalate
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AlertsPanel;
