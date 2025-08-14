# US001 LIQUIBASE ROLLBACK PROCEDURES AND EMERGENCY RECOVERY

## Document Control

| Field | Value |
|-------|-------|
| Document Title | US001 Liquibase Rollback Procedures and Emergency Recovery |
| Version | 1.0 |
| Date | 2025-08-13 |
| Author | Senior Full Stack Developer Agent |
| Classification | INTERNAL - BANKING CONFIDENTIAL |
| Approval Status | FOR ARCHITECT REVIEW |

---

## Table of Contents

1. [Overview](#overview)
2. [Rollback Scenarios](#rollback-scenarios)
3. [Pre-Rollback Checklist](#pre-rollback-checklist)
4. [Rollback Procedures](#rollback-procedures)
5. [Emergency Recovery Procedures](#emergency-recovery-procedures)
6. [Post-Rollback Validation](#post-rollback-validation)
7. [Communication Procedures](#communication-procedures)
8. [Risk Assessment and Mitigation](#risk-assessment-and-mitigation)

---

## Overview

This document provides comprehensive procedures for rolling back US001 Manual Job Configuration Interface database changes implemented via Liquibase. These procedures ensure safe and reliable rollback operations while maintaining data integrity and regulatory compliance.

### Critical Success Factors

- **Data Backup Verification**: All data backups must be verified before rollback
- **Stakeholder Communication**: All stakeholders must be notified before rollback
- **SOX Compliance**: All rollback activities must maintain audit trail integrity
- **Zero Data Loss**: Rollback procedures must not result in data loss
- **Minimal Downtime**: Rollback should complete within defined maintenance window

---

## Rollback Scenarios

### Scenario 1: Individual Changeset Rollback
**Trigger**: Single changeset deployment failure or validation error  
**Impact**: Low - affects specific database objects only  
**Recovery Time**: 15-30 minutes  
**Data Loss Risk**: Minimal  

### Scenario 2: Complete Release Rollback
**Trigger**: Multiple changeset failures or critical functional issues  
**Impact**: Medium - affects entire US001 feature set  
**Recovery Time**: 30-60 minutes  
**Data Loss Risk**: Low (with proper backup)  

### Scenario 3: Emergency Production Rollback
**Trigger**: Production system failure or security incident  
**Impact**: High - affects production operations  
**Recovery Time**: 60-120 minutes  
**Data Loss Risk**: Medium (depends on backup timing)  

### Scenario 4: Corruption Recovery
**Trigger**: Database corruption or data integrity violations  
**Impact**: Critical - affects data consistency  
**Recovery Time**: 2-4 hours  
**Data Loss Risk**: Variable (depends on corruption extent)  

---

## Pre-Rollback Checklist

### Database Assessment

- [ ] **Current State Documentation**
  - [ ] Document current database schema state
  - [ ] Capture current Liquibase changelog status
  - [ ] Record all applied changesets and their sequence
  - [ ] Document current data volumes

- [ ] **Backup Verification**
  - [ ] Verify database backup exists and is current
  - [ ] Test backup restoration in non-production environment
  - [ ] Confirm backup includes all US001 tables and data
  - [ ] Validate backup integrity checksums

- [ ] **Impact Assessment**
  - [ ] Identify dependent systems and applications
  - [ ] Document data relationships and constraints
  - [ ] Assess business impact during rollback window
  - [ ] Review user activity and scheduled jobs

### Technical Preparation

- [ ] **Environment Readiness**
  - [ ] Ensure sufficient storage space for rollback operations
  - [ ] Verify database connection stability
  - [ ] Confirm Liquibase tools and versions are available
  - [ ] Prepare rollback SQL scripts

- [ ] **Access and Permissions**
  - [ ] Verify DBA access credentials
  - [ ] Confirm emergency access procedures
  - [ ] Validate backup restoration permissions
  - [ ] Check audit logging capabilities

### Communication and Approval

- [ ] **Stakeholder Notification**
  - [ ] Notify Principal Enterprise Architect
  - [ ] Inform Lending Product Owner
  - [ ] Alert Operations Team
  - [ ] Update change management systems

- [ ] **Approval Chain**
  - [ ] Obtain rollback approval from appropriate authorities
  - [ ] Document business justification for rollback
  - [ ] Record emergency escalation contacts
  - [ ] Update incident management systems

---

## Rollback Procedures

### Procedure 1: Tag-Based Rollback (Recommended)

**Purpose**: Roll back to a specific tagged version using Liquibase tags  
**Use Case**: Complete release rollback or major issue resolution  

```bash
# 1. Connect to database and verify current state
sqlplus fabric_user@database_name

# 2. Check current Liquibase status
SELECT * FROM DATABASECHANGELOG 
WHERE ID LIKE 'us001-%' 
ORDER BY DATEEXECUTED DESC;

# 3. Perform tag-based rollback using Maven
cd /path/to/fabric-api
mvn liquibase:rollback -Dliquibase.rollbackTag=BEFORE_US001

# Alternative: Direct Liquibase command
liquibase --changeLogFile=src/main/resources/db/changelog/db.changelog-master.xml \
          --url=jdbc:oracle:thin:@localhost:1521:XE \
          --username=fabric_user \
          --password=fabric_password \
          rollback BEFORE_US001
```

### Procedure 2: Count-Based Rollback

**Purpose**: Roll back a specific number of changesets  
**Use Case**: Recent changeset issues or incremental rollback  

```bash
# 1. Determine number of changesets to rollback
SELECT COUNT(*) FROM DATABASECHANGELOG 
WHERE ID LIKE 'us001-%' 
AND DATEEXECUTED > (SELECT MAX(DATEEXECUTED) FROM DATABASECHANGELOG WHERE TAG = 'STABLE_BASELINE');

# 2. Perform count-based rollback
mvn liquibase:rollbackCount -Dliquibase.rollbackCount=7

# 3. Verify rollback completion
SELECT * FROM DATABASECHANGELOG 
WHERE ID LIKE 'us001-%' 
ORDER BY DATEEXECUTED DESC;
```

### Procedure 3: Date-Based Rollback

**Purpose**: Roll back changes made after a specific date  
**Use Case**: Time-based recovery or incident response  

```bash
# 1. Identify target rollback date
SELECT ID, DATEEXECUTED FROM DATABASECHANGELOG 
WHERE DATEEXECUTED > TO_DATE('2025-08-13 10:00:00', 'YYYY-MM-DD HH24:MI:SS')
ORDER BY DATEEXECUTED;

# 2. Generate rollback SQL for review
mvn liquibase:rollbackSQL -Dliquibase.rollbackDate="2025-08-13T10:00:00" \
    -Dliquibase.outputFile=rollback-preview.sql

# 3. Review generated SQL before execution
cat rollback-preview.sql

# 4. Execute rollback if SQL is acceptable
mvn liquibase:rollback -Dliquibase.rollbackDate="2025-08-13T10:00:00"
```

### Procedure 4: Manual Rollback (Emergency)

**Purpose**: Manual rollback when Liquibase automation fails  
**Use Case**: Emergency recovery or corrupted changelog  

```sql
-- 1. Manually execute rollback operations in reverse order

-- Step 1: Drop audit triggers (prevents constraint violations)
DROP TRIGGER TRG_MANUAL_JOB_CONFIG_AUDIT;
DROP TRIGGER TRG_MANUAL_JOB_AUDIT_IMMUTABLE;

-- Step 2: Drop indexes and constraints
DROP INDEX IDX_MANUAL_JOB_CONFIG_TYPE;
DROP INDEX IDX_MANUAL_JOB_EXEC_STATUS;
DROP INDEX IDX_JOB_TEMPLATE_TYPE;
DROP INDEX IDX_MANUAL_JOB_AUDIT_DATE;

-- Step 3: Drop foreign key constraints
ALTER TABLE MANUAL_JOB_EXECUTION DROP CONSTRAINT FK_MANUAL_JOB_EXECUTION_CONFIG;
ALTER TABLE MANUAL_JOB_AUDIT DROP CONSTRAINT FK_MANUAL_JOB_AUDIT_CONFIG;

-- Step 4: Drop tables in reverse dependency order
DROP TABLE MANUAL_JOB_AUDIT;
DROP TABLE MANUAL_JOB_EXECUTION;
DROP TABLE JOB_PARAMETER_TEMPLATES;
DROP TABLE MANUAL_JOB_CONFIG;
DROP TABLE SYSTEM_ERROR_LOG;

-- Step 5: Remove Liquibase changelog entries
DELETE FROM DATABASECHANGELOG WHERE ID LIKE 'us001-%';

-- Step 6: Update changelog lock if necessary
DELETE FROM DATABASECHANGELOGLOCK;

COMMIT;
```

---

## Emergency Recovery Procedures

### Recovery Scenario 1: Database Corruption

**Symptoms**:
- Table corruption errors
- Data integrity constraint violations
- Inconsistent query results

**Recovery Steps**:

```bash
# 1. Immediate Actions
# Stop application services
systemctl stop fabric-api
systemctl stop fabric-ui

# 2. Assess corruption extent
sqlplus fabric_user@database_name
ANALYZE TABLE MANUAL_JOB_CONFIG VALIDATE STRUCTURE CASCADE;
ANALYZE TABLE MANUAL_JOB_EXECUTION VALIDATE STRUCTURE CASCADE;
ANALYZE TABLE JOB_PARAMETER_TEMPLATES VALIDATE STRUCTURE CASCADE;
ANALYZE TABLE MANUAL_JOB_AUDIT VALIDATE STRUCTURE CASCADE;

# 3. If corruption is limited, attempt repair
# For Oracle: Use DBMS_REPAIR package
BEGIN
  DBMS_REPAIR.CHECK_OBJECT(
    schema_name => 'FABRIC_CORE',
    object_name => 'MANUAL_JOB_CONFIG',
    repair_table_name => 'REPAIR_TABLE',
    corrupt_count => :corrupt_count
  );
END;
/

# 4. If extensive corruption, restore from backup
# Restore database from last known good backup
RESTORE DATABASE FROM 'backup_location' UNTIL TIME '2025-08-13 09:00:00';

# 5. Re-apply Liquibase changes if needed
mvn liquibase:update
```

### Recovery Scenario 2: Application Failure Post-Deployment

**Symptoms**:
- Application startup failures
- API endpoint errors
- UI component crashes

**Recovery Steps**:

```bash
# 1. Quick rollback using pre-generated scripts
cd /path/to/fabric-api/rollback-scripts
sqlplus fabric_user@database_name @us001-emergency-rollback.sql

# 2. Verify application functionality
curl -H "Authorization: Bearer $JWT_TOKEN" \
     http://localhost:8080/api/v1/configurations/health

# 3. If issues persist, full environment rollback
# Stop services
docker-compose down

# Restore previous container versions
docker-compose -f docker-compose.rollback.yml up -d

# 4. Validate system recovery
./scripts/health-check.sh
```

### Recovery Scenario 3: Performance Degradation

**Symptoms**:
- Slow query performance
- Timeout errors
- High CPU/memory usage

**Recovery Steps**:

```bash
# 1. Immediate performance analysis
sqlplus fabric_user@database_name
SELECT sql_text, executions, cpu_time 
FROM v$sql 
WHERE sql_text LIKE '%MANUAL_JOB_%' 
ORDER BY cpu_time DESC;

# 2. Check for missing indexes
SELECT table_name, index_name, status 
FROM user_indexes 
WHERE table_name IN ('MANUAL_JOB_CONFIG', 'MANUAL_JOB_EXECUTION', 
                     'JOB_PARAMETER_TEMPLATES', 'MANUAL_JOB_AUDIT');

# 3. Rebuild statistics if needed
EXEC DBMS_STATS.GATHER_TABLE_STATS('FABRIC_CORE', 'MANUAL_JOB_CONFIG');
EXEC DBMS_STATS.GATHER_TABLE_STATS('FABRIC_CORE', 'MANUAL_JOB_EXECUTION');

# 4. If performance issues persist, consider rollback
mvn liquibase:rollback -Dliquibase.rollbackTag=PERFORMANCE_BASELINE
```

---

## Post-Rollback Validation

### Database Validation Checklist

- [ ] **Schema Consistency**
  - [ ] Verify all expected tables exist or are properly removed
  - [ ] Confirm constraint relationships are intact
  - [ ] Validate index performance is acceptable
  - [ ] Check trigger functionality

- [ ] **Data Integrity**
  - [ ] Run data consistency checks
  - [ ] Verify referential integrity
  - [ ] Confirm backup data matches expectations
  - [ ] Validate audit trail completeness

- [ ] **Application Testing**
  - [ ] Test critical application functionality
  - [ ] Verify API endpoint responses
  - [ ] Confirm UI component functionality
  - [ ] Validate integration points

### Validation Scripts

```sql
-- Post-rollback validation script
SET SERVEROUTPUT ON;

DECLARE
  v_error_count NUMBER := 0;
BEGIN
  -- Check table existence after rollback
  SELECT COUNT(*) INTO v_count
  FROM user_tables 
  WHERE table_name IN ('MANUAL_JOB_CONFIG', 'MANUAL_JOB_EXECUTION', 
                       'JOB_PARAMETER_TEMPLATES', 'MANUAL_JOB_AUDIT');
  
  -- Expected count depends on rollback scope
  -- Complete rollback: 0, Partial rollback: varies
  
  DBMS_OUTPUT.PUT_LINE('Tables found after rollback: ' || v_count);
  
  -- Check for orphaned data
  IF v_count > 0 THEN
    -- Run data integrity checks
    FOR rec IN (SELECT table_name FROM user_tables 
                WHERE table_name LIKE 'MANUAL_JOB_%') LOOP
      EXECUTE IMMEDIATE 'ANALYZE TABLE ' || rec.table_name || ' VALIDATE STRUCTURE';
    END LOOP;
  END IF;
  
  -- Check Liquibase state
  SELECT COUNT(*) INTO v_count
  FROM DATABASECHANGELOG 
  WHERE ID LIKE 'us001-%';
  
  DBMS_OUTPUT.PUT_LINE('US001 changesets remaining: ' || v_count);
  
  DBMS_OUTPUT.PUT_LINE('Post-rollback validation completed successfully');
  
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Validation error: ' || SQLERRM);
    RAISE;
END;
/
```

---

## Communication Procedures

### Rollback Communication Template

**Subject**: [URGENT] US001 Database Rollback - Action Required

**Recipients**:
- Principal Enterprise Architect
- Lending Product Owner
- Operations Team Lead
- Security Team
- Compliance Officer

**Template**:
```
ROLLBACK NOTIFICATION - US001 MANUAL JOB CONFIGURATION INTERFACE

Rollback Details:
- Initiated By: [Name and Role]
- Rollback Type: [Tag-based/Count-based/Date-based/Emergency]
- Target State: [Description of target state]
- Estimated Duration: [Time estimate]
- Business Impact: [Description of impact]

Reason for Rollback:
[Detailed explanation of issue requiring rollback]

Actions Taken:
1. [List of specific actions]
2. [Include timestamps]
3. [Reference ticket numbers]

Current Status:
[In Progress/Completed/Failed]

Next Steps:
[Immediate actions and timeline]

Contact Information:
Emergency Contact: [Phone number]
Incident ID: [Reference number]
```

### Status Update Template

```
ROLLBACK STATUS UPDATE - US001

Current Status: [Status]
Completion Percentage: [X%]
Time Remaining: [Estimate]

Completed Activities:
- [Activity 1] - [Timestamp]
- [Activity 2] - [Timestamp]

In Progress:
- [Current activity] - [Expected completion]

Issues Encountered:
[Any issues and their resolution status]

Next Update: [Time of next update]
```

---

## Risk Assessment and Mitigation

### High-Risk Scenarios

#### Risk 1: Data Loss During Rollback
**Probability**: Low  
**Impact**: Critical  
**Mitigation**:
- Mandatory backup verification before rollback
- Test rollback in non-production environment first
- Implement point-in-time recovery capabilities
- Maintain multiple backup generations

#### Risk 2: Extended Downtime
**Probability**: Medium  
**Impact**: High  
**Mitigation**:
- Pre-generate rollback scripts for faster execution
- Practice rollback procedures during maintenance windows
- Implement parallel processing where possible
- Prepare communication for extended outages

#### Risk 3: Incomplete Rollback
**Probability**: Medium  
**Impact**: Medium  
**Mitigation**:
- Use comprehensive validation scripts
- Implement rollback verification checkpoints
- Maintain detailed rollback logs
- Have manual cleanup procedures ready

#### Risk 4: Regulatory Compliance Issues
**Probability**: Low  
**Impact**: Critical  
**Mitigation**:
- Ensure all rollback activities are properly audited
- Maintain SOX compliance throughout rollback process
- Document all changes for regulatory review
- Notify compliance team immediately of any issues

### Contingency Planning

#### Plan A: Automated Rollback Success
- **Scenario**: Liquibase rollback completes successfully
- **Actions**: Standard validation and communication procedures
- **Timeline**: 30-60 minutes total

#### Plan B: Automated Rollback Failure
- **Scenario**: Liquibase rollback encounters errors
- **Actions**: Switch to manual rollback procedures
- **Timeline**: 60-120 minutes total

#### Plan C: Manual Rollback Required
- **Scenario**: Complete automation failure
- **Actions**: Execute emergency manual procedures
- **Timeline**: 120-240 minutes total

#### Plan D: Disaster Recovery
- **Scenario**: Database corruption or complete system failure
- **Actions**: Full system restore from backup
- **Timeline**: 4-8 hours total

---

## Conclusion

This document provides comprehensive procedures for safely rolling back US001 database changes while maintaining data integrity and regulatory compliance. Regular review and practice of these procedures is essential for maintaining operational readiness.

### Key Success Factors

1. **Preparation**: Proper backup and validation procedures
2. **Communication**: Clear stakeholder communication throughout process
3. **Documentation**: Detailed logging of all activities
4. **Testing**: Regular practice of rollback procedures
5. **Compliance**: Maintenance of SOX and regulatory requirements

### Review and Update Schedule

- **Monthly**: Review rollback procedures and update as needed
- **Quarterly**: Practice rollback procedures in non-production environment
- **Annually**: Comprehensive review with all stakeholders

---

**Document Status**: FOR ARCHITECT REVIEW  
**Next Review Date**: 2025-09-13  
**Emergency Contact**: [To be provided by Operations Team]