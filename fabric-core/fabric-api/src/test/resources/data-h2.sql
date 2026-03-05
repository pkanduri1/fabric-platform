-- Test data for integration tests
-- Used by SourceSystemValidationIntegrationTest

INSERT INTO CM3INT.SOURCE_SYSTEMS (ID, NAME, TYPE, DESCRIPTION, ENABLED, JOB_COUNT)
VALUES ('MTG', 'MTG Source System', 'BATCH', 'MTG source system for testing', 'Y', 0);

INSERT INTO CM3INT.SOURCE_SYSTEMS (ID, NAME, TYPE, DESCRIPTION, ENABLED, JOB_COUNT)
VALUES ('SHAW', 'SHAW Source System', 'BATCH', 'SHAW source system for testing', 'Y', 0);

INSERT INTO CM3INT.SOURCE_SYSTEMS (ID, NAME, TYPE, DESCRIPTION, ENABLED, JOB_COUNT)
VALUES ('ENCORE', 'ENCORE Source System', 'BATCH', 'ENCORE source system for testing', 'Y', 0);
