# FABRIC Architecture Rulebook (Config-First)

## Purpose
Enforce a stable, highly configurable architecture for FABRIC batch processing.

## Mandatory Rules

1. **Configuration-first behavior**
   - New business behavior must be driven by configuration.
   - Hardcoded business rules are forbidden unless approved by ADR.

2. **Module boundaries are strict**
   - `fabric-api`: request handling, authz/authn, orchestration entrypoints.
   - `fabric-batch`: runtime processing orchestration and execution policies.
   - `fabric-data-loader`: ingestion, SQL*Loader lifecycle, file-to-staging concerns.
   - `fabric-utils`: shared primitives only (DTO/value types/common utils).

3. **Dependency direction**
   - Allowed: `api -> batch`, `api -> utils`, `batch -> loader`, `batch -> utils`, `loader -> utils`.
   - Forbidden: `utils -> *`, `loader -> api`, `batch -> api`, UI coupling inside batch/loader.

4. **Versioned configuration contract required**
   - Every runnable configuration must include contract version.
   - Runtime must validate contract compatibility before execution.

5. **Schema policy**
   - Dev may use single schema for simplicity.
   - Higher environments must support separated responsibilities (config/audit/staging) via explicit policy and checks.

6. **Operational observability baseline**
   - Correlation IDs must flow across loader->batch->audit.
   - Required metrics: throughput, retry count, reject rate, stage timings.

7. **Failure semantics must be deterministic**
   - Retry behavior is bounded and configurable.
   - Terminal states must be explicit and documented.

## Exception Process
- Any exception requires ADR with:
  - reason and scope
  - risk and rollback plan
  - duration (temporary/permanent)
  - owner approval

## Enforcement in PRs
- PR must include architecture checklist completion.
- Boundary violations require explicit ADR link.
- Config contract changes require compatibility notes + tests.
- Any new dependency edge between core modules must be listed in PR and validated against this rulebook.
- If a PR introduces a temporary exception, it must include expiry/removal criteria.

## Gate Conditions (must pass)
1. Architecture checklist fully completed.
2. No forbidden module dependency direction introduced.
3. ADR linked for every exception.
4. Config contract compatibility impact documented.

## Change Control
- Rule changes require architecture review and update to this document.
- Every accepted rule change must be recorded by ADR and referenced from the next release notes.
