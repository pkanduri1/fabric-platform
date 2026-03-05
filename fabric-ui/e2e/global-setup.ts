import { chromium, FullConfig } from '@playwright/test';
import { config } from 'dotenv';
import fs from 'fs';
import path from 'path';

config({ path: path.resolve(__dirname, '../.env.playwright') });

async function globalSetup(_config: FullConfig) {
  const baseURL = process.env.BASE_URL ?? 'http://localhost:3000';
  const username = process.env.E2E_USERNAME ?? 'testuser';
  const password = process.env.E2E_PASSWORD ?? 'testpass1234';

  const browser = await chromium.launch();
  try {
    const page = await browser.newPage();

    await page.goto(`${baseURL}/login`);

    // Fill login form (testids added in Task 3)
    await page.getByTestId('username-input').fill(username);
    await page.getByTestId('password-input').fill(password);
    await page.getByTestId('login-submit').click();

    // Wait for redirect to dashboard
    await page.waitForURL(`${baseURL}/dashboard`, { timeout: 15_000 });

    // Patch fabric_user in localStorage to include MONITORING_USER role so
    // tests that navigate to /monitoring are not blocked by the role check.
    await page.evaluate(() => {
      const raw = localStorage.getItem('fabric_user');
      if (raw) {
        const user = JSON.parse(raw);
        if (!user.roles.includes('MONITORING_USER')) {
          user.roles.push('MONITORING_USER');
        }
        localStorage.setItem('fabric_user', JSON.stringify(user));
      }
    });

    // Ensure .auth directory exists before writing
    const authDir = path.resolve(__dirname, '.auth');
    fs.mkdirSync(authDir, { recursive: true });

    // Save auth state
    await page.context().storageState({
      path: path.resolve(__dirname, '.auth/user.json'),
    });

    console.log('✅ Global setup: auth state saved to e2e/.auth/user.json');
  } finally {
    await browser.close();
  }
}

export default globalSetup;
