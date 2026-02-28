# ARCH Review 2026 Q1 — Batch Enhancements Pre-Start

## Status
- **Phase:** In Progress
- **Driver Issue:** https://github.com/pkanduri1/fabric-platform/issues/3
- **Related:** #4, #5

## Review Goals
1. Ensure architecture supports **extreme configurability** without runtime fragility.
2. Tighten module boundaries and prevent drift.
3. Define enforceable architecture rules and exception path.

## Initial Focus Areas
- Module ownership boundaries (`fabric-api`, `fabric-batch`, `fabric-data-loader`, `fabric-utils`)
- Configuration model versioning + compatibility
- Runtime reliability contracts (retry/checkpoint/idempotency)
- Operational observability requirements

## Findings (initial)
1. **Architecture doc drift vs implementation**
   - Architecture states JPA/Hibernate as primary DAL for backend.
   - `fabric-api` explicitly uses `spring-boot-starter-jdbc` and documents "without JPA/Hibernate" in `pom.xml`.
   - `fabric-data-loader` still includes JPA + JDBC, creating mixed access patterns.

2. **Module boundary inconsistency**
   - `fabric-api/pom.xml` comments out direct `fabric-data-loader` dependency due to interface instantiation issue.
   - `fabric-api` depends on `fabric-batch`, which excludes `fabric-data-loader` transitively.
   - Indicates unresolved layering/dependency boundary and potential architectural leakage.

3. **Schema boundary not yet aligned with target architecture narrative**
   - Current resources heavily use `CM3INT` as default schema.
   - Target narrative references dedicated config/audit/staging schema separation; enforcement appears incomplete.

4. **Configurability objective is strong but not yet contract-enforced**
   - Multiple docs promote configuration-driven behavior.
   - Missing single enforced versioned configuration contract gate before runtime execution.

## Decisions (proposed, pending approval)
- Standardize data access policy per module (avoid mixed JPA/JDBC in same logical path unless justified by ADR).
- Enforce strict dependency direction: `api -> service contracts`, `batch -> runtime orchestration`, `loader -> ingestion`, `utils -> shared primitives only`.
- Define schema strategy explicitly for environments (single-schema dev allowed, separated schema in higher envs with policy checks).
- Introduce required config contract version validation before execution starts.

## Recommended Changes (P0/P1)
### P0 (before Sprint 1 implementation)
- Finalize and publish architecture rulebook (`#4`).
- Define and implement versioned config contract + validation gate (`#5`).
- Resolve api/batch/loader dependency ambiguity (documented ownership and interface contracts).

### P1 (during Sprint 1/2)
- Unify data access approach per module and document exceptions via ADR.
- Add architecture compliance checks to PR template + CI guardrails.
- Add schema-separation readiness checks (especially audit/staging concerns).

## Prioritized Remediation List

| Priority | Item | Owner Role | Tracking |
|---|---|---|---|
| P0 | Publish architecture rulebook + PR enforcement checks | Architect + Backend | #4 |
| P0 | Define and enforce versioned configuration contract gate | Architect + Backend + QA | #5 |
| P0 | Resolve api/batch/loader boundary contract ambiguity | Architect + Backend | #4 |
| P1 | Unify data access policy per module (JDBC/JPA policy + ADRs) | Architect + Backend | #4 |
| P1 | Add schema separation readiness policy checks | Architect + Ops | #5 |
| P2 | Add automated architecture compliance checks in CI | QA + Backend | #4 |

## Go/No-Go Recommendation
- **Conditional Go** for Sprint 1 only if P0 decisions are finalized and tracked as blocking gates in #4/#5.
- Otherwise **No-Go** for deep implementation changes.

## Review Outcome
- Architecture gate review completed with conditional approval.
