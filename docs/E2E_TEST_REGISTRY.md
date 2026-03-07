# Fabric Platform - E2E Test Registry

**Framework:** Playwright (Chromium)
**Location:** `fabric-ui/e2e/tests/`
**Total Tests:** 29 across 8 spec files

## Running Tests

```bash
cd fabric-ui

# Prerequisites: backend (port 8080) and frontend (port 3000) must be running

# Run all tests
npx playwright test

# Run specific spec
npx playwright test e2e/tests/monitoring.spec.ts

# Run headed (visible browser)
npx playwright test --headed

# View HTML report
npx playwright show-report
```

## Test Index

### auth.spec.ts (6 tests)

| # | Test | Category |
|---|------|----------|
| 1 | Login redirects to dashboard | Auth - happy path |
| 2 | Client-side validation shows errors | Auth - validation |
| 3 | Logout clears session and redirects to login | Auth - logout |
| 13 | /dashboard redirects unauthenticated users to /login | Auth - route protection |
| 14 | /monitoring redirects unauthenticated users to /login | Auth - route protection |
| 15 | /configuration redirects unauthenticated users to /login | Auth - route protection |

### dashboard.spec.ts (5 tests)

| # | Test | Category |
|---|------|----------|
| 4 | Dashboard shows source system cards | Dashboard - rendering |
| 5 | Navigate to monitoring via sidebar | Navigation |
| 16 | System Settings button navigates to /configuration | Quick Actions |
| 17 | Run All Jobs button opens source system picker dialog | Quick Actions |
| 18 | Export Configuration button navigates to /configuration | Quick Actions |
| 23 | Configure button enabled for systems with jobs (#28 regression) | Regression |

### configuration.spec.ts (3 tests)

| # | Test | Category |
|---|------|----------|
| 6 | /configuration/:systemId/:jobName route renders | Routing |
| 7 | Configuration page shows 3-panel layout | UI layout |
| 8 | YAML preview page loads | Navigation |

### monitoring.spec.ts (6 tests)

| # | Test | Category |
|---|------|----------|
| 9 | Job status grid renders | Monitoring - grid |
| 10 | Alert acknowledgment updates alert state | Monitoring - alerts |
| 11 | Metric chart tabs switch content | Monitoring - charts |
| 12 | WebSocket real-time indicator shows connection | Monitoring - live indicator |
| 19 | Export button triggers API call to export endpoint | Monitoring - export |
| 56 | Monitoring dashboard API returns valid response | Monitoring - API integration |

### templates.spec.ts (2 tests)

| # | Test | Category |
|---|------|----------|
| 13 | Template studio renders field list and search | Templates - rendering |
| 14 | Admin create file type appears in dropdown | Templates - admin |

### sidebar.spec.ts (3 tests)

| # | Test | Category |
|---|------|----------|
| 20 | Sidebar source system click keeps dashboard cards visible (#30 regression) | Regression |
| 21 | Sidebar source system expands to show jobs on click | Sidebar |
| 22 | Sidebar second click on source system collapses it | Sidebar |

### no-infinite-refresh.spec.ts (2 tests)

| # | Test | Category |
|---|------|----------|
| 24 | Page does not infinite-loop on source-systems API | Performance |
| 25 | Page renders without continuous re-mounting | Performance |

### manual-job.spec.ts (1 test)

| # | Test | Category |
|---|------|----------|
| 15 | Manual job config form submit | Job config |

## Regression Tests

Tests added specifically for bug fixes:

| Test # | Issue | Description |
|--------|-------|-------------|
| 23 | #28 | Configure button disabled check uses jobCount |
| 20 | #30 | Sidebar source system click doesn't blank dashboard |
| 13-15 | Auth | Route protection redirects unauthenticated users |

## Test Data Dependencies

- **Oracle Database** must be running on port 1522 with the CM3INT schema
- **Source systems** `hr` and `shaw` must exist in `SOURCE_SYSTEMS` table
- **Test user** `testuser` / `testpass1234` must be configured
- Auth state is cached in `e2e/.auth/user.json` by `global-setup.ts`

---

Last Updated: 2026-03-07
