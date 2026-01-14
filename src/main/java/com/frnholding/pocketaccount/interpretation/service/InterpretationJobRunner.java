package com.frnholding.pocketaccount.interpretation.service;

import com.frnholding.pocketaccount.interpretation.domain.*;
import com.frnholding.pocketaccount.interpretation.repository.InterpretationJobRepository;
import com.frnholding.pocketaccount.interpretation.repository.InterpretationResultRepository;
import com.frnholding.pocketaccount.domain.Document;
import com.frnholding.pocketaccount.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class InterpretationJobRunner {

    @Autowired
    private InterpretationJobRepository interpretationJobRepository;

    @Autowired
    private InterpretationResultRepository interpretationResultRepository;

    @Autowired
    private DocumentService documentService;

    @Transactional
    public void runJob(String jobId, boolean useOcr, boolean useAi, String languageHint) {
        log.info("Starting interpretation job: {} with useOcr={}, useAi={}, languageHint={}", 
                jobId, useOcr, useAi, languageHint);

        InterpretationJob job = interpretationJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        try {
            // Update job status to RUNNING
            job.setStatus("RUNNING");
            job.setStartedAt(Instant.now());
            interpretationJobRepository.save(job);

            // Get document
            Document document = documentService.getDocument(job.getDocumentId());
            if (document == null) {
                throw new IllegalArgumentException("Document not found: " + job.getDocumentId());
            }

            // Perform interpretation based on configuration
            InterpretationResult result = performInterpretation(
                    job.getDocumentId(),
                    job.getDocumentType(),
                    useOcr,
                    useAi,
                    languageHint
            );

            // Save result
            interpretationResultRepository.save(result);

            // Update job status to COMPLETED
            job.setStatus("COMPLETED");
            job.setFinishedAt(Instant.now());
            interpretationJobRepository.save(job);

            log.info("Interpretation job completed successfully: {}", jobId);

        } catch (Exception e) {
            log.error("Error running interpretation job: {}", jobId, e);
            
            // Update job status to FAILED
            job.setStatus("FAILED");
            job.setFinishedAt(Instant.now());
            job.setError(e.getMessage());
            interpretationJobRepository.save(job);
            
            throw new RuntimeException("Interpretation job failed: " + jobId, e);
        }
    }

    private InterpretationResult performInterpretation(
            String documentId,
            String documentType,
            boolean useOcr,
            boolean useAi,
            String languageHint) {

        log.info("Performing interpretation for document: {} with type: {}", documentId, documentType);

        InterpretationResult result = new InterpretationResult();
        result.setDocumentId(documentId);
        result.setDocumentType(documentType);
        result.setInterpretedAt(Instant.now());

        if ("INVOICE".equals(documentType)) {
            result.setInvoiceFields(extractInvoiceFields(documentId, useOcr, useAi, languageHint));
        } else if ("STATEMENT".equals(documentType)) {
            result.setStatementTransactions(extractStatementTransactions(result, documentId, useOcr, useAi, languageHint));
        }

        return result;
    }

    private InvoiceFields extractInvoiceFields(String documentId, boolean useOcr, boolean useAi, String languageHint) {
        // TODO: Implement actual OCR and AI-based extraction
        // For now, return mock data with higher accuracy if AI is enabled
        
        double amount = useAi ? 12450.00 : 12400.00;
        String description = useAi ? "Faktura strøm januar - AI enhanced" : "Faktura strøm januar";
        
        log.debug("Extracted invoice fields with OCR={}, AI={}, language={}", useOcr, useAi, languageHint);
        
        return new InvoiceFields(
                amount,
                "NOK",
                LocalDate.parse("2026-01-02"),
                description,
                "Strøm AS"
        );
    }

    private List<StatementTransaction> extractStatementTransactions(
            InterpretationResult result,
            String documentId,
            boolean useOcr,
            boolean useAi,
            String languageHint) {
        
        // TODO: Implement actual OCR and AI-based extraction
        // For now, return mock data
        
        List<StatementTransaction> transactions = new ArrayList<>();
        
        StatementTransaction transaction1 = new StatementTransaction();
        transaction1.setInterpretationResult(result);
        transaction1.setAmount(-399.00);
        transaction1.setCurrency("NOK");
        transaction1.setDate(LocalDate.parse("2026-01-03"));
        transaction1.setDescription(useAi ? "KIWI 123 - Groceries" : "KIWI 123");
        transactions.add(transaction1);

        if (useAi) {
            // AI mode extracts more transactions
            StatementTransaction transaction2 = new StatementTransaction();
            transaction2.setInterpretationResult(result);
            transaction2.setAmount(-150.00);
            transaction2.setCurrency("NOK");
            transaction2.setDate(LocalDate.parse("2026-01-04"));
            transaction2.setDescription("Circle K - Fuel");
            transactions.add(transaction2);
        }

        log.debug("Extracted {} transactions with OCR={}, AI={}, language={}", 
                transactions.size(), useOcr, useAi, languageHint);
        
        return transactions;
    }
}
