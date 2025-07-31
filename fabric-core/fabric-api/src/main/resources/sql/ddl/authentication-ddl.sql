-- ============================================================================
-- FABRIC-011 AUTHENTICATION & AUTHORIZATION SCHEMA
-- Enterprise-grade security with audit trails and compliance support
-- ============================================================================

-- User management table (integrates with LDAP but stores local attributes)
CREATE TABLE fabric_users (
    user_id VARCHAR2(50) PRIMARY KEY,
    username VARCHAR2(100) NOT NULL UNIQUE,
    email VARCHAR2(255) NOT NULL UNIQUE,
    first_name VARCHAR2(100) NOT NULL,
    last_name VARCHAR2(100) NOT NULL,
    department VARCHAR2(100),
    title VARCHAR2(100),
    phone VARCHAR2(20),
    ldap_dn VARCHAR2(500), -- LDAP Distinguished Name
    status VARCHAR2(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING')),
    password_last_changed TIMESTAMP,
    last_login_date TIMESTAMP,
    failed_login_attempts NUMBER(2) DEFAULT 0,
    account_locked_until TIMESTAMP,
    mfa_enabled VARCHAR2(1) DEFAULT 'N' CHECK (mfa_enabled IN ('Y', 'N')),
    mfa_secret VARCHAR2(100), -- Encrypted TOTP secret
    created_by VARCHAR2(50) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    modified_by VARCHAR2(50),
    modified_date TIMESTAMP,
    version NUMBER DEFAULT 1 NOT NULL
);

-- Hierarchical role definitions (5-tier structure)
CREATE TABLE fabric_roles (
    role_id VARCHAR2(50) PRIMARY KEY,
    role_name VARCHAR2(100) NOT NULL UNIQUE,
    role_description VARCHAR2(500),
    role_level NUMBER(1) CHECK (role_level BETWEEN 1 AND 5),
    parent_role_id VARCHAR2(50),
    permissions CLOB, -- JSON array of permissions
    is_system_role VARCHAR2(1) DEFAULT 'N' CHECK (is_system_role IN ('Y', 'N')),
    created_by VARCHAR2(50) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    modified_by VARCHAR2(50),
    modified_date TIMESTAMP,
    FOREIGN KEY (parent_role_id) REFERENCES fabric_roles(role_id)
);

-- User-Role assignments with temporal validity
CREATE TABLE fabric_user_roles (
    user_role_id VARCHAR2(50) PRIMARY KEY,
    user_id VARCHAR2(50) NOT NULL,
    role_id VARCHAR2(50) NOT NULL,
    assigned_by VARCHAR2(50) NOT NULL,
    assigned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    effective_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    effective_until TIMESTAMP,
    is_active VARCHAR2(1) DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
    assignment_reason VARCHAR2(500),
    FOREIGN KEY (user_id) REFERENCES fabric_users(user_id),
    FOREIGN KEY (role_id) REFERENCES fabric_roles(role_id),
    UNIQUE(user_id, role_id, effective_from)
);

-- Permission definitions (granular resource access control)
CREATE TABLE fabric_permissions (
    permission_id VARCHAR2(50) PRIMARY KEY,
    permission_name VARCHAR2(100) NOT NULL UNIQUE,
    resource_type VARCHAR2(50) NOT NULL, -- BATCH_CONFIG, TEMPLATE, SYSTEM, etc.
    action VARCHAR2(50) NOT NULL, -- CREATE, READ, UPDATE, DELETE, EXECUTE
    resource_pattern VARCHAR2(200), -- Pattern for resource matching
    description VARCHAR2(500),
    is_system_permission VARCHAR2(1) DEFAULT 'N' CHECK (is_system_permission IN ('Y', 'N')),
    created_by VARCHAR2(50) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Role-Permission mappings
CREATE TABLE fabric_role_permissions (
    role_permission_id VARCHAR2(50) PRIMARY KEY,
    role_id VARCHAR2(50) NOT NULL,
    permission_id VARCHAR2(50) NOT NULL,
    granted_by VARCHAR2(50) NOT NULL,
    granted_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (role_id) REFERENCES fabric_roles(role_id),
    FOREIGN KEY (permission_id) REFERENCES fabric_permissions(permission_id),
    UNIQUE(role_id, permission_id)
);

-- JWT token blacklist (for logout and security incidents)
CREATE TABLE fabric_token_blacklist (
    token_id VARCHAR2(100) PRIMARY KEY,
    user_id VARCHAR2(50) NOT NULL,
    token_hash VARCHAR2(256) NOT NULL, -- SHA-256 hash of token
    token_type VARCHAR2(20) CHECK (token_type IN ('ACCESS', 'REFRESH')),
    blacklisted_by VARCHAR2(50) NOT NULL,
    blacklisted_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    reason VARCHAR2(200),
    FOREIGN KEY (user_id) REFERENCES fabric_users(user_id)
);

-- User sessions with device fingerprinting
CREATE TABLE fabric_user_sessions (
    session_id VARCHAR2(100) PRIMARY KEY,
    user_id VARCHAR2(50) NOT NULL,
    correlation_id VARCHAR2(100) NOT NULL, -- For distributed tracing
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ip_address VARCHAR2(45), -- Support IPv6
    user_agent VARCHAR2(1000),
    device_fingerprint VARCHAR2(500),
    location_info VARCHAR2(200), -- City, Country from IP geolocation
    session_status VARCHAR2(20) DEFAULT 'ACTIVE' CHECK (session_status IN ('ACTIVE', 'EXPIRED', 'TERMINATED')),
    logout_time TIMESTAMP,
    logout_reason VARCHAR2(100),
    risk_score NUMBER(3,2) DEFAULT 0.00, -- 0.00 to 99.99
    mfa_verified VARCHAR2(1) DEFAULT 'N' CHECK (mfa_verified IN ('Y', 'N')),
    FOREIGN KEY (user_id) REFERENCES fabric_users(user_id)
);

-- Comprehensive security audit log
CREATE TABLE fabric_security_audit (
    audit_id NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    correlation_id VARCHAR2(100) NOT NULL,
    event_type VARCHAR2(50) NOT NULL, -- LOGIN, LOGOUT, ACCESS_DENIED, TOKEN_REFRESH, etc.
    user_id VARCHAR2(50),
    username VARCHAR2(100),
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ip_address VARCHAR2(45),
    user_agent VARCHAR2(1000),
    resource_accessed VARCHAR2(200),
    action_attempted VARCHAR2(100),
    result VARCHAR2(20) CHECK (result IN ('SUCCESS', 'FAILURE', 'BLOCKED')),
    failure_reason VARCHAR2(500),
    risk_indicators CLOB, -- JSON with risk factors
    session_id VARCHAR2(100),
    additional_data CLOB, -- JSON for extensibility
    severity VARCHAR2(10) CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Risk assessment rules (for adaptive authentication)
CREATE TABLE fabric_risk_rules (
    rule_id VARCHAR2(50) PRIMARY KEY,
    rule_name VARCHAR2(100) NOT NULL,
    rule_description VARCHAR2(500),
    rule_condition CLOB NOT NULL, -- JSON expression for rule evaluation
    risk_score NUMBER(3,2) NOT NULL, -- Score to add if rule matches
    action_required VARCHAR2(50), -- MFA, BLOCK, LOG, etc.
    is_active VARCHAR2(1) DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
    created_by VARCHAR2(50) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    modified_by VARCHAR2(50),
    modified_date TIMESTAMP
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- User management indexes
CREATE INDEX idx_fabric_users_username ON fabric_users(username);
CREATE INDEX idx_fabric_users_email ON fabric_users(email);
CREATE INDEX idx_fabric_users_status ON fabric_users(status);
CREATE INDEX idx_fabric_users_last_login ON fabric_users(last_login_date);

-- Role and permission indexes
CREATE INDEX idx_fabric_user_roles_user ON fabric_user_roles(user_id);
CREATE INDEX idx_fabric_user_roles_role ON fabric_user_roles(role_id);
CREATE INDEX idx_fabric_user_roles_active ON fabric_user_roles(is_active, effective_from, effective_until);
CREATE INDEX idx_fabric_role_permissions_role ON fabric_role_permissions(role_id);
CREATE INDEX idx_fabric_permissions_resource ON fabric_permissions(resource_type, action);

-- Security indexes
CREATE INDEX idx_fabric_sessions_user ON fabric_user_sessions(user_id);
CREATE INDEX idx_fabric_sessions_correlation ON fabric_user_sessions(correlation_id);
CREATE INDEX idx_fabric_sessions_status ON fabric_user_sessions(session_status);
CREATE INDEX idx_fabric_sessions_activity ON fabric_user_sessions(last_activity);
CREATE INDEX idx_fabric_audit_user ON fabric_security_audit(user_id);
CREATE INDEX idx_fabric_audit_correlation ON fabric_security_audit(correlation_id);
CREATE INDEX idx_fabric_audit_timestamp ON fabric_security_audit(event_timestamp);
CREATE INDEX idx_fabric_audit_event_type ON fabric_security_audit(event_type);
CREATE INDEX idx_fabric_blacklist_user ON fabric_token_blacklist(user_id);
CREATE INDEX idx_fabric_blacklist_expires ON fabric_token_blacklist(expires_at);

-- ============================================================================
-- INITIAL DATA SETUP
-- ============================================================================

-- System roles (hierarchical: Admin > Manager > Analyst > Operator > Viewer)
INSERT INTO fabric_roles (role_id, role_name, role_description, role_level, parent_role_id, is_system_role, created_by) VALUES
('ADMIN', 'System Administrator', 'Full system access with user management capabilities', 1, NULL, 'Y', 'SYSTEM');

INSERT INTO fabric_roles (role_id, role_name, role_description, role_level, parent_role_id, is_system_role, created_by) VALUES
('MANAGER', 'Business Manager', 'Manage configurations and approve changes', 2, 'ADMIN', 'Y', 'SYSTEM');

INSERT INTO fabric_roles (role_id, role_name, role_description, role_level, parent_role_id, is_system_role, created_by) VALUES
('ANALYST', 'Business Analyst', 'Create and modify batch configurations', 3, 'MANAGER', 'Y', 'SYSTEM');

INSERT INTO fabric_roles (role_id, role_name, role_description, role_level, parent_role_id, is_system_role, created_by) VALUES
('OPERATOR', 'System Operator', 'Execute batch jobs and monitor operations', 4, 'ANALYST', 'Y', 'SYSTEM');

INSERT INTO fabric_roles (role_id, role_name, role_description, role_level, parent_role_id, is_system_role, created_by) VALUES
('VIEWER', 'Read-Only User', 'View configurations and job status only', 5, 'OPERATOR', 'Y', 'SYSTEM');

-- Core permissions
INSERT INTO fabric_permissions (permission_id, permission_name, resource_type, action, resource_pattern, description, is_system_permission, created_by) VALUES
('USER_MGMT_ALL', 'User Management', 'USER', 'ALL', '*', 'Complete user management access', 'Y', 'SYSTEM'),
('CONFIG_CREATE', 'Create Configurations', 'BATCH_CONFIG', 'CREATE', '*', 'Create new batch configurations', 'Y', 'SYSTEM'),
('CONFIG_READ', 'Read Configurations', 'BATCH_CONFIG', 'READ', '*', 'View batch configurations', 'Y', 'SYSTEM'),
('CONFIG_UPDATE', 'Update Configurations', 'BATCH_CONFIG', 'UPDATE', '*', 'Modify existing configurations', 'Y', 'SYSTEM'),
('CONFIG_DELETE', 'Delete Configurations', 'BATCH_CONFIG', 'DELETE', '*', 'Remove configurations', 'Y', 'SYSTEM'),
('JOB_EXECUTE', 'Execute Jobs', 'BATCH_JOB', 'EXECUTE', '*', 'Run batch jobs', 'Y', 'SYSTEM'),
('JOB_MONITOR', 'Monitor Jobs', 'BATCH_JOB', 'READ', '*', 'View job execution status', 'Y', 'SYSTEM'),
('TEMPLATE_MANAGE', 'Manage Templates', 'TEMPLATE', 'ALL', '*', 'Full template management', 'Y', 'SYSTEM'),
('SYSTEM_CONFIG', 'System Configuration', 'SYSTEM', 'ALL', '*', 'System-level configuration access', 'Y', 'SYSTEM'),
('AUDIT_VIEW', 'View Audit Logs', 'AUDIT', 'READ', '*', 'Access audit and security logs', 'Y', 'SYSTEM');

-- Role-Permission mappings (hierarchical inheritance)
-- ADMIN: All permissions
INSERT INTO fabric_role_permissions (role_permission_id, role_id, permission_id, granted_by) VALUES
('ADMIN_USER_MGMT', 'ADMIN', 'USER_MGMT_ALL', 'SYSTEM'),
('ADMIN_CONFIG_CREATE', 'ADMIN', 'CONFIG_CREATE', 'SYSTEM'),
('ADMIN_CONFIG_READ', 'ADMIN', 'CONFIG_READ', 'SYSTEM'),
('ADMIN_CONFIG_UPDATE', 'ADMIN', 'CONFIG_UPDATE', 'SYSTEM'),
('ADMIN_CONFIG_DELETE', 'ADMIN', 'CONFIG_DELETE', 'SYSTEM'),
('ADMIN_JOB_EXECUTE', 'ADMIN', 'JOB_EXECUTE', 'SYSTEM'),
('ADMIN_JOB_MONITOR', 'ADMIN', 'JOB_MONITOR', 'SYSTEM'),
('ADMIN_TEMPLATE_MANAGE', 'ADMIN', 'TEMPLATE_MANAGE', 'SYSTEM'),
('ADMIN_SYSTEM_CONFIG', 'ADMIN', 'SYSTEM_CONFIG', 'SYSTEM'),
('ADMIN_AUDIT_VIEW', 'ADMIN', 'AUDIT_VIEW', 'SYSTEM');

-- MANAGER: Configuration management and job execution
INSERT INTO fabric_role_permissions (role_permission_id, role_id, permission_id, granted_by) VALUES
('MGR_CONFIG_CREATE', 'MANAGER', 'CONFIG_CREATE', 'SYSTEM'),
('MGR_CONFIG_READ', 'MANAGER', 'CONFIG_READ', 'SYSTEM'),
('MGR_CONFIG_UPDATE', 'MANAGER', 'CONFIG_UPDATE', 'SYSTEM'),
('MGR_JOB_EXECUTE', 'MANAGER', 'JOB_EXECUTE', 'SYSTEM'),
('MGR_JOB_MONITOR', 'MANAGER', 'JOB_MONITOR', 'SYSTEM'),
('MGR_TEMPLATE_MANAGE', 'MANAGER', 'TEMPLATE_MANAGE', 'SYSTEM'),
('MGR_AUDIT_VIEW', 'MANAGER', 'AUDIT_VIEW', 'SYSTEM');

-- ANALYST: Configuration creation and modification
INSERT INTO fabric_role_permissions (role_permission_id, role_id, permission_id, granted_by) VALUES
('ANALYST_CONFIG_CREATE', 'ANALYST', 'CONFIG_CREATE', 'SYSTEM'),
('ANALYST_CONFIG_READ', 'ANALYST', 'CONFIG_READ', 'SYSTEM'),
('ANALYST_CONFIG_UPDATE', 'ANALYST', 'CONFIG_UPDATE', 'SYSTEM'),
('ANALYST_JOB_MONITOR', 'ANALYST', 'JOB_MONITOR', 'SYSTEM'),
('ANALYST_TEMPLATE_MANAGE', 'ANALYST', 'TEMPLATE_MANAGE', 'SYSTEM');

-- OPERATOR: Job execution and monitoring
INSERT INTO fabric_role_permissions (role_permission_id, role_id, permission_id, granted_by) VALUES
('OP_CONFIG_READ', 'OPERATOR', 'CONFIG_READ', 'SYSTEM'),
('OP_JOB_EXECUTE', 'OPERATOR', 'JOB_EXECUTE', 'SYSTEM'),
('OP_JOB_MONITOR', 'OPERATOR', 'JOB_MONITOR', 'SYSTEM');

-- VIEWER: Read-only access
INSERT INTO fabric_role_permissions (role_permission_id, role_id, permission_id, granted_by) VALUES
('VIEWER_CONFIG_READ', 'VIEWER', 'CONFIG_READ', 'SYSTEM'),
('VIEWER_JOB_MONITOR', 'VIEWER', 'JOB_MONITOR', 'SYSTEM');

-- Sample risk rules
INSERT INTO fabric_risk_rules (rule_id, rule_name, rule_description, rule_condition, risk_score, action_required, created_by) VALUES
('RULE_001', 'New Device Access', 'First time device access increases risk', '{"condition": "new_device", "threshold": true}', 25.00, 'MFA', 'SYSTEM'),
('RULE_002', 'Off-Hours Access', 'Access outside business hours', '{"condition": "time_range", "start": "18:00", "end": "08:00"}', 15.00, 'LOG', 'SYSTEM'),
('RULE_003', 'Geolocation Anomaly', 'Access from unusual location', '{"condition": "location_change", "threshold_miles": 100}', 35.00, 'MFA', 'SYSTEM'),
('RULE_004', 'Multiple Failed Logins', 'Repeated login failures', '{"condition": "failed_attempts", "threshold": 3, "window_minutes": 15}', 50.00, 'BLOCK', 'SYSTEM'),
('RULE_005', 'Privileged Account Access', 'Admin or Manager role access', '{"condition": "role_level", "threshold": 2}', 20.00, 'MFA', 'SYSTEM');

COMMIT;

-- ============================================================================
-- VIEWS FOR COMMON QUERIES
-- ============================================================================

-- Active user permissions (flattened view)
CREATE OR REPLACE VIEW fabric_user_permissions AS
SELECT DISTINCT 
    u.user_id,
    u.username,
    u.email,
    r.role_id,
    r.role_name,
    r.role_level,
    p.permission_id,
    p.permission_name,
    p.resource_type,
    p.action,
    p.resource_pattern
FROM fabric_users u
JOIN fabric_user_roles ur ON u.user_id = ur.user_id
JOIN fabric_roles r ON ur.role_id = r.role_id
JOIN fabric_role_permissions rp ON r.role_id = rp.role_id
JOIN fabric_permissions p ON rp.permission_id = p.permission_id
WHERE u.status = 'ACTIVE'
  AND ur.is_active = 'Y'
  AND (ur.effective_until IS NULL OR ur.effective_until > SYSDATE)
  AND ur.effective_from <= SYSDATE;

-- User session summary
CREATE OR REPLACE VIEW fabric_active_sessions AS
SELECT 
    s.session_id,
    s.user_id,
    u.username,
    u.email,
    s.correlation_id,
    s.login_time,
    s.last_activity,
    s.ip_address,
    s.device_fingerprint,
    s.risk_score,
    s.mfa_verified,
    ROUND((SYSDATE - s.last_activity) * 24 * 60, 2) AS idle_minutes
FROM fabric_user_sessions s
JOIN fabric_users u ON s.user_id = u.user_id
WHERE s.session_status = 'ACTIVE'
  AND s.last_activity > SYSDATE - INTERVAL '8' HOUR;