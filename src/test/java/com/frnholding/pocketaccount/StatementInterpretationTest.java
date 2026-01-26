package com.frnholding.pocketaccount;

import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
import com.frnholding.pocketaccount.interpretation.infra.HeuristicStatementExtractor;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Simple test for interpreting STATEMENT documents from testdata folder.
 * Reads PDFs directly without database storage - only displays results in terminal.
 */
@Slf4j
@SpringBootTest
public class StatementInterpretationTest {

    @Autowired
    private HeuristicStatementExtractor heuristicExtractor;

    private static final String TESTDATA_DIR = "testdata";

    @Test
    public void testStatementInterpretation() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("STATEMENT DOCUMENT INTERPRETATION TEST - NO DATABASE");
        System.out.println("=".repeat(80) + "\n");

        Path testdataPath = Paths.get(TESTDATA_DIR);
        if (!Files.exists(testdataPath)) {
            System.err.println("ERROR: testdata directory not found at: " + testdataPath.toAbsolutePath());
            return;
        }

        // Get all PDF files from testdata
        try (Stream<Path> paths = Files.list(testdataPath)) {
            List<Path> pdfFiles = paths
                    .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .sorted()
                    .toList();

            if (pdfFiles.isEmpty()) {
                System.err.println("ERROR: No PDF files found in testdata directory");
                return;
            }

            System.out.println("Found " + pdfFiles.size() + " PDF files to process\n");

            int fileNumber = 1;
            for (Path pdfFile : pdfFiles) {
                processPdfFile(pdfFile, fileNumber++, pdfFiles.size());
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST COMPLETED - No data stored in database");
        System.out.println("=".repeat(80) + "\n");
    }

    private void processPdfFile(Path pdfFile, int fileNumber, int totalFiles) {
        String filename = pdfFile.getFileName().toString();
        
        System.out.println("┌" + "─".repeat(78) + "┐");
        System.out.println("│ [" + fileNumber + "/" + totalFiles + "] Processing: " + filename);
        System.out.println("└" + "─".repeat(78) + "┘");

        try {
            // Extract text directly from PDF using PDFBox
            InterpretedText interpretedText = extractTextFromPdf(pdfFile.toFile());
            System.out.println("  ✓ Text extracted: " + interpretedText.getLines().size() + " lines, " + 
                             interpretedText.getRawText().length() + " characters");
            
            // Debug: Print first 30 lines to see the format
            if (filename.contains("september")) {
                System.out.println("\n  → Debug: First 30 lines of extracted text:");
                List<String> lines = interpretedText.getLines();
                for (int i = 0; i < Math.min(30, lines.size()); i++) {
                    System.out.println("    Line " + (i+1) + ": [" + lines.get(i) + "]");
                }
                System.out.println();
            }

            // Test HEURISTIC extraction
            System.out.println("\n  → Running HEURISTIC extraction...");
            List<StatementTransaction> heuristicTransactions = heuristicExtractor.extract(interpretedText);
            displayResult(heuristicTransactions, "HEURISTIC", "HeuristicStatementExtractor");

            System.out.println();
        } catch (Exception e) {
            System.err.println("  ✗ ERROR processing file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private InterpretedText extractTextFromPdf(File pdfFile) throws Exception {
        try (PDDocument pdDocument = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            
            String rawText = stripper.getText(pdDocument);
            List<String> lines = Arrays.stream(rawText.split("\n"))
                    .filter(line -> !line.trim().isEmpty())
                    .toList();

            InterpretedText result = new InterpretedText();
            result.setRawText(rawText);
            result.setLines(lines);
            result.setOcrUsed(false);
            result.setLanguageDetected("nb");
            result.setTextExtractorUsed("PDFBox");
            
            return result;
        }
    }

    private void displayResult(List<StatementTransaction> transactions, String method, String extractorName) {
        System.out.println("    ┌─ " + method + " RESULTS ─────────────────────────────────");
        System.out.println("    │ Extraction Method: " + extractorName);
        
        if (transactions != null && !transactions.isEmpty()) {
            System.out.println("    │ Transactions Found: " + transactions.size());
            System.out.println("    │");
            System.out.println("    │ Transaction Details:");
            
            for (int i = 0; i < transactions.size(); i++) {
                StatementTransaction tx = transactions.get(i);
                String type = tx.getAmount() < 0 ? "Withdrawal" : "Deposit  ";
                System.out.println("    │   [" + (i + 1) + "] " + type + " | Date: " + tx.getDate() + 
                                 " | Amount: " + formatAmount(tx.getAmount()) + 
                                 " | Desc: " + truncate(tx.getDescription(), 30));
            }
        } else {
            System.out.println("    │ Transactions Found: 0");
        }
        
        System.out.println("    └" + "─".repeat(60));
    }

    private String formatAmount(Double amount) {
        if (amount == null) return "N/A";
        return String.format("%,.2f", amount);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
