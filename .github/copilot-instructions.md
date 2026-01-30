# PocketAccount AI Copilot Instructions

## Project Overview
**PocketAccount** is a full-stack document intelligence system combining Spring Boot backend with Svelte frontend. It automatically extracts structured data from PDFs using multiple strategies: PDFBox native text extraction, OCR (Tesseract), and AI-powered extraction (OpenAI GPT-4o).

Core use cases: invoice/bank statement processing with smart method selection and multi-interpretation support.

## Architecture Overview

### Backend Stack (Spring Boot 4.0.1, Java 21)
- **Document Pipeline**: Upload → Extraction → Interpretation → Corrections
- **Database**: PostgreSQL 17 (Flyway migrations in `src/main/resources/db/migration/`)
- **Key Dependencies**:
  - Apache PDFBox 3.0.3 (native PDF text)
  - Tesseract/Tess4J 5.13.0 (OCR)
  - OpenAI API (gpt-4o, gpt-4o-mini)
  - Spring Data JPA + Hibernate
  - Lombok for boilerplate reduction

### Frontend Stack (Svelte Kit)
- Build tool: Vite
- Testing: Playwright (integration) + Vitest (unit)
- Routes in `test-ui/src/routes/`: upload, interpret, compare, results
- Entry point: `+page.svelte` components with `+page.js` loaders

### Service Boundaries
1. **DocumentService** (`src/.../service/DocumentService.java`): File upload, storage, retrieval
2. **InterpretationService** (`src/.../interpretation/service/InterpretationService.java`): Job orchestration, interpretation results
3. **Text Extractors** (`src/.../interpretation/infra/`): PDFBox, OCR, CompositeTextExtractor (fallback logic)
4. **Field Extractors** (`src/.../interpretation/infra/`): OpenAI-powered invoice/statement extractors

## Critical Data Flow Patterns

### Document Processing Pipeline
```
File Upload → DocumentService.uploadDocument() → PDF saved to disk
→ InterpretationService.startInterpretation(documentId) → Creates InterpretationJob
→ InterpretationJobRunner (async) → Selects extraction strategy → Persists InterpretationResult
```

### Text Extraction Strategy (CompositeTextExtractor)
1. **Try PDFBox first** (fast path for native PDFs)
2. **Evaluate quality**: min text 100 chars, min 5 lines, 10+ chars/line average, >50% alphanumeric
3. **Fallback to OCR** if quality metrics fail
4. Config in `application.properties`: `ocr.fallback.*` properties control thresholds

### Multi-Interpretation Support
- Document → Multiple InterpretationResults (different extractors can produce different results)
- Users can save corrections → Creates versioned correction records
- Data model tracks `extraction_method` (PDFBox, OCR, AI, Heuristic) for each result

## Configuration Management

**application.properties** structure:
- **Database**: `spring.datasource.*` (PostgreSQL localhost:5432)
- **OCR**: `ocr.tesseract.language=eng+deu+fra+nor`, `ocr.dpi=300`
- **OpenAI**: `openai.api.key=${OPENAI_API_KEY}` (env var required), `openai.model=gpt-4o`
- **File upload limits**: `spring.servlet.multipart.max-file-size=50MB`

**Never commit API keys**—use environment variables: `export OPENAI_API_KEY=sk-...`

## Development Workflows

### Building & Running Backend
```bash
# Maven build
./mvnw clean install

# Run Spring Boot
./mvnw spring-boot:run
# Server on http://localhost:8080

# Test
./mvnw test
```

### Frontend Development
```bash
cd test-ui
npm install
npm run dev        # Dev server with hot reload
npm run build      # Production build
npm run test       # Run all tests (integration + unit)
npm run test:integration  # Playwright tests
npm run test:unit  # Vitest unit tests
```

### Database Management
- Flyway migrations auto-run on startup (configured in pom.xml)
- New migrations: `src/main/resources/db/migration/V{N}__Description.sql`
- Schema: `documents` table (id, status, created, original_filename, file_path, document_type)

## Codebase Conventions

### Package Organization
- **api/dto/**: Data transfer objects (DTO suffix: `DocumentUploadResponseDTO`, `InvoiceFieldsDTO`)
- **domain/**: Entity classes (JPA entities, represent DB tables)
- **repository/**: Spring Data JPA interfaces extending `JpaRepository<Entity, ID>`
- **service/**: Business logic layer with `@Service` annotation
- **controller/**: REST endpoints with `@RestController`, `@RequestMapping("/api/v1")`

### Interpretation Module Structure
```
interpretation/
├── api/          # REST controllers (ExtractionController)
├── domain/       # InterpretationJob, InterpretationResult entities
├── service/      # InterpretationService, InterpretationJobRunner
├── infra/        # Text/field extractors (PdfBoxTextExtractor, OcrTextExtractor, OpenAiInvoiceExtractor)
├── pipeline/     # Job execution orchestration
└── repository/   # Data access for interpretation entities
```

### Naming Conventions
- Controllers: `*Controller` + `@RestController`
- Services: `*Service` + `@Service`
- Entities: Domain object names (Document, InterpretationJob, InterpretationResult)
- DTOs: `*DTO` or `*ResponseDTO`, `*RequestDTO`
- Extractors: `*Extractor` or `*TextExtractor`, `*FieldExtractor`

### API Patterns
- Base path: `/api/v1/*`
- Document endpoints: `POST /documents` (upload), `GET /documents`, `GET /documents/{id}`
- Interpretation endpoints: `POST /interpretations` (start job), `GET /interpretations/jobs/{id}` (status)
- CORS enabled globally: `@CrossOrigin(origins = "*")`

## Common Implementation Tasks

### Adding New Document Type
1. Update `documentType` enum/constants (domain/controller)
2. Add new extractor: extend `FieldExtractor` interface in `interpretation/infra/`
3. Update `InterpretationService.startInterpretation()` to route to new extractor
4. Add DTO: create `*FieldsDTO` in `api/dto/`
5. Test: use mock data first (see `createMockInterpretationResult()` example)

### Adding OCR Language Support
1. Install language pack: `brew install tesseract-lang` (macOS) or apt-get (Linux)
2. Update `application.properties`: `ocr.tesseract.language=eng+deu+fra+nor+ita`
3. Restart application

### Debugging Extraction Issues
1. Check extraction method used: Database stores `extraction_method` in InterpretationResult
2. View extracted text in logs: extractors log text length and quality metrics
3. Test fallback thresholds: lower `ocr.fallback.min-*` properties to debug PDFBox→OCR transitions
4. OpenAI failures return empty fields (graceful degradation)

## Integration Points

### External Dependencies
- **OpenAI**: REST calls for invoice/statement field extraction (timeout: 30s configurable)
- **Tesseract**: System binary (requires installation), called via Tess4J
- **PostgreSQL**: JDBC connection with HikariCP pooling

### Frontend-Backend Communication
- JSON REST API on `/api/v1/*`
- Multipart form upload for PDFs (form-data with file + metadata)
- Status polling for async jobs (frontend polls `/interpretations/jobs/{id}`)

### Data Persistence Strategy
- Document → file on disk (path stored in DB)
- InterpretationJob → tracks async processing state (PENDING, COMPLETED, FAILED)
- InterpretationResult → immutable extraction output, corrections tracked separately

## Important Notes for AI Agents

1. **Async Processing**: InterpretationJobRunner is placeholder—actual async orchestration needed
2. **Error Handling**: Field extractors return empty/null gracefully; don't throw on AI failures
3. **Multi-language**: Support eng+deu+fra+nor configured; extractors are language-agnostic
4. **File Storage**: PDFs stored on disk; secure path handling required in production
5. **Testing**: Mock extractors available; avoid external API calls in unit tests
6. **Version Control**: Ignore `application.properties` API keys; use environment variables
