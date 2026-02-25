package com.frnholding.pocketaccount.interpretation.pipeline;

import com.frnholding.pocketaccount.interpretation.domain.InterpretationResult;
import com.frnholding.pocketaccount.interpretation.domain.InvoiceFieldsDTO;
import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
import com.frnholding.pocketaccount.interpretation.infra.OpenAiAuthenticationException;
import com.frnholding.pocketaccount.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class InterpretationPipeline {

    private static final String DEFAULT_CURRENCY = "NOK";

    @Autowired(required = false)
    @Qualifier("compositeTextExtractor")
    private DocumentTextInterpreter documentTextInterpreter;

    @Autowired(required = false)
    private DocumentClassifier documentClassifier;

    @Autowired(required = false)
    @Qualifier("regexInvoiceExtractor")
    private InvoiceExtractor heuristicInvoiceExtractor;

    @Autowired(required = false)
    @Qualifier("openAiInvoiceExtractor")
    private InvoiceExtractor aiInvoiceExtractor;

    @Autowired(required = false)
    @Qualifier("taxiReceiptExtractor")
    private InvoiceExtractor taxiReceiptExtractor;

    @Autowired(required = false)
    @Qualifier("heuristicStatementExtractor")
    private StatementExtractor heuristicStatementExtractor;

    @Autowired(required = false)
    @Qualifier("openAiStatementExtractor")
    private StatementExtractor aiStatementExtractor;

    @Autowired(required = false)
    private ConfidenceScorer confidenceScorer;

    @Autowired
    private DocumentRepository documentRepository;

    /**
     * Executes the interpretation pipeline for a document
     * 
     * @param documentId the document UUID
     * @param options interpretation options (useOcr, useAi, languageHint, hintedType)
     * @return InterpretationResult with extracted data
     */
    public InterpretationResult execute(UUID documentId, InterpretationOptions options) {
        log.info("Starting interpretation pipeline for document: {} with options: {}", documentId, options);

        try {
            // Step 1: Extract/Interpret text from document
            InterpretedText interpretedText = extractText(documentId);
            log.debug("Text extraction completed. OCR used: {}, Language: {}", 
                    interpretedText.isOcrUsed(), interpretedText.getLanguageDetected());

            // Step 2: Classify document type
            DocumentType documentType = classifyDocument(interpretedText, options.getHintedType());
            log.info("Document classified as: {}", documentType);

            // Step 3: Extract fields based on document type
            InterpretationResult result = new InterpretationResult();
            result.setDocumentId(documentId.toString());
            result.setDocumentType(documentType.name());
            result.setInterpretedAt(Instant.now());

            // Track extraction methods used
            StringBuilder extractionMethods = new StringBuilder();
            if (interpretedText.getTextExtractorUsed() != null) {
                extractionMethods.append(interpretedText.getTextExtractorUsed());
            }
            if (interpretedText.isOcrUsed()) {
                if (extractionMethods.length() > 0) extractionMethods.append(", ");
                extractionMethods.append("OCR");
            }

            if (documentType == DocumentType.INVOICE || documentType == DocumentType.RECEIPT) {
                InvoiceExtractor extractor = options.isUseAi() ? aiInvoiceExtractor : heuristicInvoiceExtractor;
                if (documentType == DocumentType.RECEIPT && isTaxiReceipt(documentId) && taxiReceiptExtractor != null) {
                    extractor = taxiReceiptExtractor;
                }
                InvoiceFieldsDTO invoiceFields = extractInvoiceFields(interpretedText, extractor, extractionMethods);
                if (invoiceFields != null && isBlank(invoiceFields.getCurrency())) {
                    invoiceFields.setCurrency(DEFAULT_CURRENCY);
                }
                result.setInvoiceFields(invoiceFields);
                log.debug("Invoice/receipt fields extracted: {}", invoiceFields);
            } else if (documentType == DocumentType.STATEMENT) {
                List<StatementTransaction> transactions = extractStatementTransactions(interpretedText, result, options.isUseAi(), extractionMethods);
                for (StatementTransaction transaction : transactions) {
                    if (transaction != null && isBlank(transaction.getCurrency())) {
                        transaction.setCurrency(DEFAULT_CURRENCY);
                    }
                }
                result.setStatementTransactions(transactions);
                log.debug("Statement transactions extracted: {} transactions", transactions.size());
            } else {
                log.info("Document type {} has no extraction step; result will be empty", documentType);
            }

            // Set the extraction methods used
            result.setExtractionMethods(extractionMethods.toString());

            // Step 4: Calculate confidence scores (if available)
            if (confidenceScorer != null) {
                Map<String, Double> scores = calculateConfidenceScores(result, interpretedText);
                log.debug("Confidence scores calculated: {}", scores);
            }

            log.info("Interpretation pipeline completed successfully for document: {}", documentId);
            return result;

        } catch (OpenAiAuthenticationException e) {
            log.error("OpenAI authentication failed in interpretation pipeline for document: {}", documentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Error in interpretation pipeline for document: {}", documentId, e);
            throw new RuntimeException("Interpretation pipeline failed: " + e.getMessage(), e);
        }
    }

    private InterpretedText extractText(UUID documentId) {
        if (documentTextInterpreter == null) {
            log.warn("DocumentTextInterpreter not available, using mock data");
            return createMockInterpretedText();
        }
        return documentTextInterpreter.extract(documentId);
    }

    private DocumentType classifyDocument(InterpretedText text, DocumentType hintedType) {
        if (documentClassifier == null) {
            log.warn("DocumentClassifier not available, using hinted type or default");
            return hintedType != null ? hintedType : DocumentType.INVOICE;
        }
        return documentClassifier.classify(text, hintedType);
    }

    private InvoiceFieldsDTO extractInvoiceFields(InterpretedText text, InvoiceExtractor extractor, StringBuilder extractionMethods) {
        if (extractor == null) {
            log.warn("InvoiceExtractor not available, returning null");
            return null;
        }
        
        // Track which extractor is being used
        String extractorType = extractor.getClass().getSimpleName();
        if (extractionMethods.length() > 0) extractionMethods.append(", ");
        extractionMethods.append(extractorType);
        
        return extractor.extract(text);
    }

    private List<StatementTransaction> extractStatementTransactions(InterpretedText text, InterpretationResult result, boolean useAi, StringBuilder extractionMethods) {
        StatementExtractor extractor = useAi ? aiStatementExtractor : heuristicStatementExtractor;
        
        if (extractor == null) {
            log.warn("StatementExtractor ({}) not available, returning empty list", useAi ? "AI" : "Heuristic");
            return List.of();
        }
        
        // Track which extractor is being used
        String extractorType = useAi ? "AI" : "Heuristic";
        if (extractionMethods.length() > 0) extractionMethods.append(", ");
        extractionMethods.append(extractorType).append("StatementExtractor");
        
        List<StatementTransaction> transactions = extractor.extract(text);
        String accountNo = extractAccountNo(text);
        if (accountNo != null) {
            result.setAccountNo(accountNo);
            for (StatementTransaction transaction : transactions) {
                if (transaction.getAccountNo() == null) {
                    transaction.setAccountNo(accountNo);
                }
            }
        }
        return transactions;
    }

    private Map<String, Double> calculateConfidenceScores(InterpretationResult result, InterpretedText text) {
        Map<String, Object> extractedFields = Map.of(); // Build from result
        return confidenceScorer.score(extractedFields, text);
    }

    private InterpretedText createMockInterpretedText() {
        InterpretedText text = new InterpretedText();
        text.setRawText("Mock document text");
        text.setLines(List.of("Line 1", "Line 2"));
        text.setOcrUsed(false);
        text.setLanguageDetected("nb");
        text.setTextExtractorUsed("Mock");
        return text;
    }

    private String extractAccountNo(InterpretedText text) {
        if (text == null) {
            return null;
        }
        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (text.getLines() != null && !text.getLines().isEmpty()) {
            candidates.addAll(text.getLines());
        }
        if (text.getRawText() != null) {
            candidates.add(text.getRawText());
        }

        for (String line : candidates) {
            String normalized = findAccountNoFromIban(line);
            if (normalized != null) {
                return normalized;
            }
        }

        for (String line : candidates) {
            String normalized = findAccountNoInLine(line, true);
            if (normalized != null) {
                return normalized;
            }
        }

        return null;
    }

        private boolean isTaxiReceipt(UUID documentId) {
        return documentRepository.findById(documentId.toString())
            .map(doc -> doc.getOriginalFilename() != null
                && doc.getOriginalFilename().toLowerCase().contains("taxi"))
            .orElse(false);
        }

    private String findAccountNoFromIban(String line) {
        if (line == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("NO\\s*\\d{2}\\s*(\\d{4})\\s*(\\d{2})\\s*(\\d{5})", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(line);
        if (matcher.find()) {
            return matcher.group(1) + matcher.group(2) + matcher.group(3);
        }
        return null;
    }

    private String findAccountNoInLine(String line, boolean requireHint) {
        if (line == null) {
            return null;
        }
        String lower = line.toLowerCase();
        if (requireHint && !(lower.contains("konto") || lower.contains("iban") || lower.contains("account"))) {
            return null;
        }
        if (lower.contains("til:") || lower.contains("betalt") || lower.contains("fra:")) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[0-9][0-9\\s\\.]{9,}[0-9]")
                .matcher(line);
        while (matcher.find()) {
            String candidate = matcher.group();
            String digitsOnly = candidate.replaceAll("[^0-9]", "");
            if (digitsOnly.length() == 11) {
                return digitsOnly;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
