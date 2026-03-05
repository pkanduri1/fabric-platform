import { defineConfig, devices } from '@playwright/test';
import { config } from 'dotenv';

config({ path: '.env.playwright' });

export default defineConfig({
  testDir: './e2e/tests',
  globalSetup: './e2e/global-setup.ts',
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  timeout: 30_000,
  reporter: [['html', { open: 'never' }], ['line']],
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    storageState: 'e2e/.auth/user.json',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
