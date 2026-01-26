package com.frnholding.pocketaccount;

import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
import com.frnholding.pocketaccount.interpretation.infra.OpenAiStatementExtractor;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * AI-based test for interpreting STATEMENT documents from testdata folder using OpenAI.
 * Reads PDFs directly without database storage - only displays results in terminal.
 */
@Slf4j
@SpringBootTest
public class StatementInterpretationAiTest {

    @Autowired
    private OpenAiStatementExtractor aiExtractor;

    @Value("${openai.enabled:false}")
    private boolean openAiEnabled;

    @Value("${openai.api.key:#{null}}")
    private String openAiApiKey;

    private static final String TESTDATA_DIR = "testdata";

    @Test
    public void testStatementInterpretationWithAi() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("STATEMENT DOCUMENT AI INTERPRETATION TEST - NO DATABASE");
        System.out.println("Using OpenAI for extraction");
        System.out.println("=".repeat(80) + "\n");

        // Check if OpenAI is configured
        if (!openAiEnabled || openAiApiKey == null || openAiApiKey.isEmpty()) {
            System.err.println("ERROR: OpenAI is not enabled or API key not configured");
            System.err.println("Set openai.enabled=true and openai.api.key in application.properties");
            return;
        }

        System.out.println("✓ OpenAI is enabled and configured\n");

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
        System.out.println("AI TEST COMPLETED - No data stored in database");
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

            // Test AI extraction
            System.out.println("\n  → Running AI (OpenAI) extraction...");
            long startTime = System.currentTimeMillis();
            List<StatementTransaction> aiTransactions = aiExtractor.extract(interpretedText);
            long duration = System.currentTimeMillis() - startTime;
            
            displayResult(aiTransactions, "ARTIFICIAL INTELLIGENCE (OpenAI)", "OpenAiStatementExtractor", duration);

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

    private void displayResult(List<StatementTransaction> transactions, String method, String extractorName, long durationMs) {
        System.out.println("    ┌─ " + method + " RESULTS ─────────────────────────────────");
        System.out.println("    │ Extraction Method: " + extractorName);
        System.out.println("    │ Processing Time: " + durationMs + " ms");
        
        if (transactions != null && !transactions.isEmpty()) {
            System.out.println("    │ Transactions Found: " + transactions.size());
            System.out.println("    │");
            System.out.println("    │ Transaction Details:");
            
            double totalWithdrawals = 0.0;
            double totalDeposits = 0.0;
            
            for (int i = 0; i < transactions.size(); i++) {
                StatementTransaction tx = transactions.get(i);
                String type = tx.getAmount() < 0 ? "Withdrawal" : "Deposit  ";
                
                if (tx.getAmount() < 0) {
                    totalWithdrawals += tx.getAmount();
                } else {
                    totalDeposits += tx.getAmount();
                }
                
                System.out.println("    │   [" + (i + 1) + "] " + type + " | Date: " + tx.getDate() + 
                                 " | Amount: " + formatAmount(tx.getAmount()) + 
                                 " | Desc: " + truncate(tx.getDescription(), 30));
            }
            
            System.out.println("    │");
            System.out.println("    │ Summary:");
            System.out.println("    │   Total Withdrawals: " + formatAmount(totalWithdrawals));
            System.out.println("    │   Total Deposits: " + formatAmount(totalDeposits));
            System.out.println("    │   Net Change: " + formatAmount(totalWithdrawals + totalDeposits));
        } else {
            System.out.println("    │ Transactions Found: 0");
            System.out.println("    │ ⚠ No transactions extracted - check OpenAI API key and credits");
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
