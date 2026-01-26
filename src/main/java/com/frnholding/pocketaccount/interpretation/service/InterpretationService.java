package com.frnholding.pocketaccount.interpretation.service;

import com.frnholding.pocketaccount.interpretation.api.dto.*;
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
import java.util.stream.Collectors;

@Service
public class InterpretationService {

    @Autowired
    private InterpretationJobRepository interpretationJobRepository;

    @Autowired
    private InterpretationResultRepository interpretationResultRepository;

    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private InterpretationJobRunner interpretationJobRunner;

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
            InvoiceFieldsDTO invoiceFields = new InvoiceFieldsDTO(
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

    /**
     * Start a new extraction job with configuration options.
     */
    @Transactional
    public StartExtractionResponseDTO startExtraction(String documentId, StartExtractionRequestDTO request) {
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

        interpretationJobRepository.save(job);

        // Trigger async interpretation process
        interpretationJobRunner.runJob(
                jobId,
                request.isUseOcr(),
                request.isUseAi(),
                request.getLanguageHint()
        );

        return new StartExtractionResponseDTO(
                job.getId(),
                job.getDocumentId(),
                job.getStatus(),
                job.getCreated(),
                job.getDocumentType()
        );
    }

    /**
     * Get job status.
     */
    public JobStatusResponseDTO getJobStatus(String jobId) {
        InterpretationJob job = interpretationJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        JobStatusResponseDTO response = new JobStatusResponseDTO();
        response.setJobId(job.getId());
        response.setDocumentId(job.getDocumentId());
        response.setStatus(job.getStatus());
        response.setDocumentType(job.getDocumentType());
        response.setCreated(job.getCreated());
        response.setStartedAt(job.getStartedAt());
        response.setFinishedAt(job.getFinishedAt());
        response.setError(job.getError());
        
        // Fetch document to get original filename
        Document doc = documentService.getDocument(job.getDocumentId());
        if (doc != null) {
            response.setOriginalFilename(doc.getOriginalFilename());
        }
        
        // Fetch extraction methods from result if job is completed
        if ("COMPLETED".equals(job.getStatus())) {
            interpretationResultRepository.findByJobId(job.getId())
                .ifPresent(result -> response.setExtractionMethods(result.getExtractionMethods()));
        }
        
        return response;
    }

    /**
     * Get all interpretation jobs with document information.
     */
    public List<JobStatusResponseDTO> getAllJobs() {
        List<InterpretationJob> jobs = interpretationJobRepository.findAll();
        
        return jobs.stream()
                .map(job -> {
                    Document doc = documentService.getDocument(job.getDocumentId());
                    JobStatusResponseDTO response = new JobStatusResponseDTO();
                    response.setJobId(job.getId());
                    response.setDocumentId(job.getDocumentId());
                    response.setStatus(job.getStatus());
                    response.setDocumentType(job.getDocumentType());
                    response.setCreated(job.getCreated());
                    response.setStartedAt(job.getStartedAt());
                    response.setFinishedAt(job.getFinishedAt());
                    response.setError(job.getError());
                    
                    if (doc != null) {
                        response.setOriginalFilename(doc.getOriginalFilename());
                    }
                    // Fetch extraction methods from result if job is completed
                    if ("COMPLETED".equals(job.getStatus())) {
                        interpretationResultRepository.findByJobId(job.getId())
                            .ifPresent(result -> response.setExtractionMethods(result.getExtractionMethods()));
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get extraction results for a document.
     */
    public ExtractionResultResponseDTO getExtractionResult(String documentId) {
        InterpretationResult result = interpretationResultRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No interpretation result found for document: " + documentId));

        return buildExtractionResultResponse(result);
    }

    /**
     * Get extraction results for a specific job.
     */
    public ExtractionResultResponseDTO getJobResult(String jobId) {
        InterpretationResult result = interpretationResultRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("No interpretation result found for job: " + jobId));

        return buildExtractionResultResponse(result);
    }

    private ExtractionResultResponseDTO buildExtractionResultResponse(InterpretationResult result) {
        ExtractionResultResponseDTO response = new ExtractionResultResponseDTO();
        response.setDocumentId(result.getDocumentId());
        response.setDocumentType(result.getDocumentType());
        response.setInterpretedAt(result.getInterpretedAt());
        response.setExtractionMethods(result.getExtractionMethods());

        // Populate invoice fields if present
        if (result.getInvoiceFields() != null) {
            InvoiceFieldsDTO fields = result.getInvoiceFields();
            response.setInvoiceFields(new ExtractionResultResponseDTO.InvoiceFieldsDto(
                    fields.getAmount(),
                    fields.getCurrency(),
                    fields.getDate(),
                    fields.getDescription(),
                    fields.getSender()
            ));
        }

        // Populate transactions if present
        if (result.getStatementTransactions() != null && !result.getStatementTransactions().isEmpty()) {
            List<ExtractionResultResponseDTO.TransactionDto> transactions = result.getStatementTransactions().stream()
                    .map(t -> new ExtractionResultResponseDTO.TransactionDto(
                            t.getAmount(),
                            t.getCurrency(),
                            t.getDate(),
                            t.getDescription()
                    ))
                    .collect(Collectors.toList());
            response.setTransactions(transactions);
        }

        return response;
    }

    /**
     * Save corrections to interpretation results.
     */
    @Transactional
    public void saveCorrection(String documentId, SaveCorrectionRequestDTO request) {
        // Validate document exists
        Document document = documentService.getDocument(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        // Find or create interpretation result
        InterpretationResult result = interpretationResultRepository.findByDocumentId(documentId)
                .orElseGet(() -> {
                    InterpretationResult newResult = new InterpretationResult();
                    newResult.setDocumentId(documentId);
                    newResult.setDocumentType(request.getDocumentType());
                    newResult.setInterpretedAt(Instant.now());
                    return newResult;
                });

        // Update with corrected data
        result.setDocumentType(request.getDocumentType());

        if (request.getInvoiceFields() != null) {
            SaveCorrectionRequestDTO.InvoiceFieldsDto dto = request.getInvoiceFields();
            InvoiceFieldsDTO fields = new InvoiceFieldsDTO(
                    dto.getAmount(),
                    dto.getCurrency(),
                    dto.getDate(),
                    dto.getDescription(),
                    dto.getSender()
            );
            result.setInvoiceFields(fields);
            result.setStatementTransactions(new ArrayList<>()); // Clear transactions for invoice
        }

        if (request.getTransactions() != null && !request.getTransactions().isEmpty()) {
            // Clear existing transactions
            result.getStatementTransactions().clear();

            // Add corrected transactions
            for (SaveCorrectionRequestDTO.TransactionDto dto : request.getTransactions()) {
                StatementTransaction transaction = new StatementTransaction();
                transaction.setInterpretationResult(result);
                transaction.setAmount(dto.getAmount());
                transaction.setCurrency(dto.getCurrency());
                transaction.setDate(dto.getDate());
                transaction.setDescription(dto.getDescription());
                result.getStatementTransactions().add(transaction);
            }
            result.setInvoiceFields(null); // Clear invoice fields for statement
        }

        interpretationResultRepository.save(result);
    }
}
