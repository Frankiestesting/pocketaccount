package com.frnholding.pocketaccount.interpretation.infra;

import com.frnholding.pocketaccount.interpretation.pipeline.DocumentTextInterpreter;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import com.frnholding.pocketaccount.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Composite text extractor that intelligently selects between PDFBox and OCR.
 * Strategy: Try PDFBox first (faster), fallback to OCR if text extraction is poor.
 * 
 * This provides the best balance between speed and accuracy:
 * - PDFBox for native PDF text (fast, accurate)
 * - OCR for scanned documents or poor quality PDFs (slower, but necessary)
 */
@Component("compositeTextExtractor")
public class CompositeTextExtractor implements DocumentTextInterpreter {

    private static final Logger log = LoggerFactory.getLogger(CompositeTextExtractor.class);

    private final PdfBoxTextExtractor pdfBoxExtractor;
    private final OcrTextExtractor ocrExtractor;
    private final DocumentRepository documentRepository;

    public CompositeTextExtractor(PdfBoxTextExtractor pdfBoxExtractor, OcrTextExtractor ocrExtractor, DocumentRepository documentRepository) {
        this.pdfBoxExtractor = pdfBoxExtractor;
        this.ocrExtractor = ocrExtractor;
        this.documentRepository = documentRepository;
    }

    @Value("${ocr.fallback.min-text-length:100}")
    private int minTextLength;

    @Value("${ocr.fallback.min-lines:5}")
    private int minLines;

    @Value("${ocr.fallback.min-char-per-line:10}")
    private int minCharsPerLine;

    @Override
    public InterpretedText extract(UUID documentId) {
        log.info("Starting composite text extraction for document {}", documentId);
        
        boolean isPdf = isPdfDocument(documentId);

        // Step 1: Try PDFBox extraction first (fast path)
        InterpretedText pdfBoxResult = null;
        boolean pdfBoxSuccessful = false;
        
        if (isPdf) {
            try {
                pdfBoxResult = pdfBoxExtractor.extract(documentId);
                pdfBoxSuccessful = isExtractionSufficient(pdfBoxResult);
                
                if (pdfBoxSuccessful) {
                    log.info("PDFBox extraction successful for document {}, OCR not needed", documentId);
                    pdfBoxResult.setTextExtractorUsed("PDFBox");
                    enrichMetadata(pdfBoxResult, "PDFBox", false);
                    return pdfBoxResult;
                } else {
                    log.info("PDFBox extraction insufficient for document {}, falling back to OCR. " +
                            "Extracted {} chars in {} lines", 
                            documentId, 
                            pdfBoxResult.getRawText().length(), 
                            pdfBoxResult.getLines().size());
                }
            } catch (Exception e) {
                log.warn("PDFBox extraction failed for document {}: {}. Falling back to OCR", 
                        documentId, e.getMessage());
            }
        } else {
            log.info("Document {} is not a PDF, skipping PDFBox and using OCR", documentId);
        }

        // Step 2: Fallback to OCR (slow but thorough path)
        try {
            InterpretedText ocrResult = ocrExtractor.extract(documentId);
            log.info("OCR extraction completed for document {}, extracted {} chars in {} lines",
                    documentId, ocrResult.getRawText().length(), ocrResult.getLines().size());
            
            ocrResult.setTextExtractorUsed("Composite(OCR)");
            enrichMetadata(ocrResult, "OCR", true);
            
            // Include PDFBox attempt info in metadata
            if (pdfBoxResult != null) {
                ocrResult.getMetadata().put("pdfboxAttempted", true);
                ocrResult.getMetadata().put("pdfboxCharCount", pdfBoxResult.getRawText().length());
                ocrResult.getMetadata().put("pdfboxLineCount", pdfBoxResult.getLines().size());
            }
            
            return ocrResult;
            
        } catch (Exception e) {
            log.error("Both PDFBox and OCR extraction failed for document {}", documentId, e);
            
            // If we have a PDFBox result (even if poor), return it as last resort
            if (pdfBoxResult != null) {
                log.warn("Returning incomplete PDFBox result as last resort for document {}", documentId);
                enrichMetadata(pdfBoxResult, "PDFBox (fallback)", false);
                pdfBoxResult.getMetadata().put("extractionQuality", "poor");
                pdfBoxResult.getMetadata().put("ocrFailed", true);
                return pdfBoxResult;
            }
            
            throw new RuntimeException("All text extraction methods failed for document " + documentId, e);
        }
    }

    /**
     * Determines if the extraction result has sufficient text to be useful.
     * Uses multiple heuristics to assess quality:
     * - Total text length
     * - Number of lines
     * - Average characters per line
     */
    private boolean isExtractionSufficient(InterpretedText result) {
        if (result == null) {
            return false;
        }

        String rawText = result.getRawText();
        if (rawText == null || rawText.isEmpty()) {
            return false;
        }

        int textLength = rawText.length();
        int lineCount = result.getLines().size();

        // Check minimum text length
        if (textLength < minTextLength) {
            log.debug("Text length {} below minimum {}", textLength, minTextLength);
            return false;
        }

        // Check minimum line count
        if (lineCount < minLines) {
            log.debug("Line count {} below minimum {}", lineCount, minLines);
            return false;
        }

        // Check average characters per line
        double avgCharsPerLine = lineCount > 0 ? (double) textLength / lineCount : 0;
        if (avgCharsPerLine < minCharsPerLine) {
            log.debug("Average chars per line {} below minimum {}", avgCharsPerLine, minCharsPerLine);
            return false;
        }

        // Check for meaningful content (not just whitespace or special chars)
        long alphanumericCount = rawText.chars()
                .filter(Character::isLetterOrDigit)
                .count();
        double alphanumericRatio = (double) alphanumericCount / textLength;
        
        if (alphanumericRatio < 0.5) {
            log.debug("Alphanumeric ratio {} too low, text may be garbled", alphanumericRatio);
            return false;
        }

        log.debug("Extraction sufficient: {} chars, {} lines, avg {:.1f} chars/line, {:.1%} alphanumeric",
                textLength, lineCount, avgCharsPerLine, alphanumericRatio);
        return true;
    }

    private boolean isPdfDocument(UUID documentId) {
        return documentRepository.findById(documentId.toString())
                .map(entity -> entity.getFilePath())
                .map(path -> path.toLowerCase(Locale.ROOT).endsWith(".pdf"))
                .orElse(true);
    }

    /**
     * Enriches the result metadata with composite extraction information.
     */
    private void enrichMetadata(InterpretedText result, String method, boolean ocrUsed) {
        Map<String, Object> metadata = result.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            result.setMetadata(metadata);
        }
        
        metadata.put("compositeExtractor", true);
        metadata.put("extractionMethod", method);
        metadata.put("ocrUsed", ocrUsed);
        
        // Add quality metrics
        int textLength = result.getRawText().length();
        int lineCount = result.getLines().size();
        metadata.put("textLength", textLength);
        metadata.put("lineCount", lineCount);
        metadata.put("avgCharsPerLine", lineCount > 0 ? (double) textLength / lineCount : 0);
    }
}
