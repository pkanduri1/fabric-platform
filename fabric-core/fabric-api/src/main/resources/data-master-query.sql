-- =========================================================================
-- MASTER QUERY DATA POPULATION SCRIPT
-- =========================================================================
-- Purpose: Populate TEMPLATE_MASTER_QUERY_MAPPING and MASTER_QUERY_COLUMNS
--          with realistic banking data for ENCORE, HR, and SHAW systems
-- Author: Senior Full Stack Developer Agent
-- Version: 1.0.0
-- Date: 2025-08-20
--
-- Security: All queries are read-only SELECT statements with parameterized
--          inputs and banking-grade security classifications
-- =========================================================================

-- Clear existing test data (if any)
DELETE FROM MASTER_QUERY_COLUMNS WHERE MASTER_QUERY_ID LIKE 'mq_%';
DELETE FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE MASTER_QUERY_ID LIKE 'mq_%';

-- =========================================================================
-- ENCORE SYSTEM MASTER QUERIES (Core Banking)
-- =========================================================================

-- 1. ENCORE Account Summary Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_encore_001_20250820', 'cfg_encore_123', 'mq_encore_account_summary', 'ENCORE Account Summary',
    'SELECT 
        account_id,
        account_number,
        customer_id,
        account_type,
        account_status,
        current_balance,
        available_balance,
        currency_code,
        branch_code,
        open_date,
        last_transaction_date,
        interest_rate,
        overdraft_limit,
        account_officer_id,
        risk_rating
     FROM encore_accounts 
     WHERE account_status = :account_status 
       AND branch_code = :branch_code 
       AND open_date >= :start_date 
       AND open_date <= :end_date
     ORDER BY account_number',
    'Retrieve account summary information for active accounts within specified date range and branch',
    'SELECT', 30, 100,
    '{"account_status": {"type": "string", "default": "ACTIVE"}, "branch_code": {"type": "string", "required": true}, "start_date": {"type": "date", "required": true}, "end_date": {"type": "date", "required": true}}',
    '{"account_status": {"values": ["ACTIVE", "INACTIVE", "CLOSED", "DORMANT"]}, "branch_code": {"pattern": "^[A-Z]{3}[0-9]{3}$"}, "start_date": {"format": "yyyy-MM-dd"}, "end_date": {"format": "yyyy-MM-dd", "after": "start_date"}}',
    'ACTIVE', 'Y', 'CONFIDENTIAL', 'Y',
    'system_admin', 'cor_setup_20250820_001'
);

-- 2. ENCORE Transaction Detail Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_encore_002_20250820', 'cfg_encore_124', 'mq_encore_transaction_detail', 'ENCORE Transaction Analysis',
    'SELECT 
        transaction_id,
        account_id,
        transaction_type,
        transaction_code,
        transaction_amount,
        transaction_date,
        value_date,
        description,
        reference_number,
        channel_code,
        teller_id,
        authorization_code,
        reversal_flag,
        batch_number,
        currency_code,
        exchange_rate,
        fee_amount,
        balance_after_transaction
     FROM encore_transactions 
     WHERE account_id = :account_id 
       AND transaction_date >= :start_date 
       AND transaction_date <= :end_date
       AND transaction_type IN (:transaction_types)
     ORDER BY transaction_date DESC, transaction_id DESC',
    'Retrieve detailed transaction history for specific account within date range',
    'SELECT', 25, 100,
    '{"account_id": {"type": "string", "required": true}, "start_date": {"type": "date", "required": true}, "end_date": {"type": "date", "required": true}, "transaction_types": {"type": "array", "default": ["CREDIT", "DEBIT"]}}',
    '{"account_id": {"pattern": "^[0-9]{10,15}$"}, "start_date": {"format": "yyyy-MM-dd"}, "end_date": {"format": "yyyy-MM-dd"}, "transaction_types": {"values": ["CREDIT", "DEBIT", "TRANSFER", "FEE", "INTEREST", "ADJUSTMENT"]}}',
    'ACTIVE', 'Y', 'CONFIDENTIAL', 'Y',
    'system_admin', 'cor_setup_20250820_002'
);

-- 3. ENCORE Customer Profile Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_encore_003_20250820', 'cfg_encore_125', 'mq_encore_customer_profile', 'ENCORE Customer Demographics',
    'SELECT 
        customer_id,
        customer_number,
        first_name,
        last_name,
        date_of_birth,
        customer_type,
        customer_segment,
        relationship_start_date,
        primary_branch_code,
        customer_status,
        kyc_status,
        risk_rating,
        total_relationship_balance,
        number_of_accounts,
        primary_phone,
        email_address,
        preferred_communication_method,
        last_contact_date
     FROM encore_customers 
     WHERE customer_segment = :customer_segment 
       AND customer_status = :customer_status
       AND primary_branch_code = :branch_code
       AND relationship_start_date >= :start_date
     ORDER BY total_relationship_balance DESC',
    'Retrieve customer demographic and relationship information for segmentation analysis',
    'SELECT', 20, 100,
    '{"customer_segment": {"type": "string", "required": true}, "customer_status": {"type": "string", "default": "ACTIVE"}, "branch_code": {"type": "string", "required": true}, "start_date": {"type": "date", "required": true}}',
    '{"customer_segment": {"values": ["RETAIL", "COMMERCIAL", "PRIVATE", "CORPORATE"]}, "customer_status": {"values": ["ACTIVE", "INACTIVE", "PROSPECT"]}, "branch_code": {"pattern": "^[A-Z]{3}[0-9]{3}$"}, "start_date": {"format": "yyyy-MM-dd"}}',
    'ACTIVE', 'Y', 'RESTRICTED', 'Y',
    'system_admin', 'cor_setup_20250820_003'
);

-- 4. ENCORE Loan Portfolio Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_encore_004_20250820', 'cfg_encore_126', 'mq_encore_loan_portfolio', 'ENCORE Loan Portfolio Analysis',
    'SELECT 
        loan_id,
        customer_id,
        loan_type,
        loan_product_code,
        original_loan_amount,
        current_balance,
        interest_rate,
        loan_status,
        origination_date,
        maturity_date,
        payment_frequency,
        next_payment_date,
        payment_amount,
        days_past_due,
        delinquency_status,
        collateral_type,
        collateral_value,
        loan_officer_id,
        branch_code
     FROM encore_loans 
     WHERE loan_status = :loan_status 
       AND loan_type = :loan_type
       AND branch_code = :branch_code
       AND origination_date >= :start_date
     ORDER BY current_balance DESC',
    'Retrieve loan portfolio information for risk analysis and reporting',
    'SELECT', 30, 100,
    '{"loan_status": {"type": "string", "required": true}, "loan_type": {"type": "string", "required": true}, "branch_code": {"type": "string", "required": true}, "start_date": {"type": "date", "required": true}}',
    '{"loan_status": {"values": ["ACTIVE", "CLOSED", "CHARGED_OFF", "RESTRUCTURED"]}, "loan_type": {"values": ["MORTGAGE", "AUTO", "PERSONAL", "COMMERCIAL", "LINE_OF_CREDIT"]}, "branch_code": {"pattern": "^[A-Z]{3}[0-9]{3}$"}, "start_date": {"format": "yyyy-MM-dd"}}',
    'ACTIVE', 'Y', 'CONFIDENTIAL', 'Y',
    'system_admin', 'cor_setup_20250820_004'
);

-- 5. ENCORE Deposit Analysis Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_encore_005_20250820', 'cfg_encore_127', 'mq_encore_deposit_analysis', 'ENCORE Deposit Portfolio',
    'SELECT 
        account_id,
        customer_id,
        product_type,
        account_balance,
        average_balance_3_months,
        average_balance_6_months,
        interest_rate,
        interest_earned_ytd,
        account_open_date,
        last_deposit_date,
        last_deposit_amount,
        number_deposits_3_months,
        account_status,
        maturity_date,
        renewal_instructions,
        branch_code,
        relationship_manager_id
     FROM encore_deposits 
     WHERE product_type = :product_type 
       AND account_balance >= :min_balance
       AND branch_code = :branch_code
       AND account_status = :account_status
     ORDER BY account_balance DESC',
    'Analyze deposit portfolio for relationship management and product development',
    'SELECT', 25, 100,
    '{"product_type": {"type": "string", "required": true}, "min_balance": {"type": "decimal", "default": 0}, "branch_code": {"type": "string", "required": true}, "account_status": {"type": "string", "default": "ACTIVE"}}',
    '{"product_type": {"values": ["CHECKING", "SAVINGS", "MONEY_MARKET", "CD", "IRA"]}, "min_balance": {"minimum": 0}, "branch_code": {"pattern": "^[A-Z]{3}[0-9]{3}$"}, "account_status": {"values": ["ACTIVE", "INACTIVE", "CLOSED"]}}',
    'ACTIVE', 'Y', 'INTERNAL', 'Y',
    'system_admin', 'cor_setup_20250820_005'
);

-- =========================================================================
-- HR SYSTEM MASTER QUERIES (Employee & Compliance)
-- =========================================================================

-- 6. HR Employee Compliance Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_hr_001_20250820', 'cfg_hr_201', 'mq_hr_employee_compliance', 'HR Employee Compliance Status',
    'SELECT 
        employee_id,
        employee_number,
        first_name,
        last_name,
        department,
        position_title,
        hire_date,
        employment_status,
        security_clearance_level,
        background_check_date,
        background_check_status,
        training_compliance_score,
        last_training_date,
        next_training_due,
        sox_certification_status,
        sox_certification_date,
        access_review_date,
        manager_employee_id,
        cost_center
     FROM hr_employees 
     WHERE department = :department 
       AND employment_status = :employment_status
       AND security_clearance_level >= :min_clearance_level
       AND hire_date >= :start_date
     ORDER BY employee_number',
    'Monitor employee compliance status for SOX and regulatory requirements',
    'SELECT', 20, 100,
    '{"department": {"type": "string", "required": true}, "employment_status": {"type": "string", "default": "ACTIVE"}, "min_clearance_level": {"type": "integer", "default": 1}, "start_date": {"type": "date", "required": true}}',
    '{"department": {"values": ["TECHNOLOGY", "OPERATIONS", "FINANCE", "RISK", "AUDIT", "COMPLIANCE", "LEGAL"]}, "employment_status": {"values": ["ACTIVE", "INACTIVE", "TERMINATED"]}, "min_clearance_level": {"minimum": 1, "maximum": 5}, "start_date": {"format": "yyyy-MM-dd"}}',
    'ACTIVE', 'Y', 'RESTRICTED', 'Y',
    'system_admin', 'cor_setup_20250820_006'
);

-- 7. HR Access Control Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_hr_002_20250820', 'cfg_hr_202', 'mq_hr_access_control', 'HR System Access Audit',
    'SELECT 
        employee_id,
        system_name,
        access_level,
        role_name,
        permission_group,
        access_granted_date,
        access_expiry_date,
        last_access_date,
        access_request_id,
        approved_by,
        approval_date,
        business_justification,
        review_status,
        next_review_date,
        segregation_of_duties_flag,
        high_risk_access_flag
     FROM hr_system_access 
     WHERE system_name = :system_name 
       AND access_level = :access_level
       AND employee_id = :employee_id
       AND access_granted_date >= :start_date
     ORDER BY access_granted_date DESC',
    'Audit system access permissions for segregation of duties compliance',
    'SELECT', 15, 50,
    '{"system_name": {"type": "string", "required": true}, "access_level": {"type": "string", "required": true}, "employee_id": {"type": "string", "required": true}, "start_date": {"type": "date", "required": true}}',
    '{"system_name": {"values": ["ENCORE", "SHAW", "CORE_BANKING", "RISK_SYSTEM", "ACCOUNTING"]}, "access_level": {"values": ["READ", "WRITE", "ADMIN", "SUPER_USER"]}, "employee_id": {"pattern": "^[0-9]{6}$"}, "start_date": {"format": "yyyy-MM-dd"}}',
    'ACTIVE', 'Y', 'RESTRICTED', 'Y',
    'system_admin', 'cor_setup_20250820_007'
);

-- 8. HR Training Compliance Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_hr_003_20250820', 'cfg_hr_203', 'mq_hr_training_compliance', 'HR Training & Certification Status',
    'SELECT 
        employee_id,
        training_course_id,
        course_name,
        training_category,
        completion_date,
        expiry_date,
        score_percentage,
        pass_fail_status,
        certification_level,
        trainer_id,
        training_method,
        renewal_required,
        next_renewal_date,
        cost_center_charged,
        training_hours,
        cpe_credits_earned
     FROM hr_training_records 
     WHERE training_category = :training_category 
       AND completion_date >= :start_date
       AND completion_date <= :end_date
       AND pass_fail_status = :pass_status
     ORDER BY completion_date DESC',
    'Track employee training completion and certification status for compliance',
    'SELECT', 20, 100,
    '{"training_category": {"type": "string", "required": true}, "start_date": {"type": "date", "required": true}, "end_date": {"type": "date", "required": true}, "pass_status": {"type": "string", "default": "PASS"}}',
    '{"training_category": {"values": ["SOX_COMPLIANCE", "ANTI_MONEY_LAUNDERING", "CYBER_SECURITY", "DATA_PRIVACY", "RISK_MANAGEMENT"]}, "start_date": {"format": "yyyy-MM-dd"}, "end_date": {"format": "yyyy-MM-dd"}, "pass_status": {"values": ["PASS", "FAIL", "PENDING"]}}',
    'ACTIVE', 'Y', 'INTERNAL', 'Y',
    'system_admin', 'cor_setup_20250820_008'
);

-- =========================================================================
-- SHAW SYSTEM MASTER QUERIES (Risk Assessment)
-- =========================================================================

-- 9. SHAW Risk Assessment Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_shaw_001_20250820', 'cfg_shaw_301', 'mq_shaw_risk_assessment', 'SHAW Risk Scoring Analysis',
    'SELECT 
        customer_id,
        risk_assessment_id,
        assessment_date,
        risk_score,
        risk_grade,
        risk_category,
        probability_of_default,
        loss_given_default,
        exposure_at_default,
        economic_capital,
        regulatory_capital,
        industry_code,
        geographic_risk_factor,
        financial_strength_score,
        management_quality_score,
        collateral_coverage_ratio,
        guarantor_strength_score,
        next_review_date
     FROM shaw_risk_assessments 
     WHERE risk_category = :risk_category 
       AND assessment_date >= :start_date
       AND assessment_date <= :end_date
       AND risk_grade IN (:risk_grades)
     ORDER BY risk_score DESC, assessment_date DESC',
    'Analyze customer risk profiles and scoring for credit and operational risk management',
    'SELECT', 30, 100,
    '{"risk_category": {"type": "string", "required": true}, "start_date": {"type": "date", "required": true}, "end_date": {"type": "date", "required": true}, "risk_grades": {"type": "array", "required": true}}',
    '{"risk_category": {"values": ["CREDIT", "OPERATIONAL", "MARKET", "LIQUIDITY", "COMPLIANCE"]}, "start_date": {"format": "yyyy-MM-dd"}, "end_date": {"format": "yyyy-MM-dd"}, "risk_grades": {"values": ["AAA", "AA", "A", "BBB", "BB", "B", "CCC", "CC", "C", "D"]}}',
    'ACTIVE', 'Y', 'CONFIDENTIAL', 'Y',
    'system_admin', 'cor_setup_20250820_009'
);

-- 10. SHAW Credit Exposure Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_shaw_002_20250820', 'cfg_shaw_302', 'mq_shaw_credit_exposure', 'SHAW Credit Exposure Analysis',
    'SELECT 
        customer_id,
        exposure_id,
        facility_type,
        commitment_amount,
        outstanding_balance,
        available_amount,
        utilization_percentage,
        maturity_date,
        pricing_spread,
        internal_rating,
        external_rating,
        industry_concentration,
        geographic_concentration,
        collateral_type,
        collateral_value,
        guarantee_amount,
        covenant_compliance_status,
        last_review_date,
        next_review_date
     FROM shaw_credit_exposures 
     WHERE facility_type = :facility_type 
       AND internal_rating IN (:internal_ratings)
       AND outstanding_balance >= :min_exposure
       AND maturity_date >= :start_date
     ORDER BY outstanding_balance DESC',
    'Monitor credit exposure concentration and covenant compliance across portfolio',
    'SELECT', 25, 100,
    '{"facility_type": {"type": "string", "required": true}, "internal_ratings": {"type": "array", "required": true}, "min_exposure": {"type": "decimal", "default": 1000000}, "start_date": {"type": "date", "required": true}}',
    '{"facility_type": {"values": ["TERM_LOAN", "REVOLVING_CREDIT", "LETTER_OF_CREDIT", "TRADE_FINANCE", "MORTGAGE"]}, "internal_ratings": {"values": ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"]}, "min_exposure": {"minimum": 0}, "start_date": {"format": "yyyy-MM-dd"}}',
    'ACTIVE', 'Y', 'CONFIDENTIAL', 'Y',
    'system_admin', 'cor_setup_20250820_010'
);

-- 11. SHAW Market Risk Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_shaw_003_20250820', 'cfg_shaw_303', 'mq_shaw_market_risk', 'SHAW Market Risk Metrics',
    'SELECT 
        portfolio_id,
        risk_factor_type,
        measurement_date,
        value_at_risk_1day,
        value_at_risk_10day,
        expected_shortfall,
        stressed_var,
        basis_point_value,
        duration,
        convexity,
        delta_equivalent,
        gamma_equivalent,
        vega_equivalent,
        theta_equivalent,
        portfolio_value,
        benchmark_value,
        tracking_error,
        information_ratio
     FROM shaw_market_risk_metrics 
     WHERE risk_factor_type = :risk_factor_type 
       AND measurement_date >= :start_date
       AND measurement_date <= :end_date
       AND portfolio_id = :portfolio_id
     ORDER BY measurement_date DESC',
    'Analyze market risk metrics including VaR, sensitivity measures, and tracking error',
    'SELECT', 20, 100,
    '{"risk_factor_type": {"type": "string", "required": true}, "start_date": {"type": "date", "required": true}, "end_date": {"type": "date", "required": true}, "portfolio_id": {"type": "string", "required": true}}',
    '{"risk_factor_type": {"values": ["INTEREST_RATE", "EQUITY", "FX", "COMMODITY", "CREDIT_SPREAD"]}, "start_date": {"format": "yyyy-MM-dd"}, "end_date": {"format": "yyyy-MM-dd"}, "portfolio_id": {"pattern": "^[A-Z]{3}_[0-9]{4}$"}}',
    'ACTIVE', 'Y', 'CONFIDENTIAL', 'Y',
    'system_admin', 'cor_setup_20250820_011'
);

-- 12. SHAW Operational Risk Query
INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
    MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, QUERY_DESCRIPTION,
    QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
    QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES,
    STATUS, IS_READ_ONLY, SECURITY_CLASSIFICATION, REQUIRES_APPROVAL,
    CREATED_BY, CORRELATION_ID
) VALUES (
    'tmq_shaw_004_20250820', 'cfg_shaw_304', 'mq_shaw_operational_risk', 'SHAW Operational Risk Events',
    'SELECT 
        event_id,
        event_type,
        business_line,
        event_date,
        discovery_date,
        loss_amount,
        recovery_amount,
        net_loss_amount,
        event_status,
        root_cause_category,
        description,
        impact_assessment,
        remediation_actions,
        control_effectiveness,
        regulatory_reporting_required,
        insurance_claim_amount,
        responsible_manager,
        resolution_date
     FROM shaw_operational_risk_events 
     WHERE business_line = :business_line 
       AND event_type = :event_type
       AND event_date >= :start_date
       AND event_date <= :end_date
       AND net_loss_amount >= :min_loss_amount
     ORDER BY net_loss_amount DESC, event_date DESC',
    'Track operational risk events, losses, and remediation actions for Basel III reporting',
    'SELECT', 25, 100,
    '{"business_line": {"type": "string", "required": true}, "event_type": {"type": "string", "required": true}, "start_date": {"type": "date", "required": true}, "end_date": {"type": "date", "required": true}, "min_loss_amount": {"type": "decimal", "default": 10000}}',
    '{"business_line": {"values": ["CORPORATE_FINANCE", "TRADING", "RETAIL_BANKING", "COMMERCIAL_BANKING", "PAYMENT_SETTLEMENT", "AGENCY_SERVICES", "ASSET_MANAGEMENT", "RETAIL_BROKERAGE"]}, "event_type": {"values": ["INTERNAL_FRAUD", "EXTERNAL_FRAUD", "EMPLOYMENT_PRACTICES", "CLIENTS_PRODUCTS", "DAMAGE_ASSETS", "BUSINESS_DISRUPTION", "EXECUTION_DELIVERY", "TECHNOLOGY"]}, "start_date": {"format": "yyyy-MM-dd"}, "end_date": {"format": "yyyy-MM-dd"}, "min_loss_amount": {"minimum": 0}}',
    'ACTIVE', 'Y', 'RESTRICTED', 'Y',
    'system_admin', 'cor_setup_20250820_012'
);

COMMIT;

-- =========================================================================
-- MASTER QUERY COLUMNS METADATA
-- =========================================================================

-- ENCORE Account Summary Columns
INSERT INTO MASTER_QUERY_COLUMNS (COLUMN_ID, MASTER_QUERY_ID, COLUMN_NAME, COLUMN_ALIAS, COLUMN_TYPE, COLUMN_LENGTH, COLUMN_PRECISION, COLUMN_SCALE, IS_NULLABLE, IS_PRIMARY_KEY, COLUMN_ORDER, VALIDATION_RULES, DISPLAY_FORMAT, COLUMN_DESCRIPTION, IS_SENSITIVE_DATA, DATA_CLASSIFICATION, CREATED_BY) VALUES
('col_mq_encore_001_001', 'mq_encore_account_summary', 'account_id', 'Account ID', 'VARCHAR', 15, NULL, NULL, 'N', 'Y', 1, '{"pattern": "^[0-9]{10,15}$"}', NULL, 'Unique account identifier', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_encore_001_002', 'mq_encore_account_summary', 'account_number', 'Account Number', 'VARCHAR', 20, NULL, NULL, 'N', 'N', 2, '{"pattern": "^[0-9]{8,20}$"}', '####-####-####', 'Customer-facing account number', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_encore_001_003', 'mq_encore_account_summary', 'customer_id', 'Customer ID', 'VARCHAR', 12, NULL, NULL, 'N', 'N', 3, '{"pattern": "^[0-9]{8,12}$"}', NULL, 'Customer identifier', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_encore_001_004', 'mq_encore_account_summary', 'account_type', 'Account Type', 'VARCHAR', 50, NULL, NULL, 'N', 'N', 4, '{"values": ["CHECKING", "SAVINGS", "MONEY_MARKET", "CD", "LOAN"]}', NULL, 'Type of banking account', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_001_005', 'mq_encore_account_summary', 'account_status', 'Status', 'VARCHAR', 20, NULL, NULL, 'N', 'N', 5, '{"values": ["ACTIVE", "INACTIVE", "CLOSED", "DORMANT"]}', NULL, 'Current account status', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_001_006', 'mq_encore_account_summary', 'current_balance', 'Current Balance', 'DECIMAL', NULL, 15, 2, 'Y', 'N', 6, '{"minimum": -999999999999.99, "maximum": 999999999999.99}', '$#,##0.00', 'Current account balance', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_encore_001_007', 'mq_encore_account_summary', 'available_balance', 'Available Balance', 'DECIMAL', NULL, 15, 2, 'Y', 'N', 7, '{"minimum": -999999999999.99, "maximum": 999999999999.99}', '$#,##0.00', 'Available balance for transactions', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_encore_001_008', 'mq_encore_account_summary', 'currency_code', 'Currency', 'CHAR', 3, NULL, NULL, 'N', 'N', 8, '{"values": ["USD", "EUR", "GBP", "CAD"]}', NULL, 'Account currency code', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_001_009', 'mq_encore_account_summary', 'branch_code', 'Branch Code', 'VARCHAR', 6, NULL, NULL, 'N', 'N', 9, '{"pattern": "^[A-Z]{3}[0-9]{3}$"}', NULL, 'Branch identifier', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_001_010', 'mq_encore_account_summary', 'open_date', 'Open Date', 'DATE', NULL, NULL, NULL, 'N', 'N', 10, '{"format": "yyyy-MM-dd"}', 'MM/dd/yyyy', 'Account opening date', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_001_011', 'mq_encore_account_summary', 'last_transaction_date', 'Last Transaction', 'DATE', NULL, NULL, NULL, 'Y', 'N', 11, '{"format": "yyyy-MM-dd"}', 'MM/dd/yyyy', 'Date of last transaction', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_001_012', 'mq_encore_account_summary', 'interest_rate', 'Interest Rate', 'DECIMAL', NULL, 8, 4, 'Y', 'N', 12, '{"minimum": 0, "maximum": 99.9999}', '#0.00%', 'Current interest rate', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_001_013', 'mq_encore_account_summary', 'overdraft_limit', 'Overdraft Limit', 'DECIMAL', NULL, 12, 2, 'Y', 'N', 13, '{"minimum": 0, "maximum": 999999999.99}', '$#,##0.00', 'Authorized overdraft limit', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_encore_001_014', 'mq_encore_account_summary', 'account_officer_id', 'Account Officer', 'VARCHAR', 10, NULL, NULL, 'Y', 'N', 14, '{"pattern": "^[A-Z]{2}[0-9]{6}$"}', NULL, 'Assigned account officer', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_001_015', 'mq_encore_account_summary', 'risk_rating', 'Risk Rating', 'VARCHAR', 5, NULL, NULL, 'Y', 'N', 15, '{"values": ["AAA", "AA", "A", "BBB", "BB", "B", "CCC"]}', NULL, 'Account risk classification', 'Y', 'CONFIDENTIAL', 'system_admin');

-- ENCORE Transaction Detail Columns
INSERT INTO MASTER_QUERY_COLUMNS (COLUMN_ID, MASTER_QUERY_ID, COLUMN_NAME, COLUMN_ALIAS, COLUMN_TYPE, COLUMN_LENGTH, COLUMN_PRECISION, COLUMN_SCALE, IS_NULLABLE, IS_PRIMARY_KEY, COLUMN_ORDER, VALIDATION_RULES, DISPLAY_FORMAT, COLUMN_DESCRIPTION, IS_SENSITIVE_DATA, DATA_CLASSIFICATION, CREATED_BY) VALUES
('col_mq_encore_002_001', 'mq_encore_transaction_detail', 'transaction_id', 'Transaction ID', 'VARCHAR', 20, NULL, NULL, 'N', 'Y', 1, '{"pattern": "^TXN[0-9]{15}$"}', NULL, 'Unique transaction identifier', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_002_002', 'mq_encore_transaction_detail', 'account_id', 'Account ID', 'VARCHAR', 15, NULL, NULL, 'N', 'N', 2, '{"pattern": "^[0-9]{10,15}$"}', NULL, 'Account identifier', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_encore_002_003', 'mq_encore_transaction_detail', 'transaction_type', 'Type', 'VARCHAR', 20, NULL, NULL, 'N', 'N', 3, '{"values": ["CREDIT", "DEBIT", "TRANSFER", "FEE", "INTEREST"]}', NULL, 'Transaction type classification', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_002_004', 'mq_encore_transaction_detail', 'transaction_code', 'Transaction Code', 'VARCHAR', 10, NULL, NULL, 'N', 'N', 4, '{"pattern": "^[0-9]{3}$"}', NULL, 'Standard transaction code', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_002_005', 'mq_encore_transaction_detail', 'transaction_amount', 'Amount', 'DECIMAL', NULL, 15, 2, 'N', 'N', 5, '{"minimum": -999999999999.99, "maximum": 999999999999.99}', '$#,##0.00', 'Transaction amount', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_encore_002_006', 'mq_encore_transaction_detail', 'transaction_date', 'Date', 'DATE', NULL, NULL, NULL, 'N', 'N', 6, '{"format": "yyyy-MM-dd"}', 'MM/dd/yyyy', 'Transaction processing date', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_002_007', 'mq_encore_transaction_detail', 'value_date', 'Value Date', 'DATE', NULL, NULL, NULL, 'Y', 'N', 7, '{"format": "yyyy-MM-dd"}', 'MM/dd/yyyy', 'Value date for interest calculation', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_002_008', 'mq_encore_transaction_detail', 'description', 'Description', 'VARCHAR', 200, NULL, NULL, 'Y', 'N', 8, '{"maxLength": 200}', NULL, 'Transaction description', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_encore_002_009', 'mq_encore_transaction_detail', 'reference_number', 'Reference', 'VARCHAR', 50, NULL, NULL, 'Y', 'N', 9, '{"maxLength": 50}', NULL, 'External reference number', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_002_010', 'mq_encore_transaction_detail', 'channel_code', 'Channel', 'VARCHAR', 20, NULL, NULL, 'Y', 'N', 10, '{"values": ["BRANCH", "ATM", "ONLINE", "MOBILE", "WIRE"]}', NULL, 'Transaction channel', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_002_011', 'mq_encore_transaction_detail', 'teller_id', 'Teller ID', 'VARCHAR', 10, NULL, NULL, 'Y', 'N', 11, '{"pattern": "^[A-Z]{2}[0-9]{6}$"}', NULL, 'Processing teller identifier', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_002_012', 'mq_encore_transaction_detail', 'authorization_code', 'Auth Code', 'VARCHAR', 20, NULL, NULL, 'Y', 'N', 12, '{"maxLength": 20}', NULL, 'Transaction authorization code', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_encore_002_013', 'mq_encore_transaction_detail', 'reversal_flag', 'Reversal', 'CHAR', 1, NULL, NULL, 'N', 'N', 13, '{"values": ["Y", "N"]}', NULL, 'Transaction reversal indicator', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_002_014', 'mq_encore_transaction_detail', 'batch_number', 'Batch Number', 'VARCHAR', 20, NULL, NULL, 'Y', 'N', 14, '{"pattern": "^BATCH[0-9]{8}$"}', NULL, 'Processing batch identifier', 'N', 'INTERNAL', 'system_admin'),
('col_mq_encore_002_015', 'mq_encore_transaction_detail', 'balance_after_transaction', 'Balance After', 'DECIMAL', NULL, 15, 2, 'Y', 'N', 15, '{"minimum": -999999999999.99, "maximum": 999999999999.99}', '$#,##0.00', 'Account balance after transaction', 'Y', 'CONFIDENTIAL', 'system_admin');

-- HR Employee Compliance Columns
INSERT INTO MASTER_QUERY_COLUMNS (COLUMN_ID, MASTER_QUERY_ID, COLUMN_NAME, COLUMN_ALIAS, COLUMN_TYPE, COLUMN_LENGTH, COLUMN_PRECISION, COLUMN_SCALE, IS_NULLABLE, IS_PRIMARY_KEY, COLUMN_ORDER, VALIDATION_RULES, DISPLAY_FORMAT, COLUMN_DESCRIPTION, IS_SENSITIVE_DATA, DATA_CLASSIFICATION, CREATED_BY) VALUES
('col_mq_hr_001_001', 'mq_hr_employee_compliance', 'employee_id', 'Employee ID', 'VARCHAR', 10, NULL, NULL, 'N', 'Y', 1, '{"pattern": "^[0-9]{6}$"}', NULL, 'Unique employee identifier', 'Y', 'RESTRICTED', 'system_admin'),
('col_mq_hr_001_002', 'mq_hr_employee_compliance', 'employee_number', 'Employee Number', 'VARCHAR', 10, NULL, NULL, 'N', 'N', 2, '{"pattern": "^EMP[0-9]{6}$"}', NULL, 'Employee badge number', 'Y', 'RESTRICTED', 'system_admin'),
('col_mq_hr_001_003', 'mq_hr_employee_compliance', 'first_name', 'First Name', 'VARCHAR', 50, NULL, NULL, 'N', 'N', 3, '{"maxLength": 50}', NULL, 'Employee first name', 'Y', 'RESTRICTED', 'system_admin'),
('col_mq_hr_001_004', 'mq_hr_employee_compliance', 'last_name', 'Last Name', 'VARCHAR', 50, NULL, NULL, 'N', 'N', 4, '{"maxLength": 50}', NULL, 'Employee last name', 'Y', 'RESTRICTED', 'system_admin'),
('col_mq_hr_001_005', 'mq_hr_employee_compliance', 'department', 'Department', 'VARCHAR', 50, NULL, NULL, 'N', 'N', 5, '{"values": ["TECHNOLOGY", "OPERATIONS", "FINANCE", "RISK", "AUDIT"]}', NULL, 'Employee department', 'N', 'INTERNAL', 'system_admin'),
('col_mq_hr_001_006', 'mq_hr_employee_compliance', 'position_title', 'Position', 'VARCHAR', 100, NULL, NULL, 'N', 'N', 6, '{"maxLength": 100}', NULL, 'Job title', 'N', 'INTERNAL', 'system_admin'),
('col_mq_hr_001_007', 'mq_hr_employee_compliance', 'hire_date', 'Hire Date', 'DATE', NULL, NULL, NULL, 'N', 'N', 7, '{"format": "yyyy-MM-dd"}', 'MM/dd/yyyy', 'Employment start date', 'Y', 'RESTRICTED', 'system_admin'),
('col_mq_hr_001_008', 'mq_hr_employee_compliance', 'employment_status', 'Status', 'VARCHAR', 20, NULL, NULL, 'N', 'N', 8, '{"values": ["ACTIVE", "INACTIVE", "TERMINATED"]}', NULL, 'Employment status', 'Y', 'RESTRICTED', 'system_admin'),
('col_mq_hr_001_009', 'mq_hr_employee_compliance', 'security_clearance_level', 'Clearance Level', 'NUMBER', NULL, 1, NULL, 'Y', 'N', 9, '{"minimum": 1, "maximum": 5}', NULL, 'Security clearance level (1-5)', 'Y', 'RESTRICTED', 'system_admin'),
('col_mq_hr_001_010', 'mq_hr_employee_compliance', 'background_check_date', 'Background Check', 'DATE', NULL, NULL, NULL, 'Y', 'N', 10, '{"format": "yyyy-MM-dd"}', 'MM/dd/yyyy', 'Last background check date', 'Y', 'RESTRICTED', 'system_admin'),
('col_mq_hr_001_011', 'mq_hr_employee_compliance', 'sox_certification_status', 'SOX Status', 'VARCHAR', 20, NULL, NULL, 'Y', 'N', 11, '{"values": ["CERTIFIED", "EXPIRED", "PENDING", "NOT_REQUIRED"]}', NULL, 'SOX certification status', 'Y', 'RESTRICTED', 'system_admin'),
('col_mq_hr_001_012', 'mq_hr_employee_compliance', 'training_compliance_score', 'Training Score', 'DECIMAL', NULL, 5, 2, 'Y', 'N', 12, '{"minimum": 0, "maximum": 100}', '#0.0%', 'Training compliance percentage', 'N', 'INTERNAL', 'system_admin');

-- SHAW Risk Assessment Columns
INSERT INTO MASTER_QUERY_COLUMNS (COLUMN_ID, MASTER_QUERY_ID, COLUMN_NAME, COLUMN_ALIAS, COLUMN_TYPE, COLUMN_LENGTH, COLUMN_PRECISION, COLUMN_SCALE, IS_NULLABLE, IS_PRIMARY_KEY, COLUMN_ORDER, VALIDATION_RULES, DISPLAY_FORMAT, COLUMN_DESCRIPTION, IS_SENSITIVE_DATA, DATA_CLASSIFICATION, CREATED_BY) VALUES
('col_mq_shaw_001_001', 'mq_shaw_risk_assessment', 'customer_id', 'Customer ID', 'VARCHAR', 12, NULL, NULL, 'N', 'N', 1, '{"pattern": "^[0-9]{8,12}$"}', NULL, 'Customer identifier', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_shaw_001_002', 'mq_shaw_risk_assessment', 'risk_assessment_id', 'Assessment ID', 'VARCHAR', 20, NULL, NULL, 'N', 'Y', 2, '{"pattern": "^RA[0-9]{15}$"}', NULL, 'Risk assessment identifier', 'N', 'INTERNAL', 'system_admin'),
('col_mq_shaw_001_003', 'mq_shaw_risk_assessment', 'assessment_date', 'Assessment Date', 'DATE', NULL, NULL, NULL, 'N', 'N', 3, '{"format": "yyyy-MM-dd"}', 'MM/dd/yyyy', 'Risk assessment date', 'N', 'INTERNAL', 'system_admin'),
('col_mq_shaw_001_004', 'mq_shaw_risk_assessment', 'risk_score', 'Risk Score', 'DECIMAL', NULL, 8, 4, 'N', 'N', 4, '{"minimum": 0, "maximum": 1000}', '#,##0.00', 'Calculated risk score', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_shaw_001_005', 'mq_shaw_risk_assessment', 'risk_grade', 'Risk Grade', 'VARCHAR', 5, NULL, NULL, 'N', 'N', 5, '{"values": ["AAA", "AA", "A", "BBB", "BB", "B", "CCC", "CC", "C", "D"]}', NULL, 'Risk rating grade', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_shaw_001_006', 'mq_shaw_risk_assessment', 'risk_category', 'Category', 'VARCHAR', 20, NULL, NULL, 'N', 'N', 6, '{"values": ["CREDIT", "OPERATIONAL", "MARKET", "LIQUIDITY"]}', NULL, 'Risk category type', 'N', 'INTERNAL', 'system_admin'),
('col_mq_shaw_001_007', 'mq_shaw_risk_assessment', 'probability_of_default', 'PD', 'DECIMAL', NULL, 8, 6, 'Y', 'N', 7, '{"minimum": 0, "maximum": 1}', '0.00%', 'Probability of default', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_shaw_001_008', 'mq_shaw_risk_assessment', 'loss_given_default', 'LGD', 'DECIMAL', NULL, 8, 6, 'Y', 'N', 8, '{"minimum": 0, "maximum": 1}', '0.00%', 'Loss given default', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_shaw_001_009', 'mq_shaw_risk_assessment', 'exposure_at_default', 'EAD', 'DECIMAL', NULL, 15, 2, 'Y', 'N', 9, '{"minimum": 0}', '$#,##0.00', 'Exposure at default amount', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_shaw_001_010', 'mq_shaw_risk_assessment', 'economic_capital', 'Economic Capital', 'DECIMAL', NULL, 15, 2, 'Y', 'N', 10, '{"minimum": 0}', '$#,##0.00', 'Required economic capital', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_shaw_001_011', 'mq_shaw_risk_assessment', 'regulatory_capital', 'Regulatory Capital', 'DECIMAL', NULL, 15, 2, 'Y', 'N', 11, '{"minimum": 0}', '$#,##0.00', 'Required regulatory capital', 'Y', 'CONFIDENTIAL', 'system_admin'),
('col_mq_shaw_001_012', 'mq_shaw_risk_assessment', 'industry_code', 'Industry Code', 'VARCHAR', 10, NULL, NULL, 'Y', 'N', 12, '{"pattern": "^[0-9]{4,6}$"}', NULL, 'NAICS industry code', 'N', 'INTERNAL', 'system_admin');

COMMIT;

-- =========================================================================
-- VERIFICATION QUERIES
-- =========================================================================
/*
-- Verify data population
SELECT 'TEMPLATE_MASTER_QUERY_MAPPING' as table_name, COUNT(*) as record_count FROM TEMPLATE_MASTER_QUERY_MAPPING
UNION ALL
SELECT 'MASTER_QUERY_COLUMNS' as table_name, COUNT(*) as record_count FROM MASTER_QUERY_COLUMNS
ORDER BY table_name;

-- Sample master queries by system
SELECT SUBSTR(MASTER_QUERY_ID, 1, 10) as system_prefix, COUNT(*) as query_count
FROM TEMPLATE_MASTER_QUERY_MAPPING
GROUP BY SUBSTR(MASTER_QUERY_ID, 1, 10)
ORDER BY system_prefix;

-- Column metadata summary
SELECT mqc.MASTER_QUERY_ID, tmqm.QUERY_NAME, COUNT(mqc.COLUMN_ID) as column_count,
       SUM(CASE WHEN mqc.IS_SENSITIVE_DATA = 'Y' THEN 1 ELSE 0 END) as sensitive_columns,
       tmqm.SECURITY_CLASSIFICATION
FROM MASTER_QUERY_COLUMNS mqc
JOIN TEMPLATE_MASTER_QUERY_MAPPING tmqm ON mqc.MASTER_QUERY_ID = tmqm.MASTER_QUERY_ID
GROUP BY mqc.MASTER_QUERY_ID, tmqm.QUERY_NAME, tmqm.SECURITY_CLASSIFICATION
ORDER BY mqc.MASTER_QUERY_ID;
*/

-- =========================================================================
-- END OF SCRIPT
-- =========================================================================