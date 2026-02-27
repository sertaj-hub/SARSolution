# Database Structure & ER Diagram

## Table of Contents
- [ER Diagram](#er-diagram)
- [Table Details](#table-details)
  - [sar_reports](#sar_reports)
  - [sar_subjects](#sar_subjects)
  - [sar_accounts](#sar_accounts)
  - [sar_transactions](#sar_transactions)
  - [sar_activity_types](#sar_activity_types)
  - [sar_branches](#sar_branches)
  - [efiling_batches](#efiling_batches)
  - [efiling_batch_reports](#efiling_batch_reports)
  - [audit_logs](#audit_logs)
  - [revinfo (Hibernate Envers)](#revinfo)
  - [Envers Audit Tables](#envers-audit-tables)
  - [Spring Batch Tables](#spring-batch-tables)
- [Indexes](#indexes)
- [Enum Reference Values](#enum-reference-values)

---

## ER Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              FinCEN SAR — Entity Relationship Diagram                │
└──────────────────────────────────────────────────────────────────────────────────────┘

                        ┌─────────────────────────────────┐
                        │           sar_reports            │
                        │─────────────────────────────────│
                        │ PK  id              UUID        │
                        │     report_number   VARCHAR(50) │
                        │     case_id         VARCHAR(255)│
                        │     status          VARCHAR(30) │  ◄── DRAFT/IN_REVIEW/APPROVED/
                        │     filing_status   VARCHAR(30) │       SUBMITTED/ACKNOWLEDGED...
                        │     batch_id        UUID        │  ──────────────────────────────┐
                        │     bsa_identifier  VARCHAR(50) │                                │
                        │     narrative       TEXT        │                                │
                        │     filing_institution_name     │                                │
                        │     ...                         │                                │
                        └──────────────┬──────────────────┘                               │
                                       │ PK=id                                            │
              ┌────────────────────────┼──────────────────────────┐                       │
              │                        │                          │                        │
              │ (1:N)                  │ (1:N)                    │ (1:N)                  │
              ▼                        ▼                          ▼                        │
  ┌─────────────────────┐  ┌────────────────────┐  ┌──────────────────────────┐          │
  │    sar_subjects     │  │  sar_activity_types │  │      sar_branches        │          │
  │─────────────────────│  │────────────────────│  │──────────────────────────│          │
  │ PK id      UUID     │  │ PK id       UUID   │  │ PK id           UUID     │          │
  │ FK sar_report_id    │  │ FK sar_report_id   │  │ FK sar_report_id UUID    │          │
  │    seq_num  INT     │  │    seq_num  INT    │  │    branch_name  VARCHAR  │          │
  │    is_entity BOOL   │  │    activity_type_  │  │    rssd_number  VARCHAR  │          │
  │    last_name VARCHAR│  │      code VARCHAR  │  │    address      VARCHAR  │          │
  │    first_name       │  │    amount  DECIMAL │  │    city/state/zip        │          │
  │    entity_name      │  │    ...             │  │    is_primary   BOOL     │          │
  │    id_type  VARCHAR │  └────────────────────┘  └──────────────────────────┘          │
  │    id_number VARCHAR│                                                                  │
  │    address/city/state                                                                  │
  │    role_code VARCHAR│                                                                  │
  │    ...              │                                                                  │
  └──────────┬──────────┘                                                                 │
             │ PK=id                                                                      │
             │ (1:N, optional FK from sar_accounts)                                       │
             ▼                                                                            │
  ┌───────────────────────────┐                                                           │
  │       sar_accounts        │                                                           │
  │───────────────────────────│                                                           │
  │ PK id              UUID   │                                                           │
  │ FK sar_report_id   UUID  ─┼──► sar_reports                                           │
  │ FK subject_id      UUID  ─┼──► sar_subjects (optional)                               │
  │    account_number  VARCHAR│                                                           │
  │    account_type    VARCHAR│                                                           │
  │    institution_name       │                                                           │
  │    routing_number  VARCHAR│                                                           │
  │    balance       DECIMAL  │                                                           │
  │    is_foreign_account BOOL│                                                           │
  │    ...                    │                                                           │
  └──────────┬────────────────┘                                                           │
             │ PK=id                                                                      │
             │ (1:N, optional FK from sar_transactions)                                   │
             ▼                                                                            │
  ┌───────────────────────────┐                                                           │
  │     sar_transactions      │                                                           │
  │───────────────────────────│                                                           │
  │ PK id              UUID   │                                                           │
  │ FK sar_report_id   UUID  ─┼──► sar_reports                                           │
  │ FK account_id      UUID  ─┼──► sar_accounts (optional)                               │
  │    transaction_date DATE   │                                                           │
  │    transaction_type VARCHAR│                                                           │
  │    amount        DECIMAL   │                                                           │
  │    direction       VARCHAR │                                                           │
  │    counterparty_name       │                                                           │
  │    structuring_flag  BOOL  │                                                           │
  │    ...                     │                                                           │
  └───────────────────────────┘                                                           │
                                                                                          │
  ┌────────────────────────────────────────┐                                              │
  │           efiling_batches              │  ◄───────────────────────────────────────────┘
  │────────────────────────────────────────│  (sar_reports.batch_id → efiling_batches.id)
  │ PK id                  UUID           │
  │    batch_number        VARCHAR(50)    │
  │    transmitter_name    VARCHAR(255)   │
  │    transmitter_ein     VARCHAR(20)    │
  │    xml_file_path       VARCHAR(1000)  │
  │    activity_count      INT            │
  │    status              VARCHAR(30)    │  ◄── PENDING/BATCH_GENERATED/SUBMITTED/
  │    fincen_tracking_id  VARCHAR(100)   │       ACCEPTED/REJECTED...
  │    acknowledgment_status VARCHAR(5)   │
  │    ...                                │
  └──────────────┬─────────────────────── ┘
                 │ PK=id
                 │ (1:N)
                 ▼
  ┌──────────────────────────────────┐
  │      efiling_batch_reports       │  (junction / element collection)
  │──────────────────────────────────│
  │ PK FK batch_id      UUID        ─┼──► efiling_batches
  │ PK    sar_report_id UUID         │    (FK; no FK enforced to sar_reports
  └──────────────────────────────────┘     since it's an @ElementCollection)

  ┌──────────────────────────────────────────────────────────────┐
  │                          audit_logs                           │  (no FK constraints —
  │──────────────────────────────────────────────────────────────│   soft reference)
  │ PK id              UUID                                      │
  │    sar_report_id   UUID  (soft ref → sar_reports)            │
  │    entity_type     VARCHAR(100)                              │
  │    entity_id       UUID                                      │
  │    action          VARCHAR(50)                               │
  │    field_name      VARCHAR(100)                              │
  │    old_value       TEXT                                      │
  │    new_value       TEXT                                      │
  │    performed_by    VARCHAR(100)                              │
  │    performed_at    TIMESTAMP                                 │
  │    ip_address      VARCHAR(45)                               │
  │    reason          TEXT                                      │
  └──────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────┐
  │                     revinfo  (Envers)                         │
  │──────────────────────────────────────────────────────────────│
  │ PK rev         INT (sequence: revinfo_seq)                   │
  │    revtstmp    BIGINT (epoch millis)                         │
  └──────────────────────────────────────────────────────────────┘
           │
           │ rev FK in every *_aud table
           ▼
  ┌──────────────────────────────────────────────────────────────┐
  │   sar_reports_aud  /  sar_subjects_aud  /  sar_accounts_aud  │
  │   sar_transactions_aud  /  sar_activity_types_aud  / ...      │
  │──────────────────────────────────────────────────────────────│
  │ PK id     UUID                                               │
  │ PK rev    INT ──────────────────────────────────► revinfo    │
  │    revtype SMALLINT  (0=INSERT, 1=UPDATE, 2=DELETE)          │
  │    <all audited columns mirrored from base table>            │
  └──────────────────────────────────────────────────────────────┘
```

---

## Relationship Summary

| Parent Table | Child Table | Cardinality | FK Column | On Delete |
|---|---|---|---|---|
| `sar_reports` | `sar_subjects` | 1 : N | `sar_report_id` | CASCADE |
| `sar_reports` | `sar_accounts` | 1 : N | `sar_report_id` | CASCADE |
| `sar_reports` | `sar_transactions` | 1 : N | `sar_report_id` | CASCADE |
| `sar_reports` | `sar_activity_types` | 1 : N | `sar_report_id` | CASCADE |
| `sar_reports` | `sar_branches` | 1 : N | `sar_report_id` | CASCADE |
| `sar_subjects` | `sar_accounts` | 1 : N | `subject_id` | RESTRICT (nullable) |
| `sar_accounts` | `sar_transactions` | 1 : N | `account_id` | RESTRICT (nullable) |
| `efiling_batches` | `efiling_batch_reports` | 1 : N | `batch_id` | CASCADE |
| `sar_reports` | `efiling_batches` | N : 1 | `batch_id` (soft) | — |
| `revinfo` | `*_aud` tables | 1 : N | `rev` | — |

---

## Table Details

### sar_reports

Core aggregate root. One row = one FinCEN SAR Activity.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | UUID | NOT NULL | `gen_random_uuid()` | **PK** |
| `report_number` | VARCHAR(50) | NOT NULL | — | Unique internal SAR number (auto-generated) |
| `case_id` | VARCHAR(255) | YES | — | Linked AML case management ID |
| `case_system` | VARCHAR(100) | YES | — | Source AML system name (e.g. NICE-Actimize) |
| `prior_bsa_identifier` | VARCHAR(50) | YES | — | BSA ID of prior filing (amendments only) |
| `bsa_identifier` | VARCHAR(50) | YES | — | BSA ID assigned by FinCEN upon acceptance |
| `status` | VARCHAR(30) | NOT NULL | `'DRAFT'` | SAR lifecycle status (see `SarStatus` enum) |
| `filing_status` | VARCHAR(30) | YES | — | eFiling submission status (see `FilingStatus` enum) |
| `activity_from_date` | DATE | YES | — | Start of suspicious activity period |
| `activity_to_date` | DATE | YES | — | End of suspicious activity period |
| `filing_date` | DATE | YES | — | Date SAR was filed |
| `continuing_activity` | BOOLEAN | YES | `FALSE` | True if activity is ongoing |
| `corrects_amends_prior` | BOOLEAN | YES | `FALSE` | True if this is an amendment |
| `joint_report` | BOOLEAN | YES | `FALSE` | True if jointly filed with another institution |
| `fi_noted_suspicious_activity` | BOOLEAN | YES | `TRUE` | Filing institution noted the activity |
| `total_suspicious_amount` | DECIMAL(19,2) | YES | — | Total dollar amount of suspicious activity |
| `no_amount_involved` | BOOLEAN | YES | `FALSE` | Check if no dollar amount involved |
| `narrative` | TEXT | YES | — | Part V — free-form narrative description |
| `batch_id` | UUID | YES | — | Soft reference to `efiling_batches.id` |
| `submitted_at` | TIMESTAMP | YES | — | When SAR was submitted to FinCEN |
| `acknowledged_at` | TIMESTAMP | YES | — | When FinCEN acknowledged receipt |
| `fincen_tracking_number` | VARCHAR(100) | YES | — | FinCEN-assigned tracking number |
| `rejection_reason` | TEXT | YES | — | FinCEN rejection reason (if rejected) |
| `filing_institution_name` | VARCHAR(255) | NOT NULL | — | Part III — filing institution name |
| `filing_institution_ein` | VARCHAR(20) | YES | — | Institution EIN / TIN |
| `filing_institution_type` | VARCHAR(50) | YES | — | Bank, MSB, Casino, etc. |
| `contact_office_name` | VARCHAR(255) | YES | — | Designated contact office (Part III) |
| `contact_phone` | VARCHAR(30) | YES | — | Contact phone number |
| `contact_email` | VARCHAR(255) | YES | — | Contact email address |
| `created_at` | TIMESTAMP | NOT NULL | `CURRENT_TIMESTAMP` | BaseEntity — auto-set on insert |
| `updated_at` | TIMESTAMP | YES | — | BaseEntity — auto-set on update |
| `created_by` | VARCHAR(100) | YES | — | BaseEntity — username from Spring Security |
| `updated_by` | VARCHAR(100) | YES | — | BaseEntity — username from Spring Security |
| `version` | BIGINT | YES | `0` | Optimistic locking (@Version) |

---

### sar_subjects

SAR Part I — subjects (individuals or entities) involved in suspicious activity.
Up to 999 subjects per SAR per BSA XML schema. Corresponds to BSA XML Party type 23.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | UUID | NOT NULL | `gen_random_uuid()` | **PK** |
| `sar_report_id` | UUID | NOT NULL | — | **FK** → `sar_reports(id)` CASCADE |
| `seq_num` | INTEGER | YES | — | Sequence number for XML generation (1-999) |
| `case_subject_id` | VARCHAR(255) | YES | — | Source AML system subject ID |
| `auto_populated` | BOOLEAN | YES | `FALSE` | True if pulled from AML case system |
| `is_entity` | BOOLEAN | YES | `FALSE` | False=individual, True=organization |
| `last_name` | VARCHAR(150) | YES | — | Individual: last name |
| `first_name` | VARCHAR(100) | YES | — | Individual: first name |
| `middle_name` | VARCHAR(100) | YES | — | Individual: middle name |
| `suffix` | VARCHAR(20) | YES | — | Individual: Jr., Sr., III, etc. |
| `date_of_birth` | DATE | YES | — | Individual: date of birth |
| `entity_name` | VARCHAR(255) | YES | — | Organization: legal entity name |
| `doing_business_as` | VARCHAR(255) | YES | — | DBA name |
| `id_type` | VARCHAR(30) | YES | — | `IdentificationTypeCode` enum value |
| `id_number` | VARCHAR(100) | YES | — | ID document number |
| `id_issue_state` | VARCHAR(3) | YES | — | 2-letter US state code |
| `id_issue_country` | VARCHAR(3) | YES | — | ISO 3166 country code |
| `address` | VARCHAR(500) | YES | — | Street address |
| `city` | VARCHAR(100) | YES | — | City |
| `state` | VARCHAR(3) | YES | — | 2-letter state code |
| `zip_code` | VARCHAR(20) | YES | — | ZIP / postal code |
| `country` | VARCHAR(3) | YES | `'US'` | ISO country code |
| `phone_number` | VARCHAR(30) | YES | — | Primary phone |
| `phone_extension` | VARCHAR(10) | YES | — | Phone extension |
| `email` | VARCHAR(255) | YES | — | Email address |
| `occupation` | VARCHAR(255) | YES | — | Occupation / business type |
| `naics_code` | VARCHAR(10) | YES | — | NAICS industry code |
| `role_code` | VARCHAR(10) | YES | — | `SubjectRoleCode` enum (CU, EM, OW, etc.) |
| `role_other_description` | VARCHAR(255) | YES | — | Free text when role_code=OT |
| `is_unknown` | BOOLEAN | YES | `FALSE` | True if subject identity unknown |
| `still_employed` | BOOLEAN | YES | — | For employee subjects |
| `corrective_action` | VARCHAR(500) | YES | — | Action taken against subject |
| `has_relationship_to_account` | BOOLEAN | YES | `FALSE` | Subject linked to reported account |
| `created_at` | TIMESTAMP | NOT NULL | `CURRENT_TIMESTAMP` | BaseEntity |
| `updated_at` | TIMESTAMP | YES | — | BaseEntity |
| `created_by` | VARCHAR(100) | YES | — | BaseEntity |
| `updated_by` | VARCHAR(100) | YES | — | BaseEntity |
| `version` | BIGINT | YES | `0` | Optimistic locking |

---

### sar_accounts

Account information linked to a SAR and optionally to a specific subject.
Corresponds to the `Account` element in BSA XML 2.0.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | UUID | NOT NULL | `gen_random_uuid()` | **PK** |
| `sar_report_id` | UUID | NOT NULL | — | **FK** → `sar_reports(id)` CASCADE |
| `subject_id` | UUID | YES | — | **FK** → `sar_subjects(id)` (nullable) |
| `case_account_id` | VARCHAR(255) | YES | — | Source AML system account ID |
| `auto_populated` | BOOLEAN | YES | `FALSE` | True if pulled from AML case system |
| `account_number` | VARCHAR(100) | YES | — | Account number |
| `account_number_closed` | BOOLEAN | YES | `FALSE` | True if account number is for a closed account |
| `account_type` | VARCHAR(50) | YES | — | Checking, Savings, Loan, CD, etc. |
| `product_type` | VARCHAR(50) | YES | — | FinCEN product type code |
| `institution_name` | VARCHAR(255) | YES | — | Financial institution name |
| `institution_ein` | VARCHAR(20) | YES | — | Institution EIN |
| `routing_number` | VARCHAR(20) | YES | — | ABA routing number |
| `opened_date` | DATE | YES | — | Account opening date |
| `closed_date` | DATE | YES | — | Account closure date |
| `balance` | DECIMAL(19,2) | YES | — | Account balance |
| `currency_code` | VARCHAR(5) | YES | `'USD'` | ISO 4217 currency code |
| `is_foreign_account` | BOOLEAN | YES | `FALSE` | True if a foreign account |
| `foreign_country` | VARCHAR(3) | YES | — | ISO country code (if foreign) |
| `action_account_closed` | BOOLEAN | YES | `FALSE` | Action: account closed |
| `action_account_frozen` | BOOLEAN | YES | `FALSE` | Action: account frozen |
| `no_action_taken` | BOOLEAN | YES | `TRUE` | No action taken on account |
| `created_at` | TIMESTAMP | NOT NULL | `CURRENT_TIMESTAMP` | BaseEntity |
| `updated_at` | TIMESTAMP | YES | — | BaseEntity |
| `created_by` | VARCHAR(100) | YES | — | BaseEntity |
| `updated_by` | VARCHAR(100) | YES | — | BaseEntity |
| `version` | BIGINT | YES | `0` | Optimistic locking |

---

### sar_transactions

Suspicious transaction records. Linked to a SAR and optionally to a specific account.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | UUID | NOT NULL | `gen_random_uuid()` | **PK** |
| `sar_report_id` | UUID | NOT NULL | — | **FK** → `sar_reports(id)` CASCADE |
| `account_id` | UUID | YES | — | **FK** → `sar_accounts(id)` (nullable) |
| `case_transaction_id` | VARCHAR(255) | YES | — | Source AML transaction ID |
| `alert_id` | VARCHAR(100) | YES | — | AML alert that triggered SAR |
| `auto_populated` | BOOLEAN | YES | `FALSE` | True if pulled from AML case system |
| `transaction_date` | DATE | YES | — | Date of transaction |
| `transaction_datetime` | TIMESTAMP | YES | — | Full timestamp (if available) |
| `transaction_type` | VARCHAR(50) | YES | — | Wire, ACH, Cash, Check, etc. |
| `transaction_type_other` | VARCHAR(100) | YES | — | Free text when type = "Other" |
| `amount` | DECIMAL(19,2) | YES | — | Transaction amount |
| `currency_code` | VARCHAR(5) | YES | `'USD'` | ISO 4217 currency code |
| `currency_foreign` | VARCHAR(5) | YES | — | Foreign currency code (if applicable) |
| `direction` | VARCHAR(10) | YES | — | CREDIT or DEBIT |
| `counterparty_name` | VARCHAR(255) | YES | — | Counterparty name |
| `counterparty_account` | VARCHAR(100) | YES | — | Counterparty account number |
| `counterparty_institution` | VARCHAR(255) | YES | — | Counterparty institution name |
| `counterparty_country` | VARCHAR(3) | YES | — | Counterparty country code |
| `location_type` | VARCHAR(50) | YES | — | Branch, ATM, Online, etc. |
| `location_description` | VARCHAR(255) | YES | — | Location detail |
| `structuring_flag` | BOOLEAN | YES | `FALSE` | Suspicious: structuring pattern |
| `rapid_movement_flag` | BOOLEAN | YES | `FALSE` | Suspicious: rapid movement of funds |
| `unusual_pattern_flag` | BOOLEAN | YES | `FALSE` | Suspicious: unusual pattern |
| `suspicion_notes` | TEXT | YES | — | Analyst notes on this transaction |
| `reference_number` | VARCHAR(100) | YES | — | Internal reference number |
| `check_number` | VARCHAR(50) | YES | — | Check number (if applicable) |
| `created_at` | TIMESTAMP | NOT NULL | `CURRENT_TIMESTAMP` | BaseEntity |
| `updated_at` | TIMESTAMP | YES | — | BaseEntity |
| `created_by` | VARCHAR(100) | YES | — | BaseEntity |
| `updated_by` | VARCHAR(100) | YES | — | BaseEntity |
| `version` | BIGINT | YES | `0` | Optimistic locking |

---

### sar_activity_types

SAR Part II — suspicious activity category codes per FinCENReferenceCodes.xsd.
One SAR can have multiple activity types. The pair `(sar_report_id, activity_type_code)` is unique.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | UUID | NOT NULL | `gen_random_uuid()` | **PK** |
| `sar_report_id` | UUID | NOT NULL | — | **FK** → `sar_reports(id)` CASCADE |
| `seq_num` | INTEGER | YES | — | Sequence number for XML generation |
| `activity_type_code` | VARCHAR(50) | NOT NULL | — | `ActivityTypeCode` enum name (e.g. STRUCTURING) |
| `activity_type_other` | VARCHAR(255) | YES | — | Free text when code=OTHER |
| `amount` | DECIMAL(19,2) | YES | — | Dollar amount for this activity type |
| `product_instrument_description` | VARCHAR(255) | YES | — | Product / instrument involved |
| `product_type` | VARCHAR(50) | YES | — | FinCEN product type code |
| `created_at` | TIMESTAMP | NOT NULL | `CURRENT_TIMESTAMP` | BaseEntity |
| `updated_at` | TIMESTAMP | YES | — | BaseEntity |
| `created_by` | VARCHAR(100) | YES | — | BaseEntity |
| `updated_by` | VARCHAR(100) | YES | — | BaseEntity |
| `version` | BIGINT | YES | `0` | Optimistic locking |

**Unique constraint:** `UNIQUE (sar_report_id, activity_type_code)`

---

### sar_branches

SAR Part III — branches where suspicious activity occurred.
Corresponds to BSA XML Party type 34.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | UUID | NOT NULL | `gen_random_uuid()` | **PK** |
| `sar_report_id` | UUID | NOT NULL | — | **FK** → `sar_reports(id)` CASCADE |
| `seq_num` | INTEGER | YES | — | Sequence number for XML generation |
| `branch_name` | VARCHAR(255) | YES | — | Branch name |
| `rssd_number` | VARCHAR(20) | YES | — | Federal Reserve RSSD number |
| `address` | VARCHAR(500) | YES | — | Street address |
| `city` | VARCHAR(100) | YES | — | City |
| `state` | VARCHAR(3) | YES | — | 2-letter state code |
| `zip_code` | VARCHAR(20) | YES | — | ZIP code |
| `country` | VARCHAR(3) | YES | `'US'` | ISO country code |
| `is_primary` | BOOLEAN | YES | `FALSE` | True = primary activity branch |
| `created_at` | TIMESTAMP | NOT NULL | `CURRENT_TIMESTAMP` | BaseEntity |
| `updated_at` | TIMESTAMP | YES | — | BaseEntity |
| `created_by` | VARCHAR(100) | YES | — | BaseEntity |
| `updated_by` | VARCHAR(100) | YES | — | BaseEntity |
| `version` | BIGINT | YES | `0` | Optimistic locking |

---

### efiling_batches

One batch = one BSA XML file submitted to FinCEN. Contains multiple SAR activities.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | UUID | NOT NULL | `gen_random_uuid()` | **PK** |
| `batch_number` | VARCHAR(50) | NOT NULL | — | Unique batch identifier |
| `transmitter_name` | VARCHAR(255) | NOT NULL | — | BSA XML transmitter name |
| `transmitter_ein` | VARCHAR(20) | NOT NULL | — | Transmitter EIN |
| `transmitter_contact_name` | VARCHAR(255) | YES | — | Contact person name |
| `transmitter_contact_phone` | VARCHAR(30) | YES | — | Contact phone |
| `transmitter_contact_email` | VARCHAR(255) | YES | — | Contact email |
| `xml_file_path` | VARCHAR(1000) | YES | — | Full path to generated BSA XML file |
| `xml_file_name` | VARCHAR(255) | YES | — | Filename of generated XML |
| `activity_count` | INTEGER | YES | `0` | Number of SAR activities in batch |
| `status` | VARCHAR(30) | NOT NULL | `'PENDING'` | `FilingStatus` enum value |
| `generated_at` | TIMESTAMP | YES | — | When XML was generated |
| `submitted_at` | TIMESTAMP | YES | — | When batch was submitted to FinCEN |
| `submission_attempt` | INTEGER | YES | `0` | Retry count |
| `fincen_tracking_id` | VARCHAR(100) | YES | — | FinCEN-assigned tracking ID |
| `acknowledged_at` | TIMESTAMP | YES | — | When ACK was received |
| `acknowledgment_status` | VARCHAR(5) | YES | — | `A`=Accepted, `R`=Rejected |
| `acknowledgment_file_path` | VARCHAR(1000) | YES | — | Path to FinCEN ACK XML file |
| `error_codes` | TEXT | YES | — | Comma-separated FinCEN error codes |
| `error_description` | TEXT | YES | — | Human-readable error description |
| `created_at` | TIMESTAMP | NOT NULL | `CURRENT_TIMESTAMP` | BaseEntity |
| `updated_at` | TIMESTAMP | YES | — | BaseEntity |
| `created_by` | VARCHAR(100) | YES | — | BaseEntity |
| `updated_by` | VARCHAR(100) | YES | — | BaseEntity |
| `version` | BIGINT | YES | `0` | Optimistic locking |

---

### efiling_batch_reports

Junction table mapping which SAR reports are included in which eFiling batch.
Backed by `@ElementCollection` on `EFilingBatch`.

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `batch_id` | UUID | NOT NULL | **PK + FK** → `efiling_batches(id)` CASCADE |
| `sar_report_id` | UUID | NOT NULL | **PK** — SAR report UUID (soft reference) |

---

### audit_logs

Business-level audit events. No FK constraints — survives entity deletion.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | UUID | NOT NULL | `gen_random_uuid()` | **PK** |
| `sar_report_id` | UUID | YES | — | Soft reference to `sar_reports.id` |
| `entity_type` | VARCHAR(100) | NOT NULL | — | e.g. `SarReport`, `SarSubject` |
| `entity_id` | UUID | YES | — | ID of the affected entity |
| `action` | VARCHAR(50) | NOT NULL | — | `CREATE`, `UPDATE`, `DELETE`, `STATUS_CHANGE`, `EFILING_SUBMITTED`, `FINCEN_ACKNOWLEDGED` |
| `field_name` | VARCHAR(100) | YES | — | Specific field changed (for UPDATE events) |
| `old_value` | TEXT | YES | — | Previous field value |
| `new_value` | TEXT | YES | — | New field value |
| `performed_by` | VARCHAR(100) | NOT NULL | — | Username who performed the action |
| `performed_at` | TIMESTAMP | NOT NULL | `CURRENT_TIMESTAMP` | When the action occurred |
| `ip_address` | VARCHAR(45) | YES | — | Requester IP (IPv4 or IPv6) |
| `session_id` | VARCHAR(100) | YES | — | HTTP session ID |
| `reason` | TEXT | YES | — | Justification (e.g. rejection reason) |
| `additional_data` | TEXT | YES | — | JSON blob for extra context |

---

### revinfo

Hibernate Envers revision metadata table. One row per database transaction that modifies an `@Audited` entity.

| Column | Type | Description |
|--------|------|-------------|
| `rev` | INTEGER | **PK** — sequence from `revinfo_seq` |
| `revtstmp` | BIGINT | Epoch milliseconds timestamp of revision |

Sequence: `revinfo_seq` (starts at 1, increments by 50)

---

### Envers Audit Tables

Created automatically by Hibernate Envers for each `@Audited` entity. Mirror all base table columns plus:

| Extra Column | Type | Description |
|---|---|---|
| `rev` | INTEGER | **PK** + FK → `revinfo(rev)` |
| `revtype` | SMALLINT | `0`=INSERT, `1`=UPDATE, `2`=DELETE |

| Audit Table | Mirrors |
|---|---|
| `sar_reports_aud` | `sar_reports` |
| `sar_subjects_aud` | `sar_subjects` |
| `sar_accounts_aud` | `sar_accounts` |
| `sar_transactions_aud` | `sar_transactions` |
| `sar_activity_types_aud` | `sar_activity_types` |
| `sar_branches_aud` | `sar_branches` |
| `efiling_batches_aud` | `efiling_batches` |

---

### Spring Batch Tables

Auto-created by Spring Batch (`initialize-schema: always`). Used by the eFiling batch job.

| Table | Purpose |
|-------|---------|
| `BATCH_JOB_INSTANCE` | One row per unique job+parameters combination |
| `BATCH_JOB_EXECUTION` | One row per job execution attempt |
| `BATCH_JOB_EXECUTION_PARAMS` | Job parameters for each execution |
| `BATCH_STEP_EXECUTION` | One row per step execution |
| `BATCH_JOB_EXECUTION_CONTEXT` | Serialized job-level `ExecutionContext` |
| `BATCH_STEP_EXECUTION_CONTEXT` | Serialized step-level `ExecutionContext` |
| `BATCH_JOB_SEQ` | Job instance ID sequence |
| `BATCH_JOB_EXECUTION_SEQ` | Job execution ID sequence |
| `BATCH_STEP_EXECUTION_SEQ` | Step execution ID sequence |

---

## Indexes

| Index | Table | Column(s) | Purpose |
|-------|-------|-----------|---------|
| `idx_sar_reports_status` | `sar_reports` | `status` | Filter by lifecycle status |
| `idx_sar_reports_case_id` | `sar_reports` | `case_id` | Look up SAR by AML case ID |
| `idx_sar_reports_batch_id` | `sar_reports` | `batch_id` | Find SARs in a batch |
| `idx_sar_reports_filing_date` | `sar_reports` | `filing_date` | Date-range queries |
| `idx_sar_reports_bsa_id` | `sar_reports` | `bsa_identifier` | Look up by FinCEN BSA ID |
| `idx_sar_subjects_report` | `sar_subjects` | `sar_report_id` | Subjects per report |
| `idx_sar_accounts_report` | `sar_accounts` | `sar_report_id` | Accounts per report |
| `idx_sar_accounts_subject` | `sar_accounts` | `subject_id` | Accounts per subject |
| `idx_sar_transactions_report` | `sar_transactions` | `sar_report_id` | Transactions per report |
| `idx_sar_transactions_alert` | `sar_transactions` | `alert_id` | Look up by AML alert |
| `idx_efiling_batch_status` | `efiling_batches` | `status` | Filter batches by status |
| `idx_efiling_batch_tracking` | `efiling_batches` | `fincen_tracking_id` | Look up FinCEN tracking ID |
| `idx_audit_logs_sar_report` | `audit_logs` | `sar_report_id` | Audit trail for a SAR |
| `idx_audit_logs_performed_at` | `audit_logs` | `performed_at` | Time-range audit queries |
| `idx_audit_logs_action` | `audit_logs` | `action` | Filter by action type |

---

## Enum Reference Values

### SarStatus — `sar_reports.status`

| Value | Description |
|-------|-------------|
| `DRAFT` | Under preparation by analyst |
| `IN_REVIEW` | Submitted for compliance officer review |
| `APPROVED` | Approved — ready for eFiling |
| `SUBMITTED` | Batch sent to FinCEN BSA E-Filing |
| `ACKNOWLEDGED` | FinCEN acceptance confirmed |
| `REJECTED` | FinCEN returned filing with errors |
| `AMENDED` | Correction submitted referencing prior BSA ID |
| `CLOSED` | Closed without filing |

### FilingStatus — `efiling_batches.status` / `sar_reports.filing_status`

| Value | Description |
|-------|-------------|
| `PENDING` | Awaiting batch processing |
| `BATCH_GENERATED` | BSA XML file generated |
| `SUBMITTED` | Submitted to FinCEN BSA E-Filing |
| `PROCESSING` | Being processed by FinCEN |
| `ACCEPTED` | Accepted — BSA Identifier assigned |
| `REJECTED` | Rejected with FinCEN error codes |
| `PARTIALLY_ACCEPTED` | Partially accepted with warnings |

### IdentificationTypeCode — `sar_subjects.id_type`

| Enum Name | FinCEN Code | Description |
|-----------|-------------|-------------|
| `ALIEN_REGISTRATION` | 1 | Alien registration number |
| `EIN` | 2 | Employer Identification Number |
| `DRIVERS_LICENSE` | 3 | Driver's license / State ID |
| `FOREIGN_ID` | 4 | Foreign identification |
| `SSN_ITIN` | 5 | Social Security / ITIN |
| `PASSPORT` | 6 | Passport |
| `OTHER` | 7 | Other |
| `NATIONAL_ID` | 8 | National ID number |
| `UNKNOWN` | 999 | Unknown |

### SubjectRoleCode — `sar_subjects.role_code`

| Enum Name | Code | Description |
|-----------|------|-------------|
| `ACCOUNTANT` | A | Accountant |
| `AGENT` | AG | Agent |
| `APPRAISER` | AP | Appraiser |
| `ATTORNEY` | AT | Attorney |
| `BORROWER` | BO | Borrower |
| `BROKER_DEALER` | BD | Broker-dealer |
| `CUSTOMER` | CU | Customer |
| `DIRECTOR` | DI | Director |
| `EMPLOYEE` | EM | Employee |
| `LOAN_APPLICANT` | LA | Loan applicant |
| `OFFICER` | OF | Officer |
| `OTHER` | OT | Other |
| `OWNER` | OW | Owner |
| `SHAREHOLDER` | SH | Shareholder |

### ActivityTypeCode — `sar_activity_types.activity_type_code`

| Enum Name | FinCEN Code | Category | Description |
|-----------|-------------|----------|-------------|
| `BRIBERY_GRATUITY` | 1 | Fraud | Bribery/gratuity |
| `CHECK_FRAUD` | 2 | Fraud | Check fraud |
| `CHECK_KITING` | 3 | Fraud | Check kiting |
| `COMMERCIAL_LOAN_FRAUD` | 4 | Fraud | Commercial loan fraud |
| `CONSUMER_LOAN_FRAUD` | 5 | Fraud | Consumer loan fraud |
| `COUNTERFEIT_CHECK` | 6 | Fraud | Counterfeit check |
| `COUNTERFEIT_CREDIT_DEBIT_CARD` | 7 | Fraud | Counterfeit credit/debit card |
| `COUNTERFEIT_INSTRUMENT_OTHER` | 8 | Fraud | Counterfeit instrument (other) |
| `CREDIT_CARD_FRAUD` | 9 | Fraud | Credit card fraud |
| `DEBIT_CARD_FRAUD` | 10 | Fraud | Debit card fraud |
| `DEFALCATION_EMBEZZLEMENT` | 11 | Fraud | Defalcation/embezzlement |
| `FALSE_STATEMENT` | 12 | Fraud | False statement |
| `MISUSE_OF_POSITION` | 13 | Fraud | Misuse of position or self-dealing |
| `MORTGAGE_LOAN_FRAUD` | 14 | Fraud | Mortgage loan fraud |
| `MYSTERIOUS_DISAPPEARANCE` | 15 | Fraud | Mysterious disappearance |
| `WIRE_TRANSFER_FRAUD` | 16 | Fraud | Wire transfer fraud |
| `FRAUD_OTHER` | 17 | Fraud | Other (Fraud) |
| `IDENTIFICATION_DOCUMENTATION` | 18 | Money Laundering | Identification documentation |
| `MONEY_LAUNDERING` | 19 | Money Laundering | Money laundering |
| `MONEY_LAUNDERING_OTHER` | 20 | Money Laundering | Other (Money laundering) |
| `TERRORIST_FINANCING` | 21 | Terrorist Financing | Terrorist financing |
| `TERRORIST_FINANCING_OTHER` | 22 | Terrorist Financing | Other (Terrorist financing) |
| `GAMING_ACTIVITIES` | 23 | Structuring | Gaming activities |
| `STRUCTURING` | 24 | Structuring | Structuring |
| `TRANSACTIONS_BELOW_THRESHOLD` | 25 | Structuring | Transaction(s) below BSA threshold |
| `TWO_OR_MORE_INDIVIDUALS` | 26 | Structuring | Two or more individuals working together |
| `UNUSUAL_MULTIPLE_TRANSACTION_TYPES` | 27 | Structuring | Unusual use of multiple transaction types |
| `STRUCTURING_ML_OTHER` | 28 | Structuring | Other (Structuring/Money laundering) |
| `ACCOUNT_TAKEOVER` | 29 | Cyber/Identity | Account takeover |
| `COMPUTER_INTRUSION` | 30 | Cyber/Identity | Computer intrusion |
| `CREDIT_DEBIT_CARD_THEFT` | 31 | Cyber/Identity | Credit/debit card theft |
| `DEBIT_CARD_FRAUD_OTHER` | 32 | Cyber/Identity | Debit card fraud (other) |
| `ELDER_FINANCIAL_EXPLOITATION` | 33 | Cyber/Identity | Elder financial exploitation |
| `EMAIL_COMPROMISE` | 34 | Cyber/Identity | E-mail compromise / e-mail related fraud |
| `EXTORTION_BLACKMAIL` | 35 | Cyber/Identity | Extortion/blackmail |
| `FALSE_POLICE_REPORT` | 36 | Cyber/Identity | False police report |
| `HOME_EQUITY_FRAUD` | 37 | Cyber/Identity | Home equity loan/line fraud |
| `HUMAN_TRAFFICKING` | 38 | Cyber/Identity | Human trafficking |
| `IDENTITY_THEFT` | 39 | Cyber/Identity | Identity theft |
| `ILLICIT_MARKETPLACE` | 40 | Cyber/Identity | Illicit marketplace |
| `INSURANCE_FRAUD` | 41 | Cyber/Identity | Insurance fraud |
| `INVESTMENT_FRAUD` | 42 | Cyber/Identity | Investment fraud |
| `LOTTERY_SWEEPSTAKES_SCAM` | 43 | Cyber/Identity | Lottery/sweepstakes scams |
| `MALWARE_RANSOMWARE` | 44 | Cyber/Identity | Malware/ransomware |
| `MASS_MARKETING_FRAUD` | 45 | Cyber/Identity | Mass marketing fraud |
| `PONZI_PYRAMID_SCHEME` | 46 | Cyber/Identity | Ponzi scheme/pyramid scheme |
| `ROMANCE_FRAUD` | 47 | Cyber/Identity | Romance fraud |
| `SECURITIES_FRAUD` | 48 | Cyber/Identity | Securities fraud |
| `SOCIAL_ENGINEERING` | 49 | Cyber/Identity | Social engineering |
| `TAX_REFUND_FRAUD` | 50 | Cyber/Identity | Tax refund fraud |
| `TELEMARKETING_PHONE_FRAUD` | 51 | Cyber/Identity | Telemarketing/phone fraud |
| `TRADE_BASED_ML` | 52 | Cyber/Identity | Trade-based money laundering / BMPE |
