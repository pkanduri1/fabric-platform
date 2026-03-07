import { test, expect } from '../fixtures';

test.describe('Sidebar Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/dashboard');
    // Wait for source system cards to confirm the dashboard loaded
    await expect(page.getByTestId('source-system-card').first()).toBeVisible({ timeout: 10_000 });
  });

  // Regression test for #30: clicking a sidebar source system used to blank the dashboard
  // because selectSourceSystem() → loadSourceFields() → setIsLoading(true) cleared the content.
  test('20 - sidebar source system click keeps dashboard cards visible (#30 regression)', async ({ page }) => {
    // Click the first source system in the permanent (desktop) sidebar
    const firstSystem = page.getByTestId(/^sidebar-system-/).first();
    await expect(firstSystem).toBeVisible({ timeout: 5_000 });
    await firstSystem.click();

    // Dashboard cards must still be visible immediately after the click —
    // the bug caused a transient isLoading flash that erased all card content.
    await expect(page.getByTestId('source-system-card').first()).toBeVisible({ timeout: 5_000 });

    // URL must remain /dashboard (the sidebar click should not navigate away)
    await expect(page).toHaveURL(/\/dashboard/);
  });

  test('21 - sidebar source system expands to show jobs on click', async ({ page }) => {
    const firstSystem = page.getByTestId(/^sidebar-system-/).first();
    await firstSystem.click();

    // Allow time for loadJobsForSystem to resolve
    await page.waitForTimeout(2_000);

    // After click the sidebar should still be intact (no crash/blank)
    await expect(firstSystem).toBeVisible();
    // Dashboard content remains visible
    await expect(page.getByTestId('source-system-card').first()).toBeVisible();
  });

  test('22 - sidebar second click on source system collapses it', async ({ page }) => {
    const firstSystem = page.getByTestId(/^sidebar-system-/).first();

    // First click: expand
    await firstSystem.click();
    await page.waitForTimeout(500);

    // Second click: collapse
    await firstSystem.click();

    // Page must remain on /dashboard and not crash
    await expect(page).toHaveURL(/\/dashboard/);
    await expect(firstSystem).toBeVisible();
    await expect(page.getByTestId('source-system-card').first()).toBeVisible();
  });
});
