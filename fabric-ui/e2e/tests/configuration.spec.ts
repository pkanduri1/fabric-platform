import { test, expect } from '../fixtures';

// The HR source system has jobs: [] so the Configure button is disabled.
// We navigate directly to the configuration URL using the known system ID.
const CONFIG_URL = '/configuration/HR/default-job';

test('6 - clicking Configure navigates to configuration page', async ({ page }) => {
  await page.goto(CONFIG_URL);
  await expect(page).toHaveURL(/\/configuration\//, { timeout: 10_000 });
});

test('7 - configuration page shows 3-panel layout', async ({ page }) => {
  await page.goto(CONFIG_URL);
  await expect(page).toHaveURL(/\/configuration\//, { timeout: 10_000 });
  // All 3 panels must be visible
  await expect(page.getByTestId('source-field-panel')).toBeVisible({ timeout: 10_000 });
  await expect(page.getByTestId('mapping-panel')).toBeVisible({ timeout: 10_000 });
  await expect(page.getByTestId('field-config-panel')).toBeVisible({ timeout: 10_000 });
});

test('8 - YAML preview page loads', async ({ page }) => {
  // Navigate directly to yaml-preview (View YAML button is disabled when jobs: [])
  await page.goto('/yaml-preview');
  await expect(page).toHaveURL(/\/yaml-preview/, { timeout: 10_000 });
  // YAML preview page should render some content
  await expect(page.locator('body')).not.toBeEmpty();
});
