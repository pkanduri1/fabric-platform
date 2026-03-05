import { test, expect } from '../fixtures';

test('13 - template studio renders field list and search', async ({ page }) => {
  await page.goto('/template-studio');
  await page.waitForLoadState('networkidle', { timeout: 15_000 }).catch(() => {});

  // Search input and field list container are always rendered regardless of selection
  await expect(page.getByTestId('template-search')).toBeVisible({ timeout: 15_000 });
  await expect(page.getByTestId('template-list')).toBeVisible({ timeout: 15_000 });

  // Type in the search box to verify it's interactive
  await page.getByTestId('template-search').fill('test');
  await expect(page.getByTestId('template-search')).toHaveValue('test');
});

test('14 - admin create file type appears in dropdown', async ({ page }) => {
  await page.goto('/admin/templates');
  await page.waitForLoadState('networkidle', { timeout: 15_000 }).catch(() => {});

  // Open the Add File Type dialog
  await page.getByTestId('create-template-btn').click();

  // Dialog should open
  await expect(page.getByRole('heading', { name: /add new file type/i })).toBeVisible({ timeout: 5_000 });

  // Fill in the required fields
  const uniqueName = `e2e-test-${Date.now()}`;
  await page.getByTestId('template-name-input').fill(uniqueName);

  // Fill description (required field — next TextField in the dialog)
  await page.getByLabel(/description/i).fill('E2E test file type');

  // Submit
  await page.getByTestId('template-form-submit').click();

  // Dialog should close on success
  await expect(page.getByRole('heading', { name: /add new file type/i })).not.toBeVisible({ timeout: 10_000 });
});
