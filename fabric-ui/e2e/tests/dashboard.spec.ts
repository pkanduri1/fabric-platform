import { test, expect } from '../fixtures';

test.beforeEach(async ({ page }) => {
  await page.goto('/dashboard');
});

test('4 - dashboard shows source system cards', async ({ page }) => {
  // Wait for API to load source systems
  await expect(page.getByTestId('source-system-card').first()).toBeVisible({ timeout: 10_000 });
  // At least one card should be present
  const cards = page.getByTestId('source-system-card');
  expect(await cards.count()).toBeGreaterThan(0);
});

test('5 - navigate to monitoring via sidebar', async ({ page }) => {
  // Sidebar renders both mobile and desktop drawers; use first() to target desktop drawer
  await page.getByTestId('nav-monitoring').first().click();
  await expect(page).toHaveURL(/\/monitoring/, { timeout: 10_000 });
  await expect(page.getByTestId('job-status-grid')).toBeVisible({ timeout: 15_000 });
});
