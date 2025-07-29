-- ============================================================================
-- SQL*Loader Control File Template for Pipe-Delimited Files
-- ============================================================================
-- Template Name: pipe-delimited-control-template.ctl
-- File Type: Pipe-delimited data files
-- Description: Standard template for loading pipe-delimited files with
--              comprehensive error handling and data validation
-- ============================================================================

-- SQL*Loader Options
OPTIONS (
  DIRECT=TRUE,                    -- Use direct path loading for performance
  ERRORS=1000,                    -- Maximum number of errors allowed
  SKIP=1,                         -- Skip header row
  BINDSIZE=256000,               -- Bind array size in bytes
  READSIZE=1048576,              -- Read buffer size in bytes
  RESUMABLE=TRUE,                -- Enable resumable operations
  RESUMABLE_TIMEOUT=7200,        -- Resumable timeout (2 hours)
  MULTITHREADING=TRUE            -- Enable multithreading
)

-- Load specification
LOAD DATA
CHARACTERSET UTF8               -- Character set for input file
INFILE 'data_file.dat'         -- Input data file (will be replaced)
BADFILE 'data_file.bad'        -- Bad records file
DISCARDFILE 'data_file.dsc'    -- Discarded records file

-- Table and loading method
INSERT INTO TABLE ${TARGET_TABLE}    -- Target table (will be replaced)

-- Field specification for pipe-delimited format
FIELDS TERMINATED BY '|'             -- Field delimiter
       OPTIONALLY ENCLOSED BY '"'    -- Optional string delimiter
       LTRIM                          -- Left trim whitespace

-- Record specification  
TRAILING NULLCOLS                     -- Allow missing trailing columns

(
  -- ========================================================================
  -- FIELD DEFINITIONS
  -- ========================================================================
  -- Note: Customize these field definitions based on your data structure
  -- Format: COLUMN_NAME [POSITION] [DATATYPE] [CONSTRAINTS]
  
  -- Example fields (replace with actual field definitions)
  customer_id               CHAR NULLIF customer_id=BLANKS,
  customer_name             CHAR(100) NULLIF customer_name=BLANKS "LTRIM(RTRIM(:customer_name))",
  email_address             CHAR(255) NULLIF email_address=BLANKS "LOWER(LTRIM(RTRIM(:email_address)))",
  phone_number              CHAR(20) NULLIF phone_number=BLANKS "REGEXP_REPLACE(:phone_number, '[^0-9]', '')",
  date_created              DATE "YYYY-MM-DD HH24:MI:SS" NULLIF date_created=BLANKS,
  account_balance           DECIMAL EXTERNAL NULLIF account_balance=BLANKS,
  status_code               CHAR(10) NULLIF status_code=BLANKS "UPPER(:status_code)",
  
  -- Data transformation examples
  full_name                 CHAR(200) "LTRIM(RTRIM(:full_name))",
  normalized_phone          CHAR(15) "CASE WHEN LENGTH(:phone_number) = 10 THEN 
                                      '(' || SUBSTR(:phone_number,1,3) || ') ' || 
                                      SUBSTR(:phone_number,4,3) || '-' || 
                                      SUBSTR(:phone_number,7,4) 
                                      ELSE :phone_number END",
  
  -- Conditional loading example
  active_flag               CHAR(1) "CASE WHEN UPPER(:status_code) = 'ACTIVE' THEN 'Y' ELSE 'N' END",
  
  -- Date with default value
  record_date               DATE "YYYY-MM-DD" "NVL(:record_date, SYSDATE)",
  
  -- Audit fields (automatically populated)
  load_timestamp            TIMESTAMP "SYSTIMESTAMP",
  load_user                 CHAR(50) "USER",
  load_session              CHAR(100) "SYS_CONTEXT('USERENV','SESSIONID')"
)

-- ============================================================================
-- FIELD CUSTOMIZATION GUIDE
-- ============================================================================
-- 
-- Data Types:
-- -----------
-- CHAR              - Character data (default)
-- CHAR(n)           - Character data with specific length
-- INTEGER EXTERNAL  - Integer stored as character
-- DECIMAL EXTERNAL  - Decimal number stored as character
-- FLOAT EXTERNAL    - Floating point number stored as character
-- DATE "format"     - Date with specific format
-- TIMESTAMP "format"- Timestamp with specific format
--
-- Common Transformations:
-- ----------------------
-- LTRIM(RTRIM(:field))                    - Remove leading/trailing spaces
-- UPPER(:field)                           - Convert to uppercase
-- LOWER(:field)                           - Convert to lowercase
-- SUBSTR(:field, start, length)           - Extract substring
-- REPLACE(:field, 'old', 'new')          - Replace text
-- REGEXP_REPLACE(:field, pattern, replacement) - Regular expression replace
-- TO_DATE(:field, 'format')              - Convert to date
-- TO_NUMBER(:field)                       - Convert to number
-- NVL(:field, default_value)              - Provide default for null
-- CASE WHEN condition THEN value ELSE value END - Conditional logic
--
-- Null Handling:
-- --------------
-- NULLIF field=BLANKS                     - Treat blanks as null
-- NULLIF field='N/A'                      - Treat 'N/A' as null
-- DEFAULTIF field=BLANKS 'DEFAULT_VALUE'  - Provide default for blanks
--
-- Position-based (for fixed-width files):
-- ---------------------------------------
-- field_name POSITION(1:10) CHAR          - Characters 1-10
-- field_name POSITION(11:20) INTEGER EXTERNAL - Characters 11-20 as integer
--
-- ============================================================================
-- PERFORMANCE TUNING OPTIONS
-- ============================================================================
--
-- For large files (> 1GB):
-- OPTIONS (
--   DIRECT=TRUE,
--   PARALLEL=TRUE,
--   DEGREE=4,                    -- Number of parallel streams
--   MULTITHREADING=TRUE,
--   STREAMSIZE=256000,
--   BINDSIZE=1048576,
--   READSIZE=4194304
-- )
--
-- For high-error tolerance:
-- OPTIONS (
--   ERRORS=10000,               -- Allow more errors
--   CONTINUEIF LAST != '|'      -- Continue on format errors
-- )
--
-- For data validation:
-- Add constraints in field definitions:
--   field_name CHAR "CASE WHEN LENGTH(:field_name) > 0 THEN :field_name ELSE NULL END"
--
-- ============================================================================
-- COMMON ERROR HANDLING PATTERNS
-- ============================================================================
--
-- 1. Handle missing required fields:
--    required_field CHAR NULLIF required_field=BLANKS 
--                        "CASE WHEN :required_field IS NULL THEN 
--                         RAISE_APPLICATION_ERROR(-20001, 'Required field missing') 
--                         ELSE :required_field END"
--
-- 2. Data format validation:
--    email_field CHAR "CASE WHEN :email_field LIKE '%@%' 
--                       THEN :email_field 
--                       ELSE NULL END"
--
-- 3. Reference data validation:
--    status_code CHAR "CASE WHEN :status_code IN ('A', 'I', 'P') 
--                       THEN :status_code 
--                       ELSE 'U' END"
--
-- ============================================================================
-- TEMPLATE VARIABLES TO REPLACE
-- ============================================================================
-- ${TARGET_TABLE}     - Target database table name
-- ${INPUT_FILE}       - Input data file path
-- ${BAD_FILE}         - Bad records output file path
-- ${DISCARD_FILE}     - Discarded records output file path
-- ${FIELD_DELIMITER}  - Field separator character
-- ${RECORD_DELIMITER} - Record separator character
-- ${SKIP_ROWS}        - Number of header rows to skip
-- ${MAX_ERRORS}       - Maximum allowable errors
-- ${CHAR_SET}         - Character set encoding
-- ============================================================================