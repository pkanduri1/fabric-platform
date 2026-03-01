# Configuration Contract Policy

## Purpose
Define versioning and compatibility rules for runnable batch configuration contracts.

## Versioning model
- Contract version format: `MAJOR.MINOR`
- Current version: `1.1`
- Minimum compatible version: `1.0`
- Compatibility window: **N-1** (current + immediate previous minor)

## Required fields
- `contractVersion` is required for persisted UI configuration payloads.
- Runtime job parameters must include `configContractVersion` (or `contractVersion`) unless legacy override flag is enabled.

## Runtime validation gate
Validation happens before execution:
1. Parse job parameters JSON
2. Read contract version key (`configContractVersion` preferred)
3. Ensure version is within supported range
4. Reject execution on missing/unsupported version (unless `batch.configContract.allowLegacyWithoutVersion=true`)

## Compatibility strategy
- Backward compatibility for N-1 only.
- Breaking changes require major version increment and migration plan.
- Contract changes must include compatibility tests.

## Migration notes
- Legacy jobs without version can be temporarily allowed by feature flag:
  - `batch.configContract.allowLegacyWithoutVersion=true`
- Flag must be time-bound and tracked by ADR exception with cleanup owner.
