# Fix All Failing Tests — Design Doc

**Date:** 2026-03-04
**Status:** Approved

## Goal

Bring both test suites to 100% passing. No pre-existing failures are caused by recent code changes — all are infrastructure/environment mismatches that need targeted fixes.

## Current State

| Suite | Total | Passing | Failing |
|-------|-------|---------|---------|
| Backend (JUnit/Maven) | 22 | 0 | 22 |
| Frontend (Jest/React) | 221 | 83 | 138 |

## Root Causes

### Backend

**Issue 1 — ByteBuddy Java 25 incompatibility (16 tests)**
- `ConstantTransformationTest` (5 tests), `ValidSourceSystemValidatorTest` (11 tests)
- pom.xml declares Java 17; environment runs Java 25 (Homebrew)
- ByteBuddy 1.15.11 only supports class-file version 68 (Java 24); Java 25 = version 69
- Mockito inline mock-maker requires Java agent attachment for annotation mocking

**Issue 2 — Oracle URL hardcoded in integration test (6 tests)**
- `SourceSystemValidationIntegrationTest` tries `localhost:1521/ORCLPDB1` (old URL)
- Actual Oracle: `localhost:1522/FREEPDB1`

### Frontend

**Issue 3 — WebSocket mock infrastructure broken (cascading 6+ suites)**
- `global.WebSocket` mock not assigned before tests access `mock.instances`
- Affects: `WebSocketIntegration.test.ts`, `WebSocketService.test.ts`, `useWebSocket.test.ts`, `AlertsPanel.test.tsx`, `MonitoringDashboard.test.tsx`

**Issue 4 — Async cleanup / worker exit leaks (multiple suites)**
- Timers and open handles not cleaned up in `afterEach`/`afterAll`
- `PerformanceMetricsChart.test.tsx`, `JobStatusGrid.test.tsx`, `configApi.test.ts`

## Approach

### Backend fixes (fabric-core/pom.xml + test files)

1. **Override ByteBuddy** to `1.15.14+` in parent `pom.xml` `<properties>` — this is the first release with Java 25 class-file-version-69 support
2. **Configure maven-surefire-plugin** in parent `pom.xml`:
   - Attach `mockito-core` as `-javaagent` (required for mocking annotation interfaces like `ValidSourceSystem`)
   - Add `--add-opens` flags for Java module access
3. **Fix Oracle URL** in `SourceSystemValidationIntegrationTest.java` — change test properties or add `@TestPropertySource` with `localhost:1522/FREEPDB1`

### Frontend fixes (fabric-ui/src/)

1. **Fix WebSocket mock** — ensure `global.WebSocket` is set to a proper Jest mock constructor before any test suite accesses `mock.instances`. Fix setup order in `setupTests.ts` or the relevant mock file.
2. **Fix async cleanup** — add `jest.useFakeTimers()` / `jest.useRealTimers()` guards and `afterEach` cleanup for pending promises/timers in each failing suite.
3. **Fix individual test assertions** — once infrastructure is fixed, address any remaining per-test failures (wrong mock return values, missing stub setup, etc.)

## Success Criteria

- `mvn test -pl fabric-api` → 22/22 tests pass
- `CI=true npm test -- --watchAll=false` → 221/221 tests pass, no worker leak warnings
