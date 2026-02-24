# PocketAccount Architecture & Conventions

Base package: java.com.frnholding.pocketaccount

This backend has three primary features:

1) document       : upload/store PDFs and document metadata (Invoice/Statement)
2) interpretation : async jobs producing structured JSON results + user corrections
3) accounting     : bank transactions, receipts, matching, reconciliation exports

## 1) Feature-first packages (REQUIRED)

java.com.frnholding.pocketaccount
  common/
    config/
    error/
    util/

  document/
    controller/
    api/dto/
    service/
    domain/
    repository/
    repository/entity/
    infra/

  interpretation/
    controller/
    api/dto/
    service/
    domain/
    repository/
    repository/entity/
    pipeline/
    infra/
    mapper/ (optional)

  accounting/
    controller/
    api/dto/
    service/
    domain/
    repository/
    repository/entity/

### Non-negotiable rules
- DTOs live only in api/dto
- Entities live only in repository/entity
- Controllers call services only (never repositories)

## 2) Layer responsibilities

Controller:
- HTTP only (validation, mapping to DTO)
- calls service
- returns DTOs

Service:
- Use cases & orchestration
- owns transactions (@Transactional)
- calls repositories and infra clients

Domain:
- business concepts and invariants
- avoid Spring/JPA dependencies where possible

Repository:
- data access only (JPA repositories, projections, queries)

Infra:
- PDF storage, PDF parsing, OCR, OpenAI calls

## 3) Data types
- IDs: UUID
- Dates: LocalDate
- Timestamps: OffsetDateTime
- Money: BigDecimal
- Never use double/float for monetary values

## 4) API conventions

Base path: /api/v1

Document & interpretation endpoints:
- POST /documents                           (upload PDF)
- POST /documents/{id}/jobs                 (start interpretation job)
- GET  /jobs/{jobId}                        (job status)
- GET  /documents/{id}/result               (latest interpreted JSON result)
- PUT  /documents/{id}/correction           (save corrected JSON)

Accounting endpoints:
- POST /accounts
- GET  /accounts
- POST /bank-transactions/import
- GET  /bank-transactions?accountId=...&from=...&to=...
- POST /receipts
- GET  /receipts?from=...&to=...
- POST /matches
- DELETE /matches/{matchId}
- GET /bank-transactions/{id}/match-status
- GET /reconciliation?accountId=...&from=...&to=...
- GET /reconciliation/export?accountId=...&from=...&to=...&format=csv

## 5) Idempotency, matching and reconciliation

Bank import MUST be idempotent:
- unique(account_id, source_line_hash)

Matching supports partial matching:
- receipt_match.matched_amount

Match status is derived:
- abs(bank_transaction.amount) compared to sum(receipt_match.matched_amount)

## 6) Error handling

Standard error response:
ApiErrorResponse { code, message, details }

Use @ControllerAdvice and map:
- validation -> 400
- not found -> 404
- unique/conflict -> 409

## 7) System overview (Mermaid)

```mermaid
flowchart LR
  UI[UI (web & mobile)] -->|upload PDF| DOC[Document API]
  DOC -->|store file + metadata| FS[(File storage)]
  DOC -->|create job| JQ[Job row]
  JQ -->|after commit| RUN[Interpretation job runner]
  RUN -->|PDFBox/OCR/AI| PIPE[Interpretation pipeline]
  PIPE --> RES[Interpretation result]
  RES --> CORR[Corrections]
  RES --> RECEIPT[Receipts]
  RES --> STMT[Statement transactions]
  RECEIPT --> MATCH[Receipt match]
  STMT --> BANK[Bank transactions]
  MATCH --> RECON[Reconciliation export]
  RES --> API[Result & listing APIs]
  API --> UI
```
  MATCH --> RECON[Reconciliation export]
  RES --> API[Result & listing APIs]
  API --> UI
```
