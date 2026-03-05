import { chromium, FullConfig } from '@playwright/test';
import { config } from 'dotenv';
import path from 'path';

config({ path: path.resolve(__dirname, '../.env.playwright') });

async function globalSetup(_config: FullConfig) {
  const baseURL = process.env.BASE_URL ?? 'http://localhost:3000';
  const username = process.env.E2E_USERNAME ?? 'testuser';
  const password = process.env.E2E_PASSWORD ?? 'testpass1234';

  const browser = await chromium.launch();
  const page = await browser.newPage();

  await page.goto(`${baseURL}/login`);

  // Fill login form (testids added in Task 3)
  await page.getByTestId('username-input').fill(username);
  await page.getByTestId('password-input').fill(password);
  await page.getByTestId('login-submit').click();

  // Wait for redirect to dashboard
  await page.waitForURL(`${baseURL}/dashboard`, { timeout: 15_000 });

  // Save auth state
  await page.context().storageState({
    path: path.resolve(__dirname, '.auth/user.json'),
  });

  await browser.close();
  console.log('✅ Global setup: auth state saved to e2e/.auth/user.json');
}

export default globalSetup;
