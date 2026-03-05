import { test, expect } from '../fixtures';

// The HR source system has jobs: [] so the Configure button is disabled.
// We navigate directly to the configuration URL using the known system ID.
// NOTE: 'HR' must match a source system ID in the database.
// This system was seeded in test setup. If it disappears, tests 6 and 7 will show
// the "Select a source system" alert instead of the 3-panel layout.
const CONFIG_URL = '/configuration/HR/default-job';

test('6 - /configuration/:systemId/:jobName route renders', async ({ page }) => {
  await page.goto(CONFIG_URL);
  await expect(page).toHaveURL(/\/configuration\//, { timeout: 10_000 });
});

test('7 - configuration page shows 3-panel layout', async ({ page }) => {
  await page.goto(CONFIG_URL);
  await expect(page).toHaveURL(/\/configuration\//, { timeout: 10_000 });
  // Wait for loading spinner to clear before asserting panels
  await expect(page.locator('[role="progressbar"]')).toBeHidden({ timeout: 15_000 });
  // All 3 panels must be visible
  await expect(page.getByTestId('source-field-panel')).toBeVisible({ timeout: 10_000 });
  await expect(page.getByTestId('mapping-panel')).toBeVisible({ timeout: 10_000 });
  await expect(page.getByTestId('field-config-panel')).toBeVisible({ timeout: 10_000 });
});

test('8 - YAML preview page loads', async ({ page }) => {
  // Navigate directly to yaml-preview (View YAML button is disabled when jobs: [])
  await page.goto('/yaml-preview');
  await expect(page).toHaveURL(/\/yaml-preview/, { timeout: 10_000 });
  // YAML preview page should render its heading or content
  await expect(page.getByRole('heading', { name: /yaml preview/i }).or(
    page.getByText(/yaml|preview/i).first()
  )).toBeVisible({ timeout: 10_000 });
});
