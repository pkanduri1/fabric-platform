# SQL*Loader Runtime Hardening (Issue #7)

## Implemented

### 1) Pre-flight validation gate
In `DataLoadOrchestrator` before SQL*Loader execution:
- config checks (target table, delimiter, header rows)
- target table format sanity
- file-shape checks (column consistency on sampled rows)
- remediation hints included in result summary

If pre-flight fails, execution is stopped before loader run.

### 2) Deterministic artifact handling
In `SqlLoaderExecutor`:
- artifacts are grouped under configured root directory
- per-run deterministic folder key based on `jobName + correlationId`
- deterministic `.log/.bad/.dsc` file naming and metadata capture (`artifactRunDir`)

### 3) Reject-threshold policies
`SqlLoaderExecutor` supports configurable reject policy:
- `FAIL_FAST`: marks execution failed when thresholds exceeded
- `TOLERATE`: allows completion with warnings and explicit remediation hints

Threshold properties:
- `sqlloader.reject.max.count`
- `sqlloader.reject.max.percent`

### 4) State transition consistency
Execution now carries standardized status progression including:
- pre-flight pass/fail
- execution success/failure
- reject-threshold failure or tolerated completion states

## New/Relevant properties
- `sqlloader.artifact.root.dir`
- `sqlloader.reject.policy`
- `sqlloader.reject.max.count`
- `sqlloader.reject.max.percent`

## Validation
- Data-loader compile: `mvn -q -DskipTests compile`
- Hardening tests: `mvn -q -Dtest=SqlLoaderExecutorHardeningTest test`
