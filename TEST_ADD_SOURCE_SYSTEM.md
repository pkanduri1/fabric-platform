# Test Guide: Add New Source System Feature

## ✅ Implementation Complete - WITH JOB CREATION!

The "Add New Source System" button and modal are now fully functional with the following features:

### 🎯 **ISSUE FIXED**: Jobs are now created in the database!

### Features Implemented:
1. ✅ **Add Source System Button** - Available in dashboard (3 locations):
   - Quick Actions section button
   - Floating Action Button (FAB) in bottom-right
   - Card with dashed border in the grid

2. ✅ **Modal Dialog** with:
   - Basic Information (Name, Type, Description)
   - Path Configuration (Input/Output paths)
   - Jobs Configuration (Add multiple jobs)
   - Form validation
   - Loading states
   - Error handling

3. ✅ **API Integration**:
   - POST endpoint: `/api/admin/source-systems` 
   - Proper request mapping to backend format
   - **NEW**: Jobs are saved to `job_definitions` table
   - Error handling for 409 (duplicate) and 400 (validation) errors
   - Success handling with list refresh

4. ✅ **Database Integration**:
   - Source system saved to `source_systems` table
   - **NEW**: Jobs saved to `job_definitions` table with foreign key relationship
   - Job count updated in source system record
   - Proper transaction handling

## 🧪 Testing Steps:

### 1. Start Both Applications
```bash
# Backend (Terminal 1)
cd fabric-core/fabric-api
mvn spring-boot:run

# Frontend (Terminal 2)
cd fabric-ui
npm start
```

### 2. Test the Add Source System Flow

1. **Navigate to Dashboard**: http://localhost:3000
   
2. **Open the Modal** (any of these methods):
   - Click "Add Source System" button in Quick Actions
   - Click the floating "+" button in bottom-right
   - Click the dashed "Add New Source System" card

3. **Fill in the Form**:
   ```
   Name: Test System
   Type: Oracle Database
   Description: Test source system for demo
   Input Path: /data/input (optional)
   Output Path: /data/output (optional)
   ```

4. **Add at least one Job**:
   ```
   Job Name: test_job
   Job Description: Test job for processing
   Multi-Txn: No
   ```
   Click the "+" button to add the job

5. **Submit**: Click "Add Source System"

### 3. Expected Results:

✅ **Success Case**:
- Loading spinner appears on button
- Modal closes on success
- New source system appears in dashboard
- Source systems list refreshes automatically

✅ **Error Cases Handled**:
- Duplicate system name → "Source system with this name already exists"
- Validation errors → Specific field error messages
- Network errors → General error message

### 4. Verify in Backend:

Check the backend logs to confirm the API call:
```bash
tail -f fabric-core/fabric-api/backend.log | grep -E "source.system|job.definition"
```

You should see:
```
INFO: Creating new source system with ID: TEST_SYSTEM
INFO: Successfully created source system: TEST_SYSTEM
INFO: Created job definition: TEST_SYSTEM-test_job
```

### 5. Verify in Database:

Check that both tables were updated:
```sql
-- Check source system was created
SELECT * FROM CM3INT.SOURCE_SYSTEMS WHERE ID = 'TEST_SYSTEM';

-- Check jobs were created
SELECT * FROM CM3INT.JOB_DEFINITIONS WHERE SOURCE_SYSTEM_ID = 'TEST_SYSTEM';
```

You should see:
- Source system record with `JOB_COUNT = 1`
- Job definition record(s) with the job(s) you added

## 🎯 Key Implementation Details:

### Frontend Changes:
- `HomePage.tsx`: Wired up API call in `handleAddSourceSystem`
- `configApi.ts`: Added `addSourceSystem` method with **job mapping**
- `AddSourceSystemDialog.tsx`: Added loading states and error handling

### Backend Changes:
- `CreateSourceSystemRequest.java`: Added `JobRequest` nested class and `jobs` field
- `JobDefinitionEntity.java`: Created JdbcTemplate-compatible entity (no JPA)
- `JobDefinitionRepository.java`: Created JdbcTemplate-based repository
- `SourceSystemServiceImpl.java`: Enhanced to create jobs after source system creation

### API Mapping:
```javascript
// Frontend SourceSystem → Backend CreateSourceSystemRequest
{
  id: "TEST_SYSTEM",           // Generated from name
  name: "Test System",
  type: "ORACLE",              // Uppercase enum
  description: "Test system",
  connectionString: "",        // Optional
  enabled: true,
  jobs: [                      // NEW: Jobs array
    {
      name: "test_job",
      description: "Test job for processing",
      transactionTypes: "default"
    }
  ]
}
```

### Database Structure:
```sql
-- SOURCE_SYSTEMS table
CREATE TABLE source_systems (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100),
    type VARCHAR(20),
    description VARCHAR(500),
    job_count INTEGER DEFAULT 0,
    enabled VARCHAR2(1),
    created_date TIMESTAMP
);

-- JOB_DEFINITIONS table  
CREATE TABLE job_definitions (
    id VARCHAR(100) PRIMARY KEY,
    source_system_id VARCHAR(50),
    job_name VARCHAR(50),
    description VARCHAR(500),
    transaction_types VARCHAR(200),
    enabled VARCHAR2(1),
    created_date TIMESTAMP,
    FOREIGN KEY (source_system_id) REFERENCES source_systems(id)
);
```

### System Type Mapping:
- oracle → ORACLE
- mssql → SQLSERVER
- file → FILE
- api → API

## 🚨 Troubleshooting:

If you encounter issues:

1. **Check Backend is Running**:
   ```bash
   curl http://localhost:8080/api/admin/source-systems/health
   ```

2. **Check Frontend Console**:
   - Open browser DevTools → Console
   - Look for network errors or validation issues

3. **Verify CORS**:
   - Backend allows `http://localhost:3000`
   - Check for CORS errors in browser console

4. **Database Issues**:
   - Ensure Oracle database is accessible
   - Check if source_systems table exists

## ✅ Feature is Ready for Testing!

The "Add New Source System" button and modal are fully functional and integrated with the backend API.