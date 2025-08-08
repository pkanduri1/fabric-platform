# 📁 Fabric Platform - Test Data Directory

This directory contains test data files and directories used for end-to-end testing of the Fabric Platform.

## Directory Structure

```
data/
├── input/          # Input data files for processing
│   ├── TCB_SIMPLE_TRANSACTIONS_20250808.dat      # Simple transaction test data (10 records)
│   └── TCB_COMPLEX_TRANSACTIONS_20250808.dat     # Complex transaction test data (10 records)
├── output/         # Processed output files
├── archive/        # Successfully processed files moved here
├── error/          # Files with processing errors
└── logs/           # SQL*Loader and processing logs
```

## Test Data Files

### Simple Transactions (TCB_SIMPLE_TRANSACTIONS_20250808.dat)
- **Records:** 10 transactions
- **Format:** Pipe-delimited
- **Transaction Types:** DEBIT, CREDIT, TRANSFER
- **Accounts:** 5 different account numbers
- **Amount Range:** $45.75 - $2,000.00

### Complex Transactions (TCB_COMPLEX_TRANSACTIONS_20250808.dat)
- **Records:** 10 transactions with dependencies
- **Format:** Pipe-delimited with dependency information
- **Workflows:**
  - **Wire Transfer:** 4-step sequential process (INIT → VALIDATE → EXECUTE → CONFIRM)
  - **Loan Processing:** 2-step parallel process (INIT + VALIDATE)
  - **Securities Trade:** 4-step sequential process (INIT → VALIDATE → EXECUTE → SETTLE)

## Usage

These files are used in the **End-to-End Testing Guide** (`docs/END_TO_END_TESTING_GUIDE.md`) to validate:

1. SQL*Loader file processing
2. Epic 2: Simple Transaction Processing (parallel processing)
3. Epic 3: Complex Transaction Processing (dependency management + staging)

## File Permissions

Ensure the application has read/write access to all directories:
```bash
chmod -R 755 /Users/pavankanduri/claude-ws/fabric-platform/data/
```