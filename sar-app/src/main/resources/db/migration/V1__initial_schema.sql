-- FinCEN SAR Solution - Initial Database Schema
-- V1: Core tables for SAR Report modular monolith

-- =============================================
-- SAR REPORTS (Core aggregate root)
-- =============================================
CREATE TABLE IF NOT EXISTS sar_reports (
    id                          UUID NOT NULL DEFAULT gen_random_uuid(),
    report_number               VARCHAR(50) UNIQUE NOT NULL,
    case_id                     VARCHAR(255),
    case_system                 VARCHAR(100),
    prior_bsa_identifier        VARCHAR(50),
    bsa_identifier              VARCHAR(50),
    status                      VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    filing_status               VARCHAR(30),
    activity_from_date          DATE,
    activity_to_date            DATE,
    filing_date                 DATE,
    continuing_activity         BOOLEAN DEFAULT FALSE,
    corrects_amends_prior       BOOLEAN DEFAULT FALSE,
    joint_report                BOOLEAN DEFAULT FALSE,
    fi_noted_suspicious_activity BOOLEAN DEFAULT TRUE,
    total_suspicious_amount     DECIMAL(19,2),
    no_amount_involved          BOOLEAN DEFAULT FALSE,
    narrative                   TEXT,
    batch_id                    UUID,
    submitted_at                TIMESTAMP,
    acknowledged_at             TIMESTAMP,
    fincen_tracking_number      VARCHAR(100),
    rejection_reason            TEXT,
    filing_institution_name     VARCHAR(255) NOT NULL,
    filing_institution_ein      VARCHAR(20),
    filing_institution_type     VARCHAR(50),
    contact_office_name         VARCHAR(255),
    contact_phone               VARCHAR(30),
    contact_email               VARCHAR(255),
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    version                     BIGINT DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_sar_reports_status ON sar_reports(status);
CREATE INDEX idx_sar_reports_case_id ON sar_reports(case_id);
CREATE INDEX idx_sar_reports_batch_id ON sar_reports(batch_id);
CREATE INDEX idx_sar_reports_filing_date ON sar_reports(filing_date);
CREATE INDEX idx_sar_reports_bsa_id ON sar_reports(bsa_identifier);

-- =============================================
-- SAR SUBJECTS (Part I - up to 999 per report)
-- =============================================
CREATE TABLE IF NOT EXISTS sar_subjects (
    id                          UUID NOT NULL DEFAULT gen_random_uuid(),
    sar_report_id               UUID NOT NULL,
    seq_num                     INTEGER,
    case_subject_id             VARCHAR(255),
    auto_populated              BOOLEAN DEFAULT FALSE,
    is_entity                   BOOLEAN DEFAULT FALSE,
    last_name                   VARCHAR(150),
    first_name                  VARCHAR(100),
    middle_name                 VARCHAR(100),
    suffix                      VARCHAR(20),
    date_of_birth               DATE,
    entity_name                 VARCHAR(255),
    doing_business_as           VARCHAR(255),
    id_type                     VARCHAR(30),
    id_number                   VARCHAR(100),
    id_issue_state              VARCHAR(3),
    id_issue_country            VARCHAR(3),
    address                     VARCHAR(500),
    city                        VARCHAR(100),
    state                       VARCHAR(3),
    zip_code                    VARCHAR(20),
    country                     VARCHAR(3) DEFAULT 'US',
    phone_number                VARCHAR(30),
    phone_extension             VARCHAR(10),
    email                       VARCHAR(255),
    occupation                  VARCHAR(255),
    naics_code                  VARCHAR(10),
    role_code                   VARCHAR(10),
    role_other_description      VARCHAR(255),
    is_unknown                  BOOLEAN DEFAULT FALSE,
    still_employed              BOOLEAN,
    corrective_action           VARCHAR(500),
    has_relationship_to_account BOOLEAN DEFAULT FALSE,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    version                     BIGINT DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (sar_report_id) REFERENCES sar_reports(id) ON DELETE CASCADE
);

CREATE INDEX idx_sar_subjects_report ON sar_subjects(sar_report_id);

-- =============================================
-- SAR ACCOUNTS
-- =============================================
CREATE TABLE IF NOT EXISTS sar_accounts (
    id                          UUID NOT NULL DEFAULT gen_random_uuid(),
    sar_report_id               UUID NOT NULL,
    subject_id                  UUID,
    case_account_id             VARCHAR(255),
    auto_populated              BOOLEAN DEFAULT FALSE,
    account_number              VARCHAR(100),
    account_number_closed       BOOLEAN DEFAULT FALSE,
    account_type                VARCHAR(50),
    product_type                VARCHAR(50),
    institution_name            VARCHAR(255),
    institution_ein             VARCHAR(20),
    routing_number              VARCHAR(20),
    opened_date                 DATE,
    closed_date                 DATE,
    balance                     DECIMAL(19,2),
    currency_code               VARCHAR(5) DEFAULT 'USD',
    is_foreign_account          BOOLEAN DEFAULT FALSE,
    foreign_country             VARCHAR(3),
    action_account_closed       BOOLEAN DEFAULT FALSE,
    action_account_frozen       BOOLEAN DEFAULT FALSE,
    no_action_taken             BOOLEAN DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    version                     BIGINT DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (sar_report_id) REFERENCES sar_reports(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES sar_subjects(id)
);

CREATE INDEX idx_sar_accounts_report ON sar_accounts(sar_report_id);
CREATE INDEX idx_sar_accounts_subject ON sar_accounts(subject_id);

-- =============================================
-- SAR TRANSACTIONS
-- =============================================
CREATE TABLE IF NOT EXISTS sar_transactions (
    id                          UUID NOT NULL DEFAULT gen_random_uuid(),
    sar_report_id               UUID NOT NULL,
    account_id                  UUID,
    case_transaction_id         VARCHAR(255),
    alert_id                    VARCHAR(100),
    auto_populated              BOOLEAN DEFAULT FALSE,
    transaction_date            DATE,
    transaction_datetime        TIMESTAMP,
    transaction_type            VARCHAR(50),
    transaction_type_other      VARCHAR(100),
    amount                      DECIMAL(19,2),
    currency_code               VARCHAR(5) DEFAULT 'USD',
    currency_foreign            VARCHAR(5),
    direction                   VARCHAR(10),
    counterparty_name           VARCHAR(255),
    counterparty_account        VARCHAR(100),
    counterparty_institution    VARCHAR(255),
    counterparty_country        VARCHAR(3),
    location_type               VARCHAR(50),
    location_description        VARCHAR(255),
    structuring_flag            BOOLEAN DEFAULT FALSE,
    rapid_movement_flag         BOOLEAN DEFAULT FALSE,
    unusual_pattern_flag        BOOLEAN DEFAULT FALSE,
    suspicion_notes             TEXT,
    reference_number            VARCHAR(100),
    check_number                VARCHAR(50),
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    version                     BIGINT DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (sar_report_id) REFERENCES sar_reports(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES sar_accounts(id)
);

CREATE INDEX idx_sar_transactions_report ON sar_transactions(sar_report_id);
CREATE INDEX idx_sar_transactions_alert ON sar_transactions(alert_id);

-- =============================================
-- SAR ACTIVITY TYPES (Part II - suspicious activity subtypes)
-- Per FinCENReferenceCodes.xsd SuspiciousActivitySubtypeID values
-- =============================================
CREATE TABLE IF NOT EXISTS sar_activity_types (
    id                              UUID NOT NULL DEFAULT gen_random_uuid(),
    sar_report_id                   UUID NOT NULL,
    seq_num                         INTEGER,
    activity_type_code              VARCHAR(50) NOT NULL,
    activity_type_other             VARCHAR(255),
    amount                          DECIMAL(19,2),
    product_instrument_description  VARCHAR(255),
    product_type                    VARCHAR(50),
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP,
    created_by                      VARCHAR(100),
    updated_by                      VARCHAR(100),
    version                         BIGINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (sar_report_id, activity_type_code),
    FOREIGN KEY (sar_report_id) REFERENCES sar_reports(id) ON DELETE CASCADE
);

-- =============================================
-- SAR BRANCHES (Part III)
-- =============================================
CREATE TABLE IF NOT EXISTS sar_branches (
    id                  UUID NOT NULL DEFAULT gen_random_uuid(),
    sar_report_id       UUID NOT NULL,
    seq_num             INTEGER,
    branch_name         VARCHAR(255),
    rssd_number         VARCHAR(20),
    address             VARCHAR(500),
    city                VARCHAR(100),
    state               VARCHAR(3),
    zip_code            VARCHAR(20),
    country             VARCHAR(3) DEFAULT 'US',
    is_primary          BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    version             BIGINT DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (sar_report_id) REFERENCES sar_reports(id) ON DELETE CASCADE
);

-- =============================================
-- EFILING BATCHES
-- =============================================
CREATE TABLE IF NOT EXISTS efiling_batches (
    id                          UUID NOT NULL DEFAULT gen_random_uuid(),
    batch_number                VARCHAR(50) UNIQUE NOT NULL,
    transmitter_name            VARCHAR(255) NOT NULL,
    transmitter_ein             VARCHAR(20) NOT NULL,
    transmitter_contact_name    VARCHAR(255),
    transmitter_contact_phone   VARCHAR(30),
    transmitter_contact_email   VARCHAR(255),
    xml_file_path               VARCHAR(1000),
    xml_file_name               VARCHAR(255),
    activity_count              INTEGER DEFAULT 0,
    status                      VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    generated_at                TIMESTAMP,
    submitted_at                TIMESTAMP,
    submission_attempt          INTEGER DEFAULT 0,
    fincen_tracking_id          VARCHAR(100),
    acknowledged_at             TIMESTAMP,
    acknowledgment_status       VARCHAR(5),
    acknowledgment_file_path    VARCHAR(1000),
    error_codes                 TEXT,
    error_description           TEXT,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    version                     BIGINT DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_efiling_batch_status ON efiling_batches(status);
CREATE INDEX idx_efiling_batch_tracking ON efiling_batches(fincen_tracking_id);

-- eFiling batch report IDs (element collection)
CREATE TABLE IF NOT EXISTS efiling_batch_reports (
    batch_id        UUID NOT NULL,
    sar_report_id   UUID NOT NULL,
    PRIMARY KEY (batch_id, sar_report_id),
    FOREIGN KEY (batch_id) REFERENCES efiling_batches(id) ON DELETE CASCADE
);

-- =============================================
-- AUDIT LOGS (Business-level audit events)
-- =============================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    sar_report_id   UUID,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       UUID,
    action          VARCHAR(50) NOT NULL,
    field_name      VARCHAR(100),
    old_value       TEXT,
    new_value       TEXT,
    performed_by    VARCHAR(100) NOT NULL,
    performed_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address      VARCHAR(45),
    session_id      VARCHAR(100),
    reason          TEXT,
    additional_data TEXT,
    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_logs_sar_report ON audit_logs(sar_report_id);
CREATE INDEX idx_audit_logs_performed_at ON audit_logs(performed_at);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);

-- =============================================
-- HIBERNATE ENVERS REVISION INFO
-- =============================================
CREATE TABLE IF NOT EXISTS revinfo (
    rev         INTEGER NOT NULL,
    revtstmp    BIGINT,
    PRIMARY KEY (rev)
);

CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

-- Audit tables (created by Hibernate Envers automatically, but defining here for clarity)
-- sar_reports_aud, sar_subjects_aud, sar_accounts_aud, sar_transactions_aud, etc.
-- These are created automatically by @Audited entities with Hibernate Envers

-- =============================================
-- SPRING BATCH TABLES (for eFiling batch jobs)
-- Spring Batch creates these automatically with initialize-schema: always
-- =============================================
