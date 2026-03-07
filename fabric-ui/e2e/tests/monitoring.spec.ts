import { test, expect } from '../fixtures';

test.beforeEach(async ({ page }) => {
  await page.goto('/monitoring');
  await page.waitForLoadState('networkidle', { timeout: 15_000 }).catch(() => {});
});

test('9 - job status grid renders', async ({ page }) => {
  await expect(page.getByTestId('job-status-grid')).toBeVisible({ timeout: 15_000 });
});

test('10 - alert acknowledgment updates alert state', async ({ page }) => {
  const alertsPanel = page.getByTestId('alerts-panel');
  await expect(alertsPanel).toBeVisible({ timeout: 15_000 });

  // Only proceed if there are alerts to acknowledge
  const firstAckBtn = alertsPanel.getByRole('button', { name: /acknowledge/i }).first();
  const hasAlerts = await firstAckBtn.isVisible().catch(() => false);

  if (!hasAlerts) {
    test.skip(true, 'No unacknowledged alerts present — skipping ack assertion');
    return;
  }

  await firstAckBtn.click();
  // Assert the button disappears (alert transitioned to acknowledged state)
  await expect(firstAckBtn).not.toBeVisible({ timeout: 5_000 });
});

test('11 - metric chart tabs switch content', async ({ page }) => {
  const chart = page.getByTestId('performance-metrics-chart');
  await expect(chart).toBeVisible({ timeout: 15_000 });

  // Click Error Rate tab
  const errorRateTab = chart.getByRole('tab', { name: /error rate/i });
  await errorRateTab.click();
  await expect(errorRateTab).toHaveAttribute('aria-selected', 'true');

  // Click Throughput tab — also verifies Error Rate is deselected
  const throughputTab = chart.getByRole('tab', { name: /throughput/i });
  await throughputTab.click();
  await expect(throughputTab).toHaveAttribute('aria-selected', 'true');
  await expect(errorRateTab).toHaveAttribute('aria-selected', 'false');
});

test('12 - WebSocket real-time indicator shows connection', async ({ page }) => {
  const indicator = page.getByTestId('real-time-indicator');
  await expect(indicator).toBeVisible({ timeout: 15_000 });
  await expect(indicator).toHaveText('LIVE');
});

// #49 — Export button calls API (not a stub)
test('19 - Export button triggers API call to export endpoint', async ({ page }) => {
  const exportRequest = page.waitForRequest(
    (req) => req.url().includes('/monitoring/export') && req.method() === 'GET',
    { timeout: 10_000 }
  );

  // Open the More Options menu and click Export Data
  await page.getByTestId('monitoring-menu-btn').click();
  await page.getByTestId('monitoring-export-btn').click();

  // Verify the export API was called (may 404 in dev — what matters is it was called)
  const req = await exportRequest;
  expect(req.url()).toContain('/monitoring/export');
});
