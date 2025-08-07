-- Test script to check if batch_configurations table exists and if data can be saved
-- Run this in your Oracle database

-- Check if table exists
SELECT table_name FROM user_tables WHERE table_name = 'BATCH_CONFIGURATIONS';

-- Check current data in the table (if it exists)
SELECT * FROM batch_configurations;

-- Check source systems
SELECT * FROM source_systems;

-- Check if the DDL has been executed by looking for configuration tables
SELECT table_name FROM user_tables WHERE table_name IN ('BATCH_CONFIGURATIONS', 'CONFIGURATION_AUDIT', 'SOURCE_SYSTEMS', 'JOB_DEFINITIONS');