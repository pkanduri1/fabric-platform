import { test, expect } from '../fixtures';

// These 2 tests need an UNAUTHENTICATED context (override storageState)
test.describe('Auth — unauthenticated', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('1 - login redirects to dashboard', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('username-input').fill('testuser');
    await page.getByTestId('password-input').fill('testpass1234');
    await page.getByTestId('login-submit').click();
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 10_000 });
    await expect(page.getByText('Batch Configuration Dashboard')).toBeVisible();
  });

  test('2 - client-side validation shows errors', async ({ page }) => {
    await page.goto('/login');
    // Submit with username < 3 chars and password < 8 chars to trigger validation
    await page.getByTestId('username-input').fill('ab');
    await page.getByTestId('password-input').fill('short');
    await page.getByTestId('login-submit').click();
    // MUI TextField helperText shows validation errors
    await expect(page.getByText('Username must be at least 3 characters')).toBeVisible();
    await expect(page.getByText('Password must be at least 8 characters')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });
});

// Tests 13-15: unauthenticated access to protected routes must redirect to /login.
// ProtectedLayout in AppRouter.tsx calls <Navigate to="/login" replace /> when
// isAuthenticated is false, so these routes should never render their content.
//
// NOTE: storageState: undefined does NOT clear the global storageState in Playwright
// because undefined is treated as "inherit". Using { cookies: [], origins: [] }
// explicitly provides a fresh empty browser context with no localStorage tokens.
test.describe('Auth — unauthenticated route protection', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('13 - /dashboard redirects unauthenticated users to /login', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/, { timeout: 10_000 });
  });

  test('14 - /monitoring redirects unauthenticated users to /login', async ({ page }) => {
    await page.goto('/monitoring');
    await expect(page).toHaveURL(/\/login/, { timeout: 10_000 });
  });

  test('15 - /configuration redirects unauthenticated users to /login', async ({ page }) => {
    await page.goto('/configuration');
    await expect(page).toHaveURL(/\/login/, { timeout: 10_000 });
  });
});

// Test 3 uses the authenticated storageState from global-setup
test('3 - logout clears session and redirects to login', async ({ page }) => {
  await page.goto('/dashboard');
  await page.getByTestId('logout-btn').click();
  await expect(page).toHaveURL(/\/login/, { timeout: 10_000 });
  // localStorage token must be cleared
  const token = await page.evaluate(() =>
    localStorage.getItem('fabric_access_token')
  );
  expect(token).toBeNull();
});
