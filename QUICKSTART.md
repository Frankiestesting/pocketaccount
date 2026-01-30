# PocketAccount - Quick Start Guide

## Prerequisites

Before starting the application, ensure you have:
- Java 21+ installed
- Maven 3.6+
- Node.js 16+ and npm
- PostgreSQL 17 running locally
- Tesseract OCR installed (optional, for scanned documents)

## Environment Setup

### 1. Database Setup
Create the PostgreSQL database:

```bash
createdb PocketAccount
createuser -P PocketAccountDBUser  # password: admin
psql PocketAccount -c "GRANT ALL PRIVILEGES ON DATABASE PocketAccount TO PocketAccountDBUser;"
```

### 2. Set Environment Variables

**OpenAI API Key (required):**
```bash
export OPENAI_API_KEY="sk-your-api-key-here"
# Or for testing without OpenAI:
export OPENAI_API_KEY="sk-test-placeholder"
```

**Optional - Tesseract Path (if not in default location):**
```bash
export TESSERACT_OCR_PATH="/usr/local/Cellar/tesseract/5.x.x/bin/tesseract"
```

## Starting the Application

### Option 1: Automated Start Script (Recommended)

```bash
cd /Users/frank/Documents/Vscode-java/pocketaccount
./start.sh
```

This will:
1. Start Spring Boot backend (port 8080)
2. Start Svelte frontend dev server (port 5173)
3. Wait for both to be ready
4. Display URLs and logs

### Option 2: Manual Start

**Terminal 1 - Backend:**
```bash
cd /Users/frank/Documents/Vscode-java/pocketaccount
export OPENAI_API_KEY="sk-your-key"
./mvnw spring-boot:run
```

**Terminal 2 - Frontend:**
```bash
cd /Users/frank/Documents/Vscode-java/pocketaccount/test-ui
npm run dev
```

## Accessing the Application

Once both services are running:

| Service | URL | Purpose |
|---------|-----|---------|
| **Frontend** | http://localhost:5173 | Main UI - Upload and process documents |
| **API Docs** | http://localhost:8080/swagger-ui.html | Interactive API documentation |
| **API Endpoint** | http://localhost:8080/api/v1 | REST API base URL |

## Testing the UI

### 1. Upload a Document
1. Navigate to http://localhost:5173
2. Go to the "Upload" section
3. Select a PDF file
4. Click "Upload"
5. You should see the document listed with metadata

### 2. Extract Data
1. Select a document from the list
2. Click "Interpret" or "Extract"
3. Choose extraction method:
   - **PDFBox** (fast, for digital PDFs)
   - **OCR** (for scanned documents)
   - **AI** (GPT-4o for structured extraction)
4. Wait for processing to complete
5. View extracted fields in the "Results" section

### 3. View Extraction Results
1. Navigate to "Results" page
2. See extracted invoice/statement fields
3. Make corrections if needed
4. Save corrections

### 4. Compare Multiple Interpretations
1. Go to "Compare" section
2. View different extraction methods side-by-side
3. See how PDFBox, OCR, and AI results differ

## API Testing with Swagger

Interactive API documentation is available at: **http://localhost:8080/swagger-ui.html**

### Example API Calls

**Upload a Document:**
```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -F "file=@/path/to/document.pdf" \
  -F "source=mobile-app" \
  -F "originalFilename=invoice.pdf" \
  -F "documentType=INVOICE"
```

**Start Extraction Job:**
```bash
curl -X POST http://localhost:8080/api/v1/interpretation/documents/{documentId}/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "useOcr": true,
    "useAi": true,
    "languageHint": "eng",
    "hintedType": "INVOICE"
  }'
```

**Get Extraction Result:**
```bash
curl http://localhost:8080/api/v1/documents/{documentId}/result
```

## Troubleshooting

### Backend won't start
- Check PostgreSQL is running: `psql -U postgres -c "\l"`
- Check logs: `tail -100 /tmp/pocketaccount-backend.log`
- Ensure OPENAI_API_KEY is set: `echo $OPENAI_API_KEY`
- Check port 8080 is free: `lsof -i :8080`

### Frontend won't start
- Check logs: `tail -100 /tmp/pocketaccount-frontend.log`
- Delete node_modules and reinstall:
  ```bash
  cd test-ui
  rm -rf node_modules package-lock.json
  npm install
  npm run dev
  ```

### Database connection errors
- Verify PostgreSQL is running on localhost:5432
- Check credentials in `application.properties`
- Recreate database if needed

### OCR issues
- Verify Tesseract is installed: `tesseract --version`
- Check language data: `ls /usr/local/Cellar/tesseract/share/tessdata/`

## File Locations

- **Backend config**: `src/main/resources/application.properties`
- **Frontend config**: `test-ui/svelte.config.js`
- **Database migrations**: `src/main/resources/db/migration/`
- **Frontend routes**: `test-ui/src/routes/`
- **Backend API**: `src/main/java/.../controller/` and `src/main/java/.../interpretation/api/`

## Test Data

Sample PDFs for testing are located in: `testdata/` directory

## Useful Commands

```bash
# Build the project
./mvnw clean install

# Run tests
./mvnw test

# Build frontend
cd test-ui && npm run build

# Run frontend tests
cd test-ui && npm run test:integration

# Clean up logs
rm /tmp/pocketaccount-*.log

# Kill all running services
pkill -f "spring-boot:run"
pkill -f "npm run dev"
```

## Next Steps

1. ✅ Start the application using `./start.sh`
2. ✅ Test document upload at http://localhost:5173
3. ✅ Try extraction with different methods (PDFBox, OCR, AI)
4. ✅ Review Swagger API docs at http://localhost:8080/swagger-ui.html
5. ✅ Make a correction and save it

## Support

For more detailed information, see:
- [SWAGGER_SETUP.md](SWAGGER_SETUP.md) - Swagger/OpenAPI documentation
- [.github/copilot-instructions.md](.github/copilot-instructions.md) - Architecture overview
- [README.md](README.md) - Full project documentation
