package com.frnholding.pocketaccount.interpretation.service;

import com.frnholding.pocketaccount.interpretation.domain.*;
import com.frnholding.pocketaccount.interpretation.infra.OpenAiAuthenticationException;
import com.frnholding.pocketaccount.interpretation.repository.InterpretationJobRepository;
import com.frnholding.pocketaccount.interpretation.repository.InterpretationResultRepository;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretationPipeline;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretationOptions;
import com.frnholding.pocketaccount.interpretation.pipeline.DocumentType;
import com.frnholding.pocketaccount.domain.Document;
import com.frnholding.pocketaccount.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class InterpretationJobRunner {

    private static final Logger log = LoggerFactory.getLogger(InterpretationJobRunner.class);

    @Autowired
    private InterpretationJobRepository interpretationJobRepository;

    @Autowired
    private InterpretationResultRepository interpretationResultRepository;

    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private InterpretationPipeline interpretationPipeline;

    @Async
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
                    jobId,
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
            String jobId,
            String documentId,
            String documentType,
            boolean useOcr,
            boolean useAi,
            String languageHint) {

        log.info("Performing interpretation for document: {} with type: {}", documentId, documentType);

        try {
            // Convert documentType string to enum
            DocumentType hintedType = null;
            if (documentType != null) {
                try {
                    hintedType = DocumentType.valueOf(documentType);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid document type: {}, will auto-detect", documentType);
                }
            }

            // Build interpretation options
            InterpretationOptions options = InterpretationOptions.builder()
                    .useOcr(useOcr)
                    .useAi(useAi)
                    .languageHint(languageHint)
                    .hintedType(hintedType)
                    .build();

            // Execute the interpretation pipeline
            InterpretationResult result = interpretationPipeline.execute(
                    UUID.fromString(documentId),
                    options
            );

            // Set the jobId for this result
            result.setJobId(jobId);
            
            // Ensure StatementTransactions are linked to the result
            if (result.getStatementTransactions() != null) {
                result.getStatementTransactions().forEach(t -> t.setInterpretationResult(result));
            }

            log.info("Interpretation completed for document: {} with type: {}", documentId, result.getDocumentType());
            return result;

        } catch (OpenAiAuthenticationException e) {
            log.error("OpenAI authentication failed for document: {}", documentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Error performing interpretation for document: {}", documentId, e);
            throw new RuntimeException("Interpretation failed: " + e.getMessage(), e);
        }
    }
}
