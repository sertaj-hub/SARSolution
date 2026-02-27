# FinCEN SAR Solution

A **Spring Boot Modular Monolith** for end-to-end FinCEN Suspicious Activity Report (SAR) filing. Built to integrate with AML case management systems, auto-populate SAR forms, maintain full audit history, and submit BSA XML batch files to FinCEN's BSA E-Filing system.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Module Breakdown](#module-breakdown)
- [Technology Stack](#technology-stack)
- [FinCEN XSD Schemas](#fincen-xsd-schemas)
- [SAR Lifecycle](#sar-lifecycle)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Database](#database)
- [API Reference](#api-reference)
- [AML Case Integration](#aml-case-integration)
- [eFiling Workflow](#efiling-workflow)
- [Audit History](#audit-history)
- [Security](#security)
- [Project Structure](#project-structure)

---

## Overview

The SAR Solution automates the complete SAR filing workflow required by the Bank Secrecy Act (BSA):

1. **Create** SAR reports (manually or from AML case management data)
2. **Auto-populate** subjects, accounts, and transactions from connected AML systems
3. **Review & approve** through a compliance workflow
4. **Validate** generated XML against official FinCEN XSD schemas
5. **Submit** BSA XML batch files to FinCEN BSA E-Filing via Spring Batch
6. **Track** FinCEN acknowledgments and BSA Identifiers
7. **Audit** every change with full field-level revision history

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      SAR Solution (Monolith)                     │
│                                                                  │
│  ┌──────────┐  ┌──────────────────┐  ┌──────────────────────┐   │
│  │  sar-api │  │ sar-case-        │  │    sar-efiling        │   │
│  │  REST    │  │ integration      │  │                       │   │
│  │  Controllers│ AML CMS Client  │  │  FinCEN XML Generator  │   │
│  │  OpenAPI │  │  Auto-populate   │  │  XSD Validator        │   │
│  └────┬─────┘  └────────┬─────────┘  │  Spring Batch Job     │   │
│       │                 │            │  Acknowledgment Parser │   │
│  ┌────▼─────────────────▼──────┐     └──────────┬────────────┘   │
│  │         sar-form            │                │                 │
│  │   SAR Lifecycle Service     │                │                 │
│  │   Form Validation           │                │                 │
│  └────────────────┬────────────┘                │                 │
│                   │                             │                 │
│  ┌────────────────▼─────────────────────────────▼──────────────┐ │
│  │                        sar-core                              │ │
│  │  Domain Entities · Repositories · Enums · Exceptions        │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                       sar-audit                           │   │
│  │   Hibernate Envers (field-level) · Business Event Log    │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
         │                          │
         ▼                          ▼
  ┌─────────────┐          ┌─────────────────────┐
  │  H2 / PostgreSQL       │ FinCEN BSA E-Filing  │
  │  Database   │          │ (sandbox/production) │
  └─────────────┘          └─────────────────────┘
```

---

## Module Breakdown

| Module | Package | Responsibility |
|--------|---------|----------------|
| `sar-core` | `com.fincen.sar.core` | Domain entities, JPA repositories, enums (FinCEN codes), shared exceptions |
| `sar-case-integration` | `com.fincen.sar.caseintegration` | AML case management REST client, auto-population of subjects/accounts/transactions |
| `sar-form` | `com.fincen.sar.form` | SAR lifecycle management, form validation, status transitions |
| `sar-audit` | `com.fincen.sar.audit` | Hibernate Envers revision history + business-level audit event logging |
| `sar-efiling` | `com.fincen.sar.efiling` | BSA XML generation, FinCEN XSD validation, Spring Batch job, FinCEN submission |
| `sar-api` | `com.fincen.sar.api` | REST controllers, global exception handler, OpenAPI documentation |
| `sar-app` | `com.fincen.sar.app` | Application entry point, Spring Security config, Flyway migrations |

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Framework | Spring Boot 3.2.3 |
| Language | Java 17 |
| Persistence | Spring Data JPA + Hibernate |
| Audit | Hibernate Envers |
| Batch Processing | Spring Batch |
| Database (Dev) | H2 In-Memory |
| Database (Prod) | PostgreSQL |
| Schema Migrations | Flyway |
| Object Mapping | MapStruct |
| Code Generation | Lombok |
| XML Generation | Java DOM API (javax.xml) |
| XSD Validation | Java SchemaFactory (javax.xml.validation) |
| Security | Spring Security (Basic Auth — extensible to JWT/OAuth2) |
| API Documentation | SpringDoc OpenAPI 3.0 / Swagger UI |
| Build | Maven (multi-module) |

---

## FinCEN XSD Schemas

The application validates and generates XML against official FinCEN schemas:

| Schema | URL | Purpose |
|--------|-----|---------|
| `EFL_SARXBatchSchema.xsd` | `https://www.fincen.gov/base/EFL_SARXBatchSchema.xsd` | Primary batch file structure |
| `BSA_XML_2.0.xsd` | `https://www.fincen.gov/base/BSA_XML_2.0.xsd` | BSA data element definitions |
| `FinCENReferenceCodes.xsd` | `https://www.fincen.gov/system/files/schema/code/FinCENReferenceCodes.xsd` | Reference code validation (SuspiciousActivitySubtypeID, PartyIdentificationTypeCode, etc.) |

> Per FinCEN BSA E-Filing guidance, `BSA_XML_2.0.xsd` and `FinCENReferenceCodes.xsd` must be co-located with batch files for schema validation. The `FinCenXsdValidator` loads these schemas from FinCEN's online URLs at validation time.

### FinCEN Party Type Codes (BSA XML 2.0)

| Code | Type |
|------|------|
| 8 | Transmitter |
| 9 | Transmitter Contact |
| 35 | Filing Institution |
| 46 | Designated Contact Office |
| 33 | Financial Institution Where Activity Occurred |
| 34 | Branch Where Activity Occurred |
| 23 | Subject |

---

## SAR Lifecycle

```
                ┌──────────────────────────────────────┐
                │         SAR Status Flow               │
                └──────────────────────────────────────┘

  [Create SAR] ──► DRAFT ──► IN_REVIEW ──► APPROVED ──► SUBMITTED ──► ACKNOWLEDGED
                     │            │                                          │
                     │    (reject)│                                          │
                     │            └──────────────────────► DRAFT             │
                     │                                                       │
                     └──────────────── AMENDED ◄─────────────────────────────┘
                                      (correction)
```

| Transition | Trigger | Actor |
|-----------|---------|-------|
| `DRAFT → IN_REVIEW` | `POST /sar-reports/{id}/submit-for-review` | ANALYST |
| `IN_REVIEW → APPROVED` | `POST /sar-reports/{id}/approve` | COMPLIANCE_OFFICER |
| `IN_REVIEW → DRAFT` | `POST /sar-reports/{id}/reject?reason=...` | COMPLIANCE_OFFICER |
| `APPROVED → SUBMITTED` | Spring Batch eFiling job | System/ADMIN |
| `SUBMITTED → ACKNOWLEDGED` | FinCEN acknowledgment processing | System |
| `ACKNOWLEDGED → AMENDED` | Create amendment with prior BSA ID | ANALYST |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Build

```bash
git clone <repository-url>
cd SARSolution
mvn clean package -DskipTests
```

### Run (H2 Development Mode)

```bash
cd sar-app
mvn spring-boot:run
```

Or with the packaged jar:

```bash
java -jar sar-app/target/sar-app-1.0.0-SNAPSHOT.jar
```

The application starts on **`http://localhost:8080`**.

### Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html

# H2 Console (dev only)
open http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:sardb
# Username: sa / Password: (empty)
```

---

## Configuration

All properties are in `sar-app/src/main/resources/application.yml`. Key settings:

### eFiling

```yaml
sar:
  efiling:
    mode: offline           # offline | sandbox | production
    transmitter:
      name: My Institution
      ein: 12-3456789
      contact-name: John Doe
      contact-phone: "5551234567"
      contact-email: bsa@institution.com
    fincen:
      submission-url: https://bsaefiling-sandbox.fincen.gov/api/v1/batch
      api-key: ${FINCEN_API_KEY}
      institution-id: ${FINCEN_INSTITUTION_ID}
    batch:
      output-dir: /var/sar-efiling/batches
      max-reports-per-batch: 100
    validation:
      use-online-schema: true     # Validate against FinCEN XSD before filing
    scheduler:
      enabled: true
      cron: "0 0 2 * * ?"         # Daily at 2:00 AM
      poll-interval-ms: 1800000   # Poll acknowledgments every 30 min
```

### AML Case Management Integration

```yaml
sar:
  case-management:
    type: rest                              # rest | mock
    base-url: https://your-aml-system.com
    system-name: NICE-Actimize
    api-key: ${CMS_API_KEY}
    connect-timeout: 5000
    read-timeout: 15000
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | JDBC connection URL | H2 in-memory |
| `DB_USER` | Database username | `sa` |
| `DB_PASS` | Database password | _(empty)_ |
| `FINCEN_API_KEY` | FinCEN BSA E-Filing API key | _(empty)_ |
| `FINCEN_INSTITUTION_ID` | Institution ID for FinCEN | _(empty)_ |
| `EFILING_MODE` | `offline` / `sandbox` / `production` | `offline` |
| `CMS_BASE_URL` | AML case management system URL | `http://localhost:8090` |
| `CMS_API_KEY` | AML system API key | _(empty)_ |
| `TRANSMITTER_EIN` | Institution EIN for BSA XML transmitter | _(empty)_ |
| `SCHEDULER_ENABLED` | Enable scheduled eFiling batch | `false` |
| `LOG_LEVEL` | Application log level | `INFO` |

---

## Database

### Development — H2 (default)

No setup required. H2 runs in-memory on application start.

```
JDBC URL: jdbc:h2:mem:sardb
Username: sa
Password: (empty)
```

### Production — PostgreSQL

1. Create the database:

```sql
CREATE DATABASE sardb;
CREATE USER saruser WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE sardb TO saruser;
```

2. Activate the PostgreSQL profile:

```bash
export SPRING_PROFILES_ACTIVE=postgres
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=sardb
export DB_USER=saruser
export DB_PASS=yourpassword
```

Flyway automatically runs `V1__initial_schema.sql` on first startup, creating all tables.

### Schema Overview

| Table | Description |
|-------|-------------|
| `sar_reports` | Core SAR report aggregate |
| `sar_subjects` | SAR Part I — subject information (up to 999 per report) |
| `sar_accounts` | Account information linked to subjects |
| `sar_transactions` | Suspicious transactions linked to accounts |
| `sar_activity_types` | SAR Part II — suspicious activity subtypes |
| `sar_branches` | SAR Part III — branch where activity occurred |
| `efiling_batches` | FinCEN BSA E-Filing batch tracking |
| `efiling_batch_reports` | Batch ↔ SAR report mapping |
| `audit_logs` | Business-level audit events |
| `revinfo` | Hibernate Envers revision table |
| `sar_reports_aud` | Envers: SAR report field-level history |
| `sar_subjects_aud` | Envers: Subject field-level history |

---

## API Reference

Full interactive API docs: **`http://localhost:8080/swagger-ui.html`**

### SAR Reports

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| `POST` | `/api/v1/sar-reports` | Create new SAR (DRAFT) | ANALYST |
| `GET` | `/api/v1/sar-reports` | List SARs (paginated, filter by status) | All |
| `GET` | `/api/v1/sar-reports/{id}` | Get SAR by ID | All |
| `GET` | `/api/v1/sar-reports/by-number/{number}` | Get SAR by report number | All |
| `PUT` | `/api/v1/sar-reports/{id}` | Update SAR fields | ANALYST |
| `POST` | `/api/v1/sar-reports/{id}/submit-for-review` | DRAFT → IN_REVIEW | ANALYST |
| `POST` | `/api/v1/sar-reports/{id}/approve` | IN_REVIEW → APPROVED | COMPLIANCE_OFFICER |
| `POST` | `/api/v1/sar-reports/{id}/reject` | IN_REVIEW → DRAFT | COMPLIANCE_OFFICER |
| `POST` | `/api/v1/sar-reports/{id}/populate-from-case` | Auto-populate from AML case | ANALYST |
| `POST` | `/api/v1/sar-reports/{id}/subjects` | Add subject (Part I) | ANALYST |
| `PUT` | `/api/v1/sar-reports/{id}/subjects/{subjectId}` | Update subject | ANALYST |
| `DELETE` | `/api/v1/sar-reports/{id}/subjects/{subjectId}` | Remove subject | ANALYST |
| `POST` | `/api/v1/sar-reports/{id}/accounts` | Add account | ANALYST |
| `DELETE` | `/api/v1/sar-reports/{id}/accounts/{accountId}` | Remove account | ANALYST |
| `POST` | `/api/v1/sar-reports/{id}/transactions` | Add transaction | ANALYST |
| `DELETE` | `/api/v1/sar-reports/{id}/transactions/{txnId}` | Remove transaction | ANALYST |
| `GET` | `/api/v1/sar-reports/{id}/audit-history` | Get audit log | All |
| `GET` | `/api/v1/sar-reports/{id}/revision-history` | Get Envers revisions | COMPLIANCE_OFFICER |

### eFiling

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| `POST` | `/api/v1/efiling/batches/trigger` | Manually trigger batch job | COMPLIANCE_OFFICER |
| `GET` | `/api/v1/efiling/batches` | List all batches | All |
| `GET` | `/api/v1/efiling/batches/{batchId}` | Get batch details | All |
| `POST` | `/api/v1/efiling/batches/{batchId}/resubmit` | Resubmit rejected batch | COMPLIANCE_OFFICER |
| `POST` | `/api/v1/efiling/batches/{batchId}/process-acknowledgment` | Process FinCEN ACK | COMPLIANCE_OFFICER |
| `POST` | `/api/v1/efiling/batches/poll-acknowledgments` | Poll FinCEN for ACKs | COMPLIANCE_OFFICER |
| `GET` | `/api/v1/efiling/reports/{sarId}/preview-xml` | Preview generated BSA XML | COMPLIANCE_OFFICER |
| `POST` | `/api/v1/efiling/reports/{sarId}/validate-xml` | Validate XML against FinCEN XSDs | COMPLIANCE_OFFICER |

### Example: Create SAR Report

```bash
curl -X POST http://localhost:8080/api/v1/sar-reports \
  -u analyst:analyst123 \
  -H "Content-Type: application/json" \
  -d '{
    "filingInstitutionName": "First National Bank",
    "filingInstitutionEin": "12-3456789",
    "filingInstitutionType": "Bank",
    "contactOfficeName": "BSA Compliance",
    "contactPhone": "5551234567",
    "contactEmail": "bsa@fnb.com",
    "caseId": "CASE-2024-001234",
    "autoPopulateFromCase": true,
    "activityFromDate": "2024-01-01",
    "activityToDate": "2024-01-31",
    "totalSuspiciousAmount": 95000.00
  }'
```

### Example: Validate XML Against FinCEN XSDs

```bash
curl -X POST http://localhost:8080/api/v1/efiling/reports/{sarId}/validate-xml \
  -u compliance:compliance123
```

Response:
```json
{
  "valid": true,
  "skipped": false,
  "errors": [],
  "warnings": [],
  "schemas": {
    "batchSchema": "https://www.fincen.gov/base/EFL_SARXBatchSchema.xsd",
    "baseSchema": "https://www.fincen.gov/base/BSA_XML_2.0.xsd",
    "referenceCodes": "https://www.fincen.gov/system/files/schema/code/FinCENReferenceCodes.xsd"
  }
}
```

---

## AML Case Integration

The `CaseManagementClient` interface supports plugging in any AML case system:

```java
public interface CaseManagementClient {
    Optional<CaseDto> getCaseById(String caseId);
    List<CaseDto> searchCases(CaseSearchCriteria criteria);
    String getSystemName();
    boolean isAvailable();
}
```

The default `RestCaseManagementClient` implementation makes REST calls to the configured `sar.case-management.base-url`. Expected endpoints:

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/cases/{caseId}` | Fetch case with subjects, accounts, transactions |
| `GET /api/v1/cases/search` | Search cases by criteria |
| `GET /api/v1/health` | Health check |

The `CaseDto` response should include:
- `subjects[]` — mapped to `SarSubject` (Part I)
- `accounts[]` — mapped to `SarAccount`
- `transactions[]` — mapped to `SarTransaction`
- `activityTypes[]` — mapped to `SarActivityType` (suspicious activity codes)
- `suspiciousActivitySummary` — used as narrative seed

To implement a custom adapter (e.g., for database-direct access):

```java
@Component
@ConditionalOnProperty(name = "sar.case-management.type", havingValue = "custom")
public class MyAmlSystemClient implements CaseManagementClient {
    // Implement interface methods
}
```

---

## eFiling Workflow

```
APPROVED SARs
      │
      ▼
┌─────────────────────────────────────────────────┐
│              Spring Batch Job: eFilingJob         │
│                                                   │
│  Step 1: validateSarReportsStep                   │
│    - Read APPROVED SARs from DB                   │
│    - Filter: subjects present, narrative present  │
│                                                   │
│  Step 2: submitBatchStep                          │
│    - Generate EFilingBatch entity                 │
│    - Build BSA XML 2.0 (FinCenXmlGenerator)       │
│    - Validate XML vs FinCENReferenceCodes.xsd     │
│    - Save XML to batch output directory           │
│    - POST to FinCEN BSA E-Filing endpoint         │
│    - Mark SARs as SUBMITTED                       │
│                                                   │
│  Step 3: pollAcknowledgmentStep                   │
│    - Poll FinCEN for pending batch ACKs           │
│    - Parse ACK XML (status A/R, BSA IDs)          │
│    - Mark SARs ACKNOWLEDGED + store BSA ID        │
│    - Log to audit trail                           │
└─────────────────────────────────────────────────┘
```

### eFiling Modes

| Mode | Behavior |
|------|----------|
| `offline` | Generates XML file only, simulates FinCEN acceptance (for dev/testing) |
| `sandbox` | Submits to `https://bsaefiling-sandbox.fincen.gov` |
| `production` | Submits to live FinCEN BSA E-Filing system |

### Generated BSA XML Structure

```xml
<EFilingBatchXML xsi:noNamespaceSchemaLocation="...EFL_SARXBatchSchema.xsd" StatusCode="A">
  <EFilingSubmissionXML SeqNum="1">
    <EFilingActivityXML SeqNum="1">
      <Activity SeqNum="1">
        <FilingDateText>20240201</FilingDateText>
        <ActivityAssociation SeqNum="1">
          <CorrectsAmendsPriorReportIndicator>N</CorrectsAmendsPriorReportIndicator>
        </ActivityAssociation>
        <Party SeqNum="1">  <!-- Transmitter (type 8) -->
        <Party SeqNum="2">  <!-- Transmitter Contact (type 9) -->
        <Party SeqNum="3">  <!-- Filing Institution (type 35) -->
        <Party SeqNum="4">  <!-- Designated Contact (type 46) -->
        <Party SeqNum="5">  <!-- FI Where Activity Occurred (type 33) -->
        <Party SeqNum="6">  <!-- Branch (type 34) -->
        <Party SeqNum="7">  <!-- Subject (type 23) -->
          <PartyName SeqNum="1">...</PartyName>
          <PartyIdentification SeqNum="1">...</PartyIdentification>
          <Address SeqNum="1">...</Address>
          <Account SeqNum="1">...</Account>
        </Party>
        <SuspiciousActivity SeqNum="1">
          <SuspiciousActivitySubtype SeqNum="1">
            <SuspiciousActivitySubtypeID>24</SuspiciousActivitySubtypeID>  <!-- Structuring -->
          </SuspiciousActivitySubtype>
        </SuspiciousActivity>
        <ActivityNarrativeInformation SeqNum="1">
          <ActivityNarrativeText>...</ActivityNarrativeText>
        </ActivityNarrativeInformation>
      </Activity>
    </EFilingActivityXML>
  </EFilingSubmissionXML>
</EFilingBatchXML>
```

---

## Audit History

Two complementary audit mechanisms are in place:

### 1. Hibernate Envers (Field-Level)
All core entities annotated with `@Audited` automatically track every field change across revisions.

```bash
GET /api/v1/sar-reports/{id}/revision-history
```

Returns each revision with: revision number, status, narrative snapshot, updated by, updated at.

### 2. Business Event Audit Log
Significant business events written to `audit_logs` table:

| Action | Trigger |
|--------|---------|
| `STATUS_CHANGE` | Any SAR status transition |
| `CASE_POPULATE` | Auto-population from AML case |
| `EFILING_SUBMITTED` | SAR included in FinCEN batch submission |
| `FINCEN_ACKNOWLEDGED` | FinCEN returns acceptance/rejection |
| `UPDATE` | Field-level change with old/new values |

```bash
GET /api/v1/sar-reports/{id}/audit-history?page=0&size=50
```

Each entry records: action, field changed, old value, new value, performed by, timestamp, IP address.

---

## Security

Default roles and credentials (development only — replace with LDAP/OAuth2/JWT in production):

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| `analyst` | `analyst123` | ANALYST | Create/edit SARs, manage subjects/accounts/transactions |
| `compliance` | `compliance123` | COMPLIANCE_OFFICER | Approve/reject SARs, trigger eFiling, process ACKs |
| `admin` | `admin123` | ADMIN + ANALYST + COMPLIANCE_OFFICER | Full access |
| `viewer` | `viewer123` | VIEWER | Read-only access to SARs and audit history |

To replace with JWT or OAuth2, update `SecurityConfig.java` in `sar-app`.

---

## Project Structure

```
SARSolution/
├── pom.xml                                         # Parent POM (multi-module)
│
├── sar-core/                                       # Shared domain kernel
│   └── src/main/java/com/fincen/sar/core/
│       ├── domain/         # JPA entities (SarReport, SarSubject, ...)
│       ├── enums/          # FinCEN code enums (ActivityTypeCode, PartyTypeCode, ...)
│       ├── repository/     # Spring Data JPA repositories
│       ├── exception/      # SarException, SarNotFoundException, ...
│       └── util/           # ReportNumberGenerator
│
├── sar-case-integration/                           # AML system client
│   └── src/main/java/com/fincen/sar/caseintegration/
│       ├── client/         # CaseManagementClient interface + REST impl
│       ├── dto/            # CaseDto, CaseSubjectDto, CaseAccountDto, ...
│       ├── mapper/         # CaseToSarMapper (MapStruct)
│       └── service/        # CasePopulationService
│
├── sar-form/                                       # SAR form management
│   └── src/main/java/com/fincen/sar/form/
│       ├── dto/            # SarReportDto, SarSubjectDto, CreateSarRequest, ...
│       ├── mapper/         # SarFormMapper (MapStruct)
│       ├── service/        # SarFormService
│       └── validator/      # SarFormValidator (Parts I-V completeness)
│
├── sar-audit/                                      # Audit history
│   └── src/main/java/com/fincen/sar/audit/
│       ├── config/         # AuditConfig (AuditorAware)
│       ├── dto/            # AuditLogDto, RevisionHistoryDto
│       └── service/        # AuditService (Envers + business events)
│
├── sar-efiling/                                    # FinCEN eFiling
│   └── src/main/java/com/fincen/sar/efiling/
│       ├── batch/          # EFilingBatchJobConfig (Spring Batch)
│       ├── config/         # EFilingConfig (RestTemplate bean)
│       ├── scheduler/      # EFilingScheduler (cron + polling)
│       ├── service/        # EFilingService, FinCenSubmissionService, AcknowledgmentParser
│       └── xml/            # FinCenXmlGenerator, FinCenXsdValidator
│
├── sar-api/                                        # REST layer
│   └── src/main/java/com/fincen/sar/api/
│       ├── controller/     # SarReportController, EFilingController
│       └── exception/      # GlobalExceptionHandler (RFC 9457 ProblemDetail)
│
└── sar-app/                                        # Application entry point
    ├── src/main/java/com/fincen/sar/app/
    │   ├── SarApplication.java
    │   ├── config/         # OpenApiConfig
    │   └── security/       # SecurityConfig
    └── src/main/resources/
        ├── application.yml
        ├── application-postgres.yml
        └── db/migration/
            └── V1__initial_schema.sql
```

---

## References

- [FinCEN SAR XML User Guide](https://bsaefiling.fincen.gov/docs/XMLUserGuide_FinCENSAR.pdf)
- [BSA XML Schema Validation Guidance](https://bsaefiling.fincen.gov/docs/XMLSchemaGuidance_SchemaValidation.pdf)
- [FinCEN SAR Electronic Filing Instructions](https://www.fincen.gov/system/files/shared/FinCEN%20SAR%20ElectronicFilingInstructions-%20Stand%20Alone%20doc.pdf)
- [BSA E-Filing System](https://bsaefiling.fincen.gov)
