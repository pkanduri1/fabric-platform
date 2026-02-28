# Batch Reliability Notes (Issue #6)

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

## Operational guidance
- Keep execution IDs stable for retries/restarts.
- Do not manually delete checkpoint files during active recovery unless you intend full restart.
- If state directory is wiped, restart falls back to full rerun.
