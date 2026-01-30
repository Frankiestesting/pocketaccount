# Copilot Instructions (PocketAccount Brain)

Base package: java.com.frnholding.pocketaccount
Language: Java
Framework: Spring Boot (Spring Web, Spring Data JPA, Validation)
Database: PostgreSQL

This backend:
- Stores PDF documents categorized as INVOICE or STATEMENT
- Runs async interpretation jobs to produce structured JSON results
- Allows user corrections linked to the PDF/document
- Provides accounting: bank transactions, receipts, matching, reconciliation exports

## Global Coding Rules
- IDs: UUID everywhere
- Dates: LocalDate
- Timestamps: OffsetDateTime
- Money: BigDecimal (never double/float)
- Prefer immutable DTOs where practical, but keep it simple
- NO Lombok on JPA entities or core business logic classes
- Lombok allowed only in DTOs (optional). If unsure: do not use Lombok.

## Package Structure (feature-first REQUIRED)
Use feature packages under the base package:

java.com.frnholding.pocketaccount
  common/
  document/
  interpretation/
  accounting/

Each feature should use:
- controller/       : HTTP only, thin controllers
- api/dto/          : request/response DTOs only
- service/          : use cases + orchestration + @Transactional boundaries
- domain/           : domain objects (avoid JPA + Spring dependencies where possible)
- repository/       : Spring Data repositories
- repository/entity/: JPA entities only
- infra/            : external integrations (PDFBox, OCR, OpenAI, file storage)
- (interpretation only) pipeline/ : interpretation pipeline interfaces + orchestrator

Never place DTOs in root packages. Never place entities outside repository/entity.

## Naming Conventions
### DTO naming (use-case based)
Use verbs and clear intent:
- CreateAccountRequest, AccountResponse
- ImportBankStatementRequest, ImportBankStatementResponse
- CreateReceiptRequest, ReceiptResponse
- CreateReceiptMatchRequest, MatchStatusResponse
- StartInterpretationRequest, StartInterpretationResponse
Avoid generic names like:
- JobRequest, DocumentRequest, GenericResponse, DataDTO

### Entity naming
Entity classes end with Entity:
- DocumentEntity, InterpretationJobEntity, BankTransactionEntity, ReceiptEntity

## Entity Rules (JPA)
- Map with @Table(name="snake_case")
- Avoid bidirectional relations unless necessary
- No @Data on entities
- equals/hashCode should be ID-only OR omit for MVP
- Keep entities persistence-focused (no heavy business logic)

## API Rules
- Base path: /api/v1
- Status codes:
  - POST create: 201 Created
  - POST start job: 202 Accepted
  - DELETE: 204 No Content
  - Validation: 400 with field errors
  - Not found: 404
  - Conflicts/unique constraint: 409
- Controllers must NOT access repositories directly; always call a service.
- Controllers return DTOs only.

## Error Handling
Use a single error response DTO:
ApiErrorResponse { String code; String message; Map<String,String> details; }

Use @ControllerAdvice to map:
- MethodArgumentNotValidException -> 400
- EntityNotFoundException -> 404
- DataIntegrityViolationException -> 409

Do not expose raw DB error messages or stack traces to clients.

## Performance Rules
- Avoid N+1 queries
- Prefer bulk operations for imports
- Use pagination on list endpoints where data can grow

## Accounting Domain Rules
Tables/entities:
- account
- bank_transaction (idempotent import; unique(account_id, source_line_hash))
- receipt
- receipt_match (supports partial matches using matched_amount)

Idempotent import:
- Must not insert duplicates by (accountId, sourceLineHash)
- Prefer bulk lookup of existing hashes instead of per-row exists() calls when feasible

Matching:
- receipt_match supports partial matching
- Match status is derived:
  UNMATCHED, PARTIAL, MATCHED, OVER
- Derived from abs(bank_transaction.amount) vs sum(receipt_match.matched_amount)

## Interpretation Rules
Interpretation is async job-based:
- Start job -> store job row -> worker runs pipeline -> store result row
Pipeline steps:
1) Extract text (PDFBox; OCR fallback optional)
2) Classify (Invoice/Statement/Unknown)
3) Extract fields/transactions (regex/heuristics; AI optional)
4) Normalize + confidence + warnings

Store:
- raw interpretation result
- corrected result (user "final")

Always link results to documentId and version them.
