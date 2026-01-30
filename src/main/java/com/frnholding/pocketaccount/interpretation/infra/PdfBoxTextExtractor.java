package com.frnholding.pocketaccount.interpretation.infra;

import com.frnholding.pocketaccount.repository.DocumentRepository;
import com.frnholding.pocketaccount.interpretation.pipeline.DocumentTextInterpreter;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Extracts text from PDF documents using Apache PDFBox.
 * This extractor works well for PDFs with selectable text layers.
 */
@Component
public class PdfBoxTextExtractor implements DocumentTextInterpreter {

    private static final Logger log = LoggerFactory.getLogger(PdfBoxTextExtractor.class);
    private final DocumentRepository documentRepository;

    public PdfBoxTextExtractor(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public InterpretedText extract(UUID documentId) {
        log.info("Extracting text from document {} using PDFBox", documentId);
        
        var document = documentRepository.findById(documentId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        File pdfFile = new File(document.getFilePath());
        if (!pdfFile.exists()) {
            throw new IllegalStateException("Document file not found: " + document.getFilePath());
        }

        try (PDDocument pdDocument = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            
            String rawText = stripper.getText(pdDocument);
            List<String> lines = Arrays.stream(rawText.split("\n"))
                    .filter(line -> !line.trim().isEmpty())
                    .toList();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pageCount", pdDocument.getNumberOfPages());
            metadata.put("encrypted", pdDocument.isEncrypted());
            metadata.put("version", pdDocument.getVersion());
            metadata.put("producer", Optional.ofNullable(pdDocument.getDocumentInformation())
                    .map(info -> info.getProducer())
                    .orElse("Unknown"));
            metadata.put("author", Optional.ofNullable(pdDocument.getDocumentInformation())
                    .map(info -> info.getAuthor())
                    .orElse("Unknown"));
            metadata.put("extractor", "PDFBox");

            log.info("Successfully extracted {} lines from {} pages using PDFBox", 
                    lines.size(), pdDocument.getNumberOfPages());

            InterpretedText result = new InterpretedText(
                    rawText.trim(),
                    lines,
                    metadata,
                    false, // OCR not used
                    detectLanguage(rawText),
                    null  // textExtractorUsed set below
            );
            result.setTextExtractorUsed("PDFBox");
            return result;

        } catch (IOException e) {
            log.error("Failed to extract text from PDF using PDFBox: {}", e.getMessage(), e);
            throw new RuntimeException("PDF text extraction failed", e);
        }
    }

    /**
     * Simple language detection based on character analysis.
     * Can be enhanced with dedicated language detection libraries.
     */
    private String detectLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return "unknown";
        }
        
        // Simple heuristic: check for common characters
        long germanChars = text.chars().filter(c -> c == 'ä' || c == 'ö' || c == 'ü' || c == 'ß').count();
        long frenchChars = text.chars().filter(c -> c == 'é' || c == 'è' || c == 'ê' || c == 'à' || c == 'ç').count();
        
        if (germanChars > frenchChars && germanChars > 5) {
            return "de";
        } else if (frenchChars > germanChars && frenchChars > 5) {
            return "fr";
        }
        
        return "en"; // Default to English
    }
}
