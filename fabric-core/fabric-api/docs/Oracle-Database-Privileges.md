# Oracle Database Privilege Requirements for Liquibase

## Overview

This document outlines the Oracle database privileges required for the Fabric Platform's Liquibase database migrations to execute successfully. The current failure is due to insufficient privileges when creating views.

## Current Issue

**Error**: `ORA-01031: insufficient privileges`

**Failed Operation**: Creating view `CM3INT.V_BATCH_JOB_STATUS`

**Liquibase Changeset**: `src/main/resources/db/changelog/releases/us001/us001-009-spring-batch-metadata-tables.xml::us001-009-08-create-views`

## Required Database Privileges

### Core Schema Privileges (CM3INT)

The `CM3INT` schema user requires the following privileges:

#### Table Management
- `CREATE TABLE` - For creating new tables
- `ALTER TABLE` - For modifying existing table structures  
- `DROP TABLE` - For removing tables during rollbacks
- `CREATE INDEX` - For creating database indexes
- `DROP INDEX` - For removing indexes

#### View Management
- `CREATE VIEW` - **CRITICAL** - Currently missing, causing the build failure
- `DROP VIEW` - For removing views during rollbacks
- `CREATE OR REPLACE VIEW` - For updating view definitions

#### Sequence Management
- `CREATE SEQUENCE` - For creating auto-increment sequences
- `ALTER SEQUENCE` - For modifying sequence properties
- `DROP SEQUENCE` - For removing sequences

#### Data Manipulation
- `SELECT` - On all tables within CM3INT schema
- `INSERT` - On all tables within CM3INT schema  
- `UPDATE` - On all tables within CM3INT schema
- `DELETE` - On all tables within CM3INT schema

### Spring Batch Metadata Tables

The application requires access to Spring Batch metadata tables:

#### Required Tables
- `BATCH_JOB_INSTANCE`
- `BATCH_JOB_EXECUTION`
- `BATCH_JOB_EXECUTION_PARAMS`
- `BATCH_JOB_EXECUTION_CONTEXT`
- `BATCH_STEP_EXECUTION`
- `BATCH_STEP_EXECUTION_CONTEXT`

#### Required Privileges
- `SELECT`, `INSERT`, `UPDATE`, `DELETE` on all batch metadata tables
- `CREATE VIEW` privilege to create monitoring views on these tables

### System-Level Privileges

For enterprise deployments, consider these additional privileges:

#### Monitoring and Diagnostics
- `CREATE VIEW` on system tables (for performance monitoring views)
- `SELECT` on `V$SESSION` (for connection monitoring)
- `SELECT` on `V$SQL` (for query performance analysis)

#### Advanced Features (Optional)
- `CREATE MATERIALIZED VIEW` (for performance optimization)
- `CREATE SYNONYM` (for schema abstraction)
- `CREATE PROCEDURE` (for stored procedures if needed)

## Grant Statements

Execute these statements as a DBA user to grant required privileges:

```sql
-- Connect as DBA user (e.g., system, sys)
CONNECT system/password@ORCLPDB1;

-- Grant core table privileges
GRANT CREATE TABLE TO CM3INT;
GRANT ALTER TABLE TO CM3INT; 
GRANT DROP TABLE TO CM3INT;
GRANT CREATE INDEX TO CM3INT;
GRANT DROP INDEX TO CM3INT;

-- Grant view privileges (CRITICAL for current failure)
GRANT CREATE VIEW TO CM3INT;
GRANT DROP VIEW TO CM3INT;

-- Grant sequence privileges
GRANT CREATE SEQUENCE TO CM3INT;
GRANT ALTER SEQUENCE TO CM3INT;
GRANT DROP SEQUENCE TO CM3INT;

-- Grant Spring Batch table access
GRANT SELECT, INSERT, UPDATE, DELETE ON BATCH_JOB_INSTANCE TO CM3INT;
GRANT SELECT, INSERT, UPDATE, DELETE ON BATCH_JOB_EXECUTION TO CM3INT;
GRANT SELECT, INSERT, UPDATE, DELETE ON BATCH_JOB_EXECUTION_PARAMS TO CM3INT;
GRANT SELECT, INSERT, UPDATE, DELETE ON BATCH_JOB_EXECUTION_CONTEXT TO CM3INT;
GRANT SELECT, INSERT, UPDATE, DELETE ON BATCH_STEP_EXECUTION TO CM3INT;
GRANT SELECT, INSERT, UPDATE, DELETE ON BATCH_STEP_EXECUTION_CONTEXT TO CM3INT;

-- Optional: Grant resource quota (if needed)
ALTER USER CM3INT QUOTA UNLIMITED ON USERS;
ALTER USER CM3INT QUOTA UNLIMITED ON TEMP;
```

## Verification Commands

After granting privileges, verify the setup:

```sql
-- Connect as CM3INT user
CONNECT cm3int/MySecurePass123@localhost:1521/ORCLPDB1;

-- Test view creation privilege
CREATE OR REPLACE VIEW TEST_VIEW AS SELECT 1 as test_col FROM DUAL;
DROP VIEW TEST_VIEW;

-- Verify table creation
CREATE TABLE TEST_TABLE (id NUMBER, name VARCHAR2(50));
DROP TABLE TEST_TABLE;

-- Check granted privileges
SELECT * FROM USER_SYS_PRIVS WHERE PRIVILEGE LIKE '%VIEW%';
SELECT * FROM USER_SYS_PRIVS WHERE PRIVILEGE LIKE '%TABLE%';
```

## Security Considerations

### Principle of Least Privilege
- Grant only the minimum privileges required for Liquibase operations
- Consider using a separate deployment user with elevated privileges
- Restrict runtime application user to data manipulation only

### Environment-Specific Privileges
- **Development**: Full DDL privileges for schema evolution
- **Testing**: Limited DDL privileges for controlled testing
- **Production**: Minimal privileges, potentially using database deployment automation

### Privilege Separation Strategy
```sql
-- Option 1: Single user with all privileges (current setup)
-- Pros: Simple setup
-- Cons: Violates least privilege principle

-- Option 2: Separate deployment and runtime users (recommended)
-- CM3INT_DEPLOY: DDL privileges for Liquibase
-- CM3INT_APP: DML privileges for application runtime
```

## Alternative Solutions

### 1. Database Administrator Deployment
- Have DBA team execute Liquibase migrations with elevated privileges
- Application uses runtime user with limited privileges

### 2. Liquibase Pro Features
- Use Liquibase Pro's database user management features
- Automated privilege escalation during deployments

### 3. Docker/Containerized Database
```bash
# For development environments, use Docker with pre-configured privileges
docker run -d \
  --name oracle-db \
  -p 1521:1521 \
  -e ORACLE_SID=ORCLCDB \
  -e ORACLE_PDB=ORCLPDB1 \
  -e ORACLE_PWD=MySecurePass123 \
  -e ORACLE_MEM=2GB \
  -v oracle-data:/opt/oracle/oradata \
  container-registry.oracle.com/database/enterprise:19.3.0.0
```

## Troubleshooting

### Common Issues

1. **ORA-01031: insufficient privileges**
   - Solution: Grant `CREATE VIEW` privilege to CM3INT user

2. **ORA-00942: table or view does not exist**
   - Solution: Ensure Spring Batch metadata tables exist
   - Run Spring Batch initialization scripts first

3. **ORA-01950: no privileges on tablespace**
   - Solution: Grant quota on tablespace or use `UNLIMITED` quota

### Debug Commands
```sql
-- Check current user privileges
SELECT * FROM USER_SYS_PRIVS;

-- Check granted roles
SELECT * FROM USER_ROLE_PRIVS;

-- Check tablespace quota
SELECT * FROM USER_TS_QUOTAS;

-- Check if specific privilege exists
SELECT COUNT(*) FROM USER_SYS_PRIVS WHERE PRIVILEGE = 'CREATE VIEW';
```

## Recommendations

### Immediate Action (Fix Current Build)
1. Connect as DBA user to Oracle database
2. Execute: `GRANT CREATE VIEW TO CM3INT;`
3. Execute: `GRANT DROP VIEW TO CM3INT;`
4. Retry Maven build: `mvn clean compile`

### Long-term Improvements
1. Implement privilege separation with deployment vs runtime users
2. Create automated privilege verification scripts
3. Document privilege requirements in deployment runbooks
4. Consider using Oracle Database Cloud Service for simplified management

### Enterprise Deployment
1. Work with DBA team to establish proper privilege management
2. Implement infrastructure-as-code for database provisioning
3. Use automated deployment pipelines with proper security controls
4. Regular privilege audits and compliance reporting

## Related Documentation

- [Spring Batch Database Schema](https://docs.spring.io/spring-batch/docs/current/reference/html/schema-appendix.html)
- [Liquibase Oracle Database Tutorial](https://docs.liquibase.com/start/tutorials/oracle.html)
- [Oracle Database Security Guide](https://docs.oracle.com/en/database/oracle/oracle-database/19/dbseg/)

---

**Document Version**: 1.0  
**Last Updated**: August 16, 2025  
**Author**: Senior Full Stack Developer Agent  
**Status**: Current Issue Documentation