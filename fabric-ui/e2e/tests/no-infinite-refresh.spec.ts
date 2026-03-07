import { test, expect } from '@playwright/test';

// Skip global setup auth — test the page directly
test.use({ storageState: { cookies: [], origins: [] } });

// Regression test: the UI was stuck in an infinite refresh loop where
// GET /api/config/source-systems was called repeatedly because
// useSourceSystems created a new array reference on every API response,
// triggering cascading re-renders through ConfigurationContext.
test('24 - page does not infinite-loop on source-systems API', async ({ page }) => {
  // Intercept source-systems API calls and count them
  let apiCallCount = 0;
  await page.route('**/api/config/source-systems', (route) => {
    apiCallCount++;
    route.continue();
  });

  await page.goto('/');

  // Wait for the page to settle (give it enough time for any loop to manifest)
  await page.waitForTimeout(6_000);

  // Before the fix, apiCallCount would be 50+ within 5 seconds.
  // After the fix, it should be 1-3 at most (initial load + possible cache miss retry).
  console.log(`source-systems API called ${apiCallCount} times in 6 seconds`);
  expect(apiCallCount).toBeLessThanOrEqual(5);
});

test('25 - page renders without continuous re-mounting', async ({ page }) => {
  await page.goto('/');

  // Inject a mutation observer to count large DOM changes (re-mount indicator)
  const mutationCount = await page.evaluate(async () => {
    let count = 0;
    const observer = new MutationObserver((mutations) => {
      for (const m of mutations) {
        if (m.type === 'childList' && m.addedNodes.length > 3) {
          count++;
        }
      }
    });
    observer.observe(document.body, { childList: true, subtree: true });

    await new Promise((r) => setTimeout(r, 5_000));
    observer.disconnect();
    return count;
  });

  console.log(`Significant DOM mutations in 5 seconds: ${mutationCount}`);
  // In an infinite loop, this would be very high (100+).
  expect(mutationCount).toBeLessThan(30);
});
