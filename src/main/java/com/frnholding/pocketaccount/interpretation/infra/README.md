# PDF Text Extraction Infrastructure

This package contains infrastructure implementations for extracting text from PDF documents and AI-based field extraction.

## Text Extraction Components

### 1. PdfBoxTextExtractor
- **Technology**: Apache PDFBox 3.0.3
- **Use Case**: PDFs with native text layers (digitally created documents)
- **Advantages**: 
  - Fast processing
  - High accuracy for native PDFs
  - Extracts metadata (page count, author, producer, etc.)
  - Simple language detection
- **Limitations**: 
  - Cannot read scanned documents
  - Poor results for image-based PDFs

### 2. OcrTextExtractor
- **Technology**: Tesseract OCR (via Tess4J)
- **Use Case**: Scanned documents, image-based PDFs, documents without text layers
- **Advantages**:
  - Works with scanned documents
  - Multi-language support (eng+deu+fra configured)
  - Mobile-ready architecture
- **Limitations**:
  - Slower processing (renders PDF pages to images)
  - Requires Tesseract installation
  - Lower accuracy than native text extraction

#### Mobile OCR Support
The `OcrTextExtractor` is prepared for mobile OCR integration:
- Cloud OCR services (Google Vision, AWS Textract, Azure OCR)
- On-device mobile OCR
- Hybrid approach: quick mobile OCR + server-side verification

### 3. CompositeTextExtractor (Recommended)
- **Strategy**: Intelligent fallback approach
- **Process**:
  1. Try PDFBox first (fast path)
  2. Evaluate extraction quality
  3. Fallback to OCR if quality is poor
- **Quality Metrics**:
  - Minimum text length: 100 characters
  - Minimum lines: 5
  - Minimum chars per line: 10
  - Alphanumeric ratio: >50%

## AI-Based Field Extraction Components

### 4. OpenAiInvoiceExtractor
- **Technology**: OpenAI GPT (default: gpt-4o-mini)
- **Use Case**: Extract structured invoice fields from text
- **Advantages**:
  - High accuracy with contextual understanding
  - Handles various invoice formats
  - Robust date parsing (multiple formats)
  - Language agnostic
- **Extracts**:
  - Amount (numeric)
  - Currency (code)
  - Date (LocalDate)
  - Description

# OpenAI Configuration
# Get your API key from https://platform.openai.com/api-keys
# openai.api.key=sk-your-key-here
openai.enabled=false
openai.model=gpt-4o-mini
openai.timeout=30
```

### API Key Setup
To use OpenAI extractors:
1. Get API key from https://platform.openai.com/api-keys
2. Set in application.properties: `openai.api.key=sk-your-key-here`
3. Enable: `openai.enabled=true`

**Security Note**: Never commit API keys to version control. Use environment variables:
```bash
export OPENAI_API_KEY=sk-your-key-here
```
Then in application.properties:
```properties
openai.api.key=${OPENAI_API_KEY}
  - Sender/Issuer
- **Error Handling**: Returns empty fields on failure, never throws

### 5. OpenAiStatementExtractor
- **Technology**: OpenAI GPT (default: gpt-4o-mini)
- **Use Case**: Extract transaction list from bank statements
- **Advantages**:
  - Extracts multiple transactions
  - Handles complex statement formats
  - Negative amounts for debits
  - Robust date parsing
- **Extracts**:
  - List of transactions with:
    - Amount (positive/negative)
    - Currency
    - Date
    - Description (max 1000 chars)
- **Error Handling**: Returns empty list on failure, filters incomplete transactions

## Configuration

Add these properties to `application.properties`:

```properties
# OCR Configuration
# Path to Tesseract tessdata directory (leave empty to use default)
# ocr.tesseract.datapath=/usr/local/share/tessdata
ocr.tesseract.language=eng+deu+fra
ocr.dpi=300

# Composite Extractor Fallback Configuration
ocr.fallback.min-text-length=100
ocr.fallback.min-lines=5
ocr.fallback.min-char-per-line=10
```

## Installation Requirements

### Tesseract OCR
**macOS:**
```bash
brew install tesseract
brew install tesseract-lang  # For additional languages
```

**Ubuntu/Debian:**
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>service</artifactId>
    <version>0.18.2</version>
</dependency>
```bash
sudo apt-get install tesseract-ocr
sudo apt-get install tesseract-ocr-deu  # German
sudo apt-get install tesseract-ocr-fra  # French
```

**Windows:**
Download installer from: https://github.com/UB-Mannheim/tesseract/wiki

### Maven Dependencies
Already configured in `pom.xml`:
```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.13.0</version>
</dependency>
```

## Usage

Th# Using AI Extractors

```java
@Autowired
private OpenAiInvoiceExtractor invoiceExtractor;

@Autowired
private OpenAiStatementExtractor statementExtractor;

// Extract invoice fields
InterpretedText text = textExtractor.extract(documentId);
InvoiceFields invoice = invoiceExtractor.extract(text);

// Extract statement transactions
List<StatementTransaction> transactions = statementExtractor.extract(text);
```

## Response Formatsomponents and can be autowired:

``# InvoiceFields Response

```json
{
    "amount": 1234.56,
    "currency": "EUR",
    "date": "2026-01-14",
    "description": "Professional services",
    "sender": "ACME Corporation"
}
```

### StatementTransaction List Response

```json
[
    {
        "amount": -150.00,
        "currency": "USD",
        "date": "2026-01-10",
        "description": "Payment to Merchant ABC"
    },
    {
        "amount": 2500.00,
        "currency": "USD",
        "date": "2026-01-05",
        "description": "Salary deposit"
    }
]
```

##`java
@Autowired
@Qualifier("compositeTextExtractor")
private DocumentTextInterpreter textExtractor;

// Extract text from a document
InterpretedText result = textExtractor.extract(documentId);
```
# OpenAI (API-dependent)
- Processing time: ~1-3s per request
- Cost: Token-based pricing (~$0.0001-0.0003 per request for gpt-4o-mini)
- Requires internet connection
- Subject to rate limits
- Best for: Structured field extraction with high accuracy

## Cost Optimization for OpenAI

1. **Model Selection**:
   - `gpt-4o-mini`: Cheaper, faster, good accuracy (recommended)
   - `gpt-4o`: Higher accuracy, more expensive
   - `gpt-3.5-turbo`: Cheapest, lower accuracy

2. **Text Length Limiting**:
   - Invoice extraction: Max 3000 chars (already implemented)
   - Statement extraction: Max 4000 chars (already implemented)
# "OpenAI API errors"
- Verify API key is set correctly
- Check `openai.enabled=true`
- Verify internet connectivity
- Check rate limits on OpenAI dashboard
- Review timeout setting if requests are slow

### "Empty extraction results"
- OpenAI may be disabled or not configured
- Check logs for specific error messages
- Verify text input quality
- Try with different model (gpt-4o for better accuracy)

### "Invalid JSON response from OpenAI"
- Rare but handled gracefully
- Check logs for actual response
- May need prompt adjustment for specific formats

##
3. **Caching**:

5. **AI Enhancements**
   - Response caching
   - Batch processing
   - Custom fine-tuned models
   - Fallback to rule-based extraction
   - Confidence scoring for AI extractions
   - Multi-language prompt templates
   - Cache extraction results
   - Don't re-extract unchanged documents

4. **Fallback Strategy**:
   - Use regex/rule-based extraction for simple cases
   - Use AI only when needed

##
### Using Specific Extractors

```java
@Autowired
private PdfBoxTextExtractor pdfBoxExtractor;

@Autowired
private OcrTextExtractor ocrExtractor;

// Use PDFBox directly
InterpretedText pdfBoxResult = pdfBoxExtractor.extract(documentId);

// Use OCR directly
InterpretedText ocrResult = ocrExtractor.extract(documentId);
```

## InterpretedText Response

```java
{
    "rawText": "Full extracted text...",
    "lines": ["Line 1", "Line 2", ...],
    "metadata": {
        "pageCount": 2,
        "extractor": "PDFBox",
        "compositeExtractor": true,
        "extractionMethod": "PDFBox",
        "textLength": 1234,
        "lineCount": 45
    },
    "ocrUsed": false,
    "languageDetected": "en"
}
```

## Performance Considerations

### PDFBox (Fast)
- Processing time: ~100-500ms for typical document
- Memory usage: Low
- Best for: Native PDF text

### OCR (Slow)
- Processing time: ~2-10s per page at 300 DPI
- Memory usage: High (image rendering + OCR)
- Best for: Scanned documents

### Composite (Smart)
- Combines both approaches
- Minimizes OCR usage (only when necessary)
- Best overall performance/accuracy balance

## Troubleshooting

### "Tesseract not found"
- Ensure Tesseract is installed
- Set `ocr.tesseract.datapath` if installed in non-standard location
- Verify tessdata directory contains language files

### "Poor OCR quality"
- Increase DPI: `ocr.dpi=600`
- Ensure correct language is configured
- Check document image quality

### "PDFBox extraction returns empty text"
- Document may be scanned (no text layer)
- CompositeTextExtractor will automatically fallback to OCR
- Use OcrTextExtractor directly for known scanned documents

## Future Enhancements

1. **Mobile OCR Integration**
   - Cloud OCR services integration
   - Mobile device on-device OCR support
   - Hybrid processing strategies

2. **Advanced Language Detection**
   - Integration with language detection libraries
   - Improved multi-language support

3. **Performance Optimization**
   - Parallel page processing
   - Caching mechanisms
   - Adaptive DPI selection

4. **Quality Metrics**
   - Confidence scoring for OCR results
   - Automatic quality assessment
   - User feedback integration
