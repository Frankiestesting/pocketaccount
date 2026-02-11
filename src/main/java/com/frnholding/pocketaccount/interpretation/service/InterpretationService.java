package com.frnholding.pocketaccount.interpretation.service;

import com.frnholding.pocketaccount.interpretation.api.dto.*;
import com.frnholding.pocketaccount.interpretation.domain.*;
import com.frnholding.pocketaccount.interpretation.repository.InterpretationJobRepository;
import com.frnholding.pocketaccount.interpretation.repository.InterpretationResultRepository;
import com.frnholding.pocketaccount.interpretation.repository.CorrectionHistoryRepository;
import com.frnholding.pocketaccount.interpretation.repository.entity.CorrectionHistoryEntity;
import com.frnholding.pocketaccount.domain.Document;
import com.frnholding.pocketaccount.exception.EntityNotFoundException;
import com.frnholding.pocketaccount.service.DocumentService;
import com.frnholding.pocketaccount.accounting.domain.BankTransaction;
import com.frnholding.pocketaccount.accounting.domain.Account;
import com.frnholding.pocketaccount.accounting.repository.AccountRepository;
import com.frnholding.pocketaccount.accounting.repository.BankTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InterpretationService {

    @Autowired
    private InterpretationJobRepository interpretationJobRepository;

    @Autowired
    private InterpretationResultRepository interpretationResultRepository;

    @Autowired
    private CorrectionHistoryRepository correctionHistoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private InterpretationJobRunner interpretationJobRunner;

    @Transactional
    public InterpretationJob startInterpretation(String documentId) {
        // Validate document exists
        Document document = documentService.getDocument(documentId);
        if (document == null) {
            throw new EntityNotFoundException("Document not found: " + documentId);
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
                .orElseThrow(() -> new EntityNotFoundException("No interpretation result found for document: " + documentId));
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
            throw new EntityNotFoundException("Document not found: " + documentId);
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

        // Trigger async interpretation process after commit so the job exists for the async thread.
        Runnable startJob = () -> interpretationJobRunner.runJob(
                jobId,
                request.isUseOcr(),
                request.isUseAi(),
                request.getLanguageHint()
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    startJob.run();
                }
            });
        } else {
            startJob.run();
        }

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
            .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

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

    public List<JobStatusResponseDTO> getJobs(int page, int size) {
        List<InterpretationJob> jobs = interpretationJobRepository.findAll(PageRequest.of(page, size)).getContent();

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
                .orElseThrow(() -> new EntityNotFoundException("No interpretation result found for document: " + documentId));

        return buildExtractionResultResponse(result);
    }

    /**
     * Get extraction results for a specific job.
     */
    public ExtractionResultResponseDTO getJobResult(String jobId) {
        InterpretationResult result = interpretationResultRepository.findByJobId(jobId)
                .orElseThrow(() -> new EntityNotFoundException("No interpretation result found for job: " + jobId));

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
                    t.getDescription(),
                    t.getAccountNo(),
                    t.isApproved()
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
    public Integer saveCorrection(String documentId, SaveCorrectionRequestDTO request) {
        // Validate document exists
        Document document = documentService.getDocument(documentId);
        if (document == null) {
            throw new EntityNotFoundException("Document not found: " + documentId);
        }

        Integer nextVersion = null;
        OffsetDateTime correctedAt = OffsetDateTime.now();
        String correctedBy = "user:123";
        boolean historySaved = false;

        // Find or create interpretation result
        InterpretationResult result = interpretationResultRepository.findByDocumentId(documentId)
                .orElseGet(() -> {
                    InterpretationResult newResult = new InterpretationResult();
                    newResult.setDocumentId(documentId);
                    newResult.setDocumentType(request.getDocumentType());
                    newResult.setInterpretedAt(Instant.now());
                    return newResult;
                });

        String resolvedDocumentType = request.getDocumentType() != null
            ? request.getDocumentType()
            : (result.getDocumentType() != null ? result.getDocumentType() : document.getDocumentType());

        // Update with corrected data (capture previous snapshot first)
        result.setDocumentType(resolvedDocumentType);

        if (request.getInvoiceFields() != null) {
            Map<String, Object> previousSnapshot = buildInvoiceSnapshot(result.getInvoiceFields());
            nextVersion = ensureVersion(nextVersion, documentId);
            saveHistory(document, resolvedDocumentType, "INVOICE_FIELDS", null,
                previousSnapshot != null ? previousSnapshot : new LinkedHashMap<>(),
                request.getNote(), nextVersion, correctedAt, correctedBy);
            historySaved = true;

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
            if (result.getStatementTransactions() != null && !result.getStatementTransactions().isEmpty()) {
                nextVersion = ensureVersion(nextVersion, documentId);
                for (StatementTransaction existing : result.getStatementTransactions()) {
                    Map<String, Object> previousSnapshot = buildStatementSnapshot(existing);
                        saveHistory(document, resolvedDocumentType, "STATEMENT_TRANSACTION",
                            existing.getId() != null ? existing.getId().toString() : null,
                            previousSnapshot,
                            request.getNote(),
                            nextVersion,
                            correctedAt,
                            correctedBy);
                    historySaved = true;
                }
            }

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
                transaction.setAccountNo(dto.getAccountNo());
                transaction.setApproved(Boolean.TRUE.equals(dto.getApproved()));
                result.getStatementTransactions().add(transaction);
            }
            result.setInvoiceFields(null); // Clear invoice fields for statement
        }

        InterpretationResult saved = interpretationResultRepository.save(result);

        syncApprovedBankTransactions(saved, documentId);
        return historySaved ? nextVersion : null;
    }

        private void saveHistory(Document document, String documentType, String entityType, String entityId,
                     Map<String, Object> snapshot, String note, Integer version,
                     OffsetDateTime correctedAt, String correctedBy) {
        CorrectionHistoryEntity history = new CorrectionHistoryEntity(
                UUID.randomUUID(),
                document.getId(),
            documentType,
                entityType,
                entityId,
                snapshot,
                note,
            version,
            correctedAt,
            correctedBy
        );
        correctionHistoryRepository.save(history);
    }

    private Map<String, Object> buildInvoiceSnapshot(InvoiceFieldsDTO fields) {
        if (fields == null) {
            return null;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("amount", fields.getAmount());
        snapshot.put("currency", fields.getCurrency());
        snapshot.put("date", fields.getDate());
        snapshot.put("description", fields.getDescription());
        snapshot.put("sender", fields.getSender());
        return snapshot;
    }

    private Map<String, Object> buildStatementSnapshot(StatementTransaction transaction) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("amount", transaction.getAmount());
        snapshot.put("currency", transaction.getCurrency());
        snapshot.put("date", transaction.getDate());
        snapshot.put("description", transaction.getDescription());
        snapshot.put("accountNo", transaction.getAccountNo());
        snapshot.put("approved", transaction.isApproved());
        return snapshot;
    }

    private Integer ensureVersion(Integer currentVersion, String documentId) {
        if (currentVersion != null) {
            return currentVersion;
        }
        return correctionHistoryRepository.findMaxCorrectionVersionByDocumentId(documentId)
                .orElse(0) + 1;
    }

    private void syncApprovedBankTransactions(InterpretationResult result, String documentId) {
        if (result.getStatementTransactions() == null || result.getStatementTransactions().isEmpty()) {
            return;
        }

        for (StatementTransaction transaction : result.getStatementTransactions()) {
            if (!transaction.isApproved()) {
                continue;
            }
            if (transaction.getAccountNo() == null) {
                throw new IllegalArgumentException("Approved statement transaction missing accountNo");
            }
            if (transaction.getDate() == null) {
                throw new IllegalArgumentException("Approved statement transaction missing date");
            }
            if (transaction.getAmount() == null || transaction.getCurrency() == null || transaction.getDescription() == null) {
                throw new IllegalArgumentException("Approved statement transaction missing required fields");
            }

            Account account = accountRepository.findByAccountNo(transaction.getAccountNo())
                    .orElseThrow(() -> new EntityNotFoundException("Account not found for accountNo: " + transaction.getAccountNo()));

            BigDecimal amount = toBigDecimal(transaction.getAmount());
            BankTransaction bankTransaction = bankTransactionRepository
                    .findFirstByAccountIdAndBookingDateAndAmountAndCurrencyAndDescription(
                            account.getId(),
                            transaction.getDate(),
                            amount,
                            transaction.getCurrency(),
                            transaction.getDescription()
                    )
                    .orElseGet(() -> createBankTransactionFromStatement(transaction, account, documentId, amount));

            transaction.setBankTransaction(bankTransaction);
        }

        interpretationResultRepository.save(result);
    }

    private BankTransaction createBankTransactionFromStatement(StatementTransaction transaction, Account account,
                                                               String documentId, BigDecimal amount) {
        BankTransaction bankTransaction = new BankTransaction();
        bankTransaction.setAccount(account);
        bankTransaction.setBookingDate(transaction.getDate());
        bankTransaction.setValueDate(null);
        bankTransaction.setAmount(amount);
        bankTransaction.setCurrency(transaction.getCurrency());
        bankTransaction.setCounterparty(null);
        bankTransaction.setDescription(transaction.getDescription());
        bankTransaction.setReference(null);
        bankTransaction.setSourceDocumentId(parseDocumentId(documentId));
        bankTransaction.setSourceLineHash(buildSourceLineHash(transaction));
        bankTransaction.setCreatedAt(Instant.now());
        return bankTransactionRepository.save(bankTransaction);
    }

    private UUID parseDocumentId(String documentId) {
        try {
            return UUID.fromString(documentId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildSourceLineHash(StatementTransaction transaction) {
        String payload = String.format("%s|%s|%s|%s",
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDescription(),
                transaction.getDate());
        return sha256Hex(payload);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
