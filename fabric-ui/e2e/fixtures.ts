import { test as base, expect } from '@playwright/test';

// Re-export test with any custom fixtures here.
// Currently all tests use the default storageState from playwright.config.ts.
// This file is the extension point — add authedPage or api fixtures later.

export { expect };
export const test = base;
