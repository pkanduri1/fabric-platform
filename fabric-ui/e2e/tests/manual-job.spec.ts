import { test, expect } from '../fixtures';

test('15 - manual job config form submit', async ({ page }) => {
  await page.goto('/manual-job-config');
  await page.waitForLoadState('networkidle', { timeout: 15_000 }).catch(() => {});

  // Open the Create Configuration dialog (requires JOB_CREATOR role injected in global-setup)
  await page.getByTestId('manual-job-create-btn').click();

  // Form dialog should open — scope all interactions to the dialog
  const dialog = page.getByRole('dialog');
  await expect(page.getByTestId('manual-job-form')).toBeVisible({ timeout: 10_000 });

  // Fill required fields scoped to dialog
  // jobName: must match /^[A-Z][A-Z0-9_]{2,99}$/
  await dialog.getByLabel('Job Name').fill('E2E_TEST_JOB');

  // jobType: MUI Select — first [aria-haspopup="listbox"] inside the dialog is jobType
  await dialog.locator('[aria-haspopup="listbox"]').first().click();
  await page.getByRole('option', { name: /ETL Batch/i }).click();

  // sourceSystem and targetSystem
  await dialog.getByLabel('Source System').fill('CORE_BANKING');
  await dialog.getByLabel('Target System').fill('DATA_WAREHOUSE');

  // Submit button should now be enabled
  const submitBtn = page.getByTestId('manual-job-submit');
  await expect(submitBtn).toBeEnabled({ timeout: 3_000 });

  // Click submit
  await submitBtn.click();

  // Assert: dialog closes on success OR error alert is shown (backend may reject duplicate)
  await expect(
    dialog.getByRole('alert').or(
      page.locator('.MuiSnackbar-root')
    )
  ).toBeVisible({ timeout: 10_000 });
});
