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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

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
        
        var document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        File inputFile = new File(document.getFilePath());
        if (!inputFile.exists()) {
            throw new IllegalStateException("Document file not found: " + document.getFilePath());
        }

        try {
            Tesseract tesseract = configureTesseract();
            
            List<String> allLines = new ArrayList<>();
            StringBuilder rawTextBuilder = new StringBuilder();
            Map<String, Object> metadata = new HashMap<>();
            
            if (isPdfFile(inputFile)) {
                // Convert PDF pages to images and run OCR
                try (PDDocument pdDocument = Loader.loadPDF(inputFile)) {
                    PDFRenderer renderer = new PDFRenderer(pdDocument);
                    int pageCount = pdDocument.getNumberOfPages();
                    metadata.put("pageCount", pageCount);
                    
                    for (int page = 0; page < pageCount; page++) {
                        log.debug("Processing page {} of {} with OCR", page + 1, pageCount);
                        
                        BufferedImage image = renderer.renderImageWithDPI(page, dpi);
                        BufferedImage scaled = dpi <= 300 ? scaleImage(image, 2) : image;
                        BufferedImage processed = preprocessForOcr(scaled);
                        String pageText = runOcrWithFallback(processed, tesseract);
                        
                        if (pageText != null && !pageText.trim().isEmpty()) {
                            rawTextBuilder.append(pageText).append("\n");
                            Arrays.stream(pageText.split("\n"))
                                    .filter(line -> !line.trim().isEmpty())
                                    .forEach(allLines::add);
                        }
                    }
                }
            } else {
                BufferedImage image = ImageIO.read(inputFile);
                if (image == null) {
                    throw new IOException("Unsupported image format for OCR: " + inputFile.getName());
                }
                BufferedImage scaled = dpi <= 300 ? scaleImage(image, 2) : image;
                BufferedImage processed = preprocessForOcr(scaled);
                String pageText = runOcrWithFallback(processed, tesseract);
                metadata.put("pageCount", 1);
                metadata.put("imageFormat", getExtension(inputFile.getName()));

                if (pageText != null && !pageText.trim().isEmpty()) {
                    rawTextBuilder.append(pageText).append("\n");
                    Arrays.stream(pageText.split("\n"))
                            .filter(line -> !line.trim().isEmpty())
                            .forEach(allLines::add);
                }
            }

            String rawText = rawTextBuilder.toString().trim();
                log.debug("OCR raw text (first 1500 chars): {}",
                    rawText.length() > 1500 ? rawText.substring(0, 1500) : rawText);
            metadata.put("extractor", "Tesseract OCR");
            metadata.put("originalFilename", document.getOriginalFilename());
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
        
        // Optimize for receipt-style text recognition
        tesseract.setPageSegMode(6); // Assume a single uniform block of text
        tesseract.setOcrEngineMode(3); // Default, based on what is available
        tesseract.setTessVariable("preserve_interword_spaces", "1");
        tesseract.setTessVariable("tessedit_char_whitelist", "");
        tesseract.setTessVariable("user_defined_dpi", String.valueOf(dpi));
        
        // Mobile OCR preparation: can be enhanced with custom config
        // For mobile, consider: smaller models, faster processing, lower quality acceptable
        
        return tesseract;
    }

    private String runOcrWithFallback(BufferedImage image, Tesseract tesseract) throws TesseractException {
        String primary = runOcr(image, tesseract, 6, true);
        String secondary = runOcr(image, tesseract, 3, false);

        int primaryScore = scoreOcrText(primary);
        int secondaryScore = scoreOcrText(secondary);

        return secondaryScore > primaryScore ? secondary : primary;
    }

    private boolean isPdfFile(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private String runOcr(BufferedImage image, Tesseract tesseract, int pageSegMode, boolean whitelist) throws TesseractException {
        tesseract.setPageSegMode(pageSegMode);
        if (whitelist) {
            tesseract.setTessVariable("tessedit_char_whitelist", "0123456789.,:-krNOKTotaltSUMbeloep ");
        } else {
            tesseract.setTessVariable("tessedit_char_whitelist", "");
        }
        return tesseract.doOCR(image);
    }

    private int scoreOcrText(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String lower = text.toLowerCase();
        int digitCount = (int) lower.chars().filter(Character::isDigit).count();
        int keywordHits = 0;
        if (lower.contains("tot")) {
            keywordHits += 2;
        }
        if (lower.contains("total")) {
            keywordHits += 3;
        }
        if (lower.contains("kr")) {
            keywordHits += 2;
        }
        if (lower.contains("nok")) {
            keywordHits += 2;
        }
        return digitCount + (keywordHits * 10);
    }

    private BufferedImage preprocessForOcr(BufferedImage image) {
        BufferedImage grayscale = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(image, 0, 0, Color.WHITE, null);
        g2d.dispose();

        WritableRaster raster = grayscale.getRaster();
        int width = grayscale.getWidth();
        int height = grayscale.getHeight();

        int threshold = computeThreshold(raster, width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = raster.getSample(x, y, 0);
                int binary = value > threshold ? 255 : 0;
                raster.setSample(x, y, 0, binary);
            }
        }
        return grayscale;
    }

    private BufferedImage scaleImage(BufferedImage image, int factor) {
        if (image == null || factor <= 1) {
            return image;
        }
        int width = image.getWidth() * factor;
        int height = image.getHeight() * factor;
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = scaled.createGraphics();
        g2d.drawImage(image, 0, 0, width, height, Color.WHITE, null);
        g2d.dispose();
        return scaled;
    }

    private int computeThreshold(WritableRaster raster, int width, int height) {
        long sum = 0;
        long count = (long) width * height;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sum += raster.getSample(x, y, 0);
            }
        }
        return count == 0 ? 128 : (int) (sum / count);
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
