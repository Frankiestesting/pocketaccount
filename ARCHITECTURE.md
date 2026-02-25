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
  UI["UI (web & mobile)"]
  DOC["Document API"]
  INTAPI["Interpretation API"]
  FS[(File storage)]
  JQ["Interpretation job (async)"]
  RUN["Interpretation job runner"]

  subgraph PIPE["Interpretation pipeline"]
    OCR["OCR"]
    AI["AI extraction"]
    NORM["Normalization"]
  end

  RES["Interpretation result"]
  CORR["Corrections"]
  RECEIPT["Receipts"]
  STMT["Statement transactions"]
  MATCH["Receipt match"]
  BANK["Bank transactions"]
  RECON["Reconciliation export"]
  API["Result & listing APIs"]

  UI -->|"upload PDF"| DOC
  DOC -->|"store file + metadata"| FS
  DOC -->|"create job"| JQ
  INTAPI -->|"create job"| JQ
  JQ -.->|"async trigger"| RUN
  RUN --> OCR
  OCR --> AI
  AI --> NORM
  NORM --> RES
  RES --> CORR
  RES --> RECEIPT
  RES --> STMT
  RECEIPT -->|"match vs bank tx"| MATCH
  STMT --> BANK
  MATCH --> RECON
  RES --> API
  API --> UI
```

## 8) Use case: Upload file (sequence)

```mermaid
sequenceDiagram
  participant User
  participant UI
  participant DocumentAPI as Document API
  participant Storage as File storage
  participant Repo as Document repository

  User->>UI: Velg PDF og send
  UI->>DocumentAPI: POST /api/v1/documents (multipart)
  DocumentAPI->>Storage: Lagre fil (UUID.pdf)
  DocumentAPI->>Repo: Lagre dokument-metadata (id, path, type)
  Repo-->>DocumentAPI: Bekreft lagring
  Storage-->>DocumentAPI: OK
  DocumentAPI-->>UI: 201 Created + documentId
  UI-->>User: Viser opplastet dokument og tilbyr "start tolkning"
```

## 9) Use case: Interpret document (sequence)

```mermaid
sequenceDiagram
  participant User
  participant UI
  participant IntAPI as Interpretation API
  participant JobRepo as Interpretation job repo
  participant Runner as Job runner
  participant Pipeline as Pipeline
  participant ResultRepo as Interpretation result repo

  User->>UI: Velg dokument (opplastet fil) og metodevalg (PDFBox/OCR/AI)
  UI->>IntAPI: POST /api/v1/interpretation/documents/{documentId}/jobs { useOcr, useAi, languageHint }
  IntAPI->>JobRepo: Lagre job (PENDING)
  JobRepo-->>IntAPI: OK
  IntAPI-->>UI: 202 Accepted + jobId
  IntAPI-->>Runner: (after commit) start job async

  Runner->>JobRepo: Oppdater status RUNNING
  Runner->>Pipeline: Start pipeline
  alt useOcr = true
    Pipeline->>Pipeline: OCR (tekstuttrekk)
  else useOcr = false
    Pipeline->>Pipeline: PDFBox tekstuttrekk
  end
  alt useAi = true
    Pipeline->>Pipeline: AI extraction (klassifisering/felt)
  else
    Pipeline->>Pipeline: Heuristikk/regex extraction
  end
  Pipeline->>Pipeline: Normalisering + confidence + warnings
  alt documentType = RECEIPT
    Pipeline-->>Runner: Invoice fields
  else documentType = STATEMENT
    Pipeline-->>Runner: Transactions list
  else
    Pipeline-->>Runner: Generic fields
  end
  Runner->>ResultRepo: Lagre resultat (jobId, documentId, felt/transaksjoner)
  Runner->>JobRepo: Oppdater status COMPLETED/FAILED
  Runner-->>UI: (poll via GET /jobs/{jobId}) status/resultat tilgjengelig
```

sequenceDiagram
  participant User
  participant UI
  participant IntAPI as "Interpretation API"
  participant ResultRepo as "Interpretation result repo"
  participant CorrHist as "Correction history"

  User->>UI: "Åpne tolkingsresultat (viser original)"
  UI->>IntAPI: "GET /api/v1/interpretation/documents/{id}/result"
  IntAPI-->>UI: "Returnerer latest result (original)"
  User->>UI: "Rediger felt/transaksjoner (korrigert)"
  UI->>IntAPI: "PUT /api/v1/interpretation/documents/{id}/correction"
  IntAPI->>ResultRepo: "Lagre korrigert versjon (samme documentId)"
  IntAPI->>CorrHist: "Lagre snapshot (før/etter)"
  ResultRepo-->>IntAPI: "OK"
  CorrHist-->>IntAPI: "OK"
  IntAPI-->>UI: "200 OK"
  UI-->>User: "Viser korrigert, original er bevart i historikk"
## 11) Use case: Approve statement transaction (sequence)

```mermaid
sequenceDiagram
  participant User
  participant UI
  participant IntAPI as Interpretation API
  participant ResultRepo as Interpretation result repo
  participant BankRepo as Bank transaction repo

  User->>UI: Velg statement-transaksjon og klikk "Godkjenn"
  UI->>IntAPI: POST /api/v1/interpretation/statement-transactions/{id}/approve { accountId? }
  IntAPI->>ResultRepo: Hent statement-transaction (fra interpretation_result)
  alt har koblet bankTransactionId
    ResultRepo-->>IntAPI: Return existing bank transaction id
  else
    IntAPI->>BankRepo: Opprett bank transaction for account
    BankRepo-->>IntAPI: Ny bank transaction id
  end
  IntAPI-->>UI: 200 OK + bankTransactionId
  UI-->>User: Viser status "approved" og koblet banktransaksjon
```

## 12) Use case: Approve receipt & match (sequence)

```mermaid
sequenceDiagram
  participant User
  participant UI
  participant IntAPI as Interpretation API
  participant ReceiptRepo as Receipt repo
  participant MatchRepo as Receipt match repo
  participant BankRepo as Bank transaction repo

  User->>UI: Fra tolkningsresultat (RECEIPT) klikker "Opprett kvittering"
  UI->>IntAPI: POST /api/v1/interpretation/documents/{id}/receipt
  IntAPI->>ReceiptRepo: Lagre receipt (fra invoice fields)
  ReceiptRepo-->>IntAPI: receiptId
  IntAPI-->>UI: 201 Created + receiptId
  UI-->>User: Kvittering opprettet; velg banktransaksjon å matche

  User->>UI: Velg banktransaksjon + beløp (delvis/hel)
  UI->>MatchRepo: POST /api/v1/matches { receiptId, bankTransactionId, matchedAmount }
  MatchRepo->>BankRepo: Valider bankTransactionId og status
  MatchRepo-->>UI: 201 Created
  UI-->>User: Viser match-status (PARTIAL/MATCHED/OVER) basert på sum matchedAmount vs receipt total
```
