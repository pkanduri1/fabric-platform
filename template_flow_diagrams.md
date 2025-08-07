# Template-to-Source System Flow Diagrams

## 1. CURRENT STATE FLOW (Existing US001 Implementation)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CURRENT WORKFLOW                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  User Journey: "I want to configure CD01 template for HR system"           │
│                                                                             │
│  Step 1: User selects "HR" in sidebar                                      │
│  ┌─────────────┐                                                           │
│  │   Sidebar   │ ──────────────────────────────────────────────────────────┐│
│  │             │                                                           ││
│  │  • HR ✓     │  (User must remember HR is selected)                     ││
│  │  • DDA      │                                                           ││
│  │  • Shaw     │                                                           ││
│  └─────────────┘                                                           ││
│                                                                             ││
│  Step 2: Navigate to Template Admin                                        ││
│  ┌─────────────────────────────────────────────────────────────────────────┘│
│  │  Template Admin Page                                                     │
│  │  ┌─────────────────────────────────────────────────────────────────────┐│
│  │  │  CD01 Consumer Default Template                                     ││
│  │  │  ┌─────────────────┐    ┌─────────────────┐                       ││
│  │  │  │  View Details   │    │  Edit Template  │                       ││
│  │  │  └─────────────────┘    └─────────────────┘                       ││
│  │  │                                                                   ││
│  │  │  ❌ NO CLEAR WAY TO ASSOCIATE WITH HR SYSTEM                       ││
│  │  └─────────────────────────────────────────────────────────────────────┘│
│  └───────────────────────────────────────────────────────────────────────── │
│                                                                             │
│  Step 3: User calls separate API endpoint                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  POST /api/admin/templates/CD01/DEFAULT/create-config                  │
│  │  {                                                                     │
│  │    "sourceSystem": "hr",    # User must remember from sidebar          │
│  │    "jobName": "cd01_hr_job" # User must manually create name          │
│  │  }                                                                     │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ❌ PROBLEMS:                                                               │
│  • Context switching between sidebar and template                          │
│  • No visual connection between template and source system                 │
│  • Manual job name creation                                                │
│  • No visibility into existing configurations                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2. PROPOSED ENHANCED FLOW (With Template Enhancement)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ENHANCED WORKFLOW                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  User Journey: "I want to configure CD01 template for HR system"           │
│                                                                             │
│  Step 1: User goes directly to Template Admin                              │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  Template Admin Page (Enhanced)                                        │
│  │  ┌─────────────────────────────────────────────────────────────────────┐│
│  │  │  CD01 Consumer Default Template                                     ││
│  │  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐ ││
│  │  │  │  View Details   │  │  Edit Template  │  │ Configure Sources   │ ││
│  │  │  └─────────────────┘  └─────────────────┘  └─────────────────────┘ ││
│  │  │                                            ⬆ NEW BUTTON             ││
│  │  │  Used by: HR (2 configs), DDA (1 config)  📊 Usage Analytics       ││
│  │  │           ⬆ USAGE VISIBILITY                                        ││
│  │  └─────────────────────────────────────────────────────────────────────┘│
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Step 2: Click "Configure Sources" → Opens Contextual Modal                │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  ┌─────────────────────────────────────────────────────────────────────┐│
│  │  │                Contextual Configuration Modal                       ││
│  │  │                                                                     ││
│  │  │  Configure CD01/DEFAULT for Source Systems                         ││
│  │  │                                                                     ││
│  │  │  Source Systems:                                                    ││
│  │  │  ☑ HR (Suggested job: cd01_hr_default_job) ✨ Smart Default        ││
│  │  │  ☐ DDA (Already configured: cd01_dda_job) ⚠ Existing Config        ││
│  │  │  ☐ Shaw (Suggested job: cd01_shaw_default_job)                     ││
│  │  │                                                                     ││
│  │  │  Job Name Preferences:                                              ││
│  │  │  HR: [cd01_hr_default_job            ] ✨ Auto-suggested           ││
│  │  │                                                                     ││
│  │  │  ☑ Apply smart defaults                                             ││
│  │  │                                                                     ││
│  │  │  [Cancel]  [Configure 1 Source System]                             ││
│  │  └─────────────────────────────────────────────────────────────────────┘│
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Step 3: System automatically creates configuration                        │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  Backend Processing:                                                    │
│  │  1. Validate template exists ✓                                         │
│  │  2. Check source system availability ✓                                 │
│  │  3. Generate smart job name ✓                                          │
│  │  4. Create configuration via existing API                              │
│  │  5. Track usage analytics ✓                                            │
│  │  6. Update usage dashboard ✓                                           │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ✅ BENEFITS:                                                               │
│  • Single-screen workflow                                                  │
│  • Visual context maintained                                               │
│  • Smart defaults reduce errors                                            │
│  • Usage analytics provide insights                                        │
│  • Batch operations for multiple systems                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 3. DETAILED SYSTEM ARCHITECTURE FLOW

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SYSTEM ARCHITECTURE FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Frontend (React)                     Backend (Spring Boot)                │
│  ┌─────────────────────┐               ┌─────────────────────────────────────┐ │
│  │  Template Admin     │               │           Controllers               │ │
│  │  Page               │               │  ┌─────────────────────────────────┐ │ │
│  │  ┌─────────────────┐│  HTTP Request │  │    TemplateController          │ │ │
│  │  │ Configure       ││ ────────────> │  │    (Existing)                  │ │ │
│  │  │ Sources Button  ││               │  └─────────────────────────────────┘ │ │
│  │  └─────────────────┘│               │  ┌─────────────────────────────────┐ │ │
│  └─────────────────────┘               │  │ TemplateAnalyticsController     │ │ │
│           │                            │  │ (New)                           │ │ │
│           │                            │  └─────────────────────────────────┘ │ │
│           ▼                            └─────────────────────────────────────┘ │
│  ┌─────────────────────┐                              │                       │
│  │  Contextual Config  │                              ▼                       │
│  │  Modal              │               ┌─────────────────────────────────────┐ │
│  │  ┌─────────────────┐│  HTTP Request │           Service Layer              │ │
│  │  │ Source System   ││ ────────────> │  ┌─────────────────────────────────┐ │ │
│  │  │ Dropdown        ││               │  │    TemplateService              │ │ │
│  │  │ ☑ HR            ││               │  │    (Existing)                   │ │ │
│  │  │ ☐ DDA           ││               │  └─────────────────────────────────┘ │ │
│  │  │ ☐ Shaw          ││               │  ┌─────────────────────────────────┐ │ │
│  │  └─────────────────┘│               │  │ TemplateUsageAnalyticsService   │ │ │
│  │  ┌─────────────────┐│               │  │ (New)                           │ │ │
│  │  │ Job Name Field  ││               │  │ • recordUsageAsync()            │ │ │
│  │  │ [cd01_hr_job]   ││               │  │ • getUsageAnalytics()           │ │ │
│  │  └─────────────────┘│               │  │ • createContextualConfig()      │ │ │
│  │  ┌─────────────────┐│               │  └─────────────────────────────────┘ │ │
│  │  │ [Configure]     ││               └─────────────────────────────────────┘ │
│  │  │ Button          ││                              │                       │
│  │  └─────────────────┘│                              ▼                       │
│  └─────────────────────┘               ┌─────────────────────────────────────┐ │
│           │                            │           Data Layer                 │ │
│           │                            │                                     │ │
│           ▼                            │  ┌─────────────────────────────────┐ │ │
│  ┌─────────────────────┐               │  │         Oracle Database         │ │ │
│  │  Success Response   │  HTTP Response│  │                                 │ │ │
│  │  ┌─────────────────┐│ <──────────── │  │  Existing Tables:               │ │ │
│  │  │ Configuration   ││               │  │  • FILE_TYPE_TEMPLATES          │ │ │
│  │  │ Created         ││               │  │  • FIELD_TEMPLATES              │ │ │
│  │  │ Successfully    ││               │  │  • BATCH_CONFIGURATIONS         │ │ │
│  │  └─────────────────┘│               │  │                                 │ │ │
│  │  ┌─────────────────┐│               │  │  New Tables:                    │ │ │
│  │  │ Updated Usage   ││               │  │  • FABRIC_TEMPLATE_USAGE_       │ │ │
│  │  │ Dashboard       ││               │  │    ANALYTICS                    │ │ │
│  │  └─────────────────┘│               │  │  • FABRIC_TEMPLATE_USAGE_       │ │ │
│  └─────────────────────┘               │  │    METRICS                      │ │ │
│                                        │  └─────────────────────────────────┘ │ │
│                                        │  ┌─────────────────────────────────┐ │ │
│                                        │  │           Redis Cache            │ │ │
│                                        │  │                                 │ │ │
│                                        │  │  • Template usage metrics       │ │ │
│                                        │  │  • Smart defaults cache         │ │ │
│                                        │  │  • User preferences             │ │ │
│                                        │  └─────────────────────────────────┘ │ │
│                                        └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 4. DATA FLOW DIAGRAM

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DATA FLOW                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Template Configuration Request Flow:                                       │
│                                                                             │
│  1. User Action                                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  User clicks "Configure Sources" for CD01 template                     │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                ↓                                           │
│  2. Frontend API Call                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  GET /api/admin/templates/CD01/DEFAULT/smart-defaults?sourceSystem=hr   │
│  │  Response: {                                                            │
│  │    "suggestedJobName": "cd01_hr_default_job",                          │
│  │    "existingConfigs": [],                                              │
│  │    "sourceSystemInfo": { "id": "hr", "name": "HR System" }            │
│  │  }                                                                     │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                ↓                                           │
│  3. User Submits Configuration                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  POST /api/admin/templates/analytics/CD01/DEFAULT/create-contextual-config│
│  │  {                                                                     │
│  │    "sourceSystems": ["hr"],                                           │
│  │    "jobNamePreferences": { "hr": "cd01_hr_default_job" },             │
│  │    "applySmartDefaults": true                                          │
│  │  }                                                                     │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                ↓                                           │
│  4. Backend Processing                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  TemplateUsageAnalyticsService.createContextualConfiguration()         │
│  │  │                                                                     │
│  │  ├─ Validate template exists ✓                                         │
│  │  ├─ Check source system availability ✓                                 │
│  │  ├─ Call existing TemplateService.createConfigurationFromTemplate()    │
│  │  ├─ Record usage analytics (async) ✓                                   │
│  │  └─ Update cache and metrics ✓                                         │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                ↓                                           │
│  5. Database Updates                                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  Transaction 1 (Main):                                                 │
│  │  INSERT INTO BATCH_CONFIGURATIONS                                      │
│  │  (source_system, job_name, file_type, transaction_type, ...)          │
│  │  VALUES ('hr', 'cd01_hr_default_job', 'CD01', 'DEFAULT', ...)         │
│  │                                                                        │
│  │  Transaction 2 (Async):                                                │
│  │  INSERT INTO FABRIC_TEMPLATE_USAGE_ANALYTICS                           │
│  │  (file_type, source_system, operation_type, user_id, ...)             │
│  │  VALUES ('CD01', 'hr', 'CREATE_CONFIG', 'test_user', ...)             │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                ↓                                           │
│  6. Response to Frontend                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  {                                                                     │
│  │    "success": true,                                                    │
│  │    "results": [                                                        │
│  │      {                                                                 │
│  │        "sourceSystem": "hr",                                          │
│  │        "jobName": "cd01_hr_default_job",                              │
│  │        "configId": "config-12345",                                    │
│  │        "success": true                                                 │
│  │      }                                                                 │
│  │    ],                                                                  │
│  │    "message": "Successfully configured 1 source system"               │
│  │  }                                                                     │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                ↓                                           │
│  7. Frontend Updates                                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  • Close modal with success message                                    │
│  │  • Refresh template usage indicators                                   │
│  │  • Update "Used by" display: "HR (3 configs), DDA (1 config)"        │
│  │  • Invalidate cache for usage dashboard                                │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 5. ERROR HANDLING FLOW

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ERROR HANDLING FLOW                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Scenario: User tries to configure already configured source system        │
│                                                                             │
│  1. User Selection                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  User selects HR system (already has configuration)                    │
│  │  Modal shows: ⚠ HR (Already configured: cd01_hr_job)                   │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                ↓                                           │
│  2. Smart Detection                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  Backend query:                                                        │
│  │  SELECT job_name FROM batch_configurations                             │
│  │  WHERE source_system = 'hr' AND file_type = 'CD01'                    │
│  │                                                                        │
│  │  Found: cd01_hr_job                                                    │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                ↓                                           │
│  3. User Choice                                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  Modal presents options:                                               │
│  │  ○ Update existing configuration                                       │
│  │  ○ Create new configuration with different job name                    │
│  │  ○ Cancel                                                              │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                ↓                                           │
│  4. Graceful Resolution                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  If Update: PATCH existing configuration                               │
│  │  If New: CREATE with validation for unique job name                    │
│  │  If Cancel: Close modal with no changes                                │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Database Error Scenario:                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  1. Configuration creation fails                                       │
│  │  2. Transaction rolled back automatically                              │
│  │  3. User sees specific error message                                   │
│  │  4. Analytics recording continues (separate transaction)               │
│  │  5. User can retry with corrected input                                │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 6. PERFORMANCE & SCALABILITY FLOW

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      PERFORMANCE & SCALABILITY                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Caching Strategy:                                                          │
│                                                                             │
│  1. Template Usage Data (High Frequency)                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  Redis Key: "template:usage:CD01:DEFAULT"                              │
│  │  TTL: 5 minutes                                                        │
│  │  Data: {                                                               │
│  │    "sourceSystems": ["hr", "dda"],                                    │
│  │    "totalConfigs": 3,                                                 │
│  │    "lastUpdated": "2025-08-04T22:30:00Z"                             │
│  │  }                                                                     │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  2. Smart Defaults (Medium Frequency)                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  Redis Key: "smart:defaults:CD01:DEFAULT:hr"                          │
│  │  TTL: 1 hour                                                          │
│  │  Data: {                                                               │
│  │    "suggestedJobName": "cd01_hr_default_job",                         │
│  │    "similarConfigs": [...],                                           │
│  │    "recommendations": [...]                                           │
│  │  }                                                                     │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  3. Analytics Dashboard (Low Frequency)                                    │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  Redis Key: "dashboard:analytics:global"                              │
│  │  TTL: 15 minutes                                                      │
│  │  Data: Aggregated analytics across all templates                      │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Database Query Optimization:                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  • Partitioned analytics tables by month                              │
│  │  • Indexed on (file_type, usage_timestamp)                            │
│  │  • Materialized views for complex aggregations                        │
│  │  • Async processing for non-critical analytics                        │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Concurrent User Handling:                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┤
│  │  • Separate thread pool for analytics operations                       │
│  │  • Redis clustering for multi-instance deployments                    │
│  │  • Database connection pooling scaled for increased load               │
│  │  • Circuit breakers for analytics service failures                    │
│  └─────────────────────────────────────────────────────────────────────────┤
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```