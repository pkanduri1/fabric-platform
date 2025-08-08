-- Migration Script: V2024.002__add_consumer_default_template_fields.sql
-- Purpose: Add consumer default template support fields to field_templates table
-- Author: Claude Code - Senior Full Stack Developer
-- Date: 2025-08-02

-- Add consumer default template enhancement columns
ALTER TABLE field_templates ADD (
    template_category VARCHAR2(30) DEFAULT 'GENERAL' NOT NULL,
    pii_classification VARCHAR2(20) DEFAULT 'NONE',
    encryption_required CHAR(1) DEFAULT 'N' CHECK (encryption_required IN ('Y', 'N')),
    consumer_default_rules CLOB,
    risk_level VARCHAR2(10) DEFAULT 'LOW' CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Add comments for documentation
COMMENT ON COLUMN field_templates.template_category IS 'Template category: GENERAL, CONSUMER_DEFAULT, MORTGAGE_APPLICATION, PERSONAL_LOAN, CREDIT_CARD_APPLICATION, DEFAULT_MANAGEMENT';
COMMENT ON COLUMN field_templates.pii_classification IS 'PII classification level: NONE, LOW, MEDIUM, HIGH for data protection';
COMMENT ON COLUMN field_templates.encryption_required IS 'Y/N flag indicating if field requires encryption at rest';
COMMENT ON COLUMN field_templates.consumer_default_rules IS 'JSON configuration for consumer-specific validation rules';
COMMENT ON COLUMN field_templates.risk_level IS 'Risk assessment level for the field: LOW, MEDIUM, HIGH, CRITICAL';

-- Create indexes for performance optimization
CREATE INDEX idx_field_templates_category ON field_templates(template_category, enabled);
CREATE INDEX idx_field_templates_pii ON field_templates(pii_classification, encryption_required);
CREATE INDEX idx_field_templates_consumer ON field_templates(template_category, file_type, transaction_type) WHERE template_category LIKE 'CONSUMER%';

-- Update existing records to have proper template category
UPDATE field_templates 
SET template_category = 'CONSUMER_DEFAULT'
WHERE file_type IN ('p327', 'consumer', 'default')
  AND template_category = 'GENERAL';

-- Create audit trigger for consumer default template changes
CREATE OR REPLACE TRIGGER trg_field_templates_consumer_audit
    BEFORE UPDATE OF template_category, pii_classification, encryption_required, consumer_default_rules
    ON field_templates
    FOR EACH ROW
DECLARE
    v_audit_message VARCHAR2(500);
BEGIN
    -- Log PII classification changes for compliance
    IF :OLD.pii_classification != :NEW.pii_classification THEN
        v_audit_message := 'PII classification changed from ' || :OLD.pii_classification || ' to ' || :NEW.pii_classification;
        INSERT INTO audit_log (table_name, record_id, operation, audit_message, created_by, created_date)
        VALUES ('field_templates', :NEW.file_type || '|' || :NEW.transaction_type || '|' || :NEW.field_name, 
                'PII_CHANGE', v_audit_message, :NEW.modified_by, SYSDATE);
    END IF;
    
    -- Log encryption requirement changes
    IF :OLD.encryption_required != :NEW.encryption_required THEN
        v_audit_message := 'Encryption requirement changed from ' || :OLD.encryption_required || ' to ' || :NEW.encryption_required;
        INSERT INTO audit_log (table_name, record_id, operation, audit_message, created_by, created_date)
        VALUES ('field_templates', :NEW.file_type || '|' || :NEW.transaction_type || '|' || :NEW.field_name, 
                'ENCRYPTION_CHANGE', v_audit_message, :NEW.modified_by, SYSDATE);
    END IF;
END;
/

-- Grant necessary permissions
GRANT SELECT, INSERT, UPDATE ON field_templates TO fabric_app_role;
GRANT SELECT ON audit_log TO fabric_app_role;

-- Create validation function for consumer default rules JSON
CREATE OR REPLACE FUNCTION validate_consumer_default_rules(p_rules CLOB) 
RETURN VARCHAR2 IS
    v_json_valid NUMBER;
BEGIN
    -- Basic JSON validation
    SELECT CASE 
        WHEN JSON_VALID(p_rules) = 1 THEN 'VALID'
        ELSE 'INVALID'
    END INTO v_json_valid FROM DUAL;
    
    RETURN v_json_valid;
EXCEPTION
    WHEN OTHERS THEN
        RETURN 'INVALID';
END;
/

-- Add check constraint for JSON validation
ALTER TABLE field_templates ADD CONSTRAINT chk_consumer_rules_json 
    CHECK (consumer_default_rules IS NULL OR validate_consumer_default_rules(consumer_default_rules) = 'VALID');

-- Create materialized view for consumer default template summary
CREATE MATERIALIZED VIEW mv_consumer_default_template_summary
BUILD IMMEDIATE
REFRESH COMPLETE ON DEMAND
AS
SELECT 
    template_category,
    file_type,
    transaction_type,
    COUNT(*) as total_fields,
    SUM(CASE WHEN required = 'Y' THEN 1 ELSE 0 END) as required_fields,
    SUM(CASE WHEN pii_classification IN ('MEDIUM', 'HIGH') THEN 1 ELSE 0 END) as pii_fields,
    SUM(CASE WHEN encryption_required = 'Y' THEN 1 ELSE 0 END) as encrypted_fields,
    MAX(target_position) as max_position,
    SUM(length) as total_record_length
FROM field_templates 
WHERE enabled = 'Y' 
  AND template_category LIKE 'CONSUMER%'
GROUP BY template_category, file_type, transaction_type;

-- Create refresh job for materialized view
BEGIN
    DBMS_SCHEDULER.CREATE_JOB (
        job_name        => 'REFRESH_CONSUMER_TEMPLATE_SUMMARY',
        job_type        => 'PLSQL_BLOCK',
        job_action      => 'BEGIN DBMS_MVIEW.REFRESH(''MV_CONSUMER_DEFAULT_TEMPLATE_SUMMARY'', ''C''); END;',
        start_date      => SYSTIMESTAMP,
        repeat_interval => 'FREQ=HOURLY;INTERVAL=4',  -- Refresh every 4 hours
        enabled         => TRUE,
        comments        => 'Refresh consumer default template summary every 4 hours'
    );
END;
/

COMMIT;