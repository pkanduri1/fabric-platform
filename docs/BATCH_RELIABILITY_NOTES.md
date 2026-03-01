# Batch Reliability Notes (Issues #6 and #7)

## Implemented behaviors

### Retry policy
- `batch.execution.retry.maxAttempts` (default `3`)
- `batch.execution.retry.backoffMs` (default `500`)
- Applied to master query fetch and output generation phases.

### Checkpoint/Restart
- `batch.execution.checkpoint.interval` controls checkpoint frequency.
- Checkpoint stores last successfully processed record index.
- On rerun with same execution ID, generation resumes from checkpoint index.

### Idempotent rerun guard
- Completion marker prevents duplicate full reprocessing for same execution ID.
- Completion marker stores output file path and returns completed result immediately.

### Runtime state path
- `batch.execution.state.dir` (default `/tmp/fabric-batch-state`)

## Issue #7 SQL*Loader hardening additions

### Pre-flight validation gate
- Pre-flight checks now run before loader execution for:
  - required config fields (target table, delimiter, header rows)
  - target table name sanity format
  - file-shape consistency (column count across sampled data rows)
- If pre-flight fails, the run is blocked with remediation hints.

### Standardized SQL*Loader execution states
- `COMPLETED`
- `COMPLETED_WITH_WARNINGS`
- `COMPLETED_WITH_REJECTIONS`
- `FAILED_REJECTED_ROWS`
- `FAILED_FATAL`
- `FAILED_RESOURCE`
- `FAILED_REJECT_THRESHOLD`
- `FAILED_UNKNOWN`

### Deterministic artifacts
- Loader artifacts are written under:
  - `sqlloader.artifact.root.dir` (default `${java.io.tmpdir}/fabric-sqlloader-artifacts`)
  - per-run subdirectory keyed by `jobName + correlationId`
- Standard artifacts include:
  - `.ctl`, `.log`, `.bad`, `.dsc`

### Reject-threshold policy
- `sqlloader.reject.policy`: `FAIL_FAST` or `TOLERATE`
- `sqlloader.reject.max.count`
- `sqlloader.reject.max.percent`
- In `FAIL_FAST`, run is forced to `FAILED_REJECT_THRESHOLD` when threshold is exceeded.
- In `TOLERATE`, run is marked `COMPLETED_WITH_REJECTIONS` with remediation warnings.

## Operational guidance
- Keep execution IDs stable for retries/restarts.
- Do not manually delete checkpoint files during active recovery unless you intend full restart.
- If state directory is wiped, restart falls back to full rerun.
- For partial-load use-cases, explicitly set reject policy and thresholds per source system risk tolerance.
