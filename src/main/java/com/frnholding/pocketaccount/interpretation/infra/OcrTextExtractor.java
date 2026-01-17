package com.frnholding.pocketaccount.interpretation.infra;

import com.frnholding.pocketaccount.repository.DocumentRepository;
import com.frnholding.pocketaccount.interpretation.pipeline.DocumentTextInterpreter;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Extracts text from PDF documents using OCR (Tesseract).
 * This extractor works for scanned PDFs and images without text layers.
 * Supports mobile OCR preparation and multiple language recognition.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrTextExtractor implements DocumentTextInterpreter {

    private final DocumentRepository documentRepository;

    @Value("${ocr.tesseract.datapath:#{null}}")
    private String tesseractDataPath;

    @Value("${ocr.tesseract.language:eng+deu+fra}")
    private String tesseractLanguages;

    @Value("${ocr.dpi:300}")
    private int dpi;

    @Override
    public InterpretedText extract(UUID documentId) {
        log.info("Extracting text from document {} using OCR", documentId);
        
        var document = documentRepository.findById(documentId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        File pdfFile = new File(document.getFilePath());
        if (!pdfFile.exists()) {
            throw new IllegalStateException("Document file not found: " + document.getFilePath());
        }

        try {
            Tesseract tesseract = configureTesseract();
            
            List<String> allLines = new ArrayList<>();
            StringBuilder rawTextBuilder = new StringBuilder();
            Map<String, Object> metadata = new HashMap<>();
            
            // Convert PDF pages to images and run OCR
            try (PDDocument pdDocument = Loader.loadPDF(pdfFile)) {
                PDFRenderer renderer = new PDFRenderer(pdDocument);
                int pageCount = pdDocument.getNumberOfPages();
                metadata.put("pageCount", pageCount);
                
                for (int page = 0; page < pageCount; page++) {
                    log.debug("Processing page {} of {} with OCR", page + 1, pageCount);
                    
                    BufferedImage image = renderer.renderImageWithDPI(page, dpi);
                    String pageText = tesseract.doOCR(image);
                    
                    if (pageText != null && !pageText.trim().isEmpty()) {
                        rawTextBuilder.append(pageText).append("\n");
                        Arrays.stream(pageText.split("\n"))
                                .filter(line -> !line.trim().isEmpty())
                                .forEach(allLines::add);
                    }
                }
            }

            String rawText = rawTextBuilder.toString().trim();
            metadata.put("extractor", "Tesseract OCR");
            metadata.put("ocrLanguages", tesseractLanguages);
            metadata.put("dpi", dpi);
            metadata.put("characterCount", rawText.length());
            metadata.put("mobileReady", true); // Prepared for mobile OCR

            log.info("Successfully extracted {} lines from document using OCR", allLines.size());

            InterpretedText result = new InterpretedText(
                    rawText,
                    allLines,
                    metadata,
                    true, // OCR used
                    detectLanguageFromOcr(rawText),
                    null  // textExtractorUsed set below
            );
            result.setTextExtractorUsed("Tesseract");
            return result;

        } catch (IOException | TesseractException e) {
            log.error("Failed to extract text using OCR: {}", e.getMessage(), e);
            throw new RuntimeException("OCR text extraction failed", e);
        }
    }

    /**
     * Configures Tesseract OCR engine with appropriate settings.
     * Prepared for mobile OCR integration.
     */
    private Tesseract configureTesseract() {
        Tesseract tesseract = new Tesseract();
        
        // Set data path if configured (for tessdata directory)
        if (tesseractDataPath != null && !tesseractDataPath.isEmpty()) {
            tesseract.setDatapath(tesseractDataPath);
        }
        
        // Set languages (supports multiple: eng+deu+fra)
        tesseract.setLanguage(tesseractLanguages);
        
        // Optimize for document text recognition
        tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
        tesseract.setOcrEngineMode(3); // Default, based on what is available
        
        // Mobile OCR preparation: can be enhanced with custom config
        // For mobile, consider: smaller models, faster processing, lower quality acceptable
        
        return tesseract;
    }

    /**
     * Detects language from OCR text.
     * Enhanced for mobile OCR scenarios.
     */
    private String detectLanguageFromOcr(String text) {
        if (text == null || text.isEmpty()) {
            return "unknown";
        }
        
        // Check which language is most prevalent
        String[] languages = tesseractLanguages.split("\\+");
        if (languages.length == 1) {
            return languages[0];
        }
        
        // Simple character-based detection
        long germanChars = text.chars().filter(c -> c == 'ä' || c == 'ö' || c == 'ü' || c == 'ß').count();
        long frenchChars = text.chars().filter(c -> c == 'é' || c == 'è' || c == 'ê' || c == 'à' || c == 'ç').count();
        
        if (germanChars > frenchChars && germanChars > 5) {
            return "deu";
        } else if (frenchChars > germanChars && frenchChars > 5) {
            return "fra";
        }
        
        return "eng"; // Default to English
    }

    /**
     * For mobile OCR integration:
     * This class can be extended to support:
     * - Cloud OCR services (Google Vision, AWS Textract, Azure OCR)
     * - Mobile device on-device OCR
     * - Hybrid approach: quick mobile OCR + server-side verification
     */
}
