# Playwright E2E Test Suite — Design Doc

**Date:** 2026-03-05
**Status:** Approved

## Goal

Add a Playwright E2E test suite covering the 15 most critical user journeys (P1 + P2) across authentication, configuration, monitoring, templates, and manual job execution. Tests run against the real backend (Spring Boot + Oracle, local profile).

## Decisions

| Decision | Choice | Reason |
|----------|--------|--------|
| Test target | Real backend (local profile) | Full stack validation |
| Auth strategy | `storageState` (login once, reuse) | Validates login once, no per-test overhead |
| Suite structure | Fixtures-only (no Page Object Model) | Lean, idiomatic Playwright for 15 tests |
| Browser | Chromium only | Consistent, fastest iteration |
| Credentials | `.env.playwright` (gitignored) | Any creds work with local Spring profile |

## Project Structure

```
fabric-ui/
├── e2e/
│   ├── fixtures.ts              # authedPage + api fixtures
│   ├── global-setup.ts          # login once, save storageState
│   ├── .auth/
│   │   └── user.json            # saved browser storage (gitignored)
│   └── tests/
│       ├── auth.spec.ts         # login, logout, invalid creds (3 tests)
│       ├── dashboard.spec.ts    # job tiles, navigation (2 tests)
│       ├── configuration.spec.ts # source system, field mapping, YAML (3 tests)
│       ├── monitoring.spec.ts   # grid, alerts, metrics, WebSocket (4 tests)
│       ├── templates.spec.ts    # studio browse, admin create (2 tests)
│       └── manual-job.spec.ts  # manual job config + submit (1 test)
├── playwright.config.ts
└── .env.playwright              # E2E_USERNAME, E2E_PASSWORD (gitignored)
```

## Auth Setup

### `global-setup.ts`
Runs once before all tests:
1. Launch browser, navigate to `/login`
2. Fill `E2E_USERNAME` / `E2E_PASSWORD` from `.env.playwright`
3. Submit — local Spring profile accepts any credentials, JWT stored in `localStorage`
4. Save storage state to `e2e/.auth/user.json`

### `fixtures.ts`
Exports extended `test` with:
- **`authedPage`** — `Page` pre-loaded with `.auth/user.json`. Fresh authenticated context per test, no re-login.
- **`api`** — `APIRequestContext` with `Authorization: Bearer {token}` for direct API calls (seed/verify without UI).

### `.env.playwright` (gitignored)
```
E2E_USERNAME=testuser
E2E_PASSWORD=anypassword
BASE_URL=http://localhost:3000
API_URL=http://localhost:8080
```

## Playwright Config

```typescript
// playwright.config.ts key settings
{
  globalSetup: './e2e/global-setup.ts',
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    storageState: 'e2e/.auth/user.json',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }]
}
```

## Test Scenarios (15 total)

### auth.spec.ts (3 tests)
1. **Login → Dashboard** — Submit credentials, assert `/dashboard` loads, nav visible
2. **Invalid credentials** — Submit bad creds, assert error message shown, stay on `/login`
3. **Logout** — Click logout, assert `localStorage` cleared, redirect to `/login`

> Note: Test 2 runs outside `storageState` (needs unauthenticated context). Uses `test.use({ storageState: undefined })`.

### dashboard.spec.ts (2 tests)
4. **Job status tiles render** — Cards visible with status badges (Running/Completed/Failed)
5. **Navigate to Monitoring** — Click nav link → `/monitoring` loads, grid visible

### configuration.spec.ts (3 tests)
6. **Source system dropdown** — Dropdown populates from API with HR/ENCORE/SHAW
7. **Field mapping panel** — Select system+job → 3 panels appear, drag field to mapping column
8. **YAML preview** — After mapping, click Preview → YAML content rendered on screen

### monitoring.spec.ts (4 tests)
9. **Job status grid** — Grid shows job rows with executionId, status, timestamps
10. **Alert acknowledgment** — Click Ack on alert → alert row updates state
11. **Metric chart tabs** — Click Throughput/Error Rate tabs → chart area re-renders
12. **Real-time WebSocket update** — Backend pushes job status update via WS → grid row changes without page refresh

> Test 12 timeout: 45s (WebSocket latency).

### templates.spec.ts (2 tests)
13. **Template studio browse** — Template list renders, search input filters results
14. **Admin — create template** — Fill name/type form, submit → new template appears in list

### manual-job.spec.ts (1 test)
15. **Manual job config + submit** — Fill config form fields, submit → success response shown

## data-testid Additions Required

Several pages lack `data-testid` attributes. Implementation plan will add minimal testids alongside tests:

| Page/Component | New testids needed |
|---|---|
| `LoginPage` | `login-form`, `username-input`, `password-input`, `login-submit`, `login-error` |
| `HomePage` (dashboard) | `job-status-card`, `status-badge` |
| `ConfigurationPage` | `source-system-select`, `field-panel`, `mapping-panel`, `yaml-preview-btn` |
| `TemplateStudioPage` | `template-list`, `template-search` |
| `TemplateAdminPage` | `create-template-btn`, `template-name-input`, `template-form-submit` |
| `ManualJobConfigurationPage` | `manual-job-form`, `manual-job-submit` |
| `MonitoringDashboard nav` | `nav-monitoring-link` |
| Logout button | `logout-btn` |

Existing testids (`alerts-panel`, `alert-item`, `ack-${id}`, `job-status-grid`, `job-row`, `performance-metrics-chart`) are used as-is.

## Success Criteria

- `npx playwright test` → 15/15 pass against running stack (frontend port 3000, backend port 8080)
- No test depends on another test's state (each is independently runnable)
- `screenshot` and `video` artifacts saved on failure
- CI-ready: `retries: 1`, headless by default
