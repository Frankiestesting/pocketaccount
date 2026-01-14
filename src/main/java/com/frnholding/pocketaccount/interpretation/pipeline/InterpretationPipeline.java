package com.frnholding.pocketaccount.interpretation.pipeline;

import com.frnholding.pocketaccount.interpretation.domain.InterpretationResult;
import com.frnholding.pocketaccount.interpretation.domain.InvoiceFields;
import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
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

    @Autowired(required = false)
    @Qualifier("compositeTextExtractor")
    private DocumentTextInterpreter documentTextInterpreter;

    @Autowired(required = false)
    private DocumentClassifier documentClassifier;

    @Autowired(required = false)
    private InvoiceExtractor invoiceExtractor;

    @Autowired(required = false)
    private StatementExtractor statementExtractor;

    @Autowired(required = false)
    private FieldNormalizer fieldNormalizer;

    @Autowired(required = false)
    private ConfidenceScorer confidenceScorer;

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

            if (documentType == DocumentType.INVOICE) {
                InvoiceFields invoiceFields = extractInvoiceFields(interpretedText);
                result.setInvoiceFields(invoiceFields);
                log.debug("Invoice fields extracted: {}", invoiceFields);
            } else if (documentType == DocumentType.STATEMENT) {
                List<StatementTransaction> transactions = extractStatementTransactions(interpretedText, result);
                result.setStatementTransactions(transactions);
                log.debug("Statement transactions extracted: {} transactions", transactions.size());
            }

            // Step 4: Calculate confidence scores (if available)
            if (confidenceScorer != null) {
                Map<String, Double> scores = calculateConfidenceScores(result, interpretedText);
                log.debug("Confidence scores calculated: {}", scores);
            }

            log.info("Interpretation pipeline completed successfully for document: {}", documentId);
            return result;

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

    private InvoiceFields extractInvoiceFields(InterpretedText text) {
        if (invoiceExtractor == null) {
            log.warn("InvoiceExtractor not available, returning null");
            return null;
        }
        return invoiceExtractor.extract(text);
    }

    private List<StatementTransaction> extractStatementTransactions(InterpretedText text, InterpretationResult result) {
        if (statementExtractor == null) {
            log.warn("StatementExtractor not available, returning empty list");
            return List.of();
        }
        return statementExtractor.extract(text);
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
        return text;
    }
}
