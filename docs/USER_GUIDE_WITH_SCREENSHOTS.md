# 📚 Fabric Platform - Comprehensive User Guide

**Complete User Manual for SQL*Loader Configuration Management**

---

## 📋 Table of Contents

1. [Getting Started](#getting-started)
2. [Platform Overview](#platform-overview)  
3. [SQL*Loader Configuration Walkthrough](#sqlloader-configuration-walkthrough)
4. [Advanced Features](#advanced-features)
5. [Troubleshooting](#troubleshooting)
6. [Best Practices](#best-practices)
7. [FAQ](#faq)

---

## 🚀 Getting Started

### Prerequisites Checklist

Before using the Fabric Platform, ensure you have:

- [ ] **Access Credentials**: Valid user account with appropriate permissions
- [ ] **Browser Compatibility**: Chrome 90+, Firefox 88+, Safari 14+, Edge 90+
- [ ] **Network Access**: Connection to the platform URL
- [ ] **Oracle Database**: Access to source and target database systems

### First-Time Login

1. **Navigate to Platform URL**
   ```
   Production: https://fabric-platform.company.com
   Development: http://localhost:3000
   ```

2. **Login Process**
   - Enter your corporate username and password
   - Complete multi-factor authentication if prompted
   - Click "Sign In" to access the platform

---

## 🏛️ Platform Overview

### Main Navigation

The Fabric Platform consists of several key sections:

#### 📊 Dashboard
- **Real-time Metrics**: View current system status and job execution statistics
- **Recent Activity**: Track your recent configuration changes and job executions
- **Quick Actions**: Access frequently used features

#### ⚙️ Configuration Management
- **Template Configuration**: Manage field mapping templates
- **SQL*Loader Configuration**: Configure SQL*Loader settings (NEW)
- **Source System Management**: Manage source system definitions

#### 📈 Monitoring
- **Job Execution**: Monitor batch job status and performance
- **System Health**: View platform health and performance metrics
- **Audit Logs**: Access comprehensive audit trails

#### 👥 Administration
- **User Management**: Manage user accounts and permissions
- **System Settings**: Configure platform-wide settings
- **Security**: Manage security policies and access controls

---

## 🔧 SQL*Loader Configuration Walkthrough

### Overview of SQL*Loader Features

The SQL*Loader configuration module provides a comprehensive interface for:
- **Dynamic Control File Generation**: Automatically generate Oracle SQL*Loader control files
- **Field-Level Mapping**: Configure data transformations and validation rules
- **Performance Optimization**: Configure parallel processing and memory settings
- **Security & Compliance**: Handle PII data classification and compliance requirements

### Step-by-Step Configuration Process

#### Step 1: Access SQL*Loader Configuration

1. **Navigate to Configuration Page**
   - Click "Configuration" in the main navigation
   - Select "SQL*Loader Configuration" tab

**Screenshot Location**: *[Screenshot of main navigation and SQL*Loader tab would be inserted here]*

#### Step 2: Create New Configuration

2. **Start New Configuration**
   - Click the "New Configuration" button
   - The 3-tab interface will open:
     - **Basic Settings**
     - **Column Mapping**  
     - **Control Options**

**Screenshot Location**: *[Screenshot of new configuration dialog would be inserted here]*

#### Step 3: Basic Settings Configuration

3. **Configure Basic Settings**

Fill out the following required fields:

**Configuration Details:**
- **Configuration Name**: Descriptive name (e.g., "HR Employee Data Load")
- **Source System**: Select from dropdown (e.g., "HR", "Finance", "Operations")
- **Target Table**: Oracle table name (e.g., "EMPLOYEE_STAGING")

**Load Options:**
- **Load Method**: 
  - `INSERT` - Insert new records only
  - `APPEND` - Add to existing data  
  - `REPLACE` - Replace all data
  - `TRUNCATE` - Truncate table before load
- **Direct Path**: Enable for high-performance loading
- **Parallel Degree**: Number of parallel processes (1-16)

**Example Configuration:**
```
Configuration Name: Employee Monthly Load
Source System: HR
Target Table: EMPLOYEE_STAGING
Load Method: TRUNCATE
Direct Path: ✓ Enabled
Parallel Degree: 4
```

**Screenshot Location**: *[Screenshot of basic settings form would be inserted here]*

#### Step 4: Column Mapping Configuration

4. **Configure Column Mappings**

This is where you map source file columns to target database columns:

**Left Panel: Source Columns**
- View available columns from the source file
- Drag columns to map them to target fields

**Right Panel: Target Columns**
- Shows target table structure
- Drop source columns onto target fields

**Column Configuration Options:**
- **Data Type**: Oracle data type (VARCHAR2, NUMBER, DATE, etc.)
- **Field Length**: Maximum field length
- **Format**: Date formats, number formats
- **Nullable**: Allow null values
- **Default Value**: Default if source is empty
- **Validation Rules**: Custom validation expressions

**Drag-and-Drop Interface:**
1. **Drag** a source column from the left panel
2. **Drop** it onto a target column in the right panel  
3. **Configure** the field properties in the popup dialog
4. **Save** the mapping

**Screenshot Location**: *[Screenshot of drag-and-drop column mapping would be inserted here]*

**Example Column Mapping:**
```
Source: employee_id (Text) → Target: EMPLOYEE_ID (NUMBER)
Source: first_name (Text) → Target: FIRST_NAME (VARCHAR2(50))
Source: hire_date (Text) → Target: HIRE_DATE (DATE "YYYY-MM-DD")
Source: salary (Text) → Target: SALARY (NUMBER(10,2))
```

#### Step 5: Control Options Configuration

5. **Configure Control File Options**

Advanced options for SQL*Loader control file generation:

**File Format Options:**
- **Field Delimiter**: Character separating fields (default: `|`)
- **Record Delimiter**: Character separating records (default: newline)
- **String Delimiter**: Character enclosing text fields (default: `"`)
- **Skip Rows**: Number of header rows to skip (default: 1)

**Error Handling:**
- **Max Errors**: Maximum errors before stopping (default: 1000)
- **Continue Load**: Continue on non-fatal errors
- **Error File**: Generate .bad file for rejected records

**Performance Tuning:**
- **Bind Size**: Memory for bind arrays (default: 256KB)
- **Read Size**: Buffer size for reading (default: 1MB)
- **Rows Per Commit**: Commit frequency (default: 64)

**Security Options:**
- **Character Set**: File character encoding (default: UTF8)
- **PII Classification**: Mark fields containing personal information
- **Encryption**: Enable field-level encryption for sensitive data

**Screenshot Location**: *[Screenshot of control options form would be inserted here]*

#### Step 6: Preview and Validate

6. **Preview Generated Control File**

Before saving, you can preview the generated SQL*Loader control file:

- Click "Preview Control File" button
- Review the generated control file syntax
- Validate configuration completeness
- Check for any warnings or errors

**Example Generated Control File:**
```sql
-- Generated by Fabric Platform SQL*Loader Module
-- Configuration: Employee Monthly Load
-- Generated on: 2025-08-07 14:30:00

LOAD DATA
INFILE 'employee_data.dat'
INTO TABLE EMPLOYEE_STAGING
FIELDS TERMINATED BY '|' 
OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(
    EMPLOYEE_ID INTEGER EXTERNAL,
    FIRST_NAME CHAR(50),
    LAST_NAME CHAR(50),
    EMAIL CHAR(100),
    HIRE_DATE DATE "YYYY-MM-DD",
    DEPARTMENT CHAR(30),
    SALARY DECIMAL EXTERNAL
)
```

**Screenshot Location**: *[Screenshot of control file preview dialog would be inserted here]*

#### Step 7: Save and Test

7. **Save Configuration**

- Click "Save Configuration" to store in the database
- The configuration receives a unique ID for tracking
- Audit trail records all configuration changes

8. **Test Configuration** (Optional)

- Click "Test Configuration" to validate settings
- System performs:
  - Syntax validation of control file
  - Database connectivity test
  - Table structure verification
  - Permission validation

**Screenshot Location**: *[Screenshot of save confirmation and test results would be inserted here]*

### Configuration Management

#### Viewing Existing Configurations

**Configuration List View:**
- View all saved SQL*Loader configurations  
- Search and filter by name, source system, date
- Sort by last modified, creation date, status

**Configuration Details:**
- Click any configuration to view/edit details
- See configuration history and audit trail
- Export configuration as JSON or XML

**Screenshot Location**: *[Screenshot of configuration list and details view would be inserted here]*

#### Editing Configurations

**Edit Process:**
1. Select configuration from list
2. Click "Edit" button
3. Make changes in the 3-tab interface
4. Save changes (creates new version)
5. Old version preserved for audit trail

#### Copying Configurations

**Copy Process:**
1. Select existing configuration
2. Click "Copy" button
3. Modify copied configuration as needed
4. Save with new name

---

## 🚀 Advanced Features

### Security and Compliance

#### PII Data Classification

The platform provides 5-level PII classification:

1. **PUBLIC**: No restrictions (e.g., company name, job title)
2. **INTERNAL**: Internal use only (e.g., employee ID, department)  
3. **CONFIDENTIAL**: Restricted access (e.g., salary, performance ratings)
4. **RESTRICTED**: Highly sensitive (e.g., SSN, bank account)
5. **PII_SENSITIVE**: Personal information requiring encryption

**Configuring PII Classification:**
1. In Column Mapping, select a field
2. Choose appropriate PII classification level
3. System automatically applies security controls
4. Audit trail tracks all access to sensitive fields

#### Compliance Framework

**Supported Compliance Standards:**
- **SOX (Sarbanes-Oxley)**: Financial data controls
- **PCI-DSS**: Payment card industry standards  
- **GDPR**: European privacy regulation
- **Basel III**: Banking regulatory framework

**Compliance Features:**
- Automatic compliance validation
- Required approval workflows for sensitive data
- Comprehensive audit trails
- Data retention policies
- Right to deletion support

### Performance Optimization

#### Monitoring Performance

**Performance Metrics Available:**
- Configuration creation time
- Control file generation time  
- Validation processing time
- Memory usage optimization
- Throughput analysis

**Performance Dashboard:**
- Real-time performance metrics
- Historical performance trends
- Optimization recommendations
- Resource utilization graphs

#### Tuning Recommendations

The system provides automatic tuning recommendations:

**Memory Optimization:**
- Optimal bind size based on data volume
- Read buffer size recommendations
- Parallel degree suggestions

**Processing Optimization:**
- Direct path loading recommendations
- Partition-wise loading suggestions
- Index optimization advice

### Integration Features

#### REST API Access

All SQL*Loader functionality is available via REST APIs:

**Configuration Management APIs:**
```bash
GET /api/v1/sql-loader/configurations
POST /api/v1/sql-loader/configurations
PUT /api/v1/sql-loader/configurations/{id}
DELETE /api/v1/sql-loader/configurations/{id}
```

**Control File Generation APIs:**
```bash
POST /api/v1/sql-loader/configurations/{id}/control-file
GET /api/v1/sql-loader/configurations/{id}/preview
```

**Validation APIs:**
```bash
POST /api/v1/sql-loader/configurations/{id}/validate
GET /api/v1/sql-loader/configurations/{id}/compliance
```

#### Export/Import

**Export Configurations:**
- Export single or multiple configurations
- Formats: JSON, XML, YAML
- Include or exclude sensitive data
- Package with dependencies

**Import Configurations:**
- Import from exported files
- Validate before import
- Conflict resolution options
- Bulk import support

---

## 🔍 Troubleshooting

### Common Issues and Solutions

#### Configuration Validation Errors

**Issue**: "Invalid target table name"
- **Cause**: Table doesn't exist or no access permissions
- **Solution**: 
  1. Verify table exists in target schema
  2. Check database permissions
  3. Contact DBA if needed

**Issue**: "Column mapping validation failed"  
- **Cause**: Source and target data types incompatible
- **Solution**:
  1. Review column data type mappings
  2. Add appropriate format masks
  3. Use data transformation functions

**Issue**: "Control file generation failed"
- **Cause**: Invalid configuration parameters
- **Solution**:
  1. Use "Validate Configuration" function
  2. Check all required fields are completed
  3. Verify field delimiter settings

#### Performance Issues

**Issue**: "Configuration loading slowly"
- **Cause**: Large number of columns or complex mappings
- **Solution**:
  1. Reduce parallel degree temporarily
  2. Simplify column mapping rules
  3. Contact administrator for system optimization

**Issue**: "Control file preview timeout"
- **Cause**: Complex configuration taking too long to generate
- **Solution**:
  1. Save configuration first
  2. Use API to generate control file asynchronously
  3. Check system resource usage

#### Access and Permission Issues

**Issue**: "Access denied to SQL*Loader configuration"
- **Cause**: Insufficient user permissions
- **Solution**:
  1. Contact system administrator
  2. Request SQL*Loader configuration role
  3. Verify LDAP group membership

**Issue**: "Cannot save configuration"
- **Cause**: Database permissions or quota issues
- **Solution**:
  1. Check database connectivity
  2. Verify write permissions to configuration tables
  3. Contact DBA for quota increase if needed

### Getting Help

#### Self-Service Resources

1. **Built-in Help**: Click "?" icons throughout the interface
2. **Documentation**: Access comprehensive technical documentation  
3. **API Reference**: Swagger UI at `/api/swagger-ui.html`
4. **Community**: Internal knowledge base and forums

#### Support Contacts

- **Level 1 Support**: fabric-platform-support@company.com
- **Technical Issues**: fabric-platform-tech@company.com  
- **Security Issues**: fabric-platform-security@company.com
- **Emergency**: 24/7 support hotline: 1-800-FABRIC-1

---

## ✅ Best Practices

### Configuration Management

#### Naming Conventions

**Configuration Names:**
- Use descriptive names: `HR_Employee_Monthly_Load`
- Include frequency: `Daily`, `Weekly`, `Monthly`
- Include data type: `Employee`, `Transaction`, `Customer`
- Avoid special characters and spaces

**Field Names:**
- Match source system naming where possible
- Use consistent case (prefer UPPER_CASE for Oracle)
- Document any transformations in comments

#### Version Control

**Configuration Versioning:**
- Always test configurations in development first
- Document changes in version comments
- Keep previous versions for rollback capability
- Use approval workflow for production changes

#### Security Best Practices

**PII Data Handling:**
- Always classify PII fields appropriately
- Use encryption for sensitive data
- Implement approval workflows for PII access
- Regular audit access to sensitive configurations

**Access Management:**
- Follow principle of least privilege
- Regular review of user permissions
- Use service accounts for automated processes
- Log and monitor all administrative actions

### Performance Optimization

#### Configuration Design

**Column Mapping:**
- Map only required fields to improve performance
- Use appropriate data types to minimize conversion
- Consider using direct path loading for large datasets
- Implement field validation to catch errors early

**Resource Management:**
- Set appropriate parallel degree based on system capacity
- Monitor memory usage during large loads
- Use appropriate bind sizes for your data volume
- Schedule large loads during off-peak hours

#### Monitoring and Maintenance

**Regular Monitoring:**
- Review performance metrics weekly
- Monitor error rates and failure patterns
- Track resource utilization trends  
- Validate data quality metrics

**Preventive Maintenance:**
- Regular backup of configuration database
- Archive old audit logs according to retention policy
- Update configurations when source schemas change
- Test disaster recovery procedures quarterly

---

## ❓ Frequently Asked Questions

### General Questions

**Q: What is SQL*Loader and why use this platform?**
A: SQL*Loader is Oracle's bulk data loading utility. This platform provides a user-friendly interface to configure SQL*Loader without writing complex control files manually. It includes enterprise features like security, compliance, and performance optimization.

**Q: Can I use this platform with non-Oracle databases?**  
A: Currently, the SQL*Loader module is specifically for Oracle databases. However, the platform supports other databases through different modules like Spring Batch processing.

**Q: Is there a limit to the number of configurations I can create?**
A: There are no built-in limits on configuration count. Limits depend on database storage capacity and organizational policies.

### Technical Questions

**Q: How do I handle large files with millions of records?**
A: Use direct path loading with appropriate parallel degree. Configure larger bind sizes and enable resumable operations. Consider partitioning large files if needed.

**Q: Can I schedule SQL*Loader jobs automatically?**
A: Configuration management is available now. Job scheduling and execution will be available in Phase 2 of the platform roadmap.

**Q: How do I handle errors during data loading?**
A: Configure appropriate error thresholds and enable generation of .bad files for rejected records. Use the error analysis features to identify and resolve data quality issues.

**Q: Can I integrate this with our existing ETL processes?**
A: Yes, the platform provides REST APIs for integration. You can programmatically create configurations, generate control files, and monitor job status.

### Security Questions

**Q: How is sensitive data protected?**
A: The platform provides PII classification, field-level encryption, role-based access control, and comprehensive audit trails. All security controls are configurable based on your data classification.

**Q: Are configuration changes audited?**
A: Yes, all configuration changes are fully audited with user identification, timestamps, change details, and approval workflows where required.

**Q: Can I control who accesses certain configurations?**
A: Yes, the platform supports role-based access control integrated with LDAP/Active Directory. You can control access at configuration, field, and operation levels.

### Troubleshooting Questions

**Q: Why is my control file generation failing?**
A: Common causes include incomplete field mappings, invalid table names, or permission issues. Use the built-in validation function to identify specific problems.

**Q: How do I optimize performance for large configurations?**
A: Use appropriate parallel degrees, enable direct path loading, optimize bind sizes, and consider breaking large configurations into smaller, more manageable pieces.

**Q: What should I do if the platform is unavailable?**  
A: Contact support immediately. For critical loads, you can manually generate control files using exported configurations as reference.

---

## 📞 Support Information

### Documentation Resources

- **Technical Documentation**: `SQL_LOADER_IMPLEMENTATION.md`
- **API Reference**: Available at `/api/swagger-ui.html`
- **Testing Guide**: `SQL_LOADER_PHASE_1_4_TEST_DOCUMENTATION.md`
- **Administrator Guide**: `docs/ADMINISTRATOR_GUIDE.md` (coming soon)

### Contact Information

**Business Hours Support** (8 AM - 6 PM EST):
- **General Questions**: fabric-platform-support@company.com
- **Technical Issues**: fabric-platform-tech@company.com
- **Phone Support**: 1-800-FABRIC-1

**24/7 Emergency Support**:
- **Critical System Issues**: fabric-platform-emergency@company.com
- **Emergency Hotline**: 1-800-FABRIC-911

**Escalation Path**:
1. **Level 1**: General support team
2. **Level 2**: Technical specialists  
3. **Level 3**: Development team
4. **Level 4**: Architecture team

---

**Last Updated**: August 2025  
**Version**: 1.0  
**Document Status**: Complete with Phase 1 features

*This user guide will be updated with screenshots and additional Phase 2 features as they become available.*

**© 2025 Truist Financial Corporation. All rights reserved.**