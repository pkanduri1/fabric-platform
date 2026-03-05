import { test, expect } from '../fixtures';

test.beforeEach(async ({ page }) => {
  await page.goto('/dashboard');
});

test('4 - dashboard shows source system cards', async ({ page }) => {
  // Wait for API to load source systems
  await expect(page.getByTestId('source-system-card').first()).toBeVisible({ timeout: 10_000 });
});

test('5 - navigate to monitoring via sidebar', async ({ page }) => {
  // Note: Sidebar renders nav items twice — once in the inline permanent drawer (desktop)
  // and once in a Portal-based temporary drawer (mobile). React Portals render to
  // document.body, so the inline permanent drawer's button is first in DOM order and
  // is the one clicked here. The temporary drawer is CSS-hidden at desktop viewport.
  await page.getByTestId('nav-monitoring').first().click();
  await expect(page).toHaveURL(/\/monitoring/, { timeout: 10_000 });
  await expect(page.getByTestId('job-status-grid')).toBeVisible({ timeout: 15_000 });
});
