package com.frnholding.pocketaccount.interpretation.service;

import com.frnholding.pocketaccount.interpretation.domain.*;
import com.frnholding.pocketaccount.interpretation.repository.InterpretationJobRepository;
import com.frnholding.pocketaccount.interpretation.repository.InterpretationResultRepository;
import com.frnholding.pocketaccount.domain.Document;
import com.frnholding.pocketaccount.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InterpretationService {

    @Autowired
    private InterpretationJobRepository interpretationJobRepository;

    @Autowired
    private InterpretationResultRepository interpretationResultRepository;

    @Autowired
    private DocumentService documentService;

    @Transactional
    public InterpretationJob startInterpretation(String documentId) {
        // Validate document exists
        Document document = documentService.getDocument(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        // Create interpretation job
        String jobId = UUID.randomUUID().toString();
        InterpretationJob job = new InterpretationJob(
                jobId,
                documentId,
                "PENDING",
                Instant.now(),
                null,
                null,
                null,
                document.getDocumentType()
        );

        // Save job
        interpretationJobRepository.save(job);

        // TODO: Trigger async interpretation process here
        // For now, we'll create mock results immediately
        createMockInterpretationResult(documentId, document.getDocumentType());

        return job;
    }

    public InterpretationResult getInterpretationResult(String documentId) {
        return interpretationResultRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No interpretation result found for document: " + documentId));
    }

    @Transactional
    private void createMockInterpretationResult(String documentId, String documentType) {
        InterpretationResult result = new InterpretationResult();
        result.setDocumentId(documentId);
        result.setDocumentType(documentType);
        result.setInterpretedAt(Instant.now());

        if ("INVOICE".equals(documentType)) {
            // Create mock invoice fields
            InvoiceFields invoiceFields = new InvoiceFields(
                    12450.00,
                    "NOK",
                    LocalDate.parse("2026-01-02"),
                    "Faktura strøm januar",
                    "Strøm AS"
            );
            result.setInvoiceFields(invoiceFields);
        } else if ("STATEMENT".equals(documentType)) {
            // Create mock statement transactions
            List<StatementTransaction> transactions = new ArrayList<>();
            StatementTransaction transaction = new StatementTransaction();
            transaction.setInterpretationResult(result);
            transaction.setAmount(-399.00);
            transaction.setCurrency("NOK");
            transaction.setDate(LocalDate.parse("2026-01-03"));
            transaction.setDescription("KIWI 123");
            transactions.add(transaction);
            
            result.setStatementTransactions(transactions);
        }

        interpretationResultRepository.save(result);
    }
}
