# PocketAccount

A Spring Boot application for intelligent document interpretation and extraction, specializing in invoices and bank statements using OCR, AI, and heuristic methods.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Architecture](#architecture)
- [Usage Examples](#usage-examples)
- [Development Guidelines](#development-guidelines)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

## Overview

PocketAccount is an advanced document processing system that automatically extracts structured data from PDF documents, with specialized support for:

- **Invoices**: Extract amounts, dates, descriptions, senders, and currencies
- **Bank Statements**: Extract transactions with dates, amounts, and descriptions

The system intelligently selects the best extraction method (PDFBox for native PDFs, OCR for scanned documents) and supports both AI-powered and rule-based extraction strategies.

## Features

- 📄 **PDF Document Upload & Management**
- 🔍 **Smart Text Extraction** (PDFBox + Tesseract OCR with automatic fallback)
- 🤖 **AI-Powered Extraction** (OpenAI GPT-4o-mini integration)
- 📊 **Multiple Document Types** (Invoice, Statement, Receipt)
- ⚡ **Async Job Processing** (Background processing with status tracking)
- 🔄 **Multiple Interpretations** (Track multiple interpretations per document)
- 📝 **Manual Corrections** (Save and version user corrections)
- 🏷️ **Extraction Method Tracking** (Know which methods were used: PDFBox, OCR, AI, Heuristic)
- 🌍 **Multi-language Support** (English, Norwegian, German, French)

## Tech Stack

- **Framework**: Spring Boot 3.4.1
- **Language**: Java 17
- **Database**: PostgreSQL 17.3
- **ORM**: Spring Data JPA / Hibernate
- **PDF Processing**: Apache PDFBox 3.0.3
- **OCR**: Tess4J 5.13.0 (Tesseract)
- **AI**: OpenAI API (openai-gpt3-java 0.18.2)
- **Build Tool**: Maven
- **Utilities**: Lombok

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 17+
- Tesseract OCR (for scanned document processing)
- OpenAI API Key (optional, for AI-powered extraction)

### Installing Tesseract

**macOS:**
```bash
brew install tesseract
brew install tesseract-lang  # For additional languages
```

**Ubuntu/Debian:**
```bash
sudo apt-get install tesseract-ocr
sudo apt-get install tesseract-ocr-eng tesseract-ocr-deu tesseract-ocr-fra
```

**Windows:**
Download from: https://github.com/UB-Mannheim/tesseract/wiki

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd pocketaccount
```

### 2. Database Setup

Create PostgreSQL database and user:

```sql
CREATE DATABASE PocketAccount;
CREATE USER PocketAccountDBUser WITH PASSWORD 'admin';
GRANT ALL PRIVILEGES ON DATABASE PocketAccount TO PocketAccountDBUser;
```

### 3. Configure Application

Edit `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/PocketAccount
spring.datasource.username=PocketAccountDBUser
spring.datasource.password=admin

# File Upload Size
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# OCR Configuration
ocr.tesseract.language=eng+deu+fra
ocr.dpi=300
ocr.fallback.min-text-length=100
ocr.fallback.min-lines=5
ocr.fallback.min-char-per-line=10

# OpenAI Configuration (Optional)
openai.enabled=false
openai.api.key=your-api-key-here
openai.model=gpt-4o-mini
```

### 4. Build the Application

```bash
./mvnw clean package -DskipTests
```

## Running the Application

### Option 1: Using Maven

```bash
./mvnw spring-boot:run
```

### Option 2: Using JAR

```bash
java -jar target/pocketaccount-0.0.1-SNAPSHOT.jar
```

### Option 3: Background Process

```bash
nohup java -jar target/pocketaccount-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
```

The application will start on `http://localhost:8080`

## Configuration

### Key Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `spring.datasource.url` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/PocketAccount` |
| `spring.jpa.hibernate.ddl-auto` | Hibernate DDL mode | `update` |
| `spring.servlet.multipart.max-file-size` | Max upload size | `50MB` |
| `ocr.tesseract.language` | OCR languages | `eng+deu+fra` |
| `ocr.dpi` | PDF to image DPI | `300` |
| `openai.enabled` | Enable AI extraction | `false` |
| `openai.model` | OpenAI model | `gpt-4o-mini` |

## API Documentation

### Base URL

```
http://localhost:8080/api/v1
```

### Document Management Endpoints

#### Upload Document

```http
POST /documents
Content-Type: multipart/form-data

Parameters:
- file: PDF file (required)
- documentType: INVOICE | STATEMENT | RECEIPT (optional)

Response: 201 Created
{
  "documentId": "uuid",
  "originalFilename": "invoice.pdf",
  "status": "UPLOADED",
  "created": "2026-01-14T15:30:00Z",
  "documentType": "INVOICE",
  "fileSize": 245678
}
```

#### List Documents

```http
GET /documents

Response: 200 OK
[
  {
    "documentId": "uuid",
    "originalFilename": "invoice.pdf",
    "status": "UPLOADED",
    "created": "2026-01-14T15:30:00Z",
    "documentType": "INVOICE"
  }
]
```

#### Get Document

```http
GET /documents/{id}

Response: 200 OK
{
  "documentId": "uuid",
  "originalFilename": "invoice.pdf",
  "status": "UPLOADED",
  "created": "2026-01-14T15:30:00Z",
  "documentType": "INVOICE"
}
```

#### Download Document

```http
GET /documents/{id}/file

Response: 200 OK
Content-Type: application/pdf
```

### Interpretation Endpoints

#### Start Interpretation Job

```http
POST /interpretation/documents/{id}/jobs
Content-Type: application/json

{
  "useOcr": true,
  "useAi": false,
  "languageHint": "nb",
  "hintedType": "INVOICE"
}

Response: 202 Accepted
{
  "jobId": "uuid",
  "documentId": "uuid",
  "status": "PENDING",
  "created": "2026-01-14T15:30:00Z",
  "documentType": "INVOICE"
}
```

#### Get Job Status

```http
GET /interpretation/jobs/{jobId}

Response: 200 OK
{
  "jobId": "uuid",
  "documentId": "uuid",
  "status": "COMPLETED",
  "documentType": "INVOICE",
  "created": "2026-01-14T15:30:00Z",
  "startedAt": "2026-01-14T15:30:01Z",
  "finishedAt": "2026-01-14T15:30:05Z",
  "error": null,
  "originalFilename": "invoice.pdf"
}
```

Status values: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`

#### List All Jobs

```http
GET /interpretation/jobs

Response: 200 OK
[
  {
    "jobId": "uuid",
    "documentId": "uuid",
    "status": "COMPLETED",
    "documentType": "INVOICE",
    "created": "2026-01-14T15:30:00Z"
  }
]
```

#### Get Interpretation Result

```http
GET /interpretation/jobs/{jobId}/result

Response: 200 OK (for Invoice)
{
  "documentId": "uuid",
  "documentType": "INVOICE",
  "interpretedAt": "2026-01-14T15:30:05Z",
  "extractionMethods": "PDFBox, HeuristicInvoiceExtractor",
  "invoiceFields": {
    "amount": 12450.00,
    "currency": "NOK",
    "date": "2026-01-02",
    "description": "Faktura strøm januar",
    "sender": "Strøm AS"
  }
}

Response: 200 OK (for Statement)
{
  "documentId": "uuid",
  "documentType": "STATEMENT",
  "interpretedAt": "2026-01-14T15:30:05Z",
  "extractionMethods": "Composite(OCR), AIStatementExtractor",
  "transactions": [
    {
      "amount": -399.00,
      "currency": "NOK",
      "date": "2026-01-03",
      "description": "KIWI 123"
    }
  ]
}
```

## Architecture

### System Architecture

```
┌─────────────────────────────────────────────┐
│           REST API Layer                    │
│  (DocumentController, ExtractionController) │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│          Service Layer                      │
│  (DocumentService, InterpretationService)   │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│     Async Job Processing                    │
│  (InterpretationJobRunner with @Async)      │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│       Interpretation Pipeline               │
│  (Text Extraction → Classification →        │
│   Field Extraction → Result Storage)        │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│       Repository Layer (JPA)                │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│       PostgreSQL Database                   │
└─────────────────────────────────────────────┘
```

### Interpretation Pipeline

```
1. Text Extraction (CompositeTextExtractor)
   ├─→ Try PDFBox (fast, for native PDFs)
   └─→ Fallback to Tesseract OCR (for scanned)
   
2. Document Classification
   ├─→ INVOICE
   ├─→ STATEMENT
   └─→ RECEIPT
   
3. Field Extraction
   Invoice:
   ├─→ RegexInvoiceExtractor (rule-based)
   └─→ OpenAiInvoiceExtractor (AI-powered)
   
   Statement:
   ├─→ HeuristicStatementExtractor (rule-based)
   └─→ OpenAiStatementExtractor (AI-powered)
   
4. Result Storage
   └─→ Save with extraction method tracking
```

### Database Schema

```
documents
├─ id (UUID, PK)
├─ status (VARCHAR)
├─ created (TIMESTAMP)
├─ original_filename (VARCHAR)
├─ file_path (VARCHAR)
└─ document_type (VARCHAR)

interpretation_jobs
├─ id (VARCHAR, PK)
├─ document_id (UUID, FK)
├─ status (VARCHAR)
├─ created (TIMESTAMP)
├─ started_at (TIMESTAMP)
├─ finished_at (TIMESTAMP)
├─ error (TEXT)
└─ document_type (VARCHAR)

interpretation_results
├─ id (BIGINT, PK)
├─ job_id (VARCHAR, FK, Unique)
├─ document_id (UUID, FK)
├─ document_type (VARCHAR)
├─ interpreted_at (TIMESTAMP)
├─ extraction_methods (VARCHAR)
├─ amount (DOUBLE)
├─ currency (VARCHAR)
├─ date (DATE)
├─ description (TEXT)
└─ sender (VARCHAR)

statement_transactions
├─ id (BIGINT, PK)
├─ result_id (BIGINT, FK)
├─ amount (DOUBLE)
├─ currency (VARCHAR)
├─ date (DATE)
└─ description (TEXT)
```

## Usage Examples

### Example 1: Upload and Interpret an Invoice

```bash
# 1. Upload invoice PDF
curl -X POST http://localhost:8080/api/v1/documents \
  -F "file=@invoice.pdf" \
  -F "documentType=INVOICE"

# Response:
# {
#   "documentId": "123e4567-e89b-12d3-a456-426614174000",
#   "originalFilename": "invoice.pdf",
#   "status": "UPLOADED"
# }

# 2. Start interpretation job
curl -X POST http://localhost:8080/api/v1/interpretation/documents/123e4567-e89b-12d3-a456-426614174000/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "useOcr": true,
    "useAi": false,
    "languageHint": "nb"
  }'

# Response:
# {
#   "jobId": "job-456",
#   "status": "PENDING"
# }

# 3. Check job status
curl http://localhost:8080/api/v1/interpretation/jobs/job-456

# Response:
# {
#   "jobId": "job-456",
#   "status": "COMPLETED"
# }

# 4. Get interpretation result
curl http://localhost:8080/api/v1/interpretation/jobs/job-456/result

# Response:
# {
#   "documentType": "INVOICE",
#   "extractionMethods": "PDFBox, HeuristicInvoiceExtractor",
#   "invoiceFields": {
#     "amount": 12450.00,
#     "currency": "NOK",
#     "date": "2026-01-02",
#     "description": "Faktura strøm januar",
#     "sender": "Strøm AS"
#   }
# }
```

### Example 2: Process a Bank Statement with AI

```bash
# Upload and interpret with AI enabled
curl -X POST http://localhost:8080/api/v1/documents \
  -F "file=@statement.pdf" \
  -F "documentType=STATEMENT"

# Start AI-powered interpretation
curl -X POST http://localhost:8080/api/v1/interpretation/documents/{docId}/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "useOcr": true,
    "useAi": true,
    "languageHint": "nb"
  }'
```

### Example 3: List All Interpretation Jobs

```bash
curl http://localhost:8080/api/v1/interpretation/jobs
```

## Development Guidelines

### Project Structure

```
src/main/java/com/frnholding/pocketaccount/
├── controller/              # REST endpoints
├── service/                 # Business logic
├── repository/              # Data access
├── domain/                  # Domain models & JPA entities
├── interpretation/          # Interpretation module
│   ├── api/                # Interpretation REST API
│   ├── service/            # Interpretation services
│   ├── pipeline/           # Pipeline interfaces
│   └── infra/              # Pipeline implementations
└── config/                  # Configuration classes
```

### Coding Standards

1. **Use Lombok** to reduce boilerplate:
   ```java
   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   public class MyClass { ... }
   ```

2. **Follow REST conventions**:
   - POST for creation (201 Created)
   - GET for retrieval (200 OK)
   - PUT for updates (200 OK)
   - DELETE for removal (204 No Content)
   - 202 Accepted for async operations

3. **Use DTOs** for API layer:
   - Separate request/response objects
   - Don't expose domain entities directly

4. **Async processing** for long-running tasks:
   ```java
   @Async
   @Transactional
   public void runJob(String jobId, ...) {
       // Long-running task
   }
   ```

5. **Logging**:
   ```java
   @Slf4j
   public class MyService {
       public void process() {
           log.info("Processing started");
           log.debug("Details: {}", data);
           log.error("Error occurred", exception);
       }
   }
   ```

### Adding New Document Types

1. Add to `DocumentType` enum:
   ```java
   public enum DocumentType {
       INVOICE,
       STATEMENT,
       RECEIPT,
       CONTRACT  // New type
   }
   ```

2. Create extractor interface:
   ```java
   public interface ContractExtractor {
       ContractFields extract(InterpretedText text);
   }
   ```

3. Implement extractors:
   - Rule-based: `HeuristicContractExtractor`
   - AI-powered: `OpenAiContractExtractor`

4. Update `InterpretationPipeline` to handle new type

### Adding New Extraction Methods

1. Implement the appropriate interface:
   ```java
   @Component
   public class MyCustomExtractor implements InvoiceExtractor {
       @Override
       public InvoiceFields extract(InterpretedText text) {
           // Custom extraction logic
       }
   }
   ```

2. Register as Spring Bean with `@Component`

3. Use `@Qualifier` if multiple implementations exist

## Testing

### Run Tests

```bash
./mvnw test
```

### Run Specific Test Class

```bash
./mvnw test -Dtest=DocumentServiceTest
```

### Skip Tests During Build

```bash
./mvnw clean package -DskipTests
```

### Manual Testing with Test UI

A test UI is included in the `test-ui/` directory:

```bash
cd test-ui
npm install
npm run dev
```

Access at: http://localhost:5173

## Troubleshooting

### Common Issues

**Issue: "Tesseract not found"**
```
Solution: Install Tesseract OCR and ensure it's in your PATH
macOS: brew install tesseract
```

**Issue: "Connection refused to PostgreSQL"**
```
Solution: Ensure PostgreSQL is running
macOS: brew services start postgresql
Linux: sudo systemctl start postgresql
```

**Issue: "Port 8080 already in use"**
```
Solution: Kill the process or change the port
lsof -ti:8080 | xargs kill -9
Or change in application.properties: server.port=8081
```

**Issue: "File upload too large"**
```
Solution: Increase max file size in application.properties
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
```

**Issue: "Job stuck in PENDING status"**
```
Solution: Check backend.log for errors
tail -f backend.log
Ensure async executor is configured properly
```

### Debug Mode

Enable debug logging:

```properties
logging.level.com.frnholding.pocketaccount=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Check Application Health

```bash
curl http://localhost:8080/actuator/health
```

## License

Copyright © 2026 FRN Holding. All rights reserved.

## Support

For issues and questions, please contact the development team.

---

**Version**: 0.0.1-SNAPSHOT  
**Last Updated**: January 14, 2026
